# 0030 — Percorso end-to-end dell'app

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0021`, `0024` — la lista di lavoro chiude il giro dell'intervento, l'esito lo misura; il percorso attraversa comunque tutte le storie da `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio una prova automatica che percorra RenewGrove dal collegamento di una fonte fino all'esito di un rapporto,
> sullo stack locale reale
> così da sapere che la catena regge davvero — segnale, punteggio, spiegazione, contestazione, intervento, conferma,
> misura — e non solo che ciascun pezzo passa le proprie prove.

**Contesto.** Questa app ha una particolarità che il suo percorso deve rispettare: **il fatto non nasce dentro
l'app**. Arriva pubblicato da un'altra applicazione sul canale a eventi, si aggrega su un rapporto, forma un
giudizio, viene contraddetto, produce un'azione preparata e infine una misura. Un percorso che partisse da righe
inserite a mano nel database salterebbe proprio i passi che questa app esiste per garantire: la copia del
`tenant_id` dall'evento (§4.2) e il passaggio umano obbligatorio (§4.4). Perciò il percorso comincia dal **segnale
pubblicato** — con i dati inventati della storia `0005` — e finisce su un esito valutato. Il registro di copertura è
sorvegliato da un controllo automatico: registro incoerente significa suite rossa
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §11).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-FIDELIZZAZIONE.spec.ts`, eseguito con Playwright
   **senza finestra** sullo stack locale reale, con **accesso programmatico** (nessun modulo di ingresso pilotato a
   mano), **nessuna attesa a tempo** e dati **inventati e deterministici** (etichette palesemente finte come
   «Panificio Aurora», indirizzi nel dominio riservato alle prove).
2. **RF-2** — Il percorso copre, in quest'ordine: attivazione dell'app e abilitazione dell'account →
   **collegamento di una fonte**, con l'elenco dei tipi di segnale mostrato prima → **arrivo di segnali** pubblicati
   sul canale a eventi → **nascita del rapporto sorvegliato** e **consumo di una unità** di `rapporti_sorvegliati` →
   **punteggio** calcolato con la sua **spiegazione** (contributi, pesi, verso, fatti datati) → **contestazione di un
   segnale** con **ricalcolo** verificato → **preparazione di un intervento** che resta in `bozza` → **conferma
   umana** che lo porta a `confermato` → comparsa nella **lista di lavoro** → registrazione dell'**esito** alla
   scadenza della finestra, con l'orologio del servizio pilotato dalla prova.
3. **RF-3** — Ogni test porta **l'etichetta del percorso in testa al titolo**: `test('[J-FIDELIZZAZIONE] …')`.
4. **RF-4** — Il percorso verifica quattro casi negativi, scelti perché sono i modi in cui questa app può fallire in
   silenzio: (a) un secondo account non vede nulla del primo, né in elenco né in spiegazione né nella lista di
   lavoro; (b) un intervento **non** passa da `bozza` a `confermato` per alcuna via automatica; (c) a quota esaurita
   la nascita di un nuovo rapporto sorvegliato risponde `429` e nulla viene creato; (d) con una fonte resa silente,
   il punteggio porta il contrassegno di incompletezza **prima** della cifra.
5. **RF-5** — Il percorso esercita **una** lettura e **una** scrittura del contratto degli strumenti (`0028`,
   `0029`) **chiamando il servizio direttamente**, perché il server conversazionale non esiste (UC 0061-0063). Il
   registro delle decisioni annota che quel passo andrà rifatto passando dal server quando esisterà.
6. **RF-6** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato con
   una voce per **ogni** storia dell'app: «coperta» per quelle che il percorso attraversa — comprese tutte quelle
   che avevano risposto *rimando* indicando `0030` come storia proprietaria — `da-coprire` con motivo e storia
   proprietaria per quelle che restano scoperte, e nessuna voce per quelle senza impatto.
7. **RF-7** — Il percorso è **ripetibile**: non dipende dall'ordine di esecuzione degli altri percorsi, non lascia
   dati che facciano fallire l'esecuzione successiva, e la sua durata non dipende da attese reali (le finestre di
   osservazione si attraversano pilotando l'orologio, non aspettando).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso usa **due** account, entrambi alimentati dalle stesse fonti
  simulate, e verifica che il secondo non veda nulla del primo. Qui l'invariante si prova su **due strade** — la
  lettura dalle schermate e la **scrittura dei segnali in arrivo dal canale a eventi** — che è la particolarità di
  questa app (§4.2 della descrizione).
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova: il percorso attraversa quelle esistenti.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. I segnali del percorso arrivano dal generatore di dati
  inventati della storia `0005`, pubblicati sul canale, mai scritti direttamente nello schema.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Il percorso verifica che il contrassegno di
  incompletezza e la fascia di rischio siano percepibili **senza affidarsi al colore**, e include il controllo
  automatico di accessibilità sulle schermate attraversate.
- **RT-5 — Cinque lingue (§4).** Il percorso gira nella lingua predefinita; una verifica separata controlla che
  nessuna chiave di traduzione compaia grezza, nelle cinque lingue, sulle schermate principali del modulo
  `fidelizzazione`.
- **RT-6 — Varchi e quota (§6, §7).** Il percorso verifica la catena dei varchi almeno una volta: accesso senza
  abilitazione (`402`), collegamento di una fonte da parte di un `member` (`403`), tetto di `rapporti_sorvegliati`
  raggiunto (`429`).
- **RT-7 — Esposizione conversazionale (§12).** Come al **RF-5**: contratto esercitato chiamando il servizio
  direttamente. Il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo e nessun dato reale**: tutte le etichette dei
  rapporti sono palesemente inventate, gli indirizzi stanno nel dominio riservato alle prove, e il percorso verifica
  che nessuna schermata e nessuna esportazione del primo account contenga dati del secondo.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo. Il percorso verifica che i registri prodotti durante
  l'esecuzione **non** contengano etichette di rapporti né contenuti di intervento.
- **RT-10 — Prove (§11).** `run-tests.sh` esegue il percorso nell'area dei percorsi di piattaforma; il controllo del
  registro di copertura (`tools/e2e-coverage`) resta verde.

## 4. Criteri di accettazione

**CA-1 — Il percorso passa**
- **Dato** lo stack locale avviato
- **Quando** si esegue `./run-tests.sh` nell'area dei percorsi di piattaforma
- **Allora** il percorso `[J-FIDELIZZAZIONE]` è verde dall'inizio alla fine

**CA-2 — La catena dal segnale all'esito regge**
- **Dato** una fonte simulata che pubblica sei segnali su un rapporto, con riferimenti d'origine noti
- **Quando** il percorso apre la spiegazione del punteggio, marca uno dei segnali come non pertinente, prepara e
  conferma un intervento, e porta l'orologio oltre la finestra di osservazione
- **Allora** la spiegazione elenca i sei fatti datati, il punteggio ricalcolato dopo la contestazione è diverso e il
  segnale escluso compare con la sua ragione, l'intervento risulta `confermato` con chi l'ha confermato, e l'esito è
  valutato secondo la regola congelata

**CA-3 — Da bozza non si esce senza una persona**
- **Dato** un intervento in `bozza` prodotto dal percorso
- **Quando** il percorso tenta ogni via disponibile per farlo avanzare senza il passaggio di conferma di un utente
  identificato
- **Allora** l'intervento resta in `bozza`, la lista di lavoro non lo mostra come da eseguire e nulla è consegnato

**CA-4 — Isolamento provato end-to-end, su lettura e su scrittura**
- **Dato** i due account del percorso, alimentati dalle stesse fonti simulate
- **Quando** il secondo apre elenchi, spiegazioni, lista di lavoro ed esportazioni
- **Allora** non vede alcun rapporto, segnale, punteggio o intervento del primo, e i segnali pubblicati con il
  `tenant_id` del primo restano sotto il primo

**CA-5 — Quota e incompletezza**
- **Dato** un account al tetto di `rapporti_sorvegliati` e una fonte resa silente oltre il ritardo atteso
- **Quando** arriva un segnale su un soggetto nuovo, e si apre un punteggio che dipende dalla fonte muta
- **Allora** la nascita del rapporto è respinta con `429` e il rimedio indicato, nulla è creato, e il punteggio
  mostra il contrassegno di incompletezza prima della cifra

**CA-6 — Registro coerente, e rosso quando non lo è**
- **Dato** il registro di copertura aggiornato
- **Quando** si esegue il controllo nell'area degli strumenti, e poi si rimuove un test lasciando la sua voce
- **Allora** nel primo caso è verde — ogni voce punta a un test che esiste e ogni test etichettato compare nel
  registro — e nel secondo la suite è rossa con l'indicazione della voce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` **completo**, non solo le aree toccate;
