// Libreria del controllo di copertura end-to-end (UC 0093).
//
// Confronta il registro `docs/testing/copertura-e2e.yaml` con la realtà del repository:
// i file di test che esistono davvero, le etichette `[ID]` che portano nei titoli e il
// catalogo degli use case. Nessuna euristica sulla prosa: solo dati verificabili.
//
// Tutte le funzioni lavorano su una `root` (la radice del repository o di una cartella di
// prova), così i test possono esercitarle su repository finti senza toccare quello vero.

import fs from 'node:fs'
import path from 'node:path'
import YAML from 'yaml'

/** Percorso del registro, relativo alla radice. */
export const REGISTRY_PATH = 'docs/testing/copertura-e2e.yaml'

/** Cartelle dei test end-to-end, con il livello che il registro deve dichiarare.
 *  Un solo `*` per segmento è supportato (basta e resta prevedibile). */
export const TEST_ROOTS = [
  { glob: 'tools/platform-e2e/journeys', livello: 'platform' },
  { glob: 'frontend/apps/*/e2e', livello: 'l2' },
  { glob: 'frontend/apps/*/e2e-l3', livello: 'l3' },
]

export const STATI = ['coperto', 'da-coprire', 'escluso']
export const LIVELLI = ['platform', 'l2', 'l3']
export const CATEGORIE = ['senza-superficie', 'vetrina-statica', 'non-implementato']

