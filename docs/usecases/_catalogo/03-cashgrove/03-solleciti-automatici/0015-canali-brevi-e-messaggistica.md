# 0015 — Canali brevi e messaggistica

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 03 — Solleciti automatici
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa
> voglio poter mandare l'ultimo sollecito con un messaggio breve, perché la posta elettronica il mio cliente non la
> legge
> così da avere una via che arriva davvero, senza dover telefonare io.

**Contesto.** La scheda di catalogo prevede solleciti multicanale (posta elettronica, messaggio breve, messaggistica).
Il canale breve funziona: viene letto quasi sempre e in poche ore. Ma porta con sé tre problemi che questa storia deve
risolvere in modo esplicito, non nascondere: **costa a messaggio** (la piattaforma WhatsApp Business si paga per
messaggio consegnato, con tariffa per Paese e modelli approvati in anticipo), mentre il listino di appgrove ammette
**solo abbonamento ricorrente** e vieta l'addebito a consumo; **espone il contenuto** a chi guarda lo schermo del
telefono, con i limiti di condotta che il Garante pone; e **introduce un fornitore fuori dall'Unione** nel caso della
messaggistica di Meta ([documento capofila](../application-description.md) §2.4, §6, §11).

## 2. Requisiti funzionali

1. **RF-1** — L'account collega il proprio fornitore di messaggi brevi con le **proprie** credenziali: è lui che ha il
   contratto e paga il traffico; appgrove non rivende messaggi.
2. **RF-2** — Un passo di sequenza può usare il canale «messaggio breve» solo se il fornitore è collegato e verificato;
   altrimenti il passo è mostrato come non eseguibile, con la ragione, già in fase di configurazione.
3. **RF-3** — I modelli per il canale breve sono separati da quelli della posta elettronica: hanno un limite di
   lunghezza, non hanno oggetto e il testo non nomina il motivo del contatto oltre lo stretto necessario.
4. **RF-4** — Il messaggio breve non è mai il **primo** passo di una sequenza: si usa dopo che la posta elettronica non
   ha prodotto effetto.
5. **RF-5** — Gli esiti di trasmissione (accettato, respinto, numero non valido) sono trattati come per la posta
   elettronica, e un numero non valido marca il debitore «non raggiungibile per messaggio breve».
6. **RF-6** — La messaggistica istantanea (WhatsApp e simili) **non è implementata in questa storia**: l'app dichiara il
   canale come previsto ma non attivo, con la ragione scritta nell'interfaccia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le credenziali del fornitore appartengono a un solo account; ogni invio filtra
  per `tenant_id` preso dal token verificato o dall'account in elaborazione.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `PUT /api/crediti/v1/impostazioni/canale-breve` e
  `POST /api/crediti/v1/impostazioni/canale-breve/verifica`; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `canale_breve` sullo schema `app_crediti` con `tenant_id`,
  chiave UUID versione 7, colonne di controllo e cancellazione logica; credenziali cifrate e mai leggibili da nessuna
  rotta.
