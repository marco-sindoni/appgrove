# 0020 — Avviso sulle recensioni negative

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 04 — Risposte e recensioni negative
**Storia**: `0020` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che non passa la giornata davanti a un cruscotto
> voglio essere avvisato quando arriva una recensione negativa, e vedere subito quali aspettano una risposta
> così da intervenire nel giro di ore, quando ancora serve a qualcosa.

**Contesto.** La velocità di risposta è l'unica leva vera che un'attività ha su una recensione negativa: una
risposta il giorno dopo cambia la lettura che ne dà chi passa di lì, una risposta dopo tre settimane no. Ma
«avviso» è una parola pericolosa: un prodotto che manda una notifica per ogni recensione a quattro stelle viene
messo a tacere entro una settimana e da quel momento non avvisa più di niente.

E c'è un punto di posizionamento che vale la pena rendere esplicito qui, perché è il momento in cui il cliente è
arrabbiato: **l'avviso porta a rispondere, non a far sparire**. Non c'è, e non ci sarà, un pulsante «rimuovi
questa recensione», né un modello di diffida. L'unica strada verso la rimozione è la segnalazione motivata per i
casi che la legge prevede (storia 0021).

## 2. Requisiti funzionali

1. **RF-1** — Quando arriva una recensione con voto sotto una soglia (proposta predefinita: 3 stelle su 5 o
   equivalente), l'app avvisa gli utenti dell'account con ruolo `admin` o `owner` per posta elettronica e con una
   segnalazione nell'interfaccia.
2. **RF-2** — La soglia e il canale sono configurabili per sede, e l'avviso si può disattivare. Gli avvisi sono
   **raggruppati**: se ne arrivano cinque nella stessa ora, il messaggio è uno.
3. **RF-3** — La *Panoramica* mostra in cima «da prendere in carico»: le recensioni negative senza risposta,
   ordinate dalla più vecchia, con da quanto tempo aspettano.
4. **RF-4** — Una recensione si può marcare «presa in carico» con una nota interna, anche prima di rispondere:
   serve a chi lavora in due, per non scrivere due risposte alla stessa recensione.
5. **RF-5** — L'app misura e mostra il **tempo medio di risposta** per sede: è l'unico indicatore che questa
   storia introduce, ed è quello su cui il cliente può davvero agire.
6. **RF-6** — L'avviso per posta elettronica **non contiene il testo della recensione**: contiene sede, voto,
   piattaforma e un collegamento all'app. Il testo di terzi non si spedisce in giro per comodità.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Gli avvisi si generano per account e vanno solo agli utenti di quel
  account; nessun avviso attraversa il confine.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `PUT /api/recensioni/v1/sedi/{id}/impostazioni-avvisi`,
  `POST /api/recensioni/v1/recensioni/{id}/presa-in-carico`,
  `GET /api/recensioni/v1/sedi/{id}/tempo-di-risposta`; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Colonne di presa in carico su `recensione` (chi, quando, nota) e le impostazioni
  degli avvisi sulla sede; migrazione `V8__presa_in_carico.sql`. La nota interna è testo libero: va trattata come
  tale nel manifesto.
- **RT-4 — Modulo frontend (§3, §5).** *Panoramica*: blocco «da prendere in carico» in cima, con il tempo di
  attesa; scheda della recensione con il pulsante di presa in carico; *Impostazioni* con soglia e canale. Il
  segnale visivo del negativo usa **anche** una forma e un'etichetta, non solo il rosso: chi non distingue i
  colori deve vedere la stessa cosa.
- **RT-5 — Cinque lingue (§4).** Interfaccia **e messaggi di avviso** in `en, it, fr, es, de`; l'avviso arriva
  nella lingua dell'utente che lo riceve.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota; con abbonamento `canceled` gli avvisi si fermano e
  l'app lo dice, invece di smettere in silenzio.
- **RT-7 — Esposizione conversazionale (§12).** È lo strumento `recensioni_negative_da_gestire` (storia 0027), di
  sola lettura: restituisce l'elenco ordinato per anzianità con voto, sede e stato, minimizzato.
