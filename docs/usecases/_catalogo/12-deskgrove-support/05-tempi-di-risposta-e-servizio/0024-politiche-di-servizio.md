# 0024 — Politiche di servizio

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 05 — Tempi di risposta e livello di servizio
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0021`, `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile del servizio clienti
> voglio dichiarare entro quante ore lavorative promettiamo la prima risposta e la risoluzione, per ciascuna
> priorità
> così da avere un obiettivo scritto contro cui misurarmi, invece di un'impressione.

**Contesto.** Il calendario dell'azienda esiste (storia `0023`), le quattro priorità esistono (storia `0021`): manca
la promessa. Il [documento capofila](../application-description.md) §2.5 dà l'ancoraggio di mercato e anche il suo
limite: le rilevazioni ricorrenti — fonti di parte, usate per la **forma** e non come dato da citare al cliente —
indicano che circa il 46% dei clienti si aspetta una risposta alla posta elettronica entro 4 ore, mentre il tempo
medio effettivo nel mercato sta fra le 7 e le 12 ore. Il numero non è affidabile; la forma sì: **l'aspettativa si
misura in ore, non in minuti**. Da qui la scelta di esprimere gli obiettivi in ore lavorative intere. E la scelta,
altrettanto importante, di fermarsi: una politica per account, al più una per coda, **nessun motore di regole** —
perché è esattamente la funzione che travolge una squadra di tre persone (§2.5).

## 2. Requisiti funzionali

1. **RF-1** — Una politica di servizio è un insieme di quattro righe, una per ciascuna priorità (bassa, normale,
   alta, urgente), e ogni riga porta due obiettivi espressi in ore lavorative intere: prima risposta e risoluzione.
2. **RF-2** — L'account ha sempre una politica predefinita, creata alla prima apertura del modulo con valori
   iniziali coerenti con l'ancoraggio di mercato (proposta: prima risposta 16 / 8 / 4 / 2 ore lavorative e
   risoluzione 40 / 24 / 8 / 4 ore lavorative, dalla priorità bassa alla urgente), che non si può cancellare.
3. **RF-3** — Una coda (storia `0019`) può indicare una politica diversa da quella predefinita dell'account; se non
   ne indica nessuna si applica la predefinita.
4. **RF-4** — Il numero di politiche per account è limitato a cinque: oltre quel numero la creazione viene
   rifiutata con la spiegazione che l'app non è un motore di regole.
5. **RF-5** — Ogni politica riferisce l'orario di servizio dell'account (storia `0023`), che è quindi il righello
   unico su cui le sue ore lavorative si contano.
6. **RF-6** — Un obiettivo può essere lasciato assente per una priorità: significa «per questa priorità non
   promettiamo niente» e non produrrà alcuna scadenza, senza che la politica vada cancellata.
7. **RF-7** — La modifica di una politica vale per le richieste aperte da quel momento in poi: le scadenze già
   calcolate sulle richieste esistenti non vengono riscritte.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle entità `service_policy` e
  `service_policy_target` filtra per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo
  della richiesta o dai parametri viene ignorato. L'associazione fra coda e politica è verificata dentro lo stesso
  account: una coda non può puntare a una politica di un altro account, nemmeno forzandone l'identificativo.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/helpdesk/v1/politiche-di-servizio`,
  `GET|PUT|DELETE /api/helpdesk/v1/politiche-di-servizio/{id}` e l'assegnazione alla coda con
  `PUT /api/helpdesk/v1/code/{id}/politica-di-servizio`; corpo validato (ore intere maggiori di zero, risoluzione
  non inferiore alla prima risposta della stessa priorità); errori in `application/problem+json`, compreso il
  rifiuto del tetto di cinque politiche; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__service_policy.sql` sullo schema `app_helpdesk`: tabelle
  `service_policy` (nome, contrassegno di politica predefinita) e `service_policy_target` (priorità, ore della
  prima risposta, ore della risoluzione), più la colonna `service_policy_id` sulla tabella delle code — riferimento
  **logico**, senza chiave esterna verso altri schemi. Entrambe con `tenant_id`, chiave primaria UUID versione 7,
  colonne di controllo e cancellazione logica `deleted_at`. Vincolo di unicità su (`tenant_id`, politica,
  priorità).
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Impostazioni → Livello di servizio* del modulo `helpdesk`: la
  politica come tabella a quattro righe e due colonne, l'elenco delle politiche esistenti e il selettore nella
  schermata della coda. Dati letti con il client generato; solo token del sistema di design; funziona in tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi delle priorità, intestazioni «prima risposta» e
  «risoluzione», unità «ore lavorative», messaggi di rifiuto — passano dallo spazio-nomi `helpdesk` e sono presenti
  in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Le politiche non consumano la metrica `agents` (natura `stock`). La storia
  **non fissa prezzi** e non sposta funzioni fra i piani: il livello di servizio sta in tutti i piani, coerentemente
  con la proposta di listino del [documento capofila](../application-description.md) §5. Restano i varchi a monte:
  `401` senza token, `402` con abbonamento `canceled`, `403` per ruolo insufficiente — creare e modificare le
  politiche è riservato ai ruoli `owner` e `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: la promessa fatta ai clienti finali non si
  cambia da una chat. Gli obiettivi restano leggibili attraverso `stato_del_servizio` (storia `0028`). Esclusione
  deliberata, annotata nel registro delle decisioni; dipendenza di piattaforma dichiarata: UC 0061-0063, non
  ancora implementati.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: una politica descrive una promessa dell'azienda,
  non una persona. Le due tabelle entrano comunque in `exportData` del contratto `HelpdeskDataContract` come parte
  della configurazione da restituire al titolare del trattamento alla fine del rapporto.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «politica di servizio creata», «politica di servizio
  modificata», «politica assegnata a una coda», «creazione respinta per tetto delle politiche» sono registrati con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — La politica predefinita esiste da subito**
