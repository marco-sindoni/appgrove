# 0013 — Imposte del documento

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 03 — Preventivi e fatture
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come professionista in regime agevolato che non applica l'imposta sul valore aggiunto
> voglio che il documento calcoli da sé imponibili, imposte, riepilogo per aliquota e imposta di bollo
> così da non dovermi ricordare che sopra 77,47 euro serve il bollo da 2 euro, e da non sbagliare i totali.

**Contesto.** È la parte del documento che nessun utente vuole fare a mano e che nessun foglio di calcolo fa bene.
La ricerca mostra due obblighi precisi che entrano nel calcolo: il riepilogo per aliquota con l'indicazione della
natura per le operazioni non imponibili o esenti, e l'imposta di bollo di 2 euro sui documenti non soggetti a
imposta sul valore aggiunto sopra 77,47 euro, assolta in modo virtuale con addebito trimestrale (§2.3 della
descrizione). Va fatta prima della stampa e della forma canonica, che entrambe li devono riportare.

## 2. Requisiti funzionali

1. **RF-1** — Ogni riga porta la propria aliquota; il documento produce un **riepilogo per aliquota** con
   imponibile e imposta.
2. **RF-2** — Per le righe non soggette, non imponibili o esenti si indica la **natura dell'operazione**, e senza di
   essa il documento non è emettibile.
3. **RF-3** — Il documento calcola l'**imposta di bollo** quando l'importo non soggetto supera la soglia prevista, e
   consente di indicare chi la assolve e se è addebitata al cliente.
4. **RF-4** — Il documento gestisce la **ritenuta d'acconto** e i contributi previdenziali di rivalsa, con
   l'effetto corretto sul netto a pagare.
5. **RF-5** — Gli arrotondamenti sono espliciti e coerenti: il totale del documento è sempre la somma dei suoi
   riepiloghi, mai un numero che non torna di un centesimo.
6. **RF-6** — Soglia, importo del bollo e aliquote sono **parametri**, non numeri scritti nel codice: cambiano per
   legge e per paese.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I parametri fiscali dell'account (regime, aliquota predefinita, gestione
  del bollo) sono per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Il calcolo è esposto sia come effetto della scrittura del
  documento sia come rotta di anteprima `POST /api/billing/v1/documents/preview-totals`, così che il frontend possa
  mostrare i totali prima del salvataggio; errori in `application/problem+json`; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_billing`: tabella `tax_summary` legata al documento e
  colonne per bollo, ritenuta e rivalsa, con `tenant_id`, chiave primaria UUID versione 7 e colonne di controllo.
  I riepiloghi si **scrivono** al momento dell'emissione: non si ricalcolano a posteriori, perché le aliquote
  cambiano e un documento emesso non deve cambiare mai.
- **RT-4 — Modulo frontend (§3, §5).** Il riepilogo per aliquota è sempre visibile mentre si compone il documento;
  gli importi usano il formato della lingua scelta. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le etichette delle imposte passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`. Attenzione: i nomi delle **nature** delle operazioni sono codici normativi, non stringhe da
  tradurre liberamente.
- **RT-6 — Varchi e quota (§6).** Nessun consumo di quota: il calcolo accompagna il documento, non lo crea.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento proprio: i totali calcolati compaiono dentro la
  bozza che `crea_fattura` restituisce (epica 06). È importante che la bozza mostrata per la conferma umana riporti
  i totali **calcolati dal servizio**, non quelli suggeriti da chi ha scritto la richiesta.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: le imposte riguardano importi, non persone.
- **RT-9 — Registrazione eventi (§14).** L'evento `emissione respinta per natura mancante` è registrato con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Riepilogo per aliquota**
- **Dato** un documento con due righe al 22% e una al 10%
- **Quando** si calcolano i totali
- **Allora** il riepilogo ha due voci con imponibile e imposta corretti, e il totale del documento è la loro somma

**CA-2 — Natura obbligatoria**
- **Dato** una riga con aliquota zero e nessuna natura indicata
- **Quando** si tenta di emettere il documento
- **Allora** la risposta è `409` con l'indicazione della riga incompleta, e nulla viene emesso

**CA-3 — Imposta di bollo**
- **Dato** un documento non soggetto a imposta sul valore aggiunto di 500 €
- **Quando** si calcolano i totali
- **Allora** il documento riporta l'imposta di bollo prevista e indica chi la assolve

**CA-4 — Sotto soglia**
- **Dato** un documento non soggetto di 50 € · **Quando** si calcolano i totali
- **Allora** nessuna imposta di bollo viene applicata

**CA-5 — Ritenuta d'acconto**
- **Dato** un documento con ritenuta del 20% su un imponibile di 1.000 €
- **Quando** si calcolano i totali
- **Allora** il netto a pagare è ridotto della ritenuta, mentre il totale del documento non lo è

**CA-6 — Arrotondamenti**
- **Dato** un documento con righe che producono terze cifre decimali
- **Quando** si calcolano i totali
- **Allora** la somma dei riepiloghi coincide al centesimo con il totale del documento

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo, fitte: sono la parte dell'app che più merita prove mirate (aliquote miste,
      esenzioni, bollo sopra e sotto soglia, ritenuta, arrotondamenti);
- [ ] prova di **integrazione** che verifica che i riepiloghi scritti all'emissione non cambino più;
- [ ] prova di **isolamento fra account** sui parametri fiscali dell'account;
- [ ] **prova end-to-end**: *coprire ora* — il documento del percorso `[J-BILLING]` ha due aliquote e un totale
      verificato; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di scrivere i riepiloghi invece di
      ricalcolarli;
- [ ] contratto degli **strumenti conversazionali**: nessuno proprio, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Servono documento e righe |
| storia `0007` | L'aliquota predefinita viene dalla voce di catalogo |

## 7. Fuori ambito

- le regole fiscali di paesi diversi dall'Italia: rimandate. BillGrove tiene i parametri fuori dal codice proprio
  per renderlo possibile, ma la conformità per giurisdizione è di InvoiceGrove (1);
- lo split payment e il reverse charge: rimandati, perché sono casi di clientela pubblica o intracomunitaria poco
  frequenti nel segmento micro; vanno però tenuti presenti perché il modello canonico EN 16931 li prevede (storia
  `0024`);
- la liquidazione periodica dell'imposta: non è di BillGrove, è del commercialista.

## 8. Punti aperti

I valori di soglia e importo del bollo sono parametri: chi li aggiorna quando la legge cambia è una domanda di
esercizio, non di sviluppo, e va posta allo sviluppatore. La proposta è che vivano nella configurazione del
servizio, non nella base dati per account, così che un aggiornamento normativo valga per tutti in un colpo solo.
