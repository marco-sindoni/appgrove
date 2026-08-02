# Principi di piattaforma appgrove — digest per chi scrive le storie del catalogo

**A chi serve.** All'agente che scrive il drill-down di **una** applicazione del catalogo
([appgrove-catalogo-applicazioni.md](../appgrove-catalogo-applicazioni.md)) senza avere in contesto il resto del
monorepo. Questo documento è **autoconsistente**: contiene i vincoli che ogni storia di ogni app deve rispettare,
con il percorso del file d'origine per chi voglia verificarli.

**Come si usa.** Non si copia dentro le storie. Si usa per scrivere i **requisiti tecnici** di ciascuna storia:
ogni voce qui sotto chiude con la riga «*In storia si scrive così*», che è la forma verificabile da riportare.

**Regola d'onestà.** Se un vincolo qui non copre il caso della tua app, **non inventare**: scrivi il punto aperto
nella sezione «Rischi e punti aperti» della descrizione dell'applicazione. Un requisito inventato costa più di
una domanda lasciata aperta.

**Lingua.** Tutto ciò che scrivi è in **italiano** ([CLAUDE.md](../../../../CLAUDE.md), sezione «Lingua»). Restano in
inglese solo gli identificatori tecnici: nomi di file, di simboli, comandi, chiavi di configurazione.

---

## 1. Isolamento fra account (multi-tenancy) — l'invariante numero uno

**La regola.** Il *tenant* è l'account (l'azienda cliente). Il suo identificativo `tenant_id` arriva **solo** dal
token di accesso verificato — è un dato che il servizio di autenticazione mette dentro il token; `sub` è invece
l'identificativo dell'utente. Non si legge **mai** dal corpo della richiesta, dai parametri o da un'intestazione.
Ogni tabella che contiene dati di clienti porta la colonna `tenant_id` e ogni interrogazione filtra
`WHERE tenant_id = :tid`. Se il filtro manca, il comportamento corretto è **negare**, non mostrare tutto.

Il collaudo lo sorveglia: esiste una suite di isolamento fra account sempre eseguita (mai esclusa dai filtri di
percorso) e un controllo strutturale che fa fallire la compilazione se qualcuno aggira il filtro.

**Origine.** [docs/01-architettura.md](../../../01-architettura.md) dec. 1-3, 9, 17 ·
[docs/05-persistenza-dati.md](../../../05-persistenza-dati.md) dec. 8 ·
[docs/10-testing.md](../../../10-testing.md) dec. 8-16.

**In storia si scrive così:** «Ogni interrogazione delle entità `<X>` filtra per `tenant_id` preso dal token
verificato; un tentativo di forzare `tenant_id` dal corpo della richiesta viene ignorato. Test di isolamento
fra due account su ogni risorsa introdotta.»

---

## 2. Struttura del backend — Quarkus, Java, un servizio per app

**La regola.** Un'app = un servizio Maven `services/<app_id>/`, che dipende da `services/commons` (contesto del
tenant, mappatura degli errori, entità di base con dati di controllo, paginazione). Stack: **Quarkus 3.20.6**,
**Java 21**, Quarkus REST + Hibernate ORM **bloccante** (la variante reattiva è vietata), accesso ai dati con il
modello *repository*. Pacchetto radice `app.appgrove.<app_id>`.

Regole di confine: gli oggetti di trasferimento (DTO) sempre al bordo — le entità non si espongono mai; validazione
dichiarativa sui DTO; gli errori escono in formato `application/problem+json`; paginazione a pagina/dimensione con
totale; la definizione OpenAPI è generata e versionata.

**Comunicazione.** Un'app **non chiama** un'altra app. L'unica via fra servizi è **asincrona a eventi**. Le rotte
pubbliche sono `/api/<app_id>/v1/...`.

**Origine.** [docs/04-services-backend.md](../../../04-services-backend.md) dec. 1-9 ·
[docs/01-architettura.md](../../../01-architettura.md) dec. 6, 7, 12, 13.

