# 0033 — Percorso end-to-end interno

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0033` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0005`, `0012`, `0021`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che deve poter cambiare il calendario dei rinnovi senza svegliarsi di notte
> voglio una prova che percorra l'app come la percorre il cliente — dal piano fino al sollecito
> così da accorgermi di una rottura mentre scrivo il codice, e non quando un abbonato riceve un avviso sbagliato.

**Contesto.** Molte storie precedenti hanno risposto «coprire ora» alla domanda di copertura, indicando questa
storia come **proprietaria** del percorso `[J-ABBONATI]`: `0006`, `0008`, `0010`, `0011`, `0012`, `0013`, `0016`,
`0017`, `0018`, `0019`, `0020`, `0021`, `0022` e le metriche dell'epica 06. È il momento di saldare il debito: il
percorso si scrive **una volta**, attraversa lo stack locale reale con un browser vero, e diventa la rete di
sicurezza di tutta l'app.

C'è una difficoltà propria di SubGrove che va affrontata di petto, perché nessun'altra app del catalogo ce l'ha
nella stessa misura: **il valore di questa app è che le cose succedono da sole il giorno giusto**. Una prova che
non sa far passare il tempo non può verificare nulla di ciò che conta — il rinnovo che matura, l'avviso che parte
trenta giorni prima, il sollecito che parte subito e poi a scalare, la sospensione dopo l'ultimo passo. Il percorso
deve quindi poter **muovere l'orologio dell'applicazione** in modo controllato, senza attese a tempo e senza date
relative a «oggi».

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-ABBONATI.spec.ts`, eseguito senza finestra sullo
   stack locale reale, con ogni prova etichettata `[J-ABBONATI]` in testa al titolo.
2. **RF-2** — Il percorso attraversa, in un solo racconto continuo: creazione di un piano con le sue condizioni →
   prima versione di prezzo → creazione di un abbonato → sottoscrizione di un abbonamento → maturazione del rinnovo
   con generazione della scadenza → invio dell'avviso di rinnovo con la sua prova → registrazione di un esito
   fallito → primo sollecito → sospensione automatica a catena esaurita → lettura della sezione *Andamento*.
3. **RF-3** — Il percorso verifica anche i due **rifiuti** che raccontano le regole: il tetto di
   `abbonamenti_attivi` che risponde `429` con il rimedio, e la riduzione di piano sbarrata finché gli attivi
   eccedono il tetto di destinazione.
4. **RF-4** — Il percorso usa **solo** i dati di prova deterministici della storia `0005`: nomi inventati, indirizzi
   nel dominio riservato alle prove, importi tondi. Nessun dato che possa sembrare di un cliente vero.
5. **RF-5** — Il registro di copertura
   [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve la voce del percorso
   `J-ABBONATI` con l'elenco degli use case coperti, e le voci `da-coprire` lasciate dalle storie precedenti che
   questo percorso chiude vengono **tolte**: un registro che dichiara un debito già saldato è falso quanto uno che
   ne nasconde uno vero.
6. **RF-6** — Il percorso non tollera attese a tempo: ogni passo aspetta una **condizione osservabile**, e
   l'accesso avviene per via programmatica come negli altri percorsi di piattaforma.

## 3. Requisiti tecnici

- **RT-1 — Prove end-to-end (§11).** Playwright senza finestra sullo stack locale reale; niente attese a tempo;
  accesso programmatico; dati deterministici e inventati. L'etichetta `[J-ABBONATI]` in testa al titolo è ciò che
  il controllo `tools/e2e-coverage` cerca: senza, il registro diventa rosso.
- **RT-2 — Controllo del tempo.** Le lavorazioni programmate (rinnovi, avvisi, solleciti, sospensione) devono poter
  essere **innescate esplicitamente** con una data di riferimento fornita dalla prova, attraverso una via
  disponibile **solo** nel profilo locale e di collaudo. Non si aspetta la mezzanotte e non si cambia l'orologio
  della macchina. La stessa via serve alla `0012` per dimostrare l'idempotenza e il recupero dei giorni saltati.
- **RT-3 — Isolamento fra account (§1).** Il percorso lavora dentro un account creato per l'occasione e ne verifica
  il confine: un secondo account non vede né gli abbonamenti né le scadenze del primo.
- **RT-4 — Modulo frontend (§3, §5).** Il percorso passa dall'interfaccia vera, non dalle sole interfacce di
  programmazione: è l'unico modo per accorgersi che una sezione non si monta o che una tabella non compare.
- **RT-5 — Cinque lingue (§4).** Il percorso gira nella lingua predefinita; una prova breve verifica che cambiando
  lingua le schermate attraversate non mostrino chiavi al posto del testo (rete di sicurezza per il requisito delle
  cinque lingue, non una prova di traduzione).
- **RT-6 — Comunicazioni.** Avviso di rinnovo e primo sollecito si verificano sulla casella di posta locale di
  collaudo già usata dagli altri percorsi di piattaforma: si controlla che il messaggio esista, che porti il
  collegamento alla pagina dell'abbonato e che la prova d'invio sia registrata — non il suo aspetto grafico.
- **RT-7 — Dati personali (§10).** **Nessun dato personale nuovo**; i dati della prova sono inventati e vivono solo
  nell'ambiente locale.
- **RT-8 — Registrazione eventi (§14).** Nessun requisito nuovo: il percorso osserva il comportamento, non i
  registri.
- **RT-9 — Avvio locale (§15).** Il percorso si appoggia alla scoperta automatica dei servizi: nessuna riga
  incollata negli script di avvio, nessun passo manuale prima di eseguirlo.

## 4. Criteri di accettazione

**CA-1 — Il racconto completo passa**
- **Dato** uno stack locale pulito con i dati di prova della storia `0005`
- **Quando** si esegue `./run-tests.sh platform` (o l'esecuzione mirata del percorso)
- **Allora** il percorso `[J-ABBONATI]` va a termine: piano, prezzo, abbonato, abbonamento, rinnovo, avviso,
  incasso fallito, sollecito, sospensione, sezione *Andamento* con il ricavo aggiornato

**CA-2 — Il tempo si muove per volontà della prova**
- **Dato** un abbonamento mensile appena sottoscritto
- **Quando** la prova innesca la lavorazione con la data del giorno di rinnovo
- **Allora** la scadenza nuova esiste, è una sola, e ripetendo l'innesco non ne nasce una seconda

**CA-3 — L'avviso di rinnovo parte nei termini**
- **Dato** un piano con preavviso configurato
- **Quando** la prova porta la data al giorno del preavviso
- **Allora** il messaggio è nella casella di collaudo, porta il collegamento alla pagina dell'abbonato, e la prova
  d'invio è registrata sull'abbonamento

**CA-4 — I rifiuti si vedono**
- **Dato** un account al tetto di `abbonamenti_attivi`
- **Quando** si prova a sottoscriverne un altro dall'interfaccia
- **Allora** compare il messaggio che spiega come rimediare, e nulla viene creato

**CA-5 — Registro coerente**
- **Dato** il registro di copertura aggiornato
- **Quando** gira `./run-tests.sh tooling`
- **Allora** il controllo è verde: nessuna voce `da-coprire` residua per ciò che questo percorso copre, nessun
  percorso dichiarato e assente

**CA-6 — Isolamento fra account**
- **Dato** due account creati dal percorso
- **Quando** il secondo apre le sezioni dell'app
- **Allora** non vede alcun dato del primo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (intera suite prima del commit: il percorso tocca `platform` e
      `tooling`);
- [ ] percorso `[J-ABBONATI]` scritto, stabile su tre esecuzioni consecutive (niente prove intermittenti: una prova
      che passa a volte è peggio di una prova che manca);
- [ ] prova di **isolamento fra account** dentro il percorso;
- [ ] **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
      aggiornato: voce del percorso, use case coperti, voci `da-coprire` chiuse;
- [ ] **traduzioni**: nessuna stringa nuova; verificata l'assenza di chiavi non tradotte sulle schermate
      attraversate;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato: forma del controllo del tempo, confine del percorso, voci di registro
      chiuse;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione di collaudo aggiornata dove descrive i percorsi dell'app.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0005` | i dati di prova deterministici sono il presupposto del percorso |
