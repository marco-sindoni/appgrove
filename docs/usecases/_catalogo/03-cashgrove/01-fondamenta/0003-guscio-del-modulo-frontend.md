# 0003 — Guscio del modulo frontend

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa che ha appena attivato CashGrove
> voglio vedere l'app comparire nella barra laterale del mio spazio di lavoro e potervi entrare
> così da capire subito che l'abbonamento ha prodotto qualcosa di visibile.

**Contesto.** Oggi il servizio risponde ma nessuno lo può usare: non c'è nulla nel backoffice. Questa storia crea il
guscio del modulo — manifesto, registrazione, sezioni della barra laterale, traduzioni, tema — con le schermate ancora
vuote. Si fa prima delle schermate vere perché il guscio è ciò che rende visibile e collaudabile tutto il resto: senza,
ogni storia successiva dovrebbe reinventare dove mettere la propria pagina.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/crediti/` con il manifesto che dichiara
   identificativo `crediti`, nome visibile, icona, colore d'accento `amber`, sezioni, risorse e metrica di quota.
2. **RF-2** — Il modulo è aggiunto all'elenco dei moduli del registro delle app e compare nella barra laterale **solo**
   quando registro e abilitazione dicono entrambi di sì.
3. **RF-3** — Le sezioni dichiarate sono cinque: *Panoramica*, *Crediti*, *Debitori*, *Solleciti*, *Impostazioni*; in
   questa storia mostrano uno stato vuoto con una riga che spiega cosa arriverà.
4. **RF-4** — La *Panoramica* mostra già l'intestazione dell'app e il consumo della quota `crediti_monitorati` letto dal
   servizio.
5. **RF-5** — Tutti i testi visibili esistono in `en, it, fr, es, de`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il modulo non conosce il `tenant_id` se non attraverso il contesto che la
  shell gli passa, e non lo invia mai come parametro.
- **RT-2 — Interfaccia di programmazione (§2).** I dati si leggono con il client generato dalla definizione OpenAPI del
  servizio `crediti`; nessuna chiamata scritta a mano.
- **RT-3 — Persistenza (§8).** Nessuna modifica al database.
- **RT-4 — Modulo frontend (§3, §5).** Modulo React + TypeScript caricato su richiesta dentro
  `frontend/apps/backoffice/src/modules/crediti/`, registrato in `frontend/apps/backoffice/src/registry/registry.ts`.
  Solo token del sistema di design, componenti senza stile proprio, nessun colore scritto a mano; funziona in tema
  chiaro e scuro. `accentToken` = `amber`, lo stesso valore di `category` nel listino.
- **RT-5 — Cinque lingue (§4).** Traduzioni in `frontend/apps/backoffice/src/modules/crediti/i18n/{en,it,fr,es,de}.ts`
  sotto lo spazio-nomi `crediti`; nessun testo visibile scritto a mano nei componenti. La storia non è conclusa se ne
  manca una.
- **RT-6 — Varchi e quota (§6, §7).** La *Panoramica* mostra il consumo della metrica `crediti_monitorati` (natura
  `stock`) così come lo pubblica l'abilitazione; se l'abbonamento non dà accesso, il modulo non compare affatto — non
  compare disabilitato.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: il guscio non espone funzioni.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il guscio non mostra ancora dati di debitori.
- **RT-9 — Registrazione eventi (§14).** Nessun evento applicativo nuovo lato servizio.
- **RT-10 — Accessibilità (§11).** Controllo automatico di accessibilità sulle schermate introdotte, anche se vuote:
  intestazioni, punti di riferimento e ordine di lettura corretti fin dall'inizio.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare**
- **Dato** un account abilitato all'app `crediti`
- **Quando** l'utente apre il backoffice
- **Allora** nella barra laterale, sotto «Le tue app», compare CashGrove con le sue cinque sezioni

**CA-2 — Il modulo non compare a chi non è abilitato**
- **Dato** un account senza abbonamento a `crediti`
- **Quando** l'utente apre il backoffice
- **Allora** l'app non compare in nessuna forma, e la navigazione diretta al suo percorso porta alla pagina «non
  disponibile» della shell

**CA-3 — Cinque lingue**
- **Dato** l'interfaccia in una qualsiasi fra `en, it, fr, es, de`
- **Quando** si aprono le cinque sezioni
- **Allora** nessuna stringa compare nella lingua sbagliata e nessuna chiave resta non tradotta

**CA-4 — Tema chiaro e scuro**
- **Dato** il tema scuro attivo · **Quando** si apre la *Panoramica* · **Allora** i colori vengono dai token, il
  contrasto è sufficiente e nessun elemento resta illeggibile

**CA-5 — Consumo della quota visibile**
- **Dato** un account sul piano con tetto 150 e 12 crediti monitorati
- **Quando** si apre la *Panoramica*
- **Allora** compare «12 di 150 crediti monitorati», letta dal servizio e non calcolata nel browser

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh frontend` (compreso il controllo dei tipi `tsc --noEmit`);
- [ ] prove di **unità** sui componenti del guscio con strato di rete finto;
- [ ] prova di **isolamento fra account**: non applicabile lato frontend — il modulo non riceve né invia `tenant_id`;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-CREDITI]` nasce con la storia `0031`, quando esiste un flusso
      completo da percorrere;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica, dichiarato esplicitamente;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta delle cinque sezioni;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta;
- [ ] il modulo è abilitato nello stub locale dell'abilitazione, così l'app si vede subito dopo l'unione del ramo.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | Servono il servizio e la definizione OpenAPI da cui si genera il client |

## 7. Fuori ambito

- Le schermate vere: arrivano una per una con le storie che le riempiono (`0006` in poi).
- Il blocco a quota esaurita nell'interfaccia: storia `0004`.

## 8. Punti aperti

Nessuno.
