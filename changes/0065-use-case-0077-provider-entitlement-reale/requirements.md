# Change 0065: Provider entitlement reale — chiusura dei punti scoperti

**Branch**: `change/0065-use-case-0077-provider-entitlement-reale`
**Aree**: `services/core`, `frontend/`
**Data**: 2026-07-30
**Autore**: Platform Engineering (modalità autopilot)
**Use case sorgente**: [docs/usecases/15-supporto-e-piattaforma/0077-provider-entitlement-reale.md](../../docs/usecases/15-supporto-e-piattaforma/0077-provider-entitlement-reale.md)
**Tocca dati personali?**: No — la change lavora su identificatori tecnici di app e su stati di abbonamento
già trattati (UC 0013/0025). Nessun nuovo trattamento, nessun nuovo campo personale, nessuna nuova
finalità. Lo scanner dei segnali privacy va comunque eseguito a step-03 come da procedura.

## Problema / Obiettivo

Lo use case 0077 chiede di sostituire lo stub degli entitlement del backoffice con la loro fonte reale.
**Il cuore di quel lavoro è già in `main`**, consegnato in anticipo dalla change che ha implementato
UC 0027: l'endpoint del tenant [`GET /api/platform/v1/me/entitlements`](../../services/core/src/main/java/app/appgrove/core/billing/MeResource.java)
esiste con la sua derivazione (account dal token verificato, abbonamento che concede accesso ∩ app attiva,
più la baseline del piano gratuito), il backoffice lo consuma davvero via client API e libreria di gestione
delle richieste, e lo stub è già confinato ai test. I punti 1, 2 e 5 della Definition of Done dello use case
sono quindi soddisfatti e **non vengono rifatti**.

Restano scoperti quattro punti, e tre di essi sono difetti osservabili, non rifiniture:

1. **L'errore è indistinguibile dall'assenza di diritti.** Il provider espone la condizione di errore ma
   nessun consumatore la legge. Se l'endpoint entitlement non risponde, l'insieme delle app abilitate resta
   vuoto e la shell si comporta come se l'account non avesse comprato nulla: la barra laterale dice "Ancora
   niente qui" e qualsiasi rotta di modulo rimbalza su "Non hai accesso a questa app". Un cliente pagante
   viene informato di non avere accesso a ciò che ha pagato, per un guasto di rete. Lo use case lo vieta
   esplicitamente (§5: messaggio chiaro, e mai un ripiego silenzioso).
2. **Il menu resta stantio.** La lettura degli entitlement è messa in cache e non viene mai invalidata: dopo
   un acquisto andato a buon fine, un cambio piano, una disdetta o una ripresa, il menu mostra la situazione
   precedente finché l'utente non ricarica la pagina.
3. **Le due viste sulla stessa verità divergono.** La regola di accesso è scritta due volte — in Java per il
   read-model del tenant, in SQL per la matrice della console admin — e le due copie non dicono la stessa
   cosa: la matrice admin parte dalle righe di abbonamento, quindi non vede mai un'app abilitata dal piano
   gratuito di baseline, e ignora l'account in attesa di eliminazione. Chi apre la console per capire "cosa
   vede questo cliente" oggi può leggere una risposta falsa.
4. **Il polling post-acquisto può contraddire il menu.** Lo stato minimale usato dal polling (UC 0024) è
   calcolato sul solo stato dell'abbonamento e ignora lo stato dell'app: se un'app venisse disabilitata dalla
   piattaforma (UC 0076) durante un acquisto, il checkout direbbe "attiva" e il menu direbbe di no.

Obiettivo osservabile a fine change: **una sola regola di accesso** alimenta menu del backoffice, matrice
admin e polling post-acquisto; la shell distingue i quattro stati previsti; il menu si aggiorna da sé quando
gli entitlement cambiano per azione dell'utente.

## Scope

**Backend (`services/core`)**

