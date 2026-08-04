# 0003 — Guscio del modulo frontend

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che ha appena attivato AuditGrove sul proprio account
> voglio vedere l'applicazione comparire nella barra laterale del backoffice, con le sue sezioni e una schermata
> che mi dice cosa fare per primo
> così da capire che l'app c'è, che è mia, e da dove si comincia.

**Contesto.** Dopo la storia 0001 il servizio esiste ma è invisibile: nessuno può accorgersene dal backoffice.
Questa storia costruisce il **guscio** del modulo frontend — il manifesto, la registrazione nell'elenco dei
moduli, le sezioni vuote, le traduzioni nelle cinque lingue e lo stato vuoto iniziale. Non costruisce nessuna
schermata piena: la cronologia è la storia 0024, le approvazioni la 0021, gli strumenti la 0018, le sorgenti la
0006, l'integrità la 0014. Si fa adesso perché ogni storia successiva con superficie utente ha bisogno di un
posto dove appendersi, e perché lo stato vuoto è il primo testo che il cliente legge: merita di essere scritto
una volta bene invece che cinque volte di corsa.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/agentaudit/` con un manifesto che dichiara
   identificativo, nome, icona `shield-check`, colore d'accento `violet`, sezioni, risorse e componente radice.
2. **RF-2** — Il modulo è registrato nell'elenco dei moduli del backoffice e compare nella barra laterale
   **solo** quando registro e abilitazione dell'account concordano.
3. **RF-3** — Il manifesto dichiara cinque sezioni, nell'ordine in cui hanno senso per chi arriva: **Cronologia**,
   **Approvazioni**, **Strumenti e regole**, **Sorgenti**, **Integrità**.
4. **RF-4** — Ogni sezione è navigabile e mostra un contenuto d'attesa dichiarato («questa sezione arriva con la
   storia …»), non una pagina bianca né un errore.
5. **RF-5** — Quando l'account non ha ancora nessuna azione registrata, la sezione Cronologia mostra lo **stato
   vuoto** con il testo «nessuna azione ancora registrata: collega una sorgente» e un rimando alla sezione
   Sorgenti.
6. **RF-6** — Tutti i testi visibili passano dallo spazio-nomi di traduzione `agentaudit` e sono presenti in
   tutte e cinque le lingue.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il modulo non conosce il `tenant_id` se non attraverso il contesto che
  la shell gli passa, e non lo invia mai nelle richieste: il servizio lo ricava dal token verificato. Nessuna
  chiamata di dominio in questa storia.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova. Il modulo prepara l'uso del client generato
  dalla definizione OpenAPI dell'app, senza ancora invocarlo.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: la storia è interamente sul frontend.
- **RT-4 — Modulo frontend (§3, §5).** Modulo React caricato su richiesta dentro
  `frontend/apps/backoffice/src/modules/agentaudit/`, con `manifest.ts` che dichiara
  `{ id, name, icon, accentToken, sections[], resources, quota, component }` e viene aggiunto all'elenco dei
  moduli del registro. Interfaccia con Tailwind e componenti senza stile proprio sopra i token del sistema di
  design; **nessun colore scritto a mano**; funziona in tema chiaro e in tema scuro. Il colore d'accento `violet`
  deve coincidere con il campo `category` del listino dell'app (storia 0001).
- **RT-5 — Cinque lingue (§4).** Traduzioni accanto al modulo, in
  `frontend/apps/backoffice/src/modules/agentaudit/i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi `agentaudit`.
  **Nessun testo visibile scritto a mano nei componenti**; la storia non è conclusa se manca una lingua.
- **RT-6 — Varchi e quota (§6, §7).** Il manifesto dichiara la metrica di quota `actions` così che la shell possa
  mostrarne il consumo; il conteggio vero e il comportamento al tetto sono la storia 0004.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: gli strumenti di lettura sono
  dichiarati alla storia 0034.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il guscio non legge e non scrive dati di persone.
