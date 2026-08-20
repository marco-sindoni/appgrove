# 22 — Rifacimento del modello di appartenenza (membership)

Epica evolutiva che rifà la gestione degli utenti della piattaforma: **elenco unico** delle persone,
**ruolo per applicazione**, **posti a pagamento** con listino a fasce. Analisi prodotta dalla change
`0087` su requisiti dettati dallo sviluppatore.

**Struttura di quest'area** — diversa dalle altre, per volontà dello sviluppatore:

| Cartella | Che cosa contiene |
|---|---|
| [epic/](epic/) | l'epica madre e le cinque sotto-epiche: visione, decisioni portanti, rischi |
| [story/](story/) | le ventuno storie, numerate `0098`–`0118`, nel formato drill-down del catalogo |
| [task/](task/) | i ventuno piani di lavoro: passi ordinati, percorsi di file reali, trappole note |
| [prototype/](prototype/README.md) | cinque prototipi navigabili (quattro ruoli + console di piattaforma) e la loro documentazione per l'implementazione |

Punto di ingresso consigliato: **[l'epica madre](epic/E22-00-rifacimento-modello-appartenenza.md)** per
capire il modello, poi i **[prototipi](prototype/README.md)** per vederlo.

## Le ventuno storie, nell'ordine di esecuzione

| # | UC | Titolo | Sotto-epica | Dipende da |
|---|---|---|---|---|
| 1 | [0116](story/0116-identita-e-appartenenze.md) | Identità della persona e appartenenze agli account | E22.5 | — |
| 2 | [0117](story/0117-account-attivo-e-selettore.md) | Account attivo nella sessione e selettore | E22.5 | 0116 |
| 3 | [0118](story/0118-inviti-e-registrazione-con-identita-esistente.md) | Inviti e registrazione quando l'identità esiste già | E22.5 | 0116, 0117 |
| 4 | [0098](story/0098-modello-dati-accesso-per-applicazione.md) | Modello dati dell'accesso per applicazione e ruolo di piattaforma | E22.1 | 0116 |
| 5 | [0099](story/0099-autorizzazione-per-applicazione.md) | Autorizzazione per applicazione: token, propagazione, varco riusabile | E22.1 | 0098 |
| 6 | [0101](story/0101-semantica-ruoli-viewer-editor-admin.md) | Semantica dei tre ruoli come contratto di piattaforma | E22.1 | 0098, 0099 |
| 7 | [0100](story/0100-sezione-members-elenco-unico.md) | «Members» come elenco unico di utenti, senza ruolo | E22.1 | 0098, 0099 |
| 8 | [0102](story/0102-listino-posti-a-fasce.md) | Listino dei posti a fasce: modello versionato e calcolo | E22.2 | 0098 |
| 9 | [0103](story/0103-acquisto-anticipato-posto-invito.md) | Acquisto anticipato del posto all'invito | E22.2 | 0100, 0102 |
| 10 | [0104](story/0104-riduzione-posti-in-attesa.md) | Riduzione dei posti in attesa | E22.2 | 0103 |
| 11 | [0105](story/0105-governo-listino-console-piattaforma.md) | Governo del listino dalla console di piattaforma | E22.2 | 0102 |
| 12 | [0106](story/0106-posti-in-billing.md) | I posti nella sezione «Billing» | E22.2 | 0103, 0105 |
| 13 | [0107](story/0107-menu-rotte-visibilita-per-ruolo.md) | Menu, rotte e visibilità per ruolo | E22.3 | 0099, 0100 |
| 14 | [0108](story/0108-cruscotto-collaboratore.md) | Cruscotto del collaboratore, senza azioni dispositive | E22.3 | 0107 |
| 15 | [0109](story/0109-catalogo-sola-lettura-richiesta-owner.md) | Catalogo in sola lettura e richiesta «chiedi all'owner» | E22.3 | 0107 |
| 16 | [0110](story/0110-miei-dati-forma-ridotta.md) | «I miei dati» in forma ridotta per il collaboratore | E22.3 | 0107 |
| 17 | [0111](story/0111-schermata-gestione-utenti-app.md) | Schermata «Gestione utenti» dentro ogni applicazione | E22.4 | 0098, 0107 |
| 18 | [0112](story/0112-copilota-ruoli-new-application.md) | Copilota dei ruoli nella skill `new-application` | E22.4 | 0101, 0111 |
| 19 | [0114](story/0114-ritiro-categoria-b2c-b2b.md) | Ritiro della categoria B2C/B2B (`App.user_model`) | E22.4 | 0099, 0101, 0112 |
| 20 | [0115](story/0115-ambito-dati-applicazione.md) | Ambito dei dati: del gruppo di lavoro o della persona | E22.4 | 0114, 0101 |
| 21 | [0113](story/0113-migrazione-account-e-copertura-e2e.md) | Migrazione degli account esistenti e copertura end-to-end | E22.4 | tutte |

Criterio dell'ordine: **nulla si mostra per ruolo prima che il ruolo esista** nei dati e nel token;
**nulla si vende prima che il posto esista** come oggetto contabile.

## Rapporto con l'epica 14

Quest'epica **supera** l'epica [14 — modello utenti multi-app](../14-modello-utenti-multiapp/) (storie
`0072`, `0073`, `0074`), che descriveva l'impianto opposto — appartenenza e posti **per applicazione**,
un listino per ogni applicazione — e registrava la gestione utenti **centralizzata** come «opzione
scartata dall'utente». Lo sviluppatore ha cambiato direzione: quel modello centralizzato è oggi il
requisito. Le tre storie restano come archivio della decisione precedente, marcate come superate, e
uscono dall'ordine di esecuzione dell'onda 2. Dettaglio nel
[§4 dell'epica madre](epic/E22-00-rifacimento-modello-appartenenza.md).

## Le decisioni già prese (non sono da rinegoziare in implementazione)

1. **Elenco unico** delle persone, senza ruolo: il ruolo appartiene alla coppia persona × applicazione.
2. **Un solo owner** per account, per ora; il modello dati non lo rende definitivo.
3. **Tre ruoli sull'applicazione**: `viewer` (sola lettura, vede **tutti** i dati), `editor` (ogni
   operazione dell'applicazione), `admin` (come editor, più abilitare persone già esistenti e cambiarne
   i ruoli).
4. **Posti di piattaforma**, non di applicazione: si paga la persona una volta, poi la si abilita
   dove serve senza costi aggiuntivi.
5. **Listino a scaglioni progressivi**: 3 posti gratuiti **owner compreso**; oltre, ogni posto paga la
   tariffa della fascia in cui cade — 4–10 → 2,99 · 11–50 → 1,99 · 51–100 → 0,99 · oltre 100 → 0,49
   €/mese. Con 52 posti: 7 × 2,99 + 40 × 1,99 + 2 × 0,99 = **102,51 €**. Il totale cresce sempre; a
   scendere è il costo del posto successivo.
6. **Pagamento anticipato** all'invito, permanenza minima di un mese, **riduzione in attesa** con blocco
   delle aggiunte e annullamento possibile; chi è indicato per la cessazione **lavora fino a scadenza**.
7. **Solo l'owner invita** (l'operazione ha effetto economico) e solo lui vede Account, Billing e
   Members.
