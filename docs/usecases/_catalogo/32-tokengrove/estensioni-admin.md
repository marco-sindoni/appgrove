# TokenGrove — estensioni della console di amministrazione

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

L'app dipende da servizi di terzi che il cliente ci fa interrogare con una propria credenziale: quando smettono di
rispondere, il cliente vede i numeri fermarsi e chiama noi. Senza una vista sullo stato delle fonti, chi risponde
all'assistenza non ha modo di distinguere «la chiave è scaduta» da «il fornitore è lento» da «il cliente non ha mai
finito il collegamento» — e l'unica alternativa sarebbe entrare nell'account, che è vietato. Servono inoltre una
vista sul **catalogo dei prezzi dei fornitori**, che è un dato di piattaforma condiviso e che se invecchia sbaglia i
conti di tutti gli account insieme, e una deroga temporanea sulla quota per chi al primo giorno importa uno storico
lungo.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `frequenza_importazione_rendiconti` | Ogni quanto interroghiamo la fonte del fornitore | 15 minuti | amministratore di piattaforma | È un parametro di carico verso un servizio di terzi con limiti di interrogazione (Anthropic raccomanda una interrogazione al minuto per organizzazione): se lo esponessimo al cliente, il cliente lo porterebbe al minimo e ci farebbe limitare la banda |
| `profondita_importazione_iniziale` | Quanti giorni di storico si recuperano al primo collegamento | 90 giorni | amministratore di piattaforma | Il recupero iniziale è la lavorazione più pesante dell'app e consuma quota: va governata da chi vede il carico complessivo, non dal singolo account |
| `soglia_scarto_riconciliazione` | Oltre quale differenza percentuale fra misure e rendiconto si segnala l'anomalia | 5% | amministratore di piattaforma | È una taratura del prodotto: se fosse del cliente, chi ha lo scarto alto lo alzerebbe finché sparisce l'avviso |
| `eta_massima_catalogo_prezzi` | Da quanti giorni il catalogo dei prezzi può essere vecchio prima che l'account veda l'avvertenza | 30 giorni | amministratore di piattaforma | Dipende da quanto spesso **noi** aggiorniamo il catalogo, non da una preferenza del cliente |

## 3. Quote e deroghe

- **Metrica governata**: `misure_registrate` (natura `flow`).
- **Serve una deroga manuale?** **Sì.** Il caso è preciso e ricorrente: al primo collegamento di una fonte si
  importano fino a 90 giorni di storico, e quel recupero può da solo superare il tetto mensile del piano di un
  cliente che a regime starebbe comodamente dentro. Bloccarlo significherebbe rifiutare il valore proprio nel
  momento in cui il cliente lo sta valutando.
- **Forma della deroga**: tetto alternativo con **data di scadenza obbligatoria** (per esempio 3× il tetto del
  piano per 7 giorni). Non è ammessa una deroga senza scadenza: una deroga permanente è uno sconto travestito.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, quale tetto alternativo e il
  motivo scritto. Compare nella cronologia dell'account.
- **Limite**: la deroga non cambia l'abbonamento e non è uno sconto. Se un cliente ha bisogno stabilmente di più
  misure, passa di piano; se chiede la seconda deroga sullo stesso motivo, la risposta corretta è un cambio di
  piano, non una terza deroga.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| Stato delle fonti per account | Elenco delle fonti collegate: fornitore, tipo (rendiconto o invio), stato, istante dell'ultima importazione riuscita, ritardo osservato, codice dell'ultimo errore | «Perché al cliente si sono fermati i numeri?» | metadati soltanto: fornitore, stato, orari, codice di errore. **Mai** la chiave, mai le misure |
| Arretrato di ricezione | Per account: misure ricevute nell'ultima ora, misure in attesa di lavorazione, misure respinte con il motivo aggregato | «C'è un accumulo? Il cliente sta mandando record non validi?» | conteggi e codici di respingimento aggregati, nessun contenuto della misura |
| Scarto di riconciliazione | Per account e per giorno: differenza percentuale fra somma delle misure e importo dichiarato dal fornitore | «Il cliente dice che i nostri numeri non tornano con la sua fattura: ha ragione?» | percentuali e conteggi; **non** gli importi in chiaro del cliente (vedi §6) |
| Salute del catalogo dei prezzi | Versione corrente, data di pubblicazione, età, numero di modelli, numero di chiavi di modello sconosciute incontrate nell'ultima settimana **in tutta la piattaforma**, ordinate per frequenza | «Quali prezzi mancano e stanno sbagliando i conti a tutti?» | dati di piattaforma, nessun dato di cliente |
| Lavorazioni programmate | Code di importazione, di calcolo del costo, di valutazione dei budget: arretrato, durata media, ultimi errori | «C'è un accumulo generale?» | conteggi |

