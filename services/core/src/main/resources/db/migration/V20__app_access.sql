-- UC 0098 (change 0091) — l'accesso di una persona a una applicazione, con il suo ruolo.
--
-- È il luogo in cui vive la frase «questa persona può usare questa applicazione con questo ruolo».
-- Fino a qui il ruolo stava sull'APPARTENENZA all'account e valeva per tutto: chi era `admin`
-- dell'account lo era di ogni applicazione e anche delle schermate di piattaforma. Il Mini-CRM, che
-- aveva bisogno della domanda vera, si era dovuto costruire una tabella di «posti» locale: cioè la
-- domanda esisteva già e aveva trovato una risposta privata invece di una condivisa.
--
-- Perché una ENTITÀ e non una lista di ruoli dentro la persona (epica 22, E22.1): con una lista non si
-- potrebbe interrogare («chi ha accesso al Mini-CRM?»), non si potrebbe vincolare («un solo accesso per
-- terna») e non si potrebbe filtrare riga per riga come impone l'invariante di separazione fra account.
--
-- Il riferimento è all'IDENTITÀ della persona (platform.identity, UC 0116) e non alla sua appartenenza:
-- l'appartenenza a un account può chiudersi e riaprirsi, l'identità no. È anche la ragione per cui questa
-- tabella nasce dopo V17 e può riferire l'identità direttamente, senza il doppio passaggio che il piano
-- di lavoro della storia ipotizzava quando l'identità non esisteva ancora.
--
-- Il ruolo di PIATTAFORMA scende in parallelo a due soli valori (owner | member) — ma quel cambio vive
-- nell'enumerazione Java, non qui: la conversione delle righe `admin` esistenti e il vincolo di controllo
-- che la sigilla sono di UC 0113. Un vincolo di controllo aggiunto adesso rifiuterebbe di applicarsi su
-- una banca dati non ancora convertita, ed è la classe di guasto peggiore: una migrazione che non parte.

CREATE TABLE platform.app_access (
    id          uuid        PRIMARY KEY,
    tenant_id   varchar(64) NOT NULL,                                   -- discriminatore di account (invariante #2)
    app_id      uuid        NOT NULL REFERENCES platform.app (id),
    identity_id uuid        NOT NULL REFERENCES platform.identity (id),
    role        varchar(16) NOT NULL,                                   -- viewer | editor | admin
    granted_by  uuid,                                                   -- identità di chi ha concesso (traccia di controllo)
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    created_by  varchar(64),
    updated_by  varchar(64),
    deleted_at  timestamptz,
    CONSTRAINT ck_app_access_role CHECK (role IN ('viewer', 'editor', 'admin'))
);

-- Nessuna chiave esterna dal discriminatore verso platform.accounts, coerentemente con ogni altra
-- tabella di account: il tenant_id è una chiave logica governata dal token, non una chiave esterna.

-- Un solo accesso per terna, sulle sole righe VIVE: revocare e riconcedere deve essere possibile, e con
-- un vincolo incondizionato la revoca chiuderebbe la porta per sempre.
CREATE UNIQUE INDEX ux_app_access_tenant_app_identity
    ON platform.app_access (tenant_id, app_id, identity_id)
    WHERE deleted_at IS NULL;

-- «Quali applicazioni vede questa persona?» — la domanda del menu laterale, fatta a ogni caricamento.
CREATE INDEX ix_app_access_tenant_identity ON platform.app_access (tenant_id, identity_id);

-- «Chi ha accesso a questa applicazione?» — la domanda della schermata di gestione utenti (UC 0111).
CREATE INDEX ix_app_access_tenant_app ON platform.app_access (tenant_id, app_id);

COMMENT ON TABLE platform.app_access IS
    'UC 0098 — accesso di una persona a una applicazione, con il ruolo (viewer|editor|admin). Entità di '
    'account (porta tenant_id). L''OWNER non ha righe qui: l''accesso gli è implicito su tutte le '
    'applicazioni dell''account, e ogni lettura di «chi ha accesso» lo aggiunge al risultato.';

COMMENT ON COLUMN platform.app_access.identity_id IS
    'Identità della persona (platform.identity), non la sua appartenenza: l''appartenenza cambia nel '
    'tempo, l''identità no.';

COMMENT ON COLUMN platform.app_access.granted_by IS
    'Identità di chi ha concesso l''accesso: serve alla traccia di controllo, non all''autorizzazione.';
