# DeskGrove Support — estensioni della console di amministrazione

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

Le viste comuni della console — account, utenti, abilitazioni, fatturazione, riconciliazione, richieste di
assistenza — bastano per quasi tutto. Restano scoperte tre cose che questa applicazione ha e le altre no: l'app
**riceve dati dall'esterno attraverso canali che si possono rompere** (un dominio di posta non verificato, una
connessione a un fornitore di messaggistica scaduta), **accumula file** il cui costo cresce senza che nessuna
metrica lo governi, e ha una quota **a giacenza** che durante una migrazione può bloccare un cliente in un momento
in cui bloccarlo è il danno peggiore. Nessuna delle tre si diagnostica dalla scheda dell'account.

**Una premessa che qui vale più che altrove.** Su DeskGrove appgrove è **responsabile del trattamento** per conto
del cliente: i dati sono dei clienti finali dell'azienda cliente, persone che con appgrove non hanno alcun
rapporto. Il divieto di guardare i contenuti del cliente non è quindi soltanto la buona regola di piattaforma
(niente impersonificazione): è un **obbligo verso terzi**. Ne discende una regola che attraversa tutto questo
documento: **nessuna vista, nessuna azione e nessun campo espone il testo di un messaggio, l'oggetto di una
richiesta, il nome o il recapito di un richiedente.** Chi amministra vede quanti, quando, in che stato — mai cosa.
Se una diagnosi sembra richiedere il contenuto, la diagnosi è formulata male.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `channel.whatsapp.enabled` | Se l'account può configurare il canale WhatsApp | disattivato | amministratore di piattaforma | Il canale comporta un fornitore intermedio, un contratto e una configurazione assistita: non è una casella che il cliente spunta da solo. Resta chiuso finché non è stato predisposto |
| `attachments.max_total_gb` | Tetto di guardia sullo spazio occupato dagli allegati dell'account | valore prudente uguale per tutti | amministratore di piattaforma | Non è un limite commerciale (la quota è sui posti operatore, §5 del capofila): è una difesa contro l'anomalia — un cliente che carica cento volte il previsto è quasi sempre un difetto o un abuso, non un uso |
| `retention.max_months` | Il massimo che il cliente può impostare come durata di conservazione | valore prudente uguale per tutti | amministratore di piattaforma | La durata la sceglie il cliente, che è il titolare (storia `0036`); il **massimo** consentito è una scelta della piattaforma, che risponde del costo e dell'esposizione |

Nient'altro: tutto il resto della configurazione — orario di servizio, code, politiche di servizio, risposte
predefinite — è del cliente e sta nell'app, dove lui la vede e la cambia.

## 3. Quote e deroghe

- **Metrica governata**: `agents` (posti operatore), natura `stock`.
- **Serve una deroga manuale?** **Sì**, e per un motivo concreto. La metrica è a giacenza: durante il passaggio da
  un altro strumento capita che l'azienda debba tenere **tutti** i suoi operatori attivi per qualche giorno mentre
  smista l'arretrato, e si trova bloccata proprio nel momento in cui lo strumento le serve di più. Un cliente
  bloccato durante una migrazione è un cliente che torna indietro.
- **Forma della deroga**: un **tetto alternativo con data di scadenza**, non una sospensione del blocco. Alla
  scadenza il tetto torna quello del piano; se i posti occupati eccedono, il sistema **non** disattiva nessuno da
  solo — segnala e chiede al cliente di scegliere, perché disattivare un operatore al posto suo significa far
  sparire una persona dalla coda senza che nessuno se ne accorga.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, il tetto concesso e il motivo
  scritto. Nessuna deroga senza motivo.
- **Limite**: una deroga **non è uno sconto** e non cambia l'abbonamento. Se il cliente ha stabilmente bisogno di
  più posti, passa di piano: la deroga serve a superare un momento, non a farne l'abitudine.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Stato dei canali per account** | Per ogni canale: tipo (posta, modulo web, messaggistica), stato della connessione, esito e orario dell'ultima ricezione, codice dell'ultimo errore, stato della verifica del dominio | «Il cliente dice che non gli arrivano più le richieste: è rotto da noi o da lui?» | Metadati: tipo, stato, orario, codice di errore. **Nessun indirizzo di posta di un richiedente, nessun oggetto, nessun contenuto** |
| **Recapiti non riusciti** | Conteggio dei messaggi in uscita respinti o non recapitati per account, per motivo (casella inesistente, respinto come indesiderato, cassetta piena) | «Le nostre risposte arrivano? Stiamo rovinando la reputazione del dominio?» | Conteggi per motivo. **Mai l'indirizzo del destinatario** |
| **Spazio occupato dagli allegati** | Spazio totale per account, andamento negli ultimi mesi, numero di file | «Perché il costo di archiviazione è cresciuto del quaranta per cento?» | Conteggi e dimensioni. **Mai il nome né il contenuto di un file** |
| **Consumo dei posti operatore** | Posti occupati su tetto del piano, con l'eventuale deroga attiva e la sua scadenza | «Perché il cliente dice che non riesce ad aggiungere una persona?» | Conteggi e stato della deroga |
| **Arretrato delle lavorazioni** | Code interne dell'app (ricezione della posta, purga periodica, invio delle indagini di soddisfazione): elementi in attesa, ultimi errori | «C'è un accumulo che si sta trasformando in un guasto?» | Conteggi e codici di errore |
| **Volumi anomali** | Richieste create per account e per ora, con evidenza degli scostamenti forti | «Questo modulo di contatto sta ricevendo messaggi automatici?» | Conteggi nel tempo |