- **RT-8 — Dati personali (§10).** **Voce nuova nel manifesto**: `recensione.nota_interna` (testo libero scritto
  dal cliente, può contenere riferimenti a persone). L'avviso per posta elettronica va verso un indirizzo di un
  utente della piattaforma: è un trattamento già coperto, ma va detto che **non contiene il testo di terzi**.
- **RT-9 — Registrazione eventi (§14).** `avviso inviato` con conteggio e sede, `recensione presa in carico`, con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — L'avviso parte**
- **Dato** una sede con soglia a 3 stelle e avvisi attivi
- **Quando** arriva una recensione da 2 stelle
- **Allora** gli utenti `admin` e `owner` ricevono un avviso che non contiene il testo della recensione, e la
  *Panoramica* la mostra fra quelle da prendere in carico

**CA-2 — Gli avvisi si raggruppano**
- **Dato** cinque recensioni negative arrivate nella stessa ora
- **Quando** la lavorazione degli avvisi gira
- **Allora** parte un solo messaggio che ne riassume cinque

**CA-3 — Sopra soglia niente avviso**
- **Dato** la stessa sede
- **Quando** arriva una recensione da 4 stelle
- **Allora** nessun avviso parte, e la recensione compare comunque nell'elenco

**CA-4 — Presa in carico**
- **Dato** una recensione negativa senza risposta
- **Quando** un utente la prende in carico con una nota
- **Allora** l'altro utente dell'account la vede presa in carico, con chi e quando

**CA-5 — Tempo di risposta**
- **Dato** tre recensioni negative con risposte pubblicate rispettivamente dopo 2, 10 e 24 ore
- **Quando** si guarda l'indicatore della sede
- **Allora** mostra il tempo medio calcolato su quelle, con il periodo di riferimento dichiarato

**CA-6 — Isolamento fra account**
- **Dato** due account con recensioni negative
- **Quando** la lavorazione degli avvisi gira
- **Allora** gli utenti di `A` ricevono solo avvisi su recensioni di `A`

**CA-7 — Nessuna scorciatoia**
- **Dato** la scheda di una recensione negativa
- **Quando** si cerca un modo di rimuoverla o di nasconderla
- **Allora** non esiste: le uniche azioni sono rispondere, prendere in carico e — quando ricorrono i motivi di
  legge — segnalare (storia 0021)

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul raggruppamento degli avvisi e sul calcolo del tempo medio; di **integrazione** sulla
      lavorazione con fornitore di posta **simulato**;
- [ ] prova di **isolamento fra account** sugli avvisi;
- [ ] **prova end-to-end**: *coprire ora* il passo «la recensione negativa compare fra quelle da prendere in
      carico» nel percorso `[J-RECENSIONI]`, e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, testi degli avvisi compresi;
- [ ] **manifesto dei dati** aggiornato con `recensione.nota_interna`;
- [ ] **registro delle decisioni** compilato, con la scelta di non mettere il testo della recensione nell'avviso;
- [ ] controllo automatico di **accessibilità** verde, compresa la leggibilità del segnale negativo senza colore.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | servono l'elenco e la scheda della recensione |
| storia `0009` | l'avviso nasce dall'arrivo di una recensione nuova |

## 7. Fuori ambito

- la risposta — storie 0018 e 0019;
- la segnalazione — storia 0021;
- gli avvisi su altri eventi (calo del punteggio, assenza di recensioni): il rapporto periodico li copre meglio,
  storia 0026.

## 8. Punti aperti

- **La soglia predefinita di 3 stelle su 5** è una proposta ragionata, non un dato rilevato: per certi settori il
  4 è già un problema. Va tarata con clienti veri, e va tarata **per settore**, non a occhio.
- **Canale dell'avviso.** La posta elettronica è la scelta minima. Un messaggio breve arriverebbe prima ma costa e
  introduce un altro fornitore: non lo propongo per la prima versione.
</content>
