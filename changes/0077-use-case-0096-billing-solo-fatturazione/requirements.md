# Change 0077: Billing solo-fatturazione (abbonamenti + storico pagamenti/ricevute)

**Branch**: `change/0077-use-case-0096-billing-solo-fatturazione`
**Aree**: `services/core`, `frontend` (backoffice + catalogo traduzioni), `tools/platform-e2e`, documentazione
**Data**: 2026-08-01
**Autore**: Platform Engineering (modalità **fast**, autopilot senza gate di workflow)
**Use case sorgente**: [docs/usecases/21-catalogo-app-backoffice/0096-billing-solo-fatturazione.md](../../docs/usecases/21-catalogo-app-backoffice/0096-billing-solo-fatturazione.md)
**Tocca dati personali?**: **Sì, in senso lato** — nasce una tabella di transazioni di fatturazione riferite
all'account (importo, valuta, esito, riferimento del fornitore). Non è una categoria nuova di dati personali e
non introduce identificatori diretti: si applica comunque il gate privacy/RoPA di step-03 (scanner + verifica
del manifesto), e la nuova tabella entra nell'esportazione e nella cancellazione dei dati dell'account.

## Problema / Obiettivo

La pagina **Billing** del backoffice fa oggi due mestieri diversi. Si intitola "Get an app" ed è per metà una
vetrina d'acquisto costruita sui moduli impacchettati nel frontend — cosa che la pagina **App catalog**
(UC 0095, appena consegnata) fa ora sul catalogo vero — e per metà il posto dove si gestiscono gli
abbonamenti. Il risultato è che chi cerca "dove si compra" e chi cerca "quanto ho pagato" finiscono nello
stesso posto e nessuno dei due trova quello che cerca: **lo storico dei pagamenti e le ricevute oggi non
esistono affatto** nel prodotto. L'unica via alle ricevute è il portale del fornitore, dietro un pulsante che
compare solo dopo il primo acquisto.

Al termine di questa change Billing è una pagina di **sola fatturazione**:

- si intitola "Billing — Manage your plans, payments and receipts";
- mostra **gli abbonamenti del workspace** con le azioni self-service già esistenti (cambio piano, disdetta,
  riattivazione) e, per un'app spenta dalla piattaforma, lo dice invece di mostrare un "Attivo" muto;
- mostra lo **storico dei pagamenti** del workspace — tutte le transazioni, anche quelle fallite — con
  l'importo, l'esito e il collegamento alla ricevuta del fornitore;
- **non contiene più alcun elemento di vetrina**: né il titolo "Get an app" né la griglia delle app
  acquistabili. Chi non ha abbonamenti legge uno stato vuoto che lo manda al catalogo.

In più questa change chiude il punto aperto che UC 0095 le ha esplicitamente assegnato: **un'app freemium
(fascia gratuita di base più fasce a pagamento) risulta in vetrina già `active`, quindi dal catalogo non
esisteva alcuna via per comprarne il piano a pagamento.**

## Scope

### `services/core` — lo storico dei pagamenti diventa un dato del prodotto

1. **Nuova tabella tenant-scoped delle transazioni di fatturazione**, alimentata dalla pipeline webhook
   esistente (UC 0025). Contiene, per ogni transazione del fornitore: account, app, fascia, riferimento della
   transazione presso il fornitore, esito, importo in unità minori, valuta, ciclo di fatturazione, collegamento
   alla ricevuta, data di addebito. La scrittura avviene **nella stessa transazione di database** in cui
   l'evento viene applicato all'abbonamento: idempotente per riferimento della transazione e con la stessa
   guardia contro gli eventi fuori ordine già usata per gli abbonamenti.
2. **Completezza**: entrano tutte le transazioni del set di eventi sottoscritto, **compresi i pagamenti
   falliti e le contestazioni**. Un pagamento fallito è esattamente ciò che l'utente deve poter vedere.
