-- UC 0039 (change 0052) — newsletter con double opt-in + registro consensi (art. 7).
--
-- newsletter_subscriber: iscritti alla newsletter. PLATFORM-LEVEL (NON tenant-scoped): l'iscrizione
-- dal sito vetrina arriva senza JWT, quindi senza tenant_id — come webhook_event (V3). Una riga per
-- indirizzo (unicità su lower(email)). Lo stato governa il double opt-in: 'pending' non riceve
-- marketing, 'confirmed' sì, 'unsubscribed' è la revoca. Il token di conferma è salvato solo come
-- hash SHA-256 (single-use, come gli inviti). Dato personale: email (finalità marketing diretto,
-- base consenso art. 6.1.a, retention iscritto + 24 mesi post-disiscrizione, #13 E/F29).
--
-- consent_event: registro APPEND-ONLY della prova di consenso/conferma/revoca (art. 7). Minimizzato:
-- tipo + versione del testo + canale + marcatempo, NESSUN IP/user-agent. Nessun dato identificativo
-- diretto: il legame all'email passa dal solo subscriber_id.

CREATE TABLE platform.newsletter_subscriber (
    id                 uuid          PRIMARY KEY,
    email              varchar(320)  NOT NULL,
    status             varchar(16)   NOT NULL,   -- pending | confirmed | unsubscribed
    locale             varchar(8)    NOT NULL,
    origin_channel     varchar(16)   NOT NULL,   -- site | signup | account | email
    user_id            uuid,                     -- provenienza (toggle account); NON base del legame GDPR
    confirm_token_hash varchar(64),              -- SHA-256 hex del token di conferma; null dopo la conferma
    confirm_expires_at timestamptz,
    confirmed_at       timestamptz,
    unsubscribed_at    timestamptz,
    created_at         timestamptz   NOT NULL,
    updated_at         timestamptz   NOT NULL,
    created_by         varchar(64),
    updated_by         varchar(64),
    deleted_at         timestamptz
);
-- una sola riga per indirizzo (case-insensitive)
CREATE UNIQUE INDEX ux_newsletter_subscriber_email ON platform.newsletter_subscriber (lower(email));
-- lookup del double opt-in per hash del token
CREATE INDEX ix_newsletter_subscriber_confirm_token ON platform.newsletter_subscriber (confirm_token_hash);

CREATE TABLE platform.consent_event (
    id                   uuid          PRIMARY KEY,
    subscriber_id        uuid          NOT NULL REFERENCES platform.newsletter_subscriber (id),
    event_type           varchar(16)   NOT NULL,   -- grant | confirm | revoke
    consent_text_version varchar(64)   NOT NULL,
    channel              varchar(16)   NOT NULL,   -- site | signup | account | email
    occurred_at          timestamptz   NOT NULL,
    created_at           timestamptz   NOT NULL,
    updated_at           timestamptz   NOT NULL,
    created_by           varchar(64),
    updated_by           varchar(64),
    deleted_at           timestamptz
);
CREATE INDEX ix_consent_event_subscriber ON platform.consent_event (subscriber_id);
