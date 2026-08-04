# 0025 — Importazione da file tabellare

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 05 — Acquisizione e scambio dei lead
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007` — è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha la propria rubrica in un foglio di calcolo da otto anni
> voglio caricarla in LeadGrove senza ricopiarla a mano
> così da poter cominciare a usare l'app oggi invece che «quando avrò tempo».

**Contesto.** L'analisi in rete è netta: chi non usa un CRM sta su fogli di calcolo, e l'inserimento manuale è il
motivo per cui non passa ([application-description.md](../application-description.md) §2.5). L'importazione non è
una funzione avanzata da rimandare: è un requisito del **primo giorno**, e va fatta bene, perché una importazione
che sporca l'archivio è peggio del foglio di calcolo.

## 2. Requisiti funzionali

1. **RF-1** — L'utente carica un file tabellare (valori separati da virgola o punto e virgola, o foglio di
   calcolo) e l'app riconosce le colonne, proponendo la corrispondenza con i campi di contatto e azienda.
2. **RF-2** — L'utente può correggere la corrispondenza colonna per colonna e ignorare le colonne che non servono.
3. **RF-3** — Prima di scrivere qualcosa l'app mostra un'**anteprima** delle prime righe come verranno importate,
   con il conteggio di quante saranno create, quante scartate e perché.
4. **RF-4** — L'importazione crea contatti e, quando la colonna dell'azienda è valorizzata, le aziende collegate,
   evitando di crearne due volte la stessa nello stesso caricamento.
5. **RF-5** — Al termine l'utente vede il riepilogo e può scaricare l'elenco delle righe **scartate** con il
   motivo, per correggerle e ricaricarle.
6. **RF-6** — I contatti importati portano origine «importazione» e il riferimento all'importazione che li ha
   creati.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'importazione scrive solo dentro l'account del token verificato; le
  righe grezze conservate portano `tenant_id`.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/sales/v1/imports` (caricamento e analisi),
  `POST /api/sales/v1/imports/{id}/confirm` (esecuzione) e `GET /api/sales/v1/imports/{id}`; il caricamento ha un
  limite di dimensione dichiarato e restituisce `413` se superato; errori in `application/problem+json`; OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabelle `import_job` e `import_row` già create dalla storia 0002. L'esecuzione
  avviene a lotti in transazioni separate, così che un file grande non tenga aperta una transazione lunga; ogni
  riga registra il proprio esito.
- **RT-4 — Modulo frontend (§3, §5).** Procedura in tre passi (caricamento, corrispondenza, anteprima e conferma);
  solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutti i testi della procedura e i motivi di scarto in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo della metrica `seats`: importare non consuma posti, ed è
  deliberato — la metrica non deve punire chi porta dentro i propri dati (§3 della descrizione dell'applicazione).
  Importare richiede ruolo `owner` o `admin`.
- **RT-7 — Esposizione conversazionale (§12).** L'importazione **non** è esposta alla chat: richiede di guardare
  una corrispondenza di colonne e un'anteprima, e scrive molti dati in un colpo solo.
- **RT-8 — Dati personali (§10).** `import_row.payload` conserva la riga grezza ed è già dichiarata nel manifesto:
  qui si valorizza. La proposta di conservazione è **90 giorni** dalla fine dell'importazione, poi cancellazione
  del grezzo mantenendo i contatti creati; è una proposta da confermare. Le tabelle `import_job` e `import_row`
  devono comparire in `exportData` e `purgeData`: sono fra quelle che «sembrano log» e non lo sono.
- **RT-9 — Registrazione eventi (§14).** «Importazione avviata/conclusa» con conteggi di righe totali, create e
  scartate; **mai** il contenuto delle righe.

## 4. Criteri di accettazione

**CA-1 — Importazione riuscita**
- **Dato** un file con 50 righe valide e le colonne riconosciute
- **Quando** l'utente conferma dopo l'anteprima
- **Allora** vengono creati 50 contatti con origine «importazione» e le aziende collegate, senza doppioni interni
  al file

**CA-2 — Righe scartate**
- **Dato** un file con 10 righe di cui 3 senza nome
- **Quando** l'utente conferma
- **Allora** ne vengono create 7, il riepilogo dice che 3 sono state scartate per «nome mancante» e l'elenco degli
  scarti si può scaricare

**CA-3 — Niente si scrive prima della conferma**
- **Dato** un file caricato e l'anteprima aperta
- **Quando** l'utente abbandona la procedura
- **Allora** nessun contatto e nessuna azienda risultano creati

**CA-4 — File troppo grande**
- **Dato** un file oltre il limite dichiarato
- **Quando** viene caricato
- **Allora** riceve `413` con un messaggio che dice il limite e suggerisce di dividere il file

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede l'esito di un'importazione di `B`
- **Allora** riceve `404`

**CA-6 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** avvia un'importazione
- **Allora** riceve `403`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sul riconoscimento delle colonne e sui motivi di scarto, di **integrazione** sull'esecuzione
      a lotti;
- [ ] prova di **isolamento fra account** su importazioni e righe;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo; coperta da prove d'integrazione con un file
      d'esempio inventato, con il motivo nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, motivi di scarto compresi;
- [ ] **manifesto dei dati** verificato per `import_job` e `import_row`, presenti in esportazione e cancellazione,
      con la durata di conservazione proposta annotata come **da confermare**;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: non esposta, con la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sulla procedura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0006`, `0007` | Servono le entità da creare |

## 7. Fuori ambito

- il riconoscimento dei doppioni **rispetto all'archivio esistente**: storia 0026;
- l'importazione di trattative e attività: fuori perimetro in questa proposta, si importa l'anagrafica;
- l'importazione da altri prodotti tramite loro interfacce: sarebbe un fornitore esterno.

## 8. Punti aperti

- **Durata di conservazione delle righe grezze** — proposta 90 giorni, non c'è un termine di legge. Chiude lo
  sviluppatore in sede di manifesto.
- **Consensi importati.** Un file può contenere una colonna «acconsente al marketing». Importarla come prova di
  consenso sarebbe comodo e **scorretto**: la prova richiede momento, testo accettato e canale (storia 0011). La
  proposta è importarla come «legittimo interesse dichiarato dal cliente» con origine «importazione», mai come
  consenso. Va confermato: è una classificazione di dati personali.
