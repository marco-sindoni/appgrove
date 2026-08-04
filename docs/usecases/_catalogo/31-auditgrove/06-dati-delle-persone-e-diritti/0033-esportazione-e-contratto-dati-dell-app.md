# 0033 — Esportazione e contratto dati dell'app

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 06 — Dati delle persone e diritti
**Storia**: `0033` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0021`, `0030`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che presidia la conformità della piattaforma
> voglio che AuditGrove dichiari nel manifesto ogni campo che riguarda una persona e li implementi tutti
> nell'esportazione e nella cancellazione
> così da poter rispondere a una richiesta di un interessato senza scoprire, a richiesta arrivata, che una tabella
> era rimasta indietro.

**Contesto.** È la storia che chiude l'epica e che tiene insieme tutte le voci sparse nelle precedenti. Il vincolo
di piattaforma è netto: il manifesto dei dati è la fonte unica da cui si generano il registro dei trattamenti e gli
strumenti di esportazione e cancellazione, e **un campo non dichiarato è un campo che l'esportazione dimentica e
la cancellazione lascia indietro**. In un'app che tratta identificativi in sette tabelle diverse, dimenticarne una
è il difetto più probabile — ed è anche quello che si scopre nel momento peggiore.

Questa storia porta però un problema che nessun'altra app del catalogo ha, e che va affrontato invece che
aggirato: **il contratto dati impone una cancellazione fisica, e la ragion d'essere di questa app impone che le
righe della catena non si tocchino**. Le due cose non si conciliano con il codice.

## 2. Requisiti funzionali

1. **RF-1** — Il manifesto `docs/compliance/manifests/agentaudit.yaml` è completo, in italiano e inglese su ogni
   testo, e contiene tutte le voci introdotte dall'applicazione: identificativo del richiedente, identificativo
   dell'agente, identificativo di chi ha deciso un nulla osta, **motivo scritto** della decisione, impronte dei
   parametri, contenuto allegato, chiavi di contenuto, contatto di avviso della sorgente, identificativo di chi ha
   esportato o generato un rapporto, attribuzione del ruolo di revisore.
2. **RF-2** — Esiste il contratto `AgentauditDataContract` che implementa `appId()`, `exportData(scope)`,
   `purgeData(scope)` e `manifest()`.
3. **RF-3** — **Ogni** tabella che contiene dati di persone compare sia in `exportData` sia in `purgeData`:
   azioni, nulla osta, sorgenti, contenuti allegati, chiavi di contenuto, avvisi e destinatari, esportazioni,
   rapporti.
4. **RF-4** — Per la tabella delle azioni, `purgeData` **dichiara esplicitamente** il proprio comportamento
   durante il periodo di conservazione, invece di eseguire silenziosamente una cancellazione o silenziosamente
   nulla: la risposta indica che cosa è stato cancellato, che cosa è stato reso illeggibile e che cosa è stato
   trattenuto, con il motivo e la data in cui potrà essere cancellato.
5. **RF-5** — L'esportazione dei dati di un interessato e la cancellazione **restano accessibili anche quando
   l'app è disabilitata o l'abbonamento è scaduto**.
6. **RF-6** — Una prova automatica fa fallire la compilazione o la suite se una tabella con campi annotati non
   compare in entrambi i metodi del contratto: il presidio non è la memoria di chi scrive, è il collaudo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Esportazione e cancellazione operano sull'account e sull'ambito
  richiesti, con `tenant_id` preso dal token verificato; nessuna operazione può attraversare due account, e un
  ambito che indicasse un altro account non produce nulla.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova dell'app: esportazione e
  cancellazione dei dati dell'interessato passano dalle rotte comuni della piattaforma, che chiamano il contratto
  dati dell'app. Errori in `application/problem+json`; definizione OpenAPI aggiornata dove serve.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. La cancellazione è **fisica** dove è ammessa (contenuti,
  chiavi, avvisi, esportazioni, rapporti, sorgenti) e lascia una riga di prova nel registro delle purghe di
  piattaforma. Sulla tabella delle azioni valgono la regola di sola aggiunta della storia 0002 e la dichiarazione
  del RF-4.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata propria: i diritti dell'interessato si esercitano dalle
  schermate comuni della piattaforma. Il modulo mostra però, dove utile, che i contenuti di un intervallo sono
  stati resi illeggibili (storia 0032).
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile nuovo nel modulo. **Attenzione a non confondere due
  elenchi**: l'interfaccia vuole cinque lingue, il **manifesto dei dati ne vuole due** — italiano e inglese — su
  ogni testo, ed è il vincolo che vale qui.
- **RT-6 — Varchi e quota (§6, §7).** I diritti dell'interessato **non** passano dal varco dell'abbonamento: con
  `canceled` restano accessibili, mentre il resto dell'app risponde `402`. Non consumano quota, tranne la riga di
  registro che traccia una cancellazione di contenuti (storia 0032).
- **RT-7 — Esposizione conversazionale (§12).** Né l'esportazione dei dati di un interessato né la cancellazione
  vengono esposte come strumenti a un assistente: sono operazioni su dati di terzi con effetti irreversibili, e
  devono avere una persona identificata all'origine. Il divieto va scritto nel contratto degli strumenti (storia
  0035) accanto agli altri due.
- **RT-8 — Dati personali (§10).** È la storia che *è* il requisito: manifesto completo in italiano e inglese,
  campi annotati `@PersonalData` — con la conseguenza che un campo annotato e non dichiarato fa fallire la
  compilazione — e ogni tabella presente in `exportData` e `purgeData`. Va dichiarato anche il **doppio ruolo**
  dell'app (§6.1 della [descrizione dell'applicazione](../application-description.md)): titolare per la sorgente
  nativa appgrove, responsabile per le sorgenti del cliente. Le due nature convivono nella stessa tabella e la
  riga porta l'indicazione della natura della sorgente.
- **RT-9 — Registrazione eventi (§14).** Esportazione e cancellazione dei dati di un interessato sono registrate
  con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali nel registro
  tecnico.

## 4. Criteri di accettazione

**CA-1 — Nessuna tabella dimenticata**
- **Dato** il codice dell'applicazione con tutti i campi annotati
- **Quando** si esegue la suite di conformità
- **Allora** ogni tabella con campi annotati risulta presente sia in `exportData` sia in `purgeData`, e
  l'aggiunta di una tabella nuova senza aggiornarli fa fallire il collaudo

**CA-2 — L'esportazione restituisce tutto ciò che è dichiarato**
- **Dato** un account con azioni, nulla osta decisi, sorgenti, avvisi, esportazioni e rapporti
- **Quando** si esegue l'esportazione dei dati
- **Allora** il risultato contiene i dati di tutte le tabelle dichiarate nel manifesto, e nulla che non sia
  dichiarato

**CA-3 — La cancellazione dice la verità sulle azioni**
- **Dato** un account con azioni ancora dentro il periodo di conservazione
- **Quando** si esegue la cancellazione
- **Allora** la risposta dichiara esplicitamente che cosa è stato cancellato, che cosa è stato reso illeggibile e
  che le righe della catena sono trattenute fino a una data indicata, con il motivo — e non finge di aver
  cancellato tutto

**CA-4 — I diritti sopravvivono all'abbonamento**
- **Dato** un account con abbonamento `canceled`
- **Quando** si chiede l'esportazione dei dati o la cancellazione
- **Allora** entrambe funzionano, mentre le rotte ordinarie dell'app rispondono `402`

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** si esporta o si cancella per `A`
- **Allora** nessun dato di `B` compare nell'esportazione e nessuna sua riga viene toccata

## 5. Definizione di fatto

- [ ] **l'esito della revisione legale sul conflitto fra prova e cancellazione è recepito nel comportamento
      dichiarato del RF-4** — voce di sbarramento: è la risposta che il contratto dati deve dare, e non la può
      inventare il codice;
- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit), compresa
      l'area di conformità;
- [ ] prove di **unità** sulla completezza del contratto dati e di **integrazione** su esportazione e
      cancellazione, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su esportazione e cancellazione;
- [ ] **prova end-to-end**: **coprire ora** — il percorso `[J-AGENTAUDIT]` (storia 0037) riceve il passo
      «esportazione dei dati dell'interessato con app disabilitata», e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato;
- [ ] **traduzioni**: nessun testo visibile nuovo; **manifesto in italiano e inglese**, che è l'elenco che vale
      qui;
- [ ] **manifesto dei dati** completo, con tutte le voci elencate al RF-1 e il doppio ruolo dichiarato;
- [ ] **registro delle decisioni** compilato, con le voci su: comportamento di `purgeData` sulle azioni, doppio
      ruolo titolare/responsabile, divieto di esposizione conversazionale dei diritti dell'interessato;
- [ ] contratto degli **strumenti conversazionali**: dichiarati i divieti, con il motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] il registro dei trattamenti generato dal manifesto è aggiornato e supera il controllo di aggiornamento.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | Introduce i primi campi che riguardano una persona (richiedente, agente) |
| storia `0021` | Introduce chi ha deciso e il motivo scritto |
| storia `0030` | Introduce contenuti e chiavi, le due tabelle più delicate |
| storie `0026`, `0027`, `0028`, `0029` | Introducono destinatari, esportazioni, rapporti e attribuzioni di ruolo, che vanno tutti nel manifesto |
| **Revisione legale** | Il comportamento dichiarato del RF-4 è la sua risposta, non una scelta di implementazione |

## 7. Fuori ambito

- **la risposta formale all'interessato**: la fa il titolare, che per le sorgenti del cliente è il cliente;
- **la cancellazione delle righe della catena**: non si fa (storia 0032, §8);
- **la modifica del registro dei trattamenti a mano**: si genera dal manifesto, che è la fonte unica;
- **l'accordo sul trattamento fra noi e il cliente**: è un documento, non codice, ed è un punto della revisione
  legale (§6.1 della descrizione dell'applicazione).

## 8. Punti aperti

- **Che cosa risponde `purgeData` sulle azioni durante il periodo di conservazione.** È **la** domanda aperta di
  tutta l'epica, e non la chiude questa storia: la chiude la revisione legale. Le tre risposte possibili — si
  cancella comunque, si trattiene con motivazione fino alla scadenza, si trattiene solo per certe basi giuridiche
  — producono tre prodotti diversi. Chi chiude: revisione legale, poi sviluppatore.
- **Se il doppio ruolo debba essere due manifesti o uno.** Titolare per la sorgente nativa, responsabile per le
  sorgenti del cliente: il formato del manifesto prevede una app, un manifesto. Propongo un manifesto solo con la
  natura dichiarata voce per voce, ma è una domanda che riguarda lo strumento di conformità di piattaforma e non
  solo questa app.
- **Se la cancellazione debba poter essere parziale per interessato** e non per periodo: dipende dalla
  granularità delle chiavi (storia 0030) ed è il limite già dichiarato lì.
