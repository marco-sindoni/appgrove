# UC 0118 — Inviti e registrazione quando l'identità esiste già

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.5 Identità e appartenenze](../epic/E22-05-identita-e-appartenenze.md)
**Dipendenze**: [UC 0116](0116-identita-e-appartenenze.md), [UC 0117](0117-account-attivo-e-selettore.md), UC 0058 (flussi di accesso locali), UC 0059 (schermata membri e inviti)
**Piano di lavoro**: [task/0118](../task/0118-inviti-e-registrazione-con-identita-esistente.md)
**Ultimo aggiornamento**: 2026-08-20

## 1. Obiettivo / Scope

I due percorsi d'ingresso che oggi si scontrano contro l'unicità dell'indirizzo, e che dopo
[UC 0116](0116-identita-e-appartenenze.md) devono funzionare:

- **A** — un'azienda invita una persona che **ha già** un'identità appgrove (perché ha un proprio account,
  o perché è membro di un'altra azienda): l'invito deve **collegare** l'identità esistente, non tentare di
  crearne una nuova;
- **B** — una persona che è **già membro** di un'azienda vuole aprire un proprio account: si crea un nuovo
  account con una nuova appartenenza in ruolo `owner`, **non** una seconda identità.

**Incluso**: i due percorsi; l'accettazione dell'invito senza rifare la registrazione; i messaggi
comprensibili al posto delle violazioni di indice; la conciliazione fra messaggi utili e riservatezza; i
posti a pagamento nei due percorsi.

**Escluso**: il modello dati → UC 0116; l'account attivo → UC 0117; il prezzo dei posti → UC 0102.

## 2. Attori & ruoli

- **Owner che invita**: scrive un indirizzo. Non deve sapere, e non deve poter dedurre, se quella persona
  esista già sulla piattaforma.
- **Persona invitata**: se ha già un'identità, accetta e si trova dentro — senza creare una seconda
  parola d'accesso, che sarebbe una seconda identità mascherata.
- **Persona che apre un proprio account** pur essendo membro altrove: percorso di registrazione normale,
  con l'unica differenza che l'identità c'è già.

## 3. Precondizioni

- Identità e appartenenze esistono (UC 0116) e l'account attivo funziona (UC 0117).
- Esistono i percorsi attuali: invio e accettazione dell'invito
  ([AuthResource.java](../../../../services/auth/src/main/java/app/appgrove/auth/AuthResource.java),
  `/invitations/accept`) e la creazione di account con owner
  ([PlatformWriter.java](../../../../services/auth/src/main/java/app/appgrove/auth/PlatformWriter.java),
  `createAccountWithOwner`).

## 4. Flusso principale

**Percorso A — invito a chi esiste già**

1. L'owner invita un indirizzo. Il controllo di oggi verifica soltanto se esista già un invito in attesa
   **in quell'account** ([InvitationResource.java](../../../../services/core/src/main/java/app/appgrove/core/platform/InvitationResource.java)):
   va **aggiunto** il controllo «questa persona è già membro di questo account», che è la vera collisione.
2. L'invito si crea comunque, che l'identità esista o no: l'owner non riceve un esito diverso nei due casi
   (§5).
3. Alla **accettazione**, il sistema distingue:
   - identità **inesistente** → si crea l'identità e l'appartenenza, come oggi (con parola d'accesso e
     nome);
   - identità **esistente** → si crea **solo** l'appartenenza. Non si chiede una parola d'accesso nuova,
     non si chiede il nome: si chiede di **autenticarsi**, e l'accettazione avviene a nome
     dell'identità autenticata.
4. Se al momento dell'accettazione la persona è già dentro la piattaforma, l'invito diventa un **consenso
   da dare** nell'interfaccia: «l'azienda X ti invita ad accedere; accetti?». Non un collegamento da posta
   elettronica che chiede una registrazione già fatta. Il posto giusto è una **sezione del cruscotto**, in
   testa, non un pulsante nell'intestazione: un invito a collaborare con un'altra azienda merita una
   decisione consapevole, e un invito non risposto non è un dettaglio — è un rapporto di lavoro in
   sospeso. Perché resti visibile anche da un'altra schermata, la voce «Dashboard» del menu porta il
   **numero** degli inviti in attesa.
5. Accettato, l'account nuovo diventa **selezionabile** dal selettore (UC 0117), e in genere quello attivo
   subito dopo l'accettazione — perché la persona ha appena detto che vuole andare lì.

**Percorso B — chi è membro apre un proprio account**

6. Registrazione normale. Il sistema trova l'identità esistente, **si autentica** invece di crearla, e
   crea il nuovo account con l'appartenenza in ruolo `owner`.
7. La persona si trova con due appartenenze: `owner` del proprio account, `member` dell'azienda. Nessun
   dato passa da una parte all'altra.

## 5. Il nodo della storia: messaggi utili senza rivelare nulla

Un messaggio chiaro — «questa persona ha già un account appgrove» — sarebbe comodo per chi invita e
**inaccettabile**: rivelerebbe a un'azienda l'esistenza di un rapporto fra quella persona e la piattaforma,
cioè un'informazione che non le appartiene. È lo stesso motivo per cui i percorsi di accesso di oggi danno
risposte neutre («risposta neutra», «neutra contro l'enumerazione» in
[AuthResource.java](../../../../services/auth/src/main/java/app/appgrove/auth/AuthResource.java)).

La regola: **l'esito dell'invito è sempre lo stesso**, esista l'identità o no. La differenza si vede solo
**dopo**, e solo dalla persona invitata, che sa già di sé.

Il messaggio comprensibile serve invece per la collisione **legittima**, quella che l'owner può conoscere
perché riguarda il suo account: «questa persona è già membro» oppure «c'è già un invito in attesa per
questo indirizzo». Entrambe sono informazioni **sue**. La violazione di indice sparisce non perché la
nascondiamo meglio, ma perché non è più un caso possibile.

## 6. Flussi alternativi / edge / errori

- **Edge — invito a chi è già membro dello stesso account**: rifiuto con messaggio chiaro, ed è lecito
  perché è informazione dell'account.
- **Edge — invito accettato da un'identità con indirizzo diverso** da quello invitato: rifiutato. L'invito
  vale per l'indirizzo a cui è stato mandato, altrimenti diventa trasferibile.
- **Edge — invito in attesa e nel frattempo la persona apre un proprio account**: l'invito resta valido.
  All'accettazione diventa la sua seconda appartenenza.
- **Edge — la persona rifiuta l'invito**: l'invito si chiude come rifiutato e il posto pagato **si libera**.
  Va deciso cosa fare del pagamento anticipato: vedi §7.
- **Errore — l'account ha già raggiunto i posti pagati**: rifiuto, come per qualunque invito
  ([UC 0103](0103-acquisto-anticipato-posto-invito.md)). Il fatto che l'identità esista già non cambia nulla:
  **il posto si paga in ogni caso**.
- **Edge — persona sospesa in un account, attiva in un altro**: gli stati sono dell'appartenenza, non
  dell'identità. La sospensione in un account non tocca l'altro. L'unica sospensione che ferma tutto è
  quella dell'**identità**, che spetta all'amministratore di piattaforma.
- **Edge — cancellazione dell'identità richiesta dalla persona** mentre esistono appartenenze attive: non
  si esegue subito. La persona deve prima uscire dagli account (o esserne rimossa): non può cancellare
  unilateralmente dati di cui titolari sono altri. Va detto chiaramente nella schermata, non scoperto a
  operazione rifiutata.

## 7. Dati toccati

- **`platform.invitations`**: acquista il riferimento all'identità collegata quando esiste già
  (`identity_id`, annullabile), e lo stato «rifiutato», che oggi non è previsto in modo esplicito.
- **Nessun dato personale nuovo**: gli indirizzi sono già trattati. Cambia però il **momento** in cui si
  sa che l'indirizzo corrisponde a un'identità: quel confronto va fatto lato server e **non** deve
  produrre risposte distinguibili (§5).
- **Posti**: la persona invitata occupa **un posto in quell'account**, indipendentemente dalle sue altre
  appartenenze. È la regola più semplice ed è anche quella giusta — ogni account paga le persone che usano
  *le sue* applicazioni. Va scritta nel testo mostrato al cliente, perché la prima reazione sarà «ma la
  paga già l'altra azienda».

## 8. Permessi & gate

- **Solo l'owner invita** (requisito già stabilito): non cambia.
- **Risposte non distinguibili** sull'esistenza dell'identità: è un requisito di sicurezza, con collaudo
  dedicato, non una raccomandazione.
- **L'accettazione richiede l'autenticazione dell'identità invitata**: l'invito da solo non concede nulla.
- **Il posto si paga prima dell'invio dell'invito**, in ogni percorso (UC 0103).

## 9. Requisiti di test

- **Integrazione, percorso A**: invito a identità esistente → accettazione autenticata → seconda
  appartenenza, senza seconda identità e senza seconda parola d'accesso.
- **Integrazione, percorso B**: membro che apre un proprio account → nuovo account, appartenenza `owner`,
  identità unica.
- **Sicurezza, la prova che conta**: gli esiti dell'invio dell'invito sono **indistinguibili** fra identità
  esistente e inesistente — stesso codice, stesso corpo, tempi non discriminanti. È la prova che tiene la
  riservatezza; da scrivere accanto a quelle già esistenti sulle risposte neutre.
- **Integrazione, collisioni legittime**: già membro e invito già in attesa producono messaggi chiari e
  distinti.
- **Posti**: l'invito a un'identità esistente consuma un posto come qualunque altro; senza posti
  disponibili viene rifiutato.
- **Percorsi end-to-end**: un'azienda invita una persona che ha già un proprio account; la persona accetta
  e passa fra i due account. Percorso `J-INVITE-EXISTING`, nel registro di copertura.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [AuthResource.java](../../../../services/auth/src/main/java/app/appgrove/auth/AuthResource.java)
  (accettazione e risposte neutre), [PlatformWriter.java](../../../../services/auth/src/main/java/app/appgrove/auth/PlatformWriter.java)
  (creazione di account e utenti), [InvitationResource.java](../../../../services/core/src/main/java/app/appgrove/core/platform/InvitationResource.java)
  (il controllo di oggi), UC 0058 e UC 0059.
- **Definition of Done**:
  1. l'invito collega un'identità esistente invece di tentare di crearne una;
  2. chi è già dentro accetta autenticandosi, senza seconda registrazione;
  3. chi è membro può aprire un proprio account, con una sola identità;
  4. gli esiti dell'invito non rivelano l'esistenza dell'identità, provato da un collaudo dedicato;
  5. le collisioni legittime hanno messaggi comprensibili;
  6. il posto si paga in ogni percorso e la regola è scritta nel testo mostrato al cliente;
  7. `run-tests.sh backend frontend` verde.

## Punti aperti / decisioni differite

- **Lasciato indietro dalla change `0088` (UC 0116), perché è di questo use case**: lo stato
  «appartenenza in attesa di accettazione» **non** esiste su `platform.membership`. Oggi l'attesa è la
  riga di invito (`platform.invitations`, stato `pending`) e un secondo modo di dirla, che nessun
  percorso usa, sarebbe solo un'ambiguità in più. Se i percorsi d'ingresso di questa storia hanno
  bisogno di un'appartenenza «prenotata» (per esempio per tenere occupato un posto acquistato in
  anticipo, UC 0103), è qui che lo stato si introduce — con il vincolo di unicità che va riletto,
  perché è limitato alle righe vive.
