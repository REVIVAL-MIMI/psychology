#!/bin/bash

echo "Проверка зависимостей для тестирования..."

DEPENDENCIES=("curl" "jq" "nc")

for dep in "${DEPENDENCIES[@]}"; do
    if command -v "$dep" &> /dev/null; then
        echo "✓ $dep установлен"
    else
        echo "✗ $dep не установлен"
    fi
done

echo ""
echo "Проверка доступности сервисов:"

# Проверка Redis
if nc -z localhost 6379 2>/dev/null; then
    echo "✓ Redis доступен на localhost:6379"
else
    echo "✗ Redis недоступен на localhost:6379"
fi

# Проверка PostgreSQL
if nc -z localhost 5432 2>/dev/null; then
    echo "✓ PostgreSQL доступен на localhost:5432"
else
    echo "✗ PostgreSQL недоступен на localhost:5432"
fi

# Проверка приложения
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✓ Spring Boot приложение доступно"
else
    echo "✗ Spring Boot приложение недоступно"
fi
