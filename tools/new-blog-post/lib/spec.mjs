// ─────────────────────────────────────────────────────────────────────────────
// tools/new-blog-post/lib/spec.mjs — la SPECIFICA di un post e la sua validazione
// preventiva (UC 0084).
//
// La specifica è il contratto fra le due metà della skill: il co-pilota scrive il
// contenuto editoriale nelle 5 lingue in un file JSON, questo strumento lo valida e lo
// materializza. Validare PRIMA di scrivere è il punto: lo use case chiede il "rifiuto
// pulito" — su una specifica difettosa il repository non deve essere toccato affatto.
//
// Attenzione alla divisione delle responsabilità: la validazione AUTOREVOLE del registro
// blog resta in site/src/lib/blog.ts (UC 0042) ed è coperta dai test dell'area `site`.
// Qui si controlla solo ciò che serve per non corrompere il registro e per non produrre
// un post che farebbe fallire quella validazione a valle. Non è un secondo padrone delle
// regole: è il filtro d'ingresso dello scaffolding.
// ─────────────────────────────────────────────────────────────────────────────

/** Le 5 lingue del sito vetrina (#14 31). Inglese = lingua sorgente marketing. */
export const LOCALES = ['en', 'it', 'fr', 'es', 'de']
export const DEFAULT_LOCALE = 'en'

/** Slug riservati DENTRO la sezione blog (specchio di RESERVED_BLOG_SLUGS in lib/blog.ts). */
export const RESERVED_BLOG_SLUGS = ['index']

/** Campi di identità di un post, nell'ordine in cui il generatore li scrive. */
export const POST_FIELDS = ['key', 'kind', 'datePublished', 'appId', 'pillarKey', 'clusterKeys', 'content']

/** Campi del contenuto di UNA lingua, nell'ordine in cui il generatore li scrive. */
export const LOCALE_FIELDS = ['slug', 'title', 'description', 'question', 'intro', 'sections', 'faq', 'ctaText']

const SLUG_RE = /^[a-z0-9]+(?:-[a-z0-9]+)*$/
const DATE_RE = /^\d{4}-\d{2}-\d{2}$/

/**
 * Accetta le tre forme comode di specifica — un singolo post, un array di post, oppure
 * un oggetto `{ posts: [...] }` — e ritorna sempre un array. Più post in una sola
 * esecuzione è il modo in cui si soddisfa il caso "il pilastro non esiste ancora":
 * pilastro e primo articolo nascono insieme, già agganciati.
 */
export function normalizeSpec(raw) {
  if (Array.isArray(raw)) return raw
  if (raw && typeof raw === 'object' && Array.isArray(raw.posts)) return raw.posts
  if (raw && typeof raw === 'object') return [raw]
  return []
}

function isNonEmptyString(v) {
  return typeof v === 'string' && v.trim().length > 0
}

/** Forma di un valore, per confrontare le lingue fra loro (mirror del test di UC 0042). */
function shapeOf(value) {
  if (Array.isArray(value)) return `[${value.length}:${value.map(shapeOf).join(',')}]`
  if (value && typeof value === 'object') {
    return `{${Object.keys(value)
      .sort()
      .map((k) => `${k}:${shapeOf(value[k])}`)
      .join(',')}}`
  }
  return typeof value
}

function validateLocaleContent(errors, where, c) {
  if (!c || typeof c !== 'object' || Array.isArray(c)) {
    errors.push(`${where}: contenuto assente o non è un oggetto`)
    return
  }
  const keys = Object.keys(c).sort()
  const expected = [...LOCALE_FIELDS].sort()
  const missing = expected.filter((k) => !keys.includes(k))
  const extra = keys.filter((k) => !expected.includes(k))
  if (missing.length) errors.push(`${where}: campi mancanti — ${missing.join(', ')}`)
  if (extra.length) errors.push(`${where}: campi non previsti — ${extra.join(', ')}`)

  if (!isNonEmptyString(c.slug)) {
    errors.push(`${where}: slug mancante o vuoto`)
  } else if (!SLUG_RE.test(c.slug)) {
    errors.push(`${where}: slug "${c.slug}" non valido (minuscolo, cifre e trattini singoli: [a-z0-9-])`)
  } else if (RESERVED_BLOG_SLUGS.includes(c.slug)) {
    errors.push(`${where}: slug "${c.slug}" riservato (collide con l'indice del blog)`)
  }

  for (const f of ['title', 'description', 'question', 'ctaText']) {
    if (!isNonEmptyString(c[f])) errors.push(`${where}: ${f} mancante o vuoto`)
  }

  if (!Array.isArray(c.intro) || c.intro.length === 0) {
    errors.push(`${where}: intro deve essere una lista non vuota di paragrafi`)
  } else if (!c.intro.every(isNonEmptyString)) {
    errors.push(`${where}: intro contiene paragrafi vuoti`)
  }

  if (!Array.isArray(c.sections) || c.sections.length === 0) {
    errors.push(`${where}: sections deve essere una lista non vuota`)
  } else {
    c.sections.forEach((s, i) => {
      if (!s || typeof s !== 'object') {
        errors.push(`${where}: sections[${i}] non è un oggetto`)
        return
      }
      if (!isNonEmptyString(s.heading)) errors.push(`${where}: sections[${i}].heading mancante o vuoto`)
      if (!Array.isArray(s.paragraphs) || s.paragraphs.length === 0) {
        errors.push(`${where}: sections[${i}].paragraphs deve essere una lista non vuota`)
      } else if (!s.paragraphs.every(isNonEmptyString)) {
        errors.push(`${where}: sections[${i}].paragraphs contiene paragrafi vuoti`)
      }
    })
  }

  if (!c.faq || typeof c.faq !== 'object') {
    errors.push(`${where}: faq mancante`)
  } else {
    if (!isNonEmptyString(c.faq.title)) errors.push(`${where}: faq.title mancante o vuoto`)
    if (!Array.isArray(c.faq.items) || c.faq.items.length === 0) {
      errors.push(`${where}: faq.items deve essere una lista non vuota`)
    } else {
      c.faq.items.forEach((it, i) => {
        if (!it || typeof it !== 'object' || !isNonEmptyString(it.q) || !isNonEmptyString(it.a)) {
          errors.push(`${where}: faq.items[${i}] deve avere domanda (q) e risposta (a) non vuote`)
        }
      })
    }
  }
}

