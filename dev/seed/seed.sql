-- ─────────────────────────────────────────────────────────────────────────────
-- appgrove — SEED deterministico (UC 0011), condiviso dev↔E2E.
--
-- Proprietà:
--  * IDEMPOTENTE: INSERT ... ON CONFLICT (id) DO UPDATE → ri-esecuzione = stesso stato.
--  * DETERMINISTICO: UUID e timestamp FISSI (niente now()/gen_random_uuid()).
--  * SINTETICO: email *.test, nessun PII reale (#08/#13/#10 I33).
--
-- Cast e ID stabili sono documentati in dev/seed/README.md (gli E2E ci asseriscono sopra).
-- Caricato da `dev seed` (psql) dopo `dev migrate`; validato da services/core SeedDataTest.
-- ─────────────────────────────────────────────────────────────────────────────

-- Timestamp di audit fisso per tutte le righe (determinismo).
-- (usato inline come '2024-01-01T00:00:00Z')

-- ── accounts (radice tenant: id = tenant_id) ─────────────────────────────────
INSERT INTO platform.accounts (id, name, status, paddle_customer_id, created_at, updated_at, created_by) VALUES
  ('a0000000-0000-4000-8000-000000000001', 'Acme Corp',         'active', NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('a0000000-0000-4000-8000-000000000002', 'Bob Personal',      'active', NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('a0000000-0000-4000-8000-000000000003', 'Appgrove Platform', 'active', NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name, status = EXCLUDED.status,
  paddle_customer_id = EXCLUDED.paddle_customer_id, updated_at = EXCLUDED.updated_at;

-- ── users (tenant-scoped; membership foldata) ────────────────────────────────
-- Acme: owner + admin + member · Bob: owner · Platform: utente piattaforma
-- (il gruppo JWT 'platform-admin' è assegnato dall'auth locale, UC 0010).
INSERT INTO platform.users (id, tenant_id, cognito_sub, email, display_name, role, status, created_at, updated_at, created_by) VALUES
  ('b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'seed-acme-owner',     'owner@acme.test',      'Acme Owner',     'owner',  'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'seed-acme-admin',     'admin@acme.test',      'Acme Admin',     'admin',  'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('b0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001', 'seed-acme-member',    'member@acme.test',     'Acme Member',    'member', 'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('b0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000002', 'seed-bob-owner',      'bob@bob.test',         'Bob',            'owner',  'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('b0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000003', 'seed-platform-admin', 'admin@appgrove.test',  'Platform Admin', 'owner',  'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed')
ON CONFLICT (id) DO UPDATE SET
  tenant_id = EXCLUDED.tenant_id, cognito_sub = EXCLUDED.cognito_sub, email = EXCLUDED.email,
  display_name = EXCLUDED.display_name, role = EXCLUDED.role, status = EXCLUDED.status,
  updated_at = EXCLUDED.updated_at;

-- ── invitations (Acme, pending) ──────────────────────────────────────────────
-- token_hash = SHA-256(hex) dei token fissi documentati nel README.
--   admin  → token 'seed-invite-acme-admin'
--   member → token 'seed-invite-acme-member'
INSERT INTO platform.invitations (id, tenant_id, email, role, token_hash, status, expires_at, invited_by, accepted_user_id, created_at, updated_at, created_by) VALUES
  ('c0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'invitee-admin@acme.test',  'admin',  '1ddd1a3f17c576bf0a17e22bdd4e136384a7d55d49bbbf53e58c11111b15ffb0', 'pending', '2999-12-31T00:00:00Z', 'b0000000-0000-4000-8000-000000000001', NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('c0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'invitee-member@acme.test', 'member', 'cc3424114feeb02469d842b816874ebd9844167135eda959e841d1e09191cd45', 'pending', '2999-12-31T00:00:00Z', 'b0000000-0000-4000-8000-000000000001', NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed')
ON CONFLICT (id) DO UPDATE SET
  tenant_id = EXCLUDED.tenant_id, email = EXCLUDED.email, role = EXCLUDED.role,
  token_hash = EXCLUDED.token_hash, status = EXCLUDED.status, expires_at = EXCLUDED.expires_at,
  invited_by = EXCLUDED.invited_by, accepted_user_id = EXCLUDED.accepted_user_id, updated_at = EXCLUDED.updated_at;

-- ── catalogo: app (platform-level) ───────────────────────────────────────────
-- notes = single-user · teams = multi-user · legacy = multi-user DISABILITATA (status inactive)
-- fatture = app #1 reale (B2C single-user, UC 0051).
INSERT INTO platform.app (id, slug, name, user_model, status, paddle_product_id, created_at, updated_at, created_by) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'notes',   'Notes',   'single_user', 'active',   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('d0000000-0000-4000-8000-000000000002', 'teams',   'Teams',   'multi_user',  'active',   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('d0000000-0000-4000-8000-000000000003', 'legacy',  'Legacy',  'multi_user',  'inactive', NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('d0000000-0000-4000-8000-000000000004', 'fatture', 'Fatture', 'single_user', 'active',   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed')
ON CONFLICT (id) DO UPDATE SET
  slug = EXCLUDED.slug, name = EXCLUDED.name, user_model = EXCLUDED.user_model,
  status = EXCLUDED.status, paddle_product_id = EXCLUDED.paddle_product_id, updated_at = EXCLUDED.updated_at;

-- ── catalogo: app_tier (limits jsonb: metrica/finestra/tetto, flow|stock) ─────
INSERT INTO platform.app_tier (id, app_id, key, name, limits, features, trial_days, created_at, updated_at, created_by) VALUES
  ('e0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', 'free', 'Notes Free', '{"metric":"notes","window":"month","cap":100,"type":"flow"}'::jsonb,   '{}'::jsonb, 14, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('e0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000001', 'pro',  'Notes Pro',  '{"metric":"notes","window":"month","cap":10000,"type":"flow"}'::jsonb, '{}'::jsonb, 0,  '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('e0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000002', 'team', 'Teams',      '{"metric":"seats","cap":10,"type":"stock"}'::jsonb,                   '{}'::jsonb, 14, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('e0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000003', 'std',  'Legacy Std',   '{"metric":"items","window":"month","cap":1000,"type":"flow"}'::jsonb,  '{}'::jsonb, 0,  '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('e0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000004', 'free', 'Fatture Free', '{"metric":"fatture","window":"month","cap":10,"type":"flow"}'::jsonb,   '{}'::jsonb, 0,  '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed')
ON CONFLICT (id) DO UPDATE SET
  app_id = EXCLUDED.app_id, key = EXCLUDED.key, name = EXCLUDED.name, limits = EXCLUDED.limits,
  features = EXCLUDED.features, trial_days = EXCLUDED.trial_days, updated_at = EXCLUDED.updated_at;

-- ── catalogo: app_price (monthly + annual, minor units, EUR) ──────────────────
INSERT INTO platform.app_price (id, app_tier_id, billing_cycle, paddle_price_id, amount, currency, created_at, updated_at, created_by) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'e0000000-0000-4000-8000-000000000002', 'monthly', NULL,  900, 'EUR', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('f0000000-0000-4000-8000-000000000002', 'e0000000-0000-4000-8000-000000000002', 'annual',  NULL, 9000, 'EUR', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('f0000000-0000-4000-8000-000000000003', 'e0000000-0000-4000-8000-000000000003', 'monthly', NULL, 1900, 'EUR', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('f0000000-0000-4000-8000-000000000004', 'e0000000-0000-4000-8000-000000000003', 'annual',  NULL,19000, 'EUR', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed')
ON CONFLICT (id) DO UPDATE SET
  app_tier_id = EXCLUDED.app_tier_id, billing_cycle = EXCLUDED.billing_cycle,
  paddle_price_id = EXCLUDED.paddle_price_id, amount = EXCLUDED.amount, currency = EXCLUDED.currency,
  updated_at = EXCLUDED.updated_at;

-- ── subscription (tenant↔app, stati di lifecycle vari → entitlement derivato) ─
--  Acme→teams  active     · Acme→notes  past_due
--  Bob →notes  trialing   · Acme→legacy active (ma app 'legacy' inactive → gate "app abilitata")
--  Bob →teams  canceled
INSERT INTO platform.subscription (id, tenant_id, app_id, app_tier_id, status, current_period_start, current_period_end, cancel_at, trial_end, paddle_subscription_id, created_at, updated_at, created_by) VALUES
  ('10000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000002', 'e0000000-0000-4000-8000-000000000003', 'active',   '2024-06-01T00:00:00Z', '2024-07-01T00:00:00Z', NULL,                   NULL,                   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('10000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', 'e0000000-0000-4000-8000-000000000002', 'past_due', '2024-06-01T00:00:00Z', '2024-07-01T00:00:00Z', NULL,                   NULL,                   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('10000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000001', 'e0000000-0000-4000-8000-000000000001', 'trialing', '2024-06-01T00:00:00Z', '2024-07-01T00:00:00Z', NULL,                   '2999-12-31T00:00:00Z', NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('10000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000003', 'e0000000-0000-4000-8000-000000000004', 'active',   '2024-06-01T00:00:00Z', '2024-07-01T00:00:00Z', NULL,                   NULL,                   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('10000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000002', 'e0000000-0000-4000-8000-000000000003', 'canceled', '2024-06-01T00:00:00Z', '2024-07-01T00:00:00Z', '2024-06-15T00:00:00Z', NULL,                   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed')
ON CONFLICT (id) DO UPDATE SET
  tenant_id = EXCLUDED.tenant_id, app_id = EXCLUDED.app_id, app_tier_id = EXCLUDED.app_tier_id,
  status = EXCLUDED.status, current_period_start = EXCLUDED.current_period_start,
  current_period_end = EXCLUDED.current_period_end, cancel_at = EXCLUDED.cancel_at,
  trial_end = EXCLUDED.trial_end, paddle_subscription_id = EXCLUDED.paddle_subscription_id,
  updated_at = EXCLUDED.updated_at;
