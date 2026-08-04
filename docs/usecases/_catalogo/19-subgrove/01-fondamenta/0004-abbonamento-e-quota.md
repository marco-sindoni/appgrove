# 0004 — Abbonamento e quota

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha centocinquanta iscritti e un piano che ne prevede centocinquanta
> voglio sapere in anticipo cosa succede quando arriva il centocinquantunesimo
> così da non scoprirlo il giorno in cui una persona è davanti al banco con la carta in mano.

**Contesto.** La quota di questa app è **a giacenza** sulla metrica `abbonamenti_attivi`: il tetto vale su quanti
abbonamenti l'app sorveglia adesso, non su quanti se ne sottoscrivono nel mese. La scelta è argomentata nel varco
d'identità (§3 della descrizione) e ha due conseguenze che questa storia deve rendere vere. La prima: il rifiuto
capita a chi **sottoscrive** un abbonamento nuovo, cioè a un utente autenticato, mai a un abbonato che sta
guardando la propria pagina. La seconda: la **riduzione di piano è sbarrata** finché lo stato eccede il tetto di
destinazione — è la regola di piattaforma per le metriche a giacenza, e va spiegata con il rimedio, non solo con
il divieto. Sta nelle fondamenta perché un varco che arriva tardi lascia dietro di sé tutte le funzioni nate
senza.

C'è poi una precisazione che vale la pena scrivere una volta per tutte: la quota di cui si parla qui è quella
**dell'abbonamento di appgrove con il proprio cliente**. Non ha nulla a che vedere con i piani che il cliente
vende ai propri abbonati (storia `0006`). Sono due livelli diversi e non si toccano mai.

## 2. Requisiti funzionali

1. **RF-1** — L'accesso a ogni funzione dell'app attraversa la catena dei varchi: token valido, app non spenta
   dalla piattaforma, account abilitato, ruolo sufficiente, quota non esaurita.
2. **RF-2** — Contano nella metrica `abbonamenti_attivi` gli abbonamenti negli stati `in_prova`, `attivo`,
   `in_ritardo`, `disdetto_a_scadenza` e `sospeso`; **non** conta lo stato `cessato`.
3. **RF-3** — Sottoscrivere un abbonamento oltre il tetto risponde `429` con un messaggio che dice quanti ne sono
   vivi, quanti ne prevede il piano e come rimediare; nulla viene creato.
4. **RF-4** — Il passaggio a un piano di appgrove inferiore è **bloccato** finché gli abbonamenti vivi superano il
   tetto del piano di destinazione, con un messaggio che dice quanti cessarne.
5. **RF-5** — Con abbonamento di piattaforma in `trialing`, `active` o `past_due` l'app funziona; con `paused` o
   `canceled` risponde `402`. L'esportazione e la cancellazione dei dati restano accessibili in ogni caso.
6. **RF-6** — L'abilitazione si legge dalla **proiezione locale** alimentata a eventi, mai con una chiamata di
   rete sincrona al servizio centrale sul percorso caldo.

## 3. Requisiti tecnici

- **RT-1 — Varchi e quota (§6, §7).** Prima di creare un abbonamento il servizio prenota una unità della metrica
  `abbonamenti_attivi` (natura `stock`); a quota esaurita risponde `429`. Con abbonamento non attivo risponde
  `402`. La storia **non fissa prezzi**: consuma il tetto pubblicato dall'abilitazione.
- **RT-2 — Isolamento fra account (§1).** Il conteggio è per `tenant_id` preso dal token verificato; nessun
  conteggio attraversa gli account.
- **RT-3 — Interfaccia di programmazione (§2).** Errori in `problem+json` con codici stabili per «quota
  esaurita», «abbonamento di piattaforma non attivo» e «ruolo insufficiente»; definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** Un riquadro nella panoramica mostra abbonamenti vivi su tetto del piano, e
  lo dice **prima** che il tetto sia toccato, non dopo il rifiuto; solo token del sistema di design, tema chiaro
  e scuro.
- **RT-5 — Cinque lingue (§4).** Messaggi di rifiuto e indicatore di quota in `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: si contano righe, non persone.
- **RT-7 — Registrazione eventi (§14).** `abbonamento creato`, `creazione respinta per quota`, `riduzione di
  piano bloccata` con `tenant_id`, `app_id`, `user_id` e correlazione, senza dati personali.
- **RT-8 — Prove (§11).** Integrazione sulla catena dei varchi; matrice dei ruoli; prova con abbonamento di
  piattaforma in ciascuno stato; prova che `cessato` non consuma quota.

## 4. Criteri di accettazione

**CA-1 — Quota rispettata**
- **Dato** un account sul piano `studio` con 150 abbonamenti vivi
- **Quando** un utente prova a sottoscriverne un altro
- **Allora** riceve `429`, il messaggio dice «150 su 150: cessane uno o passa al piano superiore», e nulla viene
  creato

**CA-2 — La cessazione restituisce la quota**
- **Dato** lo stesso account · **Quando** cessa un abbonamento e ne sottoscrive un altro
- **Allora** l'operazione riesce e il conteggio resta 150

**CA-3 — Il sospeso continua a contare**
- **Dato** un account al tetto con dieci abbonamenti sospesi per mancato incasso
- **Quando** prova a sottoscriverne uno nuovo
- **Allora** riceve `429`: l'app sorveglia anche i sospesi, e il messaggio lo spiega

**CA-4 — Riduzione di piano sbarrata**
- **Dato** un account con 200 abbonamenti vivi che tenta di scendere a un piano da 150
- **Quando** conferma la riduzione
- **Allora** l'operazione è bloccata con un messaggio che dice quanti cessarne, e nulla è programmato

**CA-5 — Abbonamento di piattaforma non attivo**
- **Dato** un account con abbonamento `canceled` · **Quando** apre l'app · **Allora** riceve `402`, ma
  l'esportazione dei propri dati resta accessibile

**CA-6 — Tolleranza sui pagamenti falliti**
- **Dato** un account in `past_due` · **Quando** lavora sugli abbonati · **Allora** funziona tutto normalmente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sul conteggio a giacenza (quali stati contano) e di **integrazione** sulla catena dei
      varchi;
- [ ] prova di **isolamento fra account** sul conteggio;
- [ ] **prova end-to-end**: *rimando* — il rifiuto per quota entra nel percorso `[J-ABBONATI]` della storia
      `0033`, con voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dei messaggi in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato: quali stati contano nella metrica e perché il sospeso conta;
- [ ] avvio locale invariato, con fornitore di pagamento della piattaforma simulato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | serve una tabella su cui contare |
| decisione dello sviluppatore sul listino (§5 della descrizione) | i tetti dei piani vengono da lì |

## 7. Fuori ambito

- la sottoscrizione vera e propria, con i suoi campi: storia `0010`;
- i piani che il **cliente** vende ai suoi abbonati: storia `0006` — livello diverso, da non confondere;
- l'acquisto e il cambio del piano di appgrove: è di piattaforma.

## 8. Punti aperti

**Il sospeso deve davvero consumare quota?** La proposta dice di sì, perché l'app continua a sorvegliarlo,
a tenerne lo storico e a permettergli di rientrare. C'è però un effetto sgradevole: un cliente con molti sospesi
si trova al tetto senza avere clienti paganti in più. Il rimedio esiste già ed è la cessazione, che li toglie di
mezzo. Se lo sviluppatore preferisse escluderli, va deciso **prima** dello scaffolding, perché cambia il
significato del tetto pubblicato nel listino. Chiude: lo sviluppatore, insieme al listino.
