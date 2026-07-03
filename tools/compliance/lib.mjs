// Logica pura dell'automazione RoPA (UC 0030, #13 C14): validazione dei manifesti dati
// (parità lingue) e rendering del RoPA per lingua. Nessun accesso al filesystem: la CLI è ropa.mjs.

/**
 * Etichette del documento RoPA per lingua. Per aggiungere una lingua a `required_languages`
 * (docs/compliance/manifests/_config.yaml) va aggiunta la voce corrispondente qui.
 */
export const LABELS = {
  it: {
    title: 'Registro dei trattamenti (RoPA) — appgrove',
    intro:
      'Documento **INTERNO** (art. 30.4 GDPR): si fornisce solo al Garante su richiesta, non è la ' +
      'privacy policy pubblica (#13 C17). **File GENERATO** dai manifesti dati ' +
      '(`docs/compliance/manifests/*.yaml`) con `tools/compliance` — **non modificare a mano**: ' +
      'aggiorna il manifesto e rigenera (`npm run assemble`). Bozza sotto disclaimer: validazione ' +
      'finale del legale (docs/_REVISIONE-LEGALE.md).',
    processing: 'Trattamenti',
    columns: ['Voce', 'Categoria di dati', 'Ubicazione', 'Interessati', 'Finalità', 'Base giuridica', 'Retention'],
  },
  en: {
    title: 'Record of processing activities (RoPA) — appgrove',
    intro:
      '**INTERNAL** document (art. 30(4) GDPR): provided to the supervisory authority on request only; ' +
      'this is not the public privacy policy (#13 C17). **GENERATED file** from the data manifests ' +
      '(`docs/compliance/manifests/*.yaml`) via `tools/compliance` — **do not edit by hand**: ' +
      'update the manifest and regenerate (`npm run assemble`). Draft under disclaimer: final ' +
      'validation by legal counsel (docs/_REVISIONE-LEGALE.md).',
    processing: 'Processing activities',
    columns: ['Entry', 'Data category', 'Location', 'Data subjects', 'Purpose', 'Legal basis', 'Retention'],
  },
};

/** Campi lang-keyed di una voce del manifesto (obbligatori salvo dove indicato). */
const ENTRY_TEXT_FIELDS = ['location', 'data_subjects', 'data_category', 'purpose', 'legal_basis', 'retention'];

/**
 * Valida config + manifesti: ritorna la lista di errori (vuota = ok).
 * Parità lingue (#13 C14): ogni testo lang-keyed deve avere TUTTE le `required_languages`.
 * @param {Array<{file: string, data: object}>} manifests
 * @param {{required_languages: string[]}} config
 */
export function validateManifests(manifests, config) {
  const errors = [];
  const langs = config?.required_languages;
  if (!Array.isArray(langs) || langs.length === 0) {
    return ['_config.yaml: `required_languages` mancante o vuoto'];
  }
  for (const lang of langs) {
    if (!LABELS[lang]) {
      errors.push(`_config.yaml: lingua richiesta "${lang}" senza etichette in tools/compliance/lib.mjs (LABELS)`);
    }
  }

  const ids = new Set();
  for (const { file, data } of manifests) {
    const where = (detail) => `${file}: ${detail}`;
    if (!data || typeof data !== 'object') {
      errors.push(where('manifesto vuoto o non valido'));
      continue;
    }
    if (!data.id) errors.push(where('campo `id` mancante'));
    if (data.id && ids.has(data.id)) errors.push(where(`id duplicato "${data.id}"`));
    ids.add(data.id);

    checkLangMap(errors, langs, data.name, where('`name`'));
    if (data.description !== undefined) checkLangMap(errors, langs, data.description, where('`description`'));

    const keys = new Set();
    for (const [i, entry] of (data.entries ?? []).entries()) {
      const ref = where(`entries[${i}]${entry?.key ? ` (${entry.key})` : ''}`);
      if (!entry?.key) errors.push(`${ref}: campo \`key\` mancante`);
      if (entry?.key && keys.has(entry.key)) errors.push(`${ref}: key duplicata`);
      keys.add(entry?.key);
      if ((entry?.entity && !entry?.field) || (!entry?.entity && entry?.field)) {
        errors.push(`${ref}: \`entity\` e \`field\` vanno dichiarati insieme (voce entity-backed)`);
      }
      for (const field of ENTRY_TEXT_FIELDS) {
        checkLangMap(errors, langs, entry?.[field], `${ref}: \`${field}\``);
      }
    }

    for (const [i, section] of (data.sections ?? []).entries()) {
      const ref = where(`sections[${i}]${section?.key ? ` (${section.key})` : ''}`);
      checkLangMap(errors, langs, section?.title, `${ref}: \`title\``);
      checkLangMap(errors, langs, section?.body, `${ref}: \`body\``);
    }
  }
  return errors;
}

function checkLangMap(errors, langs, value, ref) {
  if (value === undefined || value === null) {
    errors.push(`${ref}: testo lang-keyed mancante`);
    return;
  }
  if (typeof value !== 'object') {
    errors.push(`${ref}: deve essere una mappa lingua → testo (es. "it:", "en:"), non un testo semplice`);
    return;
  }
  for (const lang of langs) {
    const text = value[lang];
    if (typeof text !== 'string' || text.trim() === '') {
      errors.push(`${ref}: manca la traduzione "${lang}"`);
    }
  }
}

/**
 * Renderizza il RoPA di una lingua dai manifesti (piattaforma per prima, poi le app per id).
 * Output deterministico (niente timestamp): il freshness check confronta byte-a-byte.
 */
export function renderRopa(manifests, lang) {
  const labels = LABELS[lang];
  if (!labels) throw new Error(`lingua "${lang}" senza etichette (LABELS)`);
  const ordered = [...manifests]
    .map(({ data }) => data)
    .sort((a, b) => (a.id === 'platform' ? -1 : b.id === 'platform' ? 1 : a.id.localeCompare(b.id)));

  const out = [`# ${labels.title}`, '', `> ${labels.intro}`, ''];
  for (const manifest of ordered) {
    out.push(`## ${manifest.name[lang]}`, '');
    if (manifest.description) out.push(manifest.description[lang].trim(), '');
    const entries = manifest.entries ?? [];
    if (entries.length > 0) {
      out.push(`### ${labels.processing}`, '');
      out.push(`| ${labels.columns.join(' | ')} |`);
      out.push(`|${labels.columns.map(() => '---').join('|')}|`);
      for (const entry of entries) {
        const row = [
          `\`${entry.key}\``,
          entry.data_category[lang],
          entry.location[lang],
          entry.data_subjects[lang],
          entry.purpose[lang],
          entry.legal_basis[lang],
          entry.retention[lang],
        ].map(cell);
        out.push(`| ${row.join(' | ')} |`);
      }
      out.push('');
    }
    for (const section of manifest.sections ?? []) {
      out.push(`### ${section.title[lang]}`, '', section.body[lang].trim(), '');
    }
  }
  return out.join('\n');
}

function cell(text) {
  return String(text).replace(/\s+/g, ' ').replaceAll('|', '\\|').trim();
}

/** Nome file RoPA per lingua (docs/compliance/). */
export function ropaFileName(lang) {
  return `ropa.${lang}.md`;
}
