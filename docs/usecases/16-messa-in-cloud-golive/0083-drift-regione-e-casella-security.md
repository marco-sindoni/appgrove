# UC 0083 — Correzione drift regione + provisioning casella security@

**Area**: 16-messa-in-cloud-golive · **Fase**: evo (messa in cloud) · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0005 (config per-ambiente in pipeline), UC 0037 (security.txt), UC 0049 (skill breach-response)
**Fonte**: R18, R10 (Tabella residui _INDEX.md); docs/_BACKLOG.md §Script/tooling DevOps
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Chiudere **due piccoli lavori operativi** distinti ma entrambi legati alla messa in cloud.
**(a) Drift della regione.** Nei profili di default di `services/core` e `services/fatture` la proprietà
`appgrove.sqs.region` vale `eu-south-1` (e c'è un commento coerente nella console dei diritti dell'interessato, il regolamento
europeo sui dati personali — RGPD — di UC 0034), mentre l'infrastruttura è **decisa su `eu-west-1`**. È un disallineamento
("drift"): in locale è irrilevante (i sostituti locali — ElasticMQ per le code, MinIO per gli oggetti — ignorano la regione),
ma al deploy nel cloud (UC 0005) va corretto o sovrascritto per-ambiente su `eu-west-1`.
**(b) Casella `security@appgrove.app`.** Il file `security.txt` è già pubblicato (UC 0037) e la skill `breach-response` esiste
già (UC 0049); resta l'**azione operativa del founder** di attivare e presidiare la casella di posta `security@appgrove.app`
per la **divulgazione responsabile** delle vulnerabilità (chi trova un problema di sicurezza deve poterlo segnalare a un
indirizzo che qualcuno legge). Nessuna infrastruttura da creare.
**Escluso**: la definizione della pipeline (UC 0005), del `security.txt` (UC 0037), della skill di risposta (UC 0049).

## 2. Attori & ruoli
- **Platform engineer**: corregge/sovrascrive la proprietà `appgrove.sqs.region` e verifica il valore effettivo nel cloud.
- **Founder**: attiva la casella `security@` e ne presidia la lettura.
- **Sistema**: SQS (Simple Queue Service, il servizio di code di AWS) usa la regione configurata; la pipeline inietta la config
  per-ambiente.
- **Terzi**: **AWS** (regione/SQS), fornitore di posta per la casella `security@`.

## 3. Precondizioni
- Meccanismo di **configurazione per-ambiente** della pipeline disponibile (UC 0005): permette di sovrascrivere valori come la
  regione senza toccare i default del codice.
- Dominio `appgrove.app` gestito e `security.txt` già pubblicato (UC 0037).
- Regione infra decisa e coerente = `eu-west-1`.

## 4. Flusso principale
1. **Drift regione — individuare.** Localizzare `appgrove.sqs.region=eu-south-1` nei profili di default di `services/core` e
   `services/fatture`, e il commento coerente nella console dei diritti dell'interessato (UC 0034).
2. **Drift regione — correggere.** Scegliere la via: allineare i **default** a `eu-west-1`, oppure lasciare i default e
   **sovrascrivere** il valore via configurazione per-ambiente al deploy (UC 0005). La sovrascrittura per-ambiente è preferibile
   se si vuole tenere il default innocuo per il locale, ma il valore effettivo nel cloud **deve** essere `eu-west-1`.
3. **Drift regione — verificare.** Nel cloud, controllare che i servizi usino davvero `eu-west-1` per SQS (le code sono nella
   stessa regione dell'infra; una regione sbagliata farebbe fallire l'accesso alle code).
4. **Casella security@ — attivare.** Provisioning della casella `security@appgrove.app` presso il fornitore di posta del
   dominio, con inoltro/lettura garantiti dal founder.
5. **Casella security@ — presidiare.** Definire chi legge e con quale cadenza; collegare mentalmente l'arrivo di una
   segnalazione al runbook della skill `breach-response` (UC 0049) quando la segnalazione riguarda una violazione.
6. **Casella security@ — coerenza con security.txt.** Verificare che l'indirizzo pubblicato in `security.txt` (UC 0037) sia
   esattamente `security@appgrove.app` e che la casella risponda.

