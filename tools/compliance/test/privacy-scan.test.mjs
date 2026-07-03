// Test dello scanner segnali privacy (UC 0031) su diff sintetici — un caso per tipo di segnale
// + il caso pulito (nessun falso positivo su test java, lockfile, _config, localhost).

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseDiff, scanDiff, renderReport } from '../privacy-scan.mjs';

const diff = (file, { added = [], removed = [], isNew = false } = {}) =>
  [
    `diff --git a/${file} b/${file}`,
    ...(isNew ? ['new file mode 100644'] : []),
    `--- ${isNew ? '/dev/null' : `a/${file}`}`,
    `+++ b/${file}`,
    '@@ -0,0 +1 @@',
    ...removed.map((l) => `-${l}`),
    ...added.map((l) => `+${l}`),
  ].join('\n') + '\n';

test('parseDiff: file, righe aggiunte/rimosse, new file', () => {
  const files = parseDiff(
    diff('a.txt', { added: ['x'], removed: ['y'] }) + diff('b.txt', { added: ['z'], isNew: true }),
  );
  assert.equal(files.length, 2);
  assert.deepEqual(files[0], { file: 'a.txt', isNew: false, added: ['x'], removed: ['y'] });
  assert.equal(files[1].isNew, true);
});

test('migrazione Flyway: CREATE TABLE e ADD COLUMN rilevati', () => {
  const signals = scanDiff(
    diff('services/core/src/main/resources/db/migration/V12__add_phone.sql', {
      added: ['CREATE TABLE contact (id uuid);', 'ALTER TABLE users ADD COLUMN phone text;', 'CREATE INDEX i ON x;'],
    }),
  );
  assert.deepEqual(signals.map((s) => s.type), ['migration', 'migration']);
});

test('nuovo campo entità/DTO in src/main/java (i metodi non scattano)', () => {
  const signals = scanDiff(
    diff('services/core/src/main/java/app/appgrove/core/User.java', {
      added: ['    private String phoneNumber;', '    public String getPhoneNumber() { return phoneNumber; }'],
    }),
  );
  assert.deepEqual(signals.map((s) => s.type), ['entity-field']);
  assert.match(signals[0].detail, /phoneNumber/);
});

test('campi in src/test/java NON scattano', () => {
  const signals = scanDiff(
    diff('services/core/src/test/java/app/appgrove/core/UserTest.java', { added: ['  private String fixture;'] }),
  );
  assert.deepEqual(signals, []);
});

test('nuova dipendenza: pom.xml e package.json (non il lockfile)', () => {
  const signals = scanDiff(
    diff('services/core/pom.xml', { added: ['      <artifactId>aws-ses-sdk</artifactId>'] }) +
      diff('frontend/package.json', { added: ['    "posthog-js": "^1.2.3",', '    "start": "vite dev",'] }) +
      diff('frontend/package-lock.json', { added: ['    "left-pad": "1.0.0",'] }),
  );
  assert.deepEqual(signals.map((s) => s.type), ['dependency', 'dependency']);
  assert.deepEqual(signals.map((s) => s.detail), ['aws-ses-sdk', 'posthog-js@^1.2.3']);
});

test('nuovo host esterno in config (localhost ignorato, docs/ esclusa)', () => {
  const signals = scanDiff(
    diff('services/core/src/main/resources/application.properties', {
      added: ['mail.api=https://api.mailprovider.io/v1', 'quarkus.datasource.url=jdbc:postgresql://localhost:5432/x'],
    }) + diff('docs/compliance/ropa.it.md', { added: ['vedi https://esempio.example'] }),
  );
  assert.deepEqual(signals.map((s) => s.type), ['external-host']);
  assert.equal(signals[0].detail, 'api.mailprovider.io');
});

test('manifesto dati: retention/purpose/legal_basis toccati, _config escluso', () => {
  const signals = scanDiff(
    diff('docs/compliance/manifests/fatture.yaml', {
      removed: ['    retention: { it: 3 anni, en: 3 years }'],
      added: ['    retention: { it: 5 anni, en: 5 years }', '    note: irrilevante'],
    }) + diff('docs/compliance/manifests/_config.yaml', { added: ['required_languages: [it, en, fr]'] }),
  );
  assert.deepEqual(signals.map((s) => s.type), ['manifest-classification']);
  assert.match(signals[0].detail, /5 anni/);
});

test('manifesto nuovo → segnale; chiave rimossa senza sostituzione → segnalata', () => {
  const signals = scanDiff(
    diff('docs/compliance/manifests/nuovaapp.yaml', { added: ['app: nuovaapp'], isNew: true }) +
      diff('docs/compliance/manifests/platform.yaml', { removed: ['    legal_basis: { it: x, en: y }'] }),
  );
  assert.deepEqual(signals.map((s) => s.detail), ['nuovo manifesto', 'rimossa: legal_basis: { it: x, en: y }']);
});

test('diff pulito → nessun segnale, exit-style report verde', () => {
  const signals = scanDiff(diff('README.md', { added: ['testo'] }));
  assert.deepEqual(signals, []);
  assert.match(renderReport(signals), /nessun segnale/);
});

test('report con segnali: raggruppato per tipo e con puntatore al gate', () => {
  const report = renderReport(
    scanDiff(
      diff('services/core/src/main/resources/db/migration/V13__t.sql', { added: ['CREATE TABLE t (id uuid);'] }),
    ),
  );
  assert.match(report, /Migrazioni Flyway/);
  assert.match(report, /UC 0031/);
  assert.match(report, /manifesto \+ RoPA/);
});
