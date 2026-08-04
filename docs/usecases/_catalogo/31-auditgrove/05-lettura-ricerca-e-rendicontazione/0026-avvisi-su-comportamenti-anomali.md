# 0026 — Avvisi su comportamenti anomali

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 05 — Lettura, ricerca e rendicontazione
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0011`, `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio essere avvisato quando qualcosa esce dall'ordinario, con la spiegazione di che cosa è scattato
> così da non dover guardare la cronologia tutti i giorni per accorgermi di un guaio già successo.

**Contesto.** Le storie 0024 e 0025 servono chi **cerca**; questa serve chi **non sta guardando**, che è la
condizione normale del cliente di questa app: un'azienda di dieci persone non ha nessuno il cui mestiere sia
sorvegliare gli agenti (§1 della descrizione dell'applicazione). Senza avvisi, il registro si scopre utile solo
dopo il danno.

La scelta di progetto che governa la storia viene dall'analisi in rete (§2.5 della descrizione): **in nessuna
fonte consultata i clienti chiedono punteggi di rischio predittivi o rilevamento automatico di intenti malevoli**.
Chiedono di sapere che cosa è successo. Perciò gli avvisi qui sono **regole semplici, dichiarate e spiegabili** —
una condizione che chiunque può leggere e capire perché è scattata — e non un modello che assegna un punteggio.
Un avviso che non sa dire perché è scattato è rumore, e il rumore si disattiva.

## 2. Requisiti funzionali

1. **RF-1** — L'app valuta a intervalli regolari sei condizioni dichiarate: **volume fuori scala** rispetto alla
   media recente della sorgente; **strumento mai visto prima**; **azione senza nulla osta** quando la regola ne
   prevedeva uno; **raffica di rifiuti** su una sorgente o uno strumento; **sorgente silenziosa** da più tempo
   della sua cadenza abituale; **buchi di sequenza** rilevati (storia 0011).
2. **RF-2** — Ogni avviso porta: la condizione che è scattata, **perché** è scattata (i numeri che l'hanno fatta
   scattare, per esempio «142 azioni nell'ultima ora contro una media di 9»), il momento, l'oggetto (sorgente,
   strumento o intervallo) e un rimando alla cronologia già filtrata su ciò che va guardato.
3. **RF-3** — Le soglie delle condizioni sono modificabili per account e per sorgente, con valori predefiniti
   ragionevoli; una condizione si può disattivare del tutto.
4. **RF-4** — Un avviso ricorrente si può **sospendere** con un motivo scritto e una scadenza; la sospensione è
   visibile, non silenziosa, e scaduta la sospensione l'avviso torna attivo.
5. **RF-5** — Gli avvisi si recapitano in applicazione (elenco degli avvisi aperti con contatore nella barra
   laterale) e per posta elettronica ai destinatari scelti dall'account, con raggruppamento per non trasformare
   una raffica in cento messaggi.
6. **RF-6** — Ogni avviso emesso è **una riga del registro**: che l'app abbia avvisato, e quando, fa parte della
   storia dei fatti tanto quanto l'azione che l'ha provocato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La valutazione delle condizioni gira **per account** e legge solo dati
  dell'account: nessuna soglia, nessuna media e nessun confronto attraversa il confine fra due account. La lettura
  degli avvisi filtra per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/agentaudit/v1/alerts` (elenco, con filtro per
  stato), `POST /api/agentaudit/v1/alerts/{id}/mute` (sospensione con motivo e scadenza) e
  `PUT /api/agentaudit/v1/alert-rules/{key}` (soglie); corpi validati; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V…__avvisi.sql` sullo schema `app_agentaudit`: tabelle `alerts` e
  `alert_rules` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica —
  qui la cancellazione logica **si usa** normalmente, perché avvisi e soglie non sono la catena di prova: la riga
  di registro che dice «è stato emesso un avviso» sta invece nella catena e non si tocca (RF-6).
- **RT-4 — Modulo frontend (§3, §5).** Sezione `avvisi` del modulo `agentaudit` con l'elenco degli avvisi aperti,
  il pannello delle soglie e il comando di sospensione; solo token del sistema di design; funziona in tema chiaro
  e scuro; il colore d'allarme è quello funzionale, non il colore-categoria dell'app.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono
  presenti in `en, it, fr, es, de` — compresi i **testi dei messaggi di posta elettronica**, che sono testi
  visibili a tutti gli effetti e vanno inviati nella lingua del destinatario.
- **RT-6 — Varchi e quota (§6, §7).** La riga di registro dell'avviso (RF-6) **consuma** una unità della metrica
  `actions` come ogni altra riga accodata: è coerente, ma va detto nell'interfaccia perché un cliente che scopre
  di consumare quota per gli avvisi che non ha chiesto si arrabbia a ragione. A quota esaurita l'avviso viene
  comunque **recapitato**, e la riga rientra nel conteggio delle azioni rifiutate previsto dalla storia 0004:
  perdere un avviso perché è finita la quota sarebbe il peggior guasto possibile di questa app.
- **RT-7 — Esposizione conversazionale (§12).** Questa storia non dichiara strumenti. La lettura degli avvisi
  rientrerà nello strumento `riepiloga_attivita` della storia 0034; la modifica delle soglie **non** viene esposta
  a un assistente, perché abbassare una soglia è il modo più semplice per far sparire un avviso scomodo.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: gli avvisi trattano conteggi, identificativi di
  sorgente e di strumento. Il **motivo scritto** della sospensione è un testo libero redatto da una persona: va
  dichiarato nel manifesto in italiano e inglese come già fatto per il motivo delle decisioni (storia 0021). I
  destinatari degli avvisi per posta elettronica sono indirizzi di persone: voce nel manifesto, campo annotato,
  tabella presente in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** L'emissione, la sospensione e il recapito di un avviso sono registrati
  con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione; nel registro tecnico non finiscono né
  l'indirizzo del destinatario né il testo del motivo.

## 4. Criteri di accettazione

**CA-1 — Volume fuori scala**
- **Dato** una sorgente con una media di 9 azioni all'ora nelle ultime due settimane
- **Quando** ne dichiara 142 in un'ora
- **Allora** viene emesso un avviso che riporta i due numeri, il rimando alla cronologia filtrata su quella
  sorgente e quell'ora, e una riga nel registro

**CA-2 — L'avviso spiega sé stesso**
- **Dato** un avviso di tipo «strumento mai visto prima»
- **Quando** l'utente lo apre
- **Allora** legge il nome dello strumento, la prima comparsa, la sorgente che l'ha usato e il motivo per cui la
  condizione è scattata, senza dover interpretare un punteggio

**CA-3 — Sospensione con motivo e scadenza**
- **Dato** un avviso ricorrente su uno strumento noto e innocuo
- **Quando** l'utente lo sospende per 30 giorni scrivendo il motivo
- **Allora** l'avviso non viene più recapitato fino alla scadenza, la sospensione è visibile nell'elenco con il
  motivo e chi l'ha decisa, e alla scadenza l'avviso torna attivo da solo

**CA-4 — Nessun avviso perso per quota**
- **Dato** un account che ha esaurito la quota e la banda di cortesia
- **Quando** scatta una condizione di avviso
- **Allora** l'avviso viene recapitato ugualmente, e il conteggio delle azioni rifiutate registra la riga non
  accodata

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con sorgenti dai nomi identici
- **Quando** in `B` scatta una condizione
- **Allora** nessun utente di `A` vede quell'avviso, e la media che ha fatto scattare la condizione è calcolata
  sui soli dati di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** su ciascuna delle sei condizioni, con dati costruiti apposta, e di **integrazione** sulle
      rotte degli avvisi e delle soglie, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla valutazione delle condizioni e sulla lettura degli avvisi;
- [ ] **prova end-to-end**: **rimando** — il percorso `[J-AGENTAUDIT]` della storia 0037 copre l'emissione di un
      avviso dopo un'azione senza approvazione; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con storia proprietaria `0037`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), **compresi i messaggi di posta
      elettronica**;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per i destinatari degli avvisi e per il motivo della
      sospensione, con i campi annotati e le tabelle in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato, con la voce sul perché gli avvisi sono regole dichiarate e non un
      modello di punteggio, e la voce sul recapito garantito anche a quota esaurita;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia; è dichiarato che la modifica delle
      soglie **non sarà** esposta a un assistente, con il motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` | La condizione «strumento mai visto prima» ha senso solo con il catalogo degli strumenti |
| storia `0011` | I buchi di sequenza sono rilevati lì; qui si avvisa |
| storia `0023` | Gli scostamenti fra nulla osta ed esito sono rilevati lì; qui diventano avvisi |
| storia `0004` | Il comportamento a quota esaurita, e la contabilità delle righe rifiutate, viene da lì |
| Recapito per posta elettronica di piattaforma | Se la piattaforma non offre l'invio, il recapito resta in applicazione e la cosa va dichiarata invece che aggirata con un fornitore esterno |

## 7. Fuori ambito

- **il rilevamento di intenti malevoli** e i punteggi di rischio predittivi: esplicitamente esclusi, per la
  ragione al §1. Se un giorno serviranno, saranno un'altra storia con un'altra giustificazione;
- **gli avvisi definiti dall'utente** con condizioni libere: qui le condizioni sono sei e dichiarate; le
  condizioni componibili sono un'evoluzione, non il primo passo;
- **il recapito su messaggistica di squadra** (Slack, Teams): introdurrebbe un fornitore esterno che tratta dati
  per nostro conto (§2.4 della descrizione dell'applicazione), ed è deliberatamente fuori dal perimetro iniziale;
- **la reazione automatica** a un avviso (per esempio spegnere una sorgente da sola): l'app non esegue e non
  blocca da sola, dice soltanto (§1 della descrizione).

## 8. Punti aperti

- **Le soglie predefinite non le so.** «Volume fuori scala» e «sorgente silenziosa» hanno bisogno di numeri, e
  quei numeri si ricavano dai dati reali dei primi clienti, non da un documento. Propongo di partire con soglie
  prudenti e molto visibili, e di rivederle dopo il primo mese di uso vero. Chi chiude: sviluppatore, con i dati.
- **Se il recapito per posta elettronica sia una capacità di piattaforma disponibile.** Se non lo è, la storia
  consegna solo il recapito in applicazione: va deciso prima, perché cambia la promessa fatta al cliente. Chi
  chiude: piattaforma.
- **Se l'avviso debba consumare quota.** È coerente (è una riga della catena) ed è antipatico (il cliente paga per
  essere avvisato). L'alternativa — righe di sistema che non consumano — apre la porta a una seconda classe di
  righe, che è complessità. Fermata di escalation: decide lo sviluppatore, insieme al comportamento al tetto della
  storia 0004.
