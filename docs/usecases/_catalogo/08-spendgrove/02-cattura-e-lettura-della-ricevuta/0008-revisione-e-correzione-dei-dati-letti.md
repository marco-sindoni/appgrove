# 0008 — Revisione e correzione dei dati letti

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 02 — Cattura e lettura della ricevuta
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore che ha fotografato lo scontrino
> voglio vedere l'immagine e i campi letti uno accanto all'altro, con evidenziato ciò di cui la macchina è meno
> sicura, e poter correggere in due tocchi
> così da confermare una spesa in dieci secondi sapendo che il numero che finisce in contabilità l'ho guardato io.

**Contesto.** È **la** schermata di questa applicazione: quella che trasforma un'ipotesi in un dato. La storia
precedente ha prodotto valori con una fiducia; questa li fa vedere a una persona e le fa dire di sì. Il rischio che
attenua è il più grave dell'app: se la revisione diventasse un clic da saltare, SpendGrove produrrebbe errori più
in fretta di quanto li produca la ricopiatura a mano, con l'aggravante che sembrerebbero verificati (descrizione,
§11).

## 2. Requisiti funzionali

1. **RF-1** — La schermata mostra **a sinistra l'immagine** della ricevuta (ingrandibile, ruotabile) e **a destra i
   campi** proposti, in modo che l'occhio non debba cambiare pagina per confrontare.
2. **RF-2** — I campi con fiducia sotto la soglia sono **evidenziati come da controllare**, con una dicitura che
   spiega cosa significa («la macchina non è sicura di questo valore»), e il primo di essi riceve il fuoco alla
   apertura.
3. **RF-3** — Ogni campo è modificabile; la modifica **non cancella** il valore letto: l'app conserva entrambi e sa
   dire, per ogni campo, se è stato corretto e da chi.
4. **RF-4** — La conferma è **un atto esplicito** («Conferma la spesa»), disponibile solo quando i campi
   obbligatori — data, totale, categoria, mezzo di pagamento — sono valorizzati; alla conferma la spesa passa a
   `confermata` e consuma una unità di quota.
5. **RF-5** — Controlli di coerenza mostrati **prima** della conferma, come avvisi non bloccanti: totale diverso
   dalla somma di imponibile e imposta, data futura, data più vecchia del periodo contabile aperto, importo
   anomalo rispetto alla categoria.
6. **RF-6** — Si passa alla ricevuta successiva da rivedere senza tornare all'elenco: chi ha venti scontrini deve
   poterli scorrere di fila.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Lettura e scrittura della spesa e dell'esito filtrano per `tenant_id`
  preso dal token verificato; l'immagine si recupera con l'indirizzo firmato a scadenza breve della storia `0006`.
- **RT-2 — Interfaccia di programmazione (§2).** `PATCH /api/notespese/v1/spese/{id}` per le correzioni e
  `POST /api/notespese/v1/spese/{id}/conferma` per l'atto di conferma; il corpo della conferma contiene **i valori
  finali**, così che il servizio sappia distinguere «confermato com'era» da «confermato dopo correzione». Errori in
  `application/problem+json`; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V5__correzioni_campi.sql`: tabella delle correzioni con `tenant_id`,
  campo corretto, valore letto, valore confermato, autore, momento; colonne di controllo e cancellazione logica.
  Serve alla misura della qualità della lettura (console di amministrazione) e alla difendibilità del dato.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione *Da rivedere* nel manifesto del modulo `notespese`; i dati si
  leggono con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro. La disposizione
  a due colonne diventa una colonna sola su schermo stretto, con l'immagine sopra e i campi sotto.
- **RT-5 — Cinque lingue (§4).** Etichette dei campi, spiegazione della fiducia e testi degli avvisi di coerenza
  passano dallo spazio-nomi `notespese` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Alla conferma il servizio prenota una unità della metrica `receipts` (natura
  `flow`); a quota esaurita risponde `429` con l'indicazione del rimedio e la spesa resta in `da_rivedere`.
  L'avviso di quota compare **prima** del modulo, non dopo il tentativo.
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara `elenca_da_rivedere(collaboratore?) → elenco
  delle spese in attesa di revisione con i campi a bassa fiducia`, marcato **lettura**. La conferma **non** è
  esposta come strumento: è il divieto n. 2 della descrizione dell'applicazione (§7) — se l'assistente potesse
  confermare la propria estrazione, la revisione umana sarebbe una finzione.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo che riguardi una persona oltre a quelli già dichiarati nelle
  storie `0006` e `0007`; si aggiunge però la **tabella delle correzioni**, che contiene valori estratti dal
  documento: voce nel manifesto in italiano e inglese e tabella presente in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `spesa confermata`, `campo corretto` (con il **nome** del campo,
  mai il valore), `conferma respinta per quota` portano `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione.
