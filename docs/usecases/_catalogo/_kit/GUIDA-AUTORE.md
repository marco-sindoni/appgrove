# Guida d'autore — come si scrive la cartella di UNA applicazione del catalogo

**Chi sei.** Sei l'agente incaricato di **una sola** applicazione del catalogo
[appgrove-catalogo-applicazioni.md](../appgrove-catalogo-applicazioni.md). Sessanta agenti come te lavorano in
parallelo, ognuno senza vedere il lavoro degli altri. Questa guida è il **metro unico**: se la segui alla lettera,
la tua cartella e le altre 59 saranno indistinguibili nella forma, per quanto diverse nel contenuto.

**Prima di iniziare, leggi:**

1. la **scheda della tua app** nel catalogo (sezione 2 se il numero è fra 1 e 30, sezione 3 se è fra 31 e 60);
2. [PRINCIPI-APPGROVE.md](PRINCIPI-APPGROVE.md) — i quindici vincoli di piattaforma che ogni storia deve rispettare;
3. i tre modelli: [TEMPLATE-application-description.md](TEMPLATE-application-description.md),
   [TEMPLATE-user-story.md](TEMPLATE-user-story.md), [TEMPLATE-estensioni-admin.md](TEMPLATE-estensioni-admin.md);
4. il guscio dell'artefatto: [TEMPLATE-artefatto-ux.html](TEMPLATE-artefatto-ux.html).

**Lingua: italiano, sempre.** Documenti, titoli, tabelle, commenti nell'HTML, testi visibili del mockup. In inglese
restano solo gli identificatori tecnici: nomi di file, di simboli, comandi, chiavi di configurazione. Niente sigle
non spiegate ([CLAUDE.md](../../../../CLAUDE.md), sezione «Lingua»).

---

## 1. Struttura della cartella — esattamente questa

La cartella della tua applicazione vive accanto a questo kit, dentro
`docs/usecases/_catalogo/`, e si chiama **`NN-<slug>`** dove `NN` è il numero di catalogo a **due cifre**
(01…60, con lo zero davanti quando serve) e `<slug>` è il nome dell'app in minuscolo con i trattini.

```
docs/usecases/_catalogo/32-tokengrove/
├── application-description.md        ← documento capofila: identità + mercato + indice delle storie
├── estensioni-admin.md               ← cosa serve alla console di amministrazione
├── artefatto-ux.html                 ← mockup navigabile (copia del guscio, riempita)
├── 01-fondamenta/
│   ├── 0001-impianto-del-servizio.md
│   ├── 0002-modello-dati-multi-account.md
│   ├── 0003-guscio-del-modulo-frontend.md
│   ├── 0004-abbonamento-e-quota.md
│   └── 0005-avvio-locale-e-dati-di-prova.md
├── 02-tracciamento-della-spesa/
│   ├── 0006-raccolta-delle-chiamate.md
│   ├── 0007-attribuzione-per-squadra.md
│   └── …
├── 03-…/
└── 06-esposizione-conversazionale-e-prove/
    ├── 0031-contratto-degli-strumenti-di-lettura.md
    ├── 0032-strumenti-di-scrittura-con-conferma.md
    └── 0033-percorso-end-to-end-dell-app.md
```

**Niente altri file.** Niente `README.md` di cartella, niente note sparse, niente file di appunti: il documento
capofila è `application-description.md` ed è lui l'indice.

---

## 2. Regola di numerazione — non negoziabile

| Cosa | Forma | Esempio | Note |
|---|---|---|---|
| Cartella dell'app | `NN-<slug>` | `07-bookgrove` | `NN` = numero di catalogo, **due cifre**, zero davanti |
| Cartella dell'epica | `NN-<slug-epica>` | `01-fondamenta`, `02-agenda-e-prenotazioni` | `NN` riparte da `01` **dentro ogni app** |
| File di storia | `NNNN-<slug-storia>.md` | `0014-promemoria-al-cliente.md` | `NNNN` progressivo **a livello di applicazione** |

La numerazione delle storie **non si azzera** a ogni epica: la prima storia dell'app è `0001`, l'ultima è
`0001 + (numero totale di storie − 1)`, qualunque epica la contenga. Serve a poterle citare senza ambiguità
(«storia 0021 di BookGrove») e a rendere evidenti i buchi.

Lo **slug** è in italiano, minuscolo, con trattini, senza articoli inutili e senza accenti
(`0009-invio-del-preventivo.md`, non `0009-l-invio-del-preventivo-al-cliente-finale.md`).

