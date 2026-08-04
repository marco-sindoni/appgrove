# 0027 — Strumenti di lettura

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0015`, `0019`, `0024`, `0025`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta guidando e chiede al telefono «è andata la fattura di ieri al cliente belga?»
> voglio una risposta esatta in una frase
> così da non dover aprire il computer per una domanda che vale dieci secondi.

**Contesto.** Le storie precedenti hanno dichiarato strumenti sparsi; questa li **raccoglie in un contratto
unico, versionato con il servizio**, e ne fissa la forma. È il requisito trasversale del catalogo: ogni funzione
dev'essere comandabile da una chat. Va detto chiaramente che il livello conversazionale **non esiste ancora** nel
repository — è l'epica `12-ready-for-ai-mcp`, use case 0061-0066, scritti e non implementati: qui si costruisce il
lato dell'app, non il server.

Si comincia dalla lettura perché è la parte senza rischio e con più valore: le due domande del cliente sono «è
andata?» e «se no, perché?», e si rispondono entrambe leggendo.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un descrittore, dentro il servizio e versionato con esso, che dichiara per ogni strumento:
   nome stabile, descrizione in lingua naturale, schema dei parametri, schema del risultato, marcatura
   **lettura**, e idempotenza.
2. **RF-2** — Gli strumenti di lettura dichiarati sono: `list_documents`, `get_document_status`,
   `explain_rejection`, `validate_before_send`, `list_overdue`, `get_vat_report`, `list_jurisdictions`,
   `list_upcoming_mandates`, `verify_counterparty_routing`, `search_archive`, `get_archive_receipt`,
   `get_retention_status`, `preview_official_format`.
3. **RF-3** — Ogni risultato è **minimizzato**: si restituisce ciò che serve alla domanda, non l'entità intera.
   `list_documents` restituisce numero, data, totale, stato e giurisdizione — non le righe, non gli indirizzi.
4. **RF-4** — Nessuno strumento di lettura produce effetti: sono tutti sicuri da ripetere.
5. **RF-5** — Il descrittore è **verificato da una prova**: se una firma cambia senza che il descrittore cambi, la
   suite è rossa.
6. **RF-6** — Ogni strumento dichiara cosa fa quando i dati non ci sono, e non restituisce mai un risultato vuoto
   senza spiegazione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni strumento riceve il `tenant_id` dal **contesto della chiamata
  autenticata**, mai da un parametro: un parametro `tenant_id` in uno strumento sarebbe la via più diretta per far
  leggere a un agente i dati di un altro cliente. Prova di isolamento su **ogni** strumento dichiarato.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte esistenti; nessuna logica
  duplicata. Errori in `application/problem+json` con tipi di problema stabili, perché un agente deve poterli
  riferire all'utente in modo comprensibile.
- **RT-3 — Persistenza (§8).** Nessuna migrazione: gli strumenti leggono, non scrivono.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Nella panoramica, il riquadro «Dalla chat» spiega
  con due esempi cosa si può chiedere: è l'unico modo perché qualcuno lo scopra.
- **RT-5 — Cinque lingue (§4).** Le **descrizioni in lingua naturale** degli strumenti servono al modello, non
  all'utente, e restano in una sola lingua di riferimento: la scelta va scritta. I testi del riquadro «Dalla chat»
  invece sono visibili e passano dallo spazio-nomi `einvoicing`, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Gli strumenti di lettura **non** consumano la metrica `documenti`.
  L'applicazione dei varchi alle chiamate dell'assistente è però di piattaforma (UC 0064, non implementato):
  qui si dichiara che ogni strumento attraversa la stessa catena della rotta corrispondente, e la dipendenza è
  scritta.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che realizza il punto: contratto dentro il servizio,
  tutti gli strumenti marcati **lettura**, nessuna conferma umana richiesta perché nessuno produce effetti.
  Dipendenza dichiarata: UC 0061-0063 (architettura del server, autenticazione delegata, mappatura operazioni →
  strumenti), non implementati.
- **RT-8 — Dati personali (§10).** ⚠️ **La minimizzazione dei risultati è una misura di protezione dei dati, non
  una scelta di prestazioni.** Uno strumento che restituisce l'entità intera espone a un modello, e alla sua
  memoria, dati personali che alla domanda non servivano. Va scritto nel manifesto: gli strumenti non introducono
  campi nuovi, ma introducono un **nuovo modo di leggerli**, e nessuno strumento restituisce indirizzi o codici
  fiscali se non è esattamente ciò che è stato chiesto.
- **RT-9 — Registrazione eventi (§14).** Ogni invocazione è registrata con `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione, nome dello strumento e esito — **senza** parametri e senza risultato, perché
  entrambi possono contenere dati personali.

