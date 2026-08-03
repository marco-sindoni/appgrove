# BookGrove — estensioni della console di amministrazione

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

BookGrove ha tre cose che le viste comuni non governano. La prima sono le **integrazioni con i calendari esterni**:
dipendono da due fornitori terzi, si rompono da sole quando un'autorizzazione scade, e la segnalazione che
arriverà all'assistenza sarà sempre la stessa — «non mi si aggiorna più l'agenda» — a cui non si può rispondere
senza vedere lo stato del collegamento. La seconda è la **coda dei promemoria**: se i messaggi non partono, la
promessa economica dell'app salta e il cliente se ne accorge un giorno dopo. La terza è una **deroga temporanea**
sul tetto delle risorse prenotabili, che serve nel solo caso della migrazione iniziale.

Tutto il resto si governa con le viste comuni. Niente di quanto segue dà accesso ai contenuti dell'account.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `finestra_disponibilita_massima_giorni` | quanto in là nel futuro la pagina pubblica calcola gli spazi liberi in una sola richiesta | 60 | amministratore di piattaforma | è un limite di protezione del servizio, non una scelta commerciale del cliente: alzarlo per un account che ha davvero bisogno di prenotazioni a lungo termine è una decisione operativa |
| `soglia_frequenza_pubblica` | la soglia di richieste per indirizzo di rete sulla superficie pubblica | valore comune | amministratore di piattaforma | va alzata o abbassata quando un account è sotto abuso o quando la soglia comune è troppo stretta per un'attività molto visitata; è un presidio di sicurezza, non una funzione |
| `pagina_pubblica_sospesa` | interruttore di emergenza sulla pagina pubblica dell'account | spenta | amministratore di piattaforma (l'attività ha il proprio interruttore) | serve quando l'abuso è in corso e il cliente non risponde: è un intervento di protezione della piattaforma |

Nessun altro parametro: la configurazione dell'app — servizi, risorse, orari, promemoria, politica di disdetta —
è interamente nelle mani del cliente, ed è giusto che lo resti.

## 3. Quote e deroghe

- **Metrica governata**: `risorse_prenotabili` (natura `stock`, a giacenza).
- **Serve una deroga manuale?** **Sì**, per un caso solo e ben delimitato: la **migrazione iniziale**. Un'attività
  che arriva da un altro programma con otto operatori e sceglie un piano da tre, durante la migrazione ha
  bisogno di tenerne aperte otto per il tempo di sistemare i dati. Senza deroga, il cliente si blocca proprio nel
  momento in cui sta decidendo se restare.
- **Forma della deroga**: tetto alternativo **con data di scadenza obbligatoria**; alla scadenza il tetto torna
  quello del piano, e le risorse in eccesso non si chiudono da sole — si segnalano al cliente, che deve
  intervenire.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando e perché, con il motivo scritto
  obbligatorio.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha bisogno stabilmente di più
  risorse, passa di piano; la deroga rinnovata due volte è il segnale che qualcuno sta usando lo strumento
  sbagliato.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Stato dei collegamenti ai calendari** | per account: quanti collegamenti esistono, con quale fornitore, stato di ciascuno (attivo, scaduto, in errore), momento dell'ultima sincronizzazione, codice dell'ultimo errore | «Perché il cliente dice che non gli si aggiorna più l'agenda?» | metadati: fornitore, stato, orario, codice di errore — **nessun identificativo dell'account esterno, nessuna credenziale, nessun contenuto di calendario** |
| **Coda dei promemoria** | messaggi programmati, inviati, falliti nelle ultime 24 ore e nei prossimi 7 giorni, per account; arretrato e ultimi codici di errore del fornitore | «I promemoria stanno partendo? C'è un accumulo?» | conteggi e codici di errore — **nessun destinatario, nessun testo** |
| **Stato della pagina pubblica** | pubblicata o sospesa, richieste nell'ultima ora, rifiuti per frequenza, prenotazioni pubbliche create | «Questo account è sotto abuso?» | conteggi — **nessun dato dei visitatori, nessun indirizzo di rete in chiaro** |
| **Occupazione della quota** | risorse aperte su tetto del piano, deroghe attive con scadenza | «Perché il cliente non riesce ad aprire un operatore?» | conteggi |

