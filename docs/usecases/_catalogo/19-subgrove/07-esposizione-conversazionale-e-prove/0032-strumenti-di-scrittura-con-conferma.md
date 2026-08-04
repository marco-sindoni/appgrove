# 0032 — Strumenti di scrittura con conferma

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0032` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0031`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che detta a voce «iscrivi Maria al piano trimestrale e sollecita la rata di Luca»
> voglio vedere **prima** cosa sta per succedere e confermarlo io
> così da usare la comodità della chat senza rischiare che qualcosa parta a mia insaputa verso una persona vera.

**Contesto.** Qui si applica la regola di sicurezza non negoziabile del catalogo: **lettura libera, scrittura con
bozza e conferma umana**. In SubGrove non è una precauzione generica: **tutti** gli strumenti di scrittura toccano
il rapporto fra il cliente e una **terza persona** — le si manda un messaggio, le si cambia il prezzo, le si toglie
il servizio (§7 della [descrizione](../application-description.md)). Tre di essi hanno effetti **verso l'esterno o
irreversibili** e vanno trattati con una severità in più: sollecitare un abbonato (parte un messaggio a una persona
che non è nostro utente e non si richiama indietro), disdire un abbonamento (chiude un rapporto contrattuale con
effetti economici), sospendere un abbonamento (toglie un servizio pagato). Per questi tre la conferma umana **non è
disattivabile** in nessuna configurazione: non esiste una modalità «fidati e vai».

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara sei strumenti di **scrittura**, ciascuno con nome stabile, schema dei parametri
   e schema del risultato: `crea_abbonamento`, `cambia_piano`, `registra_incasso`, `sollecita_scadenza`,
   `disdici_abbonamento`, `sospendi_abbonamento`.
2. **RF-2** — Ogni strumento di scrittura produce **prima una bozza**: un risultato che descrive in parole
   comprensibili cosa accadrà, con tutti i valori calcolati (importo, conguaglio, date, destinatario, effetto sullo
   stato) e un identificativo di bozza a **scadenza breve**. Nulla è ancora cambiato.
3. **RF-3** — L'esecuzione avviene **solo** presentando l'identificativo di bozza insieme a una **conferma umana
   esplicita**; una bozza scaduta o già usata non si esegue.
4. **RF-4** — Per i tre strumenti con effetti verso l'esterno o irreversibili — `sollecita_scadenza`,
   `disdici_abbonamento`, `sospendi_abbonamento` — la conferma è **obbligatoria e non disattivabile**, e la bozza
   dichiara in modo esplicito **cosa non si potrà annullare** (il messaggio già partito, il rapporto già chiuso).
5. **RF-5** — Nessuno strumento agisce **in blocco**: si sollecita una scadenza, si disdice un abbonamento. Una
   richiesta che ne coinvolgesse molti si rifiuta con la spiegazione, e l'operazione di massa resta
   nell'interfaccia, dove si vede cosa si sta selezionando.
6. **RF-6** — Ogni esecuzione passa dalle **stesse regole** dell'interfaccia: la macchina a stati della storia
   `0011`, il calcolo del conguaglio della `0014`, il tetto di sicurezza dei solleciti della `0021`. Uno strumento
   non è una scorciatoia: se una transizione è vietata a schermo, è vietata anche in chat.
7. **RF-7** — Ogni scrittura eseguita da un assistente resta **riconoscibile**: la cronologia dell'abbonamento
   mostra che l'azione è arrivata dal livello conversazionale, con l'utente che ha confermato e quando.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Contratto dentro il servizio, versionato con esso; ogni strumento
  marcato **scrittura**, e i tre del **RF-4** marcati anche **irreversibile**. Bozza e conferma sono parte del
  contratto, non una convenzione fra persone.
- **RT-2 — Isolamento fra account (§1).** Il `tenant_id` arriva solo dal token verificato della sessione delegata;
  la bozza è legata all'account che l'ha prodotta e non è utilizzabile da un altro, nemmeno conoscendone
  l'identificativo.
