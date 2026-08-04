# 0013 — Fotografie del trattamento

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 03 — Scheda tecnica e storia del cliente
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatrice
> voglio allegare alla scheda tecnica la foto del prima e del dopo, con il consenso della cliente registrato
> così da ritrovare il risultato quando lei mi chiede «come quella volta», senza tenermi le foto sul telefono.

**Contesto.** Tutti i gestionali italiani esaminati tengono le fotografie prima/dopo nella scheda cliente (§2.5
della descrizione): è una funzione attesa, e oggi quelle foto stanno nel telefono personale dell'operatrice, che è
il posto peggiore in cui potrebbero stare. Ma un'immagine di una persona è **il dato più identificante di tutta
l'applicazione**, può rivelare informazioni sulla salute senza che nessuno l'abbia scritto (una diradazione, una
irritazione), e in certi casi riguarda **minori** — perché nei saloni si tagliano anche i capelli ai bambini. È
per questo che è una storia a sé e non un allegato della `0010`.

## 2. Requisiti funzionali

1. **RF-1** — A una scheda tecnica si possono allegare fotografie, marcate «prima» o «dopo», scattate o caricate
   dal dispositivo.
2. **RF-2** — Non si carica nessuna fotografia se non è stato **registrato il consenso** del cliente, con la sua
   data: il consenso è un varco, non una casella accanto.
3. **RF-3** — Il consenso è **revocabile** in qualunque momento: alla revoca le fotografie di quel cliente si
   cancellano fisicamente, e la revoca lascia una riga di prova.
4. **RF-4** — Le fotografie hanno una **scadenza** propria, più breve di quella della scheda tecnica: alla
   scadenza si cancellano da sole.
5. **RF-5** — Le fotografie **non compaiono** negli strumenti conversazionali, nella console di amministrazione e
   in nessuna esportazione che non sia quella richiesta dall'interessato.
6. **RF-6** — Se il cliente è un **minore**, il programma lo segnala e richiede la registrazione di un consenso
   prestato da chi ne ha la responsabilità: senza, la fotografia non si carica.

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** ⚠️ Voce nuova nel manifesto in italiano e inglese:
  `foto_trattamento.immagine` — di chi: il cliente del salone; che dato è: **immagine di una persona, dato
  particolarmente identificante**; finalità: mostrare il prima e il dopo; base giuridica: **consenso**, revocabile,
  registrato con data; durata: **24 mesi o fino alla revoca** (proposta). Campo annotato `@PersonalData`; tabella
  `foto_trattamento` in `exportData` e `purgeData`. **È la voce più probabile da dimenticare
  nell'esportazione**, perché l'immagine non è una colonna di testo (§6 della descrizione).
- **RT-2 — Nessun trattamento biometrico.** Le immagini **non sono mai** sottoposte a elaborazioni tecniche che
  permettano di identificare univocamente una persona: nessun riconoscimento, nessun confronto automatico,
  nessuna estrazione di caratteristiche. È ciò che le tiene fuori dalla definizione di dato biometrico
  dell'articolo 9, e va scritto sia nel manifesto sia in una prova che verifichi l'assenza di quelle chiamate.
- **RT-3 — Dove stanno.** A riposo **solo in regione europea**, cifrate, raggiungibili solo attraverso collegamenti
  a scadenza breve generati dal server e mai indirizzabili in modo indovinabile.
- **RT-4 — Isolamento fra account (§1).** Il collegamento a scadenza è legato al `tenant_id` del token verificato:
  un collegamento valido per un account non apre nulla in un altro.
- **RT-5 — Interfaccia di programmazione (§2).** `POST|DELETE /api/<app>/v1/schede-tecniche/{id}/foto`,
  `GET /api/<app>/v1/foto/{id}` con collegamento a scadenza; validazione di formato e dimensione; errori in
  `problem+json`; OpenAPI aggiornata.
- **RT-6 — Persistenza (§8).** Tabella `foto_trattamento` con `tenant_id`, UUID versione 7, riferimento alla
  scheda, momento, riferimento al consenso, scadenza, colonne di controllo. La cancellazione qui è **fisica**, non
  logica: una fotografia cancellata logicamente è una fotografia che c'è ancora.
- **RT-7 — Modulo frontend (§3, §5).** Il caricamento è bloccato finché il consenso non è registrato, con un
  messaggio che dice perché; le miniature portano la data di scadenza. Solo token del sistema di design.
- **RT-8 — Cinque lingue (§4).** Testo del consenso, avviso sul minore, messaggi di blocco e di revoca in
  `en, it, fr, es, de`, **rivisti come quelli della storia `0012`**.
