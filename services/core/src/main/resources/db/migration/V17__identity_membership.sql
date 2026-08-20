-- UC 0116 (change 0088) — identità della persona e appartenenze agli account.
--
-- Scioglie il vincolo «una persona appartiene a un solo account». Oggi il vincolo non è una
-- convenzione: è scritto in V2__core_domain.sql come due indici unici GLOBALI (cognito_sub, email)
-- su `platform.users`, che è una tabella INTERNA all'account. È quel disallineamento — unicità
-- globale su una riga di account — a produrre il vincolo di troppo.
--
-- Dopo questa migrazione:
--   * `platform.identity`   — l'identità di accesso della persona. Entità di PIATTAFORMA: non porta
--                             tenant_id, come `platform.app`. Qui vive l'unicità globale.
--   * `platform.membership` — l'appartenenza (account, identità) con ruolo e stato. Entità di
--                             ACCOUNT: porta tenant_id (invariante #2). Qui vive il vincolo che
--                             serve davvero — «non due volte nello stesso account» — ESPLICITO
--                             sulle righe vive, invece di essere l'effetto collaterale di un
--                             vincolo più forte del necessario.
--
-- Lo stato si SDOPPIA e non si sposta (change 0088, decisione 5):
--   * identity.status   = la persona può accedere alla piattaforma → leva del titolare (limitazione
--                         del trattamento, art. 18: `suspended_reason = 'gdpr_restriction'`);
--   * membership.status = la persona può presentarsi come persona di QUELL'account → leva dell'owner.
-- Chi emette il token e chi autentica pretende ENTRAMBI attivi (a chiusura in caso di dubbio).
--
-- `platform.users` RESTA in piedi come rete di ritorno, ma diventa FREDDA: nessuno la legge e
-- nessuno la scrive più. La rimozione fisica è di una migrazione successiva — toglierla adesso
-- significherebbe non avere via di ritorno se il travaso avesse un difetto.

-- ── identità della persona (piattaforma: NESSUN tenant_id) ───────────────────
CREATE TABLE platform.identity (
    id               uuid         PRIMARY KEY,
    cognito_sub      varchar(128) NOT NULL,
    email            varchar(320) NOT NULL,
    display_name     varchar(255),
    locale           varchar(8)   NOT NULL DEFAULT 'en',
    status           varchar(32)  NOT NULL DEFAULT 'active',
    suspended_reason varchar(32),
    created_at       timestamptz  NOT NULL,
    updated_at       timestamptz  NOT NULL,
    created_by       varchar(64),
    updated_by       varchar(64),
    deleted_at       timestamptz,
    CONSTRAINT ck_identity_locale CHECK (locale IN ('en', 'it'))
);
-- Unicità INCONDIZIONATA (anche sulle righe cancellate), come è oggi su platform.users: l'identità
-- è il punto in cui «una persona = un indirizzo» diventa vero per la piattaforma intera. Il riuso di
-- un indirizzo dopo la cancellazione di un'identità è materia dei percorsi d'ingresso (UC 0118).
CREATE UNIQUE INDEX ux_identity_cognito_sub ON platform.identity (cognito_sub);
CREATE UNIQUE INDEX ux_identity_email       ON platform.identity (lower(email));

COMMENT ON TABLE platform.identity IS
    'UC 0116 — identità di accesso della persona. Entità di piattaforma (nessun tenant_id): qui vive '
    'l''unicità globale di indirizzo e identificativo di autenticazione. status/suspended_reason = '
    'leva del titolare (limitazione del trattamento, art. 18).';

-- ── appartenenza (account: porta tenant_id) ──────────────────────────────────
CREATE TABLE platform.membership (
    id          uuid        PRIMARY KEY,
    tenant_id   varchar(64) NOT NULL,
    identity_id uuid        NOT NULL REFERENCES platform.identity (id),
    role        varchar(32) NOT NULL,
    status      varchar(32) NOT NULL DEFAULT 'active',
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    created_by  varchar(64),
    updated_by  varchar(64),
    deleted_at  timestamptz
);
-- Il vincolo che serve davvero: non due volte nello stesso account. PARZIALE (righe vive) perché chi
-- esce da un account deve poterci rientrare.
CREATE UNIQUE INDEX ux_membership_tenant_identity
    ON platform.membership (tenant_id, identity_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_membership_tenant   ON platform.membership (tenant_id);
-- «A quali account appartiene questa persona?»: lettura di PIATTAFORMA, senza filtro per account.
CREATE INDEX ix_membership_identity ON platform.membership (identity_id);

COMMENT ON TABLE platform.membership IS
    'UC 0116 — appartenenza (account, identità) con ruolo e stato. Entità di account (tenant_id, '
    'invariante #2). Unicità su (tenant_id, identity_id) limitata alle righe vive. status = leva '
    'dell''owner sul singolo account.';

-- ── travaso ─────────────────────────────────────────────────────────────────
-- L'identità nasce con lo STESSO id della riga utente: ogni riferimento già memorizzato altrove
-- (invitations.invited_by, invitations.accepted_user_id, gdpr_restriction_audit.target_id,
-- legal_acceptance/consent_event…) continua a risolvere senza essere riscritto. L'appartenenza
-- prende invece un id nuovo.
INSERT INTO platform.identity
    (id, cognito_sub, email, display_name, locale, status, suspended_reason,
     created_at, updated_at, created_by, updated_by, deleted_at)
SELECT u.id, u.cognito_sub, u.email, u.display_name, u.locale, u.status, u.suspended_reason,
       u.created_at, u.updated_at, u.created_by, u.updated_by, u.deleted_at
FROM platform.users u
WHERE u.deleted_at IS NULL;

INSERT INTO platform.membership
    (id, tenant_id, identity_id, role, status, created_at, updated_at, created_by, updated_by, deleted_at)
SELECT gen_random_uuid(), u.tenant_id, u.id, u.role, u.status,
       u.created_at, u.updated_at, u.created_by, u.updated_by, u.deleted_at
FROM platform.users u
WHERE u.deleted_at IS NULL;

-- ── il vincolo di troppo se ne va ────────────────────────────────────────────
-- Sono i due indici che imponevano «1 utente → 1 account»: l'unicità ora vive sull'identità.
DROP INDEX platform.ux_users_cognito_sub;
DROP INDEX platform.ux_users_email;

COMMENT ON TABLE platform.users IS
    'FREDDA dalla change 0088 (UC 0116): rete di ritorno del travaso verso platform.identity + '
    'platform.membership. Nessun codice la legge e nessuno la scrive più; la rimozione fisica è di '
    'una migrazione successiva. Gli indici unici globali (che imponevano 1 utente → 1 account) sono '
    'stati rimossi qui.';

-- ── guardia dei conteggi, DENTRO la migrazione ──────────────────────────────
-- Una migrazione che perde persone in silenzio è il difetto peggiore possibile qui: il controllo sta
-- nella migrazione e non in un collaudo che qualcuno potrebbe non eseguire.
DO $$
DECLARE
    utenti      bigint;
    identita    bigint;
    appartenenze bigint;
    indirizzi   bigint;
BEGIN
    SELECT count(*) INTO utenti       FROM platform.users WHERE deleted_at IS NULL;
    SELECT count(*) INTO identita     FROM platform.identity;
    SELECT count(*) INTO appartenenze FROM platform.membership;
    SELECT count(*) INTO indirizzi
      FROM platform.users u
     WHERE u.deleted_at IS NULL
       AND NOT EXISTS (SELECT 1 FROM platform.identity i
                        WHERE i.id = u.id
                          AND lower(i.email) = lower(u.email)
                          AND i.cognito_sub = u.cognito_sub);

    IF identita <> utenti THEN
        RAISE EXCEPTION 'V17: travaso incompleto — % utenti vivi, % identità', utenti, identita;
    END IF;
    IF appartenenze <> utenti THEN
        RAISE EXCEPTION 'V17: travaso incompleto — % utenti vivi, % appartenenze', utenti, appartenenze;
    END IF;
    IF indirizzi <> 0 THEN
        RAISE EXCEPTION 'V17: travaso infedele — % utenti senza identità corrispondente', indirizzi;
    END IF;
END $$;
