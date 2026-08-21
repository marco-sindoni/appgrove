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
-- Acme: owner + due member · Bob: owner · Platform: persona di piattaforma
-- UC 0098: il ruolo di piattaforma ha DUE soli valori (owner | member). La persona `admin@acme.test`
-- resta nel seme e resta la «amministratrice», ma il suo potere ora sta su una applicazione: è
-- `member` di piattaforma con ruolo `admin` sul Mini-CRM (vedi il blocco app_access qui sotto). È la
-- stessa traduzione che UC 0113 applicherà ai dati reali, fatta qui perché un seme che dichiara un
-- ruolo che l'enumerazione non ammette più non si caricherebbe nemmeno.
-- (il gruppo JWT 'platform-admin' è assegnato dall'auth locale, UC 0010).
-- Tutte le persone del seme hanno UNA sola appartenenza: è il caso di tutti gli utenti di oggi, e il
-- seme deve restare la fotografia del caso normale. Il caso «una persona, due account» si costruisce
-- nei collaudi, non qui.
-- Prima di reinserire: si liberano le righe del SEME rimaste CANCELLATE LOGICAMENTE. Serve perché
-- l'arbitro scelto qui sotto — l'indice unico su (tenant_id, identity_id) — è PARZIALE sulle righe vive:
-- una riga morta non lo attiva, quindi l'inserimento non trova conflitto e sbatte invece sulla chiave
-- primaria («duplicate key ... membership_pkey»), che nessun ON CONFLICT copre. Il caso si crea appena un
-- collaudo rimuove una persona dall'account — cioè proprio quando si vuole rimettere il seme come prima —
-- e faceva fallire `./dev.sh seed` lasciando la persona fuori dall'account (trovato collaudando la change
-- 0091, corretto qui). Il filtro su `created_by = 'seed'` è ciò che rende la cancellazione lecita: si
-- tolgono solo righe che questo file ha creato, mai quelle nate da un collaudo o da un utente. La
-- cancellazione fisica è sicura: l'unico riferimento a `membership.id` è `identity.active_membership_id`,
-- che è `ON DELETE SET NULL` (change 0089, decisione 15).
DELETE FROM platform.membership
 WHERE created_by = 'seed' AND deleted_at IS NOT NULL;

INSERT INTO platform.membership (id, tenant_id, identity_id, role, status, created_at, updated_at, created_by) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'owner',  'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('d0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002', 'member', 'active', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
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

-- ── app_access (accessi per applicazione: entità di ACCOUNT) ─────────────────
-- UC 0098: qui vive «questa persona può usare questa applicazione con questo ruolo».
-- L'OWNER non ha righe: l'accesso gli è implicito su tutte le applicazioni dell'account.
-- L'app_id si legge dal CATALOGO, che non sta in questo file (è prodotto dal loader del
-- pricing-as-code con identificativi deterministici): la forma `INSERT … SELECT` fa sì che dove il
-- catalogo non esiste — i servizi di sola identità applicano solo questo seed.sql — non venga inserito
-- nulla, invece di fallire.
-- Stessa cura delle appartenenze, e per la stessa ragione: l'indice unico è PARZIALE sulle righe vive,
-- quindi una riga REVOCATA occupa la chiave primaria senza attivare l'ON CONFLICT, e l'inserimento sbatte
-- su «duplicate key ... app_access_pkey». Qui il caso è ancora più facile da incontrare: revocare un
-- accesso è un'operazione ordinaria (e la revoca è per definizione logica), quindi bastava un collaudo
-- del §4 di UC 0098 per rendere `./dev.sh seed` non più ripetibile.
DELETE FROM platform.app_access
 WHERE created_by = 'seed' AND deleted_at IS NOT NULL;

INSERT INTO platform.app_access (id, tenant_id, app_id, identity_id, role, granted_by, created_at, updated_at, created_by)
-- I cast espliciti servono: in un `INSERT … SELECT` i letterali della SELECT nascono `text` e non
-- vengono adattati alla colonna come accade in un `INSERT … VALUES`.
SELECT 'e0000000-0000-4000-8000-000000000001'::uuid, 'a0000000-0000-4000-8000-000000000001', app.id,
       'b0000000-0000-4000-8000-000000000002'::uuid, 'admin', 'b0000000-0000-4000-8000-000000000001'::uuid,
       '2024-01-01T00:00:00Z'::timestamptz, '2024-01-01T00:00:00Z'::timestamptz, 'seed'
  FROM platform.app app WHERE app.slug = 'crm'
UNION ALL
SELECT 'e0000000-0000-4000-8000-000000000002'::uuid, 'a0000000-0000-4000-8000-000000000001', app.id,
       'b0000000-0000-4000-8000-000000000003'::uuid, 'editor', 'b0000000-0000-4000-8000-000000000001'::uuid,
       '2024-01-01T00:00:00Z'::timestamptz, '2024-01-01T00:00:00Z'::timestamptz, 'seed'
  FROM platform.app app WHERE app.slug = 'crm'
-- Come per le appartenenze, l'arbitro del conflitto è il vincolo VERO — la terna sulle righe vive —
-- e non la chiave primaria.
ON CONFLICT (tenant_id, app_id, identity_id) WHERE deleted_at IS NULL DO UPDATE SET
  role = EXCLUDED.role, granted_by = EXCLUDED.granted_by, updated_at = EXCLUDED.updated_at;

-- ── invitations (Acme, pending) ──────────────────────────────────────────────
-- token_hash = SHA-256(hex) dei token fissi documentati nel README.
--   invitee-admin@acme.test  → token 'seed-invite-acme-admin'
--   invitee-member@acme.test → token 'seed-invite-acme-member'
-- UC 0098: entrambi gli inviti sono di ruolo `member`, perché il ruolo di piattaforma ha due soli
-- valori e chi entra non porta con sé alcun potere — i poteri si concedono dopo, una applicazione alla
-- volta. Il primo indirizzo conserva il nome storico 'invitee-admin' perché i collaudi lo nominano.
INSERT INTO platform.invitations (id, tenant_id, email, role, token_hash, status, expires_at, invited_by, accepted_user_id, created_at, updated_at, created_by) VALUES
  ('c0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'invitee-admin@acme.test',  'member', '1ddd1a3f17c576bf0a17e22bdd4e136384a7d55d49bbbf53e58c11111b15ffb0', 'pending', '2999-12-31T00:00:00Z', 'b0000000-0000-4000-8000-000000000001', NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
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
