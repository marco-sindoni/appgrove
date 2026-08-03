# 0035 — Strumenti di scrittura con bozza e conferma

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0035` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0034`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore che esce da un incontro
> voglio dettare «crea un lead per Alfa Utensili, richiamare martedì» e vedermelo proposto prima che venga scritto
> così da riempire l'archivio in dieci secondi senza rischiare che l'assistente scriva qualcosa che non volevo.

**Contesto.** È la storia che affronta il rischio dell'archivio vuoto — il primo rischio di prodotto dell'app
([application-description.md](../application-description.md) §11) — e insieme il vincolo di sicurezza del catalogo
(§8): gli strumenti di scrittura producono una **bozza** e richiedono una **conferma umana esplicita**. L'assistente
prepara, la persona approva. Due dei cinque strumenti hanno effetti che escono dall'app e per quelli la conferma
non è disattivabile in nessun caso.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara cinque strumenti di **scrittura**: `create_lead`, `log_activity`,
   `update_deal_stage`, `close_deal`, `export_contacts`.
2. **RF-2** — Ogni strumento di scrittura restituisce una **bozza**: una descrizione in lingua naturale di cosa
   verrà fatto, i dati esatti che verranno scritti e un identificativo di conferma con scadenza breve. Nulla viene
   scritto in questa fase.
3. **RF-3** — La scrittura avviene solo con una seconda chiamata che porta l'identificativo di conferma, ed è
   **idempotente**: la stessa conferma inviata due volte scrive una volta sola.
4. **RF-4** — `close_deal` e `export_contacts` hanno conferma **obbligatoria e non disattivabile**, perché hanno
   effetti fuori dall'app: il primo emette un evento verso le altre app della suite, il secondo fa uscire dati
   personali in massa.
5. **RF-5** — La bozza di `create_lead` dice se esiste già un contatto simile, con la stessa logica della storia
   0026: proporre un doppione è il modo più facile di sporcare l'archivio.
6. **RF-6** — La bozza di `export_contacts` riporta lo stesso avviso della storia 0027 (dati di persone,
   responsabilità di chi custodisce, base giuridica, Registro pubblico delle opposizioni per le telefonate in
   Italia) e il numero di righe che uscirebbero.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Come per le letture, nessuno schema contiene un parametro di account;
  bozza e conferma appartengono all'account del chiamante e una conferma non è utilizzabile da un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti riusano i servizi applicativi esistenti, con le
  stesse validazioni delle rotte; la bozza si conserva per il tempo della sua scadenza e non oltre.
- **RT-3 — Persistenza (§8).** Tabella `tool_draft` sullo schema `app_sales`, con migrazione
  `V<N>__tool_draft.sql`: `tenant_id`, autore, strumento, contenuto proposto, scadenza, stato. Le bozze scadute si
  rimuovono; una bozza confermata non si riusa.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata propria; le scritture confermate compaiono nelle
  schermate esistenti e nella cronologia (storia 0022) marcate come **provenienti dall'assistente**, perché chi
  legge deve sapere chi ha scritto.
- **RT-5 — Cinque lingue (§4).** Le descrizioni delle bozze le compone il livello conversazionale nella lingua
  della conversazione; il servizio restituisce **dati strutturati**, non frasi già scritte — altrimenti sarebbero
  traducibili solo in una lingua.
- **RT-6 — Varchi e quota (§6, §7).** Ogni strumento di scrittura attraversa l'intera catena dei varchi:
  `401`, `403` app spenta, `402` senza abbonamento, `403` senza posto o ruolo, `429` se la scrittura consumasse
  quota. La conferma **ricontrolla** i varchi: fra bozza e conferma l'abbonamento potrebbe essere cambiato.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che realizza la regola di sicurezza del catalogo.
  Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** `tool_draft.payload` contiene i dati proposti, che possono riguardare persone:
  va **dichiarato nel manifesto** in italiano e inglese, annotato `@PersonalData` e aggiunto a `exportData` e
  `purgeData`. È il tipo di tabella che si dimentica perché «è temporanea»: le bozze scadute si cancellano, ma
  finché esistono contengono dati di persone.
- **RT-9 — Registrazione eventi (§14).** «Bozza prodotta», «bozza confermata», «bozza scaduta» con nome dello
  strumento, autore e identificativo di correlazione; **mai** il contenuto proposto.

## 4. Criteri di accettazione

**CA-1 — Nulla si scrive senza conferma**
- **Dato** una chiamata a `create_lead` con nome e azienda
- **Quando** lo strumento risponde
- **Allora** restituisce una bozza con i dati proposti e **nessun** contatto risulta creato

**CA-2 — Conferma che scrive**
- **Dato** una bozza valida
- **Quando** arriva la conferma con il suo identificativo
- **Allora** il contatto viene creato con origine che indica l'assistente, e la cronologia lo mostra come tale

**CA-3 — Doppia conferma**
- **Dato** la stessa conferma inviata due volte
- **Quando** arriva la seconda
- **Allora** l'esito è identico e nulla viene creato una seconda volta

**CA-4 — Bozza scaduta**
- **Dato** una bozza oltre la sua scadenza
- **Quando** si tenta di confermarla
- **Allora** la conferma è rifiutata e va prodotta una bozza nuova

**CA-5 — Conferma obbligatoria sugli effetti esterni**
- **Dato** una chiamata a `close_deal` o `export_contacts` con qualunque parametro che tenti di saltare la conferma
- **Quando** arriva al servizio
- **Allora** la scrittura non avviene: la conferma non è disattivabile

**CA-6 — Varchi ricontrollati alla conferma**
- **Dato** una bozza prodotta mentre l'abbonamento era attivo
- **Quando** la conferma arriva dopo che l'abbonamento è passato a `canceled`
- **Allora** la conferma riceve `402` e nulla viene scritto

**CA-7 — Isolamento fra account**
- **Dato** una bozza prodotta nell'account `A`
- **Quando** un utente di `B` tenta di confermarla
- **Allora** riceve un esito «non trovato» e nulla viene scritto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sul ciclo bozza-conferma-scadenza e di **integrazione** su ciascuno dei cinque strumenti;
- [ ] prova di **isolamento fra account** su bozze e conferme;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-SALES]` (storia 0037) comprende un passo di creazione
      via bozza e conferma, per provare che la scrittura dall'assistente funziona davvero; voce nel registro di
      copertura;
- [ ] **traduzioni**: nessun testo visibile nuovo; verificato che il servizio restituisca dati strutturati e non
      frasi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `tool_draft`, campo annotato, tabella in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato quali strumenti hanno conferma non disattivabile e
      perché;
- [ ] contratto degli **strumenti conversazionali** completato con le cinque scritture;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0034` | Il contratto degli strumenti nasce lì |
| Storie `0013`, `0016`, `0019`, `0027` | Sono le funzioni che gli strumenti richiamano |
| UC 0061-0063 (livello conversazionale di piattaforma) | Non implementati: il ciclo bozza-conferma si prova, ma la chat vera non c'è ancora |

## 7. Fuori ambito

- il modo in cui l'assistente **presenta** la bozza all'utente: è del livello conversazionale;
- la scrittura di note dalla chat: punto aperto della storia 0021;
- l'assegnazione dei posti, la fusione dei duplicati, l'importazione e la configurazione dei moduli web: escluse
  per scelta nelle rispettive storie, con la motivazione scritta.

## 8. Punti aperti

- **Durata della bozza** — proposta breve (dell'ordine dei minuti). Una bozza che vive troppo è una scrittura
  differita che nessuno ricorda di aver chiesto. Il valore esatto lo conferma lo sviluppatore.
