#!/bin/bash

# ============================================
# АВТОМАТИЧЕСКИЙ ТЕСТ BACKEND API
# Для Spring Boot приложения Psychology Service
# ============================================

# Конфигурация
BASE_URL="http://localhost:8080"
API_BASE="$BASE_URL/api/v1"
LOGFILE="backend_test_$(date +%Y%m%d_%H%M%S).log"
TIMEOUT=10
RETRY_COUNT=3
RETRY_DELAY=2

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Переменные для хранения данных
ACCESS_TOKEN=""
REFRESH_TOKEN=""
PSYCHOLOGIST_ID=""
CLIENT_ID=""
INVITE_TOKEN=""
SESSION_ID=""

# Функции утилиты
log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1" | tee -a "$LOGFILE"
}

success() {
    echo -e "${GREEN}✓ $1${NC}" | tee -a "$LOGFILE"
}

error() {
    echo -e "${RED}✗ $1${NC}" | tee -a "$LOGFILE"
}

warn() {
    echo -e "${YELLOW}⚠ $1${NC}" | tee -a "$LOGFILE"
}

run_with_retry() {
    local cmd="$1"
    local desc="$2"
    local max_retries=${3:-$RETRY_COUNT}
    local retry_delay=${4:-$RETRY_DELAY}

    for i in $(seq 1 $max_retries); do
        log "Попытка $i/$max_retries: $desc"
        if eval "$cmd"; then
            return 0
        fi

        if [ $i -lt $max_retries ]; then
            warn "Повтор через ${retry_delay}с..."
            sleep $retry_delay
        fi
    done

    return 1
}

check_service() {
    log "Проверка доступности сервиса..."

    if curl -s --max-time $TIMEOUT "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        success "Сервис доступен на $BASE_URL"
        return 0
    else
        error "Сервис не доступен на $BASE_URL"
        log "Убедитесь, что приложение запущено:"
        log "  mvn spring-boot:run"
        log "  или"
        log "  java -jar target/*.jar"
        return 1
    fi
}

# ============================================
# ТЕСТ 1: Публичные эндпоинты (без аутентификации)
# ============================================
test_public_endpoints() {
    log "\n=== ТЕСТ 1: Публичные эндпоинты ==="

    # Тест health-check
    log "Тест /api/v1/test/simple..."
    response=$(curl -s -w "\n%{http_code}" "$API_BASE/test/simple")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ] && [ "$body" = "OK" ]; then
        success "Test endpoint работает"
    else
        error "Test endpoint не работает: HTTP $http_code, тело: $body"
        return 1
    fi

    # Тест debug endpoints
    log "Тест debug endpoints..."

    # Проверка psychologists debug
    response=$(curl -s -w "\n%{http_code}" "$API_BASE/debug/psychologists")
    http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" = "200" ]; then
        success "Debug psychologists работает"
        # Выводим количество психологов
        count=$(echo "$response" | head -n -1 | jq '. | length' 2>/dev/null || echo "0")
        log "  Найдено психологов: $count"
    else
        warn "Debug psychologists вернул HTTP $http_code (возможно нет данных)"
    fi

    # Проверка invites debug
    response=$(curl -s -w "\n%{http_code}" "$API_BASE/debug/invites")
    http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" = "200" ]; then
        success "Debug invites работает"
    else
        warn "Debug invites вернул HTTP $http_code"
    fi

    return 0
}

# ============================================
# ТЕСТ 2: OTP и Аутентификация
# ============================================
test_otp_auth() {
    log "\n=== ТЕСТ 2: OTP и Аутентификация ==="

    # Генерация тестового номера телефона
    TEST_PHONE="+79991112233"

    log "Отправка OTP на номер: $TEST_PHONE..."

    # Отправка OTP (в логах появится код)
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/auth/send-otp" \
        -H "Content-Type: application/json" \
        -d "{\"phone\": \"$TEST_PHONE\"}")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        success "OTP отправлен успешно"
        # Извлекаем OTP из логов (симуляция)
        TEST_OTP="123456"  # В реальности нужно проверять логи Spring
        log "  Тестовый OTP (смотреть в логах Spring): $TEST_OTP"
        log "  ⚠ ВНИМАНИЕ: В продакшене OTP приходит по SMS"
    else
        error "Не удалось отправить OTP: HTTP $http_code"
        log "  Ответ: $body"
        return 1
    fi

    # Проверка верификации OTP (ожидаем ошибку, так как пользователь не зарегистрирован)
    log "Проверка верификации OTP (ожидаем ошибку 'User not found')..."

    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/auth/verify-otp" \
        -H "Content-Type: application/json" \
        -d "{\"phone\": \"$TEST_PHONE\", \"otp\": \"$TEST_OTP\"}")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if echo "$body" | grep -q "User not found"; then
        success "OTP верификация работает корректно (пользователь не найден, как и ожидалось)"
    else
        warn "Неожиданный ответ от verify-otp: HTTP $http_code"
        log "  Тело ответа: $body"
    fi

    return 0
}

