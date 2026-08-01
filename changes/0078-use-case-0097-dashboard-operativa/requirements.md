# Change 0078: Dashboard operativa del workspace (+ Workspace ID in Account)

**Branch**: `change/0078-use-case-0097-dashboard-operativa`
**Aree**: `services/auth`, `frontend` (backoffice + catalogo traduzioni + moduli app), `tools/new-application`,
`tools/platform-e2e`, documentazione
**Data**: 2026-08-01
**Autore**: Platform Engineering (modalità **fast**, autopilot senza gate di workflow)
**Use case sorgente**: [docs/usecases/21-catalogo-app-backoffice/0097-dashboard-operativa.md](../../docs/usecases/21-catalogo-app-backoffice/0097-dashboard-operativa.md)
**Tocca dati personali?**: **No dati nuovi.** La pagina compone read-model già esistenti. L'unica informazione
nuova che circola è un **sì/no** sullo stato del secondo fattore dell'utente in sessione, letto dal servizio di
autenticazione: è un attributo di sicurezza dell'utenza, già memorizzato, che ora l'utente può vedere per sé
stesso. Nessuna nuova finalità, base giuridica, categoria o conservazione. Il gate privacy di step-03 gira
comunque.

## Problema / Obiettivo

La **Dashboard** è la pagina d'atterraggio del backoffice e oggi mostra **una sola cosa**: il nome dell'account
e l'identificativo tecnico del workspace (un codice esadecimale). È un segnaposto: non dice che cosa sta
succedendo nel workspace, non porta da nessuna parte e mette in primo piano l'unico dato della pagina che
l'utente non usa mai — l'identificativo serve solo quando si apre un ticket di assistenza.

Al termine di questa change la Dashboard è una **panoramica operativa**:

- un saluto e il nome del workspace;
- gli **avvisi azionabili** — solo quelli veri, ordinati per gravità, ciascuno con la sua azione;
- **Your apps**: una card per app attiva, con la tinta di categoria, lo stato e la **barra di consumo della
  quota** dell'app, più le azioni "Open" e "Manage plan"; l'ultima cella è l'invito al catalogo;
- **At a glance**: membri, inviti pendenti, app attive, prossimo rinnovo;
- **scorciatoie** verso Members, Billing e App catalog.

E l'**identificativo del workspace** si sposta nella pagina **Account**, in carattere a larghezza fissa e con un
pulsante di copia, dove serve davvero.

## Scope

### `services/auth` — lo stato del secondo fattore diventa leggibile

