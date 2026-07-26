// Pilastro "Fatturazione per piccole imprese in UE" — italiano (UC 0042).
import type { PostLocaleContent } from '../types.ts'

export const it: PostLocaleContent = {
  slug: 'fatturazione-piccole-imprese-ue',
  title: 'Fatturazione per le piccole imprese in UE',
  description:
    'Guida in parole semplici alla fatturazione per le piccole imprese in UE: cosa serve a una fattura a norma, come funzionano IVA e vendite transfrontaliere, e come conservare i documenti nel rispetto del GDPR.',
  question: 'Come funziona la fatturazione per una piccola impresa in UE?',
  intro: [
    'Se gestisci una piccola impresa in Europa, la fatturazione è il punto in cui la burocrazia si accumula in silenzio: ogni vendita richiede un documento corretto, numerato, conservato per anni e — sempre più spesso — elettronico. Sbagliare significa rischiare una fattura respinta, un pagamento in ritardo o una domanda del fisco a cui non sai rispondere.',
    'Questa guida è la mappa. Spiega cosa deve contenere una fattura a norma, come cambiano le cose con l’imposta sul valore aggiunto (IVA) e con le vendite transfrontaliere, e come conservare i documenti senza trasformare i tuoi dati nel patrimonio di qualcun altro. Gli articoli collegati approfondiscono le due domande più frequenti.',
  ],
  sections: [
    {
      heading: 'Cosa deve contenere una fattura a norma',
      paragraphs: [
        'In tutta l’UE una fattura ha un nucleo comune: chi vende e chi acquista, un numero univoco e progressivo, la data, una descrizione chiara di ciò che è stato venduto, gli importi al netto e al lordo dell’imposta e l’aliquota IVA applicata (o il motivo per cui non c’è). Le regole nazionali aggiungono dettagli, ma questa ossatura è la stessa che tu fatturi a Milano, Madrid o Monaco.',
        'Il numero conta più di quanto sembri: deve essere univoco e senza salti all’interno della tua serie di numerazione, perché è il modo con cui il fisco rintraccia il documento. Uno strumento che lo assegna al posto tuo elimina la singola fonte di errore manuale più comune.',
      ],
    },
    {
      heading: 'IVA e vendite transfrontaliere',
      paragraphs: [
        'L’imposta sul valore aggiunto è la parte che mette in difficoltà. Vendere a un’impresa di un altro Paese UE spesso significa che è l’acquirente a versare l’imposta (meccanismo dell’inversione contabile, il cosiddetto reverse charge): la tua fattura non espone IVA ma deve dirne il motivo. Vendere a consumatori oltre confine può farti entrare nel regime dello sportello unico (One-Stop-Shop) una volta superata una soglia. Le regole si imparano, ma non perdonano le approssimazioni.',
        'L’abitudine sicura è decidere il trattamento IVA prima di inviare, non dopo. Conoscere lo stato e il Paese del cliente, e annotarlo in fattura, è ciò che tiene pulita una vendita transfrontaliera al momento di un controllo.',
      ],
    },
    {
      heading: 'Conservare i documenti — e tenerli tuoi',
      paragraphs: [
        'La maggior parte dei Paesi UE impone di conservare le fatture per diversi anni (spesso dieci) e molti oggi obbligano o incoraggiano la fattura elettronica in un formato strutturato. Vuol dire che il tuo archivio non è un cassetto di carta: è un insieme di dati, e dove vivono quei dati è una scelta reale.',
        'Qui appgrove prende una posizione netta: i tuoi dati di fatturazione sono ospitati in UE, sotto la legge europea, con pieni diritti GDPR e senza tracciatori nascosti. La conformità non è un componente che aggiungi dopo: è il contesto in cui i tuoi documenti stanno fin dal primo giorno.',
      ],
    },
  ],
  faq: {
    title: 'Domande frequenti sulla fatturazione in UE',
    items: [
      {
        q: 'Sono obbligato a emettere fatture elettroniche in UE?',
        a: 'Dipende dal Paese e dal cliente. La fatturazione verso la pubblica amministrazione è già elettronica in tutta l’UE, e diversi Paesi stanno estendendo la fattura elettronica strutturata anche alle vendite tra imprese. Anche dove la carta è ancora ammessa, i documenti elettronici sono più facili da conservare e da dimostrare.',
      },
      {
        q: 'Per quanto tempo devo conservare le fatture?',
        a: 'La maggior parte degli Stati membri UE richiede di conservare le fatture per diversi anni — spesso dieci. Il periodo esatto è fissato a livello nazionale, quindi verifica la regola del tuo Paese, ma prevedi una conservazione a lungo termine e a prova di manomissione, non file sparsi.',
      },
      {
        q: 'Cosa succede se la numerazione delle fatture ha un salto?',
        a: 'Un salto o un doppione nella numerazione progressiva è un campanello d’allarme in sede di controllo, perché la sequenza è il modo con cui il fisco verifica che non manchi nessuna fattura. La numerazione automatica e senza salti è il modo più semplice per evitare del tutto il problema.',
      },
    ],
  },
  ctaText: 'Scopri come appgrove Fatture se ne occupa per te',
}
