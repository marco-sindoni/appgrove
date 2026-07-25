// Bozza landing di @@APP_NAME@@ — francese. Copy generico on-brand: `finalize-landing`
// lo rifinisce. Il badge dell'hero porta il sentinella «DA RIFINIRE».
import type { LandingLocaleContent } from '../types.ts'

export const fr: LandingLocaleContent = {
  slug: '@@LANDING_SLUG@@',
  meta: {
    title: '@@APP_NAME@@ — faites le travail, la confidentialité d’abord',
    description:
      '@@APP_NAME@@ est une micro-app 100 % UE et RGPD-first d’appgrove : elle fait bien une seule chose, vos données à l’abri en Europe.',
    ogImage: null,
  },
  hero: {
    badge: 'DA RIFINIRE — 100 % UE · RGPD-first',
    title: 'Faites le travail en minutes, pas en après-midi',
    subtitle:
      '@@APP_NAME@@ vous décharge d’une tâche récurrente — en quelques clics, avec vos données hébergées en Europe et tous vos droits RGPD.',
    ctaPrimary: 'Démarrer l’essai gratuit',
    ctaSecondary: 'Voir comment ça marche',
    screenshot: {
      src: null,
      alt: 'Capture d’écran du tableau de bord @@APP_NAME@@',
    },
  },
  problemSolution: {
    title: 'Moins d’administratif, plus d’essentiel',
    problem:
      'Les petites équipes perdent des heures en tâches administratives répétitives — éparpillées entre des outils qui ne se parlent jamais vraiment.',
    solution:
      '@@APP_NAME@@ fait bien une seule chose : elle vous enlève cet administratif, en quelques clics, vos données à l’abri en Europe.',
  },
  features: {
    title: 'Tout ce qu’il faut, rien de superflu',
    subtitle: 'Des fonctions ciblées qui font bien une seule chose — pas de suite surchargée, pas de verrouillage.',
    items: [
      {
        icon: 'bolt',
        title: 'Rapide par défaut',
        body: 'Configuré en quelques minutes ; les tâches quotidiennes se bouclent en deux clics.',
      },
      {
        icon: 'lock',
        title: 'Privé par conception',
        body: 'Vos données vivent dans l’UE, sous le droit européen, avec tous vos droits RGPD.',
      },
      {
        icon: 'sync',
        title: 'Un compte, tous les outils',
        body: 'Ajoutez d’autres apps appgrove quand vous en avez besoin — même connexion, même maison de confiance.',
      },
      {
        icon: 'smart_toy',
        title: 'Prête pour l’IA',
        body: 'Conçue pour être atteinte par votre assistant IA, pour faire le travail depuis la conversation que vous utilisez déjà.',
      },
    ],
  },
  howItWorks: {
    title: 'Opérationnel en trois étapes',
    steps: [
      { title: 'Créez votre compte', body: 'Inscription en quelques secondes — pas de carte pour démarrer l’essai.' },
      { title: 'Préparez votre espace', body: 'Une configuration guidée vous rend productif dès le premier jour.' },
      { title: 'Faites le travail', body: 'Faites ce que vous avez à faire et laissez @@APP_NAME@@ écarter l’administratif.' },
    ],
  },
  pricing: {
    title: 'Des tarifs simples et justes',
    subtitle: 'Choisissez le forfait qui convient. L’annuel coûte moins ; le mensuel offre de la souplesse.',
    monthlyLabel: 'Mensuel',
    yearlyLabel: 'Annuel',
    trialNote: 'Chaque forfait payant commence par 14 jours d’essai gratuit — rien n’est débité avant la fin.',
    tiers: [
      {
        name: 'Starter',
        priceMonthly: '0 €',
        priceYearly: '0 €',
        features: ['Pour démarrer', 'Fonctions de base', 'Support communautaire'],
      },
      {
        name: 'Pro',
        priceMonthly: '00 € / mois',
        priceYearly: '000 € / an',
        features: ['Tout Starter', 'Toutes les fonctions', 'Support prioritaire'],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'Hébergée dans l’UE. Tous vos droits RGPD.',
    body: 'Ici la confidentialité n’est pas une option — c’est ainsi qu’appgrove est construit. Vos données restent en Europe et ne sont jamais vendues.',
    points: ['Toutes les données hébergées dans l’UE', 'Tous vos droits RGPD, par conception', 'Aucun traqueur caché, aucune donnée vendue'],
  },
  faq: {
    title: 'Questions fréquentes',
    items: [
      {
        q: 'Faut-il une carte pour essayer @@APP_NAME@@ ?',
        a: 'Non. L’essai gratuit de 14 jours démarre sans carte ; vous ne payez que si vous décidez de continuer.',
      },
      {
        q: 'Où sont stockées mes données ?',
        a: 'Entièrement dans l’Union européenne, sous le droit européen, avec tous vos droits RGPD.',
      },
      {
        q: 'Puis-je résilier à tout moment ?',
        a: 'Oui. Vous pouvez résilier depuis votre compte à tout moment ; le forfait court jusqu’à la fin de la période payée.',
      },
    ],
  },
  finalCta: {
    title: 'Prêt à commencer ?',
    body: 'Créez votre compte et essayez @@APP_NAME@@ gratuitement pendant 14 jours.',
    primary: 'Démarrer l’essai gratuit',
    secondary: 'Pourquoi appgrove',
  },
}
