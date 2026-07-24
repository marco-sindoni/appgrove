// ─────────────────────────────────────────────────────────────────────────────
// tools/new-application/lib/context.mjs — dal comando al contesto di sostituzione.
//
// Raccoglie in un punto solo TUTTE le decisioni derivate: come si scrive
// l'identificativo dell'app nelle varie convenzioni (nome di classe, nome di
// variabile, titolo leggibile), quale porta le tocca, quale schema Postgres
// possiede. Il resto del generatore non ricalcola nulla: legge da qui.
//
// La scoperta delle porte già assegnate NON è reimplementata: arriva da
// `dev/lib/services.sh`, la sorgente unica che deriva la mappa
// servizio → app_id → porta → schema dagli `application.properties` dei servizi
// esistenti. Duplicarla qui vorrebbe dire avere due verità sulle porte, e
// scoprire il conflitto solo all'avvio dello stack locale.
// ─────────────────────────────────────────────────────────────────────────────
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

/** Identificativo app: stessa regola di `infra/scripts/service-add` (deve restare allineata). */
export const APP_ID_PATTERN = /^[a-z][a-z0-9_]{0,30}$/

/**
 * Nomi che un'app non può assumere: sono già presi da servizi di piattaforma o
 * da convenzioni del monorepo. Prenderli non darebbe un errore subito, ma rotte
 * e schemi sovrapposti molto più tardi.
 */
export const RESERVED_APP_IDS = new Set([
  'platform', // app_id del core
  'core', // directory del servizio di piattaforma
  'auth', // servizio di autenticazione (Lambda, ciclo di vita a parte)
  'commons', // libreria condivisa, non un servizio
  'admin', // SPA di amministrazione
  'backoffice', // SPA cliente
  'demo', // modulo di prova dell'App Registry
])

/** Porta HTTP di default di Quarkus: è quella del core, quindi mai assegnabile a un'app. */
const DEFAULT_HTTP_PORT = 8080
/** Debug JVM = 5005 + (porta http − 8080), stessa formula di dev/lib/services.sh. */
const DEBUG_PORT_BASE = 5005

/**
 * Servizi già presenti, come li vede lo stack di sviluppo locale.
 * Righe TSV `<svc> <app_id> <porta> <schema> <ruolo>`.
 */
export function discoverServices(repoRoot) {
  const script = path.join(repoRoot, 'dev/lib/services.sh')
  if (!fs.existsSync(script)) {
    throw new Error(
      `dev/lib/services.sh non trovato in ${repoRoot}.\n`
      + '  È la sorgente unica della mappa servizio → app_id → porta → schema: senza di essa il\n'
      + '  generatore non può sapere quali porte sono già assegnate, e la sceglierebbe alla cieca.',
    )
  }
  const out = execFileSync(
    'bash',
    ['-c', `REPO_ROOT=${JSON.stringify(repoRoot)}; source ${JSON.stringify(script)}; discover_services`],
    { encoding: 'utf8' },
  )
  return out
    .split('\n')
    .filter((line) => line.trim() !== '')
    .map((line) => {
      const [svc, appId, port, schema, role] = line.split('\t')
      return { svc, appId, port: Number(port), schema: schema === '-' ? '' : schema, role }
    })
}

/**
 * Prima porta libera nella fascia delle app (core = 8080, prima app = 8081, …).
 *
 * Si guarda solo alle porte dei ruoli `core` e `app`: il servizio di
 * autenticazione vive fuori fascia (Lambda su 9100) e includerlo spingerebbe
 * ogni nuova app a 9101, sparpagliando le app su un intervallo che non è il
 * loro. La verifica di collisione, invece, si fa su TUTTE le porte occupate.
 */
export function nextFreePort(services) {
  const appPorts = services.filter((s) => s.role !== 'auth').map((s) => s.port)
  const allPorts = new Set(services.map((s) => s.port))
  let candidate = Math.max(DEFAULT_HTTP_PORT, ...appPorts) + 1
  while (allPorts.has(candidate)) candidate += 1
  return candidate
}

/** `demo_gen` → `DemoGen` (nome di classe Java e di componente React). */
export function toPascalCase(appId) {
  return appId
    .split('_')
    .filter(Boolean)
    .map((part) => part[0].toUpperCase() + part.slice(1))
    .join('')
}

/** `demo_gen` → `demoGen` (nome di variabile esportata, es. `demoGenManifest`). */
export function toCamelCase(appId) {
  const pascal = toPascalCase(appId)
  return pascal[0].toLowerCase() + pascal.slice(1)
}

/**
 * Nature ammesse per la metrica di quota. Non è una preferenza di stile: decide COME si conta.
 * <ul>
 *   <li>`flow` — a consumo: eventi in una finestra che si azzera (es. "200 documenti al mese");</li>
 *   <li>`stock` — a giacenza: tetto su quanti oggetti esistono ORA (es. "10 posti").</li>
 * </ul>
 * Sbagliarla è l'errore più costoso del listino: un tetto a giacenza contato a consumo non si
 * libera mai, e una metrica a consumo contata a giacenza blocca l'utente per sempre.
 */
