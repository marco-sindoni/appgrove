# 0009 — Importazione delle anagrafiche da file

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 02 — Anagrafiche e catalogo
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta lasciando il prodotto che usava prima
> voglio caricare i miei clienti e le mie voci da un file tabellare
> così da poter cominciare a lavorare il primo giorno, invece di ricopiare duecento anagrafiche a mano e rinunciare
> al cambio.

**Contesto.** La ricerca dice che la migrazione lunga è una delle ragioni per cui una micro-impresa rinuncia a
cambiare prodotto (§2.5 della descrizione). Senza importazione, BillGrove è utilizzabile solo da chi parte da zero,
cioè da una minoranza del mercato. Va fatta subito dopo le due anagrafiche, e prima delle storie di documento,
perché è ciò che rende l'app popolabile.

## 2. Requisiti funzionali

1. **RF-1** — Si può caricare un file tabellare (valori separati da virgola) di clienti o di voci di catalogo.
2. **RF-2** — Prima di scrivere qualsiasi cosa l'utente vede un'**anteprima**: quante righe verranno create, quante
   scartate e perché, con l'associazione fra le colonne del file e i campi dell'app modificabile a mano.
3. **RF-3** — Le righe con errori vengono scartate **singolarmente**: un file con dieci righe sbagliate su duecento
   importa centonovanta righe, non zero.
4. **RF-4** — L'importazione riconosce i duplicati (per partita IVA o codice fiscale sui clienti, per codice sulle
   voci) e per ciascuno chiede una scelta unica per l'intero caricamento: salta, aggiorna o crea comunque.
5. **RF-5** — Al termine è disponibile un rapporto scaricabile con l'esito riga per riga.
6. **RF-6** — L'importazione è **ripetibile senza danno**: caricare due volte lo stesso file con la scelta «salta»
   non crea doppioni.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le righe importate nascono con il `tenant_id` preso dal token verificato;
  un `tenant_id` presente nel file viene **ignorato**, non usato. È il punto in cui un'importazione mal fatta
  diventa una falla di isolamento.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/billing/v1/imports` (caricamento e anteprima) e
  `POST /api/billing/v1/imports/{id}/confirm` (esecuzione); errori in `application/problem+json`; definizione
  OpenAPI aggiornata. Limite di dimensione del file dichiarato e verificato.
- **RT-3 — Persistenza (§8).** Migrazione `V6__import_job.sql` sullo schema `app_billing`: tabella `import_job` con
  `tenant_id`, stato, esito per riga, colonne di controllo e cancellazione logica. Il file caricato **non** si
  conserva oltre il tempo necessario all'importazione.
- **RT-4 — Modulo frontend (§3, §5).** Percorso in tre passi: carica, verifica l'anteprima, conferma. Solo token del
  sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutti i messaggi di errore di importazione passano dallo spazio-nomi `billing` e
  sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** L'importazione **non** consuma la metrica `documenti`: importa anagrafiche, non
  documenti. Richiede ruolo `admin`.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento**: caricare un file da chat non ha senso e
  l'importazione in blocco è esattamente il genere di operazione che non si vuole innescare per conversazione. Va
  dichiarato, perché il silenzio non è una risposta.
- **RT-8 — Dati personali (§10).** La storia **non introduce campi nuovi** ma introduce un trattamento nuovo: un
  file di anagrafiche caricato è una raccolta di dati personali. Nel manifesto va dichiarata la voce
  `import.file_caricato` con conservazione limitata al tempo dell'elaborazione, e `import_job` va aggiunta a
  `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `importazione avviata`, `importazione conclusa` (con i conteggi)
  e `riga scartata` (con il **numero di riga**, mai il contenuto) sono registrati con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Importazione riuscita**
- **Dato** un file con 50 clienti validi · **Quando** l'utente carica, verifica l'anteprima e conferma
- **Allora** vengono creati 50 clienti e il rapporto riporta 50 righe accettate e 0 scartate

**CA-2 — Righe sbagliate scartate singolarmente**
- **Dato** un file con 200 righe di cui 10 senza denominazione
- **Quando** si conferma l'importazione
- **Allora** vengono create 190 anagrafiche e il rapporto elenca le 10 righe scartate con il motivo

**CA-3 — Duplicati**
- **Dato** un file che contiene una partita IVA già presente in anagrafica e la scelta «salta»
- **Quando** si conferma · **Allora** quella riga non viene creata e compare nel rapporto come saltata

**CA-4 — Ripetizione senza danno**
- **Dato** lo stesso file importato una seconda volta con la scelta «salta»
- **Quando** si conferma · **Allora** non viene creata nessuna anagrafica nuova

**CA-5 — Isolamento fra account**
- **Dato** un file che contiene una colonna con l'identificativo di un altro account
- **Quando** un utente di `A` lo importa
- **Allora** tutte le righe nascono nell'account `A`, la colonna è ignorata, e l'account `B` non vede nulla di nuovo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sull'analisi del file e sul riconoscimento dei duplicati, di **integrazione**
      sull'importazione completa con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul caso del `tenant_id` presente nel file;
- [ ] **prova end-to-end**: *rimando* — l'importazione non è nel percorso principale `[J-BILLING]`, che parte dai
      dati di prova. Motivo: tenere il percorso corto. Proprietaria del rimando: storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la voce del file caricato e la sua conservazione limitata;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: nessuno, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | Si importano clienti |
| storia `0007` | Si importano voci di catalogo |

## 7. Fuori ambito

- l'importazione dei **documenti storici** dal prodotto precedente: rimandata, perché tocca la numerazione e la
  conservazione decennale (storie `0012` e `0026`) e merita una storia propria quando servirà;
- i connettori diretti verso altri prodotti: fuori ambito, si passa dal file tabellare;
- l'esportazione: storia `0027`.

## 8. Punti aperti

L'importazione dei documenti storici è un rimando esplicito, non una dimenticanza: chi migra da un altro prodotto
porta con sé fatture già emesse e numerate, e farle entrare senza rompere il contatore di numerazione è un problema
suo. Lo chiude una storia futura, dopo che la `0012` avrà fissato il comportamento del contatore.
