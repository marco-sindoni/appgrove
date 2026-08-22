# UC 0120 — Guida di collaudo manuale unica di fine epica

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.4 Dentro le applicazioni](../epic/E22-04-app-e-industrializzazione.md)
**Dipendenze**: **tutte** le storie dell'epica, e in particolare [UC 0113](0113-migrazione-account-e-copertura-e2e.md) (copertura automatica per ruolo), che deve essere **chiusa e verde** prima
**Posizione nell'ordine**: **ultima dell'epica**, dopo `0113`
**Origine**: decisione dello sviluppatore del 22 agosto 2026, dopo il collaudo manuale del lotto `0095`–`0099`
**Piano di lavoro**: nessuno separato — vedi la nota in coda al §10
**Ultimo aggiornamento**: 2026-08-22

## 1. Obiettivo / Scope

Ricavare dalle guide di collaudo delle change dell'epica **una sola** guida di collaudo manuale, e
**eseguirla** con lo sviluppatore sullo stato finale di `main`. Una sequenza coerente di percorsi, di
soli passi **manuali e visivi**, che copra tutto ciò che l'epica ha cambiato e che **sostituisca** la
rilettura di una ventina di guide sparse nelle cartelle delle change.

**Il problema che chiude.** Collaudare a vista una storia per volta, in un'epica di **rifacimento**,
significa giudicare la forma di stati intermedi che nessun utente vedrà mai: quando lo sviluppatore ha
guardato la schermata «Membri» dopo il lotto `0095`–`0099`, aveva davanti un mezzo passaggio — `0100`
aveva tolto il ruolo dall'elenco, `0111` deve ancora costruire il posto dove il ruolo si governa, `0107`
deve ancora decidere che cosa si vede per ruolo. Il giudizio su **forma, coerenza e sequenza** ha senso
solo su un insieme completo: prima è un giudizio su un cantiere.

Da qui la decisione gemella, registrata nel [documento dell'epica](../epic/E22-00-rifacimento-modello-appartenenza.md#9-il-collaudo-di-questa-epica-visivo-sospeso-unico-a-chiusura):
il **collaudo visivo delle singole storie è sospeso** fino alla fine dell'epica. Questa storia è il punto
in cui quel debito si paga — tutto in una volta, e una volta sola.

**Incluso**: la raccolta delle guide delle change dell'epica; l'estrazione dei soli passi manuali e
visivi; la **riscrittura per percorsi** (§4); l'allestimento iniziale unico; l'esecuzione con lo
sviluppatore; il trattamento degli scarti; la dichiarazione che la sospensione del collaudo visivo è
decaduta.

**Escluso**: i collaudi **automatici**, che sono di [UC 0113](0113-migrazione-account-e-copertura-e2e.md)
e che devono già essere verdi (questa storia non ne scrive né ne ripara); la **riscrittura** delle guide
delle singole change, che restano intatte come archivio della loro change; l'estensione della pratica
**alle altre epiche** — opzione esaminata e **scartata dallo sviluppatore**: è una storia di *questa*
epica, non una regola generale, e nulla di `CLAUDE.md` o delle skill viene toccato.

## 2. Attori & ruoli

- **Lo sviluppatore**: unico esecutore. È l'unica persona che può giudicare forma, coerenza e sequenza,
  ed è la ragione per cui la guida esiste.
- **L'agente**: raccoglie, estrae, riscrive, prepara l'allestimento, sta accanto durante l'esecuzione,
  registra gli esiti e apre i difetti trovati.
- **Nessun attore esterno**: tutto in locale, con il simulatore del pagamento e la posta locale.

## 3. Precondizioni

- **Tutte** le storie dell'epica sono in `main`, `0113` compresa, e `./run-tests.sh` intero è verde: una
  guida manuale eseguita su una suite rossa misura due cose insieme e non ne conclude nessuna.
- I percorsi `J-ROLES` e `J-SEATS` di `0113` sono verdi: sono la copertura **automatica** per ruolo, e
  ciò che loro provano la guida manuale **non** lo ripete.
- Le guide delle change dell'epica esistono in `changes/*/how-to-test.md` (dieci al 22 agosto 2026 —
  `0088`–`0092`, `0095`–`0099` — e altrettante in arrivo con le storie che restano).
- Lo stack locale si avvia (`./app-start.sh`), la posta locale raccoglie i messaggi, il simulatore del
  fornitore di pagamento funziona.

## 4. Flusso principale

