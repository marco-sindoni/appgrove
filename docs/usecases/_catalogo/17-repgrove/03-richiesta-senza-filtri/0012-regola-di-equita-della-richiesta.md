# 0012 — Regola di equità della richiesta

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 03 — Richiesta di recensione senza filtri
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vuole più recensioni ma non vuole guai
> voglio scegliere **con quale regola** l'app decide chi invitare, fra due sole possibilità che non guardano se il
> cliente è contento
> così da sapere che quello che sto facendo è quello che le piattaforme chiedono, e da poterlo dimostrare.

**Contesto. È la storia che definisce il prodotto.** Le due piattaforme dicono la stessa cosa in due modi. Google
vieta di «sollecitare selettivamente recensioni positive dai clienti». Trustpilot impone di «invitare in modo
coerente ed equo — invitare tutti allo stesso modo, indipendentemente dal fatto che abbiano avuto un'esperienza
positiva o negativa» e vieta di «scegliere quali clienti invitare», compreso l'invitare in un punto del percorso
che raggiunge solo i soddisfatti. Ammette però esplicitamente, in alternativa all'«invita tutti», un **criterio
imparziale** — per esempio un cliente ogni tre (descrizione §2.3, fonti 1, 3, 4).

Da qui la forma del prodotto: la regola ha **due sole possibilità**, `tutti` e `uno_ogni_n`, e nessuna delle due
prende in ingresso il giudizio del cliente. Non è una limitazione da compensare altrove: è la funzione.

