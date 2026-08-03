# 0004 — Abbonamento e quota

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa
> voglio sapere quanti documenti mi restano nel mese e cosa succede quando finiscono
> così da non scoprire il limite nel momento peggiore, cioè mentre sto trasmettendo una fattura in scadenza.

**Contesto.** Ogni documento che passa da InvoiceGrove ha un costo verso un fornitore esterno (da 7 a 25
centesimi, descrizione dell'applicazione §2.2): la quota non è un espediente commerciale, è la traduzione di un
costo reale. Questa storia mette in piedi la catena dei varchi e la metrica `documenti` **prima** che esista
qualunque funzione che la consuma, perché aggiungere il conteggio dopo significa dimenticarsi di un percorso.

Va fatta adesso anche per un secondo motivo, meno ovvio: qui si stabilisce la **modalità prova**, cioè il fatto
che un abbonamento in prova valida e converte ma **non trasmette all'autorità**. È una regola di sicurezza, non
una limitazione commerciale, e va nel codice prima che esista la trasmissione.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio legge l'abilitazione dell'account dalla **proiezione locale** alimentata a eventi, mai
   con una chiamata di rete sincrona all'app centrale sul percorso caldo.
2. **RF-2** — La metrica `documenti` conta i documenti **trasmessi o ricevuti** in un mese di calendario, e si
   azzera all'inizio del mese successivo.
3. **RF-3** — Prima di un'operazione che consuma quota, il servizio **prenota** una unità; se il tetto è
   raggiunto risponde `429` con un messaggio che dice cosa è successo, cosa non si può più fare e come si rimedia.
4. **RF-4** — Con abbonamento in stato `trialing`, `active` o `past_due` l'app è accessibile; con `paused` o
   `canceled` risponde `402`.
5. **RF-5** — In stato `trialing` (modalità prova) le operazioni **con effetto verso l'esterno** sono negate con
   un errore dedicato e un messaggio che spiega la ragione; validazione, conversione e anteprima restano
   disponibili.
6. **RF-6** — Il modulo frontend mostra il consumo corrente («38 di 50 documenti, questo mese») e un avviso
   quando il consumo supera l'80% del tetto.
7. **RF-7** — I **diritti dell'interessato** — esportazione e cancellazione dei dati — restano accessibili anche
   con app disabilitata o abbonamento scaduto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il conteggio della quota è per `tenant_id` preso dal token verificato;
  un account non può vedere né consumare la quota di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Il `429` e il `402` escono in `application/problem+json` con un
  tipo di problema stabile e leggibile da un programma (serve al livello conversazionale, epica 06).
- **RT-3 — Persistenza (§8).** Migrazione `V2__quota_and_entitlement.sql`: tabella di conteggio del consumo per
  account e finestra, e proiezione locale dell'abilitazione. `tenant_id`, chiave UUID versione 7, colonne di
  controllo.
- **RT-4 — Modulo frontend (§3, §5).** La panoramica mostra la barra di consumo e l'avviso di quota con i soli
  token del sistema di design, in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I messaggi di quota e di abbonamento passano dallo spazio-nomi `einvoicing` e
  sono presenti in `en, it, fr, es, de`. Sono fra i testi più letti dell'app: se ne manca uno si vede subito.
- **RT-6 — Varchi e quota (§6, §7).** La catena completa: `401` senza token, `403` ad app spenta, `402` senza
  abilitazione, `403` a ruolo insufficiente, `429` a quota esaurita. La metrica è `documenti`, natura **`flow`**
  su finestra mensile. Al limite si **blocca**, non si addebita.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento qui, ma il tipo di problema del `429` va
  progettato perché un agente lo sappia interpretare e riferire (dipendenza dichiarata: UC 0064, applicazione di
  abilitazione e quota alle chiamate dell'assistente, non implementata).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il conteggio è per account, non per persona.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `quota prenotata`, `documento respinto per quota` e
  `operazione negata in modalità prova` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Consumo e residuo visibili**
- **Dato** un account sul piano con tetto 50 e 38 documenti già consumati nel mese
- **Quando** apre la panoramica
- **Allora** legge «38 di 50 documenti, questo mese» e vede l'avviso di avvicinamento al limite

**CA-2 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `documenti`
- **Quando** tenta un'operazione che consuma quota
- **Allora** riceve `429`, un messaggio che spiega come rimediare, e **nulla viene creato né trasmesso**

**CA-3 — La finestra si azzera**
- **Dato** un account che ha esaurito la quota a fine mese
- **Quando** comincia il mese successivo
- **Allora** il consumo riparte da zero e l'operazione riesce

**CA-4 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled`
- **Quando** chiama una qualunque rotta di dominio dell'app
- **Allora** riceve `402`; ma l'esportazione dei propri dati resta accessibile e risponde `200`

**CA-5 — Modalità prova**
- **Dato** un account in stato `trialing`
- **Quando** tenta un'operazione con effetto verso l'esterno
- **Allora** riceve un errore dedicato che spiega che in prova non si trasmette all'autorità, mentre la
  validazione dello stesso documento riesce

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con consumi diversi
- **Quando** un utente di `A` chiede il proprio consumo forzando l'identificativo di `B`
- **Allora** vede il proprio, non quello di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio e sulla finestra, di **integrazione** sulla catena dei varchi;
- [ ] prova di **isolamento fra account** sul conteggio della quota;
- [ ] prove dei **pagamenti** di livello 1 (eventi del fornitore con carichi sintetici firmati) sugli stati che
      danno e tolgono accesso;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-EINVOICING]` nasce con la storia `0030`, che coprirà anche
      il blocco per quota;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per i messaggi di quota e abbonamento;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con la scelta della natura `flow` e la modalità prova motivate;
- [ ] listino `services/core/src/main/resources/pricing/einvoicing.yaml` scritto e registrato in `pricing/index.yaml`,
      **con i valori confermati dallo sviluppatore**.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | Serve il servizio |
| `0002` | Servono schema e colonne di controllo |
| `0003` | Serve il modulo frontend che mostra il consumo |
| Decisione dello sviluppatore sul listino | Prezzi, tetti e durata della prova sono una fermata di escalation (descrizione dell'applicazione §5) |
| UC 0064 (quota per le chiamate dell'assistente), non implementato | Il `429` verso il livello conversazionale sarà governato lì; qui si prepara solo il tipo di problema |

## 7. Fuori ambito

- Il flusso di acquisto e di disdetta: è della piattaforma, non dell'app.
- La deroga di quota concessa in assistenza: sta nella console di amministrazione
  ([estensioni-admin.md](../estensioni-admin.md)).
- La decisione su cosa succede all'archivio dopo la disdetta: storia `0026`.

## 8. Punti aperti

- **Prezzi, tetti e durata della prova sono una fermata di escalation dello sviluppatore.** La proposta è nella
  descrizione dell'applicazione §5 (piani `italia` €12, `europa` €29, `studio` €69; nessun piano gratuito; prova
  di 14 giorni in modalità prova), con i conti di margine in chiaro. Non si implementa il listino prima della
  conferma.
- **Il conflitto struttura/metrica va ricordato qui**: la quota è un consumo mensile, ma la conservazione è una
  giacenza decennale. Chi disdice smette di consumare e continua a costare. La storia `0026` è la proprietaria di
  questo punto; qui si annota perché non venga perso.
