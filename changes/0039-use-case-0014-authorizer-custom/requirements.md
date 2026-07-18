# Change 0039: Authorizer al bordo (UC 0014) — chiusura dell'API pubblica

**Branch**: `change/0039-use-case-0014-authorizer-custom`
**Aree**: `infra/` (+ documentazione: use case 0014 e documenti di area)
**Data**: 2026-07-18
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/04-platform-core/0014-authorizer-custom.md](../../docs/usecases/04-platform-core/0014-authorizer-custom.md)
**Tocca dati personali?**: No — la change è solo infrastrutturale (configurazione del gateway e di un gruppo di sicurezza). Nessun nuovo trattamento, nessun nuovo campo, nessun nuovo log applicativo.

## Problema / Obiettivo

Oggi le rotte pubbliche `/api/<app_id>/v1/*` sono **completamente aperte**: chiunque può far arrivare
traffico non autenticato fino ai servizi, che lo rifiutano solo al proprio interno. Il modulo delle app
porta una deroga esplicita al controllo di sicurezza automatico che documenta proprio questa mancanza,
in attesa di questo use case.

Obiettivo: **nessuna richiesta priva di un token valido raggiunge i servizi**, chiudendo la deroga.

### Deviazione dallo use case, decisa in questa change

Lo use case prevedeva una funzione Lambda scritta da noi che eseguisse al bordo tre controlli:
token valido (401), app abilitata (403), tenant abilitato (402). **Non è realizzabile correttamente**
sulla variante di gateway che usiamo ("HTTP API", scelta per contenere i costi): il rifiuto di un
authorizer scritto da noi viene tradotto **sempre in 403**, senza personalizzazione (la riscrittura
delle risposte d'errore esiste solo sulla variante "REST API", più costosa).

Conseguenze verificate sul codice esistente:

- un token **scaduto** produrrebbe 403 invece di 401 → il **rinnovo silenzioso della sessione**
  ([auth-middleware.ts](../../frontend/packages/api-client/src/auth-middleware.ts)) non scatterebbe più e
  l'utente verrebbe disconnesso a ogni scadenza del token (pochi minuti);
- un tenant non abilitato riceverebbe 403 invece di 402 → l'avviso azionabile "abbonamento richiesto"
  ([enforcement.ts](../../frontend/apps/backoffice/src/billing/enforcement.ts)) non comparirebbe più.

**Decisione**: al bordo si usa l'**authorizer nativo per token JWT** del gateway (401 corretto, costo
zero, nessun avvio a freddo, nessuna interrogazione al database sul percorso critico); i controlli
**app abilitata** e **tenant abilitato** restano **nel servizio**, dove sono già implementati e testati
con i codici di risposta corretti (UC 0027). La sostanza dello use case è preservata; cade solo la
collocazione al bordo dei due controlli grossolani.

## Scope

Solo `infra/`.

1. **Authorizer nativo per token JWT** nel modulo delle risorse condivise (`platform_shared`):
   emittente e destinatario del pool Cognito già esistenti, sorgente dell'identità = intestazione
   `Authorization`. Nuovo output `authorizer_id`.
2. **Modulo delle app** (`microsaas_app`): le rotte `ANY /api/<app_id>/v1/{proxy+}` passano ad
   `authorization_type = "JWT"` agganciate all'authorizer; rimozione della deroga al controllo di
   sicurezza automatico. Nuovo campo `authorizer_id` nell'oggetto `shared`, propagato dagli ambienti.
3. **Rotta dedicata e scoperta** per il webhook dei pagamenti
   (`POST /api/platform/v1/webhooks/paddle`): è chiamata dal fornitore dei pagamenti, che non ha un
   token di sessione, ed è autenticata dalla **firma** del messaggio. Il gateway sceglie la rotta più
   specifica, quindi questa "buca" il proxy generico in modo dichiarativo, senza liste di eccezioni
   nel codice. **È l'unico endpoint pubblico** sotto `/api/<app_id>/v1/*` (censimento esaustivo svolto
   in fase di analisi: tutto il resto è già annotato come autenticato o riservato a ruoli).
4. **Stretta del gruppo di sicurezza del proxy del database** (residuo E23, parte b): l'ingresso passa
   dall'**intera rete privata** ai soli gruppi di sicurezza delle due funzioni di autenticazione.
   Verificato che i servizi applicativi si collegano **direttamente** al database, non al proxy.
5. **Test infrastrutturali**: nuove verifiche sul modulo delle app; **nuova suite** per il modulo delle
   risorse condivise (oggi assente) e sua registrazione nello script di controllo.
6. **Aggiornamento della documentazione**: revisione dello use case 0014 (regola di accesso allineata al
   codice, ripartizione dei controlli, vincolo di piattaforma) e dei documenti di area che descrivono
   l'authorizer come funzione Lambda.

