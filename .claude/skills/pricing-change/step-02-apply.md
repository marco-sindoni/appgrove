# Step 02 — Applica il cambio (con la fee davanti)

Le modifiche passano **sempre** dal tool (`tools/pricing-change/change.mjs`), mai a mano: sono deterministiche e
non violano l'immutabilità per distrazione. Usa `--dry-run` per mostrare il risultato prima di scrivere.

## Mostra la fee effettiva — è l'arma principale, non un veto

Per **ogni** prezzo che introduci o cambi (mensile **e** annuale), calcola e mostra la **fee effettiva** + il
**netto**, prima della conferma (#09 K47):

```bash
( cd tools/pricing-change && node change.mjs fee --amount <centesimi> --cycle monthly --amount <centesimi> --cycle annual )
```

La parte fissa del fornitore (~€0,50/transazione) pesa sui prezzi **bassi e mensili**: sotto i ~€5-6/mese la fee
effettiva supera il 10% e il tool accende un **avviso soft**. Non è un blocco (#09 K47/K48): riportalo con
franchezza, indica i due rimedi naturali — un prezzo più alto, oppure spingere l'**annuale** (una transazione
l'anno → fee molto più bassa) — e lascia la scelta allo sviluppatore. In **autopilot**, se il prezzo che
proponi accende l'avviso, dillo e chiedi conferma: il prezzo è denaro, è un caso di escalation.

## Nuovo tier / aggiungi ciclo / cambia limiti

Casi diretti, senza immutabilità (nessun prezzo esistente viene mutato):

```bash
# nuovo tier (con i suoi limiti e prezzi)
node change.mjs add-tier --slug <slug> --tier <chiave> --name "<nome>" --metric <metrica> --cap <n> --type <stock|flow> [--window month] \
  --cycle monthly --amount <centesimi> --cycle annual --amount <centesimi> [--trial-days 14]

# aggiungi l'annuale (o il mensile) a un tier esistente
node change.mjs add-cycle --slug <slug> --tier <chiave> --cycle annual --amount <centesimi>

# cambia i limiti di un tier (non tocca i prezzi)
node change.mjs set-limits --slug <slug> --tier <chiave> --metric <metrica> --cap <n> --type <stock|flow> [--window month]
```

Attenzione alla **natura della metrica**: `stock` (a giacenza, nessuna `window`) vs `flow` (a consumo, con
`window`). Deve corrispondere a come il `QuotaService` dell'app conta l'uso: le due cose si cambiano insieme.
Se qui la natura risultasse sbagliata, è un cambio più profondo (tocca il codice dell'app) — fermati e
traccialo, non forzarlo dal listino.

## Cambio prezzo — scegli la via (immutabilità)

**Questa è la decisione delicata, ed è tua** (SKILL.md «Il concetto da spiegare prima di tutto»). Chiedi allo
sviluppatore se il prezzo è **già vivo**, cioè già pubblicato/sincronizzato sul fornitore di pagamento
(tipicamente: l'app è già in vendita in produzione). Lo YAML non lo sa — solo tu/lui lo sapete.

- **Non ancora vivo** (bozza, tipicamente prima del lancio): l'importo si corregge sul posto.

  ```bash
  node change.mjs change-price --slug <slug> --tier <chiave> --cycle monthly --amount <centesimi> --in-place
  ```

- **Vivo** (immutabile): il nuovo prezzo si porta con un **nuovo tier**; il vecchio resta definito per gli
  abbonati esistenti (grandfathering — la decisione su di loro è step-03). Scegli una chiave nuova, stabile e
  parlante (es. `team_2026`).

  ```bash
  node change.mjs change-price --slug <slug> --tier <chiave-vecchia> --cycle monthly --amount <centesimi> --new-tier <chiave-nuova>
  ```

  Il tool clona il tier sorgente (limiti, prova, gli altri cicli invariati) nel nuovo, con il nuovo importo per
  il ciclo indicato, e **lascia intatto** il vecchio. Se anche l'annuale cambia, applica un secondo
  `change-price ... --cycle annual` sullo **stesso** nuovo tier con `--in-place` (il nuovo tier non è ancora
  vivo), oppure passa entrambi gli importi al momento della clonazione.

**In dubbio, la via sicura è il nuovo tier**: non viola mai l'immutabilità. Il comando `change-price` rifiuta
di procedere senza una via esplicita (`--in-place` o `--new-tier`), proprio per non indovinare al posto tuo.

## Verifica

Dopo la scrittura, mostra il risultato e ottieni conferma esplicita. Controlla che il listino ricarichi contro
lo schema del core (i test del catalogo leggono i file del pricing):

```bash
( cd services && mvn -B -pl core -am test )
```

## Registra

La via scelta per il cambio prezzo (in loco vs nuovo tier, con la ragione), gli importi e la lettura della fee
(soprattutto se accettata sopra la soglia del 10%) vanno in `decisions.json` appena decisi. Prosegui con
`step-03-grandfathering.md` (obbligatorio se hai cambiato un prezzo su un tier che può avere abbonati).
