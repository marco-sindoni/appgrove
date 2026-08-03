# 0026 — Avvio da evento

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 05 — Automazioni
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0013`, `0025`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha preparato un percorso di benvenuto
> voglio che parta da solo quando qualcuno si iscrive davvero
> così da non dover guardare ogni giorno chi è arrivato per mandargli il primo messaggio a mano.

**Contesto.** Un percorso senza avvio è un elenco di passi che non fa niente: è questa storia a metterci dentro le
persone, ed è quindi il punto in cui il rigore sul consenso deve valere anche per la macchina. La regola è la stessa
dell'epica 02 e non ammette scorciatoie: **un iscritto non confermato non fa partire niente**. Il Garante ha
stabilito che gli account non confermati vanno esclusi dalle liste di marketing
([application-description.md](../application-description.md) §2.3 punto 3): se il percorso partisse all'iscrizione
invece che alla **conferma**, avremmo costruito il modo automatico di violare quella regola migliaia di volte al
mese. Il secondo punto delicato è il doppio ingresso: chi entra due volte nello stesso percorso riceve due volte il
benvenuto, e la seconda volta è il momento in cui si segnala il messaggio come indesiderato.

## 2. Requisiti funzionali

1. **RF-1** — Un percorso ha **un** evento d'avvio, scelto fra tre: **iscrizione confermata** (facoltativamente
   ristretta a una lista o a un modulo di iscrizione), **ingresso in un segmento** (storia 0013), **data
   ricorrente** (per esempio ogni primo del mese, o l'anniversario di un campo data dell'iscritto).
2. **RF-2** — L'iscrizione **non confermata** non fa partire nulla: l'ingresso avviene alla conferma della doppia
   opt-in (storia 0008), non alla compilazione del modulo. Vale anche per l'ingresso in un segmento: un iscritto in
   quarantena o in attesa di conferma che corrisponde ai criteri **non entra**.
3. **RF-3** — Uno stesso iscritto **non entra due volte** nello stesso percorso, a meno che il percorso dichiari
   espressamente di consentire il rientro; in quel caso il rientro ha una distanza minima obbligatoria fra un
   ingresso e il successivo.
4. **RF-4** — All'attivazione del percorso **nessuno entra retroattivamente**: entrano solo le persone che
   soddisfano l'evento d'avvio da quel momento in poi. È dichiarato nel riepilogo dell'attivazione (storia 0025).
5. **RF-5** — L'evento d'avvio si valuta al momento in cui accade e produce l'ingresso in modo **idempotente**: lo
   stesso evento consegnato due volte crea una esecuzione sola.
6. **RF-6** — Al momento dell'ingresso il sistema registra **perché** quella persona è entrata (quale evento, quale
   lista o segmento, quando), così che il registro delle esecuzioni (storia 0028) possa rispondere alla domanda
   «perché ha ricevuto questo messaggio?».
7. **RF-7** — Un percorso avviato dall'ingresso in un segmento che il cliente cancella si ferma e lo dice, invece di
   restare attivo senza poter mai partire.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le esecuzioni filtrano per `tenant_id` dal token verificato quando sono
  lette dall'interfaccia, e la valutazione degli eventi d'avvio lavora **per account**: un evento di un account non
  può far entrare un iscritto in un percorso di un altro. È una prova d'isolamento obbligatoria anche sul percorso
  interno, non solo sulle rotte.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `PUT /api/campaigns/v1/automations/{id}/trigger`
  (configurazione dell'evento d'avvio) e `GET /api/campaigns/v1/automations/{id}/runs` (esecuzioni, in comune con la
  storia 0028). Corpo validato; l'elenco dei tipi di evento è chiuso. Errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__automation_run.sql` sullo schema `app_campaigns`: tabella
  `automation_run` con `tenant_id`, riferimento logico all'iscritto e al percorso, passo corrente, momento del
  prossimo passo, motivo d'ingresso, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
  Un vincolo di unicità su (`tenant_id`, percorso, iscritto, esecuzione aperta) impedisce il doppio ingresso a
  livello di dati, non solo di codice.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro «Come parte» nella scheda del percorso: scelta dell'evento,
  restrizioni, interruttore del rientro con la distanza minima. Solo token del sistema di design; tema chiaro e
  scuro.
