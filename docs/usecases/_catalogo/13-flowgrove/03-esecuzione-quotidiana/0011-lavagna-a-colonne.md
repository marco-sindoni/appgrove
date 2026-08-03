# 0011 — Lavagna a colonne

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 03 — Esecuzione quotidiana
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come squadra di quattro persone che si trova la mattina
> voglio vedere le attività del progetto disposte per stato e spostarle trascinandole
> così da capire in tre secondi cosa è fermo, senza leggere una tabella.

**Contesto.** La lavagna a colonne è la funzione che il mercato associa alla categoria ed è ciò che il cliente si
aspetta di vedere. Va detto con chiarezza che **non è il valore di FlowGrove**: è commodity, esiste gratis in
ClickUp e Trello ([application-description.md](../application-description.md) §2.1). La si costruisce perché
senza di essa l'app appare incompleta, e la si costruisce **semplice**: colonne fisse corrispondenti agli stati,
nessuna configurazione, nessuna automazione. È anche il punto in cui l'accessibilità morde, perché il
trascinamento non è utilizzabile da tutti.

## 2. Requisiti funzionali

1. **RF-1** — La lavagna mostra una colonna per ciascuno stato non terminale (`da fare`, `in corso`,
   `in verifica`), con le attività come schede, il conteggio e la somma delle stime in testa a ogni colonna.
2. **RF-2** — Trascinando una scheda da una colonna all'altra l'attività cambia stato; l'esito si vede subito e,
   se la scrittura fallisce, la scheda torna al suo posto con un messaggio.
3. **RF-3** — Esiste un'alternativa **da tastiera** equivalente: selezionare la scheda e scegliere lo stato da un
   elenco, senza usare il puntatore.
4. **RF-4** — La lavagna si filtra per progetto e per assegnatario, e ricorda l'ultima scelta.
5. **RF-5** — Portare un'attività a `fatta` dalla lavagna avviene da un'azione esplicita sulla scheda, non
   trascinando: `fatta` è uno stato terminale e conviene che costi un gesto in più.
6. **RF-6** — Le colonne con molte attività caricano a scaglioni e dicono quante ne restano.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavagna legge solo attività dell'account del token verificato; il
  cambio di stato verifica che l'attività appartenga allo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/progetti/v1/tasks/{id}/state` (già introdotta
  dalla storia 0007) estesa con lo **stato atteso di partenza**, così che due persone che spostano la stessa
  scheda nello stesso momento non si sovrascrivano: se non corrisponde, risposta `409` con lo stato attuale.
  Errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Progetti → Lavagna*; aggiornamento ottimistico con rientro in
  caso di errore; solo token del sistema di design; funziona in tema chiaro e scuro. Nessun colore scritto a mano
  sulle colonne.
- **RT-5 — Cinque lingue (§4).** Intestazioni delle colonne, messaggi di conflitto e di errore in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Ruolo minimo per spostare una scheda: `member`.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento corrispondente è `update_status(id, stato)`, marcato
  **scrittura con bozza e conferma umana** (storia 0029): la chat propone lo spostamento, la persona approva.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la scheda mostra il nome dell'assegnatario, già
  dichiarato.
- **RT-9 — Registrazione eventi (§14).** «Stato cambiato» con identificativo dell'attività, stato di partenza e
  d'arrivo, autore; **mai** il titolo dell'attività.

## 4. Criteri di accettazione

**CA-1 — Spostamento riuscito**
- **Dato** un'attività in `da fare`
- **Quando** l'utente la trascina in `in corso`
- **Allora** la scheda resta nella nuova colonna e i conteggi delle due colonne si aggiornano

**CA-2 — Movimento in conflitto**
- **Dato** due utenti con la stessa lavagna aperta e un'attività in `da fare`
- **Quando** il primo la porta a `in corso` e subito dopo il secondo la porta a `in verifica`
- **Allora** il secondo riceve `409`, la lavagna gli si aggiorna con lo stato reale e nessun cambio spurio viene
  scritto

**CA-3 — Alternativa da tastiera**
- **Dato** un utente che naviga solo con la tastiera
- **Quando** seleziona una scheda e sceglie lo stato di destinazione
- **Allora** l'attività si sposta esattamente come con il trascinamento

**CA-4 — Attività padre con figli aperti**
- **Dato** un'attività con sotto-attività aperte
- **Quando** si tenta di portarla a `fatta`
- **Allora** compare il rifiuto della storia 0007 e la scheda resta dov'era

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` invia un cambio di stato per un'attività di `B`
- **Allora** riceve `404` e nulla cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sul controllo di concorrenza e di **integrazione** sul cambio di stato;
- [ ] prova di **isolamento fra account** sul cambio di stato;
- [ ] **prova end-to-end**: coprire ora — lo spostamento di stato è un passo del percorso `[J-PROGETTI]`
      (storia 0031), eseguito con l'alternativa da tastiera perché sia stabile; voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotate le colonne fisse e il controllo di concorrenza;
- [ ] controllo automatico di **accessibilità** verde sulla lavagna, con verifica esplicita dell'uso da tastiera;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | Servono le attività e la macchina a stati |
| Storia `0008` | Il filtro della lavagna riusa quello dell'elenco |

## 7. Fuori ambito

- colonne configurabili dal cliente: escluse per scelta (§2.5 della descrizione);
- automazioni («quando arriva in verifica avvisa Tizio»): fuori perimetro;
- il riordino delle schede dentro una colonna: l'ordine è per scadenza.

## 8. Punti aperti

- Nessuno.
