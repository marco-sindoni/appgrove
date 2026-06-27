-- UC 0051 — modello dati dell'app fatture.
-- Tenant-scoped: tenant_id è il discriminatore (varchar(64), invariante #2), contiene l'UUID
-- dell'account (= tenant). Non è una FK: chiave logica gestita da Hibernate DISCRIMINATOR.
-- Audit + soft-delete + PK UUID v7 ereditati dal pattern commons (BaseTenantEntity).

-- ── invoice (fattura; tenant-scoped) ──────────────────────────────────────────
-- customer_name / customer_email sono DATI PERSONALI del cliente (base: contratto, #13 A2/L).
CREATE TABLE app_fatture.invoice (
    id             uuid          PRIMARY KEY,
    tenant_id      varchar(64)   NOT NULL,
    number         varchar(32)   NOT NULL,           -- progressivo per-tenant per-anno (es. 2026-0001)
    customer_name  varchar(255)  NOT NULL,           -- @PersonalData
    customer_email varchar(320),                     -- @PersonalData (nullable)
    issue_date     date          NOT NULL,
    status         varchar(16)   NOT NULL DEFAULT 'draft',  -- draft | issued | paid | voided
    currency       varchar(3)    NOT NULL DEFAULT 'EUR',
    total_amount   numeric(14,2) NOT NULL DEFAULT 0,
    created_at     timestamptz   NOT NULL,
    updated_at     timestamptz   NOT NULL,
    created_by     varchar(64),
    updated_by     varchar(64),
    deleted_at     timestamptz
);
-- numero univoco per tenant (la numerazione progressiva è generata server-side).
CREATE UNIQUE INDEX ux_invoice_tenant_number ON app_fatture.invoice (tenant_id, number);
CREATE INDEX ix_invoice_tenant ON app_fatture.invoice (tenant_id);
-- supporta il conteggio quota "flow" (fatture create nella finestra) per tenant.
CREATE INDEX ix_invoice_tenant_created ON app_fatture.invoice (tenant_id, created_at);

-- ── invoice_line (riga fattura; tenant-scoped, figlia di invoice) ─────────────
CREATE TABLE app_fatture.invoice_line (
    id          uuid          PRIMARY KEY,
    tenant_id   varchar(64)   NOT NULL,
    invoice_id  uuid          NOT NULL REFERENCES app_fatture.invoice (id),
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
CREATE INDEX ix_invoice_line_invoice ON app_fatture.invoice_line (invoice_id);
CREATE INDEX ix_invoice_line_tenant ON app_fatture.invoice_line (tenant_id);
