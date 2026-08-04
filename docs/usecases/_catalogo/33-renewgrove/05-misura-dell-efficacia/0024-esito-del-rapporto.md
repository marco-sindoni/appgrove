# 0024 — Esito del rapporto

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 05 — Misura dell'efficacia
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019` — l'esito misura ciò che un intervento confermato ha prodotto, quindi l'intervento deve esistere prima
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena confermato una telefonata a un cliente che rischiavo di perdere
> voglio che l'app fissi **adesso** entro quando andrà verificato se quel cliente è rimasto, e con quale regola
> così da poter dire, fra sei mesi, se è servito — senza scegliere a quel punto la misura che mi dà ragione.

**Contesto.** Fin qui l'app sa dire chi si rischia di perdere (epica 03) e sa preparare qualcosa per trattenerlo
(epica 04). Non sa dire se è servito, e finché non lo sa il prodotto vende una promessa: la
[descrizione](../application-description.md) lo scrive senza addolcirlo al §11 («il punteggio non azzecca, e la
fiducia crolla»). La misura però si rovina con un gesto solo, ed è il gesto più naturale del mondo: guardare come
sono andate le cose e **poi** decidere quanto tempo contava. È il modo più elegante di dimostrare qualsiasi tesi.
Per questo la finestra di osservazione e la regola di perdita si dichiarano **prima** dell'intervento, si congelano,
e l'esito valutato non si riscrive. È il momento giusto per farlo ora e non prima perché solo adesso esiste
l'intervento confermato (`0019`) da cui la finestra parte.

## 2. Requisiti funzionali

1. **RF-1** — Alla **conferma** di un intervento (`0019`) nasce automaticamente un `EsitoDelRapporto` in stato
   **ancora aperto**, che porta scritti: il rapporto, il momento della conferma, la **durata della finestra di
   osservazione**, il **giorno della valutazione** che ne discende, la **regola di perdita** applicabile e il gruppo
   `intervenuto`. Nessuno di questi campi si può valorizzare dopo.
2. **RF-2** — La durata della finestra si sceglie da un **elenco chiuso** dichiarato per attività (proposta: 30, 90,
   180, 365 giorni) al momento della conferma, con un valore predefinito visibile. Una finestra già creata **non è
   modificabile**: chi vuole una misura diversa apre una valutazione nuova, e la precedente resta con il proprio
   esito.
3. **RF-3** — La **regola di perdita** è dichiarata **per fonte**, come elenco chiuso, e dice cosa conta come «perso»
   per i fatti che quella fonte pubblica: disdetta registrata da SubGrove; nessun documento emesso per un numero
   dichiarato di mesi in BillGrove; nessuna prenotazione onorata per un numero dichiarato di mesi in BookGrove;
   nessun segnale di alcun tipo per l'intera finestra quando l'unica fonte è l'inserimento a mano (`0010`). La regola
   applicata è **mostrata a schermo** accanto all'esito, con parole e non con un codice.
4. **RF-4** — Al **giorno della valutazione** un processo valuta l'esito **una volta sola** e lo porta a
   **trattenuto** o **perso** secondo la regola congelata. La riga è in **sola aggiunta**: un esito valutato non si
   riscrive, non si annulla e non si rivaluta.
5. **RF-5** — Se al giorno della valutazione la fonte che porta la regola di perdita è **in silenzio oltre il proprio
   ritardo atteso** (`0011`), la valutazione è **rimandata** e l'esito resta *ancora aperto* con il contrassegno di
   attesa e il motivo. Assenza di segnali da una fonte muta **non** vale come «trattenuto»: sarebbe la scorciatoia
   che fa vincere il prodotto per difetto.
6. **RF-6** — La scheda del rapporto elenca le valutazioni aperte e chiuse: per ciascuna, giorno di valutazione,
   durata della finestra, regola applicata, gruppo ed esito. Chi guarda deve poter capire **perché** un rapporto è
   contato come trattenuto senza chiedere a nessuno.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `esito_del_rapporto` filtra per `tenant_id`
  preso dal token verificato; il processo di valutazione lavora **per account**, e un `tenant_id` che arrivasse dal
  corpo della richiesta o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/fidelizzazione/v1/rapporti/{id}/esiti`,
  `POST /api/fidelizzazione/v1/interventi/{id}/finestra-di-osservazione` (dichiarazione della finestra alla
  conferma); corpo validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__esito_del_rapporto.sql` sullo schema `app_fidelizzazione`: tabella
  `esito_del_rapporto` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione
  logica. Tabella **in sola aggiunta**: nessun aggiornamento dei campi di misura dopo la valutazione, come già per
  `segnale` e `punteggio` (§4.4 della descrizione). Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione **Esiti** dentro la scheda del rapporto del modulo `fidelizzazione`;
  dati letti con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro. Lo stato
  *ancora aperto con attesa* è percepibile **senza affidarsi al colore**.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi degli esiti, durate della finestra, testi delle
  regole di perdita, motivo dell'attesa — passano dallo spazio-nomi `fidelizzazione` e sono presenti in `en, it, fr,
  es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La storia **non consuma quota**: la metrica `rapporti_sorvegliati` è a
  giacenza e si consuma alla nascita del rapporto (`0009`), non alla misura. Con abbonamento `canceled` le rotte
  rispondono `402`; dichiarare una finestra è riservato a chi può confermare un intervento (`owner`/`admin`), un
  `member` legge gli esiti e riceve `403` se prova a dichiararne una.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo qui: l'esito compare dentro `stato_rapporto`
  e `efficacia_degli_interventi`, dichiarati nella storia `0028`. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Sì, la storia tratta dati personali**: `esito_del_rapporto` è un fatto riferito
  a un cliente del nostro cliente, cioè a una persona identificabile attraverso il rapporto. Voce
  `esito_del_rapporto` nel manifesto `docs/compliance/manifests/fidelizzazione.yaml` in **italiano e inglese**
  (dove vive, di chi è, che dato è, a cosa serve, base giuridica, conservazione proposta 24 mesi — §6 della
  descrizione); campi annotati `@PersonalData`; tabella aggiunta a `exportData` e `purgeData` del contratto
  `FidelizzazioneDataContract`. **Nessun testo libero** in questa tabella: gli esiti sono un elenco chiuso e le
  regole sono dichiarate, non scritte a mano.