export const QUOTA_NATURES = ['flow', 'stock']

/**
 * Marcatore di VARIANTE nei nomi dei modelli-sorgente: `QuotaTest.flow.java` e `QuotaTest.stock.java`
 * producono entrambi `QuotaTest.java`, ma solo quello della natura scelta viene istanziato.
 *
 * Perché una variante di file e non un segnaposto dentro un file solo: la differenza fra le due
 * nature è il CORPO di un metodo di conteggio (più le sue importazioni e i suoi test), non una
 * parola. Comprimerla in segnaposto avrebbe prodotto modelli illeggibili proprio nel punto in cui
 * si sbaglia più facilmente.
 */
export const VARIANT_RE = /\.(flow|stock)(?=\.[^.]+$)/

/** `demo_gen` → `Demo Gen` (titolo leggibile di default, poi riscritto dall'autore dell'app). */
export function toDisplayName(appId) {
  return appId
    .split('_')
    .filter(Boolean)
    .map((part) => part[0].toUpperCase() + part.slice(1))
    .join(' ')
}

/**
 * Costruisce la mappa segnaposto → valore. È l'unico posto dove si decide cosa
 * significa ciascun segnaposto: aggiungerne uno vuol dire aggiungerlo qui e nei
 * modelli-sorgente, e il controllo finale sui residui fa il resto.
 */
export function buildContext(options) {
  const appId = options.appId
  const appClass = toPascalCase(appId)

  return {
    APP_ID: appId,
    APP_CLASS: appClass,
    APP_CAMEL: toCamelCase(appId),
    APP_NAME: options.appName ?? toDisplayName(appId),
    SCHEMA: `app_${appId}`,
    HTTP_PORT: String(options.port),
    DEBUG_PORT: String(DEBUG_PORT_BASE + options.port - DEFAULT_HTTP_PORT),
    METRIC: options.metric,
    FREE_CAP: String(options.freeCap),
    QUOTA_NATURE: options.quotaNature,
    // Come si legge il tetto in prosa: compare nei commenti dei modelli e nella nota del listino.
    // Tenerla qui evita che ogni modello se la riscriva a modo suo.
    QUOTA_CAP_NOTE:
      options.quotaNature === 'stock'
        ? `${options.freeCap} ${options.metric} contemporanei`
        : `${options.freeCap} ${options.metric}/mese`,
    // Finestra della metrica nei dati di prova (Java e TypeScript). A giacenza NON esiste finestra:
    // scriverne una — anche solo in un test — insegnerebbe al lettore una regola che non c'è.
    QUOTA_NATURE_ENUM: options.quotaNature === 'stock' ? 'STOCK' : 'FLOW',
    QUOTA_WINDOW_JAVA: options.quotaNature === 'stock' ? 'null' : '"month"',
    QUOTA_WINDOW_TS: options.quotaNature === 'stock' ? 'null' : "'month'",
    QUOTA_LABEL: options.quotaNature === 'stock' ? 'Consumo attuale' : 'Consumo questo mese',
    QUOTA_REACHED:
      options.quotaNature === 'stock'
        ? 'Hai raggiunto il limite del tuo piano. Libera un elemento oppure passa a un piano superiore.'
        : 'Hai raggiunto il limite mensile del tuo piano.',
    USER_MODEL: options.userModel === 'multi' ? 'multi_user' : 'single_user',
    USER_MODEL_NOTE:
      options.userModel === 'multi'
        ? 'multi-utente (B2B): piu utenti per account, con ruoli'
        : 'utente singolo (B2C): un solo utente per account',
    // Ruoli ammessi sugli endpoint. In multi-utente esiste anche il membro semplice: senza di lui
    // un'app B2B nascerebbe con tutti gli endpoint riservati ai soli amministratori.
    ROLES_ALLOWED:
      options.userModel === 'multi'
        ? 'Roles.OWNER, Roles.ADMIN, Roles.MEMBER'
        : 'Roles.OWNER, Roles.ADMIN',
    ROLES_EXTRA_CONSTANTS:
      options.userModel === 'multi'
        ? '    /** Membro semplice dell\'account (solo modello multi-utente). */\n'
          + '    public static final String MEMBER = "member";\n'
        : '',
    ICON: options.icon,
    ACCENT: options.accent,
    // Testi del manifesto dati che NESSUN generatore può inventare: restano marcati come da
    // completare, così il co-pilota dati personali (e chi legge la revisione) li vede subito.
    PD_PURPOSE: 'DA COMPLETARE — finalita del trattamento',
    PD_RETENTION: 'DA COMPLETARE — periodo di conservazione',
  }
}
