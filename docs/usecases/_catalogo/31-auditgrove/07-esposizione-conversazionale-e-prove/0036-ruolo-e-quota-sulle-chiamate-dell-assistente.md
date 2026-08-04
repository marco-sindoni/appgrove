# 0036 — Ruolo e quota sulle chiamate dell'assistente

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0036` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0034`, `0035`, `0004`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio che anche l'assistente che interroga AuditGrove sia soggetto alle stesse regole di chi entra
> dall'interfaccia, e che le sue letture finiscano nel registro
> così da non avere una porta di servizio che aggira proprio il controllo che ho comprato.

**Contesto.** AuditGrove è l'unica applicazione del catalogo in cui il livello conversazionale interroga il
registro **delle proprie stesse azioni**. Ne discende una simmetria che va resa esplicita: se un assistente può
leggere il registro senza lasciare traccia, il registro ha un buco esattamente nel punto in cui serve di più.
Questa storia chiude il cerchio: i varchi valgono per l'assistente come per le persone, la quota conta anche le
sue chiamate, e ogni sua invocazione diventa una riga del registro.

## 2. Requisiti funzionali

1. **RF-1** — Ogni invocazione di uno strumento (di lettura o di scrittura) attraversa la stessa catena di varchi
   delle rotte normali: autenticazione, app accesa, account abilitato, ruolo sufficiente, quota disponibile.
2. **RF-2** — Il ruolo che vale è quello della **persona che ha delegato** l'assistente, mai un ruolo proprio
   dell'assistente: un utente con ruolo di revisore in sola lettura non ottiene, attraverso l'assistente, poteri
   che non ha.
3. **RF-3** — Ogni invocazione di uno strumento **è una riga del registro**, con la sorgente «assistente», la
   persona delegante, lo strumento invocato e l'esito, e consuma una unità della metrica `actions`.
4. **RF-4** — Fa eccezione `verifica_integrita`, che non consuma quota, coerentemente con la scelta della storia
   0014; l'invocazione resta comunque registrata.
5. **RF-5** — A quota esaurita l'assistente riceve un errore comprensibile che spiega il rimedio, e il rifiuto è
   contato come previsto dalla storia 0004.
6. **RF-6** — Le letture dell'assistente sono distinguibili nella cronologia da quelle fatte dall'interfaccia:
   chi guarda il registro deve poter chiedere «cosa ha letto l'assistente».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` si ricava dalla credenziale delegata verificata al bordo
  e **mai** dai parametri della chiamata conversazionale; ogni operazione servita dietro uno strumento filtra per
  account.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova: si estende l'applicazione dei
  varchi già esistenti al canale conversazionale.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si aggiunge alla riga dell'azione la distinzione del canale
  di origine, con migrazione sullo schema `app_agentaudit`.
- **RT-4 — Modulo frontend (§3, §5).** Un filtro «canale» nella cronologia (interfaccia / ingresso dichiarato /
  assistente), che riusa la schermata della storia 0024. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi dei canali e i messaggi di errore passano dallo spazio-nomi `agentaudit`
  e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Prima di servire uno strumento il servizio prenota una unità della metrica
  `actions` (natura `flow`); a quota esaurita risponde con l'errore previsto dal protocollo conversazionale e
  registra il rifiuto. Con abbonamento `canceled` risponde `402`; con `past_due` la funzione resta accessibile.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: la storia governa quelli dichiarati alle
  0034 e 0035. Rispetta UC 0064 (applicazione di abilitazione e quota alle chiamate dell'assistente) e UC 0065
  (tracciamento e minimizzazione), entrambi non implementati.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo che riguardi una persona, salvo l'indicazione della persona
  delegante sull'azione registrata, che coincide con il campo già dichiarato di chi ha chiesto. La voce del
  manifesto va estesa per dire che quel campo può riferirsi anche a una delega conversazionale.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «strumento invocato», «strumento negato per ruolo»,
  «strumento negato per quota» sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — La lettura dell'assistente lascia traccia**
- **Dato** un account con azioni registrate
- **Quando** un assistente invoca `elenca_azioni` per conto di una persona
- **Allora** compare nella cronologia una riga con canale «assistente», la persona delegante e lo strumento
  invocato, e la quota consumata è aumentata di una unità

**CA-2 — Il ruolo non si amplia passando dall'assistente**
- **Dato** una persona con ruolo di revisore in sola lettura
- **Quando** il suo assistente invoca `proponi_regola`
- **Allora** l'invocazione è negata per ruolo insufficiente, la negazione è registrata e nessuna bozza è creata

**CA-3 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto della metrica `actions`, banda di cortesia compresa
- **Quando** un assistente invoca uno strumento di lettura
- **Allora** riceve un errore che spiega come rimediare, nulla viene letto, e il rifiuto è contato

**CA-4 — La verifica non consuma quota**
- **Dato** un account vicino al proprio tetto
- **Quando** un assistente invoca `verifica_integrita`
- **Allora** la verifica viene eseguita, la quota non cambia, e l'invocazione compare comunque nel registro

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un assistente delegato da un utente di `A` invoca uno strumento indicando nei parametri l'account `B`
- **Allora** opera su `A`, e il tentativo resta registrato come tale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'applicazione dei varchi al canale conversazionale e di **integrazione** sul consumo
      di quota, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle invocazioni conversazionali;
- [ ] **prova end-to-end**: risposta «rimando» per la parte conversazionale — proprietaria l'epica di piattaforma
      12 — e «coprire ora» per il filtro «canale» nella cronologia, nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per la delega conversazionale;
- [ ] **registro delle decisioni** compilato, con la scelta di registrare anche le letture dell'assistente e il
      suo costo in quota;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, e il fatto è dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove il comportamento della quota è descritto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0034`, `0035` | Sono gli strumenti su cui si applicano varchi e quota |
| storia `0004` | La metrica, la banda di cortesia e il conteggio dei rifiuti |
| storia `0029` | La matrice dei ruoli, che qui non deve poter essere aggirata |
| UC 0062, 0064, 0065 (non implementati) | Delega, applicazione dei varchi e tracciamento del canale conversazionale sono di piattaforma: qui si dichiara il comportamento atteso e lo si collauda sul canale simulato |

## 7. Fuori ambito

- la costruzione del meccanismo di delega e di consenso: è di piattaforma (UC 0062);
- la scelta di quali strumenti esistono: storie 0034 e 0035;
- il percorso end-to-end complessivo: storia 0037.

## 8. Punti aperti

- **Far pagare le letture dell'assistente è una scelta discutibile.** Registrarle è indiscutibile; contarle nella
  quota rende più caro proprio il modo d'uso che vogliamo incoraggiare. L'alternativa — registrarle senza contarle
  — apre però la strada a un consumo di risorse non limitato. Propongo di contarle e di dimensionare i piani di
  conseguenza; decide lo sviluppatore, perché tocca il listino.
- **Il modello di consenso delegato non è deciso** (UC 0062 è dichiarato punto di escalation di piattaforma): la
  granularità degli ambiti concessi all'assistente determina quanto di questa storia sia davvero applicabile.
