# 0017 — Ciclo di vita dell'ordine

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 04 — Ordini e pagamenti
**Storia**: `0017` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio vedere a che punto è ogni ordine e chi l'ha spostato
> così da sapere la sera cosa devo ancora preparare e cosa devo ancora incassare.

**Contesto.** Un ordine senza stati è solo una lista della spesa: non dice cosa fare adesso. Gli stati sono
anche la base delle due funzioni che seguono — la richiesta di pagamento parte da un ordine `confermato`, il
recupero dei carrelli guarda quelli che non sono mai diventati ordini. La storia è piccola di proposito:
aggiunge una macchina a stati e il suo storico, niente altro.

## 2. Requisiti funzionali

1. **RF-1** — L'ordine attraversa gli stati `bozza` → `confermato` → `pagato` → `consegnato` → `chiuso`, e da
   `bozza` o `confermato` può passare ad `annullato`.
2. **RF-2** — I passaggi non previsti sono **rifiutati** con un messaggio che dice quali sono possibili da
   quello stato.
3. **RF-3** — Ogni passaggio registra chi l'ha fatto, quando e da quale stato a quale: lo storico è visibile
   nella scheda dell'ordine.
4. **RF-4** — L'annullamento **ripristina** la disponibilità a quantità scalata alla conferma.
5. **RF-5** — L'elenco degli ordini si filtra per stato e per periodo, e mostra il totale per stato.
6. **RF-6** — Un ordine `pagato` non torna a `confermato`: se serve, si registra una nota di restituzione, che
   nella prima versione è testo, non un movimento.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I cambi di stato e lo storico filtrano per `tenant_id` preso dal
  token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/chat_commerce/v1/orders/{id}/transitions`
  con lo stato di destinazione; corpo validato; il passaggio non ammesso risponde `409` con l'elenco di quelli
  possibili; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V10__stati_ordine.sql`: tabella `order_status_event` con
  `tenant_id`, chiave primaria UUID versione 7 e colonne di controllo. Il ripristino della disponibilità e il
  cambio di stato avvengono nella **stessa transazione**.
- **RT-4 — Ruoli (§6).** Tutti i ruoli possono far avanzare un ordine; solo `owner` e `admin` possono
  annullarlo. Un `member` che tenta l'annullamento riceve `403`.
- **RT-5 — Modulo frontend (§3, §4, §5).** Azioni di stato nella scheda dell'ordine, con conferma esplicita
  per l'annullamento; storico in forma di cronologia. Tutte le stringhe — **compresi i nomi degli stati** — in
  `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** Lo storico contiene l'identificativo dell'utente che ha agito: è un dato di
  lavoro già dichiarato nel manifesto come `conversation.assignee`; va esteso alla nuova tabella, che entra in
  `exportData` e `purgeData`.
- **RT-7 — Registrazione eventi (§14).** `ordine passato a <stato>` con `tenant_id`, `app_id`, `user_id`,
  numero dell'ordine e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Avanzamento**
- **Dato** un ordine in `bozza` · **Quando** lo si conferma · **Allora** passa a `confermato`, la disponibilità
  si scala e lo storico registra il passaggio con autore e orario

**CA-2 — Passaggio non ammesso**
- **Dato** un ordine `chiuso` · **Quando** si tenta di portarlo a `confermato` · **Allora** riceve `409` con
  l'elenco dei passaggi possibili, e lo stato non cambia

**CA-3 — Annullamento**
- **Dato** un ordine `confermato` che aveva scalato 2 pezzi
- **Quando** lo si annulla · **Allora** lo stato è `annullato` e la disponibilità è tornata a prima

**CA-4 — Ruolo insufficiente**
- **Dato** un utente `member` · **Quando** tenta di annullare un ordine · **Allora** riceve `403` e lo stato
  resta invariato

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** un utente di `A` tenta un passaggio su un ordine di `B` · **Allora**
  riceve `404`

**CA-6 — Filtro per stato**
- **Dato** un account con ordini in stati diversi · **Quando** si filtra su `confermato` · **Allora** si vedono
  solo quelli, con il totale complessivo di quello stato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati (compresi tutti i passaggi vietati) e di **integrazione** sul
      ripristino della disponibilità nella stessa transazione;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sui passaggi di stato;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i nomi degli stati;
- [ ] **manifesto dei dati** aggiornato con la tabella dello storico degli stati in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la macchina a stati scelta e il divieto di tornare indietro
      da `pagato`;
- [ ] contratto degli **strumenti conversazionali**: l'avanzamento di stato è **scrittura con conferma umana**
      (cambia la realtà per il cliente), l'annullamento a maggior ragione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0016` | Serve l'ordine |

## 7. Fuori ambito

- la restituzione del denaro: qui è una nota, non un movimento; un rimborso vero passerebbe dallo strumento di
  incasso del negozio;
- la spedizione e il tracciamento del corriere: dichiarati fuori perimetro nella descrizione dell'applicazione.

## 8. Punti aperti

- **Se lo stato `pagato` debba essere raggiunto solo dalla registrazione dell'incasso** (storia `0019`) o
  anche a mano: la proposta è entrambe le vie, perché nei mercati di destinazione si incassa anche in contanti
  alla consegna. Da confermare.
