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

-- ── identity (persone: entità di PIATTAFORMA, nessun tenant_id) ──────────────
-- UC 0116: la persona è unica sulla piattaforma (indirizzo e identificativo di autenticazione unici
-- globalmente). Gli id sono gli stessi che avevano le righe utente di prima della change 0088 — così
-- `invitations.invited_by` e ogni altro riferimento memorizzato continuano a puntare alla persona giusta.
INSERT INTO platform.identity (id, cognito_sub, email, display_name, locale, status, created_at, updated_at, created_by) VALUES
  ('b0000000-0000-4000-8000-000000000001', 'seed-acme-owner',     'owner@acme.test',      'Acme Owner',     'en', 'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('b0000000-0000-4000-8000-000000000002', 'seed-acme-admin',     'admin@acme.test',      'Acme Admin',     'en', 'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('b0000000-0000-4000-8000-000000000003', 'seed-acme-member',    'member@acme.test',     'Acme Member',    'en', 'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('b0000000-0000-4000-8000-000000000004', 'seed-bob-owner',      'bob@bob.test',         'Bob',            'en', 'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('b0000000-0000-4000-8000-000000000005', 'seed-platform-admin', 'admin@appgrove.test',  'Platform Admin', 'en', 'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed')
ON CONFLICT (id) DO UPDATE SET
  cognito_sub = EXCLUDED.cognito_sub, email = EXCLUDED.email, display_name = EXCLUDED.display_name,
  locale = EXCLUDED.locale, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at;

-- ── membership (appartenenze: entità di ACCOUNT, porta tenant_id) ────────────
-- Acme: owner + admin + member · Bob: owner · Platform: persona di piattaforma
-- (il gruppo JWT 'platform-admin' è assegnato dall'auth locale, UC 0010).
-- Tutte le persone del seme hanno UNA sola appartenenza: è il caso di tutti gli utenti di oggi, e il
-- seme deve restare la fotografia del caso normale. Il caso «una persona, due account» si costruisce
-- nei collaudi, non qui.
INSERT INTO platform.membership (id, tenant_id, identity_id, role, status, created_at, updated_at, created_by) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'owner',  'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('d0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002', 'admin',  'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('d0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000003', 'member', 'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('d0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000004', 'owner',  'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('d0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000005', 'owner',  'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed')
-- L'arbitro del conflitto è il vincolo VERO dell'appartenenza — (tenant_id, identity_id) sulle righe
-- vive — e non la chiave primaria. Con `ON CONFLICT (id)` il ri-seme falliva con
-- «duplicate key ... ux_membership_tenant_identity» su ogni banca dati che aveva già ATTRAVERSATO la
-- migrazione V17: il travaso conia identificativi nuovi (gen_random_uuid()), quindi la coppia esiste già
-- con un id diverso dal nostro, l'inserimento non conflitta sulla chiave primaria e sbatte sull'indice.
-- Difetto lasciato dalla change 0088 e corretto qui (change 0090). Conseguenza accettata: su una banca
-- dati migrata gli identificativi delle appartenenze restano quelli del travaso e non i d0000000-… di
-- questo file — nessuno vi si appoggia, e la persona giusta nell'account giusto è ciò che conta.
ON CONFLICT (tenant_id, identity_id) WHERE deleted_at IS NULL DO UPDATE SET
  role = EXCLUDED.role, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at;

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
