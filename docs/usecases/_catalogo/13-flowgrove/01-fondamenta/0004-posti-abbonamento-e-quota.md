# 0004 — Posti, abbonamento e quota

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che paga il piano Squadra
> voglio che l'app sappia quante persone ho dentro e me lo dica prima che io sfori
> così da non scoprire il limite nel momento sbagliato, e da capire subito come si rimedia.

**Contesto.** La metrica di quota di FlowGrove è `seats` (posti occupati) di natura `stock`
([application-description.md](../application-description.md) §3): è un tetto su ciò che esiste ora, non un
consumo che si azzera. Le conseguenze sono due, e sono entrambe controintuitive se si è abituati alle metriche a
consumo: il posto si libera **togliendo** una persona, e il passaggio a un piano inferiore va **bloccato** finché
i posti occupati eccedono il tetto di destinazione, altrimenti l'account resterebbe stabilmente fuori norma
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §13).

## 2. Requisiti funzionali

1. **RF-1** — L'app mantiene una **proiezione locale** dell'abilitazione dell'account, alimentata a eventi: piano
   attivo, stato dell'abbonamento, tetto sui posti. Nessuna chiamata di rete sincrona all'app centrale sul
   percorso caldo.
2. **RF-2** — Un utente dell'account **occupa un posto** quando gli viene assegnata la prima attività oppure
   quando dichiara le prime ore; il conteggio dei posti occupati è visibile nella *Panoramica*.
3. **RF-3** — Quando i posti occupati raggiungono il tetto, l'assegnazione a una persona nuova e la prima
   dichiarazione di ore di una persona nuova rispondono `429` con un messaggio che dice quanti posti ci sono,
   quanti se ne usano e come si rimedia (liberare un posto o cambiare piano).
4. **RF-4** — Un posto si libera togliendo la persona dall'app (nessuna assegnazione aperta e nessuna
   dichiarazione di ore nel periodo aperto); l'operazione **non** cancella le ore già dichiarate, che restano nel
   consuntivo.
5. **RF-5** — Il passaggio a un piano con tetto inferiore ai posti occupati è **bloccato**, con un messaggio che
   dice quante persone vanno tolte prima.
6. **RF-6** — Con abbonamento in `trialing`, `active` o `past_due` l'app funziona; con `paused` o `canceled`
   risponde `402`. L'esportazione dei propri dati resta accessibile in ogni caso.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La proiezione dell'abilitazione è per account e si legge sempre con il
  `tenant_id` del token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/progetti/v1/quota` che restituisce tetto, posti
  occupati e stato dell'abbonamento; errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabelle `entitlement_projection` e `seat_usage` sullo schema `app_progetti`, con
  `tenant_id`, colonne di controllo e cancellazione logica; il conteggio dei posti è **derivato** e ricalcolabile,
  mai un numero scritto a mano.
- **RT-4 — Varchi e quota (§6, §7).** La catena completa è rispettata: `401` senza token, `403` ad app spenta,
  `402` senza abbonamento valido, `403` a ruolo insufficiente, `429` a quota esaurita. La prenotazione del posto
  avviene **prima** dell'operazione, e il rilascio avviene se l'operazione fallisce.
- **RT-5 — Modulo frontend (§3, §5).** Barra di consumo dei posti nella *Panoramica*; l'avviso compare **prima**
  dell'azione che sforerebbe, non dopo l'errore; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Tutti i messaggi di quota e di abbonamento in `en, it, fr, es, de`, compreso il
  testo che spiega come si libera un posto.
- **RT-7 — Dati personali (§10).** `seat_usage` contiene l'identificativo dell'utente: voce nuova nel manifesto in
  italiano e inglese, campo annotato, tabella in esportazione e cancellazione.
- **RT-8 — Registrazione eventi (§14).** «Posto occupato», «posto liberato», «operazione respinta per quota» con
  `tenant_id`, `app_id`, `user_id` e correlazione; mai il nome della persona.

## 4. Criteri di accettazione

**CA-1 — Occupazione di un posto**
- **Dato** un account sul piano `squadra` con 9 posti occupati su 10
- **Quando** si assegna un'attività a una decima persona
- **Allora** l'assegnazione riesce e il conteggio passa a 10 su 10

**CA-2 — Quota esaurita**
- **Dato** lo stesso account con 10 posti su 10
- **Quando** si assegna un'attività a un'undicesima persona
- **Allora** la risposta è `429`, nessuna assegnazione viene creata e il messaggio dice che i posti sono 10 su 10
  e come si rimedia

**CA-3 — Rilascio del posto senza perdita di dati**
- **Dato** una persona con 40 ore dichiarate e nessuna assegnazione aperta
- **Quando** la si toglie dall'app
- **Allora** il posto si libera e le 40 ore restano nel consuntivo del progetto

**CA-4 — Passaggio a un piano inferiore bloccato**
- **Dato** un account con 8 posti occupati che chiede il piano `free` (tetto 3)
- **Quando** conferma il cambio
- **Allora** il cambio è rifiutato con un messaggio che dice che vanno tolte 5 persone prima

**CA-5 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled`
- **Quando** chiama una qualsiasi rotta di FlowGrove
- **Allora** riceve `402` — tranne l'esportazione dei propri dati, che risponde normalmente

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` su piani diversi
- **Quando** un utente di `A` chiede la propria quota
- **Allora** vede il tetto del proprio piano, mai quello di `B`, anche forzando l'identificativo nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio dei posti e sul blocco del passaggio a un piano inferiore, e di
      **integrazione** sulla catena dei varchi;
- [ ] prova di **isolamento fra account** sulla proiezione dell'abilitazione;
- [ ] **prova end-to-end**: rimando alla storia 0031, che percorre anche il rifiuto per quota;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per `seat_usage`;
- [ ] **registro delle decisioni** compilato, con annotata la regola «un posto si occupa alla prima assegnazione o
      alla prima riga di ore» e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuno proprio, ma tutti gli strumenti di scrittura
      dell'epica 06 attraversano questi varchi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Serve il servizio |
| Storia `0002` | Servono le tabelle |
| Storia `0003` | Serve la *Panoramica* dove mostrare il consumo |
| File di listino `pricing/progetti.yaml` | Tetti e piani vengono da lì; i **numeri** li conferma lo sviluppatore (§5 della descrizione) |

## 7. Fuori ambito

- l'acquisto e il cambio di piano: sono della sezione Fatturazione della piattaforma, non dell'app;
- la deroga temporanea sul tetto concessa in assistenza: è della console di amministrazione
  ([estensioni-admin.md](../estensioni-admin.md) §3).

## 8. Punti aperti

- **Prezzi e tetti** restano una fermata di escalation dello sviluppatore
  ([application-description.md](../application-description.md) §5): questa storia implementa il meccanismo, non
  fissa i numeri.
- Se il piano `free` a 3 posti si rivelasse troppo generoso o troppo stretto, cambiarlo dopo il lancio è possibile
  solo creando un piano nuovo: i prezzi vivi sono immutabili.
