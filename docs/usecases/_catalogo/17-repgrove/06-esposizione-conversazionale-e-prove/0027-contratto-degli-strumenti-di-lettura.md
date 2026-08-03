# 0027 — Contratto degli strumenti di lettura

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`, `0017`, `0020`, `0022`, `0025`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che alle otto di sera apre una chat invece dell'app
> voglio poter chiedere «com'è andata questa settimana, cosa mi è arrivato di brutto e ho invitato tutti quelli di
> ieri?» e ricevere una risposta vera
> così da governare la reputazione delle mie sedi senza aprire una schermata.

**Contesto.** Il catalogo pone a tutte le sessanta app il requisito di essere comandabili da una chat
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §12). Il livello conversazionale **non esiste ancora** nel
repository: è l'epica di piattaforma `12-ready-for-ai-mcp` (casi d'uso 0061-0066), scritta e non implementata. Il
dovere di questa storia è quindi dichiarare il **contratto** degli strumenti di sola lettura e tenerlo dentro il
servizio dell'app, versionato con esso — non costruire il server.

Le funzioni ci sono già tutte: le hanno introdotte le storie delle epiche precedenti, che nei propri requisiti
tecnici hanno rimandato qui la dichiarazione formale (0009, 0011, 0012, 0015, 0016, 0017, 0020, 0022, 0025). Qui si
scrive il contratto una volta sola, per tutte.

C'è una ragione, propria di questa app, per cui la lettura non è la parte banale: quello che gli strumenti
restituiscono **esce dal nostro perimetro** e finisce in un modello linguistico. Il testo di una recensione può
contenere dati sulla salute (descrizione §6, avviso sull'articolo 9) e il registro di equità contiene nomi e
recapiti di clienti finali. La minimizzazione non è qui una buona pratica: è la condizione perché questi strumenti
siano ammissibili.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara, in un descrittore versionato che vive nel repository dell'app, gli strumenti di
   **sola lettura**: `elenca_recensioni`, `punteggio_reputazione`, `recensioni_negative_da_gestire`,
   `stato_delle_richieste`, `dichiarazione_trasparenza`. Per ciascuno: nome stabile, descrizione in lingua naturale,
   schema dei parametri, schema del risultato, marcatura `lettura` e dichiarazione di idempotenza.
2. **RF-2** — Ogni strumento di lettura è **idempotente e privo di effetti**: nessuna scrittura, nessun invio,
   nessuna presa in carico, nemmeno come effetto collaterale di una registrazione di consultazione.
3. **RF-3** — I risultati sono **minimizzati per costruzione**: `elenca_recensioni` restituisce voto, momento,
   piattaforma, stato della risposta, identificativo e un **estratto** del testo entro un limite dichiarato; il
   testo completo si ottiene solo chiedendo esplicitamente una singola recensione, e la risposta dice che il testo
   può contenere dati di terzi. `stato_delle_richieste` restituisce **conteggi e motivi**, mai nomi e mai recapiti
   dei clienti finali.
4. **RF-4** — Ogni risultato porta con sé **da dove viene**: piattaforma d'origine, momento dell'ultima raccolta e
   se il dato è una fotografia o è vivo. Un assistente che non sa quanto è vecchio un numero lo presenta come
   attuale, e la responsabilità di quell'errore sarebbe nostra.
5. **RF-5** — Gli strumenti rispettano l'ambito dell'utente che ha delegato: chi vede una sola sede nell'interfaccia
   vede una sola sede anche da qui. Non esiste parametro che allarghi l'ambito.
6. **RF-6** — Il contratto è verificato da una prova automatica contro l'implementazione: uno strumento dichiarato e
   non implementato, o un risultato che non rispetta il proprio schema, fanno fallire la suite.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` arriva dal token delegato verificato (UC 0062), **mai** da
  un parametro dello strumento. Nessuno strumento accetta un identificativo di account, di sede o di recensione che
  non appartenga all'ambito del token: se arriva, si risponde «non trovato», non «non autorizzato», per non
  rivelare l'esistenza.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti riusano i servizi applicativi già esistenti dietro le
  rotte `/api/recensioni/v1/*`: nessuna via alternativa ai dati, nessuna interrogazione scritta apposta che scavalchi
  i controlli. Errori in `application/problem+json` tradotti nella forma d'errore del contratto.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. Le consultazioni non si memorizzano come dati applicativi: se
  servirà una traccia, è materia di sicurezza e tracciamento di piattaforma (UC 0065).
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova.
- **RT-5 — Cinque lingue (§4).** Le descrizioni degli strumenti e i messaggi d'errore del contratto esistono in
  `en, it, fr, es, de`: è testo che un assistente ripete all'utente, quindi è testo visibile a tutti gli effetti.
- **RT-6 — Varchi e quota (§6, §7).** Le chiamate attraversano la stessa catena delle rotte: `402` senza
  abbonamento attivo, `403` per ruolo insufficiente. Nessun consumo della metrica `sedi_monitorate`: leggere non
  aggiunge sedi. L'applicazione dei varchi alle chiamate dell'assistente è di piattaforma (UC 0064): qui si
  dichiara che il varco esiste e da dove si legge.