- **RT-3 — Persistenza (§8).** Migrazione `V22__bozza_strumento.sql` sullo schema `app_abbonati`: tabella
  `bozza_strumento` con `tenant_id`, chiave primaria UUID versione 7, strumento, argomenti normalizzati, esito
  previsto, scadenza, momento e autore della conferma, colonne di controllo e cancellazione logica. Le bozze
  scadute si ripuliscono da sole.
- **RT-4 — Idempotenza.** L'esecuzione è idempotente rispetto all'identificativo di bozza: ripetere la stessa
  conferma non produce due solleciti né due disdette. È il presidio contro il difetto tipico di questa superficie
  — la richiesta ripetuta perché la prima sembrava non aver risposto.
- **RT-5 — Varchi e quota (§6, §7).** La catena vale identica: `402` senza abilitazione, `403` per ruolo
  insufficiente, `429` a quota esaurita. `crea_abbonamento` **consuma** la metrica `abbonamenti_attivi` (natura
  `stock`) e a tetto raggiunto la bozza si rifiuta **prima** di essere prodotta, spiegando come rimediare: mostrare
  una bozza che non si potrà eseguire è una crudeltà inutile.
- **RT-6 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte esistenti; nessuna
  scrittura passa da una via propria della chat. Errori in `application/problem+json`.
- **RT-7 — Comunicazioni.** `sollecita_scadenza` produce la bozza **del messaggio effettivo**, composto con il
  renderer condiviso della piattaforma (change `0079`): si conferma ciò che si è letto, non un riassunto di ciò che
  partirà.
- **RT-8 — Dati personali (§10).** Le bozze contengono nomi e, per il sollecito, il **recapito** del destinatario:
  la tabella `bozza_strumento` va dichiarata nel manifesto in italiano e inglese, annotata, e inserita in
  `exportData` e `purgeData` (storia `0035`). Le bozze hanno vita breve, ma finché esistono sono dati di persone.
- **RT-9 — Registrazione eventi (§14).** `bozza prodotta (strumento)`, `bozza confermata (chi, quando)`,
  `bozza scaduta`, `esecuzione in blocco rifiutata`, con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, **senza** contenuti né recapiti.
- **RT-10 — Cinque lingue (§4).** I testi della bozza che l'utente legge — la descrizione dell'effetto e
  l'avvertenza di irreversibilità — passano dallo spazio-nomi `abbonati` e sono presenti in `en, it, fr, es, de`.
- **RT-11 — Prove (§11).** Prova che nessuno dei tre strumenti irreversibili si esegue senza conferma, scritta in
  modo che si rompa se qualcuno introduce una modalità automatica; prova di idempotenza; prova che una bozza di un
  account non si esegue in un altro; prova che il rifiuto in blocco funziona.

## 4. Criteri di accettazione

**CA-1 — Bozza prima, effetto poi**
- **Dato** la richiesta di sollecitare una scadenza scoperta
- **Quando** si invoca `sollecita_scadenza`
- **Allora** torna la bozza con il testo del messaggio, il destinatario indicato in forma minimizzata e
  l'avvertenza che una volta partito non si richiama; **nessun messaggio è stato inviato**

**CA-2 — Conferma obbligatoria e non disattivabile**
- **Dato** una configurazione qualunque dell'account
- **Quando** si tenta di eseguire `disdici_abbonamento` senza identificativo di bozza confermato
- **Allora** l'esecuzione è rifiutata, l'abbonamento non cambia stato, e non esiste alcuna impostazione che possa
  cambiare questo comportamento

**CA-3 — Idempotenza**
- **Dato** una bozza confermata di `registra_incasso`
- **Quando** la stessa conferma arriva due volte
- **Allora** l'incasso è registrato una volta sola e la seconda risposta lo dichiara

**CA-4 — Le regole restano quelle**
- **Dato** un abbonamento `cessato` · **Quando** si chiede `cambia_piano`
- **Allora** la bozza **non** viene prodotta e la risposta spiega che la macchina a stati non ammette quella
  transizione, con le stesse parole dell'interfaccia

