# ChatGrove — estensioni della console di amministrazione

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

ChatGrove è l'unica app della sua parte di catalogo che dipende, per funzionare, da una **connessione esterna
che si rompe**: credenziali scadute, numero sospeso dal fornitore, modelli respinti, punteggio di qualità in
caduta. Quando succede, il cliente scrive «non partono più i miei messaggi» e chi assiste, senza poter entrare
nell'account, oggi non avrebbe nulla da guardare. Serve inoltre una deroga temporanea sulla quota per il primo
mese di un cliente che migra da un altro strumento, e una vista sull'arretrato degli invii per capire se il
problema è nostro o del fornitore.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `invii_sospesi` | Interruttore che blocca ogni invio in uscita dell'account, comprese le campagne | spento | amministratore di piattaforma | È una misura di contenimento: si usa quando un account sta danneggiando la reputazione dell'infrastruttura o quando un abuso è in corso. Nelle mani del cliente sarebbe un piede sul freno che nessuno userebbe |
| `tetto_campagna` | Numero massimo di destinatari per singola campagna | non impostato (vale il tetto del piano) | amministratore di piattaforma | Serve a contenere il danno di un account nuovo o segnalato, senza cambiargli il piano |
| `soglia_avviso_fallimenti` | Percentuale di fallimenti oltre la quale l'app avvisa il cliente | 20 % | amministratore di piattaforma | È un valore di taratura, non una preferenza: se lo esponessimo, un cliente lo alzerebbe per far sparire l'avviso invece di risolvere il problema |

## 3. Quote e deroghe

- **Metrica governata**: `messaggi_template` (natura `flow`, finestra mensile).
- **Serve una deroga manuale?** **Sì.** Il caso reale è uno solo e ricorrente: un negozio che arriva da un
  altro strumento con una rubrica già formata manda, il primo mese, molti più messaggi di quanti ne manderà
  poi. Senza deroga il cliente si blocca nella settimana in cui sta decidendo se restare.
- **Forma della deroga**: tetto alternativo **con data di scadenza obbligatoria** (per esempio 5.000 messaggi
  fino al 30 settembre). Alla scadenza si torna al tetto del piano senza alcun intervento.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, il valore e il motivo scritto.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha bisogno stabilmente di
  più, passa di piano. Una deroga rinnovata tre volte è un errore di listino, non un caso di assistenza.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| Stato delle connessioni | Per account: connessione presente sì/no, stato, esito e orario dell'ultima verifica, tipo di errore | «Perché il cliente dice che non gli arrivano più i messaggi?» | Metadati: stato, orario, categoria dell'errore. **Nessuna credenziale, nemmeno mascherata; nessun numero di telefono** |
| Salute degli invii | Per account e per ultime 24 ore e 7 giorni: inviati, consegnati, falliti, percentuale di fallimento | «È un problema del cliente o del fornitore? È un guasto diffuso?» | Conteggi aggregati |
| Arretrato delle lavorazioni | Code di invio delle campagne, arretrato, ultimi errori tecnici | «C'è un accumulo? Stiamo perdendo invii?» | Conteggi, nessun contenuto |
| Consumo della metrica | Per account: consumo del mese, tetto, deroghe attive | «Il cliente è bloccato per quota?» | Numeri |
| Stato dei modelli | Per account: quanti modelli, quanti approvati, quanti sospesi o respinti | «Perché non riesce a fare campagne?» | Conteggi per stato. **Non il testo dei modelli**, che è materiale commerciale del cliente |

**Divieto di impersonificazione.** Nessuna di queste viste mostra conversazioni, messaggi, contatti, ordini o
il testo di ciò che il cliente manda. La domanda «vediamo cosa vede il cliente» qui non si concede: si
risponde con metadati e conteggi. Se un caso di assistenza non si risolve così, si chiede al cliente, non si
guarda.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| Concedere una deroga di quota | Migrazione iniziale, o blocco durante un guasto nostro | sì (scade da sola) | Operatore, data, valore, scadenza, motivo | Diventa uno sconto mascherato se ripetuta |
| Sospendere gli invii dell'account | Abuso in corso, reputazione dell'infrastruttura a rischio | sì | Operatore, data, motivo | Il cliente si ferma di colpo: richiede avviso e motivo scritto |
| Ripetere l'elaborazione di una lavorazione fallita | Una campagna si è fermata per un guasto tecnico nostro | sì | Operatore, motivo, identificativo della lavorazione | **Invii doppi** se l'idempotenza per destinatario non regge: è la ragione per cui la storia `0023` la richiede |
| Forzare l'allineamento dei modelli | Il cliente dice che un modello è approvato ma l'app non lo vede | sì | Operatore, data | Nessuno rilevante |
| Marcare una connessione come da riverificare | Sospetto di credenziale scaduta | sì | Operatore, data | Il cliente vede uno stato in errore: va usata quando è vero |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni con effetti verso l'esterno — in
particolare la ripetizione di una lavorazione di invio — richiedono una conferma esplicita e non sono mai
automatiche; nessuna azione dà accesso ai contenuti dell'account. **Non esiste** un'azione «invia un messaggio
di prova al numero del cliente»: farebbe partire un messaggio vero verso una persona vera dall'account di
qualcun altro.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Stato e orario dell'ultima verifica della connessione | Metadato | no | Diagnosi del guasto più frequente |
| Categoria dell'errore di connessione | Metadato | no | Distinguere credenziale scaduta da numero sospeso |
| Conteggi di invii, consegne e fallimenti per account | Metrica | no | Capire se il guasto è del cliente o diffuso |
| Consumo della metrica `messaggi_template` e deroghe attive | Metrica | no | Diagnosi delle quote |
| Numero di contatti, conversazioni, ordini per account | Metrica | no | Dimensionamento e diagnosi delle prestazioni |
| Conteggi dei modelli per stato di approvazione | Metrica | no | Capire perché le campagne non partono |
| Identificativo dell'operatore che ha compiuto un'azione | Dato di lavoro | sì (personale interno) | Tracciabilità delle azioni amministrative |

**Verifica obbligatoria.** L'unica riga che contiene dati personali riguarda **il nostro personale**, non i
clienti: l'operatore che compie l'azione. Va dichiarata come trattamento nel registro della piattaforma, non
nel manifesto dell'app. Nessuna riga espone dati dei clienti finali del negozio, ed è il presidio più
importante di questo documento: in un'app che tratta il contenuto di comunicazioni personali, l'accesso
amministrativo ai contenuti sarebbe il rischio maggiore di tutto il progetto.

## 7. Punti aperti

- **Se serva una vista di piattaforma sulla reputazione complessiva** dei numeri collegati (quanti account
  hanno modelli sospesi, andamento nel tempo). Sarebbe utile per capire se il prodotto sta inducendo
  comportamenti che il fornitore punisce, ma è una funzione di analisi, non di assistenza: la annoto, non la
  decido.
- **Chi risponde quando il fornitore sospende il numero di un cliente.** È una questione di condizioni di
  servizio e di aspettative, non di console: va scritta nella documentazione dell'app prima del lancio.
  Decisione dello sviluppatore.
