-- Uso a giacenza per-app materializzato in core (UC 0054).
--
-- Chiude, limitatamente al gate del downgrade, il punto aperto di UC 0028 "gate stock del downgrade
-- contro l'uso reale": TierChangePolicy.evaluateDowngrade riceveva una mappa d'uso VUOTA, quindi a
-- runtime non bloccava mai. Ora le app pubblicano la propria giacenza sul bus interno (coda condivisa
-- `app-usage`, commons/UsageEvents) e core la materializza qui; il gate la legge da questa tabella.
--
-- NON è una fonte di verità: la verità sui posti occupati resta nell'app (schema `app_<slug>`). Questa
-- è la sua proiezione lato core, aggiornata per eventi — l'immagine speculare della proiezione locale
-- degli entitlement (UC 0046), che va nel verso opposto (core → app).

create table platform.app_usage_stock (
    app_slug    varchar(64)  not null,
    tenant_id   varchar(64)  not null,
    metric      varchar(64)  not null,
    -- giacenza attuale riportata dall'app: quante unità della metrica esistono ORA per il tenant.
    value       bigint       not null,
    -- istante della misura riportato dall'app (diagnostica del ritardo di propagazione).
    reported_at timestamptz  not null,
    -- quando core ha applicato il report (utile a distinguere "vecchio ma noto" da "mai ricevuto").
    updated_at  timestamptz  not null default now(),
    primary key (app_slug, tenant_id, metric)
);

-- Nessun dato personale: identificativi di app/account e un conteggio.
comment on table platform.app_usage_stock is
    'Uso a giacenza per-app (UC 0054): proiezione in core del conteggio che vive nell app, per il gate downgrade.';
