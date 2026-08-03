# 0019 — Importazione degli esiti da file

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che a fine mese scarica dalla banca l'elenco degli addebiti andati a buon fine
> voglio caricare quel file e chiudere in un colpo tutte le scadenze rientrate
> così da non passare due ore a spuntare centoventi righe una per una.

**Contesto.** È la storia che rende sopportabile la postura «non incassiamo». Un cliente con centoventi
abbonamenti riceve dalla banca o dal proprio fornitore un elenco di esiti — chi ha pagato, chi è tornato indietro
e perché — e senza questa storia dovrebbe ribatterlo a mano nella `0018`. Il lavoro vero non è leggere il file:
è **riconciliare**, cioè capire quale riga del file corrisponde a quale scadenza. Ed è per questo che la storia
ha due presidi non negoziabili: l'**anteprima prima di applicare**, e la gestione esplicita delle righe che non
si riconoscono. Un'importazione che applica in silenzio e poi dice «fatto» è peggio del lavoro a mano, perché
l'errore è invisibile.

## 2. Requisiti funzionali

1. **RF-1** — Si carica un file tabellare e si dice, una volta per formato, **quale colonna è cosa**
   (riferimento, importo, data, esito, motivo); la corrispondenza si **ricorda** per le importazioni successive.
2. **RF-2** — Prima di applicare, l'app mostra un'**anteprima**: quante righe si abbinano a una scadenza, quante
   non si abbinano, quante sono già state importate, e il totale che verrà registrato.
3. **RF-3** — L'abbinamento cerca, in ordine: il riferimento della scadenza, il riferimento
   dell'autorizzazione all'addebito, la coppia abbonato + importo + periodo. Le righe che non si abbinano **non**
   si applicano: finiscono in un elenco di scarti con il motivo.
4. **RF-4** — L'importazione è **idempotente**: caricare due volte lo stesso file non registra due volte lo
   stesso incasso, e la chiave è il riferimento dell'operazione.
5. **RF-5** — Applicata l'importazione, resta un **riepilogo** consultabile: quando, chi, quante righe applicate,
   quante scartate, con la possibilità di scaricare gli scarti per lavorarli a mano.
6. **RF-6** — Un'importazione si può **annullare in blocco** entro la stessa giornata, riportando le scadenze
   coinvolte allo stato precedente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il file appartiene all'account del token verificato; nessun
  abbinamento può toccare scadenze di un altro account, nemmeno se il riferimento coincidesse.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/abbonati/v1/importazioni` (carica e prepara),
  `POST /api/abbonati/v1/importazioni/{id}/applica`, `DELETE .../{id}` (annulla),
  `GET .../{id}/scarti`; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V14__importazione.sql`: tabelle `importazione` e `riga_importazione`
  con `tenant_id`, colonne di controllo, e **vincolo di unicità** sul riferimento dell'operazione, che è il
  presidio contro il doppio caricamento.
- **RT-4 — Modulo frontend (§3, §5).** Procedura in tre passi (carica → verifica l'anteprima → applica) con la
  possibilità di tornare indietro; elenco degli scarti scaricabile; solo token del sistema di design.
- **RT-5 — Cinque lingue (§4).** Etichette dei passi, motivi di scarto e riepilogo in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** Nessun consumo di quota. Con abbonamento di piattaforma non attivo, `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: caricare un file da una chat non ha senso, e
  applicare in blocco senza vedere l'anteprima è precisamente ciò che la storia vuole impedire.
- **RT-8 — Dati personali (§10).** Il file caricato può contenere nomi e riferimenti bancari messi lì dalla
  banca: **non** si conserva il file originale oltre l'applicazione, e le righe conservate portano solo ciò che
  serve (riferimento, importo, data, esito). Va dichiarato nel manifesto, con la conservazione dichiarata breve.
- **RT-9 — Registrazione eventi (§14).** `importazione caricata (righe)`, `importazione applicata (applicate,
  scartate)`, `importazione annullata`, con `tenant_id`, `app_id`, `user_id` e correlazione, senza contenuti.
- **RT-10 — Prove (§11).** Prove con file volutamente sporchi: colonne mancanti, importi con separatori diversi,
  date in formati diversi, righe duplicate. È il genere di prova che si scrive una volta e ripaga per anni.

## 4. Criteri di accettazione

**CA-1 — Importazione pulita**
- **Dato** un file con 100 righe che si abbinano tutte
- **Quando** l'utente carica, vede «100 abbinate, 0 scartate, totale 3.900 €» e applica
- **Allora** le 100 scadenze risultano incassate e il riepilogo resta consultabile

**CA-2 — Righe non abbinate**
- **Dato** un file con 100 righe di cui 7 non corrispondono ad alcuna scadenza
- **Quando** l'utente applica
- **Allora** vengono applicate 93 righe, le 7 restano negli scarti con il motivo e sono scaricabili

**CA-3 — Doppio caricamento**
- **Dato** un file già applicato · **Quando** lo si ricarica
- **Allora** l'anteprima dice «100 già importate, 0 nuove» e l'applicazione non registra nulla

**CA-4 — Annullamento**
- **Dato** un'importazione applicata poco fa · **Quando** l'utente la annulla
- **Allora** le scadenze coinvolte tornano allo stato precedente e il riepilogo lo registra

**CA-5 — Isolamento fra account**
- **Dato** un file con riferimenti che esistono anche nell'account `B` · **Quando** lo carica l'account `A`
- **Allora** quelle righe risultano non abbinate: non si attraversa il confine dell'account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sull'abbinamento e sull'idempotenza; **integrazione** sul percorso completo, con file
      sporchi;
- [ ] prova di **isolamento fra account** sull'abbinamento;
- [ ] **prova end-to-end**: *rimando* — voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml), storia proprietaria `0033`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: righe importate e conservazione breve del caricato;
- [ ] **registro delle decisioni** compilato: ordine di abbinamento, anteprima obbligatoria, file originale non
      conservato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` | l'importazione registra gli stessi incassi, per un'altra via |
| storia `0017` | il riferimento dell'autorizzazione è uno dei criteri di abbinamento |

## 7. Fuori ambito

- la lettura automatica senza file, direttamente dal fornitore: storia `0020`;
- l'importazione dell'**anagrafica** da foglio di calcolo: fuori dal primo giro (punto aperto della storia
  `0005`), ma riuserà questo impianto di anteprima e scarti;
- il riconoscimento automatico del formato di ciascuna banca: qui la corrispondenza delle colonne la dichiara
  l'utente, una volta.

## 8. Punti aperti

**Formati preconfezionati.** Sarebbe comodo riconoscere da soli i formati delle banche più diffuse, evitando
all'utente la mappatura iniziale. È però un impegno di manutenzione permanente (i formati cambiano) per un
guadagno una tantum. **Proposta**: nessun formato preconfezionato nel primo giro; se ne aggiungeranno solo se
molti clienti caricheranno lo stesso. Chiude: lo sviluppatore, con i dati d'uso.
