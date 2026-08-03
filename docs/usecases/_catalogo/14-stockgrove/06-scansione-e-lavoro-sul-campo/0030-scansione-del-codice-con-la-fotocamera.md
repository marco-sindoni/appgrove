# 0030 — Scansione del codice con la fotocamera

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 06 — Scansione e lavoro sul campo
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto al magazzino
> voglio inquadrare con il telefono il codice stampato su una scatola e arrivare subito all'articolo giusto
> così da non dover cercare a mano fra quattrocento righe mentre ho le mani occupate davanti allo scaffale.

**Contesto.** Oggi, senza questa storia, l'unico modo di raggiungere un articolo è cercarlo per descrizione o per
codice interno da una tastiera: è il passaggio che, in piedi davanti a un bancale, fa abbandonare il programma e
tornare al foglio di calcolo. L'analisi in rete lo conferma come aspettativa numero uno del segmento («sapere
quanto ce n'è, subito, dal telefono», descrizione dell'applicazione §2.5). È il momento giusto per farla adesso e
non prima perché la risoluzione del codice ha bisogno dei codici multipli della storia `0007` e dei depositi della
`0008`: senza quelli non c'è niente su cui atterrare.

**La promessa va dimensionata con onestà.** La guida al lettore di codici a barre letta in analisi (descrizione
§2.6, fonte 8) dice tre cose che questa storia accetta invece di nascondere: la fotocamera di un telefono impiega
**uno o due secondi di messa a fuoco per lettura**, **soffre la scarsa luce**, e **non legge alcuni codici lineari
vecchi o rovinati**. Sotto una cinquantina di letture al giorno basta il telefono; sopra serve un lettore dedicato.
Da qui discendono due vincoli di progetto: la **via manuale resta sempre a un tocco di distanza** (non è un
ripiego, è la via alternativa dichiarata) e i **lettori esterni che si comportano da tastiera** devono funzionare
senza alcuna configurazione. Nel materiale di vendita non si mostra mai una persona che scansiona cento pezzi al
minuto con un telefono: sarebbe una promessa che il dominio non regge.

## 2. Requisiti funzionali

1. **RF-1** — Dalle sezioni `giacenze`, `articoli` e `movimenti` è raggiungibile un lettore di codici che usa la
   fotocamera del dispositivo tramite l'interfaccia del browser, senza alcuna applicazione da installare; il
   permesso alla fotocamera è chiesto al primo uso e il suo rifiuto non blocca la funzione (si resta sulla via
   manuale).
2. **RF-2** — Il lettore riconosce i codici a barre lineari di uso comune sulla merce e i codici a due dimensioni
   (codice QR); a lettura riuscita il codice grezzo è mostrato prima di essere risolto, così che chi legge veda
   cosa ha letto.
3. **RF-3** — Il codice letto viene risolto sull'articolo passando dai codici della storia `0007`: si cercano prima
   i codici interni dell'account, poi i codici GTIN registrati; l'esito è **un** articolo, con la sua giacenza per
   deposito.
4. **RF-4** — La **via manuale** è sempre disponibile e produce lo stesso esito: un campo in cui digitare o
   incollare il codice, che accetta anche l'invio automatico dei lettori esterni collegati come tastiera (lettura
   seguita da un ritorno a capo), senza configurazione né modalità speciale.
5. **RF-5** — Se il codice non corrisponde a nessun articolo, la risposta non è un vicolo cieco: si offre di
   **collegarlo a un articolo esistente** (ricerca per descrizione o codice interno) oppure di **creare un articolo
   nuovo** con quel codice già valorizzato.
6. **RF-6** — Se lo stesso codice risulta collegato a più di un articolo — situazione possibile su dati importati —
   l'esito è la lista dei candidati con il codice interno e la descrizione, e la scelta è di chi legge; il sistema
   non ne indovina uno.