3. **Nuova lettura** dello storico per l'account del token verificato: elenco ordinato dalla più recente, con
   nome dell'app, nome della fascia, ciclo, importo, valuta, esito, data e collegamento alla ricevuta quando
   c'è. Nessun parametro consente di indicare un account.
4. **Simulatore locale**: gli eventi di transazione emessi dallo stub portano i dati economici veri, risolti
   dal listino della fascia, e il percorso felice di attivazione emette anche l'evento di pagamento riuscito.
   Senza questo, in locale lo storico resterebbe sempre vuoto e la pagina non sarebbe verificabile a mano.
5. **Diritti dell'interessato**: la nuova tabella entra nell'**esportazione** dei dati dell'account e nella
   **cancellazione fisica** eseguita alla chiusura dell'account.
6. **Catalogo**: il read-model della vetrina dichiara, per una card `active`, se esiste ancora un piano a
   pagamento acquistabile (caso freemium senza abbonamento).

### `frontend` — la pagina Billing e la card del catalogo

7. **Billing ristrutturata**: nuovo titolo e sottotitolo; sezione **"Your subscriptions"** (il pannello
   self-service esistente, con la sua intestazione di sezione e il rimando al catalogo nello stato vuoto);
   nuova sezione **"Payments & receipts"**. Spariscono il titolo "Get an app" e la griglia delle app
   acquistabili. Il flusso di acquisto resta montato in Billing **solo** per la riattivazione di un
   abbonamento scaduto, che è un'azione di fatturazione, non di scoperta.
8. **Tabella dello storico**: data, app, descrizione (fascia + ciclo), importo in carattere a larghezza fissa,
   esito con il suo tono, collegamento "Receipt ↗" che apre la ricevuta del fornitore in una scheda nuova.
   Stati di caricamento, vuoto ed errore **propri della sezione**: un guasto della lettura dello storico non
   deve rendere rossa tutta la pagina né nascondere gli abbonamenti.
9. **Ricevuta non disponibile**: la riga esiste comunque, senza collegamento, senza inventare un testo che
   prometta qualcosa che non c'è.
10. **Catalogo — via all'acquisto per le app freemium**: la card `active` di un'app senza abbonamento e con
    almeno un piano a pagamento vivo guadagna un'azione secondaria che apre lo stesso flusso di acquisto già
    ospitato dal catalogo.
11. **Traduzioni**: tutti i testi nuovi nelle 5 lingue del prodotto.

### Collaudo

12. Test unitari e di integrazione lato servizio; test di componente lato frontend; percorso end-to-end di
    livello 2 sulla nuova pagina; estensione del percorso di piattaforma dell'acquisto perché la transazione
    compaia davvero nello storico dopo un acquisto vero.
13. Registro di copertura end-to-end aggiornato di conseguenza.

## Fuori scope

- **Comportamento** del checkout (UC 0024) e delle azioni self-service (UC 0028): cambiano posto, non
  comportamento. Nessuna modifica alle regole di cambio piano, disdetta, riattivazione.
- **Rimborsi, note di credito, fatture fiscali**: il venditore di record è il fornitore di pagamento, che
  emette e conserva i documenti fiscali. Noi mostriamo lo storico e il collegamento alla sua ricevuta.
