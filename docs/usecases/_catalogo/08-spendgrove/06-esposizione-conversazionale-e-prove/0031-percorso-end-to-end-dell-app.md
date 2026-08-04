# 0031 — Percorso end-to-end dell'app

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: tutte le storie precedenti (`0001`–`0030`)
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che domani cambierà qualcosa in questa applicazione
> voglio una prova automatica che percorra tutto il ciclo, dalla foto della ricevuta al pacchetto per il
> commercialista, sullo stack vero
> così da sapere subito se ho rotto il filo che tiene insieme le trenta storie precedenti.

**Contesto.** Ogni storia ha le sue prove, ma nessuna prova che il **ciclo** funzioni: il punto in cui le
applicazioni si rompono non è dentro una funzione, è nel passaggio da una all'altra. Questa storia costruisce il
percorso di piattaforma `[J-NOTESPESE]` e mette in ordine il registro di copertura, chiudendo i rimandi lasciati
dalle storie precedenti. Va per ultima perché ha bisogno che tutto esista; ma i rimandi che raccoglie sono stati
**dichiarati man mano**, non scoperti alla fine — è la differenza fra un registro tenuto vero e una pulizia
periodica.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-NOTESPESE.spec.ts`, eseguito senza finestra sullo
   stack locale reale, che copre il ciclo principale: accesso, caricamento di una ricevuta finta, lettura simulata,
   revisione con correzione di un campo, conferma, composizione della nota, invio, approvazione con un secondo
   utente, registrazione del rimborso, produzione del pacchetto.
2. **RF-2** — Ogni test porta **l'etichetta del percorso in testa al titolo**: `test('[J-NOTESPESE] …')`.
3. **RF-3** — Il percorso copre anche i quattro rami che le storie hanno dichiarato come propri: quota esaurita,
   doppione scartato, spesa in contanti che avvisa sulla deducibilità, percorrenza chilometrica.
4. **RF-4** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato e
   coerente: ogni storia con superficie applicativa ha la sua voce, e i rimandi lasciati dalle storie `0002`,
   `0003`, `0004`, `0005`, `0009`, `0010`, `0012`, `0017`, `0018`, `0021`, `0022`, `0026`, `0030` sono chiusi o
   dichiarati con motivo e storia proprietaria.
5. **RF-5** — I dati del percorso sono **deterministici e inventati** (nomi, esercenti, immagini generate al
   momento, indirizzi nel dominio riservato alle prove); non c'è nessuna attesa a tempo e l'accesso avviene in modo
   programmatico.
6. **RF-6** — Il percorso attraversa **due utenti con ruoli diversi** — chi sostiene e chi approva — perché
   l'approvazione di sé stessi non prova nulla del meccanismo che l'app esiste per garantire.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso usa un account dedicato e verifica, in un passo esplicito,
  che un utente del secondo account non veda nulla del primo: il percorso end-to-end è l'ultimo posto in cui
  l'isolamento può ancora essere provato dal punto di vista dell'utente.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova. Il percorso guida l'interfaccia, non le
  rotte: se serve chiamare una rotta per preparare lo stato, lo si fa nella preparazione, non nei passi provati.
- **RT-3 — Persistenza (§8).** Nessuna migrazione. I dati del percorso nascono dai dati di prova della storia
  `0005`, estesi dove serve.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Il percorso verifica che le schermate esistenti
  siano raggiungibili e funzionanti in sequenza.
- **RT-5 — Cinque lingue (§4).** Il percorso gira in una lingua sola (quella predefinita) per non moltiplicarsi;
  la presenza delle cinque lingue è verificata dalle prove del frontend, non da qui. Va detto, altrimenti sembra
  una dimenticanza.
- **RT-6 — Varchi e quota (§6, §7).** Un ramo del percorso porta l'account a quota esaurita e verifica che la
  conferma risponda con il blocco e il messaggio di rimedio, invece di fallire in modo oscuro.
- **RT-7 — Esposizione conversazionale (§12).** Il percorso **non** prova gli strumenti conversazionali: il livello
  non esiste (UC 0061-0063). Prova invece che il contratto dichiarato dalle storie `0028` e `0029` sia presente e
  coerente, e che i due strumenti esclusi non ci siano.
- **RT-8 — Dati personali (§10).** I dati del percorso sono **inventati** e vanno dichiarati come tali; le immagini
  di ricevuta sono generate, non fotografie di documenti veri. Un percorso end-to-end che usasse dati veri sarebbe
  un trattamento non dichiarato.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo; il percorso può verificare che gli eventi attesi siano
  stati registrati, ma **non** che contengano dati — perché non devono contenerne.
- **RT-10 — Accessibilità (§11).** Il controllo automatico di accessibilità gira sulle schermate principali
  attraversate dal percorso.

## 4. Criteri di accettazione

**CA-1 — Ciclo completo verde**
- **Dato** lo stack locale avviato con i dati di prova
- **Quando** si esegue `./run-tests.sh platform`
- **Allora** il percorso `[J-NOTESPESE]` completa tutti i passi dalla ricevuta al pacchetto e termina verde

**CA-2 — Due ruoli**
- **Dato** il percorso in esecuzione
- **Quando** si arriva all'approvazione
- **Allora** l'atto è compiuto da un utente diverso da chi ha composto la nota, e il tentativo di approvarla con il
  primo utente fallisce come previsto

**CA-3 — Registro coerente**
- **Dato** il registro di copertura
- **Quando** gira il controllo dell'area `tooling`
- **Allora** è verde: nessuna voce senza test, nessun test senza voce, nessun rimando senza motivo e storia
  proprietaria

**CA-4 — Rami dichiarati**
- **Dato** il percorso
- **Quando** lo si esamina
- **Allora** contiene i quattro rami dichiarati: quota esaurita, doppione scartato, avviso sulla deducibilità,
  percorrenza chilometrica

**CA-5 — Nessuna attesa a tempo**
- **Dato** il codice del percorso
- **Quando** lo si esamina
- **Allora** non contiene attese a tempo fisso: le attese sono su condizioni osservabili

**CA-6 — Isolamento visto dall'utente**
- **Dato** il secondo account del percorso
- **Quando** il suo utente naviga nel modulo
- **Allora** non vede nessuna spesa, nessuna nota e nessun pacchetto del primo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` **completo**: è la storia in cui la suite intera deve essere verde, non
      solo le aree toccate;
