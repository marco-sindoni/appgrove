# 0027 — Esportazione di una tavola

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 06 — Rapporti, esportazione e previsioni
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0016`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve mandare dei numeri a qualcuno che non entrerà mai in InsightGrove — il commercialista,
> la banca, il socio
> voglio scaricare la tavola che sto guardando in un file che si apre con qualunque foglio di calcolo, con scritto
> in testa da dove vengono quei numeri
> così da poterli consegnare senza doverli rispiegare a voce.

**Contesto.** L'esportazione in foglio di calcolo è la richiesta numero uno di tutta la categoria, e il pubblico
esterno vuole un documento **statico**, non un accesso a un cruscotto vivo (§2.4 e §2.5 della
[descrizione](../application-description.md), fonte 9). È anche la via **sanzionata** in alternativa al
collegamento pubblico verso l'esterno, che questa proposta tiene fuori ambito (§11, punto 4). Il punto delicato
non è produrre il file: è che un numero staccato dalla sua schermata **perde la ricevuta**. Un foglio con dentro
solo cifre è esattamente il documento che, tre settimane dopo, nessuno sa più come è stato ottenuto: per questo la
provenienza viaggia **dentro** il file, non accanto.

## 2. Requisiti funzionali

1. **RF-1** — Da qualunque riquadro del cruscotto, tavola o risposta del copilota si esporta la tavola
   sottostante in due formati: `.csv` (separato da virgole, con codifica dichiarata) e foglio di calcolo `.xlsx`.
   Nessun altro formato in questa storia.
2. **RF-2** — Ogni file porta **in testa** un blocco di provenienza con le stesse voci della scheda del numero
   (storia 0016): metrica e **versione della definizione**, periodo e calendario, fonti che hanno concorso con il
   conteggio dei fatti per ciascuna, momento del fatto più recente, **grado di completezza**, momento
   dell'esportazione, chi l'ha prodotta, e la frase «numeri di InsightGrove, senza valore contabile».
3. **RF-3** — Ogni riga di dati porta una colonna **`tipo_valore`** con due soli valori: `rilevato` (calcolato su
   fatti osservati) o `previsto` (proiezione, storia 0030). Le righe `previsto` portano anche metodo e intervallo;
   una tavola che mescolasse i due senza distinguerli è il difetto che questa colonna esiste per impedire.
4. **RF-4** — L'esportazione rispetta la **classe di riservatezza** delle metriche: se la tavola contiene una
   metrica che il ruolo di chi esporta non può vedere, l'esportazione **si rifiuta** con un messaggio che dice
   quale metrica lo impedisce. Non produce mai un file «ridotto» con quella colonna tolta in silenzio.
5. **RF-5** — Ogni riga porta il proprio **riferimento d'origine** in forma opaca (app sorgente, tipo di entità,
   identificativo) più, per il formato foglio di calcolo, il collegamento che apre l'app sorgente sulla riga
   (storia 0011). Chi riceve il file senza accesso alla suite vede il riferimento e non il contenuto.
6. **RF-6** — Sopra le 20.000 righe l'esportazione diventa **differita**: la richiesta risponde subito, il file si
   prepara e l'utente lo trova nella sezione Esportazioni, disponibile per sette giorni e poi cancellato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La tavola si compone leggendo solo dati filtrati per `tenant_id` preso
  dal gettone verificato; un identificativo di cruscotto o di traccia di un altro account restituisce `404`.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/insights/v1/esportazioni` (crea la richiesta) e
  `GET /api/insights/v1/esportazioni/{id}` (stato e ritiro del file); errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__esportazioni.sql` sullo schema `app_insights`: tabella
  `esportazione` con `tenant_id`, piano esportato, formato, stato, riferimento alla traccia, numero di righe,
  scadenza del file; chiave primaria UUID versione 7, colonne di controllo, cancellazione logica. Il **file** non
  sta nel database: sta nell'archivio a oggetti della piattaforma, con scadenza a sette giorni.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Esportazioni` del modulo `insights` più il gesto «esporta»
  presente su riquadri, tavole e risposte; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le stringhe dell'interfaccia **e le intestazioni del file** passano dallo
  spazio-nomi `insights` e sono presenti in `en, it, fr, es, de`: il file esce nella lingua di chi lo produce, e
  la lingua usata è scritta nel blocco di provenienza.
- **RT-6 — Varchi e quota (§6, §7).** L'esportazione **non consuma** la metrica `questions`: non interpella il
  modello, è un calcolo deterministico. Con abbonamento `past_due` resta accessibile; con `canceled` risponde
  `402`. L'**esportazione dei dati personali dell'interessato** è un'altra cosa e resta accessibile sempre
  (storia 0035).
