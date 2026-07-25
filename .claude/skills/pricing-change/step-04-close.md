# Step 04 — Runbook e chiusura

## Il runbook — gli unici passi che restano manuali

La skill ha scritto il pricing-as-code (reversibile con git). Restano gli atti **verso l'esterno o su abbonati
reali**, che si eseguono **dopo il merge**, con le loro protezioni. Consegna un runbook esplicito
nell'`implementation-log.md` della change quando servono:

1. **Sincronizzazione** — la propaga la pipeline di deploy (UC 0022), non la skill: al **merge** verso test la
   sync va sul fornitore **sandbox**; al **tag** verso produzione va sul fornitore **produzione** (#09 H37).
   Verso il fornitore reale è comunque bloccata finché l'account non esiste (#14). Il cambio prezzo si **prova
   in sandbox** automaticamente arrivando in test.
2. **Migrazione abbonati** (solo se decisa a step-03) — `PaymentProvider.changeSubscriptionTier` per ogni
   subscription dal vecchio tier al nuovo, con la comunicazione/preavviso decisi dallo sviluppatore. Mai prima
   del merge; mai in automatico.
3. **Ritiro del vecchio tier** (eventuale, più avanti) — quando non resta più alcun abbonato sul vecchio tier,
   lo si può rimuovere dal listino: alla sync successiva viene **archiviato** (soft-delete), perché privo di
   subscription attive. Finché ci sono abbonati, **resta** (grandfathering).

## Esegui le suite toccate

Almeno l'area che hai toccato dev'essere verde:

```bash
./run-tests.sh tooling      # il tool pricing-change: fee + modifiche al pricing-as-code (immutabilità)
```

Se hai modificato il listino di un'app **reale**, verifica anche che il catalogo ricarichi:

```bash
( cd services && mvn -B -pl core -am test )   # i test del catalogo leggono i file del pricing
```

## Checklist di consegna

- il cambio è passato dal **tool** (non a mano); il listino ricarica valido
- per un cambio prezzo: la **via** è corretta (in loco solo se non vivo; altrimenti **nuovo tier**), il vecchio
  prezzo **non è stato mutato**
- **fee effettiva** mostrata per ogni prezzo (mensile e annuale); se sopra il 10%, accettata consapevolmente
- **grandfathering deciso**: default (gli esistenti restano) o migrazione a runbook
- decisioni tutte in `decisions.json`; punti che appartengono ad altri use case tracciati nei loro file

## STOP obbligatorio — consegna a new-change

I gate di commit e merge sono di `new-change` e **non** sono indeboliti qui. Stampa:

```
🛑 Cambio di listino applicato a <slug> (<tipo di cambio>; pricing-as-code aggiornato, immutabilità rispettata).
   Fee effettiva: <esiti>   ·   Grandfathering: <default | migrazione a runbook>
   Suite: <esiti reali per area>
   Runbook (sync/migrazione): scritto nell'implementation-log — da eseguire DOPO il merge.
Rivedi, poi dai il consenso esplicito al commit. Non committo, non faccio merge, non sincronizzo, non migro nessuno senza il tuo via libera.
```

Poi **fermati**. Questa skill scrive YAML e lascia un branch. Non parla col fornitore di pagamento, non
sincronizza, non esegue migrazioni — la sync è della pipeline (UC 0022), la migrazione è un atto deliberato del
runbook, con la persona presente.