- [ ] il percorso `[J-NOTESPESE]` esiste, gira senza finestra sullo stack locale reale ed è stabile su tre
      esecuzioni consecutive;
- [ ] prova di **isolamento fra account** presente anche nel percorso, dal punto di vista dell'utente;
- [ ] **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
      aggiornato e verde al controllo automatico dell'area `tooling`;
- [ ] **traduzioni**: nessuna stringa nuova; il percorso gira in una lingua e lo dichiara;
- [ ] **manifesto dei dati**: nessuna voce nuova; i dati del percorso sono dichiarati inventati;
- [ ] **registro delle decisioni** compilato, con i rami scelti e quelli deliberatamente esclusi;
- [ ] contratto degli **strumenti conversazionali** verificato per presenza e coerenza, compresa l'assenza dei due
      strumenti esclusi;
- [ ] controllo automatico di **accessibilità** verde sulle schermate attraversate;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001`–`0030` | Il percorso attraversa tutto: senza una delle storie, un passo non esiste |
| Percorso di piattaforma e registro di copertura (UC 0093, 0094) | Formato del registro, convenzione dell'etichetta e controllo automatico sono di piattaforma |

## 7. Fuori ambito

- Le prove degli strumenti conversazionali in esecuzione: il livello non esiste (UC 0061-0063). Si prova il
  contratto, non l'esposizione.
- Le prove di carico: il ciclo della nota spese di una micro-impresa non ne ha bisogno, e farle qui darebbe una
  falsa sicurezza.
- Le prove sul fornitore reale di lettura automatica: in locale il fornitore è **sempre** simulato, e nel percorso a
  maggior ragione.

## 8. Punti aperti

- **Quanto del ramo forfettario delle trasferte portare nel percorso**: la storia `0021` lo ha rimandato qui, ma un
  percorso che copre ogni variante diventa lento e fragile. Proposta: coprire il regime analitico nel percorso
  principale e il forfettario come variante breve. Da confermare con chi mantiene la suite.
