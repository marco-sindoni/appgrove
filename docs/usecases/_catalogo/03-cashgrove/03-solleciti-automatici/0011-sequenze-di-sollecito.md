# 0011 — Sequenze di sollecito

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 03 — Solleciti automatici
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio decidere una volta per tutte quanti solleciti partono, quando e con che tono
> così da non dover ripensare ogni volta se è il momento di scrivere e con quali parole.

**Contesto.** Oggi il sollecito è un atto improvvisato: si manda quando ci si ricorda, e il tono dipende dall'umore.
I prodotti concorrenti danno tutti per scontata la sequenza configurabile — è la funzione base della categoria
([documento capofila](../application-description.md) §2.5) — ma la parte che conta per una micro-impresa non è la
ricchezza delle opzioni, è che il tono resti **suo**: il cliente moroso è spesso anche il cliente migliore. Questa
storia definisce il piano; l'esecuzione è della storia `0013`.

## 2. Requisiti funzionali

1. **RF-1** — L'utente crea una sequenza con un nome, un interruttore attiva/non attiva e un elenco ordinato di passi.
2. **RF-2** — Ogni passo dichiara: scarto in giorni rispetto alla data di scadenza (negativo = prima della scadenza,
   come promemoria di cortesia), canale, modello di messaggio e tono (cortese, fermo, formale).
3. **RF-3** — Due passi della stessa sequenza non possono avere lo stesso scarto in giorni; l'ordine dei passi segue lo
   scarto ed è ricalcolato dal servizio.
4. **RF-4** — Una sequenza si assegna a un credito in tre modi: predefinita dell'account, assegnata al debitore,
   assegnata al singolo credito — dove la più specifica vince.
5. **RF-5** — Alla fine dell'ultimo passo, se il credito è ancora aperto, esso passa a `in_escalation` e nessun altro
   sollecito automatico parte.
6. **RF-6** — L'account nasce con una sequenza predefinita già pronta (promemoria a −3 giorni, sollecito cortese a +7,
   fermo a +21, formale a +45): l'utente la può cambiare, ma non deve costruirne una per cominciare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle entità `SequenzaSolleciti` e `PassoSollecito`
  filtra per `tenant_id` preso dal token verificato; l'assegnazione di una sequenza a un credito verifica che entrambi
  appartengano allo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET`, `POST`, `PATCH`, `DELETE /api/crediti/v1/sequenze` (e
  `/{id}`, `/{id}/passi`); corpo validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per le tabelle `sequenza_solleciti` e `passo_sollecito` sullo schema
  `app_crediti`, con `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione logica. Una sequenza
  cancellata logicamente non si applica più, ma i solleciti già inviati continuano a riferirla.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Solleciti* del modulo `crediti`: elenco delle sequenze, editor dei
  passi con anteprima della linea del tempo («il cliente riceverà: −3 giorni, +7, +21, +45»); solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`. I **toni** hanno nomi tradotti, ma il loro identificativo tecnico resta stabile.
- **RT-6 — Varchi e quota (§6, §7).** Le sequenze non consumano quota. Restano i varchi di abilitazione e ruolo:
  modificare le sequenze richiede ruolo `owner` o `admin`, perché decide cosa arriva ai clienti dell'azienda.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: la configurazione delle sequenze
  non è una operazione da chat. Scelta esplicita, annotata con il motivo.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: una sequenza è una regola, non contiene persone. Le
  tabelle sono comunque aggiunte a `exportData` perché fanno parte della configurazione del cliente.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «sequenza creata», «sequenza modificata», «credito portato in
  escalation» sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati
  personali.
- **RT-10 — Condotta verso il debitore.** Il numero massimo di passi per sequenza e la distanza minima fra due passi
  sono limiti **del motore**, non impostazioni dell'utente: il vademecum del Garante considera invasiva la
  sollecitazione ripetuta, e un prodotto che permette dodici solleciti in dieci giorni mette il cliente nei guai.

## 4. Criteri di accettazione

**CA-1 — Sequenza predefinita al primo accesso**
- **Dato** un account che attiva CashGrove
- **Quando** apre la sezione *Solleciti*
- **Allora** trova una sequenza predefinita già attiva con quattro passi, e la linea del tempo la spiega a parole

**CA-2 — Passi con lo stesso scarto**
- **Dato** una sequenza con un passo a +7 giorni · **Quando** si aggiunge un altro passo a +7 · **Allora** la richiesta
  è respinta con `400` e un messaggio che dice quale passo è in conflitto

**CA-3 — Distanza minima**
- **Dato** una sequenza con un passo a +7 · **Quando** si aggiunge un passo a +8 e la distanza minima del motore è di 3
  giorni · **Allora** la richiesta è respinta spiegando il perché del limite

**CA-4 — La più specifica vince**
- **Dato** una sequenza predefinita dell'account, una assegnata al debitore Alfa e una assegnata al credito `2026/114`
  di Alfa
- **Quando** si consulta quale sequenza si applica a `2026/114`
- **Allora** è quella assegnata al credito

**CA-5 — Fine sequenza**
- **Dato** un credito che ha ricevuto tutti i passi e non è stato pagato
- **Quando** passa la data dell'ultimo passo
- **Allora** il credito è in stato `in_escalation` e nessun sollecito automatico parte più

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con sequenze proprie · **Quando** un utente di `A` tenta di assegnare a un proprio
  credito una sequenza di `B` · **Allora** riceve l'errore di risorsa non trovata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sulla risoluzione della sequenza applicabile (le tre precedenze) e di **integrazione** sulla
      risorsa `sequenze`;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulle rotte introdotte;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che percorre la sequenza dall'inizio alla fine;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: tabelle di configurazione presenti in esportazione;
- [ ] **registro delle decisioni** compilato, in particolare sui limiti del motore (numero di passi, distanza minima) e
      sul perché non sono configurabili;
- [ ] contratto degli **strumenti conversazionali**: esclusione deliberata, annotata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | Serve il credito a cui assegnare la sequenza |
| storia `0010` | Serve la macchina a stati che accoglie `in_escalation` |

## 7. Fuori ambito

- I testi dei messaggi: storia `0012`.
- L'esecuzione temporale dei passi: storia `0013`.
- La sospensione della sequenza: storia `0016`.

## 8. Punti aperti

I **valori** della sequenza predefinita (−3, +7, +21, +45) sono una proposta ragionevole ma non fondata su una fonte:
nessuno dei prodotti esaminati pubblica la propria cadenza predefinita. Li conferma lo sviluppatore.
