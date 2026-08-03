# 0037 — Percorso end-to-end e registro di copertura

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0037` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: tutte le storie precedenti dell'applicazione (`0001`-`0036`)
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio un percorso automatico che parta dal modulo di contatto di un cliente finale e arrivi al suo voto di
> soddisfazione, passando dallo stack vero
> così da sapere che l'applicazione funziona **davvero**, e non soltanto che le sue parti superano ciascuna la
> propria prova.

**Contesto.** Ogni applicazione della piattaforma ha il proprio percorso end-to-end in
`tools/platform-e2e/journeys/J-<APP>.spec.ts`, e ogni prova porta l'etichetta del percorso in testa al titolo. Il
registro `docs/testing/copertura-e2e.yaml` tiene la mappa *storia → percorso → prova* ed è sorvegliato da un
controllo automatico: registro incoerente uguale suite rossa. Questa storia chiude l'applicazione perché è
l'unica che può percorrerla tutta — e perché il valore del percorso sta proprio nel toccare le giunture, che sono
i punti dove le prove di unità non guardano mai: il messaggio che entra da un canale ed esce da un altro, il
contatore del livello di servizio che si ferma e riprende, la bozza che diventa messaggio solo dopo che una
persona ha detto di sì.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-HELPDESK.spec.ts`, con ogni prova etichettata
   `[J-HELPDESK]` in testa al titolo.
2. **RF-2** — Il percorso principale segue la vita completa di una richiesta: un cliente finale invia il modulo di
   contatto → la richiesta compare in coda → un operatore la prende in carico → prepara una bozza dal livello
   conversazionale, la corregge e la approva → la risposta parte → il cliente finale la legge dal collegamento di
   stato e replica → l'operatore risolve → parte l'invito a votare → il cliente vota.
3. **RF-3** — Un secondo percorso copre il **blocco per quota**: un account al tetto dei posti operatore tenta di
   abilitarne un altro e riceve il rifiuto con il messaggio che dice come rimediare.
4. **RF-4** — Un terzo percorso copre l'**isolamento fra account** sulle superfici pubbliche: il portale degli
   articoli e il collegamento di stato di un account non mostrano nulla di un altro account, nemmeno manipolando
   l'identificativo o il gettone.
5. **RF-5** — Il registro `docs/testing/copertura-e2e.yaml` riporta le voci di questa applicazione e il controllo
   automatico è verde.
6. **RF-6** — I dati usati dal percorso sono **inventati e deterministici**, con indirizzi su dominio riservato
   alle prove: nessun dato vero, mai.

## 3. Requisiti tecnici

- **RT-1 — Prove end-to-end (§11).** Percorso con Playwright senza finestra, sullo **stack locale reale**: niente
  attese a tempo, accesso programmatico, dati deterministici. La posta si verifica sulla casella simulata dello
  stack, non su un servizio esterno. **Vietato** guidare con il browser la finestra del fornitore di pagamento: lo
  stato dell'abbonamento si predispone dal server.
- **RT-2 — Registro di copertura (§11).** Le voci del registro dichiarano per ogni storia con superficie il
  percorso richiesto e la prova che lo copre; le storie senza superficie restano con la risposta esplicita già data
  nella loro definizione di fatto — *rimando* o *nessun impatto* — e questa storia le riconcilia. Il silenzio non è
  una risposta ammessa.
- **RT-3 — Isolamento fra account (§1).** Il percorso di isolamento usa **due** account veri creati dai dati di
  prova, non finti, e tenta l'accesso incrociato sulle sole superfici pubbliche introdotte dall'epica 06, che sono
  quelle dove l'errore costerebbe di più.
- **RT-4 — Cinque lingue (§4).** Il percorso esegue almeno un passo con l'interfaccia in una lingua diversa
  dall'inglese, per accorgersi delle stringhe dimenticate: è il modo più economico per non scoprirle in produzione.
- **RT-5 — Dati personali (§10).** Nessun dato personale nuovo e nessun dato reale: i richiedenti del percorso sono
  inventati e i loro indirizzi stanno sul dominio riservato alle prove.
- **RT-6 — Esposizione conversazionale (§12).** Il passo della bozza si esercita attraverso la risorsa del servizio,
  non attraverso il server conversazionale, che **non esiste ancora** (UC 0061-0063): quello che si prova qui è che
  il presidio — bozza, correzione, conferma umana — funziona **dentro l'app**, dove deve stare.
