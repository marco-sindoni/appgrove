# Change 0097: Listino dei posti a scaglioni progressivi — modello versionato e calcolo

**Branch**: `change/0097-use-case-0102-listino-posti-fasce`
**Aree**: `services/core`
**Data**: 2026-08-22
**Autore**: Platform Engineering (modalità fast, orchestrata da `go-fast`)
**Use case sorgente**: [docs/usecases/22-refactor-membership-model/story/0102-listino-posti-a-fasce.md](../../docs/usecases/22-refactor-membership-model/story/0102-listino-posti-a-fasce.md)
(piano di lavoro: [task/0102](../../docs/usecases/22-refactor-membership-model/task/0102-listino-posti-a-fasce.md))
**Tocca dati personali?**: No — il listino conserva tariffe; il conteggio dei posti produce un **numero**,
non un elenco di persone, e non memorizza nulla di nuovo su nessuno. Il gate privacy di step-03 va comunque
eseguito, perché la change introduce tabelle nuove (segnale che lo scanner rileva per costruzione).

## Problema / Obiettivo

Oggi non esiste alcun modo di rispondere alla domanda «quanto deve questo account per le sue persone»: il
posto a pagamento è la novità dell'epica 22 e la sua unica base è la prosa dello use case. Serve la parte
fredda e verificabile, prima di qualunque interfaccia e di qualunque atto d'acquisto:

1. una **regola di calcolo** del dovuto, esatta al centesimo e provata caso per caso;
2. una **conservazione versionata** del listino, così che fra un anno si possa ancora rispondere a «quanto
   pagava questo cliente in marzo?»;
3. una definizione **unica** di «che cosa occupa un posto», perché due definizioni divergono sempre.

Il risultato osservabile a fine change: la banca dati contiene la prima versione del listino con le sue
cinque fasce, una chiamata autenticata restituisce quel listino, e la funzione di calcolo produce
esattamente i valori della tabella di riferimento dello use case (§4), confini compresi.

## Scope

Tutto dentro `services/core`.

**Il modello di conservazione.** Due tabelle **di piattaforma** (senza discriminatore di account: il
listino è di tutti) nello schema `platform`:

