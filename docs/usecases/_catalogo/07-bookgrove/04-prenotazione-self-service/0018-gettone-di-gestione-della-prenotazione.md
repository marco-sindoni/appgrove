# 0018 — Gettone di gestione della prenotazione

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 04 — Prenotazione self-service del cliente finale
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che ha prenotato e a cui è successo un imprevisto
> voglio spostare o disdire il mio appuntamento da sola, dal collegamento che ho ricevuto
> così da non dover telefonare per disdire — che è esattamente il motivo per cui la gente non disdice e non si
> presenta.

**Contesto.** È la storia che chiude il cerchio economico dell'applicazione: una disdetta fatta in tempo è uno
spazio che si può rivendere, una mancata presentazione è un'ora persa. Rendere facilissimo disdire è
controintuitivo ma è il modo più efficace per ridurre le mancate presentazioni.

Qui l'impostazione è **identica a quella dell'app 06 (QuoteGrove)**, e l'allineamento è voluto: un **gettone di
capacità firmato dal server**, di ambito singolo, a scadenza, revocabile, che dà accesso a **una sola**
prenotazione. La differenza rispetto alla storia `0016` è netta e va tenuta ferma: l'identificativo pubblico di
sede **non è un segreto** e apre una vetrina; questo gettone **è un segreto** e apre un dato personale.

## 2. Requisiti funzionali

1. **RF-1** — Alla conferma, il cliente riceve un collegamento personale che apre una pagina con il riepilogo
   della **sua** prenotazione: quando, dove, quale servizio, con chi.
2. **RF-2** — Dalla stessa pagina può **disdire** oppure **spostare** l'appuntamento, entro i limiti della
   politica di disdetta dell'attività (storia `0024`).
3. **RF-3** — Lo spostamento propone solo spazi realmente liberi per lo stesso servizio, e vale come uno
   spostamento a tutti gli effetti, non come disdetta più nuova prenotazione.
4. **RF-4** — Il gettone **scade**: dopo l'appuntamento (più un margine breve) non apre più nulla. L'attività può
   **revocarlo** in qualsiasi momento.
5. **RF-5** — Un gettone scaduto, revocato, manomesso o inesistente porta alla **stessa** pagina neutra, che non
   rivela se la prenotazione esista e invita a contattare l'attività.
6. **RF-6** — Ogni azione del cliente lascia una traccia in `evento_prenotazione` e si vede in agenda, così che
   l'attività sappia che è stato il cliente a disdire e non un errore del personale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1) — deviazione dichiarata, allineata all'app 06.** Il `tenant_id` e
  l'identificativo della prenotazione stanno **dentro il gettone**, firmato dal server, verificato a ogni
  richiesta. Il gettone abilita **una sola** cosa: leggere quella prenotazione e agire su di essa. Non concede
  nessun'altra lettura, non è un'identità, non diventa mai una sessione e non si scambia con un token di accesso.
- **RT-2 — Forma del gettone.** Lungo e casuale, ricavato da una sorgente crittograficamente sicura; **memorizzato
  come impronta**, mai in chiaro; con scadenza propria; revocabile; legato a una sola prenotazione. Cambiare una
  parte del gettone per raggiungerne un'altra deve fallire come qualunque gettone inesistente.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte pubbliche separate, per esempio
  `GET /api/prenotazioni/v1/pubblico/prenotazione/{gettone}` e le azioni corrispondenti, con limitazione di
  frequenza per indirizzo di rete; risposte in `problem+json`; OpenAPI aggiornata.
- **RT-4 — Persistenza (§8).** Migrazione `V10__gettoni_prenotazione.sql`: tabella `gettone_prenotazione` con
  `tenant_id`, UUID versione 7, colonne di controllo, impronta del gettone, scadenza e momento di revoca.
- **RT-5 — Frontend (§3, §5).** Pagina fuori dal backoffice, con gli stessi token del sistema di design, leggibile
  da telefono, in tema chiaro e scuro, senza nulla che richieda autenticazione.
