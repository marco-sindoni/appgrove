# FlowGrove — descrizione dell'applicazione

**Numero di catalogo**: 13 · **Tipo**: orizzontale · produttività · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 13](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** FlowGrove organizza il lavoro che un'azienda piccola svolge **per un cliente**: un progetto (o
commessa) con le sue attività, chi le fa, entro quando, a che punto sono. Sopra ci appoggia due cose che le
lavagne di attività di solito non hanno: le **ore dichiarate** dalle persone sulle attività, con la distinzione
fra ore fatturabili e non fatturabili, e il **conto della commessa** — quanto era stato preventivato, quanto è
stato consumato in ore e in costi, quanto resta di margine. Alla fine del mese produce l'elenco delle **righe
pronte da fatturare**, che l'app di fatturazione (02 BillGrove) trasforma in un documento.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50, mercato globale con priorità europea.
Il profilo per cui è scritta è quello che **lavora su commessa**: studi professionali, agenzie, laboratori
artigiani, piccole imprese di installazione e assistenza, società di consulenza. Chi compra è il titolare, che
vuole sapere se la commessa ha guadagnato; chi la usa tutti i giorni sono da tre a otto persone che eseguono e
dichiarano le proprie ore, spesso mentre fanno anche altro.

**Quale problema toglie.** Il problema **non** è «non sappiamo cosa dobbiamo fare»: quello lo risolvono già
gratis Trello, ClickUp o un gruppo di messaggistica. Il problema è che **le ore e il lavoro stanno in posti
diversi dai soldi**. Oggi il tipico studio da cinque persone tiene le attività su una lavagna gratuita, le ore su
un foglio di calcolo che ognuno compila la sera (o non compila), e il preventivo e la fattura su un terzo
strumento. Il costo di questa separazione è documentato ed è sempre lo stesso: lo sforamento si scopre **a fine
mese, quando si fattura** — si sono lavorate sessanta ore su un preventivo da quaranta e non c'è più niente da
fare ([Productive, guida alla fatturazione di progetto](https://productive.io/blog/project-billing/), §2.6/6).
FlowGrove toglie quel ritardo: le ore stanno attaccate all'attività, l'attività sta attaccata al budget, il budget
sta attaccato al preventivo accettato e alla fattura che ne uscirà.

**Cosa NON fa.**

- non emette preventivi e non emette fatture (sono le app 06 QuoteGrove e 02 BillGrove): FlowGrove **prepara** le
  righe fatturabili e le consegna, non le trasforma in documento;
- non è un sistema di rilevazione presenze, di controllo dell'attività lavorativa né di sorveglianza: nessuna
  posizione geografica, nessuna cattura di schermate, nessun rilevamento di inattività, nessun conteggio di tasti
  o di applicazioni aperte, nessun cronometro che parte da solo (§2.3 e §6 — è una scelta di prodotto, non una
  omissione);
- **non registra assenze, malattie, permessi né la loro causale**: sono di un'app del personale (09 HrGrove) e
  sono, in parte, dati sulla salute che questa app deve stare attenta a non attirare (§6);
- non calcola stipendi, straordinari, riposi né turni (10 PayGrove, 11 ShiftGrove);
- non ha un portale per il cliente finale: nessuna pagina pubblica, nessun accesso esterno al progetto (§11.4);
- non fa pianificazione di capacità, diagrammi a barre temporali (Gantt) con dipendenze fra attività, né
  livellamento delle risorse: è deliberatamente una gestione **leggera**, ed è il punto su cui accetta di perdere
  il confronto con gli strumenti grandi (§2.5);
- non è un archivio documentale: gli allegati sono quelli di lavoro sull'attività, non la conservazione a norma.

**Rischio di sostituzione da parte dei modelli linguistici.** `minacciata`, come dice il catalogo, e va detto
senza edulcorare: un elenco di attività con stati è esattamente ciò che un assistente generico sa già tenere, e
la parte «lavagna» di questa app non è difendibile. Ciò che **non** è sostituibile è il collegamento con i dati
proprietari dell'account — il preventivo accettato, la tariffa concordata con quel cliente, le ore realmente
dichiarate, la fattura che ne discende. Se FlowGrove venisse costruita come sola lavagna, sarebbe la prima app
del catalogo a diventare inutile. La difesa non è funzionale, è **di collocazione**: è l'anello fra il preventivo
e la fattura (§10, §11).

---

## 2. Mercato e analisi in rete

> Compilata dopo 9 fra ricerche mirate e recuperi di pagina ufficiale
> ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4). Ciò che non è stato trovato è **dichiarato** al §2.7, non
> colmato a intuito. Il catalogo stesso avverte (§8) che molti prezzi in giro provengono da comparatori e
> invecchiano male: qui è segnato riga per riga che cosa viene dalla pagina ufficiale e che cosa no.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| monday.com (Work Management) | Israele/globale | Il riferimento pubblicitario della categoria: tabelle configurabili che diventano lavagne, automazioni, molto ampio | Free **fino a 2 postazioni**, 3 bacheche, 3 documenti; Basic 9 €/postazione/mese annuale, Standard 12 €, Pro 19 €, tutti con **minimo di postazioni** (la pagina letta il 2026-08-03 indicava un minimo di 10 su Work Management e di 3 sugli altri prodotti; i comparatori riportano 3 — §2.7) — **rilevato su pagina ufficiale** | [monday.com/pricing](https://monday.com/pricing) |
| Asana | Stati Uniti/globale | Gestione del lavoro per squadre, molto forte sui flussi ripetitivi | Personal (gratuito) **fino a 2 utenti**; Starter 10,99 $/utente/mese annuale (13,49 $ mensile); Advanced 24,99 $ annuale (30,49 $ mensile). **Il tracciamento delle ore è solo su Advanced** — **rilevato su pagina ufficiale** | [asana.com/pricing](https://asana.com/pricing) |
| Trello | Stati Uniti/globale (Atlassian) | La lavagna a colonne nella sua forma più semplice; è il termine di paragone del segmento micro | Free (10 bacheche per spazio di lavoro, schede illimitate); Standard 5 $/utente/mese annuale (6 $ mensile); Premium 10 $ (12,50 $) — **rilevato su pagina ufficiale** | [trello.com/pricing](https://trello.com/pricing) |
| ClickUp | Stati Uniti/globale | Il più ampio dei quattro; il piano gratuito è il più generoso del mercato | Free Forever con **utenti illimitati** e attività illimitate, ma 60 MB di archivio, 100 usi delle funzioni avanzate (diagrammi temporali, campi personalizzati), 100 esecuzioni di automazione al mese — **valori da fonti editoriali, non dalla pagina ufficiale** (§2.7) | [eesel — prezzi ClickUp 2026](https://www.eesel.ai/blog/clickup-pricing) · [UpSys — i piani ClickUp spiegati](https://www.upsys-consulting.com/en/blog/clickup-pricing-explained) |
| Plane / Vikunja (aperti nel codice sorgente) | Europa e globale | Alternative installabili sul proprio server, gratuite, in crescita rapida | 0 € di licenza; il costo è il server e chi lo tiene su — **da fonte editoriale** | [Plane — i migliori strumenti aperti nel 2026](https://plane.so/blog/top-6-open-source-project-management-software-in-2026) |
| T-PPM, BPilot, EcosAgile (Italia) | Italia | Gestione **commesse** con consuntivazione delle ore e controllo del margine: sono i concorrenti veri del pezzo che conta, non le lavagne | **prezzo non rilevato**: nessuno dei tre pubblica un listino, si passa da una richiesta di contatto (§2.7) | [t-ppm.it](https://www.t-ppm.it/) · [bpilot.it — software gestione commesse](https://www.bpilot.it/software-gestione-commesse/) · [ecosagile.com — progetti e fogli ore](https://ecosagile.com/ITA/software-gestione-progetti-timesheet) |
| Harvest · Toggl Track · Clockify | Stati Uniti / Estonia / Serbia | Solo ore: cronometro, foglio ore, e in alcuni casi la fattura che ne esce | Clockify gratuito con utenti illimitati, Basic 3,99 $, Standard 5,49 $ (con fogli ore approvabili e fatturazione); Toggl Track gratuito fino a 5 utenti, Starter 9 $, Premium 18 $; Harvest gratuito 1 postazione e 2 progetti, poi ~12 $ — **valori da fonte editoriale** | [Clockify — confronto Clockify/Toggl/Harvest 2026](https://clockify.me/blog/apps-tools/clockify-vs-toggl-vs-harvest/) |

**Lettura.** Il mercato è spaccato in due metà che non si parlano, e la frattura è l'occasione.

Da una parte le **lavagne**: abbondanti, mature, e con piani gratuiti che il segmento micro usa davvero. Su questo
lato non c'è nessuna partita da vincere — chi prova a rivendere una lavagna a pagamento a una micro-impresa
europea nel 2026 perde contro ClickUp gratis o contro Plane installato su un server da 5 € al mese.

Dall'altra le **commesse**: gli italiani T-PPM, BPilot ed EcosAgile fanno esattamente la cosa che conta (ore →
costo → margine per commessa) ma non pubblicano un prezzo, il che nel segmento micro è già un fuori-gioco: chi ha
cinque dipendenti non apre una trattativa per comprare un software da 30 € al mese.

Nel mezzo c'è una **doppia spesa che nessuno risolve**: per chiudere il giro «lavoro → ore → fattura» un'azienda
da cinque persone oggi paga *due* abbonamenti (una lavagna e un contatore di ore) e poi **ricopia a mano** le ore
nel programma di fatturazione. È il punto in cui si perdono ore fatturabili e si sbagliano gli importi
([Productive](https://productive.io/blog/project-billing/)). FlowGrove esiste per questo, e per nient'altro.

Un dato secondario ma pesante sui costi reali: **i piani gratuiti dei due prodotti più pubblicizzati si fermano a
2 utenti** (monday, Asana). Una squadra da cinque persone è già fuori dal gratuito il primo giorno; e su monday
Work Management il minimo di postazioni fa pagare più posti di quanti se ne usino.

### 2.2 Prezzi praticati nel dominio

- **Unità di misura**: quasi unanimemente il **posto** (utente nominale). È l'unità su cui il cliente sa fare il
  confronto, ed è quella che il catalogo indica per questa app (6-12 €/utente/mese).
- **Fascia d'ingresso**: 5-11 $ per posto al mese con fatturazione annuale (Trello Standard 5 $, monday Basic
  9 €, Asana Starter 10,99 $). La fatturazione mensile costa dal 18 % (monday, dichiarato) al 25 % (Trello,
  Asana) in più.
- **Fascia media**: 10-19 $/€ per posto (Trello Premium, monday Standard/Pro). Sopra i 25 $ (Asana Advanced) si
  esce dal segmento micro — e proprio lì Asana mette il tracciamento delle ore, cioè fa pagare più del doppio la
  funzione che a una micro-impresa su commessa serve **per prima**.
- **Contatore di ore separato**: altri 4-12 $ per posto al mese. Sommato alla lavagna, un'azienda da cinque
  persone spende fra 45 e 100 $ al mese per avere due metà di uno stesso lavoro.
- **Piano gratuito**: presente in tutti e quattro i grandi, ma con due modelli opposti — **a giacenza sugli
  utenti** (monday e Asana: 2 utenti) oppure **a giacenza sulle funzioni** (ClickUp: utenti illimitati, 60 MB e
  100 usi; Trello: 10 bacheche). La differenza conta per il nostro listino (§5).
- **Prova gratuita**: dove c'è, 14 giorni; coincide con la raccomandazione predefinita della piattaforma
  ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §7).
- **Sconto annuale**: 18-25 % dichiarato. La convenzione appgrove — annuale = 10× il mensile, «due mesi in
  regalo», cioè 17 % — sta appena sotto il mercato ed è difendibile.

Distinzione d'onestà: monday, Asana e Trello sono **letti sulla pagina di prezzo ufficiale** il 2026-08-03.
ClickUp, i contatori di ore e gli strumenti aperti nel codice sorgente vengono da fonti editoriali, da trattare
come ordini di grandezza.

### 2.3 Obblighi normativi del settore

La gestione di progetti in sé **non è un dominio normato**: nessun obbligo di conservazione, nessun formato
imposto, nessun adempimento fiscale — a differenza della fatturazione o della sicurezza sul lavoro. Va detto
chiaramente, perché è un'ottima notizia per il modello dati.

C'è però **una** materia che morde, e morde forte, ed è quella che decide la forma dell'epica 04: nel momento in
cui l'app registra **le ore lavorate da una persona su un'attività**, entra nella disciplina dei **controlli a
distanza sull'attività dei lavoratori**.

1. **Articolo 4 dello Statuto dei lavoratori (legge 300/1970, riscritto dal decreto legislativo 151/2015).** Gli
   strumenti da cui può derivare un controllo a distanza dell'attività dei lavoratori si possono usare solo per
   esigenze organizzative, produttive, di sicurezza o di tutela del patrimonio, e **previo accordo sindacale o
   autorizzazione dell'ispettorato del lavoro** (comma 1). Il comma 2 esenta da quella procedura gli **strumenti
   utilizzati dal lavoratore per rendere la prestazione**; ma il comma 3 impone comunque, perché i dati raccolti
   siano utilizzabili «a tutti i fini connessi al rapporto di lavoro», una **informativa adeguata** su come lo
   strumento si usa e come sono effettuati i controlli.
   Fonte: [Altalex — controllo a distanza dei lavoratori e strumenti informatici](https://www.altalex.com/documents/news/2022/03/04/controllo-a-distanza-dei-lavoratori-e-strumenti-informatici) ·
   [Agenda Digitale — come fare legalmente i controlli a distanza](https://www.agendadigitale.eu/sicurezza/privacy/controlli-a-distanza-sui-lavoratori-come-farli-legalmente/).
2. **Un foglio ore dichiarativo di norma sta nel comma 2, una funzione di sorveglianza no.** È la riga di
   confine che questa app deve rispettare per costruzione: appena si aggiunge una funzione che *rileva* invece di
   *raccogliere una dichiarazione* — posizione, schermate, inattività, applicazioni aperte — lo strumento
   cambia natura e si trascina dietro accordo sindacale, valutazione d'impatto e un rischio sanzionatorio che
   nessuna micro-impresa vuole. La nota del Garante richiamata dalle fonti sopra dice esattamente questo: la
   modifica di uno strumento di lavoro fatta per controllare lo declassifica automaticamente a strumento di
   controllo.
3. **Il precedente concreto, ed è recente.** Con il provvedimento del 13 marzo 2025 il Garante per la protezione
   dei dati personali ha sanzionato l'ARSAC con **50.000 €** per aver geolocalizzato i dipendenti in lavoro agile
   tramite l'applicazione «Time Relax» e per aver poi usato quei dati in un procedimento disciplinare: né il
   consenso raccolto né l'accordo sindacale sono bastati, perché mancavano base giuridica valida, trasparenza,
   proporzionalità e valutazione d'impatto.
   Fonte: [Garante privacy — provvedimento del 13 marzo 2025, doc. web 10128005](https://www.garanteprivacy.it/web/guest/home/docweb/-/docweb-display/docweb/10128005).
   **Conseguenza sul prodotto, non sulla documentazione**: FlowGrove non geolocalizza, non cattura schermate, non
   misura l'inattività e non fa partire cronometri da sola. Le storie 0017, 0019 e 0020 lo scrivono come
   requisito, non come buona intenzione.
4. **Diritti dell'interessato.** I lavoratori dell'azienda cliente sono interessati come chiunque altro:
   esportazione e cancellazione devono raggiungere le righe di ore, i commenti e gli allegati (§6, storia 0030).
5. **Assenze e salute.** La causale di un'assenza («malattia») è un dato sulla salute. È il motivo per cui
   FlowGrove **non ha** il concetto di assenza: la esclude dal perimetro invece di gestirla male (§6).

Non ho trovato alcun obbligo di **conservazione minima** dei fogli ore che valga in quanto tali per un'azienda
qualsiasi: le durate di conservazione lunghe che si incontrano nascono dalla documentazione del rapporto di
lavoro e dalla contabilità, non dal foglio ore in sé. La durata resta una scelta del titolare, da dichiarare
(§2.7).

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta prevedibile per il profilo descritto:

| # | Integrazione | Perché la chiedono | Fornitore esterno che tratterebbe dati? |
|---|---|---|---|
| 1 | **Le altre app della suite** (06 QuoteGrove, 02 BillGrove, 08 SpendGrove, 04 LeadGrove) | è la ragione d'essere dell'app: preventivo accettato → progetto, ore → fattura, spese → costo di commessa | **no** — sono app dello stesso titolare, comunicanti a eventi interni ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §2) |
| 2 | **Contabilità / commercialista** (esportazione tabellare del consuntivo di commessa) | il titolare vuole passare il consuntivo a chi tiene i conti | no, se resta un file scaricato dall'utente |
| 3 | **Calendario** (Google, Microsoft 365): vedere le scadenze delle attività nel proprio calendario | richiesta ricorrente in tutta la categoria | **sì** — collegamento vivo a un fornitore terzo. **Fuori perimetro in questa stesura**: l'app esporta un file di calendario, non si collega (storia 0027, punto aperto §11.3) |
| 4 | **Messaggistica di squadra** (notifica «attività assegnata» su un canale) | le squadre piccole vivono lì | **sì**. Fuori perimetro: le notifiche restano dentro l'app (storia 0016) |
| 5 | **Archivio di file** (Drive, OneDrive, Dropbox) per gli allegati | non volere una seconda copia dei file | **sì**. Fuori perimetro: gli allegati stanno nell'archivio della piattaforma (storia 0015) |

Nessuna integrazione con fornitori esterni è prevista in questa stesura. È una scelta: ogni collegamento vivo
aggiungerebbe un responsabile del trattamento e un punto di rottura, per un valore che nel segmento micro è
inferiore a quello del giro preventivo → ore → fattura.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Quello che si trova ripetuto nelle discussioni tecniche sulla categoria è una sola lamentela, declinata in venti
modi: **gli strumenti sono troppi e troppo configurabili per una squadra piccola**. Le piattaforme sono
descritte come sovraingegnerizzate, pensate per organizzazioni grandi, al punto che «serve un corso per gestire
una lavagna di attività»
([DEV — gli strumenti di gestione progetti sono troppo complicati](https://dev.to/martinbaun/project-management-tools-are-too-complicated-be2) ·
[DEV — alternative leggere per squadre piccole](https://dev.to/tanmay-m-chaudhari/best-lightweight-jira-alternatives-for-startups-and-small-teams-jeg)).

Tradotto in requisiti, e sono requisiti veri:

- **niente configurazione obbligatoria all'avvio.** Un progetto nuovo deve essere utilizzabile con un titolo e
  nient'altro; stati, modelli e tariffe sono opzionali con valori predefiniti sensati (storie 0006, 0010, 0018);
- **poche viste, non venti.** Elenco, lavagna, «le mie attività di oggi». Chi vuole diagrammi temporali con
  dipendenze non è il nostro cliente (§1, «cosa NON fa»);
- **l'inserimento delle ore deve costare meno di trenta secondi al giorno**, altrimenti non lo fa nessuno e tutto
  il resto dell'app crolla (storia 0019: il foglio ore settimanale su una schermata sola);
- **la risposta alla domanda «ci ho guadagnato?» deve essere una schermata, non un rapporto da costruire**
  (storia 0026);
- ciò che **rifiutano** in modo netto, e che va detto in fase di vendita: essere misurati. Uno strumento percepito
  come sorveglianza del titolare sui dipendenti viene compilato male o non viene compilato, e i dati diventano
  inutilizzabili. È la ragione pratica — oltre a quella giuridica del §2.3 — dell'impostazione dichiarativa.

### 2.6 Fonti consultate

1. **monday.com — pagina dei prezzi ufficiale** — https://monday.com/pricing — piano gratuito fermo a 2
   postazioni, 3 bacheche, 3 documenti; Basic/Standard/Pro a 9/12/19 € per postazione con fatturazione annuale e
   con un minimo di postazioni. Ne ho ricavato la fascia alta di riferimento e la prova che il gratuito dei grandi
   **non copre una squadra da cinque persone**.
2. **Asana — pagina dei prezzi ufficiale** — https://asana.com/pricing — piano Personal gratuito **fino a 2
   utenti**; Starter 10,99 $ e Advanced 24,99 $ per utente/mese annuale. Il dato che conta: **il tracciamento
   delle ore è confinato ad Advanced**, cioè costa più del doppio dell'ingresso. È l'argomento di prezzo più forte
   che FlowGrove ha.
3. **Trello — pagina dei prezzi ufficiale** — https://trello.com/pricing — gratuito con 10 bacheche per spazio di
   lavoro, Standard 5 $, Premium 10 $. Ne ho ricavato il pavimento di prezzo della categoria: sotto i 5 $ per
   posto non esiste mercato a pagamento per la sola lavagna.
4. **eesel — i prezzi di ClickUp nel 2026** — https://www.eesel.ai/blog/clickup-pricing — e **UpSys — i piani
   ClickUp spiegati** — https://www.upsys-consulting.com/en/blog/clickup-pricing-explained — piano gratuito con
   utenti illimitati, limitato su archivio (60 MB), usi delle funzioni avanzate (100) e automazioni (100 al mese).
   È il vero avversario del segmento micro ed è il motivo per cui la lavagna non è vendibile da sola.
5. **Clockify — confronto Clockify / Toggl Track / Harvest 2026** —
   https://clockify.me/blog/apps-tools/clockify-vs-toggl-vs-harvest/ — prezzi dei contatori di ore (3,99-18 $ per
   utente/mese) e loro piani gratuiti. Ne ho ricavato la stima della **doppia spesa** che una micro-impresa
   sostiene oggi per chiudere il giro ore → fattura.
6. **Productive — guida alla fatturazione di progetto** — https://productive.io/blog/project-billing/ — il
   passaggio che descrive il danno: le agenzie scoprono di aver lavorato sessanta ore su un budget da quaranta
   **solo quando fatturano a fine mese**, perché gestione del lavoro e contabilità sono strumenti scollegati con
   ricopiatura a mano in mezzo. È la citazione su cui poggia l'intera epica 04.
7. **Garante per la protezione dei dati personali — provvedimento del 13 marzo 2025 (doc. web 10128005)** —
   https://www.garanteprivacy.it/web/guest/home/docweb/-/docweb-display/docweb/10128005 — sanzione di 50.000 € per
   la geolocalizzazione dei dipendenti in lavoro agile tramite un'applicazione di rilevazione. Ne ho ricavato il
   confine di prodotto dell'epica 04: dichiarazione sì, rilevazione no.
8. **Altalex — controllo a distanza dei lavoratori e strumenti informatici** —
   https://www.altalex.com/documents/news/2022/03/04/controllo-a-distanza-dei-lavoratori-e-strumenti-informatici —
   e **Agenda Digitale — controlli a distanza sui lavoratori, come farli legalmente** —
   https://www.agendadigitale.eu/sicurezza/privacy/controlli-a-distanza-sui-lavoratori-come-farli-legalmente/ —
   struttura dell'articolo 4 dello Statuto dei lavoratori: comma 1 (accordo sindacale o autorizzazione), comma 2
   (strumenti per rendere la prestazione), comma 3 (informativa). È la base del §2.3 e del §6.
9. **Plane — i migliori strumenti aperti nel codice sorgente nel 2026** —
   https://plane.so/blog/top-6-open-source-project-management-software-in-2026 — panorama delle alternative
   installabili gratuite (Plane, Vikunja, OpenProject). Ne ho ricavato il rischio di fondo del §11: il pezzo
   «lavagna» ha un sostituto gratuito e credibile.
10. **DEV — gli strumenti di gestione progetti sono troppo complicati** —
    https://dev.to/martinbaun/project-management-tools-are-too-complicated-be2 — e **DEV — alternative leggere per
    startup e squadre piccole** —
    https://dev.to/tanmay-m-chaudhari/best-lightweight-jira-alternatives-for-startups-and-small-teams-jeg — la
    lamentela ricorrente sulla complessità. Requisiti travestiti, raccolti al §2.5.
11. **T-PPM** — https://www.t-ppm.it/ — **BPilot, software gestione commesse** —
    https://www.bpilot.it/software-gestione-commesse/ — **EcosAgile, progetti e fogli ore** —
    https://ecosagile.com/ITA/software-gestione-progetti-timesheet — i concorrenti italiani sul pezzo che conta
    (consuntivazione e margine di commessa). Nessuno pubblica un listino: ne ho ricavato che il segmento micro
    italiano su questo bisogno **non è servito a prezzo trasparente**.
12. **Xero — controllo dei costi per commessa** —
    https://www.xero.com/us/accounting-software/track-projects/job-costing/ — come la contabilità affronta lo
    stesso problema dal lato opposto (dal conto verso il progetto). Ne ho ricavato la conferma che il calcolo del
    margine di commessa è una funzione riconosciuta e comprata, non una nostra invenzione.

### 2.7 Cosa NON sono riuscito a determinare

- **Il minimo di postazioni di monday.com** — la pagina ufficiale letta il 2026-08-03 mostrava un minimo di 10
  postazioni su Work Management e di 3 sugli altri prodotti, mentre i comparatori riportano 3 ovunque. Non ho
  potuto risolvere la discordanza. Serve una verifica diretta al momento in cui il posizionamento di prezzo va
  fissato. Non incide sulla proposta del §5, che non è per posto.
- **I prezzi dei concorrenti italiani sulle commesse** (T-PPM, BPilot, EcosAgile) — nessuno dei tre pubblica un
  listino: si passa da una richiesta di contatto. La proposta del §5 nasce quindi dalle fasce delle lavagne
  internazionali e dal confronto con la categoria adiacente dei contatori di ore, **non** dal confronto con questi
  tre, che sono i concorrenti più vicini sul valore. Per chiuderlo servirebbe una richiesta di offerta.
- **Quanto una micro-impresa europea sia disposta a pagare per il solo margine di commessa**, separatamente dalla
  lavagna. Non ho trovato nessuna fonte che isoli quel valore. È il numero che deciderebbe se FlowGrove va venduta
  come app o come funzione di BillGrove (§11.1) — ed è una domanda di direzione di prodotto, non mia.
- **La quota di micro-imprese su commessa che oggi tiene le ore su carta o su foglio di calcolo.** Non ho trovato
  una rilevazione affidabile; l'ipotesi «la maggioranza» è verosimile ma resta un'ipotesi, e non l'ho usata come
  argomento di dimensionamento.
- **Se il foglio ore dichiarativo ricada sempre nel comma 2 dell'articolo 4** dello Statuto dei lavoratori: le
  fonti consultate lo lasciano intendere ma la qualificazione è caso per caso e la fa il titolare del
  trattamento, cioè il **cliente**, non noi. Trattato come punto aperto (§11.5) e come avviso in prodotto
  (storia 0017).

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa. Cambiare
> l'identificativo dopo lo scaffolding **non è una rinomina, è una migrazione di dati**: finisce nello schema del
> database, nei nomi delle code, nella rotta pubblica e nell'istanza del modulo di infrastruttura.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `progetti` | Rispetta `^[a-z][a-z0-9_]{0,30}$` (8 caratteri, minuscolo, sole lettere). Segue la convenzione già viva nel repository, dove l'app numero uno è `fatture`: identificativo tecnico in italiano che dice **cosa l'app è**, non come è commercializzata («FlowGrove» è il nome di listino, e i nomi di listino cambiano). Scartato `flowgrove` perché lega l'infrastruttura al marchio; scartati `tasks` e `pm` perché il primo descrive il pezzo commodity e il secondo è una sigla. Verificare che `progetti` sia libero al momento dello scaffolding: nel repository esistono oggi `fatture` (porta 8081) e `crm` (8082). |
| **Modello utente** | `multi` | Non è una scelta: senza più persone per account l'app non ha nulla da fare. Le tre domande che giustificano l'esistenza di FlowGrove — «chi lo sta facendo», «quante ore ci ha messo», «quanto ci abbiamo guadagnato» — sono tutte domande su **chi ha fatto cosa**. Un'app a utente singolo non ha il concetto di assegnazione e non può attribuire ore a una persona. Il cliente tipo ha da 3 a 8 utilizzatori quotidiani. |
| **Porta locale** | `8113` | Convenzione del kit (8100 + numero di catalogo 13), per non far collidere le sessanta proposte fra loro né con le app reali (`8081` fatture, `8082` mini-CRM, `9100` autenticazione). Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `seats` (posti occupati) | È la **sola** cosa che il piano limita, ed è l'unità che l'intero mercato usa (§2.2): il cliente sa già confrontarla. È anche l'unica metrica che cresce insieme al valore ricevuto — un progetto in più non costa nulla a nessuno, una persona in più che dichiara le proprie ore è esattamente il motivo per cui l'app serve. Scartate le alternative: `progetti_attivi` punirebbe chi lavora su tante commesse piccole (l'artigiano con dodici cantieri aperti), che è proprio il cliente giusto; `ore_registrate` sarebbe un disastro, perché metterebbe un tetto alla cosa che vogliamo che il cliente faccia tutti i giorni. |
| **Natura della metrica** | `stock` | È un tetto su ciò che esiste ora, non un consumo su una finestra: «il piano Squadra ha 10 posti» significa che per far entrare l'undicesima persona bisogna liberare un posto o cambiare piano — non che a fine mese i posti si azzerano e se ne possono occupare altri dieci. Conseguenza operativa dalla piattaforma ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §13): il passaggio a un piano inferiore è **bloccato** finché i posti occupati eccedono il tetto di destinazione, e il messaggio deve dire quante persone vanno rimosse prima (storia 0004). |
| **Colore-categoria e icona** | `violet` · icona `kanban` (tre colonne verticali di altezza diversa) | Nel sistema di design i sei colori-categoria si distribuiscono per famiglia di significato: la catena del documento commerciale prende `teal` e `amber` (02 BillGrove, 03 CashGrove, 08 SpendGrove), la relazione commerciale prende `blue` (04 LeadGrove), l'agenda verso l'esterno prende `green` (07 BookGrove). FlowGrove è **il lavoro interno della squadra**: `violet` la separa a colpo d'occhio dalle app che parlano con clienti e con denaro. `red` è scartato perché nel sistema di design segnala il pericolo. **Nota di coordinamento**: 06 QuoteGrove ha proposto anch'essa `violet`; con sei colori e sessanta app le ripetizioni sono inevitabili, ma le due app sono adiacenti nel percorso d'uso (preventivo → progetto) e converrebbe distinguerle. È un punto aperto di piattaforma (§11.6), non una decisione di questa scheda. Il colore deve comunque restare identico fra listino (`category`) e modulo frontend (`accentToken`). |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Project` (progetto/commessa) | Il lavoro venduto a un cliente, con un inizio e una fine | codice, titolo, cliente (denominazione + riferimento logico all'anagrafica condivisa), referente, stato (`bozza`, `attivo`, `sospeso`, `chiuso`, `archiviato`), date previste, budget in ore e in importo, origine (manuale oppure preventivo accettato) | **sì** — nome e recapito del referente del cliente |
| `Task` (attività) | Una cosa da fare dentro un progetto | titolo, descrizione, stato, priorità, scadenza, stima in ore, attività padre (per le sotto-attività), traguardo, fatturabile sì/no | **sì** — indirettamente, tramite l'assegnazione |
| `Assignment` (assegnazione) | Chi è responsabile di un'attività | attività, utente assegnato, data di assegnazione, chi ha assegnato | **sì** — identifica una persona |
| `Milestone` (traguardo) | Una tappa del progetto con una data e un significato per il cliente | titolo, data prevista, data raggiunta, attività collegate | no |
| `TimeEntry` (riga di ore) | **Ore dichiarate** da una persona, su un giorno, su un'attività | data di competenza, durata in minuti, attività, autore, fatturabile sì/no, tariffa applicata, nota, stato (`aperta`, `bloccata`, `consegnata`) | **sì** — è il dato personale più delicato dell'app (§6) |
| `Rate` (tariffa) | Il prezzo orario con cui una riga di ore diventa denaro | ambito (progetto, oppure predefinita dell'account), importo orario, valuta, validità | no |
| `Comment` (commento) | Una nota di lavoro su un'attività | testo, autore, data, menzioni | **sì** — autore, e testo libero (§6) |
| `Attachment` (allegato) | Un file di lavoro appeso a un'attività | nome, tipo, dimensione, riferimento all'archivio, chi l'ha caricato | **sì** — chi carica, e il contenuto del file (§6) |
| `ProjectCost` (costo di commessa) | Una spesa esterna imputata al progetto (materiali, fornitore, trasferta) | descrizione, importo, data, origine (manuale oppure evento da 08 SpendGrove) | no di norma — ma la descrizione è testo libero |
| `ProjectTemplate` (modello di progetto) | Una struttura di attività riusabile | titolo, attività prototipo con stime e scarti temporali | no |
| `BillableBatch` (lotto fatturabile) | L'insieme di righe consegnate alla fatturazione per un periodo | progetto, periodo, righe, totale, stato della consegna | no direttamente — aggrega ore di persone |

**Relazioni.** `Project` 1→N `Task`; `Task` 1→N sotto-`Task` (**un solo livello**: la sotto-attività non ha
figli, e non è una limitazione tecnica ma una scelta contro la complessità, §2.5); `Task` 0..1→1 `Milestone`;
`Task` 1→N `TimeEntry`, `Comment`, `Attachment`; `Project` 1→N `ProjectCost`, `Rate`, `BillableBatch`.

Due macchine a stati che le storie devono rispettare:

- **attività**: `da fare` → `in corso` → `in verifica` → `fatta`, con `sospesa` raggiungibile da qualunque stato
  non terminale e `annullata` come uscita. Gli stati sono **fissi**: non sono configurabili dal cliente, ed è
  ancora una volta la scelta contro la complessità (storia 0011);
- **riga di ore**: `aperta` (l'autore può modificarla e cancellarla) → `bloccata` (il periodo è chiuso, si
  corregge solo con una riga di rettifica tracciata) → `consegnata` (è finita in un lotto verso la fatturazione,
  non si tocca più). È la macchina a stati che rende affidabile il conto della commessa (storie 0017, 0020, 0022).

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_progetti`; nessuna chiave esterna verso altri schemi — il riferimento al cliente
dell'anagrafica condivisa e all'identificativo dell'utente sono riferimenti **logici**
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata
> della prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di
> scrivere il file `services/core/src/main/resources/pricing/progetti.yaml`.

**Ragionamento.** Tre numeri governano la proposta.

1. **Il pavimento del mercato è basso e il gratuito è forte** (§2.1-2.2): Trello Standard a 5 $ per posto,
   ClickUp gratuito con utenti illimitati, Plane installabile a costo zero. Chiedere il prezzo di una lavagna per
   una lavagna è perso in partenza.
2. **Il valore che vendiamo non è la lavagna**: è il giro ore → margine → fattura, che oggi il cliente ottiene
   pagando **due** abbonamenti (lavagna + contatore di ore, 45-100 $ al mese per cinque persone) e ricopiando a
   mano. Il confronto giusto è con quella somma, non con Trello.
3. **La forma del listino della piattaforma non è per posto**: il file di listino ha un prezzo di piano e un
   tetto sulla metrica (`limits: { metric: seats, cap: N }`, [PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md)
   §7). Si vende quindi **a piano con tetto di posti**, non a posto occupato — cosa che, per una micro-impresa,
   è anche un vantaggio comunicativo: il prezzo non cambia quando entra uno stagista.

| Piano | Prezzo mensile | Prezzo annuale | Limite sulla metrica `seats` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 3 posti | — | Il titolare e due collaboratori. Abbastanza per tenere davvero i progetti e le ore di una micro-impresa e vedere il primo conto di commessa; non abbastanza per una squadra. |
| `squadra` | 19 € | 190 € (= 10× il mensile, «due mesi in regalo») | 10 posti | 14 giorni | Lo studio o l'agenzia da 4 a 10 persone: è il cliente centrale dell'app. A dieci posti pieni fanno 1,90 € per persona al mese, contro i 9-11 € di monday o Asana **più** il contatore di ore. |
| `studio` | 39 € | 390 € | 30 posti | 14 giorni | La piccola impresa fino a 30 addetti, dove le commesse aperte sono decine e il margine per commessa è una domanda quotidiana. |

**Note obbligate.**

- **Tre piani, non di più**: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite lasciato vuoto significherebbe **illimitato**, non zero. Qui nessun limite è vuoto: tutti e tre i
  piani hanno un tetto esplicito.
- **La prova gratuita su `squadra` e `studio` non è ridondante** nonostante esista un piano gratuito, perché il
  gratuito è limitato **a giacenza sui posti**: una squadra da sei persone non può provare il prodotto vero senza
  la prova. Con carta richiesta all'inizio, come da raccomandazione di piattaforma.
- **Costo effettivo dell'incasso**: nessun piano scende sotto i 5 € al mese, quindi la parte fissa per
  transazione non morde in modo anomalo. Il piano `squadra` a 19 € è però il vero motore del listino: se il
  prezzo va rivisto, si rivede lì.
- **Il posizionamento è deliberatamente sotto il catalogo.** La scheda indica 6-12 €/utente/mese; questa proposta
  equivale a 1,90-6,30 € per persona a seconda di quanti posti si riempiono. È voluto e ha due ragioni: la
  lavagna è commodity, e il margine della suite sta nelle app a valle (BillGrove, QuoteGrove) che FlowGrove
  alimenta. **Se lo sviluppatore ritiene che FlowGrove debba stare in piedi da sola sui propri ricavi, questa
  proposta è sbagliata e va rifatta**: è esattamente la decisione di direzione di prodotto che un agente non può
  prendere (§11.1).
- I prezzi sono **immutabili una volta vivi**: un cambio si fa creando un prezzo nuovo e archiviando il vecchio,
  gli abbonati restano sul loro.
- Sulla metrica **a giacenza** il passaggio a un piano inferiore resta bloccato finché i posti occupati eccedono
  il tetto di destinazione (storia 0004).

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/progetti.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è peggio di uno assente, perché sembra conformità ed è finzione.

> ⚠️ **Attenzione — questa app tratta dati di lavoratori nel contesto del rapporto di lavoro.**
> Non è l'articolo 9 (vedi sotto), ma è la seconda area più delicata: le ore che una persona dichiara su
> un'attività, lette insieme, raccontano la sua giornata lavorativa. Da qui discendono l'informativa dovuta dal
> **cliente** ai propri lavoratori (articolo 4, comma 3 dello Statuto dei lavoratori, §2.3) e i confini di
> prodotto elencati sotto, che non sono opzioni ma requisiti.

**Categorie particolari (articolo 9): NO — ed è una scelta attiva, non un caso.** FlowGrove non tratta dati sulla
salute, biometrici, genetici, opinioni politiche, convinzioni religiose, orientamento sessuale né appartenenza
sindacale. Tre esclusioni deliberate tengono in piedi questa affermazione, e vanno mantenute:

1. **nessuna assenza e nessuna causale di assenza.** «Malattia», «infortunio», «permesso per visita medica» sono
   dati sulla salute: l'app non ha il concetto di assenza, non ha un calendario delle assenze e non deve
   acquisirlo. Chi non c'è semplicemente non dichiara ore. Le assenze sono di 09 HrGrove;
2. **nessun dato sindacale.** Non esistono campi su rappresentanze, permessi sindacali o adesioni;
3. **nessuna categoria di appartenenza della persona**: il collaboratore ha un ruolo applicativo e una tariffa,
   non attributi personali.

Resta un ingresso non presidiato — il **testo libero** (note delle righe di ore, commenti, descrizioni delle
attività, nomi dei file allegati): nulla impedisce a un utente di scriverci «rientrato dopo l'operazione». L'app
non fa rilevazione di contenuto; l'avviso in linea sui campi liberi («non inserire dati sensibili») è il presidio
minimo previsto, e il tema generale è trasversale alla piattaforma.

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `project.referente_nome` | `project.contact_name` | referente del cliente dell'account | anagrafico | sapere con chi si parla della commessa | esecuzione del contratto fra il cliente dell'account e il suo cliente | fino a 24 mesi dalla chiusura del progetto, poi cancellazione |
| `project.referente_email` | `project.contact_email` | referente del cliente dell'account | contatto | come sopra | come sopra | come sopra |
| `assignment.user_id` | `assignment.user_id` | collaboratore dell'account | identificativo di persona | sapere chi è responsabile di un'attività | esecuzione del contratto di lavoro / legittimo interesse organizzativo del datore | vita del progetto + 24 mesi |
| `time_entry.user_id` | `time_entry.user_id` | collaboratore dell'account | identificativo di persona | attribuire le ore a chi le ha dichiarate, per fatturare e calcolare il margine | esecuzione del contratto di lavoro e del contratto col cliente finale; obbligo contabile a valle | **da definire con lo sviluppatore**: la durata utile all'app è 24 mesi, ma le ore che diventano fattura hanno una vita contabile più lunga in capo al cliente (§11.5) |
| `time_entry.durata` + `data` + `nota` | `time_entry.minutes`, `work_date`, `note` | collaboratore dell'account | dato sull'attività lavorativa | consuntivo, fatturazione, margine | come sopra | come sopra |
| `comment.author_id` + `comment.body` | `comment` | collaboratore dell'account (e chiunque sia citato nel testo) | contenuto di lavoro, testo libero | discutere un'attività | legittimo interesse organizzativo | vita del progetto + 24 mesi |
| `attachment.uploaded_by` + contenuto | `attachment` e archivio dei file | collaboratore; potenzialmente terzi citati nel file | file di lavoro | svolgere l'attività | esecuzione del contratto | vita del progetto + 24 mesi |
| `project_cost.descrizione` | `project_cost.description` | eventuale fornitore o persona citata | testo libero | imputare un costo alla commessa | esecuzione del contratto | vita del progetto + 24 mesi |
| tracce di controllo (`created_by`, `updated_by`) | tutte le tabelle | collaboratore dell'account | identificativo di persona | ricostruire chi ha fatto cosa | legittimo interesse (responsabilità e sicurezza) | come la riga cui si riferiscono |

**Confini di prodotto che rendono vera questa classificazione.** Non sono buone intenzioni: sono requisiti scritti
nelle storie 0017, 0019, 0020 e 0026.

- **si dichiara, non si rileva**: nessuna posizione geografica, nessuna cattura di schermate, nessun rilevamento
  di inattività, nessun conteggio di tasti o di applicazioni aperte, nessun cronometro che parta da solo o resti
  acceso in sottofondo. Il cronometro esiste solo come scorciatoia di inserimento avviata e fermata dalla persona,
  e ciò che resta scritto è comunque una riga di ore modificabile dal suo autore;
- **granularità per giornata e attività**, non flusso continuo di orari di entrata e uscita: l'app non ricostruisce
  l'orario di lavoro di nessuno, e non è quindi un sistema di rilevazione presenze;
- **le viste aggregate sono per commessa, non per persona**: nessuna classifica di produttività, nessun punteggio,
  nessun indicatore «ore per dipendente» come schermata di prodotto. Il totale per persona esiste solo *dentro* un
  progetto e un periodo, perché senza non si può fatturare né calcolare un costo;
- **trasparenza verso chi è misurato**: ogni persona vede sempre le proprie righe, le corregge finché il periodo è
  aperto, e vede scritto in chiaro cosa il titolare vede di lei;
- **nessuna decisione automatizzata** sulle persone: l'app non valuta, non assegna punteggi, non segnala «poco
  produttivi».

**Esportazione e cancellazione.** Ogni tabella con dati di persone deve comparire **sia** in `exportData` **sia**
in `purgeData` del contratto dati (`ProgettiDataContract`): `project` (referente), `task`, `assignment`,
`time_entry`, `comment`, `attachment` (riga e file nell'archivio), `project_cost`, `billable_batch_line`, più le
colonne di controllo di tutte le tabelle. Dimenticarne una è il difetto di conformità più probabile.
La cancellazione è **fisica**: sostituire il nome di una persona con un codice non è cancellare. Attenzione al
caso proprio di questa app: cancellare le ore di una persona **cambia il consuntivo di una commessa già
fatturata** — la storia 0030 deve dire come si concilia il diritto dell'interessato con la traccia contabile
(rimane il totale aggregato del lotto già consegnato, spariscono le righe individuali), ed è un punto che va
validato (§11.5).

**Integrazioni esterne.** Nessuna in questa stesura (§2.4): nessun fornitore esterno riceve dati personali. Se in
futuro entrassero calendario, messaggistica o archivio di file, ciascuno sarebbe un nuovo responsabile del
trattamento da aggiungere all'elenco dei fornitori e all'informativa.

**Classificazione della change.** Una app nuova introduce finalità e categorie nuove: è un cambiamento
**sostanziale**. Qui lo è in modo particolare, perché introduce nella piattaforma la prima finalità che riguarda
**i lavoratori del cliente** e non i clienti del cliente. Va trattata come tale: la classificazione descrive la
realtà, non è una leva per evitare adempimenti.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `list_projects` | `(stato?, cliente?) → elenco di progetti con avanzamento e budget residuo` | dice quali commesse sono aperte e come stanno | lettura | no |
| `get_project_progress` | `(id_progetto) → avanzamento, traguardi, ore consumate su budget, ritardi` | è l'azione richiesta dalla scheda di catalogo | lettura | no |
| `get_my_tasks` | `(periodo?) → le attività assegnate a chi sta parlando` | l'agenda personale; **restituisce solo le attività di chi chiama**, mai quelle di altri | lettura | no |
| `search_tasks` | `(testo?, progetto?, stato?, scadenza?) → elenco di attività minimizzato` | ricerca trasversale | lettura | no |
| `get_time_summary` | `(progetto, periodo) → ore per attività e per riga, totali fatturabili e non` | il consuntivo delle ore di una commessa | lettura | no |
| `get_project_margin` | `(id_progetto) → ricavo previsto, costo delle ore, costi esterni, margine` | la domanda «ci ho guadagnato?» | lettura | no |
| `create_task` | `(progetto, titolo, scadenza?, stima?) → bozza di attività` | crea un'attività | scrittura | **sì** |
| `assign_task` | `(id_attività, persona) → bozza di assegnazione` | assegna a una persona: tocca l'organizzazione del lavoro altrui, quindi non è mai automatica | scrittura | **sì** |
| `update_status` | `(id_attività, nuovo stato) → bozza di cambio di stato` | fa avanzare l'attività | scrittura | **sì** |
| `log_time` | `(attività, data, durata, fatturabile?, nota?) → bozza di riga di ore` | dichiara ore **solo a nome di chi sta parlando**: l'assistente non può mai scrivere ore per un'altra persona (§6) | scrittura | **sì** |
| `close_period` | `(progetto, periodo) → esito del blocco delle righe` | blocca le ore di un periodo: da lì in poi si corregge solo con una rettifica tracciata | scrittura con effetto difficilmente reversibile | **sì, obbligatoria** |
| `handoff_billable_lines` | `(progetto, periodo) → esito della consegna alla fatturazione` | manda le righe fatturabili all'app di fatturazione: è un effetto **verso un'altra app** e a valle diventa un documento | scrittura irreversibile | **sì, obbligatoria** |

**Lettura.** Gli strumenti che giustificano il livello conversazionale in questa app sono tre, e sono tutti di
lettura: `get_my_tasks` («cosa devo fare oggi»), `get_project_progress` («a che punto è il cantiere Verdi») e
soprattutto `get_project_margin` («sul lavoro per Rossi ci abbiamo guadagnato?»). Sono domande che un elenco non
risponde da solo e che nessuno si mette a cercare aprendo un rapporto: chiedendole a voce, la risposta arriva a
chi non entrerebbe mai nell'app. `log_time` è il caso opposto ed è quello che potrebbe cambiare l'adozione: la
resistenza al foglio ore è di attrito, e «segna due ore sul cantiere Verdi per ieri» è l'attrito più basso
possibile — a patto che resti una bozza da confermare e resti limitato a sé stessi.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, è accesa, è vuota e utilizzabile: servizio avviato, schema creato, modulo
visibile nella barra laterale, posti governati dal piano, avvio locale senza passi manuali.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Il servizio `progetti` nasce dal generatore, risponde su `/api/progetti/v1/health` e si avvia in locale |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_progetti`, prime tabelle con `tenant_id`, colonne di controllo e cancellazione logica |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Modulo registrato, sezioni, cinque lingue, colore-categoria, tema chiaro e scuro |
| [0004](01-fondamenta/0004-posti-abbonamento-e-quota.md) | Posti, abbonamento e quota | Proiezione dell'abilitazione, conteggio dei posti occupati, `402`/`429`, blocco del passaggio a un piano inferiore |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | Dati di prova inventati e deterministici, e la garanzia che l'app parta senza cablaggi a mano |

### Epica 02 — Progetti e struttura del lavoro

Alla fine dell'epica si può descrivere il lavoro: progetti, attività e sotto-attività, traguardi, e la scorciatoia
per non ricominciare ogni volta da capo.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-progetti-e-struttura-del-lavoro/0006-anagrafica-del-progetto.md) | Anagrafica del progetto | Creare una commessa con titolo, cliente, referente, date e stato; utilizzabile con il solo titolo |
| [0007](02-progetti-e-struttura-del-lavoro/0007-attivita-e-sotto-attivita.md) | Attività e sotto-attività | L'attività dentro il progetto, con un solo livello di sotto-attività e stati fissi |
| [0008](02-progetti-e-struttura-del-lavoro/0008-elenco-delle-attivita-e-filtri.md) | Elenco delle attività e filtri | Vista a elenco con ricerca, filtri e ordinamento, paginata |
| [0009](02-progetti-e-struttura-del-lavoro/0009-traguardi-del-progetto.md) | Traguardi del progetto | Tappe con una data, a cui si agganciano le attività |
| [0010](02-progetti-e-struttura-del-lavoro/0010-modelli-di-progetto.md) | Modelli di progetto | Salvare una struttura ricorrente e generarne un progetto nuovo |

### Epica 03 — Esecuzione quotidiana

Alla fine dell'epica la squadra ci lavora dentro davvero: lavagna, assegnazioni e scadenze, l'agenda personale,
la conversazione sull'attività e gli avvisi di ritardo.

| # | Storia | In una riga |
|---|---|---|
| [0011](03-esecuzione-quotidiana/0011-lavagna-a-colonne.md) | Lavagna a colonne | Le attività disposte per stato, spostabili col trascinamento e da tastiera |
| [0012](03-esecuzione-quotidiana/0012-assegnazione-e-scadenze.md) | Assegnazione e scadenze | Chi fa cosa entro quando, con la vista del carico per persona limitata al progetto |
| [0013](03-esecuzione-quotidiana/0013-le-mie-attivita-di-oggi.md) | Le mie attività di oggi | L'unica schermata che un esecutore apre ogni mattina |
| [0014](03-esecuzione-quotidiana/0014-commenti-sulle-attivita.md) | Commenti sulle attività | La discussione resta attaccata al lavoro, non nella messaggistica |
| [0015](03-esecuzione-quotidiana/0015-allegati-delle-attivita.md) | Allegati delle attività | File di lavoro sull'attività, con limiti espliciti |
| [0016](03-esecuzione-quotidiana/0016-avvisi-di-ritardo.md) | Avvisi di ritardo | Scadenze superate e attività ferme segnalate dentro l'app |

### Epica 04 — Ore lavorate e fatturabilità

È il cuore dell'applicazione e la ragione per cui esiste. Alla fine dell'epica le ore dichiarate diventano
denaro: tariffe, foglio ore settimanale, chiusura del periodo, budget e sforamento, consegna alla fatturazione.

| # | Storia | In una riga |
|---|---|---|
| [0017](04-ore-lavorate-e-fatturabilita/0017-registrazione-dichiarativa-delle-ore.md) | Registrazione dichiarativa delle ore | La riga di ore su un'attività, dichiarata e non rilevata: il confine con la sorveglianza |
| [0018](04-ore-lavorate-e-fatturabilita/0018-tariffe-e-righe-fatturabili.md) | Tariffe e righe fatturabili | Tariffa oraria del progetto, distinzione fra fatturabile e non fatturabile |
| [0019](04-ore-lavorate-e-fatturabilita/0019-foglio-ore-settimanale.md) | Foglio ore settimanale | Inserire e correggere una settimana di ore in una schermata sola, sotto i trenta secondi |
| [0020](04-ore-lavorate-e-fatturabilita/0020-chiusura-del-periodo-e-blocco-delle-ore.md) | Chiusura del periodo e blocco delle ore | Il periodo si chiude, le righe si bloccano, le correzioni diventano rettifiche tracciate |
| [0021](04-ore-lavorate-e-fatturabilita/0021-budget-di-commessa-e-sforamento.md) | Budget di commessa e sforamento | Budget in ore e in importo, con l'avviso **prima** dello sforamento, non a fine mese |
| [0022](04-ore-lavorate-e-fatturabilita/0022-consegna-delle-righe-alla-fatturazione.md) | Consegna delle righe alla fatturazione | Il lotto fatturabile del periodo esce verso 02 BillGrove, una volta sola |

### Epica 05 — Avanzamento, margine e catena della suite

Alla fine dell'epica l'app risponde alle due domande del titolare — «a che punto siamo» e «ci abbiamo
guadagnato» — e si aggancia alle app a monte e a valle.

| # | Storia | In una riga |
|---|---|---|
| [0023](05-avanzamento-margine-e-catena-della-suite/0023-progetto-da-preventivo-accettato.md) | Progetto da preventivo accettato | Un preventivo accettato in 06 QuoteGrove fa nascere il progetto con budget e righe |
| [0024](05-avanzamento-margine-e-catena-della-suite/0024-costi-esterni-della-commessa.md) | Costi esterni della commessa | Materiali, fornitori e spese imputate al progetto, a mano o da 08 SpendGrove |
| [0025](05-avanzamento-margine-e-catena-della-suite/0025-avanzamento-del-progetto.md) | Avanzamento del progetto | Percentuale di completamento, traguardi raggiunti, ritardi, in una schermata |
| [0026](05-avanzamento-margine-e-catena-della-suite/0026-redditivita-per-commessa.md) | Redditività per commessa | Ricavo previsto meno costo delle ore e costi esterni: il margine, senza classifiche di persone |
| [0027](05-avanzamento-margine-e-catena-della-suite/0027-esportazione-dei-rapporti.md) | Esportazione dei rapporti | Consuntivo e ore in formato tabellare, più le scadenze in formato calendario |

### Epica 06 — Esposizione conversazionale e prove end-to-end

Alla fine dell'epica l'app è comandabile da una chat entro i limiti di sicurezza, i diritti dell'interessato sono
serviti e il percorso end-to-end `[J-PROGETTI]` è nel registro di copertura.

| # | Storia | In una riga |
|---|---|---|
| [0028](06-esposizione-conversazionale-e-prove/0028-strumenti-di-lettura.md) | Strumenti di lettura | Il contratto dei sei strumenti di lettura, con minimizzazione dei dati |
| [0029](06-esposizione-conversazionale-e-prove/0029-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con conferma | Bozza e conferma umana per le sei azioni che scrivono |
| [0030](06-esposizione-conversazionale-e-prove/0030-esportazione-e-cancellazione.md) | Esportazione e cancellazione | Il contratto dati dell'app: tutte le tabelle, cancellazione fisica, ore già fatturate |
| [0031](06-esposizione-conversazionale-e-prove/0031-percorso-end-to-end.md) | Percorso end-to-end | `[J-PROGETTI]`: dal progetto alla riga fatturabile, in un solo percorso automatico |

**Totale**: 6 epiche, 31 storie (`0001`-`0031`). Dentro la fascia raccomandata (4-7 epiche, 4-8 storie per epica,
20-45 storie).

---

## 9. Estensioni della console di amministrazione

Servono tre cose oltre lo standard, tutte piccole: una **deroga temporanea sul tetto dei posti** (una migrazione
iniziale fa entrare tutti insieme prima che il cliente decida il piano), una **vista diagnostica sugli eventi di
suite** in ingresso da 06 QuoteGrove e 08 SpendGrove e in uscita verso 02 BillGrove (è il punto in cui il cliente
dirà «le ore non sono arrivate in fattura»), e la **ripetizione di una consegna fallita**. Nessun accesso ai
contenuti: solo metadati, stati e conteggi.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| 06 — QuoteGrove (preventivi) | **dipende da** (facoltativamente) | Il preventivo accettato genera il progetto con il suo budget in ore e in importo: è l'ingresso della catena (storia 0023) |
| 02 — BillGrove (fatturazione) | **alimenta** | Il lotto di righe fatturabili del periodo diventa una fattura: è l'uscita della catena e il punto in cui l'app produce denaro (storia 0022) |
| 08 — SpendGrove (note spese) | **si fa alimentare da** | Le spese approvate con un riferimento di commessa diventano costi del progetto e abbassano il margine (storia 0024) |
| 04 — LeadGrove (mini-CRM) | **condivide dati con** | L'anagrafica clienti condivisa: il progetto punta al cliente, non ne tiene una seconda copia |
| 09 — HrGrove (personale) | **confina con** | L'anagrafica dei collaboratori e — soprattutto — **le assenze**, che restano di HrGrove: è il confine che tiene FlowGrove fuori dall'articolo 9 (§6) |
| 11 — ShiftGrove (turni) | **si sovrappone a** | Entrambe parlano di «chi lavora quando». Confine: ShiftGrove pianifica la presenza, FlowGrove consuntiva il lavoro su commessa. Da tenere d'occhio |
| 12 — DeskGrove (assistenza) | **si sovrappone a** | Un ticket e un'attività si assomigliano. Confine: il ticket nasce dal cliente ed è reattivo, l'attività nasce dal progetto ed è pianificata |

**Lettura, e va detta senza giri di parole: da sola FlowGrove non ha molte ragioni di esistere.** Contro ClickUp
gratuito e Plane installabile, una lavagna a pagamento non si vende. Dentro la suite invece copre l'unico buco
della catena del valore che il catalogo stesso indica come argomento di vendita più forte (§6 del catalogo,
«preventivo → ordine → fattura → incasso»): fra il preventivo accettato e la fattura c'è **il lavoro**, e oggi
quel pezzo non lo tiene nessuna app della suite. FlowGrove è quel pezzo. È anche la ragione per cui il listino
(§5) è volutamente basso: il valore che genera lo incassano le app a valle.

**Sovrapposizioni da evitare.** FlowGrove **non** deve ricostruire l'anagrafica clienti (è di 04/02), **non**
deve gestire assenze e presenze (09/11), **non** deve emettere documenti (02/06). Se una di queste tentazioni
passa, l'app diventa un gestionale a metà e perde la sua unica difesa, che è la collocazione.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **FlowGrove è un'app o una funzione di BillGrove?** La proposta la tratta come app a sé, con un listino basso e dichiaratamente sotto il catalogo (§5). L'alternativa seria è che «progetti e ore» sia una sezione dell'app di fatturazione, come è già l'e-invoicing rispetto a BillGrove (§6 del catalogo) | È una decisione di **direzione di prodotto** e di prezzo: nessun agente la prende. Il dato che la deciderebbe — quanto vale il margine di commessa da solo — non l'ho trovato (§2.7) | sviluppatore |
| 2 | **Prezzi, tetti dei posti e durata della prova** (§5) | Fermata di escalation di piattaforma | sviluppatore |
| 3 | **Collegamento vivo al calendario** (Google, Microsoft 365) per le scadenze: molto richiesto in categoria, escluso in questa stesura | Introdurrebbe un responsabile del trattamento esterno e un punto di rottura; il valore per il segmento micro è inferiore a quello della catena preventivo → fattura | sviluppatore, in una storia successiva; oggi copre parzialmente la 0027 (esportazione in formato calendario) |
| 4 | **Portale per il cliente finale** (il cliente vede l'avanzamento del proprio progetto) | È una superficie pubblica: autenticazione dei destinatari, difese contro gli abusi, informativa. È un'epica a sé, non una storia | sviluppatore; nel frattempo l'avanzamento si condivide esportandolo (storia 0027) |
| 5 | **Durata di conservazione delle righe di ore** e **conciliazione fra diritto alla cancellazione e ore già fatturate** (§6) | Non è una scelta tecnica: dipende dagli obblighi contabili del cliente titolare e dalla qualificazione del foglio ore rispetto all'articolo 4 dello Statuto dei lavoratori, che le fonti lasciano caso per caso (§2.7) | sviluppatore, con la revisione legale pre-go-live; la storia 0030 non si chiude senza |
| 6 | **Colore-categoria `violet` già proposto da 06 QuoteGrove** (§3) | Sei colori per sessanta app: la collisione è strutturale, ma queste due app sono adiacenti nel percorso d'uso | piattaforma, quando si assegnano i colori sul serio |
| 7 | **Il minimo di postazioni di monday.com** non è stato risolto (§2.7) | Discordanza fra pagina ufficiale e comparatori | chi fissa il posizionamento di prezzo |

**Rischi noti**

- **La lavagna è commodity e ha sostituti gratuiti credibili** (ClickUp gratuito con utenti illimitati, Plane e
  Vikunja installabili, §2.1) — effetto: se il cliente valuta FlowGrove come lavagna, il confronto è perso prima
  di cominciare. Attenuazione: non venderla mai come lavagna. La prima schermata che il cliente deve vedere in
  fase di vendita è il margine di commessa, non il kanban; e la prima domanda commerciale è «quanto ti costa oggi
  scoprire lo sforamento a fine mese».
- **Nessuno usa il foglio ore se costa fatica** — effetto: senza ore, il margine è finto e tutta l'epica 05
  crolla. È il rischio operativo numero uno di questa app. Attenuazione: la storia 0019 ha un requisito di
  attrito (una settimana in una schermata sola), la storia 0013 mette l'inserimento accanto a ciò che la persona
  già guarda ogni mattina, e `log_time` dalla chat abbassa ulteriormente la soglia.
- **Percezione di sorveglianza** — effetto: se i collaboratori leggono lo strumento come un controllo del
  titolare, compilano male e i dati diventano inutilizzabili; sul piano giuridico, una funzione di rilevazione
  farebbe scattare l'articolo 4 comma 1 (accordo sindacale o autorizzazione) e il precedente da 50.000 € del
  Garante (§2.3). Attenuazione: i confini di prodotto del §6, scritti come requisiti nelle storie 0017/0019/0020,
  e la trasparenza verso chi è misurato.
- **Dipendenza dalla suite** — effetto: venduta da sola l'app rende poco (§10); ma se il cliente non ha
  QuoteGrove né BillGrove, il valore residuo è basso. Attenuazione: presentarla come parte di un percorso, e
  tenere il piano gratuito a 3 posti come porta d'ingresso alla suite più che come prodotto.
- **Sovrapposizione con 11 ShiftGrove e 12 DeskGrove** — effetto: costruire due volte la stessa cosa.
  Attenuazione: i confini del §10, da riverificare quando quelle due app verranno scritte.

**Fuori dimensionamento**: non applicabile. 6 epiche, 31 storie, da 4 a 6 storie per epica: dentro la fascia
raccomandata.
