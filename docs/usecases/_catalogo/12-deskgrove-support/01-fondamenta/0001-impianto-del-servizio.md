# 0001 — Impianto del servizio

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio che l'applicazione dell'assistenza clienti esista come servizio a sé, avviabile e raggiungibile
> così da poter costruire il dominio sopra fondamenta uguali a quelle di tutte le altre app, invece che sopra
> un impianto inventato per l'occasione.

**Contesto.** Oggi DeskGrove Support non esiste: c'è una scheda di catalogo e un documento di proposta. Questa
storia porta in vita l'involucro — servizio, rotte, definizione delle interfacce, istanza di infrastruttura — e
nient'altro. Va fatta per prima perché ogni altra storia dell'app presuppone un posto dove mettere il codice.
Una applicazione **non si scaffolda a mano**: nasce dalla skill `new-application`, che esegue un generatore
deterministico e poi chiede le due decisioni che un generatore non può prendere (listino e dati personali). Se
l'esito del generatore è sbagliato si corregge il modello e si rigenera: non si toppa l'uscita, perché la toppa
nessuno se la ricorda e l'applicazione successiva eredita il difetto.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/helpdesk`, generato dalla skill `new-application` con
   l'identificativo `helpdesk`, e non da modifiche a mano ai modelli di partenza.
2. **RF-2** — Il servizio risponde su `/api/helpdesk/v1/*` e pubblica un punto di verifica dello stato che
   risponde senza richiedere un token.
3. **RF-3** — Il servizio dipende da `services/commons` (contesto dell'account, mappatura degli errori, entità di
   base con colonne di controllo, paginazione) e non ridefinisce nulla di ciò che quel modulo già offre.
4. **RF-4** — La definizione delle interfacce (OpenAPI) è generata e versionata nel repository, ed è la sorgente da
   cui il modulo frontend ricava il proprio client.
5. **RF-5** — L'infrastruttura dell'app nasce dall'istanza del modulo Terraform `microsaas_app` prodotta dallo
   script di aggiunta del servizio; nessuna risorsa scritta a mano, nessuna modifica manuale al blocco generato.
6. **RF-6** — `run-tests.sh` conosce il modulo nuovo: la suite completa lo compila e lo prova, e l'area `backend`
   resta verde.

## 3. Requisiti tecnici

- **RT-1 — Struttura del backend (§2).** Servizio Maven `services/helpdesk`, Quarkus 3.20.6 su Java 21, Quarkus
  REST con Hibernate ORM **bloccante** (la variante reattiva è vietata), accesso ai dati con il modello
  *repository*, pacchetto radice `app.appgrove.helpdesk`. Oggetti di trasferimento sempre al bordo, validazione
  dichiarativa, errori in `application/problem+json`, paginazione a pagina e dimensione con totale.
- **RT-2 — Comunicazione fra servizi (§2).** L'app **non chiama** altre app: l'unica via ammessa è asincrona a
  eventi. La storia non introduce alcuna chiamata di rete verso `core` o verso altre applicazioni.
- **RT-3 — Infrastruttura (§9).** L'istanza del modulo `microsaas_app` si crea con `infra/scripts/service-add`;
  l'infrastruttura si valida in continuo (formattazione, validazione, analisi statica, prove del modulo) e si
  applica solo dalla catena di integrazione continua, mai da un portatile.
- **RT-4 — Avvio locale (§15).** Le proprietà in `services/helpdesk/src/main/resources/application.properties`
  dichiarano identificativo dell'app, porta `8112` e schema `app_helpdesk`: la scoperta automatica dei servizi ne
  ricava da sola gli script di avvio, le migrazioni, le rotte del proxy locale e gli avvii di collaudo. **Nessuna
  riga incollata a mano** negli script.
- **RT-5 — Registrazione eventi (§14).** Ogni riga di registro porta `tenant_id`, `app_id`, `user_id` e
  l'identificativo di correlazione della richiesta; formato leggibile in sviluppo, strutturato in produzione;
  nessun dato personale nei registri.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: la storia non introduce tabelle né campi. Il
  manifesto `docs/compliance/manifests/helpdesk.yaml` nasce però qui, vuoto di voci e completo di intestazione in
  italiano e inglese, insieme allo scheletro del contratto dati `HelpdeskDataContract` con `appId()` che risponde
  `helpdesk`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: il contratto degli strumenti
  nasce con le funzioni, e le prime funzioni arrivano dall'epica 02. La storia predispone soltanto il posto dove
  vivrà — un pacchetto `tools` dentro il servizio.

