# 0032 — Strumenti di scrittura con conferma

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 07 — Esposizione conversazionale e prove
**Storia**: `0032` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0031`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio poter dire «prenota la signora Bianchi giovedì alle tre» e vedere cosa sta per succedere prima che
> succeda
> così da avere la comodità della voce senza il rischio di impegnare il tempo di qualcuno per un malinteso.

**Contesto.** Il catalogo pone una regola di sicurezza non negoziabile (§8 del catalogo): gli strumenti di
**lettura** sono liberi, quelli di **scrittura** con effetti irreversibili producono una **bozza** e richiedono
una **conferma umana esplicita**. In questa applicazione la regola è particolarmente stringente, perché quasi
tutte le scritture toccano una persona fuori dall'azienda: prenotare impegna il tempo di un operatore, disdire
tocca un impegno preso, e mandare un messaggio o un'offerta parla direttamente a un cliente finale. L'assistente
prepara, la persona approva.

## 2. Requisiti funzionali

1. **RF-1** — Sono dichiarati cinque strumenti di scrittura: `crea_prenotazione`, `sposta_prenotazione`,
   `disdici_prenotazione`, `invia_promemoria`, `offri_posto_da_lista_attesa`.
2. **RF-2** — Ogni strumento di scrittura produce una **bozza** che descrive in parole comprensibili cosa
   accadrà: chi, quando, quale risorsa, quali messaggi partiranno e a chi.
3. **RF-3** — Nulla viene eseguito senza una **conferma umana esplicita**; la conferma è un atto separato, non un
   parametro dello strumento.
4. **RF-4** — I tre strumenti con effetti **verso l'esterno o irreversibili** — disdetta, promemoria, offerta —
   hanno la conferma **obbligatoria e non disattivabile**.
5. **RF-5** — La bozza **scade**: una conferma che arriva molto dopo trova uno stato del mondo diverso e va
   ricalcolata, non applicata alla cieca.
6. **RF-6** — Ogni esecuzione lascia traccia dell'origine conversazionale, così che in agenda si veda che quella
   prenotazione è nata da un assistente e chi l'ha confermata.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Contratto dentro il servizio; server di piattaforma non ancora
  implementato (UC 0061-0063). Ogni strumento dichiara nome stabile, parametri, risultato, marcatura
  **scrittura**, idempotenza e **necessità di conferma**.
- **RT-2 — Isolamento fra account (§1).** Il contesto dell'account arriva dal livello di piattaforma, mai dai
  parametri; la conferma vale solo per l'account e l'utente che l'hanno chiesta.
- **RT-3 — Interfaccia di programmazione (§2).** Le esecuzioni riusano le rotte esistenti delle storie `0014`,
  `0015`, `0020` e `0022`: nessuna scorciatoia che salti validazioni, vincoli o varchi.
- **RT-4 — Idempotenza.** La conferma porta la chiave della bozza: confermare due volte la stessa bozza produce
  un solo effetto.
- **RT-5 — Varchi e quota (§6, §7).** Gli strumenti attraversano la stessa catena dei varchi; nessuna scrittura
  conversazionale può aggirare `402` o `429`.
- **RT-6 — Cinque lingue (§4).** Il testo della bozza è comprensibile e disponibile in `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** La bozza contiene il nome della persona coinvolta: è un dato che esce verso il
  livello conversazionale e va dichiarato nel manifesto come tale.
- **RT-8 — Registrazione eventi (§14).** `bozza prodotta`, `bozza confermata`, `bozza scaduta` con `tenant_id`,
  `app_id`, `user_id`, strumento e correlazione — mai il contenuto della bozza.

## 4. Criteri di accettazione

**CA-1 — Bozza prima dell'effetto**
- **Dato** l'invocazione di `crea_prenotazione` · **Quando** si guarda il risultato · **Allora** è una bozza che
  descrive l'appuntamento, e in agenda non c'è ancora nulla

**CA-2 — Conferma umana**
- **Dato** una bozza · **Quando** una persona la conferma · **Allora** l'appuntamento compare in agenda, marcato
  come nato da assistente, con chi ha confermato

**CA-3 — Conferma obbligatoria sugli effetti esterni**
- **Dato** `invia_promemoria` o `offri_posto_da_lista_attesa` · **Quando** si prova a eseguirli senza conferma
- **Allora** non è possibile in nessun modo, nemmeno cambiando le impostazioni

**CA-4 — Bozza scaduta**
- **Dato** una bozza vecchia il cui spazio è stato nel frattempo occupato · **Quando** la si conferma · **Allora**
  la conferma è rifiutata con un messaggio comprensibile e nulla viene creato

**CA-5 — Doppia conferma**
- **Dato** la stessa bozza confermata due volte · **Quando** si guarda l'agenda · **Allora** c'è un solo
  appuntamento

**CA-6 — Varchi rispettati**
- **Dato** un account con abbonamento `canceled` · **Quando** si invoca uno strumento di scrittura · **Allora**
  riceve lo stesso rifiuto che darebbe l'interfaccia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`);
- [ ] prove di **unità** sulla scadenza della bozza e sull'idempotenza, e di **integrazione** sul riuso delle
      rotte esistenti;
- [ ] prova di **isolamento fra account** su bozze e conferme;
- [ ] **prova end-to-end**: *rimando* — il livello conversazionale non esiste ancora (UC 0061-0063); motivo e
      storia proprietaria dichiarati in
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dei testi delle bozze in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con i dati che escono nelle bozze;
- [ ] **registro delle decisioni** compilato: quali strumenti hanno conferma obbligatoria e perché;
- [ ] contratto degli **strumenti conversazionali** completo, lettura e scrittura, dentro il servizio;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0031` | il contratto di lettura e la sua impalcatura |
| storie `0014`, `0015`, `0020`, `0022` | sono le operazioni che gli strumenti eseguono |
| UC 0061-0063 (livello conversazionale di piattaforma) | non ancora implementati |

## 7. Fuori ambito

- la prenotazione fatta a voce **dal cliente finale**: richiederebbe di autenticare una persona che non è un
  nostro utente ed è fuori dal perimetro;
- l'esecuzione automatica senza conferma per gli strumenti «poco rischiosi»: **deliberatamente esclusa**, perché
  qui anche il meno rischioso impegna il tempo di una persona.

## 8. Punti aperti

**Chi può confermare.** Se l'assistente lavora per conto del titolare, la conferma è del titolare; ma se lo usa
un addetto con ruolo limitato, la conferma deve rispettare il suo ruolo. La regola proposta è che la conferma
valga esattamente quanto l'atto compiuto dall'interfaccia dallo stesso utente. Da confermare quando esisteranno
il consenso delegato e le sue regole (UC 0062).
