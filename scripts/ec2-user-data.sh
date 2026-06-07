#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  EC2 USER DATA — Mundial 2026 Predictions (Backend + Frontend)
#  Instancia: t3.medium — Amazon Linux 2023
#
#  INSTRUCCIONES (solo 3 pasos):
#  1. Copia este script completo
#  2. Pega en AWS → Launch Instance → Advanced Details → User data
#  3. Launch Instance — todo lo demás es automático (~15 min)
# ═══════════════════════════════════════════════════════════════

exec > /var/log/user-data.log 2>&1
echo "=== Iniciando instalacion Mundial 2026 === $(date)"

REPO_BACKEND="https://github.com/Rodrigo-Salva/api-quarkus-mundial-2026.git"
REPO_FRONTEND="https://github.com/Rodrigo-Salva/front-nextjs-mundial-2026.git"
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)

# ── 1. Sistema ────────────────────────────────────────────────
echo "--- Instalando dependencias del sistema..."
yum update -y
yum install -y docker git openssl java-21-amazon-corretto-devel

# Node.js 20 (para el frontend Next.js)
curl -fsSL https://rpm.nodesource.com/setup_20.x | bash -
yum install -y nodejs

echo "  Node: $(node -v) | npm: $(npm -v)"

# ── 2. Docker ─────────────────────────────────────────────────
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# ── 3. Docker Compose ─────────────────────────────────────────
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
     -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# ── 4. Clonar backend ─────────────────────────────────────────
echo "--- Clonando backend..."
cd /opt
git clone "$REPO_BACKEND" mundial2026
cd /opt/mundial2026

# ── 5. Generar claves JWT ─────────────────────────────────────
echo "--- Generando claves JWT..."
openssl genrsa -out src/main/resources/privateKey.pem 2048
openssl rsa -in src/main/resources/privateKey.pem \
            -pubout -out src/main/resources/publicKey.pem

# ── 6. Construir JAR ──────────────────────────────────────────
echo "--- Compilando backend (tarda ~5 min)..."
export HOME=/root
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
chmod +x mvnw
./mvnw package -DskipTests -q
echo "--- JAR listo"

# ── 7. Imagen Docker del backend ──────────────────────────────
echo "--- Construyendo imagen Docker..."
docker build -f src/main/docker/Dockerfile.jvm \
             -t mundial2026-api:latest . --quiet

# ── 8. Kafka UUID valido ──────────────────────────────────────
echo "--- Configurando Kafka..."
KAFKA_UUID=$(docker run --rm confluentinc/cp-kafka:7.6.0 \
             kafka-storage random-uuid 2>/dev/null)
sed -i "s/CLUSTER_ID: \"mundial2026-kafka-cluster-01\"/CLUSTER_ID: \"${KAFKA_UUID}\"/" \
    docker-compose.yml

# ── 9. Imagen en docker-compose.prod.yml ─────────────────────
sed -i 's|image: ${DOCKER_IMAGE:-.*}|image: mundial2026-api:latest|g' \
    docker-compose.prod.yml

# ── 10. Crear .env del backend ────────────────────────────────
echo "--- Creando .env del backend..."
cp .env.example .env

sed -i "s|DB_URL=jdbc:postgresql://localhost:5433|DB_URL=jdbc:postgresql://postgres:5432|g" .env
sed -i "s|DB_URL_TEST=jdbc:postgresql://localhost:5433|DB_URL_TEST=jdbc:postgresql://postgres:5432|g" .env
sed -i "s|REDIS_HOST=localhost|REDIS_HOST=redis|g" .env
sed -i "s|KAFKA_BROKERS=localhost:9092|KAFKA_BROKERS=kafka:9092|g" .env
sed -i "s|DYNAMODB_ENDPOINT=http://localhost:8000|DYNAMODB_ENDPOINT=http://dynamodb:8000|g" .env
sed -i "s|S3_ENDPOINT=http://localhost:9000|S3_ENDPOINT=http://minio:9000|g" .env
sed -i "s|S3_PUBLIC_URL=http://localhost:9000|S3_PUBLIC_URL=http://${PUBLIC_IP}:9000|g" .env
sed -i "s|CORS_ORIGINS=http://localhost:3000|CORS_ORIGINS=http://${PUBLIC_IP}:3000|g" .env
sed -i "s|FRONTEND_URL=http://localhost:3000|FRONTEND_URL=http://${PUBLIC_IP}:3000|g" .env

