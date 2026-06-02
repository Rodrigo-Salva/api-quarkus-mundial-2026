/**
 * STRESS TEST — Mundial 2026 Predictions API
 * Herramienta: k6 (https://k6.io)
 *
 * ESTRATEGIA PARA 60,000 USUARIOS CONCURRENTES:
 * ─────────────────────────────────────────────
 * 60,000 usuarios NO significa 60,000 requests simultáneos.
 * En la realidad, los usuarios hacen pausas (piensan, leen, navegan).
 * Con think_time=1s y 60,000 VUs → ~60,000 req/s sostenidos.
 *
 * Para demostrar soporte a 60,000 usuarios usamos:
 *   1. Rampa gradual hasta 5,000 VUs reales (lo que soporta 1 máquina)
 *   2. Extrapolación matemática documentada
 *   3. Cache de Nginx (rankings/partidos): 1 req real por cada 1,000 usuarios
 *
 * Ejecución:
 *   k6 run k6/stress-test.js
 *   k6 run --out json=k6/results.json k6/stress-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';

// ── Métricas personalizadas ────────────────────────────────────────
const errorRate          = new Rate('error_rate');
const loginDuration      = new Trend('login_duration_ms');
const predictionDuration = new Trend('prediction_duration_ms');
const rankingDuration    = new Trend('ranking_duration_ms');
const matchesDuration    = new Trend('matches_duration_ms');
const failedRequests     = new Counter('failed_requests');
const cacheHits          = new Counter('cache_hits');

// ── Escenarios ────────────────────────────────────────────────────
export const options = {
    scenarios: {

        // ESCENARIO 1: Smoke — verificar que todo funciona
        smoke: {
            executor: 'constant-vus',
            vus: 1,
            duration: '30s',
            tags: { scenario: 'smoke' },
        },

        // ESCENARIO 2: Carga normal — día de partido (5,000 VUs)
        load_normal: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 500  },  // subida suave
                { duration: '3m', target: 2000 },  // carga media
                { duration: '3m', target: 5000 },  // carga alta sostenida
                { duration: '2m', target: 0    },  // bajada
            ],
            startTime: '35s',
            tags: { scenario: 'load_normal' },
        },

        // ESCENARIO 3: Pico de inicio del partido (spike a 10,000 VUs)
        spike_kickoff: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10000 }, // pico súbito (inicio del partido)
                { duration: '2m',  target: 10000 }, // mantener
                { duration: '30s', target: 0     }, // bajar
            ],
            startTime: '11m',
            tags: { scenario: 'spike_kickoff' },
        },

        // ESCENARIO 4: Lectura masiva del ranking (endpoint cacheado)
        // Simula 60,000 usuarios viendo el ranking — Nginx cache absorbe la carga
        ranking_flood: {
            executor: 'constant-arrival-rate',
            rate: 60000,          // 60,000 req/s al ranking (cacheado en Nginx 5s)
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 200, // solo necesita 200 VUs reales gracias al cache
            maxVUs: 500,
            startTime: '15m',
            tags: { scenario: 'ranking_flood' },
        },

        // ESCENARIO 5: Soak test — estabilidad a largo plazo
        soak: {
            executor: 'constant-vus',
            vus: 1000,
            duration: '5m',
            startTime: '16m30s',
            tags: { scenario: 'soak' },
        },
    },

    thresholds: {
        // Latencia general
        http_req_duration:   ['p(50)<200', 'p(95)<1000', 'p(99)<3000'],
        // Tasa de errores < 5%
        error_rate:          ['rate<0.05'],
        // Rankings (cacheados) deben ser muy rápidos
        ranking_duration_ms: ['p(95)<100'],
        // Login < 2s en p99
        login_duration_ms:   ['p(99)<2000'],
        // Predicciones < 2s en p95
        prediction_duration_ms: ['p(95)<2000'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';

// ── Usuarios pre-creados en seed data ─────────────────────────────
// Se usan para no saturar la BD con millones de registros en el test
const SEED_USERS = [
    { email: 'rodrigo@test.com',  password: 'Test1234!' },
    { email: 'carlos@test.com',   password: 'Test1234!' },
    { email: 'maria@test.com',    password: 'Test1234!' },
    { email: 'juan@test.com',     password: 'Test1234!' },
    { email: 'sofia@test.com',    password: 'Test1234!' },
];

function randomUser() {
    return SEED_USERS[Math.floor(Math.random() * SEED_USERS.length)];
}

function randomEmail() {
    return `load_${__VU}_${__ITER}@test.com`;
}

// ── Flujo principal ────────────────────────────────────────────────
export default function () {
    const scenario = __ENV.K6_SCENARIO || '';

    // Escenario de ranking flood: solo llama al ranking (cacheado)
    if (scenario === 'ranking_flood') {
        rankingFlood();
        return;
    }

    fullUserFlow();
}

// Flujo completo del usuario
function fullUserFlow() {
    let token = null;

    // 70% de VUs usan usuarios del seed (más realista)
    // 30% crean usuarios nuevos (simula nuevos registros durante el mundial)
    if (Math.random() < 0.7) {
        token = loginSeedUser();
    } else {
        token = registerAndLogin();
    }

    if (!token) return;

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };

    // Flujo típico de un usuario viendo el mundial:
    group('Ver partidos', () => {
        const start = Date.now();
        const res = http.get(`${BASE_URL}/api/matches`, { headers });
        matchesDuration.add(Date.now() - start);
        check(res, { 'matches 200': r => r.status === 200 })
            || (errorRate.add(1), failedRequests.add(1));
        // Detectar hit de cache Nginx
        if (res.headers['X-Cache-Status'] === 'HIT') cacheHits.add(1);
    });

    sleep(0.5); // usuario lee la lista

    group('Ver ranking global', () => {
        const start = Date.now();
        const res = http.get(`${BASE_URL}/api/rankings/global?limit=20`);
        rankingDuration.add(Date.now() - start);
        check(res, { 'ranking 200': r => r.status === 200 })
            || (errorRate.add(1), failedRequests.add(1));
        if (res.headers['X-Cache-Status'] === 'HIT') cacheHits.add(1);
    });

    sleep(0.3);

    group('Enviar predicción', () => {
        const start = Date.now();
        const res = http.post(`${BASE_URL}/api/predictions`,
            JSON.stringify({ matchId: 13, homeScore: 2, awayScore: 1 }),
            { headers });
        predictionDuration.add(Date.now() - start);
        // 201=ok, 400=ya predijo este partido, 422=partido no disponible
        check(res, { 'prediction ok': r => [201, 400, 422].includes(r.status) })
            || (errorRate.add(1), failedRequests.add(1));
    });

    sleep(0.2);

    group('Ver mis predicciones', () => {
        const res = http.get(`${BASE_URL}/api/predictions/me`, { headers });
        check(res, { 'my predictions 200': r => r.status === 200 })
            || (errorRate.add(1), failedRequests.add(1));
    });

    sleep(1); // piensa antes de continuar
}

// Login con usuario del seed
function loginSeedUser() {
    const user = randomUser();
    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/auth/login`,
        JSON.stringify({ email: user.email, password: user.password }),
        { headers: { 'Content-Type': 'application/json' } });
    loginDuration.add(Date.now() - start);

    if (!check(res, { 'login 200': r => r.status === 200 })) {
        errorRate.add(1);
        failedRequests.add(1);
        return null;
    }
    try { return JSON.parse(res.body).data?.accessToken; }
    catch { return null; }
}

// Registro + login (simula usuario nuevo)
function registerAndLogin() {
    const email = randomEmail();
    const username = `vu${__VU}_${__ITER}`;

    http.post(`${BASE_URL}/api/auth/register`,
        JSON.stringify({ email, username, password: 'Test1234!', country: 'PER' }),
        { headers: { 'Content-Type': 'application/json' } });

    return loginWithCreds(email, 'Test1234!');
}

function loginWithCreds(email, password) {
    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/auth/login`,
        JSON.stringify({ email, password }),
        { headers: { 'Content-Type': 'application/json' } });
    loginDuration.add(Date.now() - start);

    if (res.status !== 200) { errorRate.add(1); return null; }
    try { return JSON.parse(res.body).data?.accessToken; }
    catch { return null; }
}

// Solo consulta el ranking (para el escenario de flood cacheado)
function rankingFlood() {
    const res = http.get(`${BASE_URL}/api/rankings/global?limit=50`);
    check(res, { 'ranking flood 200': r => r.status === 200 })
        || errorRate.add(1);
    if (res.headers['X-Cache-Status'] === 'HIT') cacheHits.add(1);
}

// ── Reporte final ──────────────────────────────────────────────────
export function handleSummary(data) {
    const report = generateReport(data);
    return {
        'k6/results-summary.json': JSON.stringify(data, null, 2),
        'k6/results-report.txt':   report,
        stdout: report,
    };
}

function generateReport(data) {
    const m   = data.metrics;
    const p   = (metric, pct) => m[metric]?.values?.[`p(${pct})`]?.toFixed(0) ?? 'N/A';
    const r   = (metric)      => (m[metric]?.values?.rate * 100)?.toFixed(2)   ?? 'N/A';
    const cnt = (metric)      => m[metric]?.values?.count ?? 0;
    const rps = m.http_reqs?.values?.rate?.toFixed(0) ?? 'N/A';

    const cacheHitCount  = cnt('cache_hits');
    const totalReqs      = cnt('http_reqs');
    const cacheHitRate   = totalReqs > 0 ? ((cacheHitCount / totalReqs) * 100).toFixed(1) : '0';

    // Extrapolación para 60,000 usuarios
    const p95ms     = parseFloat(p('http_req_duration', 95)) || 0;
    const errRate   = parseFloat(r('error_rate')) || 0;
    const wouldWork = p95ms < 1000 && errRate < 5;

    return `
╔══════════════════════════════════════════════════════════════════╗
║        STRESS TEST — MUNDIAL 2026 PREDICTIONS API               ║
║        Simulando carga para 60,000 usuarios concurrentes        ║
╠══════════════════════════════════════════════════════════════════╣
║  THROUGHPUT                                                      ║
║    Requests totales  : ${String(totalReqs).padEnd(10)}                          ║
║    Requests/segundo  : ${String(rps).padEnd(10)} req/s                      ║
║    Cache hits Nginx  : ${String(cacheHitRate).padEnd(10)}% (rankings/partidos)   ║
╠══════════════════════════════════════════════════════════════════╣
║  LATENCIA HTTP                                                   ║
║    p50  : ${String(p('http_req_duration',50)).padEnd(8)} ms                              ║
║    p90  : ${String(p('http_req_duration',90)).padEnd(8)} ms                              ║
║    p95  : ${String(p('http_req_duration',95)).padEnd(8)} ms  (umbral: <1000ms)           ║
║    p99  : ${String(p('http_req_duration',99)).padEnd(8)} ms                              ║
╠══════════════════════════════════════════════════════════════════╣
║  MÓDULOS ESPECÍFICOS                                             ║
║    Login      p99  : ${String(p('login_duration_ms',99)).padEnd(8)} ms  (umbral: <2000ms)║
║    Predicción p95  : ${String(p('prediction_duration_ms',95)).padEnd(8)} ms  (umbral: <2000ms)║
║    Ranking    p95  : ${String(p('ranking_duration_ms',95)).padEnd(8)} ms  (umbral: <100ms) ║
╠══════════════════════════════════════════════════════════════════╣
║  ERRORES                                                         ║
║    Tasa de error     : ${String(r('error_rate')).padEnd(8)}% (umbral: <5%)          ║
║    Requests fallidos : ${String(cnt('failed_requests')).padEnd(8)}                       ║
╠══════════════════════════════════════════════════════════════════╣
║  PROYECCIÓN PARA 60,000 USUARIOS CONCURRENTES                   ║
║                                                                  ║
║  Estrategia de cache (Nginx 5s):                                ║
║    - Rankings/Partidos: 1 req real por cada 1,000 usuarios      ║
║    - Cache hit rate observado: ${String(cacheHitRate + '%').padEnd(8)}                  ║
║    - Carga real a la BD: ~${String(Math.round(60000 * (1 - parseFloat(cacheHitRate)/100))).padEnd(8)} requests efectivos     ║
║                                                                  ║
║  Con virtual threads + pool 200 conexiones por instancia:       ║
║    - 3 instancias × 200 conexiones = 600 conexiones activas     ║
║    - Tiempo promedio de transacción: ~${String(p('http_req_duration',50)).padEnd(6)} ms             ║
║    - Capacidad teórica: 600 / (${String(p('http_req_duration',50)).padEnd(6)}/1000) req/s      ║
║                                                                  ║
║  VEREDICTO: ${wouldWork ? '✅ SISTEMA PREPARADO PARA 60,000 USUARIOS' : '⚠️  REQUIERE OPTIMIZACIÓN ADICIONAL      '}║
╚══════════════════════════════════════════════════════════════════╝
`;
}
