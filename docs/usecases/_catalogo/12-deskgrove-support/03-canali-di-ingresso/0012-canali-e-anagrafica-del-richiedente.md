# 0012 — Canali e anagrafica del richiedente

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 03 — Canali di ingresso
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile del servizio clienti
> voglio che l'app sappia da dove è arrivata una richiesta e chi è la persona che l'ha scritta, anche quando ci
> aveva già scritto sei mesi fa
> così da vedere la storia di quel cliente in un colpo d'occhio invece di cercarla in tre caselle di posta.

**Contesto.** Fino alla storia `0011` le richieste le inserisce un operatore a mano e il richiedente lo scrive lui:
funziona, ma ogni nuova richiesta crea una persona nuova e la storia del cliente si sbriciola. Prima di aprire i
canali automatici (`0013`, `0014`, `0017`) servono le due cose che tutti e tre useranno: **il canale**, cioè il
recapito da cui le richieste entrano e la coda dove finiscono, e il **riconoscimento del richiedente**, cioè la
regola che dice quando due messaggi vengono dalla stessa persona. Farlo dopo significherebbe ripetere la stessa
logica tre volte e ritrovarsi tre anagrafiche diverse.

Un chiarimento che vale per tutta l'epica: **l'anagrafica clienti condivisa della suite non esiste ancora** e le
regole di piattaforma vietano a un'app di chiamarne un'altra o di leggerne lo schema
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §2, §8). DeskGrove tiene quindi la **propria** tabella
`requester` e si limita a **prevedere il campo** di riferimento all'identità condivisa, che oggi resta vuoto: il
giorno in cui l'anagrafica esisterà, l'allineamento avverrà per eventi e non servirà una migrazione
([application-description.md](../application-description.md) §10).

## 2. Requisiti funzionali

1. **RF-1** — Un amministratore dell'account crea un canale indicando il tipo (`posta`, `modulo web`, `whatsapp`),
   il recapito o identificativo che lo distingue e la coda di destinazione delle richieste che ne arrivano.
2. **RF-2** — Ogni canale ha uno stato della connessione fra `da configurare`, `attivo`, `sospeso` e `in errore`,
   con la data dell'ultima verifica e l'ultimo errore in forma leggibile; un canale che non è `attivo` **non fa
   entrare richieste**, e quelle già arrivate restano dove sono.
3. **RF-3** — Quando una richiesta entra da un canale portando un recapito, il servizio cerca nello **stesso
   account** un richiedente con quel recapito: se lo trova lo riusa, altrimenti ne crea uno nuovo. Non crea mai un
   secondo richiedente per un recapito già presente.
4. **RF-4** — I recapiti sono normalizzati prima del confronto — indirizzo di posta ridotto a minuscole e ripulito
   dagli spazi, numero di telefono portato in formato internazionale — e ciascun recapito normalizzato è unico
   dentro l'account.
5. **RF-5** — Il richiedente porta un campo di riferimento all'identità dell'anagrafica condivisa della suite, che
   questa storia **non valorizza mai**: esiste per essere riempito quando l'anagrafica esisterà.
