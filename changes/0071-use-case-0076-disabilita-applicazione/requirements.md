# Change 0071: Disabilita applicazione (feature admin reversibile)

**Branch**: `change/0071-use-case-0076-disabilita-applicazione`
**Aree**: `services/core` (catalogo, console admin, read-model abbonamenti, retention) · `frontend/apps/admin` ·
`frontend/apps/backoffice` · `frontend/packages/i18n` · `frontend/packages/api-client` · docs
**Data**: 2026-08-01
**Autore**: Platform Engineering (modalità fast, autopilot)
**Use case sorgente**: [docs/usecases/15-supporto-e-piattaforma/0076-disabilita-applicazione.md](../../docs/usecases/15-supporto-e-piattaforma/0076-disabilita-applicazione.md)
**Tocca dati personali?**: No — nessun nuovo trattamento di dati degli utenti finali. L'unico identificativo
registrato è quello dell'**operatore di piattaforma** (`sub` del token), già trattato per finalità di sicurezza e
tracciabilità come in ogni altra riga di audit esistente. La **motivazione** scritta dall'operatore è testo libero:
per prudenza resta **solo nel database** (retention 12 mesi) e **non** viene mai messa nel log strutturato di audit,
che finisce nell'archivio a 12 mesi — stessa regola già applicata alla nota della limitazione art. 18.

## Problema / Obiettivo

Serve una **leva reversibile** che renda un'applicazione del marketplace indisponibile a **tutti** gli account
insieme, senza toccare dati né infrastruttura, e che si annulli riportando l'app disponibile. Oggi metà del
meccanismo esiste già ma è incompleta:

- **c'è**: il campo `app.status` nel catalogo, l'endpoint `PATCH /api/platform/v1/admin/apps/{id}` riservato a
  `platform-admin`, il pulsante Abilita/Disabilita con dialogo di conferma nella console admin, e soprattutto
  l'**applicazione della regola** a valle (`EntitlementAccess`/`EntitlementReadModel`, UC 0027/0077: un'app non
  `active` non concede accesso a nessuno);
- **manca**: la **motivazione** dell'azione, l'**audit persistito e consultabile** delle transizioni (chi, quando,
  quale app, da quale stato a quale, perché), l'**idempotenza** esplicita del toggle, un **copy** che distingua in
  modo inequivocabile questa pausa reversibile dalla dismissione definitiva (skill `drop-application`, UC 0048), e
  la coerenza dell'esperienza utente: la pagina Billing del backoffice mostra oggi l'abbonamento a un'app
  disabilitata come se nulla fosse (punto aperto già annotato nello use case, di proprietà di questo use case).

Al termine il fondatore deve poter mettere in pausa un'app, dire perché, vederlo scritto in un registro, e
riattivarla — e l'utente finale deve capire cosa sta succedendo invece di vedere un abbonamento "attivo" per
un'app che non c'è più nella sua barra laterale.

## Scope

**Backend (`services/core`)**

1. **Motivazione dell'azione**: il corpo del `PATCH /admin/apps/{id}` accetta un campo facoltativo con la
   motivazione (testo libero, lunghezza massima limitata). L'assenza del campo resta valida — i chiamanti esistenti
   (compresa la suite end-to-end di piattaforma, che usa la leva in `global-setup`) non si rompono.
2. **Audit persistito**: ogni **transizione effettiva** di `app.status` scrive **una** riga in una nuova tabella
   di piattaforma con app, stato di partenza, stato di arrivo, operatore, motivazione, istante. Nuova migrazione
   Flyway, sullo stesso modello delle tabelle di prova già esistenti (`gdpr_restriction_audit`).
3. **Idempotenza**: chiedere lo stato in cui l'app già si trova **non** è un errore e **non** scrive audit — la
   risposta è la vista corrente dell'app. Nessuna riga di audit senza cambio di stato.
