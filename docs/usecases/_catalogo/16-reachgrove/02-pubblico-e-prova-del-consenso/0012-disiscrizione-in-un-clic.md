# 0012 — Disiscrizione in un clic

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 02 — Pubblico e prova del consenso
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che riceve un messaggio commerciale e non lo vuole più
> voglio uscire dalla lista con un solo gesto, senza dover entrare da nessuna parte e senza rispondere a domande
> così da non dover ricorrere al pulsante «segnala come posta indesiderata», che è l'alternativa che ho.

**Contesto.** La disiscrizione è insieme un diritto e una difesa. È un diritto perché il consenso è revocabile in
ogni momento ([application-description.md](../application-description.md) §2.3 punto 1). Ed è la difesa più
efficace della recapitabilità, perché l'unica alternativa che il destinatario ha, se non la trova, è segnalare il
messaggio come posta indesiderata — e il tasso di segnalazione oltre lo 0,3 % fa **respingere** i messaggi di
tutti gli account (§2.3 punto 5). I grandi fornitori di posta lo hanno reso un requisito tecnico: chi manda in
volume deve offrire la disiscrizione in un clic dentro il messaggio, secondo la specifica RFC 8058, e onorarla
**entro due giorni**. Questa storia la implementa alla lettera e poi fa una cosa in più: la onora **subito**,
perché due giorni sono un termine massimo, non un obiettivo.

## 2. Requisiti funzionali

1. **RF-1** — Ogni messaggio commerciale in uscita porta **due** vie di disiscrizione: un collegamento visibile nel
   piè di pagina e le intestazioni tecniche di disiscrizione in un clic previste dalla specifica RFC 8058, che
   permettono al programma di posta del destinatario di mostrare un proprio pulsante.
2. **RF-2** — Nessuna delle due si può disattivare: non c'è un'opzione nel messaggio, nel modello, nella campagna
   né nelle impostazioni dell'account. Il piè di pagina identifica inoltre il mittente, come richiede una
   comunicazione commerciale.
3. **RF-3** — La disiscrizione è **immediata e senza domande**: un solo gesto, nessuna pagina intermedia
   obbligatoria, nessuna richiesta di accedere, nessun modulo da compilare. La pagina di esito può *offrire* di
   modificare le preferenze o di dire perché, ma solo **dopo** che la disiscrizione è già avvenuta.
4. **RF-4** — La disiscrizione produce due effetti nello stesso atto: una registrazione di consenso con esito
   `revocato` (storia 0007) e una voce nell'elenco di soppressione con motivo `disiscrizione` (storia 0011).
5. **RF-5** — L'operazione è idempotente: ripetuta più volte non crea registrazioni doppie e risponde sempre nello
   stesso modo. Il collegamento identifica destinatario e campagna, non è indovinabile e non scade — un messaggio
   ricevuto un anno fa deve restare disattivabile.
6. **RF-6** — La disiscrizione è **per canale**: chi esce dalla posta elettronica non esce automaticamente dai
   messaggi brevi, e viceversa; la pagina di esito dice con chiarezza da cosa la persona è uscita e da cosa no.
7. **RF-7** — L'effetto è **istantaneo sulle spedizioni in corso**: una campagna che sta consegnando in quel
   momento non gli manda il messaggio (il meccanismo è la storia 0020).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il collegamento di disiscrizione è firmato e risolve da solo account,
  iscritto, canale e campagna: **niente** si legge da un parametro modificabile. Un collegamento manomesso non
  disiscrive nessuno e non rivela niente.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte pubbliche, senza token di accesso:
  `POST /api/campaigns/v1/public/unsubscribe/{token}` — usata sia dalla pagina sia dal comando in un clic dei
  programmi di posta, che invia una richiesta `POST` all'indirizzo indicato nell'intestazione — e
  `GET /api/campaigns/v1/public/unsubscribe/{token}` per la pagina di esito. Il metodo `GET` **non** produce da
  solo la disiscrizione quando è aperto da un anticipatore automatico di collegamenti: la distinzione si gestisce
  come previsto dalla specifica, e la pagina conferma con un gesto quando serve. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si scrive in `consent_record` e in `suppression`. Si
  aggiunge alla consegna (`delivery`) il riferimento al segno di disiscrizione, conservato come impronta
  crittografica e non in chiaro.
- **RT-4 — Modulo frontend (§3, §5).** Pagina pubblica di esito con i token del sistema di design, tema chiaro e
  scuro, nessuna risorsa da domini di terzi. Nell'app: la disiscrizione compare nello storico dell'iscritto e nel
  rapporto della campagna. Il piè di pagina obbligatorio è mostrato nell'anteprima del messaggio (storia 0015)
  così che il cliente veda che c'è e non provi a toglierlo.
- **RT-5 — Cinque lingue (§4).** Il piè di pagina e la pagina di esito escono nella **lingua dell'iscritto** fra
  `en, it, fr, es, de`. Una disiscrizione che il destinatario non capisce non è una disiscrizione.
- **RT-6 — Varchi e quota (§6, §7).** Le rotte pubbliche di disiscrizione funzionano **sempre**: anche con
  abbonamento `canceled`, anche con quota `messages_sent` esaurita, anche con l'app disabilitata per l'account. È
  la stessa logica per cui i diritti dell'interessato restano accessibili in ogni caso
  ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §13): non si può smettere di onorare un'opposizione
  perché il cliente non ha pagato.
