# 0012 — Anagrafica dei collaboratori

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 03 — Note spese e approvazione
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve rimborsare le spese
> voglio l'elenco delle persone che possono sostenerne, con il loro ruolo e la loro sede di lavoro
> così da attribuire ogni spesa a qualcuno e da poter includere anche il collaboratore occasionale che non usa
> l'applicazione.

**Contesto.** Finora la spesa punta a un identificativo di collaboratore che non esiste come entità. Serve adesso
perché tre cose successive ne dipendono: l'approvazione (chi approva chi), il regime fiscale della trasferta (che
dipende dal **Comune sede di lavoro** della persona, descrizione §2.3) e il pacchetto per il commercialista (che
raggruppa per persona). È anche il punto in cui si tocca il tema più delicato dell'app: da qui in poi trattiamo
**anagrafiche di lavoratori**, non di clienti (descrizione, §6).

## 2. Requisiti funzionali

1. **RF-1** — Si gestisce un elenco di collaboratori dell'account, ciascuno con nome, cognome, Comune sede di
   lavoro, centro di costo predefinito facoltativo e stato attivo o cessato.
2. **RF-2** — Un collaboratore può essere **collegato a un membro dell'account** (chi usa l'app) oppure esistere
   **senza accesso** (chi consegna gli scontrini a qualcun altro): entrambi i casi sono di prima classe.
3. **RF-3** — Ogni collaboratore ha un **ruolo di spesa**: `sostiene` (crea le proprie spese), `approva` (approva
   quelle altrui), `amministra` (vede tutto e prepara i pacchetti). I ruoli si sommano.
4. **RF-4** — Chi ha ruolo `sostiene` vede **solo le proprie** spese e le proprie note; chi `approva` vede quelle
   dei collaboratori a lui assegnati; chi `amministra` vede tutte quelle dell'account.
5. **RF-5** — Un collaboratore non si cancella se ha spese: si porta a `cessato`, sparisce dalle scelte future e
   resta leggibile sullo storico.
6. **RF-6** — L'anagrafica si può **importare da un file** e, quando la piattaforma avrà un'anagrafica dipendenti
   condivisa, alimentare per eventi: la struttura è pronta a riceverla, non la duplica per sempre.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `collaboratore` filtra per `tenant_id` preso
  dal token verificato. In più, e questa è la differenza rispetto alle altre risorse, vale un **filtro di
  visibilità dentro l'account**: il ruolo di spesa decide quali collaboratori e quali spese si vedono, e il filtro
  sta nel servizio, non nell'interfaccia.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST|PATCH /api/notespese/v1/collaboratori`; corpo validato;
  errori in `application/problem+json`; definizione OpenAPI aggiornata. La rotta di elenco restituisce ciò che il
  ruolo di chi chiede permette di vedere, e non di più.
- **RT-3 — Persistenza (§8).** Migrazione `V9__collaboratori.sql` sullo schema `app_notespese`: tabella
  `collaboratore` con `tenant_id`, chiave UUID versione 7, riferimento **logico** al membro dell'account,
  colonne di controllo e cancellazione logica; tabella di assegnazione approvatore → collaboratori. Nessuna chiave
  esterna verso lo schema dell'app centrale: il legame con il membro dell'account è logico.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Impostazioni → Collaboratori*: elenco, scheda, importazione da file.
  Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, ruoli e messaggi di errore passano dallo spazio-nomi `notespese` e sono
  presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo: i collaboratori **non** sono la metrica di quota — è una
  scelta deliberata (descrizione, §3), perché limitarli spingerebbe a escludere il collaboratore occasionale,
  esattamente il comportamento che non vogliamo.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: l'anagrafica di persone si tocca
  nell'interfaccia. Gli strumenti di lettura dell'epica 06 accettano il collaboratore come **filtro**, restituendo
  solo ciò che il ruolo di chi chiede permette.
