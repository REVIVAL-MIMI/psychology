#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <dockerhub-username> [tag]"
  exit 1
fi

USERNAME="$1"
TAG="${2:-latest}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

REMOTE_IMAGE="${USERNAME}/tbg-care-allinone:${TAG}"
PLATFORMS="${PLATFORMS:-linux/amd64,linux/arm64}"
BUILDER_NAME="${BUILDER_NAME:-tbg-multi}"

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

echo "[4/7] Ensuring buildx builder..."
if ! docker buildx inspect "$BUILDER_NAME" >/dev/null 2>&1; then
  docker buildx create --name "$BUILDER_NAME" --driver docker-container --use
else
  docker buildx use "$BUILDER_NAME"
fi
docker buildx inspect --bootstrap >/dev/null

echo "[5/7] Building and pushing multi-platform image ($PLATFORMS)..."
docker buildx build \
  --platform "$PLATFORMS" \
  -f Dockerfile.all-in-one \
  -t "$REMOTE_IMAGE" \
  --push \
  .

echo "[6/7] Inspecting manifest..."
docker buildx imagetools inspect "$REMOTE_IMAGE"

echo "[7/7] Done."

cat <<EOF
Published:
- $REMOTE_IMAGE

Client run:
docker pull $REMOTE_IMAGE
docker run -d --name tbg-care -p 3000:80 -v tbg-care-data:/var/lib/postgresql -v tbg-care-redis:/data -v tbg-care-uploads:/app/uploads $REMOTE_IMAGE
EOF
