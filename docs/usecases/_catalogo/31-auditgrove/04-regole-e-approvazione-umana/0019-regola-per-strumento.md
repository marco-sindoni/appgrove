# 0019 — Regola per strumento

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 04 — Regole e approvazione umana
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio stabilire, strumento per strumento, che cosa un agente può fare da solo e che cosa deve farmi approvare
> così da lasciar correre il lavoro ordinario e fermarmi solo dove il danno sarebbe irreparabile.

**Contesto.** Il catalogo della storia 0018 dice che cosa gli agenti hanno per le mani. Questa storia è il momento
in cui il cliente prende posizione: per ogni strumento, una regola fra tre — **consenti**, **nega**, **richiedi
approvazione**.

Due scelte di progetto meritano di stare qui e non in una nota.

La prima: **la regola predefinita per gli strumenti nuovi**. Un agente che comincia a usare uno strumento mai
visto non può essere lasciato libero per il solo fatto che nessuno ha ancora scritto una regola. La proposta è:
*richiedi approvazione* per le classi di effetto cancellazione, pagamento e invio verso l'esterno; *consenti* per
la lettura. È una proposta da confermare, e la conseguenza va vista in faccia: la prima volta che un agente prova
uno strumento distruttivo nuovo, si ferma. È scomodo, ed è il comportamento giusto.

La seconda: **le regole sono versionate, non modificate**. Cambiare una regola non sovrascrive la precedente:
scrive una riga nuova nel registro, con chi e quando. Il motivo è ovvio a posteriori e si dimentica sempre: quando
si guarderà indietro a un'azione consentita sei mesi fa, la domanda sarà *«quale regola valeva allora?»*, e una
tabella sovrascritta non sa rispondere.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni strumento del catalogo esiste una **regola vigente** fra tre: `consenti`, `nega`,
   `richiedi approvazione`.
2. **RF-2** — Uno strumento senza regola scritta da una persona ricade nella **regola predefinita per classe di
   effetto**, che è configurabile per account e ha i valori proposti sopra.
3. **RF-3** — Scrivere o cambiare una regola **non modifica la precedente**: crea una versione nuova con la data
   da cui vale, e lascia leggibile lo storico completo delle versioni con chi le ha scritte.
4. **RF-4** — Data un'azione e un momento, il sistema sa dire **quale versione della regola era vigente** in quel
   momento: è la domanda che ci si farà guardando indietro.
5. **RF-5** — Cambiare una regola richiede un ruolo amministrativo dell'account; ogni cambio è una riga del
   registro e produce un avviso a chi sorveglia, perché ammorbidire una regola è esattamente il gesto che va
   guardato.
6. **RF-6** — Esiste una schermata «Regole» che mostra gli strumenti con la loro regola vigente, permette di
   cambiarla e mostra lo storico delle versioni di ciascuna.
