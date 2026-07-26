# Change 0055: Localizzazione UI delle app alle 5 lingue del sito (shell i18n + modulo fatture)

**Branch**: `change/0055-use-case-0060-localizzazione-ui-app-multilingua`
**Aree**: `frontend/` (pacchetto `@appgrove/i18n`, shell `backoffice` + `admin`, modulo `fatture`) · `tools/new-application` (parità dei modelli di scaffolding)
**Data**: 2026-07-26
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: [`docs/usecases/06-frontend/0060-localizzazione-ui-app-multilingua.md`](../../docs/usecases/06-frontend/0060-localizzazione-ui-app-multilingua.md)
**Tocca dati personali?**: No — sono risorse di traduzione (frontend) e configurazione della lingua attiva; nessuna query tenant-scoped introdotta, nessun dato personale (UC 0060 §7). Lo scanner segnali privacy di step-03 gira comunque e ci si attende esito negativo.

## Problema / Obiettivo

La UI delle app è ferma a **2 lingue** (`it`/`en`) per la shell (pacchetto `@appgrove/i18n`) e a **solo italiano** per i moduli app, che non usano affatto l'i18n: il modulo `fatture` porta uno `strings.ts` cablato in italiano. Il sito vetrina è invece già a **5 lingue** (`en`/`it`/`fr`/`es`/`de`).

Questo disallineamento è emerso concretamente alla finalizzazione della landing dell'app #1 (change 0054): gli screenshot per-lingua escono tutti con la UI in italiano, perché l'app non è localizzata. Inoltre non esiste alcun rilevamento della lingua: la shell parte sempre da `en`, quindi anche una sessione di cattura in francese renderebbe la UI in inglese.

**Obiettivo**: portare la UI delle app alle stesse 5 lingue del sito e convertire il modulo `fatture` all'i18n, così che una sessione di navigazione in una data lingua renda **tutta** la UI (shell + modulo) in quella lingua. È la precondizione per screenshot landing per-lingua coerenti e allinea la parità linguistica dell'app a quella già garantita dal sito.

**Osservabile a fine change**: aprendo il backoffice con il browser impostato su `fr`/`es`/`de` (oltre a `it`/`en`), sidebar, topbar, stati vuoti/errore e l'intero modulo `fatture` (elenco, creazione, dettaglio, banner quota) compaiono nella lingua scelta, senza chiavi grezze a schermo; il selettore in topbar permette di passare fra le 5 lingue e la scelta persiste al ricaricamento.

## Scope

Tutto in area **frontend**, più l'aggiornamento in parallelo dei modelli di scaffolding.

1. **Pacchetto `@appgrove/i18n`** (`frontend/packages/i18n`)
   - Estendere le lingue supportate da `['en','it']` a `['en','it','fr','es','de']` e fornire i cataloghi completi `fr`/`es`/`de` (traduzione integrale delle chiavi oggi presenti in `en`/`it`, con parità verificata). La lingua predefinita resta `en`.
   - Introdurre il **rilevamento della lingua all'avvio**: ordine di scelta `preferenza salvata (localStorage) → lingua del browser (navigator.language, prime due lettere) → en`, sempre ristretta all'insieme supportato. La scelta manuale dell'utente viene **persistita** e riletta al ricaricamento.
   - Supportare la registrazione di **bundle di traduzione per-modulo** nell'istanza i18n (spazio-nomi con nome = id del modulo), così che i moduli app portino le proprie stringhe restando disaccoppiati dal catalogo della shell.

2. **Shell `backoffice` e `admin`** (`frontend/apps/backoffice`, `frontend/apps/admin`)
   - All'avvio l'istanza i18n parte dalla lingua rilevata (non più sempre `en`).
   - Il **selettore lingua** in topbar gestisce 5 lingue in modo leggibile (menu a tendina compatto al posto del controllo segmentato pensato per poche opzioni), in entrambe le app.
   - La shell **registra i bundle di traduzione dei moduli** visibili e risolve le **label di navigazione** (nome modulo, label di sezione) come chiavi i18n, così che la sidebar cambi lingua insieme al resto.

3. **Modulo `fatture`** (`frontend/apps/backoffice/src/modules/fatture`)
   - Sostituire `strings.ts` con bundle di traduzione per-modulo `i18n/{en,it,fr,es,de}.ts` (le ~40 stringhe attuali tradotte nelle 5 lingue) e convertire i componenti (schermate elenco/creazione/dettaglio, badge di stato, banner quota, manifest) a `useTranslation`.
   - Rendere l'helper di formattazione importo **consapevole della lingua** (il locale di `Intl.NumberFormat` deriva dalla lingua attiva; valuta invariata, EUR).
   - Aggiornare i test del modulo (`fatture.test.tsx`) e, dove tocca la resa delle label, il test della sidebar.

