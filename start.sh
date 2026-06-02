#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  START.SH — Mundial 2026 Predictions API
#  Levanta toda la aplicación con un solo comando.
#
#  Uso:
#    chmod +x start.sh
#    ./start.sh          # modo producción (imagen de Docker Hub)
#    ./start.sh dev      # modo desarrollo (construye local)
#    ./start.sh stop     # detiene todo
#    ./start.sh reset    # detiene y borra volúmenes (reset total)
# ═══════════════════════════════════════════════════════════════

set -e

MODE=${1:-prod}
COMPOSE_BASE="-f docker-compose.yml"

# ── Colores ───────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ── Stop ──────────────────────────────────────────────────────
if [ "$MODE" = "stop" ]; then
    info "Deteniendo todos los servicios..."
    docker-compose $COMPOSE_BASE down
    exit 0
fi

if [ "$MODE" = "reset" ]; then
    warn "Deteniendo y borrando todos los volúmenes (se perderán los datos)..."
    docker-compose $COMPOSE_BASE down -v
    exit 0
fi

# ── Verificar .env ────────────────────────────────────────────
if [ ! -f ".env" ]; then
    warn "No se encontró .env — copiando desde .env.example..."
    cp .env.example .env
    warn "Edita .env con tus credenciales reales antes de continuar."
    warn "Especialmente: DB_PASSWORD, AI_*_API_KEY"
    echo ""
fi

# ── Generar claves JWT si no existen ─────────────────────────
if [ ! -f "src/main/resources/privateKey.pem" ]; then
    info "Generando claves JWT RSA..."
    if command -v openssl &> /dev/null; then
        openssl genrsa -out src/main/resources/privateKey.pem 2048 2>/dev/null
        openssl rsa -in src/main/resources/privateKey.pem \
                    -pubout -out src/main/resources/publicKey.pem 2>/dev/null
        info "Claves JWT generadas."
    else
        error "openssl no encontrado. Instálalo o genera las claves manualmente."
    fi
fi

# ── Modo DESARROLLO ───────────────────────────────────────────
if [ "$MODE" = "dev" ]; then
    info "Modo DESARROLLO — construyendo imagen local..."

    if ! command -v java &> /dev/null; then
        error "Java no encontrado. Instala JDK 21+."
    fi

    info "Compilando con Maven..."
    ./mvnw package -DskipTests -q
    info "Build completado."

    info "Levantando servicios..."
    docker-compose $COMPOSE_BASE -f docker-compose.dev.yml up -d --build

# ── Modo PRODUCCIÓN ───────────────────────────────────────────
else
    info "Modo PRODUCCIÓN — usando imagen de Docker Hub..."
    info "Levantando servicios..."
    docker-compose $COMPOSE_BASE -f docker-compose.prod.yml up -d
fi

# ── Esperar que la API esté lista ─────────────────────────────
info "Esperando que la API esté lista..."
MAX_WAIT=120
WAITED=0
until curl -sf http://localhost:80/health > /dev/null 2>&1; do
    if [ $WAITED -ge $MAX_WAIT ]; then
        error "La API no respondió en ${MAX_WAIT}s. Revisa: docker-compose logs api-1"
    fi
    sleep 3
    WAITED=$((WAITED + 3))
    echo -n "."
done

echo ""
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ✅ Mundial 2026 API está corriendo!              ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo ""
echo -e "  🌐 API:        http://localhost:80"
echo -e "  📖 Swagger:    http://localhost:8080/q/swagger-ui"
echo -e "  ❤️  Health:     http://localhost:80/health"
echo -e "  📊 Kafka UI:   http://localhost:8091"
echo ""
echo -e "  👤 Admin:      admin@mundial2026.com / Test1234!"
echo -e "  👤 Usuario:    rodrigo@test.com / Test1234!"
echo ""
echo -e "  Para detener: ${YELLOW}./start.sh stop${NC}"
echo ""
