# UC 0113 — Migrazione degli account esistenti e copertura end-to-end per ruolo

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.4 Dentro le applicazioni](../epic/E22-04-app-e-industrializzazione.md)
**Dipendenze**: tutte le storie precedenti dell'epica (0098–0112, 0114, 0115), UC 0090 (fondamenta della suite di piattaforma), UC 0093 (registro di copertura)
**Piano di lavoro**: [task/0113](../task/0113-migrazione-account-e-copertura-e2e.md)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Portare il mondo esistente nel modello nuovo **senza che nessuno perda accesso**, e provare l'intero
impianto con percorsi automatici che entrano nel prodotto come `owner`, `admin`, `editor` e `viewer`.

**Incluso**: la conversione dei dati; il piano di rilascio con i token già emessi; la rilevazione degli
account che superano la franchigia e come trattarli; il percorso di piattaforma «stessa applicazione vista
dai quattro ruoli»; l'aggiornamento del registro di copertura per tutte le storie dell'epica.

**Escluso**: le funzioni in sé, che appartengono alle storie precedenti.

## 2. Attori & ruoli

- **Chi rilascia** (Platform Engineer): esegue la conversione e verifica gli esiti.
- **Sistema**: applica la conversione e sopporta i token vecchi per il tempo necessario.
- **Chi decide i prezzi** (lo sviluppatore): decide come trattare gli account oltre la franchigia.

## 3. Precondizioni

- Tutte le storie precedenti dell'epica (0098–0112, 0114, 0115) sono in `main`.
- L'ambiente di prova è stato usato per una prova completa della conversione, con dati simili a quelli reali.

## 4. Flusso principale — la conversione

Una migrazione di banca dati, in questo ordine:

1. **Accessi**: per ogni account, per ogni utente non-owner, per ogni applicazione a cui l'account ha
   diritto, si crea una riga di accesso con il ruolo tradotto:

   | Ruolo di piattaforma di oggi | Diventa | Ruolo su tutte le applicazioni dell'account |
   |---|---|---|
   | `owner` | `owner` | accesso implicito, nessuna riga |
   | `admin` | `member` | `admin` |
   | `member` | `member` | `editor` |

   La scelta di `editor` per gli attuali `member` è deliberata: oggi un `member` **può** modificare i dati
   delle applicazioni, e una migrazione non è il momento per togliere poteri di soppiatto. Chi vuole
   stringere lo farà consapevolmente, dopo, con la schermata di UC 0111.

2. **Posti del Mini-CRM**: le righe di posto locali dell'applicazione si traducono in righe di accesso su
   quella applicazione (chi aveva un posto ottiene almeno `editor`); poi la tabella locale si dismette
   (UC 0111).
3. **Ruolo di piattaforma**: i valori `admin` diventano `member`. Da fare **dopo** il passo 1, che quel
   valore lo usa per tradurre.
4. **Inviti in attesa**: perdono il ruolo (non esiste più a quel livello). Restano validi: chi accetta entra
   come `member` senza accessi, e va poi abilitato.
5. **Posti e abbonamento**: si conta quanti posti occupa ogni account (regola di UC 0102) e si registra il
   dato **senza addebitare nulla** (§5).

## 5. Il nodo economico della migrazione — chiuso

Era il punto aperto principale della storia; lo sviluppatore l'ha **chiuso**: **nessun account supera i tre
posti**, perché la piattaforma è ancora **solo in locale** e non ha clienti. Non esiste quindi alcun
rincaro da comunicare, nessun periodo di grazia da concedere, nessuna comunicazione commerciale da
scrivere.

Conseguenze pratiche, tutte semplificazioni:

- la migrazione **non ha una parte economica**: converte accessi e ruoli, conta i posti e si ferma lì;
- **non serve** il comando che elenca gli account oltre la franchigia, né la logica del periodo di grazia:
  erano previsti per un problema che non esiste;
- resta utile un **controllo di sicurezza** dopo la conversione — se un account risultasse oltre i tre
  posti, è il segno che il presupposto è cambiato (per esempio si è già andati in cloud) e la migrazione
  deve **fermarsi e chiedere**, invece di addebitare qualcosa a qualcuno di nascosto.

Il controllo costa poche righe e vale la pena: è l'unico presidio contro un'ipotesi che invecchia.

## 6. Il piano di rilascio

