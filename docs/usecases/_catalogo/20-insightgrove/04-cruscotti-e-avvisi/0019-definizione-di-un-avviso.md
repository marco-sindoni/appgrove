# 0019 — Definizione di un avviso su soglia

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 04 — Cruscotti e avvisi
**Storia**: `0019` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che non guarda il cruscotto tutti i giorni
> voglio essere avvisato quando i crediti scaduti superano una cifra che decido io
> così da accorgermene quando c'è ancora tempo per fare qualcosa.

**Contesto.** Il cruscotto serve a chi lo guarda; l'avviso serve a chi non lo guarda — cioè, in una
micro-impresa, quasi sempre. È la funzione che trasforma InsightGrove da pagina da consultare a strumento che
lavora da solo. Questa storia costruisce la **definizione** dell'avviso: che cosa si sorveglia, con quale
condizione, chi viene avvisato. La valutazione e il recapito sono la storia 0020, e sono separati perché il
problema difficile sta là — un avviso che suona su un numero incompleto è peggio di un avviso che non suona.

## 2. Requisiti funzionali

1. **RF-1** — Un avviso dichiara: metrica, periodo di valutazione, condizione (maggiore di, minore di, variazione
   percentuale rispetto al periodo precedente maggiore di), valore di soglia, destinatari e stato (attivo,
   sospeso).
2. **RF-2** — I destinatari sono indirizzi di posta elettronica di persone dell'account, scelti fra quelli
   dell'account; è ammesso aggiungere un indirizzo esterno (il commercialista) con una conferma esplicita che
   dica che quella persona riceverà dati economici dell'azienda.
3. **RF-3** — Un avviso su una metrica **economica** può essere creato solo da `owner` o `admin`, e i suoi
   destinatari sono chi ha il ruolo per vedere quella metrica — con l'eccezione, esplicitamente confermata, di
   un indirizzo esterno.
4. **RF-4** — Alla creazione, l'app mostra **che cosa sarebbe successo negli ultimi tre mesi** con quella soglia:
   quante volte sarebbe scattato. È il modo più semplice di evitare soglie inutili.
5. **RF-5** — Un avviso ha una **frequenza massima**: non suona più di una volta per periodo di valutazione,
   qualunque cosa succeda.
6. **RF-6** — Un avviso su una metrica ritirata, o su una fonte scollegata, passa automaticamente a **sospeso** e
   lo dice, invece di restare attivo e non suonare mai.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `avviso` filtra per `tenant_id` preso dal
  gettone verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/insights/v1/avvisi`,
  `GET|PUT|DELETE /api/insights/v1/avvisi/{id}`; corpo validato; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__avvisi.sql` sullo schema `app_insights`: tabella `avviso` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Avvisi` del modulo `insights`; solo token del sistema di design;
  tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, comprese le condizioni e la conferma sull'indirizzo esterno,
  esistono in `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** Creare un avviso su metrica economica richiede `owner` o `admin`; un `member`
  può creare avvisi su metriche operative. **Nessun consumo di quota**: gli avvisi sono illimitati in ogni piano.
- **RT-8 — Dati personali (§10).** **Voce nuova nel manifesto**: `avviso.destinatari` è un dato di contatto di
  persone dell'account o esterne, in italiano e inglese, con campo annotato `@PersonalData`, e la tabella
  `avviso` presente in `exportData` e `purgeData`.
- **RT-14 — Registrazione eventi (§14).** «Avviso creato», «avviso modificato», «avviso sospeso» con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione; **mai** gli indirizzi dei destinatari.

## 4. Criteri di accettazione

**CA-1 — Creazione con anteprima storica**
- **Dato** un `owner` che crea un avviso «crediti scaduti maggiori di 10.000 €, valutazione settimanale»
- **Quando** compila la soglia
- **Allora** vede «negli ultimi tre mesi sarebbe scattato 2 volte» prima di salvare

**CA-2 — Destinatario esterno con conferma**
- **Dato** un avviso su una metrica economica
- **Quando** si aggiunge un indirizzo che non appartiene all'account
- **Allora** compare una conferma che dice che quella persona riceverà dati economici dell'azienda, e solo dopo
  la conferma l'indirizzo viene accettato

**CA-3 — Un `member` non crea avvisi economici**
- **Dato** un utente `member`
- **Quando** tenta di creare un avviso su `fatturato_emesso` (economica)
- **Allora** riceve `403`; può invece creare un avviso su una metrica operativa

**CA-4 — Avviso orfano si sospende**
- **Dato** un avviso su una metrica la cui fonte viene scollegata
- **Quando** si apre la sezione Avvisi
- **Allora** l'avviso risulta sospeso con il motivo, e non resta «attivo» in silenzio

**CA-5 — Isolamento fra account**
- **Dato** due account con avvisi propri
- **Quando** un utente di `A` chiede un avviso di `B` per identificativo
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'anteprima storica e sulla sospensione automatica, e di **integrazione** sulle
      risorse;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sugli avvisi;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la voce `avviso.destinatari`, campo annotato
      `@PersonalData`, tabella presente in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato, con l'anteprima storica e la conferma sull'indirizzo esterno;
- [ ] contratto degli **strumenti conversazionali**: `crea_avviso` dichiarato come **scrittura con conferma**
      (storia 0032);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0015` | l'avviso valuta un valore su un periodo |
| storia `0017` | l'avviso si crea anche a partire da un riquadro del cruscotto |
| storia `0014` | la classe di riservatezza governa chi può creare l'avviso e chi può riceverlo |

## 7. Fuori ambito

- la valutazione periodica e il recapito: storia 0020;
- il registro di ciò che è scattato e la sospensione temporanea: storia 0021;
- avvisi su canali diversi dalla posta elettronica: non ce ne sono di disponibili oggi in piattaforma.

## 8. Punti aperti

- **L'indirizzo esterno è una buona idea?** Manda dati economici dell'azienda fuori dall'account, a una persona
  che non ha un accesso e di cui non si controlla la casella. È **utile** (il commercialista) e insieme è un
  **effetto verso l'esterno**. Raccomandazione: ammetterlo con conferma esplicita e traccia di chi l'ha
  aggiunto, come descritto. Ma è una decisione che non spetta a un agente: chiude **sviluppatore**.
- **Tre condizioni bastano?** Maggiore, minore e variazione percentuale coprono i casi delle fonti consultate.
  Aggiungere condizioni composte trasformerebbe l'avviso in un linguaggio. Raccomandazione: restare a tre.
  Chiude: **sviluppatore**.
