# 0018 — Operatori e posti

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 04 — Organizzazione del lavoro
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come amministratore dell'account
> voglio decidere chi della mia squadra lavora sulle richieste di assistenza, e vedere quanti posti mi restano
> così da far entrare una persona nuova in coda senza scoprire il limite del piano nel momento sbagliato.

**Contesto.** Fino a qui il posto operatore è stato un numero: la storia `0004` ha acceso il contatore e cablato il
varco (`402` senza abbonamento, `429` a posti esauriti), ma il posto non aveva ancora un soggetto. Questa storia
introduce l'entità `Agent` — la persona che risponde, con il suo nome visibile e la sua firma — ed è quindi la
storia in cui la metrica `agents` **diventa reale**: dare un posto a una persona in più oltre il tetto risponde
`429`, toglierlo lo libera nello stesso istante. Va fatta adesso, prima delle code e della presa in carico
(`0019`, `0020`), perché quelle storie hanno bisogno di sapere *chi* è un operatore: senza, tornerebbero indietro
ad aggiungere il concetto. Il punto delicato lo dichiara la descrizione dell'applicazione
([application-description.md](../application-description.md) §3): **un posto operatore non è un utente
dell'account**. Chi guarda soltanto il cruscotto del servizio non consuma nulla, e confondere le due cose farebbe
pagare al cliente persone che non rispondono a nessuno.

## 2. Requisiti funzionali

1. **RF-1** — Un amministratore dell'account può dare un posto operatore a un utente già membro dell'account:
   l'operatore nasce con un nome visibile e una firma, entrambi modificabili in seguito.
2. **RF-2** — Dare un posto quando i posti attivi hanno raggiunto il tetto del piano è rifiutato con `429`, un
   messaggio che dice quanti posti sono occupati, qual è il tetto e come si rimedia (sospendere un operatore o
   passare di piano); **nessun** operatore viene creato.
3. **RF-3** — Un operatore si **sospende** e si **riattiva**: sospeso non accede alle funzioni sulle richieste e
   **non occupa il posto**, che torna disponibile subito. L'operatore sospeso non si cancella e resta l'autore dei
   messaggi che ha già scritto.
4. **RF-4** — Un utente dell'account **senza** posto non consuma quota: qualunque azione sulle richieste gli
   risponde `403`, mentre le viste di sola lettura previste dall'epica 05 (cruscotto del servizio) restano
   accessibili.
5. **RF-5** — La sezione Impostazioni → Operatori mostra i posti occupati, il tetto del piano, chi li occupa e chi
   è sospeso, con le azioni di assegnazione, sospensione e riattivazione.
6. **RF-6** — Il passaggio a un piano con un tetto inferiore al numero di posti attivi è **bloccato**, con un
   messaggio che dice quanti operatori vanno sospesi prima di poter procedere.
7. **RF-7** — Il **nome visibile** è ciò che il cliente finale legge nelle risposte; la **firma** è un testo con
   lunghezza massima dichiarata che viene appeso ai messaggi in uscita (storia `0015`). Entrambi hanno un valore
   predefinito derivato dal profilo dell'utente e sono modificabili dall'operatore stesso.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dell'entità `Agent` filtra per `tenant_id`
  preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene
  ignorato. Il conteggio dei posti attivi si fa **dentro** l'account, mai su una vista globale.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/helpdesk/v1/agents`,
  `POST /api/helpdesk/v1/agents`, `PATCH /api/helpdesk/v1/agents/{id}` (nome visibile, firma, stato) e
  `GET /api/helpdesk/v1/quota`; corpo validato; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `agent` già creata dalla storia `0002` sullo schema `app_helpdesk`; qui la
  migrazione `V<N>__agent_seat_constraints.sql` aggiunge le colonne `display_name`, `signature`, `status`
  (`active` / `suspended`), il vincolo di unicità su `(tenant_id, user_ref)` fra le righe non cancellate e
  l'indice per il conteggio dei posti attivi a partire da `tenant_id`. Chiave primaria UUID versione 7, colonne di
  controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Impostazioni → Operatori del modulo `helpdesk`: elenco degli
  operatori, barra del consumo dei posti, azioni di assegnazione e sospensione, modulo del nome visibile e della
  firma. Dati letti con il client generato dalla definizione OpenAPI; solo token del sistema di design con il
  colore-categoria `teal`; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `helpdesk` e sono presenti in
  `en, it, fr, es, de`, **compreso** il messaggio di rifiuto per quota e quello di blocco del passaggio a un piano
  inferiore. Il nome visibile e la firma li scrive il cliente e restano nella sua lingua.
