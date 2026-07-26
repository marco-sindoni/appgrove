# Change 0062 — Lancio paid/social (UC 0043)

**Use case sorgente**: [docs/usecases/09-marketing-site/0043-lancio-paid-social.md](../../docs/usecases/09-marketing-site/0043-lancio-paid-social.md)
**Modalità**: autopilot · **Branch**: `change/0062-use-case-0043-lancio-paid-social`

## Inquadramento

UC 0043 è **operativo/non-software**: il "lancio" sono azioni del founder verso l'esterno (creare
gli account brand, pubblicare su Product Hunt, iscrivere le directory, avviare le campagne paid con
budget reale), guidate dalla skill `campaign-guide` (UC 0050, già esistente). Quelle azioni —
denaro, effetti irreversibili/verso l'esterno — **non sono eseguite dall'autopilot**: restano al
founder e sono tracciate come checklist nel runbook.

Questa change consegna gli **artefatti in-repo** che supportano e codificano il lancio, senza
anticipare altri use case:

1. **Cablaggio dei link social nel footer del sito** — chiude il punto differito posseduto da questo
   UC (tracciato dalla change `0047`). Idioma appgrove "cablato ma spento": non mostra nulla finché
   gli account brand non esistono.
2. **Runbook di lancio** `docs/_LANCIO.md` — registro vivo che codifica il lancio a due livelli, i
   canali, la strategia paid, la postura cookieless difesa, le convenzioni UTM e la checklist delle
   azioni founder.

**Fuori scope** (già altrove o azione founder): la skill `campaign-guide` (UC 0050), il blog (UC
0042), la newsletter backend (UC 0039), l'entità canonica/GEO (UC 0041), e l'**esecuzione reale del
lancio** (account, submit, spesa paid).

## Requisiti funzionali

### R1 — Link social nel footer, centralizzati in un file YAML di configurazione
- **Fonte unica**: `content/marketing/social.yaml` — file di configurazione del sito (stesso pattern
  di `content/legal/entity.yaml`), chiave `links:` con voci `{ label, href }`. Consegnato **vuoto /
  commentato**: alla creazione di un account brand (azione founder) basta aggiungere/scommentare la
  voce, **senza toccare codice**.
- `site/src/lib/social.ts` (nuovo): carica e valida lo YAML con `js-yaml` (come `legal.ts`) ed
  esporta `SOCIAL_LINKS: readonly { label: string; href: string }[]`. File assente, chiave assente o
  lista vuota → array vuoto. Ogni voce presente deve avere `label` non vuota e `href` URL **https
  assoluto**; una voce malformata è un **errore bloccante** (integrità, come i token legali). Espone
  anche la funzione pura `parseSocialLinks(raw)` per i test.
- `FooterContent` guadagna `socialHeading: string`; presente e tradotto nelle **5 lingue**
  (it/en/fr/es/de).
- `site/src/layouts/BaseLayout.astro` legge `SOCIAL_LINKS` da `lib/social.ts` e rende una `<nav>`
  social nel footer **solo se** non vuota (con file vuoto non cambia nulla di visibile; compilato →
  letto e mostrato direttamente).

### R2 — Runbook di lancio `docs/_LANCIO.md`
Documento vivo in italiano, plain language, che copre:
- lancio a due livelli (per-app sulla ICP + brand);
- canali off-site/directory con link (Product Hunt, AlternativeTo/G2/Capterra/SaaSHub, community
  indie/dev) → corroborazione + GEO;
- social organico brand (LinkedIn primario, X opzionale; no personale/no build-in-public; contenuti
  riusati dal blog);
- paid (Google Search primario + Meta dopo, budget piccoli di validazione);
- postura cookieless difesa (no pixel/CAPI con dati personali; obiettivo Traffico + Lead Form
  native; attribuzione UTM + goal Plausible + click piattaforme);
- rimando alla skill `campaign-guide` come porta obbligata di ogni campagna (checklist di conformità);
- **checklist azioni founder** (le escalation): creare account brand, submit Product Hunt, iscrivere
  directory, avviare paid — con il passo esplicito "aggiungere l'URL in `site/src/lib/links.ts`
  `SOCIAL_LINKS`" alla creazione degli account.
- puntatore aggiunto in `CLAUDE.md` accanto agli altri registri vivi (`_COSTI-AWS`, ecc.).

## Requisiti di test
- `site/src/lib/social.test.ts` (nuovo): `parseSocialLinks` restituisce un array; una lista valida è
  letta correttamente; una voce malformata (etichetta vuota o `href` non https assoluto) **lancia**;
  il file consegnato (vuoto/commentato) dà lista vuota. Il contratto è armato: voci future malformate
  diventano rosse.
- Suite `site` verde: `npm test` (vitest) + `astro build` + `postbuild-check.mjs` (il footer con
  `SOCIAL_LINKS` vuoto non introduce link rotti né rompe la parità 5 lingue).

## Aree toccate
- `site/` (Astro) → area di test **`site`**.
- `docs/` + `CLAUDE.md` → documentazione (nessun test).

## Invarianti
Nessuna toccata (marketing, niente dati tenant/JWT/query). Postura privacy: nessun tracking di dati
personali sul sito; misura aggregata Plausible; lead solo via Lead Form con consenso (UC 0039).
