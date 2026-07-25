// Landing dell'app #1 "Fatture" (UC 0053) — francese.
import type { LandingLocaleContent } from '../types.ts'

export const fr: LandingLocaleContent = {
  slug: 'factures',
  meta: {
    title: 'Fatture — la facturation sans la paperasse, hébergée dans l’UE',
    description:
      'Créez, envoyez et suivez vos factures en quelques clics. Une application de facturation simple et mono-utilisateur — vos données hébergées dans l’UE, avec tous les droits RGPD.',
    ogImage: '/landings/fatture/og.png',
  },
  hero: {
    badge: 'tout dans l’UE · RGPD d’abord',
    title: 'La facturation, sans la paperasse',
    subtitle:
      'Avec Fatture, créez, envoyez et suivez vos factures en quelques clics — une application simple qui fait bien une seule chose, avec vos données hébergées dans l’UE.',
    ctaPrimary: 'Commencer gratuitement',
    ctaSecondary: 'Voir comment ça marche',
    screenshot: {
      src: '/landings/fatture/hero.fr.png',
      alt: 'La liste des factures de Fatture, avec numéro, client, statut et total',
    },
  },
  problemSolution: {
    title: 'Moins de temps sur la paperasse',
    problem:
      'Pour les indépendants et les petites entreprises, facturer rime avec modèles pénibles, numérotation manuelle et tableurs qui ne tombent jamais juste — du temps pris sur le vrai travail.',
    solution:
      'Fatture réduit tout cela à quelques clics : ajoutez un client, listez les lignes, et le total et le numéro progressif sont calculés pour vous. Rien à installer, aucun encombrement.',
  },
  features: {
    title: 'Tout ce qu’il faut pour facturer, rien de superflu',
    subtitle: 'Un outil simple qui fait bien une seule chose — pas de suite surchargée, aucun verrouillage.',
    items: [
      {
        icon: 'receipt_long',
        title: 'Des factures en quelques clics',
        body: 'Ajoutez un client et des lignes — description, quantité, prix unitaire — et Fatture calcule le total pour vous.',
      },
      {
        icon: 'tag',
        title: 'Numérotation automatique',
        body: 'Un numéro de facture progressif par an, attribué à votre place et jamais réutilisé — net et cohérent.',
      },
      {
        icon: 'fact_check',
        title: 'Un statut toujours clair',
        body: 'Faites passer chaque facture de brouillon à émise, payée ou annulée, pour toujours savoir où vous en êtes.',
      },
      {
        icon: 'lock',
        title: 'Hébergée dans l’UE',
        body: 'Vos factures vivent dans l’UE, sous le droit européen, avec tous les droits RGPD et aucun traceur caché.',
      },
    ],
  },
  howItWorks: {
    title: 'Opérationnel en trois étapes',
    steps: [
      { title: 'Créez votre compte', body: 'Inscription en quelques secondes — sans carte, le plan gratuit est prêt à l’emploi.' },
      { title: 'Ajoutez votre première facture', body: 'Saisissez le client et les lignes ; le total et le numéro de facture sont remplis pour vous.' },
      { title: 'Suivez-la jusqu’au paiement', body: 'Émettez la facture et mettez à jour son statut à mesure qu’elle passe de brouillon à payée.' },
    ],
  },
  pricing: {
    title: 'Un seul plan gratuit, sans surprise',
    subtitle: 'Fatture est gratuit. Créez jusqu’à 10 factures par mois — sans carte, sans essai qui expire.',
    monthlyLabel: 'Mensuel',
    yearlyLabel: 'Annuel',
    trialNote: 'Sans carte, jamais. Le plan gratuit reste le vôtre.',
    tiers: [
      {
        name: 'Gratuit',
        priceMonthly: '0 €',
        priceYearly: '0 €',
        features: [
          'Jusqu’à 10 factures par mois',
          'Clients et lignes de facture',
          'Suivi du statut et numérotation automatique',
          'Données hébergées dans l’UE, tous les droits RGPD',
        ],
        highlighted: true,
      },
    ],
  },
  privacy: {
    title: 'Hébergée dans l’UE. Tous les droits RGPD.',
    body: 'Vos factures restent en Europe, sous le droit européen. Vous pouvez exporter ou supprimer vos données vous-même, à tout moment — et elles ne sont jamais vendues.',
    points: [
      'Toutes les données hébergées dans l’UE',
      'Exportez ou supprimez vos données vous-même (RGPD)',
      'Factures conservées conformément aux obligations fiscales',
      'Aucun traceur caché, aucune donnée vendue',
    ],
  },
  faq: {
    title: 'Questions fréquentes',
    items: [
      {
        q: 'Combien coûte Fatture ?',
        a: 'C’est gratuit. Le plan actuel vous permet de créer jusqu’à 10 factures par mois, sans carte et sans essai qui expire.',
      },
      {
        q: 'Que se passe-t-il quand j’atteins 10 factures dans un mois ?',
        a: 'Le compteur mensuel se réinitialise au début de chaque mois civil : vous pouvez donc créer de nouvelles factures dès le mois suivant.',
      },
      {
        q: 'Où sont stockées mes données ?',
        a: 'Entièrement dans l’Union européenne, sous le droit européen, avec tous les droits RGPD.',
      },
      {
        q: 'Puis-je exporter ou supprimer mes données ?',
        a: 'Oui. Vous pouvez exporter ou supprimer définitivement vos données vous-même, depuis votre compte, à tout moment.',
      },
    ],
  },
  finalCta: {
    title: 'Prêt à envoyer votre première facture ?',
    body: 'Créez votre compte et commencez à facturer gratuitement — sans carte.',
    primary: 'Commencer gratuitement',
    secondary: 'Pourquoi appgrove',
  },
}