4. **Errori tipizzati**: stato non ammesso → 400 *problem+json*; app inesistente o cancellata → 404 *problem+json*;
   chiamante senza ruolo `platform-admin` → rifiuto senza alcun cambio di stato.
5. **Registro consultabile**: nuova lettura riservata a `platform-admin` che restituisce il registro delle
   transizioni, più recenti prima, con il nome/slug dell'app risolto.
6. **Retention**: le righe del nuovo registro seguono la stessa conservazione a 12 mesi delle altre prove di audit
   nel database (UC 0035), tramite il job già esistente.
7. **Coerenza per l'utente finale**: il read-model degli abbonamenti (`/me/subscriptions`, UC 0028) espone se
   l'app dell'abbonamento è **disabilitata a livello di piattaforma**. Continua a elencare l'abbonamento (per
   disegno mostra anche i non-attivi), ma dice la verità sul motivo per cui l'app non è raggiungibile.
8. **Logging strutturato** dell'azione con `app_id`, operatore ed esito (già presente, va mantenuto e completato
   con la transizione).

**Frontend — console admin (`frontend/apps/admin`)**

9. La sezione **App** ospita nel dialogo di conferma un **campo motivazione facoltativo** e un copy che spiega
   l'effetto per esteso: indisponibile a **tutti** gli account, **reversibile**, **i dati restano intatti**,
   l'effetto pieno si vede alla **successiva lettura degli entitlement**, e **non** è la dismissione definitiva
   dell'app.
10. Sotto la tabella delle app compare il **registro delle disabilitazioni** (app, azione, operatore, quando,
    motivazione), con i suoi stati di caricamento/errore/vuoto.
11. Il badge di stato dell'app resta la fonte visiva immediata e si aggiorna dopo l'azione.

**Frontend — backoffice (`frontend/apps/backoffice`)**

12. La pagina **Billing** segnala sull'abbonamento di un'app disabilitata dalla piattaforma un avviso esplicito
    ("app sospesa dalla piattaforma, l'abbonamento resta valido e i dati sono intatti"), così che la sidebar che
    correttamente non mostra l'app e il pannello abbonamenti raccontino la stessa storia.

**Trasversale**

13. Testi in **5 lingue** (it/en/es/fr/de) per tutto ciò che è nuovo.
14. Contratto OpenAPI rigenerato e tipi del client frontend riallineati.
15. Copertura di test: unità/integrazione backend, componenti frontend, **end-to-end Playwright L2** sia sulla
    console admin sia sul backoffice.
16. Indice di esecuzione delle storie evo (`docs/usecases/EPICS-WAVE-2.md`): 0076 → ✅.

## Fuori scope

- **Rinominare lo stato** `inactive` in `disabled`: il valore vive già nello schema, nei dati, nel pricing-as-code
  e nella suite end-to-end di piattaforma; la parola "disabilitata" resta nel **copy** dell'interfaccia.
- **Kill-switch all'edge**: l'authorizer non legge il database, le richieste verso un'app disabilitata continuano
  ad arrivare al servizio, che le respinge con l'enforcement esistente. Rinuncia consapevole, già scritta nei punti
  aperti dello use case.
- **Disabilitazione per singolo account**, **comunicazione automatica agli utenti impattati**, **finestra di
  manutenzione programmata**: restano punti aperti dello use case 0076, non maturi qui.
- **Dismissione definitiva** di un'app (rimozione di servizio, modulo, dati, infrastruttura): è UC 0048, skill
  `drop-application`.
- **Journey end-to-end di piattaforma lato admin** (browser vero contro stack vero): è UC 0092, la storia
  immediatamente successiva, di cui questa è prerequisito. Qui si copre il livello L2 con le superfici mockate.
- **Annullamento o rimborso** degli abbonamenti su un'app disabilitata: gestione commerciale, fuori dallo use case.
- Qualunque modifica a dati di account, di abbonamento o applicativi, e qualunque modifica infrastrutturale.

