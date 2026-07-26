# Registro delle violazioni di dati personali (breach register)

**Documento INTERNO e OBBLIGATORIO** (art. 33.5 GDPR). Come la RoPA, si mette a disposizione del Garante su
richiesta; non si pubblica.
**Regola**: si registra **OGNI** violazione di dati personali — **anche quelle non notificate**. Per le non
notificate, la **motivazione del "no-rischio"** è parte obbligatoria della voce (dimostra l'accountability).
**Come si compila**: durante un incidente, la skill [`/breach-response`](../../.claude/skills/breach-response/SKILL.md)
redige la bozza della voce seguendo il [runbook](breach-runbook.md); template della voce in
[reference/template-registro.md](../../.claude/skills/breach-response/reference/template-registro.md).

> **Nessuna violazione registrata.** La tabella è vuota: al primo incidente si aggiunge una riga (e, se serve
> più spazio, una sotto-sezione di dettaglio con lo stesso schema).

---

## Schema di una voce (art. 33.5)

Ogni voce del registro riporta:

| Campo | Contenuto |
|---|---|
| **ID incidente** | Identificativo progressivo (es. `BR-2026-001`) + riferimento al ticket interno. |
| **Data/ora di conoscenza** | Quando si è venuti a conoscenza (da qui corrono le 72h). |
| **Data/ora di rilevazione/accadimento** | Se nota/stimabile. |
| **Ruolo di appgrove** | Titolare / Responsabile (per il trattamento colpito). |
| **Natura della violazione** | Riservatezza / Integrità / Disponibilità (una o più). |
| **Descrizione dei fatti** | Cosa è successo, come, il canale di detection. |
| **Categorie di interessati** | Chi è coinvolto (utenti, membri di tenant, iscritti newsletter, …) + presenza di soggetti vulnerabili. |
| **Numero di interessati** | Esatto o stimato. |
| **Categorie di dati** | Quali dati (identificativi, credenziali, contenuti app, …); segnalare eventuali **categorie particolari art. 9**. |
| **Cifratura / inintelligibilità** | I dati erano cifrati/inintelligibili? Chiavi compromesse? (leva art. 34.3). |
| **Effetti probabili** | Conseguenze possibili per gli interessati (frode, furto d'identità, danno reputazionale, …). |
| **Misure adottate e proposte** | Contenimento, correzione, azioni per attenuare gli effetti e prevenire recidive. |
| **Esito della valutazione del rischio** | **Improbabile** / **Rischio** / **Rischio elevato** — con la **motivazione** (per "improbabile" è obbligatoria). |
| **Decisione di notifica — Garante** | Sì (data/ora invio, eventuale ritardo motivato, in fasi?) / No (motivo). |
| **Decisione di notifica — Interessati** | Sì (data/ora, modalità) / No (motivo, es. leva cifratura art. 34.3). |
| **Notifica al tenant-titolare** (se appgrove è responsabile) | Sì (data/ora) / N/A. |
| **Riferimenti** | Ticket interno, allegati/prove, corrispondenza con Garante/tenant/segnalante. |

---

## Registro

_(Vuoto — nessuna violazione registrata alla data di creazione del documento.)_

| ID | Conoscenza | Ruolo | Natura | Interessati (cat. / n.) | Dati (cat.) | Esito rischio | Notifica Garante | Notifica interessati |
|----|-----------|-------|--------|-------------------------|-------------|---------------|------------------|----------------------|
| —  | —         | —     | —      | —                       | —           | —             | —                | —                    |

> Per ogni riga aggiunta qui, mantieni una **sotto-sezione di dettaglio** con tutti i campi dello schema
> (la tabella è l'indice sintetico; il dettaglio completo — fatti, misure, motivazioni — sta nella
> sotto-sezione, per non perdere ciò che l'art. 33.5 richiede).
