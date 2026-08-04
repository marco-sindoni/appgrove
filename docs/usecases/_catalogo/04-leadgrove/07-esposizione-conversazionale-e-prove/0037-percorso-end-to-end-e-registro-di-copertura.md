# 0037 — Percorso end-to-end e registro di copertura

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0037` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: tutte le storie precedenti — è l'ultima dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio una prova automatica che percorra LeadGrove dall'inizio alla fine sullo stack reale
> così da sapere che l'app funziona davvero, e non solo che i suoi pezzi passano le proprie prove.

**Contesto.** Ogni app di appgrove ha il suo percorso end-to-end e ogni percorso è registrato nella mappa
*use case → percorso → test*, sorvegliata da un controllo automatico: registro incoerente significa suite rossa.
Questa storia chiude l'applicazione mettendo insieme i passi che le storie precedenti hanno annotato come «coprire
ora» e allineando il registro.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-SALES.spec.ts`, eseguito senza finestra sullo
   stack locale reale, con accesso programmatico e dati inventati deterministici.
2. **RF-2** — Il percorso copre, in quest'ordine: attivazione dell'app e assegnazione di un posto → creazione di
   un'azienda e di un contatto → registrazione di un consenso → creazione di una trattativa → spostamento di fase
   (con l'alternativa da tastiera) → creazione di un'attività e verifica nell'agenda → invio dal modulo web
   pubblico con i consensi e comparsa della trattativa → creazione via bozza e conferma di uno strumento di
   scrittura → riassunto dell'azienda → chiusura come vinta → esportazione con l'avviso.
3. **RF-3** — Ogni test porta **l'etichetta del percorso in testa al titolo**: `test('[J-SALES] …')`.
4. **RF-4** — Il percorso verifica anche i due casi negativi che contano: un secondo account non vede nulla del
   primo, e l'assegnazione di un posto oltre il tetto riceve `429`.
5. **RF-5** — Il registro `docs/testing/copertura-e2e.yaml` contiene una voce per ogni storia che ha risposto
   «coprire ora» e una voce `da-coprire` con motivo e storia proprietaria per quelle che hanno risposto
   «rimando»; le storie che hanno risposto «nessun impatto» non compaiono.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso usa **due** account e verifica esplicitamente che il secondo
  non veda nulla del primo: è la prova end-to-end dell'invariante numero uno.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; i dati del percorso sono inventati e deterministici, con
  indirizzi nel dominio riservato alle prove.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova; il percorso attraversa quelle esistenti,
  compreso il modulo pubblico.
- **RT-5 — Cinque lingue (§4).** Il percorso gira nella lingua predefinita; una verifica separata controlla che
  nessuna chiave di traduzione compaia grezza nelle cinque lingue sulle schermate principali.
- **RT-6 — Varchi e quota (§6, §7).** Il percorso verifica la catena dei varchi almeno una volta: accesso senza
  posto (`403`), assegnazione oltre il tetto (`429`).
- **RT-7 — Esposizione conversazionale (§12).** Il percorso esercita il ciclo bozza-conferma di uno strumento di
  scrittura attraverso il servizio: il server conversazionale non esiste (UC 0061-0063), quindi si chiama il
  contratto direttamente. Va scritto nel registro, perché quando il server arriverà quel passo andrà rifatto
  passando da lui.
- **RT-8 — Dati personali (§10).** Nessun dato reale: nomi palesemente finti e indirizzi nel dominio riservato
  alle prove. Il percorso verifica che l'esportazione mostri l'avviso e che il file non contenga dati dell'altro
  account.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo.

## 4. Criteri di accettazione

**CA-1 — Il percorso passa**
- **Dato** lo stack locale avviato
- **Quando** si esegue `./run-tests.sh` nell'area dei percorsi di piattaforma
- **Allora** il percorso `[J-SALES]` è verde dall'inizio alla fine

**CA-2 — Isolamento provato end-to-end**
- **Dato** i due account del percorso
- **Quando** il secondo apre elenchi, ricerca e rapporti
- **Allora** non vede nulla del primo

**CA-3 — Quota provata end-to-end**
- **Dato** l'account del percorso al tetto dei posti
- **Quando** tenta di assegnarne un altro
- **Allora** riceve `429` e il messaggio con il rimedio

**CA-4 — Registro coerente**
- **Dato** il registro di copertura aggiornato
- **Quando** si esegue il controllo nell'area degli strumenti
- **Allora** è verde: ogni voce punta a un test che esiste e ogni test etichettato compare nel registro

**CA-5 — Registro incoerente fa rosso**
- **Dato** una voce del registro che punta a un test rimosso
- **Quando** si esegue il controllo
- **Allora** la suite è rossa con l'indicazione della voce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` **completo**, non solo le aree toccate;
- [ ] il percorso `[J-SALES]` non usa attese a tempo e non dipende dall'ordine di esecuzione degli altri percorsi;
- [ ] prova di **isolamento fra account** compresa nel percorso;
- [ ] **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
      aggiornato con le voci di tutte e 37 le storie, ognuna nella forma «coperta», «rimandata» con motivo e
      storia proprietaria, oppure assente perché senza impatto;
- [ ] **traduzioni**: verifica che nessuna chiave compaia grezza nelle cinque lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che i dati del percorso siano inventati;
- [ ] **registro delle decisioni** compilato, con annotato che il passo conversazionale andrà rifatto quando il
      server esisterà;
- [ ] contratto degli **strumenti conversazionali**: esercitato dal percorso;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Tutte le storie da `0001` a `0036` | Il percorso le attraversa |
| UC 0061-0063 (livello conversazionale) | Non implementati: il passo conversazionale chiama il contratto direttamente, e andrà rifatto |

## 7. Fuori ambito

- le prove di carico e di prestazione: fuori perimetro;
- la suite di terzo livello sul fornitore di pagamento reale: è pre-rilascio e di piattaforma;
- i percorsi che attraversano più app della suite: dipendono dal contratto degli eventi condivisi
  ([application-description.md](../application-description.md) §11.4).

## 8. Punti aperti

- Nessuno.
