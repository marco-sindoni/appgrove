# 0014 — Lavagna a colonne

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 03 — Pipeline e trattative
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che entra nell'app la mattina
> voglio vedere tutte le trattative aperte disposte per fase, e spostarle trascinandole
> così da capire in tre secondi dove sono i soldi in ballo e cosa si è mosso.

**Contesto.** La pipeline visuale è la funzione che il mercato associa alla categoria — è quella per cui Pipedrive
è conosciuto ([application-description.md](../application-description.md) §2.1). Non è decorazione: è il modo in
cui una persona capisce a colpo d'occhio uno stato che una tabella non comunica. È anche il punto in cui i
requisiti di accessibilità mordono, perché il trascinamento non è utilizzabile da tutti e serve sempre
un'alternativa da tastiera.

## 2. Requisiti funzionali

1. **RF-1** — La lavagna mostra una colonna per fase non terminale, con le trattative aperte come schede, il
   conteggio e la somma dei valori in testa a ogni colonna.
2. **RF-2** — Trascinando una scheda da una colonna all'altra la trattativa cambia fase; l'esito si vede subito e,
   se la scrittura fallisce, la scheda torna al suo posto con un messaggio.
3. **RF-3** — Esiste un'alternativa **da tastiera** equivalente: selezionare la scheda e scegliere la fase da un
   elenco, senza usare il puntatore.
4. **RF-4** — La lavagna si può filtrare per responsabile e per pipeline, e ricorda l'ultima scelta.
5. **RF-5** — Le colonne con molte trattative caricano a scaglioni invece di mostrarle tutte, e dicono quante ne
   restano.
6. **RF-6** — Spostare una trattativa in una fase terminale («Vinta», «Persa») dalla lavagna apre il flusso di
   chiusura della storia 0016 invece di completare il movimento in silenzio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavagna legge solo trattative dell'account del token verificato; il
  cambio di fase verifica che trattativa e fase appartengano allo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `PATCH /api/sales/v1/deals/{id}/stage` con la fase di
  destinazione e la fase attesa di partenza, così che due utenti che spostano la stessa scheda nello stesso momento
  non si sovrascrivano: se la fase di partenza non corrisponde, risposta `409` con lo stato attuale. Errori in
  `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; il movimento scrive un `deal_stage_event` (storia 0015),
  nella stessa transazione del cambio di fase.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Trattative → Lavagna; stato del server con la libreria di
  interrogazione, aggiornamento ottimistico con rientro in caso di errore; solo token del sistema di design;
  funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Intestazioni delle colonne (che sono nomi di fase del cliente) a parte, tutte le
  stringhe dell'interfaccia in `en, it, fr, es, de`; le somme si formattano secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Chi non ha un posto riceve `403`.
- **RT-7 — Esposizione conversazionale (§12).** `update_deal_stage` è lo strumento corrispondente ed è **scrittura
  con bozza e conferma** (storia 0035): la chat propone lo spostamento, la persona approva.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. Le schede della lavagna mostrano il nome
  dell'azienda e del contatto: dati già dichiarati.
- **RT-9 — Registrazione eventi (§14).** «Fase cambiata» con identificativo della trattativa, fase di partenza e
  di arrivo, autore; **mai** il titolo della trattativa.

## 4. Criteri di accettazione

**CA-1 — Spostamento riuscito**
- **Dato** una trattativa in «Qualificato»
- **Quando** l'utente la trascina in «Proposta inviata»
- **Allora** la scheda resta nella nuova colonna, i conteggi e le somme delle due colonne si aggiornano e viene
  scritto un passaggio di fase

**CA-2 — Movimento in conflitto**
- **Dato** due utenti che hanno la stessa lavagna aperta e una trattativa in «Qualificato»
- **Quando** il primo la sposta in «In negoziazione» e subito dopo il secondo la sposta in «Proposta inviata»
- **Allora** il secondo riceve `409`, la lavagna gli si aggiorna mostrando lo stato reale e nessun passaggio
  spurio viene scritto

**CA-3 — Alternativa da tastiera**
- **Dato** un utente che naviga solo con la tastiera
- **Quando** seleziona una scheda e sceglie la fase di destinazione
- **Allora** la trattativa si sposta esattamente come con il trascinamento

**CA-4 — Fase terminale**
- **Dato** una trattativa aperta
- **Quando** l'utente la trascina nella colonna «Vinta»
- **Allora** si apre il flusso di chiusura (storia 0016) e la trattativa **non** risulta chiusa finché non si
  conferma

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` invia un cambio di fase per una trattativa di `B`
- **Allora** riceve `404` e nulla cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul controllo di concorrenza e di **integrazione** sul cambio di fase;
- [ ] prova di **isolamento fra account** sul cambio di fase;
- [ ] **prova end-to-end**: coprire ora — lo spostamento di fase è il quarto passo del percorso `[J-SALES]`
      (storia 0037), eseguito con l'alternativa da tastiera perché sia stabile; voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta del controllo di concorrenza sulla fase attesa;
- [ ] contratto degli **strumenti conversazionali**: rimando a `update_deal_stage` (storia 0035);
- [ ] controllo automatico di **accessibilità** verde sulla lavagna, con verifica esplicita dell'uso da tastiera;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0012` | Servono le fasi che diventano colonne |
| Storia `0013` | Servono le trattative da disporre |
| Storia `0015` | Il movimento scrive lo storico: le due si implementano insieme o 0015 subito dopo, nella stessa transazione |

## 7. Fuori ambito

- il riordino delle trattative **dentro** una colonna: non previsto, l'ordine è per data attesa di chiusura;
- la modifica dei campi dalla scheda della lavagna: si apre il dettaglio;
- la lavagna delle attività: l'agenda è la storia 0020.

## 8. Punti aperti

- Nessuno.
