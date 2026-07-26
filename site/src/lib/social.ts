// Link social BRAND del sito vetrina (UC 0043 — lancio paid/social).
//
// Fonte unica: content/marketing/social.yaml (stesso pattern di content/legal/entity.yaml,
// letto con js-yaml). Il footer (BaseLayout.astro) mostra la sezione social SOLO se
// `SOCIAL_LINKS` non è vuoto: file vuoto / lista assente → niente sezione; voci compilate
// → lette e mostrate direttamente, senza toccare codice.
//
// Contratto: ogni voce deve avere `label` non vuota e `href` URL https ASSOLUTO. Una voce
// malformata è un errore BLOCCANTE del build (integrità, come i token legali) — così un
// URL sbagliato non finisce online in silenzio.
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import yaml from 'js-yaml'

const SOCIAL_YAML = path.resolve(
  fileURLToPath(new URL('.', import.meta.url)),
  '../../../content/marketing/social.yaml',
)

export interface SocialLink {
  /** Nome leggibile del canale, es. "LinkedIn". */
  label: string
  /** URL https assoluto del profilo brand. */
  href: string
}

/**
 * Interpreta il contenuto grezzo di social.yaml in una lista di link validati.
 * Ritorna lista vuota se il file è vuoto o non ha voci; LANCIA su voce malformata.
 * Funzione pura (nessun accesso al filesystem) per essere testabile in isolamento.
 */
export function parseSocialLinks(raw: string): SocialLink[] {
  const parsed = (yaml.load(raw) ?? {}) as Record<string, unknown>
  const links = parsed.links
  if (links == null) return []
  if (!Array.isArray(links)) {
    throw new Error('content/marketing/social.yaml: `links` deve essere una lista')
  }
  return links.map((entry, i) => {
    const e = entry as Record<string, unknown>
    const label = typeof e?.label === 'string' ? e.label.trim() : ''
    const href = typeof e?.href === 'string' ? e.href.trim() : ''
    if (!label) {
      throw new Error(`content/marketing/social.yaml: voce #${i + 1} senza \`label\``)
    }
    if (!/^https:\/\/\S+/.test(href)) {
      throw new Error(
        `content/marketing/social.yaml: voce "${label}" con \`href\` non valido (serve un URL https assoluto): ${href || '(vuoto)'}`,
      )
    }
    return { label, href }
  })
}

/** Link social del brand, letti da content/marketing/social.yaml. Vuoto = nessuna sezione. */
export const SOCIAL_LINKS: readonly SocialLink[] = parseSocialLinks(fs.readFileSync(SOCIAL_YAML, 'utf8'))
