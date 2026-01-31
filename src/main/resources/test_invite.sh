#!/bin/bash

echo "=== ДИАГНОСТИКА ПРОБЛЕМЫ С ИНВАЙТАМИ ==="

PHONE="+79161234560"
echo "Используемый телефон: $PHONE"

# 1. Проверяем всех психологов в базе
echo -e "\n1. Проверяем всех психологов:"
curl -X GET http://localhost:8080/api/v1/debug/psychologists

# 2. Проверяем аутентификацию
echo -e "\n\n2. Проверяем аутентификацию (нужен токен):"
echo "Введите ваш JWT токен: "
read TOKEN

curl -X GET http://localhost:8080/api/v1/debug/check-auth \
  -H "Authorization: Bearer $TOKEN"

# 3. Проверяем инвайты
echo -e "\n\n3. Проверяем существующие инвайты:"
curl -X GET http://localhost:8080/api/v1/debug/invites

# 4. Пробуем создать инвайт с детальным логированием
echo -e "\n\n4. Создаем инвайт:"
curl -X POST http://localhost:8080/api/v1/invites \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\n\nStatus: %{http_code}\n"