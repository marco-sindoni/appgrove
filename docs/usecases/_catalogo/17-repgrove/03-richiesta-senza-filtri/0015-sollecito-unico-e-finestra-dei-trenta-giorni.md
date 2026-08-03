# 0015 — Sollecito unico e finestra dei trenta giorni

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 03 — Richiesta di recensione senza filtri
**Storia**: `0015` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha mandato l'invito e non ha ricevuto risposta
> voglio che parta **un solo** promemoria, e che l'app smetta di insistere quando la recensione non sarebbe più
> lecita
> così da recuperare le recensioni dimenticate senza diventare molesto e senza far scrivere al cliente qualcosa
> che verrebbe rimosso.

**Contesto.** Il promemoria è la funzione che più alza la resa e più facilmente diventa un fastidio. Qui però non
è solo una questione di buon gusto: la legge italiana 34/2026 richiede che la recensione sia pubblicata **entro
trenta giorni** dalla fruizione del servizio, per le imprese di ristorazione, le strutture ricettive, gli
stabilimenti termali e le attrazioni turistiche situate in Italia (descrizione §2.3). Sollecitare al
trentacinquesimo giorno significa spingere il cliente a scrivere una recensione che nasce già priva di un
requisito.

Da qui la forma della storia: **un solo sollecito**, e una **finestra** che si chiude — obbligatoria dove la legge
si applica, consigliata altrove, e in entrambi i casi visibile.

## 2. Requisiti funzionali

1. **RF-1** — Se dopo un intervallo configurabile (proposta predefinita: 5 giorni) la richiesta risulta inviata e
   non risulta arrivata una recensione, parte **un solo** sollecito, con lo stesso modello approvato in una
   variante breve.
2. **RF-2** — Il sollecito è disattivabile per sede. Non è mai più di uno: la seconda ripetizione non esiste come
   opzione.
3. **RF-3** — Ogni servizio erogato ha una **finestra utile** che parte dal momento dell'erogazione. Per le sedi
   il cui settore ricade nella legge italiana la finestra è di **30 giorni** e non è allungabile; per gli altri
   settori è un valore predefinito modificabile, con un avviso che spiega la ragione del predefinito.
4. **RF-4** — Alla chiusura della finestra la richiesta passa in stato `scaduta`: non parte più nulla, né invito
   né sollecito, e il motivo compare nel registro di equità come esclusione lecita.
5. **RF-5** — L'app **non sa** se il cliente ha scritto la recensione, e non deve fingere di saperlo: le
   piattaforme non collegano una recensione all'invito. La condizione «non risulta arrivata una recensione» è
   quindi approssimata, con un criterio dichiarato (nessuna recensione nuova sulla sede fra l'invito e adesso), e
   l'interfaccia lo dice con una riga onesta.
6. **RF-6** — Un cliente che ha ricevuto un sollecito e non risponde **non viene reinserito** in nessun ciclo
   successivo per lo stesso servizio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione dei solleciti gira per account e scrive sempre con il
  `tenant_id` della richiesta d'origine.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova rivolta al cliente oltre alla configurazione
  (`PUT /api/recensioni/v1/sedi/{id}/impostazioni-invito`); errori in `application/problem+json`; il tentativo di
  impostare una finestra superiore a 30 giorni su una sede soggetta alla legge è rifiutato con `422` e con la
  spiegazione.
- **RT-3 — Persistenza (§8).** Colonne `sollecito_inviato_il` e `finestra_chiude_il` su `richiesta_recensione`;
  migrazione `V7__richiesta_sollecito.sql`. Anche queste sono colonne di prova: si scrivono una volta.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Richieste*: colonna con lo stato della finestra («chiude fra 12
  giorni», «scaduta»), e nelle impostazioni della sede l'intervallo del sollecito con la sua spiegazione.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe in `en, it, fr, es, de`, **compreso il testo che spiega la
  finestra**, che è un testo normativo e va tradotto con attenzione.
- **RT-6 — Varchi e quota (§6, §7).** Il sollecito non consuma quota. Con abbonamento `canceled` la lavorazione
  non parte.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo; lo stato della finestra compare nel
  risultato di `stato_delle_richieste` (storia 0027), perché è la risposta alla domanda «faccio ancora in tempo?».
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: si aggiungono due momenti a una tabella già
  dichiarata.
- **RT-9 — Registrazione eventi (§14).** `sollecito inviato`, `finestra chiusa: n richieste scadute`, con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza recapiti.

## 4. Criteri di accettazione

**CA-1 — Un solo sollecito**
- **Dato** una richiesta inviata sei giorni fa, senza recensioni nuove sulla sede
- **Quando** la lavorazione gira due volte a distanza di un giorno
- **Allora** il sollecito parte una volta sola

**CA-2 — Sollecito disattivato**
- **Dato** una sede con il sollecito disattivato
- **Quando** la lavorazione gira
- **Allora** non parte nessun sollecito, e l'elenco lo mostra come scelta, non come guasto

**CA-3 — Finestra obbligatoria dove la legge si applica**
- **Dato** una sede di settore «ristorazione» in Italia
- **Quando** si tenta di impostare una finestra di 45 giorni
- **Allora** la richiesta è rifiutata con `422` e la spiegazione del termine di trenta giorni

**CA-4 — Finestra chiusa**
- **Dato** un servizio erogato 31 giorni fa con richiesta inviata e nessun sollecito ancora partito
- **Quando** la lavorazione gira
- **Allora** la richiesta passa in `scaduta`, non parte nulla, e il registro di equità riporta il motivo

**CA-5 — Isolamento fra account**
- **Dato** due account con richieste in attesa di sollecito
- **Quando** la lavorazione gira per `A`
- **Allora** nessun sollecito parte per i clienti di `B`

**CA-6 — L'onestà sull'approssimazione**
- **Dato** la schermata delle richieste
- **Quando** una richiesta risulta «senza recensione»
- **Allora** l'interfaccia spiega che l'app non può sapere chi ha scritto cosa e con quale criterio lo deduce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della finestra per settore e sull'unicità del sollecito; di **integrazione**
      sulla lavorazione con database effimero e fornitore simulato;
- [ ] prova di **isolamento fra account** sulla lavorazione dei solleciti;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-RECENSIONI]` copre l'invito, non il decorso di trenta
      giorni; il comportamento nel tempo è coperto a livello di integrazione con un orologio controllabile. Voce
      motivata nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con il riferimento normativo alla finestra dei trenta giorni e la
      dichiarazione che il testo di legge non è stato letto in originale (descrizione §2.7);
- [ ] contratto degli **strumenti conversazionali**: lo stato della finestra entra nel risultato di lettura.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` | il sollecito è il seguito di un invito |
| storia `0006` | il settore della sede decide se la finestra è obbligatoria |

## 7. Fuori ambito

- il registro di equità esportabile — storia 0016;
- la decadenza biennale delle recensioni prevista dalla stessa legge: riguarda le recensioni ricevute, non gli
  inviti, e sta nella storia 0021.

## 8. Punti aperti

- **Il testo della legge non è stato letto in originale** (descrizione §2.7): i trenta giorni e l'ambito di
  applicazione vengono da due fonti qualificate ma secondarie. Prima di far dipendere il codice da quel numero, va
  letto l'articolato e vanno lette le linee guida attuative dell'Autorità garante.
- **L'intervallo di cinque giorni** per il sollecito è una proposta, non un dato: va misurato.
- **Se un giorno le piattaforme permettessero di sapere chi ha recensito**, il criterio approssimato del RF-5
  andrebbe sostituito. Oggi non è possibile, e va detto invece di simulare una certezza.
</content>
