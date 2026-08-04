# 0003 — Guscio del modulo frontend

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'impresa abbonata a StockGrove
> voglio trovare il magazzino nella barra laterale del backoffice e potermici muovere dentro
> così da vedere subito che l'app è mia, è nella mia lingua e mi dice cosa fare per cominciare, invece di
> presentarmi una pagina bianca.

**Contesto.** Il servizio risponde ma nessuno lo vede. Questa storia crea il modulo del backoffice: manifesto,
registrazione, sezioni, traduzioni nelle cinque lingue, tema chiaro e scuro. Non porta nessuna funzione nuova — le
schermate vere arrivano dalle epiche 02, 03 e 04 — ma porta la **forma** dentro cui tutte quelle schermate
andranno, e la porta adesso perché ogni schermata scritta prima del guscio va poi riscritta. L'unica cosa che
l'utente può fare a fine storia è aprire l'app, vedere sei sezioni e un elenco vuoto che gli dice cosa manca.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/magazzino/` con un `manifest.ts` che dichiara
   `{ id: 'magazzino', name, icon: 'boxes', accentToken: 'amber', sections, resources, quota, component }`, ed è
   aggiunto all'elenco `MODULES` del registro delle app.
2. **RF-2** — Il modulo espone sei sezioni nell'ordine in cui si lavora: **giacenze** (la schermata d'ingresso),
   **articoli**, **movimenti**, **inventari**, **riordino**, **impostazioni**.
3. **RF-3** — La sezione **articoli** mostra l'elenco reale letto dal servizio (storia `0002`), paginato; quando
   l'account non ha ancora articoli mostra uno stato vuoto che dice «ancora nessun articolo» e indica il primo
   passo da fare, non una tabella con zero righe.
4. **RF-4** — Le altre cinque sezioni esistono, sono raggiungibili e mostrano uno stato vuoto onesto che dichiara
   quale funzione arriverà lì; nessuna sezione porta a una pagina rotta.
5. **RF-5** — L'app compare nella barra laterale **solo** quando registro delle app e abilitazione dell'account
   concordano; quando l'abilitazione manca, il modulo non è raggiungibile e la barra laterale non lo mostra.
6. **RF-6** — Tutte le stringhe visibili passano dallo spazio-nomi `magazzino` e sono presenti nelle cinque lingue.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il modulo **non** conosce l'identificativo dell'account se non attraverso
  il contesto che la shell gli passa, non lo invia mai nelle richieste e non gestisce l'autenticazione: il filtro
  per account è del servizio e resta lì.
- **RT-2 — Interfaccia di programmazione (§2).** I dati si leggono con il **client generato** dalla definizione
  OpenAPI del servizio `magazzino`; nessuna chiamata scritta a mano, nessun indirizzo di rete cablato nel
  componente. Gli errori del servizio arrivano in `application/problem+json` e vengono mostrati con il messaggio
  che portano.
- **RT-3 — Persistenza (§8).** Nessuna migrazione: la storia non tocca la base di dati.
- **RT-4 — Modulo frontend (§3, §5).** React + TypeScript + Vite, modulo caricato **su richiesta**; interfaccia con
  Tailwind e componenti senza stile proprio sopra i token del sistema di design; stato del server con TanStack
  Query. Solo token del sistema di design, **nessun colore scritto a mano**; funziona in tema chiaro e scuro.
  Il colore-categoria `amber` dichiarato in `accentToken` deve coincidere con il `category` del listino.
- **RT-5 — Cinque lingue (§4).** Traduzioni in
  `frontend/apps/backoffice/src/modules/magazzino/i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi `magazzino`;
  **nessun testo visibile scritto a mano nei componenti**; la storia non è conclusa se manca una lingua.
