# 0019 — Pubblicazione della risposta

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 04 — Risposte e recensioni negative
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha riletto la bozza e la trova giusta
> voglio pubblicarla sulla piattaforma con un gesto esplicito, sapendo che da quel momento è pubblica
> così da non trovarmi mai una risposta a nome della mia azienda che non ho letto.

**Contesto.** È l'unico punto dell'app in cui qualcosa esce verso il mondo a nome del cliente, ed è quindi il
punto in cui vale la regola di sicurezza del catalogo: **l'intelligenza artificiale prepara, la persona approva**
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §12). La conferma umana non è una finestra di dialogo che
si può disattivare nelle impostazioni: è nella macchina a stati (storia 0002), che non ha nessuna transizione da
`bozza` a `pubblicata` senza passare da `approvata` con un utente.

C'è un secondo presidio, meno ovvio e altrettanto importante. Una risposta pubblica può rivelare dati che la
recensione non conteneva: «Signora Rossi, il trattamento che le abbiamo fatto…». La risposta finisce su una pagina
pubblica e non si può richiamare. Prima di pubblicare, l'app avvisa se il testo contiene qualcosa che sembra un
dato personale non presente nella recensione.

## 2. Requisiti funzionali

1. **RF-1** — La pubblicazione richiede due gesti distinti: approvare la bozza e confermare la pubblicazione, con
   un riepilogo che mostra il testo esatto che verrà pubblicato e dove.
2. **RF-2** — Prima della conferma, un controllo segnala se il testo contiene elementi che sembrano dati personali
   non presenti nella recensione (nomi propri, indirizzi di posta, numeri di telefono, riferimenti a prestazioni).
   È un **avviso**, non un blocco: la decisione resta della persona, ma consapevole.
3. **RF-3** — La pubblicazione avviene attraverso il collegamento della sede alla piattaforma; l'esito è
   registrato con il momento e l'identificativo restituito. In caso di errore la risposta resta `approvata`, non
   scompare, e si può ritentare.
4. **RF-4** — Una risposta già pubblicata si può **modificare** (le piattaforme lo permettono) e la modifica passa
   dallo stesso percorso di conferma; la versione precedente resta nella storia.
5. **RF-5** — Non esiste nessuna impostazione, nessuna automazione e nessuna chiamata che pubblichi una risposta
   senza un utente che l'ha confermata. Nemmeno per le recensioni a cinque stelle, nemmeno «solo per i
   ringraziamenti».
