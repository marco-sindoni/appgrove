# 0011 — Registrazione del servizio erogato

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 03 — Richiesta di recensione senza filtri
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che sta alla cassa
> voglio registrare che a un cliente è stato erogato un servizio, in dieci secondi o senza fare niente
> così che l'invito a recensire possa partire da solo, verso chi davvero è stato servito e nessun altro.

**Contesto.** L'invito non può partire nel vuoto: serve un fatto che lo giustifichi. Quel fatto è il **servizio
erogato**, e non è solo una comodità di prodotto — è un requisito normativo. La legge italiana 34/2026 richiede
che la recensione derivi da una **fruizione effettiva e personale** del servizio e che sia pubblicata entro trenta
giorni da quella fruizione (descrizione §2.3): senza sapere **quando** il servizio è stato erogato non si può né
rispettare la finestra (storia 0015) né dimostrare la legittimità dell'invito.

Il servizio erogato può arrivare da tre parti, e l'app deve accettarle tutte perché il cliente tipo ne ha solo una:
inserimento a mano (il parrucchiere con una sede), appuntamento erogato da 07 BookGrove, fattura emessa da 02
BillGrove. Le ultime due arrivano **a eventi**, mai con una chiamata da app ad app.

## 2. Requisiti funzionali

1. **RF-1** — Si registra un servizio erogato indicando sede, momento dell'erogazione, un riferimento al cliente
   (nome) e **almeno un recapito** (posta elettronica o telefono). Senza recapito il servizio si registra lo
   stesso, ma nasce già segnato come non invitabile, con il motivo.
2. **RF-2** — L'inserimento a mano è veloce: un modulo con quattro campi, con il momento preimpostato ad adesso, e
   la possibilità di incollare un elenco di righe per registrare la giornata in una volta sola.
3. **RF-3** — L'app consuma gli eventi «appuntamento erogato» e «fattura emessa» delle altre app della suite,
   quando presenti, e ne ricava un servizio erogato con l'origine indicata (`manuale`, `appuntamento`, `fattura`).
