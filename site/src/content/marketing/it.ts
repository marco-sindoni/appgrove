// Contenuti marketing — italiano (UC 0037). Tono lean, job-led + privacy come firma.
import type { MarketingContent } from './types.ts'

export const it: MarketingContent = {
  nav: {
    app: 'App',
    why: 'Perché appgrove',
    pricing: 'Prezzi',
    login: 'Accedi',
    signup: 'Registrati',
  },
  hero: {
    badge: 'tutto in Europa · GDPR-first',
    title: 'Strumenti semplici che crescono con la tua attività',
    subtitle:
      "Un ecosistema di app veloci e a costi sostenibili per piccole e medie imprese. Meno tempo sulla gestione, più tempo per il tuo lavoro.",
    ctaPrimary: 'Inizia ora',
    ctaSecondary: 'Scopri le app',
  },
  apps: {
    title: 'Un account, tutti gli strumenti che ti servono',
    subtitle: 'App focalizzate che fanno bene una cosa sola — niente suite gonfie, niente lock-in.',
    comingSoon: 'In arrivo',
    flagshipName: 'Fatturazione',
    flagshipCategory: 'Fatture e pagamenti',
    flagshipDescription:
      'Crea e invia fatture a norma in pochi minuti, tieni traccia dei pagamenti e resta in ordine con i conti.',
    moreTitle: 'Altri strumenti in arrivo',
    moreText:
      "Il catalogo è piccolo di proposito, e cresce. Nuove app entrano nell'ecosistema con regolarità — e un solo account le sblocca tutte.",
  },
  crossSell: {
    title: 'Un account, tanti strumenti',
    body: "Man mano che l'ecosistema cresce, il tuo account cresce con lui. Aggiungi un nuovo strumento quando ti serve — stesso accesso, stessa casa affidabile per i tuoi dati.",
    points: [
      'Un unico accesso per tutte le app',
      "Un'esperienza semplice e coerente tra gli strumenti",
      'Aggiungi strumenti mentre la tua attività cresce',
    ],
  },
  ai: {
    title: 'Pronte per l’AI',
    body: 'Ogni app di appgrove è progettata per essere richiamata dal tuo assistente AI. Chiedi in linguaggio naturale su ChatGPT, Claude o Perplexity — e lascia che porti a termine il lavoro dentro la tua app.',
    points: [
      'Basate su MCP (Model Context Protocol), lo standard aperto con cui gli assistenti AI richiamano strumenti esterni',
      'Fai il lavoro direttamente dalla chat che già usi',
      'I tuoi strumenti, raggiungibili dove già lavori',
    ],
    note: 'È così che progettiamo l’ecosistema — ci stiamo arrivando oggi, un’app alla volta.',
  },
  privacy: {
    title: 'Ospitato in Europa. Pieni diritti GDPR.',
    body: 'I tuoi dati vivono in Europa, sotto la legge europea. Qui la privacy non è un extra: è il modo in cui appgrove è costruito.',
    points: [
      'Tutti i dati ospitati in Europa',
      'Pieni diritti GDPR, per progettazione',
      'Nessun tracciamento nascosto, nessun dato venduto',
    ],
  },
  faq: {
    title: 'Domande frequenti',
    items: [
      {
        q: 'Cos’è appgrove?',
        a: 'appgrove è un marketplace tutto europeo e GDPR-first di micro-app focalizzate per piccole e medie imprese. Un solo account sblocca un ecosistema in crescita di strumenti semplici e a costi sostenibili, ognuno dei quali fa bene una cosa sola.',
      },
      {
        q: 'Dove sono conservati i miei dati?',
        a: 'Tutti i dati di appgrove sono ospitati nell’Unione Europea, sotto la legge europea. Non ci sono tracciatori nascosti e i tuoi dati non vengono mai venduti.',
      },
      {
        q: 'Ho pieni diritti GDPR?',
        a: 'Sì. I diritti GDPR sono integrati per progettazione — puoi accedere, esportare e cancellare i tuoi dati. La privacy è la base, non un extra.',
      },
      {
        q: 'Come funziona il prezzo?',
        a: 'Ogni app fissa il proprio prezzo sulla sua pagina. Paghi solo gli strumenti che usi, mensile o annuale, e ogni piano a pagamento inizia con 14 giorni di prova gratuita.',
      },
      {
        q: 'Posso usare le app di appgrove con un assistente AI?',
        a: 'È così che progettiamo l’ecosistema: ogni app è pensata per essere richiamata dagli assistenti AI tramite MCP (Model Context Protocol). Ci stiamo arrivando oggi, un’app alla volta.',
      },
    ],
  },
  newsletter: {
    title: 'Resta aggiornato',
    body: "Ti avvisiamo quando arrivano nuove app. Niente spam, disiscrizione quando vuoi.",
    placeholder: 'tu@email.com',
    cta: 'Avvisami',
    note: 'Ti scriveremo solo di appgrove. Le iscrizioni aprono presto.',
  },
  finalCta: {
    title: 'Pronto a iniziare?',
    body: "Crea il tuo account ed esplora l'ecosistema — provalo gratis.",
    primary: 'Inizia ora',
    secondary: 'Perché appgrove',
  },
  why: {
    title: 'Perché appgrove',
    intro:
      'Costruiamo strumenti semplici e focalizzati per le piccole e medie imprese in Europa. Quattro promesse guidano tutto ciò che facciamo.',
    sections: [
      {
        title: 'Strumenti che ti restituiscono tempo',
        body: 'Le piccole imprese non hanno bisogno di suite gonfie. Hanno bisogno di strumenti che fanno bene una cosa, in fretta, a un prezzo giusto. Ogni app di appgrove è pensata per toglierti la gestione di dosso, così ti concentri sul tuo lavoro.',
      },
      {
        title: 'Un ecosistema in crescita',
        body: "Un account, tanti strumenti. Quando le tue esigenze crescono, l'ecosistema cresce con te — aggiungi una nuova app quando ti serve, senza nuovi accessi né nuovi silos.",
      },
      {
        title: 'Pronte per l’AI',
        body: 'Progettiamo ogni app per essere richiamata dal tuo assistente AI, tramite MCP — lo standard aperto con cui gli assistenti AI richiamano strumenti esterni. La visione: chiedi in linguaggio naturale e il lavoro viene fatto dentro la tua app. Ci stiamo arrivando oggi, un’app alla volta.',
      },
      {
        title: 'Europea per scelta, privata per impostazione',
        body: 'I tuoi dati sono ospitati in Europa, sotto la legge europea, con pieni diritti GDPR. La privacy è la base, non una funzione — nessun tracciamento nascosto, e i tuoi dati non vengono mai venduti.',
      },
    ],
    faq: {
      title: 'Altro su appgrove',
      items: [
        {
          q: 'Cosa distingue appgrove da una grande suite software?',
          a: 'appgrove è un ecosistema di piccole app focalizzate, non un’unica suite gonfia. Ogni strumento fa bene una cosa e un solo account te li dà tutti — niente lock-in, niente silos.',
        },
        {
          q: 'Serve un account separato per ogni app?',
          a: 'No. Un solo account sblocca ogni app dell’ecosistema, con un unico accesso e una sola casa affidabile per i tuoi dati.',
        },
        {
          q: 'Cosa significa "Pronte per l’AI"?',
          a: 'Ogni app è progettata per essere richiamata dal tuo assistente AI tramite MCP, lo standard aperto con cui gli assistenti raggiungono strumenti esterni. È la nostra direzione di progetto, costruita un’app alla volta.',
        },
        {
          q: 'appgrove è davvero tutto in Europa?',
          a: 'Sì. I dati sono ospitati in Europa sotto la legge europea, con pieni diritti GDPR, nessun tracciatore nascosto e nessuna vendita di dati — il cuneo di fiducia dietro tutto ciò che costruiamo.',
        },
      ],
    },
  },
  pricing: {
    title: 'Prezzi — come funziona la fatturazione',
    intro:
      "Ogni app fissa il proprio prezzo sulla sua pagina. Ecco come funziona la fatturazione in appgrove.",
    sections: [
      {
        title: 'Prezzo per singola app',
        body: 'Paghi solo gli strumenti che usi. Ogni app mostra piani e prezzi sulla propria pagina — scegli ciò che serve alla tua attività.',
      },
      {
        title: 'Mensile o annuale',
        body: "Scegli il ciclo di fatturazione che preferisci. L'annuale è il predefinito e costa meno sull'anno; il mensile ti dà flessibilità.",
      },
      {
        title: 'Prova gratuita di 14 giorni',
        body: 'Provi prima di pagare. Ogni piano a pagamento inizia con 14 giorni di prova gratuita — nulla viene addebitato finché non termina.',
      },
      {
        title: 'Disdici quando vuoi',
        body: 'Ogni piano inizia con una prova gratuita, così valuti senza rischi prima di pagare — per questo gli importi già addebitati non vengono restituiti. In ogni caso puoi disdire l’abbonamento quando vuoi: dal ciclo di fatturazione successivo non paghi più nulla e il periodo in corso resta attivo fino alla scadenza. I dettagli sono nella nostra Refund Policy.',
      },
    ],
    refundLinkText: 'Leggi la Refund Policy',
    faq: {
      title: 'Domande sulla fatturazione',
      items: [
        {
          q: 'Quanto costa appgrove?',
          a: 'Non c’è un prezzo unico: ogni app mostra i propri piani e prezzi sulla sua pagina. Paghi solo gli strumenti che usi.',
        },
        {
          q: 'Mensile o annuale?',
          a: 'Entrambi. L’annuale è il predefinito e costa meno sull’anno; il mensile dà più flessibilità. Scegli per ogni app.',
        },
        {
          q: 'C’è una prova gratuita?',
          a: 'Sì. Ogni piano a pagamento inizia con 14 giorni di prova gratuita — nulla viene addebitato finché non termina.',
        },
        {
          q: 'Posso avere un rimborso?',
          a: 'Ogni piano inizia con una prova gratuita, così provi senza rischi prima di pagare — per questo gli importi già addebitati non vengono restituiti. Puoi però disdire l’abbonamento quando vuoi: dal ciclo di fatturazione successivo non ti viene addebitato più nulla e il periodo in corso resta attivo fino alla scadenza. I dettagli sono nella Refund Policy.',
        },
      ],
    },
  },
  footer: {
    tagline: 'Strumenti semplici che crescono con la tua attività.',
    legalHeading: 'Legale',
    supportLabel: 'Assistenza',
    securityLabel: 'Sicurezza',
    newsletterTitle: 'Newsletter',
    newsletterBody: 'Le nuove app, dritte nella tua casella.',
    newsletterPlaceholder: 'tu@email.com',
    newsletterCta: 'Avvisami',
    rights: 'appgrove — tutto in Europa, GDPR-first.',
  },
}
