# 0012 — Perimetro senza dati sanitari

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 03 — Scheda tecnica e storia del cliente
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare del salone e come chi risponde della piattaforma
> voglio che il programma renda difficile scrivere dentro informazioni sulla salute delle mie clienti, e lo dica
> apertamente invece di lasciarlo capire
> così da non ritrovarmi a custodire dati che non so proteggere e che non ho titolo per trattare.

**Contesto.** È la storia che tiene l'applicazione dentro il suo perimetro, ed è tanto importante quanto è
scomoda. Il §6 della [descrizione](../application-description.md) lo argomenta per esteso: la legge 4 gennaio 1990
n. 1, all'articolo 1 comma 3, esclude dall'attività di estetista «le prestazioni dirette in linea specifica ed
esclusiva a finalità di carattere terapeutico», e questo tiene il **servizio** fuori dal sanitario. Ma la pratica
del mestiere raccoglie allergie, test cutanei, farmaci e gravidanze — dati dell'articolo 9 — e nessuna codifica
furba li fa sparire. La via raccomandata è: **non tenerli**, dirlo, e presidiare il punto da cui entrerebbero
comunque, cioè i campi a testo libero.

⚠️ **Questa storia implementa una decisione dello sviluppatore, non la prende.** Se la decisione fosse un'altra —
per esempio un modulo «sicurezza del trattamento» attivabile per account, con consenso esplicito e valutazione
d'impatto — questa storia va riscritta da capo, e con essa la storia `0010`.

## 2. Requisiti funzionali

1. **RF-1** — Il modello dati e la definizione delle interfacce **non contengono** campi per: patologie, terapie,
   farmaci, gravidanza e allattamento, diagnosi, allergie, intolleranze, esiti di test cutanei. La loro assenza è
   verificata da una **prova automatica** che fallisce se qualcuno li aggiunge.
2. **RF-2** — Ogni campo a testo libero che riguarda un cliente — nota interna sul cliente, note tecniche della
   scheda, nota di riga sul conto — porta un **avviso a schermo** che chiede di non scriverci informazioni sulla
   salute, visibile accanto al campo e non nascosto in un aiuto contestuale.
3. **RF-3** — Le condizioni d'uso e l'informativa del prodotto dichiarano che SalonGrove è per attività estetiche
   **non terapeutiche**, e che medicina estetica, dermatologia, fisioterapia e podologia sanitaria sono fuori
   perimetro.
4. **RF-4** — Il salone può registrare che una cliente ha firmato un **consenso informato** con la sua data e la
   sua scadenza: si registra **il fatto**, mai il contenuto e mai il motivo.
5. **RF-5** — Il salone può marcare un servizio come «non erogabile a questa cliente» **senza indicarne la
   ragione**: è un'informazione operativa, e il programma non chiede né conserva il perché.
6. **RF-6** — Il primo accesso alla scheda tecnica mostra una volta, e una volta sola, la spiegazione del perimetro
   in parole semplici: che cosa questo programma tiene e che cosa deve restare altrove.

## 3. Requisiti tecnici

- **RT-1 — Dati personali (§10).** ⚠️ **È il cuore della storia.** Il manifesto dichiara in italiano e inglese
  **anche ciò che non si tratta**, con una nota che spiega perché: è la sola forma di documentazione che
  sopravvive al ricambio di chi scrive il codice. Il campo «non erogabile» si dichiara come **preferenza
  operativa**, con base «legittimo interesse del salone all'esecuzione corretta del servizio», e **non** come dato
  sanitario — con l'avvertenza onesta che, se il salone lo usasse solo per le controindicazioni, la
  classificazione andrebbe rivista. Il campo «consenso informato firmato il» si dichiara come dato amministrativo.
- **RT-2 — Prova strutturale.** Un controllo automatico esamina il modello dati e la definizione delle interfacce
  e **fa fallire la compilazione o la suite** se compare un campo il cui nome o la cui annotazione ricadono
  nell'elenco vietato. È lo stesso principio per cui un campo annotato e non dichiarato nel manifesto fa fallire
  la compilazione: un divieto che non è meccanico non è un divieto.
- **RT-3 — Isolamento fra account (§1).** Consenso registrato e marcature «non erogabile» filtrano per
  `tenant_id` dal token verificato.
- **RT-4 — Persistenza (§8).** I due campi introdotti stanno sulle tabelle esistenti (cliente, servizio-cliente)
  con `tenant_id`, colonne di controllo e cancellazione logica.
- **RT-5 — Modulo frontend (§3, §5).** L'avviso accanto ai campi liberi è un elemento del sistema di design, non
  un testo grigio: dev'essere leggibile in tema chiaro e scuro, e non dev'essere chiudibile per sempre.
- **RT-6 — Cinque lingue (§4).** Gli avvisi, la spiegazione del perimetro e le etichette in `en, it, fr, es, de`.
  ⚠️ **Sono i testi più delicati dell'applicazione**: vanno rivisti da chi conosce la materia in ciascuna lingua,
  non tradotti alla lettera.
