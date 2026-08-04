# 0031 — Prove di non-aggiramento dell'isolamento

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0019` — la ricezione dei segnali e la macchina a stati dell'intervento sono ciò che qui si mette alla prova
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma che ha messo in mano a un'app i fatti prodotti da tutte le altre
> voglio prove automatiche che rendano **impossibili** le tre cose che non devono accadere, invece di documenti che
> le dichiarano vietate
> così da poter dire a un cliente, e a chi ci controlla, che l'isolamento fra account e il passaggio umano non sono
> una promessa ma una proprietà verificata a ogni esecuzione della suite.

**Contesto.** RenewGrove è, con 20 InsightGrove, una delle due applicazioni della suite che **vivono di dati
altrui**, e la [descrizione](../application-description.md) al §4.2 dichiara una cosa che va presa alla lettera:
qui i fatti in arrivo sono riferiti a **un singolo cliente identificabile**, quindi non si può rivendicare il
contenimento del danno per costruzione che InsightGrove rivendica. Le contropartite sono le prove. La stessa
sezione elenca le tre scorciatoie vietate — leggere lo schema altrui, chiamare l'interfaccia di un'altra app,
copiarne le tabelle — e il §4.4 aggiunge la regola non negoziabile della macchina a stati: da `bozza` non si esce
senza una persona. Tre divieti, tre prove. È il momento giusto per farle ora perché solo adesso tutte e tre le
superfici esistono davvero.

## 2. Requisiti funzionali

1. **RF-1** — Una prova verifica che il servizio `fidelizzazione` **non apra alcuna connessione di rete verso
   un'altra applicazione della suite**: nessun client verso `/api/abbonati/*`, `/api/fatture/*` o qualunque altra
   rotta d'app. La verifica è **strutturale** (nessuna dipendenza né configurazione che punti a un altro servizio
   d'app) e **comportamentale** (durante un giro completo di prove, nessuna chiamata in uscita verso quegli
   indirizzi). Introdurne una fa diventare rossa la suite.
2. **RF-2** — Una prova verifica che il **ruolo del database** del servizio non abbia privilegi fuori dal proprio
   schema: un tentativo di leggere una tabella di un altro schema applicativo fallisce con un errore di permesso, e
   la prova lo asserisce. È un permesso che non esiste, non una convenzione da rispettare
   ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §8).
3. **RF-3** — Una prova verifica che **i due percorsi non condividano codice**: il `tenant_id` di una richiesta web
   non è raggiungibile dal consumatore di eventi, e il `tenant_id` copiato da un evento non è raggiungibile dallo
   strato che serve le richieste. Verifica **strutturale** sui pacchetti (nessun tipo del contesto di richiesta
   importato dal consumatore, e viceversa) e **di comportamento**: con un utente di `A` connesso, un segnale
   pubblicato con il `tenant_id` di `B` finisce **sotto `B`**, e viceversa nulla di `B` diventa leggibile ad `A`.
4. **RF-4** — Una prova verifica che **nessun effetto verso l'esterno parta senza il passaggio umano**: non esiste
   alcuna via — rotta, consumatore di eventi, processo periodico, strumento conversazionale, impostazione — che
   porti un intervento da `bozza` a `confermato` senza un identificativo utente; il tentativo per via di
   programmazione fallisce e nulla viene consegnato. La prova enumera **tutte** le transizioni ammesse della
   macchina a stati del §4.4 e fallisce se ne compare una nuova non dichiarata.
5. **RF-5** — Una prova verifica che un segnale **senza `tenant_id`**, o con un `tenant_id` sconosciuto alla
   piattaforma, sia **scartato** con la ragione registrata, e che nessuna riga venga scritta.
6. **RF-6** — Una prova a matrice verifica ruoli e abilitazione su tutte le superfici introdotte dall'app:
   `member` in **sola lettura sulle fonti** (`collega_fonte`/`scollega_fonte` → `403`), abbonamento `canceled` →
   `402` sulle funzioni di business, quota di `rapporti_sorvegliati` esaurita → `429` con il rimedio indicato, e
   **esportazione dei dati sempre accessibile**, anche con app disabilitata o abbonamento cessato, perché è un
   diritto e non una funzione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** È l'oggetto stesso della storia: ogni prova usa **almeno due** account e
  verifica anche il tentativo di forzare `tenant_id` dal corpo della richiesta o dai parametri, che deve essere
  ignorato. La suite di isolamento non è mai esclusa dai filtri di percorso.
- **RT-2 — Nessuna chiamata fra app (§2).** L'unica via fra servizi è **asincrona a eventi**. Il **RF-1** è la
  prova di questo invariante, e va scritta in modo che un client aggiunto per comodità la faccia fallire subito,
  non fra sei mesi.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. La prova del **RF-2** gira su database effimero con
  migrazioni vere e con **i ruoli reali**, non con un utente amministrativo: una prova eseguita da un
  superutente dimostrerebbe l'opposto di ciò che deve dimostrare.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. La matrice del **RF-6** verifica però che i rifiuti
  arrivino a schermo come **messaggi comprensibili** con il rimedio, non come errori grezzi: solo token del sistema
  di design, tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I messaggi di rifiuto `402`, `403` e `429` passano dallo spazio-nomi
  `fidelizzazione` e sono presenti in `en, it, fr, es, de`: un blocco che il cliente non capisce vale come un
  guasto.
- **RT-6 — Varchi e quota (§6, §7).** La matrice del **RF-6** copre l'intera catena dei varchi per questa app, con
  la particolarità della metrica a giacenza: a tetto raggiunto la nascita di un rapporto sorvegliato è respinta con
  `429`, e il rimedio dice «togli N rapporti dalla sorveglianza» (§3 della descrizione).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Le prove del **RF-4** e del **RF-6** si
  applicano **anche** alle superfici degli strumenti dichiarati nelle storie `0028` e `0029`, chiamandole
  direttamente: il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063), ma il contratto
  esiste già e va sorvegliato.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: la storia aggiunge prove, non campi. Le prove
  usano dati inventati e verificano, come effetto collaterale utile, che nessun rifiuto e nessun registro contenga
  etichette di rapporti.
- **RT-9 — Registrazione eventi (§14).** `segnale scartato (ragione)` e `transizione rifiutata (stato, ragione)`
  sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali; le
  prove verificano che la ragione ci sia, perché uno scarto silenzioso è indistinguibile da un dato mai arrivato.
- **RT-10 — Prove (§11).** Livelli usati: **strutturale** (dipendenze e pacchetti), **integrazione** con
  Testcontainers e ruoli reali, **end-to-end** per la sola matrice del **RF-6**, che è l'unica parte che si vede da
  fuori.

## 4. Criteri di accettazione

**CA-1 — Nessuna chiamata verso un'altra app**
- **Dato** il servizio `fidelizzazione` e un giro completo di prove
- **Quando** si esegue la verifica strutturale e quella comportamentale
- **Allora** non risulta alcuna dipendenza né alcuna chiamata verso le rotte di un'altra applicazione; aggiungendo
  un client verso `/api/abbonati/v1/…`, la suite diventa rossa indicando il punto

**CA-2 — Nessuna lettura fra schemi**
- **Dato** il database effimero con i ruoli reali
- **Quando** il servizio tenta di leggere una tabella di un altro schema applicativo
- **Allora** l'operazione fallisce con un errore di permesso del database, e la prova lo asserisce

**CA-3 — I due percorsi non si toccano**
- **Dato** un utente dell'account `A` connesso e un segnale pubblicato con il `tenant_id` dell'account `B`
- **Quando** il consumatore lo elabora e l'utente di `A` ricarica i propri elenchi
- **Allora** il segnale è scritto sotto `B`, `A` non lo vede, e la verifica strutturale conferma che nessun tipo del
  contesto di richiesta è raggiungibile dal consumatore

**CA-4 — Da bozza non si esce senza una persona, per nessuna via**
- **Dato** un intervento in `bozza`
- **Quando** si tenta di portarlo a `confermato` da rotta, da consumatore di eventi, da processo periodico e da
  strumento conversazionale, sempre senza identificativo utente
- **Allora** tutti e quattro i tentativi falliscono, l'intervento resta in `bozza`, nulla è consegnato, e
  l'enumerazione delle transizioni ammesse coincide con quella del §4.4 della descrizione

**CA-5 — Segnale senza account**
- **Dato** un evento senza `tenant_id` e un evento con un `tenant_id` sconosciuto
- **Quando** il consumatore li elabora
- **Allora** entrambi sono scartati con la ragione registrata, e nessuna riga è scritta in alcuno schema

**CA-6 — Matrice ruoli e abilitazione**
- **Dato** un `member`, un account con abbonamento `canceled` e un account al tetto di `rapporti_sorvegliati`
- **Quando** il `member` tenta di collegare una fonte, l'account cessato apre gli elenchi, e al terzo arriva un
  segnale su un soggetto nuovo
- **Allora** si ottengono rispettivamente `403`, `402` e `429` con messaggi comprensibili e il rimedio; e in tutti e
  tre i casi l'**esportazione dei dati** resta accessibile

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e i percorsi di piattaforma; l'intera suite
      prima del commit);
- [ ] prove **strutturali** (dipendenze fra servizi, separazione dei pacchetti, enumerazione delle transizioni) che
      falliscono quando qualcuno introduce la scorciatoia;
- [ ] prove di **integrazione** con database effimero, migrazioni vere e **ruoli reali** del database;
- [ ] prova di **isolamento fra account** su ogni superficie introdotta dall'app, comprese quelle degli strumenti;
- [ ] **prova end-to-end**: *coprire ora, in estensione* — la sola matrice del **RF-6** estende il percorso
      `[J-FIDELIZZAZIONE]` della storia `0030` con i casi `402`, `403`, `429` e l'esportazione sempre accessibile; il
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato di
      conseguenza. Le prove **(a)**, **(b)** e **(c)** restano di integrazione e strutturali, perché non si vedono
      da fuori;
- [ ] **traduzioni** dei messaggi `402`, `403`, `429` in `en, it, fr, es, de`;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato: che cosa esattamente rende rossa la suite per ciascuno dei tre
      divieti, e perché la matrice dei ruoli sta nel percorso mentre gli altri no;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, ma le prove si applicano anche alle sue
      superfici;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la descrizione elenca le tre scorciatoie vietate (§4.2).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` (ricezione e scrittura dei segnali) | è là che il `tenant_id` si copia dall'evento e che i due percorsi vengono separati: qui si prova che la separazione tiene |
| storia `0019` (intervento con conferma umana) | è là che nasce la macchina a stati: qui si prova che non ha vie laterali |
| storie `0004`, `0008`, `0029` | il varco della quota, il collegamento delle fonti e gli strumenti sono le superfici su cui gira la matrice |
| storia `0032` (chiusura del contratto dati) | l'esportazione «sempre accessibile» della matrice è la funzione che quella storia completa: qui si prova che nessun varco le sta davanti |

## 7. Fuori ambito

- la **lacuna dichiarata** del §4.2: se una fonte pubblicasse un `tenant_id` sbagliato, `fidelizzazione`
  scriverebbe sotto l'account sbagliato senza potersene accorgere. È un difetto **della fonte**, coperto dalle prove
  di isolamento della fonte, e qui non è risolvibile — nasconderlo sarebbe peggio che scriverlo;
- l'**analisi dinamica della sicurezza** dell'applicazione (tentativi di intrusione, prove di penetrazione): è di
  piattaforma;
- la verifica che le **altre applicazioni** rispettino il contratto del segnale: è loro, e il presidio da questo lato
  è il validatore in ingresso della storia `0006`;
- la **sicurezza e il tracciamento** delle sessioni conversazionali (UC 0065): di piattaforma;
- la completezza di esportazione e cancellazione: storia `0032`. Qui si prova solo che nessun varco impedisca
  l'accesso al diritto.

## 8. Punti aperti

- **Fin dove spingere la prova comportamentale del RF-1.** Osservare le connessioni in uscita durante l'intera
  suite è robusto ma lento; limitarsi alla verifica strutturale è veloce e lascia scoperta la chiamata costruita a
  runtime da una configurazione. La proposta è tenerle entrambe, con la comportamentale su un sottoinsieme di
  prove. Chiude: **sviluppatore**, in fase di implementazione.
- **Se l'enumerazione delle transizioni ammesse debba vivere nel codice o nel documento.** Nel codice si rompe da
  sola quando qualcuno ne aggiunge una; nel documento resta leggibile da chi non compila il progetto. La proposta è
  nel codice, con il diagramma del §4.4 della [descrizione](../application-description.md) come testo di
  riferimento. Chiude: **sviluppatore**.
