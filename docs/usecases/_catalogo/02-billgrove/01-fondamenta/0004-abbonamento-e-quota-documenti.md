# 0004 — Abbonamento e quota documenti

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha scelto il piano più piccolo
> voglio che l'app mi dica chiaramente quanti documenti mi restano e che cosa succede quando finiscono
> così da non scoprire il limite nel momento peggiore, cioè mentre sto emettendo la fattura di un cliente che
> aspetta.

**Contesto.** La metrica di quota di BillGrove è `documenti`, di natura `flow` (§3 della descrizione): un consumo su
una finestra che si azzera. Va costruita adesso, prima delle storie che emettono documenti, perché l'ordine inverso
produce sempre lo stesso difetto — la funzione nasce senza varco e il varco si aggiunge dopo, dimenticandone metà.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio conosce l'abilitazione dell'account leggendo la **proiezione locale** alimentata a eventi,
   senza mai chiamare l'app centrale sul percorso caldo.
2. **RF-2** — Esiste una rotta che restituisce il consumo corrente e il tetto del piano per la metrica `documenti`.
3. **RF-3** — Prima di un'operazione che consuma quota il servizio **prenota** una unità; se il tetto è raggiunto
   risponde `429` e non crea nulla.
4. **RF-4** — Il messaggio del `429` dice tre cose: che cosa è successo, che cosa non si può più fare, come si
   rimedia.
5. **RF-5** — Con abbonamento in `trialing`, `active` o `past_due` la funzione resta accessibile; con `paused` o
   `canceled` risponde `402`.
6. **RF-6** — L'esportazione dei dati e i diritti dell'interessato restano accessibili anche con app disabilitata o
   abbonamento scaduto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il consumo si conta per `tenant_id` preso dal token verificato; nessun
  conteggio è leggibile o alterabile da un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/billing/v1/quota`; errori in `application/problem+json`
  con il codice `429` corredato dell'informazione su quando la finestra si azzera; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_billing`: tabella di proiezione dell'abilitazione e
  tabella del contatore di consumo per finestra, con `tenant_id` e colonne di controllo.
- **RT-4 — Modulo frontend (§3, §5).** La Panoramica mostra la barra di consumo; l'avviso compare **prima** del
  modulo di creazione, non dopo il salvataggio.
- **RT-5 — Cinque lingue (§4).** I messaggi di quota e di abbonamento passano dallo spazio-nomi `billing` e ci sono
  in tutte e cinque le lingue.
- **RT-6 — Varchi e quota (§6, §7).** La catena completa: `401` senza token, `403` ad app spenta, `402` senza
  abilitazione, `403` a ruolo insufficiente, `429` a quota esaurita. La storia **non fissa prezzi**: consuma il
  tetto pubblicato dall'abilitazione per la metrica `documenti`, di natura `flow`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento qui, ma il varco costruito adesso è lo stesso che
  la storia `0030` applicherà alle chiamate dell'assistente: la prenotazione va scritta in un punto solo.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: i contatori sono numeri per account.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `quota prenotata`, `quota esaurita` e `accesso negato per
  abbonamento` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Consumo visibile**
- **Dato** un account sul piano con tetto 60 documenti al mese, che ne ha emessi 12
- **Quando** chiama la rotta della quota
- **Allora** riceve consumo 12, tetto 60 e la data in cui la finestra si azzera

**CA-2 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `documenti`
- **Quando** tenta un'operazione che consuma quota
- **Allora** riceve `429`, un messaggio che spiega come rimediare, e **nulla viene creato**

**CA-3 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled` · **Quando** chiama una rotta protetta dell'app
- **Allora** riceve `402`; ma la rotta di esportazione dei propri dati continua a rispondere `200`

**CA-4 — Tolleranza sui pagamenti falliti**
- **Dato** un account in `past_due` · **Quando** usa l'app
- **Allora** la funzione resta accessibile

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con consumi diversi
- **Quando** un utente di `A` chiede la quota, anche forzando l'identificativo di `B`
- **Allora** vede solo il consumo di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla prenotazione e di **integrazione** sulla rotta della quota, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** sul contatore di consumo, e matrice dei ruoli;
- [ ] **prova end-to-end**: *coprire ora* — passo del percorso `[J-BILLING]` che verifica il blocco a quota
      esaurita con il fornitore di pagamento simulato; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica, dichiarato;
- [ ] **registro delle decisioni** compilato, con annotata la natura `flow` della metrica e la sua finestra;
- [ ] contratto degli **strumenti conversazionali**: nessuno qui, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | Serve la catena dei varchi cablata |
| storia `0002` | Serve qualcosa da contare |
| Listino `pricing/billing.yaml` | Il tetto arriva da lì: senza listino non c'è tetto. È una fermata di escalation |

## 7. Fuori ambito

- il consumo effettivo all'emissione: storia `0012`, che chiama la prenotazione costruita qui;
- la deroga amministrativa alla quota: [estensioni-admin.md](../estensioni-admin.md);
- il cambio di piano e la disdetta: sono di piattaforma, non dell'app.

## 8. Punti aperti

La **finestra** della metrica — mensile o annuale — non è decisa: il mercato italiano vende a documenti per anno,
l'app reale `fatture` usa una finestra mensile (§5 della descrizione). È parte della fermata di escalation sul
listino e la chiude lo sviluppatore.
