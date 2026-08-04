# 0022 — Punteggio di reputazione della sede

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 05 — Reputazione e vetrina
**Storia**: `0022` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare con due sedi
> voglio vedere in un colpo d'occhio come sta ciascuna: media, quante recensioni, come stanno distribuite, e se
> sto migliorando o peggiorando
> così da capire se il lavoro che sto facendo serve, senza dovermi mettere a contare.

**Contesto.** La media delle stelle è il numero che il cliente guarda per primo, e da solo dice poco: una media di
4,3 su 12 recensioni e una su 400 sono situazioni diverse, e una media stabile che nasconde un peggioramento
recente è la trappola più comune. Questa storia mette insieme i quattro numeri che servono davvero — media,
volume, distribuzione, andamento — e resiste alla tentazione di aggiungerne altri dieci.

Un punto che va deciso qui e non altrove: **la media dell'app non coincide con quella della piattaforma**. Google
calcola la sua a modo suo, non lo pubblica, e può escludere recensioni che noi vediamo. Se mostrassimo un numero
diverso da quello che il cliente legge sulla sua scheda senza spiegarlo, l'app perderebbe credibilità al primo
sguardo.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni sede e per ogni piattaforma collegata l'app calcola: media dei voti, numero di recensioni,
   distribuzione per voto (quante a 1, 2, 3, 4, 5 stelle) e variazione rispetto al periodo precedente.
2. **RF-2** — La media si calcola **solo sulle recensioni ancora pubbliche all'origine** (storia 0009): quelle
   sparite non contano, e l'app lo dice quando il numero cambia per quel motivo.
3. **RF-3** — Accanto alla media calcolata da noi si mostra, quando la piattaforma la fornisce, **la media
   pubblicata dalla piattaforma**, con una riga che spiega perché possono differire. Se non la fornisce, si dice
   che non è disponibile invece di far credere che i due numeri siano lo stesso.
4. **RF-4** — L'andamento si vede su un periodo scelto (ultimi 30, 90, 365 giorni) come serie della media mobile e
   del volume: due linee, non un cruscotto.
5. **RF-5** — Con meno di un numero minimo di recensioni (proposta: 5) l'app **non mostra una media** ma dice
   quante ne mancano perché il numero significhi qualcosa. Mostrare «5,0 su 1 recensione» come se fosse un
   risultato è disonesto verso chi legge.
6. **RF-6** — Con più sedi, una vista di confronto mostra le sedi affiancate. È un confronto **interno**, fra sedi
   dello stesso cliente: il confronto con i concorrenti non c'è (descrizione §11.3).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni calcolo filtra per `tenant_id` preso dal token verificato; nessuna
  media aggregata attraversa gli account, nemmeno per confronti «di settore».
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/recensioni/v1/sedi/{id}/punteggio?periodo=` e
  `GET /api/recensioni/v1/punteggi` per il confronto fra sedi; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `punteggio_reputazione` (storia 0002) come **fotografia periodica**: serve
  a ricostruire l'andamento anche dopo che le recensioni sono scadute (storia 0010). Il valore corrente si
  calcola dal vivo; la serie storica viene dalle fotografie. Migrazione `V9__punteggio_periodico.sql`.
- **RT-4 — Modulo frontend (§3, §5).** *Panoramica*: blocco del punteggio per sede con media, volume,
  distribuzione a barre e variazione; pagina di dettaglio con l'andamento. La distribuzione si legge **anche
  senza colore** (etichette e valori, non solo barre colorate). Solo token del sistema di design; tema chiaro e
  scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe in `en, it, fr, es, de`, compresa la spiegazione della
  differenza fra la nostra media e quella della piattaforma. Attenzione alla formattazione dei numeri per lingua:
  la virgola decimale non è uguale ovunque.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota; `402` con abbonamento `canceled`.
- **RT-7 — Esposizione conversazionale (§12).** È lo strumento `punteggio_reputazione` (storia 0027), di sola
  lettura, che restituisce media, volume, distribuzione e andamento per sede e periodo.
- **RT-8 — Dati personali (§10).** Nessun dato personale: le fotografie sono aggregati. Va detto nel manifesto,
  perché è la ragione per cui possono sopravvivere alla cancellazione delle recensioni.
- **RT-9 — Registrazione eventi (§14).** `fotografia del punteggio calcolata` per sede e periodo, con `tenant_id`,
  `app_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — I quattro numeri**
- **Dato** una sede con dodici recensioni di voto misto
- **Quando** si apre la *Panoramica*
- **Allora** si vedono media, numero, distribuzione per voto e variazione rispetto al periodo precedente

**CA-2 — Le recensioni sparite non contano**
- **Dato** una sede con dieci recensioni, di cui una marcata non più pubblica
- **Quando** si calcola la media
- **Allora** è calcolata su nove, e l'app spiega perché il numero è cambiato

**CA-3 — Troppo poche recensioni**
- **Dato** una sede con tre recensioni
- **Quando** si apre la *Panoramica*
- **Allora** non compare una media, ma l'indicazione di quante ne servono ancora

**CA-4 — Differenza con la piattaforma**
- **Dato** una piattaforma che pubblica una media diversa dalla nostra
- **Quando** si guarda il punteggio
- **Allora** si vedono entrambe, con la spiegazione della differenza

**CA-5 — Isolamento fra account**
- **Dato** due account con sedi omonime
- **Quando** un utente di `A` chiede il punteggio
- **Allora** è calcolato solo sulle recensioni di `A`

**CA-6 — L'andamento sopravvive alla scadenza**
- **Dato** recensioni vecchie cancellate per scadenza di conservazione (storia 0010)
- **Quando** si guarda l'andamento dell'ultimo anno
- **Allora** la serie storica c'è comunque, ricostruita dalle fotografie periodiche

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della media, della distribuzione e della soglia minima; di **integrazione**
      sulla lavorazione delle fotografie periodiche con database effimero;
- [ ] prova di **isolamento fra account** sul calcolo;
- [ ] **prova end-to-end**: *coprire ora* il passo «il punteggio della sede riflette le recensioni raccolte» nel
      percorso `[J-RECENSIONI]`, e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, formattazione dei numeri compresa;
- [ ] **manifesto dei dati**: nessuna voce nuova; dichiarato che le fotografie sono aggregate;
- [ ] **registro delle decisioni** compilato, con la scelta della soglia minima e della doppia media;
- [ ] controllo automatico di **accessibilità** verde, compresa la leggibilità della distribuzione senza colore.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | servono le recensioni e il loro stato |
| storia `0010` | la scadenza delle recensioni è il motivo per cui servono le fotografie periodiche |

## 7. Fuori ambito

- l'analisi dei temi ricorrenti — storia 0023;
- il rapporto periodico — storia 0026;
- il confronto con i concorrenti (descrizione §11.3): non c'è, e il motivo è scritto.

## 8. Punti aperti

- **La soglia di cinque recensioni** sotto la quale non si mostra una media è una proposta: è la scelta che
  protegge chi legge, ma può frustrare un cliente nuovo che vuole vedere subito un numero. Da confermare.
- **Se la media della piattaforma sia leggibile** attraverso le interfacce che usiamo: da verificare insieme alle
  condizioni della storia 0010. Se non lo fosse, il RF-3 diventa «non disponibile», che è comunque meglio del
  silenzio.
</content>
