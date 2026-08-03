# 0027 — Uscita e sospensione del percorso

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 05 — Automazioni
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0012`, `0025`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che si è disiscritta ieri
> voglio smettere di ricevere anche i messaggi che erano già in programma per me
> così da non dover chiedere due volte la stessa cosa.

**Contesto.** È la storia che rende un percorso automatico accettabile invece che pericoloso. Una campagna dura
un'ora; un percorso automatico dura settimane, e in settimane succede tutto: la persona si disiscrive, il suo
recapito rimbalza per sempre, segnala il messaggio come indesiderato, l'account esaurisce la quota, il canale
aggiuntivo viene scollegato. Se la contattabilità si controllasse **all'ingresso** — cioè settimane prima — il
percorso continuerebbe a mandare a chi si è opposto, che è la violazione più facile da dimostrare e da sanzionare
([application-description.md](../application-description.md) §2.3). La regola di questa storia è quindi una sola,
e vale senza eccezioni: **il controllo di contattabilità si fa al momento del passo, non all'ingresso**, ed è lo
stesso controllo che fa la spedizione (storia 0020).

## 2. Requisiti funzionali

1. **RF-1** — Prima di **ogni** passo «manda», il percorso verifica che il destinatario sia ancora inviabile su quel
   canale: consenso valido, non disiscritto, non in quarantena, recapito non soppresso.
2. **RF-2** — Se il destinatario non è più inviabile, l'esecuzione **termina subito** con il motivo scritto
   (`disiscritto`, `soppresso`, `in quarantena`, `consenso revocato`) e nessun messaggio parte. L'uscita è
   definitiva: non si rientra da soli.
3. **RF-3** — Se manca una condizione **dell'account** e non della persona — quota esaurita, canale aggiuntivo non
   più attivo, dominio mittente non più verificato, piano che non comprende più le automazioni — l'esecuzione si
   **sospende** invece di terminare, con il motivo scritto, e riprende dal passo dove si era fermata quando la
   condizione torna vera.
4. **RF-4** — Distinzione visibile e non ambigua fra **terminata** (la persona è uscita, non torna) e **sospesa**
   (l'account ha un problema, riprenderà): sono due stati diversi, con due rimedi diversi.
5. **RF-5** — Fermare il percorso (storia 0025) fa uscire tutte le esecuzioni aperte con motivo «percorso fermato
   dall'utente»; riattivarlo **non** le fa riprendere: entrano solo le persone nuove.
6. **RF-6** — Una sospensione che dura oltre un limite dichiarato (proposta: 30 giorni) termina l'esecuzione con il
   motivo «sospesa troppo a lungo», invece di mandare settimane dopo un messaggio che non ha più senso.
7. **RF-7** — L'utente può far uscire a mano un singolo iscritto da un percorso, con un motivo che resta scritto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni verifica e ogni cambio di stato delle esecuzioni filtra per
  `tenant_id`; la lavorazione che fa avanzare i passi lavora per account. Un `tenant_id` che arrivasse dal corpo
  della richiesta di uscita manuale viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/campaigns/v1/automations/{id}/runs/{runId}/exit`
  (uscita manuale con motivo obbligatorio) e `GET .../runs/{runId}` per lo stato. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__automation_run_exit.sql`: colonne di stato dell'esecuzione
  (`aperta`, `sospesa`, `terminata`), motivo, momento del cambio e momento d'inizio della sospensione, sulla tabella
  `automation_run` creata dalla storia 0026. Il motivo è un valore di un elenco chiuso, non testo libero: serve a
  contare e a spiegare.
- **RT-4 — Modulo frontend (§3, §5).** Nella scheda del percorso, distinzione fra esecuzioni aperte, sospese e
  terminate con i motivi e i conteggi; azione di uscita manuale con motivo obbligatorio; avviso in evidenza quando
  esistono esecuzioni sospese per una condizione dell'account, con il rimedio. Solo token del sistema di design;
  tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I motivi di uscita e di sospensione sono **valori tradotti** nello spazio-nomi
  `campaigns` in `en, it, fr, es, de`: nel database resta il codice, non la parola.
- **RT-6 — Varchi e quota (§6, §7).** La quota esaurita sulla metrica `messages_sent` (natura `flow`) **sospende**,
  non termina: è un problema dell'account, non della persona. La prenotazione dell'unità di quota avviene prima
  dell'invio del passo e a esito negativo produce la sospensione con il motivo, mai la perdita silenziosa del passo.
  Piano che non comprende più le automazioni → sospensione con motivo, e `402` sulle rotte di gestione.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `disiscrivi` (storia 0035), che è **scrittura
  irreversibile con conferma umana**, deve far uscire la persona anche dai percorsi in cui si trova: la disiscrizione
  è una sola e vale ovunque. Va dichiarato nel contratto degli strumenti, altrimenti resta una promessa non scritta.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo che riguarda una persona oltre a quelli della storia 0026; si
  aggiorna la voce `automation_run.*` del manifesto con il motivo di uscita, che è un dato riferito a una persona
  (dice che si è disiscritta). Tabella già presente in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Esecuzione terminata» e «esecuzione sospesa» con il **codice** del motivo,
  gli identificativi di esecuzione, percorso e iscritto, `tenant_id`, `app_id`, `user_id` o indicazione di
  esecuzione automatica, e identificativo di correlazione. Mai il recapito, mai il nome, mai il contenuto.

## 4. Criteri di accettazione

**CA-1 — Disiscrizione onorata a percorso in corso**
- **Dato** un iscritto dentro un percorso, con il prossimo passo «manda» previsto per domani
- **Quando** oggi si disiscrive
- **Allora** domani non riceve niente, l'esecuzione risulta terminata con motivo «disiscritto» e nessuna unità di
  quota viene consumata

**CA-2 — Controllo al passo, non all'ingresso**
- **Dato** un iscritto entrato tre settimane fa quando era pienamente contattabile, il cui recapito è stato
  soppresso la settimana scorsa per rimbalzo permanente
- **Quando** arriva il momento del passo «manda»
- **Allora** l'invio non parte e l'esecuzione termina con motivo «soppresso»

**CA-3 — Quota esaurita: sospende, non termina**
- **Dato** un account che ha esaurito il tetto mensile di invii e 40 esecuzioni aperte
- **Quando** arrivano i passi «manda»
- **Allora** le esecuzioni risultano **sospese** con motivo «quota esaurita»; il mese successivo, o dopo il
  passaggio di piano, riprendono dal passo dove si erano fermate

**CA-4 — Fermare il percorso fa uscire tutti**
- **Dato** un percorso attivo con 120 esecuzioni aperte
- **Quando** l'utente lo ferma
- **Allora** le 120 esecuzioni risultano terminate con motivo «percorso fermato dall'utente»; riattivandolo, nessuna
  di quelle 120 riprende

**CA-5 — Sospensione troppo lunga**
- **Dato** un'esecuzione sospesa da oltre il limite dichiarato
- **Quando** la lavorazione periodica la esamina
- **Allora** viene terminata con motivo «sospesa troppo a lungo» e il fatto è visibile nel registro delle esecuzioni

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede di far uscire un'esecuzione di `B`
- **Allora** riceve `404` e nulla cambia in `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla decisione «termina oppure sospende» per ciascun motivo, e di **integrazione** sul
      percorso completo ingresso → disiscrizione → nessun invio, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sull'uscita manuale e sulla lavorazione periodica;
