# 0007 — Registrazione dei crediti

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 02 — Portafoglio crediti
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0004`, `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetta all'amministrazione
> voglio registrare una fattura emessa e non ancora incassata, con la sua scadenza e il suo importo
> così da avere in un solo posto l'elenco di quello che devo ancora incassare.

**Contesto.** È la storia che dà all'app la sua ragione d'essere: senza crediti dentro, tutto il resto è impianto. Si
inserisce a mano perché è la via che funziona **subito**, senza collegare nulla: dall'analisi in rete risulta che la
configurazione lunga è la prima causa di abbandono di questi prodotti ([documento capofila](../application-description.md)
§2.5). L'importazione da file arriva subito dopo (storia `0008`) e l'innesto sul gestionale è dichiaratamente fuori
dalle 31 storie.

## 2. Requisiti funzionali

1. **RF-1** — L'utente registra un credito indicando debitore, numero e data del documento, data di scadenza, importo e
   valuta; l'importo residuo nasce uguale all'importo originario.
2. **RF-2** — Il credito nasce in stato `aperto` se la scadenza è futura, in stato `scaduto` se è già passata.
3. **RF-3** — Il debitore si sceglie da quelli esistenti oppure si crea al volo dalla stessa schermata, senza perdere
   quello che si è già digitato.
4. **RF-4** — L'elenco dei crediti si filtra per stato, per debitore e per fascia di scaduto, e si ordina per scadenza.
5. **RF-5** — La creazione consuma una unità della metrica `crediti_monitorati`; l'avviso di consumo compare **prima**
   del modulo, non dopo il salvataggio.
6. **RF-6** — La scheda del credito mostra importo originario, incassato, residuo e giorni di scaduto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dell'entità `Credito` filtra per `tenant_id` preso
  dal token verificato; il riferimento al debitore è verificato nello **stesso** account, altrimenti la richiesta è
  respinta: è il punto in cui un errore permetterebbe di scoprire l'esistenza di dati altrui.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET`, `POST`, `PATCH /api/crediti/v1/crediti` (e `/{id}`);
  corpo validato; errori in `application/problem+json`; paginazione con totale; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** La tabella esiste dalla storia `0002`; qui si aggiunge il calcolo dei giorni di scaduto
  come dato derivato (non colonna) e si verifica l'uso degli indici su (`tenant_id`, `stato`, `data_scadenza`).
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Crediti* del modulo `crediti`: elenco con filtri, scheda di dettaglio,
  modulo di inserimento. Dati letti con il client generato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`; gli importi e le date sono formattati secondo la lingua attiva, non scritti a mano.
- **RT-6 — Varchi e quota (§6, §7).** Prima di creare un credito il servizio prenota una unità della metrica
  `crediti_monitorati` (natura `stock`); a quota esaurita risponde `429` con l'indicazione del rimedio e non crea nulla.
  Con abbonamento non attivo risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: la creazione di un credito da chat
  è una scrittura e viene dichiarata insieme alle altre nella storia `0029`, dove si definisce la regola della bozza.
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo: gli importi riguardano il debitore ma le voci del
  manifesto per `credito` esistono già; si verifica che siano coerenti con i campi realmente raccolti.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «credito creato», «credito modificato», «creazione respinta per
  quota» sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo del credito e identificativo di
  correlazione, senza dati personali e **senza importi** nei registri.

## 4. Criteri di accettazione

**CA-1 — Registrazione di un credito futuro**
- **Dato** un debitore esistente e una scadenza fra 30 giorni
- **Quando** l'utente registra un credito da 1.200 €
- **Allora** il credito compare in elenco in stato `aperto`, con residuo 1.200 € e zero giorni di scaduto

**CA-2 — Registrazione di un credito già scaduto**
- **Dato** una scadenza di 45 giorni fa · **Quando** l'utente registra il credito · **Allora** nasce in stato `scaduto`
  e l'elenco lo mostra nella fascia «31-60 giorni»

**CA-3 — Debitore di un altro account**
- **Dato** un utente dell'account `A` che indica l'identificativo di un debitore dell'account `B`
- **Quando** invia la richiesta
- **Allora** riceve lo stesso errore che riceverebbe per un debitore inesistente — l'esistenza altrui non è deducibile

**CA-4 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `crediti_monitorati`
- **Quando** tenta di registrare un credito
- **Allora** riceve `429` con un messaggio che spiega come rimediare, e nulla viene creato

**CA-5 — Importo non valido**
- **Dato** il modulo di inserimento · **Quando** si indica un importo pari a zero o negativo · **Allora** la
  validazione lo respinge in linea, prima dell'invio, e il servizio lo respinge comunque con `400`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sul calcolo dei giorni di scaduto e di **integrazione** sulla risorsa `crediti`;
- [ ] prova di **isolamento fra account** sulla risorsa, compreso il riferimento incrociato al debitore;
- [ ] **prova end-to-end**: *rimando* alla storia `0031` — motivo: il percorso completo comprende il sollecito, che non
      esiste ancora;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato; nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di far nascere `scaduto` alla creazione;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0004` | La creazione deve passare dalla prenotazione di quota |
| storia `0006` | Un credito senza debitore non esiste |

## 7. Fuori ambito

- L'importazione da file: storia `0008`.
- Gli incassi e il residuo che scende: storia `0009`.
- Il passaggio automatico `aperto` → `scaduto` al maturare della scadenza: storia `0010`.
- Note di credito e storni: rimandati; se emergeranno saranno una storia dell'epica 02.

## 8. Punti aperti

Nessuno.
