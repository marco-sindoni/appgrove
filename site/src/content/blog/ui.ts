// Stringhe di contorno del blog/risorse (UC 0042): titolo/occhiello dell'indice e
// poche etichette della cornice delle pagine post (serie pilastro↔cluster, breadcrumb).
// Tenute qui, localizzate nelle 5 lingue, per non spargere i18n nelle rotte. La parità
// di forma è garantita dal tipo Record<Locale, …>; i valori sono coperti dal test.

import type { Locale } from '../../lib/i18n.ts'

export interface BlogUi {
  /** Titolo dell'indice blog (/<lang>/blog/). */
  indexTitle: string
  /** Occhiello dell'indice blog. */
  indexIntro: string
  /** Intestazione della lista degli articoli cluster nella pagina pilastro. */
  seriesHeading: string
  /** Prefisso del richiamo "fa parte di …" dall'articolo verso il suo pilastro. */
  partOf: string
  /** Etichetta breadcrumb della sezione blog (riusa comunque nav.blog dove disponibile). */
  breadcrumb: string
}

export const BLOG_UI: Record<Locale, BlogUi> = {
  en: {
    indexTitle: 'Blog & resources',
    indexIntro:
      'Practical guides for running a small business in the EU — invoicing, compliance and the tools that give you time back. Written to answer the questions you would ask an assistant.',
    seriesHeading: 'In this series',
    partOf: 'Part of',
    breadcrumb: 'Blog',
  },
  it: {
    indexTitle: 'Blog e risorse',
    indexIntro:
      'Guide pratiche per gestire una piccola impresa in UE — fatturazione, conformità e gli strumenti che ti restituiscono tempo. Scritte per rispondere alle domande che faresti a un assistente.',
    seriesHeading: 'In questa serie',
    partOf: 'Fa parte di',
    breadcrumb: 'Blog',
  },
  fr: {
    indexTitle: 'Blog et ressources',
    indexIntro:
      "Des guides pratiques pour gérer une petite entreprise dans l'UE — facturation, conformité et outils qui vous font gagner du temps. Écrits pour répondre aux questions que vous poseriez à un assistant.",
    seriesHeading: 'Dans cette série',
    partOf: 'Fait partie de',
    breadcrumb: 'Blog',
  },
  es: {
    indexTitle: 'Blog y recursos',
    indexIntro:
      'Guías prácticas para gestionar una pequeña empresa en la UE — facturación, cumplimiento y las herramientas que te devuelven tiempo. Escritas para responder a las preguntas que le harías a un asistente.',
    seriesHeading: 'En esta serie',
    partOf: 'Forma parte de',
    breadcrumb: 'Blog',
  },
  de: {
    indexTitle: 'Blog & Ressourcen',
    indexIntro:
      'Praktische Leitfäden für die Führung eines Kleinunternehmens in der EU — Rechnungsstellung, Compliance und die Werkzeuge, die Ihnen Zeit zurückgeben. Geschrieben als Antwort auf die Fragen, die Sie einem Assistenten stellen würden.',
    seriesHeading: 'In dieser Reihe',
    partOf: 'Teil von',
    breadcrumb: 'Blog',
  },
}
