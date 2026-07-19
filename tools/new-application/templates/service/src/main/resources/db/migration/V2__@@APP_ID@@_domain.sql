-- Modello dati SEGNAPOSTO dell'app @@APP_NAME@@ (generato da tools/new-application, UC 0046).
--
-- ATTENZIONE: questo è lo scheletro del dominio, non il dominio vero. Serve a far nascere l'app con
-- la suite verde e con gli invarianti già dimostrati (isolamento tenant, quota, export/erasure).
-- Sostituirlo col modello reale è il PRIMO lavoro dopo lo scaffolding: rinominare le tabelle qui e
-- le entità Java corrispondenti, poi aggiornare manifesto dati (docs/compliance/manifests) e test.
--
-- Tenant-scoped: tenant_id è il discriminatore (varchar(64), invariante #2), contiene l'UUID
-- dell'account (= tenant). Non è una FK: chiave logica gestita da Hibernate DISCRIMINATOR.
-- Audit + soft-delete + PK UUID v7 ereditati dal pattern commons (BaseTenantEntity).

-- ── item (record principale; tenant-scoped) ───────────────────────────────────
-- contact_name / contact_email sono DATI PERSONALI (base: contratto, #13 A2/L): ogni campo
-- annotato @PersonalData nell'entità DEVE avere la voce corrispondente nel manifesto dati,
-- altrimenti PersonalDataManifestTest è rosso (gate UC 0030).
CREATE TABLE @@SCHEMA@@.item (
    id            uuid          PRIMARY KEY,
    tenant_id     varchar(64)   NOT NULL,
    code          varchar(32)   NOT NULL,           -- progressivo per-tenant per-anno (es. 2026-0001)
    contact_name  varchar(255)  NOT NULL,           -- @PersonalData
    contact_email varchar(320),                     -- @PersonalData (nullable)
    recorded_on   date          NOT NULL,
    status        varchar(16)   NOT NULL DEFAULT 'draft',  -- draft | active | done | archived
    currency      varchar(3)    NOT NULL DEFAULT 'EUR',
    total_amount  numeric(14,2) NOT NULL DEFAULT 0,
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    created_by    varchar(64),
    updated_by    varchar(64),
    deleted_at    timestamptz
);
-- codice univoco per tenant (la numerazione progressiva è generata server-side).
CREATE UNIQUE INDEX ux_item_tenant_code ON @@SCHEMA@@.item (tenant_id, code);
CREATE INDEX ix_item_tenant ON @@SCHEMA@@.item (tenant_id);
-- supporta il conteggio quota "flow" (record creati nella finestra) per tenant.
CREATE INDEX ix_item_tenant_created ON @@SCHEMA@@.item (tenant_id, created_at);

-- ── item_line (riga; tenant-scoped, figlia di item) ──────────────────────────
CREATE TABLE @@SCHEMA@@.item_line (
    id          uuid          PRIMARY KEY,
    tenant_id   varchar(64)   NOT NULL,
    item_id     uuid          NOT NULL REFERENCES @@SCHEMA@@.item (id),
    description varchar(500)  NOT NULL,
    quantity    numeric(14,3) NOT NULL DEFAULT 1,
    unit_amount numeric(14,2) NOT NULL DEFAULT 0,
    line_amount numeric(14,2) NOT NULL DEFAULT 0,
    created_at  timestamptz   NOT NULL,
    updated_at  timestamptz   NOT NULL,
    created_by  varchar(64),
    updated_by  varchar(64),
    deleted_at  timestamptz
);
CREATE INDEX ix_item_line_item ON @@SCHEMA@@.item_line (item_id);
CREATE INDEX ix_item_line_tenant ON @@SCHEMA@@.item_line (tenant_id);
