# 0020 — Consegna all'app proprietaria della relazione

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 04 — Interventi con conferma umana
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha già i recapiti dei clienti in SubGrove e in BillGrove
> voglio che il messaggio deciso qui parta da quelle applicazioni, con l'indirizzo che loro hanno aggiornato
> così da non ritrovarmi una seconda rubrica che invecchia e da non spiegare a un cliente perché gli ho scritto
> a un indirizzo che aveva cambiato due anni fa.

**Contesto.** La [descrizione](../application-description.md) §4.3 pone due vie e ne raccomanda una senza esitare.
**Via A**: RenewGrove **non conserva i recapiti** e **non invia**; l'intervento confermato esce come **evento di
richiesta di comunicazione** verso l'applicazione che possiede la relazione — SubGrove per un abbonato, BillGrove
per un cliente fatturato, DeskGrove per una segnalazione — che ha il recapito aggiornato, il modello di messaggio
e, cosa che conta di più, il rapporto già dichiarato nella propria informativa. L'esito torna indietro come
segnale. **Via B**: RenewGrove tiene una copia dei recapiti e invia da sé, con tutto quello che ne consegue — una
rubrica che invecchia, un possibile fornitore di messaggistica, e una seconda applicazione che scrive a persone
che non sono nostri utenti.

Si implementa la via A. Il §2.4 lo dice come risultato voluto e non come coincidenza: **nessuna integrazione
esterna nuova, nessun responsabile esterno del trattamento**, manifesto dei dati piccolo.

> ⚠️ **Costo dichiarato, da leggere prima di stimare la storia.** Il contratto dell'evento di richiesta di
> comunicazione **oggi non esiste nel repository**, e non è una mancanza di RenewGrove: riguarda tutte le
> applicazioni destinatarie. È una **decisione di piattaforma** — punto aperto n. 2 della descrizione, gemella di
> quella che InsightGrove ha sollevato sul contratto del fatto di misura — e va chiusa **prima** che questa storia
> si implementi. Che cosa si fa nel frattempo è scritto nei requisiti funzionali (RF-6) e nella sezione 8.

## 2. Requisiti funzionali

1. **RF-1** — Alla conferma di un intervento (`0019`), il servizio determina **quale applicazione possiede la
   relazione** con quel rapporto: è la fonte da cui il rapporto è nato o quella che ha pubblicato i segnali più
   recenti su di esso, secondo un ordine di preferenza dichiarato e leggibile a schermo.
2. **RF-2** — L'intervento confermato viene **pubblicato come evento di richiesta di comunicazione** sul canale a
   eventi della piattaforma, indirizzato a quell'applicazione, e passa in stato `consegnato`. L'evento porta:
   `tenant_id`, riferimento **opaco** al soggetto nell'app destinataria, canale richiesto, contenuto proposto,
   identificativo dell'intervento per la correlazione. **Non porta alcun recapito**, perché RenewGrove non ne ha.
3. **RF-3** — **RenewGrove non invia nulla e non conserva alcun recapito**: nessun indirizzo di posta elettronica,
   nessun numero di telefono, in nessuna tabella. La verifica è parte del contratto del segnale (`0006`) e qui si
   estende alla consegna.
4. **RF-4** — L'**esito** della comunicazione torna indietro come **segnale** dall'applicazione destinataria
   (consegnato, non consegnato, risposta ricevuta) e fa passare l'intervento in stato `eseguito`. Un esito che non
   arriva entro una finestra dichiarata lascia l'intervento `consegnato` e lo mostra come «in attesa di esito da
   *N* giorni»: il silenzio non si scambia per successo.
5. **RF-5** — Se **nessuna applicazione può inviare** — nessuna fonte collegata sa farlo, oppure la fonte
   d'origine non dichiara di saper ricevere richieste di comunicazione — l'intervento **ripiega sulla lista di
   lavoro** (storia `0021`) invece di fallire, e lo dice a schermo prima della conferma, non dopo. È il caso più
   frequente nelle micro-imprese e non è un errore.