- **Token già emessi** contengono il valore `admin` come ruolo di piattaforma: i servizi lo accettano
  trattandolo come `member` fino alla scadenza naturale (UC 0099). La tolleranza va **rimossa** dopo un
  periodo dichiarato, con un promemoria scritto, altrimenti resta per sempre.
- **Ordine di rilascio**: prima i servizi (che tollerano entrambi i modelli), poi la conversione dei dati,
  poi il frontend. Un frontend nuovo su servizi vecchi mostrerebbe schermate che non funzionano.
- **Ritorno indietro**: la conversione è **additiva** nei primi due passi (crea righe, non le distrugge),
  quindi un ritorno indietro è possibile fino al passo 3. Dal passo 3 in poi serve la conversione inversa,
  che va scritta e provata **prima** del rilascio, non dopo.
- **Verifiche a rilascio avvenuto**: numero di righe di accesso create per account, confronto con il numero
  di utenti per applicazione atteso, e un campione manuale su tre account reali.

## 7. Requisiti di test — la copertura dell'epica

Il pezzo più visibile di questa storia è la **suite per ruolo**, che il registro di copertura pretende:

**Nuovo percorso di piattaforma `J-ROLES`** — «la stessa applicazione vista dai quattro ruoli»: crea un
account, invita tre persone (con il simulatore del pagamento), le abilita al Mini-CRM con i tre ruoli, e poi
**entra come ognuna** verificando, per ciascuna: le voci di menu presenti, le applicazioni visibili, i
comandi attivi e disabilitati nella schermata dei contatti, e ciò che vede nella schermata degli utenti.
È il percorso che dà valore a tutta l'epica, ed è anche il collaudo che i quattro prototipi illustrano.

**Nuovo percorso di piattaforma `J-SEATS`** — «il ciclo di vita del posto»: l'owner supera la franchigia e
vede l'importo, indica una persona per la cessazione, prova a invitare (bloccato), annulla, invita di nuovo
(riuscito).

**Estensione dei percorsi esistenti di livello 2**: menu della shell, membri, catalogo, cruscotto,
fatturazione, diritti dell'interessato — uno per ognuna delle storie 0100, 0106, 0107, 0108, 0109, 0110.

**Registro di copertura**: alla fine dell'epica ogni storia è classificata correttamente — quelle con
superficie referenziate da almeno un percorso, quelle senza superficie esenti col loro motivo. Le esenzioni
`non-implementato` inserite dalla change di analisi vanno **rimosse** man mano che le storie si
implementano: è il dovere di ogni change, non una pulizia finale.

## 8. Permessi & gate

- La conversione gira con le credenziali di migrazione, come tutte le altre.
- Nessun cambiamento di permessi introdotto qui: si applicano quelli delle storie precedenti.
- La parte economica è **bloccata** dietro conferma dello sviluppatore.

## 9. Riferimenti & Definition of Done

- **Riferimenti**: [UC 0090](../../20-test-e2e-piattaforma/0090-e2e-platform-fondamenta.md),
  [UC 0093](../../20-test-e2e-piattaforma/0093-e2e-platform-registro-copertura.md),
  [docs/testing/copertura-e2e.yaml](../../../testing/copertura-e2e.yaml),
  [tools/platform-e2e](../../../../tools/platform-e2e/).
- **Definition of Done**:
  1. la conversione gira su dati realistici in ambiente di prova, con esiti verificati;
  2. nessun cliente perde accesso: provato per ognuna delle tre traduzioni di ruolo;
  3. i token vecchi sono tollerati, con promemoria scritto per togliere la tolleranza;
  4. la conversione inversa esiste ed è provata prima del rilascio;
  5. i percorsi `J-ROLES` e `J-SEATS` esistono e sono verdi;
  6. il registro di copertura è coerente e non contiene più esenzioni per le storie implementate;
  7. `./run-tests.sh` **intero** verde.

## Punti aperti / decisioni differite

### Lasciato da UC 0103 (change 0098)

- **Il percorso `J-SEATS` esiste solo al livello 2.** La change 0098 ha creato il percorso nel registro di
  copertura e l'ha coperto con `frontend/apps/backoffice/e2e/seats.spec.ts`: backend **simulato**, quindi
  prova le frasi che il cliente legge, la stima prima della conferma e il pulsante che resta spento — non
  prova che l'addebito crei davvero l'abbonamento. Il tratto con lo **stack vero** (banca dati, simulatore
  del fornitore di pagamento, abbonamento di piattaforma che nasce con la sua quantità) appartiene alla
  suite di piattaforma, cioè a questa storia. Il registro dichiara `J-SEATS` come `coperto` con una sola
  voce di livello 2: quando arriverà la voce di piattaforma andrà aggiunta accanto, non al posto.
