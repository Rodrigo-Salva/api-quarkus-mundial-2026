/**
 * SMOKE TEST — verificación rápida de endpoints críticos
 * Ejecución: k6 run k6/smoke-test.js
 */

import http from 'k6/http';
import { check, group } from 'k6';

export const options = {
    vus: 1,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(99)<1000'],
        http_req_failed:   ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';

export default function () {
    group('Health checks', () => {
        check(http.get(`${BASE_URL}/health`),       { 'health UP':   (r) => r.status === 200 });
        check(http.get(`${BASE_URL}/nginx-health`), { 'nginx UP':    (r) => r.status === 200 });
        check(http.get(`${BASE_URL}/q/openapi`),    { 'openapi 200': (r) => r.status === 200 });
    });

    group('Public endpoints', () => {
        check(http.get(`${BASE_URL}/api/matches`),          { 'matches 200':  (r) => r.status === 200 });
        check(http.get(`${BASE_URL}/api/rankings/global`),  { 'ranking 200':  (r) => r.status === 200 });
        check(http.get(`${BASE_URL}/api/odds/match/1`),     { 'odds 200/404': (r) => [200, 404].includes(r.status) });
    });
}