**In storia si scrive così:** «Risorsa `POST /api/<app_id>/v1/<risorsa>`, corpo validato, errori in
`problem+json`, definizione OpenAPI aggiornata nello stesso commit.»

---

## 3. Struttura del frontend — modulo React caricato su richiesta

**La regola.** Il frontend è una applicazione a pagina singola in **React + TypeScript + Vite** (niente Lit, niente
rendering sul server, niente macchinari da micro-frontend). Un'app è un **modulo caricato su richiesta** dentro
`frontend/apps/backoffice/src/modules/<app_id>/`, con un manifesto `manifest.ts` che dichiara
`{ id, name, icon, accentToken, sections[], resources, quota?, component }` e viene aggiunto all'elenco `MODULES`
in `frontend/apps/backoffice/src/registry/registry.ts`. La barra laterale mostra il modulo quando **registro ∩
abilitazione** dicono di sì.

Il modulo **non** gestisce l'autenticazione e **non** conosce il `tenant_id` se non attraverso il contesto che la
shell gli passa. Usa il client delle interfacce generato dalla definizione OpenAPI.
Interfaccia: Tailwind + componenti senza stile proprio (shadcn/Radix) sopra i token del sistema di design; stato
del server con TanStack Query, stato locale con Zustand; moduli di inserimento con React Hook Form + Zod.

**Origine.** [docs/03-frontend.md](../../../03-frontend.md) dec. 1-8, 11-13 ·
[docs/01-architettura.md](../../../01-architettura.md) dec. 10, 11.

**In storia si scrive così:** «Nuova sezione `<nome>` nel manifesto del modulo `<app_id>`; la schermata legge i
dati con il client generato e non accede al token se non tramite il contesto della shell.»

---

## 4. Interfaccia in cinque lingue

**La regola.** Le lingue dell'interfaccia sono esattamente **`en, it, fr, es, de`** (elenco `LANGUAGES` in
`frontend/packages/i18n/src/index.ts`, lingua predefinita `en`). Le traduzioni del modulo stanno **accanto al
modulo**, in `frontend/apps/backoffice/src/modules/<app_id>/i18n/{en,it,fr,es,de}.ts`, sotto uno spazio-nomi che
coincide con l'`id` del modulo. **Nessun testo visibile scritto a mano nei componenti.**

Attenzione a non confondere due elenchi diversi: l'**interfaccia** vuole 5 lingue; il **manifesto dei dati
personali** e il registro dei trattamenti ne vogliono 2 (italiano e inglese, punto 10).

**Origine.** `frontend/packages/i18n/src/index.ts` · [docs/13-compliance-privacy.md](../../../13-compliance-privacy.md) G38.

**In storia si scrive così:** «Tutte le stringhe della schermata passano dallo spazio-nomi `<app_id>` e sono
presenti in tutte e cinque le lingue; la storia non è conclusa se ne manca una.»

---

## 5. Sistema di design condiviso

**La regola.** Colori, raggi, ombre e caratteri vengono dai token in
`frontend/packages/design-system/src/tokens/tokens.css`: neutri caldi, accento configurabile a runtime, tema chiaro
e scuro, colori-categoria per-app (`--ag-cat-green|amber|red|blue|violet|teal`). Caratteri: Plus Jakarta Sans e
JetBrains Mono. **Vietate** le librerie con un aspetto proprio marcato (MUI, Ant): sfondano il tema.
Ogni app riceve **un** colore-categoria, dichiarato sia nel modulo frontend (`accentToken`) sia nel listino
(`category`): devono coincidere.

**Origine.** [docs/03-frontend.md](../../../03-frontend.md) dec. 4, 11-12 · file dei token (UC 0019).

**In storia si scrive così:** «La schermata usa solo i token del sistema di design; funziona in tema chiaro e
scuro; nessun colore scritto a mano.»

---

## 6. Abilitazione all'app e quota — la catena dei varchi

**La regola.** L'accesso a una funzione attraversa cinque varchi, e **solo l'ultimo** è responsabilità dell'app:

