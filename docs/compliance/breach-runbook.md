# Runbook — Risposta alle violazioni di dati personali (data breach)

**Documento INTERNO** (come la RoPA e il registro breach). Non pubblicare.
**Fonte decisioni**: #13 J56–J64, #08 (detection/audit), #06 §20bis (cifratura). Use case: [0049](../usecases/10-skills-tooling/0049-skill-breach-response.md).
**Co-pilota operativo**: skill [`/breach-response`](../../.claude/skills/breach-response/SKILL.md) — durante un incidente, guida l'albero delle soglie e redige registro + bozze di notifica.

> **Il termine delle 72 ore parte da "quando si viene a conoscenza"**, non da quando si finisce di
> indagare. Questo runbook esiste per essere **già pronto** al momento dell'incidente: leggilo a freddo
> almeno una volta, così che durante un incidente reale si esegua e non si improvvisi.

---

## 0. Prerequisiti operativi (da verificare a freddo, non durante l'incidente)

- [ ] **Casella `security@appgrove.app` attiva e monitorata** — è il canale di responsible disclosure
  pubblicato in `security.txt`. *Azione del founder*: creare la casella/alias e assicurarsi che le
  segnalazioni arrivino a chi risponde agli incidenti. (Il file `security.txt` è già pubblicato sul sito:
  [site/public/.well-known/security.txt](../../site/public/.well-known/security.txt).)
