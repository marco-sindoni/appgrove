# 0007 — Contratto dell'azione registrata

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 02 — Sorgenti e ingresso delle azioni
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che dovrà collegare i propri agenti ad AuditGrove
> voglio sapere esattamente quali informazioni devo dichiarare per ogni azione, e in che forma
> così da poterlo fare una volta sola e da avere la certezza che il registro risponderà alle domande che mi
> verranno fatte.

**Contesto.** Il valore del registro dipende interamente da **cosa c'è scritto in una riga**. Le sei domande a cui
un'azione deve rispondere sono elencate al §1 della [descrizione dell'applicazione](../application-description.md):
chi l'ha chiesta, quale strumento è stato usato, con quali parametri, quale effetto ha prodotto, quale
approvazione umana c'era o non c'era, cosa è stato letto e cosa scritto. Questa storia le traduce in uno schema.
Non inventa nomi propri: esiste già una grammatica condivisa per descrivere l'azione di un agente — lo standard
aperto **AOS (*Agent Observability Standard*, «standard di osservabilità degli agenti») di OWASP** — e allinearvisi
è gratis, mentre discostarsene costa a ogni cliente che deve scrivere un adattatore. La storia consegna lo schema
e il suo versionamento; la rotta che lo riceve è la 0008.

## 2. Requisiti funzionali

1. **RF-1** — Esiste lo schema dell'**azione dichiarata**, versionato con il servizio, che copre le sei domande:
   momento dichiarato, identificativo di esecuzione, agente, richiedente, strumento, natura, classe di effetto,
   risorse lette e scritte, esito, riferimento al nulla osta.
2. **RF-2** — Lo schema distingue la **natura** dell'azione (lettura o scrittura) dalla sua **classe di effetto**:
   lettura, scrittura, cancellazione, invio verso l'esterno, pagamento. Sono due assi diversi e servono a due cose
   diverse: la natura descrive l'azione, la classe di effetto è ciò su cui si appendono le regole (storia 0019).
3. **RF-3** — Lo schema è allineato agli eventi dello standard OWASP: la richiesta di chiamata a uno strumento
   porta identificativo dello strumento, identificativo di esecuzione, argomenti e **motivazione della scelta**;
   l'esito porta il risultato e l'indicazione di errore. Riferimento: https://aos.owasp.org/spec/trace/events/
4. **RF-4** — Lo schema dichiara quali campi sono **obbligatori** e quali facoltativi, e la risposta a una
   dichiarazione incompleta è un errore descrittivo che dice cosa manca — non un'accettazione parziale.
5. **RF-5** — Lo schema porta un **numero di versione**; una dichiarazione senza versione è rifiutata, e la
   convivenza di due versioni durante un passaggio è prevista e descritta.
6. **RF-6** — Il campo dell'**approvazione** ammette tre valori distinti e non confondibili: approvata da una
   persona (con riferimento al nulla osta), esplicitamente non richiesta dalle regole, oppure **assente** — cioè
   l'azione è stata compiuta senza che nessuno l'abbia autorizzata. Il terzo valore è la cosa più interessante che
   il registro possa contenere e non deve poter essere confuso con gli altri due.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Lo schema **non contiene** l'identificativo dell'account: quello si
  ricava dalla credenziale verificata della sorgente (storia 0006). Un `tenant_id` che arrivasse nel corpo della
  dichiarazione viene ignorato, e la cosa è verificata da un caso di prova esplicito.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta in questa storia: lo schema è dichiarato e
  pubblicato nella definizione OpenAPI del servizio, che viene aggiornata nello stesso commit. Gli errori di
  validazione escono in `application/problem+json` con l'indicazione del campo mancante o non valido.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: lo schema descrive ciò che la tabella delle azioni (storia
  0002) riceverà. I campi che ne discendono vanno aggiunti con la migrazione della storia 0008, non qui.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata. La documentazione del contratto va però resa leggibile
  dal cliente: propongo una pagina di sola lettura nella sezione Sorgenti, alla storia 0008.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile nuovo. **Attenzione**: i nomi dei campi dello schema
  restano identificatori tecnici in inglese, allineati allo standard; sono descrizioni tecniche, non testo
  d'interfaccia.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo: lo schema non è un'azione. Il consumo avviene
  all'accodamento (storia 0008).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia. Lo schema dell'azione e il
  contratto degli strumenti dell'app (epica 07) sono due cose diverse e non vanno confuse: il primo descrive ciò
  che AuditGrove **riceve**, il secondo ciò che AuditGrove **espone**.
- **RT-8 — Dati personali (§10).** Lo schema **prevede** campi che riguardano persone — l'identificativo del
  richiedente e quello dell'agente — ma non li introduce ancora in nessuna tabella: le voci di manifesto nascono
  con la storia 0008, che è quella che li scrive. Qui si dichiara la **postura**: identificativi, mai nomi; e i
  parametri non entrano in chiaro (storie 0009 e 0010).