- **RT-4 — Modulo frontend (§3, §5).** Configurazione nella sezione *Impostazioni*, con avviso esplicito che il traffico
  è a carico del cliente e a quali condizioni; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`, compreso l'avviso sul costo a carico del cliente e quello sul canale di messaggistica non
  attivo.
- **RT-6 — Varchi e quota (§6, §7).** Il canale breve **non** introduce una seconda metrica di quota: la metrica resta
  una sola, `crediti_monitorati`. La disponibilità del canale è una **funzionalità del piano** (campo `features` del
  listino), non un limite sorvegliato — è la sola forma compatibile con il vincolo «niente addebito a consumo».
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: `prepara_sollecito` e `invia_sollecito`
  (storia `0029`) accettano il canale come parametro, e per il canale breve la conferma umana resta obbligatoria come
  per la posta elettronica.
- **RT-8 — Dati personali (§10).** Il fornitore dei messaggi brevi è un **responsabile esterno del trattamento**: va
  dichiarato nell'elenco dei fornitori e nell'informativa. Il numero di telefono del debitore è già nel manifesto dalla
  storia `0002`; la tabella `canale_breve` vi si aggiunge. **Preferenza per fornitori con trattamento nell'Unione**,
  coerente con la postura della piattaforma.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «messaggio breve trasmesso», «numero non valido» sono registrati con
  `tenant_id`, `app_id`, `user_id` (o «sistema»), esito e identificativo di correlazione: **mai il numero, mai il
  testo**.
- **RT-10 — Condotta verso il debitore.** Il testo del messaggio breve non contiene parole che rivelino l'inadempienza a
  chi legge la notifica sullo schermo bloccato; vale la stessa finestra oraria della storia `0013`, ristretta se
  possibile, perché un messaggio di sera è più invasivo di una posta elettronica di sera.

## 4. Criteri di accettazione

**CA-1 — Canale non collegato**
- **Dato** un account senza fornitore collegato
- **Quando** si tenta di impostare un passo di sequenza sul canale breve
- **Allora** la configurazione è respinta con un messaggio che spiega cosa manca

**CA-2 — Invio riuscito**
- **Dato** un fornitore collegato e verificato e un invio maturo sul canale breve
- **Quando** il motore lo trasmette
- **Allora** l'esito è «accettato» e la scheda del credito lo mostra accanto agli invii per posta elettronica

**CA-3 — Messaggio breve come primo passo**
- **Dato** una sequenza il cui primo passo è sul canale breve · **Quando** si tenta di salvarla · **Allora** la
  richiesta è respinta, spiegando che il canale breve è un passo di rinforzo

**CA-4 — Numero non valido**
- **Dato** un debitore con numero inesistente · **Quando** la trasmissione fallisce in modo definitivo · **Allora** il
  debitore è marcato «non raggiungibile per messaggio breve» e i passi successivi su quel canale restano in coda con
  l'avviso

**CA-5 — Messaggistica dichiarata non attiva**
- **Dato** la sezione *Impostazioni* · **Quando** l'utente cerca il canale di messaggistica istantanea · **Allora** lo
  trova elencato come previsto ma non attivo, con la ragione scritta in tutte e cinque le lingue

**CA-6 — Isolamento fra account**
- **Dato** due account con fornitori diversi · **Quando** entrambi trasmettono · **Allora** ogni messaggio usa le
  credenziali del proprio account e nessuna è utilizzabile dall'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sui vincoli del canale (lunghezza, non primo passo) e di **integrazione** con fornitore
      **simulato**: nelle prove non parte mai un messaggio vero;
- [ ] prova di **isolamento fra account** su credenziali e invii;
- [ ] **prova end-to-end**: *nessun impatto sul percorso principale* — il percorso `[J-CREDITI]` della storia `0031`
      usa la posta elettronica, che è il canale predefinito; il canale breve resta coperto da prove di integrazione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `canale_breve` e con la dichiarazione del fornitore esterno;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta del fornitore portato dal cliente e sulla
      esclusione della messaggistica istantanea;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta, il canale è un parametro degli strumenti
      esistenti;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` | Il motore di trasmissione e la gestione degli esiti nascono lì; qui si aggiunge un canale |

## 7. Fuori ambito

- **La messaggistica istantanea (WhatsApp Business e simili)**, benché prevista dalla scheda di catalogo. Tre ragioni,
  tutte dichiarate: il costo a messaggio confligge col vincolo «solo abbonamento ricorrente»; i modelli vanno approvati
  in anticipo dal fornitore, il che rende la personalizzazione dei testi molto più rigida di quanto la storia `0012`
  suppone; il trattamento avverrebbe fuori dall'Unione, contro la postura della piattaforma. Se lo sviluppatore vorrà
  attivarla, sarà una storia propria con le sue decisioni.
- Le chiamate vocali automatiche: **escluse deliberatamente**. Il vademecum del Garante cita i solleciti preregistrati
  fra le pratiche invasive.

## 8. Punti aperti

**Chi paga il traffico** è il punto aperto n. 5 del documento capofila §11: la proposta è «il cliente, con le proprie
credenziali», ma se lo sviluppatore volesse comprendere i messaggi nel canone servirebbe un tetto per piano che, non
potendo essere una seconda metrica di quota, resterebbe non sorvegliato. **Decide lo sviluppatore.**