- **RT-9 — Esposizione conversazionale (§12).** **Nessuno strumento** legge le fotografie, né di lettura né di
  scrittura. È un divieto, non un'omissione, ed è dichiarato nel contratto (storia `0028`).
- **RT-10 — Registrazione eventi (§14).** `foto caricata`, `consenso revocato`, `foto cancellata per scadenza` con
  `tenant_id`, `app_id`, `user_id` e correlazione — mai l'immagine, mai il nome.

## 4. Criteri di accettazione

**CA-1 — Senza consenso non si carica**
- **Dato** una cliente senza consenso registrato
- **Quando** si tenta di allegare una fotografia
- **Allora** l'operazione è rifiutata con un messaggio che spiega come registrare il consenso, e nulla viene
  caricato

**CA-2 — La revoca cancella davvero**
- **Dato** una cliente con tre fotografie e il consenso registrato
- **Quando** si revoca il consenso
- **Allora** le tre immagini non esistono più fisicamente, e resta una riga di prova che dice quando e per mano di
  chi

**CA-3 — La scadenza fa il suo lavoro**
- **Dato** una fotografia con scadenza superata
- **Quando** la lavorazione periodica gira
- **Allora** l'immagine è cancellata e la scheda tecnica resta, senza buchi visibili

**CA-4 — Il minore ha un varco in più**
- **Dato** un cliente registrato come minore
- **Quando** si tenta di allegare una fotografia senza il consenso di chi ne ha la responsabilità
- **Allora** l'operazione è rifiutata

**CA-5 — Il collegamento non viaggia**
- **Dato** un collegamento a scadenza generato per un account
- **Quando** lo si usa da un altro account, o dopo la scadenza
- **Allora** non apre nulla

**CA-6 — Gli strumenti conversazionali non le vedono**
- **Dato** una scheda tecnica con fotografie
- **Quando** si invoca lo strumento di lettura della scheda
- **Allora** la risposta non contiene né immagini né riferimenti a immagini

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite intera: la storia tocca `compliance`);
- [ ] prove di **unità** su consenso, scadenza e revoca; di **integrazione** su caricamento, collegamento a
      scadenza e cancellazione fisica;
- [ ] prova di **isolamento fra account** su caricamento, lettura e collegamento;
- [ ] prova che **nessuna elaborazione biometrica** è presente nel percorso delle immagini;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-SALONGROVE]` della storia `0030`, limitato al
      blocco senza consenso (una prova end-to-end non carica immagini vere);
- [ ] **traduzioni** in tutte e cinque le lingue, marcate come da rivedere;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese; tabella `foto_trattamento` presente **sia** in
      esportazione **sia** in cancellazione — verificato, non assunto;
- [ ] **registro delle decisioni**: consenso come varco, cancellazione fisica alla revoca, scadenza propria,
      divieto biometrico, esclusione dagli strumenti conversazionali e dalla console;
- [ ] avvio locale invariato; i dati di prova **non contengono immagini di persone**, nemmeno finte.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | la fotografia si appende a una scheda tecnica |
| storia `0012` | il perimetro e i suoi presidi sono il contesto in cui questa funzione è ammissibile |
| infrastruttura di archiviazione dei file in regione europea | non esiste ancora un uso di file binari in questa suite: va verificato che la piattaforma lo preveda |

## 7. Fuori ambito

- la pubblicazione delle fotografie verso l'esterno (sito, canali sociali): è un effetto verso l'esterno e una
  decisione di prodotto, **fuori** da questa stesura;
- il confronto automatico fra prima e dopo: sarebbe elaborazione dell'immagine, ed è espressamente vietato qui;
- il ritocco o il ritaglio automatico: fuori ambito.

## 8. Punti aperti

**Se questa funzione valga il suo rischio.** È l'unica dell'applicazione che introduce una categoria di dati nuova
per l'intera piattaforma (le immagini di persone) e che richiede archiviazione di file, cifratura, collegamenti a
scadenza, consenso e una lavorazione di scadenza. Il beneficio è reale ma non è la ragione per cui un salone
compra. **Proposta alternativa da valutare: rimandarla e rilasciare il verticale senza fotografie**, tenendo il
resto dell'epica. Non la decido: è una scelta di prodotto e di rischio, e va messa accanto al punto 4 dei rischi.

**Il minore.** Il programma può sapere che un cliente è minore solo se qualcuno gliel'ha detto: non c'è nessun
controllo che lo accerti. Il presidio è quindi debole per costruzione, e va detto invece che lasciato credere.
