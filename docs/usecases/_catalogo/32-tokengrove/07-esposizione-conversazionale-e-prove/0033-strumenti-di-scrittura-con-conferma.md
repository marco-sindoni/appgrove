# 0033 — Strumenti di scrittura con conferma

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0033` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0020`, `0021`, `0023`, `0025`, `0032`
**Ultimo aggiornamento**: 2026-08-04

## 1. Narrazione

> Come titolare che chiede all'assistente «metti un tetto di 500 € al mese sul progetto Alfa»
> voglio vedere **prima** che cosa verrà creato e doverlo approvare io
> così da usare la chat anche per cambiare le cose, senza il timore che una frase male interpretata riscriva i miei
> conti.

**Contesto.** La regola di sicurezza del catalogo non è negoziabile: gli strumenti di **lettura** sono liberi,
quelli di **scrittura** con effetti irreversibili producono una **bozza** e richiedono una **conferma umana
esplicita** — l'intelligenza artificiale prepara, la persona approva (§12 dei principi di piattaforma). In questa
app il caso più delicato non è creare un budget, che si cancella: è **riscrivere l'attribuzione di dati passati** su
cui il cliente può aver già fatturato un cliente finale (storia `0021`, `0017`, `0022`). Quel comando cambia numeri
che sono già usciti dall'app: la conferma non è una cortesia, è l'unica cosa che sta fra una frase ambigua e una
riconciliazione contabile da rifare.

## 2. Requisiti funzionali

1. **RF-1** — Sono dichiarati **quattro** strumenti di scrittura, con la firma già fissata al §7 del documento
   capofila, tutti con conferma umana obbligatoria:
   - `definisci_budget(ambito, importo, periodo, soglie)` → bozza di budget;
   - `crea_regola_di_attribuzione(condizione, assegnazioni, valida_da)` → bozza di regola;
   - `applica_regola_allo_storico(regola, periodo)` → bozza **con l'anteprima del numero di misure e dell'importo
     che cambierebbero attribuzione**;
   - `sospendi_avvisi(budget, fino_a, motivo)` → bozza.
2. **RF-2** — Nessuno strumento di scrittura **esegue** al momento dell'invocazione: produce una **bozza** con
   identificativo, contenuto normalizzato, effetti previsti in parole e scadenza. L'esecuzione avviene solo con una
   conferma successiva ed esplicita di una persona.
3. **RF-3** — La bozza dichiara **che cosa cambierà**, non che cosa è stato chiesto: per `definisci_budget` il tetto
   e le soglie con i destinatari; per `crea_regola_di_attribuzione` quante misure future toccherebbe la condizione;
   per `applica_regola_allo_storico` il numero di misure, l'importo e il periodo interessati, con l'avvertimento se
   quel periodo è già stato esportato o ribaltato (storie `0030`, `0022`); per `sospendi_avvisi` fino a quando si
   resterà senza avvisi e su quali budget.
4. **RF-4** — Una bozza **scade** se non confermata; una bozza confermata **non si riconferma**; se lo stato del
   mondo è cambiato fra la bozza e la conferma (il periodo ha ricevuto misure nuove, la regola è stata modificata,
   il budget non esiste più) la conferma è **rifiutata** e si chiede una bozza nuova.
5. **RF-5** — La conferma la dà una **persona identificata**, con ruolo sufficiente per l'operazione, e resta a
   registro: chi ha chiesto, che cosa diceva la bozza, chi ha confermato, quando, e che cosa è successo.
6. **RF-6** — Le operazioni di scrittura dichiarate da altre storie e destinate alla chat — generare un prospetto di
   ribaltamento (storia `0022`), esportare in forma dettagliata (storia `0030`), programmare un rapporto periodico
   (storia `0031`) — seguono **questo stesso protocollo** e sono registrate in questo stesso contratto: non esiste
   una seconda via di scrittura conversazionale nell'app.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Bozza e conferma vivono nel solo `tenant_id` del gettone verificato. Una
  bozza creata in un account non è confermabile da un altro, nemmeno conoscendone l'identificativo.
