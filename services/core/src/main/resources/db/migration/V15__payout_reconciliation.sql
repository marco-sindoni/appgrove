-- UC 0071 (change 0083) — riconciliazione fra ricavo lordo e denaro davvero accreditato.
--
-- Il fornitore di pagamento è venditore ufficiale verso il cliente: incassa lui, trattiene le proprie
-- commissioni (una percentuale più una quota fissa per transazione) e ci accredita il NETTO, con accrediti
-- periodici che non coincidono con le singole vendite. Finora la piattaforma registrava solo il lordo: la
-- differenza — che non è una percentuale fissa, perché la quota fissa pesa moltissimo sui piccoli importi —
-- era invisibile.

-- 1) Commissioni e netto sulla riga di transazione già esistente: sono ATTRIBUTI della transazione, non
--    un'entità a sé. `fee_source` dice da dove viene il numero, perché una stima non deve mai poter essere
--    scambiata per un dato dichiarato dal fornitore (decisione 3, change 0083).
ALTER TABLE platform.billing_transaction
    ADD COLUMN fee_amount integer,      -- unità minori trattenute dal fornitore; NULL = non applicabile
    ADD COLUMN net_amount integer,      -- unità minori che restano a noi; 0 su storni e tentativi falliti
    ADD COLUMN fee_source varchar(16);  -- provider | estimated | NULL (non applicabile)

COMMENT ON COLUMN platform.billing_transaction.fee_amount IS
    'Commissione trattenuta dal fornitore, in unità minori (UC 0071).';
COMMENT ON COLUMN platform.billing_transaction.net_amount IS
    'Netto residuo dopo la commissione, in unità minori; 0 per storni e tentativi falliti (UC 0071).';
COMMENT ON COLUMN platform.billing_transaction.fee_source IS
    'provider = dichiarata dal fornitore; estimated = stimata dalla formula del listino (UC 0071).';

-- Le righe già registrate restano senza commissione: il dato non esisteva quando sono state scritte e
-- inventarlo a posteriori scriverebbe un numero falso in una vista che serve proprio a dire la verità sui
-- soldi. Restano visibili nel lordo e fuori dal netto, ed è il comportamento corretto.

-- 2) Gli accrediti. Dati economici DELLA PIATTAFORMA: non hanno un tenant, perché un accredito raccoglie
--    transazioni di conti diversi. Nessuna colonna `tenant_id`, nessuna entità tenant-scoped.
CREATE TABLE platform.payout (
    id                     uuid        PRIMARY KEY,
    paddle_payout_id       varchar(64) NOT NULL,   -- riferimento dell'accredito presso il fornitore
    amount                 integer     NOT NULL,   -- importo accreditato, unità minori (può essere negativo)
    currency               varchar(3)  NOT NULL,
    paid_at                timestamptz NOT NULL,   -- data dell'accredito sul conto
    last_event_occurred_at timestamptz,            -- guardia out-of-order, come su subscription
    created_at             timestamptz NOT NULL,
    updated_at             timestamptz NOT NULL,
    created_by             varchar(64),
    updated_by             varchar(64),
    deleted_at             timestamptz
);

-- Idempotenza: lo stesso accredito si registra una volta sola, anche se più eventi lo riguardano.
CREATE UNIQUE INDEX ux_payout_paddle ON platform.payout (paddle_payout_id);
CREATE INDEX ix_payout_paid_at ON platform.payout (paid_at DESC);

-- 3) Il dettaglio dell'accredito: una riga per transazione accreditata, con il netto accreditato ALLORA.
--    Non si ricalcola dalla transazione: è ciò che rende la quadratura stabile nel tempo. Un rimborso o una
--    contestazione successivi non devono far apparire sbagliato un accredito che allora era corretto — il
--    denaro restituito ricompare come riga NEGATIVA in un accredito successivo, come lo comunica il
--    fornitore (decisione 5, change 0083).
CREATE TABLE platform.payout_line (
    payout_id             uuid        NOT NULL REFERENCES platform.payout (id) ON DELETE CASCADE,
    paddle_transaction_id varchar(64) NOT NULL,
    net_amount            integer     NOT NULL,   -- con segno: negativo per gli storni
    currency              varchar(3)  NOT NULL,   -- autoconsistente: l'accredito può citare transazioni
                                                  -- che non abbiamo mai ricevuto localmente
    PRIMARY KEY (payout_id, paddle_transaction_id)
);

CREATE INDEX ix_payout_line_transaction ON platform.payout_line (paddle_transaction_id);
