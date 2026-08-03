# 0028 — Strumenti di lettura

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 07 — Esposizione conversazionale e prove
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0016`, `0020`, `0025`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come parrucchiera che ha le mani nei capelli di una cliente
> voglio poter chiedere a voce «che formula avevamo fatto ad Anna a giugno?» e ottenere la risposta giusta
> così da non asciugarmi le mani, aprire un programma e cercare, mentre la cliente aspetta.

**Contesto.** Il catalogo pone a tutte le sessanta applicazioni il requisito di essere comandabili da una chat. Il
livello conversazionale della piattaforma **non esiste ancora** (epica `12-ready-for-ai-mcp`, casi d'uso 0061-0066,
scritti e non implementati): il dovere di questa storia non è costruire il server, ma **dichiarare il contratto
degli strumenti di lettura** e tenerlo versionato dentro il servizio dell'app
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §12).

Per SalonGrove è il punto in cui l'app diventa più utile delle sue concorrenti, e non per moda: il momento in cui
serve la formula è esattamente il momento in cui non si possono usare le mani (§1 e §7 della
[descrizione](../application-description.md)). Gli strumenti di lettura non cambiano nulla; i rischi da presidiare
sono altri due: la **sovraesposizione dei dati dei clienti** — fotografie, note libere — e la **sovraesposizione
dei dati di chi lavora nel salone**, cioè quanto ha prodotto e quanto gli spetta.

## 2. Requisiti funzionali

1. **RF-1** — Sono dichiarati i **sei** strumenti di lettura, ciascuno con nome stabile, descrizione in lingua
   naturale, schema dei parametri, schema del risultato, marcatura *lettura* e dichiarazione di idempotenza:
   `storico_servizi(cliente, periodo?)`, `scheda_tecnica_cliente(cliente, ultime_n?)`,
   `giacenza_prodotti(deposito?, sotto_soglia?)`, `stato_pacchetti(cliente?)`,
   `provvigioni_periodo(periodo, operatore?)`, `clienti_da_richiamare(giorni_di_assenza, servizio?)`.
2. **RF-2** — I risultati sono **minimizzati**. In particolare `scheda_tecnica_cliente` restituisce base, tono,
   ossidante e volume, minuti di posa, prodotti usati e data: **mai le fotografie** (storia `0013`) e **mai il
   campo di nota libera** (storia `0012`), che è il punto in cui potrebbe essere finita per inerzia
   un'informazione sulla salute.
3. **RF-3** — `provvigioni_periodo` restituisce base e importo **solo** a chi amministra l'account e, per la
   propria, all'interessato; **non ordina e non confronta** operatori, e senza il parametro `operatore` risponde
   con il totale del salone, non con l'elenco delle persone. È il confine della storia `0026`, portato dentro il
   contratto.
4. **RF-4** — **Nessuno strumento** espone le regole di provvigione (storia `0024`), le fotografie, le note libere,
   i recapiti dei clienti in elenco, l'esportazione o la cancellazione dei dati personali. Sono divieti dichiarati
   nel contratto, non omissioni.
5. **RF-5** — Gli strumenti riusano le rotte e i calcoli già esistenti: nessuna seconda implementazione della
   giacenza o del maturato, o la chat e la schermata si metteranno a dire numeri diversi.
6. **RF-6** — Il contratto è documentato e versionato dentro il servizio: cambiare lo schema di uno strumento senza
   cambiare la versione dichiarata fa fallire una prova.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Sei strumenti dichiarati con firma, descrizione, schemi e marcatura
  **lettura**; contratto dentro il servizio dell'app, versionato con esso. Il server conversazionale è di
  piattaforma e non ancora implementato — **dipendenza dichiarata: casi d'uso 0061-0063**.
- **RT-2 — Isolamento fra account (§1).** Ogni strumento riceve il contesto dell'account dal livello di piattaforma
  e filtra per `tenant_id` preso dal token verificato: **mai** un parametro dell'account nello schema dello
  strumento. Un modello linguistico che inventasse l'identificativo di un cliente altrui non deve ottenere nulla.
- **RT-3 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte `/api/<app>/v1/*` già
  esistenti e ai loro servizi applicativi; errori in `application/problem+json`; nessuna rotta nuova.
- **RT-4 — Varchi e quota (§6, §7).** Le invocazioni attraversano gli stessi cinque varchi delle rotte
  (`401`/`403`/`402`/`403`/`429`). La lettura **non** consuma la metrica `postazioni`, che è a giacenza: leggere non
  apre una postazione. Con abbonamento `canceled` gli strumenti rispondono `402`.
- **RT-5 — Dati personali (§10).** Gli strumenti espongono dati di **clienti** e di **chi lavora nel salone**: la
  minimizzazione è un requisito, non un'ottimizzazione. Il manifesto dichiara in italiano e inglese che quelle voci
  sono esposte **anche per questa via**, e che fotografie, note libere e regole di provvigione **non** lo sono.
- **RT-6 — Perimetro senza dati sanitari.** Il divieto sulla nota libera non è una scelta di minimizzazione fra le
  altre: è il presidio che impedisce a un'informazione sulla salute, finita lì per inerzia professionale, di uscire
  da una chat (storie `0012` e `0013`, §6 della descrizione).
- **RT-7 — Registrazione eventi (§14).** Ogni invocazione è registrata con `tenant_id`, `app_id`, `user_id`,
  correlazione e nome dello strumento; **mai** i parametri che possono contenere il nome di una persona.
- **RT-8 — Prove (§11).** Prova di contratto sugli schemi; prova di isolamento fra due account su tutti e sei gli
  strumenti; prova **negativa** che fotografie, note libere e regole di provvigione non compaiano in nessun
  risultato; prova che `provvigioni_periodo` applichi il filtro di ruolo.

## 4. Criteri di accettazione

**CA-1 — La domanda che vale l'app**
- **Dato** una cliente con tre schede tecniche, l'ultima di giugno
- **Quando** si invoca `scheda_tecnica_cliente(cliente, ultime_n: 1)`
- **Allora** si ottengono base, tono, ossidante e volume, minuti di posa, prodotti e data, con gli stessi valori
  della schermata (storia `0010`)

**CA-2 — Quello che non esce mai**
- **Dato** la stessa cliente, con fotografie e una nota tecnica scritta a mano
- **Quando** si invoca lo stesso strumento
- **Allora** il risultato non contiene né immagini né il testo della nota, in nessuna forma

**CA-3 — Le provvigioni non diventano una classifica**
- **Dato** un salone con tre operatori e un periodo chiuso
- **Quando** si invoca `provvigioni_periodo(periodo)` senza indicare l'operatore
- **Allora** si ottiene il totale del salone, non l'elenco delle persone, e non esiste parametro che produca un
  ordinamento

**CA-4 — Il ruolo conta**
- **Dato** un operatore senza ruolo amministrativo
- **Quando** invoca `provvigioni_periodo(periodo, operatore: <un collega>)`
- **Allora** la risposta non contiene alcun importo e spiega che può chiedere solo il proprio

**CA-5 — Isolamento fra account**
- **Dato** una cliente dell'account `A`
- **Quando** uno strumento invocato nel contesto di `B` ne chiede lo storico con l'identificativo di `A`
- **Allora** ottiene la risposta che otterrebbe per una cliente inesistente, e l'evento registra il tentativo

**CA-6 — Contratto stabile**
- **Dato** una modifica allo schema di uno strumento senza cambio di versione
- **Quando** si eseguono le prove
- **Allora** la prova di contratto fallisce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (area `backend`);
- [ ] prove di **unità** sugli schemi e di **integrazione** sulle invocazioni, con database effimero e migrazioni
      vere;
- [ ] prova di **isolamento fra account** su tutti e sei gli strumenti;
- [ ] prova **negativa** su fotografie, note libere e regole di provvigione;
- [ ] **prova end-to-end**: *nessun impatto* sulla superficie utente — il livello conversazionale non esiste ancora
      e non c'è nulla da percorrere; risposta scritta nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e in `decisions.json`;
- [ ] **traduzioni**: non applicabile — le descrizioni degli strumenti sono in lingua naturale per il modello, non
      testo di interfaccia; nessuna stringa nuova nel modulo frontend;
- [ ] **manifesto dei dati** aggiornato con la nota sull'esposizione conversazionale e con i divieti dichiarati;
- [ ] **registro delle decisioni**: elenco degli strumenti, campi restituiti, criterio di minimizzazione, regola di
      ruolo sulle provvigioni, divieti;
- [ ] avvio locale invariato (`./dev.sh services`).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | `scheda_tecnica_cliente` legge la scheda già calcolata, non una seconda versione |
| storia `0016` | `giacenza_prodotti` legge la giacenza come somma dei movimenti |
| storia `0020` | `stato_pacchetti` legge sedute residue e scadenza |
| storia `0025` | `provvigioni_periodo` legge il prospetto e ne eredita il filtro di ruolo |
| storia `0027` | `clienti_da_richiamare` legge l'elenco derivato, con le sue esclusioni |
| casi d'uso di piattaforma 0061-0063 (non implementati) | server conversazionale, autenticazione delegata e mappatura operazioni → strumenti; nel frattempo il contratto resta dichiarato e provato dentro il servizio |

## 7. Fuori ambito

- gli strumenti che **scrivono** — apertura e chiusura del conto, rettifica di giacenza, chiusura del prospetto:
  storia `0029`;
- la costruzione del server conversazionale e il consenso delegato: sono di piattaforma;
- gli strumenti dell'agenda (`crea_prenotazione`, `verifica_disponibilita`): sono di BookGrove e non si riscrivono
  (§7 della descrizione).

## 8. Punti aperti

**Quanto storico far restituire per volta.** Tre schede sono quasi sempre la risposta giusta; dieci rendono la
risposta lunga e un modello che riassume male è peggio di uno che non risponde. La proposta è tre come valore
predefinito e un massimo dichiarato nello schema, da verificare su un risultato vero e da registrare in
`decisions.json`.
