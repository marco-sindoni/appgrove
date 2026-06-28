-- UC 0025 (change 0020) — hardening pipeline webhook: dedup su event_id + guardia out-of-order.
--
-- webhook_event: log di pipeline (NON tenant-scoped: dedup globale per event_id, audit/osservabilità).
-- Il consumer inserisce qui (ON CONFLICT (event_id) DO NOTHING) NELLA STESSA transazione in cui applica
-- l'evento a subscription/accounts → idempotenza corretta anche sotto redelivery/concorrenza (#09 D18b).
CREATE TABLE platform.webhook_event (
    id           uuid         PRIMARY KEY,
    event_id     varchar(64)  NOT NULL,   -- Paddle event_id (dedup)
    event_type   varchar(64)  NOT NULL,
    occurred_at  timestamptz  NOT NULL,
    tenant_id    varchar(64),             -- audit (no-PII): account dell'evento, se applicabile
    app_id       uuid,                    -- audit: app dell'evento, se applicabile
    outcome      varchar(32)  NOT NULL,   -- received | processed | skipped_stale
    received_at  timestamptz  NOT NULL,
    processed_at timestamptz
);
-- dedup: lo stesso event_id si applica una sola volta (#09 D18b).
CREATE UNIQUE INDEX ux_webhook_event_event_id ON platform.webhook_event (event_id);
CREATE INDEX ix_webhook_event_app ON platform.webhook_event (app_id);

-- out-of-order (#09 D18c): timestamp dell'ultimo evento applicato alla subscription. Un evento con
-- occurred_at più vecchio non sovrascrive uno stato più recente (guardia nel DO UPDATE del consumer).
ALTER TABLE platform.subscription ADD COLUMN last_event_occurred_at timestamptz;
