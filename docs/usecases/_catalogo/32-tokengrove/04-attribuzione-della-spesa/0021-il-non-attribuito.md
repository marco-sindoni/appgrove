# 0021 — Il non attribuito

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 04 — Attribuzione della spesa
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0020`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che guarda la spesa divisa per cliente
> voglio che la voce «non attribuito» sia piccola, e se è grande voglio sapere subito come rimpicciolirla
> così da poter davvero usare questi numeri per decidere, invece di guardare una torta con dentro una fetta enorme
> che non so cosa sia.

**Contesto.** È il difetto tipico di ogni sistema di attribuzione, in questo dominio come nel governo dei costi
della nuvola: si comincia con le migliori intenzioni e dopo tre mesi la voce più grande è «non attribuito», che
non serve a nessuno. La contromisura riconosciuta è **misurare la copertura come un numero in prima pagina** e
darsi un obiettivo: la pratica descritta dal settore prevede di restare in sola visibilità per quattro-sei settimane
e di passare al ribaltamento sulle squadre solo quando la copertura supera l'80% circa (§2.6, fonte 14). Questa
storia costruisce il numero e la via per farlo salire; la storia `0022` usa la soglia.

## 2. Requisiti funzionali

1. **RF-1** — La **copertura di attribuzione** — quota della spesa del periodo imputata a un valore su un dato
   asse — è calcolata per ogni asse e mostrata come indicatore, in percentuale, accanto ai totali.
2. **RF-2** — La schermata del non attribuito mostra da dove viene ciò che manca, ordinato per importo: quali
   chiavi del fornitore, quali fonti, quali modelli, e se manca perché non c'era etichetta o perché nessuna regola
   copriva il caso.
3. **RF-3** — Da ogni riga del non attribuito si arriva in un clic alla creazione della regola che la coprirebbe,
   con la condizione già compilata (storia `0020`).
4. **RF-4** — È possibile **applicare una regola allo storico** su un intervallo scelto: con anteprima del numero
   di misure e dell'importo che cambierebbero attribuzione, conferma esplicita e traccia dell'operazione.
   L'attribuzione precedente resta consultabile.
5. **RF-5** — L'andamento della copertura nel tempo è visibile: una copertura che scende è un segnale che qualcosa
   di nuovo è entrato in produzione senza etichette.
6. **RF-6** — La copertura è **calcolata sull'importo, non sul numero di chiamate**: mille chiamate da un
   millesimo di euro non devono nascondere una chiamata da cento euro.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo della copertura e l'applicazione allo storico agiscono sul
  solo `tenant_id` preso dal gettone verificato.
- **RT-2 — Persistenza (§8).** L'attribuzione di una misura è versionata come lo è il costo (storia `0017`):
  applicare una regola allo storico **non sovrascrive** l'attribuzione precedente ma ne aggiunge una nuova
  revisione, così che si possa sempre mostrare come erano i conti quando il cliente li ha guardati.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `GET /api/spesa_modelli/v1/copertura`,
  `GET /api/spesa_modelli/v1/non-attribuito`, `POST /api/spesa_modelli/v1/regole/{id}/applica-allo-storico` con
  anteprima obbligatoria; errori in `problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-4 — Varchi, ruoli e quota (§6, §7).** L'applicazione allo storico è riservata a `owner` e `admin` e **non
  consuma** la metrica `misure_registrate`, per la stessa ragione del ricalcolo (storia `0017`): non registra
  misure nuove.
- **RT-5 — Modulo frontend (§3, §5).** L'indicatore di copertura sta nella panoramica, accanto al totale; la
  schermata del non attribuito è nella sezione «Attribuzione». Solo token del sistema di design; tema chiaro e
  scuro.
- **RT-6 — Cinque lingue (§4).** «Non attribuito» e «copertura di attribuzione» sono termini che vanno tradotti
  con cura in `en, it, fr, es, de`: sono i due che il cliente ripeterà nelle proprie riunioni.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `leggi_spesa` restituisce **sempre** la copertura
  insieme al totale (storia `0032`), e `applica_regola_allo_storico` è marcato **scrittura irreversibile** con
  conferma obbligatoria e anteprima del numero di righe toccate (storia `0033`).
- **RT-8 — Dati personali (§10).** Nessuna categoria nuova; le revisioni dell'attribuzione entrano in `exportData`
  e `purgeData` insieme alla misura.
- **RT-9 — Registrazione eventi (§14).** Eventi «copertura scesa sotto la soglia», «regola applicata allo storico
  su N misure» con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — La copertura è visibile**
- **Dato** un account con il 72% della spesa del mese attribuita all'asse «cliente»
- **Quando** apre la panoramica
- **Allora** vede «copertura di attribuzione 72%» accanto al totale, con il rimando alla schermata del non
  attribuito

**CA-2 — Da dove viene ciò che manca**
- **Dato** una spesa non attribuita concentrata su una chiave del fornitore
- **Quando** apre la schermata del non attribuito
- **Allora** vede quella chiave in cima con il proprio importo e il motivo «nessuna regola la copre», e un pulsante
  che apre la creazione della regola con la condizione già compilata

**CA-3 — Applicazione allo storico con anteprima**
- **Dato** una regola nuova e un intervallo di due mesi
- **Quando** si chiede l'applicazione allo storico
- **Allora** si vedono numero di misure e importo che cambierebbero attribuzione, e nulla cambia finché non si
  conferma

**CA-4 — L'attribuzione precedente resta consultabile**
- **Dato** un'applicazione allo storico confermata
- **Quando** si apre il dettaglio di una misura toccata
- **Allora** si vede l'attribuzione corrente e quella precedente, con la data del cambiamento

**CA-5 — Copertura sull'importo, non sul numero**
- **Dato** 1.000 chiamate da 0,001 € attribuite e 1 chiamata da 100 € non attribuita
- **Quando** si legge la copertura
- **Allora** è circa dell'1%, non del 99,9%

**CA-6 — Isolamento fra account**
- **Dato** due account con coperture diverse
- **Quando** ciascuno legge la propria
- **Allora** i numeri non si influenzano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della copertura per importo e sulle revisioni dell'attribuzione, e di
      **integrazione** sull'applicazione allo storico;
- [ ] prova di **isolamento fra account** sulla copertura e sull'applicazione allo storico;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «copertura bassa, creo la
      regola suggerita, la applico allo storico, la copertura sale», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: le revisioni dell'attribuzione in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla copertura calcolata per importo e sulla
      versionatura dell'attribuzione;
- [ ] contratto degli **strumenti conversazionali** aggiornato per `applica_regola_allo_storico`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0019` | Serve l'attribuzione dalle etichette |
| Storia `0020` | Servono le regole da suggerire e da applicare |

## 7. Fuori ambito

- il ribaltamento del costo sulle squadre: è la storia `0022`, che usa la copertura come condizione;
- il ricalcolo del **costo** (quanto), che è cosa diversa dalla riattribuzione (a chi): è la storia `0017`.

## 8. Punti aperti

- **La soglia a cui la copertura è considerata sufficiente.** La pratica di settore indica circa l'80% (§2.6,
  fonte 14), ma è una convenzione, non una legge, e in un'azienda di cinque persone può essere diversa. Proposta:
  soglia predefinita all'80%, modificabile dall'account, e usata come condizione dalla storia `0022`. La conferma
  lo sviluppatore.
