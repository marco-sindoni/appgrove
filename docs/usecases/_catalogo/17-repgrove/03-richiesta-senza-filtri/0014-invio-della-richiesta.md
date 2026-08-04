# 0014 — Invio della richiesta

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 03 — Richiesta di recensione senza filtri
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha registrato i servizi di ieri
> voglio che l'invito parta da solo, con il collegamento giusto per lasciare la recensione
> così da non dover ricordare niente e da non dover copiare a mano un indirizzo per ogni cliente.

**Contesto.** Qui l'app agisce **verso l'esterno**: manda un messaggio a una persona. È il punto in cui tutto il
lavoro delle storie precedenti diventa un fatto, e in cui gli errori diventano visibili a chi non è nostro
cliente. Due scelte tecniche contano più delle altre. La prima: il collegamento contenuto nel messaggio è
**quello che la piattaforma stessa mette a disposizione** — Google fornisce dalla scheda dell'attività un
collegamento (e un codice a barre bidimensionale) fatto apposta per chiedere le recensioni
([Google Business Profile Help](https://support.google.com/business/answer/16816815?hl=en)). Usiamo la strada che
il proprietario della piattaforma indica, non una nostra pagina intermedia. La seconda: la richiesta si scrive
**prima** di inviare, con lo stato `programmata`, e cambia stato dopo: così un guasto durante l'invio lascia una
traccia invece di un vuoto.

## 2. Requisiti funzionali

1. **RF-1** — Una lavorazione periodica prende i servizi erogati ammissibili, applica la regola di equità (storia
   0012), crea una `richiesta_recensione` per ciascun selezionato e la invia sul canale disponibile (posta
   elettronica come predefinito; messaggio breve se configurato e se il recapito è un telefono).
2. **RF-2** — Il messaggio è quello del modello **approvato** nella lingua del cliente, con i campi variabili
   risolti, e contiene il collegamento ufficiale della piattaforma collegata alla sede. Se la sede ha più
   piattaforme collegate, il collegamento è **uno solo per invito**, scelto secondo una preferenza dichiarata dal
   cliente: chiedere due recensioni con lo stesso messaggio è invadente e sposta il problema.
3. **RF-3** — C'è un ritardo configurabile fra il servizio erogato e l'invito (proposta predefinita: 2 ore,
   massimo 7 giorni), perché un invito che arriva mentre il cliente è ancora nel locale è pressione in loco, cioè
   una pratica vietata (descrizione §2.3).
4. **RF-4** — Ogni richiesta registra: momento dell'invio, canale, recapito usato, modello e lingua, esito
   (`recapitata`, `respinta`, `errore`) e — se non è partita — il motivo dall'elenco chiuso della storia 0012.
5. **RF-5** — Nessun invito parte due volte per lo stesso servizio erogato. Il vincolo è del database, non del
   codice.
6. **RF-6** — Il cliente può inviare **manualmente** un invito per un singolo servizio erogato, ma l'invito
   manuale passa **dagli stessi controlli**: regola di equità, modello approvato, ritardo minimo, esclusioni. Non
   esiste una via manuale che aggiri i presidi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione gira per account; ogni richiesta nasce con il
  `tenant_id` del servizio erogato da cui deriva. Nessun percorso crea una richiesta senza account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/recensioni/v1/richieste` e
  `POST /api/recensioni/v1/servizi/{id}/invita`; errori in `application/problem+json` con codici che distinguono
  «modello non approvato», «escluso dalla regola», «già invitato», «troppo presto», «quota esaurita»; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `richiesta_recensione` **ad accrescimento** sui campi di prova (storia
  0002), con vincolo di unicità su `(tenant_id, servizio_erogato_id)`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Richieste*: elenco con stato ed esito, filtro per sede e periodo,
  azione «invia adesso» sul singolo servizio. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'interfaccia in cinque lingue; **il messaggio al cliente finale** nella lingua
  del cliente, che può non essere una delle cinque: in quel caso si usa la lingua predefinita della sede e lo si
  dice nell'anteprima.
- **RT-6 — Varchi e quota (§6, §7).** L'invio non consuma `sedi_monitorate` — la quota è la sede — ma richiede una
  sede attiva e un abbonamento in stato che dà accesso (`trialing`, `active`, `past_due`); con `canceled` risponde
  `402`. **Nessun conteggio a invito**: vedi il registro delle decisioni della descrizione §3.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `programma_richieste` (storia 0028) prepara una
  **bozza del lotto** — chi verrebbe invitato e chi no, con il motivo — e richiede conferma umana prima di
  inviare: è un effetto verso l'esterno.
- **RT-8 — Dati personali (§10).** **Voce nuova nel manifesto** in italiano e inglese:
  `richiesta.recapito_usato`, con la finalità «prova di che cosa è stato inviato e a chi» e una conservazione più
  lunga del dato d'origine, perché è materiale di prova (descrizione §6). Campo annotato `@PersonalData`; tabella
  in `exportData` e `purgeData`. **Il fornitore di recapito** dei messaggi va nell'elenco dei fornitori: è quello
  che riceve nome e recapito di ogni cliente invitato.
- **RT-9 — Registrazione eventi (§14).** `lotto inviato: n richieste`, `richiesta inviata`, `richiesta respinta`
  con il codice, `invio fallito` con il motivo tecnico, tutti con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione. **Mai** il recapito nei registri.

## 4. Criteri di accettazione

**CA-1 — L'invito parte**
- **Dato** una sede con regola `tutti`, modello approvato, un collegamento attivo e tre servizi erogati di più di
  due ore fa
- **Quando** la lavorazione gira
- **Allora** partono tre richieste, ciascuna con il collegamento ufficiale della piattaforma, e l'elenco mostra
  l'esito

**CA-2 — Troppo presto**
- **Dato** un servizio erogato dieci minuti fa
- **Quando** la lavorazione gira
- **Allora** non parte nulla per quel servizio, che resta in attesa fino allo scadere del ritardo minimo

**CA-3 — Modello non approvato**
- **Dato** una sede il cui modello è in stato `respinto`
- **Quando** la lavorazione gira o si chiede l'invio manuale
- **Allora** nessun invito parte, e il messaggio rimanda alla schermata del modello

**CA-4 — Nessun doppio invito**
- **Dato** un servizio erogato per cui l'invito è già partito
- **Quando** si tenta di inviarlo di nuovo, anche manualmente
- **Allora** l'operazione è rifiutata dal vincolo di unicità e nulla parte

**CA-5 — L'invio manuale non aggira i presidi**
- **Dato** un servizio escluso dalla regola `uno_ogni_n`
- **Quando** si tenta l'invio manuale
- **Allora** è rifiutato con il motivo, perché la via manuale non è una scappatoia

**CA-6 — Isolamento fra account**
- **Dato** due account con servizi erogati pronti
- **Quando** la lavorazione gira per `A`
- **Allora** non parte nessun messaggio per i clienti di `B` e nessuna richiesta di `B` viene creata

**CA-7 — Guasto durante l'invio**
- **Dato** un fornitore di recapito che risponde con un errore
- **Quando** la lavorazione tenta l'invio
- **Allora** la richiesta resta registrata con esito `errore` e il motivo, e viene ritentata secondo una politica
  dichiarata, senza mai duplicare il messaggio

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla scelta del collegamento e sul ritardo minimo; di **integrazione** sulla lavorazione,
      con database effimero, migrazioni vere e fornitore di recapito **simulato** (nessun messaggio reale nelle
      prove);
- [ ] prova di **isolamento fra account** sulla lavorazione di invio;
- [ ] **prova end-to-end**: *coprire ora* il passo «l'invito parte per tutti i clienti serviti» nel percorso
      `[J-RECENSIONI]`, con fornitore simulato, e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `richiesta.recapito_usato`, campo annotato, tabella in esportazione e
      cancellazione, **fornitore di recapito** nell'elenco dei fornitori;
- [ ] **registro delle decisioni** compilato, con la scelta del collegamento ufficiale e del ritardo minimo;
- [ ] contratto degli **strumenti conversazionali**: `programma_richieste` produce una bozza e richiede conferma.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | la regola decide chi riceve l'invito |
| storia `0013` | senza modello approvato non parte niente |
| storia `0007` o `0008` | serve un collegamento attivo per avere il collegamento da mettere nel messaggio |
| fornitore di recapito dei messaggi | è un'integrazione esterna: va scelto e contrattualizzato prima |

## 7. Fuori ambito

- il sollecito e la finestra dei trenta giorni — storia 0015;
- il registro di equità esportabile — storia 0016;
- l'uso del meccanismo di inviti nativo di Trustpilot (storia 0008, punti aperti).

## 8. Punti aperti

- **Ritardo minimo di due ore**: è una proposta ragionata (evitare la pressione in loco), non un valore rilevato.
  Per certi settori — un ristorante a pranzo — potrebbe essere troppo poco. Da tarare con clienti veri.
- **Messaggi brevi**: costano al messaggio e hanno regole proprie per paese. La proposta è tenerli come canale
  secondario, con il contratto del cliente presso il fornitore, come fa 16 ReachGrove per lo stesso problema. Non
  è deciso.
- **Scelta della piattaforma quando ce ne sono due**: la proposta è una preferenza dichiarata per sede. Alternarle
  darebbe più equilibrio ma rende il registro di equità più difficile da spiegare. Da confermare.
</content>
