# 0002 — Modello dati multi-account

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio le due tabelle portanti dell'app — il debitore e il credito — con l'isolamento fra account già cablato
> così da poter costruire il resto sapendo che nessuno vedrà mai i crediti di un altro.

**Contesto.** La storia `0001` ha creato lo schema vuoto. Questa mette dentro le due entità senza le quali non esiste
niente: chi deve pagare e che cosa deve pagare. Si fa adesso e non dopo perché l'isolamento fra account non è una
funzione che si aggiunge: o c'è dalla prima tabella o non c'è. La forma dei campi segue il perimetro dettato dal
Garante nel vademecum sul recupero crediti — dati identificativi, codice fiscale o partita IVA, recapiti, importo e
condizioni di pagamento e nulla di più ([documento capofila](../application-description.md) §2.3).

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella `debitore` con denominazione, forma (impresa o persona fisica), identificativo fiscale,
   recapito di posta elettronica, telefono, nome del referente, lingua preferita e note libere.
2. **RF-2** — Esiste la tabella `credito` con numero e data del documento, data di scadenza, importo originario,
   importo residuo, valuta, stato, riferimento logico al debitore e origine del dato.
3. **RF-3** — Lo stato del credito è uno fra `aperto`, `scaduto`, `sospeso`, `in_escalation`, `incassato`, `stralciato`,
   e la transizione fra stati è governata dal servizio, non dal chiamante.
4. **RF-4** — Due crediti dello stesso account non possono avere lo stesso numero di documento per lo stesso debitore.
5. **RF-5** — L'importo residuo non può essere negativo né superiore all'importo originario.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `debitore` e `credito` filtra per `tenant_id`
  preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Il
  vincolo di unicità di RF-4 è per account, non globale.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica in questa storia: si introducono le entità, i
  repository e la validazione. Le rotte arrivano con le storie `0006` e `0007`.
- **RT-3 — Persistenza (§8).** Migrazione `V2__debitore_e_credito.sql` sullo schema `app_crediti`: tabelle `debitore` e
  `credito` con `tenant_id`, chiave primaria UUID versione 7 generata dall'applicazione, colonne di controllo
  (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica (`deleted_at`). Nessuna chiave esterna
  verso altri schemi: il riferimento al debitore è una colonna, non un vincolo verso l'esterno. Indici su
  (`tenant_id`, `stato`, `data_scadenza`) e (`tenant_id`, `debitore_id`), che sono le due interrogazioni di ogni giorno.
- **RT-4 — Modulo frontend (§3, §5).** Fuori ambito: storia `0003`.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto.
- **RT-6 — Varchi e quota (§6, §7).** Fuori ambito: storia `0004`. Qui si predispone solo il conteggio dei crediti in
  stato diverso da `incassato` e `stralciato`, che sarà la base della metrica `crediti_monitorati`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: non c'è ancora nulla da leggere o scrivere
  dall'esterno.
- **RT-8 — Dati personali (§10).** Voci nuove nel manifesto `docs/compliance/manifests/crediti.yaml` in italiano e
  inglese per `debitore.denominazione`, `debitore.codice_fiscale`, `debitore.referente_nome`, `debitore.email`,
  `debitore.telefono`, `debitore.lingua`, `debitore.note` e per i campi economici di `credito`; campi Java annotati
  `@PersonalData`; tabelle `debitore` e `credito` aggiunte a `exportData` e `purgeData` del contratto dati.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «debitore creato», «credito creato», «stato del credito cambiato»
  sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, riportando **identificativi e
  non nomi**.

## 4. Criteri di accettazione

**CA-1 — Le tabelle esistono con la forma attesa**
- **Dato** un database effimero
- **Quando** si applicano le migrazioni vere
- **Allora** esistono `app_crediti.debitore` e `app_crediti.credito` con `tenant_id`, chiave UUID versione 7, colonne di
  controllo e `deleted_at`

**CA-2 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri debitori e crediti
- **Quando** un utente di `A` chiede l'elenco dei crediti attraverso il repository
- **Allora** vede solo i propri, anche se forza l'identificativo dell'account `B` in ogni punto in cui potrebbe entrare

**CA-3 — Numero di documento duplicato**
- **Dato** un credito già registrato con numero `2026/114` per il debitore Alfa
- **Quando** se ne registra un altro con lo stesso numero e lo stesso debitore, nello stesso account
- **Allora** l'operazione è respinta con un errore che nomina il conflitto; lo **stesso** numero in un altro account è
  invece accettato

**CA-4 — Importo residuo coerente**
- **Dato** un credito da 1.000 € · **Quando** si tenta di portare il residuo a 1.200 € o a −50 €
- **Allora** l'operazione è respinta con un errore di validazione

**CA-5 — Cancellazione logica**
- **Dato** un debitore cancellato logicamente
- **Quando** si interroga l'elenco dei debitori
- **Allora** non compare, ma la riga è ancora in tabella con `deleted_at` valorizzato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e compliance);
- [ ] prove di **unità** sulla validazione degli importi e di **integrazione** sulle migrazioni, con database effimero;
- [ ] prova di **isolamento fra account** su entrambe le entità introdotte;
- [ ] **prova end-to-end**: *nessun impatto* — nessuna superficie utente in questa storia;
- [ ] **traduzioni**: non applicabile;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati, tabelle presenti in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta degli indici e sul perimetro dei campi ammessi;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta, dichiarato esplicitamente;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | Il servizio e lo schema devono esistere |

## 7. Fuori ambito

- Incassi, solleciti, promesse, contestazioni, punteggi: sono tabelle delle epiche successive, introdotte dalla storia
  che le usa.
- Le rotte di lettura e scrittura: storie `0006` e `0007`.
- La logica di transizione automatica `aperto` → `scaduto`: storia `0010`.

## 8. Punti aperti

Le **durate di conservazione** dichiarate nel manifesto sono una proposta del documento capofila §6 e non sono fondate
su una fonte: le conferma lo sviluppatore, eventualmente in sede di revisione legale.
