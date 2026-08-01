# Come verificare a mano — Change 0072 (UC 0092)

Questa change **non aggiunge funzionalità di prodotto**: aggiunge quattro collaudi automatici e la leva che
permette di fermare e riavviare un servizio della suite. La verifica manuale serve quindi a due cose diverse:

- **A.** vedere coi propri occhi i comportamenti che i journey asseriscono (sono comportamenti già esistenti:
  se qualcosa non torna, il difetto è del prodotto, non del collaudo);
- **B.** provare che la nuova meccanica della suite si comporta bene, compreso il caso in cui la si interrompe
  a metà.

Ogni voce è **azione → risultato atteso**. Dove serve un controllo non visivo (database, chiamata, casella di
posta) è indicato esplicitamente.

---

## Preparazione

| # | Azione | Risultato atteso |
|---|---|---|
| P1 | `./app-start.sh` dalla radice del repo | Lo stack locale sale: Postgres, code, Mailpit, tutti i servizi scoperti, backoffice e console admin |
| P2 | `./dev.sh services` | La mappa mostra `auth`, `core`, `crm`, `fatture` con porta e schema |
| P3 | Apri la console admin e accedi come **admin@appgrove.test / Password1!** | Entri nella pagina iniziale «Overview» con quattro riquadri: Account, Utenti, Abbonamenti attivi, App disabilitate |
| P4 | Nella console, sezione **App**, riabilita il **Mini-CRM** se risulta disabilitato | Il badge di stato della riga diventa **Attiva/Active** |
| P5 | Registra un utente nuovo dal backoffice (`/signup`), verifica l'email da Mailpit (<http://localhost:8025>) e accetta i documenti legali al primo ingresso | Arrivi nella dashboard con la barra laterale visibile |

> Tieni aperte **due finestre separate** del browser (o una normale e una in navigazione privata): una per il
> cliente sul backoffice, una per l'amministratore sulla console. È il punto di tutta la verifica: le due
> sessioni non devono condividere nulla.

---

## A. I comportamenti asseriti dai journey

### A-CONSOLE — disabilitare un'applicazione, visto dal cliente

| # | Azione | Risultato atteso |
|---|---|---|
| 1 | Come cliente: acquista il **Mini-CRM Team** da «Ottieni un'app» (`/billing`), completa il finto pagamento | La barra laterale mostra il gruppo **Mini-CRM**; la pagina abbonamenti lo elenca come attivo |
| 2 | Come cliente: apri il Mini-CRM, assegna un posto a te stesso e crea un **contatto** con un nome riconoscibile | Il contatto compare nell'elenco |
| 3 | Come amministratore: **App** → riga Mini-CRM → **Disabilita** | Si apre un dialogo che spiega che l'azione è **reversibile**, che i dati restano intatti, e che **non** è la dismissione definitiva dell'app |
| 4 | Scrivi una motivazione (es. «prova manuale») e conferma | Il badge della riga diventa **Disabilitata/Disabled** — l'etichetta tradotta, non il valore grezzo `inactive` |
| 5 | Scorri sotto la tabella, al **registro delle disabilitazioni** | Una riga nuova, in cima: app, azione, operatore, data e la motivazione che hai scritto |
| 6 | Come cliente: **ricarica** la pagina | Il gruppo Mini-CRM **sparisce** dalla barra laterale; le altre app restano |
| 7 | Come cliente: vai a mano su `/app/crm` | Vieni portato su **/forbidden** (qui il diniego è corretto: il diritto è venuto meno davvero) |
| 8 | Come cliente: apri la pagina **Abbonamenti** | L'abbonamento è ancora elencato, con l'avviso «app sospesa dalla piattaforma»: dati e abbonamento restano validi |
| 9 | Controllo su database: `select count(*) from app_crm.contact where tenant_id = '<il tuo tenant>';` | Ritorna **1**: il contatto non è stato cancellato |
| 10 | Come amministratore: **Riabilita** il Mini-CRM e conferma | Badge di nuovo **Attiva**; nel registro una **seconda** riga |
| 11 | Come cliente: ricarica e riapri `/app/crm` | Il gruppo torna nella barra laterale e **il contatto di prima è ancora lì** |