6. **RF-6** — **Finché il contratto dell'evento non esiste** (punto aperto n. 2), il ripiego di RF-5 è la **sola**
   via percorribile e l'interfaccia lo dichiara apertamente: «nessuna delle tue applicazioni può ancora inviare
   per conto di RenewGrove; l'intervento finirà nella lista di lavoro». La consegna a evento resta implementata e
   collaudata con un destinatario simulato, così che l'arrivo del contratto sia una configurazione e non una
   riscrittura.
7. **RF-7** — Un intervento consegnato **non si annulla**: la macchina a stati del §4.4 non lo prevede, perché
   dopo la consegna l'effetto è fuori dal nostro controllo. Ciò che si può fare è registrarne l'esito.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lettura delle consegne filtra per `tenant_id` preso dal token di
  accesso verificato. Sull'evento in uscita il `tenant_id` è quello dell'intervento, a sua volta derivato dal
  token con cui la conferma è avvenuta: **non si deduce e non si accetta dal corpo di una richiesta**. Sull'esito
  in ingresso vale la regola già stabilita dalla storia `0007`: il `tenant_id` si **copia** dall'evento, e un
  esito con `tenant_id` mancante o sconosciuto viene **scartato**.
- **RT-2 — Interfaccia di programmazione (§2).** **Un'app non chiama un'altra app**: la consegna è
  esclusivamente **asincrona a eventi**, mai una chiamata di rete verso SubGrove, BillGrove o DeskGrove. Rotte di
  questa storia, tutte in lettura sul proprio schema: `GET /api/fidelizzazione/v1/interventi/{id}/consegna` (stato
  della consegna, destinatario, momento, esito) e `GET /api/fidelizzazione/v1/destinatari` (quali applicazioni
  dichiarano di saper ricevere richieste di comunicazione). Errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V16__consegna_intervento.sql` sullo schema `app_fidelizzazione`:
  tabella `consegna_intervento` con `tenant_id`, intervento, applicazione destinataria, riferimento opaco al
  soggetto, momento della pubblicazione, chiave di idempotenza, stato della consegna, esito e momento dell'esito;
  chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. **Nessuna colonna di recapito**, e
  un collaudo strutturale che ne verifica l'assenza. La pubblicazione dell'evento avviene con il modello della
  coda di uscita nella stessa transazione della transizione di stato: un intervento `consegnato` senza evento
  pubblicato sarebbe una bugia a schermo.
- **RT-4 — Modulo frontend (§3, §5).** Nella scheda dell'intervento: a chi è stato consegnato, quando, e lo stato
  dell'esito con l'indicazione «in attesa di esito da *N* giorni»; nella schermata di conferma (`0019`), la riga
  che dice **dove andrà** l'intervento — a quale applicazione o alla lista di lavoro — **prima** di confermare.
  Dati letti con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — nomi degli stati di consegna, «in attesa di esito», il testo
  che annuncia il ripiego sulla lista di lavoro — passano dallo spazio-nomi `fidelizzazione` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Catena dei varchi completa: `401`, `403` ad app spenta, `402` ad account non
  abilitato o abbonamento `canceled`, `403` a ruolo insufficiente (la conferma segue la regola della storia
  `0019`). **Nessun consumo di quota nuovo**: la metrica `rapporti_sorvegliati` (natura `stock`) non conta le
  consegne. L'abilitazione dell'applicazione **destinataria** si legge dalla proiezione locale alimentata a
  eventi, **mai** con una chiamata di rete sul percorso caldo.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `conferma_intervento(intervento) → consegna all'app proprietaria o alla lista di lavoro`, marcato **scrittura
  irreversibile** con **conferma umana obbligatoria**: è lo strumento che fa uscire qualcosa verso una persona che
  non è nostro utente. Dalla chat vale a maggior ragione la regola della descrizione §7 — l'intelligenza
  artificiale prepara, la persona approva — e il risultato dello strumento dichiara sempre **dove** l'intervento
  andrà. Il server conversazionale è di piattaforma e **non è ancora implementato** (UC 0061-0063); la storia
  `0029` assembla gli strumenti di scrittura.
