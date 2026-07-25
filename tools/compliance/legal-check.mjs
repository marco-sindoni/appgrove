#!/usr/bin/env node
// CLI del check dei documenti legali pubblici (UC 0002): legge content/legal/, ne parsa il
// frontmatter e valida parità lingue + frontmatter + integrità dei token verso entity.yaml.
// Exit != 0 se rosso. Logica pura in legal.mjs.

import { readdirSync, readFileSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { parse } from 'yaml';
import { parseFileName, validateLegal } from './legal.mjs';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const LEGAL_DIR = join(ROOT, 'content', 'legal');

/** Splitta il frontmatter YAML (blocco fra i delimitatori ---) dal corpo del markdown. */
export function splitFrontmatter(raw) {
  const m = /^---\r?\n([\s\S]*?)\r?\n---\r?\n?([\s\S]*)$/.exec(raw);
  if (!m) return { frontmatter: null, body: raw };
  let frontmatter = null;
  try {
    frontmatter = parse(m[1]) ?? {};
  } catch {
    frontmatter = null; // frontmatter YAML non valido → lo tratta come assente (errore a valle)
  }
  return { frontmatter, body: m[2] };
}

function load() {
  if (!existsSync(LEGAL_DIR)) {
    return { config: null, entity: null, docs: [], fatal: `content/legal/ assente` };
  }
  const config = existsSync(join(LEGAL_DIR, '_config.yaml'))
    ? parse(readFileSync(join(LEGAL_DIR, '_config.yaml'), 'utf8'))
    : null;
  const entity = existsSync(join(LEGAL_DIR, 'entity.yaml'))
    ? parse(readFileSync(join(LEGAL_DIR, 'entity.yaml'), 'utf8'))
    : null;
  const docs = readdirSync(LEGAL_DIR)
    .filter((f) => f.endsWith('.md') && f !== 'README.md')
    .map((f) => {
      const meta = parseFileName(f);
      if (!meta) return { file: `content/legal/${f}`, component: '?', lang: '?', frontmatter: null, body: '' };
      const { frontmatter, body } = splitFrontmatter(readFileSync(join(LEGAL_DIR, f), 'utf8'));
      return { file: `content/legal/${f}`, component: meta.component, lang: meta.lang, frontmatter, body };
    });
  return { config, entity, docs, fatal: null };
}

function check() {
  const { config, entity, docs, fatal } = load();
  const errors = [];
  if (fatal) errors.push(fatal);
  if (!config) errors.push('content/legal/_config.yaml assente');
  if (!entity) errors.push('content/legal/entity.yaml assente');

  let warnings = [];
  if (errors.length === 0) {
    const res = validateLegal(docs, config, entity);
    errors.push(...res.errors);
    warnings = res.warnings;
  }

  for (const w of warnings) console.warn(`  ⚠ ${w}`);
  if (errors.length > 0) {
    console.error('✗ check documenti legali FALLITO (UC 0002, #14 C13 / #13 G38):');
    for (const e of errors) console.error(`  - ${e}`);
    process.exit(1);
  }
  const langs = config.required_languages.join(', ');
  console.log(`✓ documenti legali ok (componenti: ${config.components.join(', ')}; lingue: ${langs})`);
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  check();
}
