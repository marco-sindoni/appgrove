-- UC 0028 (change 0024) — portale cliente self-service: persistenza del cambio tier SCHEDULATO.
--
-- A livello di accesso un downgrade programmato "attivo dal giorno X verso il tier Y" è identico ad
-- ACTIVE (si resta sul tier corrente fino a fine periodo), quindi la sola derivazione lifecycle (UC 0026)
-- non basta a mostrarlo. Qui si persiste il cambio programmato così che il read-model self-service
-- (GET /me/subscriptions) possa esporlo. Popolato dal consumer webhook su subscription.updated; azzerato
-- da un cambio immediato (upgrade) o da un resume. Punto aperto di cui UC 0028 è owner (vedi 0028 §Punti aperti).
ALTER TABLE platform.subscription
    ADD COLUMN scheduled_tier_id   uuid REFERENCES platform.app_tier (id),
    ADD COLUMN scheduled_change_at timestamptz;
