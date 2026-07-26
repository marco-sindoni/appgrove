# UC 0087 — Artwork logo finale + stile illustrazioni on-brand

**Area**: 18-brand-e-design-system · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0019 (design system & brand kit), UC 0086 (pacchetto brand kit / token condiviso), UC 0037 (homepage vetrina)
**Fonte**: R20 (Tabella residui _INDEX.md) · docs/_BACKLOG.md §"Brand kit & design system (#14 F)"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Produrre gli **artwork grafici finali** dell'identità appgrove: il **logo definitivo** (a partire dal concept già scelto,
la "foglia dentro un quadrato ad angoli morbidi" con la parte scritta accanto) e un piccolo insieme di **illustrazioni
custom**, coerenti fra loro, dentro uno **stile ben definito**. È un use case di **produzione di contenuti visivi** — non
di infrastruttura né di logica applicativa — e può essere svolto con l'assistenza di strumenti di intelligenza artificiale
per la generazione, purché il risultato rispetti lo stile e i token del brand.

**Incluso**: la versione finale del logo (chiara e scura, nei formati e nelle dimensioni utili a vetrina, applicazioni e
metadati); poche illustrazioni on-brand realizzate come immagini vettoriali SVG che seguono i token del design system e
supportano tema chiaro e scuro.
**Escluso**: la definizione dei token e la loro confezione come pacchetto condiviso — è UC 0086; qui si **consumano** quei
token e la nota di stile che vi risiede.

## 2. Attori & ruoli
- **Designer / sviluppatore**: guida la produzione, seleziona e rifinisce gli artwork, garantisce coerenza con i token.
- **Strumenti di intelligenza artificiale (AI)**: assistono la generazione di bozze di logo e illustrazioni; l'output è
  sempre selezionato e rifinito da una persona, non pubblicato tale e quale.
- **Consumatori degli artwork**: il sito vetrina (in particolare la homepage, UC 0037), le due SPA, e le landing generate —
  tutti attraverso il pacchetto brand kit (UC 0086).
- Nessun terzo esterno con cui interagire in runtime; **nessun dato personale**.

## 3. Precondizioni
- Esiste il pacchetto brand kit condiviso (UC 0086) con i token e la **nota di stile delle illustrazioni** che questo use
  case deve seguire.
- Il concept del logo è già deciso a livello di direzione ("foglia-in-quadrato" + parte scritta), quindi qui si tratta di
  portarlo alla forma finale, non di riaprire la scelta creativa.
- La homepage della vetrina (UC 0037) e gli altri consumatori esistono come destinazione degli artwork.
- Lo stile "illustrazioni = SVG on-brand che seguono i token e supportano chiaro/scuro" è già la convenzione in uso nel
  progetto e va riusato, non reinventato.

## 4. Flusso principale
1. **Logo — rifinitura del concept**: partendo dalla "foglia-in-quadrato", produrre le bozze finali (anche con assistenza
   AI), scegliere la versione definitiva e ripulirla come vettoriale nitido a ogni dimensione.
