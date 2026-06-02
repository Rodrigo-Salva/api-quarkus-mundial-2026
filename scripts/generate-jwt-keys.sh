#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# Genera las claves JWT para el proyecto.
# Ejecutar UNA VEZ antes de levantar la API.
#
# Uso:
#   chmod +x scripts/generate-jwt-keys.sh
#   ./scripts/generate-jwt-keys.sh
# ═══════════════════════════════════════════════════════════════

KEYS_DIR="src/main/resources"

if [ -f "$KEYS_DIR/privateKey.pem" ]; then
  echo "⚠️  Las claves ya existen. Si quieres regenerarlas borra los .pem primero."
  exit 0
fi

echo "🔑 Generando claves RSA para JWT..."
openssl genrsa -out "$KEYS_DIR/privateKey.pem" 2048
openssl rsa -in "$KEYS_DIR/privateKey.pem" -pubout -out "$KEYS_DIR/publicKey.pem"

echo "✅ Claves generadas en $KEYS_DIR/"
echo "⚠️  IMPORTANTE: Estas claves están en .gitignore — nunca las subas al repositorio."
