# 0003 — Guscio del modulo frontend

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'attività che ha appena attivato RepGrove
> voglio vedere l'app nella barra laterale del backoffice, con le sue sezioni e nella mia lingua
> così da capire subito dove si fa cosa, anche prima di aver collegato qualsiasi piattaforma.

**Contesto.** Un'app che risponde solo alle chiamate non esiste per il cliente. Questa storia mette il modulo
nella barra laterale con le sezioni che le epiche successive riempiranno, tradotte in cinque lingue e funzionanti
nei due temi. È anche il momento in cui si fissa l'aspetto: colore-categoria `amber`, icona a stella, così che nel
resto del lavoro nessuno debba più decidere il colore di niente.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/recensioni/` con il manifesto `manifest.ts`
   che dichiara identificativo, nome, icona `star`, `accentToken: 'amber'`, sezioni, risorse e metrica di quota.
2. **RF-2** — Il modulo è registrato nell'elenco `MODULES` del registro delle app e compare nella barra laterale
   **solo** quando registro e abilitazione dicono entrambi di sì.
3. **RF-3** — Le sezioni dichiarate sono cinque, nell'ordine in cui si usano: *Panoramica*, *Recensioni*, *Sedi*,
   *Richieste*, *Impostazioni*. Ognuna esiste come schermata, anche se al primo avvio è uno stato vuoto che
   spiega cosa fare.
4. **RF-4** — La *Panoramica* al primo avvio non è una schermata vuota generica: dice esattamente i due passi che
   servono («aggiungi una sede», «collega una piattaforma») e porta ai posti giusti.
5. **RF-5** — Tutte le stringhe visibili passano dallo spazio-nomi `recensioni` e sono presenti in `en, it, fr, es,
   de`; nessun testo scritto a mano nei componenti.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il modulo non conosce il `tenant_id` se non attraverso il contesto che
  la shell gli passa, e non gestisce l'autenticazione.
- **RT-2 — Interfaccia di programmazione (§2).** Le schermate leggono i dati con il client generato dalla
  definizione delle interfacce del servizio `recensioni`; nessuna chiamata scritta a mano.
- **RT-3 — Persistenza (§8).** Non applicabile.
- **RT-4 — Modulo frontend (§3, §5).** React + TypeScript + Vite, modulo caricato su richiesta; Tailwind e
  componenti senza stile proprio sopra i token del sistema di design; stato del server con TanStack Query. Solo
  token del sistema di design: nessun colore scritto a mano. Funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Traduzioni accanto al modulo in `i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi
  `recensioni`. La storia non è conclusa se ne manca una.
- **RT-6 — Varchi e quota (§6, §7).** Il manifesto dichiara la metrica `sedi_monitorate`; la barra laterale non
  mostra il modulo quando l'abilitazione manca. L'applicazione vera del limite è la storia 0004.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: le schermate sono vuote.
- **RT-9 — Registrazione eventi (§14).** Non applicabile lato frontend; nessun dato personale nei registri del
  browser.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare quando deve**
- **Dato** un account abilitato all'app `recensioni`
- **Quando** l'utente apre il backoffice
- **Allora** vede la voce «Recensioni» con l'icona a stella e l'accento ambra, e le cinque sezioni

**CA-2 — Il modulo non compare quando non deve**
- **Dato** un account **non** abilitato
- **Quando** l'utente apre il backoffice
- **Allora** la voce non compare, e la navigazione diretta all'indirizzo del modulo porta a una schermata che
  spiega che l'app non è attiva

**CA-3 — Cinque lingue davvero**
- **Dato** l'interfaccia impostata su ognuna delle cinque lingue
- **Quando** si visitano le cinque sezioni
- **Allora** non compare nessuna chiave di traduzione grezza e nessun testo in una lingua diversa da quella scelta

**CA-4 — Due temi davvero**
- **Dato** il tema chiaro e poi il tema scuro
- **Quando** si visitano le cinque sezioni
- **Allora** il contrasto rispetta il controllo automatico di accessibilità e nessun colore è scritto a mano

**CA-5 — Il primo avvio guida**
- **Dato** un account appena abilitato, senza sedi
- **Quando** apre la *Panoramica*
- **Allora** vede due passi espliciti e i pulsanti che ci portano, non un elenco vuoto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sui componenti con Vitest e Testing Library, con lo strato di rete finto; controllo dei
      tipi `tsc --noEmit` verde;
- [ ] prova di **isolamento fra account**: non applicabile lato frontend, coperta dal servizio;
- [ ] **prova end-to-end**: *rimando* alla storia 0030, proprietaria del percorso `[J-RECENSIONI]`, con voce
      `da-coprire` nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la motivazione dell'ordine delle sezioni;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] controllo automatico di **accessibilità** verde sulle schermate introdotte.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve la definizione delle interfacce da cui si genera il client |
| stub locale dell'abilitazione | finché l'abilitazione reale non esiste, il modulo va acceso nello stub per poterlo vedere in locale |

## 7. Fuori ambito

- il contenuto vero delle sezioni: arriva con le epiche 02-05;
- il riquadro pubblico per il sito del cliente, che non vive nel backoffice — storia 0024.

## 8. Punti aperti

- Il **colore-categoria `amber` è ripetuto** da altre tre app di catalogo (descrizione §11.6): se la piattaforma
  introducesse una seconda dimensione visiva, il manifesto andrà aggiornato.
- Nel repository risulta un **disallineamento fra listino e manifesto** sui colori delle due app reali: va
  verificato al momento dello scaffolding che `category` e `accentToken` di `recensioni` coincidano davvero.
</content>
