# 0018 — Controllo pre-volo

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 04 — Spedizione e canali
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0012`, `0013`, `0015`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta per mandare un messaggio a duemila persone
> voglio che il sistema mi dica **prima** se qualcosa non va e cosa esattamente
> così da non scoprire l'errore quando è già partito e non si torna indietro.

**Contesto.** Un invio è un atto **irreversibile verso l'esterno**: una volta consegnato non si richiama. Tutti i
concorrenti esaminati (§2.1) forniscono gli strumenti per essere in regola ma **nessuno impedisce** di spedire a
una lista di cui il cliente non sa niente; è esattamente lo spazio che questa proposta occupa mettendo il presidio
nelle fondamenta invece che nella pagina di aiuto.

Il controllo pre-volo è il punto in cui quel presidio diventa codice: è **l'unica** porta fra `in verifica` e
`programmata` nella macchina a stati della campagna ([application-description.md](../application-description.md)
§4), e non esiste nessuna transizione che la scavalchi. Va scritto adesso, prima della spedizione (storia `0019`):
se arriva dopo, per qualche settimana esiste un modo di spedire senza controlli — e quel modo, una volta scritto,
non lo toglie più nessuno.

## 2. Requisiti funzionali

1. **RF-1** — Il passaggio da `in verifica` a `programmata` esegue un elenco **chiuso** di controlli **bloccanti**:
   dominio mittente `verificato`; canale della campagna attivo per l'account; segmento non vuoto; destinatari
   effettivamente inviabili maggiori di zero; piè di pagina con collegamento di disiscrizione presente; tutti i
   campi variabili risolvibili (valore o ripiego); quota residua `messages_sent` sufficiente per i destinatari
   previsti; tasso di segnalazione dell'account sotto la soglia dello 0,3 %.
2. **RF-2** — Esiste un secondo elenco di controlli di **avviso**, che non bloccano: oggetto vuoto o molto lungo,
   nessuna versione in solo testo, quota residua sufficiente ma sotto il 20 %, segmento molto più piccolo
   dell'ultima volta, invio programmato in orario notturno.
3. **RF-3** — L'esito è **leggibile**: per ogni controllo si vede se è verde, giallo o rosso, il motivo in una
   frase e — dove ha senso — il collegamento diretto alla schermata che lo risolve.
4. **RF-4** — **Non esiste alcun modo di forzare** un controllo bloccante: nessun pulsante «procedi comunque»,
   nessun parametro della richiesta, nessuna deroga dalla console di amministrazione. Un rosso si risolve, non si
   ignora.
5. **RF-5** — Il controllo si può eseguire **a vuoto** in qualunque momento mentre la campagna è in `bozza`, senza
   cambiarne lo stato: serve a lavorare, non solo a passare l'esame.
6. **RF-6** — L'esito di ogni esecuzione viene conservato con il momento, chi l'ha chiesta e il risultato di
   ciascun controllo: è la prova che al momento della programmazione le condizioni erano soddisfatte.
7. **RF-7** — Il controllo viene **ripetuto** immediatamente prima dell'inizio effettivo della spedizione: fra la
   programmazione e la partenza possono passare giorni, e nel frattempo un dominio può decadere o una quota
   esaurirsi. Se il secondo passaggio è rosso, la campagna va in `bloccata` e non parte.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni controllo legge dati filtrati per `tenant_id` preso dal token
  verificato; il conteggio dei destinatari inviabili, la quota e il tasso di segnalazione sono **dell'account che
  chiede**, mai di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/campaigns/v1/campaigns/{id}/preflight` (esegue e
  restituisce l'esito senza cambiare stato) e `POST /api/campaigns/v1/campaigns/{id}/schedule` (esegue e, **solo se
  tutti i bloccanti sono verdi**, porta a `programmata`). La seconda **non accetta** parametri di forzatura. Errori
  in `application/problem+json` con l'elenco strutturato dei controlli falliti; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__create_preflight_result.sql` sullo schema `app_campaigns`: tabella
  `preflight_result` con `tenant_id`, chiave primaria UUID versione 7, riferimento logico alla campagna, esito per
  controllo in forma strutturata, colonne di controllo e cancellazione logica. Nessuna chiave esterna verso altri
  schemi.
