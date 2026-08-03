# 0006 — Anagrafica delle sedi

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 02 — Sedi e collegamento alle piattaforme
**Storia**: `0006` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'attività con uno o più punti vendita
> voglio dichiarare le mie sedi, con nome, indirizzo e settore
> così da avere per ciascuna un punteggio, un flusso di recensioni e un invito separati, senza mescolare cose che
> il cliente finale vive come diverse.

**Contesto.** La sede è l'unità attorno a cui gira tutta l'app: è ciò che si collega a una piattaforma, ciò che ha
un punteggio, ciò che consuma quota (descrizione §3). Va prima di tutto il resto dell'epica. Un dato che sembra
burocratico e non lo è: il **settore dichiarato**. La legge italiana 34/2026 si applica solo a ristorazione,
strutture ricettive, stabilimenti termali e attrazioni turistiche situate in Italia (descrizione §2.3): sapere in
che settore sta la sede decide se la finestra dei trenta giorni della storia 0015 è un obbligo o un consiglio.

## 2. Requisiti funzionali

1. **RF-1** — Si possono creare, modificare, sospendere e riattivare le sedi. Ogni sede ha nome, indirizzo, paese,
   fuso orario, settore dichiarato e stato (`attiva`, `sospesa`).
2. **RF-2** — La creazione di una sede **attiva** passa dal varco della quota `sedi_monitorate` (storia 0004): a
   tetto raggiunto risponde `429` e non crea nulla.
3. **RF-3** — Il settore si sceglie da un elenco chiuso, e l'elenco distingue esplicitamente i quattro settori a
   cui si applica la legge italiana sulle recensioni dagli altri. La scelta non è cosmetica: cambia il
   comportamento della storia 0015 e va spiegata nell'interfaccia in una riga.
4. **RF-4** — Una sede non si cancella finché ha recensioni collegate: si **sospende**. La cancellazione logica
   resta possibile solo per una sede senza storia, e la cancellazione fisica solo attraverso i diritti
   dell'interessato o la chiusura dell'account.
5. **RF-5** — L'elenco delle sedi mostra, per ciascuna, lo stato del collegamento alle piattaforme (in questa
   storia sempre «nessuno») e il numero di recensioni raccolte, così da diventare la schermata di regia dell'app.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `sede` filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/recensioni/v1/sedi`,
  `GET|PUT /api/recensioni/v1/sedi/{id}`, `POST /api/recensioni/v1/sedi/{id}/sospendi` e `/riattiva`; corpo
  validato in modo dichiarativo; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Nessuna migrazione nuova se la tabella `sede` della storia 0002 è completa;
  altrimenti `V3__sede_settore.sql` sullo schema `app_recensioni`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Sedi* del modulo `recensioni`: elenco con stato, scheda di
  dettaglio, modulo di inserimento con React Hook Form e Zod. Dati letti con il client generato; solo token del
  sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, **compresi i nomi dei settori**, passano dallo spazio-nomi
  `recensioni` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Prima di rendere `attiva` una sede il servizio verifica il tetto della
  metrica `sedi_monitorate` (natura `stock`); a quota esaurita risponde `429` con l'indicazione del rimedio. Con
  abbonamento non attivo risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: la sede compare come filtro negli
  strumenti di lettura dell'epica 06.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: la sede è un dato dell'azienda cliente. Va
  detto esplicitamente nel registro delle decisioni, perché è controintuitivo (un indirizzo non è sempre un dato
  aziendale: se un artigiano dichiara la propria abitazione, quell'indirizzo è anche un dato personale suo — vedi
  i punti aperti).
- **RT-9 — Registrazione eventi (§14).** `sede creata`, `sede sospesa`, `sede riattivata`, `sede respinta per
  quota`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza indirizzi.

## 4. Criteri di accettazione

**CA-1 — Creazione di una sede**
- **Dato** un account abilitato, sotto il tetto della quota
- **Quando** crea una sede con nome, indirizzo, paese, fuso e settore
- **Allora** la sede compare nell'elenco in stato `attiva` e il contatore della quota aumenta di uno

**CA-2 — Settore obbligatorio e chiuso**
- **Dato** il modulo di inserimento
- **Quando** si tenta di salvare senza settore o con un valore fuori elenco
- **Allora** la richiesta è rifiutata con `400` e il messaggio dice quali sono i valori ammessi

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie sedi
- **Quando** un utente di `A` chiede l'elenco delle sedi o il dettaglio di una sede di `B`
- **Allora** vede solo le proprie e sul dettaglio altrui riceve `404`, anche forzando l'identificativo dell'altro
  account nella richiesta

**CA-4 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `sedi_monitorate`
- **Quando** tenta di creare una sede attiva
- **Allora** riceve `429`, un messaggio che spiega come rimediare, e **nessuna sede viene creata**

**CA-5 — Una sede con storia non si cancella**
- **Dato** una sede con recensioni raccolte
- **Quando** si tenta di cancellarla
- **Allora** l'operazione è rifiutata con un messaggio che propone la sospensione, e i dati restano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione e di **integrazione** sulle rotte, con database effimero e migrazioni
      vere;
- [ ] prova di **isolamento fra account** sulla risorsa `sede`;
- [ ] **prova end-to-end**: *coprire ora* il passo «creazione della prima sede» dentro il percorso
      `[J-RECENSIONI]` avviato dalla storia 0030 — oppure, se 0030 non è ancora scritta, voce `da-coprire` nel
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con 0030 come
      proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, nomi dei settori compresi;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il motivo è scritto;
- [ ] **registro delle decisioni** compilato, con la motivazione dell'elenco chiuso dei settori;
- [ ] controllo automatico di **accessibilità** verde sulle schermate introdotte.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | serve la tabella `sede` |
| storia `0004` | la creazione di una sede attiva attraversa il varco della quota |

## 7. Fuori ambito

- il collegamento della sede a una piattaforma — storie 0007 e 0008;
- il punteggio della sede — storia 0022;
- la gestione dei permessi per sede (chi vede quale sede): vedi i punti aperti.

## 8. Punti aperti

- **Permessi per sede.** Con più sedi è naturale che il responsabile di una sede veda solo la sua. La matrice dei
  ruoli di piattaforma (`owner`, `admin`, `member`) non ha il concetto di «ambito per sede». Non lo invento qui:
  nella prima versione tutti gli utenti dell'account vedono tutte le sedi, e la restrizione per sede è un punto
  aperto di prodotto da valutare quando esisteranno clienti con più di due sedi.
- **Indirizzo dell'artigiano che coincide con l'abitazione.** In quel caso l'indirizzo della sede è anche un dato
  personale del titolare. Non cambia il manifesto (il titolare è utente della piattaforma, non interessato di
  questa app), ma va verificato con lo sviluppatore in sede di classificazione.
- **Se i settori sanitari restassero fuori dal perimetro** (descrizione §11.7), questa storia deve rifiutarli
  esplicitamente all'inserimento, con un messaggio che spiega perché.
</content>
