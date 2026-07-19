// ─────────────────────────────────────────────────────────────────────────────
// tools/new-application/lib/edits.mjs — le modifiche ai file che ESISTONO già.
//
// Sono poche di proposito. Ogni riga che il generatore incolla in un file
// condiviso è una riga che qualcuno dovrà ricordarsi di togliere quando l'app
// verrà ritirata, e un punto in cui due app possono litigare. Tutto ciò che si
// può DERIVARE (avvio locale, rotte del proxy, liste della CI, migrazioni) è già
// derivato da `dev/lib/services.sh` e `tools/ci/services.sh`: quei file NON si
// toccano, ed è il motivo per cui questo elenco è corto.
//
// Ogni modifica è una funzione pura `(contenuto, contesto) => nuovoContenuto`
// che DEVE essere idempotente nel senso forte: se il segno della modifica è già
// presente, solleva un errore invece di applicarla due volte. Il generatore
// controlla i conflitti prima di scrivere, ma queste funzioni sono l'ultima
// difesa, ed è meglio un errore chiaro di un file corrotto in silenzio.
// ─────────────────────────────────────────────────────────────────────────────

/** Percorsi (relativi alla root) dei file modificati, nell'ordine in cui si applicano. */
export const EDITED_FILES = [
  'services/pom.xml',
  'frontend/apps/backoffice/src/registry/registry.ts',
  'frontend/apps/backoffice/package.json',
  'services/core/src/main/resources/pricing/index.yaml',
  'dev/elasticmq.conf',
]

/** Aggiunge il modulo Maven all'elenco dei moduli aggregati. */
export function editServicesPom(content, ctx) {
  const marker = `<module>${ctx.APP_ID}</module>`
  if (content.includes(marker)) {
    throw new Error(`services/pom.xml contiene già ${marker}`)
  }
  if (!content.includes('  </modules>')) {
    throw new Error("services/pom.xml: blocco </modules> non trovato (formato cambiato?)")
  }
  return content.replace('  </modules>', `    ${marker}\n  </modules>`)
}

/**
 * Registra il modulo nell'App Registry del backoffice: import del manifest +
 * voce nell'array MODULES. La sidebar mostra poi l'intersezione con i diritti
 * del tenant, quindi comparire qui non significa comparire a schermo.
 */
export function editRegistry(content, ctx) {
  const importLine = `import { ${ctx.APP_CAMEL}Manifest } from '../modules/${ctx.APP_ID}/manifest'`
  if (content.includes(importLine)) {
    throw new Error(`registry.ts importa già il manifest di ${ctx.APP_ID}`)
  }

  // L'import va dopo l'ultimo import di manifest, così l'elenco resta leggibile.
  const importRe = /^import \{ \w+Manifest \} from '\.\.\/modules\/[a-z0-9_]+\/manifest'$/gm
  const matches = [...content.matchAll(importRe)]
  if (matches.length === 0) {
    throw new Error('registry.ts: nessun import di manifest trovato (formato cambiato?)')
  }
  const last = matches[matches.length - 1]
  const insertAt = last.index + last[0].length
  let next = content.slice(0, insertAt) + '\n' + importLine + content.slice(insertAt)

  // Voce nell'array MODULES.
  const arrayRe = /(export const MODULES: ModuleManifest\[\] = \[)([^\]]*)(\])/
  if (!arrayRe.test(next)) {
    throw new Error('registry.ts: array MODULES non trovato (formato cambiato?)')
  }
  next = next.replace(arrayRe, (_m, open, inner, close) => {
    const trimmed = inner.trim()
    const separator = trimmed === '' ? '' : ', '
    return `${open}${trimmed}${separator}${ctx.APP_CAMEL}Manifest${close}`
  })
  return next
}

/**
 * Aggiunge lo script che rigenera i tipi del client dall'OpenAPI del servizio.
 * Il file si rilegge e riscrive come JSON: incollare una riga a mano in un
 * package.json è il modo più semplice per produrre un file non parsabile.
 */
export function editBackofficePackageJson(content, ctx) {
  const pkg = JSON.parse(content)
  const scriptName = `gen:${ctx.APP_ID}`
  if (pkg.scripts?.[scriptName]) {
    throw new Error(`package.json ha già lo script ${scriptName}`)
  }
  pkg.scripts = pkg.scripts ?? {}
  pkg.scripts[scriptName] =
    `openapi-typescript ../../../services/${ctx.APP_ID}/src/main/resources/META-INF/openapi/openapi.yaml`
    + ` -o src/modules/${ctx.APP_ID}/api/schema.ts`
  return JSON.stringify(pkg, null, 2) + '\n'
}