**Attenzione a non confondere due numerazioni.** Le storie del catalogo usano una numerazione **locale all'app** e
**non** consumano i numeri assoluti degli use case del repository (`docs/usecases/<area>/NNNN-*.md`, oggi arrivati
a 0097). Quando una di queste applicazioni verrà davvero costruita, i suoi use case reali riceveranno numeri
assoluti dalla skill `new-usecase`: qui stiamo scrivendo il **materiale di partenza**, non il catalogo ufficiale.

---

## 3. Quante epiche, quante storie — regola di dimensionamento

**Indicativamente 4-7 epiche per applicazione, 4-8 storie per epica.** Un'app che ne ha meno è probabilmente
descritta a un livello troppo alto; una che ne ha di più sta anticipando lavoro che nessuno ha chiesto.
Fascia sana: **20-45 storie in tutto**. Se esci da questa fascia, dillo e motivalo nella sezione «Rischi e punti
aperti» della descrizione dell'applicazione — non falsificare la struttura per rientrarci.

Due epiche sono **obbligatorie** e stanno agli estremi:

- **la prima epica è sempre `01-fondamenta`**: impianto del servizio (istanza di scaffolding, rotte, definizione
  delle interfacce), modello dati multi-account (schema `app_<app_id>`, migrazioni, `tenant_id`, colonne di
  controllo), guscio del modulo frontend (manifesto, registrazione, sezioni, cinque lingue, tema),
  abbonamento e quota (piani, metrica, varco a `429`), avvio locale e dati di prova;
- **l'ultima epica è sempre `esposizione-conversazionale-e-prove`**: contratto degli strumenti di lettura,
  strumenti di scrittura con bozza e conferma, percorso end-to-end dell'app etichettato `[J-<APP>]` e voci del
  registro di copertura.

In mezzo stanno le epiche di dominio: ricavale dai «Casi d'uso principali» e dalle «Entità di dominio» della
scheda di catalogo, raggruppando per **flusso di lavoro dell'utente**, non per strato tecnico. Un'epica si chiama
«Agenda e prenotazioni», non «API e servizi».

### Taglia di una storia

**Una storia = una change** (nel senso della skill `new-change`): un ramo, un giro di prove, un'unione.
Regola pratica: se l'implementazione richiede **più di circa un giorno di lavoro**, la storia va spezzata.

Segnali che una storia è troppo grande — se ne riconosci due, spezzala:

- tocca più di una entità principale in scrittura;
- ha più di 6-7 requisiti funzionali;
- ha una congiunzione nel titolo («creazione **e** invio **e** promemoria»);
- introduce contemporaneamente una tabella nuova, una schermata nuova e una integrazione esterna;
- i criteri di accettazione superano la mezza dozzina di scenari.

Segnali che una storia è troppo piccola — accorpala:

- non produce nulla di osservabile per l'utente («aggiungere un indice»);
- è un dettaglio implementativo di un'altra storia già scritta.

**Ordine.** Dentro l'epica, le storie stanno in ordine di dipendenza: la prima non dipende da nessuna delle
successive. Se una storia dipende da una successiva, hai sbagliato l'ordine.

---

## 4. Regola dell'analisi in rete — obbligatoria, con le fonti

La scheda di catalogo è un punto di partenza, non una ricerca di mercato. **Prima di scrivere l'indice delle
epiche devi fare almeno 4-6 ricerche mirate.** Le cinque piste, in ordine di utilità:

1. **Concorrenti reali del dominio** — chi vende già questo prodotto a micro e piccole imprese in Europa? Cerca
   nomi di prodotto, non categorie: «software gestione prenotazioni parrucchiere Italia», «fleet management
   software small business pricing».
2. **Prezzi praticati** — cosa costa davvero: fasce, unità di misura (per utente, per sede, per veicolo, per
   documento), presenza di un piano gratuito, durata della prova. Preferisci le **pagine ufficiali dei prezzi**
   ai siti di comparazione, che invecchiano male (il catalogo stesso lo avverte, §8).
3. **Obblighi normativi del settore** — cosa la legge impone a chi usa questo software: conservazione dei
   documenti, tracciabilità, dati sanitari, sicurezza sul lavoro, requisiti fiscali per giurisdizione. È la
   sorgente più frequente di requisiti che nessuno immagina e che cambiano il modello dati.
4. **Integrazioni attese** — cosa si aspetta di collegare un cliente tipo: contabilità, calendari, incassi,
   messaggistica, posta elettronica, marketplace. Ogni integrazione esterna è anche un potenziale fornitore che
   tratta dati per nostro conto: va segnalata (vedi [PRINCIPI-APPGROVE.md](PRINCIPI-APPGROVE.md) §10).
5. **Aspettative funzionali dei clienti micro e piccoli** — cosa **non** vogliono: recensioni, discussioni,
   confronti. Spesso è più informativo dell'elenco delle funzioni.

