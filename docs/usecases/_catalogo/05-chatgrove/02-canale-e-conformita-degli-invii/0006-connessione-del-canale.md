# 0006 — Connessione del canale

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 02 — Canale di messaggistica e conformità degli invii
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0003`, `0005`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio che ha già un numero aziendale sulla messaggistica
> voglio collegarlo a ChatGrove seguendo istruzioni che capisco
> così da iniziare a ricevere i messaggi nell'app senza chiamare un tecnico.

**Contesto.** È il passo che decide se il cliente resta o abbandona. L'analisi in rete (§2.5) indica la
complessità di messa in funzione come la barriera principale all'adozione nel segmento micro, e con la scelta
«porta il tuo canale» (§5.1 della descrizione) questo passo è **a carico del cliente**: se lo sbagliamo,
perdiamo il cliente prima ancora che veda l'app. La storia costruisce quindi due cose insieme: la conservazione
sicura delle credenziali e una guida passo passo che dice, in parole non tecniche, cosa fare e dove.

## 2. Requisiti funzionali

1. **RF-1** — Nelle Impostazioni del modulo il negozio inserisce i dati della propria connessione al canale
   (identificativo del numero presso il fornitore e credenziale di accesso) e li salva.
2. **RF-2** — La credenziale è conservata **cifrata** e non è mai restituita in chiaro da nessuna rotta:
   l'interfaccia ne mostra solo le ultime quattro cifre e la data di inserimento.
3. **RF-3** — Al salvataggio il servizio esegue una **verifica di connessione** e mostra l'esito: collegato,
   credenziale rifiutata, numero non abilitato, fornitore irraggiungibile — ognuno con la propria spiegazione
   in parole semplici.
4. **RF-4** — La schermata mostra una guida in cinque passi con ciò che il negozio deve aver fatto presso il
   proprio fornitore, e dichiara apertamente che **i messaggi si pagano al fornitore, non ad appgrove**.
5. **RF-5** — La connessione si può scollegare; scollegandola l'app smette di inviare e di ricevere, ma **non**
   cancella conversazioni, ordini e contatti già presenti.
6. **RF-6** — Lo stato della connessione (collegato / in errore / non collegato, con l'orario dell'ultima
   verifica) è visibile nella pagina d'atterraggio del modulo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La connessione è per account: ogni lettura e scrittura di `channel`
  filtra per `tenant_id` preso dal token verificato. Un utente non può leggere né usare la connessione di un
  altro account, nemmeno forzando l'identificativo nella richiesta.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|PUT|DELETE /api/chat_commerce/v1/channel` e
  `POST /api/chat_commerce/v1/channel/verify`; corpo validato; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V3__canale.sql` sullo schema `app_chat_commerce`: tabella `channel`
  con `tenant_id`, chiave primaria UUID versione 7, riferimento **cifrato** alla credenziale, stato, esito e
  orario dell'ultima verifica, colonne di controllo e cancellazione logica.
- **RT-4 — Ruoli (§6).** Solo i ruoli `owner` e `admin` possono modificare o scollegare la connessione; il
  ruolo `member` la vede in sola lettura. Un `member` che tenta la modifica riceve `403`.
- **RT-5 — Modulo frontend (§3, §4, §5).** Sezione Impostazioni del modulo `chat_commerce`; dati letti con il
  client generato; tutte le stringhe — comprese le quattro spiegazioni d'errore e i cinque passi della guida —
  presenti in `en, it, fr, es, de`; solo token del sistema di design, tema chiaro e scuro.
- **RT-6 — Dati personali (§10).** La connessione contiene dati **dell'azienda**, non di una persona: nessuna
  voce nuova nel manifesto. Ma la connessione è ciò che rende il fornitore del canale un **responsabile del
  trattamento** per conto del cliente: la scheda dell'app deve dirlo e rimandare all'informativa (vedi §8).
- **RT-7 — Registrazione eventi (§14).** Gli eventi `canale collegato`, `verifica fallita`, `canale scollegato`
  sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione. **Mai** la credenziale,
  nemmeno parziale, nemmeno in errore.
- **RT-8 — Segreti.** La credenziale è un segreto: non compare nei registri, non compare nelle risposte, non
  compare nell'esportazione dei dati dell'interessato (non è un dato personale del cliente finale).

## 4. Criteri di accettazione

**CA-1 — Collegamento riuscito**
- **Dato** un titolare con credenziali valide del proprio fornitore
- **Quando** le salva nelle Impostazioni
- **Allora** lo stato passa a «collegato», compare l'orario della verifica e la pagina d'atterraggio smette di
  chiedere di collegare il canale

**CA-2 — Credenziale rifiutata**
- **Dato** un titolare che digita una credenziale errata
- **Quando** salva
- **Allora** riceve un errore che dice **quale** delle quattro cause è (credenziale rifiutata) e cosa fare, la
  connessione resta «non collegata» e nulla di segreto compare nel messaggio

**CA-3 — La credenziale non torna mai indietro**
- **Dato** una connessione salvata
- **Quando** si rilegge la connessione da qualunque rotta
- **Allora** la credenziale non compare: si vedono solo le ultime quattro cifre e la data

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con la propria connessione
- **Quando** un utente di `A` chiede la connessione
- **Allora** vede la propria, anche se forza l'identificativo dell'account `B` nella richiesta

**CA-5 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member` · **Quando** tenta di scollegare il canale · **Allora** riceve `403` e
  la connessione resta invariata