- **RT-9 — Registrazione eventi (§14).** `finestra di osservazione dichiarata (durata, regola)`, `esito valutato
  (esito, gruppo)`, `valutazione rimandata (fonte silente)`, con `tenant_id`, `app_id`, `user_id` e identificativo
  di correlazione, **senza** etichette di rapporti né nomi.
- **RT-10 — Prove (§11).** Unità sul calcolo del giorno di valutazione e sull'applicazione della regola di perdita;
  integrazione sul processo di valutazione con database effimero e migrazioni vere, tempo pilotato da un orologio
  iniettabile e **nessuna attesa a tempo**; isolamento fra due account.

## 4. Criteri di accettazione

**CA-1 — La finestra si dichiara prima**
- **Dato** un rapporto in fascia alta e un intervento in `bozza`
- **Quando** un utente `owner` lo conferma scegliendo la finestra di 90 giorni e la regola «disdetta registrata da
  SubGrove»
- **Allora** nasce un `EsitoDelRapporto` *ancora aperto*, con giorno di valutazione a 90 giorni dalla conferma,
  regola e gruppo `intervenuto` scritti sulla riga

**CA-2 — La finestra non si cambia dopo**
- **Dato** un esito *ancora aperto* con finestra di 90 giorni
- **Quando** si tenta di portarla a 365 giorni per via web o di programmazione
- **Allora** la richiesta è respinta con `409` e un messaggio che spiega che una finestra dichiarata non si modifica
  e che si può aprire una valutazione nuova; nulla è cambiato sulla riga esistente

**CA-3 — La valutazione avviene una volta sola e non si riscrive**
- **Dato** un esito il cui giorno di valutazione è arrivato, con la fonte richiesta aggiornata
- **Quando** il processo di valutazione gira, e poi gira di nuovo il giorno dopo
- **Allora** l'esito è `trattenuto` o `perso` secondo la regola congelata, e la seconda esecuzione **non** lo tocca

