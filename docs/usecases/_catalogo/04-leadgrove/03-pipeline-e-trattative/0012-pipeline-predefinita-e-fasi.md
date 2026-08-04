# 0012 — Pipeline predefinita e fasi

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 03 — Pipeline e trattative
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0004` — è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che attiva LeadGrove oggi pomeriggio
> voglio trovare un imbuto di vendita già pronto e sensato
> così da registrare la prima trattativa in due minuti invece di dover progettare il mio processo di vendita.

**Contesto.** È la storia che recepisce il risultato più netto dell'analisi in rete: una implementazione di CRM su
tre fallisce, e il meccanismo è sempre lo stesso — si configura tutto **prima** di aver registrato una sola
trattativa ([application-description.md](../application-description.md) §2.5, fonte
[U.S. Small Business Administration](https://www.sba.gov/blog/3-biggest-problems-implementing-crm-system-what-do-about-them)).
La risposta di prodotto è: l'app nasce con una pipeline già fatta; modificarla è possibile ma non è un passo
obbligato dell'avvio.

## 2. Requisiti funzionali

1. **RF-1** — All'attivazione dell'app, ogni account riceve una pipeline predefinita con cinque fasi:
   «Da qualificare», «Qualificato», «Proposta inviata», «In negoziazione», «In chiusura», più le due fasi terminali
   «Vinta» e «Persa».
2. **RF-2** — Un amministratore può rinominare, riordinare, aggiungere e disattivare le fasi non terminali; le due
   terminali non si eliminano.
3. **RF-3** — Ogni fase porta una **probabilità di chiusura** in percentuale, precompilata con valori crescenti e
   modificabile: è ciò su cui si regge il valore atteso (storia 0017).
4. **RF-4** — Un account può avere più pipeline (per esempio «vendita diretta» e «rivenditori»); una è la
   predefinita e viene proposta quando si crea una trattativa.
5. **RF-5** — Una fase con trattative dentro non si può eliminare: si disattiva, e le trattative restano dove sono
   finché qualcuno non le sposta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Pipeline e fasi filtrano per `tenant_id` dal token verificato: la
  pipeline predefinita è una riga per account, non una riga condivisa.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST|PATCH /api/sales/v1/pipelines[/{id}]` e
  `.../stages[/{id}]`; corpo validato (percentuali fra 0 e 100, posizioni univoche); errori in
  `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabelle `pipeline` e `stage` già create dalla storia 0002; la pipeline predefinita
  si crea al primo accesso dell'account all'app, **non** con una migrazione (una migrazione non conosce gli account
  futuri).
- **RT-4 — Modulo frontend (§3, §5).** Sezione Impostazioni → Pipeline: elenco delle fasi con riordino, modifica
  della probabilità, disattivazione; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi delle fasi **predefinite** nascono tradotti in `en, it, fr, es, de` nella
  lingua dell'account che attiva l'app; una volta creati sono dati del cliente e non si ritraducono. Tutta
  l'interfaccia di configurazione è nelle cinque lingue.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Configurare le pipeline richiede ruolo `owner` o
  `admin`; un `member` legge e riceve `403` in scrittura.
- **RT-7 — Esposizione conversazionale (§12).** `get_pipeline` (storia 0034) restituisce fasi, conteggi e valore
  per fase. La configurazione delle fasi **non** è esposta alla chat: è un atto di impostazione, raro e visivo.
- **RT-8 — Dati personali (§10).** Nessun dato personale: pipeline e fasi contengono nomi di processo, non di
  persone. Nessuna voce nuova nel manifesto.
- **RT-9 — Registrazione eventi (§14).** «Pipeline predefinita creata», «fase aggiunta/rinominata/disattivata»
  registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Pronta all'attivazione**
- **Dato** un account che accede a LeadGrove per la prima volta
- **Quando** apre la sezione Trattative
- **Allora** trova la pipeline predefinita con le cinque fasi più le due terminali, senza aver configurato nulla

**CA-2 — Riordino**
- **Dato** una pipeline con cinque fasi
- **Quando** l'amministratore sposta «Proposta inviata» prima di «Qualificato»
- **Allora** l'ordine cambia ovunque, lavagna compresa, e le trattative restano nelle loro fasi

**CA-3 — Fase non eliminabile**
- **Dato** una fase con tre trattative dentro
- **Quando** l'amministratore tenta di eliminarla
- **Allora** riceve `422` con un messaggio che propone la disattivazione e dice quante trattative la occupano

**CA-4 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** tenta di rinominare una fase
- **Allora** riceve `403` e la fase resta invariata

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che hanno entrambi personalizzato le fasi
- **Quando** un utente di `A` chiede l'elenco delle pipeline
- **Allora** vede solo le proprie

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla creazione della pipeline predefinita e di **integrazione** sulle risorse;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli**;
- [ ] **prova end-to-end**: rimando alla storia 0037, dove la pipeline predefinita è il presupposto del percorso;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese le fasi predefinite;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la motivazione scritta;
- [ ] **registro delle decisioni** compilato, con annotate le cinque fasi predefinite e le loro probabilità;
- [ ] contratto degli **strumenti conversazionali**: `get_pipeline` in lettura, configurazione non esposta;
- [ ] controllo automatico di **accessibilità** verde sulla schermata di configurazione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | Servono le tabelle `pipeline` e `stage` |
| Storia `0004` | La creazione della pipeline predefinita avviene al primo accesso di un utente con un posto |

## 7. Fuori ambito

- la lavagna visuale: storia 0014;
- il valore atteso calcolato dalle probabilità: storia 0017;
- le automazioni sul cambio di fase («quando entra in negoziazione crea un'attività»): non previste in questa
  proposta, sono l'inizio della complessità che il §2.5 dice di evitare.

## 8. Punti aperti

- **I nomi delle cinque fasi predefinite** sono una proposta di prodotto. Se lo sviluppatore preferisce un imbuto
  più corto (tre fasi), va deciso prima: cambiare le fasi predefinite dopo non tocca i clienti esistenti, ma
  cambia il significato dei rapporti confrontati fra account.
