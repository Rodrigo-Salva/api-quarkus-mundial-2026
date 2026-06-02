# Propuesta de Arquitectura — Mundial 2026 Predictions API

---

## 0. Instalación y Ejecución

### Requisitos previos
- Docker Desktop instalado y corriendo
- Git

**Eso es todo. No necesitas Java, Maven ni ninguna otra dependencia.**

---

### Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/mundial2026-api.git
cd mundial2026-api
```

---

### Paso 2 — Configurar variables de entorno

```bash
cp .env.example .env
```

Abre `.env` y edita las credenciales que necesites:

```env
# Base de datos — cambia la contraseña si quieres
DB_USER=mundial_user
DB_PASSWORD=28demarzo

# IA — pon la API key del proveedor que vayas a usar (opcional)
AI_PROVIDER=gemini
AI_GEMINI_API_KEY=tu_api_key_aqui

# URL de tu frontend (para los QR de invitación)
FRONTEND_URL=http://localhost:3000
```

> Para desarrollo local los valores por defecto funcionan sin cambios.

---

### Paso 3 — Levantar la aplicación

```bash
chmod +x start.sh
./start.sh
```

El script hace automáticamente:
1. Genera las claves JWT (si no existen)
2. Descarga las imágenes de Docker
3. Levanta PostgreSQL, Redis, Kafka, DynamoDB y la API
4. Espera a que todo esté listo
5. Muestra las URLs de acceso

**Resultado esperado:**
```
═══════════════════════════════════════════════════
  ✅ Mundial 2026 API está corriendo!
═══════════════════════════════════════════════════

  🌐 API:        http://localhost:80
  📖 Swagger:    http://localhost:8080/q/swagger-ui
  ❤️  Health:     http://localhost:80/health
  📊 Kafka UI:   http://localhost:8091

  👤 Admin:      admin@mundial2026.com / Test1234!
  👤 Usuario:    rodrigo@test.com / Test1234!
```

---

### Paso 4 — Verificar que funciona

```bash
# Health check
curl http://localhost:80/health

# Login de prueba
curl -X POST http://localhost:80/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"rodrigo@test.com","password":"Test1234!"}'

# Ver partidos (sin token)
curl http://localhost:80/api/matches

# Ver ranking global (sin token)
curl http://localhost:80/api/rankings/global
```

O abre directamente el Swagger UI: `http://localhost:8080/q/swagger-ui`

---

### Paso 5 — Comandos útiles

```bash
./start.sh           # levantar (modo producción)
./start.sh dev       # levantar (modo desarrollo, build local)
./start.sh stop      # detener todos los servicios
./start.sh reset     # detener y borrar todos los datos (reset total)

./mvnw quarkus:dev   # desarrollo con hot reload en :8080
./mvnw test -Dquarkus.profile=test   # correr los 76 tests

k6 run k6/smoke-test.js    # verificación rápida
k6 run k6/stress-test.js   # stress test completo (requiere k6 instalado)
```

---

### Datos de prueba incluidos

Al arrancar, Flyway inserta automáticamente:

| Qué | Detalle |
|---|---|
| 6 usuarios | admin + 5 usuarios — contraseña: `Test1234!` |
| 13 partidos | 12 terminados + 6 programados + 1 en vivo |
| Predicciones | rodrigo (18pts), carlos (8pts), maria (6pts) |
| 2 salas | `OFIC26` y `BARRI0` con miembros |
| Wallets | 500 PEN cada usuario, 1000 PEN admin |
| KYC | admin y rodrigo aprobados |

---

### Despliegue en producción

#### Opción A — EC2 / VPS (igual que local)

```bash
# En tu servidor remoto
git clone https://github.com/tu-usuario/mundial2026-api.git
cd mundial2026-api
cp .env.example .env
nano .env              # editar con credenciales reales
./start.sh
```

#### Opción B — Publicar imagen en Docker Hub (cualquiera la usa sin código)

