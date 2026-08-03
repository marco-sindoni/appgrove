# 0014 — Carico della merce

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 03 — Registro dei movimenti e giacenze
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che riceve la merce al banco di carico
> voglio registrare quello che è arrivato, dove l'ho messo e da quale documento proviene
> così da far salire la giacenza nel momento in cui la merce entra davvero, e non la sera quando me ne ricordo.

**Contesto.** Il registro esiste (storia `0013`) ma non ha ancora un modo comodo di scrivere un'entrata. Il carico è
il primo movimento che un cliente registra in assoluto — prima di poter scaricare qualcosa bisogna averlo caricato —
ed è anche il momento in cui l'app impara **quanto costa** la merce: il costo medio ponderato mobile si aggiorna
qui, a ogni entrata, ed è il dato su cui poggia il valore gestionale delle giacenze (storia `0025`).

**Un'avvertenza che vale per tutta la storia.** Il costo che si calcola qui è un costo **gestionale**: serve a
rispondere a «quanto capitale ho fermo sugli scaffali?», non a compilare un rigo di dichiarazione. La valutazione
delle rimanenze ai fini del bilancio — scelta del metodo, svalutazioni, raccordo con la contabilità — è materia del
commercialista e resta fuori dall'applicazione (descrizione dell'applicazione, §2.3 punto 2). Il confine si scrive
in interfaccia, non solo nella documentazione, ed è di proprietà della storia `0025`.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la registrazione di un carico: articolo, deposito, ubicazione facoltativa, quantità
   **strettamente positiva**, motivo fra quelli ammessi con segno positivo, riferimento al documento d'origine
   facoltativo (numero della bolla, dell'ordine o della fattura d'acquisto), nota facoltativa.
2. **RF-2** — Il carico produce **un** movimento di tipo `carico` con quantità positiva e aggiorna la riga di
   giacenza della coppia articolo-deposito nella stessa transazione (storia `0013`, RF-4).
3. **RF-3** — Il carico accetta facoltativamente il **costo unitario** della merce entrata; quando è presente,
   aggiorna il costo medio ponderato mobile dell'articolo secondo la formula
   `nuovo_costo_medio = (giacenza_totale_precedente × costo_medio_precedente + quantità_entrata × costo_unitario) ÷
   (giacenza_totale_precedente + quantità_entrata)`, calcolata sulla giacenza **totale dell'account**, non del
   singolo deposito.
4. **RF-4** — Quando il costo unitario non è indicato, il costo medio dell'articolo **resta quello di prima** e non
   viene diluito a zero; il movimento riporta che il costo non era noto.
5. **RF-5** — Gli importi di costo sono conservati in **centesimi interi** con la valuta accanto, mai in virgola
   mobile; la valuta è quella dell'account e un costo in valuta diversa è respinto con `422`.
6. **RF-6** — La schermata di carico consente di ripetere l'operazione su un altro articolo senza tornare indietro,
   mantenendo deposito e motivo già scelti: chi scarica un bancale registra venti righe di seguito, non una.
