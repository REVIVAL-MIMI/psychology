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
