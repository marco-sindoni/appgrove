# QuoteGrove — estensioni della console di amministrazione

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

Tre cose che la console comune non può dare. La prima: QuoteGrove **manda messaggi a persone fuori dagli account
nostri** (invii e promemoria), quindi quando un cliente dice «il mio preventivo non è arrivato» la domanda è sul
recapito, e la console oggi non sa nulla di recapiti. La seconda: la quota è **a consumo mensile** e un cliente che
migra il primo mese può superare il tetto per un motivo legittimo e irripetibile — serve una deroga a tempo, che
non è uno sconto. La terza, la più seria: l'app espone **collegamenti pubblici** verso l'esterno, e se uno di essi
finisce dove non deve (inoltrato per sbaglio, pubblicato su un gruppo) deve poter essere revocato subito, anche
mentre il cliente non è raggiungibile.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `durata_massima_collegamento_pubblico` | il tetto massimo alla scadenza del collegamento riservato che il cliente può impostare | 90 giorni | amministratore di piattaforma | è un limite di sicurezza della piattaforma, non una preferenza: se lo governasse il cliente potrebbe metterlo a dieci anni |
| `tetto_rigido_promemoria` | il numero massimo assoluto di solleciti per preventivo, oltre il quale nessuna configurazione del cliente può andare | 3 | amministratore di piattaforma | protegge i destinatari e la reputazione di appgrove come mittente: non può dipendere da chi ha interesse a insistere |
| `sospensione_invii` | interruttore che ferma invii e promemoria di un singolo account | spento | amministratore di piattaforma | serve solo in caso di abuso segnalato o di reputazione del canale di posta a rischio; è una misura di emergenza, non una funzione |

Nessun altro parametro per account: la configurazione dell'app — modelli, listini, soglie di sconto, sequenza dei
promemoria — è interamente nelle mani del cliente.

## 3. Quote e deroghe

- **Metrica governata**: `preventivi_inviati` (natura `flow`, finestra mensile).
- **Serve una deroga manuale?** **Sì.** Il caso reale è la migrazione: un cliente che passa a QuoteGrove da un
  altro strumento manda nel primo mese anche i preventivi arretrati, e supera il tetto per un motivo che non si
  ripeterà. Bloccarlo nel mese in cui sta decidendo se restare è il modo migliore per perderlo. Secondo caso: un
  guasto nostro che ha consumato quota per invii mai recapitati (storia `0017` rilascia la quota, ma un difetto è
  sempre possibile).
- **Forma della deroga**: tetto alternativo **con data di scadenza obbligatoria** (proposta: non oltre due
  finestre). Alla scadenza il tetto torna quello del piano, senza bisogno di ricordarsene.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, con quale tetto e **perché** —
  motivo scritto obbligatorio.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha stabilmente bisogno di più,
  passa di piano; una deroga rinnovata tre volte è un errore di piano travestito, e la console deve renderlo
  visibile.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Stato dei recapiti** | per account: invii e promemoria degli ultimi trenta giorni, con esito (recapitato, respinto, rifiutato dal destinatario), codice di errore del fornitore e momento | «Perché il mio cliente dice che non ha ricevuto il preventivo?» | metadati: esito, momento, codice di errore, **indirizzo di posta mostrato in forma oscurata** (`m***@e***.it`); mai l'oggetto né il testo del messaggio |
| **Consumo della quota** | per account: consumo della finestra corrente e delle tre precedenti, deroghe attive e scadute | «È vicino al tetto? È il caso di suggerire un piano diverso?» | conteggi |
| **Coda dei promemoria** | numero di promemoria programmati, in arretrato e falliti, per account e complessivi | «C'è un accumulo? La lavorazione periodica è ferma?» | conteggi e stati |
| **Collegamenti pubblici attivi** | per account: quanti collegamenti sono validi, quanti scaduti, quanti revocati, e la data di scadenza più lontana | «Ci sono collegamenti con scadenza anomala?» | conteggi e date; **nessun gettone in chiaro**, nemmeno all'amministratore |
| **Salute della lavorazione periodica** | ultima esecuzione delle scadenze (storia `0021`) e dei promemoria (storia `0022`), con durata ed esito | «Perché i preventivi non passano in scaduto?» | metadati tecnici |

