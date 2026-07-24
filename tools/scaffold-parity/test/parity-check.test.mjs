// Test del collaudo di parità (UC 0046, strato 1): verificano le funzioni pure di confronto e la
// lettura delle deroghe dal registro. Il collaudo sui modelli veri si lancia con `npm run parity`.

import assert from 'node:assert/strict';
import { existsSync, mkdtempSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { test } from 'node:test';

import {
  annotazioniFile,
  caricaConfig,
  chiaviProperties,
  confrontaCoppia,
  corrisponde,
  dipendenzePom,
  elencaFile,
  leggiDeroghe,
  normalizzaPercorsoModello,
  RADICE,
  renderReport,
  sostituisciSegnaposto,
  trovaRadiceModello,
  varianteDi,
} from '../parity-check.mjs';

const cfg = caricaConfig();

test('i segnaposto del generatore si risolvono nei valori dell’app #1', () => {
  assert.equal(sostituisciSegnaposto('services/@@APP_ID@@/pom.xml', cfg.segnaposto), 'services/fatture/pom.xml');
  assert.equal(sostituisciSegnaposto('@@APP_CLASS@@Main.java', cfg.segnaposto), 'FattureMain.java');
  assert.equal(sostituisciSegnaposto('V1__create_@@SCHEMA@@_schema.sql', cfg.segnaposto), 'V1__create_app_fatture_schema.sql');
});

test('il dominio di esempio dei modelli si riconduce al dominio reale dell’app #1 — solo nei nomi', () => {
  assert.equal(normalizzaPercorsoModello('src/main/java/ItemRepository.java', cfg), 'src/main/java/InvoiceRepository.java');
  // il contenuto non viene toccato dalla mappa di dominio
  assert.equal(sostituisciSegnaposto('class Item {}', cfg.segnaposto), 'class Item {}');
});

test('il suffisso di modello viene tolto dal nome normalizzato', () => {
  assert.equal(normalizzaPercorsoModello('pom.xml.tmpl', cfg), 'pom.xml');
  assert.equal(normalizzaPercorsoModello('src/main/java/@@APP_CLASS@@Main.java.template', cfg), 'src/main/java/FattureMain.java');
});

test('il marcatore di variante si riconosce solo se dichiarato in configurazione', () => {
  assert.equal(varianteDi('QuotaTest.flow.java', cfg.varianti), 'flow');
  assert.equal(varianteDi('QuotaTest.stock.java', cfg.varianti), 'stock');
  // un punto qualunque nel nome non è una variante
  assert.equal(varianteDi('Dockerfile.jvm', cfg.varianti), null);
  assert.equal(varianteDi('src/main/resources/application.properties', cfg.varianti), null);
  assert.equal(varianteDi('QuotaTest.qualcosaltro.java', cfg.varianti), null);
});

test('le varianti confluiscono sullo stesso nome dell’app #1', () => {
  const atteso = 'src/main/java/FattureQuotaService.java';
  assert.equal(normalizzaPercorsoModello('src/main/java/@@APP_CLASS@@QuotaService.flow.java', cfg), atteso);
  assert.equal(normalizzaPercorsoModello('src/main/java/@@APP_CLASS@@QuotaService.stock.java', cfg), atteso);
});

test('solo la variante di riferimento viene confrontata con l’app #1', () => {
  const base = mkdtempSync(join(tmpdir(), 'parita-varianti-'));
  const modello = join(base, 'modelli', 'servizio');
  const rif = join(base, 'app1');
  const scrivi = (dir, rel, testo) => {
    mkdirSync(dirname(join(dir, rel)), { recursive: true });
    writeFileSync(join(dir, rel), testo);
  };
  scrivi(modello, 'pom.xml', '<project/>');
  scrivi(modello, 'src/Quota.flow.java', '@ApplicationScoped class Quota {}');
  // la variante a giacenza non ha corrispondente nell'app #1 (che è a consumo): non deve risultare "extra"
  scrivi(modello, 'src/Quota.stock.java', '@ApplicationScoped class Quota {}');
  scrivi(rif, 'pom.xml', '<project/>');
  scrivi(rif, 'src/Quota.java', '@ApplicationScoped class Quota {}');

  const cfgLocale = {
    ...cfg,
    templatesRoot: join(base, 'modelli'),
    dominio: {},
    segnaposto: {},
  };
  const coppia = { id: 'servizio', riferimento: rif, marcatore: 'pom.xml', controlli: ['file'] };
  const div = confrontaCoppia(coppia, cfgLocale, join(base, 'modelli'));
  assert.deepEqual(div, [], `divergenze inattese: ${JSON.stringify(div)}`);
});

test('il glob minimale distingue * da **', () => {
  assert.ok(corrisponde('target/classes/x.class', 'target/**'));
  assert.ok(corrisponde('a/b/.DS_Store', '**/.DS_Store'));
  assert.ok(!corrisponde('src/target/x', 'target/**'));
});

test('le dipendenze del pom si leggono come groupId:artifactId', () => {
  const deps = dipendenzePom(`
    <dependencies>
      <dependency><groupId>io.quarkus</groupId><artifactId>quarkus-rest</artifactId></dependency>
      <dependency><groupId>app.appgrove</groupId><artifactId>commons</artifactId><scope>test</scope></dependency>
    </dependencies>`);
  assert.deepEqual([...deps].sort(), ['app.appgrove:commons', 'io.quarkus:quarkus-rest']);
});

test('le chiavi di application.properties includono i profili e saltano i commenti', () => {
  const chiavi = chiaviProperties('# commento\nquarkus.http.port=8081\n\n%dev.quarkus.log.console.json=false\n');
  assert.deepEqual([...chiavi].sort(), ['%dev.quarkus.log.console.json', 'quarkus.http.port']);
});

test('si estraggono le sole annotazioni portanti dichiarate in configurazione', () => {
  const testo = '@ApplicationScoped\n@RequiresEntitlement("fatture")\npublic class X { @Inject Y y; }';
  const a = annotazioniFile(testo, cfg.annotazioniPortanti);
  assert.deepEqual([...a].sort(), ['ApplicationScoped', 'RequiresEntitlement']);
  assert.ok(!a.has('Inject'), 'Inject non è portante: non deve entrare nel confronto');
});

test('le deroghe si leggono solo fra i marcatori del registro', () => {
  const md = [
    'Prosa iniziale con `file:services/fatture/fuori.java` che NON deve contare.',
    '<!-- deviazioni:inizio -->',
    '| `dep:io.quarkus:quarkus-scheduler` | perché | 2026-07-19 |',
    '| `file:services/fatture/src/main/resources/META-INF/openapi/openapi.json` | perché | 2026-07-19 |',
    '<!-- deviazioni:fine -->',
    'Storico con `prop:vecchia.chiave` che NON deve contare.',
  ].join('\n');
  const deroghe = leggiDeroghe(md);
  assert.deepEqual(
    [...deroghe].sort(),
    ['dep:io.quarkus:quarkus-scheduler', 'file:services/fatture/src/main/resources/META-INF/openapi/openapi.json'],
  );
});

test('il registro reale del repo esiste ed è interpretabile', () => {
  const registro = join(RADICE, cfg.registro);
  assert.ok(existsSync(registro), `manca il registro dello strato 3: ${cfg.registro}`);
  const md = readFileSync(registro, 'utf8');
  assert.ok(md.includes('<!-- deviazioni:inizio -->'), 'il registro deve contenere i marcatori della tabella deroghe');
  assert.ok(md.includes('<!-- deviazioni:fine -->'));
  leggiDeroghe(md); // non deve lanciare
});

// Confronto end-to-end su una coppia finta, per dimostrare che ogni tipo di divergenza è rilevato.
test('confrontaCoppia rileva file, dipendenze, chiavi e annotazioni divergenti', () => {
  const base = mkdtempSync(join(tmpdir(), 'parita-'));
  const scrivi = (p, testo) => {
    mkdirSync(dirname(p), { recursive: true });
    writeFileSync(p, testo);
  };

  // "app di riferimento" finta
  const rif = join(base, 'app');
  scrivi(join(rif, 'pom.xml'), '<dependency><groupId>g</groupId><artifactId>a</artifactId></dependency>');
  scrivi(join(rif, 'src/main/resources/application.properties'), 'k1=1\nk2=2\n');
  scrivi(join(rif, 'src/main/java/Res.java'), '@ApplicationScoped\n@RequiresEntitlement\nclass Res {}');
  scrivi(join(rif, 'src/main/java/Solo.java'), 'class Solo {}');

  // "modello" finto: manca un file, manca una chiave, manca un'annotazione, ha una dipendenza in più
  const modello = join(base, 'templates', 'servizio');
  scrivi(join(modello, 'pom.xml.tmpl'), '<dependency><groupId>g</groupId><artifactId>a</artifactId></dependency><dependency><groupId>g</groupId><artifactId>extra</artifactId></dependency>');
  scrivi(join(modello, 'src/main/resources/application.properties'), 'k1=1\n');
  scrivi(join(modello, 'src/main/java/Res.java'), '@ApplicationScoped\nclass Res {}');

  const coppia = {
    id: 'finta',
    riferimento: rif,
    marcatore: 'pom.xml',
    controlli: ['file', 'pom', 'properties', 'annotazioni'],
    pom: 'pom.xml',
    properties: 'src/main/resources/application.properties',
  };
  // `riferimento` è assoluto: RADICE + percorso assoluto → percorso assoluto, quindi il confronto funziona.
  const div = confrontaCoppia(coppia, cfg, join(base, 'templates'));
  const tipi = div.map((d) => d.tipo);

  assert.ok(tipi.includes('file-mancante'), 'Solo.java assente dal modello');
  assert.ok(tipi.includes('dep-extra'), 'g:extra solo nel modello');
  assert.ok(tipi.includes('prop-mancante'), 'k2 assente dal modello');
  assert.ok(tipi.includes('annotazione'), '@RequiresEntitlement assente dal modello');
  assert.ok(div.some((d) => d.chiave.endsWith('#RequiresEntitlement')));
});

test('trovaRadiceModello individua la cartella col marcatore, comunque si chiami', () => {
  const base = mkdtempSync(join(tmpdir(), 'parita-radice-'));
  mkdirSync(join(base, 'services', '__app_id__'), { recursive: true });
  writeFileSync(join(base, 'services', '__app_id__', 'pom.xml.tmpl'), '<project/>');
  const radici = trovaRadiceModello(base, { marcatore: 'pom.xml' }, cfg);
  assert.deepEqual(radici, ['services/__app_id__']);
});

test('il messaggio di modelli assenti spiega cosa fare', () => {
  const testo = renderReport({ modelliAssenti: true, divergenze: [], derogate: [] }, cfg);
  assert.match(testo, /modelli-sorgente non ancora presenti/);
  assert.match(testo, /tools\/new-application\/templates/);
});
