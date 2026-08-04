# 0020 — Scarti e rinvio del documento

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 04 — Trasmissione e ciclo di vita legale
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo che si è vista scartare una fattura
> voglio correggere quello che non andava e rimandarla, senza dover ricostruire tutto da capo
> così da chiudere il problema in cinque minuti invece che in mezza giornata.

**Contesto.** Lo scarto è il momento in cui il cliente capisce se ha comprato un buon prodotto. La storia `0015`
gli dice **cosa** non va; questa gli permette di **rimediare**. C'è una regola fiscale che governa tutto il resto:
una fattura scartata dal Sistema di Interscambio **non esiste giuridicamente**, quindi si corregge e si rimanda,
di norma conservando numero e data entro i termini; una fattura **accettata** invece esiste, e l'unico rimedio è
una nota di credito. Confondere i due casi è l'errore che costa di più al cliente, ed è compito dell'app non
farglielo fare.

## 2. Requisiti funzionali

1. **RF-1** — Da un documento in stato `scartato` si può generare un **documento correttivo** che eredita dati,
   righe e riferimenti, e resta in stato `bozza`.
2. **RF-2** — Il legame fra il documento scartato e il correttivo è **tracciato in entrambe le direzioni** e
   visibile su entrambe le schede.
3. **RF-3** — L'app propone le correzioni suggerite dalla diagnosi (storia `0015`) e porta l'utente dove
   intervenire — anche fuori dal documento, per esempio sull'anagrafica della controparte.
4. **RF-4** — Un documento **già accettato dall'autorità** non genera un correttivo: l'app lo dice esplicitamente e
   indica che il rimedio è una nota di credito, che nasce nella sorgente del documento, non qui.
5. **RF-5** — Il documento correttivo segue il percorso normale: validazione, conferma esplicita, trasmissione — e
   consuma una nuova unità di quota.
6. **RF-6** — Il documento scartato **resta**, con la sua cronologia: non si cancella e non si sovrascrive.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Documento scartato e correttivo appartengono allo stesso account,
  filtrato per `tenant_id` preso dal token verificato; il legame non può attraversare gli account. Prova di
  isolamento dedicata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/einvoicing/v1/documents/{id}/correct` che
  restituisce il correttivo in bozza; errori in `application/problem+json`, con un problema dedicato per il caso
  «documento già accettato»; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V16__document_correction_link.sql`: colonne di legame sul
  `canonical_document` (documento corretto, documento correttivo) con `tenant_id` e colonne di controllo. Nessuna
  chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sulla scheda del documento scartato: il riquadro di diagnosi in testa e il
  pulsante «Correggi e rimanda»; sulla scheda del correttivo, il rimando al documento d'origine. Solo token del
  sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I testi «questo documento è stato scartato», «questo documento è già stato
  accettato: il rimedio è una nota di credito» e le azioni dallo spazio-nomi `einvoicing`, presenti in
  `en, it, fr, es, de`. Sono i testi che impediscono l'errore costoso: vanno tradotti con cura.
- **RT-6 — Varchi e quota (§6, §7).** La **creazione** del correttivo non consuma quota; la sua **trasmissione**
  sì, come qualunque altra (storia `0017`). Va detto all'utente prima, non dopo: un cliente che pensa che
  correggere sia gratuito si arrabbia al secondo scarto.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `fix_and_resubmit(id, correzioni) → nuova bozza collegata allo scarto`, marcato **scrittura**: produce una
  **bozza** e richiede **conferma umana**; **non** ritrasmette — la trasmissione resta un atto separato con il suo
  varco (storia `0029`). È una distinzione importante: il nome «resubmit» non deve far credere che l'oggetto
  parta. Contratto dentro il servizio; server conversazionale non implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo nuovo che riguardi una persona: il correttivo è un documento come
  gli altri, già coperto dalle voci del manifesto delle storie `0008` e `0011`. **Attenzione**: il correttivo
  **duplica** i dati personali del documento d'origine, quindi la cancellazione deve raggiungerli entrambi —
  verifica esplicita da fare, non da assumere.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `correttivo generato`, `correzione rifiutata perché il
  documento è accettato` sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e
  i due identificativi di documento, senza dati del documento.

