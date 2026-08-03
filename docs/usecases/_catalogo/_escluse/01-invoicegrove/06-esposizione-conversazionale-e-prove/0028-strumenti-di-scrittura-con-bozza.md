# 0028 — Strumenti di scrittura con bozza

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0020`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio poter dire «prepara una fattura per il cliente Alfa di 1.200 euro per la consulenza di luglio» e
> **rivederla** prima che esista davvero
> così da risparmiare la digitazione senza cedere il controllo di quello che porta la mia partita IVA.

**Contesto.** È il primo passo in cui un agente **scrive**, e la regola della piattaforma è netta e non
negoziabile: gli strumenti di scrittura producono una **bozza** e richiedono una **conferma umana**. L'intelligenza
artificiale prepara, la persona approva. In questo dominio la regola non è una cautela generica: una bozza
sbagliata costa un minuto, un documento trasmesso costa una nota di credito.

Questa storia copre le scritture che **non** escono verso l'esterno — creare, importare, correggere. L'uscita
verso l'autorità ha un varco suo, ed è la storia `0029`.

## 2. Requisiti funzionali

1. **RF-1** — Gli strumenti di scrittura dichiarati sono `create_document`, `import_document` e `fix_and_resubmit`;
   tutti e tre producono un documento in stato `bozza` e **nessuno** trasmette.
2. **RF-2** — Ogni invocazione restituisce la bozza **per intero e in forma leggibile**, con l'elenco di cosa è
   stato dedotto e cosa è stato preso alla lettera: chi conferma deve poter vedere cosa sta confermando.
3. **RF-3** — La bozza prodotta è marcata con l'**origine «assistente»** ed è distinguibile nell'elenco dei
   documenti.
4. **RF-4** — Nessuna bozza prodotta da uno strumento diventa definitiva senza un'azione umana esplicita: la
   conferma non è un parametro dello strumento, è un atto separato.
5. **RF-5** — Se i dati forniti non bastano, lo strumento **non inventa**: restituisce cosa manca e non crea nulla.
6. **RF-6** — Gli strumenti di scrittura sono **idempotenti su una chiave fornita dal chiamante**: una ripetizione
   dovuta a un errore di rete non produce due bozze.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` viene dal contesto della chiamata autenticata, mai da un
  parametro. Prova di isolamento su ogni strumento di scrittura.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte delle storie `0013` e
  `0020`; nessuna logica duplicata e nessuna scorciatoia che aggiri le validazioni delle rotte. Errori in
  `application/problem+json`.
- **RT-3 — Persistenza (§8).** Migrazione `V22__tool_origin_and_idempotency.sql`: colonna di origine
  «assistente» su `canonical_document` e tabella di idempotenza delle invocazioni di scrittura, con `tenant_id`,
  chiave UUID versione 7 e colonne di controllo.
- **RT-4 — Modulo frontend (§3, §5).** Le bozze di origine «assistente» sono **visibilmente marcate** nell'elenco
  e nella scheda: chi apre l'app deve capire da dove viene quel documento. Solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'etichetta «preparata dall'assistente» e i testi di conferma dallo spazio-nomi
  `einvoicing`, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La creazione di una bozza **non** consuma la metrica `documenti`,
  coerentemente con le storie `0012` e `0013`: il consumo è alla trasmissione. Ma va posto un **limite di
  frequenza** sulle invocazioni di scrittura, o un agente in errore riempie l'account di bozze. Il varco di
  abilitazione resta quello della rotta sottostante.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che realizza il punto per la scrittura: tutti e tre
  gli strumenti marcati **scrittura**, tutti producono una **bozza**, tutti richiedono **conferma umana** prima
  di qualunque effetto ulteriore. Nessuno di essi ha effetti verso l'esterno. Dipendenza dichiarata: UC 0061-0063,
  non implementati.
