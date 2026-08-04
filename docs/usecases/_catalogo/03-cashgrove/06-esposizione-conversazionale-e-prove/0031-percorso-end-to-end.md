# 0031 — Percorso end-to-end

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0014`, `0016`, `0017`, `0021`, `0022`, `0023`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio una prova automatica che percorra CashGrove dall'inizio alla fine sullo stack locale reale
> così da sapere, a ogni modifica, se la catena che porta dal credito scaduto all'incasso è ancora intera.

**Contesto.** Le storie precedenti hanno ciascuna le proprie prove, e diverse hanno lasciato una voce `da-coprire` nel
registro di copertura indicando questa storia come proprietaria. È il momento di riscuotere: si costruisce il percorso
`[J-CREDITI]`, che attraversa la superficie utente vera, e si mette in ordine il registro. Il percorso non serve a
verificare i dettagli — quelli li coprono le prove di unità e integrazione — serve a verificare che i pezzi si parlino:
è la classe di guasti che nessuna prova per componente vede.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-CREDITI.spec.ts`, eseguito senza finestra sullo stack
   locale reale, con accesso programmatico e dati di prova deterministici e inventati.
2. **RF-2** — Il percorso attraversa, nell'ordine: attivazione e comparsa del modulo → importazione di un file di
   crediti → maturazione e invio di un sollecito per posta elettronica con fornitore simulato → verifica della riga nel
   registro dei solleciti → apertura della pagina pubblica da parte del debitore → registrazione di un incasso →
   verifica che il sollecito successivo **non** parta → verifica che il prospetto di anzianità sia cambiato.
3. **RF-3** — Un secondo percorso, più corto, copre il caso formale: credito che esaurisce la sequenza → stato
   `in_escalation` → generazione della bozza di messa in mora con mora calcolata.
4. **RF-4** — Un terzo percorso copre la regola della bozza e della conferma: creazione di una bozza di sollecito,
   tentativo di esecuzione senza conferma (rifiutato), conferma, invio.
5. **RF-5** — Ogni test porta l'etichetta del percorso in testa al titolo, nella forma `test('[J-CREDITI] …')`.
6. **RF-6** — Il registro `docs/testing/copertura-e2e.yaml` è aggiornato: le voci `da-coprire` lasciate dalle storie
   precedenti sono chiuse o riformulate, e nessuna resta orfana.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso esercita due account e verifica, in almeno un punto, che i dati
  dell'uno non compaiano nell'altro: l'isolamento va provato anche end-to-end, non solo nelle prove di integrazione.
- **RT-2 — Interfaccia di programmazione (§2).** Il percorso usa l'interfaccia utente reale per le azioni e le rotte
  solo per la preparazione dello stato: un percorso che fa tutto dalle rotte non prova la superficie.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. I dati di prova nascono dal comando della storia `0005`, esteso
  con quanto serve ai tre percorsi.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Se il percorso trova difficile individuare un elemento,
  la correzione è aggiungere gli attributi di accessibilità mancanti nel modulo, non aggirare il problema nel test.
- **RT-5 — Cinque lingue (§4).** Il percorso gira in una lingua sola; un controllo separato verifica che nessuna chiave
  resti non tradotta nelle cinque lingue.
- **RT-6 — Varchi e quota (§6, §7).** Il percorso comprende un passo con **quota esaurita**: si verifica il `429`, il
  messaggio con il rimedio e il fatto che nulla sia stato creato.
- **RT-7 — Esposizione conversazionale (§12).** Il terzo percorso (RF-4) esercita il contratto degli strumenti di
  scrittura per la parte già verificabile — bozza, rifiuto senza conferma, conferma — anche se il server
  conversazionale di piattaforma non esiste ancora (UC 0061-0063): si invoca il contratto direttamente.
- **RT-8 — Dati personali (§10).** I dati del percorso sono **inventati**: nomi di fantasia, indirizzi su dominio
  `*.test`, nessun dato reale. Il fornitore di posta è **simulato**: nessun messaggio esce davvero.
- **RT-9 — Registrazione eventi (§14).** Nessun evento applicativo nuovo.
- **RT-10 — Niente attese a tempo.** Il percorso attende condizioni osservabili, mai un numero di secondi: un percorso
  che dipende dai tempi è un percorso che fallirà a caso e verrà disattivato.

## 4. Criteri di accettazione

**CA-1 — Il percorso principale è verde**
- **Dato** lo stack locale avviato con i dati di prova
- **Quando** si esegue `./run-tests.sh platform`
- **Allora** il percorso `[J-CREDITI]` passa attraversando tutti i passi di RF-2

**CA-2 — Il sollecito non parte dopo l'incasso**
- **Dato** il percorso al passo dell'incasso
- **Quando** matura il sollecito successivo
- **Allora** il test verifica che **nessun messaggio** sia stato consegnato al fornitore simulato e che l'invio risulti
  annullato con la ragione

**CA-3 — Quota esaurita**
- **Dato** l'account di prova al tetto · **Quando** il percorso tenta di registrare un credito in più · **Allora**
  osserva il messaggio di quota esaurita nell'interfaccia, con il rimedio

**CA-4 — Bozza senza conferma**
- **Dato** una bozza di sollecito creata dal contratto degli strumenti · **Quando** si tenta di eseguirla senza conferma
  · **Allora** il tentativo è rifiutato e nessun messaggio risulta consegnato

**CA-5 — Registro di copertura coerente**
- **Dato** il registro `docs/testing/copertura-e2e.yaml` aggiornato
- **Quando** si esegue il controllo `tools/e2e-coverage` nell'area `tooling`
- **Allora** il controllo è verde e nessuna voce `da-coprire` è rimasta senza storia proprietaria

**CA-6 — Isolamento end-to-end**
- **Dato** i due account di prova · **Quando** il percorso accede con il secondo · **Allora** non vede alcun credito del
  primo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` **completo**: è la storia che chiude l'applicazione e non ammette aree rosse;
- [ ] i tre percorsi esistono, sono etichettati `[J-CREDITI]` e girano senza finestra e senza attese a tempo;
- [ ] prova di **isolamento fra account** anche a livello di percorso;
- [ ] **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato e
      verde al controllo automatico;
- [ ] **traduzioni**: controllo che nessuna chiave resti non tradotta nelle cinque lingue;
- [ ] **manifesto dei dati**: nessuna modifica, ma si verifica che i dati di prova non contengano nulla di reale;
- [ ] **registro delle decisioni** compilato con la composizione dei tre percorsi e il motivo di ciascuno;
- [ ] contratto degli **strumenti conversazionali**: esercitato nella parte verificabile;
- [ ] `./dev.sh services`, `./app-start.sh` e `tools/smoke` funzionano senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0008`, `0014`, `0016`, `0017`, `0021`, `0022`, `0023` | Sono i passi che il percorso attraversa |
| storia `0029` | Il terzo percorso esercita bozza e conferma |
| UC 0061-0063 (livello conversazionale) | Non implementati: il contratto si invoca direttamente, senza server |

## 7. Fuori ambito

- Percorsi per le funzioni coperte da sole prove di integrazione (canali brevi, indicatori, previsione, esportazione):
  la scelta è dichiarata storia per storia ed è registrata nel registro di copertura.
- Il livello di prova sui pagamenti con fornitore reale, che è pre-rilascio e di piattaforma.
- Le prove di carico: fuori dal perimetro di questa applicazione.

## 8. Punti aperti

Nessuno.
