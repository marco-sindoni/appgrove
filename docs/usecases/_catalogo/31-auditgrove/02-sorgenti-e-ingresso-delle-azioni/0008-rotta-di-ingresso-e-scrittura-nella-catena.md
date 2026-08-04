# 0008 — Rotta di ingresso e scrittura nella catena

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 02 — Sorgenti e ingresso delle azioni
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0004`, `0006`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come agente automatico che sta lavorando per conto di un'azienda
> voglio poter dichiarare ciò che ho fatto con una sola chiamata, e ricevere una ricevuta che dimostri che è stato
> registrato
> così che chi risponde di me possa provarlo anche fra un anno, e così che ripetere la chiamata per un errore di
> rete non produca due righe.

**Contesto.** È la storia che mette insieme tutto ciò che l'epica ha preparato: la sorgente autenticata (0006), lo
schema dell'azione (0007), la catena (0002) e la quota (0004). Da qui in poi il registro si riempie. Due dettagli
che sembrano tecnici e sono invece di prodotto: la **ricevuta** — numero di sequenza e impronta restituiti al
chiamante, che può conservarli e usarli per dimostrare in seguito che quella riga esisteva già allora — e
l'**idempotenza**, perché un agente che ritenta dopo un errore di rete non deve sporcare il registro con doppioni,
e un registro con doppioni è un registro di cui ci si fida meno.

## 2. Requisiti funzionali

1. **RF-1** — `POST /api/agentaudit/v1/actions` accetta una dichiarazione di azione conforme allo schema della
   storia 0007, autenticata con la chiave d'ingresso di una sorgente attiva.
2. **RF-2** — La dichiarazione viene **validata contro lo schema prima di toccare qualunque cosa**: se non è
   valida, nessuna riga viene scritta e la risposta dice cosa non va.
3. **RF-3** — La stessa dichiarazione ripetuta con lo stesso **identificativo di esecuzione** non crea una riga
   nuova: la risposta è la stessa ricevuta della prima volta.
4. **RF-4** — Una dichiarazione accettata viene accodata nella catena dell'account e consuma una unità della
   metrica `actions`.
5. **RF-5** — La risposta è una **ricevuta**: numero di sequenza nella catena, impronta dell'evento e momento di
   ricezione. È ciò che il chiamante può conservare come prova indipendente.
6. **RF-6** — La rotta accetta anche un **blocco di più azioni** in una sola chiamata, con esito per ciascuna:
   un'azione non valida nel blocco non impedisce alle altre di essere registrate, e la risposta dice quali sono
   passate e quali no.
7. **RF-7** — Una chiave revocata, una sorgente inesistente o una credenziale assente producono un rifiuto senza
   rivelare se l'account esista: si nega, non si spiega.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` si deriva **dal record della chiave verificata lato
  server**, mai dal corpo della richiesta: un campo con l'identificativo di un altro account nel corpo viene
  ignorato. La riga finisce nella catena dell'account proprietario della chiave e in nessun'altra. È il caso di
  prova più importante della storia.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/agentaudit/v1/actions` (singola e in blocco);
  corpo validato in modo dichiarativo; errori in `application/problem+json` con l'indicazione del campo; codici:
  `201` con ricevuta, `200` con la ricevuta originale in caso di ripetizione, `400` per schema non valido, `401`
  per credenziale assente o revocata, `402` ad abbonamento non attivo, `429` a quota e banda esaurite. Definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V5__azioni_campi_dichiarati.sql` sullo schema `app_agentaudit`: i campi
  dello schema dell'azione sulla tabella `actions`, più l'indice sull'identificativo di esecuzione per account che
  rende possibile l'idempotenza. Restano le regole della storia 0002: sola aggiunta, nessun percorso di modifica,
  `deleted_at` mai valorizzato.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione Sorgenti compare la documentazione di sola lettura del
  contratto — indirizzo della rotta, forma della dichiarazione, esempio — perché chi collega un agente deve
  trovarla dove sta la chiave, non altrove. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le stringhe della pagina di documentazione e i messaggi d'errore mostrati
  all'utente passano dallo spazio-nomi `agentaudit` e sono presenti in `en, it, fr, es, de`. L'esempio di
  dichiarazione resta in forma tecnica e non si traduce.
- **RT-6 — Varchi e quota (§6, §7).** Prima di accodare, il servizio prenota una unità della metrica `actions`
  (natura `flow`); a tetto e banda di cortesia esauriti risponde `429` con l'indicazione del rimedio, e il rifiuto
  viene contato come previsto dalla storia 0004. Con abbonamento `canceled` risponde `402`; con `past_due`
  continua a servire.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. **Dichiarazione esplicita**: la rotta di
  ingresso **non** sarà mai esposta come strumento all'assistente — un assistente che può scrivere nel registro di
  ciò che gli assistenti fanno è un conflitto d'interesse costruito apposta.
- **RT-8 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: **identificativo del
  richiedente** e **identificativo dell'agente** sulla tabella `actions`, campi annotati `@PersonalData`, tabella
  aggiunta a `exportData` e `purgeData`. Nel manifesto va scritta la postura: sono identificativi, non nomi; la
  minimizzazione dei parametri è la storia 0009.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «azione registrata», «dichiarazione respinta per schema»,
  «dichiarazione respinta per quota», «credenziale rifiutata» sono registrati con `tenant_id`, `app_id`,
  identificativo della sorgente e identificativo di correlazione, **senza il contenuto della dichiarazione**.

