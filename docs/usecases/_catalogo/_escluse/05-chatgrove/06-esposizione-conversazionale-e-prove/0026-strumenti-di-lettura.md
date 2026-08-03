# 0026 — Strumenti di lettura

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0017`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio poter chiedere a voce «quanti ordini non pagati ho?» e «chi mi ha scritto e non ho ancora risposto?»
> così da sapere come va senza attraversare quattro schermate mentre servo un cliente.

**Contesto.** Il requisito trasversale del catalogo chiede che ogni funzione sia comandabile da una chat. Il
livello conversazionale **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066,
scritta e non implementata): questa storia non lo costruisce. Dichiara il **contratto degli strumenti di
lettura** — nome stabile, descrizione in lingua naturale, schema dei parametri e del risultato — e lo tiene
dentro il servizio dell'app, versionato con essa. Si comincia dalla lettura perché è libera: nessun effetto,
nessuna conferma.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara gli strumenti di lettura: `elenca_conversazioni`, `leggi_conversazione`,
   `cerca_prodotto`, `elenca_ordini`, `elenca_carrelli_abbandonati`, `riepiloga_contatto`.
2. **RF-2** — Ogni strumento ha un nome stabile, una descrizione in lingua naturale, lo schema dei parametri e
   lo schema del risultato, ed è marcato **lettura** e idempotente.
3. **RF-3** — I risultati sono **minimizzati**: si restituisce ciò che serve a rispondere, non tutto il record.
   `riepiloga_contatto` non restituisce l'elenco dei messaggi; `elenca_conversazioni` restituisce l'ultimo
   messaggio troncato, non il filo.
4. **RF-4** — Ogni strumento è paginato e ha un limite massimo di elementi restituiti, così che una domanda
   generica non scarichi un archivio.
5. **RF-5** — Il contratto è esposto in una forma leggibile da un programma e verificato da una prova: se un
   parametro cambia senza che il contratto lo dica, la prova diventa rossa.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni strumento riceve il `tenant_id` dal token verificato della
  sessione dell'assistente, **mai** come parametro: un parametro `tenant_id` non esiste nello schema, e se
  arrivasse verrebbe ignorato.
- **RT-2 — Esposizione conversazionale (§12).** Gli strumenti sono dichiarati dentro il servizio dell'app;
  il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063). La storia non costruisce
  il server: dichiara il contratto e lo rende verificabile.
- **RT-3 — Varchi (§6).** Ogni strumento attraversa la stessa catena di varchi delle rotte corrispondenti:
  utente autenticato, app accesa, account abilitato, ruolo sufficiente. La lettura non consuma quota.
- **RT-4 — Dati personali (§10).** Il principio di minimizzazione **è** il requisito principale: uno strumento
  che restituisce più dati personali del necessario è un difetto, non una comodità. Nessuna voce nuova nel
  manifesto; va invece verificato che nessuno strumento esponga campi non dichiarati.
- **RT-5 — Registrazione eventi (§14).** Ogni chiamata a uno strumento registra nome dello strumento,
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** parametri che possano contenere
  dati personali (per esempio il testo cercato).
- **RT-6 — Prove (§11).** Prove di unità sullo schema di ogni strumento e di integrazione su almeno due, con
  due account, per verificare che il risultato sia filtrato.

## 4. Criteri di accettazione

**CA-1 — Contratto completo**
- **Dato** il servizio avviato
- **Quando** si legge il contratto degli strumenti
- **Allora** ci sono sei strumenti di lettura, ciascuno con nome, descrizione, schema dei parametri e del
  risultato, e la marcatura «lettura»

**CA-2 — Risultato minimizzato**
- **Dato** un contatto con 400 messaggi
- **Quando** si chiama `riepiloga_contatto`
- **Allora** il risultato contiene conteggi, storico degli ordini e stato del consenso, **non** i messaggi

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** uno strumento viene chiamato nella sessione di `A` indicando l'identificativo di una conversazione
  di `B`
- **Allora** il risultato è vuoto o `404`: mai i dati di `B`

**CA-4 — Limite di pagina**
- **Dato** un account con 5.000 contatti · **Quando** si chiama uno strumento senza limite
- **Allora** il risultato è troncato al massimo previsto e indica che ci sono altri elementi

**CA-5 — Contratto verificato**
- **Dato** una modifica allo schema dei parametri di uno strumento senza aggiornarne la dichiarazione
- **Quando** si eseguono le prove · **Allora** la suite diventa rossa

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend; l'intera suite prima del commit);
- [ ] prove di **unità** sugli schemi e di **integrazione** sul filtro per account;
- [ ] prova di **isolamento fra account** su ogni strumento che legge dati di clienti;
- [ ] **prova end-to-end**: *nessun impatto* — gli strumenti non hanno superficie utente finché il livello
      conversazionale non esiste; dichiarato nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: le descrizioni degli strumenti sono in lingua naturale; la lingua di dichiarazione va
      decisa una volta per tutta la piattaforma (vedi punti aperti);
- [ ] **manifesto dei dati**: verificato che nessuno strumento esponga campi non dichiarati;
- [ ] **registro delle decisioni** compilato, con l'elenco degli strumenti e la regola di minimizzazione;
- [ ] contratto degli **strumenti conversazionali** dichiarato — **è l'oggetto della storia**;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0014`, `0017`, `0021` | Gli strumenti leggono conversazioni, prodotti, ordini e contatti: devono esistere |
| UC 0061-0063 (livello conversazionale di piattaforma) | Non implementati: qui si dichiara il contratto, il server arriverà dopo. Nel frattempo il contratto è verificato dalle prove, non da un client vero |

## 7. Fuori ambito

- gli strumenti che scrivono: storia `0027`;
- il server conversazionale, l'autenticazione delegata e il consenso: sono di piattaforma (UC 0061-0062);
- l'orchestrazione fra più app: è il valore della suite, non di questa app.

## 8. Punti aperti

- **In quale lingua si scrivono le descrizioni degli strumenti.** Sono testo letto da un modello, non
  dall'utente: la scelta (inglese come lingua tecnica, oppure la lingua dell'utente) è una decisione di
  piattaforma che riguarda tutte le sessanta app, non questa. Va posta nell'epica `12-ready-for-ai-mcp`.
