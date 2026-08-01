# UC 0097 — Dashboard operativa del workspace (+ Workspace ID in Account)

**Area**: 21-catalogo-app-backoffice · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0095 (App catalog — destinazione delle scorciatoie), UC 0027 (quota SPI), UC 0028 (subscription read-model), UC 0059 (membri & inviti), UC 0056 (documenti legali pendenti), UC 0077 (entitlement + stati shell)
**Fonte decisioni**: change `0066` (proposta UX approvata), #03 (frontend)
**Ultimo aggiornamento**: 2026-08-01

## 1. Obiettivo / Scope

Trasformare la **Dashboard** da pagina-segnaposto (oggi mostra solo l'identificativo del workspace) a **panoramica
operativa**: le app attive con il consumo, gli avvisi che richiedono un'azione, i numeri essenziali del workspace e
le scorciatoie. L'**identificativo tecnico del workspace (UUID)** si sposta nella pagina **Account**, con un
pulsante di copia (serve a chi apre un ticket di supporto).

**Incluso**: nuova Dashboard; spostamento dell'UUID in Account. **Escluso**: nuovi dati/telemetrie non ancora
esposti dal backend (niente grafici di utilizzo storico); catalogo (UC 0095) e Billing (UC 0096).

## 2. Attori & ruoli

- **Tutti i ruoli del tenant**: vedono la panoramica (con le azioni filtrate dal proprio ruolo).
- **Sistema**: read-model esistenti — entitlement (UC 0077), subscription (UC 0028), quota (UC 0027), membri
  (UC 0059), stato 2FA (UC 0058/0017), documenti legali pendenti (UC 0056).

## 3. Precondizioni

Utente autenticato. Nessun nuovo endpoint obbligatorio: la Dashboard compone dati già esposti; se un'informazione
richiede un'aggregazione nuova, la change lo dichiara e la tiene minima.

## 4. Flusso principale

1. L'utente entra nel backoffice → **Dashboard**: saluto e nome del workspace.
2. **Avvisi azionabili** (solo se pertinenti, ordinati per gravità): 2FA non attiva → "Enable 2FA"; pagamento in
   sospeso → "Go to Billing"; documenti legali pendenti → rimando al flusso di ri-accettazione (se non già
   bloccante per rotta, UC 0056).
3. **Your apps**: una card per app attiva — tinta categoria, stato, **consumo quota** (barra usato/limite, in
   avviso oltre la soglia), azioni "Open" e "Manage plan" (→ Billing). Card finale "Get more apps" → catalogo.
4. **At a glance**: membri, inviti pendenti, app attive, prossimo rinnovo.
5. **Scorciatoie**: invita un membro (→ Members), pagamenti e ricevute (→ Billing), sfoglia il catalogo (→ App
   catalog).
6. In **Account** compare la sezione "Workspace ID": UUID in carattere mono + pulsante **Copy**; la Dashboard non
   lo mostra più.

## 5. Flussi alternativi / edge / errori

- **Workspace senza app attive**: la sezione app diventa un invito al catalogo (nessuna griglia vuota).
- **Guasto di una fonte** (es. quota non leggibile): degrada la singola card/sezione con stato di errore locale e
  riprova; il resto della Dashboard resta utile (mai pagina tutta rossa).
- **Member**: vede la panoramica; le azioni riservate (Manage plan, Invite) non gli sono offerte attive.
- **Entitlement non leggibili**: la sezione app segue il pattern errore-con-riprova di UC 0077.

## 6. Schermate & stati

Riferimento visivo vincolante: viste "Dashboard" e "Account" dell'artefatto approvato (change 0066) — avvisi a
striscia tinta funzionale; card app con barra quota; colonna "At a glance" + scorciatoie; Account con UUID mono e
Copy. Stati loading/empty/error/success per sezione.

## 7. Dati toccati

Nessun dato personale nuovo: composizione di read-model esistenti. L'UUID del workspace era già mostrato (cambia
solo pagina).

## 8. Permessi & gate

- Le azioni rispettano i ruoli esistenti (il frontend nasconde/disabilita, il backend fa fede).
- Invarianti: `tenant_id` solo dal JWT in ogni lettura; nessuna aggregazione cross-tenant; logging strutturato.

## 9. Requisiti di test

- Unit: composizione degli avvisi (presenza/ordine per gravità); derivazione della barra quota (soglie).
- L2 Playwright: dashboard con app attive + avvisi; workspace vuoto → invito al catalogo; guasto di una fonte →
  degradazione locale; Account mostra l'UUID con Copy e la Dashboard non lo mostra più.
- **Journey di piattaforma** (epica 20): J-REG atterra sulla nuova Dashboard (aggiornare le assert del journey di
  registrazione quando esiste); registro di copertura (UC 0093) quando disponibile.

## 10. Riferimenti & Definition of Done

- **Decisioni**: change 0066; UC 0077 (pattern stati); #03 (frontend).
- **DoD**: nuova Dashboard in locale; UUID solo in Account; degradazione per-sezione dimostrata nei test; i18n 5
  lingue; test verdi delle aree toccate; indici aggiornati dalla change.
