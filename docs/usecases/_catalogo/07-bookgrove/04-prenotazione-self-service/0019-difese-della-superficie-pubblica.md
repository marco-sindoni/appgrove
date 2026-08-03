# 0019 — Difese della superficie pubblica

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 04 — Prenotazione self-service del cliente finale
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha aperto la propria agenda al pubblico
> voglio che nessuno possa riempirmela di appuntamenti finti o prenotare a nome di un'altra persona
> così da poter continuare a fidarmi di quello che vedo in agenda.

**Contesto.** L'identificativo pubblico di sede non è un segreto (storia `0016`): chiunque lo conosca può
raggiungere la rotta che crea una prenotazione. Le difese non possono quindi essere la segretezza, e vanno
costruite apposta. È la storia che rende **mettibile in produzione** la `0017`: senza, la superficie pubblica non
si accende. Il rischio non è teorico ed è il primo dei rischi noti dichiarati al §11 della descrizione: un'agenda
piena di appuntamenti falsi fa perdere fiducia nello strumento più in fretta di quanto la guadagni.

## 2. Requisiti funzionali

1. **RF-1** — La prenotazione dalla pagina pubblica diventa **ferma** solo dopo la verifica del contatto: un
   codice breve inviato all'indirizzo o al numero indicato, da riportare per confermare.
2. **RF-2** — In attesa della verifica lo spazio è **trattenuto** per pochi minuti e poi si libera da solo: chi
   non completa non blocca l'agenda.
3. **RF-3** — Esiste una limitazione di frequenza per indirizzo di rete e per contatto: oltre una soglia le
   richieste vengono rifiutate con un messaggio neutro, senza rivelare quale soglia sia stata superata.
4. **RF-4** — L'attività può impostare un tetto di prenotazioni aperte per singolo contatto (per esempio non più
   di tre appuntamenti futuri per la stessa persona), per fermare gli abusi ingenui.
5. **RF-5** — L'attività può **sospendere immediatamente** la pagina pubblica, e la sospensione ha effetto subito.
6. **RF-6** — Le prenotazioni pubbliche non verificate o sospette sono visibili in agenda come tali, distinguibili
   dalle altre.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il conteggio delle richieste, dei trattenimenti e dei tentativi è per
  account e non attraversa gli account; l'abuso su un'attività non deve poter degradare il servizio di un'altra.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/prenotazioni/v1/pubblico/{identificativo}/verifica` e la corrispondente conferma; errori in
  `problem+json` con un unico codice neutro «troppe richieste», uguale in tutti i casi di rifiuto per difesa;
  OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V11__difese_pubbliche.sql`: tabella `trattenimento` con `tenant_id`,
  UUID versione 7, colonne di controllo, scadenza e impronta del codice di verifica (**mai il codice in chiaro**);
  estensione di `evento_prenotazione` con l'indirizzo di rete.
- **RT-4 — Difese, non segretezza.** Le difese sono: limitazione di frequenza per indirizzo di rete e per
  contatto, trattenimento a scadenza breve, verifica del contatto, risposte indistinguibili, nessuna
  enumerazione (identificativi di servizio e di prenotazione non deducibili l'uno dall'altro), e nessuna
  differenza di tempo di risposta fra un identificativo esistente e uno inesistente.
- **RT-5 — Frontend (§3, §5).** Il passo di verifica è un solo campo, con la possibilità di rinviare il codice
  dopo un'attesa; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Il codice di verifica e i suoi messaggi in `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: l'**indirizzo di rete** di chi
  usa la pagina pubblica, con finalità «difesa dagli abusi» e base giuridica «legittimo interesse», durata
  proposta 12 mesi; campi annotati `@PersonalData`; tabelle `trattenimento` ed `evento_prenotazione` in
  `exportData` e `purgeData`. **Nessun tracciamento** oltre a questo: niente strumenti di analisi, niente impronta
  del navigatore, nessun servizio antirobot di terze parti senza una decisione esplicita (vedi i punti aperti).
- **RT-8 — Registrazione eventi (§14).** `verifica inviata`, `verifica fallita`, `richiesta respinta per
  frequenza` con `tenant_id`, `app_id` e correlazione — **mai il contatto né il codice**.
- **RT-9 — Prove (§11).** Prova di sicurezza dedicata: sequenza di richieste oltre soglia, codice sbagliato
  ripetuto, trattenimento scaduto, tentativo di prenotare a nome di un contatto non verificato.

## 4. Criteri di accettazione

**CA-1 — Verifica del contatto**
- **Dato** un visitatore che completa il modulo · **Quando** conferma · **Allora** riceve un codice e la
  prenotazione diventa ferma solo dopo averlo riportato correttamente

**CA-2 — Il trattenimento scade**
- **Dato** un visitatore che non completa la verifica · **Quando** passano i minuti previsti · **Allora** lo
  spazio torna disponibile senza intervento di nessuno

**CA-3 — Limitazione di frequenza**
- **Dato** una serie di richieste dallo stesso indirizzo di rete oltre la soglia · **Quando** arrivano
- **Allora** vengono rifiutate con il messaggio neutro, uguale a quello degli altri casi di difesa

**CA-4 — Nessuna enumerazione**
- **Dato** un tentativo di scoprire quali attività o quali prenotazioni esistono, variando gli identificativi
- **Quando** si osservano risposte e tempi di risposta
- **Allora** sono indistinguibili fra caso esistente e caso inesistente

**CA-5 — Sospensione immediata**
- **Dato** un'attività sotto abuso · **Quando** sospende la pagina · **Allora** la pagina risponde con la schermata
  neutra dalla richiesta successiva

**CA-6 — Tetto per contatto**
- **Dato** un tetto di tre appuntamenti futuri per contatto · **Quando** lo stesso contatto prova a prenotare il
  quarto · **Allora** viene rifiutato con un messaggio comprensibile

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sulla scadenza del trattenimento e di **integrazione** sulle difese;
- [ ] prova di **isolamento fra account** e **prova di sicurezza** dedicata (RT-9);
- [ ] **prova end-to-end**: **coperta ora** — il passo di verifica entra nel percorso `[J-BOOKGROVE-PUB]` della
      storia `0034`, con il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con l'indirizzo di rete e i trattenimenti;
- [ ] **registro delle decisioni** compilato: le difese scelte, il perché la segretezza non è fra queste, e la
      decisione sul servizio antirobot;
- [ ] avvio locale invariato: in locale la verifica non manda messaggi veri;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | difende quello che la `0017` apre |
| storia `0021` | il codice di verifica viaggia sui canali di recapito; se non esistono ancora, si usa la sola posta elettronica |

## 7. Fuori ambito

- la difesa dagli attacchi di volume a livello di rete: è di piattaforma, non di questa app;
- il blocco di singoli contatti o indirizzi di rete su base permanente: rimandato, si valuta se emergerà.

## 8. Punti aperti

**Servizio antirobot di terze parti.** Un controllo antirobot ridurrebbe molto l'abuso automatico, ma quasi tutti
i servizi disponibili sono di fornitori extraeuropei e osservano il comportamento del visitatore: è un fornitore
nuovo che tratta dati per nostro conto, su una pagina che oggi non ha **nessun** tracciamento. La proposta è
**non introdurlo** e affidarsi a verifica del contatto e limitazione di frequenza, riservandosi di valutare una
soluzione europea se l'abuso diventerà reale. È una decisione dello sviluppatore, con un risvolto sulla privacy.