**CA-5 — Niente azioni in blocco**
- **Dato** la richiesta «sollecita tutti quelli che non hanno pagato»
- **Quando** l'assistente prova a eseguirla
- **Allora** la richiesta è rifiutata con la spiegazione, e l'utente è indirizzato all'elenco nell'interfaccia

**CA-6 — Quota esaurita prima della bozza**
- **Dato** un account che ha raggiunto il tetto di `abbonamenti_attivi`
- **Quando** si chiede `crea_abbonamento`
- **Allora** si riceve `429` con il rimedio, **nessuna bozza** viene prodotta e nulla viene creato

**CA-7 — Tracciabilità**
- **Dato** una disdetta eseguita dalla chat · **Quando** si apre la cronologia dell'abbonamento
- **Allora** si legge che l'azione è arrivata dal livello conversazionale, chi l'ha confermata e quando

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `compliance`);
- [ ] prove di **unità** su bozza, scadenza della bozza e idempotenza; **integrazione** sugli strumenti con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su bozze ed esecuzioni;
- [ ] prova che i tre strumenti irreversibili **non** si eseguono senza conferma, scritta per rompersi se qualcuno
      aggiunge una modalità automatica;
- [ ] **prova end-to-end**: *rimando* — il livello conversazionale non è implementato (UC 0061-0066): voce
      `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo «server conversazionale
      di piattaforma assente» e storia proprietaria UC 0063;
- [ ] **traduzioni** dei testi di bozza e delle avvertenze in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la tabella delle bozze e i suoi campi;
- [ ] **registro delle decisioni** compilato: elenco degli strumenti, quali portano conferma obbligatoria e perché,
      divieto delle azioni in blocco, idempotenza;
- [ ] documentazione aggiornata dove descrive l'esposizione conversazionale dell'app.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0031` | contratto, sessione delegata e varchi degli strumenti sono impostati lì |
| storia `0011` | ogni scrittura passa dalla macchina a stati |
| storie `0014`, `0018`, `0021`, `0022`, `0024` | sono le operazioni che questi strumenti richiamano, con le loro regole |
| **UC 0061-0064** (livello conversazionale di piattaforma, non implementato) | server, sessione delegata, consenso e applicazione dei varchi |

## 7. Fuori ambito

- gli strumenti di **lettura**: storia `0031`;
- l'invio di comunicazioni **commerciali** agli abbonati: non è di questa app (è mestiere di **16 ReachGrove**), e
  un sollecito di pagamento non è una comunicazione commerciale;
- qualunque strumento che disponga un **pagamento**: **mai** — sarebbe avvio di un pagamento, cioè il confine che
  il §5.2 della [descrizione](../application-description.md) esclude;
- l'esecuzione automatica programmata («ogni lunedì sollecita»): le automazioni dell'app sono le lavorazioni delle
  storie `0012`, `0021` e `0022`, che hanno le proprie regole e i propri freni; farle rifare da un assistente
  aggiungerebbe un secondo automatismo non sorvegliato.

## 8. Punti aperti

**Chi è «la persona che conferma».** Il modello di conferma dipende dalla sessione delegata di piattaforma (UC
0062): serve sapere che la conferma arriva da un essere umano identificato e con il ruolo giusto, non dallo stesso
assistente che ha prodotto la bozza. Finché quella parte non esiste, il contratto lo **richiede** ma non lo può
verificare. Chiude: **piattaforma**, con la UC 0062.

**Durata della bozza.** Troppo breve e la conferma arriva sempre tardi; troppo lunga e si conferma qualcosa
calcolato su uno stato che nel frattempo è cambiato — il conguaglio di ieri su un piano cambiato stanotte.
**Proposta**: durata di pochi minuti e **ricalcolo di controllo** alla conferma, che rifiuta se l'esito previsto non
coincide più. Chiude: lo sviluppatore.
