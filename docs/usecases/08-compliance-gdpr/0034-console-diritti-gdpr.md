# UC 0034 — Console "Diritti GDPR" (admin single pane)

**Area**: 08-compliance-gdpr · **Fase**: 6 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0032](0032-framework-esportazione-cancellazione.md) (export/erasure), UC [0021](../06-frontend/0021-console-admin-spa.md) (admin SPA)
**Fonte decisioni**: #13 L75 (console diritti), #13 D21 (ticketing in-house), #13 J (breach/notifiche)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [13-compliance-privacy](../../13-compliance-privacy.md), [06-frontend/0021-console-admin-spa](../06-frontend/0021-console-admin-spa.md), [08-observability](../../08-observability.md)

## 1. Obiettivo / Scope
Implementare la **console "Diritti GDPR"** nell'admin: **single pane of glass** (read/ops) che aggrega tutti gli oggetti di
esercizio diritti, **scoped alla retention**.
**Incluso**: vista di **aggregazione** (platform-admin) di: richieste **export** (stato/progress + link file S3 + scadenza),
**recessi per-app**, **eliminazioni account** (stato grace 14gg), cambi **consenso**, **ticket privacy**; ciascuno con stato/timeline
e **puntatori all'accessorio** (deep-link CloudWatch Logs Insights pre-filtrato per `correlation_id`/`user_id`, job export/oggetto
S3, registro audit/breach); **ticketing in-house** (entità `support_ticket`, thread, notifiche SES) come fallback/supporto.
**Escluso**: il framework export/erasure (UC 0032), il self-service utente (UC 0033), i job retention (UC 0035); la skill breach (UC 0049).

## 2. Attori & ruoli
- **platform-admin**: consulta/gestisce (read/ops); **nessuna impersonation** (#03 15).
- **Tenant-titolare B2B**: per i dati app è lui il titolare; appgrove assiste con tooling (#13 D20).
- **Sistema**: auto-crea ticket privacy da eventi (export FAILED, escalation art. 9) (#13 D21).

## 3. Precondizioni
- Admin SPA (UC 0021); framework export/erasure (UC 0032); ticketing in-house (entità + vista admin); audit/log (UC 0006).

## 4. Flusso principale
1. **Aggregazione**: convoglia export/recessi/eliminazioni/consensi/ticket privacy, ciascuno con stato/timeline (#13 L75).
2. **Puntatori all'accessorio**: deep-link a **Logs Insights** (pre-filtrato `correlation_id`/`user_id`), job export/oggetto S3, registro audit/breach (#13 L75).
3. **Ticketing in-house**: tipo/priorità/stato, `tenant_id`/`user_id`, oggetto, **thread** utente↔admin, **notifiche SES**; ticket privacy = tipo speciale (SLA legale 1 mese, auto-creati da eventi) (#13 D21).
4. **Scoped alla retention** (#13 E): i record spariscono a fine finestra (minimizzazione); la **prova di evasione** resta nel registro audit per il periodo dovuto (#13 L75).
5. **Tooling B2B**: per i dati app, l'admin assiste il tenant-titolare (export/cancellazione dei propri utenti) (#13 D20).

## 5. Flussi alternativi / edge / errori
- **Export FAILED / escalation art. 9** → **auto-creazione ticket privacy** (#13 D21).
- **Breach** → la console linka registro breach/notifiche (UC 0049) (#13 J).
- **Nessuna impersonation**: l'admin agisce solo dai dati della console (#03 15).
- **Non è un nuovo store**: è **aggregazione** su export job/audit/ticketing esistenti (#13 L75).

## 6. Schermate & stati
Console "Diritti GDPR": tabella aggregata (tipo, soggetto, stato, timeline, scadenza, link accessorio), dettaglio richiesta,
vista ticket (thread). Stati loading/empty/error; deep-link esterni (Logs Insights/S3). Read/ops, no danger-zone qui.

## 7. Dati toccati
Aggrega metadati di richieste/ticket (utente/tenant, stato, link). **Dati personali**: identificativi minimi (user_id/tenant_id),
ticket con PII minimizzati (no allegati MVP). Retention: ticket privacy 24 mesi (#13 E). Manifest: trattamento "gestione diritti/supporto".

## 8. Permessi & gate
- **Invarianti**: accesso **solo platform-admin** (route `requireRole('platform-admin')`, admin SPA separata); query tenant-scoped dove pertinente.
- **Gate**: nessuna impersonation; ops sicure (no editing arbitrario dei dati utente). Diritti esercitati via framework (UC 0032), esenti dai gate runtime (#09 F31).

## 9. Requisiti di test
- **E2E admin**: aggregazione corretta; deep-link funzionanti; ticket privacy auto-creati da eventi (export FAILED).
- **Security**: solo platform-admin; nessuna impersonation; scoping retention (i record scadono).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #13 L75, D20/D21, E, J, #03 15.
- **DoD**:
  1. Single pane che aggrega export/recessi/eliminazioni/consensi/ticket con stato+puntatori.
  2. Ticketing in-house (thread + SES) con ticket privacy speciali auto-creati.
  3. Scoped alla retention; prova di evasione nell'audit; nessuna impersonation.
  4. E2E admin + security verdi.

## Punti aperti / decisioni differite

_Tracciato dalla change `0029-use-case-0033-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Gestione operativa della limitazione del trattamento (art. 18, da UC 0033)**. La change 0029 espone la
  limitazione nella pagina dei diritti come **diritto dichiarato con canale di richiesta** (contatto
  privacy), senza flag self-service: è un diritto eccezionale valutato caso per caso e senza questa console
  nessuno potrebbe gestirlo. Quando questo use case verrà implementato deve coprire la presa in carico
  della richiesta (ticket privacy), l'applicazione del **flag/sospensione di limitazione** (la meccanica di
  sospensione utente/account esiste già) e la prova di evasione nell'audit (#13 D19).
- **Ticket privacy automatico su export FAILED (da UC 0032/0033)**: già rimandato qui dalla change 0028 —
  resta valido; la UI export della change 0029 mostra lo stato FAILED e invita a contattare il supporto
  finché il ticketing non esiste.
