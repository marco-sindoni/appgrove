# UC 0060 — Localizzazione UI delle app alle 5 lingue del sito (shell i18n + moduli)

**Area**: 06-frontend · **Fase**: 6 · **Stato**: 🔴 da scrivere
**Dipendenze**: UC [0020](0020-shell-backoffice.md) (shell + i18n, oggi 2 lingue), UC [0052](../11-apps/0052-app1-modulo-frontend.md) (modulo app #1), UC [0037](../09-marketing-site/0037-homepage-nav-footer.md) (le 5 lingue del sito come insieme obiettivo)
**Fonte decisioni**: `docs/_BACKLOG.md` (standardizzazione EN/IT dei moduli app, qui ampliata a 5 lingue), change `0054-use-case-0053-…` (divario emerso: screenshot landing non localizzati)
**Ultimo aggiornamento**: 2026-07-25

> Use case scaffoldato a valle della change 0054 (UC 0053): la finalizzazione della landing dell'app #1 ha
> mostrato che gli screenshot escono con la UI in italiano in tutte le lingue, perché **l'app non è
> localizzata**. Compilare le sezioni; cancellare questa nota a stesura conclusa.

## 1. Obiettivo / Scope
Portare la **UI delle app** (shell backoffice + moduli app) alle **5 lingue del sito** (`en/it/fr/es/de`), oggi
ferma a 2 (`it/en`) per la shell e a **solo italiano** per i moduli (es. `fatture` usa `strings.ts` cablato, non
l'i18n). Sblocca gli **screenshot per-lingua coerenti** della landing (UC 0053) e allinea la UI dell'app alla parità
linguistica già garantita dal sito vetrina.
**Incluso**: estensione del pacchetto `@appgrove/i18n` (`frontend/packages/i18n`) a `fr/es/de`; conversione dei moduli
app dall'uso di `strings.ts` all'i18n; selezione della lingua guidabile dall'ambiente di cattura (locale del browser /
preferenza) così che `finalize-landing` produca figure localizzate; ri-esecuzione di `/finalize-landing fatture` per
gli screenshot per-lingua.
**Escluso**: la localizzazione dei **contenuti** della landing (già a 5 lingue, UC 0038/0053); le email auth (UC 0018).

## 2. Attori & ruoli
Utente dell'app (B2C/B2B) che naviga nella propria lingua; sistema di cattura screenshot (`finalize-landing`);
sviluppatore/traduttore che rivede le stringhe.

## 3. Precondizioni
Shell (UC 0020) e almeno un modulo app (UC 0052) esistenti; sito a 5 lingue (UC 0037) come insieme obiettivo.

## 4. Flusso principale
1. Estendere le risorse i18n della shell da `it/en` a `en/it/fr/es/de` (`frontend/packages/i18n/src/resources/`).
2. Convertire i moduli app (a partire da `fatture`) dall'uso di `strings.ts` a chiavi i18n, con traduzioni nelle 5 lingue.
3. Far derivare la lingua attiva dell'app da una fonte controllabile dalla cattura (locale del browser / preferenza),
   così che una sessione in `fr` renda la UI in francese.
4. Ri-eseguire `/finalize-landing fatture`: il generatore (già per-lingua) produce ora 5 screenshot **coerenti** con la
   lingua di navigazione; ripubblicare gli asset.

## 5. Flussi alternativi / edge / errori
- **Chiave mancante in una lingua** → fallback a EN (sorgente), segnalato dai test di parità i18n.
- **Modulo non ancora convertito** → resta in italiano finché non migrato; migrazione incrementale per modulo.

## 6. Schermate & stati
Shell e moduli resi nelle 5 lingue (sidebar, header, tabelle, form, banner quota, stati empty/error). Screenshot
landing per-lingua reali dopo la ri-finalizzazione.

## 7. Dati toccati
Nessun dato applicativo: sono risorse di traduzione (frontend) + configurazione della lingua attiva. Nessun dato personale.

## 8. Permessi & gate
N/A (UI/i18n). Nessuna query tenant-scoped introdotta.

## 9. Requisiti di test
- **Parità i18n**: ogni chiave presente in tutte e 5 le lingue (test del pacchetto i18n, sullo schema della parità già
  usata per i contenuti del sito).
- **Render dei moduli** nelle 5 lingue (nessuna chiave grezza a schermo).
- **Screenshot**: ri-cattura via `finalize-landing` con verifica visiva della coerenza di lingua.

## 10. Riferimenti & Definition of Done
- **Decisioni**: `docs/_BACKLOG.md` (standardizzazione moduli), change `0054-use-case-0053-…` (divario screenshot), #14 (5 lingue sito).
- **DoD**:
  1. `@appgrove/i18n` copre `en/it/fr/es/de` con parità delle chiavi verificata.
  2. Modulo `fatture` (e gli altri moduli app) resi via i18n, nessuna stringa cablata residua.
  3. `/finalize-landing fatture` ri-eseguita: 5 screenshot con UI coerente alla lingua; landing ripubblicata.
  4. Test verdi (parità i18n + render moduli); `run-tests.sh` aggiornato se serve.