Vale il divieto di impersonificazione: nessuna di queste viste mostra l'agenda, i clienti o i contenuti
dell'account. La domanda «vorrei vedere cosa vede il cliente» va riformulata come diagnostica su metadati, oppure
resta senza risposta.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Concedere una deroga sul tetto delle risorse** | migrazione iniziale da un altro programma | sì (revocabile, e comunque a scadenza) | riga di controllo con operatore, motivo, tetto e scadenza | una deroga dimenticata diventa uno sconto involontario: la scadenza obbligatoria la previene |
| **Ripetere l'invio di un promemoria fallito** | il cliente segnala che un avviso non è arrivato | sì | riga di controllo con operatore e motivo | doppio invio se il primo era in realtà riuscito: l'azione riusa la chiave di idempotenza della storia `0022`, quindi il doppione è escluso per costruzione |
| **Forzare la revoca di un collegamento a un calendario** | il collegamento è in uno stato irrecuperabile e il cliente non riesce a staccarlo | no (va riautorizzato dall'operatore) | riga di controllo con operatore e motivo | l'operatore deve rifare l'autorizzazione: va avvisato prima |
| **Sospendere la pagina pubblica di un account** | abuso in corso, prenotazioni automatiche in massa, contenuti impropri segnalati | sì | riga di controllo con operatore e motivo | l'attività smette di ricevere prenotazioni: è un atto pesante e va usato solo se il cliente non risponde |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni irreversibili o con effetti verso l'esterno
(la revoca di un collegamento, la sospensione della pagina pubblica) richiedono una conferma esplicita e non sono
mai automatiche; nessuna azione dà accesso ai contenuti dell'account. Nessuna azione amministrativa può creare,
spostare o disdire una prenotazione: l'agenda è del cliente.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| conteggio delle risorse aperte per account, con il tetto del piano | metrica | no | diagnosi delle quote e delle deroghe |
| conteggio delle prenotazioni per periodo e per origine (banco o pubblica) | metrica | no | capire se un account è attivo o abbandonato, e se la superficie pubblica gli funziona |
| stato dei collegamenti ai calendari, per fornitore | metadato | no — **a condizione che non compaia l'identificativo dell'account esterno**, che è un dato personale dell'operatore | diagnosi della segnalazione più prevedibile |
| conteggi e codici di errore della coda dei promemoria | metadato | no | diagnosi dei mancati recapiti |
| conteggi delle richieste e dei rifiuti sulla superficie pubblica | metrica | no — gli indirizzi di rete sono **aggregati**, mai esposti singolarmente | riconoscere un abuso in corso |
| deroghe attive, con operatore, motivo e scadenza | metadato amministrativo | contiene l'identità dell'**operatore di piattaforma**, non del cliente | responsabilità delle deroghe concesse |

**Verifica obbligatoria.** Nessuna riga di questa tabella contiene dati personali dei clienti finali dell'attività,
ed è una condizione da verificare in implementazione, non da dare per acquisita: la tentazione di aggiungere «e
mostrami anche l'ultimo messaggio fallito con il destinatario» arriverà alla prima segnalazione difficile, e va
rifiutata. Se in futuro una vista dovesse esporre un dato personale, va dichiarata nel manifesto dell'app e va
scritto perché la console ne ha bisogno: l'accesso amministrativo è un trattamento come gli altri.

## 7. Punti aperti

- **La soglia di frequenza della superficie pubblica è un parametro per account o di piattaforma?** La proposta è
  «di piattaforma, con deroga per account», ma dipende da quanto è variabile il traffico legittimo fra
  un'attività e l'altra: non ho un dato per deciderlo. Chi lo chiude: lo sviluppatore, dopo i primi clienti veri.
- **Chi risponde delle prenotazioni fasulle già entrate in agenda.** La console può sospendere la pagina pubblica,
  ma non può ripulire l'agenda del cliente — sarebbe un accesso ai contenuti. La proposta è che la pulizia resti
  del cliente, con una funzione nell'app che elenchi le prenotazioni pubbliche non verificate di un periodo. È un
  lavoro che non appartiene a nessuna storia scritta: va aggiunto se il problema si presenterà.