| storia `0012` | la lavorazione dei rinnovi è il cuore di ciò che si prova, e la via di innesco nasce lì |
| storie `0021`, `0022` | sollecito e sospensione chiudono il racconto |
| storie `0027`-`0030` | la sezione *Andamento* è l'ultimo passo del percorso |
| numerazione assoluta degli use case | le voci del registro nominano gli use case **assoluti** che la skill `new-usecase` assegnerà a queste storie: qui si usano numeri locali dell'app, che non sono quelli del registro ([GUIDA-AUTORE.md](../../_kit/GUIDA-AUTORE.md) §2) |

## 7. Fuori ambito

- il percorso **pubblico** dell'abbonato (pagina firmata, disdetta, difese): storia `0034`, percorso
  `[J-ABBONATI-PUBBLICO]`;
- le prove del livello **conversazionale**: rimandate finché il server di piattaforma non esiste (storie `0031`,
  `0032`);
- il collegamento in sola lettura al fornitore di incasso (storia `0020`): dipende da un fornitore esterno e non si
  guida da una prova end-to-end;
- le prove di prestazione e di carico: non sono di questa storia.

## 8. Punti aperti

**La forma della via che muove il tempo.** Una porta che innesca lavorazioni con una data arbitraria è comodissima
per le prove e pericolosa se sopravvive fuori dal profilo locale. **Proposta**: parametro di configurazione
disponibile **solo** nei profili locale e di collaudo, con una prova che ne verifica l'assenza nel profilo di
spedizione (l'area `smoke` di `run-tests.sh` esiste per questa classe di guasti). Chiude: lo sviluppatore.

**Quanto deve essere lungo un solo percorso.** Un racconto che attraversa dieci passi è potente e lento, e quando
si rompe dice «qualcosa non va» invece di dire dove. **Proposta**: un percorso continuo per il racconto principale,
più prove d'integrazione mirate — già previste dalle singole storie — per i dettagli. Se il percorso supera i pochi
minuti, si spezza in due dichiarandolo nel registro. Chiude: lo sviluppatore.