- **RT-5 — Cinque lingue (§4).** Nomi degli eventi d'avvio, spiegazioni e avvisi passano dallo spazio-nomi
  `campaigns` in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** L'ingresso non consuma la metrica `messages_sent` (natura `flow`): consuma
  l'invio, quando il passo «manda» arriva (storia 0025). Restano il varco sull'abilitazione alla funzionalità
  (piano senza automazioni → `402`) e i varchi comuni.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo di scrittura. `elenca_percorsi`
  (storia 0025) riporta anche quanti sono entrati negli ultimi giorni, perché è la domanda che si fa a voce.
  Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Questa storia introduce la voce `automation_run.*` nel manifesto
  `docs/compliance/manifests/campaigns.yaml` in italiano e inglese: di chi è il dato (l'iscritto), che dato è (stato
  del percorso e motivo d'ingresso), a cosa serve, perché è lecito (esecuzione del contratto col nostro cliente) e
  per quanto si tiene (proposta: fino a conclusione più 12 mesi, §6 della descrizione). Campi annotati
  `@PersonalData`; tabella `automation_run` in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Iscritto entrato nel percorso» con l'identificativo dell'esecuzione, del
  percorso e dell'iscritto, il tipo di evento d'avvio, `tenant_id`, `app_id`, `user_id` o indicazione di esecuzione
  automatica, e identificativo di correlazione. Mai il recapito, mai il nome.

## 4. Criteri di accettazione

**CA-1 — Solo dopo la conferma**
- **Dato** un percorso attivo che parte dall'iscrizione confermata
- **Quando** una persona compila il modulo pubblico ma non conferma
- **Allora** non entra in nessuna esecuzione e non riceve niente; entra nel momento in cui conferma

**CA-2 — Niente ingresso retroattivo**
- **Dato** 400 iscritti confermati nei mesi scorsi e un percorso appena attivato
- **Quando** il percorso passa in stato attivo
- **Allora** nessuno dei 400 entra; entra solo chi conferma dopo l'attivazione

**CA-3 — Nessun doppio ingresso**
- **Dato** un iscritto già dentro un'esecuzione aperta di un percorso che non consente il rientro
- **Quando** l'evento d'avvio si verifica di nuovo per lui
- **Allora** non nasce una seconda esecuzione, e l'evento viene scartato con il motivo registrato

**CA-4 — Evento consegnato due volte**
- **Dato** lo stesso evento d'avvio consegnato due volte dalla piattaforma
- **Quando** entrambi vengono elaborati
- **Allora** esiste **una** esecuzione sola

**CA-5 — Quarantena esclusa**
- **Dato** un iscritto in quarantena che corrisponde ai criteri del segmento d'avvio
- **Quando** viene valutato l'ingresso
- **Allora** non entra, e il motivo è registrato come «non inviabile: in quarantena»

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con percorsi omonimi e iscritti con lo stesso indirizzo di posta
- **Quando** un iscritto di `B` conferma l'iscrizione
- **Allora** entra **solo** nel percorso di `B`, e nessuna esecuzione compare in `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla valutazione degli eventi d'avvio e sulla regola del doppio ingresso, e di
      **integrazione** sull'ingresso idempotente, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sull'ingresso, non solo sulle rotte di lettura;
- [ ] **prova end-to-end**: rimando — voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con motivo (serve il controllo del
      tempo per far avanzare i passi) e storia proprietaria 0037;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `automation_run.*`, campi annotati, tabella in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché l'avvio è alla conferma e non all'iscrizione;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, conteggio degli ingressi dentro
      `elenca_percorsi`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0008` | L'evento «iscrizione confermata» nasce dalla doppia conferma: senza, l'avvio sarebbe sull'iscrizione e violerebbe la regola |
| Storia `0013` | L'avvio per ingresso in un segmento usa i segmenti salvati |
| Storia `0025` | I passi devono esistere prima che qualcuno li percorra |
| Storia `0011` | La verifica di soppressione e quarantena all'ingresso legge l'elenco di soppressione |

## 7. Fuori ambito

- l'uscita e la sospensione: è la storia 0027, e la verifica di contattabilità **al momento del passo** sta lì,
  perché è lì che conta;
- la vista delle esecuzioni: è la storia 0028;
- eventi d'avvio provenienti da altre app della suite (per esempio un appuntamento prenotato): richiedono il
  contratto degli eventi condivisi, che nel repository non esiste
  ([application-description.md](../application-description.md) §11.5);
- eventi d'avvio da un negozio in rete (carrello abbandonato): fuori perimetro dichiarato (§11.4).

## 8. Punti aperti

- **Distanza minima per il rientro.** Proposta: un valore predefinito dichiarato (per esempio 90 giorni)
  modificabile dal cliente. Non ho trovato un riferimento di settore: è una scelta di prodotto.
- **Se l'avvio per data ricorrente su un campo data debba valere per campi personalizzati liberi.** Un anniversario
  calcolato su un campo che il cliente ha riempito male produce invii nel giorno sbagliato. Proposta: solo campi
  dichiarati di tipo data. Chiude lo sviluppatore.
