# 0032 — Esportazione e cancellazione complete

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 07 — Esposizione conversazionale e prove
**Storia**: `0032` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0020`, `0025`, `0027`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come cliente di un salone, o come persona che nel salone ci ha lavorato, che chiede una copia dei propri dati o
> la loro cancellazione
> voglio che «tutti» voglia dire davvero tutti, anche le cose aggiunte nell'ultimo anno di sviluppo
> così da non dipendere dalla memoria di chi ha scritto l'ultima tabella.

**Contesto.** La storia `0014` ha chiuso il contratto dei dati quando esistevano le epiche 01-03, e ha lasciato
scritto un punto aperto onesto: *le sue voci arrivano fino all'epica 06, e un manifesto scritto alla fine è un
manifesto ricostruito a memoria*. Le epiche 04, 05 e 06 hanno poi aggiunto magazzino, conti, pacchetti, fedeltà,
attribuzioni, regole di provvigione e prospetti, e la storia `0029` ha aggiunto le bozze delle operazioni
conversazionali. **Questa storia è la chiusura**: verifica che ogni tabella arrivata dopo sia in esportazione e in
cancellazione, e — soprattutto — installa il **presidio automatico** che impedisce alla prossima tabella di essere
dimenticata. È l'ultima storia dell'applicazione perché è l'unica che può essere completa: prima non lo sarebbe.

Le candidate a essere dimenticate sono note e si nominano: le **fotografie** (non sono una colonna di testo), i
**movimenti di magazzino** (sembrano un registro tecnico e invece dicono chi ha fatto cosa) e tutto ciò che
riguarda **chi lavora nel salone**, che è la categoria di interessati che nella piattaforma è meno battuta.

## 2. Requisiti funzionali

1. **RF-1** — `exportData` e `purgeData` coprono **tutte** le tabelle del verticale con dati di persone, comprese
   quelle delle epiche 04-06 e le bozze della storia `0029`: schede tecniche, fotografie, preferenze di variante,
   conti e righe, attribuzioni, pacchetti e utilizzi, tessere e movimenti di punti, esiti di richiamo, regole di
   provvigione, prospetti e loro voci, movimenti di magazzino (per la colonna di chi li ha causati), bozze di
   operazione.
2. **RF-2** — Esiste un **controllo automatico** che confronta l'elenco delle tabelle dello schema che contengono
   una colonna riferita a una persona con l'elenco dichiarato nel contratto dati e nel manifesto: una tabella
   presente nello schema e assente da uno dei due **fa fallire la suite**. È il requisito centrale della storia:
   senza, la conformità dipende da chi si ricorda.
3. **RF-3** — L'esportazione di un **cliente** produce un archivio leggibile che comprende anche i **file** delle
   fotografie, non solo i loro riferimenti.
4. **RF-4** — La cancellazione di un cliente con **pacchetto pagato e non consumato** applica la regola già decisa
   nella storia `0014`: il pacchetto si chiude, resta un movimento senza intestatario con l'importo residuo, le
   sedute future collegate si annullano.
5. **RF-5** — L'esportazione e la cancellazione dei dati di **chi lavora nel salone** sono trattate in modo
   esplicito e distinto: che cosa viene esportato (regole, prospetti, attribuzioni), che cosa viene cancellato e
   che cosa il salone conserva come documentazione amministrativa, con la motivazione scritta nel manifesto.
6. **RF-6** — I diritti restano accessibili **anche** con abbonamento scaduto o app disabilitata, e **nessuno
   strumento conversazionale** li espone (storie `0028` e `0029`).

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** Manifesto completo in italiano e inglese su ogni testo; ogni campo che riguarda
  una persona annotato `@PersonalData` — un campo annotato e non dichiarato fa fallire la compilazione, ed è il
  presidio su cui questa storia poggia; contratto dati con `appId()`, `exportData(scope)`, `purgeData(scope)`,
  `manifest()`.
- **RT-2 — Il controllo di completezza.** Il confronto dell'RF-2 gira nelle prove del servizio: legge le tabelle
  dello schema dell'app dalle migrazioni vere applicate su database effimero e le confronta con quanto dichiarato.
  Una tabella nuova esce dal silenzio il giorno stesso in cui nasce, non il giorno di una verifica.
- **RT-3 — Isolamento fra account (§1).** Esportazione e cancellazione agiscono solo dentro l'account del token
  verificato; il perimetro si calcola dal `tenant_id`, mai da un identificativo che arriva con la richiesta.
- **RT-4 — Cancellazione fisica (§8, §10).** La cancellazione è **fisica** e lascia una riga di prova nel registro
  delle purghe; sostituire il nome con un codice non è cancellare. I movimenti immutabili non fanno eccezione: si
  rimuove il collegamento alla persona, e il movimento quando esiste solo per lei.
- **RT-5 — Fotografie.** L'esportazione include i file, la cancellazione li rimuove dall'archivio oltre che dalla
  riga: una cancellazione che lascia l'immagine nell'archivio è la peggiore delle cancellazioni, perché sembra
  fatta.
- **RT-6 — Cinque lingue (§4).** I testi rivolti all'utente che accompagnano esportazione e cancellazione presenti
  in `en, it, fr, es, de`.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento**, né di lettura né di scrittura: sono atti che
  si compiono guardando in faccia un'interfaccia. Divieto dichiarato nel contratto (storie `0028`, `0029`).
- **RT-8 — Registrazione eventi (§14).** `esportazione richiesta`, `purga eseguita` con `tenant_id`, `app_id`,
  `user_id`, correlazione e **conteggi** — mai l'identità dell'interessato.

## 4. Criteri di accettazione

**CA-1 — Nessuna tabella resta indietro**
- **Dato** lo schema dell'applicazione con tutte le tabelle delle epiche 01-07
- **Quando** gira il controllo di completezza
- **Allora** è verde; e aggiungendo alla migrazione una tabella con un riferimento a una persona senza dichiararla,
  **la suite diventa rossa** indicando quale tabella manca

**CA-2 — L'esportazione porta anche i file**
- **Dato** una cliente con schede tecniche, due fotografie, conti, un pacchetto, punti e un esito di richiamo
- **Quando** si esportano i suoi dati
- **Allora** l'archivio contiene tutte le categorie **e i due file** delle fotografie

**CA-3 — Cancellato vuol dire cancellato**
- **Dato** la stessa cliente · **Quando** si esegue la cancellazione
- **Allora** nessuna riga e nessun file la riguarda più in nessuna delle tabelle dichiarate, e nel registro delle
  purghe c'è la prova dell'operazione

**CA-4 — Il pacchetto non consumato**
- **Dato** una cliente con un pacchetto da dieci sedute di cui tre usate
- **Quando** si cancellano i suoi dati
- **Allora** il pacchetto risulta chiuso, resta un movimento senza intestatario con il residuo e le sedute future
  collegate sono annullate

**CA-5 — Chi ha lavorato nel salone**
- **Dato** un operatore che se n'è andato, con regole di provvigione, due prospetti chiusi e attribuzioni su conti
  passati
- **Quando** se ne cancellano i dati
- **Allora** ciascuna delle tre categorie è trattata come dichiarato nel manifesto — cancellata o conservata con la
  sua motivazione — e nessuna resta indietro in silenzio

**CA-6 — Diritti sempre accessibili, e mai da una chat**
- **Dato** un account con abbonamento `canceled`
- **Quando** chiede l'esportazione, e quando si cerca uno strumento conversazionale che esporti o cancelli
- **Allora** l'esportazione riesce e nessuno strumento del genere esiste

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite intera; le aree `compliance` e `backend` sono quelle che
      contano);
- [ ] prova di **integrazione** che esporta e poi cancella un cliente **completo** e un operatore **completo**, e
      verifica tabella per tabella che non resti nulla;
- [ ] prova del **controllo di completezza**, verificata anche nel verso negativo: una tabella non dichiarata deve
      far fallire;
- [ ] prova di **isolamento fra account** su esportazione e cancellazione;
- [ ] **prova end-to-end**: *rimando motivato* — i diritti dell'interessato hanno un percorso di piattaforma
      proprio, e una prova che cancella i propri dati di partenza renderebbe instabili i percorsi `[J-SALONGROVE]` e
      `[J-SALONGROVE-PKG]`; risposta scritta nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e in `decisions.json`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** completo e in parità italiano/inglese, comprese le voci delle epiche 04-06 e la nota su
      ciò che l'app **non** tratta (storia `0012`);
- [ ] **registro delle decisioni**: elenco delle tabelle coperte, trattamento dei dati di chi lavora nel salone,
      trattamento del pacchetto non consumato, durate di conservazione e loro motivo;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` | è il contratto che questa storia completa e chiude; il suo punto aperto si risolve qui |