- **RT-8 — Dati personali (§10).** **Nessun campo anagrafico nuovo, ed è il punto della storia**: la via A esiste
  proprio per non conservare recapiti. Voce nuova nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml`, in **italiano e inglese**, per la sola tabella di consegna:
  `consegna_intervento.destinazione_ed_esito` — dove vive: tabella `consegna_intervento`; di chi è: cliente del
  nostro cliente (per riferimento opaco); che dato è: comportamentale più prova; a cosa serve: sapere a chi è
  stato consegnato l'intervento e con che esito; base giuridica: esecuzione del rapporto commerciale fra il nostro
  cliente e il suo cliente; conservazione: 24 mesi come l'intervento. Campo del riferimento opaco annotato
  `@PersonalData`; tabella `consegna_intervento` aggiunta a `exportData` e a `purgeData` di
  `FidelizzazioneDataContract`. **Esclusione esplicita da scrivere nel manifesto**: nessun recapito del cliente
  finale è conservato in questa applicazione. Se un giorno si scegliesse la via B, questa riga, il §2.4 e la
  sezione 6 della descrizione andrebbero riscritti insieme.
- **RT-9 — Registrazione eventi (§14).** `intervento consegnato (applicazione destinataria)`,
  `consegna ripiegata sulla lista di lavoro (motivo)`, `esito ricevuto (tipo)`, `esito assente oltre la finestra`,
  con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e identificativo dell'intervento; **mai il
  contenuto** e mai il riferimento al soggetto in chiaro.

## 4. Criteri di accettazione

**CA-1 — Consegna all'app che possiede la relazione**
- **Dato** un rapporto nato dalla fonte SubGrove e un intervento appena confermato
- **Quando** avviene la consegna
- **Allora** viene pubblicato un evento di richiesta di comunicazione indirizzato a SubGrove, con `tenant_id`,
  riferimento opaco, canale e contenuto, **senza alcun recapito**; l'intervento passa in stato `consegnato` e la
  scheda mostra il destinatario

**CA-2 — Nessun recapito conservato**
- **Dato** lo schema `app_fidelizzazione` dopo tutte le migrazioni
- **Quando** gira il collaudo strutturale sulle colonne
- **Allora** nessuna tabella contiene un campo di recapito (indirizzo di posta elettronica, numero di telefono), e
  il collaudo fallisce se ne venisse aggiunto uno

**CA-3 — Ripiego dichiarato prima della conferma**
- **Dato** un account in cui nessuna applicazione collegata dichiara di saper ricevere richieste di comunicazione
- **Quando** l'utente apre la schermata di conferma di un intervento
- **Allora** legge, **prima** di confermare, che l'intervento finirà nella lista di lavoro e non partirà alcun
  messaggio; dopo la conferma l'intervento risulta `consegnato` alla lista di lavoro

**CA-4 — L'esito torna e chiude l'intervento**
- **Dato** un intervento `consegnato` a SubGrove
- **Quando** arriva l'evento di esito «consegnato al destinatario» con la chiave di correlazione
- **Allora** l'intervento passa in stato `eseguito` con l'esito e il suo momento; un secondo evento identico non
  produce una seconda transizione

**CA-5 — Il silenzio non è successo**
- **Dato** un intervento `consegnato` da più giorni della finestra dichiarata, senza esito
- **Quando** si apre la sua scheda
- **Allora** risulta «in attesa di esito da *N* giorni» e **non** compare fra quelli eseguiti; l'evento
  corrispondente è registrato

**CA-6 — Isolamento fra account sull'esito in ingresso**
- **Dato** due account `A` e `B`
- **Quando** arriva un evento di esito con un `tenant_id` sconosciuto alla piattaforma o assente
- **Allora** l'evento viene **scartato** con registrazione del motivo, e nessun intervento di `A` o di `B` cambia
  stato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla scelta dell'applicazione destinataria e sull'idempotenza dell'esito; prove di
      **integrazione** sulla pubblicazione con coda di uscita e sulla ricezione dell'esito, con database effimero
      e migrazioni Flyway vere;
- [ ] **collaudo strutturale** che verifica l'assenza di qualunque colonna di recapito e l'assenza di chiamate di
      rete verso altre applicazioni (la comunicazione è solo a eventi);
- [ ] prova di **isolamento fra account** sulla risorsa della consegna e sullo scarto di un esito con `tenant_id`
      sconosciuto;
- [ ] **prova end-to-end**: *rimandare* — il percorso `[J-FIDELIZZAZIONE]` nasce nella storia `0030` e dovrà
      coprire il tratto «confermo → l'evento viene pubblicato → l'esito torna → l'intervento è eseguito», con
      destinatario simulato finché il contratto di piattaforma non esiste; voce `da-coprire` con motivo e storia
      proprietaria `0030` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `consegna_intervento.destinazione_ed_esito`,
      campo annotato `@PersonalData`, tabella in `exportData` e in `purgeData`, ed **esclusione esplicita dei
      recapiti** scritta nel manifesto;
- [ ] **registro delle decisioni** compilato con: perché la via A e non la via B, come si sceglie l'applicazione
      destinataria, che cosa si fa finché il contratto dell'evento non esiste, perché un intervento consegnato non
      si annulla;
- [ ] contratto dello strumento `conferma_intervento` dichiarato come **scrittura irreversibile con conferma
      obbligatoria**;
- [ ] documentazione aggiornata: la descrizione §4.3 riflette la forma dell'evento effettivamente pubblicato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` — intervento con conferma umana | la consegna parte dallo stato `confermato`: senza la macchina a stati non c'è niente da consegnare |
| storia `0008` — collegamento e revoca di una fonte | l'applicazione che possiede la relazione è una fonte collegata; senza collegamento non c'è destinatario |
| storia `0021` — lista di lavoro | è il ripiego di RF-5 e, finché il contratto di piattaforma non esiste, l'unica via percorribile: le due storie si consegnano insieme |
| **contratto dell'evento di richiesta di comunicazione** — decisione di piattaforma, **non esistente** | è il punto aperto n. 2. Nel frattempo: consegna implementata e collaudata con destinatario simulato, ripiego sulla lista di lavoro dichiarato a schermo |
| **19 SubGrove**, **02 BillGrove**, **12 DeskGrove** (app del catalogo, non implementate) | destinatarie della richiesta di comunicazione; finché non esistono, l'evento resta senza consumatori e il ripiego copre il caso |
| epica di piattaforma non implementata, UC 0061-0063 | `conferma_intervento` è dichiarato e non esposto |

