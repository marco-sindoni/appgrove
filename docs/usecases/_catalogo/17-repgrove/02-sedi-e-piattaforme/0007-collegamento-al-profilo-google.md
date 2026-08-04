# 0007 — Collegamento al profilo Google

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 02 — Sedi e collegamento alle piattaforme
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha già una scheda della propria attività su Google
> voglio collegare quella scheda alla sede che ho creato in RepGrove, autorizzandolo io stesso
> così che le mie recensioni arrivino nell'app senza che io debba dare a nessuno la password del mio profilo.

**Contesto.** È la storia che decide se l'app è lecita o no nel modo in cui legge i dati. L'unica via ammessa è la
**delega del proprietario del profilo**: il cliente autorizza RepGrove ad agire sul proprio profilo attraverso le
interfacce ufficiali di Google, che restituiscono dati solo per gli account di cui l'utente autenticato ha la
proprietà o la gestione verificata
([Google Business Profile APIs — Work with review data](https://developers.google.com/my-business/content/review-data)).
Non esiste una via alternativa che sia insieme comoda e permessa: leggere le recensioni altrui dalle interfacce
dei luoghi ha condizioni di conservazione molto più strette (descrizione §2.7 e §11.2) ed estrarle dalle pagine è
vietato.

C'è un vincolo pratico da mettere in conto **prima**: l'accesso a quelle interfacce va richiesto formalmente, con
un caso d'uso motivato, e i progetti nuovi partono da quota zero. Se la domanda non è stata fatta, questa storia
non si può nemmeno cominciare (descrizione §11, «rischi noti»).

## 2. Requisiti funzionali

1. **RF-1** — Dalla scheda di una sede si avvia il collegamento a Google: il cliente viene portato alla schermata
   di autorizzazione di Google, autorizza con il proprio account e torna nell'app.
2. **RF-2** — Dopo l'autorizzazione l'app mostra le sedi del profilo a cui quell'account ha accesso e chiede a
   quale corrisponde la sede di RepGrove. L'abbinamento è **esplicito**, mai indovinato dall'indirizzo.
3. **RF-3** — Il collegamento ha uno stato visibile e comprensibile: `da autorizzare`, `attivo`, `scaduto`,
   `revocato`, `in errore`, con la data dell'ultima sincronizzazione riuscita e, se c'è, il motivo dell'errore in
   parole comprensibili.
4. **RF-4** — Il cliente può **revocare** il collegamento dall'app in ogni momento; la revoca ferma la raccolta e
   non cancella le recensioni già raccolte (che restano fino alla loro scadenza di conservazione, storia 0010).
5. **RF-5** — Le credenziali di delega sono cifrate a riposo, non compaiono in nessuna risposta delle rotte e non
   compaiono mai nei registri.
6. **RF-6** — Un solo collegamento attivo per coppia (sede, piattaforma). Collegare una sede già collegata
   sostituisce il collegamento precedente, dopo una conferma esplicita.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `collegamento_piattaforma` filtra per
  `tenant_id` preso dal token verificato. Un collegamento non è mai visibile o utilizzabile da un altro account,
  nemmeno per errore di correlazione al ritorno dall'autorizzazione: lo stato dello scambio porta con sé l'account
  e viene verificato al ritorno.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/recensioni/v1/sedi/{id}/collegamenti/google/avvio`,
  `POST /api/recensioni/v1/collegamenti/google/ritorno`,
  `POST /api/recensioni/v1/collegamenti/{id}/abbinamento`,
  `DELETE /api/recensioni/v1/collegamenti/{id}`; errori in `application/problem+json` con un codice che distingue
  «autorizzazione negata», «nessuna sede disponibile», «quota della piattaforma esaurita»; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `collegamento_piattaforma` già presente (storia 0002); il segreto sta in
  una colonna cifrata, con la chiave gestita dall'infrastruttura, mai nel codice.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Sedi* → scheda della sede: riquadro «Piattaforme collegate» con
  stato, ultimo aggiornamento e i due pulsanti (collega / revoca). Solo token del sistema di design; funziona in
  tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, compresi i messaggi di errore dell'autorizzazione, passano
  dallo spazio-nomi `recensioni` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Si può collegare solo una sede `attiva`, quindi dentro il tetto di
  `sedi_monitorate`. Con abbonamento non attivo la rotta risponde `402`. Il ruolo richiesto è `admin` o `owner`:
  autorizzare un accesso al profilo dell'azienda non è un'azione da `member`.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento**: l'autorizzazione a un servizio esterno
  richiede un passaggio di autenticazione su un sito di terzi e non è un'operazione che un assistente possa
  compiere per conto dell'utente. Va dichiarato esplicitamente nel contratto degli strumenti (storia 0027).
- **RT-8 — Dati personali (§10).** Il collegamento non introduce dati di persone, ma introduce **un fornitore
  esterno che tratterà dati per conto del cliente**: va aggiunto all'elenco dei fornitori e all'informativa
  (descrizione §6). Le credenziali di delega si trattano come segreti, non come dati personali.
- **RT-9 — Registrazione eventi (§14).** `collegamento avviato`, `collegamento attivato`, `collegamento revocato`,
  `collegamento scaduto`, con `tenant_id`, `app_id`, `user_id`, identificativo del collegamento e identificativo di
  correlazione. **Mai** il segreto, mai l'indirizzo di posta dell'account Google.

## 4. Criteri di accettazione

**CA-1 — Collegamento riuscito**
- **Dato** un account con una sede attiva e un utente con ruolo `admin`
- **Quando** avvia il collegamento, autorizza su Google e sceglie la sede corrispondente
- **Allora** il collegamento risulta `attivo`, con la data dell'autorizzazione, e la scheda della sede lo mostra

**CA-2 — Autorizzazione negata**
- **Dato** un cliente che rifiuta l'autorizzazione sulla schermata di Google
- **Quando** torna nell'app
- **Allora** il collegamento resta `da autorizzare`, l'app spiega cosa è successo e cosa fare, e nessun segreto è
  stato salvato

**CA-3 — Nessuna sede disponibile**
- **Dato** un cliente che autorizza con un account che non gestisce nessuna scheda
- **Quando** l'app chiede l'elenco delle sedi disponibili
- **Allora** mostra un messaggio che spiega che quell'account non gestisce schede e come rimediare (rivendicare la
  scheda su Google), **senza** proporre scorciatoie

**CA-4 — Isolamento allo scambio di autorizzazione**
- **Dato** uno scambio di autorizzazione avviato dall'account `A`
- **Quando** il ritorno arriva dentro una sessione dell'account `B`
- **Allora** la richiesta è rifiutata e nessun collegamento viene creato

**CA-5 — Revoca**
- **Dato** un collegamento attivo con recensioni già raccolte
- **Quando** il cliente lo revoca
- **Allora** lo stato diventa `revocato`, la raccolta si ferma, le recensioni già raccolte restano visibili e il
  segreto viene cancellato

**CA-6 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** tenta di avviare o revocare un collegamento
- **Allora** riceve `403` e nulla cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati del collegamento e di **integrazione** sulle rotte, con il
      fornitore esterno **simulato** (nessuna chiamata reale nelle prove);
- [ ] prova di **isolamento fra account** sulla risorsa `collegamento_piattaforma` e sullo scambio di
      autorizzazione;
- [ ] **prova end-to-end**: *coprire ora* il passo «collegamento della sede» nel percorso `[J-RECENSIONI]`, con il
      fornitore simulato, e registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
      aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, ma **fornitore esterno aggiunto** all'elenco e all'informativa;
- [ ] **registro delle decisioni** compilato, con la scelta dell'interfaccia usata e il perché delle alternative
      scartate;
- [ ] contratto degli **strumenti conversazionali**: dichiarato che questa operazione **non** è esponibile;
- [ ] verificato che le credenziali non compaiano in nessuna risposta né in nessun registro.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | serve una sede a cui collegare il profilo |
| **domanda di accesso alle interfacce di Google** | l'accesso va richiesto e approvato, e i progetti nuovi partono da quota zero: è un passo che sta fuori dal codice e va fatto prima |
| gestione dei segreti dell'infrastruttura | serve un posto dove cifrare le credenziali di delega |

## 7. Fuori ambito

- la raccolta vera delle recensioni — storia 0009;
- la pubblicazione delle risposte, che usa lo stesso collegamento — storia 0019;
- Trustpilot — storia 0008;
- qualunque altra piattaforma (descrizione §11.4).

## 8. Punti aperti

- **Quota e limiti della piattaforma.** L'accesso approvato parte con quote basse, sufficienti per poche sedi. Con
  molti clienti la quota diventa un collo di bottiglia condiviso fra tutti gli account: è un problema di
  piattaforma, non di questa app, e va sollevato prima di vendere il piano a cinque sedi.
- **Che cosa succede quando la delega scade.** Le deleghe hanno una vita e si interrompono. La mia inclinazione è
  avvisare il cliente per posta elettronica al primo fallimento e mostrare lo stato `scaduto` in evidenza, non
  tentare rinnovi silenziosi che il cliente non capirebbe. Da confermare.
- **Se il cliente non è proprietario della scheda** (capita: l'ha creata un'agenzia). L'app non può risolverlo, e
  non deve fingere di poterlo: mostra la spiegazione e si ferma.
</content>
