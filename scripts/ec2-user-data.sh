#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  EC2 USER DATA — Mundial 2026 Predictions API
#
#  Pega este script en:
#  AWS Console → EC2 → Launch Instance → Advanced Details → User data
#
#  Al lanzar la instancia, este script se ejecuta automáticamente
#  y deja la API corriendo sin que toques nada.
#
#  Instancia recomendada: t3.medium (2 vCPU, 4 GB RAM)
#  AMI recomendada:       Amazon Linux 2023
# ═══════════════════════════════════════════════════════════════

set -e
exec > /var/log/user-data.log 2>&1
echo "=== Iniciando instalación Mundial 2026 API === $(date)"

# ── Variables — EDITA ESTOS VALORES ANTES DE USAR ────────────
REPO_URL="https://github.com/TU_USUARIO/mundial2026-api.git"
DB_PASSWORD="CAMBIA_ESTA_PASSWORD_SEGURA"
AI_PROVIDER="gemini"
AI_API_KEY=""
FRONTEND_URL="http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)"
DOCKER_IMAGE="TU_USUARIO/mundial2026-api:latest"

# ─────────────────────────────────────────────────────────────

# 1. Actualizar el sistema
echo "--- Actualizando sistema..."
yum update -y

# 2. Instalar Docker
echo "--- Instalando Docker..."
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# 3. Instalar Docker Compose
echo "--- Instalando Docker Compose..."
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
     -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

# 4. Instalar Git y OpenSSL
echo "--- Instalando Git y OpenSSL..."
yum install -y git openssl

# 5. Clonar el repositorio
echo "--- Clonando repositorio..."
cd /opt
git clone "$REPO_URL" mundial2026
cd /opt/mundial2026

# 6. Crear el .env con las credenciales
echo "--- Configurando variables de entorno..."
cat > /opt/mundial2026/.env << EOF
# Base de datos
DB_USER=mundial_user
DB_PASSWORD=${DB_PASSWORD}
DB_URL=jdbc:postgresql://localhost:5433/mundial2026
DB_URL_TEST=jdbc:postgresql://localhost:5433/mundial2026_test

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BROKERS=localhost:9092

# DynamoDB Local
AWS_REGION=us-east-1
DYNAMODB_ENDPOINT=http://localhost:8000

# Storage MinIO
S3_ENDPOINT=http://localhost:9000
S3_ACCESS_KEY=mundial_admin
S3_SECRET_KEY=mundial_secret_123
S3_PATH_STYLE=true
S3_PUBLIC_URL=http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):9000
S3_REGION=us-east-1

# IA
AI_PROVIDER=${AI_PROVIDER}
AI_GEMINI_API_KEY=${AI_API_KEY}

# Frontend
CORS_ORIGINS=${FRONTEND_URL}
FRONTEND_URL=${FRONTEND_URL}

# Docker image
DOCKER_IMAGE=${DOCKER_IMAGE}
EOF

# 7. Generar claves JWT
echo "--- Generando claves JWT..."
openssl genrsa -out /opt/mundial2026/src/main/resources/privateKey.pem 2048
openssl rsa -in /opt/mundial2026/src/main/resources/privateKey.pem \
            -pubout -out /opt/mundial2026/src/main/resources/publicKey.pem

# 8. Dar permisos
chown -R ec2-user:ec2-user /opt/mundial2026
chmod +x /opt/mundial2026/start.sh

# 9. Levantar la aplicación
echo "--- Levantando la aplicación..."
cd /opt/mundial2026
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 10. Crear servicio systemd para que arranque con la instancia
echo "--- Configurando arranque automático..."
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

echo ""
echo "=== ✅ Instalación completada === $(date)"
echo "=== API disponible en: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):80"
echo "=== Swagger UI:        http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080/q/swagger-ui"
echo "=== MinIO consola:     http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):9001"
