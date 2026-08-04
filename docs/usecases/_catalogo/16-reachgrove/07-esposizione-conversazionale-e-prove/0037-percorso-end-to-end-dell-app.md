# 0037 — Percorso end-to-end dell'app

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0037` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: tutte le storie precedenti — è l'ultima dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio una prova automatica che percorra ReachGrove dall'iscrizione alla disiscrizione sullo stack locale reale
> così da sapere che la catena del consenso regge davvero, e non solo che i suoi pezzi passano le proprie prove.

**Contesto.** Ogni app di appgrove ha il suo percorso end-to-end e ogni percorso è registrato nella mappa
*use case → percorso → test*, sorvegliata da un controllo automatico: registro incoerente significa suite rossa.
Qui il percorso ha un compito in più che altrove. In questa app la promessa non è «i messaggi partono»: è **«non
si può mandare un messaggio a chi non ha acconsentito»**, e una promessa negativa si dimostra solo provando che il
tentativo fallisce. Perciò il percorso non si limita al caso felice: contiene i casi in cui l'app **rifiuta**.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `tools/platform-e2e/journeys/J-CAMPAIGNS.spec.ts`, eseguito senza finestra sullo
   stack locale reale, con accesso programmatico e dati inventati deterministici.
2. **RF-2** — Il percorso copre, in quest'ordine: attivazione dell'app → verifica del dominio mittente →
   iscrizione dal modulo pubblico con doppia conferma → comparsa della registrazione di consenso con testo e
   momento → creazione di un segmento → composizione della campagna → controllo pre-volo verde → invio →
   disiscrizione in un clic dal messaggio ricevuto → verifica che il recapito sia soppresso → lettura del
   rapporto con l'escluso e del cruscotto di salute della lista → esportazione dei numeri.
3. **RF-3** — Ogni test porta **l'etichetta del percorso in testa al titolo**: `test('[J-CAMPAIGNS] …')`.
4. **RF-4** — Il percorso verifica i quattro casi in cui l'app deve **rifiutare**: un iscritto non confermato non
   riceve; una lista importata senza prova resta in quarantena e la campagna la esclude; una campagna con dominio
   mittente non verificato non lascia lo stato di verifica; un secondo invio verso un recapito soppresso non
   genera alcun invio. Sono i casi che dimostrano la promessa dell'app.
5. **RF-5** — Il percorso verifica anche i due casi trasversali di piattaforma: un secondo account non vede nulla
   del primo, e il superamento del tetto della metrica `messages_sent` risponde `429` senza generare invii.
6. **RF-6** — Il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) contiene una
   voce per ogni storia che ha risposto «coprire ora», e una voce `da-coprire` con motivo e storia proprietaria
   per quelle che hanno risposto «rimando» — fra cui la storia `0024` (canale di messaggistica, non implementata
   perché subordinata alla revisione legale, §11.1 della descrizione), la `0031` e la `0036`. Le storie che hanno
   risposto «nessun impatto» non compaiono.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il percorso usa **due** account e verifica esplicitamente che il
  secondo non veda né iscritti né campagne né rapporti del primo: è la prova end-to-end dell'invariante numero uno.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova. Il percorso attraversa quelle esistenti,
  comprese le due superfici pubbliche: il modulo di iscrizione e il collegamento di disiscrizione.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; i dati del percorso sono inventati e deterministici, con
  indirizzi nel dominio `.test`, riservato alle prove. Il fornitore di consegna della posta è simulato: nessun
  messaggio esce davvero.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova; il percorso attraversa quelle esistenti del modulo
  `campaigns`.
- **RT-5 — Cinque lingue (§4).** Il percorso gira nella lingua predefinita; una verifica separata controlla che
  nessuna chiave di traduzione compaia grezza nelle cinque lingue sulle schermate principali, **compreso il
  modulo pubblico di iscrizione**, che è l'unica superficie che vede una persona esterna all'account.
- **RT-6 — Varchi e quota (§6, §7).** Il percorso verifica la catena dei varchi almeno una volta: accesso senza
  abilitazione (`402`) e invio oltre il tetto della metrica `messages_sent`, natura `flow` (`429`), con la
  verifica che nessun invio parziale sia stato generato.
- **RT-7 — Esposizione conversazionale (§12).** Il percorso esercita il ciclo bozza-conferma di
  `crea_bozza_di_campagna` e `programma_invio` chiamando il contratto direttamente, perché il server
  conversazionale non esiste (UC 0061-0063). Va scritto nel registro: quando il server arriverà, quel passo andrà
  rifatto passando da lui.
- **RT-8 — Dati personali (§10).** Nessun dato reale: nomi palesemente finti e indirizzi nel dominio riservato
  alle prove. Il percorso verifica inoltre che l'esportazione dei numeri **non** contenga recapiti e che il
  messaggio inviato con le impostazioni predefinite non contenga né immagine invisibile né collegamenti riscritti
  (storia 0029).
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo. Il percorso verifica che le righe di registro
  prodotte durante l'invio non contengano recapiti.

## 4. Criteri di accettazione

**CA-1 — Il percorso passa**
- **Dato** lo stack locale avviato
- **Quando** si esegue `./run-tests.sh` nell'area dei percorsi di piattaforma
- **Allora** il percorso `[J-CAMPAIGNS]` è verde dall'inizio alla fine

**CA-2 — La promessa negativa è provata**
- **Dato** un iscritto mai confermato, una riga importata in quarantena e un recapito soppresso
- **Quando** si manda una campagna al segmento che li contiene
- **Allora** nessuno dei tre riceve, e il rapporto li mostra fra gli esclusi con il motivo corretto

**CA-3 — La disiscrizione è onorata subito**
- **Dato** un destinatario che apre il collegamento di disiscrizione contenuto nel messaggio
- **Quando** si manda una seconda campagna che lo comprenderebbe
- **Allora** non gli viene generato alcun invio, e il suo recapito risulta nell'elenco di soppressione

**CA-4 — Isolamento provato end-to-end**
- **Dato** i due account del percorso
- **Quando** il secondo apre iscritti, campagne, rapporti e cruscotto
- **Allora** non vede nulla del primo

**CA-5 — Quota provata end-to-end**
- **Dato** l'account del percorso vicino al tetto della metrica degli invii
- **Quando** programma una campagna che lo supererebbe
- **Allora** riceve `429` con il rimedio e **nessun** invio viene generato, nemmeno parzialmente

**CA-6 — Registro coerente**
- **Dato** il registro di copertura aggiornato
- **Quando** si esegue il controllo nell'area degli strumenti
- **Allora** è verde: ogni voce punta a un test che esiste e ogni test etichettato compare nel registro; se una
  voce punta a un test rimosso, la suite è rossa con l'indicazione della voce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` **completo**, non solo le aree toccate;
