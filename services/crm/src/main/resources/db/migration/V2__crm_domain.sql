-- Dominio reale dell'app Mini-CRM (UC 0054), che sostituisce il modello segnaposto dello scaffolding.
--
-- Tre entità tenant-scoped (tenant_id = UUID dell'account, discriminatore invariante #2; non è una FK,
-- è la chiave logica gestita da Hibernate). Audit + soft-delete + PK UUID v7 ereditati da BaseTenantEntity.
--
--  · contact      — anagrafica di contatto (persone di terzi immesse dal tenant: DATI PERSONALI)
--  · interaction  — nota datata collegata a un contatto (la nota è testo libero: DATO PERSONALE)
--  · seat         — un POSTO occupato: un utente dell'account abilitato a QUESTA app. È la giacenza
--                   contata dalla quota `seats` (natura stock): il conteggio dei posti occupati è un
--                   dato dell'app, non di core (UC 0054).
--
-- Ogni campo annotato @PersonalData nelle entità DEVE avere la voce corrispondente nel manifesto dati
-- (docs/compliance/manifests/crm.yaml), altrimenti PersonalDataManifestTest è rosso (gate UC 0030).

-- ── contact (contatto; tenant-scoped) ────────────────────────────────────────
CREATE TABLE app_crm.contact (
    id            uuid          PRIMARY KEY,
    tenant_id     varchar(64)   NOT NULL,
    display_name  varchar(255)  NOT NULL,           -- @PersonalData
    email         varchar(320),                     -- @PersonalData (facoltativo)
    phone         varchar(64),                      -- @PersonalData (facoltativo)
    organization  varchar(255),                     -- denominazione dell'organizzazione del contatto
    stage         varchar(16)   NOT NULL DEFAULT 'lead',  -- lead | qualified | negotiating | won | lost
    notes         varchar(2000),                    -- @PersonalData (testo libero, facoltativo)
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    created_by    varchar(64),
    updated_by    varchar(64),
    deleted_at    timestamptz
);
CREATE INDEX ix_contact_tenant ON app_crm.contact (tenant_id);
CREATE INDEX ix_contact_tenant_stage ON app_crm.contact (tenant_id, stage);

-- ── interaction (interazione datata; figlia di contact) ──────────────────────
CREATE TABLE app_crm.interaction (
    id          uuid          PRIMARY KEY,
    tenant_id   varchar(64)   NOT NULL,
    contact_id  uuid          NOT NULL REFERENCES app_crm.contact (id),
    kind        varchar(16)   NOT NULL DEFAULT 'note',  -- call | email | meeting | note
    occurred_on date          NOT NULL,
    note        varchar(2000),                    -- @PersonalData (testo libero, facoltativo)
    created_at  timestamptz   NOT NULL,
    updated_at  timestamptz   NOT NULL,
    created_by  varchar(64),
    updated_by  varchar(64),
    deleted_at  timestamptz
);
CREATE INDEX ix_interaction_contact ON app_crm.interaction (contact_id);
CREATE INDEX ix_interaction_tenant ON app_crm.interaction (tenant_id);

-- ── seat (posto occupato; giacenza della quota `seats`) ──────────────────────
-- Un posto = un utente dell'account (user_id = claim `sub` del token) abilitato all'app. La quota
-- stock conta le righe ATTIVE (deleted_at is null): revocare un posto è una cancellazione logica e
-- libera SUBITO la giacenza. L'unicità parziale impedisce due posti attivi per lo stesso utente,
-- lasciando però lo storico delle revoche.
CREATE TABLE app_crm.seat (
    id          uuid          PRIMARY KEY,
    tenant_id   varchar(64)   NOT NULL,
    user_id     varchar(64)   NOT NULL,           -- `sub` del membro abilitato (identità utente, non dato di terzi)
    created_at  timestamptz   NOT NULL,
    updated_at  timestamptz   NOT NULL,
    created_by  varchar(64),
    updated_by  varchar(64),
    deleted_at  timestamptz
);
CREATE UNIQUE INDEX ux_seat_tenant_user_active
    ON app_crm.seat (tenant_id, user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_seat_tenant ON app_crm.seat (tenant_id);
