# 0026 — Soglie di scorta per articolo e deposito

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 05 — Scorte minime e riordino
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0008`, `0009`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa che tiene merce
> voglio dire, per gli articoli che mi interessano, sotto quale quantità voglio essere avvisato e quanto ne
> ricompro di solito
> così da smettere di accorgermi che un articolo è finito solo quando un cliente me lo chiede.

**Contesto.** Fino a qui l'applicazione sa dire **quanto** c'è e **perché** c'è: il registro dei movimenti e la
giacenza derivata sono in piedi (`0013`-`0020`) e l'inventario fisico permette di verificarli (`0021`-`0025`).
Manca il passaggio dal passato al futuro: nessuno ha ancora detto all'applicazione quale quantità considera
troppo bassa. Questa storia mette a terra il solo dato che serve — la **regola di scorta** per coppia articolo e
deposito — e non fa nulla con esso: gli avvisi sono della storia `0027`, la lista della spesa della `0028`.
È il momento giusto perché la regola ha bisogno di articoli (`0006`), depositi (`0008`) e fornitori (`0009`)
esistenti, e perché tenerla separata dal calcolo dell'avviso evita di scrivere due volte la stessa soglia.

Due scelte nascono dall'analisi in rete e non dall'immaginazione. La prima: la soglia è **per articolo e
deposito**, non per articolo soltanto, perché lo stesso ricambio ha senso averne cinque in magazzino e uno in
furgone (descrizione dell'applicazione, §2.5). La seconda: una soglia **assente significa «non sorvegliato»**,
non zero — chi non ha mai impostato una soglia non vuole essere avvisato, e un magazzino con quattrocento
articoli tutti a zero produrrebbe quattrocento avvisi il primo giorno, cioè il rumore che le fonti sul segmento
indicano come la ragione per cui questi programmi vengono spenti (§2.5, §2.6 fonte 10).

## 2. Requisiti funzionali

1. **RF-1** — Per ogni coppia articolo e deposito si può registrare **una sola** regola di scorta, con tre
   quantità: `scorta_minima` (sotto questa si avvisa), `scorta_sicurezza` (il cuscinetto che si vuole avere
   comunque) e `quantita_riordino` (il livello fino al quale si ricompra). Le tre quantità sono facoltative una
   per una.
2. **RF-2** — Una coppia articolo e deposito **senza** regola è «non sorvegliata»: non compare in nessun elenco di
   sotto scorta e non genera proposte. L'assenza non equivale a una soglia pari a zero, e l'interfaccia lo dice
   con parole, non con un campo vuoto.
3. **RF-3** — La regola può indicare un **fornitore preferito**, scelto fra i fornitori dell'account (`0009`); il
   fornitore è facoltativo e la sua assenza non impedisce la regola.
4. **RF-4** — Le tre quantità sono coerenti fra loro: `scorta_minima` ≤ `quantita_riordino`, tutte non negative,
   con l'unità di misura dell'articolo. Una regola incoerente è respinta con un messaggio che dice quale vincolo
   ha violato.
5. **RF-5** — Le regole si possono impostare **in massa** su una selezione di articoli o su una intera categoria,
   per un deposito indicato: l'operazione mostra prima quante regole verrebbero create e quante sovrascritte, e si
   applica solo dopo conferma.
6. **RF-6** — L'elenco delle regole è consultabile e filtrabile per deposito, categoria e presenza del fornitore
   preferito; una regola si modifica e si rimuove, e rimuoverla riporta la coppia allo stato «non sorvegliata».
7. **RF-7** — Le regole si esportano e si importano da file con lo stesso formato dell'anagrafica (`0011`), perché
   quattrocento soglie non si impostano una per una da una schermata.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `regola_scorta` filtra per `tenant_id`
  preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene
  ignorato. L'articolo, il deposito e il fornitore indicati devono appartenere allo stesso account, altrimenti la
  richiesta è respinta con `404` e non con un errore che riveli l'esistenza altrove. Prova di isolamento fra due
  account sulla risorsa.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/magazzino/v1/regole-scorta`,
  `GET|PATCH|DELETE /api/magazzino/v1/regole-scorta/{id}` e `POST /api/magazzino/v1/regole-scorta/in-massa`;
  oggetti di trasferimento al bordo (le entità non si espongono mai); validazione dichiarativa sui vincoli di
  RF-4; errori in `application/problem+json`; paginazione a pagina e dimensione con totale; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V16__regola_scorta.sql` sullo schema `app_magazzino`: tabella
  `regola_scorta` con `tenant_id`, chiave primaria UUID versione 7, `articolo_id`, `deposito_id`,
  `fornitore_id` facoltativo, le tre quantità come numeri esatti (mai in virgola mobile), colonne di controllo e
  `deleted_at`. Vincolo di unicità su `(tenant_id, articolo_id, deposito_id)` fra le righe non cancellate.
  Nessuna chiave esterna verso altri schemi: i riferimenti restano logici.
- **RT-4 — Modulo frontend (§3, §5).** Le regole si governano da due punti della sezione `riordino` del modulo
  `magazzino`: un elenco filtrabile e un pannello di impostazione in massa; la scheda dell'articolo mostra la
  regola del deposito corrente in sola lettura, con un collegamento alla modifica. Dati letti con il client
  generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `magazzino` e sono presenti
  in `en, it, fr, es, de`, compreso il testo che distingue «non sorvegliato» da «soglia pari a zero»: è la
  distinzione che la storia esiste per rendere evidente, e una traduzione approssimativa la cancella.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: la metrica `articoli_gestiti` (natura `stock`) è
  legata al numero di articoli attivi e la tocca solo la loro creazione (`0004`, `0006`). Impostare, modificare o
  rimuovere una regola di scorta **non viene mai respinto con `429`**, nemmeno a tetto raggiunto. Restano attivi i
  varchi precedenti: `402` con abbonamento non attivo, `403` per ruolo insufficiente — l'impostazione in massa è
  riservata ai ruoli `owner` e `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato in questa storia: la lettura delle
  soglie arriva insieme a `elenca_sotto_scorta` nella storia `0034`, e la modifica di una soglia a voce non è
  prevista nel contratto della descrizione (§7). Il contratto vive dentro il servizio; il server conversazionale è
  di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. La regola porta un riferimento al **fornitore**,
  la cui anagrafica è già dichiarata nel manifesto `docs/compliance/manifests/magazzino.yaml` dalla storia `0010`;
  questa storia aggiunge la tabella `regola_scorta` alle tabelle trattate da `exportData` e `purgeData` del
  contratto `MagazzinoDataContract`, perché contiene quel riferimento. Nessun campo di testo libero introdotto.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `regola di scorta creata`, `regola di scorta modificata`,
  `regola di scorta rimossa` e `impostazione in massa applicata` (con il numero di righe create e sovrascritte)
  sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** la ragione
  sociale del fornitore e senza la descrizione dell'articolo.

