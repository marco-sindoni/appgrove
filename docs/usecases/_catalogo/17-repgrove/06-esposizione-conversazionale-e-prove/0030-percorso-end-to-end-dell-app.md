# 0030 — Percorso end-to-end dell'app

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0005`, `0006`, `0007`, `0009`, `0011`, `0012`, `0014`, `0016`, `0017`, `0018`, `0019`, `0024`, `0025`, `0028`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio una prova automatica che percorra RepGrove dall'inizio alla fine sullo stack locale reale
> così da sapere, a ogni modifica, se la catena che porta dal cliente servito alla risposta pubblicata è ancora
> intera — e se i rifiuti su cui è costruita l'app tengono ancora.

**Contesto.** Diverse storie precedenti hanno lasciato una voce `da-coprire` nel registro di copertura indicando
questa storia come proprietaria (0001, 0003, 0004, 0005, 0006, 0010, 0026, 0027). È il momento di riscuotere: si
costruisce il percorso `[J-RECENSIONI]`, che attraversa la superficie utente vera, e si mette in ordine il registro
([docs/testing/README.md](../../../../testing/README.md)).

Il percorso non serve a verificare i dettagli — quelli li coprono le prove di unità e di integrazione — serve a
verificare che i pezzi si parlino: è la classe di guasti che nessuna prova per componente vede. In questa app c'è
però un secondo compito, che altrove non esiste: **provare che le cose vietate continuino a non essere possibili**.
Un divieto che vive in nove punti diversi del codice (descrizione §1) è un divieto che prima o poi qualcuno
riapre per sbaglio, e la prova end-to-end è l'unico posto da cui si vede l'insieme.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-RECENSIONI.spec.ts`, eseguito senza finestra sullo
   stack locale reale, con accesso programmatico e dati di prova deterministici e **inventati**.
2. **RF-2** — Il **percorso principale** attraversa, nell'ordine: attivazione dell'abbonamento e comparsa del modulo
   nella barra laterale → creazione di una sede → collegamento a una piattaforma **simulata** → raccolta delle
   recensioni → elenco unico con la recensione negativa in evidenza → generazione della bozza di risposta →
   approvazione e pubblicazione con conferma esplicita → verifica che la risposta risulti pubblicata sulla
   piattaforma simulata.
3. **RF-3** — Il **percorso dell'equità** attraversa: registrazione di tre servizi erogati di cui uno senza recapito
   → regola di equità `tutti` → programmazione del lotto → invio con fornitore di recapito simulato → verifica che
   il cliente senza recapito risulti `non_inviata` **con il motivo scritto** → esportazione del registro di equità →
   verifica che il documento contenga invitati, esclusi e regola applicata.
4. **RF-4** — Il **percorso conversazionale e dei rifiuti** attraversa: creazione di una bozza attraverso il
   contratto degli strumenti → tentativo di esecuzione senza conferma, **rifiutato** → conferma da parte di un
   utente con ruolo sufficiente → esecuzione → richiesta di una pratica vietata → verifica del rifiuto spiegato, con
   la categoria e la fonte.
5. **RF-5** — Il percorso comprende un passo di **quota esaurita**: con la metrica `sedi_monitorate` al tetto, il
   tentativo di collegare una sede in più mostra il messaggio di quota esaurita con il rimedio, e nulla viene creato.
6. **RF-6** — Il percorso comprende un passo sulla **vetrina**: senza dichiarazione di trasparenza accettata il
   riquadro non serve dati; dopo l'accettazione mostra media reale, totale reale e attribuzione, e non espone alcun
   modo di filtrare per voto.
7. **RF-7** — Ogni test porta l'etichetta del percorso in testa al titolo, nella forma `test('[J-RECENSIONI] …')`, e
   il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato: le voci
   `da-coprire` lasciate dalle storie precedenti sono chiuse o riformulate, e nessuna resta orfana.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso esercita **due** account e verifica, in almeno un punto, che
  i dati dell'uno non compaiano nell'altro: l'isolamento va provato anche end-to-end, non solo nelle prove di
  integrazione. Un punto obbligato è la rotta pubblica del riquadro, che è l'unica non autenticata dell'app.
- **RT-2 — Interfaccia di programmazione (§2).** Il percorso usa l'**interfaccia utente reale** per le azioni e le
  rotte solo per preparare lo stato: un percorso che fa tutto dalle rotte non prova la superficie.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. I dati di prova nascono dal comando della storia 0005, esteso
  con quanto serve ai tre percorsi (una sede al tetto della quota, una recensione negativa, un cliente senza
  recapito).
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Se il percorso fatica a individuare un elemento, la
  correzione è aggiungere nel modulo gli attributi di accessibilità mancanti, non aggirare il problema nel test.
- **RT-5 — Cinque lingue (§4).** Il percorso gira in una lingua sola; un controllo separato verifica che nessuna
  chiave resti non tradotta nelle cinque lingue.
- **RT-6 — Varchi e quota (§6, §7).** Oltre al passo di quota esaurita (RF-5), il percorso verifica che con
  abbonamento `canceled` il riquadro pubblico smetta di servire dati e che i diritti dell'interessato restino
  accessibili (storia 0031).
- **RT-7 — Esposizione conversazionale (§12).** Il terzo percorso esercita il contratto degli strumenti nella parte
  verificabile — bozza, rifiuto senza conferma, conferma, rifiuto della pratica vietata — anche se il server
  conversazionale di piattaforma non esiste ancora (UC 0061-0063): il contratto si invoca direttamente.
