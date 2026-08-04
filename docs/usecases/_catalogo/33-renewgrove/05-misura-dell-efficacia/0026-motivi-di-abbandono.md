# 0026 — Motivi di abbandono

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 05 — Misura dell'efficacia
**Storia**: `0026` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024` — il motivo si registra su un esito già valutato come «perso»
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena perso un cliente sorvegliato
> voglio registrare in due secondi **perché** se n'è andato, scegliendo da un elenco corto
> così da scoprire, dopo venti volte, che non è il prezzo come credevo, ma i tempi di risposta.

**Contesto.** La `0024` sa dire *come* è finita; non sa dire *perché*, e il perché è l'unica informazione che cambia
il comportamento di chi legge. La [descrizione](../application-description.md) al §0.1 lo colloca con precisione: è
l'unico dei quattro doppioni con SubGrove che sia solo **parziale** — SubGrove sa *quanti* se ne sono andati e con
che etichetta contrattuale, non sa *perché* nelle parole di chi ha visto la cosa da vicino. Il rischio di questa
storia è uno solo e va disinnescato in partenza: un campo libero «note» sembra generoso e produce venti frasi
diverse per lo stesso motivo, che non si contano — e, molto peggio, è la porta da cui entrano le categorie
particolari dell'articolo 9 (§6). Per questo l'elenco è **corto, chiuso e senza testo libero**.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un **elenco chiuso di sei motivi**, con chiavi stabili e testi tradotti: *prezzo*, *qualità del
   servizio*, *non serve più*, *passato alla concorrenza*, *chiuso l'attività*, *altro*. L'elenco non è configurabile
   dal cliente: sei etichette che tutti usano nello stesso modo valgono più di quaranta che ciascuno usa a modo suo.
2. **RF-2** — Il motivo si registra **solo** su un esito già valutato **perso** (`0024`), **solo** da un nostro
   utente (`owner`, `admin` o `member`), dall'interfaccia. Un motivo su un esito *trattenuto* o *ancora aperto* è
   respinto con un messaggio che dice perché.
3. **RF-3** — **Nessun campo di testo libero**, nemmeno accanto ad *altro*. La descrizione (§6) elenca esattamente
   due punti in cui il testo libero esiste dentro l'app — il motivo di una contestazione (`0015`) e la nota su un
   intervento (`0019`) — e questa storia **non ne aggiunge un terzo**: sarebbe un ingresso in più per dati sulla
   salute o su vicende personali del cliente finale, su una popolazione di interessati che non ha rapporti con noi.
4. **RF-4** — Il motivo **non arriva mai da una fonte**: nessun evento in ingresso può valorizzarlo, e il contratto
   del segnale (`0006`) non prevede alcun campo che lo porti. In particolare l'app **non importa** l'attributo
   *motivo di cessazione* che SubGrove tiene sull'abbonamento: qui il motivo riguarda **il rapporto commerciale**,
   là riguarda **il contratto**, e sono due cose che possono legittimamente non coincidere.
5. **RF-5** — Il motivo è **l'unico campo aggiornabile** di un esito valutato — eccezione dichiarata alla regola di
   sola aggiunta della `0024`, e motivata: l'esito è un fatto misurato dall'app, il motivo è un'informazione raccolta
   da una persona, che può arrivare dopo o essere corretta. Ogni correzione lascia una riga di prova (chi, quando, da
   quale motivo a quale motivo).
6. **RF-6** — Il motivo compare **nella scheda del rapporto** accanto all'esito, e **nel rendiconto** (`0027`) come
   conteggio per motivo, con il numero di esiti persi **senza motivo registrato** mostrato accanto: una distribuzione
   che nasconde quanti casi non ha classificato è una distribuzione che mente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura del motivo filtra per `tenant_id` preso dal token
  verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** `PUT /api/fidelizzazione/v1/esiti/{id}/motivo` con corpo validato
  contro l'elenco chiuso (un valore fuori elenco è `400`, non un motivo nuovo); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__motivo_di_abbandono.sql` sullo schema `app_fidelizzazione`: colonna
  `motivo` su `esito_del_rapporto`, vincolata all'elenco chiuso, più la tabella `correzione_motivo` con `tenant_id`,
  chiave primaria UUID versione 7, colonne di controllo e cancellazione logica, che conserva la riga di prova del
  **RF-5**. Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Il motivo si sceglie da un elenco a scelta secca nella scheda del rapporto del
  modulo `fidelizzazione`; dati letti con il client generato; solo token del sistema di design; tema chiaro e scuro.
  Il gesto deve costare **un clic**: se costa di più nessuno lo farà e l'informazione non esisterà.