- **RT-4 — Modulo frontend (§3, §5).** Schermata «Verifica prima dell'invio» del modulo `campaigns`: elenco dei
  controlli con semaforo, motivo e collegamento di risoluzione; il pulsante «Programma l'invio» è **disabilitato**
  finché c'è un rosso, e il motivo è leggibile senza passarci sopra col puntatore. Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi dei controlli, i motivi di fallimento e i testi di rimedio passano dallo
  spazio-nomi `campaigns` e sono presenti in `en, it, fr, es, de`. I motivi sono **stringhe con parametri**, non
  frasi composte a pezzi: una frase composta per concatenazione non si traduce.
- **RT-6 — Varchi e quota (§6, §7).** Il controllo pre-volo **non consuma** `messages_sent` (natura `flow`): la
  legge soltanto. La prenotazione avviene per destinatario al momento dell'invio (storia `0019`). Con abbonamento
  `canceled` la rotta risponde `402`; con `past_due` funziona.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `programma_invio(id_campagna, momento)` dichiarato
  nella descrizione (§7) **non programma**: esegue il controllo pre-volo e **restituisce l'esito**, poi chiede.
  Marcato **scrittura**, con **conferma umana obbligatoria**. È il caso da manuale della regola «l'intelligenza
  artificiale prepara, la persona approva». Dipendenza: UC 0061-0063, non ancora implementati.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: l'esito conserva **conteggi e codici**, non
  elenchi di destinatari. In particolare il controllo «destinatari inviabili > 0» salva il numero, mai gli
  indirizzi. Si aggiunge comunque `preflight_result` a `exportData` e `purgeData` perché è collegata alla campagna.
- **RT-9 — Registrazione eventi (§14).** «Controllo pre-volo eseguito» con esito complessivo e codici dei controlli
  falliti, «campagna programmata», «campagna bloccata dal controllo pre-volo», tutti con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione. Nessun recapito nei registri.

## 4. Criteri di accettazione

**CA-1 — Tutto verde, la campagna si programma**
- **Dato** una campagna in `in verifica` con dominio verificato, segmento da 340 destinatari inviabili, piè di
  pagina completo e quota residua di 2.000 invii
- **Quando** l'utente chiede di programmare l'invio per domani alle 9
- **Allora** tutti i controlli bloccanti sono verdi, la campagna passa a `programmata` e l'esito viene conservato
  con il momento e chi l'ha chiesto

**CA-2 — Un rosso blocca, e si capisce quale**
- **Dato** la stessa campagna, ma con il dominio mittente passato a `decaduto`
- **Quando** si chiede di programmare
- **Allora** la richiesta è respinta, la campagna **resta** in `in verifica`, e l'esito indica il controllo
  «dominio mittente verificato» in rosso con il motivo e il collegamento alla sezione dei domini

**CA-3 — Nessuna forzatura possibile**
- **Dato** una campagna con un controllo bloccante rosso
- **Quando** si invia la richiesta di programmazione con un parametro aggiuntivo di forzatura (per esempio
  `force=true`) o con l'esito manipolato nel corpo
- **Allora** il parametro viene ignorato, il controllo viene comunque rieseguito lato servizio e la richiesta è
  respinta con lo stesso esito

**CA-4 — Segmento senza nessuno di contattabile**
- **Dato** un segmento che seleziona 120 iscritti, tutti in quarantena o disiscritti
- **Quando** si esegue il controllo pre-volo
- **Allora** il controllo «destinatari inviabili > 0» è rosso, il motivo dice quanti sono stati esclusi e per quale
  causa (in forma di conteggi per motivo, non di elenco di persone)