- **RT-6 — Varchi e quota (§6, §7).** Il modulo mostra l'indicatore di quota della metrica `articoli_gestiti`
  (natura `stock`) letto dall'abilitazione: «*n* articoli su *m*». Non decide nulla: il blocco è del servizio
  (storia `0004`) e il modulo si limita a mostrare la risposta `429` con il rimedio che contiene.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato: il guscio non introduce funzioni.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: il modulo mostra dati già esistenti e non ne
  crea. Nessun tracciamento dentro l'app, solo cookie tecnici, nessun banner di consenso.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo lato servizio. Gli errori di rete del modulo sono
  registrati dal servizio che li produce, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — L'app compare e si apre**
- **Dato** un utente di un account **abilitato** a StockGrove
- **Quando** apre il backoffice
- **Allora** vede «StockGrove» nella barra laterale con l'icona e il colore `amber`, e aprendola arriva sulla
  sezione **giacenze**

**CA-2 — Account non abilitato**
- **Dato** un utente di un account **non** abilitato a StockGrove
- **Quando** apre il backoffice e prova a raggiungere la rotta del modulo
- **Allora** l'app non compare nella barra laterale e la rotta non è accessibile

**CA-3 — Stato vuoto invece di una tabella a zero righe**
- **Dato** un account abilitato senza alcun articolo · **Quando** apre la sezione **articoli**
- **Allora** vede il messaggio «ancora nessun articolo» con l'indicazione del primo passo, non una tabella vuota
  con le sole intestazioni

**CA-4 — Cinque lingue e nessun testo cablato**
- **Dato** l'interfaccia impostata su ciascuna delle cinque lingue (`en, it, fr, es, de`)
- **Quando** si percorrono le sei sezioni
- **Allora** nessuna stringa compare nella lingua sbagliata o come chiave grezza, e il controllo automatico sulle
  traduzioni mancanti è verde

**CA-5 — Tema chiaro e tema scuro**
- **Dato** il backoffice in tema chiaro e poi in tema scuro
- **Quando** si aprono le sei sezioni
- **Allora** testi e sfondi restano leggibili in entrambi, il controllo automatico di accessibilità sulla schermata
  principale passa e nessun colore proviene da fuori i token del sistema di design

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno frontend; l'intera suite prima del commit), compreso il controllo
      dei tipi `tsc --noEmit`;
- [ ] prove di **unità** sul modulo con Vitest e Testing Library e strato di rete finto: elenco, stato vuoto, errore
      del servizio;
- [ ] prova di **isolamento fra account**: non applicabile lato modulo — il filtro è del servizio ed è provato dalla
      storia `0002`;
- [ ] **prova end-to-end**: *rimando* — la superficie esiste ma è ancora un guscio; il percorso `[J-MAGAZZINO]`
      nasce con la storia `0036` e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna modifica, la storia non tratta dati personali;
- [ ] **registro delle decisioni** compilato, con l'ordine delle sezioni e il colore-categoria;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali; il modulo è abilitato nello
      stub locale delle abilitazioni finché quella reale non esiste;
- [ ] controllo automatico di **accessibilità** verde sulla schermata principale del modulo;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | La definizione OpenAPI da cui si genera il client deve esistere |
| `0002` | Senza la risorsa degli articoli la sezione **articoli** non avrebbe nulla da leggere |

## 7. Fuori ambito

- La gestione completa dell'anagrafica dalla interfaccia (creazione, modifica, archiviazione): storia `0006`.
- Le schermate di giacenza, movimenti, inventario e riordino: rispettivamente epiche 03, 04 e 05; qui esistono solo
  le sezioni vuote.
- La scansione da telefono: epica 06.
- Il blocco a `429` e la sua spiegazione in interfaccia: storia `0004`.

## 8. Punti aperti

- **Nome dell'icona `boxes`**: va verificato che esista nell'insieme di icone in uso nel backoffice; se non c'è, se
  ne sceglie una equivalente al momento dell'implementazione, senza introdurre una libreria nuova.
- **Sezione d'ingresso**: la proposta è **giacenze**, perché è la domanda che si fa mille volte al giorno. Se la
  prova sul campo mostrasse che chi apre l'app cerca prima il riordino, l'ordine si cambia in una riga del
  manifesto: è una scelta di prodotto minore, la chiude lo sviluppatore.
