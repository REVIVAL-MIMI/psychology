# Deploy To Timeweb (HTTPS + WebRTC Calls)

This setup uses:
- `docker-compose.timeweb.yml`
- self-signed ("pseudo") HTTPS certificate
- `coturn` for stable voice/video calls

## 1. Prepare Server

1. Create a VPS in Timeweb (Ubuntu 22.04+).
2. Point your domain to VPS IP (`A` record).
3. Open ports in Timeweb firewall:
- `80/tcp`
- `443/tcp`
- `3478/tcp`
- `3478/udp`
- `49160-49200/udp`

## 2. Install Docker

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker
```

## 3. Upload Project

```bash
git clone <your-repo-url> psychology
cd psychology
```

## 4. Configure Environment

```bash
cp .env.timeweb.example .env.timeweb
```

Edit `.env.timeweb`:
- set `SERVER_DOMAIN`
- set `TURN_EXTERNAL_IP` (your VPS public IP)
- set `VITE_TURN_URL` with your domain
- set SMTP values for email OTP
- set strong DB password

## 5. Generate Pseudo Certificate

Run from project root:

```bash
./deploy/timeweb/generate-self-signed-cert.sh <your-domain> <your-server-ip>
```

Example:

```bash
./deploy/timeweb/generate-self-signed-cert.sh telecare.example.ru 203.0.113.10
```

Certificate files will be created in `deploy/timeweb/certs/`.

## 6. Start Services

```bash
docker compose --env-file .env.timeweb -f docker-compose.timeweb.yml up -d --build
```

Check status:

```bash
docker compose --env-file .env.timeweb -f docker-compose.timeweb.yml ps
```

## 7. Verify

- Open `https://<your-domain>`
- Browser will show certificate warning (self-signed): trust/allow it
- Login and test chat call

Check logs:

```bash
docker compose --env-file .env.timeweb -f docker-compose.timeweb.yml logs -f edge
docker compose --env-file .env.timeweb -f docker-compose.timeweb.yml logs -f backend
docker compose --env-file .env.timeweb -f docker-compose.timeweb.yml logs -f coturn
```

## Important Notes For Calls

- Camera/microphone in browser requires HTTPS.
- With self-signed cert, each client device must trust the certificate manually.
- `coturn` ports must stay open, otherwise some users will not connect in call.

## Update Deployment

```bash
git pull
docker compose --env-file .env.timeweb -f docker-compose.timeweb.yml up -d --build
```

## Stop / Remove

```bash
docker compose --env-file .env.timeweb -f docker-compose.timeweb.yml down
docker compose --env-file .env.timeweb -f docker-compose.timeweb.yml down -v
```