## 4. Criteri di accettazione

**CA-1 — Correttivo da uno scarto**
- **Dato** un documento italiano in stato `scartato` con una diagnosi che indica il recapito della controparte
- **Quando** l'utente sceglie «Correggi e rimanda»
- **Allora** nasce un documento correttivo in `bozza` con gli stessi dati, il legame è visibile su entrambe le
  schede, e l'app porta l'utente sull'anagrafica della controparte

**CA-2 — Documento già accettato**
- **Dato** un documento in stato `accettato_dall_autorita`
- **Quando** si tenta di generare un correttivo
- **Allora** l'operazione è rifiutata con un messaggio che spiega che il documento esiste giuridicamente e che il
  rimedio è una nota di credito, da emettere nella sorgente

**CA-3 — Il documento scartato resta**
- **Dato** un correttivo trasmesso con successo
- **Quando** si apre l'elenco dei documenti
- **Allora** il documento scartato è ancora presente con la sua cronologia e il suo legame

**CA-4 — La trasmissione del correttivo consuma quota**
- **Dato** un account con una sola unità di quota residua
- **Quando** trasmette il correttivo
- **Allora** la quota residua diventa zero, e l'utente era stato avvisato prima di confermare

**CA-5 — Isolamento fra account**
- **Dato** due account con documenti scartati
- **Quando** un utente dell'uno tenta di generare un correttivo per un documento dell'altro
- **Allora** riceve `404`

**CA-6 — Rifiuto commerciale, non scarto**
- **Dato** un documento della famiglia a quattro angoli `rifiutato_dal_destinatario`
- **Quando** l'utente apre la scheda
- **Allora** l'app distingue il caso dallo scarto di conformità e propone l'azione giusta per quella famiglia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla generazione del correttivo e sul rifiuto per documento accettato; **integrazione**
      sul percorso scarto → correzione → trasmissione;
- [ ] prova di **isolamento fra account** sul legame fra documenti;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) attraverserà scarto,
      diagnosi, correzione e nuova trasmissione: è il tratto che dimostra il valore dell'app;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con cura sui due testi che impediscono l'errore
      costoso;
- [ ] **manifesto dei dati**: nessuna voce nuova, ma **verificato che la cancellazione raggiunga anche i
      correttivi**;
- [ ] **registro delle decisioni** compilato, con la distinzione scarto/accettato e la sua conseguenza;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `fix_and_resubmit`, marcato scrittura con bozza.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0015` | Le correzioni proposte vengono dalla diagnosi |
| `0019` | Serve lo stato `scartato` e la cronologia da cui si parte |

## 7. Fuori ambito

- L'**emissione della nota di credito**: appartiene alla sorgente del documento (app di fatturazione), non a
  questa app. Qui si dice che serve e dove si fa.
- La correzione automatica senza intervento umano: esclusa. Correggere e ritrasmettere è un atto con effetti
  fiscali e resta dell'utente.
- La rinumerazione automatica del documento correttivo: è una regola fiscale che dipende dai termini e dal
  registro dei documenti, e vive nella sorgente.

## 8. Punti aperti

- **Se il correttivo debba mantenere numero e data dell'originale.** Nel caso dello scarto italiano di norma sì,
  entro i termini, ma la regola dipende dai termini di emissione e non è banale: va confermata con lo sviluppatore
  e, se serve, con un professionista. Nel dubbio, l'app **non decide**: propone e lascia modificare.
- **Quante volte si può correggere lo stesso documento.** Nessun limite tecnico, ma una catena lunga di correttivi
  è il segnale di un problema a monte: vale la pena mostrarla, non impedirla.
