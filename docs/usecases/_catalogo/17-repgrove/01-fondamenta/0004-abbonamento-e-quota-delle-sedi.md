# 0004 — Abbonamento e quota delle sedi

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che paga il piano a una sede
> voglio che l'app mi dica chiaramente quante sedi posso tenere collegate e cosa fare se ne servono di più
> così da non scoprire il limite nel momento sbagliato e da non pagare per capacità che non uso.

**Contesto.** La metrica di quota di RepGrove è `sedi_monitorate`, di natura **a giacenza**: un tetto su quante
sedi esistono adesso, non un consumo che si azzera a fine mese (descrizione §3). Questa storia mette in piedi il
conteggio, il blocco e — punto che si dimentica sempre — il **rifiuto del passaggio a un piano più piccolo**
quando le sedi collegate superano il tetto di destinazione. Va fatta prima delle sedi vere (storia 0006), così che
la prima sede nasca già dentro il varco della quota e non ci sia mai un momento in cui il conteggio è finto.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio legge l'abilitazione e il tetto della metrica `sedi_monitorate` dalla **proiezione
   locale** alimentata a eventi: nessuna chiamata di rete sincrona all'app centrale sul percorso caldo.
2. **RF-2** — Prima di rendere attiva una sede il servizio verifica il tetto: se è raggiunto risponde `429` con un
   messaggio che dice quante sedi sono attive, qual è il tetto e come rimediare (cambiare piano o sospendere una
   sede).
3. **RF-3** — Contano verso la quota le sedi in stato `attiva`; le sedi `sospese` non contano ma restano coi loro
   dati, così che sospendere sia un rimedio reale e non una cancellazione mascherata.
4. **RF-4** — Il passaggio a un piano con tetto inferiore è **bloccato** finché le sedi attive superano il nuovo
   tetto, con un messaggio che dice esattamente quante sedi vanno sospese.
5. **RF-5** — La catena dei varchi è rispettata in questo ordine: `401` senza token, `403` ad app spenta dalla
   piattaforma, `402` ad account non abilitato, `403` a ruolo insufficiente, `429` a quota esaurita.
6. **RF-6** — Gli stati dell'abbonamento che danno accesso sono `trialing`, `active`, `past_due`; `paused` e
   `canceled` non lo danno. I diritti dell'interessato (esportazione e cancellazione) restano accessibili in ogni
   caso.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il conteggio delle sedi è per `tenant_id` preso dal token verificato;
  nessun conteggio globale, nessuna lettura di sedi altrui.
- **RT-2 — Interfaccia di programmazione (§2).** Il `429` esce in `application/problem+json` con un campo che
  distingue il motivo (`quota_sedi_esaurita`) da altri rifiuti, così che il frontend possa mostrare il messaggio
  giusto invece di un errore generico.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: il conteggio si fa su `sede.stato`. Un indice su
  `(tenant_id, stato)` rende il conteggio economico.
- **RT-4 — Modulo frontend (§3, §5).** La sezione *Impostazioni* mostra sedi attive su tetto e lo stato
  dell'abbonamento; il messaggio di quota raggiunta è un avviso bloccante, non una notifica che sparisce.
- **RT-5 — Cinque lingue (§4).** Tutti i messaggi di quota e di abbonamento passano dallo spazio-nomi
  `recensioni` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** È la storia che li implementa. La metrica è `sedi_monitorate`, natura
  `stock`; il tetto arriva dal listino come codice (`pricing/recensioni.yaml`), **mai** scritto nel servizio.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento; ma il rifiuto per quota dovrà essere
  comprensibile anche quando arriverà dal livello conversazionale: il messaggio è testo, non un codice.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-9 — Registrazione eventi (§14).** Sono registrati `sede attivata`, `sede sospesa`, `sede respinta per
  quota` e `passaggio di piano rifiutato`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione,
  senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Sotto il tetto si passa**
- **Dato** un account sul piano a una sede, con zero sedi attive
- **Quando** attiva la prima sede
- **Allora** l'operazione riesce e il contatore mostra 1 su 1

**CA-2 — Quota esaurita**
- **Dato** un account sul piano a una sede, con una sede attiva
- **Quando** tenta di attivarne una seconda
- **Allora** riceve `429`, un messaggio che spiega il tetto e il rimedio, e **nulla viene creato o attivato**

**CA-3 — Sospendere libera un posto**
- **Dato** lo stesso account con la sede al tetto
- **Quando** sospende la sede esistente e ne attiva un'altra
- **Allora** l'operazione riesce, i dati della sede sospesa restano e il contatore torna a 1 su 1

**CA-4 — Il passaggio a un piano più piccolo è bloccato**
- **Dato** un account sul piano a cinque sedi, con tre sedi attive
- **Quando** chiede di passare al piano a una sede
- **Allora** il passaggio è rifiutato con un messaggio che dice che vanno sospese due sedi

**CA-5 — Catena dei varchi**
- **Dato** rispettivamente: nessun token, app spenta dalla piattaforma, account non abilitato, ruolo `member`
  quando serve `admin`, quota esaurita
- **Quando** si chiama l'attivazione di una sede
- **Allora** si ottiene rispettivamente `401`, `403`, `402`, `403`, `429`

**CA-6 — I diritti restano**
- **Dato** un account con abbonamento `canceled`
- **Quando** chiede l'esportazione dei propri dati
- **Allora** l'esportazione funziona, anche se ogni altra funzione risponde `402`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio e sulla decisione di blocco, di **integrazione** sulla rotta di attivazione
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul conteggio: le sedi di `B` non spostano il contatore di `A`;
- [ ] **prova end-to-end**: *rimando* alla storia 0030, con voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la motivazione della natura `stock` e delle conseguenze sul
      passaggio di piano;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] il file `pricing/recensioni.yaml` è coerente con quanto il servizio si aspetta (metrica, natura, tetti).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve il servizio e il file di listino generato |
| storia `0002` | serve la tabella `sede` con il suo stato |
| decisione sul **listino** (descrizione §5) | i tetti dei due piani sono una fermata di escalation dello sviluppatore |

## 7. Fuori ambito

- la gestione vera delle sedi (creazione, modifica, indirizzo) — storia 0006;
- l'acquisto dell'abbonamento e il passaggio di piano dal lato del cliente: è funzione di piattaforma, non
  dell'app.

## 8. Punti aperti

- **Cosa succede alle recensioni di una sede sospesa**: continuano ad arrivare o la raccolta si ferma? La mia
  inclinazione è fermare la raccolta (una sede sospesa non consuma quota e non deve consumare chiamate verso le
  piattaforme) e dirlo chiaramente nell'interfaccia. Va confermato, perché ha un effetto visibile sul cliente.
- Il **tetto di cinque sedi** del piano superiore è una proposta: chi ha sei sedi oggi non ha un piano
  (descrizione §11.4).
</content>
