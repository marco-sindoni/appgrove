# Step 04 — Runbook e chiusura

## Il runbook — gli unici passi che restano manuali

La skill ha disfatto il codice (reversibile con git). Restano gli atti **irreversibili o verso l'esterno**,
che si eseguono **dopo il merge**, nell'ordine, con le loro protezioni. Consegna un runbook esplicito nel
`implementation-log.md` della change:

1. **Abbonati** — applica il trattamento deciso a step-02 (disdetta a fine periodo / migrazione /
   comunicazione). Nessun passo successivo prima che questo sia fatto (#09 H35).
2. **Export garantito** — assicurati che la finestra di export sia stata data agli utenti (diritti, #13 D).
3. **Dati (purga)** — `core offboard-app <app_id>`: accoda la purga per ogni tenant, con audit.
4. **Infrastruttura** — `./infra/scripts/service-remove <app_id>` (rimuove l'istanza del modulo da test e
   prod), poi il destroy mirato con safety #06 K e flusso PR→CI (#07 19):
   `terraform -chdir=infra/envs/<env> destroy -target='module.app_<app_id>'`.
5. **Database (fisico)** — `pg_dump --schema=app_<app_id>` se serve un archivio, poi
   `DROP SCHEMA app_<app_id> CASCADE; DROP ROLE app_<app_id>;` e rimozione del segreto
   `appgrove/<env>/<app_id>/db` (Secrets Manager).
6. **Listino** — l'app è già tolta da `pricing/index.yaml`: al prossimo sync i price vengono archiviati
   (soft-delete) se privi di subscription attive; verso il fornitore reale è bloccato finché l'account non
   esiste (#14).

## Esegui ogni suite toccata

La dismissione tocca più aree: **tutte** devono restare verdi.

```bash
./run-tests.sh tooling     # round-trip del de-generatore + inversi
./run-tests.sh backend     # il comando offboard-app e la sua orchestrazione
./run-tests.sh compliance  # RoPA senza drift dopo la rimozione del manifesto
./run-tests.sh frontend    # registry coerente senza il modulo dismesso
```

Infrastruttura validata, mai applicata:

```bash
( cd infra/envs/test && terraform fmt -check && terraform validate )
```

## Checklist di consegna

- servizio, modulo frontend, test end-to-end di livello 2, **journey di piattaforma**, manifesto, listino:
  **rimossi**; RoPA rigenerata senza drift
- sei modifiche condivise disfatte (pom, registry, package.json, pricing index, elasticmq, **registro di
  copertura end-to-end**): un journey orfano non solo non collauda nulla, rende rossa l'area `tooling`
- comando `offboard-app` disponibile e testato; runbook scritto con tutti i passi irreversibili
- trattamento abbonati **deciso dallo sviluppatore** e registrato
- decisioni tutte in `decisions.json`; punti aperti (landing, audit oltre DROP SCHEMA) tracciati nei loro UC

## STOP obbligatorio — consegna a new-change

I gate di commit e merge sono di `new-change` e **non** sono indeboliti qui. Stampa:

```
🛑 App <app_id> dismessa nel codice (servizio + modulo frontend + journey e voci di copertura + manifesto + listino + modifiche condivise disfatte; RoPA rigenerata).
   Suite: <esiti reali per area>
   Runbook degli atti irreversibili (infra/purga/DB) scritto nell'implementation-log: da eseguire DOPO il merge.
Rivedi, poi dai il consenso esplicito al commit. Non committo, non faccio merge, non distruggo nulla senza il tuo via libera.
```

Poi **fermati**. Questa skill scrive codice e lascia un branch. Non esegue `service-remove`, non lancia
`offboard-app`, non fa destroy, non tocca il database né il fornitore di pagamento — la distruzione è un
atto deliberato del runbook, con la persona presente.
