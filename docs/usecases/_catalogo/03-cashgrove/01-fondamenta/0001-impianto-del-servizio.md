# 0001 — Impianto del servizio

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio che il servizio `crediti` esista, si avvii e risponda su una rotta propria
> così da avere un posto dove mettere tutto il resto senza inventarne l'impianto ogni volta.

**Contesto.** Oggi CashGrove non esiste: non c'è servizio, non c'è schema, non c'è rotta. La piattaforma però ha già
il modo giusto di far nascere un'app — la skill `new-application`, che esegue un generatore deterministico e poi chiede
le due decisioni che un generatore non può prendere (listino e dati personali). Questa storia è l'esecuzione di quel
percorso con le risposte già preparate nel [documento capofila](../application-description.md) §3, più la verifica che
ciò che ne esce sia davvero avviabile. Si fa adesso perché nessun'altra storia può cominciare prima.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/crediti/` generato dalla skill `new-application` con identificativo
   `crediti`, modello utente `multi`, porta locale `8103`, colore-categoria `amber`, metrica `crediti_monitorati` di
   natura `stock`.
2. **RF-2** — Il servizio espone una rotta di diagnosi `GET /api/crediti/v1/stato` che risponde con la versione
   dell'applicazione e lo stato della connessione al database, e nient'altro.
3. **RF-3** — La definizione delle interfacce (OpenAPI) è generata e versionata nel repository, e comprende la rotta di
   diagnosi.
4. **RF-4** — L'infrastruttura dell'app nasce dall'istanza del modulo Terraform `microsaas_app` prodotta dallo
   scaffolding; nessuna risorsa scritta a mano.
5. **RF-5** — `./run-tests.sh` conosce la nuova area di test del servizio e la esegue.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La rotta di diagnosi **non** legge dati di account e non accetta alcun
  identificativo dall'esterno. La regola vale già come impostazione: nessuna rotta di questa app leggerà mai
  `tenant_id` dal corpo o dai parametri, solo dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Servizio Quarkus 3.20.6 su Java 21, dipendente da `services/commons`,
  pacchetto radice `app.appgrove.crediti`, Hibernate ORM bloccante. Rotte pubbliche sotto `/api/crediti/v1/...`, errori
  in `application/problem+json`, definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** In questa storia lo schema `app_crediti` viene creato vuoto dalla migrazione iniziale;
  le tabelle arrivano con la storia `0002`.
- **RT-4 — Modulo frontend (§3, §5).** Fuori ambito: il guscio del modulo è la storia `0003`.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto da questa storia.
- **RT-6 — Varchi e quota (§6, §7).** Il file di listino `services/core/src/main/resources/pricing/crediti.yaml` viene
  creato con i valori **decisi dallo sviluppatore** e registrato in `pricing/index.yaml`; la proposta del documento
  capofila §5 è solo una proposta. La catena dei varchi si applica dalla storia `0004`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato in questa storia; il file del contratto
  degli strumenti viene creato vuoto dentro il servizio, così le storie successive lo accrescono invece di inventarne
  la collocazione. Dipendenza dichiarata: UC 0061-0063, livello conversazionale non ancora implementato.
- **RT-8 — Dati personali (§10).** Il manifesto `docs/compliance/manifests/crediti.yaml` nasce qui, con identità e
  descrizione in italiano e inglese e **nessuna voce**: le voci arrivano con le tabelle. Il contratto
  `CreditiDataContract` è dichiarato con `exportData` e `purgeData` che non hanno ancora nulla da fare.
- **RT-9 — Registrazione eventi (§14).** Il servizio registra avvio e arresto con `app_id`, e ogni richiesta porta
  l'identificativo di correlazione; nessun dato personale nei registri.
- **RT-10 — Avvio locale automatico (§15).** Le proprietà di `services/crediti/src/main/resources/application.properties`
  dichiarano identificativo, porta e schema in modo che la scoperta automatica dei servizi li ricavi da sola: nessuna
  riga incollata a mano negli script di avvio.
- **RT-11 — Infrastruttura (§9).** Istanza del modulo `microsaas_app` creata tramite `infra/scripts/service-add`; il
  blocco generato non si modifica a mano.

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia**
- **Dato** il repository con la change unita e il database locale in esecuzione
- **Quando** si lancia `./app-start.sh`
- **Allora** il servizio `crediti` risponde su `http://localhost:8103` e `GET /api/crediti/v1/stato` restituisce `200`
  con versione e stato della connessione

**CA-2 — La scoperta automatica lo vede**
- **Dato** il servizio generato
- **Quando** si esegue `./dev.sh services`
- **Allora** l'elenco mostra `crediti` con porta `8103` e schema `app_crediti`, senza che nessuno script sia stato
  modificato a mano

**CA-3 — Errore in formato di piattaforma**
- **Dato** il servizio avviato
- **Quando** si chiama una rotta inesistente sotto `/api/crediti/v1/`
- **Allora** la risposta è `404` con corpo `application/problem+json`

**CA-4 — L'infrastruttura è validata**
- **Dato** il blocco Terraform generato
- **Quando** si esegue la validazione dell'area infrastruttura
- **Allora** formattazione, validazione e analisi statica passano, e nessuna risorsa risulta scritta fuori dal modulo

**CA-5 — La suite conosce la nuova area**
- **Dato** il servizio generato · **Quando** si esegue `./run-tests.sh backend` · **Allora** i test del modulo `crediti`
  vengono eseguiti e sono verdi

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e infra; l'intera suite prima del commit);
- [ ] prove di **unità** sulla configurazione e di **integrazione** sulla rotta di diagnosi con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account**: non applicabile — nessuna risorsa con dati di account (dalla storia `0002`);
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; la copre la storia `0031`;
- [ ] **traduzioni**: non applicabile, nessun testo visibile;
- [ ] **manifesto dei dati** creato in italiano e inglese, senza voci, con il contratto dati dichiarato;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, in particolare con le due fermate di
      escalation (listino e dati personali) e le risposte ricevute;
- [ ] contratto degli **strumenti conversazionali** creato vuoto dentro il servizio;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] `run-tests.sh` aggiornato con la nuova area, `_INDEX.md` degli use case aggiornato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Decisione dello sviluppatore su listino e quota | La skill `new-application` non genera senza; la proposta è nel documento capofila §5 |
| Decisione dello sviluppatore sul manifesto dei dati | Stessa ragione; la proposta è nel documento capofila §6 |

## 7. Fuori ambito

- Le tabelle di dominio: storia `0002`.
- Il modulo frontend: storia `0003`.
- L'applicazione dei varchi e della quota: storia `0004`.
- Qualunque integrazione esterna: nessuna delle 31 storie ne prevede.

## 8. Punti aperti

Le due fermate di escalation di questa storia sono, per definizione, dello sviluppatore: **prezzi e limiti dei piani**
(documento capofila §5, compresa la respinta della componente a percentuale sul recuperato, §5.1) e **classificazione
dei dati personali** (§6). Finché non sono state date, la skill non genera e la storia non parte.
