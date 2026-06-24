-- Migration di SOLO TEST (UC 0012): crea la tabella dell'harness multitenancy.
-- In produzione questa tabella NON esiste (lo schema platform resta vuoto fino a UC 0013).
CREATE TABLE platform.widget (
    id          uuid        PRIMARY KEY,
    tenant_id   varchar(64) NOT NULL,
    name        varchar(255) NOT NULL,
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    created_by  varchar(64),
    updated_by  varchar(64),
    deleted_at  timestamptz
);
CREATE INDEX idx_widget_tenant ON platform.widget (tenant_id);
