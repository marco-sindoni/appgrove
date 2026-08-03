# 0020 — Agenda del giorno

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 04 — Attività e storico della relazione
**Storia**: `0020` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore che apre l'app alle otto del mattino
> voglio una sola schermata che mi dica chi devo sentire oggi e cosa ho lasciato indietro
> così da cominciare a lavorare invece di decidere da dove cominciare.

**Contesto.** L'analisi in rete dice che la vista che conta per una squadra piccola non è il cruscotto ma
«cosa devo fare oggi» ([application-description.md](../application-description.md) §2.5). È anche la schermata che
rende visibile il costo del non usare l'app: l'arretrato. Va tenuta piccola di proposito — tre gruppi, nessun
grafico.

## 2. Requisiti funzionali

1. **RF-1** — L'agenda mostra tre gruppi in quest'ordine: **in ritardo** (scadute e non completate), **oggi**,
   **prossimi sette giorni**.
2. **RF-2** — Ogni riga dice tipo, titolo, a chi si riferisce e da quanto è in ritardo; da lì si completa
   l'attività senza cambiare schermata.
3. **RF-3** — L'agenda è filtrata sul proprio portafoglio, con la possibilità di vedere quella di tutta la squadra
   se si ha ruolo `owner` o `admin`.
4. **RF-4** — Quando non c'è nulla, lo stato vuoto lo dice in modo positivo e propone l'azione utile (per esempio
   «programma il prossimo richiamo su una trattativa ferma»), invece di mostrare una pagina bianca.
5. **RF-5** — L'agenda è la schermata di atterraggio del modulo quando l'account ha almeno un'attività aperta;
   altrimenti atterra sulla panoramica.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'agenda legge solo attività dell'account del token verificato, filtrate
  per responsabile quando la vista è personale.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/sales/v1/activities/agenda` con parametri di
  responsabile e finestra temporale; errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; usa l'indice della storia 0019.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Attività → Agenda; azione di completamento in linea; solo token del
  sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Titoli dei gruppi, espressioni di ritardo («in ritardo di 3 giorni») e stato
  vuoto in `en, it, fr, es, de`, con le forme di plurale corrette.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. La vista di squadra richiede ruolo `owner` o
  `admin`; un `member` che la richiede riceve `403`.
- **RT-7 — Esposizione conversazionale (§12).** È la schermata che corrisponde a `list_activities` (storia 0034):
  i parametri devono essere gli stessi, così che «cosa devo fare oggi» in chat e in interfaccia diano la stessa
  risposta.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo; l'agenda mostra dati già dichiarati.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo oltre al completamento, già previsto dalla storia
  0019.

## 4. Criteri di accettazione

**CA-1 — Tre gruppi**
- **Dato** un venditore con un'attività scaduta ieri, una per oggi e una fra tre giorni
- **Quando** apre l'agenda
- **Allora** le vede nei tre gruppi, con quella in ritardo per prima

**CA-2 — Completamento in linea**
- **Dato** l'agenda aperta
- **Quando** il venditore completa un'attività registrando l'esito
- **Allora** la riga sparisce dal gruppo e i conteggi si aggiornano senza cambiare schermata

**CA-3 — Vista di squadra negata a un membro**
- **Dato** un utente con ruolo `member`
- **Quando** richiede l'agenda di tutta la squadra
- **Allora** riceve `403`

**CA-4 — Nessuna attività**
- **Dato** un venditore senza attività aperte
- **Quando** apre l'agenda
- **Allora** vede uno stato vuoto con un'azione utile, non una pagina bianca

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con attività in scadenza oggi
- **Quando** un utente di `A` apre l'agenda
- **Allora** vede solo le proprie

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul raggruppamento per finestra temporale, fusi orari compresi, e di **integrazione**
      sulla rotta;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla vista di squadra;
- [ ] **prova end-to-end**: coprire ora — l'agenda è il passo in cui il percorso `[J-SALES]` verifica che
      l'attività creata sia comparsa; voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con plurali e espressioni di ritardo corretti;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta della schermata di atterraggio;
- [ ] contratto degli **strumenti conversazionali**: parametri allineati a `list_activities`;
- [ ] controllo automatico di **accessibilità** verde sull'agenda;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0019` | Servono le attività da mostrare |

## 7. Fuori ambito

- la vista a calendario mensile: non prevista, l'agenda è una lista;
- i promemoria in arrivo: punto aperto della storia 0019;
- l'esportazione in formato calendario: storia 0024.

## 8. Punti aperti

- **Fuso orario di riferimento.** «Oggi» dipende dal fuso: la proposta è usare quello dell'account, non quello del
  dispositivo, così che una squadra veda la stessa agenda. Va confermato, perché un venditore all'estero vedrebbe
  «oggi» diverso dal proprio.
