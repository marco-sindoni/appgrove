# AuditGrove — estensioni della console di amministrazione

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

Servono poche cose, e servono per un motivo che vale la pena scrivere subito: questa è l'app in cui **chi
amministra la piattaforma deve poter vedere pochissimo e non deve poter toccare niente**. Le viste comuni
(account, abilitazioni, fatturazione, richieste di assistenza) bastano per il governo ordinario, ma non
rispondono alle due domande di assistenza tipiche di questa app — «perché le mie azioni non arrivano più?» e
«perché la mia coda di approvazioni è ferma?» — che si risolvono su metadati e non sui contenuti del cliente.

Ma la vera ragione per cui questo documento non è la risposta «A» è **il divieto**: da qualche parte deve stare
scritto, in modo esplicito e verificabile, che nessuna azione amministrativa può scrivere, correggere o cancellare
righe del registro di un cliente. Una capacità del genere, se esistesse, distruggerebbe il valore del prodotto per
tutti i clienti insieme — non per quello su cui viene usata: perché la domanda che conta non è «l'avete fatto?»,
è «potreste farlo?».

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `ingestion_rate_ceiling` | Il tetto tecnico di dichiarazioni al secondo accettate da una singola sorgente, a protezione del servizio | valore comune a tutti gli account | amministratore di piattaforma | È una protezione dell'infrastruttura condivisa, non una scelta del cliente: esporlo significherebbe permettere a un account di alzarlo |
| `seal_interval` | La cadenza con cui si produce il sigillo della catena | giornaliera | amministratore di piattaforma | La cadenza incide sul carico di tutti; il cliente sceglie *a chi* consegnare il sigillo (storia 0017), non *ogni quanto* si produce |
| `content_retention_allowed` | Se l'account può attivare la conservazione dei contenuti dei parametri (storia 0031) | attiva | amministratore di piattaforma | È l'interruttore che permette di **spegnere** una funzione rischiosa per un account che non deve averla, per esempio su richiesta del cliente stesso o a valle di una valutazione sui dati personali. Non è una funzione commerciale: è un presidio |

Nessun altro parametro per account: la configurazione dell'app — sorgenti, regole, destinatari degli avvisi,
conservazione dei contenuti — è interamente nelle mani del cliente.

## 3. Quote e deroghe

- **Metrica governata**: `actions` (natura `flow`).
- **Serve una deroga manuale?** **Sì**, e in questa app più che in altre. Il caso reale è preciso: un cliente
  collega una sorgente nuova, sbaglia la configurazione di un agente che entra in raffica e brucia il tetto del
  mese in un pomeriggio. Bloccarlo significa lasciarlo senza registro proprio mentre l'agente sta facendo
  qualcosa di anomalo — cioè togliergli la prova nel momento in cui gli serve.
- **Forma della deroga**: tetto alternativo **con data di scadenza obbligatoria**, mai indeterminato.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando e perché; ed è **una riga del
  registro del cliente**, non solo un record amministrativo. Un tetto alzato da noi è un fatto che riguarda la
  sua prova, e deve poterlo vedere.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha stabilmente bisogno di più,
  passa di piano. Una deroga non può prolungare la **conservazione**: quella è una funzionalità del piano, e
  allungarla a mano creerebbe un cliente che ha prove che il suo piano non prevede.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Stato delle sorgenti** | Per account: numero di sorgenti, stato di ciascuna, momento dell'ultimo contatto, numero di buchi di sequenza rilevati nelle ultime 24 ore | «Perché il cliente dice che non gli arrivano più le azioni?» | Metadati: nome della sorgente, stato, orari, conteggi. **Nessuna azione, nessun parametro, nessuna impronta** |
| **Consumo della metrica** | Andamento delle azioni registrate rispetto al tetto, ingressi rifiutati per quota, banda di cortesia in uso | «Il cliente è al tetto? Da quando?» | Conteggi |
| **Salute dei sigilli** | Per account: momento e esito dell'ultimo sigillo prodotto, momento dell'ultima consegna fuori perimetro, sigilli non consegnati | «Il meccanismo che regge la promessa del prodotto sta funzionando per questo cliente?» | Metadati: momenti, esiti, conteggi. **Non l'impronta di testa** — che è un dato del cliente e non serve alla diagnosi |
| **Arretrato delle approvazioni** | Numero di richieste in attesa e più vecchia in attesa, per account | «Perché il cliente dice che i suoi agenti sono fermi?» | Conteggi e età, **non** lo strumento richiesto né chi ha chiesto |
| **Salute delle lavorazioni programmate** | Code di sigillo, di rapporto periodico e di esportazione: arretrato e ultimi errori tecnici | «C'è un accumulo?» | Conteggi ed errori tecnici |

