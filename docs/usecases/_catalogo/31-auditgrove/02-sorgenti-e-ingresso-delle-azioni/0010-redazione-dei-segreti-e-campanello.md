# 0010 — Redazione dei segreti e campanello

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 02 — Sorgenti e ingresso delle azioni
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che ha collegato i propri agenti ad AuditGrove
> voglio che le chiavi di accesso e le password non entrino mai nel registro, e voglio essere avvisato quando un
> mio agente ne passa una in chiaro
> così da non aggiungere un secondo problema al primo, e da scoprire una falla che oggi non vedo.

**Contesto.** La storia 0009 ha stabilito che dei parametri si conserva l'impronta e non il valore. Resta un
problema che l'impronta non risolve: **i segreti**. Se un agente passa una chiave di accesso come parametro, anche
la sola impronta è materiale delicato, e il nome del parametro può bastare a rivelare che quel segreto esiste e
dove. Perciò prima dell'impronta viene la **redazione**, e la redazione la fa il servizio: non ci si può fidare
che la faccia il chiamante, perché se il cliente avesse la disciplina di ripulire i propri parametri non avrebbe
bisogno di noi. Poi c'è la parte che trasforma una difesa in una funzione: quando la redazione trova un segreto,
**lo dice al cliente**. È probabilmente una delle cose più utili che questo prodotto possa fare, e nasce come
effetto collaterale di un presidio.

## 2. Requisiti funzionali

1. **RF-1** — All'ingresso, il servizio riconosce come segreto un parametro il cui **nome** rientra fra quelli
   noti: `password`, `token`, `secret`, `api_key`, `authorization` e le loro varianti ragionevoli.
2. **RF-2** — Il servizio riconosce come segreto anche un **valore** che ha la forma nota di una chiave di
   accesso, di una carta di pagamento o di un codice fiscale, indipendentemente dal nome del parametro.
3. **RF-3** — Ciò che viene riconosciuto è sostituito da un **marcatore che dice che cosa è stato rimosso** — per
   esempio «rimosso: chiave di accesso» — e non da un segno generico: sapere *che genere* di segreto era è
   informazione utile, sapere solo che c'era qualcosa non lo è.
4. **RF-4** — La redazione avviene **prima** del calcolo dell'impronta del parametro e prima del calcolo
   dell'impronta dell'evento: nella catena entra il valore redatto, non l'originale.
5. **RF-5** — La riga dichiara di essere stata redatta, e quanti parametri lo sono stati: la redazione non è
   invisibile: è un fatto del registro.
6. **RF-6** — **Il campanello**: quando la redazione trova un segreto, l'account riceve un avviso del tipo «un tuo
   agente ha passato in chiaro quella che sembra una chiave di accesso allo strumento `X`», con il rimando
   all'azione e senza mai ripetere il valore.
