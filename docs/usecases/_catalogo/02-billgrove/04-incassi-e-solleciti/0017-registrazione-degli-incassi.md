# 0017 — Registrazione degli incassi

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0017` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che guarda l'estratto conto una volta a settimana
> voglio segnare quali fatture sono state pagate, anche solo in parte
> così da sapere in ogni momento chi mi deve dei soldi, senza tenere una colonna a parte su un foglio di calcolo.

**Contesto.** Senza gli incassi, l'app sa solo che cosa è stato emesso: metà dell'informazione che serve al
titolare. È la storia che rende possibili scadenzario, solleciti e report, ed è la prima dell'epica perché tutte le
altre la usano. In questa stesura l'incasso si registra **a mano**: il collegamento del conto corrente e la
riconciliazione automatica sono fuori ambito (§2.4 della descrizione) e appartengono a CashGrove (3).

## 2. Requisiti funzionali

1. **RF-1** — Si può registrare un incasso su un documento emesso, con data, importo, mezzo di pagamento e nota.
2. **RF-2** — Sono ammessi incassi **parziali** e più incassi sullo stesso documento.
3. **RF-3** — Lo stato di pagamento del documento si aggiorna da sé: `non pagato`, `pagato in parte`, `pagato`.
4. **RF-4** — La somma degli incassi non può superare il totale del documento al netto delle note di credito.
5. **RF-5** — Un incasso registrato per errore si può eliminare, e lo stato del documento torna indietro di
   conseguenza.
6. **RF-6** — Il documento mostra sempre il **residuo** da incassare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `payment` filtra per `tenant_id` preso dal
  token verificato; registrare un incasso su un documento di un altro account risponde `404`.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/billing/v1/documents/{id}/payments`,
  `DELETE /api/billing/v1/payments/{id}`; corpo validato; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V8__payment.sql` sullo schema `app_billing`: tabella `payment` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. Lo stato di pagamento
  del documento è **derivato** dalla somma degli incassi, non un interruttore scritto a parte: due sorgenti di
  verità qui producono sempre incoerenze.
- **RT-4 — Modulo frontend (§3, §5).** Azione «Registra incasso» sulla scheda del documento, con il residuo
  proposto come importo predefinito. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`, compresi i nomi dei mezzi di pagamento.
- **RT-6 — Varchi e quota (§6).** Nessun consumo di quota: la quota è sull'emissione. Ruolo `member` per registrare,
  `admin` per eliminare un incasso.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `registra_incasso(id_documento, importo, data, mezzo) → nuovo stato del documento`, marcato **scrittura**; produce
  una bozza e richiede conferma umana. Non è irreversibile — un incasso si può togliere — ma tocca i numeri su cui
  il titolare decide, e per questo la conferma resta. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** La nota dell'incasso è un **campo libero**: voce nuova nel manifesto in italiano
  e inglese, e la tabella `payment` va aggiunta a `exportData` e `purgeData`. Il **mezzo di pagamento** è testuale:
  l'app **non** registra coordinate bancarie né dati di carte, e questo va scritto, non sottinteso.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `incasso registrato`, `incasso eliminato` e `documento
  saldato` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati
  personali e **senza importi riferibili a un cliente**.

## 4. Criteri di accettazione

**CA-1 — Incasso totale**
- **Dato** una fattura emessa da 1.220 € non pagata
- **Quando** si registra un incasso di 1.220 €
- **Allora** il documento risulta `pagato` e il residuo è zero

**CA-2 — Incasso parziale**
- **Dato** la stessa fattura · **Quando** si registra un incasso di 500 €
- **Allora** il documento risulta `pagato in parte` e il residuo è 720 €

**CA-3 — Importo eccedente**
- **Dato** una fattura da 1.220 € già incassata per 1.000 €
- **Quando** si tenta di registrare un incasso di 300 €
- **Allora** la risposta è `409` con il residuo, e nulla viene registrato

**CA-4 — Eliminazione di un incasso**
- **Dato** una fattura `pagata` con un solo incasso · **Quando** si elimina l'incasso
- **Allora** il documento torna `non pagato` e il residuo torna pari al totale

**CA-5 — Isolamento fra account**
- **Dato** una fattura dell'account `B` · **Quando** un utente di `A` tenta di registrarci un incasso
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul calcolo dello stato derivato e del residuo, di **integrazione** sulla risorsa, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `payment`;
- [ ] **prova end-to-end**: *coprire ora* — passo finale del percorso `[J-BILLING]`: registra l'incasso e verifica
      che il documento risulti pagato; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la nota dell'incasso, e `payment` presente in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con annotata la scelta dello stato derivato;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `registra_incasso`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | Si incassano documenti emessi |

## 7. Fuori ambito

- il collegamento del conto corrente e la riconciliazione automatica dei movimenti: **fuori ambito dichiarato**,
  perché introdurrebbe un fornitore esterno che tratta dati bancari; la riconciliazione appartiene a CashGrove (3);
- il collegamento di pagamento sul documento (il cliente paga cliccando): rimandato, apre una superficie non
  autenticata e un fornitore di incasso;
- gli interessi di mora: sono di CashGrove (3).

## 8. Punti aperti

Nessuno.
