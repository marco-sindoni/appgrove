# 0082 — Log di implementazione

**Use case**: 0067 (gestione abbonamento self-service, sezione "Abbonamenti" del backoffice) · **Modalità**: fast · **Branch**: `change/0082-use-case-0067-abbonamento-self-service`
**Registro strutturato delle scelte**: [`decisions.json`](decisions.json) (14 voci) · **Verifica manuale**: [`how-to-test.md`](how-to-test.md)

## Punto di partenza

La sezione esisteva già — UC 0028 ne ha costruito il motore (read-model `/me/subscriptions` e i quattro comandi:
cambia piano, disdici, riattiva, apri portale), UC 0096 l'ha collocata nella pagina *Fatturazione* — ma era una
lista, non un pannello di governo: piano, una riga di stato, i tetti teorici del piano, e per cambiare piano un
riquadro di pulsanti nudi con due finestre di conferma del browser. Questa change non ha aggiunto un'azione: ha reso
possibile **decidere** prima di compierla.

## Cosa è cambiato

### 1. Il read-model dice due cose in più (`services/core`)

`SubscriptionView` acquisisce `usage` (uso corrente per metrica) e `blockedTiers` (piano → spiegazione del rifiuto).

Il primo è la giacenza reale già proiettata in `platform.app_usage_stock` (UC 0054), la stessa che il comando usa per
rifiutare una riduzione: renderla visibile trasforma "il piano prevede 10 posti" in "ne stai usando 8 su 10".

Il secondo è il pezzo che mancava perché la finestra di scelta potesse **disabilitare** un piano troppo piccolo invece
di lasciarlo cliccabile e mostrare un rifiuto dopo. È calcolato con `TierChangePolicy.evaluateDowngrade` — la stessa
regola che governa il 409 — su tutti i piani dell'app: un piano capiente non è mai bloccato, quindi non serve
filtrare per direzione, e il frontend non deve conoscere la regola. Alternativa scartata: ricalcolarla in TypeScript
incrociando uso e limiti (due verità che divergono alla prima modifica).

Nel passaggio, la conversione del JSON `app_tier.limits` — che esisteva **due volte identica** — è stata unificata in
`TierChangePolicy.limitsOf`. I chiamanti sarebbero diventati tre.

Contratto: `openapi.yaml`/`openapi.json` rigenerati dalla build Quarkus, `frontend/packages/api-client/src/schema.ts`
rigenerato da quelli. Campi aggiuntivi e facoltativi: nulla si rompe a valle.

### 2. La sezione, riscritta dove serviva (`frontend/apps/backoffice`)

| Prima | Ora |
|---|---|
| riga "Caricamento…" | scheletro di due card con la forma di quelle vere |
| errore senza rimedio | avviso con **Riprova** che rilancia la lettura |
| tetti teorici del piano | **"8 su 10 posti"** con barra e avviso a 80% / al limite, dove l'uso è misurato |
| riquadro di pulsanti nudi | **finestra di cambio piano**: prezzo per ciclo, limiti, piano attuale marcato, "consigliato" sul primo superiore, piani non ammissibili disabilitati **con la spiegazione**, scelta mensile/annuale |
| `window.confirm` | conferma nell'applicazione che dice **cosa succede e da quando** ("passi a Basic dal 1/1/2027", "l'accesso resta fino al …") |
| ciclo fisso `monthly` | il ciclo scelto nella finestra è quello inviato nel comando |
| nessun segnale dopo il comando | **"aggiornamento in corso"** finché il read-model non riflette il cambiamento |
| bollino "Pagamento in sospeso" | **avviso persistente** di pagamento in ritardo, con il pulsante che apre il portale |
| pulsanti sparsi | **avviso di scadenza** che spiega che i dati restano, accanto a riattiva ed esporta/elimina |
| azioni sempre attive | azioni di fatturazione **disabilitate** a chi non è titolare, con la ragione scritta |

Due scelte meritano una riga. Lo **stato transitorio** non si fida della risposta del comando: il modello è
comando → fornitore → webhook → read-model, quindi si fotografano i tre campi che un comando fa cambiare (piano,
riduzione programmata, disdetta) e si rilegge ogni 1,5 s finché uno non cambia — smettendo comunque dopo 30 s, perché
insistere per sempre è un modo elegante di mentire. Il **"consigliato"** non è un dato nuovo di catalogo (sarebbe una
scelta commerciale, non nostra): è il primo piano di prezzo superiore all'attuale, derivato dai prezzi già esposti.

Il guscio della finestra modale (sovrapposizione, `role="dialog"`, Escape) è stato estratto in `shell/Modal.tsx` e
`pages/members/ConfirmDialog.tsx` riscritto sopra di esso, invece di copiarlo: due gestioni del focus divergono alla
prima correzione.

Tutte le diciture nuove esistono nelle 5 lingue; le due chiavi delle vecchie conferme del browser sono state rimosse.

### 3. Prove

- **`services/core`** — `SubscriptionUsageReadModelTest` (nuovo, 4 prove): uso esposto e piano troppo piccolo marcato
  bloccato con la spiegazione; senza riporti d'uso né uso né divieti; metriche a finestra mai bloccanti; la giacenza
  di un tenant non compare nel read-model di un altro.
- **`frontend`** — `SubscriptionsPanel.test.tsx` (nuovo, 14 prove) sul comportamento visibile; `subscriptionsView.test.ts`
  esteso a 25 prove sulla logica pura (consumo, opzioni di piano, direzione, riepilogo, stato transitorio).
- **End-to-end L2** — `e2e/subscriptions.spec.ts` da 3 a **7** prove `[L2-SUB]`; la prova di riduzione è stata riscritta
  sul nuovo percorso a due passi (scelta, poi conferma), non più su una finestra del browser.
- **Registro di copertura** — 0067 esce dalle esenzioni `non-implementato`, entra fra gli use case con superficie ed è
  collegato a `L2-SUB`; `tools/e2e-coverage` verde.

**Gate privacy (UC 0031)**: scanner eseguito, **nessun segnale**. I due campi nuovi sono dati contrattuali derivati
(giacenza d'uso e tetti del piano), non dati personali nuovi: nessuna finalità nuova, nessuna base giuridica nuova,
nessun nuovo responsabile esterno. Classificazione **non applicabile** (né MAJOR né MINOR).

## Cosa resta fuori, e dove è scritto

- **Consumo delle metriche "a finestra"** (quelle che si azzerano ogni periodo): in `core` esiste solo la proiezione
  della giacenza, quindi per quelle metriche la card mostra il solo tetto. Tracciato nei **punti aperti di UC 0067**,
  insieme alla domanda che va decisa per chiuderlo (un contatore a finestra si riporta azzerato o cumulativo?).
- **Pausa/ripresa** → UC 0068. **Prova gratuita una-tantum** → UC 0069. **Incassato netto** → UC 0071.
- **Fornitore Paddle reale**: i metodi restano non implementati finché non esiste l'account (bloccato da #14); vale lo
  stub locale, che emette i webhook sintetici da cui passa tutta la riconciliazione.

## Verde prima del commit

`./run-tests.sh` **senza parametri** (tutte le aree), come impone la modalità fast.
