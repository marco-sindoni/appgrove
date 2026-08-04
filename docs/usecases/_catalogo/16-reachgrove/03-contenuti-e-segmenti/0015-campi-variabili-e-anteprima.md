# 0015 — Campi variabili e anteprima

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 03 — Contenuti e segmenti
**Storia**: `0015` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che scrive a qualche centinaio di persone
> voglio inserire nel messaggio il nome di chi lo riceve e qualche altro dato che ho già
> così da mandare qualcosa che non sembri un volantino, senza rischiare che a qualcuno arrivi «Ciao {{nome}}».

**Contesto.** La personalizzazione è la funzione più richiesta e la più facile da sbagliare in modo visibile: il
campo variabile non risolto è l'errore che il destinatario nota di sicuro, perché gli arriva scritto in faccia. Il
problema non è tecnico ma di disciplina — chi compone non sa quali iscritti hanno il nome compilato e quali no.
Perciò qui si fanno due cose insieme, ed è il motivo per cui la storia è piccola: **valore di ripiego
obbligatorio** su ogni campo variabile, e anteprima su un destinatario scelto che mostra il messaggio come lo vedrà
lui. È anche il posto giusto per aggiungere il terzo controllo, quello che il controllo pre-volo (storia 0018) userà
come regola bloccante: nessun campo variabile può restare irrisolto al momento dell'invio.

## 2. Requisiti funzionali

1. **RF-1** — Dentro oggetto, blocchi di testo e piè di pagina si inseriscono campi variabili scelti da un elenco:
   i campi dell'iscritto (nome, cognome, lingua), i campi personalizzati definiti dall'account e i dati del mittente.
   L'inserimento avviene da un menù, non scrivendo la sintassi a mano.
2. **RF-2** — Ogni campo variabile porta un **valore di ripiego obbligatorio**, usato quando il dato manca per quel
   destinatario. Un campo variabile senza valore di ripiego non si salva.
3. **RF-3** — L'anteprima si calcola su un destinatario scelto dall'utente (ricerca fra i propri iscritti) oppure su
   un destinatario di esempio inventato, e mostra il messaggio con i valori sostituiti, sia nella versione grafica
   sia in quella in solo testo.
4. **RF-4** — Un controllo elenca, per il segmento scelto, **quanti destinatari userebbero il valore di ripiego** per
   ciascun campo variabile: è l'informazione che dice se la personalizzazione conviene o se farà brutta figura.
5. **RF-5** — Un campo variabile che si riferisce a un campo personalizzato **non più esistente** è segnalato come
   errore bloccante nel messaggio, non sostituito con una stringa vuota.
6. **RF-6** — Il sistema garantisce che nel testo consegnato non resti **nessuna** sequenza di campo variabile non
   risolta: se la sostituzione fallisce, l'invio a quel destinatario non parte e finisce fra gli scarti con il
   motivo, invece di partire sbagliato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'anteprima su un destinatario reale legge un iscritto dell'account che
  ne fa richiesta, filtrando per `tenant_id` dal token verificato: è il punto in cui una svista d'isolamento
  mostrerebbe il dato di un'altra azienda dentro un'anteprima. L'elenco dei campi personalizzati disponibili è
  anch'esso per account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/campaigns/v1/merge-fields` (elenco dei campi
  disponibili) e `POST /api/campaigns/v1/messages/{id}/preview` estesa con il destinatario su cui calcolare. Corpo
  validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: i campi variabili vivono dentro il corpo del messaggio
  (storia 0014) e i valori di ripiego con loro. Se serve un indice per la ricerca del destinatario dell'anteprima,
  si aggiunge sulla tabella `subscriber`, sempre a partire da `tenant_id`.
- **RT-4 — Modulo frontend (§3, §5).** Menù di inserimento nei blocchi di testo, riquadro dei valori di ripiego,
  selettore del destinatario dell'anteprima, riepilogo di quanti userebbero il ripiego. Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi dei campi variabili mostrati nel menù e i messaggi d'errore passano dallo
  spazio-nomi `campaigns` in `en, it, fr, es, de`. I **valori di ripiego** li scrive il cliente nella lingua del suo
  messaggio e non si traducono.
