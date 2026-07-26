// Forma unica dei contenuti del blog/risorse del sito vetrina (UC 0042).
//
// Il blog è il motore SEO/GEO che cresce nel tempo, secondo il modello PILASTRO +
// CLUSTER: una pagina pilastro per tema, più articoli long-tail (how-to/confronto)
// che vi rimandano (internal linking). Come per i contenuti marketing (UC 0037) e le
// landing (UC 0038), i contenuti sono STRUTTURATI e tipizzati, non markdown liberi: il
// tipo `Record<Locale, PostLocaleContent>` forza a COMPILE-TIME la parità delle 5 lingue.
//
// Contenuti QUESTION-BASED (GEO, UC 0041): ogni post ha una domanda-guida (`question`)
// formulata come si interroga un assistente AI, così pagina e dato strutturato rispondono
// nel formato che gli assistenti citano. Il registro è disegnato perché aggiungere un
// articolo sia "una cartella + una entry nell'array": lo scaffolding meccanico sarà la
// futura skill `new-blog-post` (docs/_BACKLOG.md), con questa validazione a fare da rete.

import type { Locale } from '../../lib/i18n.ts'

/** Un post è un PILASTRO (testa di serie per tema) o un ARTICOLO cluster. */
export type PostKind = 'pillar' | 'article'

/** Sezione di corpo: sottotitolo + paragrafi. */
export interface PostSection {
  heading: string
  paragraphs: string[]
}

/** Voce FAQ question-based (resa in pagina + FAQPage JSON-LD, GEO UC 0041). */
export interface PostFaqItem {
  q: string
  a: string
}

/** Contenuto di un post in UNA lingua: slug localizzato + meta + corpo + FAQ. */
export interface PostLocaleContent {
  /** Slug localizzato (#14 31), usato in /<lang>/blog/<slug>/. Minuscolo, [a-z0-9-]. */
  slug: string
  /** <title> senza il suffisso "· appgrove" (aggiunto dal layout). */
  title: string
  /** meta description + og:description + occhiello nell'indice blog. */
  description: string
  /** Domanda-guida question-based (H1 della pagina): formulata come si chiede a un'AI. */
  question: string
  /** Introduzione (uno o più paragrafi). */
  intro: string[]
  /** Corpo strutturato: sezioni con sottotitolo e paragrafi. */
  sections: PostSection[]
  /** FAQ question-based della pagina (resa + FAQPage JSON-LD). */
  faq: { title: string; items: PostFaqItem[] }
  /** Testo del link interno (call to action) verso la landing dell'app collegata. */
  ctaText: string
}

/**
 * Un post del blog: identità e relazioni a livello POST (non per-lingua, così non
 * possono divergere) più il contenuto nelle 5 lingue. Il tipo Record<Locale, …>
 * garantisce la parità delle lingue a compile-time.
 */
export interface BlogPost {
  /** Chiave stabile NON tradotta: nome cartella e ancora dei riferimenti pilastro↔cluster. */
  key: string
  kind: PostKind
  /** Data di pubblicazione ISO `AAAA-MM-GG` (datePublished dello Schema.org Article). */
  datePublished: string
  /** appId della landing a cui il post rimanda (internal linking risolto per lingua). */
  appId: string
  /** Solo ARTICOLO: chiave del pilastro di cui fa parte (riferimento inverso di clusterKeys). */
  pillarKey?: string
  /** Solo PILASTRO: chiavi degli articoli cluster che vi rimandano. */
  clusterKeys?: string[]
  content: Record<Locale, PostLocaleContent>
}