```bash
# Tú (una vez, cuando hagas release)
./mvnw package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm \
             -t tu-usuario/mundial2026-api:latest .
docker push tu-usuario/mundial2026-api:latest

# Cualquier persona (sin clonar el repo, solo 3 archivos)
curl -O https://raw.githubusercontent.com/tu-usuario/mundial2026-api/main/docker-compose.yml
curl -O https://raw.githubusercontent.com/tu-usuario/mundial2026-api/main/docker-compose.prod.yml
curl -O https://raw.githubusercontent.com/tu-usuario/mundial2026-api/main/.env.example
cp .env.example .env
# editar .env
DOCKER_IMAGE=tu-usuario/mundial2026-api:latest \
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

#### Opción C — AWS ECS Fargate

```bash
# 1. Subir imagen a ECR
aws ecr get-login-password | docker login --username AWS \
    --password-stdin TU_CUENTA.dkr.ecr.REGION.amazonaws.com

docker tag mundial2026-api:latest \
    TU_CUENTA.dkr.ecr.REGION.amazonaws.com/mundial2026-api:latest
docker push TU_CUENTA.dkr.ecr.REGION.amazonaws.com/mundial2026-api:latest

# 2. Variables de entorno → AWS Secrets Manager o ECS Task Definition
# 3. Servicios recomendados en AWS:
#    PostgreSQL → Amazon RDS (Multi-AZ para producción)
#    Redis      → Amazon ElastiCache
#    Kafka      → Amazon MSK
#    DynamoDB   → Amazon DynamoDB (sin -local)
```

---

## 1. Descripción General

Sistema backend para predicciones del Mundial de Fútbol 2026. Los usuarios registran predicciones de resultados, acumulan puntos según precisión y compiten en rankings globales y salas privadas.

**Tecnología principal:** Quarkus 3.36.0 (Java 21)  
**Patrón arquitectónico:** Monolito Modular  
**Base URL:** `http://localhost:80` (a través de Nginx)

---

## 2. Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENTE (Frontend)                       │
│                    Next.js / React / Mobile                     │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP / WebSocket
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    NGINX — Load Balancer                        │
│              Puerto 80 — Algoritmo: least_conn                  │
│         Rate limit: 20 req/s por IP — Burst: 40                 │
└──────────┬──────────────────┬──────────────────┬───────────────┘
           │                  │                  │
           ▼                  ▼                  ▼
    ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
    │   api-1     │   │   api-2     │   │   api-3     │
    │ Quarkus:8080│   │ Quarkus:8080│   │ Quarkus:8080│
    └──────┬──────┘   └──────┬──────┘   └──────┬──────┘
           └──────────────────┼──────────────────┘
                              │
          ┌───────────────────┼──────────────────┐
          │                   │                  │
          ▼                   ▼                  ▼
   ┌─────────────┐    ┌─────────────┐   ┌──────────────┐
   │ PostgreSQL  │    │    Redis    │   │    Kafka     │
   │   :5433     │    │   :6379     │   │    :9092     │
   │  Datos ACID │    │  Rankings   │   │   Eventos    │
   └─────────────┘    └─────────────┘   └──────────────┘
          │
          ▼
   ┌─────────────┐
   │  DynamoDB   │
   │   :8000     │
   │  Historial  │
   └─────────────┘
```

---

## 3. Patrón: Monolito Modular

Cada módulo es **autónomo** — tiene sus propias capas y es dueño de sus datos. La comunicación entre módulos se hace únicamente a través de sus Services, nunca entre repositorios.

```
src/main/java/com/mundial2026/predictions/
│
├── auth/           → Autenticación JWT
├── matches/        → Partidos del Mundial
├── predictions/    → Sistema de predicciones y puntuación ← CORE
├── rankings/       → Tabla de posiciones (Redis)
├── leagues/        → Salas/grupos privados
├── notifications/  → Notificaciones en tiempo real
├── realtime/       → WebSocket + estado en vivo
├── history/        → Historial de actividad (DynamoDB)
├── kyc/            → Verificación y juego responsable
├── wallet/         → Billetera simulada
├── betting/        → Apuestas con cuotas dinámicas
├── odds/           → Gestión de cuotas
├── ai/             → Estadísticas con IA
└── shared/         → Componentes transversales
```

**Estructura interna de cada módulo:**
```
{modulo}/
  controller/   → Endpoints REST (@Path)
  service/      → Lógica de negocio (@ApplicationScoped)
  repository/   → Acceso a datos (PanacheRepository)
  entity/       → Entidades JPA (@Entity)
  dto/          → Objetos de transferencia (records Java)
