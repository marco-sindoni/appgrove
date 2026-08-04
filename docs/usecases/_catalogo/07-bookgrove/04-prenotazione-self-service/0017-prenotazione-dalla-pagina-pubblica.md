# 0017 — Prenotazione dalla pagina pubblica

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 04 — Prenotazione self-service del cliente finale
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che vuole un appuntamento
> voglio scegliere il servizio e l'ora e lasciare il mio nome e un contatto, in meno di un minuto e dal telefono
> così da non dover chiamare durante l'orario di lavoro.

**Contesto.** È il momento in cui la superficie pubblica **scrive** nel database di un account: una persona senza
alcun rapporto con appgrove crea una riga e occupa il tempo di qualcun altro. È diverso dal caso dell'app 06
(QuoteGrove), dove la pagina pubblica accetta un documento già esistente e già intestato. Qui si crea, e questo
impone tre cose: la minimizzazione dei dati richiesti, la verifica che il contatto sia vero (storia `0019`) e
l'impossibilità di consumare risorse dell'account — che è garantita dalla scelta della metrica di quota a
giacenza (§3 e §11 della descrizione).

## 2. Requisiti funzionali

1. **RF-1** — Dalla pagina pubblica si sceglie un servizio, poi un giorno, poi un orario fra quelli liberi, e si
   conferma lasciando **nome e un solo contatto** (posta elettronica oppure telefono, secondo quanto l'attività
   ha impostato).
2. **RF-2** — Se il contatto corrisponde a un cliente già esistente in quell'account, la prenotazione si aggancia
   alla scheda esistente invece di crearne una nuova; il visitatore non viene informato dell'esito del confronto.
3. **RF-3** — L'attività sceglie fra **conferma automatica** (la prenotazione nasce `confermata`) e **conferma
   manuale** (nasce `richiesta` e attende); la pagina dice al visitatore quale delle due si applica.
4. **RF-4** — Se lo spazio viene preso da qualcun altro fra la scelta e la conferma, il visitatore riceve un
   messaggio comprensibile e la disponibilità aggiornata, non un errore tecnico.
5. **RF-5** — A conferma avvenuta il visitatore riceve il riepilogo e il **collegamento per gestire la propria
   prenotazione** (storia `0018`), e vede a schermo la politica di disdetta che ha appena accettato (storia
   `0024`).
6. **RF-6** — Il campo delle note del visitatore è facoltativo e porta un avviso esplicito: **non scrivere qui
   informazioni sulla propria salute**.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1) — deviazione dichiarata.** Il `tenant_id` arriva dall'identificativo
  pubblico risolto dal server (storia `0016`), mai dalla richiesta. La scrittura è limitata a **una** cosa:
  creare una prenotazione per quell'account, su un servizio pubblico e su uno spazio calcolato come libero.
  Nessun'altra scrittura è raggiungibile dalla superficie pubblica.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/prenotazioni/v1/pubblico/{identificativo}/prenotazioni`;
  corpo validato con parsimonia (nome, un contatto, servizio, inizio, note facoltative); errori in `problem+json`
  con codici stabili per «spazio non più disponibile», «servizio non pubblico», «troppe richieste»; OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si usano `cliente` e `prenotazione`. La prenotazione porta
  l'**origine** (pubblica) e passa dallo stesso **vincolo di non sovrapposizione nel database** della storia
  `0014`: il presidio contro la doppia prenotazione è unico per entrambe le superfici.
- **RT-4 — Varchi e quota (§6, §7).** La superficie pubblica **non consuma quota**: la metrica
  `risorse_prenotabili` è a giacenza e nessun visitatore anonimo può esaurirla. È una proprietà voluta e va
  verificata da una prova, non solo affermata. Se l'abbonamento dell'account non è attivo, la pagina pubblica è
  sospesa e risponde con la pagina neutra: al visitatore non si mostrano mai i fatti commerciali dell'attività.
- **RT-5 — Frontend (§3, §5).** Percorso a tre passi su una sola schermata, leggibile da telefono; solo token del
  sistema di design; tema chiaro e scuro; nessun elemento che richieda autenticazione.
