# 0021 — Rubrica dei contatti

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 05 — Contatti, campagne e recupero
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio una rubrica dei clienti che mi scrivono, con quello che hanno comprato
> così da riconoscere chi ho davanti prima di rispondere, invece di scorrere mesi di messaggi.

**Contesto.** I contatti esistono dalla storia `0002` ma nessuno li vede: nascono di lato, come effetto della
ricezione. Questa storia li rende un oggetto di lavoro — con una scheda, uno storico e la possibilità di
aggiungerne a mano — ed è il presupposto dei segmenti e delle campagne. Tocca la prima delle entità condivise
del catalogo (l'anagrafica clienti, §6 del catalogo): è la porta d'ingresso naturale al CRM della suite.

## 2. Requisiti funzionali

1. **RF-1** — L'elenco dei contatti si cerca per nome e numero, si filtra per stato del consenso e si pagina.
2. **RF-2** — La scheda del contatto mostra numero, nomi, lingua, stato del consenso con la sua origine, note
   interne, conversazioni e ordini con il totale speso.
3. **RF-3** — Il negozio può modificare il nome che dà al contatto e le note interne; il nome che il contatto
   si è dato **non** si modifica.
4. **RF-4** — Si può aggiungere un contatto a mano indicando numero e nome, dichiarando l'origine del consenso.
5. **RF-5** — Un contatto si può cancellare; la cancellazione è **fisica** e porta con sé conversazioni,
   messaggi, carrelli e il collegamento agli ordini, secondo il contratto dati dell'app.
6. **RF-6** — Il numero è mostrato in forma normalizzata, con il prefisso internazionale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `contact` filtra per `tenant_id` preso
  dal token verificato; lo stesso numero in due account resta due contatti distinti.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/chat_commerce/v1/contacts`,
  `GET|PUT|DELETE /api/chat_commerce/v1/contacts/{id}`; corpo validato (numero in formato internazionale);
  paginazione con totale; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Migrazione `V14__contatti_note.sql`: colonne di nota e lingua su `contact`. La
  cancellazione del contatto è **fisica** e transazionale su tutte le tabelle collegate; lascia una riga di
  prova nel registro delle purghe.
- **RT-4 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese per la nota interna
  (testo libero riferito a una persona) e per la lingua; campi annotati `@PersonalData`. Questa storia è
  l'occasione per **verificare l'intera catena**: tutte le tabelle dichiarate al §6 della descrizione
  dell'applicazione devono comparire in `exportData` e `purgeData`.
- **RT-5 — Modulo frontend (§3, §4, §5).** Sezione Contatti del modulo `chat_commerce`; il campo nota porta
  l'avviso di non inserire dati sensibili. Tutte le stringhe in `en, it, fr, es, de`; solo token del sistema di
  design; tema chiaro e scuro.
- **RT-6 — Ruoli (§6).** Tutti i ruoli leggono e annotano; solo `owner` e `admin` cancellano un contatto.
- **RT-7 — Registrazione eventi (§14).** `contatto creato a mano`, `contatto cancellato` con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione — **senza** il numero di telefono.

## 4. Criteri di accettazione

**CA-1 — Scheda con storico**
- **Dato** un contatto con due ordini pagati per 45,00 € complessivi
- **Quando** si apre la sua scheda
- **Allora** si vedono i due ordini, il totale speso e la conversazione da cui provengono

**CA-2 — Ricerca**
- **Dato** trecento contatti · **Quando** si cerca una parte del numero · **Allora** l'elenco mostra i contatti
  corrispondenti, paginati, con il totale dei risultati

**CA-3 — Aggiunta a mano**
- **Dato** un titolare che ha raccolto un numero in negozio
- **Quando** lo aggiunge indicando l'origine del consenso
- **Allora** il contatto compare con quel consenso e la sua origine, non con un consenso presunto

**CA-4 — Cancellazione fisica**
- **Dato** un contatto con conversazioni, messaggi e un carrello
- **Quando** lo si cancella
- **Allora** nessuna riga collegata resta nel database, resta la prova nel registro delle purghe, e gli ordini
  restano solo se privi di riferimenti alla persona secondo quanto dichiarato nel manifesto

**CA-5 — Isolamento fra account**
- **Dato** la stessa persona come contatto in due account
- **Quando** `A` la cancella · **Allora** il contatto di `B` resta intatto

**CA-6 — Ruolo insufficiente**
- **Dato** un utente `member` · **Quando** tenta di cancellare un contatto · **Allora** riceve `403`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla normalizzazione del numero e di **integrazione** sulla cancellazione fisica a
      cascata;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sui contatti;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato e **verificato per intero**: ogni tabella con dati personali presente
      in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta di non modificare il nome dato dal contatto;
- [ ] contratto degli **strumenti conversazionali**: `riepiloga_contatto` dichiarato come **lettura**, con
      risultato minimizzato (niente elenco completo dei messaggi);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0010` | La scheda mostra il consenso con la sua origine |
| `0016` | La scheda mostra gli ordini e il totale speso |

## 7. Fuori ambito

- l'importazione della rubrica da file: possibile in futuro, ma introdurrebbe una raccolta massiva di numeri
  senza origine del consenso — va progettata con attenzione, non aggiunta di corsa;
- la fusione di due contatti duplicati: utile, è una storia a sé;
- la sincronizzazione con l'anagrafica clienti della suite (app 4): sarebbe a eventi, fuori da questa app.

## 8. Punti aperti

- **Importazione massiva di numeri.** È la richiesta che arriverà per prima dopo il rilascio ed è anche la più
  delicata: numeri raccolti altrove, senza prova del consenso, usati per invii promozionali. Se si farà,
  servirà l'origine obbligatoria per ogni riga. Decisione dello sviluppatore, non di questa storia.