## 4. Criteri di accettazione

**CA-1 — Impostazione di una soglia**
- **Dato** un utente autenticato di un account abilitato, con l'articolo `RIC-014` e il deposito `MAG`
- **Quando** registra una regola con scorta minima 5, scorta di sicurezza 2 e quantità di riordino 20
- **Allora** la regola esiste, è l'unica per quella coppia, e la scheda dell'articolo mostra la soglia accanto alla
  giacenza del deposito `MAG`

**CA-2 — Assente non è zero**
- **Dato** un articolo con giacenza 0 in un deposito e **nessuna** regola di scorta per quella coppia
- **Quando** si chiede l'elenco delle coppie sorvegliate
- **Allora** quella coppia non compare, ed è etichettata «non sorvegliata» nella scheda dell'articolo — non
  «sotto scorta», non «soglia 0»

**CA-3 — Soglie incoerenti**
- **Dato** un utente che imposta una regola con scorta minima 30 e quantità di riordino 10
- **Quando** invia la richiesta
- **Allora** riceve `422` in `application/problem+json` con l'indicazione che la quantità di riordino non può
  essere inferiore alla scorta minima, e nessuna regola viene creata

**CA-4 — Impostazione in massa con anteprima**
- **Dato** una categoria con 40 articoli, di cui 12 hanno già una regola sul deposito `MAG`
- **Quando** si imposta in massa scorta minima 3 su quella categoria e su quel deposito
- **Allora** l'anteprima annuncia 28 regole create e 12 sovrascritte, e solo dopo la conferma le 40 regole
  risultano impostate; senza conferma nulla cambia

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri articoli, depositi e regole di scorta
- **Quando** un utente di `A` chiede l'elenco delle regole, o tenta di crearne una indicando l'identificativo di un
  articolo di `B`
