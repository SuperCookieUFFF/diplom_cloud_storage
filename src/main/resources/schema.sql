CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     login VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS auth_tokens (
                                           id BIGSERIAL PRIMARY KEY,
                                           user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS files (
                                     id BIGSERIAL PRIMARY KEY,
                                     filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    content_type VARCHAR(100),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

INSERT INTO users (login, password) VALUES
                                        ('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV2Ui.'),
                                        ('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV2Ui.')
    ON CONFLICT (login) DO NOTHING;