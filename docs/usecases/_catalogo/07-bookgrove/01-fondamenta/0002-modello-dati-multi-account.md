# 0002 — Modello dati multi-account

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore
> voglio lo schema del database con l'isolamento fra account e una rappresentazione del tempo che regga il
> cambio dell'ora legale
> così da non dover riscrivere ogni tabella quando ci si accorge che gli orari si sono spostati di un'ora.

**Contesto.** Un'app di agenda ha una particolarità che le altre non hanno: quasi ogni riga contiene un istante, e
gli istanti sono la cosa che si sbaglia più spesso. Se il momento di una prenotazione si conserva come «ora
locale» senza fuso, la notte del cambio dell'ora tutte le prenotazioni del giorno dopo diventano sbagliate di
un'ora e nessuno se ne accorge fino a quando un cliente arriva a vuoto. Questa storia mette a terra la scelta —
istante in tempo universale coordinato più fuso orario della sede — mentre lo schema è vuoto e costa niente
cambiarlo.

## 2. Requisiti funzionali

1. **RF-1** — Esiste lo schema `app_prenotazioni` con le prime tabelle: `sede`, `servizio`, `risorsa`, e la
   tabella di raccordo fra servizio e risorsa.
2. **RF-2** — Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
   cancellazione logica.
3. **RF-3** — Ogni istante si conserva in tempo universale coordinato; ogni `sede` porta il proprio fuso orario e
   ogni interrogazione che mostra orari li converte in quel fuso.
4. **RF-4** — Un account nuovo nasce con una `sede` predefinita, perché il caso normale è l'attività con un solo
   luogo e non deve dover configurare nulla.
5. **RF-5** — Esiste il contratto dati dell'app (`PrenotazioniDataContract`) con `appId()`, `exportData(scope)`,
   `purgeData(scope)` e `manifest()`, ancora sulle sole tabelle esistenti.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura filtra per `tenant_id` preso dal token
  verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Se il filtro
  mancasse, il comportamento corretto è negare.
- **RT-2 — Persistenza (§8).** Migrazione `V1__impianto.sql` sullo schema `app_prenotazioni`; nessuna chiave
  esterna verso altri schemi (`tenant_id` è un riferimento logico); un ruolo del database per il servizio, con
  privilegi solo sul proprio schema.
- **RT-3 — Tempo.** Le colonne di istante sono `timestamptz`; il fuso della sede è un identificativo della base
  dati dei fusi orari (per esempio `Europe/Rome`), non uno scostamento fisso: lo scostamento cambia due volte
  l'anno, il fuso no.
- **RT-4 — Interfaccia di programmazione (§2).** Nessuna rotta nuova in questa storia oltre a quelle minime di
  lettura di `sede`; corpo validato; errori in `problem+json`; OpenAPI aggiornata.
- **RT-5 — Dati personali (§10).** `risorsa.nome` può essere il nome di un operatore: è una voce del manifesto in
  italiano e inglese, il campo si annota `@PersonalData` e la tabella entra in `exportData` e `purgeData`.
- **RT-6 — Registrazione eventi (§14).** Le migrazioni applicate e gli errori di migrazione sono registrati con
  `app_id` e correlazione.
- **RT-7 — Prove (§11).** Integrazione con PostgreSQL 17 effimero e **migrazioni Flyway vere**; prova di
  isolamento fra due account su ogni tabella introdotta; prova di unità sulla conversione degli istanti nei giorni
  di cambio dell'ora.

## 4. Criteri di accettazione

**CA-1 — Lo schema esiste e regge**
- **Dato** un database vuoto · **Quando** si avvia il servizio con le migrazioni · **Allora** lo schema
  `app_prenotazioni` contiene le tabelle previste, con `tenant_id`, colonne di controllo e `deleted_at`

**CA-2 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie risorse
- **Quando** un utente di `A` chiede l'elenco delle risorse forzando l'identificativo dell'account di `B`
- **Allora** vede solo le proprie, e il valore forzato è ignorato

**CA-3 — L'ora legale non sposta niente**
- **Dato** una sede con fuso `Europe/Rome` e un istante conservato per il 26 ottobre alle 10:00 locali
- **Quando** lo si rilegge dopo il cambio dell'ora
- **Allora** è ancora le 10:00 locali di quel giorno, non le 9:00 né le 11:00

**CA-4 — Sede predefinita**
- **Dato** un account che apre l'app per la prima volta · **Quando** entra · **Allora** esiste già una sede con il
  fuso ricavato dalle impostazioni dell'account, modificabile

**CA-5 — Diritti dell'interessato già collegati**
- **Dato** un account con una risorsa intestata a una persona · **Quando** si chiede l'esportazione dei dati
- **Allora** il nome dell'operatore compare nell'esportazione, e la cancellazione lo rimuove fisicamente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `compliance`);
- [ ] prove di **unità** sulla conversione degli istanti e di **integrazione** sulle migrazioni, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su ogni tabella introdotta;
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; la copre la storia `0033`, proprietaria
      del percorso `[J-BOOKGROVE]`, con il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: non applicabile, nessun testo visibile;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `risorsa.nome`, campo annotato e tabella
      presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato: la rappresentazione del tempo e il perché;
- [ ] avvio locale invariato, `dev migrate` applica le migrazioni senza passi manuali;
- [ ] documentazione aggiornata dove serve.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | il servizio e lo schema devono esistere |

## 7. Fuori ambito

- le tabelle di prenotazione, cliente, promemoria e lista d'attesa: nascono nelle epiche che le usano, così ogni
  tabella arriva con il codice che la riempie;
- le regole di disponibilità: storia `0008`.

## 8. Punti aperti

**Le due ore patologiche del cambio dell'ora.** Nel giorno in cui si va avanti, un'ora locale **non esiste**; nel
giorno in cui si torna indietro, un'ora locale **esiste due volte**. Le regole di disponibilità sono espresse in
ora locale e vanno risolte in istanti: la proposta è saltare l'ora che non esiste e prendere la prima delle due
occorrenze di quella doppia, dichiarandolo. Da confermare in implementazione, ed è un caso da coprire con una
prova.
