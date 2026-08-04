# 0024 — Riquadro pubblico per il sito

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 05 — Reputazione e vetrina
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0022`, `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha un sito o una pagina di prenotazione
> voglio mostrare lì le mie recensioni vere, con un riquadro che si incolla in un minuto
> così che chi arriva sul mio sito veda quello che dicono i clienti senza dover andare a cercarlo altrove.

**Contesto.** È la funzione «widget recensioni» della scheda di catalogo, e ha una regola che la definisce: **il
riquadro non si può filtrare per voto**. Mostrare solo le recensioni buone facendole passare per l'insieme delle
recensioni è una pratica ingannevole, esplicitamente presa di mira dalla regola statunitense sulla soppressione
(descrizione §2.3, punto 5) e riconducibile in Europa alla disciplina sulle pratiche commerciali scorrette. È il
rifiuto n. 9 dell'elenco della descrizione §1, e qui diventa codice: **non esiste il parametro**.

Quello che si può fare, e che l'app fa: mostrare le più recenti, mostrarne un numero scelto, ordinarle per data,
mostrare la media reale e il totale. Con l'attribuzione alla piattaforma d'origine (storia 0010) e con la
dichiarazione di trasparenza che la direttiva europea impone (storia 0025), senza la quale il riquadro non si
pubblica.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni sede si genera un riquadro incorporabile: un frammento da incollare nel sito, che mostra
   media, totale e un numero scelto di recensioni recenti, con nome pubblico dell'autore, voto, data, testo e
   piattaforma d'origine.
2. **RF-2** — Le uniche impostazioni sono: numero di recensioni mostrate, ordinamento (solo per data), tema chiaro
   o scuro, lingua delle etichette. **Nessun filtro per voto, in nessuna forma**: né un minimo, né una selezione
   manuale delle recensioni da mostrare.
3. **RF-3** — Il riquadro mostra sempre la **media reale e il totale reale**, anche quando ne visualizza cinque su
   duecento: quello che si limita è la lista, mai il numero che riassume.
4. **RF-4** — Il riquadro non si pubblica finché non è stata generata e accettata la dichiarazione di trasparenza
   (storia 0025), che compare nel riquadro stesso o dietro un collegamento visibile.
5. **RF-5** — Il riquadro è pubblico ma **non espone dati oltre a quelli già pubblici** sulla piattaforma
   d'origine, e non traccia i visitatori: nessun cookie, nessun identificatore, nessuna misurazione di chi lo
   guarda.
6. **RF-6** — Il riquadro si disattiva dall'app; da quel momento la chiave pubblica non serve più niente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La rotta pubblica del riquadro è servita per **chiave pubblica**, che
  identifica una sola sede di un solo account; non esiste modo di enumerare le chiavi o di leggere dati di un'altra
  sede. La chiave si può rigenerare.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta **non autenticata** `GET /api/recensioni/v1/pubblico/riquadro/{chiave}`
  con limite di frequenza e memorizzazione temporanea della risposta; rotte autenticate per generare, configurare
  e disattivare. Errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `riquadro_pubblico` (storia 0002) con chiave, impostazioni e stato; la
  chiave è un valore casuale, non derivabile dall'identificativo della sede.
- **RT-4 — Modulo frontend (§3, §5).** *Impostazioni* → «Riquadro per il sito»: anteprima dal vivo, impostazioni,
  frammento da copiare, pulsante di disattivazione. Il riquadro pubblico è servito come pagina autonoma da
  includere in una cornice: usa i token del sistema di design, funziona in tema chiaro e scuro, ed è responsivo
  perché finisce su siti che non controlliamo.
- **RT-5 — Cinque lingue (§4).** Le **etichette** del riquadro (media, recensioni, «vedi su…») in `en, it, fr, es,
  de`; il testo delle recensioni resta nella lingua originale.
- **RT-6 — Varchi e quota (§6, §7).** La generazione richiede ruolo `admin` o `owner`. **Con abbonamento
  `canceled` il riquadro smette di servire dati** e mostra un messaggio neutro: un riquadro che continua a
  funzionare dopo la disdetta sarebbe un servizio non pagato; uno che sparisce lasciando un buco nel sito del
  cliente sarebbe scortese. Il messaggio neutro è il compromesso, e va concordato.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: il riquadro è una configurazione, e la sua
  attivazione ha effetti verso l'esterno (pubblica dati su un sito). Se un giorno servisse, sarebbe scrittura con
  conferma.
