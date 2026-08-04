# 0022 — Offerte di trattenuta e loro limiti

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 04 — Interventi con conferma umana
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare disposto a rinunciare a qualcosa pur di non perdere un cliente da diecimila euro l'anno
> voglio poter proporre uno sconto o una proroga con un tetto che ho deciso io e con la firma di chi può darla
> così da non scoprire a fine anno che qualcuno ha regalato margine per trattenere clienti che se ne sono andati
> lo stesso.

**Contesto.** È l'unico punto dell'applicazione in cui si impegna **denaro**, e per questo ha due presidi che le
altre storie non hanno: un'**autorizzazione** di chi ha il ruolo per impegnarlo, e un **tetto** che nessun
intervento può superare. Il §5 della [descrizione](../application-description.md) e il modello utente `multi` del
§3 nascono anche da qui: «il titolare autorizza ciò che ha un costo».

> 🛑 **Divieto, non raccomandazione.** Un'offerta di RenewGrove **non può frapporsi al percorso di disdetta** di
> **19 SubGrove**: vive solo **prima** che la disdetta sia stata chiesta. La ragione è normativa. Il § 312k del
> codice civile tedesco impone dal **1° luglio 2022** un **pulsante di disdetta** chiaramente riconoscibile per i
> contratti a esecuzione continuata conclusi online; la Corte federale, con sentenza del **22 maggio 2025**, ne ha
> esteso la portata anche a contratti con pagamento unico quando il fornitore continua a erogare durante la
> durata. Chi non lo rispetta si espone a diffide e azioni inibitorie, **e il consumatore può recedere in
> qualunque momento senza preavviso** (§2.3, punto 2, fonte Noerr).
>
> ⚠️ **Onestà sulla fonte**: la fonte consultata **non chiarisce** se sia ammesso frapporre uno sconto o un
> sondaggio prima della conferma di disdetta, e per l'Italia non è stata trovata risposta. Non si inventa: si
> adotta la **postura più prudente possibile** — l'offerta vive solo prima della richiesta di disdetta — e si
> lascia la domanda al punto aperto n. 5 per la revisione legale. È la domanda più direttamente commerciale del
> prodotto, e va chiusa da chi può.

C'è un secondo confine, meno vistoso e altrettanto reale: un'offerta proposta **a ridosso di un rinnovo** cambia
le condizioni già comunicate nell'avviso di rinnovo che SubGrove ha l'obbligo di mandare (§2.3, punto 3). Non è un
dettaglio di comodo: è un coordinamento fra due applicazioni.

## 2. Requisiti funzionali

1. **RF-1** — Esiste l'entità `OffertaDiTrattenuta`, collegata a un intervento: **tipo** (`sconto`, `proroga`,
   `cambio condizioni`), **valore** (importo o percentuale o durata, secondo il tipo), **validità** (data entro
   cui vale), **chi l'ha autorizzata** e **quando**, stato.
2. **RF-2** — Un intervento che contiene un'offerta **non si conferma** senza l'**autorizzazione** di un utente
   con il ruolo dichiarato dal piano (`0018`), di norma `owner`. Chi prepara e chi autorizza restano campi
   distinti, come per la conferma (`0019`).
3. **RF-3** — Esiste un **tetto** per account, impostato da `owner`: valore massimo di una singola offerta ed
   eventuale tetto complessivo su una finestra. Un'offerta oltre il tetto è **respinta**, non «segnalata»: un
   tetto che si può superare non è un tetto.
4. **RF-4** — 🛑 Un'offerta **non può essere collegata a un rapporto per cui risulta già chiesta una disdetta**.
   Se il segnale di richiesta di disdetta è arrivato da SubGrove, la creazione dell'offerta è **respinta** con un
   messaggio che spiega il motivo per esteso, non con un codice muto. È l'attuazione del divieto del riquadro
   qui sopra.
5. **RF-5** — Quando un rapporto ha un **rinnovo imminente** entro una finestra dichiarata, la schermata avverte
   prima della conferma che l'offerta **cambia condizioni già comunicate** dall'avviso di rinnovo di SubGrove, e
   invita a coordinarsi. È un avviso, non un blocco: la decisione resta del titolare.