2. **Logo — varianti**: derivare le versioni per **tema chiaro e scuro**, il segno da solo (senza parte scritta) per gli
   spazi piccoli, e i formati/dimensioni utili (favicon, icona app, immagine per l'anteprima social, intestazioni).
3. **Illustrazioni — definizione operativa dello stile**: fissare, a partire dalla nota di stile del brand kit, le regole
   concrete (tratto, colori dai token, uso dell'accento corallo, resa in chiaro/scuro) così che più illustrazioni sembrino
   "della stessa mano".
4. **Illustrazioni — produzione**: generare (anche con assistenza AI) e rifinire un **piccolo** set di illustrazioni SVG
   on-brand per i punti che ne hanno davvero bisogno (ad esempio la homepage della vetrina), evitando la proliferazione.
5. **Integrazione**: consegnare gli artwork al pacchetto brand kit (UC 0086) come asset, così che i consumatori li usino
   dalla fonte unica; verificare la resa in chiaro e scuro su vetrina, SPA e una landing di esempio.

## 5. Flussi alternativi / edge / errori
- **Output AI fuori stile**: le bozze generate che non rispettano i token o il tratto vengono scartate; nessun artwork
  entra nel brand kit senza rifinitura umana.
- **Logo su sfondi difficili**: prevedere la variante che resta leggibile su sfondo chiaro, scuro e su fotografie/aree
  colorate (versione monocromatica se serve).
- **Illustrazione che non regge il tema scuro**: se un'immagine usa colori fissi, va riportata ai token; l'SVG deve
  reagire al tema come il resto dell'interfaccia.
- **Peso/nitidezza degli asset**: preferire il vettoriale SVG (nitido e leggero) alle immagini a griglia di pixel; per il
  logo, dove servono formati a pixel (favicon, anteprima social), esportarli dalla sorgente vettoriale.
- **Troppe illustrazioni**: rischio di incoerenza e di manutenzione; la regola è "poche e coerenti", non "una per pagina".
- **Diritti/licenze**: gli artwork sono originali del progetto (anche se assistiti da AI); evitare elementi di terzi con
  licenza incompatibile.

## 6. Risorse & runbook
- **Artwork del logo**: file vettoriale sorgente + esportazioni (chiaro/scuro, segno da solo, favicon, icona app, immagine
  anteprima social), consegnati come asset del pacchetto brand kit (UC 0086).
- **Illustrazioni**: piccolo insieme di SVG on-brand che leggono i token e reagiscono al tema chiaro/scuro, riutilizzabili
  dalle pagine future (stesso stile).
- **Nota di stile**: risiede nel brand kit (UC 0086); questo use case la traduce in regole operative concrete e la applica.
- **Runbook di produzione**: bozza (anche AI) → selezione/rifinitura umana → verifica token e chiaro/scuro → consegna al
  brand kit. Nessun atto irreversibile né verso l'esterno.

## 7. Dati toccati
Nessun dato applicativo e **nessun dato personale**: solo asset grafici (logo, illustrazioni). Manifesto dati (RoPA/GDPR)
non applicabile.

## 8. Permessi & gate
- **Invarianti multi-tenancy**: non applicabili — sono asset presentazionali, non leggono `tenant_id` né alcun dato.
  Nessun controllo di autorizzazione, entitlement o quota.
- Gate di processo: gli artwork entrano nel brand kit condiviso, quindi la loro modifica è trasversale ai consumatori e
  segue il consenso al commit/merge del flusso di change; l'output AI passa sempre da una rifinitura umana prima della
  pubblicazione.

## 9. Requisiti di test
- **Resa cross-tema**: verificare che logo e illustrazioni siano corretti e leggibili in chiaro e scuro (poche snapshot
  visive su vetrina/homepage e su una schermata di SPA).
- **Nitidezza e formati del logo**: le esportazioni (favicon, icona, anteprima social) sono nitide e delle dimensioni
  attese; la variante su sfondo difficile resta leggibile.
- **Coerenza di stile**: le illustrazioni condividono tratto e palette dai token (controllo visivo; nessun colore fisso
  fuori dai token).
- **Integrazione**: gli asset caricano correttamente dai consumatori attraverso il brand kit (nessun percorso rotto).
- Deve essere verde prima del merge: build dei consumatori che usano gli asset + snapshot visive di riferimento.

## 10. Riferimenti & Definition of Done
- **Fonte**: R20 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Brand kit & design system (#14 F)".
- **Correlati**: UC 0019 (design system), UC 0037 (homepage vetrina, consumatore principale), e il file fratello
  dell'epica [UC 0086 — Pacchetto brand kit / token condiviso](0086-brand-kit-token-condiviso.md), dove vive la nota di stile.
- **DoD**:
  1. Esiste il logo finale (versioni chiara/scura, segno da solo, formati favicon/icona/anteprima social) consegnato come
     asset del brand kit condiviso.
  2. Esiste un piccolo insieme di illustrazioni SVG on-brand coerenti, che seguono i token e supportano chiaro/scuro.
  3. La resa in chiaro e scuro è verificata su vetrina/homepage e SPA.
  4. Ogni artwork generato con AI è stato selezionato e rifinito da una persona e rispetta lo stile del brand kit.

## Punti aperti / decisioni differite
- **Quantità e soggetti delle illustrazioni**: quali pagine future oltre alla homepage meritino davvero un'illustrazione —
  scelta di prodotto/contenuto da fare pagina per pagina; *differito perché:* dipende dalle pagine effettivamente in
  produzione; *possiede:* questo UC insieme ai singoli use case delle pagine.
- **Marchio registrato / tutela del logo**: se e quando avviare una registrazione del marchio è una decisione legale/di
  business fuori scope tecnico; *possiede:* la revisione legale pre-go-live.
- **Provenienza e licenza degli strumenti AI** usati per generare gli artwork: annotare quale strumento e con quali termini,
  per evitare sorprese di licenza; da tracciare al momento della produzione effettiva.
