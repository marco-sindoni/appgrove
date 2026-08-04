# 0010 — Salute e ritardo delle fonti

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 02 — Arrivo dei dati dalle altre app
**Storia**: `0010` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che guarda il cruscotto il lunedì mattina
> voglio sapere se uno dei numeri è fermo a martedì scorso perché una fonte ha smesso di parlare
> così da non prendere per buono un fatturato che è quello di sei giorni fa.

**Contesto.** È il difetto che rende pericoloso un cruscotto: non mostrare un errore, ma mostrare un numero
vecchio come se fosse fresco. Nessuno se ne accorge, perché il numero è plausibile. Questa storia dà a ogni fonte
un **ritardo atteso** dichiarato — ogni quanto ci si aspetta che parli — e trasforma il silenzio in un fatto
visibile, prima che il numero venga letto. È il presidio che la regola 3 del §4.3 della
[descrizione](../application-description.md) richiede.

## 2. Requisiti funzionali

1. **RF-1** — Ogni fonte dichiara nel proprio contratto un **ritardo atteso** per ciascuna misura (per esempio:
   il fatturato si aggiorna almeno una volta al giorno lavorativo; il valore di magazzino almeno una volta al
   giorno).
2. **RF-2** — Il servizio calcola, per ogni fonte collegata di ogni account, lo scarto fra adesso e il momento
   dell'ultimo fatto ricevuto, e ne deriva uno stato: **in linea**, **in ritardo**, **silente**.
3. **RF-3** — La sezione Fonti mostra lo stato di ogni fonte con il momento dell'ultimo fatto, in forma
   leggibile («ultimo dato: ieri alle 06:15»).
4. **RF-4** — Esiste una risorsa di lettura che restituisce lo stato di tutte le fonti dell'account, con
   l'elenco degli indicatori che dipendono da ciascuna: è la risposta alla domanda «perché questo numero è
   fermo?».
5. **RF-5** — Quando una fonte è **silente**, ogni indicatore che ne dipende porta il contrassegno di
   incompletezza (storia 0016) e gli avvisi che ne dipendono non suonano (storia 0020).
6. **RF-6** — Il passaggio di una fonte allo stato **silente** genera una notifica all'account, una sola volta
   per episodio: non si insiste ogni giorno sullo stesso silenzio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Lo stato di salute è calcolato **per account**: la stessa fonte può
  essere in linea per un account e silente per un altro, perché dipende dai fatti di quell'account. Ogni lettura
  filtra per `tenant_id` preso dal gettone verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/insights/v1/fonti/stato`; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** La sezione Fonti mostra lo stato con un contrassegno visivo che **non
  usa il solo colore** per distinguere gli stati (requisito di accessibilità); solo token del sistema di design;
  tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Gli stati e i testi relativi esistono in `en, it, fr, es, de`, comprese le
  formule di tempo relativo.
- **RT-6 — Varchi (§6).** La lettura dello stato è accessibile a tutti i ruoli, `member` compreso: sapere che un
  numero è vecchio non è un'informazione riservata.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-14 — Registrazione eventi (§14).** «Fonte passata a silente», «fonte tornata in linea» con `tenant_id`,
  `app_id` d'origine e identificativo di correlazione.
- **RT-11 — Prove (§11).** Prove che manipolano il tempo in modo **deterministico** (nessuna attesa reale) per
  verificare i tre stati.

## 4. Criteri di accettazione

**CA-1 — Fonte in linea**
- **Dato** una fonte con ritardo atteso di un giorno, il cui ultimo fatto è arrivato due ore fa
- **Quando** si apre la sezione Fonti
- **Allora** lo stato è «in linea» e il momento dell'ultimo dato è mostrato in forma leggibile

**CA-2 — Fonte silente**
- **Dato** una fonte con ritardo atteso di un giorno, il cui ultimo fatto è arrivato sei giorni fa
- **Quando** si apre il cruscotto
- **Allora** la fonte risulta «silente», gli indicatori che ne dipendono portano il contrassegno di
  incompletezza e la scheda del numero dice quale fonte tace e da quando

**CA-3 — Notifica una sola volta**
- **Dato** una fonte appena passata a silente, per cui la notifica è già stata mandata
- **Quando** passa un altro giorno di silenzio
- **Allora** non viene mandata una seconda notifica; quando la fonte torna in linea, l'episodio si chiude e una
  eventuale ricaduta futura potrà notificare di nuovo

**CA-4 — Isolamento fra account**
- **Dato** la stessa fonte collegata da due account, in linea per `A` e silente per `B`
- **Quando** un utente di `A` legge lo stato
- **Allora** vede «in linea», e nessuna manipolazione della richiesta gli mostra lo stato di `B`

**CA-5 — Un `member` vede lo stato**
- **Dato** un utente con ruolo `member`
- **Quando** apre la sezione Fonti
- **Allora** vede gli stati e i momenti dell'ultimo dato, pur non potendo collegare né revocare

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dei tre stati con tempo controllato, e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** sullo stato delle fonti;
- [ ] **prova end-to-end**: *rimando* alla storia 0034, dove il percorso `[J-INSIGHTS]` verifica che un numero
      su fonte silente sia marcato; voce `da-coprire` nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la soglia dei tre stati e il perché;
- [ ] contratto degli **strumenti conversazionali**: `stato_delle_fonti` dichiarato (lettura, senza conferma) —
      contratto completo nella storia 0031;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | servono fonti collegate di cui misurare la salute |
| storia `0006` | il ritardo atteso è dichiarato nel contratto della fonte |

## 7. Fuori ambito

- il contrassegno di incompletezza sul singolo valore: è la storia 0016, che ha una nozione più fine
  (buco in un periodo, non solo fonte silente);
- il comportamento degli avvisi su numeri incompleti: storia 0020;
- la diagnostica per chi amministra la piattaforma: [estensioni-admin.md](../estensioni-admin.md).

## 8. Punti aperti

- **Su quale canale arriva la notifica di fonte silente?** Posta elettronica è l'unico canale disponibile oggi;
  una notifica dentro il backoffice sarebbe meno invadente ma richiede un meccanismo di piattaforma che non
  esiste. Raccomandazione: **posta elettronica ai soli `owner` e `admin`**, con la possibilità di spegnerla.
  Chiude: **sviluppatore**.
- **Il ritardo atteso lo dichiara la fonte o lo configura il cliente?** Lo dichiara la fonte, perché lo conosce;
  ma un cliente che fattura una volta al mese vedrebbe la fatturazione sempre «silente». Raccomandazione: la
  fonte dichiara il valore predefinito, il cliente può allentarlo per il proprio account. Chiude:
  **sviluppatore**.
