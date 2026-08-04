# 0018 — Piani di intervento

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 04 — Interventi con conferma umana
**Storia**: `0018` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha imparato in vent'anni che cosa si dice a un cliente che sta scivolando via
> voglio scrivere una volta per tutte che cosa si fa quando un rapporto entra in fascia `a rischio`
> così da non dover ricominciare da capo ogni volta, e da poter far fare quella telefonata anche a chi non c'ero
> io a insegnargliela.

**Contesto.** L'epica 03 si chiude con un numero spiegato e contestabile, ma con nessuna azione. Il primo pezzo di
azione non è l'azione stessa: è il **modello** di ciò che si fa, scritto dal cliente **con le sue parole**. La
[descrizione](../application-description.md) §2.5 è netta su questo punto: il segmento rifiuta la configurazione a
regole («se lo stato è X e sono passati N giorni allora…») — è la stessa avversione già rilevata da SubGrove — e
rifiuta ancora di più i testi scritti da noi. Un titolare di micro-impresa conosce i suoi clienti per nome e non
manderà mai un messaggio che non suona come lui.

Un piano, quindi, **non è un automatismo**: è una traccia. Non scatta da solo, non parte da una soglia, non ha
condizioni di attivazione. È l'insieme dei passi consigliati per una fascia di rischio, con un testo di partenza
che una persona modificherà, e l'indicazione di chi deve autorizzare quando c'è di mezzo del denaro (`0022`).

## 2. Requisiti funzionali

1. **RF-1** — Esiste l'entità `PianoDiIntervento`: nome, fascia di rischio a cui si applica (`in salute`,
   `attenzione`, `a rischio`), elenco ordinato di **passi consigliati** e indicazione di **chi deve autorizzare**
   se il piano prevede una concessione economica.
2. **RF-2** — Ogni passo porta: che cosa fare (telefonare, scrivere, proporre un incontro, offrire una
   concessione), un **testo di partenza** scritto dal cliente, e un'indicazione di tempi consigliati («entro tre
   giorni»), che è un suggerimento a chi legge e **non** una pianificazione eseguita dal sistema.
3. **RF-3** — I piani si creano, si modificano, si duplicano e si **disattivano**; un piano disattivato non
   compare più fra quelli proponibili ma resta leggibile, perché gli interventi già preparati lo citano.
4. **RF-4** — Più piani possono applicarsi alla stessa fascia: quando si prepara un intervento (`0019`) chi lo
   prepara **sceglie**. Non esiste una selezione automatica del piano «giusto», perché sceglierlo è già una
   decisione sul cliente.
5. **RF-5** — Creare, modificare e disattivare un piano richiede ruolo `owner` o `admin`; un `member` li legge e
   li usa per preparare interventi.
6. **RF-6** — Al momento di dare un nome al piano compare un **avviso a schermo**: non usare il nome di una
   persona («piano Mario Rossi»). Il motivo è detto per esteso e non lasciato intuire — il nome del piano **non
   viene esportato** fra i dati dell'interessato, perché la tabella dei piani non contiene dati riferiti a clienti
   finali; un nome che ne contenesse uno sfuggirebbe all'esportazione e alla cancellazione (§6 della descrizione,
   punto aperto n. 8).
