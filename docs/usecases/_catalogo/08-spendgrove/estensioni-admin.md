# SpendGrove — estensioni della console di amministrazione

**Applicazione**: 08 — SpendGrove (`notespese`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

Tre cose che la console comune non può dare. La prima è la conseguenza diretta di avere un **fornitore esterno di
lettura automatica**: quando un cliente scrive «l'app non legge più le mie ricevute», la risposta sta in un numero
che nessuna vista comune conosce — quante letture stanno fallendo, da quando, e se il problema è di quel cliente o
di tutti. La seconda è la **deroga temporanea sulla quota**, perché il cliente che arriva da un anno di scontrini in
una scatola supera il tetto del piano il primo mese e mai più. La terza è la possibilità di **ripetere una lettura
fallita** senza far pagare al cliente un difetto nostro.

Nessuna delle tre dà accesso ai contenuti dell'account: si vedono conteggi, stati e codici di errore, mai una
ricevuta, mai un importo, mai il nome di un collaboratore.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `lettura_automatica_attiva` | Spegne la lettura automatica per un singolo account, lasciando l'inserimento manuale | attiva | amministratore di piattaforma | Serve a contenere un incidente (fornitore degradato, costi anomali) senza spegnere l'app a tutti. Il cliente non deve poterla spegnere per sbaglio: perderebbe la funzione per cui paga |
| `fornitore_lettura` | Quale realizzazione dell'estrattore usare per quell'account | quella predefinita | amministratore di piattaforma | Serve a spostare un cliente su un fornitore alternativo durante un guasto o una prova. È una scelta tecnica e contrattuale, non una preferenza del cliente |
| `soglia_fiducia` | Sotto quale fiducia un campo si mostra «da controllare» | 80 | amministratore di piattaforma | È un numero da tarare sui dati veri (punto aperto della storia `0007`): finché non è stabile, cambiarlo per un cliente in assistenza è più utile che esporlo a tutti |

Nessun altro parametro per account: categorie, politiche, diarie, tariffe chilometriche e regole di indetraibilità
sono **del cliente** e stanno nell'app, dove le vede e le cambia lui.

## 3. Quote e deroghe

- **Metrica governata**: `receipts` — ricevute elaborate (natura `flow`, finestra mensile).
- **Serve una deroga manuale?** **Sì**, e per un motivo ricorrente e prevedibile: la **migrazione iniziale**. Il
  cliente che entra con un arretrato di scontrini supera il tetto del piano nel primo mese e poi si stabilizza. Senza
  deroga, la scelta è fra bloccarlo il giorno dell'attivazione — cioè perderlo — e fargli comprare un piano che non
  gli serve.
- **Forma della deroga**: tetto alternativo per la sola finestra corrente, **con data di scadenza obbligatoria**.
  Non è una modifica del piano e non tocca l'abbonamento: alla scadenza il tetto torna quello del listino, senza
  che nessuno debba ricordarsene.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, con quale tetto e **perché** (motivo
  scritto obbligatorio).
- **Limite**: una deroga non è uno sconto. Se un cliente ne chiede una seconda, la risposta giusta è un cambio di
  piano, e la console deve rendere evidente quante ne ha già avute.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Salute della lettura automatica** | Per fornitore e per periodo: numero di letture riuscite, parziali e fallite, tempo di risposta mediano, fiducia media, tasso di errore | «Il fornitore sta degradando?» «È un problema di tutti o di quel cliente?» | Conteggi, tempi, codici di errore. **Nessun contenuto, nessuna immagine** |
| **Qualità per account** | Per account: quante letture, quanti campi corretti dalla persona, quale campo si corregge più spesso | «Perché questo cliente dice che l'app legge male?» — e, sul lungo periodo, «vale la pena cambiare fornitore?» | Conteggi e **nomi dei campi**, mai i valori |
| **Consumo della quota** | Consumo corrente, tetto, deroghe attive e storiche per account | «Perché il cliente riceve il blocco?» | Numeri |
| **Arretrato delle lavorazioni** | Code della lettura asincrona e della produzione dei pacchetti: lunghezza, età dell'elemento più vecchio, ultimi errori per codice | «C'è un accumulo?» | Conteggi e codici |
| **Copertura della conservazione** | Per account: quali periodi risultano versati e quali no, secondo l'esito dichiarato dal cliente | «Il cliente sostiene di aver perso una consegna» | Stati e date, nessun documento |

Le viste comuni della console — account, utenti, abilitazioni, fatturazione, richieste di assistenza — bastano per
tutto il resto.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Ripetere una lettura fallita** | Il fornitore ha avuto un guasto e alcune ricevute sono rimaste senza esito | sì (aggiunge un esito, non ne toglie) | riga di controllo con operatore, motivo e numero di elementi | Costo verso il fornitore; va limitata a un numero massimo per intervento |
| **Concedere una deroga di quota** | Migrazione iniziale, o blocco durante un guasto nostro | sì (scade da sola) | operatore, motivo, tetto, scadenza | Diventare un'abitudine al posto del cambio di piano |
| **Spegnere la lettura automatica per un account** | Costi anomali, fornitore degradato, richiesta del cliente | sì | operatore, motivo, momento | Il cliente perde la funzione principale: va comunicato, non fatto in silenzio |
| **Rimettere in coda una produzione di pacchetto fallita** | Il cliente non riesce a produrre l'esportazione del trimestre | sì (la produzione è idempotente per periodo) | operatore, motivo, periodo | Doppia produzione: si risolve con il congelamento del pacchetto per periodo |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni irreversibili o con effetti verso l'esterno
richiedono una conferma esplicita e non sono mai automatiche; **nessuna azione dà accesso ai contenuti
dell'account**.

**Due azioni chieste e non ammesse**, che vanno scritte perché prima o poi qualcuno le proporrà:

- **«Guardare la ricevuta del cliente per capire perché la lettura sbaglia».** È impersonificazione mascherata da
  diagnostica. La riformulazione ammessa è la vista «qualità per account», che dice **quale campo** si corregge più
  spesso senza mostrare nessun documento. Se un caso non si risolve così, si chiede al cliente di inviare un
  esempio, e diventa un conferimento suo, consapevole e circoscritto.
- **«Correggere un dato al posto del cliente».** Le spese, le note e le approvazioni sono atti dell'azienda cliente:
  chi amministra la piattaforma non li scrive, nemmeno per fare un favore. La strada è spiegargli come si fa.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Conteggio delle letture per esito, per account e per fornitore | metrica | no | Diagnosi dei guasti del fornitore |
| Tempo di risposta e fiducia media della lettura | metrica | no | Rilevare un degrado prima che lo segnalino i clienti |
| Numero di correzioni per **nome** di campo | metrica | no — il nome del campo non è un valore | Capire dove la lettura sbaglia e se conviene cambiare fornitore |
| Consumo e tetto della metrica `receipts` | metrica | no | Diagnosi dei blocchi |
| Deroghe concesse, con operatore, motivo e scadenza | traccia amministrativa | dati dell'**operatore di piattaforma**, non del cliente | Responsabilità delle deroghe |
| Lunghezza e arretrato delle code | metrica | no | Rilevare accumuli |
| Stato della copertura di conservazione per periodo | stato | no | Assistenza sulle consegne |

**Verifica obbligatoria.** Nessuna riga di questa tabella contiene dati personali dei collaboratori del cliente, ed
è una condizione da mantenere: l'accesso amministrativo è un trattamento come gli altri, e ogni dato che si
aggiungesse qui andrebbe dichiarato nel manifesto dell'app. La sola riga con dati personali riguarda **gli
operatori della piattaforma** — chi ha concesso una deroga — e ricade sotto il trattamento del personale interno,
non sotto quello dei clienti.

## 7. Punti aperti

- **Per quanto conservare le misure di qualità della lettura**: servono a decidere se cambiare fornitore, quindi
  vanno tenute per più di qualche settimana; ma sono comunque dati che si accumulano. Serve una durata dichiarata.
- **Se le misure di qualità possano essere aggregate fra account** per valutare il fornitore: aggregare conteggi
  tecnici non è un uso secondario dei dati dei clienti, ma il confine va scritto nero su bianco prima di farlo, non
  dopo.
- **Chi paga le ripetizioni di lettura** quando il guasto è del fornitore e non nostro: è una questione contrattuale
  con il fornitore, e la chiude lo sviluppatore insieme al punto aperto n. 2 della descrizione dell'applicazione.
