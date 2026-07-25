CREATE TABLE admins (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE admin_login_logs (
    id           BIGSERIAL    PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL,
    success      BOOLEAN      NOT NULL,
    ip_address   VARCHAR(45),
    user_agent   VARCHAR(500),
    fail_reason  VARCHAR(255),
    attempted_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_login_logs_username ON admin_login_logs (username);
CREATE INDEX idx_admin_login_logs_attempted_at ON admin_login_logs (attempted_at);
