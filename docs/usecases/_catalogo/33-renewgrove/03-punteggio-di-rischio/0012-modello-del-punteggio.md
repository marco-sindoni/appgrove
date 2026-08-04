# 0012 — Modello del punteggio

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 03 — Punteggio di rischio spiegabile e contestabile
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vuole capire prima di fidarsi
> voglio che le regole con cui l'app decide chi è a rischio siano scritte, visibili e versionate
> così da poter dire, davanti a un cliente o a un mio collaboratore, **perché** l'app pensa quello che pensa.

**Contesto.** Oggi, chiusa l'epica 02, i fatti arrivano ma nessuno li compone: c'è una pila di segnali datati e
nessun numero. Questa storia mette al mondo l'oggetto che li trasforma in un giudizio, e lo fa nella forma che la
[descrizione](../application-description.md) §6 impone come requisito e non come raccomandazione: **regole
dichiarate con pesi visibili, non un modello addestrato**. La ragione è giuridica prima che tecnica. Secondo la
sentenza della Corte di giustizia dell'Unione europea del 7 dicembre 2023, causa C-634/21, un punteggio calcolato
in automatico che determina «in modo decisivo» la sorte di un rapporto contrattuale è una decisione automatizzata
ai sensi dell'articolo 22 del regolamento europeo sulla protezione dei dati, e chi lo calcola deve saper fornire
«informazioni significative sulla logica utilizzata» (§2.3). Un modello addestrato non le fornisce: fornisce, al
massimo, una ricostruzione a posteriori. Qui la spiegabilità viene **prima** della precisione, ed è una limitazione
di prodotto accettata a occhi aperti.

La seconda scelta di forma viene dall'analisi in rete (§2.5): il segnale utile non è il valore assoluto ma lo
**scostamento dalla linea di base di quel singolo rapporto** — un cliente che apre due segnalazioni al mese da
sempre non è in crisi, uno che ne apre due dopo due anni di silenzio sì — e conta la **tendenza su una finestra
dichiarata**, non il singolo giorno.

## 2. Requisiti funzionali

1. **RF-1** — Esiste l'entità `ModelloDiPunteggio`, **versionata**, con tre stati: `bozza` (modificabile, non
   calcola nulla), `vivo` (uno solo per account, è quello con cui si calcola), `archiviato` (non calcola più, ma
   resta leggibile perché i punteggi già calcolati lo citano).