| storia `0020` | il pacchetto non consumato è il caso difficile della cancellazione |
| storia `0025` | i prospetti sono la parte dei dati di chi lavora nel salone che si dimentica più spesso |
| storia `0027` | gli esiti di richiamo sono l'ultima tabella arrivata sul cliente |
| storia `0029` | le bozze delle operazioni contengono riepiloghi che possono nominare una persona |
| **decisione sui dati personali** (§6 della descrizione) | il manifesto si compila insieme allo sviluppatore, non da soli |

## 7. Fuori ambito

- la **valutazione d'impatto**, se lo sviluppatore aprisse la via (b) del §6 della descrizione (modulo «sicurezza
  del trattamento» con dati sulla salute): è un documento, non una storia di sviluppo;
- il **registro dei trattamenti**: si genera dal manifesto, non si scrive a mano;
- la **chiusura dell'account** e la purga di fine rapporto: sono di piattaforma;
- l'esportazione in un formato interoperabile con un altro gestionale: è portabilità commerciale, non un diritto da
  soddisfare qui.

## 8. Punti aperti

**Fino a dove arriva il diritto alla cancellazione di chi ha lavorato nel salone.** Un prospetto chiuso è
documentazione amministrativa del salone su un rapporto economico concluso, e il salone — che è il titolare del
trattamento — può avere ragioni legittime di conservarlo. La proposta è: attribuzioni e regole si cancellano,
i prospetti chiusi si conservano per la durata dichiarata e poi si cancellano, con la motivazione scritta nel
manifesto. **Non è una decisione che spetta a un agente**: tocca un diritto dell'interessato e un obbligo
documentale insieme, e va chiusa dallo sviluppatore con supporto legale (§11, punto 4 e avviso sul lavoro del §6
della descrizione).

**Le durate di conservazione non nascono da una norma rilevata** (§2.3 punto 6 della descrizione): i 36 mesi della
formula e i 24 del resto sono minimizzazione ragionata, e restano da validare.
