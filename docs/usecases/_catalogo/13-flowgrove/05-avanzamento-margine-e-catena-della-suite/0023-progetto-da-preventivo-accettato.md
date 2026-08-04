# 0023 — Progetto da preventivo accettato

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 05 — Avanzamento, margine e catena della suite
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare a cui il cliente ha appena firmato il preventivo
> voglio che il progetto nasca da solo, con dentro il budget e le voci offerte
> così da cominciare a lavorare senza riscrivere quello che ho già scritto una volta.

**Contesto.** È l'ingresso della catena del valore e la prima metà della ragione per cui FlowGrove sta nella
suite: il preventivo accettato contiene già tutto ciò che serve a far partire una commessa — cliente, importo,
voci, a volte le giornate previste. Riscriverlo a mano è il genere di lavoro doppio che fa dire «tanto vale il
foglio di calcolo». Vale la regola di piattaforma: un'app non chiama un'altra app, si ascolta un **evento**
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §2). L'evento «preventivo accettato» esiste già nel
disegno di 06 QuoteGrove (sua storia 0025).

## 2. Requisiti funzionali

1. **RF-1** — All'arrivo dell'evento «preventivo accettato» l'app crea una **proposta di progetto** in stato
   `bozza`, non un progetto attivo: chi lavora deve poterla guardare prima.
2. **RF-2** — La proposta eredita: cliente, referente, titolo, importo offerto come **budget in importo**, e le
   voci dell'offerta come attività di primo livello con la relativa stima in ore quando l'offerta la contiene.
3. **RF-3** — L'utente rivede la proposta, la modifica e la attiva; oppure la scarta, e lo scarto resta tracciato.
4. **RF-4** — Il progetto conserva il **riferimento al preventivo d'origine**, visibile nella scheda, così da
   poter risalire a cosa era stato promesso.
5. **RF-5** — L'arrivo dello stesso evento due volte non produce due progetti: l'elaborazione è idempotente sul
   riferimento del preventivo.
6. **RF-6** — Se l'app dei preventivi non è attiva sull'account non succede nulla di anomalo: i progetti si creano
   a mano come nella storia 0006.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'evento porta il `tenant_id`: il progetto nasce **solo** in
  quell'account, e l'elaborazione verifica che il riferimento al cliente appartenga allo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova in ingresso: la sorgente è un consumatore di
  eventi. Rotte `GET /api/progetti/v1/project-proposals` e
  `POST /api/progetti/v1/project-proposals/{id}/accept|discard`; errori in `application/problem+json`; OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V15__proposte.sql`: `project` riceve `source` e `source_ref`
  (riferimento logico al preventivo, **senza** chiave esterna verso un altro schema); tabella di idempotenza degli
  eventi consumati con `tenant_id` e identificativo dell'evento.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro «proposte in arrivo» nella sezione *Progetti*, con l'anteprima di
  cosa verrebbe creato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, messaggi e testo dell'anteprima in `en, it, fr, es, de`; i titoli
  delle voci provengono dall'offerta del cliente e non si traducono.
- **RT-6 — Varchi e quota (§6, §7).** La creazione del progetto non consuma quota. Se l'account è in stato
  `canceled` l'evento **non si scarta**: si conserva e si elabora quando l'abbonamento torna valido, altrimenti si
  perde lavoro del cliente.
- **RT-7 — Esposizione conversazionale (§12).** Le proposte in attesa compaiono in `list_projects` (**lettura**)
  con il loro stato; l'accettazione non è esposta come strumento in questa stesura, perché è il momento in cui si
  guarda cosa è stato promesso.
- **RT-8 — Dati personali (§10).** L'evento porta nome e recapito del referente: sono dati personali già
  dichiarati per `project` (storia 0006); va aggiunta la **provenienza** nel manifesto, perché cambia l'origine
  del dato.
- **RT-9 — Registrazione eventi (§14).** «Proposta creata da preventivo», «proposta accettata», «proposta
  scartata», «evento duplicato ignorato» con `tenant_id`, `app_id`, `user_id`, riferimento al preventivo; mai il
  nome del cliente.

## 4. Criteri di accettazione

**CA-1 — Nascita della proposta**
- **Dato** un evento «preventivo accettato» con cliente, importo 8.000 € e cinque voci
- **Quando** l'app lo elabora
- **Allora** esiste una proposta di progetto in `bozza` con budget importo 8.000 € e cinque attività

**CA-2 — Attivazione**
- **Dato** una proposta in `bozza`
- **Quando** l'utente la rivede e la attiva
- **Allora** il progetto passa ad `attivo` e conserva il riferimento al preventivo

**CA-3 — Idempotenza**
- **Dato** lo stesso evento consegnato due volte
- **Quando** l'app lo elabora
- **Allora** esiste una sola proposta e il secondo arrivo è registrato come duplicato ignorato

**CA-4 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled`
- **Quando** arriva l'evento
- **Allora** l'evento non si perde: resta in attesa ed è elaborato quando l'abbonamento torna valido

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** arriva un evento per `A`
- **Allora** la proposta nasce solo in `A` e nessun utente di `B` la vede

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sulla trasformazione voci → attività e di **integrazione** sul consumo dell'evento,
      compresa la consegna doppia;
- [ ] prova di **isolamento fra account** sull'elaborazione dell'evento;
- [ ] **prova end-to-end**: rimando — il percorso `[J-PROGETTI]` crea il progetto a mano per non dipendere da
      un'app che non esiste ancora; quando 06 QuoteGrove sarà implementata, il percorso incrociato è di
      piattaforma. Motivo e proprietario registrati;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la provenienza dei dati del referente;
- [ ] **registro delle decisioni** compilato, con annotata la scelta «proposta in bozza, non progetto attivo»;
- [ ] controllo automatico di **accessibilità** verde sul riquadro delle proposte;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | La proposta diventa un progetto |
| Storia `0021` | L'importo offerto diventa il budget |
| 06 QuoteGrove | Deve pubblicare l'evento «preventivo accettato»: il contratto va concordato con quella app |

## 7. Fuori ambito

- il ritorno di informazione verso i preventivi («il progetto è finito»): non previsto;
- la nascita del progetto da un ordine o da una fattura: non previsto, il preventivo è l'unica origine;
- la modifica del preventivo dopo l'accettazione: è di 06 QuoteGrove.

## 8. Punti aperti

- **Il contratto dell'evento «preventivo accettato»** — quali campi porta, se contiene le stime in ore, come
  identifica il cliente — non è deciso qui. Va concordato con 06 QuoteGrove: se quell'app non porta le stime in
  ore, il budget nasce solo in importo e va detto all'utente invece di inventarlo.