- [ ] il percorso `[J-CAMPAIGNS]` non usa attese a tempo e non dipende dall'ordine di esecuzione degli altri
      percorsi;
- [ ] prova di **isolamento fra account** compresa nel percorso;
- [ ] **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
      aggiornato con le voci di tutte e 37 le storie, ognuna nella forma «coperta», «rimandata» con motivo e
      storia proprietaria, oppure assente perché senza impatto;
- [ ] **traduzioni**: verifica che nessuna chiave compaia grezza nelle cinque lingue, modulo pubblico compreso;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che i dati del percorso siano inventati e su dominio
      `.test`;
- [ ] **registro delle decisioni** compilato, con annotato che il passo conversazionale andrà rifatto quando il
      server esisterà e che la storia `0024` resta scoperta per una ragione dichiarata;
- [ ] contratto degli **strumenti conversazionali**: esercitato dal percorso;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Tutte le storie da `0001` a `0036` | Il percorso le attraversa |
| Storia `0024` | **Non** implementata: il canale di messaggistica è subordinato alla revisione legale, quindi entra nel registro come `da-coprire`, non come passo del percorso |
| UC 0061-0063 (livello conversazionale) | Non implementati: il passo conversazionale chiama il contratto direttamente, e andrà rifatto |

## 7. Fuori ambito

- le prove di carico sull'invio massivo: fuori perimetro, e comunque non si provano contro un fornitore simulato;
- la suite di terzo livello sul fornitore di pagamento reale: è pre-rilascio e di piattaforma;
- il percorso che attraversa ReachGrove e LeadGrove insieme (una disiscrizione qui che torna là): dipende dal
  contratto degli eventi condivisi, che non esiste ancora
  ([application-description.md](../application-description.md) §11.5).

## 8. Punti aperti

- **Come si simula il ritorno di un rimbalzo permanente e di una segnalazione di posta indesiderata** nello stack
  locale: serve un fornitore di consegna simulato che sappia produrre quei ritorni, altrimenti le storie 0021 e
  0032 restano coperte solo da prove d'integrazione. È una scelta di attrezzatura di prova che lo sviluppatore
  chiude insieme alla scelta del fornitore (§11.2 della descrizione).
