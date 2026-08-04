# 0020 — Disiscrizione onorata durante la spedizione

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 04 — Spedizione e canali
**Storia**: `0020` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0012`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che ha appena cliccato «non voglio più ricevere queste comunicazioni»
> voglio che il messaggio successivo non mi arrivi
> così da non dovermi disiscrivere tre volte e da non finire a segnalare il mittente come indesiderato.

**Contesto.** Una spedizione a diecimila destinatari, con il ritmo prudente della storia `0019`, dura ore. In
quelle ore le persone leggono, e alcune si disiscrivono — spesso proprio da **quel** messaggio. Se la lista dei
destinatari viene fotografata all'inizio e poi servita fino in fondo, chi si è disiscritto alle 9:05 riceve
comunque alle 11:40. Per la persona è la prova che la disiscrizione non funziona, e la reazione tipica non è
riprovare: è premere «segnala come posta indesiderata», che è il gesto che costa il recapito di **tutti** gli
account (§2.3 punto 5 e §11, rischi noti).

I grandi fornitori di posta chiedono che la disiscrizione sia onorata **entro due giorni**; questa storia sceglie
un vincolo molto più stretto — **subito, al singolo messaggio** — perché è tecnicamente possibile e perché è la
promessa che l'app fa. È una storia piccola nel codice e centrale nella dottrina: si scrive **dopo** la spedizione
soltanto perché è lì che si innesta, non perché sia meno importante.

## 2. Requisiti funzionali

1. **RF-1** — Immediatamente **prima** della consegna del singolo messaggio, il servizio verifica che il
   destinatario sia ancora inviabile su quel canale. La fotografia iniziale del segmento **non** è sufficiente e
   non è la fonte della decisione.
2. **RF-2** — Sono considerati **non inviabili**, senza eccezioni e senza alcun modo di forzare: l'iscritto non
   ancora confermato, quello in quarantena, quello che ha revocato il consenso sul canale, quello il cui recapito
   compare nell'elenco di soppressione.
3. **RF-3** — Una consegna saltata per questo motivo **non viene persa**: si chiude con esito «saltata» e il
   **motivo** (revoca, soppressione, quarantena, mancata conferma), e compare nel rapporto della campagna.
4. **RF-4** — Una consegna saltata **non consuma** la metrica `messages_sent`: se l'unità era già stata prenotata,
   viene restituita. Non si fa pagare un messaggio che non è partito.
5. **RF-5** — La revoca ha effetto anche sulle consegne **già create e ancora in coda**, non solo su quelle non
   ancora generate: è la differenza fra onorare l'opposizione e dichiarare di onorarla.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La verifica di contattabilità legge il registro del consenso e l'elenco
  di soppressione **dell'account della campagna**, filtrando per `tenant_id`; la soppressione di un account non
  influenza le consegne di un altro, perché l'opposizione è stata espressa verso quel mittente.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova esposta all'esterno: la verifica è un passo
  interno del percorso di consegna. Il **motivo dello scarto** entra nella risposta di
  `GET /api/campaigns/v1/campaigns/{id}/progress` e nel rapporto (storia `0030`) come conteggi per motivo; la
  definizione OpenAPI si aggiorna di conseguenza.
- **RT-3 — Persistenza (§8).** La tabella `delivery` (storia `0002`) acquisisce lo stato «saltata» e la colonna del
  motivo; indice su `(tenant_id, campaign_id, status)` per i conteggi del rapporto. Nessuna nuova tabella.
- **RT-4 — Modulo frontend (§3, §5).** La schermata di avanzamento (storia `0019`) mostra la voce «saltati» con la
  ripartizione per motivo. È informazione utile e va detta bene: «12 non contattabili» senza il perché sembra un
  guasto. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi dei motivi di scarto passano dallo spazio-nomi `campaigns` e sono presenti
  in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Restituzione dell'unità di `messages_sent` (natura `flow`) quando la consegna
  viene saltata. La restituzione dev'essere **idempotente**: una consegna saltata due volte non restituisce due
  unità.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Lo strumento di lettura `stato_iscritto`
  (storia `0034`) usa **la stessa** funzione di contattabilità di questa storia: la risposta alla domanda «posso
  scrivergli?» e la decisione «gli scrivo?» devono venire dallo stesso codice, altrimenti prima o poi divergono.
- **RT-8 — Dati personali (§10).** **Nessun campo nuovo** che riguardi una persona: si aggiunge uno stato e un
  motivo a una tabella già dichiarata. Il motivo dello scarto è però un dato **sul comportamento** dell'iscritto
  (si è opposto) e va descritto nella voce `delivery.*` del manifesto, in italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** «Consegna saltata» con il **codice** del motivo, l'identificativo
  dell'iscritto, `tenant_id`, `app_id`, `user_id` della lavorazione e identificativo di correlazione. Mai il
  recapito.

## 4. Criteri di accettazione

**CA-1 — Disiscrizione a spedizione avviata**
- **Dato** una campagna `in corso` con 500 destinatari, di cui uno non ancora servito
- **Quando** quella persona si disiscrive e la coda arriva alla sua consegna
- **Allora** il messaggio **non** parte, la consegna si chiude come «saltata — revoca del consenso» e il conteggio
  dei serviti resta coerente

**CA-2 — Corsa fra revoca e consegna**
- **Dato** una consegna in fase di invio e una revoca che arriva nello stesso istante
- **Quando** le due operazioni si sovrappongono
- **Allora** l'esito è **deterministico** e mai un doppio effetto: o il messaggio parte (e la revoca vale dal
  successivo) o viene saltato, ma non si verificano né l'invio con consegna marcata «saltata» né la doppia
  restituzione di quota. La prova esercita esplicitamente la concorrenza

**CA-3 — La quota torna indietro**
- **Dato** un account con 100 invii residui e una campagna in cui 5 destinatari risultano soppressi al momento
  della consegna
- **Quando** la spedizione si conclude
- **Allora** sono stati consumati **95** invii, non 100, e il consumo mostrato al cliente coincide con i messaggi
  effettivamente partiti

**CA-4 — Nessuna forzatura**
- **Dato** un destinatario in quarantena selezionato dal segmento
- **Quando** si tenta di far partire la consegna in qualunque modo, anche con una richiesta costruita a mano
- **Allora** la consegna viene saltata; non esiste parametro, ruolo o pulsante che produca un esito diverso

**CA-5 — Il rapporto lo dice**
- **Dato** una campagna conclusa con 12 destinatari saltati per tre motivi diversi
- **Quando** si apre l'avanzamento o il rapporto
- **Allora** compaiono i conteggi **per motivo**, non un totale indistinto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla funzione di contattabilità (una sola, condivisa con `stato_iscritto`) e prova di
      **concorrenza** sulla corsa fra revoca e consegna;
- [ ] prova di **integrazione** che verifica la restituzione idempotente della quota;
- [ ] prova di **isolamento fra account** sulla contattabilità (la soppressione di `A` non tocca `B`);
- [ ] **prova end-to-end**: coprire ora — `[J-CAMPAIGNS]` (storia `0037`) esegue una disiscrizione **mentre** la
      campagna è in corso e verifica che quel destinatario non riceva; è il passo che dimostra la promessa
      dell'app. Registro di copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml)
      aggiornato;
- [ ] **traduzioni** dei motivi di scarto in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per `delivery.*` con lo stato «saltata» e il motivo, in italiano e inglese;
- [ ] **registro delle decisioni** compilato, con annotato perché la verifica è al singolo messaggio e non a
      inizio spedizione;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; annotato che `stato_iscritto` condivide la
      funzione di contattabilità;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0011` | L'elenco di soppressione è una delle quattro cause di non inviabilità |
| Storia `0012` | La disiscrizione in un clic è il modo in cui la revoca arriva, ed è il caso che questa storia deve onorare |
| Storia `0019` | È il percorso di consegna in cui questa verifica si innesta |

## 7. Fuori ambito

- la **raccolta** della disiscrizione (collegamento, intestazione, pagina di conferma): è la storia `0012`;
- l'alimentazione della soppressione a partire dai ritorni del fornitore: è la storia `0021`;
- l'uscita di un iscritto da un percorso automatico quando si disiscrive: è la storia `0027`, stessa regola in
  un'altra epica;
- la propagazione della disiscrizione verso l'app 04 LeadGrove: passa dagli eventi ed è subordinata al contratto
  degli eventi dell'anagrafica condivisa, che non esiste ancora
  ([application-description.md](../application-description.md) §11.5).

## 8. Punti aperti

- **Che cosa succede alle consegne saltate se il cliente rimanda la stessa campagna.** La proposta è che una
  campagna nuova sia una campagna nuova, con consegne nuove: la saltata non «riparte». Sembra ovvio ma va scritto,
  perché il vincolo di unicità della storia `0019` è per campagna e non per messaggio.
- **Nessun altro**: la regola di questa storia è deliberatamente rigida e non ammette configurazione.
