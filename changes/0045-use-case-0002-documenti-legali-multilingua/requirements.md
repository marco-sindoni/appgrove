# Change 0045: Documenti legali reali multilingua (UC 0002)

**Branch**: `change/0045-use-case-0002-documenti-legali-multilingua`
**Aree**: `content/legal` (nuova) · `tools/compliance` (nuovo check + test) · `docs/compliance/manifests` (riconciliazione path) · `run-tests.sh`/`package.json` (cablaggio)
**Data**: 2026-07-25
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: [docs/usecases/01-business-legal/0002-documenti-legali-multilingua.md](../../docs/usecases/01-business-legal/0002-documenti-legali-multilingua.md)
**Aree collegate**: [#13 Compliance/Privacy](../../docs/13-compliance-privacy.md), [#14 Sito vetrina & legali](../../docs/14-sito-vetrina-legale.md), [_REVISIONE-LEGALE](../../docs/_REVISIONE-LEGALE.md)

**Tocca dati personali?**: I documenti **descrivono** i trattamenti, non contengono dati di utenti. Nessuna nuova
raccolta introdotta da questa change. Il log di accettazione (utente+versione) è UC 0056 e resta fuori. Il gate
privacy/RoPA (UC 0031) è quindi qui **inverso**: invece di classificare un nuovo trattamento, questa change **realizza
la destinazione** (`content/legal/`) dove le classificazioni MAJOR/MINOR accumulate dalle change precedenti dovevano
essere riflesse. Riporto eseguito (vedi Scope §4): tutte le classificazioni accumulate risultano **MINOR / componente
piattaforma core, senza nuovi sub-responsabili** → i documenti nascono a baseline `version: 1.0.0` e non richiedono bump
divergenti; i sub-responsabili sono i due noti (AWS, Plausible).

## Problema / Obiettivo

Paddle e il GDPR richiedono documenti legali pubblici reali (Privacy Policy art. 13-14, Terms & Conditions da Merchant
of Record, Refund Policy, disclosure cookie) più una lista sub-responsabili pubblica. Oggi non esiste né la cartella
`content/`, né alcun testo legale, né un controllo che ne garantisca la completezza linguistica. Obiettivo: produrre i
documenti come **markdown fonte unica multilingua** in `content/legal/`, in **5 lingue** (EN/IT/FR/ES/DE, **IT facente
fede** sui legali), con frontmatter `version`/`effective_date`/`lang`, e un **controllo di integrazione continua** che
renda rossa la build se manca una lingua o il frontmatter non è valido.

## Scope

### 1. Struttura `content/legal/` e documenti (5 componenti × 5 lingue = 25 file)
Creare `content/legal/` (nuova radice pubblica `content/`, distinta da `docs/` interna — #13 §50). Un file per
componente e lingua, `<componente>.<lang>.md`:
- **privacy** — Privacy Policy (art. 13-14): titolare, categorie di dati e finalità, basi giuridiche (tabella #13 §B),
  destinatari/sub-responsabili (link a `subprocessors`), trasferimenti extra-UE e garanzie (DPF+SCC per AWS), retention
  (quadro #13 §E), diritti dell'interessato e come esercitarli (`privacy@`), sezione cookie (rimando a `cookie`).
  Struttura predisposta per i **moduli per-app** (snippet generati da UC 0046/0030) senza implementarli.
- **terms** — Terms & Conditions: erogazione del servizio, ruolo **Paddle Merchant of Record** (vende Paddle, gestisce
  tax/fatturazione/rimborsi), rimando ai Paddle Buyer Terms, uso di sub-responsabili e notifica dei cambi (DPA verso i
  clienti incorporato, #13 §48), legge applicabile.
- **refund** — Refund Policy (terzo documento Paddle): "vendite finali / nessun rimborso salvo obbligo di legge",
  richiamo del **recesso 14 giorni** già concesso dai Paddle Buyer Terms (#09 J43, #14 A2).
- **cookie** — Cookie disclosure come **sezione autonoma** (nessun banner): solo tecnici essenziali (refresh token
  HttpOnly, eventuale CSRF/config) + Plausible cookieless EU → nessun consenso richiesto (#13 F27/28).
- **subprocessors** — lista sub-responsabili pubblica e viva, seminata da #13 dec. 45: **AWS** (hosting/DB/storage/SES,
  eu-west-1 + eu-central-1, UE, garanzie DPF+SCC) e **Plausible** (analytics vetrina, EU-hosted Hetzner, cookieless).
  **Paddle escluso** (ruolo MoR/titolare autonomo, non sub-responsabile). Colonne: nome/finalità/regione/categorie dati.
  Linkata da Privacy Policy e (futuro) DPA.

Frontmatter obbligatorio per ogni file: `version` (semver, avvio `1.0.0`), `effective_date` (`YYYY-MM-DD`),
`lang` (coerente col suffisso del nome file). **IT facente fede**: in caso di difformità prevale l'italiano (clausola
esplicita nel testo).

**Identità del titolare — fonte unica `content/legal/entity.yaml` (decisione dello sviluppatore)**: nome legale, sede
(domiciliazione), P.IVA e email di contatto **non esistono ancora** (posseduti da UC 0001, si concretizzano alla
monetizzazione). Invece di ripeterli nei testi, un **unico file `content/legal/entity.yaml`** li tiene come fonte di
verità; nei testi compaiono come **token** `{{titolare.<campo>}}` (es. `{{titolare.ragione_sociale}}`,
`{{titolare.sede}}`, `{{titolare.piva}}`, `{{titolare.email_privacy}}`). Alla disponibilità dei dati basta modificare
`entity.yaml` e ogni documento (e, in futuro, sito + rendering in-app) si aggiorna. **Contratto di sostituzione**
documentato in `content/legal/README.md` e tracciato come dipendenza per i renderer (UC 0036 sito, UC 0056 in-app):
oggi nessuno renderizza ancora i legali, quindi i token restano letterali nei md finché quei renderer non li risolvono.
I valori nascono come `DA COMPILARE` (compilazione pre-go-live, UC 0001).

### 2. Controllo di integrazione continua — `tools/compliance` (nuovo)
Sul modello di `ropa.mjs`/`lib.mjs` (Node ESM, `node --test`, parser `yaml`):
- **`legal.mjs`** (logica pura, nessun I/O): dato l'elenco dei file `content/legal/*.md` e la lista lingue richieste,
  verifica per ogni componente la **presenza di tutte e 5 le lingue** e la **validità del frontmatter** (`version`
  semver valido, `effective_date` data ISO valida, `lang` presente e coerente col nome file). In più valida
  l'**integrità referenziale dei token**: ogni `{{titolare.<campo>}}` usato nei testi deve avere una chiave
  corrispondente in `entity.yaml` (errore rosso se orfano); i valori ancora `DA COMPILARE` sono elencati come **avviso
  informativo, non bloccante** (attesi pre-go-live). Ritorna un array di errori (vuoto = ok), stile `validateManifests`.
- **`legal-check.mjs`** (CLI con I/O): legge `content/legal/`, splitta il frontmatter fra i delimitatori `---` e lo
  passa a `parse()` di `yaml`, invoca la logica pura, stampa gli errori con `✗` ed esce `1` se rosso (stile `bail`).
  Lingue richieste dichiarate in `content/legal/_config.yaml` (`required_languages: [en, it, fr, es, de]`).
- **`test/legal.test.mjs`**: casi verdi/rossi sulla logica pura (lingua mancante, frontmatter assente/invalido, `lang`
  incoerente col nome file).
- **Cablaggio**: nuovo script `"legal-check"` in `tools/compliance/package.json`; una riga
  `( cd tools/compliance && npm run legal-check ) || rc=1` in `run_compliance` di `run-tests.sh`. Il job `compliance`
  della CI (`.github/workflows/verify-pr.yml`) gira sempre → il controllo diventa gate bloccante senza toccare il
  workflow. `npm test` raccoglie automaticamente i nuovi test.

### 3. Riconciliazione path sub-responsabili
`docs/compliance/manifests/platform.yaml` cita ancora il path vecchio `content/subprocessors.md` (righe ~298/303, IT+EN):
aggiornarlo a `content/legal/subprocessors.<lang>.md`. Rigenerare il RoPA (`npm run assemble`) e committare i file
allineati (il check freshness lo esige).

### 4. Riporto delle decisioni differite possedute da UC 0002
Eseguito e documentato nel log:
- **Classificazioni MAJOR/MINOR accumulate**: scansione degli artefatti delle change → tutte **MINOR / piattaforma
  core** (0029 self-service GDPR, 0030 console diritti, 0035 observability, 0037 Cognito BFF). Nessuna MAJOR → baseline
  `1.0.0` senza bump divergenti.
- **Seed lista sub-responsabili**: AWS + Plausible (dec. 45). Nessuna segnalazione "potenziale nuovo sub-responsabile"
  negli implementation-log (Cognito/SES = AWS già in lista; Mailpit = solo sviluppo locale).

## Fuori scope (tracciato come rimando)
- **Rendering nel sito vetrina + link menu/footer** (DoD punto 3): il sito Astro non esiste ancora (UC 0036-0043) e la
  SPA non renderizza i legali → si traccia in UC 0002 "Punti aperti" con proprietario UC 0037 (footer) / UC 0036 (deploy).
- **Runtime accettazione/ri-accettazione**: derivazione "accettata < major → bloccante", schermata, tabella
  `legal_version`, log accettazione → **UC 0056** (già "Escluso" nel UC).
- **Snippet privacy per-app** dai manifesti → **UC 0046/0030**.
- **Compilazione identità titolare** (segnaposto `{{TITOLARE_*}}`) → **UC 0001**, pre-go-live.
- **Revisione legale** dei testi → opzionale, [_REVISIONE-LEGALE](../../docs/_REVISIONE-LEGALE.md) L2/L3/L13.

## Requisiti di test
- **`tools/compliance`**: il nuovo check è verde sui 25+5 file prodotti; test unitari coprono i rami rossi (lingua
  mancante, frontmatter invalido, `lang` incoerente). `npm test` + `npm run check` + `npm run legal-check` verdi.
- **`run-tests.sh compliance`** verde end-to-end (include RoPA freshness dopo la riconciliazione del path).
- Nessun test backend/frontend/infra: la change non tocca codice eseguibile di quelle aree.

## Definition of Done
1. PP + ToS + Refund + Cookie + subprocessors in 5 lingue (IT facente fede) in `content/legal/`, frontmatter valido,
   md fonte unica; subprocessors linkato dalla Privacy Policy.
2. Check CI 5 lingue + frontmatter verde e cablato (`run-tests.sh compliance` + CI).
3. Path sub-responsabili riconciliato nel manifesto + RoPA rigenerato allineato.
4. Riporto classificazioni + seed sub-responsabili documentato nel log; rimandi tracciati negli use case proprietari.

## Escalation aperta allo sviluppatore
L'**identità legale del titolare** (nome/sede/P.IVA/email) è una decisione di business/legale posseduta da UC 0001, non
dell'agente: oggi non esiste. Raccomandazione autopilot (adottata salvo tua diversa indicazione): **segnaposto
`{{TITOLARE_*}}` inequivocabili**, così l'impianto e i testi nascono completi e la sola compilazione finale resta a te
pre-go-live. In alternativa puoi fornire ora i valori (anche persona fisica: nome + domiciliazione + email) e li inserisco.
