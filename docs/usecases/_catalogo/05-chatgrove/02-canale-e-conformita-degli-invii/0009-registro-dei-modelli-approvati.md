# 0009 — Registro dei modelli approvati

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 02 — Canale di messaggistica e conformità degli invii
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio vedere quali messaggi preconfezionati posso davvero inviare, in che lingua e a che categoria
> appartengono
> così da poter scrivere a un cliente anche dopo un giorno, senza che il messaggio venga rifiutato.

**Contesto.** Fuori dalla finestra di servizio si può scrivere solo con un **modello approvato** dal fornitore.
I modelli hanno una categoria (marketing, utility, authentication) che ne determina il prezzo, una lingua, un
corpo con segnaposto e uno **stato di approvazione** che può cambiare da solo: un modello mal ricevuto dai
destinatari viene sospeso o disabilitato
([linee guida ufficiali](https://developers.facebook.com/docs/whatsapp/message-templates/guidelines/)). L'app
deve conoscere questa realtà, non subirla: senza il registro dei modelli, la storia `0008` resta a metà e le
campagne dell'epica 05 sono impossibili.

## 2. Requisiti funzionali

1. **RF-1** — L'app mantiene l'elenco dei modelli del numero collegato, con nome, categoria, lingua, corpo con
   i segnaposto e stato di approvazione.
2. **RF-2** — L'elenco si **allinea** al fornitore su richiesta dell'utente e periodicamente; l'orario
   dell'ultimo allineamento è visibile.
3. **RF-3** — Un modello non approvato (in attesa, respinto, sospeso, disabilitato) **non è selezionabile**
   per l'invio, e l'interfaccia dice perché.
4. **RF-4** — Dalla conversazione con finestra chiusa l'addetto sceglie un modello, compila i segnaposto e
   invia; il messaggio **consuma una unità** della metrica `messaggi_template`.
5. **RF-5** — Prima dell'invio l'app mostra l'anteprima del messaggio con i segnaposto risolti: si vede quello
   che vedrà il cliente.
6. **RF-6** — Se il modello richiede un consenso preventivo del destinatario (categoria marketing), l'invio è
   bloccato per i contatti che non l'hanno dato (storia `0010`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I modelli appartengono al numero, quindi all'account: ogni lettura
  filtra per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/chat_commerce/v1/templates`,
  `POST /api/chat_commerce/v1/templates/sync` e
  `POST /api/chat_commerce/v1/conversations/{id}/messages/template`; corpo validato (i valori dei segnaposto
  sono obbligatori e di numero esatto); errori in `application/problem+json`; definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V4__modelli.sql` sullo schema `app_chat_commerce`: tabella
  `message_template` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione
  logica. Lo stato di approvazione è una copia locale di un dato che appartiene al fornitore: si aggiorna, non
  si inventa.
- **RT-4 — Varchi e quota (§6, §7).** Prima di inviare un modello il servizio prenota una unità della metrica
  `messaggi_template` (natura `flow`); a quota esaurita risponde `429` con l'indicazione del rimedio e **nulla
  parte**. Con abbonamento `canceled` risponde `402`.
- **RT-5 — Modulo frontend (§3, §4, §5).** Sezione dei modelli dentro Impostazioni e selettore dentro la
  conversazione; tutte le stringhe in `en, it, fr, es, de`; solo token del sistema di design. Attenzione a non
  confondere due elenchi di lingue: l'**interfaccia** è a cinque lingue, mentre le lingue dei **modelli** sono
  quelle che il negozio ha registrato presso il fornitore e possono essere altre.
- **RT-6 — Dati personali (§10).** Il modello in sé non contiene dati personali; i **valori dei segnaposto**
  possono contenerli (nome del cliente, numero d'ordine) e finiscono in `message.body`, già dichiarato. Nessuna
  voce nuova, ma va verificato che i valori non finiscano nei registri.
- **RT-7 — Registrazione eventi (§14).** `modelli allineati`, `modello inviato`, `invio respinto per modello
  non approvato`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** i valori dei
  segnaposto.

## 4. Criteri di accettazione

**CA-1 — Allineamento**
- **Dato** un account con canale collegato e quattro modelli presso il fornitore
- **Quando** chiede l'allineamento
- **Allora** l'elenco mostra i quattro modelli con categoria, lingua e stato, e l'orario dell'allineamento

**CA-2 — Invio fuori finestra**
- **Dato** una conversazione con finestra chiusa e un modello approvato
- **Quando** l'addetto compila i segnaposto e invia
- **Allora** il messaggio parte, compare nel filo e il contatore `messaggi_template` aumenta di uno

**CA-3 — Modello non approvato**
- **Dato** un modello in stato «sospeso»
- **Quando** l'addetto tenta di selezionarlo
- **Allora** non è selezionabile e l'interfaccia spiega lo stato; una chiamata diretta alla rotta risponde
  `409` e nulla parte

**CA-4 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `messaggi_template`
- **Quando** tenta di inviare un modello
- **Allora** riceve `429` con il rimedio, e nulla viene inviato

**CA-5 — Segnaposto mancanti**
- **Dato** un modello con tre segnaposto · **Quando** se ne compilano due · **Allora** la richiesta è respinta
  con `400` e l'indicazione del segnaposto mancante, prima di qualunque contatto con il fornitore

**CA-6 — Isolamento fra account**
- **Dato** due account con modelli diversi
- **Quando** un utente di `A` chiede l'elenco dei modelli
- **Allora** vede solo i propri, anche indicando l'identificativo di un modello di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla risoluzione dei segnaposto e di **integrazione** sull'allineamento e sull'invio,
      con il canale simulato;
- [ ] prova di **isolamento fra account** sui modelli;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: verificato che i valori dei segnaposto rientrino in `message.body` e non finiscano
      nei registri;
- [ ] **registro delle decisioni** compilato, con la scelta di tenere una copia locale dello stato di
      approvazione;
- [ ] contratto degli **strumenti conversazionali**: `invia_modello` dichiarato come **scrittura verso
      l'esterno con conferma umana obbligatoria**;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | Serve la connessione per allinearsi al fornitore |
| `0008` | Serve la conversazione con la finestra di servizio già gestita |
| `0010` | **Non** è una dipendenza tecnica ma logica: senza il consenso, l'invio di modelli marketing va bloccato — le due storie vanno in ordine `0009` → `0010`, e il blocco si chiude nella `0010` |

## 7. Fuori ambito

- la **creazione** di un modello nuovo e la richiesta di approvazione al fornitore: si fanno presso il
  fornitore; l'app li legge. Se il negozio ne avrà bisogno dentro l'app, è una storia futura;
- l'invio massivo dello stesso modello a molti contatti: storia `0023`;
- il punteggio di qualità del numero: [estensioni-admin.md](../estensioni-admin.md) e storia `0024`.

## 8. Punti aperti

- **Il costo in denaro** di ogni modello dipende dalla categoria e dal paese del destinatario (§2.2 della
  descrizione: da 0,0014 $ a oltre 0,11 €). Mostrarlo in euro richiede un listino delle tariffe che oggi non
  abbiamo e che invecchia in fretta. La storia `0023` mostra il costo **in quota**; mostrarlo **in denaro** è
  una decisione dello sviluppatore, legata alla scelta «chi paga il canale».
