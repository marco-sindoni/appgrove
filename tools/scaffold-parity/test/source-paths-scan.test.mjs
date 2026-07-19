// Test del rilevatore dei percorsi-sorgente (UC 0046, strato 2).

import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { test } from 'node:test';

import { RADICE } from '../parity-check.mjs';
import { caricaElenco, intersezione, renderReport, tocca } from '../source-paths-scan.mjs';

const elenco = caricaElenco();

test('un percorso tocca la sorgente se coincide o le sta dentro', () => {
  assert.ok(tocca('services/fatture', 'services/fatture'));
  assert.ok(tocca('services/fatture/pom.xml', 'services/fatture'));
  assert.ok(tocca('./services/fatture/pom.xml', 'services/fatture'));
  assert.ok(!tocca('services/fatture-altro/pom.xml', 'services/fatture'));
  assert.ok(!tocca('services/core/pom.xml', 'services/fatture'));
});

test('l’intersezione elenca i percorsi-sorgente colpiti e i file che li colpiscono', () => {
  const colpiti = intersezione(
    ['services/fatture/src/main/java/app/appgrove/fatture/InvoiceResource.java', 'docs/README.md', 'dev/Caddyfile'],
    elenco,
  );
  assert.deepEqual(
    colpiti.map((c) => c.percorso).sort(),
    ['dev/Caddyfile', 'services/fatture'],
  );
  assert.equal(colpiti.find((c) => c.percorso === 'services/fatture').file.length, 1);
});

test('nessuna intersezione quando la change non tocca sorgenti dei modelli', () => {
  const colpiti = intersezione(['docs/01-architettura.md', 'services/core/src/main/java/X.java'], elenco);
  assert.deepEqual(colpiti, []);
  assert.match(renderReport(colpiti), /nessun percorso-sorgente/);
});

test('il rapporto di intersezione spiega le due vie di uscita', () => {
  const testo = renderReport(intersezione(['services/commons/pom.xml'], elenco));
  assert.match(testo, /AGGIORNA I MODELLI/);
  assert.match(testo, /_PARITA-SCAFFOLD\.md/);
});

test('ogni percorso dichiarato esiste davvero nel repo e ha una motivazione', () => {
  for (const voce of elenco.percorsi) {
    assert.ok(existsSync(join(RADICE, voce.percorso)), `percorso-sorgente dichiarato ma inesistente: ${voce.percorso}`);
    assert.ok(voce.perche && voce.perche.length > 20, `motivazione mancante o troppo vaga per ${voce.percorso}`);
  }
});
