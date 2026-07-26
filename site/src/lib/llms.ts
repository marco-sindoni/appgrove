// Costruttore di /llms.txt (UC 0041, GEO — #14 38): il "robots.txt per gli assistenti
// AI", un riassunto markdown curato che aiuta gli LLM a capire e citare correttamente
// appgrove. Logica pura e testabile; gli endpoint (src/pages/llms.txt.ts radice +
// src/pages/[lang]/llms.txt.ts per-lingua) sono gusci sottili.
//
// MULTILINGUA (scelta dello sviluppatore): oltre alla variante radice in inglese
// (/llms.txt), una variante per ciascuna lingua (/<lang>/llms.txt). Il testo di ogni
// variante è LOCALIZZATO attingendo ai contenuti marketing già tradotti (UC 0037); solo
// l'identità di marca (nome, dominio) viene dal boilerplate canonico brand.ts. La variante
// radice aggiunge in coda una sezione con i link alle altre lingue.
//
// Coerenza col gate di indicizzazione: llms.txt è sempre presente, ma pre-go-live è il
// robots.txt (Disallow totale) a tenere fuori i crawler; al go-live scatta il consenso.

import { LOCALES, LOCALE_LABELS, type Locale } from './i18n.ts'
import { MARKETING } from '../content/marketing/index.ts'
import { brandPath } from './routes.ts'
import { publishedLandings } from './landings.ts'
import { BRAND } from './brand.ts'
import { BLOG_UI } from '../content/blog/ui.ts'
import type { Landing } from '../content/landings/types.ts'

/** Etichette localizzate delle sezioni e dei link (poche, tenute qui per non spargere i18n). */
const LABELS: Record<Locale, {
  facts: string
  pages: string
  apps: string
  languages: string
  home: string
  privacy: string
  terms: string
  noApps: string
}> = {
  en: { facts: 'Key facts', pages: 'Key pages', apps: 'Apps', languages: 'Other languages', home: 'Home', privacy: 'Privacy Policy', terms: 'Terms of Service', noApps: 'No apps are published yet — the catalogue is small on purpose and growing.' },
  it: { facts: 'Fatti chiave', pages: 'Pagine chiave', apps: 'App', languages: 'Altre lingue', home: 'Home', privacy: 'Informativa privacy', terms: 'Termini di servizio', noApps: 'Nessuna app ancora pubblicata — il catalogo è piccolo di proposito e cresce.' },
  fr: { facts: 'Faits clés', pages: 'Pages clés', apps: 'Applications', languages: 'Autres langues', home: 'Accueil', privacy: 'Politique de confidentialité', terms: 'Conditions de service', noApps: "Aucune application publiée pour l'instant — le catalogue est volontairement restreint et s'étoffe." },
  es: { facts: 'Datos clave', pages: 'Páginas clave', apps: 'Aplicaciones', languages: 'Otros idiomas', home: 'Inicio', privacy: 'Política de privacidad', terms: 'Términos del servicio', noApps: 'Todavía no hay aplicaciones publicadas — el catálogo es pequeño a propósito y va creciendo.' },
  de: { facts: 'Kernfakten', pages: 'Wichtige Seiten', apps: 'Apps', languages: 'Weitere Sprachen', home: 'Startseite', privacy: 'Datenschutzerklärung', terms: 'Nutzungsbedingungen', noApps: 'Es sind noch keine Apps veröffentlicht — der Katalog ist bewusst klein und wächst.' },
}

export interface LlmsTxtOptions {
  locale: Locale
  /** URL base assoluta del sito (es. https://appgrove.app), con o senza slash finale. */
  siteUrl: string
  /** Aggiunge la sezione con i link alle altre lingue (usata solo dalla variante radice). */
  includeLanguages?: boolean
  landings?: readonly Landing[]
}

/** Rimuove lo slash finale da una URL base per comporre percorsi assoluti puliti. */
function trimTrailingSlash(url: string): string {
  return url.replace(/\/+$/, '')
}

/** Corpo markdown di /llms.txt per una lingua. */
export function buildLlmsTxt({ locale, siteUrl, includeLanguages = false, landings }: LlmsTxtOptions): string {
  const base = trimTrailingSlash(siteUrl)
  const t = MARKETING[locale]
  const L = LABELS[locale]
  const abs = (p: string) => `${base}${p}`

  // Fatti chiave, localizzati dai contenuti marketing (privacy + ecosistema + AI).
  const facts = [...t.privacy.points, t.crossSell.points[0], t.ai.points[t.ai.points.length - 1]]

  const lines: string[] = [
    `# ${BRAND.name}`,
    '',
    `> ${t.hero.subtitle}`,
    '',
    t.privacy.body,
    '',
    `## ${L.facts}`,
    ...facts.map((f) => `- ${f}`),
    '',
    `## ${L.pages}`,
    `- [${L.home}](${abs(`/${locale}/`)}): ${t.hero.title}`,
    `- [${t.nav.why}](${abs(`/${locale}${brandPath('why', locale)}`)}): ${t.why.intro}`,
    `- [${t.nav.pricing}](${abs(`/${locale}${brandPath('pricing', locale)}`)}): ${t.pricing.intro}`,
    `- [${BLOG_UI[locale].indexTitle}](${abs(`/${locale}/blog/`)}): ${BLOG_UI[locale].indexIntro}`,
    `- [${L.privacy}](${abs(`/${locale}/legal/privacy/`)})`,
    `- [${L.terms}](${abs(`/${locale}/legal/terms/`)})`,
    '',
    `## ${L.apps}`,
  ]

  const published = publishedLandings(landings)
  if (published.length === 0) {
    lines.push(`- ${L.noApps}`)
  } else {
    for (const l of published) {
      const c = l.content[locale]
      lines.push(`- [${c.meta.title}](${abs(`/${locale}/${c.slug}/`)}): ${c.meta.description}`)
    }
  }

  if (includeLanguages) {
    lines.push('', `## ${L.languages}`)
    for (const loc of LOCALES) {
      lines.push(`- [${LOCALE_LABELS[loc]}](${abs(`/${loc}/llms.txt`)})`)
    }
  }

  lines.push('') // newline finale
  return lines.join('\n')
}
