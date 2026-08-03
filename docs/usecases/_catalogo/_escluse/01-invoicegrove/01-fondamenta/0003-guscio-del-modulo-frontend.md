# 0003 — Guscio del modulo frontend

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo di una micro-impresa
> voglio trovare InvoiceGrove nella barra laterale del backoffice quando la mia azienda ci ha l'abbonamento
> così da entrarci senza cercare un altro indirizzo, un'altra password o un'altra applicazione.

**Contesto.** Oggi l'app esiste solo lato servizio. Il guscio del modulo è ciò che la rende visibile: un manifesto
che dichiara identificativo, nome, icona, colore e sezioni, la registrazione nel registro delle app, e le
traduzioni nelle cinque lingue. Va fatta prima delle schermate di dominio perché ogni schermata successiva si
appoggia a questo scheletro; farla dopo significherebbe rifare l'instradamento due volte.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/einvoicing/` con un `manifest.ts` che
   dichiara `id`, `name`, `icon`, `accentToken`, `sections[]`, `resources`, `quota` e `component`.
2. **RF-2** — Il modulo è aggiunto all'elenco `MODULES` del registro delle app e compare nella barra laterale solo
   quando **registro ∩ abilitazione** dicono di sì.
3. **RF-3** — Le sezioni dichiarate sono: Panoramica, Documenti, Controparti, Archivio, Impostazioni. Tutte
   presenti nella navigazione; il contenuto di ciascuna arriva nelle epiche successive.
4. **RF-4** — La schermata di panoramica mostra lo stato «non c'è ancora nulla qui» con una spiegazione di cosa
   fare, non una pagina bianca.
5. **RF-5** — Tutte le stringhe visibili passano dallo spazio-nomi `einvoicing` e sono presenti in `en`, `it`,
   `fr`, `es`, `de`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il modulo **non** conosce il `tenant_id` se non attraverso il contesto
  che la shell gli passa, e non gestisce l'autenticazione.
- **RT-2 — Interfaccia di programmazione (§2).** Il modulo legge i dati esclusivamente con il client generato
  dalla definizione OpenAPI del servizio; nessuna chiamata scritta a mano.
- **RT-3 — Persistenza (§8).** Nessuna migrazione.
- **RT-4 — Modulo frontend (§3, §5).** React + TypeScript + Vite, modulo caricato su richiesta. Interfaccia con
  Tailwind e componenti senza stile proprio sopra i token del sistema di design; stato del server con TanStack
  Query, stato locale con Zustand. `accentToken` = **`amber`**, che deve coincidere con `category` nel listino.
  Nessun colore scritto a mano; funziona in tema chiaro e in tema scuro.
- **RT-5 — Cinque lingue (§4).** Traduzioni in
  `frontend/apps/backoffice/src/modules/einvoicing/i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi `einvoicing`.
  **Nessun testo visibile scritto a mano nei componenti.** La storia non è conclusa se manca una lingua.
- **RT-6 — Varchi e quota (§6, §7).** Il modulo appare solo con l'abilitazione; il consumo di quota è della storia
  `0004`. Il manifesto dichiara la metrica `documenti` così che la shell sappia cosa mostrare.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il guscio non introduce campi.
- **RT-9 — Registrazione eventi (§14).** Nessun evento di servizio nuovo.
- **RT-10 — Avvio locale (§15).** Il modulo è registrato nel registro delle app e abilitato nello stub locale,
  finché l'abilitazione reale non esiste.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare a chi ha l'abbonamento**
- **Dato** un account abilitato all'app `einvoicing`
- **Quando** l'utente entra nel backoffice
- **Allora** vede «InvoiceGrove» nella barra laterale con il colore `amber` e le cinque sezioni dichiarate

**CA-2 — Il modulo non compare a chi non ce l'ha**
- **Dato** un account **non** abilitato
- **Quando** l'utente entra nel backoffice
- **Allora** la voce non compare, e la navigazione diretta all'indirizzo del modulo non mostra dati

**CA-3 — Cinque lingue**
- **Dato** l'interfaccia impostata su ciascuna delle lingue `en, it, fr, es, de`
- **Quando** si apre ogni sezione del modulo
- **Allora** nessuna stringa appare non tradotta o con la chiave grezza

**CA-4 — Tema chiaro e scuro**
- **Dato** il tema scuro attivo
- **Quando** si apre la panoramica
- **Allora** tutti i colori vengono dai token del sistema di design e nessun testo risulta illeggibile

**CA-5 — Stato vuoto utile**
- **Dato** un account abilitato ma senza alcun documento
- **Quando** si apre la panoramica
- **Allora** compare una spiegazione di cosa fare come primo passo, non una pagina vuota

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (frontend, compreso il controllo dei tipi `tsc --noEmit`);
- [ ] prove di **unità** sui componenti con Vitest e Testing Library, con finto strato di rete;
- [ ] prova di **isolamento fra account**: non applicabile lato modulo — il modulo non parla col database;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-EINVOICING]` nasce con la storia `0030`, proprietaria
      della copertura; qui si verifica solo la comparsa del modulo nella prova di piattaforma esistente;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica, nessun dato personale nuovo;
- [ ] controllo automatico di **accessibilità** sulla schermata di panoramica;
- [ ] **registro delle decisioni** compilato, in particolare sull'elenco delle sezioni e sul perché sono cinque;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | Serve la definizione OpenAPI del servizio da cui si genera il client |

## 7. Fuori ambito

- Il contenuto delle sezioni: arriva con le epiche 02-05. Qui ci sono i contenitori e lo stato vuoto.
- La barra di consumo della quota nella panoramica: storia `0004`.
- Qualunque schermata di inserimento: storia `0013` per l'inserimento manuale.

## 8. Punti aperti

- Il **colore-categoria `amber`** è una proposta della descrizione dell'applicazione §3: se lo sviluppatore ne
  sceglie un altro, va cambiato **in due posti** (`accentToken` nel manifesto e `category` nel listino) o i due
  divergono senza che nulla diventi rosso.
- Il nome dell'icona (`file-check`) va confermato rispetto all'insieme di icone effettivamente disponibile nel
  sistema di design.
