# 0008 — Collegamento e revoca di una fonte

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 02 — Arrivo dei segnali dalle altre app
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta per far entrare in RenewGrove i fatti dei miei clienti presi dalla fatturazione
> voglio vedere prima, in un elenco chiuso e in parole comprensibili, che cosa entrerà — e poter tornare indietro
> sapendo esattamente che cosa perdo
> così da decidere con cognizione invece che spuntando una casella.

**Contesto.** Il consumatore scarta i segnali delle fonti non collegate (`0007`): manca il collegamento. Questa
storia costruisce il **consenso dell'account**, che nel disegno del §4.2 della
[descrizione](../application-description.md) è il secondo dei due cancelli — il primo è l'abilitazione di
piattaforma. Due cose la rendono diversa da un normale interruttore. La prima: **non si collega al buio**. Prima di
attivare, l'interfaccia mostra l'elenco chiuso dei tipi di segnale che quella fonte dichiara, con il loro
significato: è il momento in cui il titolare capisce che «pagamento in ritardo» significa giorni di ritardo e non
importi. La seconda: **la revoca è distruttiva**, e quindi va detta prima. Una revoca che cancella in silenzio è il
modo più rapido di far sparire uno storico che serviva a misurare l'efficacia (epica 05).

## 2. Requisiti funzionali

1. **RF-1** — Fra le fonti collegabili compaiono **solo** le applicazioni a cui l'account è abilitato **e** che
   sanno pubblicare segnali. Un'app abilitata ma senza tipi di segnale dichiarati non compare; un'app che
   pubblicherebbe ma a cui l'account non è abilitato nemmeno.
2. **RF-2** — Il collegamento e la revoca sono dati da un utente con ruolo `owner` o `admin`; un `member` vede
   l'elenco delle fonti in sola lettura e riceve `403` se tenta di collegare o revocare.
3. **RF-3** — Prima del collegamento l'interfaccia mostra **l'elenco chiuso dei tipi di segnale** di quella fonte,
   ciascuno con il proprio significato e la propria unità: nessun collegamento al buio. Il collegamento si conferma
   solo dopo che l'elenco è stato mostrato.
4. **RF-4** — Una fonte sta in uno di tre stati: `collegata` (i segnali entrano), `sospesa` (i segnali vengono
   scartati e contati, lo storico resta), `scollegata` (nessun segnale, nessuno storico).
5. **RF-5** — La revoca è **distruttiva e informata**: prima di eseguirla il sistema dice **quanti segnali verranno
   cancellati** e **quali punteggi smetteranno di essere calcolabili**; alla conferma cancella **fisicamente** i
   segnali di quella fonte e lascia una riga di prova nel registro delle purghe.
6. **RF-6** — Dopo la revoca il punteggio di un rapporto che dipendeva da quella fonte **non mostra un numero più
   piccolo**: mostra «non calcolabile — fonte *X* non collegata». Un punteggio che scende perché sono spariti dei
   fatti sarebbe una bugia.
7. **RF-7** — La sospensione è reversibile e non cancella nulla: serve al caso «non voglio più questi segnali per
   ora», che è diverso da «non li voglio più affatto».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le fonti sono per account: ogni lettura e scrittura di `fonte` filtra per
  `tenant_id` preso dal token verificato, e un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato.
  Collegare una fonte in un account non collega nulla in un altro, nemmeno per la stessa app d'origine.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/fidelizzazione/v1/fonti`,
  `POST /api/fidelizzazione/v1/fonti/{app}/collegamento`, `POST /api/fidelizzazione/v1/fonti/{app}/sospensione`,
  `DELETE /api/fidelizzazione/v1/fonti/{app}/collegamento`; corpo validato; errori in `application/problem+json`
  con codice stabile per «ruolo insufficiente» e per «app non abilitata»; definizione OpenAPI aggiornata nello
  stesso commit. Nessuna chiamata sincrona verso l'app d'origine: l'elenco dei tipi di segnale arriva dalla
  dichiarazione del contratto (`0006`), non da una domanda.
- **RT-3 — Modulo frontend (§3, §5).** Sezione **Fonti** del modulo `fidelizzazione`: elenco delle fonti con stato,
  scheda di anteprima dei tipi di segnale prima del collegamento, riquadro di conferma della revoca con i conteggi.
  Dati letti con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-4 — Cinque lingue (§4).** Nomi e significati dei tipi di segnale, nomi degli stati, testo della conferma di
  revoca e messaggio «non calcolabile» passano dallo spazio-nomi `fidelizzazione` e sono presenti in
  `en, it, fr, es, de`. Attenzione al doppio elenco: i significati dei tipi stanno **anche** nel manifesto dei dati,
  dove le lingue sono due.
- **RT-5 — Varchi e quota (§6).** La catena dei varchi si applica per intero; il collegamento di una fonte **non
  consuma** la metrica `rapporti_sorvegliati`, che si consuma sui rapporti (`0009`). Una fonte collegata può però
  far superare il tetto: il rifiuto avviene lì, non qui, e il messaggio della sezione Fonti lo anticipa.
