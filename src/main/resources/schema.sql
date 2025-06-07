
CREATE TABLE IF NOT EXISTS documents
(
    id         VARCHAR(36) PRIMARY KEY,
    title      VARCHAR(255),
    type       VARCHAR(100),
    status     VARCHAR(20) CHECK (status IN ('PENDING', 'COMPLETED', 'ERROR')),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS document_step_data
(
    id            VARCHAR(36) PRIMARY KEY,
    document_id   VARCHAR(36) NOT NULL,
    question_type VARCHAR(20) CHECK (question_type IN ('INTRO', 'OCCASIONS', 'PEOPLE', 'TIME')),
    answer        TEXT,
    FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS activities
(
    id     VARCHAR(36) PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    date   TIMESTAMP    NOT NULL,
    status VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS users
(
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(255) UNIQUE NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255)        NOT NULL,
    enabled    BOOLEAN   DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    role       VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS authorization_codes
(
    id           VARCHAR(255) PRIMARY KEY,
    code         VARCHAR(255) UNIQUE NOT NULL,
    client_id    VARCHAR(255)        NOT NULL,
    user_id      BIGINT              NOT NULL REFERENCES users (id),
    redirect_uri VARCHAR(1000)       NOT NULL,
    scopes       VARCHAR(1000),
    expires_at   TIMESTAMP           NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used         BOOLEAN   DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_documents_status ON documents (status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents (created_at);
CREATE INDEX IF NOT EXISTS idx_document_step_data_document_id ON document_step_data (document_id);
CREATE INDEX IF NOT EXISTS idx_activities_date ON activities (date);