7. **RF-7** — L'elenco delle forme riconosciute è **aggiornabile senza modificare il codice**, perché le forme dei
   segreti cambiano più in fretta di quanto si rilasci un'applicazione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Gli avvisi del campanello sono per account: ogni lettura filtra per
  `tenant_id` preso dal token verificato, e un account non vede mai i segreti trovati in un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova sull'ingresso: la redazione è un passaggio
  interno di `POST /api/agentaudit/v1/actions`. Rotta `GET /api/agentaudit/v1/alerts?type=secret` per l'elenco dei
  campanelli; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V7__redazione_e_campanelli.sql` sullo schema `app_agentaudit`: il
  marcatore di redazione sui parametri, il conteggio sulla riga dell'azione, e la tabella degli avvisi con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. La configurazione
  delle forme riconosciute è una risorsa di configurazione, non codice compilato.
- **RT-4 — Modulo frontend (§3, §5).** Il campanello compare come avviso nella sezione Cronologia e nella scheda
  dell'azione, con l'indicazione di che genere di segreto è stato trovato. Solo token del sistema di design; tema
  chiaro e scuro; il colore d'allarme resta libero perché l'accento dell'app è `violet` (§3 della
  [descrizione dell'applicazione](../application-description.md)).
- **RT-5 — Cinque lingue (§4).** I testi degli avvisi e i nomi dei generi di segreto («chiave di accesso»,
  «carta di pagamento», «codice fiscale», «password») passano dallo spazio-nomi `agentaudit` e sono presenti in
  `en, it, fr, es, de`. La storia non è conclusa se ne manca una.
- **RT-6 — Varchi e quota (§6, §7).** La redazione non consuma quota a parte: fa parte dell'accodamento
  dell'azione, che consuma già una unità della metrica `actions`. Gli avvisi del campanello non consumano nulla.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia; l'elenco dei campanelli sarà
  leggibile attraverso `elenca_azioni` e `riepiloga_attivita` (storia 0034), che sono strumenti di lettura.
- **RT-8 — Dati personali (§10).** Nessun dato personale **nuovo** viene introdotto — al contrario: questa storia
  ne **rimuove**. Va però dichiarato nel manifesto che il marcatore di redazione esiste e che cosa significa,
  perché un manifesto che non racconta la misura di minimizzazione è incompleto.
- **RT-9 — Registrazione eventi (§14).** L'evento «segreto redatto» è registrato con `tenant_id`, `app_id`,
  identificativo della sorgente, genere del segreto e identificativo di correlazione — **mai il valore, mai la sua
  impronta, mai una porzione del valore**.

## 4. Criteri di accettazione

**CA-1 — Il segreto non entra**
- **Dato** una dichiarazione con un parametro chiamato `api_key` dal valore `K`
- **Quando** l'azione viene registrata
- **Allora** al posto del valore c'è il marcatore «rimosso: chiave di accesso», l'impronta è calcolata sul valore
  redatto, e `K` non compare da nessuna parte nel sistema né nei registri tecnici

**CA-2 — Anche il valore travestito viene riconosciuto**
- **Dato** una dichiarazione con un parametro chiamato `nota` il cui valore ha la forma di una carta di pagamento
- **Quando** l'azione viene registrata
- **Allora** viene comunque redatto, con il marcatore che indica il genere «carta di pagamento»

**CA-3 — Il campanello suona**
- **Dato** una sorgente che dichiara un'azione contenente un segreto
- **Quando** la redazione lo intercetta
- **Allora** l'account riceve un avviso che nomina lo strumento e il genere di segreto, con il rimando all'azione,
  e senza ripetere il valore

**CA-4 — La redazione è dichiarata, non nascosta**
- **Dato** un'azione con due parametri redatti su cinque
- **Quando** si apre la sua scheda
- **Allora** la riga dichiara di essere stata redatta e su quanti parametri, così che nessuno pensi che il
  registro abbia perso qualcosa

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, entrambi con campanelli
- **Quando** un utente di `A` chiede l'elenco degli avvisi
- **Allora** vede solo i propri, anche forzando l'identificativo dell'altro account nella richiesta

**CA-6 — Le forme si aggiornano senza rilascio**
- **Dato** una forma di segreto non ancora riconosciuta
- **Quando** viene aggiunta alla configurazione delle forme
- **Allora** le dichiarazioni successive la riconoscono, senza modificare né ricompilare il codice

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul riconoscimento per nome e per forma, sui falsi positivi ragionevoli e sull'ordine
      redazione → impronta; prove di **integrazione** sull'ingresso e sugli avvisi, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** sugli avvisi;
- [ ] **prova specifica di non conservazione del segreto**: un caso che cerca il valore originale in tutto lo
      schema e nei registri tecnici e **fallisce se lo trova**;
- [ ] **prova end-to-end**: risposta «rimando» — il campanello entra nel percorso `[J-AGENTAUDIT]` alla storia
      0037, proprietaria della copertura; fino ad allora il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta l'esenzione motivata;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), compresi i nomi dei generi di
      segreto;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la descrizione della misura di redazione;
- [ ] **registro delle decisioni** compilato, con **due voci obbligatorie**: la redazione lato servizio e non
      lato chiamante (con il motivo), e la scelta di dire *che genere* di segreto è stato rimosso;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | La redazione è un passaggio della rotta di ingresso |
| storia `0009` | L'ordine conta: si redige **prima** di calcolare l'impronta, altrimenti l'impronta è del segreto |

## 7. Fuori ambito

- **la rilevazione semantica del contenuto** — capire che un testo libero *parla* di salute o di una persona: non
  si fa, non si promette e non si simula. La difesa contro le categorie particolari è **non conservare il
  contenuto** (§6 della descrizione dell'applicazione), non riconoscerlo;
- il blocco di un'azione perché contiene un segreto: qui si redige e si avvisa, non si nega. Negare in base al
  contenuto è materia delle regole (storia 0019) e sarebbe una scelta diversa;
- la conservazione volontaria del contenuto, dove il problema si ripresenta in forma più acuta: storia 0031.

## 8. Punti aperti

- **I falsi positivi e i falsi negativi sono entrambi inevitabili.** Una redazione troppo larga rovina la capacità
  di prova (si redige ciò che serviva); una troppo stretta lascia passare segreti. Propongo di sbagliare per
  eccesso di prudenza e di rendere l'elenco delle forme visibile al cliente, così che sappia cosa gli viene tolto.
  Da confermare con lo sviluppatore.
- **Il codice fiscale non è un segreto: è un dato personale.** L'ho messo fra le forme riconosciute perché il suo
  ingresso nel registro è il caso più probabile di dato personale non voluto, ma trattarlo come un segreto è una
  scelta discutibile: forse va redatto con un marcatore di genere diverso («rimosso: dato identificativo di
  persona»). Interseca la classificazione dei dati personali, che è una **fermata di escalation**: decide lo
  sviluppatore.
- **Se il campanello debba anche recapitarsi fuori dall'applicazione.** Un avviso che nessuno guarda non serve.
  Ma un avviso per posta elettronica che dice «il tuo agente perde chiavi» è anche un messaggio delicato da
  mandare. Propongo l'avviso in applicazione adesso e il recapito esterno insieme agli altri avvisi (storia 0026).
