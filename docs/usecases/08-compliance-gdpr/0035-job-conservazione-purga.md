# UC 0035 — Job retention/purge (grace 14g, auto-delete inattività, archivio audit)

**Area**: 08-compliance-gdpr · **Fase**: 6 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0006](../02-devops-infra/0006-osservabilita-base.md) (retention/archivio), UC [0032](0032-framework-esportazione-cancellazione.md) (purge)
**Fonte decisioni**: #13 E (retention), #08 I (archivio/retention log), #06 (lifecycle)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [13-compliance-privacy](../../13-compliance-privacy.md), [08-observability](../../08-observability.md), [05-persistenza-dati](../../05-persistenza-dati.md)

## 1. Obiettivo / Scope
Implementare i **job di retention/purge programmati** che fanno rispettare la politica di retention as-code (minimizzazione art. 5.1.e).
**Incluso**: **grace cancellazione account 14gg** (disattivato subito → hard-purge dopo 14gg, annullabile); **auto-cancellazione
account inattivi 24 mesi** (email di avviso → 30gg silenzio → cancellazione); **retention per-categoria** applicata (log via
Terraform, S3 via lifecycle, DB via purge job/EventBridge); **archivio audit/sicurezza 12 mesi** (Firehose→S3→Glacier→scadenza);
retention per-app dichiarata nel manifesto.
**Escluso**: il framework export/erasure on-demand (UC 0032), la baseline observability (UC 0006, che imposta retention log/archivio), il self-service (UC 0033).

## 2. Attori & ruoli
- **Cron/EventBridge schedulato**: innesca i job.
- **Piattaforma**: orchestratore purge; invoca `purgeData` per-app (UC 0032).
- **Utente**: riceve email di avviso inattività (può evitare la cancellazione accedendo).

## 3. Precondizioni
- Framework purge (UC 0032); retention log/archivio (UC 0006); lifecycle S3 + EventBridge (UC 0003/0004); SES per avvisi.

## 4. Flusso principale
1. **Grace account 14gg** (E.1): eliminazione account → disattivato subito (inaccessibile) → **hard-purge dopo 14gg** (orchestrazione account-level UC 0032), **annullabile** entro il periodo (#13 E25).
2. **Inattività 24 mesi** (E.2): dopo 24 mesi → **email di avviso** → nessuna risposta in 30gg → **cancellazione** (#13 E26).
3. **Retention per-categoria as-code**: log (Terraform retention #08), export ZIP (lifecycle S3 7gg), DB (purge job/EventBridge), per-app (manifesto) (#13 E23/E24).
4. **Archivio audit/sicurezza 12 mesi**: subscription filter → Firehose → S3 → Glacier → scadenza (#08 28, #13 E).
5. Tutto **dichiarato e applicato**; il manifesto per-app dichiara la propria retention (#13 E24).

## 5. Flussi alternativi / edge / errori
- **Annulla eliminazione** entro 14gg → riattivazione (#13 E25).
- **Backup/PITR 7gg**: i dati cancellati spariscono entro il ciclo backup (dichiarato in policy) (#13 E23).
- **Ticket privacy 24 mesi / consenso newsletter +24 mesi**: retention specifiche applicate (#13 E23).
- **Idempotenza**: i job sono ripetibili (no doppia cancellazione).

## 6. Risorse & runbook  _(job/infra)_
**Risorse**: cron/EventBridge schedulati, purge job (core + per-app via EventBridge/SQS), lifecycle S3, Firehose/Glacier archivio.
**Runbook**: i job girano in prod (e testabili in locale per la logica, con MinIO/ElasticMQ); osservabilità via log/allarmi. Audit
delle purge registrato (#13 L70).

## 7. Dati toccati
Cancella dati a fine retention (account/app/log/export). **Hard-delete** con audit (#05 6, #13 L70). Archivio audit cifrato 12 mesi.
Manifest: applica le retention dichiarate (accountability art. 5.1.e). `@PersonalData` rispettati.

## 8. Permessi & gate
- **Invarianti**: purge per-tenant/scope con `tenant_id`; ruolo DB per-servizio; nessun cross-tenant.
- **Gate**: i job sono di sistema (non gateati da entitlement); coerenti con i diritti esenti (#09 F31).

## 9. Requisiti di test
- **Integration**: grace 14gg (disattiva→purge→annulla); inattività 24 mesi (avviso→cancellazione); retention per-categoria applicata.
- **Compliance**: dopo purge nessun dato orfano (con UC 0032); archivio audit conserva 12 mesi.
- **Security**: scope corretto; idempotenza.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #13 E23/E24/E25/E26, L70, #08 26/28, #05 6, #06 H.
- **DoD**:
  1. Grace account 14gg (annullabile) + auto-cancellazione inattivi 24 mesi.
  2. Retention per-categoria as-code applicata; archivio audit 12 mesi.
  3. Purge con audit, idempotente, no dati orfani.
  4. Integration + compliance + security verdi.

## Punti aperti / decisioni differite

- **L'eliminazione account con grace 14 giorni usa l'orchestrazione account-level della change `0028`**
  _(tracciato dalla change `0028-use-case-0032-…`)_. UC 0032 implementa la **macchina interna** della
  cancellazione account (funzione di orchestrazione: purge dei dati di piattaforma + pubblicazione
  dell'evento `tenant.offboarded` → `purgeData` di ogni app attivata via coda), **senza esporre endpoint
  utente**: esporre ora un "cancella account" senza il periodo di grazia sarebbe un comportamento destinato a
  cambiare. Qui vanno costruiti: lo stato di account disattivato/in-grace, l'annullabilità entro 14 giorni, e
  il job schedulato che allo scadere invoca l'orchestrazione di UC 0032 (nome/punto d'ingresso documentati
  nell'`implementation-log.md` della change 0028). Il trigger UI è di UC 0033.
  - **Aggiornamento (change `0029-use-case-0033-…`)**: la macchina della grazia è stata **anticipata nella
    change 0029** insieme al trigger UI, perché l'ordine di esecuzione mette UC 0033 prima di UC 0035, i
    requisiti di test di UC 0033 chiedono l'end-to-end "elimina account + grazia + annulla", e il contenuto
    decisionale era già fissato da #13 E25 (solo implementazione, nessuna decisione prematura). La change
    0029 consegna: stato account in grazia, disattivazione immediata, endpoint di annullamento entro 14
    giorni, job schedulato che allo scadere invoca `TenantOffboarding.offboard`. **Restano a questo use
    case**: auto-cancellazione account inattivi 24 mesi, retention per-categoria as-code (log Terraform, S3
    lifecycle, DB purge job/EventBridge), archivio audit/sicurezza 12 mesi, e la **verifica in cloud** della
    schedulazione (in locale gira su scheduler applicativo; il trigger EventBridge/cron di produzione è
    materia di UC 0035 con UC 0004/0006). Il punto 1 del DoD qui sopra va letto al netto della parte grace
    già consegnata.
