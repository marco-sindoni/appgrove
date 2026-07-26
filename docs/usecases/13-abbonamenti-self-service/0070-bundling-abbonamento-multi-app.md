# UC 0070 — Bundling: più app in un unico abbonamento

**Area**: 13-abbonamenti-self-service · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0022 (pricing-as-code & sincronizzazione), UC 0026 (ciclo di vita subscription)
**Fonte**: docs/_BACKLOG.md §Pagamenti (#09 K50) — priorità bassissima
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Consentire di vendere **più app in un unico abbonamento** ("bundle", pacchetto): una sola transazione invece di N. Il
beneficio è economico: Paddle applica una **quota fissa per transazione** (circa 0,50 $) oltre alla percentuale; concentrando
più app in una sola transazione, quella quota fissa si **diluisce** su tutte le app del pacchetto. È una **leva di margine
futura** per pacchetti di app economiche, dove la quota fissa pesa in proporzione. **Non fa parte del lancio.**

**Incluso** (quando maturerà): modellare un'entità **bundle** sopra i tier per-app; definire il prezzo del bundle e la sua
composizione; far sì che una sola `subscription`/transazione conceda l'entitlement a tutte le app del pacchetto; riflettere il
bundle nel catalogo e nella sezione Abbonamenti.

**Escluso**: il pricing per-app singolo (UC 0022, resta il caso base); la semantica del ciclo di vita (UC 0026, che il bundle
riusa); ogni cambio ai gate di enforcement che non sia la sorgente dell'entitlement.

## 2. Attori & ruoli
- **Platform-admin / autore del listino**: definisce quali app compongono un bundle e a che prezzo (pricing-as-code, UC 0022).
- **Utente owner del tenant**: acquista il bundle, ottiene l'accesso a tutte le app incluse con una transazione.
- **Backend `core`**: deriva l'entitlement di tutte le app del bundle dalla singola `subscription` di bundle.
- **Paddle** (Merchant of Record): elabora la singola transazione; è qui che si realizza il risparmio sulla quota fissa.

## 3. Precondizioni
- Pricing-as-code (UC 0022) e sincronizzazione col provider funzionanti per il caso per-app singolo.
- Ciclo di vita subscription (UC 0026) consolidato: il bundle riusa stati, upgrade/downgrade, dunning, cancellazione.
- Almeno due app candidate a un pacchetto economicamente sensato.

## 4. Flusso principale
1. L'autore del listino definisce un **bundle** nel pricing-as-code: identificativo, app incluse, prezzo unico, ciclo di
   fatturazione, eventuale trial.
2. La sincronizzazione (UC 0022) crea il corrispondente prodotto/price lato provider — **una** riga di prezzo per il bundle,
   non una per app.
3. L'owner acquista il bundle dal catalogo: **una sola transazione** verso Paddle → **una sola** quota fissa.
4. La pipeline webhook (UC 0025) crea/aggiorna **una** `subscription` di bundle.
5. Il backend **deriva l'entitlement per tutte le app incluse** da quella singola subscription (la sorgente dell'accesso
   diventa "sei coperto dal bundle X" invece di "hai la subscription dell'app Y").
6. La sezione Abbonamenti (UC 0067) mostra il bundle come **una** voce che elenca le app incluse.

## 5. Flussi alternativi / edge / errori
- **Sovrapposizione con una subscription per-app esistente**: se il tenant ha già l'app singola e compra il bundle che la
  include, serve una regola di composizione (rimborso/credito, sostituzione, coesistenza) → decisione di prodotto, vedi Punti aperti.
- **Downgrade/uscita da un'app del bundle**: il bundle è atomico per definizione; "togliere una sola app" contraddice il
  pacchetto → di default non ammesso, si esce dall'intero bundle.
- **Metrica quota per-app dentro il bundle**: ogni app mantiene la propria metrica `flow`/`stock`; il bundle non fonde le
  quote, concede solo l'accesso. La derivazione entitlement deve saper mappare bundle → limiti per-app.
- **Immutabilità dei prezzi**: cambiare l'importo del bundle segue la stessa regola del per-app (nuovo price, mai mutare un
  price vivo; grandfathering degli abbonati esistenti).
- **App dismessa mentre è in un bundle**: la dismissione (skill `drop-application`) deve considerare i bundle che la includono.

## 6. Risorse & runbook
- **Modello dati**: nuova entità `bundle` (identificativo, prezzo, ciclo) + relazione `bundle`→app incluse; la `subscription`
  di bundle punta al bundle invece che a un singolo `app_tier`. La **derivazione entitlement** va estesa perché una
  subscription di bundle conceda N app.
