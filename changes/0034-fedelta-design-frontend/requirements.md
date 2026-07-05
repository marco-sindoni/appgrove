# Change 0034: Fedeltà al design di riferimento (backoffice + admin)

**Branch**: `change/0034-fedelta-design-frontend`
**Aree**: frontend
**Data**: 2026-07-05
**Autore**: Platform Engineering
**Use case sorgente**: Nessuno (change ad-hoc — affinamento visivo di UC 0019/0020/0021 già implementati)
**Tocca dati personali?**: No — solo presentazione (stili, layout, icone); nessun dato nuovo raccolto o trattato.

## Problema / Obiettivo

L'implementazione reale delle due SPA (`frontend/apps/backoffice` e `frontend/apps/admin`) è visivamente
lontana dai mockup di riferimento in `docs/frontend-design/` (consumer: `v1/Appgrove.dc.html`; admin:
`admin/v1/AppgroveAdmin.dc.html`): padding e spaziature non rispettati, icone mancanti, gerarchie
tipografiche piatte, menu di secondo livello senza indentazione/riga verticale/font ridotto, tabelle e
badge di stato meno curati. Obiettivo: **ogni schermata esistente deve sembrare uscita dal mockup** —
i mockup sono la fonte di verità visiva e strutturale.

## Scope

Solo `frontend/` (le due SPA + `packages/design-system`). Le correzioni si fanno **alla radice** quando
possibile (token, primitivi e componenti condivisi del design system) e nelle shell/schermate delle due
app per la struttura. In particolare, per le schermate già esistenti di **entrambe** le app:

- spaziature e padding (sidebar, header di pagina, card, tabelle, form) allineati al mockup;
- tipografia: dimensioni, pesi e gerarchie (titolo pagina + sottotitolo, label sezioni sidebar, font
  ridotto per voci di secondo livello, numeri tabellari/monospace dove il mockup li usa);
- icone: presenti dove il mockup le prevede (voci di menu, header di pagina, azioni, stati vuoti);
- menu laterale: struttura a due livelli fedele — voce app espandibile, secondo livello indentato con
  riga verticale di evidenza, stato attivo/hover come da mockup; sezioni "PLATFORM" / "YOUR APPS";
- tabelle: header, righe, allineamenti, avatar/iniziali cliente, badge di stato colorati come da mockup;
- componenti trasversali: pulsanti (primario/secondario), breadcrumb, card, banner, tema scuro coerente.

## Fuori scope

Componenti del mockup che richiedono **dati o comportamenti nuovi**: ricerca globale (⌘K), schede
riassuntive con aggregati (Outstanding/Paid/…), tab di filtro sulle liste, selettore workspace, badge
numerici sulle voci di menu. Restano fuori e vengono **tracciati come punti aperti** negli use case che
li possiedono (gate delle decisioni differite, verificato a step-04). Nessuna funzionalità nuova, nessuna
modifica a servizi/API/infra.

## Criteri di accettazione

- [ ] Confronto affiancato mockup ↔ app (stack locale) sulle schermate principali di backoffice e admin:
      layout, spaziature, tipografia, icone e menu corrispondono al linguaggio visivo del mockup.
- [ ] Menu laterale backoffice: voce app espandibile con secondo livello indentato, riga verticale,
      font ridotto e stato attivo come nel mockup (verificabile su "Fatture").
- [ ] Le correzioni condivise vivono in `@appgrove/design-system` (token/primitivi), non duplicate
      per-app; nessuna regressione: `npm test` verde e build/typecheck ok.
- [ ] Baseline visive Playwright (UC 0029) ri-registrate **deliberatamente** con motivazione nel log di
      implementazione (mai alla cieca — hook #10 F).

## Invarianti appgrove toccati

Nessuno direttamente (change solo presentazionale). Il design system resta la fonte unica di brand & UI
(zero drift tra le due app), coerente con docs/03-frontend.md.

## Requisiti di test

- Suite frontend esistente verde (`npm test` dal workspace `frontend/`, inclusi e2e L2 Playwright).
- Le baseline visive cambieranno per definizione: ri-registrazione consapevole, con diff ispezionati.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A |
| Version bump | patch |
