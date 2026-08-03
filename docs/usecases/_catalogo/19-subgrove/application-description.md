# SubGrove — descrizione dell'applicazione

**Numero di catalogo**: 19 · **Tipo**: orizzontale · finance · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 19](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** SubGrove tiene in ordine gli abbonamenti che il **cliente** ha con i **suoi** clienti: i piani che
vende, chi è abbonato a quale piano, quando scade il periodo in corso, quanto è dovuto, se è stato incassato e
cosa succede quando non lo è. Produce tre cose concrete: il **calendario delle scadenze ricorrenti** (chi paga
cosa e quando), la **catena dei solleciti** su ciò che non è rientrato, e la **misura dei ricavi ricorrenti** —
quanto entra ogni mese, quanto se n'è aggiunto, quanto se n'è perso e per quale motivo.

**Per chi.** Micro-impresa (1-10 addetti) e piccola impresa (10-50) che vende **a canone** e non a
prestazione singola: palestre e centri sportivi, scuole di lingua e di musica, studi di consulenza con contratti
di assistenza, manutentori con contratti annuali, service informatici locali, associazioni con quote sociali,
piccoli editori con abbonamenti. Compra il titolare; usa tutti i giorni chi sta alla reception o
all'amministrazione — la persona che oggi apre il foglio di calcolo per capire «chi non ha pagato».

**Quale problema toglie.** Oggi il ricorrente si governa a memoria e a colonne: un foglio con i nomi, la data di
inizio, il piano e una casella colorata per il mese pagato. Costa in tre modi. Primo, si **dimenticano i
rinnovi**: nessuno avvisa l'abbonato che fra trenta giorni parte un altro anno, e quando l'addebito arriva a
sorpresa nasce la contestazione. Secondo, si **perde denaro senza accorgersene**: una carta scaduta o un addebito
rifiutato non lascia traccia nel foglio, e l'abbonato continua a usare il servizio gratis per mesi. Terzo, non si
sa **quanto vale l'attività**: alla domanda «quanto ti entra ogni mese, al netto di chi se n'è andato?» il foglio
non risponde, perché non tiene lo storico.

**Cosa NON fa.**

- **Non incassa denaro dei clienti del nostro cliente.** SubGrove registra ciò che è dovuto e ciò che è
  rientrato; l'incasso avviene **fuori**, con lo strumento che l'attività già usa (bonifico, addebito diretto
  presso la propria banca, il proprio conto presso un fornitore di pagamento, contanti al banco). Il denaro non
  transita mai da appgrove, e appgrove non lo dispone mai per conto altrui. Il perché sta al §5 e al §6, ed è la
  stessa postura già scelta per l'app **06 QuoteGrove** e per l'app **07 BookGrove**.
- **Non emette la fattura fiscale.** La scadenza ricorrente di SubGrove è un **avviso di addebito** interno, non
  un documento fiscale: il documento lo emette **02 BillGrove** (e, dove serve la trasmissione a norma, il suo
  strato di conformità). Se BillGrove non c'è, SubGrove esporta la scadenza in un formato tabellare e il cliente
  fattura come faceva prima.
- **Non gestisce l'accesso fisico** (tornelli, tessere, badge), **non gestisce i corsi e le presenze**, **non
  conserva certificati medici**: sono altri mestieri, e l'ultimo è un mestiere che tocca dati sulla salute
  (vedi l'avviso in testa al §6).
- **Non fa dunning con carte proprie**: SubGrove non conserva numeri di carta né coordinate bancarie; conserva
  al più il **riferimento** di un mandato o di un'autorizzazione che vive presso la banca o il fornitore del
  cliente (storia `0017`).
- **Non è il sistema di abbonamento di appgrove verso i propri clienti**: quello esiste già ed è di piattaforma.
  Il confine è al §10.1, ed è il punto più delicato di tutto il documento.

**Rischio di sostituzione da parte dei modelli linguistici.** `neutra`, come nel catalogo. Un assistente generico
sa spiegare che cos'è il tasso di abbandono e sa persino calcolarlo se gli si incolla una tabella; non può
**tenere lo stato nel tempo** — la data di rinnovo che scatta stanotte, il sollecito che non è ancora partito, il
periodo che va sospeso perché il secondo tentativo è fallito. Il valore sta nel fatto che qualcosa succede **da
solo il giorno giusto**, e nello storico che permette di dire com'era andato l'anno prima. Il livello
conversazionale, quando ci sarà, è un'ottima superficie di lettura sopra questo stato — non un sostituto.

---

## 2. Mercato e analisi in rete

> Compilata dopo otto ricerche mirate e due letture di pagine ufficiali di prezzo
> ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4). Ciò che non è stato trovato è **dichiarato** al §2.7.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| **Stripe Billing** | globale | Abbonamenti, rinnovi, riprova intelligente degli addebiti falliti, portale del cliente — sopra il proprio incasso | **0,70% del volume fatturato** (0,50% promozionale per i clienti anteriori al 10/07/2024, fino al 30/06/2025) | [support.stripe.com](https://support.stripe.com/questions/changes-to-the-stripe-billing-starter-and-scale-plans) |
| **Chargebee** | globale (sede India/USA) | Gestione abbonamenti, listini, solleciti, riconoscimento dei ricavi | Starter: **gratis fino a 250.000 USD** di fatturato cumulato, poi **0,75% sul fatturato**; Performance: **7.188 USD/anno** (fino a 100.000 USD/mese di fatturato) | [chargebee.com/pricing](https://www.chargebee.com/pricing/) |
| **GoCardless** | Regno Unito/UE | Incasso ricorrente per addebito diretto (SEPA e schemi locali), non gestione di piani | Standard **1% + 0,20 per transazione, con tetto** (4 nella valuta del listino); Advanced 1,25%; Pro 1,4% | [gocardless.com/pricing](https://gocardless.com/pricing/) |
| **Gymdesk** | USA/globale, verticale palestre | Tesseramenti, incasso, presenze, accesso — pacchetto completo per la palestra | «circa **100 USD/mese** per una palestra da 100 iscritti»; nessun costo di attivazione | [gymdesk.com](https://gymdesk.com/blog/gym-management-software-cost) — blog del fornitore, non pagina di listino |
| **TeamUp** | Regno Unito/UE, verticale studi e corsi | Iscrizioni, corsi, abbonamenti, incasso | «da circa **99 USD/mese**», cresce col numero di iscritti attivi | [gymsense.io](https://gymsense.io/blog/how-much-does-gym-management-software-cost-2026-pricing-guide) — comparatore, **non** pagina ufficiale |
| **Frisbii**, **Fakturia**, **Billsby** | Europa (DK, DE, UK) | Fatturazione ricorrente con attenzione alla conformità europea; Billsby ha un piano gratuito | non rilevato su pagina ufficiale (vedi §2.7) | [frisbii.com](https://frisbii.com/blog/the-best-subscription-management-software-tools/) |

**Lettura.** Il campo si divide in due famiglie che non si parlano. Da una parte gli **strumenti di fatturazione
ricorrente** (Stripe Billing, Chargebee, Frisbii): potentissimi, ma pensati per chi vende software online, con un
prezzo **a percentuale sul fatturato** che presuppone di essere dentro il flusso del denaro, e un impianto che
una palestra da centoventi iscritti non riesce nemmeno a configurare. Dall'altra i **verticali di settore**
(Gymdesk, TeamUp, Virtuagym): parlano la lingua giusta ma vendono un pacchetto intero — corsi, presenze,
tornelli, applicazione per l'iscritto — a 100 dollari al mese, che è molto per chi voleva solo sapere chi non ha
pagato. **In mezzo non c'è quasi niente**: uno strumento orizzontale, in italiano, che governa piani, rinnovi,
solleciti e metriche del ricorrente **senza** pretendere di incassare e **senza** chiedere di rifare tutta la
gestione dell'attività. È lì che sta SubGrove, ed è anche il motivo per cui la sua promessa dev'essere stretta:
appena si allarga, si finisce a competere con un verticale che fa dieci cose in più.

### 2.2 Prezzi praticati nel dominio

**Unità di misura prevalente: due, molto diverse fra loro.**

1. **Percentuale sul fatturato incassato** — è lo standard degli strumenti di fatturazione ricorrente: Stripe
   Billing **0,70%** del volume fatturato (rilevato su pagina ufficiale di supporto Stripe), Chargebee **0,75%**
   oltre la soglia gratuita (rilevato su pagina ufficiale di listino). Vale la pena notarlo: le due percentuali
   sono **più del doppio** dello 0,3-0,5% che la scheda di catalogo ipotizzava per SubGrove. Ma sono anche
   percentuali prese da chi **sta dentro l'incasso**; noi non ci saremmo (§5).
2. **Canone fisso scalato sul numero di iscritti** — è lo standard dei verticali: Gymdesk «sotto i 100 USD/mese»
   per una struttura piccola, TeamUp «da circa 99 USD/mese» che cresce con gli iscritti attivi. Il numero di
   **abbonati attivi** è quindi l'unità di misura che il segmento riconosce e sa confrontare.

**Piano gratuito**: presente in Billsby e in Chargebee (quest'ultimo in forma di soglia di fatturato, non di
funzioni). Nei verticali di settore, di norma assente.

**Durata della prova**: **non rilevata in modo affidabile** su pagine ufficiali per nessuno dei prodotti
esaminati — vedi §2.7.

**Costo dell'incasso, che il cliente paga comunque a qualcun altro.** Serve per capire quanto spazio resta al
nostro canone: addebito diretto SEPA con GoCardless **1% + 0,20 con tetto a 4** per transazione; il commissionamento
di una carta presso un fornitore italiano si somma a parte. Su un abbonamento da 50 € al mese, l'incasso costa
al cliente circa 0,70 € a rata: un canone di SubGrove da 24 € al mese equivale, per lui, al costo di incasso di
circa trentaquattro rate. È un confronto che il titolare fa da solo, e conviene averlo in testa quando si sceglie
il prezzo.

### 2.3 Obblighi normativi del settore

Il dominio è **normato**, e le norme cadono quasi tutte sul **rinnovo automatico** e sulla **disdetta**. Sono
requisiti che cambiano il modello dati, non note a piè di pagina.

1. **Germania — pulsante di disdetta (§ 312k BGB).** Dal 1° luglio 2022 chi offre online a consumatori contratti
   a esecuzione continuata deve mettere a disposizione un **pulsante di disdetta** ben riconoscibile, etichettato
   «Jetzt kündigen» o formula altrettanto chiara, **raggiungibile senza dover fare accesso con credenziali** e
   presente per tutta la procedura; la Corte federale ne ha esteso l'obbligo anche a contratti che si esaurirebbero
   da soli. Chi non lo mette rischia diffide e azioni inibitorie, e il consumatore può recedere in qualunque
   momento senza preavviso. → è la ragione per cui la storia `0024` esiste e per cui il portale dell'abbonato
   (`0023`) funziona con un **collegamento firmato** invece che con un accesso a credenziali.
   Fonti: [dejure.org, testo del § 312k BGB](https://dejure.org/gesetze/BGB/312k.html) ·
   [Noerr, sintesi in inglese](https://www.noerr.com/en/insights/cancellation-button-in-online-sales).
2. **Italia — rinnovo tacito e recesso.** Le fonti divulgative consultate concordano su tre obblighi: informare
   il consumatore **almeno 30 giorni prima** della data di rinnovo indicando data, durata, prezzo applicato dopo
   il rinnovo e le modalità per recedere; **evidenziare la clausola di rinnovo tacito** in fase precontrattuale
   con accettazione separata (niente caselle preselezionate); mettere a disposizione, per i contratti sottoscritti
   online, **un canale digitale di recesso semplice almeno quanto quello di adesione**. Sono vietate le pratiche
   dilatorie (obbligo di telefonare a un centralino, moduli irraggiungibili). → è la ragione delle storie `0013`
   (avviso di rinnovo con preavviso e prova dell'invio) e `0006` (le condizioni del piano sono un dato del piano,
   non una convenzione a voce).
   Fonte: [ADICU, «Rinnovo automatico degli abbonamenti e diritto di disdetta»](https://www.adicu.it/2025/09/08/rinnovo-automatico-degli-abbonamenti-e-diritto-di-disdetta-alla-luce-delle-piu-recenti-tutele-pro-concorrenziali-e-consumeristiche/).
   **Avvertenza d'onestà**: è una fonte associativa di tutela dei consumatori, non il testo di legge. Non ho
   verificato sul testo ufficiale né l'articolo del Codice del consumo applicabile né i termini esatti → §2.7 e
   punto aperto n. 3.
3. **Addebito diretto SEPA — regole di schema.** Chi incassa a mandato deve rispettare il regolamento dello
   schema: **pre-notifica al debitore prima dell'addebito** (14 giorni di calendario salvo diverso accordo, e per
   gli importi ricorrenti uguali basta una notifica sola con tutte le date), diritto del debitore a chiedere il
   **rimborso senza motivazione entro 8 settimane** nello schema Core (non nello schema fra imprese), **decadenza
   del mandato dopo 36 mesi** senza alcun incasso. → è la ragione per cui la storia `0017` tiene lo **stato** del
   mandato e la sua **data di ultimo utilizzo**, e per cui l'avviso di rinnovo (`0013`) vale anche come
   pre-notifica quando l'incasso è a mandato.
   Fonti: [EPC, SEPA Direct Debit Core Rulebook (PDF ufficiale)](https://www.europeanpaymentscouncil.eu/sites/default/files/kb/file/2024-11/EPC016-06%202025%20SDD%20Core%20Rulebook%20version%201.0.pdf) ·
   [GoCardless, guida ai mandati](https://gocardless.com/guides/sepa/mandates) ·
   [Stripe, documentazione addebito SEPA](https://docs.stripe.com/payments/sepa-debit).
   **Avvertenza d'onestà**: i tre numeri (14 giorni, 8 settimane, 36 mesi) li ho letti nelle sintesi di
   GoCardless e Stripe e nei riepiloghi divulgativi, **non** verificandoli riga per riga nel regolamento ufficiale,
   che è il documento che comanda.
4. **Servizi di pagamento (PSD2) — il confine che decide l'architettura.** Una piattaforma che **entra in
   possesso o nel controllo** dei fondi dovuti dall'acquirente al venditore e poi glieli gira **presta un
   servizio di pagamento regolato**, salvo eccezioni; l'eccezione dell'agente commerciale vale solo per chi agisce
   per **una** delle due parti, e le analisi consultate la danno per praticamente inapplicabile alle piattaforme,
   con un ulteriore restringimento atteso. Conclusione operativa: o si ha una licenza, o si lavora con un
   prestatore autorizzato che si assume la responsabilità, **o non si tocca il denaro**. SubGrove sceglie la
   terza. Fonti: [Online Payment Platform, «Commercial agent exemption»](https://blog.onlinepaymentplatform.com/en/commercial-agent-exemption-for-platforms-an-untenable-exception-to-psd2-rules) ·
   [Stripe, «How PSD2 impacts marketplaces and platforms»](https://stripe.com/guides/how-psd2-impacts-marketplaces-and-platforms).

**Cosa non è normato in modo rilevante**: il calcolo delle metriche di ricavo ricorrente (nessuno standard
vincolante per micro-imprese: è gestione, non contabilità) e la conservazione dell'avviso di addebito interno,
che non è un documento fiscale.

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta prevista:

1. **La contabilità e la fatturazione** — la scadenza ricorrente deve diventare una fattura. Dentro la suite è
   **02 BillGrove**; fuori, un'esportazione tabellare. *Non* introduce un fornitore esterno se resta dentro casa.
2. **Il proprio fornitore di incasso** (banca per l'addebito diretto, oppure il conto che il cliente ha già presso
   un fornitore di pagamento) — per **leggere** gli esiti invece di batterli a mano. ⚠️ **Introduce un
   responsabile esterno del trattamento** e credenziali del cliente: è la storia `0020`, e non è nel nucleo.
3. **La posta elettronica** per avvisi di rinnovo e solleciti — il servizio di invio è già di piattaforma
   (renderer condiviso, change `0079`), quindi nessun fornitore nuovo.
4. **La messaggistica** (breve messaggio di testo o applicazione di messaggistica) per i solleciti — ⚠️ **fornitore
   esterno**, fuori dal nucleo; l'app 07 BookGrove ha già affrontato lo stesso tema.
5. **L'anagrafica clienti condivisa della suite** — l'abbonato *è* un cliente: vedi §10.
6. **Il foglio di calcolo che il cliente ha oggi** — importazione iniziale. È la prima cosa che chiedono, e la si
   sottovaluta sempre.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Non ho trovato una raccolta di recensioni sufficientemente specifica per il segmento italiano da poterla citare
come fonte (§2.7). Quello che le pagine consultate dicono in modo consistente, e che vale come **requisito
travestito**, è che il denaro perduto per **abbandono involontario** — l'addebito che fallisce, non il cliente
che se ne va — è una quota grande del totale: le fonti la collocano fra il **20% e il 40%**, con punte fino al
53%, e riportano che una catena di solleciti ben fatta ne recupera fra il **50% e l'80%**, con la parte del leone
nelle **prime 72 ore** e un decadimento netto dopo due settimane; un servizio di aggiornamento automatico delle
carte scadute abbatterebbe fino al 30% dell'abbandono involontario legato alla carta. Questo dice tre cose:
(a) la catena dei solleciti **è il prodotto**, non un accessorio; (b) il primo sollecito deve partire **subito**,
non «a fine mese»; (c) la finestra utile è **breve**, quindi la sospensione automatica va calibrata su giorni,
non su mesi. Fonti: [Recurly, dati sul recupero degli addebiti falliti](https://recurly.com/blog/failed-payment-recovery-data-based-strategy/) ·
[ProsperStack, «Subscription dunning»](https://prosperstack.com/blog/subscription-dunning/). Sono fonti di
fornitori, con l'interesse commerciale che ne consegue: le leggo come **ordini di grandezza**, non come misure.

Quello che questo segmento **rifiuta**, e che va tenuto fuori: la configurazione a regole («se lo stato è X e
sono passati N giorni allora…»), i cruscotti con dodici indicatori, e qualunque cosa che chieda di ridefinire
i piani commerciali per farli entrare nel modello dello strumento.

### 2.6 Fonti consultate

1. **Stripe — modifiche ai piani Billing** — https://support.stripe.com/questions/changes-to-the-stripe-billing-starter-and-scale-plans — la percentuale ufficiale sul volume fatturato (0,70%, dal 10/07/2024) e la promozionale allo 0,50%: è il riferimento con cui confrontare lo 0,3-0,5% ipotizzato dalla scheda di catalogo.
2. **Chargebee — listino ufficiale** — https://www.chargebee.com/pricing/ — piano Starter gratuito fino a 250.000 USD cumulati poi 0,75% sul fatturato, Performance a 7.188 USD/anno: conferma che il modello a percentuale è lo standard di categoria e mostra a che ordine di prezzo sta il piano «serio».
3. **GoCardless — listino ufficiale** — https://gocardless.com/pricing/ — 1% + 0,20 per transazione con tetto (Standard): mi ha dato il **costo dell'incasso** che il cliente paga comunque, cioè lo spazio che resta al nostro canone.
4. **Gymdesk — quanto costa un gestionale per palestre** — https://gymdesk.com/blog/gym-management-software-cost — «circa 100 USD/mese per 100 iscritti»: l'ordine di grandezza del verticale, e la conferma che l'unità di misura del settore è il numero di iscritti attivi.
5. **§ 312k BGB, testo** — https://dejure.org/gesetze/BGB/312k.html — e **Noerr, sintesi in inglese** — https://www.noerr.com/en/insights/cancellation-button-in-online-sales — l'obbligo del pulsante di disdetta senza accesso a credenziali: ha determinato il disegno del portale dell'abbonato (`0023`, `0024`).
6. **ADICU — rinnovo automatico e diritto di disdetta** — https://www.adicu.it/2025/09/08/rinnovo-automatico-degli-abbonamenti-e-diritto-di-disdetta-alla-luce-delle-piu-recenti-tutele-pro-concorrenziali-e-consumeristiche/ — preavviso di 30 giorni, accettazione separata della clausola di rinnovo, canale digitale di recesso: ha determinato le storie `0013` e `0006`. Fonte **associativa**, non testo di legge.
7. **EPC — SEPA Direct Debit Core Rulebook** — https://www.europeanpaymentscouncil.eu/sites/default/files/kb/file/2024-11/EPC016-06%202025%20SDD%20Core%20Rulebook%20version%201.0.pdf — insieme a https://gocardless.com/guides/sepa/mandates e https://docs.stripe.com/payments/sepa-debit — pre-notifica, rimborso a 8 settimane, decadenza del mandato a 36 mesi: ha determinato la storia `0017`.
8. **Online Payment Platform, «Commercial agent exemption»** — https://blog.onlinepaymentplatform.com/en/commercial-agent-exemption-for-platforms-an-untenable-exception-to-psd2-rules — e **Stripe, PSD2 per piattaforme e mercati** — https://stripe.com/guides/how-psd2-impacts-marketplaces-and-platforms — il confine fra «software che registra» e «piattaforma che presta un servizio di pagamento»: è la motivazione tecnica della postura del §5 e del §1.
9. **Recurly, recupero degli addebiti falliti** — https://recurly.com/blog/failed-payment-recovery-data-based-strategy/ — e **ProsperStack** — https://prosperstack.com/blog/subscription-dunning/ — quote di abbandono involontario e tassi di recupero: hanno determinato la forma della catena di solleciti (`0021`) e la finestra della sospensione automatica (`0022`).
10. **Frisbii, panoramica degli strumenti europei** — https://frisbii.com/blog/the-best-subscription-management-software-tools/ — mi ha dato i nomi dei prodotti europei del segmento (Frisbii, Fakturia, Billsby) di cui però **non** ho potuto rilevare i prezzi ufficiali.

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzi ufficiali dei concorrenti europei diretti** (Frisbii, Fakturia, Billsby) — le pagine di listino non
  sono state aperte e i numeri circolanti vengono da comparatori, che invecchiano male. Servirebbe una lettura
  diretta delle tre pagine di listino; finché non c'è, la proposta del §5 **non** poggia su di loro.
- **Durata tipica della prova gratuita nel dominio** — nessuna delle fonti consultate la riporta in modo
  affidabile. La proposta del §5 usa quindi il **predefinito di piattaforma** (14 giorni), non un dato di mercato.
- **Prezzo di TeamUp e Virtuagym su pagina ufficiale** — i numeri riportati al §2.1 vengono da comparatori e da
  blog di fornitori concorrenti. Vanno letti come ordini di grandezza.
- **L'articolo di legge italiano esatto** che impone il preavviso di 30 giorni sul rinnovo tacito e il canale
  digitale di recesso — la fonte consultata è associativa e non lo cita. È un punto per la **revisione legale**
  (punto aperto n. 3): la storia `0013` è progettata perché il termine di preavviso sia un **parametro del
  piano**, non una costante nel codice, proprio per non doverla riscrivere quando il termine sarà verificato.
- **Se esista già un concorrente italiano orizzontale** con questa esatta promessa — la ricerca in italiano ha
  restituito soltanto passerelle di pagamento e gestionali verticali. L'assenza di risultati **non** è prova di
  assenza del concorrente.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `abbonati` | Rispetta `^[a-z][a-z0-9_]{0,30}$` (8 caratteri, minuscolo, sole lettere) e segue la convenzione già viva nel repository, dove l'app numero uno è `fatture`: identificativo tecnico in italiano, nome commerciale a parte. **Ho deliberatamente scartato `abbonamenti`**, che sarebbe la parola più naturale: la piattaforma chiama «abbonamento» il **proprio** contratto con il cliente (sezione «Abbonamenti» del backoffice, `subscription` in `services/core`, use case 0067). Due cose diverse con lo stesso nome, dentro lo stesso repository, sono un incidente che aspetta di succedere — nello schema (`app_abbonati` contro `platform`), nella rotta (`/api/abbonati/v1/*` contro `/api/platform/v1/me/subscriptions`), nei nomi degli strumenti conversazionali e, soprattutto, nelle conversazioni fra persone. `abbonati` dice **di chi** sono gli abbonamenti: dei clienti del nostro cliente. Cambiarlo dopo non è una rinomina, è una migrazione di dati. |
| **Modello utente** | `multi` | In una giornata tipo del cliente lavorano almeno due figure diverse: chi sta alla reception iscrive, incassa al banco e segna il pagato; il titolare (o l'amministrazione) decide i piani, guarda i numeri e autorizza le eccezioni — uno sconto, una sospensione, una disdetta senza penale. Le domande «chi ha registrato questo incasso», «chi ha sospeso questo abbonamento» e «chi ha cambiato il piano a questo iscritto» non sono un lusso: sono la condizione per non litigare su chi ha sbagliato, e su dati che riguardano il denaro dovuto da terzi. Un'app a utente singolo non ha il concetto di «chi ha fatto cosa». |
| **Porta locale** | `8119` | Convenzione del kit (8100 + 19) per non far collidere le sessanta proposte. Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `abbonamenti_attivi` | La **sola** cosa che il piano limita: quanti abbonamenti l'app tiene vivi in un dato momento (in prova, attivi, in ritardo di pagamento o sospesi — cioè tutti quelli che l'app **sorveglia**; i cessati non contano). È l'unità che il segmento riconosce e sa confrontare, perché è la stessa dei verticali di settore (§2.2), e cresce esattamente con il valore ricevuto: chi ha trecento iscritti riceve trecento volte il servizio di chi ne ha uno. Ho scartato «rinnovi elaborati al mese», che pure sarebbe un consumo misurabile, perché punisce chi fattura mensilmente rispetto a chi fattura annualmente a parità di clientela: una distorsione che il cliente non capirebbe. |
| **Natura della metrica** | `stock` | Tetto su ciò che esiste **ora**: «il piano Studio segue 150 abbonamenti; per attivare il 151° bisogna che uno cessi, o si passa di piano». Non è un consumo su una finestra che si azzera — un abbonamento attivo il primo agosto è ancora lì il primo settembre, e continua a costare lavoro all'app ogni giorno (rinnovi, avvisi, solleciti, metriche). Contarlo come consumo lascerebbe accumulare senza limite: un cliente arriverebbe a duemila abbonamenti sul piano più piccolo. Conseguenza voluta e da spiegare bene nell'interfaccia: la **riduzione di piano è sbarrata** finché gli abbonamenti attivi eccedono il tetto del piano di destinazione (regola di piattaforma, [docs/09-pagamenti.md](../../../09-pagamenti.md) dec. 23), e il rimedio si dice a schermo — «cessa o archivia N abbonamenti, poi potrai scendere». |
| **Colore-categoria e icona** | `red` · icona `repeat` (due frecce che si inseguono in cerchio) | Deve essere lo stesso nel listino (`category`) e nel modulo frontend (`accentToken`). Nel repository i colori delle app **reali** sono già presi: `green` (`fatture`) e `blue` (`crm`). Fra le sorelle di catalogo esaminabili sono già proposti `teal` (02 BillGrove, 12 DeskGrove), `amber` (03 CashGrove, 08 SpendGrove, 14 StockGrove), `blue` (04 LeadGrove), `violet` (06 QuoteGrove, 13 FlowGrove, 16 ReachGrove), `green` (07 BookGrove). **Resta `red`**, che nel sistema di design non è un rosso d'allarme ma una **terracotta calda** (`--cat-red: 227 101 79`), distinta dal colore funzionale di errore. È anche difendibile nel merito: la superficie che il titolare apre ogni giorno in SubGrove è quella di **ciò che non è andato** — l'addebito fallito, la disdetta arrivata, l'abbonamento da sospendere. **Fermata onesta**: se la piattaforma preferisce tenere `red` libero per non confondersi con gli stati d'errore, il ripiego è `teal`, ed è una decisione di piattaforma, non mia (punto aperto n. 6). Sei colori per sessanta app: la collisione è strutturale, come già osservato dalle app 13 e 16. |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Piano` | Ciò che il cliente vende a canone | nome, descrizione, ciclo (mensile/trimestrale/annuale), durata minima, rinnovo tacito sì/no, giorni di preavviso per la disdetta, giorni di prova, stato (bozza/attivo/archiviato) | no |
| `VersionePrezzo` | Il prezzo di un piano in un dato momento | importo in centesimi, valuta, aliquota, decorrenza, stato (viva/archiviata) | no |
| `Abbonato` | Il cliente del nostro cliente | nome o ragione sociale, recapiti, indirizzo di fatturazione, identificativo fiscale, riferimento all'anagrafica condivisa della suite | **sì** — anagrafica e recapiti (§6) |
| `Abbonamento` | Il contratto vivo fra i due | abbonato, piano, versione di prezzo agganciata, decorrenza, inizio e fine del periodo in corso, stato, cambio programmato, motivo di cessazione | **sì**, per riferimento all'abbonato |
| `Scadenza` | Quanto è dovuto per un periodo | periodo coperto, importo, data di esigibilità, stato (attesa/incassata/fallita/stornata/annullata), riferimento al documento fiscale se esiste | no (importi, non persone) |
| `AutorizzazioneAddebito` | Il permesso di addebitare, che vive **fuori** | tipo (mandato di addebito diretto / autorizzazione presso il fornitore del cliente / nessuna), **riferimento** rilasciato da chi la custodisce, stato, data di firma, data d'ultimo uso | **sì**, in senso stretto — è un riferimento a un rapporto bancario (§6). **Mai** l'IBAN, **mai** il numero di carta |
| `Sollecito` | Un tentativo di far rientrare una scadenza | scadenza, progressivo, canale, momento d'invio, esito, messaggio usato | **sì**, per riferimento al recapito usato |
| `AvvisoDiRinnovo` | La prova di aver avvisato prima del rinnovo | abbonamento, data di invio, data di rinnovo comunicata, prezzo comunicato, canale, ricevuta di consegna | **sì**, per riferimento |
| `RichiestaDellAbbonato` | Ciò che l'abbonato chiede dal portale | tipo (disdetta / cambio piano / aggiornamento recapiti), momento, esito, prova della richiesta | **sì**, per riferimento |
| `IstantaneaRicavi` | La fotografia mensile del ricorrente | mese, ricavo ricorrente mensile, numero di abbonamenti attivi, scomposizione (nuovo, espansione, contrazione, abbandono) | no — aggregati |

**Relazioni.** `Piano` 1→N `VersionePrezzo`; `Abbonato` 1→N `Abbonamento`; `Abbonamento` 1→N `Scadenza`;
`Scadenza` 1→N `Sollecito`; `Abbonato` 1→0..N `AutorizzazioneAddebito`. `IstantaneaRicavi` non ha padre: è
calcolata e messa da parte una volta al mese, e **non si ricalcola all'indietro** (storia `0027`).

**Macchina a stati dell'abbonamento** — è la parte che tutte le storie devono rispettare:

```
                    ┌──────────────┐
     (con prova)    │   in_prova   │
   ──────────────►  └───────┬──────┘
                            │ prima scadenza incassata / fine prova
   (senza prova)            ▼
   ──────────────►  ┌──────────────┐  cambio piano  ┌──────────────┐
                    │    attivo    │ ◄────────────► │    attivo    │
                    └───┬───┬───┬──┘                └──────────────┘
      scadenza fallita  │   │   │ disdetta ricevuta
                        ▼   │   ▼
              ┌──────────┐  │  ┌───────────────────┐
              │in_ritardo│  │  │disdetto_a_scadenza│  (accesso fino a fine periodo)
              └────┬──┬──┘  │  └─────────┬─────────┘
     incassato     │  │ soglia di        │ arriva la fine del periodo
   ◄──────────────┘   │ solleciti        ▼
    (torna attivo)    ▼ superata   ┌──────────┐
              ┌────────────┐       │  cessato │ (stato finale)
              │  sospeso   ├──────►└──────────┘
              └─────┬──────┘  cessazione
                    │ ripresa
                    └──────► attivo
```

Regole della macchina, tutte verificabili: `in_prova`, `attivo`, `in_ritardo` e `disdetto_a_scadenza`
**contano** nella metrica di quota; `sospeso` **conta** (l'app continua a sorvegliarlo); `cessato` **non conta**.
Solo `attivo`, `in_prova` e `disdetto_a_scadenza` generano scadenze nuove. Il passaggio a `sospeso` è
**automatico** al superamento della soglia di solleciti del piano (storia `0022`) ma **reversibile** a mano.
La cessazione è definitiva e non si annulla: si riparte con un abbonamento nuovo.

**Somiglianza voluta con la piattaforma, e sua misura.** Questa macchina a stati è deliberatamente **parente**
di quella degli abbonamenti di piattaforma (`trialing / active / past_due / paused / canceled`,
[docs/09-pagamenti.md](../../../09-pagamenti.md) dec. 29): stessi passaggi, stessa idea di «tolleranza sul
pagamento fallito», stessa idea di «disdetta con accesso fino a scadenza». Riusare il **vocabolario** e la
**semantica** già ragionati è un guadagno netto. Riusare il **codice** e i **dati** sarebbe un errore: perché,
sta al §10.1.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_abbonati`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata
> della prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di
> scrivere il file `services/core/src/main/resources/pricing/abbonati.yaml`.

### 5.1 🛑 La componente a percentuale della scheda di catalogo va tolta — e non è una questione di prezzo

La scheda 19 propone «€19-49/mese flat **+ 0,3-0,5% sui pagamenti ricorrenti**». **Raccomando di abbandonare la
componente a percentuale**, per due ragioni indipendenti, ciascuna delle quali basterebbe da sola.

1. **Contrasta con un vincolo non negoziabile di piattaforma.** Il listino come codice ammette **solo abbonamento
   ricorrente**: «niente pagamento una tantum, niente addebito a consumo per lo sforamento», e al raggiungimento
   del limite si **blocca**, non si addebita ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §7,
   [docs/09-pagamenti.md](../../../09-pagamenti.md) dec. 1-9). Una percentuale sull'incassato è, per definizione,
   un addebito a consumo. Non c'è modo di scriverla nel formato del listino, e non è un dettaglio di forma: il
   fornitore di pagamento della piattaforma vende un abbonamento, non un contatore.
2. **Presuppone di stare dentro il flusso del denaro, dove abbiamo deciso di non stare.** Chi applica una
   percentuale sull'incassato — Stripe Billing allo 0,70%, Chargebee allo 0,75% (§2.2) — la **misura** perché il
   denaro gli passa davanti. A noi non passa (§5.2): la percentuale andrebbe calcolata su un importo **dichiarato
   dal cliente**, cioè su un numero che il cliente ha tutto l'interesse a dichiarare piccolo e nessun obbligo di
   dichiarare giusto. Un prezzo che si può ridurre mentendo non è un prezzo: è un invito.

**Dove finisce, allora, la dimensione «quanto incassi»?** Nel **limite**, non nel prezzo: chi ha più abbonamenti
sta su un piano più alto. È lo stesso effetto economico — chi usa di più paga di più — ottenuto con lo strumento
che la piattaforma sa maneggiare, e con un conto che il cliente può prevedere all'euro.

### 5.2 🛑 SubGrove non incassa denaro di terzi — dove passa esattamente il confine

L'app tocca il denaro che i clienti del cliente pagano. Il confine, e le sue ragioni:

**Il denaro non transita da appgrove, in nessuna forma e in nessun piano.** SubGrove sa **quanto è dovuto**
(perché lo calcola dal piano) e **se è rientrato** (perché qualcuno glielo dice); non riceve fondi, non li
detiene, non li gira, non ordina un addebito. L'incasso avviene fuori: bonifico sul conto dell'attività, addebito
diretto con il mandato che l'attività ha con la **propria** banca, addebito sul conto che l'attività ha presso il
**proprio** fornitore di pagamento, contanti al banco.

**Perché.** Una piattaforma che entra in possesso o nel controllo dei fondi dovuti dall'acquirente al venditore
presta un servizio di pagamento regolato, salvo eccezioni che le analisi consultate danno per praticamente
inapplicabili alle piattaforme e destinate a restringersi ancora (§2.3, punto 4). Le vie legittime sono tre:
prendere una licenza, appoggiarsi a un prestatore autorizzato che si assuma la responsabilità, oppure **non
toccare il denaro**. Per un marchio che sta ancora costruendo il proprio sito vetrina, e che sul **proprio**
incasso ha già scelto di delegare tutto a un venditore di riferimento, la terza è l'unica proporzionata. È anche
la stessa scelta già fatta per l'app **06 QuoteGrove** (l'acconto si scrive e si segna incassato a mano) e per
l'app **07 BookGrove** (l'acconto si registra, non si incassa).

**Cosa resta fuori, esplicitamente**: ordinare un addebito da SubGrove (è **avvio di un pagamento**, servizio
regolato); custodire fondi anche solo per un'ora; far pagare l'abbonato «dentro» una pagina di appgrove;
trattenere una percentuale sull'incassato (§5.1); conservare numeri di carta o coordinate bancarie complete.

**Cosa resta dentro, e va detto perché non sfonda il confine**: il **collegamento in sola lettura** al conto che
il cliente ha già presso il proprio fornitore di incasso (storia `0020`), che serve solo a **leggere gli esiti**
invece di batterli a mano. Anche così, introduce un responsabile esterno del trattamento e credenziali del
cliente: per questo è una storia **separata**, marcata come fermata di escalation, e il nucleo dell'app funziona
interamente senza di essa. **Non è la postura di default**: la postura di default è la registrazione manuale e
l'importazione da file (`0018`, `0019`).

**Se lo sviluppatore decidesse diversamente** — cioè di incassare per conto del cliente — non sarebbe una
variante di questa app: sarebbe un altro prodotto, con licenza o con un prestatore autorizzato, un contratto
diverso, una responsabilità diversa e un'analisi legale che nessun agente può scrivere. Va trattata come tale.

### 5.3 Ragionamento sui numeri

I riferimenti utili sono tre. In alto, i **verticali di settore** a circa 100 dollari al mese per una struttura
piccola (§2.1), che però comprendono corsi, presenze e controllo accessi: SubGrove deve stare **nettamente
sotto**, perché fa una fetta di quel lavoro. In basso, il **costo dell'incasso** che il cliente paga comunque
(circa 1% con tetto, §2.2): il nostro canone dev'essere confrontabile con poche decine di rate incassate, non con
centinaia. In mezzo, la fascia della scheda di catalogo, 19-49 €/mese, che risulta **compatibile** con entrambi i
riferimenti — è uno dei rari casi in cui la ricerca conferma l'ipotesi di partenza invece di smentirla.

| Piano | Prezzo mensile | Prezzo annuale | Limite su `abbonamenti_attivi` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 10 | — | Il professionista con una manciata di contratti di assistenza: abbastanza per vedere il valore su clienti veri, non abbastanza per starci dentro un'attività |
| `studio` | **24 €** | **240 €** (= 10× il mensile, «due mesi in regalo») | 150 | 14 giorni | La scuola, lo studio, il piccolo centro: il grosso del mercato che oggi usa il foglio di calcolo |
| `club` | **49 €** | **490 €** | 750 | 14 giorni | La palestra strutturata, l'associazione con molte quote, il service con parecchi contratti annuali |

**Note obbligate.**

- **Tre piani, non di più**: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- **Un limite lasciato vuoto significa illimitato, non zero.** Qui nessun limite è vuoto: il piano `club` ha un
  tetto esplicito a 750, ed è voluto — un'attività con più di settecentocinquanta abbonati attivi non è più il
  cliente per cui questa app è disegnata, e merita una conversazione, non un'attivazione silenziosa.
- **Prova gratuita e piano gratuito insieme: qui non sono ridondanti.** Il piano `free` è un **posto dove
  restare** (dieci contratti, per sempre); la prova di 14 giorni serve a verificare i **tetti alti** e le funzioni
  del piano a pagamento su una base reale di duecento iscritti. Sono due domande diverse. Resta però un'obiezione
  legittima — il piano gratuito attira esattamente il segmento che non converte mai — e la lascio scritta, perché
  la decisione non è mia (punto aperto n. 4).
- **Costo effettivo dell'incasso** (piattaforma, [docs/09-pagamenti.md](../../../09-pagamenti.md) dec. 46-49): con
  una commissione dell'ordine del 5% più mezzo dollaro a transazione, su `studio` mensile a 24 € la commissione
  effettiva sta attorno al **7%**; sull'annuale a 240 € scende attorno al **5%**. Nessun piano proposto sta sotto
  i 5 €/mese, quindi non scatta l'avviso morbido del co-pilota (soglia ~10%). L'annuale va messo in evidenza, come
  vuole la regola generale.
- **Prezzi immutabili una volta vivi**: un cambio di prezzo si fa creando un prezzo nuovo e archiviando il
  vecchio, mai modificando quello esistente. Vale per **noi**; e — cosa che val la pena notare — è la stessa
  regola che la storia `0007` fa rispettare al **cliente** sui prezzi dei suoi piani, per lo stesso identico
  motivo: chi è già dentro non deve vedersi cambiare l'importo sotto i piedi.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/abbonati.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

> 🛑 **Attenzione — categorie particolari (articolo 9): NON ce ne sono, e c'è una ragione precisa per cui
> potrebbero entrare dalla finestra.** Come progettata, SubGrove **non** tratta dati sulla salute, biometrici,
> genetici, né opinioni, convinzioni, orientamento o appartenenza sindacale. Ma due dei suoi segmenti di mercato
> naturali — **palestre e centri sportivi**, e **associazioni** — portano con sé due trappole classiche:
> il **certificato medico di idoneità sportiva** (dato sulla salute, articolo 9) e la **quota associativa a un
> sindacato o a un'associazione di categoria** (appartenenza sindacale, articolo 9, quando il piano o l'abbonato
> ne rivelano la natura). La prima entra appena qualcuno chiede «una casella con la scadenza del certificato»; la
> seconda entra da sola se il nome del piano è quello di un sindacato.
> **Presidio proposto, che vale come requisito e non come raccomandazione**: (a) SubGrove **non ha e non avrà** un
> campo per documenti sanitari né per la loro scadenza — chi ne ha bisogno usa **18 VaultGrove**, che è l'app
> disegnata per i documenti; (b) i campi a testo libero portano l'avvertenza esplicita di non inserire dati
> sanitari (storia `0008`); (c) se un giorno si volesse aggiungere la scadenza del certificato medico, quella è una
> **valutazione d'impatto e una base giuridica rafforzata**, cioè una decisione dello sviluppatore, non una
> spunta in un modulo. Vale anche se il campo fosse facoltativo: un dato particolare facoltativo resta un dato
> particolare. Un'app che può evitarli, di norma deve evitarli — e questa può.

**Chi sono gli interessati.** Due popolazioni distinte, e conviene non confonderle: gli **utenti del cliente**
(reception, amministrazione, titolare — già trattati dalla piattaforma) e gli **abbonati**, cioè i **clienti del
nostro cliente**, che non hanno alcun rapporto con appgrove e non sanno che esistiamo. La seconda popolazione è
quella che questo manifesto deve coprire, ed è la stessa situazione già affrontata dalle app 03, 04, 07 e 16.

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `abbonato.nome` | `abbonato.denominazione` | cliente del cliente | anagrafico | identificare chi è abbonato e a cosa | esecuzione del contratto fra il cliente e il suo abbonato (il titolare è il cliente; appgrove è responsabile) | durata dell'abbonamento + periodo di conservazione deciso dal cliente |
| `abbonato.email` | `abbonato.email` | cliente del cliente | contatto | mandare avviso di rinnovo, sollecito, collegamento al portale | esecuzione del contratto (l'avviso di rinnovo è **dovuto per legge**, §2.3) | come sopra |
| `abbonato.telefono` | `abbonato.telefono` | cliente del cliente | contatto | sollecito su canale alternativo | esecuzione del contratto | come sopra |
| `abbonato.indirizzo_fatturazione` | `abbonato.indirizzo_*` | cliente del cliente | anagrafico/fiscale | intestare la scadenza e, a valle, il documento fiscale | obbligo di legge del cliente | come sopra |
| `abbonato.identificativo_fiscale` | `abbonato.codice_fiscale_o_partita_iva` | cliente del cliente | fiscale | intestazione del documento | obbligo di legge del cliente | come sopra |
| `autorizzazione.riferimento` | `autorizzazione_addebito.riferimento_esterno` | cliente del cliente | **riferimento a un rapporto bancario** — non la coordinata | sapere se esiste un permesso di addebito valido e quando decade (36 mesi, §2.3) | esecuzione del contratto | fino a revoca o decadenza + prova |
| `abbonamento.stato_e_storia` | `abbonamento`, `scadenza` | cliente del cliente | economico/contrattuale | governare rinnovi, solleciti, sospensioni | esecuzione del contratto | come sopra |
| `sollecito.recapito_usato` | `sollecito.destinatario`, `sollecito.esito` | cliente del cliente | contatto + comportamentale | **prova** di aver sollecitato, e non risollecitare a vuoto | esecuzione del contratto; **prova** in caso di contestazione | conservazione decisa dal cliente, con predefinito prudente |
| `avviso_rinnovo.prova_di_invio` | `avviso_di_rinnovo` | cliente del cliente | contatto + prova | dimostrare di aver rispettato il preavviso di legge | **obbligo di legge** del cliente (§2.3) | almeno quanto la prescrizione dei diritti nascenti dal contratto — da fissare con il legale |
| `richiesta_abbonato.prova` | `richiesta_dell_abbonato` | cliente del cliente | prova | dimostrare che la disdetta è stata resa possibile e ricevuta (§312k) | **obbligo di legge** del cliente | come sopra |

**Cosa NON si conserva, e va scritto nel manifesto come esclusione esplicita**: numeri di carta (mai, in nessuna
forma, nemmeno mascherati), IBAN e coordinate bancarie complete, documenti sanitari, fotografie, dati di accesso
fisico.

**Esportazione e cancellazione.** Devono comparire **tutte** in `exportData` e in `purgeData` del contratto dati
dell'app (`AbbonatiDataContract`): `abbonato`, `abbonamento`, `scadenza`, `autorizzazione_addebito`, `sollecito`,
`avviso_di_rinnovo`, `richiesta_dell_abbonato`. Fuori restano `piano`, `versione_prezzo` e `istantanea_ricavi`,
che non contengono dati riferiti a persone — **con un'eccezione da non dimenticare**: se un cliente battezzasse
un piano con il nome di un abbonato («piano Mario Rossi»), il dato personale finirebbe in una tabella non
esportata. È il genere di cosa che si scopre tardi: la storia `0006` lo affronta con un avviso a schermo, e la
questione se basti è il punto aperto n. 5. La cancellazione è **fisica** e lascia una riga di prova nel registro
delle purghe: sostituire i nomi con dei codici non è cancellare.

**Testo libero.** Ci sono due campi nota liberi (sull'abbonato e sull'abbonamento). Sono un ingresso non
presidiato per categorie particolari — è esattamente da lì che entrerebbe «ha il certificato medico scaduto».
L'app non fa rilevazione di contenuto; il presidio è l'avvertenza a schermo (storia `0008`) e il fatto che il
campo sia esportato e cancellato come tutto il resto. Se servisse un presidio vero, è un tema trasversale, non di
questa app.

**Integrazioni esterne.** Del §2.4, due introdurrebbero un **responsabile esterno del trattamento** e vanno
elencate nell'informativa e nella lista dei fornitori: il **collegamento in sola lettura al fornitore di incasso
del cliente** (storia `0020`) e la **messaggistica** per i solleciti (fuori dal nucleo, non ha una storia).
Posta elettronica e fatturazione restano in casa.

**Classificazione della change.** Una app nuova introduce finalità nuove e una popolazione di interessati nuova
(gli abbonati del cliente): è un cambiamento **sostanziale**, con aggiornamento dell'informativa e della lista dei
fornitori. La classificazione descrive la realtà, non è una leva per evitare adempimenti.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, use case 0061-0066, scritti e non
> implementati): qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_abbonamenti` | `(stato?, piano?, scadenza_entro?) → elenco minimizzato` | Chi è abbonato a cosa, con stato e prossima scadenza | lettura | no |
| `prossimi_rinnovi` | `(giorni) → elenco di rinnovi con importo e data` | «Cosa mi si rinnova nei prossimi 30 giorni» | lettura | no |
| `scadenze_non_incassate` | `(periodo?, oltre_giorni?) → elenco con progressivo dei solleciti` | Il buco di cassa e a che punto è il recupero | lettura | no |
| `metriche_ricorrenti` | `(mese) → ricavo mensile, attivi, nuovo/espansione/contrazione/abbandono` | La fotografia del mese | lettura | no |
| `stato_abbonato` | `(riferimento_abbonato) → scheda minimizzata` | Tutto su un abbonato, senza aprire l'interfaccia | lettura | no |
| `crea_abbonamento` | `(abbonato, piano, decorrenza) → bozza` | Sottoscrive un abbonamento | scrittura | **sì** |
| `cambia_piano` | `(abbonamento, nuovo_piano, quando) → bozza con conguaglio calcolato` | Cambia quanto l'abbonato pagherà | scrittura | **sì** |
| `registra_incasso` | `(scadenza, importo, data, riferimento) → bozza` | Segna una scadenza come rientrata | scrittura | **sì** |
| `sollecita_scadenza` | `(scadenza, canale) → bozza del messaggio` | **Manda un messaggio a una persona che non è il nostro utente** | scrittura irreversibile | **sì, obbligatoria** |
| `disdici_abbonamento` | `(abbonamento, decorrenza, motivo) → bozza` | Chiude un rapporto contrattuale con effetti economici | scrittura irreversibile | **sì, obbligatoria** |
| `sospendi_abbonamento` | `(abbonamento, motivo) → bozza` | Toglie il servizio a una persona | scrittura irreversibile | **sì, obbligatoria** |

**Lettura.** Gli strumenti che rendono questa app più utile delle concorrenti dalla chat sono i primi quattro,
e per un motivo preciso: le domande che il titolare si fa sul ricorrente sono **domande di aggregazione su uno
stato che cambia da solo** — «quanto mi entra il mese prossimo», «chi non ha pagato da più di venti giorni»,
«quanti se ne sono andati da gennaio». Sono esattamente le domande che nel foglio di calcolo richiedono mezz'ora
di tabelle pivot e che nell'interfaccia richiedono di sapere dove cliccare. Gli strumenti di scrittura, al
contrario, toccano tutti il rapporto fra il cliente e una **terza persona** — le si manda un messaggio, le si
cambia il prezzo, le si toglie il servizio: nessuno di essi è ammesso senza una conferma umana esplicita, e i tre
marcati «irreversibile» hanno effetti verso l'esterno che non si annullano.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, si avvia in locale, si vede nella barra laterale di chi ha l'abilitazione, ha
il suo schema vuoto e sa dire «no» quando la quota è finita.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Istanza di scaffolding, rotte `/api/abbonati/v1/*`, definizione delle interfacce, infrastruttura dal modulo comune |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_abbonati`, prime migrazioni, `tenant_id` e colonne di controllo ovunque |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Manifesto, registrazione, sezioni, cinque lingue, colore-categoria |
| [0004](01-fondamenta/0004-abbonamento-e-quota.md) | Abbonamento e quota | Metrica `abbonamenti_attivi` a giacenza, varco a `429`, riduzione di piano sbarrata |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` la vede, dati inventati che coprono tutti gli stati |

### Epica 02 — Piani e abbonati

Alla fine dell'epica il cliente può descrivere che cosa vende, a chi lo vende, e avere abbonamenti vivi con uno
stato che significa qualcosa.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-piani-e-abbonati/0006-catalogo-dei-piani.md) | Catalogo dei piani | Il piano con le sue condizioni contrattuali: ciclo, durata minima, rinnovo tacito, preavviso |
| [0007](02-piani-e-abbonati/0007-versioni-di-prezzo-del-piano.md) | Versioni di prezzo del piano | Il prezzo vivo non si modifica: se ne crea uno nuovo, chi è dentro resta sul suo |
| [0008](02-piani-e-abbonati/0008-anagrafica-degli-abbonati.md) | Anagrafica degli abbonati | Chi sono i clienti del cliente, con i recapiti che servono agli avvisi |
| [0009](02-piani-e-abbonati/0009-manifesto-dati-e-diritti-dell-interessato.md) | Manifesto dati e diritti dell'interessato | Manifesto in italiano e inglese, esportazione e cancellazione che non dimenticano tabelle |
| [0010](02-piani-e-abbonati/0010-sottoscrizione-di-un-abbonamento.md) | Sottoscrizione di un abbonamento | Abbonato + piano + decorrenza = un abbonamento vivo, che consuma quota |
| [0011](02-piani-e-abbonati/0011-ciclo-di-vita-dell-abbonamento.md) | Ciclo di vita dell'abbonamento | La macchina a stati, i passaggi ammessi e quelli vietati, con la cronologia |

### Epica 03 — Rinnovi e scadenze

Alla fine dell'epica le cose succedono **da sole il giorno giusto**: le scadenze nascono, gli avvisi partono nei
termini di legge, i cambi di piano si conguagliano.

| # | Storia | In una riga |
|---|---|---|
| [0012](03-rinnovi-e-scadenze/0012-calendario-dei-rinnovi.md) | Calendario dei rinnovi | La lavorazione giornaliera che apre il periodo nuovo e genera la scadenza, senza doppioni |
| [0013](03-rinnovi-e-scadenze/0013-avviso-di-rinnovo-con-preavviso.md) | Avviso di rinnovo con preavviso | L'avviso dovuto per legge prima del rinnovo, con la prova di averlo mandato |
| [0014](03-rinnovi-e-scadenze/0014-cambio-di-piano-con-conguaglio.md) | Cambio di piano con conguaglio | Aumento subito con conguaglio proporzionale, riduzione a fine periodo |
| [0015](03-rinnovi-e-scadenze/0015-sospensione-e-ripresa.md) | Sospensione e ripresa | Il periodo si ferma e riparte, e le date si spostano di conseguenza |
| [0016](03-rinnovi-e-scadenze/0016-uscita-verso-il-documento-contabile.md) | Uscita verso il documento contabile | La scadenza diventa fattura in BillGrove, o esce in un file se BillGrove non c'è |

### Epica 04 — Incassi e solleciti

Alla fine dell'epica si sa **chi non ha pagato**, il recupero parte da solo e finisce con una conseguenza
prevedibile — senza che appgrove abbia toccato un euro.

| # | Storia | In una riga |
|---|---|---|
| [0017](04-incassi-e-solleciti/0017-autorizzazione-all-addebito.md) | Autorizzazione all'addebito | Il riferimento del mandato e il suo stato, senza mai una coordinata bancaria |
| [0018](04-incassi-e-solleciti/0018-registrazione-manuale-dell-incasso.md) | Registrazione manuale dell'incasso | Segnare incassato, parziale o fallito, con il motivo — la via predefinita |
| [0019](04-incassi-e-solleciti/0019-importazione-degli-esiti-da-file.md) | Importazione degli esiti da file | Un file dal fornitore o dalla banca chiude cento scadenze in un colpo, con anteprima |
| [0020](04-incassi-e-solleciti/0020-collegamento-in-sola-lettura-al-fornitore.md) | Collegamento in sola lettura al fornitore | Leggere gli esiti dal conto del cliente senza mai disporre un pagamento — fermata di escalation |
| [0021](04-incassi-e-solleciti/0021-catena-dei-solleciti.md) | Catena dei solleciti | Il piano di recupero: primo sollecito subito, poi a scalare, con prova di ogni invio |
| [0022](04-incassi-e-solleciti/0022-sospensione-automatica-per-mancato-incasso.md) | Sospensione automatica per mancato incasso | Esaurita la catena, l'abbonamento si sospende — con preavviso e con il freno a mano |

### Epica 05 — Portale dell'abbonato

Alla fine dell'epica l'abbonato — che non è nostro utente e non ha credenziali — può vedere il suo abbonamento e
disdirlo con un clic, come la legge pretende.

| # | Storia | In una riga |
|---|---|---|
| [0023](05-portale-dell-abbonato/0023-pagina-dell-abbonato-con-collegamento-firmato.md) | Pagina dell'abbonato con collegamento firmato | Una pagina pubblica per abbonamento, raggiungibile senza credenziali e senza indovinare nulla |
| [0024](05-portale-dell-abbonato/0024-disdetta-con-un-pulsante.md) | Disdetta con un pulsante | Il pulsante di disdetta richiesto dalla legge tedesca, con ricevuta e prova |
| [0025](05-portale-dell-abbonato/0025-richiesta-di-cambio-piano.md) | Richiesta di cambio piano | L'abbonato chiede, il cliente approva: nessun cambio silenzioso |
| [0026](05-portale-dell-abbonato/0026-difese-della-superficie-pubblica.md) | Difese della superficie pubblica | Scadenza del gettone, limiti di frequenza, nessuna enumerazione, nessun dato di troppo |

### Epica 06 — Metriche dei ricavi ricorrenti

Alla fine dell'epica il titolare sa **quanto entra ogni mese** e **perché è cambiato**, con numeri che non si
riscrivono all'indietro.

| # | Storia | In una riga |
|---|---|---|
| [0027](06-metriche-dei-ricavi-ricorrenti/0027-istantanea-mensile-dei-ricavi.md) | Istantanea mensile dei ricavi | Il ricavo ricorrente normalizzato a mese, fotografato e non più ricalcolato |
| [0028](06-metriche-dei-ricavi-ricorrenti/0028-scomposizione-della-variazione.md) | Scomposizione della variazione | Nuovo, espansione, contrazione, abbandono: perché il numero è cambiato |
| [0029](06-metriche-dei-ricavi-ricorrenti/0029-abbandono-e-durata-media.md) | Abbandono e durata media | Quanti se ne vanno, quanto restano, e quanto vale in media un abbonato |
| [0030](06-metriche-dei-ricavi-ricorrenti/0030-previsione-degli-incassi-ricorrenti.md) | Previsione degli incassi ricorrenti | Cosa è già impegnato nei prossimi mesi, distinto da ciò che è solo sperato |

### Epica 07 — Esposizione conversazionale e prove end-to-end

Alla fine dell'epica l'app è comandabile da una chat con la regola «lettura libera, scrittura con conferma», e i
due percorsi che contano — quello interno e quello dell'abbonato — sono coperti da prove vere.

| # | Storia | In una riga |
|---|---|---|
| [0031](07-esposizione-conversazionale-e-prove/0031-strumenti-di-lettura.md) | Strumenti di lettura | I cinque strumenti che rispondono alle domande sul ricorrente, con dati minimizzati |
| [0032](07-esposizione-conversazionale-e-prove/0032-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con conferma | Sei strumenti che scrivono, tutti con bozza; tre con conferma obbligatoria |
| [0033](07-esposizione-conversazionale-e-prove/0033-percorso-end-to-end-interno.md) | Percorso end-to-end interno | Dal piano al sollecito: il percorso `[J-ABBONATI]` sullo stack locale reale |
| [0034](07-esposizione-conversazionale-e-prove/0034-percorso-end-to-end-dell-abbonato.md) | Percorso end-to-end dell'abbonato | Il portale pubblico e la disdetta con un clic, provati come li vive l'abbonato |

**Totale**: 7 epiche, 34 storie.

---

## 9. Estensioni della console di amministrazione

Servono estensioni, ma poche e tutte di **diagnosi**: SubGrove è l'unica app della suite che manda messaggi a
persone che non sono nostri utenti (avvisi di rinnovo, solleciti) e l'unica che espone una **superficie pubblica**
senza credenziali; entrambe le cose vanno sorvegliate dalla piattaforma, per volume e per abuso, senza mai
guardare i contenuti. Serve inoltre una vista sullo stato della lavorazione giornaliera dei rinnovi, perché un
suo arresto silenzioso è il guasto che il cliente scopre per ultimo e nel modo peggiore.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

### 10.1 🛑 Il rapporto con gli abbonamenti **di piattaforma** — cosa si riusa e cosa no

È il punto più delicato del documento, e va risolto prima di progettare qualunque altra cosa. appgrove **ha già**
un sistema di abbonamenti: listino come codice, quote, abbonamento self-service (use case 0067, change
`0082`), riconciliazione dei ricavi (change `0083`), fornitore di pagamento come venditore di riferimento
([docs/09-pagamenti.md](../../../09-pagamenti.md)). Il modello di dominio è quasi lo stesso di SubGrove. La
differenza è **di chi sono gli abbonamenti**: quelli di piattaforma sono i contratti fra **appgrove e i suoi
clienti**; quelli di SubGrove sono i contratti fra **il cliente e i suoi clienti**. Il rischio di costruire due
volte la stessa macchina è reale e va disinnescato con una risposta esplicita.

**Cosa si riusa — e sono cose di valore.**

| Cosa | Come si riusa | Perché conviene |
|---|---|---|
| **La semantica del ciclo di vita** | SubGrove adotta gli stessi passaggi già ragionati per la piattaforma: prova → attivo → in ritardo con tolleranza → disdetto con accesso fino a fine periodo → cessato; aumento di piano **subito con conguaglio proporzionale**, riduzione **a fine periodo senza rimborso** ([docs/09-pagamenti.md](../../../09-pagamenti.md) dec. 22, 25, 26, 29) | Sono decisioni già prese, già discusse e coerenti con lo standard del settore. Riprenderle costa zero e fa risparmiare un giro di ragionamento; discostarsene senza motivo produrrebbe due prodotti che si comportano in modo diverso davanti alla stessa domanda |
| **La distinzione consumo/giacenza** e la regola che la riduzione di piano è sbarrata finché lo stato eccede il tetto (dec. 23) | Vale identica dentro SubGrove per i piani **del cliente**: se il piano di destinazione ha meno posti di quelli occupati, si blocca e si dice come rimediare | È la lezione più costosa del listino di piattaforma, già pagata una volta |
| **L'immutabilità del prezzo vivo** (dec. 35) | La storia `0007` la impone ai piani del cliente: prezzo nuovo, vecchio archiviato, chi è dentro resta sul suo | Stessa regola, stesso motivo — e la si può spiegare al cliente con le stesse parole |
| **Il vocabolario italiano** già fissato dal backoffice per la sezione abbonamenti (change `0082`) | Stessi termini a schermo: «cambio programmato», «fine periodo», «pagamento in ritardo» | Un cliente che usa entrambe le superfici non deve imparare due lingue |
| **Il renderer condiviso delle comunicazioni** (change `0079`) | Avvisi di rinnovo e solleciti passano da lì | Non è codice degli abbonamenti: è infrastruttura comune, e va usata |

**Cosa deve restare separato — e perché non è negoziabile.**

1. **I dati.** Gli abbonamenti di piattaforma vivono nel servizio centrale e **non** hanno un `tenant_id` nel
   senso di SubGrove: sono la piattaforma che guarda i propri clienti. Gli abbonamenti di SubGrove vivono in
   `app_abbonati` e sono rigorosamente filtrati per account. Mescolarli violerebbe l'invariante numero uno, e
   nemmeno la regola «vietate le interrogazioni fra schemi diversi» lo permetterebbe.
2. **Il fornitore di pagamento.** La piattaforma vende attraverso un **venditore di riferimento** che incassa in
   nome proprio. Se SubGrove passasse da quel conto, appgrove diventerebbe il venditore dei servizi del proprio
   cliente — cioè esattamente la cosa che il §5.2 esclude, con il contorno di responsabilità fiscali e
   regolamentari che ne consegue. **Non è una scorciatoia da valutare: è un errore da vietare per iscritto.**
3. **Il listino come codice.** Il listino di piattaforma è un file nel repository che cambia via richiesta di
   modifica revisionata. I piani del **cliente** sono **dati a runtime**: li crea e li cambia lui, quando vuole,
   senza che nessuno di noi guardi. È una differenza architetturale, non di forma: qualunque tentativo di far
   servire lo stesso meccanismo a entrambi finirebbe o con un editor di prezzi a runtime nella piattaforma
   (esplicitamente escluso, dec. 34) o con i clienti che aprono richieste di modifica al nostro repository.
4. **I varchi.** Abilitazione, ruoli e quota di piattaforma decidono se il cliente **può usare SubGrove**. Lo
   stato dell'abbonamento di un abbonato dentro SubGrove non decide **nulla** su appgrove: è un dato di business
   del cliente. Confonderli produrrebbe il difetto peggiore possibile — un cliente che perde l'accesso alla
   propria app perché un suo iscritto non ha pagato.
5. **I solleciti.** La piattaforma ha deciso di **non** fare solleciti propri: li fa il fornitore di pagamento
   (dec. 26). SubGrove **deve** farli, perché nessun fornitore li fa per il suo cliente. Non c'è codice da
   riusare: c'è da costruirlo (epica 04). Vale la pena dirlo, perché è la parte in cui l'illusione del riuso
   costerebbe di più.

**Punto aperto per la piattaforma — non lo decido io.** Esiste una parte davvero comune ed è **piccola e pura**:
l'aritmetica del calendario ricorrente (dato un ciclo e una decorrenza, quando finisce il periodo; quanto vale il
conguaglio proporzionale di un cambio a metà periodo; come si normalizza a mese un importo trimestrale o annuale
per misurare il ricavo ricorrente). Sono funzioni senza persistenza, senza fornitore e senza account, oggi
scritte una volta nel servizio centrale e destinate a essere riscritte in SubGrove. **Proposta**: estrarle in una
libreria condivisa (`services/commons`, area «ricorrenza»), con le proprie prove, quando la seconda
implementazione esiste davvero e non prima — estrarre su una sola implementazione è il modo classico di
astrarre la cosa sbagliata. **È una decisione di piattaforma**, non di questa app: la registro qui e nel punto
aperto n. 1, e la storia `0012` la richiama esplicitamente perché sia chi la implementa a sollevare la mano.

### 10.2 Sinergie con le altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **02 BillGrove** (fatturazione) | **alimenta** | La catena del documento contabile: la scadenza ricorrente di SubGrove è ciò che fa nascere la fattura. È la sinergia più forte e la più semplice da spiegare al cliente (storia `0016`) |
| **03 CashGrove** (incasso crediti) | **alimenta / confina con** | Una scadenza non incassata è un credito. Confine dichiarato: SubGrove insegue il **mancato incasso ricorrente** e ne trae la conseguenza contrattuale (sospendere, disdire); CashGrove insegue il **credito da fattura** e ne trae la conseguenza finanziaria (recupero, stralcio). Se ci sono entrambe, SubGrove consegna e smette di sollecitare (storia `0021`) |
| **04 LeadGrove** (vendite) e **01/02** | **condivide dati con** | L'**anagrafica clienti condivisa** del §6 del catalogo: l'abbonato *è* un cliente. SubGrove tiene un riferimento all'anagrafica condivisa, non una copia autorevole (storia `0008`) |
| **07 BookGrove** (prenotazioni) | **si sovrappone in parte a** | In palestra convivono l'abbonamento (SubGrove) e la prenotazione del corso (BookGrove). Confine: SubGrove dice **se il rapporto è in regola**, BookGrove dice **se c'è posto giovedì alle 19**. La domanda «l'abbonato può prenotare?» le mette in relazione ed è un evento, non una chiamata diretta |
| **18 VaultGrove** (documenti e conformità) | **dipende da, per delega** | Tutto ciò che è documento — contratto firmato, certificato medico, consenso — sta lì, non qui. È il presidio pratico contro l'articolo 9 (§6) |
| **20 InsightGrove** (analitiche) | **alimenta** | Le istantanee mensili dell'epica 06 sono materia prima naturale per il cruscotto trasversale |

**Riga di lettura.** SubGrove ha senso **anche da sola** — il foglio di calcolo che sostituisce è un problema
completo in sé — ma dentro la suite guadagna il pezzo che le manca per definizione: l'anagrafica condivisa in
ingresso e il documento fiscale in uscita.

**Sovrapposizioni da evitare.** Tre, e conviene averle scritte adesso:

1. **Con CashGrove**: due catene di solleciti sullo stesso denaro sono il modo migliore per mandare due messaggi
   diversi allo stesso cliente lo stesso giorno. Confine sopra, e una regola operativa nella storia `0021`.
2. **Con i verticali di settore** (21 SalonGrove, e simili): se un verticale gestisce anche gli abbonamenti,
   SubGrove non va venduta accanto ad esso. È una scelta di catalogo, non tecnica.
3. **Con la sezione «Abbonamenti» del backoffice di piattaforma**: due voci di menu con lo stesso nome che
   parlano di cose diverse. Mitigazione già presa: l'app si chiama `abbonati` e le sue sezioni parlano di
   «abbonati», «piani», «scadenze» — mai di «i tuoi abbonamenti», che è la sezione della piattaforma (§3).

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **Estrarre o no l'aritmetica del ricorrente in una libreria condivisa** (§10.1) | È una decisione di piattaforma con effetti su `services/commons` e sul servizio centrale, non di una singola app; e va presa quando la seconda implementazione esiste, non prima | **piattaforma** (sviluppatore), al momento della storia `0012` |
| 2 | **Componente a percentuale sull'incassato: proposta di eliminarla** (§5.1) | Contrasta con un vincolo non negoziabile del listino ed è misurabile solo stando dentro il flusso del denaro, dove non stiamo | **sviluppatore** — prezzi, fermata di escalation |
| 3 | **L'articolo di legge italiano** su preavviso di 30 giorni e canale digitale di recesso (§2.3, §2.7) | La fonte consultata è associativa e non lo cita; il termine esatto cambia i valori predefiniti della storia `0013` | **revisione legale** ([docs/_REVISIONE-LEGALE.md](../../../_REVISIONE-LEGALE.md)) |
| 4 | **Il piano gratuito conviene?** (§5.3) | Attira il segmento che non converte, ma è la porta d'ingresso naturale alla suite: è posizionamento commerciale | **sviluppatore** — prezzi |
| 5 | **Nome del piano come possibile dato personale** (§6) | Se un cliente battezza un piano con il nome di un abbonato, un dato personale finisce in una tabella non esportata. L'avviso a schermo basta? | **sviluppatore** — classificazione dati personali |
| 6 | **Colore-categoria `red`** (§3) | È l'unico non ancora proposto dalle sorelle di catalogo, ma la piattaforma potrebbe volerlo tenere libero per non confonderlo con gli stati d'errore. Ripiego: `teal` | **piattaforma**, quando i colori si assegnano davvero |
| 7 | **Collegamento in sola lettura al fornitore di incasso** (storia `0020`) | Introduce un responsabile esterno del trattamento e credenziali del cliente: è un effetto verso l'esterno | **sviluppatore** — fermata di escalation |
| 8 | **Conservazione della prova di invio** di avvisi e solleciti (§6) | Va tenuta almeno quanto la prescrizione dei diritti nascenti dal contratto, che non so quantificare | **revisione legale** |

**Rischi noti**

- **Confusione fra i due mondi degli abbonamenti** — il rischio numero uno. Se si avvera, si costruisce due volte
  la stessa macchina, oppure — peggio — si prova a farne servire una sola a entrambi e si finisce con il cliente
  che perde l'accesso all'app perché un suo iscritto non ha pagato. *Attenuazione*: identificativo distinto
  (`abbonati`), §10.1 letto prima di implementare, e il divieto scritto di passare per il conto del fornitore di
  piattaforma.
- **Slittamento verso l'incasso** — «facciamo pagare direttamente qui, è comodo». Se si avvera, si presta un
  servizio di pagamento regolato senza esserlo. *Attenuazione*: §5.2 e la storia `0020` che si ferma
  deliberatamente alla sola lettura.
- **Articolo 9 dalla finestra** — la casella «scadenza del certificato medico» che sembra innocua. *Attenuazione*:
  esclusione scritta al §6, delega a VaultGrove, nessun campo per documenti.
- **La catena dei solleciti diventa spam** — messaggi ripetuti a persone che non sono nostri utenti, su recapiti
  che il cliente ha caricato senza che nessuno verifichi da dove vengano. *Attenuazione*: tetto ai tentativi per
  scadenza, prova d'invio, disattivazione per abbonato, e la vista di sorveglianza sui volumi in console di
  amministrazione (§9).
- **Dipendenza dalla lavorazione giornaliera** — se il calendario dei rinnovi non gira per tre giorni, nessuno se
  ne accorge finché non manca un avviso di legge. *Attenuazione*: la lavorazione è idempotente e recupera i
  giorni saltati (storia `0012`), e il suo stato è visibile in console (§9).

**Fuori dimensionamento**: nessuno. 7 epiche (fascia 4-7), da 4 a 6 storie per epica (fascia 4-8), 34 storie in
tutto (fascia 20-45).
