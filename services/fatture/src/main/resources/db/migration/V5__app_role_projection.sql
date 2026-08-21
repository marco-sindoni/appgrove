-- Copia locale del RUOLO della persona su questa applicazione (UC 0099).
--
-- È la gemella di entitlement_projection (UC 0046) e risponde all'altra domanda: non «l'account ha
-- diritto a questa applicazione» ma «questa persona che potere ha su di essa». Il ruolo NON sta nel
-- token, ed è la decisione centrale di UC 0099: nel token un cambio di ruolo avrebbe effetto solo al
-- rinnovo, e un account con dieci applicazioni gonfierebbe ogni richiesta. Il prezzo è questa copia.
--
-- POPOLATA DALL'APP dopo un rinfresco riuscito da core (GET /api/platform/v1/me/app-access) e
-- INVALIDATA dal consumer della coda entitlement-fatture, la stessa dei diritti d'accesso: una coda
-- per servizio, il tipo di evento nel messaggio. Nessuna lettura cross-schema.
--
-- UNA DIFFERENZA VOLUTA rispetto alla copia dei diritti: questa SCADE
-- (appgrove.app-role.projection.max-age, sessanta secondi). Un abbonamento cambia di rado e l'evento
-- basta; un ruolo cambia spesso e un evento perso qui significa un permesso revocato che sopravvive.

create table if not exists app_fatture.app_role_projection (
    tenant_id      varchar(64)  not null,
    -- Identificativo di AUTENTICAZIONE della persona (claim `sub` del token verificato): nessuna
    -- email, nessun nome. È la copia di un dato già dichiarato nel manifesto della piattaforma
    -- (platform.app_access.identity_id) e viene cancellata fisicamente con l'account (#13 L70).
    subject        varchar(128) not null,
    app_slug       varchar(64)  not null,
    -- Ruolo sull'applicazione: viewer | editor | admin. L'owner dell'account arriva qui come `admin`
    -- (accesso implicito, UC 0098 §5). NULL = diniego noto: la persona NON ha accesso. Distinguerlo
    -- dall'assenza di riga evita di rifare la chiamata di rete a ogni richiesta di chi non ha accesso.
    role           varchar(16),
    -- true = un evento ha invalidato la riga e serve un rinfresco. La riga NON viene cancellata: il
    -- valore vecchio resta l'ultima verità nota se al rinfresco core non risponde (ma scade).
    stale          boolean      not null default false,
    refreshed_at   timestamptz  not null,
    invalidated_at timestamptz,
    primary key (tenant_id, subject, app_slug),
    constraint app_role_projection_role_check
        check (role is null or role in ('viewer', 'editor', 'admin'))
);

comment on table app_fatture.app_role_projection is
    'Copia locale del ruolo per applicazione (UC 0099): cache di un permesso, la fonte di verita resta core.';
