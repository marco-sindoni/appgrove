# 0005 — Avvio locale e dati di prova

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi sviluppa o collauda il verticale
> voglio uno stack locale che parta con dentro un salone finto già configurato
> così da poter provare un colore con la posa, uno scarico di cabina e una provvigione senza passare mezz'ora a
> inventare dati.

**Contesto.** Le epiche di dominio hanno bisogno di uno stato di partenza ricco: un servizio a fasi, due
operatori con percentuali diverse, prodotti in cabina con giacenze, un pacchetto già venduto e mezzo consumato. Se
questo stato non c'è, ogni storia se lo costruisce da sé in modo diverso e i percorsi end-to-end diventano
irriproducibili. I dati sono **inventati**, e non «realistici al punto da sembrare veri»: indirizzi su dominio
`*.test`, nomi palesemente di fantasia.

## 2. Requisiti funzionali

1. **RF-1** — Un comando riempie l'ambiente locale con un salone di prova: tre postazioni, tre operatori, un
   listino di servizi di cui almeno uno **a fasi**, prodotti in cabina e in rivendita con giacenze, clienti,
   appuntamenti passati e futuri.
2. **RF-2** — Il salone di prova contiene almeno: un **pacchetto** a sedute determinate mezzo consumato, un
   pacchetto a valore, una **tessera fedeltà** con punti, due **regole di provvigione** diverse e un mese di conti
   chiusi da cui il prospetto delle provvigioni si possa calcolare.
3. **RF-3** — Il riempimento è **idempotente**: eseguirlo due volte non raddoppia niente.
4. **RF-4** — I dati esistono in **due account distinti**, così che le prove di isolamento abbiano subito un
   secondo account da usare.
5. **RF-5** — `./dev.sh services` mostra il verticale e `./app-start.sh` lo avvia senza modifiche manuali agli
   script.

## 3. Requisiti tecnici

- **RT-1 — Avvio locale automatico (§15).** La mappa servizio → identificativo → porta → schema discende dal solo
  `application.properties`. Nessuna riga incollata negli script: se venisse la tentazione, è un difetto della
  scoperta automatica, non un passo del lavoro.
- **RT-2 — Isolamento fra account (§1).** I due account di prova sono realmente separati e i dati dell'uno non
  sono raggiungibili dall'altro.
- **RT-3 — Dati di prova inventati (§11).** Nessun dato reale, nessun indirizzo esistente; indirizzi di posta su
  dominio `*.test`; **nessuna informazione sulla salute** nemmeno per finta, perché un dato di prova sbagliato
  diventa un esempio da copiare (§6 della descrizione).
- **RT-4 — Dati personali (§10).** Nessuna voce nuova nel manifesto: i dati di prova non sono un trattamento, ma
  vanno comunque generati in modo che non assomiglino a persone vere.
- **RT-5 — Registrazione eventi (§14).** Il riempimento registra quante entità ha creato, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — Da zero a salone in un comando**
- **Dato** uno stack locale appena avviato e vuoto
- **Quando** si esegue il comando di riempimento
- **Allora** entrando nel backoffice si vede un salone con tre postazioni, servizi, prodotti, clienti e
  appuntamenti

**CA-2 — Ripetibile**
- **Dato** l'ambiente già riempito
- **Quando** si esegue di nuovo il comando
- **Allora** i conteggi restano quelli, non raddoppiano

**CA-3 — Due account**
- **Dato** l'ambiente riempito
- **Quando** si accede con l'utente del secondo account
- **Allora** si vede un salone diverso, e nessun dato del primo

**CA-4 — Materia prima per le epiche successive**
- **Dato** l'ambiente riempito
- **Quando** si apre la sezione Provvigioni per il mese passato
- **Allora** c'è già qualcosa da calcolare, perché ci sono conti chiusi e regole valide

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite intera: la storia tocca l'avvio locale e gli strumenti);
- [ ] prova di **integrazione** che esegue il riempimento due volte e verifica l'idempotenza;
- [ ] prova di **isolamento fra account** fra i due account di prova;
- [ ] **prova end-to-end**: *rimando* — il salone di prova è lo stato di partenza dei percorsi `[J-SALONGROVE]` e
      `[J-SALONGROVE-PKG]` (storie `0030` e `0031`);
- [ ] **traduzioni**: non applicabile, i dati di prova non sono interfaccia;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni**: contenuto del salone di prova e motivo di ciascun elemento;
- [ ] `./dev.sh services` mostra l'app e l'avvio locale funziona senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0002`, `0003`, `0004` | servono tabelle, modulo e piano per avere qualcosa da riempire e da mostrare |

## 7. Fuori ambito

- i dati di prova dell'agenda (servizi, risorse, orari, clienti): sono di BookGrove, storia `0005` di quell'app.
  Sotto la via (b) si estendono; sotto la via (a) andrebbero rifatti;
- gli scenari specifici di ciascuna prova end-to-end: li costruisce la storia che li usa.

## 8. Punti aperti

Nessuno.
