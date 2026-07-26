# UC 0071 — Riconciliazione netto/revenue

**Area**: 13-abbonamenti-self-service · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0025 (pipeline webhook), UC 0006 (observability), UC 0021 (console admin)
**Fonte**: docs/_BACKLOG.md §Pagamenti (#09 K51)
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Dare **osservabilità del netto incassato**. Paddle è **Merchant of Record** (venditore ufficiale verso il cliente): incassa dal
cliente, trattiene le proprie fee (percentuale + quota fissa per transazione) e paga a noi il **netto**, su una **schedule di
payout** (accrediti periodici, non uno per ogni vendita). La conseguenza è che il fatturato "lordo" mostrato al cliente e il
denaro che effettivamente arriva sul conto **differiscono**, e la differenza non è costante (dipende dal mix di transazioni).
Questo use case rende visibile il netto reale, così che i KPI economici (ricavo ricorrente mensile, churn) siano leggibili
insieme a quanto è davvero entrato.

**Non è un blocco**: è una **nota operativa / di osservabilità**, non un gate né un vincolo di go-live. Si aggancia
naturalmente ai KPI ricchi della console admin (UC 0021) e all'observability (UC 0006).

**Incluso**: acquisire dal provider gli eventi/dati di **payout** e di **fee per transazione**; ricondurli alle nostre
transazioni; esporre nella console admin il **netto** accanto al lordo; una vista di riconciliazione (lordo → fee → netto →
payout accreditato).

**Escluso**: contabilità fiscale e adempimenti (competenza del commercialista, vedi docs/_COMMERCIALISTA.md); la gestione del
metodo di pagamento e delle fatture verso il cliente (in capo a Paddle come Merchant of Record); il calcolo del prezzo (UC 0022).

## 2. Attori & ruoli
- **Platform-admin / founder**: consuma la vista di riconciliazione per capire quanto entra davvero.
- **Backend `core`**: acquisisce gli eventi payout/fee dal provider (via webhook UC 0025 o pull periodico) e li riconcilia.
- **Paddle** (Merchant of Record): fonte autorevole di fee applicate e payout accreditati.
- **Sistema di observability** (UC 0006): raccoglie le metriche del netto per dashboard e allarmi.

## 3. Precondizioni
- Pipeline webhook (UC 0025) attiva e in grado di ricevere gli eventi di transazione/payout dal provider.
- Console admin (UC 0021) disponibile per ospitare la vista.
- Observability (UC 0006) disponibile per le metriche aggregate.
- Account Paddle reale (gated da #14): in locale/test si usano dati simulati dallo stub.

## 4. Flusso principale
1. A ogni transazione andata a buon fine, il provider comunica (via webhook) l'importo **lordo** e le **fee** applicate
   (percentuale + quota fissa).
2. Il backend registra, per transazione, lordo e fee, e ne deriva il **netto per transazione**.
3. Periodicamente il provider effettua un **payout** (accredito) del netto accumulato; l'evento di payout viene acquisito e
   ricondotto all'insieme di transazioni che lo compongono.
4. Il backend **riconcilia**: somma dei netti per transazione ↔ importo del payout accreditato; segnala scostamenti.
5. La console admin (UC 0021) mostra la vista **lordo → fee → netto → payout**, per periodo, insieme ai KPI (ricavo ricorrente
   mensile, churn).
6. L'observability (UC 0006) espone le metriche del netto (serie temporali, dashboard, eventuali allarmi su payout mancati).

## 5. Flussi alternativi / edge / errori
- **Payout in valuta diversa / cambio**: se il provider accredita in una valuta con conversione, la riconciliazione deve
  tracciare il tasso applicato → di norma è il provider a comunicarlo; da mappare.
- **Rimborsi e chargeback**: riducono il netto e possono comparire in un payout successivo con segno negativo; la
  riconciliazione deve gestirli senza rompere la quadratura del periodo.
- **Payout multi-periodo**: un payout può coprire transazioni a cavallo di due periodi; la vista deve attribuire correttamente.
- **Evento payout mancante o in ritardo**: allarme di observability (UC 0006) "payout atteso non ricevuto"; nessun blocco, solo
  segnalazione.
- **Scostamento lordo-netto oltre soglia**: se le fee effettive superano una soglia attesa (es. molte micro-transazioni con
  quota fissa pesante), evidenziarlo — è il segnale che alimenta la leva di bundling (UC 0070).
- **Dati provider non disponibili in locale**: lo stub genera payout/fee simulati; la vista funziona su dati finti in dev/test.

## 6. Risorse & runbook
- **Acquisizione**: estendere il consumer webhook (UC 0025) per mappare gli eventi di transazione (con fee) e di payout; in
  alternativa un pull periodico dall'API del provider se il webhook non porta il dettaglio fee.
- **Persistenza**: registrazione per-transazione di lordo/fee/netto e degli eventi di payout, ricondotti alle transazioni.
- **Vista**: pannello nella console admin (UC 0021) "Riconciliazione" con lordo → fee → netto → payout per periodo.
- **Metriche**: serie temporali del netto e del rapporto fee/lordo esposte a observability (UC 0006).
- **Runbook**: verifica periodica della quadratura payout ↔ somma netti; in caso di scostamento, confronto con il pannello
  transazioni del provider. Nessuna azione automatica correttiva: è osservabilità.
- **Rollback**: la funzione è in sola lettura/aggregazione; disattivarla non impatta gli abbonamenti né i pagamenti.

## 7. Dati toccati
- **Nuove entità**: registrazione delle **fee per transazione** (lordo, fee, netto, valuta) e degli **eventi di payout**
  (importo accreditato, periodo, transazioni collegate). Non tenant-scoped nel senso applicativo: sono dati economici della
  piattaforma; l'aggregazione per-tenant/per-app serve solo ai KPI.
- **Lettura**: eventi di transazione dal provider (UC 0025); `platform.subscription` per collegare la transazione all'app/tenant.
- **Dati personali**: dati economici di transazione, non categorie particolari. Se collegati a un account, valgono base
  "esecuzione del contratto" e retention del manifesto billing (#13). La vista admin aggrega; non è un nuovo trattamento verso
  l'esterno. Nessun dato di carta è mai trattato da noi (in capo a Paddle).

## 8. Permessi & gate
- **Accesso**: la vista di riconciliazione è **solo per il platform-admin** nella console admin (UC 0021), non per i tenant.
- **Invariante 1**: dove si aggrega per tenant/app, il `tenant_id` è quello registrato dalla pipeline, mai da input di richiesta.
- **Invariante 2**: le eventuali viste per-tenant restano filtrate; la vista admin globale è un contesto amministrativo separato,
  soggetto ai propri controlli di ruolo.
- **Invariante 4**: log strutturati con `tenant_id`/`app_id` dove la transazione è riconducibile, oltre all'identità admin che consulta.
- **Gate**: non introduce gate verso i tenant; i diritti sulla protezione dei dati personali non sono interessati (dati economici aggregati).

## 9. Requisiti di test
- **Integration (Testcontainers)**: dato un insieme di transazioni con fee note, il netto derivato è corretto; un payout
  simulato quadra con la somma dei netti collegati.
- **Edge**: rimborso/chargeback riducono il netto e non rompono la quadratura; payout multi-periodo attribuito correttamente.
- **Observability**: le metriche del netto sono esposte; allarme su payout atteso mancante.
- **Security**: la vista è accessibile solo al platform-admin; nessun tenant vede i dati economici globali.
- **Verde prima del merge**: aree `backend` e (se toccata) `infra`/observability di `run-tests.sh`.

## 10. Riferimenti & Definition of Done
- **Fonte**: docs/_BACKLOG.md §Pagamenti (#09 K51).
- **Storie collegate**: UC 0025 (pipeline webhook, sorgente eventi), UC 0006 (observability), UC 0021 (console admin, sede della vista).
- **Definition of Done**:
  1. Acquisizione di fee per transazione e di eventi payout dal provider (reale gated #14, stub in dev/test).
  2. Riconciliazione lordo → fee → netto → payout, con gestione di rimborsi/chargeback e payout multi-periodo.
  3. Vista admin del netto accanto ai KPI (ricavo ricorrente mensile, churn); metriche esposte a observability.
  4. Test integration + edge + observability + security verdi.

## Punti aperti / decisioni differite
- **Sorgente del dettaglio fee** *(owner: questo UC 0071)*: verificare se il webhook Paddle porta il dettaglio fee per
  transazione o se serve un pull periodico dall'API; decisione rimandata all'account Paddle reale (gated #14).
- **Valuta e cambio**: se i payout arrivano in valuta convertita, definire come tracciare il tasso applicato ai fini della
  quadratura.
- **Soglia di allarme fee/lordo**: fissare la soglia oltre cui evidenziare che le fee pesano troppo (segnale per il bundling,
  UC 0070) — è una scelta operativa da tarare sui dati reali.
- **Confine con la fiscalità**: questa vista è osservabilità gestionale, non contabilità; gli adempimenti fiscali restano col
  commercialista (docs/_COMMERCIALISTA.md). Da non confondere le due cose.
- **Priorità**: nota operativa, non bloccante; si implementa quando c'è volume reale di payout da riconciliare.
