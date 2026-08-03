# 0013 — Segmenti salvati

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 03 — Contenuti e segmenti
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che manda comunicazioni commerciali
> voglio salvare con un nome i criteri con cui scelgo a chi scrivere
> così da non ricostruire l'elenco ogni volta e da sapere **prima** quante persone lo riceveranno davvero.

**Contesto.** Con l'epica 02 l'archivio degli iscritti esiste e ognuno porta con sé la prova del proprio consenso.
Manca il modo di dire «questi sì, questi no» senza rifare a mano la selezione a ogni campagna. Il punto delicato
non è filtrare: è che un elenco filtrato **assomiglia a un permesso**, e non lo è. Chi guarda un segmento da 1.240
persone dà per scontato che partiranno 1.240 messaggi; se 300 di quelle persone non hanno confermato l'iscrizione o
sono finite nell'elenco di soppressione, il numero è una bugia che si scopre a invio fatto. Per questo il segmento
nasce fin da subito con **due conteggi separati** e con il ricalcolo al momento dell'invio scritto nel suo
comportamento, non in una nota a piè di pagina.

## 2. Requisiti funzionali

1. **RF-1** — Un segmento è un nome più un elenco di criteri combinati con «tutte queste condizioni» oppure «almeno
   una di queste condizioni». Criteri ammessi: stato dell'iscritto, lingua, data d'iscrizione, origine
   dell'iscrizione, valore di un campo personalizzato, appartenenza a una lista e — **solo se la misurazione del
   comportamento è attiva** (storia 0029) — «ha aperto» o «ha fatto clic» in un periodo.
2. **RF-2** — I criteri sono scritti da una persona, leggibili nella stessa forma in cui sono stati inseriti e
   riproducibili: nessun punteggio attribuito alle persone, nessuna classificazione automatica, nessuna selezione
   che l'utente non possa rileggere e spiegare.
3. **RF-3** — Il segmento mostra **due conteggi distinti** riferiti all'ultimo calcolo: gli iscritti che
   **corrispondono ai criteri** e quelli che **riceverebbero davvero** sul canale scelto. La differenza è spiegata
   voce per voce: in attesa di conferma, in quarantena, disiscritti, recapito soppresso, canale non attivo.
4. **RF-4** — I conteggi sono una fotografia con il proprio momento di calcolo, non un elenco congelato: il segmento
   **non autorizza nessun invio** e l'elenco effettivo dei destinatari si ricalcola alla spedizione (storie 0019 e
   0020). L'interfaccia lo dice a chiare lettere accanto ai numeri.
5. **RF-5** — Un'anteprima mostra fino a 25 iscritti corrispondenti, ciascuno con l'indicazione se è inviabile e, se
   non lo è, il motivo.
6. **RF-6** — Alla creazione o alla modifica, se il nome del segmento o il valore di un criterio contiene termini
   che possono rivelare **categorie particolari di dati personali** (articolo 9: salute, convinzioni religiose,
   opinioni politiche, appartenenza sindacale, orientamento sessuale, dati biometrici o genetici), l'interfaccia
   mostra un avviso che spiega il rischio e chiede una conferma esplicita. L'avviso non blocca: informa chi è
   titolare del trattamento, cioè il cliente.