1. utente autenticato (token valido) → altrimenti `401`;
2. app non spenta dalla piattaforma → altrimenti `403`;
3. account **abilitato** all'app (l'abilitazione è **derivata** dall'abbonamento, non è una tabella scritta a mano)
   → altrimenti `402`;
4. ruolo sufficiente (`owner` / `admin` / `member`) → altrimenti `403`;
5. **quota** non esaurita → altrimenti `429`.

L'abilitazione si legge dalla **proiezione locale** dell'app, alimentata a eventi: **mai** una chiamata di rete
sincrona all'app centrale sul percorso caldo.

**Origine.** [docs/04-services-backend.md](../../../04-services-backend.md) dec. 7 ·
[docs/09-pagamenti.md](../../../09-pagamenti.md) dec. 29-30 ·
[docs/01-architettura.md](../../../01-architettura.md) dec. 5.

**In storia si scrive così:** «Prima di creare un `<X>` il servizio prenota una unità di quota sulla metrica
`<metrica>`; a quota esaurita risponde `429` con un messaggio che dice come rimediare.»

---

## 7. Listino come codice e modello di quota

**La regola.** Il listino di un'app è **un file nel repository**, non un pannello a runtime:
`services/core/src/main/resources/pricing/<app_id>.yaml`, registrato in `pricing/index.yaml`. Forma reale:

```yaml
slug: crm
name: Mini-CRM
userModel: multi_user            # single_user | multi_user
status: active                   # active | inactive
category: blue                   # colore-categoria dell'app
descriptions: { it: …, en: …, fr: …, es: …, de: … }
tiers:
  - key: free
    name: Mini-CRM Free
    trialDays: 0
    limits: { metric: seats, cap: 2, type: stock }        # a giacenza: nessuna finestra
    features: {}
    prices: []
  - key: team
    name: Mini-CRM Team
    trialDays: 14
    limits: { metric: seats, cap: 10, type: stock }
    features: {}
    prices:
      - { billingCycle: monthly, amount: 1900, currency: EUR }   # importi in CENTESIMI
      - { billingCycle: annual,  amount: 19000, currency: EUR }
```

Vincoli che non si negoziano:

- **solo abbonamento ricorrente**: niente pagamento una tantum, niente addebito a consumo per lo sforamento;
- **doppio ciclo** mensile e annuale, con l'annuale in evidenza (di norma 10× il mensile = «due mesi in regalo»);
- **prova gratuita di 14 giorni** come raccomandazione predefinita, disattivabile; con carta richiesta all'inizio;
- **la metrica di quota dichiara la propria natura**: `flow` (consumo su una finestra che si azzera — «200 documenti
  al mese») oppure `stock` (tetto su ciò che esiste ora — «10 posti»). Sbagliarla è l'errore più costoso del listino;
- al raggiungimento del limite si **blocca** (`429`), non si addebita a sorpresa;
- **prezzi immutabili**: un prezzo con abbonamenti vivi non si modifica — si crea un prezzo nuovo e si archivia il
  vecchio, gli abbonati restano sul loro;
- limiti e funzionalità **non** stanno dal fornitore di pagamento: stanno in questo file.

**Origine.** [docs/09-pagamenti.md](../../../09-pagamenti.md) dec. 1-9, 22-23, 25-30, 34-37, 44, 49 ·
`services/core/src/main/resources/pricing/crm.yaml`.

**In storia si scrive così:** «La storia non fissa prezzi: consuma il tetto pubblicato dall'abilitazione per la
metrica `<metrica>` (natura `<flow|stock>`).» I prezzi si **propongono** nella descrizione dell'applicazione, non
nelle storie (vedi punto 12).

---

## 8. Persistenza — uno schema per app, migrazioni versionate

**La regola.** Un solo database PostgreSQL (Aurora Serverless v2) condiviso, con **uno schema per app**:
`app_<app_id>` (i trattini diventano trattini bassi: `mini-crm` → `app_mini_crm`). Migrazioni **Flyway**, scritte
in SQL, sotto `services/<app_id>/src/main/resources/db/migration/`, **non** applicate all'avvio in produzione.

