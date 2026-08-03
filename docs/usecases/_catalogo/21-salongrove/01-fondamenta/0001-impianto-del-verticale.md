# 0001 — Impianto del verticale

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 01 — Fondamenta
**Storia**: `0001` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: nessuna: è la prima dell'epica — ma **è bloccata** dalla decisione del §0 della descrizione
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio che il verticale beauty abbia un impianto tecnico deciso e generato, non improvvisato
> così da non scoprire a metà lavoro di aver duplicato un motore di prenotazione senza accorgermene.

**Contesto.** Questa storia esiste per mettere in pratica la decisione del §0 della
[descrizione dell'applicazione](../application-description.md), ed è l'unica dell'intera cartella il cui contenuto
cambia a seconda di come quella decisione va. Non si comincia da nessun'altra parte: se si scrivesse prima una
qualsiasi delle storie di dominio, la si scriverebbe dentro uno dei due mondi senza averlo scelto. La
raccomandazione dell'autore è la **via (b)** — verticale di BookGrove — ma **la scelta è dello sviluppatore** ed è
la prima cosa che questa storia deve registrare in `decisions.json`.

## 2. Requisiti funzionali

1. **RF-1** — La decisione del §0 è **presa, scritta e motivata** nel registro delle decisioni della change, con
   la via scelta e il perché: è il primo atto della storia, prima di qualunque comando.
2. **RF-2** — *Se via (a)*: l'applicazione `salone` esiste come servizio, generata dalla skill `new-application`
   (non a mano), con la sua istanza del modulo di infrastruttura, la sua rotta pubblica e la sua definizione delle
   interfacce vuota ma valida. *Se via (b)*: nessuna applicazione nuova; il servizio `prenotazioni` acquisisce un
   pacchetto `salone` e le rotte `/api/prenotazioni/v1/salone/*`.
3. **RF-3** — Il verticale risponde a una chiamata di verifica dello stato e compare fra i servizi scoperti
   automaticamente, senza che nessuno abbia modificato a mano uno script di avvio.
4. **RF-4** — Il documento [docs/_PARITA-SCAFFOLD.md](../../../../_PARITA-SCAFFOLD.md) è aggiornato se la via
   scelta introduce una **deviazione consapevole** rispetto ai modelli di scaffolding (sotto la via (b) è quasi
   certo: un verticale dentro un'app esistente non nasce dal generatore).
5. **RF-5** — `run-tests.sh` conosce il modulo nuovo (via a) o continua a coprire quello esistente (via b), e resta
   l'unico punto d'ingresso per «lanciare tutti i test».

## 3. Requisiti tecnici

- **RT-1 — Infrastruttura (§9).** *Via (a)*: l'infrastruttura nasce dall'istanza del modulo Terraform
  `microsaas_app` prodotta dallo scaffolding, tramite `infra/scripts/service-add`; nessuna risorsa scritta a mano,
  nessuna modifica manuale al blocco generato. *Via (b)*: nessuna infrastruttura nuova — è il risparmio più
  visibile della via raccomandata.
- **RT-2 — Struttura del backend (§2).** Quarkus 3.20.6, Java 21, Quarkus REST e Hibernate ORM **bloccante**;
  pacchetto radice `app.appgrove.salone` (via a) oppure `app.appgrove.prenotazioni.salone` (via b); oggetti di
  trasferimento al bordo, errori in `application/problem+json`, definizione OpenAPI generata e versionata.
- **RT-3 — L'app non chiama un'altra app (§2).** Vincolo da verificare **espressamente in questa storia** sotto la
  via (a): la tentazione di far leggere a `salone` i servizi e i clienti di `prenotazioni` è immediata e sarebbe
  una violazione dell'invariante. L'unica via ammessa è asincrona a eventi.
- **RT-4 — Avvio locale automatico (§15).** `./dev.sh services` mostra il verticale con la sua porta e il suo
  schema; `./app-start.sh` lo avvia senza modifiche manuali agli script. Sotto la via (a) la porta proposta è
  `8121`, da confermare con `./dev.sh services`.
- **RT-5 — Registrazione eventi (§14).** L'avvio del servizio e la risposta alla verifica dello stato portano
  `app_id` e identificativo di correlazione.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: questa storia non crea tabelle di dominio.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: il contratto si dichiara
  nell'epica 07. Va però **fissato adesso il prefisso stabile** dei nomi degli strumenti (`salone.*`), perché
  cambiarlo dopo significa cambiare i nomi che un assistente ha già imparato.

## 4. Criteri di accettazione

**CA-1 — La decisione è tracciata**
- **Dato** una change che apre questa storia
- **Quando** la si esamina a lavoro finito
- **Allora** `decisions.json` contiene una voce che dice quale via è stata scelta fra (a) e (b), con la motivazione
  e le conseguenze accettate, e nessuna riga di codice la contraddice

**CA-2 — Il servizio risponde**
- **Dato** lo stack locale avviato con `./app-start.sh`
- **Quando** si interroga la verifica di stato del verticale
- **Allora** risponde correttamente e il servizio compare nell'elenco di `./dev.sh services`

**CA-3 — Nessun cablaggio a mano**
- **Dato** il ramo della change
- **Quando** si esaminano le differenze
- **Allora** nessuno script di avvio, nessun file di rotte del proxy locale e nessun elenco di servizi è stato
  modificato a mano: tutto discende dalle proprietà dichiarate in `application.properties`

**CA-4 — Nessuna chiamata fra applicazioni** *(solo via a)*
- **Dato** il servizio `salone`
- **Quando** si cerca nel codice una chiamata di rete verso `prenotazioni`
- **Allora** non ce n'è nessuna, e il collaudo strutturale lo verifica

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite intera: la storia tocca infrastruttura, backend e strumenti);
- [ ] prove di **unità** sulla configurazione e di **integrazione** sulla verifica di stato;
- [ ] prova di **isolamento fra account**: non applicabile, non ci sono ancora risorse — dichiarato, non taciuto;
- [ ] **prova end-to-end**: *nessun impatto* — non c'è superficie utente in questa storia; il percorso
      `[J-SALONGROVE]` nasce nella storia `0030`, con la voce nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni**: non applicabile, nessun testo visibile;
- [ ] **manifesto dei dati**: creato vuoto e valido (italiano e inglese) se via (a); invariato se via (b);
- [ ] **registro delle decisioni** compilato con la scelta del §0, la porta, l'identificativo e il prefisso degli
      strumenti conversazionali;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] [docs/_PARITA-SCAFFOLD.md](../../../../_PARITA-SCAFFOLD.md) aggiornato se la via scelta devia dai modelli.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| **la decisione del §0** della descrizione | è ciò che questa storia mette in pratica; senza, non si può cominciare |
| skill `new-application` (UC 0046) | sotto la via (a) è il solo modo ammesso di creare un'app |
| app 07 BookGrove | sotto la via (b) dev'essere già costruita: SalonGrove estende un'app viva, non una prevista |

## 7. Fuori ambito

- le tabelle di dominio: storia `0002`;
- il modulo frontend: storia `0003`;
- il listino e la quota: storia `0004`;
- la decisione su prezzi e dati personali: sono fermate di escalation e stanno nella descrizione, non qui.

## 8. Punti aperti

**È la storia che porta il punto aperto numero 1 dell'applicazione**, e non lo risolve: lo **presenta**. Sotto la
via (a) resta aperta anche la domanda se estrarre prima il motore di prenotazione in una libreria condivisa
(`services/commons-booking`): sarebbe una decisione di piattaforma, non di questa change, e va posta prima di
scrivere la prima riga di un secondo motore.
