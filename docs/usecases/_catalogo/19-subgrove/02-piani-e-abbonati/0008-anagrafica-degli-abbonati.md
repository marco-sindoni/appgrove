# 0008 — Anagrafica degli abbonati

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 02 — Piani e abbonati
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetta alla reception
> voglio registrare chi si abbona con i recapiti giusti e i dati per fatturare
> così da poter mandare avvisi e solleciti a un indirizzo che funziona, senza rincorrere nessuno.

**Contesto.** L'abbonato è il **cliente del nostro cliente**: una persona che non ha alcun rapporto con appgrove
e non sa che esistiamo. Tutto quello che di lui teniamo serve a due cose sole: mandargli le comunicazioni che il
contratto e la legge impongono, e intestare correttamente ciò che deve pagare. Niente di più — e la disciplina di
tenere l'anagrafica **minima** è ciò che rende sostenibile la posizione di appgrove come responsabile del
trattamento per conto del cliente. Dentro la suite l'abbonato è anche una voce dell'**anagrafica clienti
condivisa** (§6 del catalogo): questa storia tiene un riferimento a quella, non una copia autorevole, perché due
copie autorevoli dello stesso cliente sono un problema che si scopre alla prima variazione di indirizzo.

## 2. Requisiti funzionali

1. **RF-1** — Si può creare, modificare, cercare e archiviare un abbonato con: denominazione (nome e cognome
   oppure ragione sociale), posta elettronica, telefono, indirizzo di fatturazione, identificativo fiscale, nota
   libera.
2. **RF-2** — La posta elettronica è **obbligatoria** quando l'abbonato ha o avrà un abbonamento: senza di essa
   non si possono mandare né l'avviso di rinnovo dovuto per legge né il collegamento al portale.
3. **RF-3** — L'elenco si cerca per nome, recapito e identificativo fiscale, e si filtra per «ha abbonamenti
   vivi» / «non ne ha».
4. **RF-4** — Un abbonato con abbonamenti vivi **non si archivia**: prima si cessano gli abbonamenti, e il
   messaggio lo dice.
5. **RF-5** — Il campo nota porta l'avvertenza esplicita: **non inserire dati sanitari** (per esempio la scadenza
   di un certificato medico) né altre informazioni particolari; chi ne ha bisogno usa l'app dedicata ai documenti.
6. **RF-6** — Se l'account ha anche l'anagrafica condivisa della suite, l'abbonato può essere **collegato** a una
   sua voce; il collegamento è logico e non impedisce a SubGrove di funzionare da sola.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura degli abbonati filtra per `tenant_id` preso
  dal token verificato; un identificativo di account che arrivasse dalla richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST|PATCH /api/abbonati/v1/abbonati` e
  `/api/abbonati/v1/abbonati/{id}`; validazione dichiarativa sui campi; paginazione a pagina/dimensione con
  totale; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V4__abbonato_campi.sql`: la tabella `abbonato` si arricchisce dei
  campi anagrafici, tutti con `tenant_id`, colonne di controllo e cancellazione logica. Il collegamento
  all'anagrafica condivisa è un riferimento **logico**: **vietate** le chiavi esterne e le interrogazioni fra
  schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Abbonati*: elenco con ricerca e filtro, scheda di dettaglio,
  modulo di inserimento; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, aiuti, avvertenza sui dati sanitari e messaggi di errore in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** L'anagrafica **non** consuma quota: la metrica conta gli abbonamenti, non le
  persone. Un abbonato senza abbonamenti non costa nulla al cliente, ed è voluto.
- **RT-7 — Dati personali (§10).** ⚠️ **È la storia che introduce i dati personali dell'app.** I campi si
  annotano `@PersonalData`, e devono comparire nel manifesto **e** nell'esportazione e cancellazione: la
  compilazione avviene nella storia `0009`, che è la sua gemella e **non** va rimandata a dopo il rilascio. Un
  campo annotato e non dichiarato fa fallire la compilazione, ed è un presidio, non un fastidio.
- **RT-8 — Registrazione eventi (§14).** `abbonato creato`, `abbonato archiviato`, `archiviazione rifiutata per
  abbonamenti vivi` con `tenant_id`, `app_id`, `user_id` e correlazione, **senza** nomi né recapiti.
- **RT-9 — Prove (§11).** Integrazione sulla risorsa; isolamento fra due account; prova che il registro non
  contiene dati personali.

## 4. Criteri di accettazione

**CA-1 — Creazione con recapito valido**
- **Dato** un utente con ruolo sufficiente
- **Quando** crea un abbonato con nome e posta elettronica
- **Allora** l'abbonato compare nell'elenco ed è cercabile per nome e per recapito

**CA-2 — Recapito mancante**
- **Dato** un abbonato senza posta elettronica · **Quando** si prova a sottoscrivergli un abbonamento
- **Allora** l'operazione è rifiutata con un messaggio che spiega che senza recapito non si può avvisare del
  rinnovo

**CA-3 — Archiviazione bloccata**
- **Dato** un abbonato con un abbonamento attivo · **Quando** si prova ad archiviarlo
- **Allora** il rifiuto dice quanti abbonamenti vanno cessati prima

**CA-4 — Isolamento fra account**
- **Dato** due account con un abbonato omonimo · **Quando** uno cerca per quel nome
- **Allora** trova solo il proprio, anche forzando l'identificativo dell'altro account

**CA-5 — Nessun dato personale nei registri**
- **Dato** la creazione di un abbonato · **Quando** si ispeziona il registro degli eventi
- **Allora** ci sono identificativi e non compaiono né il nome né il recapito

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sulla validazione e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** sugli abbonati;
- [ ] **prova end-to-end**: *rimando* — la creazione dell'abbonato è il secondo passo del percorso
      `[J-ABBONATI]` della storia `0033`; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: le voci di questa storia sono dichiarate in italiano e inglese **nella storia
      `0009`, che va implementata subito dopo**; nessun rilascio con campi annotati e non dichiarati;
- [ ] **registro delle decisioni** compilato: anagrafica minima, avvertenza sui dati sanitari, riferimento
      logico all'anagrafica condivisa;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | serve la tabella `abbonato` |
| storia `0003` | serve la sezione *Abbonati* |
| storia `0009` | è la gemella: senza manifesto e contratto dati, questa storia non è rilasciabile |

## 7. Fuori ambito

- l'abbonamento vero e proprio: storia `0010`;
- l'autorizzazione all'addebito: storia `0017`, che è un'entità a parte e con regole proprie;
- l'importazione in blocco da foglio di calcolo: fuori dal primo giro (punto aperto della storia `0005`);
- la deduplicazione degli abbonati: vedi punto aperto.

## 8. Punti aperti

**Doppioni.** Due iscrizioni della stessa persona, con recapiti diversi, sono la norma in reception. La proposta
non impone l'unicità del recapito, perché nella realtà una coppia condivide un indirizzo di posta e un genitore
iscrive due figli; ma senza alcun presidio l'anagrafica si sporca in fretta. **Proposta**: un avviso morbido
(«esiste già un abbonato con questo recapito, vuoi collegarti a quello?») senza mai bloccare. Chiude: lo
sviluppatore, con la direzione di prodotto.
