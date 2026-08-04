# 0017 — Autorizzazione all'addebito

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che incassa gli abbonamenti con l'addebito diretto della mia banca
> voglio sapere per quali iscritti ho un'autorizzazione valida e per quali no
> così da non scoprire a fine mese che venti addebiti sono tornati indietro perché il mandato era decaduto.

**Contesto.** SubGrove **non incassa** (§5.2 della descrizione): l'addebito lo dispone il cliente con la propria
banca o il proprio fornitore. Ma l'app deve sapere **se il permesso esiste**, perché è la differenza fra un
rinnovo che si incassa da solo e uno che va rincorso a mano. Da qui una regola stretta e non negoziabile: si
conserva il **riferimento** dell'autorizzazione — l'identificativo che la banca o il fornitore ha rilasciato — e
mai la coordinata bancaria, mai il numero di carta, in nessuna forma, nemmeno mascherata. Conservare la
coordinata non servirebbe a nulla (non possiamo usarla) e ci farebbe custodire un dato che non ci compete.

C'è poi una regola di schema che nessuno ricorda finché non morde: un mandato di addebito diretto **decade** dopo
36 mesi senza alcun incasso (§2.3 della descrizione). Un cliente con abbonamenti annuali e una sospensione lunga
ci arriva davvero.

## 2. Requisiti funzionali

1. **RF-1** — Un abbonato può avere una o più autorizzazioni all'addebito, ciascuna con: **tipo** (mandato di
   addebito diretto / autorizzazione presso il fornitore del cliente / nessuna, cioè pagamento manuale),
   **riferimento esterno**, data di firma, stato (`valida`, `revocata`, `decaduta`), data di ultimo utilizzo.
2. **RF-2** — L'app **non** accetta, in alcun campo, coordinate bancarie complete o numeri di carta: la
   validazione li rifiuta se riconosciuti, e l'aiuto a schermo spiega perché.
3. **RF-3** — Un'autorizzazione di tipo mandato non usata da 36 mesi passa in automatico a `decaduta`, e
   l'abbonamento collegato compare in un elenco di **autorizzazioni da rinnovare**.
4. **RF-4** — L'abbonamento dichiara come si incassa: con quale autorizzazione, oppure «pagamento manuale».
   L'elenco degli abbonamenti si filtra su questo.
5. **RF-5** — Revocare un'autorizzazione non tocca né l'abbonamento né le scadenze: cambia solo il modo in cui
   si incasserà, e l'app avvisa che d'ora in poi l'incasso è manuale.
6. **RF-6** — Ogni schermata che tratta di autorizzazioni dichiara esplicitamente che **l'app non dispone
   addebiti**: registra soltanto se il permesso esiste.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Autorizzazioni filtrate per `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `GET|POST /api/abbonati/v1/abbonati/{id}/autorizzazioni` e `PATCH .../autorizzazioni/{idAut}`; errori in
  `problem+json` con codice stabile per «dato bancario non ammesso»; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V12__autorizzazione_addebito.sql`: tabella
  `autorizzazione_addebito` con `tenant_id`, chiave UUID versione 7, colonne di controllo, cancellazione logica.
  **Nessuna colonna** può contenere una coordinata bancaria: non esiste il campo, quindi non si può riempirlo per
  sbaglio.
- **RT-4 — Nessun movimento di denaro.** Nessuna integrazione con un fornitore di pagamento in questa storia,
  nessun dato di carta, nessun numero di conto: se in futuro si vorrà **leggere** gli esiti da un fornitore, è la
  storia `0020`, che è separata e con fermata di escalation.
- **RT-5 — Modulo frontend (§3, §5).** Nella scheda dell'abbonato, un riquadro «come si incassa» con lo stato
  dell'autorizzazione e l'avvertenza sul perimetro; elenco «autorizzazioni da rinnovare» nella panoramica; solo
  token del sistema di design.
- **RT-6 — Cinque lingue (§4).** Tipi, stati, avvertenze e messaggi di rifiuto in `en, it, fr, es, de`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura. La presenza di
  un'autorizzazione valida compare nella lettura `stato_abbonato` (storia `0031`), perché è ciò che spiega
  perché un incasso non è partito.
- **RT-8 — Dati personali (§10).** Il riferimento dell'autorizzazione è un dato riferito a una persona e a un
  suo rapporto bancario: voce nuova nel manifesto in italiano e inglese, campo annotato, tabella in `exportData`
  e `purgeData`. Nel manifesto va dichiarata anche l'**esclusione**: coordinate e numeri di carta non sono
  trattati.
- **RT-9 — Registrazione eventi (§14).** `autorizzazione registrata`, `autorizzazione revocata`,
  `autorizzazione decaduta`, con `tenant_id`, `app_id`, `user_id` e correlazione, **senza** il riferimento
  esterno.

## 4. Criteri di accettazione

**CA-1 — Registrazione del permesso**
- **Dato** un abbonato che ha firmato un mandato in banca
- **Quando** l'addetta registra tipo, riferimento e data di firma
- **Allora** l'abbonamento risulta «incasso con mandato», e la scheda dice che l'app non dispone addebiti

**CA-2 — Dato bancario rifiutato**
- **Dato** un utente che incolla una coordinata bancaria nel campo del riferimento
- **Quando** salva
- **Allora** riceve un rifiuto con spiegazione, e nulla viene salvato

**CA-3 — Decadenza a 36 mesi**
- **Dato** un mandato con ultimo utilizzo di 36 mesi fa · **Quando** gira il controllo periodico
- **Allora** l'autorizzazione passa a `decaduta` e l'abbonamento compare fra quelle da rinnovare

**CA-4 — Revoca senza danni**
- **Dato** un abbonamento con mandato valido e due scadenze in attesa
- **Quando** l'autorizzazione viene revocata
- **Allora** le scadenze restano, l'abbonamento passa a «pagamento manuale» e l'utente ne è avvisato

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** uno legge le autorizzazioni · **Allora** vede solo le proprie

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sul rifiuto dei dati bancari e sul calcolo della decadenza; **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: *rimando* — voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml), storia proprietaria `0033`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con l'autorizzazione **e con le esclusioni dichiarate**;
- [ ] **registro delle decisioni** compilato: **solo il riferimento, mai la coordinata**, e perché; decadenza a
      36 mesi come regola di schema;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | l'autorizzazione appartiene a un abbonato |
| storia `0010` | l'abbonamento dichiara con quale autorizzazione si incassa |

## 7. Fuori ambito

- **disporre** un addebito: è avvio di un pagamento, servizio regolato, fuori dal perimetro dichiarato (§5.2
  della descrizione);
- la firma del mandato dentro l'app: la firma avviene in banca o presso il fornitore, non qui;
- la lettura automatica degli esiti dal fornitore: storia `0020`.

## 8. Punti aperti

**Il conteggio dei 36 mesi è affidabile solo se l'app sa quando c'è stato l'ultimo incasso.** Con la
registrazione manuale (storia `0018`) lo sa per sentito dire: se l'addetta non segna, la data non si aggiorna e
un mandato vivo risulta decaduto. Non è un difetto grave — un falso allarme è meglio di un addebito respinto — ma
va detto a schermo. Chiude: la storia `0018`, che deve aggiornare la data d'uso a ogni incasso registrato.

**Le regole citate valgono per l'addebito diretto europeo.** Un cliente che incassa con altri schemi ha regole
diverse, che non ho verificato. Il modello le regge (il tipo è un dato), ma i valori predefiniti no. Chiude: lo
sviluppatore, se e quando si allarga oltre l'area europea.
