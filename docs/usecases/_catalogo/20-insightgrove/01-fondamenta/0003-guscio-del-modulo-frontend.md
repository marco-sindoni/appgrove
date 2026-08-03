# 0003 — Guscio del modulo frontend

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena attivato InsightGrove
> voglio vedere la voce dell'app nella barra laterale del backoffice e poterci entrare
> così da avere la prova che l'app è accesa, anche prima che ci sia un solo numero da guardare.

**Contesto.** Il backend esiste (0001) e ha dove mettere i dati (0002), ma il cliente non vede niente. Questa
storia porta il modulo dentro il backoffice: manifesto, registrazione, sezioni della barra laterale, cinque
lingue, tema chiaro e scuro. Non porta contenuto: le sezioni esistono e mostrano lo stato vuoto giusto — «non
hai ancora collegato nessuna fonte» — che è precisamente ciò che l'app deve dire il primo giorno.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/insights/` con il manifesto `manifest.ts`
   che dichiara `id: 'insights'`, nome, icona `gauge`, `accentToken: 'blue'`, le sezioni, le risorse e la quota.
2. **RF-2** — Il modulo è aggiunto all'elenco `MODULES` del registro delle app e compare nella barra laterale
   quando **registro ∩ abilitazione** dicono di sì; finché l'abilitazione reale non esiste, è abilitato nello
   stub locale.
3. **RF-3** — Le sezioni dichiarate sono cinque: **Cruscotto**, **Copilota**, **Metriche**, **Fonti**,
   **Avvisi**. Ogni sezione esiste come pagina raggiungibile e mostra, in assenza di dati, uno stato vuoto con
   titolo, spiegazione e **una azione** — mai un vicolo cieco.
4. **RF-4** — La sezione **Cruscotto** è quella predefinita all'ingresso nell'app.
5. **RF-5** — Tutte le stringhe visibili passano dallo spazio-nomi `insights` e sono presenti in `en, it, fr, es,
   de`.

## 3. Requisiti tecnici

- **RT-4 — Modulo frontend (§3, §5).** Modulo React caricato su richiesta dentro
  `frontend/apps/backoffice/src/modules/insights/`; i dati si leggeranno con il client generato dalla definizione
  OpenAPI; il modulo **non** gestisce l'autenticazione e **non** conosce il `tenant_id` se non attraverso il
  contesto che la shell gli passa. Solo token del sistema di design; nessun colore scritto a mano; funziona in
  tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Traduzioni in
  `frontend/apps/backoffice/src/modules/insights/i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi `insights`;
  **nessun testo visibile scritto a mano nei componenti**; la storia non è conclusa se manca una lingua.
- **RT-6 — Varchi (§6).** Il modulo non compare se l'account non è abilitato; entrando senza abilitazione la
  shell mostra il proprio messaggio, non una schermata rotta dell'app.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la storia non introduce campi.
- **RT-11 — Prove (§11).** Vitest + Testing Library con finto strato di rete sulle cinque sezioni; controllo dei
  tipi `tsc --noEmit`; controllo automatico di accessibilità sulle schermate introdotte.

## 4. Criteri di accettazione

**CA-1 — L'app compare nella barra laterale**
- **Dato** un account abilitato a InsightGrove
- **Quando** l'utente entra nel backoffice
- **Allora** vede la voce «InsightGrove» sotto «Le tue app», con l'accento `blue`, e cliccandola atterra sulla
  sezione Cruscotto

**CA-2 — Lo stato vuoto dice cosa fare**
- **Dato** un account abilitato che non ha collegato nessuna fonte
- **Quando** apre una qualsiasi delle cinque sezioni
- **Allora** vede uno stato vuoto con titolo, spiegazione e un pulsante che porta alla sezione Fonti

**CA-3 — Cinque lingue**
- **Dato** l'interfaccia impostata su una qualsiasi delle cinque lingue
- **Quando** si aprono le cinque sezioni
- **Allora** nessuna stringa compare nella lingua sbagliata o come chiave grezza

**CA-4 — Tema chiaro e scuro**
- **Dato** il backoffice in tema scuro
- **Quando** si aprono le cinque sezioni
- **Allora** testi e sfondi restano leggibili e nessun colore è scritto a mano fuori dai token

**CA-5 — L'app non compare senza abilitazione**
- **Dato** un account **non** abilitato a InsightGrove
- **Quando** l'utente entra nel backoffice
- **Allora** la voce non compare nella barra laterale, e una navigazione diretta all'indirizzo della sezione
  mostra il messaggio di piattaforma, non una pagina rotta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove **frontend** con Vitest e finto strato di rete sulle cinque sezioni, più `tsc --noEmit`;
- [ ] prova di **isolamento fra account**: non applicabile (nessuna lettura di dati), e detto esplicitamente;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-INSIGHTS]` nasce con la storia 0034; voce `da-coprire`
      nel registro di copertura con motivo e storia proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con la scelta delle cinque sezioni e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve la definizione OpenAPI da cui si genera il client, e il servizio da chiamare |

## 7. Fuori ambito

- il contenuto delle cinque sezioni: Fonti è la storia 0008, Metriche la 0012, Cruscotto la 0017, Avvisi la 0019,
  Copilota la 0022;
- l'avviso di quota nell'interfaccia: storia 0004.

## 8. Punti aperti

Nessuno.
