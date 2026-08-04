# 0015 — Storico dei passaggi di fase

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 03 — Pipeline e trattative
**Storia**: `0015` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile commerciale
> voglio sapere quando ogni trattativa è passata da una fase all'altra e per mano di chi
> così da poter dire quanto tempo ci mettiamo davvero a chiudere, invece di andare a sensazione.

**Contesto.** È la storia meno visibile dell'epica e quella su cui si regge tutta l'epica 06: senza una riga per
ogni movimento, il «tempo medio di chiusura» si può solo stimare dalla data di creazione, e i colli di bottiglia
fra fasi sono invisibili. Va fatta **insieme** al movimento, non dopo: uno storico che comincia a metà è peggio di
nessuno storico, perché sembra completo.

## 2. Requisiti funzionali

1. **RF-1** — Ogni cambio di fase di una trattativa scrive una riga con fase di partenza, fase di arrivo, momento
   e autore.
2. **RF-2** — Anche la **creazione** scrive una riga (da «nessuna fase» alla fase iniziale) e anche la
   **riapertura** ne scrive una: nessuna transizione resta muta.
3. **RF-3** — Lo storico è **immutabile**: non si modifica e non si cancella se non con l'esercizio dei diritti
   dell'interessato.
4. **RF-4** — La scheda della trattativa mostra lo storico in ordine cronologico, con il tempo trascorso in ogni
   fase.
5. **RF-5** — Un movimento che fallisce non lascia righe: cambio di fase e scrittura dello storico stanno nella
   stessa transazione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le righe dello storico portano `tenant_id` e si leggono solo dentro
  l'account del token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta di sola lettura
  `GET /api/sales/v1/deals/{id}/stage-events`; nessuna rotta di scrittura diretta: lo storico lo scrive il
  servizio, non il client. Errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `deal_stage_event` già creata dalla storia 0002; indice su
  `(tenant_id, deal_id, created_at)`. La tabella porta comunque le colonne di controllo e la cancellazione logica,
  che qui si usa **solo** per i diritti dell'interessato.
- **RT-4 — Modulo frontend (§3, §5).** Blocco «Storico» nella scheda della trattativa, con la durata per fase;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le durate («3 giorni», «2 ore») si esprimono con le forme corrette in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota.
- **RT-7 — Esposizione conversazionale (§12).** Lo storico entra in `summarize_account` (storia 0036) come
  «da quanto è ferma questa trattativa»; nessuno strumento di scrittura.
- **RT-8 — Dati personali (§10).** La tabella contiene identificativi, non nomi — ma rimanda a una trattativa che
  rimanda a una persona: per questo resta in `exportData` e `purgeData` (è una delle tre tabelle che si tende a
  dimenticare, §6 della descrizione dell'applicazione).
- **RT-9 — Registrazione eventi (§14).** Il registro applicativo non duplica lo storico: si limita alla riga
  «fase cambiata» già prevista dalla storia 0014.

## 4. Criteri di accettazione

**CA-1 — Ogni movimento lascia traccia**
- **Dato** una trattativa creata e poi spostata due volte
- **Quando** si apre lo storico
- **Allora** contiene tre righe: la creazione e i due movimenti, con momenti e autori

**CA-2 — Immutabilità**
- **Dato** una riga di storico esistente
- **Quando** si tenta di modificarla o cancellarla attraverso le interfacce pubbliche
- **Allora** l'operazione non esiste: non c'è rotta che lo consenta

**CA-3 — Transazione unica**
- **Dato** un cambio di fase che fallisce dopo la scrittura della trattativa (guasto simulato)
- **Quando** la transazione viene annullata
- **Allora** né la fase né la riga di storico risultano cambiate

**CA-4 — Tempo per fase**
- **Dato** una trattativa rimasta 4 giorni in «Qualificato» e 2 in «Proposta inviata»
- **Quando** si apre la scheda
- **Allora** lo storico mostra le due durate

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede lo storico di una trattativa di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo delle durate e di **integrazione** sulla transazione unica;
- [ ] prova di **isolamento fra account** sullo storico;
- [ ] **prova end-to-end**: nessun impatto aggiuntivo — il percorso `[J-SALES]` verifica lo storico come effetto
      del passo di spostamento (storia 0014);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con le forme corrette delle durate;
- [ ] **manifesto dei dati** verificato: `deal_stage_event` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotata l'immutabilità dello storico;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, rimando a `summarize_account`;
- [ ] controllo automatico di **accessibilità** verde sul blocco dello storico;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0013` | Servono le trattative |
| Storia `0014` | È il movimento che genera le righe; le due vanno rilasciate insieme |

## 7. Fuori ambito

- i rapporti che usano lo storico: epica 06;
- la cronologia unificata di attività, note e movimenti: storia 0022;
- l'avviso sulle trattative ferme: storia 0023.

## 8. Punti aperti

- Nessuno.
