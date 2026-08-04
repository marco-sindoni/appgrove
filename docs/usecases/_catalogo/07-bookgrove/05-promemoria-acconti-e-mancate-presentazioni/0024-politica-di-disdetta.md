# 0024 — Politica di disdetta

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 05 — Promemoria, acconti e mancate presentazioni
**Storia**: `0024` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio dire chiaramente entro quando si può disdire senza conseguenze, e farlo accettare prima della conferma
> così da poter far valere la regola quando serve, invece di litigare al telefono.

**Contesto.** La regola giuridica rilevata è precisa e non intuitiva (§2.3, punto 2 della descrizione): un
appuntamento fissato a una data determinata **non** dà al consumatore i quattordici giorni di ripensamento
previsti per i contratti a distanza. Ma perché una politica di disdetta sia opponibile a chi ha prenotato,
dev'essere stata **mostrata e accettata prima** della conferma. Questa storia mette la politica dove serve — sulla
pagina pubblica, prima del pulsante — e la conserva insieme alla prenotazione, perché una regola cambiata dopo non
vale per chi ha prenotato prima.

## 2. Requisiti funzionali

1. **RF-1** — L'attività definisce la propria politica: finestra entro cui si disdice liberamente (per esempio 24
   ore prima), cosa succede oltre quella finestra, e un testo libero che spiega le condizioni.
2. **RF-2** — La politica è mostrata sulla pagina pubblica **prima** della conferma, con un atto di accettazione
   distinto dagli altri consensi.
3. **RF-3** — La versione della politica accettata viene **conservata insieme alla prenotazione**: una modifica
   successiva non si applica a chi ha già prenotato.
4. **RF-4** — Il collegamento di gestione (storia `0018`) rispetta la finestra: dentro la finestra la disdetta è
   libera, fuori il cliente vede cosa comporta prima di confermare.
5. **RF-5** — La politica è disponibile nelle cinque lingue: il testo scritto dall'attività resta nella sua
   lingua, l'impalcatura intorno è tradotta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura della politica filtra per `tenant_id` preso
  dal token verificato; sulla pagina pubblica arriva dall'identificativo di sede.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|PUT /api/prenotazioni/v1/politiche-disdetta`; la
  politica in vigore è esposta anche sulla rotta pubblica della storia `0016`; errori in `problem+json`; OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V16__politica_disdetta.sql`: tabella `politica_disdetta` con
  `tenant_id`, UUID versione 7, colonne di controllo e **versionamento**; la prenotazione porta il riferimento
  alla versione accettata e il momento dell'accettazione.
- **RT-4 — Modulo frontend (§3, §5).** Impostazione della politica con anteprima di come la vedrà il cliente; sulla
  pagina pubblica, testo visibile senza doverlo aprire; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'impalcatura e i messaggi in `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** Nessuna voce nuova di dato personale, ma **sì** una voce sul momento e la
  versione dell'accettazione, che riguarda una persona identificata: si dichiara nel manifesto in italiano e
  inglese e si aggiunge alle tabelle esportate e cancellate.
- **RT-7 — Registrazione eventi (§14).** `politica aggiornata`, `politica accettata` con `tenant_id`, `app_id`,
  versione e correlazione — mai l'identità di chi ha accettato.

## 4. Criteri di accettazione

**CA-1 — Accettazione prima della conferma**
- **Dato** una pagina pubblica con politica attiva · **Quando** un visitatore prenota · **Allora** non può
  confermare senza aver accettato la politica, e l'accettazione è registrata con la versione

**CA-2 — La politica non cambia il passato**
- **Dato** una prenotazione fatta con la versione 1 · **Quando** l'attività pubblica la versione 2
- **Allora** quella prenotazione resta legata alla versione 1, e il cliente continua a vedere quella

**CA-3 — Dentro la finestra**
- **Dato** una finestra di 24 ore e un appuntamento fra tre giorni · **Quando** il cliente disdice
- **Allora** la disdetta è libera e il messaggio lo conferma

**CA-4 — Fuori dalla finestra**
- **Dato** un appuntamento fra due ore · **Quando** il cliente apre il collegamento e chiede di disdire
- **Allora** vede cosa comporta secondo la politica accettata e deve confermare esplicitamente

**CA-5 — Isolamento fra account**
- **Dato** due account con politiche diverse · **Quando** si legge la pagina pubblica di uno · **Allora** si vede
  solo la sua

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sul calcolo della finestra e di **integrazione** sul versionamento;
- [ ] prova di **isolamento fra account** sulla politica;
- [ ] **prova end-to-end**: **coperta ora** — l'accettazione è un passo del percorso `[J-BOOKGROVE-PUB]` della
      storia `0034`, con il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con il momento e la versione dell'accettazione;
- [ ] **registro delle decisioni** compilato: versionamento della politica e conservazione della versione
      accettata, con la ragione giuridica;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | la politica si accetta al momento della prenotazione |
| storia `0018` | la disdetta del cliente deve rispettarla |

## 7. Fuori ambito

- la somma trattenuta in caso di disdetta tardiva: storia `0025`, che tratta l'acconto e la sua natura;
- la validazione legale dei testi scritti dall'attività: **non è compito nostro**, l'app li ospita e non li
  scrive.

## 8. Punti aperti

**Modelli di politica preconfezionati.** Sarebbe comodo offrire due o tre testi pronti. Ma un testo pronto è un
testo giuridico che avremmo scritto noi per conto del cliente, con la responsabilità che ne consegue. Proposta:
**non** fornirli, e limitarsi alla struttura (finestra e conseguenza) più un testo che scrive l'attività. Da
confermare dallo sviluppatore, eventualmente con il supporto legale.
