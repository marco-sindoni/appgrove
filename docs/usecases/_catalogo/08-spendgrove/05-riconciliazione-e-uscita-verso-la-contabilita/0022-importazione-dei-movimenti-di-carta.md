# 0022 — Importazione dei movimenti di carta

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 05 — Riconciliazione e uscita verso la contabilità
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi tiene l'amministrazione
> voglio caricare l'estratto dei movimenti delle carte aziendali
> così da avere l'elenco di quello che è stato davvero speso, e poter scoprire le spese di cui non è mai arrivata
> nessuna ricevuta.

**Contesto.** Quando l'azienda dà una carta al collaboratore, il pagamento esiste anche se lo scontrino non arriva
mai. Il movimento della carta è quindi la **verità di riscontro**: dice quanto è uscito e quando. Serve prima
dell'abbinamento (storia `0023`) e prima del pacchetto per il commercialista (storia `0025`), perché senza di esso
la domanda «mi manca qualche giustificativo?» non ha risposta. La scelta di perimetro è deliberata: si **importa un
file**, non ci si collega al conto. Un collegamento bancario introdurrebbe un fornitore che tratta dati per nostro
conto e un impianto di autorizzazioni che questa app non ha bisogno di portarsi dietro.

## 2. Requisiti funzionali

1. **RF-1** — Si carica un file di movimenti in formato tabellare, associandolo a una carta dell'account; l'app
   mostra un'anteprima con la corrispondenza fra colonne del file e campi attesi, correggibile prima di importare.
2. **RF-2** — Si registrano le carte dell'account: etichetta, ultime quattro cifre, collaboratore assegnatario,
   stato attiva o cessata.
3. **RF-3** — L'importazione è **idempotente**: righe già importate (stessa carta, data, importo e descrizione) non
   si duplicano, e il riepilogo dice quante ne sono state importate, quante saltate e quante respinte.
4. **RF-4** — Ogni movimento nasce in stato `da abbinare`; l'elenco si può filtrare per carta, periodo e stato.
5. **RF-5** — Le corrispondenze fra colonne si possono **salvare come profilo** per carta, così che l'importazione
   del mese successivo sia un caricamento e basta.
6. **RF-6** — Un'importazione si può **annullare in blocco** finché nessuno dei suoi movimenti è stato abbinato:
   un file sbagliato non deve costare mezza giornata di pulizia a mano.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Carte, importazioni e movimenti filtrano per `tenant_id` preso dal token
  verificato; l'importazione è riservata ai ruoli `approva` e `amministra`. Un collaboratore con ruolo `sostiene`
  vede al massimo i movimenti della **propria** carta.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/notespese/v1/carte`,
  `POST /api/notespese/v1/importazioni-movimenti` (multiparte),
  `GET /api/notespese/v1/movimenti`, `DELETE /api/notespese/v1/importazioni-movimenti/{id}`; errori in
  `application/problem+json` con distinzione fra file illeggibile, colonne mancanti e righe non interpretabili;
  definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V19__carte_e_movimenti.sql`: tabelle `carta`, `importazione_movimenti`
  e `movimento_carta` con `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione logica; indice
  di unicità tecnica per il riconoscimento delle righe già importate.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Riconciliazione*: elenco dei movimenti, procedura di importazione
  in tre passi (file, corrispondenza colonne, riepilogo). Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Testi della procedura, dei formati attesi e degli errori passano dallo spazio-nomi
  `notespese` e sono presenti in `en, it, fr, es, de`; il formato di data e di numero del **file** è indipendente
  dalla lingua dell'interfaccia e va scelto esplicitamente nella corrispondenza.
- **RT-6 — Varchi e quota (§6, §7).** L'importazione **non** consuma quota: un movimento non è un documento di
  spesa lavorato finché non diventa una spesa. Consumerà alla conferma della spesa abbinata (storia `0023`).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: non si carica un file da una conversazione. In
  lettura, `elenca_movimenti_orfani` è dichiarato nell'epica 06 fra le letture ammesse.
- **RT-8 — Dati personali (§10).** 🛑 Il movimento dice **quanto e dove ha speso una persona**: voce nuova nel
  manifesto in italiano e inglese, tabella `movimento_carta` in `exportData` e `purgeData`. **Nessun numero di
  carta completo**: si conservano al massimo le ultime quattro cifre, e il file caricato **non si conserva** dopo
  l'importazione — si tiene ciò che serve, non l'originale.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `importazione eseguita`, `importazione annullata` portano
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e i **conteggi** — mai descrizioni di esercenti
  né importi.

## 4. Criteri di accettazione

**CA-1 — Importazione con profilo di corrispondenza**
- **Dato** un file di 40 movimenti e un profilo salvato per quella carta
- **Quando** si carica il file
- **Allora** i 40 movimenti sono importati in stato `da abbinare` e il riepilogo lo conferma

**CA-2 — Idempotenza**
- **Dato** un file già importato · **Quando** lo si carica di nuovo
- **Allora** nessun movimento viene duplicato e il riepilogo dice che tutte le righe sono state saltate

**CA-3 — Righe non interpretabili**
- **Dato** un file con tre righe prive di importo
- **Quando** lo si importa
- **Allora** le altre passano, le tre sono respinte con il numero di riga e il motivo, e l'esito è consultabile
  dopo la chiusura della finestra

**CA-4 — Annullamento in blocco**
- **Dato** un'importazione i cui movimenti non sono ancora abbinati · **Quando** la si annulla
- **Allora** tutti i suoi movimenti spariscono; se anche uno solo fosse abbinato, l'annullamento è respinto con
  `409` e dice quale

**CA-5 — Nessun numero di carta completo**
- **Dato** un file che contiene il numero completo della carta
- **Quando** lo si importa
- **Allora** nel sistema restano solo le ultime quattro cifre, e il file caricato non è più recuperabile

**CA-6 — Isolamento fra account**
- **Dato** due account con carte diverse · **Quando** l'uno consulta i movimenti
- **Allora** non vede quelli dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sull'interpretazione del file (separatori, formati di data e numero) e sul riconoscimento
      delle righe già presenti; di **integrazione** sull'importazione con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e di ruolo su carte, importazioni e movimenti;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che nel percorso `[J-NOTESPESE]` importa un piccolo file
      di movimenti prima dell'abbinamento; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la dichiarazione che il file caricato non si
      conserva e che non si tiene il numero completo della carta;
- [ ] **registro delle decisioni** compilato, con la scelta di importare un file invece di collegarsi al conto;
- [ ] contratto degli **strumenti conversazionali**: nessuno di scrittura, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0012` | La carta si assegna a un collaboratore |
| Catalogo 49 — ReconGrove | **Confine, non dipendenza**: la riconciliazione bancaria completa è di quell'app; qui se ne fa la fetta minima che serve alle note spese |

## 7. Fuori ambito

- Il **collegamento diretto al conto o alla carta** tramite un aggregatore: introdurrebbe un fornitore esterno che
  tratta dati per nostro conto e un impianto di autorizzazioni che non serve al ciclo della nota spese. Escluso in
  questo giro e dichiarato nella descrizione dell'applicazione (§2.4).
- La riconciliazione del conto corrente aziendale nel suo complesso: è ReconGrove (catalogo 49).
- L'emissione di carte: non è mestiere nostro.

## 8. Punti aperti

- **Formati dei file dei principali istituti**: ogni banca esporta a modo suo. Il profilo di corrispondenza risolve
  il problema caso per caso, ma un piccolo elenco di profili pronti per gli istituti più diffusi sarebbe una
  scorciatoia utile: quali, lo dice il mercato, non questa storia.
