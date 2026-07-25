// Test del check dei documenti legali pubblici (UC 0002 §9): parità lingue, frontmatter,
// integrità dei token verso entity.yaml.
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { extractTokens, flattenKeys, parseFileName, validateLegal } from '../legal.mjs';
import { splitFrontmatter } from '../legal-check.mjs';

const CONFIG = { required_languages: ['en', 'it'], components: ['privacy', 'terms'] };
const ENTITY = { titolare: { email_privacy: 'privacy@appgrove.app', ragione_sociale: 'DA COMPILARE' } };

function doc(component, lang, overrides = {}) {
  return {
    file: `content/legal/${component}.${lang}.md`,
    component,
    lang,
    frontmatter: { version: '1.0.0', effective_date: '2026-07-25', lang, ...overrides.frontmatter },
    body: overrides.body ?? 'Testo.',
  };
}

/** Insieme completo e valido per CONFIG (privacy+terms in en+it). */
function fullSet() {
  return [doc('privacy', 'en'), doc('privacy', 'it'), doc('terms', 'en'), doc('terms', 'it')];
}

test('insieme completo e valido → nessun errore', () => {
  const { errors } = validateLegal(fullSet(), CONFIG, ENTITY);
  assert.deepEqual(errors, []);
});

test('lingua mancante per un componente → errore di parità', () => {
  const docs = fullSet().filter((d) => !(d.component === 'terms' && d.lang === 'it'));
  const { errors } = validateLegal(docs, CONFIG, ENTITY);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /terms\.it\.md/);
});

test('frontmatter version non semver → errore', () => {
  const docs = fullSet();
  docs[0].frontmatter.version = 'v1';
  const { errors } = validateLegal(docs, CONFIG, ENTITY);
  assert.ok(errors.some((e) => /version/.test(e)));
});

test('effective_date non ISO → errore', () => {
  const docs = fullSet();
  docs[0].frontmatter.effective_date = '25-07-2026';
  const { errors } = validateLegal(docs, CONFIG, ENTITY);
  assert.ok(errors.some((e) => /effective_date/.test(e)));
});

test('lang del frontmatter incoerente col nome file → errore', () => {
  const docs = fullSet();
  docs[0].frontmatter.lang = 'it'; // file è privacy.en.md
  const { errors } = validateLegal(docs, CONFIG, ENTITY);
  assert.ok(errors.some((e) => /incoerente/.test(e)));
});

test('token orfano (chiave assente in entity.yaml) → errore', () => {
  const docs = fullSet();
  docs[0].body = 'Titolare: {{titolare.inesistente}}.';
  const { errors } = validateLegal(docs, CONFIG, ENTITY);
  assert.ok(errors.some((e) => /token \{\{titolare\.inesistente\}\}/.test(e)));
});

test('token valido con valore DA COMPILARE → avviso non bloccante', () => {
  const docs = fullSet();
  docs[0].body = '{{titolare.ragione_sociale}}';
  const { errors, warnings } = validateLegal(docs, CONFIG, ENTITY);
  assert.deepEqual(errors, []);
  assert.ok(warnings.some((w) => /ragione_sociale/.test(w)));
});

test('token valido con valore reale → nessun errore né avviso', () => {
  const docs = fullSet();
  docs[0].body = 'Scrivere a {{titolare.email_privacy}}.';
  const { errors, warnings } = validateLegal(docs, CONFIG, ENTITY);
  assert.deepEqual(errors, []);
  assert.deepEqual(warnings, []);
});

test('parseFileName estrae componente e lingua', () => {
  assert.deepEqual(parseFileName('privacy.it.md'), { component: 'privacy', lang: 'it' });
  assert.equal(parseFileName('entity.yaml'), null);
  assert.equal(parseFileName('README.md'), null);
});

test('extractTokens raccoglie token distinti e tollera spazi', () => {
  assert.deepEqual(extractTokens('{{ titolare.sede }} e {{titolare.sede}}'), ['titolare.sede']);
});

test('flattenKeys appiattisce oggetti annidati', () => {
  assert.deepEqual(flattenKeys({ a: { b: 1 }, c: 2 }), { 'a.b': 1, c: 2 });
});

test('splitFrontmatter separa frontmatter e corpo', () => {
  const { frontmatter, body } = splitFrontmatter('---\nversion: 1.0.0\nlang: it\n---\nCorpo.');
  assert.equal(frontmatter.version, '1.0.0');
  assert.equal(body.trim(), 'Corpo.');
});

test('splitFrontmatter su testo senza frontmatter → null', () => {
  const { frontmatter } = splitFrontmatter('Nessun frontmatter.');
  assert.equal(frontmatter, null);
});
