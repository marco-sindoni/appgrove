// Costruttore del corpo di robots.txt (UC 0040 gate indicizzazione + UC 0041 GEO).
//
// Logica pura e testabile (l'endpoint src/pages/robots.txt.ts è solo un guscio, come
// per sitemap.ts). Due stati, guidati dallo stesso interruttore SITE_INDEXABLE:
//   • pre-go-live (non indicizzabile): Disallow totale per TUTTI, coerente col meta
//     noindex — i crawler AI vedranno il sito solo al lancio (#14 54, flusso alt. UC 0041);
//   • go-live (indicizzabile): Allow generale PIÙ blocchi espliciti che DANNO IL CONSENSO
//     al più ampio insieme possibile di crawler di assistenti AI/LLM (scopo della GEO,
//     #14 39), la riga Sitemap e un rimando a /llms.txt.
//
// Perché elencare i crawler se il wildcard `Allow: /` già li ammette? Per rendere il
// consenso VERIFICABILE e testabile: è la dichiarazione d'intento della GEO. La postura
// resta purista — viene servito solo contenuto marketing pubblico, nessun dato utente.

/**
 * Crawler di assistenti AI/LLM a cui diamo consenso esplicito. Sono user-agent REALI e
 * documentati dai rispettivi operatori (niente nomi inventati); l'elenco è volutamente
 * ampio. Chi non è elencato resta comunque ammesso dal blocco wildcard `User-agent: *`.
 */
export const AI_CRAWLERS: readonly string[] = [
  // OpenAI
  'GPTBot',
  'ChatGPT-User',
  'OAI-SearchBot',
  // Anthropic (Claude)
  'ClaudeBot',
  'anthropic-ai',
  'Claude-Web',
  'Claude-User',
  'Claude-SearchBot',
  // Google (Gemini / Vertex — opt-in separato dalla ricerca)
  'Google-Extended',
  // Apple (Apple Intelligence)
  'Applebot-Extended',
  // Perplexity
  'PerplexityBot',
  'Perplexity-User',
  // Common Crawl (dataset usato da molti LLM)
  'CCBot',
  // Meta (Llama)
  'Meta-ExternalAgent',
  'Meta-ExternalFetcher',
  'FacebookBot',
  // Amazon
  'Amazonbot',
  // ByteDance
  'Bytespider',
  'TikTokSpider',
  // Cohere
  'cohere-ai',
  'cohere-training-data-crawler',
  // Mistral
  'MistralAI-User',
  // You.com
  'YouBot',
  // Huawei (Petal / assistente)
  'PetalBot',
  // DuckDuckGo (DuckAssist)
  'DuckAssistBot',
  // Diffbot
  'Diffbot',
  // Timpi
  'Timpibot',
  // Webz.io / Omgili
  'Omgilibot',
  'Omgili',
  // Allen Institute for AI
  'AI2Bot',
]

export interface RobotsOptions {
  indexable: boolean
  /** URL assoluta della sitemap (emessa solo al go-live). */
  sitemapUrl: string
  /** URL assoluta di /llms.txt (rimando in commento, solo al go-live). */
  llmsTxtUrl: string
}

/** Corpo di robots.txt secondo il gate di indicizzazione e il consenso ai crawler AI. */
export function buildRobotsTxt({ indexable, sitemapUrl, llmsTxtUrl }: RobotsOptions): string {
  if (!indexable) {
    // Pre-go-live: nessuno indicizza, nemmeno i crawler AI (coerente col meta noindex).
    return 'User-agent: *\nDisallow: /\n'
  }

  const blocks: string[] = [
    `# Guida per gli assistenti AI (GEO, UC 0041): ${llmsTxtUrl}`,
    '# Solo contenuto marketing pubblico — nessun dato utente.',
    '',
    'User-agent: *',
    'Allow: /',
  ]
  // Consenso esplicito, un blocco per crawler AI (dichiarazione d'intento verificabile).
  for (const ua of AI_CRAWLERS) {
    blocks.push('', `User-agent: ${ua}`, 'Allow: /')
  }
  blocks.push('', `Sitemap: ${sitemapUrl}`, '')
  return blocks.join('\n')
}
