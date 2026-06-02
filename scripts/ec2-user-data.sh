#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  EC2 USER DATA — Mundial 2026 Predictions API
#  Instancia: t3.medium — Amazon Linux 2023
# ═══════════════════════════════════════════════════════════════

set -e
exec > /var/log/user-data.log 2>&1
echo "=== Iniciando instalacion Mundial 2026 API === $(date)"

# ── Configuracion ─────────────────────────────────────────────
REPO_URL="https://github.com/Rodrigo-Salva/api-quarkus-mundial-2026.git"
DB_PASSWORD="Rd2026$ecure#Api!"
AI_PROVIDER="gemini"
AI_API_KEY=""
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
S3_ACCESS_KEY="rs_minio_2026"
S3_SECRET_KEY="rs_s3key_Mnd26#"

# ── 1. Actualizar sistema ─────────────────────────────────────
echo "--- Actualizando sistema..."
yum update -y

# ── 2. Instalar Java 21 (para construir la imagen) ────────────
echo "--- Instalando Java 21..."
yum install -y java-21-amazon-corretto-devel

# ── 3. Instalar Docker ────────────────────────────────────────
echo "--- Instalando Docker..."
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# ── 4. Instalar Docker Compose ────────────────────────────────
echo "--- Instalando Docker Compose..."
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
     -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

# ── 5. Instalar Git y OpenSSL ─────────────────────────────────
echo "--- Instalando Git y OpenSSL..."
yum install -y git openssl

# ── 6. Clonar repositorio ─────────────────────────────────────
echo "--- Clonando repositorio..."
cd /opt
git clone "$REPO_URL" mundial2026
cd /opt/mundial2026

# ── 7. Generar claves JWT ─────────────────────────────────────
echo "--- Generando claves JWT..."
openssl genrsa -out src/main/resources/privateKey.pem 2048
openssl rsa -in src/main/resources/privateKey.pem \
            -pubout -out src/main/resources/publicKey.pem

# ── 8. Construir el JAR ───────────────────────────────────────
echo "--- Construyendo la aplicacion (Maven)..."
chmod +x mvnw
./mvnw package -DskipTests -q
echo "--- JAR construido correctamente"

# ── 9. Construir imagen Docker ────────────────────────────────
echo "--- Construyendo imagen Docker..."
docker build -f src/main/docker/Dockerfile.jvm \
             -t mundial2026-api:latest . --quiet
echo "--- Imagen construida correctamente"

# ── 10. Crear .env con credenciales ──────────────────────────
echo "--- Configurando variables de entorno..."
cat > /opt/mundial2026/.env << ENVFILE
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
AI_GEMINI_API_KEY=${AI_API_KEY}
CORS_ORIGINS=http://${PUBLIC_IP}
FRONTEND_URL=http://${PUBLIC_IP}:3000
DOCKER_IMAGE=mundial2026-api:latest
ENVFILE

# ── 11. Actualizar docker-compose.prod.yml con imagen local ──
sed -i "s|image: \${DOCKER_IMAGE:-.*}|image: mundial2026-api:latest|g" \
    /opt/mundial2026/docker-compose.prod.yml

# ── 12. Levantar servicios ────────────────────────────────────
echo "--- Levantando servicios..."
chmod +x start.sh
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# ── 13. Esperar que la API arranque ───────────────────────────
echo "--- Esperando que la API este lista..."
MAX=120
WAITED=0
until curl -sf http://localhost:80/health > /dev/null 2>&1; do
    if [ $WAITED -ge $MAX ]; then
        echo "WARN: API no respondio en ${MAX}s — revisa: docker logs mundial_api_1"
        break
    fi
    sleep 5
    WAITED=$((WAITED + 5))
    echo "  Esperando... ${WAITED}s"
done

# ── 14. Servicio systemd para arranque automatico ─────────────
cat > /etc/systemd/system/mundial2026.service << 'SYSTEMD'
[Unit]
Description=Mundial 2026 Predictions API
Requires=docker.service
After=docker.service network-online.target

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
chown -R ec2-user:ec2-user /opt/mundial2026

PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
echo ""
echo "=============================================="
echo "  MUNDIAL 2026 API - INSTALACION COMPLETADA"
echo "=============================================="
echo "  API:      http://${PUBLIC_IP}:80"
echo "  Swagger:  http://${PUBLIC_IP}:8080/q/swagger-ui"
echo "  MinIO:    http://${PUBLIC_IP}:9001"
echo "  Kafka UI: http://${PUBLIC_IP}:8091"
echo ""
echo "  Admin:    admin@mundial2026.com / Test1234!"
echo "  Usuario:  rodrigo@test.com / Test1234!"
echo "=============================================="
echo "=== Completado: $(date) ==="
