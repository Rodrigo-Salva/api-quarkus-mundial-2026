#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  EC2 USER DATA — Mundial 2026 Predictions API
#  Instancia: t3.medium — Amazon Linux 2023
# ═══════════════════════════════════════════════════════════════

exec > /var/log/user-data.log 2>&1
set -e
echo "=== Iniciando instalacion Mundial 2026 API === $(date)"

# ── EDITA ESTOS 3 VALORES ANTES DE PEGAR EN AWS ──────────────
DB_PASSWORD="CAMBIA_ESTE_PASSWORD"
S3_ACCESS_KEY="CAMBIA_ESTE_ACCESS_KEY"
S3_SECRET_KEY="CAMBIA_ESTE_SECRET_KEY"
# ─────────────────────────────────────────────────────────────

REPO_URL="https://github.com/Rodrigo-Salva/api-quarkus-mundial-2026.git"
AI_PROVIDER="gemini"
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)

# ── 1. Sistema ────────────────────────────────────────────────
echo "--- Actualizando sistema..."
yum update -y
yum install -y docker git openssl java-21-amazon-corretto-devel

# ── 2. Docker ─────────────────────────────────────────────────
echo "--- Configurando Docker..."
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# ── 3. Docker Compose ─────────────────────────────────────────
echo "--- Instalando Docker Compose..."
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
     -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# ── 4. Clonar repo ────────────────────────────────────────────
echo "--- Clonando repositorio..."
cd /opt
git clone "$REPO_URL" mundial2026
cd /opt/mundial2026

# ── 5. Generar claves JWT ─────────────────────────────────────
echo "--- Generando claves JWT..."
openssl genrsa -out src/main/resources/privateKey.pem 2048
openssl rsa -in src/main/resources/privateKey.pem \
            -pubout -out src/main/resources/publicKey.pem

# ── 6. Construir JAR ──────────────────────────────────────────
echo "--- Compilando con Maven (tarda 3-5 min)..."
export HOME=/root
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
chmod +x mvnw
./mvnw package -DskipTests -q
echo "--- JAR listo"

# ── 7. Construir imagen Docker ────────────────────────────────
echo "--- Construyendo imagen Docker..."
docker build -f src/main/docker/Dockerfile.jvm \
             -t mundial2026-api:latest . --quiet
echo "--- Imagen lista"

# ── 8. Generar CLUSTER_ID valido para Kafka ───────────────────
echo "--- Generando Kafka Cluster ID..."
KAFKA_UUID=$(docker run --rm confluentinc/cp-kafka:7.6.0 \
             kafka-storage random-uuid 2>/dev/null)
sed -i "s/CLUSTER_ID: \"mundial2026-kafka-cluster-01\"/CLUSTER_ID: \"${KAFKA_UUID}\"/" \
    /opt/mundial2026/docker-compose.yml

# ── 9. Reemplazar imagen en docker-compose.prod.yml ──────────
sed -i 's|image: ${DOCKER_IMAGE:-.*}|image: mundial2026-api:latest|g' \
    /opt/mundial2026/docker-compose.prod.yml

# ── 10. Crear .env completo ───────────────────────────────────
echo "--- Creando .env..."
cat > /opt/mundial2026/.env << ENVEOF
DB_USER=mundial_user
DB_PASSWORD=${DB_PASSWORD}
DB_URL=jdbc:postgresql://postgres:5432/mundial2026
DB_URL_TEST=jdbc:postgresql://postgres:5432/mundial2026_test
POSTGRES_DB=mundial2026
REDIS_HOST=redis
REDIS_PORT=6379
KAFKA_BROKERS=kafka:9092
AWS_REGION=us-east-1
DYNAMODB_ENDPOINT=http://dynamodb:8000
S3_ENDPOINT=http://minio:9000
S3_ACCESS_KEY=${S3_ACCESS_KEY}
S3_SECRET_KEY=${S3_SECRET_KEY}
S3_PATH_STYLE=true
S3_PUBLIC_URL=http://${PUBLIC_IP}:9000
S3_REGION=us-east-1
AI_PROVIDER=${AI_PROVIDER}
AI_GEMINI_API_KEY=not-configured
AI_CHATGPT_API_KEY=not-configured
AI_CLAUDE_API_KEY=not-configured
AI_GROK_API_KEY=not-configured
CORS_ORIGINS=http://${PUBLIC_IP}
FRONTEND_URL=http://${PUBLIC_IP}:3000
DOCKER_IMAGE=mundial2026-api:latest
ENVEOF

# ── 11. nginx.conf simplificado y sin variables bash ─────────
printf 'worker_processes auto;\n\nevents {\n    worker_connections 10000;\n}\n\nhttp {\n    upstream predictions_api {\n        least_conn;\n        server mundial_api_1:8080;\n        server mundial_api_2:8080;\n        server mundial_api_3:8080;\n    }\n\n    server {\n        listen 80;\n\n        location / {\n            proxy_pass http://predictions_api;\n            proxy_http_version 1.1;\n            proxy_set_header Connection "";\n            proxy_set_header Host $host;\n            proxy_set_header X-Real-IP $remote_addr;\n        }\n\n        location /ws/ {\n            proxy_pass http://predictions_api;\n            proxy_http_version 1.1;\n            proxy_set_header Upgrade $http_upgrade;\n            proxy_set_header Connection "upgrade";\n            proxy_read_timeout 3600s;\n        }\n\n        location /nginx-health {\n            return 200 "ok";\n            add_header Content-Type text/plain;\n        }\n    }\n}\n' \
    > /opt/mundial2026/nginx/nginx.conf

# ── 12. Levantar todos los servicios ─────────────────────────
echo "--- Levantando servicios..."
chown -R ec2-user:ec2-user /opt/mundial2026
cd /opt/mundial2026
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# ── 13. Esperar que la API responda ───────────────────────────
echo "--- Esperando que la API este lista (max 3 min)..."
MAX=180
WAITED=0
until curl -sf http://localhost/health > /dev/null 2>&1; do
    if [ $WAITED -ge $MAX ]; then
        echo "TIMEOUT: revisa con: docker logs mundial_api_1"
        break
    fi
    sleep 5
    WAITED=$((WAITED + 5))
    echo "  Esperando... ${WAITED}s"
done

# ── 14. Arranque automatico con systemd ───────────────────────
cat > /etc/systemd/system/mundial2026.service << 'SYSTEMD'
[Unit]
Description=Mundial 2026 API
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/mundial2026
ExecStart=/usr/local/bin/docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
ExecStop=/usr/local/bin/docker-compose -f docker-compose.yml -f docker-compose.prod.yml down
TimeoutStartSec=300

[Install]
WantedBy=multi-user.target
SYSTEMD

systemctl daemon-reload
systemctl enable mundial2026

PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
echo ""
echo "================================================"
echo "  MUNDIAL 2026 API - LISTA EN AWS"
echo "================================================"
echo "  API:      http://${PUBLIC_IP}/health"
echo "  Swagger:  http://${PUBLIC_IP}/q/swagger-ui"
echo "  MinIO:    http://${PUBLIC_IP}:9001"
echo "  Kafka UI: http://${PUBLIC_IP}:8091"
echo ""
echo "  Admin:    admin@mundial2026.com / Test1234!"
echo "  Usuario:  rodrigo@test.com / Test1234!"
echo "================================================"
echo "=== Completado: $(date) ==="
