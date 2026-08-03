# 0004 — Posti, abbonamento e quota

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come amministratore dell'account
> voglio decidere chi della mia squadra occupa un posto in LeadGrove, e vedere quanti me ne restano
> così da non scoprire il limite del mio piano nel momento sbagliato.

**Contesto.** La metrica di quota di LeadGrove è `seats`, di natura **a giacenza**: il tetto vale sui posti
occupati adesso ([application-description.md](../application-description.md) §3). È la scelta che regge tutto il
listino, quindi va implementata prima delle funzioni di dominio: se arrivasse dopo, ogni storia dovrebbe tornare
indietro ad aggiungere il varco. Va implementata anche la conseguenza scomoda: il passaggio a un piano inferiore
è **bloccato** finché i posti occupati superano il tetto di destinazione.

## 2. Requisiti funzionali

1. **RF-1** — Un amministratore dell'account può assegnare un posto a un membro e revocarglielo; solo chi ha un
   posto può usare le funzioni di LeadGrove.
2. **RF-2** — Assegnare un posto oltre il tetto del piano è rifiutato con `429` e un messaggio che dice quanti
   posti sono occupati, qual è il tetto e come si rimedia (liberare un posto o passare di piano).
3. **RF-3** — La sezione Impostazioni mostra i posti occupati, il tetto del piano e chi li occupa.
4. **RF-4** — Un posto revocato torna disponibile **subito**, non alla fine del periodo: è ciò che significa «a
   giacenza».
5. **RF-5** — Il passaggio a un piano con un tetto inferiore al numero di posti occupati è bloccato, con un
   messaggio che dice quanti posti vanno liberati.
6. **RF-6** — Con abbonamento in `past_due` le funzioni restano accessibili; con `canceled` o `paused` rispondono
   `402`. L'esportazione dei dati resta accessibile **in ogni caso**.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I posti si contano e si assegnano solo dentro l'account del token
  verificato; un identificativo di account che arrivasse dal corpo viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/sales/v1/seats`,
  `POST /api/sales/v1/seats`, `DELETE /api/sales/v1/seats/{id}` e `GET /api/sales/v1/quota`; corpo validato;
  errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `seat` già creata dalla storia 0002; qui si aggiunge il vincolo di unicità
  su `(tenant_id, member_id)` fra le righe non cancellate.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Impostazioni del modulo `sales`: elenco dei posti, barra di
  consumo, azione di assegnazione e revoca; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutti i messaggi, compresi quelli di rifiuto per quota, presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Prima di assegnare un posto il servizio prenota una unità della metrica
  `seats` (natura `stock`); a tetto raggiunto risponde `429`. L'abilitazione si legge dalla **proiezione locale**
  alimentata a eventi, mai con una chiamata sincrona sul percorso caldo. La catena completa dei varchi è
  `401 → 403 → 402 → 403 → 429`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura sui posti: assegnare un posto è un
  atto che cambia la fattura del cliente e resta nelle mani di una persona nell'interfaccia. Lo si dichiara qui
  perché sia una scelta, non una dimenticanza.
- **RT-8 — Dati personali (§10).** `seat.member_id` è solo l'identificativo interno del membro, già dichiarato nel
  manifesto dalla storia 0002: nessun dato personale nuovo.
- **RT-9 — Registrazione eventi (§14).** «Posto assegnato», «posto revocato», «assegnazione respinta per quota»
  registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza nomi.

## 4. Criteri di accettazione

**CA-1 — Assegnazione dentro il tetto**
- **Dato** un account sul piano `team` (5 posti) con 3 posti occupati
- **Quando** l'amministratore assegna un posto a un quarto membro
- **Allora** il posto è assegnato e il consumo mostra «4 di 5»

**CA-2 — Quota esaurita**
- **Dato** un account con tutti i posti del piano occupati
- **Quando** l'amministratore prova ad assegnarne un altro
- **Allora** riceve `429`, un messaggio che spiega come rimediare, e **nessun** posto viene creato

**CA-3 — Revoca che libera subito**
- **Dato** un account al tetto
- **Quando** l'amministratore revoca un posto e ne assegna un altro nello stesso minuto
- **Allora** la seconda assegnazione riesce: la giacenza non ha finestre

**CA-4 — Passaggio a piano inferiore bloccato**
- **Dato** un account sul piano `team` con 4 posti occupati
- **Quando** tenta di passare al piano `free` (1 posto)
- **Allora** il passaggio è rifiutato con un messaggio che dice quanti posti liberare

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con i propri posti
- **Quando** un amministratore di `A` chiede l'elenco dei posti forzando l'identificativo di `B`
- **Allora** vede solo i posti di `A`

**CA-6 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled`
- **Quando** un membro apre una qualunque funzione di LeadGrove
- **Allora** riceve `402`, ma l'esportazione dei propri dati resta accessibile

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio della giacenza e di **integrazione** sulle rotte dei posti;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** (solo `owner`/`admin` assegnano posti);
- [ ] **prova end-to-end**: rimando alla storia 0037, che è la proprietaria del percorso `[J-SALES]`; qui si
      coprono i varchi con prove d'integrazione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, messaggi di errore compresi;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotato perché la metrica è a giacenza e non a consumo;
- [ ] contratto degli **strumenti conversazionali**: nessuno, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | Serve la tabella `seat` |
| Storia `0003` | Serve la sezione Impostazioni dove mostrare il consumo |
| Listino `pricing/sales.yaml` approvato dallo sviluppatore | Senza i tetti dei piani non c'è nulla da far rispettare |

## 7. Fuori ambito

- il flusso di acquisto e di disdetta: è della piattaforma (fatturazione), non dell'app;
- gli inviti ai membri dell'account: sono della piattaforma; qui si assegna un posto a chi è già membro;
- la deroga temporanea sui posti concessa dall'assistenza: [estensioni-admin.md](../estensioni-admin.md).

## 8. Punti aperti

- **Prezzi e tetti dei piani** — fermata di escalation dello sviluppatore. La proposta (`free` 1, `team` 5,
  `business` 20) è al §5 della descrizione dell'applicazione e non è una decisione presa.
