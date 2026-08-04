# 0027 — Esportazione del registro

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 05 — Lettura, ricerca e rendicontazione
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che deve portare il registro dove serve — un foglio di calcolo, il proprio sistema di sorveglianza
> della sicurezza, la cartella di una contestazione
> voglio esportare un intervallo di azioni nei formati che i miei strumenti sanno leggere
> così da non restare prigioniero della schermata di un fornitore quando ho bisogno dei miei dati.

**Contesto.** L'esportazione è una delle poche funzioni che la scheda di catalogo elenca esplicitamente, e per una
ragione solida: un registro che si consulta solo dentro la nostra interfaccia è un registro di cui il cliente non
dispone davvero. Serve in tre momenti diversi — mostrare a un revisore, riconciliare con i propri registri
tecnici, alimentare il proprio sistema di sorveglianza — e i tre momenti vogliono formati diversi.

Il terzo caso merita attenzione: esiste già una grammatica condivisa per descrivere l'uso di uno strumento da parte
di un agente. Lo standard aperto **OCSF** (schema aperto per gli eventi di sicurezza) viene esteso dallo standard
OWASP «*Agent Observability Standard*» con l'attività «uso di strumento da parte di un agente»
([owasp.github.io](https://owasp.github.io/www-project-agent-observability-standard/spec/trace/extend_ocsf/)).
Esportare in quella forma significa che i dati entrano nei sistemi del cliente senza che nessuno debba scrivere un
convertitore — ed è esattamente il genere di cosa che fa scegliere un prodotto invece di un altro.

**Attenzione, il punto che qualifica questa storia**: un'esportazione porta fuori identificativi di persone. È un
trattamento, non un comando di comodo. Va tracciata, e la traccia deve stare nel registro.

## 2. Requisiti funzionali

1. **RF-1** — Si può esportare un intervallo di azioni scegliendo i filtri della cronologia (storia 0024) e uno
   fra tre formati: **CSV** (per il foglio di calcolo), **JSON** (per chi programma), **forma normalizzata OCSF**
   (per i sistemi di sorveglianza della sicurezza).
2. **RF-2** — L'esportazione **non include mai i contenuti conservati** dei parametri, salvo richiesta esplicita e
   separata di chi ne ha il diritto (epica 06); include sempre la forma dei parametri e le impronte.
3. **RF-3** — L'esportazione di un intervallo grande è **asincrona**: si richiede, si viene avvisati quando è
   pronta, si scarica entro una scadenza dichiarata, dopodiché il file prodotto viene distrutto.
4. **RF-4** — Ogni esportazione è **tracciata**: chi l'ha chiesta, quando, quale intervallo, quali filtri, quale
   formato, quante righe. La traccia è **una riga del registro**, non una tabella di servizio.
5. **RF-5** — Il file esportato dichiara in testa il proprio contesto: account, intervallo, filtri applicati,
   momento di produzione, numero di righe — così che un file trovato in una cartella sei mesi dopo si spieghi da
   solo.
6. **RF-6** — L'esportazione rispetta il ruolo di chi la chiede: chi non può vedere una riga nella cronologia non
   può ottenerla in un file.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'estrazione filtra per `tenant_id` preso dal token verificato, con lo
  stesso livello di interrogazione della cronologia — **non una copia**, perché due percorsi di lettura separati
  sono due occasioni di sbagliare il filtro. Un file già prodotto è scaricabile solo da utenti dello stesso
  account, e il collegamento di scaricamento non è indovinabile.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/agentaudit/v1/exports` (richiesta, corpo
  validato con filtri e formato), `GET /api/agentaudit/v1/exports` (elenco con stato) e
  `GET /api/agentaudit/v1/exports/{id}/download`; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V…__esportazioni.sql` sullo schema `app_agentaudit`: tabella `exports`
  con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo, stato, filtri, formato, conteggio,
  scadenza del file. La cancellazione logica si usa normalmente su questa tabella; la **riga di registro**
  dell'esportazione (RF-4) sta invece nella catena in sola aggiunta.
- **RT-4 — Modulo frontend (§3, §5).** Comando di esportazione nella schermata di cronologia, che eredita i filtri
  correnti, più un pannello «esportazioni» con lo stato di quelle in corso; solo token del sistema di design;
  funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono presenti
  in `en, it, fr, es, de`. **Le intestazioni di colonna del file CSV no**: restano in inglese e stabili, perché un
  file le cui colonne cambiano nome con la lingua dell'utente rompe ogni foglio di calcolo costruito sopra. Questa
  è una deroga consapevole e va scritta nel registro delle decisioni.
- **RT-6 — Varchi e quota (§6, §7).** L'esportazione **non consuma** la metrica `actions` in proporzione alle
  righe estratte — sarebbe far pagare due volte lo stesso dato; consuma **una** unità per la riga di registro che
  la traccia. La richiesta è soggetta a un limite di frequenza per account, per evitare che una raffica di
  esportazioni diventi un modo di estrarre tutto in continuazione.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `prepara_esportazione` è dichiarato alla storia 0034
  ed è di **scrittura con conferma umana obbligatoria**: produce una bozza di esportazione che una persona deve
  confermare. Il motivo non è la reversibilità — un'esportazione si può cancellare — ma il fatto che porta fuori
  identificativi di persone, e quel passo lo autorizza una persona.
- **RT-8 — Dati personali (§10).** L'esportazione **è** un trattamento di dati personali: va dichiarata nel
  manifesto `docs/compliance/manifests/agentaudit.yaml` in italiano e inglese come finalità (mettere il titolare
  in condizione di disporre dei propri dati e di adempiere ai propri obblighi), con la propria durata — il file
  prodotto ha vita breve e la scadenza va dichiarata. La tabella `exports` contiene l'identificativo di chi ha
  esportato: campo annotato, tabella presente in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Richiesta, completamento, scaricamento e distruzione del file sono
  registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali nel
  registro tecnico.

## 4. Criteri di accettazione

**CA-1 — I tre formati**
- **Dato** un intervallo di 200 azioni
- **Quando** l'utente lo esporta nei tre formati
- **Allora** ottiene un file CSV leggibile da un foglio di calcolo, un file JSON con gli stessi campi, e un file
  nella forma normalizzata OCSF in cui ogni azione compare come attività «uso di strumento da parte di un agente»

**CA-2 — I contenuti non escono**
- **Dato** un intervallo che contiene azioni su uno strumento con conservazione dei contenuti attiva
- **Quando** l'utente esporta
- **Allora** il file contiene forma e impronte dei parametri e **nessun contenuto**, e il fatto è dichiarato
  nell'intestazione del file

**CA-3 — L'esportazione lascia traccia nel registro**
- **Dato** un'esportazione completata
- **Quando** si guarda la cronologia
- **Allora** compare una riga che dice chi ha esportato, quando, quale intervallo, quale formato e quante righe —
  e quella riga non si può cancellare

**CA-4 — Il file scade**
- **Dato** un'esportazione completata e mai scaricata
- **Quando** passa il termine dichiarato
- **Allora** il file non è più scaricabile ed è stato distrutto, mentre la riga di registro che ne attesta
  l'esistenza resta

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` e il collegamento di scaricamento di un'esportazione di `B`
- **Quando** un utente di `A` prova a usarlo
- **Allora** riceve `404`, e nulla del contenuto o dell'esistenza del file di `B` viene rivelato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sui tre formattatori (in particolare sulla corrispondenza dei campi verso la forma
      normalizzata) e di **integrazione** sul ciclo richiesta → produzione → scaricamento → scadenza, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sull'estrazione e sul collegamento di scaricamento;
- [ ] **prova end-to-end**: **rimando** — il percorso `[J-AGENTAUDIT]` della storia 0037 include un'esportazione;
      voce `da-coprire` nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con
      storia proprietaria `0037`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`) per l'interfaccia, con la deroga
      dichiarata sulle intestazioni del file;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la finalità di esportazione e la durata del file
      prodotto, con la tabella presente in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato, con le voci su: intestazioni del file non tradotte, quota consumata
      per la traccia e non per le righe, scadenza del file prodotto;
- [ ] contratto degli **strumenti conversazionali**: dichiarato che `prepara_esportazione` sarà di scrittura con
      conferma umana obbligatoria (storia 0034);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0024` | I filtri dell'esportazione sono quelli della cronologia, e il livello di interrogazione è lo stesso |
| storia `0002` | La riga di registro che traccia l'esportazione si accoda nella catena |
| Deposito dei file temporanei | Il file prodotto deve vivere da qualche parte con una scadenza; se la piattaforma non offre un deposito adatto, va deciso prima |

## 7. Fuori ambito

- **il pacchetto di prova verificabile da terzi**: è un'altra cosa e ha un'altra storia (0015). Questa
  esportazione serve a lavorare con i dati, quel pacchetto serve a dimostrare che i dati non sono stati toccati;
- **l'invio automatico e continuo** verso il sistema di sorveglianza del cliente: qui si esporta su richiesta o su
  programma (storia 0028), non si apre un flusso permanente;
- **l'esportazione dei contenuti conservati**: epica 06;
- **il rapporto leggibile da una persona**: storia 0028. Questa produce dati, non un documento.

## 8. Punti aperti

- **Per quanto tempo resta scaricabile un file prodotto.** Propongo poche ore, perché è un file pieno di
  identificativi che sta in un deposito: più vive, più è un rischio. Ma un'esportazione lunga chiesta il venerdì
  sera e scaricata il lunedì è un caso reale. Chi chiude: sviluppatore.
- **Se la forma normalizzata debba essere completa o parziale.** La corrispondenza fra i nostri campi e lo schema
  OCSF esteso non è uno a uno: alcuni nostri campi non hanno un posto previsto. Propongo di collocarli nella
  sezione non mappata prevista dallo schema, dichiarandolo, invece di inventare campi. Va verificato sullo schema
  aggiornato al momento dell'implementazione, perché è uno standard giovane e si muove.
- **Limite di frequenza delle esportazioni.** Serve, ma il numero giusto dipende dall'uso reale.
