// ─────────────────────────────────────────────────────────────────────────────
// tools/new-blog-post/lib/scaffold.mjs — le operazioni complete (UC 0084).
//
// Mette insieme i tre pezzi: validazione della specifica (spec.mjs), resa dei file
// (render.mjs) e modifiche al registro (registry.mjs), dentro la scrittura transazionale
// (apply.mjs). L'ordine è sempre lo stesso e non è negoziabile: PRIMA tutti i controlli,
// POI tutte le scritture. Una specifica difettosa non deve lasciare traccia sul disco.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { LOCALES, normalizeSpec, validateSpecShape } from './spec.mjs'
import { renderLocaleFile, renderPostIndex } from './render.mjs'
import {
  addClusterKey,
  addToRegistry,
  blogDir,
  postDir,
  publishedAppIds,
  readAllPosts,
  readPost,
  registryFile,
  removeClusterKey,
  removeFromRegistry,
} from './registry.mjs'
import { transactional } from './apply.mjs'

/**
 * Controlli che richiedono di guardare il registro REALE: chiavi e slug già usati, pilastro
 * di destinazione esistente e davvero pilastro, app collegata a una landing pubblicata.
 * I post della specifica si vedono fra loro (un pilastro nuovo vale per l'articolo che lo
 * segue), così "pilastro assente" si risolve in una sola esecuzione.
 */
export function validateAgainstRegistry(repoRoot, posts) {
  const errors = []
  const existing = readAllPosts(repoRoot)
  const published = publishedAppIds(repoRoot)

  const known = new Map(existing.map((p) => [p.key, p]))
  const slugOwner = new Map() // `${lang}:${slug}` → chiave del post
  for (const p of existing) {
    for (const [lang, slug] of Object.entries(p.slugs)) slugOwner.set(`${lang}:${slug}`, p.key)
  }

  for (const p of posts) {
    if (known.has(p.key)) {
      errors.push(`${p.key}: esiste già un post con questa chiave (scegli una chiave diversa)`)
    }
    if (fs.existsSync(postDir(repoRoot, p.key))) {
      errors.push(`${p.key}: la cartella site/src/content/blog/${p.key}/ esiste già`)
    }
    if (p.appId && !published.includes(p.appId)) {
      errors.push(
        `${p.key}: l'app "${p.appId}" non ha una landing pubblicata — ` +
          `scegli un'app pubblicata (${published.join(', ') || 'nessuna'}) o rimanda l'articolo`,
      )
    }
    for (const lang of LOCALES) {
      const slug = p.content?.[lang]?.slug
      if (!slug) continue
      const owner = slugOwner.get(`${lang}:${slug}`)
      if (owner && owner !== p.key) {
        errors.push(`${p.key}/${lang}: slug "${slug}" già usato dal post "${owner}" — servirebbe uno slug diverso`)
      }
    }
    if (p.kind === 'article' && p.pillarKey) {
      const parent = known.get(p.pillarKey)
      if (!parent) {
        errors.push(
          `${p.key}: il pilastro "${p.pillarKey}" non esiste — ` +
            `mettilo nella stessa specifica prima dell'articolo, oppure scegline uno esistente`,
        )
      } else if (parent.kind !== 'pillar') {
        errors.push(`${p.key}: "${p.pillarKey}" esiste ma non è un pilastro`)
      }
    }
    // Il post appena validato diventa visibile a quelli successivi della stessa specifica.
    known.set(p.key, { key: p.key, kind: p.kind, slugs: {} })
    for (const lang of LOCALES) {
      const slug = p.content?.[lang]?.slug
      if (slug) slugOwner.set(`${lang}:${slug}`, p.key)
    }
  }

  return errors
}

/** Validazione completa (forma + registro reale). Ritorna `{ posts, errors }`. */
export function validate(repoRoot, raw) {
  const posts = normalizeSpec(raw)
  const errors = validateSpecShape(posts)
  if (errors.length > 0) return { posts, errors }
  return { posts, errors: validateAgainstRegistry(repoRoot, posts) }
}

/**
 * Materializza i post della specifica. Presuppone la validazione già passata (chi chiama
 * deve averla eseguita: scrivere senza validare è un difetto). Ritorna l'elenco dei file
 * creati o modificati.
 */
export function scaffold(repoRoot, posts) {
  return transactional((tx) => {
    const touched = []
    for (const post of posts) {
      const dir = postDir(repoRoot, post.key)
      tx.mkdir(dir)
      for (const lang of LOCALES) {
        const file = path.join(dir, `${lang}.ts`)
        tx.write(file, renderLocaleFile(post, lang))
        touched.push(file)
      }
      const idx = path.join(dir, 'index.ts')
      tx.write(idx, renderPostIndex(post))
      touched.push(idx)

      const reg = registryFile(repoRoot)
      tx.write(reg, addToRegistry(fs.readFileSync(reg, 'utf8'), post.key))
      touched.push(reg)

      if (post.kind === 'article') {
        const pillarFile = path.join(postDir(repoRoot, post.pillarKey), 'index.ts')
        tx.write(pillarFile, addClusterKey(fs.readFileSync(pillarFile, 'utf8'), post.key))
        touched.push(pillarFile)
      }
    }
    return [...new Set(touched)]
  })
}

/**
 * Inverso della generazione: toglie i post indicati (cartella, entry nel registro,
 * riferimento nel pilastro). Serve al ripristino quando il build del sito diventa rosso
 * dopo la generazione, e al collaudo andata-ritorno.
 */
export function remove(repoRoot, keys) {
  return transactional((tx) => {
    const touched = []
    for (const key of keys) {
      const dir = postDir(repoRoot, key)
      if (!fs.existsSync(dir)) throw new Error(`il post "${key}" non esiste in site/src/content/blog/`)
      const post = readPost(repoRoot, key)

      if (post.kind === 'article' && post.pillarKey) {
        const pillarFile = path.join(postDir(repoRoot, post.pillarKey), 'index.ts')
        if (fs.existsSync(pillarFile)) {
          tx.write(pillarFile, removeClusterKey(fs.readFileSync(pillarFile, 'utf8'), key))
          touched.push(pillarFile)
        }
      }

      const reg = registryFile(repoRoot)
      tx.write(reg, removeFromRegistry(fs.readFileSync(reg, 'utf8'), key))
      touched.push(reg)

      for (const entry of fs.readdirSync(dir)) tx.unlink(path.join(dir, entry))
      tx.rmdir(dir)
      touched.push(dir)
    }
    return [...new Set(touched)]
  })
}

/** Vista del registro per il co-pilota: pilastri con i loro cluster, più eventuali orfani. */
export function outline(repoRoot) {
  const posts = readAllPosts(repoRoot)
  const byKey = new Map(posts.map((p) => [p.key, p]))
  const pillars = posts
    .filter((p) => p.kind === 'pillar')
    .map((p) => ({
      key: p.key,
      appId: p.appId,
      slugEn: p.slugs.en,
      clusters: (p.clusterKeys ?? []).map((k) => ({
        key: k,
        appId: byKey.get(k)?.appId,
        slugEn: byKey.get(k)?.slugs?.en,
      })),
    }))
  const orphans = posts
    .filter((p) => p.kind === 'article' && (!p.pillarKey || !byKey.has(p.pillarKey)))
    .map((p) => ({ key: p.key, appId: p.appId, slugEn: p.slugs.en }))
  return { blogDir: blogDir(repoRoot), pillars, orphans, total: posts.length }
}