**CA-4 — Fonte muta: si rimanda, non si dichiara vittoria**
- **Dato** un esito il cui giorno di valutazione è arrivato, con la fonte della regola in silenzio oltre il proprio
  ritardo atteso
- **Quando** il processo di valutazione gira
- **Allora** l'esito resta *ancora aperto* con il contrassegno di attesa e il motivo mostrato a schermo, e **non**
  viene contato come `trattenuto` in nessun conteggio

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con rapporti ed esiti propri
- **Quando** un utente di `A` chiede gli esiti di un rapporto, forzando nella richiesta l'identificativo di un
  rapporto di `B`
- **Allora** riceve `404` e non vede alcun dato di `B`, e il processo di valutazione di `A` non tocca righe di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul calcolo del giorno di valutazione e sulla regola di perdita, e di **integrazione** sul
      processo di valutazione, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `esito_del_rapporto` e sul processo di valutazione;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-FIDELIZZAZIONE]` nasce con la storia `0030`, che chiude sul
      passo «esito»; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta la voce `da-coprire` con
      motivo («percorso di piattaforma non ancora creato») e storia proprietaria `0030`;
- [ ] **traduzioni** presenti in `en, it, fr, es, de`;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la voce `esito_del_rapporto`, campi annotati
      `@PersonalData`, tabella presente in `exportData` e `purgeData`;
- [ ] registro dei trattamenti rigenerato dal manifesto nello stesso commit;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato: elenco chiuso delle durate, elenco
      chiuso delle regole di perdita per fonte, scelta di rimandare la valutazione invece di dichiarare
      «trattenuto» con fonte muta;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento nuovo, l'esito entra in quelli della `0028`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove descrive la macchina a stati dell'intervento (§4.4 della descrizione).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` (intervento con conferma umana) | la finestra si dichiara **al momento della conferma**: senza conferma non c'è da cosa far partire la misura |
| storia `0011` (salute e ritardo delle fonti) | il rimando della valutazione con fonte muta si appoggia al ritardo atteso già calcolato lì |
| storia `0009` (il rapporto sorvegliato) | l'esito si attacca a un rapporto, che deve esistere e essere sorvegliato |
| punto aperto n. 9 della [descrizione](../application-description.md) (conservazione 24 mesi) | il termine di conservazione degli esiti è una proposta prudente, non un dato: lo chiude la revisione legale |

## 7. Fuori ambito

- il **gruppo di confronto** e la sua formazione: storia `0025`. Qui il campo `gruppo` esiste e vale `intervenuto`
  per gli esiti nati da un intervento;
- il **motivo di abbandono**: storia `0026`. Qui l'esito dice *come* è finita, non *perché*;
- il **rendiconto** che somma gli esiti in un periodo: storia `0027`;
- la valutazione di esiti su rapporti **mai** intervenuti in assenza di un gruppo di confronto: non serve a nulla
  senza un termine di paragone, e arriva con la `0025`;
- qualunque effetto verso il cliente finale a seguito dell'esito (un ringraziamento, una riconquista): è epica 04 e
  passa comunque dalla conferma umana.

## 8. Punti aperti

- **Le durate ammesse della finestra e i mesi delle regole di perdita** (30/90/180/365 giorni; «nessun documento per
  N mesi») sono una **convenzione dichiarata, non una stima**: la [descrizione](../application-description.md) al
  §2.7 scrive che non esistono misure validate per imprese non-software. Vanno rese modificabili per attività, come
  i pesi del punteggio (`0016`), oppure confermate come predefiniti. Chiude: **sviluppatore** — direzione di
  prodotto.
- **Se l'esito debba poter essere corretto a mano** quando il titolare *sa* che il cliente è andato via ma nessuna
  fonte lo registra. Correggere a mano un esito è il modo più rapido di rendere la misura inutile; non correggerlo
  mai è il modo più rapido di renderla falsa. La `0026` apre uno spiraglio (il motivo si registra a mano), ma
  l'**esito** qui resta automatico. Chiude: **sviluppatore** — direzione di prodotto.
