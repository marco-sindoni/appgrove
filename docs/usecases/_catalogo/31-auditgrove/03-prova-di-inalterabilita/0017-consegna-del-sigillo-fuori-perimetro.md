# 0017 — Consegna del sigillo fuori perimetro

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 03 — Prova di inalterabilità
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che dovrà dimostrare qualcosa fra un anno
> voglio ricevere io stesso, periodicamente e fuori dai sistemi del fornitore, la fotografia firmata del mio
> registro
> così da avere in mano una copia che il fornitore non può più raggiungere, e quindi non può più cambiare.

**Contesto.** È la storia che chiude il ragionamento delle due precedenti, ed è la più importante delle tre.

La catena (0002) rende rilevabile una modifica isolata. Il sigillo (0013) fissa un punto nel tempo. Ma finché il
sigillo vive **soltanto nella nostra base di dati**, chi può riscrivere le righe può anche rifirmare i sigilli: il
presidio si riduce alla fiducia in noi, che è esattamente ciò che il prodotto promette di non chiedere.

Quello che rompe il cerchio è banale e decisivo: **il sigillo deve uscire**. Un sigillo che il cliente ha ricevuto
due mesi fa nella propria casella di posta è una prova, perché noi non possiamo più andare a prenderlo. Se un
giorno il nostro registro raccontasse una storia diversa da quel sigillo, la differenza sarebbe visibile a
chiunque metta i due documenti uno accanto all'altro. Non serve altro: non serve un registro pubblico, non serve
un notaio. Serve che una copia stia in un posto dove non arriviamo.

## 2. Requisiti funzionali

1. **RF-1** — Il cliente può indicare uno o più **destinatari di recapito** del sigillo (indirizzi di posta
   elettronica) e la **cadenza** del recapito, fra le cadenze offerte.
2. **RF-2** — A ogni scadenza, l'app recapita ai destinatari un messaggio che contiene il sigillo del periodo in
   forma verificabile: periodo, prima e ultima sequenza, conteggio, impronta di testa, algoritmo, firma, e le
   istruzioni per verificarlo.
3. **RF-3** — Il messaggio spiega in lingua semplice **perché va conservato** e che cosa se ne fa: è il punto in
   cui un cliente capisce o non capisce il valore di ciò che sta comprando.
4. **RF-4** — Ogni recapito è **tracciato**: quale sigillo, a quali destinatari, quando, con quale esito
   dell'invio; ed è a sua volta una riga del registro.
5. **RF-5** — Il sigillo è disponibile anche per **esportazione programmata** verso un deposito scelto dal
   cliente, per chi non vuole affidarsi alla posta elettronica.
6. **RF-6** — Un recapito fallito viene ritentato e, se continua a fallire, il cliente viene avvisato dentro
   l'applicazione: un recapito che non arriva e che nessuno nota vanifica l'intero presidio.
