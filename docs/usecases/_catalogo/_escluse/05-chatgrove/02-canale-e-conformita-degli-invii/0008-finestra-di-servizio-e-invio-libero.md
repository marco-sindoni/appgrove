# 0008 — Finestra di servizio e risposta libera

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 02 — Canale di messaggistica e conformità degli invii
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0004`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che risponde ai clienti
> voglio rispondere direttamente dall'app e capire subito quando non posso più farlo liberamente
> così da non scoprire dopo l'invio che il messaggio non è partito o che mi è costato.

**Contesto.** La regola più importante del canale è anche la meno intuitiva: si può rispondere liberamente solo
entro **24 ore** dall'ultimo messaggio del cliente; dopo, si può scrivere **soltanto** con un modello
approvato, che si paga
([documentazione ufficiale](https://developers.facebook.com/docs/whatsapp/pricing/)). Nessun negoziante
conosce questa regola prima di sbatterci contro. Renderla visibile nell'interfaccia — con un contatore alla
rovescia e un blocco che spiega — è metà del valore dell'app, e la ragione per cui questa storia viene subito
dopo la ricezione.

## 2. Requisiti funzionali

1. **RF-1** — Dalla conversazione l'addetto scrive una risposta e la invia; il messaggio compare nel filo con
   il suo stato di consegna.
2. **RF-2** — Se la finestra di servizio è **aperta**, l'invio è libero e **non consuma quota**.
3. **RF-3** — Se la finestra è **chiusa**, la casella di scrittura è disattivata e al suo posto compare una
   spiegazione: perché non si può scrivere, che cosa serve (un modello approvato) e quanto costerebbe in quota.
4. **RF-4** — La conversazione mostra il tempo che resta alla chiusura della finestra, in forma comprensibile
   («puoi rispondere liberamente ancora per 3 ore»).
5. **RF-5** — Un addetto può **prendere in carico** una conversazione; il suo nome compare agli altri addetti,
   e la presa in carico si può rilasciare.
6. **RF-6** — Se l'invio fallisce presso il fornitore, il messaggio resta nel filo con stato «non inviato» e
   il motivo, e si può ritentare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di conversazioni e messaggi filtra per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/chat_commerce/v1/conversations/{id}/messages`
  e `POST|DELETE /api/chat_commerce/v1/conversations/{id}/assignee`; corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit. Il tentativo di invio libero
  a finestra chiusa risponde `409` con la spiegazione, non `500`.
- **RT-3 — Varchi e quota (§6, §7).** L'invio dentro la finestra **non** prenota quota; l'invio fuori dalla
  finestra passa dal punto unico di prenotazione della metrica `messaggi_template` (storia `0004`) — ma in
  questa storia è **vietato**, perché i modelli arrivano nella `0009`: fuori finestra si risponde `409`, non si
  aggira.
- **RT-4 — Persistenza (§8).** La scadenza della finestra è una colonna della conversazione, calcolata alla
  ricezione: non si ricalcola leggendo tutti i messaggi ogni volta.
- **RT-5 — Modulo frontend (§3, §4, §5).** Sezione Conversazioni del modulo `chat_commerce`; il tempo residuo
  e le spiegazioni passano dallo spazio-nomi `chat_commerce` in `en, it, fr, es, de`; solo token del sistema
  di design; funziona in tema chiaro e scuro; la casella disattivata è annunciata anche alle tecnologie
  assistive, non solo mostrata in grigio.
- **RT-6 — Dati personali (§10).** Nessun campo nuovo: il corpo del messaggio in uscita rientra nella voce
  `message.body` già dichiarata. L'autore del messaggio in uscita è un dato di lavoro, già dichiarato come
  `conversation.assignee`.
- **RT-7 — Registrazione eventi (§14).** `messaggio inviato`, `invio respinto per finestra chiusa`, `invio
  fallito`, `conversazione presa in carico` con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza il corpo del messaggio.

## 4. Criteri di accettazione

**CA-1 — Risposta dentro la finestra**
- **Dato** una conversazione il cui cliente ha scritto un'ora fa
- **Quando** l'addetto invia una risposta
- **Allora** il messaggio parte, compare nel filo con stato «consegnato» e il contatore della quota **non**
  aumenta

**CA-2 — Finestra chiusa**
- **Dato** una conversazione il cui ultimo messaggio del cliente è di 30 ore fa
- **Quando** l'addetto apre la conversazione
- **Allora** la casella di scrittura è disattivata, con la spiegazione del perché e l'indicazione che serve un
  modello approvato

**CA-3 — Tentativo di aggiramento**
- **Dato** la stessa conversazione con finestra chiusa
- **Quando** si chiama direttamente la rotta di invio libero
- **Allora** la risposta è `409` con `application/problem+json`, **nulla viene inviato** e l'evento è registrato

**CA-4 — Presa in carico**
- **Dato** due addetti dello stesso account sulla stessa conversazione
- **Quando** il primo la prende in carico
- **Allora** il secondo vede chi l'ha presa e da quando, e può comunque scrivere se serve — l'app segnala, non
  impedisce

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con le proprie conversazioni
- **Quando** un utente di `A` tenta di inviare un messaggio in una conversazione di `B` indicandone
  l'identificativo
- **Allora** riceve `404`, e nulla viene inviato

**CA-6 — Invio fallito**
- **Dato** il canale che rifiuta l'invio
- **Quando** l'addetto invia · **Allora** il messaggio resta nel filo come «non inviato» con il motivo, ed
  esiste l'azione «riprova»

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della finestra e di **integrazione** sull'invio, comprese finestra chiusa
      e fallimento del canale;
- [ ] prova di **isolamento fra account** sull'invio e sulla presa in carico;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`, dove la risposta dentro la finestra è un passo del
      percorso `[J-CHAT-COMMERCE]`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i testi del blocco a finestra chiusa;
- [ ] **manifesto dei dati**: nessuna voce nuova, verificato che il corpo in uscita rientri in `message.body`;
- [ ] **registro delle decisioni** compilato, con la scelta di disattivare la casella invece di lasciarla
      attiva e fallire dopo;
- [ ] contratto degli **strumenti conversazionali**: `invia_messaggio` dichiarato come **scrittura verso
      l'esterno con conferma umana obbligatoria** (contratto completo nella storia `0027`);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0004` | Serve il punto unico di prenotazione della quota, anche solo per non usarlo dentro la finestra |
| `0007` | Servono conversazioni vere e la scadenza della finestra calcolata alla ricezione |

## 7. Fuori ambito

- l'invio **fuori** dalla finestra con un modello approvato: storia `0009`;
- le risposte automatiche: storia `0025`;
- il carrello e l'ordine dentro la conversazione: epica 04.

## 8. Punti aperti

- Nessuno.
