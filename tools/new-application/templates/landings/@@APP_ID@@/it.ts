// Bozza landing di @@APP_NAME@@ — italiano. Copy generico on-brand: `finalize-landing`
// lo rifinisce. Il badge dell'hero porta il sentinella «DA RIFINIRE».
import type { LandingLocaleContent } from '../types.ts'

export const it: LandingLocaleContent = {
  slug: '@@LANDING_SLUG@@',
  meta: {
    title: '@@APP_NAME@@ — fai il lavoro, prima la privacy',
    description:
      '@@APP_NAME@@ è una micro-app tutta-UE e GDPR-first di appgrove: fa bene una cosa sola, con i tuoi dati al sicuro in Europa.',
    ogImage: null,
  },
  hero: {
    badge: 'DA RIFINIRE — tutta-UE · GDPR-first',
    title: 'Porta a termine il lavoro in minuti, non in un pomeriggio',
    subtitle:
      '@@APP_NAME@@ ti toglie di dosso un compito ricorrente — in pochi clic, con i dati ospitati in Europa e pieni diritti GDPR.',
    ctaPrimary: 'Inizia la prova gratuita',
    ctaSecondary: 'Guarda come funziona',
    screenshot: {
      src: null,
      alt: 'Schermata della dashboard di @@APP_NAME@@',
    },
  },
  problemSolution: {
    title: 'Meno lavoro amministrativo, più di ciò che conta',
    problem:
      'I piccoli team perdono ore in adempimenti ripetitivi e macchinosi — sparsi tra strumenti che non si parlano mai davvero.',
    solution:
      '@@APP_NAME@@ fa bene una cosa sola: ti toglie quel lavoro di mezzo, in pochi clic, con i dati al sicuro in Europa.',
  },
  features: {
    title: 'Tutto ciò che serve, niente che non serva',
    subtitle: 'Funzioni essenziali che fanno bene una cosa sola — nessuna suite gonfia, nessun lock-in.',
    items: [
      {
        icon: 'bolt',
        title: 'Veloce per default',
        body: 'Pronto in pochi minuti; le attività di ogni giorno si chiudono in un paio di clic.',
      },
      {
        icon: 'lock',
        title: 'Privato per progetto',
        body: 'I tuoi dati vivono nell’UE, sotto legge europea, con pieni diritti GDPR.',
      },
      {
        icon: 'sync',
        title: 'Un account, tutti gli strumenti',
        body: 'Aggiungi altre app appgrove quando servono — stesso accesso, stessa casa affidabile.',
      },
      {
        icon: 'smart_toy',
        title: 'Pronta per l’AI',
        body: 'Progettata per essere raggiunta dal tuo assistente AI, così il lavoro si fa dalla chat che usi già.',
      },
    ],
  },
  howItWorks: {
    title: 'Operativo in tre passi',
    steps: [
      { title: 'Crea il tuo account', body: 'Registrati in pochi secondi — nessuna carta per iniziare la prova.' },
      { title: 'Configura lo spazio di lavoro', body: 'Una configurazione guidata ti rende produttivo dal primo giorno.' },
      { title: 'Porta a termine il lavoro', body: 'Fai ciò che devi e lascia che @@APP_NAME@@ tenga l’amministrazione fuori dai piedi.' },
    ],
  },
  pricing: {
    title: 'Prezzi semplici e onesti',
    subtitle: 'Scegli il piano giusto. L’annuale costa meno; il mensile ti dà flessibilità.',
    monthlyLabel: 'Mensile',
    yearlyLabel: 'Annuale',
    trialNote: 'Ogni piano a pagamento parte con 14 giorni di prova gratuita — nulla è addebitato finché non finisce.',
    tiers: [
      {
        name: 'Starter',
        priceMonthly: '€0',
        priceYearly: '€0',
        features: ['Per iniziare', 'Funzioni di base', 'Supporto della community'],
      },
      {
        name: 'Pro',
        priceMonthly: '€00 / mese',
        priceYearly: '€000 / anno',
        features: ['Tutto di Starter', 'Tutte le funzioni', 'Supporto prioritario'],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'Ospitata nell’UE. Pieni diritti GDPR.',
    body: 'Qui la privacy non è un accessorio — è come è costruita appgrove. I tuoi dati restano in Europa e non vengono mai venduti.',
    points: ['Tutti i dati ospitati nell’UE', 'Pieni diritti GDPR, per progetto', 'Nessun tracciatore nascosto, nessun dato venduto'],
  },
  faq: {
    title: 'Domande frequenti',
    items: [
      {
        q: 'Serve una carta per provare @@APP_NAME@@?',
        a: 'No. La prova gratuita di 14 giorni parte senza carta; paghi solo se decidi di continuare.',
      },
      {
        q: 'Dove sono conservati i miei dati?',
        a: 'Interamente nell’Unione Europea, sotto legge europea, con pieni diritti GDPR.',
      },
      {
        q: 'Posso disdire quando voglio?',
        a: 'Sì. Puoi disdire dal tuo account in qualsiasi momento; il piano prosegue fino a fine periodo pagato.',
      },
    ],
  },
  finalCta: {
    title: 'Pronto a iniziare?',
    body: 'Crea il tuo account e prova @@APP_NAME@@ gratis per 14 giorni.',
    primary: 'Inizia la prova gratuita',
    secondary: 'Perché appgrove',
  },
}