- **RT-6 — Varchi e quota (§6, §7).** Prima di attivare un operatore il servizio prenota una unità della metrica
  `agents` (natura `stock`); a tetto raggiunto risponde `429` con l'indicazione del rimedio. L'abilitazione si
  legge dalla **proiezione locale** alimentata a eventi, mai con una chiamata sincrona sul percorso caldo. Catena
  completa dei varchi: `401 → 403 → 402 → 403 → 429`. Con abbonamento in `past_due` la funzione resta accessibile;
  con `canceled` o `paused` risponde `402`; l'esportazione dei dati resta accessibile in ogni caso. La storia
  **non fissa prezzi**: consuma il tetto pubblicato dall'abilitazione.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento nuovo**, ed è una scelta dichiarata: attivare
  un posto operatore cambia ciò che il cliente paga e il passaggio di piano è un effetto verso l'esterno. Resta
  nelle mani di una persona nell'interfaccia. Lo si scrive qui perché sia una decisione e non una dimenticanza.
  Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** La storia introduce campi che riguardano una persona: `agent.display_name` e
  `agent.signature` sono dati di **dipendenti dell'azienda cliente**. Voci nuove nel manifesto
  `docs/compliance/manifests/helpdesk.yaml` in **italiano e inglese** (`agent.display_name` è già proposto al §6
  della descrizione dell'applicazione; `agent.signature` va aggiunto, perché una firma contiene tipicamente nome,
  ruolo e recapito), campi annotati `@PersonalData`, tabella `agent` presente **sia** in `exportData` **sia** in
  `purgeData` del contratto `HelpdeskDataContract`. Su DeskGrove appgrove è **responsabile del trattamento** per
  conto dell'azienda cliente, non titolare: la base giuridica del manifesto è quella del titolare e va verificata
  (vedi §8).
- **RT-9 — Registrazione eventi (§14).** Gli eventi «posto assegnato», «operatore sospeso», «operatore riattivato»,
  «assegnazione respinta per quota» e «passaggio di piano bloccato per posti eccedenti» sono registrati con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** nomi visibili né firme.

## 4. Criteri di accettazione

**CA-1 — Assegnazione dentro il tetto**
- **Dato** un account sul piano `team` (tre posti) con due operatori attivi
- **Quando** l'amministratore dà un posto a un terzo membro
- **Allora** l'operatore è creato con il nome visibile indicato e il consumo mostra «3 di 3»

**CA-2 — Quota esaurita**
- **Dato** un account con tutti i posti del piano occupati da operatori attivi
- **Quando** l'amministratore prova a darne uno a un altro membro
- **Allora** riceve `429`, un messaggio che dice occupati, tetto e rimedio, e **nessun** operatore viene creato

**CA-3 — La sospensione libera il posto subito**
- **Dato** un account al tetto dei posti
- **Quando** l'amministratore sospende un operatore e nello stesso minuto ne attiva un altro
- **Allora** la seconda attivazione riesce: la giacenza non ha finestre, e i messaggi già scritti dall'operatore
  sospeso continuano a mostrare il suo nome visibile

**CA-4 — Utente dell'account senza posto**
- **Dato** un membro dell'account a cui non è stato dato alcun posto operatore
- **Quando** apre il cruscotto del servizio e poi prova ad aprire una richiesta
- **Allora** vede il cruscotto, riceve `403` sull'azione sulla richiesta, e il consumo dei posti **non** cambia

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri operatori
- **Quando** un amministratore di `A` chiede l'elenco degli operatori forzando l'identificativo di `B` nel corpo
  della richiesta
- **Allora** vede solo gli operatori di `A`, e il conteggio dei posti di `B` resta invariato