- **RT-8 — Dati personali (§10).** Il riquadro **pubblica** nome dell'autore e testo su un sito di terzi: è una
  destinazione da dichiarare nel manifesto, anche se il dato era già pubblico all'origine. La rotta pubblica non
  registra indirizzi di rete oltre a quanto serve al limite di frequenza, e per il tempo minimo. Nessun
  tracciamento: solo cookie tecnici, e in realtà nemmeno quelli.
- **RT-9 — Registrazione eventi (§14).** `riquadro generato`, `riquadro disattivato`, `chiave rigenerata`, con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione. Le richieste pubbliche si registrano in forma
  aggregata, senza indirizzi.

## 4. Criteri di accettazione

**CA-1 — Il riquadro funziona**
- **Dato** una sede con quindici recensioni e la dichiarazione di trasparenza accettata
- **Quando** si genera il riquadro e si apre il frammento in una pagina di prova
- **Allora** mostra media reale, totale reale e le cinque recensioni più recenti con l'attribuzione

**CA-2 — Nessun filtro per voto**
- **Dato** le impostazioni del riquadro e la rotta pubblica
- **Quando** si cerca un modo di mostrare solo le recensioni sopra un certo voto, anche passando parametri alla
  rotta a mano
- **Allora** non esiste: parametri di quel genere sono ignorati o rifiutati

**CA-3 — Senza dichiarazione non si pubblica**
- **Dato** una sede senza dichiarazione di trasparenza accettata
- **Quando** si tenta di generare il riquadro
- **Allora** l'operazione è rifiutata con il rimando alla storia della dichiarazione

**CA-4 — La media non mente**
- **Dato** una sede con media 3,8 su duecento recensioni e un riquadro che ne mostra cinque
- **Quando** si guarda il riquadro
- **Allora** la media mostrata è 3,8 e il totale è duecento, non la media delle cinque mostrate

**CA-5 — Isolamento fra account**
- **Dato** due chiavi pubbliche di account diversi
- **Quando** si chiama la rotta pubblica con l'una e con l'altra
- **Allora** ciascuna restituisce solo i dati della propria sede, e non esiste modo di ottenerne altri

**CA-6 — Abbonamento scaduto**
- **Dato** un account con abbonamento `canceled`
- **Quando** un visitatore carica la pagina che contiene il riquadro
- **Allora** vede un messaggio neutro e nessun dato

**CA-7 — Nessun tracciamento**
- **Dato** il riquadro caricato in una pagina
- **Quando** si osservano le richieste e la memoria del browser
- **Allora** non c'è nessun cookie, nessun identificatore e nessuna chiamata di misurazione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla selezione delle recensioni mostrate (con la verifica esplicita che **non esiste**
      un percorso di filtro per voto) e di **integrazione** sulla rotta pubblica, compreso il limite di frequenza;
- [ ] prova di **isolamento fra account** sulla rotta pubblica per chiave;
- [ ] **prova end-to-end**: *coprire ora* il passo «genero il riquadro e mostra le recensioni» nel percorso
      `[J-RECENSIONI]`, e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per le etichette del riquadro;
- [ ] **manifesto dei dati** aggiornato con la pubblicazione su siti di terzi come destinazione;
- [ ] **registro delle decisioni** compilato, con il divieto di filtro per voto e la sua fonte, e con la scelta
      del comportamento ad abbonamento scaduto;
- [ ] controllo automatico di **accessibilità** verde sul riquadro pubblico, che finisce su siti che non
      controlliamo.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0022` | servono media e totale reali |
| storia `0010` | l'attribuzione è obbligatoria e le condizioni di conservazione valgono anche qui, a maggior ragione |
| storia `0025` | senza dichiarazione di trasparenza il riquadro non si pubblica: le due si implementano insieme o in quest'ordine |

## 7. Fuori ambito

- riquadri per piattaforme di terzi (negozi in rete, portali): la prima versione serve un frammento generico;
- la personalizzazione grafica libera: cambiare i colori a piacere farebbe divergere il riquadro dal sistema di
  design e aprirebbe la porta a riquadri che non sembrano più recensioni;
- la raccolta di recensioni **dal** riquadro: chiedere una recensione a un visitatore anonimo che non risulta
  cliente è esattamente ciò che le piattaforme non vogliono.

## 8. Punti aperti

- **Comportamento ad abbonamento scaduto**: il messaggio neutro è una proposta. Le alternative (riquadro vuoto,
  riquadro che sparisce) hanno effetti sul sito del cliente e vanno concordate, perché toccano un sito che non è
  nostro.
- **Se sia lecito ripubblicare il testo delle recensioni** su un sito diverso da quello d'origine dipende dalle
  condizioni delle piattaforme, che non sono state verificate (storia 0010, descrizione §11.2). È il punto che
  può ridurre questa storia alla sola media con collegamento, e va chiuso prima di implementarla.
</content>
