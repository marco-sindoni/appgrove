# 0009 — Minimizzazione dei parametri

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 02 — Sorgenti e ingresso delle azioni
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde dei dati della propria azienda
> voglio che il registro delle azioni non diventi il posto dove finiscono per sbaglio tutti i dati dei miei
> clienti
> così da poter tenere le prove senza aver creato, senza accorgermene, la raccolta di dati più grande e meno
> presidiata che ho.

**Contesto.** I parametri di una chiamata a uno strumento sono testo libero deciso da un agente: dentro può
esserci il codice fiscale di un cliente, il testo di un messaggio privato, una diagnosi. Un registro che li
conserva tutti diventa in poche settimane un problema più grande di quello che risolve, e per costruzione non si
può ripulire (§6.3 della [descrizione dell'applicazione](../application-description.md)). La risposta è non
conservarli: si conserva la **forma** e l'**impronta**. Non è timidezza nostra — è la postura standard del
settore: le convenzioni OpenTelemetry per l'intelligenza artificiale generativa **per impostazione predefinita non
registrano gli argomenti degli strumenti**, proprio perché possono contenere dati sensibili
(https://opentelemetry.io/docs/specs/semconv/registry/attributes/gen-ai/).

## 2. Requisiti funzionali

1. **RF-1** — Per impostazione predefinita il registro **non conserva il valore** di nessun parametro: dopo la
   scrittura, il valore non è più presente da nessuna parte nel sistema.
2. **RF-2** — Di ogni parametro si conserva la **forma**: nome, tipo, lunghezza del valore, e se era vuoto o
   assente.
3. **RF-3** — Di ogni parametro si conserva l'**impronta** del valore, calcolata con un **sale per account**
   tenuto separato dalle righe del registro.
4. **RF-4** — Esiste una funzione di **confronto**: si fornisce un valore, il servizio ricalcola l'impronta e dice
   se corrisponde a quella registrata. È così che si verifica l'affermazione «l'agente è stato chiamato con questo
   valore» senza aver conservato il valore.
5. **RF-5** — La scheda di un'azione mostra forma e impronta dei parametri in modo comprensibile, e dice
   esplicitamente che il contenuto non è conservato — così nessuno crede di aver perso qualcosa per un guasto.
6. **RF-6** — Lo stesso trattamento vale per il **risultato** dichiarato dell'azione e per la motivazione della
   scelta dello strumento (storia 0007), che sono anch'essi testo libero.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il sale è **per account**: due account che registrano lo stesso valore
  producono impronte diverse, così che nemmeno confrontando le righe si possa dedurre che due account hanno
  trattato lo stesso dato. Ogni lettura filtra per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/agentaudit/v1/actions/{id}/verify-parameter`
  per il confronto di un valore con l'impronta registrata; corpo validato; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit. La rotta **non restituisce mai** l'impronta, solo l'esito
  del confronto: altrimenti sarebbe un modo elegante per portarsi via ciò che non conserviamo.
- **RT-3 — Persistenza (§8).** Migrazione `V6__parametri_minimizzati.sql` sullo schema `app_agentaudit`: forma e
  impronta dei parametri come struttura collegata all'azione, e il sale per account in una tabella separata, con
  `tenant_id`, chiave primaria UUID versione 7 e colonne di controllo. Il sale **non** viene mai restituito da
  nessuna rotta.
- **RT-4 — Modulo frontend (§3, §5).** La scheda dell'azione (storia 0025) mostrerà forma e impronta; qui si
  consegna il componente che le rappresenta, con la frase che spiega perché il contenuto non c'è. Solo token del
  sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I testi «contenuto non conservato per scelta di minimizzazione», «valore
  corrispondente», «valore non corrispondente» passano dallo spazio-nomi `agentaudit` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La verifica di un parametro **non consuma** la metrica `actions`: è una
  lettura, e far pagare la verifica della prova sarebbe grottesco (§3 della descrizione). Richiede però un ruolo
  sufficiente, perché confrontare valori è un'operazione che dice qualcosa.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo in questa storia. **Dichiarazione**: la
  verifica di un parametro **non** verrà esposta all'assistente, perché sarebbe uno strumento con cui indovinare
  valori a tentativi.
- **RT-8 — Dati personali (§10).** Voce nuova nel manifesto in italiano e inglese: le **impronte dei parametri**,
  che riguardano chiunque compaia nei parametri. Vanno dichiarate proprio perché sono la misura che *riduce* il
  trattamento: il manifesto deve raccontare la verità, compresa quella buona. Campi annotati, tabella in
  `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** L'evento «parametro verificato» è registrato con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, **senza il valore fornito per il confronto** e senza l'impronta.

## 4. Criteri di accettazione

**CA-1 — Il contenuto non resta**
- **Dato** una dichiarazione con un parametro `codice_cliente` dal valore `X`
- **Quando** l'azione viene registrata
- **Allora** nel sistema si trovano nome, tipo, lunghezza e impronta del parametro, e **il valore `X` non è
  presente da nessuna parte**, né nella riga né nei registri tecnici

**CA-2 — L'impronta prova senza conservare**
- **Dato** un'azione registrata con quel parametro
- **Quando** si chiede il confronto fornendo il valore `X`
- **Allora** la risposta è «corrisponde»; fornendo un valore diverso la risposta è «non corrisponde»; in nessuno
  dei due casi la risposta contiene l'impronta

**CA-3 — Il sale separa gli account**
- **Dato** due account `A` e `B` che registrano un parametro con lo **stesso** valore
- **Quando** si confrontano le due righe
- **Allora** le impronte sono diverse, e un utente di `A` non può verificare un parametro di `B` nemmeno forzando
  l'identificativo dell'altro account nella richiesta

**CA-4 — L'interfaccia dice la verità**
- **Dato** la scheda di un'azione
- **Quando** un utente la apre
- **Allora** legge che il contenuto dei parametri non è conservato per scelta, e non ha modo di scambiare
  l'assenza per un guasto

**CA-5 — Anche il risultato è minimizzato**
- **Dato** una dichiarazione che porta un risultato testuale lungo
- **Quando** viene registrata
- **Allora** del risultato restano lunghezza e impronta, non il testo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dell'impronta con sale e sul confronto, e di **integrazione** sulla rotta di
      verifica, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sui sali e sulle verifiche;
- [ ] **prova specifica di non conservazione**: un caso di prova che cerca il valore in chiaro in tutto lo schema
      e nei registri tecnici e **fallisce se lo trova**. È la prova più importante della storia;
- [ ] **prova end-to-end**: risposta «rimando» — la scheda dell'azione entra nel percorso `[J-AGENTAUDIT]` alla
      storia 0037, proprietaria della copertura; fino ad allora il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta l'esenzione motivata;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le impronte dei parametri e il sale, tabelle
      presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con **due voci obbligatorie**: la non conservazione del contenuto
      come impostazione predefinita (con il riferimento alla postura OpenTelemetry) e il sale per account;
- [ ] contratto degli **strumenti conversazionali**: nessuno, e il divieto di esporre la verifica è dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | I parametri arrivano dalla rotta di ingresso: qui si decide cosa se ne fa |
| storia `0002` | L'impronta del parametro entra nella forma canonica su cui si calcola l'impronta dell'evento |

## 7. Fuori ambito

- la **rimozione dei segreti**, che è un problema diverso e va risolto anche prima dell'impronta: storia 0010;
- la **conservazione volontaria del contenuto** per singolo strumento, con cifratura e chiave separata: storie
  0030 e 0031;
- qualunque analisi del contenuto dei parametri: non conservandolo, non si può fare — ed è voluto.

## 8. Punti aperti

- ⚠️ **Robustezza dell'impronta sui valori a bassa entropia — da valutare con chi presidia la sicurezza.** Un
  indirizzo di posta elettronica, un numero di telefono, un codice fiscale hanno uno spazio di valori piccolo
  abbastanza da poter essere **indovinati per tentativi** da chi conoscesse il sale. Il sale è per account e sta in
  una tabella separata, il che alza l'asticella ma non la elimina. Mitigazioni possibili: una funzione di
  derivazione lenta invece di una impronta semplice (costa in prestazioni), oppure la troncatura dell'impronta
  (riduce la capacità di prova). È il punto 10 dei rischi della descrizione dell'applicazione, e va deciso da chi
  presidia la sicurezza, non qui.
- **Dove custodire il sale.** Nella stessa base di dati è comodo e debole; in un deposito di segreti separato è
  più forte e più complicato. Propongo il deposito separato, coerentemente con la scelta della storia 0030 per le
  chiavi di contenuto. Da confermare.
- **Cosa fare del sale quando si cancella un account.** Distruggerlo rende le impronte non più verificabili: è una
  perdita di capacità di prova, ma anche una forma di cancellazione. Interseca il §6.2 della descrizione ed è
  materia della revisione legale, non di questa storia.
