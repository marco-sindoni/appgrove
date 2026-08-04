# 0034 — Percorso end-to-end e registro di copertura

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0034` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: tutte le storie da `0001` a `0033`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio una prova automatica che percorra InsightGrove dal collegamento di una fonte fino a una risposta
> verificabile, sullo stack reale
> così da sapere che la catena di custodia del numero regge davvero, e non solo che i suoi pezzi passano le
> proprie prove.

**Contesto.** Questa applicazione ha una particolarità che il suo percorso end-to-end deve rispettare: **il numero
non nasce dentro l'app**. Arriva come fatto pubblicato da un'altra applicazione, viene aggregato, mostrato,
spiegato e infine esportato. Un percorso che partisse da dati inseriti a mano nel database salterebbe proprio il
passo che questa app esiste per garantire. Perciò il percorso comincia dal **fatto pubblicato sul bus** (con il
generatore di fatti inventati della storia 0005) e finisce su una scheda del numero che risale fino al rimando
d'origine. Il registro di copertura è sorvegliato da un controllo automatico: registro incoerente significa suite
rossa ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §11).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-INSIGHTS.spec.ts`, eseguito senza finestra sullo
   stack locale reale, con accesso programmatico e dati **inventati e deterministici**.
2. **RF-2** — Il percorso copre, in quest'ordine: attivazione dell'app → collegamento di una fonte → arrivo di
   fatti pubblicati sul bus con ripopolamento dello storico → cruscotto iniziale già pieno → apertura della
   **scheda del numero** e salto al rimando d'origine → domanda al copilota con risposta e ricevuta → definizione
   di un avviso e suo scatto → esportazione della tavola con il blocco di provenienza → richiesta di una
   **proiezione**, verificata come stima nel disegno e nell'esportazione → bozza e conferma di uno strumento di
   scrittura.
3. **RF-3** — Ogni test porta **l'etichetta del percorso in testa al titolo**: `test('[J-INSIGHTS] …')`.
4. **RF-4** — Il percorso verifica i quattro casi negativi che contano per questa app: (a) un secondo account non
   vede nulla del primo; (b) una metrica economica chiesta da un `member` riceve un rifiuto, non un numero
   ridotto; (c) a quota esaurita il copilota risponde `429` e non esegue; (d) un valore con una fonte silente
   porta il contrassegno di incompletezza **prima** della cifra, e l'avviso su quel valore **non scatta**.
5. **RF-5** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) contiene una
   voce per ogni storia che ha risposto «coprire ora», una voce `da-coprire` con motivo e storia proprietaria per
   quelle che hanno risposto «rimando» — comprese quelle che aspettano il livello conversazionale di piattaforma
   (UC 0061-0064) — e nessuna voce per quelle che hanno risposto «nessun impatto».
6. **RF-6** — Il percorso è **ripetibile**: non usa attese a tempo, non dipende dall'ordine di esecuzione degli
   altri percorsi e non lascia dati che facciano fallire l'esecuzione successiva.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso usa **due** account, entrambi con fatti pubblicati dalle
  stesse fonti, e verifica che il secondo non veda nulla del primo: qui l'invariante si prova su **due strade**,
  la lettura dalle schermate e la scrittura dei fatti in arrivo dal bus, che è la particolarità di questa app.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. I fatti del percorso arrivano dal generatore della storia
  0005 e sono palesemente inventati.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova: il percorso attraversa quelle esistenti. Verifica
  che il contrassegno di incompletezza sia percepibile **senza affidarsi al colore**.
- **RT-5 — Cinque lingue (§4).** Il percorso gira nella lingua predefinita; una verifica separata controlla che
  nessuna chiave di traduzione compaia grezza, nelle cinque lingue, sulle schermate principali del modulo.
- **RT-6 — Varchi e quota (§6, §7).** Il percorso verifica la catena dei varchi almeno una volta: accesso senza
  abilitazione (`402`), metrica economica senza ruolo (`403`), quota delle domande esaurita (`429`).
- **RT-7 — Esposizione conversazionale (§12).** Il passo di bozza e conferma esercita il contratto **chiamando il
  servizio direttamente**, perché il server conversazionale non esiste (UC 0061-0063). Va scritto nel registro
  delle decisioni: quando il server arriverà, quel passo va rifatto passando da lui.
