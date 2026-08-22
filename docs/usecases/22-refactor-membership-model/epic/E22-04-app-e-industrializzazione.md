# E22.4 — Dentro le applicazioni e industrializzazione

**Epica madre**: [Epica 22](E22-00-rifacimento-modello-appartenenza.md) · **Storie**: 0111, 0112, 0114, 0115, 0113, **0120**
**Stato**: 🟢 analisi scritta · **Ultimo aggiornamento**: 2026-08-22

## Obiettivo

Portare il modello **dentro** le applicazioni — dove i ruoli si sentono davvero — e renderlo il
comportamento **predefinito** di ogni applicazione futura, invece di una buona pratica che qualcuno
ricorderà. Poi migrare gli account esistenti senza che nessuno perda accesso, e coprire il tutto con
prove automatiche per ruolo.

## Perché questa sotto-epica esiste separata

Le prime tre sotto-epiche costruiscono un modello e lo mostrano nel backoffice. Ma se il modello si
fermasse lì, ogni applicazione continuerebbe a decidere per conto proprio chi può fare cosa — che è
esattamente il difetto di partenza, spostato di un metro. Qui si chiude il cerchio in cinque mosse:
**usarlo** (0111), **industrializzarlo** (0112), **togliere la categoria che il nuovo modello rende falsa**
(0114), **mettere al suo posto la distinzione che conta davvero** (0115) e **portarci il mondo esistente**
(0113).

## Le due storie aggiunte dopo la prima revisione

Lo sviluppatore ha chiesto di riverificare la categorizzazione **B2C/B2B** delle applicazioni, che aveva
introdotto proprio sulla differenza del modello utenti («private» contro «con altri utenti»). La verifica ha
dato un esito netto: il nuovo modello la rende **falsa** — l'accesso è di piattaforma, quindi qualunque
applicazione può avere più persone con ruoli — e il suo unico uso funzionale (scegliere i ruoli degli
endpoint generati) **scompare** col varco per applicazione. Da qui [UC 0114](../story/0114-ritiro-categoria-b2c-b2b.md),
che la ritira.

Ma l'intuizione dietro quella categoria era buona, e riguardava una domanda diversa da quella che il campo
poneva: **i dati sono del gruppo di lavoro o della persona che li ha creati?** Quella distinzione ha
conseguenze verificabili nel codice — cambia il filtro delle interrogazioni, non un'etichetta — ed è
[UC 0115](../story/0115-ambito-dati-applicazione.md).

Di quella storia, qui si fa la **dichiarazione**: l'ambito è una caratteristica che l'applicazione dichiara
alla nascita, e una **guardia** impedisce di rilasciare un'applicazione ad ambito `utente` finché il filtro
che lo fa rispettare non esiste. Il filtro stesso si costruisce con la **prima applicazione che ne ha
bisogno** — dopo questa epica, quando il lavoro si sposta sulle applicazioni — col progetto già pronto. È
la stessa logica del resto dell'epica: si decide adesso, mentre il modello è chiaro; si costruisce quando
c'è qualcuno che lo usa.

## Le decisioni portanti

**Ogni applicazione ha la sua schermata di gestione utenti** (storia 0111), con lo stesso aspetto e le
stesse regole in tutte: chi ha accesso, con quale ruolo, chi può aggiungere e chi può soltanto guardare.
La schermata è **generata** per le applicazioni nuove e aggiunta a mano alle due esistenti.

**Il Mini-CRM restituisce i suoi posti.** L'applicazione si era costruita una tabella di posti e un
varco propri, perché la piattaforma non offriva nulla. Ora l'offre: quel codice va **ritirato**, non
affiancato. Due contatori dello stesso concetto sono un difetto in attesa di manifestarsi, e in questo
caso si manifesterebbe come un cliente che paga due volte lo stesso posto.

**Chi genera un'applicazione risponde a una domanda in più** (storia 0112). Il copilota dei ruoli chiede
a chi crea l'applicazione: *quali sono le operazioni dispositive di questa applicazione? c'è qualcosa
che un `viewer` non deve vedere affatto, e non solo non modificare? l'`admin` di questa applicazione ha
poteri specifici oltre alla gestione degli utenti?* Le risposte finiscono in un documento
dell'applicazione e nei suoi collaudi. Senza questa domanda ogni applicazione nuova nascerebbe con tre
ruoli **dichiarati** e un comportamento **indifferenziato**: peggio che non averli, perché sembrerebbe
funzionare.

**Nessuno perde accesso il giorno del rilascio** (storia 0113). La migrazione concede a **ogni** utente
esistente l'accesso a **ogni** applicazione dell'account, traducendo il ruolo attuale: `owner` resta
`owner`; `admin` diventa `member` con ruolo `admin` su tutte; `member` diventa `member` con ruolo
`editor` su tutte. La scelta di `editor` (e non `viewer`) è deliberata: oggi un `member` **può**
modificare i dati delle applicazioni, e una migrazione non è il momento per togliere poteri di
soppiatto. Chi vuole stringere lo farà consapevolmente, dopo.

**I posti esistenti diventano gratuiti per gli account attivi.** Nessun cliente si sveglia con una
fattura più alta perché abbiamo cambiato modello: la storia 0113 prevede la rilevazione degli account
che superano la franchigia e la loro gestione come **caso di comunicazione commerciale**, non come
addebito automatico. È una decisione che vale denaro e che va confermata da chi decide i prezzi: la
storia la marca come punto aperto invece di risolverla d'autorità.

## La sesta storia, aggiunta dopo: il collaudo manuale di chiusura (UC 0120)

[UC 0120](../story/0120-guida-collaudo-manuale-epica.md) sta qui perché questa è la sotto-epica che
**chiude**, e perché la sua gemella è già dentro: 0113 porta la copertura **automatica** per ruolo
(`J-ROLES`, `J-SEATS`) e 0120 porta quella **manuale** — una guida unica, di soli passi visivi, ricavata
dalle guide di tutte le change dell'epica e riscritta per percorsi coerenti. Le due si eseguono in fila,
0120 subito dopo 0113, e insieme sono l'unico momento in cui l'epica viene guardata per intero.

Ha senso solo alla fine, e per la stessa ragione per cui il collaudo visivo delle singole storie è stato
**sospeso** (decisione del 22 agosto 2026,
[§9 dell'epica madre](E22-00-rifacimento-modello-appartenenza.md#9-il-collaudo-di-questa-epica-visivo-sospeso-unico-a-chiusura)):
in un rifacimento, gli stati intermedi non sono giudicabili a vista. È 0120, eseguendo la guida, a far
**decadere** quella sospensione.

## Come si vede che ha funzionato

- Un'applicazione generata da zero rispetta i tre ruoli **senza che nessuno scriva codice** di
  autorizzazione.
- Il collaudo di parità dello scaffolding va in rosso se un modello resta indietro sui ruoli.
- Dopo la migrazione, nessun cliente segnala accessi persi.
- Esistono percorsi di prova automatici che entrano nel prodotto come `owner`, `admin`, `editor` e
  `viewer` e verificano ciò che ognuno vede.
- Lo sviluppatore ha percorso il prodotto **una volta**, a epica chiusa, seguendo una guida sola: e ciò
  che ha trovato è stato discriminato fra «la guida è sbagliata» e «il prodotto è sbagliato», senza
  ammorbidire nessun passo.
