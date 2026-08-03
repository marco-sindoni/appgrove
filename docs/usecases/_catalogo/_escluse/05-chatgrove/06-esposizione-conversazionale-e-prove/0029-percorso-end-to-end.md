# 0029 — Percorso end-to-end dell'app

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0023`, `0028`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio un percorso automatico che parta da un messaggio in arrivo e arrivi all'incasso registrato
> così da sapere, a ogni modifica, che la catena di vendita di ChatGrove funziona davvero e non solo a pezzi.

**Contesto.** Le storie precedenti hanno ciascuna le proprie prove, ma nessuna verifica che la **catena** regga:
messaggio → conversazione → carrello → ordine → richiesta di pagamento → incasso. È esattamente la sequenza che
il cliente compie ogni giorno, ed è quella che si rompe quando si modifica un pezzo. La storia chiude anche i
rimandi lasciati aperti da quasi tutte le altre nel registro di copertura: è il momento in cui quei debiti si
pagano.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-CHAT-COMMERCE.spec.ts` che percorre, sullo
   stack locale reale: accesso, abilitazione all'app, connessione del canale simulato, messaggio in arrivo,
   risposta dentro la finestra, invio di una scheda prodotto, carrello, ordine, richiesta di pagamento con
   conferma, registrazione dell'incasso.
2. **RF-2** — Ogni prova del percorso porta l'etichetta in testa al titolo: `test('[J-CHAT-COMMERCE] …')`.
3. **RF-3** — Il percorso verifica anche i due comportamenti di blocco: **finestra chiusa** (la casella di
   scrittura è disattivata) e **quota esaurita** (l'invio con modello risponde `429` e nulla parte).
4. **RF-4** — Il percorso verifica la traccia dell'origine (storia `0028`) su almeno un'azione.
5. **RF-5** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è
   aggiornato: le voci `da-coprire` lasciate dalle storie precedenti sono chiuse o motivate una per una.
6. **RF-6** — I dati del percorso sono **inventati e deterministici**: nomi finti, numeri non assegnabili a
   persone reali, indirizzi `*.test`, nessuna attesa a tempo.

## 3. Requisiti tecnici

- **RT-1 — Prove (§11).** Playwright senza finestra, sullo stack locale reale; accesso programmatico; nessuna
  attesa a tempo; dati di prova deterministici e inventati. Il percorso usa il **canale simulato** della storia
  `0005`: nessuna chiamata a un fornitore vero, mai.
- **RT-2 — Registro di copertura (§11).** L'etichetta `[J-CHAT-COMMERCE]` in testa al titolo di ogni prova e la
  voce nel registro sono sorvegliate dal controllo automatico: registro incoerente = suite rossa.
- **RT-3 — Isolamento fra account (§1).** Il percorso gira su un account dedicato e verifica, in almeno un
  passo, che un secondo account non veda nulla di quanto creato.
- **RT-4 — Varchi e quota (§6, §7).** Il percorso comprende il caso di quota esaurita, che è il comportamento
  di blocco più visibile per il cliente.
- **RT-5 — Dati personali (§10).** Il percorso crea dati di persone finte: alla fine li rimuove, oppure gira su
  un ambiente effimero. Nessun dato reale entra mai in una prova.
- **RT-6 — Cinque lingue (§4).** Almeno un passo del percorso gira con l'interfaccia in una lingua diversa
  dall'inglese, per accorgersi delle stringhe non tradotte.
- **RT-7 — Avvio locale (§15).** Il percorso presuppone soltanto `./app-start.sh` e la scoperta automatica dei
  servizi: nessun passo manuale di preparazione.

## 4. Criteri di accettazione

**CA-1 — La catena regge**
- **Dato** lo stack locale avviato con il canale simulato
- **Quando** si esegue `./run-tests.sh platform`
- **Allora** il percorso `[J-CHAT-COMMERCE]` è verde dall'accesso fino all'incasso registrato

**CA-2 — Finestra chiusa**
- **Dato** una conversazione con finestra scaduta nel percorso
- **Quando** la prova apre la conversazione · **Allora** verifica che la casella di scrittura sia disattivata e
  che la spiegazione sia presente

**CA-3 — Quota esaurita**
- **Dato** un account portato al tetto di `messaggi_template` dentro il percorso
- **Quando** si tenta l'invio con modello · **Allora** la prova verifica il blocco e che nessun messaggio sia
  partito nel canale simulato

**CA-4 — Registro coerente**
- **Dato** il registro di copertura aggiornato · **Quando** si esegue `./run-tests.sh tooling`
- **Allora** il controllo è verde: nessuna voce orfana, nessuna storia con superficie senza percorso

**CA-5 — Nessuna chiamata all'esterno**
- **Dato** l'esecuzione del percorso · **Quando** si osservano le chiamate uscenti · **Allora** nessuna è
  diretta al fornitore del canale: tutte finiscono nel simulatore

**CA-6 — Una lingua diversa**
- **Dato** un passo eseguito con l'interfaccia in italiano · **Quando** la prova verifica i testi visibili
- **Allora** non compare alcuna chiave di traduzione grezza

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` — **l'intera suite**, comprese le aree `platform` e `tooling`;
- [ ] prove di **unità** e **integrazione**: non introdotte da questa storia, che ne è la verifica d'insieme;
- [ ] prova di **isolamento fra account** presente come passo del percorso;
- [ ] **prova end-to-end**: *coperta ora* — è l'oggetto della storia; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato e i rimandi delle
      storie precedenti sono chiusi o motivati;
- [ ] **traduzioni**: verificate dal percorso in almeno una lingua diversa dall'inglese;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che i dati di prova siano inventati;
- [ ] **registro delle decisioni** compilato, con l'elenco dei passi del percorso e il perché di quel taglio;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; il percorso non prova il livello
      conversazionale, che non esiste ancora;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: `docs/testing/README.md` se cambia il modo di leggere il registro.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0019` | Il percorso arriva fino all'incasso |
| `0023` | Il percorso tocca la campagna, o almeno il suo controllo preventivo |
| `0028` | Il percorso verifica l'origine dell'azione |
| Storia `0005` | Il canale simulato è la condizione perché il percorso non esca verso la rete |

## 7. Fuori ambito

- il percorso del livello conversazionale: non esiste il server (UC 0061-0063), quindi non c'è nulla da
  percorrere;
- le prove di carico e la resistenza a volumi alti: non richieste in questa fase;
- la prova su un fornitore del canale vero: appartiene alla verifica pre-rilascio, non alla suite bloccante —
  è la stessa regola per cui non si guida con Playwright la finestra del fornitore di pagamento.

## 8. Punti aperti

- **Verifica con un numero vero prima del rilascio.** Il canale simulato prova la nostra parte, non il
  fornitore. Serve almeno una verifica manuale con un numero reale prima di mettere l'app nelle mani dei
  clienti: è un passo di rilascio, da inserire nella guida di verifica manuale della change che implementerà
  questa storia.
