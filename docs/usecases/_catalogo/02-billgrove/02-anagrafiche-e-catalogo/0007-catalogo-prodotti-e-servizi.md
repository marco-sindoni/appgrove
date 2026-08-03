# 0007 — Catalogo prodotti e servizi

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 02 — Anagrafiche e catalogo
**Storia**: `0007` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come artigiano che vende sempre le stesse dieci lavorazioni
> voglio avere le mie voci pronte con descrizione, prezzo e aliquota
> così da comporre un documento scegliendo dall'elenco invece di riscrivere ogni volta la stessa riga, con il
> rischio di sbagliare l'aliquota.

**Contesto.** Il catalogo prodotti e listini è la seconda **entità condivisa** del catalogo appgrove (§6): la stessa
voce serve ai preventivi, alla fatturazione, al magazzino e ai verticali. Qui nasce nella sua forma minima — una
voce riutilizzabile — perché senza di essa ogni riga di documento è testo libero, e il testo libero non si può
sommare, filtrare né analizzare.

## 2. Requisiti funzionali

1. **RF-1** — Si può creare, modificare, cercare e archiviare una voce di catalogo con: codice, descrizione, tipo
   (bene o servizio), unità di misura, prezzo base e aliquota predefinita.
2. **RF-2** — Il codice è univoco per account; il tentativo di riusarlo viene rifiutato con un messaggio chiaro.
3. **RF-3** — L'elenco è paginato e ricercabile per codice e descrizione.
4. **RF-4** — Una voce archiviata non compare più nella scelta ma resta leggibile nei documenti che la citano.
5. **RF-5** — La riga di documento può nascere da una voce di catalogo **oppure** essere libera: la voce propone i
   valori, non li impone.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `product` filtra per `tenant_id` preso dal
  token verificato; nessuna forzatura dall'esterno è accettata.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/billing/v1/products`,
  `GET|PUT|DELETE /api/billing/v1/products/{id}`; corpo validato; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V4__product.sql` sullo schema `app_billing`: tabella `product` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo, cancellazione logica e vincolo di unicità su
  `(tenant_id, code)`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Catalogo» del modulo `billing`: elenco con ricerca e modulo di
  inserimento. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`. Attenzione: l'unità di misura è un valore del cliente, non una stringa da tradurre.
- **RT-6 — Varchi e quota (§6).** Il catalogo **non** consuma quota. Ruolo `member` per creare e modificare.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento proprio: le voci compaiono dentro le firme di
  `crea_preventivo` e `crea_fattura` (epica 06) come riferimento facoltativo di riga.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: una voce di catalogo descrive un bene o un
  servizio, non una persona. Va dichiarato esplicitamente nel registro delle decisioni.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `voce di catalogo creata` e `voce archiviata` sono registrati
  con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Creazione di una voce**
- **Dato** un utente abilitato · **Quando** crea una voce con codice, descrizione, prezzo e aliquota
- **Allora** la voce compare nel catalogo ed è selezionabile nella composizione di un documento

**CA-2 — Codice duplicato**
- **Dato** una voce con codice `MAN-01` · **Quando** se ne crea un'altra con lo stesso codice
- **Allora** la risposta è `409` in `problem+json` e nulla viene creato

**CA-3 — Voce archiviata**
- **Dato** una voce archiviata già usata in un documento emesso
- **Quando** si apre quel documento
- **Allora** la riga resta leggibile con i valori di allora; la voce non compare più nell'elenco di scelta

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B` con cataloghi diversi
- **Quando** un utente di `A` cerca una voce, anche forzando l'identificativo di `B`
- **Allora** vede solo il proprio catalogo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sull'unicità del codice e di **integrazione** sulla risorsa, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** su `product`;
- [ ] **prova end-to-end**: *coprire ora* — passo «scegli una voce di catalogo» del percorso `[J-BILLING]`;
      registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il perché è dichiarato;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: nessuno proprio, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | La riga del documento deve poter citare una voce |
| storia `0003` | Serve il guscio del modulo per appendere la sezione «Catalogo» |

## 7. Fuori ambito

- i prezzi differenziati per cliente: storia `0008`;
- la giacenza di magazzino: non è di BillGrove (app 14 del catalogo);
- l'importazione del catalogo da file: storia `0009`, che tratta insieme clienti e voci.

## 8. Punti aperti

Nessuno.