## 5. Flussi alternativi / edge / errori
- **Default non corretti e nessuna sovrascrittura**: il servizio nel cloud punterebbe a `eu-south-1`, dove non ci sono code →
  errori di accesso a SQS. Va evitato: se si sceglie la via "sovrascrittura per-ambiente", assicurarsi che sia effettivamente
  applicata in `test` e `prod`.
- **Altri riferimenti a `eu-south-1`**: oltre a `core`/`fatture`, verificare che non restino altre occorrenze sparse (commenti,
  esempi) che possano trarre in inganno.
- **Casella security@ non presidiata**: una segnalazione di vulnerabilità che nessuno legge vanifica la divulgazione
  responsabile → definire un presidio, non solo creare la casella.
- **Posta security@ finita nello spam**: verificare la consegna con un invio di prova.

## 6. Risorse & runbook
**Risorse toccate (a)**: i profili di default di `services/core` e `services/fatture` (proprietà `appgrove.sqs.region`) e/o la
configurazione per-ambiente della pipeline (UC 0005); il commento nella console diritti dell'interessato (UC 0034).
**Risorse (b)**: nessuna infrastruttura da creare; provisioning della casella `security@appgrove.app` presso il fornitore di
posta del dominio.
**Runbook passo-passo**:
1. Cercare tutte le occorrenze di `eu-south-1` nei profili di default; decidere la via (default vs sovrascrittura per-ambiente).
2. Applicare la correzione e verificare il valore **effettivo** nel cloud (`test`, poi `prod`).
3. Attivare `security@appgrove.app`; fare un invio di prova e confermare la lettura.
4. Verificare che `security.txt` (UC 0037) riporti l'indirizzo corretto e che la casella risponda.
**Rollback (a)**: la modifica di configurazione è reversibile (ripristino del valore o della sovrascrittura precedente). **(b)**
la casella si può disattivare, ma non va fatto finché `security.txt` la pubblicizza.

## 7. Dati toccati
**(a)** Nessun dato personale: si tocca la **configurazione** della regione. **(b)** La casella `security@` riceverà messaggi
che possono contenere dati personali di chi segnala e dettagli tecnici; il trattamento è quello della **divulgazione
responsabile** e si collega al registro delle violazioni (UC 0049) quando pertinente. Non si aprono nuovi trattamenti
strutturati: è una casella di posta operativa.

## 8. Permessi & gate
- **(a)** La correzione della regione passa dalla pipeline (config per-ambiente, ruoli OIDC per ambiente); nessuna scrittura a
  mano nel cloud. Verifica coerente con l'invariante "regione infra = `eu-west-1`".
- **(b)** L'attivazione della casella è un'azione del founder presso il fornitore di posta; non passa dalla pipeline.
- Nessuna invariante multi-tenancy in gioco (non si tocca `tenant_id` né query tenant-scoped).

## 9. Requisiti di test / verifica
- **(a)** Verifica dal vivo che i servizi nel cloud usino `eu-west-1` per SQS (accesso alle code funzionante); ricerca a
  conferma che non restino occorrenze di `eu-south-1` nei default rilevanti. Se il progetto ha un controllo di configurazione
  per-ambiente, farlo passare al verde.
- **(b)** Invio di prova a `security@appgrove.app` consegnato e letto; l'indirizzo in `security.txt` coincide e risponde.

## 10. Riferimenti & Definition of Done
- **Fonte**: R18, R10 (Tabella residui _INDEX.md); docs/_BACKLOG.md §Script/tooling DevOps; UC 0037 (security.txt), UC 0049
  (breach-response), UC 0034 (console diritti dell'interessato).
- **DoD**: nel cloud `appgrove.sqs.region` risulta `eu-west-1` per `core` e `fatture` (via default o sovrascrittura
  per-ambiente), nessun residuo fuorviante di `eu-south-1`; casella `security@appgrove.app` attiva, presidiata e coerente con
  `security.txt`.

## Punti aperti / decisioni differite
- **Via scelta per la regione (default vs sovrascrittura)**: la scelta definitiva si prende al momento dell'implementazione,
  coerentemente col meccanismo di config per-ambiente di UC 0005 — di competenza di questo UC.
- **Presidio della casella nel tempo**: cadenza e responsabile della lettura di `security@` andranno riconfermati se il team
  cresce; oggi è il founder. Traccia da tenere qui.