**Divieto di impersonificazione.** Nessuna di queste viste mostra la spesa del cliente, le sue etichette o i suoi
budget. La domanda «fammi vedere il cruscotto come lo vede lui» non ha una risposta ammessa: si riformula in una
delle viste sopra, oppure si chiede al cliente uno schermo condiviso.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| Forzare una reimportazione di un intervallo di giorni per una fonte | Il fornitore aveva pubblicato dati incompleti e poi li ha corretti | sì (l'importazione è idempotente per identificativo esterno) | riga di controllo con operatore, fonte, intervallo, motivo | Consumo di quota: va concessa insieme alla deroga se l'intervallo è lungo |
| Concedere una deroga di quota a termine | Recupero iniziale dello storico | sì (scade da sola) | operatore, tetto, scadenza, motivo | Deroga usata come sconto: si previene con la scadenza obbligatoria |
| Sospendere l'importazione di una fonte | La fonte fa errori a raffica e stiamo maltrattando il servizio di un terzo | sì | operatore, motivo, istante | Il cliente resta cieco: l'app deve dirglielo, non farlo in silenzio |
| Pubblicare una nuova versione del catalogo dei prezzi | Un fornitore ha cambiato i prezzi | no nel senso che la versione resta nella storia (si pubblica una versione successiva, non si modifica la precedente) | versione, data di validità, origine, impronta del contenuto, operatore | Una versione sbagliata sbaglia i conti di **tutti** gli account: richiede conferma esplicita con anteprima dei modelli toccati |
| Ricalcolare lo storico di un account | Il catalogo aveva un prezzo sbagliato e il cliente ha già visto i numeri | **no** | operatore, account, intervallo, versione di listino di partenza e di arrivo, numero di righe interessate, motivo | È l'azione più pesante dell'app: cambia numeri che il cliente può aver già usato per fatturare. Richiede conferma esplicita, avvisa il cliente e produce righe nuove senza cancellare le vecchie (storia 0017) |

**Regole comuni.** Ogni azione richiede un motivo scritto. Le azioni irreversibili (ricalcolo dello storico,
pubblicazione del catalogo) richiedono una conferma esplicita e non sono mai automatiche. Nessuna azione dà accesso
ai contenuti dell'account. **Nessuna azione permette di leggere una credenziale del cliente**: la chiave di sola
lettura di un fornitore si può revocare o sostituire, mai visualizzare.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Conteggio delle misure registrate per account e periodo | metrica | no | Diagnosi delle quote e delle deroghe |
| Stato, orari e codici di errore delle fonti | metadati | no | Diagnosi del guasto senza entrare nell'account |
| Fornitore e tipo di ciascuna fonte | metadati | no | Sapere con quale interfaccia di terzi si ha a che fare |
| Scarto di riconciliazione in **percentuale** | metrica | no | Rispondere a «i vostri numeri non tornano» senza vedere quanto spende il cliente |
| Codici di respingimento delle misure, aggregati | metrica | no | Capire se il cliente manda record malformati |
| Chiavi di modello sconosciute incontrate (aggregate su tutta la piattaforma) | metadati | no | Sapere quali prezzi mancano nel catalogo |

**Verifica obbligatoria.** Nessuna riga di questa tabella contiene dati personali, ed è una scelta: lo scarto di
riconciliazione è esposto **in percentuale e non in euro** proprio per non far vedere alla console quanto spende un
cliente, che è un dato commerciale suo. Le etichette di attribuzione — le uniche che possono contenere dati
riferibili a persone (§6 del documento capofila) — **non arrivano mai alla console**, in nessuna forma, nemmeno
aggregata: un elenco delle etichette più usate direbbe i nomi dei clienti del nostro cliente.

## 7. Punti aperti

- **Chi pubblica il catalogo dei prezzi dei fornitori e con quale processo di verifica.** L'azione esiste nella
  console, ma la responsabilità operativa (chi controlla che un prezzo sia giusto prima di pubblicarlo, ogni
  quanto) è una decisione di conduzione del servizio, non di questa app. Chiude: lo sviluppatore, insieme al punto
  P6 del documento capofila.
- **Se avvisare il cliente quando un operatore forza una reimportazione.** Consuma la sua quota e cambia i suoi
  numeri, quindi la risposta ragionevole è sì; ma un avviso per ogni intervento di assistenza può essere rumore.
  Chiude: lo sviluppatore.