- La regola di accesso a un'app per un account diventa **un punto solo**, condiviso: oggi vive duplicata fra
  il read-model del tenant e la matrice admin. La regola completa è quella già in vigore per il backoffice —
  app attiva **e** (abbonamento che concede accesso **oppure** piano gratuito di baseline disponibile), con
  l'account in attesa di eliminazione che azzera tutto. Questa è una **costrizione strutturale**: lo use case
  chiede una sola logica coerente fra le viste, e la coerenza per copia-e-incolla è ciò che ha prodotto la
  divergenza attuale.
- La **matrice entitlement della console admin** risponde secondo quella regola: include le app abilitate
  dalla baseline gratuita (oggi invisibili) e azzera gli entitlement degli account in attesa di eliminazione.
  Resta cross-account e in sola lettura, gated `platform-admin` — l'eccezione esplicita già documentata
  all'invariante di filtro per riga.
- Lo **stato minimale del polling post-checkout** deriva l'accesso dalla stessa regola invece che dal solo
  stato dell'abbonamento. Restano **due endpoint distinti** (il polling vuole una risposta minima su una
  singola app, il read-model un elenco completo con piano e limiti), ma con **una sola derivazione**.
- L'endpoint del tenant `GET /api/platform/v1/me/entitlements` **non cambia forma né percorso**: nessun
  secondo endpoint viene introdotto.

**Frontend (`frontend/`)**

- La sezione "Le tue app" della barra laterale distingue i quattro stati: **caricamento** (nessuna
  affermazione sul contenuto), **errore** (messaggio chiaro e azione per ritentare la lettura, senza
  ricaricare la pagina), **vuoto** (nessuna app attiva, con invito esplicito verso la pagina di
  fatturazione), **pronto** (le sole app abilitate).
- Le **guardie di rotta** trasportano anche la condizione di errore: con gli entitlement non leggibili, la
  rotta di un modulo mostra un messaggio di errore, **mai** il diniego "non hai accesso". Il diniego resta la
  risposta corretta quando gli entitlement sono noti e l'app non c'è.
- La lettura degli entitlement viene **rinfrescata** quando cambiano per azione dell'utente: attivazione
  post-acquisto completata, cambio piano, disdetta, ripresa. Nessuna rilettura periodica di fondo.
- I testi nuovi sono localizzati nelle lingue già gestite dal catalogo condiviso, come ogni testo della shell.

**Documentazione**

- Chiusura dei punti aperti dello use case 0077 che questa change decide (endpoint unico contro due
  endpoint, freschezza contro cache, forma della risposta) e rimando scritto per ciò che resta.

## Fuori scope

- **La catena di enforcement** lato servizio (UC 0027) e il rifiuto tipizzato "pagamento richiesto": la
  change verifica che la vista dell'utente e ciò che il servizio concede combacino, ma non modifica
  l'enforcement.
- **Il modello dati dell'abbonamento** e la pipeline che lo materializza (UC 0025).
- **La funzionalità di disabilitazione app** come lavoro proprio (UC 0076): qui si consuma soltanto lo stato
  dell'app come ingrediente della derivazione. Il comando amministrativo di cambio stato esiste già e non
  viene esteso.
- **Arricchimento della risposta dell'endpoint** con metadati ulteriori per la shell (fascia, stato
  dell'abbonamento nel menu): la forma attuale resta, ampliabile in seguito senza rompere i consumatori.
  Rimando tracciato nello use case 0077.
- **Aggiornamento in tempo reale del menu** verso eventi non originati dall'utente corrente (un
  amministratore disabilita un'app mentre la sessione è aperta): richiede un canale di notifica che oggi non
  esiste. Rimando tracciato nello use case 0077.
- **Estrazione dei componenti di stato nel sistema di design** (UC 0019): la shell continua a usare i propri.
- `infra/`: nessuna modifica.

## Criteri di accettazione

- [ ] Esiste **un solo punto** nel codice del core che decide se un account ha accesso a un'app; il
      read-model del tenant, la matrice admin e lo stato del polling post-checkout lo usano tutti, e nessuno
      dei tre riscrive quella condizione per conto proprio.
