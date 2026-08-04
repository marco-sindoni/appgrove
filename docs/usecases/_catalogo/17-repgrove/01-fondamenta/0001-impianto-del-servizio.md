# 0001 — Impianto del servizio

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica e dell'applicazione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio che il servizio `recensioni` esista, parta e risponda su una rotta propria
> così da poter costruire tutto il resto dell'app sopra una base generata dallo scaffolding e non scritta a mano.

**Contesto.** Oggi RepGrove non esiste: non c'è un servizio, non c'è uno schema, non c'è un modulo. Questa storia
è il momento in cui l'app nasce, e nasce **dalla skill `new-application`** — non si scaffolda a mano
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §16). Va fatta per prima perché tutte le altre storie
presuppongono che esistano il pacchetto `app.appgrove.recensioni`, la rotta `/api/recensioni/v1/*` e l'istanza del
modulo di infrastruttura. Prima di lanciarla vanno chiuse le due fermate di escalation della descrizione
dell'applicazione: listino (§5) e dati personali (§6).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il servizio `services/recensioni`, generato dalla skill `new-application` con le risposte del
   varco d'identità: identificativo `recensioni`, modello utente `multi`, porta `8117`, metrica `sedi_monitorate`
   a giacenza, colore-categoria `amber`.
2. **RF-2** — Il servizio espone una rotta di salute e almeno una rotta di dominio funzionante
   (`GET /api/recensioni/v1/sedi`, che al primo avvio restituisce un elenco vuoto), con paginazione a pagina e
   dimensione.
3. **RF-3** — Gli errori escono in `application/problem+json`; la definizione delle interfacce (OpenAPI) è generata
   e versionata nello stesso commit.
4. **RF-4** — L'infrastruttura dell'app nasce dall'istanza del modulo `microsaas_app` prodotta dallo scaffolding
   (via `infra/scripts/service-add`): nessuna risorsa scritta a mano, nessuna modifica manuale al blocco generato.
5. **RF-5** — `run-tests.sh` conosce il nuovo modulo: l'area `backend` lo compila e ne esegue le prove.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il contesto del tenant è quello di `services/commons`: `tenant_id`
  arriva **solo** dal token verificato; un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato.
  La rotta d'elenco è già scritta col filtro, anche se non ha ancora dati da filtrare.
- **RT-2 — Interfaccia di programmazione (§2).** Quarkus 3.20.6, Java 21, Quarkus REST + Hibernate ORM
  **bloccante**, accesso ai dati con il modello *repository*, pacchetto radice `app.appgrove.recensioni`. Rotte
  `/api/recensioni/v1/...`. Nessuna chiamata sincrona verso altre app.
- **RT-3 — Persistenza (§8).** Migrazione `V1__baseline.sql` sullo schema `app_recensioni`, con il ruolo del
  database dedicato al servizio. Le tabelle di dominio arrivano con la storia 0002.
- **RT-4 — Modulo frontend (§3, §5).** Fuori ambito qui: il guscio del modulo è la storia 0003.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile in questa storia.
- **RT-6 — Varchi e quota (§6, §7).** Il file di listino `pricing/recensioni.yaml` viene creato dalla skill con i
  valori confermati dallo sviluppatore; l'applicazione della quota è la storia 0004.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia; il contratto nasce nell'epica
  06 (casi d'uso di piattaforma 0061-0063, non ancora implementati).
- **RT-8 — Dati personali (§10).** Il manifesto `docs/compliance/manifests/recensioni.yaml` viene creato **vuoto
  di voci ma valido** (identificativo, nome e descrizione in italiano e inglese) e il contratto
  `RecensioniDataContract` nasce con `exportData` e `purgeData` che non hanno ancora tabelle da trattare. Le voci
  arrivano con le storie che introducono i campi.
- **RT-9 — Registrazione eventi (§14).** Ogni riga di registro porta `tenant_id`, `app_id`, `user_id` e
  l'identificativo di correlazione della richiesta; formato JSON in produzione, testo leggibile in sviluppo.

## 4. Criteri di accettazione

**CA-1 — Il servizio parte e risponde**
- **Dato** il ramo con lo scaffolding appena eseguito
- **Quando** si avvia lo stack locale e si chiama `GET /api/recensioni/v1/sedi` con un token valido
- **Allora** la risposta è `200` con un elenco vuoto e i campi di paginazione (pagina, dimensione, totale = 0)

**CA-2 — Errore in formato di piattaforma**
- **Dato** un token valido
- **Quando** si chiama una rotta inesistente sotto `/api/recensioni/v1/`
- **Allora** la risposta è `404` con corpo `application/problem+json` e nessuno stacco di eccezione nel corpo

**CA-3 — Il tenant non si forza dall'esterno**
- **Dato** un utente dell'account `A`
- **Quando** invia una richiesta che contiene `tenant_id` dell'account `B` nel corpo o in un'intestazione
- **Allora** il valore è ignorato e la richiesta è servita nel contesto di `A`

**CA-4 — L'infrastruttura è generata, non scritta**
- **Dato** il ramo della storia
- **Quando** si esegue la validazione dell'area `infra` di `run-tests.sh`
- **Allora** è verde e il blocco `module` di `recensioni` risulta prodotto da `infra/scripts/service-add`, senza
  modifiche manuali

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla logica introdotta e di **integrazione** sulla risorsa, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** sulla risorsa nuova;
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; la copre la storia 0030, proprietaria del
      percorso `[J-RECENSIONI]`. Voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: non applicabile (nessun testo visibile);
- [ ] **manifesto dei dati** creato e valido in italiano e inglese, ancora senza voci;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato con le scelte fatte e il perché,
      comprese le risposte alle due fermate di escalation (listino e dati personali);
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, dichiarato esplicitamente;
- [ ] `./dev.sh services` mostra `recensioni` con la porta `8117` e lo schema `app_recensioni`, e `./app-start.sh`
      la avvia senza modifiche manuali agli script;
- [ ] `run-tests.sh` aggiornato con il nuovo modulo nello stesso commit.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| decisione sul **listino** (descrizione §5) | la skill `new-application` chiede piani, prezzi e limiti prima di generare |
| decisione sui **dati personali** (descrizione §6), compreso il punto sui settori sanitari | la skill chiede la classificazione prima di generare il manifesto |

## 7. Fuori ambito

- le tabelle di dominio — storia 0002;
- il modulo frontend — storia 0003;
- l'applicazione della quota — storia 0004;
- qualunque collegamento a piattaforme esterne — epica 02.

## 8. Punti aperti

- La **porta 8117** è una convenzione del kit: va confermata con `./dev.sh services` al momento dello scaffolding.
- L'identificativo `recensioni` va verificato libero: nel repository esistono oggi `fatture` e `crm`.
- Resta aperta la decisione sui settori sanitari (descrizione §11.7): se venissero esclusi dal perimetro, la
  `Sede` nasce con un settore dichiarato e un elenco di settori non ammessi — cosa che cambia già la storia 0006.
</content>
