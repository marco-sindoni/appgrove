# 0026 — Uscita e restituzione dell'archivio

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 05 — Conservazione a norma e adempimenti
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha deciso di cambiare fornitore
> voglio portarmi via i miei documenti conservati e sapere con chiarezza cosa succede all'obbligo dei dieci anni
> così da non trovarmi, fra sei anni, senza le mie fatture e senza sapere chi le ha.

**Contesto.** È la storia che nessuno scrive e che poi diventa un problema. La quota di InvoiceGrove è un
**consumo mensile**; la conservazione è una **giacenza decennale**. Un cliente che sta un anno e trasmette 1.200
documenti ci lascia un obbligo di custodia lungo dieci anni e undici mesi dopo che ha smesso di pagare
(descrizione dell'applicazione §11). Non è un dettaglio operativo: è il punto in cui il modello economico dell'app
e la legge divergono, e va affrontato per iscritto prima che il primo cliente disdica, non dopo.

Qui si affronta anche il conflitto fra **cancellazione** e **conservazione**: la cancellazione sulla piattaforma è
fisica, ma un documento in conservazione a norma non si cancella per dieci anni, perché c'è un obbligo di legge
che prevale.

## 2. Requisiti funzionali

1. **RF-1** — In qualunque momento, e **anche con abbonamento scaduto**, il cliente può richiedere la
   **restituzione integrale** del proprio archivio: tutti i documenti conservati con le loro ricevute e i loro
   metadati.
2. **RF-2** — Alla disdetta, l'app **dichiara in modo esplicito** cosa succede all'archivio: per quanto tempo
   resta accessibile, chi lo custodisce, e cosa deve fare il cliente per essere in regola.
3. **RF-3** — Una richiesta di **cancellazione** dell'interessato viene eseguita su tutto ciò che non è coperto
   dall'obbligo di conservazione, e produce una **risposta scritta** che dichiara cosa resta, perché, e fino a
   quando.
4. **RF-4** — Ogni cancellazione parziale lascia una **riga di prova** nel registro delle purghe, con il perimetro
   effettivamente cancellato e quello escluso con la motivazione.
5. **RF-5** — L'app mostra in ogni momento **quanti documenti** sono in conservazione e **fino a quando** dura
   l'obbligo del più recente: è l'informazione che il cliente non ha mai e che decide le sue scelte.
6. **RF-6** — Nessuna funzione di questa storia esegue una cancellazione **irreversibile** senza una conferma
   umana esplicita e una motivazione scritta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Restituzione e cancellazione operano **solo** sul `tenant_id` preso dal
  token verificato o dal contesto della richiesta dell'interessato. È la funzione più pericolosa dell'app: una
  perdita di isolamento qui **cancella i dati di un altro cliente**. Prova di isolamento obbligatoria, rafforzata
  e mai esclusa.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/einvoicing/v1/archive/handover` (restituzione
  integrale) e l'aggancio al contratto `EinvoicingDataContract` per `purgeData`; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V21__retention_and_handover.sql`: colonne di scadenza dell'obbligo su
  `archive_record`, tabella delle restituzioni con perimetro ed esito. `tenant_id`, chiave UUID versione 7,
  colonne di controllo. Nessuna cancellazione logica su queste tabelle: sono prova.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione «Archivio», il riquadro «Il tuo obbligo di conservazione» con
  numero di documenti e data di scadenza più lontana, e l'azione «Porta via tutto». Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** ⚠️ I testi di questa storia hanno effetti legali sulla percezione del cliente
  («cosa resta e fino a quando»): dallo spazio-nomi `einvoicing`, presenti in `en, it, fr, es, de`, e da rivedere
  con particolare attenzione.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. La restituzione **resta accessibile con
  abbonamento `canceled`**, in deroga al `402`: i diritti dell'interessato e la disponibilità dei propri documenti
  restano accessibili anche quando l'app è disabilitata
  ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §13).
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `get_retention_status() → quanti documenti in conservazione e fino a quando`, marcato **lettura**, nessuna
  conferma. ⚠️ **La restituzione integrale e la cancellazione NON sono esposte** al livello conversazionale: sono
  effetti irreversibili di massa e restano azioni umane con conferma esplicita e motivazione scritta. La scelta va
  scritta, non lasciata implicita. Contratto dentro il servizio; server conversazionale non implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** È la storia che **realizza** `purgeData` per l'app. Ogni tabella con dati
  personali dell'elenco della descrizione dell'applicazione §6 va raggiunta; `archive_record` va raggiunto con
  **esito parziale dichiarato**. Ricordare che **sostituire i nomi con dei codici non è cancellare**: la
  cancellazione è fisica su ciò che si può cancellare, e dichiarata su ciò che non si può.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `restituzione richiesta`, `restituzione completata`,
  `cancellazione parziale eseguita`, `perimetro escluso per obbligo di legge` sono registrati con `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione, conteggi e motivazione — senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Restituzione integrale**
- **Dato** un account con duecento documenti conservati
- **Quando** richiede la restituzione integrale
- **Allora** ottiene un pacchetto con tutti i documenti, le ricevute e i metadati, e l'elenco di ciò che contiene

**CA-2 — Restituzione con abbonamento scaduto**
- **Dato** un account con abbonamento `canceled`
- **Quando** richiede la restituzione
- **Allora** l'operazione riesce, mentre le rotte di trasmissione rispondono `402`

**CA-3 — Cancellazione parziale con risposta scritta**
- **Dato** una richiesta di cancellazione dell'interessato su un account con documenti in conservazione
- **Quando** la cancellazione viene eseguita
- **Allora** tutto ciò che non è coperto dall'obbligo è **fisicamente** cancellato, e la risposta dichiara cosa
  resta, perché e fino a quando

**CA-4 — Riga di prova**
- **Dato** la cancellazione parziale precedente
- **Quando** si guarda il registro delle purghe
- **Allora** c'è una riga con il perimetro cancellato, quello escluso e la motivazione

**CA-5 — Obbligo visibile**
- **Dato** un account con documenti conservati
- **Quando** apre la sezione «Archivio»
- **Allora** legge quanti documenti sono in conservazione e fino a quando dura l'obbligo del più recente

**CA-6 — Isolamento fra account**
- **Dato** due account con archivi propri
- **Quando** si esegue una cancellazione per uno dei due
- **Allora** l'archivio dell'altro è **intatto**, verificato documento per documento

**CA-7 — Nessuna cancellazione senza conferma**
- **Dato** una richiesta di cancellazione senza conferma esplicita e senza motivazione
- **Quando** la si invia
- **Allora** viene rifiutata e nulla viene cancellato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sul calcolo del perimetro cancellabile e sulla scadenza dell'obbligo; **integrazione**
      su restituzione e cancellazione con database effimero;
- [ ] prova di **isolamento fra account** rafforzata: la cancellazione di un account non tocca l'altro, verificato
      su ogni tabella;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) includerà la richiesta di
      restituzione, che è l'unico modo di verificare che funzioni davvero;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, riviste con attenzione perché hanno effetti legali;
- [ ] **manifesto dei dati** completo e coerente: **ogni** tabella con dati personali raggiunta da `exportData` e
      `purgeData`, con l'esito parziale dichiarato su `archive_record`;
- [ ] **registro delle decisioni** compilato, con il conflitto cancellazione/conservazione e la soluzione adottata;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `get_retention_status`, e **scritto** che
      restituzione e cancellazione non sono esposte.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0023` | Servono i documenti effettivamente conservati e le loro ricevute |
