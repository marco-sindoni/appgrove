// Articolo cluster "Fattura elettronica a norma" — francese (UC 0042).
import type { PostLocaleContent } from '../types.ts'

export const fr: PostLocaleContent = {
  slug: 'facture-electronique-conforme',
  title: 'Comment créer une facture électronique conforme',
  description:
    'Une réponse pas à pas : ce qui rend une facture électronique conforme dans l’UE, les champs qu’elle doit porter, et comment l’envoyer et l’archiver pour qu’elle tienne lors d’un contrôle.',
  question: 'Comment créer une facture électronique conforme ?',
  intro: [
    'Une facture électronique n’est pas simplement un PDF envoyé par e-mail. Au sens de l’UE, c’est une facture émise, transmise et reçue dans un format électronique structuré qu’un ordinateur peut traiter automatiquement — et être conforme signifie qu’elle porte les bons champs et qu’elle est archivée de façon à ne pas pouvoir être modifiée discrètement.',
    'Voici la version courte et pratique : quoi y mettre, comment l’envoyer et comment l’archiver. Rien de difficile une fois que vous le faites toujours de la même manière — ce à quoi sert précisément un outil.',
  ],
  sections: [
    {
      heading: 'Les champs qu’elle doit porter',
      paragraphs: [
        'Partez du socle obligatoire : vos coordonnées et celles de votre client, un numéro unique et séquentiel, la date d’émission, une description claire ligne par ligne, le montant hors taxe, le taux et le montant de TVA (ou le motif d’exonération) et le total. Pour les clients professionnels, dans de nombreux pays, il faut aussi leur numéro de TVA ; pour les ventes transfrontalières entre entreprises, la mention que l’autoliquidation s’applique.',
        'Oubliez-en un et la facture peut être rejetée ou contestée. L’intérêt de les remplir à partir de données structurées — client, produit, règle fiscale — plutôt que de les ressaisir, c’est que les mêmes champs sortent corrects à chaque fois.',
      ],
    },
    {
      heading: 'L’envoyer dans le bon format',
      paragraphs: [
        'La conformité signifie de plus en plus un format structuré, pas l’image d’une facture. La facturation vers le secteur public dans l’UE utilise déjà des formats électroniques structurés, et plusieurs pays acheminent les factures entre entreprises par une plateforme nationale ou un système de contrôle avant qu’elles n’atteignent le client.',
        'Le point pratique : vérifiez si votre pays ou votre client exigent un canal spécifique, et générez la facture dans un format que ce canal accepte. Un outil qui produit le format structuré à votre place transforme une question de conformité en non-événement.',
      ],
    },
    {
      heading: 'L’archiver pour qu’elle tienne',
      paragraphs: [
        'Une facture électronique conforme doit être conservée pendant toute la durée de conservation — souvent dix ans — de manière à en préserver l’authenticité et l’intégrité. En clair : vous devez pouvoir prouver qu’elle n’a pas été modifiée depuis son émission, et la produire sur demande.',
        'C’est autant une décision d’archivage qu’une question de format. Avec appgrove, vos factures sont archivées dans l’UE, sous le droit européen, avec tous les droits RGPD — de sorte que le document sur lequel vous vous appuierez dans des années reste le vôtre et sous votre contrôle.',
      ],
    },
  ],
  faq: {
    title: 'Questions sur les factures électroniques conformes',
    items: [
      {
        q: 'Un PDF est-il une facture électronique ?',
        a: 'Pas au sens strict de l’UE. Un PDF est une image lisible par l’humain ; une facture électronique conforme est émise dans un format structuré qu’un ordinateur peut traiter automatiquement. Là où le format structuré est exigé, un simple PDF ne suffit pas.',
      },
      {
        q: 'Quelle est l’erreur la plus fréquente ?',
        a: 'Une rupture dans la numérotation séquentielle, ou un champ obligatoire manquant comme le traitement TVA sur une vente transfrontalière. Les deux s’évitent quand la facture est construite à partir de données structurées plutôt que saisie à la main à chaque fois.',
      },
      {
        q: 'Comment prouver qu’une facture n’a pas été modifiée ?',
        a: 'En l’archivant dans un système qui en préserve l’intégrité pendant toute la durée de conservation et peut la produire inchangée sur demande. L’enjeu n’est pas une technique unique, mais un dispositif d’archivage que vous pouvez défendre lors d’un contrôle.',
      },
    ],
  },
  ctaText: 'Créez des factures conformes avec appgrove Facturation',
}
