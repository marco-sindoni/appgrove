# 0009 — Traguardi del progetto

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 02 — Progetti e struttura del lavoro
**Storia**: `0009` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha promesso al cliente «la prima consegna entro il 20»
> voglio segnare quella promessa come un traguardo e appenderci le attività che la compongono
> così da sapere, guardando una riga sola, se la promessa regge.

**Contesto.** La scheda di catalogo elenca le milestone di progetto fra i casi d'uso principali. In una
micro-impresa il traguardo non è uno strumento di pianificazione: è una **promessa fatta a un cliente**, spesso
scritta nel preventivo. Da lì discendono le due scelte di questa storia: il traguardo ha una data e un nome
comprensibile al cliente, e il suo stato si deduce dalle attività collegate invece di essere aggiornato a mano —
perché lo stato aggiornato a mano è sempre vecchio.

## 2. Requisiti funzionali

1. **RF-1** — Un progetto può avere traguardi, ciascuno con titolo, data prevista e note facoltative.
2. **RF-2** — Ogni attività può essere collegata a **un** traguardo (facoltativo).
3. **RF-3** — Lo stato del traguardo è **derivato**: `da raggiungere` finché almeno un'attività collegata è
   aperta, `raggiunto` quando tutte sono terminate (con la data del raggiungimento), `a rischio` quando la data
   prevista è vicina o superata e restano attività aperte.
4. **RF-4** — Un traguardo senza attività collegate è `da raggiungere` e viene segnalato come tale nella scheda:
   una promessa senza lavoro dietro è quasi sempre una dimenticanza.
5. **RF-5** — Il riepilogo dei traguardi compare nella scheda del progetto, ordinato per data, con quante attività
   sono aperte su quante.
6. **RF-6** — Cancellare un traguardo non cancella le attività: le scollega e lo dice prima di procedere.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `milestone` filtra per `tenant_id` dal
  token verificato; il collegamento verifica che attività e traguardo appartengano allo stesso progetto e allo
  stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/progetti/v1/projects/{id}/milestones` e
  `PATCH|DELETE /api/progetti/v1/milestones/{id}`; errori in `application/problem+json`; OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V4__traguardi.sql`: la tabella `milestone` riceve vincoli e indice
  `(tenant_id, project_id, due_date)`; `task.milestone_id` come riferimento **dentro lo stesso schema**; colonne
  di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro dei traguardi nella scheda del progetto, con la barra di
  completamento derivata; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Titoli degli stati derivati e messaggi in `en, it, fr, es, de`; le date si
  formattano secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Ruolo minimo per creare e cancellare: `admin`.
- **RT-7 — Esposizione conversazionale (§12).** I traguardi compaiono nel risultato di
  `get_project_progress(id)` (**lettura**, storia 0028); non è previsto uno strumento di scrittura dedicato.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il traguardo descrive lavoro, non persone. Il
  campo note è testo libero e porta l'avviso in linea.
- **RT-9 — Registrazione eventi (§14).** «Traguardo creato», «traguardo raggiunto» con `tenant_id`, `app_id`,
  `user_id` e correlazione; mai il titolo, che è contenuto del cliente.

## 4. Criteri di accettazione

**CA-1 — Stato derivato**
- **Dato** un traguardo con quattro attività collegate, tre terminate
- **Quando** si apre la scheda del progetto
- **Allora** il traguardo risulta `da raggiungere` con «3 di 4 completate»

**CA-2 — Raggiungimento**
- **Dato** lo stesso traguardo
- **Quando** l'ultima attività passa a `fatta`
- **Allora** il traguardo diventa `raggiunto` con la data di oggi, senza che nessuno lo aggiorni a mano

**CA-3 — A rischio**
- **Dato** un traguardo con data prevista ieri e due attività aperte
- **Quando** si apre la scheda
- **Allora** il traguardo è marcato `a rischio` e compare in cima al riepilogo

**CA-4 — Cancellazione**
- **Dato** un traguardo con attività collegate
- **Quando** si chiede di cancellarlo
- **Allora** compare l'avviso che le attività verranno scollegate; confermando, le attività restano e il
  collegamento sparisce

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` collega un'attività a un traguardo di `B`
- **Allora** riceve `404` e nulla cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sul calcolo dello stato derivato (compresi i casi limite: zero attività, tutte annullate)
      e di **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account** su tutte le rotte introdotte;
- [ ] **prova end-to-end**: rimando — il traguardo compare nel percorso `[J-PROGETTI]` solo come lettura
      dell'avanzamento (storia 0025); motivo registrato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta dello stato derivato invece che dichiarato;
- [ ] controllo automatico di **accessibilità** verde sul riquadro dei traguardi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | Servono le attività da collegare |
| Storia `0006` | Il traguardo vive dentro un progetto |

## 7. Fuori ambito

- il pagamento a stato di avanzamento («fattura il 30 % al primo traguardo»): è una funzione della fatturazione a
  fasi, rimandata; oggi la fatturazione parte dalle ore (storia 0022);
- la notifica al cliente del traguardo raggiunto: non c'è portale né invio verso l'esterno (§1 della descrizione).

## 8. Punti aperti

- **Fatturazione a stato di avanzamento**: se il cliente vende a corpo con acconti legati ai traguardi, la storia
  0022 non basta. È una decisione di prodotto che tocca anche 02 BillGrove e 06 QuoteGrove e va presa lì, non
  qui.
