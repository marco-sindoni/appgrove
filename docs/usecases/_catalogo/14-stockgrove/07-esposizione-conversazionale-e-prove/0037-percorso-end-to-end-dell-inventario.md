# 0037 — Percorso end-to-end dell'inventario

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0037` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0036`, `0021`, `0022`, `0023`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che una volta l'anno conta quello che ha
> voglio la garanzia automatica che contare, trovare una differenza e correggerla lasci una traccia leggibile e un
> saldo che torna
> così da poter credere al numero che l'applicazione mi mostra il resto dell'anno.

**Contesto.** Il percorso `0036` dimostra che il registro regge sotto i movimenti quotidiani. Resta il momento in
cui il registro incontra la realtà: il conteggio fisico. È il punto in cui l'applicazione ammette che il proprio
saldo poteva essere sbagliato e lo corregge con una rettifica motivata — mai riscrivendo la quantità, perché una
casella «quantità» modificabile sarebbe il difetto d'origine di tutta l'applicazione (§4 della descrizione).
Questa storia copre quella catena da un capo all'altro e la chiude con il controllo che dimostra che la proiezione
dice la verità: la giacenza ricostruita dal registro deve coincidere con quella pubblicata. È anche la storia che
**chiude il registro di copertura** dell'applicazione, incluse le voci che le storie precedenti avevano lasciato in
rimando.

## 2. Requisiti funzionali

1. **RF-1** — Un secondo gruppo di test in `tools/platform-e2e/journeys/J-MAGAZZINO.spec.ts`, con l'etichetta
   `[J-MAGAZZINO]` in testa a ogni titolo, copre il percorso dell'inventario fisico: apertura della sessione,
   conteggio, elenco delle differenze, chiusura, verifica nel registro, ricostruzione della giacenza.
2. **RF-2** — Il percorso apre una sessione di inventario su un deposito con l'ambito «tutto il deposito», conta un
   articolo che risulta a **9** trovandone **6**, e verifica che la quantità attesa sia stata **congelata**
   all'apertura della sessione.
3. **RF-3** — Prima della chiusura il percorso verifica che l'elenco delle differenze sia mostrato per intero —
   articolo, quantità attesa, quantità contata, scarto — e che **finché non si conferma** non esista alcuna
   rettifica e la giacenza resti **9**.
4. **RF-4** — La chiusura richiede la scelta di un **motivo** per la differenza; il percorso verifica che senza
   motivo la chiusura sia rifiutata, e che con il motivo generi **una** rettifica per riga in differenza.
5. **RF-5** — Dopo la chiusura il percorso ritrova nel registro il movimento di rettifica `−3` con il **motivo** e
   l'**autore** che ha contato, verifica che la giacenza sia **6**, e verifica che il movimento di carico
   originario sia ancora lì, invariato: la rettifica aggiunge un fatto, non ne riscrive uno.
6. **RF-6** — Il percorso esegue la **ricostruzione della giacenza dal registro** (storia `0024`) e verifica che la
   somma algebrica dei movimenti coincida con la proiezione pubblicata per ogni coppia articolo-deposito toccata,
   e che il contatore delle divergenze resti a zero.
7. **RF-7** — Il registro di copertura [`docs/testing/copertura-e2e.yaml`](../../../../testing/copertura-e2e.yaml)
   viene **chiuso** per questa applicazione: ogni storia con superficie applicativa ha la sua voce, e le storie che
   avevano risposto *rimando* vengono ricondotte alla voce che le copre (§3 qui sotto). Nessuna storia dell'app
   resta senza risposta alla domanda di copertura.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso verifica che una sessione di inventario di un account non sia
  visibile né chiudibile da un altro, anche conoscendone l'identificativo; il `tenant_id` viene sempre dal token
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Il percorso usa le rotte pubbliche `/api/magazzino/v1/inventari`,
  `/inventari/{id}/righe`, `/inventari/{id}/differenze`, `/inventari/{id}/chiusura` e `/movimenti` così come sono
  pubblicate; gli errori attesi sono verificati in `application/problem+json`.
- **RT-3 — Persistenza (§8).** Nessuna migrazione nuova. Il percorso gira sulla base di dati dello stack locale con
  le migrazioni Flyway vere e ripulisce i propri account alla fine.
- **RT-4 — Modulo frontend (§3, §5).** Il percorso attraversa le schermate dell'inventario dove serve — apertura,
  conteggio, elenco delle differenze, conferma — con selettori stabili e non con testi tradotti.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile nuovo; il percorso gira nella lingua predefinita `en` e non
  asserisce su stringhe tradotte.
