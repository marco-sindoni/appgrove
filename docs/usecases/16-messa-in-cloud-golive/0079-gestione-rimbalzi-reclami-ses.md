# UC 0079 — Gestione rimbalzi/reclami SES

**Area**: 16-messa-in-cloud-golive · **Fase**: evo (messa in cloud) · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0018 (email transazionali via SES), UC 0006 (osservabilità base)
**Fonte**: R13 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Recapito email SES"
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Predisporre la gestione dei **rimbalzi** (bounce: email respinte perché l'indirizzo non esiste o rifiuta) e dei **reclami**
(complaint: destinatari che segnalano la posta come indesiderata) del servizio **SES** (Simple Email Service di AWS). Oggi non
esiste nulla. Il rischio è netto e non graduale: SES **sospende l'intero account** se il tasso di rimbalzo o di reclamo supera
le sue soglie. Non è un degrado progressivo — da un momento all'altro **smettono di partire tutte le email**, comprese quelle
critiche di verifica indirizzo e reset password: chi si registra resta bloccato fuori.
**Incluso**, in ordine di valore: (1) **destinazione delle notifiche** di rimbalzo/reclamo; (2) **lista di soppressione**
(smettere di riscrivere a indirizzi che rimbalzano stabilmente); (3) **allarme sul tasso** prima della soglia di AWS.
**Escluso**: la richiesta di uscita dalla modalità di prova (UC 0078, di cui questo UC è il prerequisito di sostanza) e il
contenuto delle email (UC 0018).

## 2. Attori & ruoli
- **Platform engineer**: implementa notifiche, lista di soppressione e allarme.
- **Founder**: presidia l'allarme sul tasso e decide in caso di avvicinamento alla soglia.
- **Sistema**: SES emette gli eventi di rimbalzo/reclamo; il consumo li registra e aggiorna la soppressione; l'osservabilità
  emette l'allarme.
- **Terzi**: **AWS/SES** (origine degli eventi), **SNS** (Simple Notification Service, il servizio di notifiche di AWS usato
  come tramite tra SES e il nostro consumo).

## 3. Precondizioni
- Ambiente `test` acceso con dominio verificato in SES (UC 0018).
- Osservabilità di base attiva (UC 0006), per poter agganciare l'allarme sul tasso.
- Da completare **prima di volumi reali** di posta (quindi prima del lancio, UC 0043).

## 4. Flusso principale
1. **Destinazione delle notifiche** (priorità più alta). Configurare SES per pubblicare gli eventi di **rimbalzo** e
   **reclamo** su un **argomento di notifica SNS** dedicato; agganciare a quell'argomento un consumo (funzione Lambda o coda)
   che li registra in modo strutturato (con `tenant_id`, `app_id`, `user_id` quando ricostruibili, secondo l'invariante di
   logging).
2. **Lista di soppressione.** Alimentare una lista degli indirizzi che rimbalzano stabilmente (rimbalzi "duri", cioè
   permanenti) o che hanno reclamato, così da **non riscrivere** più a quegli indirizzi. Si può usare la lista di soppressione
   gestita da SES a livello di account e/o una lista propria consultata prima dell'invio.
3. **Allarme sul tasso.** Definire un allarme che scatti **prima** della soglia di sospensione di AWS, sul tasso di rimbalzo e
   sul tasso di reclamo (aggancio con l'osservabilità di UC 0006), così che il founder possa intervenire prima che SES agisca.
4. **Verifica** con eventi simulati (SES espone indirizzi di test che generano rimbalzo/reclamo deterministici) e controllo
   che notifica, soppressione e allarme reagiscano come atteso.

## 5. Flussi alternativi / edge / errori
- **Rimbalzo "morbido"** (temporaneo, es. casella piena): non va messo in soppressione permanente → distinguere rimbalzi
  permanenti da temporanei nel consumo.
- **Reclamo su email transazionale legittima**: capita (l'utente segnala come indesiderata una posta che ha richiesto) →
  comunque sopprimere quell'indirizzo per non peggiorare il tasso, e registrarlo.
- **Argomento SNS irraggiungibile / consumo in errore**: gli eventi non vanno persi → prevedere ripetizione e coda di scarto
  (dead-letter) sul consumo.
- **Sospensione già avvenuta**: se SES ha già sospeso l'account, l'allarme ha fallito il suo scopo → procedura di ripristino
  con AWS e revisione delle soglie di allarme (rendere l'allarme più prudente).

## 6. Risorse & runbook
**Risorse (via infrastruttura come codice, regione `eu-west-1`)**: configurazione SES per pubblicare rimbalzi/reclami su un
argomento SNS; l'argomento SNS; il consumo (funzione/coda) che registra e aggiorna la soppressione; la definizione
dell'allarme sul tasso presso l'osservabilità (UC 0006). Andranno collocate coerentemente con il modulo/risorse condivise
per-ambiente.
**Runbook passo-passo**:
1. Creare l'argomento SNS e collegare SES → SNS per gli eventi di rimbalzo e reclamo.
2. Distribuire il consumo che registra gli eventi e alimenta la lista di soppressione.
3. Definire l'allarme sul tasso (soglia sotto quella di AWS) in osservabilità.
4. Verificare con gli indirizzi di test di SES (rimbalzo e reclamo simulati).
5. Solo dopo, procedere con UC 0078 (uscita dalla modalità di prova), citando questi presidi.
**Rollback**: rimuovere argomento/consumo/allarme è possibile via infrastruttura come codice; ma non va fatto in presenza di
volumi reali, perché riespone al rischio di sospensione.

## 7. Dati toccati
Gli eventi di rimbalzo/reclamo contengono l'**indirizzo email** del destinatario (dato personale) e vengono conservati per la
finalità di **igiene della posta** (non riscrivere a indirizzi problematici) e prevenzione della sospensione. Vanno inquadrati
nel manifesto dati/RoPA (Registro dei trattamenti) coerentemente con UC 0018: categoria "dati di contatto", base giuridica
legittimo interesse alla continuità del servizio email, retention limitata a quanto serve alla soppressione. La lista di
soppressione è essa stessa un insieme di indirizzi email.

## 8. Permessi & gate
- Le risorse (SNS, consumo, allarme) sono create dalla pipeline con i ruoli **OIDC** (OpenID Connect, l'autenticazione senza
  chiavi statiche di GitHub Actions verso AWS) per ambiente; nessun accesso a mano oltre alla verifica.
- L'invariante di **logging strutturato** vale anche qui: ogni evento registrato porta gli identificatori disponibili.
- **Gate di ambiente**: si attiva su `test`, poi si porta in `prod` prima del go-live.

## 9. Requisiti di test / verifica
Verifiche dal vivo (nessun test unitario possibile per il comportamento reale di SES):
- un rimbalzo simulato genera una notifica sull'argomento SNS e l'indirizzo finisce in soppressione;
- un reclamo simulato viene registrato e sopprime l'indirizzo;
- il consumo non riscrive a un indirizzo in soppressione;
- l'allarme sul tasso scatta quando il tasso simulato supera la soglia prudenziale (sotto quella di AWS).

## 10. Riferimenti & Definition of Done
- **Fonte**: R13 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Recapito email SES".
- **DoD**: rimbalzi e reclami confluiscono su SNS e vengono registrati; la lista di soppressione impedisce nuovi invii agli
  indirizzi problematici; l'allarme sul tasso è attivo e verificato; il tutto pronto **prima** di UC 0078 e dei volumi reali.

## Punti aperti / decisioni differite
- **Soglie numeriche dell'allarme**: i valori esatti (percentuali di rimbalzo/reclamo) vanno tarati sui limiti pubblicati da
  AWS al momento dell'attivazione — decisione differita al momento dell'implementazione, di competenza di questo UC.
- **Consumo condiviso vs per-app**: se in futuro più app generassero posta con profili di rimbalzo diversi, valutare un
  consumo per-app; oggi il consumo è unico. Traccia da tenere qui.