const ID_RE = /^[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+$/
const UC_RE = /^\d{4}$/
const TAG_IN_TITLE_RE = /^\[([A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+)\]\s+\S/
// Dichiarazione di un test Playwright il cui primo argomento è una stringa letterale.
// `test.skip(condizione, 'motivo')` non ha una stringa come primo argomento e non viene raccolto.
const TEST_DECL_RE = /\btest(?:\.(?:only|skip|fixme))?\s*\(\s*(['"`])((?:\\.|(?!\1)[\s\S])*?)\1/g

// ── lettura del registro ─────────────────────────────────────────────────────

/** Legge e interpreta il registro. Ritorna `{ registro }` oppure `{ errore }`. */
export function readRegistry(root) {
  const file = path.join(root, REGISTRY_PATH)
  if (!fs.existsSync(file)) return { errore: `registro assente: ${REGISTRY_PATH}` }
  try {
    const registro = YAML.parse(fs.readFileSync(file, 'utf8'))
    if (registro === null || typeof registro !== 'object' || Array.isArray(registro)) {
      return { errore: `${REGISTRY_PATH}: il contenuto non è una mappa YAML` }
    }
    return { registro }
  } catch (e) {
    return { errore: `${REGISTRY_PATH}: YAML non interpretabile — ${e.message}` }
  }
}

// ── catalogo degli use case ──────────────────────────────────────────────────

/**
 * Numeri (stringhe a 4 cifre) di tutti gli use case presenti in docs/usecases/.
 *
 * Guarda dentro ogni area E dentro un livello di SOTTOCARTELLE dell'area. Il secondo livello serve
 * perché un'area può organizzarsi in sottocartelle invece di tenere i file in piano: l'area 22
 * (rifacimento del modello di appartenenza) separa `story/` da `task/` e `epic/`. Senza questo, le
 * storie dentro `story/` sarebbero INVISIBILI al controllo — e quindi esenti di fatto dalla regola
 * «ogni use case è classificato», che è il presidio per cui questo file esiste. Un buco silenzioso
 * vale meno di zero: dà l'impressione che il registro sia completo.
 *
 * Un livello e non di più: basta per le strutture reali e resta prevedibile. Lo stesso numero trovato
 * in due punti (la storia in `story/`, il suo piano di lavoro in `task/`) conta una volta sola.
 */
export function listCatalogUseCases(root) {
  const base = path.join(root, 'docs/usecases')
  if (!fs.existsSync(base)) return []
  const numeri = new Set()
  const raccogli = (dir) => {
    for (const f of fs.readdirSync(dir)) {
      const m = /^(\d{4})-.*\.md$/.exec(f)
      if (m) numeri.add(m[1])
    }
  }
  for (const area of fs.readdirSync(base, { withFileTypes: true })) {
    if (!area.isDirectory()) continue
    const dirArea = path.join(base, area.name)
    raccogli(dirArea)
    for (const sub of fs.readdirSync(dirArea, { withFileTypes: true })) {
      if (sub.isDirectory()) raccogli(path.join(dirArea, sub.name))
    }
  }
  return [...numeri].sort()
}

/** Numeri degli use case per cui esiste già una cartella `changes/*-use-case-NNNN-*`. */
export function listImplementedUseCases(root) {
  const base = path.join(root, 'changes')
  if (!fs.existsSync(base)) return new Set()
  const numeri = new Set()
  for (const d of fs.readdirSync(base, { withFileTypes: true })) {
    if (!d.isDirectory()) continue
    const m = /-use-case-(\d{4})-/.exec(d.name)
    if (m) numeri.add(m[1])
  }
  return numeri
}

// ── scansione dei test ───────────────────────────────────────────────────────

function expandGlob(root, glob) {
  const parti = glob.split('/')
  let correnti = ['']
  for (const parte of parti) {
    const prossimi = []
    for (const c of correnti) {
      const dir = path.join(root, c)
      if (parte === '*') {
        if (!fs.existsSync(dir)) continue
        for (const d of fs.readdirSync(dir, { withFileTypes: true })) {
          if (d.isDirectory()) prossimi.push(path.posix.join(c, d.name))
        }
      } else {
        prossimi.push(path.posix.join(c, parte))
      }
    }
    correnti = prossimi
  }
  return correnti.filter((c) => fs.existsSync(path.join(root, c)) && fs.statSync(path.join(root, c)).isDirectory())
}

/**
 * Raccoglie tutte le dichiarazioni di test end-to-end del repository.
 * Ritorna un elenco di `{ file, livello, titolo, tag, riga }` con `tag` a `null` se il titolo
 * non porta l'etichetta.
 */
export function scanTests(root) {
  const trovati = []
  for (const { glob, livello } of TEST_ROOTS) {
    for (const dir of expandGlob(root, glob)) {
      for (const f of fs.readdirSync(path.join(root, dir)).sort()) {
        if (!f.endsWith('.spec.ts') && !f.endsWith('.spec.tsx')) continue
        const rel = path.posix.join(dir, f)
        const testo = fs.readFileSync(path.join(root, rel), 'utf8')
        TEST_DECL_RE.lastIndex = 0
        let m
        while ((m = TEST_DECL_RE.exec(testo)) !== null) {
          const titolo = m[2]
          const tag = TAG_IN_TITLE_RE.exec(titolo)
          trovati.push({
            file: rel,
            livello,
            titolo,
            tag: tag ? tag[1] : null,
            riga: testo.slice(0, m.index).split('\n').length,
          })
        }
      }
    }
  }
  return trovati
}

/** Il livello atteso per un file, dedotto dalla cartella in cui vive (`null` se fuori dalle cartelle note). */
export function livelloDelFile(root, rel) {
  for (const { glob, livello } of TEST_ROOTS) {
    for (const dir of expandGlob(root, glob)) {
      if (rel.startsWith(dir + '/')) return livello
    }
  }
  return null
}

// ── validazione ──────────────────────────────────────────────────────────────

const isStringaPiena = (v) => typeof v === 'string' && v.trim().length > 0

/**
 * Esegue tutte le regole del controllo. Ritorna un elenco di violazioni
 * `{ regola, messaggio }`; elenco vuoto = registro coerente con la realtà.
 */
export function validate(root) {
  const violazioni = []
  const ko = (regola, messaggio) => violazioni.push({ regola, messaggio })

  const { registro, errore } = readRegistry(root)
  if (errore) {
    ko('registro', errore)
    return violazioni
  }

  // ── 1. forma del registro ──────────────────────────────────────────────────
  const percorsi = Array.isArray(registro.percorsi) ? registro.percorsi : null
  if (!percorsi) ko('registro', 'campo `percorsi` mancante o non è un elenco')
  const conSuperficie = Array.isArray(registro.usecases_con_superficie) ? registro.usecases_con_superficie : null
  if (!conSuperficie) ko('registro', 'campo `usecases_con_superficie` mancante o non è un elenco')
  const esenzioni = Array.isArray(registro.esenzioni) ? registro.esenzioni : null
  if (!esenzioni) ko('registro', 'campo `esenzioni` mancante o non è un elenco')
  if (!percorsi || !conSuperficie || !esenzioni) return violazioni

  const catalogo = new Set(listCatalogUseCases(root))
  if (catalogo.size === 0) ko('catalogo', 'nessuno use case trovato in docs/usecases/: catalogo illeggibile')

  const idVisti = new Set()
  const fileDichiarati = new Map() // "ID\0file" → true
  const ucReferenziati = new Set()

  for (const [i, p] of percorsi.entries()) {
    const dove = isStringaPiena(p?.id) ? `percorso ${p.id}` : `percorsi[${i}]`
    if (!p || typeof p !== 'object') {
      ko('percorso', `${dove}: la voce non è una mappa`)
      continue
    }
    if (!isStringaPiena(p.id) || !ID_RE.test(p.id)) {
      ko('percorso', `${dove}: \`id\` mancante o non conforme (atteso es. J-BUY, L2-SHELL)`)
    } else if (idVisti.has(p.id)) {
      ko('percorso', `${dove}: identificativo duplicato`)
    } else {
      idVisti.add(p.id)
    }
    if (!isStringaPiena(p.titolo)) ko('percorso', `${dove}: \`titolo\` mancante`)

    if (!Array.isArray(p.usecases) || p.usecases.length === 0) {
      ko('percorso', `${dove}: \`usecases\` mancante o vuoto`)
    } else {
      for (const uc of p.usecases) {
        if (typeof uc !== 'string' || !UC_RE.test(uc)) {
          ko('percorso', `${dove}: use case "${uc}" non è un numero a 4 cifre fra virgolette`)
        } else if (!catalogo.has(uc)) {
          ko('percorso', `${dove}: use case ${uc} non esiste in docs/usecases/`)
        } else {
          ucReferenziati.add(uc)
        }
      }
    }

    if (!STATI.includes(p.stato)) {
      ko('percorso', `${dove}: \`stato\` mancante o non ammesso (ammessi: ${STATI.join(', ')})`)
      continue
    }

    if (p.stato === 'coperto') {
      if (!Array.isArray(p.test) || p.test.length === 0) {
        ko('coperto', `${dove}: dichiarato \`coperto\` ma senza alcun test — o si elenca il test, o lo stato è \`da-coprire\``)
        continue
      }
      for (const [j, t] of p.test.entries()) {
        const dt = `${dove}, test[${j}]`
        if (!t || typeof t !== 'object' || !isStringaPiena(t.file)) {
          ko('coperto', `${dt}: \`file\` mancante`)
          continue
        }
        if (!LIVELLI.includes(t.livello)) {
          ko('coperto', `${dt}: \`livello\` mancante o non ammesso (ammessi: ${LIVELLI.join(', ')})`)
        }
        const abs = path.join(root, t.file)
        if (!fs.existsSync(abs)) {
          ko('coperto', `${dt}: il file ${t.file} non esiste — test spostato o rinominato: aggiorna il registro`)
          continue
        }
        const atteso = livelloDelFile(root, t.file)
        if (atteso && t.livello && t.livello !== atteso) {
          ko('coperto', `${dt}: ${t.file} vive fra i test di livello "${atteso}" ma il registro dichiara "${t.livello}"`)
        }
        const contenuto = fs.readFileSync(abs, 'utf8')
        if (!contenuto.includes(`[${p.id}]`)) {
          ko('coperto', `${dt}: ${t.file} non contiene l'etichetta [${p.id}] — aggiungila in testa al titolo del test`)
        }
        fileDichiarati.set(`${p.id} ${t.file}`, true)
      }
    } else {
      if (p.test !== undefined) {
        ko('percorso', `${dove}: lo stato è \`${p.stato}\` ma è presente un elenco \`test\``)
      }
      if (!isStringaPiena(p.motivo)) {
        ko(p.stato, `${dove}: \`motivo\` mancante — uno stato \`${p.stato}\` senza motivo è un buco non dichiarato`)
      }
      if (p.stato === 'da-coprire') {
        if (!isStringaPiena(p.possiede) || !UC_RE.test(p.possiede)) {
          ko('da-coprire', `${dove}: \`possiede\` mancante — indica lo use case che sbloccherà la copertura`)
        } else if (!catalogo.has(p.possiede)) {
          ko('da-coprire', `${dove}: \`possiede\` punta allo use case ${p.possiede}, che non esiste in docs/usecases/`)
        }
      }
      if (p.stato === 'escluso' && p.possiede !== undefined) {
        ko('percorso', `${dove}: uno stato \`escluso\` non ha un proprietario (\`possiede\` va rimosso)`)
      }
    }
  }

  // ── 2. nessuna copertura fantasma: ogni etichetta nei test è nel registro ──
  for (const t of scanTests(root)) {
    if (!t.tag) {
      ko(
        'etichetta',
        `${t.file}:${t.riga}: il test «${t.titolo}» non porta l'etichetta del percorso — attesa la forma test('[ID] …')`,
      )
      continue
    }
    if (!idVisti.has(t.tag)) {
      ko('etichetta', `${t.file}:${t.riga}: etichetta [${t.tag}] assente dal registro — copertura fantasma`)
      continue
    }
    if (!fileDichiarati.has(`${t.tag} ${t.file}`)) {
      ko(
        'etichetta',
        `${t.file}:${t.riga}: il percorso ${t.tag} non dichiara questo file fra i suoi \`test\` — aggiungilo al registro`,
      )
    }
  }

  // ── 3. completezza del catalogo ───────────────────────────────────────────
  const superficie = new Set()
  for (const uc of conSuperficie) {
    if (typeof uc !== 'string' || !UC_RE.test(uc)) {
      ko('catalogo', `usecases_con_superficie: "${uc}" non è un numero a 4 cifre fra virgolette`)
      continue
    }
    if (!catalogo.has(uc)) ko('catalogo', `usecases_con_superficie: lo use case ${uc} non esiste in docs/usecases/`)
    if (superficie.has(uc)) ko('catalogo', `usecases_con_superficie: ${uc} elencato due volte`)
    superficie.add(uc)
  }

  const implementati = listImplementedUseCases(root)
  const esentati = new Set()
  for (const [i, e] of esenzioni.entries()) {
    const dove = isStringaPiena(e?.usecase) ? `esenzione ${e.usecase}` : `esenzioni[${i}]`
    if (!e || typeof e !== 'object' || typeof e.usecase !== 'string' || !UC_RE.test(e.usecase)) {
      ko('esenzione', `${dove}: \`usecase\` mancante o non è un numero a 4 cifre fra virgolette`)
      continue
    }
    if (!catalogo.has(e.usecase)) ko('esenzione', `${dove}: lo use case non esiste in docs/usecases/`)
    if (esentati.has(e.usecase)) ko('esenzione', `${dove}: esentato due volte`)
    esentati.add(e.usecase)
    if (superficie.has(e.usecase)) {
      ko('esenzione', `${dove}: è insieme in \`usecases_con_superficie\` e nelle esenzioni — scegli`)
    }
    if (!CATEGORIE.includes(e.categoria)) {
      ko('esenzione', `${dove}: \`categoria\` mancante o non ammessa (ammesse: ${CATEGORIE.join(', ')})`)
    }
    if (!isStringaPiena(e.motivo)) {
      ko('esenzione', `${dove}: \`motivo\` mancante — un'esenzione senza motivo non è verificabile`)
    }
    if (e.categoria === 'non-implementato' && implementati.has(e.usecase)) {
      ko(
        'esenzione',
        `${dove}: esentato come \`non-implementato\`, ma esiste già una cartella changes/*-use-case-${e.usecase}-* — ` +
          'la superficie ora esiste: classificala e dichiara il percorso che la copre',
      )
    }
  }

  for (const uc of [...catalogo].sort()) {
    if (!superficie.has(uc) && !esentati.has(uc)) {
      ko(
        'catalogo',
        `use case ${uc} non classificato: va in \`usecases_con_superficie\` oppure fra le \`esenzioni\` con categoria e motivo`,
      )
    }
  }

  // ── 4. ogni superficie esistente è referenziata da almeno un percorso ─────
  for (const uc of [...superficie].sort()) {
    if (!ucReferenziati.has(uc)) {
      ko(
        'superficie-scoperta',
        `use case ${uc} ha superficie in main ma nessun percorso lo referenzia — aggiungi una voce (anche \`da-coprire\`)`,
      )
    }
  }

  return violazioni
}
