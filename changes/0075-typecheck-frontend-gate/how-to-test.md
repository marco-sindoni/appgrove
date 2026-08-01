# Come verificare a mano la change 0075 — il controllo dei tipi entra nel cancello del frontend

Change di strumenti: quello che va guardato con i propri occhi è il **comportamento del cancello** (diventa
davvero rosso quando deve?) e la **non-regressione visiva** del componente corretto.

Tutti i comandi partono dalla radice del repository, sul branch `change/0075-typecheck-frontend-gate`.

---

## 1. Il controllo dei tipi passa così com'è

**Azione**

```bash
cd frontend && npm run typecheck; echo "uscita=$?"; cd ..
```

**Risultato atteso** — scorre l'elenco dei sette progetti (`api-client`, `design-system`, `error-reporter`,
`i18n`, `paddle-stub`, `admin`, `backoffice`), nessuna riga di errore, `uscita=0`.
*(Prima di questa change lo stesso comando usciva con 2 e due errori: `PageHeader.tsx(5,18) TS2430` e
`privacy.spec.ts(92,43) TS2550`.)*

---

## 2. Il cancello esegue davvero il controllo — e lo dice

**Azione**

```bash
./run-tests.sh frontend
```

**Risultato atteso** — nel registro a video, in quest'ordine:

1. l'intestazione dell'area è `━━ FRONTEND — frontend/ (tsc --noEmit + npm test + Playwright e2e)`;
2. `✓ frontend: pacchetti-libreria costruiti`;
3. **`✓ frontend: controllo dei tipi verde`** ← il passo nuovo, prima delle altre suite;
4. `✓ frontend: unit/component verdi`;
5. `✓ frontend: e2e verdi`;
6. riepilogo con `✓ frontend` e uscita 0.

Se il passo 3 non compare, il cancello non è agganciato: la change non ha funzionato.

---

## 3. Il cuore della verifica — il cancello diventa rosso su un errore di tipo

Questo è il controllo che dimostra il valore della change: prima, un errore di tipo passava **inosservato**
perché `vite build` traspila senza verificare i tipi.

**Azione (a) — introdurre di proposito un errore.** Aggiungi in fondo a un file qualsiasi del frontend, per
esempio `frontend/packages/design-system/src/lib/cn.ts`, una riga palesemente sbagliata:

```ts
export const erroreDiProposito: number = 'questa è una stringa, non un numero'
```

**Azione (b) — eseguire il cancello.**

```bash
./run-tests.sh frontend; echo "uscita=$?"
```

**Risultato atteso**

- compare l'errore del compilatore con file, riga e codice, del tipo
  `src/lib/cn.ts(N,14): error TS2322: Type 'string' is not assignable to type 'number'.`;
- compare la riga rossa **`✗ frontend: controllo dei tipi fallito (tsc --noEmit)`**;
- l'area frontend risulta `✗ frontend` nel riepilogo e `uscita=1`;
- **attenzione a questo dettaglio**: le suite successive (vitest ed end-to-end) girano lo stesso e possono
  restare verdi — è voluto, `run-tests.sh` non si ferma al primo errore. Ciò che deve essere rosso è
  **l'area** e l'uscita complessiva.

**Azione (c) — rimuovere l'errore e riprovare.**

```bash
git checkout -- frontend/packages/design-system/src/lib/cn.ts
./run-tests.sh frontend; echo "uscita=$?"
```

**Risultato atteso** — torna tutto verde, `✓ frontend`, `uscita=0`. Verifica anche con `git status` che
l'albero di lavoro sia pulito, cioè che l'errore di prova non sia rimasto in giro.

---

## 4. La prova che la costruzione da sola NON basta (facoltativa, ma è il perché della change)

**Azione** — reintroduci l'errore del punto 3(a) e lancia **solo** la costruzione:

```bash
cd frontend && npm run build --workspace packages/design-system; echo "uscita build=$?"; cd ..
```

**Risultato atteso** — l'errore viene soltanto **stampato** dal generatore delle dichiarazioni, ma
`uscita build=0` e l'artefatto viene prodotto. È esattamente il buco che questa change chiude: la
costruzione non è un cancello sui tipi. Poi rimuovi l'errore (`git checkout -- <file>`).

---

## 5. `PageHeader` si rende ancora bene — controllo visivo

La correzione tocca solo le dichiarazioni di tipo, quindi a video **non deve cambiare nulla**. Va guardato
con gli occhi perché è l'unico file di prodotto modificato.

**Azione**

```bash
./app-start.sh
```

Poi apri il backoffice nel browser (l'indirizzo è quello stampato dallo script; in locale è servito da Caddy)
e accedi con un utente di prova.

**Risultato atteso** — l'intestazione di pagina è identica a prima in tutte e tre le sue varianti:

| Dove guardare | Cosa deve apparire |
|---|---|
| Backoffice → **Fatture** | riquadro icona colorato a sinistra, titolo in grassetto accanto, sottotitolo attenuato sotto, pulsante di azione allineato a destra sulla stessa linea |
| Backoffice → **Contatti** (modulo CRM) | stessa forma con l'icona del proprio modulo |
| Backoffice → **Membri** | riquadro icona + titolo + sottotitolo, **senza** pulsanti di azione a destra |
| Pannello di amministrazione → **Panoramica**, **Account**, **Utenti**, **Fatturazione**, **Diritti GDPR** | titolo grande senza icona; dove previsto, il sottotitolo attenuato |

Nessun titolo deve risultare vuoto, troncato o spostato rispetto a prima. Al termine: `./app-stop.sh`.

**Variante più rapida (senza stack)** — le stesse varianti sono visibili in Storybook:

```bash
cd frontend && npm run storybook
```

e apri `Components/PageHeader`, storie **Piattaforma** e **PaginaApp**: la seconda contiene un titolo con
pulsante di azione composto, cioè proprio il caso che rende necessario tenere il titolo come contenuto React.

---

## 6. Il cancello esiste anche nell'integrazione continua

**Azione** — controllo a lettura, nessun comando da eseguire su GitHub:

```bash
grep -n -A 3 "Controllo dei tipi" .github/workflows/verify-pr.yml
```

**Risultato atteso** — nel lavoro `frontend` compare il passo `Controllo dei tipi (tsc --noEmit)`, con
`working-directory: frontend`, comando `npm run typecheck` e la stessa condizione di percorso del passo
vitest (`needs.changes.outputs.frontend == 'true'`), collocato **prima** di unit/component.
La conferma sul campo arriverà alla prima richiesta di unione che tocca `frontend/`.

---

## 7. L'aiuto dello script è rimasto integro

L'intestazione di `run-tests.sh` è cresciuta di tre righe e la funzione che stampa l'aiuto legge un
intervallo di righe fisso: va verificato che non sia rimasto troncato.

**Azione**

```bash
./run-tests.sh -h | tail -6
```

**Risultato atteso** — l'aiuto termina con la sezione `Uso:` completa, fino alla riga `./run-tests.sh -h`;
non deve interrompersi a metà di una descrizione d'area né mostrare righe di codice della shell.
