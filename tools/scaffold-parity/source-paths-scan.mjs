#!/usr/bin/env node
// Rilevatore dei percorsi-sorgente (UC 0046, strato 2 del presidio anti-invecchiamento).
//
// Dato un insieme di percorsi modificati, segnala l'intersezione con l'elenco dichiarato in
// source-paths.json — cioè i percorsi da cui derivano i modelli della skill `new-application`.
// Se c'è intersezione, il varco di `new-change` (step-04) obbliga a una delle due: aggiornare i
// modelli nello stesso commit, oppure registrare la motivazione in docs/_PARITA-SCAFFOLD.md.
//
// Uso: node source-paths-scan.mjs [range-git] [--paths <p1> <p2> …] [--json]
//   senza argomenti → percorsi da merge-base(main, HEAD) al working tree, file non tracciati inclusi;
//   con range       → passato a `git diff --name-only` così com'è (es. main...HEAD);
//   con --paths     → percorsi espliciti, senza toccare git (utile nei test e in altri strumenti).
// Exit: 0 = nessuna intersezione · 1 = intersezione (il varco deve scattare)

import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const QUI = dirname(fileURLToPath(import.meta.url));

export function caricaElenco() {
  return JSON.parse(readFileSync(join(QUI, 'source-paths.json'), 'utf8'));
}

// Un percorso modificato tocca un percorso-sorgente se coincide con esso o gli sta dentro.
export function tocca(percorsoModificato, percorsoSorgente) {
  const m = percorsoModificato.replaceAll('\\', '/').replace(/^\.\//, '');
  return m === percorsoSorgente || m.startsWith(`${percorsoSorgente}/`);
}

// → [{ percorso, perche, file: [...] }] per i soli percorsi-sorgente effettivamente toccati.
export function intersezione(percorsiModificati, elenco = caricaElenco()) {
  const colpiti = [];
  for (const voce of elenco.percorsi) {
    const file = percorsiModificati.filter((p) => tocca(p, voce.percorso)).sort();
    if (file.length > 0) colpiti.push({ percorso: voce.percorso, perche: voce.perche, file });
  }
  return colpiti;
}

export function renderReport(colpiti) {
  if (colpiti.length === 0) {
    return '✓ nessun percorso-sorgente dei modelli toccato — il varco di `new-change` non scatta';
  }
  const totale = colpiti.reduce((n, c) => n + c.file.length, 0);
  const righe = [
    `✗ ${colpiti.length} percorso/i-sorgente dei modelli toccato/i (${totale} file) — il varco di \`new-change\` scatta:`,
  ];
  for (const c of colpiti) {
    righe.push(`\n${c.percorso}`);
    righe.push(`  perché è sorgente: ${c.perche}`);
    for (const f of c.file.slice(0, 12)) righe.push(`  - ${f}`);
    if (c.file.length > 12) righe.push(`  … e altri ${c.file.length - 12} file`);
  }
  righe.push(
    '',
    'Che fare — una delle due, mai nessuna delle due:',
    '  1. AGGIORNA I MODELLI in tools/new-application/templates/ nello stesso commit, così che le app',
    '     nuove nascano con la stessa evoluzione. Verifica con `npm run parity --prefix tools/scaffold-parity`.',
    '  2. REGISTRA LA MOTIVAZIONE in docs/_PARITA-SCAFFOLD.md se il cambiamento è specifico del dominio',
    '     di `fatture` (o comunque non generalizzabile) e i modelli devono restare indietro di proposito.',
    '',
    'Non chiudere la change lasciando la scelta implicita: un modello che invecchia in silenzio non',
    'rompe nulla oggi e fa nascere antiquate tutte le app di domani.',
  );
  return righe.join('\n');
}

function percorsiDaGit(range) {
  const git = (args) => execFileSync('git', args, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
  if (range) return git(['diff', '--name-only', range]).split('\n').filter(Boolean);
  const base = git(['merge-base', 'main', 'HEAD']).trim();
  const modificati = git(['diff', '--name-only', base]).split('\n').filter(Boolean);
  const nonTracciati = git(['ls-files', '--others', '--exclude-standard']).split('\n').filter(Boolean);
  return [...new Set([...modificati, ...nonTracciati])];
}

function main() {
  const argv = process.argv.slice(2);
  const json = argv.includes('--json');
  const iPaths = argv.indexOf('--paths');

  let percorsi;
  let origine;
  if (iPaths !== -1) {
    percorsi = argv.slice(iPaths + 1).filter((a) => !a.startsWith('--'));
    origine = 'percorsi espliciti';
  } else {
    const range = argv.find((a) => !a.startsWith('--'));
    percorsi = percorsiDaGit(range);
    origine = range ?? 'merge-base(main, HEAD) + working tree';
  }

  const colpiti = intersezione(percorsi);
  if (json) console.log(JSON.stringify({ origine, esaminati: percorsi.length, colpiti }, null, 2));
  else console.log(renderReport(colpiti));
  process.exit(colpiti.length > 0 ? 1 : 0);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main();
