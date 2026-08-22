-- UC 0103 (change 0098) — l'abbonamento dei POSTI: una voce di catalogo che non è una applicazione,
-- un abbonamento con una quantità, e l'invito che porta con sé l'addebito che lo ha autorizzato.
--
-- ── Perché una voce di catalogo e non una struttura dedicata ─────────────────────────────────────
-- L'epica E22.2 ha soppesato le due vie e ha scelto la PRIMA, per una ragione di costo e non di
-- eleganza: appendere i posti a una voce di catalogo permette di riusare INTERI il pagamento, la
-- ricezione degli eventi del fornitore, il ciclo di vita dell'abbonamento, la riconciliazione e la
-- sezione «Billing». La via concettualmente più pulita — una struttura tutta sua per i posti —
-- costringerebbe a riscrivere tutte e cinque quelle cose.
--
-- Il prezzo di questa scelta è dichiarato e va pagato: una voce del catalogo che NON è una
-- applicazione dev'essere esclusa da ogni superficie che elenca applicazioni. Le esclusioni sono
-- cinque, sono elencate nello use case, e ognuna ha il suo collaudo. Se un giorno diventassero più di
-- una manciata, la via della struttura dedicata va rivalutata (punto aperto dell'epica 22).

-- ── `kind`: che tipo di voce di catalogo è questa ────────────────────────────────────────────────
-- UN ATTRIBUTO, NON UN ELENCO DI SLUG. È la differenza fra una regola che il codice può applicare e
-- una regola che invecchia in silenzio: la change 0092 ha rimandato l'esclusione della voce dei posti
-- proprio perché «un elenco di slug da escludere sarebbe una regola destinata a invecchiare». Con la
-- colonna, ogni lettura che elenca applicazioni chiede «kind = application» e la voce nuova di domani
-- non ha bisogno di essere aggiunta da nessuna parte.
--
-- Predefinito `application`: le cinque righe già in tabella sono applicazioni, e la sincronizzazione
-- del listino (pricing-as-code) non nomina questa colonna — quindi le app che nasceranno da lì
-- continueranno a nascere come applicazioni senza toccare il motore di sincronizzazione.
ALTER TABLE platform.app
    ADD COLUMN kind varchar(32) NOT NULL DEFAULT 'application';

ALTER TABLE platform.app
    ADD CONSTRAINT ck_app_kind CHECK (kind IN ('application', 'platform'));

COMMENT ON COLUMN platform.app.kind IS
    'UC 0103 — `application` = applicazione del marketplace, da vendere e da aprire; `platform` = voce '
    'di piattaforma (i posti), che porta un abbonamento ma NON è una applicazione: va esclusa da ogni '
    'superficie che elenca applicazioni (diritti, vetrina, «dove posso entrare», applicazioni per '
    'persona, matrice dei diritti in console).';

-- ── la riga della voce dei posti ─────────────────────────────────────────────────────────────────
-- Identificativo DETERMINISTICO dalla chiave stabile `app:platform-seats`, con lo stesso algoritmo di
-- CatalogIds (UUID versione 3 sul nome): così è lo stesso in ogni ambiente e il codice può risolverla
-- per slug senza doverla cercare per tentativi. Il valore è scritto in chiaro qui perché una
-- migrazione non può chiamare Java, e un collaudo verifica che i due coincidano — se qualcuno cambiasse
-- lo slug, quel collaudo diventerebbe rosso invece di lasciare in tabella una riga orfana.
--
-- `status = active`: la voce è viva, altrimenti l'abbonamento dei posti non concederebbe nulla (la
-- regola unica di accesso richiede l'app attiva). `user_model = multi_user`: i posti riguardano per
-- definizione più persone. Nessuna fascia e nessun prezzo di catalogo: il prezzo dei posti sta nel suo
-- listino versionato (V21), non fra gli `app_price`, perché è a scaglioni e non a fascia unica.
--
-- `paddle_product_id` resta nullo: la sincronizzazione del listino non conosce questa riga (il file
-- `pricing/seats.yaml` sta fuori da `pricing/index.yaml`, per scelta della change 0097). Il prodotto
-- presso il fornitore di pagamento nascerà quando il fornitore vero sarà attivabile (prerequisito #14).
INSERT INTO platform.app
    (id, slug, name, user_model, status, kind, paddle_product_id, created_at, updated_at, created_by, updated_by)
VALUES
    ('22c25c07-0247-3196-8d05-a2d26587295a', 'platform-seats', 'Posti dell''account',
     'multi_user', 'active', 'platform', NULL, now(), now(), 'migration', 'migration');

-- ── `quantity`: quanti posti sono PAGATI ─────────────────────────────────────────────────────────
-- Gli abbonamenti delle applicazioni sono a quantità uno (si compra una fascia, non N copie): per
-- questo la colonna non esisteva. L'abbonamento dei posti è il primo che ne ha bisogno.
--
-- Il valore è il numero di posti A PAGAMENTO già pagati per il periodo in corso — non il numero di
-- posti occupati. Con la franchigia di tre e sei persone in tutto, `quantity` vale 3.
--
-- È un HIGH-WATER MARK del periodo, e sale soltanto (in questa storia). Da questa sola proprietà
-- derivano due casi dello use case senza scrivere una riga di codice per ciascuno: un invito scaduto o
-- revocato libera il posto SENZA rimborso, e un invito nuovo entro lo stesso periodo NON produce un
-- secondo addebito, perché il posto bersaglio risulta già pagato. La discesa — al termine del periodo,
-- dopo una riduzione — è di UC 0104.
--
-- Predefinito 1 per non riscrivere le righe esistenti: un abbonamento di applicazione è, ed è sempre
-- stato, «una volta».
ALTER TABLE platform.subscription
    ADD COLUMN quantity integer NOT NULL DEFAULT 1;

ALTER TABLE platform.subscription
    ADD CONSTRAINT ck_subscription_quantity CHECK (quantity >= 0);

COMMENT ON COLUMN platform.subscription.quantity IS
    'UC 0103 — numero di unità dell''abbonamento. Vale 1 per gli abbonamenti delle applicazioni (si '
    'compra una fascia). Per l''abbonamento dei POSTI è il numero di posti a pagamento già pagati per '
    'il periodo in corso: high-water mark che in UC 0103 sale soltanto, e che scende a fine periodo '
    'con la riduzione di UC 0104.';

-- ── l'addebito che ha autorizzato l'invito ───────────────────────────────────────────────────────
-- Riferimento OPACO alla transazione presso il fornitore di pagamento, non una chiave esterna verso
-- `platform.billing_transaction`: quella riga la scrive il consumatore degli eventi del fornitore,
-- quindi arriva DOPO — a volte secondi dopo. Un vincolo di integrità verso una riga che non è ancora
-- nata renderebbe impossibile creare l'invito nell'istante in cui l'addebito è appena riuscito, che è
-- esattamente l'istante in cui la storia vuole crearlo.
--
-- Nullo quando l'invito è entro la franchigia (nessun addebito, quindi nessun riferimento) oppure
-- quando il posto era già pagato nel periodo. Un valore nullo NON significa «non verificato»:
-- significa «questo invito non ha richiesto denaro».
--
-- Dichiarato come dato personale per PRUDENZA nel manifesto della piattaforma: descrive una transazione
-- dell'account, non la persona, ma sta su una riga il cui soggetto è una persona e la collega a un
-- pagamento. La classificazione conservativa non aggiunge obblighi (la riga di invito è già esportata ed
-- eliminata con l'account) e chiude la classe «campo non dichiarato».
ALTER TABLE platform.invitations
    ADD COLUMN seat_charge_ref varchar(64);

COMMENT ON COLUMN platform.invitations.seat_charge_ref IS
    'UC 0103 — riferimento alla transazione presso il fornitore di pagamento che ha autorizzato il '
    'posto di questo invito; nullo quando il posto era gratuito (franchigia) o già pagato nel periodo '
    'in corso. Riferimento opaco e non chiave esterna: la riga locale della transazione la scrive il '
    'consumatore degli eventi del fornitore, che arriva dopo.';
