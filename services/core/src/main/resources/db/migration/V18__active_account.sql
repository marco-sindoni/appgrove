-- UC 0117 (change 0089) — account attivo nella sessione.
--
-- Dopo UC 0116 una persona può appartenere a più account, e il token non può più dedurre l'account
-- da lei: deve portare l'account ATTIVO in quella sessione. Questa migrazione crea il posto in cui
-- quell'informazione vive, e il registro dei suoi cambi.
--
-- PERCHÉ IN BANCA DATI E NON PRESSO IL FORNITORE DI IDENTITÀ — la via «più elegante» sarebbe un
-- attributo personalizzato sull'utente Cognito. Non si fa, e la ragione va letta prima di riprovarci:
-- il gruppo di utenti (infra/modules/platform_shared/auth.tf) NON dichiara attributi personalizzati, e
-- aggiungerne uno per via dichiarativa può forzare la RICREAZIONE del gruppo — cioè la perdita di
-- tutti gli utenti. Rischio inaccettabile per una comodità. La funzione che compone il token
-- interroga già questa banca dati: una colonna in più non costa nulla e non tocca l'infrastruttura.

-- ── il riferimento all'appartenenza attiva ───────────────────────────────────
-- Annullabile: chi ha una sola appartenenza non ha bisogno di sceglierla, e per lui il valore resta
-- vuoto senza che cambi nulla (è il caso di tutti gli utenti di oggi). La chiave esterna garantisce
-- che il valore punti a un'appartenenza esistente, MA non che sia ancora valida: la validità si
-- riverifica a ogni creazione di token e il valore conservato NON è creduto. È quella riverifica —
-- non questo vincolo — che impedisce a una manomissione di questa colonna di diventare un varco fra
-- due aziende.
--
-- ON DELETE SET NULL, e non è un dettaglio: l'uscita da un account è un soft-delete, ma la
-- cancellazione di un account (UC 0033) elimina FISICAMENTE le sue appartenenze. Senza questa regola
-- la purga si romperebbe contro il vincolo ogni volta che qualcuno stava lavorando in quell'account —
-- e la persona rimasta viva altrove tornerebbe semplicemente senza scelta, che è l'esito giusto:
-- l'appartenenza non c'è più, il riferimento non deve sopravviverle.
ALTER TABLE platform.identity
    ADD COLUMN active_membership_id uuid REFERENCES platform.membership (id) ON DELETE SET NULL;

COMMENT ON COLUMN platform.identity.active_membership_id IS
    'UC 0117 — appartenenza attiva nella sessione: SUGGERIMENTO, non fonte di verità. La verità è '
    'l''appartenenza riverificata al momento della creazione del token (una sola appartenenza attiva '
    'vince su questo valore; un valore che non corrisponde a un''appartenenza attiva viene ignorato). '
    'Sta qui e non come attributo del gruppo di utenti Cognito perché quel gruppo non dichiara '
    'attributi personalizzati e aggiungerne uno per via dichiarativa rischia di ricrearlo, cioè di '
    'perdere gli utenti.';

-- ── traccia di controllo del cambio di account attivo ────────────────────────
-- Una riga per ogni cambio EFFETTIVO (scegliere l'account su cui si è già non produce nulla). Soli
-- identificativi opachi: nessun indirizzo, nessun nome. Serve alla domanda «chi ha fatto cosa e per
-- conto di chi», che senza questo registro non avrebbe risposta.
--
-- NON è tenant-scoped: l'atto attraversa gli account per costruzione — è il passaggio da uno all'altro
-- — e attribuirlo a uno dei due sarebbe arbitrario. Il soggetto è la PERSONA (l'identità).
--
-- ON DELETE CASCADE sul riferimento alla persona: questa non è una prova di adempimento come
-- `gdpr_purge_audit` (che deve sopravvivere alla cancellazione, perché prova che è avvenuta) ma una
-- traccia operativa il cui unico soggetto è la persona. Quando la persona viene cancellata (UC 0033)
-- la traccia se ne va con lei — sia perché contiene un suo identificativo, sia perché senza vincolo
-- la purga si romperebbe contro la chiave esterna.
CREATE TABLE platform.active_account_audit (
    id             uuid        PRIMARY KEY,
    identity_id    uuid        NOT NULL REFERENCES platform.identity (id) ON DELETE CASCADE,
    from_tenant_id varchar(64),            -- account attivo prima del cambio (null: nessuno)
    to_tenant_id   varchar(64) NOT NULL,   -- account attivo dopo il cambio
    executed_at    timestamptz NOT NULL
);
CREATE INDEX ix_active_account_audit_identity
    ON platform.active_account_audit (identity_id, executed_at DESC);

COMMENT ON TABLE platform.active_account_audit IS
    'UC 0117 — prova del cambio di account attivo di una persona. Soli identificativi opachi. Non '
    'tenant-scoped: l''atto attraversa gli account per costruzione. Conservazione 12 mesi come le '
    'altre prove di audit nella banca dati (AuditRetentionSweeper).';
