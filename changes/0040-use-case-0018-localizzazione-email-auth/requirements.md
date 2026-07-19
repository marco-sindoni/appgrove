# Change 0040: Localizzazione email auth (EN/IT) + invio via SES

**Branch**: `change/0040-use-case-0018-localizzazione-email-auth`
**Aree**: `services/auth`, `services/core` (migrazione), `frontend`, `infra`, `shared/email-templates` (nuova)
**Data**: 2026-07-19
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/05-auth/0018-localizzazione-email-auth.md](../../docs/usecases/05-auth/0018-localizzazione-email-auth.md)
**Tocca dati personali?**: **Sì** — si aggiunge `locale` come attributo dell'utente (dato personale nuovo su
`platform.users`) e si formalizza il trattamento "email transazionali di autenticazione" (base **contratto**, #13 B).
Si applica il gate privacy/RoPA di step-03: aggiornamento manifesto + RoPA e classificazione MAJOR/MINOR motivata.

## Problema / Obiettivo

Le email di autenticazione oggi sono **inutilizzabili in produzione**:

- il testo è **concatenato a mano** in `EmailService.java`, **solo in italiano**, senza template né grafica;
- **la lingua dell'utente non esiste** da nessuna parte: nessuna colonna sul database, nessun attributo su Cognito,
  e la lingua scelta nella topbar del frontend non viene né salvata né trasmessa al backend;
- in cloud verifica e reimpostazione password partono col **mittente di default di Cognito** (inglese non
  brandizzato, limite ~50 email al giorno);
- l'email di invito parte dal nostro backend via SES, ma **non può funzionare**: manca l'identità di dominio SES e
  la Lambda sta in una rete privata senza uscita verso internet.

Obiettivo: email di verifica, reimpostazione password e invito **localizzate EN/IT** in base alla lingua dell'utente
(ripiego su EN), con testi da **sorgente unica**, spedite da `noreply@appgrove.app` con firma DKIM, verificabili in
locale via Mailpit e pronte all'accensione del cloud.

## Decisioni prese al gate di chiarimento

1. **La lingua è un attributo dell'utente sul nostro database** (`platform.users.locale`), unica fonte di verità —
   niente duplicazione su Cognito. Valorizzata alla registrazione dalla lingua attiva dell'interfaccia.
2. **La lingua raggiunge la Lambda come parametro della chiamata**, non come attributo dell'utente Cognito: il backend
   la passa in `ClientMetadata` a ogni chiamata Cognito che genera un'email. La Lambda non tocca il database (niente
   rete privata, niente avvio a freddo aggiuntivo su ogni email). Se il parametro manca → ripiego su EN.
3. **Infrastruttura inclusa nella change** (scritta e validata, **non applicata**: il cloud resta spento, costo oggi
   zero): identità di dominio SES + firma DKIM su Route53, Cognito configurato per spedire via SES, accesso di rete
   verso SES dalla rete privata (~$7/mese/ambiente, scatta solo all'accensione — stima costi e evoluzione **E24**
   già aggiornate).
4. **Il collegamento nell'email usa due parametri distinti**, `…/verify?email=…&code={####}`, **non** il token unico
   che il punto aperto dello use case ipotizzava. Motivo: quando Cognito invoca la Lambda **il codice non esiste
   ancora** (la Lambda riceve solo il segnaposto `{####}`, sostituito da Cognito *dopo*); costruire lì il token
   `base64url(email|codice)` incapsulerebbe il segnaposto dentro la codifica e il collegamento arriverebbe rotto.
   La ricomposizione avviene **nel backend**: gli endpoint di verifica e reimpostazione accettano, **in alternativa**
   al `token` odierno, la coppia `email` + `code`. Il contratto a token unico resta valido e continua a servire il
   provider locale — nessuna rottura.
5. **Sorgente unica dei testi**: file di dati per lingua in `shared/email-templates/`, impacchettati sia
   nell'artefatto Java sia nell'archivio della Lambda Python. Nessuna copia da tenere allineata a mano.
6. **Formato**: doppia versione grafica + solo testo dove possiamo scegliere (invito, e tutte le email in locale);
   per verifica/reimpostazione in cloud Cognito accetta un corpo solo → versione grafica. Grafica **minima**
   (intestazione col nome prodotto, corpo essenziale), **nessun tracciamento** di aperture o click.

## Scope

**`services/core`** — nuova migrazione Flyway: colonna `locale` su `platform.users` (valori ammessi `en`/`it`,
default `en`, non nulla).

**`shared/email-templates/`** (nuova) — `en.json` e `it.json`: oggetto, corpo grafico e corpo testuale per i tre
messaggi (verifica, reimpostazione, invito), con i punti di sostituzione per codice e collegamento.

**`services/auth`** — resa dei template dalla sorgente unica al posto delle stringhe concatenate; `MailSender`
esteso al corpo grafico oltre a quello testuale; lingua risolta dall'utente (ripiego EN); `ClientMetadata` con la
lingua su tutte le chiamate Cognito che generano email; endpoint di verifica e reimpostazione estesi ad accettare
`email` + `code` in alternativa al `token`; la registrazione accetta e memorizza la lingua.