Per ricavare il tuo `tenant_id`:
`docker exec -it $(docker ps --format '{{.Names}}' | grep -m1 postgres) psql -U appgrove -d appgrove -c "select tenant_id, email from platform.users order by created_at desc limit 5;"`

### A-GDPR — console Diritti GDPR fra due attori

| # | Azione | Risultato atteso |
|---|---|---|
| 12 | Come cliente: **Supporto** → tipo **Privacy**, oggetto e messaggio → «Apri ticket» | Si apre il dettaglio del ticket col tuo messaggio nel filo |
| 13 | Come cliente: **I miei dati** → «Avvia export» | Dopo qualche secondo compare «Export pronto» con il pulsante per scaricare l'archivio |
| 14 | Come amministratore: **Diritti GDPR** | Nella tabella delle richieste trovi **due righe** per il tuo account: una di tipo **Export** in stato `COMPLETED` con la data di completamento, una di tipo **Ticket privacy** |
| 15 | Apri il **dettaglio dell'export** dal collegamento «Dettaglio» | Vedi l'avanzamento per servizio e la chiave dell'archivio |
| 16 | Torna indietro, apri il **ticket** dall'elenco ticket, scrivi una risposta e invia | La risposta compare nel filo, marcata come «Admin» |
| 17 | Come cliente: **Supporto** → apri il ticket | **Vedi la risposta dell'amministratore** nel filo |
| 18 | Ricava l'identificativo dell'utente cliente: `select id from platform.users where email = '<email cliente>';` | Un UUID |
| 19 | Come amministratore: **Diritti GDPR** → sezione «Limitazione del trattamento (art. 18)» → bersaglio **Utente**, incolla l'UUID, aggiungi una nota → «Applica limitazione» → conferma | La limitazione compare nell'elenco di quelle attive, e in fondo il **registro delle limitazioni** mostra la riga `applied` con l'operatore |
| 20 | Come cliente: **ricarica** una pagina qualsiasi | Vieni portato alla pagina di **accesso** (la sessione non è più ripristinabile) |
| 21 | Come cliente: prova ad accedere con le stesse credenziali | L'accesso è **rifiutato** con un avviso. Nota consapevole: oggi il messaggio è quello generico delle credenziali non valide — è un punto aperto annotato in UC 0034, non un difetto di questa change |
| 22 | Come amministratore: **Rimuovi** la limitazione e conferma | Sparisce dall'elenco delle attive; nel registro compare una seconda riga `removed` |
| 23 | Come cliente: ricarica | **Rientri da solo**, senza riaccedere: il cookie di sessione non era stato cancellato, era il server a non onorarlo più |
| 24 | Controllo su database: `select action, actor from platform.gdpr_restriction_audit where target_id = '<UUID utente>' order by executed_at;` | Due righe: `applied` e `removed`, entrambe con l'identificativo dell'amministratore |

### A-ENTITLE — la matrice dice la verità

| # | Azione | Risultato atteso |
|---|---|---|
| 25 | Registra **due** utenti nuovi (chiamiamoli *pagante* e *gratuito*), ciascuno col proprio nome riconoscibile | Entrambi entrano nella dashboard |
| 26 | Col *pagante*: acquista il Mini-CRM Team | La barra laterale mostra Mini-CRM e Fatture |
| 27 | Col *gratuito*: non comprare nulla, guarda la barra laterale | Mostra comunque **Mini-CRM e Fatture**: entrambe offrono una fascia gratuita di base |
| 28 | Come amministratore: **Entitlements** | Per ciascuno dei due account trovi le righe delle app con «Diritto d'accesso = Sì». Il *pagante* ha uno stato di abbonamento (`active` o `trialing`) sulla riga Mini-CRM; il *gratuito* ha «—» sulla stessa riga ma **Sì** lo stesso — è la fascia gratuita, che prima della regola unica di accesso restava invisibile qui |
| 29 | Con un terzo utente nuovo: **I miei dati** → «Elimina questo account» → conferma | Compare «Eliminazione richiesta: l'account è disattivato» con la data di cancellazione definitiva |
| 30 | Col terzo utente: guarda la barra laterale (ricarica) | **Nessuna app**: l'account in eliminazione ha zero diritti |
| 31 | Come amministratore: **Entitlements**, cerca il terzo account | Le sue righe hanno «Diritto d'accesso = **No**» — la console e il cliente dicono la stessa cosa |
| 32 | Col terzo utente: «Annulla eliminazione» | L'account torna attivo e le app riappaiono dopo un ricaricamento |