- **RT-5 — Cinque lingue (§4).** Le sei etichette e i messaggi di errore passano dallo spazio-nomi `fidelizzazione` e
  sono presenti in `en, it, fr, es, de`. Le **chiavi** restano stabili e non tradotte: si conta sulla chiave, mai
  sull'etichetta mostrata.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Con abbonamento `canceled` la rotta risponde `402`;
  la registrazione del motivo è consentita anche a un `member`, perché è chi vede il cliente da vicino a saperlo.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento**: il motivo è un giudizio che una persona dà su
  una vicenda che ha vissuto, e farlo scrivere a un assistente sulla base di quanto trova nei dati produrrebbe
  esattamente la classificazione plausibile e non verificabile che questa app esiste per evitare (§1 della
  descrizione). Il motivo si **legge** dalla chat dentro `stato_rapporto` ed `efficacia_degli_interventi` (`0028`).
  Il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Sì, la storia tratta dati personali**: il motivo è un'informazione riferita a un
  cliente del nostro cliente. Nessuna tabella nuova con dati personali oltre a `correzione_motivo`, che porta chi ha
  corretto (**utente del cliente**) e i due valori dall'elenco chiuso. Voce nuova nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml` in **italiano e inglese** per il campo `motivo` di
  `esito_del_rapporto` e per `correzione_motivo`; campi annotati `@PersonalData`; entrambe presenti in `exportData` e
  `purgeData` del contratto `FidelizzazioneDataContract`. Il divieto di testo libero del **RF-3** è il presidio che
  tiene fuori le categorie particolari dell'articolo 9, ed è **contrattuale e strutturale** (il campo non esiste),
  non un controllo sul contenuto.
- **RT-9 — Registrazione eventi (§14).** `motivo di abbandono registrato (chiave del motivo)` e `motivo corretto (da
  chiave, a chiave)`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza etichette di
  rapporti.
- **RT-10 — Prove (§11).** Unità sul rifiuto di un valore fuori elenco e sul rifiuto su esiti non *persi*;
  integrazione sulla rotta con database effimero e migrazioni vere; isolamento fra due account.

## 4. Criteri di accettazione

**CA-1 — Registrazione in un gesto**
- **Dato** un rapporto con esito valutato `perso` e nessun motivo
- **Quando** un utente sceglie *passato alla concorrenza* dalla scheda del rapporto
- **Allora** il motivo è salvato sull'esito, compare nella scheda e nel conteggio del rendiconto

**CA-2 — Elenco chiuso davvero**
- **Dato** una chiamata di programmazione con motivo `il_titolare_e_andato_in_pensione`
- **Quando** il servizio la elabora
- **Allora** risponde `400` in `application/problem+json` indicando i sei valori ammessi, e nulla è salvato

**CA-3 — Solo su esiti persi**
- **Dato** un esito `trattenuto` e un esito *ancora aperto*
- **Quando** si tenta di registrare un motivo su ciascuno
- **Allora** entrambe le richieste sono respinte con `409` e un messaggio che spiega che il motivo si registra solo
  su un rapporto perso

**CA-4 — Correzione tracciata**
- **Dato** un esito `perso` con motivo *prezzo*
- **Quando** un altro utente lo corregge in *qualità del servizio*
- **Allora** il motivo corrente è *qualità del servizio*, e la riga di prova riporta chi, quando e i due valori