2. **RF-2** — Un modello è composto di **voci**, una per tipo di segnale, ciascuna con quattro attributi
   obbligatori: **tipo di segnale** (preso dall'elenco chiuso della storia `0006`), **peso** numerico, **verso**
   (`alza il rischio` / `abbassa il rischio`), **finestra di osservazione** in giorni.
3. **RF-3** — Una voce lavora sullo **scostamento dalla linea di base del rapporto** (quanto il comportamento
   della finestra si discosta dal comportamento abituale di *quel* rapporto), non su una soglia assoluta uguale
   per tutti; per un rapporto senza linea di base sufficiente la voce non contribuisce e lo dichiara.
4. **RF-4** — Il modello dichiara le **soglie delle fasce**: `in salute`, `attenzione`, `a rischio`. Le fasce sono
   tre, non di più: un cruscotto a cinque livelli è esattamente ciò che il segmento rifiuta (§2.5).
5. **RF-5** — Ogni account nasce con un **modello predefinito** già `vivo`, popolato con i pesi di partenza, così
   che l'app produca un numero dal primo giorno senza chiedere una configurazione.
6. **RF-6** — La schermata del modello dichiara a chiare lettere, non in una nota a piè di pagina, che **i pesi di
   partenza sono una convenzione e non una stima**: non esistono pesi validati per imprese che non vendono
   software (§2.7), e il modo per correggerli è la taratura (`0016`) letta insieme al rendiconto di efficacia
   (`0027`).
7. **RF-7** — Un modello `vivo` **non si modifica**: qualunque cambiamento produce una versione nuova in stato
   `bozza`. È la storia `0016` a governare il passaggio; qui si costruisce il vincolo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `modello_di_punteggio` e delle sue voci
  filtra per `tenant_id` preso dal token di accesso verificato; un `tenant_id` che arrivasse dal corpo della
  richiesta o dai parametri viene ignorato. Il modello di un account non è visibile né riusabile da un altro:
  il modello predefinito è un **seme applicativo**, non una riga condivisa.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/fidelizzazione/v1/modelli`,
  `GET /api/fidelizzazione/v1/modelli/{id}`, `GET /api/fidelizzazione/v1/modelli/vivo`; corpo validato con
  vincoli dichiarativi (peso entro un intervallo chiuso, finestra fra 7 e 365 giorni, tipo di segnale appartenente
  all'elenco chiuso); errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
  La creazione di una versione nuova è della storia `0016`.
- **RT-3 — Persistenza (§8).** Migrazione `V9__modello_di_punteggio.sql` sullo schema `app_fidelizzazione`:
  tabelle `modello_di_punteggio` (con `tenant_id`, numero di versione, stato, chi e quando) e `voce_modello`
  (tipo di segnale, peso, verso, finestra), entrambe con chiave primaria UUID versione 7, colonne di controllo
  `created_at`, `updated_at`, `created_by`, `updated_by` e cancellazione logica `deleted_at`. Vincolo di unicità:
  **un solo modello `vivo` per account**. Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Modello` del modulo `fidelizzazione`, in sola lettura in questa
  storia: elenco delle voci con peso, verso e finestra, soglie delle fasce, versione e stato. Dati letti con il
  client generato dalla definizione OpenAPI; solo token del sistema di design (colore-categoria `teal`); funziona
  in tema chiaro e scuro. Il rischio si colora con i neutri e con i colori-categoria disponibili: `green`, `amber`
  e `red` restano riservati alle fasce e non entrano nell'insegna dell'app (§3).
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi dei tipi di segnale, nomi delle fasce, il testo
  che dichiara la natura convenzionale dei pesi — passano dallo spazio-nomi `fidelizzazione` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401` senza token valido, `403` ad app spenta,
  `402` ad account non abilitato o abbonamento `canceled`, `403` a ruolo insufficiente. La lettura del modello è
  aperta a `member`; la modifica è di `owner` e `admin` (storia `0016`). **Nessun consumo di quota**: la metrica
  `rapporti_sorvegliati` (natura `stock`) si consuma quando un rapporto entra in sorveglianza (`0009`), non
  quando si legge un modello.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento nuovo**, e la scelta è motivata: il modello è
  configurazione, e la tabella degli strumenti (§7 della descrizione) non ne prevede uno perché nessuno tara i
  pesi da una chat. La versione del modello compare però nel risultato di `spiega_punteggio` (storia `0014`), che
  senza di essa non sarebbe una spiegazione ma un'opinione. Dipendenza dichiarata: il livello conversazionale è di
  piattaforma e **non è ancora implementato** (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** `modello_di_punteggio` e `voce_modello`
  contengono regole e numeri, non fatti riferiti a persone: restano fuori da `exportData` e `purgeData` di
  `FidelizzazioneDataContract`, coerentemente con l'elenco del §6 della descrizione. Il momento in cui i dati
  personali entrano in gioco è il calcolo (`0013`), non la regola.
- **RT-9 — Registrazione eventi (§14).** `modello creato`, `modello reso vivo`, `modello archiviato`, con
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e numero di versione; senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Un account nuovo ha già un modello vivo**
- **Dato** un account appena abilitato a `fidelizzazione`, senza alcuna configurazione fatta
- **Quando** apre la sezione Modello
- **Allora** vede un modello in stato `vivo`, versione 1, con le sue voci (tipo di segnale, peso, verso, finestra)
  e le tre soglie di fascia, e legge l'avvertenza che i pesi di partenza sono una convenzione dichiarata

**CA-2 — Una voce dichiara scostamento e finestra, non una soglia assoluta**
- **Dato** il modello vivo di un account
- **Quando** si legge la voce relativa al tipo di segnale «segnalazione di assistenza riaperta»
- **Allora** la voce riporta la finestra di osservazione in giorni e il fatto che il contributo si misura come
  scostamento dalla linea di base del singolo rapporto, non come conteggio assoluto

**CA-3 — Il modello vivo non si modifica**
- **Dato** un modello in stato `vivo`
- **Quando** si tenta di cambiarne un peso con una scrittura diretta sulla risorsa
- **Allora** la richiesta è respinta con `409` e un messaggio in `problem+json` che indica il rimedio: creare una
  versione nuova (storia `0016`); il modello vivo resta immutato

**CA-4 — Un solo modello vivo per account**
- **Dato** un account con un modello `vivo` alla versione 1
- **Quando** una seconda riga in stato `vivo` viene tentata per lo stesso account
- **Allora** la scrittura fallisce sul vincolo di unicità e nessuna delle due righe resta in uno stato incoerente

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con il proprio modello
- **Quando** un utente di `A` chiede il modello forzando nella richiesta l'identificativo dell'account `B`
- **Allora** riceve il modello di `A`, e nessun dato di `B` compare nella risposta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione delle voci (peso, verso, finestra, tipo di segnale ammesso) e sul
      vincolo «un solo modello vivo»; prove di **integrazione** sulla risorsa, con database effimero e migrazioni
      Flyway vere;
- [ ] prova di **isolamento fra account** sulla risorsa dei modelli;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-FIDELIZZAZIONE]` nasce nella storia `0030`, dove dovrà
      attraversare la lettura del modello vivo come primo passo del tratto «punteggio»; qui si registra la voce
      `da-coprire` con motivo «percorso non ancora creato» e storia proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, e la scelta è scritta nel registro delle decisioni — il modello
      non contiene dati riferiti a persone;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato con: perché regole dichiarate e non
      modello addestrato, perché scostamento dalla linea di base e non soglia assoluta, perché tre fasce, e da
      dove vengono i pesi di partenza;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: la descrizione dell'applicazione §4.1 riflette gli stati e gli attributi
      effettivamente implementati.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` — contratto del segnale | i tipi di segnale che una voce può citare vengono dall'elenco chiuso; senza, il modello riferirebbe tipi inesistenti |
| storia `0009` — il rapporto sorvegliato | la linea di base è una proprietà del rapporto: senza rapporti non c'è nulla da cui misurare uno scostamento |
| epica di piattaforma non implementata, UC 0061-0063 | il livello conversazionale non esiste: qui non serve alcuno strumento, ma la versione del modello va tenuta nel risultato di `spiega_punteggio` quando quello strumento arriverà (`0014`) |

## 7. Fuori ambito

- **il calcolo** e la serie storica dei punteggi: storia `0013`. Qui si costruisce la regola, non il risultato;
- **la modifica dei pesi da parte del cliente** con anteprima dell'effetto: storia `0016`;
- **la spiegazione** mostrata a chi legge: storia `0014`;
- **la validazione statistica dei pesi**: deliberatamente rimandata, e non a una storia — non esistono dati su cui
  validarla finché l'epica 05 non ha misurato esiti veri (`0027`). Fingere una taratura oggi sarebbe inventare;
- **un modello addestrato**: escluso per scelta, non rimandato. Rientrerebbe solo se cambiasse la postura sulla
  spiegabilità, che è una decisione di prodotto e di conformità insieme.

## 8. Punti aperti

- **I valori numerici dei pesi di partenza.** Sono una convenzione dichiarata (§2.7): l'unica fonte trovata parla
  di aziende di software con dati di utilizzo del prodotto, che il nostro cliente non ha. **Proposta**: ordinare i
  pesi secondo la gerarchia rilevata — esiti di pagamento, andamento delle segnalazioni di assistenza, calo del
  ritmo di acquisto rispetto alla linea di base, completezza dell'avvio del rapporto nei primi novanta giorni — e
  scrivere nel registro delle decisioni che l'ordine viene da lì e i valori no. Chiude: **sviluppatore**, con la
  direzione di prodotto.
- **Se il punteggio ricada nel regolamento europeo sull'intelligenza artificiale.** L'allegato applicabile non è
  stato verificato (§2.7). La forma a regole dichiarate scelta qui è, fra le due possibili, quella che regge
  meglio qualunque esito. Chiude: **revisione legale** — punto aperto n. 6 della descrizione.
