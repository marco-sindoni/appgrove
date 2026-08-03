# 0027 — Istantanea mensile dei ricavi

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 06 — Metriche dei ricavi ricorrenti
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una scuola di musica con centoventi iscritti a canone
> voglio sapere quanto mi entra ogni mese, contato sempre allo stesso modo
> così da poter dire com'è andato marzo anche a novembre, senza rifare i conti a mano.

**Contesto.** È la terza delle tre promesse dell'applicazione (§1 della
[descrizione](../application-description.md)): il foglio di calcolo non risponde alla domanda «quanto ti entra
ogni mese, al netto di chi se n'è andato?», perché **non tiene lo storico**. Qui si costruisce il numero che
risponde: il **ricavo ricorrente mensile**, cioè la somma dei canoni vivi normalizzata a un mese — un abbonamento
annuale da 240 € vale 20 € al mese, uno trimestrale da 90 € vale 30 € al mese. Il numero da solo però non basta:
deve essere **fotografato** una volta al mese e **non ricalcolato all'indietro**, altrimenti la storia cambia sotto
i piedi di chi la guarda. Se a giugno si corregge il prezzo di un piano, il ricavo di marzo deve restare quello che
si vedeva a marzo: è la differenza fra una misura e un'opinione.

Nessuna norma disciplina questo calcolo per una micro-impresa (§2.3 della descrizione): è **gestione, non
contabilità**, e va detto anche a schermo, perché nessuno lo scambi per un dato di bilancio.

## 2. Requisiti funzionali

1. **RF-1** — L'app calcola il **ricavo ricorrente mensile** dell'account come somma dei canoni degli abbonamenti
   che stanno negli stati che contano, ciascuno normalizzato a un mese secondo il proprio ciclo (mensile ×1,
   trimestrale ÷3, annuale ÷12).
2. **RF-2** — Contano gli abbonamenti in `attivo`, `in_ritardo` e `disdetto_a_scadenza` (il canone è ancora dovuto
   fino a fine periodo); **non** contano `in_prova` (non è ancora ricavo), `sospeso` (il servizio è fermo) e
   `cessato`. La regola è scritta a schermo, non solo nel codice.
3. **RF-3** — Una volta al mese una lavorazione salva l'**istantanea** del mese chiuso: ricavo ricorrente mensile,
   numero di abbonamenti che l'hanno prodotto, numero di abbonati distinti, valuta.
4. **RF-4** — L'istantanea **non si ricalcola**: una correzione fatta oggi su un piano o su un prezzo cambia il
   mese in corso, mai i mesi già fotografati.
5. **RF-5** — La lavorazione è **idempotente** e recupera i mesi saltati: se non gira per due mesi, al primo giro
   utile produce le istantanee mancanti, ciascuna con i dati di quel mese, e non ne produce due per lo stesso mese.
6. **RF-6** — La sezione *Andamento* mostra la serie degli ultimi mesi con il valore del mese in corso separato e
   marcato come **provvisorio**, perché il mese non è finito.
7. **RF-7** — La serie si **esporta** in un file tabellare, con l'avvertenza scritta che è una misura di gestione
   e non un documento contabile.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo e la lettura delle istantanee filtrano per `tenant_id` preso
  dal token verificato; una richiesta che portasse un `tenant_id` proprio viene ignorata. La lavorazione mensile
  scorre gli account uno per uno e non aggrega mai fra account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/abbonati/v1/metriche/ricavo-ricorrente`
  (con intervallo di mesi) e `GET /api/abbonati/v1/metriche/ricavo-ricorrente/esporta`; corpo e parametri validati;
  errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V20__istantanea_ricavi.sql` sullo schema `app_abbonati`: tabella
  `istantanea_ricavi` con `tenant_id`, chiave primaria UUID versione 7, mese, importo normalizzato in **centesimi**,
  valuta, conteggi, colonne di controllo e cancellazione logica; vincolo di unicità su (`tenant_id`, `mese`) che
  rende impossibile la doppia istantanea.
