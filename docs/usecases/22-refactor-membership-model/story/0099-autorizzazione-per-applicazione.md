# UC 0099 — Autorizzazione per applicazione: token, propagazione del ruolo, varco riusabile

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.1 Fondamenta](../epic/E22-01-fondamenta-modello-centralizzato.md)
**Dipendenze**: UC 0098 (modello dati dell'accesso), UC 0016 (funzione che costruisce il token), UC 0010 (fornitore di identità locale), UC 0027 (applicazione dei diritti e delle quote)
**Piano di lavoro**: [task/0099](../task/0099-autorizzazione-per-applicazione.md)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Fare in modo che il ruolo su un'applicazione **venga rispettato** da ogni servizio, con **un solo**
meccanismo condiviso, e decidere che cosa il token porta e cosa no.

**Incluso**: la riduzione del claim dei ruoli al solo ruolo di piattaforma (nella funzione in cloud e nel
suo gemello locale); la lettura del ruolo per applicazione da parte dei servizi, con copia locale
invalidata a eventi; il varco dichiarativo riusabile in `services/commons`; la lettura che il backoffice
usa per sapere dove la persona può entrare e con che ruolo.

**Escluso**: il significato di comportamento dei ruoli → UC 0101; le schermate → UC 0107 e 0111; il
modello dati → UC 0098.

## 2. Attori & ruoli

- **Chi sviluppa un'applicazione**: dichiara il ruolo minimo richiesto da una operazione e non scrive
  logica di autorizzazione.
- **Sistema di identità**: costruisce il token con account, identità e ruolo di piattaforma.
- **Servizio del core**: unica fonte di verità del ruolo per applicazione.
- **Servizio dell'applicazione**: tiene una copia locale del ruolo e la usa per decidere.

## 3. Precondizioni

- Esiste `platform.app_access` (UC 0098).
- Esiste il meccanismo di copia locale dei diritti d'accesso con invalidazione a eventi
  (`services/commons/.../entitlement/projection/`).
- Esistono la funzione che costruisce il token in cloud e il fornitore di identità locale, con gli stessi
  claim.

## 4. Flusso principale

1. La persona entra: il sistema di identità costruisce il token con **account**, **identità** e claim dei
   ruoli contenente **solo** il ruolo di piattaforma (`owner` oppure `member`; più `platform-admin` per
   chi amministra la piattaforma, come oggi).
2. Il backoffice chiede al core **dove** quella persona può entrare: una lettura che restituisce, per
   ogni applicazione a cui l'account ha diritto **e** la persona ha accesso, l'identificativo
   dell'applicazione e il **ruolo** della persona su di essa.
3. La persona apre un'applicazione. Il servizio dell'applicazione riceve la richiesta e, prima di
   eseguirla, chiede al proprio strato condiviso il ruolo della persona su di sé.
4. Lo strato condiviso risponde dalla **copia locale**; se non ce l'ha (o è scaduta) la chiede al core e
   la conserva.
5. L'operazione dichiara il ruolo minimo che richiede. Se il ruolo della persona è sufficiente
   l'operazione procede; altrimenti riceve un rifiuto tipizzato con la spiegazione del ruolo mancante.
6. Un cambio di accesso o di ruolo nel core (UC 0098) emette un evento che **invalida** la copia locale:
   il nuovo ruolo vale entro pochi secondi, senza che la persona rientri.

## 5. Flussi alternativi / edge / errori

- **Errore — nessun accesso all'applicazione**: rifiuto `403` tipizzato, con testo che spiega che serve
  l'abilitazione dell'owner o di un `admin` dell'applicazione. È lo stesso caso che il Mini-CRM già
  gestisce oggi col suo varco locale, e che ora diventa comune.
- **Errore — ruolo insufficiente**: rifiuto `403` tipizzato che nomina il ruolo richiesto («serve almeno
  `editor`»). Distinto dal caso precedente: uno dice «non entri», l'altro «non puoi fare *questo*».
- **Edge — owner**: ha sempre il ruolo massimo, per costruzione, senza righe di accesso.
- **Edge — copia locale non disponibile e core non raggiungibile**: **si nega**. Il criterio è quello già
  adottato per i diritti d'accesso: in assenza di informazione non si concede. Va però distinto nel testo
  («servizio momentaneamente non disponibile», non «non hai i permessi») per non accusare l'utente di
  un guasto nostro.
- **Edge — operazioni distruttive**: per le operazioni irreversibili (cancellazioni di massa, cambio di
  ruoli, revoche) il ruolo si **rilegge dal core** invece di fidarsi della copia locale. Costa una
  chiamata in più su pochissime operazioni e chiude la finestra in cui una revoca appena fatta non è
  ancora arrivata.
- **Edge — chi amministra la piattaforma**: il ruolo `platform-admin` **non** dà accesso ai dati dei
  clienti (regola già in vigore); resta fuori da questo meccanismo.
- **Edge — token vecchio emesso prima del rilascio**: contiene il valore `admin` come ruolo di
  piattaforma. Va accettato come `member` fino alla scadenza naturale, e va scritto nel piano di rilascio
  (UC 0113).

## 6. Risorse & runbook _(storia di piattaforma)_

Nessuna schermata. La lettura «dove posso entrare e con che ruolo» è consumata da UC 0107 (menu e rotte)
e ogni servizio la usa attraverso lo strato condiviso.

## 7. Dati toccati

- **Claim del token**: il claim dei ruoli **perde** i valori `admin` a livello di piattaforma. Nessun
  dato personale nuovo; il token non porta l'elenco delle applicazioni né i ruoli su di esse — **questa
  è la decisione centrale** della storia.
- **Copia locale del ruolo** dentro ogni servizio di applicazione: identificativo della persona,
  identificativo dell'applicazione, ruolo, istante di validità. Nessun dato personale (nessuna email,
  nessun nome): la copia è indicizzata sull'identificativo di autenticazione.
