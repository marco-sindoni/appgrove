-- UC 0033 — eliminazione account self-service con grace 14 giorni (#13 E25).
-- La richiesta di cancellazione disattiva subito l'account (status = 'pending_deletion') e
-- persiste l'istante della richiesta: allo scadere della grace il job schedulato invoca
-- l'orchestrazione di offboarding (UC 0032). Nessun dato personale nuovo: è uno stato tecnico.
ALTER TABLE platform.accounts ADD COLUMN deletion_requested_at timestamptz;