- **RT-9 — Registrazione eventi (§14).** Nessun evento applicativo nuovo; restano i registri tecnici comuni della
  shell, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare a chi ha diritto**
- **Dato** un utente di un account abilitato ad AuditGrove
- **Quando** apre il backoffice
- **Allora** vede AuditGrove nella barra laterale con l'icona `shield-check` e l'accento `violet`, e può aprire
  tutte e cinque le sezioni

**CA-2 — Il modulo non compare a chi non ha diritto**
- **Dato** un utente di un account **non** abilitato ad AuditGrove
- **Quando** apre il backoffice
- **Allora** non vede il modulo nella barra laterale, e raggiungerne l'indirizzo a mano non mostra nessun dato

**CA-3 — Lo stato vuoto guida**
- **Dato** un account abilitato che non ha ancora registrato nessuna azione
- **Quando** apre la sezione Cronologia
- **Allora** legge «nessuna azione ancora registrata: collega una sorgente» e trova un rimando che porta alla
  sezione Sorgenti

**CA-4 — Le cinque lingue ci sono tutte**
- **Dato** il modulo compilato
- **Quando** si esegue il controllo delle traduzioni sullo spazio-nomi `agentaudit`
- **Allora** ogni chiave esiste in `en, it, fr, es, de` e nessun testo visibile è scritto dentro un componente

**CA-5 — Il tema regge**
- **Dato** il modulo aperto
- **Quando** si passa dal tema chiaro al tema scuro
- **Allora** tutte le sezioni restano leggibili e nessun colore è fuori dai token del sistema di design

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit), compreso il
      controllo dei tipi `tsc --noEmit`;
- [ ] prove di **unità** sul manifesto e sulla composizione delle sezioni, con strato di rete finto;
- [ ] prova di **isolamento fra account**: non applicabile lato frontend in questa storia, e la cosa è dichiarata
      invece che sottintesa; la visibilità del modulo è comunque provata nei due casi (abilitato e non abilitato);
- [ ] **prova end-to-end**: risposta «rimando» — il percorso `[J-AGENTAUDIT]` nasce alla storia 0037, che è
      proprietaria della copertura; fino ad allora il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta la voce dell'app con
      l'esenzione motivata;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la scelta dell'ordine delle
      cinque sezioni e il testo dello stato vuoto;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali; il modulo è abilitato
      nello stub locale di abilitazione finché quella reale non esiste;
- [ ] controllo automatico di **accessibilità** sulle sezioni introdotte.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | Servono l'app scaffoldata, il listino con il colore-categoria e il client generato dalla definizione delle interfacce |
| Registro dei moduli del backoffice | Il modulo compare solo se registrato e abilitato: senza registrazione non è raggiungibile |

## 7. Fuori ambito

- ogni schermata piena: Cronologia (0024), Scheda dell'azione (0025), Approvazioni (0021), Strumenti e regole
  (0018 e 0019), Sorgenti (0006), Integrità (0014);
- il conteggio della quota mostrato all'utente: storia 0004;
- il ruolo di revisore in sola lettura, che cambia ciò che le sezioni mostrano: storia 0029.

## 8. Punti aperti

- **Il nome visibile dell'app nelle cinque lingue.** «AuditGrove» resta invariato come marchio, ma la riga di
  descrizione sotto il nome va scritta in cinque lingue senza promettere conformità normativa (§2.3 e §11 punto 6
  della [descrizione dell'applicazione](../application-description.md)): i testi commerciali sono materia dello
  sviluppatore, non di questa storia.
- **L'ordine delle sezioni è una proposta.** Cronologia per prima perché è ciò che si guarda tutti i giorni;
  Approvazioni seconda perché è ciò che si fa. Se l'adozione mostrasse che si entra per approvare, l'ordine va
  invertito — decisione da prendere sui dati, non ora.
