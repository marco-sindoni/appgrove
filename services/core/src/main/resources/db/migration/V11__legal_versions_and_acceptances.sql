-- UC 0056 (change 0056) — ri-accettazione ToU/Privacy a runtime.
--
-- legal_version: versioni CORRENTI dei documenti legali, PLATFORM-LEVEL (NON tenant-scoped).
-- Fonte di verità per il confronto "accettata < corrente", disaccoppia il core da content/legal/:
-- popolata dal comando `sync-legal` (CI al deploy dei legali) e dallo startup-sync in locale/test.
-- Una riga per componente (la versione corrente); `major` = prima cifra del semver `version`.
-- Dato NON personale (identità del prodotto, non dell'utente).
--
-- legal_acceptance: registro APPEND-ONLY della prova di accettazione/presa d'atto (accountability,
-- art. 5.2 + contratto art. 6.1.b). TENANT/UTENTE-SCOPED. Minimizzato: chi (user_id), cosa
-- (component + version + major), che atto (accept | acknowledge), quando (accepted_at) e su quale
-- commit dei legali (commit_hash). NESSUN IP/user-agent. Dato personale: user_id (identificativo).

CREATE TABLE platform.legal_version (
    id             uuid          PRIMARY KEY,
    component      varchar(32)   NOT NULL,   -- terms | privacy | cookie | refund | subprocessors
    major          integer       NOT NULL,   -- prima cifra del semver (soglia di ri-accettazione)
    version        varchar(32)   NOT NULL,   -- semver pieno (es. 2.0.0)
    effective_date date          NOT NULL,
    created_at     timestamptz   NOT NULL,
    updated_at     timestamptz   NOT NULL,
    created_by     varchar(64),
    updated_by     varchar(64),
    deleted_at     timestamptz
);
-- una sola versione corrente per componente
CREATE UNIQUE INDEX ux_legal_version_component ON platform.legal_version (component);

CREATE TABLE platform.legal_acceptance (
    id           uuid          PRIMARY KEY,
    tenant_id    varchar(64)   NOT NULL,
    user_id      varchar(255)  NOT NULL,   -- cognito_sub (identità utente dal JWT)
    component    varchar(32)   NOT NULL,
    version      varchar(32)   NOT NULL,
    major        integer       NOT NULL,
    act_type     varchar(16)   NOT NULL,   -- accept (Termini) | acknowledge (Privacy/Cookie)
    accepted_at  timestamptz   NOT NULL,
    commit_hash  varchar(64),              -- commit dei legali su cui si è accettato (null in locale)
    created_at   timestamptz   NOT NULL,
    updated_at   timestamptz   NOT NULL,
    created_by   varchar(64),
    updated_by   varchar(64),
    deleted_at   timestamptz
);
-- derivazione stato: max(major accettata) per (tenant, utente, componente)
CREATE INDEX ix_legal_acceptance_lookup ON platform.legal_acceptance (tenant_id, user_id, component);
-- idempotenza del POST: una accettazione per (tenant, utente, componente, versione)
CREATE UNIQUE INDEX ux_legal_acceptance_once
    ON platform.legal_acceptance (tenant_id, user_id, component, version);
