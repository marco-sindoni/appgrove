# 0013 — Calcolo e storico del punteggio

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 03 — Punteggio di rischio spiegabile e contestabile
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che guarda l'elenco dei clienti al mattino
> voglio un numero aggiornato per ogni rapporto, e la possibilità di vedere com'era un mese fa
> così da accorgermi non solo di chi è messo male, ma di **chi sta peggiorando**, che è la cosa che si vede tardi.

**Contesto.** Il modello esiste (`0012`) ma non ha ancora prodotto nulla. Questa storia lo fa girare, e prende
subito la decisione che condiziona tutte le epiche successive: il punteggio **si conserva come serie storica in
sola aggiunta e non si riscrive mai all'indietro**. È scritto nel §4.4 della [descrizione](../application-description.md)
come unica eccezione motivata alle regole di persistenza, e la ragione è pratica prima che formale. Se ricalcolando
si sovrascrivesse il passato, l'epica 05 — quella che dovrebbe dimostrare o smentire il valore del prodotto — non
misurerebbe più nulla, perché il «prima» dell'intervento cambierebbe insieme al «dopo»; e una contestazione
(`0015`) non avrebbe un fatto contro cui essere sollevata. Uno storico che si riscrive è uno storico che non regge
un reclamo.

La seconda decisione riguarda l'onestà del numero. Se una fonte ha smesso di pubblicare (`0011`), il punteggio
continuerebbe a sembrare attuale mentre è cieco su una parte dei fatti: è, come dice la descrizione, «il modo più
elegante di essere inutili». Perciò ogni valore porta con sé la marcatura che dice su quali fonti è stato calcolato
e quali erano in silenzio.

## 2. Requisiti funzionali

1. **RF-1** — Il punteggio di un rapporto si ricalcola in **due occasioni**: quando arrivano segnali nuovi per quel
   rapporto, e a **cadenza giornaliera** per tutti i rapporti sorvegliati — perché una finestra di osservazione
   scorre anche quando non succede nulla, e «non succede nulla» è a sua volta un segnale.
2. **RF-2** — Ogni calcolo produce una **riga nuova** nella serie storica del rapporto: valore, fascia, momento del
   calcolo, **versione del modello** con cui è stato calcolato, e l'elenco dei **contributi** (quale segnale, con
   quanto peso, in che verso). Nessuna riga già scritta viene aggiornata o cancellata.
3. **RF-3** — Un punteggio calcolato mentre una fonte collegata è **in silenzio** oltre il ritardo atteso (storia
   `0011`) è marcato come **parziale**, con l'elenco delle fonti mancanti; l'interfaccia lo mostra come tale e non
   come un numero pieno.
4. **RF-4** — La lavorazione giornaliera è **idempotente** e **recupera i giorni saltati**: se non ha girato per
   tre giorni, al risveglio produce i punteggi dei giorni mancanti con la finestra corretta di ciascun giorno, e
   rieseguirla sullo stesso giorno non duplica righe.
5. **RF-5** — La scheda del rapporto mostra il **punteggio corrente**, la sua fascia e **l'andamento** sulla
   finestra dichiarata dal modello: è la tendenza a valere, non il valore di un lunedì (§2.5).
