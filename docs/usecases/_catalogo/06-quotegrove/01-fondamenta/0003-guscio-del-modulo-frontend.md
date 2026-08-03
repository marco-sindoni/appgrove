# 0003 — Guscio del modulo frontend

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena attivato QuoteGrove
> voglio vedere l'app nella barra laterale del backoffice e potermici muovere dentro
> così da capire subito che l'ho comprata e che funziona, anche prima di avere dati.

**Contesto.** Il servizio risponde, ma nessuno lo vede. Questa storia crea il modulo caricato su richiesta dentro
il backoffice, lo registra e disegna il guscio delle sue sezioni con lo stato vuoto. Si fa adesso perché ogni
storia successiva aggiunge una schermata a questo guscio: se il guscio non c'è, ogni storia se lo porta dietro.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/preventivi/` con il suo `manifest.ts` che
   dichiara identificativo, nome, icona, `accentToken`, sezioni, risorse e metrica di quota, ed è aggiunto
   all'elenco `MODULES` del registro.
2. **RF-2** — La barra laterale mostra QuoteGrove quando registro e abilitazione dicono entrambi di sì, e non lo
   mostra altrimenti.
3. **RF-3** — Le sezioni previste sono: **Panoramica**, **Preventivi**, **Catalogo e listini**, **Modelli**,
   **Impostazioni**; in questa storia esistono tutte con il proprio stato vuoto.
4. **RF-4** — Lo stato vuoto della sezione Preventivi dice cosa fare («crea il primo preventivo»), non solo che
   non c'è niente.
5. **RF-5** — L'elenco dei preventivi legge dalla risorsa della storia `0002` con il client generato dalla
   definizione OpenAPI.

## 3. Requisiti tecnici

- **RT-1 — Modulo frontend (§3).** React + TypeScript + Vite, modulo caricato su richiesta; non gestisce
  l'autenticazione e non conosce il `tenant_id` se non tramite il contesto che la shell gli passa; stato del
  server con TanStack Query.
- **RT-2 — Sistema di design (§5).** Solo token del sistema di design; `accentToken: 'cat-violet'`, che deve
  coincidere con `category: violet` del listino; funziona in tema chiaro e in tema scuro; nessun colore scritto a
  mano; vietate le librerie con un aspetto proprio marcato.
- **RT-3 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `preventivi` e sono presenti
  in `en, it, fr, es, de`, nei file `modules/preventivi/i18n/{en,it,fr,es,de}.ts`. Nessun testo scritto a mano nei
  componenti: la storia non è conclusa se manca una lingua.
- **RT-4 — Isolamento fra account (§1).** Il modulo non passa mai un identificativo di account: lo deduce il
  servizio dal token.
- **RT-5 — Varchi (§6).** Se l'account non è abilitato la shell non mostra il modulo; se l'abilitazione decade
  mentre l'utente è dentro, la schermata mostra il messaggio di piattaforma e non un errore tecnico.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-7 — Prove (§11).** Vitest + Testing Library con strato di rete finto; controllo dei tipi `tsc --noEmit`;
  controllo automatico di accessibilità sulle schermate introdotte.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare a chi è abilitato**
- **Dato** un account abilitato a `preventivi` · **Quando** l'utente apre il backoffice · **Allora** vede
  QuoteGrove nel gruppo delle sue app, con il colore `violet`, e può aprirne le cinque sezioni

**CA-2 — Il modulo non compare a chi non è abilitato**
- **Dato** un account senza abbonamento all'app · **Quando** l'utente apre il backoffice · **Allora** QuoteGrove
  non è nella barra laterale e l'indirizzo diretto della sezione non mostra dati

**CA-3 — Stato vuoto utile**
- **Dato** un account abilitato e nessun preventivo · **Quando** apre la sezione Preventivi · **Allora** legge un
  testo che spiega cosa fare e un pulsante per cominciare

**CA-4 — Cinque lingue e due temi**
- **Dato** l'interfaccia in ciascuna delle cinque lingue · **Quando** si aprono le sezioni · **Allora** nessuna
  stringa resta non tradotta, e la resa è corretta sia in tema chiaro sia in tema scuro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sui componenti e controllo dei tipi `tsc --noEmit`;
- [ ] prova di **isolamento fra account**: non applicabile al frontend, coperta lato servizio da `0002`;
- [ ] **prova end-to-end**: rimando alla storia `0029`, che crea il percorso `[J-PREVENTIVI]` quando ci sarà
      qualcosa da fare oltre a guardare uno stato vuoto; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato (elenco delle sezioni, colore, forma dello stato vuoto);
- [ ] contratto degli **strumenti conversazionali**: nessuno;
- [ ] avvio locale invariato;
- [ ] modulo abilitato nello stub locale finché l'abilitazione reale non è disponibile.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | l'elenco deve leggere da qualche parte |

## 7. Fuori ambito

- il modulo di creazione del preventivo: storia `0012`;
- la pagina pubblica del destinatario, che **non** vive nel backoffice: storia `0018`.

## 8. Punti aperti

Nessuno.
