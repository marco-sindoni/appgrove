# 0011 — Salute e ritardo delle fonti

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 02 — Arrivo dei segnali dalle altre app
**Storia**: `0011` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che si fida di RenewGrove per sapere chi sta per andarsene
> voglio accorgermi subito quando una fonte smette di mandare fatti
> così da non guardare per settimane un elenco tranquillo che è tranquillo solo perché non arriva più niente.

**Contesto.** È il rischio più elegante di quest'app e uno dei cinque dichiarati al §11 della
[descrizione](../application-description.md): una fonte che smette di pubblicare non produce alcun errore. I
punteggi restano fermi, l'elenco dei rapporti a rischio si accorcia, e tutto sembra andare bene mentre in realtà
l'app è cieca. **Il silenzio non è salute**: è la frase che questa storia deve rendere vera nel prodotto, non solo
nella descrizione. Sta in fondo all'epica perché ha bisogno di fonti collegate e di un flusso di segnali da
osservare, e va fatta **prima** dell'epica 03: un punteggio calcolato su una fonte in silenzio non deve nascere
senza la sua avvertenza addosso.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni fonte collegata la sezione **Fonti** mostra: momento dell'ultimo segnale ricevuto, ritardo
   rispetto a quello atteso, conteggio dei segnali ricevuti e conteggio di quelli scartati con il motivo prevalente.
2. **RF-2** — Ogni fonte dichiara un **ritardo atteso** — il tempo oltre il quale l'assenza di segnali è anomala per
   quella fonte — con un valore predefinito modificabile dal cliente, perché la fatturazione mensile e l'assistenza
   quotidiana non hanno lo stesso ritmo.
3. **RF-3** — Superato il ritardo atteso la fonte è marcata **«in silenzio»**, con da quando; è uno stato di
   diagnosi visibile, non un errore che blocca qualcosa.
4. **RF-4** — I punteggi che dipendono da una fonte in silenzio portano **l'avvertenza** «calcolato su dati fermi
   dal *giorno X*, fonte *Y* in silenzio», ovunque il punteggio si mostri. Un punteggio senza avvertenza è un
   punteggio che mente per omissione.
5. **RF-5** — Quando una fonte entra in silenzio, chi lavora nell'account riceve un **avviso interno** per posta
   elettronica, con quale fonte, da quando e che cosa smette di essere affidabile. L'avviso va a `owner` e `admin`,
   una volta per episodio di silenzio, non a ogni controllo.
