// Logica pura del check dei documenti legali pubblici (UC 0002, #14 C13 / #13 G38):
// parità lingue dei componenti in content/legal/, validità del frontmatter e integrità
// referenziale dei token {{titolare.*}} verso entity.yaml. Nessun accesso al filesystem:
// la CLI (I/O + parsing) è legal-check.mjs.

const SEMVER_RE = /^\d+\.\d+\.\d+$/;
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
// Token {{ chiave.puntata }} — chiavi minuscole, cifre, underscore e punti.
const TOKEN_RE = /\{\{\s*([a-z0-9_.]+)\s*\}\}/g;
// Valore ancora da compilare (case-insensitive, spazi tollerati).
const PLACEHOLDER_RE = /^\s*da compilare\s*$/i;

/** Estrae component e lang dal nome file `<component>.<lang>.md` (null se non combacia). */
export function parseFileName(name) {
  const m = /^([a-z0-9-]+)\.([a-z]{2})\.md$/.exec(name);
  return m ? { component: m[1], lang: m[2] } : null;
}

/** Estrae i token {{...}} distinti da un testo. */
export function extractTokens(text) {
  const out = new Set();
  for (const m of String(text).matchAll(TOKEN_RE)) out.add(m[1]);
  return [...out];
}

/** Appiattisce un oggetto annidato in chiavi puntate: {a:{b:1}} → {"a.b":1}. */
export function flattenKeys(obj, prefix = '', acc = {}) {
  if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
    for (const [k, v] of Object.entries(obj)) flattenKeys(v, prefix ? `${prefix}.${k}` : k, acc);
  } else {
    acc[prefix] = obj;
  }
  return acc;
}

/**
 * Valida i documenti legali. Ritorna { errors, warnings } (errors vuoto = ok).
 * @param {Array<{file:string, component:string, lang:string, frontmatter:object|null, body:string}>} docs
 * @param {{required_languages:string[], components?:string[]}} config
 * @param {object} entity  contenuto di entity.yaml (annidato)
 */
export function validateLegal(docs, config, entity) {
  const errors = [];
  const warnings = [];

  const langs = config?.required_languages;
  if (!Array.isArray(langs) || langs.length === 0) {
    return { errors: ['_config.yaml: `required_languages` mancante o vuoto'], warnings };
  }
  const components = config?.components;
  if (!Array.isArray(components) || components.length === 0) {
    return { errors: ['_config.yaml: `components` mancante o vuoto'], warnings };
  }

  // Indice (component, lang) → doc; segnala file fuori-convenzione e duplicati.
  const byKey = new Map();
  for (const doc of docs) {
    const key = `${doc.component}.${doc.lang}`;
    if (byKey.has(key)) errors.push(`${doc.file}: doppione per componente "${doc.component}" lingua "${doc.lang}"`);
    byKey.set(key, doc);
    if (!components.includes(doc.component)) {
      errors.push(`${doc.file}: componente "${doc.component}" non dichiarato in _config.yaml (components)`);
    }
    if (!langs.includes(doc.lang)) {
      errors.push(`${doc.file}: lingua "${doc.lang}" non dichiarata in _config.yaml (required_languages)`);
    }
  }

  // Parità lingue: ogni componente in tutte le lingue.
  for (const component of components) {
    for (const lang of langs) {
      if (!byKey.has(`${component}.${lang}`)) {
        errors.push(`content/legal: manca "${component}.${lang}.md" (parità 5 lingue non soddisfatta)`);
      }
    }
  }

  // Frontmatter per ogni doc.
  for (const doc of docs) {
    const ref = doc.file;
    const fm = doc.frontmatter;
    if (!fm || typeof fm !== 'object') {
      errors.push(`${ref}: frontmatter assente o non valido (blocco --- ... --- in testa)`);
      continue;
    }
    if (typeof fm.version !== 'string' || !SEMVER_RE.test(fm.version)) {
      errors.push(`${ref}: \`version\` mancante o non semver (atteso es. 1.0.0)`);
    }
    const eff = fm.effective_date instanceof Date ? isoOf(fm.effective_date) : fm.effective_date;
    if (typeof eff !== 'string' || !ISO_DATE_RE.test(eff) || Number.isNaN(Date.parse(eff))) {
      errors.push(`${ref}: \`effective_date\` mancante o non data ISO (atteso YYYY-MM-DD)`);
    }
    if (fm.lang !== doc.lang) {
      errors.push(`${ref}: \`lang\` del frontmatter ("${fm.lang}") incoerente col nome file ("${doc.lang}")`);
    }
  }

  // Integrità referenziale dei token verso entity.yaml.
  const flat = flattenKeys(entity ?? {});
  const seenPlaceholders = new Set();
  for (const doc of docs) {
    for (const token of extractTokens(doc.body)) {
      if (!(token in flat)) {
        errors.push(`${doc.file}: token {{${token}}} senza chiave corrispondente in entity.yaml`);
      } else if (PLACEHOLDER_RE.test(String(flat[token]))) {
        seenPlaceholders.add(token);
      }
    }
  }
  for (const token of [...seenPlaceholders].sort()) {
    warnings.push(`entity.yaml: "${token}" è ancora "DA COMPILARE" (atteso pre-go-live, UC 0001)`);
  }

  return { errors, warnings };
}

function isoOf(date) {
  return date.toISOString().slice(0, 10);
}
