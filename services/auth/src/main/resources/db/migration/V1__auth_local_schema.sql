-- UC 0058 — stato auth DEV-ONLY (emulazione Cognito in locale).
-- Schema SEPARATO da platform: in prod password/2FA stanno in Cognito, non nel nostro DB.
-- Storia Flyway propria (auth_local.flyway_schema_history); non tocca lo schema platform.
CREATE SCHEMA IF NOT EXISTS auth_local;

CREATE TABLE auth_local.credentials (
    cognito_sub    varchar(128) PRIMARY KEY,
    password_hash  varchar(255) NOT NULL,
    email_verified boolean      NOT NULL DEFAULT false,
    totp_secret    varchar(128),
    totp_enabled   boolean      NOT NULL DEFAULT false,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);
