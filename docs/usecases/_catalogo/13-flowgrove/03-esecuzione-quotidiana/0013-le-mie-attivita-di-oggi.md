# 0013 — Le mie attività di oggi

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 03 — Esecuzione quotidiana
**Storia**: `0013` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come tecnico che apre l'app due minuti la mattina e due la sera
> voglio una sola schermata con quello che tocca a me
> così da non dover imparare l'app per usarla.

**Contesto.** È la schermata più importante per l'adozione. Il titolare guarda progetti e margini; chi esegue
guarda **solo questa**, e se non la trova subito smette di usare l'app — e senza di lui non ci sono ore, e senza
ore non c'è margine. È anche il luogo naturale dove mettere l'inserimento delle ore: la persona è già lì e sta
già guardando l'attività su cui ha lavorato (storia 0017).

## 2. Requisiti funzionali

1. **RF-1** — La schermata mostra le attività assegnate a chi sta guardando, raggruppate in: **in ritardo**,
   **oggi**, **prossimi sette giorni**, **senza scadenza**.
2. **RF-2** — Da ogni riga si cambia stato con un gesto solo, senza aprire il dettaglio.
3. **RF-3** — Ogni riga mostra le ore che la persona ha già dichiarato oggi su quella attività, e offre
   l'inserimento rapido di una durata (la scrittura è della storia 0017).
4. **RF-4** — La schermata mostra il totale delle ore che la persona ha dichiarato **oggi**, come promemoria per
   sé — non come misura per altri.
5. **RF-5** — Se non c'è nulla assegnato, lo stato vuoto lo dice e propone di guardare l'elenco delle attività del
   progetto: mai una pagina bianca.
6. **RF-6** — La schermata è la prima che si apre entrando nel modulo per un utente con ruolo `member`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'interrogazione filtra per `tenant_id` dal token verificato **e** per
  `sub` (l'identificativo dell'utente che chiama): la persona non può chiedere le attività di un'altra tramite
  questa rotta, nemmeno passandone l'identificativo.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/progetti/v1/me/tasks`; l'identità viene **solo**
  dal token; errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; indice `(tenant_id, user_id, due_date)` a supporto
  dell'interrogazione.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Le mie attività*; schermata d'atterraggio predefinita per il
  ruolo `member`; solo token del sistema di design; tema chiaro e scuro; funziona bene su schermo stretto, perché
  è la schermata che si guarda dal telefono.
- **RT-5 — Cinque lingue (§4).** Titoli dei gruppi, stato vuoto e messaggi in `en, it, fr, es, de`; le date
  relative («oggi», «domani») si formattano secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota in lettura. Con abbonamento `canceled` risponde
  `402`; con `past_due` funziona.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `get_my_tasks(periodo?)`, **lettura**, con la stessa
  regola di identità: restituisce solo le attività di chi chiama, mai quelle di un'altra persona (storia 0028).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la schermata mostra dati della persona a sé
  stessa.
- **RT-9 — Registrazione eventi (§14).** Nessun evento di dominio proprio; i cambi di stato fatti da qui
  registrano come nella storia 0011.

## 4. Criteri di accettazione

**CA-1 — Raggruppamento**
- **Dato** una persona con due attività scadute, una per oggi e tre senza scadenza
- **Quando** apre *Le mie attività*
- **Allora** vede i quattro gruppi nell'ordine previsto, con i conteggi giusti e le scadute in cima

**CA-2 — Cambio di stato rapido**
- **Dato** un'attività in `da fare` nella riga di oggi
- **Quando** la persona la porta a `in corso` dal gesto rapido
- **Allora** lo stato cambia senza aprire il dettaglio e la riga si aggiorna

**CA-3 — Nessuna attività di altri**
- **Dato** due persone `X` e `Y` dello stesso account, ciascuna con proprie attività
- **Quando** `X` chiama la rotta passando l'identificativo di `Y`
- **Allora** riceve comunque solo le proprie attività: l'identità arriva dal token e il parametro viene ignorato

**CA-4 — Stato vuoto**
- **Dato** una persona senza attività assegnate
- **Quando** apre la schermata
- **Allora** vede il messaggio che lo spiega e un collegamento all'elenco delle attività del progetto

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` apre la schermata
- **Allora** non compare nessuna attività di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sul raggruppamento per scadenza e di **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account** e prova specifica che l'identità non si possa forzare dall'esterno;
- [ ] **prova end-to-end**: coprire ora — `[J-PROGETTI]` passa da qui per il cambio di stato e per l'inserimento
      delle ore (storia 0031); voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta della schermata d'atterraggio per ruolo;
- [ ] controllo automatico di **accessibilità** verde, compresa la verifica su schermo stretto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0012` | Senza assegnazioni non c'è nulla da mostrare |
| Storia `0017` | L'inserimento rapido delle ore si completa lì; finché non c'è, la riga mostra solo il totale a zero |

## 7. Fuori ambito

- le notifiche in tempo reale: storia 0016;
- la vista delle attività di un'altra persona: c'è la vista del carico per progetto (storia 0012), e non serve
  altro.

## 8. Punti aperti

- Nessuno.
