# SubGrove — estensioni della console di amministrazione

**Applicazione**: 19 — SubGrove (`abbonati`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

SubGrove è, fra le app del catalogo esaminate, quella con due caratteristiche che la piattaforma da sola non sa
sorvegliare. Primo: **manda messaggi a persone che non sono nostri utenti** — avvisi di rinnovo (dovuti per legge)
e solleciti — su recapiti che il cliente ha caricato senza che nessuno verifichi da dove vengano; un cliente che
configura male la catena, o che importa una lista sbagliata, produce un volume di posta che ricade sulla
reputazione di spedizione di **tutta** la piattaforma, non solo sulla sua. Secondo: **espone una superficie
pubblica senza credenziali** (la pagina dell'abbonato e il pulsante di disdetta), che è per definizione il
bersaglio più esposto dell'intero prodotto. A queste due si aggiunge una terza ragione, meno drammatica ma più
frequente: l'app fa lavorare una **lavorazione giornaliera** da cui dipende tutto ciò che «succede da solo il
giorno giusto», e il suo arresto silenzioso è il guasto che il cliente scopre per ultimo e nel modo peggiore — un
avviso di legge non partito.

Tutte le estensioni qui descritte sono di **diagnosi e di freno**: nessuna dà accesso ai contenuti dell'account,
nessuna mostra chi sono gli abbonati, nessuna consente di entrare nell'account del cliente.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `tetto_messaggi_abbonato` | Il tetto di sicurezza non aggirabile del numero di messaggi verso lo stesso abbonato in una finestra di tempo (storia `0021`, RF-6) | valore prudente di piattaforma | amministratore di piattaforma | È il presidio contro la molestia: se il cliente potesse alzarlo, non sarebbe un presidio. Si può solo **abbassare** dall'app, mai alzare |
| `tetto_messaggi_account_giorno` | Il volume complessivo di messaggi verso abbonati che un account può produrre in un giorno | valore prudente di piattaforma | amministratore di piattaforma | Protegge la reputazione di spedizione condivisa: un solo account che sbaglia la penalizza per tutti |
| `sospensione_invii` | Freno d'emergenza: sospende ogni invio verso abbonati per quell'account, lasciando l'app per il resto funzionante | spento | amministratore di piattaforma | È una misura di contenimento di un abuso in corso; se fosse nell'app, la spegnerebbe proprio chi la sta causando |
| `frequenza_pubblica` | Il limite di frequenza sulle rotte pubbliche dell'abbonato (storia `0026`) | valore di piattaforma | amministratore di piattaforma | È sicurezza, non una funzione: non si vende e non si configura |

**Regola comune.** Nessuno di questi parametri è un limite commerciale e nessuno si vende: la quota commerciale è
una sola, `abbonamenti_attivi`, e sta nel listino come codice.

## 3. Quote e deroghe

- **Metrica governata**: `abbonamenti_attivi` (natura `stock`).
- **Serve una deroga manuale?** **Sì**, per un caso solo e ben delimitato: la **migrazione iniziale**. Chi arriva
  dal foglio di calcolo carica in un colpo tutti i suoi abbonati e può superare il tetto del piano prima ancora di
  aver capito quale piano gli serve. Rifiutarglielo al primo giorno significa perderlo; regalargli il tetto
  significa non farlo passare mai di piano.
- **Forma della deroga**: **tetto alternativo con data di scadenza**, non sospensione del blocco. Alla scadenza il
  tetto torna quello del piano e, se gli attivi lo eccedono, valgono le regole ordinarie: nessuna creazione nuova
  finché non si rientra o non si passa di piano (regola di piattaforma sulla metrica a giacenza).
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, il tetto alternativo e il motivo
  scritto. Le deroghe sono visibili nella scheda dell'account, anche quando sono scadute.
- **Limite**: una deroga **non è uno sconto** e non cambia l'abbonamento. Se il cliente ha bisogno stabilmente di
  più posti, passa di piano: la deroga serve a dargli il tempo di capirlo, non a evitarglielo.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Comunicazioni verso abbonati** | Per account e per giorno: quanti avvisi di rinnovo e quanti solleciti sono partiti, quanti sono stati respinti dal destinatario, quante volte è scattato il tetto di sicurezza | «Chi sta producendo tutta questa posta?» · «Perché la nostra reputazione di spedizione è peggiorata?» | Conteggi e tassi. **Nessun recapito**, nessun contenuto, nessun nome di abbonato |
| **Superficie pubblica** | Per account: chiamate alle rotte pubbliche, quota di risposte negative, quante volte è scattato il limite di frequenza, quanti collegamenti sono stati richiesti di nuovo | «Qualcuno sta provando a pescare dati dal portale?» · «Il cliente si lamenta che i collegamenti non funzionano: è il limite di frequenza?» | Conteggi e codici di esito. **Nessun gettone**, nessun indirizzo di rete oltre la finestra di conteggio |
| **Lavorazione dei rinnovi** | Ultima esecuzione riuscita per account, giorni eventualmente saltati, arretrato, ultimi errori per tipo | «La lavorazione gira?» · «Da quanto questo account non vede maturare un rinnovo?» | Orari, conteggi, codici di errore. **Nessun contenuto** |
| **Collegamenti al fornitore di incasso** | Per account che usano la storia `0020`: stato della connessione ed esito dell'ultima lettura | «Perché il cliente dice che gli esiti non arrivano più?» | Stato, orario, codice di errore. **Nessuna credenziale**, nessun movimento |

Le viste comuni della console — account, utenti, abilitazioni, fatturazione, riconciliazione, diritti degli
interessati, richieste di assistenza — restano quelle di piattaforma e bastano per tutto il resto.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Ripetere la lavorazione dei rinnovi** per un account e una data | La lavorazione è saltata e il cliente attende un avviso dovuto per legge | sì | riga di controllo con operatore, account, data di riferimento e motivo | Doppia scadenza se la lavorazione non fosse idempotente: lo è per requisito (storia `0012`), e questa azione è il caso che lo mette alla prova |
| **Sospendere gli invii verso abbonati** di un account | Abuso in corso, configurazione palesemente sbagliata, segnalazione di spam | sì | operatore, motivo scritto obbligatorio, momento; il cliente vede nell'app che gli invii sono sospesi e perché | Il cliente non manda più avvisi dovuti per legge: va comunicato subito, ed è il motivo per cui l'azione richiede conferma esplicita |
| **Revocare i collegamenti pubblici** di un account | Sospetto che i gettoni siano circolati o siano stati raccolti | sì (se ne emettono di nuovi) | operatore, motivo, quanti gettoni revocati | Gli abbonati con un collegamento in mano non riescono più a disdire finché non ne ricevono uno nuovo: **è un ostacolo alla disdetta**, quindi l'azione impone di dichiarare come i collegamenti verranno riemessi |
| **Concedere una deroga di quota** (§3) | Migrazione iniziale | sì (scade da sola) | operatore, tetto, scadenza, motivo | Deroga usata come sconto occulto: il tracciamento esiste per renderlo visibile |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni con effetti verso l'esterno o che tolgono una
capacità al cliente richiedono una conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai
contenuti dell'account; **nessuna azione consente di impersonare un utente del cliente** — chi amministra la
piattaforma non entra nell'account e non guarda i dati degli abbonati.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Numero di abbonamenti attivi per account | metrica | no | Diagnosi della quota e delle deroghe |
| Messaggi verso abbonati per account e per giorno, per tipo ed esito | metrica | no — conteggi aggregati | Sorveglianza dell'abuso e della reputazione di spedizione |
| Chiamate e esiti delle rotte pubbliche per account | metrica | no — conteggi e codici | Sorveglianza dell'abuso della superficie senza credenziali |
| Stato e orari della lavorazione dei rinnovi | stato tecnico | no | Diagnosi del guasto silenzioso |
| Stato del collegamento al fornitore di incasso | stato tecnico | no | Assistenza sulla storia `0020` |
| Deroghe di quota concesse, con operatore e motivo | traccia amministrativa | riguarda un **nostro** utente (l'operatore), già trattato dalla piattaforma | Responsabilità di chi concede |

**Verifica obbligatoria.** Nessuna riga di questa tabella espone dati degli **abbonati** (i clienti del cliente):
sono tutte metriche aggregate o stati tecnici. È una scelta deliberata e va tenuta: nel momento in cui alla console
servisse un recapito o un nome per fare assistenza, quella sarebbe una finalità nuova — accesso amministrativo a
dati personali trattati per conto del cliente — da dichiarare nel manifesto dei dati e da concordare con il
cliente titolare, non da aggiungere a una vista esistente.

## 7. Punti aperti

- **Quando il volume di posta diventa un problema di piattaforma e non del singolo account.** La vista mostra i
  numeri, ma la soglia oltre la quale si interviene — e chi decide di intervenire — non è di questa app: è una
  regola di condotta della piattaforma sull'invio. Chiude: **piattaforma**.
- **La revoca dei collegamenti pubblici confligge con l'obbligo di disdetta facile.** L'azione serve, ma toglie
  temporaneamente all'abbonato la via di uscita che la legge pretende. **Proposta**: consentirla solo con
  riemissione contestuale dei collegamenti agli abbonati con abbonamenti vivi. Chiude: **sviluppatore**, con la
  revisione legale.
- **Se la sospensione degli invii debba essere comunicata all'abbonato oltre che al cliente.** Un avviso di rinnovo
  non partito è un problema che ricade sull'abbonato, non sul cliente che ha sbagliato. Chiude: **revisione
  legale**.