7. **RF-7** — Ogni account nasce con **due piani di esempio** già scritti, chiaramente marcati come esempi da
   riscrivere: uno per la fascia `attenzione` e uno per `a rischio`. Servono a mostrare la forma, non a essere
   usati così come sono.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `piano_di_intervento` e dei suoi passi
  filtra per `tenant_id` preso dal token di accesso verificato; un `tenant_id` che arrivasse dal corpo della
  richiesta o dai parametri viene ignorato. I piani di esempio sono un **seme applicativo** per account, non righe
  condivise fra account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/fidelizzazione/v1/piani` (con filtro per fascia
  e per stato), `POST /api/fidelizzazione/v1/piani`, `PUT /api/fidelizzazione/v1/piani/{id}`,
  `POST /api/fidelizzazione/v1/piani/{id}/disattivazione`; corpo validato (nome obbligatorio con lunghezza
  massima, fascia appartenente alle tre dichiarate dal modello, almeno un passo); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V14__piano_di_intervento.sql` sullo schema `app_fidelizzazione`:
  tabelle `piano_di_intervento` (`tenant_id`, nome, fascia, ruolo autorizzante, stato attivo/disattivato) e
  `passo_piano` (`tenant_id`, piano, ordine, tipo di azione, testo di partenza, tempo consigliato), con chiave
  primaria UUID versione 7, colonne di controllo `created_at`, `updated_at`, `created_by`, `updated_by` e
  cancellazione logica `deleted_at`. Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Piani` del modulo `fidelizzazione`: elenco per fascia, editor dei
  passi con riordino, avviso sul nome del piano mostrato **accanto al campo del nome** e non in fondo al modulo.
  Dati letti e scritti con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro;
  controllo automatico di accessibilità sulla schermata.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe dell'interfaccia — etichette, tipi di azione, avviso sul nome,
  testi dei piani di esempio — passano dallo spazio-nomi `fidelizzazione` e sono presenti in `en, it, fr, es, de`.
  I testi **scritti dal cliente** restano nella lingua in cui li ha scritti: non si traducono e non si riscrivono.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente — scrittura per `owner` e `admin`, lettura per
  `member`. **Nessun consumo di quota**: la metrica `rapporti_sorvegliati` (natura `stock`) conta i rapporti,
  non i piani, e il numero di piani è deliberatamente illimitato in ogni piano di listino.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento nuovo**: la tabella degli strumenti (§7 della
  descrizione) non prevede la scrittura dei piani, perché comporre il testo che si dirà a un cliente è
  precisamente il lavoro che il titolare vuole fare lui. I piani compaiono però come **parametro facoltativo** di
  `prepara_intervento(rapporto, piano?)` (storia `0019`), ed è per questo che il loro contratto — identificativo
  stabile e nome — va dichiarato qui. Il livello conversazionale è di piattaforma e **non è ancora implementato**
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo, con un'eccezione dichiarata e non risolta.**
  `piano_di_intervento` e `passo_piano` contengono testi scritti dal nostro utente su che cosa dire in generale, e
  non contengono riferimenti a clienti finali: restano fuori da `exportData` e `purgeData` di
  `FidelizzazioneDataContract`, coerentemente con l'elenco del §6. L'eccezione è il **nome del piano**: se un
  cliente vi mettesse il nome di una persona, un dato personale finirebbe in una tabella non esportata e non
  purgata. Il presidio adottato è l'avviso a schermo di RF-6, ed è **contrattuale, non tecnico**: non esiste un
  rilevamento automatico dei nomi di persona e non se ne inventa uno. Se l'avviso basti è il punto aperto n. 8. Lo
  stesso vale per il **testo di partenza**, che porta l'avvertenza di non inserire dati sulla salute né altre
  categorie particolari dell'articolo 9.
- **RT-9 — Registrazione eventi (§14).** `piano creato`, `piano modificato`, `piano disattivato`, con `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione e identificativo del piano; **mai il nome del piano** né i
  testi, proprio perché il nome potrebbe contenere un dato personale.

## 4. Criteri di accettazione

**CA-1 — Un account nuovo ha due piani di esempio**
- **Dato** un account appena abilitato a `fidelizzazione`
- **Quando** apre la sezione Piani
- **Allora** trova due piani marcati come esempi, uno per la fascia `attenzione` e uno per `a rischio`, ciascuno
  con i propri passi e testi di partenza, e l'invito a riscriverli con le proprie parole

**CA-2 — Un piano si scrive con le proprie parole**
- **Dato** un utente `admin`
- **Quando** crea un piano per la fascia `a rischio` con tre passi (telefonata entro 3 giorni, messaggio di
  riepilogo, proposta di incontro) e i rispettivi testi di partenza
- **Allora** il piano compare fra quelli proponibili per quella fascia, con i passi nell'ordine dato

**CA-3 — Avviso sul nome del piano**
- **Dato** un utente che sta creando un piano
- **Quando** posiziona il cursore sul campo del nome
- **Allora** vede accanto al campo l'avviso di non usare il nome di una persona, con il motivo («il nome del piano
  non entra nell'esportazione dei dati dell'interessato»), in tutte e cinque le lingue