```

**Reglas de comunicación entre módulos:**
```
✅ PredictionService  →  MatchService.findById()       (via Service)
✅ BetService         →  WalletService.debitBet()      (via Service)
✅ BetService         →  KycService.isVerified()       (via Service)
❌ PredictionService  →  MatchRepository directamente  (prohibido)
❌ RankingResource    →  UserRepository directamente   (prohibido)
```

---

## 4. Módulo Core: Sistema de Predicciones

El módulo más importante del laboratorio. Implementa las 5 reglas de puntuación.

### 4.1 Flujo de una predicción

```
Usuario                API                  Base de datos
  │                     │                        │
  ├─ POST /api/predictions ──────────────────────┤
  │   { matchId, homeScore, awayScore }          │
  │                     │                        │
  │              Valida partido SCHEDULED        │
  │              Verifica no duplicado           │
  │              Calcula earlyBonus              │
  │                     ├── INSERT predictions ──┤
  │                     │                        │
  │◄── 201 Created ─────┤                        │
  │  { points:0, earlyBonus:1, settled:false }   │
```

### 4.2 Flujo de liquidación (cuando termina el partido)

```
ADMIN                  API                   Todos los usuarios
  │                     │                          │
  ├─ PUT /api/matches/{id}/result ─────────────────┤
  │   { homeScore:2, awayScore:1, status:FINISHED }│
  │                     │                          │
  │              Para cada predicción:             │
  │              ├── computeBasePoints()           │
  │              ├── computeEarlyBonus()           │
  │              └── recalculateStreakBonus()       │
  │                     │                          │
  │                     ├── UPDATE predictions ────┤
  │                     │   settled=true           │
  │                     │   totalPoints calculado  │
  │                     │                          │
  │◄── 200 OK ──────────┤                          │
```

### 4.3 Reglas de puntuación implementadas

```java
// PredictionService.java — computeBasePoints()

Regla 1: predice 2-1, termina 2-1         → 5 pts (exacto)
Regla 2: predice 3-0, termina 1-0         → 3 pts (ganador correcto)
Regla 3: predice 3-1, termina 2-0         → 2 pts (diferencia +2 == +2)
Regla 4: 3 consecutivos acertados         → +2 pts (bonus racha)
Regla 5: predicción >24h antes del match  → +1 pt  (anticipada)

totalPoints = points + earlyBonus + streakBonus
```

---

## 5. Selección de Tecnologías

### 5.1 PostgreSQL — Datos relacionales

**Por qué:** Los datos de usuarios, predicciones y ligas requieren ACID (atomicidad y consistencia), joins entre tablas y transacciones.

```
Tablas principales:
users           → datos de cuenta y perfil
matches         → partidos del mundial
predictions     → predicciones con puntos calculados
leagues         → salas privadas
league_members  → relación usuario-sala
wallets         → saldo simulado (billetera)
bets            → apuestas realizadas
```

### 5.2 Redis — Rankings en tiempo real

**Por qué:** El ranking global puede tener miles de usuarios. Una query SQL con ORDER BY sería lenta. Redis Sorted Sets (`ZADD`/`ZREVRANGE`) retornan el top N en O(log N) con latencia < 5ms.

```
Claves Redis:
ranking:global           → ranking mundial (todos)
ranking:country:{CODE}   → ranking por país (PER, ARG, BRA...)
live:match:{matchId}     → estado en vivo del partido (TTL: 4h)
```

### 5.3 Kafka — Mensajería asíncrona

**Por qué:** Cuando termina un partido, necesitamos notificar a múltiples sistemas (WebSocket, predicciones, rankings) sin acoplarlos. Kafka desacopla el productor del consumidor.

```
Topics:
match.events          → productor: MatchService
                        consumidor: MatchEventConsumer → Redis + WebSocket
