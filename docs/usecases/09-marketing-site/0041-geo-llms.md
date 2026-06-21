# UC 0041 — GEO (`llms.txt`, crawler AI consentiti, entità canonica)

**Area**: 09-marketing-site · **Fase**: 3 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0036](0036-vetrina-astro-scheletro.md) (skeleton sito)
**Fonte decisioni**: #14 I (GEO)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md)

## 1. Obiettivo / Scope
Implementare la **GEO (Generative Engine Optimization)**: farsi citare/raccomandare dagli assistenti AI sfruttando l'angolo
all-EU/GDPR + micro-tool focalizzati.
**Incluso**: **`llms.txt`** (riassunto markdown curato per gli LLM); **consenso crawler AI** in robots.txt (GPTBot, ClaudeBot,
PerplexityBot, Google-Extended, …); **contenuti machine-readable** (Schema.org, HTML semantico, **FAQ Q&A**, tabelle di
confronto, statement fattuali); **entità canonica/boilerplate** (descrizione canonica del prodotto usata identica ovunque);
misurazione referral AI via Plausible + check manuali.
**Escluso**: il SEO tecnico (UC 0040), i contenuti question-based del blog (UC 0042), la presenza off-site/directory (UC 0043).

## 2. Attori & ruoli
- **Assistenti AI/LLM** (ChatGPT, Claude, Perplexity, Gemini, AI Overviews): crawlano contenuti marketing e citano.
- **`new-application`** (UC 0046): genera materiale GEO-friendly per-app (FAQ strutturate, fatti, entità coerente).

## 3. Precondizioni
- Skeleton sito (UC 0036) con HTML semantico; Schema.org (UC 0040); contenuti md fonte unica.

## 4. Flusso principale
1. Pubblicare **`llms.txt`** (riassunto markdown del prodotto/sito, "robots.txt per le AI"), costo ~0 (#14 38).
2. **Consentire i crawler AI** in robots.txt — è lo **scopo** della GEO; viene crawlato **contenuto marketing**, non dati utenti (#14 39).
3. **Contenuti machine-readable**: FAQ Q&A, tabelle di confronto, liste feature, statement fattuali (la fonte md è ideale) (#14 37).
4. **Entità canonica/boilerplate**: descrizione canonica (nome, categoria, fatti chiave, angolo EU/GDPR) usata **identica** ovunque (sito/directory/social) (#14 40).
5. **Misurazione**: referral dai motori AI via Plausible (referrer chatgpt.com/perplexity.ai/…) + check periodici manuali (interrogare gli LLM) (#14 43).

## 5. Flussi alternativi / edge / errori
- **Pre-go-live**: `noindex`/non ancora promosso → i crawler AI vedranno il sito al lancio (coerente UC 0036/0040).
- **Entità incoerente tra canali**: evitata usando il boilerplate canonico unico (#14 40).
- **Postura purista**: solo contenuto marketing crawlato, nessun dato utente (#14 39).

## 6. Risorse & runbook
**Artefatti**: `llms.txt`, blocchi FAQ/confronto nei contenuti, boilerplate canonico (file condiviso). **Runbook**: generato/curato
da noi; `new-application` produce materiale GEO-friendly per-app; misura referral AI in Plausible + check manuali periodici.

## 7. Dati toccati
Solo contenuti marketing pubblici; nessun dato personale. Manifest: N/A.

## 8. Permessi & gate
- **Invarianti**: N/A (sito pubblico). Nessun gate runtime; coerente con `noindex` pre-go-live (i crawler AI agiscono al lancio).

## 9. Requisiti di test
- **Check CI**: presenza `llms.txt`, regole crawler AI in robots.txt, blocchi FAQ/structured presenti sulle pagine chiave.
- **Manuale**: interrogare gli LLM su query target e verificare le citazioni (periodico).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 36/37/38/39/40/41/42/43/44.
- **DoD**:
  1. `llms.txt` pubblicato; crawler AI consentiti in robots.txt.
  2. Contenuti machine-readable (FAQ/confronti/Schema.org) + entità canonica unica.
  3. Misurazione referral AI (Plausible) + check manuali.
  4. `new-application` produce materiale GEO-friendly per-app.
