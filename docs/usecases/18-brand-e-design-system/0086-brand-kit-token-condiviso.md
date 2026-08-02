# UC 0086 — Pacchetto brand kit / token condiviso nel monorepo

**Area**: 18-brand-e-design-system · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0019 (design system & brand kit), UC 0036 (sito vetrina Astro), UC 0046 (landing generate da new-application)
**Fonte**: R20 (Tabella residui _INDEX.md) · docs/_BACKLOG.md §"Brand kit & design system (#14 F)"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Estrarre i **token visivi** (i valori di base dell'identità: colori, tipografia, dimensioni, forme) dai mockup di
riferimento e confezionarli in un **unico pacchetto condiviso** del monorepo, così che esista **una sola fonte** per
l'aspetto grafico di tutti i prodotti appgrove. Oggi i token vivono già dentro il design system della prima applicazione
web — l'interfaccia utente scritta in React, che chiamiamo per brevità SPA, cioè "applicazione a pagina singola" — ma
**non** sono ancora un pacchetto separato consumato anche dal sito vetrina e dalle pagine di presentazione (landing)
generate automaticamente. Questo use case chiude proprio quel divario.

**Incluso**: colori con tema chiaro e scuro (palette accento corallo `#ec5a72`, neutri caldi, colori-categoria per-app);
la scala tipografica (type scale) con carattere **Plus Jakarta Sans** per il testo e **JetBrains Mono** per il testo a
spaziatura fissa/monospazio; raggi degli angoli (radii), ombre e spaziature (spacing); il **logo** nelle versioni chiara e
scura; il set di icone **Material Symbols**; una **nota sullo stile delle illustrazioni** (che rimanda a UC 0087 per la
produzione degli artwork).
**Escluso**: la produzione dell'artwork del logo finale e delle illustrazioni — è UC 0087 (Artwork logo finale +
illustrazioni). Escluse anche le implementazioni dei componenti applicativi delle singole SPA (restano nei rispettivi
use case), che qui si limitano a **consumare** il pacchetto.

## 2. Attori & ruoli
- **Sviluppatore / designer**: estrae i token dai mockup, definisce la struttura del pacchetto e lo mantiene nel tempo.
- **Consumatori del pacchetto** (nessuna duplicazione di valori): la SPA di backoffice (interfaccia degli utenti finali),
  la SPA di amministrazione (console interna), il **sito vetrina** costruito con Astro, e le **landing** generate dalla
  skill di scaffolding delle app (UC 0046).
- **Sistema / build**: il processo di compilazione del monorepo che importa il pacchetto nei vari progetti.
- Nessun terzo esterno; nessun trattamento di dati personali.

## 3. Precondizioni
- I mockup di riferimento sono disponibili in `docs/frontend-design/` (varianti backoffice e admin, con token coerenti).
- Il design system della prima SPA (UC 0019) esiste e contiene già i token in forma non ancora estratta come pacchetto.
- Il monorepo frontend ha un'organizzazione a workspace che consente di aggiungere un pacchetto condiviso importabile.
- Il sito vetrina (UC 0036) e la generazione landing (UC 0046) esistono come consumatori a cui offrire i token.

## 4. Flusso principale
1. **Ricognizione**: raccogliere i token oggi sparsi nel design system della SPA (UC 0019) e confrontarli con i mockup in
   `docs/frontend-design/`, così da fissare i valori canonici (una sola verità per ogni colore, dimensione, forma).
2. **Estrazione**: portare i token in un formato neutro e riusabile — in pratica variabili CSS (le "custom properties",
   cioè variabili di stile lette dal browser) più un preset per lo strumento di stile utilitario Tailwind — in modo che
   siano consumabili anche fuori dal mondo React.
3. **Confezionamento**: creare il **pacchetto condiviso** nel monorepo (ad esempio `packages/design-system`), che esporta:
   token colore chiaro/scuro, scala tipografica e definizioni dei caratteri, raggi/ombre/spaziature, asset del logo
   (chiaro/scuro), riferimento alle icone Material Symbols, e la nota di stile delle illustrazioni.
