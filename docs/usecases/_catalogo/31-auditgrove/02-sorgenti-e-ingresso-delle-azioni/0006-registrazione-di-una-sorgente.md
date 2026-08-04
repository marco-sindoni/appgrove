# 0006 — Registrazione di una sorgente

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 02 — Sorgenti e ingresso delle azioni
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde degli agenti della propria azienda
> voglio dichiarare quali agenti hanno il permesso di scrivere nel mio registro e dare a ciascuno una propria
> chiave d'ingresso
> così da sapere sempre da chi arriva una riga, e da poter togliere quel permesso in un momento se qualcosa va
> storto.

**Contesto.** Fino a qui il registro esiste ma nessuno può scriverci. Una **sorgente** è un'origine dichiarata di
azioni: un agente del cliente, un adattatore messo davanti al suo server di strumenti, oppure la sorgente nativa
appgrove (storia 0012). È la prima storia dell'epica perché tutto il resto dell'ingresso presuppone di sapere
**chi sta parlando**: senza sorgenti registrate, la rotta di ingresso della storia 0008 non avrebbe modo di
distinguere una dichiarazione legittima da una qualsiasi. La chiave d'ingresso si vede una volta sola: da noi ne
resta solo l'impronta, come per qualunque credenziale seria.

## 2. Requisiti funzionali

1. **RF-1** — Un utente con ruolo sufficiente registra una sorgente indicando **nome** e **genere**: agente del
   cliente, adattatore davanti a un server di strumenti, oppure sorgente nativa appgrove.
2. **RF-2** — Alla creazione la sorgente riceve una **chiave d'ingresso**, mostrata **una sola volta** al momento
   della creazione; da quel momento il servizio ne conserva la sola impronta e non è più in grado di rimostrarla.
3. **RF-3** — Una sorgente si può **revocare**: da quel momento la sua chiave non è più accettata, ma le azioni
   che ha già scritto restano intatte nel registro — revocare una sorgente non cancella la sua storia.
4. **RF-4** — Una sorgente si può **rigenerare**: si emette una chiave nuova e la precedente smette di funzionare,
   senza creare una sorgente diversa e senza spezzare la continuità delle sue azioni.
5. **RF-5** — La scheda di una sorgente mostra il proprio stato: attiva o revocata, momento dell'ultimo contatto,
   ultima sequenza ricevuta, numero di azioni dichiarate.
6. **RF-6** — La creazione, la revoca e la rigenerazione di una sorgente sono **esse stesse righe del registro**:
   sono decisioni che cambiano chi può scrivere, e devono essere dimostrabili quanto le azioni.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle sorgenti filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Una
  chiave d'ingresso appartiene a un solo account e non può in nessun caso scrivere nella catena di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/agentaudit/v1/sources`,
  `GET /api/agentaudit/v1/sources`, `GET /api/agentaudit/v1/sources/{id}`,
  `POST /api/agentaudit/v1/sources/{id}/revoke` e `POST /api/agentaudit/v1/sources/{id}/rotate`; corpo validato;
  errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V4__sorgenti.sql` sullo schema `app_agentaudit`: tabella `sources` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. **L'impronta della
  chiave non è la chiave**: si conserva una derivazione non reversibile, mai il valore in chiaro.
- **RT-4 — Modulo frontend (§3, §5).** Sezione **Sorgenti** del modulo `agentaudit`: elenco, creazione, scheda,
  revoca e rigenerazione. La chiave appena emessa si mostra una volta, con un avviso esplicito che non sarà più
  recuperabile. Dati letti con il client generato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe della sezione — compreso l'avviso «questa chiave non sarà più
  mostrata» e il messaggio di revoca — passano dallo spazio-nomi `agentaudit` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Le sorgenti **non consumano quota**: la metrica `actions` conta le azioni,
  non le sorgenti, e far pagare le sorgenti scoraggerebbe esattamente il comportamento da incoraggiare (§3 della
  [descrizione dell'applicazione](../application-description.md)). La creazione e la revoca richiedono ruolo
  `owner` o `admin`: un `member` le vede e non le tocca.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. La creazione di una sorgente **non** sarà
  esposta all'assistente nemmeno in seguito: emettere una credenziale che dà diritto di scrivere nel registro è
  un'azione che deve restare in mano a una persona.
- **RT-8 — Dati personali (§10).** Voce nuova nel manifesto `docs/compliance/manifests/agentaudit.yaml`, in
  italiano e inglese: il **contatto di avviso** della sorgente (indirizzo di posta elettronica di una persona
  presso il cliente), campo annotato `@PersonalData`, tabella `sources` aggiunta a `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «sorgente creata», «sorgente revocata», «chiave rigenerata»
  sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza mai la chiave**
  né la sua impronta.