- **Allora** nel primo caso vede solo le proprie, anche forzando l'identificativo dell'account nel corpo o nei
  parametri; nel secondo riceve `404` e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del
      commit);
- [ ] prove di **unità** sui vincoli di coerenza fra le tre quantità e sul calcolo dell'anteprima
      dell'impostazione in massa, e di **integrazione** sulla risorsa `regole-scorta`, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** su `regola_scorta`, compreso il tentativo di legare la regola a un
      articolo di un altro account;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` sulle scorte è di proprietà della storia
      `0028`, che porta l'intera catena soglia → avviso → proposta; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), con la distinzione fra «non
      sorvegliato» e «soglia pari a zero» rivista lingua per lingua;
- [ ] **manifesto dei dati** invariato quanto ai campi, con `regola_scorta` aggiunta a `exportData` e `purgeData`
      per il riferimento al fornitore;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la scelta «soglia assente =
      non sorvegliato» e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta in questa storia, la lettura arriva
      con la `0034`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` — anagrafica degli articoli | la regola si riferisce a un articolo attivo e ne eredita l'unità di misura |
| `0008` — depositi e ubicazioni | la soglia è per coppia articolo e deposito, non per articolo soltanto |
| `0009` — anagrafica dei fornitori | il fornitore preferito si sceglie fra quelli dell'account |
| `0013` — registro dei movimenti e giacenza derivata | la soglia ha senso solo confrontata con una giacenza che dice il vero |
| `0011` — importazione dell'anagrafica da file | riusa lo stesso formato e la stessa anteprima per l'importazione delle regole (RF-7) |

## 7. Fuori ambito

- **Il calcolo di chi è sotto soglia e l'avviso**: sono della storia `0027`. Qui si registra il dato, non lo si
  interpreta.
- **La lista della spesa e il raggruppamento per fornitore**: storia `0028`.
- **Il suggerimento automatico della soglia a partire dal consumo** e la segnalazione delle soglie diventate
  sbagliate: storia `0029`. Qui la soglia la scrive una persona.
- **Il tempo di consegna del fornitore** come componente della scorta di sicurezza: non è nel modello di dominio
  della descrizione (§4) e aggiungerlo ora significherebbe promettere un calcolo di riapprovvigionamento che
  l'applicazione non fa.

## 8. Punti aperti

- **Una soglia sul totale fra tutti i depositi** — un cliente con magazzino e furgone potrebbe volere «cinque in
  tutto», non «cinque per deposito». È una scelta di prodotto: la proposta è di **non** offrirla ora, perché il
  totale nasconde proprio l'informazione che serve (dove manca), ma va confermata dallo sviluppatore.
- **Che cosa succede alla regola quando l'articolo viene archiviato**: la proposta è che la regola sopravviva
  inerte e torni attiva se l'articolo viene riattivato, senza comparire in nessun avviso nel frattempo. Da
  confermare insieme al comportamento dell'archiviazione (`0006`).
