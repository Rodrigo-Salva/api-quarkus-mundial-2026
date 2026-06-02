#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  EC2 MANAGE — Gestión remota de la instancia
#
#  Uso (desde tu máquina local):
#    ./scripts/ec2-manage.sh status   IP_PUBLICA  KEY.pem
#    ./scripts/ec2-manage.sh logs     IP_PUBLICA  KEY.pem
#    ./scripts/ec2-manage.sh restart  IP_PUBLICA  KEY.pem
#    ./scripts/ec2-manage.sh update   IP_PUBLICA  KEY.pem
#    ./scripts/ec2-manage.sh ssh      IP_PUBLICA  KEY.pem
# ═══════════════════════════════════════════════════════════════

COMMAND=$1
IP=$2
KEY=$3

if [ -z "$COMMAND" ] || [ -z "$IP" ] || [ -z "$KEY" ]; then
    echo "Uso: $0 <comando> <ip-publica> <key.pem>"
    echo "Comandos: status | logs | restart | update | ssh"
    exit 1
fi

SSH="ssh -i $KEY -o StrictHostKeyChecking=no ec2-user@$IP"
REMOTE_DIR="/opt/mundial2026"

case $COMMAND in

    status)
        echo "=== Estado de los contenedores ==="
        $SSH "cd $REMOTE_DIR && docker-compose -f docker-compose.yml -f docker-compose.prod.yml ps"
        echo ""
        echo "=== Health check ==="
        curl -sf "http://$IP:80/health" && echo "✅ API OK" || echo "❌ API no responde"
        ;;

    logs)
        echo "=== Logs de api-1 (últimas 100 líneas) ==="
        $SSH "docker logs mundial_api_1 --tail=100 -f"
        ;;

    restart)
        echo "=== Reiniciando servicios ==="
        $SSH "cd $REMOTE_DIR && docker-compose -f docker-compose.yml -f docker-compose.prod.yml restart"
        echo "✅ Reiniciado"
        ;;

    update)
        echo "=== Actualizando la aplicación ==="
        # Descarga la nueva imagen y reinicia
        $SSH "cd $REMOTE_DIR && \
              docker-compose -f docker-compose.yml -f docker-compose.prod.yml pull && \
              docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d"
        echo "✅ Actualizado"
        ;;

    ssh)
        echo "=== Conectando por SSH ==="
        $SSH
        ;;

    *)
        echo "Comando no reconocido: $COMMAND"
        echo "Comandos disponibles: status | logs | restart | update | ssh"
        exit 1
        ;;
esac