- chiavi primarie **UUID versione 7** generate dall'applicazione;
- colonne di controllo ovunque: `created_at`, `updated_at`, `created_by`, `updated_by`;
- **cancellazione logica** con `deleted_at`; la cancellazione fisica esiste solo per i diritti dell'interessato e
  per la chiusura dell'account;
- **vietate** le chiavi esterne e le interrogazioni fra schemi diversi: `tenant_id` è un riferimento *logico*;
- un ruolo del database per servizio, con privilegi solo sul proprio schema.

**Origine.** [docs/05-persistenza-dati.md](../../../05-persistenza-dati.md) dec. 1-2, 4-6, 8-11 ·
[docs/01-architettura.md](../../../01-architettura.md) dec. 16.

**In storia si scrive così:** «Migrazione `V<N>__<descrizione>.sql` sullo schema `app_<app_id>`: tabella `<x>` con
`tenant_id`, colonne di controllo e cancellazione logica.»

---

## 9. Infrastruttura — il modulo `microsaas_app`, mai infrastruttura su misura

**La regola.** Una app nuova **istanzia il modulo Terraform `microsaas_app`** (`infra/modules/microsaas_app`),
tramite lo script `infra/scripts/service-add`. Non si scrive infrastruttura parallela a mano, non si modifica a
mano il blocco `module` generato. L'infrastruttura si **valida** in continuo (`fmt`, `validate`, analisi statica,
`terraform test` sul modulo) e si **applica** solo dalla catena di integrazione continua, mai da un portatile.

**Origine.** [CLAUDE.md](../../../../CLAUDE.md) invariante 3 · [docs/10-testing.md](../../../10-testing.md) dec. 28-30.

**In storia si scrive così:** solo la storia delle fondamenta la cita: «L'infrastruttura dell'app nasce
dall'istanza del modulo `microsaas_app` prodotta dallo scaffolding; nessuna risorsa scritta a mano.»

---

## 10. Dati personali — manifesto, esportazione, cancellazione, articolo 9

**La regola.** Ogni app ha un **manifesto dei dati** `docs/compliance/manifests/<app_id>.yaml`. È la fonte unica da
cui si generano il registro dei trattamenti e gli strumenti di esportazione e cancellazione: un campo non
dichiarato è un campo che l'esportazione dimentica e la cancellazione lascia indietro. Schema reale:

```yaml
id: crm
name:        { it: …, en: … }
description: { it: …, en: … }
entries:
  - key: contact.email                      # univoca
    entity: app.appgrove.crm.Contact        # classe Java (facoltativo, ma se c'è serve anche `field`)
    field: email
    location:      { it: …, en: … }         # dove vive il dato
    data_subjects: { it: …, en: … }         # di chi è
    data_category: { it: …, en: … }         # che genere di dato è
    purpose:       { it: …, en: … }         # a cosa serve
    legal_basis:   { it: …, en: … }         # perché è lecito trattarlo
    retention:     { it: …, en: … }         # per quanto si tiene
```

Vincoli:

- **italiano e inglese obbligatori** su ogni testo del manifesto (`docs/compliance/manifests/_config.yaml`);
- il campo Java si annota `@PersonalData`: **un campo annotato e non dichiarato fa fallire la compilazione**;
- l'app implementa il contratto `AppDataContract` (`services/commons/.../gdpr/AppDataContract.java`) con
  `appId()`, `exportData(scope)`, `purgeData(scope)`, `manifest()` — nome per convenzione `<App>DataContract`.
  **Ogni** tabella che contiene dati personali deve comparire in entrambi: dimenticarne una è il difetto di
  conformità più probabile in un'app nuova;
- **sostituire i nomi con dei codici non è cancellare**: la pseudonimizzazione non soddisfa la richiesta di
  cancellazione, la cancellazione è fisica e lascia una riga di prova nel registro delle purghe;
- **categorie particolari (articolo 9)** — salute, dati biometrici, genetici, opinioni politiche, convinzioni
  religiose, orientamento sessuale, appartenenza sindacale: **ci si ferma e si avvisa**. Servono una base giuridica
  rafforzata e una valutazione d'impatto. Vale anche se il campo «è solo facoltativo». Un'app che può evitarle,
  di solito deve evitarle;
