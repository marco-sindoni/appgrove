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

-- ── catalogo + subscription: NON sono in questo file ─────────────────────────
-- • CATALOGO (app/app_tier/app_price): con il pricing-as-code (UC 0022, "Strada 1") è prodotto dal LOADER
--   dagli YAML in services/core/src/main/resources/pricing/, con UUID DETERMINISTICI dalla chiave stabile
--   (CatalogIds: UUIDv3 su 'app:<slug>' / 'tier:<slug>:<key>' / 'price:<slug>:<key>:<cycle>').
-- • SUBSCRIPTION (dipendono dal catalogo via FK): vivono in `seed-subscriptions.sql`, applicato SOLO dove il
--   catalogo esiste (core @QuarkusTest, dev/E2E dopo `sync-pricing`). I servizi di sola identità (auth)
--   applicano SOLO questo seed.sql. Vedi dev/seed/README.md.