6. **RF-6** — Nel dettaglio di una richiesta l'operatore vede l'elenco delle richieste precedenti dello stesso
   richiedente, con numero, oggetto, stato e data, e vi accede con un clic.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `channel` e `requester` filtra per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. Il riconoscimento del richiedente cerca **solo dentro l'account**: due account che ricevono
  scritture dallo stesso indirizzo hanno due richiedenti distinti, e non deve esistere alcuna interrogazione che li
  metta in relazione.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST|PATCH /api/helpdesk/v1/channels[/{id}]` e
  `GET /api/helpdesk/v1/requesters[/{id}]` più `GET /api/helpdesk/v1/requesters/{id}/tickets`; corpo validato
  (tipo del canale fra i valori ammessi, recapito conforme al tipo); errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__channel_and_requester_identity.sql` sullo schema `app_helpdesk`:
  tabella `channel` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo (`created_at`,
  `updated_at`, `created_by`, `updated_by`) e cancellazione logica (`deleted_at`); su `requester` si aggiungono le
  colonne dei recapiti normalizzati e il riferimento all'identità condivisa. Indici unici **parziali** su
  `(tenant_id, email_normalized)` e `(tenant_id, phone_e164)` limitati alle righe non cancellate logicamente, e
  indice su `(tenant_id, channel_id)`. Nessuna chiave esterna verso altri schemi: il riferimento all'identità
  condivisa è **logico**.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Impostazioni → Canali» del modulo `helpdesk` per l'elenco e la
  configurazione, e blocco «Richieste precedenti di questa persona» nel dettaglio della richiesta; dati letti con
  il client generato; solo token del sistema di design, colore-categoria `teal`; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi dei tipi di canale, stati della connessione,
  messaggi di errore sui recapiti — passano dallo spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La gestione dei canali e dei richiedenti **non consuma** la metrica `agents`:
  non è un posto operatore. Restano i varchi a monte: `402` con abbonamento non attivo, `403` per un ruolo
  insufficiente (la configurazione dei canali è riservata a `owner` e `admin`).
- **RT-7 — Esposizione conversazionale (§12).** La configurazione dei canali **non** è esposta come strumento: apre
  superfici verso l'esterno. Il richiedente riconosciuto entra invece nei risultati di `leggi_richiesta` e
  `elenca_richieste` già dichiarati; nessuno strumento nuovo. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Le voci `requester.name`, `requester.email`, `requester.phone`,
  `requester.locale` del manifesto `docs/compliance/manifests/helpdesk.yaml` (italiano e inglese) si **aggiornano**
  qui con i recapiti normalizzati, che sono dati personali quanto gli originali; i campi sono annotati
  `@PersonalData`; `requester` è già presente in `exportData` e `purgeData` del contratto `HelpdeskDataContract` e
  vi resta. La tabella `channel` **non contiene dati di persone**: è configurazione dell'account. Nessun fornitore
  esterno nuovo. Ruolo di appgrove: **responsabile del trattamento** per conto del cliente.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «canale creato», «canale passato in errore», «richiedente
  riconosciuto», «richiedente creato» sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, riportando **identificativi e non recapiti**: nessun indirizzo di posta e nessun numero di telefono
  finisce nel registro.

## 4. Criteri di accettazione

**CA-1 — La persona che ha già scritto viene riconosciuta**
- **Dato** un account con un richiedente `giulia.bianchi@example.test` e una richiesta chiusa a suo nome
- **Quando** entra una nuova richiesta da un canale con lo stesso indirizzo
- **Allora** la richiesta è collegata al richiedente esistente, non ne viene creato un secondo, e il dettaglio
  mostra la richiesta precedente

**CA-2 — La normalizzazione evita il duplicato per differenza di scrittura**
- **Dato** un richiedente registrato come `giulia.bianchi@example.test`
- **Quando** entra una richiesta con `  Giulia.Bianchi@Example.TEST  `
- **Allora** è la stessa persona: nessun richiedente nuovo

**CA-3 — Canale non attivo**
- **Dato** un canale in stato `sospeso`
- **Quando** arriva una richiesta da quel canale
- **Allora** nulla viene creato, il rifiuto è contato con il motivo, e le richieste già arrivate da quel canale
  restano visibili e lavorabili

**CA-4 — Recapito già usato da un altro richiedente**
- **Dato** un richiedente con il numero `+390212345678`
- **Quando** un operatore prova ad assegnare lo stesso numero a un secondo richiedente
- **Allora** riceve `409` in `application/problem+json` con l'indicazione del richiedente che già lo usa, e nulla
  viene modificato

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che ricevono entrambi scritture da `mario.rossi@example.test`
- **Quando** un utente di `A` apre la scheda del richiedente e chiede l'elenco delle sue richieste
- **Allora** vede solo le richieste di `A`, anche se forza nella richiesta l'identificativo del richiedente di `B`
  o un `tenant_id` diverso

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla normalizzazione dei recapiti e sulla regola di riconoscimento, e di **integrazione**
      sulle risorse `channels` e `requesters`, con database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** su entrambe le risorse nuove, con tentativo di forzare `tenant_id` e
      identificativi dall'esterno;
