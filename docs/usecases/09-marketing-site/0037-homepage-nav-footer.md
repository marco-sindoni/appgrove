# UC 0037 — Homepage + nav/footer + "Perché appgrove / Privacy & EU"

**Area**: 09-marketing-site · **Fase**: 3 · **Stato**: 🟢 deciso (contenuti AI-generati, review utente)
**Dipendenze**: UC [0036](0036-vetrina-astro-scheletro.md) (skeleton sito)
**Fonte decisioni**: #14 E (posizionamento), #14 G (IA/contenuti), #14 F (brand)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md)

## 1. Obiettivo / Scope
Creare le pagine **brand** del sito: homepage, navigazione/footer e la pagina **"Perché appgrove / Privacy & EU"** (storia del
wedge, forte per GEO/PR).
**Incluso**: **homepage** (hero "strumenti semplici che crescono con te" → vetrina app anche 1 "altri in arrivo" → "un account,
tanti strumenti" cross-sell → sezione privacy/EU wedge → newsletter → CTA); **top nav** (App · Perché appgrove · Prezzi · Blog ·
Login + CTA Registrati); **footer** (legali · Support · `security.txt` · newsletter · selettore lingua · social); pagina
**"Perché appgrove"** (mission/valori **senza founder story**); pagina **"Prezzi — come funziona la fatturazione"** (mensile/annuale,
trial, no-refund). Tutti i contenuti **AI-generati on-brand** (tono lean), 5 lingue.
**Escluso**: landing per-app (UC 0038), newsletter backend (UC 0039), SEO tecnico (UC 0040), GEO (UC 0041), blog (UC 0042).

## 2. Attori & ruoli
- **Visitatore/ICP** (micro-business EU): legge e converte (CTA/registrati/newsletter).
- **AI (Claude)**: genera copy + visivi on-brand; **utente rivede e approva** (#14 35).

## 3. Precondizioni
- Skeleton sito (UC 0036); brand kit (UC 0019); posizionamento E (job-led + privacy wedge, #14 E7).

## 4. Flusso principale
1. **Homepage** con la sequenza narrativa (#14 26): hero promessa → app (onesta col catalogo piccolo, #14 11) → cross-sell → privacy/EU → newsletter → CTA.
2. **Gerarchia messaggio** (a) **job-led + privacy come wedge** (firma di fiducia, non headline) (#14 E7); strati value prop job→semplicità→prezzo→EU-privacy→marketplace (#14 E8).
3. **Nav/footer** come da #14 27, con selettore lingua e legali (UC 0002).
4. **"Perché appgrove"**: wedge EU/privacy, mission/valori **senza narrativa personale** (#14 G24).
5. **"Prezzi"**: come funziona la fatturazione (mensile/annuale default annuale, trial 14gg, no-refund); il prezzo vero sta sulle landing app (#14 G24).
6. Contenuti **AI-generati on-brand** nelle 5 lingue (#14 35), review utente.

## 5. Flussi alternativi / edge / errori
- **Catalogo con una sola app**: homepage onesta "altri strumenti in arrivo", non finge un catalogo pieno (#14 11/26).
- **Lingua mancante** → check CI 5 lingue (UC 0036).
- **Privacy come headline**: evitato (converte poco sugli SMB); privacy = wedge/firma (#14 E7).

## 6. Schermate & stati
Homepage (hero/sezioni), pagina "Perché appgrove", pagina "Prezzi", nav/footer responsive. Stile **screenshot-first** +
Material Symbols + illustrazioni AI minimali (#14 F3). Light/dark dal brand kit. CTA verso registrazione/newsletter.

## 7. Dati toccati
Contenuti statici, nessun dato personale (la cattura email è la newsletter, UC 0039). Manifest: N/A (la sezione privacy/EU
**descrive** la postura, non raccoglie dati). Coerenza con i legali (UC 0002).

## 8. Permessi & gate
- **Invarianti**: N/A (sito pubblico). Gate `published` (UC 0036). Nessun tracking comportamentale (solo Plausible cookieless, UC 0039).

## 9. Requisiti di test
- **Check CI**: 5 lingue, link non rotti (incluso menu/footer ai legali), `published`.
- **a11y/perf**: pagine statiche performanti, accessibili; OG/meta in UC 0040.
- Coerenza messaggi col posizionamento E (job-led + wedge).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 E5/E6/E7/E8, G24/26/27, F1/F3, 11, 35.
- **DoD**:
  1. Homepage + "Perché appgrove" + "Prezzi" + nav/footer, 5 lingue, AI-generati on-brand (review utente).
  2. Posizionamento job-led + privacy wedge; cross-sell "un account, tanti strumenti".
  3. Nessuna founder story; catalogo onesto anche con 1 app.
  4. Check CI 5 lingue + link verdi; pagine `published`.
