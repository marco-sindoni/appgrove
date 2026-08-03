# 0010 — Scheda tecnica del servizio

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 03 — Scheda tecnica e storia del cliente
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatrice che ha appena finito un colore
> voglio scrivere in trenta secondi che formula ho usato e come è venuto
> così da poterlo rifare identico fra sei settimane, anche se quel giorno c'è la mia collega.

**Contesto.** È la funzione che, secondo tutti i gestionali italiani esaminati (§2.5 della descrizione), distingue
un programma per parrucchieri da un'agenda: base, tono, ossidante e volume, minuti di posa applicati, prodotti
usati, risultato. È anche la funzione che porta il rischio più grande dell'applicazione, perché nella pratica di
mercato la stessa scheda contiene «allergie e patch test» — cioè dati sulla salute. **Questa storia costruisce la
scheda; la storia `0012` costruisce il perimetro che la tiene fuori dall'articolo 9, e le due vanno lette
insieme.**

⚠️ **Vincolo dello sviluppatore, non della storia.** Il punto 4 dei rischi (§11 della descrizione) va chiuso
**prima** di implementare questa storia: è lì che si decide se la scheda potrà mai contenere informazioni di
sicurezza del trattamento e a quali condizioni. Questa storia assume la via raccomandata — **nessun dato sulla
salute** — e va riscritta se la decisione è un'altra.

## 2. Requisiti funzionali

1. **RF-1** — A un servizio eseguito si può associare una **scheda tecnica** con: data, operatore, formula
   (base, tono, ossidante e volume, minuti di posa effettivamente applicati), prodotti usati con le quantità, e un
   campo di **note tecniche**.
2. **RF-2** — La scheda si compila dall'appuntamento con un solo passaggio, e i campi arrivano già precompilati da
   ciò che il programma sa: servizio, operatore, prodotti previsti dal servizio (storia `0017`).
3. **RF-3** — La scheda cliente mostra le schede tecniche in ordine dalla più recente, con la formula in evidenza:
   deve leggersi in due secondi, non aprirsi in tre clic.
4. **RF-4** — La formula si scrive in **campi separati** (base, tono, ossidante, volume, minuti) e non come una
   riga di testo: è ciò che permette di cercarla, ripeterla (storia `0011`) e confrontarla.
5. **RF-5** — Una scheda si può correggere per un tempo limitato dopo la creazione, poi diventa **storia**: le
   correzioni successive si fanno aggiungendo una scheda nuova, non riscrivendo il passato.
6. **RF-6** — La scheda tecnica **non esiste** per i servizi che non ne hanno bisogno (un taglio, una manicure): è
   il servizio a dichiarare se ne prevede una.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle schede filtra per `tenant_id` dal token
  verificato; **anche la ricerca per formula** resta dentro l'account.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/<app>/v1/clienti/{id}/schede-tecniche`,
  `GET|PUT /api/<app>/v1/schede-tecniche/{id}`; corpo validato (volume fra valori ammessi, minuti positivi);
  errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Tabella `scheda_tecnica` con `tenant_id`, UUID versione 7, colonne di controllo e
  cancellazione logica; indice sul cliente e sulla data.