**CA-5 — La verifica si ripete alla partenza**
- **Dato** una campagna `programmata` per domani e una quota che nel frattempo si esaurisce
- **Quando** arriva il momento della partenza
- **Allora** il controllo viene rieseguito, risulta rosso sulla quota, la campagna passa a `bloccata`, non parte
  **nessun** messaggio e il cliente riceve l'avviso

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie campagne
- **Quando** un utente di `A` chiede il controllo pre-volo sull'identificativo di una campagna di `B`
- **Allora** riceve `404` e nessun dato di `B` compare nell'esito

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** su ciascun controllo bloccante preso singolarmente e sulla regola di composizione
      dell'esito; prove di **integrazione** sulle due rotte, compresa la ripetizione del controllo alla partenza;
- [ ] prova di **isolamento fra account** sul controllo pre-volo;
- [ ] prova esplicita che **nessun parametro di forzatura** cambia il comportamento;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia `0037`) include un tentativo di
      programmazione **respinto** e uno riuscito, perché il rifiuto è la funzione, non un caso limite; registro di
      copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con i motivi come stringhe parametriche;
- [ ] **manifesto dei dati**: `preflight_result` dichiarata, con la nota che conserva conteggi e non destinatari;
      tabella in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato, con annotato perché l'elenco dei controlli bloccanti è **chiuso** e
      perché non esiste la forzatura;
- [ ] contratto degli **strumenti conversazionali**: `programma_invio` dichiarato come scrittura con conferma
      obbligatoria, con la precisazione che restituisce l'esito e non programma;
- [ ] controllo automatico di **accessibilità** verde sulla schermata di verifica (il semaforo non deve essere
      l'unico veicolo dell'informazione: serve anche il testo);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0011` | L'elenco di soppressione entra nel conteggio dei destinatari inviabili |
| Storia `0012` | Il controllo «disiscrizione presente» verifica ciò che quella storia rende obbligatorio |
| Storia `0013` | Senza segmenti salvati non c'è niente da contare |
| Storia `0015` | I campi variabili e i loro valori di ripiego sono ciò che il controllo verifica |
| Storia `0017` | Lo stato del dominio mittente è il primo controllo bloccante |

## 7. Fuori ambito

- la **spedizione** vera e propria e la sua coda: è la storia `0019`;
- il controllo di contattabilità **per singolo destinatario al momento della consegna**: è la storia `0020`. Qui si
  contano i destinatari inviabili in blocco, che è una fotografia; là si verifica uno per uno, che è la garanzia;
- il calcolo del tasso di segnalazione: è la storia `0021`, qui se ne **legge** il valore;
- la sospensione dell'invio decisa dalla piattaforma: sta in [estensioni-admin.md](../estensioni-admin.md) e agisce
  a monte, spegnendo il canale dell'account;
- la prova a due varianti, che ha condizioni proprie: storia `0031`.

## 8. Punti aperti

- **Soglia del controllo «quota residua sotto il 20 %»**: è un avviso, quindi non pericoloso, ma la percentuale è
  scelta a occhio. Chiude lo sviluppatore guardando i dati reali dopo qualche mese.
- **Orario notturno come avviso**: che cosa sia «notte» dipende dal fuso orario dei destinatari, che spesso non
  conosciamo. La proposta è usare il fuso dell'account e dirlo esplicitamente nell'avviso. Da confermare.
- **Se il controllo pre-volo debba considerare anche il rapporto fra dimensione del segmento e storia dell'account**
  (un account che ha sempre mandato a 200 persone e improvvisamente ne fa 20.000 è il profilo tipico della lista
  comprata). È un presidio antifrode che tocca la reputazione condivisa: appartiene alla piattaforma più che a
  questa app, e va deciso insieme alla sospensione dalla console di amministrazione.
