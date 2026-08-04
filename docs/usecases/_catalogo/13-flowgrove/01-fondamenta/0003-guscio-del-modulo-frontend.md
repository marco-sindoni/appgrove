# 0003 — Guscio del modulo frontend

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena attivato FlowGrove
> voglio vedere la voce dell'app nella barra laterale del backoffice e potervi entrare
> così da capire che l'attivazione ha prodotto qualcosa, anche prima che ci sia un progetto dentro.

**Contesto.** Un'app che non compare nella barra laterale non esiste per l'utente. Il modulo è caricato su
richiesta e appare quando **registro delle app ∩ abilitazione** dicono di sì
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §3): questa storia mette in piedi il guscio — manifesto,
sezioni, traduzioni, colore — dentro cui le schermate vere si innesteranno una alla volta.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/progetti/` con il manifesto `manifest.ts` che
   dichiara identificativo, nome, icona, colore d'accento, sezioni, risorse e metrica di quota.
2. **RF-2** — Il modulo è aggiunto all'elenco `MODULES` del registro; la voce compare nella barra laterale solo
   quando l'account è abilitato.
3. **RF-3** — Le sezioni dichiarate sono quelle previste dal disegno: *Panoramica*, *Progetti*, *Le mie attività*,
   *Ore*, *Rapporti*. Le sezioni che non hanno ancora contenuto mostrano uno stato vuoto che dice cosa arriverà,
   non una pagina bianca.
4. **RF-4** — La *Panoramica* mostra il nome dell'app, la sua promessa in una riga e il consumo dei posti sul
   piano attivo.
5. **RF-5** — Tutte le stringhe visibili esistono nelle cinque lingue `en, it, fr, es, de`.
6. **RF-6** — L'interfaccia funziona in tema chiaro e in tema scuro e usa solo i token del sistema di design.

## 3. Requisiti tecnici

- **RT-1 — Modulo frontend (§3).** React + TypeScript + Vite; il modulo non gestisce l'autenticazione e non
  conosce il `tenant_id` se non attraverso il contesto della shell; i dati si leggono con il client generato dalla
  definizione OpenAPI.
- **RT-2 — Sistema di design (§5).** Solo token da `tokens.css`; colore-categoria `violet` dichiarato in
  `accentToken` e **identico** a `category` nel file di listino; nessun colore scritto a mano; vietate le librerie
  con un aspetto proprio marcato.
- **RT-3 — Cinque lingue (§4).** Traduzioni in `modules/progetti/i18n/{en,it,fr,es,de}.ts` sotto lo spazio-nomi
  `progetti`; nessun testo visibile scritto a mano nei componenti; la storia non è conclusa se manca una lingua.
- **RT-4 — Varchi (§6).** Il modulo non compare senza abilitazione; se l'abbonamento è `canceled` le chiamate
  rispondono `402` e la schermata lo spiega invece di mostrare un errore generico.
- **RT-5 — Avvio locale (§15).** Il modulo va registrato nel registro delle app e abilitato nello stub locale
  finché l'abilitazione reale non esiste.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: il guscio non mostra ancora dati.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia.

## 4. Criteri di accettazione

**CA-1 — L'app compare**
- **Dato** un account abilitato a FlowGrove
- **Quando** l'utente entra nel backoffice
- **Allora** sotto «Le tue app» compare *FlowGrove* con il colore `violet` e le cinque sezioni previste

**CA-2 — L'app non compare senza abilitazione**
- **Dato** un account non abilitato
- **Quando** l'utente entra nel backoffice
- **Allora** la voce non c'è, e l'accesso diretto alla rotta del modulo porta a un messaggio che spiega come
  attivare l'app

**CA-3 — Cinque lingue**
- **Dato** l'interfaccia in ciascuna delle lingue `en, it, fr, es, de`
- **Quando** si visitano tutte le sezioni
- **Allora** non compare nessuna chiave di traduzione grezza e nessun testo in lingua sbagliata

**CA-4 — Tema chiaro e scuro**
- **Dato** il tema scuro attivo
- **Quando** si visita la *Panoramica*
- **Allora** il contrasto è quello del sistema di design e nessun elemento resta illeggibile

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (area `frontend`, compreso il controllo dei tipi `tsc --noEmit`);
- [ ] prove di **unità** sul manifesto e sulla comparsa condizionata all'abilitazione;
- [ ] prova di **isolamento fra account**: non applicabile lato modulo (nessuna lettura di dati);
- [ ] **prova end-to-end**: rimando — il primo percorso `[J-PROGETTI]` nasce con la storia 0031, quando c'è
      qualcosa da percorrere;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotate le sezioni scelte e perché;
- [ ] controllo automatico di **accessibilità** verde sulla *Panoramica*;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Serve la definizione OpenAPI da cui si genera il client |
| Colore-categoria confermato (§3 della descrizione) | `accentToken` e `category` devono coincidere: se il colore cambia, cambiano due file |

## 7. Fuori ambito

- le schermate con dati veri: sono delle epiche 02-05;
- il consumo dei posti calcolato davvero: la logica è della storia 0004, qui la *Panoramica* mostra il dato che
  l'abilitazione le passa.

## 8. Punti aperti

- Il colore `violet` è proposto anche da 06 QuoteGrove
  ([application-description.md](../application-description.md) §11.6): se la piattaforma decide diversamente,
  questa storia cambia una riga del manifesto e una del listino, non di più — purché restino coerenti.
