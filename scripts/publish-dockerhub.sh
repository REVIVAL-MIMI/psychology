#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <dockerhub-username> [tag]"
  exit 1
fi

USERNAME="$1"
TAG="${2:-latest}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BACKEND_LOCAL="tbg-care-backend:local"
FRONTEND_LOCAL="tbg-care-frontend:local"
BACKEND_REMOTE="${USERNAME}/tbg-care-backend:${TAG}"
FRONTEND_REMOTE="${USERNAME}/tbg-care-frontend:${TAG}"

cd "$ROOT_DIR"

echo "[1/6] Building backend jar..."
MAVEN_USER_HOME=.m2 ./mvnw -Dmaven.repo.local=.m2 -q -DskipTests package

echo "[2/6] Building frontend dist..."
(
  cd frontend
  npm run build
)

echo "[3/6] Preparing release context..."
mkdir -p docker-release/backend docker-release/frontend
cp target/*.jar docker-release/backend/app.jar
rm -rf docker-release/frontend/dist
cp -R frontend/dist docker-release/frontend/dist
cp frontend/nginx.conf docker-release/frontend/nginx.conf

echo "[4/6] Building release images..."
docker build -f Dockerfile.release-backend -t "$BACKEND_LOCAL" .
docker build -f Dockerfile.release-frontend -t "$FRONTEND_LOCAL" .

echo "[5/6] Tagging for Docker Hub..."
docker tag "$BACKEND_LOCAL" "$BACKEND_REMOTE"
docker tag "$FRONTEND_LOCAL" "$FRONTEND_REMOTE"

echo "[6/6] Pushing to Docker Hub..."
docker push "$BACKEND_REMOTE"
docker push "$FRONTEND_REMOTE"

cat <<EOF
Published:
- $BACKEND_REMOTE
- $FRONTEND_REMOTE

Client run commands:
docker pull $BACKEND_REMOTE
docker pull $FRONTEND_REMOTE
docker compose --env-file .env.docker -f docker-compose.hub.yml up -d
EOF
