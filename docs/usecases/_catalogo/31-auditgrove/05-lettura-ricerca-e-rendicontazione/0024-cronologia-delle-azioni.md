# 0024 — Cronologia delle azioni

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 05 — Lettura, ricerca e rendicontazione
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio vedere in un elenco tutte le azioni dichiarate, filtrabili per periodo, sorgente, strumento ed esito
> così da poter rispondere alla domanda «cosa è successo?» in trenta secondi invece che in mezza giornata.

**Contesto.** Alla fine dell'epica 02 il registro si riempie ma **nessuno lo vede**: le azioni entrano dalla rotta
di ingresso e restano dentro la base di dati. Questa storia è la prima superficie utente vera dell'applicazione ed
è la schermata su cui si aprirà il modulo — quella che l'utente guarda quando gli è appena successo qualcosa e non
sa ancora cosa. Il vincolo che la governa non è la ricchezza dei filtri: è che **l'elenco non deve mai mostrare i
contenuti dei parametri** (storia 0009), nemmeno quando esistono, perché una schermata che li mostra è una
schermata che li fa finire in uno screenshot dentro una conversazione di assistenza.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la rotta `GET /api/agentaudit/v1/actions` che restituisce le azioni dell'account, ordinate
   per momento decrescente, con paginazione a pagina e dimensione e con il totale dei risultati.
2. **RF-2** — L'elenco si filtra per: intervallo di date, sorgente, strumento, esito, classe di effetto,
   identificativo del richiedente, e con il filtro composito **«solo quelle senza approvazione»** — che seleziona
   le azioni eseguite senza un nulla osta collegato quando la regola dello strumento ne prevedeva uno.
3. **RF-3** — Ogni riga dell'elenco mostra: momento, sorgente, agente, richiedente, strumento, classe di effetto,
   esito, presenza o assenza del nulla osta. **Non mostra i valori dei parametri**, nemmeno in forma abbreviata.
4. **RF-4** — L'elenco è la schermata di apertura del modulo `agentaudit` e ha uno **stato vuoto** che distingue
   due situazioni diverse: nessuna sorgente collegata (invito a collegarne una) e sorgenti collegate ma nessun
   risultato per i filtri correnti (invito ad allargare i filtri).
5. **RF-5** — I filtri applicati sono leggibili nell'indirizzo della schermata, così che un collegamento a una
   ricerca si possa incollare in una conversazione di assistenza senza doverla ripetere a voce.
