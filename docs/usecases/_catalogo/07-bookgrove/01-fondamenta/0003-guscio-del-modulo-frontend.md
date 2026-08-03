# 0003 — Guscio del modulo frontend

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena attivato BookGrove
> voglio vedere l'app comparire nella barra laterale del mio backoffice, con le sue sezioni
> così da capire che l'ho attivata davvero e da dove si comincia.

**Contesto.** Il modulo frontend è un guscio: nessuna funzione, ma la struttura che tutte le schermate successive
riempiranno. Farlo adesso costa poco e stabilisce le due cose che poi diventano care da cambiare — quali sezioni
esistono e come si chiamano nelle cinque lingue.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/prenotazioni/` con il suo manifesto, ed è
   registrato nell'elenco dei moduli.
2. **RF-2** — Il manifesto dichiara le sezioni previste: **Agenda**, **Prenotazioni**, **Clienti**, **Servizi e
   risorse**, **Impostazioni**; le sezioni non ancora implementate mostrano uno stato vuoto onesto, non una
   schermata rotta.
3. **RF-3** — Il modulo compare nella barra laterale solo quando registro e abilitazione dicono entrambi di sì.
4. **RF-4** — Tutti i testi visibili passano dallo spazio-nomi `prenotazioni` e sono presenti in `en, it, fr, es,
   de`.
5. **RF-5** — Il colore-categoria del modulo (`accentToken`) è `green` e coincide con quello del listino.

## 3. Requisiti tecnici

- **RT-1 — Modulo frontend (§3).** React con TypeScript, caricato su richiesta, manifesto con
  `{ id, name, icon, accentToken, sections[], resources, quota, component }`; il modulo non gestisce
  l'autenticazione e non conosce il `tenant_id` se non dal contesto della shell; i dati si leggono con il client
  generato dalla definizione OpenAPI.
- **RT-2 — Sistema di design (§5).** Solo token del sistema di design; nessun colore scritto a mano; funziona in
  tema chiaro e scuro; niente librerie con aspetto proprio marcato.
- **RT-3 — Cinque lingue (§4).** Traduzioni accanto al modulo, in
  `modules/prenotazioni/i18n/{en,it,fr,es,de}.ts`; nessun testo visibile scritto a mano nei componenti; la storia
  non è conclusa se ne manca una.
- **RT-4 — Avvio locale (§15).** Il modulo è registrato nel registro delle app e abilitato nello stub locale
  finché l'abilitazione reale non esiste.
- **RT-5 — Dati personali (§10).** Nessun dato personale nuovo: il guscio non legge né scrive dati di persone.
- **RT-6 — Prove (§11).** Vitest con Testing Library e strato di rete finto; controllo dei tipi `tsc --noEmit`;
  controllo automatico di accessibilità sulla schermata principale.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare**
- **Dato** un account abilitato a `prenotazioni` · **Quando** l'utente apre il backoffice · **Allora** vede
  BookGrove nella barra laterale con l'icona e il colore verde, e le cinque sezioni

**CA-2 — Il modulo non compare a chi non è abilitato**
- **Dato** un account senza abilitazione · **Quando** apre il backoffice · **Allora** non vede il modulo, e
  raggiungendo l'indirizzo a mano ottiene la schermata di app non attiva

**CA-3 — Cinque lingue**
- **Dato** l'interfaccia · **Quando** si cambia lingua fra `en, it, fr, es, de` · **Allora** nessun testo resta
  nella lingua sbagliata e nessuna chiave di traduzione compare a schermo

**CA-4 — Due temi**
- **Dato** il modulo · **Quando** si passa da tema chiaro a tema scuro · **Allora** tutto resta leggibile e
  nessun colore è fuori dai token

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh frontend`, compreso il controllo dei tipi;
- [ ] prove di **unità** sulla registrazione del modulo e sulla resa delle sezioni vuote;
- [ ] prova di **isolamento fra account**: non applicabile, il guscio non legge dati — dichiarato nel registro
      delle decisioni;
- [ ] **prova end-to-end**: *rimando* — la copre la storia `0033`, con il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica, nessun dato personale;
- [ ] **registro delle decisioni** compilato: elenco e nomi delle sezioni, colore-categoria;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve la definizione OpenAPI da cui si genera il client |

## 7. Fuori ambito

- il contenuto delle sezioni: lo riempiono le epiche 02, 03 e 05;
- la pagina pubblica: non vive dentro il backoffice ed è della storia `0016`.

## 8. Punti aperti

Nessuno.
