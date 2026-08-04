# 0033 — Ruolo e quota sulle chiamate dell'assistente

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0033` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0025`, `0026`, `0031`, `0032`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha dato al commerciale l'accesso a InsightGrove ma non ai margini
> voglio che quel limite valga **anche** quando chiede i numeri da una chat
> così da non scoprire che la porta chiusa nell'app aveva una finestra aperta sul retro.

**Contesto.** È la storia che impedisce alla chat di diventare una **porta di servizio**. La catena dei varchi
della piattaforma — autenticato, app accesa, account abilitato, ruolo sufficiente, quota non esaurita — è scritta
per le richieste che arrivano dalle schermate; qui si dichiara che vale **identica** per le chiamate degli
strumenti (UC 0064, non ancora implementato). Per InsightGrove il punto è più acuto che altrove, per due motivi:
la classe di riservatezza delle metriche (storia 0014) è un controllo **dell'app**, non della piattaforma, e
l'unica metrica di quota è la domanda al copilota — cioè proprio ciò che un assistente esterno fa tutto il giorno.
Se le chiamate non consumassero, il piano gratuito diventerebbe illimitato per chiunque colleghi una chat.

## 2. Requisiti funzionali

1. **RF-1** — Ogni chiamata di strumento attraversa la **stessa** catena di varchi delle richieste dell'interfaccia
   e nello stesso ordine: `401` non autenticato, `403` app spenta, `402` account non abilitato, `403` ruolo o
   classe di riservatezza insufficiente, `429` quota esaurita.
2. **RF-2** — La **classe di riservatezza** si applica al risultato: `elenca_metriche` restituisce solo le
   metriche che chi chiede può vedere, e `interroga_metrica` su una metrica economica chiesta da un `member`
   riceve un **rifiuto motivato** — non un numero calcolato senza quel pezzo (regola 3 del §4.3 della
   [descrizione](../application-description.md), storia 0025).
3. **RF-3** — Consuma **una** unità della metrica `questions` ogni chiamata che **esegue un piano**:
   `interroga_metrica` e `spiega_scostamento` con riassunto. **Non** consumano `elenca_metriche`, `spiega_numero`,
   `stato_delle_fonti` e la creazione di una bozza: sono letture di metadati o preparazioni, e farle pagare
   spingerebbe l'assistente a saltare proprio i passi che rendono verificabile la risposta.
4. **RF-4** — Il consumo dal canale conversazionale confluisce **nello stesso contatore** delle domande poste
   nell'app: un solo tetto, una sola finestra, un solo posto dove guardarlo. Il registro delle domande (storia
   0026) marca il **canale** di provenienza (`app` o `assistente`).
5. **RF-5** — A quota esaurita la chiamata riceve `429` con un messaggio comprensibile **da un assistente**: che
   cosa è successo, che cosa non si può più fare, come si rimedia. Le chiamate che non consumano continuano a
   funzionare: si può ancora sapere quali metriche esistono e spiegare un numero già ottenuto.