## 4. Criteri di accettazione

**CA-1 — Si registra una sorgente e si ottiene la chiave una volta sola**
- **Dato** un utente con ruolo `admin` di un account abilitato
- **Quando** registra una sorgente di genere «agente del cliente» chiamata «agente di fatturazione»
- **Allora** la sorgente compare nell'elenco in stato attivo, la chiave viene mostrata una volta con l'avviso che
  non sarà più recuperabile, e una richiesta successiva della scheda **non** la ripropone

**CA-2 — La revoca chiude la porta e non riscrive la storia**
- **Dato** una sorgente attiva che ha già dichiarato dieci azioni
- **Quando** viene revocata
- **Allora** la sua chiave non è più accettata dall'ingresso, le dieci azioni restano nel registro invariate, e
  nella catena compare la riga «sorgente revocata»

**CA-3 — La rigenerazione non spezza la continuità**
- **Dato** una sorgente attiva con chiave in uso
- **Quando** si rigenera la chiave
- **Allora** la vecchia chiave viene rifiutata, la nuova è accettata, la sorgente resta la stessa e la sua
  numerazione di sequenza prosegue senza salti artificiali

**CA-4 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** tenta di creare o revocare una sorgente
- **Allora** riceve `403` e nulla viene creato né modificato

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie sorgenti
- **Quando** un utente di `A` chiede l'elenco delle sorgenti
- **Allora** vede solo le proprie, anche se forza l'identificativo dell'altro account nella richiesta; e la chiave
  di una sorgente di `B` non scrive nulla nella catena di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla derivazione e sul confronto dell'impronta della chiave, e di **integrazione** sulle
      rotte delle sorgenti, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle sorgenti e sulle chiavi, compreso il tentativo di forzare
      l'identificativo dell'account;
- [ ] **prova end-to-end**: risposta «rimando» — il collegamento di una sorgente entra nel percorso
      `[J-AGENTAUDIT]` alla storia 0037, proprietaria della copertura; fino ad allora il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta l'esenzione motivata;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con il contatto di avviso, campo annotato, tabella
      `sources` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta di mostrare la chiave una volta sola e con il punto
      aperto sulla credenziale non umana (sezione 8);
- [ ] contratto degli **strumenti conversazionali**: nessuno, e il divieto futuro di esporre la creazione di
      sorgenti è dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Creazione, revoca e rigenerazione sono righe della catena: serve il registro |
| storia `0003` | Serve il guscio del modulo dove vive la sezione Sorgenti |
| Bordo di piattaforma (UC 0014, UC 0016) | La verifica di una credenziale che **non** è un token di persona è materia del bordo: vedi punti aperti |

## 7. Fuori ambito

- l'uso della chiave per scrivere davvero: storia 0008;
- la sorgente nativa appgrove, che non ha chiavi da distribuire: storia 0012;
- la rilevazione dei buchi nella numerazione di una sorgente: storia 0011;
- qualunque forma di scoperta automatica delle sorgenti: una sorgente si dichiara, non si indovina.

## 8. Punti aperti

- ⚠️ **Come una macchina si autentica — punto aperto forte, che questa storia assume e non decide.** L'invariante
  di piattaforma dice che il `tenant_id` arriva **solo** da un token di persona verificato. Ma una sorgente è una
  macchina: non c'è una persona che accede. La proposta è che la chiave d'ingresso sia una credenziale verificata
  **lato server**, dal cui record si deriva il `tenant_id` — che quindi continua a **non** arrivare mai dal corpo
  della richiesta, e l'invariante resta rispettato nella sostanza. Ma introdurre una credenziale non umana tocca
  il bordo della piattaforma (UC 0014, UC 0016) e supera il perimetro di una singola storia d'app. È il punto 7
  dei rischi della [descrizione dell'applicazione](../application-description.md). Chi chiude: sviluppatore
  insieme alla piattaforma, **prima** dell'implementazione della storia 0008.
- **Durata e rotazione automatica della chiave.** Propongo chiavi senza scadenza con rotazione manuale, perché una
  scadenza automatica su un agente che nessuno sorveglia produce interruzioni silenziose del registro — cioè
  esattamente ciò che il prodotto deve evitare. Da confermare con chi presidia la sicurezza.
- **Quante sorgenti per piano.** Il §3 della descrizione dichiara le sorgenti illimitate in tutti i piani. È una
  proposta di listino, e come tale una fermata di escalation dello sviluppatore.
