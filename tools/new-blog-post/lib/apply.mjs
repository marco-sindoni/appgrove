// ─────────────────────────────────────────────────────────────────────────────
// tools/new-blog-post/lib/apply.mjs — scrittura TRANSAZIONALE sul filesystem (UC 0084).
//
// Il requisito è "rieseguire su uno slug esistente non corrompe il registro", e validare
// prima non basta: se la scrittura si interrompe a metà (permessi, disco, un file del
// pilastro in formato inatteso) il repository resterebbe in uno stato ibrido — cartella
// creata ma registro non aggiornato — che è peggio del rifiuto.
//
// Qui le operazioni si accumulano su uno storico minimo (contenuto originale dei file
// toccati, elenco di ciò che è stato creato o rimosso) e, al primo errore, si torna
// indietro. Lo stesso meccanismo serve la generazione e la rimozione.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'

export function createTransaction() {
  const originals = new Map() // file esistente toccato → contenuto originale
  const createdFiles = [] // file nati dalla transazione → da cancellare al ripristino
  const createdDirs = [] // cartelle nate dalla transazione → da rimuovere al ripristino
  const removedDirs = [] // cartelle cancellate → da ricreare al ripristino

  function remember(file) {
    if (originals.has(file) || createdFiles.includes(file)) return
    if (fs.existsSync(file)) originals.set(file, fs.readFileSync(file, 'utf8'))
    else createdFiles.push(file)
  }

  return {
    /** Crea una cartella (e le intermedie), ricordando quelle davvero create. */
    mkdir(dir) {
      const missing = []
      let cur = dir
      while (!fs.existsSync(cur)) {
        missing.unshift(cur)
        const parent = path.dirname(cur)
        if (parent === cur) break
        cur = parent
      }
      fs.mkdirSync(dir, { recursive: true })
      createdDirs.push(...missing)
    },

    /** Scrive un file (nuovo o esistente), ricordando com'era prima. */
    write(file, content) {
      remember(file)
      fs.writeFileSync(file, content)
    },

    /** Cancella un file, ricordandone il contenuto. */
    unlink(file) {
      remember(file)
      if (fs.existsSync(file)) fs.unlinkSync(file)
    },

    /** Rimuove una cartella vuota, ricordando che andrà ricreata al ripristino. */
    rmdir(dir) {
      if (!fs.existsSync(dir)) return
      fs.rmdirSync(dir)
      removedDirs.push(dir)
    },

    /** Riporta il filesystem esattamente com'era prima della transazione. */
    rollback() {
      for (const dir of removedDirs) {
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
      }
      for (const [file, content] of originals) fs.writeFileSync(file, content)
      for (const file of [...createdFiles].reverse()) {
        if (fs.existsSync(file)) fs.unlinkSync(file)
      }
      for (const dir of [...createdDirs].reverse()) {
        if (fs.existsSync(dir) && fs.readdirSync(dir).length === 0) fs.rmdirSync(dir)
      }
      originals.clear()
      createdFiles.length = 0
      createdDirs.length = 0
      removedDirs.length = 0
    },
  }
}

/** Esegue `fn` dentro una transazione: al primo errore ripristina e rilancia. */
export function transactional(fn) {
  const tx = createTransaction()
  try {
    return fn(tx)
  } catch (err) {
    tx.rollback()
    throw err
  }
}
