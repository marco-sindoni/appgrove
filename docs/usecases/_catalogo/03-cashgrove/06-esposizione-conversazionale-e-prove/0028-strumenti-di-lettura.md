# 0028 — Strumenti di lettura

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`, `0024`, `0025`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che comanda l'azienda dalla chat
> voglio poter chiedere «chi mi deve soldi da più di 60 giorni?» e ottenere una risposta vera
> così da fare, in cinque secondi, la domanda che oggi non faccio perché aprire il programma costa dieci minuti.

**Contesto.** È il requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Nel repository
il livello conversazionale **non esiste ancora** — è l'epica di piattaforma `12-ready-for-ai-mcp` (UC 0061-0066),
scritta e non implementata. Il compito di questa storia non è costruire il server, che è di piattaforma: è **dichiarare
il contratto** degli strumenti di sola lettura e tenerlo dentro il servizio, versionato con esso. Gli strumenti di
lettura sono anche la parte in cui questa app guadagna di più dal livello conversazionale: le domande sul portafoglio
crediti sono esattamente quelle che nessuno fa perché costano tempo.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara sei strumenti di sola lettura: `elenca_crediti_scaduti`, `riepilogo_anzianita`,
   `indicatore_tempo_medio_incasso`, `previsione_incassi`, `punteggio_rischio_debitore`, `storico_solleciti`.
2. **RF-2** — Ogni strumento dichiara: nome stabile, descrizione in lingua naturale, schema dei parametri, schema del
   risultato, marcatura **lettura** e idempotenza.
3. **RF-3** — I risultati sono **minimizzati**: contengono ciò che serve a rispondere e nulla di più — niente recapiti,
   niente corpi di messaggio integrali, niente identificativi fiscali se non richiesti espressamente.
4. **RF-4** — Ogni risultato che contiene un numero derivato porta con sé **come è stato ottenuto** (le ipotesi della
   previsione, i componenti del punteggio, il criterio di ponderazione dell'indicatore).
5. **RF-5** — Il contratto è verificato da una prova automatica: uno strumento dichiarato senza schema, o con uno schema
   che non corrisponde al risultato reale, fa fallire la compilazione.
6. **RF-6** — Il contratto dichiara esplicitamente anche gli strumenti che **non** esistono e perché — importazione da
   file, generazione del collegamento pubblico, esportazione, stralcio, configurazione delle sequenze — così che
   l'assenza sia una scelta leggibile e non una dimenticanza.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni strumento esegue con il `tenant_id` del token verificato della sessione
  che lo invoca; **nessuno strumento accetta un parametro di account**, nemmeno facoltativo. È il punto in cui un
  errore sarebbe più grave che altrove, perché il chiamante è un programma che può provare tutte le combinazioni.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti riusano le rotte già esistenti e la loro logica: non si
  duplica il codice di lettura, si espone un contratto sopra di esso. Errori in `application/problem+json`.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova; nella *Panoramica* si aggiunge un riquadro che elenca in
  parole comuni cosa si può chiedere in chat, così che l'utente lo scopra.
- **RT-5 — Cinque lingue (§4).** Le stringhe del riquadro passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`. Le **descrizioni degli strumenti** nel contratto restano in una lingua sola e stabile: sono
  destinate a un programma, non a un utente.
- **RT-6 — Varchi e quota (§6, §7).** Una chiamata di strumento attraversa gli **stessi** varchi di una chiamata
  dall'interfaccia: `401`, `403` app spenta, `402` senza abilitazione, `403` ruolo insufficiente, `429` a quota
  esaurita. Nessuna scorciatoia. Dipendenza dichiarata: UC 0064 (applicazione di abilitazione e quota alle chiamate
  dell'assistente), non implementato.
- **RT-7 — Esposizione conversazionale (§12).** È l'oggetto stesso della storia. Il contratto vive dentro il servizio
  `crediti` ed è versionato con esso; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** La minimizzazione di RF-3 non è una scelta estetica: uno strumento che restituisce
  l'elenco completo dei recapiti a ogni domanda moltiplica l'esposizione. Il manifesto registra che gli strumenti di
  lettura restituiscono un sottoinsieme dichiarato dei campi già mappati, e nessun campo nuovo.
- **RT-9 — Registrazione eventi (§14).** Ogni invocazione è registrata con `tenant_id`, `app_id`, `user_id`, nome dello
  strumento e identificativo di correlazione, **senza i parametri e senza il risultato** — che contengono dati
  personali. Dipendenza dichiarata: UC 0065 (sicurezza e tracciamento del livello conversazionale).

## 4. Criteri di accettazione

**CA-1 — Il contratto è completo**
- **Dato** il servizio compilato
- **Quando** si legge il contratto degli strumenti
- **Allora** i sei strumenti sono presenti, ciascuno con nome, descrizione, schema dei parametri, schema del risultato e
  marcatura «lettura»

**CA-2 — Nessun parametro di account**
- **Dato** un qualsiasi strumento · **Quando** si esamina il suo schema dei parametri · **Allora** non esiste alcun
  campo che indichi un account, e una prova automatica lo verifica per tutti gli strumenti

**CA-3 — Risultato minimizzato**
- **Dato** `elenca_crediti_scaduti` invocato senza parametri
- **Quando** si osserva il risultato
- **Allora** contiene denominazione del debitore, numero del documento, residuo e giorni di ritardo, e **non** contiene
  recapiti né identificativo fiscale

**CA-4 — Le ipotesi viaggiano col numero**
- **Dato** `previsione_incassi` invocato · **Quando** si osserva il risultato · **Allora** contiene gli importi e le
  ipotesi usate; un risultato senza ipotesi fa fallire la prova

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** uno strumento è invocato in una sessione di `A` · **Allora** restituisce
  solo dati di `A`, e nessun parametro può cambiarlo

**CA-6 — Quota e abilitazione**
- **Dato** un account con abbonamento `canceled` · **Quando** uno strumento viene invocato · **Allora** la risposta è
  `402`, come per una chiamata dall'interfaccia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sulla corrispondenza fra schema dichiarato e risultato reale, di **integrazione** su ciascuno
      dei sei strumenti;
- [ ] prova di **isolamento fra account** su tutti gli strumenti, con tentativo di iniettare un account nei parametri;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che percorre anche una invocazione di strumento;
- [ ] **traduzioni** del riquadro presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la nota sulla minimizzazione;
- [ ] **registro delle decisioni** compilato, in particolare sugli strumenti **non** dichiarati e sul perché;
- [ ] contratto degli **strumenti conversazionali** completo per la parte di lettura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0023`, `0024`, `0025`, `0026` | Sono le funzioni che gli strumenti espongono; senza, non c'è nulla da leggere |
| UC 0061-0063 (livello conversazionale di piattaforma) | Non implementati: il contratto si scrive e si verifica, ma nessun assistente lo può ancora invocare. Nel frattempo gli strumenti sono esercitati dalle prove |
| UC 0064-0065 (abilitazione, quota, tracciamento delle chiamate) | Non implementati: la storia dichiara come devono comportarsi, la piattaforma li applicherà |

## 7. Fuori ambito

- Il server conversazionale, l'autenticazione delegata e il consenso: sono di piattaforma (UC 0061-0062).
- Gli strumenti di **scrittura**: storia `0029`, dove si definisce la regola della bozza e della conferma.

## 8. Punti aperti

Nessuno che spetti a questa storia. Resta la dipendenza aperta dalla piattaforma: finché l'epica `12-ready-for-ai-mcp`
non è implementata, questo contratto è verificato ma non usato.
