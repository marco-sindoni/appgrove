# 0007 — Attività e sotto-attività

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 02 — Progetti e struttura del lavoro
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile di una commessa
> voglio spezzare il lavoro in attività, e le attività grandi in qualche sotto-attività
> così da avere qualcosa di abbastanza piccolo da poter dire «fatto» e da poterci attaccare le ore.

**Contesto.** L'attività è l'unità su cui poggia tutto il resto dell'app: le ore si dichiarano su un'attività, il
budget si consuma per attività, l'avanzamento si calcola sulle attività. La decisione strutturale di questa
storia è la **profondità**: un solo livello di sotto-attività, senza gerarchie ricorsive. Non è una limitazione
tecnica, è la difesa contro la complessità che il segmento micro rifiuta
([application-description.md](../application-description.md) §2.5): con l'annidamento illimitato arrivano
l'aggregazione ricorsiva, le viste ad albero e i problemi di prestazione, per un valore che a cinque persone non
serve.

## 2. Requisiti funzionali

1. **RF-1** — Si crea un'attività dentro un progetto con il solo **titolo**; sono facoltativi descrizione,
   scadenza, stima in ore, priorità, traguardo.
2. **RF-2** — Un'attività può avere sotto-attività per **un solo livello**: una sotto-attività non può avere
   figli, e il tentativo di crearne uno viene rifiutato con una spiegazione.
3. **RF-3** — Gli stati dell'attività sono fissi e non configurabili dal cliente: `da fare`, `in corso`,
   `in verifica`, `fatta`, più `sospesa` e `annullata`. I passaggi ammessi sono quelli descritti nel modello di
   dominio ([application-description.md](../application-description.md) §4).
4. **RF-4** — Un'attività padre non si può portare a `fatta` finché ha sotto-attività aperte; il messaggio dice
   quante e quali.
5. **RF-5** — L'attività si può spostare da un progetto a un altro **solo** se non ha righe di ore; altrimenti
   l'operazione è rifiutata, perché sposterebbe ore già consuntivate su un'altra commessa.
6. **RF-6** — L'attività si cancella solo se non ha righe di ore né sotto-attività con righe di ore; la
   cancellazione è logica.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `task` filtra per `tenant_id` dal token
  verificato; la creazione verifica che il progetto indicato appartenga allo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/progetti/v1/tasks`,
  `GET|PATCH|DELETE /api/progetti/v1/tasks/{id}`, `POST /api/progetti/v1/tasks/{id}/state`; corpo validato; errori
  in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V3__attivita.sql` sullo schema `app_progetti`: la tabella `task`
  riceve `parent_task_id` con il vincolo di **profondità uno** verificato applicativamente e a livello di dato,
  gli indici per progetto, stato e scadenza; colonne di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Scheda del progetto con l'albero a un livello delle attività; creazione
  rapida in linea (una riga di testo, invio); solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi degli stati, messaggi di rifiuto e testi di aiuto in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** L'attività **non consuma quota** (la metrica è `seats`). Ruolo minimo per
  creare e modificare: `member`.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `create_task(progetto, titolo, scadenza?,
  stima?)` e `update_status(id, stato)`, entrambi **scrittura con bozza e conferma umana** (storia 0029);
  `search_tasks(...)` in lettura (storia 0028).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo in questa storia: l'assegnazione a una persona è
  della storia 0012. La descrizione dell'attività è **testo libero** e porta l'avviso in linea «non inserire dati
  sensibili».
- **RT-9 — Registrazione eventi (§14).** «Attività creata», «stato cambiato», «attività spostata» con `tenant_id`,
  `app_id`, `user_id` e correlazione; mai il titolo dell'attività, che è contenuto del cliente.

## 4. Criteri di accettazione

**CA-1 — Creazione rapida**
- **Dato** un progetto `attivo`
- **Quando** si scrive «Sopralluogo» nella riga di creazione rapida e si conferma
- **Allora** l'attività esiste in stato `da fare` e compare in cima all'elenco del progetto

**CA-2 — Profondità massima**
- **Dato** una sotto-attività
- **Quando** si tenta di crearne una figlia
- **Allora** la risposta è `422` con un messaggio che spiega che le sotto-attività non si annidano oltre un
  livello, e nulla viene creato

**CA-3 — Chiusura con figli aperti**
- **Dato** un'attività con due sotto-attività non ancora `fatte`
- **Quando** si tenta di portarla a `fatta`
- **Allora** la risposta è `409` e il messaggio elenca le due sotto-attività aperte

**CA-4 — Spostamento vietato con ore**
- **Dato** un'attività con 6 ore dichiarate
- **Quando** si tenta di spostarla in un altro progetto
- **Allora** la risposta è `409` e nulla cambia

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` crea un'attività indicando un progetto di `B`
- **Allora** riceve `404` e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sulla macchina a stati e sul vincolo di profondità, e di **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account** su tutte le rotte introdotte;
- [ ] **prova end-to-end**: coprire ora — la creazione dell'attività è il secondo passo di `[J-PROGETTI]`
      (storia 0031); voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato;
- [ ] **registro delle decisioni** compilato, con annotata la scelta della profondità uno e degli stati fissi;
- [ ] contratto degli **strumenti conversazionali**: `create_task` e `update_status` dichiarati, marcati scrittura
      con conferma;
- [ ] controllo automatico di **accessibilità** verde sull'albero delle attività;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | Un'attività vive dentro un progetto |

## 7. Fuori ambito

- l'assegnazione a una persona e la scadenza governata: storia 0012;
- la lavagna a colonne: storia 0011 (qui c'è solo la vista ad albero dentro il progetto);
- le dipendenze fra attività («questa comincia quando finisce quella») e i diagrammi temporali: **fuori
  perimetro dell'app**, dichiarato nella descrizione (§1).

## 8. Punti aperti

- Nessuno.
