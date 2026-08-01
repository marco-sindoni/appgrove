-- UC 0096 (change 0077) — storico pagamenti e ricevute della pagina Billing.
--
-- Le transazioni del fornitore di pagamento (Paddle, venditore di record) vengono PERSISTITE dalla stessa
-- pipeline webhook che tiene aggiornata `subscription`: la pagina deve poter elencare TUTTE le transazioni
-- del conto — comprese quelle fallite — senza dipendere dalla raggiungibilità del fornitore nel momento in
-- cui qualcuno apre la pagina (decisione 3, change 0077).
--
-- Tenant-scoped come `subscription`: entità JPA con filtro `WHERE tenant_id` automatico in lettura; la
-- scrittura è del consumer webhook, che gira fuori da una richiesta autenticata e usa SQL nativo con il
-- tenant preso dai dati personalizzati del payload FIRMATO (fiducia dalla firma, non da input del client).
CREATE TABLE platform.billing_transaction (
    id                     uuid        PRIMARY KEY,
    tenant_id              varchar(64) NOT NULL,
    app_id                 uuid        REFERENCES platform.app (id),
    app_tier_id            uuid        REFERENCES platform.app_tier (id),
    paddle_transaction_id  varchar(64) NOT NULL,   -- riferimento della transazione presso il fornitore
    status                 varchar(32) NOT NULL,   -- paid | failed | disputed
    amount                 integer     NOT NULL,   -- unità minori (centesimi), come il resto del listino
    currency               varchar(3)  NOT NULL,
    billing_cycle          varchar(16),            -- etichetta da mostrare (monthly | annual | …), mai un enum
    receipt_url            text,                   -- ricevuta del fornitore; NULL = non ancora disponibile
    billed_at              timestamptz NOT NULL,
    last_event_occurred_at timestamptz,            -- guardia out-of-order, come su `subscription`
    created_at             timestamptz NOT NULL,
    updated_at             timestamptz NOT NULL,
    created_by             varchar(64),
    updated_by             varchar(64),
    deleted_at             timestamptz
);

-- Idempotenza della scrittura: la stessa transazione del fornitore si registra una volta sola, anche se
-- più eventi la riguardano (pagamento fallito poi riuscito, contestazione successiva).
CREATE UNIQUE INDEX ux_billing_transaction_paddle
    ON platform.billing_transaction (paddle_transaction_id);

-- Lettura della pagina: le transazioni di un conto, dalla più recente.
CREATE INDEX ix_billing_transaction_tenant
    ON platform.billing_transaction (tenant_id, billed_at DESC);