Tutte le viste sono in **sola lettura** e nessuna apre il dettaglio di una richiesta: dalla console non esiste un
percorso che porti al contenuto di una conversazione. È una proprietà da verificare con una prova, non da
raccomandare a parole.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Concedere una deroga sui posti** | Migrazione da un altro strumento, o incidente che richiede più persone per qualche giorno | sì — scade da sola | Chi, quando, fino a quando, tetto concesso, motivo scritto | Diventare una scorciatoia al posto del cambio di piano: per questo ha sempre una scadenza |
| **Ripetere una ricezione fallita** | Un messaggio in ingresso non è diventato una richiesta per un errore transitorio | sì, se l'operazione è idempotente | Riga di controllo con operatore e motivo | Doppia richiesta creata se l'idempotenza non regge: la prova va scritta insieme alla storia `0014` |
| **Riavviare la verifica di un dominio di posta** | Il cliente ha cambiato le impostazioni del proprio dominio e la verifica è rimasta indietro | sì | Riga di controllo | Nessuno rilevante: è una rilettura di configurazione |
| **Sospendere un canale** | Il modulo di contatto di un account è sotto abuso e sta generando migliaia di richieste automatiche | sì — si riattiva | Riga di controllo con motivo obbligatorio e avviso al cliente | **Il cliente smette di ricevere richieste**: è l'azione più invasiva dell'elenco e richiede conferma esplicita e comunicazione al cliente |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni con effetti verso l'esterno o invasive
richiedono una conferma esplicita e non sono **mai** automatiche; **nessuna azione dà accesso ai contenuti
dell'account**. In particolare, non esistono e non vanno introdotte: la lettura di una richiesta, l'invio di un
messaggio al posto del cliente, l'esportazione dei dati di un account dalla console. L'esportazione e la
cancellazione sono percorsi del **cliente**, dentro l'app (storia `0036`), perché è lui il titolare dei dati.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Conteggio delle richieste per account e per periodo | metrica | no | Diagnosi dei volumi e delle anomalie |
| Posti operatore occupati su tetto | metrica | no | Diagnosi delle quote e delle deroghe |
| Stato e tipo di ogni canale, con esito dell'ultima ricezione | metadato di configurazione | no | Diagnosi «non mi arrivano le richieste» |
| Stato della verifica del dominio di posta | metadato di configurazione | no — il dominio è dell'azienda, non di una persona | Diagnosi della recapitabilità |
| Conteggio dei recapiti non riusciti per motivo | metrica | no | Sorveglianza della reputazione di invio |
| Spazio occupato dagli allegati e numero di file | metrica | no | Diagnosi del costo di archiviazione |
| Arretrato e ultimi errori delle lavorazioni interne | metrica + codice di errore | no | Diagnosi degli accumuli |
| Durata di conservazione impostata dal cliente | metadato di configurazione | no | Verifica che il massimo consentito sia rispettato |

**Verifica obbligatoria.** Nessuna riga di questa tabella contiene dati personali, ed è una scelta, non una
coincidenza: su un'applicazione dove appgrove tratta dati di terzi, l'accesso amministrativo sarebbe un
trattamento in più da giustificare verso persone che non hanno alcun rapporto con noi. Il modo più solido di
giustificarlo è **non farlo**. Attenzione al punto in cui la regola rischia di rompersi: i **codici di errore dei
canali** possono contenere l'indirizzo di posta del mittente dentro il testo dell'errore restituito dal fornitore.
Vanno ripuliti prima di essere mostrati, e la prova che lo siano va scritta insieme alla vista.

## 7. Punti aperti

- **Quando un canale sotto abuso vada sospeso d'ufficio.** Sospendere protegge la piattaforma ma interrompe il
  servizio del cliente senza il suo consenso. La proposta è: mai in automatico, sempre con motivo scritto e avviso
  contestuale al cliente. La conferma spetta allo sviluppatore.
- **Se le viste sui canali debbano esistere prima o dopo il canale WhatsApp** (storia `0017`, sospesa a una
  decisione): se quel canale non si farà, la vista resta utile lo stesso per la posta e per il modulo web, ma con
  una colonna in meno.
- **Il massimo di conservazione consentito** e il tetto di guardia sullo spazio degli allegati sono numeri che
  toccano insieme costo, conformità e aspettativa del cliente: li fissa lo sviluppatore, non un agente
  (storia `0036`, §5 del documento capofila).
- **Se la console debba mostrare il numero di richieste contrassegnate «da guardare con attenzione»** — quelle in
  cui il riconoscitore ha visto una parola che potrebbe indicare una categoria particolare. È solo un conteggio e
  non rivela nulla di nessuno, ma è anche un'informazione su come sta andando un presidio di conformità: potrebbe
  servire, oppure essere un'attenzione mal riposta su un dato che non è nostro. Da decidere con la revisione legale.
