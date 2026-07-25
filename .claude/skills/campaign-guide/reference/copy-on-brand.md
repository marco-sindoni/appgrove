# Copy e creatività on-brand per le campagne

Tutto il copy delle campagne è **generato con l'intelligenza artificiale** (dec. 35), coerente col **tono di
voce del brand** (F1): **lean, semplice, chiaro**. L'utente non è un copywriter: la skill produce le varianti,
lui sceglie. Fonte: #14 J46 (account brand), J50; dec. 35; F1.

## Principi di tono (F1)
- **Semplice e diretto**: frasi brevi, parole comuni, **niente gergo** non spiegato. Chi legge capisce al primo colpo.
- **Beneficio concreto prima**: cosa ottiene la persona, non l'elenco delle funzioni.
- **On-message con la landing**: il messaggio dell'annuncio e quello della pagina di destinazione coincidono
  (chi clicca ritrova ciò che gli è stato promesso).
- **Account brand, non personale**: voce dell'azienda/prodotto, **niente storia personale o "build-in-public"**
  (#14 J46).
- **Onestà**: nessun claim non sostenibile, nessuna urgenza fasulla, nessuna promessa fuori dalla postura
  (es. non promettere ciò che richiederebbe tracciamento).

## Formati degli annunci (limiti pratici)

**Google Search** (annuncio di ricerca adattivo):
- **Titoli**: più varianti, ciascuna ~**30 caratteri**. Fornisci almeno 5 titoli diversi.
- **Descrizioni**: ~**90 caratteri** ciascuna. Fornisci almeno 2 descrizioni.
- Il sistema combina titoli/descrizioni: scrivili **autonomi** (devono funzionare in qualsiasi combinazione).

**Meta** (inserzione):
- **Testo principale** (primary text): 1–3 frasi, il gancio nelle prime righe.
- **Titolo** (headline): breve, il beneficio.
- **Descrizione**: opzionale, di supporto.
- **Call-to-action**: coerente con l'obiettivo (es. "Scopri di più" per Traffico, "Iscriviti" per Lead Form).

## Modello di prompt per generare le varianti
Quando generi il copy, parti da: **app/offerta**, **pubblico**, **beneficio principale**, **obiettivo ammesso**
(Traffico/Lead Form), **lingua**. Poi:

> Genera copy on-brand (tono lean, semplice, chiaro — F1) per una campagna [piattaforma] con obiettivo
> [Traffico|Lead Form] che promuove [app/offerta] a [pubblico]. Beneficio principale: [beneficio]. Rispetta i
> limiti di caratteri del formato [Google Search|Meta]. Nessun gergo non spiegato, nessun claim non sostenibile,
> nessuna urgenza fasulla. Voce del brand (account aziendale, non personale). Fornisci [N] varianti di titolo e
> [M] di descrizione/testo, autonome e confrontabili (per test A/B via `utm_content`).

## Coerenza con le altre parti
- Le **varianti** che vuoi confrontare vanno mappate su `utm_content` (vedi `convenzioni-utm.md`): un valore
  per variante, così Plausible ti dice quale rende meglio.
- La **destinazione** del copy è una pagina del **sito vetrina** (checklist voce 4): il messaggio deve combaciare
  con quella pagina.
- Le **5 lingue** del sito (EN sorgente, poi IT/FR/ES/DE) mantengono il **tono**, non la traduzione parola-per-parola
  (#14 10): se la campagna è multilingua, genera il copy adattando il tono, non traducendo letteralmente.