/** Registra l'app nel listino pricing-as-code caricato dal core. */
export function editPricingIndex(content, ctx) {
  const entry = `  - ${ctx.APP_ID}`
  const lines = content.split('\n')
  if (lines.some((line) => line.trimEnd() === entry)) {
    throw new Error(`pricing/index.yaml elenca già ${ctx.APP_ID}`)
  }
  const appsAt = lines.findIndex((line) => line.trimEnd() === 'apps:')
  if (appsAt < 0) {
    throw new Error("pricing/index.yaml: chiave `apps:` non trovata (formato cambiato?)")
  }
  // Ultima riga dell'elenco `apps:` (le voci iniziano con "  - ").
  let insertAt = appsAt + 1
  while (insertAt < lines.length && lines[insertAt].startsWith('  - ')) insertAt += 1
  lines.splice(insertAt, 0, entry)
  return lines.join('\n')
}

/**
 * Dichiara le tre code dell'app nello stack locale (≈SQS via ElasticMQ), ognuna
 * con la propria coda degli scarti. Nel cloud le stesse code nascono dal modulo
 * Terraform `microsaas_app`: qui si replica solo l'ambiente di sviluppo.
 *
 * La coda degli scarti non è un ornamento: un'invalidazione persa significa
 * servire diritti vecchi senza accorgersene, ed è esattamente il guasto che la
 * proiezione locale potrebbe nascondere.
 */
export function editElasticMq(content, ctx) {
  const id = ctx.APP_ID
  if (content.includes(`entitlement-${id} {`)) {
    throw new Error(`dev/elasticmq.conf dichiara già le code di ${id}`)
  }

  const block = [
    ``,
    `    # ── code dell'app ${id} (generate da tools/new-application, UC 0046) ──`,
    `    tenant-purge-${id} {`,
    `        deadLettersQueue { name = "tenant-purge-${id}-dlq", maxReceiveCount = 5 }`,
    `    }`,
    `    tenant-purge-${id}-dlq { }`,
    `    gdpr-export-${id} {`,
    `        deadLettersQueue { name = "gdpr-export-${id}-dlq", maxReceiveCount = 5 }`,
    `    }`,
    `    gdpr-export-${id}-dlq { }`,
    `    entitlement-${id} {`,
    `        deadLettersQueue { name = "entitlement-${id}-dlq", maxReceiveCount = 5 }`,
    `    }`,
    `    entitlement-${id}-dlq { }`,
  ].join('\n')

  // Il blocco va in fondo a `queues { … }`. La chiusura si trova CONTANDO le graffe a
  // partire dall'apertura di `queues`, non cercando l'ultima `}` del file: il formato
  // HOCON ha altri blocchi di primo livello (`node-address`, `rest-sqs`) e una ricerca
  // "l'ultima graffa che sembra giusta" inserirebbe le code nel blocco sbagliato senza
  // dare errore — un guasto silenzioso, il peggiore possibile in un file di
  // configurazione che nessuno rilegge.
  const lines = content.split('\n')
  const openAt = lines.findIndex((line) => /^\s*queues\s*\{\s*$/.test(line))
  if (openAt < 0) {
    throw new Error('dev/elasticmq.conf: blocco `queues {` non trovato (formato cambiato?)')
  }
  let depth = 0
  let closeAt = -1
  for (let i = openAt; i < lines.length; i += 1) {
    for (const ch of lines[i]) {
      if (ch === '{') depth += 1
      else if (ch === '}') depth -= 1
    }
    if (depth === 0) {
      closeAt = i
      break
    }
  }
  if (closeAt < 0) {
    throw new Error(
      'dev/elasticmq.conf: il blocco `queues` non si chiude (graffe sbilanciate). '
      + 'Nessuna modifica applicata: correggere il file a mano.',
    )
  }
  lines.splice(closeAt, 0, ...block.split('\n'))
  return lines.join('\n')
}

/** Mappa percorso → funzione di modifica, nell'ordine di EDITED_FILES. */
export const EDITORS = {
  'services/pom.xml': editServicesPom,
  'frontend/apps/backoffice/src/registry/registry.ts': editRegistry,
  'frontend/apps/backoffice/package.json': editBackofficePackageJson,
  'services/core/src/main/resources/pricing/index.yaml': editPricingIndex,
  'dev/elasticmq.conf': editElasticMq,
}
