# Revisione legale pre-go-live (documento vivo)

Registro **unico** dei punti che conviene far rivedere da un **avvocato/DPO prima del go-live** (quando entrano utenti
reali e responsabilità reale). Tutte le decisioni e i testi sono prodotti **internamente** in fase di sviluppo; questa è
una **revisione finale mirata**, non un'attività di drafting da zero.

> **Natura**: **consigliata, opzionale**. È una **scelta di rischio dell'utente**: può essere ridotta o saltata.
> **Nessun blocco** prima del go-live (fase locale/test = nessun utente reale). Il valore di un legale = firma con
> responsabilità professionale, aggiornamento normativo/giurisprudenziale locale, casi-limite specifici del business.

Aggiornato a ogni decisione che tocca aspetti legali.

## Checklist

| # | Punto da rivedere | Origine | Quando | Stato |
|---|---|---|---|---|
| L1 | **Testo del DPA** (Data Processing Agreement) incorporato nei termini, verso i clienti professionali/titolari | #13 A/H | pre-go-live | ⏳ bozza interna |
| L2 | **Privacy Policy** pubblica (minimizzata; **5 lingue EN/IT/FR/ES/DE**, **IT facente fede**, FR/ES/DE traduzioni fedeli) | #13 G | pre-go-live | ⏳ da redigere |
| L3 | **Terms & Conditions** del servizio (Paddle MoR; 5 lingue, IT facente fede) | #13 G / #09 | pre-go-live | ⏳ da redigere |
| L4 | **Dicitura esatta titolare/responsabile sui contenuti** (postura uniforme A; household exemption) | #13 A | pre-go-live | ⏳ bozza interna |
| L5 | **Gestione categorie particolari (art. 9)** — se/quando una app le introduce: base rafforzata + DPIA | #13 C/K | all'introduzione | n/a finché non usate |
| L6 | **Validità modello "cancellazione immediata post-export"** e periodi di retention scelti | #13 D/E | pre-go-live | ⏳ deciso internamente |
| L7 | **Postura consenso/cookie** — da rivedere SOLO se si introduce tracking non essenziale (oggi: analytics cookieless, no banner) | #13 B/F | se cambia il tracking | ok attuale (no consenso) |
| L8 | **Eventuali obblighi settoriali** di singole app (dati regolati) | per-app | alla creazione app | per-app |
| L9 | **Lista sub-responsabili & DPA con i sub-processor** (AWS, Paddle, SES, Cloudflare, Atlassian/Jira) | #13 H | pre-go-live | ⏳ da compilare |
| L10 | **Accessibilità** — dichiarazione/conformità European Accessibility Act (in vigore da giugno 2025) | #10 K | pre-go-live | ⏳ |
| L11 | **Entità legale titolare** (ditta/società) con indirizzo/contatto — richiesta da privacy policy e da Paddle (MoR) | #13 G | pre-go-live | ⏳ prerequisito business |

## Note
- I documenti **interni** (RoPA, manifesti-dati per-app) NON sono pubblici e non rientrano nella revisione "pubblica":
  servono per accountability verso il Garante (#13 C).
- Aggiungere qui ogni nuovo punto legale che emerge dalle decisioni o dal **gate privacy/RoPA** di `new-change` (#13 C).
