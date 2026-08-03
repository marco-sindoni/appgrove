# 0016 — Avvisi di ritardo

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 03 — Esecuzione quotidiana
**Storia**: `0016` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile che non ha tempo di controllare
> voglio che sia l'app a dirmi cosa è scaduto e cosa è fermo da troppo
> così da intervenire quando serve, e non quando il cliente telefona.

**Contesto.** In una micro-impresa nessuno apre un cruscotto per cercare i problemi: i problemi devono venire a
galla da soli. Questa storia introduce gli avvisi, con un confine da tenere fermo: restano **dentro l'app**.
Mandare posta elettronica o messaggi verso l'esterno significherebbe aggiungere recapiti, consensi, disiscrizioni
e un fornitore di invio — tutta roba che appartiene ad altre app della suite e che qui non serve per il valore
promesso.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un elenco di avvisi personale, con il conteggio dei non letti visibile nel modulo.
2. **RF-2** — Generano un avviso: un'attività assegnata a me, una citazione con la chiocciola in un commento, una
   mia attività scaduta, una mia attività ferma nello stesso stato da più di un numero di giorni prefissato.
3. **RF-3** — Il responsabile del progetto (ruolo `admin`) riceve inoltre l'avviso quando un **traguardo** passa
   a rischio o quando il budget del progetto supera una soglia (storia 0021).
4. **RF-4** — Gli avvisi si segnano come letti, singolarmente o tutti insieme; non si accumulano oltre un tetto
   ragionevole, oltre il quale i più vecchi vengono archiviati.
5. **RF-5** — Gli avvisi **non escono dall'app**: nessuna posta elettronica, nessun messaggio, nessun
   collegamento a canali esterni.
6. **RF-6** — Il calcolo degli avvisi di scadenza e di attività ferme avviene con una lavorazione programmata,
   idempotente: eseguita due volte nello stesso giorno non produce doppioni.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Gli avvisi sono per account e per persona: la lettura filtra per
  `tenant_id` dal token verificato **e** per l'identificativo dell'utente che chiama.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/progetti/v1/me/notifications` e
  `POST /api/progetti/v1/me/notifications/read`; errori in `application/problem+json`; OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V9__avvisi.sql`: `notification` con `tenant_id`, `user_id`, tipo,
  riferimento all'entità, stato di lettura, colonne di controllo e cancellazione logica; chiave di idempotenza
  `(tenant_id, user_id, tipo, riferimento, giorno)` che impedisce i doppioni.
- **RT-4 — Modulo frontend (§3, §5).** Campanello con conteggio nella barra del modulo e pannello degli avvisi;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I testi degli avvisi si compongono da modelli tradotti in `en, it, fr, es, de`:
  **non** si salva il testo già composto nella lingua di chi lo ha generato, altrimenti chi legge in un'altra
  lingua trova un messaggio nella lingua sbagliata.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. La lavorazione programmata **non** genera avvisi
  per account con abbonamento `canceled` o `paused`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dedicato: chiedere «cosa è in ritardo» si
  risponde con `search_tasks` e `get_project_progress` (storia 0028), che sono già letture.
- **RT-8 — Dati personali (§10).** `notification.user_id` è un dato personale: voce nel manifesto in italiano e
  inglese, campo annotato, tabella in `exportData` e `purgeData`. L'avviso **non** contiene il testo del commento
  che l'ha generato, solo il riferimento.
- **RT-9 — Registrazione eventi (§14).** «Avvisi generati» con conteggio per tipo, `tenant_id`, `app_id` e
  correlazione; mai contenuti.

## 4. Criteri di accettazione

**CA-1 — Avviso di assegnazione**
- **Dato** una persona dell'account
- **Quando** le viene assegnata un'attività
- **Allora** riceve un avviso non letto, e il conteggio nel modulo si aggiorna

**CA-2 — Idempotenza**
- **Dato** un'attività scaduta da tre giorni
- **Quando** la lavorazione programmata gira due volte nello stesso giorno
- **Allora** esiste un solo avviso di scadenza per quel giorno

**CA-3 — Nessun invio esterno**
- **Dato** la generazione di un avviso di qualunque tipo
- **Quando** si osservano le chiamate in uscita del servizio
- **Allora** nessuna comunicazione lascia la piattaforma

**CA-4 — Lingua di lettura**
- **Dato** un avviso generato da una persona che usa l'italiano
- **Quando** lo legge una persona che usa il tedesco
- **Allora** il testo dell'avviso è in tedesco

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede i propri avvisi
- **Allora** non vede nulla dell'account `B`, in nessuna condizione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sull'idempotenza della lavorazione e di **integrazione** sulla generazione e la lettura;
- [ ] prova di **isolamento fra account** sugli avvisi;
- [ ] **prova end-to-end**: nessun impatto — `[J-PROGETTI]` non attende una lavorazione programmata; motivo
      registrato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con i testi composti al momento della lettura;
- [ ] **manifesto dei dati** aggiornato per `notification`;
- [ ] **registro delle decisioni** compilato, con annotata la scelta «avvisi solo dentro l'app» e il perché;
- [ ] controllo automatico di **accessibilità** verde sul pannello degli avvisi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0012` | L'assegnazione è la prima sorgente di avvisi |
| Storia `0013` | Il pannello vive accanto alle attività personali |
| Storia `0014` | La citazione con la chiocciola genera un avviso |
| Storia `0021` | Gli avvisi di budget arrivano da lì: finché non c'è, quel tipo di avviso non si genera |

## 7. Fuori ambito

- l'invio di posta elettronica, messaggi o notifiche verso canali esterni: escluso per scelta (RF-5);
- le preferenze fini su quali avvisi ricevere: rimandate, perché sono configurazione;
- il riepilogo giornaliero: rimandato per lo stesso motivo.

## 8. Punti aperti

- **Se in futuro gli avvisi dovessero uscire dall'app** (posta elettronica o messaggistica) servirebbero recapito,
  informativa, disiscrizione e probabilmente un fornitore di invio: è una decisione di prodotto con impatti sui
  dati personali, da prendere in sede di piattaforma e non dentro questa app.