7. **RF-7** — La schermata dichiara i limiti d'uso in una nota leggibile: uno o due secondi per lettura, difficoltà
   con poca luce e con i codici rovinati, e l'indicazione di usare un lettore dedicato oltre le cinquanta letture
   al giorno.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La ricerca per codice filtra per `tenant_id` preso dal token verificato;
  un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Un codice GTIN identico
  registrato da due account risolve, per ciascuno, **solo** sul proprio articolo. Prova di isolamento fra due
  account sulla rotta di ricerca.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/magazzino/v1/articoli/ricerca-per-codice?codice=…`
  che restituisce zero, uno o più articoli con la giacenza per deposito; oggetti di trasferimento al bordo;
  parametro validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** **Nessuna migrazione nuova**: i codici stanno nella tabella della storia `0007` e
  le giacenze nella proiezione della `0013`. Va aggiunto l'indice di ricerca sul codice normalizzato, se la `0007`
  non lo ha già previsto.
- **RT-4 — Modulo frontend (§3, §5).** Il lettore è un percorso **dentro le sezioni esistenti** (`giacenze`,
  `articoli`, `movimenti`), non una sezione nuova del manifesto del modulo `magazzino`: si apre a tutta pagina, è
  pensato per lo schermo stretto del telefono, usa i soli token del sistema di design e funziona in tema chiaro e
  scuro. La decodifica del codice avviene **sul dispositivo**, con la libreria già presente nel pacchetto del
  frontend: nessuna risorsa caricata dalla rete al momento dell'uso.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — compresi il testo del permesso negato, la nota sui
  limiti d'uso e i messaggi di codice non trovato — passano dallo spazio-nomi `magazzino` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** **La scansione non consuma quota e non risponde mai `429`**: leggere un
  codice non aggiunge nulla all'inventario. L'unico varco di quota che si può incontrare da qui è quello della
  **creazione di un articolo nuovo** (RF-5): se il tetto `articoli_gestiti` (natura `stock`) è raggiunto, la
  creazione risponde `429` con un messaggio che dice quanti articoli attivi ci sono, qual è il tetto del piano e
  che si può archiviarne uno o passare di piano — e la lettura appena fatta resta a schermo, non si perde. Con
  abbonamento `canceled` la rotta risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** La ricerca per codice alimenta lo strumento di **lettura**
  `trova_articolo(testo_o_codice) → articolo, ubicazione, giacenza per deposito`, il cui contratto è dichiarato
  nella storia `0034`; qui si garantisce che il servizio esponga la stessa risoluzione a entrambe le vie. Nessuno
  strumento di scrittura in questa storia. Il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** In particolare: **nessuna immagine della
  fotocamera viene inviata al servizio né conservata da nessuna parte** — la decodifica avviene sul dispositivo e
  verso il servizio viaggia solo la stringa del codice. L'unico dato di persone che questa storia sfiora è
  l'autore, già dichiarato nel manifesto dalla storia `0010`; qui non si scrive nulla, quindi non si registra
  nemmeno quello.
- **RT-9 — Registrazione eventi (§14).** L'evento `codice non risolto` è registrato con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, **senza** la stringa del codice letto (potrebbe comparire in un
  codice QR interno del cliente) e senza alcun contenuto. Le letture riuscite non si registrano una per una: non
  serve a diagnosticare e produrrebbe un conteggio per persona, che questa app non vuole avere (descrizione §6).

## 4. Criteri di accettazione

**CA-1 — Dal codice all'articolo**
- **Dato** un utente autenticato di un account abilitato, e un articolo con codice interno `RIC-0042` e giacenza 7
  nel deposito «Magazzino»
- **Quando** legge con la fotocamera il codice `RIC-0042`
- **Allora** vede la scheda dell'articolo con la descrizione e la giacenza 7 sul deposito «Magazzino», e il codice
  grezzo letto è mostrato sopra il risultato

**CA-2 — Via manuale e lettore esterno equivalenti**
- **Dato** lo stesso articolo e un utente che ha negato il permesso alla fotocamera
- **Quando** digita `RIC-0042` nel campo manuale, oppure lo invia con un lettore esterno collegato come tastiera
  (codice seguito da ritorno a capo)
- **Allora** ottiene **lo stesso** risultato di CA-1, senza alcuna configurazione preliminare e senza messaggi di
  errore sul permesso negato

**CA-3 — Codice sconosciuto**
- **Dato** un codice `8012345678905` non registrato su nessun articolo dell'account
- **Quando** lo si legge
- **Allora** compare la scelta fra «collega a un articolo esistente» e «crea un articolo nuovo», con il codice già
  valorizzato nel modulo di creazione, e nessun articolo viene creato finché non si conferma

**CA-4 — Creazione bloccata dal tetto del piano**
- **Dato** un account che ha raggiunto il tetto di `articoli_gestiti` del proprio piano
- **Quando** da un codice sconosciuto tenta di creare l'articolo nuovo
- **Allora** riceve `429` in `application/problem+json` con il numero di articoli attivi, il tetto del piano e
  l'indicazione di archiviare un articolo o passare di piano; nulla viene creato e la lettura resta a schermo

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che hanno registrato **lo stesso** codice GTIN su due articoli diversi
- **Quando** un utente di `A` cerca quel codice
- **Allora** ottiene solo l'articolo di `A`, anche forzando l'identificativo dell'account `B` nei parametri della
  richiesta

**CA-6 — Codice ambiguo**
- **Dato** un codice collegato a due articoli dello stesso account, per un'importazione imperfetta
- **Quando** lo si legge
- **Allora** compaiono entrambi i candidati con codice interno e descrizione, la scelta è dell'utente e il sistema
  non ne preseleziona nessuno

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla normalizzazione e risoluzione del codice (interno prima del GTIN, esito multiplo) e
      di **integrazione** sulla rotta di ricerca, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla ricerca per codice, con lo stesso GTIN su due account;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` è di proprietà della storia `0036`, che vi
      aggiunge il passo «trova l'articolo dal codice»; la voce nel registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) si scrive lì. Motivo del rimando:
      guidare la fotocamera in una prova automatica non è praticabile, mentre la via manuale — che produce lo
      stesso esito (CA-2) — è coperta dal percorso;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresa la nota sui limiti d'uso;
