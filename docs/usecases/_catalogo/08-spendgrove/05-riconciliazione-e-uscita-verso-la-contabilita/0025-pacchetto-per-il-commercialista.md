# 0025 — Pacchetto per il commercialista

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 05 — Riconciliazione e uscita verso la contabilità
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ogni trimestre porta le spese allo studio
> voglio produrre un unico pacchetto con la tabella dei movimenti e tutti i giustificativi dentro
> così da consegnare una cosa sola, sapendo che il commercialista non mi richiamerà per chiedermi la ricevuta
> numero quattordici.

**Contesto.** È l'uscita dell'app, quella che chiude il ciclo, e secondo l'analisi in rete è **la** integrazione
attesa dal cliente (descrizione, §2.4): la scelta del gestionale segue quasi sempre quella dello studio, quindi
l'app non si lega a nessuno e produce un pacchetto che chiunque può leggere. Va fatta dopo la qualificazione
dell'imposta e dopo il rimborso, perché il pacchetto deve contenere il dato definitivo, non una fotografia a metà
strada.

## 2. Requisiti funzionali

1. **RF-1** — Si produce un pacchetto per un periodo scelto, contenente le note spese **approvate** di quel periodo:
   una tabella dei movimenti (una riga per spesa, con tutti i campi contabili), i giustificativi allegati e un
   riepilogo per categoria e per collaboratore.
2. **RF-2** — Ogni allegato ha un nome prevedibile e ricostruibile che lo lega alla sua riga: chi apre il pacchetto
   deve poter passare dalla riga al documento senza cercare.
3. **RF-3** — Il pacchetto è **congelato**: una volta prodotto non cambia, porta la sua data, il suo autore e
   l'impronta del contenuto. Se serve una correzione, si produce un pacchetto nuovo e il vecchio resta.
4. **RF-4** — La produzione **avverte** se il periodo contiene note non ancora approvate, movimenti orfani o spese
   senza giustificativo: sono le tre cose che il commercialista rimanderebbe indietro.