## 4. Criteri di accettazione

**CA-1 — Il servizio si avvia**
- **Dato** un ambiente di sviluppo pulito
- **Quando** si lancia `./app-start.sh`
- **Allora** il servizio `helpdesk` risponde sulla porta `8112` e il punto di verifica dello stato restituisce
  `200`, senza che nessuno abbia modificato uno script a mano

**CA-2 — Scoperta automatica**
- **Dato** il solo file `application.properties` del servizio
- **Quando** si esegue `./dev.sh services`
- **Allora** l'elenco mostra `helpdesk` con porta `8112` e schema `app_helpdesk`, e il proxy locale espone
  `/api/helpdesk/v1/*`

**CA-3 — Errori tipizzati**
- **Dato** il servizio avviato
- **Quando** si chiama una rotta inesistente sotto `/api/helpdesk/v1/`
- **Allora** la risposta è `404` in formato `application/problem+json`, non una pagina di errore predefinita

**CA-4 — Infrastruttura generata, non scritta**
- **Dato** il repository dopo la change
- **Quando** si eseguono formattazione, validazione e prove del modulo di infrastruttura
- **Allora** sono verdi e l'unica aggiunta è l'istanza del modulo `microsaas_app`, senza risorse su misura

**CA-5 — La suite conosce il modulo**
- **Dato** il servizio nuovo
- **Quando** si esegue `./run-tests.sh backend`
- **Allora** il modulo `helpdesk` viene compilato e provato, e l'esito complessivo è verde

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `infra`; l'intera suite prima del commit);
- [ ] `run-tests.sh` aggiornato nello stesso commit, perché la change **aggiunge un modulo**;
- [ ] prove di **unità** sull'avvio e sulla mappatura degli errori;
- [ ] prova di **isolamento fra account**: non applicabile — nessuna risorsa che legga dati; la prima arriva con la
      storia `0002`;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-HELPDESK]` nasce con la storia `0037`, che possiede la voce
      del registro di copertura `docs/testing/copertura-e2e.yaml`; qui non c'è superficie utente da percorrere;
- [ ] **traduzioni**: non applicabile — nessun testo visibile (arrivano con la storia `0003`);
- [ ] **manifesto dei dati** creato con intestazione in italiano e inglese e nessuna voce, e contratto dati
      dell'app abbozzato;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotata in particolare la scelta
      dell'identificativo `helpdesk` e il perché non è `deskgrove`;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta, nessuno strumento da dichiarare;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] `docs/_PARITA-SCAFFOLD.md` consultato prima di generare, e aggiornato se la generazione ha rivelato una
      deviazione consapevole dei modelli di partenza.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Skill `new-application` (UC 0046) | È il solo modo ammesso per far nascere un'app: esegue il generatore deterministico e co-pilota le decisioni di listino e dati personali |
| Decisione sul listino (§5 del documento capofila) | La skill chiede piani, metrica e limiti **prima** di generare: senza la conferma dello sviluppatore la generazione si ferma |
| Decisione sui dati personali (§6 del documento capofila) | Stessa cosa: è la seconda fermata di escalation della skill |

## 7. Fuori ambito

- **Le tabelle del dominio**: le crea la storia `0002`.
- **Il modulo frontend**: lo crea la storia `0003`.
- **Il listino come file nel repository**: lo scrive la storia `0004`, dopo la conferma dei prezzi.
- **Qualunque funzione visibile all'utente**: qui non c'è nulla da vedere, ed è giusto così.

## 8. Punti aperti

- **Prezzi e limiti dei piani** (§5 del documento capofila): fermata di escalation dello sviluppatore. La skill
  `new-application` non genera finché non ha una risposta.
- **Classificazione dei dati personali** (§6): seconda fermata. Qui è più delicata che altrove, perché appgrove
  agisce come **responsabile del trattamento** per conto del cliente e non come titolare.
- **Porta locale definitiva**: `8112` è la proposta del kit di catalogo (8100 + numero di catalogo). Va confermata
  con `./dev.sh services` al momento della generazione, perché le porte già occupate le conosce solo il repository.
