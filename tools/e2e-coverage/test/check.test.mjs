// Test del controllo di copertura end-to-end (UC 0093) su cartelle di prova.
//
// Ogni test costruisce un repository finto minimo (catalogo use case + un file di test + registro),
// poi verifica che `validate()` sia verde o rosso PER LA REGOLA GIUSTA. Nessun test tocca il
// repository vero: lo fa `check.test-repo-reale.mjs`.

import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import YAML from 'yaml'
import { validate, scanTests } from '../lib.mjs'

// ── impalcatura ──────────────────────────────────────────────────────────────

function scrivi(root, rel, contenuto) {
  const abs = path.join(root, rel)
  fs.mkdirSync(path.dirname(abs), { recursive: true })
  fs.writeFileSync(abs, contenuto)
}

/**
 * Repository di prova: due use case (0001 con superficie, 0002 senza), un file di test di
 * piattaforma etichettato [J-UNO], e un registro coerente. Le `modifiche` permettono a ogni
 * test di rompere UNA cosa sola.
 */
function repoDiProva(modifiche = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'e2e-coverage-'))
  scrivi(root, 'docs/usecases/01-area/0001-primo.md', '# UC 0001\n')
  scrivi(root, 'docs/usecases/01-area/0002-secondo.md', '# UC 0002\n')
  scrivi(
    root,
    'tools/platform-e2e/journeys/J-UNO.spec.ts',
    "import { test } from '@playwright/test'\ntest('[J-UNO] primo percorso', async () => {})\n",
  )
  const registro = {
    versione: 1,
    usecases_con_superficie: ['0001'],
    percorsi: [
      {
        id: 'J-UNO',
        titolo: 'Primo percorso',
        usecases: ['0001'],
        stato: 'coperto',
        test: [{ livello: 'platform', file: 'tools/platform-e2e/journeys/J-UNO.spec.ts' }],
      },
    ],
    esenzioni: [{ usecase: '0002', categoria: 'senza-superficie', motivo: 'Servizio senza interfaccia.' }],
  }
  const finale = modifiche.registro ? modifiche.registro(registro) : registro
  scrivi(root, 'docs/testing/copertura-e2e.yaml', YAML.stringify(finale))
  if (modifiche.dopo) modifiche.dopo(root)
  return root
}

const regole = (root) => validate(root).map((v) => v.regola)
const messaggi = (root) => validate(root).map((v) => v.messaggio).join('\n')

// ── il caso felice ───────────────────────────────────────────────────────────

test('registro valido → nessuna violazione', () => {
  assert.deepEqual(validate(repoDiProva()), [])
})

test('registro assente → una violazione parlante', () => {
  const root = repoDiProva()
  fs.rmSync(path.join(root, 'docs/testing/copertura-e2e.yaml'))
  const v = validate(root)
  assert.equal(v.length, 1)
  assert.match(v[0].messaggio, /registro assente/)
})

test('YAML malformato → violazione, senza eccezione', () => {
  const root = repoDiProva()
  scrivi(root, 'docs/testing/copertura-e2e.yaml', 'percorsi: [\n  - id: "J-UNO"\n   sbagliato\n')
  const v = validate(root)
  assert.equal(v.length, 1)
  assert.equal(v[0].regola, 'registro')
})

// ── voci `coperto` ───────────────────────────────────────────────────────────

test('voce `coperto` senza test → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      delete r.percorsi[0].test
      return r
    },
  })
  assert.ok(regole(root).includes('coperto'))
  assert.match(messaggi(root), /senza alcun test/)
})

test('voce `coperto` che punta a un file inesistente → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.percorsi[0].test[0].file = 'tools/platform-e2e/journeys/SPOSTATO.spec.ts'
      return r
    },
  })
  assert.match(messaggi(root), /non esiste — test spostato o rinominato/)
})

