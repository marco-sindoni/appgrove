# 0002 — Modello dati multi-account

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio le tabelle di ReachGrove create con le colonne di controllo e il perimetro dell'account
> così da poter scrivere le storie di dominio senza rimettere mano allo schema a ogni funzione.

**Contesto.** Il modello di dominio è già disegnato ([application-description.md](../application-description.md)
§4): quindici tabelle, di cui alcune con un vincolo insolito — la prova del consenso e l'elenco di soppressione
**non si correggono**, si accrescono. Quel vincolo non è un dettaglio implementativo da aggiungere dopo: se le
tabelle nascono modificabili, la prima storia che scrive un aggiornamento distrugge la prova, e nessuna prova
automatica se ne accorge. Per questo lo schema si scrive tutto insieme e adesso, con i divieti dentro.

Una seconda scelta va fatta qui e non dopo: `subscriber` **non ha** una colonna «stato del consenso» che qualcuno
possa impostare a mano. Se ci fosse, prima o poi qualcuno la scriverebbe, e l'app avrebbe un modo di rendere una
persona contattabile senza prova — cioè esattamente ciò che questa applicazione esiste per impedire.

## 2. Requisiti funzionali

1. **RF-1** — Le migrazioni creano sullo schema `app_campaigns` le tabelle del modello di dominio: `subscriber`,
   `consent_record`, `suppression`, `segment`, `message_template`, `campaign`, `delivery`, `delivery_event`,
   `automation`, `automation_run`, `subscription_form`, `sender_domain`, `channel_connection`, più le due tabelle
   di appoggio con valore di prova `form_submission` e `import_row`.
2. **RF-2** — Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7 generata dall'applicazione, colonne
   di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e colonna di cancellazione logica
   (`deleted_at`).
3. **RF-3** — `subscriber` **non ha** alcuna colonna «stato del consenso» o «contattabile» scrivibile: lo stato
   attuale per canale si deriva dalle registrazioni di `consent_record` e dall'elenco di soppressione. La
   derivazione la implementa la storia 0007; qui si garantisce che la colonna non esista.
4. **RF-4** — `consent_record` e `suppression` sono tabelle **ad accrescimento**: il livello di accesso ai dati
   espone solo l'inserimento e la lettura, e non esiste alcun metodo di aggiornamento o di cancellazione se non
   quello riservato all'esercizio dei diritti dell'interessato.
5. **RF-5** — Nessuna chiave esterna verso altri schemi e nessuna interrogazione fra schemi: `tenant_id` e ogni
   riferimento a entità di altre app sono riferimenti **logici**.
6. **RF-6** — Ogni entità ha il proprio archivio (*repository*) con le interrogazioni di base già filtrate per
   account, e ogni tabella entra nel manifesto dei dati e nel contratto di esportazione e cancellazione dell'app.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni archivio filtra `WHERE tenant_id = :tid` con il valore preso dal
  contesto dell'account, che a sua volta viene dal token verificato. Nessun metodo di archivio accetta un
  `tenant_id` come parametro dal chiamante. Prova di isolamento su tutte le entità introdotte.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova in questa storia: le risorse arrivano con le
  storie di dominio. La definizione OpenAPI resta quella della sonda.
- **RT-3 — Persistenza (§8).** Migrazioni Flyway `V2__…` e seguenti sullo schema `app_campaigns`, scritte in SQL,
  non applicate all'avvio in produzione. Indici previsti fin d'ora, perché nascono da interrogazioni certe:
  `(tenant_id, email)` su `subscriber`, `(tenant_id, subscriber_id, channel, created_at)` su `consent_record` per
  recuperare l'ultima registrazione, `(tenant_id, contact_hash)` su `suppression`, `(tenant_id, campaign_id)` su
  `delivery`, `(tenant_id, delivery_id, type)` su `delivery_event`.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata in questa storia.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto. Nota che vale per le storie successive:
  `subscriber` porta la colonna `language` dell'iscritto, che è la lingua **del destinatario** e non ha nulla a
  che vedere con le cinque lingue dell'interfaccia.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: creare tabelle non è un invio. La metrica
  `messages_sent` entra in gioco nella storia 0004.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia. Il modello dati però decide
  cosa gli strumenti potranno rispondere: `stato_iscritto` (storia 0034) esiste solo perché lo stato è derivabile
  dalle registrazioni. Dipendenza dichiarata: UC 0061-0063, non ancora implementati.
- **RT-8 — Dati personali (§10).** È la storia che riempie il manifesto
  `docs/compliance/manifests/campaigns.yaml`: undici voci in italiano e inglese secondo la tabella del §6 della
  descrizione dell'applicazione, campi Java annotati `@PersonalData`, **tutte** le tabelle presenti sia in
  `exportData` sia in `purgeData` del contratto dati. Due note vanno scritte nel manifesto, non solo qui: la prova
  del consenso **sopravvive alla revoca** e viene meno solo con la cancellazione dei dati dell'interessato; la
  soppressione conserva l'impronta crittografica non reversibile del recapito, perché cancellarla riaprirebbe la
  porta agli invii verso chi si è opposto.
