# Docker Deployment

## Published Images

- `rvivalmimi/tbg-care-allinone:latest`
- `rvivalmimi/tbg-care-backend:latest`
- `rvivalmimi/tbg-care-frontend:latest`

## Publish One-Container Image (Recommended)

1. Login to Docker Hub:

```bash
docker login
```

2. Build and publish single image:

```bash
./scripts/publish-dockerhub-allinone.sh <dockerhub-username> latest
```

After publish:

- `<dockerhub-username>/tbg-care-allinone:latest`

## Client Quick Start (One Container)

```bash
docker pull rvivalmimi/tbg-care-allinone:latest
docker run -d --name tbg-care \
  --restart unless-stopped \
  -p 3000:80 \
  -v tbg-care-db:/var/lib/postgresql \
  -v tbg-care-redis:/data \
  -v tbg-care-uploads:/app/uploads \
  rvivalmimi/tbg-care-allinone:latest
```

Open: `http://localhost:3000`

Stop:

```bash
docker stop tbg-care
```

Start again:

```bash
docker start tbg-care
```

Remove with data reset:

```bash
docker rm -f tbg-care
docker volume rm tbg-care-db tbg-care-redis tbg-care-uploads
```

## Publish Multi-Container Images

```bash
docker login
./scripts/publish-dockerhub.sh <dockerhub-username> latest
```

## Client Quick Start (Multi-Container Compose)

1. Create `.env.docker`:

```bash
cp .env.docker.example .env.docker
```

2. Set image tags inside `.env.docker`:

```env
BACKEND_IMAGE=<dockerhub-username>/tbg-care-backend:latest
FRONTEND_IMAGE=<dockerhub-username>/tbg-care-frontend:latest
```

3. Pull and start:

```bash
docker pull <dockerhub-username>/tbg-care-backend:latest
docker pull <dockerhub-username>/tbg-care-frontend:latest
docker compose --env-file .env.docker -f docker-compose.hub.yml up -d
```

4. Open:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`

## Client Run With Pure Docker Commands (Without Project Files)

```bash
docker network create tbg-care-net

docker volume create tbg-care-postgres-data
docker volume create tbg-care-redis-data
docker volume create tbg-care-uploads-data

docker pull postgres:15-alpine
docker pull redis:7-alpine
docker pull rvivalmimi/tbg-care-backend:latest
docker pull rvivalmimi/tbg-care-frontend:latest

docker run -d --name tbg-care-postgres --restart unless-stopped \
  --network tbg-care-net \
  -e POSTGRES_DB=psychology_db \
  -e POSTGRES_USER=psychology_user \
  -e POSTGRES_PASSWORD=psychology_pass \
  -v tbg-care-postgres-data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:15-alpine

docker run -d --name tbg-care-redis --restart unless-stopped \
  --network tbg-care-net \
  -v tbg-care-redis-data:/data \
  -p 6379:6379 \
  redis:7-alpine redis-server --appendonly yes

docker run -d --name tbg-care-backend --restart unless-stopped \
  --network tbg-care-net \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://tbg-care-postgres:5432/psychology_db \
  -e SPRING_DATASOURCE_USERNAME=psychology_user \
  -e SPRING_DATASOURCE_PASSWORD=psychology_pass \
  -e SPRING_DATA_REDIS_HOST=tbg-care-redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  -e APP_SEED_ENABLED=true \
  -e APP_ORGANIZATION_NAME='ООО «Телеком без границ»' \
  -e APP_PSYCHOLOGISTS_REQUIRE_VERIFICATION=false \
  -v tbg-care-uploads-data:/app/uploads \
  -p 8080:8080 \
  rvivalmimi/tbg-care-backend:latest

docker run -d --name tbg-care-frontend --restart unless-stopped \
  --network tbg-care-net \
  -p 3000:80 \
  rvivalmimi/tbg-care-frontend:latest
```

## Stop

```bash
docker compose --env-file .env.docker -f docker-compose.hub.yml down
```

## Remove Data

```bash
docker compose --env-file .env.docker -f docker-compose.hub.yml down -v
```