- **RT-6 — Cinque lingue (§4).** La pagina è resa nella lingua preferita del cliente fra `en, it, fr, es, de`,
  con ricaduta sulla lingua predefinita.
- **RT-7 — Dati personali (§10).** La pagina mostra dati personali del solo interessato che ha il gettone: nessuna
  voce nuova nel manifesto oltre alla tabella dei gettoni, che va comunque dichiarata e aggiunta a `exportData` e
  `purgeData` perché è collegata a una persona. **Nessun tracciamento** sulla pagina.
- **RT-8 — Registrazione eventi (§14).** `prenotazione aperta dal cliente`, `disdetta dal cliente`, `gettone
  rifiutato` con `tenant_id`, `app_id` e correlazione — mai il contenuto della prenotazione.
- **RT-9 — Prove (§11).** Prova di sicurezza dedicata: gettone di un'altra prenotazione, gettone di un altro
  account, gettone scaduto, gettone revocato, gettone manomesso — tutti respinti **allo stesso modo**.

## 4. Criteri di accettazione

**CA-1 — Il cliente vede la sua prenotazione**
- **Dato** una prenotazione confermata · **Quando** il cliente apre il collegamento ricevuto dal telefono
- **Allora** vede quando, dove e cosa, nella propria lingua, senza doversi registrare

**CA-2 — Disdetta autonoma**
- **Dato** la stessa pagina, dentro la finestra di disdetta libera · **Quando** il cliente disdice · **Allora** lo
  spazio torna disponibile, l'agenda mostra «disdetta dal cliente» e la traccia lo conferma

**CA-3 — Spostamento**
- **Dato** la stessa pagina · **Quando** il cliente sceglie un altro orario fra quelli proposti · **Allora**
  l'appuntamento si sposta, resta una sola prenotazione e i conteggi non registrano nessuna disdetta

**CA-4 — Gettone non valido**
- **Dato** un gettone scaduto, revocato, manomesso o di un'altra prenotazione · **Quando** lo si apre
- **Allora** si vede la **stessa** pagina neutra in tutti e quattro i casi, e nulla lascia capire se la
  prenotazione esista

**CA-5 — Nessuna scoperta laterale**
- **Dato** un gettone valido · **Quando** si tenta di modificarne una parte per raggiungere un'altra prenotazione
- **Allora** la richiesta è respinta e la frequenza delle richieste dallo stesso indirizzo di rete è limitata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sulla verifica del gettone e di **integrazione** sulle rotte pubbliche;
- [ ] prova di **isolamento fra account** in forma di **prova di sicurezza** sul gettone (RT-9);
- [ ] **prova end-to-end**: **coperta ora** — è la seconda metà del percorso `[J-BOOKGROVE-PUB]` della storia
      `0034`, con il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la tabella dei gettoni, in italiano e inglese;
- [ ] **registro delle decisioni** compilato: **la forma esatta del gettone di capacità e l'allineamento
      dichiarato con l'app 06**, più la differenza rispetto all'identificativo pubblico della storia `0016`;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | il gettone si emette alla conferma |
| storia `0015` | disdetta e spostamento riusano le stesse transizioni di stato |
| storia `0024` | la politica di disdetta decide fin quando il cliente può disdire senza conseguenze |

## 7. Fuori ambito

- l'invio del collegamento: è il motore dei messaggi, storia `0022`;
- la penale in caso di disdetta tardiva: storia `0024`;
- un'area personale del cliente con lo storico di tutte le sue prenotazioni: **deliberatamente rimandata**, perché
  richiederebbe di autenticare il cliente finale e ne farebbe un utente, cosa che oggi non è.

## 8. Punti aperti

**Anteprime automatiche dei programmi di posta.** Alcuni sistemi aprono i collegamenti per generare l'anteprima o
per controllo di sicurezza: se il gettone fosse consumabile alla prima apertura, si brucerebbe da solo. Per questo
il gettone qui è **a scadenza e riusabile**, non monouso. Va però verificato che nessuna azione irreversibile
possa essere innescata da una semplice apertura: le azioni devono richiedere un secondo atto esplicito.