1. **Raccolta.** Elencare le guide delle change dell'epica, in ordine di change, con la storia che
   ognuna implementa. L'elenco entra nella guida come **tabella di provenienza**: chi la legge deve poter
   risalire dal percorso alla guida originale, che è più dettagliata.
2. **Estrazione.** Da ogni guida tenere **solo** i passi manuali e visivi. Si buttano i comandi verso la
   banca dati, le chiamate alle interfacce di programmazione, le ispezioni di code, log e posta, le
   attese su una riga o un codice di stato: quelli le change li hanno **già eseguiti**, e la passata di
   fine lotto li ha **rieseguiti** contro lo stato finale di `main`. Ripeterli a mano è esattamente il
   lavoro che questa storia esiste per togliere.
3. **Riscrittura per percorsi.** La guida **non è la somma** delle guide: non un capitolo per change, ma
   un capitolo per **chi agisce e che cosa sta cercando di fare**. Un capitolo per change costringerebbe
   a rifare il login sei volte e a leggere sei volte lo stesso elenco; un capitolo per percorso racconta
   una storia, ed è dentro una storia che le incoerenze si vedono.
4. **Allestimento iniziale unico.** Un solo stato di partenza, dichiarato una volta in testa, valido per
   tutti i percorsi: quale conto, quante persone, quali applicazioni, quale listino in vigore, e come si
   riporta tutto al punto di partenza alla fine (le guide del lotto `0095`–`0099` hanno già imparato che
   una guida che non pulisce lascia indietro persone e inviti a ogni esecuzione).
5. **Esecuzione con lo sviluppatore.** Percorso per percorso, passo per passo. Ogni passo dice *che cosa
   fare* e *che cosa si deve vedere*, con le **etichette come si leggono a schermo** e non con i nomi
   tecnici.
6. **Trattamento degli scarti.** Per ogni scostamento si discrimina, come prescrive la regola della
   change `0093`: *la guida è sbagliata* → si corregge la guida; *il prodotto è sbagliato* → è un
   difetto, si corregge o si traccia. **Mai ammorbidire un passo per farlo combaciare con un difetto**:
   lo nasconderebbe due volte.
7. **Chiusura.** Esito registrato per percorso, con data e commit; i difetti aperti o corretti nominati;
   e la dichiarazione, nel documento dell'epica, che la **sospensione del collaudo visivo è decaduta**.

### I percorsi (la sostanza della guida)

Proposta, da rifinire sullo stato reale di `main` al momento dell'esecuzione. Ogni percorso è una
persona che prova a ottenere qualcosa, non un elenco di funzioni:

| # | Percorso | Chi agisce | Che cosa si guarda | Storie coperte |
|---|---|---|---|---|
| P1 | **Metto su il gruppo di lavoro** | owner | invita, legge il riquadro dei posti e il costo del prossimo, abilita le persone alle applicazioni con i tre ruoli, cambia un ruolo | 0100, 0103, 0111 |
| P2 | **Riduco il gruppo di lavoro** | owner | indica una persona per la cessazione, vede la stima, incontra il blocco delle aggiunte, annulla, torna a invitare | 0104 |
| P3 | **Entro come collaboratore** | invitato | accetta l'invito, vede il cruscotto senza leve, il menu con le sole applicazioni sue, il catalogo in sola lettura con «chiedi all'owner», «I miei dati» in forma ridotta | 0107, 0108, 0109, 0110 |
| P4 | **I tre ruoli dentro un'applicazione** | viewer, editor, admin | il `viewer` trova i comandi **spenti con una spiegazione**; l'`editor` opera; l'`admin` abilita una persona già esistente e le cambia ruolo, ma **non** invita | 0101, 0111 |
| P5 | **Vedo che cosa pago** | owner | i posti nelle righe di «Billing», lo storico, il prossimo rinnovo | 0106 |
| P6 | **Cambio le tariffe** | amministratore di piattaforma | nuova versione del listino con decorrenza dal ciclo successivo, e che cosa il cliente vede | 0105 |
| P7 | **Lo stesso prodotto su schermo stretto** | owner e collaboratore | le schermate dei percorsi precedenti su finestra da telefono | 0119 |
| P8 | **Una persona, due conti** | chi appartiene a due conti | il selettore del conto, l'invito a chi esiste già, la scelta all'accesso | 0116, 0117, 0118 |

La **tabella di corrispondenza storia → percorso** è parte della guida: serve a rendere visibile ciò che
**non** è coperto, che è l'unica cosa che una guida di collaudo non può permettersi di nascondere.

