#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <dockerhub-username> [tag]"
  exit 1
fi

USERNAME="$1"
TAG="${2:-latest}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

LOCAL_IMAGE="tbg-care-allinone:local"
REMOTE_IMAGE="${USERNAME}/tbg-care-allinone:${TAG}"

cd "$ROOT_DIR"

echo "[1/7] Building backend jar..."
MAVEN_USER_HOME=.m2 ./mvnw -Dmaven.repo.local=.m2 -q -DskipTests package

echo "[2/7] Building frontend dist..."
(
  cd frontend
  npm run build
)

echo "[3/7] Preparing release context..."
mkdir -p docker-release/backend docker-release/frontend
cp target/*.jar docker-release/backend/app.jar
rm -rf docker-release/frontend/dist
cp -R frontend/dist docker-release/frontend/dist
cp frontend/nginx.conf docker-release/frontend/nginx.conf

echo "[4/7] Rebuilding base backend image for fresh app.jar..."
docker build -f Dockerfile.release-backend -t tbg-care-backend:local .

echo "[5/7] Building all-in-one image..."
docker build -f Dockerfile.all-in-one -t "$LOCAL_IMAGE" .

echo "[6/7] Tagging..."
docker tag "$LOCAL_IMAGE" "$REMOTE_IMAGE"

echo "[7/7] Pushing..."
docker push "$REMOTE_IMAGE"

cat <<EOF
Published:
- $REMOTE_IMAGE

Client run:
docker pull $REMOTE_IMAGE
docker run -d --name tbg-care -p 3000:80 -v tbg-care-data:/var/lib/postgresql -v tbg-care-redis:/data -v tbg-care-uploads:/app/uploads $REMOTE_IMAGE
EOF
