-- UC 0034 (change 0030) — ticketing in-house + limitazione del trattamento (art. 18).
--
-- support_ticket / support_ticket_message: ticket di supporto con thread utente↔admin (#13 D21).
-- I ticket "privacy" sono il tipo speciale con scadenza legale a 1 mese (art. 12): auto-creati
-- dagli eventi (export FAILED → ux_support_ticket_export_job garantisce l'idempotenza) o aperti
-- dall'utente. Dati personali minimizzati: oggetto e testo dei messaggi (niente allegati, MVP).
-- Retention 24 mesi dalla chiusura (#13 E): hard-delete dello sweeper (change 0030).
--
-- suspended_reason su accounts/users: causale della sospensione — distingue la limitazione del
-- trattamento ('gdpr_restriction', art. 18, #13 D19) da una sospensione amministrativa.
--
-- gdpr_restriction_audit: prova di evasione della limitazione (#13 D19/L75) — una riga per
-- applica/rimuovi, con attore e ticket collegato. Solo identificativi (nessun dato personale);
-- come gdpr_purge_audit NON viene toccata dalla purge (retention audit 12 mesi, #08).

CREATE TABLE platform.support_ticket (
    id            uuid          PRIMARY KEY,
    tenant_id     varchar(64)   NOT NULL,
    type          varchar(16)   NOT NULL,   -- support | privacy
    subject       varchar(200)  NOT NULL,
    priority      varchar(16)   NOT NULL,   -- low | normal | high
    status        varchar(16)   NOT NULL,   -- open | in_progress | resolved | closed
    due_at        timestamptz,              -- ticket privacy: presa in carico entro 1 mese (art. 12)
    export_job_id uuid          REFERENCES platform.gdpr_export_job (id),
    closed_at     timestamptz,              -- da qui decorrono i 24 mesi di retention
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    created_by    varchar(64),
    updated_by    varchar(64),
    deleted_at    timestamptz
);
CREATE INDEX ix_support_ticket_tenant ON platform.support_ticket (tenant_id);
-- idempotenza dell'auto-ticket: al più un ticket per export job fallito
CREATE UNIQUE INDEX ux_support_ticket_export_job
    ON platform.support_ticket (export_job_id) WHERE export_job_id IS NOT NULL;

CREATE TABLE platform.support_ticket_message (
    id         uuid          PRIMARY KEY,
    tenant_id  varchar(64)   NOT NULL,
    ticket_id  uuid          NOT NULL REFERENCES platform.support_ticket (id),
    author     varchar(16)   NOT NULL,   -- user | admin | system
    body       varchar(4000) NOT NULL,
    created_at timestamptz   NOT NULL,
    updated_at timestamptz   NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    deleted_at timestamptz
);
CREATE INDEX ix_support_ticket_message_ticket ON platform.support_ticket_message (ticket_id);

ALTER TABLE platform.accounts ADD COLUMN suspended_reason varchar(32);
ALTER TABLE platform.users    ADD COLUMN suspended_reason varchar(32);

CREATE TABLE platform.gdpr_restriction_audit (
    id          uuid        PRIMARY KEY,
    tenant_id   varchar(64) NOT NULL,
    target_kind varchar(16) NOT NULL,   -- account | user
    target_id   varchar(64) NOT NULL,
    action      varchar(16) NOT NULL,   -- applied | removed
    ticket_id   uuid,
    actor       varchar(64) NOT NULL,
    note        varchar(512),
    executed_at timestamptz NOT NULL
);
CREATE INDEX ix_gdpr_restriction_audit_tenant ON platform.gdpr_restriction_audit (tenant_id);