6. **RF-6** — L'esito di un'offerta si registra (`accettata`, `rifiutata`, `scaduta senza risposta`) e alimenta la
   misura dell'efficacia, dove il **costo delle concessioni** è una delle due colonne del rendiconto (`0027`).
7. **RF-7** — Le offerte di un periodo sono consultabili con il loro valore complessivo: sapere quanto si è
   concesso è metà del motivo per cui questa funzione esiste.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `offerta_di_trattenuta` e del tetto filtra
  per `tenant_id` preso dal token di accesso verificato; un `tenant_id` che arrivasse dal corpo della richiesta o
  dai parametri viene ignorato. Il tetto di un account non è leggibile né applicabile a un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/fidelizzazione/v1/interventi/{id}/offerta`,
  `POST /api/fidelizzazione/v1/offerte/{id}/autorizzazione`,
  `POST /api/fidelizzazione/v1/offerte/{id}/esito`,
  `GET /api/fidelizzazione/v1/offerte` (paginata, con totale del valore concesso nel periodo),
  `PUT /api/fidelizzazione/v1/impostazioni/tetto-offerte`. Corpo validato (tipo dall'elenco chiuso, valore
  coerente con il tipo, validità nel futuro); errori in `application/problem+json` — `409` per il divieto di RF-4,
  `422` per il superamento del tetto, entrambi con messaggio che spiega il motivo. Definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V18__offerta_di_trattenuta.sql` sullo schema `app_fidelizzazione`:
  tabella `offerta_di_trattenuta` (`tenant_id`, intervento, tipo, valore, unità, validità, autorizzata da,
  autorizzata il, stato, esito, momento dell'esito) e le colonne del tetto nelle impostazioni dell'account;
  chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. Gli importi si conservano in
  **centesimi**, coerentemente con la convenzione del listino (§7). Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Nella scheda dell'intervento: riquadro dell'offerta con tipo, valore,
  validità e stato dell'autorizzazione; nella sezione impostazioni, il tetto; nella sezione offerte, l'elenco del
  periodo con il totale concesso. Il messaggio del divieto di RF-4 e l'avviso di RF-5 sono testi per esseri umani,
  non codici. Dati letti e scritti con il client generato; solo token del sistema di design; funziona in tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — tipi di offerta, esiti, messaggio del divieto sulla disdetta,
  avviso sul rinnovo imminente, messaggio di superamento del tetto — passano dallo spazio-nomi `fidelizzazione` e
  sono presenti in `en, it, fr, es, de`. La spiegazione del divieto va scritta bene in tutte e cinque: è il testo
  che evita a un cliente una diffida.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente — **autorizzare un'offerta richiede il ruolo
  dichiarato dal piano**, di norma `owner`, e la matrice dei ruoli è collaudata. **Nessun consumo di quota
  nuovo**: la metrica `rapporti_sorvegliati` (natura `stock`) non conta le offerte. Il **tetto** delle offerte è
  una regola dell'account, non una quota di piattaforma, e non va confuso con essa: non risponde `429` ma `422`,
  perché non si rimedia comprando un piano superiore.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `autorizza_offerta(intervento, tipo, valore, validità) → bozza dell'offerta`, marcato **scrittura
  irreversibile** con **conferma umana obbligatoria**: impegna denaro del nostro cliente verso un terzo. Lo
  strumento applica **gli stessi controlli dell'interfaccia** — ruolo, tetto, divieto sulla disdetta — e li
  applica **prima** di produrre la bozza, così che dalla chat non esista una via più permissiva. Il server
  conversazionale è di piattaforma e **non è ancora implementato** (UC 0061-0063); la storia `0029` assembla gli
  strumenti di scrittura.
- **RT-8 — Dati personali (§10).** **Nessun campo anagrafico nuovo**: l'offerta contiene importi, date e
  riferimenti, non dati anagrafici. È però riferita a un intervento e quindi a un rapporto, cioè a una persona
  identificabile: per questo la tabella `offerta_di_trattenuta` entra in `exportData` e in `purgeData` di
  `FidelizzazioneDataContract`, come già previsto dall'elenco del §6 della descrizione. Voce nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml`, in **italiano e inglese**:
  `offerta.valore_ed_esito` — di chi è: cliente del nostro cliente (per riferimento); che dato è: economico più
  prova; a cosa serve: sapere che cosa è stato concesso, da chi autorizzato e con che esito; base giuridica:
  esecuzione del rapporto commerciale fra il nostro cliente e il suo cliente; conservazione: 24 mesi. Il campo
  «autorizzata da» riguarda un **utente del nostro cliente** ed è annotato `@PersonalData`.
- **RT-9 — Registrazione eventi (§14).** `offerta preparata (tipo)`, `offerta autorizzata (tipo, ruolo di chi ha
  autorizzato)`, `offerta respinta per tetto`, `offerta respinta per disdetta già chiesta`,
  `esito dell'offerta registrato`, con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e
  identificativo dell'intervento; **mai l'etichetta del rapporto**. Il **valore** dell'offerta si registra come
  ordine di grandezza e non in chiaro, per non riversare nei registri l'economia del cliente.

## 4. Criteri di accettazione

**CA-1 — Offerta autorizzata da chi ha il ruolo**
- **Dato** un intervento in bozza con un'offerta di sconto entro il tetto, su un piano che richiede `owner`
- **Quando** un utente `admin` tenta di confermare l'intervento
- **Allora** riceve `403` con l'indicazione del ruolo necessario, l'intervento resta in `bozza`, e dopo
  l'autorizzazione dell'`owner` la conferma va a buon fine

**CA-2 — Il tetto respinge, non segnala**
- **Dato** un account con tetto di 500 € per singola offerta
- **Quando** un utente prepara un'offerta da 800 €
- **Allora** riceve `422` in `problem+json` con il tetto e il valore richiesto, **nessuna offerta viene creata**, e
  l'evento del rifiuto è registrato

**CA-3 — 🛑 Divieto di frapporsi alla disdetta**
- **Dato** un rapporto per cui è arrivato da SubGrove il segnale «disdetta richiesta»
- **Quando** un utente tenta di collegare un'offerta di trattenuta a un intervento su quel rapporto
- **Allora** riceve `409` con il messaggio che spiega per esteso il motivo (un'offerta non può frapporsi al
  percorso di disdetta), nessuna offerta viene creata, e la spiegazione è presente in tutte e cinque le lingue

**CA-4 — Avviso sul rinnovo imminente**
- **Dato** un rapporto con rinnovo entro la finestra dichiarata, per cui SubGrove ha già mandato l'avviso di
  rinnovo
- **Quando** l'utente prepara un'offerta e apre la conferma
- **Allora** legge l'avviso che l'offerta cambia condizioni già comunicate e l'invito a coordinarsi; **può
  comunque procedere**, e l'avviso mostrato è registrato

**CA-5 — Esito e costo delle concessioni**
- **Dato** tre offerte autorizzate nel mese, due accettate e una scaduta
- **Quando** si apre l'elenco delle offerte del periodo
- **Allora** si vede il valore complessivo effettivamente concesso (le due accettate) distinto da quello proposto,
  con il dettaglio per tipo

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, con tetti diversi
- **Quando** un utente di `A` prepara un'offerta forzando nella richiesta l'identificativo di `B`
- **Allora** si applica il tetto di `A`, l'offerta nasce sotto `A`, e nulla di `B` è leggibile o modificato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul tetto (singola offerta e finestra), sulla coerenza valore/tipo e sul calcolo del
      valore concesso; prove di **integrazione** sulla risorsa, con database effimero e migrazioni Flyway vere;
- [ ] **prova dedicata al divieto di RF-4**: con il segnale di disdetta presente, nessuna offerta può essere
      creata, da nessuna via — interfaccia e strumento conversazionale compresi;
- [ ] prova di **isolamento fra account** e sulla **matrice dei ruoli** dell'autorizzazione;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e dovrà
      coprire il tratto «preparo un'offerta oltre il tetto → respinta; entro il tetto e autorizzata → conferma
      possibile»; voce `da-coprire` con motivo e storia proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), messaggio del divieto compreso;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `offerta.valore_ed_esito`, campo «autorizzata
      da» annotato `@PersonalData`, tabella `offerta_di_trattenuta` in `exportData` e in `purgeData`;
