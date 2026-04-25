-- Cleanup existing tables to ensure schema changes are applied
DROP TABLE IF EXISTS event_participants CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS case_documents CASCADE;
DROP TABLE IF EXISTS case_parties CASCADE;
DROP TABLE IF EXISTS authorization_codes CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS team_members CASCADE;
DROP TABLE IF EXISTS legal_entities CASCADE;
DROP TABLE IF EXISTS document_history CASCADE;
DROP TABLE IF EXISTS storage_files CASCADE;
DROP TABLE IF EXISTS document_groups CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS cases CASCADE;
DROP TABLE IF EXISTS activity CASCADE;
DROP TABLE IF EXISTS teams CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS document_examples CASCADE;

CREATE TABLE IF NOT EXISTS users
(
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(255) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password        VARCHAR(255),
    cognito_sub     VARCHAR(255) UNIQUE,
    email_verified  BOOLEAN   DEFAULT FALSE,
    enabled         BOOLEAN   DEFAULT TRUE,
    superadmin      BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    version         INTEGER             NOT NULL
);

CREATE TABLE IF NOT EXISTS teams
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    version    INTEGER      NOT NULL,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS document_examples
(
    id          UUID PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    questions   JSONB,
    base        JSONB,
    requirements JSONB,
    template_files JSONB,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    search_vector tsvector
);

CREATE TABLE IF NOT EXISTS document_groups
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255),
    description TEXT,
    updated_at  TIMESTAMP,
    created_at  TIMESTAMP,
    created_by  BIGINT,
    version     INTEGER NOT NULL,
    team_id     UUID REFERENCES teams (id),
    owner_id    BIGINT REFERENCES users (id),
    case_id     UUID,
    deleted_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS documents
(
    id                UUID PRIMARY KEY,
    title             VARCHAR(255),
    type              VARCHAR(100),
    status            VARCHAR(20) CHECK (status IN ('PENDING', 'COMPLETED', 'ERROR')),
    updated_at        TIMESTAMP,
    text              TEXT,
    created_at        TIMESTAMP,
    created_by        BIGINT,
    version           INTEGER NOT NULL,
    team_id           UUID REFERENCES teams (id),
    owner_id          BIGINT REFERENCES users (id),
    document_group_id UUID REFERENCES document_groups (id) ON DELETE SET NULL,
    deleted_at        TIMESTAMP
);