**CA-4 — Un piano disattivato resta leggibile**
- **Dato** un piano disattivato e un intervento preparato in passato che lo citava
- **Quando** si apre quell'intervento
- **Allora** il piano d'origine è leggibile con i suoi passi, e non compare più fra quelli proponibili per un
  intervento nuovo

**CA-5 — Un `member` non scrive i piani**
- **Dato** un utente con ruolo `member`
- **Quando** tenta di creare o modificare un piano
- **Allora** riceve `403` in `problem+json`, e continua a vedere l'elenco e a poterlo usare per preparare un
  intervento

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri piani
- **Quando** un utente di `A` chiede l'elenco dei piani forzando nella richiesta l'identificativo di `B`
- **Allora** vede solo i piani di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione del piano (fascia ammessa, almeno un passo, ordine dei passi) e di
      **integrazione** sulla risorsa, con database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** sulla risorsa dei piani;
- [ ] prova sulla **matrice dei ruoli**: `owner` e `admin` scrivono, `member` legge;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e vi userà un
      piano come punto di partenza dell'intervento; voce `da-coprire` con motivo e storia proprietaria `0030` nel
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), avviso sul nome del piano
      compreso;
- [ ] **manifesto dei dati**: nessuna voce nuova, e l'**eccezione del nome del piano** scritta esplicitamente nel
      registro delle decisioni con il rimando al punto aperto n. 8;
- [ ] **registro delle decisioni** compilato con: perché un piano non è un automatismo, perché non esiste
      selezione automatica del piano, perché l'avviso sul nome è contrattuale e non tecnico;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; identificativo e nome del piano dichiarati
      come parametro di `prepara_intervento`;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` — modello del punteggio | le fasce a cui un piano si applica sono quelle dichiarate dal modello: senza, un piano si riferirebbe a fasce inesistenti |
| storia `0003` — guscio del modulo frontend | serve la sezione in cui la schermata dei piani vive |
| epica di piattaforma non implementata, UC 0061-0063 | i piani non hanno strumenti propri ma compaiono come parametro di `prepara_intervento`: il contratto va dichiarato ora perché sia stabile quando il livello arriverà |

## 7. Fuori ambito

- **preparare un intervento** a partire da un piano, e la sua macchina a stati: storia `0019`;
- **le offerte di trattenuta** e il loro tetto: storia `0022`. Qui il piano dice soltanto *chi deve autorizzare*,
  non quanto si può concedere;
- **l'attivazione automatica di un piano** al superamento di una soglia: **esclusa per scelta, non rimandata**. È
  il divieto della storia `0017`, e reintrodurla da qui sarebbe il modo più naturale di aggirarlo;
- **la generazione dei testi con un modello linguistico**: deliberatamente fuori. Il valore del piano è che suona
  come il titolare; un testo generato suona come tutti gli altri, ed è ciò che il §2.5 dice che il segmento
  rifiuta;
- **la traduzione automatica dei testi scritti dal cliente**: fuori, per la stessa ragione.

## 8. Punti aperti

- **Il nome di un piano di intervento come possibile dato personale.** L'avviso a schermo basta? La stessa
  questione è già stata sollevata da SubGrove sui nomi dei propri piani, e conviene che le due app rispondano allo
  stesso modo invece di divergere. **Raccomandazione**: avviso a schermo ora, e se si decidesse che non basta, la
  via è includere `piano_di_intervento` in `exportData` e `purgeData` con una voce di manifesto dedicata — non un
  rilevamento automatico dei nomi, che sarebbe un presidio finto. Chiude: **sviluppatore** — punto aperto n. 8
  della descrizione.
- **Se i piani di esempio debbano essere scritti da noi in cinque lingue.** Sono l'unico testo rivolto al cliente
  finale che nasce da appgrove, e questo li rende delicati: un esempio scritto male viene usato così com'è.
  **Raccomandazione**: due esempi brevi e volutamente incompleti, marcati come esempi, tradotti nelle cinque
  lingue dell'interfaccia. Chiude: **sviluppatore**, con la direzione di prodotto.
