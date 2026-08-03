# 0011 — Anagrafica dei clienti

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 03 — Anagrafica dei clienti e agenda interna
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi sta alla reception
> voglio ritrovare in due lettere la scheda di chi chiama, con i suoi contatti e le sue preferenze
> così da non chiedere ogni volta il numero di telefono a una persona che viene da tre anni.

**Contesto.** Il cliente finale è l'entità che porta la quasi totalità dei dati personali di questa applicazione,
e per la piattaforma è una **categoria di interessati nuova**: non è un nostro utente, è il cliente del nostro
cliente, e non ha nessun rapporto contrattuale con appgrove. Va quindi trattato con la massima parsimonia: si
raccoglie ciò che serve a erogare l'appuntamento e a ricordarlo, nient'altro. Il catalogo (§6) indica
l'anagrafica clienti come il cuore della suite: qui nasce la versione di BookGrove, con la consapevolezza che la
condivisione fra app è un punto aperto (§11 punto 7 della descrizione).

## 2. Requisiti funzionali

1. **RF-1** — Si crea, modifica e archivia un cliente con: nome, cognome, posta elettronica, telefono, lingua
   preferita, nota interna.
2. **RF-2** — La ricerca trova un cliente per nome, cognome, telefono o posta elettronica, con poche lettere e
   senza distinzione fra maiuscole e accenti.
3. **RF-3** — Il programma **segnala i probabili doppioni** al momento della creazione (stesso telefono o stessa
   posta elettronica) e propone di unire, invece di lasciare che la stessa persona esista tre volte.
4. **RF-4** — La scheda mostra lo storico degli appuntamenti della persona, con quelli non presentati in
   evidenza.
5. **RF-5** — Almeno uno fra posta elettronica e telefono è obbligatorio: senza un contatto non si può né
   confermare né ricordare, e una prenotazione senza contatto è una mancata presentazione annunciata.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dei `cliente` filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dalla richiesta viene ignorato. **Anche la ricerca dei
  doppioni** resta dentro l'account: due attività diverse non devono poter scoprire di avere lo stesso cliente.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/prenotazioni/v1/clienti`,
  `GET|PUT|DELETE /api/prenotazioni/v1/clienti/{id}`, `POST /api/prenotazioni/v1/clienti/{id}/unione`; corpo
  validato (forma dell'indirizzo, forma del numero); errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V6__clienti.sql`: tabella `cliente` con `tenant_id`, UUID versione 7,
  colonne di controllo e cancellazione logica; indici sui campi di ricerca.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Clienti»: elenco con ricerca istantanea, scheda con storico,
  finestra di unione dei doppioni; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Interfaccia in `en, it, fr, es, de`. La **lingua preferita del cliente** è un
  campo del cliente, fra le stesse cinque, e serve alla storia `0022` per scrivergli nella sua lingua.
- **RT-6 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: `cliente.nome`,
  `cliente.email`, `cliente.telefono`, `cliente.lingua`, `cliente.note`, con finalità, base giuridica e durata
  proposte al §6 della descrizione. Campi annotati `@PersonalData`; tabella `cliente` aggiunta a `exportData` e
  `purgeData` nella storia `0012`, che chiude il contratto. **Nessun campo su salute, religione o altre categorie
  particolari**, e la nota interna porta un avviso a schermo che invita a non scriverci quelle informazioni.
- **RT-7 — Registrazione eventi (§14).** `cliente creato`, `clienti uniti` con `tenant_id`, `app_id`, `user_id` e
  correlazione — **mai nome, indirizzo o telefono**.

## 4. Criteri di accettazione

**CA-1 — Creazione e ricerca**
- **Dato** un'anagrafica vuota · **Quando** si crea «Giulia Ferrari» con telefono e si cerca «fer» · **Allora** la
  scheda compare in cima ai risultati

**CA-2 — Contatto obbligatorio**
- **Dato** il modulo di inserimento · **Quando** si salva senza né posta elettronica né telefono · **Allora**
  l'errore è chiaro e nulla viene salvato

**CA-3 — Doppioni**
- **Dato** un cliente con un certo numero di telefono · **Quando** se ne crea un altro con lo stesso numero
- **Allora** il programma lo segnala prima di salvare e propone di unire le due schede

**CA-4 — Isolamento fra account**
- **Dato** due account, ciascuno con un cliente con lo stesso indirizzo di posta
- **Quando** un utente del primo cerca quell'indirizzo
- **Allora** vede solo il proprio cliente, e nessuna segnalazione di doppione riferita all'altro account

**CA-5 — Storico**
- **Dato** un cliente con cinque appuntamenti passati, uno dei quali non presentato · **Quando** si apre la scheda
- **Allora** si vedono tutti e cinque e il non presentato è evidenziato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sul riconoscimento dei doppioni e di **integrazione** sulla risorsa `clienti`;
- [ ] prova di **isolamento fra account** su ricerca, lettura e riconoscimento dei doppioni;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-BOOKGROVE]` della storia `0033`, dove si aggiorna
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con tutte le voci del cliente, campi annotati
      `@PersonalData`;
- [ ] **registro delle decisioni** compilato: contatto obbligatorio, gestione dei doppioni, assenza di campi su
      categorie particolari;
- [ ] avvio locale invariato; i dati di prova comprendono clienti inventati con indirizzi su dominio `*.test`;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | schema e isolamento |
| storia `0003` | la sezione «Clienti» del modulo |

## 7. Fuori ambito

- esportazione, cancellazione e manifesto completo: storia `0012`, che chiude il contratto dati;
- il consenso ai canali di recapito: storia `0021`;
- la condivisione dell'anagrafica con le altre app della suite: punto aperto di piattaforma (§11 punto 7 della
  descrizione).

## 8. Punti aperti

**L'unione di due schede è irreversibile.** Unire due clienti fonde storici e contatti e non si può disfare senza
una copia di sicurezza. Proposta: chiedere una conferma esplicita che mostri cosa resterà e cosa sparirà, e
registrare l'operazione. Se servisse la reversibilità, va progettata a parte.
