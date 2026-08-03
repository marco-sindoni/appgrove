# 0025 — Acconto richiesto

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 05 — Promemoria, acconti e mancate presentazioni
**Storia**: `0025` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha servizi lunghi e costosi
> voglio poter chiedere una somma alla prenotazione e tenere traccia di chi l'ha versata
> così da avere uno strumento in più contro le mancate presentazioni sui trattamenti che mi tengono occupata
> l'agenda per due ore.

**Contesto.** L'acconto è la leva anti-disdetta più forte, ed è quella che i portali usano come argomento di
vendita. Ma qui c'è un confine di perimetro dichiarato: **appgrove non incassa denaro dei clienti dei nostri
clienti** (§1 della descrizione). Muovere denaro fra l'attività e il suo cliente farebbe di noi un intermediario
di pagamento, con conseguenze che nessuno ha valutato e che non appartengono a questa storia. La proposta è
quindi che BookGrove **registri** l'acconto senza incassarlo: dice quanto è dovuto, di che natura è, e se è
arrivato — l'incasso avviene fuori, come l'attività preferisce.

C'è poi una distinzione giuridica che non si può ignorare (§2.3, punto 1 della descrizione): la **caparra
confirmatoria** e la **caparra penitenziale** hanno conseguenze diverse in caso di disdetta, e quale delle due
sia dipende da come è stata pattuita. L'app deve registrarlo, non deciderlo.

## 2. Requisiti funzionali

1. **RF-1** — Per ciascun servizio si può richiedere un acconto, in cifra fissa o in percentuale sul prezzo
   indicativo.
2. **RF-2** — Si dichiara la **natura** della somma — caparra confirmatoria, caparra penitenziale, semplice
   anticipo sul prezzo — e la scelta compare nel testo mostrato al cliente prima della conferma.
3. **RF-3** — La prenotazione porta lo stato dell'acconto: atteso, ricevuto, non ricevuto, restituito; lo stato lo
   aggiorna l'attività, a mano.
4. **RF-4** — Il cliente vede sulla pagina pubblica che è richiesto un acconto, quanto, di che natura e come
   versarlo, con le istruzioni che l'attività ha scritto.
5. **RF-5** — Un acconto atteso e non ricevuto entro un tempo configurabile fa comparire la prenotazione fra
   quelle da sollecitare; non la disdice da solo.
6. **RF-6** — L'applicazione dichiara chiaramente, a schermo, che **non incassa e non gestisce denaro**: non deve
   esserci ambiguità né per l'attività né per il cliente finale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dell'`acconto` filtra per `tenant_id` preso
  dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `PUT /api/prenotazioni/v1/prenotazioni/{id}/acconto` e la
  configurazione per servizio; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V17__acconti.sql`: tabella `acconto` con `tenant_id`, UUID versione 7,
  colonne di controllo, importo, valuta, natura, stato e nota su come è stato versato.
- **RT-4 — Nessun movimento di denaro.** Nessuna integrazione con un fornitore di pagamento, nessun dato di carta,
  nessun numero di conto conservato: se in futuro si vorrà incassare davvero, sarà una decisione separata e con
  una valutazione a parte (§11, punto 6, della descrizione).
- **RT-5 — Modulo frontend (§3, §5).** Stato dell'acconto visibile dal blocco in agenda e dalla prenotazione;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Interfaccia e testi mostrati al cliente in `en, it, fr, es, de`; le istruzioni
  scritte dall'attività restano nella sua lingua.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo: l'acconto riguarda una somma, non una persona, e
  **non si conservano dati di pagamento**. Va detto esplicitamente nel manifesto, perché l'assenza è essa stessa
  un'informazione utile a chi legge.
- **RT-8 — Registrazione eventi (§14).** `acconto richiesto`, `acconto segnato come ricevuto` con `tenant_id`,
  `app_id`, `user_id`, importo e correlazione — mai il cliente.

## 4. Criteri di accettazione

**CA-1 — Acconto richiesto e mostrato**
- **Dato** un servizio con acconto del 30 % · **Quando** un visitatore prenota · **Allora** vede l'importo, la
  natura della somma e le istruzioni prima di confermare

**CA-2 — Registrazione manuale**
- **Dato** un acconto atteso · **Quando** l'attività lo segna come ricevuto · **Allora** lo stato cambia e resta la
  traccia di chi e quando

**CA-3 — Sollecito, non disdetta**
- **Dato** un acconto non ricevuto oltre il termine · **Quando** passa il tempo · **Allora** la prenotazione
  compare fra quelle da sollecitare e **non** viene disdetta automaticamente

**CA-4 — Nessun dato di pagamento**
- **Dato** l'intero flusso · **Quando** si esaminano tabelle, registri ed esportazioni · **Allora** non esiste
  nessun dato di carta né di conto

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** uno prova ad aggiornare l'acconto di una prenotazione dell'altro · **Allora**
  la richiesta è rifiutata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sul calcolo dell'importo e di **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account** sull'acconto;
- [ ] **prova end-to-end**: *rimando* — l'acconto non ha un passo automatico proprio; motivo e storia
      proprietaria dichiarati in [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la dichiarazione esplicita che non si trattano dati di pagamento;
- [ ] **registro delle decisioni** compilato: **appgrove non incassa**, e la registrazione della natura giuridica
      della somma;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0024` | acconto e politica di disdetta si mostrano insieme e si applicano insieme |
| **decisione dello sviluppatore** sul perimetro dell'incasso | è un effetto verso l'esterno e una scelta di prodotto |

## 7. Fuori ambito

- l'incasso vero, la restituzione automatica e la ricevuta: fuori dal perimetro dichiarato dell'app;
- l'emissione di un documento fiscale sull'acconto: è dell'applicazione 02.

## 8. Punti aperti

**L'acconto non incassato riduce davvero le mancate presentazioni?** La leva funziona perché il cliente ha già
pagato; un acconto solo dichiarato ha un effetto molto minore. È possibile che il mercato consideri questa
versione insufficiente. È il punto 6 dei rischi della descrizione, e la decisione — restare fuori dai pagamenti,
oppure aprire un percorso con una valutazione dedicata — spetta allo sviluppatore.