## 5. Flussi alternativi / edge / errori

- **Edge — un passo che in locale non si può provare** (il fornitore di pagamento vero, la posta vera
  fuori dalla casella locale): si **dichiara non eseguibile**, col motivo, e si rimanda all'ambiente di
  prova. Non si finge di averlo fatto e non si toglie dalla guida.
- **Edge — un percorso reso impossibile da una decisione presa in corsa**: si corregge il percorso e si
  annota che la guida della change d'origine è **superata** su quel punto. È l'invecchiamento che si
  intercetta solo eseguendo.
- **Edge — un difetto trovato a metà percorso**: si prosegue fino in fondo, poi si decide. Fermarsi al
  primo rosso fa perdere gli altri.
- **Errore — la suite automatica è rossa** all'inizio: non si comincia. Prima si rimette verde: una
  guida manuale non è il posto dove si scopre un regresso che una macchina sapeva già.
- **Trappola — la guida che diventa una firma.** Un elenco di caselle spuntate senza esiti scritti non
  prova nulla. Per ogni percorso si scrive **che cosa si è visto**, anche quando è quello che si
  attendeva.

## 6. Risorse & runbook

**Dove vive la guida**: `docs/usecases/22-refactor-membership-model/collaudo-manuale.md`, cioè **nella
cartella dell'epica** e non in quella della change. La ragione è precisa: le guide delle change sono
**fotografie** di un branch e file d'archivio per costruzione; questa è il collaudo di un'epica intera,
la si cerca dove sta l'epica, e se l'epica si riapre la si riesegue senza dover ricordare quale change
l'aveva prodotta. La change che la implementa la **produce e la esegue**, e il suo `how-to-test.md` non
la duplica: la richiama.

**Forma obbligatoria** (la stessa che `CLAUDE.md` pretende dalle guide, con una restrizione in più):

- **intestazione** con il commit di `main` su cui la guida è scritta e la data;
- **allestimento iniziale** unico e la sua pulizia finale;
- **un capitolo per percorso**, passi numerati, ognuno con *azione* e *che cosa si deve vedere*;
- **etichette come si leggono a schermo**, nella lingua dell'interfaccia usata per il collaudo;
- **soli passi manuali e visivi**: se in un passo compare un comando, quel passo è nel posto sbagliato —
  con **una** eccezione ammessa, i comandi dell'**allestimento** e della **pulizia**, che non sono
  verifiche;
- **tabella di provenienza** (percorso → guide delle change da cui viene) e **tabella di copertura**
  (storia → percorso, con le caselle vuote in evidenza);
- **spazio per l'esito** di ogni percorso.

**Che cosa la guida non abolisce**: le guide delle singole change restano dove sono, intatte. Sono
l'archivio della loro change e il materiale grezzo di questa. Cambia una cosa sola, ed è la cosa che
allo sviluppatore interessa: la guida unica diventa **l'unica che gli si chiede di eseguire**.

## 7. Dati toccati

**Nessuno** dal punto di vista del prodotto: la storia produce un documento ed esegue passi
d'interfaccia già coperti dai contratti esistenti.

**Dati personali**: nessun trattamento nuovo. Due regole per la guida in quanto **testo che resta nel
repository**: gli indirizzi di posta sono di prova (`@example.com`) e i nomi inventati; nessuna
schermata catturata deve contenere dati che non siano quelli dell'allestimento. Una guida di collaudo è
un documento pubblico del repository, e i dati di prova sono l'unica cosa che vi può stare.

## 8. Permessi & gate

- L'esecuzione avviene **in locale**, con conti di prova: nessun effetto verso l'esterno, nessun addebito
  vero (il fornitore di pagamento è simulato).
- I percorsi si eseguono **come i ruoli che descrivono**, non con scorciatoie amministrative: un collaudo
  che entra da una porta di servizio non prova la porta d'ingresso.
- **Invarianti di piattaforma**: nulla da difendere qui, perché nulla di eseguibile viene scritto.

## 9. Requisiti di test

Questa storia **non porta collaudi automatici nuovi**, e la ragione va detta invece di essere
sottintesa: automatizzare il collaudo manuale sarebbe la contraddizione della storia. Ciò che la
sorveglia è già in piedi:

- la **copertura automatica** di `0113` (`J-ROLES`, `J-SEATS`) e i percorsi di livello 2 delle singole
  storie: sono loro il presidio contro i **regressi**, e devono essere verdi **prima**;
- l'**esecuzione** della guida: un fallimento durante l'esecuzione è la prova, e una rilettura non lo è.
  Rileggere produce un giudizio, eseguire produce un fallimento;
