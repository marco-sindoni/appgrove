# Come verificare a mano la change 0070 (batteria journey e2e lato utente)

Questa change **non cambia il prodotto**: aggiunge sette percorsi di collaudo automatici che navigano
l'applicazione come farebbe una persona. La verifica manuale serve quindi a due cose: (1) vedere coi propri
occhi che i percorsi collaudati esistono davvero e si comportano come descritto, (2) controllare che gli
strumenti della suite (avvio, diagnosi, rilancio mirato) funzionino.

Tempo indicativo: 30–40 minuti per il giro completo; il solo blocco A ne richiede 10.

---

## Blocco A — La suite gira e racconta cosa copre

| # | Azione | Risultato atteso |
|---|---|---|
| A1 | Da radice del repository: `./run-tests.sh platform` | La corsa passa per: container (Postgres, ElasticMQ, Mailpit, **MinIO**), migrazioni+seed, avvio dei servizi su porte +12000, build delle SPA, esecuzione dei journey. Finisce con **9 test verdi** e la riga `✓ suite di piattaforma: verde`. Nessun test segnalato come `flaky`. |
| A2 | Guardare l'elenco dei test stampati | Compaiono gli otto identificativi: `J-REG` (due test), `J-BUY`, `J-QUOTA`, `J-MEMBERS`, `J-SUB`, `J-PWD`, `J-PRIVACY`, `J-LEGAL`. `J-LEGAL` è l'**ultimo** a partire (gira da solo, dopo gli altri). |
| A3 | Rilanciare subito, senza pulire nulla: `./run-tests.sh platform` | Di nuovo verde: la doppia esecuzione consecutiva non richiede alcuna pulizia manuale (email e tenant sono unici per corsa). |
| A4 | Rilancio mirato: `tools/platform-e2e/run.sh --journey J-BUY` | Viene eseguito **solo** J-BUY (nessun altro journey riparte per "dipendenza"). |
| A5 | Aprire `tools/platform-e2e/README.md` | C'è la tabella "I journey": ogni identificativo con la funzionalità coperta e le note (app usata, journey serializzato). |

---

## Blocco B — Percorsi visivi: rifare a mano ciò che i journey collaudano

Avviare lo stack di sviluppo con `./app-start.sh` e aprire il backoffice. Servono un paio di **account nuovi**
(la registrazione è quella vera: l'email di verifica arriva su Mailpit, <http://localhost:8025>).

**Nota sul catalogo**: nello stack di sviluppo il Mini-CRM è normalmente **spento** (scelta di prodotto). La
suite lo accende da sola per la durata della corsa. Per rifare a mano i passi B2–B5, accenderlo una volta
entrando come amministratore di piattaforma (`admin@appgrove.test`, password `Password1!`) e portando l'app
`crm` a `active` dalla console di amministrazione; a fine prova si può rimetterla spenta (il riavvio dello
stack la riallinea comunque al listino).

| # | Azione | Risultato atteso |
|---|---|---|
| B1 | Registrare un account nuovo, verificare l'email da Mailpit, completare l'onboarding | Al primo ingresso compare la **finestra bloccante dei documenti legali**: spuntando i consensi si entra nella dashboard. Nella barra laterale, sotto "Your apps", compare **Invoices** (piano gratuito) e nient'altro. |
| B2 | Andare su **Billing** → card **Mini-CRM** → *Subscribe* | Si apre la scelta del piano: il piano **Free** ha il bottone disattivato, il piano **Team** è acquistabile e mostra "14-day free trial". |
| B3 | Acquistare **Mini-CRM Team** | Compare "Activating your subscription…", poi **"All set! Your subscription is active."** e il bottone *Open app*. Cliccandolo si apre il modulo **Contatti**; nella barra laterale è comparso **Mini-CRM**. |
| B4 | Aprire **Invoices** e creare fatture fino a superare le 10 del mese | Il banner in cima alla lista avanza (`1 / 10`, `2 / 10`, …). A 10 mostra "You have reached your plan's monthly limit." Alla **undicesima** creazione compare l'avviso "Monthly limit reached: upgrade to create more invoices." e, in alto, la fascia **"Plan limit reached"** con il bottone *Upgrade*. |
| B5 | Da **Mini-CRM → Membri**, assegnare posti finché si esauriscono (piano Free: 2) | Il contatore mostra `1 / 2`, `2 / 2`; al terzo tentativo compare "Posti esauriti…" con l'invito a passare a un piano superiore. Dopo l'acquisto del piano Team il contatore diventa `… / 10` e l'assegnazione riesce. |
| B6 | Da **Members**: invitare un collega con un'altra email | Compare "Invitation sent to …" e la riga tra gli inviti pendenti. **In Mailpit** arriva l'email *"You have been invited to appgrove"*. |
| B7 | Aprire il link dell'invito **in una finestra separata** (o in incognito), impostare la password | Si entra come **membro** dello stesso spazio di lavoro, attraversando la finestra dei legali. |
| B8 | Tornati come titolare: cambiare il ruolo del membro, poi rimuoverlo | Il ruolo si aggiorna nella tabella; dopo la rimozione la riga sparisce e, **ricaricando la finestra del membro**, questi viene rimandato alla pagina di accesso. |
| B9 | Sempre in **Members**, guardare la riga del titolare | Non c'è alcun menu a tendina per cambiargli ruolo e il bottone *Remove* è **disattivato** (protezione dell'ultimo titolare). |
| B10 | In **Billing**, sul pannello "Your subscriptions": *Change plan* → piano inferiore, poi *Cancel* | Compare "Downgrade scheduled to …" e poi "Cancellation scheduled: access until …". |
| B11 | Andare su **Security** e attivare la verifica in due passaggi | Compare il QR con il segreto; inserendo il codice dell'app di autenticazione si legge "Two-factor authentication is enabled." Uscendo e rientrando, dopo la password viene chiesto il **codice a 6 cifre**. |
| B12 | Uscire, *Forgot your password?*, inserire la propria email | Messaggio neutro "If an account exists for that email, a reset link is on its way." In **Mailpit** arriva *"Reset your password"*; il link porta a "Choose a new password" e la nuova password funziona (la vecchia no). |
| B13 | Andare su **My data**: cambiare il nome, poi *Start export* | Il nome si salva ("Saved"). L'esportazione passa da "in corso" a **"Export ready."** con *Download archive* e la scritta "Link valid until …". |
| B14 | Scaricare l'archivio e aprirlo | È uno **zip** con dentro `platform.json` e `crm.json`: contengono i dati del **proprio** account (email, nome rettificato, i contatti creati) e **nessun dato di altri account**. |
| B15 | Sempre in **My data**: *Export app data* per Mini-CRM, poi *Confirm withdrawal* | Dopo la conferma compare "Withdrawal completed…". Riaprendo Mini-CRM i contatti non ci sono più. |
| B16 | *Delete this account* → conferma | "Deletion requested: the account is deactivated." con la data di cancellazione definitiva. Il bottone *Cancel deletion* riporta l'account attivo. |

