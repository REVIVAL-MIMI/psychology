// Общие функции для всех страниц
const API_BASE = 'http://localhost:8080';

// Состояние приложения
let currentUser = null;
let accessToken = null;

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    // Загрузить сохраненного пользователя
    const savedUser = localStorage.getItem('currentUser');
    const savedToken = localStorage.getItem('accessToken');

    if (savedUser && savedToken) {
        currentUser = JSON.parse(savedUser);
        accessToken = savedToken;
        updateUIForLoggedInUser();
    }

    // Добавить текущий год в футер
    const yearElement = document.querySelector('.current-year');
    if (yearElement) {
        yearElement.textContent = new Date().getFullYear();
    }

    // Инициализация табов
    initTabs();
});

// Обновление UI для авторизованного пользователя
function updateUIForLoggedInUser() {
    const loginButtons = document.querySelectorAll('.login-btn');
    const userElements = document.querySelectorAll('.user-info');

    if (currentUser) {
        loginButtons.forEach(btn => {
            btn.style.display = 'none';
        });

        userElements.forEach(el => {
            el.style.display = 'block';
            el.innerHTML = `
                <span class="user-name">${currentUser.fullName || currentUser.phone}</span>
                <span class="user-role">${getRoleName(currentUser.userRole)}</span>
            `;
        });
    }
}

// Получение читаемого названия роли
function getRoleName(role) {
    const roles = {
        'ROLE_PSYCHOLOGIST': 'Психолог',
        'ROLE_CLIENT': 'Клиент',
        'ROLE_ADMIN': 'Администратор'
    };
    return roles[role] || role;
}

// Показать/скрыть статус
function showStatus(message, type = 'info', duration = 5000) {
    // Удалить старые статусы
    const oldStatus = document.querySelector('.status-message');
    if (oldStatus) oldStatus.remove();

    // Создать новый статус
    const status = document.createElement('div');
    status.className = `status-message status ${type}`;
    status.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
        <span>${message}</span>
    `;

    // Добавить на страницу
    document.body.appendChild(status);

    // Показать с анимацией
    setTimeout(() => {
        status.style.opacity = '1';
        status.style.transform = 'translateY(0)';
    }, 10);

    // Скрыть через duration миллисекунд
    if (duration > 0) {
        setTimeout(() => {
            hideStatus(status);
        }, duration);
    }

    return status;
}

function hideStatus(statusElement) {
    statusElement.style.opacity = '0';
    statusElement.style.transform = 'translateY(-20px)';
    setTimeout(() => {
        if (statusElement.parentNode) {
            statusElement.parentNode.removeChild(statusElement);
        }
    }, 300);
}

// Валидация номера телефона
function validatePhone(phone) {
    const phoneRegex = /^\+?[1-9]\d{1,14}$/;
    return phoneRegex.test(phone);
}

// Валидация email
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Форматирование даты
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Инициализация табов
function initTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const tabName = button.getAttribute('data-tab');

            // Убрать активный класс у всех кнопок и контента
            tabButtons.forEach(btn => btn.classList.remove('active'));
            tabContents.forEach(content => content.classList.remove('active'));

            // Добавить активный класс текущей кнопке и контенту
            button.classList.add('active');
            document.getElementById(tabName).classList.add('active');
        });
    });
}

// Загрузка данных пользователя
async function loadUserData() {
    if (!accessToken) return null;

    try {
        const response = await fetch(`${API_BASE}/api/v1/profile`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`
            }
        });

        if (response.ok) {
            const data = await response.json();
            currentUser = data;
            localStorage.setItem('currentUser', JSON.stringify(data));
            return data;
        }
    } catch (error) {
        console.error('Ошибка загрузки данных пользователя:', error);
    }

    return null;
}

// Выход из системы
function logout() {
    // Отправить запрос на выход (если доступен токен)
    if (accessToken) {
        fetch(`${API_BASE}/api/v1/auth/logout`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${accessToken}`
            }
        }).catch(console.error);
    }

    // Очистить локальное хранилище
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('currentUser');

    // Обновить состояние
    currentUser = null;
    accessToken = null;

    // Перенаправить на главную
    window.location.href = 'index.html';
}

// Показать/скрыть пароль
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    if (input.type === 'password') {
        input.type = 'text';
    } else {
        input.type = 'password';
    }
}

// Добавить CSS для статус-сообщений
const style = document.createElement('style');
style.textContent = `
    .status-message {
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 10000;
        padding: 16px 24px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        gap: 12px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        opacity: 0;
        transform: translateY(-20px);
        transition: all 0.3s ease;
    }
    
    .status-message i {
        font-size: 20px;
    }
    
    @media (max-width: 768px) {
        .status-message {
            left: 20px;
            right: 20px;
            top: 80px;
        }
    }
`;
document.head.appendChild(style);