// Fixture di esempio (UC 0038) — francese.
import type { LandingLocaleContent } from '../types.ts'

export const fr: LandingLocaleContent = {
  slug: 'exemple',
  meta: {
    title: 'Exemple — le modèle de landing appgrove',
    description:
      'Démonstration du modèle de landing par application : huit sections à l’image de la marque, cinq langues, états brouillon et publié.',
    ogImage: null,
  },
  hero: {
    badge: 'tout en UE · RGPD d’abord',
    title: 'Faites le travail en minutes, pas en après-midis',
    subtitle:
      'Exemple est l’application de démonstration du modèle de landing appgrove. Elle montre comment chaque application raconte son histoire — le métier d’abord, la confidentialité comme signature de confiance.',
    ctaPrimary: 'Démarrer l’essai gratuit',
    ctaSecondary: 'Voir comment ça marche',
    screenshot: {
      src: null,
      alt: 'Capture d’écran du tableau de bord de l’application Exemple',
    },
  },
  problemSolution: {
    title: 'Moins d’administratif, plus d’essentiel',
    problem:
      'Les petites équipes perdent des heures dans des tâches répétitives et fastidieuses — réparties sur des outils qui ne communiquent jamais vraiment.',
    solution:
      'Exemple fait bien une seule chose : il vous décharge de cet administratif, en quelques clics, avec vos données en sécurité en Europe.',
  },
  features: {
    title: 'Tout ce qu’il vous faut, rien de superflu',
    subtitle: 'Des fonctions ciblées qui font bien un travail — pas de suite surchargée, pas de dépendance.',
    items: [
      {
        icon: 'bolt',
        title: 'Rapide par défaut',
        body: 'Prise en main en quelques minutes et tâches quotidiennes en deux clics.',
      },
      {
        icon: 'lock',
        title: 'Privée par conception',
        body: 'Vos données vivent dans l’UE, sous le droit européen, avec les pleins droits RGPD.',
      },
      {
        icon: 'sync',
        title: 'Un compte, tous les outils',
        body: 'Ajoutez d’autres applications appgrove quand vous en avez besoin — même connexion, même maison de confiance.',
      },
      {
        icon: 'smart_toy',
        title: 'Prête pour l’IA',
        body: 'Conçue pour être accessible à votre assistant IA, afin que le travail se fasse depuis la conversation que vous utilisez déjà.',
      },
    ],
  },
  howItWorks: {
    title: 'Opérationnel en trois étapes',
    steps: [
      { title: 'Créez votre compte', body: 'Inscription en quelques secondes — pas de carte pour démarrer l’essai.' },
      { title: 'Configurez votre espace', body: 'Une configuration guidée vous rend productif dès le premier jour.' },
      { title: 'Faites le travail', body: 'Faites votre travail et laissez Exemple tenir l’administratif à l’écart.' },
    ],
  },
  pricing: {
    title: 'Des tarifs simples et justes',
    subtitle: 'Choisissez le forfait adapté. L’annuel coûte moins ; le mensuel offre de la flexibilité.',
    monthlyLabel: 'Mensuel',
    yearlyLabel: 'Annuel',
    trialNote: 'Chaque forfait payant commence par un essai gratuit de 14 jours — rien n’est facturé avant la fin.',
    tiers: [
      {
        name: 'Starter',
        priceMonthly: '0 €',
        priceYearly: '0 €',
        features: ['Pour démarrer', 'Fonctions essentielles', 'Support communautaire'],
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
    title: 'Hébergée dans l’UE. Pleins droits RGPD.',
    body: 'Ici la confidentialité n’est pas une option — c’est ainsi qu’appgrove est construit. Vos données restent en Europe et ne sont jamais vendues.',
    points: ['Toutes les données hébergées dans l’UE', 'Pleins droits RGPD, par conception', 'Pas de traceurs cachés, aucune donnée vendue'],
  },
  faq: {
    title: 'Questions fréquentes',
    items: [
      {
        q: 'Faut-il une carte pour essayer Exemple ?',
        a: 'Non. L’essai gratuit de 14 jours démarre sans carte ; vous ne payez que si vous décidez de continuer.',
      },
      {
        q: 'Où mes données sont-elles stockées ?',
        a: 'Entièrement dans l’Union européenne, sous le droit européen, avec les pleins droits RGPD.',
      },
      {
        q: 'Puis-je annuler à tout moment ?',
        a: 'Oui. Vous pouvez annuler depuis votre compte à tout moment ; le forfait court jusqu’à la fin de la période payée.',
      },
    ],
  },
  finalCta: {
    title: 'Prêt à démarrer ?',
    body: 'Créez votre compte et essayez Exemple gratuitement pendant 14 jours.',
    primary: 'Démarrer l’essai gratuit',
    secondary: 'Pourquoi appgrove',
  },
}