- **RT-2 — Bozza e conferma come stato persistito (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella
  `bozza_operazione` con `tenant_id`, strumento, parametri normalizzati, effetti previsti, impronta dello stato del
  mondo al momento della bozza, stato (`in_attesa` → `confermata` | `scaduta` | `rifiutata`), scadenza, chi ha
  chiesto, chi ha confermato; chiave primaria UUID versione 7, colonne di controllo, cancellazione logica.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `POST /api/spesa_modelli/v1/bozze` (creazione),
  `GET /api/spesa_modelli/v1/bozze/{id}`, `POST /api/spesa_modelli/v1/bozze/{id}/conferma`; la conferma è
  **idempotente**: ripeterla non esegue due volte. Errori in `problem+json` che distinguono «scaduta», «già
  confermata», «stato del mondo cambiato», «ruolo insufficiente»; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-4 — Contratto dentro il servizio (§12).** Le definizioni degli strumenti vivono in `services/spesa_modelli`
  accanto a quelle di lettura (storia `0032`), marcate **scrittura** e — per `applica_regola_allo_storico` —
  **scrittura irreversibile**. Il server conversazionale è di piattaforma e non esiste ancora (UC 0061-0064).
- **RT-5 — Varchi, ruoli e quota (§6, §7).** La conferma attraversa la stessa catena di varchi dell'azione
  equivalente fatta a mano: abbonamento (`402`), ruolo (`403` — definire budget e applicare regole allo storico
  sono di `owner` e `admin`), quota. Le operazioni che non registrano misure **non consumano**
  `misure_registrate`, come già stabilito dalle storie proprietarie.
- **RT-6 — Modulo frontend (§3, §5).** Le bozze in attesa sono visibili e confermabili anche **dentro l'app**, non
  solo in chat: chi riceve una notifica di bozza deve poterla leggere e approvare dall'interfaccia. Solo token del
  sistema di design; tema chiaro e scuro.
- **RT-7 — Cinque lingue (§4).** Descrizioni degli strumenti, testo degli effetti previsti e messaggi di rifiuto in
  `en, it, fr, es, de`. Il testo degli effetti previsti è quello che non va ammorbidito né accorciato in
  traduzione: è ciò su cui la persona decide.
- **RT-8 — Dati personali (§10).** I parametri possono contenere valori di etichetta riferibili a persone: la
  tabella `bozza_operazione` entra in `exportData` e `purgeData` (storia `0035`) e porta l'identificativo di chi ha
  chiesto e di chi ha confermato, dichiarati nel manifesto in italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** Eventi «bozza creata», «bozza confermata», «bozza scaduta», «conferma
  rifiutata per stato cambiato» con `tenant_id`, `app_id`, `user_id`, strumento ed esito — **senza** parametri,
  importi né valori di etichetta.

## 4. Criteri di accettazione

**CA-1 — La bozza non esegue**
- **Dato** l'invocazione di `definisci_budget` con ambito, importo, periodo e soglie
- **Quando** lo strumento risponde
- **Allora** esiste una bozza con gli effetti previsti in parole, **nessun budget è stato creato**, e la conferma è
  richiesta esplicitamente

**CA-2 — Il caso irreversibile mostra i numeri prima**
- **Dato** una regola e un intervallo di due mesi già esportato
- **Quando** si invoca `applica_regola_allo_storico`
- **Allora** la bozza dichiara quante misure e quale importo cambierebbero attribuzione **e** avverte che quel
  periodo è già uscito dall'app, con data e autore dell'esportazione

**CA-3 — Lo stato del mondo è cambiato**
- **Dato** una bozza di applicazione allo storico e, dopo di essa, l'arrivo di misure in ritardo nello stesso
  periodo
- **Quando** si conferma
- **Allora** la conferma è rifiutata con la spiegazione, nulla viene modificato, e si può chiedere una bozza nuova

**CA-4 — Conferma idempotente**
- **Dato** una conferma già eseguita
- **Quando** la stessa conferma viene ripetuta
- **Allora** l'esito è lo stesso della prima e **nulla viene eseguito due volte**

**CA-5 — Ruolo insufficiente**
- **Dato** un utente `member` e una bozza di budget
- **Quando** tenta di confermarla
- **Allora** riceve `403` con la spiegazione, la bozza resta in attesa e nulla viene creato

**CA-6 — Isolamento fra account**
- **Dato** una bozza dell'account `A`
- **Quando** un utente dell'account `B` ne conosce l'identificativo e tenta la conferma
- **Allora** la richiesta è rifiutata e nulla di `A` viene modificato o rivelato

**CA-7 — Sospensione degli avvisi tracciata**
- **Dato** una bozza di `sospendi_avvisi` confermata
- **Quando** si guarda il budget interessato
- **Allora** la sospensione è visibile con il motivo, chi l'ha chiesta, chi l'ha confermata e fino a quando

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul ciclo di vita della bozza (scadenza, doppia conferma, stato del mondo cambiato) e di
      **integrazione** su ciascuno dei quattro strumenti fino all'effetto reale;
- [ ] prova esplicita che **nessuno** strumento di scrittura produce effetti prima della conferma;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** su bozza e conferma;
- [ ] **prova end-to-end**: **si rimanda** alla storia `0034`, che include nel percorso `[J-SPESA-MODELLI]` il passo
      «chiedo in chat un budget, leggo la bozza, confermo, il budget esiste»;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con revisione mirata del testo degli effetti previsti;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese: `bozza_operazione` in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sull'impronta dello stato del mondo, sull'idempotenza
      della conferma e sulla conferma possibile anche dall'interfaccia;
- [ ] contratto degli **strumenti conversazionali** completo: lettura (storia `0032`) e scrittura (questa);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0020` | `crea_regola_di_attribuzione` produce una regola di quel tipo |
| Storia `0021` | `applica_regola_allo_storico` usa l'anteprima e la versionatura dell'attribuzione già costruite |
| Storia `0023` | `definisci_budget` crea un budget di quella forma |
| Storia `0025` | `sospendi_avvisi` agisce sulla sospensione già tracciata dagli avvisi |
| Storia `0032` | Riusa il contratto, l'isolamento e la registrazione degli strumenti di lettura |
| UC 0061-0064 (piattaforma) | Server conversazionale, consenso delegato e applicazione della quota alle chiamate dell'assistente non esistono ancora |

## 7. Fuori ambito

- **la conferma implicita o «ricordata»**: non esiste un modo per dire «da adesso fidati». Sarebbe la scorciatoia
  che disfa l'intero presidio, e non è rimandata: è esclusa;
- **strumenti che cancellano dati**: nessuno è esposto. La cancellazione dei dati personali passa dal contratto
  dati dell'app e dai diritti dell'interessato (storia `0035`), non da una frase in chat;
- **strumenti che agiscono verso l'esterno con le credenziali del cliente** (per esempio revocare una chiave presso
  il fornitore di modelli): esclusi per la stessa ragione della storia `0027`, §7;
- l'unione di due valori di etichetta (storia `0019`): se un giorno verrà esposta, seguirà questo protocollo, ma non
  è di questa storia.

## 8. Punti aperti

- **Quanto vive una bozza prima di scadere.** Troppo poco e la conferma di un titolare che legge la sera non arriva
  in tempo; troppo e si conferma qualcosa deciso in un contesto che non c'è più. Proposta: poche ore, con nuova
  bozza in un clic. La conferma lo sviluppatore.
- **Se la conferma di `applica_regola_allo_storico` debba essere riservata al solo `owner`.** È l'operazione che
  può far cambiare un numero già fatturato: `admin` potrebbe essere troppo largo. Proposta: `owner` e `admin`, con
  avviso al proprietario dell'account a operazione avvenuta. La conferma lo sviluppatore.