/**
 * Valida la FORMA della specifica, senza guardare il registro reale: identità, contenuto
 * completo nelle 5 lingue, nessuna stringa vuota, e stessa forma di ogni traduzione
 * rispetto alla sorgente inglese (numero di paragrafi, sezioni e voci FAQ). Quest'ultimo
 * controllo esiste perché è esattamente ciò che il test dei contenuti di UC 0042 pretende:
 * meglio rifiutare qui che far diventare rossa la suite del sito dopo la scrittura.
 *
 * Ritorna la lista degli errori (vuota = specifica ben formata).
 */
export function validateSpecShape(posts) {
  const errors = []
  if (!Array.isArray(posts) || posts.length === 0) {
    return ['specifica vuota: attesi uno o più post (oggetto, array, o { "posts": [...] })']
  }

  const seenKeys = new Set()
  const seenSlugs = new Map() // `${lang}:${slug}` → key

  for (const [i, p] of posts.entries()) {
    const label = isNonEmptyString(p?.key) ? p.key : `post[${i}]`

    if (!p || typeof p !== 'object' || Array.isArray(p)) {
      errors.push(`${label}: non è un oggetto`)
      continue
    }
    const extra = Object.keys(p).filter((k) => !POST_FIELDS.includes(k))
    if (extra.length) errors.push(`${label}: campi non previsti — ${extra.join(', ')}`)

    if (!isNonEmptyString(p.key)) errors.push(`${label}: key mancante o vuota`)
    else if (!SLUG_RE.test(p.key)) errors.push(`${label}: key "${p.key}" non valida (minuscolo, cifre e trattini)`)
    else if (seenKeys.has(p.key)) errors.push(`${label}: key duplicata nella stessa specifica`)
    else seenKeys.add(p.key)

    if (p.kind !== 'pillar' && p.kind !== 'article') {
      errors.push(`${label}: kind deve essere "pillar" (pilastro) o "article" (articolo cluster)`)
    }

    if (!isNonEmptyString(p.datePublished) || !DATE_RE.test(p.datePublished)) {
      errors.push(`${label}: datePublished mancante o non nel formato AAAA-MM-GG`)
    } else if (Number.isNaN(Date.parse(p.datePublished))) {
      errors.push(`${label}: datePublished "${p.datePublished}" non è una data reale`)
    }

    if (!isNonEmptyString(p.appId) || !SLUG_RE.test(p.appId)) {
      errors.push(`${label}: appId mancante o non valido (identificativo dell'app a cui rimanda il post)`)
    }

    if (p.kind === 'article') {
      if (!isNonEmptyString(p.pillarKey)) errors.push(`${label}: articolo senza pillarKey (a quale pilastro appartiene?)`)
      else if (p.pillarKey === p.key) errors.push(`${label}: un articolo non può essere pilastro di se stesso`)
    } else if (p.kind === 'pillar' && p.pillarKey !== undefined) {
      errors.push(`${label}: un pilastro non ha pillarKey`)
    }

    if (p.clusterKeys !== undefined) {
      errors.push(
        `${label}: clusterKeys non si dichiara nella specifica — è il generatore ad agganciare i riferimenti reciproci`,
      )
    }

    if (!p.content || typeof p.content !== 'object' || Array.isArray(p.content)) {
      errors.push(`${label}: content mancante (atteso un oggetto con le 5 lingue)`)
      continue
    }
    const langs = Object.keys(p.content).sort()
    const missingLangs = LOCALES.filter((l) => !langs.includes(l))
    const extraLangs = langs.filter((l) => !LOCALES.includes(l))
    if (missingLangs.length) errors.push(`${label}: lingue mancanti — ${missingLangs.join(', ')}`)
    if (extraLangs.length) errors.push(`${label}: lingue non previste — ${extraLangs.join(', ')}`)

    for (const lang of LOCALES) {
      if (!p.content[lang]) continue
      validateLocaleContent(errors, `${label}/${lang}`, p.content[lang])
      const slug = p.content[lang].slug
      if (isNonEmptyString(slug)) {
        const mapKey = `${lang}:${slug}`
        const prev = seenSlugs.get(mapKey)
        if (prev && prev !== p.key) {
          errors.push(`slug "${slug}" in lingua ${lang} usato due volte nella stessa specifica (${prev} e ${label})`)
        }
        seenSlugs.set(mapKey, p.key)
      }
    }

    // Parità di forma fra le traduzioni e la sorgente inglese.
    const source = p.content[DEFAULT_LOCALE]
    if (source) {
      const reference = shapeOf(source)
      for (const lang of LOCALES) {
        if (lang === DEFAULT_LOCALE || !p.content[lang]) continue
        if (shapeOf(p.content[lang]) !== reference) {
          errors.push(
            `${label}/${lang}: forma diversa dalla sorgente ${DEFAULT_LOCALE} ` +
              `(numero di paragrafi, sezioni o voci FAQ non corrispondente)`,
          )
        }
      }
    }
  }

  return errors
}
