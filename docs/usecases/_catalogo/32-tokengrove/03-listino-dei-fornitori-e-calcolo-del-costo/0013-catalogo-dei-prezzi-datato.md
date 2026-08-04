# 0013 — Catalogo dei prezzi datato

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 03 — Listino dei fornitori e calcolo del costo
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi conduce il servizio appgrove
> voglio poter aggiornare i prezzi dei modelli dei fornitori senza rilasciare una versione dell'applicazione
> così da non avere numeri sbagliati per tre settimane ogni volta che un fornitore cambia il proprio listino.

**Contesto.** È un **requisito, non una nota**: i prezzi per unità di testo dei modelli cambiano più in fretta di
un ciclo di rilascio, e cambiano senza preavviso. Se il catalogo fosse codice, ogni variazione di prezzo di un
fornitore diventerebbe un ramo, una revisione, una messa in produzione — e nel frattempo tutti i conti di tutti i
clienti sarebbero sbagliati. Chi ha già affrontato il problema lo ha risolto allo stesso modo: LiteLLM tiene un
unico file di dati con i prezzi di oltre cento modelli e lo **scarica** periodicamente senza riavviare il servizio
(§2.6, fonti 7-8). Qui si fa la stessa cosa con due aggiunte non negoziabili: **ogni prezzo ha una validità nel
tempo** e **ogni pubblicazione è una versione**, perché senza queste due cose i conti storici cambierebbero da soli.

## 2. Requisiti funzionali

1. **RF-1** — Il catalogo dei prezzi dei fornitori è un **dato**, caricabile e sostituibile senza toccare il
   codice e senza rilasciare l'applicazione.
2. **RF-2** — Ogni riga del catalogo dichiara: fornitore, chiave del modello, prezzo per unità in ingresso, in
   uscita, per l'ingresso servito da cache e per la scrittura in cache, eventuale sconto per l'elaborazione
   differita, valuta, e la data **da cui** quel prezzo è valido.
3. **RF-3** — Una pubblicazione crea una **versione** del catalogo, con numero progressivo, data di pubblicazione,
   origine, impronta del contenuto e chi l'ha caricata. Le versioni precedenti restano **immutabili** e
   consultabili: sono la prova con cui è stato calcolato un conto passato.
4. **RF-4** — Data una chiave di modello e un istante, il catalogo sa dire **quale prezzo era valido** in quel
   momento; una modifica di prezzo pubblicata oggi non cambia il prezzo valido ieri.
5. **RF-5** — La pubblicazione di una versione mostra **prima** della conferma un'anteprima: quali modelli sono
   nuovi, quali hanno cambiato prezzo e di quanto, quali sono scomparsi. Una pubblicazione al buio è vietata.
6. **RF-6** — L'età del catalogo (giorni dall'ultima pubblicazione) è un dato visibile agli account accanto ai
   totali; oltre la soglia configurata compare un'avvertenza che dice che i conti potrebbero essere basati su
   prezzi vecchi.

## 3. Requisiti tecnici

- **RT-1 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabelle `versione_listino` e
  `prezzo_modello`, con chiave primaria UUID versione 7 e colonne di controllo. **Attenzione**: sono dati di
  piattaforma, non di account, e quindi **non** portano `tenant_id`, con l'unica eccezione dei prezzi negoziati
  della storia `0016`, che invece lo portano. La distinzione va scritta nel codice e nelle prove, perché è
  l'unica tabella dell'app che sfugge alla regola generale ed è quindi quella su cui si sbaglia.
- **RT-2 — Isolamento fra account (§1).** Il catalogo pubblico è leggibile da ogni account e non contiene dati di
  nessuno; nessuna rotta dell'app permette a un account di **modificarlo**. Prova esplicita: un account non può
  pubblicare né alterare una versione.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte di sola lettura per gli account:
  `GET /api/spesa_modelli/v1/catalogo-prezzi` (versione corrente ed età) e
  `GET /api/spesa_modelli/v1/catalogo-prezzi/modelli`. La pubblicazione è un'azione della console di
  amministrazione, non dell'app ([estensioni-admin.md](../estensioni-admin.md) §5).
- **RT-4 — Modulo frontend (§3, §5).** L'età del catalogo e la sua versione compaiono accanto ai totali di spesa,
  non in un pannello di configurazione: chi guarda un numero deve vedere con quali prezzi è stato calcolato. Solo
  token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'avvertenza sul catalogo vecchio è presente in `en, it, fr, es, de`.