## Criteri di accettazione

- [ ] Da `platform-admin`, disabilitare un'app scrive `app.status = inactive`, registra **una** riga di audit con
      operatore, istante, stato di partenza e di arrivo, e la motivazione se fornita; riabilitare fa il simmetrico.
- [ ] Ripetere l'azione sullo stato in cui l'app già si trova risponde 200 senza scrivere nulla: nessun cambio di
      stato, **nessuna** riga di audit aggiuntiva.
- [ ] App inesistente → 404 *problem+json*; stato non ammesso → 400 *problem+json*; nessun ruolo diverso da
      `platform-admin` può cambiare lo stato (rifiuto e nessuna scrittura).
- [ ] Con l'app `inactive`, la lettura degli entitlement dell'account di prova **non** la include; tornata `active`,
      la include di nuovo (raccordo con UC 0027 verificato su database reale).
- [ ] La console admin mostra il registro delle transizioni con motivazione, e il dialogo di conferma ospita il
      campo motivazione e il copy che distingue la pausa reversibile dalla dismissione definitiva.
- [ ] La pagina Billing del backoffice segnala l'app disabilitata dalla piattaforma invece di mostrare solo
      "abbonamento attivo".
- [ ] Le righe del registro oltre 12 mesi sono eliminate dal job di conservazione, quelle recenti no.
- [ ] `./run-tests.sh` (suite completa) verde.

## Invarianti appgrove toccati

- **`tenant_id` solo dal JWT verificato**: l'operatore è identificato dal `sub` del token (`CallerContext`), mai da
  parametri di richiesta. Il registro non accetta né usa identificativi di tenant presi dal corpo.
- **Filtro row-level `WHERE tenant_id`**: la scrittura riguarda il **catalogo di piattaforma**, che non è
  tenant-scoped; nessuna riga di account viene letta o scritta. L'eccezione cross-account della console admin
  resta quella già documentata e gated `platform-admin`. Il read-model `/me/subscriptions` continua a leggere le
  subscription tenant-scoped e aggiunge solo un dato di catalogo (platform-level, non tenant-scoped).
- **Modulo Terraform `microsaas_app`**: nessuna modifica infrastrutturale, l'invariante non è in gioco.
- **Logging strutturato**: l'azione emette un log con `app_id`, transizione ed operatore, più l'evento di audit
  `admin.app.status-changed` con soli identificativi opachi (mai la motivazione in chiaro).

## Requisiti di test

- **Unità/integrazione backend** (database reale): transizione in entrambi i versi con riga di audit; idempotenza
  senza audit; motivazione persistita e troncamento/validazione della lunghezza; 400 su stato non ammesso; 404 su
  app inesistente; rifiuto senza ruolo `platform-admin`; lettura del registro ordinata dal più recente; raccordo
  entitlement (disabilitata → esclusa, riabilitata → inclusa); retention a 12 mesi della nuova tabella.
- **Frontend (vitest)**: dialogo con campo motivazione che finisce nel corpo della richiesta; registro reso in
  tabella; avviso "app disabilitata" sulla card abbonamento del backoffice.
- **End-to-end Playwright L2**: console admin — disabilita con motivazione → badge aggiornato → la motivazione
  compare nel registro → riabilita → il registro mostra entrambe le transizioni. Backoffice — abbonamento su app
  disabilitata → l'avviso è visibile.
- **Non regressione**: la suite end-to-end di piattaforma (`tools/platform-e2e`) usa il `PATCH` senza motivazione
  in `global-setup`; deve continuare a funzionare invariata.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — il campo motivazione è facoltativo, il campo aggiunto al read-model abbonamenti è additivo |
| Contratto cross-area | Sì — OpenAPI del core → client frontend (nuovo endpoint di lettura registro, campo motivazione, campo "app disabilitata" su `/me/subscriptions`) |
| Version bump | minor |
