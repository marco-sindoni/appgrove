# 0029 — Strumenti di scrittura con conferma

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 06 — Esposizione conversazionale e prove
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0028`, `0007`, `0012`, `0017`, `0020`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come tecnico che esce dal cantiere con il telefono in mano
> voglio poter dire «segna due ore sul cantiere Verdi per ieri» e vedermi proporre la riga da confermare
> così da dichiarare le mie ore in dieci secondi, invece di rimandare a stasera e poi non farlo.

**Contesto.** È la storia che può cambiare l'adozione dell'app. Il rischio operativo numero uno di FlowGrove è
che **nessuno compili il foglio ore** perché costa fatica: senza ore, il margine di commessa è finto e tutta
l'epica 05 crolla ([application-description.md](../application-description.md) §11). La chat è l'attrito più
basso possibile. Ma è anche la superficie più pericolosa: qui si scrive nel registro delle ore di una persona, si
assegna lavoro ad altri e si fanno partire due operazioni che a valle diventano denaro. Vale perciò senza sconti
la regola di sicurezza della piattaforma ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §12):
**l'intelligenza artificiale prepara, la persona approva**.

## 2. Requisiti funzionali

1. **RF-1** — Sono dichiarati i **sei** strumenti di scrittura, ciascuno con nome stabile, descrizione, schema dei
   parametri, schema della bozza restituita e marcatura *scrittura*:
   `create_task(progetto, titolo, scadenza?, stima?)`, `assign_task(id_attività, persona)`,
   `update_status(id_attività, nuovo_stato)`, `log_time(attività, data, durata, fatturabile?, nota?)`,
   `close_period(progetto, periodo)`, `handoff_billable_lines(progetto, periodo)`.
2. **RF-2** — **Nessuno dei sei scrive direttamente.** Ognuno restituisce una **bozza** con quello che accadrebbe
   — testo leggibile più i dati strutturati — e un identificativo di conferma; la scrittura avviene solo alla
   conferma esplicita della persona.
3. **RF-3** — `log_time` scrive **solo a nome di chi sta parlando**: non ha un parametro «persona» e non c'è modo
   di dichiarare ore per un collega. È il confine che tiene l'app fuori dalla sorveglianza (§6 della descrizione)
   e non è negoziabile.
4. **RF-4** — `close_period` e `handoff_billable_lines` hanno effetti **difficilmente reversibili** e verso
   l'esterno: la loro bozza mostra il riepilogo completo (quante righe, quante ore, quale importo, quale periodo)
   e la conferma è **obbligatoria e non disattivabile**, anche se il livello conversazionale offrisse in futuro una
   modalità automatica.
5. **RF-5** — Una bozza **scade** (proposta: 15 minuti) e vale **una volta sola**: confermata due volte non
   produce due scritture. Se lo stato del mondo è cambiato fra proposta e conferma — il periodo è stato chiuso da
   qualcun altro, l'attività è stata cancellata — la conferma fallisce con una spiegazione e non scrive nulla.
6. **RF-6** — Ogni strumento di scrittura attraversa gli stessi varchi e le stesse regole di ruolo della rotta
   corrispondente: `close_period` e `handoff_billable_lines` richiedono ruolo `admin`, come dalle storie 0020 e
   0022. Un `member` che li invoca riceve un rifiuto, non una bozza.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Sei strumenti dichiarati **scrittura**, tutti con **bozza e
  conferma umana**; `close_period` e `handoff_billable_lines` marcati anche come **effetto irreversibile**. Il
  contratto vive dentro il servizio `progetti` — **dipendenza dichiarata: casi d'uso 0061-0064**.
- **RT-2 — Isolamento fra account (§1).** Bozza e conferma portano il `tenant_id` del token verificato; una bozza
  non è confermabile da un altro account, né da un altro utente dello stesso account. Il tentativo viene
  registrato e respinto.
- **RT-3 — Interfaccia di programmazione (§2).** La conferma riusa il servizio applicativo della rotta
  corrispondente (`/api/progetti/v1/tasks`, `/time-entries`, `/periods/close`, `/billable-batches/{id}/handoff`):
  nessuna seconda strada di scrittura, altrimenti le regole di dominio andrebbero mantenute in due posti.
- **RT-4 — Persistenza (§8).** Migrazione `V18__bozze_conversazionali.sql` sullo schema `app_progetti`: tabella
  `tool_draft` con `tenant_id`, chiave primaria UUID versione 7, autore, strumento, carico proposto, scadenza,
  esito, colonne di controllo e cancellazione logica. Le bozze scadute si eliminano.
- **RT-5 — Varchi e quota (§6, §7).** Gli stessi cinque varchi delle rotte. Nessuna scrittura consuma la metrica
  `seats` (a giacenza, legata alle persone abilitate, non alle azioni); con abbonamento `canceled` la risposta è
  `402` già in fase di bozza — non si propone ciò che non si potrebbe confermare.
- **RT-6 — Dati personali (§10).** `tool_draft.created_by` e il carico proposto di `log_time` contengono dati di
  un lavoratore (data, durata, nota): voci nuove nel manifesto in italiano e inglese, campi annotati
  `@PersonalData`, tabella in `exportData` e `purgeData` (storia 0030). La nota della bozza è **testo libero**:
  vale l'avviso «non inserire dati sensibili», che qui va dato nella descrizione dello strumento.
- **RT-7 — Registrazione eventi (§14).** «Bozza proposta», «bozza confermata», «bozza scaduta», «conferma
  rifiutata» con `tenant_id`, `app_id`, `user_id`, strumento e identificativo di correlazione; **mai** il testo
  della nota.
- **RT-8 — Prove (§11).** Prova che nessuno dei sei scriva senza conferma; prova di idempotenza della conferma;
  prova che `log_time` non possa scrivere per un'altra persona nemmeno forzando i parametri; prova di ruolo su
  `close_period` e `handoff_billable_lines`.

## 4. Criteri di accettazione

**CA-1 — La bozza non scrive**
- **Dato** un'attività esistente
- **Quando** si invoca `log_time(attività, data: "ieri", durata: "2h")`
- **Allora** si ottiene una bozza leggibile con attività, data, durata e fatturabilità proposta, e **nessuna riga
  di ore risulta creata** finché non arriva la conferma

**CA-2 — La conferma scrive una volta sola**
- **Dato** una bozza di `log_time` appena proposta
- **Quando** la si conferma due volte
- **Allora** esiste **una** riga di ore, e la seconda conferma risponde che la bozza è già stata usata

**CA-3 — Le ore sono sempre proprie**
- **Dato** un utente `Anna` e un collega `Bruno`
- **Quando** `Anna` chiede di registrare due ore a nome di `Bruno`
- **Allora** lo strumento non lo consente: lo schema non ha il parametro, e la risposta spiega che ciascuno
  dichiara le proprie ore

**CA-4 — La consegna alla fatturazione chiede sempre**
- **Dato** un periodo chiuso con 120 ore fatturabili e un utente con ruolo `admin`
- **Quando** si invoca `handoff_billable_lines(progetto, periodo)`
- **Allora** si ottiene una bozza con righe, ore, importo e periodo, la consegna **non** parte, e parte solo dopo
  la conferma esplicita

**CA-5 — Il mondo è cambiato fra proposta e conferma**
- **Dato** una bozza di `close_period` e un periodo chiuso nel frattempo da un'altra persona
- **Quando** si conferma la bozza
- **Allora** la conferma fallisce con `409`, spiega cosa è cambiato e non produce alcuna seconda chiusura

**CA-6 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** invoca `close_period`
- **Allora** riceve un rifiuto `403` **senza** che venga creata alcuna bozza

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (area `backend`);
- [ ] prove di **unità** sulla macchina bozza → conferma e di **integrazione** sulle sei scritture, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla conferma di una bozza altrui, e prova di identità su `log_time`;
- [ ] **prova end-to-end**: nessun impatto sulla superficie utente — il livello conversazionale non esiste ancora;
      la scrittura corrispondente è già percorsa da `[J-PROGETTI]` attraverso l'interfaccia (storia 0031); risposta
      scritta nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e in
      `decisions.json`;
- [ ] **traduzioni**: non applicabile alle descrizioni degli strumenti; se la conferma passasse da una schermata
      del backoffice, le sue stringhe andrebbero in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per `tool_draft`, con le tabelle in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato: durata della bozza, elenco degli strumenti irreversibili, motivo per
      cui `log_time` non ha il parametro «persona»;
- [ ] avvio locale invariato (`./dev.sh services`).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0028` | condivide contratto, varchi e registrazione degli strumenti: la scrittura si innesta lì |
| storie `0007`, `0012` | creazione di attività, assegnazione e cambio di stato esistono come rotte |
| storia `0017` | la riga di ore e le sue regole, che la conferma riusa senza duplicare |
| storia `0020` | la chiusura del periodo, con la sua irreversibilità |
| storia `0022` | la consegna del lotto alla fatturazione, effetto verso un'altra app |
| casi d'uso di piattaforma 0061-0064 (non implementati) | server conversazionale, consenso delegato, mappatura degli strumenti e applicazione di abilitazione e quota alle chiamate dell'assistente |