- [ ] il percorso `[J-FIDELIZZAZIONE]` non usa attese a tempo e non dipende dall'ordine di esecuzione degli altri
      percorsi;
- [ ] prova di **isolamento fra account** compresa nel percorso, su lettura **e** su scrittura dei segnali;
- [ ] **prova end-to-end**: *coprire ora* — è questa la storia che copre; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato con le voci di tutte e
      32 le storie dell'app, ciascuna «coperta», «rimandata» con motivo e storia proprietaria, oppure assente perché
      senza impatto;
- [ ] **traduzioni**: verifica che nessuna chiave compaia grezza nelle cinque lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che i dati del percorso siano inventati;
- [ ] **registro delle decisioni** compilato: percorso che parte dal canale a eventi e non dal database, orologio
      pilotato per attraversare la finestra di osservazione, fonte simulata, passo conversazionale da rifare quando
      il server esisterà;
- [ ] contratto degli **strumenti conversazionali**: esercitato dal percorso, una lettura e una scrittura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] `run-tests.sh` aggiornato nello stesso commit se cambia il comando dell'area.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0021` (lista di lavoro) | è il passo che chiude il giro dell'intervento nella via A (§4.3): senza, il percorso si ferma alla conferma |
| storia `0024` (esito del rapporto) | è l'ultimo passo del percorso, e quello che dà senso a tutti i precedenti |
| storia `0005` (avvio locale e dati di prova) | il generatore di segnali inventati è ciò che rende il percorso possibile senza le app sorgenti vere |
| tutte le storie da `0001` a `0029` | il percorso le attraversa |
| UC 0061-0063 (livello conversazionale) | **non implementati**: il passo conversazionale chiama il contratto direttamente e andrà rifatto |

