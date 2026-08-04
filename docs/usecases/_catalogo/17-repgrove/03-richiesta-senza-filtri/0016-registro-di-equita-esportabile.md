# 0016 — Registro di equità esportabile

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 03 — Richiesta di recensione senza filtri
**Storia**: `0016` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0014`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare a cui una piattaforma contesta di aver invitato solo i clienti contenti
> voglio poter tirare fuori un documento che mostra chi ho invitato, chi no e perché
> così da rispondere con dei fatti invece che con delle affermazioni.

**Contesto.** È la funzione che nessun cliente chiede e che protegge tutti (descrizione §2.5). Le piattaforme
hanno strumenti automatici che rimuovono le recensioni non conformi e possono arrivare a mettere un avviso sul
profilo o a sospenderlo (descrizione §2.3). Quando succede, la domanda è: «come hai raccolto queste recensioni?».
Le tre storie precedenti producono già tutti i dati per rispondere — regola applicata, chi è stato selezionato,
chi escluso e con quale motivo dall'elenco chiuso — ma i dati sparsi in tre tabelle non sono una risposta. Questa
storia li rende **un documento**.

Va fatta adesso e non «un giorno»: se il registro nasce dopo, i primi mesi di attività restano scoperti proprio
nel periodo in cui il cliente sta imparando a usare lo strumento ed è più probabile che sbagli.

## 2. Requisiti funzionali

1. **RF-1** — Per una sede e un periodo, l'app produce un registro che contiene: la regola di equità in vigore in
   quel periodo (con le sue eventuali successioni), il numero di servizi erogati, il numero di invitati, il numero
   di esclusi **spezzato per motivo**, e la riga per riga con momento, esito e motivo.
2. **RF-2** — Il registro è **esportabile** in un formato leggibile da una persona e in uno leggibile da un
   programma; entrambi portano la sede, il periodo, il momento di generazione e chi l'ha generato.
3. **RF-3** — Il registro esiste in due forme: **con i recapiti** (per uso interno) e **senza** (anonimizzata sui
   destinatari, con soli conteggi e motivi), da usare quando si risponde a una piattaforma. La seconda è quella
   proposta per difetto: mandare a un terzo l'elenco dei propri clienti sarebbe un trattamento in più che nessuno
   ha chiesto.
4. **RF-4** — Nessun motivo di esclusione può riguardare la soddisfazione del cliente: il registro **verifica** la
   proprietà e, se trovasse un motivo fuori dall'elenco chiuso, lo segnalerebbe come anomalia invece di stamparlo.
   È un controllo che non dovrebbe mai scattare, e proprio per questo va scritto.
5. **RF-5** — Il registro copre anche le richieste `scadute` per chiusura della finestra (storia 0015), perché
   «non l'ho invitato perché era tardi» è una spiegazione che va data.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La generazione filtra per `tenant_id` preso dal token verificato; non
  esiste un registro che attraversi due account, nemmeno per un amministratore di piattaforma.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta
  `GET /api/recensioni/v1/sedi/{id}/registro-equita?dal=&al=&conRecapiti=`; l'esportazione grande passa da una
  lavorazione asincrona con esito scaricabile; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: il registro si compone da `regola_di_equita`,
  `servizio_erogato` e `richiesta_recensione`. Se la generazione risultasse costosa, si aggiunge un indice, non
  una tabella di riepilogo: un riepilogo che si può disallineare dai fatti sarebbe una prova debole.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Richieste* → «Registro di equità»: scelta di sede e periodo,
  riepilogo a numeri, tabella, due pulsanti di esportazione con la differenza fra le due forme spiegata in una
  riga. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Interfaccia ed **etichette del documento esportato** in `en, it, fr, es, de`: un
  registro che si esporta solo in italiano non serve al cliente francese che deve rispondere a una piattaforma.
- **RT-6 — Varchi e quota (§6, §7).** Generare il registro richiede ruolo `admin` o `owner`. **Resta accessibile
  anche con abbonamento `canceled`**: è materiale di prova del cliente, non una funzione a pagamento — stessa
  logica dei diritti dell'interessato.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `stato_delle_richieste` (storia 0027) restituisce la
  forma sintetica del registro: conteggi, motivi e regola applicata, **senza recapiti**. L'esportazione completa
  resta un'azione dell'interfaccia.
- **RT-8 — Dati personali (§10).** L'esportazione con recapiti è un trattamento: va tracciata (chi l'ha generata e
  quando) e va detto nel manifesto che questi dati escono dall'app in quella forma. La forma senza recapiti è
  aggregata e non contiene persone.
- **RT-9 — Registrazione eventi (§14).** `registro generato` con sede, periodo e forma, `esportazione scaricata`,
  con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — Il registro racconta il periodo**
- **Dato** una sede con regola `tutti`, dieci servizi erogati, otto invitati e due esclusi (uno senza recapito,
  uno «non contattare»)
- **Quando** si genera il registro del mese
- **Allora** i numeri tornano, i due esclusi hanno il proprio motivo e la regola in vigore è riportata

**CA-2 — Cambio di regola dentro il periodo**
- **Dato** una sede che il giorno 15 è passata da `tutti` a `uno ogni 2`
- **Quando** si genera il registro del mese
- **Allora** il documento mostra entrambe le regole con le rispettive decorrenze e i conteggi separati

**CA-3 — Forma senza recapiti**
- **Dato** un registro generato nella forma predefinita
- **Quando** lo si esporta
- **Allora** non contiene nessun nome e nessun recapito, solo conteggi, motivi e momenti

**CA-4 — Anomalia sui motivi**
- **Dato** una richiesta con un motivo di esclusione fuori dall'elenco chiuso (situazione che non deve accadere)
- **Quando** si genera il registro
- **Allora** il documento segnala l'anomalia in modo visibile invece di stamparla come motivo normale

**CA-5 — Isolamento fra account**
- **Dato** due account con sedi omonime
- **Quando** un utente di `A` genera il registro
- **Allora** contiene solo dati di `A`, anche forzando l'identificativo della sede di `B`

**CA-6 — Resta accessibile con abbonamento scaduto**
- **Dato** un account con abbonamento `canceled`
- **Quando** chiede il registro di equità di un periodo passato
- **Allora** lo ottiene, anche se le altre funzioni rispondono `402`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sui conteggi e sul controllo dell'elenco chiuso dei motivi; di **integrazione** sulla
      generazione con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla generazione del registro;
- [ ] **prova end-to-end**: *coprire ora* il passo «esporto il registro di equità e i numeri tornano» nel percorso
      `[J-RECENSIONI]`, e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, etichette del documento comprese;
- [ ] **manifesto dei dati** aggiornato con l'uscita dei dati nella forma con recapiti;
- [ ] **registro delle decisioni** compilato, con la scelta della forma senza recapiti come predefinita;
- [ ] controllo automatico di **accessibilità** verde sulle schermate introdotte.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | la regola è la prima riga del documento |
| storia `0014` | gli invii e i loro esiti sono il corpo del documento |
| storia `0015` | le scadenze della finestra sono un motivo di esclusione da spiegare |

## 7. Fuori ambito

- l'invio del registro a una piattaforma: lo fa il cliente, con i suoi canali. L'app produce il documento, non lo
  trasmette a terzi;
- la conservazione a norma del documento: è un tema di piattaforma, non di questa app.

## 8. Punti aperti

- **Quanto indietro deve andare il registro.** Dipende dalla conservazione dei dati d'origine (proposta: 36 mesi
  per la prova dell'invito, descrizione §6): un registro non può raccontare un periodo di cui i dati sono stati
  cancellati. La conseguenza pratica va spiegata al cliente nell'interfaccia.
- **Se convenga generare un riepilogo mensile immutabile** al chiudersi di ogni mese, così da avere una prova
  stabile anche dopo la cancellazione dei dati puntuali. Ha senso, ma introduce una conservazione in più: non la
  decido qui.
</content>
