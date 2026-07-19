#!/usr/bin/env node
// Collaudo di parità modelli-sorgente ↔ app #1 (UC 0046, strato 1 del presidio anti-invecchiamento).
//
// Il rischio della skill `new-application` non è che si rompa, ma che INVECCHI IN SILENZIO: `fatture`
// evolve, i modelli in tools/new-application/templates/ restano indietro, e ogni app nuova nasce già
// antiquata senza che nessun test diventi rosso. Questo confronto è strutturale, non testuale:
//
//   1. stesso insieme di file (a meno dei segnaposto nei nomi)
//   2. stesse dipendenze dichiarate nel pom.xml
//   3. stesse annotazioni portanti sulle classi corrispondenti
//   4. stesse chiavi di application.properties
//
// Le deviazioni CONSAPEVOLI si dichiarano in docs/_PARITA-SCAFFOLD.md (strato 3), non qui: ogni
// divergenza segnalata stampa la chiave di deroga già pronta da incollare nel registro.
//
// Uso: node parity-check.mjs [--json]
// Exit: 0 = parità · 1 = divergenze non derogate · 2 = modelli-sorgente non ancora presenti

import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const QUI = dirname(fileURLToPath(import.meta.url));
export const RADICE = resolve(QUI, '..', '..');

// I marcatori delimitano, nel registro, la tabella delle deroghe attive: fuori da essi il file è
// prosa libera (storico, spiegazioni) e non viene interpretato.
export const DEROGHE_INIZIO = '<!-- deviazioni:inizio -->';
export const DEROGHE_FINE = '<!-- deviazioni:fine -->';

const SEGNAPOSTO_RESIDUO = /@@[A-Za-z0-9_]+@@|__[A-Za-z0-9_]+__|\{\{[^}]+\}\}|<[A-Za-z0-9_]+>/g;
const CARTELLE_SALTATE = new Set(['node_modules', '.git', 'target', 'dist', 'build']);

export const ETICHETTE = {
  'file-mancante': 'File presenti nell’app #1 e assenti dal modello',
  'file-extra': 'File presenti nel modello e assenti dall’app #1',
  'segnaposto-ignoto': 'Segnaposto non riconosciuti nei nomi dei file del modello',
  'dep-mancante': 'Dipendenze Maven dichiarate dall’app #1 e assenti dal modello',
  'dep-extra': 'Dipendenze Maven dichiarate dal modello e assenti dall’app #1',
  'prop-mancante': 'Chiavi di application.properties presenti nell’app #1 e assenti dal modello',
  'prop-extra': 'Chiavi di application.properties presenti nel modello e assenti dall’app #1',
  annotazione: 'Annotazioni portanti divergenti sulle classi corrispondenti',
};

// ── Utilità pure (esportate per i test) ──────────────────────────────────────────────────────────

// Sostituisce i token del generatore col valore corrispondente nell'app #1.
export function sostituisciSegnaposto(testo, segnaposto) {
  let out = testo;
  for (const [chiave, def] of Object.entries(segnaposto)) {
    if (chiave.startsWith('_') || !Array.isArray(def?.token)) continue; // chiavi `_commento`
    for (const token of def.token) out = out.split(token).join(def.valore);
  }
  return out;
}

// Nome del file del modello → nome atteso nell'app #1: segnaposto risolti, dominio di esempio
// ricondotto al dominio reale dell'app #1, suffisso di modello tolto.
export function normalizzaPercorsoModello(rel, cfg) {
  let out = sostituisciSegnaposto(rel, cfg.segnaposto);
  for (const [esempio, reale] of Object.entries(cfg.dominio ?? {})) {
    if (esempio.startsWith('_')) continue;
    out = out.split(esempio).join(reale);
  }
  for (const suf of cfg.suffissiModello) {
    if (out.endsWith(suf)) {
      out = out.slice(0, -suf.length);
      break;
    }
  }
  return out;
}