- **Gli account che esistono già hanno persone non pagate, e al primo invito si troverebbero addebitato
  tutto l'arretrato.** Scoperto <b>eseguendo</b> la guida di collaudo della change 0098 sullo stack locale:
  l'account di prova ha nove posti occupati, sei dei quali a pagamento — perché quelle persone sono entrate
  quando il posto era gratuito — e la quantità pagata è **zero**. Il riquadro dice quindi, correttamente,
  che il prossimo posto costa 20,93 €: è la differenza fra il dovuto dei posti bersaglio e quello dei posti
  *già pagati*, che sono nessuno. Il calcolo è giusto; ciò che manca è la **decisione di migrazione**: agli
  account preesistenti si porta la quantità pagata al numero di posti che hanno (regolarizzandoli a costo
  zero), oppure si accetta che il primo invito paghi l'arretrato, oppure si applica una franchigia
  transitoria. Oggi non è un problema perché non esistono account reali (pre-go-live), e proprio per questo
  va deciso **prima** che ne esistano. È una decisione **commerciale** oltre che tecnica: va portata a chi
  decide i prezzi. Proprietario: questa storia, che è quella della migrazione degli account.

- ~~Trattamento economico degli account esistenti oltre la franchigia~~ — **chiuso**: nessun account
  supera i tre posti, la piattaforma è ancora solo in locale. Resta il controllo di sicurezza che fa
  fermare la migrazione se il presupposto non valesse più (§5).
- **Durata della tolleranza sui token vecchi**: proposta trenta giorni, da allineare alla durata effettiva
  dei token. Proprietario: questa storia.
- **Comunicazione ai collaboratori** del nuovo modello (che vedranno un prodotto diverso da un giorno
  all'altro): utile una nota nel prodotto al primo ingresso. Rimandata; annotata in
  [docs/_BACKLOG.md](../../../_BACKLOG.md).

### Lasciato da UC 0098 (change 0091)

- **La conversione delle righe `admin` esistenti** (`admin` → `member` più accesso `admin` su ogni
  applicazione dell'account) resta interamente qui: la change 0091 ha ridotto l'**enumerazione** a due
  valori ma non ha toccato un solo dato reale. Il **seme di sviluppo** è invece già convertito, perché un
  seme che dichiara un ruolo non più ammesso non si caricherebbe.
- **Il vincolo di controllo sui valori di `membership.role`** non è stato aggiunto da `V20__app_access.sql`,
  di proposito: un vincolo aggiunto prima della conversione rifiuterebbe di applicarsi su una banca dati che
  contiene ancora righe `admin` — una migrazione che non parte. Va aggiunto **dopo** il passo 3 della
  conversione, ed è quello che la sigilla.
- **Il ritiro della tolleranza `Roles.ADMIN`** (la costante e le annotazioni `@RolesAllowed` che la
  nominano) va fatto qui, con la data dichiarata nel piano di rilascio: senza una data, resta per sempre.

### Lasciato da UC 0100 (change 0096)

- **La tolleranza `admin` è già ritirata sulle persone e sugli inviti dell'account**, in anticipo su
  questa storia: `UserResource` e `InvitationResource` ammettono il **solo** owner, perché governare le
  persone è esattamente il potere che UC 0100 gli riserva. La costante `Roles.ADMIN` e le altre otto
  operazioni che la nominano (diritti dell'interessato, pagamenti, ticket, posti del Mini-CRM) restano
  intatte e il loro ritiro — con la data — è ancora qui. Conseguenza da tenere presente nel piano di
  rilascio: chi ha in mano un token coniato con `admin` e senza `owner` perde la gestione dei membri
  **subito**, non alla data del ritiro generale.
- **La colonna `invitations.role` non è più scritta con un valore scelto**: il ruolo è uscito dal
  contratto dell'invito (corpo e vista) e la colonna vale sempre `member`, perché è `NOT NULL` senza
  valore predefinito e perché il suo valore è quello con cui nasce l'appartenenza all'accettazione. La
  **rimozione** della colonna appartiene alla conversione, insieme a quella delle righe `admin`: da
  fare quando nessun invito in attesa porta più un valore diverso.
