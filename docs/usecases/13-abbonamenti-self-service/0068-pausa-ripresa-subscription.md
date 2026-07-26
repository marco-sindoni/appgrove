# UC 0068 — Pausa/ripresa subscription self-service

**Area**: 13-abbonamenti-self-service · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0026 (ciclo di vita subscription), UC 0028 (portale cliente self-service), UC 0020 (shell SPA backoffice), UC 0067 (sezione Abbonamenti backoffice)
**Fonte**: R5 (Tabella residui _INDEX.md); docs/_BACKLOG.md §Pagamenti (#09 E)
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Permettere all'utente di **sospendere temporaneamente** un abbonamento e poi **riprenderlo**, senza disdire e ri-sottoscrivere.
È la funzione "metti in pausa" (pause/resume di Paddle) esposta nel backoffice. Priorità **bassissima**: non fa parte del
lancio, si implementa quando serve una vera esigenza (es. attività stagionale che chiude per qualche mese).

**Incluso**: pulsante **Metti in pausa** e **Riprendi** nella sezione Abbonamenti (UC 0067); invio del comando pause/resume al
provider; riflesso dello stato `paused` nella card e nell'accesso; UX che spiega cosa comporta la pausa.

**Escluso**: la semantica dello stato `paused` (già definita in UC 0026: pausa = **nessun accesso**); il resto della sezione
Abbonamenti (UC 0067); la disdetta/riattivazione, che è un'altra cosa (la disdetta chiude a fine periodo, la pausa congela).

## 2. Attori & ruoli
- **Utente owner del tenant**: unico che può mettere in pausa e riprendere (azione con impatto sul contratto).
- **Utente member**: vede lo stato "in pausa" ma non può cambiarlo.
- **Backend `core`**: invia il comando pause/resume al provider e aggiorna il read-model quando torna il webhook.
- **Paddle** (Merchant of Record): sospende/riprende l'addebito ricorrente e la fatturazione.
- **Sistema webhook** (UC 0025): riconcilia lo stato `paused`/`active`.

## 3. Precondizioni
- Utente autenticato con `tenant_id` nel token verificato; ruolo owner per l'azione.
- Esiste una `subscription` in stato `active` (o `trialing`) per l'app in questione.
- Sezione Abbonamenti (UC 0067) disponibile; provider raggiungibile (stub locale in dev/test, Paddle reale gated da #14).

## 4. Flusso principale
1. Nella card dell'abbonamento (UC 0067) l'utente owner apre il menù azioni e sceglie **Metti in pausa**.
2. Una conferma spiega gli effetti: l'addebito si ferma, **l'accesso all'app si sospende** finché non riprendi, i dati restano
   secondo la retention (la pausa **non** cancella nulla).
3. Confermando, il backend invia il comando **pause** al provider.
4. Al ritorno del webhook la `subscription` passa a `status = paused`; la card mostra **In pausa** e l'accesso all'app è chiuso
   (mappa stato→accesso di UC 0026: `paused` = nessun accesso).
5. Più avanti l'utente owner sceglie **Riprendi**: il backend invia il comando **resume**.
6. Al webhook lo stato torna `active`, riparte l'addebito ricorrente, l'accesso è ripristinato; la card torna **Attivo**.

## 5. Flussi alternativi / edge / errori
- **Pausa durante il trial**: da decidere se ammessa (vedi Punti aperti). Comportamento atteso di default: la pausa non è
  offerta durante `trialing` (non c'è ancora un addebito da sospendere).
- **Pausa durante dunning (`past_due`)**: non offerta; prima si risolve il pagamento in ritardo dal portale.
- **Riprendi su abbonamento nel frattempo scaduto**: se durante la pausa il periodo/impegno decade lato provider, la ripresa
  può non essere possibile → la UI propone invece **riattiva/ri-sottoscrivi**; nessun comando resume inviato a vuoto.
- **Comando fallito al provider**: risposta `problem+json`, messaggio non distruttivo, stato invariato finché il webhook non conferma.
- **Accesso mentre in pausa**: qualunque chiamata verso l'app in stato `paused` è rifiutata dai gate di enforcement (UC 0027),
  coerente con "nessun accesso".

## 6. Schermate & stati
- **Card abbonamento (UC 0067)** con l'azione **Metti in pausa** (quando `active`) o **Riprendi** (quando `paused`).
  - *loading*: la card mostra "in aggiornamento" tra il comando e la conferma del webhook.
  - *stato paused*: badge grigio **In pausa**, testo "L'accesso è sospeso. Riprendi quando vuoi.", pulsante **Riprendi**.
  - *error*: banner non distruttivo "Non riusciamo a mettere in pausa ora, riprova".
- **Modale di conferma pausa**: elenca gli effetti (addebito fermo, accesso sospeso, dati conservati) e un pulsante **Metti in pausa**.
- **Copy chiave** (italiano): "Metti in pausa l'abbonamento", "L'accesso resta sospeso finché non riprendi", "I tuoi dati non
  vengono cancellati", "Riprendi abbonamento".

## 7. Dati toccati
- **Lettura/scrittura logica**: `platform.subscription.status` (transizioni `active` ⇄ `paused`), aggiornato **solo** dal
  consumer webhook (UC 0025), mai a mano. Si leggono `current_period_end` e `app_tier_id` per la card.
- **Nessun dato personale nuovo**: metodo di pagamento e fatturazione restano in capo a Paddle (Merchant of Record). La base
  del trattamento dell'abbonamento è l'esecuzione del contratto; in pausa i dati dell'app restano secondo la retention (#13).

## 8. Permessi & gate
- **Invariante 1**: `tenant_id` solo dal token verificato.
- **Invariante 2**: filtro row-level `WHERE tenant_id = :tid`; si agisce solo sull'abbonamento del proprio tenant.
- **Invariante 4**: log strutturati con `tenant_id`, `app_id`, `user_id` per pause e resume.
- **Ruolo**: pause/resume riservati all'**owner** (`@RolesAllowed`).
- **Enforcement**: in stato `paused` la catena entitled→ruolo→quota nega l'accesso all'app (UC 0027); i diritti sulla
  protezione dei dati personali restano esenti (#09 F31).

## 9. Requisiti di test
- **Integration (Testcontainers)**: comando pause porta la `subscription` a `paused` (via webhook simulato); resume la riporta
  ad `active`; l'accesso derivato segue la mappa stato→accesso.
- **Security / isolamento cross-tenant**: un tenant non può mettere in pausa l'abbonamento di un altro.
- **E2E Playwright (L2)**: percorso metti in pausa → verifica accesso negato all'app → riprendi → accesso ripristinato.
- **Verde prima del merge**: aree `backend` e `frontend` di `run-tests.sh` (limitatamente alle parti toccate).

## 10. Riferimenti & Definition of Done
- **Fonte**: R5 (Tabella residui _INDEX.md), docs/_BACKLOG.md §Pagamenti (#09 E).
- **Storie collegate**: UC 0026 (stato `paused` = nessun accesso), UC 0028 (portale), UC 0020 (shell), UC 0067 (sezione Abbonamenti).
- **Definition of Done**:
  1. Azione pausa/ripresa nella card della sezione Abbonamenti, riservata all'owner.
  2. Comando pause/resume al provider; stato riconciliato dal webhook; accesso coerente con la mappa di UC 0026.
  3. UX che spiega gli effetti (accesso sospeso, dati conservati); banner errore non distruttivi.
  4. Test integration + security + E2E L2 verdi; log strutturati.

## Punti aperti / decisioni differite
- **Pausa durante il trial** *(decisione di prodotto, owner: questo UC 0068)*: ammettere la pausa mentre `trialing`? Il default
  proposto è **no** (nessun addebito da sospendere), ma va confermato quando la funzione entra in roadmap.
- **Durata massima della pausa**: Paddle consente politiche di pausa (durata, ripresa automatica). Da decidere se imporre un
  tetto o lasciare pausa a tempo indefinito → differito finché la funzione non è prioritaria.
- **Implementazione reale Paddle** *(gated #14)*: `PaddlePaymentProvider` non implementa ancora pause/resume reali; lo stub
  locale copre dev/test. Da completare quando esiste l'account Paddle.
- **Priorità**: bassissima, non al lancio. Tracciato qui per non perderlo; si implementa su reale esigenza.