## 4. Criteri di accettazione

**CA-1 — Descrittore completo**
- **Dato** il servizio avviato
- **Quando** si chiede il descrittore degli strumenti
- **Allora** contiene tutti gli strumenti di lettura dichiarati, ciascuno con nome, descrizione, schema dei
  parametri, schema del risultato e marcatura `lettura`

**CA-2 — Risultato minimizzato**
- **Dato** un account con documenti
- **Quando** si invoca `list_documents`
- **Allora** il risultato contiene numero, data, totale, stato e giurisdizione, e **non** contiene righe,
  indirizzi né codici fiscali

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** si invoca uno strumento nel contesto di `A` passando un parametro che nomina `B`
- **Allora** il risultato contiene solo dati di `A`: il parametro è ignorato

**CA-4 — Ripetibilità**
- **Dato** uno strumento di lettura invocato due volte con gli stessi parametri
- **Quando** si confrontano i risultati
- **Allora** sono uguali e nessuno stato è cambiato

**CA-5 — Assenza di dati spiegata**
- **Dato** un account senza documenti nel periodo richiesto
- **Quando** si invoca `get_vat_report`
- **Allora** il risultato dice che non ci sono documenti in quel periodo, non un insieme vuoto senza spiegazione

**CA-6 — Il descrittore non diverge**
- **Dato** una firma di strumento modificata senza aggiornare il descrittore
- **Quando** si esegue la suite
- **Allora** la suite è rossa

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla minimizzazione di ogni risultato e sulla coerenza fra firme e descrittore;
      **integrazione** su ogni strumento;
- [ ] prova di **isolamento fra account** su **ogni** strumento dichiarato, compreso il caso del parametro che
      nomina un altro account;
- [ ] **prova end-to-end**: *rimando* — il livello conversazionale non esiste (UC 0061-0063); la voce del registro
      di copertura è `da-coprire` con motivo «server conversazionale non implementato» e storia proprietaria
      UC 0061;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per il riquadro «Dalla chat»;
- [ ] **manifesto dei dati**: nessun campo nuovo, ma la **minimizzazione dichiarata** come misura;
- [ ] **registro delle decisioni** compilato, con la lingua delle descrizioni degli strumenti e la scelta di
      minimizzare;
- [ ] contratto degli **strumenti conversazionali** dichiarato per tutti gli strumenti di lettura.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0011`, `0015`, `0019`, `0024`, `0025` | Sono le storie che hanno dichiarato gli strumenti che qui si raccolgono |
| UC 0061-0063 (livello conversazionale), non implementati | Il server, l'autenticazione delegata e la mappatura sono di piattaforma. Nel frattempo il contratto vive dentro il servizio ed è verificabile con prove proprie |
| UC 0064 (varchi per le chiamate dell'assistente), non implementato | L'applicazione di abilitazione e quota alle chiamate è di piattaforma |

## 7. Fuori ambito

- Gli strumenti di **scrittura**: storia `0028`.
- Il varco di conferma umana per la trasmissione: storia `0029`.
- La costruzione del server conversazionale: è di piattaforma, non di questa app.

## 8. Punti aperti

- **In quale lingua si scrivono le descrizioni degli strumenti.** Sono testo per un modello, non per una persona,
  e non seguono la regola delle cinque lingue dell'interfaccia. La proposta è una sola lingua di riferimento, ma
  va confermato: è una scelta che riguarda tutta la piattaforma, non solo questa app, e andrebbe presa in
  UC 0063.
- **Se l'invocazione di uno strumento debba essere visibile all'utente nell'app** («l'assistente ha letto il tuo
  archivio alle 10:14»). Utile per la fiducia, ma è una funzione di piattaforma: annotata, non anticipata.
