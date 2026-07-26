# UC 0082 — Script di attivazione ambienti per fasi (test-start/test-stop + cron)

**Area**: 16-messa-in-cloud-golive · **Fase**: evo (messa in cloud) · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0004 (modulo microsaas_app e wrapper script), UC 0055 (risorse condivise per-ambiente: cluster ECS/Aurora), UC 0005 (workflow CI/CD), UC 0006 (osservabilità base)
**Fonte**: R16 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Attivazione ambienti cloud", §Script/tooling DevOps
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Implementare gli script che realizzano l'**attivazione degli ambienti per fasi** secondo il principio del costo minimo
(cost-min): non tenere accesa alcuna infrastruttura prima che serva. Le fasi sono: **fase 1** solo locale (zero AWS); **fase 2**
accendere **solo `test`**, con Aurora in scale-to-0 (il database si "spegne" quando è inattivo) e autospegnimento notturno dei
task Fargate (i contenitori applicativi); **fase 3** accendere `prod` **solo prima del go-live**. La decisione di processo è
già presa in docs/07 §28: avvio **manuale** con `test-start`, spegnimento tramite **cron giornaliero** (attività pianificata a
orario fisso) `test-stop`, idempotente, a un orario UTC fisso (per esempio 20:00). Resta l'**implementazione**.
**Incluso**: gli script `infra/scripts/test-start` e `infra/scripts/test-stop` (portano i task Fargate da 0 a 1 e viceversa,
risvegliano/mettono in pausa Aurora) e il workflow GitHub Actions `env-ops` che li orchestra (cron per lo stop, dispatch
manuale per lo start). **Escluso**: la definizione delle risorse (UC 0055) e la pipeline in sé (UC 0005). `prod` **non si
spegne**.

## 2. Attori & ruoli
- **Founder / platform engineer**: lancia `test-start` quando serve l'ambiente; verifica lo spegnimento.
- **Sistema (cron GitHub Actions `env-ops`)**: esegue `test-stop` ogni giorno a orario UTC fisso.
- **Terzi**: **AWS** (ECS/Fargate per i contenitori, Aurora per il database), **GitHub** (schedulazione cron del workflow).

## 3. Precondizioni
- Risorse condivise per-ambiente definite (UC 0055): cluster ECS e Aurora esistono in `test`.
- Wrapper script comuni (UC 0004) e workflow (UC 0005) presenti; ruoli **OIDC** (autenticazione senza chiavi) per ambiente
  disponibili.
- Regione `eu-west-1`.

## 4. Flusso principale
1. **`test-start`**: porta il numero desiderato di task Fargate da **0 a 1** per ciascun servizio dell'ambiente `test` e
   **risveglia** Aurora (se in pausa per lo scale-to-0). Attende che l'health check sia verde prima di dichiarare
   l'ambiente pronto.
2. **`test-stop`**: porta i task Fargate a **0** e **mette in pausa** Aurora. È **idempotente**: eseguirlo su un ambiente già
   spento non deve fallire né avere effetti collaterali.
3. **Workflow `env-ops`**: espone `test-start`/`test-stop` come dispatch manuali e programma `test-stop` con un **cron
   giornaliero** a orario UTC fisso (es. 20:00), così che un ambiente lasciato acceso per distrazione si spenga da solo la sera.
4. **`prod`** resta **sempre acceso** (nessuno stop schedulato): la fase 3 lo accende una volta prima del go-live e non lo
   spegne.

## 5. Flussi alternativi / edge / errori
- **`test-stop` su ambiente già spento**: non deve fallire (idempotenza) → controllare lo stato attuale prima di agire.
- **`test-start` durante un cold-start lungo di Aurora**: il primo health check può tardare → attendere/riprovare con un limite
  di tempo prima di dichiarare fallimento (aggancio col limite ~5s di Cognito visto in UC 0081).
- **Cron che spegne `test` durante una verifica serale**: rischio di spegnimento inatteso mentre si lavora → l'orario UTC fisso
  è noto e documentato; in caso, rilanciare `test-start`.
- **Applicazione errata a `prod`**: uno script che spegnesse `prod` sarebbe un incidente → gli script devono agire **solo** su
  `test` (guardia esplicita sull'ambiente).
- **Task che non scendono a 0**: eventuale task bloccato → il workflow deve segnalarlo (aggancio osservabilità UC 0006), non
  lasciarlo acceso in silenzio (spesa non voluta).

## 6. Risorse & runbook
**Risorse da creare**: `infra/scripts/test-start`, `infra/scripts/test-stop` (wrapper coerenti con gli altri `infra/scripts/`);
workflow `.github/workflows/env-ops.yml` con dispatch manuale (start/stop) e trigger cron per lo stop. Nessuna nuova risorsa
AWS: gli script **manovrano** ECS e Aurora esistenti (UC 0055).
**Runbook passo-passo**:
1. Implementare `test-start` (Fargate 0→1 + risveglio Aurora + attesa health) e `test-stop` (Fargate →0 + pausa Aurora),
   idempotenti e con guardia "solo test".
2. Cablare `env-ops`: dispatch manuale per lo start; cron giornaliero UTC fisso per lo stop.
3. Verificare accensione e spegnimento reali, e che il cron spenga a fine giornata.
**Rollback**: rimuovere il cron ferma lo spegnimento automatico (l'ambiente resterebbe acceso, spesa maggiore ma nessun danno
funzionale); gli script sono comandi reversibili (si riaccende con `test-start`).

## 7. Dati toccati
Nessun dato personale. Si manovra lo **stato di esecuzione** dell'infrastruttura (numero di task, pausa/risveglio del
database). Il database in pausa **conserva** i dati; lo scale-to-0 non li cancella.

## 8. Permessi & gate
- Gli script agiscono via ruoli OIDC dell'ambiente `test`; il workflow li usa senza chiavi statiche.
- **Guardia di ambiente non negoziabile**: gli script operano **solo** su `test`. `prod` è escluso per costruzione.
- Il logging resta strutturato: ogni esecuzione di start/stop lascia traccia.

## 9. Requisiti di test / verifica
Verifiche dal vivo (nessun test unitario per la manovra reale di ECS/Aurora):
- `test-start` porta l'ambiente a task=1 e Aurora attiva, con health verde;
- `test-stop` porta task=0 e Aurora in pausa;
- `test-stop` eseguito due volte di fila non fallisce (idempotenza);
- il cron di `env-ops` spegne `test` all'orario previsto;
- nessuno script tocca mai `prod` (verifica della guardia).

## 10. Riferimenti & Definition of Done
- **Fonte**: R16 (Tabella residui _INDEX.md); docs/_BACKLOG.md §"Attivazione ambienti cloud", §Script/tooling DevOps; decisione
  di processo in docs/07 §28.
- **DoD**: `test-start`/`test-stop` implementati (idempotenti, solo test), workflow `env-ops` con dispatch + cron giornaliero
  attivo; accensione/spegnimento verificati dal vivo; `prod` non soggetto a spegnimento.

## Punti aperti / decisioni differite
- **Orario esatto del cron**: 20:00 UTC è l'esempio; il valore definitivo va confermato al momento dell'implementazione
  (tenendo conto del fuso italiano e delle abitudini di verifica serale) — decisione differita, di competenza di questo UC.
- **Autospegnimento più fine (scale-to-0 anche di giorno)**: se `test` restasse inattivo per lunghe fasce diurne, valutare una
  logica più aggressiva; oggi basta lo spegnimento serale. Traccia da tenere qui.
