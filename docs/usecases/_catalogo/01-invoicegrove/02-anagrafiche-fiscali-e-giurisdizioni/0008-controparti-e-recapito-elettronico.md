# 0008 — Controparti e recapito elettronico

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 02 — Anagrafiche fiscali e giurisdizioni
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo
> voglio registrare i miei clienti indicando **dove** vanno consegnate le loro fatture elettroniche
> così da non dover ricordare che al cliente belga serve un identificativo di rete e a quello italiano un codice
> di sette caratteri.

**Contesto.** Il recapito elettronico è il campo che manda a monte più fatture di qualunque altro: in Italia uno
scarto per codice destinatario errato è fra i più frequenti, e il messaggio che l'autorità restituisce è un codice
numerico. Le guide per il segmento micro dicono che l'aspettativa dichiarata è «semplicità al minor costo»
(descrizione dell'applicazione §2.5): tradotto, nessuno vuole imparare la parola «Peppol». L'interfaccia deve
chiedere **a chi mandi e in che paese**, e dedurre il resto.

## 2. Requisiti funzionali

1. **RF-1** — Si può creare, modificare e disattivare una controparte con: denominazione, identificativo IVA o
   codice fiscale, indirizzo completo, paese, tipo (impresa, professionista, consumatore, pubblica
   amministrazione).
2. **RF-2** — In base al **paese** e al tipo, l'app chiede il recapito elettronico giusto e uno solo: codice
   destinatario o indirizzo di posta certificata per l'Italia, identificativo di rete a quattro angoli per il
   Belgio e i paesi della rete, identificativo fiscale per la Polonia.
3. **RF-3** — Il formato del recapito è validato secondo la regola del paese; un formato non valido è rifiutato
   con un messaggio che dice cosa ci si aspettava e dove trovarlo.
4. **RF-4** — Per l'Italia, se il cliente è un **consumatore** senza codice destinatario né posta certificata,
   l'app accetta il recapito convenzionale di «messa a disposizione» e lo spiega in una riga.
5. **RF-5** — L'elenco delle controparti si cerca a testo libero su denominazione e identificativo fiscale, si
   filtra per paese, ed è paginato.
6. **RF-6** — Una controparte citata in un documento già trasmesso non si cancella: si disattiva.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `counterparty` filtra per `tenant_id` preso
  dal token verificato; un `tenant_id` forzato dall'esterno viene ignorato. Prova di isolamento su due account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/einvoicing/v1/counterparties` e
  `GET|PUT|DELETE /api/einvoicing/v1/counterparties/{id}`; corpo validato; errori in `application/problem+json`;
  paginazione con totale; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V4__counterparty_routing.sql` sullo schema `app_einvoicing`: la tabella
  `counterparty` si arricchisce dei campi di recapito, con indice su `(tenant_id, identificativo_fiscale)` per la
  ricerca. Colonne di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Controparti» del modulo `einvoicing`: elenco con ricerca e filtro
  per paese, scheda di dettaglio, modulo di inserimento con i campi che **cambiano in base al paese scelto**.
  Solo token del sistema di design, tema chiaro e scuro, moduli con React Hook Form e Zod.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `einvoicing` e sono presenti in
  `en, it, fr, es, de`, compresi i nomi dei campi di recapito, che sono diversi per paese.
- **RT-6 — Varchi e quota (§6, §7).** La controparte **non** consuma la metrica `documenti`. Ruolo `member`
  sufficiente per creare e modificare: è lavoro quotidiano, non configurazione.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia; la ricerca delle controparti
  sarà esposta come strumento di **lettura** nella storia `0027`.
- **RT-8 — Dati personali (§10).** **Sì, ed è il nucleo dei dati personali dell'app.** Voci nuove nel manifesto in
  italiano e inglese per `counterparty.denominazione`, `counterparty.codice_fiscale`, `counterparty.indirizzo` e
  `counterparty.recapito_elettronico`; campi annotati `@PersonalData`; tabella presente in `exportData` e
  `purgeData`. Base giuridica: obbligo di legge per i campi che sono contenuto obbligatorio della fattura,
  esecuzione del contratto per il recapito.
- **RT-9 — Registrazione eventi (§14).** Gli eventi sulla controparte sono registrati con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, **senza** denominazione, indirizzo o recapito: solo identificativi.

## 4. Criteri di accettazione

**CA-1 — Controparte italiana con codice destinatario**
- **Dato** un utente su un account abilitato
- **Quando** crea una controparte con paese Italia, tipo impresa e un codice destinatario di sette caratteri
  valido
- **Allora** la controparte è creata e la scheda mostra il recapito con l'etichetta italiana

**CA-2 — Il modulo cambia col paese**
- **Dato** il modulo di inserimento aperto
- **Quando** si sceglie il paese Belgio
- **Allora** il campo del codice destinatario scompare e compare quello dell'identificativo di rete, con la
  spiegazione di dove il cliente lo trova

**CA-3 — Recapito in formato sbagliato**
- **Dato** paese Italia e tipo impresa
- **Quando** si inserisce un codice destinatario di cinque caratteri
- **Allora** si riceve `400` con un messaggio che indica il formato atteso, e nulla viene creato

**CA-4 — Consumatore italiano senza recapito**
- **Dato** paese Italia e tipo consumatore
- **Quando** non si indica né codice destinatario né posta certificata
- **Allora** la controparte è creata con il recapito convenzionale di messa a disposizione, e l'app spiega in una
  riga cosa significa per il cliente finale

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie controparti
- **Quando** un utente di `A` cerca una controparte per identificativo fiscale che esiste solo in `B`
- **Allora** non la trova, anche forzando l'identificativo dell'account nella richiesta

**CA-6 — Non si cancella ciò che è citato**
- **Dato** una controparte citata in un documento già trasmesso
- **Quando** si tenta di cancellarla
- **Allora** l'operazione è rifiutata con una spiegazione, e resta disponibile la disattivazione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulle regole di formato del recapito per paese e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** e matrice dei ruoli;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-EINVOICING]` (storia `0030`) attraverserà la creazione
      della controparte;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati, tabella in esportazione e
      cancellazione;
- [ ] controllo automatico di **accessibilità** sull'elenco e sul modulo di inserimento;
- [ ] **registro delle decisioni** compilato, con la scelta «un solo recapito per controparte» motivata;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, previsto in `0027`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0007` | Il paese del soggetto emittente determina quali recapiti hanno senso proporre |
| `0006` | Le regole di formato del recapito appartengono al profilo del paese, che vive nel registro delle giurisdizioni |

## 7. Fuori ambito

- La **verifica di esistenza** del recapito presso i registri della rete: storia `0009`. Qui si valida la forma.
- L'importazione in blocco delle controparti da un file: rimandata; ne è proprietaria la storia `0013`, che porta
  l'importazione.
- La sincronizzazione dell'anagrafica con l'app di fatturazione o con il CRM: **fuori ambito di questa app**. Il
  catalogo (§6) individua l'anagrafica clienti come entità condivisa della suite, ma la condivisione è un tema di
  piattaforma, non di InvoiceGrove: annotata nella descrizione dell'applicazione §10.

## 8. Punti aperti

- **Una controparte, un recapito**: è la scelta di questa storia perché semplifica moltissimo l'interfaccia, ma
  un'impresa può avere recapiti diversi per sede. Se emergesse il bisogno, è una storia nuova, non una modifica di
  questa.
- La classificazione dei dati personali delle controparti è una **proposta** (descrizione dell'applicazione §6) e
  va confermata dallo sviluppatore prima di scrivere il manifesto in produzione.
