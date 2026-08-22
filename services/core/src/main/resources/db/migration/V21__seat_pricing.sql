-- UC 0102 (change 0097) — il listino dei posti, versionato e a scaglioni progressivi.
--
-- È il luogo in cui vive la frase «quanto costa la quarta persona di questo account». Fino a qui il posto
-- a pagamento non esisteva da nessuna parte se non nella prosa dell'epica 22.
--
-- Due tabelle di PIATTAFORMA, senza `tenant_id`: il listino è di tutti, come `platform.app`. Non è una
-- dimenticanza dell'invariante di separazione fra account — è il suo contrario, dichiarato: un listino per
-- account sarebbe una tariffa negoziata, che l'epica 22 esclude esplicitamente (se un giorno servisse, sarà
-- una deroga per account SOPRA il listino, non una modifica del listino).
--
-- Perché VERSIONATO e non «una tabella di fasce che si aggiorna»: l'unica ragione per cui questo modello
-- esiste è poter rispondere fra un anno a «quanto pagava questo cliente in marzo?». Con le fasce mutabili
-- quella domanda non ha risposta, e la risposta sbagliata a una domanda sul denaro è un danno, non un
-- fastidio. Le versioni quindi NON si modificano: cambiare una tariffa significa creare una versione nuova
-- con la sua decorrenza (UC 0105).
--
-- La FRANCHIGIA di tre posti non è un caso speciale: è la prima fascia, da 1 a 3, a tariffa zero. Il codice
-- del calcolo non ha alcun ramo «se i posti sono al massimo tre»; il conto lo produce da sé, e il giorno in
-- cui la franchigia cambia cambia una riga di listino invece di una riga di codice.

-- ── la versione del listino ──────────────────────────────────────────────────
CREATE TABLE platform.seat_pricing_version (
    id             uuid        PRIMARY KEY,
    effective_from timestamptz NOT NULL,             -- decorrenza: da questo istante la versione è vigente
    currency       varchar(3)  NOT NULL,             -- oggi sempre EUR; le altre valute seguono le applicazioni
    note           varchar(500),                     -- perché questa versione esiste, per chi la legge fra un anno
    created_at     timestamptz NOT NULL,             -- quando è stata creata (≠ da quando vige)
    updated_at     timestamptz NOT NULL,
    created_by     varchar(64),                      -- chi l'ha creata (identità dal token, o 'seat-pricing-loader')
    updated_by     varchar(64),
    deleted_at     timestamptz,
    CONSTRAINT ck_seat_pricing_version_currency CHECK (char_length(currency) = 3)
);

-- «Quale listino vigeva a questa data?» è l'UNICA lettura ammessa del listino, e questo è il suo indice.
CREATE INDEX ix_seat_pricing_version_effective_from
    ON platform.seat_pricing_version (effective_from);

-- Due versioni con la STESSA decorrenza renderebbero ambigua proprio la domanda per cui il modello è
-- versionato: quale delle due vigeva quel giorno? Parziale (righe vive) come ogni altra unicità del
-- monorepo. Regge anche la corsa fra due istanze che si avviano insieme: la seconda inserzione viola
-- l'indice invece di creare un listino doppio.
CREATE UNIQUE INDEX ux_seat_pricing_version_effective_from
    ON platform.seat_pricing_version (effective_from) WHERE deleted_at IS NULL;

COMMENT ON TABLE platform.seat_pricing_version IS
    'UC 0102 — una versione del listino dei posti: decorrenza, valuta, nota. Entità di piattaforma '
    '(nessun tenant_id: il listino è di tutti). IMMUTABILE: una tariffa nuova è una versione nuova, '
    'mai la mutazione di una esistente (UC 0105).';

COMMENT ON COLUMN platform.seat_pricing_version.effective_from IS
    'Da quando la versione è vigente. La versione vigente a una data è quella con la decorrenza più '
    'recente fra quelle già decorse: una versione con decorrenza futura esiste ma non si applica.';

-- ── le fasce di una versione ─────────────────────────────────────────────────
CREATE TABLE platform.seat_pricing_band (
    id               uuid        PRIMARY KEY,
    version_id       uuid        NOT NULL REFERENCES platform.seat_pricing_version (id),
    from_seat        integer     NOT NULL,           -- primo posto della fascia (la prima fascia parte da 1)
    to_seat          integer,                        -- ultimo posto; NULL = fascia aperta, l'ultima
    unit_price_cents integer     NOT NULL,           -- tariffa mensile del singolo posto, in centesimi interi
    created_at       timestamptz NOT NULL,
    updated_at       timestamptz NOT NULL,
    created_by       varchar(64),
    updated_by       varchar(64),
    deleted_at       timestamptz,
    CONSTRAINT ck_seat_pricing_band_from  CHECK (from_seat >= 1),
    CONSTRAINT ck_seat_pricing_band_to    CHECK (to_seat IS NULL OR to_seat >= from_seat),
    CONSTRAINT ck_seat_pricing_band_price CHECK (unit_price_cents >= 0)
);

-- Le fasce di una versione si leggono sempre tutte insieme, ordinate dal primo posto.
CREATE INDEX ix_seat_pricing_band_version ON platform.seat_pricing_band (version_id);

-- Due fasce che iniziano allo stesso posto sono un listino indecidibile. La contiguità piena (nessun
-- buco, ultima fascia aperta) NON è esprimibile come vincolo di riga: la controlla il codice che carica e
-- che calcola, e la prova un collaudo — un vincolo che copre metà della regola dà una falsa sicurezza,
-- quindi qui si dichiara quale metà copre.
CREATE UNIQUE INDEX ux_seat_pricing_band_version_from
    ON platform.seat_pricing_band (version_id, from_seat) WHERE deleted_at IS NULL;

COMMENT ON TABLE platform.seat_pricing_band IS
    'UC 0102 — le fasce di una versione del listino: posto iniziale, posto finale (NULL per l''ultima), '
    'tariffa in centesimi. La franchigia è la prima fascia (1–3) a tariffa 0: così la regola di calcolo '
    'non ha casi speciali cablati nel codice.';

COMMENT ON COLUMN platform.seat_pricing_band.to_seat IS
    'Ultimo posto della fascia; NULL solo per l''ultima fascia, che è aperta verso l''alto. Un listino '
    'la cui ultima fascia è chiusa non saprebbe che prezzo dare al posto successivo: è rifiutato.';

COMMENT ON COLUMN platform.seat_pricing_band.unit_price_cents IS
    'Tariffa mensile del SINGOLO posto che cade in questa fascia, in centesimi interi (il denaro non si '
    'calcola in virgola mobile). Il calcolo è a scaglioni progressivi: ogni posto paga la tariffa della '
    'fascia in cui cade quel posto, non la tariffa dell''ultima fascia raggiunta.';
