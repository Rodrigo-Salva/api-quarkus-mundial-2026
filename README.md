# Mundial 2026 — Predictions API

API REST para predicciones del Mundial de Fútbol 2026. Los usuarios predicen resultados, acumulan puntos y compiten en salas privadas con amigos.

## Inicio rápido

### Opción A — Solo quieres probarlo (sin código, solo Docker)

```bash
git clone https://github.com/tu-usuario/mundial2026-api.git
cd mundial2026-api
cp .env.example .env
chmod +x start.sh
./start.sh
```

La API queda corriendo en `http://localhost:80` con datos de prueba cargados.

---

### Opción B — Desarrollo local con hot reload

```bash
git clone https://github.com/tu-usuario/mundial2026-api.git
cd mundial2026-api
cp .env.example .env
chmod +x start.sh
./start.sh dev          # construye y levanta con 3 instancias
# O más simple para desarrollo:
docker-compose up -d    # solo infra
./mvnw quarkus:dev      # API con hot reload en :8080
```

Requiere: Java 21, Maven

---

## Credenciales de prueba

| Email | Password | Rol | Puntos |
|---|---|---|---|
| admin@mundial2026.com | Test1234! | ADMIN | — |
| rodrigo@test.com | Test1234! | USER | 18 pts |
| carlos@test.com | Test1234! | USER | 8 pts |
| maria@test.com | Test1234! | USER | 6 pts |

---

## URLs

| Servicio | URL |
|---|---|
| API (load balancer) | http://localhost:80 |
| Swagger UI | http://localhost:8080/q/swagger-ui |
| Health check | http://localhost:80/health |
| Kafka UI | http://localhost:8091 |

---

## Configuración (.env)

Copia `.env.example` a `.env` y edita los valores necesarios:

```env
DB_PASSWORD=tu_password_seguro
AI_PROVIDER=gemini
AI_GEMINI_API_KEY=tu_api_key_aqui
FRONTEND_URL=http://localhost:3000
```

Los valores por defecto funcionan para desarrollo local sin cambios.

---

## Comandos

```bash
./start.sh           # levantar en modo producción (imagen Docker Hub)
./start.sh dev       # levantar en modo desarrollo (build local)
./start.sh stop      # detener todo
./start.sh reset     # detener y borrar todos los datos

./mvnw test -Dquarkus.profile=test    # correr 76 tests
./mvnw quarkus:dev                    # desarrollo con hot reload

k6 run k6/smoke-test.js               # smoke test rápido
k6 run k6/stress-test.js              # stress test completo
```

---

## Flujo del sistema

```
ADMIN
  └── Crea partidos (POST /api/matches)

USUARIO
  ├── Se registra / loguea
  ├── Crea una sala (POST /api/leagues) → recibe código "ABC123"
  ├── Comparte QR: GET /api/leagues/{id}/invite/qr
  ├── Amigos se unen: POST /api/leagues/ABC123/join
  └── Predice partidos (POST /api/predictions)
       └── SOLO si está en al menos una sala

ADMIN registra resultado → automáticamente:
  ├── Calcula puntos (5 reglas)
  ├── Actualiza ranking global (Redis)
  ├── Actualiza ranking de cada sala
  └── Notifica por WebSocket a usuarios conectados
```

---

## Reglas de puntuación

| Regla | Puntos |
|---|---|
| Resultado exacto (ej: predice 2-1, termina 2-1) | 5 pts |
| Ganador correcto (acertó quién gana pero no el marcador) | 3 pts |
| Diferencia de goles correcta (mismo margen, mismo ganador) | 2 pts |
| Bonus por racha (3 partidos consecutivos acertando el ganador) | +2 pts |
| Predicción anticipada (registrada >24h antes del partido) | +1 pt |

---

## Despliegue en producción

### EC2 / VPS (más simple)

```bash
git clone https://github.com/tu-usuario/mundial2026-api.git
cd mundial2026-api
cp .env.example .env
# editar .env con credenciales reales
./start.sh
```

### Publicar imagen en Docker Hub (para que otros la usen sin código)

```bash
# 1. Construir
./mvnw package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm \
             -t tu-usuario/mundial2026-api:latest .

# 2. Publicar
docker push tu-usuario/mundial2026-api:latest

# 3. Cualquiera puede usarla así (sin clonar el repo):
DOCKER_IMAGE=tu-usuario/mundial2026-api:latest \
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### AWS ECS Fargate

```bash
# Subir imagen a ECR
aws ecr get-login-password | docker login --username AWS \
    --password-stdin TU_CUENTA.dkr.ecr.REGION.amazonaws.com

docker tag mundial2026-api:latest \
    TU_CUENTA.dkr.ecr.REGION.amazonaws.com/mundial2026-api:latest

docker push TU_CUENTA.dkr.ecr.REGION.amazonaws.com/mundial2026-api:latest

# Variables de entorno → AWS Secrets Manager
# PostgreSQL → Amazon RDS
# Redis      → Amazon ElastiCache
# Kafka      → Amazon MSK
```

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Framework | Quarkus 3.36.0 (Java 21) |
| Base de datos | PostgreSQL 16 + Flyway |
| Cache / Rankings | Redis 7 (Sorted Sets) |
| Mensajería | Apache Kafka |
| Historial | DynamoDB Local / AWS DynamoDB |
| Load Balancer | Nginx |
| Auth | JWT (SmallRye) |
| Docs | OpenAPI / Swagger UI |
| Tests | JUnit 5 + QuarkusTest (76 tests) |
| Stress test | k6 |