6. **RF-6** — L'elenco resta utilizzabile su un account con milioni di righe: nessuna interrogazione che scandisca
   l'intera tabella, e i filtri sui campi previsti sono sostenuti da indici.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni interrogazione delle azioni filtra per `tenant_id` preso dal token
  verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri di ricerca viene ignorato.
  Nessun filtro dell'elenco può ampliare l'insieme oltre l'account corrente: il filtro per account si applica
  **prima** e non è negoziabile dai parametri.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/agentaudit/v1/actions` con parametri di ricerca
  validati; paginazione a pagina/dimensione con totale; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit. Il risultato è un oggetto di trasferimento, non l'entità.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. Migrazione `V…__indici_cronologia.sql` sullo schema
  `app_agentaudit` che aggiunge gli indici a sostegno dei filtri previsti (account e momento; account, sorgente e
  momento; account, strumento e momento). Nessuna modifica alle righe esistenti: la tabella resta in sola
  aggiunta.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `cronologia` del modulo `agentaudit`, schermata di apertura; i dati
  si leggono con il client generato dalla definizione delle interfacce; solo token del sistema di design;
  funziona in tema chiaro e in tema scuro. Il colore-categoria dell'app è `violet`, e `rosso`, `ambra` e `verde`
  restano liberi di significare *negato*, *in attesa* e *consentito* dentro le righe.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — intestazioni di colonna, etichette dei filtri, testi
  degli stati vuoti, nomi degli esiti — passano dallo spazio-nomi `agentaudit` e sono presenti in `en, it, fr, es,
  de`. La storia non è conclusa se ne manca una.
- **RT-6 — Varchi e quota (§6, §7).** La lettura della cronologia **non consuma** la metrica `actions`: la quota
  misura le azioni registrate, non le volte che si guardano. Restano i varchi comuni: `401` senza token, `402`
  senza abbonamento attivo, `403` per ruolo insufficiente.
- **RT-7 — Esposizione conversazionale (§12).** Questa storia non dichiara strumenti: lo strumento di lettura
  `elenca_azioni` che serve la stessa ricerca è dichiarato alla storia 0034, e riuserà **lo stesso** livello di
  interrogazione, non una copia — la parità di comportamento fra rotta e strumento è un requisito di UC 0063.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo: l'elenco **espone** identificativi di persone già
  dichiarati alla storia 0008 (richiedente, agente). Nessuna voce nuova nel manifesto
  `docs/compliance/manifests/agentaudit.yaml`, e il fatto va dichiarato nel registro delle decisioni invece che
  sottinteso. Vincolo di minimizzazione: l'elenco restituisce i soli campi che la schermata mostra.
- **RT-9 — Registrazione eventi (§14).** La consultazione della cronologia è registrata nel registro tecnico con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** i valori dei filtri che potrebbero
  contenere un identificativo di persona in chiaro.

## 4. Criteri di accettazione

**CA-1 — L'elenco risponde e pagina**
- **Dato** un account con 250 azioni registrate
- **Quando** un utente apre la cronologia con dimensione di pagina 50
- **Allora** vede le 50 più recenti in ordine di momento decrescente e il totale dichiarato è 250

**CA-2 — Il filtro delle azioni senza approvazione**
- **Dato** un account con 10 azioni, di cui 3 eseguite senza nulla osta pur essendo su uno strumento che lo
  richiedeva
- **Quando** l'utente applica il filtro «solo quelle senza approvazione»
- **Allora** vede esattamente quelle 3, e ciascuna è marcata in modo visibile come priva di approvazione

**CA-3 — I contenuti dei parametri non compaiono**
- **Dato** un'azione su uno strumento per cui la conservazione dei contenuti è attiva (storia 0031)
- **Quando** l'utente guarda l'elenco
- **Allora** vede il nome dello strumento e la forma dei parametri, e **nessun valore**: il contenuto è visibile
  solo nella scheda di dettaglio, a chi ha il ruolo per vederlo

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie azioni
- **Quando** un utente di `A` chiede l'elenco, anche forzando l'identificativo dell'account di `B` nei parametri
  della richiesta
- **Allora** vede solo le azioni di `A`, e il parametro forzato è ignorato senza rivelare l'esistenza di `B`

**CA-5 — Stato vuoto parlante**
- **Dato** un account appena attivato, senza sorgenti collegate
- **Quando** l'utente apre la cronologia
- **Allora** vede il testo «nessuna azione ancora registrata: collega una sorgente» con il rimando alla
  registrazione della sorgente, e non un elenco vuoto muto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla costruzione dell'interrogazione filtrata e di **integrazione** sulla rotta
      dell'elenco, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sull'elenco, compreso il tentativo di forzare l'account dai parametri;
- [ ] **prova end-to-end**: **rimando** — il percorso `[J-AGENTAUDIT]` nasce alla storia 0037, che copre il
      cammino completo «sorgente collegata → azione dichiarata → azione visibile in cronologia»; il registro di
      copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve una voce
      `da-coprire` con motivo e storia proprietaria `0037`;
- [ ] prova di **accessibilità** automatica sulla schermata, che è la principale del modulo;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la voce sul perché l'elenco non
      mostra mai i valori dei parametri;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, dichiarato (è la 0034);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | Senza la rotta di ingresso non c'è niente da elencare |
| storia `0003` | Serve il guscio del modulo frontend dove innestare la sezione |
| storia `0004` | I varchi di abbonamento e ruolo sono quelli comuni, già cablati |

## 7. Fuori ambito

- **la scheda di dettaglio di una singola azione**: storia 0025. Qui si vede l'elenco, non la riga aperta;
- **la posizione nella catena e l'esito della verifica di integrità**: si vedono nella scheda (storia 0025), non
  nell'elenco, dove sarebbero rumore;
- **l'esportazione** dei risultati filtrati: storia 0027;
- **gli avvisi** che segnalano da soli le cose anomale: storia 0026. Questa storia serve chi cerca, non chi
  aspetta di essere avvisato;
- **la ricerca a testo libero** dentro i contenuti: deliberatamente esclusa, perché i contenuti per impostazione
  predefinita non ci sono (storia 0009).

## 8. Punti aperti

- **Fino a che profondità si può paginare.** Su un account con milioni di righe, chiedere la pagina numero
  ventimila è costoso e non serve a nessuno. Propongo di limitare la navigazione profonda e spingere sull'uso dei
  filtri per intervallo di date, ma il limite esatto va misurato sui volumi reali, non deciso ora. Chi chiude:
  sviluppatore, in sede di implementazione.
- **Se il filtro per richiedente debba accettare il nome invece dell'identificativo.** Il registro conserva
  identificativi, non nomi (§6.2 della descrizione dell'applicazione): cercare per nome richiederebbe di
  risolverlo altrove, e per i richiedenti dichiarati dal cliente noi il nome non lo abbiamo affatto. Resta aperto
  come tema di usabilità, non di modello dati.