## 4. Criteri di accettazione

**CA-1 — Una dichiarazione valida diventa una riga con ricevuta**
- **Dato** una sorgente attiva di un account abilitato e con quota disponibile
- **Quando** dichiara un'azione conforme allo schema
- **Allora** riceve `201` con numero di sequenza, impronta e momento di ricezione, la riga compare nella catena e
  il consumo di quota aumenta di uno

**CA-2 — Ripetere non duplica**
- **Dato** una dichiarazione già registrata con un certo identificativo di esecuzione
- **Quando** la stessa dichiarazione viene inviata di nuovo
- **Allora** la risposta è la **stessa ricevuta** della prima volta, non viene scritta nessuna riga nuova e la
  quota non viene consumata una seconda volta

**CA-3 — Schema non valido**
- **Dato** una dichiarazione priva dello strumento
- **Quando** viene inviata
- **Allora** riceve `400` in `problem+json` con il nome del campo mancante, e nulla viene scritto

**CA-4 — Chiave revocata**
- **Dato** una sorgente la cui chiave è stata revocata
- **Quando** tenta di dichiarare un'azione
- **Allora** riceve un rifiuto che non rivela l'esistenza né lo stato dell'account, e nulla viene scritto

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, e la chiave di una sorgente di `A`
- **Quando** con quella chiave si dichiara un'azione che porta nel corpo l'identificativo dell'account `B`
- **Allora** la riga finisce nella catena di `A`, il campo nel corpo è ignorato, e nella catena di `B` non compare
  nulla

**CA-6 — Blocco parzialmente valido**
- **Dato** un blocco di cinque dichiarazioni di cui una non conforme
- **Quando** viene inviato
- **Allora** le quattro valide vengono registrate con le rispettive ricevute, la quinta è respinta con il proprio
  errore, e la risposta distingue chiaramente gli esiti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'idempotenza e sulla composizione della ricevuta, e di **integrazione** sulla rotta
      di ingresso, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla rotta di ingresso, compreso il tentativo di forzare
      l'identificativo dell'account dal corpo;
- [ ] **prova end-to-end**: risposta «rimando» — la dichiarazione di un'azione è il primo passo del percorso
      `[J-AGENTAUDIT]`, che nasce alla storia 0037, proprietaria della copertura; fino ad allora il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta l'esenzione motivata;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con identificativo del richiedente e
      dell'agente, campi annotati `@PersonalData`, tabella `actions` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con **tre voci obbligatorie**: la ricevuta come prova indipendente,
      l'idempotenza sull'identificativo di esecuzione, e il divieto permanente di esporre l'ingresso come
      strumento conversazionale;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto, e il divieto è dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali; l'agente finto della
      storia 0005 usa questa rotta.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | L'accodamento nella catena e il calcolo dell'impronta |
| storia `0004` | Il consumo di quota, la banda di cortesia e il rifiuto contato |
| storia `0006` | La credenziale della sorgente, da cui si deriva l'account |
| storia `0007` | Lo schema contro cui si valida |
| Punto aperto sulla credenziale non umana (storia 0006, sezione 8) | Va chiuso **prima** di implementare questa storia: è ciò che rende legittima la derivazione dell'account dalla chiave |

## 7. Fuori ambito

- il trattamento dei parametri, che qui arrivano ma non vengono ancora minimizzati né ripuliti: storie 0009 e
  0010. **Finché quelle due non esistono, questa rotta non va aperta a un cliente vero**;
- la rilevazione dei buchi nella numerazione della sorgente: storia 0011;
- la richiesta di nulla osta **prima** dell'azione, che è l'altro capo del flusso: storia 0020.

**Dentro l'ambito, invece, e da non dimenticare**: l'agente finto per lo sviluppo locale (storia 0005) va
**ripuntato su questa rotta**. Finora scriveva attraverso il servizio di accodamento interno; da qui in avanti
deve percorrere la stessa strada dei clienti, altrimenti lo strumento con cui si sviluppa smette di esercitare
ciò che si vende.

## 8. Punti aperti

- **Il limite di dimensione di una dichiarazione e di un blocco.** Un blocco senza tetto è un modo per far male al
  servizio; un tetto troppo basso costringe l'agente a molte chiamate. Propongo un tetto dichiarato nella
  documentazione e un errore esplicito al superamento. Da confermare.
- **Che cosa succede se il servizio non riesce a scrivere.** Per UC 0065 la postura corretta su un'operazione dove
  la tracciabilità è requisito è *in caso di dubbio si nega*. Ma qui **noi non siamo l'operazione**: siamo il
  registro. Rifiutare la ricevuta è giusto; ma l'agente cosa dovrebbe fare — fermarsi o procedere non tracciato?
  Propongo che il contratto dica esplicitamente all'agente di fermarsi per le azioni a effetto irreversibile e di
  procedere per le letture, ma è **direzione di prodotto**: chi chiude è lo sviluppatore.
- **Conservazione della ricevuta da parte del cliente.** La ricevuta vale come prova solo se il cliente la
  conserva. Vale la pena spiegarglielo nella documentazione: è lo stesso principio del sigillo consegnato fuori
  perimetro (storia 0017).