- **RT-4 — Dati personali (§10).** ⚠️ **La scheda tecnica è legata a una persona e va dichiarata nel manifesto in
  italiano e inglese**: `scheda_tecnica.formula` (dato tecnico riferito al trattamento, finalità «rifare lo stesso
  servizio», base «esecuzione del contratto», durata proposta 36 mesi dall'ultimo servizio) e
  `scheda_tecnica.note_tecniche` (**testo libero**, con l'avviso della storia `0012`). Campi annotati
  `@PersonalData`; tabella aggiunta a `exportData` e `purgeData` nella storia `0014`.
  **Nessun campo su salute, allergie, patologie, farmaci, gravidanza o esiti di test cutanei**: non ci sono, e la
  loro assenza è verificata da una prova (storia `0012`).
- **RT-5 — Modulo frontend (§3, §5).** La scheda si apre dall'appuntamento e dalla scheda cliente; la formula si
  compila con campi corti affiancati, pensati per un tablet e per le dita bagnate. Solo token del sistema di
  design, tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Etichette dei campi della formula, messaggi di errore e avviso sulla nota libera
  in `en, it, fr, es, de`.
- **RT-7 — Esposizione conversazionale (§12).** Dichiara lo strumento di lettura `scheda_tecnica_cliente(cliente,
  ultime_n?) → formule e prodotti usati`, **che non restituisce mai il campo note libere né le fotografie**; il
  contratto vive nel servizio, il server è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Registrazione eventi (§14).** `scheda tecnica creata`, `scheda tecnica corretta` con `tenant_id`,
  `app_id`, `user_id` e correlazione — **mai il contenuto della formula né il nome del cliente**.

## 4. Criteri di accettazione

**CA-1 — Compilazione in un passaggio**
- **Dato** un appuntamento di colore appena eseguito
- **Quando** si apre la scheda tecnica
- **Allora** servizio, operatore, data e prodotti previsti sono già compilati, e restano da inserire solo formula
  e risultato

**CA-2 — La formula è cercabile**
- **Dato** dieci schede tecniche di clienti diversi
- **Quando** si cerca «7.3»
- **Allora** compaiono le schede che hanno quel tono, e solo quelle del proprio account

**CA-3 — Il passato non si riscrive**
- **Dato** una scheda creata due settimane fa
- **Quando** si tenta di modificarla
- **Allora** la modifica è rifiutata e il programma propone di creare una scheda nuova

**CA-4 — I campi sanitari non esistono**
- **Dato** il modulo della scheda tecnica e la definizione delle interfacce
- **Quando** si cerca un campo per allergie, patologie, farmaci, gravidanza o esito di test cutanei
- **Allora** non ce n'è nessuno, in nessuno dei due

**CA-5 — Isolamento fra account**
- **Dato** due account con una cliente omonima
- **Quando** un utente del primo apre le schede tecniche forzando l'identificativo della cliente dell'altro
- **Allora** riceve una risposta di non trovato, indistinguibile da quella di un identificativo inesistente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; suite intera prima del
      commit);
- [ ] prove di **unità** sulla precompilazione e sulla finestra di correzione, di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** su lettura, scrittura e ricerca per formula;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-SALONGROVE]` (storia `0030`) compila una scheda
      tecnica dopo il servizio; registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
      aggiornato;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le voci della scheda tecnica, campi annotati
      `@PersonalData`, tabella dichiarata per esportazione e cancellazione (chiusura alla storia `0014`);
- [ ] **registro delle decisioni**: formula in campi separati, finestra di correzione, assenza deliberata dei campi
      sanitari con il rimando alla decisione dello sviluppatore;
- [ ] avvio locale invariato; il salone di prova contiene schede tecniche.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | la tabella |
| storia `0003` | la sezione «Clienti e schede» |
| **decisione sul punto 4 dei rischi** (§11 della descrizione) | è ciò che stabilisce fin dove la scheda può arrivare |
| anagrafica clienti di BookGrove (storia `0011` di quell'app) | la scheda si appende a un cliente che deve esistere |

## 7. Fuori ambito

- ripetere una formula passata sul servizio di oggi: storia `0011`;
- l'avviso sulla nota libera e la prova che i campi sanitari non ci sono: storia `0012`;
- le fotografie: storia `0013`;
- il consumo effettivo di prodotto: storia `0017`, che scarica il magazzino usando ciò che la scheda dichiara.

## 8. Punti aperti

**La finestra di correzione quanto dura.** Propongo la fine della giornata lavorativa: abbastanza per correggere un
errore di battitura, troppo poco per riscrivere la storia. Non ho una fonte che dica quale sia la durata giusta: è
una scelta di prodotto, e va confermata.
