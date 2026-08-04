# 0024 — Ricostruzione della giacenza dal registro

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 04 — Inventario fisico, rettifiche e valore
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi gestisce la piattaforma, e come titolare che si fida del numero che legge
> voglio che qualcuno risommi periodicamente il registro e verifichi che la giacenza pubblicata coincida
> così da scoprire una divergenza prima che la scopra un cliente promettendo merce che non ha.

**Contesto.** La giacenza è una **proiezione**: esiste per non risommare cinque anni di movimenti mille volte al
giorno, ma l'autorità resta al registro (descrizione dell'applicazione, §4). Una proiezione può sbagliare — un
difetto in una transazione, una migrazione andata male, un movimento importato fuori dal percorso normale — e il
guaio di questo tipo di errore è che **è silenzioso**: nessuno se ne accorge finché il numero non serve davvero.
La corruzione silenziosa del saldo è dichiarata come il rischio esistenziale dell'applicazione (descrizione, §11),
e questa storia è la sua principale attenuazione insieme all'aritmetica nella base di dati.

## 2. Requisiti funzionali

1. **RF-1** — Esiste una verifica che, per un ambito dato (un account, un deposito, un articolo), ricalcola il
   saldo **sommando i movimenti del registro** e lo confronta con la proiezione della giacenza.
2. **RF-2** — La verifica gira **periodicamente** per tutti gli account e si può lanciare **su richiesta** su un
   ambito ristretto; in entrambi i casi produce un esito persistente con: momento, ambito, righe controllate, righe
   divergenti e durata.
3. **RF-3** — Ogni divergenza è registrata con `tenant_id`, articolo, deposito, saldo dal registro, saldo dalla
   proiezione, scarto e momento della rilevazione.
4. **RF-4** — Quando registro e proiezione divergono, **la verità è il registro**: la proiezione viene riscritta con
   il valore ricalcolato, insieme al riferimento all'ultimo movimento applicato e a una `versione` incrementata.
   La riparazione lascia traccia sull'esito della verifica.
5. **RF-5** — La riparazione **non crea alcun movimento**: non è una rettifica, perché nessun fatto nuovo è
   accaduto nel magazzino. Il registro non si tocca **mai**, in nessun ramo di questo codice.
6. **RF-6** — La verifica **non blocca le scritture**: legge a lotti e ripara riga per riga con un aggiornamento
   condizionato sulla `versione`; se nel frattempo un movimento ha già aggiornato quella riga, la riparazione della
   riga viene saltata e ritentata al giro successivo, senza far fallire l'intera verifica.
