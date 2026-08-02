# Change 0079 — Log di implementazione

**Branch**: `change/0079-use-case-0085-renderer-email-commons`
**Use case sorgente**: `docs/usecases/17-skill-e-tooling-contenuto/0085-unificazione-renderer-email-commons.md`
**Modalità**: fast (autopilot senza gate di workflow, dichiarata all'invocazione)
**Aree toccate**: `services/commons`, `services/auth`, `services/core` + documentazione

## Cosa è stato fatto

La logica di resa delle email esisteva in **due copie quasi identiche** — `EmailTemplates` in `services/auth` e
`NewsletterEmailRenderer` in `services/core`. Ora esiste **una sola volta**, in `services/commons`.

### 1. Il renderer unico (`services/commons`)

- `app/appgrove/commons/email/EmailTemplateRenderer.java` — i due passaggi della resa (stringhe della lingua risolte
  contro i valori dinamici; stringhe risolte che riempiono l'impaginazione condivisa), l'escape dei valori nella
  versione grafica e la guardia che fa fallire un messaggio con segnaposto non risolti. Espone
  `Rendered(subject, text, html)`. Non è un bean gestito dal contenitore di iniezione: si costruisce passando il set
  di lingue, e carica impaginazione e cataloghi **nel costruttore**, così un artefatto senza template fallisce
  all'avvio del servizio e non alla prima email da spedire.
- `app/appgrove/commons/email/EmailLocales.java` — il **set di lingue parametrizzabile**: lingue coperte + lingua di
  ripiego, con la normalizzazione delle forme comuni (`it`, `it-IT`, `IT_it`, con spazi). Valida sé stesso: il ripiego
  deve essere fra le lingue supportate. È l'unica sede dell'algoritmo, che prima esisteva in tre punti.

### 2. I due servizi

- `services/auth/EmailTemplates.java` — ridotto ad **adattatore**: costruisce il renderer condiviso con le lingue del
  servizio e delega. Zero logica di resa. Resta il punto di iniezione già usato da `EmailService`.
- `services/auth/Locales.java` — continua a dichiarare **quali** lingue copre il servizio (serve anche fuori dalle
  email: lingua scritta a database, attributo passato a Cognito), ma `normalize` delega ora al set condiviso.
- `services/core/NewsletterEmailRenderer.java` — ridotto ad **adattatore**: dichiara le lingue della newsletter
  (inglese/italiano, ripiego sull'inglese) e la chiave del messaggio, delega la resa; mantiene il metodo statico di
  normalizzazione già richiamato da `NewsletterService` e `NewsletterResource`.
- `EmailService` (auth) e `NewsletterMailer` (core) usano il tipo di risultato del renderer condiviso: è l'unica
  modifica che tocca codice fuori dai due renderer.
- `services/commons/pom.xml` — copia di `shared/email-templates` nelle risorse **di test** soltanto, perché i test del
  renderer condiviso possano rendere davvero. I `pom.xml` di `auth` e `core`, che copiano i template nel classpath di
  produzione, restano invariati come lo use case prescriveva.

### 3. Test

- **Nuovi in `services/commons`**: `EmailTemplateRendererTest` (11 test: lingue e ripiego, varianti regionali,
  sostituzione dei valori dinamici, escape del collegamento e dei valori, resa della conferma newsletter, guardia sui
  segnaposto, messaggio sconosciuto, e i due casi che dimostrano il set di lingue parametrizzabile — insieme ristretto
  e default diverso); `EmailLocalesTest` (6 test: normalizzazione, ripiego, validazione, copia difensiva).
- **Spostato**: `EmailTemplatesParityTest` da `services/auth` → `services/commons` come `EmailCatalogParityTest` (la
  parità fra le lingue dei cataloghi vale per tutti i servizi che spediscono email, non per uno solo).
- **Ridotto**: `EmailTemplatesTest` in `services/auth` resta come test di integrazione — il bean cablato sul renderer
  condiviso e i template davvero presenti **nell'artefatto di auth** per tutti e tre i messaggi. Se la copia
  configurata nel suo `pom.xml` si rompesse, sarebbe rosso lì invece che all'avvio in produzione. Stesso presidio per
  il core attraverso i test di flusso della newsletter, che rendono l'email per davvero.

## Collaudo di parità byte-a-byte — superato

Criterio di accettazione centrale dello use case: le email non devono cambiare di un carattere.

- **Prima** della rifattorizzazione sono stati catturati **37 casi** resi dai due renderer originali: `verify`,
  `reset` e `invite` per sette forme di lingua (`en`, `it`, `de`, `it-IT`, `IT_it`, `" it "` con spazi, stringa vuota)
  più il caso della lingua assente, con un valore dinamico contenente caratteri da proteggere (`& <b>`);
  `newsletter-confirm` per le stesse forme più la lingua assente; e l'esito della normalizzazione della lingua della
  newsletter.
- **Dopo**, gli stessi 37 casi sono stati rigenerati passando dal renderer condiviso.
- **Esito**: nessuna differenza. Confronto integrale vuoto, impronte crittografiche dei due insiemi identiche.

Gli strumenti di cattura erano temporanei e sono stati rimossi: file di riferimento congelati legherebbero la suite ai
testi delle email, che possono cambiare legittimamente (decisione 8 del registro).

## Test eseguiti

`./run-tests.sh` (suite **completa**, come prescritto dalla modalità fast): **verde**.

- `services/commons`: 19 test nell'area email (11 + 6 nuovi, 2 spostati), oltre al resto del modulo;
- `services/auth` e `services/core`: verdi senza modifiche sostanziali ai loro test di flusso;
- tutte le altre aree (frontend con controllo dei tipi e Playwright, infra, compliance, smoke, tooling) non toccate e
  verdi.

## Copertura end-to-end

**Nessun impatto**: lo use case 0085 è già registrato in `docs/testing/copertura-e2e.yaml` come esente in categoria
`senza-superficie`. La change non introduce pagine, rotte o comportamenti visibili: nessun percorso Playwright da
aggiungere o estendere, registro invariato.

## Dati personali

**Nessuno**. Rifattorizzazione interna: nessuna nuova tabella, nessun nuovo trattamento, nessun manifesto dati o RoPA
da aggiornare. Gli indirizzi email trattati sono gli stessi di prima, per le stesse finalità.

## Rimandi tracciati

- `docs/usecases/05-auth/0018-localizzazione-email-auth.md` — due punti nuovi, entrambi di proprietà di UC 0018:
  (1) la **seconda implementazione in Python** della stessa resa nel Custom Message Lambda, che non è unificabile col
  codice Java e può divergere in silenzio; (2) **dove vivono i template nell'artefatto** — se spostarli dentro il jar
  della libreria condivisa e togliere i passi di copia dai due servizi.
- `docs/usecases/17-skill-e-tooling-contenuto/0085-…` — punto aperto «dove collocare il renderer» chiuso con la scelta
  presa; aggiunto il punto sulla copia dei template, con rimando a UC 0018.
- `docs/_BACKLOG.md` §Tooling — la voce che aveva sollevato il tema è marcata chiusa, con il residuo (Lambda Python)
  esplicitato.

## Indice di esecuzione

`docs/usecases/EPICS-WAVE-2.md`: UC 0085 → ✅ implementato. (Le storie evolutive vivono in quell'indice, non nella
tabella topologica di `docs/usecases/_INDEX.md`.)

## Definition of Done

1. ✅ Un unico renderer email in `services/commons`, con set di lingue parametrizzabile.
2. ✅ `services/auth` e `services/core` lo usano; le due copie precedenti sono adattatori sottili senza logica di resa.
3. ✅ Email invariate: parità verificata carattere per carattere su 37 casi.
4. ✅ Suite completa verde; nessun dato personale o manifesto toccato.
5. ✅ `run-tests.sh` non necessita modifiche (nessun modulo aggiunto o rimosso, nessun comando di test cambiato).
6. ✅ Registro `decisions.json` completo (16 voci) e guida di verifica manuale `how-to-test.md`.
