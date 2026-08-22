-- UC 0104 (change 0099) — la RIDUZIONE DEI POSTI IN ATTESA: ridurre non è immediato.
--
-- ── Perché due tabelle e non un contrassegno sulla persona ───────────────────────────────────────
-- La via più breve sarebbe una colonna `ending_at` sulla riga dell'appartenenza. Lo use case §7 l'ha
-- soppesata e scartata, per due ragioni che non sono di eleganza:
--   1. la riduzione è UN ATTO SU PIÙ PERSONE, con una data di esecuzione COMUNE. Con il contrassegno la
--      data starebbe scritta N volte e potrebbe divergere: tre persone «in cessazione» con tre date
--      diverse sarebbero tre riduzioni, e l'account non ne ha chiesta nessuna delle tre;
--   2. l'annullamento è un atto unico. Con il contrassegno «annulla la riduzione» diventerebbe «azzera
--      N colonne», cioè un'operazione che può riuscire a metà.
-- Con l'atto in tabella, la data è una, l'annullamento è una riga che cambia stato, e il vincolo di
-- unicità qui sotto rende impossibile lo stato che nessuna schermata saprebbe mostrare.

-- ── L'atto ───────────────────────────────────────────────────────────────────────────────────────
CREATE TABLE platform.seat_downgrade (
    id           uuid        PRIMARY KEY,
    tenant_id    varchar(64) NOT NULL,          -- discriminatore di account (invariante #2)
    execute_at   timestamptz NOT NULL,          -- fine del periodo GIÀ PAGATO: prima di allora non si esegue
    status       varchar(16) NOT NULL,          -- pending | executed | cancelled
    requested_by uuid REFERENCES platform.identity (id),  -- identità di chi l'ha chiesta (traccia di controllo)
    executed_at  timestamptz,                   -- quando l'esecuzione è avvenuta davvero
    created_at   timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL,
    created_by   varchar(64),
    updated_by   varchar(64),
    deleted_at   timestamptz,
    CONSTRAINT ck_seat_downgrade_status CHECK (status IN ('pending', 'executed', 'cancelled'))
);

-- UNA SOLA riduzione in attesa per account, e il vincolo sta QUI.
--
-- Il piano di lavoro lo dice senza mezzi termini: «il vincolo in banca dati vale più di qualunque
-- controllo applicativo». Il controllo applicativo esiste comunque — serve a restituire un rifiuto
-- comprensibile invece di una violazione di indice — ma è il secondo presidio, non il primo: due
-- richieste simultanee dello stesso owner passerebbero entrambe il controllo e solo una passa da qui.
--
-- Parziale su `pending` e sulle righe vive: una riduzione eseguita o annullata non deve impedire la
-- successiva, altrimenti il primo ripensamento chiuderebbe la funzione per sempre.
CREATE UNIQUE INDEX ux_seat_downgrade_pending
    ON platform.seat_downgrade (tenant_id)
    WHERE status = 'pending' AND deleted_at IS NULL;

-- «Quali riduzioni sono da eseguire adesso?» — la domanda dello spazzino, fatta ogni ora su tutta la
-- piattaforma: è l'unica lettura di questa tabella che NON è filtrata per account, quindi è l'unica che
-- ha bisogno di un indice suo.
CREATE INDEX ix_seat_downgrade_due
    ON platform.seat_downgrade (execute_at)
    WHERE status = 'pending' AND deleted_at IS NULL;

COMMENT ON TABLE platform.seat_downgrade IS
    'UC 0104 — riduzione dei posti PROGRAMMATA: l''owner indica le persone da cessare e l''account entra '
    'in «riduzione in attesa» fino alla fine del periodo già pagato. Entità di account (porta tenant_id). '
    'La quantità dell''abbonamento dei posti NON cambia alla richiesta: scende solo all''esecuzione.';

COMMENT ON COLUMN platform.seat_downgrade.execute_at IS
    'Fine del periodo già pagato (platform.subscription.current_period_end dell''abbonamento dei posti): '
    'la riduzione non si esegue prima, perché il posto è stato pagato per tutto il mese (permanenza '
    'minima mensile, epica E22.2).';

COMMENT ON COLUMN platform.seat_downgrade.requested_by IS
    'Identità della persona che ha chiesto la riduzione. Dato personale (identificativo online): la '
    'traccia di chi ha deciso la cessazione di un collega. Titolare: l''account.';

-- ── Le persone indicate ──────────────────────────────────────────────────────────────────────────
CREATE TABLE platform.seat_downgrade_item (
    id           uuid        PRIMARY KEY,
    tenant_id    varchar(64) NOT NULL,          -- discriminatore di account (invariante #2)
    downgrade_id uuid        NOT NULL REFERENCES platform.seat_downgrade (id),
    identity_id  uuid        NOT NULL REFERENCES platform.identity (id),
    created_at   timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL,
    created_by   varchar(64),
    updated_by   varchar(64),
    deleted_at   timestamptz
);

-- Una persona compare UNA volta per riduzione, sulle sole righe vive: l'owner può togliere una persona
-- dall'elenco e poi reindicarla, e con un vincolo incondizionato il primo ripensamento la escluderebbe
-- per sempre da quella riduzione. È la stessa forma di ux_app_access_tenant_app_identity (V20).
CREATE UNIQUE INDEX ux_seat_downgrade_item_person
    ON platform.seat_downgrade_item (downgrade_id, identity_id)
    WHERE deleted_at IS NULL;

-- «Chi è indicato in questa riduzione?» — la domanda del riquadro di avviso e dell'esecuzione.
CREATE INDEX ix_seat_downgrade_item_downgrade
    ON platform.seat_downgrade_item (downgrade_id);

-- «Questa persona è in cessazione?» — la domanda dell'elenco unico delle persone, riga per riga.
CREATE INDEX ix_seat_downgrade_item_tenant_identity
    ON platform.seat_downgrade_item (tenant_id, identity_id);

COMMENT ON TABLE platform.seat_downgrade_item IS
    'UC 0104 — le persone indicate per la cessazione in una riduzione. Restano ATTIVE, con lo stesso '
    'accesso e gli stessi ruoli, fino alla data di esecuzione: la riga dice «questa persona uscirà», non '
    '«questa persona è uscita».';

COMMENT ON COLUMN platform.seat_downgrade_item.identity_id IS
    'Identità della persona indicata (platform.identity), non la sua appartenenza: la stessa scelta di '
    'app_access.identity_id (V20). Dato personale (identificativo online). Titolare: l''account.';
