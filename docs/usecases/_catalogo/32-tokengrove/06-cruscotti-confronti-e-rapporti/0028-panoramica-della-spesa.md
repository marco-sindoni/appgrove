# 0028 — Panoramica della spesa

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 06 — Cruscotti, confronti e rapporti
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0021`, `0024`
**Ultimo aggiornamento**: 2026-08-04

## 1. Narrazione

> Come titolare che apre TokenGrove una volta alla settimana
> voglio vedere in una sola schermata quanto ho speso, per cosa, e dove sto andando a finire
> così da capire in un minuto se devo preoccuparmi, senza dover sapere in anticipo quale filtro impostare.

**Contesto.** Fino a qui l'app sa misurare, prezzare, attribuire e avvisare, ma non ha una schermata che risponda
alle tre domande che l'indagine di settore trova sistematicamente senza risposta: **chi possiede il conto, perché è
salito, se sta rendendo** (§2.6, fonte 12 del documento capofila). Questa è la schermata dei primi sessanta secondi
— quella che il titolare guarda e da cui parte per tutto il resto. È anche il posto dove le due misure di
affidabilità costruite prima, la **copertura di attribuzione** (storia `0021`) e la **freschezza dei dati** (storia
`0012`), devono stare accanto ai numeri e non nascoste in un pannello: un totale senza il suo grado di fiducia è un
numero che il cliente userà male.

## 2. Requisiti funzionali

1. **RF-1** — La panoramica mostra, per il periodo scelto (predefinito: il mese in corso), il **totale speso in
   euro**, il confronto con lo stesso intervallo del periodo precedente in valore e in percentuale, e la
   **previsione di fine periodo** (storia `0024`).
2. **RF-2** — Sotto il totale c'è la scomposizione su **un asse per volta**, scelto dall'utente fra quelli
   dichiarati dall'account (modello, fornitore, squadra, progetto, cliente finale, funzionalità, ambiente): elenco
   ordinato per importo decrescente, con quota sul totale e variazione rispetto al periodo precedente.
3. **RF-3** — L'andamento nel tempo è un grafico a barre per giorno (o per mese, se il periodo è lungo), con la
   possibilità di scegliere un giorno e vedere la scomposizione di quel solo giorno.
4. **RF-4** — Tre indicatori di affidabilità sono sempre visibili accanto ai numeri, mai in una scheda separata:
   **copertura di attribuzione** dell'asse scelto, **freschezza** (l'istante dell'ultima misura per fonte e il
   ritardo osservato), **età del catalogo dei prezzi** con cui i costi sono stati calcolati.
5. **RF-5** — Da ogni elemento della panoramica si arriva in un clic al luogo dove si agisce: dal non attribuito
   alla schermata delle regole, da un budget in giallo alla scheda del budget, da una fonte ferma alla scheda della
   fonte.
6. **RF-6** — Se l'account non ha ancora nessuna misura, la panoramica non mostra zeri ma il **percorso di avvio**:
   collega una fonte in sola lettura, oppure manda la prima misura. Uno zero è indistinguibile da un guasto, e chi
   apre l'app il primo giorno deve capire di non aver sbagliato nulla.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni aggregazione filtra per `tenant_id` preso dal gettone verificato;
  un `tenant_id` che arrivasse dai parametri della richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/spesa_modelli/v1/panoramica` (totali, confronto,
  previsione, indicatori di affidabilità) e `GET /api/spesa_modelli/v1/spesa` (serie e scomposizione, con
  parametri di periodo, asse e filtro); errori in `problem+json`; definizione OpenAPI aggiornata nello stesso
  commit. Le due rotte sono di sola lettura e idempotenti.
- **RT-3 — Persistenza e prestazioni (§8).** Le aggregazioni si servono da una **sintesi giornaliera per asse**
  mantenuta in scrittura all'ingresso delle misure, non da una scansione della tabella delle misure a ogni
  apertura. La sintesi porta `tenant_id`, giorno, asse, valore, importo e numero di chiamate, con colonne di
  controllo; è **derivata** e ricostruibile, quindi un difetto della sintesi si ripara ricalcolandola, mai
  correggendo a mano.
- **RT-4 — Costi congelati (§ documento capofila, §4).** La panoramica **somma i costi congelati sulle righe**, non
  ricalcola nulla al momento della lettura: due aperture della stessa schermata a distanza di un mese, sullo stesso
  periodo chiuso, danno lo stesso numero.
- **RT-5 — Modulo frontend (§3, §5).** Sezione «Panoramica» del modulo `spesa_modelli`, prima voce del menù;
  grafico costruito con i componenti del sistema di design e i token di colore-categoria `teal`; l'ambra e il rosso
  restano riservati agli stati di budget. Solo token del sistema di design; tema chiaro e scuro; controllo
  automatico di accessibilità sulla schermata.
- **RT-6 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `spesa_modelli` e sono presenti in
  `en, it, fr, es, de`, compresi i formati di numero, valuta e data, che seguono la lingua scelta.
