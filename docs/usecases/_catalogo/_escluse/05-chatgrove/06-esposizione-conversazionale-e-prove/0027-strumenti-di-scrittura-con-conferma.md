# 0027 — Strumenti di scrittura con conferma

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0023`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio poter dire «prepara l'ordine per Rina con due chili di quello di ieri e mandale la richiesta di
> pagamento», e poi vedere e approvare quello che partirà
> così da lavorare a voce senza il timore che qualcosa esca a mia insaputa.

**Contesto.** È il punto in cui il livello conversazionale diventa utile e insieme pericoloso. La regola di
sicurezza del catalogo (§8) e dei principi di piattaforma (§12) non ammette eccezioni: gli strumenti di
scrittura con effetti irreversibili producono una **bozza** e richiedono una **conferma umana esplicita**. In
quest'app la regola è ancora più stringente che altrove, perché ogni messaggio in uscita è denaro speso,
reputazione del numero e una persona reale disturbata.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara gli strumenti di scrittura: `aggiungi_al_carrello`, `crea_ordine`,
   `invia_messaggio`, `invia_modello`, `richiedi_pagamento`, `avvia_campagna`.
2. **RF-2** — Ogni strumento di scrittura è marcato per **classe di effetto**: reversibile interno (il
   carrello), scrittura interna (l'ordine), scrittura verso l'esterno (i messaggi), irreversibile con effetto
   economico (richiesta di pagamento, campagna).
3. **RF-3** — Tutti gli strumenti tranne `aggiungi_al_carrello` producono una **bozza** con l'esito esatto che
   verrà prodotto — testo del messaggio, destinatari, importo, costo in quota — e **non** eseguono nulla.
4. **RF-4** — La bozza si conferma con un'azione separata e ha una **scadenza**: passata quella, va rifatta.
   Una bozza scaduta non si può confermare.
5. **RF-5** — La conferma è **umana e attribuita**: registra quale utente ha confermato, quando e da quale
   bozza. Un'approvazione automatica non è prevista, in nessun caso.
6. **RF-6** — Gli strumenti di scrittura attraversano gli stessi controlli delle rotte corrispondenti — quota,
   consenso, finestra di servizio, ruolo — e quando uno di questi blocca, la bozza **non si crea**, con la
   spiegazione al posto suo.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Contratto dichiarato dentro il servizio dell'app; gli
  strumenti di scrittura con effetti irreversibili producono una bozza e richiedono conferma umana. Il server
  conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-2 — Isolamento fra account (§1).** Bozza e conferma vivono nell'account della sessione; una bozza di un
  account non è confermabile da un altro, e il `tenant_id` non è mai un parametro.
- **RT-3 — Varchi e quota (§6, §7).** La creazione della bozza **verifica** la quota ma non la consuma; la
  conferma la prenota. Se fra bozza e conferma la quota si esaurisce, la conferma risponde `429` e nulla parte.
- **RT-4 — Persistenza (§8).** Migrazione `V19__bozze_assistente.sql`: tabella delle bozze con `tenant_id`,
  chiave primaria UUID versione 7, contenuto della bozza, scadenza, stato e colonne di controllo. Una bozza
  confermata **non** si riconferma: la conferma è idempotente per identificativo.
- **RT-5 — Ruoli (§6).** La conferma richiede il ruolo che la corrispondente azione richiede: campagna e
  mezzi di incasso solo per `owner` e `admin`. L'assistente non aggira i ruoli.
- **RT-6 — Dati personali (§10).** La bozza contiene il testo che verrà inviato e può quindi contenere dati
  personali: la tabella delle bozze va aggiunta a `exportData` e `purgeData`, e le bozze scadute si
  eliminano — conservare per sempre messaggi mai inviati sarebbe una raccolta senza scopo.
- **RT-7 — Registrazione eventi (§14).** `bozza creata`, `bozza confermata`, `bozza scaduta`, `bozza rifiutata`
  con nome dello strumento, `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** il
  contenuto.

## 4. Criteri di accettazione

**CA-1 — Niente esce senza conferma**
- **Dato** un assistente che chiama `invia_modello` verso un contatto
- **Quando** lo strumento risponde
- **Allora** l'esito è una **bozza** con il testo esatto e il costo in quota, e **nessun messaggio è partito**
- **Quando poi** l'utente conferma la bozza
- **Allora** il messaggio parte, la quota viene consumata e la conferma resta registrata con il nome
  dell'utente

**CA-2 — Bozza scaduta**
- **Dato** una bozza creata oltre la scadenza prevista · **Quando** si tenta di confermarla · **Allora** la
  risposta è `409` e nulla parte

**CA-3 — Blocco a monte**
- **Dato** un contatto con consenso revocato e un modello promozionale
- **Quando** l'assistente chiama `invia_modello` · **Allora** la bozza **non** viene creata e l'esito spiega il
  motivo

**CA-4 — La quota finisce fra bozza e conferma**
- **Dato** una bozza di campagna da 100 destinatari e una quota che nel frattempo scende a 20
- **Quando** si conferma · **Allora** la risposta è `429`, nulla parte e la bozza resta non confermata

**CA-5 — Ruolo insufficiente**
- **Dato** un utente `member` · **Quando** conferma una bozza di campagna · **Allora** riceve `403` e nulla parte

**CA-6 — Isolamento fra account**
- **Dato** una bozza dell'account `A` · **Quando** un utente di `B` tenta di confermarla · **Allora** riceve
  `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla scadenza e sull'idempotenza della conferma, e di **integrazione** su ogni
      strumento di scrittura, verificando che **nulla** parta prima della conferma;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** su bozze e conferme;
- [ ] **prova end-to-end**: *nessun impatto* finché il livello conversazionale non esiste; dichiarato nel
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: i testi delle bozze mostrati all'utente passano dallo spazio-nomi `chat_commerce` in
      tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: tabella delle bozze in esportazione e cancellazione, con la regola di
      eliminazione delle bozze scadute;
- [ ] **registro delle decisioni** compilato, con le quattro classi di effetto e il divieto di approvazione
      automatica;
- [ ] contratto degli **strumenti conversazionali** dichiarato — **è l'oggetto della storia**;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0018`, `0023` | Sono le due azioni irreversibili da cui nasce la disciplina della bozza |
| `0026` | Riusa la forma del contratto degli strumenti |
| UC 0061-0063 | Livello conversazionale di piattaforma, non implementato |

## 7. Fuori ambito

- l'interfaccia di conferma dentro un client conversazionale: è di piattaforma;
- la composizione automatica del testo del messaggio da parte di un modello linguistico: qui la bozza contiene
  ciò che l'app produce, non ciò che un modello inventa (vedi punti aperti);
- il percorso end-to-end: storia `0029`.

## 8. Punti aperti

- **Testo generato da un modello linguistico.** Un assistente che scrive di suo pugno il messaggio al cliente
  finale è la cosa che i clienti chiederanno per prima ed è un effetto verso l'esterno con parole non nostre.
  La conferma umana lo rende gestibile, ma restano da decidere se il testo generato debba essere marcato come
  tale e chi risponda di ciò che dice. Decisione di prodotto e di responsabilità: dello sviluppatore.
- **Durata della scadenza della bozza.** Proposta: un'ora. Troppo corta irrita, troppo lunga fa confermare
  cose vecchie.