- [ ] Sugli **stessi dati**, la matrice entitlement della console admin e il read-model del tenant dicono la
      stessa cosa: un'app abilitata dal piano gratuito di baseline risulta abilitata anche in admin; un
      account in attesa di eliminazione risulta senza entitlement in entrambe le viste; un'app disabilitata
      dalla piattaforma non risulta abilitata in nessuna delle due. Verificato da un test di integrazione che
      confronta le due risposte.
- [ ] Lo stato usato dal polling post-checkout **non** dichiara attiva un'app disabilitata dalla piattaforma,
      anche con abbonamento in stato che concederebbe accesso.
- [ ] Con l'endpoint entitlement in errore, la shell mostra un **messaggio di errore con azione di riprova**
      e **non** afferma che l'account non ha app; la rotta di un modulo mostra l'errore e non il diniego di
      accesso. La riprova, riuscita, popola il menu senza ricaricare la pagina.
- [ ] Con l'endpoint che risponde un elenco vuoto, la shell mostra lo stato "nessuna app attiva" **con
      l'invito all'acquisto** verso la pagina di fatturazione — distinto, nel testo e nel ruolo di
      accessibilità, dallo stato di errore.
- [ ] Dopo l'attivazione post-acquisto e dopo cambio piano, disdetta e ripresa, la lettura degli entitlement
      viene invalidata: il menu riflette la nuova situazione senza ricaricare la pagina.
- [ ] Lo stub degli entitlement resta usato **solo** dai test e dallo sviluppo locale: nessun percorso
      dell'applicazione vi ricade come ripiego in caso di errore.
- [ ] `./run-tests.sh backend` e `./run-tests.sh frontend` verdi.

## Invarianti appgrove toccati

- **Tenant ID solo dal token verificato** — l'endpoint entitlement del tenant e lo stato del polling
  continuano a ricavare l'account esclusivamente dal claim verificato, mai da corpo o parametri. La regola di
  accesso condivisa non prende l'account come parametro dal chiamante nel percorso tenant: chi la usa per il
  tenant corrente lo ricava dal contesto già verificato. La matrice admin resta l'**eccezione esplicita**
  cross-account, ammessa solo per il ruolo `platform-admin` e in sola lettura.
- **Filtro riga per riga** — le letture degli abbonamenti nel percorso del tenant restano tenant-scoped.
  L'allargamento della matrice admin alla baseline gratuita non allenta il filtro nel percorso del tenant:
  agisce solo dentro la superficie admin già gated.
- **Modulo Terraform `microsaas_app`** — non pertinente: nessuna modifica infrastrutturale.
- **Logging strutturato** — le letture degli entitlement continuano a registrare `tenant_id` e `user_id`; il
  cambio di stato di un'app lato admin continua a registrare `app_id`, attore ed esito.

## Requisiti di test

- **Unità (backend)**: la regola di accesso condivisa, caso per caso — app attiva con abbonamento che
  concede accesso; app attiva senza abbonamento ma con piano gratuito di baseline; app attiva senza
  abbonamento e senza piano gratuito; app disabilitata con abbonamento valido; account in attesa di
  eliminazione.
- **Integrazione (backend, database reale)**: sugli stessi dati di partenza, l'insieme delle app abilitate
  visto dalla matrice admin coincide con quello del read-model del tenant; cambiando lo stato dell'app a
  disabilitata l'app sparisce da **entrambe** le viste e dallo stato del polling.
- **Isolamento fra account**: il test esistente resta verde e non viene indebolito.
- **Frontend (unità)**: la barra laterale distingue caricamento, errore (con riprova), vuoto (con invito
  all'acquisto) e pronto; la guardia di rotta con entitlement in errore non produce il diniego di accesso; le
  mutazioni di abbonamento e il completamento dell'attivazione invalidano la lettura degli entitlement.
- **End-to-end**: con l'endpoint entitlement in errore il menu non dichiara "nessuna app" e offre la riprova;
  con elenco vuoto mostra l'invito all'acquisto.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — nessun percorso o forma di risposta esistente viene rimosso o rinominato |
| Contratto cross-area | Sì — la matrice entitlement della console admin può contenere righe in più (app abilitate dalla baseline gratuita): la vista admin va verificata su questa forma |
| Version bump | minor |