test('file esistente ma senza etichetta nel titolo → rosso', () => {
  const root = repoDiProva({
    dopo: (root) =>
      scrivi(
        root,
        'tools/platform-e2e/journeys/J-UNO.spec.ts',
        "import { test } from '@playwright/test'\ntest('primo percorso', async () => {})\n",
      ),
  })
  assert.match(messaggi(root), /non contiene l'etichetta \[J-UNO\]/)
})

test('livello dichiarato incoerente con la cartella → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.percorsi[0].test[0].livello = 'l2'
      return r
    },
  })
  assert.match(messaggi(root), /vive fra i test di livello "platform" ma il registro dichiara "l2"/)
})

// ── etichette nei test ───────────────────────────────────────────────────────

test('etichetta presente nei test ma assente dal registro → rosso (copertura fantasma)', () => {
  const root = repoDiProva({
    dopo: (root) =>
      scrivi(
        root,
        'tools/platform-e2e/journeys/J-DUE.spec.ts',
        "import { test } from '@playwright/test'\ntest('[J-DUE] percorso non registrato', async () => {})\n",
      ),
  })
  assert.match(messaggi(root), /etichetta \[J-DUE\] assente dal registro — copertura fantasma/)
})

test('etichetta nota ma in un file non dichiarato dal percorso → rosso', () => {
  const root = repoDiProva({
    dopo: (root) =>
      scrivi(
        root,
        'tools/platform-e2e/journeys/J-UNO-bis.spec.ts',
        "import { test } from '@playwright/test'\ntest('[J-UNO] altro file', async () => {})\n",
      ),
  })
  assert.match(messaggi(root), /non dichiara questo file fra i suoi `test`/)
})

test('test senza etichetta → rosso', () => {
  const root = repoDiProva({
    dopo: (root) =>
      scrivi(
        root,
        'tools/platform-e2e/journeys/J-UNO.spec.ts',
        "import { test } from '@playwright/test'\n" +
          "test('[J-UNO] primo percorso', async () => {})\n" +
          "test('secondo test senza etichetta', async () => {})\n",
      ),
  })
  assert.match(messaggi(root), /non porta l'etichetta del percorso/)
})

test("test.skip(condizione, 'motivo') non viene scambiato per una dichiarazione di test", () => {
  const root = repoDiProva({
    dopo: (root) =>
      scrivi(
        root,
        'tools/platform-e2e/journeys/J-UNO.spec.ts',
        "import { test } from '@playwright/test'\n" +
          "test.describe('gruppo', () => {\n" +
          "  test.skip(!process.env.ATTIVO, 'serve una variabile d\\'ambiente')\n" +
          "  test('[J-UNO] primo percorso', async () => {})\n" +
          '})\n',
      ),
  })
  assert.deepEqual(validate(root), [])
  assert.deepEqual(
    scanTests(root).map((t) => t.tag),
    ['J-UNO'],
  )
})

// ── voci `da-coprire` ed `escluso` ───────────────────────────────────────────

test('voce `da-coprire` senza proprietario → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.percorsi.push({ id: 'J-TRE', titolo: 'Buco', usecases: ['0001'], stato: 'da-coprire', motivo: 'Non pronto.' })
      return r
    },
  })
  assert.ok(regole(root).includes('da-coprire'))
  assert.match(messaggi(root), /`possiede` mancante/)
})

test('voce `da-coprire` senza motivo → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.percorsi.push({ id: 'J-TRE', titolo: 'Buco', usecases: ['0001'], stato: 'da-coprire', possiede: '0002' })
      return r
    },
  })
  assert.match(messaggi(root), /`motivo` mancante/)
})

test('voce `da-coprire` con proprietario inesistente → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.percorsi.push({
        id: 'J-TRE',
        titolo: 'Buco',
        usecases: ['0001'],
        stato: 'da-coprire',
        motivo: 'Non pronto.',
        possiede: '9999',
      })
      return r
    },
  })
  assert.match(messaggi(root), /punta allo use case 9999, che non esiste/)
})

test('voce `escluso` senza motivo → rosso; con motivo → verde', () => {
  const senza = repoDiProva({
    registro: (r) => {
      r.percorsi.push({ id: 'J-QUATTRO', titolo: 'Fuori perimetro', usecases: ['0001'], stato: 'escluso' })
      return r
    },
  })
  assert.match(messaggi(senza), /`motivo` mancante/)

  const con = repoDiProva({
    registro: (r) => {
      r.percorsi.push({
        id: 'J-QUATTRO',
        titolo: 'Fuori perimetro',
        usecases: ['0001'],
        stato: 'escluso',
        motivo: 'Coperto in modo esaustivo a un livello inferiore.',
      })
      return r
    },
  })
  assert.deepEqual(validate(con), [])
})