1. **Nuova lettura `GET /api/auth/2fa/status`** (autenticata, sull'utente del token) → `{ enabled: <sì/no> }`,
   implementata da entrambi i provider di identità: quello locale legge la propria riga di credenziali, quello
   Cognito legge le impostazioni di secondo fattore dell'utente. Serve perché **oggi il prodotto non sa** se un
   utente ha il secondo fattore attivo: l'invito ad attivarlo è un banner che si può solo chiudere a mano e che
   ricompare identico a chi l'ha già attivato. Un avviso permanente sulla pagina d'atterraggio che dice il falso
   sarebbe peggio dell'avviso che sostituisce.

### `frontend` — la Dashboard, la pagina Account e il descrittore di quota dei moduli

2. **Nuova Dashboard**, con quattro blocchi (intestazione, avvisi, app attive, colonna laterale) e
   **stati per-sezione**: caricamento, vuoto, errore con riprova. Un guasto di una fonte degrada **la sua sola
   sezione o card**: la pagina non diventa mai tutta rossa.
3. **Avvisi azionabili**, ordinati per gravità e mostrati **solo se pertinenti**:
   pagamento in sospeso (→ Billing), secondo fattore non attivo (→ Security), documenti legali aggiornati da
   rivedere (→ apre il flusso di ri-accettazione non bloccante già esistente).
4. **Card dell'app attiva**: tinta di categoria e iniziale (le stesse del catalogo), nome, badge di stato,
   **barra di consumo** `usato / limite` che passa in avviso oltre la soglia, azioni "Open" e "Manage plan".
   Ultima cella: invito tratteggiato **"Get more apps"** verso il catalogo. Con **zero app attive** la sezione
   è il solo invito al catalogo, non una griglia vuota.
5. **Consumo di quota**: il dato non esiste in un read-model di piattaforma — vive nel servizio di ciascuna app.
   Il manifest del modulo frontend guadagna quindi un **descrittore di quota facoltativo** (percorso della
   lettura + etichetta dell'unità); la Dashboard lo usa per la barra. Se il descrittore manca, o la lettura
   fallisce, o il ruolo non può leggerla, la card resta **senza barra** e senza errore di pagina.
6. **At a glance**: numero di membri, inviti pendenti, app attive, prossimo rinnovo. Le prime due righe compaiono
   **solo a chi può leggerle** (owner/admin): sono letture riservate, e mostrarle rotte a un member sarebbe
   peggio che non mostrarle.
7. **Scorciatoie**: invita un membro (owner/admin), pagamenti e ricevute, sfoglia il catalogo.
8. **Account**: nuova sezione **Workspace** con il nome del workspace, il **Workspace ID** in carattere a
   larghezza fissa e il pulsante **Copy** (con conferma visibile), più la nota che l'identificativo serve
   all'assistenza. La Dashboard **non lo mostra più**.
9. **Il banner di invito al secondo fattore sparisce dal guscio**: la sua ragione d'essere passa agli avvisi
   della Dashboard, che sono veri e non si chiudono finché il problema c'è. La pagina **Security** dice a sua
   volta la verità: se il secondo fattore è già attivo, lo annuncia invece di proporre di attivarlo.
10. **Traduzioni**: tutti i testi nuovi nelle 5 lingue del prodotto.

### `tools/new-application` — parità dei modelli

11. Il modello del manifest di modulo e i bundle di traduzione generati dichiarano il **descrittore di quota**:
    un'app nuova nasce con la sua barra di consumo in Dashboard, senza interventi a mano.

### Collaudo

12. Test unitari sulla logica **pura** (composizione e ordine degli avvisi, derivazione della barra di quota e
    della soglia, elenco delle app attive, prossimo rinnovo); test di componente sulla pagina; percorso
    end-to-end di livello 2 dedicato; estensione del percorso di piattaforma di registrazione (J-REG), che
    atterra sulla nuova Dashboard.
13. Registro di copertura end-to-end aggiornato: UC 0097 esce dalle esenzioni ed entra fra gli use case con
    superficie.

## Fuori scope

- **Nuove telemetrie o serie storiche**: nessun grafico di utilizzo nel tempo. Si mostra ciò che i read-model
  espongono oggi (UC 0097 §1).
- **Nuovo read-model aggregato nel core**: UC 0097 §3 chiede esplicitamente di comporre l'esistente e di tenere
  minima qualunque aggregazione nuova. Non ne serve nessuna.
- **Disattivazione del secondo fattore**: il servizio di autenticazione non la espone (rinvio già tracciato in
  UC 0058). Qui si legge lo stato, non lo si cambia.
- **Catalogo (UC 0095) e Billing (UC 0096)**: già consegnati, la Dashboard ci rimanda soltanto.
- **Danger zone del workspace** (eliminazione) presente nel mockup della change 0066: l'eliminazione del conto
  vive nella pagina "I miei dati" (UC 0033) e duplicarne il comando non è richiesto da UC 0097.

## Criteri di accettazione

- [ ] La Dashboard **non mostra più** l'identificativo tecnico del workspace; l'Account sì, in carattere a
      larghezza fissa e con un pulsante di copia che conferma di aver copiato.
- [ ] Un workspace con app attive vede una card per app, con stato e barra di consumo, e le azioni "Open" e
      "Manage plan"; l'ultima cella porta al catalogo.
- [ ] Un workspace **senza app attive** vede l'invito al catalogo, non una griglia vuota.
- [ ] Gli avvisi compaiono **solo** quando il problema esiste, in ordine di gravità, ciascuno con la sua azione.
- [ ] Con il secondo fattore **già attivo** nessun avviso lo propone — né in Dashboard né nel guscio — e la
      pagina Security lo dichiara attivo.
- [ ] Il guasto di **una** fonte (quota di un'app, catalogo, abbonamenti, membri) degrada solo la sua
      sezione/card: il resto della pagina resta utile e c'è una riprova.
- [ ] Un **member** vede la panoramica; non gli sono offerte le azioni riservate (cambio piano, invito) né le
      righe di riepilogo che non può leggere.
- [ ] Testi nuovi presenti in tutte e 5 le lingue.
- [ ] `./run-tests.sh` (suite completa) verde e registro di copertura end-to-end coerente.

## Invarianti appgrove toccati

- **`tenant_id` solo dal token verificato** — ogni lettura della Dashboard è un read-model `me/*` che ricava
  l'account dal contesto del chiamante; nessun parametro del client indica un account. La nuova lettura dello
  stato del secondo fattore lavora sul `sub` del token, non su un identificativo passato dal client.
- **Filtro riga per riga `WHERE tenant_id`** — invariato: nessuna query nuova lato piattaforma.
- **Modulo Terraform `microsaas_app`** — non toccato: nessuna infrastruttura nuova.
- **Logging strutturato** — la nuova lettura del servizio di autenticazione emette il proprio log con il
  soggetto del token.

## Requisiti di test

- **Unità (frontend, funzioni pure)**: composizione e **ordine per gravità** degli avvisi; nessun avviso quando
  non c'è nulla da dire; derivazione della barra di quota (percentuale, tetto illimitato, superamento, soglia di
  avviso); elenco delle app attive dal catalogo; prossimo rinnovo dal più vicino degli abbonamenti vivi.
- **Unità (servizio auth)**: lo stato del secondo fattore è falso prima dell'iscrizione e vero dopo la conferma.
- **Componente (frontend)**: Dashboard con app attive e avvisi; workspace vuoto; degradazione della sola card di
  quota; righe riservate assenti per un member; Account con identificativo e copia.
- **End-to-end livello 2**: percorso `L2-DASHBOARD` — panoramica con app e avvisi, workspace vuoto con invito al
  catalogo, guasto di una sola fonte, identificativo del workspace in Account e non in Dashboard.
- **Percorso di piattaforma**: J-REG atterra sulla nuova Dashboard e ne verifica i blocchi su stack vero.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — sola aggiunta lato servizio; lato interfaccia la Dashboard cambia contenuto e l'identificativo del workspace cambia pagina |
| Contratto cross-area | Sì — nuova lettura `GET /api/auth/2fa/status` (servizio di autenticazione, fuori dallo spec OpenAPI del core) e nuovo campo facoltativo nel manifest dei moduli frontend |
| Version bump | minor |
