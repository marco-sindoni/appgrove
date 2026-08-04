# 0014 — Verifica dell'integrità

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 03 — Prova di inalterabilità
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che deve rispondere di ciò che è successo
> voglio poter chiedere in qualunque momento se il registro di un certo periodo è ancora quello di allora
> così da sapere se posso appoggiarmi a quelle righe prima di mostrarle a qualcuno, e da accorgermi subito se
> qualcosa non torna.

**Contesto.** Fino a questa storia l'integrità è una proprietà che esiste ma che nessuno può interrogare: la
funzione di ricalcolo introdotta dalla 0002 è interna, e i sigilli della 0013 stanno in una tabella che nessuna
schermata mostra. Qui la verifica diventa una cosa che una persona può fare da sola, in trenta secondi, senza
chiedere niente a nessuno.

Il punto delicato è **cosa si risponde quando la verifica fallisce**. Un «non integra» secco è inutile: chi lo
legge non sa se ha perso tutto o quasi niente. La risposta utile è duplice — *dove* si rompe (la prima riga
divergente) e *fin dove* si può ancora stare tranquilli (il sigillo più recente che risulta ancora coerente). La
seconda metà è quella che permette di continuare a lavorare invece di buttare via il registro.

## 2. Requisiti funzionali

1. **RF-1** — Una persona dell'account può chiedere la verifica di un intervallo, indicato per date o per numeri
   di sequenza; l'intervallo predefinito è l'ultimo periodo coperto da almeno un sigillo.
2. **RF-2** — La verifica ricalcola la catena dell'intervallo e la confronta con **tutti** i sigilli che lo
   coprono, non solo con l'ultimo: un sigillo intermedio incoerente è un'informazione, non un dettaglio.
3. **RF-3** — L'esito è `integra` oppure `non integra`. Se `non integra`, la risposta indica la **prima** riga
   divergente (numero di sequenza, momento, tipo di divergenza: impronta del contenuto oppure impronta della
   precedente) e **il sigillo più recente che risulta ancora coerente**, con la sua data.
4. **RF-4** — La verifica è **accessibile anche a chi ha solo la lettura**: chi controlla non deve dipendere da
   chi amministra. È il caso d'uso del revisore (storia 0029).
5. **RF-5** — Esiste una schermata «Integrità» nel modulo che mostra l'elenco dei sigilli con il loro periodo e il
   loro esito di ultima verifica, e permette di lanciare una verifica su un intervallo scelto.
