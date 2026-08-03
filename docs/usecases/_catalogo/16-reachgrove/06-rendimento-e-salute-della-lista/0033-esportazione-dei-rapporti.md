# 0033 — Esportazione dei rapporti

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 06 — Rendimento e salute della lista
**Storia**: `0033` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0030`, `0032` — è l'ultima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve mostrare al commercialista, al socio o a sé stesso come vanno le campagne
> voglio portare fuori i numeri in un file che si apra in un foglio di calcolo
> così da poterli usare dove servono, sapendo cosa quel file contiene e di cosa rispondo io.

**Contesto.** L'esportazione è la funzione più banale dell'epica e quella che fa uscire i dati dal perimetro
dell'app: da quel momento il file vive su un portatile, in una casella di posta, in una cartella condivisa, e la
piattaforma non ha più nessun controllo. Per questo l'esportazione qui fa due cose che di solito non fa: separa i
**numeri** dagli **elenchi di recapiti**, e scrive nel file stesso a quali condizioni quei dati si possono usare.
Arriva alla fine dell'epica perché esporta ciò che le storie 0030 e 0032 hanno calcolato.

## 2. Requisiti funzionali

1. **RF-1** — Si può esportare in un file tabellare il rapporto di una campagna (storia 0030) oppure la
   composizione della lista con i tassi (storia 0032), scegliendo il periodo.
2. **RF-2** — L'esportazione dei **numeri** non contiene recapiti né nomi: contiene conteggi, percentuali e motivi
   di esclusione. È l'esportazione predefinita.
3. **RF-3** — Gli elenchi che contengono recapiti — rimbalzi permanenti e disiscritti — sono un'esportazione
   **distinta**, che si sceglie apposta, richiede ruolo `owner` o `admin` e mostra prima di partire quante righe
   con dati di persone conterrà e a cosa servono (tenere pulite le liste altrove, non ricontattare).
4. **RF-4** — Ogni file esportato porta un'intestazione con: account, periodo, momento di generazione, chi l'ha
   generato e un'**avvertenza scritta** — i dati riguardano persone, il titolare del trattamento verso di loro è
   il cliente, e riusarli fuori dall'app è un trattamento di cui risponde lui.
5. **RF-5** — Esiste un registro delle esportazioni — chi, quando, quale tipo, quante righe — consultabile dal
   titolare dell'account, perché la domanda «chi ha portato fuori questi dati?» deve avere una risposta.
6. **RF-6** — L'esportazione **non è** l'esercizio dei diritti dell'interessato: quella resta la funzione comune
   della piattaforma e resta accessibile anche quando l'app è disabilitata o l'abbonamento è scaduto. La
   differenza va detta nell'interfaccia, perché è esattamente il punto in cui si confondono.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La generazione del file filtra per `tenant_id` preso dal token
  verificato; il collegamento di scarico contiene un riferimento opaco e viene verificato di nuovo contro
  l'account del chiamante al momento dello scarico — un collegamento indovinato non deve poter aprire il file di
  un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/campaigns/v1/exports` (avvia) e
  `GET /api/campaigns/v1/exports/{id}` (stato e scarico); corpo validato; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit. Sopra una dimensione la generazione è asincrona e il
  collegamento ha una scadenza.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__add_report_export.sql` sullo schema `app_campaigns`: tabella
  `report_export` con `tenant_id`, chiave primaria UUID versione 7, tipo, periodo, numero di righe,
  identificativo di chi ha esportato, scadenza, colonne di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Pulsante di esportazione nelle sezioni «Rapporto» e «Salute della lista»,
  con la scelta fra numeri ed elenchi, la finestra di conferma per il secondo caso e il registro delle
  esportazioni; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `campaigns` in `en, it, fr, es, de`.
  **Le intestazioni delle colonne del file e l'avvertenza seguono la lingua dell'interfaccia di chi esporta**, e
  la scelta va scritta: un file con le colonne in una lingua e i testi in un'altra è inutilizzabile.
- **RT-6 — Varchi e quota (§6, §7).** Esportare non consuma la metrica `messages_sent` (natura `flow`).
  L'esportazione dei numeri è accessibile a tutti i ruoli; quella con i recapiti richiede `owner` o `admin`,
  altrimenti `403`. Con abbonamento `canceled` risponde `402`, mentre i diritti dell'interessato restano
  accessibili in ogni caso (RF-6).
- **RT-7 — Esposizione conversazionale (§12).** L'esportazione **non è esposta** alla chat: produrre un file con
  recapiti su richiesta vocale è un'uscita di dati troppo facile da provocare per sbaglio. Gli stessi numeri sono
  già leggibili con `statistiche_campagna` e `salute_della_lista` (storia 0034). Motivazione da scrivere nel
  contratto. Livello conversazionale non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** L'esportazione con recapiti è un trattamento con una finalità propria: va
  dichiarata nel manifesto `docs/compliance/manifests/campaigns.yaml` in italiano e inglese, insieme alla tabella
  `report_export` (che contiene l'identificativo di chi ha esportato), e la tabella entra in `exportData` e
  `purgeData`. I file generati scadono e vengono rimossi: un archivio di file dimenticati è un archivio di dati
  personali dimenticati.
- **RT-9 — Registrazione eventi (§14).** «Esportazione richiesta» e «esportazione scaricata» con `tenant_id`,
  `app_id`, `user_id`, tipo, numero di righe e identificativo di correlazione; mai il contenuto.

## 4. Criteri di accettazione

**CA-1 — I numeri escono senza recapiti**
- **Dato** il rapporto di una campagna
- **Quando** si esporta nel modo predefinito
- **Allora** il file contiene conteggi, percentuali e motivi di esclusione, e nessun indirizzo di posta né nome

**CA-2 — L'elenco con i recapiti è una scelta esplicita**
- **Dato** un utente con ruolo `member`
- **Quando** prova a esportare l'elenco dei disiscritti
- **Allora** riceve `403`; con ruolo `admin` l'esportazione parte, ma solo dopo una conferma che dice quante righe
  con dati di persone conterrà

**CA-3 — L'avvertenza è dentro il file**
- **Dato** un file esportato
- **Quando** lo si apre in un foglio di calcolo
- **Allora** in testa compaiono account, periodo, momento, chi ha esportato e l'avvertenza sull'uso dei dati

**CA-4 — Il registro risponde alla domanda**
- **Dato** tre esportazioni fatte da due utenti diversi
- **Quando** il titolare apre il registro delle esportazioni
- **Allora** vede chi, quando, quale tipo e quante righe

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con esportazioni proprie
- **Quando** un utente di `A` usa il collegamento di scarico di un file di `B`
- **Allora** riceve `404` e il file non viene servito

**CA-6 — I file scadono**
- **Dato** un file esportato oltre la scadenza dichiarata
- **Quando** si tenta di scaricarlo
- **Allora** non è più disponibile, e il registro conserva comunque la riga di chi l'aveva generato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione del file e sull'assenza di recapiti nell'esportazione predefinita, e di
      **integrazione** sulle due rotte con generazione asincrona;
- [ ] prova di **isolamento fra account** sul collegamento di scarico;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) esporta i numeri e verifica
      l'avvertenza e l'assenza di recapiti; voce aggiunta al registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, intestazioni delle colonne comprese;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `report_export` e per la finalità
      dell'esportazione con recapiti;
- [ ] **registro delle decisioni** compilato, con annotata la separazione fra esportazione di numeri ed
      esportazione di recapiti;
- [ ] contratto degli **strumenti conversazionali**: esportazione **non** esposta, con la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sulla finestra di conferma;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0030` | Esporta i numeri del rapporto di campagna |
| Storia `0032` | Esporta la composizione della lista e i tassi |

## 7. Fuori ambito

- l'esportazione dell'anagrafica completa degli iscritti: non è un rapporto, ed è la funzione che trasformerebbe
  l'app in un distributore di liste. Se servirà, sarà una storia sua con le proprie cautele;
- l'invio automatico del rapporto per posta elettronica a scadenza fissa: rimandato, perché è una comunicazione
  ricorrente e va progettata come tale;
- l'esportazione dei dati dell'interessato: è la funzione comune di piattaforma, non questa.

## 8. Punti aperti

- **Durata di validità del collegamento di scarico** e **conservazione dei file generati**: proposta di
  settantadue ore per il collegamento e rimozione del file alla scadenza; è una scelta operativa che lo
  sviluppatore conferma insieme alla politica comune della piattaforma sugli allegati temporanei.
