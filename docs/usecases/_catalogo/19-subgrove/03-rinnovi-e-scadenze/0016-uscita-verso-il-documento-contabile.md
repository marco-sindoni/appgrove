# 0016 — Uscita verso il documento contabile

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 03 — Rinnovi e scadenze
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve fatturare centoventi abbonamenti il primo del mese
> voglio che le scadenze diventino fatture senza che nessuno le ribatta a mano
> così da smettere di passare il primo pomeriggio di ogni mese a copiare righe da un foglio all'altro.

**Contesto.** È il pezzo che collega SubGrove alla **catena del documento contabile** del §6 del catalogo
(preventivo → ordine → fattura → incasso) ed è l'argomento di vendita più forte della suite: la scadenza
ricorrente è precisamente ciò che fa nascere la fattura. Va detto subito, però, cosa SubGrove **non** fa: non
emette documenti fiscali. La scadenza è un avviso di addebito interno; il documento lo emette **02 BillGrove**,
e dove serve la trasmissione a norma se ne occupa il suo strato di conformità. Qui si costruisce l'uscita, in due
forme: **a evento** verso BillGrove quando c'è, e in **un file** quando non c'è — perché un'app che funziona solo
dentro la suite non è vendibile da sola.

**Vincolo architetturale da non aggirare**: un'app **non chiama** un'altra app. L'unica via è asincrona a eventi.

## 2. Requisiti funzionali

1. **RF-1** — Una scadenza può essere marcata come **da fatturare**; l'app pubblica un evento «scadenza pronta
   per la fatturazione» con tutto ciò che serve a comporre il documento: intestatario, descrizione, periodo,
   importo, aliquota.
2. **RF-2** — L'evento è **idempotente per scadenza**: pubblicarlo due volte non produce due documenti, e la
   chiave è l'identificativo della scadenza.
3. **RF-3** — La scadenza registra l'esito: se un documento è stato emesso, ne conserva il **riferimento** e la
   scheda lo mostra; se l'emissione è fallita, mostra il motivo e permette di riprovare.
4. **RF-4** — Se BillGrove non è attiva sull'account, la stessa selezione di scadenze si **esporta in un file
   tabellare** con le stesse colonne, e la scadenza risulta «esportata il giorno X».
5. **RF-5** — L'operazione si può fare in blocco su una selezione filtrata (per periodo, per stato, per piano),
   con un riepilogo di quante righe e quale totale **prima** della conferma.
6. **RF-6** — Una scadenza già fatturata **non si rifattura**: l'azione è disabilitata e la scheda spiega perché.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Selezione, evento ed esportazione portano il `tenant_id` del token
  verificato; nessun evento può riferirsi a un altro account.
- **RT-2 — Comunicazione fra app (§2).** **Nessuna chiamata diretta a BillGrove**: solo pubblicazione di evento.
  Se BillGrove non c'è, l'evento resta senza consumatori e non è un errore.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `POST /api/abbonati/v1/scadenze/da-fatturare` (in blocco,
  con anteprima) e `GET /api/abbonati/v1/scadenze/esportazione`; errori in `problem+json`; OpenAPI aggiornata.
- **RT-4 — Persistenza (§8).** Migrazione `V11__scadenza_fatturazione.sql`: sulla tabella `scadenza`, lo stato di
  fatturazione, il riferimento al documento e la data di esportazione.
- **RT-5 — Modulo frontend (§3, §5).** Nella sezione *Scadenze*, selezione multipla con riepilogo e conferma;
  colonna che dice se e come la scadenza è uscita; solo token del sistema di design.
- **RT-6 — Cinque lingue (§4).** Etichette, riepilogo, motivi di fallimento in `en, it, fr, es, de`. Il file
  esportato porta intestazioni nella lingua dell'utente.
- **RT-7 — Varchi e quota (§6).** Nessun consumo di quota. Con abbonamento di piattaforma non attivo, `402`.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento: mandare in fatturazione centoventi righe da
  una chat è esattamente il genere di azione che non deve essere comoda.
- **RT-9 — Dati personali (§10).** L'evento e il file contengono l'intestatario, cioè dati riferiti a una
  persona: il campo va dichiarato, e va detto nel manifesto che il dato **esce dall'app** verso un'altra app
  dello stesso titolare. Il file esportato resta a carico del cliente una volta scaricato.
- **RT-10 — Registrazione eventi (§14).** `scadenze mandate in fatturazione (quante, totale)`, `esportazione
  eseguita`, `emissione fallita (causa)`, con `tenant_id`, `app_id`, `user_id` e correlazione, senza nomi.

## 4. Criteri di accettazione

**CA-1 — Uscita a evento**
- **Dato** cinque scadenze in attesa e BillGrove attiva
- **Quando** l'utente le manda in fatturazione e conferma il riepilogo «5 righe, 195 €»
- **Allora** vengono pubblicati cinque eventi, e quando i documenti sono emessi le scadenze mostrano il
  riferimento

**CA-2 — Idempotenza**
- **Dato** le stesse cinque scadenze già mandate · **Quando** l'operazione si ripete
- **Allora** non nascono documenti nuovi e l'utente riceve un messaggio che dice quante erano già uscite

**CA-3 — Senza BillGrove**
- **Dato** un account senza BillGrove · **Quando** l'utente esporta la stessa selezione
- **Allora** ottiene un file tabellare con le stesse colonne e le scadenze risultano esportate

**CA-4 — Fallimento visibile**
- **Dato** un'emissione fallita a valle · **Quando** l'utente apre la scadenza
- **Allora** vede il motivo e può riprovare, senza che la scadenza risulti fatturata

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** uno esporta · **Allora** il file contiene solo le proprie scadenze

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sull'idempotenza e di **integrazione** sulla pubblicazione dell'evento e
      sull'esportazione;
- [ ] prova di **isolamento fra account** su evento ed esportazione;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-ABBONATI]` verifica l'esportazione in file (la via
      che funziona senza altre app); registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: l'intestatario **esce** dall'app, e va detto;
- [ ] **registro delle decisioni** compilato: nessuna chiamata diretta fra app, doppia uscita (evento e file),
      e il confine «SubGrove non emette documenti fiscali»;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | servono scadenze da mandare fuori |
| **02 BillGrove** (app del catalogo, non implementata) | il consumatore dell'evento; finché non c'è, funziona la via del file |

## 7. Fuori ambito

- l'emissione del documento fiscale e la sua trasmissione: sono di BillGrove e del suo strato di conformità;
- il recupero del credito da fattura non pagata: è di **03 CashGrove**, e il confine è nella storia `0021`;
- la contabilizzazione dei ricavi per competenza: è materia del commercialista, non di questa app.

## 8. Punti aperti

**Chi decide il momento della fatturazione.** La proposta lascia l'azione all'utente, in blocco, perché il primo
del mese il titolare vuole guardare prima di mandare. L'alternativa — fatturazione automatica alla maturazione
della scadenza — è più comoda e più rischiosa (un errore si moltiplica per centoventi). **Proposta**: manuale nel
primo giro, con automatismo opzionale dopo che il cliente si è fidato. Chiude: lo sviluppatore, con la direzione
di prodotto.
