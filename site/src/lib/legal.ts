// Renderer dei documenti legali del sito (UC 0036).
//
// Fonte unica: content/legal/*.md (5 lingue) + content/legal/entity.yaml
// (dati del titolare). Gli stessi markdown servono sito e, in futuro, il
// rendering in-app (UC 0056): non si duplicano i testi.
//
// Contratto di sostituzione dei token (posseduto da questa UC, ereditato da
// UC 0002): ogni `{{titolare.<campo>}}` è sostituito col valore da entity.yaml
// PRIMA di emettere l'HTML. Un token senza chiave è un ERRORE di build. I valori
// ancora "DA COMPILARE" restano tali (attesi pre-go-live, UC 0001).

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import matter from 'gray-matter'
import { marked } from 'marked'
import yaml from 'js-yaml'
import { LOCALES, type Locale } from './i18n.ts'

// content/legal/ sta alla radice del repo; questo file è in site/src/lib/.
const LEGAL_DIR = path.resolve(fileURLToPath(new URL('.', import.meta.url)), '../../../content/legal')

/** I 5 componenti legali pubblici (allineati a content/legal/_config.yaml). */
export const LEGAL_COMPONENTS = ['privacy', 'terms', 'refund', 'cookie', 'subprocessors'] as const
export type LegalComponent = (typeof LEGAL_COMPONENTS)[number]

export interface LegalFrontmatter {
  version: string
  effective_date: string
  lang: string
  status?: string
}

export interface LegalDoc {
  component: LegalComponent
  lang: Locale
  frontmatter: LegalFrontmatter
  html: string
}

/**
 * Gate di pubblicazione (#14 52): un contenuto è online salvo che dichiari
 * esplicitamente `status: draft` (o uno stato diverso da `published`). I legali
 * non portano `status` → sempre pubblicati.
 */
export function isPublished(frontmatter: { status?: string }): boolean {
  const status = frontmatter.status
  return status === undefined || status === 'published'
}

/** Legge i valori del titolare come mappa piatta `titolare.<campo>` → stringa. */
export function loadEntity(): Record<string, string> {
  const raw = fs.readFileSync(path.join(LEGAL_DIR, 'entity.yaml'), 'utf8')
  const parsed = yaml.load(raw) as Record<string, unknown>
  const flat: Record<string, string> = {}
  const walk = (obj: Record<string, unknown>, prefix: string) => {
    for (const [key, value] of Object.entries(obj)) {
      const dotted = prefix ? `${prefix}.${key}` : key
      if (value !== null && typeof value === 'object') {
        walk(value as Record<string, unknown>, dotted)
      } else {
        flat[dotted] = String(value)
      }
    }
  }
  walk(parsed, '')
  return flat
}

const TOKEN_RE = /\{\{\s*([a-zA-Z0-9_.]+)\s*\}\}/g

/**
 * Sostituisce i token `{{titolare.<campo>}}` con i valori di `entity`.
 * Lancia se un token non risolve (integrità referenziale, come il check CI).
 */
export function substituteTokens(markdown: string, entity: Record<string, string>): string {
  return markdown.replace(TOKEN_RE, (_match, key: string) => {
    if (!(key in entity)) {
      throw new Error(`Token legale non risolto: {{${key}}} — nessuna chiave "${key}" in content/legal/entity.yaml`)
    }
    return entity[key]
  })
}

/** Carica, sostituisce i token e converte in HTML un documento legale. */
export function loadLegalDoc(component: LegalComponent, lang: Locale): LegalDoc {
  const file = path.join(LEGAL_DIR, `${component}.${lang}.md`)
  const raw = fs.readFileSync(file, 'utf8')
  const { data, content } = matter(raw)
  const frontmatter = data as LegalFrontmatter
  const substituted = substituteTokens(content, loadEntity())
  const html = marked.parse(substituted, { async: false }) as string
  return { component, lang, frontmatter, html }
}

/**
 * Tutte le combinazioni (componente × lingua) PUBBLICATE. Alimenta getStaticPaths.
 * La parità 5 lingue è garantita dal check dedicato: qui si emette ciò che esiste.
 */
export function publishedLegalPaths(): Array<{ component: LegalComponent; lang: Locale }> {
  const out: Array<{ component: LegalComponent; lang: Locale }> = []
  for (const component of LEGAL_COMPONENTS) {
    for (const lang of LOCALES) {
      const file = path.join(LEGAL_DIR, `${component}.${lang}.md`)
      if (!fs.existsSync(file)) continue
      const { data } = matter(fs.readFileSync(file, 'utf8'))
      if (isPublished(data)) out.push({ component, lang })
    }
  }
  return out
}
