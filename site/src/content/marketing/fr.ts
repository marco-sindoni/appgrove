// Contenuti marketing — francese (UC 0037). Tono lean, job-led + privacy come firma.
import type { MarketingContent } from './types.ts'

export const fr: MarketingContent = {
  nav: {
    app: 'Applications',
    why: 'Pourquoi appgrove',
    pricing: 'Tarifs',
    login: 'Se connecter',
    signup: "S'inscrire",
  },
  hero: {
    badge: 'tout en Europe · priorité au RGPD',
    title: 'Des outils simples qui grandissent avec votre entreprise',
    subtitle:
      "Un écosystème d'applications rapides et abordables pour les petites et moyennes entreprises. Moins de temps sur la gestion, plus de temps pour votre métier.",
    ctaPrimary: 'Commencer',
    ctaSecondary: 'Découvrir les applications',
  },
  apps: {
    title: "Un seul compte, tous les outils qu'il vous faut",
    subtitle: 'Des applications ciblées qui font bien une seule chose — sans suites surchargées, sans verrouillage.',
    comingSoon: 'Bientôt disponible',
    flagshipName: 'Facturation',
    flagshipCategory: 'Factures et paiements',
    flagshipDescription:
      'Créez et envoyez des factures conformes en quelques minutes, suivez les paiements et gardez vos comptes en ordre.',
    moreTitle: "D'autres outils arrivent",
    moreText:
      "Le catalogue est volontairement restreint, et il s'étoffe. De nouvelles applications rejoignent régulièrement l'écosystème — et un seul compte les débloque toutes.",
  },
  crossSell: {
    title: 'Un compte, de nombreux outils',
    body: "À mesure que l'écosystème grandit, votre compte grandit avec lui. Ajoutez un nouvel outil quand vous en avez besoin — même connexion, même foyer de confiance pour vos données.",
    points: [
      'Une seule connexion pour toutes les applications',
      'Une expérience simple et cohérente entre les outils',
      'Ajoutez des outils à mesure que votre entreprise grandit',
    ],
  },
  ai: {
    title: "Prêtes pour l'IA",
    body: "Chaque application appgrove est conçue pour être appelée par votre assistant IA. Demandez en langage naturel dans ChatGPT, Claude ou Perplexity — et laissez-le accomplir la tâche dans votre application.",
    points: [
      "Basées sur MCP (Model Context Protocol), le standard ouvert par lequel les assistants IA appellent des outils externes",
      'Faites le travail directement depuis la conversation que vous utilisez déjà',
      'Vos outils, accessibles là où vous travaillez déjà',
    ],
    note: "C'est ainsi que nous concevons l'écosystème — nous y avançons dès aujourd'hui, application par application.",
  },
  privacy: {
    title: 'Hébergé en Europe. Droits RGPD complets.',
    body: "Vos données vivent en Europe, sous le droit européen. Ici, la confidentialité n'est pas une option : c'est la façon dont appgrove est construit.",
    points: [
      'Toutes les données hébergées en Europe',
      'Droits RGPD complets, dès la conception',
      'Aucun traceur caché, aucune donnée revendue',
    ],
  },
  faq: {
    title: 'Questions fréquentes',
    items: [
      {
        q: "Qu'est-ce qu'appgrove ?",
        a: "appgrove est une place de marché entièrement européenne et axée RGPD d'applications micro-SaaS ciblées pour les petites et moyennes entreprises. Un seul compte débloque un écosystème grandissant d'outils simples et abordables, chacun faisant bien une seule chose.",
      },
      {
        q: 'Où mes données sont-elles stockées ?',
        a: "Toutes les données d'appgrove sont hébergées dans l'Union européenne, sous le droit européen. Aucun traceur caché, et vos données ne sont jamais revendues.",
      },
      {
        q: 'Ai-je des droits RGPD complets ?',
        a: 'Oui. Les droits RGPD sont intégrés dès la conception — vous pouvez accéder à vos données, les exporter et les effacer. La confidentialité est le socle, pas une option.',
      },
      {
        q: 'Comment fonctionne la tarification ?',
        a: "Chaque application fixe son prix sur sa propre page. Vous ne payez que les outils que vous utilisez, au mois ou à l'année, et chaque formule payante commence par un essai gratuit de 14 jours.",
      },
      {
        q: 'Puis-je utiliser les applications appgrove avec un assistant IA ?',
        a: "C'est ainsi que nous concevons l'écosystème : chaque application est conçue pour être appelée par les assistants IA via MCP (Model Context Protocol). Nous y avançons dès aujourd'hui, application par application.",
      },
    ],
  },
  newsletter: {
    title: 'Restez informé',
    body: "On vous prévient quand de nouvelles applications arrivent. Pas de spam, et vous vous désinscrivez quand vous voulez.",
    placeholder: 'vous@email.com',
    cta: 'Me prévenir',
    consentLabel: "J'accepte de recevoir la newsletter d'appgrove. Pas de spam, désinscription en un clic.",
    success: 'Vous y êtes presque : vérifiez votre e-mail et confirmez votre inscription.',
    error: "Une erreur s'est produite. Veuillez réessayer.",
    note: "Nous ne vous écrirons qu'au sujet d'appgrove. Désinscription à tout moment.",
  },
  finalCta: {
    title: 'Prêt à commencer ?',
    body: "Créez votre compte et explorez l'écosystème — essai gratuit.",
    primary: 'Commencer',
    secondary: 'Pourquoi appgrove',
  },
  why: {
    title: 'Pourquoi appgrove',
    intro:
      'Nous construisons des outils simples et ciblés pour les petites et moyennes entreprises en Europe. Quatre promesses guident tout ce que nous faisons.',
    sections: [
      {
        title: 'Des outils qui vous rendent du temps',
        body: "Les petites entreprises n'ont pas besoin de suites surchargées. Elles ont besoin d'outils qui font bien une chose, vite, à un prix juste. Chaque application appgrove est pensée pour vous décharger de la gestion afin que vous vous concentriez sur votre métier.",
      },
      {
        title: 'Un écosystème en croissance',
        body: "Un compte, de nombreux outils. Quand vos besoins grandissent, l'écosystème grandit avec vous — ajoutez une nouvelle application quand il le faut, sans nouvelles connexions ni nouveaux silos.",
      },
      {
        title: "Prêtes pour l'IA",
        body: "Nous concevons chaque application pour qu'elle soit accessible à votre assistant IA, via MCP — le standard ouvert par lequel les assistants IA appellent des outils externes. La vision : vous demandez en langage naturel, et le travail se fait dans votre application. Nous y avançons dès aujourd'hui, application par application.",
      },
      {
        title: 'Européen par conception, privé par défaut',
        body: 'Vos données sont hébergées en Europe, sous le droit européen, avec des droits RGPD complets. La confidentialité est le socle, pas une fonctionnalité — aucun traceur caché, et vos données ne sont jamais revendues.',
      },
    ],
    faq: {
      title: "En savoir plus sur appgrove",
      items: [
        {
          q: "Qu'est-ce qui distingue appgrove d'une grande suite logicielle ?",
          a: "appgrove est un écosystème de petites applications ciblées plutôt qu'une seule suite surchargée. Chaque outil fait bien une chose, et un seul compte vous les donne toutes — sans verrouillage, sans silos.",
        },
        {
          q: 'Ai-je besoin d’un compte distinct pour chaque application ?',
          a: "Non. Un seul compte débloque toutes les applications de l'écosystème, avec une seule connexion et un seul foyer de confiance pour vos données.",
        },
        {
          q: 'Que signifie « Prêtes pour l’IA » ?',
          a: "Chaque application est conçue pour être appelée par votre assistant IA via MCP, le standard ouvert que les assistants utilisent pour atteindre des outils externes. C'est notre direction de conception, construite application par application.",
        },
        {
          q: 'appgrove est-il vraiment tout européen ?',
          a: "Oui. Les données sont hébergées en Europe sous le droit européen, avec des droits RGPD complets, aucun traceur caché et aucune revente de données — le socle de confiance derrière tout ce que nous construisons.",
        },
      ],
    },
  },
  pricing: {
    title: 'Tarifs — comment fonctionne la facturation',
    intro:
      "Chaque application fixe son prix sur sa propre page. Voici comment fonctionne la facturation dans appgrove.",
    sections: [
      {
        title: 'Un tarif par application',
        body: "Vous ne payez que les outils que vous utilisez. Chaque application affiche ses formules et ses prix sur sa page — choisissez ce qui convient à votre entreprise.",
      },
      {
        title: 'Mensuel ou annuel',
        body: "Choisissez le cycle de facturation qui vous convient. L'annuel est proposé par défaut et coûte moins cher sur l'année ; le mensuel offre de la souplesse.",
      },
      {
        title: "Essai gratuit de 14 jours",
        body: 'Essayez avant de payer. Chaque formule payante commence par un essai gratuit de 14 jours — rien ne vous est facturé avant la fin.',
      },
      {
        title: 'Résiliez quand vous voulez',
        body: "Chaque formule commence par un essai gratuit, vous décidez donc sans risque avant de payer — c'est pourquoi les montants déjà facturés ne sont pas restitués. Dans tous les cas, vous pouvez résilier votre abonnement quand vous voulez : dès le cycle de facturation suivant, plus rien ne vous est facturé, et la période en cours reste active jusqu'à son terme. Les détails figurent dans notre politique de remboursement.",
      },
    ],
    refundLinkText: 'Lire la politique de remboursement',
    faq: {
      title: 'Questions sur la facturation',
      items: [
        {
          q: 'Combien coûte appgrove ?',
          a: "Il n'y a pas de prix unique : chaque application affiche ses propres formules et prix sur sa page. Vous ne payez que les outils que vous utilisez.",
        },
        {
          q: 'Mensuel ou annuel ?',
          a: "Les deux. L'annuel est proposé par défaut et coûte moins cher sur l'année ; le mensuel offre plus de souplesse. Vous choisissez pour chaque application.",
        },
        {
          q: 'Y a-t-il un essai gratuit ?',
          a: 'Oui. Chaque formule payante commence par un essai gratuit de 14 jours — rien ne vous est facturé avant la fin.',
        },
        {
          q: 'Puis-je obtenir un remboursement ?',
          a: "Chaque formule commence par un essai gratuit, vous essayez donc sans risque avant de payer — c'est pourquoi les montants déjà facturés ne sont pas restitués. Vous pouvez toutefois résilier votre abonnement quand vous voulez : dès le cycle de facturation suivant, plus rien ne vous est facturé, et la période en cours reste active jusqu'à son terme. Les détails figurent dans notre politique de remboursement.",
        },
      ],
    },
  },
  footer: {
    tagline: 'Des outils simples qui grandissent avec votre entreprise.',
    legalHeading: 'Mentions légales',
    supportLabel: 'Assistance',
    securityLabel: 'Sécurité',
    newsletterTitle: 'Newsletter',
    newsletterBody: 'Les nouvelles applications, dans votre boîte mail.',
    newsletterPlaceholder: 'vous@email.com',
    newsletterCta: 'Me prévenir',
    rights: 'appgrove — tout en Europe, priorité au RGPD.',
  },
}