**Divieto di impersonificazione, applicato.** Nessuna di queste viste mostra righe del registro, parametri,
strumenti chiamati, identificativi di chi ha chiesto o di chi ha approvato. Se una richiesta di assistenza non si
può risolvere con questi metadati, la risposta corretta è chiedere al cliente di guardare, o di produrre
un'esportazione — non guardare noi.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| Concedere una deroga di quota a termine | Il cliente ha sfondato il tetto e sta perdendo prove | sì (scade da sola) | Riga di controllo con operatore, motivo e scadenza, **più una riga nel registro del cliente** | Un tetto alzato e dimenticato diventa un piano regalato: la scadenza è obbligatoria |
| Sospendere l'ingresso da una sorgente | Una sorgente in raffica sta danneggiando il servizio condiviso | sì | Come sopra, e il cliente riceve un avviso | Sospendere l'ingresso significa creare un buco nel registro del cliente: **l'avviso non è cortesia, è dovuto**, e il buco va dichiarato come tale (storia 0011) |
| Ripetere una lavorazione fallita (sigillo, rapporto, esportazione) | Un errore tecnico ha lasciato indietro una lavorazione | sì | Riga di controllo con operatore e motivo | Doppia esecuzione se la lavorazione non è idempotente: il sigillo lo è per costruzione, l'esportazione va resa tale |
| Spegnere per un account la conservazione dei contenuti | Valutazione sui dati personali, o richiesta del cliente | sì | Riga di controllo, avviso al cliente, riga nel registro | Il cliente perde una funzione che stava usando: va concordato, non imposto di sorpresa |

**Azioni deliberatamente NON previste, ed è la parte importante di questo documento.**

- **Nessuna scrittura, correzione o cancellazione di righe del registro di un cliente.** Non esiste il percorso,
  non esiste il permesso, e il ruolo di database non ne ha i privilegi (storia 0002). Vale anche per la
  correzione di una riga palesemente sbagliata: si aggiunge una riga che lo dice, non si tocca quella vecchia.
- **Nessuna produzione di sigilli su richiesta dell'amministrazione**, se non attraverso la stessa lavorazione
  programmata che li produce per tutti: un sigillo prodotto a mano è un sigillo di cui bisogna fidarsi.
- **Nessuna lettura dei contenuti allegati**, in nessuna circostanza e per nessun ruolo. Le chiavi vivono separate
  e la console non le raggiunge.
- **Nessuna decisione di approvazione al posto del cliente.** Se la sua coda è ferma perché nessuno guarda, la
  soluzione è avvisarlo.

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni con effetti verso il cliente richiedono una
conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai contenuti dell'account.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Conteggio delle azioni registrate per account e per periodo | metrica | no | Diagnosi delle quote e delle deroghe |
| Numero di sorgenti per account, con stato e momento dell'ultimo contatto | metadato | no | Prima domanda di assistenza dell'app |
| Nome della sorgente | metadato scritto dal cliente | **potenzialmente sì** | Serve a capire di quale sorgente parla il cliente. ⚠️ Il nome è testo libero: un cliente può chiamare una sorgente col nome di una persona. Va dichiarato nel manifesto dei dati come campo che può contenere un riferimento personale, e la console deve mostrarlo solo dove è indispensabile |
| Conteggio dei buchi di sequenza per sorgente | metrica | no | Diagnosi «non mi arrivano le azioni» |
| Momento ed esito dell'ultimo sigillo, momento dell'ultima consegna | metadato | no | Salute della promessa centrale del prodotto |
| Numero di approvazioni in attesa e età della più vecchia | metrica | no | Diagnosi «i miei agenti sono fermi» |
| Ingressi rifiutati per quota | metrica | no | Decisione sulla deroga |

**Verifica obbligatoria.** L'unica riga con un rischio di dati personali è il **nome della sorgente**, e va
trattata come tale: dichiarata nel manifesto, mostrata solo dove serve, esclusa dalle esportazioni amministrative
aggregate. L'accesso amministrativo è un trattamento come gli altri e va scritto nel registro dei trattamenti.

## 7. Punti aperti

- **Se le deroghe di quota debbano comparire nel registro del cliente.** Propongo di sì, e con decisione: un tetto
  alzato dall'esterno è un fatto che riguarda la sua prova, e nasconderlo sarebbe esattamente il genere di cosa
  che un cliente scopre nel momento peggiore. Ma è una scelta di prodotto: chiude lo sviluppatore.
- **Se la console debba poter vedere l'impronta di testa della catena di un account.** Non serve alla diagnosi, e
  non vederla è una garanzia in più; però renderebbe più veloce rispondere a un cliente che segnala una verifica
  fallita. Propendo per il no. Chiude lo sviluppatore.
- **Come si assiste un cliente che segnala una verifica di integrità fallita** senza guardare i suoi dati. È lo
  scenario di assistenza più grave possibile per questa app e non ha ancora una procedura: va scritto un runbook
  dedicato, presumibilmente basato sul pacchetto di prova che il cliente produce e ci manda lui (storia 0015).
  Chiude: sviluppatore, prima del rilascio.