// Glob minimale: `**` attraversa le cartelle, `*` no.
export function corrisponde(percorso, schema) {
  const re = new RegExp(
    '^' +
      schema
        .split('**')
        .map((p) => p.replace(/[.+^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '[^/]*'))
        .join('.*') +
      '$',
  );
  return re.test(percorso);
}

// Elenca ricorsivamente i file di una cartella, in percorsi relativi a essa.
export function elencaFile(radice, ignora = []) {
  const out = [];
  const visita = (dir) => {
    for (const voce of readdirSync(dir, { withFileTypes: true })) {
      if (voce.isDirectory()) {
        if (!CARTELLE_SALTATE.has(voce.name)) visita(join(dir, voce.name));
        continue;
      }
      const rel = relative(radice, join(dir, voce.name));
      if (!ignora.some((s) => corrisponde(rel, s))) out.push(rel);
    }
  };
  visita(radice);
  return out.sort();
}

// `groupId:artifactId` di ogni <dependency> del pom (parsing volutamente ingenuo: i pom del monorepo
// sono generati da modello e non usano profili o import condizionati).
export function dipendenzePom(xml) {
  const deps = new Set();
  for (const blocco of xml.match(/<dependency>[\s\S]*?<\/dependency>/g) ?? []) {
    const g = blocco.match(/<groupId>([^<]+)<\/groupId>/)?.[1]?.trim();
    const a = blocco.match(/<artifactId>([^<]+)<\/artifactId>/)?.[1]?.trim();
    if (g && a) deps.add(`${g}:${a}`);
  }
  return deps;
}

// Chiavi di un application.properties, profili inclusi (`%dev.` resta parte della chiave: un blocco
// presente solo in un profilo è a tutti gli effetti una convenzione da replicare).
export function chiaviProperties(testo) {
  const chiavi = new Set();
  for (const riga of testo.split('\n')) {
    const t = riga.trim();
    if (!t || t.startsWith('#') || t.startsWith('!')) continue;
    const i = t.indexOf('=');
    if (i > 0) chiavi.add(t.slice(0, i).trim());
  }
  return chiavi;
}

// Annotazioni portanti presenti in un file Java (solo quelle dichiarate in configurazione: le altre
// sono dettaglio di dominio e non fanno parte del pattern).
export function annotazioniFile(testo, portanti) {
  const insieme = new Set();
  for (const m of testo.matchAll(/@([A-Z][A-Za-z0-9_]*)/g)) {
    if (portanti.includes(m[1])) insieme.add(m[1]);
  }
  return insieme;
}

// Chiavi di deroga attive dichiarate nel registro (strato 3), fra i due marcatori.
export function leggiDeroghe(markdown) {
  const da = markdown.indexOf(DEROGHE_INIZIO);
  const a = markdown.indexOf(DEROGHE_FINE);
  if (da === -1 || a === -1 || a < da) return new Set();
  const chiavi = new Set();
  for (const m of markdown.slice(da, a).matchAll(/`((?:file|file-extra|dep|prop|ann):[^`]+)`/g)) {
    chiavi.add(m[1].trim());
  }
  return chiavi;
}

export function differenza(a, b) {
  return [...a].filter((x) => !b.has(x)).sort();
}

// ── Confronto ────────────────────────────────────────────────────────────────────────────────────

// Individua la radice del modello di una coppia: la cartella, sotto templatesRoot, che contiene il
// file marcatore (pom.xml, manifest.ts…). Così il collaudo non dipende da come il generatore ha
// scelto di chiamare le sue cartelle.
export function trovaRadiceModello(templatesAbs, coppia, cfg) {
  const candidate = new Set();
  for (const rel of elencaFile(templatesAbs)) {
    const nome = normalizzaPercorsoModello(rel, cfg).split('/').pop();
    if (nome === coppia.marcatore) candidate.add(dirname(rel) === '.' ? '' : dirname(rel));
  }
  return [...candidate].sort((x, y) => x.length - y.length);
}

export function confrontaCoppia(coppia, cfg, templatesAbs) {
  const div = [];
  const nota = (tipo, chiave, dettaglio = '') => div.push({ tipo, chiave, dettaglio, coppia: coppia.id });

  const radici = trovaRadiceModello(templatesAbs, coppia, cfg);
  if (radici.length === 0) {
    nota('file-mancante', `file:${coppia.riferimento}/${coppia.marcatore}`, `nessun modello con marcatore \`${coppia.marcatore}\` sotto ${cfg.templatesRoot}`);
    return div;
  }
  const modelloRel = radici[0];
  const modelloAbs = join(templatesAbs, modelloRel);
  const rifAbs = resolve(RADICE, coppia.riferimento);
  const percorsoModello = (rel) => join(cfg.templatesRoot, modelloRel, rel).replaceAll('\\', '/');
  const percorsoRif = (rel) => `${coppia.riferimento}/${rel}`;

  const fileRif = elencaFile(rifAbs, coppia.ignora ?? []);
  const fileModello = elencaFile(modelloAbs, coppia.ignora ?? []);

  // mappa "percorso in termini dell'app #1" → "percorso reale nel modello"
  const mappa = new Map();
  for (const rel of fileModello) {
    const normalizzato = normalizzaPercorsoModello(rel, cfg);
    mappa.set(normalizzato, rel);
    const residui = normalizzato.match(SEGNAPOSTO_RESIDUO);
    if (residui) {
      nota('segnaposto-ignoto', `file-extra:${percorsoModello(rel)}`, `token non mappati: ${[...new Set(residui)].join(', ')}`);
    }
  }

  // 1. insieme dei file
  for (const rel of fileRif) {
    if (!mappa.has(rel)) nota('file-mancante', `file:${percorsoRif(rel)}`, 'presente nell’app #1, assente dal modello');
  }
  for (const [normalizzato, rel] of mappa) {
    if (!fileRif.includes(normalizzato)) {
      nota('file-extra', `file-extra:${percorsoModello(rel)}`, `nessun corrispondente in ${coppia.riferimento}`);
    }
  }

  const controlli = coppia.controlli ?? [];
  const leggi = (base, rel) => {
    const p = join(base, rel);
    return existsSync(p) && statSync(p).isFile() ? readFileSync(p, 'utf8') : null;
  };
  const relModello = (relRif) => mappa.get(relRif);

  // 2. dipendenze Maven
  if (controlli.includes('pom') && coppia.pom) {
    const rif = leggi(rifAbs, coppia.pom);
    const mod = relModello(coppia.pom) ? leggi(modelloAbs, relModello(coppia.pom)) : null;
    if (rif && mod) {
      const dRif = dipendenzePom(sostituisciSegnaposto(rif, cfg.segnaposto));
      const dMod = dipendenzePom(sostituisciSegnaposto(mod, cfg.segnaposto));
      for (const d of differenza(dRif, dMod)) nota('dep-mancante', `dep:${d}`, `dichiarata in ${percorsoRif(coppia.pom)}`);
      for (const d of differenza(dMod, dRif)) nota('dep-extra', `dep:${d}`, `dichiarata solo nel modello`);
    }
  }

  // 3. chiavi di application.properties
  if (controlli.includes('properties') && coppia.properties) {
    const rif = leggi(rifAbs, coppia.properties);
    const mod = relModello(coppia.properties) ? leggi(modelloAbs, relModello(coppia.properties)) : null;
    if (rif && mod) {
      const kRif = chiaviProperties(sostituisciSegnaposto(rif, cfg.segnaposto));
      const kMod = chiaviProperties(sostituisciSegnaposto(mod, cfg.segnaposto));
      for (const k of differenza(kRif, kMod)) nota('prop-mancante', `prop:${k}`, `presente in ${percorsoRif(coppia.properties)}`);
      for (const k of differenza(kMod, kRif)) nota('prop-extra', `prop:${k}`, 'presente solo nel modello');
    }
  }

  // 4. annotazioni portanti, sulle sole classi che esistono da entrambe le parti
  if (controlli.includes('annotazioni')) {
    for (const rel of fileRif) {
      if (!rel.startsWith('src/main/java/') || !rel.endsWith('.java')) continue;
      const relMod = relModello(rel);
      if (!relMod) continue; // già segnalato come file mancante
      const aRif = annotazioniFile(leggi(rifAbs, rel) ?? '', cfg.annotazioniPortanti);
      const aMod = annotazioniFile(leggi(modelloAbs, relMod) ?? '', cfg.annotazioniPortanti);
      for (const a of differenza(aRif, aMod)) nota('annotazione', `ann:${percorsoRif(rel)}#${a}`, 'sull’app #1, non sul modello');
      for (const a of differenza(aMod, aRif)) nota('annotazione', `ann:${percorsoRif(rel)}#${a}`, 'sul modello, non sull’app #1');
    }
  }

  return div;
}

