# 0021 — Rettifica con motivo obbligatorio

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 04 — Inventario fisico, rettifiche e valore
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto al magazzino
> voglio dichiarare quanti pezzi ci sono davvero su uno scaffale, dicendo perché il numero non tornava
> così da riportare la giacenza alla realtà lasciando scritto il motivo, invece di correggere un numero in silenzio.

**Contesto.** Fino a qui il saldo cambia solo per fatti che si raccontano da soli: è entrata merce, è uscita merce,
è stata spostata. Ma il magazzino vero produce differenze che nessun documento spiega — un pezzo rotto e buttato,
una confezione trovata dietro un pallet, uno scarico registrato due volte il mese scorso. Serve una via per
riallineare il saldo, ed è il punto in cui l'applicazione rischia di suicidarsi: la via ovvia — una casella
«quantità» che si modifica — distruggerebbe tutto il modello costruito dalla storia `0013`. Questa storia apre
l'unica via ammessa e la chiude a chiave: **si dichiara la quantità reale, non si scrive il saldo**, e il motivo è
obbligatorio (descrizione dell'applicazione, §4, regola 2).

## 2. Requisiti funzionali

1. **RF-1** — Esiste un'operazione di rettifica che accetta articolo, deposito, **quantità reale contata** e motivo;
   il servizio calcola da sé il delta rispetto alla giacenza corrente e registra **un movimento di tipo
   `rettifica`** con quel delta. Chi chiama non fornisce mai il delta: fornisce ciò che ha visto.
2. **RF-2** — Il motivo è obbligatorio e scelto da un elenco chiuso di codici: `merce_trovata`, `merce_mancante`,
   `rottura`, `calo_naturale`, `errore_registrazione_precedente`, `altro`. Il codice `altro` richiede in più una
   **nota scritta non vuota**; un motivo assente, sconosciuto o `altro` senza nota è respinto e nulla viene scritto.
3. **RF-3** — Una rettifica che non cambia nulla (quantità reale uguale alla giacenza corrente) **non crea alcun
   movimento** e risponde con l'esito «nessuna differenza»: il registro non si sporca di righe a zero.
4. **RF-4** — La quantità reale non può essere negativa; una rettifica che porterebbe il saldo sotto zero è
   respinta con `422` e un messaggio che dice la giacenza corrente. La rettifica **non** è la porta di servizio per
   ottenere saldi negativi (descrizione, §11 punto 3).
5. **RF-5** — Una rettifica **non si modifica e non si cancella**: l'unica correzione è lo storno della storia
   `0017`, che aggiunge un movimento contrario e lascia leggibile la sequenza «ho rettificato, poi ho stornato».
6. **RF-6** — In nessuna schermata dell'applicazione esiste un campo «quantità in giacenza» modificabile: la
   sezione `giacenze` mostra il saldo in sola lettura e offre l'azione «rettifica», che apre il modulo con quantità
   reale, motivo e nota. È un requisito verificabile: una prova dell'interfaccia controlla che il valore della
   giacenza sia reso non modificabile.
7. **RF-7** — L'elenco dei motivi è tradotto e mostrato per etichetta, mai per codice; il motivo scelto e la nota
   restano visibili sul movimento nello storico della sezione `movimenti`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La rettifica legge la giacenza e scrive il movimento filtrando per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. Prova di isolamento fra due account sulla rotta delle rettifiche.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/magazzino/v1/rettifiche` con corpo
  `{articolo_id, deposito_id, quantita_reale, motivo, nota?, chiave_idempotenza}`; validazione dichiarativa;
  errori in `application/problem+json` (`422` per quantità o motivo non validi, `409` per conflitto di concorrenza);
  definizione OpenAPI aggiornata nello stesso commit. Nessuna rotta di modifica o di cancellazione: non esistono.
- **RT-3 — Persistenza (§8).** **Nessuna migrazione nuova**: la rettifica usa `movimento` e `motivo_movimento`
  create dalla storia `0013`. Questa storia aggiunge soltanto le righe dei sei codici di motivo predefiniti, che
  ogni account riceve alla prima apertura, e il vincolo che i codici con `richiede_nota` vero rifiutino la nota
  vuota.
- **RT-4 — Modulo frontend (§3, §5).** Azione «rettifica» nella sezione `giacenze` del modulo `magazzino`, con il
  saldo corrente mostrato accanto al campo della quantità reale perché si veda la differenza mentre si scrive; dati
  letti e scritti con il client generato; solo token del sistema di design, colore-categoria `amber`; funziona in
  tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le etichette dei sei motivi, l'avviso sulla nota obbligatoria e i messaggi di
  errore passano dallo spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`. I motivi sono **codici** nel
  database e testo tradotto nell'interfaccia: mai stringhe scritte a mano nei componenti.
- **RT-6 — Varchi e quota (§6, §7).** La rettifica **non consuma quota e non viene mai respinta con `429`**: il
  tetto `articoli_gestiti` (natura `stock`) colpisce solo la creazione di articoli nuovi. Impedire di correggere un
  saldo perché il piano è pieno restituirebbe al cliente un dato falso (descrizione, §5, punto delicato del
  listino). Valgono gli altri varchi: `401` senza token, `402` con abbonamento `canceled`, `403` per ruolo
  insufficiente.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `rettifica_giacenza(articolo, deposito, quantità_reale,
  motivo) → bozza di rettifica`, marcato **scrittura con effetto non annullabile se non con un altro movimento**:
  produce sempre una bozza che mostra saldo corrente, quantità dichiarata, delta risultante e motivo, e richiede
  **conferma umana esplicita con il motivo già scritto**. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e non ancora implementato (UC 0061-0063). Dettaglio nella storia `0035`.
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo: l'autore del movimento (`created_by`) e le note a
  testo libero sono già dichiarati nel manifesto dalla storia `0010`. L'interfaccia della nota porta l'avviso
  «campo a testo libero: non inserire dati sensibili». **Nessun indicatore di produttività per persona**: il numero
  di rettifiche fatte da un operatore non è una schermata di questa applicazione (descrizione, §6, e art. 4 della
  legge 300/1970 sullo Statuto dei lavoratori).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `rettifica registrata`, `rettifica senza differenza`,
  `rettifica respinta per motivo mancante` sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo di
  correlazione, articolo e deposito come identificativi — **senza** la nota a testo libero.

## 4. Criteri di accettazione

**CA-1 — Rettifica in aumento con motivo**
- **Dato** un articolo con giacenza 8 nel deposito centrale
- **Quando** un utente registra una rettifica con quantità reale 11 e motivo `merce_trovata`
- **Allora** nasce un movimento di tipo `rettifica` con quantità `+3` e motivo `merce_trovata`, la giacenza diventa
  11 con `versione` incrementata di uno e `ultimo_movimento_id` uguale al movimento appena creato

**CA-2 — Motivo `altro` senza nota**
- **Dato** un articolo con giacenza 8 · **Quando** si tenta una rettifica a 5 con motivo `altro` e nota vuota
- **Allora** la risposta è `422` in `application/problem+json` con l'indicazione che il motivo `altro` richiede una
  nota, nessun movimento viene creato e la giacenza resta 8

**CA-3 — Rettifica senza differenza**
- **Dato** un articolo con giacenza 8 · **Quando** si registra una rettifica con quantità reale 8
- **Allora** la risposta è `200` con esito «nessuna differenza», **nessun movimento** compare nello storico e la
  `versione` della giacenza non cambia

**CA-4 — Rettifica sotto zero rifiutata**
- **Dato** un articolo con giacenza 4 · **Quando** si tenta una rettifica con quantità reale `-1`
- **Allora** la risposta è `422`, il messaggio riporta la giacenza corrente e nulla viene scritto

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri articoli e depositi
- **Quando** un utente di `A` tenta una rettifica indicando l'identificativo di un articolo di `B`
- **Allora** riceve `404` (l'articolo non esiste nel suo perimetro) e la giacenza di `B` resta intatta, anche se
  forza l'identificativo dell'altro account nel corpo della richiesta

**CA-6 — La giacenza non è modificabile in interfaccia**
- **Dato** la sezione `giacenze` aperta su un articolo
- **Quando** si prova a modificare il valore del saldo mostrato
- **Allora** il campo è reso in sola lettura e l'unica azione disponibile è «rettifica», che chiede quantità reale e
  motivo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del delta e sulla validazione del motivo, di **integrazione** sulla rotta delle
      rettifiche con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla rotta delle rettifiche;
- [ ] prova che dimostra che **nessuna rotta** consente di scrivere direttamente la giacenza;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` dell'inventario è di proprietà della storia
      `0037`, che registra la rettifica dentro il flusso completo di conteggio; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** dei sei motivi e dei messaggi presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessun campo nuovo, verifica che l'avviso sul testo libero sia presente nel modulo;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la scelta di far dichiarare la
      quantità reale invece del delta e il perché;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `rettifica_giacenza`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | Registro dei movimenti, proiezione della giacenza con `versione` e `ultimo_movimento_id`, tabella dei motivi e chiave di idempotenza |
| `0015` | L'aggiornamento condizionato in una sola transazione e la gestione del conflitto `409` nascono lì e qui si riusano identici |
| `0017` (a valle) | Lo storno è l'unico modo di annullare una rettifica sbagliata: se non esistesse, un errore resterebbe senza rimedio |

## 7. Fuori ambito

- **Il conteggio fisico organizzato**: contare un deposito intero in sessione, in più mani, con l'atteso congelato è
  la storia `0022`; qui la rettifica è puntuale, su un articolo alla volta.
- **La generazione in blocco di rettifiche** alla chiusura di un inventario: storia `0023`, che riusa esattamente
  questa operazione riga per riga.
- **La riparazione della proiezione** quando diverge dal registro: è un'altra cosa e non è una rettifica —
  storia `0024`.
- **La valorizzazione della differenza**: quanto vale in denaro ciò che manca è la storia `0025`.
- **Lo storno di un movimento** in quanto tale: storia `0017`.

## 8. Punti aperti

- **L'elenco dei sei motivi è una proposta di prodotto**, non un dato rilevato: `calo_naturale` ha senso per chi
  tiene liquidi o sfusi e nessuno per chi tiene ricambi. Se debba essere modificabile dal cliente — con il rischio
  che ognuno si inventi una tassonomia inutilizzabile — lo decide lo sviluppatore; la proposta è: elenco chiuso
  ora, personalizzazione solo se la richiesta arriva davvero.
- **Chi può rettificare.** La proposta è che la rettifica sia consentita a `owner`, `admin` e `member`, perché chi
  conta è chi sta in magazzino. Se il cliente volesse riservarla ai soli responsabili servirebbe un ruolo in più:
  è una decisione di prodotto, non di questa storia.