- [ ] **prova end-to-end**: *rimando* — la configurazione del canale e il riconoscimento del richiedente sono passi
      del percorso `[J-HELPDESK]` completo, di proprietà della storia `0037`; voce `da-coprire` nel
      **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con
      motivo e storia proprietaria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per i recapiti normalizzati, con i campi annotati
      `@PersonalData` e la tabella `requester` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotata la scelta di tenere
      un'anagrafica propria e il campo di riferimento all'identità condivisa non valorizzato;
- [ ] contratto degli **strumenti conversazionali** dichiarato: nessuno strumento nuovo, configurazione dei canali
      non esposta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove (modello di dominio §4
      della descrizione dell'applicazione).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` — modello dati multi-account | Le tabelle `ticket`, `ticket_message` e `requester` devono esistere: questa storia le estende, non le crea |
| Storia `0006` — apertura manuale di una richiesta | È il primo consumatore del riconoscimento: aprendo a mano una richiesta si riusa il richiedente già noto |
| Storia `0019` — code di lavoro (epica 04, non ancora scritta) | Il canale dichiara una coda di destinazione, ma le code arrivano dopo: fino ad allora il campo punta alla coda predefinita dell'account creata dallo scaffolding |
| Anagrafica clienti condivisa della suite | **Non esiste**: nel frattempo il campo di riferimento resta vuoto e l'app funziona da sola. Nessuna chiamata ad altre app, nessuna interrogazione fra schemi |

## 7. Fuori ambito

- **L'ingresso automatico vero e proprio**: qui si costruiscono il canale e il riconoscimento, non le porte da cui
  i messaggi entrano — le fanno le storie `0013` (modulo web), `0014` (posta in ingresso) e `0017` (WhatsApp);
- **le credenziali dei canali**: i tipi introdotti da questa storia non ne hanno bisogno (il modulo web usa una
  chiave pubblica generata dalla storia `0013`, la posta un recapito assegnato). Dove vivano le credenziali di un
  canale che ne richiede è problema della storia che lo introduce — `0017` per WhatsApp;
- **l'unione di due richiedenti** riconosciuti tardi come la stessa persona: l'unione riguarda le richieste ed è
  della storia `0011`; l'unione delle anagrafiche resta fuori da questa epica;
- **la scheda completa del richiedente** con note e dati commerciali: è il mestiere di LeadGrove, e qui si mostra
  solo ciò che serve a rispondere.

## 8. Punti aperti

- **Riconoscimento per nome quando manca il recapito.** Una richiesta aperta a mano dopo una telefonata può non
  avere né indirizzo né numero. Questa storia in quel caso crea un richiedente nuovo, e accetta il duplicato: fare
  il riconoscimento «per nome somigliante» significherebbe unire persone diverse con lo stesso cognome. Se il
  duplicato diventasse un fastidio reale, la via è un'unione manuale assistita, non un accostamento automatico.
  Chiude: sviluppatore, come decisione di prodotto, non prima di aver visto il fastidio.
- **Allineamento con l'anagrafica condivisa quando esisterà.** Chi è la sorgente di verità del nome e del recapito
  quando i due archivi divergono? Chiude: chi possiede l'anagrafica condivisa della suite, insieme allo
  sviluppatore; oggi non c'è nessuno con cui allinearsi.
- **Conservazione dei richiedenti senza richieste.** Un richiedente le cui richieste sono tutte scadute va
  cancellato o resta? La proposta è che segua la conservazione delle richieste, ma il termine è un parametro
  dell'account ed è materia della storia `0036`. Chiude: storia `0036`.
