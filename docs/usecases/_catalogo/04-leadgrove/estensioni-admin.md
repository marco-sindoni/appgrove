# LeadGrove — estensioni della console di amministrazione

**Applicazione**: 04 — LeadGrove (`sales`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

LeadGrove ha una cosa che la maggior parte delle app della suite non ha: una **superficie pubblica raggiungibile
senza autenticazione**, il modulo web di acquisizione (storia 0028). È l'unico punto dell'app che uno sconosciuto
può interrogare, quindi l'unico che può essere inondato di invii falsi, e chi amministra la piattaforma deve poter
vedere che sta succedendo e spegnerlo. Servono poi due cose minori ma ricorrenti nell'assistenza: lo stato delle
**importazioni** (è la prima operazione che un cliente nuovo fa, e quando va storta chiama) e una **deroga
temporanea sui posti** per chi sta migrando da un altro prodotto. Tutto il resto — abbonamenti, abilitazioni,
fatturazione, diritti degli interessati — si governa con le viste comuni.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `web_form.rate_limit_per_minute` | Quanti invii al minuto accetta un modulo pubblico dallo stesso indirizzo di rete | valore di piattaforma | amministratore di piattaforma | È un presidio contro gli abusi: se lo governasse il cliente, il primo effetto sarebbe che chi subisce un abuso lo alza per «far passare tutto» |
| `web_form.enabled_override` | Interruttore che spegne **tutti** i moduli pubblici di un account, indipendentemente da come li ha configurati il cliente | acceso | amministratore di piattaforma | È la leva di emergenza quando un modulo viene usato per inondare l'archivio o per inviare contenuti illeciti. Non può stare nelle mani di chi subisce l'abuso, che spesso non se ne accorge |
| `import.max_rows` | Numero massimo di righe per singola importazione | valore di piattaforma | amministratore di piattaforma | È un limite di risorse condivise, non una funzione: alzarlo per un cliente in migrazione è una scelta operativa |

## 3. Quote e deroghe

- **Metrica governata**: `seats` (natura `stock`, a giacenza).
- **Serve una deroga manuale?** **Sì**, per un caso preciso: il cliente che sta migrando da un altro prodotto e ha
  bisogno per qualche settimana di più posti di quanti il suo piano ne preveda, perché durante la migrazione
  lavorano insieme le persone che entrano e quelle che devono ancora uscire dal vecchio sistema. È una richiesta
  concreta e temporanea, e senza deroga la risposta sarebbe «passi di piano e poi torni indietro», che con una
  metrica a giacenza è scomodo e con i prezzi immutabili è peggio.
- **Forma della deroga**: tetto alternativo sulla metrica `seats`, con **data di scadenza obbligatoria** (proposta:
  non oltre 60 giorni). Alla scadenza il tetto torna quello del piano; i posti in eccesso **non** vengono revocati
  in automatico — l'account entra in uno stato «oltre il tetto» che blocca le nuove assegnazioni e che
  l'amministratore vede.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, il tetto concesso e il motivo
  scritto.
- **Limite**: una deroga **non è uno sconto** e non cambia l'abbonamento. Se il cliente ha bisogno stabilmente di
  più posti, passa di piano. Una deroga rinnovata due volte è un piano sbagliato, non una necessità.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Moduli web pubblici** | Per ogni account: numero di moduli attivi, invii ricevuti nelle ultime 24 ore e negli ultimi 7 giorni, invii respinti per limite di frequenza, invii respinti come automatici | «Questo account sta subendo un abuso?» e «perché il cliente dice che gli arrivano solo richieste false?» | Conteggi e stati. **Nessun contenuto degli invii**, nessun indirizzo di posta, nessun indirizzo di rete completo |
| **Importazioni** | Ultime importazioni per account: momento, righe totali, create, saltate, scartate, motivo prevalente dello scarto | «Perché il cliente dice che l'importazione non ha funzionato?» | Conteggi e **codici** di motivo (per esempio «nome mancante»), mai le righe |
| **Posti** | Posti occupati, tetto del piano, eventuale deroga attiva con scadenza | «Perché non riesce ad aggiungere un venditore?» | Conteggi e identificativi interni dei membri, mai i loro nomi |
| **Salute del servizio** | Errori per rotta, durata delle interrogazioni dei rapporti, arretrato delle esportazioni asincrone | «È un problema del cliente o nostro?» | Metriche aggregate |

**Divieto di impersonificazione.** Nessuna di queste viste mostra contenuti dell'account: non si vedono contatti,
trattative, note né invii. Se in assistenza serve capire *cosa* c'è dentro un dato, la strada è chiedere al cliente
o guidarlo, mai guardare al posto suo.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Spegnere i moduli pubblici di un account** | Un modulo viene inondato di invii falsi o usato per inviare contenuti illeciti | sì (si riaccende) | riga di controllo con operatore, momento e motivo scritto | Il cliente smette di ricevere richieste vere: va avvisato, e l'azione richiede un motivo |
| **Concedere una deroga sui posti** | Migrazione da un altro prodotto | sì (scade da sola) | operatore, motivo, tetto concesso, scadenza | Diventare la via ordinaria per non far passare di piano |
| **Ripetere l'elaborazione di un'importazione fallita** | Il cliente segnala un caricamento interrotto a metà | sì, ma va fatta con attenzione | operatore, motivo, identificativo dell'importazione | **Doppia elaborazione**: l'operazione dev'essere idempotente rispetto all'importazione, altrimenti raddoppia i contatti |
| **Rimuovere un file di esportazione prima della scadenza** | Il cliente segnala di aver esportato per errore dati che non voleva | no (il file non torna) | operatore, motivo, identificativo dell'esportazione | Nessuno rilevante: rimuovere un file è la direzione prudente |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni **irreversibili** o con effetti verso
l'esterno richiedono una conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai contenuti
dell'account. Nessuna azione della console **scrive** dati di dominio del cliente: non si creano contatti, non si
spostano trattative, non si chiudono affari per conto suo.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Numero di contatti, aziende e trattative per account | metrica | no | Capire se l'account è vuoto (cliente da aiutare) o pieno (cliente attivo) |
| Posti occupati e tetto | metrica | no | Diagnosi delle quote |
| Invii ricevuti e respinti per modulo web | metrica | no | Rilevare gli abusi sulla superficie pubblica |
| Esiti delle importazioni con codici di scarto | metrica + codici | no | Assistenza sul primo giorno del cliente |
| Registro delle esportazioni: chi, quando, tipo, numero di righe | metadato | **l'identificativo dell'operatore del cliente**, non il contenuto | Ricostruire un'uscita di dati se il cliente la segnala |
| Deroghe concesse | metadato | l'identificativo dell'amministratore di piattaforma | Rendicontazione delle deroghe |

**Verifica obbligatoria.** L'unica riga che tocca dati di persone è il registro delle esportazioni, e solo per
l'identificativo interno di chi ha esportato — non il suo nome, non il contenuto del file. Serve perché una
segnalazione del tipo «i nostri contatti sono usciti» si risolve solo sapendo chi ha esportato e quando: senza,
l'assistenza non può rispondere. L'accesso amministrativo è un trattamento come gli altri e va dichiarato nel
manifesto dati dell'app.

## 7. Punti aperti

- **Soglia di allarme sugli invii dei moduli pubblici.** Non ho un valore di riferimento: quante richieste al
  giorno siano «normali» per il sito di una micro-impresa non è un dato che ho trovato. La proposta è non fissare
  una soglia assoluta ma segnalare gli scostamenti dal comportamento abituale dell'account, e comunque partire
  guardando i dati reali dopo qualche mese. Chiude lo sviluppatore.
- **Durata massima di una deroga sui posti.** Proposta 60 giorni; è una decisione commerciale, non tecnica.
- **Chi avvisa il cliente quando i suoi moduli pubblici vengono spenti d'ufficio.** L'azione è nostra e l'effetto
  lo subisce lui: serve un percorso di comunicazione, che è di piattaforma (assistenza) e non di questa app.