- **RT-4 — Aritmetica del ricorrente.** La normalizzazione a mese è **la stessa funzione** che la storia `0012` usa
  per il calendario e la `0014` per il conguaglio: è il terzo utilizzo, ed è il momento in cui si solleva la mano
  sul punto aperto n. 1 della descrizione (estrarre o no l'aritmetica in una libreria condivisa). Gli importi si
  trattano in centesimi interi; l'arrotondamento avviene **una sola volta**, alla presentazione, e la regola scelta
  si scrive nel registro delle decisioni.
- **RT-5 — Modulo frontend (§3, §5).** Sezione *Andamento* del modulo `abbonati`: serie mensile con il mese in
  corso distinto; dati letti con il client generato; solo token del sistema di design; funziona in tema chiaro e
  scuro; nessun colore scritto a mano.
- **RT-6 — Cinque lingue (§4).** Etichette, avvertenza «misura di gestione, non contabilità» e intestazioni
  dell'esportazione dallo spazio-nomi `abbonati`, presenti in `en, it, fr, es, de`.
- **RT-7 — Varchi e quota (§6, §7).** La lettura non consuma la metrica `abbonamenti_attivi` (natura `stock`): non
  crea nulla. Con abbonamento di piattaforma `canceled` risponde `402`; in `past_due` resta accessibile.
- **RT-8 — Esposizione conversazionale (§12).** Contratto dello strumento
  `metriche_ricorrenti(mese) → ricavo mensile, attivi, scomposizione`, marcato **lettura**: dichiarato qui per la
  parte del ricavo, completato dalla storia `0028` per la scomposizione, raccolto nella `0031`.
- **RT-9 — Dati personali (§10).** **Nessun dato personale nuovo**: l'istantanea è un aggregato e non contiene
  riferimenti a persone. Per la stessa ragione `istantanea_ricavi` **resta fuori** da `exportData` e `purgeData`, e
  il motivo va scritto: un aggregato che non identifica nessuno non è un dato personale, ma la scelta va motivata
  perché non sia scambiata per una dimenticanza (storia `0035`).
- **RT-10 — Registrazione eventi (§14).** `istantanea prodotta (mese)`, `istantanee di recupero prodotte (quante)`,
  `esportazione della serie`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza importi
  riferibili a persone.
- **RT-11 — Prove (§11).** Unità sulla normalizzazione dei tre cicli e sugli stati che contano; integrazione sulla
  lavorazione mensile con database effimero, compreso il recupero dei mesi saltati; prova che un ricalcolo non
  altera un'istantanea già scritta.

## 4. Criteri di accettazione

**CA-1 — Normalizzazione dei cicli**
- **Dato** tre abbonamenti attivi: mensile da 30 €, trimestrale da 90 €, annuale da 240 €
- **Quando** si legge il ricavo ricorrente mensile
- **Allora** vale 80 € (30 + 30 + 20) e il dettaglio mostra come ciascuno ha contribuito

**CA-2 — Stati che contano**
- **Dato** un abbonamento `in_prova`, uno `sospeso` e uno `disdetto_a_scadenza` ancora dentro il periodo
- **Quando** si legge il ricavo del mese
- **Allora** conta solo il terzo, e la spiegazione a schermo dice perché

**CA-3 — Il passato non si riscrive**
- **Dato** l'istantanea di marzo già salvata
- **Quando** a giugno si crea una versione di prezzo nuova per un piano usato a marzo
- **Allora** il valore di marzo resta invariato e cambia soltanto il mese in corso

**CA-4 — Lavorazione idempotente e recupero**
- **Dato** una lavorazione che non gira per due mesi
- **Quando** riparte
- **Allora** produce le due istantanee mancanti con i dati di quei mesi, non ne produce doppioni, e ripetendo il
  giro nulla cambia

**CA-5 — Mese in corso marcato provvisorio**
- **Dato** il giorno 12 del mese · **Quando** si apre *Andamento*
- **Allora** il mese in corso è distinto dagli altri e dichiarato provvisorio, e l'esportazione lo marca allo
  stesso modo

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con abbonamenti propri
- **Quando** un utente di `A` legge la serie del ricavo
- **Allora** vede solo i propri numeri, anche forzando l'identificativo dell'altro account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`);
- [ ] prove di **unità** sulla normalizzazione e sugli stati che contano; di **integrazione** sulla lavorazione
      mensile con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla risorsa delle metriche;
- [ ] **prova end-to-end**: *rimando* — la serie compare nel percorso `[J-ABBONATI]` della storia `0033`, con voce
      `da-coprire` nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e storia
      proprietaria `0033`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, avvertenza compresa;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la motivazione scritta dell'esclusione dell'aggregato;
- [ ] **registro delle decisioni** compilato: stati che contano, regola di arrotondamento, immutabilità
      dell'istantanea, terzo utilizzo dell'aritmetica del ricorrente;
- [ ] contratto dello strumento `metriche_ricorrenti` dichiarato per la parte del ricavo;
- [ ] documentazione aggiornata dove descrive le metriche.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0011` | gli stati dell'abbonamento sono ciò che decide cosa conta |
| storia `0012` | l'aritmetica del calendario ricorrente e la lavorazione programmata nascono lì |
| storia `0007` | il canone da normalizzare è quello della versione di prezzo agganciata all'abbonamento |
| decisione di piattaforma sul punto aperto n. 1 | se l'aritmetica del ricorrente diventi una libreria condivisa: qui si solleva la mano, non si decide |

## 7. Fuori ambito

- la **scomposizione** della variazione fra due mesi (nuovo, espansione, contrazione, abbandono): storia `0028`;
- l'**abbandono** e la durata media: storia `0029`;
- la **previsione** dei mesi futuri: storia `0030`;
- il riconoscimento contabile dei ricavi per competenza: non è di questa app e nemmeno della suite — lo fa il
  commercialista con i documenti di **02 BillGrove**.

## 8. Punti aperti

**Le valute diverse.** L'istantanea assume **una** valuta per account. Se un cliente vendesse a canone in due
valute, sommarle sarebbe un errore e convertirle richiederebbe un tasso di cambio, cioè una fonte esterna e una
data di riferimento. **Proposta**: una valuta per account nel nucleo, con blocco esplicito e messaggio chiaro se
compaiono prezzi in valute diverse; il resto è un tema da riaprire solo se qualcuno lo chiede davvero.
Chiude: lo sviluppatore.

**Se contare o no la parte incassata davvero.** Il ricavo ricorrente misura il **dovuto ricorrente**, non
l'incassato: un abbonamento `in_ritardo` continua a contare. È la convenzione più diffusa e la più utile per
guardare avanti, ma un titolare potrebbe leggerla come «numeri gonfiati». **Proposta**: tenere la convenzione e
mostrare accanto, sempre, il «da recuperare» dell'epica 04, così che le due letture stiano una accanto all'altra.
Chiude: lo sviluppatore, con la direzione di prodotto.