**CA-5 — Nessun motivo dalle fonti**
- **Dato** un evento pubblicato da una fonte che contenga un campo `motivo` non previsto dal contratto del segnale
- **Quando** il consumatore lo elabora
- **Allora** l'evento è rifiutato dal validatore della `0006` con l'indicazione della regola violata, e nessun motivo
  di abbandono viene scritto

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con esiti persi
- **Quando** un utente di `A` forza nella richiesta l'identificativo di un esito di `B`
- **Allora** riceve `404` e il motivo di `B` resta invariato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla validazione dell'elenco chiuso e di **integrazione** sulla rotta, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla registrazione e sulla lettura del motivo;
- [ ] **prova end-to-end**: *rimando* — la registrazione del motivo compare nel percorso `[J-FIDELIZZAZIONE]` della
      storia `0030` come passo finale opzionale; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta la voce `da-coprire` con
      motivo («percorso di piattaforma non ancora creato») e storia proprietaria `0030`;
- [ ] **traduzioni** delle sei etichette e dei messaggi in `en, it, fr, es, de`, con chiavi stabili;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per il campo `motivo` e per `correzione_motivo`, campi
      annotati `@PersonalData`, entrambi in `exportData` e `purgeData`;
- [ ] registro dei trattamenti rigenerato dal manifesto nello stesso commit;
- [ ] **registro delle decisioni** compilato: i sei motivi e perché sei, il divieto di testo libero e la sua ragione
      di conformità, l'eccezione dichiarata alla regola di sola aggiunta, la non-importazione del motivo di
      cessazione di SubGrove;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento di scrittura, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la descrizione tratta il confine con SubGrove (§0.1).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0024` (esito del rapporto) | il motivo vive su un esito valutato `perso`: senza esito non c'è dove metterlo |
| storia `0006` (contratto del segnale) | il validatore che rifiuta i campi non dichiarati è ciò che rende vero il **RF-4** |
| **19 SubGrove**, attributo *motivo di cessazione* sull'abbonamento | non è una dipendenza di codice ma di significato: le due nozioni convivono e vanno riconciliate — vedi punti aperti |

## 7. Fuori ambito

- **la riconciliazione fra il motivo di RenewGrove e il motivo di cessazione di SubGrove**: qui si dichiara solo che
  le due nozioni sono diverse e che nessuna sovrascrive l'altra. Costruire la vista che le affianca, o decidere
  quale prevale, richiede un accordo fra le due applicazioni che oggi non esiste — è un **punto da coordinare**, non
  una funzione di questa storia;
- **chiedere il motivo al cliente finale** (un sondaggio all'uscita, un modulo): sarebbe un effetto verso l'esterno,
  quindi materia dell'epica 04 con conferma umana, e nella via A del §4.3 l'app non ha nemmeno i recapiti;
- l'**analisi** dei motivi nel tempo, per fascia o per fonte: il conteggio di base sta nel rendiconto (`0027`), la
  lettura trasversale è di **20 InsightGrove**;
- la deduzione automatica del motivo dai segnali: sarebbe una seconda profilazione, con nuovi obblighi, per
  un'informazione che una persona conosce meglio.

## 8. Punti aperti

- **Punto da coordinare con 19 SubGrove.** Quando un rapporto è anche un abbonato, esistono due motivi: quello
  contrattuale registrato là e quello commerciale registrato qui. Possono divergere in modo legittimo («ha disdetto
  per fine contratto» / «è passato alla concorrenza»). Serve una regola condivisa su quale si mostra dove e se uno
  dei due prevale nei conteggi. Non si decide dentro RenewGrove. Chiude: **piattaforma** (sviluppatore), insieme al
  contratto degli eventi di dominio del punto aperto n. 2 della [descrizione](../application-description.md).
- **Se i sei motivi bastino** per i settori del segmento (studi, service, manutentori, scuole). Un elenco corto
  invecchia male; un elenco lungo non si compila. La proposta è partire da sei e rivedere l'elenco quando ci saranno
  dati d'uso, non renderlo configurabile. Chiude: **sviluppatore** — direzione di prodotto.
