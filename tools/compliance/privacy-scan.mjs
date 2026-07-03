#!/usr/bin/env node
// Scanner segnali privacy (UC 0031): analizza il diff di una change e rileva i segnali che innescano
// il gate privacy/RoPA di `new-change` (#13 C16): nuove tabelle/colonne nelle migrazioni Flyway, nuovi
// campi entità/DTO, nuove dipendenze/host esterni (potenziale sub-processor), classificazioni toccate
// nei manifesti dati. Informativo per il co-pilota: exit 1 = segnali trovati (NON è il build-gate,
// quello è il verifier @PersonalData↔manifesto di UC 0030).
//
// Uso: node privacy-scan.mjs [range-git] [--json]
//   senza range → diff da merge-base(main, HEAD) al working tree, inclusi i file non tracciati;
//   con range   → passato a `git diff` così com'è (es. main...HEAD).

import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

const MIGRATION_FILE = /\/db\/migration\/[^/]+\.sql$/;
const JAVA_MAIN_FILE = /src\/main\/java\/.+\.java$/;
const MANIFEST_FILE = /^docs\/compliance\/manifests\/(?!_)[^/]+\.ya?ml$/;
const CONFIG_FILE = /\.(properties|ya?ml|env|toml|conf)$/;

const MIGRATION_DDL = [/\bCREATE\s+TABLE\b/i, /\bADD\s+COLUMN\b/i, /\bALTER\s+TABLE\b.+\bADD\b/i];
// Dichiarazione di campo Java: modificatore + tipo + nome; esclude costanti statiche e metodi (parentesi).
const JAVA_FIELD = /^\s*(?:private|protected|public)\s+(?!static\b)[\w.<>\[\], ?]+\s+\w+\s*(?:=[^;]*)?;\s*$/;
const MANIFEST_KEY = /^\s*-?\s*(retention|purpose|legal_basis)\s*:/;
const POM_ARTIFACT = /^\s*<artifactId>([^<]+)<\/artifactId>/;
const NPM_DEP = /^\s*"([^"]+)"\s*:\s*"((?:[~^><=]|\d|latest|workspace:|file:|npm:|git)[^"]*)"/;
const NPM_NON_DEP_KEYS = new Set(['version']);
const URL_HOST = /https?:\/\/([\w.-]+)/g;
const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '0.0.0.0', 'host.docker.internal']);

export const SIGNAL_LABELS = {
  migration: 'Migrazioni Flyway — nuove tabelle/colonne',
  'entity-field': 'Nuovi campi entità/DTO (src/main/java)',
  dependency: 'Nuove dipendenze (potenziale sub-processor)',
  'external-host': 'Nuovi host esterni in config (potenziale sub-processor)',
  'manifest-classification': 'Manifesti dati — classificazioni toccate (retention/purpose/legal_basis)',
};

// Parsa un diff unificato in file con righe aggiunte/rimosse.
export function parseDiff(diffText) {
  const files = [];
  let cur = null;
  for (const line of diffText.split('\n')) {
    if (line.startsWith('diff --git ')) {
      cur = { file: (line.match(/ b\/(.+)$/) ?? [])[1] ?? null, isNew: false, added: [], removed: [] };
      files.push(cur);
    } else if (!cur) {
      continue;
    } else if (line.startsWith('new file mode')) cur.isNew = true;
    else if (line.startsWith('+++ b/')) cur.file = line.slice(6);
    else if (line.startsWith('+') && !line.startsWith('+++')) cur.added.push(line.slice(1));
    else if (line.startsWith('-') && !line.startsWith('---')) cur.removed.push(line.slice(1));
  }
  return files.filter((f) => f.file);
}

