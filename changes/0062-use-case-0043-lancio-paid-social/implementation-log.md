# Log di implementazione — Change 0062 (UC 0043, lancio paid/social)

**Modalità**: autopilot · **Aree**: `site/` (Astro) + documentazione.

## Inquadramento

UC 0043 è operativo/non-software: l'esecuzione del lancio (account brand, Product Hunt, directory,
campagne paid con budget reale) è azione del founder verso l'esterno → **non eseguita dall'autopilot**
(escalation: denaro, effetti esterni/irreversibili). La change consegna gli artefatti in-repo che
supportano e codificano il lancio, e traccia le azioni founder come checklist nel runbook.

## Cosa è stato fatto

### 1. Link social nel footer, centralizzati in un file YAML (R1)
- **Nuovo** [content/marketing/social.yaml](../../content/marketing/social.yaml): fonte unica dei link
  social brand, consegnata **vuota/commentata**. Comportamento: vuoto → niente sezione; compilato →
  letto e mostrato dal footer senza toccare codice.
- **Nuovo** [site/src/lib/social.ts](../../site/src/lib/social.ts): legge e valida lo YAML con
  `js-yaml` (stesso pattern di `legal.ts`), espone `SOCIAL_LINKS` e la funzione pura
  `parseSocialLinks(raw)`. File/lista vuota → array vuoto; voce malformata (label vuota o `href` non
  https assoluto) → **errore bloccante** del build.
- [site/src/content/marketing/types.ts](../../site/src/content/marketing/types.ts): `FooterContent`
  guadagna `socialHeading`. Tradotto nelle **5 lingue** (it "Seguici", en "Follow us", fr
  "Suivez-nous", es "Síguenos", de "Folge uns").
- [site/src/layouts/BaseLayout.astro](../../site/src/layouts/BaseLayout.astro): importa `SOCIAL_LINKS`
  e rende una `<nav>` social nel footer **solo se** non vuoto (link `rel="me noopener"`,
  `target="_blank"`).

### 2. Runbook di lancio (R2)
- **Nuovo** [docs/_LANCIO.md](../../docs/_LANCIO.md): registro vivo che codifica postura cookieless
  difesa, lancio a due livelli, canali off-site/directory, social organico brand, paid (Google Search
  prima, Meta poi), misura UTM+Plausible+referral AI, e la **checklist delle azioni founder** (le
  escalation). Puntatore aggiunto in [CLAUDE.md](../../CLAUDE.md) accanto agli altri registri vivi.

### 3. Chiusura punto differito
- [docs/usecases/09-marketing-site/0043-lancio-paid-social.md](../../docs/usecases/09-marketing-site/0043-lancio-paid-social.md):
  il punto differito "link social nel footer" (aperto dalla change 0047) è marcato **chiuso**, con i
  percorsi dell'implementazione. Creazione account e riempimento file restano azioni founder.
- [docs/usecases/_INDEX.md](../../docs/usecases/_INDEX.md): UC 0043 → ✅.

## Test
- **Nuovo** [site/src/lib/social.test.ts](../../site/src/lib/social.test.ts): 8 test sul contratto —
  lista vuota/assente → nessun link; voci valide lette in ordine; trim di label/href; lancio su label
  mancante, su `href` non https assoluto e su `links` non-lista; `SOCIAL_LINKS` consegnato rispetta il
  contratto. Verde.
- Suite `site` (`./run-tests.sh site`): **verde** — vitest + `astro build` + controllo post-build (67
  pagine, parità 5 lingue).
- Verifica end-to-end manuale del comportamento richiesto: con `social.yaml` compilato il link e
  l'intestazione compaiono nell'HTML costruito di ogni pagina; ripristinato vuoto → 0 occorrenze.

## Fuori scope / escalation
Esecuzione reale del lancio: creare account brand (LinkedIn/X), submit Product Hunt, iscrivere
directory, avviare campagne paid e budget → **founder**, tracciato in `docs/_LANCIO.md`. La guida
alle campagne è la skill `campaign-guide` (UC 0050). Nessun punto differito nuovo aperto.