4. **Ricablaggio della prima SPA**: far sì che la SPA di backoffice smetta di contenere i token "in casa" e li importi dal
   pacchetto — verifica di non-regressione: l'aspetto non cambia.
5. **Estensione agli altri consumatori**: rendere gli stessi token disponibili alla SPA di amministrazione, al sito
   vetrina Astro e alle landing generate, così che tutti e quattro condividano la medesima base.
6. **Documentazione della fonte unica**: la nota di stile delle illustrazioni vive qui e diventa il riferimento che UC 0087
   segue per produrre gli artwork.

## 5. Flussi alternativi / edge / errori
- **Consumatore non-React (Astro / landing)**: non può importare componenti React; per questo i token vanno esposti anche
  come variabili CSS + preset Tailwind + asset statici, non solo come componenti. È il vincolo progettuale principale.
- **Tema scuro**: ogni token ha la sua variante scura; nessun consumatore deve "codificare a mano" un colore, altrimenti il
  tema scuro si rompe in modo silenzioso.
- **Accento configurabile**: corallo come predefinito, con alternative selezionabili (violet/teal/blue); il cambio è
  istantaneo perché passa dalle variabili di stile.
- **Rischio di divergenza (drift)**: se un consumatore duplicasse un valore invece di leggerlo dal pacchetto, l'estetica
  divergerebbe senza che nessun test diventi rosso. Mitigazione: i valori vivono **solo** nel pacchetto; un controllo
  automatico può segnalare colori/dimensioni scritti a mano nei consumatori.
- **Aggiornamento di un token**: un cambio nel pacchetto richiede la ripubblicazione dei consumatori che lo compilano
  (le due SPA a build-time; vetrina e landing alla loro generazione/deploy).
- **Font mancante o non caricato**: i consumatori devono degradare in modo pulito su un carattere di sistema affine,
  senza salti di impaginazione vistosi.

## 6. Risorse & runbook
- **Pacchetto token** (es. `packages/design-system` nel monorepo frontend): esporta variabili CSS (chiaro/scuro), preset
  Tailwind, definizioni della scala tipografica (Plus Jakarta Sans per il testo, JetBrains Mono per il monospazio),
  raggi/ombre/spaziature, asset del logo (chiaro/scuro), riferimento alle icone Material Symbols, nota di stile
  illustrazioni.
- **Consumatori** (quattro, stessa fonte): SPA di backoffice, SPA di amministrazione, sito vetrina Astro, landing generate
  da new-application (UC 0046).
- **Mockup sorgente**: `docs/frontend-design/` (backoffice + admin) come riferimento per l'estrazione.
- **Runbook di aggiornamento**: modificare il token nel pacchetto → ricompilare/ripubblicare i consumatori interessati →
  verifica visiva chiaro/scuro. Nessun atto irreversibile né verso l'esterno.

## 7. Dati toccati
Nessun dato applicativo e **nessun dato personale**: il pacchetto contiene solo token, asset grafici (logo, caratteri) e
riferimenti a icone. Manifesto dati (RoPA/GDPR) non applicabile.

## 8. Permessi & gate
- **Invarianti multi-tenancy**: non applicabili — il pacchetto è puramente presentazionale, non legge `tenant_id` né alcun
  dato. Nessun controllo di autorizzazione, nessun entitlement, nessuna quota.
- Il gate rilevante è di processo, non di runtime: un cambio ai token è un cambio "trasversale" che tocca tutti i
  consumatori e va trattato con l'attenzione dovuta (rilettura + consenso al commit/merge come da flusso di change).

## 9. Requisiti di test
- **Non-regressione visiva** sulla prima SPA dopo il ricablaggio: l'aspetto prima/dopo l'estrazione deve coincidere (poche
  pixel-snapshot tolleranti su schermate chiave, in chiaro e scuro).
- **Coerenza cross-consumatore**: verificare che le due SPA, la vetrina e una landing di esempio rendano gli stessi valori
  chiave (accento, sfondi, tipografia) — meglio se con un controllo automatico che rilevi valori codificati a mano fuori
  dal pacchetto (anti-drift).