- **Dato** un account che apre il modulo `helpdesk` per la prima volta
- **Quando** consulta la sezione *Livello di servizio*
- **Allora** trova una politica predefinita con i quattro obiettivi di prima risposta e i quattro di risoluzione
  già compilati, e non può cancellarla

**CA-2 — Obiettivo incoerente rifiutato**
- **Dato** un utente con ruolo `admin` · **Quando** salva per la priorità «alta» una risoluzione di 2 ore e una
  prima risposta di 4 ore · **Allora** riceve `400` in `application/problem+json` con la spiegazione che la
  risoluzione non può precedere la prima risposta, e nulla viene salvato

**CA-3 — Politica di coda**
- **Dato** una coda «Amministrazione» a cui è assegnata una politica più lenta della predefinita
- **Quando** si legge quale politica si applica alle richieste di quella coda
- **Allora** risulta quella della coda, mentre le richieste delle altre code restano sulla predefinita

**CA-4 — Tetto delle politiche**
- **Dato** un account che ha già cinque politiche · **Quando** ne crea una sesta · **Allora** riceve `422` con un
  messaggio che spiega la scelta di prodotto, e nessuna politica viene creata

**CA-5 — Obiettivo assente**
- **Dato** una politica in cui la priorità «bassa» ha entrambi gli obiettivi lasciati vuoti
- **Quando** si legge la politica
- **Allora** la priorità «bassa» risulta esplicitamente senza promessa, ed è distinguibile da un obiettivo pari a
  zero ore

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie politiche e le proprie code
- **Quando** un utente di `A` chiede l'elenco delle politiche, o prova ad assegnare a una propria coda
  l'identificativo di una politica di `B`
- **Allora** vede solo le proprie e l'assegnazione viene rifiutata come se la politica di `B` non esistesse

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione degli obiettivi e sulla risoluzione «quale politica si applica a questa
      coda», di **integrazione** sulle rotte con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su politiche e assegnazione alle code;
- [ ] **prova end-to-end**: *rimando* alla storia `0037`, proprietaria del percorso `[J-HELPDESK]`, dove la
      politica è il presupposto del passo sulle scadenze; motivo e storia proprietaria annotati nel registro di
      copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato: nessuna voce nuova di persone, tabelle di configurazione presenti in
      esportazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, in particolare sui valori predefiniti
      proposti e sul tetto di cinque politiche;
- [ ] contratto degli **strumenti conversazionali**: esclusione deliberata, annotata con il motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0023` (orario di servizio) | Le ore lavorative non esistono senza il calendario dell'azienda |
| storia `0021` (priorità ed etichette) | Gli obiettivi si differenziano per priorità: senza le quattro priorità la politica non ha righe |
| storia `0019` (code di lavoro) | Serve la coda a cui associare una politica diversa dalla predefinita |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: nessuno strumento dichiarato qui |

## 7. Fuori ambito

- **Il calcolo delle scadenze sulle richieste**, la pausa dell'orologio e la registrazione delle violazioni: storia
  `0025`. Qui si scrive la promessa, là la si misura.
- **Gli avvisi all'operatore** prima che la scadenza passi: storia `0026`.
- **I numeri di rispetto della promessa** (quante violate, quanto tempo medio): storia `0028`.
- **Una politica per cliente o per contratto** («questo cliente ha comprato l'assistenza prioritaria»): rimandata
  perché è il primo passo verso i livelli di supporto che il [documento capofila](../application-description.md)
  §2.5 esclude dal perimetro. Se il cliente ne ha bisogno, ha comprato il prodotto sbagliato.
- **Le condizioni di applicazione a più rami** (per canale, per etichetta, per orario di arrivo): stessa ragione —
  sarebbe un motore di regole.
- **La pubblicazione della promessa verso il cliente finale** (mostrarla sul portale o nel messaggio di conferma):
  rimandata alle storie del portale, epica 06.

## 8. Punti aperti

- **I valori predefiniti degli obiettivi** (prima risposta 16 / 8 / 4 / 2 ore lavorative, risoluzione 40 / 24 / 8 /
  4) sono una proposta ricavata dalla forma dell'ancoraggio di mercato, non da un dato affidabile: le fonti sono
  dichiarate di parte nel [documento capofila](../application-description.md) §2.6, fonte 8. **Decide lo
  sviluppatore**, sapendo che un obiettivo troppo ambizioso produce violazioni continue e fa smettere di guardare
  la misura.
- **Il tetto di cinque politiche** è una scelta di prodotto per tenere l'app lontana dal motore di regole, non un
  vincolo tecnico. **Decide lo sviluppatore.**
- **Se il livello di servizio debba restare in tutti i piani** o diventare un elemento di differenziazione fra
  `free`, `team` e `business`: la proposta del capofila §5 è di non spostarlo, ma la conferma spetta allo
  sviluppatore insieme al listino (punto aperto n. 1 del capofila §11).