// Applica le euristiche di segnale (#13 C16) ai file del diff → [{ type, file, detail }].
export function scanFiles(files) {
  const signals = [];
  const add = (type, file, detail) => signals.push({ type, file, detail: detail.trim() });

  for (const f of files) {
    if (MIGRATION_FILE.test(f.file)) {
      for (const line of f.added) if (MIGRATION_DDL.some((re) => re.test(line))) add('migration', f.file, line);
    }

    if (JAVA_MAIN_FILE.test(f.file)) {
      for (const line of f.added) {
        if (!line.includes('(') && JAVA_FIELD.test(line)) add('entity-field', f.file, line);
      }
    }

    const base = f.file.split('/').pop();
    if (base === 'pom.xml') {
      for (const line of f.added) {
        const m = line.match(POM_ARTIFACT);
        if (m) add('dependency', f.file, m[1]);
      }
    } else if (base === 'package.json') {
      for (const line of f.added) {
        const m = line.match(NPM_DEP);
        if (m && !NPM_NON_DEP_KEYS.has(m[1])) add('dependency', f.file, `${m[1]}@${m[2]}`);
      }
    }

    if (CONFIG_FILE.test(f.file) && !f.file.startsWith('docs/')) {
      for (const line of f.added) {
        for (const m of line.matchAll(URL_HOST)) {
          if (!LOCAL_HOSTS.has(m[1])) add('external-host', f.file, m[1]);
        }
      }
    }

    if (MANIFEST_FILE.test(f.file)) {
      if (f.isNew) add('manifest-classification', f.file, 'nuovo manifesto');
      const addedKeys = f.added.filter((l) => MANIFEST_KEY.test(l));
      for (const line of addedKeys) add('manifest-classification', f.file, line);
      for (const line of f.removed) {
        const key = (line.match(MANIFEST_KEY) ?? [])[1];
        if (key && !addedKeys.some((l) => l.match(MANIFEST_KEY)?.[1] === key)) {
          add('manifest-classification', f.file, `rimossa: ${line.trim()}`);
        }
      }
    }
  }
  return signals;
}

export function scanDiff(diffText) {
  return scanFiles(parseDiff(diffText));
}

export function renderReport(signals) {
  if (signals.length === 0) return '✓ nessun segnale privacy nel diff (gate UC 0031)';
  const lines = [`✗ ${signals.length} segnale/i privacy nel diff — serve il gate privacy/RoPA (UC 0031):`];
  for (const type of Object.keys(SIGNAL_LABELS)) {
    const group = signals.filter((s) => s.type === type);
    if (group.length === 0) continue;
    lines.push(`\n${SIGNAL_LABELS[type]}:`);
    for (const s of group) lines.push(`  - ${s.file}: ${s.detail}`);
  }
  lines.push('\n→ classifica col co-pilota di `new-change` (step-03): natura/finalità/base/retention,');
  lines.push('  aggiorna manifesto + RoPA (`npm run assemble`), annota `@PersonalData`, valuta MAJOR/MINOR.');
  return lines.join('\n');
}

function gitDiffDefault() {
  const git = (args) => execFileSync('git', args, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
  const base = git(['merge-base', 'main', 'HEAD']).trim();
  const files = parseDiff(git(['diff', base]));
  // I file non ancora tracciati non compaiono in `git diff`: li aggiungiamo come interamente nuovi.
  for (const f of git(['ls-files', '--others', '--exclude-standard']).split('\n').filter(Boolean)) {
    try {
      files.push({ file: f, isNew: true, added: readFileSync(f, 'utf8').split('\n'), removed: [] });
    } catch {
      /* file sparito nel frattempo: ignora */
    }
  }
  return files;
}

function main() {
  const argv = process.argv.slice(2);
  const json = argv.includes('--json');
  const range = argv.find((a) => !a.startsWith('--'));

  const files = range
    ? parseDiff(execFileSync('git', ['diff', range], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }))
    : gitDiffDefault();
  const signals = scanFiles(files);

  if (json) console.log(JSON.stringify({ range: range ?? 'merge-base(main, HEAD) + working tree', signals }, null, 2));
  else console.log(renderReport(signals));
  process.exit(signals.length > 0 ? 1 : 0);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main();
