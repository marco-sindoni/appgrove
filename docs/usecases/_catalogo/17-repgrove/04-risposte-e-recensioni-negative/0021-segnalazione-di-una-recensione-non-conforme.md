# 0021 — Segnalazione di una recensione non conforme

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 04 — Risposte e recensioni negative
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha ricevuto una recensione da qualcuno che non è mai stato nel mio locale
> voglio preparare una segnalazione motivata alla piattaforma, con gli elementi che servono
> così da usare la strada che la legge mi dà, invece di quella che mi metterebbe nei guai.

**Contesto.** Prima o poi arriva la recensione ingiusta: di un concorrente, di una persona che ha sbagliato
locale, di qualcuno che si lamenta di un servizio che non offriamo. Il mercato risponde a questo bisogno in due
modi, uno lecito e uno no. Quello che non lo è: modelli di diffida, minacce legali infondate, servizi di
«rimozione recensioni». La regola statunitense sulla soppressione delle recensioni vieta espressamente le
minacce legali infondate per far togliere una recensione negativa (descrizione §2.3, punto 5 — segnalato come
non verificato sul testo ufficiale), e in Europa il quadro non è più permissivo.

Quello che lo è: la **segnalazione motivata alla piattaforma**, che in Italia ha adesso una base precisa. La legge
34/2026 elenca i requisiti che una recensione deve avere (fruizione effettiva, entro trenta giorni, pertinenza,
assenza di incentivi) e stabilisce che dopo **due anni** una recensione perde attualità e può essere rimossa; la
segnalazione si fa alla piattaforma secondo la procedura prevista dal regolamento europeo sui servizi digitali, e
richiede: la motivazione dei requisiti violati, l'indirizzo della recensione contestata, l'identità di chi segnala
e una dichiarazione di buona fede (descrizione §2.3, punto 3).

Questa storia mette in mano al cliente **quella** strada, e nessun'altra.

## 2. Requisiti funzionali

1. **RF-1** — Dalla scheda di una recensione si prepara una segnalazione scegliendo il motivo da un **elenco
   chiuso** che corrisponde ai requisiti di legge e alle regole delle piattaforme: nessuna fruizione effettiva,
   pubblicata oltre il termine, non pertinente al servizio erogato, incentivata, contenuto vietato dalla
   piattaforma (offese, dati personali di terzi, pubblicità), decaduta per decorso dei due anni.
2. **RF-2** — La segnalazione si compone di: motivo, motivazione scritta dal cliente, indirizzo della recensione,
   identità del rappresentante che segnala e dichiarazione di buona fede spuntata esplicitamente. Senza tutti e
   cinque non si può inviare.
3. **RF-3** — L'app **aiuta a motivare, non motiva al posto del cliente**: propone i punti da toccare per il
   motivo scelto e, dove esistono, allega gli elementi che l'app conosce già (per esempio: nessun servizio erogato
   registrato per quella persona nel periodo; recensione pubblicata 45 giorni dopo l'ultima erogazione).
4. **RF-4** — La segnalazione è **irreversibile verso l'esterno**: richiede una conferma esplicita che mostra il
   testo esatto e ricorda che una segnalazione infondata è essa stessa un problema.
5. **RF-5** — L'esito si registra: `inviata`, `accolta`, `respinta`, `senza risposta`, con la data. Le
   segnalazioni respinte restano visibili: servono a non riprovare all'infinito sulla stessa recensione.