6. **RF-6** — Ogni verifica eseguita è a sua volta **una riga del registro**: chi l'ha chiesta, su quale
   intervallo, con quale esito. Una verifica che nessuno può dimostrare di aver fatto vale poco in una
   contestazione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La verifica opera solo sulla catena dell'account ricavato dal token
  verificato; un `tenant_id` o un intervallo di sequenze che puntassero a un altro account vengono ignorati e la
  risposta è la stessa che si darebbe per un intervallo vuoto — non si rivela l'esistenza di righe altrui.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/agentaudit/v1/integrity-checks` con corpo
  validato (intervallo per date o per sequenze, mutuamente esclusivi) e
  `GET /api/agentaudit/v1/integrity-checks` per lo storico; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__verifiche_integrita.sql` sullo schema `app_agentaudit`: tabella
  `integrity_checks` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo, intervallo, esito,
  prima sequenza divergente, sigillo coerente più recente. La verifica **non scrive nulla** sulla tabella delle
  azioni: legge e ricalcola.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Integrità» nel manifesto del modulo `agentaudit`; la
  schermata legge i dati con il client generato dalla definizione delle interfacce e non accede al token se non
  tramite il contesto della shell; usa solo i token del sistema di design; funziona in tema chiaro e scuro. **Il
  rosso e il verde della risposta sono colori funzionali del sistema di design**, non l'accento dell'app: è la
  ragione per cui l'app ha `violet` come colore-categoria (§3 della descrizione dell'applicazione).
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono
  presenti in `en, it, fr, es, de`. Attenzione particolare al testo dell'esito negativo: deve essere comprensibile
  senza gergo crittografico in tutte e cinque, e la storia non è conclusa se una traduzione dice qualcosa di
  diverso dalle altre.
- **RT-6 — Varchi e quota (§6, §7).** La verifica **non consuma** la metrica `actions`: far pagare la prova che la
  prova è valida sarebbe grottesco, e in più metterebbe un incentivo a non verificare (§3 della descrizione
  dell'applicazione). Restano i varchi precedenti: `401` senza token, `402` senza abbonamento attivo, `403` per
  app spenta. Con abbonamento `past_due` la verifica resta accessibile; con `canceled` risponde `402`, ma
  l'esportazione dei dati resta accessibile in ogni caso.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `verifica_integrita(periodo?) → esito della
  verifica`, marcato **lettura**, nessuna conferma umana. È lo strumento più naturale da chiedere a voce
  («il registro di marzo è a posto?»). Il contratto vive dentro il servizio; il server conversazionale è di
  piattaforma e non ancora implementato (UC 0061-0063); la dichiarazione completa è la storia 0034.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo nel risultato**: l'esito parla di sequenze, date
  e impronte. La riga che registra *chi ha chiesto la verifica* usa l'identificativo dell'utente, già dichiarato
  nel manifesto per le altre righe di registro; non si aggiungono nomi né indirizzi.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `verifica richiesta`, `verifica conclusa integra` e
  soprattutto `verifica conclusa non integra` sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza dati personali. Il terzo merita una soglia di allarme lato piattaforma:
  è il genere di evento che non deve passare inosservato.

## 4. Criteri di accettazione

**CA-1 — Verifica di un intervallo integro**
- **Dato** un account con quaranta azioni e tre sigilli giornalieri, mai alterati
- **Quando** una persona chiede la verifica dell'intero periodo
- **Allora** l'esito è `integra`, sono elencati i tre sigilli confrontati, e nel registro compare una riga che
  attesta la verifica con chi l'ha chiesta

**CA-2 — Verifica di un intervallo alterato**
- **Dato** lo stesso account, in cui il campo di una azione con sequenza 17 è stato alterato direttamente sulla
  base di dati
- **Quando** una persona chiede la verifica dell'intero periodo
- **Allora** l'esito è `non integra`, la prima riga divergente indicata è **la 17**, e viene indicato il sigillo
  più recente ancora coerente — cioè quello che copre fino alla sequenza 16 o precedente

**CA-3 — Chi ha solo la lettura può verificare**
- **Dato** un utente con il solo permesso di lettura sul registro
- **Quando** chiede una verifica
- **Allora** la verifica viene eseguita e l'esito restituito, senza che l'utente possa modificare regole, sorgenti
  o alcunché

**CA-4 — La verifica non consuma quota**
- **Dato** un account che ha già esaurito il tetto mensile di `actions`
- **Quando** chiede una verifica dell'integrità
- **Allora** la verifica viene eseguita normalmente e non riceve `429`, mentre l'accodamento di nuove azioni resta
  soggetto al comportamento definito dalla storia 0004

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con la propria catena e i propri sigilli
- **Quando** un utente di `A` chiede la verifica indicando sequenze che appartengono a `B`, anche forzando
  l'identificativo dell'altro account nella richiesta
- **Allora** ottiene la risposta di un intervallo vuoto e nessuna informazione sull'esistenza di quelle righe

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'individuazione della prima riga divergente e sulla scelta del sigillo coerente più
      recente, e di **integrazione** sulla rotta di verifica, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla verifica e sullo storico delle verifiche;
- [ ] **prova end-to-end**: risposta «coprire ora» — la schermata «Integrità» è superficie utente; il percorso
      `[J-AGENTAUDIT]` riceve il passo «apri Integrità, lancia una verifica, leggi l'esito», e il registro di
      copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) viene aggiornato di
      conseguenza. Il percorso completo dell'app resta di competenza della storia 0037;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), compreso il testo dell'esito
      negativo;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la voce obbligatoria sulla
      verifica che non consuma quota e sul perché;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `verifica_integrita`, marcato lettura;
- [ ] controllo automatico di **accessibilità** sulla schermata «Integrità»;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Serve la catena e la funzione interna di ricalcolo |
| storia `0013` | Senza sigilli la verifica confronta la catena solo con sé stessa, e quindi non dimostra granché |
| storia `0003` | Serve il guscio del modulo frontend per appendere la sezione «Integrità» |

## 7. Fuori ambito

- **il pacchetto scaricabile da far verificare a un terzo**: storia 0015. Qui la verifica la esegue il nostro
  codice, ed è utile per accorgersi; per dimostrare a qualcun altro serve la 0015;
- il recapito del sigillo al cliente: storia 0017;
- il ruolo di revisore in sola lettura come ruolo definito: storia 0029 — qui si usa il permesso di lettura già
  esistente;
- la verifica automatica periodica con allarme: deliberatamente rimandata, vedi punti aperti.

## 8. Punti aperti

- **Verifica automatica periodica.** Sarebbe naturale che l'app verificasse da sola ogni notte e avvisasse in caso
  di divergenza, invece di aspettare che qualcuno chieda. Non è in questa storia perché apre due domande che non
  spettano qui: quanto costa ricalcolare catene lunghe ogni notte, e a chi si manda l'allarme. Proprietaria
  naturale: storia 0026 (avvisi su comportamenti anomali). Chi chiude: sviluppatore.
- **Cosa fare davvero quando l'esito è «non integra».** Il prodotto lo segnala; non dice se sospendere le
  scritture, se avvisare la piattaforma, se congelare il periodo. È una procedura operativa e in parte una
  questione contrattuale con il cliente, non una funzione. Chi chiude: sviluppatore, insieme a chi presidia la
  sicurezza.
- **Costo del ricalcolo su intervalli molto lunghi.** Un account con due anni di conservazione e traffico intenso
  può avere milioni di righe: la verifica dell'intero storico non può essere una richiesta sincrona. Serve una
  soglia oltre la quale la verifica diventa una lavorazione differita con notifica. Chi chiude: sviluppatore.
