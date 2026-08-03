# 0027 — Condivisione con il commercialista

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 05 — Conformità e apertura verso l'esterno
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0021`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ogni trimestre riceve la stessa richiesta dal proprio commercialista
> voglio dargli accesso a ciò che gli serve, o mandargli il pacchetto del periodo in un colpo solo
> così da smettere di raccogliere file a mano e da non farmi più chiedere «mi mandi le fatture del trimestre?».

**Contesto.** La ricerca colloca la condivisione con il commercialista fra le prime ragioni per cui una
micro-impresa sceglie un prodotto invece di un altro (§2.4 della descrizione), e i prodotti italiani la offrono ma
in forme vissute come laboriose. È l'ultima storia dell'epica perché usa tutto ciò che le precede: report, forma
canonica, stampa. Ed è la storia in cui il modello a più utenti di BillGrove (§3) mostra il proprio valore: il
commercialista è un utente in sola lettura, non un destinatario di file.

## 2. Requisiti funzionali

1. **RF-1** — L'account può invitare una persona con un ruolo di **sola lettura**, pensato per il commercialista.
2. **RF-2** — Chi ha quel ruolo vede documenti, scadenze, incassi e report, e **non può** creare, modificare,
   emettere, trasmettere né cambiare configurazioni.
3. **RF-3** — In alternativa (o in aggiunta) si può produrre un **pacchetto di periodo** contenente le stampe, la
   forma canonica e un riepilogo tabellare.
4. **RF-4** — Il pacchetto si può generare per un periodo scelto, e la generazione dice esattamente che cosa
   contiene.
5. **RF-5** — Ogni accesso in sola lettura e ogni generazione di pacchetto restano tracciati.
6. **RF-6** — L'invito si può revocare in qualsiasi momento, con effetto immediato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il ruolo di sola lettura vale **per un account solo**: un commercialista
  che segue tre clienti riceve tre inviti e vede tre spazi separati, mai un elenco unito. È il punto in cui il
  desiderio di comodità porterebbe a rompere l'isolamento, e non si fa.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/billing/v1/accountant-packages` per il pacchetto; gli
  inviti passano dal meccanismo di piattaforma, non da rotte dell'app. Errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_billing` per la traccia dei pacchetti prodotti, con
  `tenant_id`, chiave primaria UUID versione 7 e colonne di controllo. Il pacchetto **non si conserva** oltre il
  tempo necessario allo scarico.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Commercialista» con l'invito e la generazione del pacchetto;
  l'interfaccia di chi ha sola lettura non mostra i pulsanti che non può usare, invece di mostrarli e negarli.
  Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`: il commercialista potrebbe non parlare la lingua del titolare.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo della metrica `documenti`. **Il ruolo di sola lettura non
  occupa un posto a pagamento**, perché i posti non si pagano (§5 della descrizione): è una conseguenza diretta
  della scelta di listino ed è uno degli argomenti di vendita dell'app. Va confermato con il listino.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento proprio. Va dichiarato che gli strumenti di
  lettura dell'epica 06 **rispettano il ruolo**: chi ha sola lettura, anche da chat, legge e basta (storia `0030`).
- **RT-8 — Dati personali (§10).** L'accesso di un terzo ai dati dell'account è un trattamento che va dichiarato: il
  commercialista **non** è un responsabile esterno del trattamento per conto di appgrove — è un soggetto scelto dal
  cliente, a cui il cliente dà accesso — ma il manifesto deve dire che esiste questa possibilità di accesso e da
  chi è decisa. **La qualificazione esatta va confermata**: è una classificazione con conseguenze, non una nota.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `invito in sola lettura creato`, `invito revocato` e `pacchetto
  prodotto` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati
  personali.

## 4. Criteri di accettazione

**CA-1 — Accesso in sola lettura**
- **Dato** una persona invitata con ruolo di sola lettura
- **Quando** apre BillGrove
- **Allora** vede documenti, scadenze, incassi e report, e non trova le azioni di creazione, emissione e invio

**CA-2 — Scrittura negata**
- **Dato** lo stesso utente · **Quando** chiama direttamente una rotta di scrittura
- **Allora** riceve `403`

**CA-3 — Pacchetto di periodo**
- **Dato** un trimestre con dodici documenti emessi
- **Quando** si genera il pacchetto
- **Allora** contiene le dodici stampe, le dodici forme canoniche e il riepilogo tabellare, e la generazione
  dichiara che cosa contiene

**CA-4 — Revoca immediata**
- **Dato** un invito revocato · **Quando** la persona tenta di accedere
- **Allora** non vede più l'account

**CA-5 — Nessuna vista unita fra account**
- **Dato** un commercialista invitato da due account diversi
- **Quando** accede
- **Allora** i due spazi restano separati e nessuna vista li unisce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla composizione del pacchetto e di **integrazione** sulla matrice dei ruoli, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** con lo stesso utente invitato da due account: è la prova specifica di
      questa storia;
- [ ] **prova end-to-end**: *coprire ora* — passo «invita il commercialista e verifica che veda senza poter
      scrivere» del percorso `[J-BILLING]`; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con l'accesso del terzo scelto dal cliente;
- [ ] **registro delle decisioni** compilato, con annotata la qualificazione del commercialista;
- [ ] contratto degli **strumenti conversazionali**: nessuno proprio, con la nota sul rispetto del ruolo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0021` | Il pacchetto contiene il riepilogo |
| storia `0024` | Il pacchetto contiene la forma canonica |
| Meccanismo di invito di piattaforma | I ruoli e gli inviti sono di piattaforma, l'app li usa |

## 7. Fuori ambito

- la trasmissione automatica periodica al commercialista: rimandata; usa il meccanismo della storia `0025` e
  introduce un invio ricorrente verso l'esterno, che merita una decisione a parte;
- l'integrazione diretta con i programmi di contabilità degli studi: fuori ambito, si passa dal pacchetto;
- la registrazione in prima nota: non è di BillGrove.

## 8. Punti aperti

La qualificazione del commercialista rispetto al trattamento dei dati — titolare autonomo, responsabile del cliente,
o altro — è una classificazione con conseguenze e **non la decide un agente**: va confermata dallo sviluppatore, se
necessario con la revisione legale. Resta aperto anche se il ruolo di sola lettura debba essere gratuito: è una
conseguenza del listino, che è una fermata di escalation.
