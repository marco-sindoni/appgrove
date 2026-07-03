// Test dell'automazione RoPA (UC 0030 §9): parità lingue e rendering deterministico.
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { renderRopa, validateManifests } from '../lib.mjs';

const CONFIG = { required_languages: ['it', 'en'] };

const text = (it, en) => ({ it, en });

function manifest(overrides = {}) {
  return {
    file: 'manifests/demo.yaml',
    data: {
      id: 'demo',
      name: text('App demo', 'Demo app'),
      description: text('Descrizione', 'Description'),
      entries: [
        {
          key: 'demo.email',
          entity: 'app.appgrove.demo.Demo',
          field: 'email',
          location: text('Tabella demo', 'Demo table'),
          data_subjects: text('Utenti', 'Users'),
          data_category: text('Contatto', 'Contact'),
          purpose: text('Erogazione', 'Provisioning'),
          legal_basis: text('Contratto', 'Contract'),
          retention: text('Account attivo', 'Active account'),
        },
      ],
      sections: [{ key: 's', title: text('Sezione', 'Section'), body: text('Corpo', 'Body') }],
      ...overrides,
    },
  };
}

test('manifesto completo nelle lingue richieste → nessun errore', () => {
  assert.deepEqual(validateManifests([manifest()], CONFIG), []);
});

test('traduzione mancante → errore di parità lingue (#13 C14)', () => {
  const broken = manifest();
  delete broken.data.entries[0].purpose.en;
  const errors = validateManifests([broken], CONFIG);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /purpose/);
  assert.match(errors[0], /manca la traduzione "en"/);
});

test('testo semplice al posto della mappa lingue → errore', () => {
  const broken = manifest({ name: 'App demo' });
  const errors = validateManifests([broken], CONFIG);
  assert.match(errors[0], /mappa lingua/);
});

test('entity senza field (o viceversa) → errore', () => {
  const broken = manifest();
  delete broken.data.entries[0].field;
  const errors = validateManifests([broken], CONFIG);
  assert.match(errors[0], /entity.*field/);
});

test('lingua richiesta senza etichette LABELS → errore esplicito', () => {
  const errors = validateManifests([manifest()], { required_languages: ['it', 'fr'] });
  assert.match(errors.join('\n'), /"fr" senza etichette/);
});

test('rendering: platform prima delle app, stesso set di voci nelle due lingue', () => {
  const platform = manifest({ id: 'platform', name: text('Piattaforma', 'Platform'), sections: [] });
  platform.file = 'manifests/platform.yaml';
  const it = renderRopa([manifest(), platform], 'it');
  const en = renderRopa([manifest(), platform], 'en');
  assert.ok(it.indexOf('## Piattaforma') < it.indexOf('## App demo'), 'platform per prima');
  const keys = (md) => [...md.matchAll(/\| `([^`]+)`/g)].map((m) => m[1]);
  assert.deepEqual(keys(it), keys(en), 'stesso set di voci IT/EN');
  assert.match(it, /Registro dei trattamenti/);
  assert.match(en, /Record of processing activities/);
});

test('rendering deterministico e celle senza a-capo', () => {
  const multiline = manifest();
  multiline.data.entries[0].purpose = text('riga1\nriga2 | pipe', 'line1\nline2');
  const md = renderRopa([multiline], 'it');
  assert.equal(md, renderRopa([multiline], 'it'), 'output stabile');
  assert.match(md, /riga1 riga2 \\\| pipe/);
});