test('identificativo duplicato → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.percorsi.push({ ...r.percorsi[0] })
      return r
    },
  })
  assert.match(messaggi(root), /identificativo duplicato/)
})

// ── completezza del catalogo ─────────────────────────────────────────────────

test('use case del catalogo non classificato → rosso', () => {
  const root = repoDiProva({
    dopo: (root) => scrivi(root, 'docs/usecases/01-area/0003-terzo.md', '# UC 0003\n'),
  })
  assert.match(messaggi(root), /use case 0003 non classificato/)
})

test('esenzione senza motivo → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      delete r.esenzioni[0].motivo
      return r
    },
  })
  assert.ok(regole(root).includes('esenzione'))
  assert.match(messaggi(root), /`motivo` mancante/)
})

test('esenzione con categoria non ammessa → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.esenzioni[0].categoria = 'boh'
      return r
    },
  })
  assert.match(messaggi(root), /`categoria` mancante o non ammessa/)
})

test('use case insieme fra le superfici e fra le esenzioni → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.esenzioni.push({ usecase: '0001', categoria: 'senza-superficie', motivo: 'Ripensamento.' })
      return r
    },
  })
  assert.match(messaggi(root), /è insieme in `usecases_con_superficie` e nelle esenzioni/)
})

test('esenzione `non-implementato` scaduta (esiste già la change) → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.esenzioni[0].categoria = 'non-implementato'
      r.esenzioni[0].motivo = 'Storia evolutiva non ancora implementata.'
      return r
    },
    dopo: (root) => fs.mkdirSync(path.join(root, 'changes/0042-use-case-0002-secondo'), { recursive: true }),
  })
  assert.match(messaggi(root), /esiste già una cartella changes\/\*-use-case-0002-\*/)
})

test('esenzione `non-implementato` ancora valida (nessuna change) → verde', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.esenzioni[0].categoria = 'non-implementato'
      r.esenzioni[0].motivo = 'Storia evolutiva non ancora implementata.'
      return r
    },
    dopo: (root) => fs.mkdirSync(path.join(root, 'changes/0041-altra-change'), { recursive: true }),
  })
  assert.deepEqual(validate(root), [])
})

// ── superfici scoperte ───────────────────────────────────────────────────────

test('use case con superficie non referenziato da alcun percorso → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.usecases_con_superficie.push('0002')
      r.esenzioni = []
      return r
    },
  })
  assert.ok(regole(root).includes('superficie-scoperta'))
  assert.match(messaggi(root), /use case 0002 ha superficie in main ma nessun percorso lo referenzia/)
})

test('una voce `da-coprire` basta a coprire il requisito di referenza', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.usecases_con_superficie.push('0002')
      r.esenzioni = []
      r.percorsi.push({
        id: 'J-DUE',
        titolo: 'Buco dichiarato',
        usecases: ['0002'],
        stato: 'da-coprire',
        motivo: 'Superficie appena nata, percorso non ancora scritto.',
        possiede: '0002',
      })
      return r
    },
  })
  assert.deepEqual(validate(root), [])
})

