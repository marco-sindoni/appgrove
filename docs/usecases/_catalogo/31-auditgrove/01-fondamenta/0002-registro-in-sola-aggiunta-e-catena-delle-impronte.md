# 0002 — Registro in sola aggiunta e catena delle impronte

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio che il registro delle loro azioni non si possa modificare né cancellare, e che si possa dimostrare
> così da poter usare quel registro davanti a chi mi chiede conto, invece di doverlo solo affermare.

**Contesto.** È la storia che decide se AuditGrove è un prodotto o un elenco di righe (§4.3 della descrizione
dell'applicazione). Sta nelle fondamenta e non in un'epica successiva per una ragione precisa: **l'inalterabilità
non si aggiunge dopo**. Un registro nato modificabile, a cui si appende una catena di impronte sei mesi più tardi,
ha sei mesi di storia non dimostrabile — e il cliente che scopre il particolare ha ragione a non fidarsi. Qui si
costruisce la tabella e la catena; i sigilli e la verifica sono l'epica 03.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella delle azioni, scritta **solo in aggiunta**: non esiste nel codice nessun percorso
   di modifica né di cancellazione, e il ruolo di database del servizio ha su quella tabella i soli privilegi di
   inserimento e lettura.
2. **RF-2** — Ogni azione porta l'**impronta crittografica** del proprio contenuto in forma canonica, calcolata
   sui campi che costituiscono la prova (momento, sorgente, agente, richiedente, strumento, forma dei parametri,
   impronte dei valori, natura, esito, riferimento al nulla osta, numero di sequenza).
3. **RF-3** — Ogni azione porta l'**impronta dell'azione precedente dell'account**: la catena è **una sola per
   account**, non una per sorgente.
4. **RF-4** — La prima riga della catena di un account è un **evento di apertura** scritto all'attivazione
   dell'app, così che nessuna catena inizi con un'impronta precedente vuota di significato.
5. **RF-5** — L'inserimento di due azioni contemporanee sullo stesso account non produce due righe che puntano
   alla stessa precedente: l'accodamento è serializzato per account.
6. **RF-6** — Una funzione interna sa ricalcolare la catena di un intervallo e dire se è integra e, se non lo è,
   qual è la **prima** riga divergente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle azioni filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo della richiesta viene ignorato. La catena è per
  account: un'azione non può mai puntare all'impronta di un'azione di un altro account, e questo è un caso di
  prova esplicito.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova in questa storia: qui si costruisce
  il livello di persistenza e il servizio di accodamento che le storie 0008 e 0020 useranno.
- **RT-3 — Persistenza (§8).** Migrazione `V2__registro_azioni.sql` sullo schema `app_agentaudit`: tabella
  `actions` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo, numero di sequenza,
  `event_hash`, `previous_hash`. **Deroga consapevole alla convenzione di piattaforma**: la colonna `deleted_at`
  esiste per uniformità ma **non viene mai valorizzata**; il codice non ha un percorso che la scriva, e un
  collaudo lo verifica. La deroga va scritta nel registro delle decisioni della change, perché è il genere di cosa
  che il prossimo lettore scambierebbe per una dimenticanza.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata in questa storia.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto.
- **RT-6 — Varchi e quota (§6, §7).** L'accodamento di un'azione è ciò che consuma la metrica `actions` (natura
  `flow`); il consumo e il comportamento al tetto sono la storia 0004, che si aggancia qui.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: la lettura del registro è dichiarata alla
  storia 0034.
- **RT-8 — Dati personali (§10).** La tabella delle azioni conterrà identificativi di persone (chi ha chiesto,
  chi ha approvato): le voci corrispondenti nascono con le storie che introducono quei campi (0008, 0021) e
  vengono dichiarate nel manifesto in italiano e inglese, con i campi annotati. In questa storia si introduce la
  struttura, non ancora i valori.
- **RT-9 — Registrazione eventi (§14).** L'accodamento emette un evento di registro tecnico con `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione e numero di sequenza — **non** il contenuto dell'azione.
  Attenzione a non confondere i due registri: quello tecnico di piattaforma è volatile, quello dell'app è la
  prova.

## 4. Criteri di accettazione

**CA-1 — La catena si costruisce**
- **Dato** un account con la catena aperta
- **Quando** si accodano tre azioni una dopo l'altra
- **Allora** ognuna porta l'impronta della precedente, i numeri di sequenza sono `1, 2, 3` e la verifica
  dell'intervallo risponde «integra»

**CA-2 — La manomissione è rilevabile**
- **Dato** un intervallo di dieci azioni verificato integro
- **Quando** si altera direttamente sulla base di dati un campo della quinta azione
- **Allora** la verifica risponde «non integra» e indica **la quinta** come prima riga divergente

**CA-3 — Il registro non si modifica e non si cancella**
- **Dato** il servizio in esecuzione con il proprio ruolo di database
- **Quando** si tenta un aggiornamento o una cancellazione sulla tabella delle azioni
- **Allora** l'operazione viene respinta dal database, e nel codice dell'applicazione non esiste alcun percorso
  che possa emetterla

**CA-4 — Le catene di due account non si toccano**
- **Dato** due account `A` e `B` che accodano azioni nello stesso momento
- **Quando** si verificano le due catene
- **Allora** ciascuna è integra per conto proprio, nessuna riga di `A` compare nella catena di `B`, e un utente di
  `A` che chiede la verifica dell'intervallo di `B` non ottiene nulla

**CA-5 — Accodamenti contemporanei**
- **Dato** venti richieste di accodamento simultanee sullo stesso account
- **Quando** vengono servite
- **Allora** si ottengono venti righe con numeri di sequenza consecutivi e senza doppioni, e la catena resta
  integra

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dell'impronta e sulla forma canonica, e di **integrazione**
      sull'accodamento, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle catene, compreso il caso di accodamento simultaneo;
- [ ] **prova end-to-end**: risposta «nessun impatto» — nessuna superficie utente; il percorso nasce alla 0037;
- [ ] **traduzioni**: nessun testo visibile introdotto;
- [ ] **manifesto dei dati**: nessuna voce nuova in questa storia, e il fatto è dichiarato;
- [ ] **registro delle decisioni** compilato, con **due voci obbligatorie**: la deroga sul `deleted_at` mai
      valorizzato e la scelta della catena unica per account invece che per sorgente;
- [ ] contratto degli **strumenti conversazionali**: nessuno;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: la deroga alla cancellazione logica va segnalata dove la convenzione è descritta.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | Serve il servizio e lo schema dove creare la tabella |
| Concessione dei privilegi di database per ruolo | Il divieto di modifica dev'essere imposto dal database, non solo dalla buona volontà del codice |

## 7. Fuori ambito

- **i sigilli** e la loro consegna: storie 0013 e 0017. Senza sigillo consegnato fuori dal nostro perimetro, la
  catena dimostra molto meno di quanto sembri (§4.3 della descrizione): questa storia costruisce lo strumento, non
  la prova completa;
- la verifica esposta all'utente: storia 0014 (qui la funzione è interna);
- il contenuto dei parametri: storie 0009 e 0030.

## 8. Punti aperti

- **Quale funzione di impronta.** Propongo SHA-256, per compatibilità con qualunque verificatore terzo. La scelta
  va scritta nel pacchetto di prova (storia 0015) perché chi verifica deve saperla. Chi chiude: sviluppatore.
- **Chi esegue la rimozione per scadenza, se il ruolo del servizio non può cancellare.** La storia 0016 prevede
  l'unica eccezione ammessa al divieto — la rimozione di **interi intervalli scaduti**, mai di righe scelte — ma
  quella rimozione richiede un privilegio che questa storia toglie deliberatamente al ruolo del servizio. La via
  che propongo è un **ruolo separato**, usato solo dalla lavorazione di conservazione e da nient'altro, così che
  la superficie che può cancellare resti minuscola e sorvegliabile. Non lo decido: chiude lo sviluppatore,
  **prima** di implementare la 0016.
- **Rotazione della funzione di impronta nel tempo.** Se un giorno SHA-256 non bastasse più, la catena esistente
  non si può ricalcolare: servirà un cambio di algoritmo dichiarato a partire da una certa sequenza. Non si
  affronta ora, ma il campo che dichiara l'algoritmo **va previsto adesso**, perché aggiungerlo dopo significa
  avere righe che non lo dichiarano.