predictions.submitted → predicciones enviadas
bets.placed           → apuestas colocadas
notifications.push    → notificaciones al usuario
```

### 5.4 DynamoDB Local — Historial y notificaciones

**Por qué:** El historial de actividad y notificaciones son datos append-only con acceso simple por `userId`. No necesitan joins — DynamoDB es más eficiente aquí que PostgreSQL.

```
Tablas DynamoDB:
match-events    → PK: matchId,  SK: timestamp#eventId
activity-log    → PK: userId,   SK: timestamp#actionId
notifications   → PK: userId,   SK: timestamp#notifId
```

### 5.5 JWT — Autenticación stateless

**Por qué:** Con 3 instancias de la API detrás de Nginx, no podemos usar sesiones en memoria (cada request puede ir a una instancia diferente). JWT es stateless — cualquier instancia valida el token sin comunicarse con las otras.

```
accessToken  → válido 15 minutos (uso en requests)
refreshToken → válido 7 días (para renovar el accessToken)
```

---

## 6. Escalamiento Horizontal

### 6.1 Estrategia

```
Puerto 80
    │
   Nginx (least_conn)
    ├── api-1:8080
    ├── api-2:8080
    └── api-3:8080
         │
    PostgreSQL (compartido) ← ACID garantiza consistencia
    Redis      (compartido) ← Sorted Sets son thread-safe
    Kafka      (compartido) ← Brokers distribuidos por diseño
```

### 6.2 Por qué funciona sin problemas de concurrencia

- **PostgreSQL:** transacciones con `PESSIMISTIC_WRITE` en operaciones críticas (wallet)
- **Redis:** operaciones atómicas (`ZADD`, `ZREVRANK`)
- **Kafka:** cada consumer group procesa el mensaje una sola vez
- **JWT:** stateless — no hay estado compartido entre instancias

### 6.3 Cómo escalar más

```bash
# Agregar más instancias en docker-compose sin cambiar código:
docker-compose up -d --scale api=5
```

---

## 7. API REST — Endpoints principales

| Módulo | Método | Endpoint | Auth | Descripción |
|---|---|---|---|---|
| Auth | POST | /api/auth/register | No | Crear cuenta |
| Auth | POST | /api/auth/login | No | Obtener token JWT |
| Matches | GET | /api/matches | No | Listar partidos |
| Matches | PUT | /api/matches/{id}/result | ADMIN | Registrar resultado |
| **Predictions** | **POST** | **/api/predictions** | **Sí** | **Enviar predicción** |
| **Predictions** | **GET** | **/api/predictions/me** | **Sí** | **Mis predicciones y puntos** |
| **Rankings** | **GET** | **/api/rankings/global** | **No** | **Tabla de posiciones** |
| **Rankings** | **GET** | **/api/rankings/me** | **Sí** | **Mi posición** |
| **Leagues** | **POST** | **/api/leagues** | **Sí** | **Crear sala** |
| **Leagues** | **POST** | **/api/leagues/{code}/join** | **Sí** | **Unirse con código** |
| **Leagues** | **GET** | **/api/leagues/{id}/ranking** | **Sí** | **Ranking de sala** |
| AI | GET | /api/ai/my-stats | Sí | Análisis personal con IA |
| AI | GET | /api/ai/suggest | No | Sugerencia de predicción |

> Documentación completa: `http://localhost:8080/q/swagger-ui`

---

## 8. WebSocket — Tiempo real

```
Cliente conecta: ws://localhost:80/ws/match/{matchId}
                           │
                    MatchSocket.java
                           │
                    MatchEventConsumer.java
                    (@Incoming "match-events-in")
                           │
                    Kafka topic: match.events
                           │
                    MatchService.updateResult()
                    (publica cuando ADMIN actualiza resultado)
```

---

## 9. Módulo de IA — Provider-agnostic

El módulo de IA está diseñado para cambiar de proveedor sin modificar código.

