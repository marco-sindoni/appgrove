# InvoiceGrove — estensioni della console di amministrazione

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

Questa app dipende da **tre fornitori esterni** (trasmissione al Sistema di Interscambio, punto di accesso alla
rete a quattro angoli, conservatore qualificato) e da **canali con ciclo di vita asincrono**: la domanda che
arriva all'assistenza — «perché al cliente non è partita la fattura?» — non si risponde con la scheda
dell'account, perché la risposta sta nello stato di una connessione, in un arretrato di notifiche o in un
documento fermo. Servono quindi viste diagnostiche su **metadati e conteggi** e due azioni di sblocco. Serve
inoltre una deroga di quota per il caso della migrazione iniziale, che è il momento in cui un cliente nuovo carica
in un mese ciò che ha prodotto in un anno.

Non serve invece nulla sulla configurazione per account: l'app si configura da sola con l'anagrafica del cliente.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `fornitore_trasmissione_it` | Quale fornitore di trasmissione al Sistema di Interscambio serve questo account | il fornitore predefinito della piattaforma | amministratore di piattaforma | Serve a spostare un singolo account su un fornitore alternativo durante un guasto o una migrazione. Al cliente non interessa quale fornitore usiamo, e non deve poterlo scegliere: è una decisione operativa nostra |
| `fornitore_rete_quattro_angoli` | Come sopra, per la rete a quattro angoli | il punto di accesso predefinito | amministratore di piattaforma | Stesso motivo |
| `soglia_documento_fermo` | Dopo quanti giorni un documento in stato non definitivo compare come «fermo» | quella del profilo della giurisdizione | amministratore di piattaforma | È una soglia diagnostica, non una funzione: esporla al cliente produrrebbe solo confusione |

Nessun altro parametro per account: la configurazione fiscale — soggetti emittenti, controparti, recapiti — è
interamente nelle mani del cliente, ed è giusto così perché è responsabilità sua.

## 3. Quote e deroghe

- **Metrica governata**: `documenti` (natura `flow`, finestra mensile).
- **Serve una deroga manuale?** **Sì**, per un caso solo e ben delimitato: la **migrazione iniziale**. Un cliente
  che passa da un altro prodotto porta con sé lo storico e può superare il tetto del piano nel primo mese, per poi
  tornare a un consumo normale. Costringerlo a un piano superiore per un mese è un attrito di ingresso che si paga
  in mancate attivazioni.
- **Forma della deroga**: tetto alternativo con **data di scadenza obbligatoria**, mai una sospensione a tempo
  indeterminato del blocco.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, con quale tetto e **perché**, in
  chiaro.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha bisogno stabilmente di più,
  passa di piano — e la deroga scaduta due volte di fila è il segnale che è sul piano sbagliato.

⚠️ **Nota specifica di questa app**: la deroga **non** si applica alla ricezione dei documenti passivi, perché la
ricezione non si blocca mai (storia `0021`): un documento fiscale in ingresso non si rifiuta per una questione di
abbonamento. Sulla ricezione si registra lo sforamento e si avvisa, e non c'è nulla da derogare.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Stato dei fornitori** | Per ciascuno dei tre fornitori: raggiungibilità, esito e orario dell'ultima chiamata riuscita, tasso di errore recente, arretrato | «Il problema è del cliente o è nostro?» | Metadati: stato, orario, codice di errore, conteggi. **Nessun contenuto di documento** |
| **Documenti fermi per account** | Quanti documenti sono in stato non definitivo oltre la soglia, raggruppati per stato e giurisdizione | «Perché il cliente dice che non gli parte niente?» | **Conteggi soltanto**: nessun numero di documento, nessuna denominazione, nessun importo |
| **Arretrato delle notifiche** | Notifiche in attesa di acquisizione, non elaborate, orfane, per fornitore | «C'è un accumulo che riguarda tutti o solo lui?» | Conteggi e codici di errore |
| **Lavorazioni differite** | Composizione dei pacchetti di conservazione, versamenti, esportazioni: code, arretrato, ultimi errori | «Perché i documenti non arrivano in archivio?» | Conteggi e stati |
| **Codici di diagnosi non tradotti** | I codici di errore incontrati e non presenti nel dizionario, con la frequenza | «Quali messaggi dobbiamo ancora scrivere?» | Codici e conteggi, aggregati su tutti gli account |
| **Stato di conservazione per account** | Quanti documenti conservati, quanti non conservati, la scadenza dell'obbligo più lontana | «Cosa succede se questo cliente disdice?» | Conteggi e date |
| **Scadenze di mandato in arrivo** | Quali account hanno soggetti emittenti toccati da un obbligo entro i prossimi mesi | «Chi va avvisato prima che sia tardi?» | Conteggi per paese, **senza** l'anagrafica dei soggetti |

