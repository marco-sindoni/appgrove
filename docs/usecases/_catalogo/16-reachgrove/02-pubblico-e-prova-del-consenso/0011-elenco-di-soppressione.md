# 0011 — Elenco di soppressione

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 02 — Pubblico e prova del consenso
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che non vuole scrivere di nuovo a chi gli ha detto di smettere
> voglio un elenco di recapiti che non si possono più usare, che valga sopra ogni altra cosa
> così da non poter riaprire per sbaglio una porta che qualcuno ha chiuso.

**Contesto.** Ci sono tre modi in cui un recapito diventa inutilizzabile per sempre: la persona si è disiscritta
(storia 0012), l'indirizzo non esiste più e ha prodotto un rimbalzo permanente, oppure il destinatario ha
segnalato il messaggio come posta indesiderata (storia 0021). Tutti e tre portano allo stesso posto, e quel posto
deve essere **fuori dalla portata del cliente**: il difetto tipico dei prodotti della categoria è che l'elenco dei
disiscritti si può svuotare con un pulsante, e prima o poi qualcuno lo preme. Qui la soppressione vince su tutto —
se il recapito è nell'elenco non nasce nessun invio, qualunque cosa dica il consenso
([application-description.md](../application-description.md) §4) — ed è la ragione per cui questa storia sta nelle
fondamenta dell'epica e non fra i rapporti.

## 2. Requisiti funzionali

1. **RF-1** — Un recapito entra nell'elenco di soppressione per uno di quattro motivi: `disiscrizione`,
   `rimbalzo permanente`, `segnalazione di posta indesiderata`, `richiesta diretta dell'interessato`. Ogni voce
   porta il motivo, il momento e l'origine.
2. **RF-2** — La soppressione è **per recapito e per account**, non per iscritto: sopravvive alla cancellazione
   dell'iscritto, alla sua ricreazione e a qualunque importazione successiva.
3. **RF-3** — Nessuna funzione dell'app crea una consegna verso un recapito soppresso: né una campagna, né
   un'automazione, né un messaggio di conferma, né una prova d'invio.
4. **RF-4** — Il cliente **non può rimuovere** una voce dall'elenco. Può aggiungerne (per esempio quando una
   persona gli chiede a voce di non essere più contattata) ma non toglierne.
5. **RF-5** — L'unica via di uscita è una **nuova iscrizione volontaria con doppia conferma** compiuta dalla
   persona stessa (storia 0008): la conferma rimuove la soppressione dovuta a disiscrizione, lasciandone traccia
   nello storico, e **non** rimuove quella dovuta a segnalazione di posta indesiderata.
6. **RF-6** — L'elenco si consulta e si esporta, con ricerca per recapito e filtro per motivo, e mostra il numero
   di voci per motivo: è la prima diagnosi quando il cliente chiede «perché a lui non arriva niente».
7. **RF-7** — Ogni scheda di iscritto il cui recapito è soppresso lo dice in evidenza, con il motivo e il momento,
   e nasconde le azioni che presupporrebbero un invio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La soppressione è **dell'account**: ogni lettura e scrittura filtra per
  `tenant_id` dal token verificato. Una soppressione dell'account `A` non ha alcun effetto su `B` e non è
  osservabile da `B`; il contrario significherebbe rivelare a un cliente i disiscritti di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/campaigns/v1/suppressions`. **Nessun**
  `DELETE` e **nessun** `PATCH`, assenti dalla definizione delle interfacce. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `suppression` sullo schema `app_campaigns` con `tenant_id`, chiave UUID
  versione 7, colonne di controllo; indice univoco su (`tenant_id`, impronta del recapito normalizzato) e
  interrogazione a costo costante, perché questo controllo si fa **una volta per destinatario** su ogni invio. La
  cancellazione logica si usa **solo** per l'esercizio dei diritti dell'interessato.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Recapiti soppressi» dentro le impostazioni di invio: elenco,
  ricerca, filtro per motivo, aggiunta manuale, esportazione. Nessun pulsante di rimozione, e una riga che spiega
  perché non c'è. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, compresi i quattro motivi, in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di `messages_sent`. La consultazione richiede un ruolo
  qualsiasi fra `owner`, `admin`, `member`; l'aggiunta manuale è tracciata con l'utente che l'ha compiuta. Con
  abbonamento `canceled` la sezione risponde `402`, ma **il controllo in fase di invio resta comunque attivo**:
  non è una funzione, è un presidio.
- **RT-7 — Esposizione conversazionale (§12).** `stato_iscritto` (storia 0034, **lettura**) dice se il recapito è
  soppresso e per quale motivo. `disiscrivi` (storia 0035, **scrittura irreversibile**) aggiunge una voce con
  motivo `richiesta diretta dell'interessato` e richiede conferma umana esplicita. Nessuno strumento rimuove una
  soppressione, perché nessuna rotta lo fa.
