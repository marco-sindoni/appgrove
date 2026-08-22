# Change 0096: «Members» come elenco unico di persone, senza ruolo

**Branch**: `change/0096-use-case-0100-sezione-members`
**Aree**: `frontend/` (backoffice, i18n, api-client), `services/core`
**Data**: 2026-08-22
**Autore**: Platform Engineering (modalità fast)
**Use case sorgente**: [`docs/usecases/22-refactor-membership-model/story/0100-sezione-members-elenco-unico.md`](../../docs/usecases/22-refactor-membership-model/story/0100-sezione-members-elenco-unico.md)
**Tocca dati personali?**: Sì, ma **nessun trattamento nuovo** — indirizzo e nome delle persone dell'account
sono già dichiarati (UC 0013) e già mostrati da questa schermata; la data di ingresso e il conteggio delle
applicazioni sono dati di appartenenza e di autorizzazione già presenti nel modello (UC 0098/0116). Cambia la
**presentazione**, non le categorie né le finalità: classificazione **MINOR**, nessun aggiornamento del
manifesto dati né della versione dei documenti legali.

## Problema / Obiettivo

La schermata «Members» del backoffice è ancora costruita sul modello vecchio, quello in cui il potere di una
persona era un attributo della persona: mostra una **colonna del ruolo** e tiene **due tabelle separate** —
le persone dell'account e gli inviti in attesa. Due elenchi della stessa cosa: chi guarda deve sommare a
mente per sapere quante persone ha nel proprio gruppo di lavoro.

Dopo UC 0098 il ruolo di piattaforma ha due soli valori e il potere vero sta **su ciascuna applicazione**.
La schermata deve diventare quello che è: il **registro delle persone** dell'account — chi c'è, in che stato
è, su quante applicazioni è abilitata, da quando fa parte del gruppo. Nessun ruolo, perché a questo livello
il ruolo non esiste.

In più, la gestione delle persone è ancora aperta a «owner **oppure** admin» sia nella guardia della rotta
sia nell'interfaccia del core: `admin` non è più un ruolo di piattaforma, e la sezione va riservata
all'**owner**, con la difesa vera nel servizio e non nella guardia del frontend.

Si vede fatto quando: l'owner apre «Members» e legge **una sola tabella** con tutte le persone —
comprese quelle con invito in attesa — senza alcuna colonna di ruolo, con lo stato, il numero di
applicazioni su cui ciascuna è abilitata (con il dettaglio a richiesta) e la data di ingresso; il modulo di
invito chiede **solo** l'indirizzo; e una persona che non è owner riceve un rifiuto dal servizio, non una
schermata a metà.

## Scope

### `services/core`

1. **Gestione riservata all'owner.** Le operazioni sull'elenco delle persone dell'account (elenco, lettura
   per identificativo, modifica, uscita) e quelle sugli inviti dell'account (invio, elenco, revoca) sono
   ammesse al **solo owner**. Le operazioni su di sé (`/users/me`, sia lettura sia rettifica del nome)
   restano aperte a chiunque appartenga a un account: sono i propri dati.
2. **Il ruolo esce dal contratto dell'invito.** Il corpo dell'invio non porta più un ruolo, e nemmeno la
   vista dell'invito che l'account rilegge: chi entra entra come persona dell'account, e i poteri si
   concedono dopo, una applicazione alla volta. Un ruolo eventualmente inviato da un chiamante vecchio
   **non può concedere nulla**: l'invito nasce sempre come `member`.
3. **Il conteggio delle applicazioni per persona.** La lettura dell'elenco porta, per ciascuna persona, le
   applicazioni su cui è abilitata con il ruolo che vi ha — informazione in **sola lettura**, sufficiente sia
   per il numero in colonna sia per il dettaglio a richiesta. Per l'**owner** l'insieme sono tutte le
   applicazioni a cui l'account ha diritto, perché il suo accesso è implicito e non ha righe di permesso.
   Vincolo di forma: il costo della lettura **non deve crescere con il numero di persone** — nessuna
   interrogazione per riga.
