# Change 0066: Proposta UX — App catalog vs Billing (artefatto navigabile + user story)

**Branch**: `change/0066-app-catalog-billing-ux-proposal`
**Aree**: documentazione (`changes/`, `docs/usecases/`) — nessun codice eseguibile
**Data**: 2026-08-01
**Autore**: Marco Sindoni (autopilot)
**Use case sorgente**: Nessuno (change ad-hoc) — raccoglie il follow-up "vetrina dal catalogo reale" tracciato nei punti aperti di UC 0024
**Tocca dati personali?**: No (mockup con dati fittizi; nessun trattamento nuovo)

## Problema / Obiettivo

Oggi il backoffice mescola concetti che l'utente percepisce come distinti:

- **Dashboard** contiene solo l'UUID del tenant — informazione da pannello Account, non da pagina d'atterraggio;
- **Billing** ha come titolo "Get an app" e impila tre cose diverse: il catalogo delle app acquistabili, il
  pannello degli abbonamenti e (via checkout) l'acquisto — il commento nel codice stesso la dichiara provvisoria;
- lo **stato reale** delle app (attiva, pagamento in sospeso, disabilitata dalla piattaforma) non ha una casa unica
  e coerente (incoerenza tracciata in UC 0076).

Obiettivo: produrre una **proposta di riprogettazione navigabile** (artefatto HTML) che separi i tre concetti —
**App catalog** (scoprire e attivare le app), **Billing** (pagare e gestire ciò che è attivo), **Dashboard**
(panoramica operativa del workspace) — e, **dopo la sua approvazione esplicita**, le **user story** per
implementarla, formalizzate come use case nel catalogo.

## Scope

1. **Artefatto HTML navigabile** (`changes/0066-*/proposta-ux.html`, pubblicato anche come Artifact per la
   revisione; UI simulata in inglese, annotazioni di proposta in italiano) con:
   - **Nuova voce di menu "App catalog"** sotto Dashboard (gruppo PLATFORM): griglia di card (2–3 per riga su
     desktop, 1 su mobile) con immagine/illustrazione, titolo, descrizione breve, stato e azione contestuale
     (Subscribe se disponibile; altrimenti badge di stato + azione pertinente). **Ricerca** per filtrare le app e
     **paginazione** dell'elenco (dimostrate con ~12 app fittizie). Stati mostrati: disponibile, attiva, in prova,
     pagamento in sospeso, disdetta programmata, **disabilitata dalla piattaforma**.
   - **Billing ripulita**: solo pannello abbonamenti (cambio piano, disdetta, riattivazione) + storico
     pagamenti/ricevute; il titolo "Get an app" e la griglia di acquisto spariscono (l'acquisto parte dal
     catalogo e riusa il flusso di checkout esistente, UC 0024).
   - **Dashboard ripensata** (proposta): panoramica operativa — app attive con stato e consumo quota, avvisi
     azionabili (pagamento in sospeso, 2FA, documenti legali pendenti), scorciatoie. UUID del tenant spostato in
     **Account** (mostrato nell'artefatto).
   - Navigazione funzionante tra le viste, tema chiaro/scuro, dati finti.
2. **STOP di revisione dell'artefatto** (gate esplicito richiesto dallo sviluppatore).
3. **Solo dopo l'approvazione**: scrittura delle **user story come use case numerati** (skill `new-usecase`,
   area 06-frontend), col taglio implementativo che la proposta approvata determina.

## Fuori scope

- **Nessuna modifica al codice** del backoffice (React), dei servizi o dell'infrastruttura: questa change produce
  la proposta e le storie, non l'implementazione (che avverrà via `new-change` sui singoli use case).
- Il **fix dell'incoerenza Billing/app disabilitata**: tracciato nei punti aperti di UC 0076 (decisione 3 del
  registro), non si risolve qui — ma la proposta lo *prevede* (stato "disabilitata dalla piattaforma" nel catalogo).
- Riprogettazione del **flusso di checkout** (UC 0024): la proposta ne sposta solo il punto d'ingresso.
- Backend del catalogo reale (endpoint, immagini, descrizioni localizzate): sarà oggetto delle user story.

## Criteri di accettazione

- [ ] L'artefatto HTML esiste in `changes/0066-*/`, è navigabile (menu → Dashboard / App catalog / Billing /
      Account), mostra ricerca funzionante, paginazione funzionante, i 6 stati delle card e il tema chiaro/scuro.
- [ ] Billing nella proposta non contiene più alcun elemento di catalogo/acquisto; Dashboard non mostra più l'UUID
      (che compare in Account).
- [ ] Le user story sono scritte **solo dopo** l'approvazione esplicita dell'artefatto, come use case numerati nel
      catalogo con indice aggiornato.
- [ ] Registro `decisions.json` completo e coerente con questo documento.

## Invarianti appgrove toccati

Nessuno direttamente (nessun codice). La proposta deve però restare **implementabile dentro gli invarianti**: gli
stati delle card derivano dai read-model esistenti (`/me/entitlements` con la regola unica di accesso UC 0077,
`/me/subscriptions` UC 0028) — l'artefatto non deve inventare stati che il dominio non conosce.

## Requisiti di test (opzionale)

Non applicabile: solo documentazione/mockup. Le esigenze di test end-to-end della futura implementazione andranno
dichiarate nelle user story (in coerenza con l'epica 20, UC 0093/0094 quando disponibili).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A |
| Version bump | nessuno |
