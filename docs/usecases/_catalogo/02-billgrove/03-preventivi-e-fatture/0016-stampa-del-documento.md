# 0016 — Stampa del documento

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 03 — Preventivi e fatture
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che manda i documenti ai clienti
> voglio una versione stampabile del documento, pulita e con il mio marchio
> così da mandare qualcosa che assomigli alla mia attività e non a un tabulato, perché il documento è anche il modo
> in cui mi presento.

**Contesto.** La ricerca lo colloca fra le aspettative di base: il documento «che si vede bene ed è riconoscibile»
(§2.5 della descrizione). Non è estetica fine a sé stessa — la fattura è spesso l'unico documento aziendale che il
cliente conserva. Va dopo emissione e imposte perché stampa ciò che quelle due producono, e prima della forma
canonica perché è la rappresentazione che l'utente vede e verifica.

## 2. Requisiti funzionali

1. **RF-1** — Ogni documento ha una rappresentazione stampabile in formato di documento portatile, scaricabile e
   ristampabile.
2. **RF-2** — L'account configura una volta i propri dati di intestazione: denominazione, indirizzo, partita IVA,
   contatti, logo, e il piè di pagina con le coordinate per il pagamento.
3. **RF-3** — La stampa riporta tutto ciò che il tipo di documento richiede: numero, data, dati del cliente
   **congelati**, righe, riepilogo per aliquota, imposta di bollo, totali, scadenza.
4. **RF-4** — La stampa di un documento emesso è **stabile**: ristampandolo a distanza di un anno si ottiene lo
   stesso contenuto, anche se nel frattempo l'anagrafica del cliente o i dati dell'attività sono cambiati.
5. **RF-5** — Su una bozza la stampa riporta una filigrana che dice chiaramente che non è un documento emesso.
6. **RF-6** — La generazione avviene lato servizio, non nel navigatore, così che il documento sia identico ovunque.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La stampa si genera solo per documenti del `tenant_id` del token
  verificato; chiedere la stampa di un documento altrui risponde `404`.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/billing/v1/documents/{id}/print` che restituisce il
  documento portatile; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_billing` per i dati di intestazione dell'account e per
  la **copia congelata** dell'intestazione sul documento emesso, con `tenant_id` e colonne di controllo. Il file
  generato **non** si conserva: si rigenera dai dati congelati, che sono la vera fonte.
- **RT-4 — Modulo frontend (§3, §5).** Anteprima e scarico dalla scheda del documento; sezione «Impostazioni» per
  l'intestazione e il logo. Solo token del sistema di design nell'interfaccia; il **documento stampato** ha una
  propria impaginazione, sobria, che non dipende dal tema.
- **RT-5 — Cinque lingue (§4).** Le etichette dell'interfaccia passano dallo spazio-nomi `billing` in tutte e cinque
  le lingue. La lingua **del documento stampato** è una cosa diversa e la tratta la storia `0023`: qui si stampa
  nella lingua dell'interfaccia dell'utente.
- **RT-6 — Varchi e quota (§6).** La stampa e la ristampa **non consumano quota**: la quota è sull'emissione. Una
  ristampa che consumasse quota sarebbe una trappola.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento proprio: `leggi_documento` (epica 06) restituisce
  i dati, non il file. Restituire file binari alla chat è fuori dal contratto di questa stesura e va dichiarato.
- **RT-8 — Dati personali (§10).** La copia congelata dell'intestazione contiene i dati dell'**attività**, non di
  terzi; la stampa espone dati del cliente già dichiarati. Nessuna voce nuova, ma il file generato è un'uscita di
  dati personali: va detto nel manifesto che non viene conservato.
- **RT-9 — Registrazione eventi (§14).** L'evento `documento stampato` è registrato con `tenant_id`, `app_id`,
  `user_id`, identificativo di correlazione e identificativo del documento, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Stampa di un documento emesso**
- **Dato** una fattura emessa con due aliquote e imposta di bollo
- **Quando** si chiede la stampa
- **Allora** si ottiene un documento portatile con numero, data, dati congelati del cliente, righe, riepilogo per
  aliquota, bollo e totali

**CA-2 — Stabilità nel tempo**
- **Dato** una fattura emessa e poi un cambio di denominazione del cliente e del logo dell'attività
- **Quando** si ristampa la fattura
- **Allora** il documento riporta i dati di allora, non quelli attuali

**CA-3 — Bozza**
- **Dato** un documento in `bozza` · **Quando** si chiede la stampa
- **Allora** il documento riporta una filigrana che dichiara che non è un documento emesso

**CA-4 — Documento di un altro account**
- **Dato** una fattura dell'account `B` · **Quando** un utente di `A` ne chiede la stampa
- **Allora** riceve `404`

**CA-5 — Nessun consumo di quota**
- **Dato** un account che ha esaurito la quota `documenti` · **Quando** ristampa una fattura già emessa
- **Allora** la stampa funziona

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul contenuto generato (presenza dei campi obbligatori per tipo di documento) e di
      **integrazione** sulla rotta di stampa, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla stampa di un documento altrui;
- [ ] **prova end-to-end**: *coprire ora* — passo «scarica la fattura» del percorso `[J-BILLING]`, che verifica che
      il file arrivi e non sia vuoto; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la nota che il file generato non viene conservato;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di rigenerare invece di conservare;
- [ ] contratto degli **strumenti conversazionali**: nessuno proprio, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | Si stampano documenti emessi, con i dati congelati |
| storia `0013` | Il riepilogo per aliquota e il bollo devono comparire nella stampa |

## 7. Fuori ambito

- l'invio del documento al cliente: storia `0025`;
- la lingua del documento diversa da quella dell'interfaccia: storia `0023`;
- più modelli di stampa fra cui scegliere: rimandato. Un modello solo, fatto bene, copre il segmento; la scelta fra
  modelli è una funzione da piano alto che nessuna delle fonti indica come discriminante;
- il formato canonico per la trasmissione: storia `0024` — è un'altra cosa, e confonderle è un errore comune.

## 8. Punti aperti

Nessuno.
