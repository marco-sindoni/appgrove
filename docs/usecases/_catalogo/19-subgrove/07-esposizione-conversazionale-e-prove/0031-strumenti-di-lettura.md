# 0031 — Strumenti di lettura

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0018`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta guidando e riceve la telefonata di un iscritto
> voglio poter chiedere a voce «chi non ha pagato da più di venti giorni» e «cosa mi si rinnova questo mese»
> così da avere la risposta senza aprire il portatile e senza sapere dove si clicca.

**Contesto.** È il requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Per
SubGrove la lettura è il caso d'uso **più forte** di tutta l'app, e per una ragione precisa (§7 della
[descrizione](../application-description.md)): le domande che il titolare si fa sul ricorrente sono **domande di
aggregazione su uno stato che cambia da solo** — «quanto mi entra il mese prossimo», «chi non ha pagato», «quanti
se ne sono andati da gennaio». Nel foglio di calcolo richiedono mezz'ora di tabelle riassuntive; nell'interfaccia
richiedono di sapere dove cliccare; in una chat sono una frase.

**Stato reale, da dire chiaro**: il livello conversazionale **non esiste ancora** nel repository (use case
0061-0066, scritti e non implementati). Questa storia non costruisce il server: **dichiara il contratto** degli
strumenti di lettura e lo tiene dentro il servizio dell'app, versionato con essa.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara sei strumenti di **sola lettura**, con nome stabile, descrizione in lingua
   naturale, schema dei parametri e schema del risultato:
   `elenca_abbonamenti`, `prossimi_rinnovi`, `scadenze_non_incassate`, `metriche_ricorrenti`,
   `previsione_incassi`, `stato_abbonato`.
2. **RF-2** — Ogni strumento è **idempotente** e non modifica nulla: nessuno di essi può creare, cambiare o
   cancellare un dato, e questo è verificabile dal contratto stesso, non solo dalla buona volontà di chi
   implementa.
3. **RF-3** — I risultati sono **minimizzati**: portano ciò che serve a rispondere alla domanda e un
   identificativo per approfondire, **non** l'anagrafica completa dell'abbonato. In particolare non escono mai da
   uno strumento di lettura: recapiti completi, indirizzo di fatturazione, identificativo fiscale, riferimento
   dell'autorizzazione all'addebito, note libere.
4. **RF-4** — Ogni risultato numerico porta con sé **come è stato calcolato**: finestra, denominatore, marcatura di
   numerosità insufficiente (storia `0029`), marcatura «solo impegnato» per la previsione (storia `0030`), stato
   provvisorio del mese in corso (storia `0027`).
5. **RF-5** — I risultati sono **paginati e limitati**: un elenco non restituisce mai l'intero archivio, dichiara
   quanti elementi ci sono in tutto e come chiedere il seguito.
6. **RF-6** — Gli strumenti rispettano gli stessi **varchi** dell'interfaccia: abilitazione all'app, ruolo
   dell'utente per conto del quale l'assistente agisce, e stato dell'abbonamento di piattaforma. Una chiamata
   dall'assistente non è una porta di servizio.
7. **RF-7** — Nessuno strumento espone la **superficie pubblica** dell'abbonato (storie `0023`-`0026`): il portale
   non si comanda da chat, né per leggerlo né per generarne collegamenti.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Il contratto vive **dentro** il servizio `abbonati`, versionato con
  esso, nella forma prevista dalla mappatura operazioni → strumenti (UC 0063). Ogni strumento è marcato
  **lettura**; la marcatura è parte del contratto e non un commento.
- **RT-2 — Isolamento fra account (§1).** Il `tenant_id` arriva **solo** dal token verificato della sessione
  delegata (UC 0062): un parametro `tenant_id` fra gli argomenti dello strumento **non esiste** e, se arrivasse,
  verrebbe ignorato. È l'invariante numero uno applicata a una superficie nuova, ed è il punto in cui è più facile
  sbagliare.
- **RT-3 — Varchi e quota (§6, §7).** Le chiamate degli strumenti attraversano la stessa catena delle rotte:
  `402` senza abilitazione, `403` per ruolo insufficiente, `429` a quota esaurita (UC 0064). La lettura non
  consuma la metrica `abbonamenti_attivi`, che è a giacenza.
- **RT-4 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte già esistenti delle storie
  precedenti: nessuna interrogazione scritta apposta per la chat, nessuna via che aggiri i controlli della risorsa.
  Se uno strumento ha bisogno di un dato che la rotta non dà, si estende la rotta.
- **RT-5 — Dati personali (§10).** La minimizzazione del **RF-3** è un requisito di conformità, non di eleganza:
  ciò che uno strumento restituisce esce dal perimetro dell'app e finisce in una conversazione. L'elenco dei campi
  esposti da ciascuno strumento va scritto nel manifesto dei dati come **destinazione** del trattamento, in
  italiano e inglese.
- **RT-6 — Registrazione eventi (§14).** Ogni chiamata registra `strumento invocato (nome)`, esito e numero di
  elementi restituiti, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** i contenuti
  e senza i nomi degli abbonati (UC 0065).
- **RT-7 — Cinque lingue (§4).** Le descrizioni degli strumenti e dei parametri sono testo rivolto a un modello,
  non all'interfaccia: restano nella lingua del contratto, mentre i **messaggi di errore** che l'utente può vedere
  passano dallo spazio-nomi `abbonati` in `en, it, fr, es, de`.
- **RT-8 — Prove (§11).** Prova che nessuno strumento di lettura scrive; prova che i campi vietati dal **RF-3** non
  compaiono in alcun risultato; prova di isolamento fra account su ogni strumento; prova che il tetto di
  paginazione non si può aggirare.

## 4. Criteri di accettazione

**CA-1 — La domanda tipica trova risposta**
- **Dato** un account con quaranta abbonamenti, di cui tre con scadenze scoperte da oltre venti giorni
- **Quando** si invoca `scadenze_non_incassate(oltre_giorni: 20)`
- **Allora** tornano i tre, con importo, giorni di ritardo e progressivo dei solleciti, e **senza** recapiti

**CA-2 — Minimizzazione**
- **Dato** un abbonato con recapiti, identificativo fiscale, mandato e una nota libera
- **Quando** si invoca `stato_abbonato`
- **Allora** il risultato porta nome, piano, stato, prossima scadenza e presenza di un'autorizzazione valida
  (sì/no), e **nessuno** dei campi vietati

**CA-3 — I numeri portano il loro contesto**
- **Dato** un account con dodici abbonamenti · **Quando** si invoca `metriche_ricorrenti`
- **Allora** il risultato porta la marcatura di numerosità insufficiente sulle percentuali e dichiara il
  denominatore

**CA-4 — Nessuna scrittura**
- **Dato** l'elenco dichiarato degli strumenti di lettura
- **Quando** gira la prova automatica del contratto
- **Allora** nessuno di essi risulta capace di scrivere, e aggiungerne uno che scriva fa fallire la prova

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un assistente delegato da un utente di `A` invoca `elenca_abbonamenti`
- **Allora** vede solo gli abbonamenti di `A`, anche se qualcuno prova a passare l'identificativo di `B`

**CA-6 — Varchi rispettati**
- **Dato** un account con abbonamento di piattaforma `canceled`
- **Quando** un assistente invoca uno strumento di lettura
- **Allora** riceve `402` con il rimedio, e nulla viene letto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `compliance`);
- [ ] prove di **unità** sulla minimizzazione dei risultati e sulla paginazione; **integrazione** sugli strumenti
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su ogni strumento dichiarato;
- [ ] **prova end-to-end**: *rimando* — il livello conversazionale non è implementato (UC 0061-0066): voce
      `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo «server conversazionale
      di piattaforma assente» e storia proprietaria UC 0063;