6. **RF-6** — Un rifiuto per ruolo o riservatezza **non rivela** ciò che nasconde: dice che la metrica esiste e
   non è accessibile con quel ruolo, mai il suo valore, e mai una stima «indicativa».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` viene dal gettone verificato della chiamata; una chiamata
  senza gettone valido non arriva alla logica applicativa.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova: gli strumenti passano dalle rotte esistenti
  (storia 0031) e quindi dallo stesso strato di varchi. Non esiste, e non deve poter esistere, un percorso di
  calcolo che salti la catena.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: la colonna `canale` si aggiunge alla tabella `domanda`
  (storia 0022) con migrazione `V<N>__canale_della_domanda.sql`.
- **RT-4 — Modulo frontend (§3, §5).** Il registro delle domande mostra il canale e il consumo per canale; solo
  token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I messaggi di rifiuto e di quota esaurita esistono in `en, it, fr, es, de` e
  seguono la lingua dichiarata dalla chiamata.
- **RT-6 — Varchi e quota (§6, §7).** Prenotazione di una unità di `questions` (natura `flow`) **prima**
  dell'esecuzione del piano; a quota esaurita `429` e nulla viene eseguito. Con abbonamento `past_due` gli
  strumenti restano accessibili; con `canceled` rispondono `402`.
- **RT-7 — Esposizione conversazionale (§12).** L'applicazione di abilitazione e quota alle chiamate
  dell'assistente è UC 0064, non implementato: questa storia **dichiara il comportamento atteso** e lo collauda
  chiamando il contratto direttamente. Quando il server esisterà, il collaudo va rifatto passando da lui — e va
  scritto nel registro delle decisioni.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. La colonna `canale` non è un dato personale, ma
  vive nella tabella `domanda`, che lo è: resta in `exportData` e `purgeData`.
- **RT-14 — Registrazione eventi (§14).** «Chiamata di strumento consumata», «chiamata rifiutata per ruolo»,
  «chiamata rifiutata per quota» con `tenant_id`, `app_id`, `user_id`, strumento, canale ed esito; **mai** i
  parametri, che possono contenere un nome.

## 4. Criteri di accettazione

**CA-1 — Il limite di ruolo vale anche in chat**
- **Dato** un utente `member` e la metrica economica «margine»
- **Quando** il suo assistente chiama `interroga_metrica` su quella metrica
- **Allora** riceve un rifiuto motivato con `403`; il valore non compare, nemmeno arrotondato o parziale

**CA-2 — Il catalogo si restringe da solo**
- **Dato** lo stesso utente `member`
- **Quando** l'assistente chiama `elenca_metriche`
- **Allora** riceve solo le metriche operative: quelle economiche non compaiono nell'elenco

**CA-3 — Un solo contatore**
- **Dato** un account sul piano `pro` con 298 domande consumate dall'app
- **Quando** l'assistente esegue due `interroga_metrica`
- **Allora** il contatore arriva a 300 e la terza chiamata riceve `429`; il registro mostra le due domande con
  canale `assistente`

**CA-4 — Le letture di metadati non consumano**
- **Dato** un account a quota esaurita
- **Quando** l'assistente chiama `elenca_metriche`, `spiega_numero` su una traccia esistente e
  `stato_delle_fonti`
- **Allora** tutte e tre rispondono normalmente, e il contatore non cambia

**CA-5 — Abbonamento scaduto**
- **Dato** un account con abbonamento `canceled`
- **Quando** l'assistente invoca qualunque strumento
- **Allora** riceve `402`; l'esportazione dei dati personali dell'interessato resta comunque accessibile
  (storia 0035)

**CA-6 — Isolamento fra account**
- **Dato** un assistente autorizzato sull'account `A`
- **Quando** invoca uno strumento mentre esiste un account `B` con gli stessi nomi di metrica
- **Allora** ottiene esclusivamente numeri di `A`, e una prova lo verifica su entrambi

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla decisione di consumo (quali strumenti consumano e quali no) e di **integrazione**
      sulla catena dei varchi applicata a ogni strumento;
- [ ] **matrice dei ruoli** provata su tutti gli strumenti, compreso il caso «metrica economica chiesta da
      `member`»;
- [ ] prova di **isolamento fra account** su ogni strumento;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura con motivo
      «applicazione di quota e ruolo alle chiamate dell'assistente: UC 0064 non implementato»;
- [ ] **traduzioni** dei messaggi di rifiuto in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con l'elenco degli strumenti che consumano, il contatore unico e la
      nota che il collaudo andrà rifatto quando il server conversazionale esisterà;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento nuovo, ma il contratto dichiara per
      ciascuno se consuma quota;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` | la classe di riservatezza è il controllo che qui si estende alla chat |
| storia `0025` | la regola «rifiuto, non numero ridotto» è definita lì e qui vale identica |
| storia `0026` | il contatore, il registro e la regola «una domanda, una unità» esistono già |
| storia `0031` | gli strumenti di lettura da proteggere |
| storia `0032` | gli strumenti di scrittura: la creazione della bozza non consuma, l'azione confermata segue le regole dell'app |
| UC 0064 (non implementato) | l'applicazione dei varchi alle chiamate dell'assistente è di piattaforma |

## 7. Fuori ambito

- il consenso delegato e l'identità con cui l'assistente agisce: sono di piattaforma (UC 0062);
- la registrazione a fini di governance di chi ha chiesto cosa: è l'app 31 AuditGrove;
- un tetto **separato** per il canale conversazionale: si veda il punto aperto;
- la tariffazione a consumo: vietata dalla piattaforma, si blocca e non si addebita (§2.2 della descrizione).

## 8. Punti aperti

- **Contare le chiamate dell'assistente sulla stessa metrica è giusto?** Va detto con onestà: quando la domanda
  arriva da un assistente esterno, **il costo del modello non è nostro** — noi eseguiamo un piano deterministico.
  Il conteggio serve quindi all'equità del listino e alla protezione del servizio, non a coprire un costo. È una
  scelta di prodotto, e il piano `business` del §5 della descrizione esiste proprio per questo consumo più alto.
  Chiude: **sviluppatore**.
- **Un tetto separato per canale** (per esempio 300 dall'app e 300 dalla chat) sarebbe più leggibile o più
  confuso? Raccomandazione: **un solo tetto**, perché due contatori raddoppiano le domande dell'assistenza.
  Chiude: **sviluppatore**.
- **Il ruolo `member` è la granularità sbagliata** per «vede il fatturato ma non i margini»: è il punto aperto di
  piattaforma numero 3 della descrizione (§4.4). Qui si applica ciò che c'è, senza inventare un secondo modello di
  autorizzazione. Chiude: **piattaforma**.
