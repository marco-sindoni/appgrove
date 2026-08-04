# 0011 — Numerazione di sequenza e buchi

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 02 — Sorgenti e ingresso delle azioni
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che deve fidarsi del proprio registro
> voglio sapere non solo cosa c'è scritto, ma anche **cosa manca**
> così da non scambiare un registro incompleto per un registro tranquillo.

**Contesto.** AuditGrove è un registro **cooperativo**: gli agenti dichiarano ciò che fanno, non veniamo a saperlo
intercettandoli (§0 della [descrizione dell'applicazione](../application-description.md)). Ne discende un limite
che non va nascosto: **AuditGrove prova ciò che è stato dichiarato, non ciò che non è mai stato dichiarato**. Un
prodotto onesto non può fingere che il limite non esista; può però fare una cosa molto utile, cioè **accorgersi
delle mancanze e dichiararle**. Se ogni sorgente numera le proprie dichiarazioni in modo crescente, un numero che
non arriva è un buco visibile. Un registro che dice «qui mancano diciassette dichiarazioni della sorgente
`agente di fatturazione`, fra le 14:02 e le 14:40» è enormemente più affidabile di uno che tace.

## 2. Requisiti funzionali

1. **RF-1** — Ogni sorgente numera le proprie dichiarazioni con un **numero di sequenza strettamente crescente**,
   che dichiara a ogni chiamata.
2. **RF-2** — Il servizio rileva i numeri **mancanti** per ciascuna sorgente e li dichiara come **buchi**, con
   intervallo dei numeri mancanti e finestra temporale in cui sarebbero dovuti arrivare.
3. **RF-3** — Un buco è **esso stesso una riga della catena**: non è un calcolo fatto al volo su una schermata, è
   un fatto registrato e incatenato come gli altri.
4. **RF-4** — Un numero che arriva **in ritardo** (dopo che il buco è stato dichiarato) viene accettato e chiude
   il buco con una riga nuova che lo dichiara chiuso; la riga che dichiarava il buco **non si modifica** — resta,
   perché il registro non si riscrive.
5. **RF-5** — Un numero **ripetuto** o **minore** dell'ultimo ricevuto non sovrascrive nulla: viene registrato
   come anomalia di numerazione, perché è un segnale (una sorgente clonata, una chiave usata in due posti).
6. **RF-6** — Il **silenzio prolungato** di una sorgente dichiarata attiva è a sua volta un segnale: superata una
   soglia di inattività, il servizio lo dichiara.
7. **RF-7** — L'interfaccia distingue in modo inequivocabile le **due numerazioni**, che sono due cose diverse: il
   numero di sequenza **della sorgente** (lo assegna l'agente, serve a scoprire cosa manca) e il numero di
   sequenza **della catena dell'account** (lo assegna AuditGrove, serve a dimostrare che nulla è stato riscritto).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Buchi e anomalie sono per account e per sorgente; ogni lettura filtra
  per `tenant_id` preso dal token verificato. Le sorgenti di due account non condividono nessuna numerazione.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/agentaudit/v1/sources/{id}/gaps` per l'elenco dei
  buchi di una sorgente, e il campo del numero di sequenza dichiarato aggiunto alla rotta di ingresso; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V8__sequenze_e_buchi.sql` sullo schema `app_agentaudit`: numero di
  sequenza dichiarato sulla tabella delle azioni, stato di numerazione per sorgente, tabella dei buchi con
  `tenant_id`, chiave primaria UUID versione 7 e colonne di controllo. Le righe di buco entrano nella catena con
  le regole della storia 0002: sola aggiunta, `deleted_at` mai valorizzato.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione Sorgenti, la scheda di ogni sorgente mostra buchi, anomalie e
  silenzio; nella Cronologia i buchi compaiono **in linea con le azioni**, nell'ordine temporale, perché una
  mancanza si capisce solo vedendo dove sarebbe stata. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I testi «dichiarazioni mancanti», «buco chiuso in ritardo», «numerazione
  anomala», «sorgente silente da …» passano dallo spazio-nomi `agentaudit` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Le righe di buco e di anomalia **non consumano** la metrica `actions`: sono
  righe che il servizio scrive per proprio conto, e farle pagare al cliente significherebbe fargli pagare le
  mancanze del proprio agente. La conseguenza va scritta nel listino come funzionalità, non come metrica.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo; i buchi compaiono nei risultati di
  `elenca_azioni` e `riepiloga_attivita` (storia 0034), perché una risposta che li omettesse sarebbe una risposta
  falsa.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: un buco è un intervallo di numeri e una finestra
  di tempo.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «buco rilevato», «buco chiuso», «numerazione anomala»,
  «sorgente silente» sono registrati con `tenant_id`, `app_id`, identificativo della sorgente e identificativo di
  correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Il buco si vede**
- **Dato** una sorgente che ha dichiarato i numeri 1, 2, 3 e poi 7
- **Quando** la dichiarazione numero 7 viene registrata
- **Allora** compare una riga di buco che dichiara mancanti i numeri da 4 a 6, con la finestra temporale, e la
  riga è incatenata come le altre

**CA-2 — Il ritardatario chiude il buco senza riscrivere niente**
- **Dato** un buco dichiarato sui numeri da 4 a 6
- **Quando** arrivano in ritardo le dichiarazioni 4, 5 e 6
- **Allora** vengono registrate, compare una riga che dichiara il buco chiuso, e **la riga che dichiarava il buco
  resta invariata**

**CA-3 — Il numero ripetuto è un segnale, non un errore silenzioso**
- **Dato** una sorgente che ha già dichiarato il numero 10
- **Quando** dichiara di nuovo il numero 10 con un contenuto diverso
- **Allora** nulla viene sovrascritto e compare una riga di anomalia di numerazione

**CA-4 — Il silenzio parla**
- **Dato** una sorgente attiva che ha dichiarato azioni ogni pochi minuti
- **Quando** smette di dichiarare oltre la soglia di inattività
- **Allora** compare la dichiarazione di sorgente silente, con il momento dell'ultimo contatto

**CA-5 — Le due numerazioni non si confondono**
- **Dato** un'azione registrata
- **Quando** si apre la sua scheda
- **Allora** si leggono, distinti e nominati in modo diverso, il numero di sequenza della sorgente e il numero di
  sequenza della catena dell'account

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con sorgenti che usano numeri di sequenza sovrapposti
- **Quando** si guardano i buchi di `A`
- **Allora** non risentono in alcun modo della numerazione di `B`, nemmeno forzando l'identificativo dell'altro
  account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul rilevamento dei buchi, sull'arrivo in ritardo e sulla numerazione anomala, e di
      **integrazione** sulla rotta dei buchi, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle numerazioni e sui buchi;
- [ ] **prova end-to-end**: risposta «rimando» — la comparsa di un buco nella cronologia entra nel percorso
      `[J-AGENTAUDIT]` alla storia 0037, proprietaria della copertura; fino ad allora il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta l'esenzione motivata;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni** compilato, con **due voci obbligatorie**: il buco come riga della catena (e non
      come calcolo di schermata), e la non modificabilità della riga di buco quando il ritardatario arriva;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto; l'obbligo che i buchi compaiano nelle
      risposte degli strumenti di lettura è dichiarato per la storia 0034;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: il limite del registro cooperativo va spiegato al cliente, non solo a noi.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | La numerazione è per sorgente: senza sorgenti registrate non c'è nulla da numerare |
| storia `0008` | Il numero di sequenza arriva con la dichiarazione |
| storia `0002` | Le righe di buco sono righe della catena |

## 7. Fuori ambito

- l'avviso attivo sui buchi (recapito, soglie configurabili): storia 0026, dove stanno tutti gli avvisi su
  comportamenti anomali;
- qualunque tentativo di **ricostruire** ciò che manca: un buco è un buco, e inventare cosa ci sarebbe stato è
  esattamente il contrario di ciò che questo prodotto fa;
- l'intercettazione del traffico, che eliminerebbe i buchi alla radice: è la scelta di prodotto rifiutata al §1
  della descrizione dell'applicazione, con le sue ragioni.

## 8. Punti aperti

- **La soglia del silenzio non può essere una sola per tutti.** Un agente che gira ogni notte è silente di giorno
  per costruzione. Propongo una soglia dichiarata per sorgente al momento della registrazione, con un valore
  predefinito prudente. Da confermare.
- **Cosa fare se una sorgente non dichiara affatto il numero di sequenza.** Un agente scritto in fretta potrebbe
  ometterlo. Rifiutare la dichiarazione sarebbe rigoroso e farebbe perdere righe; accettarla senza numero
  rinuncia alla rilevazione dei buchi per quella sorgente. Propongo di accettarla, marcarla come «non numerata» e
  dichiarare esplicitamente sulla scheda della sorgente che per essa i buchi **non sono rilevabili** — meglio un
  limite dichiarato che una falsa sicurezza. Chi chiude: sviluppatore.
- **Ogni quanto valutare il silenzio e i buchi.** Serve una lavorazione periodica; la sua frequenza incide sulla
  tempestività degli avvisi e sul costo. Da dimensionare in fase di implementazione.
