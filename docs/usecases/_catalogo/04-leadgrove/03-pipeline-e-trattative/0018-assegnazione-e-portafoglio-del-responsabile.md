# 0018 — Assegnazione e portafoglio del responsabile

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 03 — Pipeline e trattative
**Storia**: `0018` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore in una squadra di quattro
> voglio vedere subito solo le trattative di cui rispondo io
> così da non perdere tempo a filtrare quelle degli altri ogni volta che apro l'app.

**Contesto.** È la storia che giustifica il modello utente `multi`
([application-description.md](../application-description.md) §3): in una squadra, «di chi è questa trattativa» è
la domanda che decide chi chiama e chi no. È anche la premessa del rendimento per responsabile (storia 0031). La
posizione proposta è deliberatamente semplice: tutti vedono tutto, ma la vista predefinita è «le mie». La
segmentazione della **visibilità** per ruolo non è prevista, e il motivo è ai punti aperti.

## 2. Requisiti funzionali

1. **RF-1** — Ogni trattativa ha un responsabile, riassegnabile a un altro membro che occupa un posto.
2. **RF-2** — La lavagna e l'elenco delle trattative hanno un filtro per responsabile, con «le mie» come valore
   predefinito quando l'account ha più di un posto occupato.
3. **RF-3** — Riassegnare una trattativa lascia traccia nella cronologia della trattativa (chi, quando, da chi a
   chi).
4. **RF-4** — Riassegnare **non** sposta le attività già programmate: queste hanno un proprio responsabile e si
   riassegnano a parte, perché sono impegni presi da una persona.
5. **RF-5** — Quando un posto viene revocato, le trattative del membro restano e vengono segnalate come «senza
   responsabile attivo», con un'azione per riassegnarle in blocco.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il nuovo responsabile deve essere un membro dell'account del token
  verificato con un posto attivo; altrimenti la richiesta è rifiutata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `PATCH /api/sales/v1/deals/{id}/owner` e parametro
  `owner` sugli elenchi; errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; indice su `(tenant_id, owner_user_id, stage_id)` per rendere
  veloce la vista predefinita.
- **RT-4 — Modulo frontend (§3, §5).** Selettore del responsabile nella scheda e filtro nella barra degli
  strumenti; il filtro ricorda l'ultima scelta; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette e messaggi in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Riassegnare a un membro **senza posto** è rifiutato: sarebbe un modo per
  aggirare la metrica `seats`. Il rifiuto è `422` con la spiegazione, non `429`, perché non si sta consumando
  quota ma usando un riferimento non valido.
- **RT-7 — Esposizione conversazionale (§12).** Il filtro per responsabile è un parametro di `list_deals` e
  `list_activities` (storia 0034). La riassegnazione **non** è esposta alla chat: cambia di chi è la responsabilità
  di un affare e va fatta da una persona.
- **RT-8 — Dati personali (§10).** Il responsabile è un identificativo di membro, già coperto: nessuna voce nuova.
- **RT-9 — Registrazione eventi (§14).** «Trattativa riassegnata» con identificativo della trattativa, del vecchio
  e del nuovo responsabile, autore e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Vista predefinita**
- **Dato** un account con tre posti occupati e trattative di tutti e tre
- **Quando** un venditore apre la lavagna
- **Allora** vede per prime solo le proprie, e può togliere il filtro con un clic

**CA-2 — Riassegnazione**
- **Dato** una trattativa di cui è responsabile il venditore `X`
- **Quando** il responsabile commerciale la riassegna a `Y`
- **Allora** compare nel portafoglio di `Y`, esce da quello di `X` e la cronologia registra il passaggio

**CA-3 — Membro senza posto**
- **Dato** un membro dell'account che non occupa un posto
- **Quando** si tenta di assegnargli una trattativa
- **Allora** riceve `422` con la spiegazione, e la trattativa resta al responsabile precedente

**CA-4 — Posto revocato**
- **Dato** un venditore con 5 trattative a cui viene revocato il posto
- **Quando** il responsabile commerciale apre l'elenco
- **Allora** le 5 trattative sono segnalate come «senza responsabile attivo» con l'azione di riassegnazione in
  blocco

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` tenta di assegnare una trattativa a un membro di `B`
- **Allora** la richiesta è rifiutata e nulla cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla verifica del posto attivo e di **integrazione** sulla riassegnazione;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli**;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo, che usa un solo venditore; coperta da prove
      d'integrazione, con il motivo annotato nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta «tutti vedono tutto, la vista predefinita è
      le mie»;
- [ ] contratto degli **strumenti conversazionali**: filtro per responsabile in lettura, riassegnazione non
      esposta;
- [ ] controllo automatico di **accessibilità** verde sul selettore;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0013` | Il responsabile sta sulla trattativa |
| Storia `0014` | Il filtro vive sulla lavagna |
| Storia `0004` | Il posto attivo è la condizione per essere responsabile |

## 7. Fuori ambito

- la **visibilità segmentata** (un venditore che non può vedere le trattative degli altri): vedi punti aperti;
- l'assegnazione automatica dei lead in arrivo a rotazione: non prevista in questa proposta;
- il rendimento per responsabile: storia 0031.

## 8. Punti aperti

- **Visibilità segmentata per responsabile.** In una micro-impresa «tutti vedono tutto» è normale e desiderato; in
  una piccola impresa con venditori su territori diversi può non esserlo, e alcuni concorrenti la offrono nei piani
  alti. Introdurla significherebbe però aggiungere un livello di autorizzazione dentro l'app, oltre alla matrice
  dei ruoli di piattaforma. È una **decisione di prodotto dello sviluppatore**, da prendere prima di vendere il
  piano `business`.
