# 0032 — Strumenti di scrittura con bozza e conferma

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0032` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0012`, `0019`, `0028`, `0031`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che chiede all'assistente «avvisami se i crediti scaduti superano diecimila euro»
> voglio che l'assistente **prepari** l'avviso e me lo faccia leggere prima che esista
> così da non scoprire per caso che qualcuno riceve messaggi che non ho mai approvato.

**Contesto.** La regola di piattaforma è netta: gli strumenti di lettura sono liberi, quelli di **scrittura con
effetti irreversibili producono una bozza e richiedono una conferma umana esplicita** — l'intelligenza artificiale
prepara, la persona approva ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §12). In InsightGrove le
azioni di scrittura sono quattro e nessuna è innocua, per ragioni diverse fra loro: due mandano messaggi a
persone, una **cambia il significato di un numero per tutti e retroattivamente**, una **cancella fisicamente**
dati già ricevuti (§7 della [descrizione](../application-description.md)). È il motivo per cui la conferma qui non
è una formalità: è il punto in cui una persona si assume una responsabilità che nessun modello può assumersi.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara **quattro** strumenti di scrittura, tutti a conferma umana obbligatoria:
   `crea_avviso`, `programma_rapporto`, `pubblica_metrica`, `collega_fonte` / `scollega_fonte`.
2. **RF-2** — Ogni chiamata produce una **bozza**, mai un effetto: la bozza ha un identificativo, una scadenza
   (proposta: 15 minuti), l'elenco di ciò che accadrà alla conferma e chi l'ha proposta. Nessuno strumento di
   scrittura ha un parametro «conferma subito».
3. **RF-3** — La bozza mostra le **conseguenze in numeri**, non a parole: `scollega_fonte` dichiara quanti fatti
   e quante etichette verranno cancellati e quali metriche smetteranno di produrre valori (storia 0008);
   `pubblica_metrica` dichiara quali cruscotti, avvisi e rapporti useranno la nuova definizione e da quando;
   `crea_avviso` e `programma_rapporto` dichiarano **chi riceverà messaggi**.
4. **RF-4** — La conferma è un atto **dell'utente**, non dell'assistente: avviene nell'applicazione (o su una
   superficie di conferma di piattaforma), da una persona con il ruolo sufficiente, e resta tracciata con autore,
   momento e identificativo della bozza. Una bozza scaduta o già confermata non si può confermare di nuovo.
5. **RF-5** — Le stesse regole valgono **identiche** al copilota interno: una richiesta di scrittura fatta nella
   chat dentro l'app produce la stessa bozza con la stessa conferma. Non esiste una via più corta perché si è
   «dentro casa».
6. **RF-6** — Ogni strumento di scrittura è **idempotente sulla conferma**: confermare due volte la stessa bozza
   produce un solo effetto, e la seconda conferma risponde dicendo che l'effetto c'è già.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Bozza e conferma portano il `tenant_id` del gettone verificato; una
  bozza di un altro account non è confermabile nemmeno conoscendone l'identificativo (`404`).
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/insights/v1/bozze` (crea la bozza da uno
  strumento) e `POST /api/insights/v1/bozze/{id}/conferma`; errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit. La conferma riusa le rotte applicative già esistenti: **nessuna
  scrittura ha un secondo percorso** riservato alla chat.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__bozze_di_azione.sql` sullo schema `app_insights`: tabella
  `bozza_di_azione` con `tenant_id`, strumento, parametri validati, conseguenze calcolate, stato (proposta,
  confermata, scaduta, annullata), scadenza, autore della proposta e della conferma; chiave primaria UUID
  versione 7, colonne di controllo, cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Schermata di conferma con le conseguenze in evidenza e il pulsante
  distruttivo distinto; per `scollega_fonte` la conferma richiede di **rileggere il conteggio** di ciò che verrà
  cancellato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Testi delle bozze, delle conseguenze e delle conferme in `en, it, fr, es, de`.
  Una conferma che l'utente non capisce non è una conferma.
- **RT-6 — Varchi e quota (§6, §7).** La creazione di una bozza attraversa la catena dei varchi e **non consuma**
  la metrica `questions`; il ruolo richiesto è quello dell'azione confermata, non della proposta: proporre non è
  potere.
- **RT-7 — Esposizione conversazionale (§12).** Quattro strumenti marcati **scrittura**, tutti con conferma umana;
  `scollega_fonte` è marcata **irreversibile**. Il server conversazionale è di piattaforma e non ancora
  implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** `crea_avviso` e `programma_rapporto` scrivono **destinatari** (indirizzi di
  posta elettronica), già dichiarati nel manifesto dalle storie 0019 e 0028. La tabella `bozza_di_azione`
  contiene quei parametri finché la bozza vive: entra in `exportData` e `purgeData` ed è cancellata alla scadenza.
