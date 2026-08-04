# 0028 — Registro delle esecuzioni

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 05 — Automazioni
**Storia**: `0028` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0025`, `0026`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha un percorso automatico acceso da un mese
> voglio vedere chi è dentro, a che punto è e cosa non ha funzionato
> così da poter rispondere a un cliente che mi chiede «perché mi avete scritto?» e da accorgermi se il percorso si
> è fermato senza dirmelo.

**Contesto.** Un percorso automatico è l'unica parte dell'app che lavora quando nessuno guarda, e per questo è
l'unica che può fallire in silenzio per settimane. Le storie 0026 e 0027 hanno già scritto tutto ciò che serve —
motivo d'ingresso, passo corrente, motivo di uscita o di sospensione — ma scritto in un archivio non è la stessa
cosa di leggibile. Questa storia è piccola e mette insieme le due domande che il cliente fa davvero: *«sta
funzionando?»*, cioè quanti sono dentro e quanti si sono fermati e perché; e *«perché ha ricevuto questo
messaggio?»*, che è la domanda a cui bisogna saper rispondere quando qualcuno si lamenta — e che si risponde
mostrando l'evento d'avvio con il suo momento.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni percorso è consultabile l'elenco delle esecuzioni con: iscritto, stato (aperta, sospesa,
   terminata), passo corrente, momento del prossimo passo, motivo d'ingresso e — se c'è — motivo di uscita o di
   sospensione.
2. **RF-2** — L'elenco si filtra per stato e per motivo, e si cerca per iscritto: è così che si risponde alla
   domanda «perché ha ricevuto questo messaggio?» partendo da un indirizzo.
3. **RF-3** — Un riepilogo in testa mostra i conteggi: quanti dentro, quanti sospesi e per quale motivo, quanti
   terminati e per quale motivo, quanti invii il percorso ha prodotto nel mese.
4. **RF-4** — Ogni esecuzione ha un dettaglio con la sequenza dei passi già eseguiti e il loro esito (inviato,
   saltato per condizione, non inviato con il motivo), in ordine di tempo.
5. **RF-5** — I **guasti** — invii falliti per errore del fornitore di consegna, passi non eseguiti per problemi
   tecnici — sono contati a parte e mostrati in evidenza: un percorso con guasti non deve sembrare sano.
6. **RF-6** — Il riepilogo si esporta in un file tabellare, con la stessa avvertenza usata altrove sull'uso dei dati
   esportati (storia 0033).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Elenco, dettaglio, conteggi ed esportazione filtrano per `tenant_id`
  preso dal token verificato; un `tenant_id` dal corpo della richiesta o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/campaigns/v1/automations/{id}/runs` con
  paginazione a pagina e dimensione con totale, filtri per stato e motivo e ricerca per iscritto;
  `GET .../runs/{runId}` per il dettaglio; `POST .../runs/export` per il file. Errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova di dominio: si legge `automation_run` (storia 0026) e i suoi
  stati (storia 0027). Si aggiunge la migrazione `V<N>__automation_run_step_log.sql` con la tabella
  `automation_run_step` — un passo eseguito per riga, con esito e momento — perché senza di essa il dettaglio del
  requisito RF-4 non è ricostruibile. Con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Scheda del percorso con il riepilogo in testa, tabella delle esecuzioni,
  filtri e dettaglio dell'esecuzione. Solo token del sistema di design; tema chiaro e scuro; stato vuoto che dice
  cosa fare quando non è ancora entrato nessuno.
- **RT-5 — Cinque lingue (§4).** Etichette, stati, motivi ed esiti dei passi passano dallo spazio-nomi `campaigns`
  in `en, it, fr, es, de`. Nel file esportato le intestazioni seguono la lingua dell'utente che esporta.