## 7. Fuori ambito

- gli strumenti di **lettura**: storia `0028`;
- la **scrittura di commenti** da chat: deliberatamente esclusa (storia 0014) — far scrivere a un assistente un
  commento a nome di una persona è precisamente ciò che va evitato;
- la **generazione di un progetto da modello** e la **creazione di un progetto** da chat: escluse in questa
  stesura (storie 0006, 0010), perché creano decine di righe in un colpo solo e vanno viste sullo schermo;
- la **riapertura** di un periodo chiuso: operazione eccezionale, non esposta come strumento (storia 0020);
- l'interfaccia di conferma: la disegna la piattaforma nel livello conversazionale, non questa app.

## 8. Punti aperti

- **La durata della bozza (proposta: 15 minuti)** non viene da una fonte: è un valore ragionevole scelto qui. Se
  la piattaforma fisserà una durata comune nel caso d'uso 0063, questa app la eredita e questa proposta decade.
- **Se `log_time` debba accettare date relative** («ieri», «lunedì scorso») o solo date esplicite. Le date
  relative abbassano moltissimo l'attrito, che è il punto della storia, ma introducono un'interpretazione del
  modello linguistico su un dato che finisce in fattura: la bozza deve comunque mostrare la data risolta in chiaro.
  Lo chiude lo sviluppatore.