- **Effetto sull'addebito durante una sospensione di piattaforma**: se un'app spenta dalla piattaforma debba
  smettere di essere addebitata è una decisione commerciale, non tecnica; UC 0076 la lascia esplicitamente
  fuori. La card dice la verità che conosciamo (abbonamento valido, dati intatti, come contattare
  l'assistenza) e la domanda resta tracciata nei punti aperti di UC 0076.
- **Cruscotto operativo** e scorciatoia "Payments & receipts" dalla dashboard: è UC 0097.
- **Storico pagamenti nella console di piattaforma** (vista dell'amministratore): non richiesto qui.

## Criteri di accettazione

- [ ] La pagina Billing non contiene **alcun** elemento di vetrina: né il titolo "Get an app", né la griglia
      delle app acquistabili; il titolo è "Billing" con il sottotitolo sui piani, i pagamenti e le ricevute.
- [ ] Un workspace **senza abbonamenti** vede uno stato vuoto con un rimando esplicito al catalogo.
- [ ] La sezione **"Your subscriptions"** elenca gli abbonamenti con piano, quota inclusa, rinnovo/scadenza e
      le azioni self-service esistenti; per un'app spenta dalla piattaforma mostra il badge e l'avviso
      esplicativo, non un "Attivo" muto.
- [ ] La sezione **"Payments & receipts"** mostra lo storico reale del workspace — data, app, descrizione,
      importo, esito, collegamento alla ricevuta — comprese le righe con esito **fallito**.
- [ ] Una riga senza ricevuta disponibile compare comunque, senza collegamento.
- [ ] Un guasto della lettura dello storico mostra l'errore con la riprova **solo** in quella sezione: gli
      abbonamenti restano visibili.
- [ ] Dopo un acquisto vero in locale (stub del fornitore) la transazione compare nello storico.
- [ ] Le transazioni sono lette con il filtro riga per riga sull'account del token verificato; nessun
      parametro del client può indicare un account.
- [ ] La card `active` di un'app freemium senza abbonamento offre una via all'acquisto del piano a pagamento.
- [ ] Testi nuovi presenti in tutte e 5 le lingue.
- [ ] `./run-tests.sh` (suite completa) verde e registro di copertura end-to-end coerente.

## Invarianti appgrove toccati

- **`tenant_id` solo dal token verificato** — la lettura dello storico ricava l'account dal contesto del
  chiamante e non espone alcun parametro con cui indicarne uno. La **scrittura** avviene fuori da una
  richiesta autenticata (consumer asincrono): l'account viene dai dati personalizzati del payload **firmato**,
  esattamente come già avviene per gli abbonamenti — la fiducia viene dalla firma, non da un input del client.
- **Filtro riga per riga `WHERE tenant_id`** — su ogni lettura dello storico; il catalogo e il listino restano
  dati di piattaforma.
- **Modulo Terraform `microsaas_app`** — non toccato: nessuna infrastruttura nuova.
- **Logging strutturato** — la lettura dello storico e la registrazione di una transazione emettono log con
  `tenant_id`, `app_id`, `user_id`.

## Requisiti di test

- **Unità (servizio)**: composizione della riga di storico a partire da una transazione (app, fascia, ciclo,
  importo, esito); una transazione la cui app o fascia non esiste più resta mostrabile.
- **Integrazione (servizio, database reale)**: evento di pagamento riuscito → transazione registrata; **stesso
  evento due volte** → una sola riga; evento **fuori ordine** → non regredisce lo stato della riga; pagamento
  **fallito** e contestazione → riga con l'esito giusto; due account → nessuno vede le transazioni dell'altro;
  token senza account → rifiuto chiuso.
- **Componente (frontend)**: tabella con esito riuscito e fallito, riga senza ricevuta, stato vuoto, errore
  della sola sezione pagamenti con gli abbonamenti visibili, assenza di elementi di vetrina nella pagina.
- **End-to-end livello 2**: pagina senza elementi di catalogo; card di un'app spenta dalla piattaforma con
  l'avviso; storico con un pagamento fallito; stato vuoto con rimando al catalogo; guasto della sola sezione
  pagamenti.
- **Percorso di piattaforma**: estensione dell'acquisto — dopo un acquisto vero con webhook vero, la
  transazione compare nello storico di Billing.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — sola aggiunta: nuova tabella, nuova lettura, nuovo campo facoltativo nel catalogo |
| Contratto cross-area | Sì — nuova lettura `GET /api/platform/v1/me/payments` e nuovo campo nella vetrina; specifica OpenAPI e tipi del frontend rigenerati nello stesso commit |
| Version bump | minor |