6. **RF-6** — Se il rapporto non ha una linea di base sufficiente o non ha segnali nella finestra, il punteggio
   **non si inventa**: il rapporto risulta «non ancora valutabile» con il motivo, e non finisce in fascia
   `in salute` per omissione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lettura dei punteggi filtra per `tenant_id` preso dal token di
  accesso verificato. La **lavorazione** non ha un token di utente: itera per account e porta il `tenant_id` nel
  contesto di esecuzione, che è lo stesso usato dal filtro riga per riga; non esiste un percorso di calcolo che
  legga o scriva senza `tenant_id`. Un `tenant_id` proveniente dal corpo di una richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/fidelizzazione/v1/rapporti/{id}/punteggio`
  (corrente) e `GET /api/fidelizzazione/v1/rapporti/{id}/punteggi` (serie storica, paginata a pagina e dimensione
  con totale); `GET /api/fidelizzazione/v1/rapporti?fascia=…&ordina=punteggio` per l'elenco. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit. **Nessuna rotta che ricalcoli
  su richiesta di un utente qualsiasi**: il ricalcolo davanti a chi guarda esiste solo nella contestazione
  (`0015`) e nell'anteprima della taratura (`0016`).
- **RT-3 — Persistenza (§8).** Migrazione `V10__punteggio.sql` sullo schema `app_fidelizzazione`: tabella
  `punteggio` (`tenant_id`, rapporto, versione del modello, valore, fascia, momento del calcolo, marcatura di
  parzialità, elenco delle fonti in silenzio) e tabella `contributo_punteggio` (`tenant_id`, punteggio, segnale,
  peso applicato, verso, scostamento misurato). Chiavi primarie UUID versione 7, colonne di controllo
  `created_at`, `updated_at`, `created_by`, `updated_by`. **Sola aggiunta**: nessun aggiornamento e nessuna
  cancellazione logica sulle righe di serie storica — `deleted_at` esiste per la sola cancellazione fisica dei
  diritti dell'interessato. Indice su `(tenant_id, rapporto, momento del calcolo)` e vincolo di unicità su
  `(tenant_id, rapporto, giorno di calcolo, origine del calcolo)` che rende idempotente la lavorazione. Nessuna
  chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione `Rapporti` del modulo `fidelizzazione`: colonna con punteggio
  e fascia, ordinamento per rischio; nella scheda del rapporto, l'andamento sulla finestra e la marcatura
  «parziale» quando c'è. Dati letti con il client generato; solo token del sistema di design; funziona in tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi delle fasce, testo della marcatura di parzialità, motivo del «non ancora
  valutabile»: tutte le stringhe passano dallo spazio-nomi `fidelizzazione` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente. La lettura dei punteggi è aperta anche a
  `member`. **Nessun consumo di quota nuovo**: la metrica `rapporti_sorvegliati` (natura `stock`) si consuma
  quando un rapporto entra in sorveglianza (`0009`); calcolare il suo punteggio è il servizio, non un consumo
  ulteriore. La lavorazione giornaliera **non calcola** per un account con abbonamento `canceled`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `elenca_rapporti_a_rischio(fascia?, fonte?, entro_giorni?) → elenco minimizzato con punteggio e fascia`, marcato
  **lettura** e idempotente. L'elenco minimizzato porta etichetta, punteggio, fascia e marcatura di parzialità:
  non porta segnali né contributi, che sono di `spiega_punteggio` (`0014`). Il contratto vive dentro il servizio;
  il server conversazionale è di piattaforma e **non è ancora implementato** (UC 0061-0063); l'assemblaggio degli
  strumenti di lettura è della storia `0028`.
- **RT-8 — Dati personali (§10).** **Sì, e sono i più delicati dell'app.** Voce nuova nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml`, in **italiano e inglese**:
  `punteggio.valore_e_contributi` — dove vive: tabelle `punteggio` e `contributo_punteggio`; di chi è: cliente del
  nostro cliente; che dato è: **previsione, quindi profilazione**; a cosa serve: dire chi si rischia di perdere e
  perché; base giuridica: legittimo interesse del titolare, **da confermare in revisione legale** (punto aperto
  n. 4); conservazione: serie storica per 24 mesi, mai riscritta all'indietro (punto aperto n. 9). I campi che
  riferiscono il rapporto sono annotati `@PersonalData`; le tabelle `punteggio` e `contributo_punteggio` entrano
  in `exportData` e in `purgeData` di `FidelizzazioneDataContract`. La cancellazione è **fisica**: sostituire il
  riferimento con un codice non è cancellare.
- **RT-9 — Registrazione eventi (§14).** `punteggio calcolato (versione del modello, fascia)`,
  `punteggio marcato parziale (numero di fonti in silenzio)`, `lavorazione giornaliera conclusa (rapporti
  trattati, giorni recuperati)`, con `tenant_id`, `app_id`, `user_id` — vuoto per la lavorazione — e
  identificativo di correlazione; **nessun valore riferibile a una persona nel registro**: si scrivono
  identificativi e conteggi, non etichette.

## 4. Criteri di accettazione

**CA-1 — Un segnale nuovo produce una riga nuova**
- **Dato** un rapporto con un punteggio calcolato ieri
- **Quando** arriva un segnale nuovo per quel rapporto
- **Allora** viene scritta una **riga nuova** nella serie con il momento del calcolo, la versione del modello e i
  contributi; la riga di ieri esiste ancora, identica

**CA-2 — La lavorazione recupera i giorni saltati ed è idempotente**
- **Dato** un account i cui punteggi non vengono calcolati da tre giorni
- **Quando** la lavorazione giornaliera gira, e poi gira una seconda volta lo stesso giorno
- **Allora** esistono i punteggi dei tre giorni mancanti, ciascuno calcolato con la finestra del proprio giorno, e
  la seconda esecuzione **non aggiunge righe**

**CA-3 — Punteggio parziale su fonte in silenzio**
- **Dato** un rapporto i cui segnali di pagamento arrivano da una fonte in silenzio da oltre il ritardo atteso
- **Quando** il punteggio viene calcolato
- **Allora** il valore è marcato **parziale**, elenca la fonte mancante, e l'interfaccia lo mostra con quella
  marcatura invece che come un numero pieno

