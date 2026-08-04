# 0027 — Semaforo del budget consultabile

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 05 — Budget, avvisi e anomalie
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore del prodotto di un cliente che ha un budget mensile rigido
> voglio poter chiedere a TokenGrove «posso ancora spendere?» e decidere io cosa fare della risposta
> così da poter davvero fermare le chiamate quando serve, senza mettere TokenGrove sulla strada del mio prodotto.

**Contesto.** È la storia che chiude in modo onesto il tema del «tetto di spesa» della scheda di catalogo. Poiché
TokenGrove non sta in mezzo alle chiamate (§3.1 del documento capofila), **non può fermarle** — e sarebbe una
promessa falsa dire il contrario. Quello che può fare è esporre un semaforo che il codice del cliente consulta e
usa come vuole. La responsabilità del blocco resta di chi la può portare.

**Va detto con la massima chiarezza, ed è il punto centrale di questa storia: fermare le chiamate di un cliente è
un effetto pesante.** Significa che il suo prodotto smette di rispondere ai suoi utenti, che i suoi clienti vedono
un errore, e che magari quella chiamata era l'unica cosa importante della giornata. Non è una funzione di
protezione: è un'interruzione di servizio decisa in anticipo. Per questo il semaforo (a) non blocca nulla da sé,
(b) è per costruzione **permissivo in caso di dubbio**, e (c) è accompagnato da una spiegazione esplicita delle
conseguenze prima che qualcuno lo attivi.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un'interrogazione di sola lettura, veloce, che dato un ambito (tutto l'account, o un valore di
   dimensione) restituisce un semaforo a tre stati — verde, giallo, rosso — con il consumato, il tetto, la
   previsione e l'istante a cui il dato si riferisce.
2. **RF-2** — La risposta dichiara sempre la propria **freschezza**: il semaforo si basa su dati che possono avere
   qualche minuto di ritardo, e chi lo usa deve saperlo.
3. **RF-3** — L'interrogazione è progettata per essere chiamata spesso e **memorizzata dal chiamante** per un
   tempo dichiarato; non è pensata per essere interrogata prima di ogni singola chiamata al modello.
4. **RF-4** — In caso di dubbio si risponde **verde**: se il dato non è disponibile, se una fonte è ferma, se il
   servizio è in difficoltà, la risposta non è rosso. Un semaforo che diventa rosso perché *noi* abbiamo un
   problema fermerebbe il prodotto del cliente per un guasto nostro, che è esattamente ciò che questa architettura
   esiste per evitare.
5. **RF-5** — La documentazione destinata a chi integra spiega, prima dell'esempio di codice, **cosa comporta
   fermarsi**: quali chiamate si perdono, cosa vede l'utente finale, e la raccomandazione di degradare (usare un
   modello più economico, mettere in coda) invece di rifiutare.
6. **RF-6** — L'app registra quante volte il semaforo è stato consultato e con quale esito, e lo mostra: se un
   cliente riceve rosso da tre giorni, è un'informazione che deve vedere anche nell'interfaccia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il semaforo si consulta con la chiave di invio dell'account (storia
  `0009`) e risponde solo per quell'account; nessun ambito di un altro account è raggiungibile.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/spesa_modelli/v1/semaforo` con parametro
  dell'ambito; risposta minuta e con indicazione di quanto memorizzarla; errori in `problem+json`. **Ogni** errore
  del servizio, incluso il sovraccarico, produce una risposta interpretabile come verde da chi integra: la regola
  è scritta nel contratto e non lasciata all'interpretazione.
- **RT-3 — Varchi e quota (§6, §7).** La consultazione del semaforo **non consuma** la metrica
  `misure_registrate`: non registra nulla. Ha però un proprio limite di frequenza, coerente con la
  memorizzazione raccomandata.
- **RT-4 — Prestazioni.** La risposta si serve da un valore precalcolato e aggiornato dalla valutazione dei budget,
  non da un'aggregazione fatta al momento sulla tabella delle misure: è l'unica rotta dell'app che potrebbe essere
  chiamata molto spesso.