- **Riuso di un indirizzo dopo la cancellazione di un'identità**: l'unicità di `platform.identity` su
  indirizzo e identificativo di autenticazione è **incondizionata** (vale anche sulle righe cancellate),
  come era su `platform.users`. Chi si ripresenta con l'indirizzo di un'identità cancellata trova un
  rifiuto di indice, non un messaggio. Rientra nel tema di questa storia: messaggi comprensibili senza
  rivelare l'esistenza di un'identità.

- **Rimborso del posto se l'invito viene rifiutato o scade**: non deciso. La linea coerente col modello a
  mese intero è che il posto resti pagato per il periodo in corso e torni disponibile per un altro invito.
  Va confermato perché **riguarda denaro**. Proprietario: [UC 0103](0103-acquisto-anticipato-posto-invito.md).
- ~~**Invito mostrato dentro l'applicazione** a chi è già dentro (§4.4)~~ — **chiuso**: è una **sezione
  del cruscotto**, in testa, con il numero riportato sulla voce «Dashboard» del menu perché resti visibile
  anche da altrove. Scartata la prima ipotesi (un pulsante nell'intestazione): passava inosservata, e la
  decisione che chiede vale più di un pulsantino. Reso in [prototype/admin.html](../prototype/admin.html)
  e mappato su `pages/dashboard/DashboardPage.tsx` + un componente nuovo
  `dashboard/PendingInvitesSection.tsx` nella [documentazione dei prototipi](../prototype/README.md).
- **Unione di due identità** create per errore con indirizzi diversi dalla stessa persona: fuori scope, ed
  è un lavoro sgradevole. Da annotare come possibile richiesta di assistenza. Proprietario: docs/_BACKLOG.md.
