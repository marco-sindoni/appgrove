# 0004 — Abbonamento e quota

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha tre poltrone e un piano che ne prevede tre
> voglio che il programma mi dica chiaramente cosa succede se provo ad aprirne una quarta
> così da sapere cosa sto comprando prima di trovarmi bloccato in un giorno di lavoro.

**Contesto.** La quota di questa app è **a giacenza** sulla metrica `risorse_prenotabili`: il tetto vale su
quante risorse sono aperte alla prenotazione adesso, non su quante prenotazioni si prendono nel mese. La scelta è
argomentata nel varco d'identità (§3 della descrizione) e ha una conseguenza che questa storia deve rendere vera:
il rifiuto per quota esaurita capita **solo** a un utente autenticato che sta configurando l'attività, mai a un
cliente finale che sta prenotando. È anche la ragione per cui la storia sta nelle fondamenta e non dopo: se il
varco arriva tardi, tutte le funzioni intermedie nascono senza.

## 2. Requisiti funzionali

1. **RF-1** — L'accesso a ogni funzione dell'app attraversa la catena dei varchi: token valido, app non spenta
   dalla piattaforma, account abilitato, ruolo sufficiente, quota non esaurita.
2. **RF-2** — Attivare una risorsa come prenotabile consuma una unità della metrica `risorse_prenotabili`;
   disattivarla la restituisce.
3. **RF-3** — Al raggiungimento del tetto, l'attivazione di una risorsa in più risponde `429` con un messaggio che
   dice quante ne sono aperte, quante ne prevede il piano e come rimediare.
4. **RF-4** — Il passaggio a un piano inferiore è **bloccato** finché le risorse aperte superano il tetto del
   piano di destinazione, con un messaggio che dice quante disattivarne.
5. **RF-5** — Con abbonamento in `trialing`, `active` o `past_due` l'app funziona; con `paused` o `canceled`
   risponde `402`. L'esportazione dei dati e la cancellazione restano accessibili in ogni caso.
6. **RF-6** — L'abilitazione si legge dalla **proiezione locale** alimentata a eventi, mai con una chiamata di
   rete sincrona all'app centrale sul percorso caldo.

## 3. Requisiti tecnici

- **RT-1 — Varchi e quota (§6, §7).** Prima di attivare una risorsa il servizio prenota una unità della metrica
  `risorse_prenotabili` (natura `stock`); a quota esaurita risponde `429`. Con abbonamento non attivo risponde
  `402`. Il listino resta un file nel repository: la storia **non fissa prezzi**, consuma il tetto pubblicato.
- **RT-2 — Isolamento fra account (§1).** Il conteggio delle risorse aperte è per `tenant_id` preso dal token
  verificato; nessun conteggio attraversa gli account.
- **RT-3 — Interfaccia di programmazione (§2).** Errori in `problem+json` con codici stabili per «quota
  esaurita», «abbonamento non attivo», «ruolo insufficiente»; OpenAPI aggiornata.
- **RT-4 — Modulo frontend (§3, §5).** Un riquadro nelle impostazioni mostra risorse aperte su tetto del piano;
  quando manca poco lo dice prima, non dopo il rifiuto; solo token del sistema di design, tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutti i messaggi di rifiuto e l'indicatore di quota in `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-7 — Registrazione eventi (§14).** `risorsa attivata`, `attivazione respinta per quota`, `passaggio di
  piano bloccato` con `tenant_id`, `app_id`, `user_id` e correlazione, senza dati personali.
- **RT-8 — Prove (§11).** Prova di integrazione sulla catena dei varchi; matrice dei ruoli; prova con abbonamento
  in ciascuno stato.

## 4. Criteri di accettazione

**CA-1 — Quota rispettata**
- **Dato** un account sul piano con tre risorse e tre risorse aperte
- **Quando** un amministratore prova ad aprirne una quarta
- **Allora** riceve `429`, il messaggio dice «3 su 3, disattivane una o passa di piano», e nulla viene attivato

**CA-2 — Restituzione della quota**
- **Dato** lo stesso account · **Quando** disattiva una risorsa e ne apre un'altra · **Allora** l'operazione
  riesce e il conteggio resta tre

**CA-3 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled` · **Quando** apre l'app · **Allora** riceve `402`, ma
  l'esportazione dei propri dati resta accessibile

**CA-4 — Tolleranza sui pagamenti falliti**
- **Dato** un account in `past_due` · **Quando** usa l'agenda · **Allora** funziona tutto normalmente

**CA-5 — Isolamento del conteggio**
- **Dato** due account, uno al tetto e uno vuoto · **Quando** quello vuoto apre una risorsa · **Allora** riesce:
  il tetto dell'altro non lo riguarda

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sul conteggio a giacenza e di **integrazione** sulla catena dei varchi;
- [ ] prova di **isolamento fra account** sul conteggio della quota;
- [ ] **prova end-to-end**: *rimando* — il rifiuto per quota entra nel percorso `[J-BOOKGROVE]` della storia
      `0033`, con il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** dei messaggi in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato: natura a giacenza della metrica e conseguenze sulla superficie
      pubblica;
- [ ] avvio locale invariato, con fornitore di pagamento simulato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | serve la tabella `risorsa` su cui contare |
| decisione dello sviluppatore sul listino (§5 della descrizione) | i tetti dei piani vengono da lì |

## 7. Fuori ambito

- la gestione completa delle risorse (tipi, colori, servizi erogati): storia `0007`;
- l'acquisto e il cambio di piano dal catalogo app: è di piattaforma.

## 8. Punti aperti

**Cosa fa una risorsa disattivata.** Disattivare una risorsa che ha prenotazioni future è una richiesta legittima
(un operatore se ne va) ma lascia appuntamenti orfani. Proposta: la disattivazione restituisce la quota solo se
non ci sono prenotazioni future, altrimenti chiede prima di spostarle. Da confermare con la storia `0007`.