- **Nuova lettura del core**: `/me/app-access`, che espone per la persona che chiama la coppia
  applicazione → ruolo. Filtra sull'account del token.

## 8. Permessi & gate

- **Account solo dal token verificato**, anche nello strato condiviso: la copia locale è indicizzata per
  account e per persona, e non si legge mai un ruolo di un'altra coppia.
- **Il varco è dichiarativo**: l'operazione dice `@RichiedeRuolo(EDITOR)` (nome definitivo da fissare in
  implementazione) e il filtro condiviso decide. Nessuna applicazione scrive confronti di ruolo a mano;
  chi lo fa viene colto dal collaudo di parità (UC 0112).
- **In assenza di informazione si nega** (criterio di sicurezza già adottato per i diritti d'accesso).
- **Le operazioni distruttive rileggono dal core**.

## 9. Requisiti di test

- **Unità**: la funzione che confronta ruolo posseduto e ruolo richiesto (compreso l'ordinamento
  `viewer` < `editor` < `admin` e la posizione dell'owner sopra tutti); la scadenza della copia locale.
- **Integrazione** nel core: la lettura `/me/app-access` restituisce solo le applicazioni con accesso e
  diritto dell'account; l'owner le vede tutte.
- **Integrazione** in un servizio di applicazione: con ruolo sufficiente l'operazione passa, con ruolo
  insufficiente riceve il rifiuto tipizzato giusto, senza accesso riceve l'altro rifiuto.
- **Invalidazione**: cambiato il ruolo nel core, il servizio dell'applicazione applica il nuovo ruolo
  entro il tempo previsto; prova esplicita che la copia vecchia non sopravvive.
- **Fallimento chiuso**: core non raggiungibile e copia assente → rifiuto, con il messaggio di guasto e
  non quello di permesso negato.
- **Prova sui token vecchi**: un token col valore `admin` di piattaforma è trattato come `member`.
- **Percorsi end-to-end**: nessuno proprio; esente come *senza superficie* nel registro di copertura.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [handler.py](../../../../infra/modules/platform_shared/lambda/pre_token_gen/handler.py),
  [UC 0016](../../05-auth/0016-pre-token-gen-jwt.md), [UC 0010](../../03-local-dev/0010-provider-auth-locale.md),
  [SeatAccess.java](../../../../services/crm/src/main/java/app/appgrove/crm/SeatAccess.java) come precedente da generalizzare,
  `services/commons/src/main/java/app/appgrove/commons/entitlement/projection/` come modello del meccanismo.
- **Definition of Done**:
  1. il token porta solo il ruolo di piattaforma, in cloud e in locale, con gli stessi claim;
  2. esiste la lettura «dove posso entrare e con che ruolo»;
  3. esiste un varco dichiarativo in `services/commons` che ogni servizio usa senza scrivere logica;
  4. un cambio di ruolo si sente entro il tempo dichiarato; le operazioni distruttive non si fidano della
     copia;
  5. in assenza di informazione si nega, con messaggio di guasto distinto;
  6. `run-tests.sh backend` verde; l'infrastruttura della funzione del token verificata.

## Punti aperti / decisioni differite

- **Durata massima della copia locale**: proposta di partenza sessanta secondi, allineata a quella dei
  diritti d'accesso. Da confermare misurando il traffico. Proprietario: questa storia in implementazione.
- **Nome definitivo dell'annotazione del varco**: da fissare con chi implementa, coerente con
  `@RequiresEntitlement` già esistente.
- **Se esporre anche il ruolo nel contratto fra shell e moduli del frontend**: sì, ed è necessario per
  UC 0111 (la schermata deve sapere se disabilitare i comandi). Il campo `roles` del contratto attuale va
  ripensato in `appRole`. Proprietario: UC 0107.

### Lasciato da UC 0098 (change 0091)

- **L'evento di invalidazione della copia locale non viene emesso.** `platform.app_access` esiste e le tre
  operazioni che lo scrivono (concessione, cambio di ruolo, revoca) sono in `AppAccessResource`, con un
  commento che dice a voce alta «da qui andrà emesso l'evento»: i punti sono già individuati, non vanno
  cercati. Il meccanismo — copia locale nei servizi delle applicazioni e invalidazione a eventi — è di
  questa storia.
- **La lettura «dove può entrare questa persona, e con che ruolo»** non esiste ancora: il repository ha
  `findByIdentity(identityId)`, che è l'ingrediente, ma nessuna operazione di rete la espone. È la lettura
  che il backoffice usa per il menu laterale, e appartiene a questa storia.
- **Il ritiro del valore `admin` dal claim dei ruoli**: la costante `Roles.ADMIN` e le annotazioni
  `@RolesAllowed` che la nominano sono rimaste intatte, come tolleranza dei token già emessi. La riduzione
  del claim è di questa storia; il ritiro della tolleranza, con la sua data, è di UC 0113.
