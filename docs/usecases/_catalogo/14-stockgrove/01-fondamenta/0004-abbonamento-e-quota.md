# 0004 — Abbonamento e quota

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'impresa abbonata
> voglio che il mio piano abbia un limite chiaro, che io sappia sempre quanto ne ho usato e che il limite non mi
> impedisca di registrare quello che succede in magazzino
> così da poter scegliere il piano giusto senza il timore che un giorno il programma mi lasci con un saldo falso.

**Contesto.** Il servizio e il modulo esistono, l'app è aperta a chiunque. Questa storia chiude la catena dei
cinque varchi e aggancia la metrica di quota `articoli_gestiti`, di natura **a giacenza**: è un tetto su ciò che
esiste ora, non un consumo che si azzera ogni mese. Un articolo censito l'anno scorso occupa un posto anche oggi, e
archiviarlo lo libera davvero; se lo trattassimo come consumo mensile un account potrebbe accumulare diecimila
articoli dentro un piano da cinquecento e il tetto non significherebbe nulla.

**Il punto di sostanza: il blocco non tocca mai i movimenti.** Su una metrica a giacenza il rifiuto `429` colpisce
la creazione di un articolo nuovo. **Non deve mai colpire la registrazione di un movimento su un articolo che
esiste già.** Impedire a un'impresa di registrare uno scarico perché ha finito il piano non le farebbe risparmiare
nulla: la merce esce lo stesso, il fatto avviene lo stesso, e l'unica conseguenza sarebbe che il registro smette di
descrivere la realtà. Quando quel cliente tornasse a pagare, ritroverebbe un saldo falso e nessun modo di sapere da
dove viene la differenza — cioè esattamente il problema per cui aveva comprato l'app. Un contatore commerciale non
può essere la causa della corruzione del dato che vendiamo. È la scelta proposta nella descrizione
dell'applicazione (§5, punto delicato del listino) e va confermata dallo sviluppatore insieme ai prezzi.

## 2. Requisiti funzionali

1. **RF-1** — L'accesso alle funzioni dell'app attraversa i cinque varchi nell'ordine: token non valido → `401`;
   app spenta dalla piattaforma → `403`; account non abilitato → `402`; ruolo insufficiente → `403`; quota
   esaurita → `429`.
2. **RF-2** — La metrica `articoli_gestiti` conta gli articoli in stato **attivo** e non cancellati dell'account;
   la creazione di un articolo, o la riattivazione di un articolo archiviato, prenota una unità **prima** di
   scrivere e riceve `429` se il tetto è raggiunto.
3. **RF-3** — **Nessuna operazione sui movimenti consuma o verifica la quota**: carichi, scarichi, trasferimenti,
   rettifiche, storni e chiusure d'inventario passano sempre, anche con la quota esaurita.
4. **RF-4** — Archiviare un articolo **libera un posto** immediatamente; il conteggio pubblicato all'interfaccia si
   aggiorna nella stessa risposta.
5. **RF-5** — Il modulo mostra in ogni momento quanti articoli attivi ci sono sul tetto del piano, e quando la
   creazione viene respinta spiega i due rimedi possibili — archiviare un articolo oppure passare di piano — invece
   di mostrare un errore tecnico.
6. **RF-6** — Il passaggio a un piano inferiore è **bloccato** finché gli articoli attivi superano il tetto del
   piano di destinazione, e l'interfaccia dice quanti articoli vanno archiviati perché diventi possibile.
7. **RF-7** — Gli stati di abbonamento `trialing`, `active` e `past_due` danno accesso; `paused` e `canceled` no e
   producono `402`. L'esportazione e la cancellazione dei dati restano accessibili **in ogni caso**, anche con
   l'app disabilitata o l'abbonamento scaduto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il conteggio della metrica si calcola sempre con il filtro
  `tenant_id = :tid` preso dal token verificato: un account non vede né influenza il contatore di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Il rifiuto per quota è `429` in `application/problem+json`, con il
  tetto, il valore attuale e il rimedio nel corpo; il rifiuto per abbonamento è `402` con lo stesso formato.
  Definizione OpenAPI aggiornata con i codici di errore nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: il conteggio si ricava dalla tabella `articolo` con un indice
  che lo renda economico. La **proiezione locale delle abilitazioni**, alimentata a eventi, è quella comune fornita
  dalle parti condivise: **mai** una chiamata di rete sincrona all'app centrale sul percorso caldo.