test('use case citato da un percorso ma inesistente nel catalogo → rosso', () => {
  const root = repoDiProva({
    registro: (r) => {
      r.percorsi[0].usecases.push('9999')
      return r
    },
  })
  assert.match(messaggi(root), /use case 9999 non esiste in docs\/usecases\//)
})

// ── collaudo di PROCESSO (UC 0094) ───────────────────────────────────────────
//
// I test qui sopra verificano una regola alla volta. Questi verificano gli SCENARI del workflow:
// le tre situazioni in cui una change o uno use case nuovo lasciano indietro il registro, e la
// rimediazione che il processo prescrive. Sono la dimostrazione che il presidio morde davvero nei
// punti in cui le skill sono state agganciate — e che la via d'uscita esiste ed è quella scritta.

test('processo — change con superficie nuova e registro non aggiornato → rosso; con la voce → verde', () => {
  // La change aggiunge un test di livello 2 etichettato ma non tocca il registro: copertura fantasma.
  const rosso = repoDiProva({
    dopo: (root) =>
      scrivi(
        root,
        'frontend/apps/backoffice/e2e/nuova-schermata.spec.ts',
        "import { test } from '@playwright/test'\ntest('[L2-NUOVA] nuova schermata', async () => {})\n",
      ),
  })
  assert.ok(regole(rosso).includes('etichetta'))
  assert.match(messaggi(rosso), /etichetta \[L2-NUOVA\] assente dal registro/)

  // Stessa change, con il passo di copertura eseguito: la voce c'è e dichiara il file.
  const verde = repoDiProva({
    registro: (r) => {
      r.percorsi.push({
        id: 'L2-NUOVA',
        titolo: 'Nuova schermata',
        usecases: ['0001'],
        stato: 'coperto',
        test: [{ livello: 'l2', file: 'frontend/apps/backoffice/e2e/nuova-schermata.spec.ts' }],
      })
      return r
    },
    dopo: (root) =>
      scrivi(
        root,
        'frontend/apps/backoffice/e2e/nuova-schermata.spec.ts',
        "import { test } from '@playwright/test'\ntest('[L2-NUOVA] nuova schermata', async () => {})\n",
      ),
  })
  assert.deepEqual(validate(verde), [])
})

test('processo — change con superficie nuova e test senza etichetta → rosso (il test non si aggancia a nulla)', () => {
  const root = repoDiProva({
    dopo: (root) =>
      scrivi(
        root,
        'frontend/apps/backoffice/e2e/nuova-schermata.spec.ts',
        "import { test } from '@playwright/test'\ntest('nuova schermata', async () => {})\n",
      ),
  })
  assert.match(messaggi(root), /non porta l'etichetta del percorso/)
})

test('processo — storia evolutiva implementata e ancora esentata `non-implementato` → rosso; riclassificata → verde', () => {
  const modificheComuni = {
    dopo: (root) => fs.mkdirSync(path.join(root, 'changes/0074-use-case-0002-storia-evolutiva'), { recursive: true }),
  }

  const rosso = repoDiProva({
    ...modificheComuni,
    registro: (r) => {
      r.esenzioni[0].categoria = 'non-implementato'
      r.esenzioni[0].motivo = 'Storia evolutiva non ancora implementata.'
      return r
    },
  })
  assert.ok(regole(rosso).includes('esenzione'))
  assert.match(messaggi(rosso), /la superficie ora esiste: classificala/)

  // Rimedio prescritto da new-change: via l'esenzione, dentro la superficie e almeno un percorso.
  const verde = repoDiProva({
    ...modificheComuni,
    registro: (r) => {
      r.esenzioni = []
      r.usecases_con_superficie.push('0002')
      r.percorsi.push({
        id: 'J-DUE',
        titolo: 'Percorso della storia evolutiva',
        usecases: ['0002'],
        stato: 'da-coprire',
        motivo: 'Superficie appena nata: il percorso arriva con la storia successiva.',
        possiede: '0002',
      })
      return r
    },
  })
  assert.deepEqual(validate(verde), [])
})

test('processo — use case scaffoldato e non classificato → rosso; classificato allo scaffolding → verde', () => {
  const nuovoUseCase = (root) => scrivi(root, 'docs/usecases/01-area/0003-terzo.md', '# UC 0003\n')

  const rosso = repoDiProva({ dopo: nuovoUseCase })
  assert.ok(regole(rosso).includes('catalogo'))
  assert.match(messaggi(rosso), /use case 0003 non classificato/)

  const verde = repoDiProva({
    dopo: nuovoUseCase,
    registro: (r) => {
      r.esenzioni.push({
        usecase: '0003',
        categoria: 'non-implementato',
        motivo: 'Superficie non ancora esistente in main.',
      })
      return r
    },
  })
  assert.deepEqual(validate(verde), [])
})