# ============================================
# ТЕСТ 3: Регистрация психолога
# ============================================
test_psychologist_registration() {
    log "\n=== ТЕСТ 3: Регистрация психолога ==="

    # Генерация уникальных тестовых данных
    TEST_PHONE="+7999$(date +%H%M%S)1"
    TEST_EMAIL="psychologist_$(date +%s)@test.com"
    TEST_OTP="123456"

    log "Регистрация психолога:"
    log "  Телефон: $TEST_PHONE"
    log "  Email: $TEST_EMAIL"

    # Сначала отправляем OTP
    curl -s -X POST "$API_BASE/auth/send-otp" \
        -H "Content-Type: application/json" \
        -d "{\"phone\": \"$TEST_PHONE\"}" > /dev/null

    sleep 1  # Даем время на обработку

    # Регистрация психолога
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/auth/psychologist/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"phone\": \"$TEST_PHONE\",
            \"otp\": \"$TEST_OTP\",
            \"fullName\": \"Тестовый Психолог\",
            \"email\": \"$TEST_EMAIL\",
            \"education\": \"МГУ, факультет психологии\",
            \"specialization\": \"Когнитивно-поведенческая терапия\",
            \"description\": \"Опыт работы 5 лет\",
            \"photoUrl\": \"https://example.com/photo.jpg\"
        }")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "201" ]; then
        success "Психолог зарегистрирован успешно"

        # Сохраняем токены
        ACCESS_TOKEN=$(echo "$body" | jq -r '.accessToken' 2>/dev/null)
        REFRESH_TOKEN=$(echo "$body" | jq -r '.refreshToken' 2>/dev/null)
        PSYCHOLOGIST_ID=$(echo "$body" | jq -r '.userId' 2>/dev/null)

        if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
            success "Access Token получен: ${ACCESS_TOKEN:0:20}..."
            success "ID психолога: $PSYCHOLOGIST_ID"
            return 0
        else
            error "Не удалось извлечь токены из ответа"
            return 1
        fi
    else
        error "Ошибка регистрации психолога: HTTP $http_code"
        log "  Ответ: $body"
        return 1
    fi
}

