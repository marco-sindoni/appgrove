# 0021 — Rimbalzi e segnalazioni

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 04 — Spedizione e canali
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che non sa di stare rovinando il proprio recapito
> voglio che gli indirizzi che rimbalzano e le segnalazioni di posta indesiderata siano raccolte e mi fermino
> così da non perdere la possibilità di scrivere ai clienti che mi leggono davvero.

**Contesto.** I grandi fornitori di posta chiedono a chi manda in volume di stare **sotto lo 0,3 % di
segnalazioni** di posta indesiderata, con lo 0,1 % come obiettivo; chi supera non finisce in una cartella, viene
**respinto** al livello del protocollo ([application-description.md](../application-description.md) §2.3 punto 5).
Su una campagna da 2.000 destinatari lo 0,3 % sono **sei persone**: una soglia che si supera senza accorgersene.

C'è un secondo motivo, più serio, per cui questa storia sta qui e non fra i rapporti: la reputazione dell'indirizzo
di invio è **condivisa fra tutti gli account** (§11, rischi noti). Un solo cliente che spedisce a una lista comprata
fa respingere i messaggi di tutti gli altri. Il blocco automatico di questa storia non protegge il cliente da sé
stesso: protegge gli altri clienti da lui, ed è per questo che non è configurabile né disattivabile.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio riceve dal fornitore di consegna i ritorni sulle consegne: **rimbalzo permanente**
   (l'indirizzo non esiste), **rimbalzo temporaneo** (casella piena, server non disponibile), **segnalazione di
   posta indesiderata**, consegna riuscita.
2. **RF-2** — I ritorni sono accettati solo se **autenticati** come provenienti dal fornitore e sono **idempotenti**:
   lo stesso ritorno consegnato tre volte produce un solo effetto.
3. **RF-3** — Un rimbalzo **permanente** e una **segnalazione** portano il recapito nell'elenco di soppressione
   **subito**, con il motivo; da quel momento nessun invio parte più verso quel recapito, in nessuna campagna e in
   nessun percorso automatico.
4. **RF-4** — Un rimbalzo **temporaneo** non sopprime: si conta. Al superamento di una soglia di temporanei
   consecutivi sullo stesso recapito, l'iscritto passa a uno stato di attenzione ed esce dai segmenti finché non
   torna raggiungibile.
5. **RF-5** — Il **tasso di segnalazione** è calcolato per account su una finestra mobile sui messaggi
   effettivamente consegnati, ed è visibile al cliente insieme alla soglia.
6. **RF-6** — Superata la soglia dello **0,3 %**, l'invio dell'account viene **bloccato automaticamente**: le
   campagne in corso si mettono in pausa, quelle programmate passano a `bloccata`, e il cliente riceve un avviso
   che spiega cosa è successo, perché e cosa deve fare. Lo sblocco **non** è automatico e non è del cliente: passa
   dalla console di amministrazione ([estensioni-admin.md](../estensioni-admin.md)).
7. **RF-7** — Il tasso di segnalazione e di rimbalzo per account è esposto alla console di amministrazione, perché
   è lì che si vede il rischio che riguarda tutti.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni ritorno viene attribuito alla consegna e quindi all'account **della
  campagna che lo ha generato**; il `tenant_id` non arriva mai dal carico del fornitore, si risolve dalla consegna.
  Un ritorno che non si risolve a una consegna nota viene scartato e registrato, non applicato «al meglio».
- **RT-2 — Interfaccia di programmazione (§2).** Rotta di ricezione `POST /api/campaigns/v1/deliveries/events`,
  **non autenticata dal token utente** ma verificata con la firma del fornitore; risponde sempre rapidamente e
  lavora in modo asincrono. Rotte di lettura `GET /api/campaigns/v1/list-health/complaint-rate`. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `delivery_event` (storia `0002`) con **vincolo di unicità** sulla coppia
  `(tenant_id, provider_event_id)`, che è ciò che rende l'idempotenza una proprietà del database e non una
  speranza. Tabella `suppression` alimentata da qui. Schema `app_campaigns`, chiave primaria UUID versione 7,
  colonne di controllo, cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Nella schermata «Salute della lista» (storia `0032`, che qui riceve i suoi
  dati) e nel rapporto di campagna (storia `0030`): tasso di segnalazione contro la soglia, rimbalzi per tipo.
  L'avviso di blocco compare in testa a tutto il modulo, non in una sezione secondaria. Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tipi di ritorno, motivi di soppressione e — soprattutto — il **testo dell'avviso
  di blocco** presenti in `en, it, fr, es, de`. Quel testo è il più importante dell'app: deve dire cosa è successo,
  perché è un problema e qual è la via d'uscita.
- **RT-6 — Varchi e quota (§6, §7).** Il blocco per tasso di segnalazione è **indipendente** dalla quota e dai
  varchi ordinari: agisce anche su un account in regola con l'abbonamento. Un tentativo di spedizione da un account
  bloccato risponde `403` con motivo esplicito, non `429` — non è una questione di limiti del piano.
- **RT-7 — Esposizione conversazionale (§12).** `salute_della_lista` (storia `0034`, lettura) restituisce tasso di
  segnalazione, rimbalzi, inattivi e iscritti in quarantena, **con** la distanza dalla soglia. È il secondo
  strumento che giustifica il livello conversazionale in questa app (descrizione §7): è un numero che il cliente
  non guarderà mai spontaneamente e che gli costa l'account quando lo supera. Nessuno strumento di **scrittura**:
  lo sblocco non è del cliente e non è dell'assistente.
- **RT-8 — Dati personali (§10).** I ritorni riguardano persone: voce `delivery_event.*` nel manifesto
  `docs/compliance/manifests/campaigns.yaml` in italiano e inglese (categoria «comportamento», finalità «gestire la
  recapitabilità»). La `suppression` conserva l'**impronta crittografica non reversibile** del recapito più il
  recapito cifrato, come proposto al §6 della descrizione, ed è la voce a conservazione **permanente**:
  cancellarla riaprirebbe la porta agli invii. Entrambe le tabelle in `exportData`; per `purgeData` vale
  l'avvertenza del §6, che è un punto aperto dichiarato.
- **RT-9 — Registrazione eventi (§14).** «Ritorno ricevuto» con tipo e identificativo della consegna, «recapito
  soppresso» con motivo, «account bloccato per tasso di segnalazione» con il valore e la soglia. Con `tenant_id`,
  `app_id`, `user_id` (lavorazione automatica) e identificativo di correlazione; **mai** il recapito.

## 4. Criteri di accettazione

**CA-1 — Rimbalzo permanente e soppressione immediata**
- **Dato** una consegna verso un indirizzo inesistente
- **Quando** arriva il ritorno di rimbalzo permanente
- **Allora** il recapito entra nell'elenco di soppressione con motivo «rimbalzo permanente», e una campagna
  successiva verso lo stesso iscritto lo salta

**CA-2 — Idempotenza dei ritorni**
- **Dato** lo stesso ritorno del fornitore consegnato tre volte
- **Quando** viene elaborato
- **Allora** esiste **una** riga di evento, la soppressione è una sola e i conteggi non sono gonfiati

**CA-3 — Ritorno non autenticato**
- **Dato** una richiesta alla rotta di ricezione con firma assente o non valida
- **Quando** arriva al servizio
- **Allora** viene respinta senza produrre alcun effetto, e il tentativo è registrato

**CA-4 — Superamento della soglia e blocco**
- **Dato** un account con 2.000 messaggi consegnati nella finestra e la settima segnalazione (0,35 %)
- **Quando** il ritorno viene elaborato
- **Allora** l'invio dell'account viene bloccato, la campagna in corso si mette in pausa, quelle programmate
  passano a `bloccata`, il cliente riceve l'avviso e un nuovo tentativo di invio risponde `403`

**CA-5 — Lo sblocco non è del cliente**
- **Dato** un account bloccato per tasso di segnalazione
- **Quando** l'utente cerca in ogni modo di riprendere la spedizione dall'interfaccia o dalle rotte
- **Allora** non esiste alcuna via: lo sblocco richiede un intervento dalla console di amministrazione, tracciato
  con operatore e motivo

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, con `A` sopra la soglia
- **Quando** `B` spedisce
- **Allora** `B` non è toccato: il tasso è per account, e la soppressione di `A` non compare fra i dati di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del tasso di segnalazione sulla finestra mobile e sulla classificazione dei
      ritorni; prove di **integrazione** sulla rotta di ricezione con carichi sintetici **firmati**, compreso il
      caso della firma non valida e della consegna ripetuta;
- [ ] prova di **isolamento fra account** su eventi, soppressione e tasso;
- [ ] **prova end-to-end**: coprire ora — `[J-CAMPAIGNS]` (storia `0037`) include un rimbalzo permanente simulato e
      verifica che l'iscritto risulti soppresso; il **blocco per soglia** è invece coperto da una prova di
      integrazione, perché generare seimila consegne in un percorso end-to-end non è ragionevole. Registro di
      copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato con entrambe
      le voci;
- [ ] **traduzioni** in tutte e cinque le lingue, con cura particolare per il testo dell'avviso di blocco;
- [ ] **manifesto dei dati** aggiornato per `delivery_event` e `suppression` in italiano e inglese, con la nota
      sulla conservazione permanente della soppressione e sul suo trattamento in cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché il blocco non è configurabile dal cliente e
      perché lo sblocco è amministrativo;
- [ ] contratto degli **strumenti conversazionali**: `salute_della_lista` in lettura, nessuno strumento di
      scrittura, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0011` | L'elenco di soppressione è la struttura che questa storia alimenta |
| Storia `0019` | Senza consegne non ci sono ritorni da ricevere |
| Scelta del fornitore di consegna ([application-description.md](../application-description.md) §11.2) | Forma dei ritorni, modo di firmarli e classificazione dei rimbalzi dipendono da lui |
| [estensioni-admin.md](../estensioni-admin.md) | Lo sblocco e la sospensione preventiva vivono nella console di amministrazione |

## 7. Fuori ambito

- il **cruscotto** della salute della lista con inattivi e iscritti in quarantena: è la storia `0032`, che consuma
  i numeri prodotti qui;
- il rapporto della singola campagna: è la storia `0030`;
- aperture e clic: sono un'altra categoria di eventi, facoltativa e spenta in partenza (storia `0029`);
- la sospensione **preventiva** decisa dalla piattaforma su un account sospetto: sta nella console di
  amministrazione;
- il riscaldamento graduale di un indirizzo di invio nuovo: è una pratica di piattaforma, non dell'app.

## 8. Punti aperti

- **Finestra di calcolo del tasso di segnalazione.** Trenta giorni? Le ultime cinque campagne? Un account che
  manda una volta al trimestre ha una finestra quasi sempre vuota, e un tasso calcolato su venti consegne non
  significa niente. Proposta: finestra mobile di 30 giorni **con un minimo di consegne** sotto il quale il tasso
  non blocca (proposta: 500). Chiude lo sviluppatore.
- **Soglia di rimbalzi temporanei consecutivi** oltre la quale l'iscritto esce dai segmenti: proposta 5. Non ho
  trovato un riferimento pubblico; è un numero da rivedere sui dati.
- **Se il blocco debba essere per account o per dominio mittente.** Un account con due domini che ne rovina uno
  solo: bloccare tutto è prudente e forse eccessivo. La proposta di questa storia è **per account**, perché la
  reputazione condivisa è dell'infrastruttura di invio, non del dominio del cliente. Da confermare con la scelta
  del fornitore.
