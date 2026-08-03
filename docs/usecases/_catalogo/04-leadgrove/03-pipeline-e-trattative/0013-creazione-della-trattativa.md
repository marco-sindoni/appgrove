# 0013 — Creazione della trattativa

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 03 — Pipeline e trattative
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore appena uscito da una telefonata promettente
> voglio registrare in mezzo minuto che c'è un possibile affare, con quanto vale e con chi
> così da non affidarlo alla memoria fino a sera.

**Contesto.** La trattativa è l'unità di misura del lavoro commerciale: senza di lei l'app è una rubrica. La
storia deve tenere insieme due esigenze in tensione — registrare deve costare pochissimo (altrimenti l'archivio
resta vuoto, §2.5 della descrizione dell'applicazione) ma la trattativa deve avere abbastanza dati da poter essere
misurata dopo (epica 06). Il compromesso proposto: due soli campi obbligatori, il resto facoltativo.

## 2. Requisiti funzionali

1. **RF-1** — Un utente con un posto crea una trattativa indicando titolo e azienda; valore, valuta, contatto,
   pipeline, fase e data attesa di chiusura sono facoltativi e precompilati con valori sensati (pipeline
   predefinita, prima fase, valuta dell'account).
2. **RF-2** — La trattativa nasce nello stato **aperta**, in una fase non terminale.
3. **RF-3** — Si può creare una trattativa direttamente dalla scheda di un'azienda o di un contatto, con i
   riferimenti già compilati.
4. **RF-4** — Il valore è un importo con valuta; senza valore la trattativa è valida ma viene segnalata nei
   rapporti come «senza valore», invece di essere contata come zero.
5. **RF-5** — La trattativa ha un responsabile, impostato di norma su chi la crea.
6. **RF-6** — Si può modificare titolo, valore, data attesa, contatto e responsabile; il **cambio di fase** ha una
   sua storia dedicata (0014, 0015) perché scrive lo storico.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `Deal` filtra per `tenant_id` dal token
  verificato; azienda, contatto, pipeline e fase indicati devono appartenere allo stesso account, altrimenti la
  richiesta è rifiutata con `404` sul riferimento.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST|GET|PATCH|DELETE /api/sales/v1/deals[/{id}]`; corpo
  validato (importi non negativi, valuta a tre lettere, data attesa non nel passato remoto); errori in
  `application/problem+json`; paginazione con totale; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `deal` già creata dalla storia 0002; qui gli indici per fase, responsabile e
  azienda, tutti a partire da `tenant_id`. Gli importi si conservano in **unità minime intere** con la valuta
  accanto, mai in virgola mobile.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Trattative: elenco, scheda e modulo di inserimento; azione «nuova
  trattativa» anche dalle schede di azienda e contatto; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili in `en, it, fr, es, de`; gli importi e le date si
  formattano secondo la lingua scelta.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo della metrica `seats`: il numero di trattative **non** è
  limitato, di proposito (limitarlo spingerebbe a non registrarle, §3 della descrizione). Chi non ha un posto
  riceve `403`; con abbonamento non attivo `402`.
- **RT-7 — Esposizione conversazionale (§12).** `list_deals` in lettura (storia 0034); la creazione entra in
  `create_lead` (storia 0035) come **bozza con conferma**.
- **RT-8 — Dati personali (§10).** Il campo `deal.loss_reason` è già dichiarato nel manifesto come testo libero ma
  si valorizza nella storia 0016; qui la trattativa contiene solo riferimenti a contatto e azienda. Nessuna voce
  nuova; la tabella `deal` resta in `exportData` e `purgeData` perché rimanda a persone.
- **RT-9 — Registrazione eventi (§14).** «Trattativa creata», «trattativa modificata» con identificativi, valore e
  valuta; **mai** il titolo, che è testo libero scritto da una persona.

## 4. Criteri di accettazione

**CA-1 — Creazione minima**
- **Dato** un venditore con un posto e un'azienda esistente
- **Quando** crea una trattativa indicando solo titolo e azienda
- **Allora** la trattativa esiste, è aperta, sta nella prima fase della pipeline predefinita e il responsabile è
  il venditore stesso

**CA-2 — Riferimento di un altro account**
- **Dato** un venditore dell'account `A`
- **Quando** crea una trattativa indicando un'azienda dell'account `B`
- **Allora** riceve `404` sul riferimento e nulla viene creato

**CA-3 — Valore non valido**
- **Dato** lo stesso venditore
- **Quando** indica un valore negativo
- **Allora** riceve `400` in `application/problem+json` con l'indicazione del campo

**CA-4 — Trattativa senza valore**
- **Dato** una trattativa creata senza valore
- **Quando** si apre l'elenco
- **Allora** compare con l'indicazione «senza valore», e non come «0,00 €»

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con le proprie trattative
- **Quando** un utente di `A` chiede l'elenco forzando l'identificativo di `B`
- **Allora** vede solo le proprie

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione degli importi e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** sulla risorsa `deals`, compresi i riferimenti incrociati;
- [ ] **prova end-to-end**: coprire ora — la creazione della trattativa è il terzo passo del percorso `[J-SALES]`
      (storia 0037); voce aggiunta al registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con formattazione di importi e date per lingua;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che `deal` resti in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché le trattative non consumano quota;
- [ ] contratto degli **strumenti conversazionali**: rimando alle storie 0034 e 0035;
- [ ] controllo automatico di **accessibilità** verde su elenco e modulo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | La trattativa si aggancia a un'azienda |
| Storia `0007` | Il contatto di riferimento |
| Storia `0012` | Servono pipeline e fasi in cui collocarla |

## 7. Fuori ambito

- il cambio di fase e la lavagna: storie 0014 e 0015;
- la chiusura vinta/persa: storia 0016;
- il valore atteso: storia 0017;
- i prodotti o le righe dentro la trattativa: fuori perimetro, è materia dell'app 06 (preventivi).

## 8. Punti aperti

- **Valuta multipla** — la proposta è una valuta per trattativa, con quella dell'account come valore predefinito.
  Se in futuro servisse sommare trattative in valute diverse nei rapporti, serve un tasso di conversione e una
  data di riferimento: è una decisione di prodotto, non di questa storia.
