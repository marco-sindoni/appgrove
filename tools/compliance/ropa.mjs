#!/usr/bin/env node
// CLI dell'automazione RoPA (UC 0030): `assemble` genera docs/compliance/ropa.<lang>.md dai
// manifesti dati; `check` verifica parità lingue + freshness dei file generati (exit != 0 se rosso).

import { readdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'yaml';
import { LABELS, renderRopa, ropaFileName, validateManifests } from './lib.mjs';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const MANIFESTS_DIR = join(ROOT, 'docs', 'compliance', 'manifests');
const COMPLIANCE_DIR = join(ROOT, 'docs', 'compliance');

function load() {
  const config = parse(readFileSync(join(MANIFESTS_DIR, '_config.yaml'), 'utf8'));
  const manifests = readdirSync(MANIFESTS_DIR)
    .filter((f) => f.endsWith('.yaml') && !f.startsWith('_'))
    .sort()
    .map((f) => ({ file: `manifests/${f}`, data: parse(readFileSync(join(MANIFESTS_DIR, f), 'utf8')) }));
  return { config, manifests };
}

function assemble() {
  const { config, manifests } = load();
  bail(validateManifests(manifests, config));
  for (const lang of config.required_languages) {
    const file = join(COMPLIANCE_DIR, ropaFileName(lang));
    writeFileSync(file, renderRopa(manifests, lang) + '\n');
    console.log(`✓ generato ${file}`);
  }
}

function check() {
  const { config, manifests } = load();
  const errors = validateManifests(manifests, config);
  if (errors.length === 0) {
    for (const lang of config.required_languages) {
      const file = join(COMPLIANCE_DIR, ropaFileName(lang));
      const expected = renderRopa(manifests, lang) + '\n';
      if (!existsSync(file)) {
        errors.push(`${ropaFileName(lang)} assente: rigenera con \`npm run assemble\` (tools/compliance)`);
      } else if (readFileSync(file, 'utf8') !== expected) {
        errors.push(
          `${ropaFileName(lang)} NON allineato ai manifesti (drift): rigenera con \`npm run assemble\` e committa`,
        );
      }
    }
  }
  bail(errors);
  console.log(`✓ manifesti ok (lingue: ${config.required_languages.join(', ')}) e RoPA allineato`);
}

function bail(errors) {
  if (errors.length > 0) {
    console.error('✗ check compliance FALLITO (#13 C14, UC 0030):');
    for (const e of errors) console.error(`  - ${e}`);
    process.exit(1);
  }
}

const command = process.argv[2];
if (command === 'assemble') assemble();
else if (command === 'check') check();
else {
  console.error(`uso: node ropa.mjs <assemble|check>  (lingue supportate: ${Object.keys(LABELS).join(', ')})`);
  process.exit(2);
}