- [ ] **Accesso agli strumenti di detection e scoping** (#08): allarmi anomalie, registro audit, error
  tracking, CloudWatch Logs Insights.
- [ ] **Manifesti dati a portata di mano** ([docs/compliance/manifests/](manifests/)): dicono, per ogni
  trattamento, *quali* categorie di dati e *quali* interessati sono coinvolti — indispensabili per lo scoping.
- [ ] **Registro breach** pronto ad accogliere le voci: [breach-register.md](breach-register.md).
- [ ] **Recapiti del Garante** (Garante per la protezione dei dati personali) per la notifica art. 33 e la
  procedura telematica; **contatto del legale** per la validazione (revisione L12).

---

## 1. Le cinque fasi

### 1.1 DETECT — rilevare
La violazione emerge da uno di questi canali:
- **Detection interna (#08)**: allarme su anomalie, picco di errori, accesso anomalo nel registro audit.
- **Scoperta esterna**: segnalazione via `security@appgrove.app` / `security.txt` (responsible disclosure).
- **Segnalazione di un tenant** o di un fornitore (sub-processor).

Appena una violazione è **plausibile**, **annota data e ora di conoscenza**: è l'istante da cui corrono le
72 ore. Apri subito un ticket nel ticketing interno e una voce provvisoria nel [registro breach](breach-register.md).

### 1.2 ASSESS — valutare il rischio (→ §2 albero delle soglie)
Determina la **natura** della violazione (riservatezza / integrità / disponibilità), **cosa** e **chi** è
colpito (→ §3 scoping), e applica l'**albero delle soglie** (§2) per decidere: solo registro, Garante, o
Garante + interessati. Qui si applica anche la **leva cifratura** (§2.1).

### 1.3 CONTAIN — contenere
Ferma l'emorragia **prima** di rincorrere le notifiche (ma senza fermare il cronometro delle 72h):
- revoca credenziali/token compromessi; l'isolamento per-tenant limita già il raggio (`tenant_id` dal JWT);
- chiudi la falla (patch, regola IAM, rotazione segreti); preserva le **prove** (log, snapshot) per il registro;
- se il canale è un sub-processor, coordina il contenimento con lui.

### 1.4 NOTIFY — notificare (per ruolo → §4)
In base all'esito di §2 e al **ruolo** di appgrove nel trattamento colpito (titolare vs responsabile):
notifica **Garante** (entro 72h), **interessati** (se rischio elevato), o **il tenant-titolare** (se appgrove
è responsabile dei dati di un'app B2B). Le bozze le produce la skill; i template sono in
[.claude/skills/breach-response/reference/template-notifiche.md](../../.claude/skills/breach-response/reference/template-notifiche.md).

### 1.5 DOCUMENT — documentare
Completa la voce nel [registro breach](breach-register.md): fatti, effetti probabili, misure adottate e
proposte, **esito della valutazione** con motivazione (anche il "no-rischio" va motivato, art. 33.5), e la
**decisione di notifica** con le date. Traccia tutto nel ticketing interno. Chiudi l'incidente solo quando
registro e notifiche sono completi.

---

## 2. Albero delle soglie (rischio per gli interessati)

Riferimento di dettaglio: [reference/albero-soglie.md](../../.claude/skills/breach-response/reference/albero-soglie.md) (usato anche dalla skill).

```
Violazione di dati personali confermata?
│
├─ NO  → non è un data breach ai fini GDPR. Valuta comunque se è un incidente di sicurezza
│        da tracciare altrove. Fine.
│
└─ SÌ  → i dati coinvolti sono CIFRATI / resi inintelligibili a chi non è autorizzato?  (leva art. 34.3, §2.1)
         │
         ├─ SÌ, robustamente → il rischio scende, spesso a "improbabile": vai al ramo "improbabile"
         │                     documentando la cifratura come motivazione.
         │
         └─ NO / parziale     → valuta il rischio con i criteri EDPB (§2.2):
                                │
   ┌────────────────────────────┼─────────────────────────────────┐
   │                            │                                 │
RISCHIO IMPROBABILE        RISCHIO (non elevato)           RISCHIO ELEVATO
per i diritti/libertà      per i diritti/libertà           per i diritti/libertà
   │                            │                                 │
→ NIENTE notifica          → Garante ENTRO 72h              → Garante ENTRO 72h (art. 33)
→ SOLO registro +            (art. 33) + registro           + INTERESSATI "senza
  motivazione del                                             ingiustificato ritardo" (art. 34)
  "no-rischio"                                              + registro
```

### 2.1 Leva cifratura (art. 34.3)
Se i dati colpiti erano **cifrati at-rest e in-transit** con chiavi non compromesse (encryption ovunque,
#06 §20bis) o comunque **resi inintelligibili** a chi ha avuto accesso, la notifica **agli interessati** (art.
34) **spesso non è dovuta**, e il rischio può scendere a "improbabile". La cifratura va **documentata come
motivazione** nel registro. Attenzione: se sono state compromesse **anche le chiavi**, la leva non si applica.

### 2.2 Criteri di valutazione (EDPB)
Pesa tutti questi fattori — nessuno da solo decide:
- **tipo di violazione** (riservatezza / integrità / disponibilità);
- **natura, sensibilità e volume** dei dati — **categorie particolari art. 9** (salute, biometrici, …) alzano
  il rischio in modo marcato;
- **facilità di identificazione** degli interessati;
- **gravità delle conseguenze** possibili (frode, furto d'identità, danno reputazionale, discriminazione);
- **caratteristiche degli interessati** (presenza di **soggetti vulnerabili**);
- **numero** di interessati coinvolti.

> **Regola prudenziale**: nel dubbio tra "improbabile" e "rischio", **notifica**. Il costo di una notifica
> in più è basso; quello di una notifica mancata dovuta è alto.

---

## 3. Scoping rapido (chi/cosa è colpito)

1. **Log strutturati e audit (#08)**: ricostruisci gli accessi e le operazioni; i log portano
   `tenant_id`/`app_id`/`user_id`, quindi delimitano l'impatto **per-tenant**.
2. **Isolamento per-tenant**: il filtro row-level `WHERE tenant_id = :tid` limita per costruzione il raggio di
   una compromissione a un singolo account, salvo violazioni a livello di piattaforma.
3. **Manifesti dati** ([manifests/](manifests/)): per il trattamento colpito, dicono **categorie di dati** e
   **categorie di interessati** → traducono "questa tabella/oggetto" in "queste persone, questi dati".
4. Accedi **solo al necessario** per lo scoping: non ampliare l'accesso ai dati oltre quanto serve a
   dimensionare l'incidente.

---

## 4. Notifiche per ruolo (A)

Il **ruolo** di appgrove nel trattamento colpito determina *chi* si notifica:

- **appgrove TITOLARE** (dati della piattaforma e dei consumatori): notifica **il Garante** entro 72h (se
  rischio ≥ non-elevato) e, se **rischio elevato**, **gli interessati** senza ingiustificato ritardo (art. 34).
- **appgrove RESPONSABILE** (dati trattati per conto di un'app/tenant B2B): **non** notifica direttamente il
  Garante/gli interessati; **notifica il tenant-titolare senza ritardo** (art. 33.2), fornendogli gli elementi
  utili. Sarà il tenant a valutare e a notificare autorità/interessati. Traccia la comunicazione nel ticketing.

Bozze e template (IT/EN) per Garante, interessati e controller B2B:
[reference/template-notifiche.md](../../.claude/skills/breach-response/reference/template-notifiche.md).
**Le bozze prodotte dalla skill NON sono validate legalmente**: prima dell'invio, revisione legale (L12,
[docs/_REVISIONE-LEGALE.md](../_REVISIONE-LEGALE.md)).

---

## 5. Timeline 72 ore (promemoria)

| Momento | Azione |
|---|---|
| **T0 — conoscenza** | Annota data/ora. Apri ticket + voce provvisoria nel registro. |
| **T0 → contenimento** | Contieni (§1.3) preservando le prove. |
| **Entro 72h** | Se rischio ≥ non-elevato e appgrove è titolare: **notifica al Garante** (art. 33). Se non tutte le informazioni sono disponibili, è ammessa la **notifica in fasi**. |
| **Senza ingiustificato ritardo** | Se rischio **elevato**: **notifica agli interessati** (art. 34). |
| **Alla chiusura** | Completa il registro (fatti/effetti/azioni/motivazione/decisione). |

> Se le 72h scadono, si notifica **comunque**, indicando i **motivi del ritardo** (art. 33.1).

---

## 6. Responsible disclosure

Le segnalazioni esterne di vulnerabilità arrivano via **`security@appgrove.app`** e sono descritte in
**`security.txt`** ([RFC 9116](https://www.rfc-editor.org/rfc/rfc9116)) pubblicato sul sito. Una segnalazione
credibile che indica un accesso non autorizzato **alimenta questo runbook dalla fase DETECT**. Rispondi al
segnalante, valuta, e — se si conferma una violazione di dati personali — apri l'incidente.

---

## 7. Quando invocare la skill

Durante un incidente, esegui **`/breach-response`**: ti chiede i fatti, percorre con te l'albero delle soglie,
applica la leva cifratura, individua il ruolo, e **redige la voce del registro e le bozze di notifica** (IT/EN)
pronte per la revisione legale. La skill è un **co-pilota**: assiste fino a una bozza solida, non sostituisce
la decisione del responsabile né la validazione del legale.