4. **La data di ingresso.** La lettura dell'elenco porta la data in cui l'appartenenza è nata.

### `frontend/`

5. **Un elenco unico.** Una sola tabella con le persone dell'account e gli inviti in attesa, ordinata con
   l'owner in testa. Colonne: indirizzo, nome, **stato**, **applicazioni**, **data di ingresso**, azioni.
   Nessuna colonna di ruolo, in nessuna delle due parti dell'elenco.
6. **Stato leggibile.** Tre stati oggi rappresentabili: attiva, **invito in attesa** (con la data di
   scadenza dell'invito accanto, perché un invito ha una vita finita), sospesa.
7. **La colonna delle applicazioni** mostra il numero, e su richiesta il dettaglio — quali applicazioni e
   con quale ruolo — in **sola lettura**, con la frase che dice dove si cambia (dentro la gestione utenti
   dell'applicazione). Chi non è abilitato a nulla si legge come «nessuna applicazione», che è uno stato
   legittimo e non un errore.
8. **L'invito non chiede il ruolo.** Solo l'indirizzo, con una riga che **spiega** l'assenza del selettore:
   chi conosce la schermata attuale penserà altrimenti a un difetto.
9. **La rotta è dell'owner.** La guardia della rotta `/members` passa da «owner oppure admin» a «owner».
   Coerentemente, le letture di persone e inviti si eseguono solo quando chi è in sessione è owner — anche
   dove le stesse letture alimentano altre schermate (i due numeri del cruscotto): a chi non le può leggere
   non si mostra una riga rotta, si omette la riga.
10. **Cinque lingue complete**, senza chiavi orfane: le chiavi del ruolo che nessuno usa più escono, quelle
    nuove entrano in tutte e cinque.

### Collaudi

11. Collaudi di integrazione nel core: una persona che non è owner riceve un rifiuto su persone e inviti;
    un ruolo inviato nel corpo dell'invito non concede nulla; il conteggio e il dettaglio delle
    applicazioni sono quelli attesi per l'owner e per una persona abilitata.
12. Collaudi di componente sull'elenco unico e collaudo di unità sulla funzione **pura** che fonde le due
    letture (persone e inviti) in un solo elenco.
13. Percorso end-to-end di livello 2 esistente (`L2-MEMBERS`) **riscritto** sul flusso nuovo, e registro di
    copertura aggiornato: UC 0100 esce dalle esenzioni ed entra fra gli use case con superficie.

## Fuori scope

- **Il riquadro dei posti** (posti usati su totali, costo del posto successivo) → UC 0103. Nella pagina si
  lascia il posto dove andrà, così che non serva rimaneggiare la struttura una seconda volta.
- **La riduzione dei posti in attesa** e lo stato «in cessazione» → UC 0104. Oggi lo stato non è nemmeno
  rappresentabile nel modello (l'appartenenza ha due stati: attiva, sospesa): non si aggiunge una etichetta
  che nessun dato può produrre. Rimando scritto nella storia 0104.
- **La gestione degli accessi per applicazione** (concedere, cambiare ruolo, revocare) → UC 0111. Da qui si
  legge, non si scrive; e il **collegamento** alla schermata di UC 0111 non si può fare perché quella
  schermata non esiste ancora: il dettaglio si apre sul posto. Rimando scritto nella storia 0111.
- **La visibilità della voce di menu** per chi non è owner → UC 0107. Questa change stringe la rotta, non
  ridisegna il menu.
- **Distinguere a schermo una sospensione per limitazione del trattamento** (art. 18) da una sospensione
  amministrativa → già rimandata dalla storia a UC 0033, e oggi impossibile: il contratto riporta un solo
  valore «sospesa» per entrambe.
- **La colonna `role` della tabella degli inviti in banca dati** resta: è obbligatoria e serve
  all'accettazione. La sua rimozione appartiene alla conversione di UC 0113.
- **Il testo dell'email di invito** («sei stato invitato … come *member*») non cambia: `member` è ancora uno
  dei due valori vivi del ruolo di piattaforma, quindi la frase è vera e non è un residuo di qualcosa che è
  stato ritirato.

## Criteri di accettazione

- [ ] La schermata «Members» è **una** tabella che contiene sia le persone attive/sospese sia gli inviti in
      attesa, con l'owner in testa, e **nessuna** colonna o etichetta di ruolo di piattaforma.
- [ ] Per ogni riga si legge lo stato, il numero di applicazioni (con dettaglio a richiesta: quali e con che
      ruolo, in sola lettura) e la data di ingresso; una persona senza applicazioni si legge come «nessuna
      applicazione».
- [ ] Il modulo di invito chiede **solo** l'indirizzo, con la riga che spiega perché non c'è il ruolo; le due
      collisioni lecite dell'invito (già membro · già invitato) continuano a dare due messaggi distinti.
- [ ] Le operazioni del core su persone e inviti rispondono **403** a chi non è owner, e un `role` nel corpo
      dell'invito non concede nulla (l'invito nasce `member`).
- [ ] La rotta `/members` è raggiungibile dal solo owner; le letture di persone e inviti non partono per chi
      non è owner.
- [ ] Le cinque lingue sono complete e senza chiavi orfane (collaudo di parità verde).
- [ ] `L2-MEMBERS` è riscritto sul flusso nuovo ed è verde; il registro di copertura end-to-end classifica
      UC 0100 fra gli use case con superficie e il controllo `tools/e2e-coverage` è verde.
- [ ] `./run-tests.sh` (suite completa) verde.

## Invarianti appgrove toccati

- **Account solo dal token verificato**: le letture nuove (accessi per applicazione, diritti dell'account,
  data di ingresso) passano dalle entità già tenant-scoped e dal read-model degli entitlement, che leggono
  l'account dal token. Nessun identificativo di account arriva dal corpo o dai parametri.
- **Filtro riga per riga**: le entità di appartenenza e di accesso portano il discriminatore di account
  (`@TenantId`), quindi il filtro `WHERE tenant_id = ?` è automatico su ogni lettura, comprese quelle
  aggiunte qui. Non si scrive alcuna interrogazione trasversale agli account.
- **Modulo Terraform `microsaas_app`**: non toccato (nessuna applicazione nuova).
- **Logging strutturato**: i log del core continuano a portare account, applicazione e persona dal contesto
  già in essere; questa change non aggiunge percorsi che registrino dati personali.

## Requisiti di test

- **Rifiuto per ruolo** (integrazione, core): il collaudo che oggi prova che un `admin` **può** invitare va
  **sostituito** da quello che prova che non può — cancellarlo senza rimpiazzo lascerebbe tornare il varco
  senza che nulla diventi rosso.
- **Nessuna elevazione dal corpo dell'invito**: il collaudo che oggi prova il rifiuto del ruolo `owner`
  nell'invito va sostituito da quello che prova che un ruolo inviato è **ignorato** e l'invito nasce
  `member`.
- **Costo della lettura**: il conteggio delle applicazioni si prova su un account con più persone e più
  righe di accesso, per non lasciare passare l'implementazione che interroga una volta per riga.
- **Assenza del ruolo a schermo**: il collaudo di componente pretende l'**assenza** della colonna e di
  qualunque etichetta di ruolo di piattaforma, non solo la presenza delle colonne nuove.
- **Funzione di fusione**: provata da sola (ordinamento, stati, conteggi, righe bloccate) fuori dal
  componente.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | **Sì** (contratto): il corpo dell'invito non porta più il ruolo e la vista dell'invito non lo espone; la gestione di persone e inviti si stringe all'owner. Consumatore unico è il backoffice, aggiornato nello stesso commit. Un token già emesso che porta `admin` e non `owner` perde la gestione dei membri: è il punto della storia. |
| Contratto cross-area | **Sì** — frontend ↔ `services/core` (spec OpenAPI rigenerato e tipi del client rigenerati nello stesso commit) |
| Version bump | minor |