- [ ] **traduzioni** dei messaggi di errore visibili in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con i campi esposti da ciascuno strumento e la loro destinazione;
- [ ] **registro delle decisioni** compilato: elenco degli strumenti, campi esclusi e perché, tetto di paginazione;
- [ ] contratto degli strumenti versionato dentro il servizio e allineato alla forma della UC 0063;
- [ ] documentazione aggiornata dove descrive l'esposizione conversazionale dell'app.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0010`, `0011` | gli abbonamenti e i loro stati sono ciò che si legge |
| storie `0018`, `0021` | scadenze non incassate e progressivo dei solleciti |
| storie `0027`-`0030` | i risultati delle metriche e della previsione, con le loro marcature |
| **UC 0061-0063** (livello conversazionale di piattaforma, non implementato) | il server e la sessione delegata sono di piattaforma: qui si dichiara solo il contratto |
| **UC 0064** (abilitazione e quota sulle chiamate dell'assistente) | i varchi vanno applicati anche a questa superficie |

## 7. Fuori ambito

- gli strumenti che **scrivono**: storia `0032`;
- la costruzione del server conversazionale, la sessione delegata e il consenso: di piattaforma (UC 0061-0062);
- qualunque strumento sulla **superficie pubblica** dell'abbonato: escluso per disegno (**RF-7**);
- la generazione di testo libero (messaggi, riassunti) a partire dai dati letti: non è di questa storia.

## 8. Punti aperti

**Quanto può essere grande una risposta.** Un elenco di quattrocento abbonamenti dentro una conversazione non è una
risposta: è un archivio incollato, costoso e illeggibile. **Proposta**: tetto basso e dichiarato, con un riassunto
in testa («quaranta in tutto, ne mostro dieci») e il modo per chiedere il seguito. Il valore giusto dipende dal
server conversazionale, che non esiste ancora. Chiude: **piattaforma**, con la UC 0063.

**Il nome degli abbonati dentro una conversazione.** Anche minimizzato, `elenca_abbonamenti` restituisce nomi di
persone che non sono nostri utenti, e la conversazione può essere conservata da un fornitore che non abbiamo
scelto noi. È una questione di trattamento, non di prodotto. **Proposta**: dichiararla nel manifesto come
destinazione e portarla alla decisione di piattaforma insieme alla UC 0062. Chiude: **sviluppatore** (dati
personali) con la revisione legale.