- **RT-8 — Dati personali (§10).** ⚠️ Uno strumento di scrittura riceve dati personali **da un modello**, che li
  ha ricavati da una conversazione: sono dati di provenienza incerta. Due conseguenze: la validazione delle rotte
  sottostanti non si può saltare (RT-2), e l'origine «assistente» va conservata perché in caso di contestazione si
  sappia da dove veniva un dato. La colonna di origine va dichiarata nel manifesto.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `bozza creata da strumento`, `invocazione rifiutata per dati
  insufficienti`, `invocazione duplicata ignorata` sono registrati con `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione e nome dello strumento — **senza** i parametri, che contengono dati personali.

## 4. Criteri di accettazione

**CA-1 — Bozza prodotta e rivedibile**
- **Dato** un account abilitato
- **Quando** si invoca `create_document` con controparte, importo e descrizione
- **Allora** nasce un documento in stato `bozza`, il risultato lo mostra per intero, e l'elenco lo marca come
  preparato dall'assistente

**CA-2 — Nessuna trasmissione**
- **Dato** la bozza appena creata
- **Quando** si osserva lo stato del sistema
- **Allora** nulla è uscito verso l'esterno e nessuna quota è stata consumata

**CA-3 — Dati insufficienti**
- **Dato** un'invocazione senza controparte
- **Quando** la si esegue
- **Allora** lo strumento restituisce cosa manca, **non inventa** una controparte, e nulla viene creato

**CA-4 — Idempotenza**
- **Dato** la stessa invocazione ripetuta con la stessa chiave
- **Quando** entrambe arrivano
- **Allora** esiste una sola bozza

**CA-5 — Le validazioni non si saltano**
- **Dato** un'invocazione con un identificativo fiscale in formato non valido
- **Quando** la si esegue
- **Allora** viene rifiutata dalla stessa validazione che rifiuterebbe l'inserimento a mano

**CA-6 — Isolamento fra account**
- **Dato** due account
- **Quando** si invoca uno strumento di scrittura nel contesto di uno nominando l'altro nei parametri
- **Allora** la bozza nasce nel proprio account e il parametro è ignorato

**CA-7 — Limite di frequenza**
- **Dato** molte invocazioni di scrittura in rapida successione
- **Quando** si supera il limite
- **Allora** le successive sono rifiutate con l'indicazione di quando riprovare

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sull'idempotenza e sul rifiuto per dati insufficienti; **integrazione** su ogni strumento
      di scrittura, **compresa la verifica che non salti le validazioni delle rotte**;
- [ ] prova di **isolamento fra account** su ogni strumento di scrittura;
- [ ] **prova end-to-end**: *rimando* — il livello conversazionale non esiste (UC 0061-0063); voce `da-coprire`
      nel registro con storia proprietaria UC 0061;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la colonna di origine e con la nota sulla provenienza incerta dei dati;
- [ ] **registro delle decisioni** compilato, con la scelta «bozza sempre, nessuna trasmissione, origine
      conservata»;
- [ ] contratto degli **strumenti conversazionali** dichiarato per i tre strumenti di scrittura.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | Gli strumenti si appoggiano alle rotte di creazione e importazione |
| `0020` | `fix_and_resubmit` si appoggia alla generazione del correttivo |
| `0027` | Serve il descrittore degli strumenti e la sua prova di coerenza |

## 7. Fuori ambito

- La **trasmissione** e ogni altro effetto verso l'esterno: storia `0029`.
- L'approvazione della bozza come flusso a più persone: non c'è, e non serve in un'impresa da dieci persone. La
  conferma è di chi ha il ruolo.
- La modifica di documenti già trasmessi: non esiste, per regola fiscale (storia `0011`).

## 8. Punti aperti

- **Se le bozze prodotte dall'assistente debbano scadere** se nessuno le conferma. Lasciarle accumulare sporca
  l'elenco; cancellarle da sole distrugge lavoro. Proposta: non scadono, ma sono filtrabili. Da confermare.
- **Chi risponde di una bozza sbagliata prodotta dall'assistente** e poi confermata distrattamente. È una
  questione di responsabilità, non di codice, e va affrontata nel linguaggio del prodotto prima che accada.