- il **registro di copertura** resta la mappa di ciò che le macchine provano; la tabella di copertura
  della guida è la mappa di ciò che una persona ha guardato. Sono due mappe diverse e servono entrambe.

**Che cosa deve essere verde prima del commit della change**: `./run-tests.sh` intero, come per ogni
change; nessuna area nuova, perché nessun codice nuovo.

### Journey end-to-end di piattaforma

**Esenzione**, categoria `senza-superficie`. Motivo: la storia consegna un **documento di processo** —
una guida di collaudo manuale — e non ha superficie applicativa propria. Le superfici che la guida
percorre appartengono tutte ad altre storie, che le hanno già dichiarate; i percorsi automatici della
stessa materia (la stessa applicazione vista dai quattro ruoli, il ciclo di vita del posto) sono di
`0113`. Aggiungere qui un percorso significherebbe contarli due volte.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [CLAUDE.md](../../../../CLAUDE.md) — «La guida di collaudo si esegue, non si scrive
  soltanto» (change `0093`) e il processo di copertura (change `0094`) ·
  [UC 0113](0113-migrazione-account-e-copertura-e2e.md) (la copertura automatica che deve precederla) ·
  [UC 0119](0119-responsivita-backoffice.md) (il percorso P7) ·
  [docs/usecases/22-refactor-membership-model/README.md](../README.md) (l'elenco delle storie da coprire) ·
  le guide `changes/*/how-to-test.md` delle change dell'epica.
- **Definition of Done**:
  1. la guida esiste in `docs/usecases/22-refactor-membership-model/collaudo-manuale.md`, con
     intestazione, commit e data;
  2. contiene **soli** passi manuali e visivi (salvo allestimento e pulizia), organizzati per percorsi e
     non per change;
  3. la tabella di copertura storia → percorso è completa, con le caselle vuote **in evidenza** e
     motivate;
  4. la guida è stata **eseguita** con lo sviluppatore e l'esito di ogni percorso è scritto;
  5. ogni scostamento è stato discriminato (guida sbagliata → corretta; prodotto sbagliato → corretto o
     tracciato) e **nessun passo è stato ammorbidito**;
  6. le guide delle singole change sono **intatte**;
  7. il documento dell'epica dichiara **decaduta** la sospensione del collaudo visivo;
  8. `./run-tests.sh` intero verde.

**Nota — nessun piano di lavoro separato.** Come per [UC 0119](0119-responsivita-backoffice.md): le
ventuno storie nate dall'analisi della change `0087` hanno un gemello in [task/](../task/), queste due
sono state aggiunte dopo e i loro passi stanno nel drill-down (§4 e §6). Qui, in più, un piano di lavoro
sarebbe la terza copia della stessa sequenza.

## Punti aperti / decisioni differite

- **Non diventa una regola generale.** L'ipotesi di estendere «collaudo visivo sospeso + guida unica a
  chiusura» a **tutte** le epiche è stata esaminata e **scartata dallo sviluppatore**: si applica a
  questa epica, che è un rifacimento e i cui stati intermedi non sono giudicabili. `CLAUDE.md` e le
  skill non vengono toccate. Se dopo l'esecuzione il beneficio si vedrà, sarà una proposta da fare
  allora, con un'esperienza in mano invece che con un'intuizione.
- **Chi tiene viva la guida se l'epica si riapre.** Una guida nella cartella dell'epica invecchia più
  lentamente di una nella cartella di una change, ma non è immortale: nessun controllo meccanico la
  rende rossa. La proposta è la più semplice — si riesegue quando l'epica si riapre, e l'intestazione
  col commit dice quanto è vecchia. Un controllo automatico su un documento di prosa costerebbe più di
  quello che salva. Proprietario: questa storia.
- **I passi che il locale non può provare** (fornitore di pagamento vero, posta vera, e tutto ciò che
  vive solo in cloud) restano dichiarati non eseguibili finché l'ambiente di prova non è accesso. Il
  collegamento naturale è la prima accensione dell'ambiente di prova
  ([UC 0081](../../16-messa-in-cloud-golive/0081-smoke-reali-cloud-test.md)), che è dopo questa epica.
- **Se la guida debba esistere anche nelle cinque lingue** del prodotto: no, e non è un rimando ma una
  scelta — la guida è per lo sviluppatore, che legge l'italiano. La parità delle lingue
  dell'**interfaccia** è già sorvegliata dal suo controllo.
