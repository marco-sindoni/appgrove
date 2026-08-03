# 0002 — Modello dati multi-account

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio lo schema `app_recensioni` con le tabelle di base e il filtro per account provato su ciascuna
> così da poter aggiungere le funzioni senza dover tornare indietro a sistemare le fondamenta dei dati.

**Contesto.** Il servizio esiste ma non ha dove mettere niente. Questa storia crea le tabelle che tutte le epiche
successive useranno e — più importante — **fissa le regole che non si potranno più cambiare a costo zero**: la
colonna `tenant_id` ovunque, le colonne di controllo, la cancellazione logica, e le due eccezioni motivate
(`regola_di_equita` e `richiesta_recensione`, che sono materiale di prova e non si modificano). Farlo adesso costa
una giornata; farlo dopo la terza epica costa una migrazione di dati.

## 2. Requisiti funzionali

1. **RF-1** — Esistono le tabelle `sede`, `collegamento_piattaforma`, `regola_di_equita`, `servizio_erogato`,
   `richiesta_recensione`, `modello_di_messaggio`, `recensione`, `risposta`, `segnalazione`,
   `punteggio_reputazione`, `riquadro_pubblico`, ciascuna con `tenant_id`, chiave primaria a identificativo
   universale versione 7, colonne di controllo e `deleted_at`.
2. **RF-2** — Le tabelle `regola_di_equita` e `richiesta_recensione` sono **ad accrescimento**: non ammettono
   aggiornamento dei campi che costituiscono prova (forma della regola, decorrenza; destinatario, momento, esito).
   Il vincolo è imposto dal database, non solo dal codice.
3. **RF-3** — Esistono gli indici che servono davvero: per `(tenant_id, sede_id)` su tutte le tabelle figlie, per
   `(tenant_id, piattaforma, identificativo_esterno)` unico su `recensione` (è ciò che impedisce i doppioni della
   storia 0009), per `(tenant_id, sede_id, erogato_il)` su `servizio_erogato`.
4. **RF-4** — Nessuna chiave esterna verso schemi diversi da `app_recensioni`: il riferimento al cliente
   dell'anagrafica condivisa è **logico** (un identificativo e una copia dei campi che servono), non relazionale.
5. **RF-5** — Il contratto dati dell'app (`RecensioniDataContract`) elenca già tutte le tabelle che conterranno
   dati di persone, anche quelle ancora vuote di funzionalità: `servizio_erogato`, `richiesta_recensione`,
   `recensione`, `risposta`, `segnalazione`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di ognuna di queste tabelle filtra per
  `tenant_id` preso dal token verificato. Il controllo strutturale che fa fallire la compilazione quando qualcuno
  aggira il filtro deve coprire il nuovo pacchetto.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova in questa storia oltre a quella già esistente:
  è una storia di modello dati. Gli oggetti di trasferimento restano al bordo; le entità non si espongono.
- **RT-3 — Persistenza (§8).** Migrazione `V2__modello_base.sql` sullo schema `app_recensioni`. Chiavi primarie a
  identificativo universale versione 7 generate dall'applicazione. Cancellazione logica con `deleted_at`; la
  cancellazione fisica esiste solo per i diritti dell'interessato e per la chiusura dell'account.
- **RT-4 — Modulo frontend (§3, §5).** Non applicabile.
- **RT-5 — Cinque lingue (§4).** Non applicabile: nessun testo visibile.
- **RT-6 — Varchi e quota (§6, §7).** Non applicabile: nessuna funzione che consuma quota. La colonna che il
  conteggio delle sedi userà (`sede.stato`) nasce qui.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento.
- **RT-8 — Dati personali (§10).** Le colonne che contengono dati di persone nascono annotate `@PersonalData` e
  **contemporaneamente** dichiarate nel manifesto in italiano e inglese: `servizio_erogato.nome`,
  `servizio_erogato.email`, `servizio_erogato.telefono`, `servizio_erogato.erogato_il`,
  `richiesta_recensione.destinazione`, `recensione.autore`, `recensione.testo`, `segnalazione.segnalante`. Un
  campo annotato e non dichiarato fa fallire la compilazione: è il presidio che vogliamo, non un ostacolo.
  ⚠️ `recensione.testo` porta con sé l'avviso sull'articolo 9 della descrizione §6: se la decisione dello
  sviluppatore fosse «non conservare il testo», questa storia cambia.
- **RT-9 — Registrazione eventi (§14).** Le migrazioni applicate sono registrate con `tenant_id` assente (sono di
  schema) e identificativo di correlazione; nessun dato personale nei registri.

## 4. Criteri di accettazione

**CA-1 — Le migrazioni girano su un database vuoto**
- **Dato** un database PostgreSQL 17 effimero
- **Quando** si esegue la suite di integrazione
- **Allora** le migrazioni Flyway si applicano nell'ordine e lo schema `app_recensioni` contiene le undici tabelle
  con le colonne di controllo

**CA-2 — Isolamento fra account su ogni tabella**
- **Dato** due account `A` e `B`, ciascuno con una riga in ognuna delle tabelle
- **Quando** un utente di `A` interroga qualunque risorsa
- **Allora** vede solo le righe di `A`, anche forzando l'identificativo dell'account `B` nella richiesta

**CA-3 — La prova non si riscrive**
- **Dato** una riga di `regola_di_equita` già scritta
- **Quando** si tenta di aggiornarne la forma o la decorrenza
- **Allora** il database rifiuta l'operazione e il servizio risponde con un errore in `problem+json` che spiega
  che la regola si sostituisce aggiungendone una nuova

**CA-4 — Nessun doppione di recensione**
- **Dato** una recensione già presente per una piattaforma e un identificativo esterno
- **Quando** si tenta di inserirne un'altra con gli stessi valori nello stesso account
- **Allora** l'inserimento è rifiutato dal vincolo di unicità

**CA-5 — Esportazione e cancellazione coprono tutto**
- **Dato** un account con dati in tutte le tabelle che contengono persone
- **Quando** si esegue l'esportazione e poi la cancellazione del contratto dati
- **Allora** l'esportazione contiene tutte e cinque le tabelle e dopo la cancellazione nessuna riga di persona
  resta, con una riga di prova nel registro delle purghe

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione degli identificativi e di **integrazione** sulle migrazioni, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su **ogni** tabella introdotta;
- [ ] **prova end-to-end**: *nessun impatto* — non c'è superficie utente;
- [ ] **traduzioni**: non applicabile;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le otto voci elencate in RT-8, campi annotati e
      tabelle presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la motivazione delle due tabelle ad accrescimento;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve il servizio, lo schema e il ruolo del database |
| decisione sull'articolo 9 (descrizione §6) | se il testo delle recensioni non si conserva, la tabella `recensione` cambia forma |

## 7. Fuori ambito

- le rotte di scrittura sulle tabelle: ognuna arriva con la storia che introduce la funzione;
- l'analisi dei temi ricorrenti, che potrebbe volere colonne aggiuntive — storia 0023, con la sua migrazione.

## 8. Punti aperti

- **Quanto del cliente si copia dall'anagrafica condivisa.** Copiare nome e recapito significa duplicare dati
  personali; non copiarli significa dipendere da un'altra app per mandare un messaggio. La mia inclinazione è
  copiare il minimo (nome e un solo recapito) al momento della registrazione del servizio, ma è una scelta con
  effetti sulla conformità: la chiude lo sviluppatore insieme al manifesto.
- La durata di conservazione proposta (24 mesi per i dati del cliente, 36 per la prova dell'invito) è una
  proposta della descrizione §6, non una decisione.
</content>