- **RT-14 — Registrazione eventi (§14).** «Bozza proposta», «bozza confermata», «bozza scaduta», «conferma
  rifiutata per ruolo» con `tenant_id`, `app_id`, `user_id`, strumento e identificativo della bozza; **mai** i
  destinatari in chiaro.

## 4. Criteri di accettazione

**CA-1 — La bozza non esegue**
- **Dato** un assistente che chiama `crea_avviso(metrica: "crediti_scaduti", condizione: "sopra", soglia: 10000,
  destinatari: [una persona dell'account])`
- **Quando** la chiamata viene eseguita
- **Allora** nasce una bozza con scadenza e conseguenze, **nessun avviso esiste**, e nessun messaggio parte

**CA-2 — Conseguenze in numeri**
- **Dato** un assistente che chiama `scollega_fonte(app: "fatturazione")` su un account con 12.480 fatti e 214
  etichette da quella fonte
- **Quando** la bozza viene mostrata
- **Allora** dichiara «verranno cancellati 12.480 fatti e 214 etichette; tre metriche smetteranno di produrre
  valori», e la conferma richiede la rilettura di quel conteggio

**CA-3 — Conferma solo umana e con il ruolo giusto**
- **Dato** una bozza di `pubblica_metrica` proposta da un `member`
- **Quando** il `member` prova a confermarla
- **Allora** riceve `403`; la stessa bozza confermata da un `owner` produce la nuova versione della definizione,
  con autore e momento tracciati

**CA-4 — Scadenza**
- **Dato** una bozza creata 20 minuti fa
- **Quando** qualcuno la conferma
- **Allora** riceve un errore che dice che la bozza è scaduta e va rifatta; **nulla accade**

**CA-5 — Idempotenza**
- **Dato** una bozza già confermata
- **Quando** arriva una seconda conferma con lo stesso identificativo
- **Allora** l'effetto resta uno solo e la risposta dice che l'azione è già stata eseguita

**CA-6 — Isolamento fra account**
- **Dato** una bozza dell'account `B`
- **Quando** un utente di `A` prova a confermarla con il suo identificativo
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo delle conseguenze (conteggi di `scollega_fonte`, elenco degli usi di
      `pubblica_metrica`) e di **integrazione** sul ciclo bozza → conferma → effetto, compresi scadenza e
      doppia conferma;
- [ ] prova che **nessuno** dei quattro strumenti produce un effetto senza conferma — è la prova che vale più di
      tutte le altre di questa storia;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** su bozza e conferma;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura con motivo
      «livello conversazionale di piattaforma non implementato (UC 0061-0063)»;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per `bozza_di_azione`, con conservazione limitata alla scadenza;
- [ ] **registro delle decisioni** compilato, con i 15 minuti di scadenza, «proporre non è potere» e
      l'idempotenza della conferma;
- [ ] contratto degli **strumenti conversazionali**: quattro strumenti di scrittura dichiarati, con
      `scollega_fonte` marcata irreversibile;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | `collega_fonte` / `scollega_fonte` confermano un'azione già definita lì, revoca distruttiva compresa |
| storia `0012` | `pubblica_metrica` pubblica una nuova versione di definizione |
| storia `0019` | `crea_avviso` scrive un avviso con destinatari |
| storia `0028` | `programma_rapporto` scrive un invio ricorrente |
| storia `0031` | gli strumenti di scrittura vivono nello stesso contratto di quelli di lettura |

## 7. Fuori ambito

- l'esportazione di una tavola richiesta da un assistente: produrre un file e mandarlo fuori è un effetto verso
  l'esterno che questa proposta non concede alla chat; l'esportazione resta un gesto dell'utente (storia 0027);
- la cancellazione dei dati personali su richiesta dell'interessato: non è mai uno strumento conversazionale,
  è un diritto esercitato dalla piattaforma (storia 0035);
- la superficie di conferma del server conversazionale: è di piattaforma (UC 0062);
- la modifica di un avviso o di un rapporto esistente da chat: si crea una bozza nuova, non si modifica al volo.

## 8. Punti aperti

- **Quindici minuti di validità della bozza** sono un compromesso fra «faccio in tempo a leggerla» e «non mi
  ritrovo domani una conferma di ieri». Non è un dato rilevato. Chiude: **sviluppatore**.
- **`pubblica_metrica` dovrebbe essere esposta agli assistenti?** Cambia il significato di un numero per tutti e
  retroattivamente: è l'azione più pericolosa dell'app. Raccomandazione: **esposta, ma con conferma da `owner` e
  con la spiegazione dell'impatto**; se lo sviluppatore preferisce toglierla del tutto dalla chat, è una
  posizione difendibile. Chiude: **sviluppatore**.
- **Chi può confermare una bozza proposta da un assistente che agisce per conto di un'altra persona?** Dipende
  dal modello di consenso delegato di UC 0062, che non esiste. Nel frattempo: **conferma solo dalla stessa
  persona per conto della quale l'assistente agisce**. Chiude: **piattaforma**.
