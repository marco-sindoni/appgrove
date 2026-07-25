// Entità canonica / boilerplate del marchio appgrove (UC 0041, GEO — #14 40).
//
// È la descrizione CANONICA del prodotto, pensata per essere usata IDENTICA ovunque:
// nel dato strutturato Organization (JSON-LD, su ogni pagina), nella variante radice di
// /llms.txt e — a mano — nelle directory e nei profili social. "Identica ovunque"
// significa UNA versione autorevole riusata verbatim, non cinque traduzioni che possono
// divergere: perciò il testo canonico è in inglese (lingua franca degli assistenti AI e
// dei canali off-site). Le pagine del sito restano localizzate coi contenuti marketing
// (UC 0037); da quelli le varianti localizzate di llms.txt derivano il loro testo.
//
// L'IDENTITÀ (nome, categoria, dominio) è neutra rispetto alla lingua. Il dominio è lo
// stesso di content/legal/entity.yaml (titolare) — qui è ripetuto come costante di marca
// perché serve prima e senza dipendere dai dati del titolare (ancora "DA COMPILARE").

/** Identità di marca, neutra rispetto alla lingua. */
export const BRAND = {
  name: 'appgrove',
  /** Categoria merceologica canonica (usata negli statement fattuali). */
  category: 'Marketplace of all-EU, GDPR-first micro-SaaS apps for small and medium businesses',
  domain: 'appgrove.app',
  url: 'https://appgrove.app',
  /** Slogan breve. */
  tagline: 'Simple tools that grow with your business.',
  /**
   * Descrizione canonica (inglese): una frase autosufficiente che dice cos'è appgrove,
   * per chi, e col cuneo di fiducia UE/GDPR. Iniettata come `description` dell'Organization.
   */
  description:
    'appgrove is an all-EU, GDPR-first marketplace of focused micro-SaaS apps for small and ' +
    'medium businesses. One account unlocks a growing ecosystem of fast, affordable tools, each ' +
    'doing one job well. All data is hosted in the European Union under European law, with full ' +
    'GDPR rights and no hidden trackers. Every app is designed to be reachable by AI assistants ' +
    'through MCP (Model Context Protocol).',
} as const

export type Brand = typeof BRAND