- **RT-7 — Esposizione conversazionale (§12).** È l'oggetto della storia. Il descrittore è **dentro** il servizio,
  versionato con esso; il server conversazionale è di piattaforma e non esiste (UC 0061-0063): il contratto si prova
  invocandolo direttamente.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo, ma una **destinazione nuova**: i dati escono verso un modello
  linguistico. Va scritto nel manifesto come destinazione, e va limitato a ciò che serve — estratto invece del testo
  intero, conteggi invece di elenchi di persone. Il divieto è esplicito: **nessuno strumento di lettura restituisce
  recapiti di clienti finali**, per nessuna combinazione di parametri.
- **RT-9 — Registrazione eventi (§14).** `strumento invocato` con nome, esito, numero di elementi restituiti,
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione. **Mai** i parametri liberi né i contenuti.

## 4. Criteri di accettazione

**CA-1 — Il contratto è completo e verificato**
- **Dato** il descrittore degli strumenti nel repository dell'app
- **Quando** gira la prova di conformità
- **Allora** ogni strumento dichiarato esiste, ogni risultato rispetta il proprio schema e nessuno strumento
  implementato è privo di dichiarazione

**CA-2 — Minimizzazione**
- **Dato** una recensione lunga con nome dell'autore
- **Quando** si invoca `elenca_recensioni`
- **Allora** si riceve un estratto entro il limite dichiarato e nessun recapito; il testo intero arriva solo con la
  richiesta esplicita della singola recensione, accompagnata dall'avvertenza

**CA-3 — Nessun recapito nel registro di equità**
- **Dato** un periodo con dieci clienti serviti, otto invitati e due esclusi
- **Quando** si invoca `stato_delle_richieste`
- **Allora** si ricevono i conteggi, i motivi delle esclusioni e la regola applicata — e **nessun nome, nessun
  indirizzo, nessun numero di telefono**

**CA-4 — Isolamento fra account**
- **Dato** due account con sedi e recensioni
- **Quando** con il token di `A` si invoca uno strumento passando l'identificativo di una sede di `B`
- **Allora** la risposta è «non trovato» e nessun dato di `B` compare

**CA-5 — Ambito dell'utente**
- **Dato** un utente `member` associato a una sola sede
- **Quando** invoca `punteggio_reputazione` senza specificare la sede
- **Allora** riceve i dati della sola sede che gli compete

**CA-6 — Nessun effetto**
- **Dato** una recensione negativa non presa in carico
- **Quando** si invoca `recensioni_negative_da_gestire` dieci volte
- **Allora** lo stato non cambia, nulla viene marcato come letto e nessuna riga viene scritta

**CA-7 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled` · **Quando** si invoca uno strumento · **Allora** l'esito è un
  errore che corrisponde a `402`, con l'indicazione del rimedio

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla minimizzazione (una prova per ciascuna regola: estratto, assenza di recapiti,
      conteggi) e di **integrazione** su ogni strumento con database effimero;
- [ ] prova di **isolamento fra account** su **ogni** strumento, non su uno di esempio;
- [ ] **prova end-to-end**: *rimando* alla storia 0030, che esercita il contratto nel percorso `[J-RECENSIONI]`, con
      voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** delle descrizioni e degli errori in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la destinazione «livello conversazionale» e con il limite di ciò che
      esce;
- [ ] **registro delle decisioni** compilato, con la scelta dell'estratto invece del testo intero e con il divieto
      di restituire recapiti;
- [ ] contratto degli **strumenti conversazionali**: è questa storia, e comprende la prova che lo verifica.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0016`, `0017`, `0020`, `0022`, `0025` | sono le funzioni che gli strumenti espongono: senza di loro non c'è niente da leggere |
| UC 0061-0063 (livello conversazionale di piattaforma) | non implementati: qui si dichiara il contratto e lo si prova invocandolo direttamente, senza server |
| UC 0064 (abilitazione e quota sulle chiamate dell'assistente) | non implementato: la catena dei varchi si dichiara, la sua applicazione centrale arriverà dopo |

## 7. Fuori ambito

- il server conversazionale, l'autenticazione delegata e il consenso: sono di piattaforma (UC 0061-0062);
- gli strumenti di **scrittura**, che hanno regole proprie — storia 0028;
- il rifiuto delle richieste che chiedono pratiche vietate — storia 0029;
- la conservazione delle conversazioni: non è materia dell'app.

## 8. Punti aperti

- **Quanto testo di recensione è lecito far uscire verso un modello linguistico** dipende dalla decisione
  sull'articolo 9 (descrizione §6) e dalle condizioni delle piattaforme (§11.2). La proposta prudente è l'estratto;
  se la decisione fosse «non conservare il testo», questi strumenti restituirebbero solo voto e momento e la storia
  si semplifica. **Decide lo sviluppatore.**
- **Se il fornitore del modello linguistico vada dichiarato come fornitore che tratta dati per nostro conto** anche
  quando il modello è scelto dall'utente finale fuori dalla piattaforma: è una domanda di piattaforma (UC 0062),
  non di questa app, ma qui se ne sente l'effetto.
