# 0010 — Sconti e soglie di approvazione

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 02 — Anagrafica, catalogo e listini
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che lascia preparare i preventivi ad altri
> voglio che gli sconti oltre una certa soglia debbano passare da me
> così da non scoprire a lavoro finito che l'offerta è stata chiusa sotto il margine.

**Contesto.** Il modello utente dell'app è `multi` proprio per questo: quando le persone che offrono sono più di
una, lo sconto è il punto in cui l'azienda perde soldi senza accorgersene. La ricerca di mercato dice che i flussi
di approvazione **a più livelli** sono rifiutati dal segmento (§2.5): qui se ne fa uno solo, a un livello, che si
può spegnere.

## 2. Requisiti funzionali

1. **RF-1** — Si applica uno sconto **di riga** (percentuale o importo) e uno sconto **di documento**, e il
   documento mostra sempre entrambi separatamente.
2. **RF-2** — L'account configura una **soglia di approvazione**: oltre quella percentuale di sconto complessivo
   il preventivo non si può inviare finché non lo approva chi ha il ruolo per farlo.
3. **RF-3** — L'approvazione è un atto tracciato: chi, quando, su quale versione, con un commento facoltativo.
4. **RF-4** — Se il preventivo cambia dopo l'approvazione, l'approvazione decade e va richiesta di nuovo.
5. **RF-5** — La soglia si può disattivare del tutto: un artigiano che lavora da solo non deve approvare se stesso.
6. **RF-6** — L'interfaccia mostra a chi prepara l'offerta il **margine** rispetto al prezzo di listino, non solo
   lo sconto: è il numero che serve per decidere.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Regole di sconto, soglie e approvazioni filtrano per `tenant_id` dal
  token verificato.
- **RT-2 — Ruoli (§6).** L'approvazione richiede ruolo `owner` o `admin`; a un `member` risponde `403`. È il
  quarto varco della catena e va provato con la matrice dei ruoli.
- **RT-3 — Interfaccia di programmazione (§2).** `POST /api/preventivi/v1/preventivi/{id}/approvazione` e le
  impostazioni sotto `/api/preventivi/v1/impostazioni/sconti`; errori in `problem+json`; OpenAPI aggiornata.
- **RT-4 — Persistenza (§8).** Migrazione `V5__sconti_approvazioni.sql`: colonne di sconto su preventivo e riga,
  tabella `approvazione` con `tenant_id`, UUID versione 7, colonne di controllo, cancellazione logica.
- **RT-5 — Modulo frontend (§3, §5).** Nella schermata del preventivo: sconto, margine, stato dell'approvazione,
  pulsante per richiederla; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Tutte le stringhe visibili in `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** Nessun campo di persona nuovo: chi approva è un utente dell'account, già
  coperto dalle colonne di controllo di piattaforma.
- **RT-8 — Registrazione eventi (§14).** `approvazione richiesta`, `approvazione concessa`, `approvazione
  decaduta`, con `tenant_id`, `app_id`, `user_id` e correlazione.

## 4. Criteri di accettazione

**CA-1 — Sotto soglia si invia**
- **Dato** una soglia al 15 % e un preventivo scontato del 10 % · **Quando** si chiede di inviarlo · **Allora**
  l'invio procede senza approvazione

**CA-2 — Sopra soglia si blocca**
- **Dato** la stessa soglia e uno sconto del 22 % · **Quando** si chiede di inviarlo · **Allora** l'app rifiuta
  con un messaggio che dice chi può approvare, e nulla viene inviato

**CA-3 — Il ruolo conta**
- **Dato** un utente con ruolo `member` · **Quando** tenta di approvare · **Allora** riceve `403` e
  l'approvazione non risulta concessa

**CA-4 — L'approvazione decade**
- **Dato** un preventivo approvato · **Quando** qualcuno ne cambia una riga · **Allora** lo stato torna «da
  approvare» e l'evento è registrato

**CA-5 — Soglia disattivata**
- **Dato** un account con la soglia spenta · **Quando** si invia un preventivo scontato del 40 % · **Allora**
  l'invio procede e nessuna approvazione è richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sul calcolo dello sconto complessivo e sul decadimento, di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sull'approvazione;
- [ ] **prova end-to-end**: rimando alla storia `0029`, che percorre il caso «sopra soglia» come variante;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato (un solo livello di approvazione, decadimento alla modifica,
      possibilità di spegnere la soglia);
- [ ] avvio locale invariato; dati di prova estesi con un preventivo sopra soglia.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` | il margine si misura rispetto al prezzo di listino |

## 7. Fuori ambito

- approvazioni a più livelli o per catena gerarchica: rifiutate dal segmento;
- sconti automatici per fedeltà o volume annuo: rimandati, nessuna evidenza di richiesta.

## 8. Punti aperti

Nessuno.