- ogni **integrazione esterna nuova** è un potenziale nuovo fornitore che tratta dati per conto nostro: va segnalata;
- i dati personali stanno **a riposo solo in regioni europee**;
- **nessun tracciamento** dentro l'app: solo cookie tecnici, nessun banner di consenso, nessun uso secondario dei
  dati dei clienti.

**Origine.** [docs/13-compliance-privacy.md](../../../13-compliance-privacy.md) C14-C17, F27, I51-I52, K67, L69-L74 ·
`docs/compliance/manifests/fatture.yaml` · `tools/compliance/lib.mjs`.

**In storia si scrive così:** «Se la storia introduce campi che riguardano una persona: voci nuove nel manifesto
in italiano e inglese, campi annotati `@PersonalData`, tabelle aggiunte a `exportData` e `purgeData`.»

---

## 11. Prove automatiche — cosa deve essere verde

**La regola.** Il comando unico è `./run-tests.sh` alla radice; le aree sono
`backend | frontend | infra | compliance | tooling | smoke | platform | site`. Livelli richiesti a un'app:

- **unità**: JUnit 5 + AssertJ + Mockito, mirati (se servono troppi finti, è una prova d'integrazione);
- **integrazione**: `@QuarkusTest` + Testcontainers con PostgreSQL 17 effimero e **migrazioni Flyway vere**;
- **isolamento fra account e ruoli**: obbligatoria, mai esclusa — almeno due account per ogni risorsa, tentativo di
  forzare `tenant_id` dall'esterno, matrice dei ruoli, abilitazione negata;
- **frontend**: Vitest + Testing Library + finto strato di rete; controllo dei tipi `tsc --noEmit`;
- **end-to-end**: Playwright senza finestra, sullo stack locale reale; niente attese a tempo, accesso programmatico,
  dati di prova deterministici e **inventati** (mai dati veri, indirizzi `*.test`);
- **pagamenti**: livello 1 (eventi del fornitore con carichi sintetici firmati) e livello 2 (percorso end-to-end con
  il fornitore simulato) sono bloccanti; il livello 3 su ambiente di prova reale è pre-rilascio. **Vietato**
  guidare con Playwright la finestra del fornitore di pagamento;
- **accessibilità**: controllo automatico sulle schermate principali.

**Percorso end-to-end di piattaforma.** Ogni app ha il suo percorso in `tools/platform-e2e/journeys/J-<APP>.spec.ts`
e ogni test porta **l'etichetta del percorso in testa al titolo**: `test('[J-<APP>] …')`. Il registro
[docs/testing/copertura-e2e.yaml](../../../testing/copertura-e2e.yaml) tiene la mappa *use case → percorso → test*
ed è sorvegliato da un controllo automatico: registro incoerente = suite rossa.
Ogni storia risponde alla domanda di copertura in **uno** dei tre modi ammessi: *coprire ora*, *rimandare* (con
motivo e storia proprietaria), *nessun impatto*. Il silenzio non è una risposta.

**Origine.** [docs/10-testing.md](../../../10-testing.md) dec. 5-8, 17-21, 32-33, 39-40 ·
[docs/testing/README.md](../../../testing/README.md) · [CLAUDE.md](../../../../CLAUDE.md).

**In storia si scrive così:** «Prove: unità sul calcolo `<x>`; integrazione sulla risorsa `<y>`; isolamento fra
due account; percorso end-to-end `[J-<APP>]` esteso con il passo `<z>`.»

---

## 12. Esposizione conversazionale — requisito trasversale del catalogo

**La regola.** Il catalogo pone un requisito trasversale a tutte le 60 app: **ogni funzionalità deve essere
esponibile come strumento al livello conversazionale**, così che l'imprenditore comandi l'azienda da una chat.

**Stato reale, da dire chiaramente:** nel repository **il livello conversazionale non esiste ancora**. È l'epica
`12-ready-for-ai-mcp`, scritta ma non implementata: architettura del server (UC 0061), autenticazione e consenso
delegato (0062), **mappatura operazioni → strumenti, contratto per-app (0063)**, applicazione di abilitazione e
quota alle chiamate dell'assistente (0064), sicurezza e tracciamento (0065), industrializzazione nello scaffolding
(0066).

Cosa deve fare quindi una storia d'app: **dichiarare il contratto degli strumenti** — nome stabile, descrizione in
lingua naturale, schema dei parametri, schema del risultato, marcatura *lettura* o *scrittura* e idempotenza — e
tenerlo **dentro il servizio dell'app**, versionato con essa. Non deve costruire il server: è di piattaforma.

**Regola di sicurezza, non negoziabile** ([catalogo, §8](../appgrove-catalogo-applicazioni.md)): gli strumenti di
**lettura** sono liberi; gli strumenti di **scrittura** con effetti irreversibili — trasmettere un documento a
un'autorità, eseguire un pagamento, cancellare dati, leggere un segreto — producono una **bozza** e richiedono una
**conferma umana esplicita**. L'intelligenza artificiale prepara, la persona approva.

**Origine.** [docs/usecases/12-ready-for-ai-mcp/](../../12-ready-for-ai-mcp/) · catalogo, «Contesto di prodotto» e §8.

**In storia si scrive così:** «La storia dichiara gli strumenti `<nome>(parametri) → risultato`, marcati
lettura/scrittura; gli strumenti di scrittura producono una bozza con conferma. Dipendenza dichiarata: UC 0061-0063
(livello conversazionale, non ancora implementato).»

---

## 13. Abbonamento self-service e prova gratuita

**La regola.** Il cliente attiva, cambia e disdice da solo, dal catalogo app e dalla sezione Fatturazione del
backoffice. Il pagamento passa dal fornitore che agisce come venditore di riferimento; l'acquisto si avvia dal
server e l'abilitazione si accende **solo** quando arriva l'evento di conferma. In locale il fornitore è **sempre
simulato**: nessun pagamento vero.

Stati che danno accesso: `trialing`, `active`, `past_due` (c'è un periodo di tolleranza di due settimane sui
pagamenti falliti). Non danno accesso: `paused`, `canceled` — con la disdetta l'accesso resta fino a fine periodo,
senza rimborso. Il passaggio a un piano inferiore su una metrica **a giacenza** è **bloccato** finché lo stato
eccede il tetto del piano di destinazione. I **diritti dell'interessato** (esportazione, cancellazione) restano
accessibili anche quando l'app è disabilitata o l'abbonamento è scaduto.

**Origine.** [docs/09-pagamenti.md](../../../09-pagamenti.md) dec. 13-16, 22-27, 29, 31, 39, 43-44.

**In storia si scrive così:** «Con abbonamento in `past_due` la funzione resta accessibile; con `canceled` risponde
`402`. L'esportazione dei dati resta accessibile in ogni caso.»

---

## 14. Registrazione strutturata degli eventi

**La regola.** Ogni riga di registro porta `tenant_id`, `app_id`, `user_id`, più l'identificativo di correlazione
della richiesta (`X-Request-Id` / `traceparent`). In produzione il formato è JSON, in sviluppo testo leggibile.
**Nessun dato personale nei registri**: si scrivono identificativi, non nomi, indirizzi o contenuti.

**Origine.** [CLAUDE.md](../../../../CLAUDE.md) invariante 4 · [docs/04-services-backend.md](../../../04-services-backend.md) dec. 6 ·
[docs/13-compliance-privacy.md](../../../13-compliance-privacy.md).

**In storia si scrive così:** «Gli eventi rilevanti (`<x> creato`, `<y> respinto per quota`) sono registrati con
`tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali.»

---

## 15. Avvio locale automatico — niente cablaggi a mano

**La regola.** Una app nuova deve essere **eseguibile in locale subito dopo l'unione del ramo**, senza passi
manuali. La scoperta automatica dei servizi ([dev/lib/services.sh](../../../../dev/lib/services.sh) e
[tools/ci/services.sh](../../../../tools/ci/services.sh)) ricava la mappa *servizio → identificativo app → porta →
schema* dal **solo** file `services/<app_id>/src/main/resources/application.properties`. Da lì discendono da soli
gli script di avvio, le migrazioni, le rotte del proxy locale e gli avvii di collaudo.

Il dovere di chi scrive è quindi **dichiarare bene quelle proprietà**, non incollare righe negli script. Se viene
voglia di modificare a mano uno script di avvio, è un difetto della scoperta automatica, non un passo del lavoro.

Sul frontend: registrare il modulo nel registro delle app e, finché l'abilitazione reale non esiste, abilitarlo
nello stub locale.

**Origine.** [CLAUDE.md](../../../../CLAUDE.md), sezione «Avvio locale di nuove app/moduli».

**In storia si scrive così:** solo la storia delle fondamenta la cita: «`./dev.sh services` mostra l'app con la sua
porta e il suo schema; `./app-start.sh` la avvia senza modifiche manuali agli script.»

---

## 16. L'app non si scaffolda a mano

**La regola.** Una app nuova nasce dalla skill `new-application`
([.claude/skills/new-application/SKILL.md](../../../../.claude/skills/new-application/SKILL.md), UC 0046), che esegue un
generatore deterministico e poi co-pilota le due decisioni che un generatore non può prendere: **listino/quota** e
**dati personali**. Se l'esito del generatore è sbagliato si **corregge il modello e si rigenera**: non si toppa
mai l'output, perché la toppa nessuno se la ricorda e l'app successiva eredita il difetto.

Conseguenza pratica per chi scrive le storie: la **prima epica** di ogni app descrive ciò che lo scaffolding
produce e ciò che va deciso *prima* di lanciarlo — è il motivo per cui la descrizione dell'applicazione contiene
il varco d'identità già compilato (vedi [GUIDA-AUTORE.md](GUIDA-AUTORE.md) e
[TEMPLATE-application-description.md](TEMPLATE-application-description.md)).

**Origine.** [CLAUDE.md](../../../../CLAUDE.md), «Avvio locale» e «Parità dei modelli di scaffolding» ·
[docs/_PARITA-SCAFFOLD.md](../../../_PARITA-SCAFFOLD.md).

---

## Riepilogo — la lista di controllo dei quindici vincoli

| # | Vincolo | Tocca la storia se… |
|---|---|---|
| 1 | `tenant_id` solo dal token, filtro riga per riga | sempre, se legge o scrive dati |
| 2 | Quarkus 3.20.6 / Java 21, rotte `/api/<app_id>/v1/*`, errori `problem+json` | c'è una risorsa nuova |
| 3 | Modulo React caricato su richiesta, registrato nel registro delle app | c'è una schermata |
| 4 | Cinque lingue `en, it, fr, es, de` | c'è un testo visibile |
| 5 | Solo token del sistema di design, tema chiaro e scuro | c'è una schermata |
| 6 | Catena dei varchi: 401 / 403 / 402 / 403 / 429 | c'è una funzione protetta |
| 7 | Listino come codice, metrica `flow` o `stock`, prova di 14 giorni | c'è consumo di quota |
| 8 | Schema `app_<app_id>`, migrazioni Flyway, UUID v7, cancellazione logica | c'è una tabella |
| 9 | Modulo Terraform `microsaas_app` | fondamenta dell'app |
| 10 | Manifesto dati (it+en), `@PersonalData`, esportazione e cancellazione, articolo 9 | tocca dati di persone |
| 11 | Prove: unità, integrazione, isolamento, end-to-end etichettato `[J-<APP>]` | sempre |
| 12 | Contratto degli strumenti conversazionali; scrittura = bozza + conferma | c'è una funzione |
| 13 | Abbonamento self-service, stati che danno accesso, diritti sempre accessibili | tocca l'accesso |
| 14 | Registro eventi con `tenant_id`, `app_id`, `user_id`, senza dati personali | sempre |
| 15 | Avvio locale dalla sola scoperta automatica dei servizi | fondamenta dell'app |
