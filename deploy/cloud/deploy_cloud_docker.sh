#!/bin/bash
# ==============================================================================
# SyntricDB One-Click Universal Cloud Docker Deployment Script
# Works on AWS EC2, DigitalOcean, GCP Compute, Hetzner, Azure, or any Linux VM
# ==============================================================================
set -e

PORT=${SYNTRICDB_PORT:-8080}
ADMIN_USER=${SYNTRICDB_ADMIN_USER:-admin}
ADMIN_PASS=${SYNTRICDB_ADMIN_PASSWORD:-syntricdb_secret_pass}
IMAGE=${SYNTRICDB_IMAGE:-ghcr.io/upendra-manike/syntricdb:latest}

echo "=========================================================================="
echo "⚡ Starting SyntricDB Automated Cloud Deployment..."
echo "=========================================================================="

# 1. Install Docker if missing
if ! command -v docker &> /dev/null; then
    echo "📦 Installing Docker Engine..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable --now docker
fi

# 2. Stop and remove existing container if running
if [ $(docker ps -a -q -f name=syntricdb-server) ]; then
    echo "🧹 Removing old SyntricDB container instance..."
    docker stop syntricdb-server || true
    docker rm syntricdb-server || true
fi

# 3. Pull latest image or build locally if image is local
echo "📥 Fetching SyntricDB Docker image ($IMAGE)..."
docker pull "$IMAGE" || echo "⚠️ Warning: Could not pull remote image, attempting local image execution."

# 4. Start Production Container
echo "🚀 Launching SyntricDB Container on port $PORT..."
docker run -d \
  --name syntricdb-server \
  --restart unless-stopped \
  -p "$PORT":8080 \
  -e SYNTRICDB_BIND_ADDRESS=0.0.0.0 \
  -e SYNTRICDB_PORT=8080 \
  -e SYNTRICDB_AUTH_ENABLED=true \
  -e SYNTRICDB_ADMIN_USER="$ADMIN_USER" \
  -e SYNTRICDB_ADMIN_PASSWORD="$ADMIN_PASS" \
  -e SYNTRICDB_DATA_DIR=/var/lib/syntricdb/data \
  -v syntricdb_data:/var/lib/syntricdb \
  "$IMAGE"

echo ""
echo "=========================================================================="
echo "🎉 SyntricDB Cloud Database Deployed Successfully!"
echo "=========================================================================="
echo "🔑 Admin Credentials : Username: $ADMIN_USER | Password: $ADMIN_PASS"
echo "🌐 Web Dashboard     : http://$(curl -s https://api.ipify.org || echo "YOUR_SERVER_IP"):$PORT/"
echo "📡 REST Query API   : POST http://$(curl -s https://api.ipify.org || echo "YOUR_SERVER_IP"):$PORT/api/sql"
echo "=========================================================================="
