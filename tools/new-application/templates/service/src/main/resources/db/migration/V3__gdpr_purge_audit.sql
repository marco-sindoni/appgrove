-- UC 0032 — audit della purge GDPR (prova dell'erasure, #13 L70).
-- Una riga per invocazione di purgeData: solo identificativi e conteggi (nessun dato personale).
-- NON viene cancellata dalla purge (è la prova; retention audit 12 mesi, #08).

CREATE TABLE @@SCHEMA@@.gdpr_purge_audit (
    id                uuid        PRIMARY KEY,
    tenant_id         varchar(64) NOT NULL,
    app_id            varchar(64) NOT NULL,
    reason            varchar(64) NOT NULL,
    deleted_by_entity jsonb       NOT NULL,
    total             integer     NOT NULL,
    executed_at       timestamptz NOT NULL
);
CREATE INDEX ix_@@APP_ID@@_gdpr_purge_audit_tenant ON @@SCHEMA@@.gdpr_purge_audit (tenant_id);
