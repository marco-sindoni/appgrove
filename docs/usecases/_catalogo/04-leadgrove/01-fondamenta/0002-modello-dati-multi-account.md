# 0002 — Modello dati multi-account

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio le tabelle centrali di LeadGrove create con le colonne d'obbligo e il filtro per account
> così da non dover rincorrere l'isolamento tabella per tabella mentre scrivo le storie di dominio.

**Contesto.** Le tredici tabelle di LeadGrove hanno tutte gli stessi vincoli: `tenant_id`, chiave primaria UUID
versione 7, colonne di controllo, cancellazione logica. Farle nascere insieme e giuste costa meno che aggiungerle
una per volta e scoprire alla decima che una non filtra. Questa storia crea lo **scheletro** — tabelle, indici,
entità di base e repository — non il comportamento: creare una azienda o una trattativa è materia delle epiche 02
e 03.

## 2. Requisiti funzionali

1. **RF-1** — Lo schema `app_sales` contiene le tabelle `company`, `contact`, `contact_preference`, `pipeline`,
   `stage`, `deal`, `deal_stage_event`, `activity`, `note`, `custom_field`, `custom_field_value`, `web_form`,
   `form_submission`, `import_job`, `import_row`, `seat`.
2. **RF-2** — Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7, `created_at`, `updated_at`,
   `created_by`, `updated_by` e `deleted_at`.
3. **RF-3** — Ogni tabella ha un indice che comincia per `tenant_id`, così che nessuna interrogazione utile possa
   essere scritta senza il filtro.
4. **RF-4** — Le relazioni fra entità sono riferimenti **logici** dentro lo stesso schema; nessuna chiave esterna
   verso altri schemi e nessuna interrogazione fra schemi.
5. **RF-5** — I campi che contengono dati di persone sono annotati `@PersonalData` e dichiarati nel manifesto
   `docs/compliance/manifests/sales.yaml` in italiano e inglese.
6. **RF-6** — Esiste il contratto `SalesDataContract` con `appId()`, `exportData(scope)`, `purgeData(scope)` e
   `manifest()`, che copre **tutte** le tabelle con dati di persone.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I repository di base filtrano `WHERE tenant_id = :tid` con
  l'identificativo preso dal token verificato; il controllo strutturale fa fallire la compilazione se una
  interrogazione aggira il filtro.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova in questa storia oltre a quelle dei diritti
  dell'interessato già fornite dal contratto dati.
- **RT-3 — Persistenza (§8).** Migrazione `V2__sales_domain.sql` sullo schema `app_sales`: sedici tabelle con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. Le migrazioni non si
  applicano all'avvio in produzione.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: le tabelle esistono, non contano.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento; le entità che gli strumenti useranno nascono
  qui. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** Voci nuove nel manifesto per `contact.name`, `contact.email`, `contact.phone`,
  `contact.role`, `contact.source`, `contact_preference.*`, `company.name`, `note.body`, `activity.title`,
  `activity.outcome`, `deal.loss_reason`, `custom_field_value.value`, `form_submission.payload`,
  `import_row.payload`, `seat.member_id` — tutte in italiano e inglese, tutte con i campi Java annotati, tutte
  presenti in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Nessun evento applicativo; le migrazioni si registrano con
  identificativi, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — Lo schema nasce completo**
- **Dato** un database effimero vuoto
- **Quando** si applicano le migrazioni vere
- **Allora** lo schema `app_sales` contiene le sedici tabelle, ognuna con `tenant_id`, chiave primaria, colonne di
  controllo e `deleted_at`

**CA-2 — Il filtro non si può dimenticare**
- **Dato** una interrogazione scritta senza filtro sull'account
- **Quando** si esegue il controllo strutturale
- **Allora** la compilazione fallisce con un messaggio che indica la classe e il metodo

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ognuno con una riga in ciascuna tabella
- **Quando** un utente di `A` legge attraverso i repository di base
- **Allora** vede solo le righe di `A`, anche forzando l'identificativo di `B` nella richiesta

**CA-4 — Nessun campo personale sfugge al manifesto**
- **Dato** un campo annotato `@PersonalData` non dichiarato nel manifesto
- **Quando** si eseguono le prove del backend
- **Allora** la compilazione fallisce

**CA-5 — Esportazione e cancellazione sono complete**
- **Dato** un account con almeno una riga in ognuna delle quattordici tabelle con dati di persone
- **Quando** si richiedono esportazione e poi cancellazione
- **Allora** l'esportazione contiene tutte e quattordici e la cancellazione le svuota fisicamente, lasciando una
  riga di prova nel registro delle purghe

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend` e `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sui repository di base e di **integrazione** sulle migrazioni, con database effimero;
- [ ] prova di **isolamento fra account** su ogni tabella introdotta;
- [ ] **prova end-to-end**: nessun impatto — nessuna superficie utente;
- [ ] **traduzioni**: nessun testo visibile;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati, tabelle presenti in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con annotate le durate di conservazione proposte e il fatto che sono
      da confermare;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare;
- [ ] documentazione aggiornata: il modello di dominio della descrizione dell'applicazione se diverge.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Serve il servizio e lo schema prima delle tabelle |
| Conferma dello sviluppatore sulla classificazione dei dati personali | Il manifesto non si compila da soli: «niente contratto, niente produzione» |

## 7. Fuori ambito

- il comportamento delle entità (creazione, modifica, transizioni): epiche 02-05;
- i campi personalizzati come funzione utente: storia 0009, qui nascono solo le tabelle;
- l'indicizzazione fine per la ricerca a testo: storia 0008.

## 8. Punti aperti

- **Durata di conservazione di `form_submission.payload` e `import_row.payload`** — proposte rispettivamente 24 e
  90 giorni; non esiste un termine di legge. Chiude lo sviluppatore in sede di manifesto, con revisione legale.
