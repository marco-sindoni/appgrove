# Albero delle soglie — valutazione del rischio per gli interessati

Riferimento della fase **ASSESS** del [runbook](../../../../docs/compliance/breach-runbook.md) e del **Passo 4**
della skill. Decide la conseguenza della violazione: **solo registro**, **Garante entro 72h**, oppure **Garante +
interessati**. Fonte: #13 J57–J58, criteri EDPB, art. 33/34 GDPR.

## L'albero

```
Violazione di DATI PERSONALI confermata?
│
├─ NO  → non è un data breach ai fini GDPR (magari è un incidente di sicurezza senza dati personali).
│        Valuta se tracciarlo altrove. Fine.
│
└─ SÌ  → i dati erano CIFRATI / resi inintelligibili, con CHIAVI NON compromesse?  (leva art. 34.3)
         │
         ├─ SÌ, robustamente → rischio tipicamente "improbabile": ramo IMPROBABILE,
         │                     motivando con la cifratura.
         │
         └─ NO / parziale / chiavi compromesse → pesa i criteri EDPB (sotto):
                │
   ┌────────────┼─────────────────────────────┬──────────────────────────────┐
   │            │                             │                              │
IMPROBABILE   RISCHIO (non elevato)     RISCHIO ELEVATO
   │            │                             │
→ NIENTE      → Garante ENTRO 72h        → Garante ENTRO 72h (art. 33)
  notifica      (art. 33) + registro      + INTERESSATI "senza ingiustificato
→ SOLO                                      ritardo" (art. 34) + registro
  registro +
  motivazione
  del "no-rischio"
```

## Leva cifratura (art. 34.3)

Se i dati colpiti erano **cifrati at-rest e in-transit** (encryption ovunque, #06 §20bis) o comunque **resi
inintelligibili** a chi vi ha avuto accesso, e le **chiavi non sono state compromesse**:

- la **notifica agli interessati** (art. 34) **spesso non è dovuta**;
- il rischio complessivo può scendere a **"improbabile"**.

**Documenta sempre la cifratura come motivazione** nel registro. **Eccezione**: se sono state compromesse
**anche le chiavi**, la leva **non** si applica — tratta i dati come in chiaro.

> **Attenzione — la cifratura a riposo non aiuta se il difetto espone i dati già decifrati.** La leva vale solo
> se chi ha avuto accesso ha ottenuto dati *inintelligibili*. Se la violazione avviene a un livello dove i dati
> sono **già in chiaro** (un endpoint/API senza autorizzazione che restituisce contenuti decifrati, un log che
> stampa dati leggibili, un export non protetto), la cifratura at-rest del volume **non riduce il rischio**: i
> dati sono stati esposti in chiaro. La leva art. 34.3 si applica al *dato inintelligibile*, non al *volume cifrato*.

## Criteri EDPB (nessuno decide da solo — si pesano insieme)

| Criterio | Alza il rischio quando… |
|---|---|
| **Tipo di violazione** | è compromessa la **riservatezza** (accesso non autorizzato) più che la sola disponibilità. |
| **Natura/sensibilità dei dati** | ci sono **categorie particolari art. 9** (salute, biometrici, genetici, orientamento, …), credenziali, dati finanziari. |
| **Volume** | grande quantità di dati per singolo interessato. |
| **Identificabilità** | gli interessati sono **facilmente identificabili** dai dati esposti. |
| **Gravità delle conseguenze** | possibili frode, furto d'identità, danno reputazionale/economico, discriminazione. |
| **Soggetti vulnerabili** | minori o altre categorie vulnerabili tra gli interessati. |
| **Numero di interessati** | molte persone coinvolte. |

## Regole di decisione

- **Art. 9 in gioco** → il rischio è quasi sempre almeno "rischio", spesso "elevato": **non minimizzare**, ed
  **escala** (fermati e chiedi conferma).
- **Nel dubbio tra due soglie** → scegli la **più cautelativa** (notificare). Il costo di una notifica in più è
  basso, quello di una mancata dovuta è alto.
- **Motivazione obbligatoria** per l'esito "improbabile" (art. 33.5): senza motivazione la voce di registro è
  incompleta.

## Casi tipo (per auto-verifica del ragionamento)

| Scenario | Esito atteso |
|---|---|
| Backup **cifrato** perso, chiavi al sicuro | **Improbabile** → solo registro (leva cifratura), motivazione = cifratura. |
| Log applicativi senza dati personali esposti per errore | **Non è un data breach** di dati personali (o improbabile) → traccia/registra. |
| Elenco email di iscritti newsletter esposto in chiaro (no art. 9) | **Rischio** → Garante entro 72h + registro. |
| Esposizione in chiaro di **dati sanitari** (art. 9) di molti interessati | **Rischio elevato** → Garante 72h + interessati (art. 34) + registro; **escala**. |
| Credenziali (anche solo hash deboli) di utenti esposte | **Rischio/elevato** secondo robustezza dell'hashing e riuso → cautelativo: notificare. |