- [ ] **registro delle decisioni** compilato con: il divieto di frapporsi alla disdetta e la fonte normativa che
      lo motiva, l'onestà sulla lacuna della fonte, perché il tetto respinge con `422` e non con `429`, il
      coordinamento con l'avviso di rinnovo di SubGrove;
- [ ] contratto dello strumento `autorizza_offerta` dichiarato come **scrittura irreversibile con conferma
      obbligatoria**, con gli stessi controlli dell'interfaccia applicati prima della bozza;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` — intervento con conferma umana | un'offerta vive dentro un intervento e ne blocca la conferma finché non è autorizzata |
| storia `0018` — piani di intervento | è il piano a dichiarare **chi deve autorizzare** quando c'è di mezzo una concessione |
| storia `0006` — contratto del segnale | il divieto di RF-4 si regge sul tipo di segnale «disdetta richiesta» pubblicato da SubGrove: senza quel tipo nell'elenco chiuso, il divieto non è applicabile |
| **19 SubGrove** (app del catalogo, non implementata) | è la fonte del segnale di disdetta e del rinnovo imminente; finché non c'è, il divieto resta implementato e collaudato con segnali di prova |
| epica di piattaforma non implementata, UC 0061-0063 | `autorizza_offerta` è dichiarato e non esposto |