- **RT-4 — Modulo frontend (§3, §5).** L'indicatore di quota e i messaggi di rifiuto vivono nel modulo `magazzino`;
  solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I messaggi di quota esaurita, di abbonamento non attivo e di blocco del passaggio
  di piano passano dallo spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`. Sono fra i testi più
  delicati dell'app: un cliente li legge nel momento in cui è più contrariato.
- **RT-6 — Varchi e quota (§6, §7).** Prima di creare o riattivare un articolo il servizio prenota una unità della
  metrica `articoli_gestiti` (natura `stock`); a quota esaurita risponde `429` con l'indicazione del rimedio. Con
  abbonamento in `past_due` la funzione resta accessibile; con `canceled` risponde `402`. **La storia non fissa
  prezzi**: consuma il tetto pubblicato dall'abilitazione, che arriva dal listino come codice.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo, ma una regola che vale per tutti quelli
  delle storie `0034` e `0035`: le chiamate dell'assistente attraversano **gli stessi** varchi delle chiamate
  dell'interfaccia, senza scorciatoie. Applicare quota e abilitazione alle chiamate conversazionali è di
  piattaforma (UC 0064) e non è ancora implementato.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** Vale però il vincolo del §13 dei principi: i
  diritti dell'interessato (esportazione e cancellazione) non passano dal varco dell'abilitazione e restano
  raggiungibili anche quando l'app è spenta per quell'account.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `articolo respinto per quota`, `accesso negato per abbonamento`
  e `passaggio di piano bloccato` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali. **Nessun indicatore per persona**: si registra il fatto, non chi lo ha
  provocato più spesso.

## 4. Criteri di accettazione

**CA-1 — Tetto raggiunto sulla creazione**
- **Dato** un account con il numero di articoli attivi pari al tetto del suo piano
- **Quando** un utente prova a creare un altro articolo
- **Allora** riceve `429` in `application/problem+json` con tetto, valore attuale e i due rimedi, e **nessun
  articolo viene creato**

**CA-2 — I movimenti passano anche a quota esaurita**
- **Dato** lo stesso account con la quota esaurita e un articolo esistente con giacenza 10
- **Quando** registra uno scarico di 3 pezzi
- **Allora** il movimento viene registrato, la giacenza diventa 7 e **nessun `429` viene restituito**

**CA-3 — Archiviare libera un posto**
- **Dato** un account a tetto raggiunto · **Quando** archivia un articolo e subito dopo ne crea uno nuovo
- **Allora** la creazione riesce e il conteggio pubblicato resta pari al tetto

**CA-4 — Abbonamento non più attivo**
- **Dato** un account con abbonamento in stato `canceled`
- **Quando** un utente apre l'elenco degli articoli
- **Allora** riceve `402` con l'indicazione di come riattivare, mentre la richiesta di **esportazione dei propri
  dati** continua a rispondere `200`

**CA-5 — Passaggio a un piano inferiore bloccato**
- **Dato** un account con 700 articoli attivi che chiede di passare a un piano con tetto 500
- **Quando** conferma il cambio
- **Allora** il cambio è rifiutato con un messaggio che dice quanti articoli vanno archiviati prima, e
  l'abbonamento resta sul piano attuale

**CA-6 — Isolamento del contatore fra account**
- **Dato** due account `A` e `B` sullo stesso piano, con `B` a tetto raggiunto
- **Quando** un utente di `A` crea un articolo
- **Allora** la creazione riesce, perché il contatore di `B` non lo riguarda

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della metrica e di **integrazione** sui varchi, con database effimero e
      migrazioni vere; prova esplicita che un movimento **non** consuma quota;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** (`owner`, `admin`, `member`) sulle rotte
      introdotte, con il caso dell'abilitazione negata;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` nasce con la storia `0036`, che vi include il
      passo del rifiuto per quota; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** dei messaggi di quota e abbonamento presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica, la storia non tratta dati personali;
- [ ] **registro delle decisioni** compilato, con la scelta «il blocco non tocca i movimenti» e il suo motivo;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova, ma la regola che le chiamate
      dell'assistente attraversano gli stessi varchi è scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | Il listino come codice `pricing/magazzino.yaml` deve esistere e essere registrato |
| `0002` | La metrica conta gli articoli attivi: senza la tabella non c'è nulla da contare |
| `0003` | L'indicatore di quota e i messaggi di rifiuto vivono nel modulo |
| conferma dello sviluppatore su piani, tetti e prova gratuita | È una **fermata di escalation**: nessun agente fissa prezzi e limiti (descrizione dell'applicazione, §5) |

## 7. Fuori ambito

- **I prezzi e i tetti dei piani**: si propongono nella descrizione dell'applicazione e si confermano fuori da
  questa storia; qui si consuma il tetto pubblicato, qualunque sia.
- L'acquisto e la disdetta dell'abbonamento dal catalogo app: sono di piattaforma, non di StockGrove.
- Il numero di depositi come caratteristica del piano: proposta aperta (descrizione, §5 e §11 punto 1); finché non
  è chiarito come le caratteristiche del listino vengano applicate a runtime, i depositi non sono limitati.
- La deroga temporanea al tetto per chi importa l'anagrafica il primo mese:
  [estensioni-admin.md](../estensioni-admin.md).

## 8. Punti aperti

- **La scelta «il blocco non tocca i movimenti» è una decisione di prodotto**, non un dettaglio tecnico: va
  confermata dallo sviluppatore insieme al listino. Se venisse ribaltata, l'app cambierebbe natura e questa storia
  andrebbe riscritta, non corretta.
- **Prova gratuita di 14 giorni su un'app che ha già un piano gratuito**: in parte ridondante. La proposta la tiene
  perché il valore si vede solo dopo aver caricato l'anagrafica vera e cinquanta articoli non bastano; se lo
  sviluppatore la disattiva, il piano gratuito va allargato di conseguenza.
- **Caratteristiche del piano applicate a runtime**: non è stato verificato nel repository se la mappa delle
  caratteristiche del listino sia effettivamente fatta rispettare. Se non lo fosse, la via onesta è non promettere
  un limite sui depositi, non fingerlo.