4. **RF-4** — Lo stesso cliente servito più volte genera **più** servizi erogati, ma l'app riconosce e segnala i
   duplicati evidenti (stesso recapito, stessa sede, meno di un'ora di distanza) perché non partano due inviti per
   la stessa visita.
5. **RF-5** — Un cliente può essere marcato «non contattare»: da quel momento nessun invito parte per lui, in
   nessuna sede dell'account, e il motivo compare nel registro di equità (storia 0016) come esclusione **lecita**.
6. **RF-6** — L'elenco dei servizi erogati mostra, per ciascuno, se e quando è partito l'invito e con che esito:
   è la schermata da cui l'addetto capisce cosa sta succedendo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `servizio_erogato` filtra per `tenant_id`
  preso dal token verificato; un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato. Vale anche
  per gli eventi consumati dalle altre app: l'account arriva dall'evento firmato, non dal contenuto.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/recensioni/v1/servizi`,
  `POST /api/recensioni/v1/servizi/lotto` per l'inserimento multiplo,
  `POST /api/recensioni/v1/clienti/non-contattare`; corpo validato; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `servizio_erogato` (storia 0002) con indice
  `(tenant_id, sede_id, erogato_il)`; l'elenco di non contattare è una tabella dedicata con il recapito in forma
  confrontabile. Migrazione `V6__non_contattare.sql`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Richieste* → «Servizi erogati»: elenco, modulo rapido, incollaggio
  di un elenco. Dati letti con il client generato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La registrazione **non consuma quota**: la quota è la sede. È una scelta
  deliberata e va scritta nel registro delle decisioni — far pagare i servizi registrati spingerebbe a registrarne
  di meno, cioè a invitare meno gente, cioè verso la selettività (descrizione §3).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura qui: la registrazione di un
  servizio è un'affermazione di fatto e va fatta da chi c'era. La lettura è coperta da `stato_delle_richieste`
  (storia 0027).
- **RT-8 — Dati personali (§10).** **Voci nuove nel manifesto** in italiano e inglese: `servizio.nome_cliente`,
  `servizio.recapito`, `servizio.momento_erogazione`, più l'elenco di non contattare. Campi annotati
  `@PersonalData`; tabelle in `exportData` e `purgeData`. ⚠️ **La base giuridica è il punto aperto numero uno
  dell'app** (descrizione §11.1): questa storia non la decide, la registra come campo e la lascia allo
  sviluppatore. ⚠️ Il momento dell'erogazione presso certe attività **è** un dato sulla salute per deduzione:
  vedi l'avviso della descrizione §6.
- **RT-9 — Registrazione eventi (§14).** `servizio registrato` con origine, `duplicato segnalato`,
  `cliente marcato non contattare`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione,
  **senza nomi né recapiti**.

## 4. Criteri di accettazione

**CA-1 — Registrazione a mano**
- **Dato** un addetto con ruolo `member` su una sede attiva
- **Quando** registra un servizio con nome, recapito e momento
- **Allora** il servizio compare nell'elenco come ammissibile all'invito

**CA-2 — Senza recapito**
- **Dato** lo stesso addetto
- **Quando** registra un servizio senza recapito
- **Allora** il servizio è salvato e marcato non invitabile con motivo «nessun recapito», e comparirà come
  esclusione lecita nel registro di equità

**CA-3 — Duplicato evidente**
- **Dato** un servizio già registrato per lo stesso recapito e la stessa sede dieci minuti fa
- **Quando** se ne registra un altro
- **Allora** l'app avvisa che sembra un duplicato e chiede conferma; se si conferma, il secondo nasce già segnato
  come da non invitare

**CA-4 — Non contattare**
- **Dato** un cliente marcato «non contattare»
- **Quando** gli si registra un servizio erogato
- **Allora** il servizio è salvato ma non invitabile, con il motivo, in **tutte** le sedi dell'account

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con i propri servizi erogati
- **Quando** un utente di `A` chiede l'elenco
- **Allora** vede solo i propri, anche forzando l'identificativo dell'altro account nella richiesta

**CA-6 — Evento da un'altra app**
- **Dato** un evento «appuntamento erogato» dell'account `A`
- **Quando** l'app lo consuma
- **Allora** nasce un servizio erogato di `A` con origine `appuntamento`, senza che nessuna chiamata sincrona sia
  partita verso l'altra app

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul riconoscimento dei duplicati e di **integrazione** sulle rotte e sul consumo degli
      eventi, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `servizio_erogato` e sull'elenco di non contattare;
- [ ] **prova end-to-end**: *coprire ora* il passo «registro un servizio erogato» nel percorso `[J-RECENSIONI]`, e
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le voci del cliente, campi annotati, tabelle in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la base giuridica **dichiarata come punto aperto** e non decisa
      dall'agente;
- [ ] contratto degli **strumenti conversazionali**: dichiarato che la registrazione non è esponibile in scrittura;
- [ ] controllo automatico di **accessibilità** verde sulle schermate introdotte.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | il servizio è sempre erogato **da una sede** |
| **decisione sulla base giuridica dell'invito** (descrizione §11.1) | senza, il manifesto non si chiude |
| 07 BookGrove e 02 BillGrove, se presenti | per le origini `appuntamento` e `fattura`; senza di loro resta l'inserimento a mano, che basta |

## 7. Fuori ambito

- la decisione di **chi** invitare — storia 0012: qui si registra il fatto, non si sceglie;
- l'invio — storia 0014;
- l'anagrafica clienti completa: RepGrove tiene il minimo che serve a mandare un messaggio, l'anagrafica è di 04
  LeadGrove (descrizione §10).

## 8. Punti aperti

- **Base giuridica dell'invito** (descrizione §11.1): è il punto che decide se questa storia è vendibile in Italia
  così com'è. Lo chiude lo sviluppatore con un parere legale.
- **Quanto si copia del cliente** dall'anagrafica condivisa: vedi i punti aperti della storia 0002.
- **Se l'elenco «non contattare» debba essere condiviso con 16 ReachGrove** (descrizione §11.5): la mia
  inclinazione è **no**, sono cose diverse — chi non vuole la pubblicità può gradire di essere invitato a
  recensire — ma è una decisione di prodotto, non mia.
</content>
