# StockGrove — estensioni della console di amministrazione

**Applicazione**: 14 — StockGrove (`magazzino`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

Le viste comuni della console (account, utenti, abilitazioni, fatturazione, diritti degli interessati, richieste di
assistenza) coprono quasi tutto, ma tre cose restano scoperte e sono tutte specifiche di questa applicazione. La
prima è **la migrazione iniziale**: chi importa la propria anagrafica supera il tetto del piano per qualche giorno, e
senza una deroga a termine il cliente si ferma prima ancora di aver visto il prodotto (storie
[`0011`](02-anagrafiche-e-catalogo-prodotti/0011-importazione-dell-anagrafica-da-file.md) e
[`0018`](03-registro-dei-movimenti-e-giacenze/0018-importazione-massiva-dei-movimenti.md)). La seconda è **lo stato
delle lavorazioni asincrone** — eventi ricevuti dalle altre app, eventi pubblicati, importazioni da file: quando un
cliente dice «non mi arrivano più gli scarichi delle vendite», la risposta si trova lì e in nessun altro posto. La
terza è il **contatore delle divergenze fra registro e proiezione**, che è il segnale di salute proprio di questa
app: se in un account la somma dei movimenti smette di coincidere con la giacenza pubblicata, lo si deve sapere
prima che lo scopra il cliente — perché il cliente lo scopre promettendo merce che non ha, e a quel punto non si
fida più (descrizione dell'applicazione, §11, «rischi noti»).

Nessuna delle tre guarda dentro i dati del cliente: sono conteggi, stati e codici di errore.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `verifica_giacenza.cadenza` | ogni quanto gira la ricostruzione della giacenza dal registro ([`0024`](04-inventario-fisico-e-rettifiche/0024-ricostruzione-della-giacenza-dal-registro.md)) per quell'account | giornaliera | amministratore di piattaforma | è una scelta di esercizio, non di prodotto: su un account con milioni di movimenti la si dirada, su un account appena migrato la si infittisce per qualche giorno. Al cliente non serve saperlo e non saprebbe cosa scegliere |
| `eventi_in_ingresso.sospensione` | mette in pausa il consumo degli eventi in arrivo dalle altre app ([`0019`](03-registro-dei-movimenti-e-giacenze/0019-movimenti-dagli-eventi-delle-altre-app.md)) per quell'account | attivo | amministratore di piattaforma | serve solo durante un incidente, per smettere di applicare movimenti sbagliati mentre si indaga. Esporlo al cliente significherebbe offrirgli un interruttore che gli corrompe il saldo |

**Limite comune.** Entrambi i parametri sono **di esercizio**, non commerciali: nessuno dei due allarga funzioni o
limiti del piano, e nessuno dei due si usa per fare uno sconto tecnico.

## 3. Quote e deroghe

- **Metrica governata**: `articoli_gestiti` (natura `stock`).
- **Serve una deroga manuale?** **Sì**, e per un caso solo, ma frequente: la **migrazione iniziale**. Il valore di
  StockGrove si vede dopo aver caricato l'anagrafica vera, e un cliente che ne ha 640 su un piano da 500 va
  sbloccato per il tempo di capire se gli serve il piano superiore — non respinto sulla porta. Senza deroga la
  risposta operativa sarebbe «cambia piano prima di aver visto il prodotto», che è il modo migliore per perderlo.
- **Forma della deroga**: **tetto alternativo con data di scadenza obbligatoria** (proposta: al massimo 30 giorni e
  al massimo il doppio del tetto del piano). Alla scadenza il tetto torna quello del piano: gli articoli in eccesso
  **non** vengono cancellati né disattivati — restano e diventano di sola lettura per l'aggiunta, cioè non se ne
  creano di nuovi finché il cliente non archivia o non cambia piano. Cancellare merce censita per far tornare un
  contatore sarebbe un danno, non un'applicazione delle regole.
- **Quello che la deroga non tocca mai**: i **movimenti**. Non sono soggetti a quota per scelta di prodotto
  (descrizione, §5), quindi non c'è niente da derogare — ed è bene che chi amministra lo sappia, perché la domanda
  «gli sblocco anche gli scarichi?» arriverà.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, con quale motivo scritto, e resta
  visibile nella scheda dell'account anche dopo la scadenza.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha stabilmente più articoli di
  quanti il piano ne preveda, la risposta è il piano superiore; la seconda deroga consecutiva sullo stesso account
  è un segnale commerciale, non una soluzione tecnica.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Salute del registro** | per account: numero di divergenze fra somma dei movimenti e giacenza pubblicata rilevate dall'ultima verifica, momento dell'ultima esecuzione, scarto massimo, esito (nessuna divergenza / riparata / mai eseguita) | «C'è un account in cui il saldo ha smesso di dire la verità?» — ed è la domanda più importante di questa app | conteggi, momenti, codici di articolo e deposito **solo** come identificativi tecnici; nessuna descrizione, nessun valore, nessun nome |
| **Eventi in ingresso** | code degli eventi ricevuti dalle altre app: quanti applicati, quanti in attesa, quanti non applicabili (articolo sconosciuto) e con quale codice di errore, arretrato più vecchio | «Perché al cliente non arrivano più gli scarichi delle vendite?» | conteggi, codici di errore, identificativo dell'app di origine; **nessun contenuto** dell'evento |
| **Eventi in uscita** | coda di `giacenza.variata` ([`0020`](03-registro-dei-movimenti-e-giacenze/0020-evento-giacenza-variata.md)): pubblicati, in attesa, falliti con motivo | «Le app a valle stanno ricevendo?» | conteggi e stati |
| **Importazioni da file** | ultime importazioni di anagrafica e di movimenti per account: esito, righe accettate, righe respinte, codice del primo errore, quando | «Il cliente dice che l'importazione non ha funzionato» | conteggi e codici di errore; **mai** il contenuto del file, che è dato del cliente |
| **Verifiche pianificate** | esecuzione della ricostruzione dal registro su tutti gli account: ultima esecuzione, durata, account saltati | «Il controllo di salute sta girando davvero?» | conteggi e tempi |

**Vietato.** Nessuna vista mostra articoli, descrizioni, quantità, fornitori o note del cliente: il codice
dell'articolo compare solo dove serve a localizzare una divergenza, e la descrizione non compare mai. Non esiste, e
non deve esistere, un modo per «vedere il magazzino del cliente»: se una richiesta di assistenza lo esige, si
chiede al cliente di guardare lui e di dire cosa vede (divieto di impersonificazione, modello del kit).

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Concedere una deroga al tetto** | migrazione iniziale, o cliente in attesa del cambio di piano | sì (si revoca; la scadenza è comunque obbligatoria) | operatore, motivo scritto, tetto concesso, scadenza | trasformarla in uno sconto permanente: la seconda consecutiva va segnalata al commerciale |
| **Rieseguire la ricostruzione della giacenza** su un account | la vista «salute del registro» segnala divergenze | sì — è un ricalcolo, non produce movimenti | operatore, motivo, esito con numero di righe riparate | nessuno sul dato: la verità resta il registro. Su un account grande costa tempo di elaborazione: va pianificata, non lanciata a raffica |
| **Ripetere il consumo di un evento fallito** | un evento di vendita non è diventato scarico per un guasto temporaneo | sì, perché il consumo è idempotente sulla coppia (account, identificativo dell'evento) | operatore, motivo, identificativo dell'evento | se l'idempotenza fosse rotta si conterebbe due volte lo stesso fatto: l'azione va eseguita solo dopo aver visto la vista degli eventi in ingresso, mai «alla cieca» |
| **Sospendere e riattivare il consumo degli eventi** per un account | incidente in corso su un'app che sta emettendo eventi sbagliati | sì | operatore, motivo, durata della sospensione | mentre è sospeso il saldo del cliente resta indietro rispetto alle vendite: va detto al cliente, non nascosto |

**Regole comuni.** Ogni azione richiede un motivo scritto; nessuna azione dà accesso ai contenuti dell'account;
nessuna azione crea, modifica o cancella un movimento — **la console non scrive nel registro**, e questa è la
regola più importante di tutto il documento. Se un movimento è sbagliato, lo storna il cliente dalla sua interfaccia
([`0017`](03-registro-dei-movimenti-e-giacenze/0017-storno-di-un-movimento.md)): un registro che un amministratore
di piattaforma può ritoccare non è un registro.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| conteggio degli articoli attivi per account | metrica | no | diagnosi della quota e delle deroghe |
| conteggio dei movimenti per account e per giorno | metrica | no | capire se un account è fermo o in difficoltà; **aggregato per account, mai per persona** |
| numero di divergenze registro/proiezione, scarto massimo, momento dell'ultima verifica | metrica | no | salute del registro, §4 |
| stato delle code in ingresso e in uscita: conteggi, codici di errore, arretrato più vecchio | stato | no | diagnosi degli scambi a eventi |
| esiti delle importazioni: righe accettate e respinte, codice di errore | stato | no | assistenza sulla migrazione iniziale |
| identificativi tecnici di articolo e deposito coinvolti in una divergenza | identificativo | no | localizzare la riga da riparare |

**Verifica obbligatoria.** **Nessuna riga di questa tabella contiene dati personali**, e la scelta è deliberata. In
particolare il conteggio dei movimenti è esposto **solo aggregato per account**: una vista «movimenti per operatore»
sarebbe un indicatore di produttività per persona costruito dentro la console di chi vende il programma, cioè
esattamente ciò che la descrizione dell'applicazione esclude (§6, con il vincolo dell'articolo 4 dello Statuto dei
lavoratori). Non va aggiunta nemmeno «per diagnosi».

## 7. Punti aperti

- **Se la piattaforma non prevede un meccanismo generale di deroga alla quota**, questa app ne chiede uno per la
  prima volta: va deciso se costruirlo qui (sbagliato: lo rifarebbero tutte le app) o come funzione comune della
  console. Lo chiude lo sviluppatore insieme alla piattaforma, prima della storia
  [`0011`](02-anagrafiche-e-catalogo-prodotti/0011-importazione-dell-anagrafica-da-file.md).
- **Soglia di allarme sulle divergenze**: a partire da quante e da quale scarto la console deve segnalare
  attivamente invece di limitarsi a mostrare il numero. Nessun dato per deciderlo prima di avere account veri: si
  parte mostrando, si taglia dopo.
- **Chi riceve l'allarme e come**, visto che nel perimetro proposto l'app non manda niente a nessuno: va deciso
  insieme al presidio di esercizio della piattaforma, non da questa applicazione.
