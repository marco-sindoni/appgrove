# 0018 — Trasferta come contenitore di spese

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 04 — Trasferte e rimborsi chilometrici
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come tecnico mandato tre giorni a montare un impianto in un'altra città
> voglio raccogliere sotto un'unica trasferta l'albergo, i pranzi e il treno
> così da presentare le spese di quella missione insieme, e da far sapere all'app che erano fuori dal Comune dove
> lavoro di solito.

**Contesto.** Il campo «fuori dal Comune sede di lavoro» esiste dalla storia `0002` ma è dichiarato spesa per
spesa: chi ha fatto tre giorni fuori deve segnarlo undici volte e sbaglierà. La trasferta è il contenitore che
raccoglie quelle spese e **porta il contesto una volta sola**. Non è un vezzo organizzativo: da quel contesto
dipende il regime fiscale, perché per vitto e alloggio l'obbligo di pagamento tracciabile scatta solo **fuori** dal
Comune sede di lavoro, mentre per il trasporto vale sempre (descrizione, §2.3, fonti 3 e 4).

## 2. Requisiti funzionali

1. **RF-1** — Si crea una trasferta con collaboratore, destinazione, data di inizio e data di fine, motivo, e
   l'indicazione se è **fuori dal Comune sede di lavoro** — proposta in automatico dal confronto con la sede del
   collaboratore e sempre correggibile.
2. **RF-2** — Una spesa si può assegnare a una trasferta al momento della revisione o dopo; assegnandola, eredita
   il contesto della trasferta (dentro o fuori Comune) senza cancellare quanto già dichiarato: se i due divergono,
   l'app lo segnala.
3. **RF-3** — La trasferta mostra il totale delle proprie spese diviso per categoria e il numero di giorni.
4. **RF-4** — L'app propone di assegnare alla trasferta le spese del collaboratore che cadono nelle sue date e non
   sono ancora assegnate: assegnare tre giorni di spese deve essere un'azione sola.
5. **RF-5** — Una trasferta si chiude quando non si aspettano altre spese; le trasferte aperte da più di un tempo
   configurabile compaiono fra le cose da sistemare.
6. **RF-6** — Una spesa sta in **una sola** trasferta, e una trasferta con spese non si cancella: si può solo
   svuotare e poi eliminare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `trasferta` filtra per `tenant_id` preso dal
  token verificato; dentro l'account vale il filtro di visibilità per ruolo (storia `0012`).
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST|PATCH /api/notespese/v1/trasferte` e
  `POST /api/notespese/v1/trasferte/{id}/spese`; errori in `application/problem+json` con `409` per la spesa già
  assegnata altrove; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V15__trasferte.sql`: tabella `trasferta` con `tenant_id`, chiave UUID
  versione 7, collaboratore, destinazione, date, motivo, `fuori_comune_sede`, stato, colonne di controllo e
  cancellazione logica; riferimento alla trasferta sulla spesa, con unicità.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Trasferte*: elenco, scheda con le spese collegate, assegnazione
  multipla dalle spese proposte. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, aiuti e il testo che spiega perché «fuori Comune» conta passano dallo
  spazio-nomi `notespese` e sono presenti in `en, it, fr, es, de`. La spiegazione della regola fiscale è **italiana
  per contenuto**: va scritta come specifica di giurisdizione, non come verità universale (punto aperto).
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo: la trasferta raggruppa spese già confermate o in revisione,
  la quota si è consumata o si consumerà alla conferma di ciascuna.
- **RT-7 — Esposizione conversazionale (§12).** La storia estende `elenca_spese` con il filtro per trasferta e
  dichiara `riepilogo_spese(periodo, raggruppamento: trasferta)` fra le letture ammesse; nessuno strumento di
  scrittura, perché creare una trasferta è un atto organizzativo che una persona compie con il calendario davanti.
- **RT-8 — Dati personali (§10).** 🛑 La trasferta dice **dove è stata una persona e quando**: è un dato di
  spostamento, e sommato agli altri disegna i movimenti di un lavoratore. Voce nuova nel manifesto in italiano e
  inglese, tabella `trasferta` in `exportData` e `purgeData`. **Nessuna posizione geografica**: la destinazione è
  un testo dichiarato dall'interessato, non una coordinata rilevata — è la differenza fra uno strumento di lavoro e
  uno strumento di controllo a distanza (descrizione, §6, punto aperto n. 6).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `trasferta creata`, `spesa assegnata`, `trasferta chiusa`
  portano `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e identificativi — **mai** la
  destinazione, che è un dato personale.

## 4. Criteri di accettazione

**CA-1 — Trasferta con spese**
- **Dato** un collaboratore con sede di lavoro a Bosconero e sei spese fra il 12 e il 14 luglio
- **Quando** crea una trasferta a Valcorta per quelle date e accetta le spese proposte
- **Allora** le sei spese risultano collegate, il totale per categoria è calcolato e la trasferta è marcata «fuori
  Comune»

**CA-2 — Proposta del contesto e correzione**
- **Dato** un collaboratore con sede a Bosconero · **Quando** crea una trasferta con destinazione Bosconero
- **Allora** l'app propone «dentro il Comune», spiegando che il regime fiscale cambia, e l'utente può correggere

**CA-3 — Divergenza dichiarata**
- **Dato** una spesa dichiarata «dentro Comune» e una trasferta «fuori Comune»
- **Quando** si assegna la spesa alla trasferta
- **Allora** l'app segnala la divergenza e chiede quale delle due vale, invece di sovrascrivere in silenzio

**CA-4 — Una spesa, una trasferta**
- **Dato** una spesa già assegnata · **Quando** si tenta di assegnarla a un'altra trasferta
- **Allora** l'operazione è respinta con `409` con l'indicazione della trasferta attuale

**CA-5 — Isolamento fra account e ruoli**
- **Dato** due collaboratori con ruolo `sostiene` nello stesso account
- **Quando** l'uno chiede l'elenco delle trasferte
- **Allora** vede solo le proprie; un utente di un altro account non ne vede nessuna

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla proposta «dentro o fuori Comune» e sui totali per categoria; di **integrazione**
      sulla risorsa con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e di visibilità per ruolo sulle trasferte;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che nel percorso `[J-NOTESPESE]` attraversa una trasferta
      fuori Comune; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la dichiarazione esplicita che non si trattano
      posizioni geografiche;
- [ ] **registro delle decisioni** compilato, con la scelta della destinazione dichiarata e non rilevata;
- [ ] contratto degli **strumenti conversazionali**: filtro per trasferta nelle letture; nessuna scrittura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0012` | Serve il Comune sede di lavoro del collaboratore per proporre il contesto |
| `0013` | Trasferta e nota spese sono due raggruppamenti diversi delle stesse spese: vanno progettati insieme per non confonderli |

## 7. Fuori ambito

- Il rimborso chilometrico: storia `0019`.
- La diaria forfettaria: storia `0021`.
- La pianificazione delle trasferte (autorizzazione preventiva a partire): è un altro processo, e nella micro-impresa
  non esiste. Se servisse, sarebbe una storia a sé.

## 8. Punti aperti

- **Trasferte all'estero**: cambiano il regime (importi diversi, valute diverse) e non sono state validate. Il
  modello le regge come destinazione, ma le regole no: è il punto aperto n. 3 della descrizione dell'applicazione.
- **Come si stabilisce «fuori dal Comune»** quando la destinazione è scritta a mano: l'app propone confrontando due
  testi, il che funziona solo se sono scritti bene. Un archivio dei Comuni renderebbe la proposta affidabile ma
  introduce dati da tenere aggiornati per giurisdizione. Decisione di prodotto.
