# 0021 — Budget di commessa e sforamento

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 04 — Ore lavorate e fatturabilità
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`, `0017`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha preventivato quaranta ore
> voglio essere avvisato quando siamo a trenta, non quando siamo a sessanta
> così da poter richiamare il cliente prima di aver già perso i soldi.

**Contesto.** È la storia che porta il valore promesso dall'intera applicazione. Il danno che FlowGrove esiste per
evitare è documentato e sempre uguale: le aziende su commessa scoprono di aver lavorato sessanta ore su un budget
da quaranta **quando fatturano a fine mese**, perché il lavoro e il conto stanno in strumenti diversi
([Productive, guida alla fatturazione di progetto](https://productive.io/blog/project-billing/),
[application-description.md](../application-description.md) §2.6/6). Qui quel ritardo si toglie: il budget vive
accanto alle ore e l'avviso arriva **prima**.

## 2. Requisiti funzionali

1. **RF-1** — Un progetto può avere un budget in **ore** e/o un budget in **importo**; entrambi sono facoltativi
   e indipendenti (chi vende a corpo ragiona in importo, chi vende a giornate in ore).
2. **RF-2** — Il consumo si calcola dalle righe di ore: ore dichiarate contro budget in ore; importo maturato
   (tariffa congelata × ore fatturabili) contro budget in importo. Le ore **non fatturabili** consumano il budget
   in ore ma non l'importo, e questo va mostrato distintamente.
3. **RF-3** — Esistono due soglie di avviso per progetto, con valori predefiniti al 75 % e al 100 %: al loro
   superamento il responsabile riceve un avviso dentro l'app (storia 0016).
4. **RF-4** — La scheda del progetto mostra sempre il consumo con una barra e i numeri: consumato, budget,
   residuo, e la proiezione a fine lavoro se l'andamento resta quello.
5. **RF-5** — Il superamento del budget **non blocca** la dichiarazione delle ore: il lavoro fatto va registrato
   comunque, altrimenti l'app spinge a mentire. Blocca invece la creazione di attività nuove? No — avvisa e basta.
6. **RF-6** — Il budget iniziale può arrivare da un preventivo accettato (storia 0023) o dalle stime del modello
   di progetto (storia 0010); resta comunque modificabile, e la modifica lascia traccia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo del consumo legge solo righe dell'account del token
  verificato; il budget appartiene al progetto e quindi all'account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|PUT /api/progetti/v1/projects/{id}/budget` e
  `GET /api/progetti/v1/projects/{id}/budget-consumption`; errori in `application/problem+json`; OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V13__budget.sql`: colonne di budget su `project` (ore e importo in
  centesimi), soglie, e `project_budget_alert` per non ripetere lo stesso avviso ogni giorno. Il consumo è
  **derivato**, mai un totale scritto a mano che può divergere.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro del budget nella scheda del progetto, con la barra che cambia
  colore alle soglie; solo token del sistema di design (nessun rosso scritto a mano); tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, avvisi di soglia e testo della proiezione in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Ruolo minimo per vedere e cambiare il budget:
  `admin` (è informazione economica, come le tariffe).
- **RT-7 — Esposizione conversazionale (§12).** Il consumo del budget entra nel risultato di
  `get_project_progress(id)` e di `get_project_margin(id)`, entrambi **lettura** (storia 0028), con il filtro di
  ruolo: a chi non ha ruolo `admin` gli importi non vengono restituiti.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il budget è del progetto. Il consumo si mostra
  **aggregato per progetto**, mai per persona.
- **RT-9 — Registrazione eventi (§14).** «Budget impostato», «soglia di budget superata» con `tenant_id`,
  `app_id`, `user_id`, progetto e percentuale; nessun dato personale.

## 4. Criteri di accettazione

**CA-1 — Avviso di soglia**
- **Dato** un progetto con budget di 40 ore e soglia al 75 %
- **Quando** le ore dichiarate arrivano a 30
- **Allora** il responsabile riceve un avviso dentro l'app, una volta sola

**CA-2 — Ore non fatturabili**
- **Dato** un progetto con 30 ore fatturabili e 6 non fatturabili, budget 40 ore e budget importo 2.000 €
- **Quando** si apre il riquadro del budget
- **Allora** il consumo in ore è 36 su 40 e il consumo in importo considera solo le 30 fatturabili, con la
  differenza spiegata

**CA-3 — Sforamento senza blocco**
- **Dato** un progetto con budget esaurito
- **Quando** una persona dichiara altre ore
- **Allora** la registrazione riesce, il consumo supera il 100 % e l'avviso di superamento parte

**CA-4 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** apre la scheda del progetto
- **Allora** non vede il budget in importo né la proiezione economica

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede il consumo di un progetto di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sul calcolo del consumo e della proiezione (compresi i casi limite: budget zero, nessuna
      ora) e di **integrazione** sull'avviso di soglia non ripetuto;
- [ ] prova di **isolamento fra account** e prova della matrice dei ruoli sul budget;
- [ ] **prova end-to-end**: coprire ora — `[J-PROGETTI]` verifica il superamento della soglia (storia 0031); voce
      nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, con annotato che il consumo si mostra solo aggregato;
- [ ] **registro delle decisioni** compilato, con annotata la scelta «avvisa ma non blocca» e il perché;
- [ ] controllo automatico di **accessibilità** verde sul riquadro del budget (la barra colorata non deve essere
      l'unico veicolo dell'informazione);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0016` | L'avviso di soglia passa dagli avvisi dentro l'app |
| Storia `0017` | Il consumo si calcola dalle righe di ore |
| Storia `0018` | Il consumo in importo usa la tariffa congelata |

## 7. Fuori ambito

- il costo interno e il margine: storia 0026 (qui si guarda il consumo, non il guadagno);
- i costi esterni imputati alla commessa: storia 0024;
- il budget per singola attività o per traguardo: rimandato, perché moltiplicherebbe la configurazione.

## 8. Punti aperti

- Nessuno.
