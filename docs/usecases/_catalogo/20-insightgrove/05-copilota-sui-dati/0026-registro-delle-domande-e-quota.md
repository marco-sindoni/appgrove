# 0026 — Registro delle domande e consumo della quota

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 05 — Copilota sui dati
**Storia**: `0026` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0004`, `0022`, `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che paga un piano con un tetto di domande
> voglio sapere quante ne ho usate, poter rifare una domanda già fatta senza riscriverla, e capire perché una
> risposta di oggi è diversa da quella di ieri
> così da governare la spesa e da fidarmi di ciò che leggo.

**Contesto.** Questa storia chiude il cerchio del copilota: collega il consumo alla metrica di quota già
costruita (storia 0004), rende le domande ripetibili e stabilisce che cosa conta come una domanda. È anche la
storia in cui il dato personale del copilota — **il testo della domanda**, scritto da una persona e capace di
nominarne altre (§6.4 della [descrizione](../application-description.md)) — riceve il suo trattamento completo:
conservazione dichiarata, esportazione, cancellazione, e invisibilità a chi amministra la piattaforma.

## 2. Requisiti funzionali

1. **RF-1** — Ogni domanda eseguita consuma **una** unità della metrica `questions`, qualunque sia la
   complessità del piano prodotto. Consumano una unità anche il riepilogo scritto di un rapporto periodico
   (storia 0028) e la spiegazione di uno scostamento (storia 0029).
2. **RF-2** — Un rifiuto che non ha interpellato il modello **non consuma**; un rifiuto che lo ha interpellato
   consuma, e l'utente lo vede in entrambi i casi.
3. **RF-3** — La sezione Copilota mostra il consumo del periodo e il tetto, e avverte oltre l'80 %.
4. **RF-4** — A quota esaurita la domanda riceve `429` con il messaggio che dice cosa è successo, cosa non si può
   più fare e come si rimedia; **nulla viene eseguito**.
5. **RF-5** — Il registro delle domande dell'account elenca le domande poste, con autore, momento, piano
   prodotto, esito e collegamento alla traccia; una domanda si può **ripetere** con un gesto, e la ripetizione
   consuma una unità.
6. **RF-6** — Un utente vede le proprie domande; `owner` e `admin` vedono quelle di tutto l'account — e questo
   va **detto nell'interfaccia**, perché sapere che le proprie domande sono visibili cambia il modo in cui si
   scrivono.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Registro e contatore filtrano per `tenant_id` preso dal gettone
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/insights/v1/domande` (paginata) e
  `POST /api/insights/v1/domande/{id}/ripetizione`; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Le tabelle `domanda` e `piano_di_interrogazione` esistono dalla storia 0022;
  qui si aggiungono l'esito, il riferimento alla traccia e l'indicazione del consumo.
- **RT-4 — Modulo frontend (§3, §5).** Il registro è una parte della sezione Copilota; solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, compresi i messaggi di quota, esistono in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Prima di eseguire una domanda il servizio prenota una unità della metrica
  `questions` (natura `flow`); a quota esaurita risponde `429`. Con abbonamento `past_due` il copilota resta
  accessibile; con `canceled` risponde `402`. L'**esportazione dei propri dati resta accessibile in ogni caso**.
- **RT-8 — Dati personali (§10).** `domanda.testo` è dichiarato nel manifesto in italiano e inglese, annotato
  `@PersonalData`, e le tabelle `domanda` e `piano_di_interrogazione` sono in `exportData` e `purgeData`.
  Conservazione proposta: **dodici mesi**, poi **cancellazione fisica** — non pseudonimizzazione, perché un testo
  «anonimizzato» che conserva le parole resta un dato personale.
- **RT-14 — Registrazione eventi (§14).** «Domanda eseguita», «domanda respinta per quota», «domanda ripetuta»
  con `tenant_id`, `app_id`, `user_id`, identificativo della domanda; **mai il testo**.

## 4. Criteri di accettazione

**CA-1 — Una domanda, una unità**
- **Dato** un account sul piano `pro` con 42 domande consumate
- **Quando** l'utente pone una domanda che produce due piani
- **Allora** il contatore passa a 43, non a 44

**CA-2 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto
- **Quando** un utente pone una domanda
- **Allora** riceve `429`, il messaggio che dice come rimediare, e **nessun piano viene eseguito né registrato
  come eseguito**

**CA-3 — Rifiuto senza modello**
- **Dato** una domanda su una metrica che richiede una fonte non collegata, riconosciuta senza interpellare il
  modello
- **Quando** viene elaborata
- **Allora** il contatore non aumenta, e l'interfaccia dice che questa domanda non ha consumato

**CA-4 — Ripetizione**
- **Dato** una domanda posta due settimane fa
- **Quando** l'utente la ripete dal registro
- **Allora** il piano viene rieseguito, il contatore aumenta di uno, e la risposta mostra il valore di allora e
  quello di adesso con che cosa è cambiato

**CA-5 — Visibilità del registro**
- **Dato** un utente `member` che ha posto cinque domande e un `admin` che ne ha poste dieci
- **Quando** il `member` apre il registro
- **Allora** vede le proprie cinque; quando lo apre l'`admin`, vede tutte e quindici, e il `member` è stato
  informato nell'interfaccia che le sue domande sono visibili a chi amministra l'account

**CA-6 — Cancellazione**
- **Dato** una richiesta di cancellazione dei dati dell'interessato
- **Quando** viene eseguita
- **Allora** le domande di quella persona spariscono **fisicamente** dalle tabelle `domanda` e
  `piano_di_interrogazione`, e resta una riga di prova nel registro delle purghe

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio (una domanda = una unità, rifiuti che consumano e rifiuti che no) e di
      **integrazione** su registro e ripetizione;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sul registro;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include «a quota esaurita il copilota
      risponde 429 e non esegue»; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** completo per `domanda.testo`, in italiano e inglese, con conservazione e
      cancellazione fisica; tabelle in `exportData` e `purgeData` **verificate con una prova**;
- [ ] **registro delle decisioni** compilato, con «una domanda = una unità», la conservazione di dodici mesi e la
      visibilità del registro a `owner` e `admin`;
- [ ] contratto degli **strumenti conversazionali**: le chiamate dell'assistente consumano la stessa quota
      (storia 0033);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0004` | il contatore e il varco a `429` esistono già |
| storia `0022` | serve la domanda e il piano |
| storia `0023` | la ripetizione mostra il confronto fra la risposta di allora e quella di adesso |

## 7. Fuori ambito

- l'applicazione della quota alle chiamate dell'assistente esterno: storia 0033;
- la deroga temporanea sul tetto concessa da chi amministra la piattaforma:
  [estensioni-admin.md](../estensioni-admin.md).

## 8. Punti aperti

- **`owner` e `admin` vedono le domande di tutti: è giusto?** È utile (capire come la squadra usa lo strumento) e
  insieme è una forma di visibilità sull'attività delle persone. Non è controllo a distanza dell'attività
  lavorativa in senso stretto — non misura una prestazione — ma ci confina. Raccomandazione: **sì, con
  l'avviso esplicito nell'interfaccia**, come descritto. Ma è un punto che tocca la disciplina del §2.3 della
  descrizione: chiude **sviluppatore**, con parere legale se il dubbio resta.
- **Dodici mesi di conservazione delle domande sono i giusti?** Servono a spiegare e ripetere; oltre l'anno il
  valore cala e il rischio resta. Chiude: **sviluppatore**.