# ============================================
# ТЕСТ 4: Защищенные эндпоинты психолога
# ============================================
test_protected_endpoints() {
    log "\n=== ТЕСТ 4: Защищенные эндпоинты ==="

    if [ -z "$ACCESS_TOKEN" ]; then
        error "Нет access token. Пропускаем тесты защищенных эндпоинтов"
        return 1
    fi

    # Тест получения профиля
    log "Тест получения профиля психолога..."

    response=$(curl -s -w "\n%{http_code}" "$API_BASE/profile" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        success "Профиль получен успешно"
        PSYCHOLOGIST_NAME=$(echo "$body" | jq -r '.fullName' 2>/dev/null)
        log "  Имя психолога: $PSYCHOLOGIST_NAME"
    else
        error "Ошибка получения профиля: HTTP $http_code"
        log "  Ответ: $body"
        return 1
    fi

    # Тест создания инвайта
    log "Тест создания инвайта..."

    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/invites" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        success "Инвайт создан успешно"
        INVITE_TOKEN=$(echo "$body" | jq -r '.token' 2>/dev/null)
        INVITE_LINK=$(echo "$body" | jq -r '.inviteLink' 2>/dev/null)

        if [ -n "$INVITE_TOKEN" ] && [ "$INVITE_TOKEN" != "null" ]; then
            log "  Токен инвайта: $INVITE_TOKEN"
            log "  Ссылка: $INVITE_LINK"
        else
            warn "Не удалось извлечь токен инвайта из ответа"
        fi
    else
        error "Ошибка создания инвайта: HTTP $http_code"
        log "  Ответ: $body"
        return 1
    fi

    # Тест получения списка инвайтов
    log "Тест получения списка инвайтов..."

    response=$(curl -s -w "\n%{http_code}" "$API_BASE/invites" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        success "Список инвайтов получен"
        invite_count=$(echo "$body" | jq '. | length' 2>/dev/null || echo "0")
        log "  Количество инвайтов: $invite_count"
    else
        error "Ошибка получения инвайтов: HTTP $http_code"
    fi

    return 0
}

# ============================================
# ТЕСТ 5: Валидация инвайта
# ============================================
test_invite_validation() {
    log "\n=== ТЕСТ 5: Валидация инвайта ==="

    if [ -z "$INVITE_TOKEN" ]; then
        warn "Нет инвайт токена. Пропускаем тест валидации"
        return 1
    fi

    log "Валидация инвайт токена: $INVITE_TOKEN"

    response=$(curl -s -w "\n%{http_code}" "$API_BASE/invites/validate/$INVITE_TOKEN")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        valid=$(echo "$body" | jq -r '.valid' 2>/dev/null)
        if [ "$valid" = "true" ]; then
            success "Инвайт валиден"
            psych_name=$(echo "$body" | jq -r '.psychologistName' 2>/dev/null)
            log "  Психолог: $psych_name"
        else
            error "Инвайт не валиден"
            return 1
        fi
    else
        error "Ошибка валидации инвайта: HTTP $http_code"
        log "  Ответ: $body"
        return 1
    fi

    return 0
}

# ============================================
# ТЕСТ 6: Refresh токен
# ============================================
test_refresh_token() {
    log "\n=== ТЕСТ 6: Обновление токена ==="

    if [ -z "$REFRESH_TOKEN" ]; then
        warn "Нет refresh token. Пропускаем тест обновления"
        return 1
    fi

    log "Обновление access token..."

    # Отправляем refresh token в cookie
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/auth/refresh" \
        -H "Content-Type: application/json" \
        -b "refreshToken=$REFRESH_TOKEN")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        success "Токен успешно обновлен"

        # Сохраняем новый access token
        NEW_ACCESS_TOKEN=$(echo "$body" | jq -r '.accessToken' 2>/dev/null)
        if [ -n "$NEW_ACCESS_TOKEN" ] && [ "$NEW_ACCESS_TOKEN" != "null" ]; then
            ACCESS_TOKEN="$NEW_ACCESS_TOKEN"
            log "  Новый Access Token: ${ACCESS_TOKEN:0:20}..."

            # Проверяем, что новый токен работает
            response=$(curl -s -w "\n%{http_code}" "$API_BASE/profile" \
                -H "Authorization: Bearer $ACCESS_TOKEN")

            http_code=$(echo "$response" | tail -n1)
            if [ "$http_code" = "200" ]; then
                success "Новый токен работает корректно"
            else
                error "Новый токен не работает: HTTP $http_code"
                return 1
            fi
        fi
    else
        error "Ошибка обновления токена: HTTP $http_code"
        log "  Ответ: $body"
        return 1
    fi

    return 0
}

# ============================================
# ТЕСТ 7: Выход из системы (logout)
# ============================================
test_logout() {
    log "\n=== ТЕСТ 7: Выход из системы ==="

    if [ -z "$ACCESS_TOKEN" ]; then
        warn "Нет access token. Пропускаем тест logout"
        return 1
    fi

    log "Выход из системы..."

    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/auth/logout" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -b "refreshToken=$REFRESH_TOKEN")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        success "Logout выполнен успешно"

        # Проверяем, что токен больше не работает
        log "Проверка инвалидации токена..."

        response=$(curl -s -w "\n%{http_code}" "$API_BASE/profile" \
            -H "Authorization: Bearer $ACCESS_TOKEN")

        http_code=$(echo "$response" | tail -n1)

        if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
            success "Токен успешно инвалидирован (HTTP $http_code)"
        else
            warn "Токен все еще работает после logout (HTTP $http_code)"
        fi
    else
        error "Ошибка logout: HTTP $http_code"
        log "  Ответ: $body"
        return 1
    fi

    return 0
}

# ============================================
# ТЕСТ 8: Админские эндпоинты
# ============================================
test_admin_endpoints() {
    log "\n=== ТЕСТ 8: Админские эндпоинты ==="

    # Создаем тестового администратора
    log "Тестирование админских эндпоинтов..."
    log "⚠ ВНИМАНИЕ: Для теста админских эндпоинтов нужен ADMIN токен"

    # Тест без токена (ожидаем 401/403)
    response=$(curl -s -w "\n%{http_code}" "$API_BASE/admin/psychologists/pending")
    http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        success "Админские эндпоинты защищены (HTTP $http_code без токена)"
    else
        warn "Неожиданный ответ от админского эндпоинта: HTTP $http_code"
    fi

    return 0
}

# ============================================
# ТЕСТ 9: Ошибки и граничные случаи
# ============================================
test_error_cases() {
    log "\n=== ТЕСТ 9: Обработка ошибок ==="

    # Неверный OTP
    log "Тест: Неверный OTP..."
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/auth/verify-otp" \
        -H "Content-Type: application/json" \
        -d '{"phone": "+79999999999", "otp": "wrong"}')

    http_code=$(echo "$response" | tail -n1)
    if [ "$http_code" = "401" ]; then
        success "Неверный OTP корректно отклоняется"
    else
        warn "Неверный OTP вернул HTTP $http_code (ожидалось 401)"
    fi

    # Несуществующий эндпоинт
    log "Тест: Несуществующий эндпоинт..."
    response=$(curl -s -w "\n%{http_code}" "$API_BASE/nonexistent")
    http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" = "404" ] || [ "$http_code" = "401" ]; then
        success "Несуществующий эндпоинт корректно обрабатывается"
    else
        warn "Несуществующий эндпоинт вернул HTTP $http_code"
    fi

    # Невалидный JSON
    log "Тест: Невалидный JSON..."
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/auth/send-otp" \
        -H "Content-Type: application/json" \
        -d '{invalid json}')

    http_code=$(echo "$response" | tail -n1)
    if [ "$http_code = "400" ] || [ "$http_code" = "500" ]; then
        success "Невалидный JSON корректно обрабатывается"
    else
        warn "Невалидный JSON вернул HTTP $http_code"
    fi

    return 0
}

# ============================================
# ТЕСТ 10: Сессии (требуется клиент)
# ============================================
test_sessions() {
    log "\n=== ТЕСТ 10: Управление сессиями ==="

    if [ -z "$ACCESS_TOKEN" ]; then
        warn "Нет access token. Пропускаем тест сессий"
        return 1
    fi

    log "Тест получения сессий психолога..."

    response=$(curl -s -w "\n%{http_code}" "$API_BASE/sessions/psychologist" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        success "Список сессий получен"
        session_count=$(echo "$body" | jq '. | length' 2>/dev/null || echo "0")
        log "  Количество сессий: $session_count"
    else
        warn "Ошибка получения сессий: HTTP $http_code"
        log "  Ответ: $body"
    fi

    return 0
}

# ============================================
# ГЛАВНАЯ ФУНКЦИЯ
# ============================================
main() {
    clear
    log "🚀 ЗАПУСК АВТОМАТИЧЕСКОГО ТЕСТИРОВАНИЯ BACKEND API"
    log "=================================================="
    log "Лог файл: $LOGFILE"
    log "Базовая URL: $BASE_URL"
    log "Таймаут запросов: ${TIMEOUT}с"
    log ""

    # Проверяем зависимости
    log "Проверка зависимостей..."

    if ! command -v curl &> /dev/null; then
        error "curl не установлен. Установите: sudo apt install curl"
        exit 1
    fi

    if ! command -v jq &> /dev/null; then
        warn "jq не установлен. Установите для лучшего парсинга JSON: sudo apt install jq"
    fi

    # Проверка доступности сервиса
    if ! check_service; then
        error "Сервис недоступен. Завершение тестирования."
        exit 1
    fi

    # Запуск тестов
    TESTS_PASSED=0
    TESTS_FAILED=0
    TESTS_SKIPPED=0

    # Массив тестов
    TESTS=(
        "test_public_endpoints"
        "test_otp_auth"
        "test_psychologist_registration"
        "test_protected_endpoints"
        "test_invite_validation"
        "test_refresh_token"
        "test_logout"
        "test_admin_endpoints"
        "test_error_cases"
        "test_sessions"
    )

    # Запускаем каждый тест
    for test_func in "${TESTS[@]}"; do
        log "\n▶ Запуск теста: ${test_func#test_}"

        if run_with_retry "$test_func" "Выполнение $test_func"; then
            success "Тест ${test_func#test_} пройден"
            ((TESTS_PASSED++))
        else
            # Проверяем, был ли тест пропущен
            if tail -n 5 "$LOGFILE" | grep -q "Пропускаем"; then
                warn "Тест ${test_func#test_} пропущен"
                ((TESTS_SKIPPED++))
            else
                error "Тест ${test_func#test_} не пройден"
                ((TESTS_FAILED++))
            fi
        fi

        sleep 1  # Пауза между тестами
    done

    # Итоговая статистика
    log "\n=================================================="
    log "📊 ИТОГОВАЯ СТАТИСТИКА ТЕСТИРОВАНИЯ"
    log "=================================================="
    log "Всего тестов: ${#TESTS[@]}"
    log "Пройдено: $TESTS_PASSED"
    log "Не пройдено: $TESTS_FAILED"
    log "Пропущено: $TESTS_SKIPPED"
    log ""

    if [ $TESTS_FAILED -eq 0 ]; then
        success "✅ ВСЕ ТЕСТЫ УСПЕШНО ПРОЙДЕНЫ!"
        log "Функционал работает корректно."
    else
        error "❌ НЕКОТОРЫЕ ТЕСТЫ НЕ ПРОЙДЕНЫ"
        log "Проверьте лог файл: $LOGFILE"
        log "Ищите '✗' для обнаружения ошибок."
    fi

    log "\nСохраненные данные теста:"
    log "  Psychologist ID: $PSYCHOLOGIST_ID"
    log "  Invite Token: $INVITE_TOKEN"
    log "  Access Token: ${ACCESS_TOKEN:0:30}..."
    log ""
    log "Лог сохранен в: $LOGFILE"

    exit $TESTS_FAILED
}