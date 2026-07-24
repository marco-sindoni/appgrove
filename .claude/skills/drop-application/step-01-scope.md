# Step 01 — Identità e de-generazione

Tutti i comandi si lanciano dalla radice del monorepo `/Users/msindoni/Projects/appgrove`.

## Apri dentro una change

Questa skill non inventa il proprio flusso: gira **dentro** `new-change`, che porta il branch, i gate e il
registro delle decisioni. Avviala dichiarando lo use case sorgente (UC 0048) e la modalità:

```
/new-change
```

Descrizione: `drop app <app_id> — dismissione`. Da qui valgono i gate di `new-change`: revisione dei
requisiti, consenso al commit, consenso al merge.

## Gate di identità — verifica, non inventare

L'`app_id` è la stessa chiave di schema, code, rotta e istanza Terraform: va **verificato**, non indovinato.

1. **Quale app si dismette?** Deve essere un'app reale del marketplace. Elenca ciò che esiste e conferma:

   ```bash
   ./dev.sh services          # la mappa servizio → app_id → porta → schema
   ```

   `platform`, `core`, `auth` (e gli altri riservati) **non si dismettono**: sono piattaforma. Il
   de-generatore li rifiuta, ma il controllo è tuo, prima.

2. **L'app esiste ancora?** Se `services/<app_id>` non c'è, o è già stata dismessa, fermati: non c'è nulla
   da fare (il de-generatore uscirebbe con errore "app non trovata").

## Mostra il piano, poi de-genera

Il de-generatore è deterministico. **Sempre `--dry-run` prima**, mostra allo sviluppatore cosa toccherà,
poi eseguilo davvero:

```bash
node tools/drop-application/remove.mjs --app-id <app_id> --dry-run
node tools/drop-application/remove.mjs --app-id <app_id>
```

Rimuove i due alberi dell'app (servizio + modulo frontend), i tre file nelle cartelle condivise (test
end-to-end, manifesto dati, listino), disfa le cinque modifiche ai file condivisi e **rigenera la RoPA**.

**Non** tocca l'infrastruttura, non lancia la purga, non tocca il database: quelli sono passi del runbook
(step-04), dopo il merge. Lo strumento li stampa in coda come promemoria — è voluto.

## Verifica la de-generazione

```bash
./dev.sh services          # l'app NON deve più comparire
./run-tests.sh tooling     # il round-trip e gli inversi restano verdi
```

Se una cartella auto-scoperta (`app-start.sh`, `dev/Caddyfile`, `dev/lib/*.sh`, `tools/smoke/*`, workflow)
menziona ancora l'app, **non** modificarla a mano: significa che la scoperta automatica non l'ha vista
sparire — è un difetto della scoperta, da correggere lì, non un passo di questa skill.

Prosegui con `step-02-subscribers.md`.
