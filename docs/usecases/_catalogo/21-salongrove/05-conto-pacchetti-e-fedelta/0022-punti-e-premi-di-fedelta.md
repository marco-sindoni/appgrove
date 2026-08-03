# 0022 — Punti e premi di fedeltà

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 05 — Conto, pacchetti e fedeltà
**Storia**: `0022` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un salone
> voglio che le clienti accumulino punti su quello che spendono e possano usarli per uno sconto o per una piega
> così da avere una ragione in più perché tornino da me e non da quello che ha aperto in fondo alla via.

**Contesto.** La fidelizzazione è una delle otto funzioni chieste dalla scheda di catalogo ed è nel piano alto di
tutti i gestionali italiani esaminati (§2.1 della descrizione), il che dice che si vende. La funzione va tenuta
**semplice** per una ragione di prodotto: un programma di fedeltà che il titolare non riesce a spiegare in una
frase alla cliente non lo usa nessuno. Un punto per euro speso, un premio ogni tanti punti: il resto è
complicazione.

## 2. Requisiti funzionali

1. **RF-1** — Il salone definisce **una** regola di maturazione: quanti punti per euro speso, su servizi, su
   prodotti o su entrambi. Una sola regola attiva per volta.
2. **RF-2** — Alla chiusura del conto i punti maturano sul cliente, con un **movimento immutabile** che cita il
   conto.
3. **RF-3** — Il salone definisce i **premi**: quanti punti costano e cosa danno (uno sconto a valore, oppure un
   servizio a listino). Spendere punti aggiunge una riga al conto con l'origine «premio fedeltà».
4. **RF-4** — I punti hanno una **scadenza** facoltativa (per esempio dodici mesi dalla maturazione), e il saldo
   distingue i punti in scadenza vicina.
5. **RF-5** — Il saldo non può andare **sotto zero**, e un premio riscosso non si annulla: si corregge con una
   rettifica sul conto, che compensa anche i punti.
6. **RF-6** — Il cliente può non avere la tessera: la fedeltà è facoltativa e un salone che non la usa non se ne
   accorge.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Regola, saldi e movimenti filtrano per `tenant_id` dal token verificato:
  i punti maturati in un salone non valgono in un altro, mai, nemmeno per errore.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|PUT /api/<app>/v1/fedelta/regola`,
  `GET|POST /api/<app>/v1/fedelta/premi`, `GET /api/<app>/v1/clienti/{id}/fedelta`; la maturazione e la spesa
  avvengono dentro la chiusura del conto; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Tabelle `tessera_fedelta` e `movimento_punti` con `tenant_id`, UUID versione 7,
  colonne di controllo; **`movimento_punti` è immutabile** (storia `0002`); il saldo è la somma dei movimenti,
  come per il magazzino.
- **RT-4 — Atomicità (storia `0019`).** Maturazione e spesa dei punti stanno dentro la transazione di chiusura.
- **RT-5 — Concorrenza.** Due chiusure simultanee non portano il saldo sotto zero: il vincolo sta nel database.
- **RT-6 — Varchi e quota (§6, §7).** Funzione accesa dal piano; `402` a piano insufficiente.
- **RT-7 — Modulo frontend (§3, §5).** Il saldo compare sulla scheda della cliente e nel conto, con i premi
  raggiungibili in evidenza; la regola si imposta in un modulo di tre campi. Solo token del sistema di design.
- **RT-8 — Cinque lingue (§4).** Etichette, testi dei premi, avvisi di scadenza in `en, it, fr, es, de`.
- **RT-9 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: `fedelta.saldo` e i movimenti
  (economico, finalità «maturare e spendere i punti», base «esecuzione del contratto», durata «finché la tessera è
  attiva, poi 12 mesi»). Campi annotati; tabelle in esportazione e cancellazione.
- **RT-10 — Esposizione conversazionale (§12).** Nessuno strumento proprio: il saldo si legge nella scheda del
  cliente e la spesa avviene dentro `chiudi_conto`, già con conferma obbligatoria. **Deliberato**: un assistente
  che spende punti al posto di qualcuno è un problema, non una comodità.
- **RT-11 — Registrazione eventi (§14).** `punti maturati`, `premio riscosso`, `punti scaduti` con `tenant_id`,
  `app_id`, `user_id` e correlazione — mai il nome del cliente.

## 4. Criteri di accettazione

**CA-1 — Maturazione**
- **Dato** una regola di un punto per euro sui servizi
- **Quando** si chiude un conto di 60 € di servizi e 20 € di prodotti
- **Allora** maturano 60 punti, non 80

**CA-2 — Spesa di un premio**
- **Dato** una cliente con 200 punti e un premio da 150 punti che vale 15 € di sconto
- **Quando** si riscuote il premio su un conto
- **Allora** il conto ha una riga da −15 € con origine «premio fedeltà» e il saldo scende a 50

**CA-3 — Mai sotto zero**
- **Dato** una cliente con 100 punti
- **Quando** due conti tentano di riscuotere insieme un premio da 100 punti
- **Allora** uno solo riesce e il saldo è zero, mai negativo

**CA-4 — Rettifica che compensa i punti**
- **Dato** un conto chiuso che ha fatto maturare 60 punti
- **Quando** lo si rettifica di −20 €
- **Allora** il saldo punti si corregge di conseguenza, con un movimento visibile

**CA-5 — Scadenza**
- **Dato** punti maturati tredici mesi fa con scadenza a dodici
- **Quando** la lavorazione periodica gira
- **Allora** i punti risultano scaduti con un movimento che lo dice, e il saldo scende

**CA-6 — Isolamento fra account**
- **Dato** una cliente con la stessa posta elettronica in due saloni
- **Quando** matura punti nel primo
- **Allora** il saldo nel secondo non cambia di nulla

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sulla maturazione e sulla scadenza, di **integrazione** su concorrenza e atomicità;
- [ ] prova di **isolamento fra account** su saldi e premi;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-SALONGROVE-PKG]` della storia `0031`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per tessera e movimenti;
- [ ] **registro delle decisioni**: una sola regola attiva, saldo come somma dei movimenti, nessuno strumento
      conversazionale per la spesa dei punti e il perché;
- [ ] avvio locale invariato; il salone di prova ha una tessera con saldo.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | maturazione e spesa avvengono alla chiusura del conto |

## 7. Fuori ambito

- programmi a livelli («cliente oro»), moltiplicatori e campagne a punti doppi: complicazione che il segmento non
  chiede;
- l'invito di un'amica con premio: è marketing, ed è dell'app 16;
- la comunicazione al cliente del saldo per messaggio: usa il canale, che è un punto aperto ereditato (§5 della
  descrizione).

## 8. Punti aperti

**I punti sono un debito, come i pacchetti.** Un saldo punti riscuotibile è denaro promesso. La proposta è
mostrarlo al titolare come valore complessivo, insieme al residuo dei pacchetti (storia `0020`, RF-7). Se lo
sviluppatore volesse che i punti scadano per forza, quella è una scelta commerciale con un risvolto verso i
clienti finali: la scadenza qui resta **facoltativa** e la imposta il salone.
