# 0021 — Vendita di prodotti al banco

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 05 — Conto, pacchetti e fedeltà
**Storia**: `0021` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi sta alla cassa
> voglio aggiungere al conto lo shampoo che la cliente si porta a casa, e che la giacenza della rivendita scenda
> da sola
> così da non tenere due conti separati e da sapere quanto vale davvero la rivendita nel mio fatturato.

**Contesto.** Le fonti di settore indicano la rivendita al **5-10 % del fatturato** di un salone medio e al
**12-20 % nei saloni alti, con margini del 45-55 %** (§2.5 della descrizione): è la parte più redditizia del giro
d'affari e la più trascurata dai gestionali generici. La funzione è semplice — una riga di conto che scarica il
deposito rivendita invece della cabina — ma è la sola in cui il salone vede *guadagno di prodotto* accanto a
*guadagno di servizio*.

## 2. Requisiti funzionali

1. **RF-1** — Al conto si aggiunge una riga di **prodotto** scegliendolo per nome o per codice, con quantità e
   prezzo di vendita precompilato dall'anagrafica e modificabile.
2. **RF-2** — Alla chiusura del conto la riga scarica il deposito **rivendita** (non la cabina), con un movimento
   che cita il conto.
3. **RF-3** — Si può aprire un conto di **sola vendita**, senza appuntamento: chi entra solo per comprare uno
   shampoo esiste.
4. **RF-4** — La riga di prodotto porta l'**operatore attribuito**, come quella di servizio: la rivendita è
   spesso la parte su cui si riconosce una provvigione diversa (storia `0024`).
5. **RF-5** — La giacenza insufficiente **non blocca** la vendita: avvisa e lascia andare la giacenza sotto zero,
   perché il prodotto è uscito davvero dal negozio (stessa regola della storia `0017`).
6. **RF-6** — Il totale della giornata distingue **servizi** e **prodotti**: sono due mestieri diversi e vanno
   letti separati.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Righe di prodotto, giacenze e totali filtrano per `tenant_id` dal token
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** La rotta delle righe di conto (storia `0019`) accetta righe di
  tipo prodotto; `POST /api/<app>/v1/conti` accetta un conto senza prenotazione; errori in `problem+json`;
  OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: `riga_conto` porta già il tipo e il riferimento. Il
  movimento è un `movimento_magazzino` immutabile (storia `0016`).
- **RT-4 — Atomicità (storia `0019`).** Lo scarico della rivendita è dentro la transazione di chiusura, insieme
  agli altri effetti.
- **RT-5 — Varchi e quota (§6, §7).** Funzione accesa dal piano; `402` a piano insufficiente.
- **RT-6 — Modulo frontend (§3, §5).** La ricerca del prodotto è per nome, marca o codice, pensata per essere
  usata con una mano mentre si passa il prodotto con l'altra. Solo token del sistema di design, tema chiaro e
  scuro.
- **RT-7 — Cinque lingue (§4).** Etichette, avvisi di giacenza, intestazioni del totale in `en, it, fr, es, de`.
- **RT-8 — Dati personali (§10).** Nessuna voce nuova: `conto.cliente` e `riga_conto.operatore` sono già
  dichiarate dalla storia `0019`. Il conto di sola vendita **può non avere cliente**, ed è il caso più
  rispettoso: chi compra uno shampoo non deve lasciare il proprio nome.
- **RT-9 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: `aggiungi_riga_conto` della storia `0019`
  copre già il caso.
- **RT-10 — Registrazione eventi (§14).** `prodotto venduto` con `tenant_id`, `app_id`, `user_id`, correlazione e
  quantità.

## 4. Criteri di accettazione

**CA-1 — Prodotto sul conto del servizio**
- **Dato** un conto aperto per un taglio
- **Quando** si aggiunge uno shampoo da 18 €
- **Allora** il totale è la somma, e alla chiusura la rivendita scende di un pezzo

**CA-2 — La cabina non si muove**
- **Dato** uno shampoo presente in entrambi i depositi
- **Quando** lo si vende
- **Allora** scende solo la **rivendita**, e la cabina resta com'era

**CA-3 — Conto di sola vendita, anche anonimo**
- **Dato** nessun appuntamento
- **Quando** si apre un conto, si aggiunge un prodotto e si chiude senza indicare un cliente
- **Allora** il conto si chiude correttamente e non c'è nessun dato di persona

**CA-4 — Giacenza insufficiente**
- **Dato** zero pezzi in rivendita
- **Quando** si vende un pezzo
- **Allora** la vendita riesce, la giacenza va a meno uno e l'avviso invita a caricare

**CA-5 — Totale distinto**
- **Dato** una giornata con 400 € di servizi e 120 € di prodotti
- **Quando** si apre il totale del giorno
- **Allora** le due cifre si leggono separate

**CA-6 — Isolamento fra account**
- **Dato** due account con lo stesso prodotto
- **Quando** un utente del primo vende
- **Allora** solo la propria giacenza scende

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sui totali distinti, di **integrazione** sullo scarico dal deposito corretto;
- [ ] prova di **isolamento fra account** su vendita e giacenze;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-SALONGROVE]` della storia `0030`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; è dichiarato che il conto di sola vendita può essere senza
      cliente, ed è la forma preferibile;
- [ ] **registro delle decisioni**: scarico dal deposito rivendita, conto senza cliente ammesso, giacenza negativa
      ammessa, totali distinti fra servizi e prodotti;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | la riga vive su un conto |
| storia `0016` | lo scarico è un movimento |

## 7. Fuori ambito

- lo scontrino: perimetro escluso (storia `0019`);
- la vendita in linea dei prodotti: sarebbe un negozio, ed è un'altra app;
- le promozioni sui prodotti («tre per due»): non in questa stesura.

## 8. Punti aperti

Nessuno.
