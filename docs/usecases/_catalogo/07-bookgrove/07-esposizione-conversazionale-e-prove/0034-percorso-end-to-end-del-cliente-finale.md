# 0034 — Percorso end-to-end del cliente finale

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 07 — Esposizione conversazionale e prove
**Storia**: `0034` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0020`, `0024`, `0033`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio una prova che percorra la superficie pubblica come la percorre una persona qualsiasi
> così da sapere che la parte più esposta dell'applicazione fa esattamente quello che deve, e niente di più.

**Contesto.** La superficie pubblica è la deviazione architetturale di questa applicazione (§11, punto 3 della
descrizione) ed è la parte con il rischio più alto. Un percorso end-to-end separato non è un lusso: è il posto in
cui si verifica, sullo stack reale, che una persona senza token possa prenotare e gestire la propria
prenotazione — e **solo** quello. Il percorso interno della storia `0033` non lo copre, perché parte da un utente
autenticato.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `[J-BOOKGROVE-PUB]` in `tools/platform-e2e/journeys/J-BOOKGROVE-PUB.spec.ts`,
   eseguito sullo stack locale reale, **senza mai autenticarsi**.
2. **RF-2** — Il percorso copre: apertura della pagina pubblica, scelta del servizio e dell'orario, accettazione
   della politica di disdetta, verifica del contatto, conferma, apertura del collegamento di gestione,
   spostamento e disdetta.
3. **RF-3** — Il percorso comprende un ramo di lista d'attesa: nessuno spazio libero, iscrizione, disdetta di
   un'altra prenotazione, offerta, accettazione.
4. **RF-4** — Il percorso comprende i **casi negativi di sicurezza**, verificati come tali: identificativo di sede
   inesistente, gettone scaduto, gettone manomesso, gettone di un'altra prenotazione, servizio non pubblico
   forzato, richieste oltre la soglia di frequenza.
5. **RF-5** — Il percorso verifica esplicitamente che dalla superficie pubblica **non** siano leggibili né
   prenotazioni altrui né dati di altri clienti.

## 3. Requisiti tecnici

- **RT-1 — Prove (§11).** Playwright senza finestra sullo stack reale; niente attese a tempo; dati inventati e
  deterministici con indirizzi su dominio `*.test`; il codice di verifica del contatto si recupera dal fornitore
  simulato, non da un messaggio vero.
- **RT-2 — Registro di copertura.** `docs/testing/copertura-e2e.yaml` aggiornato: le storie `0016`, `0017`,
  `0018`, `0019`, `0020`, `0022` e `0024` avevano dichiarato «coperta ora» sulla parte pubblica e qui trovano il
  loro test.
- **RT-3 — Isolamento fra account (§1).** Il percorso crea **due** account e verifica che dalla pagina pubblica
  dell'uno non si raggiunga nulla dell'altro: è la prova che l'identificativo di sede non è una scorciatoia.
- **RT-4 — Cinque lingue (§4).** Il percorso verifica che la pagina pubblica sia resa nella lingua richiesta e che
  nessuna chiave di traduzione compaia.
- **RT-5 — Dati personali (§10).** Nessun dato vero; il percorso verifica anche che le risposte pubbliche non
  contengano dati personali oltre a quelli dell'interessato che possiede il gettone.
- **RT-6 — Accessibilità.** Controllo automatico di accessibilità sulla pagina pubblica: è la schermata che
  raggiunge il pubblico più vario, ed è quella dove l'accessibilità conta di più.

## 4. Criteri di accettazione

**CA-1 — Il percorso esiste e passa**
- **Dato** lo stack locale avviato · **Quando** si esegue `./run-tests.sh platform` · **Allora** il percorso
  `[J-BOOKGROVE-PUB]` è verde

**CA-2 — Prenotazione senza autenticazione**
- **Dato** una persona senza token · **Quando** percorre la pagina pubblica fino alla conferma · **Allora**
  ottiene una prenotazione e il collegamento per gestirla

**CA-3 — Gestione della propria prenotazione**
- **Dato** il collegamento ricevuto · **Quando** la persona sposta e poi disdice · **Allora** l'agenda
  dell'attività lo riflette e la traccia dice che è stato il cliente

**CA-4 — Casi negativi di sicurezza**
- **Dato** gettone scaduto, manomesso, di un'altra prenotazione, identificativo inesistente, servizio non
  pubblico forzato · **Quando** si tentano · **Allora** sono tutti respinti, con risposte indistinguibili dove
  previsto

**CA-5 — Niente trapela fra account**
- **Dato** due account · **Quando** dalla pagina pubblica del primo si tenta di raggiungere dati del secondo
- **Allora** non è possibile in nessuno dei modi provati

**CA-6 — Registro coerente**
- **Dato** il registro di copertura · **Quando** si esegue il controllo dell'area `tooling` · **Allora** è verde

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` completo;
- [ ] prove di **unità** e **integrazione**: non applicabile, questa storia è essa stessa una prova;
- [ ] prova di **isolamento fra account**: è uno dei criteri di accettazione (CA-5);
- [ ] **prova end-to-end**: **coperta ora** — è l'oggetto della storia, con
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato e ogni rimando delle
      storie precedenti onorato o mantenuto con motivo;
- [ ] **traduzioni**: verificata la resa della pagina pubblica nella lingua richiesta;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato: perimetro del percorso pubblico e casi negativi coperti;
- [ ] `run-tests.sh` esegue il nuovo percorso nell'area `platform`;
- [ ] controllo automatico di **accessibilità** verde sulla pagina pubblica;
- [ ] documentazione dei test aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie da `0016` a `0020` | sono la superficie pubblica che il percorso attraversa |
| storia `0024` | l'accettazione della politica è un passo del percorso |
| storia `0033` | riusa l'impalcatura del percorso interno |

## 7. Fuori ambito

- le prove di carico e di resistenza all'abuso: utili, ma sono un'altra disciplina e un altro strumento;
- i fornitori esterni reali: sempre simulati.

## 8. Punti aperti

Nessuno.
