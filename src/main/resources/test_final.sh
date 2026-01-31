#!/bin/bash

echo "=== ОБНОВЛЕННЫЙ ТЕСТ СИСТЕМЫ ==="

echo -e "\n1. Проверяем здоровье системы:"
curl -X GET http://localhost:8080/api/v1/debug/health

echo -e "\n\n2. Проверяем всех психологов:"
curl -X GET http://localhost:8080/api/v1/debug/psychologists

echo -e "\n\n3. Проверяем все инвайты:"
curl -X GET http://localhost:8080/api/v1/debug/invites

echo -e "\n\n4. Проверяем валидность созданного инвайта:"
curl -X GET "http://localhost:8080/api/v1/invites/validate/QS65WxjoNsHPEmIOptfVu8cH8Weh7ubP"

echo -e "\n\n5. Получаем инвайты психолога (с токеном):"
TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIrNzkxNjEyMzQ1NjAiLCJpYXQiOjE3Njk4ODIxMzksImV4cCI6MTc2OTg4MzkzOX0.zWbR8yh_ZVoQWnwfQO86VYYqIJJkkZCH1CTpHT_XYvE"
curl -X GET http://localhost:8080/api/v1/invites \
  -H "Authorization: Bearer $TOKEN"

echo -e "\n\n6. Создаем новый инвайт:"
curl -X POST http://localhost:8080/api/v1/invites \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

echo -e "\n\n=== ТЕСТ ЗАВЕРШЕН ==="