CREATE TABLE IF NOT EXISTS document_history
(
    id              UUID PRIMARY KEY,
    document_id     UUID    NOT NULL,
    content         TEXT,
    created_at      TIMESTAMP,
    created_by      BIGINT,
    version         INTEGER NOT NULL,
    history_version INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS storage_files
(
    id                UUID PRIMARY KEY,
    name              VARCHAR(255),
    file_type         VARCHAR(20) CHECK (file_type IN ('PDF', 'JPG', 'JPEG', 'PNG', 'GIF', 'TXT', 'DOC', 'DOCX', 'XLS', 'XLSX', 'OTHER')),
    document_group_id UUID REFERENCES document_groups (id) ON DELETE CASCADE,
    file_size         BIGINT,
    updated_at        TIMESTAMP,
    created_at        TIMESTAMP,
    created_by        BIGINT,
    version           INTEGER NOT NULL,
    team_id           UUID REFERENCES teams (id),
    owner_id          BIGINT REFERENCES users (id),
    deleted_at        TIMESTAMP
);

CREATE TABLE IF NOT EXISTS legal_entities
(
    id          UUID PRIMARY KEY,
    document_id UUID,
    details     JSONB,
    relevance   VARCHAR(255),
    created_at  TIMESTAMP,
    created_by  BIGINT,
    version     INTEGER NOT NULL,
    team_id     UUID REFERENCES teams (id),
    owner_id    BIGINT REFERENCES users (id),
    deleted_at  TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cases
(
    id               UUID PRIMARY KEY,
    title            VARCHAR(255),
    description      TEXT,
    status           VARCHAR(20) CHECK (status IN ('OPEN', 'IN_PROGRESS', 'CLOSED', 'ARCHIVED')),
    leading_court_id UUID REFERENCES legal_entities (id),
    updated_at       TIMESTAMP,
    created_at       TIMESTAMP,
    created_by       BIGINT,
    version          INTEGER NOT NULL,
    team_id          UUID REFERENCES teams (id),
    owner_id         BIGINT REFERENCES users (id),
    deleted_at       TIMESTAMP
);

ALTER TABLE document_groups DROP CONSTRAINT IF EXISTS document_groups_case_id_fkey;
ALTER TABLE document_groups ADD CONSTRAINT document_groups_case_id_fkey FOREIGN KEY (case_id) REFERENCES cases (id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS case_parties
(
    case_id         UUID         NOT NULL REFERENCES cases (id) ON DELETE CASCADE,
    legal_entity_id UUID         NOT NULL REFERENCES legal_entities (id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('DEFENDANT', 'PLAINTIFF', 'OTHER')),
    PRIMARY KEY (case_id, legal_entity_id, role)
);

CREATE TABLE IF NOT EXISTS case_documents
(
    case_id     UUID NOT NULL,
    document_id UUID NOT NULL,
    PRIMARY KEY (case_id, document_id),
    FOREIGN KEY (case_id) REFERENCES cases (id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS events
(
    id         UUID PRIMARY KEY,
    case_id    UUID        NOT NULL REFERENCES cases (id) ON DELETE CASCADE,
    type       VARCHAR(20) NOT NULL CHECK (type IN ('COURT', 'INHOUSE', 'OUTSIDE')),
    status     BOOLEAN     NOT NULL DEFAULT FALSE,
    event_date TIMESTAMP   NOT NULL,
    details    TEXT,
    created_at TIMESTAMP,
    created_by BIGINT,
    version    INTEGER     NOT NULL,
    team_id    UUID REFERENCES teams (id),
    owner_id   BIGINT REFERENCES users (id),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS event_participants
(
    event_id        UUID NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    legal_entity_id UUID NOT NULL REFERENCES legal_entities (id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, legal_entity_id)
);

CREATE TABLE IF NOT EXISTS activity
(
    id          UUID PRIMARY KEY,
    user_id     BIGINT REFERENCES users (id),
    team_id     UUID REFERENCES teams (id),
    action      VARCHAR(255) NOT NULL,
    entity_id   UUID,
    entity_type VARCHAR(255),
    details     TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    user_id      BIGINT      NOT NULL REFERENCES users (id),
    team_id      UUID        NOT NULL REFERENCES teams (id),
    access_level VARCHAR(50) NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by   BIGINT,
    version      INTEGER     NOT NULL
);

CREATE TABLE IF NOT EXISTS team_invitations
(
    id           UUID PRIMARY KEY,
    team_id      UUID         NOT NULL REFERENCES teams (id),
    email        VARCHAR(255) NOT NULL,
    access_level VARCHAR(50)  NOT NULL,
    token        VARCHAR(255) UNIQUE NOT NULL,
    status       VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    invited_by   BIGINT       NOT NULL REFERENCES users (id),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMP    NOT NULL,
    version      INTEGER      NOT NULL
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

-- Indices and full-text search
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents (status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents (created_at);
CREATE INDEX IF NOT EXISTS idx_document_history_document_id ON document_history (document_id);
CREATE INDEX IF NOT EXISTS idx_document_history_version ON document_history (document_id, history_version);
CREATE INDEX IF NOT EXISTS idx_activity_date ON activity (created_at);
CREATE INDEX IF NOT EXISTS idx_documents_owner_id ON documents (owner_id);
CREATE INDEX IF NOT EXISTS idx_documents_team_id ON documents (team_id);
CREATE INDEX IF NOT EXISTS idx_legal_entities_owner_id ON legal_entities (owner_id);
CREATE INDEX IF NOT EXISTS idx_legal_entities_team_id ON legal_entities (team_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user_id ON team_members (user_id);
CREATE INDEX IF NOT EXISTS idx_team_members_team_id ON team_members (team_id);
CREATE INDEX IF NOT EXISTS idx_team_invitations_token ON team_invitations (token);
CREATE INDEX IF NOT EXISTS idx_team_invitations_email ON team_invitations (email);
CREATE INDEX IF NOT EXISTS idx_team_invitations_team_id ON team_invitations (team_id);
CREATE INDEX IF NOT EXISTS idx_case_documents_case_id ON case_documents (case_id);
CREATE INDEX IF NOT EXISTS idx_case_documents_document_id ON case_documents (document_id);
CREATE INDEX IF NOT EXISTS idx_cases_owner_id ON cases (owner_id);
CREATE INDEX IF NOT EXISTS idx_cases_team_id ON cases (team_id);
CREATE INDEX IF NOT EXISTS idx_cases_created_at ON cases (created_at);
CREATE INDEX IF NOT EXISTS idx_case_parties_case_id ON case_parties (case_id);
CREATE INDEX IF NOT EXISTS idx_events_case_id ON events (case_id);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events (event_date);
CREATE INDEX IF NOT EXISTS idx_document_groups_owner_id ON document_groups (owner_id);
CREATE INDEX IF NOT EXISTS idx_document_groups_team_id ON document_groups (team_id);
CREATE INDEX IF NOT EXISTS idx_document_groups_created_at ON document_groups (created_at);
CREATE INDEX IF NOT EXISTS idx_storage_files_owner_id ON storage_files (owner_id);
CREATE INDEX IF NOT EXISTS idx_storage_files_team_id ON storage_files (team_id);
CREATE INDEX IF NOT EXISTS idx_storage_files_document_group_id ON storage_files (document_group_id);
CREATE INDEX IF NOT EXISTS idx_storage_files_created_at ON storage_files (created_at);

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

-- Normalize existing emails and usernames
UPDATE users SET email = LOWER(email), username = LOWER(username);

-- Create search vector update for document_examples
CREATE INDEX IF NOT EXISTS idx_document_examples_search ON document_examples USING GIN (search_vector);

CREATE OR REPLACE FUNCTION document_examples_search_vector_update()
    RETURNS trigger AS
'
    BEGIN
        NEW.search_vector :=
                setweight(to_tsvector(''english'', COALESCE(NEW.title, '''')), ''A'') ||
                setweight(to_tsvector(''english'', COALESCE(NEW.description, '''')), ''B'');
        RETURN NEW;
    END;
' LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS document_examples_search_vector_trigger ON document_examples;
CREATE TRIGGER document_examples_search_vector_trigger
    BEFORE INSERT OR UPDATE
    ON document_examples
    FOR EACH ROW
EXECUTE FUNCTION document_examples_search_vector_update();

INSERT INTO document_examples (id, title, description, questions, base, requirements, template_files)
VALUES
    ('11111111-1111-1111-1111-111111111111',
     'Pozew o rozwód (z alimentami)',
     'Pozew o rozwód zawierający wnioski o uregulowanie władzy rodzicielskiej i alimentów na dzieci.',
     '[
       {"id":"plaintiff","text":"Kto składa pozew rozwodowy?","type":"ENTITY"},
       {"id":"defendant","text":"Kim jest drugi małżonek?","type":"ENTITY"},
       {"id":"marriage_date","text":"Data zawarcia małżeństwa","type":"TEXT"},
       {"id":"marriage_place","text":"Miejsce zawarcia małżeństwa","type":"TEXT"},
       {"id":"children_count","text":"Liczba małoletnich dzieci stron","type":"TEXT"},
       {"id":"children_details","text":"Dane dzieci (imiona, daty urodzenia)","type":"TEXT"},
       {"id":"alimony_amount","text":"Wnioskowana kwota alimentów na każde dziecko","type":"TEXT"},
       {"id":"custody_request","text":"Wniosek dotyczący władzy rodzicielskiej","type":"TEXT"},
       {"id":"fault","text":"Czy rozwód z orzekaniem o winie?","type":"TEXT"},
       {"id":"separation_date","text":"Kiedy nastąpił zupełny i trwały rozkład pożycia?","type":"TEXT"}
     ]'::jsonb,
     '["eli/MP/1964/155/ogl", "eli/MP/1964/155/ogl"]'::jsonb,
     '["Odpis aktu małżeństwa", "Odpisy aktów urodzenia dzieci", "Zaświadczenie o zarobkach"]'::jsonb,
     '[
       {"id":"t1111111-1111-1111-1111-111111111111","name":"pozew_rozwod_alimenty.pdf","s3Key":"templates/document-examples/11111111-1111-1111-1111-111111111111/pozew_rozwod_alimenty.pdf"}
     ]'::jsonb),

    ('22222222-2222-2222-2222-222222222222',
     'Pozew o rozwód (bez dzieci)',
     'Pozew o rozwód w sytuacji, gdy małżonkowie nie posiadają wspólnych małoletnich dzieci.',
     '[
       {"id":"plaintiff","text":"Dane powoda/powódki","type":"ENTITY"},
       {"id":"defendant","text":"Dane pozwanego/pozwanej","type":"ENTITY"},
       {"id":"marriage_date","text":"Data zawarcia związku małżeńskiego","type":"TEXT"},
       {"id":"marriage_place","text":"Miejsce sporządzenia aktu małżeństwa","type":"TEXT"},
       {"id":"separation_date","text":"Data ustania wspólnego pożycia","type":"TEXT"},
       {"id":"fault","text":"Czy żądasz orzekania o winie małżonka?","type":"TEXT"},
       {"id":"property_division","text":"Czy składasz wniosek o podział majątku wspólnego?","type":"TEXT"},
       {"id":"maintenance_request","text":"Czy żądasz alimentów na swoją rzecz od małżonka?","type":"TEXT"},
       {"id":"reasons","text":"Główne przyczyny rozkładu pożycia","type":"TEXT"},
       {"id":"witnesses","text":"Dane świadków (jeśli dotyczy)","type":"TEXT"}
     ]'::jsonb,
     '["eli/MP/1964/155/ogl", "eli/MP/1964/155/ogl"]'::jsonb,
     '["Skrócony odpis aktu małżeństwa"]'::jsonb,
     '[
       {"id":"t2222222-2222-2222-2222-222222222222","name":"pozew_rozwod_bez-dzieci.pdf","s3Key":"templates/document-examples/22222222-2222-2222-2222-222222222222/pozew_rozwod_bez-dzieci.pdf"}
     ]'::jsonb),

    ('33333333-3333-3333-3333-333333333333',
     'Ponaglenie płatności',
     'Formalne wezwanie do zapłaty zaległego zobowiązania.',
     '[
       {"id":"creditor","text":"Kto jest wierzycielem?","type":"ENTITY"},
       {"id":"debtor","text":"Kto jest dłużnikiem?","type":"ENTITY"},
       {"id":"amount","text":"Kwota zaległego zobowiązania","type":"TEXT"},
       {"id":"currency","text":"Waluta zadłużenia","type":"TEXT"},
       {"id":"invoice_number","text":"Numer faktury lub umowy","type":"TEXT"},
       {"id":"due_date","text":"Pierwotny termin płatności","type":"TEXT"},
       {"id":"delay_days","text":"Liczba dni opóźnienia w płatności","type":"TEXT"},
       {"id":"interest","text":"Czy naliczane są odsetki ustawowe?","type":"TEXT"},
       {"id":"payment_method","text":"Preferowany sposób dokonania płatności","type":"TEXT"},
       {"id":"deadline","text":"Ostateczny termin zapłaty","type":"TEXT"}
     ]'::jsonb,
     '["eli/MP/1964/155/ogl"]'::jsonb,
     '["Kopia faktury", "Kopia umowy"]'::jsonb,
     '[
       {"id":"t3333333-3333-3333-3333-333333333331","name":"payment_reminder_template_1.pdf","s3Key":"templates/document-examples/33333333-3333-3333-3333-333333333333/t3333333-3333-3333-3333-333333333331.pdf"},
       {"id":"t3333333-3333-3333-3333-333333333332","name":"payment_reminder_template_2.pdf","s3Key":"templates/document-examples/33333333-3333-3333-3333-333333333333/t3333333-3333-3333-3333-333333333332.pdf"}
     ]'::jsonb),

    ('44444444-4444-4444-4444-444444444444',
     'Ponaglenie płatności',
     'Ostateczne wezwanie do zapłaty przed skierowaniem sprawy na drogę postępowania prawnego.',
     '[
       {"id":"creditor","text":"Wierzyciel występujący z wezwaniem","type":"ENTITY"},
       {"id":"debtor","text":"Dłużnik zobowiązany do zapłaty","type":"ENTITY"},
       {"id":"legal_basis","text":"Podstawa prawna dochodzonego roszczenia","type":"TEXT"},
       {"id":"amount","text":"Łączna kwota do zapłaty","type":"TEXT"},
       {"id":"interest","text":"Kwota naliczonych odsetek","type":"TEXT"},
       {"id":"invoice_list","text":"Wykaz niezapłaconych faktur","type":"TEXT"},
       {"id":"previous_reminders","text":"Czy były wysyłane wcześniejsze wezwania do zapłaty?","type":"TEXT"},
       {"id":"bank_account","text":"Numer rachunku bankowego do zapłaty","type":"TEXT"},
       {"id":"deadline","text":"Termin zapłaty pozwalający uniknąć dalszych kroków prawnych","type":"TEXT"},
       {"id":"consequences","text":"Skutki braku zapłaty w wyznaczonym terminie","type":"TEXT"}
     ]'::jsonb,
     '["eli/MP/1964/155/ogl"]'::jsonb,
     '["Faktury", "Dowody doręczenia"]'::jsonb,
     '[
       {"id":"t4444444-4444-4444-4444-444444444441","name":"final_payment_reminder_template_1.pdf","s3Key":"templates/document-examples/44444444-4444-4444-4444-444444444444/t4444444-4444-4444-4444-444444444441.pdf"},
       {"id":"t4444444-4444-4444-4444-444444444442","name":"final_payment_reminder_template_2.pdf","s3Key":"templates/document-examples/44444444-4444-4444-4444-444444444444/t4444444-4444-4444-4444-444444444442.pdf"}
     ]'::jsonb),

    ('55555555-5555-5555-5555-555555555555',
     'Wniosek o alimenty',
     'Wniosek o zasądzenie świadczeń alimentacyjnych.',
     '[
       {"id":"claimant","text":"Kto składa wniosek o alimenty?","type":"ENTITY"},
       {"id":"obligor","text":"Kto jest zobowiązany do płacenia alimentów?","type":"ENTITY"},
       {"id":"beneficiary","text":"Na czyją rzecz mają być zasądzone alimenty?","type":"ENTITY"},
       {"id":"relationship","text":"Jaki jest stosunek prawny pomiędzy stronami?","type":"TEXT"},
       {"id":"amount","text":"Żądana kwota alimentów","type":"TEXT"},
       {"id":"frequency","text":"Częstotliwość płatności alimentów","type":"TEXT"},
       {"id":"needs","text":"Uzasadnienie potrzeb osoby uprawnionej","type":"TEXT"},
       {"id":"income","text":"Dochody i możliwości zarobkowe zobowiązanego","type":"TEXT"},
       {"id":"court","text":"Właściwy sąd do rozpoznania sprawy","type":"ENTITY"},
       {"id":"evidence","text":"Dowody na poparcie żądania","type":"TEXT"}
     ]'::jsonb,
     '["eli/MP/1964/155/ogl"]'::jsonb,
     '["Odpis aktu urodzenia", "Zaświadczenia o dochodach"]'::jsonb,
     '[
       {"id":"t5555555-5555-5555-5555-555555555551","name":"alimony_template_1.pdf","s3Key":"templates/document-examples/55555555-5555-5555-5555-555555555555/t5555555-5555-5555-5555-555555555551.pdf"},
       {"id":"t5555555-5555-5555-5555-555555555552","name":"alimony_template_2.pdf","s3Key":"templates/document-examples/55555555-5555-5555-5555-555555555555/t5555555-5555-5555-5555-555555555552.pdf"}
     ]'::jsonb),

    ('66666666-6666-6666-6666-666666666666',
     'Wniosek o odroczenie kary',
     'Wniosek o odroczenie wykonania kary orzeczonej wyrokiem sądu.',
     '[
       {"id":"convict","text":"Kto jest osobą skazaną?","type":"ENTITY"},
       {"id":"court","text":"Który sąd wydał wyrok skazujący?","type":"TEXT"},
       {"id":"sentence","text":"Rodzaj i wymiar orzeczonej kary","type":"TEXT"},
       {"id":"start_date","text":"Planowany termin rozpoczęcia odbywania kary","type":"TEXT"},
       {"id":"requested_delay","text":"Wnioskowany okres odroczenia kary","type":"TEXT"},
       {"id":"reason","text":"Przyczyna złożenia wniosku o odroczenie kary","type":"TEXT"},
       {"id":"health","text":"Okoliczności zdrowotne uzasadniające odroczenie","type":"TEXT"},
       {"id":"family","text":"Okoliczności rodzinne uzasadniające odroczenie","type":"TEXT"},
       {"id":"evidence","text":"Dowody potwierdzające wskazane okoliczności","type":"TEXT"},
       {"id":"signature","text":"Dane podpisu wnioskodawcy","type":"TEXT"}
     ]'::jsonb,
     '["eli/MP/1964/155/ogl"]'::jsonb,
     '["Zaświadczenia lekarskie", "Dokumenty rodzinne"]'::jsonb,
     '[
       {"id":"t6666666-6666-6666-6666-666666666661","name":"sentence_delay_template_1.pdf","s3Key":"templates/document-examples/66666666-6666-6666-6666-666666666666/t6666666-6666-6666-6666-666666666661.pdf"},
       {"id":"t6666666-6666-6666-6666-666666666662","name":"sentence_delay_template_2.pdf","s3Key":"templates/document-examples/66666666-6666-6666-6666-666666666666/t6666666-6666-6666-6666-666666666662.pdf"}
     ]'::jsonb)
ON CONFLICT (id) DO NOTHING;