**Divieto di impersonificazione.** Nessuna di queste viste mostra il contenuto di un preventivo, il nome di un
destinatario in chiaro, i prezzi o i testi. Se durante l'assistenza servisse guardare un documento, la strada non
è entrare nell'account: è chiedere al cliente di esportarlo e allegarlo alla richiesta di assistenza.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Concedere una deroga di quota** | migrazione iniziale, guasto nostro | sì (si revoca) | operatore, motivo, tetto, scadenza | trasformare un problema di piano in una consuetudine: la vista del §4 lo rende visibile |
| **Revocare un collegamento pubblico** | il cliente segnala che il collegamento è finito dove non doveva | **no** (il collegamento non torna valido: se ne genera uno nuovo dall'app) | operatore, motivo, preventivo, momento | revocare quello sbagliato interrompe una trattativa: richiede conferma esplicita e il numero del preventivo indicato dal cliente |
| **Ripetere un recapito fallito** | il fornitore di posta ha respinto per un guasto temporaneo | sì | operatore, motivo, invio | doppio recapito se non è idempotente: la ripetizione riusa lo stesso invio, non ne crea un altro, e **non consuma quota** |
| **Sospendere invii e promemoria di un account** | abuso segnalato, reputazione del canale a rischio | sì | operatore, motivo, momento | ferma il lavoro del cliente: è una misura di emergenza, mai una prassi, e va comunicata |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni **irreversibili** o con effetti verso
l'esterno richiedono una conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai contenuti
dell'account. In particolare: **nessuna azione della console invia un preventivo o un promemoria** — la console
può fermare e ripetere un recapito già disposto dal cliente, mai disporne uno nuovo.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| consumo di `preventivi_inviati` per account e finestra | metrica | no | diagnosi delle quote e proposta di piano |
| numero di preventivi per stato, per account | metrica | no | capire se l'app è usata o abbandonata |
| esito dei recapiti con codice di errore | metadato | no | diagnosi dei mancati arrivi |
| **indirizzo di posta del destinatario in forma oscurata** | metadato derivato | **sì, in forma ridotta** | senza almeno il dominio non si distingue «l'indirizzo era sbagliato» da «il loro servizio ci ha respinti» |
| conteggi dei collegamenti pubblici per stato | metrica | no | individuare configurazioni anomale |
| deroghe concesse, con operatore e motivo | metadato | no (identifica un nostro operatore, già coperto) | responsabilità delle decisioni di assistenza |

**Verifica obbligatoria.** L'indirizzo di posta oscurato è **un dato personale ridotto ma pur sempre un dato
personale**: va dichiarato nel manifesto dell'app come voce a sé, con finalità «assistenza tecnica sui mancati
recapiti» e la nota che l'accesso amministrativo è un trattamento come gli altri. Se lo sviluppatore preferisce
non esporlo affatto, la diagnosi resta possibile con il solo codice di errore del fornitore — perde precisione ma
non è cieca: è una scelta legittima, e va fatta consapevolmente.

## 7. Punti aperti

- **Quanto oscurare l'indirizzo di posta**, o se non mostrarlo affatto: c'è un compromesso reale fra
  minimizzazione dei dati e capacità di risolvere il problema del cliente. Lo chiude lo sviluppatore insieme alla
  compilazione del manifesto (storia `0007`).
- **Il valore del tetto rigido dei promemoria** (proposta: 3) non è un dato rilevato: nessuna fonte consultata
  indica un riferimento per il segmento. Lo chiude lo sviluppatore; l'esistenza del tetto, invece, non è
  negoziabile.
- **La revoca di un collegamento pubblico è un'azione irreversibile su un rapporto commerciale altrui**: qui è
  proposta perché il rischio opposto (un collegamento che resta valido dopo una fuga) è peggiore. Se lo
  sviluppatore preferisce che la revoca resti solo nelle mani del cliente, va detto — e allora serve che l'app la
  renda molto facile da trovare.
