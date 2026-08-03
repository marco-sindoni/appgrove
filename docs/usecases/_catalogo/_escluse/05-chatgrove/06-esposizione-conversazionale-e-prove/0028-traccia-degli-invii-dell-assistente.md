# 0028 — Traccia degli invii dell'assistente

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0028` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio che usa l'assistente
> voglio poter risalire a chi ha mandato che cosa, se io a mano o io tramite l'assistente
> così da spiegare a un cliente perché ha ricevuto un messaggio, e da fidarmi di uno strumento che non nasconde
> quello che fa.

**Contesto.** Quando le azioni possono partire da due strade — l'interfaccia e la chat — la domanda «chi ha
mandato questo?» diventa quotidiana. Serve una traccia che distingua **l'origine** dell'azione senza
raddoppiare i registri. È anche il presidio che rende accettabile l'esposizione conversazionale: si può dare
potere all'assistente perché tutto ciò che fa resta scritto e attribuito a chi ha confermato.

## 2. Requisiti funzionali

1. **RF-1** — Ogni azione che scrive porta la propria **origine**: interfaccia, assistente conversazionale,
   lavorazione automatica dell'app.
2. **RF-2** — Le azioni con origine «assistente» portano anche l'identificativo della bozza e dell'utente che
   ha confermato.
3. **RF-3** — Il filo della conversazione mostra il segno di origine sui messaggi in uscita: «inviato da Marco»
   oppure «inviato da Marco tramite assistente».
4. **RF-4** — Esiste una vista di controllo, filtrabile per periodo, origine e tipo di azione, che elenca ciò
   che è stato fatto: chi, quando, cosa, con quale esito.
5. **RF-5** — La traccia è di sola scrittura: non si modifica e non si cancella se non con la cancellazione
   dell'account o dell'interessato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La traccia filtra per `tenant_id` preso dal token verificato; nessun
  account vede le azioni di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/chat_commerce/v1/audit`, paginata e
  filtrabile; nessuna rotta di scrittura o di cancellazione della traccia; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V20__traccia_azioni.sql`: tabella con `tenant_id`, chiave primaria
  UUID versione 7, origine, tipo di azione, riferimento all'oggetto, esito, autore e colonne di controllo.
  Nessuna cancellazione logica: la traccia si cancella solo fisicamente, con l'account o con l'interessato.
- **RT-4 — Ruoli (§6).** La vista di controllo è visibile a `owner` e `admin`; un `member` vede il segno di
  origine nel filo ma non la vista completa.
- **RT-5 — Dati personali (§10).** La traccia riferisce azioni a utenti (dati di lavoro) e a contatti (dati di
  clienti finali): va dichiarata nel manifesto in italiano e inglese e aggiunta a `exportData` e `purgeData`.
  **Non** contiene il testo dei messaggi: contiene riferimenti.
- **RT-6 — Registrazione eventi (§14).** La traccia applicativa è una cosa diversa dai registri tecnici: i
  registri restano senza dati personali, la traccia contiene riferimenti perché serve a rispondere a una
  domanda del cliente.
- **RT-7 — Modulo frontend (§3, §4, §5).** Vista dentro Impostazioni; segno di origine nel filo. Tutte le
  stringhe in `en, it, fr, es, de`.

## 4. Criteri di accettazione

**CA-1 — Origine visibile**
- **Dato** un messaggio inviato dall'interfaccia e uno inviato confermando una bozza dell'assistente
- **Quando** si apre il filo · **Allora** il primo risulta «inviato da Marco» e il secondo «inviato da Marco
  tramite assistente»

**CA-2 — Vista di controllo**
- **Dato** una giornata con dieci azioni di scrittura
- **Quando** si apre la vista filtrando per origine «assistente» · **Allora** si vedono solo quelle, con
  autore, orario, tipo ed esito

**CA-3 — La traccia non si tocca**
- **Dato** una voce di traccia · **Quando** si tenta di modificarla o cancellarla da qualunque rotta
- **Allora** non esiste alcuna rotta che lo consenta

**CA-4 — Ruolo insufficiente**
- **Dato** un utente `member` · **Quando** apre la vista di controllo · **Allora** riceve `403`

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** un utente di `A` legge la traccia · **Allora** vede solo le proprie azioni

**CA-6 — Cancellazione dell'interessato**
- **Dato** un contatto cancellato su sua richiesta · **Quando** si esegue la cancellazione
- **Allora** le voci di traccia che lo riferiscono sono cancellate fisicamente insieme al resto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla marcatura dell'origine e di **integrazione** sulla vista e sulla cancellazione;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla traccia;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`, dove la verifica dell'origine è un'asserzione del
      percorso `[J-CHAT-COMMERCE]`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, tabella in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la distinzione fra traccia applicativa e registri tecnici;
- [ ] contratto degli **strumenti conversazionali**: la traccia è esposta in **lettura**, così che si possa
      chiedere «che cosa hai mandato ieri?»;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0027` | L'origine «assistente» esiste solo se esistono bozza e conferma |

## 7. Fuori ambito

- la traccia delle **letture** (chi ha guardato cosa): sarebbe un registro molto più voluminoso e con un
  bilancio costi-benefici diverso; non è in questa versione;
- l'esportazione della traccia verso un sistema esterno di conservazione: non richiesta.

## 8. Punti aperti

- **Per quanto tempo conservare la traccia.** Proposta: 24 mesi, come gli ordini. È un dato che serve a
  rispondere a contestazioni, quindi ha senso che duri quanto l'ordine a cui si riferisce. Da confermare
  insieme alle altre durate di conservazione.