> Nella matrice compaiono anche app che nella barra laterale non ci sono (**Notes**, **Teams**): sono
> app-fixture di catalogo presenti solo in profilo di sviluppo, senza modulo nel frontend. È corretto così.

### F-DEGRADE — un guasto vero non è un diniego

| # | Azione | Risultato atteso |
|---|---|---|
| 33 | Come cliente autenticato, con la barra laterale che mostra **Fatture** | Punto di partenza sano |
| 34 | Ferma il servizio di piattaforma dello stack locale: `kill $(lsof -ti tcp:8080 -sTCP:LISTEN)`, poi verifica con `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/q/health/ready` | Il core è giù (la risposta non è più `200`) |
| 35 | Come cliente: apri `/app/fatture` (ricaricando la pagina) | Nella barra laterale compare **«Non siamo riusciti a caricare le tue app»** con il collegamento **Riprova**. **Non** deve comparire «Nessuna app attiva» |
| 36 | Guarda l'area dei contenuti e la barra degli indirizzi | Un **errore**, non la pagina di accesso negato; l'indirizzo resta `/app/fatture`, **non** diventa `/forbidden` |
| 37 | Riavvia il core (`./app-start.sh`, che è idempotente) e attendi che `http://localhost:8080/q/health/ready` risponda `200` | Il servizio è di nuovo su |
| 38 | Come cliente: premi **Riprova** nella barra laterale, **senza ricaricare la pagina** | Le app riappaiono e il modulo Fatture si monta al posto dell'errore, sulla stessa pagina |
| 39 | Sospendi la riga utente del cliente: `update platform.users set status = 'suspended' where email = '<email cliente>';` | Una riga aggiornata |
| 40 | Come cliente: ricarica una pagina qualsiasi | Finisci alla pagina di **accesso**, una volta sola: nessun rimbalzo, nessuna schermata «Ripristino sessione» che resta appesa |
| 41 | Rimetti a posto: `update platform.users set status = 'active' where email = '<email cliente>';` e ricarica | Il cliente rientra |

---

## B. La meccanica nuova della suite

| # | Azione | Risultato atteso |
|---|---|---|
| 42 | `./run-tests.sh platform` | Tutti i journey verdi, **nessuno segnalato «flaky»**. La coda dell'esecuzione mostra l'ordine: prima i journey paralleli, poi `A-CONSOLE`, poi `F-DEGRADE`, infine `J-LEGAL` |
| 43 | Subito dopo: `lsof -nP -iTCP -sTCP:LISTEN \| grep -E ":(20080\|20081\|20082\|21100\|24173\|24174)"` | **Nessuna riga**: nessun servizio della suite è rimasto in ascolto |
| 44 | Rilancia `./run-tests.sh platform` una seconda volta, **senza pulire nulla in mezzo** | Di nuovo tutto verde: nessun collaudo dipende da un database vuoto |
| 45 | Durante un'esecuzione, interrompila con `Ctrl-C` mentre gira `F-DEGRADE`, poi controlla le porte come al punto 43 | Nessun residuo: la pulizia finale ferma i servizi anche in uscita anomala |
| 46 | Dopo un'esecuzione completa, come amministratore guarda lo stato del **Mini-CRM** in console | Risulta **Disabilitata**: è lo stato di riposo previsto dal listino, ripristinato dalla pulizia finale (riabilitalo se ti serve per lavorare) |
| 47 | Rilancia un solo journey: `tools/platform-e2e/run.sh --journey A-CONSOLE` | Gira **solo** quello, senza trascinarsi dietro la catena |
| 48 | Ispeziona il descrittore dei servizi generato: `cat tools/platform-e2e/.run/services.json \| python3 -m json.tool` | Un oggetto con una voce per servizio scoperto (`auth`, `core`, `crm`, `fatture`), ciascuna con porta, registro e variabili d'ambiente — nessun elenco scritto a mano |

---

## Cosa NON serve verificare

- Il comportamento del **ritorno sulla scheda del browser** per l'aggiornamento della barra laterale: non è
  osservato dai journey (in un browser senza finestre non è deterministico) ed è coperto dai test del frontend.
- I collaudi di livello 2 della console admin: restano come sono, con backend simulato.
