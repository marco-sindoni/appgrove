# Compliance & Privacy (GDPR) — Decisioni

**Stato**: 🟡 in corso (A deciso; B–L da definire)
**Ultimo aggiornamento**: 2026-06-20

> ⚠️ **Disclaimer**: questo documento NON è parere legale. Fissa le **scelte tecniche/architetturali** che rendono la
> compliance possibile e una **postura di default ragionevole**, da **validare con un professionista** prima del go-live.
> I punti che richiedono un legale sono segnalati.

## Scope
Trattamento dei dati personali conforme a GDPR (e norme collegate), lato **tecnico** (cosa deve poter fare il sistema)
e **documentale** (policy, registri). Si appoggia a: soft-delete/erasure (#05), purge per-tenant EventBridge (#06 H),
retention log/audit + no-PII nei log (#08), Paddle MoR (#09), accessibilità (#10 K).

## Agenda
- **A. Ruoli & perimetro** (titolare/responsabile) — 🟢 deciso
- **B. Basi giuridiche & finalità** — 🔴
- **C. Data mapping / Registro trattamenti (Art. 30)** — 🔴
- **D. Diritti degli interessati** (accesso, rettifica, oblio, portabilità, opposizione) — 🔴
- **E. Data retention** (politica complessiva) — 🔴
- **F. Consenso, cookie & tracking** — 🔴
- **G. Privacy Policy & T&C** (minimizzazione informativa; ripartizione con Paddle) — 🔴
- **H. Sub-responsabili & DPA** — 🔴
- **I. Data residency & trasferimenti** — 🔴
- **J. Data breach** (notifica 72h) — 🔴
- **K. Privacy by design/default & DPIA** — 🔴
- **L. Funzionalità GDPR nelle app** (export/erasure per-app) — 🔴

## Decisioni prese

### A. Ruoli & perimetro — postura uniforme "no classificazione utenti"
1. **Non si classificano gli utenti** (personale vs professionale): è ingestibile e il ruolo GDPR non dipende dalla forma
   dell'app né dal tipo di utente. Il "modello B2B" (multi-utente, pricing a tier) è una **feature di prodotto**, NON un
   ruolo giuridico (es. un capofamiglia che invita 3 familiari su un'app "B2B" → resta uso domestico).
2. **Si distingue per TIPO DI DATO, non per tipo di utente:**
   - **Dati di piattaforma** (account/email/auth/2FA, membership/ruoli, billing, log/audit) → **appgrove = TITOLARE**
     sempre, per tutti. Una sola privacy policy.
   - **Contenuti dentro le app** (ciò che l'utente immette) → postura **uniforme**: *"i dati sono dell'utente; appgrove
     li tratta per suo conto, solo per erogare il servizio, nessun uso secondario, nessuna monetizzazione"*.
3. **DPA incorporato nei termini standard** (allegato auto-applicabile): il cliente **professionale** (titolare) che ha
   bisogno del DPA **ce l'ha già**; per il **consumatore** è inerte. **Nessuna selezione/classificazione** da parte di appgrove.
4. **Costruire allo standard più rigoroso** (export/erasure/rights per OGNI utente, no uso secondario): così l'etichetta
   "titolare vs responsabile" sui contenuti diventa **documentale**, e il comportamento del sistema soddisfa entrambe le
   interpretazioni. È il principio che rende la postura serena senza decidere caso per caso.
5. **Household exemption (art. 2(2)(c))**: esenta la **persona fisica** per uso personale/domestico → quell'utente **non è
   titolare**; ma l'esenzione **NON si estende ad appgrove** (giurisprudenza *Ryneš*) → per i contenuti dei **consumatori**
   appgrove è **titolare** (non responsabile). **Impatto sull'impianto tecnico = nullo**: privacy policy appgrove-facing e
   gestione diritti diretta erano già previste; cambia solo la **dicitura documentale**. Conseguenza vincolante: per i
   consumatori **non** si possono scaricare gli obblighi su un "cliente-titolare" (inesistente) → li gestisce appgrove.
6. **Utenti invitati** (es. familiari/colleghi) = interessati a pieno titolo → informati dalla **stessa** policy, con i
   **loro** diritti (gestiti dalla tooling rights, D/L).
7. **Paddle MoR**: per **pagamento/fiscale** Paddle è titolare/contitolare (gestisce lui tax/fatturazione); appgrove resta
   titolare della **relazione di servizio** (account/uso). Dettaglio in G/H/#09.

## Questioni aperte
B–L da definire. Punti per il **legale**: dicitura esatta titolare/responsabile sui contenuti, testo DPA, validità del
modello "cancellazione immediata post-export", periodi di retention.

## Impatti su altre aree
- [05-persistenza-dati](05-persistenza-dati.md) (soft-delete/erasure), [06-infra-iac](06-infra-iac.md) (purge EventBridge),
  [08-observability](08-observability.md) (retention/no-PII/audit), [09-pagamenti](09-pagamenti.md) (Paddle MoR),
  [10-testing](10-testing.md) (a11y), [03-frontend](03-frontend.md) (consenso/policy UI), [_BACKLOG.md](_BACKLOG.md)