| `0024` | La restituzione riusa la composizione del pacchetto di scarico |
| Presidio di conformità della piattaforma | La risposta all'interessato in caso di cancellazione parziale eccede questa app |

## 7. Fuori ambito

- La **cancellazione presso il conservatore** alla scadenza dei dieci anni: è un'operazione del fornitore,
  governata dal contratto. Qui si tiene la data e si dichiara.
- La chiusura dell'account e la sua purga complessiva: è di piattaforma; questa storia fornisce il contributo
  dell'app.
- La migrazione dell'archivio verso un **altro conservatore**: clausola contrattuale, non funzione (punto aperto
  della storia `0023`).

## 8. Punti aperti

- 🛑 **Chi paga i dieci anni di custodia di un cliente che ha smesso di pagare dopo un anno.** Le vie sono tre —
  incorporarlo nel prezzo (che alza il listino per tutti), farlo pagare all'uscita (che è un pagamento una tantum,
  **vietato** dalla piattaforma: solo abbonamento ricorrente), o restituire l'archivio e trasferire l'obbligo al
  cliente. La terza è l'unica compatibile con i vincoli, ma **è una decisione di prodotto e di rischio, non
  tecnica**: fermata di escalation dello sviluppatore.
- 🛑 **La risposta all'interessato in caso di cancellazione parziale** — cosa resta, con quale formula, con quale
  base giuridica — è materia di conformità che eccede questa app. Va portata al presidio trasversale della
  piattaforma (descrizione dell'applicazione §11, punto 3).
- **Se la restituzione debba essere automatica alla disdetta** o su richiesta. Automatica è più tutelante per il
  cliente ma produce un'estrazione massiva di dati personali non richiesta: nel dubbio, su richiesta, con un
  avviso ben visibile.