- la **versione** del listino: decorrenza, valuta, nota, più chi l'ha creata e quando. Le versioni sono
  **immutabili**: cambiare una tariffa significa creare una versione nuova, non modificare la vigente
  (l'atto di crearla è di UC 0105);
- le **fasce** di una versione: posto iniziale, posto finale (vuoto per l'ultima), tariffa in centesimi.

Due versioni non possono avere la **stessa decorrenza**: renderebbe ambigua la domanda «quale listino
vigeva quel giorno», che è l'unica ragione per cui il modello è versionato.

**La franchigia non è un caso speciale.** I primi tre posti sono rappresentati come **prima fascia a
tariffa zero da 1 a 3**. Nel codice non esiste alcuna condizione «se i posti sono al massimo tre allora
zero»: il conto la produce da sé.

**La regola di calcolo, a scaglioni progressivi.** Ogni posto paga la tariffa della fascia in cui cade
*quel* posto. Il dovuto è la somma, su tutte le fasce, dei posti che vi cadono per la tariffa della fascia.
Si calcola in **centesimi interi**, mai in virgola mobile, e non si arrotonda per riga. Con zero posti il
dovuto è zero (caso definito anche se irraggiungibile: l'owner c'è sempre).

Oltre al dovuto, la stessa funzione espone il **costo del posto successivo**: è il secondo numero che le
interfacce di UC 0106 e UC 0103 devono mostrare accanto al totale, e appartiene al calcolo, non alla
presentazione.

**Il listino vigente si sceglie per data.** L'unica lettura ammessa è «la versione vigente a questo
istante»: quella con la decorrenza più recente fra quelle già decorse. Non esiste una lettura «prendi
l'ultima creata»: con una versione futura già inserita darebbe la risposta sbagliata. Se **nessuna**
versione è vigente alla data richiesta, il calcolo si **nega** con un errore esplicito: un dovuto
calcolato su un listino inesistente è peggio di un errore.

**Il valore iniziale è codice.** Un file di risorse del servizio dichiara la prima versione (valuta,
decorrenza, fasce). All'avvio, se la tabella delle versioni è vuota, la prima versione nasce da quel file;
ai riavvii successivi non nasce nulla. Il file sta **fuori** dal registro dei listini delle applicazioni:
i posti non sono un'applicazione, e mescolarli confonderebbe la sincronizzazione col fornitore di
pagamento. Da lì in poi la verità è la banca dati.

Un file **incoerente** (fasce non contigue, prima fascia che non parte dal posto 1, ultima fascia chiusa,
tariffa negativa) fa **fallire l'avvio** con un messaggio che dice qual è il difetto: un listino a metà è
peggio di un servizio che non parte.

**Che cosa occupa un posto — una definizione sola.** Occupano un posto: l'owner, ogni persona attiva, ogni
persona sospesa, ogni persona indicata per la cessazione finché il periodo non scade, ogni invito in
attesa non scaduto. Non occupano posto: le persone rimosse, gli inviti scaduti, revocati, rifiutati o già
accettati. Il conteggio è **una** funzione, usata da chiunque, filtrata sull'account del token verificato.

**La lettura di rete.** Una chiamata autenticata restituisce il listino vigente (valuta, decorrenza,
fasce con le tariffe), aperta a **qualunque** persona autenticata: mostrare i prezzi non richiede alcun
diritto. La scrittura non esiste in questa change. Il calcolo del **proprio** dovuto non è qui: sta in
UC 0103, con il riquadro dei posti.

## Fuori scope

- **L'atto di acquistare un posto** e l'ordine verifica → addebito → invito → email → UC 0103.
- **La riduzione dei posti** e lo stato «indicata per la cessazione», che oggi il modello non ha ancora
  → UC 0104. La regola di conteggio è scritta in modo da comprenderlo **da sé** quando arriverà
  (occupa posto chi ha un'appartenenza viva, qualunque ne sia lo stato).
- **Il cambio delle tariffe dalla console di piattaforma** e la creazione di versioni nuove → UC 0105.
- **La presentazione al cliente** (righe della fattura, storico, prossimo rinnovo) → UC 0106.
- **Il ciclo annuale** dei posti: il listino nasce mensile. Se servirà, sarà una versione nuova con
  tariffe proprie, non un moltiplicatore applicato a queste (punto aperto, proprietario UC 0102).
- **Valute oltre l'euro**: rimandate, da allineare a come le applicazioni trattano la valuta
  (proprietario UC 0106).
- **Sconti o tariffe negoziate per singolo account**: non previsti; se serviranno saranno una deroga per
  account *sopra* il listino, non una modifica del listino (proprietario: epica 22).
- **Nessuna superficie frontend**: la storia non ne ha. Nel registro di copertura end-to-end lo use case
  0102 passa da esente `non-implementato` a esente `senza-superficie`.

## Criteri di accettazione

- [ ] La banca dati conserva versioni **immutabili** del listino con la loro decorrenza, e non ammette due
      versioni con la stessa decorrenza.
- [ ] La funzione di calcolo restituisce esattamente i valori della tabella dello use case §4 per
      1, 2, 3, 4, 5, 8, 10, 11, 12, 50, 51, 52, 55, 100, 101, 120 posti, e zero per zero posti.
- [ ] Il **costo del posto successivo** è esposto e vale 2,99 € fino a 10 posti, 1,99 € da 10 a 50,
      0,99 € da 50 a 100, 0,49 € da 100 in poi — cioè scende ai tre confini di fascia.
- [ ] Il dovuto è **monotono crescente** al crescere dei posti: un collaudo lo pretende su tutto
      l'intervallo 0…150, perché è la proprietà che rende il modello spiegabile a un cliente.
- [ ] La franchigia non compare come condizione nel codice del calcolo: è la prima fascia a tariffa zero.
- [ ] La selezione per data restituisce la versione vigente e **ignora** una versione con decorrenza
      futura; se nessuna versione è vigente, il calcolo fallisce con un errore esplicito e riconoscibile.
- [ ] La prima versione nasce dal file di risorse al primo avvio e **non si duplica** al secondo.
- [ ] Il conteggio dei posti comprende owner, persone attive, persone sospese e inviti in attesa non
      scaduti, ed esclude persone rimosse e inviti scaduti, revocati, rifiutati o accettati.
- [ ] La chiamata di lettura del listino risponde a qualunque autenticato e rifiuta chi non lo è.
- [ ] `./run-tests.sh` (suite completa) verde.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT verificato** — il conteggio dei posti è l'unica lettura di questa change
  legata a un account e prende il perimetro dal discriminatore automatico, alimentato dal claim
  `tenant_id`. Nessun identificativo di account arriva da parametro o da corpo della richiesta.
- **Filtro row-level `WHERE tenant_id`** — il conteggio passa dalle entità già tenant-scoped
  (appartenenze e inviti), quindi il filtro è aggiunto da Hibernate e non a mano. Le tabelle del listino
  sono invece **di piattaforma** e non portano `tenant_id`: è deliberato e va dichiarato nella migrazione,
  come per il catalogo delle applicazioni.
- **Modulo Terraform `microsaas_app`** — non toccato: nessuna applicazione nuova.
- **Logging strutturato** — la lettura di rete e il caricamento iniziale registrano una riga con il
  contesto già propagato (account, utente, applicazione) dai filtri esistenti.

## Requisiti di test

- **Unità, tabellare**: tutti i casi del §4 dello use case, con i confini, scritti **come tabella di
  casi** e non come una dozzina di metodi — si deve leggere come una specifica. È il collaudo più
  importante della sotto-epica.
- **Unità**: monotonia del dovuto su un intervallo ampio; è il collaudo che il modello scartato non
  avrebbe superato.
- **Unità**: costo del posto successivo, con i tre confini in cui scende.
- **Unità**: coerenza del listino — fasce non contigue, prima fascia che non parte da 1, ultima fascia
  chiusa e tariffa negativa vengono rifiutate con un errore parlante.
- **Unità/integrazione**: selezione della versione per data, con una versione futura presente; errore
  esplicito se nessuna versione è vigente.
- **Integrazione**: creazione della prima versione dal file al primo avvio e nessuna duplicazione al
  secondo; le fasce create combaciano col file.
- **Integrazione**: conteggio dei posti stato per stato — persona attiva, sospesa, rimossa; invito in
  attesa, scaduto, revocato, accettato — e separazione fra account (i posti di un account non contano
  nell'altro).
- **Integrazione**: la lettura di rete risponde a un autenticato qualunque e rifiuta l'anonimo.
- **Percorsi end-to-end**: nessuno. Storia senza superficie: il registro di copertura la riclassifica da
  `non-implementato` a `senza-superficie`.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — solo tabelle e operazioni nuove |
| Contratto cross-area | No (nessun consumatore frontend in questa change; l'operazione di lettura è nuova e additiva) |
| Version bump | minor |