7. **RF-7** — Un segmento si duplica e si cancella (cancellazione logica). Se è usato da una campagna programmata o
   da un percorso automatico attivo, la cancellazione avverte quali e richiede conferma.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dei segmenti e ogni calcolo del conteggio
  filtrano per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai
  parametri viene ignorato. Il calcolo del segmento è il punto in cui una perdita d'isolamento avrebbe l'effetto
  peggiore possibile — scrivere agli iscritti di un altro account — quindi la prova d'isolamento copre anche
  l'anteprima e i conteggi, non solo l'elenco dei segmenti.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/campaigns/v1/segments`,
  `GET|PUT|DELETE /api/campaigns/v1/segments/{id}`, `POST /api/campaigns/v1/segments/{id}/preview` (conteggi e
  campione). Corpo validato in modo dichiarativo — l'elenco dei criteri ammessi è chiuso, un criterio sconosciuto è
  un errore di validazione, non un filtro ignorato. Errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** La tabella `segment` nasce con la storia 0002 sullo schema `app_campaigns`, con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. Qui si aggiunge la
  migrazione `V<N>__segment_counts.sql`: colonne `matched_count`, `sendable_count`, `counted_at` e l'indice che
  serve al calcolo, sempre a partire da `tenant_id`. I criteri sono un documento strutturato e validato, non testo
  libero interpretato a runtime.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Segmenti» del modulo `campaigns`: elenco, costruttore dei criteri,
  riquadro dei due conteggi con la differenza spiegata, anteprima del campione. Dati letti con il client generato
  dalla definizione delle interfacce; solo token del sistema di design (colore-categoria `violet`); funziona in
  tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi dei criteri, motivi di non inviabilità, testo
  dell'avviso sulle categorie particolari — passano dallo spazio-nomi `campaigns` e sono presenti in
  `en, it, fr, es, de`. I **nomi dei segmenti scritti dal cliente** non si traducono: sono suoi contenuti.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo della metrica `messages_sent` (natura `flow`): un segmento non
  manda niente. Valgono i varchi precedenti: token valido, app accesa, account abilitato (altrimenti `402`), ruolo
  sufficiente. Con abbonamento in `past_due` i segmenti restano accessibili; con `canceled` la risorsa risponde
  `402`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `elenca_segmenti() → segmenti con criteri
  leggibili e i due conteggi`, marcato **lettura**. La **creazione** di un segmento non è esposta alla chat in
  questa storia, e la scelta è deliberata: un criterio dettato a voce è difficile da rileggere, e il segmento
  determina *a chi si scrive*. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e
  non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Il segmento è configurazione, non un archivio di persone: contiene criteri, non
  iscritti. Però un criterio **può nominare una persona** (per esempio «indirizzo uguale a mario.rossi@esempio.test»)
  e il nome del segmento può rivelare una categoria particolare. Si aggiunge quindi la voce `segment.criteria` al
  manifesto `docs/compliance/manifests/campaigns.yaml` in italiano e inglese, con il campo annotato `@PersonalData`
  e la tabella `segment` presente in `exportData` e `purgeData` — con la precisazione, già anticipata al §6 della
  [descrizione dell'applicazione](../application-description.md), che vi rientra **quando il criterio nomina una
  persona**. Il campione dell'anteprima non si conserva.
- **RT-9 — Registrazione eventi (§14).** «Segmento creato», «segmento modificato», «segmento calcolato» (con i due
  conteggi) e «avviso su categorie particolari confermato dall'utente» sono registrati con `tenant_id`, `app_id`
  (`campaigns`), `user_id` e identificativo di correlazione. **Mai** il nome del segmento, mai i valori dei criteri,
  mai gli indirizzi del campione: sono contenuti del cliente e in un registro non ci vanno.

## 4. Criteri di accettazione

**CA-1 — Due conteggi, non uno**
- **Dato** un account con 1.240 iscritti che corrispondono al criterio «lingua italiana», di cui 180 in attesa di
  conferma, 90 in quarantena e 30 con recapito soppresso
- **Quando** l'utente salva il segmento e ne chiede il calcolo
- **Allora** vede «1.240 corrispondono» e «940 riceverebbero», con la differenza scomposta nei tre motivi e i
  rispettivi numeri

**CA-2 — Il segmento non è un permesso**
- **Dato** un segmento calcolato ieri con 940 destinatari inviabili
- **Quando** oggi 12 di quelle persone si disiscrivono e domani parte la campagna che usa il segmento
- **Allora** la spedizione ricalcola la contattabilità al momento dell'invio e quelle 12 persone **non** ricevono,
  senza che nessuno debba ricalcolare il segmento a mano

**CA-3 — Criterio sconosciuto respinto**
- **Dato** una richiesta che contiene un criterio non compreso nell'elenco ammesso (per esempio un punteggio di
  propensione)
- **Quando** arriva al servizio
- **Allora** riceve `400` in `application/problem+json` con l'indicazione del criterio non ammesso, e il segmento
  **non** viene salvato con quel criterio ignorato in silenzio

**CA-4 — Avviso sulle categorie particolari**
- **Dato** un utente che nomina un segmento «Iscritti al gruppo diabete»
- **Quando** prova a salvarlo
- **Allora** compare un avviso che spiega che quel segmento può rivelare dati sulla salute e che la valutazione
  spetta a lui come titolare; il salvataggio avviene solo dopo una conferma esplicita, e la conferma resta
  registrata

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri segmenti e i propri iscritti
- **Quando** un utente di `A` chiede l'anteprima di un segmento di `B`, anche forzando l'identificativo dell'altro
  account nella richiesta
- **Allora** riceve `404` e nessun conteggio né campione dell'altro account

**CA-6 — Cancellazione avvertita**
- **Dato** un segmento usato da una campagna programmata
- **Quando** l'utente prova a cancellarlo
- **Allora** l'interfaccia mostra quali campagne e percorsi lo usano e chiede conferma; confermando, il segmento è
  cancellato logicamente e la campagna programmata passa in uno stato che ne segnala la mancanza

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla traduzione dei criteri in interrogazione e sul calcolo dei due conteggi, e di
      **integrazione** sulla risorsa dei segmenti, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su elenco, calcolo e anteprima;
- [ ] **prova end-to-end**: rimando — il percorso `[J-CAMPAIGNS]` include la scelta del segmento come passo della
      campagna, ma il passo nasce con la storia 0037, che è la proprietaria del percorso; qui si registra la voce
      `da-coprire` nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con
      motivo e storia proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con i nomi dei segmenti scritti dal cliente esclusi dalla
      traduzione;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `segment.criteria`, campo annotato, tabella
      `segment` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotato perché i conteggi sono
      due e perché l'elenco dei criteri è chiuso;
- [ ] contratto degli **strumenti conversazionali**: `elenca_segmenti` in lettura; creazione **non** esposta, con la
      motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sul costruttore dei criteri;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | I criteri selezionano iscritti: senza l'anagrafica non c'è niente da selezionare |
| Storia `0007` | Il conteggio degli inviabili si legge dallo stato del consenso, che è il registro |
| Storia `0011` | La soppressione vince su tutto e va sottratta dal conteggio dei destinatari reali |
| Storia `0029` (successiva) | I criteri di comportamento («ha aperto», «ha fatto clic») esistono **solo** se la misurazione è attiva: fino ad allora sono nascosti, non finti |

## 7. Fuori ambito

- l'invio: il segmento sceglie, non spedisce. Spedisce la storia 0019;
- il ricalcolo della contattabilità al momento dell'invio: è la storia 0020, che è la sede giusta perché è lì che
  l'ultima parola conta;
- i criteri di comportamento: dipendono dalla storia 0029 e restano nascosti finché la misurazione non è attiva;
- il rilevamento automatico di categorie particolari nei contenuti: l'app **non** analizza i contenuti del cliente
  ([application-description.md](../application-description.md) §6). Qui c'è solo un avviso su termini evidenti nel
  nome e nei criteri, che è un aiuto, non un presidio.

## 8. Punti aperti

- **Elenco dei termini che fanno scattare l'avviso sull'articolo 9.** Una lista di parole è per forza incompleta e
  dipende dalla lingua. La proposta è partire da un elenco breve e dichiarato, uguale nelle cinque lingue, e dire
  nell'avviso stesso che non è una verifica ma un promemoria. Chiude lo sviluppatore con la revisione legale
  ([application-description.md](../application-description.md) §11.6).
- **Se il campione dell'anteprima debba mostrare i recapiti in chiaro.** Mostrarli aiuta a riconoscere gli iscritti;
  è però una lettura di dati personali fatta per comodità. Proposta: mostrarli in forma parziale. Chiude lo
  sviluppatore in sede di compilazione del manifesto.
