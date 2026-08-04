# 0005 — Avvio locale e dati di prova

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende il lavoro su LeadGrove
> voglio avviare l'app in locale e trovarla già popolata di dati inventati
> così da vedere subito come si comporta invece di passare mezz'ora a crearne a mano.

**Contesto.** Un'app vuota si prova male: metà dei difetti di interfaccia — l'elenco che non pagina, la colonna
che va a capo, lo stato vuoto che non c'è — si vedono solo con dei dati dentro. Questa storia chiude l'epica delle
fondamenta rendendo l'app **eseguibile e navigabile** subito dopo l'unione del ramo, senza passi manuali e senza
cablaggi negli script.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` mostra `sales` con porta `8104` e schema `app_sales`, derivandolo dal solo file
   `application.properties`.
2. **RF-2** — `./app-start.sh` avvia LeadGrove insieme agli altri servizi e le rotte `/api/sales/v1/*` sono
   raggiungibili dal proxy locale, senza righe aggiunte a mano.
3. **RF-3** — Esiste un insieme di dati di prova **inventati** — due account distinti, alcune aziende, contatti,
   una pipeline con le sue fasi, trattative in fasi diverse e qualche attività — caricabile con un comando.
4. **RF-4** — I dati di prova usano nomi palesemente finti e indirizzi di posta elettronica nel dominio riservato
   alle prove: mai dati che somiglino a persone vere.
5. **RF-5** — I dati di prova comprendono **due account**, così che l'isolamento sia visibile a occhio e non solo
   nelle prove automatiche.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I dati di prova stanno su due account diversi; nessuna riga senza
  `tenant_id`.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova.
- **RT-3 — Persistenza (§8).** I dati di prova si caricano con uno script separato, **non** con una migrazione
  Flyway: le migrazioni sono lo schema, non il contenuto.
- **RT-4 — Modulo frontend (§3, §5).** Il modulo è abilitato nello stub locale dell'abilitazione, altrimenti non
  compare in barra laterale.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile nuovo.
- **RT-6 — Varchi e quota (§6, §7).** L'account di prova principale ha posti assegnati entro il tetto; il secondo
  ha un posto solo, così da poter provare a mano il rifiuto per quota.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento.
- **RT-8 — Dati personali (§10).** I dati di prova sono **inventati**: nessun dato personale reale entra nel
  repository. Nessuna voce nuova nel manifesto.
- **RT-9 — Registrazione eventi (§14).** Il caricamento dei dati di prova si registra con il conteggio delle righe
  create, senza contenuti.
- **RT-10 — Avvio locale (§15).** La scoperta automatica dei servizi deriva tutto da `application.properties`: se
  viene voglia di modificare a mano uno script di avvio, è un difetto della scoperta, non un passo del lavoro.

## 4. Criteri di accettazione

**CA-1 — L'app si scopre da sola**
- **Dato** il ramo unito e nessuna modifica manuale agli script
- **Quando** si esegue `./dev.sh services`
- **Allora** compare `sales` con porta `8104` e schema `app_sales`

**CA-2 — L'app si avvia e risponde dal proxy**
- **Dato** lo stack locale avviato con `./app-start.sh`
- **Quando** si chiama `/api/sales/v1/ping` attraverso il proxy locale con un token valido
- **Allora** risponde positivamente

**CA-3 — I dati di prova rendono l'app navigabile**
- **Dato** i dati di prova caricati
- **Quando** si apre il modulo nel backoffice locale
- **Allora** si vedono aziende, contatti, una pipeline con trattative in fasi diverse e alcune attività, e nessun
  elenco è vuoto se non quelli previsti

**CA-4 — Due account, isolamento visibile**
- **Dato** i due account di prova
- **Quando** si entra con l'utente del secondo
- **Allora** non si vede nessuna riga del primo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `smoke`; l'intera suite prima del commit);
- [ ] prova di **avvio reale** dell'artefatto nel profilo di spedizione (area `smoke`);
- [ ] prova di **isolamento fra account** già coperta dalle storie precedenti, qui verificata anche a occhio;
- [ ] **prova end-to-end**: rimando alla storia 0037; i dati di prova deterministici che il percorso userà nascono
      qui;
- [ ] **traduzioni**: nessun testo visibile nuovo;
- [ ] **manifesto dei dati**: nessuna voce nuova; i dati di prova sono inventati;
- [ ] **registro delle decisioni** compilato;
- [ ] `./dev.sh services` e `./app-start.sh` funzionano senza modifiche manuali agli script;
- [ ] documentazione aggiornata: la guida allo sviluppo locale, se cita l'elenco delle app.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0002`, `0003`, `0004` | I dati di prova hanno senso solo se ci sono tabelle, modulo e posti |

## 7. Fuori ambito

- i dati di prova per il percorso end-to-end di piattaforma nella loro forma finale: storia 0037;
- l'ambiente di prova su cloud: è di piattaforma.

## 8. Punti aperti

- Nessuno.
