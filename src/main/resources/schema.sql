-- Cleanup existing tables to ensure schema changes are applied
DROP TABLE IF EXISTS authorization_codes CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS team_members CASCADE;
DROP TABLE IF EXISTS legal_entities CASCADE;
DROP TABLE IF EXISTS document_history CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS activity CASCADE;
DROP TABLE IF EXISTS teams CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE IF NOT EXISTS documents
(
    id         UUID PRIMARY KEY,
    title      VARCHAR(255),
    type       VARCHAR(100),
    status     VARCHAR(20) CHECK (status IN ('PENDING', 'COMPLETED', 'ERROR')),
    updated_at TIMESTAMP,
    text       TEXT,
    created_at TIMESTAMP,
    created_by BIGINT,
    version    INTEGER NOT NULL,
    team_id    UUID,
    user_id    BIGINT
);

CREATE TABLE IF NOT EXISTS document_history
(
    id          UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    content     TEXT,
    created_at TIMESTAMP,
    created_by BIGINT,
    version    INTEGER NOT NULL,
    FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS legal_entities
(
    id          UUID PRIMARY KEY,
    document_id UUID,
    details     JSONB,
    relevance   VARCHAR(255),
    created_at TIMESTAMP,
    created_by BIGINT,
    version    INTEGER NOT NULL,
    team_id     UUID,
    user_id     BIGINT,
    FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS activity
(
    id        UUID PRIMARY KEY,
    action    VARCHAR(255) NOT NULL,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users
(
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(255) UNIQUE NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255)        NOT NULL,
    enabled    BOOLEAN   DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    version    INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    role       VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS team_members
(
    id           UUID PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id),
    team_id      UUID NOT NULL,
    access_level VARCHAR(50)  NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    version    INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS teams
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    version    INTEGER NOT NULL
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

-- Indices and full-text search (already updated to correct types where necessary)
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents (status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents (created_at);
CREATE INDEX IF NOT EXISTS idx_document_history_document_id ON document_history (document_id);
CREATE INDEX IF NOT EXISTS idx_document_history_version ON document_history (document_id, version);
CREATE INDEX IF NOT EXISTS idx_legal_entities_document_id ON legal_entities (document_id);
CREATE INDEX IF NOT EXISTS idx_activity_date ON activity (created_at);
CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents (user_id);
CREATE INDEX IF NOT EXISTS idx_documents_team_id ON documents (team_id);
CREATE INDEX IF NOT EXISTS idx_legal_entities_user_id ON legal_entities (user_id);
CREATE INDEX IF NOT EXISTS idx_legal_entities_team_id ON legal_entities (team_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user_id ON team_members (user_id);
CREATE INDEX IF NOT EXISTS idx_team_members_team_id ON team_members (team_id);

-- Add full-text search support
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create GIN index for fast full-text search
CREATE INDEX IF NOT EXISTS idx_documents_search ON documents USING GIN (search_vector);

-- Create function to update search vector (using single quotes for R2DBC compatibility)
CREATE OR REPLACE FUNCTION documents_search_vector_update()
    RETURNS trigger AS
'
    BEGIN
        NEW.search_vector :=
                setweight(to_tsvector(''english'', COALESCE(NEW.title, '''')), ''A'') ||
                setweight(to_tsvector(''english'', COALESCE(NEW.text, '''')), ''B'');
        RETURN NEW;
    END;
' LANGUAGE plpgsql;

-- Create trigger to auto-update search vector on INSERT/UPDATE
DROP TRIGGER IF EXISTS documents_search_vector_trigger ON documents;
CREATE TRIGGER documents_search_vector_trigger
    BEFORE INSERT OR UPDATE
    ON documents
    FOR EACH ROW
EXECUTE FUNCTION documents_search_vector_update();

-- Update existing documents with search vectors
UPDATE documents
SET search_vector =
        setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(text, '')), 'B')
WHERE search_vector IS NULL;
