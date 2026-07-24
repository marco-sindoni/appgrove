# Step 03 — Purga dati (pianificata, con audit)

La dismissione cancella i dati personali dell'app per **tutti** i tenant. La skill **pianifica** questa
cancellazione — non la esegue: è l'atto più irreversibile della dismissione, e vive nel runbook (step-04),
dopo il merge.

## Export prima della cancellazione — non negoziabile

Agli utenti va garantita la possibilità di **esportare** i propri dati prima della purga (diritti, #13 D).
Verifica che questa finestra sia stata data (o pianificala nel runbook con una tempistica), prima di
prevedere qualunque cancellazione. **Guardrail**: la pseudonimizzazione **non** è cancellazione (#13 L72):
la purga dell'app è **fisica** — `purgeData` cancella le righe e scrive un audit di prova (#13 L70).

## La primitiva di purga app-wide

La cancellazione per tutti i tenant è affidata al comando one-shot del core (introdotto dalla change 0043):

```bash
# eseguito nel runbook, dopo il merge — NON qui
core offboard-app <app_id>
```

`offboard-app` enumera i tenant con dati nell'app (subscription, incluse le soft-deleted) e accoda per
ciascuno un messaggio sulla coda `tenant-purge-<app_id>`; da lì il consumer dell'app esegue `purgeData`
(cancellazione fisica, filtro per `tenant_id`), purga la proiezione entitlement e scrive l'audit. La skill
si limita a **metterlo nel runbook** e a ricordare che l'audit dell'app vive nello schema `app_<id>`, che
la pulizia fisica del database eliminerà: la conservazione della prova oltre il `DROP SCHEMA` è retention
(UC 0035) — se rilevante per questa dismissione, annotala.

## RoPA — già rigenerata, verifica il risultato

Il de-generatore (step-01) ha rimosso il manifesto `docs/compliance/manifests/<app_id>.yaml` e rigenerato
i registri. Verifica che la RoPA non sia in drift e che l'app non compaia più:

```bash
( cd tools/compliance && npm run check )
```

La dismissione **chiude i trattamenti** dell'app: è la faccia compliance della rimozione. Se il `check`
è rosso per drift, rigenera (`npm run assemble`) e verifica di nuovo.

## Classificazione della change

Rimuovere un'app **non introduce** nuovi trattamenti, nuove finalità o nuove categorie di dati: chiude
quelli esistenti. Di norma non fa scattare un bump di versione di Informativa/Condizioni. Registra la
classificazione (di solito **minor**, con la motivazione) in `requirements.md` e nel log, come da gate
privacy di `new-change`.

Prosegui con `step-04-close.md`.
