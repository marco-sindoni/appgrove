# 0034 — Strumenti di lettura

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0034` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024`, `0025`, `0014`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio poter chiedere al mio assistente «cosa ha fatto ieri notte l'agente di fatturazione?» e «chi ha approvato
> quella cancellazione?»
> così da avere una risposta in dieci secondi invece che dopo aver imparato a usare cinque filtri.

**Contesto.** Le domande che si fanno a un registro sono domande in lingua naturale, e sono esattamente quelle a
cui un elenco con i filtri risponde male (§7 della descrizione dell'applicazione). Questa storia dichiara il
**contratto** dei cinque strumenti di sola lettura: nome stabile, descrizione in lingua naturale, schema dei
parametri, schema del risultato, marcatura di lettura. Non costruisce il server conversazionale, che è di
piattaforma e **non esiste ancora** (epica 12, UC 0061-0066).

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara cinque strumenti di **sola lettura**: `elenca_azioni`,
   `dettaglio_azione`, `elenca_approvazioni_in_attesa`, `verifica_integrita`, `riepiloga_attivita`.
2. **RF-2** — Ogni strumento dichiara nome stabile, descrizione orientata all'intento di chi chiede, schema dei
   parametri con tipi e obbligatorietà, schema del risultato, e la marcatura *lettura*.
3. **RF-3** — Il risultato di ogni strumento è **minimizzato**: identificativi e forme, **mai** i contenuti dei
   parametri, mai il contenuto allegato cifrato, mai indirizzi di posta elettronica.
4. **RF-4** — Ogni strumento produce lo stesso risultato della rotta corrispondente dell'interfaccia di
   programmazione, a parità di richiesta: se le due divergono, è un difetto.
5. **RF-5** — Quando la domanda è ambigua (due strumenti con nome simile, due sorgenti omonime), il risultato
   invita a disambiguare invece di indovinare.
6. **RF-6** — Il contratto vive **dentro `services/agentaudit`**, versionato con l'app, e non in un registro
   centrale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni strumento serve l'operazione con filtro `tenant_id` preso dal
  token verificato; il `tenant_id` non arriva mai dai parametri della chiamata conversazionale, che sono
  trattati come **non fidati** e validati contro lo schema prima di toccare il dominio.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova: gli strumenti si appoggiano ai
  servizi di dominio già usati dalle rotte esistenti, per garantire la parità di comportamento.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova.
- **RT-5 — Cinque lingue (§4).** Le descrizioni degli strumenti sono ciò su cui l'assistente ragiona: vanno
  scritte in inglese come lingua sorgente del contratto tecnico, mentre i messaggi di errore rivolti alla persona
  passano dallo spazio-nomi `agentaudit` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Gli strumenti attraversano la stessa catena di varchi delle rotte normali;
  il consumo di quota delle chiamate dell'assistente è la storia 0036. La `verifica_integrita` **non** consuma
  quota, coerentemente con la scelta della storia 0014.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `elenca_azioni(periodo?, sorgente?,
  strumento?, esito?, richiedente?) → elenco minimizzato`, `dettaglio_azione(id) → scheda`,
  `elenca_approvazioni_in_attesa(sorgente?) → elenco`, `verifica_integrita(periodo?) → esito`,
  `riepiloga_attivita(periodo) → conteggi`. Tutti marcati **lettura**, nessuna conferma umana richiesta. Il
  server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063): la storia consegna il
  contratto e le prove di parità, non il collegamento.
- **RT-8 — Dati personali (§10).** Nessun campo nuovo. Il punto delicato è **in uscita**: il risultato verso
  l'assistente contiene identificativi di persone (chi ha chiesto, chi ha approvato) e va dichiarato come tale.
  Non escono nomi, indirizzi né contenuti.
- **RT-9 — Registrazione eventi (§14).** Ogni invocazione di uno strumento produce un evento tecnico con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione — **e, in questa app soltanto, produce anche
  una riga del registro** (storia 0036): AuditGrove è l'unica app del catalogo che deve registrare le proprie
  stesse letture conversazionali.

## 4. Criteri di accettazione

**CA-1 — Parità con l'interfaccia di programmazione**
- **Dato** un account con azioni registrate
- **Quando** si invoca `elenca_azioni` con gli stessi filtri della rotta della cronologia
- **Allora** si ottengono le stesse righe, nello stesso ordine, e nessun campo di contenuto

**CA-2 — Parametri non conformi**
- **Dato** una chiamata con un periodo scritto in una forma non prevista
- **Quando** lo strumento viene invocato
- **Allora** risponde con un errore di validazione descrittivo che permette di correggere, e nessuna lettura viene
  eseguita

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie azioni
- **Quando** un assistente che opera per conto di un utente di `A` invoca `elenca_azioni` indicando nei parametri
  l'identificativo dell'account `B`
- **Allora** vede solo le azioni di `A`: l'indicazione nei parametri è ignorata

**CA-4 — Minimizzazione in uscita**
- **Dato** un'azione per cui il cliente ha attivato la conservazione del contenuto
- **Quando** si invoca `dettaglio_azione`
- **Allora** il risultato riporta forma e impronta dei parametri, e **non** il contenuto conservato

**CA-5 — Disambiguazione**
- **Dato** due sorgenti con nome simile
- **Quando** si invoca `elenca_azioni` indicando quel nome
- **Allora** il risultato elenca le due sorgenti e chiede quale, invece di sceglierne una

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione degli schemi e di **integrazione** sulla parità fra strumento e rotta,
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su ogni strumento, compreso il tentativo di forzare l'account dai
      parametri;
- [ ] **prova end-to-end**: risposta «rimando» — il percorso `[J-AGENTAUDIT]` copre l'interfaccia; la parte
      conversazionale non è collaudabile finché l'epica 12 non esiste, e la voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) indica come proprietaria l'epica
      di piattaforma 12;
- [ ] **traduzioni** dei messaggi di errore presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; è però dichiarato che gli identificativi di persone escono verso
      l'assistente;
- [ ] **registro delle decisioni** compilato, con la scelta di quali campi escono e quali no;
- [ ] contratto degli **strumenti conversazionali** dichiarato per tutte e cinque le funzioni;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove il contratto degli strumenti è descritto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0024`, `0025` | Gli strumenti servono le stesse letture della cronologia e della scheda |
| storia `0014` | `verifica_integrita` espone la verifica già costruita |
| storia `0021` | `elenca_approvazioni_in_attesa` legge la coda delle approvazioni |
| UC 0061-0063 (livello conversazionale, non implementato) | Manca il server che pubblica gli strumenti e ne instrada le chiamate: nel frattempo il contratto si scrive, si versiona e si collauda per parità |

## 7. Fuori ambito

- gli strumenti di scrittura e il divieto di auto-approvazione: storia 0035;
- il consumo di quota e i ruoli sulle chiamate dell'assistente: storia 0036;
- la costruzione del server conversazionale: è di piattaforma, non di questa app.

## 8. Punti aperti

- **Il formato del contratto degli strumenti non è deciso** (UC 0063, «Punti aperti»): dove esattamente vive nel
  servizio e come si dichiara è un nodo di piattaforma. Fino ad allora la storia definisce il *contenuto* del
  contratto, non la sua sintassi.
- **Quanto storico può leggere un assistente in una sola chiamata.** Un elenco molto lungo verso un assistente è
  un rischio di dispersione di dati: propongo un tetto per chiamata, il cui valore è da confermare.
