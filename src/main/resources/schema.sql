-- Create documents table
CREATE TABLE IF NOT EXISTS documents
(
    id         VARCHAR(36) PRIMARY KEY,
    title      VARCHAR(255),
    type       VARCHAR(100),
    status     VARCHAR(20) CHECK (status IN ('PENDING', 'COMPLETED', 'ERROR')),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create document_step_data table
CREATE TABLE IF NOT EXISTS document_step_data
(
    id            VARCHAR(36) PRIMARY KEY,
    document_id   VARCHAR(36) NOT NULL,
    question_type VARCHAR(20) CHECK (question_type IN ('INTRO', 'OCCASIONS', 'PEOPLE', 'TIME')),
    answer        TEXT,
    FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

-- Create activities table
CREATE TABLE IF NOT EXISTS activities
(
    id     VARCHAR(36) PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    date   TIMESTAMP    NOT NULL,
    status VARCHAR(100) NOT NULL
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents (status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents (created_at);
CREATE INDEX IF NOT EXISTS idx_document_step_data_document_id ON document_step_data (document_id);
CREATE INDEX IF NOT EXISTS idx_activities_date ON activities (date);

-- Insert some sample data
INSERT INTO documents (id, title, type, status, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440001', 'Sample Document 1', 'contract', 'COMPLETED', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP),
       ('550e8400-e29b-41d4-a716-446655440002', 'Sample Document 2', 'report', 'PENDING', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP),
       ('550e8400-e29b-41d4-a716-446655440003', 'Sample Document 3', 'proposal', 'ERROR', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

INSERT INTO document_step_data (id, document_id, question_type, answer)
VALUES ('650e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'INTRO',
        'This is an introduction step'),
       ('650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'OCCASIONS',
        'For business meetings'),
       ('650e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440002', 'PEOPLE',
        'Team members and stakeholders');

INSERT INTO activities (id, action, date, status)
VALUES ('750e8400-e29b-41d4-a716-446655440001', 'Document created', CURRENT_TIMESTAMP, 'success'),
       ('750e8400-e29b-41d4-a716-446655440002', 'Document updated', CURRENT_TIMESTAMP, 'success'),
       ('750e8400-e29b-41d4-a716-446655440003', 'Document processing failed', CURRENT_TIMESTAMP, 'error');