- **RT-6 — Dati personali (§10).** La tabella `fonte` non contiene dati riferiti a clienti finali e resta fuori da
  `exportData` e `purgeData`. La **revoca** è invece un'operazione sui dati personali: cancella fisicamente i
  segnali di quella fonte e lascia una riga di prova nel registro delle purghe, perché sostituire i nomi con dei
  codici non è cancellare. Il manifesto va aggiornato con la regola di conservazione «i segnali di una fonte vivono
  quanto il suo collegamento».
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `collega_fonte(app) → bozza` e
  `scollega_fonte(app) → bozza che dichiara quanti segnali cancella`, entrambi marcati **scrittura irreversibile**
  con **conferma umana obbligatoria**. Il contratto vive dentro il servizio; il server conversazionale è di
  piattaforma e non ancora implementato (UC 0061-0063), e l'esposizione vera è della storia `0029`.
- **RT-8 — Registrazione eventi (§14).** «fonte collegata», «fonte sospesa», «fonte revocata con N segnali
  cancellati», «collegamento respinto per ruolo insufficiente», con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza dati personali.
- **RT-9 — Prove (§11).** Integrazione sulle quattro rotte; matrice dei ruoli con `member` respinto; prova che la
  revoca cancella fisicamente e scrive nel registro delle purghe; prova che dopo la revoca il punteggio risulta non
  calcolabile invece che ridotto; prova di isolamento fra due account.

## 4. Criteri di accettazione

**CA-1 — Collegamento informato**
- **Dato** un utente `owner` di un account abilitato a `fatture`
- **Quando** apre la sezione Fonti e sceglie di collegare la fatturazione
- **Allora** vede l'elenco chiuso dei quattro tipi di segnale con significato e unità, e solo dopo può confermare;
  da quel momento i segnali di quella fonte vengono scritti invece che scartati

**CA-2 — Un `member` non collega**
- **Dato** un utente con ruolo `member` · **Quando** tenta di collegare una fonte
- **Allora** riceve `403` con un messaggio che dice chi può farlo, e nulla cambia

**CA-3 — Solo le app abilitate compaiono**
- **Dato** un account abilitato a `fatture` ma non ad `abbonati`
- **Quando** apre l'elenco delle fonti collegabili
- **Allora** vede la fatturazione e **non** vede gli abbonamenti, e non esiste una richiesta che aggiri l'elenco

**CA-4 — La revoca dice prima quanto costa**
- **Dato** una fonte collegata con 3.400 segnali su 120 rapporti
- **Quando** un `admin` chiede di revocarla
- **Allora** prima della conferma legge quanti segnali verranno cancellati e su quanti rapporti il punteggio
  smetterà di essere calcolabile; alla conferma i segnali sono cancellati fisicamente e una riga di prova compare
  nel registro delle purghe

**CA-5 — Dopo la revoca il punteggio non mente**
- **Dato** un rapporto il cui punteggio dipendeva anche dalla fonte revocata
- **Quando** lo si apre
- **Allora** non mostra un numero più basso ma «non calcolabile — fonte *X* non collegata»

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, entrambi con la fatturazione fra le fonti possibili
- **Quando** `A` collega la fatturazione
- **Allora** in `B` la fonte resta scollegata, e nessuna manipolazione della richiesta permette ad `A` di collegare
  o revocare una fonte di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla macchina dei tre stati e di **integrazione** sulle rotte, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** su tutte le rotte introdotte, con tentativo di forzare `tenant_id`;
- [ ] **prova end-to-end**: *rimando* alla storia `0030`, che dovrà coprire «collego una fonte → i segnali entrano →
      revoco → il punteggio diventa non calcolabile»; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), significati dei tipi di segnale
      compresi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la regola di conservazione legata al
      collegamento; `fonte` resta motivatamente fuori da esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato: i tre stati e perché la sospensione non cancella, la revoca fisica con
      prova nel registro delle purghe, e la scelta «non calcolabile» invece di «numero più piccolo»;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `collega_fonte` e `scollega_fonte`, entrambi con
      conferma obbligatoria;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | il collegamento ha senso solo se esiste chi consuma i segnali e li scarta quando la fonte non è collegata: le due storie si collaudano insieme |
| epica di piattaforma non implementata, UC 0061-0063 | gli strumenti `collega_fonte` e `scollega_fonte` sono dichiarati qui ma non esposti: il server conversazionale non esiste. Nel frattempo il contratto resta versionato dentro il servizio |

## 7. Fuori ambito

- l'aggregazione dei segnali su un rapporto: storia `0009`;
- la fonte «inserimento manuale», che è una fonte come le altre ma nasce con la storia `0010`;
- il ritardo e il silenzio di una fonte collegata: storia `0011`;
- il ripopolamento dello storico al momento del collegamento: non è previsto in questa epica — vedi i punti aperti.

## 8. Punti aperti

- **Il ripopolamento dello storico al collegamento non è in questa epica, e la conseguenza va detta.** Chi collega
  la fatturazione oggi vede solo i fatti da oggi in avanti, e quindi non ha una linea di base: il punteggio dei
  primi mesi sarà povero. Il meccanismo corretto è quello dichiarato al §4.2 della descrizione — una richiesta di
  ripopolamento **per un solo account e una sola fonte**, mai «per tutti gli account» — ma non ha una storia
  propria nel catalogo di RenewGrove. Chiude: lo sviluppatore, decidendo se aprirla o se accettare la partenza a
  freddo.
- **La revoca cancella anche i punteggi storici che quella fonte ha contribuito a formare?** La proposta è **no**:
  i punteggi restano come serie storica, marcati come non più ricalcolabili, perché cancellarli riscriverebbe
  all'indietro la misura di efficacia dell'epica 05. È una scelta con un costo di conformità da verificare.
  Chiude: lo sviluppatore, insieme alla storia `0032`.
