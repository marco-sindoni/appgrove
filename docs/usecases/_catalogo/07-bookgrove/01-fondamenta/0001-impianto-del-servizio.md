# 0001 — Impianto del servizio

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio che l'applicazione delle prenotazioni esista come servizio riconosciuto dal monorepo
> così da poter aggiungere la prima funzione senza dover prima inventare l'impalcatura.

**Contesto.** Oggi `prenotazioni` non esiste da nessuna parte: né come servizio, né come schema, né come istanza
di infrastruttura. Questa storia non consegna niente a un utente finale, ma è la sola che può stare per prima:
tutte le altre trentatré presuppongono un servizio avviabile. Non si scaffolda a mano — la skill
`new-application` esegue il generatore deterministico e poi co-pilota le due decisioni che un generatore non può
prendere, listino e dati personali, per le quali questa cartella porta già le proposte (sezioni 5 e 6 della
descrizione dell'applicazione).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/prenotazioni`, con pacchetto radice `app.appgrove.prenotazioni`, che
   compila e si avvia.
2. **RF-2** — Il servizio espone una rotta di stato sotto `/api/prenotazioni/v1/` che risponde senza errori con
   un token valido e `401` senza token.
3. **RF-3** — L'identità dell'app è quella decisa nel varco d'identità: identificativo `prenotazioni`, modello
   utente `multi`, porta locale `8107`, colore-categoria `green`, metrica `risorse_prenotabili` a giacenza.
4. **RF-4** — L'infrastruttura dell'app nasce dall'istanza del modulo `microsaas_app` prodotta dallo
   scaffolding, senza nessuna risorsa scritta a mano.
5. **RF-5** — `run-tests.sh` conosce il nuovo modulo e lo esegue nell'area `backend`.

## 3. Requisiti tecnici

- **RT-1 — L'app non si scaffolda a mano (§16).** Si usa la skill `new-application`; se l'esito del generatore è
  sbagliato si corregge il modello e si rigenera, non si toppa l'uscita.
- **RT-2 — Struttura del backend (§2).** Quarkus 3.20.6, Java 21, Quarkus REST più Hibernate ORM **bloccante**,
  accesso ai dati con il modello *repository*, dipendenza da `services/commons`; rotte `/api/prenotazioni/v1/*`;
  errori in `application/problem+json`; definizione OpenAPI generata e versionata.
- **RT-3 — Infrastruttura (§9).** Istanza del modulo Terraform `microsaas_app` tramite `infra/scripts/service-add`;
  nessuna infrastruttura parallela.
- **RT-4 — Avvio locale (§15).** Le proprietà in `services/prenotazioni/src/main/resources/application.properties`
  dichiarano identificativo, porta e schema: la scoperta automatica dei servizi fa il resto. Nessuna riga incollata
  a mano negli script di avvio.
- **RT-5 — Registrazione eventi (§14).** Il registro tecnico esce già con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, in formato leggibile in sviluppo e strutturato in produzione.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: questa storia non introduce campi che riguardino
  una persona. Il manifesto `docs/compliance/manifests/prenotazioni.yaml` nasce vuoto ma **esiste**, con nome e
  descrizione dell'app in italiano e inglese.
- **RT-7 — Prove (§11).** Prova di integrazione che avvia il servizio e verifica la rotta di stato con e senza
  token.

## 4. Criteri di accettazione

**CA-1 — Il servizio esiste e risponde**
- **Dato** il monorepo con la change unita
- **Quando** si avvia lo stack locale
- **Allora** `services/prenotazioni` si avvia sulla porta `8107` e la rotta di stato risponde

**CA-2 — Nessun accesso senza token**
- **Dato** il servizio avviato · **Quando** si chiama la rotta senza token · **Allora** risponde `401` in
  `problem+json`

**CA-3 — Infrastruttura dal modulo comune**
- **Dato** la cartella `infra/` · **Quando** si controlla come nasce l'app · **Allora** esiste una sola istanza
  del modulo `microsaas_app` e nessuna risorsa scritta a mano, e `terraform validate` è verde

**CA-4 — La suite conosce il modulo**
- **Dato** `./run-tests.sh backend` · **Quando** lo si esegue · **Allora** compila ed esegue anche
  `services/prenotazioni`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `infra`; l'intera suite prima del commit);
- [ ] prova di **integrazione** sulla rotta di stato con database effimero;
- [ ] prova di **isolamento fra account**: non applicabile, non c'è ancora nessuna risorsa con dati — lo dice
      esplicitamente il registro delle decisioni;
- [ ] **prova end-to-end**: *nessun impatto* — non c'è ancora superficie utente; il percorso `[J-BOOKGROVE]` nasce
      con la storia `0033` e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato lì;
- [ ] **traduzioni**: non applicabile, nessun testo visibile;
- [ ] **manifesto dei dati** creato, in italiano e inglese, ancora senza voci;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato con identificativo, porta, modello
      utente, metrica e colore, con il perché di ciascuno;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta, quindi nessuno strumento;
- [ ] `./dev.sh services` mostra `prenotazioni` con porta `8107` e schema `app_prenotazioni`, e `./app-start.sh`
      la avvia senza modifiche manuali agli script;
- [ ] `run-tests.sh` aggiornato nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| decisione dello sviluppatore sul **listino** (§5 della descrizione) | la skill `new-application` la chiede prima di generare |
| decisione dello sviluppatore sui **dati personali** (§6 della descrizione) | idem, ed è la fermata più delicata di questa app |

## 7. Fuori ambito

- il modello dati: storia `0002`;
- il modulo frontend: storia `0003`;
- l'applicazione della quota: storia `0004`.

## 8. Punti aperti

**Porta locale.** `8107` è la convenzione del kit di catalogo, non una verifica: al momento dello scaffolding va
confermata con `./dev.sh services`, che elenca quelle già prese. Se è occupata la si cambia qui e in un solo
altro posto, il file delle proprietà.