## 7. Fuori ambito

- il percorso che attraversa **davvero** due app della suite (SubGrove che pubblica, RenewGrove che riceve):
  dipende dal contratto degli eventi di dominio, che oggi non esiste (punto aperto n. 2 della
  [descrizione](../application-description.md)). Qui la fonte è **simulata**, e va scritto nel registro;
- le prove di **carico e prestazione** sull'arrivo dei segnali: fuori perimetro;
- la **cancellazione** dei dati personali dentro il percorso: una prova che distrugge i propri dati di partenza è
  fragile; la copertura resta alle prove di integrazione della storia `0032`;
- le prove di **non-aggiramento** dell'isolamento (nessuna chiamata fra app, nessuna lettura fra schemi): sono
  prove di integrazione e strutturali, e stanno nella storia `0031`, che estende questo percorso con la sola
  matrice dei ruoli;
- il **gruppo di confronto** e il **rendiconto** su una finestra realmente conclusa: il percorso li tocca come
  lettura, non ne verifica la statistica.

## 8. Punti aperti

- **Quando esisterà una vera app sorgente**, il percorso va esteso a un giro di suite completo: SubGrove pubblica
  una rata non rientrata, RenewGrove la riceve, il punteggio si muove. È lavoro che appartiene al contratto degli
  eventi di dominio, non a questa storia. Chiude: **piattaforma**.
- **Come si attraversa una finestra di osservazione di mesi dentro una prova di pochi secondi.** La proposta è un
  orologio iniettabile nel servizio, pilotato dalla prova; l'alternativa — finestre di durata minuscola solo nel
  profilo di prova — renderebbe il percorso verde su una configurazione che nessun cliente userà mai. Chiude:
  **sviluppatore**, in fase di implementazione.