**CA-4 — Nessun punteggio inventato**
- **Dato** un rapporto sorvegliato da due giorni, senza segnali e senza linea di base
- **Quando** la lavorazione gira
- **Allora** il rapporto risulta «non ancora valutabile» con il motivo, **non** compare in fascia `in salute`, e
  nessuna riga di punteggio con valore viene scritta

**CA-5 — La serie storica non si riscrive**
- **Dato** un punteggio calcolato con la versione 1 del modello e una versione 2 resa viva dopo
- **Quando** il rapporto viene ricalcolato
- **Allora** la riga nuova cita la versione 2 e la riga vecchia continua a citare la versione 1 con il proprio
  valore intatto; nessun aggiornamento avviene su righe già scritte

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri rapporti e punteggi
- **Quando** un utente di `A` chiede la serie storica di un rapporto di `B` forzandone l'identificativo
- **Allora** riceve `404` e nessun valore di `B` compare nella risposta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul calcolo del contributo di una voce (scostamento dalla linea di base, finestra, verso)
      e sull'idempotenza della lavorazione; prove di **integrazione** sulla serie storica, con database effimero e
      migrazioni Flyway vere, che verificano che nessuna riga esistente venga aggiornata;
- [ ] prova di **isolamento fra account** sulle risorse `punteggio` e `contributo_punteggio`, compresa la
      lavorazione: due account con dati diversi, nessuna contaminazione;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e dovrà
      coprire il tratto «arriva un segnale → il punteggio cambia → la serie storica ha due righe»; voce
      `da-coprire` con motivo e storia proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `punteggio.valore_e_contributi`, campi annotati
      `@PersonalData`, tabelle `punteggio` e `contributo_punteggio` presenti in `exportData` e in `purgeData`;
- [ ] **registro delle decisioni** compilato con: perché la serie è in sola aggiunta, perché ogni valore porta la
      versione del modello, perché esiste la marcatura di parzialità, come è resa idempotente la lavorazione;
- [ ] contratto dello strumento `elenca_rapporti_a_rischio` dichiarato come **lettura**;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: la descrizione §4.1 e §6 riflettono le due tabelle effettivamente create.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` — modello del punteggio | senza voci, pesi, versi, finestre e soglie non c'è nulla da calcolare, e senza versione non c'è nulla da citare |
| storia `0011` — salute e ritardo delle fonti | la marcatura «parziale» ha bisogno di sapere quali fonti sono in silenzio; senza, il punteggio mentirebbe per omissione |
| storia `0009` — il rapporto sorvegliato | la linea di base e l'insieme dei segnali stanno sul rapporto |
| epica di piattaforma non implementata, UC 0061-0063 | `elenca_rapporti_a_rischio` è dichiarato ma non esposto: nel frattempo vive come contratto versionato dentro il servizio |

## 7. Fuori ambito

- **la spiegazione mostrata a chi legge** — quali fatti, con quanto peso, in che verso, e che cosa lo farebbe
  scendere: storia `0014`. Qui i contributi si **scrivono**, non si raccontano;
- **la contestazione di un segnale** e il ricalcolo davanti a chi guarda: storia `0015`;
- **la modifica dei pesi**: storia `0016`;
- **gli avvisi a chi lavora** quando un rapporto cambia fascia: non c'è storia che li possieda in questa epica, e
  non li anticipo — un avviso è già un effetto, e gli effetti sono dell'epica 04;
- **il ricalcolo retroattivo della serie con un modello nuovo**: escluso per scelta, non rimandato. Sarebbe
  esattamente la riscrittura all'indietro che questa storia vieta.

## 8. Punti aperti

- **La finestra di conservazione di 24 mesi** sulla serie storica dei punteggi. È una proposta prudente e non un
  dato: dipende dalla base giuridica scelta e dalla durata tipica dei rapporti nel settore del cliente. Una serie
  più corta indebolisce la misura di efficacia (epica 05); una più lunga conserva profilazioni di cui nessuno ha
  più bisogno. Chiude: **revisione legale** — punto aperto n. 9 della descrizione.
- **Ogni quanto deve girare la lavorazione giornaliera, e a che ora.** Una cadenza notturna è la scelta ovvia, ma
  gli account stanno su fusi diversi e «il punteggio di ieri» non ha lo stesso significato ovunque.
  **Raccomandazione**: cadenza giornaliera sul fuso dell'account, con recupero dei giorni saltati come da RF-4.
  Chiude: **sviluppatore**.
