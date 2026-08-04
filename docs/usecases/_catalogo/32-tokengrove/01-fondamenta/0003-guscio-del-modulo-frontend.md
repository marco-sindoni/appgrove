# 0003 — Guscio del modulo frontend

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un account abilitato a TokenGrove
> voglio vedere l'app nella barra laterale del backoffice e aprirla
> così da capire che cosa ho comprato, anche prima di aver collegato qualunque cosa.

**Contesto.** Il modulo frontend è ciò che rende l'app una cosa che esiste per chi paga. Qui nasce vuoto: la
schermata di panoramica c'è ma non ha numeri, e proprio per questo deve dire con chiarezza **qual è il primo passo**
— collegare una fonte in sola lettura — perché il valore promesso da questa app è «in cinque minuti vedi la tua
spesa» (§3.1 del documento capofila) e il primo schermo è metà di quei cinque minuti.

## 2. Requisiti funzionali

1. **RF-1** — Il modulo `spesa_modelli` è registrato nell'elenco dei moduli del backoffice con il suo manifesto:
   identificativo, nome, icona, colore-categoria `teal`, sezioni, risorse e metrica di quota.
2. **RF-2** — La voce compare nella barra laterale **solo** quando registro e abilitazione dicono entrambi di sì;
   un account non abilitato non la vede affatto.
3. **RF-3** — Le sezioni dichiarate sono: Panoramica, Spesa, Attribuzione, Budget, Fonti. Esistono come navigazione
   e come schermate; il contenuto arriva dalle epiche successive.
4. **RF-4** — Il primo avvio, senza fonti collegate, mostra uno stato vuoto con tre parti: che cos'è l'app, quanto
   ci vuole a vedere il primo numero, e un solo pulsante che porta al collegamento della fonte.
5. **RF-5** — Tutte le stringhe visibili sono presenti in `en, it, fr, es, de`; nessun testo scritto a mano dentro
   i componenti.
6. **RF-6** — Le schermate funzionano in tema chiaro e in tema scuro e passano il controllo automatico di
   accessibilità.

## 3. Requisiti tecnici

- **RT-1 — Modulo frontend (§3).** Modulo caricato su richiesta in
  `frontend/apps/backoffice/src/modules/spesa_modelli/`, con `manifest.ts` che dichiara
  `{ id, name, icon, accentToken, sections[], resources, quota, component }` e aggiunta all'elenco dei moduli nel
  registro. Il modulo non gestisce l'autenticazione e non conosce il `tenant_id` se non attraverso il contesto che
  la shell gli passa; i dati si leggono con il client generato dalla definizione delle interfacce.
- **RT-2 — Sistema di design (§5).** Solo i token condivisi: nessun colore scritto a mano. Il colore-categoria
  `teal` del manifesto (`accentToken`) coincide con quello del listino (`category`).
- **RT-3 — Cinque lingue (§4).** Traduzioni accanto al modulo, in
  `frontend/apps/backoffice/src/modules/spesa_modelli/i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi
  `spesa_modelli`. La storia non è conclusa se ne manca una.
- **RT-4 — Interfaccia di programmazione (§2).** La panoramica legge `GET /api/spesa_modelli/v1/riepilogo` con il
  client generato; nessuna chiamata scritta a mano.
- **RT-5 — Varchi (§6).** Se la risposta è `402` (account non abilitato) il modulo non mostra un errore tecnico ma
  la schermata che spiega come attivare l'app; se è `401` la shell gestisce il rientro nell'autenticazione.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-7 — Prove (§11).** Prove di componente con finto strato di rete; controllo dei tipi verde; controllo
  automatico di accessibilità sulle schermate introdotte.

## 4. Criteri di accettazione

**CA-1 — L'app compare a chi è abilitato**
- **Dato** un account abilitato a TokenGrove
- **Quando** apre il backoffice
- **Allora** vede la voce «TokenGrove» nella barra laterale, con il colore verde acqua, e cliccandola arriva alla
  panoramica

**CA-2 — L'app non compare a chi non è abilitato**
- **Dato** un account senza abbonamento a TokenGrove
- **Quando** apre il backoffice
- **Allora** non vede la voce, e un accesso diretto all'indirizzo del modulo mostra la schermata di attivazione,
  non un errore tecnico

**CA-3 — Il primo avvio dice cosa fare**
- **Dato** un account abilitato senza fonti collegate
- **Quando** apre la panoramica
- **Allora** vede lo stato vuoto con la spiegazione, il tempo stimato e **un solo** pulsante, che porta al
  collegamento di una fonte

**CA-4 — Cinque lingue e due temi**
- **Dato** l'interfaccia impostata su una qualunque delle cinque lingue e su uno qualunque dei due temi
- **Quando** si aprono le cinque sezioni del modulo
- **Allora** nessuna stringa compare nella lingua sbagliata o come chiave di traduzione, e nessun elemento risulta
  illeggibile per contrasto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno frontend; l'intera suite prima del commit);
- [ ] prove di **componente** sulle schermate introdotte, con finto strato di rete, e controllo dei tipi verde;
- [ ] prova di **isolamento fra account**: non applicabile al frontend, che non decide l'isolamento; dichiarato;
- [ ] **prova end-to-end**: si **rimanda** alla storia `0034`, che è la proprietaria del percorso
      `[J-SPESA-MODELLI]`; il motivo è che un percorso che si ferma su una schermata vuota non dimostra nulla e
      andrebbe riscritto tre volte prima di essere utile;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica (nessun dato personale nuovo);
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta delle cinque sezioni;
- [ ] controllo automatico di **accessibilità** verde sulle schermate introdotte;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare; il modulo è abilitato nello stub locale
      dell'abilitazione.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Serve la definizione delle interfacce da cui si genera il client |
| Abilitazione reale (piattaforma) | Finché non esiste, il modulo si abilita nello stub locale |

## 7. Fuori ambito

- il contenuto vero delle cinque sezioni: arriva dalle epiche 02-06;
- il modulo di collegamento di una fonte: è della storia `0006`; qui c'è solo il pulsante che ci porta.

## 8. Punti aperti

- **Nome visibile dell'app nell'interfaccia.** «TokenGrove» è il nome commerciale, ma «token» in italiano non dice
  niente a chi non è tecnico, e nella piattaforma la stessa parola indica già il gettone di autenticazione. Il
  sottotitolo proposto è «Spesa per i modelli linguistici», ma il nome commerciale è una decisione di prodotto e la
  chiude lo sviluppatore.