6. **RF-6** — Non esiste nessuna funzione che produca diffide, minacce legali o richieste al recensore: l'unico
   destinatario di una segnalazione è la piattaforma.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `segnalazione` filtra per `tenant_id` preso
  dal token verificato; si può segnalare solo una recensione del proprio account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/recensioni/v1/recensioni/{id}/segnalazione`,
  `POST /api/recensioni/v1/segnalazioni/{id}/invia`, `PUT /api/recensioni/v1/segnalazioni/{id}/esito`; errori in
  `application/problem+json` con codici per «elementi mancanti», «già segnalata», «piattaforma non raggiungibile»;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `segnalazione` (storia 0002), una viva per recensione; la motivazione e
  l'identità del segnalante si conservano più a lungo del resto (proposta: 5 anni, descrizione §6) perché possono
  servire in un contenzioso.
- **RT-4 — Modulo frontend (§3, §5).** Scheda della recensione: azione «segnala», modulo con motivo, motivazione
  guidata, elementi automatici proposti, casella della dichiarazione di buona fede, riepilogo di conferma. Solo
  token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Interfaccia in `en, it, fr, es, de`; la **motivazione** si scrive nella lingua
  che la piattaforma si aspetta, e l'app lo dice invece di lasciarlo indovinare.
- **RT-6 — Varchi e quota (§6, §7).** Ruolo `admin` o `owner`: segnalare impegna l'azienda. `402` con abbonamento
  `canceled`. Nessun consumo di quota.
- **RT-7 — Esposizione conversazionale (§12).** È lo strumento `segnala_recensione` (storia 0028), marcato
  **scrittura irreversibile**: produce una **bozza** di segnalazione e richiede conferma umana obbligatoria. Un
  assistente che segnalasse da solo esporrebbe il cliente a una segnalazione infondata fatta a suo nome.
- **RT-8 — Dati personali (§10).** **Voce nuova nel manifesto**: `segnalazione.identita` (rappresentante che
  segnala) e `segnalazione.motivazione` (testo libero, che parla necessariamente dell'autore della recensione).
  Dichiarate in italiano e inglese, con la conservazione più lunga motivata. La segnalazione **esce verso la
  piattaforma**: è una destinazione da dichiarare.
- **RT-9 — Registrazione eventi (§14).** `segnalazione creata` con il motivo, `segnalazione inviata`,
  `esito registrato`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza il testo della
  motivazione.

## 4. Criteri di accettazione

**CA-1 — Segnalazione completa**
- **Dato** una recensione da un autore per cui non risulta nessun servizio erogato
- **Quando** il titolare sceglie il motivo «nessuna fruizione effettiva», scrive la motivazione, spunta la
  dichiarazione e conferma
- **Allora** la segnalazione risulta `inviata` con tutti gli elementi, e la scheda della recensione lo mostra

**CA-2 — Elementi mancanti**
- **Dato** una segnalazione senza dichiarazione di buona fede
- **Quando** si tenta di inviarla
- **Allora** l'invio è rifiutato con l'indicazione di cosa manca

**CA-3 — Elementi automatici**
- **Dato** una recensione pubblicata 45 giorni dopo l'ultimo servizio erogato a quel recapito
- **Quando** si sceglie il motivo «pubblicata oltre il termine»
- **Allora** l'app propone quell'elemento come materiale della motivazione, che il titolare può usare o togliere

**CA-4 — Nessuna seconda segnalazione**
- **Dato** una recensione con una segnalazione già inviata e non ancora conclusa
- **Quando** si tenta di segnalarla di nuovo
- **Allora** l'operazione è rifiutata, con il rimando alla segnalazione in corso

**CA-5 — Isolamento fra account**
- **Dato** due account con recensioni
- **Quando** un utente di `A` tenta di segnalare una recensione di `B`
- **Allora** riceve `404` e nessuna segnalazione viene creata

**CA-6 — Nessuna via alternativa**
- **Dato** la scheda della recensione e tutta l'app
- **Quando** si cerca un modello di diffida, un messaggio verso il recensore o un servizio di rimozione
- **Allora** non esiste

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla completezza degli elementi obbligatori e sul calcolo degli elementi automatici; di
      **integrazione** sulle rotte con piattaforma **simulata**;
- [ ] prova di **isolamento fra account** sulle segnalazioni;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-RECENSIONI]` copre invito, raccolta e risposta; la
      segnalazione è coperta a livello di integrazione perché richiede uno stato costruito ad arte. Voce motivata
      nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `segnalazione.identita` e `segnalazione.motivazione`, conservazione
      motivata, destinazione esterna dichiarata;
- [ ] **registro delle decisioni** compilato, con l'elenco chiuso dei motivi e la fonte normativa di ciascuno, e
      con la dichiarazione che il testo di legge non è stato letto in originale;
- [ ] contratto degli **strumenti conversazionali**: `segnala_recensione`, scrittura irreversibile, conferma
      obbligatoria.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | serve la scheda della recensione |
| storia `0011` | gli elementi automatici si ricavano dai servizi erogati |
| **lettura del testo della legge 34/2026** | l'elenco dei motivi e i termini vengono da fonti secondarie (descrizione §2.7) |

## 7. Fuori ambito

- l'invio automatico della segnalazione **dentro** i canali della piattaforma quando questi non lo permettono: in
  quel caso l'app produce il testo e il cliente lo incolla dove serve, e lo dice chiaramente;
- il ruolo dei «segnalatori attendibili» previsto dall'articolo 21 (associazioni di categoria): non è una funzione
  di un'app, è un rapporto fra il cliente e la sua associazione.

## 8. Punti aperti

- **Se le piattaforme accettino segnalazioni per via programmatica** e con quali requisiti: non l'ho verificato.
  Se non le accettano, il RF-3 e il RF-4 restano validi ma la segnalazione diventa un testo da copiare, e va detto
  al cliente senza girarci intorno.
- **La decadenza dei due anni** viene da fonti secondarie e va verificata sul testo di legge prima di offrirla
  come motivo di segnalazione: proporre un motivo inesistente farebbe fare al cliente una figura peggiore del non
  segnalare.
- **Ambito territoriale**: la legge italiana vale per strutture in Italia e per quattro settori. Per una sede
  francese o per un'officina italiana i motivi disponibili sono meno. L'app deve mostrarne solo quelli
  applicabili, ed è un dettaglio che è facile sbagliare.
</content>