- **RT-5 — Modulo frontend (§3, §5).** Nella scheda del budget compaiono lo stato del semaforo, l'esempio di
  codice pronto da copiare e — sopra l'esempio — l'avvertenza sulle conseguenze del fermarsi. Solo token del
  sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** L'avvertenza sulle conseguenze è presente in `en, it, fr, es, de`. È il testo che
  non deve essere ammorbidito in traduzione.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `stato_budget` (storia `0023`) restituisce lo stesso
  semaforo, marcato **lettura**. Non esiste e non deve esistere uno strumento conversazionale che «attivi il
  blocco»: il blocco non è nostro.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-9 — Registrazione eventi (§14).** Evento «semaforo consultato» aggregato (non uno per chiamata) con
  `tenant_id`, `app_id`, ambito ed esito, con identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Semaforo verde e rosso**
- **Dato** un budget consumato al 40%
- **Quando** si consulta il semaforo
- **Allora** risponde verde, con consumato, tetto, previsione e istante del dato; superato il 100%, risponde rosso

**CA-2 — In caso di dubbio, verde**
- **Dato** il servizio che non riesce a calcolare lo stato (dato mancante, fonte ferma, sovraccarico)
- **Quando** si consulta il semaforo
- **Allora** la risposta è interpretabile come verde e dichiara che il dato non è affidabile; **in nessun caso**
  un guasto nostro produce un rosso

**CA-3 — La freschezza è dichiarata**
- **Dato** una consultazione qualunque
- **Quando** si legge la risposta
- **Allora** contiene l'istante a cui il dato si riferisce e il tempo per cui si può memorizzare

**CA-4 — Il semaforo non consuma quota**
- **Dato** un account che consulta il semaforo diecimila volte
- **Quando** si legge la quota
- **Allora** è invariata

**CA-5 — L'avvertenza c'è ed è prima dell'esempio**
- **Dato** la scheda del budget in una qualunque delle cinque lingue
- **Quando** si guarda la sezione del semaforo
- **Allora** l'avvertenza sulle conseguenze del fermarsi precede l'esempio di codice

**CA-6 — Isolamento fra account**
- **Dato** la chiave di invio dell'account `A`
- **Quando** si consulta il semaforo per un ambito dell'account `B`
- **Allora** la richiesta non restituisce lo stato di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul comportamento permissivo in caso di dubbio, con simulazione di guasto e di
      sovraccarico, e di **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account** sul semaforo;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «budget superato, il
      semaforo diventa rosso; servizio in errore, il semaforo resta permissivo», e aggiornare il registro di
      copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con revisione mirata dell'avvertenza;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta del semaforo consultivo invece del blocco
      e sulla regola «in caso di dubbio, verde»;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0023` | Serve il budget di cui il semaforo racconta lo stato |
| Storia `0024` | La previsione fa parte della risposta |

## 7. Fuori ambito

- **qualunque forma di blocco eseguito da noi**: non è rimandato, è escluso per scelta architetturale (§3.1 e
  punto P5 del documento capofila). Riaprirlo significa riaprire la via del punto di passaggio, che è una decisione
  di prodotto dello sviluppatore, non un'aggiunta;
- la revoca automatica della chiave del fornitore quando si sfora: **non si fa mai**. Sarebbe un effetto
  irreversibile verso l'esterno, eseguito da noi con una credenziale del cliente, sulla base di un dato che
  potrebbe essere incompleto. Se un cliente lo chiedesse, la risposta è il semaforo;
- una libreria che incapsuli la consultazione: rimandata insieme alla libreria di invio (storia `0008`, §7).

## 8. Punti aperti

- **Se offrire un semaforo anche per soglie diverse dal budget** (per esempio «rosso durante un'impennata in
  corso»). Sarebbe utile, ma legherebbe una decisione di interruzione a un rilevamento probabilistico, il che è
  molto più delicato di un tetto di spesa deciso da una persona. Proposta: **no** per ora, e in ogni caso mai
  senza che l'account lo abbia scelto esplicitamente. La chiude lo sviluppatore.