7. **RF-7** — Il conteggio delle divergenze per account è esposto alla console di amministrazione come indicatore
   di salute dell'applicazione, insieme al momento dell'ultima verifica
   ([estensioni-admin.md](../estensioni-admin.md)). Nessun contenuto del cliente è esposto: identificativi, scarti
   e conteggi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La verifica su richiesta filtra per `tenant_id` preso dal token
  verificato. La verifica periodica gira per tutti gli account **un account alla volta**, con il contesto impostato
  esplicitamente: nessuna interrogazione somma movimenti di account diversi, e una prova lo dimostra confrontando i
  risultati con quelli di due verifiche separate.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/magazzino/v1/verifiche-giacenza` (lancia una
  verifica sull'ambito indicato) e `GET /api/magazzino/v1/verifiche-giacenza` (esiti, paginati); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit. La rotta di lancio è riservata ai
  ruoli `owner` e `admin`.
- **RT-3 — Persistenza (§8).** Migrazione `V15__verifica_giacenza.sql` sullo schema `app_magazzino`: tabella
  `verifica_giacenza` (ambito, momento di inizio e fine, righe controllate, righe divergenti, esito) e tabella
  `divergenza_giacenza` (articolo, deposito, saldo da registro, saldo da proiezione, scarto, riparata sì/no,
  momento), entrambe con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
  Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Voce diagnostica nella sezione `impostazioni` del modulo `magazzino`: data
  dell'ultima verifica, numero di divergenze trovate e riparate, azione «verifica adesso» sul deposito scelto. È
  una schermata sobria: se il numero è zero, come dev'essere quasi sempre, non deve occupare spazio. Solo token del
  sistema di design, colore-categoria `amber`; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, esiti e messaggi passano dallo spazio-nomi `magazzino` e sono presenti
  in `en, it, fr, es, de`. Attenzione alla parola scelta per «divergenza»: non deve suggerire al cliente che
  qualcuno abbia sbagliato a contare — è un controllo del programma su sé stesso.
- **RT-6 — Varchi e quota (§6, §7).** La verifica **non consuma quota e non viene mai respinta con `429`**: il
  tetto `articoli_gestiti` (natura `stock`) colpisce solo la creazione di articoli nuovi. La verifica periodica
  gira **anche** per gli account con abbonamento `past_due` o `canceled`, perché la correttezza dei dati conservati
  non dipende dal pagamento; la rotta su richiesta segue invece la catena ordinaria e risponde `402` con
  abbonamento `canceled`.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento dichiarato.** È deliberato: una manutenzione
  interna non è una funzione che un assistente debba poter lanciare per conto di qualcuno, e la sua descrizione in
  lingua naturale («ricalcola le giacenze») è troppo simile a «cambia le giacenze» perché il rischio di
  fraintendimento valga il beneficio. Il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: le due tabelle contengono identificativi di
  articoli, depositi e numeri. Le colonne di controllo `created_by` sono valorizzate dall'utente che lancia la
  verifica su richiesta e dall'esecutore di sistema per quella periodica; entrambe le tabelle vanno aggiunte a
  `exportData` e `purgeData` del contratto `MagazzinoDataContract` per completezza, come vuole la regola «ogni
  tabella dell'app compare in entrambi».
- **RT-9 — Registrazione eventi (§14).** Gli eventi `verifica avviata`, `verifica conclusa` (con righe controllate
  e divergenti), `divergenza rilevata`, `proiezione riparata` sono registrati con `tenant_id`, `app_id`, `user_id`
  e identificativo di correlazione. Una divergenza rilevata è un evento di livello **avviso**, non informativo:
  deve poter accendere una spia.

## 4. Criteri di accettazione

**CA-1 — Nessuna divergenza sul caso normale**
- **Dato** un account con duecento articoli e un migliaio di movimenti registrati dal percorso ordinario
- **Quando** si esegue la verifica su tutto l'account
- **Allora** l'esito riporta duecento righe controllate, zero divergenti, e nessuna riga di giacenza è stata
  riscritta

**CA-2 — Divergenza rilevata e proiezione riparata**
- **Dato** una riga di giacenza alterata artificialmente a 99 mentre la somma dei movimenti dà 7
- **Quando** si esegue la verifica
- **Allora** viene registrata una divergenza con saldo da registro 7, saldo da proiezione 99 e scarto `+92`; la
  proiezione diventa 7 con `versione` incrementata; **nessun movimento è stato creato** e il registro contiene
  esattamente le righe di prima

**CA-3 — La riparazione non è una rettifica**
- **Dato** la divergenza riparata al criterio precedente
- **Quando** si consulta lo storico dei movimenti dell'articolo nella sezione `movimenti`
- **Allora** non compare alcun movimento di rettifica né di altro tipo generato dalla verifica, e la diagnostica in
  `impostazioni` mostra una divergenza riparata

**CA-4 — Scritture concorrenti non bloccate**
- **Dato** una verifica in corso su un deposito con molte righe
- **Quando** un utente registra uno scarico su un articolo di quel deposito
- **Allora** lo scarico riesce senza attendere la fine della verifica, la giacenza risultante è corretta e la
  verifica non segnala una falsa divergenza su quella riga

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con articoli dallo stesso codice interno
- **Quando** la verifica periodica gira su entrambi
- **Allora** gli esiti e le divergenze sono separati per account, e un utente di `A` che chiede gli esiti vede solo
  i propri anche forzando l'identificativo dell'altro account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul confronto fra somma del registro e proiezione, di **integrazione** sulla verifica con
      database effimero, migrazioni vere e una divergenza indotta di proposito;
- [ ] prova che dimostra che la verifica **non scrive mai** nella tabella `movimento`;
- [ ] prova di **isolamento fra account** sugli esiti e sulla verifica periodica;
- [ ] **prova end-to-end**: *nessun impatto* — è una manutenzione interna con una sola voce diagnostica in
      `impostazioni`, non un percorso d'uso; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) non riceve voci nuove da questa
      storia;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: le due tabelle nuove aggiunte a esportazione e cancellazione, senza campi personali
      nuovi da dichiarare;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la distinzione fra riparazione
      della proiezione e rettifica scritta per esteso;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento, con il motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: [estensioni-admin.md](../estensioni-admin.md) descrive il contatore delle
      divergenze esposto alla console.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | Registro dei movimenti e proiezione con `versione` e `ultimo_movimento_id`: sono i due termini del confronto |
| `0021` | Serve avere già chiaro cosa **è** una rettifica per costruire qualcosa che deliberatamente non lo è |
| Console di amministrazione (piattaforma) | Il contatore delle divergenze per account è esposto lì, come descritto in [estensioni-admin.md](../estensioni-admin.md) |

## 7. Fuori ambito

- **Correggere il magazzino reale**: se la merce non c'è, questa storia non se ne accorge e non deve. Il confronto
  con la realtà fisica è il conteggio (`0022`, `0023`).
- **Riparare il registro**: non esiste e non esisterà. Se una riga del registro fosse sbagliata, l'unica via è lo
  storno (`0017`), che è un fatto nuovo.
- **Ricostruire la giacenza a una data passata** («quanto avevo il 31 dicembre?»): è una lettura storica utile e
  diversa, e appartiene alla storia `0025` per la parte di valore e a una storia futura per la parte di quantità.
- **Avvisare il cliente** quando una divergenza viene trovata: qui la si registra e la si mostra in diagnostica; la
  notifica attiva non è prevista, vedi i punti aperti.

## 8. Punti aperti

- **La differenza fra rettifica e riparazione è il punto più facile da sbagliare di tutta l'applicazione**, e va
  ripetuta: la **rettifica** dice «il mondo è diverso da quello che il registro racconta» ed è un fatto nuovo, con
  un motivo e un responsabile; la **riparazione** dice «il registro è giusto, il numero che pubblicavo no» e non è
  un fatto, è la correzione di una nostra copia di comodo. Se un giorno qualcuno implementasse la riparazione
  creando un movimento di rettifica «di sistema», il registro comincerebbe a raccontare movimenti di merce mai
  avvenuti e l'applicazione perderebbe la sua unica proprietà preziosa. Chi tocca questo codice deve trovarci un
  commento che lo dice.
- **Frequenza della verifica periodica.** La proposta è: una volta al giorno, in orario di basso carico. Non ho un
  dato che dimensioni la scelta; dipende dal costo di lettura sul database condiviso e va misurata prima di fissarla.
- **Cosa fare quando la divergenza si ripete sulla stessa riga.** Ripararla ogni notte senza dire nulla nasconde un
  difetto invece di risolverlo. La proposta è di segnalare alla console le divergenze **ricorrenti** in modo
  distinto dalle occasionali; la soglia e il canale di avviso li decide chi gestisce la piattaforma.
- **Se il cliente debba vedere la diagnostica.** Mostrarla costruisce fiducia («il programma si controlla da solo»)
  ma può allarmare senza motivo. La proposta è di mostrarla in `impostazioni`, discreta e senza colori d'allarme
  quando il valore è zero: da confermare con lo sviluppatore.
