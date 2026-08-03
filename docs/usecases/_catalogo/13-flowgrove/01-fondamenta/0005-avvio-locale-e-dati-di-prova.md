# 0005 — Avvio locale e dati di prova

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende il lavoro su FlowGrove
> voglio che `./app-start.sh` avvii l'app con dentro un account di prova già popolato
> così da vedere in trenta secondi come si comporta l'app piena, senza inventarmi i dati ogni volta.

**Contesto.** Un'app che parte vuota si prova male: le storie successive parlano di consuntivi, sforamenti e
margini, che su zero righe non si vedono. Servono dati di prova **inventati e deterministici**: inventati perché
è vietato usare dati veri di chiunque, deterministici perché le prove end-to-end devono poter contare su numeri
prevedibili ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §11).

## 2. Requisiti funzionali

1. **RF-1** — Esiste un insieme di dati di prova, applicabile solo in locale, che crea due account distinti con
   dati diversi, così che l'isolamento sia visibile a occhio.
2. **RF-2** — L'account principale contiene tre progetti in stati diversi (uno appena aperto, uno a metà con
   budget quasi esaurito, uno chiuso e già fatturato), quindici attività, quattro persone e circa duecento righe
   di ore distribuite su due mesi.
3. **RF-3** — I dati sono **inventati** e riconoscibili come tali: aziende «Rossi Impianti», «Studio Verdi»,
   indirizzi nel dominio `*.test`, importi tondi. Nessun dato che possa somigliare a quello di una persona reale.
4. **RF-4** — L'insieme è **deterministico**: eseguito due volte produce gli stessi identificativi, le stesse date
   relative e gli stessi totali.
5. **RF-5** — L'app si avvia con `./app-start.sh` senza modifiche manuali agli script, e `./dev.sh services`
   mostra `progetti` con porta `8113` e schema `app_progetti`.
6. **RF-6** — L'insieme di prova **non** viene applicato negli ambienti diversi da quello locale, e il tentativo
   di farlo fallisce in modo esplicito.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I due account di prova servono proprio a dimostrarlo: le prove di
  isolamento li usano come base.
- **RT-2 — Avvio locale (§15).** La scoperta automatica ricava la mappa dal solo `application.properties`;
  nessuna riga incollata in `app-start.sh`, nel proxy locale o negli avvii di collaudo. Se viene voglia di
  modificare a mano uno script, è un difetto della scoperta automatica.
- **RT-3 — Persistenza (§8).** I dati di prova si applicano come passo separato (non come migrazione Flyway), così
  che lo schema resti pulito negli ambienti veri.
- **RT-4 — Dati personali (§10).** I nomi delle persone dei dati di prova sono inventati; il manifesto non cambia,
  ma la storia verifica che l'esportazione e la cancellazione (storia 0030) abbiano su cosa lavorare.
- **RT-5 — Registrazione eventi (§14).** L'applicazione dei dati di prova è registrata con l'ambiente e il numero
  di righe create.

## 4. Criteri di accettazione

**CA-1 — Avvio pulito**
- **Dato** un ambiente locale appena preparato
- **Quando** si esegue `./app-start.sh` e poi `dev migrate`
- **Allora** FlowGrove è raggiungibile dal backoffice, con l'account di prova già popolato

**CA-2 — Determinismo**
- **Dato** l'insieme di dati di prova
- **Quando** lo si applica due volte su un database pulito
- **Allora** i totali delle ore, i budget e gli stati dei progetti sono identici

**CA-3 — Isolamento visibile**
- **Dato** i due account di prova
- **Quando** si entra con l'utente del secondo
- **Allora** non si vede nessun progetto del primo

**CA-4 — Rifiuto fuori dal locale**
- **Dato** un ambiente diverso da quello locale
- **Quando** si tenta di applicare i dati di prova
- **Allora** l'operazione fallisce con un messaggio esplicito e nulla viene scritto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `smoke`);
- [ ] prova di **integrazione** che applica i dati di prova su database effimero e ne verifica i totali;
- [ ] prova di **isolamento fra account** basata sui due account di prova;
- [ ] **prova end-to-end**: rimando alla storia 0031, che userà proprio questi dati come base;
- [ ] **traduzioni**: non applicabile (nessun testo visibile nuovo);
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la composizione dell'insieme di prova e il perché di
      quei tre progetti;
- [ ] `./dev.sh services` mostra l'app e `./app-start.sh` la avvia senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0001`-`0004` | I dati di prova devono poter popolare tabelle che esistono e rispettare i tetti dei posti |

## 7. Fuori ambito

- l'importazione di dati veri del cliente da un altro strumento: non è prevista in questa stesura ed è un punto
  che andrà valutato quando l'app avrà clienti da migrare;
- gli scenari di prova delle singole funzioni: ogni storia porta i propri.

## 8. Punti aperti

- Nessuno.
