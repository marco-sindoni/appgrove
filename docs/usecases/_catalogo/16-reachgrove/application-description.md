# ReachGrove — descrizione dell'applicazione

**Numero di catalogo**: 16 · **Tipo**: orizzontale · marketing · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 16](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** ReachGrove manda comunicazioni commerciali alle persone che hanno acconsentito a riceverle, e
conserva la prova che quel consenso esiste. Produce tre cose concrete: un archivio di iscritti in cui ogni
persona porta con sé **quando, come e con che testo** ha detto di sì; campagne di posta elettronica che partono
solo verso chi è davvero contattabile; percorsi automatici a passi (un messaggio di benvenuto, una sequenza dopo
l'iscrizione) che si fermano da soli quando la persona si disiscrive. Attorno a questo ci sono le cose che ci si
aspetta da uno strumento del genere: segmenti, modelli di messaggio, pagine e moduli pubblici di iscrizione,
prova a due varianti, rapporti di recapito e di rendimento.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50, mercato globale con priorità europea.
Chi compra è il titolare; chi usa tutti i giorni è **una** persona sola — il titolare stesso, un collaboratore
part-time, a volte un consulente esterno. Il profilo tipico ha da qualche centinaio a qualche migliaio di
indirizzi, manda da due a otto messaggi al mese e non ha nessuno il cui mestiere sia «fare marketing».

**Quale problema toglie.** Oggi quelle comunicazioni partono in tre modi, tutti sbagliati: dalla casella di posta
personale con gli indirizzi in copia nascosta (nessuna disiscrizione, recapito pessimo, e a un certo punto il
fornitore di posta blocca l'account); da un foglio di calcolo incollato dentro uno strumento gratuito (nessuno sa
più da dove vengono quegli indirizzi); oppure non partono affatto, perché il titolare ha sentito dire che «con la
privacy non si può più fare niente» e ha smesso. Il costo dei primi due modi non è il tempo perso: è che **non
esiste la prova** che quelle persone avessero acconsentito, e in un accertamento la prova la deve produrre chi ha
inviato. Il Garante italiano ha sanzionato con 45.000 € una società proprio per campagne di posta elettronica
senza consenso dimostrabile (§2.3, fonte 6).

Il terzo modo — non fare niente — è il problema più diffuso e il più caro: rinunciare al canale che costa meno di
tutti perché nessuno spiega cosa è lecito.

**Cosa NON fa.**

- **non rivende invii su canali di terzi.** La posta elettronica parte dalla nostra infrastruttura; i messaggi
  brevi e la messaggistica partono dal contratto che **il cliente** ha con il proprio fornitore, collegato da lui
  (§2.4 e epica 04). Il motivo sta al §11.1: è la stessa ragione per cui l'app 05 ChatGrove è stata esclusa dal
  catalogo, applicata qui senza uccidere il prodotto;
- **non compra e non fornisce liste di contatti**, e non ha nessun modo di importare una lista rendendola
  inviabile senza che qualcuno dichiari, contatto per contatto, da dove viene il consenso (storia 0010);
- **non fa carrello abbandonato**: richiede eventi da un negozio in rete, e nel catalogo attivo non c'è nessuna
  app di commercio elettronico (29 ShopGrove è fra le escluse). È il caso d'uso della scheda di catalogo che
  questa proposta **non copre**, detto subito (§11.4);
- **non è un archivio commerciale**: la scheda del cliente, la trattativa e lo storico della relazione sono
  l'app 04 LeadGrove. Qui c'è l'**iscrizione a una lista**, che è un'altra cosa (§10);
- **non assegna punteggi alle persone** e non prende decisioni automatizzate su di loro: i segmenti sono criteri
  scritti da una persona, leggibili e riproducibili;
- **non fa telefonate**, non compone numeri e non genera liste di chiamata: resta quindi fuori dagli obblighi di
  verifica del Registro pubblico delle opposizioni;
- **non raccoglie recensioni** (è l'app 17) e **non gestisce l'assistenza** (è l'app 12): un messaggio in arrivo
  in risposta a una campagna non apre un ticket qui.

**Rischio di sostituzione da parte dei modelli linguistici.** `misto`, come nel catalogo. La parte minacciata è
evidente: scrivere il testo di una campagna è esattamente ciò che un assistente generico fa bene, e la storia
0036 lo riconosce invece di negarlo — la generazione del testo è una **funzione della chat**, non un modulo che
proviamo a vendere. La parte rafforzata è tutto il resto: nessun assistente generico sa a chi si può scrivere e a
chi no, perché quella risposta sta in un registro di consensi che è dell'account. Il flusso di lavoro, la prova
del consenso e il recapito sono il valore; il testo è la parte che si regala.

---

## 2. Mercato e analisi in rete

> Compilata dopo 9 ricerche e recuperi di pagina ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4).
> Ciò che non è stato trovato è **dichiarato** al §2.7, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| MailerLite | Lituania (Unione europea) | Posta elettronica, automazioni, pagine di iscrizione; il più semplice della categoria | Gratuito: 250 iscritti e 2.500 email al mese, 2 posti; Comfort da 12 $/mese; Power da 25 $/mese con email illimitate; annuale −10 % — **rilevato sulla pagina ufficiale** | [mailerlite.com/pricing](https://www.mailerlite.com/pricing) |
| Brevo (ex Sendinblue) | Francia (Unione europea) | Posta elettronica, messaggi brevi, messaggistica, automazioni; il riferimento europeo | Prezzo per **email inviate**, non per contatti: gratuito 300 email al giorno; Starter da 9 $/mese (5.000 email), 18 $ (20.000), 29 $ (40.000), 65 $ (100.000); le automazioni sono escluse dallo Starter e cominciano con Business/Standard (~69 $ a 20.000 email) — **valori da fonti editoriali**, la pagina ufficiale non è risultata leggibile (§2.7) | [emailtooltester.com — Brevo pricing 2026](https://www.emailtooltester.com/en/reviews/brevo/pricing/) · [emailvendorselection.com — Brevo pricing](https://www.emailvendorselection.com/brevo-pricing/) |
| Mailchimp | Stati Uniti | Il nome che il cliente conosce; prezzo per **contatti archiviati** | Gratuito ridotto a 250 contatti e 500 email al mese (con tetto giornaliero di 250) dopo il taglio di gennaio 2026; Essentials da 13 $/mese a 500 contatti; Standard da 20 $; Premium da 350 $ a 10.000 contatti — **valori da fonti editoriali** | [emailtooltester.com — Mailchimp pricing 2026](https://www.emailtooltester.com/en/reviews/mailchimp/pricing/) · [emailvendorselection.com — Mailchimp pricing](https://www.emailvendorselection.com/mailchimp-pricing/) |
| Klaviyo | Stati Uniti | Automazione di marketing per il commercio elettronico; molto più grande del segmento micro | **prezzo non rilevato** in questa analisi; è però il prodotto attorno a cui si concentrano le lamentele sul prezzo per contatto (§2.5) | [emailvendorselection.com — guida ai costi 2026](https://www.emailvendorselection.com/email-marketing-cost-pricing-guide/) |

**Lettura.** La categoria è vecchia, affollata e con un prodotto europeo forte (Brevo) e uno semplice (MailerLite):
non si vince né per funzioni né per prezzo. I due spazi reali sono altrove. Il primo è l'**unità di misura**: il
mercato è spaccato fra chi fa pagare i contatti archiviati (Mailchimp, Klaviyo) e chi fa pagare gli invii (Brevo),
e la lamentela più ricorrente riguarda proprio il primo modello (§2.5). Il secondo è la **conformità come
funzione del prodotto**: tutti e quattro i concorrenti forniscono gli strumenti per essere in regola — la casella
di disiscrizione, l'informativa — ma nessuno **impedisce** al cliente di spedire a una lista di cui non sa niente.
È ciò che questa proposta mette nelle fondamenta invece che nella pagina di aiuto (epica 02).

### 2.2 Prezzi praticati nel dominio

- **Fascia d'ingresso**: 9-13 $/mese; **fascia media** 18-29 $/mese. La scheda di catalogo indica 15-49 €/mese,
  cioè un po' sopra il mercato rilevato: la differenza si spiega col fatto che il catalogo somma i tre canali.
- **Unità di misura**: due modelli in concorrenza fra loro.
  - **contatti archiviati** (Mailchimp, Klaviyo): il cliente paga anche gli indirizzi che non usa;
  - **email inviate** (Brevo): il cliente paga ciò che consuma;
  - **misto** (MailerLite: iscritti *e* volume).
- **Piano gratuito**: presente ovunque e in **contrazione**. Mailchimp l'ha tagliato a 250 contatti e 500 email al
  mese in gennaio 2026, da 500 contatti e 1.000 email; MailerLite dà 250 iscritti e 2.500 email; Brevo 300 email
  al giorno. La direzione del mercato è chiara: il gratuito serve a provare, non a vivere.
- **Prova gratuita**: non è la leva della categoria — dove c'è un gratuito permanente, la prova a tempo perde
  senso. È un dato che entra direttamente nella proposta di listino (§5).
- **Sconto annuale**: MailerLite dichiara −10 %. La convenzione appgrove (annuale = 10× il mensile, cioè −17 %) è
  quindi **più generosa** del riferimento europeo rilevato.

### 2.3 Obblighi normativi del settore

Il dominio ha una normativa propria, e non è solo quella generale sui dati personali: le comunicazioni
commerciali non richieste hanno una disciplina dedicata. È la sorgente principale dei requisiti di questa app.

1. **Consenso preventivo, come regola.** L'invio di comunicazioni commerciali per posta elettronica, messaggi
   brevi e sistemi automatizzati richiede il **consenso preventivo** dell'interessato (art. 130 del Codice
   privacy italiano, attuazione della direttiva sulla vita privata nelle comunicazioni elettroniche). Il consenso
   dev'essere libero, specifico, informato, **documentato** e revocabile in ogni momento. Conseguenza sul
   prodotto: una casella booleana «accetta il marketing» non basta — serve un **registro ad accrescimento** con
   momento, canale, testo accettato e origine (storia 0007).
   Fonte: [Art. 130 Codice privacy — testo e significato](https://leggeinchiaro.it/articolo-130-codice-privacy-comunicazioni-indesiderate-spam/).
2. **L'eccezione del «soft spam» esiste, ma è stretta.** Il comma 4 dell'art. 130 consente di usare l'indirizzo di
   posta elettronica raccolto **nel contesto di una vendita** per promuovere prodotti o servizi **analoghi**, se
   l'interessato è stato informato e può opporsi facilmente a ogni invio. Va interpretata restrittivamente: la
   Cassazione esclude che basti l'iscrizione a una newsletter o l'uso di un portale aggregatore — serve un
   rapporto di vendita effettivo e oneroso — e il Garante ha escluso l'estensione ai messaggi brevi. Conseguenza:
   la base giuridica dell'iscritto può valere `consenso` **o** `soft spam`, ma il secondo è ammesso **solo** sul
   canale della posta elettronica e obbliga a registrare a quale vendita si riferisce (storia 0007).
   Fonti: [Soft spam — regole d'ingaggio senza consenso, solo email](https://globalcom.it/privacy/soft-spam-regole-dingaggio-senza-consenso-solo-email/) ·
   [DGRS — l'applicazione in Italia della disciplina del soft spam, analisi comparata](https://www.dgrs.it/lapplicazione-in-italia-della-disciplina-del-soft-spam-e-linterpretazione-dei-paesi-europei-unanalisi-comparata/).
3. **La doppia conferma è la misura minima attesa.** Il Garante considera la conferma in due passi (l'iscritto
   riceve un messaggio e conferma di essere lui) una misura minima di garanzia allo stato dell'arte, in
   particolare quando le liste vengono da terzi; e ha stabilito che gli **account non confermati vanno esclusi
   dalle liste di marketing**. Conseguenza: la doppia conferma non è un'opzione da spuntare, è il comportamento
   predefinito del modulo di iscrizione (storia 0008), e un iscritto non confermato **non è inviabile**.
   Fonti: [Osservatorio Data Protection — double opt-in e consenso per finalità di marketing](https://www.osservatorio-dataprotection.it/data-protection/garante-privacy-double-opt-in-e-consenso-per-finalita-di-marketing/) ·
   [Garante privacy, provvedimento del 25 settembre 2025 n. 10191282](https://www.garanteprivacy.it/home/docweb/-/docweb-display/docweb/10191282) ·
   [Tom's Hardware — account non confermati fuori dalle liste marketing](https://www.tomshw.it/business/il-garante-multa-altroconsumo-gli-account-non-confermati-vanno-esclusi-dal-marketing).
4. **La responsabilità non si scarica sul fornitore della lista.** Chi invia deve conservare intatta la prova
   dell'origine del dato, della conferma e del consenso: un file modificabile può non bastare in un procedimento.
   Conseguenza diretta: le registrazioni di consenso di questa app **non si modificano e non si cancellano** — si
   aggiungono (storia 0007), e le liste importate senza prova finiscono in **quarantena** (storia 0010).
   Fonti: [RPLT — privacy e campagne promozionali: non basta delegare, serve controllare](https://www.rplt.it/privacy-e-campagne-promozionali-non-basta-delegare-serve-controllare/) ·
   [Federprivacy — sanzione di 45.000 € per email marketing senza consenso](https://www.federprivacy.org/informazione/garante-privacy/il-garante-sanziona-per-45mila-euro-una-societa-di-rivendita-auto-online-per-email-marketing-senza-consenso).
5. **Requisiti tecnici dei grandi fornitori di posta — non sono legge, ma vincolano quanto la legge.** Google,
   Yahoo, Microsoft e Apple impongono a chi manda posta in volume: autenticazione del mittente con SPF, DKIM e
   DMARC con allineamento del dominio della riga «Da:»; **disiscrizione in un clic** dentro il messaggio
   (RFC 8058) onorata **entro due giorni**; tasso di segnalazioni di posta indesiderata **sotto lo 0,3 %** (con
   0,1 % come obiettivo). La soglia dichiarata è 5.000 messaggi al giorno verso lo stesso fornitore. Chi non
   rispetta non finisce nella cartella della posta indesiderata: viene **respinto** dal server ricevente.
   Conseguenza sul prodotto: nessun invio parte da un dominio non verificato (storia 0017), l'intestazione di
   disiscrizione in un clic è obbligatoria e non disattivabile (storia 0012), il tasso di segnalazione è una
   metrica sorvegliata con blocco automatico (storie 0021 e 0032).
   Fonti: [Red Sift — checklist requisiti mittenti in volume 2026](https://redsift.com/guides/bulk-email-sender-requirements) ·
   [PowerDMARC — regole per mittenti in volume di Google, Yahoo, Microsoft e Apple](https://powerdmarc.com/bulk-email-sender-requirements/).
6. **Diritti dell'interessato.** Cancellazione, opposizione e portabilità arrivano al cliente dell'app (titolare
   del trattamento) e scendono su di noi (responsabili). Tutte le tabelle con dati di persone devono comparire
   nell'esportazione e nella cancellazione (§6). Attenzione al punto delicato: la **prova del consenso
   sopravvive alla revoca** — serve a dimostrare che l'invio di ieri era lecito — e viene meno solo con la
   cancellazione dei dati dell'interessato.

Non ho trovato un termine di legge per la **conservazione** delle prove di consenso: la durata è una scelta del
titolare, da dichiarare e motivare (§2.7).

### 2.4 Integrazioni attese dal cliente

| # | Integrazione | Perché la chiedono | Fornitore esterno che tratterebbe dati? |
|---|---|---|---|
| 1 | **Infrastruttura di invio della posta** | è l'app: senza un fornitore che consegna, non parte niente | **sì, e non è opzionale** — è il fornitore che tratta i dati per nostro conto sul canale primario. Va nell'elenco dei fornitori (§6) |
| 2 | **Archivio commerciale** (04 LeadGrove) | non voler reinserire a mano gli stessi contatti | no: è dentro la piattaforma, e passa da eventi, non da chiamate (§10) |
| 3 | **Messaggi brevi** | promemoria e comunicazioni urgenti | **sì, ma col contratto del cliente**: le credenziali sono sue, il fornitore risponde a lui (storia 0023) |
| 4 | **Messaggistica** | «i miei clienti mi rispondono lì» | **sì, col contratto del cliente**, e con trasferimento verso un paese terzo: è il punto più delicato dell'app (§6, §11.1) |
| 5 | **Sito del cliente** (modulo di iscrizione da incorporare) | raccogliere iscritti dove i visitatori già sono | no: il modulo è servito da noi, il sito lo ospita in una cornice |
| 6 | **Negozio in rete** (carrello abbandonato) | il caso d'uso citato nella scheda di catalogo | **fuori perimetro**: nessuna app di commercio nel catalogo attivo (§11.4) |

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Dalla rassegna delle lamentele ricorrenti sulla categoria
([emailvendorselection.com — guida ai costi 2026](https://www.emailvendorselection.com/email-marketing-cost-pricing-guide/)):

- **«pago anche i contatti che non uso»** — è la lamentela numero uno, e riguarda il modello a contatti
  archiviati: la bolletta cresce con la lista anche quando la lista è ferma. Conseguenza diretta sulla scelta
  della metrica (§3);
- **«il salto fra un piano e l'altro è brusco»** — il cliente supera di venti contatti una soglia e paga il
  doppio. Attenuazione possibile: piani larghi e pochi, non una scala fine;
- **«le automazioni avanzate hanno una curva di apprendimento ripida»** — chi è in due persone non costruisce un
  diagramma a rami. Conseguenza: l'epica 05 propone percorsi **lineari** a passi, non un editore a grafo;
- **cosa non chiedono**: punteggio dei contatti, previsioni, attribuzione multi-tocco. Sono le funzioni che
  vendono al segmento sopra il nostro e che qui restano fuori.

### 2.6 Fonti consultate

1. [mailerlite.com/pricing](https://www.mailerlite.com/pricing) — **pagina ufficiale**: gratuito 250 iscritti /
   2.500 email al mese / 2 posti; Comfort da 12 $, Power da 25 $ con email illimitate; annuale −10 %; il prezzo
   dipende «dal numero di iscritti e dal volume inviato».
2. [emailtooltester.com — Brevo pricing 2026](https://www.emailtooltester.com/en/reviews/brevo/pricing/) +
   [emailvendorselection.com — Brevo pricing](https://www.emailvendorselection.com/brevo-pricing/) — fonte
   editoriale: Brevo fa pagare **le email inviate**, non i contatti; gratuito 300 email al giorno; Starter da 9 $;
   le automazioni non sono nel piano d'ingresso. È il precedente europeo che rende difendibile una metrica a
   consumo.
3. [emailtooltester.com — Mailchimp pricing 2026](https://www.emailtooltester.com/en/reviews/mailchimp/pricing/) +
   [emailvendorselection.com](https://www.emailvendorselection.com/mailchimp-pricing/) — fonte editoriale:
   prezzo per contatti archiviati; taglio del piano gratuito a 250 contatti / 500 email al mese in gennaio 2026.
   Mi ha dato la direzione del mercato sul gratuito: si restringe.
4. [redsift.com — bulk email sender requirements 2026](https://redsift.com/guides/bulk-email-sender-requirements) +
   [powerdmarc.com](https://powerdmarc.com/bulk-email-sender-requirements/) — requisiti tecnici dei fornitori di
   posta: SPF + DKIM + DMARC allineati, disiscrizione in un clic RFC 8058 onorata entro 2 giorni, segnalazioni
   sotto lo 0,3 %, rifiuto al livello del protocollo per chi non rispetta. Da qui nascono le storie 0012, 0017,
   0021 e 0032.
5. [leggeinchiaro.it — art. 130 Codice privacy](https://leggeinchiaro.it/articolo-130-codice-privacy-comunicazioni-indesiderate-spam/) +
   [globalcom.it — soft spam solo email](https://globalcom.it/privacy/soft-spam-regole-dingaggio-senza-consenso-solo-email/) +
   [dgrs.it — analisi comparata sul soft spam](https://www.dgrs.it/lapplicazione-in-italia-della-disciplina-del-soft-spam-e-linterpretazione-dei-paesi-europei-unanalisi-comparata/) —
   consenso preventivo come regola, eccezione del soft spam limitata alla posta elettronica, ai prodotti analoghi
   e a una vendita effettiva. Da qui l'elenco chiuso delle basi giuridiche della storia 0007.
6. [osservatorio-dataprotection.it — double opt-in](https://www.osservatorio-dataprotection.it/data-protection/garante-privacy-double-opt-in-e-consenso-per-finalita-di-marketing/) +
   [Garante, provvedimento 25 settembre 2025 n. 10191282](https://www.garanteprivacy.it/home/docweb/-/docweb-display/docweb/10191282) +
   [tomshw.it — account non confermati esclusi dal marketing](https://www.tomshw.it/business/il-garante-multa-altroconsumo-gli-account-non-confermati-vanno-esclusi-dal-marketing) +
   [federprivacy.org — 45.000 € per email marketing senza consenso](https://www.federprivacy.org/informazione/garante-privacy/il-garante-sanziona-per-45mila-euro-una-societa-di-rivendita-auto-online-per-email-marketing-senza-consenso) —
   doppia conferma come misura minima, iscritti non confermati fuori dalle liste, prova non modificabile,
   responsabilità che non si scarica sul fornitore della lista. Sono le fondamenta dell'epica 02.
7. [smtpedia.com — Amazon SES pricing 2026](https://smtpedia.com/amazon-aws-ses-pricing/) — 0,10 $ per 1.000
   email in uscita, 24,95 $ al mese per un indirizzo dedicato. Serve a dimensionare il costo variabile del canale
   primario (§5). **La pagina ufficiale del fornitore non è risultata raggiungibile** durante l'analisi (§2.7).
8. [twilio.com — prezzi dei messaggi brevi verso l'Italia](https://www.twilio.com/en-us/sms/pricing/it) —
   **pagina ufficiale**: 0,0927 $ per messaggio in uscita verso l'Italia, più eventuali costi dell'operatore.
   È il numero che rende insostenibile rivendere il canale (§11.1).
9. [developers.facebook.com — prezzi della piattaforma di messaggistica per le imprese](https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing) —
   **pagina ufficiale**: dal 1° luglio 2025 si paga **a messaggio consegnato** (non più a conversazione), con
   tariffa che dipende dalla categoria del modello e dal prefisso del destinatario; i modelli di marketing si
   pagano sempre. Una fonte editoriale
   ([sendapp.live](https://sendapp.live/en/2025/08/04/how-much-does-whatsapp-business-api-cost-in-italy-complete-guide/))
   colloca il messaggio di marketing verso l'Italia attorno a 0,0572 €.

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzi ufficiali di Brevo e Mailchimp.** Le pagine ufficiali non si sono lasciate leggere (Brevo restituisce
  una pagina senza contenuto utile; per Mailchimp mi sono fermato alle fonti editoriali). I valori riportati al
  §2.1 vanno riverificati sui siti dei fornitori prima di fissare il posizionamento. *Cosa servirebbe*: una
  lettura manuale delle due pagine.
- **Prezzo ufficiale del servizio di invio di Amazon.** La pagina ufficiale non ha risposto; il valore di
  0,10 $/1.000 viene da una fonte editoriale ed è coerente fra più fonti, ma resta da confermare. *Cosa
  servirebbe*: la pagina dei prezzi del fornitore, o un fornitore europeo alternativo da valutare (§11.2).
- **Se la misurazione delle aperture e dei clic richieda un consenso proprio.** È un punto discusso: il pixel di
  apertura e il tracciamento dei clic sono trattamenti ulteriori rispetto all'invio, e non ho trovato una fonte
  che chiuda la questione per il caso della posta elettronica commerciale. *Cosa servirebbe*: una valutazione
  legale. Nel frattempo la storia 0029 li rende **facoltativi per campagna e spenti in partenza**, che è la
  scelta prudente.
- **Se una campagna di ri-richiesta del consenso sia lecita.** È la domanda naturale di chi ha una lista in
  quarantena: «posso mandare un messaggio per chiedere il consenso?». Quel messaggio è a sua volta una
  comunicazione senza consenso, e l'orientamento non è pacifico. *Cosa servirebbe*: revisione legale. La storia
  0010 **non** la implementa e dichiara il punto aperto invece di risolverlo a intuito.
- **Prezzi di Klaviyo** e di altri prodotti orientati al commercio elettronico: non rilevati, perché fuori dal
  segmento micro.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `campaigns` | Rispetta `^[a-z][a-z0-9_]{0,30}$`. Dice **cosa l'app è** — il posto da cui parte una campagna verso persone che hanno acconsentito — e non come la si vende oggi. Ho scartato `reach` perché è il nome commerciale e `marketing` perché è troppo largo: nel catalogo ci sono altre app di area marketing (17 RepGrove, recensioni) e un identificativo che se le prende tutte è un identificativo sbagliato. Da qui discendono lo schema `app_campaigns`, la rotta `/api/campaigns/v1/*` e l'etichetta del percorso end-to-end `[J-CAMPAIGNS]` |
| **Modello utente** | `multi` | Nel cliente tipo usa l'app **una** persona sola, e la tentazione di scegliere `single` è forte. Va respinta per due motivi concreti. Primo: la figura che manda le campagne è spesso **esterna** (un consulente, un'agenzia) e deve poter entrare senza usare le credenziali del titolare. Secondo, e decisivo: ogni invio è un atto di cui bisogna sapere **chi l'ha autorizzato** — un'app a utente singolo non ha il concetto di «chi ha fatto cosa», e qui quel concetto è la spina dorsale della prova (§2.3 punto 4) |
| **Porta locale** | `8116` | Convenzione del kit: 8100 + 16. Da confermare con `./dev.sh services` al momento dello scaffolding |
| **Metrica di quota** | `messages_sent` (invii al mese) | La sola cosa che il piano limita. È ciò che cresce col valore ricevuto — un cliente che manda di più sta usando l'app di più — ed è anche l'unica voce di **costo variabile** reale (§5). La scelta è deliberatamente contro il modello più diffuso (contatti archiviati, Mailchimp): quel modello genera la lamentela numero uno del segmento (§2.5), fa pagare le liste ferme e — cosa che qui conta di più — **premia chi accumula indirizzi**, cioè esattamente il comportamento che questa app esiste per scoraggiare. Il precedente europeo di Brevo (§2.1) dimostra che il modello a invii regge commercialmente. Un invio è un messaggio consegnato a un canale per un destinatario: 500 destinatari = 500 invii |
| **Natura della metrica** | `flow` | Consumo su una finestra che si azzera: «5.000 invii al mese» vuol dire che a marzo se ne possono fare altri 5.000 comunque sia andato febbraio. Gli iscritti archiviati **non** consumano quota: si può tenere una lista di ventimila persone e mandare due messaggi al mese. Contarla come giacenza sarebbe l'errore costoso descritto in [PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §7: bloccherebbe il cliente per sempre appena la lista cresce, che è il difetto che stiamo evitando |
| **Colore-categoria e icona** | `violet` · icona `send` (aeroplanino di carta) | Deve coincidere fra listino (`category`) e modulo frontend (`accentToken`). Ho **scartato `red`**, che pure sarebbe l'unico colore ancora libero fra le app di catalogo scritte: nel sistema di design il rosso è il colore del pericolo, e questa è l'app in cui un avviso rosso deve significare «questo invio è bloccato perché non è lecito». Un'app tutta rossa smetterebbe di comunicare proprio dove serve. `green` è di 07 BookGrove e dell'app reale `fatture`, `blue` di 04 LeadGrove e del mini-CRM, `teal` di 02 BillGrove e 12 DeskGrove, `amber` di 03 CashGrove e 08 SpendGrove. Resta `violet`, già proposto da 06 QuoteGrove e 13 FlowGrove: nessuna delle due è adiacente a ReachGrove nel percorso d'uso quotidiano, quindi la ripetizione è la meno dannosa possibile. Con sei colori e sessanta app la collisione è strutturale ed è un punto aperto di piattaforma (§11.7) |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Subscriber` | Una persona iscritta a una lista dell'account | indirizzo di posta, telefono (facoltativo), nome, lingua, stato (`in attesa di conferma`, `attivo`, `in quarantena`, `disiscritto`, `soppresso`) | **sì** — recapiti e nome |
| `ConsentRecord` | Una registrazione di consenso, **ad accrescimento** | canale, esito, base giuridica, momento, testo accettato, origine della prova, indirizzo di rete e momento della conferma | **sì** — è la prova |
| `Suppression` | Un recapito che non si può più usare, per sempre | recapito in forma cifrata, motivo (disiscrizione, rimbalzo permanente, segnalazione), momento | **sì** — un recapito |
| `Segment` | Un criterio salvato che seleziona iscritti | nome, criteri leggibili, conteggio all'ultimo calcolo | no (contiene criteri, non persone) |
| `MessageTemplate` | Un modello di messaggio riusabile | canale, oggetto, corpo a blocchi, campi variabili | no |
| `Campaign` | Un invio, programmato o partito | nome, canale, segmento, modello, stato (`bozza`, `in verifica`, `programmata`, `in corso`, `conclusa`, `bloccata`), momento previsto, chi l'ha autorizzata | no |
| `Delivery` | L'invio a **un** destinatario | riferimento a campagna e iscritto, stato, momento, identificativo presso il fornitore | **sì** — indirettamente, per riferimento |
| `DeliveryEvent` | Cosa è successo a quell'invio | tipo (consegnato, rimbalzo, segnalazione, apertura, clic), momento | **sì** — comportamento di una persona |
| `Automation` | Un percorso lineare a passi | evento d'avvio, passi (attesa, invio, condizione semplice), stato | no |
| `AutomationRun` | Il percorso di **un** iscritto dentro l'automazione | passo corrente, momento del prossimo passo, motivo di uscita | **sì** — per riferimento |
| `SubscriptionForm` | Un modulo o una pagina pubblica di iscrizione | campi, testo del consenso, collegamento all'informativa del cliente, chiave pubblica | no (il testo del consenso è configurazione) |
| `SenderDomain` | Un dominio mittente e il suo stato di autenticazione | dominio, esito di SPF/DKIM/DMARC, ultimo controllo | no |
| `ChannelConnection` | Il contratto che il cliente ha con un fornitore di canale | canale, riferimento cifrato alle credenziali, stato, tetto di spesa | no (credenziali, non dati di persone) |

**Relazioni.** Un `Subscriber` ha molti `ConsentRecord`: lo **stato attuale per canale** non è una colonna, è il
risultato dell'ultima registrazione — le registrazioni non si modificano mai. Una `Campaign` sceglie un `Segment`
e un `MessageTemplate` e genera molte `Delivery`, una per destinatario; ogni `Delivery` accumula
`DeliveryEvent`. Una `Suppression` **vince su tutto**: se il recapito è soppresso non nasce nessuna `Delivery`,
qualunque cosa dica il consenso.

La macchina a stati della campagna, che tutte le storie dell'epica 04 devono rispettare:

```
bozza ──▶ in verifica ──▶ programmata ──▶ in corso ──▶ conclusa
             │                                │
             └──▶ bloccata ◀──────────────────┘
```

Il passaggio `in verifica → programmata` avviene **solo** se il controllo pre-volo è verde (storia 0018). Da
`bloccata` si torna indietro a `bozza`: non esiste nessuna transizione che scavalchi la verifica.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_campaigns`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8). Eccezione motivata su `ConsentRecord` e `Suppression`:
la cancellazione logica lì si usa **solo** per l'esercizio dei diritti dell'interessato, mai per correggere una
registrazione.

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/campaigns.yaml`.

**Ragionamento.** Tre vincoli tengono insieme i numeri.

1. **Il costo variabile è basso ma non nullo.** A 0,10 $ per 1.000 email (§2.6 fonte 7), 25.000 invii al mese
   costano circa 2,50 $ di sola consegna. Il costo che conta davvero non è quello: è l'eventuale **indirizzo di
   invio dedicato**, 24,95 $ al mese, che però è un costo di piattaforma condiviso da tutti gli account, non
   per-cliente. Il margine sul canale primario regge comodamente.
2. **Il mercato è a 9-29 $/mese** (§2.2) e la scheda di catalogo indica 15-49 €/mese. La proposta sta nel mezzo,
   con due piani a pagamento invece di tre: aggiungerne è facile, toglierne è difficile.
3. **La prova gratuita a tempo qui non serve**, perché c'è un piano gratuito permanente. Proporla sarebbe la
   ridondanza di cui avverte il modello. La eccezione è il piano alto, dove i canali aggiuntivi meritano di
   essere provati.

| Piano | Prezzo mensile | Prezzo annuale | Limite su `messages_sent` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 500 invii/mese, solo posta elettronica, un dominio mittente | — | Chi vuole vedere se il proprio archivio è in ordine e mandare il primo messaggio vero. Sopra i 500 invii si blocca, non si addebita |
| `base` | 15 € | 150 € (10× il mensile, «due mesi in regalo») | 5.000 invii/mese | — (c'è già il gratuito) | La micro-impresa con qualche centinaio di iscritti che manda due o quattro messaggi al mese |
| `pro` | 39 € | 390 € | 25.000 invii/mese, automazioni, prova a due varianti, canali aggiuntivi col contratto del cliente | 14 giorni | Chi manda con regolarità, ha più liste e vuole i percorsi automatici |

**Note obbligate.**

- Due piani a pagamento, come raccomandato. Le automazioni stanno solo nel piano alto: è la stessa linea di
  separazione che usa Brevo (§2.1), quindi difendibile nel confronto.
- Il limite lasciato vuoto significherebbe **illimitato**: qui non ce ne sono: tutti e tre i piani hanno un tetto.
- **Gli iscritti archiviati non hanno tetto in nessun piano.** È la scelta di posizionamento più importante del
  listino ed è deliberata (§3): il concorrente fa pagare la lista ferma, noi facciamo pagare l'uso.
- **Costo effettivo dell'incasso**: nessun piano sta sotto i 5 €/mese, quindi la parte fissa per transazione non è
  un problema. Il gratuito non genera incassi e va dimensionato per quello: 500 invii al mese è deliberatamente
  poco — abbastanza per il primo messaggio, non per viverci.
- I prezzi sono **immutabili una volta vivi**: un cambio si fa creando un prezzo nuovo, non modificando quello
  esistente.
- **Punto che va deciso insieme al prezzo**: sul canale aggiuntivo (messaggi brevi, messaggistica) l'invio lo paga
  il cliente al **suo** fornitore, ma consuma comunque la nostra quota, perché il lavoro di segmentazione, di
  controllo del consenso e di tracciamento lo facciamo noi. È coerente, ma va spiegato bene nel listino,
  altrimenti sembra un doppio addebito.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/campaigns.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

**Categorie particolari (articolo 9): no, per costruzione — ma con due avvertenze da non minimizzare.**
L'app non chiede né archivia dati sulla salute, biometrici, genetici, opinioni politiche, convinzioni religiose,
orientamento sessuale o appartenenza sindacale. Però:

1. **il cliente può crearne senza accorgersene.** Un campo personalizzato «interessi» o un segmento chiamato
   «iscritti al gruppo diabete» trasformano dati banali in dati particolari, e in certi casi è la
   **lista stessa** a rivelare la categoria: essere iscritto alla newsletter di un sindacato o di una confessione
   religiosa è, di per sé, un dato particolare. Non è un rischio teorico ed è fuori dal nostro controllo tecnico;
2. **la misurazione del comportamento** (chi ha aperto, chi ha cliccato quale collegamento) può rivelare
   interessi sensibili in modo indiretto. È la ragione per cui la storia 0029 la rende facoltativa e spenta in
   partenza.

*Proposta*: nessuna rilevazione automatica del contenuto (non sapremmo farla e sarebbe a sua volta un
trattamento), ma un avviso esplicito nell'interfaccia al momento di creare un campo personalizzato o un segmento,
e la questione portata alla revisione legale. **Non è una decisione di questo documento** (§11.6).

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `subscriber.email` | `subscriber.email` | iscritto (persona che ha acconsentito) | contatto | recapitare la comunicazione commerciale | esecuzione del contratto col nostro cliente; verso l'interessato, consenso raccolto dal cliente | fino alla cancellazione dell'iscritto o dell'account |
| `subscriber.phone` | `subscriber.phone` | iscritto | contatto | recapitare su canale aggiuntivo | come sopra | come sopra |
| `subscriber.name` | `subscriber.first_name`, `last_name` | iscritto | anagrafica | personalizzare il messaggio | come sopra | come sopra |
| `subscriber.custom_fields` | `subscriber.custom_fields` (documento libero) | iscritto | **variabile, definita dal cliente** | segmentazione | come sopra | come sopra — *voce a rischio, vedi avvertenza 1* |
| `consent.record` | `consent_record.*` | iscritto | prova del consenso (momento, testo, origine, indirizzo di rete della conferma) | dimostrare la liceità dell'invio | obbligo di dimostrabilità in capo al titolare | **sopravvive alla revoca**; proposta: 10 anni dall'ultimo invio, da validare (§2.7) |
| `suppression.contact` | `suppression.contact_hash` + recapito cifrato | ex iscritto / segnalante | contatto | impedire per sempre nuovi invii allo stesso recapito | obbligo di onorare l'opposizione | **permanente**: cancellarla riaprirebbe la porta all'invio |
| `delivery.*` | `delivery.*` | iscritto | metadato di recapito | sapere se il messaggio è arrivato | esecuzione del contratto | proposta: 24 mesi |
| `delivery_event.open`, `.click` | `delivery_event.*` | iscritto | comportamento | misurare il rendimento | **da decidere**: consenso o legittimo interesse (§2.7) | proposta: 12 mesi, facoltativa |
| `automation_run.*` | `automation_run.*` | iscritto | stato del percorso | sapere a che punto è | esecuzione del contratto | fino a conclusione + 12 mesi |
| `form_submission.payload` | `form_submission.payload` | chiunque compili il modulo pubblico | dato grezzo | ricostruire un'iscrizione contestata | prova | proposta: 24 mesi |
| `import_row.payload` | `import_row.payload` | contatti importati | dato grezzo | dimostrare cosa è stato caricato e da chi | prova | proposta: 24 mesi |

**Esportazione e cancellazione.** Tutte e tredici le tabelle del §4 contengono dati di persone o vi puntano, e
**tutte** devono comparire sia in `exportData` sia in `purgeData` del contratto dati dell'app: `subscriber`,
`consent_record`, `suppression`, `segment` (solo se il criterio nomina una persona), `campaign`, `delivery`,
`delivery_event`, `automation_run`, `form_submission`, `import_row`, più le tabelle di appoggio. Due note che non
sono dettagli:

- la cancellazione è **fisica**: sostituire l'indirizzo con un codice non è cancellare;
- la `suppression` è il caso limite del catalogo. Cancellarla su richiesta dell'interessato **riaprirebbe la
  porta agli invii verso di lui**, cioè produrrebbe l'effetto opposto a quello voluto. *Proposta*: conservare
  la sola impronta crittografica non reversibile del recapito, che serve a bloccare e non a contattare, e
  dichiararlo nell'informativa. È una scelta che va validata, non data per buona (§11.6).

**Testo libero.** L'app ha campi liberi in tre punti: i campi personalizzati dell'iscritto, il corpo del
messaggio e il nome del segmento. Sono ingressi non presidiati per categorie particolari (avvertenza 1).
L'app non fa rilevazione di contenuto; il presidio, se servirà, è un tema trasversale.

**Integrazioni esterne.** Tre, tutte da elencare fra i fornitori che trattano dati per nostro conto o per conto
del cliente:

1. **il fornitore di consegna della posta elettronica** — riceve indirizzo e contenuto di ogni messaggio. È
   inevitabile e va scelto con i dati a riposo in Unione europea ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §10);
   se il fornitore proposto non lo garantisce, la scelta va rifatta (§11.2);
2. **il fornitore di messaggi brevi**, quando l'account attiva il canale: riceve il numero di telefono. Il
   contratto è del cliente, ma il dato lo trasmettiamo noi;
3. **il fornitore di messaggistica**, quando l'account attiva il canale: riceve il numero di telefono, ed è
   **fuori dall'Unione europea**. Il fatto che il contratto sia del cliente **non fa sparire il trasferimento**:
   sposta chi ne risponde, non se avviene. È il punto aperto più serio dell'app (§11.1) e la ragione per cui la
   storia 0024 è espressamente subordinata a una revisione legale.

**Classificazione della change.** Una app nuova che tratta recapiti di persone che **non sono clienti nostri né,
spesso, del nostro cliente** introduce finalità e categorie nuove: è un cambiamento **sostanziale**, senza
attenuanti. Va aggiornata la valutazione dei rischi, e la questione del trasferimento verso paesi terzi (punto 3)
va chiusa **prima** che il canale di messaggistica venga acceso, non dopo.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_campagne` | `(stato?, canale?, periodo?) → elenco minimizzato di campagne` | nome, stato, canale, destinatari, momento | lettura | no |
| `statistiche_campagna` | `(id_campagna) → conteggi di recapito e rendimento` | consegnati, rimbalzi, segnalazioni, aperture e clic **se misurati** | lettura | no |
| `stato_iscritto` | `(recapito) → contattabile sì/no, canale per canale, con il motivo` | risponde alla domanda «posso scrivergli?» citando la registrazione che lo determina | lettura | no |
| `elenca_segmenti` | `(?) → segmenti con criteri leggibili e conteggio` | — | lettura | no |
| `salute_della_lista` | `(?) → tasso di segnalazione, rimbalzi, inattivi, iscritti in quarantena` | il cruscotto di rischio, in una frase | lettura | no |
| `genera_testo` | `(obiettivo, tono, lingua, riferimenti?) → bozza di testo` | scrive; non salva niente da solo | scrittura | **sì** (per salvarla) |
| `crea_bozza_di_campagna` | `(nome, segmento, modello o testo, canale) → campagna in stato bozza` | crea una campagna che **non** può partire da sola | scrittura | **sì** |
| `programma_invio` | `(id_campagna, momento) → esito del controllo pre-volo` | non programma: **esegue la verifica e restituisce il risultato**, poi chiede | scrittura | **sì, obbligatoria** |
| `disiscrivi` | `(recapito, motivo) → registrazione di revoca` | onora un'opposizione arrivata a voce o per altra via | scrittura irreversibile | **sì** |

**Non esposti alla chat, con motivazione scritta**: `registra_consenso` e `importa_lista`. Il primo è una
dichiarazione con valore probatorio e dev'essere un atto compiuto da una persona nell'interfaccia, dove vede
esattamente cosa sta dichiarando (stessa scelta dell'app 04, storia 0011); il secondo perché un'importazione fatta
«a voce» è precisamente il modo in cui nascono le liste di cui nessuno sa più l'origine.

**Riga di lettura.** Lo strumento che giustifica il livello conversazionale in questa app è `stato_iscritto`: la
domanda «posso scrivere a questa persona?» ha una risposta che sta in un registro e che nessun assistente generico
può dare. Il secondo è `salute_della_lista`, perché il tasso di segnalazione sotto lo 0,3 % (§2.3 punto 5) è un
numero che il cliente non guarderà mai spontaneamente e che gli costa l'account quando lo supera.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine l'app esiste, è vuota, si avvia in locale, compare nella barra laterale a chi è abilitato e blocca a
`429` quando la quota di invii è finita.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Il servizio `campaigns` nasce dallo scaffolding, risponde su `/api/campaigns/v1/*` e ha la sua istanza di infrastruttura |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Le tredici tabelle sullo schema `app_campaigns`, con `tenant_id`, colonne di controllo e cancellazione logica |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Il modulo compare nella barra laterale con le sue sezioni, in cinque lingue e nei due temi |
| [0004](01-fondamenta/0004-abbonamento-e-quota-degli-invii.md) | Abbonamento e quota degli invii | La catena dei varchi fino al `429` sulla metrica `messages_sent` |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` la vede, `./app-start.sh` la avvia, un comando la riempie di dati inventati |

### Epica 02 — Pubblico e prova del consenso

È la fondazione vera dell'app: alla fine dell'epica esiste un archivio di iscritti in cui **nessuno è
contattabile senza una prova**, e l'unico modo per entrarci è passare da una porta che chiede quella prova.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-pubblico-e-prova-del-consenso/0006-anagrafica-degli-iscritti.md) | Anagrafica degli iscritti | La scheda dell'iscritto con i recapiti, la lingua e lo stato — e nessuno stato «attivo» che si possa impostare a mano |
| [0007](02-pubblico-e-prova-del-consenso/0007-registro-del-consenso.md) | Registro del consenso | Registrazioni ad accrescimento con canale, base giuridica, momento, testo accettato e origine: la prova |
| [0008](02-pubblico-e-prova-del-consenso/0008-iscrizione-con-doppia-conferma.md) | Iscrizione con doppia conferma | Chi si iscrive riceve un messaggio e conferma: prima della conferma non è inviabile |
| [0009](02-pubblico-e-prova-del-consenso/0009-modulo-pubblico-di-iscrizione.md) | Modulo pubblico di iscrizione | Il modulo da incorporare nel sito del cliente, con l'informativa e il testo del consenso obbligatori |
| [0010](02-pubblico-e-prova-del-consenso/0010-importazione-di-liste-in-quarantena.md) | Importazione di liste in quarantena | Una lista importata senza prova entra **non inviabile** e non c'è nessun pulsante per forzarla |
| [0011](02-pubblico-e-prova-del-consenso/0011-elenco-di-soppressione.md) | Elenco di soppressione | Disiscritti, rimbalzi permanenti e segnalazioni: un elenco che vince su tutto e che il cliente non può svuotare |
| [0012](02-pubblico-e-prova-del-consenso/0012-disiscrizione-in-un-clic.md) | Disiscrizione in un clic | Collegamento e intestazione RFC 8058 in ogni messaggio, disiscrizione registrata subito e senza domande |

### Epica 03 — Contenuti e segmenti

Alla fine l'utente sa comporre un messaggio, vederlo come lo vedrà il destinatario e scegliere a chi va.

| # | Storia | In una riga |
|---|---|---|
| [0013](03-contenuti-e-segmenti/0013-segmenti-salvati.md) | Segmenti salvati | Criteri leggibili salvati con un nome, ricalcolati al momento dell'invio |
| [0014](03-contenuti-e-segmenti/0014-composizione-del-messaggio.md) | Composizione del messaggio | Un editore a blocchi che produce un messaggio leggibile anche in solo testo |
| [0015](03-contenuti-e-segmenti/0015-campi-variabili-e-anteprima.md) | Campi variabili e anteprima | Personalizzazione con valore di ripiego e anteprima su un destinatario reale |
| [0016](03-contenuti-e-segmenti/0016-modelli-riusabili-e-duplicazione.md) | Modelli riusabili e duplicazione | Salvare un messaggio come modello e ripartire da una campagna già fatta |

### Epica 04 — Spedizione e canali

Alla fine un messaggio parte davvero, solo se è lecito e solo verso chi si può, sul canale scelto — e sui canali
a pagamento parte con le credenziali del cliente, non con le nostre.

| # | Storia | In una riga |
|---|---|---|
| [0017](04-spedizione-e-canali/0017-verifica-del-dominio-mittente.md) | Verifica del dominio mittente | Nessun invio da un dominio che non passa SPF, DKIM e DMARC allineati |
| [0018](04-spedizione-e-canali/0018-controllo-pre-volo.md) | Controllo pre-volo | Una campagna non lascia lo stato di verifica finché ogni controllo bloccante non è verde |
| [0019](04-spedizione-e-canali/0019-spedizione-programmata-della-campagna.md) | Spedizione programmata della campagna | La coda che consegna, con ripresa dopo un guasto e senza doppioni |
| [0020](04-spedizione-e-canali/0020-disiscrizione-onorata-durante-la-spedizione.md) | Disiscrizione onorata durante la spedizione | Chi si disiscrive mentre la campagna è in corso non riceve: il controllo è al momento dell'invio |
| [0021](04-spedizione-e-canali/0021-rimbalzi-e-segnalazioni.md) | Rimbalzi e segnalazioni | Ritorni del fornitore che alimentano la soppressione e il tasso di segnalazione, con blocco sopra lo 0,3 % |
| [0022](04-spedizione-e-canali/0022-canali-aggiuntivi-e-tetto-di-spesa.md) | Canali aggiuntivi e tetto di spesa | L'astrazione del canale, l'attivazione per account e il tetto di spesa che il cliente si dà |
| [0023](04-spedizione-e-canali/0023-contratto-del-cliente-per-i-messaggi-brevi.md) | Contratto del cliente per i messaggi brevi | Il cliente collega le proprie credenziali: paga il suo fornitore, non noi |
| [0024](04-spedizione-e-canali/0024-contratto-del-cliente-per-la-messaggistica.md) | Contratto del cliente per la messaggistica | Come sopra, con i modelli approvati dal fornitore e l'avviso sul trasferimento fuori dall'Unione europea |

### Epica 05 — Automazioni

Alla fine esistono percorsi lineari che partono da un evento, aspettano e mandano — e che si fermano da soli
quando la persona esce.

| # | Storia | In una riga |
|---|---|---|
| [0025](05-automazioni/0025-percorso-automatico-a-passi.md) | Percorso automatico a passi | Una sequenza lineare di passi «aspetta / manda», definita e attivabile |
| [0026](05-automazioni/0026-avvio-da-evento.md) | Avvio da evento | L'iscrizione confermata, l'ingresso in un segmento e una data ricorrente fanno partire il percorso |
| [0027](05-automazioni/0027-uscita-e-sospensione-del-percorso.md) | Uscita e sospensione del percorso | Disiscrizione, soppressione e quota esaurita fermano il percorso, con il motivo scritto |
| [0028](05-automazioni/0028-registro-delle-esecuzioni.md) | Registro delle esecuzioni | Chi è dentro, a che passo, cosa è fallito e perché |

### Epica 06 — Rendimento e salute della lista

Alla fine il cliente sa se il messaggio è arrivato, cosa ha prodotto e se la sua lista lo sta portando verso il
blocco.

| # | Storia | In una riga |
|---|---|---|
| [0029](06-rendimento-e-salute-della-lista/0029-misurazione-facoltativa-di-aperture-e-clic.md) | Misurazione facoltativa di aperture e clic | Spenta in partenza, attivabile per campagna, dichiarata nell'informativa del cliente |
| [0030](06-rendimento-e-salute-della-lista/0030-rapporto-della-campagna.md) | Rapporto della campagna | Recapito, rimbalzi, segnalazioni e — se misurati — aperture e clic |
| [0031](06-rendimento-e-salute-della-lista/0031-prova-a-due-varianti.md) | Prova a due varianti | Due oggetti su due porzioni del segmento, con la lettura del risultato |
| [0032](06-rendimento-e-salute-della-lista/0032-salute-della-lista.md) | Salute della lista | Inattivi, tasso di segnalazione contro la soglia, iscritti in quarantena: il cruscotto del rischio |
| [0033](06-rendimento-e-salute-della-lista/0033-esportazione-dei-rapporti.md) | Esportazione dei rapporti | I numeri escono in un file, con l'avvertenza su cosa il cliente può farne |

### Epica 07 — Esposizione conversazionale e prove end-to-end

Alla fine l'app è comandabile da una chat con la regola «lettura libera, scrittura con conferma», e il percorso
end-to-end `[J-CAMPAIGNS]` copre la catena consenso → campagna → invio → disiscrizione.

| # | Storia | In una riga |
|---|---|---|
| [0034](07-esposizione-conversazionale-e-prove/0034-contratto-degli-strumenti-di-lettura.md) | Contratto degli strumenti di lettura | I cinque strumenti di sola lettura, con schemi e dati minimizzati |
| [0035](07-esposizione-conversazionale-e-prove/0035-strumenti-di-scrittura-con-bozza-e-conferma.md) | Strumenti di scrittura con bozza e conferma | Creare una bozza e disiscrivere: mai un effetto senza un sì esplicito |
| [0036](07-esposizione-conversazionale-e-prove/0036-generazione-assistita-del-testo.md) | Generazione assistita del testo | La bozza del testo la scrive la chat; salvarla resta un atto di una persona |
| [0037](07-esposizione-conversazionale-e-prove/0037-percorso-end-to-end-dell-app.md) | Percorso end-to-end dell'app | `[J-CAMPAIGNS]` sullo stack locale reale, più le voci del registro di copertura |

**Totale**: 7 epiche, 37 storie.

---

## 9. Estensioni della console di amministrazione

Servono estensioni, e non sono cosmetiche: la reputazione di invio è **condivisa fra tutti gli account**, quindi
chi amministra la piattaforma deve poter vedere il tasso di segnalazione e di rimbalzo per account e sospendere
l'invio di un singolo cliente prima che bruci il recapito di tutti. Servono inoltre la diagnostica dei domini
mittenti e lo stato dei canali collegati dal cliente, che sono la causa più frequente di «non parte niente».

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

Il catalogo (§6) mette l'**anagrafica clienti condivisa** al centro della suite: «la stessa scheda cliente alimenta
CRM (4), fatturazione (2, 1), incasso crediti (3), supporto (12), prenotazioni (7) e tutti i verticali».
ReachGrove tocca quell'anagrafica ma **non ne è il sistema di origine**: lo è LeadGrove (04).

**Il confine, detto in una riga: LeadGrove possiede la scheda della persona, ReachGrove possiede l'iscrizione e
l'invio.** In pratica:

| Cosa | Chi la possiede | Perché |
|---|---|---|
| La scheda del contatto e dell'azienda, la trattativa, lo storico della relazione | **04 LeadGrove** | È il sistema di origine dell'anagrafica |
| La prova del consenso raccolta **durante la relazione commerciale** (l'operatore che registra «mi ha detto di sì al telefono») | **04 LeadGrove** (storia 0011 di quell'app) | Nasce lì, dove avviene il contatto |
| L'iscrizione a una lista, la prova del consenso raccolta **dal modulo pubblico**, la soppressione, l'invio | **16 ReachGrove** | Nasce qui |
| Lo stato «contattabile o no», visto dal venditore | 04 lo mostra, 16 lo aggiorna | La disiscrizione avviene qui e deve tornare là, altrimenti il venditore richiama qualcuno che si è appena opposto |

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| 04 — LeadGrove | **si arricchisce da / alimenta** | I contatti e le loro prove di consenso arrivano da lì; le disiscrizioni tornano là. È la sinergia principale dell'app |
| 12 — DeskGrove (assistenza) | condivide | Chi risponde a una campagna spesso apre un ticket: stessa persona, altra app |
| 17 — RepGrove (recensioni) | **si sovrappone al bordo** | Anche RepGrove manda un messaggio dopo il servizio. Confine proposto: RepGrove manda **transazionali** legati a una prestazione, ReachGrove manda **comunicazioni commerciali**. La differenza non è tecnica, è la base giuridica |
| 07 — BookGrove (prenotazioni) | alimenta | Un promemoria di appuntamento è transazionale e resta a BookGrove; ma il cliente che prenota è un potenziale iscritto |
| 05 — ChatGrove | **non esiste più nel catalogo attivo** | Era l'app del canale di messaggistica ed è stata esclusa (§11.1). Il suo caso d'uso conversazionale non viene assorbito qui: qui c'è solo l'invio, non la conversazione |
| 29 — ShopGrove | **esclusa dal catalogo** | Era la sorgente naturale degli eventi del carrello abbandonato. Senza di essa quel caso d'uso non ha da dove partire (§11.4) |

**Come si condivide, tecnicamente.** Non con chiamate: una app **non chiama** un'altra app
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §2). ReachGrove tiene una **proiezione locale** alimentata
dagli eventi «contatto creato/aggiornato» e «preferenza di contatto registrata» pubblicati da LeadGrove, e pubblica
a sua volta «iscritto disiscritto» e «recapito soppresso». Regola che non si negozia e che vale anche in ingresso:

> **un contatto che arriva per proiezione senza la sua prova di consenso entra in quarantena** (storia 0010),
> esattamente come una riga importata da un file. La provenienza interna non è una prova.

Il contratto di quegli eventi — chi è la fonte di verità, come si risolvono i conflitti, cosa propaga una
cancellazione — **non esiste ancora** nel repository ed è la stessa lacuna già segnalata da LeadGrove (§11.5).

**Sovrapposizioni da evitare.**

1. **la scheda del cliente**: se ReachGrove cominciasse a tenere aziende, trattative e note, diventerebbe un
   archivio commerciale e le due app diventerebbero una. Qui c'è l'iscritto, non il cliente;
2. **il messaggio transazionale**: la conferma di una prenotazione, il promemoria di un appuntamento, la fattura
   inviata per posta elettronica **non passano da qui**. Sono comunicazioni di servizio delle rispettive app, con
   un'altra base giuridica, e farle passare da uno strumento di marketing significherebbe sottoporle a una
   disiscrizione che non devono avere;
3. **la conversazione in entrata**: una risposta a un messaggio non apre niente qui. È l'app 12.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **I canali a pagamento di fornitori extra-europei: il contratto lo porta il cliente.** La scheda di catalogo dà a ReachGrove tre canali; il catalogo però ha **escluso l'app 05 ChatGrove** perché «il canale di messaggistica appartiene a un fornitore extra-europeo: trasferimento verso paesi terzi, regole del fornitore, e un costo variabile per conversazione che erode il margine» ([_escluse/README.md](../_escluse/README.md)). Le tre ragioni valgono identiche qui, e i numeri lo confermano: 0,0927 $ per messaggio breve verso l'Italia (§2.6 fonte 8) contro 0,0001 $ per una email — mille volte tanto — e un messaggio di marketing di messaggistica attorno a 0,057 € (fonte 9). Rivenderli dentro un piano da 39 €/mese significa che 700 messaggi bruciano l'intero canone. **La proposta di questo documento** è quindi: posta elettronica come canale primario nostro; messaggi brevi e messaggistica come canali **facoltativi che l'account attiva collegando il proprio contratto** col fornitore (storie 0022-0024). Così cadono due delle tre ragioni dell'esclusione — il costo variabile non è nostro, le regole del fornitore le accetta chi firma con lui. **La terza non cade**: il trasferimento di un numero di telefono verso un paese terzo avviene comunque, e lo effettuiamo materialmente noi per conto del cliente. Chi ne risponde e a che titolo è una domanda che non chiudo qui | **Sviluppatore + revisione legale**. Fino a quel momento la storia 0024 resta scritta e non implementata. L'app **non perde la sua ragione d'essere**: la posta elettronica è il canale che il cliente tipo usa davvero e le prime sei epiche stanno in piedi da sole |
| 2 | **Scelta del fornitore di consegna della posta.** L'unico prezzo che ho potuto verificare è quello di un fornitore statunitense, con la regione europea disponibile ma la casa madre fuori. La piattaforma vuole i dati a riposo in Unione europea e la memoria di progetto preferisce i fornitori europei. Non ho valutato alternative europee | **Sviluppatore** — è insieme una scelta di fornitore, di costo e di conformità |
| 3 | **Il modello di prezzo a invii contro il modello a contatti.** Il mercato è spaccato (§2.2) e la proposta prende la strada meno battuta. Il rischio non è il margine: è la **comparabilità**, perché il cliente arriva sapendo confrontare «quanti contatti posso avere» | **Sviluppatore** (fermata di escalation prezzi) |
| 4 | **Il carrello abbandonato non ha da dove partire.** È uno dei casi d'uso della scheda di catalogo, e richiede eventi da un negozio in rete: 29 ShopGrove è fra le escluse e nessun'altra app del catalogo attivo li produce. Le vie: rinunciarci (proposta di questo documento), oppure aprire un ingresso di eventi generico per sistemi esterni — che è un'epica in più e un fornitore esterno in più | **Sviluppatore** — decisione di prodotto |
| 5 | **Contratto degli eventi dell'anagrafica condivisa.** La sinergia del §10 richiede di sapere chi è la fonte di verità di un contatto modificato da due app e cosa propaga una cancellazione. Oggi non esiste nel repository | **Piattaforma** — è un use case di architettura, non di questa app |
| 6 | **Tre classificazioni di dati personali che non decido**: (a) se aperture e clic richiedano un consenso proprio (§2.7); (b) come conservare la soppressione senza che cancellarla riapra la porta agli invii (§6); (c) i termini di conservazione proposti, nessuno dei quali ha un fondamento di legge che io abbia trovato | **Sviluppatore**, in sede di compilazione del manifesto, con la revisione legale |
| 7 | **Colore-categoria `violet` già proposto da 06 QuoteGrove e 13 FlowGrove.** Sei colori per sessanta app: la collisione è strutturale. Ho scelto il male minore spiegandolo al §3 | **Piattaforma**, quando i colori si assegnano sul serio |
| 8 | **Campagna di ri-richiesta del consenso**: non implementata, perché non ho trovato una fonte che ne chiarisca la liceità (§2.7). Chi importa una lista in quarantena resta senza una via d'uscita che non sia allegare la prova | **Revisione legale** |

**Rischi noti**

- **La reputazione di invio è condivisa fra tutti gli account.** È il rischio numero uno e non è di questa app,
  è della piattaforma: un solo cliente che manda a una lista comprata fa salire il tasso di segnalazione
  dell'indirizzo di invio, e i messaggi di **tutti** cominciano a essere respinti (§2.3 punto 5). Non è un rischio
  teorico: è il modo tipico in cui un servizio di invio giovane muore. *Cosa lo attenua*: quarantena delle liste
  senza prova (0010), controllo pre-volo bloccante (0018), sorveglianza del tasso di segnalazione per account con
  blocco automatico sopra lo 0,3 % (0021), sospensione dalla console di amministrazione
  ([estensioni-admin.md](estensioni-admin.md)). *Cosa non lo attenua*: sperare che i clienti si comportino bene.
- **Il piano gratuito è la porta d'ingresso di chi vuole spedire a liste comprate.** Un servizio di invio gratuito
  attira esattamente il cliente che non vogliamo. *Cosa lo attenua*: il gratuito richiede comunque un dominio
  mittente verificato (che costa fatica e presuppone un dominio proprio) e ha 500 invii al mese, che non
  interessano a chi fa invii massivi.
- **Categoria vecchia con concorrenti europei forti.** Brevo fa già tutto questo, in Europa, a 9 $. *Cosa lo
  attenua*: non competere sulle funzioni ma sull'appartenenza alla suite (§10) e sulla conformità come funzione
  del prodotto, che è l'unico punto in cui i quattro concorrenti si somigliano tutti (§2.1).
- **Il consenso rigoroso rallenta il primo utilizzo.** Chi arriva con un file di 3.000 indirizzi e si sente dire
  «sono in quarantena» può andarsene. È una scelta consapevole: è il momento in cui il prodotto dice cosa è.
  *Cosa lo attenua*: spiegarlo prima dell'importazione, non dopo, e rendere semplice allegare la prova quando
  esiste (0010).

**Fuori dimensionamento**: non applicabile. 7 epiche (fascia 4-7), da 4 a 8 storie ciascuna (fascia 4-8),
37 storie in tutto (fascia 20-45).