- **Accessibilità**: contrasto sufficiente delle coppie colore testo/sfondo in chiaro e scuro.
- **Build**: ogni consumatore compila importando il pacchetto (nessun percorso rotto, font e asset risolti).
- Deve essere verde prima del merge: build dei consumatori + snapshot visive di riferimento + controllo anti-drift.

## 10. Riferimenti & Definition of Done
- **Fonte**: R20 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Brand kit & design system (#14 F)", punti F2/F3.
- **Correlati**: UC 0019 (design system & brand kit, dove oggi vivono i token), UC 0036 (sito vetrina Astro), UC 0046
  (landing generate), e il file fratello dell'epica [UC 0087 — Artwork logo finale + illustrazioni](0087-artwork-logo-e-illustrazioni.md).
- **DoD**:
  1. Esiste un pacchetto condiviso nel monorepo con tutti i token (colori chiaro/scuro con accento corallo + colori-categoria,
     scala tipografica Plus Jakarta Sans/JetBrains Mono, raggi/ombre/spaziature, logo chiaro/scuro, icone Material Symbols,
     nota stile illustrazioni).
  2. Le due SPA, il sito vetrina e le landing generate consumano **gli stessi** token, senza valori duplicati (zero drift).
  3. Non-regressione visiva verificata sulla prima SPA dopo l'estrazione.
  4. La nota di stile illustrazioni è presente nel pacchetto e serve da riferimento a UC 0087.

## Punti aperti / decisioni differite
- ~~**Meccanismo anti-drift automatico**~~ — **RISOLTO** dalla change `0080-use-case-0086-brand-kit-token`: realizzato come
  strumento a sé, `tools/design-tokens`, agganciato all'area `tooling` di `run-tests.sh`. Motivo della scelta: senza presidio
  la fonte unica si sfalda alla prima ricopiatura, e il drift trovato durante l'implementazione (dodici colori fuori palette
  fra email e anteprima social) ha mostrato che non era un rischio teorico.
- **Posizione del progetto Astro nel monorepo** e forma esatta dell'export cross-progetto: vincolo che UC 0036 possiede; qui
  si assume solo che i token siano esposti come variabili CSS + preset Tailwind + asset, non solo come componenti React.
  *Stato:* il sito vive in `site/` e collega il pacchetto come dipendenza a percorso; assunzione rispettata.
- **Versionamento del pacchetto** (se e come marcare cambi che richiedono ripubblicazione dei consumatori): da valutare se il
  numero di consumatori cresce; oggi la ripubblicazione è manuale/di processo. *Invariato dopo la change 0080.*
- **File `.svg` statici del logo su disco** (per un favicon, un allegato email, un'anteprima fuori dal codice): non creati
  dalla change 0080, perché sarebbero una seconda copia del disegno — che è esattamente ciò che questo use case elimina. Il
  disegno è disponibile come funzione (`logoMarkSvg`, `logoLockupSvg`): chi avrà bisogno di un file lo materializzerà da lì.
  *Possiede:* UC 0087, che produce l'artwork definitivo e sa quali formati serviranno davvero.

## Stato dopo l'implementazione (change 0080)
Il pacchetto era già la fonte unica per i tre consumatori che compilano CSS (SPA backoffice, SPA admin, vetrina Astro con
dentro le landing generate). La change ha chiuso il divario residuo: token leggibili **da un programma**
(`@appgrove/design-system/tokens.js`), disegno del logo isolato in un solo file e consumabile fuori da React
(`src/brand/logo.mjs`), nota di stile delle illustrazioni (`ILLUSTRAZIONI.md`), drift sanato nelle email e nell'anteprima
social, e presidio automatico contro il suo ritorno.

**Per UC 0087**: l'artwork definitivo si sostituisce intervenendo **solo** su `src/brand/logo.mjs` — i tracciati sono
dichiarati lì con il loro ruolo colore (`accent` / `contrast`), e componente React, immagine social e ogni consumatore
futuro li seguono da soli. La nota di stile delle illustrazioni è il riferimento da rispettare.
