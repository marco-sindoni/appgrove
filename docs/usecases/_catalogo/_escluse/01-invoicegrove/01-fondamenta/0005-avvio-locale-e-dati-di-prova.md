# 0005 — Avvio locale e dati di prova

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende il lavoro su questa app fra sei mesi
> voglio avviare tutto in locale con un comando e trovarci dentro dati sensati e fornitori simulati
> così da poter vedere l'app funzionare senza chiedere niente a nessuno e senza toccare servizi reali.

**Contesto.** Questa app parla con tre fornitori esterni che hanno effetti giuridici: se in locale non sono
simulati, prima o poi qualcuno trasmette una fattura di prova a un'autorità fiscale vera. Non è un rischio
teorico. La storia chiude le fondamenta stabilendo che in locale **i fornitori sono sempre simulati** e che i dati
di prova sono **inventati** — mai dati veri di un cliente, indirizzi `*.test`.

## 2. Requisiti funzionali

1. **RF-1** — `./app-start.sh` avvia il servizio `einvoicing` senza alcuna modifica manuale agli script, e
   `./dev.sh services` lo mostra con porta e schema.
2. **RF-2** — `dev migrate` applica le migrazioni dell'app sullo schema `app_einvoicing`.
3. **RF-3** — Esiste un insieme di dati di prova: due soggetti emittenti (uno italiano, uno belga), sei
   controparti in quattro paesi, una dozzina di documenti in stati diversi del ciclo di vita, tutti inventati.
4. **RF-4** — I tre fornitori esterni (trasmissione al Sistema di Interscambio, punto di accesso alla rete a
   quattro angoli, conservatore) hanno una **realizzazione simulata** che si attiva in locale e nelle prove, e che
   **non può** essere sostituita da quella reale senza una configurazione esplicita.
5. **RF-5** — La realizzazione simulata sa rispondere anche male: sa produrre uno scarto, una mancata consegna e
   un ritardo, perché sono i casi che l'app deve saper gestire.
6. **RF-6** — Il profilo di spedizione dell'artefatto si avvia davvero (prova di avvio reale, area `smoke`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I dati di prova appartengono ad almeno **due account distinti**, così
  che le prove di isolamento abbiano su cosa lavorare fin da subito.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova.
- **RT-3 — Persistenza (§8).** Nessuna migrazione nuova: i dati di prova si caricano, non si migrano.
- **RT-4 — Modulo frontend (§3, §5).** Con i dati di prova caricati, le sezioni del modulo mostrano qualcosa e non
  lo stato vuoto.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile nuovo.
- **RT-6 — Varchi e quota (§6, §7).** Uno dei due account di prova è configurato **vicino al tetto** della quota,
  così che lo stato di blocco sia raggiungibile senza costruirlo a mano ogni volta.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento.
- **RT-8 — Dati personali (§10).** I dati di prova sono **inventati**: nomi di fantasia, identificativi fiscali
  formalmente validi ma non attribuiti, indirizzi di posta elettronica su domini `*.test`. Nessuna voce nuova nel
  manifesto.
- **RT-9 — Registrazione eventi (§14).** In sviluppo il registro è in testo leggibile; in produzione in JSON. In
  nessuno dei due compaiono denominazioni o indirizzi.
- **RT-10 — Avvio locale (§15).** La mappa servizio → identificativo → porta → schema viene **solo** da
  `application.properties`. Se viene voglia di modificare a mano uno script di avvio, è un difetto della scoperta
  automatica, non un passo del lavoro.

## 4. Criteri di accettazione

**CA-1 — Avvio senza passi manuali**
- **Dato** il repository appena clonato dopo l'unione del ramo
- **Quando** si esegue `./app-start.sh` e poi `./dev.sh services`
- **Allora** `einvoicing` risulta avviato sulla sua porta, con il suo schema, e nessuno script è stato modificato
  a mano

**CA-2 — Dati di prova utilizzabili**
- **Dato** lo stack locale avviato e i dati di prova caricati
- **Quando** si entra nel modulo con l'utente di prova del primo account
- **Allora** si vedono i documenti di quell'account e nessuno di quelli del secondo

**CA-3 — I fornitori sono simulati**
- **Dato** la configurazione locale predefinita
- **Quando** si chiede al servizio quale realizzazione dei fornitori sta usando
- **Allora** risponde «simulata» per tutti e tre, e un tentativo di trasmissione non esce dalla macchina

**CA-4 — La simulazione sa fallire**
- **Dato** un documento di prova marcato per produrre uno scarto
- **Quando** lo si trasmette al fornitore simulato
- **Allora** torna uno scarto con un codice riconoscibile, e il documento passa nello stato corrispondente

**CA-5 — Quota raggiungibile**
- **Dato** l'account di prova configurato vicino al tetto
- **Quando** si consumano i documenti rimanenti
- **Allora** si arriva al `429` senza dover manipolare il database a mano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, smoke);
- [ ] prova di **avvio reale** dell'artefatto nel profilo di spedizione (area `smoke`);
- [ ] prova di **isolamento fra account** con i due account dei dati di prova;
- [ ] **prova end-to-end**: *rimando* — la storia `0030` è la proprietaria del percorso `[J-EINVOICING]`, che si
      appoggerà proprio a questi dati di prova;
- [ ] **traduzioni**: nessun testo nuovo;
- [ ] **manifesto dei dati**: nessuna modifica; verificato che i dati di prova non contengano dati veri;
- [ ] **registro delle decisioni** compilato, con la scelta «in locale i fornitori sono sempre simulati» e il
      motivo (effetti giuridici irreversibili);
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001`, `0002` | Servono servizio e schema |
| `0003` | Serve il modulo per vedere i dati |
| `0004` | Serve la quota per poter configurare un account vicino al tetto |

## 7. Fuori ambito

- Le realizzazioni **reali** dei fornitori: arrivano con le storie `0017`, `0018` e `0023`, ognuna con il proprio
  contratto e le proprie prove.
- Il percorso end-to-end: storia `0030`.

## 8. Punti aperti

- **Se e come si potrà mai provare contro un ambiente di prova reale dell'autorità fiscale.** Il Sistema di
  Interscambio ha un ambiente di prova, ma usarlo dall'integrazione continua richiede credenziali e un canale
  accreditato: è una decisione che tocca contratti, non codice. Per ora si resta sulla simulazione e si dichiara
  il limite.