- **RT-9 — Registrazione eventi (§14).** Le migrazioni applicate sono registrate con `tenant_id` non applicabile,
  `app_id` (`campaigns`) e identificativo di correlazione; nessun dato personale.

## 4. Criteri di accettazione

**CA-1 — Le migrazioni si applicano su un database vuoto**
- **Dato** un database effimero senza schema
- **Quando** si applicano le migrazioni vere
- **Allora** lo schema `app_campaigns` contiene le quindici tabelle, ciascuna con `tenant_id`, chiave primaria
  UUID versione 7, colonne di controllo e `deleted_at`

**CA-2 — Non esiste uno stato del consenso scrivibile**
- **Dato** la tabella `subscriber`
- **Quando** si ispezionano le sue colonne
- **Allora** non esiste alcuna colonna che dichiari se la persona è contattabile: quel dato non è memorizzato, è
  derivato

**CA-3 — La prova non si riscrive**
- **Dato** una registrazione presente in `consent_record`
- **Quando** si tenta di aggiornarla o di cancellarla attraverso il livello di accesso ai dati
- **Allora** l'operazione non è disponibile: l'archivio espone solo inserimento e lettura, e la prova d'unità lo
  dimostra

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con propri iscritti e proprie registrazioni di consenso
- **Quando** un archivio dell'account `A` esegue una lettura qualsiasi
- **Allora** restituisce solo le righe di `A`, anche se una riga di `B` ha lo stesso indirizzo di posta

**CA-5 — Esportazione e cancellazione conoscono tutte le tabelle**
- **Dato** il contratto dati dell'app
- **Quando** si esegue il controllo automatico di conformità
- **Allora** ogni campo annotato come dato personale compare nel manifesto, e ogni tabella con dati di persone
  compare sia in esportazione sia in cancellazione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione delle chiavi e sull'assenza di metodi di aggiornamento nelle tabelle ad
      accrescimento, e di **integrazione** sulle migrazioni con database effimero;
- [ ] prova di **isolamento fra account** su tutte le entità introdotte;
- [ ] **prova end-to-end**: nessun impatto — la storia non introduce superficie utente; il percorso
      `[J-CAMPAIGNS]` nasce nella storia 0037;
- [ ] **traduzioni**: nessun testo visibile introdotto;
- [ ] **manifesto dei dati** riempito in italiano e inglese con le voci del §6 della descrizione, campi annotati,
      tutte le tabelle in esportazione e cancellazione, comprese le due note sulla sopravvivenza della prova e
      sulla soppressione;
- [ ] **registro delle decisioni** compilato, con annotato perché `subscriber` non ha uno stato del consenso
      scrivibile e perché due tabelle sono ad accrescimento;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove il modello dati è descritto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Servono il servizio, lo schema `app_campaigns` e il manifesto vuoto da riempire |
| Conferma della classificazione dei dati personali ([application-description.md](../application-description.md) §6) | Basi giuridiche e termini di conservazione sono una fermata di escalation: il manifesto si compila **insieme** allo sviluppatore, non si inventa |

## 7. Fuori ambito

- la derivazione dello stato «contattabile» dalle registrazioni: è la storia 0007, che è anche la sola a poterla
  scrivere;
- ogni rotta, schermata e regola di dominio: sono le epiche 02-06;
- la macchina a stati della campagna: la colonna `status` esiste, ma le transizioni ammesse le impone l'epica 04
  (storie 0018 e 0019);
- la cifratura del riferimento alle credenziali in `channel_connection`: la colonna esiste, il meccanismo lo
  definisce la storia 0022.

## 8. Punti aperti

- **Termini di conservazione** — quelli proposti al §6 della descrizione (dieci anni per la prova del consenso,
  ventiquattro mesi per i recapiti, dodici per le misurazioni) non hanno un fondamento di legge che l'analisi
  abbia trovato. Sono una scelta del titolare da dichiarare e motivare. Chiude lo sviluppatore con la revisione
  legale, prima che il manifesto vada in produzione.
- **Forma della soppressione** — conservare la sola impronta crittografica non reversibile del recapito è una
  proposta ([application-description.md](../application-description.md) §11.6 lettera b), non una decisione: va
  validata perché tocca il diritto alla cancellazione.
- **Campi personalizzati dell'iscritto** — `subscriber.custom_fields` è un documento libero definito dal cliente:
  è un ingresso non presidiato per categorie particolari di dati. Il presidio, se servirà, è trasversale e non lo
  decide questa storia.