- **RT-8 — Dati personali (§10).** Voce `suppression.contact` del manifesto in italiano e inglese: contatto di ex
  iscritti o segnalanti, finalità «impedire per sempre nuovi invii allo stesso recapito», base giuridica «obbligo
  di onorare l'opposizione», conservazione **permanente**. Qui sta il caso limite del catalogo: **cancellare la
  soppressione riaprirebbe la porta agli invii**, cioè produrrebbe l'effetto opposto a quello che l'interessato
  chiede. *Proposta*: conservare la sola impronta crittografica non reversibile del recapito, che serve a bloccare
  e non a contattare, cancellando il recapito in chiaro; e dichiararlo nell'informativa. **Da validare** (§6 e
  §11.6 della descrizione): questa storia implementa la proposta ma la marca come tale.
- **RT-9 — Registrazione eventi (§14).** «Recapito soppresso» e «invio impedito da soppressione» con `tenant_id`,
  `app_id`, `user_id` quando c'è, motivo e identificativo di correlazione; **mai** il recapito.

## 4. Criteri di accettazione

**CA-1 — La soppressione vince sul consenso**
- **Dato** un iscritto con un consenso valido e il suo recapito nell'elenco di soppressione
- **Quando** si prepara e si spedisce una campagna su un segmento che lo comprende
- **Allora** nessuna consegna viene creata verso di lui, il rapporto lo conta fra gli esclusi con il motivo
  «soppresso», e la sua scheda mostra lo stato `soppresso`

**CA-2 — Non si può togliere**
- **Dato** una voce nell'elenco
- **Quando** l'utente tenta di eliminarla dall'interfaccia o con una richiesta diretta
- **Allora** l'operazione non esiste (`405`) e l'interfaccia spiega che l'unica via è una nuova iscrizione
  volontaria della persona

**CA-3 — La reiscrizione volontaria riapre, la segnalazione no**
- **Dato** due recapiti soppressi, uno per `disiscrizione` e uno per `segnalazione di posta indesiderata`
- **Quando** entrambi si iscrivono di nuovo dal modulo pubblico e confermano
- **Allora** il primo torna contattabile con la traccia della soppressione precedente nello storico; il secondo
  resta soppresso e la conferma non lo riabilita

**CA-4 — Sopravvive alla cancellazione dell'iscritto**
- **Dato** un recapito soppresso e il suo iscritto eliminato dal cliente
- **Quando** lo stesso recapito viene reinserito con un'importazione
- **Allora** la riga è scartata per soppressione e nessun iscritto viene creato

**CA-5 — Isolamento fra account**
- **Dato** un recapito soppresso nell'account `A`
- **Quando** l'account `B` cerca quel recapito nel proprio elenco e gli invia una campagna
- **Allora** in `B` non risulta soppresso e l'invio parte: la soppressione è dell'account, e `B` non apprende
  nulla di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla normalizzazione del recapito e sull'impronta crittografica, e sulla precedenza
      della soppressione nel calcolo dello stato; prove di **integrazione** sull'assenza delle rotte di rimozione;
- [ ] prova di **isolamento fra account** sulle soppressioni, compreso il caso dello stesso recapito in due
      account;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) verifica che dopo una
      disiscrizione il recapito sia soppresso e che una campagna successiva non lo raggiunga; voce aggiunta al
      registro di copertura;
- [ ] **traduzioni** in tutte e cinque le lingue, compresi i motivi;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `suppression`, con la nota sul conflitto fra
      diritto alla cancellazione e diritto a non essere più contattati, e la proposta dell'impronta crittografica
      marcata come **da validare**;
- [ ] **registro delle decisioni** compilato, con annotato perché l'elenco non si può svuotare e perché la
      segnalazione di posta indesiderata non si riapre;
- [ ] contratto degli **strumenti conversazionali**: lettura in `stato_iscritto`, scrittura irreversibile in
      `disiscrivi` con conferma, nessuno strumento di rimozione;
- [ ] controllo automatico di **accessibilità** verde sulla sezione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | Lo stato dell'iscritto deriva anche da qui, con precedenza su tutto |
| Storia `0008` | La sola via di uscita da una soppressione per disiscrizione è la nuova conferma volontaria |
| Storia `0021` (epica 04) | Rimbalzi permanenti e segnalazioni alimentano l'elenco: fino ad allora entrano solo disiscrizioni e richieste dirette |

## 7. Fuori ambito

- il meccanismo di disiscrizione dal messaggio: è la storia 0012;
- la raccolta dei ritorni del fornitore di consegna: è la storia 0021;
- un elenco di soppressione **condiviso fra account**: deliberatamente escluso. Sarebbe un archivio di persone che
  non vogliono essere contattate, costruito da noi sopra i dati di clienti diversi: un trattamento nuovo, con
  finalità propria, che nessuno ci ha autorizzato a fare.

## 8. Punti aperti

- **Come conservare la soppressione rispettando il diritto alla cancellazione** — è il punto aperto (b) del §11.6
  della descrizione. La proposta dell'impronta crittografica è implementata ma **non decisa**: chiude lo
  sviluppatore con la revisione legale, e va scritta nell'informativa prima del rilascio.
- **Se la segnalazione di posta indesiderata debba essere irreversibile davvero.** Proposta: sì, perché è il
  segnale che costa di più alla recapitabilità di tutti gli account (§2.3 punto 5). Un cliente che insiste è
  esattamente il cliente da cui difendersi. Chiude lo sviluppatore.