7. **RF-7** — Il costo medio dell'articolo è leggibile e ovunque compaia porta l'etichetta **valore gestionale**,
   mai la parola «rimanenze».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `movimento`, `giacenza` e `costo_articolo`
  filtra per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai
  parametri viene ignorato. Prova di isolamento fra due account sulla risorsa del carico e sul costo.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/magazzino/v1/movimenti` con tipo `carico`
  (il corpo dichiara il tipo, la validazione impone quantità positiva e motivo di segno compatibile);
  `GET /api/magazzino/v1/articoli/{id}/costo` per la lettura del costo medio; oggetti di trasferimento al bordo;
  errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V9__costo_articolo.sql` sullo schema `app_magazzino`: tabella
  `costo_articolo` con `tenant_id`, `articolo_id` (unico per account), `costo_medio_centesimi`, `valuta`,
  `aggiornato_il`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. L'aggiornamento del
  costo avviene nella **stessa transazione** del movimento e con lo stesso aggiornamento condizionato, mai
  leggendo e riscrivendo in memoria.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Carico» del modulo `magazzino`; i dati sono letti e scritti
  con il client generato dalla definizione OpenAPI; solo token del sistema di design con colore-categoria `amber`;
  funziona in tema chiaro e scuro; il modulo non accede al token se non tramite il contesto della shell.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `magazzino` e sono presenti in
  `en, it, fr, es, de`, compresa l'etichetta «valore gestionale» — che va tradotta con cura, perché una traduzione
  infelice ricrea la promessa fiscale che stiamo evitando (descrizione dell'applicazione, §11 punto 4).
- **RT-6 — Varchi e quota (§6, §7).** **Nessun consumo di quota**: registrare un carico **non consuma quota e non
  viene mai respinto con `429`**, nemmeno quando l'account ha raggiunto il tetto di `articoli_gestiti`; la metrica
  (natura `stock`) è consumata solo dalla creazione di articoli. Con abbonamento `canceled` la rotta risponde `402`;
  in `past_due` resta accessibile.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `registra_carico(articolo, deposito, quantità, motivo?,
  riferimento?) → bozza di movimento` è dichiarato qui come contratto e implementato nella storia `0035`: è di
  **scrittura** e produce una bozza con conferma umana esplicita. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** L'autore del carico (`created_by`) è già
  dichiarato nel manifesto dalla storia `0010`; il costo dell'articolo è un dato sulla merce, non su una persona. Il
  riferimento al documento d'origine è un numero di documento, non un'anagrafica: il fornitore resta quello della
  storia `0009`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `carico registrato` e `costo medio aggiornato` sono registrati
  con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** importi e **senza** note.

## 4. Criteri di accettazione

**CA-1 — Carico che fa salire la giacenza**
- **Dato** un articolo con giacenza `0` nel deposito «Magazzino»
- **Quando** un addetto registra un carico di 24 pezzi con motivo `acquisto` e riferimento «bolla 118»
- **Allora** la giacenza della coppia diventa `24`, esiste un movimento di tipo `carico` con quantità `+24`, motivo
  e riferimento salvati, e l'autore è l'utente che ha registrato

**CA-2 — Costo medio ponderato mobile**
- **Dato** un articolo con 10 pezzi a costo medio 5,00 €
- **Quando** si registra un carico di 10 pezzi a costo unitario 7,00 €
- **Allora** il costo medio dell'articolo diventa 6,00 € (60000 centesimi su 100 pezzi → 600 centesimi l'uno) e la
  schermata lo mostra etichettato come **valore gestionale**

**CA-3 — Carico senza costo indicato**
- **Dato** un articolo con costo medio 6,00 €
- **Quando** si registra un carico di 5 pezzi senza indicare il costo unitario
- **Allora** la giacenza sale di 5, il costo medio resta 6,00 € e il movimento riporta che il costo non era noto

**CA-4 — Quantità non positiva**
- **Dato** un utente autenticato di un account abilitato
- **Quando** tenta un carico con quantità `0` o negativa
- **Allora** la risposta è `422` in `application/problem+json` con l'indicazione che un carico richiede una quantità
  positiva, e nulla viene scritto

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con lo stesso codice articolo e giacenze diverse
- **Quando** un utente di `A` registra un carico
- **Allora** cambia solo la giacenza e il costo medio dell'account `A`, anche se forza l'identificativo dell'account
  `B` nel corpo della richiesta

**CA-6 — Quota raggiunta ma movimento consentito**
- **Dato** un account che ha raggiunto il tetto di `articoli_gestiti` del proprio piano
- **Quando** registra un carico su un articolo che esiste già
- **Allora** il carico va a buon fine: la quota non blocca i movimenti e non viene restituito `429`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del costo medio ponderato mobile (compresi i casi «primo carico», «costo
      assente» e arrotondamento in centesimi) e di **integrazione** sulla rotta del carico, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** su carico e costo dell'articolo;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` che parte dall'articolo vuoto e arriva al saldo
      corretto è di proprietà della storia `0036`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), con revisione dell'etichetta
      «valore gestionale»;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la formula del costo medio, la scelta della giacenza totale come
      base del calcolo e il comportamento a costo assente;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `registra_carico` (scrittura, con conferma);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | Registro, giacenza, motivi e idempotenza devono esistere |
| `0009` | Il riferimento al fornitore preferito compare nella schermata di carico come suggerimento |

## 7. Fuori ambito

- Lo scarico e la concorrenza fra due uscite simultanee: storia `0015`.
- Il carico che nasce da un reso o dall'ordine ricevuto di ProcureGrove (48): oggi si registra a mano; l'ingresso
  automatico è della storia `0019` per le app della suite già esistenti.
- Il valore complessivo del magazzino e la sua esportazione per il commercialista: storia `0025`.
- La lettura del codice a barre per aprire il carico già compilato: storia `0031`.

## 8. Punti aperti

- **Costo medio per account o per deposito**: la proposta è per account, perché il costo è una proprietà della
  merce e non del luogo, e perché un trasferimento non deve cambiare il costo. Se un cliente comprasse la stessa
  merce a condizioni molto diverse in due sedi, il numero unico perderebbe di significato: lo chiude lo sviluppatore.
- **Valuta diversa da quella dell'account**: oggi respinta. Se un cliente comprasse abitualmente in valuta estera
  servirebbe decidere a che cambio e di che giorno convertire, e non è una decisione di questa storia.