**CA-6 — Scollegamento non distruttivo**
- **Dato** un account con conversazioni e ordini
- **Quando** scollega il canale
- **Allora** non si inviano né ricevono più messaggi, ma conversazioni, contatti e ordini restano leggibili

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla cifratura e sul mascheramento della credenziale, e di **integrazione** sulle
      rotte della connessione, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla risorsa `channel`;
- [ ] **prova end-to-end**: *rimando* alla storia `0029` (il collegamento del canale è il primo passo del
      percorso `[J-CHAT-COMMERCE]`, che si scrive quando la catena è completa);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese le spiegazioni d'errore;
- [ ] **manifesto dei dati**: nessuna voce nuova, ma la scheda dell'app dichiara il fornitore del canale come
      responsabile del trattamento;
- [ ] **registro delle decisioni** compilato, con la scelta «porta il tuo canale» e il perché;
- [ ] contratto degli **strumenti conversazionali**: la connessione **non** è esposta come strumento — è una
      configurazione con un segreto, e i segreti non passano dall'assistente;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0003` | Serve la sezione Impostazioni del modulo |
| `0005` | Serve il canale simulato per provare la verifica senza un fornitore vero |
| Decisione «chi paga il canale» (§5.1 della descrizione) | Se si scegliesse la Via B (canale rivenduto da appgrove) questa storia cambierebbe forma: non ci sarebbe credenziale del cliente |

## 7. Fuori ambito

- la ricezione dei messaggi: storia `0007`;
- la scelta del rivenditore da consigliare al cliente: è direzione di prodotto, non codice;
- l'attivazione del numero presso il fornitore: la fa il cliente, noi la spieghiamo.

## 8. Punti aperti

- **Fornitore extra-europeo.** Collegando il canale, il cliente instrada verso un responsabile del trattamento
  fuori dall'Unione europea, con sub-responsabili negli Stati Uniti. È il punto aperto n. 2 del §11 della
  descrizione dell'applicazione: la decisione — attenuare scegliendo un rivenditore con sede e dati in Europa,
  accettare con motivazione scritta, o rinunciare — **è dello sviluppatore, con la revisione legale**. Questa
  storia non la anticipa: si limita a rendere il fatto visibile nell'interfaccia.
- **Quale forma abbia la credenziale** dipende dal rivenditore scelto: qui si assume una credenziale opaca da
  custodire, che è il caso più generale.
