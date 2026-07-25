// Fixture di esempio (UC 0038) — italiano.
import type { LandingLocaleContent } from '../types.ts'

export const it: LandingLocaleContent = {
  slug: 'esempio',
  meta: {
    title: 'Esempio — il template di landing di appgrove',
    description:
      'Dimostrazione del template ripetibile di landing per-app: otto sezioni on-brand, cinque lingue, stati bozza e pubblicata.',
    ogImage: null,
  },
  hero: {
    badge: 'tutto in EU · GDPR-first',
    title: 'Porti a termine il lavoro in minuti, non in pomeriggi',
    subtitle:
      'Esempio è l’app dimostrativa del template di landing di appgrove. Mostra come ogni app racconta la propria storia — prima il lavoro, la privacy come firma di fiducia.',
    ctaPrimary: 'Inizia la prova gratuita',
    ctaSecondary: 'Scopri come funziona',
    screenshot: {
      src: null,
      alt: 'Schermata della dashboard dell’app Esempio',
    },
  },
  problemSolution: {
    title: 'Meno burocrazia, più di ciò che conta',
    problem:
      'I piccoli team perdono ore in attività ripetitive e noiose — sparse su strumenti che non si parlano mai davvero.',
    solution:
      'Esempio fa bene una cosa sola: ti toglie quella burocrazia di dosso, in pochi clic, con i tuoi dati al sicuro in Europa.',
  },
  features: {
    title: 'Tutto ciò che ti serve, niente di superfluo',
    subtitle: 'Funzioni essenziali che fanno bene un lavoro — nessuna suite gonfia, nessun vincolo.',
    items: [
      {
        icon: 'bolt',
        title: 'Veloce per natura',
        body: 'Configurazione in pochi minuti e attività quotidiane in un paio di clic.',
      },
      {
        icon: 'lock',
        title: 'Privata per costruzione',
        body: 'I tuoi dati vivono in EU, sotto legge europea, con pieni diritti GDPR.',
      },
      {
        icon: 'sync',
        title: 'Un account, tutti gli strumenti',
        body: 'Aggiungi altre app appgrove quando ti servono — stesso accesso, stessa casa affidabile.',
      },
      {
        icon: 'smart_toy',
        title: 'Pronta per l’AI',
        body: 'Pensata per essere raggiungibile dal tuo assistente AI, così il lavoro si fa dalla chat che già usi.',
      },
    ],
  },
  howItWorks: {
    title: 'Operativo in tre passi',
    steps: [
      { title: 'Crea il tuo account', body: 'Registrazione in pochi secondi — nessuna carta per iniziare la prova.' },
      { title: 'Prepara lo spazio di lavoro', body: 'Una configurazione guidata ti rende produttivo dal primo giorno.' },
      { title: 'Porta a termine il lavoro', body: 'Fai il tuo lavoro e lascia che Esempio tenga la burocrazia lontana.' },
    ],
  },
  pricing: {
    title: 'Prezzi semplici e onesti',
    subtitle: 'Scegli il piano giusto per te. L’annuale costa meno; il mensile dà flessibilità.',
    monthlyLabel: 'Mensile',
    yearlyLabel: 'Annuale',
    trialNote: 'Ogni piano a pagamento inizia con 14 giorni di prova gratuita — nessun addebito fino alla fine.',
    tiers: [
      {
        name: 'Starter',
        priceMonthly: '0 €',
        priceYearly: '0 €',
        features: ['Per iniziare', 'Funzioni di base', 'Supporto della community'],
      },
      {
        name: 'Pro',
        priceMonthly: '00 € / mese',
        priceYearly: '000 € / anno',
        features: ['Tutto di Starter', 'Tutte le funzioni', 'Supporto prioritario'],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'Ospitata in EU. Pieni diritti GDPR.',
    body: 'Qui la privacy non è un extra — è come è costruita appgrove. I tuoi dati restano in Europa e non vengono mai venduti.',
    points: ['Tutti i dati ospitati in EU', 'Pieni diritti GDPR, per progettazione', 'Nessun tracciatore nascosto, nessun dato venduto'],
  },
  faq: {
    title: 'Domande frequenti',
    items: [
      {
        q: 'Serve una carta per provare Esempio?',
        a: 'No. La prova gratuita di 14 giorni parte senza carta; paghi solo se decidi di continuare.',
      },
      {
        q: 'Dove sono conservati i miei dati?',
        a: 'Interamente nell’Unione Europea, sotto legge europea, con pieni diritti GDPR.',
      },
      {
        q: 'Posso disdire quando voglio?',
        a: 'Sì. Puoi disdire dal tuo account in qualsiasi momento; il piano prosegue fino alla fine del periodo pagato.',
      },
    ],
  },
  finalCta: {
    title: 'Pronto a iniziare?',
    body: 'Crea il tuo account e prova Esempio gratis per 14 giorni.',
    primary: 'Inizia la prova gratuita',
    secondary: 'Perché appgrove',
  },
}
