-- Создание таблицы администраторов (если нужно)
CREATE TABLE IF NOT EXISTS admins (
                                      id BIGSERIAL PRIMARY KEY,
                                      username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Вставка начального админа (пароль: admin123)
INSERT INTO admins (username, password_hash, email)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVpUi.', 'admin@psychology.com')
    ON CONFLICT (username) DO NOTHING;