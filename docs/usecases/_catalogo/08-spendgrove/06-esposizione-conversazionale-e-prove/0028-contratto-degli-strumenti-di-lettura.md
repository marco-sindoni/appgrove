# 0028 — Contratto degli strumenti di lettura

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0020`, `0023`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta guidando e ha una domanda in testa
> voglio poter chiedere a voce «quanto abbiamo speso in trasferte a luglio?» o «quali note devo approvare?»
> così da avere la risposta senza aprire il computer, e senza che chi risponde veda più di quello che vedrei io.

**Contesto.** Il catalogo pone a tutte e sessanta le app il requisito di essere comandabili da una chat, e questa
app ha domande che si prestano meglio di altre: i numeri della spesa sono esattamente ciò che si chiede a voce. Lo
stato reale del repository va detto: **il livello conversazionale non esiste ancora** — è l'epica
`12-ready-for-ai-mcp`, use case 0061-0066, scritta e non implementata. Questa storia quindi non costruisce il
server: **dichiara il contratto** degli strumenti di lettura dentro il servizio dell'app, versionato con esso, così
che il giorno in cui il livello esisterà non ci sia da inventare niente.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara, in una forma leggibile da un programma e versionata insieme al codice, i
   **sette strumenti di lettura**: `elenca_spese`, `riepilogo_spese`, `elenca_da_rivedere`, `elenca_da_approvare`,
   `verifica_deducibilita`, `elenca_movimenti_orfani`, `stato_conservazione`.
2. **RF-2** — Ogni strumento porta: nome stabile, descrizione in lingua naturale, schema dei parametri, schema del
   risultato, marcatura **lettura**, e la dichiarazione di essere **idempotente e senza effetti**.
3. **RF-3** — I risultati sono **minimizzati**: si restituiscono identificativi, date, importi, stati e conteggi;
   **mai** immagini di giustificativi, mai campi a testo libero, mai motivi di rifiuto scritti a mano.
4. **RF-4** — Ogni strumento applica gli stessi filtri delle rotte corrispondenti: `tenant_id` dal token, visibilità
   per ruolo, e nessuna scorciatoia perché «chiede l'assistente».
5. **RF-5** — Gli strumenti restituiscono risultati paginati con un tetto massimo, perché una risposta di
   milleduecento righe in una conversazione non è una risposta.
6. **RF-6** — La documentazione dell'app elenca gli strumenti con un esempio di domanda per ciascuno: serve a
   chi scriverà il livello conversazionale, non all'utente finale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni strumento risolve `tenant_id` **dal token della chiamata**, non da
  un parametro: un identificativo di account passato come argomento viene ignorato. È il punto in cui l'errore
  sarebbe più facile e più grave, perché la chiamata arriva da un livello nuovo.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte esistenti
  `/api/notespese/v1/*` e non introducono un secondo percorso di accesso ai dati: una seconda strada è una seconda
  occasione di sbagliare i filtri.
- **RT-3 — Persistenza (§8).** Nessuna migrazione: la storia non tocca il database.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata. Nella panoramica resta il riquadro che spiega che le
  stesse funzioni saranno richiamabili a voce, già previsto dalla storia `0027`.
- **RT-5 — Cinque lingue (§4).** Le **descrizioni degli strumenti** sono per il livello conversazionale, non per
  l'utente: restano in una lingua sola e la scelta va dichiarata. I **risultati** non contengono testo tradotto:
  contengono codici, che il livello superiore presenterà nella lingua della conversazione.
- **RT-6 — Varchi e quota (§6, §7).** Gli strumenti di lettura non consumano quota, ma attraversano gli stessi
  varchi: token valido, app non spenta, account abilitato, ruolo sufficiente. Con abbonamento `canceled`
  rispondono `402` come le rotte.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che *è* il contratto. Dipendenza dichiarata: UC
  0061-0063 (architettura del server, autenticazione e consenso delegato, mappatura operazioni → strumenti), non
  ancora implementati. Finché non esistono, il contratto è **dichiarato e provato**, non esposto.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo, ma un **canale** nuovo verso cui i dati possono uscire: va
  scritto nel manifesto che l'esposizione conversazionale restituisce dati minimizzati e che immagini e testi liberi
  ne restano fuori. Il consenso delegato e la tracciabilità delle chiamate sono di piattaforma (UC 0062, 0065).
- **RT-9 — Registrazione eventi (§14).** Ogni invocazione di strumento è registrata con `tenant_id`, `app_id`,
  `user_id`, identificativo di correlazione, nome dello strumento e numero di righe restituite — mai i parametri se
  contengono dati personali.

## 4. Criteri di accettazione

**CA-1 — Contratto completo**
- **Dato** il servizio compilato
- **Quando** si legge la dichiarazione degli strumenti
- **Allora** i sette strumenti di lettura sono presenti, ciascuno con nome, descrizione, schema dei parametri,
  schema del risultato e marcatura `lettura`

**CA-2 — Minimizzazione**
- **Dato** una chiamata a `elenca_spese` su un account con giustificativi e note libere
- **Quando** si esamina il risultato
- **Allora** non contiene immagini, non contiene testo libero e non contiene motivi di rifiuto

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** si invoca `riepilogo_spese` con il token di `A` passando
  l'identificativo di `B` fra i parametri
- **Allora** il risultato è quello di `A`: il parametro è ignorato

**CA-4 — Visibilità per ruolo**
- **Dato** un utente con ruolo `sostiene` · **Quando** invoca `elenca_da_approvare`
- **Allora** riceve un elenco vuoto o un errore di ruolo, e in nessun caso le note altrui

**CA-5 — Paginazione**
- **Dato** un account con 900 spese nel periodo · **Quando** si invoca `elenca_spese` senza limiti
- **Allora** il risultato è troncato al tetto dichiarato e indica come chiedere il resto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla forma dei risultati (minimizzazione) e di **integrazione** su ogni strumento con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** **su ogni strumento**, compreso il tentativo di forzare l'account dai
      parametri, più la matrice dei ruoli;
- [ ] **prova end-to-end**: *nessun impatto* sulla superficie utente — gli strumenti non hanno schermate; la
      copertura del percorso `[J-NOTESPESE]` resta della storia `0031`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) lo dichiara;
- [ ] **traduzioni**: nessuna stringa visibile nuova;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la nota sul canale conversazionale;
- [ ] **registro delle decisioni** compilato, con l'elenco degli strumenti e il criterio di minimizzazione;
- [ ] contratto degli **strumenti conversazionali** dichiarato per intero e versionato dentro il servizio;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013`, `0020`, `0023`, `0027` | Sono le storie che producono i dati che gli strumenti leggono |
| UC 0061-0063 (livello conversazionale di piattaforma) | Il server non esiste: qui si dichiara il contratto, non lo si espone |

## 7. Fuori ambito

- La costruzione del server conversazionale, l'autenticazione delegata e l'applicazione della quota alle chiamate
  dell'assistente: sono di piattaforma (UC 0061, 0062, 0064).
- Gli strumenti di scrittura: storia `0029`.

## 8. Punti aperti

- **In quale lingua si scrivono le descrizioni degli strumenti**: sono testo destinato a un modello, non a una
  persona. Va deciso una volta per tutte le app, non app per app: è una decisione di piattaforma (UC 0063).