```
application.properties
    ai.provider=gemini     ← cambiar esta línea es suficiente

AiProviderSelector
    ├── GeminiProvider   (Google Gemini)
    ├── ChatGptProvider  (OpenAI)
    ├── ClaudeProvider   (Anthropic)
    └── GrokProvider     (xAI)
```

**Estadísticas disponibles:**
- `GET /api/ai/my-stats` — análisis personal del usuario
- `GET /api/ai/tournament` — tendencias del torneo
- `GET /api/ai/suggest?home=X&away=Y` — sugerencia de predicción

---

## 10. Pruebas

### 10.1 Tests unitarios e integración (JUnit 5 + Quarkus)

| Archivo | Tests | Cubre |
|---|---|---|
| PredictionServiceTest | 15 | Las 5 reglas de puntuación |
| AuthServiceTest | 3 | Registro, login, duplicados |
| RankingServiceTest | 3 | ZADD, ZREVRANGE, usuario sin puntos |
| LeagueServiceTest | 3 | Crear sala, unirse, duplicado |
| MatchServiceTest | 2 | Crear partido, listar |
| WalletServiceTest | 10 | Depósito, retiro, saldo insuficiente |
| BetServiceTest | 10 | Apuesta, liquidación, cancelación |
| KycServiceTest | 9 | Verificación, límites, auto-exclusión |
| OddsServiceTest | 16 | Mercados, cuotas dinámicas, jugadores |
| **Total** | **72** | **100% pasando** |

```bash
# Ejecutar todos los tests:
./mvnw test -Dquarkus.profile=test
```

### 10.2 Stress Testing (k6)

**Scripts disponibles en `k6/`:**

```
k6/
  smoke-test.js   → 1 VU, 30s — verificación básica
  stress-test.js  → 4 escenarios progresivos
```

**Escenarios del stress test:**

| Escenario | VUs | Duración | Propósito |
|---|---|---|---|
| Smoke | 1 | 30s | Verificar que la API responde |
| Load | 50 | 5min | Carga normal del torneo |
| Stress | 200 | 7min | Límite del sistema |
| Spike | 500 | 1min | Explosión de tráfico (inicio Mundial) |

**Criterios de éxito (thresholds):**
- 95% de requests < 500ms
- Tasa de errores < 5%
- 99% de logins < 1s
- Rankings (Redis) < 200ms en p95

```bash
# Ejecutar stress test completo:
k6 run k6/stress-test.js

# Solo smoke test:
k6 run k6/smoke-test.js

# Guardar resultados en JSON:
k6 run --out json=k6/results.json k6/stress-test.js
```

---

## 11. Cómo ejecutar el proyecto

> Ver sección **0. Instalación y Ejecución** al inicio de este documento para las instrucciones completas paso a paso.

```bash
# Resumen rápido:
git clone https://github.com/tu-usuario/mundial2026-api.git
cd mundial2026-api
cp .env.example .env
./start.sh                                    # ← un solo comando

# Desarrollo con hot reload:
docker-compose up -d                          # solo infra
./mvnw quarkus:dev                            # API en :8080

# Tests:
./mvnw test -Dquarkus.profile=test            # 76 tests

# Stress test:
k6 run k6/stress-test.js
```

---

## 12. Decisiones de diseño relevantes

| Decisión | Alternativa descartada | Razón |
|---|---|---|
| Monolito modular | Microservicios | Menor complejidad operacional para un lab educativo, misma separación de responsabilidades |
| Redis para ranking | Query SQL con ORDER BY | O(log N) vs O(N log N), latencia <5ms vs ~50ms con índice |
| JWT stateless | Sesiones en base de datos | Compatible con escalamiento horizontal sin sticky sessions |
| Flyway para migraciones | Hibernate DDL auto | Control versionado del schema, reproducible en cualquier entorno |
| DynamoDB para historial | PostgreSQL para todo | Historial es append-only, sin joins, acceso por userId — NoSQL más eficiente |
| Kafka para eventos | REST síncrono entre módulos | Desacoplamiento — MatchService no conoce a PredictionService |
| `least_conn` en Nginx | round-robin | Más justo cuando hay requests de duración variable (WebSocket) |
