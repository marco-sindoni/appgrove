// Pilastro "Fatturazione per piccole imprese in UE" — francese (UC 0042).
import type { PostLocaleContent } from '../types.ts'

export const fr: PostLocaleContent = {
  slug: 'facturation-petites-entreprises-ue',
  title: 'La facturation pour les petites entreprises dans l’UE',
  description:
    'Un guide en langage clair sur la facturation des petites entreprises dans l’UE : ce que doit contenir une facture conforme, comment fonctionnent la TVA et les ventes transfrontalières, et comment archiver dans le respect du RGPD.',
  question: 'Comment fonctionne la facturation pour une petite entreprise dans l’UE ?',
  intro: [
    'Quand on dirige une petite entreprise en Europe, la facturation est l’endroit où l’administratif s’accumule en silence : chaque vente exige un document correct, numéroté, conservé des années et — de plus en plus — électronique. Une erreur, et c’est une facture rejetée, un paiement en retard ou une question du fisc sans réponse.',
    'Ce guide est la carte. Il couvre ce que doit contenir une facture conforme, comment la taxe sur la valeur ajoutée (TVA) et les ventes transfrontalières changent la donne, et comment archiver sans transformer vos données en actif de quelqu’un d’autre. Les articles liés approfondissent les deux questions les plus posées.',
  ],
  sections: [
    {
      heading: 'Ce que doit contenir une facture conforme',
      paragraphs: [
        'Partout dans l’UE, une facture porte un socle commun : qui vend et qui achète, un numéro unique et séquentiel, la date, une description claire de ce qui a été vendu, les montants hors taxe et toutes taxes comprises, et le taux de TVA appliqué (ou la raison de son absence). Les règles nationales ajoutent des détails, mais cette ossature est la même que vous facturiez à Milan, Madrid ou Munich.',
        'Le numéro compte plus qu’il n’y paraît : il doit être unique et sans rupture au sein de votre série de numérotation, car c’est ainsi que l’administration fiscale retrace le document. Un outil qui l’attribue à votre place supprime la source d’erreur manuelle la plus fréquente.',
      ],
    },
    {
      heading: 'TVA et ventes transfrontalières',
      paragraphs: [
        'La taxe sur la valeur ajoutée est la partie qui piège. Vendre à une entreprise d’un autre pays de l’UE signifie souvent que c’est l’acheteur qui acquitte la taxe (mécanisme d’autoliquidation) : votre facture n’affiche pas de TVA mais doit en indiquer la raison. Vendre à des particuliers au-delà des frontières peut vous faire entrer dans le régime du guichet unique (One-Stop-Shop) une fois un seuil dépassé. Les règles s’apprennent, mais elles ne pardonnent pas l’à-peu-près.',
        'La bonne habitude est de décider du traitement TVA avant l’envoi, pas après. Connaître le statut et le pays de votre client, et le consigner sur la facture, c’est ce qui garde une vente transfrontalière propre au moment d’un contrôle.',
      ],
    },
    {
      heading: 'Archiver — et garder vos données à vous',
      paragraphs: [
        'La plupart des pays de l’UE imposent de conserver les factures plusieurs années (souvent dix), et beaucoup rendent désormais obligatoire ou encouragent la facture électronique dans un format structuré. Votre archive n’est donc pas un tiroir de papier : ce sont des données, et l’endroit où elles vivent est une véritable décision.',
        'C’est là qu’appgrove prend clairement position : vos données de facturation sont hébergées dans l’UE, sous le droit européen, avec tous les droits RGPD et sans traceurs cachés. La conformité n’est pas une option ajoutée après coup : c’est le cadre par défaut dans lequel vos documents se trouvent dès le premier jour.',
      ],
    },
  ],
  faq: {
    title: 'Questions fréquentes sur la facturation dans l’UE',
    items: [
      {
        q: 'Dois-je émettre des factures électroniques dans l’UE ?',
        a: 'Cela dépend du pays et du client. La facturation vers le secteur public est déjà électronique dans toute l’UE, et plusieurs pays étendent la facture électronique structurée aux ventes entre entreprises. Même là où le papier reste autorisé, les documents électroniques sont plus simples à conserver et à prouver.',
      },
      {
        q: 'Combien de temps dois-je conserver mes factures ?',
        a: 'La plupart des États membres de l’UE exigent de conserver les factures plusieurs années — souvent dix. La durée exacte est fixée au niveau national : vérifiez la règle de votre pays, mais prévoyez un archivage à long terme et infalsifiable, plutôt que des fichiers épars.',
      },
      {
        q: 'Que se passe-t-il si ma numérotation présente une rupture ?',
        a: 'Une rupture ou un doublon dans votre numérotation séquentielle est un signal d’alerte lors d’un contrôle, car la séquence permet à l’administration de vérifier qu’aucune facture ne manque. Une numérotation automatique et continue est le moyen le plus simple d’éviter tout le problème.',
      },
    ],
  },
  ctaText: 'Découvrez comment appgrove Facturation s’en charge pour vous',
}