E qui c'è il punto che rende questa storia diversa da tutte le altre dell'app: **la terza opzione va spiegata,
non nascosta**. Il cliente arriverà da un prodotto che gliela offriva. Se non trova la spiegazione, penserà che
manchi qualcosa e la cercherà altrove. La schermata dice, con parole sue, che cosa sarebbe successo se l'avesse
usata: rimozione delle recensioni, avviso al consumatore sul profilo, sospensione.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni sede si imposta una regola di equità con **due sole forme**: `tutti` (ogni servizio erogato
   ammissibile riceve l'invito) oppure `uno_ogni_n` con `n` fra 2 e 10 (viene invitato un servizio ogni `n`, in
   ordine di erogazione, senza guardare nient'altro).
2. **RF-2** — La regola è **ad accrescimento**: non si modifica, se ne aggiunge una nuova con una decorrenza. La
   storia delle regole resta visibile ed esportabile, perché è materiale di prova.
3. **RF-3** — La selezione con `uno_ogni_n` è **deterministica e verificabile**: dato lo stesso insieme di servizi
   erogati, produce sempre la stessa scelta, e l'app sa dire per un singolo servizio perché è stato selezionato o
   no.
4. **RF-4** — Esistono **esclusioni lecite**, e sono un elenco chiuso: nessun recapito, cliente marcato «non
   contattare», duplicato confermato, invito già inviato per lo stesso servizio, finestra dei trenta giorni
   scaduta (storia 0015). Nessuna di esse dipende dalla soddisfazione del cliente. L'elenco è chiuso **nel
   codice**: aggiungere un motivo è una modifica visibile in revisione, non una configurazione.
5. **RF-5** — La schermata della regola contiene una spiegazione, in linguaggio comune, del perché **non** esiste
   la possibilità di invitare solo i clienti soddisfatti, con i riferimenti alle regole delle due piattaforme e
   alle conseguenze per il profilo del cliente.
6. **RF-6** — Un giudizio interno raccolto dall'azienda (se esiste, per esempio un voto lasciato alla cassa)
   **non può in nessun modo** entrare nella decisione di invitare: non esiste un campo che lo permetta, e la
   struttura della decisione non ha accesso a quel dato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `regola_di_equita` filtra per `tenant_id`
  preso dal token verificato; le regole di un account non influenzano mai la selezione di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/recensioni/v1/sedi/{id}/regola-equita` e
  `GET /api/recensioni/v1/servizi/{id}/motivo-selezione`, che risponde perché un singolo servizio è stato scelto o
  escluso; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `regola_di_equita` ad accrescimento (storia 0002): il vincolo di non
  aggiornabilità è imposto dal database. La decisione di selezione si materializza in
  `richiesta_recensione.stato` e `motivo`, non si ricalcola a posteriori.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Richieste* → «Regola di equità»: due sole scelte, la storia delle
  regole, e il riquadro esplicativo del RF-5. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe in `en, it, fr, es, de`, **compreso il testo esplicativo**, che
  è il pezzo di interfaccia più importante dell'app e va tradotto con cura, non a orecchio.
- **RT-6 — Varchi e quota (§6, §7).** Impostare la regola richiede ruolo `admin` o `owner`; con abbonamento non
  attivo risponde `402`. Nessun consumo di quota.
- **RT-7 — Esposizione conversazionale (§12).** La regola è **leggibile** dagli strumenti
  (`stato_delle_richieste` la riporta) ma **non modificabile** da un assistente: cambiare il criterio con cui si
  invita è una decisione dell'azienda, non un'operazione da delegare. Dichiarato nella storia 0027.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. Il motivo dell'esclusione è però un'informazione
  **su una persona**: va conservato con la richiesta e cancellato con lei.
- **RT-9 — Registrazione eventi (§14).** `regola impostata` con forma e valore, `selezione eseguita: n invitati,
  m esclusi per motivo`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza recapiti.

## 4. Criteri di accettazione

**CA-1 — Invita tutti**
- **Dato** una sede con regola `tutti` e cinque servizi erogati ammissibili
- **Quando** si calcola la selezione
- **Allora** tutti e cinque risultano da invitare

**CA-2 — Uno ogni tre**
- **Dato** una sede con regola `uno_ogni_n` con `n = 3` e nove servizi erogati in ordine
- **Quando** si calcola la selezione
- **Allora** ne risultano selezionati tre, sempre gli stessi a parità di dati, e l'app sa dire per ciascuno degli
  altri sei perché non è stato scelto

**CA-3 — Le esclusioni sono lecite e spiegate**
- **Dato** un servizio senza recapito, uno di un cliente «non contattare» e uno già invitato
- **Quando** si calcola la selezione
- **Allora** tutti e tre sono esclusi, ciascuno con il proprio motivo dall'elenco chiuso, e nessuno con un motivo
  che riguardi la soddisfazione

**CA-4 — La regola non si riscrive**
- **Dato** una regola già impostata
- **Quando** si tenta di modificarla
- **Allora** l'operazione è rifiutata e l'app propone di aggiungerne una nuova con decorrenza

**CA-5 — Isolamento fra account**
- **Dato** due account con regole diverse sulla stessa quantità di servizi
- **Quando** si calcola la selezione per `A`
- **Allora** usa la regola di `A` e nessun servizio di `B` compare nel calcolo

**CA-6 — La terza opzione non c'è, e si vede perché**
- **Dato** la schermata della regola
- **Quando** si cerca un modo di invitare solo i clienti soddisfatti
- **Allora** non esiste nessun campo, nessuna casella e nessuna impostazione avanzata che lo permetta, e la
  schermata spiega il motivo con il riferimento alle regole delle piattaforme

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul determinismo di `uno_ogni_n` e sull'elenco chiuso dei motivi di esclusione;
      prova di **integrazione** sulle rotte con database effimero;
- [ ] prova di **isolamento fra account** sulla regola e sulla selezione;
- [ ] **prova end-to-end**: *coprire ora* il passo «imposto la regola di equità» nel percorso `[J-RECENSIONI]`, e
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, testo esplicativo compreso;
- [ ] **manifesto dei dati**: nessuna voce nuova; il motivo di esclusione è conservato e cancellato con la
      richiesta;
- [ ] **registro delle decisioni** compilato, con le fonti delle regole delle piattaforme citate per esteso;
- [ ] contratto degli **strumenti conversazionali**: dichiarato che la regola è in sola lettura per gli strumenti;
- [ ] controllo automatico di **accessibilità** verde sulle schermate introdotte.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0011` | serve l'insieme dei servizi erogati su cui la regola opera |

## 7. Fuori ambito

- l'invio vero — storia 0014;
- il testo del messaggio — storia 0013;
- il registro di equità esportabile — storia 0016: qui si produce il dato, lì lo si rende un documento.

## 8. Punti aperti

- **Nessuno sul merito**: le due forme della regola discendono da fonti ufficiali (descrizione §2.3) e non sono
  una scelta di prodotto.
- **Da verificare in fase di scrittura del testo esplicativo**: il tono. Deve informare, non spaventare, e non
  deve sembrare che stiamo accusando i concorrenti. Il modo che consiglio è citare le regole e le conseguenze,
  senza aggettivi.
</content>
