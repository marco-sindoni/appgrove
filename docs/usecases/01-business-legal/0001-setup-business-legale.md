# UC 0001 — Setup business/legale (commercialista, P.IVA forfettaria, domiciliazione/entità, account Paddle + Domain Review)

**Area**: 01-business-legal · **Fase**: 3 · **Stato**: 🟢 deciso (azioni operative extra-repo)
**Dipendenze**: UC [0002](0002-documenti-legali-multilingua.md) (testi legali), UC [0036](../09-marketing-site/0036-vetrina-astro-scheletro.md) (sito live)
**Fonte decisioni**: #14 A/D, [_COMMERCIALISTA](../../_COMMERCIALISTA.md), #09 (Paddle MoR), [_REVISIONE-LEGALE](../../_REVISIONE-LEGALE.md) L11
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [09-pagamenti](../../09-pagamenti.md), [_COMMERCIALISTA](../../_COMMERCIALISTA.md), [_REVISIONE-LEGALE](../../_REVISIONE-LEGALE.md)

## 1. Obiettivo / Scope
Eseguire il **setup business/legale** che sblocca Paddle e la monetizzazione: inquadramento fiscale, entità/sede, attivazione
account Paddle (Domain Review).
**Incluso (azioni operative, non codice)**: consulenza commercialista (checklist F1–F9); apertura **P.IVA forfettaria come
ditta individuale** **al momento del primo abbonamento a pagamento** (Via B); **domiciliazione/virtual office** (sede, no
indirizzo di casa); registrazione **account Paddle "Individual"** + **Domain Review** (sito live con 3 legali); aggiornamento
entità nei documenti (persona fisica → ditta individuale).
**Escluso**: i testi legali (UC 0002), il sito (UC 0036), l'integrazione Paddle software (UC 0022+).

## 2. Attori & ruoli
- **Founder (persona fisica → ditta individuale)**: titolare del trattamento e dell'attività.
- **Commercialista**: consulenza una-tantum (setup fiscale concreto, raccomandato).
- **Paddle**: verifica account in 3 fasi (Domain Review → Business → Identity).

## 3. Precondizioni
- Sito vetrina **live in HTTPS** con i 3 documenti legali linkati (UC 0002/0036) — requisito Domain Review (#14 A1).
- Decisione su **trigger P.IVA** = "sto per far partire un'attività a pagamento abituale" (non il payout) (_COMMERCIALISTA 5/7).

## 4. Flusso principale
1. **Fase free** (validazione senza incasso ricorrente): titolare = **persona fisica** (nome+indirizzo domiciliazione+email); sito + waitlist/free, **nessuna P.IVA** (Via A) (#14 D21, _COMMERCIALISTA 7).
2. **Commercialista**: verificare F1–F9 (forfettario/ATECO, INPS Gestione Separata, classificazione payout Paddle, IVA/reverse-charge/VIES, timing, saldo trattenuto, domiciliazione, identità titolare).
3. **Domiciliazione/virtual office**: impostare indirizzo commerciale (poche centinaia €/anno) (#14 D23, F8).
4. **Alla monetizzazione**: aprire **P.IVA forfettaria ditta individuale** (Via B), aggiornare i documenti (aggiunta P.IVA) (#14 D21).
5. **Paddle**: registrarsi come **Individual** (nessun documento societario) → sottomettere **sito vetrina** + **`app.appgrove.app`** → superare Domain Review/Business/Identity → account **live** (#14 A3, _COMMERCIALISTA 1/2).

## 5. Flussi alternativi / edge / errori
- **Sito non live** → Domain Review **non avviabile** (vale anche per sandbox: no sito, nessun account nemmeno di test) → unica via = **stub locale** (#09 I, #14 ⛔).
- **Saldo Paddle < €100** → payout si accumula (non cambia l'obbligo P.IVA, che precede il payout) (_COMMERCIALISTA 2/5).
- **Operare a pagamento senza P.IVA** → fattispecie non conforme (no "periodo di prova" legale); rischi sanzioni/arretrati/ravvedimento (_COMMERCIALISTA 6) — **da evitare**.
- **Pricing non finale per Paddle**: screenshot ok in Domain Review (#14 11).

## 6. Risorse & runbook
**Artefatti**: checklist fiscale ([_COMMERCIALISTA](../../_COMMERCIALISTA.md) F1–F9), entità legale ([_REVISIONE-LEGALE](../../_REVISIONE-LEGALE.md) L11),
dati pubblicati (nome legale, sede domiciliazione, email, P.IVA quando attiva — #14 D22). **Runbook**: free (persona fisica) →
commercialista → domiciliazione → P.IVA alla monetizzazione → account Paddle + Domain Review. **Non è codice**: è il
prerequisito business che sblocca #09.

## 7. Dati toccati
Dati del **titolare** pubblicati (nome/sede/email/P.IVA) nei documenti legali/footer. Pubblicando il sito si trattano dati →
si è **titolari** anche in fase free (#14 D21, F9). Nessun dato applicativo qui. Coerenza Paddle MoR (#09 J).

## 8. Permessi & gate
- **Invarianti tecnici**: N/A (azione business). **Gate di sequenza**: #14 (sito + 3 legali) è **prerequisito bloccante** per
  qualsiasi uso del vero Paddle (sandbox incluso) → questo UC dipende da UC 0002/0036.

## 9. Requisiti di test
Nessun test software. **Verifica di completamento**: sito live con 3 legali linkati; account Paddle **verificato/live**;
entità legale corretta nei documenti; checklist commercialista evasa (F1–F9). Aggiornare gli stati in _COMMERCIALISTA/_REVISIONE-LEGALE.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 A1/A3/D21/D22/D23/11, _COMMERCIALISTA 1–8, #09 J, _REVISIONE-LEGALE L11.
- **DoD**:
  1. Inquadramento fiscale chiarito col commercialista (F1–F9); P.IVA forfettaria aperta alla monetizzazione.
  2. Domiciliazione attiva; entità legale aggiornata nei documenti.
  3. Account Paddle Individual verificato/live (Domain Review superata) con sito + `app.appgrove.app`.
  4. Sequenza rispettata: #14 prima del vero Paddle (stub locale per sviluppare prima).
