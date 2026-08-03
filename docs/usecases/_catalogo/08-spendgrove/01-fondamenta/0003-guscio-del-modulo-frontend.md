# 0003 — Guscio del modulo nel backoffice

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena attivato SpendGrove
> voglio vedere l'app comparire nella barra laterale del mio spazio di lavoro, con le sue sezioni
> così da capire subito che l'ho comprata davvero e dove si trova quello che mi serve.

**Contesto.** Il servizio risponde ma nessuno lo vede: senza il modulo nel backoffice l'app esiste solo per chi sa
chiamare le rotte. Questa storia costruisce il **guscio** — manifesto, registrazione, sezioni, traduzioni, colore —
e una prima schermata di elenco che legge i dati veri della storia `0002`. È il momento giusto perché tutte le
storie di dominio successive aggiungeranno schermate dentro questo guscio, e ognuna dovrebbe trovarlo già pronto.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `notespese` nel backoffice, con un manifesto che dichiara identificativo, nome,
   icona, colore d'accento `amber`, sezioni e risorse.
2. **RF-2** — Il modulo è registrato nell'elenco dei moduli e compare nella barra laterale **solo** quando registro
   e abilitazione dell'account dicono entrambi di sì.
3. **RF-3** — Le sezioni dichiarate sono quattro: *Panoramica*, *Spese*, *Note spese*, *Impostazioni*; le ultime
   due mostrano per ora uno stato vuoto onesto («qui arriveranno…»), non una pagina bianca.
4. **RF-4** — La sezione *Spese* mostra l'elenco reale delle spese dell'account, con ricerca a testo libero, filtro
   per stato e paginazione.
5. **RF-5** — Tutti i testi visibili passano dallo spazio-nomi `notespese` e sono presenti in inglese, italiano,
   francese, spagnolo e tedesco.
6. **RF-6** — L'interfaccia funziona in tema chiaro e in tema scuro e non usa nessun colore scritto a mano.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il modulo non conosce il `tenant_id` se non attraverso il contesto che la
  shell gli passa e non lo invia mai nelle richieste: l'isolamento è del servizio, non dell'interfaccia.
- **RT-2 — Interfaccia di programmazione (§2).** Le chiamate usano **il client generato** dalla definizione OpenAPI
  del servizio; nessuna chiamata scritta a mano, nessun percorso concatenato a stringhe.
- **RT-3 — Persistenza (§8).** Nessuna migrazione: la storia non tocca il database.
- **RT-4 — Modulo frontend (§3, §5).** Modulo caricato su richiesta in
  `frontend/apps/backoffice/src/modules/notespese/`, con `manifest.ts` che dichiara
  `{ id, name, icon, accentToken, sections[], resources, quota, component }` e voce aggiunta all'elenco dei moduli.
  Solo token del sistema di design; nessuna libreria con aspetto proprio marcato.
- **RT-5 — Cinque lingue (§4).** Traduzioni accanto al modulo, in
  `frontend/apps/backoffice/src/modules/notespese/i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi `notespese`.
  **Nessun testo visibile scritto a mano nei componenti**: la storia non è conclusa se manca una lingua.
- **RT-6 — Varchi e quota (§6, §7).** Il manifesto dichiara la metrica `receipts` così che la shell possa mostrarne
  il consumo; il comportamento a quota esaurita è della storia `0004`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il modulo mostra dati già dichiarati nella storia
  `0002`. Nessun dato personale finisce nell'indirizzo della pagina né nel registro degli eventi del browser.
- **RT-9 — Registrazione eventi (§14).** Nessun evento di servizio nuovo.
- **RT-10 — Accessibilità (§11).** Controllo automatico di accessibilità sulle schermate introdotte; la tabella ha
  intestazioni corrette e la navigazione da tastiera funziona.

## 4. Criteri di accettazione

**CA-1 — L'app compare a chi è abilitato**
- **Dato** un account abilitato a SpendGrove
- **Quando** l'utente apre il backoffice
- **Allora** nella barra laterale, sotto «Le tue app», compare *SpendGrove* con il colore `amber` e le sue quattro
  sezioni

**CA-2 — L'app non compare a chi non è abilitato**
- **Dato** un account senza abilitazione · **Quando** l'utente apre il backoffice
- **Allora** SpendGrove non compare nella barra laterale, e l'accesso diretto all'indirizzo della sezione porta alla
  pagina che spiega come attivarla, non a un errore tecnico

**CA-3 — Elenco reale e filtri**
- **Dato** un account con dodici spese in stati diversi
- **Quando** l'utente apre *Spese* e filtra per stato `da_rivedere`
- **Allora** vede solo quelle in quello stato, il conteggio dei risultati è coerente e la paginazione funziona

**CA-4 — Cinque lingue**
- **Dato** l'interfaccia impostata su francese, poi su tedesco
- **Quando** si apre ciascuna sezione del modulo
- **Allora** nessuna chiave di traduzione compare grezza e nessun testo resta in inglese per omissione

**CA-5 — Tema chiaro e scuro**
- **Dato** il tema scuro attivo · **Quando** si naviga fra le sezioni
- **Allora** contrasti e colori restano leggibili e nessun elemento usa un colore fuori dai token

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno frontend; l'intera suite prima del commit), compreso il controllo
      dei tipi;
- [ ] prove di **unità** sui componenti con lo strato di rete finto; nessuna prova che chiami un servizio vero;
- [ ] prova di **isolamento fra account**: non applicabile all'interfaccia, garantita dal servizio (storia `0002`);
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che possiede il percorso `[J-NOTESPESE]` e la voce nel
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la scelta delle quattro sezioni e del colore `amber`;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova;
- [ ] modulo abilitato nello stub locale dell'abilitazione, così che l'app sia usabile subito dopo l'unione;
- [ ] controllo automatico di **accessibilità** verde sulle schermate introdotte.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | Serve la definizione OpenAPI da cui si genera il client |
| `0002` | Serve l'elenco delle spese da mostrare: un guscio senza dati non si può provare davvero |

## 7. Fuori ambito

- La schermata di revisione della ricevuta, che è il cuore dell'app: storia `0008`.
- Il caricamento della foto: storia `0006`.
- Le note spese e l'approvazione: epica 03. Qui la sezione esiste ma è vuota, e lo dice.

## 8. Punti aperti

- **Nome commerciale mostrato nella barra laterale**: «SpendGrove» è quello del catalogo, ma nella stessa lista
  potrebbe comparire un giorno l'app 39 con un nome quasi identico. Se accadesse, servirà una etichetta che li
  distingua a colpo d'occhio (per esempio «SpendGrove · Note spese»). Decisione di prodotto, non di questa storia.
