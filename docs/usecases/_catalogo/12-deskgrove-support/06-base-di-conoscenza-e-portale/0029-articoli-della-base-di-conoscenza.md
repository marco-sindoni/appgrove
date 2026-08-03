# 0029 — Articoli della base di conoscenza

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 06 — Base di conoscenza e portale del richiedente
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore che ha appena scritto per la quarta volta questo mese la stessa spiegazione sui tempi di consegna
> voglio scriverla una volta sola, tenerla in un posto ordinato e decidere io quando diventa pubblica
> così da recuperare i minuti che oggi butto a riscrivere, e da smettere di dare risposte diverse alla stessa domanda.

**Contesto.** Oggi le risposte ricorrenti stanno nella testa delle persone o in un documento condiviso che nessuno
aggiorna. La ricerca di mercato colloca la base di conoscenza fra le funzioni che i concorrenti spostano sui piani
superiori (§2.6, fonte 2 della descrizione dell'applicazione), e la scelta di prodotto qui è opposta: sta in tutti i
piani, perché è una delle ragioni per cui il cliente lascia la casella condivisa. È la prima storia dell'epica perché
tutte le altre leggono ciò che questa scrive. La differenza che la rende delicata: un articolo **pubblicato** diventa
leggibile da chiunque abbia il collegamento del portale (storia `0031`). È un effetto verso l'esterno, e un effetto
verso l'esterno non si produce mai per distrazione: si conferma.

## 2. Requisiti funzionali

1. **RF-1** — Un operatore crea un articolo con titolo, corpo, categoria e lingua; l'articolo **nasce in bozza** e in
   bozza non è leggibile da nessuno fuori dall'account.
2. **RF-2** — L'account gestisce un elenco **piatto** di categorie (nome e ordine di visualizzazione); ogni articolo
   appartiene a una sola categoria. Nessuna gerarchia, nessuna sotto-categoria.
3. **RF-3** — Ogni articolo dichiara la propria **lingua**. Due articoli che sono la stessa risposta in lingue diverse
   si collegano fra loro con un identificativo di gruppo, così che il portale possa proporre la variante giusta; il
   collegamento è manuale e non c'è alcuna traduzione automatica.
4. **RF-4** — La **pubblicazione** avviene solo con una conferma esplicita che dichiara, con parole non ambigue, che
   il testo diventa leggibile da chiunque abbia il collegamento del portale. Senza quella conferma nulla viene
   pubblicato: non esiste pubblicazione automatica, né alla creazione, né a tempo, né da uno strumento
   conversazionale.
5. **RF-5** — Un articolo pubblicato si può **ritirare** con effetto immediato: torna in bozza e smette di essere
   raggiungibile pubblicamente entro la stessa richiesta, senza attese di scadenza di cache.
6. **RF-6** — L'elenco degli articoli mostra stato, categoria, lingua, autore e data dell'ultima modifica, e si filtra
   per stato, categoria e lingua, con ricerca per titolo.
7. **RF-7** — Un articolo cancellato è cancellato logicamente, sparisce dagli elenchi e dal portale, e il suo indirizzo
   pubblico si comporta esattamente come quello di un articolo mai esistito.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `knowledge_article` e `knowledge_category`
  filtra per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai
  parametri viene ignorato. Prova di isolamento fra due account su entrambe le risorse.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/helpdesk/v1/knowledge-articles`,
  `GET|PATCH|DELETE /api/helpdesk/v1/knowledge-articles/{id}`,
  `POST /api/helpdesk/v1/knowledge-articles/{id}/publish` e `.../withdraw`,
  `GET|POST /api/helpdesk/v1/knowledge-categories`. La pubblicazione richiede il consenso esplicito nel corpo
  (campo di conferma): senza, risponde `400`. Corpo validato, errori in `application/problem+json`, definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__knowledge_base.sql` sullo schema `app_helpdesk`: tabelle
  `knowledge_article` (titolo, corpo, categoria, lingua, identificativo di gruppo delle varianti, stato
  `draft|published`, data di pubblicazione, autore) e `knowledge_category`, entrambe con `tenant_id`, chiave primaria
  UUID versione 7, colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione
  logica `deleted_at`. Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Base di conoscenza» nel manifesto del modulo `helpdesk`: elenco
  con i filtri, schermata di scrittura dell'articolo, finestra di conferma della pubblicazione. Dati letti con il
  client generato dalla definizione OpenAPI; solo token del sistema di design con il colore-categoria `teal`;
  funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe dell'interfaccia dell'operatore passano dallo spazio-nomi
  `helpdesk` e sono presenti in `en, it, fr, es, de`. **Da non confondere con la lingua dell'articolo**: quella è un
  dato scritto dal cliente e può essere una qualsiasi lingua in cui l'azienda risponde — l'interfaccia che la mostra
  è tradotta in cinque lingue, il contenuto no.
- **RT-6 — Varchi e quota (§6, §7).** Scrivere e pubblicare articoli **non consuma** la metrica `agents`, che è a
  giacenza e conta i posti operatore, non il lavoro prodotto. Con abbonamento non attivo (`canceled`) le rotte
  rispondono `402`. La scrittura richiede un posto operatore; **per la pubblicazione si propone il ruolo `admin`**,
  perché rende un testo visibile al pubblico (vedi §8).
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati:
  `elenca_articoli(stato?, categoria?, lingua?) → elenco di articoli` marcato **lettura**;
  `scrivi_bozza_articolo(titolo, corpo, categoria, lingua) → bozza` marcato **scrittura**, non irreversibile perché
  produce una bozza invisibile all'esterno; `pubblica_articolo(id) → esito` marcato **scrittura con effetto verso
  l'esterno**, con **conferma umana obbligatoria** — nessun percorso conversazionale può fondere la scrittura della
  bozza e la pubblicazione, per la stessa ragione per cui non si fondono `prepara_risposta` e `invia_risposta` (§7
  della descrizione dell'applicazione). Il contratto vive dentro il servizio; il server conversazionale è di
  piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo del cliente finale**: un articolo è contenuto
  redazionale dell'azienda cliente, non riguarda una persona. Autore e ultimo modificatore sono operatori, già
  coperti dalla voce `agent.display_name` del manifesto. Resta però vero che il corpo è **testo libero scritto a
  mano**: se un operatore ci incolla il nome di un cliente, quel nome diventa pubblico alla pubblicazione. Il
  presidio è la conferma esplicita di RF-4 e l'avviso che la accompagna, non una classificazione: `knowledge_article`
  va comunque inclusa in `exportData` e `purgeData` del contratto `HelpdeskDataContract`, per la stessa ragione per
  cui ci va `canned_response` (§6 della descrizione). Ricordare che qui appgrove è **responsabile del trattamento**
  per conto dell'azienda cliente, non titolare.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `articolo creato`, `articolo pubblicato`, `articolo ritirato`,
  `articolo cancellato` e `pubblicazione respinta per mancanza di conferma` sono registrati con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione, **senza il titolo e senza il corpo dell'articolo**.

## 4. Criteri di accettazione

**CA-1 — La bozza nasce invisibile**
- **Dato** un operatore con un posto attivo
- **Quando** crea un articolo con titolo, corpo, categoria e lingua
- **Allora** l'articolo esiste in stato `draft`, compare nell'elenco interno e non è raggiungibile da nessuna
  superficie pubblica

**CA-2 — Pubblicazione solo con conferma esplicita**
- **Dato** un articolo in bozza
- **Quando** si chiama la pubblicazione **senza** il consenso esplicito nel corpo
- **Allora** la risposta è `400` in `problem+json`, l'articolo resta in bozza e nulla diventa pubblico

**CA-3 — Pubblicazione confermata**
- **Dato** lo stesso articolo · **Quando** un utente con ruolo sufficiente conferma nella finestra che dichiara che
  il testo diventerà leggibile da chiunque abbia il collegamento del portale
- **Allora** l'articolo passa a `published`, con data di pubblicazione e autore registrati

**CA-4 — Ritiro immediato**
- **Dato** un articolo pubblicato · **Quando** lo si ritira
- **Allora** torna in bozza e la richiesta pubblica successiva non lo trova più, senza attese

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri articoli e le proprie categorie
- **Quando** un utente di `A` chiede l'elenco degli articoli, anche forzando l'identificativo di `B` nel corpo o nei
  parametri
- **Allora** vede solo i propri, e la richiesta dell'articolo di `B` per identificativo diretto risponde come per un
  articolo inesistente

**CA-6 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled` · **Quando** tenta di creare o pubblicare un articolo
- **Allora** riceve `402` e nulla viene creato né pubblicato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla macchina di stato dell'articolo (bozza → pubblicato → ritirato) e di **integrazione**
      sulle risorse `knowledge-articles` e `knowledge-categories`, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su entrambe le risorse nuove;
- [ ] **prova end-to-end**: *coprire ora* — passo «scrivi un articolo e pubblicalo con conferma» del percorso
      `[J-HELPDESK]`, e registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** dell'interfaccia presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese: `knowledge_article` presente in `exportData` e
      `purgeData`, con la nota sul testo libero;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotata la scelta del ruolo
      richiesto per pubblicare e il testo della conferma;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `elenca_articoli`, `scrivi_bozza_articolo` e
      `pubblica_articolo`, quest'ultimo con conferma umana obbligatoria;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` — modello dati multi-account | Lo schema `app_helpdesk`, le colonne di controllo e la convenzione della cancellazione logica devono esistere prima di aggiungere due tabelle |
| storia `0003` — guscio del modulo frontend | La sezione «Base di conoscenza» si aggiunge a un manifesto di modulo già registrato |
| storia `0018` — operatori e posti | Autore e ultimo modificatore di un articolo sono operatori; il ruolo che può pubblicare si appoggia alla matrice dei ruoli già definita lì |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: la storia dichiara il contratto degli strumenti, non lo espone |

## 7. Fuori ambito

- **La lettura pubblica degli articoli**: la fa la storia `0031` (portale pubblico). Qui si produce solo il
  contenuto e il suo stato.
- **La ricerca dell'articolo mentre si risponde e il suo inserimento nel messaggio**: storia `0030`.
- **Il suggerimento degli articoli dentro il modulo di contatto**: storia `0033`.
- **Lo storico delle revisioni e il ritorno a una versione precedente**: rimandato. Serve a squadre editoriali, non a
  tre persone che rispondono ai clienti; aggiungerlo raddoppierebbe la taglia della storia. Se servirà, sarà una
  storia propria di questa epica.
- **La traduzione automatica di un articolo in un'altra lingua**: fuori ambito. Introdurrebbe un fornitore esterno
  che riceve testo, cioè un responsabile del trattamento in più (§6 della descrizione), per un guadagno che non è
  stato misurato.
- **L'inserimento di immagini e allegati dentro un articolo**: rimandato; usa l'archivio degli allegati della storia
  `0016` e porta con sé la questione dei file leggibili senza accesso, che è materia del portale (`0031`).

## 8. Punti aperti

- **Quale ruolo può pubblicare.** La proposta è `admin`, perché la pubblicazione rende un testo visibile al pubblico
  e non è un gesto quotidiano; l'alternativa è lasciarla a ogni operatore con un avviso più forte. È una scelta di
  direzione di prodotto con effetti verso l'esterno: **la chiude lo sviluppatore**, e la risposta finisce nel
  registro delle decisioni della change.
- **Se la base di conoscenza stia in tutti i piani.** La descrizione dell'applicazione la propone in tutti (§5), al
  contrario dei concorrenti: è una scelta di listino e il listino è una fermata di escalation. **La chiude lo
  sviluppatore** insieme al resto del listino (punto 1 dei rischi della descrizione).
- **Se un articolo pubblicato debba essere indicizzabile dai motori di ricerca.** La domanda nasce qui ma la risposta
  appartiene alla storia `0031`, che possiede la superficie pubblica: annotata perché non si perda.