- **RT-8 — Dati personali (§10).** 🛑 Storia ad alta densità di dati personali: **anagrafica di lavoratori**. Voci
  nuove nel manifesto in italiano e inglese (`collaboratore.nome`, `collaboratore.sede`), campi annotati
  `@PersonalData`, tabella `collaboratore` e tabella delle assegnazioni aggiunte a `exportData` e `purgeData`. Base
  giuridica proposta: **esecuzione del contratto di lavoro e obbligo di legge**, **non** consenso — nel rapporto di
  lavoro il consenso non è liberamente prestato (descrizione, §6). Nessun dato non necessario: niente data di
  nascita, niente codice fiscale, niente coordinate bancarie.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `collaboratore creato`, `ruolo cambiato`, `collaboratore
  cessato` portano `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e l'**identificativo** del
  collaboratore — mai il nome.

## 4. Criteri di accettazione

**CA-1 — Collaboratore senza accesso all'app**
- **Dato** un account con due membri
- **Quando** l'amministrazione crea un collaboratore non collegato a nessun membro
- **Allora** il collaboratore compare fra quelli a cui si possono attribuire spese, e non riceve nessun invito né
  accesso

**CA-2 — Visibilità per ruolo**
- **Dato** i collaboratori *Lia Perotti* (`sostiene`) e *Nadir Contini* (`approva`, assegnatario di Lia)
- **Quando** Lia chiede l'elenco delle spese
- **Allora** vede solo le proprie; quando lo chiede Nadir, vede anche quelle di Lia; un terzo collaboratore con solo
  `sostiene` non vede nessuna delle due

**CA-3 — Cessazione invece di cancellazione**
- **Dato** un collaboratore con spese registrate · **Quando** si tenta di cancellarlo
- **Allora** l'operazione è respinta con `409` e viene offerta la cessazione; le spese passate continuano a mostrare
  il suo nome

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri collaboratori
- **Quando** un utente di `A` chiede la scheda di un collaboratore di `B` conoscendone l'identificativo
- **Allora** riceve `404`

**CA-5 — Dati minimi**
- **Dato** il modulo di creazione
- **Quando** lo si esamina
- **Allora** non esistono campi per data di nascita, codice fiscale o coordinate bancarie: se servissero, sarebbero
  una decisione nuova

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla risoluzione della visibilità per ruolo; di **integrazione** sulla risorsa con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** completa: `sostiene`, `approva`, `amministra`, più
      il caso dell'utente senza ruolo di spesa;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, dove il percorso `[J-NOTESPESE]` attraversa due ruoli
      diversi; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con l'anagrafica dei lavoratori, campi annotati,
      tabelle presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la base giuridica proposta e il perché non è il consenso;
- [ ] contratto degli **strumenti conversazionali**: nessuno di scrittura, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | La spesa punta già a un collaboratore per identificativo logico: qui l'entità diventa vera |
| `0003` | Serve la sezione *Impostazioni* dove metterla |
| Catalogo 09 — PeopleGrove | **Non è una dipendenza tecnica ma un confine**: se un giorno esisterà l'anagrafica dipendenti condivisa, questa tabella dovrà alimentarsi da lì per eventi invece di essere ridigitata |

## 7. Fuori ambito

- Inviti e gestione dei membri dell'account: sono di piattaforma, non del modulo.
- Le coordinate bancarie per il pagamento del rimborso: escluse deliberatamente (storia `0016`).
- Gerarchie di approvazione a più livelli: qui c'è un solo livello, il secondo è nella storia `0015`.

## 8. Punti aperti

- **Che cosa fare dei dati di un collaboratore cessato** quando chiede la cancellazione, dato l'obbligo decennale di
  conservazione dei giustificativi: è il punto aperto n. 7 della descrizione dell'applicazione e lo chiude lo
  sviluppatore con verifica legale. La storia `0030` implementerà ciò che verrà deciso.