**CA-6 — Passaggio a un piano inferiore bloccato**
- **Dato** un account sul piano `team` con tre operatori attivi
- **Quando** tenta di passare al piano `free` (un posto)
- **Allora** il passaggio è rifiutato con un messaggio che dice quanti operatori sospendere prima di procedere

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio della giacenza dei posti e di **integrazione** sulla risorsa `agents`, con
      database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** sulla risorsa `agents` e **matrice dei ruoli** (solo `owner` e `admin`
      assegnano e sospendono posti);
- [ ] **prova end-to-end**: **rimando** alla storia `0037`, proprietaria del percorso `[J-HELPDESK]` — il varco dei
      posti non sta nel percorso minimo e qui si copre con prove d'integrazione; voce `da-coprire` nel **registro
      di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo e storia
      proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), messaggi di rifiuto compresi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `agent.display_name` e `agent.signature`, campi
      annotati `@PersonalData`, tabella `agent` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotato perché la metrica è a
      giacenza e perché l'utente senza posto non consuma quota;
- [ ] contratto degli **strumenti conversazionali**: nessuno, con la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sulla sezione Operatori;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove (§3 della descrizione
      dell'applicazione, voce «metrica di quota»).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` — modello dati multi-account | Serve la tabella `agent` sullo schema `app_helpdesk` |
| Storia `0003` — guscio del modulo frontend | Serve la sezione Impostazioni dove mostrare il consumo dei posti |
| Storia `0004` — abbonamento e quota dei posti | Serve il contatore della metrica `agents` e la catena dei varchi già cablata |
| Listino `services/core/src/main/resources/pricing/helpdesk.yaml` approvato dallo sviluppatore | Senza i tetti dei piani non c'è nulla da far rispettare |
| Gestione dei membri dell'account (piattaforma) | Un posto si dà a chi è **già** membro: gli inviti non sono di questa app |

## 7. Fuori ambito

- **Le code di lavoro e il presidio** («questo operatore presidia Assistenza»): li fa la storia `0019`.
- **L'assegnazione di una richiesta a un operatore**: la fa la storia `0020`. Qui si stabilisce solo *chi può*
  riceverne una.
- **L'invito di un nuovo utente all'account**: è della piattaforma, non dell'app.
- **Il flusso di acquisto, di cambio piano e di disdetta**: è della piattaforma (fatturazione); qui si implementa
  soltanto il **blocco** del passaggio a un piano inferiore quando i posti attivi eccedono il tetto di destinazione.
- **La deroga temporanea sui posti durante una migrazione**: è un'estensione della console di amministrazione
  ([estensioni-admin.md](../estensioni-admin.md)).
- **L'uso della firma nei messaggi in uscita**: qui la firma si definisce e si conserva; ad appenderla ai messaggi
  di posta è la storia `0015`.

## 8. Punti aperti

- **Base giuridica dei dati dell'operatore nel manifesto.** Il §6 della descrizione dell'applicazione propone per
  `agent.display_name` l'«esecuzione del contratto con l'azienda cliente», mentre su tutto il resto dell'app
  appgrove agisce come **responsabile del trattamento** per conto del cliente. Le due letture non coincidono e la
  differenza non è formale: cambia chi risponde verso il dipendente. **Non la decide un agente**: la chiude lo
  sviluppatore insieme alla revisione legale pre-go-live
  ([docs/_REVISIONE-LEGALE.md](../../../../_REVISIONE-LEGALE.md)).
- **Conservazione dei dati dell'operatore.** La proposta del §6 è «finché l'operatore è attivo + 12 mesi», ma un
  operatore sospeso resta l'autore di messaggi che si conservano più a lungo: cancellare il suo nome visibile
  renderebbe illeggibile lo storico. Serve una regola esplicita — chiude lo sviluppatore, insieme alla storia
  `0036` che possiede la conservazione.
- **Tetti dei piani sulla metrica `agents`.** La proposta (`free` 1, `team` 3, `business` 10) è al §5 della
  descrizione dell'applicazione ed è una **fermata di escalation**: nessun agente fissa prezzi né limiti di piano.