6. **RF-6** — L'avviso è **interno**: va ai nostri utenti, mai al cliente finale. Nessun messaggio esce verso
   l'esterno da questa storia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Salute e ritardo si calcolano e si leggono per `tenant_id` preso dal token
  verificato; il conteggio dei segnali di un account non entra mai in quello di un altro, anche quando la fonte
  d'origine è la stessa applicazione.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/fidelizzazione/v1/fonti/salute` che restituisce lo
  stato di diagnosi per fonte, e `PUT /api/fidelizzazione/v1/fonti/{app}/ritardo-atteso`; corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit. Nessuna chiamata sincrona verso
  l'app d'origine per chiederle se è viva: la salute si deduce **solo** da ciò che è arrivato.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_fidelizzazione` che aggiunge a `fonte` ritardo atteso,
  momento dell'ultimo segnale, contatori e momento di ingresso in silenzio; `tenant_id`, chiave primaria UUID
  versione 7, colonne di controllo, cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Riquadri di salute nella sezione **Fonti** e riga di avvertenza sul
  punteggio nella sezione **Rapporti** e in **Panoramica**; dati letti con il client generato; solo token del
  sistema di design; funziona in tema chiaro e scuro. L'avvertenza non usa il rosso come tinta decorativa: il rosso
  resta al rischio.
- **RT-5 — Cinque lingue (§4).** Nome dello stato «in silenzio», testo dell'avvertenza sul punteggio, testo
  dell'avviso per posta elettronica e nomi dei motivi di scarto passano dallo spazio-nomi `fidelizzazione` e sono
  presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** La diagnosi è una lettura e non consuma quota; è accessibile a `owner`, `admin` e
  `member`, mentre modificare il ritardo atteso richiede `owner` o `admin`. Con abbonamento non attivo risponde
  `402`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `salute_delle_fonti() → per fonte: stato,
  ultimo segnale, ritardo`, marcato **lettura**, quindi libero. È lo strumento che risponde alla domanda «perché non
  vedo più niente su questo cliente?». Il contratto vive dentro il servizio; il server conversazionale è di
  piattaforma e non ancora implementato (UC 0061-0063); l'esposizione vera è della storia `0028`.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: la diagnosi tratta conteggi, momenti e stati di
  fonti, mai rapporti né etichette. L'avviso per posta elettronica va a un nostro utente, il cui recapito è già
  trattato dalla piattaforma, e **non nomina alcun cliente finale**: dice quale fonte tace, non chi si sta
  perdendo. Il servizio di invio è già di piattaforma: **nessun fornitore esterno nuovo**, coerentemente con il
  §2.4 della descrizione.
- **RT-9 — Registrazione eventi (§14).** «fonte entrata in silenzio», «fonte tornata a pubblicare», «avviso di
  silenzio inviato», «ritardo atteso modificato» con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali.
- **RT-10 — Prove (§11).** Unità sul calcolo del ritardo e sulla soglia di silenzio, con tempo controllato e
  **nessuna attesa a tempo**; integrazione sulle rotte e sull'invio dell'avviso, con database effimero e migrazioni
  vere; prova che l'avviso parte una volta sola per episodio; isolamento fra due account; controllo automatico di
  accessibilità sui riquadri introdotti.

## 4. Criteri di accettazione

**CA-1 — La salute si vede**
- **Dato** un account con tre fonti collegate
- **Quando** apre la sezione Fonti
- **Allora** per ciascuna legge momento dell'ultimo segnale, ritardo rispetto all'atteso, segnali ricevuti e
  segnali scartati con il motivo prevalente

**CA-2 — Il silenzio diventa uno stato**
- **Dato** una fonte con ritardo atteso di 7 giorni e nessun segnale da 9 giorni
- **Quando** si guarda la sezione Fonti
- **Allora** la fonte è marcata «in silenzio» dal giorno in cui la soglia è stata superata

**CA-3 — Il punteggio porta l'avvertenza**
- **Dato** un rapporto il cui punteggio dipende anche da una fonte in silenzio
- **Quando** lo si guarda, in Panoramica o nella scheda del rapporto
- **Allora** accanto al valore compare «calcolato su dati fermi dal *giorno X*, fonte *Y* in silenzio»

**CA-4 — Avviso interno una volta sola**
- **Dato** una fonte che entra in silenzio · **Quando** il controllo gira più volte mentre la fonte tace ancora
- **Allora** `owner` e `admin` ricevono **un solo** messaggio di posta elettronica per quell'episodio, e il messaggio
  non nomina alcun cliente finale

**CA-5 — Il ritorno chiude l'episodio**
- **Dato** una fonte in silenzio · **Quando** arriva un segnale valido
- **Allora** lo stato torna normale, l'avvertenza sparisce dai punteggi e un nuovo silenzio produrrà un nuovo avviso

**CA-6 — Isolamento fra account**
- **Dato** due account che hanno collegato la stessa applicazione d'origine, e in uno dei due i segnali arrivano
  regolarmente
- **Quando** si legge la salute delle fonti
- **Allora** ciascun account vede il proprio stato, e l'attività dell'uno non fa sembrare viva la fonte dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del ritardo e sulla soglia, e di **integrazione** sulle rotte e sull'avviso,
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul calcolo della salute e sull'invio dell'avviso;
- [ ] **prova end-to-end**: *rimando* alla storia `0030`, che dovrà coprire «una fonte tace → compare l'avvertenza
      sul punteggio»; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo e storia proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), testo dell'avviso compreso;
- [ ] **manifesto dei dati**: nessuna voce nuova — **nessun dato personale nuovo**; va scritto che l'avviso non
      nomina clienti finali e che non entra alcun fornitore esterno;
- [ ] **registro delle decisioni** compilato: il ritardo atteso per fonte con valore predefinito modificabile, un
      solo avviso per episodio, e la scelta di non interrogare la fonte per sapere se è viva;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `salute_delle_fonti`, marcato lettura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | senza fonti collegate non c'è salute da misurare, e il ritardo atteso è un attributo della fonte |
| storia `0007` (per i contatori) | i conteggi di segnali scritti e scartati per fonte nascono nel consumatore |
| servizio di invio della posta elettronica di piattaforma | l'avviso interno usa quello esistente: nessun fornitore nuovo |
| epica di piattaforma non implementata, UC 0061-0063 | `salute_delle_fonti` si dichiara qui e si espone nella storia `0028` |

## 7. Fuori ambito

- l'avvertenza **dentro** la spiegazione del punteggio, voce per voce: storia `0014`, che la riprende dove i
  contributi si aprono uno a uno;
- gli avvisi sui clienti a rischio, che sono un'altra cosa e stanno nell'epica 04;
- la diagnosi per la console di amministrazione di piattaforma: sta in
  [estensioni-admin.md](../estensioni-admin.md), non in questa storia;
- qualunque messaggio verso il cliente finale: qui non ne esce nessuno.

## 8. Punti aperti

- **Il valore predefinito del ritardo atteso per ciascuna fonte non è deciso.** Sette giorni per l'assistenza e
  quarantacinque per la fatturazione sono numeri plausibili, non misurati, e sbagliarli in eccesso rende l'avviso
  inutile mentre sbagliarli in difetto lo rende rumore — e il rumore si spegne, e allora l'avviso non serve più a
  niente. Proposta: partire larghi e stringere sui dati veri. Chiude: lo sviluppatore, in fase di implementazione.
- **Serve un avviso anche per il caso opposto, cioè una fonte che pubblica troppo?** Un'esplosione di segnali è
  altrettanto sospetta di un silenzio, e produrrebbe punteggi gonfiati. Non è previsto qui e non lo si inventa:
  va tracciato come possibile evoluzione. Chiude: lo sviluppatore.