- **Pricing-as-code (UC 0022)**: estendere lo schema del listino per dichiarare i bundle; la sincronizzazione crea un solo
  price per bundle.
- **Runbook**: la creazione di un bundle è un'operazione di listino (pricing-as-code + sync), non un'azione manuale sul
  provider. Il grandfathering degli abbonati segue la skill `pricing-change`.
- **Rollback**: ritirare un bundle = smettere di offrirlo nel catalogo; gli abbonati esistenti restano grandfathered fino a
  disdetta (nessuna migrazione forzata).

## 7. Dati toccati
- **Nuove entità**: `bundle` e la mappa `bundle`→`app` (o `bundle`→`app_tier`); riferimento dalla `subscription` alla natura
  "bundle". Impatta la derivazione entitlement (oggi per-app).
- **Riuso**: `platform.subscription` (stati, periodo, cancel, trial) e `platform.app_price` per il prezzo del bundle.
- **Dati personali**: nessun dato personale nuovo. Metodo di pagamento e fatture restano in capo a Paddle (Merchant of
  Record). Base del trattamento: esecuzione del contratto; retention secondo il manifesto billing (#13).

## 8. Permessi & gate
- **Invariante 1**: `tenant_id` solo dal token verificato.
- **Invariante 2**: la `subscription` di bundle è tenant-scoped, filtro row-level `WHERE tenant_id = :tid`.
- **Invariante 4**: log strutturati con `tenant_id`, `app_id` (di ciascuna app coperta) e `user_id`.
- **Catena dei gate**: invariata (entitled→ruolo→quota); cambia solo **da dove arriva l'entitled** (dal bundle, che copre più
  app). I diritti sulla protezione dei dati personali restano esenti (#09 F31).

## 9. Requisiti di test
- **Integration (Testcontainers)**: una subscription di bundle concede l'entitlement a tutte le app incluse; disdetta del
  bundle le toglie tutte insieme.
- **Derivazione entitlement**: un'app coperta dal bundle risulta `entitled` senza una subscription per-app dedicata.
- **Quota per-app**: le metriche `flow`/`stock` restano distinte per app dentro il bundle.
- **Security / isolamento cross-tenant**: il bundle di un tenant non concede accesso a un altro.
- **Immutabilità prezzo**: cambiare il prezzo del bundle crea un nuovo price; gli esistenti restano sul vecchio.
- **Verde prima del merge**: aree `backend`, `frontend`, `compliance` (RoPA) di `run-tests.sh` toccate.

## 10. Riferimenti & Definition of Done
- **Fonte**: docs/_BACKLOG.md §Pagamenti (#09 K50) — priorità bassissima, non al lancio.
- **Storie collegate**: UC 0022 (pricing-as-code), UC 0026 (ciclo di vita che il bundle riusa), UC 0027 (enforcement), UC 0067
  (sezione Abbonamenti che mostra il bundle).
- **Definition of Done**:
  1. Modello `bundle` sopra i tier per-app + prezzo unico dichiarato nel pricing-as-code.
  2. Una transazione/subscription concede l'entitlement a tutte le app incluse (derivazione estesa).
  3. Regole di composizione con subscription per-app esistenti definite e testate.
  4. Immutabilità prezzi e grandfathering rispettati; test integration + security verdi; RoPA aggiornata.

## Punti aperti / decisioni differite
- **Composizione con subscription per-app esistenti** *(decisione di prodotto, owner: questo UC 0070)*: cosa succede se il
  tenant ha già l'app singola e compra il bundle che la include (credito/rimborso/sostituzione/coesistenza)? Va deciso prima di
  implementare.
- **Uscita parziale dal bundle**: ammettere di togliere una sola app dal pacchetto? Default proposto: no (il bundle è atomico).
- **Prezzo del bundle vs somma dei per-app**: politica di sconto del pacchetto (leva di margine) è una scelta commerciale, non tecnica.
- **Priorità**: bassissima. Tracciato qui per non perderlo; si implementa quando esiste un pacchetto economicamente sensato e
  un volume di transazioni che rende significativo il risparmio sulla quota fissa. La misura del risparmio effettivo si aggancia
  a UC 0071 (file fratello `0071-riconciliazione-netto-revenue.md`).
- **Dismissione app in bundle**: coordinare con la skill `drop-application` quando un'app inclusa viene ritirata.
