# Шаг 1: Отправьте OTP
curl -X POST http://localhost:8080/api/v1/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"phone": "+79161234569"}'

# Шаг 2: Посмотрите OTP в логах IDEA

# Шаг 3: Зарегистрируйте психолога
curl -X POST http://localhost:8080/api/v1/auth/psychologist/register \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+79161234569",
    "otp": "ВСТАВЬТЕ_OTP_ЗДЕСЬ",
    "fullName": "Тест",
    "email": "test@test.com",
    "education": "Test",
    "specialization": "Test",
    "description": "Test"
  }'

# Шаг 4: Сохраните полученный accessToken

# Шаг 5: Проверьте токен
curl -X POST http://localhost:8080/api/v1/test/decode-token \
  -H "Authorization: Bearer ВАШ_ACCESS_TOKEN"

# Шаг 6: Создайте инвайт
curl -X POST http://localhost:8080/api/v1/invites \
  -H "Authorization: Bearer ВАШ_ACCESS_TOKEN" \
  -H "Content-Type: application/json"