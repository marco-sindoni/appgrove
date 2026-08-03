# RepGrove — descrizione dell'applicazione

**Numero di catalogo**: 17 · **Tipo**: trasversale · servizi locali · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 17](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** RepGrove fa tre cose per un'attività con una o più sedi fisiche. **(a)** Chiede la recensione a
**tutti** i clienti serviti, con lo stesso messaggio e senza guardare se sono contenti, e conserva la prova di
averlo fatto in modo non selettivo. **(b)** Raccoglie in un unico flusso le recensioni che quelle sedi ricevono
sulle piattaforme a cui il cliente ha dato accesso (Google, Trustpilot), le mostra ordinate e avvisa quando ne
arriva una negativa. **(c)** Prepara le **bozze** di risposta e le pubblica solo dopo che una persona le ha
approvate. Attorno a questo ci sono il punteggio di reputazione per sede, la lettura dei temi che ricorrono nelle
recensioni, un riquadro da incorporare nel sito del cliente e la dichiarazione di trasparenza che la normativa
europea impone a chi mostra recensioni.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50 con **almeno una sede fisica dove il
cliente entra**: parrucchieri e centri estetici, ristoranti e bar, officine, studi professionali, artigiani che
lavorano a domicilio, palestre, piccole catene con due-cinque punti vendita. Chi compra è il titolare, che
sull'argomento «recensioni» è insieme il più interessato e il più esposto: sa che le stelle spostano il fatturato
e non sa cosa gli è permesso fare. Chi usa tutti i giorni è chi sta alla cassa o al banco, più il titolare che
scrive le risposte la sera.

**Quale problema toglie.** Oggi la reputazione si governa in tre modi, tutti costosi. Il primo è **non chiedere
mai**: il cliente contento se ne va senza dire niente, l'unico che scrive è quello arrabbiato, e la media
scivola verso il basso senza che sia successo niente di grave. Il secondo è **chiedere a memoria**, quando ci si
ricorda e a chi si simpatizza: è esattamente il comportamento che le piattaforme puniscono, perché è selettivo.
Il terzo è **comprare uno strumento che fa la cosa vietata**: buona parte dei prodotti della categoria mette in
mezzo un «sondaggio interno» — «come è andata?», e se rispondi bene ti mando al modulo pubblico, se rispondi male
finisci in una casella di posta interna. È una pratica proibita da Google e da Trustpilot (§2.3), e il conto lo
paga il cliente, non il fornitore del software: è il **suo** profilo a essere rimosso o marchiato.

Il vero costo che RepGrove toglie non è il tempo: è il **rischio di fare la cosa sbagliata con la coscienza
tranquilla**, perché lo strumento che l'ha suggerita sembrava professionale.

**Cosa NON fa** — e questa non è una nota a piè di pagina, è il posizionamento del prodotto:

> 🚫 **Le pratiche che RepGrove rifiuta di implementare.** Ognuna è tecnicamente banale, richiesta dal mercato e
> presente nei concorrenti. Non ci sarà nessun modo di attivarle da un'impostazione nascosta, perché **non
> esistono nel codice**.
>
> 1. **Filtro dei clienti scontenti (in inglese *review gating*).** Nessuna domanda-filtro prima dell'invito,
>    nessuna deviazione di chi risponde male verso un modulo privato, nessun invito condizionato a un voto.
>    L'invito parte identico per tutti i clienti ammissibili. *Perché*: Google vieta di «sollecitare
>    selettivamente recensioni positive»; Trustpilot vieta di «scegliere quali clienti invitare» e commina
>    l'avviso al consumatore sul profilo, la sospensione dell'account e azioni legali (§2.3).
> 2. **Lo stesso filtro travestito da «sondaggio interno di qualità».** È la forma in cui la pratica viene venduta
>    oggi. Cambiare il nome non cambia l'effetto: se l'esito del sondaggio decide chi riceve il collegamento
>    pubblico, è filtro. RepGrove **può** raccogliere un giudizio interno (è utile), ma quel giudizio **non entra
>    mai** nella decisione di invitare (storia 0012).
> 3. **Incentivi.** Nessun campo «sconto in cambio di recensione», nessun buono, nessun concorso, nessun caffè
>    offerto. Il controllo dei modelli di messaggio (storia 0013) **rifiuta** i testi che promettono un vantaggio.
> 4. **Richieste di contenuto specifico.** Niente «chiedi al cliente di citare il tuo nome»: Google vieta
>    espressamente di chiedere che la recensione contenga contenuti specifici, compreso il nome di un dipendente.
> 5. **Obiettivi di raccolta per dipendente.** Nessuna classifica interna «chi ha portato più recensioni»: le
>    quote di recensioni assegnate al personale sono vietate.
> 6. **Pressione in loco.** Nessuna modalità chiosco o tavoletta condivisa al banco: chiedere la recensione
>    mentre il cliente è dentro il locale è espressamente vietato.
> 7. **Recensioni finte, comprate o scritte dall'intelligenza artificiale.** L'assistente scrive **risposte**, mai
>    recensioni. La compravendita di recensioni è sanzionata in Italia da 5.000 a 50.000 euro (§2.3).
> 8. **Soppressione delle recensioni negative.** Nessun modello di diffida, nessuna funzione «fai sparire questa
>    recensione». L'unica strada offerta è la segnalazione motivata alla piattaforma per i casi che la legge
>    prevede (storia 0021).
> 9. **Riquadro per il sito che nasconde le stelle basse.** Il riquadro pubblico si può ordinare e limitare nel
>    numero, **mai filtrare per voto**: mostrare solo il meglio spacciandolo per il tutto è una pratica
>    ingannevole (§2.3).
> 10. **Raccolta di recensioni fuori dai canali autorizzati.** Nessuna estrazione automatica dalle pagine delle
>     piattaforme: si legge solo attraverso le interfacce ufficiali e solo per le sedi di cui il cliente è
>     proprietario (§2.3, §11.2).

Fuori perimetro anche, per scelta di prodotto e non per divieto: la pubblicazione di contenuti promozionali sui
profili (è marketing, sta in 16 ReachGrove), la gestione della scheda dell'attività su decine di elenchi
(*presence management*: è un altro prodotto), il monitoraggio dei concorrenti (§11.2: quello che le piattaforme
permettono di leggere sui concorrenti è troppo poco per costruirci una funzione onesta).

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`. Un assistente generico scrive una
risposta a una recensione meglio di molti titolari, e questo è proprio il pezzo che vale meno. Quello che non può
fare è: essere collegato al profilo dell'attività, sapere **chi** è stato servito ieri e non è ancora stato
invitato, garantire che l'invito sia partito per tutti allo stesso modo, e conservarne la prova. Il valore sta nel
flusso di lavoro, nel collegamento autorizzato alle piattaforme e nella prova di equità — non nel testo.

---

## 2. Mercato e analisi in rete

> Compilata dopo 13 ricerche e letture mirate ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4).
> Ciò che non è stato trovato è **dichiarato** al §2.7, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| **NiceJob** | Canada/Stati Uniti, servizi a domicilio | inviti automatici alle recensioni, promemoria, riquadri per il sito | **75 $/mese** piano *Reviews*, **125 $/mese** piano *Pro*, prova di **14 giorni** — **pagina ufficiale** | [get.nicejob.com/pricing](https://get.nicejob.com/pricing) |
| **Feedaty** (Zoorate) | Italia, commercio e servizi | raccolta di recensioni verificate, riquadri, risposte assistite, moduli «Location» e «Reputation Manager» a parte | **da 49 €/mese** piano *Light* fino a **150 inviti/mese**; piani superiori a preventivo — **pagina ufficiale** | [feedaty.com/piani](https://www.feedaty.com/piani/) |
| **Podium** | Stati Uniti, servizi locali | recensioni + messaggistica + pagamenti, molto largo | **prezzo non pubblicato**: la pagina dei prezzi rimanda al commerciale | [podium.com/pricing](https://www.podium.com/pricing/) |
| **Birdeye** | Stati Uniti, multi-sede | recensioni, presenza sugli elenchi, sondaggi, messaggistica | **prezzo non pubblicato** | (nessuna pagina di prezzo pubblica) |
| **Partoo** | Francia, reti di punti vendita | presenza locale + recensioni, venduto per numero di sedi | **da ~149 €/mese** — dato da **fonti secondarie francesi**, non da pagina ufficiale | [societe.tech — Partoo, avis et prix](https://www.societe.tech/partoo-avis-avantages-prix/) |
| **Trustmary** | Finlandia | raccolta e mostra delle recensioni, riquadri | pagina dei prezzi **non leggibile** senza esecuzione di script; fonti secondarie indicano 19–59 $/mese — **non verificato** | [trustmary.com/pricing](https://trustmary.com/pricing/) |

**Lettura.** Il mercato si divide in due. Da un lato le suite americane (Podium, Birdeye) che non pubblicano il
prezzo, vendono per telefono, legano a contratti annuali e costano molto più di quanto una micro-impresa europea
spenda per un software (§2.5). Dall'altro gli strumenti europei di raccolta (Feedaty in Italia, Partoo in Francia)
che partono da 49–149 €/mese e sono tarati su chi ha già una struttura. **Il segmento scoperto è il più numeroso**:
il parrucchiere con una sede, l'officina con due, che vogliono chiedere la recensione a tutti i clienti senza
sbagliare e senza firmare un contratto annuale. È lo stesso segmento che una rassegna indipendente descrive così:
«gli strumenti per le recensioni partono da 289 $/mese, le piccole imprese vogliono solo chiedere ai clienti e
tenere traccia delle risposte» ([microgaps.com](https://www.microgaps.com/gaps/small-business-review-management-dashboard)).

Su cosa ci si può differenziare, in ordine: **(1)** il rifiuto esplicito e verificabile delle pratiche vietate,
che oggi nessuno usa come argomento di vendita perché quasi tutti le implementano; **(2)** il prezzo per sede
dichiarato in pagina, senza contratto annuale; **(3)** la dichiarazione di trasparenza generata da sola, che è un
adempimento europeo che i prodotti americani non hanno motivo di conoscere.

### 2.2 Prezzi praticati nel dominio

- **Unità di misura prevalente: la sede** (in inglese *location*). Partoo fattura per numero di punti vendita;
  Feedaty vende un modulo «Location» a parte; Podium e Birdeye applicano un canone per sede e le fonti secondarie
  raccolgono lamentele proprio sui costi aggiuntivi «per utenti, sedi o moduli in più» (§2.5). NiceJob è
  l'eccezione: canone unico, e le stesse fonti dicono che la gestione multi-sede è il suo punto debole.
  L'unità secondaria è il **volume di inviti** (Feedaty: 150/mese nel piano base, 500 nel successivo).
- **Fascia rilevata**: 49–149 €/mese in Europa per la raccolta strutturata; 75–125 $/mese per lo strumento
  monoprodotto nordamericano; oltre i 250 $/mese per le suite. La scheda di catalogo indica 19–39 €/mese per sede,
  cioè **sotto tutto ciò che ho rilevato**: è una scelta di posizionamento (§5), non un dato di mercato.
- **Piano gratuito**: non l'ho trovato in nessuno dei prodotti di raccolta esaminati. Esistono livelli gratuiti sui
  soli **riquadri di mostra** (mostrare recensioni già esistenti), non sulla raccolta.
- **Prova gratuita**: 14 giorni in NiceJob (dato ufficiale), «prova su richiesta» in Feedaty. La prova a tempo è
  quindi la leva della categoria, non il piano gratuito — il contrario di quanto avviene nell'invio di
  comunicazioni (16 ReachGrove).
- **Distinzione onesta**: i prezzi di NiceJob e Feedaty vengono da **pagine ufficiali**; quelli di Partoo e
  Trustmary da fonti secondarie o non sono stati letti; Podium e Birdeye **non pubblicano prezzi**.

### 2.3 Obblighi normativi del settore

Questo dominio è **molto più regolato di quanto sembri**, ed è la sezione che detta il prodotto. Quattro corpi di
regole si sovrappongono: le regole contrattuali delle piattaforme, la nuova legge italiana sulle recensioni, la
disciplina europea sulle pratiche commerciali scorrette e — solo per il mercato statunitense — la regola della
Commissione federale per il commercio.

**1. Regole di Google (contrattuali, ma con l'effetto di una legge sul cliente).**
La politica sui contenuti generati dagli utenti di Google Maps vieta al commerciante di «sollecitare
selettivamente recensioni positive dai clienti» e di «offrire incentivi — pagamento, sconti, beni o servizi
gratuiti — in cambio della pubblicazione di una recensione, della sua modifica o della rimozione di una
recensione negativa». Vieta inoltre di «obbligare o mettere sotto pressione gli utenti perché lascino una
valutazione o scrivano una recensione mentre sono nei locali» e di «chiedere che la recensione contenga
contenuti specifici», comprese le richieste al personale in tal senso. L'unica pratica ammessa è sollecitare
genuinamente, senza incentivi e senza tentare di influenzare il voto.
Fonte: [Google — Prohibited & restricted content, Maps User Generated Content Policy](https://support.google.com/contributionpolicy/answer/7400114?hl=en).
Il canale ufficiale per chiedere è il collegamento (o codice a barre bidimensionale) generato dalla scheda
dell'attività, condivisibile per posta elettronica o messaggio:
[Google — Create a Google link or QR code to request reviews](https://support.google.com/business/answer/16816815?hl=en).
**Conseguenze sul prodotto**: storie 0012 (regola di equità), 0013 (controllo dei modelli), 0016 (registro di
equità), e il rifiuto n. 6 del §1 (nessuna modalità chiosco).

**2. Regole di Trustpilot.** Le linee guida per le imprese impongono di «invitare in modo coerente ed equo — il
che significa invitare tutti allo stesso modo, indipendentemente dal fatto che abbiano avuto un'esperienza
positiva o negativa» e vietano di «essere selettivi con gli inviti, scegliendo quali clienti invitare»,
compreso l'impostare l'invito in una fase del percorso del cliente che raggiunge solo i soddisfatti. Vietano gli
incentivi «sconti, codici promozionali, partecipazioni a concorsi, rimborsi, omaggi». Le conseguenze dichiarate
dell'abuso: blocco o sospensione dell'account, risoluzione del contratto, **avviso al consumatore pubblicato sul
profilo dell'azienda**, occultamento del punteggio e azioni legali.
Fonte: [Trustpilot — Guidelines for businesses, febbraio 2026](https://corporate.trustpilot.com/legal/for-businesses/guidelines-for-businesses/feb-2026).
Trustpilot ammette esplicitamente, oltre all'«invita tutti», un **sistema di selezione imparziale** — per esempio
invitare un cliente ogni tre — purché il criterio sia indipendente dalla soddisfazione
([Trustpilot — come rispettiamo le linee guida ICPEN](https://press.trustpilot.com/trustpilot-comments-how-we-comply-with-the-icpen-guidelines)).
**Conseguenza diretta sul prodotto**: la regola di equità della storia 0012 non è booleana, ha due forme —
*tutti* oppure *uno ogni N* — e nessuna delle due guarda il giudizio del cliente.

**3. Legge italiana 34/2026 (legge annuale sulle piccole e medie imprese), articoli 18-23, in vigore dal 7 aprile
2026.** È la novità che cambia il modello dati.
- **Articolo 18 — ambito**: recensioni relative a imprese di ristorazione, strutture ricettive, stabilimenti
  termali e attrazioni turistiche **situate in Italia**, indipendentemente da dove abbia sede la piattaforma.
- **Articolo 19 — requisiti**: la recensione è lecita se deriva da una **fruizione effettiva e personale** del
  servizio, è pubblicata **entro 30 giorni** dalla fruizione, riguarda aspetti effettivamente sperimentati ed è
  priva di qualsiasi incentivo. La documentazione fiscale (lo scontrino) crea presunzione di autenticità. Dopo
  **due anni** la recensione perde attualità e può essere rimossa.
- **Articolo 20 — divieto**: è vietato «l'acquisto e la cessione a qualsiasi titolo, anche tra imprenditori e
  intermediari, di recensioni online, apprezzamenti o interazioni».
- **Articolo 21** — associazioni di categoria come segnalatori attendibili, con corsia veloce verso le
  piattaforme.
- **Articolo 22 — sanzioni** irrogate dall'Autorità garante della concorrenza e del mercato: **500-5.000 euro**
  per le recensioni prive dei requisiti, **5.000-50.000 euro** per la compravendita.
Fonti: [Legal for Digital — legge PMI e recensioni online, analisi articolo per articolo](https://legalfordigital.it/azienda/legge-pmi-recensioni-online/) ·
[FIPE — false recensioni, la legge sulle PMI entra in vigore](https://www.fipe.it/2026/04/07/normativa-di-settore/false-recensioni-la-legge-sulle-pmi-entra-in-vigore/).
**Conseguenze sul prodotto**: la finestra dei 30 giorni diventa un vincolo di programmazione dell'invito, non un
dettaglio (storia 0015); il collegamento fra invito e **fruizione effettiva** va conservato (storia 0011); la
segnalazione di una recensione non conforme ha elementi obbligatori — indirizzo della recensione, motivo,
identità di chi segnala, dichiarazione di buona fede — ed è la sola strada offerta (storia 0021).
⚠️ **Le due fonti sono qualificate ma secondarie**: non ho letto il testo in Gazzetta Ufficiale (§2.7).

**4. Direttiva europea 2019/2161 («omnibus»), recepita in Italia con il decreto legislativo 26/2023.** Chi dà
accesso a recensioni dei consumatori deve dichiarare **se e come** verifica che provengano da consumatori che
hanno effettivamente usato il prodotto o il servizio. Non c'è obbligo di verificare; c'è obbligo di **dire la
verità su cosa si fa**. Le sanzioni per le pratiche commerciali scorrette arrivano al 4 % del fatturato annuo.
Fonti: [Feedaty — la direttiva omnibus e l'impatto sulle recensioni](https://www.feedaty.com/blog/direttiva-omnibus-impatto-sulle-recensioni/) ·
[Legalblink — come gestire e pubblicare recensioni a norma di legge](https://legalblink.it/post/come-gestire-e-pubblicare-recensioni-a-norma.html).
**Conseguenza sul prodotto**: il riquadro pubblico (storia 0024) **non è pubblicabile senza** la dichiarazione di
trasparenza generata dall'app a partire da com'è configurata davvero (storia 0025). È un adempimento che l'app
può togliere di mano al cliente per intero, ed è un argomento di vendita.

**5. Stati Uniti — regola 16 CFR parte 465 della Commissione federale per il commercio**, in vigore da ottobre
2024: vieta recensioni false, compravendita di recensioni, recensioni di persone interne non dichiarate e la
**soppressione** delle recensioni negative (per esempio con minacce legali infondate o rappresentando come
complete raccolte di recensioni che complete non sono). Le sanzioni civili arrivano a decine di migliaia di
dollari per violazione. ⚠️ **Non verificata sul testo ufficiale**: la pagina della Commissione ha risposto con un
diniego di accesso e il testo consolidato ha reindirizzato altrove (§2.7). Vale come indicazione di direzione per
il mercato non europeo, non come citazione.

**6. Dati personali.** Il regolamento generale si applica: gli inviti contengono nome e recapito del cliente; le
recensioni raccolte contengono il nome pubblico dell'autore e un testo libero scritto da un terzo. Vedi §6, che
contiene un avviso forte sull'articolo 9.

### 2.4 Integrazioni attese dal cliente

| # | Integrazione | Perché la chiedono | Fornitore esterno che tratterebbe dati? |
|---|---|---|---|
| 1 | **Profilo dell'attività su Google** | è la piattaforma che sposta il fatturato locale | **sì**: la lettura delle recensioni e la pubblicazione delle risposte passano dalle interfacce di Google, con delega del proprietario del profilo (storia 0007) |
| 2 | **Trustpilot** | seconda piattaforma per notorietà, obbligatoria per chi vende anche in rete | **sì**, con il contratto del cliente: serve un account Trustpilot for Business (storia 0008) |
| 3 | **Anagrafica clienti della suite** (04 LeadGrove) | non reinserire a mano i clienti da invitare | no: è dentro la piattaforma e passa da eventi, non da chiamate fra app (§10) |
| 4 | **Appuntamenti erogati** (07 BookGrove) | «invita chi è venuto ieri» è il caso d'uso naturale | no, stesso motivo |
| 5 | **Fatture emesse** (02 BillGrove) | per chi non prende appuntamenti, la fattura è la prova che il servizio c'è stato | no, stesso motivo |
| 6 | **Invio dei messaggi di invito** (posta elettronica, messaggi brevi) | senza un canale l'invito non parte | **sì, e non è opzionale**: è il fornitore che tratta i dati per nostro conto sul canale primario (storia 0014) |
| 7 | **Sito del cliente** (riquadro delle recensioni) | mostrare le stelle dove i visitatori già sono | no: il riquadro è servito da noi e ospitato in una cornice sul sito del cliente |
| 8 | **Altre piattaforme verticali** (TripAdvisor, Booking, The Fork, Facebook) | «i miei clienti scrivono lì» | **non nel perimetro iniziale**: ognuna ha condizioni proprie da verificare una per una (§11.3) |

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Le lamentele ricorrenti sulla categoria, e cosa diventano come requisito:

- **«il contratto annuale e la disdetta impossibile»** — è la lamentela numero uno e riguarda le suite americane:
  rinnovo automatico, finestra di disdetta stretta, richieste di cancellazione ignorate. Diventa un requisito
  negativo per noi: abbonamento mensile disdicibile da soli, che è già lo standard della piattaforma appgrove
  (`PRINCIPI-APPGROVE.md` §13). ⚠️ Le fonti sono blog di **concorrenti** dei prodotti criticati e vanno lette
  come parti in causa: [truereview.co — Birdeye vs Podium vs NiceJob](https://www.truereview.co/post/birdeye-vs-podium-vs-nicejob-vs-truereview-an-honest-comparison) ·
  [prospeo.io — alternative a Podium](https://prospeo.io/s/podium-alternatives).
- **«pago un supplemento per ogni cosa»** — utenti in più, sedi in più, moduli di intelligenza artificiale in più.
  Diventa: **una sola metrica** (le sedi), tutto il resto compreso (§3).
- **«voglio solo chiedere ai clienti e vedere le risposte»** — la rassegna indipendente di
  [microgaps.com](https://www.microgaps.com/gaps/small-business-review-management-dashboard) descrive il vuoto di
  mercato esattamente così. Diventa: la panoramica dell'app è **una** schermata con il punteggio, le recensioni
  nuove e quelle senza risposta; tutto il resto è secondario.
- **cosa non chiedono**: il confronto con i concorrenti (lo chiedono a parole, ma quando si spiega che i dati
  disponibili sono la sola media pubblica smette di interessare), l'analisi del sentimento come cruscotto a sé,
  la gestione della scheda su venti elenchi diversi.
- **cosa non sanno di volere, ed è ciò che li protegge**: la prova di aver invitato tutti. Nessuno la chiede,
  perché nessuno sa che gli servirà finché non gli arriva la contestazione.

### 2.6 Fonti consultate

1. [Google — Prohibited & restricted content (Maps User Generated Content Policy)](https://support.google.com/contributionpolicy/answer/7400114?hl=en) — **pagina ufficiale**: divieto di sollecitazione selettiva di recensioni positive, di incentivi, di pressione nei locali, di richieste di contenuto specifico e di quote al personale. È la fonte primaria dei rifiuti 1, 3, 4, 5, 6 del §1.
2. [Google Business Profile Help — Create a Google link or QR code to request reviews](https://support.google.com/business/answer/16816815?hl=en) — **pagina ufficiale**: esiste un collegamento (e un codice a barre bidimensionale) generato dalla scheda dell'attività, condivisibile per posta elettronica e messaggistica. È il canale che la storia 0014 usa: chiediamo attraverso lo strumento che Google stesso indica.
3. [Trustpilot — Guidelines for businesses (feb. 2026)](https://corporate.trustpilot.com/legal/for-businesses/guidelines-for-businesses/feb-2026) — **pagina ufficiale**: «invita in modo coerente ed equo», divieto di scegliere chi invitare e di incentivare; elenco delle sanzioni, fino all'avviso al consumatore sul profilo.
4. [Trustpilot — come rispettiamo le linee guida ICPEN](https://press.trustpilot.com/trustpilot-comments-how-we-comply-with-the-icpen-guidelines) — ufficiale: ammesso, oltre a «invita tutti», un criterio imparziale come «uno ogni tre». Da qui la doppia forma della regola di equità (storia 0012).
5. [Google Business Profile APIs — Work with review data](https://developers.google.com/my-business/content/review-data) — **documentazione ufficiale**: si possono elencare le recensioni di una sede, leggerne una, rispondere e cancellare la risposta; serve autenticazione delegata del proprietario del profilo. È il contratto tecnico della storia 0007.
6. [Trustpilot developers — Service Reviews API](https://developers.trustpilot.com/service-reviews-api/) e [Business Units overview](https://developers.trustpilot.com/business-units-api-overview/) — **documentazione ufficiale**: identificativo dell'unità aziendale necessario per leggere le recensioni e creare inviti; parte pubblica con chiave, parte privata con delega dell'utente aziendale; serve un account Trustpilot for Business con il modulo di connessione. Contratto tecnico della storia 0008.
7. [Legal for Digital — legge PMI e recensioni online](https://legalfordigital.it/azienda/legge-pmi-recensioni-online/) — articoli 18-23 della legge 34/2026: ambito, requisiti, 30 giorni, decadenza a due anni, divieto di compravendita, sanzioni dell'Autorità garante.
8. [FIPE — false recensioni, la legge sulle PMI entra in vigore](https://www.fipe.it/2026/04/07/normativa-di-settore/false-recensioni-la-legge-sulle-pmi-entra-in-vigore/) — conferma della data (7 aprile 2026), del termine di 30 giorni e del divieto di incentivi, dal punto di vista della federazione dei pubblici esercizi.
9. [Feedaty — la direttiva omnibus e l'impatto sulle recensioni](https://www.feedaty.com/blog/direttiva-omnibus-impatto-sulle-recensioni/) — obbligo di dichiarare **se e come** si verifica l'autenticità: da qui la storia 0025.
10. [Legalblink — come gestire e pubblicare recensioni a norma di legge](https://legalblink.it/post/come-gestire-e-pubblicare-recensioni-a-norma.html) — stessa disciplina vista dal lato di chi pubblica le recensioni su un sito: conferma che l'obbligo cade su chi mostra, cioè sul nostro cliente quando incorpora il riquadro.
11. [get.nicejob.com/pricing](https://get.nicejob.com/pricing) — **pagina ufficiale**: 75 $/mese e 125 $/mese, prova di 14 giorni.
12. [feedaty.com/piani](https://www.feedaty.com/piani/) — **pagina ufficiale**: 49 €/mese fino a 150 inviti mensili; moduli «Location» e «Reputation Manager» a pagamento separato.
13. [podium.com/pricing](https://www.podium.com/pricing/) — **pagina ufficiale**, verificata come **priva di prezzi**: rimanda al contatto commerciale. È un dato, non un buco.
14. [microgaps.com — il vuoto di mercato nella gestione delle recensioni per piccole imprese](https://www.microgaps.com/gaps/small-business-review-management-dashboard) — «gli strumenti partono da 289 $/mese, le piccole imprese vogliono solo chiedere e tenere traccia».
15. [truereview.co — confronto Birdeye/Podium/NiceJob](https://www.truereview.co/post/birdeye-vs-podium-vs-nicejob-vs-truereview-an-honest-comparison) e [prospeo.io — alternative a Podium](https://prospeo.io/s/podium-alternatives) — lamentele su contratti annuali e supplementi. **Fonti di parte** (concorrenti), citate come tali.

### 2.7 Cosa NON sono riuscito a determinare

- **Il testo ufficiale della legge 34/2026.** Ho letto due fonti qualificate (uno studio legale specializzato e la
  federazione di categoria), non la Gazzetta Ufficiale. Prima di scrivere codice che dipende dai 30 giorni e dai
  due anni va letto l'articolato. *Serve*: il testo pubblicato e le linee guida attuative dell'Autorità garante,
  il cui schema risulta approvato il 23 aprile 2026 ma che non ho verificato.
- **Le condizioni sulla conservazione dei contenuti di Google.** La pagina ufficiale delle politiche della
  interfaccia dei luoghi ha risposto con un errore del server e i termini di servizio della piattaforma mappe si
  sono troncati in lettura. Le fonti secondarie concordano nel dire che i contenuti — recensioni comprese — non si
  possono memorizzare, con l'eccezione dell'identificativo del luogo. **Non l'ho verificato.** *Effetto sul
  progetto*: la storia 0010 fissa il principio prudenziale (si conserva il minimo indispensabile e si ricarica dal
  vivo) e il punto resta aperto (§11.2). Va detto che l'interfaccia che useremo è un'altra — quella del profilo
  dell'attività, con delega del proprietario, che ha condizioni proprie — ma nemmeno per quella ho trovato un
  termine di conservazione esplicito.
- **Il testo ufficiale della regola statunitense 16 CFR 465.** Diniego di accesso sul sito della Commissione,
  reindirizzamento sul testo consolidato. Resta come indicazione, non come citazione.
- **I prezzi di Podium e Birdeye**: non pubblicati, e non li ho stimati.
- **Il prezzo di Trustmary e di Trustindex**: la prima pagina non espone i prezzi senza esecuzione di script, la
  seconda ha chiuso la connessione. Le cifre che circolano nelle fonti secondarie non le riporto come rilevate.
- **Se l'invito a recensire sia una comunicazione commerciale** ai sensi dell'articolo 130 del Codice privacy
  italiano, e quindi quale sia la sua base giuridica corretta. È il punto aperto più importante dell'app
  (§6 e §11.1): non l'ho trovato risolto né in dottrina accessibile né in provvedimenti dell'autorità.
- **Quanto costa davvero l'accesso alle interfacce di Trustpilot**: la documentazione richiede «un account
  Trustpilot for Business con accesso al modulo di connessione», ma non ho trovato a quale piano corrisponda né
  quanto costi al cliente.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa. L'
> identificativo dell'app finisce nel nome dello schema del database, nei nomi delle code, nella rotta pubblica e
> nell'istanza del modulo di infrastruttura: cambiarlo dopo **non è una rinomina, è una migrazione di dati**.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `recensioni` | Rispetta `^[a-z][a-z0-9_]{0,30}$` (10 caratteri, minuscolo, sole lettere). Segue la convenzione viva nel repository, dove l'app numero uno è `fatture`: identificativo tecnico in italiano che dice **cosa l'app è**, non come è venduta oggi («RepGrove» è il nome di listino e i nomi di listino cambiano). Scartato `repgrove` perché lega infrastruttura e schema al marchio; scartato `reputazione` perché è più largo di quello che l'app fa (la reputazione comprende anche la presenza sugli elenchi e i contenuti social, che sono fuori perimetro, §1). Da qui discendono lo schema `app_recensioni`, la rotta `/api/recensioni/v1/*` e l'etichetta del percorso end-to-end `[J-RECENSIONI]`. |
| **Modello utente** | `multi` | Nel cliente tipo le persone in gioco sono almeno due e fanno cose diverse: chi sta alla cassa registra il servizio erogato (da cui nasce l'invito), il titolare scrive e approva le risposte. Il modello a utente singolo non ha il concetto di «chi ha approvato questa risposta», e qui serve: una risposta pubblicata è un atto **verso l'esterno**, a nome dell'azienda, e deve avere un autore. Vale a maggior ragione con più sedi, dove il responsabile di sede vede solo la sua. Cambiare dopo è scomodo. |
| **Porta locale** | `8117` | Convenzione del kit (8100 + numero di catalogo) per non far collidere le sessanta proposte. Da confermare con `./dev.sh services` al momento dello scaffolding: nel repository sono oggi occupate `8081` (fatture), `8082` (mini-CRM) e `9100` (autenticazione). |
| **Metrica di quota** | `sedi_monitorate` | La **sola** cosa che il piano limita: quante sedi l'app tiene collegate e sorvegliate in un dato momento. Tre ragioni. **(a)** È l'unità che il mercato usa (§2.2: Partoo fattura per punto vendita, Feedaty vende un modulo «Location», la scheda di catalogo dice «flat per sede»): il cliente sa già confrontarla. **(b)** Cresce esattamente con il valore ricevuto: due sedi sono due profili da sorvegliare, due flussi di recensioni, due punteggi. **(c)** — ed è la ragione che pesa di più — **le alternative sono pericolose**. Se la metrica fosse il numero di **inviti inviati**, un cliente vicino al tetto avrebbe un incentivo economico a mandare l'invito solo a chi pensa sia contento: il nostro listino spingerebbe verso la pratica che l'app esiste per rifiutare (§1). Se fosse il numero di **recensioni raccolte**, punirebbe il successo. Se fossero i **posti**, scoraggerebbe di dare l'accesso a chi sta in negozio, cioè a chi registra i servizi. |
| **Natura della metrica** | `stock` | È un tetto su ciò che esiste ora: «tre sedi collegate» significa che per collegarne una quarta bisogna scollegarne una o cambiare piano. Non si azzera a fine mese, perché una sede non si consuma. Conseguenza da tenere presente (`PRINCIPI-APPGROVE.md` §13): il passaggio a un piano inferiore è **bloccato** finché le sedi collegate superano il tetto di destinazione. Sbagliare qui costerebbe caro in entrambi i versi: contata come consumo, un cliente potrebbe collegare trenta sedi in un mese; contata come giacenza — che è la realtà — il conto è chiaro e prevedibile. |
| **Colore-categoria e icona** | `amber` · icona `star` (una stella a cinque punte con contorno) | Deve coincidere fra listino (`category`) e modulo frontend (`accentToken`). La stella è ambra in tutto il mondo: è l'unico caso del catalogo in cui il colore-categoria coincide con l'oggetto stesso dell'app, e rinunciarci per ragioni di distribuzione sarebbe una scelta contro l'utente. **Ho scartato `red`**, che pure è l'unico colore ancora poco usato fra le proposte scritte: in questa app il rosso deve significare «recensione negativa da prendere in carico» (storia 0020), e un'app tutta rossa smetterebbe di comunicare proprio dove serve — lo stesso ragionamento fatto da 16 ReachGrove. **Nota di coordinamento**: `amber` è già proposto da 03 CashGrove, 08 SpendGrove e 14 StockGrove; nessuna delle tre è adiacente a RepGrove nell'uso quotidiano, quindi la ripetizione è la meno dannosa possibile. Con sei colori-categoria e sessanta app la collisione è strutturale ed è un punto aperto di piattaforma (§11.6), non una decisione di questa scheda. |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Sede` | il luogo fisico di cui si governa la reputazione; è l'unità di quota | nome, indirizzo, fuso orario, stato (`attiva`, `sospesa`), settore dichiarato (serve per sapere se ricade nella legge 34/2026) | no (dati dell'azienda cliente) |
| `CollegamentoPiattaforma` | la delega con cui una sede è collegata a Google o a Trustpilot | piattaforma, identificativo esterno della sede o dell'unità aziendale, stato (`da_autorizzare`, `attivo`, `scaduto`, `revocato`), momento dell'ultima sincronizzazione, ultimo errore | no — ma custodisce **credenziali di delega**, che vanno cifrate e non compaiono mai nei registri |
| `RegolaDiEquita` | la regola con cui si decide **chi** viene invitato, uguale per tutti | forma (`tutti` \| `uno_ogni_n`), valore di `n`, decorrenza, chi l'ha impostata. **Ad accrescimento**: non si modifica, se ne aggiunge una nuova che vale da una data | no |
| `ServizioErogato` | il fatto che a un cliente sia stato erogato un servizio: è ciò che rende ammissibile l'invito | sede, riferimento al cliente, momento dell'erogazione, origine (`manuale`, `appuntamento`, `fattura`), riferimento del documento | **sì**: nome e recapito del cliente |
| `RichiestaRecensione` | l'invito effettivamente programmato o inviato | servizio erogato, canale, stato (`programmata`, `inviata`, `non_inviata`, `scaduta`), motivo dell'eventuale mancato invio, momento, esito del recapito | **sì**: recapito usato |
| `ModelloDiMessaggio` | il testo dell'invito, per lingua | lingua, oggetto, corpo, esito dell'ultimo controllo delle pratiche vietate, stato (`bozza`, `approvato`, `respinto`) | no |
| `Recensione` | una recensione ricevuta su una piattaforma collegata | piattaforma, identificativo esterno, voto, testo, nome pubblico dell'autore, momento di pubblicazione, presa in carico | **sì**: nome pubblico e testo scritto da un terzo — vedi l'avviso del §6 |
| `Risposta` | la replica pubblica a una recensione | recensione, testo, stato (`bozza`, `approvata`, `pubblicata`, `errore`), chi l'ha scritta, chi l'ha approvata, momento della pubblicazione | no (testo aziendale), ma può **citare** dati del cliente: il controllo di pubblicazione avvisa |
| `Segnalazione` | la richiesta motivata di rimozione di una recensione non conforme | recensione, motivo normativo, testo della motivazione, identità del segnalante, stato, esito della piattaforma | **sì**: identità di chi segnala |
| `PunteggioReputazione` | la fotografia periodica della reputazione di una sede | sede, periodo, media, volume, distribuzione dei voti, temi ricorrenti | no (è aggregato) |
| `RiquadroPubblico` | il riquadro incorporabile nel sito del cliente | sede, chiave pubblica, criterio di selezione (**mai** il voto), numero di recensioni mostrate, testo della dichiarazione di trasparenza | espone il **nome pubblico** già pubblico sulla piattaforma d'origine |

**Relazioni.** Una `Sede` ha molti `CollegamentoPiattaforma` (al massimo uno per piattaforma attivo), molte
`RegolaDiEquita` in successione temporale, molti `ServizioErogato`, un `RiquadroPubblico`. Da un `ServizioErogato`
nasce **al massimo una** `RichiestaRecensione`. Una `Recensione` arriva da un `CollegamentoPiattaforma`, ha al
massimo una `Risposta` viva e può avere una `Segnalazione`.

Due macchine a stati contano più delle altre:

- **richiesta di recensione**: `programmata` → `inviata` → (`recapitata` | `respinta`) → `scaduta` alla chiusura
  della finestra utile; oppure `programmata` → `non_inviata` con un motivo scritto (cliente senza recapito,
  cliente che ha chiesto di non essere contattato, regola *uno ogni N* che non l'ha selezionato, quota esaurita).
  **Il motivo è obbligatorio ed è la prova di equità**: un invito non partito senza motivo registrato è un difetto.
- **risposta**: `bozza` → `approvata` → `pubblicata`, con `errore` come ramo laterale. Non esiste alcuna
  transizione automatica da `bozza` a `pubblicata`: la conferma umana è nel modello, non nell'interfaccia.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria a identificativo
universale versione 7, colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e
cancellazione logica (`deleted_at`); schema `app_recensioni`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8). Fa eccezione, per natura, `RegolaDiEquita`: è ad
accrescimento e non si modifica mai, perché è materiale di prova.

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/recensioni.yaml`.

**Ragionamento.** La scheda di catalogo indica 19-39 €/mese flat per sede. Il mercato rilevato (§2.2) sta molto
più in alto: 49 €/mese il concorrente italiano più vicino, 75 $/mese quello nordamericano monoprodotto, oltre 149 €
le piattaforme multi-sede. La fascia del catalogo è quindi un **posizionamento deliberato sotto il mercato**, non
un dato rilevato, e va confermata da chi decide. Ha una logica: il cliente tipo di questa app è più piccolo del
cliente tipo di Feedaty e non ha un negozio in rete; e l'app non ha un costo variabile per recensione — il costo
variabile è solo il messaggio di invito, dell'ordine di frazioni di centesimo per la posta elettronica.

Propongo due piani, non tre: la metrica è la sede, e fra «una sede» e «più sedi» c'è tutto quello che serve per
distinguere. Nessun piano gratuito, coerentemente col mercato (§2.2: non ne ho trovato nessuno sulla raccolta) e
perché un piano gratuito con una sede collegata sarebbe indistinguibile dal piano base.

| Piano | Prezzo mensile | Prezzo annuale | Limite su `sedi_monitorate` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `sede_unica` | **19 €** | **190 €** (= 10× il mensile, «due mesi in regalo») | 1 | 14 giorni | il parrucchiere, il ristorante, l'officina: una sede, una scheda su Google, forse Trustpilot |
| `piu_sedi` | **39 €** | **390 €** | 5 | 14 giorni | la piccola catena da due a cinque punti vendita, con un responsabile per sede |

**Note obbligate.**

- **Due piani, non tre.** Aggiungerne uno è facile, toglierlo quando qualcuno ci sta sopra è difficile. Il caso
  «più di cinque sedi» esiste ma esce dal segmento dichiarato: è un punto aperto (§11.4), non un piano da
  inventare adesso.
- **Un limite lasciato vuoto significa illimitato**, non zero. Qui entrambi i piani hanno un tetto esplicito.
- **La prova gratuita ha senso qui**, a differenza di altre app: non c'è piano gratuito, e la categoria usa la
  prova a tempo come leva (NiceJob: 14 giorni, dato ufficiale). Con carta richiesta all'inizio, come da
  piattaforma. Attenzione a un punto pratico: in 14 giorni un'attività piccola raccoglie poche recensioni, quindi
  il valore visibile durante la prova è soprattutto **l'invito che parte da solo**, non il flusso pieno. La
  storia 0005 (dati di prova) conta più del solito.
- **Costo effettivo dell'incasso.** A 19 €/mese la parte fissa per transazione del fornitore di pagamento pesa in
  proporzione più che su un abbonamento da 40 €. Non è un veto, è un segnale: i rimedi naturali sono spingere
  l'annuale (che riduce il numero di transazioni da dodici a una) o alzare il prezzo base. Da valutare insieme al
  posizionamento.
- **Prezzi immutabili una volta vivi**: un cambio di prezzo si fa creando un prezzo nuovo e archiviando il
  vecchio; gli abbonati restano sul loro.
- **Quello che il listino non deve fare, mai**: contare gli inviti. Il §3 spiega perché — sarebbe un incentivo
  economico verso la pratica vietata. Se un giorno si volesse un piano «a volume», va prima risolto quel conflitto.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/recensioni.yaml`) si compila **insieme** allo sviluppatore: «niente contratto,
> niente produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

> 🛑 **Attenzione — categorie particolari (articolo 9): non sono la finalità dell'app, ma ci entrano dalla porta
> di servizio.** RepGrove non chiede a nessuno dati sulla salute, sulle convinzioni o sull'orientamento. Però fa
> due cose che li fanno entrare lo stesso:
> **(a)** importa nel proprio database il **testo delle recensioni**, scritto da terzi che non sono nostri utenti.
> Una recensione di un fisioterapista, di un centro estetico, di uno studio dentistico o di un veterinario
> contiene con ordinaria frequenza il motivo della visita — cioè un dato sulla salute. Il testo è già pubblico
> sulla piattaforma d'origine, ma **la nostra copia è un trattamento nostro**, con una base giuridica nostra;
> **(b)** l'esistenza stessa di un `ServizioErogato` presso certe attività **è** un dato sulla salute per
> deduzione: sapere che una persona è stata servita da uno studio medico dice qualcosa di lei anche se il campo
> «diagnosi» non esiste.
> Servono una base giuridica rafforzata e una valutazione d'impatto, e vanno decise **prima** dello scaffolding.
> Tre vie possibili, in ordine di prudenza: **(1)** escludere dal perimetro iniziale i settori sanitari e
> assimilati (è la via che consiglio: l'app resta per ristorazione, bellezza, artigianato, commercio);
> **(2)** non conservare il testo delle recensioni ma solo voto, identificativo e momento, ricaricando il testo
> dal vivo quando serve (ha effetti pesanti sulla ricerca e sull'analisi dei temi, storia 0023); **(3)** trattarlo
> con le garanzie rafforzate. **La decisione non è mia.**

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `servizio.nome_cliente` | `servizio_erogato.nome` | cliente dell'azienda cliente | anagrafico | personalizzare l'invito e non invitare due volte la stessa persona | ⚠️ **da decidere**: esecuzione del contratto fra cliente e consumatore, oppure legittimo interesse, oppure consenso (§11.1) | proposta: 24 mesi dall'erogazione, poi cancellazione |
| `servizio.recapito` | `servizio_erogato.email`, `.telefono` | cliente dell'azienda cliente | contatto | recapitare l'invito | ⚠️ stessa decisione aperta | proposta: 24 mesi |
| `servizio.momento_erogazione` | `servizio_erogato.erogato_il` | cliente | comportamentale; **potenzialmente articolo 9 per deduzione** in settori sanitari | calcolare la finestra dei 30 giorni della legge 34/2026 | vedi sopra | proposta: 24 mesi |
| `richiesta.recapito_usato` | `richiesta_recensione.destinazione` | cliente | contatto | prova di che cosa è stato inviato e a chi | legittimo interesse: prova di equità dell'invito (§2.3) | proposta: 36 mesi — più lungo del dato d'origine, perché è materiale di prova |
| `recensione.autore_pubblico` | `recensione.autore` | autore della recensione (**terzo**, non nostro utente) | anagrafico, già pubblico all'origine | mostrare la recensione e collegarla alla risposta | ⚠️ **da decidere**: legittimo interesse del titolare a gestire la propria reputazione | finché la recensione è viva sulla piattaforma, poi allineamento |
| `recensione.testo` | `recensione.testo` | autore della recensione | **libero — può contenere categorie particolari** | leggere, rispondere, analizzare i temi | ⚠️ **da decidere insieme all'avviso qui sopra** | vedi le tre vie dell'avviso |
| `segnalazione.identita` | `segnalazione.segnalante` | rappresentante dell'azienda cliente | anagrafico | requisito di forma della segnalazione (art. 19 c. 2) | obbligo di forma per esercitare un diritto | proposta: 5 anni (durata di un contenzioso) |
| `collegamento.credenziali` | `collegamento_piattaforma.segreto` | — (dell'azienda, non di una persona) | **segreto**, non dato personale | mantenere la delega | esecuzione del contratto | fino alla revoca |

**Esportazione e cancellazione.** Devono comparire **in entrambe** — esportazione e cancellazione del contratto
dati dell'app — le tabelle: `servizio_erogato`, `richiesta_recensione`, `recensione`, `risposta`, `segnalazione`.
Dimenticarne una è il difetto di conformità più probabile. Due avvertenze specifiche di questa app:

- **la recensione non si può cancellare dalla piattaforma d'origine**: se un autore ci chiede la cancellazione,
  noi possiamo cancellare la **nostra copia** e dobbiamo dirgli che la recensione sulla piattaforma va richiesta
  a chi la ospita. Cancellare la copia e tacere sull'originale sarebbe fuorviante;
- **la cancellazione è fisica**: sostituire il nome con un codice non è cancellare. La prova di equità
  sopravvive però in forma **aggregata** (quanti invitati, quanti esclusi e perché), che non contiene persone.

**Testo libero.** Ci sono tre campi liberi: il testo della recensione (scritto da terzi — è il problema
dell'avviso), il testo della risposta (scritto dal cliente, e può contenere dati del cliente finale: il controllo
di pubblicazione della storia 0019 avvisa se la bozza contiene il nome o un recapito) e la motivazione della
segnalazione. L'app non fa rilevazione automatica di contenuto; il presidio, se servirà, è un tema trasversale.

**Integrazioni esterne.** Diventerebbero fornitori che trattano dati per nostro conto o per conto del cliente:
**(a)** Google, per la lettura delle recensioni e la pubblicazione delle risposte — con delega del proprietario del
profilo, quindi il rapporto è del cliente ma il canale passa da noi; **(b)** Trustpilot, allo stesso modo;
**(c)** il fornitore di recapito dei messaggi di invito, che è il più invasivo dei tre perché riceve nome e
recapito di ogni cliente invitato. Tutti e tre vanno nell'elenco dei fornitori e nell'informativa.

**Classificazione della change.** Una app nuova introduce finalità nuove e categorie nuove: è un cambiamento
**sostanziale**. Qui a maggior ragione, per la possibile presenza di categorie particolari nel testo libero di
terzi: la valutazione d'impatto va messa in conto fin dalla prima storia che scrive una recensione a database
(storia 0009), non alla fine.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, casi d'uso 0061-0066, scritti e non
> implementati): qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_recensioni` | `(sede?, periodo?, voto_massimo?, solo_senza_risposta?) → elenco minimizzato` | restituisce voto, momento, piattaforma, prime righe del testo e stato della risposta | lettura | no |
| `punteggio_reputazione` | `(sede?, periodo?) → media, volume, distribuzione, andamento` | la fotografia di una sede | lettura | no |
| `recensioni_negative_da_gestire` | `(sede?) → elenco ordinato per anzianità` | ciò che aspetta una risposta | lettura | no |
| `stato_delle_richieste` | `(sede, periodo) → invitati, esclusi con motivo, regola di equità applicata` | è la **prova di equità** in forma leggibile, in conteggi e motivi: **mai** nomi e recapiti dei clienti finali | lettura | no |
| `dichiarazione_trasparenza` | `(sede) → testo in vigore, versione, stato` | il testo che la direttiva europea impone, come è pubblicato adesso (storia 0025) | lettura | no |
| `prepara_risposta` | `(id_recensione, tono?) → bozza di risposta` | scrive una bozza; non pubblica niente | scrittura | **sì** (la bozza va approvata da una persona) |
| `pubblica_risposta` | `(id_bozza) → esito della pubblicazione` | **atto pubblico verso l'esterno, a nome dell'azienda** | scrittura irreversibile | **sì, obbligatoria e non disattivabile** |
| `programma_richieste` | `(sede, dal, al) → bozza del lotto: chi verrebbe invitato e chi no, con il motivo` | prepara il lotto; non manda niente | scrittura | **sì** |
| `segnala_recensione` | `(id_recensione, motivo_normativo, motivazione) → bozza di segnalazione` | prepara la segnalazione alla piattaforma | scrittura irreversibile | **sì, obbligatoria** |

**Due cose che questi strumenti deliberatamente non hanno**, ed è la parte che conta:

1. `programma_richieste` **non ha un parametro «solo i clienti soddisfatti»**, né alcun filtro che ci assomigli.
   Non è una svista da colmare: la sua assenza è il modo in cui il rifiuto n. 1 del §1 sopravvive al livello
   conversazionale. Se domani un assistente riceverà l'istruzione «invita solo chi ha lasciato una mancia», lo
   strumento non avrà il modo di eseguirla, e la storia 0029 impone che risponda spiegando perché;
2. non esiste alcuno strumento che **scriva una recensione**. Ne esiste uno che scrive **risposte**. La
   distinzione è tutta la differenza fra un prodotto lecito e uno che produce prove contro il proprio cliente.

**Riga di lettura.** Il livello conversazionale rende questa app più utile delle concorrenti su un punto preciso:
la domanda vera del titolare non è «scrivimi una risposta» — quella la sa fare anche una chat generica — ma
«**cosa mi è arrivato di brutto oggi, su quale sede, e ho già invitato tutti quelli di ieri?**». È una domanda che
richiede il collegamento autorizzato alle piattaforme e il registro di equità: senza l'app non ha risposta.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine di questa epica l'app esiste, è accesa, vuota e utilizzabile: un servizio che parte in locale, uno
schema con le sue tabelle, un modulo nella barra laterale, un abbonamento che conta le sedi.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | il servizio `recensioni` nasce dallo scaffolding, risponde su `/api/recensioni/v1/*` e pubblica la sua definizione delle interfacce |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | schema `app_recensioni` con le tabelle di base, `tenant_id` ovunque e il filtro per account provato |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | il modulo compare nella barra laterale con le sue sezioni, in cinque lingue e nei due temi |
| [0004](01-fondamenta/0004-abbonamento-e-quota-delle-sedi.md) | Abbonamento e quota delle sedi | la metrica `sedi_monitorate` a giacenza, il blocco a `429` e il rifiuto del passaggio a un piano più piccolo |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` mostra l'app e un comando riempie un account dimostrativo con sedi, servizi e recensioni inventate |

### Epica 02 — Sedi e collegamento alle piattaforme

Alla fine di questa epica il cliente ha dichiarato le sue sedi, le ha collegate alle piattaforme con una delega
sua e le recensioni arrivano da sole dentro l'app, con le regole di conservazione rispettate.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-sedi-e-piattaforme/0006-anagrafica-delle-sedi.md) | Anagrafica delle sedi | creare, modificare e sospendere una sede; è l'unità che consuma quota |
| [0007](02-sedi-e-piattaforme/0007-collegamento-al-profilo-google.md) | Collegamento al profilo Google | delega del proprietario del profilo, scelta della sede, stato del collegamento |
| [0008](02-sedi-e-piattaforme/0008-collegamento-a-trustpilot.md) | Collegamento a Trustpilot | delega sull'unità aziendale, con le credenziali del contratto del cliente |
| [0009](02-sedi-e-piattaforme/0009-raccolta-periodica-delle-recensioni.md) | Raccolta periodica delle recensioni | lavorazione programmata che porta dentro le recensioni nuove senza doppioni |
| [0010](02-sedi-e-piattaforme/0010-conservazione-e-attribuzione-dei-contenuti-di-terzi.md) | Conservazione e attribuzione dei contenuti di terzi | cosa si conserva, per quanto, con quale attribuzione, e cosa si ricarica dal vivo |

### Epica 03 — Richiesta di recensione senza filtri

È il cuore dell'app e il suo posizionamento. Alla fine di questa epica il cliente invita **tutti** i clienti
serviti, con lo stesso messaggio, e ha la prova di averlo fatto.

| # | Storia | In una riga |
|---|---|---|
| [0011](03-richiesta-senza-filtri/0011-registrazione-del-servizio-erogato.md) | Registrazione del servizio erogato | il fatto che rende ammissibile l'invito: a mano, da appuntamento o da fattura |
| [0012](03-richiesta-senza-filtri/0012-regola-di-equita-della-richiesta.md) | Regola di equità della richiesta | *tutti* oppure *uno ogni N*: due sole forme, nessuna guarda la soddisfazione |
| [0013](03-richiesta-senza-filtri/0013-modello-del-messaggio-e-controllo-delle-pratiche-vietate.md) | Modello del messaggio e controllo delle pratiche vietate | il testo dell'invito in cinque lingue, respinto se promette un vantaggio o chiede contenuti specifici |
| [0014](03-richiesta-senza-filtri/0014-invio-della-richiesta.md) | Invio della richiesta | l'invito parte sul canale scelto e porta il collegamento ufficiale della piattaforma |
| [0015](03-richiesta-senza-filtri/0015-sollecito-unico-e-finestra-dei-trenta-giorni.md) | Sollecito unico e finestra dei trenta giorni | un solo sollecito, e la finestra si chiude quando la recensione non sarebbe più lecita |
| [0016](03-richiesta-senza-filtri/0016-registro-di-equita-esportabile.md) | Registro di equità esportabile | chi è stato invitato, chi no e perché: il documento da mostrare a chi contesta |

### Epica 04 — Risposte e recensioni negative

Alla fine di questa epica il cliente vede tutte le recensioni in un posto solo, risponde con l'aiuto di una bozza
e sa cosa fare — e cosa non fare — con una recensione negativa.

| # | Storia | In una riga |
|---|---|---|
| [0017](04-risposte-e-recensioni-negative/0017-flusso-unico-delle-recensioni.md) | Flusso unico delle recensioni | elenco unico multi-piattaforma con filtri, ricerca e stato della risposta |
| [0018](04-risposte-e-recensioni-negative/0018-bozza-di-risposta-assistita.md) | Bozza di risposta assistita | l'assistente propone un testo; resta una bozza finché una persona non la tocca |
| [0019](04-risposte-e-recensioni-negative/0019-pubblicazione-della-risposta.md) | Pubblicazione della risposta | conferma umana esplicita, controllo sui dati personali citati, esito tracciato |
| [0020](04-risposte-e-recensioni-negative/0020-avviso-sulle-recensioni-negative.md) | Avviso sulle recensioni negative | notifica, presa in carico e tempo di risposta, senza scorciatoie per farle sparire |
| [0021](04-risposte-e-recensioni-negative/0021-segnalazione-di-una-recensione-non-conforme.md) | Segnalazione di una recensione non conforme | l'unica strada offerta contro una recensione illecita, con gli elementi che la legge richiede |

### Epica 05 — Reputazione e vetrina

Alla fine di questa epica il cliente sa come sta la sua reputazione, capisce perché, e può mostrarla sul proprio
sito in modo fedele e conforme.

| # | Storia | In una riga |
|---|---|---|
| [0022](05-reputazione-e-vetrina/0022-punteggio-di-reputazione-della-sede.md) | Punteggio di reputazione della sede | media, volume, distribuzione e andamento, per sede e per piattaforma |
| [0023](05-reputazione-e-vetrina/0023-temi-ricorrenti-nelle-recensioni.md) | Temi ricorrenti nelle recensioni | cosa torna nei testi — attesa, pulizia, prezzo — con il collegamento alle recensioni d'origine |
| [0024](05-reputazione-e-vetrina/0024-riquadro-pubblico-per-il-sito.md) | Riquadro pubblico per il sito | riquadro incorporabile che non si può filtrare per voto |
| [0025](05-reputazione-e-vetrina/0025-dichiarazione-di-trasparenza.md) | Dichiarazione di trasparenza | il testo che la direttiva omnibus impone, generato da com'è configurata davvero l'app |
| [0026](05-reputazione-e-vetrina/0026-rapporto-periodico-della-reputazione.md) | Rapporto periodico della reputazione | il riepilogo mensile che arriva senza chiederlo, esportabile |

### Epica 06 — Esposizione conversazionale e prove end-to-end

Alla fine di questa epica ogni funzione è comandabile da una chat con le garanzie giuste, e il percorso completo
dell'app è provato dall'inizio alla fine.

| # | Storia | In una riga |
|---|---|---|
| [0027](06-esposizione-conversazionale-e-prove/0027-contratto-degli-strumenti-di-lettura.md) | Contratto degli strumenti di lettura | gli strumenti di sola lettura, con schema, minimizzazione e filtro per account |
| [0028](06-esposizione-conversazionale-e-prove/0028-strumenti-di-scrittura-con-bozza-e-conferma.md) | Strumenti di scrittura con bozza e conferma | i quattro strumenti che producono bozze, e la conferma umana che non si disattiva |
| [0029](06-esposizione-conversazionale-e-prove/0029-rifiuto-delle-richieste-vietate.md) | Rifiuto delle richieste vietate | quando l'istruzione chiede una pratica proibita, lo strumento rifiuta e spiega |
| [0030](06-esposizione-conversazionale-e-prove/0030-percorso-end-to-end-dell-app.md) | Percorso end-to-end dell'app | il percorso `[J-RECENSIONI]` dall'attivazione alla risposta pubblicata, e il registro di copertura |
| [0031](06-esposizione-conversazionale-e-prove/0031-esportazione-e-cancellazione-dei-dati-personali.md) | Esportazione e cancellazione dei dati personali | tutte le tabelle coperte o escluse con motivo, e la verità sull'originale che resta sulla piattaforma |

**Totale**: 6 epiche, 31 storie. La storia 0031 sta **in coda** all'epica, dopo il percorso end-to-end, perché è la
verifica finale della copertura: ha bisogno che tutte le tabelle esistano e non è prerequisito di nessun'altra.

---

## 9. Estensioni della console di amministrazione

Servono estensioni, ma poche e tutte diagnostiche: lo **stato dei collegamenti alle piattaforme** per account
(è la causa numero uno delle richieste di assistenza: la delega scade e le recensioni smettono di arrivare), la
**deroga temporanea** sul tetto delle sedi durante una migrazione, e il **registro dei modelli di messaggio
respinti** dal controllo delle pratiche vietate — che serve a capire se un cliente sta insistendo per fare
qualcosa che non deve. Nessuna estensione dà accesso ai contenuti dell'account.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **04 — LeadGrove** (`sales`) | **dipende da**, se presente | l'**anagrafica clienti condivisa**, che il catalogo (§6) indica come entità centrale della suite. RepGrove ha bisogno di sapere chi è stato servito: se LeadGrove c'è, il cliente non lo reinserisce. Il collegamento è **a eventi**, mai una chiamata da app ad app |
| **07 — BookGrove** (`prenotazioni`) | **alimentata da** | l'appuntamento **erogato** è l'evento che rende ammissibile l'invito (storia 0011). È la sinergia più forte dell'app: nel settore bellezza e servizi alla persona, «ieri sono venute dodici persone» è già scritto da qualche parte |
| **02 — BillGrove** (`billing`) | **alimentata da** | la **fattura emessa** è la prova che il servizio c'è stato per chi non prende appuntamenti (officina, artigiano). Il catalogo §6 mette la fattura nella catena del documento contabile: RepGrove si attacca in coda a quella catena |
| **16 — ReachGrove** (`campaigns`) | **si sovrappone a**, e va tenuta distinta | entrambe mandano messaggi a clienti. Sono cose diverse: ReachGrove manda **comunicazioni commerciali** a chi ha acconsentito; RepGrove manda **un invito legato a un servizio appena ricevuto**. Confonderle sarebbe grave in entrambi i versi (§11.5) |
| **12 — DeskGrove** (`helpdesk`) | **potrebbe alimentare** | una recensione negativa è spesso un reclamo. Aprire una richiesta di assistenza da una recensione è una sinergia naturale, **rimandata**: dipende da un'app che non esiste (§11.4) |
| **21 — SalonGrove**, **22**, **24** e gli altri verticali con sede fisica | **si integrerebbe con** | tutti hanno il momento «servizio erogato». Non è una dipendenza: è la ragione per cui la storia 0011 accetta l'evento da qualunque origine, e non solo da BookGrove |

**Riga di lettura.** RepGrove **ha senso da sola** — è una delle poche app del catalogo di cui questo si può dire
senza forzature: un parrucchiere con una sede la compra e la usa senza nient'altro, perché l'origine dei clienti
può essere un caricamento manuale. Dentro la suite però migliora di parecchio, perché sparisce l'unica azione
noiosa che le resta: dire chi è stato servito.

**Sovrapposizioni da evitare.** Con 16 ReachGrove sul canale di invio (vedi §11.5). Con il modulo «Reputation
Manager» che i concorrenti vendono a parte: qui è compreso, e va detto nel listino invece di farne un piano.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **Base giuridica dell'invito a recensire.** È una comunicazione commerciale ai sensi dell'articolo 130 del Codice privacy — e quindi serve consenso preventivo — oppure è un messaggio di servizio legato all'esecuzione del contratto? | Non l'ho trovato risolto: le piattaforme impongono di invitare **tutti**, la disciplina italiana sulle comunicazioni indesiderate è restrittiva, e le due cose possono entrare in tensione. È il punto che decide se l'app è vendibile in Italia così com'è | **sviluppatore, con parere legale** — è anche una fermata di escalation sui dati personali (§6) |
| 2 | **Cosa è lecito conservare dei contenuti delle piattaforme.** Le fonti secondarie dicono che i contenuti di Google non si memorizzano; l'interfaccia che useremo è però quella del profilo dell'attività, con delega del proprietario, e per quella non ho trovato un termine esplicito | Le pagine ufficiali non si sono lasciate leggere (§2.7). La risposta cambia il modello dati: se il testo non si può conservare, saltano l'analisi dei temi (0023) e la ricerca nel testo (0017) | **sviluppatore**, leggendo i termini delle due piattaforme prima della storia 0010 |
| 3 | **Confronto con i concorrenti.** La scheda di catalogo lo elenca fra i casi d'uso. Non l'ho messo in nessuna epica | Quello che si può leggere legalmente sui concorrenti è la sola valutazione media pubblica, e conservarla ha gli stessi dubbi del punto 2. Costruire una funzione «benchmark» su questo sarebbe promettere più di quanto si può mantenere | **sviluppatore**: o si rinuncia (raccomandato), o si verifica prima cosa le condizioni permettono |
| 4 | **Piattaforme oltre Google e Trustpilot** (TripAdvisor, Booking, The Fork, Facebook) e **piano oltre le cinque sedi** | Ogni piattaforma ha condizioni proprie da leggere una per una, e ogni piano nuovo è difficile da togliere | **sviluppatore**, dopo i primi clienti veri |
| 5 | **Confine con 16 ReachGrove sul canale di invio.** Due app che mandano messaggi allo stesso cliente possono sommarsi in modo sgradevole | È una decisione di prodotto: si condivide un elenco di soppressione fra le due app? Un cliente che si è disiscritto dalle comunicazioni commerciali può ricevere un invito a recensire? La mia inclinazione è **sì, sono cose diverse**, ma non è una decisione mia | **sviluppatore** (direzione di prodotto), quando entrambe le app esisteranno |
| 6 | **Ripetizione del colore-categoria.** `amber` è già proposto da tre app di catalogo | Con sei colori e sessanta app la collisione è strutturale: serve una regola di piattaforma (per esempio una seconda dimensione visiva), non una scelta di questa scheda | **piattaforma** |
| 7 | **Settori sanitari e assimilati dentro o fuori dal perimetro** (dentisti, fisioterapisti, veterinari, psicologi) | Discende direttamente dall'avviso sull'articolo 9 (§6). Escluderli semplifica moltissimo; includerli richiede garanzie rafforzate e una valutazione d'impatto | **sviluppatore** (fermata di escalation sui dati personali) |
| 8 | **Prezzi e limiti dei piani** (§5) | Fermata di escalation: la fascia del catalogo sta sotto tutto il mercato rilevato | **sviluppatore** |

**Rischi noti**

- **Rischio di piattaforma** (già segnalato dal catalogo, §8): se Google o Trustpilot cambiano le condizioni di
  accesso o le chiudono, l'app perde la funzione (b) — l'aggregazione — e resta con la (a) e la (c). *Attenuazione*:
  l'app deve valere anche solo per chiedere le recensioni in modo corretto e conservarne la prova; il collegamento
  in lettura è un miglioramento, non la fondazione. Le storie sono ordinate proprio così: 03 (chiedere) non dipende
  da 02 (leggere) se non per la sede.
- **Accesso alle interfacce non concesso o con quota insufficiente.** L'accesso alla interfaccia del profilo
  dell'attività richiede una domanda motivata e parte da quota zero. *Effetto*: potremmo non poter attivare
  l'aggregazione al primo giorno. *Attenuazione*: la domanda va fatta **prima** della storia 0007, non durante.
- **Il cliente vuole la funzione vietata.** Succederà: «il mio precedente fornitore mi mandava solo i contenti».
  *Attenuazione*: la spiegazione dev'essere nel prodotto, non nel materiale di vendita — la schermata della
  regola di equità (storia 0012) dice **perché** non c'è quella terza opzione, con il collegamento alle regole
  delle piattaforme. È anche il modo di trasformare il rifiuto in un argomento.
- **Recensioni che contengono dati sulla salute.** Vedi §6: è il rischio di conformità più serio dell'app.
- **Sanzione al cliente per una recensione non conforme raccolta tramite noi.** Se un cliente inserisce un
  incentivo nel testo dell'invito, la sanzione (500-5.000 €) arriva a lui. *Attenuazione*: il controllo della
  storia 0013 respinge il testo **prima** che venga usato, e conserva la prova di averlo respinto.

**Fuori dimensionamento**: non applicabile. 6 epiche (raccomandate 4-7), da 5 a 6 storie per epica (raccomandate
4-8), 31 storie in tutto (fascia raccomandata 20-45).
</content>
</invoke>