⚠️ **Divieto di impersonificazione, applicato a questa app.** Chi amministra la piattaforma **non** vede il
contenuto delle fatture dei clienti, né i loro numeri di documento, né le denominazioni delle controparti, né gli
importi. Sono documenti fiscali di terzi: vederli non è mai necessario per rispondere a una domanda di assistenza,
e ogni richiesta di «vedere la fattura del cliente» va riformulata come diagnostica sui metadati. Se un giorno
emergesse un caso in cui i metadati non bastano, **è un punto aperto da discutere**, non una deroga da concedere.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Ripetere l'acquisizione di una notifica non elaborata** | Il fornitore ha consegnato una notifica che non siamo riusciti a interpretare e il difetto è stato corretto | sì | riga di controllo con operatore, motivo, identificativo della notifica | Doppia acquisizione se l'idempotenza non regge: è coperta dalla storia `0019`, ma va verificata prima di usare l'azione |
| **Ripetere la composizione di un pacchetto di conservazione** | Il pacchetto è rimasto in attesa per un difetto nostro, poi risolto | sì | come sopra | Nessuno: la composizione è deterministica (storia `0022`) |
| **Concedere una deroga di quota a scadenza** | Migrazione iniziale di un cliente nuovo | sì (scade da sola) | operatore, motivo, tetto, scadenza | Che diventi permanente per dimenticanza: da qui l'obbligo della data di scadenza |
| **Spostare un account su un fornitore alternativo** | Guasto prolungato del fornitore predefinito | sì | operatore, motivo, fornitore di partenza e di arrivo | Documenti in volo sul fornitore precedente: l'azione va eseguita solo con la coda vuota, e la vista dell'arretrato serve proprio a saperlo |

**Azioni esplicitamente NON previste, e il motivo.**

- ❌ **Ritrasmettere un documento all'autorità.** È un effetto irreversibile verso l'esterno su un documento
  fiscale di un cliente: non lo fa chi amministra la piattaforma, lo fa il titolare dal suo account con la sua
  conferma (storie `0017` e `0029`). Nessuna urgenza di assistenza giustifica il contrario.
- ❌ **Versare un pacchetto in conservazione.** Stesso motivo: avvia una custodia decennale presso un terzo.
- ❌ **Modificare o cancellare un documento di un cliente.** Mai, per nessun motivo. La cancellazione passa dai
  diritti dell'interessato (storia `0026`), con il suo perimetro e la sua riga di prova.
- ❌ **Scaricare l'archivio di un cliente.** È il contenuto, non i metadati.

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni **irreversibili** o con effetti verso
l'esterno non esistono in questo elenco, ed è deliberato; nessuna azione dà accesso ai contenuti dell'account.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Conteggio dei documenti per account, stato e giurisdizione | metrica | no | Diagnosi delle quote e dei blocchi |
| Consumo della metrica `documenti` nella finestra corrente | metrica | no | Capire se il cliente è al tetto |
| Stato e arretrato di ciascun fornitore, per account e complessivo | metadato tecnico | no | Distinguere un guasto nostro da un problema del cliente |
| Codici di errore ricevuti dai canali, con frequenza | metadato tecnico | no | Diagnosi e manutenzione del dizionario |
| Numero di soggetti emittenti attivi per account, con il **solo paese** | metadato | ⚠️ **potenzialmente** — la denominazione di una ditta individuale è dato personale, per questo **non** si espone: si espone il conteggio e il paese | Verificare i limiti di piano e le scadenze di mandato |
| Documenti conservati per account e scadenza dell'obbligo più lontana | metrica e data | no | Capire l'impatto di una disdetta |
| Deroghe di quota concesse, con operatore, motivo e scadenza | traccia amministrativa | contiene l'identificativo dell'**operatore di piattaforma**, non del cliente | Controllo interno |

**Verifica obbligatoria.** Nessuna riga di questa tabella espone dati personali di clienti o di controparti, ed è
una proprietà **da preservare deliberatamente**: basterebbe aggiungere «denominazione del soggetto emittente» a
una vista diagnostica per trasformare la console in un trattamento di dati personali che oggi non è, con quel che
ne segue nel manifesto e nell'informativa. La riga sui soggetti emittenti è marcata perché è quella dove la
tentazione è più forte.

## 7. Punti aperti

- **Se i metadati bastino davvero** per l'assistenza su un dominio in cui il problema è spesso *dentro* il
  documento (un campo sbagliato, una natura di operazione mancante). La mia proposta è che bastino, perché la
  storia `0015` porta la diagnosi **dentro l'app**, dove il cliente la vede e la può leggere all'assistenza. Se
  nella pratica non bastasse, è una discussione da fare — non una deroga da concedere di fatto.
- **Chi avvisa i clienti toccati da una scadenza di mandato in arrivo** e con quale strumento. La vista
  diagnostica dice chi sono; l'avviso in uscita è un tema di piattaforma (storia `0010`, fuori ambito).
- **Se serva una vista sui costi verso i fornitori per account** — quanto ci costa davvero un cliente. Sarebbe
  utile per capire se il listino regge (descrizione dell'applicazione §5), ma è un tema di amministrazione
  economica, non di assistenza: va deciso dove si decidono le viste economiche della piattaforma, non qui.