7. **RF-7** — Una regola può essere scritta anche per uno strumento **non ancora comparso**, purché il nome sia
   noto al cliente: serve a prepararsi prima di mettere in produzione un agente nuovo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le regole sono per account: due account possono avere regole opposte
  sullo stesso nome di strumento. Ogni lettura e scrittura filtra per `tenant_id` preso dal token verificato; un
  `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/agentaudit/v1/policies` (elenco delle regole
  vigenti), `PUT /api/agentaudit/v1/policies/{toolId}` (nuova versione) e
  `GET /api/agentaudit/v1/policies/{toolId}/versions` (storico); corpo validato con i soli tre valori ammessi;
  errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__regole.sql` sullo schema `app_agentaudit`: tabella `policies` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo, riferimento allo strumento, decisione,
  momento da cui vale, autore. La tabella è **in sola aggiunta**, come le azioni e per la stessa ragione: una
  versione di regola è una prova di ciò che era stato deciso. La regola «vigente» è la versione più recente con
  data di validità non futura, non una riga marcata.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Regole» nel manifesto del modulo `agentaudit`; dati letti
  con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro. Le tre decisioni
  usano i **colori funzionali** del sistema di design (verde, ambra, rosso) e non l'accento dell'app: è la ragione
  per cui l'app ha `violet` come colore-categoria (§3 della descrizione dell'applicazione).
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono presenti
  in `en, it, fr, es, de`. I tre nomi delle decisioni sono i termini più importanti dell'intera applicazione: una
  traduzione ambigua produce una configurazione sbagliata, e una configurazione sbagliata produce un'azione
  irreversibile non fermata.
- **RT-6 — Varchi e quota (§6, §7).** La scrittura di una regola **non consuma** la metrica `actions`: si paga per
  registrare ciò che fanno gli agenti, non per governarli, e mettere un prezzo sul governo sarebbe un incentivo
  storto. Il cambio richiede ruolo amministrativo (`403` altrimenti); con abbonamento non attivo risponde `402`,
  ma **le regole restano applicate**: un abbonamento scaduto non deve trasformarsi in un via libera.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `proponi_regola(strumento, decisione,
  motivo) → bozza di regola`, marcato **scrittura**, con **conferma umana obbligatoria**: prepara la regola e
  **non la applica**. Il motivo è lo stesso per cui l'approvazione non è esposta affatto (§7 della descrizione
  dell'applicazione): un assistente che potesse ammorbidire da solo la regola che lo governa renderebbe inutile il
  presidio. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora
  implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: una regola contiene un nome di strumento, una
  decisione, una data e l'identificativo dell'autore, già dichiarato nel manifesto per le altre righe. Nessun
  campo di testo libero in questa storia — il motivo scritto è quello dell'**approvazione** (storia 0021), non
  quello della regola.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `regola creata`, `regola ammorbidita` e `regola irrigidita`
  sono registrati distintamente con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati
  personali. La distinzione fra i due ultimi non è cosmetica: è l'informazione che serve a chi guarderà il
  registro dopo un incidente.

## 4. Criteri di accettazione

**CA-1 — La regola si applica**
- **Dato** uno strumento con regola `richiedi approvazione`
- **Quando** si consulta la regola vigente per quello strumento
- **Allora** la risposta è `richiedi approvazione` con la data da cui vale e l'indicazione di chi l'ha scritta

**CA-2 — Lo strumento nuovo ricade nella predefinita**
- **Dato** un account con la regola predefinita proposta e nessuna regola scritta per lo strumento
  `cancella_archivio`, di classe di effetto cancellazione
- **Quando** quello strumento compare per la prima volta
- **Allora** la regola vigente risulta `richiedi approvazione`, ed è dichiarato che deriva dalla predefinita e non
  da una scelta esplicita

**CA-3 — Lo storico risponde alla domanda giusta**
- **Dato** uno strumento la cui regola è passata da `richiedi approvazione` a `consenti` il primo marzo
- **Quando** si chiede quale regola era vigente il quindici febbraio
- **Allora** la risposta è `richiedi approvazione`, e la versione precedente resta leggibile con il suo autore e
  la sua data

**CA-4 — Chi non ha il ruolo non cambia le regole**
- **Dato** un utente con il solo permesso di lettura
- **Quando** tenta di scrivere una regola nuova
- **Allora** riceve `403`, nessuna versione viene creata, e il tentativo non altera la regola vigente

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con uno strumento omonimo e regole opposte
- **Quando** un utente di `A` consulta e cambia la propria regola, anche forzando l'identificativo dell'altro
  account nella richiesta
- **Allora** vede e modifica esclusivamente la regola di `A`, e quella di `B` resta intatta

**CA-6 — L'abbonamento scaduto non è un via libera**
- **Dato** un account con abbonamento in stato che non dà accesso all'app
- **Quando** si consulta la regola vigente per uno strumento
- **Allora** l'interfaccia risponde `402`, ma le regole restano registrate e applicabili, e nulla viene
  interpretato come `consenti`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla risoluzione della regola vigente a una data e sulla ricaduta nella predefinita, e
      di **integrazione** sulla creazione di versioni, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle regole e sullo storico delle versioni;
- [ ] **prova end-to-end**: risposta «coprire ora» — la schermata «Regole» è superficie utente; il percorso
      `[J-AGENTAUDIT]` riceve il passo «apri Regole, imposta *richiedi approvazione* su uno strumento, verifica lo
      storico», e il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) viene aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), con verifica specifica che i tre
      nomi delle decisioni non siano ambigui in nessuna;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con **due voci obbligatorie**: la
      regola predefinita proposta per gli strumenti nuovi, e la scelta di versionare invece di modificare;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `proponi_regola`, marcato scrittura con
      conferma umana;
- [ ] controllo automatico di **accessibilità** sulla schermata «Regole»;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` | La regola si appende a uno strumento del catalogo: senza catalogo non c'è dove appenderla |
| storia `0003` | Serve il guscio del modulo frontend per appendere la sezione «Regole» |

## 7. Fuori ambito

- **l'applicazione della regola**, cioè il momento in cui un agente chiede e riceve una risposta: storia 0020.
  Qui si scrive la regola, non la si esegue;
- l'approvazione da parte di una persona: storia 0021;
- regole condizionate sui valori dei parametri (per esempio «approva solo sopra i mille euro»): deliberatamente
  fuori, vedi punti aperti — e non è un rinvio da poco, perché è la prima cosa che un cliente chiederà;
- l'ereditarietà delle regole per sorgente o per gruppo di strumenti: fuori, stesso motivo.

## 8. Punti aperti

- **Regole condizionate sui parametri.** «Consenti sotto i cento euro, fai approvare sopra» è la richiesta più
  prevedibile del mondo, e questa storia non la copre. Il motivo non è pigrizia: **il registro per impostazione
  predefinita non conserva i valori dei parametri**, ma solo la loro forma e la loro impronta (§6.3 della
  descrizione dell'applicazione). Una regola condizionata sui valori obbligherebbe l'app a leggere i valori nel
  momento della decisione — cosa che può fare, perché li riceve — senza però conservarli. È fattibile e va
  progettato con attenzione, perché è il punto in cui una funzione comoda può erodere il presidio sulla
  minimizzazione. Proprietaria naturale: una storia futura dell'epica 04. Chi chiude: sviluppatore.
- ⚠️ **La regola predefinita per gli strumenti nuovi è una proposta.** Fermarsi alla prima comparsa di uno
  strumento distruttivo è il comportamento prudente e produce attrito reale il primo giorno di ogni nuovo agente.
  L'alternativa — consentire e avvisare — è più gradevole e lascia passare esattamente l'incidente che il prodotto
  esiste per evitare. Chi chiude: sviluppatore.
- **Chi viene avvisato quando una regola viene ammorbidita.** L'avviso ha senso solo se arriva a qualcuno di
  diverso da chi l'ha ammorbidita, e in un'azienda di tre persone quel qualcuno potrebbe non esistere. Chi chiude:
  sviluppatore, insieme alla storia 0026.