7. **RF-7** — Il cambio dei destinatari di recapito è a sua volta una riga del registro: chi lo cambia e quando.
   Chi volesse far smettere il recapito per poter poi riscrivere il passato lascerebbe traccia del tentativo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I destinatari e i recapiti appartengono a un account e ogni lettura e
  scrittura filtra per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della
  richiesta viene ignorato. Un recapito non deve **mai** poter allegare il sigillo di un altro account: è una
  prova esplicita, non un'ovvietà, perché è il genere di errore che produce una violazione di dati.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET`/`PUT /api/agentaudit/v1/seal-delivery-settings` e
  `GET /api/agentaudit/v1/seal-deliveries`; corpo validato (indirizzi in forma valida, cadenza fra quelle
  ammesse); errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__recapito_sigilli.sql` sullo schema `app_agentaudit`: tabella
  `seal_delivery_settings` e tabella `seal_deliveries` con `tenant_id`, chiave primaria UUID versione 7, colonne
  di controllo e cancellazione logica sulle sole impostazioni. La tabella dei recapiti è in **sola aggiunta** come
  le altre tabelle di prova.
- **RT-4 — Modulo frontend (§3, §5).** La configurazione del recapito e lo storico dei recapiti vivono nella
  sezione «Integrità» (storia 0014); solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono
  presenti in `en, it, fr, es, de`, **compreso il testo del messaggio recapitato**, che va nella lingua scelta dal
  destinatario o, in mancanza, in quella dell'account. È il testo più letto dell'app da persone che non l'hanno
  mai aperta: se è scritto male in una lingua, in quella lingua il prodotto non si capisce.
- **RT-6 — Varchi e quota (§6, §7).** Il recapito automatico **non consuma** la metrica `actions`, per la stessa
  ragione del sigillo automatico: è la piattaforma che adempie a una promessa, non l'agente che agisce. La
  modifica delle impostazioni richiede un ruolo amministrativo dell'account (`403` altrimenti); con abbonamento
  non attivo il recapito cessa e il fatto viene detto esplicitamente al cliente, perché smettere di consegnare la
  prova in silenzio sarebbe grave.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: cambiare i destinatari del
  recapito da una chat sarebbe la via più comoda per indebolire il presidio, ed è deliberatamente non esposto —
  stessa logica per cui l'approvazione non è esposta (§7 della descrizione dell'applicazione). La sola lettura
  dello stato dei recapiti rientra in `verifica_integrita`, marcato **lettura**.
- **RT-8 — Dati personali (§10).** Introduce **una voce nuova**: l'indirizzo di posta elettronica dei destinatari
  di recapito. Va dichiarata nel manifesto `docs/compliance/manifests/agentaudit.yaml` in italiano e inglese
  (finalità: recapitare la prova; base giuridica: esecuzione del contratto; conservazione: finché il recapito è
  configurato), il campo va annotato `@PersonalData`, e la tabella va aggiunta a `exportData` e `purgeData` del
  contratto dati dell'app.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `recapito riuscito`, `recapito fallito`, `recapito
  definitivamente fallito` e `destinatari modificati` sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, **senza l'indirizzo del destinatario**: si registra che c'è stato un recapito e
  a quanti, non a chi.

## 4. Criteri di accettazione

**CA-1 — Il sigillo esce**
- **Dato** un account con un destinatario di recapito configurato e cadenza mensile
- **Quando** scade il mese
- **Allora** il destinatario riceve un messaggio contenente il sigillo del periodo in forma verificabile e le
  istruzioni per verificarlo, e nel registro compare la riga del recapito

**CA-2 — La copia recapitata smaschera una riscrittura**
- **Dato** un sigillo recapitato al cliente due mesi prima
- **Quando** si confronta quel sigillo con la catena attuale, dopo che una riga del periodo è stata alterata
- **Allora** l'impronta di testa non coincide, e la divergenza è visibile a chiunque abbia i due documenti, senza
  bisogno di accedere ai nostri sistemi

**CA-3 — Un recapito che non arriva non passa inosservato**
- **Dato** un destinatario il cui indirizzo respinge i messaggi
- **Quando** i tentativi di recapito falliscono ripetutamente
- **Allora** il cliente vede un avviso dentro l'applicazione che dice quale sigillo non è stato consegnato e a
  chi, e la riga di registro riporta l'esito negativo

**CA-4 — Spegnere il recapito lascia traccia**
- **Dato** un account con recapito attivo
- **Quando** una persona rimuove tutti i destinatari
- **Allora** l'operazione riesce ma produce una riga di registro che dice chi l'ha fatta e quando, e l'app mostra
  che il presidio è disattivato invece di tacere

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con destinatari e sigilli propri
- **Quando** vengono eseguiti i recapiti dello stesso giorno
- **Allora** ogni destinatario riceve esclusivamente il sigillo del proprio account, e un utente di `A` non vede
  né può modificare i destinatari di `B`, anche forzando l'identificativo dell'altro account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione del messaggio e sulla politica di ritentativo, e di **integrazione**
      sulla lavorazione di recapito, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul recapito: nessun sigillo può finire nella casella sbagliata, ed è
      una prova dedicata;
- [ ] **prova end-to-end**: risposta «coprire ora» limitatamente alla configurazione — il percorso
      `[J-AGENTAUDIT]` riceve il passo «configura un destinatario di recapito e verifica che compaia nello
      storico»; il recapito effettivo si verifica con il servizio di posta simulato, e il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) viene aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), compreso il testo del messaggio
      recapitato;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con l'indirizzo dei destinatari, campo annotato, e
      tabella presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la voce obbligatoria: perché il
      recapito fuori perimetro è ciò che rende il sigillo una prova, e perché non è esposto al livello
      conversazionale;
- [ ] contratto degli **strumenti conversazionali**: nessuno di scrittura, e il divieto è motivato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0013` | Non si recapita ciò che non esiste: serve il sigillo firmato |
| Servizio di invio di posta elettronica della piattaforma | Il recapito è l'unico canale verso l'esterno di questa app; in locale è sempre simulato |

## 7. Fuori ambito

- **l'ancoraggio dell'impronta a un registro pubblico di terzi** e **la marca temporale qualificata di un
  prestatore di servizi fiduciari**: entrambi renderebbero la prova più forte di quanto faccia il recapito al
  cliente, e entrambi introducono un fornitore esterno, un costo per operazione e un effetto verso l'esterno. Sono
  il punto aperto 9 della descrizione dell'applicazione e non si decidono qui;
- il recapito dei **pacchetti di prova** (storia 0015): quelli si scaricano su richiesta, non si spediscono;
- l'avviso di approvazione verso la messaggistica di squadra: altra cosa, altra storia (0021) e altro fornitore.

## 8. Punti aperti

- **La posta elettronica è un canale imperfetto per una prova.** Un messaggio può essere cancellato dalla casella,
  può finire nella posta indesiderata, può non essere conservato. Il presidio funziona solo se il cliente
  conserva; non possiamo verificarlo, e sarebbe scorretto lasciar credere il contrario. Va detto chiaramente nel
  messaggio stesso. Chi chiude: sviluppatore, sui testi.
- **Un secondo canale.** L'esportazione programmata verso un deposito del cliente è più robusta della posta ma
  richiede una configurazione che una micro-impresa spesso non sa fare. Quali depositi offrire, e se offrirne,
  è una decisione di prodotto. Chi chiude: sviluppatore.
- **Il destinatario può non essere un utente della piattaforma** — per esempio il commercialista o il consulente
  esterno. In quel caso stiamo inviando a un terzo materiale che riguarda l'account: nessun dato personale nel
  sigillo (§7 della storia 0013), ma la relazione va inquadrata. Chi chiude: revisione legale.
