-- ─────────────────────────────────────────────────────────────────────────────
-- appgrove — SEED billing/subscription (UC 0011 + UC 0022). SEPARATO da seed.sql perché dipende dal
-- CATALOGO (FK su platform.app / platform.app_tier), prodotto dal loader pricing-as-code (non dal seed).
--
-- Applicarlo SOLO dove il catalogo esiste:
--   • core (@QuarkusTest): catalogo dal loader allo startup → SeedDataTest/AdminApiTest applicano seed.sql + questo;
--   • dev/E2E: `dev seed` esegue `sync-pricing` (loader) e poi applica seed.sql + questo.
-- I servizi di sola identità (es. auth-local) applicano SOLO seed.sql (accounts/users/invitations) e NON questo:
-- non hanno il loader e non usano le subscription.
--
-- IDEMPOTENTE (ON CONFLICT) · DETERMINISTICO (UUID/timestamp fissi) · gli UUID di app/tier sono quelli
-- prodotti da CatalogIds (vedi dev/seed/README.md).
-- ─────────────────────────────────────────────────────────────────────────────

-- ── subscription (tenant↔app, stati di lifecycle vari → entitlement derivato) ─
--  Acme→teams  active     · Acme→notes  past_due
--  Bob →notes  trialing   · Acme→legacy active (ma app 'legacy' inactive → gate "app abilitata")
--  Bob →teams  canceled
INSERT INTO platform.subscription (id, tenant_id, app_id, app_tier_id, status, current_period_start, current_period_end, cancel_at, trial_end, paddle_subscription_id, created_at, updated_at, created_by) VALUES
  ('10000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', '1c4ea96d-bc57-3109-9c83-0933a3553779', 'e075f588-c33b-35c5-af41-285c1d006f8e', 'active',   '2024-06-01T00:00:00Z', '2024-07-01T00:00:00Z', NULL,                   NULL,                   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('10000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'e8b95b18-4b67-3943-aa28-5544c737f9eb', '491687be-df2b-344c-b99d-8c3a601fa7c5', 'past_due', '2024-06-01T00:00:00Z', '2024-07-01T00:00:00Z', NULL,                   NULL,                   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('10000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002', 'e8b95b18-4b67-3943-aa28-5544c737f9eb', '6f7a0317-17b2-3bbd-b2b1-6644d9a9186e', 'trialing', '2024-06-01T00:00:00Z', '2024-07-01T00:00:00Z', NULL,                   '2999-12-31T00:00:00Z', NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('10000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001', '52fbfc15-5970-3d3c-9d61-5ab3ac37b232', 'a70ee7e4-d0ae-37e6-aa7d-2f2380833d5f', 'active',   '2024-06-01T00:00:00Z', '2024-07-01T00:00:00Z', NULL,                   NULL,                   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed'),
  ('10000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000002', '1c4ea96d-bc57-3109-9c83-0933a3553779', 'e075f588-c33b-35c5-af41-285c1d006f8e', 'canceled', '2024-06-01T00:00:00Z', '2024-07-01T00:00:00Z', '2024-06-15T00:00:00Z', NULL,                   NULL, '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z', 'seed')
ON CONFLICT (id) DO UPDATE SET
  tenant_id = EXCLUDED.tenant_id, app_id = EXCLUDED.app_id, app_tier_id = EXCLUDED.app_tier_id,
  status = EXCLUDED.status, current_period_start = EXCLUDED.current_period_start,
  current_period_end = EXCLUDED.current_period_end, cancel_at = EXCLUDED.cancel_at,
  trial_end = EXCLUDED.trial_end, paddle_subscription_id = EXCLUDED.paddle_subscription_id,
  updated_at = EXCLUDED.updated_at;
