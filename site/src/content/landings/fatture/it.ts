// Landing dell'app #1 "Fatture" (UC 0053) — italiano.
import type { LandingLocaleContent } from '../types.ts'

export const it: LandingLocaleContent = {
  slug: 'fatture',
  meta: {
    title: 'Fatture — la fatturazione senza burocrazia, ospitata in UE',
    description:
      'Crea, invia e tieni traccia delle fatture in pochi clic. Un’app di fatturazione essenziale e mono-utente, con i tuoi dati ospitati in UE e pieni diritti GDPR.',
    ogImage: '/landings/fatture/og.png',
  },
  hero: {
    badge: 'tutto in UE · GDPR-first',
    title: 'La fatturazione, senza la burocrazia',
    subtitle:
      'Con Fatture crei, invii e tieni traccia delle fatture in pochi clic — un’app essenziale che fa bene una cosa sola, con i tuoi dati ospitati in UE.',
    ctaPrimary: 'Inizia gratis',
    ctaSecondary: 'Scopri come funziona',
    screenshot: {
      src: '/landings/fatture/hero.it.png',
      alt: 'La lista fatture di Fatture, con numero, cliente, stato e totale',
    },
  },
  problemSolution: {
    title: 'Meno tempo sulle scartoffie',
    problem:
      'Per liberi professionisti e piccole attività, fatturare vuol dire modelli scomodi, numerazione a mano e fogli di calcolo che non tornano mai — tempo sottratto al lavoro vero.',
    solution:
      'Fatture lo riduce a pochi clic: aggiungi un cliente, elenca le righe, e il totale e il numero progressivo li calcola per te. Niente da installare, nessun ingombro.',
  },
  features: {
    title: 'Tutto ciò che serve per fatturare, niente di superfluo',
    subtitle: 'Uno strumento essenziale che fa bene un lavoro — nessuna suite gonfia, nessun vincolo.',
    items: [
      {
        icon: 'receipt_long',
        title: 'Fatture in pochi clic',
        body: 'Aggiungi un cliente e le righe — descrizione, quantità, importo unitario — e Fatture calcola il totale per te.',
      },
      {
        icon: 'tag',
        title: 'Numerazione automatica',
        body: 'Un numero progressivo per anno, assegnato al posto tuo e mai riutilizzato — ordinato e coerente.',
      },
      {
        icon: 'fact_check',
        title: 'Stato sempre chiaro',
        body: 'Porti ogni fattura da bozza a emessa, pagata o annullata, così sai sempre a che punto sei.',
      },
      {
        icon: 'lock',
        title: 'Ospitata in UE',
        body: 'Le tue fatture vivono in UE, sotto legge europea, con pieni diritti GDPR e nessun tracciatore nascosto.',
      },
    ],
  },
  howItWorks: {
    title: 'Operativo in tre passi',
    steps: [
      { title: 'Crea il tuo account', body: 'Registrazione in pochi secondi — nessuna carta, il piano gratuito è pronto all’uso.' },
      { title: 'Aggiungi la prima fattura', body: 'Inserisci il cliente e le righe; il totale e il numero della fattura vengono compilati per te.' },
      { title: 'Seguila fino a pagata', body: 'Emetti la fattura e aggiorna lo stato mentre passa da bozza a pagata.' },
    ],
  },
  pricing: {
    title: 'Un solo piano gratuito, nessuna sorpresa',
    subtitle: 'Fatture è gratis. Crei fino a 10 fatture al mese — nessuna carta, nessuna prova che scade.',
    monthlyLabel: 'Mensile',
    yearlyLabel: 'Annuale',
    trialNote: 'Nessuna carta, mai. Il piano gratuito resta tuo.',
    tiers: [
      {
        name: 'Gratis',
        priceMonthly: '0 €',
        priceYearly: '0 €',
        features: [
          'Fino a 10 fatture al mese',
          'Clienti e righe fattura',
          'Stato e numerazione automatica',
          'Dati ospitati in UE, pieni diritti GDPR',
        ],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'Ospitata in UE. Pieni diritti GDPR.',
    body: 'Le tue fatture restano in Europa, sotto legge europea. Puoi esportare o cancellare i tuoi dati da solo, quando vuoi — e non vengono mai venduti.',
    points: [
      'Tutti i dati ospitati in UE',
      'Esporti o cancelli i tuoi dati da solo (GDPR)',
      'Fatture conservate in linea con gli obblighi fiscali',
      'Nessun tracciatore nascosto, nessun dato venduto',
    ],
  },
  faq: {
    title: 'Domande frequenti',
    items: [
      {
        q: 'Quanto costa Fatture?',
        a: 'È gratis. Il piano attuale ti permette di creare fino a 10 fatture al mese, senza carta e senza prove che scadono.',
      },
      {
        q: 'Cosa succede quando raggiungo 10 fatture in un mese?',
        a: 'Il conteggio mensile si azzera all’inizio di ogni mese di calendario, quindi puoi creare nuove fatture dal mese successivo.',
      },
      {
        q: 'Dove sono conservati i miei dati?',
        a: 'Interamente nell’Unione Europea, sotto legge europea, con pieni diritti GDPR.',
      },
      {
        q: 'Posso esportare o cancellare i miei dati?',
        a: 'Sì. Puoi esportare o cancellare definitivamente i tuoi dati da solo, dal tuo account, in qualsiasi momento.',
      },
    ],
  },
  finalCta: {
    title: 'Pronto a inviare la tua prima fattura?',
    body: 'Crea il tuo account e inizia a fatturare gratis — nessuna carta richiesta.',
    primary: 'Inizia gratis',
    secondary: 'Perché appgrove',
  },
}
