# 0019 — Registrazione dell'incasso

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 04 — Ordini e pagamenti
**Storia**: `0019` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio segnare che un ordine è stato pagato, con quanto e come
> così da sapere a colpo d'occhio chi mi deve ancora dei soldi.

**Contesto.** La richiesta di pagamento parte, ma poi qualcuno paga — con un bonifico istantaneo, in contanti
alla consegna, o con il collegamento inviato. Senza la registrazione, l'ordine resta per sempre «confermato» e
l'app perde credibilità al secondo giorno d'uso. La registrazione resta **manuale** di proposito: il riscontro
automatico richiederebbe di collegare lo strumento di incasso del negozio, che è un'altra decisione (storia
`0018`, punti aperti).

## 2. Requisiti funzionali

1. **RF-1** — Da una richiesta di pagamento `emessa` si registra l'incasso indicando importo, data e mezzo
   (collegamento, contanti, bonifico, altro), con una nota facoltativa.
2. **RF-2** — La registrazione porta la richiesta a `pagata` e l'ordine a `pagato`, se l'importo copre il
   totale.
3. **RF-3** — Sono ammessi **incassi parziali**: l'ordine resta `confermato` e mostra il residuo dovuto.
4. **RF-4** — Un incasso registrato per errore si può **stornare**, con motivo obbligatorio; lo storno riporta
   gli stati indietro e resta visibile nello storico.
5. **RF-5** — L'elenco degli ordini mostra il residuo dovuto e si filtra su «non pagati».
6. **RF-6** — Ogni registrazione porta chi l'ha fatta e quando.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `payment` filtra per `tenant_id` preso
  dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/chat_commerce/v1/payment-requests/{id}/payments` e `POST .../payments/{paymentId}/reverse`; corpo
  validato (importo positivo, non superiore al residuo; motivo obbligatorio per lo storno); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V12__incassi.sql`: tabella `payment` con `tenant_id`, chiave
  primaria UUID versione 7, colonne di controllo e cancellazione logica. Gli importi sono in centesimi; il
  residuo si **calcola**, non si conserva. Registrazione e cambio di stato dell'ordine nella stessa transazione.
- **RT-4 — Ruoli (§6).** Tutti i ruoli possono registrare un incasso; solo `owner` e `admin` possono stornarlo.
  Un `member` che tenta lo storno riceve `403`.
- **RT-5 — Modulo frontend (§3, §4, §5).** Azione nella scheda dell'ordine, con conferma esplicita per lo
  storno; colonna del residuo nell'elenco. Tutte le stringhe in `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** Voce nuova nel manifesto in italiano e inglese per `payment.reference`
  (dato economico riferito a una persona); tabella `payment` aggiunta a `exportData` e `purgeData`. Nessun dato
  di carta né coordinate bancarie del cliente: si registra un riferimento, non uno strumento di pagamento.
- **RT-7 — Registrazione eventi (§14).** `incasso registrato`, `incasso stornato` con `tenant_id`, `app_id`,
  `user_id`, numero dell'ordine, importo e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Incasso pieno**
- **Dato** un ordine da 27,00 € con richiesta `emessa`
- **Quando** si registra un incasso di 27,00 €
- **Allora** la richiesta è `pagata`, l'ordine è `pagato` e il residuo è zero

**CA-2 — Incasso parziale**
- **Dato** lo stesso ordine · **Quando** si registra un incasso di 10,00 € · **Allora** l'ordine resta
  `confermato` con residuo 17,00 €

**CA-3 — Importo eccedente**
- **Dato** un residuo di 17,00 € · **Quando** si registra un incasso di 20,00 € · **Allora** la richiesta è
  respinta con `400` e nulla viene registrato

**CA-4 — Storno**
- **Dato** un ordine `pagato` · **Quando** si storna l'incasso con motivo «registrato per errore»
- **Allora** l'ordine torna `confermato`, il residuo torna 27,00 € e lo storico mostra entrambe le operazioni

**CA-5 — Ruolo insufficiente**
- **Dato** un utente `member` · **Quando** tenta uno storno · **Allora** riceve `403`

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** un utente di `A` tenta di registrare un incasso su una richiesta di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del residuo (compresi i parziali multipli) e di **integrazione** su
      registrazione e storno nella stessa transazione;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sugli incassi;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`, dove l'incasso chiude il percorso `[J-CHAT-COMMERCE]`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, tabella in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta della registrazione manuale e degli incassi parziali;
- [ ] contratto degli **strumenti conversazionali**: la registrazione di un incasso è **scrittura con conferma
      umana** — non esce nulla, ma cambia i conti del negozio;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0018` | L'incasso si registra su una richiesta di pagamento |

## 7. Fuori ambito

- il riscontro automatico dall'istituto di incasso: punto aperto della storia `0018`;
- il rimborso vero al cliente: qui c'è lo storno di una registrazione, che è una correzione contabile interna;
- la contabilità e la fattura: app 1, 2 e 3 del catalogo.

## 8. Punti aperti

- Nessuno.