- **RT-6 — Varchi e quota (§6, §7).** Consultare ed esportare non consumano la metrica `messages_sent` (natura
  `flow`). Valgono i varchi comuni; l'abilitazione alla funzionalità delle automazioni è richiesta anche per la
  sola lettura, perché la sezione è quella (piano senza automazioni → `402`).
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `stato_iscritto` (§7 della
  [descrizione dell'applicazione](../application-description.md)), marcato **lettura**, viene esteso per riportare
  anche **in quali percorsi automatici la persona si trova e da quale evento è entrata**: è la forma
  conversazionale della domanda «perché ha ricevuto questo messaggio?». Il contratto vive dentro il servizio; il
  server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessuna categoria nuova: si mostrano dati già dichiarati (`automation_run.*` e i
  recapiti dell'iscritto). La tabella `automation_run_step` è però nuova e riferisce una persona: va aggiunta al
  manifesto `docs/compliance/manifests/campaigns.yaml` in italiano e inglese, con i campi annotati e la tabella
  presente in `exportData` e `purgeData`. L'esportazione del riepilogo è un'uscita di dati e va registrata come
  tale.
- **RT-9 — Registrazione eventi (§14).** «Esecuzioni consultate» ed «esportazione delle esecuzioni richiesta» con
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e numero di righe; **nessun contenuto**, nessun
  recapito, nessun nome — il registro tecnico conta, non racconta.

## 4. Criteri di accettazione

**CA-1 — Perché ha ricevuto questo messaggio**
- **Dato** un'iscritta che chiede spiegazioni
- **Quando** l'utente la cerca nel registro delle esecuzioni per indirizzo
- **Allora** vede in quali percorsi si trova o si è trovata, da quale evento d'avvio è entrata, in che momento e
  quali passi ha ricevuto

**CA-2 — Il riepilogo distingue i motivi**
- **Dato** un percorso con 300 esecuzioni: 210 aperte, 40 sospese per quota esaurita, 50 terminate di cui 35 per
  disiscrizione
- **Quando** l'utente apre la scheda del percorso
- **Allora** legge i conteggi per stato e, dentro ciascuno, la scomposizione per motivo con i numeri esatti

**CA-3 — I guasti si vedono**
- **Dato** un percorso in cui 12 invii sono falliti per errore del fornitore di consegna
- **Quando** l'utente apre il riepilogo
- **Allora** i 12 guasti sono contati a parte e mostrati in evidenza, distinti dalle uscite volute

**CA-4 — Dettaglio del passo**
- **Dato** un'esecuzione che ha saltato un passo per via della condizione semplice
- **Quando** si apre il dettaglio
- **Allora** il passo compare con esito «saltato per condizione» e il momento, non come se non fosse mai esistito

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con iscritti che hanno lo stesso indirizzo di posta
- **Quando** un utente di `A` cerca quell'indirizzo nel registro
- **Allora** vede solo le esecuzioni del proprio account, anche forzando l'identificativo dell'altro account nella
  richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sui conteggi per stato e motivo e di **integrazione** su elenco, filtri, dettaglio ed
      esportazione, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su elenco, ricerca per iscritto ed esportazione;
- [ ] **prova end-to-end**: nessun impatto sul percorso `[J-CAMPAIGNS]` (storia 0037), che copre la catena consenso
      → campagna → invio → disiscrizione; il registro delle esecuzioni è coperto dalle prove d'integrazione, e il
      motivo è scritto nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese le intestazioni del file esportato;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `automation_run_step`, campi annotati, tabella in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché i guasti si contano a parte;
- [ ] contratto degli **strumenti conversazionali**: `stato_iscritto` esteso ai percorsi automatici;
- [ ] controllo automatico di **accessibilità** verde sulla tabella delle esecuzioni;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0025` | Il percorso e i suoi passi |
| Storia `0026` | Le esecuzioni e il motivo d'ingresso |
| Storia `0027` | Gli stati e i motivi di uscita e sospensione, che qui si contano e si mostrano |
| Storia `0033` (parallela) | L'avvertenza sull'uso dei dati esportati è la stessa: si riusa, non si riscrive |

## 7. Fuori ambito

- il rapporto di rendimento di una campagna: è la storia 0030;
- il cruscotto della salute della lista: è la storia 0032;
- la modifica di un'esecuzione in corso — spostarla di un passo, farla ripartire: **esclusa per scelta**, perché
  significherebbe decidere a mano chi riceve cosa scavalcando le regole del percorso; l'unica azione ammessa è farla
  uscire (storia 0027);
- la diagnostica per l'amministrazione di piattaforma sull'arretrato delle lavorazioni: è nelle
  [estensioni della console di amministrazione](../estensioni-admin.md).

## 8. Punti aperti

- **Per quanto tenere le esecuzioni terminate.** La proposta del §6 della descrizione è «fino a conclusione più 12
  mesi». Dopo, l'esecuzione sparisce e con essa la risposta alla domanda «perché ha ricevuto questo messaggio?».
  Non è un termine di legge che io abbia trovato: chiude lo sviluppatore con la revisione legale.
