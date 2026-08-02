# Modello — storia utente (`NNNN-<slug>.md`)

> **Istruzioni per l'agente-app (cancellare questo riquadro a stesura conclusa).**
> Copia tutto ciò che sta **sotto la riga** in
> `docs/usecases/_catalogo/NN-<slug-app>/<NN>-<slug-epica>/NNNN-<slug-storia>.md` e riempi i segnaposto `‹…›`.
> `NNNN` è progressivo **a livello di applicazione** e non si azzera a ogni epica.
> Una storia = **una change**: se l'implementazione richiede più di circa un giorno di lavoro, spezzala
> ([GUIDA-AUTORE.md](GUIDA-AUTORE.md) §3).
> Nessuna sezione è facoltativa. «Nessuno» e «non applicabile» sono risposte legittime; il silenzio no.

---

# ‹NNNN› — ‹Titolo della storia›

**Applicazione**: ‹NN — Nome dell'app› (`‹app_id›`) · **Epica**: ‹NN — Nome dell'epica›
**Storia**: `‹NNNN›` · **Taglia stimata**: ‹mezza giornata | una giornata› · **Stato**: 🟡 bozza d'autore
**Dipende da**: ‹`0003`, `0007`› — ‹oppure «nessuna: è la prima dell'epica»›
**Ultimo aggiornamento**: ‹AAAA-MM-GG›

## 1. Narrazione

> Come ‹ruolo: titolare, addetto all'amministrazione, tecnico sul campo, amministratore dell'account…›
> voglio ‹capacità, in una riga, senza dire come si implementa›
> così da ‹beneficio concreto e misurabile per chi lavora, non per il sistema›.

**Contesto.** ‹Due o tre righe: cosa succede oggi senza questa storia, e perché è il momento giusto per farla
adesso e non prima. Se la storia nasce da un obbligo di legge o da una richiesta rilevata nell'analisi in rete,
citarne la fonte.›

## 2. Requisiti funzionali

> Numerati, uno per riga, **verificabili**. Se un requisito non si può dimostrare falso, non è un requisito.
> Da 3 a 7: oltre i sette la storia è troppo grande.

1. **RF-1** — ‹…›
2. **RF-2** — ‹…›
3. **RF-3** — ‹…›

## 3. Requisiti tecnici

> Richiamare **per nome** gli invarianti di piattaforma applicabili
> ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md)). Cancellare le voci che non si applicano, ma solo dopo averle
> lette: la maggior parte si applica quasi sempre.

- **RT-1 — Isolamento fra account (§1).** ‹Ogni lettura e scrittura delle entità `‹X›` filtra per `tenant_id`
  preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene
  ignorato.›
- **RT-2 — Interfaccia di programmazione (§2).** ‹Rotte `‹metodo› /api/‹app_id›/v1/‹risorsa›`; corpo validato;
  errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.›
- **RT-3 — Persistenza (§8).** ‹Migrazione `V‹N›__‹descrizione›.sql` sullo schema `app_‹app_id›`: tabella
  `‹nome›` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.›
- **RT-4 — Modulo frontend (§3, §5).** ‹Sezione `‹nome›` del modulo `‹app_id›`; dati letti con il client
  generato; solo token del sistema di design; funziona in tema chiaro e scuro.›
- **RT-5 — Cinque lingue (§4).** ‹Tutte le stringhe visibili passano dallo spazio-nomi `‹app_id›` e sono presenti
  in `en, it, fr, es, de`.›
- **RT-6 — Varchi e quota (§6, §7).** ‹Prima di ‹azione che consuma› il servizio prenota una unità della metrica
  `‹metrica›` (natura `‹flow|stock›`); a quota esaurita risponde `429` con l'indicazione del rimedio. Con
  abbonamento non attivo risponde `402`.›
- **RT-7 — Esposizione conversazionale (§12).** ‹Strumenti dichiarati: `‹nome›(parametri) → risultato`, marcato
  ‹lettura | scrittura›. ‹Se è scrittura con effetti irreversibili: produce una bozza e richiede conferma umana.›
  Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).›
- **RT-8 — Dati personali (§10).** ‹Se la storia introduce campi che riguardano una persona: voci nuove nel
  manifesto `docs/compliance/manifests/‹app_id›.yaml` in italiano e inglese, campi annotati `@PersonalData`,
  tabelle aggiunte a `exportData` e `purgeData`. Se non ne introduce: dirlo — «nessun dato personale nuovo».›
- **RT-9 — Registrazione eventi (§14).** ‹Gli eventi `‹x creato›`, `‹y respinto per quota›` sono registrati con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali.›

## 4. Criteri di accettazione

> Forma **dato / quando / allora**. Uno scenario per comportamento osservabile, compresi gli errori e i casi
> limite. Da 3 a 6 scenari: se ne servono di più, la storia è troppo grande.

**CA-1 — ‹nome dello scenario felice›**
- **Dato** ‹stato di partenza: chi è l'utente, cosa esiste già›
- **Quando** ‹azione›
- **Allora** ‹esito osservabile, con il valore atteso›

**CA-2 — ‹scenario di errore›**
- **Dato** ‹…› · **Quando** ‹…› · **Allora** ‹codice e messaggio attesi›

**CA-3 — ‹isolamento fra account›**
- **Dato** due account `A` e `B`, ciascuno con i propri `‹X›`
- **Quando** un utente di `A` chiede l'elenco dei `‹X›`
- **Allora** vede solo i propri, anche se forza l'identificativo dell'altro account nella richiesta

**CA-4 — ‹quota esaurita›** *(se la storia consuma quota)*
- **Dato** un account che ha raggiunto il tetto di `‹metrica›`
- **Quando** tenta ‹azione›
- **Allora** riceve `429` e un messaggio che spiega come rimediare, e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla logica introdotta e di **integrazione** sulla risorsa, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** su ogni risorsa nuova;
- [ ] **prova end-to-end** se la storia tocca la superficie utente: percorso `[J-‹APP›]` esteso o creato, e
      **registro di copertura** [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato —
      oppure risposta esplicita «rimando» (con motivo e storia proprietaria) o «nessun impatto»;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese se la storia tratta dati personali, con i campi
      annotati e le tabelle presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato con le scelte fatte e il perché;
- [ ] contratto degli **strumenti conversazionali** dichiarato per le funzioni introdotte;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| ‹storia `0003` di questa app› | ‹cosa deve esistere prima› |
| ‹epica di piattaforma non implementata, es. UC 0061-0063› | ‹cosa manca e cosa si fa nel frattempo› |

‹Se non dipende da nulla, scrivere «nessuna dipendenza».›

## 7. Fuori ambito

- ‹cosa questa storia **non** fa, e quale storia lo fa›
- ‹cosa è stato deliberatamente rimandato, con il motivo›

## 8. Punti aperti

‹Decisioni che non spettano a questa storia — direzione di prodotto, prezzi, classificazioni di dati personali
ambigue, effetti verso l'esterno — con l'indicazione di chi le chiude. Se non ce ne sono, scrivere «nessuno».›