---

## Blocco C — Ri-accettazione dei documenti legali (percorso J-LEGAL)

| # | Azione | Risultato atteso |
|---|---|---|
| C1 | Con un account già "in regola" (ha accettato al primo ingresso), simulare la pubblicazione di una nuova versione maggiore dei Termini: `docker exec -i $(docker ps --format '{{.Names}}' \| grep -m1 postgres) psql -U appgrove -d appgrove -c "update platform.legal_version set major = major + 1, version = '99.0.0' where component = 'terms'"` | Nessun errore dal comando. |
| C2 | Ricaricare il backoffice | Compare la **finestra bloccante** "Updated legal documents" con **un solo** documento (i Termini, versione 99.0.0); il bottone *Continue* è disattivato finché non si spunta. Dietro non si vede l'applicazione. |
| C3 | Con la finestra pendente, andare a mano su `/privacy` | La pagina **"My data" si apre**: i diritti sui dati restano esercitabili anche col blocco attivo. |
| C4 | Tornare alla home, leggere il documento e accettare | Si entra normalmente. |
| C5 | Controllare la registrazione della prova: `… psql -U appgrove -d appgrove -c "select tenant_id, user_id, component, version, act_type, accepted_at from platform.legal_acceptance order by accepted_at desc limit 3"` | La riga più recente è del **proprio** account/utente, componente `terms`, versione `99.0.0`, tipo `accept`, con data e ora. |
| C6 | Riavviare i servizi (`./app-stop.sh && ./app-start.sh`) | La versione dei Termini torna quella dei contenuti reali: la sincronizzazione all'avvio riallinea il database (nessuna pulizia manuale necessaria). |

---

## Blocco D — Diagnosi dopo un rosso (da provare almeno una volta)

| # | Azione | Risultato atteso |
|---|---|---|
| D1 | Rompere di proposito un'attesa in un journey (per esempio cambiare un testo atteso in `tools/platform-e2e/journeys/J-BUY.spec.ts`) e lanciare `tools/platform-e2e/run.sh --journey J-BUY` | Il verdetto è rosso e **dice quale journey** è caduto e su quale attesa; in `tools/platform-e2e/test-results/` ci sono screenshot, video e traccia. |
| D2 | Aprire la traccia: `npx playwright show-trace tools/platform-e2e/test-results/<cartella>/trace.zip` | Si rivede il percorso passo per passo nel browser. |
| D3 | Guardare i log dei servizi in `tools/platform-e2e/.run/*.log` e la casella su <http://localhost:8025> | I log della corsa ci sono; in Mailpit si vedono le email generate dai journey (verifica, invito, reset) verso indirizzi `@test.appgrove.local`. |
| D4 | Ripristinare la modifica e rilanciare | Torna verde. |
