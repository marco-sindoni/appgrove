-- Proiezione locale in sola lettura degli entitlement (UC 0046).
--
-- Sostituisce, sul percorso caldo, la chiamata sincrona app→core di UC 0027: l'app legge i propri
-- diritti dal proprio schema e interpella core solo come rete di sicurezza (riga assente o marcata
-- da rinfrescare). Non è una fonte di verità: è la cache di una verità che vive in core.
--
-- La tabella è POPOLATA DALL'APP (dopo un rinfresco riuscito) e INVALIDATA dal consumer della coda
-- entitlement-fatture. Nessuna lettura cross-schema: l'app non tocca mai lo schema di core.

create table if not exists app_fatture.entitlement_projection (
    tenant_id      varchar(64)  not null,
    app_slug       varchar(64)  not null,
    -- EntitlementView serializzata (tier, fase, accessUntil, tetti per metrica).
    -- NULL = diniego noto: il tenant NON ha accesso. Distinguerlo dall'assenza di riga evita di
    -- rifare la chiamata di rete a ogni richiesta di chi non ha accesso.
    entitlement    jsonb,
    -- true = un evento ha invalidato la riga e serve un rinfresco. La riga NON viene cancellata:
    -- il valore vecchio resta l'ultima verità nota se al rinfresco core non risponde.
    stale          boolean      not null default false,
    refreshed_at   timestamptz  not null,
    invalidated_at timestamptz,
    primary key (tenant_id, app_slug)
);

-- Nessun dato personale: la riga contiene identificativi di account/app e tetti di quota.
comment on table app_fatture.entitlement_projection is
    'Proiezione locale degli entitlement (UC 0046): cache dei diritti, la fonte di verita resta core.';