## Fuori scope

- **Autenticazione tramite identità AWS verso il proxy del database** (residuo E23, parte a): cambia il
  codice di connessione di entrambe le funzioni, non è provabile in locale e mescolerebbe due categorie
  di problemi alla prima accensione in cloud. Rimandata e tracciata.
- **Blocco al bordo per app disabilitata / tenant non abilitato**: rinunciato per il vincolo di
  piattaforma. Tracciato con le strade percorribili se servirà.
- Qualsiasi modifica a **frontend** e **servizi**: la scelta è stata presa proprio per non doverli
  toccare.
- Cache dell'authorizer: non pertinente (l'authorizer nativo non ne ha bisogno).
- Accensione degli ambienti cloud e verifica sul campo.

## Criteri di accettazione

- [ ] Le rotte `ANY /api/<app_id>/v1/{proxy+}` risultano protette dall'authorizer JWT nel piano
      Terraform; la deroga al controllo di sicurezza automatico è rimossa e il controllo passa.
- [ ] Esiste una rotta `POST /api/platform/v1/webhooks/paddle` **senza** authorizer, più specifica del
      proxy generico, con deroga motivata (firma del messaggio).
- [ ] L'authorizer è configurato con emittente e destinatario del pool Cognito esistente e legge
      l'intestazione `Authorization`.
- [ ] L'ingresso al proxy del database è consentito ai soli gruppi di sicurezza delle due funzioni di
      autenticazione, non più all'intera rete privata.
- [ ] Suite Terraform del modulo delle risorse condivise creata e registrata; `./run-tests.sh infra` verde.
- [ ] Use case 0014 rivisto (deviazione documentata) e punti differiti tracciati nei file di competenza.

## Invarianti appgrove toccati

- **Tenant ID solo dal token verificato**: rafforzato. L'authorizer verifica firma, emittente,
  destinatario e scadenza al bordo; i servizi continuano a leggere `tenant_id` dal token e a verificarne
  tipo ed emittente (difesa in profondità invariata).
- **Filtro per tenant sulle query**: non toccato.
- **Modulo Terraform `microsaas_app`**: rispettato e rafforzato — la protezione arriva dal modulo, quindi
  ogni app futura la ottiene per costruzione, senza infrastruttura dedicata.
- **Logging strutturato**: non toccato (nessun codice applicativo nuovo). L'intestazione di correlazione
  continua a essere apposta dall'integrazione del modulo.

## Requisiti di test

- Modulo delle app: la rotta deve avere `authorization_type = "JWT"` e l'identificativo dell'authorizer
  proveniente dall'oggetto `shared` (verifica che una nuova app la erediti per costruzione).
- Modulo delle risorse condivise (suite nuova): l'authorizer esiste, è di tipo JWT, punta all'emittente
  del pool e legge l'intestazione `Authorization`; la rotta del webhook esiste **senza** authorizer;
  l'ingresso del proxy del database non contiene più l'intera rete privata.
- Le suite di frontend e servizi devono restare **verdi senza modifiche**: è la verifica che la scelta
  non ha intaccato i contratti 401 (rinnovo silenzioso) e 402 (avviso abbonamento).

## Punti da tracciare prima della chiusura

| Punto | Dove va tracciato |
|---|---|
| E23 parte (a): autenticazione tramite identità AWS verso il proxy | UC 0014 (punti aperti) + registro evoluzioni (E23 aggiornato: (b) fatta) |
| Blocco al bordo per app disabilitata / tenant non abilitato: strade percorribili se servirà | UC 0014 (punti aperti) |
| La chiamata app→core per gli entitlement, quando verrà cablata in cloud, deve puntare all'indirizzo **interno** di rete, mai al dominio pubblico (altrimenti l'authorizer la blocca) | UC 0027 (punti aperti) |
| Il test end-to-end su ambiente reale è il "canarino" dell'eccezione sul webhook: se manca, va in scadenza | UC 0029 (punti aperti) |
| Eventi audit dei flussi di autenticazione: **non** si chiudono qui (non esiste più una funzione authorizer nostra); restano in capo alle funzioni di autenticazione | Riassegnare da UC 0014 a UC 0015/0016 |
| Propagazione dell'identificativo di correlazione attraverso l'authorizer: **decade** (nessun codice nostro al bordo) | UC 0014 (chiusura del punto) |

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | Sì, per i chiamanti non autenticati delle rotte applicative — che è l'obiettivo. Nessun impatto sui client legittimi. |
| Contratto cross-area | Sì: infrastruttura ↔ frontend (il contratto 401/402 viene **preservato**, ed è la ragione della scelta) |
| Version bump | minor |
