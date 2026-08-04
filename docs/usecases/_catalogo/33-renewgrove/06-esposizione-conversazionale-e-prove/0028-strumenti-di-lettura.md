# 0028 — Strumenti di lettura

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0027` — la spiegazione del punteggio e il rendiconto sono le due risposte che valgono la chat
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che apre una chat il lunedì mattina e chiede «chi rischio di perdere questo mese, e perché lo dici?»
> voglio che l'assistente risponda con i clienti in ordine di rischio e, per ciascuno, con i fatti datati che hanno
> formato il giudizio
> così da decidere chi chiamare prima senza aprire nessuna schermata, e da poter contraddire subito una risposta che
> non torna.

**Contesto.** Il catalogo pone a tutte le applicazioni il requisito di essere comandabili da una chat; per questa
app la superficie conversazionale non è un comodo in più, è la forma naturale della domanda. La
[descrizione](../application-description.md) al §7 lo dice esattamente: la domanda vera del titolare non è «chi
rischio di perdere» — quella la mostra già un elenco — ma **«perché dici così?»**. È anche la domanda che la
sentenza C-634/21 trasforma in un obbligo (§2.3): informazioni significative sulla logica utilizzata. Perciò
`spiega_punteggio` è lo strumento che rende questa app utile, e va progettato per rispondere a una persona, non per
riempire un oggetto strutturato. Lo stato reale da dire subito: **il livello conversazionale non esiste ancora nel
repository** (epica `12-ready-for-ai-mcp`, UC 0061-0066); qui si dichiara il **contratto**, che vive dentro il
servizio dell'app e viene versionato con esso.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara i **cinque strumenti di lettura** della tabella §7 della descrizione —
   `elenca_rapporti_a_rischio`, `spiega_punteggio`, `stato_rapporto`, `salute_delle_fonti`,
   `efficacia_degli_interventi` — ciascuno con: **nome stabile**, **descrizione in lingua naturale** che dica quando
   usarlo, **schema dei parametri**, **schema del risultato**, marcatura **lettura** e dichiarazione di
   **idempotenza** (stessa domanda, stessa risposta, nessun effetto).
2. **RF-2** — Ogni schema di risultato è **minimizzato per elenco chiuso**: i campi restituiti sono enumerati uno per
   uno e ciò che non è dichiarato non esce. In particolare non escono mai: contenuti di documenti, importi delle
   fatture d'origine (il segnale porta un'**intensità**, non l'importo), recapiti (che l'app non conserva, §4.3 via
   A), campi di testo libero.
3. **RF-3** — `spiega_punteggio(rapporto)` restituisce: valore, fascia, **versione del modello** che l'ha prodotto,
   i **contributi ordinati per peso** con il verso di ciascuno, i **fatti datati** che li hanno generati (tipo di
   segnale, fonte, momento, intensità) e **che cosa farebbe scendere il punteggio**. I segnali marcati *non
   pertinenti* (`0015`) compaiono come **esclusi con la loro ragione**, non spariscono: una spiegazione da cui i
   fatti scompaiono in silenzio non è contestabile.
4. **RF-4** — `salute_delle_fonti()` restituisce, per fonte, stato, momento dell'ultimo segnale e ritardo rispetto a
   quello atteso (`0011`); e **ogni** risultato degli altri quattro strumenti che dipenda da una fonte in silenzio
   porta il **contrassegno di incompletezza prima della cifra**, non dopo.
5. **RF-5** — `efficacia_degli_interventi(periodo, tipo?)` restituisce i conteggi del rendiconto (`0027`) e
   **sempre**, nello stesso risultato, i tre limiti dichiarati — nessun rapporto di causa ed effetto, numeri
   piccoli, gruppo non casuale. In una chat un numero si stacca dal suo contesto con un copia-incolla: il limite
   deve viaggiare **dentro** il risultato, non accanto.
6. **RF-6** — Gli strumenti **non aggirano nulla**: attraversano gli stessi varchi delle rotte web — `tenant_id` dal
   token verificato della chiamata, `402` con abbonamento non attivo, `403` per ruolo insufficiente — e **nessuno di
   essi consuma quota**, perché `rapporti_sorvegliati` è una metrica a giacenza che si consuma alla nascita del
   rapporto (`0009`), non alla lettura.
7. **RF-7** — Una **prova automatica** verifica che l'elenco degli strumenti dichiarati e l'elenco degli strumenti
   implementati coincidano, e che ogni schema di risultato dichiarato corrisponda a ciò che l'implementazione
   restituisce davvero: un contratto che diverge dal codice è peggio di un contratto assente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni strumento legge con `WHERE tenant_id = :tid`, dove `:tid` viene dal
  token verificato di **chi ha fatto la domanda**; nessun parametro di strumento può contenere un identificativo di
  account, e uno che vi arrivasse verrebbe ignorato. Un identificativo di rapporto di un altro account produce lo
  stesso «non trovato» che produce dall'interfaccia web.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti riusano i casi d'uso già esposti dalle rotte
  `/api/fidelizzazione/v1/*`: nessuna logica di dominio duplicata dentro lo strato degli strumenti, che è solo
  adattamento e minimizzazione. Errori in `application/problem+json`; definizione OpenAPI invariata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: gli strumenti leggono ciò che le epiche 02-05 hanno scritto.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Esiste però una pagina di **sola lettura del
  contratto** dentro il modulo `fidelizzazione` — quali strumenti esistono, cosa restituiscono, cosa **non**
  restituiscono — con i soli token del sistema di design, in tema chiaro e scuro: chi affida un'app a un assistente
  deve poter leggere che cosa quell'assistente è in grado di vedere.
- **RT-5 — Cinque lingue (§4).** Le **descrizioni in lingua naturale** degli strumenti e i testi della pagina del
  **RT-4** passano dallo spazio-nomi `fidelizzazione` e sono presenti in `en, it, fr, es, de`. I **nomi** degli
  strumenti restano stabili e non tradotti: sono identificativi tecnici.
- **RT-6 — Varchi e quota (§6, §7).** Come al **RF-6**: catena dei varchi applicata per intero, nessun consumo di
  `rapporti_sorvegliati`. Il **costo in concessioni** dentro `efficacia_degli_interventi` segue la restrizione di
  ruolo della `0027`: un `member` riceve il risultato senza la parte economica e un rifiuto esplicito su di essa.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati, tutti marcati **lettura** e quindi **liberi,
  senza conferma**: `elenca_rapporti_a_rischio(fascia?, fonte?, entro_giorni?)`, `spiega_punteggio(rapporto)`,
  `stato_rapporto(rapporto)`, `salute_delle_fonti()`, `efficacia_degli_interventi(periodo, tipo?)`. Il contratto vive
  **dentro il servizio**, versionato con esso; il server conversazionale è di piattaforma e **non ancora
  implementato** (UC 0061-0063), perciò le prove esercitano il contratto **chiamando il servizio direttamente** — e
  va scritto nel registro delle decisioni che, quando il server arriverà, quel collegamento va rifatto passando da
  lui.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: gli strumenti non creano campi, ma **espongono
  su una superficie nuova** dati riferiti a clienti finali (etichetta del rapporto, fatti datati, giudizio). Due
  presidi: la minimizzazione per elenco chiuso del **RF-2**, e il divieto assoluto di registrare **contenuti di
  risultato** nei registri (§14) — si registra quale strumento, per quale account, con quale identificativo di
  correlazione, mai che cosa ha risposto. Nessuna voce nuova nel manifesto; il manifesto va però **riletto** in
  questa storia per verificare che ogni campo esposto sia già dichiarato.
- **RT-9 — Registrazione eventi (§14).** `strumento di lettura invocato (nome)`, `strumento negato per ruolo`,
  `strumento negato per abbonamento`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza
  etichette di rapporti, senza parametri che le contengano e senza il contenuto della risposta**.
- **RT-10 — Prove (§11).** Unità sulla minimizzazione (un campo aggiunto all'entità e non allo schema **non** esce);
  integrazione su ciascuno dei cinque strumenti con database effimero e migrazioni vere; prova di coerenza
  contratto↔implementazione (**RF-7**); prova di isolamento fra due account su ogni strumento.

## 4. Criteri di accettazione

**CA-1 — «Perché dici così?» ha una risposta**
- **Dato** un rapporto in fascia alta con cinque segnali, di cui uno marcato *non pertinente*
- **Quando** si invoca `spiega_punteggio` su quel rapporto
- **Allora** il risultato porta valore, fascia, versione del modello, i quattro contributi validi ordinati per peso
  con il verso, i fatti datati che li hanno prodotti, il segnale escluso con la sua ragione, e che cosa farebbe
  scendere il punteggio

**CA-2 — Minimizzazione verificata**
- **Dato** un campo nuovo aggiunto all'entità `rapporto` e **non** allo schema del risultato
- **Quando** si invoca `stato_rapporto`
- **Allora** quel campo non compare nel risultato, e la prova di minimizzazione resta verde

**CA-3 — Il limite viaggia col numero**
- **Dato** un periodo con confronto disponibile
- **Quando** si invoca `efficacia_degli_interventi`
- **Allora** il risultato contiene i conteggi **e** i tre limiti dichiarati nello stesso oggetto, in una qualunque
  delle cinque lingue

**CA-4 — Incompletezza prima della cifra**
- **Dato** una fonte in silenzio oltre il proprio ritardo atteso
- **Quando** si invocano `elenca_rapporti_a_rischio` e `spiega_punteggio` su rapporti che ne dipendono
- **Allora** entrambi i risultati portano il contrassegno di incompletezza prima del valore, e
  `salute_delle_fonti` indica quella fonte con il suo ritardo

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri rapporti
- **Quando** un utente di `A` invoca `spiega_punteggio` passando l'identificativo di un rapporto di `B`
- **Allora** riceve «non trovato» e nessun dato di `B`, esattamente come dall'interfaccia web

**CA-6 — Il contratto non diverge dal codice**
- **Dato** uno strumento dichiarato e rimosso dall'implementazione (o viceversa)
- **Quando** si esegue `./run-tests.sh backend`
- **Allora** la suite è rossa, con l'indicazione dello strumento incoerente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla minimizzazione e di **integrazione** su tutti e cinque gli strumenti, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su ciascuno strumento;
- [ ] **prova end-to-end**: *rimando* — il server conversazionale non esiste (UC 0061-0063), quindi il percorso
      `[J-FIDELIZZAZIONE]` della storia `0030` esercita il contratto chiamando il servizio direttamente; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta la voce `da-coprire` con
      motivo («livello conversazionale di piattaforma non implementato») e storia proprietaria `0030`;
- [ ] **traduzioni** delle descrizioni degli strumenti e della pagina del contratto in `en, it, fr, es, de`;
- [ ] **manifesto dei dati**: nessuna voce nuova, ma riletto e verificato che ogni campo esposto sia già dichiarato;
- [ ] **registro delle decisioni** compilato: elenco chiuso dei campi restituiti da ciascuno strumento, divieto di
      registrare il contenuto delle risposte, nota che il collegamento andrà rifatto quando il server esisterà;
- [ ] contratto degli **strumenti conversazionali** dichiarato per i cinque strumenti di lettura, con prova di
      coerenza contratto↔implementazione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la descrizione elenca gli strumenti (§7).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` (spiegazione del punteggio) | `spiega_punteggio` non calcola nulla di nuovo: adatta e minimizza ciò che la `0014` produce |
| storia `0027` (rendiconto dell'efficacia) | `efficacia_degli_interventi` legge i conteggi e i limiti già calcolati là |
| storie `0011`, `0013`, `0015` | ritardo delle fonti, serie storica del punteggio e contestazioni sono ciò che i risultati devono saper mostrare |
| UC 0061-0063 (livello conversazionale di piattaforma) | **non implementati**: il server, l'autenticazione delegata e la mappatura operazioni→strumenti sono di piattaforma. Nel frattempo il contratto vive nel servizio e le prove lo chiamano direttamente |

## 7. Fuori ambito

- la **costruzione del server conversazionale**, l'autenticazione con consenso delegato e l'applicazione della quota
  alle chiamate dell'assistente: sono di piattaforma (UC 0061-0064), non di un'app;
- gli **strumenti di scrittura**: storia `0029`;
- la **formulazione del testo** con cui l'assistente presenta la spiegazione: è del livello conversazionale. Qui si
  garantisce che i fatti ci siano tutti e siano datati;
- qualunque strumento sui **diritti dell'interessato** (esportazione, cancellazione): non esiste e non deve esistere
  — è la stessa scelta motivata della storia `0032`.

## 8. Punti aperti

- **Quanti rapporti restituisce `elenca_rapporti_a_rischio` per impostazione predefinita.** In una chat un elenco
  lungo diventa illeggibile e, insieme, è un'esposizione di molte etichette di clienti finali in un colpo solo. La
  proposta è un valore predefinito basso con paginazione esplicita, ma il numero è una scelta di prodotto. Chiude:
  **sviluppatore** — direzione di prodotto.
- **Se la spiegazione debba essere consultabile dall'assistente anche per un rapporto archiviato.** Serve a
  rispondere a un reclamo («perché sei mesi fa mi avevate messo a rischio?»), ma allunga la vita utile di un
  giudizio su una persona. Chiude: **sviluppatore** (dati personali), coerentemente con la conservazione a 24 mesi
  del punto aperto n. 9 della [descrizione](../application-description.md).
