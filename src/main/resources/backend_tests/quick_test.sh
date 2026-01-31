#!/bin/bash

# Быстрый тест основных эндпоинтов

echo "🚀 Быстрый тест API"
echo "=================="

BASE_URL="http://localhost:8080"
API_BASE="$BASE_URL/api/v1"

echo "1. Проверка health..."
curl -s "$BASE_URL/actuator/health" | jq '.status' || echo "Сервис недоступен"

echo ""
echo "2. Тест публичных эндпоинтов..."
curl -s "$API_BASE/test/simple"

echo ""
echo "3. Проверка debug endpoints..."
curl -s "$API_BASE/debug/health"

echo ""
echo "4. Проверка CORS headers..."
curl -s -I "$API_BASE/test/simple" | grep -i "access-control"

echo ""
echo "✅ Быстрый тест завершен"
