# 0011 — Ciclo di vita dell'abbonamento

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 02 — Piani e abbonati
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che guarda l'elenco degli iscritti
> voglio che lo stato di ciascuno significhi sempre la stessa cosa e non possa saltare dove non deve
> così da potermi fidare di quello che leggo senza chiedere alla reception «ma questo qui paga o no?».

**Contesto.** Uno stato che significa cose diverse a seconda di chi l'ha messo non è uno stato: è un'opinione. La
macchina a stati disegnata al §4 della descrizione è il contratto che tutte le storie successive — rinnovi,
solleciti, sospensioni, metriche — devono rispettare, e questa storia la rende vera in un posto solo, con i suoi
passaggi ammessi e quelli vietati. La sua parentela con la macchina a stati degli abbonamenti di **piattaforma**
è voluta e dichiarata (§10.1 della descrizione): stessi passaggi, stessa idea di tolleranza sul pagamento
fallito, stessa idea di disdetta con accesso fino a fine periodo. Riusare il ragionamento è un guadagno; riusare
i dati sarebbe un errore.

## 2. Requisiti funzionali

1. **RF-1** — Gli stati sono esattamente sette: `in_prova`, `attivo`, `in_ritardo`, `disdetto_a_scadenza`,
   `sospeso`, `cessato`, e nessun altro. `cessato` è finale.
2. **RF-2** — I passaggi ammessi sono solo quelli del disegno: ogni altro tentativo è rifiutato con un messaggio
   che dice qual è lo stato attuale e quali passaggi sono possibili da lì.
3. **RF-3** — Ogni passaggio registra **chi**, **quando**, **da quale stato a quale** e **perché** (motivo scelto
   da un elenco chiuso più una nota facoltativa), e la scheda dell'abbonamento ne mostra la cronologia completa.
4. **RF-4** — Solo `in_prova`, `attivo` e `disdetto_a_scadenza` generano scadenze nuove; `in_ritardo`, `sospeso`
   e `cessato` no.
5. **RF-5** — La cessazione **non si annulla**: per riprendere il rapporto si sottoscrive un abbonamento nuovo, e
   il messaggio lo spiega prima della conferma.
6. **RF-6** — L'elenco degli abbonamenti si filtra per stato, e ogni stato ha a schermo una spiegazione in una
   riga di cosa significa per il cliente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni passaggio di stato agisce su un abbonamento dell'account del token
  verificato; nessun percorso tocca abbonamenti di altri account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta
  `POST /api/abbonati/v1/abbonamenti/{id}/transizioni` con stato di destinazione e motivo; errori in
  `problem+json` con codice stabile per «passaggio non ammesso»; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V6__transizione_abbonamento.sql`: tabella `transizione_abbonamento`
  con `tenant_id`, colonne di controllo, stato di partenza, stato di arrivo, motivo, autore. La cronologia
  **non** si cancella logicamente: è la memoria del rapporto.
- **RT-4 — Modulo frontend (§3, §5).** Nella scheda dell'abbonamento, una linea del tempo con la cronologia e i
  soli pulsanti dei passaggi ammessi da lì: un'azione che non si può fare non si mostra spenta, si nasconde;
  solo token del sistema di design.
- **RT-5 — Cinque lingue (§4).** Nomi degli stati, spiegazioni, motivi dell'elenco chiuso e messaggi di rifiuto in
  `en, it, fr, es, de`. È la storia con più stringhe da tradurre dell'epica.
- **RT-6 — Varchi e quota (§6).** Il passaggio a `cessato` **restituisce** una unità di `abbonamenti_attivi`;
  tutti gli altri stati la trattengono (storia `0004`, criterio CA-3).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo qui: i passaggi che si comandano da chat
  (`disdici_abbonamento`, `sospendi_abbonamento`) sono dichiarati dalle storie che li governano (`0022`, `0024`) e
  raccolti nella `0032`. Qui si dichiara solo che ogni strumento futuro **deve** passare da questa macchina.
- **RT-8 — Dati personali (§10).** La cronologia riferisce a una persona per relazione: la tabella va nel
  manifesto, in `exportData` e in `purgeData`.
- **RT-9 — Registrazione eventi (§14).** `passaggio di stato eseguito`, `passaggio rifiutato`, con `tenant_id`,
  `app_id`, `user_id`, stati e correlazione, senza nomi.

## 4. Criteri di accettazione

**CA-1 — Passaggio ammesso**
- **Dato** un abbonamento `attivo`
- **Quando** l'utente lo porta a `disdetto_a_scadenza` con motivo «richiesta dell'abbonato»
- **Allora** lo stato cambia, la cronologia registra chi e quando, e il rapporto resta valido fino a fine periodo

**CA-2 — Passaggio vietato**
- **Dato** un abbonamento `cessato` · **Quando** si prova a riportarlo ad `attivo`
- **Allora** il rifiuto dice che la cessazione è definitiva e suggerisce di sottoscriverne uno nuovo

**CA-3 — Generazione di scadenze coerente con lo stato**
- **Dato** un abbonamento `sospeso` che arriva alla data di rinnovo
- **Quando** gira la lavorazione dei rinnovi
- **Allora** non nasce alcuna scadenza, e il motivo è tracciato

**CA-4 — Restituzione della quota**
- **Dato** un account al tetto · **Quando** cessa un abbonamento
- **Allora** il conteggio scende di uno e una nuova sottoscrizione diventa possibile

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** uno tenta un passaggio su un abbonamento dell'altro
- **Allora** riceve una risposta di risorsa inesistente, non una di permesso negato: non si conferma nemmeno
  l'esistenza

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sulla tabella dei passaggi ammessi — **tutte** le combinazioni, ammesse e vietate — e di
      **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account** sui passaggi;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-ABBONATI]` della storia `0033` attraversa almeno
      `attivo → in_ritardo → attivo`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** di stati, spiegazioni e motivi in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la cronologia dei passaggi;
- [ ] **registro delle decisioni** compilato: sette stati, passaggi ammessi, e la **parentela dichiarata** con la
      macchina a stati di piattaforma (cosa si riusa: la semantica; cosa no: i dati);
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | serve un abbonamento da far vivere |
| storia `0004` | il conteggio della quota dipende dagli stati |

## 7. Fuori ambito

- **chi** provoca i passaggi automatici: il mancato incasso è la storia `0022`, la disdetta dell'abbonato la
  `0024`, la fine del periodo la `0012`;
- la sospensione con spostamento delle date: storia `0015`, che è un caso a sé;
- il cambio di piano, che non è un passaggio di stato ma una modifica: storia `0014`.

## 8. Punti aperti

**Nessuno.** La macchina a stati è disegnata per intero al §4 della descrizione; questa storia la implementa e
non ha margini da decidere. Se durante l'implementazione emergesse la necessità di uno stato in più, è il segnale
che il disegno è sbagliato: va corretto lì, non aggiunto qui.