- **RT-10 — Accessibilità (§11).** Controllo automatico sulla schermata; i campi da controllare non sono segnalati
  **solo** dal colore ma anche da un'icona e da un testo, perché il colore da solo esclude chi non lo distingue.

## 4. Criteri di accettazione

**CA-1 — Revisione e conferma senza correzioni**
- **Dato** una spesa in `da_rivedere` con tutti i campi ad alta fiducia
- **Quando** l'utente apre la schermata e preme «Conferma la spesa»
- **Allora** la spesa passa a `confermata`, il consumo di quota aumenta di uno e la tabella delle correzioni resta
  vuota per quella spesa

**CA-2 — Correzione tracciata**
- **Dato** una spesa con il totale letto a 41,00 € e la fiducia 62
- **Quando** l'utente lo corregge in 47,00 € e conferma
- **Allora** la spesa vale 47,00 €, e resta registrato che la macchina aveva letto 41,00 € e chi ha corretto

**CA-3 — Campi obbligatori mancanti**
- **Dato** una spesa senza categoria · **Quando** l'utente tenta di confermare
- **Allora** la conferma è respinta con l'indicazione del campo mancante, evidenziato in linea, e lo stato resta
  `da_rivedere`

**CA-4 — Avviso di coerenza non bloccante**
- **Dato** una spesa con imponibile 100 €, imposta 22 € e totale 120 €
- **Quando** l'utente apre la schermata
- **Allora** vede l'avviso che il totale non torna, **e può confermare lo stesso** dopo averlo letto: l'app
  suggerisce, non decide al posto suo

**CA-5 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `receipts`
- **Quando** l'utente tenta di confermare
- **Allora** riceve `429` con il messaggio che spiega come rimediare, e nulla viene confermato

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` apre l'indirizzo di revisione di una spesa di `B`
- **Allora** riceve `404` e nessun frammento di quella spesa compare a schermo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sui controlli di coerenza e sulla soglia di fiducia; di **integrazione** sulla conferma con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla revisione e sulla conferma;
- [ ] **prova end-to-end**: *coprire ora* il passo centrale del percorso `[J-NOTESPESE]` — apri, correggi un campo,
      conferma — e aggiornare
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la tabella delle correzioni, in italiano e inglese, presente in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta di conservare valore letto e valore confermato;
- [ ] contratto dello strumento `elenca_da_rivedere` dichiarato; il divieto sulla conferma automatica scritto nel
      contratto, non solo in questo documento;
- [ ] controllo automatico di **accessibilità** verde sulla schermata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0007` | Servono l'esito e le fiducie da mostrare |
| `0004` | La conferma è il punto in cui si consuma quota |

## 7. Fuori ambito

- La proposta automatica della categoria: storia `0010`. Qui la categoria si sceglie a mano.
- Il riconoscimento del doppione: storia `0011`.
- Gli avvisi fiscali sulla tracciabilità del pagamento: storia `0020`, che si innesta in questa stessa schermata.

## 8. Punti aperti

- **Soglia di fiducia**: proposta 80, da tarare sui dati veri (punto aperto ereditato dalla storia `0007`).
- **Revisione in blocco** («conferma tutte quelle ad alta fiducia»): sarebbe comoda per chi ha venti scontrini, ma
  è esattamente la scorciatoia che svuota la revisione. Se mai si facesse, dovrebbe restare un atto umano esplicito
  e mostrare comunque ogni valore prima di confermare. **Decisione di prodotto, non di questa storia.**