- **RT-6 — Varchi e quota (§6, §7).** L'app è già attiva dal percorso `0036`. Il percorso verifica un punto che
  vale la pena bloccare: **la chiusura di un inventario non consuma quota e non può essere respinta con `429`**,
  perché genera movimenti su articoli che esistono già; il tetto `articoli_gestiti` colpisce solo la creazione di
  un articolo nuovo.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Il percorso esercita le stesse operazioni
  che gli strumenti `rettifica_giacenza` e `chiudi_inventario` prepareranno in bozza (storia `0035`), e serve
  quindi anche da rete di sicurezza per loro finché il server conversazionale di piattaforma non esiste
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. I dati sono **inventati**, con indirizzi di posta
  su dominio `*.test`. Il percorso verifica che l'autore del conteggio compaia sulla rettifica — è il dato che
  rende spiegabile una differenza — e **non** costruisce alcuna asserzione che confronti persone fra loro: nessun
  indicatore di produttività per operatore, mai (§6 della descrizione, art. 4 della legge 300/1970 — Statuto dei
  lavoratori).
- **RT-9 — Registrazione eventi (§14).** Il percorso verifica che apertura, chiusura e rettifiche generino righe di
  registro con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** il testo del motivo e
  senza le descrizioni degli articoli.

### Chiusura del registro di copertura

Il registro [`docs/testing/copertura-e2e.yaml`](../../../../testing/copertura-e2e.yaml) va lasciato **coerente**:
il controllo dell'area `tooling` fa fallire la suite se una storia con superficie applicativa non ha una voce.
Le storie di StockGrove che, per la loro natura, avevano risposto *rimando* si chiudono così:

| Storie che avevano risposto *rimando* | Perché rimandavano | Voce che le chiude |
|---|---|---|
| `0001`, `0002`, `0004`, `0013` | fondamenta e modello dati: nessuna superficie utente al momento della scrittura | percorso `[J-MAGAZZINO]` della storia `0036`, che le attraversa tutte per arrivare al primo carico |
| `0014`, `0015`, `0017` | carico, scarico e storno provati per integrazione, senza catena completa | percorso `[J-MAGAZZINO]` della storia `0036` |
| `0021`, `0022`, `0023`, `0024` | rettifica, sessione di conteggio, chiusura e ricostruzione, provate a pezzi | percorso `[J-MAGAZZINO]` di questa storia |
| `0034`, `0035` | contratto degli strumenti senza un server che li esponga | esenzione con categoria «dipendenza di piattaforma non implementata» e rimando a UC 0061-0063, più la copertura indiretta delle stesse operazioni dai percorsi `0036` e `0037` |
| storie che restano coperte solo per integrazione (trasferimento, movimenti da eventi, evento «giacenza variata», importazioni, scorte, riordino, scansione, etichette) | portarle in un percorso end-to-end lo renderebbe lento e fragile senza aggiungere garanzia | voce `da-coprire` con motivo scritto e storia proprietaria dichiarata, oppure esenzione motivata caso per caso |

**Regola d'onestà applicata a questa tabella.** L'elenco qui sopra è quello previsto al momento della scrittura;
prima di chiudere il registro va **riletta la sezione «Definizione di fatto» di tutte e trentasette le storie**,
perché la risposta alla domanda di copertura appartiene a ciascuna storia e può essere cambiata durante
l'implementazione. Una voce inventata in questo registro è peggio di una voce mancante: il controllo automatico
resterebbe verde su una copertura che non esiste.

## 4. Criteri di accettazione

**CA-1 — L'atteso si congela all'apertura**
- **Dato** un articolo con 9 pezzi nel deposito «Magazzino centrale» e una sessione di inventario appena aperta
- **Quando** un altro utente registra uno scarico da 1 mentre la sessione è aperta
- **Allora** la quantità attesa della riga d'inventario resta **9**, quella congelata all'apertura, e la
  divergenza è visibile alla chiusura invece di sparire

**CA-2 — Le differenze si vedono prima di confermare**
- **Dato** una sessione con l'articolo contato a **6** contro un atteso di **9**
- **Quando** si apre l'elenco delle differenze
- **Allora** compare la riga con atteso 9, contato 6 e scarto −3; nel registro **non** c'è ancora alcuna rettifica
  e la giacenza è ancora 9