- **RT-8 — Dati personali (§10).** I dati del percorso sono **inventati**: nomi di fantasia, recapiti su dominio
  `*.test`, testi di recensione scritti per il collaudo. Piattaforme e fornitore di recapito **simulati**: nessun
  messaggio esce davvero, nessuna risposta viene pubblicata davvero. Nessuna voce nuova nel manifesto.
- **RT-9 — Registrazione eventi (§14).** Nessun evento applicativo nuovo.
- **RT-10 — Niente attese a tempo.** Il percorso attende condizioni osservabili, mai un numero di secondi: la
  raccolta periodica e la programmazione degli inviti si innescano da un comando di prova, non aspettando il
  calendario. Un percorso che dipende dai tempi fallirà a caso e verrà disattivato.

## 4. Criteri di accettazione

**CA-1 — Il percorso principale è verde**
- **Dato** lo stack locale avviato con i dati di prova
- **Quando** si esegue `./run-tests.sh platform`
- **Allora** il percorso `[J-RECENSIONI]` passa attraversando tutti i passi di RF-2, e la piattaforma simulata
  risulta aver ricevuto **una** risposta pubblicata

**CA-2 — Nessuna pubblicazione senza conferma**
- **Dato** una bozza di risposta creata dal contratto degli strumenti
- **Quando** si tenta di eseguirla senza conferma umana
- **Allora** il tentativo è rifiutato e la piattaforma simulata non riceve nulla

**CA-3 — Il motivo dell'esclusione c'è sempre**
- **Dato** il percorso dell'equità con un cliente senza recapito
- **Quando** si esporta il registro di equità
- **Allora** il documento contiene il cliente escluso **con il motivo** e la regola applicata; un'esclusione senza
  motivo fa fallire il test

**CA-4 — La pratica vietata resta vietata**
- **Dato** il percorso conversazionale
- **Quando** si chiede di invitare solo i clienti soddisfatti e di filtrare il riquadro per voto
- **Allora** entrambe le richieste ricevono un rifiuto con categoria e fonte, e nessuna configurazione cambia

**CA-5 — Quota esaurita**
- **Dato** l'account di prova al tetto di `sedi_monitorate`
- **Quando** il percorso tenta di collegare una sede in più
- **Allora** osserva nell'interfaccia il messaggio di quota esaurita con il rimedio, e la sede non viene creata

**CA-6 — Il riquadro dice la verità**
- **Dato** una sede con media 3,8 su venti recensioni e la dichiarazione accettata
- **Quando** il percorso apre la pagina che ospita il riquadro
- **Allora** legge media e totale reali, l'attribuzione alla piattaforma d'origine, e non trova alcun modo di
  filtrare per voto nemmeno forzando i parametri della rotta pubblica

**CA-7 — Registro di copertura coerente**
- **Dato** il registro `docs/testing/copertura-e2e.yaml` aggiornato
- **Quando** si esegue il controllo `tools/e2e-coverage` nell'area `tooling`
- **Allora** è verde e nessuna voce `da-coprire` resta senza storia proprietaria

**CA-8 — Isolamento end-to-end**
- **Dato** i due account di prova
- **Quando** il percorso accede con il secondo e chiama la rotta pubblica del riquadro del primo
- **Allora** non vede alcuna recensione dell'altro account oltre a quelle che quel riquadro pubblica per sua natura

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` **completo**: è la storia che chiude l'applicazione e non ammette aree
      rosse;
- [ ] i tre percorsi esistono, sono etichettati `[J-RECENSIONI]` e girano senza finestra e senza attese a tempo;
- [ ] prova di **isolamento fra account** anche a livello di percorso, compresa la rotta pubblica;
- [ ] **prova end-to-end**: *coprire ora* — è la storia che possiede il percorso `[J-RECENSIONI]`, e il **registro di
      copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato e verde al
      controllo automatico, senza voci `da-coprire` orfane;
- [ ] **traduzioni**: controllo che nessuna chiave resti non tradotta nelle cinque lingue;
- [ ] **manifesto dei dati**: nessuna modifica, ma si verifica che i dati di prova non contengano nulla di reale;
- [ ] **registro delle decisioni** compilato con la composizione dei tre percorsi e il motivo di ciascuno;
- [ ] contratto degli **strumenti conversazionali**: esercitato nella parte verificabile;
- [ ] `./dev.sh services`, `./app-start.sh` e `tools/smoke` funzionano senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0005`, `0006`, `0007`, `0009`, `0011`, `0012`, `0014`, `0016`, `0017`, `0018`, `0019`, `0024`, `0025` | sono i passi che i percorsi attraversano |
| storie `0028`, `0029` | il terzo percorso esercita bozza, conferma e rifiuto |
| UC 0061-0063 (livello conversazionale) | non implementati: il contratto si invoca direttamente, senza server |

## 7. Fuori ambito

- i percorsi per le funzioni coperte da sole prove di integrazione (raccolta periodica, rapporto mensile, temi
  ricorrenti, segnalazione): la scelta è dichiarata storia per storia ed è registrata nel registro di copertura;
- il collaudo con le piattaforme **reali**: richiede credenziali vere e ha effetti verso l'esterno — è materia di
  verifica manuale pre-rilascio, non di suite automatica;
- il livello di prova sui pagamenti con fornitore reale, che è pre-rilascio e di piattaforma;
- le prove di carico.

## 8. Punti aperti

Nessuno. Le decisioni che restano aperte (articolo 9, conservazione dei contenuti di terzi, base giuridica
dell'invito) cambiano **cosa** i percorsi attraversano, non il fatto che debbano esistere: si aggiornano i percorsi
quando quelle decisioni saranno prese.