- **RT-6 — Varchi e quota (§6, §7).** L'anteprima **non** consuma la metrica `messages_sent` (natura `flow`) e non
  manda niente a nessuno: è calcolata e mostrata a schermo. Questo va detto anche nell'interfaccia, perché
  l'aspettativa opposta è comune. Valgono i varchi comuni; con abbonamento `canceled` la risorsa risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. `crea_bozza_di_campagna` (storia 0035)
  produce messaggi che rispettano queste regole: se il testo generato contiene un campo variabile senza valore di
  ripiego, la bozza è respinta con il motivo, non salvata a metà.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo che riguarda una persona: la storia legge dati già dichiarati
  (`subscriber.name`, `subscriber.custom_fields`). L'anteprima su un destinatario reale è però un **accesso in
  lettura a dati di un iscritto** e va detto nel manifesto sotto la finalità già esistente; nessuna copia
  dell'anteprima viene conservata.
- **RT-9 — Registrazione eventi (§14).** «Anteprima calcolata» e «campo variabile non risolvibile» con `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione e il **nome del campo**; mai il valore, mai il destinatario,
  mai il testo.

## 4. Criteri di accettazione

**CA-1 — Ripiego obbligatorio**
- **Dato** un messaggio con il campo variabile «nome» senza valore di ripiego
- **Quando** l'utente salva
- **Allora** il salvataggio è rifiutato con `400` e l'indicazione del campo che manca del valore di ripiego

**CA-2 — Anteprima su un destinatario reale**
- **Dato** un'iscritta senza nome compilato e un messaggio che inizia con «Ciao {nome}», ripiego «a tutti voi»
- **Quando** l'utente sceglie quell'iscritta per l'anteprima
- **Allora** l'anteprima mostra «Ciao a tutti voi», sia nella versione grafica sia in quella in solo testo

**CA-3 — Quanti useranno il ripiego**
- **Dato** un segmento di 900 destinatari, di cui 240 senza nome compilato
- **Quando** l'utente apre il controllo dei campi variabili
- **Allora** legge «240 destinatari su 900 vedranno il valore di ripiego per il campo nome»

**CA-4 — Campo personalizzato scomparso**
- **Dato** un messaggio che usa il campo personalizzato «città», poi eliminato dall'account
- **Quando** si apre il messaggio o si prova a mandarlo alla verifica
- **Allora** il campo è segnalato come errore bloccante con il nome del campo mancante, e non viene sostituito con
  una stringa vuota

**CA-5 — Niente sequenze irrisolte in uscita**
- **Dato** un destinatario per cui la sostituzione fallisce
- **Quando** la spedizione prepara il suo messaggio
- **Allora** quell'invio **non** parte, finisce fra gli scarti con il motivo, e il testo con la sequenza non
  risolta non lascia il sistema

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla sostituzione dei campi variabili — compresi ripiego, campo mancante e sequenza
      malformata — e di **integrazione** sull'anteprima, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sull'anteprima calcolata su un destinatario reale;
- [ ] **prova end-to-end**: nessun impatto diretto — l'anteprima non produce superficie propria nel percorso
      `[J-CAMPAIGNS]`; la regola «nessuna sequenza irrisolta» viene però verificata dal controllo pre-volo nella
      storia 0018, che è la sede della copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con i valori di ripiego esclusi;
- [ ] **manifesto dei dati**: nessuna voce nuova; annotata la lettura per anteprima sotto la finalità esistente;
- [ ] **registro delle decisioni** compilato, con annotato perché il valore di ripiego è obbligatorio;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; regola richiamata nella bozza da chat;
- [ ] controllo automatico di **accessibilità** verde sul menù di inserimento;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | I campi variabili leggono i dati dell'iscritto e i campi personalizzati |
| Storia `0014` | I campi variabili si inseriscono dentro i blocchi del messaggio |
| Storia `0013` | Il conteggio «quanti useranno il ripiego» si calcola su un segmento |

## 7. Fuori ambito

- la composizione dei blocchi: è la storia 0014;
- il controllo pre-volo che rende bloccante la regola dei campi irrisolti: è la storia 0018;
- l'invio di un messaggio di prova a un indirizzo dell'utente: è utile ma è una spedizione vera, consuma quota e va
  con la storia 0019, non qui;
- la generazione automatica del testo: è la storia 0036.

## 8. Punti aperti

- **Se il messaggio di prova all'utente debba consumare quota.** Non consumarla è comodo e apre una via per mandare
  messaggi gratis a indirizzi arbitrari; consumarla è coerente ma irrita. Proposta: consumarla, con un tetto basso
  di prove al giorno. Non è materia di questa storia: chiude lo sviluppatore insieme alla storia 0019.
