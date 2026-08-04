# 0031 — Contratto degli strumenti di lettura

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0016`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vive dentro una chat e non vuole aprire l'ennesima applicazione
> voglio chiedere all'assistente i miei numeri e ottenere le **stesse** risposte che vedrei nell'app, con la stessa
> ricevuta
> così da non dovermi chiedere quale delle due versioni è quella giusta.

**Contesto.** Il livello conversazionale di piattaforma **non esiste ancora** nel repository (epica
`12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata): questa storia non lo costruisce, **dichiara il
contratto** degli strumenti e lo tiene dentro il servizio, versionato con esso
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §12). Per InsightGrove c'è però una ragione in più, e
sta al §7 della [descrizione](../application-description.md): il copilota interno (epica 05) e l'assistente
esterno devono essere **due clienti dello stesso contratto**, non due motori. Se divergessero, esisterebbero due
modi di ottenere lo stesso numero — cioè esattamente il difetto che questa applicazione esiste per non avere.
Il copilota interno diventa quindi il **primo consumatore** degli strumenti dichiarati qui.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara **cinque** strumenti di sola lettura, con nome stabile, descrizione in lingua
   naturale, schema dei parametri, schema del risultato e marcatura `lettura`:
   `elenca_metriche`, `interroga_metrica`, `spiega_numero`, `stato_delle_fonti`, `spiega_scostamento`.
2. **RF-2** — `elenca_metriche(classe?, fonte?)` restituisce il catalogo delle metriche **pubblicate** con
   versione, unità, dimensioni ammesse, periodi rappresentabili, fonti richieste e classe di riservatezza. È il
   primo strumento che un assistente deve chiamare, e la sua descrizione lo dice: **non si interroga una metrica
   che non si è elencata**.
3. **RF-3** — `interroga_metrica(metrica, periodo, dimensioni?, filtri?, confronto?)` esegue un **piano validato**
   contro il catalogo (storia 0022) e restituisce valore, unità, `tipo_valore` (`rilevato` o `previsto`, storia
   0030), grado di completezza e **riferimento alla traccia**. Un piano non validabile produce un **rifiuto
   motivato**, mai un numero: non esiste alcun parametro che permetta di scrivere un'interrogazione libera.
4. **RF-4** — `spiega_numero(riferimento alla traccia)` restituisce il contenuto della scheda del numero (storia
   0016): metrica e versione, periodo, fonti con i conteggi, momento dell'ultimo dato, completezza, piano
   eseguito e fino a dieci rimandi alla riga d'origine.
5. **RF-5** — Ogni risultato che contenga un valore porta **sempre** il riferimento alla traccia e il grado di
   completezza: sono campi obbligatori dello schema, non facoltativi. Un assistente che voglia rispondere senza
   ricevuta deve buttarli via di proposito.
6. **RF-6** — Il contratto è **versionato con il servizio** e pubblicato insieme alla definizione OpenAPI; un
   cambiamento che toglie un campo o cambia il significato di un parametro è un cambiamento di versione dello
   strumento, non una modifica silenziosa.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni strumento riceve il `tenant_id` **solo** dal gettone verificato
  della chiamata; nessuno schema di parametri contiene, né può contenere, un identificativo di account. Un
  parametro del genere in una revisione futura è un difetto bloccante.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte esistenti
  `/api/insights/v1/*` — non esiste un secondo percorso di calcolo dedicato alla chat: **la stessa funzione
  applicativa**, con lo stesso codice di validazione e gli stessi errori in `application/problem+json`.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. Ogni esecuzione di `interroga_metrica` produce una
  `traccia_del_calcolo` come qualunque altro calcolo (storia 0016).
- **RT-5 — Cinque lingue (§4).** Le **descrizioni** degli strumenti e i messaggi di rifiuto esistono in
  `en, it, fr, es, de`: l'assistente parla la lingua dell'utente, e un rifiuto in una lingua sola è un rifiuto
  incomprensibile.
- **RT-6 — Varchi e quota (§6, §7).** Ogni chiamata attraversa la catena completa dei varchi: `401`, `403` app
  spenta, `402` account non abilitato, `403` ruolo o classe di riservatezza, `429` quota. Il dettaglio del
  consumo e della matrice dei ruoli è della storia 0033.
- **RT-7 — Esposizione conversazionale (§12).** Il contratto vive dentro il servizio e ne segue il ciclo di vita;
  il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063). La mappatura
  operazioni → strumenti seguirà lo schema di UC 0063 quando esisterà: qui si dichiara ciò che l'app espone.
- **RT-8 — Dati personali (§10).** I risultati possono contenere **etichette di dimensione** (nomi di clienti,
  via A del §6.1 della descrizione): sono già dichiarate nel manifesto. Nessun risultato contiene il testo di una
  domanda posta da una persona, né dati di dettaglio delle app sorgenti.