- **RT-9 — Registrazione eventi (§14).** Nessun evento applicativo nuovo.

## 4. Criteri di accettazione

**CA-1 — Lo schema risponde alle sei domande**
- **Dato** lo schema pubblicato
- **Quando** si compone una dichiarazione valida di un'azione
- **Allora** da quella sola dichiarazione si possono ricavare chi ha chiesto, quale strumento, con quali parametri
  (in forma minimizzata), quale effetto, quale approvazione, cosa è stato letto e cosa scritto

**CA-2 — Dichiarazione incompleta**
- **Dato** una dichiarazione priva del campo dello strumento
- **Quando** viene validata contro lo schema
- **Allora** l'esito è un errore descrittivo in `problem+json` che nomina il campo mancante, e nessuna riga viene
  considerata valida

**CA-3 — L'assenza di approvazione non si confonde con il non averla richiesta**
- **Dato** due dichiarazioni, una con approvazione «non richiesta dalle regole» e una con approvazione «assente»
- **Quando** entrambe vengono validate e lette
- **Allora** restano distinguibili in modo non ambiguo, e la seconda è riconoscibile come azione compiuta senza
  autorizzazione

**CA-4 — Versione obbligatoria**
- **Dato** una dichiarazione senza numero di versione dello schema
- **Quando** viene validata
- **Allora** viene rifiutata con un errore che indica quali versioni sono accettate

**CA-5 — Nessun identificativo di account nel corpo**
- **Dato** una dichiarazione che contiene un campo con l'identificativo di un altro account
- **Quando** viene validata
- **Allora** quel campo viene ignorato e non ha nessun effetto sulla destinazione della riga

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione dello schema, sui campi obbligatori e sui tre valori
      dell'approvazione;
- [ ] prova di **isolamento fra account**: il caso «identificativo di account nel corpo ignorato» è provato
      esplicitamente;
- [ ] **prova end-to-end**: risposta «nessun impatto» — la storia non tocca la superficie utente;
- [ ] **traduzioni**: nessun testo visibile introdotto, e il fatto che i nomi dei campi restino identificatori
      tecnici in inglese è dichiarato;
- [ ] **manifesto dei dati**: nessuna voce nuova in questa storia; le voci nascono con la 0008, e il rimando è
      scritto;
- [ ] **registro delle decisioni** compilato, con **due voci obbligatorie**: l'allineamento allo standard aperto
      OWASP invece di uno schema proprio, e la distinzione fra natura e classe di effetto;
- [ ] contratto degli **strumenti conversazionali**: nessuno; la distinzione fra schema ricevuto e strumenti
      esposti è dichiarata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] la definizione OpenAPI del servizio pubblica lo schema ed è aggiornata nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Lo schema descrive ciò che finisce nella catena: la forma canonica su cui si calcola l'impronta dipende da questi campi |
| Standard OWASP «Agent Observability Standard» | È la grammatica a cui ci si allinea. Non è un vincolo di piattaforma: è una scelta di progetto, e come tale va nel registro delle decisioni |

## 7. Fuori ambito

- la rotta che riceve le dichiarazioni: storia 0008;
- il trattamento dei parametri, che è il punto delicato: storie 0009 e 0010;
- il collegamento fra un'azione e il nulla osta che la precede: storia 0020, che aggiunge il riferimento
  all'altro capo;
- l'esportazione nello schema normalizzato per i sistemi di sicurezza del cliente: storia 0027.

## 8. Punti aperti

- **Fino a che punto seguire lo standard.** AOS è giovane e in evoluzione: allinearsi ai nomi degli eventi è
  sicuro, ricalcarne ogni dettaglio rischia di inseguire un bersaglio mobile. Propongo di adottarne la struttura e
  i nomi principali e di dichiarare gli scostamenti, invece di rincorrere ogni revisione. Chi chiude: sviluppatore.
- **La motivazione della scelta dello strumento.** Lo standard prevede che l'agente dichiari **perché** ha scelto
  quello strumento. È l'informazione più utile per capire cosa è successo, ed è anche testo libero scritto da un
  modello linguistico: quindi un potenziale ingresso non presidiato di dati personali (§6.3 della descrizione).
  Propongo di accettarla come campo facoltativo soggetto alla stessa redazione dei parametri (storia 0010). Da
  confermare.
- **Come si dichiara «cosa è stato letto»** senza costringere il cliente a un lavoro sproporzionato. Un elenco di
  identificativi di risorse è realistico; un elenco esatto di righe lette non lo è. Propongo il primo, con la
  consapevolezza che è un'approssimazione — e va detto al cliente, non nascosto.