- **RT-8 — Dati personali (§10).** Il file può contenere **etichette di dimensione** (nomi di clienti) se è stata
  scelta la via (A) del §6.1 della descrizione: va detto nel blocco di provenienza, e la tabella `esportazione`
  entra in `exportData` e `purgeData` per le colonne `created_by`. Nessun altro dato personale nuovo.
- **RT-14 — Registrazione eventi (§14).** «Esportazione richiesta», «esportazione pronta», «esportazione
  rifiutata per riservatezza», «file scaduto e cancellato» con `tenant_id`, `app_id`, `user_id` e identificativo
  di correlazione; **mai** il contenuto della tavola né le etichette.

## 4. Criteri di accettazione

**CA-1 — Il file porta la sua ricevuta**
- **Dato** un riquadro «fatturato emesso — luglio — 42.300 €», completo
- **Quando** l'utente esporta in foglio di calcolo
- **Allora** il file si apre con un blocco di provenienza che riporta metrica `fatturato_emesso` versione 3,
  periodo 1-31 luglio, fonti fatturazione (118 fatti) e note di credito (4 fatti), ultimo dato delle 06:15,
  completezza `completo`, e la frase sul valore non contabile

**CA-2 — Rilevato e previsto non si confondono**
- **Dato** una tavola che contiene sei mesi osservati e due mesi di proiezione
- **Quando** viene esportata
- **Allora** le prime sei righe hanno `tipo_valore = rilevato` e le ultime due `tipo_valore = previsto` con
  metodo e intervallo compilati; nessuna riga ha la colonna vuota

**CA-3 — Riservatezza: rifiuto, non riduzione**
- **Dato** un utente `member` e una tavola che contiene la metrica economica «margine»
- **Quando** prova a esportarla
- **Allora** riceve `403` e il messaggio dice quale metrica lo impedisce; **nessun file viene prodotto**, nemmeno
  senza quella colonna

**CA-4 — Incompletezza dichiarata**
- **Dato** una fonte richiesta silente da sei giorni
- **Quando** si esporta la tavola
- **Allora** il blocco di provenienza riporta completezza `parziale` con la fonte che tace e da quando, e le righe
  interessate portano il contrassegno di incompletezza in una colonna dedicata

**CA-5 — Esportazione differita**
- **Dato** una tavola da 63.000 righe
- **Quando** l'utente la esporta
- **Allora** riceve subito conferma della presa in carico, trova il file nella sezione Esportazioni quando è
  pronto, e dopo sette giorni il file non è più scaricabile

**CA-6 — Isolamento fra account**
- **Dato** una esportazione dell'account `B`
- **Quando** un utente di `A` prova a ritirarla conoscendone l'identificativo
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione del blocco di provenienza e sulla colonna `tipo_valore`, e di
      **integrazione** sulla risorsa delle esportazioni con file davvero prodotto e riletto;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sull'esportazione, compreso il rifiuto per
      classe di riservatezza;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include «esporta la tavola e verifica che
      il file porti la provenienza»; registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese le intestazioni del file;
- [ ] **manifesto dei dati** verificato: le etichette di dimensione nel file sono già dichiarate; la tabella
      `esportazione` è in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato, con la soglia delle 20.000 righe, i sette giorni di conservazione
      del file e il rifiuto (anziché la riduzione) per riservatezza;
- [ ] contratto degli **strumenti conversazionali**: l'esportazione **non** è uno strumento di questa storia —
      un assistente che produce file verso l'esterno è materia della storia 0032;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` | la classe di riservatezza decide che cosa si può esportare |
| storia `0016` | il blocco di provenienza è la scheda del numero messa in testa al file |
| storia `0017` | si esporta ciò che un riquadro mostra |

## 7. Fuori ambito

- l'invio automatico del file a qualcuno: è il rapporto periodico, storia 0028;
- il documento impaginato in formato `.pdf`: lo produce il rapporto periodico, che ha un'impaginazione propria;
- il collegamento pubblico a un cruscotto vivo: **fuori ambito dichiarato** (§11, punto 4 della descrizione);
- l'esportazione dei dati personali richiesta dall'interessato: è un altro diritto e un'altra storia, la 0035.

## 8. Punti aperti

- **Sette giorni di conservazione del file prodotto** sono una scelta di prudenza (un file di numeri economici che
  resta scaricabile per sempre è una superficie inutile), non un dato rilevato. Chiude: **sviluppatore**.
- **Il formato `.xlsx` vale la dipendenza da una libreria di scrittura?** Il `.csv` copre il caso d'uso, l'`.xlsx`
  copre l'aspettativa. Raccomandazione: **entrambi**, perché il destinatario tipico è un commercialista e il
  `.csv` aperto male produce colonne sbagliate — che è il modo più stupido di perdere fiducia. Chiude:
  **sviluppatore**.