- [ ] **prova end-to-end**: coprire ora, in parte — il percorso `[J-CAMPAIGNS]` (storia 0037) contiene già il passo
      «la persona si disiscrive e non riceve più»; qui si aggiunge l'asserzione che l'uscita valga anche per le
      esecuzioni automatiche aperte, e si aggiorna la voce nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dei motivi presenti in tutte e cinque le lingue, con il codice conservato nel database;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per il motivo di uscita;
- [ ] **registro delle decisioni** compilato, con annotato perché la verifica è al passo e perché la quota sospende
      invece di terminare;
- [ ] contratto degli **strumenti conversazionali**: `disiscrivi` dichiara di far uscire anche dai percorsi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0011` | La soppressione vince su tutto: è la prima cosa che il controllo al passo guarda |
| Storia `0012` | La disiscrizione è l'evento che fa uscire dal percorso |
| Storia `0025` | Il percorso e i suoi passi |
| Storia `0026` | Le esecuzioni da far uscire nascono lì |
| Storie `0022`-`0024` | La verifica «canale non più attivo» riguarda i canali collegati col contratto del cliente |

## 7. Fuori ambito

- il registro consultabile delle esecuzioni con i conteggi per motivo: è la storia 0028, che legge quello che questa
  storia scrive;
- la disiscrizione in sé, con il collegamento e l'intestazione tecnica: è la storia 0012;
- la sorveglianza del tasso di segnalazione e il blocco d'account: è la storia 0021;
- il ripristino manuale di un'esecuzione terminata: **escluso per scelta** — riportare dentro una persona uscita
  richiede un consenso nuovo, non un pulsante.

## 8. Punti aperti

- **Limite di durata della sospensione.** Proposta: 30 giorni. Non ho un riferimento di settore; è una scelta di
  prodotto che va presa guardando anche il ciclo di fatturazione mensile, perché la causa più frequente di
  sospensione è la quota.
- **Se avvisare il cliente quando esistono esecuzioni sospese.** Un avviso dentro l'app c'è; un avviso per posta
  elettronica al titolare sarebbe una comunicazione di piattaforma verso il cliente, e non è materia di questa app.
  Chiude la piattaforma.
