// Articolo cluster "Fattura elettronica a norma" — italiano (UC 0042).
import type { PostLocaleContent } from '../types.ts'

export const it: PostLocaleContent = {
  slug: 'fattura-elettronica-a-norma',
  title: 'Come creare una fattura elettronica a norma',
  description:
    'Una risposta passo passo: cosa rende una fattura elettronica a norma in UE, i campi che deve contenere e come inviarla e conservarla perché regga a un controllo.',
  question: 'Come creo una fattura elettronica a norma?',
  intro: [
    'Una fattura elettronica non è semplicemente un PDF che invii via email. Nel senso UE è una fattura emessa, trasmessa e ricevuta in un formato elettronico strutturato che un computer può elaborare in automatico — ed essere a norma significa che contiene i campi giusti ed è conservata in modo da non poter essere alterata di nascosto.',
    'Ecco la versione breve e pratica: cosa metterci, come inviarla e come conservarla. Nulla di difficile, una volta che lo fai sempre allo stesso modo — che è esattamente ciò a cui serve uno strumento.',
  ],
  sections: [
    {
      heading: 'I campi che deve contenere',
      paragraphs: [
        'Parti dal nucleo obbligatorio: i tuoi dati e quelli del cliente, un numero univoco e progressivo, la data di emissione, una descrizione chiara riga per riga, l’imponibile, l’aliquota e l’importo IVA (o il motivo dell’esenzione) e il totale. Per i clienti business, in molti Paesi, serve anche la loro partita IVA; per le vendite transfrontaliere tra imprese, l’indicazione che si applica l’inversione contabile.',
        'Se ne salti uno, la fattura può essere respinta o contestata. Il valore di compilarli da dati strutturati — cliente, prodotto, regola fiscale — invece di riscriverli è che gli stessi campi escono corretti ogni volta.',
      ],
    },
    {
      heading: 'Inviarla nel formato giusto',
      paragraphs: [
        'Essere a norma significa sempre più un formato strutturato, non l’immagine di una fattura. La fatturazione verso la pubblica amministrazione in UE usa già formati elettronici strutturati, e diversi Paesi instradano le fatture tra imprese attraverso una piattaforma nazionale o un sistema di controllo prima che raggiungano il cliente.',
        'Il punto pratico: verifica se il tuo Paese o il tuo cliente richiedono un canale specifico e genera la fattura in un formato che quel canale accetta. Uno strumento che produce per te il formato strutturato trasforma una questione di conformità in un non-problema.',
      ],
    },
    {
      heading: 'Conservarla perché regga',
      paragraphs: [
        'Una fattura elettronica a norma va conservata per l’intero periodo di conservazione — spesso dieci anni — in modo da preservarne autenticità e integrità. In parole semplici: devi poter dimostrare che non è stata modificata da quando è stata emessa, ed esibirla su richiesta.',
        'È una decisione di conservazione tanto quanto di formato. Con appgrove le tue fatture sono archiviate in UE, sotto la legge europea, con pieni diritti GDPR — così il documento su cui farai affidamento tra anni resta tuo e sotto il tuo controllo.',
      ],
    },
  ],
  faq: {
    title: 'Domande sulle fatture elettroniche a norma',
    items: [
      {
        q: 'Un PDF è una fattura elettronica?',
        a: 'Non nel senso stretto UE. Un PDF è un’immagine leggibile dalle persone; una fattura elettronica a norma è emessa in un formato strutturato che un computer può elaborare in automatico. Dove il formato strutturato è richiesto, un semplice PDF non basta.',
      },
      {
        q: 'Qual è l’errore più comune in assoluto?',
        a: 'Un salto nella numerazione progressiva, oppure un campo obbligatorio mancante come il trattamento IVA su una vendita transfrontaliera. Entrambi si evitano quando la fattura è costruita da dati strutturati invece che digitata a mano ogni volta.',
      },
      {
        q: 'Come dimostro che una fattura non è stata alterata?',
        a: 'Conservandola in un sistema che ne preserva l’integrità per l’intero periodo di conservazione e può esibirla immutata su richiesta. Il punto non è una singola tecnica, ma un impianto di conservazione su cui puoi rispondere in sede di controllo.',
      },
    ],
  },
  ctaText: 'Crea fatture a norma con appgrove Fatture',
}