- **RT-7 — Varchi e quota (§6, §7).** La consultazione **non consuma** la metrica `misure_registrate`: non registra
  misure. Il periodo consultabile è limitato dallo **storico del piano** (30 giorni, 13 mesi, 25 mesi): oltre quel
  limite la risposta dichiara che il dato esiste ma il piano non lo copre, con l'indicazione del rimedio, e non
  restituisce un totale parziale spacciato per completo.
- **RT-8 — Esposizione conversazionale (§12).** La panoramica è la stessa cosa che gli strumenti `leggi_spesa` e
  `elenca_maggiori_consumatori` restituiscono in forma di tavola (storia `0032`), marcati **lettura**: schermata e
  strumento leggono dalla stessa sintesi, altrimenti chat e cruscotto darebbero due numeri diversi.
- **RT-9 — Dati personali (§10).** Nessun campo nuovo. La scomposizione può mostrare valori di etichetta che sono
  dati riferibili a persone (cliente finale, utente finale): la schermata li mostra a chi ha diritto di vederli e
  non li scrive nei registri applicativi.
- **RT-10 — Registrazione eventi (§14).** Evento «panoramica consultata» con `tenant_id`, `app_id`, `user_id`,
  periodo e asse, con identificativo di correlazione, **senza** importi né valori di etichetta.

## 4. Criteri di accettazione

**CA-1 — I primi sessanta secondi**
- **Dato** un account con un mese di misure su tre modelli e due clienti finali
- **Quando** apre la panoramica
- **Allora** vede il totale in euro del mese, la variazione rispetto al mese precedente, la previsione di fine mese
  e la scomposizione dell'asse scelto ordinata per importo

**CA-2 — L'affidabilità sta accanto al numero**
- **Dato** una copertura di attribuzione del 64%, una fonte ferma da sei ore e un catalogo prezzi vecchio di 40
  giorni
- **Quando** guarda la panoramica
- **Allora** i tre indicatori sono visibili nella stessa vista del totale, ciascuno con il rimando alla schermata
  dove si rimedia

**CA-3 — Il conto non cambia da solo**
- **Dato** un mese chiuso e un catalogo prezzi pubblicato dopo la chiusura di quel mese
- **Quando** riapre la panoramica su quel mese
- **Allora** il totale è identico a quello letto prima della pubblicazione del nuovo catalogo

**CA-4 — Account vuoto**
- **Dato** un account appena creato, senza fonti e senza misure
- **Quando** apre la panoramica
- **Allora** non vede zeri ma i due modi per cominciare, con il rimando al collegamento della fonte

**CA-5 — Oltre lo storico del piano**
- **Dato** un account sul piano con storico a 30 giorni
- **Quando** chiede il periodo «ultimi 12 mesi»
- **Allora** riceve i 30 giorni coperti e una dichiarazione esplicita che il resto esiste ma non è compreso nel
  piano, con l'indicazione del rimedio; nessun totale parziale è presentato come totale

**CA-6 — Isolamento fra account**
- **Dato** due account con spese diverse nello stesso periodo
- **Quando** un utente di `A` apre la panoramica forzando l'identificativo di `B` nei parametri
- **Allora** vede i propri numeri e nulla di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulle aggregazioni e sul confronto con il periodo precedente, e di **integrazione** sulle
      due rotte con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla panoramica e sulla scomposizione;
- [ ] prova che la sintesi giornaliera **ricostruita da zero** dà gli stessi numeri della sintesi mantenuta in
      scrittura;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «apro la panoramica, vedo
      totale, previsione e i tre indicatori di affidabilità», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con formati di numero, valuta e data localizzati;
- [ ] controllo automatico di **accessibilità** sulla panoramica;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sulla sintesi giornaliera derivata e sul rifiuto di
      ricalcolare i costi in lettura;
- [ ] contratto degli **strumenti conversazionali** allineato: la stessa sintesi serve schermata e strumenti;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0014` | Servono i costi congelati sulle righe: la panoramica somma, non calcola |
| Storia `0021` | La copertura di attribuzione è uno dei tre indicatori di affidabilità |
| Storia `0024` | La previsione di fine periodo è uno dei numeri della prima riga |

## 7. Fuori ambito

- il **confronto fra modelli** («quanto costerebbe su un altro modello»): è la storia `0029`;
- l'**esportazione** della tavola: è la storia `0030`;
- l'invio periodico del riepilogo a chi non apre l'app: è la storia `0031`;
- cruscotti componibili dall'utente (scelta e disposizione dei riquadri): rimandati, perché nessuno li ha chiesti e
  perché una panoramica che si può disfare è una panoramica che smette di rispondere alle tre domande.

## 8. Punti aperti

- **Se il confronto predefinito debba essere con il periodo precedente o con lo stesso periodo dell'anno scorso.**
  Il secondo è più significativo per chi ha stagionalità, ma richiede tredici mesi di storico e quindi funziona solo
  dal piano intermedio in su. Proposta: periodo precedente come predefinito, stesso periodo dell'anno scorso come
  seconda scelta quando lo storico del piano lo consente. La conferma lo sviluppatore.