- **RT-14 — Registrazione eventi (§14).** «Strumento invocato», «strumento rifiutato» con `tenant_id`, `app_id`,
  `user_id`, nome dello strumento, esito e identificativo di correlazione; **mai** i parametri in chiaro, perché
  un filtro può contenere un nome.

## 4. Criteri di accettazione

**CA-1 — Prima si elenca, poi si chiede**
- **Dato** un assistente collegato all'account
- **Quando** chiama `elenca_metriche(classe: "operativa")`
- **Allora** riceve solo le metriche pubblicate di classe operativa, con versione, unità, dimensioni ammesse e
  fonti richieste

**CA-2 — Il numero arriva con la ricevuta**
- **Dato** la metrica `fatturato_emesso` pubblicata alla versione 3
- **Quando** l'assistente chiama `interroga_metrica(metrica: "fatturato_emesso", periodo: "2026-07")`
- **Allora** riceve valore, unità, `tipo_valore = rilevato`, completezza `completo` e un riferimento alla traccia
  che, passato a `spiega_numero`, restituisce fonti, conteggi e rimandi

**CA-3 — Fuori catalogo si rifiuta**
- **Dato** una richiesta su una metrica che non esiste o su una dimensione non ammessa
- **Quando** l'assistente chiama `interroga_metrica`
- **Allora** riceve un rifiuto motivato che dice **che cosa** non è validabile e propone le alternative del
  catalogo; **nessun numero** viene restituito

**CA-4 — Stima dichiarata**
- **Dato** una richiesta che chiede esplicitamente una proiezione
- **Quando** viene eseguita
- **Allora** il risultato porta `tipo_valore = previsto` con metodo e intervallo; una richiesta che non le chiede
  non riceve mai valori previsti

**CA-5 — Isolamento fra account**
- **Dato** due account con la stessa metrica
- **Quando** un assistente autorizzato su `A` invoca uno strumento aggiungendo di sua iniziativa un parametro con
  l'identificativo di `B`
- **Allora** il parametro è ignorato o rifiutato dalla validazione dello schema, e la risposta riguarda solo `A`

**CA-6 — Una sola verità**
- **Dato** la stessa domanda posta al copilota interno e all'assistente esterno nello stesso momento
- **Quando** entrambi eseguono
- **Allora** producono lo **stesso** valore e lo stesso riferimento alla traccia, e una prova lo verifica

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione degli schemi dei parametri e sui rifiuti, e di **integrazione** su
      ciascuno dei cinque strumenti contro il servizio vero;
- [ ] prova che copilota interno e strumento esterno **passano dallo stesso codice** e restituiscono lo stesso
      risultato;
- [ ] prova di **isolamento fra account** su ogni strumento;
- [ ] **prova end-to-end**: *rimando* alla storia 0034, che possiede il percorso `[J-INSIGHTS]`; voce
      `da-coprire` nel registro di copertura con motivo «livello conversazionale di piattaforma non implementato
      (UC 0061-0063)»;
- [ ] **traduzioni** delle descrizioni e dei rifiuti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato: nessun dato personale nuovo esposto dagli strumenti;
- [ ] **registro delle decisioni** compilato, con l'elenco dei cinque strumenti, l'obbligatorietà di traccia e
      completezza nello schema del risultato e il divieto di interrogazioni libere;
- [ ] contratto degli **strumenti conversazionali** dichiarato e versionato con il servizio;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | `elenca_metriche` espone il catalogo delle metriche pubblicate |
| storia `0016` | `spiega_numero` restituisce esattamente la scheda del numero |
| storia `0022` | `interroga_metrica` esegue il piano validato costruito lì |
| epica di piattaforma non implementata (UC 0061-0063) | il server conversazionale non esiste: qui si dichiara il contratto e lo si collauda dal copilota interno |

## 7. Fuori ambito

- gli strumenti che **scrivono**: storia 0032;
- l'applicazione del ruolo e della quota alle chiamate: storia 0033;
- la costruzione del server conversazionale, l'autenticazione delegata e il consenso: sono di piattaforma
  (UC 0061-0062);
- la registrazione delle azioni degli assistenti a fini di governance: è l'app 31 AuditGrove (§10 della
  descrizione).

## 8. Punti aperti

- **Quanti rimandi restituire a un assistente?** Dieci sono giusti per una schermata; in una chat sono molto
  testo. Proposta: **tre in modo predefinito, dieci su richiesta esplicita**. Chiude: **sviluppatore**.
- **Il contratto degli strumenti è per-app o di piattaforma?** UC 0063 lo definirà; finché non esiste, questa
  storia lo tiene dentro il servizio ed è la scelta prudente. Chiude: **piattaforma**.