export function caricaConfig() {
  return JSON.parse(readFileSync(join(QUI, 'parity.config.json'), 'utf8'));
}

export function eseguiCollaudo(cfg = caricaConfig()) {
  const templatesAbs = join(RADICE, cfg.templatesRoot);
  if (!existsSync(templatesAbs)) return { modelliAssenti: true, divergenze: [], derogate: [] };

  const registro = join(RADICE, cfg.registro);
  const deroghe = existsSync(registro) ? leggiDeroghe(readFileSync(registro, 'utf8')) : new Set();

  const tutte = cfg.coppie.flatMap((c) => confrontaCoppia(c, cfg, templatesAbs));
  return {
    modelliAssenti: false,
    divergenze: tutte.filter((d) => !deroghe.has(d.chiave)),
    derogate: tutte.filter((d) => deroghe.has(d.chiave)),
  };
}

export function renderReport(esito, cfg) {
  if (esito.modelliAssenti) {
    return [
      `✗ modelli-sorgente non ancora presenti: manca ${cfg.templatesRoot}/`,
      '',
      'Il collaudo di parità non può dire nulla finché i modelli non esistono, e restare verde',
      'sarebbe peggio che essere rosso: segnalerebbe una parità mai verificata.',
      '',
      `→ crea i modelli in ${cfg.templatesRoot}/ (generatore della skill \`new-application\`, UC 0046),`,
      `  poi rilancia \`npm run parity --prefix tools/scaffold-parity\`.`,
    ].join('\n');
  }

  const coda = esito.derogate.length
    ? `\n(${esito.derogate.length} divergenza/e derogata/e consapevolmente in ${cfg.registro})`
    : '';

  if (esito.divergenze.length === 0) {
    return `✓ modelli-sorgente in parità con l’app #1 (collaudo UC 0046, strato 1)${coda}`;
  }

  const righe = [`✗ ${esito.divergenze.length} divergenza/e fra i modelli-sorgente e l’app #1 \`fatture\`:`];
  for (const tipo of Object.keys(ETICHETTE)) {
    const gruppo = esito.divergenze.filter((d) => d.tipo === tipo);
    if (gruppo.length === 0) continue;
    righe.push(`\n${ETICHETTE[tipo]}:`);
    for (const d of gruppo) righe.push(`  - ${d.chiave}${d.dettaglio ? `  — ${d.dettaglio}` : ''}`);
  }
  righe.push(
    '',
    'Che fare — una delle due, mai nessuna delle due:',
    `  1. AGGIORNA IL MODELLO in ${cfg.templatesRoot}/ così che torni in parità. È la via`,
    '     normale: l’app #1 è evoluta e le app nuove devono nascere con la stessa evoluzione.',
    `  2. REGISTRA LA DEVIAZIONE in ${cfg.registro} (strato 3) se la novità è`,
    '     specifica del dominio di `fatture` e non va generalizzata. Aggiungi una riga alla tabella',
    '     fra i marcatori `deviazioni:inizio`/`deviazioni:fine`, con la chiave qui sopra fra apici',
    '     inversi e la motivazione. La deroga è un atto consapevole e firmato, non un silenziamento.',
    '',
    coda.trim(),
  );
  return righe.filter((r) => r !== undefined).join('\n');
}

function main() {
  const cfg = caricaConfig();
  const esito = eseguiCollaudo(cfg);
  if (process.argv.includes('--json')) console.log(JSON.stringify(esito, null, 2));
  else console.log(renderReport(esito, cfg));
  process.exit(esito.modelliAssenti ? 2 : esito.divergenze.length > 0 ? 1 : 0);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main();
