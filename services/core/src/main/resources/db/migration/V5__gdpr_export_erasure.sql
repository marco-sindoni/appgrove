-- UC 0032 (change 0028) — framework export/erasure GDPR.
--
-- gdpr_export_job / gdpr_export_job_item: record del job di export asincrono (#13 D22) con
-- avanzamento per-servizio. Tenant-scoped (discriminator); chi ha richiesto = created_by
-- (valorizzato dal sub del JWT via AuditListener, come ogni colonna di audit — nessun nuovo
-- dato personale oltre a quel precedente).
--
-- gdpr_purge_audit: prova dell'erasure (#13 L70) — una riga per invocazione di purgeData con i
-- conteggi cancellati per entità. Solo identificativi e conteggi (nessun dato personale);
-- NON viene cancellata dalla purge (è la prova; retention audit 12 mesi, #08).

CREATE TABLE platform.gdpr_export_job (
    id         uuid          PRIMARY KEY,
    tenant_id  varchar(64)   NOT NULL,
    kind       varchar(16)   NOT NULL,   -- account | app
    app_id     varchar(64),              -- slug dell'app per kind=app
    status     varchar(16)   NOT NULL,   -- QUEUED | RUNNING | COMPLETED | FAILED
    zip_key    varchar(255),             -- chiave S3 dello ZIP aggregato (a COMPLETED)
    error      varchar(1024),
    completed_at timestamptz,
    created_at timestamptz   NOT NULL,
    updated_at timestamptz   NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    deleted_at timestamptz
);
CREATE INDEX ix_gdpr_export_job_tenant ON platform.gdpr_export_job (tenant_id);

CREATE TABLE platform.gdpr_export_job_item (
    id           uuid         PRIMARY KEY,
    tenant_id    varchar(64)  NOT NULL,
    job_id       uuid         NOT NULL REFERENCES platform.gdpr_export_job (id),
    app_id       varchar(64)  NOT NULL,  -- 'platform' o slug dell'app
    status       varchar(16)  NOT NULL,  -- QUEUED | COMPLETED | FAILED
    steps        jsonb,                  -- etichette di step dichiarate dal contratto (#13 D22)
    fragment_key varchar(255),
    error        varchar(1024),
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL,
    created_by   varchar(64),
    updated_by   varchar(64),
    deleted_at   timestamptz
);
CREATE INDEX ix_gdpr_export_job_item_job ON platform.gdpr_export_job_item (job_id);
CREATE UNIQUE INDEX ux_gdpr_export_job_item ON platform.gdpr_export_job_item (job_id, app_id);

CREATE TABLE platform.gdpr_purge_audit (
    id                uuid        PRIMARY KEY,
    tenant_id         varchar(64) NOT NULL,
    app_id            varchar(64) NOT NULL,
    reason            varchar(64) NOT NULL,
    deleted_by_entity jsonb       NOT NULL,
    total             integer     NOT NULL,
    executed_at       timestamptz NOT NULL
);
CREATE INDEX ix_gdpr_purge_audit_tenant ON platform.gdpr_purge_audit (tenant_id);