5. **RF-5** — I pacchetti prodotti restano elencati e riscaricabili, con periodo, data e autore.
6. **RF-6** — Le spese incluse in un pacchetto non si modificano più: il periodo si considera consegnato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Produzione e scaricamento filtrano per `tenant_id` preso dal token
  verificato; la funzione è riservata al ruolo `amministra`. L'indirizzo di scaricamento è **firmato, a scadenza
  breve e verificato contro l'account**: un pacchetto contiene tutti i giustificativi di un trimestre, cioè il
  concentrato di dati personali più denso dell'applicazione.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/notespese/v1/pacchetti` (produzione asincrona con
  stato), `GET /api/notespese/v1/pacchetti`, `GET /api/notespese/v1/pacchetti/{id}/contenuto`; errori in
  `application/problem+json`; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V22__pacchetti_esportazione.sql`: tabella `pacchetto_esportazione` con
  `tenant_id`, chiave UUID versione 7, periodo, stato, impronta, elenco dei documenti inclusi, autore, colonne di
  controllo e cancellazione logica; marcatura di inclusione sulle spese.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Esportazioni*: scelta del periodo, riepilogo di controllo prima
  della produzione, elenco dei pacchetti. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'interfaccia è nelle cinque lingue; **le intestazioni della tabella dentro il
  pacchetto** seguono la lingua scelta al momento della produzione e la scelta è mostrata, perché il file finisce a
  una persona che non usa la nostra interfaccia.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo della metrica `receipts`: le spese sono già state contate alla
  conferma. Con abbonamento `canceled` la produzione risponde `402`; **l'esportazione dei propri dati prevista dai
  diritti dell'interessato resta invece sempre accessibile** ed è un'altra cosa (storia `0030`).
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara `esporta_per_contabilita(periodo) → pacchetto
  congelato`, marcato **scrittura irreversibile**: chiude il periodo e produce un artefatto che qualcuno riceverà,
  quindi **richiede conferma umana obbligatoria**. Dipendenza: UC 0061-0063.
- **RT-8 — Dati personali (§10).** 🛑 Il pacchetto è **il punto di massima concentrazione** di dati personali
  dell'app: nomi di collaboratori, spostamenti, importi e tutte le immagini dei giustificativi in un file solo, che
  poi esce dalla nostra infrastruttura. Voce nuova nel manifesto in italiano e inglese; tabella
  `pacchetto_esportazione` **e i pacchetti archiviati** in `exportData` e `purgeData`. Va dichiarata una
  **ritenzione** dei pacchetti prodotti e va detto all'utente, al momento dello scaricamento, che da lì in poi la
  responsabilità del file è sua.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `pacchetto prodotto`, `pacchetto scaricato` portano
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione, periodo e conteggi — mai nomi né importi. Lo
  scaricamento **va registrato sempre**: è un accesso a un concentrato di dati personali.

## 4. Criteri di accettazione

**CA-1 — Pacchetto completo**
- **Dato** un trimestre con quattro note approvate e trentuno spese
- **Quando** si produce il pacchetto
- **Allora** contiene trentuno righe, trenta allegati (una spesa è senza giustificativo, e la riga lo dice) e i
  riepiloghi per categoria e collaboratore

**CA-2 — Avvisi prima della produzione**
- **Dato** un periodo con una nota ancora `inviata` e due movimenti orfani
- **Quando** si apre la produzione
- **Allora** i tre avvisi sono mostrati **prima** della conferma, con la possibilità di procedere lo stesso

**CA-3 — Congelamento**
- **Dato** un pacchetto prodotto · **Quando** si tenta di modificare una spesa che vi è inclusa
- **Allora** l'operazione è respinta con `409` e il messaggio indica il pacchetto che l'ha inclusa

**CA-4 — Impronta stabile**
- **Dato** un pacchetto prodotto · **Quando** lo si riscarica dopo un mese
- **Allora** l'impronta è la stessa dichiarata al momento della produzione

**CA-5 — Ruolo insufficiente**
- **Dato** un collaboratore con ruolo `sostiene` o `approva` · **Quando** tenta di produrre o scaricare un pacchetto
- **Allora** riceve `403`

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** un utente di `A` tenta di scaricare un pacchetto di `B` con l'indirizzo firmato
  ottenuto altrove
- **Allora** l'accesso è negato: la firma è verificata **anche** contro l'account, non solo contro la scadenza

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione della tabella e sul calcolo dell'impronta; di **integrazione** sulla
      produzione asincrona con database effimero, migrazioni vere e archivio simulato;
- [ ] prova di **isolamento fra account** sulla produzione, sull'elenco e **sull'indirizzo firmato di
      scaricamento**;
- [ ] **prova end-to-end**: *coprire ora* il passo conclusivo «produco il pacchetto del trimestre» nel percorso
      `[J-NOTESPESE]`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese le intestazioni della tabella esportata;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la ritenzione dei pacchetti dichiarata e i
      pacchetti presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta del pacchetto neutro invece del collegamento a un
      gestionale;
- [ ] contratto dello strumento `esporta_per_contabilita` dichiarato, marcato scrittura irreversibile con conferma
      obbligatoria;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0016` | Il pacchetto contiene il dato definitivo, rimborso compreso |
| `0024` | Le righe portano la qualificazione dell'imposta: senza, il commercialista deve rifare il lavoro |

## 7. Fuori ambito

- Il **collegamento diretto** ai gestionali contabili (TeamSystem, Fatture in Cloud e simili): è l'evoluzione
  naturale, ma introdurrebbe un fornitore per ciascuno e un tracciato proprietario da mantenere. Qui si produce un
  file che il cliente consegna: il destinatario è un fornitore **suo**, non nostro (descrizione, §2.4).
- La conservazione a norma del pacchetto: storia `0026`. Produrre non è conservare.
- La registrazione in prima nota: non è un software di contabilità.

## 8. Punti aperti

- **Formato del tracciato**: nessuno standard è imposto dal mercato italiano per le note spese. Serve scegliere un
  formato tabellare leggibile e, forse, una variante per i due gestionali più diffusi. Decisione di prodotto, da
  prendere guardando che cosa gli studi accettano davvero.
- **Ritenzione dei pacchetti prodotti**: quanto tenerli, dato che sono il concentrato più denso di dati personali
  dell'app. Decisione di conformità.