- [ ] **manifesto dei dati**: nessuna voce nuova, e verifica esplicita che nessuna immagine venga trasmessa o
      conservata;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la scelta di decodificare sul
      dispositivo e di tenere la via manuale come alternativa dichiarata;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto qui; la risoluzione alimenta
      `trova_articolo` della storia `0034`;
- [ ] verifica manuale su schermo stretto, in tema chiaro e scuro, con permesso alla fotocamera concesso e negato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | L'anagrafica degli articoli deve esistere per potervi atterrare |
| `0007` | I codici interni e i codici GTIN sono ciò su cui la lettura si risolve |
| `0008` | La giacenza mostrata è per deposito: i depositi devono esistere |
| `0013` | La proiezione della giacenza fornisce il numero mostrato accanto all'articolo |

## 7. Fuori ambito

- **Registrare un movimento dalla lettura**: questa storia porta all'articolo e si ferma lì; il movimento in due
  tocchi è della storia `0031`.
- **Lavorare senza rete**: la coda delle letture e l'invio idempotente sono della storia `0032`.
- **Stampare un'etichetta** per la merce che un codice non ce l'ha: storia `0033`.
- **Generare un codice GTIN**: non si fa mai, in nessuna storia — il prefisso è noleggiato a GS1 (descrizione
  §2.3 punto 4).
- **Riconoscere il prodotto da una fotografia** (senza codice): non è nel perimetro dell'app.

## 8. Punti aperti

- **Quali codici lineari sostenere davvero.** La scelta dei formati riconosciuti dipende dalla libreria già
  presente nel pacchetto del frontend, che al momento della stesura non è stata verificata: se non ne esiste una,
  l'aggiunta di una dipendenza nuova è una decisione dello sviluppatore, e va presa sapendo che nessuna risorsa
  può essere caricata dalla rete a runtime.
- **Nessun conteggio delle letture per persona.** È una conseguenza vincolante del §6 della descrizione
  dell'applicazione (art. 4 della legge 300/1970): non si costruiscono indicatori di produttività per operatore.
  Se in futuro qualcuno chiedesse «chi ha scansionato di più», la risposta corretta è no, e non spetta a questa
  storia riaprirla.
