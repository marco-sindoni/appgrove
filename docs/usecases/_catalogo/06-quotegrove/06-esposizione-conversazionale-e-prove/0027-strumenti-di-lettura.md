# 0027 — Contratto degli strumenti di lettura

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 06 — Esposizione conversazionale e prove
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che comanda la sua azienda da una chat
> voglio poter chiedere «quali preventivi sono in attesa da più di una settimana?» e ottenere una risposta vera
> così da sapere dove mettere le mani senza aprire l'applicazione.

**Contesto.** Il catalogo pone a tutte le sessanta app il requisito di essere comandabili da una chat. Il livello
conversazionale della piattaforma **non esiste ancora** (epica `12-ready-for-ai-mcp`, casi d'uso 0061-0066,
scritti e non implementati): il dovere di questa storia non è costruire il server, ma **dichiarare il contratto
degli strumenti di lettura** e tenerlo versionato dentro il servizio dell'app. Gli strumenti di lettura sono
liberi: non cambiano nulla, e il rischio è la sovraesposizione di dati, non l'effetto irreversibile.

## 2. Requisiti funzionali

1. **RF-1** — Sono dichiarati quattro strumenti di lettura, ciascuno con nome stabile, descrizione in lingua
   naturale, schema dei parametri, schema del risultato e marcatura *lettura*:
   `elenca_preventivi(stato?, destinatario?, periodo?, pagina?)`,
   `leggi_preventivo(id)`,
   `elenca_preventivi_in_attesa(giorni_senza_risposta?)`,
   `calcola_prezzo(righe, listino?, sconto?, valuta?)`.
2. **RF-2** — I risultati sono **minimizzati**: `elenca_preventivi` restituisce numero, destinatario, stato,
   totale e validità, non il documento intero; `calcola_prezzo` non salva nulla.
3. **RF-3** — Ogni strumento è **idempotente** e dichiarato tale: chiamarlo due volte non cambia niente.
4. **RF-4** — Gli strumenti applicano la stessa catena di varchi delle rotte: token, app accesa, account
   abilitato, ruolo sufficiente. Un utente che non può vedere un preventivo non lo vede nemmeno dalla chat.
5. **RF-5** — Il contratto è documentato e versionato dentro il servizio: cambiarlo senza cambiare versione fa
   fallire una prova.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Strumenti dichiarati con firma, descrizione, schemi e marcatura
  lettura; il contratto vive dentro il servizio dell'app; il server conversazionale è di piattaforma e non ancora
  implementato — **dipendenza dichiarata: casi d'uso 0061-0063**.
- **RT-2 — Isolamento fra account (§1).** Ogni strumento riceve il contesto dell'account dal livello di
  piattaforma e filtra per `tenant_id`: **mai** un parametro dell'account nello schema dello strumento. Un modello
  linguistico che «inventasse» un identificativo di account non deve poter ottenere niente.
- **RT-3 — Varchi e quota (§6).** Le chiamate degli strumenti passano dagli stessi varchi delle rotte; la lettura
  non consuma la metrica `preventivi_inviati`.
- **RT-4 — Dati personali (§10).** Gli strumenti restituiscono dati di persone (destinatari): la minimizzazione è
  un requisito, non un'ottimizzazione. Va dichiarato nel manifesto che i dati sono esposti anche per questa via.
- **RT-5 — Registrazione eventi (§14).** Ogni invocazione è registrata con `tenant_id`, `app_id`, `user_id`,
  correlazione e nome dello strumento, senza parametri che contengano dati personali.
- **RT-6 — Prove (§11).** Prova di contratto sugli schemi; prova che un utente di un account non ottiene dati di
  un altro nemmeno passando identificativi altrui nei parametri.

## 4. Criteri di accettazione

**CA-1 — Elenco minimizzato**
- **Dato** un account con venti preventivi · **Quando** si invoca `elenca_preventivi(stato: "inviato")`
- **Allora** si ottengono solo i campi dichiarati, paginati, e non il contenuto dei documenti

**CA-2 — I preventivi dimenticati**
- **Dato** tre preventivi inviati da più di sette giorni e senza risposta · **Quando** si invoca
  `elenca_preventivi_in_attesa(giorni_senza_risposta: 7)` · **Allora** si ottengono quei tre e nessun altro

**CA-3 — Calcolo che non scrive**
- **Dato** un insieme di righe · **Quando** si invoca `calcola_prezzo` · **Allora** si ottengono i totali e
  **nessun preventivo è stato creato o modificato**

**CA-4 — Isolamento fra account**
- **Dato** un preventivo dell'account `A` · **Quando** uno strumento invocato nel contesto di `B` ne chiede la
  lettura · **Allora** ottiene la risposta che otterrebbe per un documento inesistente

**CA-5 — Contratto stabile**
- **Dato** una modifica allo schema di uno strumento senza cambio di versione · **Quando** si eseguono le prove
- **Allora** la prova di contratto fallisce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sugli schemi e di **integrazione** sulle invocazioni;
- [ ] prova di **isolamento fra account** su ogni strumento;
- [ ] **prova end-to-end**: nessun impatto sulla superficie utente (il livello conversazionale non esiste
      ancora) — risposta scritta nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: non applicabile; le descrizioni degli strumenti sono in lingua naturale per il modello, non
      testo di interfaccia;
- [ ] **manifesto dei dati** aggiornato con la nota sull'esposizione conversazionale;
- [ ] **registro delle decisioni** compilato (elenco degli strumenti, campi restituiti e criterio di
      minimizzazione);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0013` | `calcola_prezzo` usa il motore di calcolo, non una copia |
| storia `0024` | `elenca_preventivi_in_attesa` si appoggia agli stati e agli esiti |
| casi d'uso di piattaforma 0061-0063 (non implementati) | il server conversazionale, l'autenticazione delegata e la mappatura operazioni → strumenti; nel frattempo il contratto resta dichiarato e provato dentro il servizio |

## 7. Fuori ambito

- gli strumenti che scrivono: storia `0028`;
- la costruzione del server conversazionale: è di piattaforma.

## 8. Punti aperti

Nessuno.