8. **«I miei dati» resta ai collaboratori in forma ridotta**: i diritti sui propri dati sono di ogni
   persona.
9. **Le tariffe sono governate dall'amministratore di piattaforma**, per versioni con decorrenza dal
   ciclo successivo, senza retroattività.
10. **La categoria B2C/B2B delle applicazioni si ritira** (UC 0114): il nuovo modello la rende falsa —
    qualunque applicazione può avere più persone con ruoli — e il suo unico uso funzionale (scegliere i
    ruoli degli endpoint generati) scompare col varco per applicazione. L'uso di «B2C/B2B» in senso
    **giuridico** resta intatto: erano due significati sotto lo stesso nome.
11. **Al suo posto nasce l'ambito dei dati** (UC 0115): i dati di un'applicazione sono **del gruppo di
    lavoro** (chi ha accesso li vede tutti) oppure **della persona** che li ha creati (ognuno vede solo i
    propri). A differenza dell'etichetta ritirata, cambia il filtro delle interrogazioni. Regola portante:
    l'ambito limita la **visibilità** dentro l'applicazione, non la **titolarità** — l'owner non vede i dati
    altrui dall'interfaccia, ma li ottiene dalle vie di conformità, che lasciano traccia. **In questa epica
    si dichiara la caratteristica, non si costruisce il filtro**: nessuna applicazione ad ambito `utente`
    nasce qui, e una **guardia** a due punti di arresto impedisce di rilasciarne una finché il filtro non
    esiste. Il progetto del filtro è però già scritto, perché il momento giusto per pensarlo è questo.

## I punti aperti che valgono denaro o direzione di prodotto

Da decidere **prima** di implementare le storie che li possiedono:

| Punto | Storia proprietaria | Stato |
|---|---|---|
| ~~Come trattare gli account **esistenti** oltre la franchigia~~ | [0113](story/0113-migrazione-account-e-copertura-e2e.md) | ✅ **chiuso**: nessun account li supera, siamo ancora solo in locale. Resta un controllo che fa fermare la migrazione se il presupposto cambia |
| ~~Conseguenza di un **pagamento dei posti non riuscito**~~ | [0106](story/0106-posti-in-billing.md) | ✅ **chiuso**: errore definitivo → l'utente non si aggiunge; temporaneo → si ritenta; sistematico → email all'amministratore di appgrove. Nessuno perde accesso, l'account non si blocca |
| Posti durante un **periodo di prova** gratuito di un'applicazione | [0103](story/0103-acquisto-anticipato-posto-invito.md) | ⏳ proposta: si pagano comunque |
| Comunicazione preventiva ai clienti in caso di **rincaro** del listino | [0105](story/0105-governo-listino-console-piattaforma.md) | ⏳ elenco fornito, invio da progettare |
| Sorte della quota `seats` nel listino del **Mini-CRM**, i cui posti locali vengono ritirati | [0111](story/0111-schermata-gestione-utenti-app.md) | ⏳ da decidere con chi cura i prezzi |
| ~~Sorte di `App.user_model`~~ | [0114](story/0114-ritiro-categoria-b2c-b2b.md) · [0115](story/0115-ambito-dati-applicazione.md) | ✅ **chiuso**: la categoria si ritira, l'ambito dei dati prende il suo posto |
| Formulazione dell'**informativa** su ambito e titolarità dei dati | [0115](story/0115-ambito-dati-applicazione.md) | ⏳ va con il filtro, non con questa epica: si scrive sul caso concreto. Annotata in [docs/_REVISIONE-LEGALE.md](../../_REVISIONE-LEGALE.md) |
| Costruzione del **filtro per utente** (piano già scritto) | [0115](story/0115-ambito-dati-applicazione.md) | ⏳ rimandata alla prima applicazione ad ambito `utente`, che verrà dopo questa epica. Fino ad allora la guardia impedisce di credere di averlo |
