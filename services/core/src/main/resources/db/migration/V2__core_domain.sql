-- UC 0013 — modello dati del core/platform.
-- accounts/users/invitations: entità di dominio (con entità JPA + API).
-- Catalogo (app/app_tier/app_price) e subscription: SOLO struttura (DDL); le entità JPA
-- arrivano dagli UC proprietari (0022 catalogo, 0025 subscription). Cfr. change 0007.
--
-- Nota multitenancy: tenant_id è il discriminatore (varchar(64), invariante #2) e contiene
-- l'UUID dell'account (accounts.id = tenant_id). Non è una FK (tipo varchar vs uuid): è la
-- chiave logica di tenant gestita da Hibernate DISCRIMINATOR.

-- ── accounts (radice tenant; accounts.id = tenant_id, NON tenant-scoped via discriminator) ──
CREATE TABLE platform.accounts (
    id                 uuid         PRIMARY KEY,
    name               varchar(255) NOT NULL,
    status             varchar(32)  NOT NULL DEFAULT 'active',
    paddle_customer_id varchar(64),
    created_at         timestamptz  NOT NULL,
    updated_at         timestamptz  NOT NULL,
    created_by         varchar(64),
    updated_by         varchar(64),
    deleted_at         timestamptz
);

-- ── users (tenant-scoped; membership foldata 1 utente→1 tenant) ──
CREATE TABLE platform.users (
    id           uuid         PRIMARY KEY,
    tenant_id    varchar(64)  NOT NULL,
    cognito_sub  varchar(128) NOT NULL,
    email        varchar(320) NOT NULL,
    display_name varchar(255),
    role         varchar(32)  NOT NULL,
    status       varchar(32)  NOT NULL DEFAULT 'active',
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL,
    created_by   varchar(64),
    updated_by   varchar(64),
    deleted_at   timestamptz
);
-- cognito_sub ed email sono unici GLOBALMENTE (cross-tenant): enforce "1 utente→1 tenant" (#02 14).
CREATE UNIQUE INDEX ux_users_cognito_sub ON platform.users (cognito_sub);
CREATE UNIQUE INDEX ux_users_email ON platform.users (lower(email));
CREATE INDEX ix_users_tenant ON platform.users (tenant_id);

-- ── invitations (tenant-scoped; token single-use) ──
CREATE TABLE platform.invitations (
    id               uuid         PRIMARY KEY,
    tenant_id        varchar(64)  NOT NULL,
    email            varchar(320) NOT NULL,
    role             varchar(32)  NOT NULL,
    token_hash       varchar(128) NOT NULL,
    status           varchar(32)  NOT NULL DEFAULT 'pending',
    expires_at       timestamptz  NOT NULL,
    invited_by       uuid,
    accepted_user_id uuid,
    created_at       timestamptz  NOT NULL,
    updated_at       timestamptz  NOT NULL,
    created_by       varchar(64),
    updated_by       varchar(64),
    deleted_at       timestamptz
);
CREATE UNIQUE INDEX ux_invitations_token ON platform.invitations (token_hash);
CREATE INDEX ix_invitations_tenant ON platform.invitations (tenant_id);

-- ── catalogo (SOLO DDL, platform-level; popolato da UC 0022, entità JPA in UC 0022) ──
CREATE TABLE platform.app (
    id                uuid         PRIMARY KEY,
    slug              varchar(64)  NOT NULL,
    name              varchar(255) NOT NULL,
    user_model        varchar(32)  NOT NULL,   -- single_user | multi_user
    status            varchar(32)  NOT NULL DEFAULT 'active',
    paddle_product_id varchar(64),
    created_at        timestamptz  NOT NULL,
    updated_at        timestamptz  NOT NULL,
    created_by        varchar(64),
    updated_by        varchar(64),
    deleted_at        timestamptz
);
CREATE UNIQUE INDEX ux_app_slug ON platform.app (slug);

CREATE TABLE platform.app_tier (
    id         uuid         PRIMARY KEY,
    app_id     uuid         NOT NULL REFERENCES platform.app (id),
    key        varchar(64)  NOT NULL,
    name       varchar(255) NOT NULL,
    limits     jsonb,
    features   jsonb,
    trial_days integer      NOT NULL DEFAULT 0,
    created_at timestamptz  NOT NULL,
    updated_at timestamptz  NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    deleted_at timestamptz
);
CREATE INDEX ix_app_tier_app ON platform.app_tier (app_id);
CREATE UNIQUE INDEX ux_app_tier_app_key ON platform.app_tier (app_id, key);

CREATE TABLE platform.app_price (
    id              uuid        PRIMARY KEY,
    app_tier_id     uuid        NOT NULL REFERENCES platform.app_tier (id),
    billing_cycle   varchar(16) NOT NULL,   -- monthly | annual
    paddle_price_id varchar(64),
    amount          integer     NOT NULL,   -- minor units
    currency        varchar(3)  NOT NULL,
    created_at      timestamptz NOT NULL,
    updated_at      timestamptz NOT NULL,
    created_by      varchar(64),
    updated_by      varchar(64),
    deleted_at      timestamptz
);
CREATE INDEX ix_app_price_tier ON platform.app_price (app_tier_id);
CREATE UNIQUE INDEX ux_app_price_tier_cycle ON platform.app_price (app_tier_id, billing_cycle);

-- ── subscription (SOLO DDL, tenant-scoped; popolata da UC 0025, entità JPA in UC 0025) ──
CREATE TABLE platform.subscription (
    id                     uuid        PRIMARY KEY,
    tenant_id              varchar(64) NOT NULL,
    app_id                 uuid        NOT NULL REFERENCES platform.app (id),
    app_tier_id            uuid        REFERENCES platform.app_tier (id),
    status                 varchar(32) NOT NULL,   -- trialing | active | past_due | canceled | paused
    current_period_start   timestamptz,
    current_period_end     timestamptz,
    cancel_at              timestamptz,
    trial_end              timestamptz,
    paddle_subscription_id varchar(64),
    created_at             timestamptz NOT NULL,
    updated_at             timestamptz NOT NULL,
    created_by             varchar(64),
    updated_by             varchar(64),
    deleted_at             timestamptz
);
CREATE INDEX ix_subscription_tenant ON platform.subscription (tenant_id);
CREATE UNIQUE INDEX ux_subscription_tenant_app ON platform.subscription (tenant_id, app_id)
    WHERE deleted_at IS NULL;