- **RT-6 — Cinque lingue (§4).** Tutti i testi in `en, it, fr, es, de`; il riepilogo e i messaggi successivi
  seguono la lingua scelta dal visitatore, che diventa la sua lingua preferita sulla scheda cliente.
- **RT-7 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: nome e contatto raccolti dalla
  pagina pubblica e le note del visitatore, con finalità «erogare e ricordare l'appuntamento», base giuridica
  «esecuzione del contratto fra l'attività e il suo cliente» e la durata decisa dalla storia `0012`. Campi
  annotati `@PersonalData`. **Minimizzazione**: si chiede un solo contatto, non due, e nessun dato che non serva
  a erogare l'appuntamento — niente data di nascita, niente indirizzo, niente codice fiscale.
- **RT-8 — Registrazione eventi (§14).** `prenotazione pubblica creata`, `spazio non più disponibile` con
  `tenant_id`, `app_id`, correlazione e origine — **mai nome, contatto o nome del servizio**.

## 4. Criteri di accettazione

**CA-1 — Prenotazione in meno di un minuto**
- **Dato** una pagina pubblicata con conferma automatica
- **Quando** una persona sceglie servizio, giorno e ora e lascia nome e indirizzo di posta
- **Allora** la prenotazione compare in agenda in stato `confermata` e il visitatore vede il riepilogo con il
  collegamento per gestirla

**CA-2 — Conferma manuale**
- **Dato** un'attività che ha scelto la conferma manuale · **Quando** una persona prenota · **Allora** la
  prenotazione nasce `richiesta`, la pagina lo dice chiaramente e in agenda è distinguibile da una confermata

**CA-3 — Corsa allo stesso spazio**
- **Dato** due visitatori che confermano lo stesso spazio nello stesso istante
- **Quando** le richieste vengono elaborate
- **Allora** una riesce e l'altra riceve «quest'orario è appena stato preso» con gli orari aggiornati, e in agenda
  c'è una sola prenotazione

**CA-4 — Nessun consumo di quota**
- **Dato** un account al tetto delle risorse aperte · **Quando** un visitatore prenota · **Allora** la
  prenotazione riesce: la quota è sulle risorse, non sulle prenotazioni

**CA-5 — Servizio non pubblico**
- **Dato** l'identificativo di un servizio marcato solo interno · **Quando** lo si forza nella richiesta
- **Allora** la richiesta è rifiutata e nulla viene creato

**CA-6 — Cliente già esistente**
- **Dato** una scheda cliente con quell'indirizzo di posta · **Quando** la persona prenota · **Allora** la
  prenotazione si aggancia alla scheda esistente, e la pagina pubblica non dice nulla che riveli che la scheda
  esisteva

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sulla validazione minima e di **integrazione** sulla rotta pubblica, compresa una prova
      di **concorrenza** sullo stesso spazio;
- [ ] prova di **isolamento fra account** in forma di prova di sicurezza: nessuna scrittura raggiungibile oltre a
      quella prevista, nessun accesso a entità di altri account;
- [ ] **prova end-to-end**: **coperta ora** — passo centrale del percorso `[J-BOOKGROVE-PUB]` della storia `0034`,
      con il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con i dati raccolti dalla pagina pubblica;
- [ ] **registro delle decisioni** compilato: minimizzazione dei campi richiesti, un solo contatto, e la
      dimostrazione che la superficie pubblica non consuma quota;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0016` | la pagina e la risoluzione dell'identificativo |
| storia `0014` | la prenotazione e il vincolo di non sovrapposizione |
| storia `0019` | le difese; se non è ancora fatta, questa storia **non** si mette in produzione |

## 7. Fuori ambito

- la gestione successiva della prenotazione da parte del cliente: storia `0018`;
- la lista d'attesa quando non c'è nessuno spazio: storia `0020`;
- l'acconto alla prenotazione: storia `0025`.

## 8. Punti aperti

**Quando la prenotazione diventa ferma.** Con la sola conferma automatica, chiunque può occupare un'ora con un
indirizzo inventato. La verifica del contatto (storia `0019`) risolve il problema ma aggiunge un passo e riduce
le prenotazioni completate. La proposta è: verifica obbligatoria per la conferma automatica, facoltativa se
l'attività usa la conferma manuale (perché lì il presidio è una persona). È una decisione di prodotto e va
confermata.