6. **RF-6** — Chi pubblica deve avere ruolo `admin` o `owner`: parlare a nome dell'azienda in pubblico non è
   un'azione da `member`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La pubblicazione usa il collegamento dell'account proprietario della
  recensione; nessun percorso permette di pubblicare su un collegamento di un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/recensioni/v1/risposte/{id}/approva` e
  `POST /api/recensioni/v1/risposte/{id}/pubblica`; la seconda è **idempotente** rispetto a un identificativo di
  operazione, così che un doppio clic o un tentativo ripetuto non pubblichi due volte; errori in
  `application/problem+json` con codici distinti per «collegamento scaduto», «rifiutata dalla piattaforma»,
  «non approvata».
- **RT-3 — Persistenza (§8).** Stato, autore dell'approvazione, autore della pubblicazione, momento e
  identificativo esterno su `risposta`; la storia delle versioni in una tabella figlia o come righe successive —
  la scelta va motivata, non improvvisata.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro della risposta con i due passi separati, finestra di conferma che
  mostra il testo definitivo e la destinazione, e l'esito ben visibile. Solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, avvisi del controllo compresi, in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Ruolo `admin` o `owner`; `402` con abbonamento `canceled`; nessun consumo di
  quota. Con collegamento `scaduto` o `revocato` la pubblicazione è rifiutata con un messaggio che dice di
  ricollegare la piattaforma.
- **RT-7 — Esposizione conversazionale (§12).** È lo strumento `pubblica_risposta` (storia 0028), marcato
  **scrittura irreversibile**: produce una richiesta di conferma e **non esegue nulla** finché una persona non
  conferma. È l'esempio da manuale della regola del catalogo: nessun assistente pubblica al posto di nessuno.
- **RT-8 — Dati personali (§10).** `risposta.testo` è già dichiarato (storia 0018). Qui si aggiunge il fatto
  rilevante: **il testo esce verso una piattaforma pubblica**. Va scritto nel manifesto come destinazione del
  dato e va spiegato all'utente nella finestra di conferma.
- **RT-9 — Registrazione eventi (§14).** `risposta approvata`, `risposta pubblicata`, `pubblicazione fallita` con
  il codice, `risposta modificata`, tutti con `tenant_id`, `app_id`, `user_id`, identificativo della recensione e
  identificativo di correlazione. **Mai** il testo nei registri.

## 4. Criteri di accettazione

**CA-1 — Pubblicazione riuscita**
- **Dato** una bozza rivista, un collegamento attivo e un utente con ruolo `admin`
- **Quando** approva e conferma la pubblicazione
- **Allora** la risposta risulta `pubblicata` con momento e identificativo esterno, e la scheda della recensione
  la mostra

**CA-2 — Nessuna pubblicazione senza conferma**
- **Dato** una bozza generata dall'assistente
- **Quando** passa il tempo, gira qualunque lavorazione o si chiama qualunque rotta automatica
- **Allora** la risposta resta `bozza` e nulla viene pubblicato

**CA-3 — Avviso sui dati personali**
- **Dato** una bozza che contiene un cognome e un numero di telefono non presenti nella recensione
- **Quando** si chiede la pubblicazione
- **Allora** l'app avvisa indicando gli elementi trovati, e pubblica solo se la persona conferma comunque

**CA-4 — Collegamento scaduto**
- **Dato** un collegamento in stato `scaduto`
- **Quando** si tenta di pubblicare
- **Allora** l'operazione è rifiutata con un messaggio che dice di ricollegare la piattaforma, e la risposta resta
  `approvata`

**CA-5 — Nessun doppione**
- **Dato** una pubblicazione già riuscita
- **Quando** si ripete la stessa richiesta con lo stesso identificativo di operazione
- **Allora** l'esito è lo stesso e non viene pubblicata una seconda risposta

**CA-6 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** tenta di approvare o pubblicare
- **Allora** riceve `403` e nulla cambia

**CA-7 — Isolamento fra account**
- **Dato** due account con risposte in bozza
- **Quando** un utente di `A` tenta di pubblicare una risposta di `B`
- **Allora** riceve `404` e nulla viene pubblicato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati (con la verifica esplicita che **non esiste** un percorso da
      `bozza` a `pubblicata`) e sul controllo dei dati personali; di **integrazione** sulle rotte con piattaforma
      **simulata**;
- [ ] prova di **isolamento fra account** sulla pubblicazione;
- [ ] **prova end-to-end**: *coprire ora* il passo «approvo e pubblico la risposta» nel percorso `[J-RECENSIONI]`,
      con piattaforma simulata, e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la destinazione pubblica del testo della risposta;
- [ ] **registro delle decisioni** compilato, con la scelta di rendere la conferma parte della macchina a stati e
      non dell'interfaccia;
- [ ] contratto degli **strumenti conversazionali**: `pubblica_risposta`, scrittura irreversibile, conferma
      obbligatoria e non disattivabile.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` | serve una bozza da approvare |
| storia `0007` (o `0008`) | serve un collegamento attivo con il permesso di rispondere |

## 7. Fuori ambito

- la cancellazione di una risposta pubblicata: le piattaforme lo permettono, ma è un'operazione rara e va
  valutata a parte;
- la segnalazione di una recensione — storia 0021.

## 8. Punti aperti

- **Il controllo sui dati personali è un avviso, non un blocco.** È la scelta che propongo: bloccare la
  pubblicazione per un falso positivo sarebbe peggio del rischio che evita, perché spingerebbe a scrivere le
  risposte fuori dall'app. Va confermata.
- **Storia delle versioni della risposta**: due modi possibili (tabella figlia o righe successive). La scelta ha
  effetti sulla cancellazione dei dati e va motivata nel registro delle decisioni.
</content>