- **RT-6 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: pubblicare un catalogo cambia i
  conti di tutti gli account e non è un'azione che un assistente possa preparare. La lettura della versione e
  dell'età è compresa nel risultato di `leggi_spesa` (storia `0032`).
- **RT-7 — Dati personali (§10).** Nessun dato personale: il catalogo contiene prezzi di listino pubblici.
- **RT-8 — Registrazione eventi (§14).** Evento «versione di catalogo pubblicata» con numero, impronta, operatore
  e conteggio delle righe cambiate; nessun `tenant_id` perché non è un'operazione di account.

## 4. Criteri di accettazione

**CA-1 — Aggiornamento senza rilascio**
- **Dato** un servizio in esecuzione e un catalogo alla versione 7
- **Quando** si pubblica la versione 8 con un prezzo cambiato
- **Allora** i calcoli successivi usano il prezzo nuovo **senza** che il servizio sia stato riavviato o rilasciato

**CA-2 — Il passato non cambia**
- **Dato** una misura del 10 luglio calcolata con la versione 7 del catalogo
- **Quando** si pubblica la versione 8 che raddoppia il prezzo di quel modello
- **Allora** il costo della misura del 10 luglio resta identico, e il totale di luglio non cambia di un centesimo

**CA-3 — Prezzo valido a una data**
- **Dato** un modello con prezzo `A` valido dal 1° giugno e prezzo `B` valido dal 1° agosto
- **Quando** si chiede il prezzo valido al 15 luglio
- **Allora** si ottiene `A`

**CA-4 — Anteprima obbligatoria**
- **Dato** una versione di catalogo pronta da pubblicare
- **Quando** l'operatore avvia la pubblicazione
- **Allora** vede l'elenco dei modelli nuovi, di quelli cambiati con la variazione percentuale e di quelli
  scomparsi, e nulla è pubblicato finché non conferma

**CA-5 — Catalogo vecchio segnalato**
- **Dato** un catalogo pubblicato 45 giorni fa e una soglia di 30
- **Quando** un account apre la schermata della spesa
- **Allora** vede l'avvertenza che i conti sono basati su un catalogo vecchio di 45 giorni

**CA-6 — Nessun account può toccare il catalogo**
- **Dato** un utente con ruolo `owner` di un account
- **Quando** tenta di modificare o pubblicare una versione del catalogo
- **Allora** riceve `403` e nulla cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla ricerca del prezzo valido a una data (con casi limite sui confini di validità) e di
      **integrazione** sulla pubblicazione di una versione;
- [ ] prova di **isolamento fra account**: nessun account può scrivere il catalogo, e il catalogo non espone dati
      di account;
- [ ] **prova end-to-end**: **si rimanda** alla storia `0034`; il percorso include il caso «pubblico un listino
      nuovo, i totali del mese scorso non cambiano», che è la dimostrazione più importante di questa epica;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica (nessun dato personale);
- [ ] **registro delle decisioni** compilato, in particolare sul perché il catalogo è un dato e non codice, e sul
      perché le sue tabelle non portano `tenant_id`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare; in locale è presente un catalogo minimo di
      prova.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | Serve lo schema dell'app |
| Punto aperto P6 del documento capofila | Da dove arriva il catalogo la prima volta e chi lo mantiene è una decisione da prendere prima |

## 7. Fuori ambito

- il **calcolo** del costo che usa questi prezzi: è la storia `0014`;
- i prezzi negoziati per singolo account: sono la storia `0016`;
- l'aggiornamento **automatico** da una fonte pubblica di terzi: rimandato, perché apre una questione di licenza e
  di affidabilità (punto P6). Qui la pubblicazione è manuale e verificata, che è la scelta prudente per cominciare.

## 8. Punti aperti

- **Unità di misura del prezzo.** I fornitori esprimono i prezzi per milione di unità di testo, ma non tutti allo
  stesso modo e alcuni cambiano convenzione. Proposta: conservare sempre il prezzo **per unità singola** con
  precisione ampia, e convertire solo alla presentazione. Da confermare con lo sviluppatore insieme al punto P7
  (valuta).
- **Chi verifica che un prezzo pubblicato sia giusto.** L'anteprima mostra il cambiamento, ma non può sapere se è
  vero. È una responsabilità di conduzione del servizio, già annotata nei punti aperti di
  [estensioni-admin.md](../estensioni-admin.md).
