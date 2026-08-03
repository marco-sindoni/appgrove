# 0002 — Modello dati multi-account

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore
> voglio le prime tabelle dello schema `app_progetti`, con l'isolamento fra account già dentro
> così da non dover rimettere mano al modello quando arriveranno le funzioni.

**Contesto.** Il modello dati di FlowGrove ha una particolarità che conviene affrontare subito: contiene dati
**dei lavoratori** del cliente (assegnazioni e righe di ore), non solo dei clienti del cliente. È la prima app
della piattaforma in questa condizione ([application-description.md](../application-description.md) §6) e le
conseguenze — quali colonne esistono, quali **non** devono esistere — vanno scritte nella migrazione iniziale,
non aggiunte dopo. Le tabelle si creano qui nella loro forma vuota; le funzioni che le riempiono stanno nelle
epiche successive.

## 2. Requisiti funzionali

1. **RF-1** — La migrazione iniziale crea nello schema `app_progetti` le tabelle `project`, `task`, `assignment`,
   `milestone`, `time_entry`, `rate`, `comment`, `attachment`, `project_cost`, `project_template`,
   `billable_batch` e `billable_batch_line`.
2. **RF-2** — Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7, le colonne di controllo
   (`created_at`, `updated_at`, `created_by`, `updated_by`) e `deleted_at` per la cancellazione logica.
3. **RF-3** — Ogni tabella ha un indice che comincia per `tenant_id`: nessuna interrogazione utile deve poter
   scandire l'intera tabella di tutti gli account.
4. **RF-4** — Le tabelle **non** contengono e non devono contenere: coordinate geografiche, indirizzi di rete
   dell'utente, orari di entrata e uscita dal lavoro, causali di assenza, riferimenti a stati di salute. È un
   vincolo del modello, non una raccomandazione ([application-description.md](../application-description.md) §6).
5. **RF-5** — Il riferimento al cliente (`project.customer_ref`) e all'utente (`assignment.user_id`,
   `time_entry.user_id`) sono riferimenti **logici**: nessuna chiave esterna verso altri schemi.
6. **RF-6** — Le entità di dominio corrispondenti esistono lato Java con i campi che riguardano una persona
   annotati `@PersonalData` e dichiarati nel manifesto dei dati in italiano e inglese.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le classi di accesso ai dati (repository) accettano il contesto del
  tenant e non espongono metodi senza filtro; il controllo strutturale che fa fallire la compilazione se qualcuno
  aggira il filtro deve restare verde.
- **RT-2 — Persistenza (§8).** Migrazione `V1__schema_iniziale.sql` sullo schema `app_progetti`; chiavi primarie
  UUID versione 7 generate dall'applicazione; cancellazione logica con `deleted_at`; **nessuna chiave esterna**
  verso altri schemi; un ruolo del database per il solo servizio `progetti`.
- **RT-3 — Interfaccia di programmazione (§2).** Nessuna rotta nuova in questa storia: le entità non si espongono
  mai direttamente, gli oggetti di trasferimento arrivano con le funzioni.
- **RT-4 — Dati personali (§10).** Voci nuove nel manifesto `docs/compliance/manifests/progetti.yaml` in italiano
  e inglese per: referente del progetto (nome, recapito), utente assegnato, autore e contenuto della riga di ore,
  autore e testo del commento, autore e contenuto dell'allegato, colonne di controllo. Campi annotati
  `@PersonalData`; tutte le tabelle elencate aggiunte a `exportData` e `purgeData` del contratto dati (storia
  0030). Un campo annotato e non dichiarato fa fallire la compilazione.
- **RT-5 — Registrazione eventi (§14).** Le migrazioni applicate sono registrate con `tenant_id` non applicabile,
  `app_id` e versione dello schema; nessun dato personale nei registri.

## 4. Criteri di accettazione

**CA-1 — Lo schema nasce**
- **Dato** un database vuoto
- **Quando** si esegue `dev migrate`
- **Allora** lo schema `app_progetti` contiene tutte le tabelle previste, ciascuna con `tenant_id`, colonne di
  controllo e `deleted_at`

**CA-2 — Isolamento a livello di riga**
- **Dato** due account `A` e `B` con righe nelle stesse tabelle
- **Quando** si legge una qualsiasi entità con il contesto dell'account `A`
- **Allora** nessuna riga dell'account `B` è visibile, e un `tenant_id` passato dall'esterno viene ignorato

**CA-3 — Manifesto coerente**
- **Dato** le entità Java con i campi annotati `@PersonalData`
- **Quando** si esegue `./run-tests.sh compliance` e la compilazione del backend
- **Allora** ogni campo annotato ha la sua voce nel manifesto in italiano e in inglese, e il controllo è verde

**CA-4 — Campi vietati assenti**
- **Dato** la migrazione iniziale
- **Quando** si ispeziona lo schema
- **Allora** non esiste nessuna colonna di posizione geografica, orario di entrata/uscita o causale di assenza

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **integrazione** sulle migrazioni con database effimero PostgreSQL 17;
- [ ] prova di **isolamento fra account** sul livello di accesso ai dati, su ogni tabella introdotta;
- [ ] **prova end-to-end**: nessun impatto — non c'è superficie utente;
- [ ] **traduzioni**: non applicabile;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con i campi annotati e tutte le tabelle presenti in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato **perché** certe colonne sono deliberatamente assenti;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Serve il servizio e lo schema dichiarato |
| Classificazione dei dati personali (§6 della descrizione) | Le voci del manifesto vanno confermate dallo sviluppatore prima di scriverle |

## 7. Fuori ambito

- le rotte e le schermate: arrivano con le epiche 02, 03 e 04;
- la logica di calcolo del margine: la tabella `project_cost` nasce qui vuota, la logica è della storia 0026;
- il contratto dati completo (esportazione e cancellazione funzionanti): storia 0030. Qui si predispongono le
  tabelle, non l'implementazione.

## 8. Punti aperti

- **Durata di conservazione delle righe di ore**: la tabella nasce, ma la politica di conservazione non è decisa
  ([application-description.md](../application-description.md) §11.5). Va scritta nel manifesto prima che l'app
  vada in produzione, non prima che la tabella esista.