- **RT-7 — Esposizione conversazionale (§12).** Gli strumenti di lettura **non restituiscono mai** i campi a testo
  libero né la marcatura «non erogabile»: sono i due punti da cui un dato particolare potrebbe uscire senza che
  nessuno l'abbia chiesto. Vincolo dichiarato nel contratto degli strumenti (storia `0028`).
- **RT-8 — Registrazione eventi (§14).** `consenso registrato`, `servizio marcato non erogabile` con `tenant_id`,
  `app_id`, `user_id` e correlazione — **mai il contenuto di nessuno dei due**.

## 4. Criteri di accettazione

**CA-1 — I campi vietati non entrano**
- **Dato** la suite di collaudo
- **Quando** qualcuno aggiunge al modello un campo `allergie` o `patologie`
- **Allora** la suite diventa rossa con un messaggio che spiega il perimetro e rimanda a questa storia

**CA-2 — L'avviso è dove serve**
- **Dato** i tre campi a testo libero dell'applicazione
- **Quando** si apre ciascuno di essi
- **Allora** accanto c'è l'avviso di non scrivere informazioni sulla salute, in tutte e cinque le lingue

**CA-3 — Il consenso è un fatto, non un contenuto**
- **Dato** una cliente con consenso informato registrato
- **Quando** si apre la sua scheda
- **Allora** si vede la data e la scadenza, e non c'è nessun campo in cui scrivere a che cosa il consenso si
  riferisca

**CA-4 — Il servizio non erogabile non chiede il perché**
- **Dato** il modulo con cui si marca un servizio come non erogabile per una cliente
- **Quando** lo si compila
- **Allora** non c'è nessun campo per la ragione, e il programma non ne chiede uno

**CA-5 — Gli strumenti conversazionali non espongono i campi liberi**
- **Dato** lo strumento di lettura della scheda tecnica
- **Quando** lo si invoca per una cliente con note tecniche compilate
- **Allora** la risposta contiene la formula e non contiene le note

**CA-6 — Isolamento fra account**
- **Dato** due account
- **Quando** un utente del primo tenta di leggere consensi o marcature dell'altro
- **Allora** non ottiene nulla

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite intera: la storia tocca `compliance` e gli strumenti);
- [ ] prova **strutturale** che vieta i campi sanitari, verificata anche in negativo (si aggiunge un campo vietato
      e la suite diventa rossa);
- [ ] prova di **isolamento fra account** sui due campi introdotti;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-SALONGROVE]` verifica la presenza dell'avviso accanto
      al campo delle note tecniche; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** in tutte e cinque le lingue, **marcate come da rivedere** finché non le controlla qualcuno che
      conosce la materia in quella lingua;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, **compresa la nota su ciò che non si tratta e
      perché**;
- [ ] **registro delle decisioni**: la via scelta fra le tre del §6 della descrizione, con la motivazione e le
      conseguenze accettate — è la voce più importante di tutta l'applicazione;
- [ ] documentazione aggiornata: condizioni d'uso e informativa;
- [ ] avvio locale invariato; nessun dato di prova contiene informazioni sulla salute, nemmeno per finta.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | l'avviso vive accanto ai campi che quella storia introduce |
| **decisione sul punto 4 dei rischi** (§11 della descrizione) | questa storia la implementa; non la sostituisce |
| revisione legale | il testo delle condizioni d'uso e dell'informativa non lo scrive un agente |

## 7. Fuori ambito

- il modulo «sicurezza del trattamento» con consenso esplicito e valutazione d'impatto: è l'evoluzione (b) del §6,
  **non** è questa stesura, e va progettata a parte se lo sviluppatore la vuole;
- la rilevazione automatica di contenuti sensibili nei campi liberi: è un tema trasversale di piattaforma, non di
  questa app;
- le fotografie, che hanno un profilo di rischio proprio: storia `0013`.

## 8. Punti aperti

**L'avviso non basta e va detto.** Un salone che ha bisogno di ricordarsi che una cliente non tollera un prodotto
lo scriverà da qualche parte: se il campo apposito non c'è, lo scrive nella nota libera, cioè esattamente nel posto
peggiore. Questa storia riduce la probabilità, **non la elimina**. La sola soluzione che la eliminerebbe è il
modulo governato dell'evoluzione (b), con consenso esplicito e tutele proprie — e costa quanto costa. Chi decide di
fermarsi qui deve sapere che si sta fermando a una mitigazione, non a un rimedio.

**L'ancora giuridica è italiana.** L'articolo 1 comma 3 della legge 1/1990 è ciò che tiene il *servizio* fuori dal
perimetro sanitario. Non ho verificato che Francia, Spagna e Germania traccino lo stesso confine (§2.7 della
descrizione): fuori dall'Italia il perimetro va difeso con le condizioni d'uso invece che con la legge, e questo va
verificato prima di vendere là.
