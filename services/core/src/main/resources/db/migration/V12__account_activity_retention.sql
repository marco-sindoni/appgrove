-- UC 0035 — substrato per l'auto-cancellazione degli account inattivi (24 mesi, #13 E26) e per la
-- retention as-code. Colonne TECNICHE di stato su platform.accounts (nessun dato personale, come
-- status / deletion_requested_at):
--   • last_active_at       = ultima attività autenticata dell'account (timbrata, con throttle, dal
--                            filtro di attività); è il segnale su cui si misura l'inattività;
--   • inactivity_warned_at = istante dell'avviso di inattività inviato (NULL = nessun avviso pendente).
-- last_active_at è NOT NULL DEFAULT now(): gli account esistenti — e ogni nuova riga — partono con un
-- orologio di inattività fresco dal rilascio, così il primo sweep dopo il deploy non avvisa/cancella
-- in massa account storici mai timbrati.

ALTER TABLE platform.accounts
    ADD COLUMN last_active_at       timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN inactivity_warned_at timestamptz;

-- Indice per lo sweeper di inattività: i candidati si filtrano per last_active_at fra gli account vivi.
CREATE INDEX ix_accounts_last_active ON platform.accounts (last_active_at) WHERE deleted_at IS NULL;