**CA-3 — Chiusura senza motivo**
- **Dato** la stessa sessione · **Quando** si tenta di chiuderla senza scegliere un motivo per la differenza
- **Allora** la risposta è `400` in `application/problem+json`, la sessione resta aperta e nulla viene rettificato

**CA-4 — Chiusura con motivo: una rettifica tracciata**
- **Dato** la sessione con il motivo «rottura non registrata» scelto
- **Quando** la si chiude
- **Allora** nasce **un** movimento di rettifica `−3` con quel motivo e con l'autore che ha contato, la giacenza è
  **6**, il carico originario è ancora presente e invariato, e la sessione risulta chiusa

**CA-5 — La proiezione dice la verità**
- **Dato** il registro con carico, scarico e rettifica
- **Quando** si esegue la ricostruzione della giacenza dal registro
- **Allora** la somma algebrica dei movimenti coincide con la giacenza pubblicata per ogni coppia
  articolo-deposito, e il contatore delle divergenze è **zero**

**CA-6 — Il registro di copertura è coerente**
- **Dato** il registro [`docs/testing/copertura-e2e.yaml`](../../../../testing/copertura-e2e.yaml) aggiornato con
  le voci di StockGrove
- **Quando** si esegue `./run-tests.sh tooling`
- **Allora** il controllo del registro è verde e nessuna storia dell'applicazione resta senza risposta alla domanda
  di copertura

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `platform` e `tooling`; l'intera suite prima del commit);
- [ ] prove di **unità** e di **integrazione** già esistenti dalle storie `0021`-`0024`: qui non si duplicano;
- [ ] prova di **isolamento fra account** sulla sessione di inventario, compresa dentro il percorso;
- [ ] **prova end-to-end**: *coperta ora* — secondo gruppo di test `[J-MAGAZZINO]` in
      `tools/platform-e2e/journeys/J-MAGAZZINO.spec.ts`, e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) **chiuso** per l'intera
      applicazione secondo la tabella del §3;
- [ ] **traduzioni**: nessun testo visibile nuovo;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che i dati di prova siano inventati e su domini
      `*.test`;
- [ ] **registro delle decisioni** compilato, con la scelta di congelare l'atteso all'apertura e con l'elenco delle
      storie chiuse in copertura;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova esposta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: [docs/testing/README.md](../../../../testing/README.md) e il §8 della descrizione
      dell'applicazione riflettono la copertura raggiunta.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0036` | Il file del percorso, l'account di prova e l'attivazione dell'app esistono già: qui si aggiunge il secondo gruppo di test |
| `0021` | La rettifica con motivo obbligatorio è ciò che la chiusura genera |
| `0022` | La sessione di inventario con l'atteso congelato è il punto di partenza |
| `0023` | L'elenco delle differenze mostrato prima di confermare è il comportamento verificato |
| `0024` | La ricostruzione della giacenza dal registro è il controllo finale del percorso |

## 7. Fuori ambito

- **Il valore gestionale delle giacenze** (storia `0025`): il percorso non asserisce su importi, perché il nome
  della grandezza nelle cinque lingue è ancora un punto aperto e una asserzione su un'etichetta destinata a
  cambiare renderebbe fragile il collaudo.
- **Il conteggio in più mani con due operatori simultanei sulla stessa sessione**: resta coperto dalle prove di
  integrazione della storia `0022`; portarlo qui raddoppierebbe la durata del percorso.
- **La scansione dal telefono durante il conteggio** (epica 06): coperta dalle sue prove; il percorso conta per via
  programmatica.
- **La riparazione della proiezione quando diverge**: il percorso verifica che le due coincidano; il caso in cui
  non coincidono è provato per integrazione nella storia `0024`, dove si può forzare la divergenza.

## 8. Punti aperti

- **Se il percorso end-to-end debba includere anche il caso della divergenza riparata.** Sarebbe la prova più
  convincente del rischio esistenziale dell'applicazione, ma richiede di corrompere di proposito la proiezione
  dall'esterno, e non è detto che lo stack locale lo consenta in modo pulito. Decisione da prendere
  all'implementazione: se non è pulito, resta una prova di integrazione e lo si scrive nel registro delle
  decisioni.
- **La forma delle esenzioni nel registro di copertura** per le storie `0034` e `0035`: la categoria «dipendenza di
  piattaforma non implementata» va verificata contro le categorie ammesse dal registro reale
  ([docs/testing/README.md](../../../../testing/README.md)) al momento dell'implementazione, non data per buona.
