#!/bin/bash

# Установщик тестового окружения

echo "Установка тестового окружения для Psychology Backend"

# Проверка и установка зависимостей
if ! command -v curl &> /dev/null; then
    echo "Установка curl..."
    sudo apt-get update && sudo apt-get install -y curl
fi

if ! command -v jq &> /dev/null; then
    echo "Установка jq для работы с JSON..."
    sudo apt-get install -y jq
fi

if ! command -v nc &> /dev/null; then
    echo "Установка netcat для проверки портов..."
    sudo apt-get install -y netcat
fi

# Создаем директорию для тестов
mkdir -p backend_tests
cd backend_tests

# Копируем тестовый скрипт
cat > test_backend.sh << 'EOF'
<вставить содержимое test_backend.sh выше>
EOF

# Делаем скрипт исполняемым
chmod +x test_backend.sh

# Создаем конфигурационный файл
cat > test_config.env << 'EOF'
# Конфигурация тестирования
BASE_URL="http://localhost:8080"
API_VERSION="v1"

# Тестовые данные
TEST_PSYCHOLOGIST_PHONE_PREFIX="+7999"
TEST_CLIENT_PHONE_PREFIX="+7998"

# Настройки тестирования
MAX_RETRIES=3
RETRY_DELAY=2
REQUEST_TIMEOUT=10

# Пути к логам
LOG_DIR="./logs"
REPORT_DIR="./reports"
EOF

# Создаем структуру директорий
mkdir -p logs reports test_data

# Создаем вспомогательные скрипты

# 1. Скрипт проверки зависимостей
cat > check_dependencies.sh << 'EOF'
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
EOF
chmod +x check_dependencies.sh

# 2. Скрипт генерации тестовых данных
cat > generate_test_data.sh << 'EOF'
#!/bin/bash

# Генератор тестовых данных

TIMESTAMP=$(date +%s)

echo "Генерация тестовых данных..."

# Генерация данных психолога
cat > test_data/psychologist_${TIMESTAMP}.json << PSYCH_EOF
{
  "phone": "+7999${TIMESTAMP}1",
  "otp": "123456",
  "fullName": "Тестовый Психолог ${TIMESTAMP}",
  "email": "psychologist.${TIMESTAMP}@test.com",
  "education": "МГУ, факультет психологии",
  "specialization": "Когнитивно-поведенческая терапия",
  "description": "Опыт работы 5 лет. Специализация: тревожные расстройства, депрессия.",
  "photoUrl": "https://example.com/psychologist_${TIMESTAMP}.jpg"
}
PSYCH_EOF

# Генерация данных клиента
cat > test_data/client_${TIMESTAMP}.json << CLIENT_EOF
{
  "phone": "+7998${TIMESTAMP}2",
  "otp": "123456",
  "fullName": "Тестовый Клиент ${TIMESTAMP}",
  "age": 30,
  "photoUrl": "https://example.com/client_${TIMESTAMP}.jpg"
}
CLIENT_EOF

# Генерация данных сессии
cat > test_data/session_${TIMESTAMP}.json << SESSION_EOF
{
  "clientId": null,
  "scheduledAt": "$(date -d '+2 days' --iso-8601=seconds)",
  "durationMinutes": 50,
  "description": "Тестовая сессия ${TIMESTAMP}"
}
SESSION_EOF

echo "✓ Тестовые данные сгенерированы в папке test_data/"
echo "  - psychologist_${TIMESTAMP}.json"
echo "  - client_${TIMESTAMP}.json"
echo "  - session_${TIMESTAMP}.json"
EOF
chmod +x generate_test_data.sh

# 3. Скрипт быстрого теста
cat > quick_test.sh << 'EOF'
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
EOF
chmod +x quick_test.sh

echo "✅ Установка завершена!"
echo ""
echo "Доступные скрипты:"
echo "  ./test_backend.sh        - Полный тест всех эндпоинтов"
echo "  ./quick_test.sh          - Быстрая проверка"
echo "  ./check_dependencies.sh  - Проверка зависимостей"
echo "  ./generate_test_data.sh  - Генерация тестовых данных"
echo ""
echo "Перед запуском убедитесь, что:"
echo "  1. Spring Boot приложение запущено"
echo "  2. Redis запущен (localhost:6379)"
echo "  3. PostgreSQL запущена (localhost:5432)"
echo ""
echo "Для запуска полного теста: ./test_backend.sh"