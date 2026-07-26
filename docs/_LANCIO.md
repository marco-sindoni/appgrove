# Runbook di lancio — paid/social/directory (UC 0043)

Registro vivo del **lancio lean a due livelli** di appgrove: cosa si fa, in che ordine, con quali
canali, rispettando la postura privacy. Fonte delle decisioni: [docs/14-sito-vetrina-legale.md](14-sito-vetrina-legale.md)
sezione J (paid/social/launch), punti J45–J50 e I41. Use case: [docs/usecases/09-marketing-site/0043-lancio-paid-social.md](usecases/09-marketing-site/0043-lancio-paid-social.md).

> **Natura del documento.** Il lancio è un'attività **operativa** del founder: creare account,
> pubblicare, avviare campagne con budget reale. Questo runbook la **codifica** (piano, canali,
> checklist di conformità); non la esegue. Ogni campagna passa **obbligatoriamente** dalla skill
> [`campaign-guide`](../.claude/skills/campaign-guide/SKILL.md) (UC 0050), che applica la checklist
> di conformità a ogni passo e genera le convenzioni di tracciamento e i testi.

## 1. Postura privacy — la difesa cookieless (non negoziabile)

Il lancio non incrina il pilastro privacy del progetto (memoria `eu-data-residency-purist`):

- **niente pixel** Meta/Google sul sito, **niente invio di conversioni server-a-server con dati
  personali** (richiederebbero consenso e banner → contraddirebbero la postura). Le inserzioni si
  fanno **comunque senza pixel**;
- obiettivi ammessi: **Traffico** e **moduli di contatto nativi** della piattaforma (i contatti
  restano sulla piattaforma, zero tracciamento sul sito);
- **attribuzione senza cookie**: parametri UTM sui link + traguardi (goal) di Plausible + i clic
  misurati dalle piattaforme. Trade-off accettato: ottimizzazione e attribuzione più deboli in
  cambio di coerenza di brand;
- i contatti raccolti confluiscono nella **newsletter** solo con consenso esplicito (doppia conferma,
  UC 0039). La raccolta segue il trattamento newsletter già mappato nella RoPA (#13 F).

## 2. Lancio a due livelli (J45)

1. **Per-app**: nella community della sua clientela di riferimento (la ICP dell'app).
2. **Brand**: sotto il marchio appgrove (marketplace all-EU, GDPR-first).

## 3. Canali off-site / directory (J45, I41) — corroborazione + visibilità AI

Presenza coerente su più fonti = più citabilità e fiducia (anche per gli assistenti AI, UC 0041).
La descrizione va tenuta **identica** all'entità canonica ([site/src/lib/brand.ts](../site/src/lib/brand.ts)).

- **Product Hunt** (lancio);
- **directory/review**: AlternativeTo, G2, Capterra, SaaSHub;
- **community indie/dev**: Indie Hackers, subreddit pertinenti, eventuale Hacker News.

## 4. Social organico — pochi canali, account BRAND (J46)

- **LinkedIn** (pagina brand, clientela professionale/piccole-medie imprese UE) → **primario**;
- **X** → opzionale (nicchia indie/dev);
- **NON** account personali, **no** racconto "costruito in pubblico" (coerente con la scelta di non
  usare la storia del founder);
- i contenuti sono **riusati dal blog** (UC 0042) e generati con l'assistente on-brand (tono F1, dec. 35).

### Link social nel footer del sito (cablati, letti da configurazione)
Il footer del sito mostra i link social **da solo** leggendoli da **[content/marketing/social.yaml](../content/marketing/social.yaml)**:
finché il file è vuoto non compare nulla; appena si compila una voce, il footer la legge e la mostra
senza modifiche di codice. Alla creazione di ogni account brand → aggiungere lì la voce
`label`/`href` (URL https assoluto). Questo chiude il punto differito aperto dalla change `0047`.

## 5. Paid — validare prima di monetizzare (J47)

- **Google Search primario**: intercetta l'intento alto (chi cerca già la soluzione);
- **Meta dopo**: scoperta e ripescaggio, in seconda battuta;
- **budget piccoli di validazione** (coerente col principio costo-minimo): si valida, poi si scala.

## 6. Misura (J49, dec. 43)

UTM coerenti (li produce `campaign-guide`) + traguardi Plausible + clic delle piattaforme +
**referral dai motori AI** via Plausible (referrer chatgpt.com/perplexity.ai/…) e verifiche manuali
periodiche (interrogare gli assistenti e controllare le citazioni).

## 7. Azioni founder (checklist operativa) — NON eseguite dall'agente

Queste azioni comportano **denaro** o effetti **verso l'esterno/irreversibili**: le esegue il
founder, non l'autopilot. Ognuna va fatta passando dalla skill `campaign-guide` dove pertinente.

- [ ] Creare la **pagina brand LinkedIn** → aggiungere la voce in `content/marketing/social.yaml`.
- [ ] (Opzionale) Creare l'account **X** brand → aggiungere la voce in `content/marketing/social.yaml`.
- [ ] Preparare e **pubblicare il lancio su Product Hunt**.
- [ ] Iscrivere le **directory** (AlternativeTo, G2, Capterra, SaaSHub) con descrizione = entità canonica.
- [ ] Presentarsi nelle **community indie/dev** pertinenti.
- [ ] Impostare le **campagne Google Search** (obiettivo Traffico, no pixel) via `campaign-guide`, budget piccolo.
- [ ] In seconda battuta, campagne **Meta** (Traffico / modulo contatti nativo) via `campaign-guide`.
- [ ] Verificare l'**attribuzione**: UTM coerenti, traguardi Plausible attivi, referral AI monitorati.

## 8. Riferimenti

- Decisioni: #14 J45/J46/J47/J48/J49, I41 · UC 0043 (DoD).
- Skill: `campaign-guide` (UC 0050, guida le campagne), `finalize-landing` (UC 0057, landing pronte).
- Dipendenze: sito/homepage/landing (UC 0036/0037/0053), newsletter+Plausible (UC 0039), GEO (UC 0041).
