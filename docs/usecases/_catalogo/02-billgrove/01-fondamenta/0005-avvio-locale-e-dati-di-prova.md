# 0005 — Avvio locale e dati di prova

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che apre il repository per la prima volta
> voglio avviare BillGrove in locale e trovarci dentro dei dati sensati
> così da poter vedere l'app funzionare in un minuto, invece di passare mezza giornata a creare a mano un cliente,
> un prodotto e una fattura per poter provare qualsiasi cosa.

**Contesto.** La regola di piattaforma è netta: una app nuova deve essere eseguibile in locale **subito dopo
l'unione del ramo**, senza passi manuali, e la scoperta automatica dei servizi ricava tutto dal solo file delle
proprietà. Questa storia chiude l'epica delle fondamenta verificando che quella promessa sia vera, e aggiunge il
minimo indispensabile per lavorare: dati di prova inventati su **due** account, così che ogni prova di isolamento
abbia qualcosa da confrontare.

## 2. Requisiti funzionali

1. **RF-1** — `./app-start.sh` avvia BillGrove insieme agli altri servizi, senza alcuna modifica manuale agli
   script.
2. **RF-2** — `./dev.sh services` mostra `billing` con la sua porta e il suo schema.
3. **RF-3** — `dev migrate` applica le migrazioni dello schema `app_billing`.
4. **RF-4** — Esiste un insieme di dati di prova **inventati** per due account distinti: clienti, voci di catalogo e
   qualche documento in stati diversi.
5. **RF-5** — I dati di prova non contengono dati veri di nessuno: nomi di fantasia, indirizzi di posta su domini
   `*.test`, partite IVA palesemente inventate.
6. **RF-6** — L'avvio in profilo di spedizione è verificato dallo strumento di collaudo comune.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I dati di prova sono divisi fra due `tenant_id` diversi: è la base di
  tutte le prove di isolamento delle storie successive.
- **RT-3 — Persistenza (§8).** I dati di prova si caricano con un meccanismo attivo **solo** nei profili locale e
  di prova, mai in produzione; non sono una migrazione Flyway di schema.
- **RT-11 — Avvio locale (§15).** La mappa servizio → identificativo app → porta → schema discende dal solo
  `services/billing/src/main/resources/application.properties`. **Se viene voglia di modificare a mano uno script di
  avvio, è un difetto della scoperta automatica, non un passo del lavoro.**
- **RT-4 — Modulo frontend (§3).** Il modulo è abilitato nello stub locale dell'abilitazione, finché quella reale
  non esiste.
- **RT-8 — Dati personali (§10).** I dati di prova sono inventati e non sono dati personali di nessuno: va detto
  esplicitamente nel registro delle decisioni, perché è la classe di errore più insidiosa (un dato «realistico»
  copiato da un cliente vero resta un dato personale).
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo.

## 4. Criteri di accettazione

**CA-1 — Avvio senza passi manuali**
- **Dato** un clone pulito del repository
- **Quando** si eseguono `./app-start.sh` e poi `./dev.sh services`
- **Allora** `billing` risulta avviato sulla porta `8102` con schema `app_billing`, e nessuno script è stato
  modificato a mano

**CA-2 — Dati di prova presenti**
- **Dato** lo stack locale avviato con il profilo di sviluppo
- **Quando** si accede con l'utente di prova del primo account
- **Allora** si vedono almeno tre clienti, tre voci di catalogo e quattro documenti in stati diversi

**CA-3 — Due account distinti**
- **Dato** i dati di prova caricati
- **Quando** si accede con l'utente del secondo account
- **Allora** si vedono dati diversi, e nessun dato del primo account

**CA-4 — Nessun dato di prova in produzione**
- **Dato** il profilo di spedizione · **Quando** il servizio si avvia
- **Allora** nessun dato di prova viene caricato, e l'avvio dell'artefatto reale è verificato dallo strumento di
  collaudo comune

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `smoke`, `tooling`; l'intera suite prima del commit);
- [ ] prove di **integrazione** che verificano il caricamento dei dati di prova nei soli profili previsti;
- [ ] prova di **isolamento fra account** che usa i due account dei dati di prova;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-BILLING]` usa questi dati come punto di partenza
      deterministico; registro di copertura aggiornato;
- [ ] **traduzioni**: non applicabile;
- [ ] **manifesto dei dati**: nessuna modifica; dichiarato che i dati di prova sono inventati;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: nessuno qui, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] `run-tests.sh` aggiornato se l'area di collaudo cambia.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0001`-`0004` | I dati di prova hanno senso solo se ci sono tabelle, modulo e quota |

## 7. Fuori ambito

- l'importazione di dati veri da un altro prodotto: storia `0009`;
- la generazione di volumi grandi per prove di carico: non prevista in questa stesura.

## 8. Punti aperti

Nessuno.