## 7. Fuori ambito

- **la lista di lavoro** vera e propria — chi chiamare, che cosa dire, com'è andata: storia `0021`;
- **i freni al contatto** che possono bloccare una consegna: storia `0023`;
- **l'invio diretto da RenewGrove** (via B del §4.3): **escluso per scelta, non rimandato**. Rientrerebbe solo con
  una decisione esplicita che riscrive il §2.4 e la sezione 6 della descrizione, introduce un possibile fornitore
  esterno e amplia il manifesto dei dati;
- **la composizione del messaggio nel formato dell'app destinataria**: è di chi lo invia. RenewGrove propone un
  contenuto, non impagina una comunicazione;
- **la scelta del canale fra più disponibili** in modo automatico: il canale è dell'intervento (`0019`), scelto da
  una persona.

## 8. Punti aperti

- **Il contratto dell'evento di richiesta di comunicazione non esiste** (§4.3, punto aperto n. 2 della
  descrizione). Riguarda tutte le applicazioni destinatarie e non solo questa: che campi porta, come si esprime il
  riferimento opaco al soggetto, come torna l'esito, che cosa succede se il destinatario non sa gestire il canale
  richiesto. **Che cosa si fa nel frattempo, e va deciso adesso**: si implementa la pubblicazione con un
  destinatario simulato nei collaudi, si dichiara a schermo che nessuna applicazione può ancora inviare, e si
  ripiega sulla lista di lavoro (`0021`) — che copre da sola il caso d'uso prevalente del segmento, dove
  l'intervento giusto è una telefonata. Chiude: **piattaforma (sviluppatore)**, prima dell'implementazione di
  questa storia.
- **Quale applicazione «possiede» la relazione quando più fonti la conoscono.** Un cliente può essere insieme
  abbonato in SubGrove, fatturato in BillGrove e presente in DeskGrove. **Raccomandazione**: ordine di preferenza
  dichiarato e leggibile a schermo — l'applicazione da cui il rapporto è nato, poi quella che ha pubblicato i
  segnali più recenti — con la possibilità per chi conferma di scegliere un'altra destinazione fra quelle
  disponibili. Chiude: **sviluppatore**.