- **RT-7 — Registrazione eventi (§14).** Il percorso verifica che gli eventi principali vengano registrati con
  `tenant_id`, `app_id`, `user_id` e che **nessun** corpo di messaggio finisca nei registri: è un controllo che
  vale la pena automatizzare, perché è esattamente il difetto che nessuno nota fino a che non è tardi.

## 4. Criteri di accettazione

**CA-1 — Il percorso principale è verde**
- **Dato** lo stack locale avviato e i dati di prova caricati
- **Quando** si esegue `./run-tests.sh platform`
- **Allora** il percorso `[J-HELPDESK]` copre l'intera vita di una richiesta, dal modulo di contatto al voto, e
  l'esito è verde

**CA-2 — Nessuna risposta esce senza una persona**
- **Dato** una bozza preparata durante il percorso
- **Quando** il percorso arriva al passo dell'invio **senza** eseguire l'approvazione
- **Allora** nessun messaggio risulta consegnato alla casella simulata del cliente finale

**CA-3 — Il blocco per quota si vede**
- **Dato** un account di prova al tetto dei posti operatore
- **Quando** il percorso tenta di abilitarne un altro
- **Allora** l'interfaccia mostra l'avviso che dice quanti posti ci sono e come si rimedia, e nessun posto viene
  assegnato

**CA-4 — Isolamento sulle superfici pubbliche**
- **Dato** due account `A` e `B`, ciascuno con articoli pubblicati e una richiesta con collegamento di stato
- **Quando** il percorso usa il gettone di `A` manipolato per puntare a una richiesta di `B`
- **Allora** la pagina non mostra alcun dato di `B` e l'accesso è respinto

**CA-5 — Il registro è coerente**
- **Dato** il registro di copertura aggiornato
- **Quando** si esegue l'area `tooling` della suite
- **Allora** il controllo del registro è verde: ogni storia con superficie ha il proprio percorso e ogni etichetta
  citata esiste davvero

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` — qui **l'intera suite**, non solo le aree toccate: è la storia che
      chiude l'applicazione;
- [ ] prove di **unità** e **integrazione**: nessuna nuova, la storia non introduce logica di dominio;
- [ ] prova di **isolamento fra account** compresa nel percorso, sulle superfici pubbliche;
- [ ] **prova end-to-end**: *coperta ora* — percorso `[J-HELPDESK]` creato e registro
      `docs/testing/copertura-e2e.yaml` aggiornato con le voci di questa applicazione; le voci rimandate dalle
      storie precedenti vengono chiuse o restano con motivo e storia proprietaria dichiarati;
- [ ] **traduzioni**: nessuna nuova, ma il percorso ne verifica la presenza in almeno una lingua diversa
      dall'inglese;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con annotato che cosa il percorso copre e che cosa **deliberatamente**
      non copre;
- [ ] contratto degli **strumenti conversazionali**: già dichiarato dalle storie `0034` e `0035`, qui verificato in
      esecuzione per la parte che vive dentro l'app;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: la scheda dell'app in `docs/testing/README.md` se il percorso introduce una
      convenzione nuova.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Tutte le storie `0001`-`0036` di questa app | Il percorso attraversa l'applicazione intera: se una parte manca, il percorso non si può scrivere per intero |
| Storia `0005` di questa app | I dati di prova deterministici sono la base su cui il percorso poggia |
| Registro di copertura e controllo automatico (UC 0093-0094) | Il registro è sorvegliato: incoerente uguale suite rossa |
| Percorso end-to-end del canale di posta (storie `0014`, `0015`) | La verifica della posta usa la casella simulata dello stack locale |

## 7. Fuori ambito

- **Il canale WhatsApp** (storia `0017`): non è percorribile in locale senza un fornitore esterno, e la storia
  stessa è sospesa a una decisione dello sviluppatore. Nel registro di copertura resta una voce **da coprire** con
  motivo e storia proprietaria.
- **Il livello conversazionale vero** (UC 0061-0063): non esiste, quindi non si percorre. Ciò che si prova è il
  presidio dentro l'app.
- **Le prove di carico**: non sono un percorso end-to-end e non appartengono a questa storia.

## 8. Punti aperti

- **Quanto deve durare il percorso.** Un percorso che attraversa nove passi e due canali rischia di diventare
  lento e fragile, e un percorso fragile viene disattivato — che è il modo peggiore di perdere copertura. Se
  dovesse superare i tempi accettabili, la strada è **dividerlo in due percorsi coerenti** (la vita della richiesta,
  le superfici pubbliche), non ridurre ciò che verifica.
- **La copertura del canale WhatsApp** resta aperta finché resta aperta la storia `0017`: è una voce del registro
  con motivo scritto, non una dimenticanza.
