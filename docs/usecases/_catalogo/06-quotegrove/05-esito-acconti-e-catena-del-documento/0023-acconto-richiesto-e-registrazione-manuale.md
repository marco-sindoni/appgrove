# 0023 — Acconto richiesto e registrazione manuale

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 05 — Esito, acconti e catena del documento
**Storia**: `0023` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come artigiano che non comincia un lavoro senza acconto
> voglio che l'offerta dica chiaramente quanto va versato prima di partire, e poter segnare quando l'ho ricevuto
> così da avere una condizione scritta invece di una frase detta al telefono, e da sapere cosa manca.

**Contesto.** «Acconti e depositi» è nei casi d'uso principali della scheda di catalogo, e i clienti se lo
aspettano (§2.4 della descrizione dell'applicazione). Ma incassare davvero significherebbe far transitare **denaro
di terzi** — il cliente del nostro cliente — dentro appgrove, con tutto ciò che ne consegue in materia di servizi
di pagamento e nel rapporto con il venditore di riferimento usato oggi solo per gli abbonamenti. Questa storia
consegna il **90 % del valore con lo 0 % del rischio**: l'acconto si scrive nel documento, si mostra al cliente e
si segna incassato a mano.

## 2. Requisiti funzionali

1. **RF-1** — Sul preventivo si dichiara un acconto in percentuale del totale o in importo fisso, con una
   scadenza («entro 7 giorni dall'accettazione») e le istruzioni di pagamento prese dal modello.
2. **RF-2** — L'acconto compare sul documento e sulla pagina pubblica come **condizione dell'offerta**, non come
   pulsante di pagamento.
3. **RF-3** — Dopo l'accettazione, chi vende segna a mano l'acconto come ricevuto, con data e nota facoltativa.
4. **RF-4** — L'elenco dei preventivi accettati mostra quali attendono ancora l'acconto.
5. **RF-5** — L'applicazione **non incassa e non chiede dati di pagamento a nessuno**: la pagina pubblica non ha
   moduli di carta e non rimanda a un fornitore di pagamento.
6. **RF-6** — Lo stato dell'acconto viaggia nell'evento verso le app a valle (storia `0025`), così che la
   fatturazione sappia se c'è un anticipo da considerare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** `richiesta_acconto` filtra per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `/api/preventivi/v1/preventivi/{id}/acconto`; errori in
  `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V14__acconto.sql`: tabella `richiesta_acconto` con `tenant_id`, UUID
  versione 7, colonne di controllo, cancellazione logica; importi in unità minime intere.
- **RT-4 — Modulo frontend (§3, §5).** Campo nell'editor del preventivo, riquadro sul documento, indicatore nello
  stato del preventivo accettato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili in `en, it, fr, es, de`; le istruzioni di pagamento
  sono contenuto del cliente, per lingua.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo. **Le istruzioni di pagamento del cliente possono
  contenere un codice di conto corrente intestato a una persona fisica**: è testo del cliente e va trattato come
  gli altri testi liberi del modello (storia `0014`).
- **RT-7 — Registrazione eventi (§14).** `acconto dichiarato`, `acconto segnato ricevuto` con `tenant_id`,
  `app_id`, `user_id` e correlazione, senza importi identificativi di persone.

## 4. Criteri di accettazione

**CA-1 — Acconto sul documento**
- **Dato** un preventivo da 4 000 € con acconto del 30 % · **Quando** si genera il documento · **Allora** vi si
  legge «acconto richiesto 1 200 €, entro 7 giorni dall'accettazione» e le istruzioni di pagamento

**CA-2 — Registrazione manuale**
- **Dato** un preventivo accettato con acconto atteso · **Quando** chi vende lo segna ricevuto · **Allora** lo
  stato cambia, la data resta scritta e il documento non attende più

**CA-3 — Elenco di chi non ha ancora versato**
- **Dato** tre preventivi accettati, uno con acconto ricevuto · **Quando** si filtra «in attesa di acconto»
- **Allora** compaiono gli altri due

**CA-4 — Nessun incasso**
- **Dato** la pagina pubblica di un preventivo con acconto · **Quando** la si esamina · **Allora** non contiene
  moduli di pagamento, non chiede dati di carta e non rimanda a nessun fornitore di pagamento

**CA-5 — Isolamento fra account**
- **Dato** due account con preventivi in attesa di acconto · **Quando** un utente di `A` guarda l'elenco
- **Allora** vede solo i propri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sul calcolo dell'acconto in percentuale e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: rimando alla storia `0030`, dove il documento con acconto è quello che il destinatario
      legge; il resto è coperto da integrazione — motivo scritto nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la nota sulle istruzioni di pagamento;
- [ ] **registro delle decisioni** compilato: **la scelta di non incassare, con la motivazione**;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | l'acconto si attende dopo l'accettazione |

## 7. Fuori ambito

- **l'incasso vero dell'acconto**: escluso per decisione dichiarata, vedi punti aperti;
- la riconciliazione con l'estratto conto bancario: è materia della fatturazione e dell'incasso crediti
  (catalogo 02 e 03).

## 8. Punti aperti

🛑 **Fermata di escalation — incassare l'acconto.** Farlo davvero significa muovere denaro di terzi e cambia la
natura del servizio: non è una funzione da aggiungere, è una decisione di direzione di prodotto e di conformità
che spetta allo sviluppatore. Se un giorno si deciderà di farlo, andranno riconsiderati il rapporto con il
fornitore di pagamento, gli obblighi che ne discendono e il modello dei costi (Qwilr, per esempio, ci applica una
commissione propria oltre a quella del fornitore, §2.1 della descrizione dell'applicazione).
