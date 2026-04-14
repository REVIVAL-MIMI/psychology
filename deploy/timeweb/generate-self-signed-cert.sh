#!/usr/bin/env bash
set -euo pipefail

DOMAIN="${1:-localhost}"
SERVER_IP="${2:-}"
DAYS="${DAYS:-825}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="${SCRIPT_DIR}/certs"
mkdir -p "${CERT_DIR}"

SAN="DNS:${DOMAIN}"
if [[ -n "${SERVER_IP}" ]]; then
  SAN="${SAN},IP:${SERVER_IP}"
fi

openssl req -x509 -nodes -newkey rsa:2048 -sha256 \
  -days "${DAYS}" \
  -keyout "${CERT_DIR}/server.key" \
  -out "${CERT_DIR}/server.crt" \
  -subj "/CN=${DOMAIN}" \
  -addext "subjectAltName=${SAN}"

chmod 600 "${CERT_DIR}/server.key"
chmod 644 "${CERT_DIR}/server.crt"

echo "Self-signed certificate generated:"
echo "  ${CERT_DIR}/server.crt"
echo "  ${CERT_DIR}/server.key"