- **RT-8 — Dati personali (§10).** Nessun dato reale: etichette di dimensione palesemente finte («Panificio
  Aurora»), indirizzi nel dominio riservato alle prove. Il percorso verifica che il file esportato non contenga
  dati dell'altro account.
- **RT-14 — Registrazione eventi (§14).** Nessun evento nuovo.

## 4. Criteri di accettazione

**CA-1 — Il percorso passa**
- **Dato** lo stack locale avviato
- **Quando** si esegue `./run-tests.sh` nell'area dei percorsi di piattaforma
- **Allora** il percorso `[J-INSIGHTS]` è verde dall'inizio alla fine

**CA-2 — La catena di custodia regge**
- **Dato** un fatto pubblicato dalla fonte simulata con un riferimento d'origine noto
- **Quando** il percorso apre la scheda del numero che lo comprende e segue il rimando
- **Allora** arriva alla riga d'origine dichiarata dal fatto, e il conteggio dei fatti nella scheda coincide con
  quelli pubblicati

**CA-3 — Isolamento provato end-to-end**
- **Dato** i due account del percorso, con fatti delle stesse fonti
- **Quando** il secondo apre cruscotti, copilota, esportazioni e schede
- **Allora** non vede alcun numero del primo

**CA-4 — Incompletezza e avvisi**
- **Dato** una fonte richiesta resa silente durante il percorso
- **Quando** il cruscotto si ricarica e la valutazione degli avvisi gira
- **Allora** il valore porta il contrassegno di incompletezza accanto alla cifra e l'avviso **non scatta**

**CA-5 — Registro coerente**
- **Dato** il registro di copertura aggiornato
- **Quando** si esegue il controllo nell'area degli strumenti
- **Allora** è verde: ogni voce punta a un test che esiste e ogni test etichettato compare nel registro

**CA-6 — Registro incoerente fa rosso**
- **Dato** una voce del registro che punta a un test rimosso
- **Quando** si esegue il controllo
- **Allora** la suite è rossa con l'indicazione della voce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` **completo**, non solo le aree toccate;
- [ ] il percorso `[J-INSIGHTS]` non usa attese a tempo e non dipende dall'ordine di esecuzione degli altri
      percorsi;
- [ ] prova di **isolamento fra account** compresa nel percorso, su lettura **e** su scrittura dei fatti;
- [ ] **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
      aggiornato con le voci di tutte e 35 le storie, ognuna nella forma «coperta», «rimandata» con motivo e
      storia proprietaria, oppure assente perché senza impatto;
- [ ] **traduzioni**: verifica che nessuna chiave compaia grezza nelle cinque lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che i dati del percorso siano inventati;
- [ ] **registro delle decisioni** compilato, con annotato che il passo conversazionale andrà rifatto quando il
      server esisterà e che il percorso parte dal bus, non dal database;
- [ ] contratto degli **strumenti conversazionali**: esercitato dal percorso, in lettura e in scrittura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| tutte le storie da `0001` a `0033` | il percorso le attraversa |
| storia `0005` | il generatore di fatti inventati è ciò che rende il percorso possibile senza le app sorgenti vere |
| UC 0061-0064 (livello conversazionale) | non implementati: il passo conversazionale chiama il contratto direttamente, e andrà rifatto |

## 7. Fuori ambito

- le prove di carico e di prestazione sul magazzino dei fatti: fuori perimetro;
- il percorso che attraversa **davvero** due app della suite (una fonte reale che pubblica e InsightGrove che
  riceve): dipende dal contratto degli eventi di dominio, che oggi non esiste (§11, punto 11 della
  [descrizione](../application-description.md)). Qui la fonte è simulata, e va detto nel registro;
- la cancellazione dei dati personali dentro il percorso: una prova che distrugge i propri dati di partenza è
  fragile; la copertura resta alle prove di integrazione della storia 0035.

## 8. Punti aperti

- **Quando esisterà una vera app sorgente**, il percorso andrà esteso a un giro di suite completo (fonte pubblica
  → InsightGrove riceve → il numero compare). È un lavoro che appartiene al contratto degli eventi di dominio, non
  a questa storia. Chiude: **piattaforma**.