grep -v '^#' .env | grep -v '^[[:space:]]*$' | grep '=' > /tmp/.env.clean
mv /tmp/.env.clean .env

# ── 11. nginx.conf ────────────────────────────────────────────
printf 'worker_processes auto;\n\nevents {\n    worker_connections 10000;\n}\n\nhttp {\n    upstream predictions_api {\n        least_conn;\n        server mundial_api_1:8080;\n        server mundial_api_2:8080;\n        server mundial_api_3:8080;\n    }\n\n    server {\n        listen 80;\n\n        location / {\n            proxy_pass http://predictions_api;\n            proxy_http_version 1.1;\n            proxy_set_header Connection "";\n            proxy_set_header Host $host;\n            proxy_set_header X-Real-IP $remote_addr;\n        }\n\n        location /ws/ {\n            proxy_pass http://predictions_api;\n            proxy_http_version 1.1;\n            proxy_set_header Upgrade $http_upgrade;\n            proxy_set_header Connection "upgrade";\n            proxy_read_timeout 3600s;\n        }\n\n        location /nginx-health {\n            return 200 "ok";\n            add_header Content-Type text/plain;\n        }\n    }\n}\n' \
    > nginx/nginx.conf

# ── 12. Levantar backend ──────────────────────────────────────
chown -R ec2-user:ec2-user /opt/mundial2026
echo "--- Levantando backend con Docker Compose..."
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# ── 13. Esperar que el backend responda ───────────────────────
echo "--- Esperando que la API este lista..."
MAX=240
WAITED=0
until curl -sf http://localhost/health > /dev/null 2>&1; do
    [ $WAITED -ge $MAX ] && { echo "TIMEOUT backend — revisa: docker logs mundial_api_1"; break; }
    sleep 5; WAITED=$((WAITED+5))
    echo "  Backend: ${WAITED}s..."
done
echo "--- Backend listo en ${WAITED}s"

# ── 14. Clonar y configurar frontend ─────────────────────────
echo "--- Clonando frontend..."
cd /opt
git clone "$REPO_FRONTEND" frontend
cd /opt/frontend

# Variables de entorno del frontend apuntando al backend en EC2
cat > .env.local << EOF
BACKEND_URL=http://localhost:80
NEXT_PUBLIC_APP_URL=http://${PUBLIC_IP}:3000
EOF

# ── 15. Build del frontend ────────────────────────────────────
echo "--- Instalando dependencias del frontend..."
npm install --legacy-peer-deps

echo "--- Compilando frontend (tarda ~3 min)..."
npm run build

echo "--- Frontend compilado"

# ── 16. Levantar frontend con systemd ────────────────────────
cat > /etc/systemd/system/mundial2026-frontend.service << EOF
[Unit]
Description=Mundial 2026 Frontend
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/frontend
ExecStart=/usr/bin/npm start -- -p 3000
Restart=always
RestartSec=5
Environment=NODE_ENV=production
Environment=PORT=3000

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable mundial2026-frontend
systemctl start mundial2026-frontend

# ── 17. Arranque automático del backend ───────────────────────
cat > /etc/systemd/system/mundial2026.service << 'SYSTEMD'
[Unit]
Description=Mundial 2026 Backend
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

# ── 18. Permisos finales ──────────────────────────────────────
chown -R ec2-user:ec2-user /opt/frontend

# ── 19. Resumen ───────────────────────────────────────────────
echo ""
echo "================================================"
echo "  MUNDIAL 2026 - LISTO EN AWS"
echo "================================================"
echo ""
echo "  FRONTEND:  http://${PUBLIC_IP}:3000"
echo "  API:       http://${PUBLIC_IP}/health"
echo "  Swagger:   http://${PUBLIC_IP}/q/swagger-ui"
echo "  Kafka UI:  http://${PUBLIC_IP}:8091"
echo "  MinIO:     http://${PUBLIC_IP}:9001"
echo ""
echo "  Admin:     admin@mundial2026.com / Test1234!"
echo "  Usuario:   rodrigo@test.com / Test1234!"
echo ""
echo "  PUERTOS A ABRIR EN SECURITY GROUP:"
echo "  - 3000 (Frontend)"
echo "  - 80   (Backend API)"
echo "  - 8091 (Kafka UI)"
echo "================================================"
echo "=== Completado: $(date) ==="