**Come si riportano.** Nella sezione «Mercato e analisi in rete» della descrizione dell'applicazione, con
**collegamento completo** e una riga su cosa hai ricavato da quella fonte. Una fonte senza collegamento non vale.

**Regola d'onestà — la più importante di questa sezione.** Quello che non hai trovato **si dice**, non si inventa.
Non esistono prezzi «stimati» presentati come rilevati, né concorrenti ricordati a memoria. Formule ammesse:

> *Non ho trovato pagine di prezzo pubbliche per i concorrenti diretti in Italia: i tre prodotti esaminati mostrano
> il prezzo solo dopo una richiesta di contatto. La proposta di listino qui sotto parte quindi dalle fasce del
> catalogo e dal confronto con la categoria adiacente `<X>`, ed è da validare.*

Il catalogo avverte esso stesso che le stime di mercato divergono anche di 3-5 volte e che molti prezzi vengono da
siti di comparazione: leggile come ordini di grandezza. Per le app **49-57** il catalogo dichiara che le fonti sono
su categorie adiacenti e non validate su concorrenti diretti — se la tua app è in quell'intervallo, la ricerca in
rete è tanto più necessaria e va detto a chiare lettere.

---

## 5. Regola della porta locale proposta — `8100 + NN`

Ogni servizio backend ha una porta locale. Poiché sessanta proposte convivono in questo documento e le app
**reali** già presenti nel repository occupano porte basse (`8081` fatture, `8082` mini-CRM, `9100` autenticazione),
la porta **proposta** per l'app di catalogo numero `NN` è:

> **porta = 8100 + NN** — esempi: app 07 → `8107`, app 32 → `8132`, app 60 → `8160`.

Nessuna collisione fra le sessanta proposte, nessuna collisione con le app reali. Va scritta nel varco d'identità
della descrizione dell'applicazione.

**La porta definitiva non la decidi tu.** Al momento dello scaffolding vero, la skill `new-application` chiede la
porta e la si verifica con `./dev.sh services`, che elenca quelle già prese. Scrivilo così:

> Porta locale proposta: `8132` (convenzione del kit: 8100 + numero di catalogo). Da confermare con
> `./dev.sh services` al momento dello scaffolding.

---

## 6. Cosa NON puoi decidere — prezzi e dati personali

Sono le due **fermate di escalation** della piattaforma: la skill `new-application` le tratta come domande che
richiedono un «sì» esplicito dello sviluppatore anche quando tutto il resto va in pilota automatico
([.claude/skills/new-application/SKILL.md](../../../../.claude/skills/new-application/SKILL.md)). Tu sei un
agente che scrive documenti: a maggior ragione **proponi, non decidi**.

**Prezzi, piani, limiti, durata della prova.** Scrivi una proposta completa e motivata — è utile e ti è chiesta —
ma intestala per quello che è:

> ⚠️ **Proposta da confermare.** I prezzi, i limiti dei piani e la durata della prova sono una **fermata di
> escalation dello sviluppatore**: nessun agente li fissa. Qui sotto una proposta motivata, da validare.

**Classificazione dei dati personali.** Vale lo stesso, con un'aggravante: se il dominio tocca **categorie
particolari** (articolo 9: salute, dati biometrici o genetici, opinioni politiche, convinzioni religiose,
orientamento sessuale, appartenenza sindacale) — cosa che accade davvero, per esempio, in un'app per studi medici,
veterinari, palestre o sicurezza sul lavoro — **fermati e segnalalo in modo forte**, in testa alla sezione, non in
una nota a piè di pagina. Non ammorbidire la classificazione per far sembrare l'app più semplice: la
classificazione descrive la realtà, non è una leva.

