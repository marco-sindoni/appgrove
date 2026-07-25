// Test unitari delle funzioni inverse e della loro copertura rispetto al generatore.
import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  UNEDITORS,
  UNEDITED_FILES,
  uneditServicesPom,
  uneditPricingIndex,
  uneditElasticMq,
  uneditRegistry,
  uneditBackofficePackageJson,
  uneditLandingsIndex,
} from '../lib/unedits.mjs'
import { EDITORS, EDITED_FILES } from '../../new-application/lib/edits.mjs'

const CTX = { APP_ID: 'demo9', APP_CAMEL: 'demo9' }

// ── Parità con il generatore: nessun file condiviso senza inverso ────────────
test('UNEDITORS copre esattamente gli stessi file di EDITORS del generatore', () => {
  assert.deepEqual(new Set(Object.keys(UNEDITORS)), new Set(Object.keys(EDITORS)))
  assert.deepEqual(UNEDITED_FILES, EDITED_FILES)
})

// ── Ogni inverso è l'inverso del diretto (round-trip su contenuto sintetico) ──
test('services/pom.xml: unedit annulla edit', () => {
  const before = '  <modules>\n    <module>core</module>\n  </modules>\n'
  const edited = EDITORS['services/pom.xml'](before, CTX)
  assert.ok(edited.includes('<module>demo9</module>'))
  const { content, changed } = uneditServicesPom(edited, CTX)
  assert.equal(changed, true)
  assert.equal(content, before)
})

test('pricing/index.yaml: unedit annulla edit', () => {
  const before = 'apps:\n  - fatture\n  - crm\n'
  const edited = EDITORS['services/core/src/main/resources/pricing/index.yaml'](before, CTX)
  assert.ok(edited.includes('  - demo9'))
  const { content, changed } = uneditPricingIndex(edited, CTX)
  assert.equal(changed, true)
  assert.equal(content, before)
})

test('registry.ts: unedit annulla edit (import + voce array)', () => {
  const before = [
    "import { fattureManifest } from '../modules/fatture/manifest'",
    '',
    'export const MODULES: ModuleManifest[] = [fattureManifest]',
    '',
  ].join('\n')
  const edited = EDITORS['frontend/apps/backoffice/src/registry/registry.ts'](before, CTX)
  assert.ok(edited.includes("modules/demo9/manifest"))
  assert.ok(edited.includes('demo9Manifest'))
  const { content, changed } = uneditRegistry(edited, CTX)
  assert.equal(changed, true)
  assert.equal(content, before)
})

test('package.json: unedit annulla edit', () => {
  const before = JSON.stringify({ scripts: { build: 'x' } }, null, 2) + '\n'
  const edited = EDITORS['frontend/apps/backoffice/package.json'](before, CTX)
  assert.ok(JSON.parse(edited).scripts['gen:demo9'])
  const { content, changed } = uneditBackofficePackageJson(edited, CTX)
  assert.equal(changed, true)
  assert.equal(content, before)
})

test('elasticmq.conf: unedit annulla edit', () => {
  const before = [
    '    queues {',
    '        paddle-webhooks { }',
    '    }',
    '',
    '    node-address { }',
    '',
  ].join('\n')
  const edited = EDITORS['dev/elasticmq.conf'](before, CTX)
  assert.ok(edited.includes(`code dell'app demo9`))
  const { content, changed } = uneditElasticMq(edited, CTX)
  assert.equal(changed, true)
  assert.equal(content, before)
})

test('landings/index.ts: unedit annulla edit (import + voce array)', () => {
  const before = [
    "import type { Landing } from './types.ts'",
    "import { en } from './example/en.ts'",
    '',
    "const example: Landing = { appId: 'example', status: 'draft', content: { en } as never }",
    '',
    'export const LANDINGS: Landing[] = [example]',
    '',
  ].join('\n')
  const edited = EDITORS['site/src/content/landings/index.ts'](before, CTX)
  assert.ok(edited.includes("from './demo9/index.ts'"))
  assert.ok(edited.includes('demo9Landing'))
  const { content, changed } = uneditLandingsIndex(edited, CTX)
  assert.equal(changed, true)
  assert.equal(content, before)
})

// ── Idempotenza: disfare due volte è sicuro (seconda volta = nessun cambiamento) ──
test('tutte le unedit sono idempotenti: assenza del marcatore → changed:false', () => {
  const clean = {
    'services/pom.xml': '  <modules>\n    <module>core</module>\n  </modules>\n',
    'services/core/src/main/resources/pricing/index.yaml': 'apps:\n  - fatture\n',
    'frontend/apps/backoffice/src/registry/registry.ts':
      "import { fattureManifest } from '../modules/fatture/manifest'\n\nexport const MODULES: ModuleManifest[] = [fattureManifest]\n",
    'frontend/apps/backoffice/package.json': JSON.stringify({ scripts: { build: 'x' } }, null, 2) + '\n',
    'dev/elasticmq.conf': '    queues {\n        paddle-webhooks { }\n    }\n',
    'site/src/content/landings/index.ts':
      "import type { Landing } from './types.ts'\nimport { en } from './example/en.ts'\n\nexport const LANDINGS: Landing[] = [example]\n",
  }
  for (const [file, content] of Object.entries(clean)) {
    const { changed } = UNEDITORS[file](content, CTX)
    assert.equal(changed, false, `${file} dovrebbe essere no-op quando l'app non è presente`)
  }
})