## 7. Fuori ambito

- **applicare** lo sconto o la proroga: RenewGrove **non tocca contratti né listini**. L'offerta è una proposta che
  esce come intervento; chi la mette in pratica è l'applicazione che possiede il contratto (SubGrove) o una
  persona;
- **un flusso di disdetta con offerte** all'atto della richiesta, come fanno Churnkey e ProsperStack (§2.1):
  **escluso per scelta**, ed è il divieto centrale di questa storia;
- **la misura di quanto le offerte abbiano trattenuto davvero**: storia `0027`, con il gruppo di confronto della
  `0025`. Qui si registra il costo, non l'efficacia;
- **l'approvazione a più firme** per offerte oltre una certa soglia: rimandata, perché in un'attività da cinque
  persone non esiste una seconda firma;
- **la generazione del documento contrattuale** dell'offerta: fuori dalla suite.

## 8. Punti aperti

- 🛑 **È ammesso frapporre un'offerta di trattenuta o un sondaggio prima della conferma di una disdetta?** La
  fonte sul § 312k tedesco non lo affronta e per l'Italia non è stata trovata risposta (§2.7). La storia adotta
  intanto la postura più prudente: l'offerta vive **solo prima** che la disdetta sia chiesta. Se la revisione
  legale concludesse che un passaggio intermedio è ammesso, la funzione andrebbe **aggiunta**, non liberata: il
  divieto di RF-4 resterebbe la scelta predefinita. Chiude: **revisione legale** — punto aperto n. 5 della
  descrizione.
- **Il coordinamento con l'avviso di rinnovo di SubGrove.** RF-5 avverte, ma non impedisce: un'offerta a ridosso
  del rinnovo cambia condizioni già comunicate al cliente finale, e la responsabilità di quella comunicazione è
  del titolare. **Raccomandazione**: avviso ora, e valutare con SubGrove se l'avviso di rinnovo debba poter essere
  rettificato — che è però una funzione di SubGrove, non di RenewGrove. Chiude: **sviluppatore**, con la direzione
  di prodotto.
- **Il valore predefinito del tetto.** Un tetto troppo basso rende la funzione inutile, uno troppo alto la rende
  pericolosa; e il valore giusto dipende dal margine del cliente, che non conosciamo. **Raccomandazione**: nessun
  tetto predefinito, ma la funzione delle offerte **spenta** finché l'`owner` non ne imposta uno. Chiude:
  **sviluppatore** — è anche una decisione di prezzo indiretta.