**`frontend`** — la registrazione trasmette la lingua attiva dell'interfaccia; le pagine di verifica e
reimpostazione accettano il collegamento nella nuova forma a due parametri oltre a quella a token unico.

**`infra`** — Custom Message Lambda in Python (stesso schema del Pre-Token-Gen, ma **fuori** dalla rete privata e
senza dipendenze esterne) e cablaggio del trigger sul bacino utenti; identità di dominio SES + firma DKIM con i
record su Route53; Cognito configurato per spedire via SES da `noreply@appgrove.app`; accesso di rete verso SES.

**Documentazione** — chiusura dei punti aperti di UC 0018 (compresa la **correzione** di quello sul formato dei
collegamenti, oggi descrittivo di una soluzione non realizzabile); manifesto dati + RoPA.

## Fuori scope

- **Selettore lingua persistente in topbar e pagina impostazioni utente** — tracciato in UC 0020 (proprietario).
- **Stesura del copy definitivo di marketing**: si scrivono testi funzionali e coerenti col tono di brand, non una
  revisione redazionale finale.
- **Lingue oltre EN/IT** (FR/ES/DE riguardano i contenuti pubblici del sito, #13 G38).
- **Email non-auth** (promemoria, esportazioni: UC 0032/0033) — la sorgente unica dei testi però le accoglierà.
- **Gestione dei rimbalzi e dei reclami SES** (notifiche, soppressione) — da tracciare come punto aperto.
- **Applicazione dell'infrastruttura**: nessun `apply`, il cloud resta spento (attivazione per fasi).
- **Uscita dalla modalità di prova di SES**: richiesta manuale ad AWS, già in checklist di accensione.

## Criteri di accettazione

- [ ] Un utente con `locale = it` riceve verifica, reimpostazione e invito **in italiano**; con `locale = en` o lingua
      assente/non riconosciuta, **in inglese**.
- [ ] I testi provengono da **un solo punto**: modificare `shared/email-templates/it.json` cambia il testo sia della
      resa Java sia di quella della Lambda Python, senza toccare altro.
- [ ] Il collegamento generato dalla Lambda contiene il segnaposto `{####}` **non codificato**, e la chiamata di
      verifica del backend accetta sia il `token` unico sia la coppia `email` + `code`, ricomponendo internamente.
- [ ] I test esistenti che estraggono il collegamento dal corpo testuale **continuano a passare** (la versione solo
      testo resta presente dove la controlliamo noi).
- [ ] `terraform validate` e le suite Terraform passano con Custom Message Lambda, identità SES, firma DKIM e accesso
      di rete definiti; nessuna risorsa applicata.
- [ ] In locale, registrandosi con interfaccia in italiano, l'email che arriva su Mailpit è in italiano e il suo
      collegamento apre la pagina di verifica funzionante.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT verificato** — non toccato: le email non veicolano identità di tenant e la lingua è un
  attributo dell'utente, non un parametro di richiesta usato per autorizzare. Gli endpoint estesi (`email` + `code`)
  restano **pre-autenticazione** e non concedono nulla senza il codice generato da Cognito.
- **Filtro row-level `WHERE tenant_id`** — la lettura della lingua avviene su `platform.users` per identità
  dell'utente; le query esistenti conservano il filtro.
- **Modulo Terraform** — la nuova Lambda entra in `platform_shared` accanto a Pre-Token-Gen, riusando lo schema
  esistente; nessuna infrastruttura fuori dai moduli.
- **Logging strutturato** — la Lambda registra lingua scelta e tipo di messaggio **senza mai registrare il codice né
  l'indirizzo in chiaro**; il backend mantiene i campi strutturati già in uso.

## Requisiti di test

- **Java**: risoluzione della lingua (utente `it` → italiano; utente `en`, lingua assente, lingua non riconosciuta →
  inglese); resa dei tre template con i campi sostituiti; endpoint di verifica e reimpostazione accettati in entrambe
  le forme (`token`, e `email` + `code`) con esito equivalente; forma malformata rifiutata.
- **Python** (agganciato alla suite infra, come `test_handler` del Pre-Token-Gen): scelta del template dalla lingua in
  `ClientMetadata` con ripiego EN; presenza del segnaposto `{####}` **non codificato** nel messaggio prodotto;
  copertura dei tipi di messaggio previsti; nessun codice nei log.
- **Parità dei template**: un test verifica che `en.json` e `it.json` abbiano **le stesse chiavi e gli stessi punti di
  sostituzione** — è la rete che impedisce la divergenza fra lingue e fra i due programmi che li rendono.
- **Terraform**: la suite verifica trigger cablato, identità di dominio, firma DKIM e accesso di rete verso SES.
- **Frontend**: la registrazione trasmette la lingua attiva; le pagine di verifica/reimpostazione accettano entrambe
  le forme di collegamento.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | **No** — gli endpoint sono estesi in modo additivo, il contratto a token unico resta valido |
| Contratto cross-area | **Sì** — frontend ↔ auth (lingua alla registrazione, nuova forma del collegamento); auth ↔ infra (`ClientMetadata` verso la Lambda); auth ↔ core (colonna `locale`) |
| Version bump | **minor** |