Vale anche per: **direzione di prodotto** (che cosa l'app deve essere), **effetti irreversibili o verso
l'esterno**, e ogni punto su cui non riesci a formulare una raccomandazione onesta. In quei casi si scrive il punto
aperto, non si sceglie.

---

## 7. L'artefatto navigabile

Copia [TEMPLATE-artefatto-ux.html](TEMPLATE-artefatto-ux.html) in `NN-<slug>/artefatto-ux.html` e riempilo:

- sostituisci **tutti** i segnaposto `@@…@@` (nome, identificativo, numero, colore-categoria, iniziale, promessa,
  metrica, entità);
- sostituisci i dati finti con qualcosa che assomigli al dominio della tua app — **inventati**, mai realistici al
  punto da sembrare dati veri di un cliente;
- tieni le schermate che servono, **cancella quelle che non servono**: un mockup con pagine vuote «tanto per
  esserci» confonde più di quanto spieghi;
- usa i riquadri di nota per spiegare **perché** una schermata è fatta così: è il motivo per cui questo artefatto
  vale più di uno screenshot;
- **nessuna risorsa dalla rete**: niente caratteri scaricati, niente librerie, niente immagini remote. Il file deve
  aprirsi con un doppio clic, anche senza connessione;
- verifica che si apra davvero prima di considerarlo finito.

Le istruzioni di dettaglio (cosa non toccare, come aggiungere una schermata, quali componenti sono già pronti)
stanno nel blocco di commento in testa al file stesso.

---

## 8. Ordine di lavoro consigliato

1. leggi la scheda di catalogo della tua app e questo kit;
2. fai le ricerche in rete (§4) e annota le fonti **mentre** le leggi, non alla fine;
3. scrivi la parte alta di `application-description.md`: descrizione, mercato, varco d'identità, listino proposto,
   dati personali proposti, modello di dominio, strumenti conversazionali;
4. disegna l'**indice delle epiche e delle storie** — è il momento in cui decidi la struttura: fallo prima di
   scrivere le storie, non dopo;
5. crea le cartelle delle epiche e scrivi le storie una per una, dal modello;
6. scrivi `estensioni-admin.md`;
7. riempi `artefatto-ux.html`;
8. torna sull'indice della descrizione e verifica che i collegamenti puntino a file che esistono davvero;
9. esegui la lista di controllo (§9).

---

## 9. Lista di controllo finale — da eseguire prima di dichiararsi finito

**Struttura**

- [ ] la cartella si chiama `NN-<slug>` con `NN` a due cifre uguale al numero di catalogo;
- [ ] contiene esattamente: `application-description.md`, `estensioni-admin.md`, `artefatto-ux.html` e le cartelle
      delle epiche; nessun file estraneo;
- [ ] le epiche sono numerate `01-…`, `02-…`, senza buchi;
- [ ] la prima epica è `01-fondamenta`, l'ultima è quella conversazionale + prove;
- [ ] le storie sono numerate `0001`, `0002`, … progressive **a livello di app**, senza buchi e senza doppioni;
- [ ] ogni file di storia corrisponde a una riga dell'indice, e viceversa.

**Dimensionamento**

- [ ] 4-7 epiche; 4-8 storie per epica; totale nella fascia 20-45 (oppure fuori fascia, ma motivato);
- [ ] nessuna storia con più di 6-7 requisiti funzionali o con una congiunzione nel titolo;
- [ ] dentro ogni epica le storie sono in ordine di dipendenza.

**Contenuto**

- [ ] ogni storia ha tutte le sezioni del modello, nessuna vuota e nessuna scritta «da definire» senza dire perché;
- [ ] i requisiti tecnici di ogni storia **richiamano per nome** gli invarianti applicabili (filtro per account,
      esposizione conversazionale, cinque lingue, prove);
- [ ] i criteri di accettazione sono in forma dato/quando/allora e sono verificabili (niente «funziona bene»);
- [ ] la definizione di fatto porta le voci di piattaforma (prove verdi, percorso end-to-end se tocca superficie,
      cinque lingue, registro delle decisioni, manifesto dei dati se tratta dati di persone).

**Analisi e onestà**

- [ ] almeno 4-6 ricerche in rete fatte, con le fonti riportate e il collegamento completo;
- [ ] ciò che non è stato trovato è dichiarato come tale, non colmato a intuito;
- [ ] il listino è marcato come **proposta da confermare** (fermata di escalation);
- [ ] la classificazione dei dati personali è marcata come **proposta**, e l'eventuale presenza di categorie
      particolari (articolo 9) è segnalata in modo forte e visibile;
- [ ] i punti che non hai potuto decidere sono nella sezione «Rischi e punti aperti», non lasciati impliciti.

**Coerenza tecnica**

- [ ] l'identificativo dell'app rispetta `^[a-z][a-z0-9_]{0,30}$`;
- [ ] la porta proposta è `8100 + NN`;
- [ ] il modello utente (`single` o `multi`) è motivato, non scelto a caso;
- [ ] la metrica di quota è **una sola** e la sua natura (`flow` o `stock`) è argomentata con un esempio nelle
      parole dell'app;
- [ ] il colore-categoria è uno fra `green, amber, red, blue, violet, teal` ed è lo stesso nel listino e nel modulo
      frontend;
- [ ] gli strumenti conversazionali distinguono lettura e scrittura, e quelli di scrittura con effetti
      irreversibili prevedono bozza + conferma umana.

**Artefatto**

- [ ] nessun segnaposto `@@…@@` rimasto;
- [ ] nessuna risorsa dalla rete;
- [ ] il file si apre in un browser e si naviga fra le schermate;
- [ ] funziona in tema chiaro e in tema scuro.

**Lingua**

- [ ] tutto in italiano, compresi i testi del mockup e i commenti che hai aggiunto;
- [ ] nessuna sigla non spiegata alla prima occorrenza.
