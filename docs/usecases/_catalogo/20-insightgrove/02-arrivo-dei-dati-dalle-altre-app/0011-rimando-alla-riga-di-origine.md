# 0011 — Rimando alla riga d'origine

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 02 — Arrivo dei dati dalle altre app
**Storia**: `0011` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vede «crediti scaduti: 12.400 €» e non ci crede
> voglio poter arrivare in due clic alle fatture che compongono quella somma
> così da poter controllare invece di doverci credere.

**Contesto.** Un aggregato non si può ispezionare: la somma non dice *di chi*. La tentazione, a questo punto,
è di arricchire il fatto finché non diventa una copia della riga d'origine — e a quel punto InsightGrove
duplicherebbe i dati di tutte le altre app, con tutta la superficie di conformità che ne consegue (§4.2 della
[descrizione](../application-description.md)). La soluzione opposta è **non recuperare niente**: il fatto porta
un riferimento opaco, InsightGrove lo trasforma in un collegamento che apre **l'app sorgente sulla sua
schermata**, e là valgono il filtro per account, l'abilitazione e il ruolo **di quell'app**. Il dettaglio si vede
dove sta, con i controlli di dove sta.

## 2. Requisiti funzionali

1. **RF-1** — Ogni fatto porta un riferimento d'origine composto da: applicazione, tipo di entità, identificativo
   opaco della riga. Nient'altro.
2. **RF-2** — Il servizio traduce il riferimento in un **indirizzo di destinazione** dentro il backoffice —
   la schermata dell'app sorgente per quel tipo di entità e quell'identificativo — usando una mappa che ogni
   fonte dichiara nel proprio contratto.
3. **RF-3** — L'interfaccia mostra i rimandi dovunque compaia un numero risalibile: nella scheda del numero
   (storia 0016), nelle risposte del copilota (storia 0023) e nelle esportazioni (storia 0027).
4. **RF-4** — Il numero di rimandi mostrati è **limitato** (proposta: dieci, i più rilevanti per contributo al
   valore): l'obiettivo è controllare a campione, non scaricare l'archivio.
5. **RF-5** — Seguendo un rimando, l'utente atterra nell'app sorgente. Se non è abilitato a quell'app, o il suo
   ruolo non gli consente di vedere quella riga, **è l'app sorgente a negare**: InsightGrove non anticipa il
   giudizio e non nasconde il rimando in base a un proprio calcolo.
6. **RF-6** — Se l'app sorgente non è abilitata per l'account, il rimando è mostrato **disattivato**, con la
   spiegazione: l'utente deve capire che il dato esiste ma sta altrove.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il riferimento d'origine si legge solo insieme al fatto, che è già
  filtrato per `tenant_id` dal gettone verificato: non esiste una risorsa che risolva un riferimento senza
  passare dal fatto. Un identificativo opaco altrui non è risolvibile perché non appartiene ad alcun fatto
  dell'account chiamante.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna chiamata verso l'app sorgente: **un'app non chiama
  un'altra app** ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §2). La traduzione del riferimento in
  indirizzo è pura composizione di stringhe su una mappa dichiarata; nessuna rete.
- **RT-4 — Modulo frontend (§3, §5).** La navigazione al rimando avviene dentro il backoffice, con il
  meccanismo di navigazione della shell; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I testi dei rimandi e delle spiegazioni («l'app fatturazione non è attiva su
  questo account») esistono in `en, it, fr, es, de`.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il riferimento è opaco e non dice niente di per
  sé. È precisamente il motivo per cui questa è la soluzione giusta al problema del dettaglio.
- **RT-14 — Registrazione eventi (§14).** «Rimando seguito» con `tenant_id`, `app_id` di destinazione, tipo di
  entità e identificativo di correlazione; **mai** l'identificativo della riga, che è dato dell'account.

## 4. Criteri di accettazione

**CA-1 — Dal numero alla fattura**
- **Dato** un valore «crediti scaduti 12.400 €» composto da 14 fatti provenienti dalla fatturazione
- **Quando** l'utente apre la scheda del numero
- **Allora** vede fino a dieci rimandi, e cliccandone uno atterra sulla schermata della fattura corrispondente
  dentro l'app sorgente

**CA-2 — App sorgente non abilitata**
- **Dato** un account che ha revocato l'abbonamento alla fatturazione ma conserva i fatti storici
- **Quando** apre la scheda del numero
- **Allora** i rimandi sono mostrati disattivati con la spiegazione «l'app fatturazione non è attiva su questo
  account»

**CA-3 — Il giudizio è dell'app sorgente**
- **Dato** un utente `member` abilitato alla fatturazione, il cui ruolo non gli consente di vedere quella fattura
- **Quando** segue il rimando
- **Allora** è l'app sorgente a negare l'accesso con il proprio messaggio; InsightGrove non ha nascosto il
  rimando e non ha mostrato alcun contenuto della fattura

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B` con fatti che rimandano a righe delle rispettive app
- **Quando** un utente di `A` prova a far risolvere un riferimento d'origine appartenente a `B`
- **Allora** la risoluzione non avviene, perché quel riferimento non appartiene ad alcun fatto di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione dell'indirizzo di destinazione, e di **integrazione** sul fatto che
      nessuna chiamata di rete verso altre app viene effettuata;
- [ ] prova di **isolamento fra account**: un riferimento d'origine altrui non è risolvibile;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include il passo «dal numero alla riga
      d'origine»; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; va confermato che il riferimento è opaco;
- [ ] **registro delle decisioni** compilato, con la scelta «rimando invece di recupero» e il perché;
- [ ] contratto degli **strumenti conversazionali**: `spiega_numero` (storia 0031) restituisce anche i rimandi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | servono fatti con il loro riferimento d'origine |
| storia `0006` | la mappa da tipo di entità a schermata è dichiarata nel contratto della fonte |

## 7. Fuori ambito

- il recupero del contenuto della riga d'origine: **non si fa e non si farà**, è la scelta architetturale di
  questa app;
- la scheda del numero come schermata: storia 0016 — qui si costruisce il rimando, là lo si mostra;
- l'esportazione con i rimandi: storia 0027.

## 8. Punti aperti

- **Quali dieci rimandi si mostrano?** «I più rilevanti per contributo» è ragionevole per una somma, ma non ha
  senso per una media o per un conteggio. Raccomandazione: **i più rilevanti per una somma, i più recenti negli
  altri casi**, con il criterio dichiarato nella scheda del numero. Chiude: **sviluppatore**.
- **La mappa da tipo di entità a schermata è stabile?** Se un'app sorgente cambia l'indirizzo delle proprie
  schermate, i rimandi si rompono in silenzio. Non esiste oggi un modo di accorgersene automaticamente: è una
  lacuna dichiarata. Un rimedio parziale è che il percorso end-to-end della storia 0034 attraversi almeno un
  rimando reale.
