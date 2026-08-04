# 0022 — Da mostrare a addebitare

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 04 — Attribuzione della spesa
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che gira il costo dei modelli sui propri clienti o sulle proprie squadre
> voglio produrre un prospetto per periodo, per ciascuno, con il metodo di calcolo scritto sopra
> così da poterlo mandare senza discussioni, e da poter rispondere alla domanda «come hai fatto questo numero?».

**Contesto.** È il punto in cui i numeri escono dall'app e diventano soldi che qualcuno chiede a qualcun altro:
una nota di addebito a un cliente finale, un ribaltamento interno su una squadra. La distinzione riconosciuta è
netta: **mostrare** è informazione senza conseguenze, **addebitare** è un costo vero che si sposta; e la pratica
prevede di passare dal primo al secondo solo quando l'attribuzione copre la maggior parte della spesa, perché
addebitare su un'attribuzione incompleta significa far pagare a qualcuno il non attribuito di qualcun altro (§2.6,
fonte 14). Questa storia mette quella regola dentro il prodotto invece di lasciarla al buon senso.

## 2. Requisiti funzionali

1. **RF-1** — Un prospetto di ribaltamento si produce per un periodo chiuso e per un asse (per esempio «cliente»),
   e contiene: valore, importo, numero di chiamate, quota del totale, copertura del periodo, e il metodo con cui
   il non attribuito è stato trattato.
2. **RF-2** — Il trattamento del non attribuito è una **scelta dichiarata**, non un comportamento implicito: si
   può escludere (ciascuno paga solo il proprio), oppure ripartire in proporzione dichiarandolo nel prospetto. Non
   esiste una terza via silenziosa.
3. **RF-3** — Se la copertura del periodo è sotto la soglia dell'account, il prospetto **si può comunque produrre**
   ma porta un'avvertenza in evidenza: sotto quella copertura i numeri non reggono un addebito.
4. **RF-4** — Il prospetto si produce solo su periodi **chiusi**: un periodo ancora aperto può ricevere misure in
   ritardo (storia `0010`) e cambierebbe dopo l'invio.
5. **RF-5** — Ogni prospetto prodotto resta nell'app con la data, il periodo, il metodo e i numeri con cui è stato
   generato: se dopo un ricalcolo (storia `0017`) i numeri cambiano, resta la prova di cosa era stato mandato.
6. **RF-6** — Il prospetto è esportabile in formato tabellare e in formato leggibile; **non** produce documenti
   commerciali: TokenGrove non emette fatture né note di credito, e il testo lo dice.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni prospetto è generato e letto nel solo `tenant_id` preso dal gettone
  verificato.
- **RT-2 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `prospetto` con `tenant_id`,
  periodo, asse, metodo, righe congelate, copertura al momento della generazione, chiave primaria UUID versione 7,
  colonne di controllo e cancellazione logica. Le righe del prospetto sono **congelate** come i costi: un prospetto
  è una fotografia, non una interrogazione salvata.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `POST /api/spesa_modelli/v1/prospetti`,
  `GET /api/spesa_modelli/v1/prospetti`, `GET .../{id}/esportazione`; errori in `problem+json` che distinguono
  «periodo non chiuso» da «copertura insufficiente» (che è un'avvertenza, non un errore); definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-4 — Varchi e ruoli (§6).** Produrre un prospetto è riservato a `owner` e `admin`: è un'informazione
  commerciale complessiva dell'azienda.
- **RT-5 — Modulo frontend (§3, §5).** Sezione «Attribuzione», scheda «Ribaltamento»: scelta del periodo, scelta
  del metodo con la spiegazione di ciascuno, anteprima, generazione. Solo token del sistema di design; tema chiaro
  e scuro.
- **RT-6 — Cinque lingue (§4).** Tutte le stringhe presenti in `en, it, fr, es, de`, comprese le due spiegazioni
  dei metodi di trattamento del non attribuito.
- **RT-7 — Esposizione conversazionale (§12).** La **lettura** dei prospetti esistenti è compresa in
  `leggi_spesa`; la **generazione** di un prospetto è marcata **scrittura con conferma** (storia `0033`), perché
  produce un documento destinato a uscire dall'azienda.
- **RT-8 — Dati personali (§10).** Il prospetto contiene i valori dell'asse, che possono essere ragioni sociali di
  ditte individuali: la tabella `prospetto` entra in `exportData` e `purgeData` e la voce è già coperta dal
  manifesto (storia `0019`).
- **RT-9 — Registrazione eventi (§14).** Evento «prospetto generato» con `tenant_id`, `app_id`, `user_id`,
  periodo, asse, metodo e copertura — **senza** gli importi né i valori dell'asse.

## 4. Criteri di accettazione

**CA-1 — Prospetto con copertura alta**
- **Dato** un mese chiuso con copertura del 92% sull'asse «cliente»
- **Quando** si genera il prospetto escludendo il non attribuito
- **Allora** ogni cliente ha il proprio importo, la somma corrisponde alla spesa attribuita, e il documento
  dichiara metodo e copertura

**CA-2 — Copertura bassa: avvertenza, non blocco**
- **Dato** un mese chiuso con copertura del 41%
- **Quando** si genera il prospetto
- **Allora** il prospetto viene prodotto ma porta in evidenza l'avvertenza che sotto la soglia dell'account i
  numeri non reggono un addebito

**CA-3 — Periodo non chiuso**
- **Dato** il mese corrente, ancora in corso
- **Quando** si tenta di generare il prospetto
- **Allora** l'operazione è rifiutata con la spiegazione che il periodo può ancora ricevere misure in ritardo

**CA-4 — Il prospetto resta com'era**
- **Dato** un prospetto generato e poi un ricalcolo dei costi sullo stesso periodo
- **Quando** si riapre il prospetto
- **Allora** mostra i numeri con cui era stato generato, e segnala che nel frattempo i costi del periodo sono
  cambiati

**CA-5 — Isolamento fra account**
- **Dato** due account con prospetti sullo stesso periodo
- **Quando** uno li elenca
- **Allora** vede solo i propri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sui due metodi di trattamento del non attribuito e sulla chiusura del periodo, e di
      **integrazione** sulla generazione del prospetto;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sui prospetti;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «chiudo il mese, genero
      il prospetto per cliente, il totale corrisponde», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: `prospetto` in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sull'avvertenza invece del blocco a copertura bassa e
      sul divieto di produrre documenti commerciali;
- [ ] contratto degli **strumenti conversazionali** dichiarato per la generazione del prospetto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0021` | Serve la copertura, che è la condizione di sensatezza del ribaltamento |

## 7. Fuori ambito

- l'emissione di documenti commerciali (fatture, note di addebito): **non si fa**, ed è un confine dichiarato con
  l'app 02 del catalogo (§10 del documento capofila);
- l'invio del prospetto al cliente finale per posta elettronica: rimandato; oggi si esporta e si manda con i propri
  strumenti. Se emergesse come bisogno reale, sarebbe una storia dell'epica 06;
- l'aggiunta di un ricarico percentuale sul costo ribaltato: rimandata, perché è una scelta commerciale del
  cliente che apre la questione di che cosa stiamo aiutando a fatturare.

## 8. Punti aperti

- **Se il piano intermedio debba comprendere il ribaltamento o se sia una funzionalità del piano alto.** La
  proposta di listino (§5 del documento capofila) lo mette nel piano alto, perché è la funzione di chi ha più
  clienti o più squadre; ma è anche una delle ragioni più forti per pagare, e metterla troppo in alto la rende
  invisibile. È una decisione di prezzo: la chiude lo sviluppatore.