- **RT-7 — Esposizione conversazionale (§12).** `disiscrivi(recapito, motivo)` è dichiarato nella storia 0035 come
  **scrittura irreversibile con conferma umana obbligatoria**: serve a onorare un'opposizione arrivata a voce o
  per altra via. Nessuno strumento annulla una disiscrizione.
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo: si scrive nelle voci già dichiarate
  `consent.record` e `suppression.contact`. Si aggiunge al manifesto la nota che la disiscrizione è **esercizio
  del diritto di opposizione** e che la sua prova si conserva.
- **RT-9 — Registrazione eventi (§14).** «Disiscrizione ricevuta» con `tenant_id`, `app_id`, identificativo
  dell'iscritto, canale, campagna d'origine e identificativo di correlazione; **mai** il recapito, **mai** il
  segno.

## 4. Criteri di accettazione

**CA-1 — Un solo gesto**
- **Dato** un messaggio ricevuto da un iscritto attivo
- **Quando** il destinatario usa il pulsante di disiscrizione del proprio programma di posta
- **Allora** entro pochi secondi risulta `disiscritto`, esiste una registrazione con esito `revocato` e il suo
  recapito è nell'elenco di soppressione con motivo `disiscrizione`, **senza** che gli sia stata chiesta alcuna
  altra azione

**CA-2 — Non si può togliere il collegamento**
- **Dato** un modello di messaggio in cui l'utente ha cancellato il piè di pagina
- **Quando** la campagna viene sottoposta al controllo pre-volo (storia 0018)
- **Allora** il controllo è rosso e bloccante, e il messaggio in uscita porta comunque il piè di pagina e le
  intestazioni tecniche

**CA-3 — Ripetere non raddoppia**
- **Dato** un iscritto già disiscritto
- **Quando** il collegamento viene usato di nuovo, anche a distanza di mesi
- **Allora** la risposta è la stessa, non nasce una seconda registrazione e non nasce una seconda soppressione

**CA-4 — Il collegamento non è indovinabile e non rivela nulla**
- **Dato** un segno manomesso
- **Quando** lo si usa
- **Allora** nessuno viene disiscritto e la risposta non dice se quel recapito esista in qualche account

**CA-5 — Funziona anche con l'abbonamento scaduto**
- **Dato** un account con abbonamento `canceled` e messaggi inviati in passato
- **Quando** un destinatario si disiscrive
- **Allora** la disiscrizione è registrata e onorata come sempre

**CA-6 — Per canale, non per persona**
- **Dato** un iscritto contattabile sia per posta elettronica sia per messaggi brevi
- **Quando** si disiscrive dal collegamento contenuto in un messaggio di posta elettronica
- **Allora** risulta `disiscritto` per la posta elettronica e ancora contattabile per i messaggi brevi, e la
  pagina di esito glielo dice in modo esplicito

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla firma e verifica del segno, sull'idempotenza e sulla separazione per canale; prove
      di **integrazione** sulle rotte pubbliche, compresa la richiesta in un clic inviata come `POST` dal
      programma di posta;
- [ ] prova di **isolamento fra account** e prova che un segno manomesso non produca effetti né informazioni;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) si chiude con la disiscrizione
      dal messaggio ricevuto e la verifica che la campagna successiva non lo raggiunga; voce aggiunta al registro
      di copertura;
- [ ] **traduzioni** del piè di pagina e della pagina di esito in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la nota sull'esercizio del diritto di opposizione;
- [ ] **registro delle decisioni** compilato, con annotato perché la disiscrizione è immediata e senza domande e
      perché le rotte pubbliche restano attive anche senza abbonamento;
- [ ] contratto degli **strumenti conversazionali**: `disiscrivi` marcato scrittura irreversibile con conferma
      obbligatoria (dichiarato nella storia 0035);
- [ ] controllo automatico di **accessibilità** verde sulla pagina pubblica di esito;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0011` | La disiscrizione scrive nell'elenco di soppressione |
| Storia `0007` | La revoca è una registrazione di consenso, non una cancellazione |
| Storia `0020` (epica 04) | Perché l'effetto sia davvero istantaneo serve che il controllo avvenga al momento della consegna del singolo messaggio: qui si crea l'effetto, là lo si onora durante una spedizione in corso |

## 7. Fuori ambito

- il centro delle preferenze in cui il destinatario sceglie **quali** comunicazioni ricevere: rimandato. Sarebbe
  utile, ma non deve mai diventare l'attrito che si frappone fra la persona e l'uscita: prima si esce, poi
  eventualmente si torna a scegliere;
- la disiscrizione rispondendo al messaggio con una parola chiave: appartiene ai canali dei messaggi brevi e della
  messaggistica (storie 0023 e 0024);
- il calcolo del tasso di segnalazione e il blocco che ne consegue: è la storia 0021.

## 8. Punti aperti

- Nessuno. È la storia meno discrezionale dell'app: i requisiti vengono da un obbligo di legge e da un requisito
  tecnico dei fornitori di posta, entrambi documentati (§2.3 punti 1 e 5 della descrizione), e non lasciano spazio
  a scelte di prodotto. L'unica cosa da non fare è renderla configurabile.