4. **Parità dei modelli di scaffolding** (`tools/new-application/templates/frontend-module/`)
   - Convertire il template del modulo frontend allo stesso pattern i18n (rimozione di `strings.ts`, bundle `i18n/*` con segnaposto, manifest/schermate/componenti via `useTranslation`), così che una nuova app nasca già localizzata e il collaudo di parità (`tools/scaffold-parity`, che confronta l'insieme dei file del modulo `fatture` reale con il template) resti verde. Gestire i segnaposto oggi contenuti in `strings.ts` (etichette quota) nel nuovo assetto.

5. **`run-tests.sh`**: aggiornare solo se il comando di test di un'area cambia (atteso: nessuna modifica strutturale; le aree `frontend` e `tooling` coprono già i test toccati).

## Fuori scope

- **Conversione dei moduli `crm` e `demo`** all'i18n: restano su `strings.ts` (in italiano) finché non migrati. UC 0060 ammette la migrazione incrementale per modulo. → tracciato in "Punti aperti / decisioni differite" di UC 0060.
- **Ri-esecuzione di `/finalize-landing fatture`** (ricattura dei 5 screenshot per-lingua e ripubblicazione della landing, DoD 3 di UC 0060): è un flusso della skill dedicata, con asset binari e senza il gate test/snapshot di new-change, ed è eseguibile solo **dopo** il merge (richiede l'app che serve la UI localizzata). Questa change consegna e verifica la capacità; la ricattura è il sotto-passo di chiusura post-merge. → tracciato in UC 0060.
- **Localizzazione dei contenuti della landing** (già a 5 lingue, UC 0038/0053) e delle **email di autenticazione** (UC 0018): esclusi da UC 0060.
- **Backend / servizi / infra**: nessuna modifica. Nessuna nuova query, nessun dato personale, nessun cambio d'infrastruttura.

## Criteri di accettazione

- [ ] `@appgrove/i18n` espone `['en','it','fr','es','de']` e fornisce i cataloghi `fr`/`es`/`de` completi; il test di parità delle chiavi verifica **tutte e 5** le lingue (nessuna chiave mancante o in eccesso) e la parità è imposta anche a compilazione dal tipo delle risorse.
- [ ] Aprendo il backoffice con lingua del browser `fr`/`es`/`de` (oltre a `it`/`en`), shell (sidebar, topbar, stati vuoti/errore) e modulo `fatture` (elenco, colonne, stati fattura, editor, dettaglio, banner quota, messaggi d'errore) sono resi in quella lingua, **senza chiavi grezze** a schermo.
- [ ] La lingua attiva all'avvio deriva da `localStorage → navigator.language → en`; il selettore in topbar (5 lingue) cambia lingua e la scelta **persiste** al ricaricamento; il cambio lingua aggiorna anche le label di navigazione della sidebar.
- [ ] Il modulo `fatture` non contiene più `strings.ts`: le stringhe vivono nei bundle per-modulo `i18n/{en,it,fr,es,de}.ts` e gli importi sono formattati secondo la lingua attiva.
- [ ] Il template `tools/new-application/templates/frontend-module/` è convertito allo stesso pattern e il collaudo di parità dello scaffolding è **verde** (oppure ogni residuo è registrato come deviazione consapevole in `docs/_PARITA-SCAFFOLD.md` con il percorso di riallineamento).
- [ ] Le suite delle aree toccate sono verdi via `run-tests.sh` (almeno `frontend` e `tooling`).

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT** / **filtro row-level** / **modulo Terraform `microsaas_app`**: **non toccati** — la change è solo frontend (risorse di traduzione e configurazione lingua), nessuna query né infrastruttura.
- **Logging strutturato**: non pertinente (nessun log backend introdotto).
- Invariante di prodotto rilevante qui — **"una nuova app gira subito in locale"** (CLAUDE.md, "Avvio locale di nuove app"): l'aggiornamento del template di scaffolding deve preservarla, cioè una app appena generata deve rendere la propria UI localizzata senza passi manuali. Ed è mantenuta la **parità dei modelli di scaffolding** (CLAUDE.md, "Parità dei modelli di scaffolding"): convertendo `fatture` si converte in parallelo il template.

## Requisiti di test

- **Parità i18n a 5 lingue**: il test del pacchetto `@appgrove/i18n` itera su **tutte** le lingue supportate (non più `en`/`it` cablate) e verifica identità dell'insieme delle chiavi; aggiornare l'asserzione sull'elenco lingue.
- **Parità delle stringhe per-modulo**: un test verifica che i bundle di traduzione del modulo `fatture` coprano le stesse chiavi in tutte e 5 le lingue.
- **Rilevamento lingua**: test dell'ordine `preferenza salvata → lingua browser → en` e della persistenza della scelta manuale.
- **Resa del modulo nelle 5 lingue**: i test del modulo `fatture` verificano che le schermate rendano stringhe localizzate (nessuna chiave grezza) in più di una lingua, e che l'importo sia formattato secondo la lingua attiva.
- **Resa delle label di navigazione**: il test della sidebar verifica che nome modulo e label di sezione seguano il cambio lingua.
- **Regressione scaffolding**: il collaudo di parità (`tools/scaffold-parity`) resta verde dopo la conversione del template.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No per l'utente finale. Internamente cambia il **contratto `ModuleManifest`** (le label di navigazione diventano chiavi i18n e il manifest espone i bundle di traduzione): impatta solo i moduli del monorepo, aggiornati nella stessa change (fatture) o retro-compatibili (crm/demo continuano a passare stringhe già localizzate). |
| Contratto cross-area | N/A (tutto interno al frontend; nessun contratto frontend ↔ API di servizio toccato). |
| Version bump | minor (nuova capacità: 3 lingue in più e localizzazione del modulo fatture; nessuna rottura esterna). |
