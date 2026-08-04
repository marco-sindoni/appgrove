# 0022 — Dalla domanda al piano d'interrogazione

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 05 — Copilota sui dati
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che non sa dove sta il riquadro giusto
> voglio poter scrivere «quanto ho fatturato a luglio rispetto all'anno scorso» e ottenere il numero
> così da non dover imparare la struttura dell'app per farle una domanda.

**Contesto.** È la funzione che il catalogo chiama «killer app», ed è anche quella che può distruggere la
fiducia nell'intero prodotto. Il dato di riferimento è duro: i modelli che raggiungono l'85-90 % sui banchi di
prova accademici scendono al **39,1 %** su schemi d'impresa veri (§2.5 della
[descrizione](../application-description.md), fonte 4), e gli errori che producono sono **plausibili**. La
scelta di questa storia è quindi netta e non negoziabile: **il modello non scrive interrogazioni**. Traduce la
domanda in un piano strutturato che nomina metriche esistenti, e il piano viene validato contro il catalogo
prima di essere eseguito. Il modello sceglie *che cosa chiedere*; il numero lo calcola codice deterministico.

## 2. Requisiti funzionali

1. **RF-1** — Il copilota riceve una domanda scritta in lingua naturale e produce un **piano d'interrogazione**
   strutturato: metrica (per chiave e versione), periodo, dimensioni, filtri, confronto, ordinamento, limite.
2. **RF-2** — Al modello viene fornito **soltanto** il catalogo delle metriche pubblicate che il ruolo di chi
   chiede può vedere, con chiavi, descrizioni, unità e dimensioni ammesse. Nessuno schema di database, nessuna
   tabella, nessun nome di colonna.
3. **RF-3** — Il piano prodotto viene **validato**: metrica esistente, pubblicata e visibile; dimensioni fra
   quelle ammesse dalla metrica; periodo rappresentabile; filtri su valori esistenti. Un piano che non passa la
   validazione **non viene eseguito**.
4. **RF-4** — Un piano non validabile produce un **rifiuto motivato** (storia 0024), mai un numero e mai un
   ripiego su una metrica «simile».
5. **RF-5** — Una domanda che si traduce in **più piani** (per esempio «fatturato e incassato di luglio») li
   esegue tutti e li presenta separatamente, ciascuno con la propria scheda.
6. **RF-6** — Il piano è **mostrato all'utente** in forma leggibile accanto alla risposta: «fatturato emesso ·
   luglio 2026 · confronto anno precedente». Non è un dettaglio tecnico nascosto: è il modo in cui l'utente si
   accorge di essere stato frainteso.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il catalogo passato al modello è quello dell'account chiamante, letto
  con `tenant_id` dal gettone verificato; il piano viene eseguito con lo stesso `tenant_id`. Il testo della
  domanda **non** può influenzare l'account su cui si esegue.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/insights/v1/domande`; corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabelle `domanda` e `piano_di_interrogazione` sullo schema `app_insights`, con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Copilota` del modulo `insights`; solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'interfaccia del copilota è nelle cinque lingue; **la lingua della domanda è
  quella che l'utente scrive** e la risposta è nella stessa lingua della domanda.
- **RT-6 — Varchi e quota (§6, §7).** Prima di eseguire una domanda il servizio prenota una unità della metrica
  `questions` (natura `flow`); a quota esaurita risponde `429` con il rimedio (storia 0026). Il catalogo
  mostrato al modello rispetta la classe di riservatezza (storia 0025).
- **RT-8 — Dati personali (§10).** **Voce nuova nel manifesto**: `domanda.testo` è testo libero scritto da una
  persona e può nominarne altre; in italiano e inglese, campo annotato `@PersonalData`, tabelle `domanda` e
  `piano_di_interrogazione` in `exportData` e `purgeData`.
- **RT-14 — Registrazione eventi (§14).** «Domanda ricevuta», «piano prodotto», «piano rifiutato» con
  `tenant_id`, `app_id`, `user_id`, identificativo della domanda e motivo del rifiuto; **mai il testo della
  domanda**, che è dato personale.
- **RT-12 — Esposizione conversazionale (§12).** Il copilota interno **usa lo stesso contratto** che l'epica 07
  espone al livello conversazionale di piattaforma: non esiste un secondo motore. Dipendenza dichiarata:
  UC 0061-0063 (livello conversazionale, non ancora implementato).

## 4. Criteri di accettazione

**CA-1 — Domanda semplice**
- **Dato** un `owner` e il catalogo con `fatturato_emesso` pubblicata
- **Quando** scrive «quanto ho fatturato a luglio rispetto all'anno scorso»
- **Allora** il piano prodotto è «metrica `fatturato_emesso`, periodo luglio 2026, confronto anno precedente»,
  è mostrato accanto alla risposta, e il numero corrisponde a quello dello stesso calcolo fatto dal cruscotto

**CA-2 — Metrica inesistente**
- **Dato** un catalogo che non contiene nulla sul costo del personale
- **Quando** l'utente chiede «quanto mi costa il personale»
- **Allora** riceve un rifiuto motivato che dice che quell'indicatore non esiste nel suo catalogo, e l'elenco di
  ciò che si può chiedere di simile — **non** un numero calcolato su un'altra metrica

**CA-3 — Dimensione non ammessa**
- **Dato** la metrica `fatturato_emesso` che ammette le dimensioni `cliente` e `categoria`
- **Quando** l'utente chiede «il fatturato per venditore»
- **Allora** il piano è rifiutato con «la dimensione venditore non è disponibile per questo indicatore»

**CA-4 — Due domande in una**
- **Dato** la domanda «fatturato e incassato di luglio»
- **Quando** viene elaborata
- **Allora** vengono prodotti ed eseguiti due piani, e la risposta presenta i due numeri separatamente, ciascuno
  con la propria scheda

**CA-5 — Il piano è visibile**
- **Dato** una risposta qualunque
- **Quando** l'utente la legge
- **Allora** vede accanto al numero il piano in forma leggibile, e può correggerlo riformulando la domanda

**CA-6 — Isolamento fra account**
- **Dato** due account con cataloghi diversi
- **Quando** un utente di `A` fa una domanda
- **Allora** il modello riceve solo il catalogo di `A` e il piano viene eseguito solo sui fatti di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul validatore del piano, con un caso di rifiuto per ciascuna regola, usando un modello
      **simulato** che restituisce piani prefabbricati — compresi piani malformati e piani che nominano metriche
      inesistenti;
- [ ] prova di **isolamento fra account** sul catalogo passato al modello e sull'esecuzione del piano;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include «fai una domanda e ottieni il
      numero»; registro di copertura aggiornato;
- [ ] **traduzioni** dell'interfaccia presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `domanda.testo`, campo annotato
      `@PersonalData`, tabelle in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato, con la scelta «il modello produce il piano, non il numero» e il
      dato di ricerca che la motiva;
- [ ] contratto degli **strumenti conversazionali** dichiarato: il copilota interno consuma `elenca_metriche` e
      `interroga_metrica` (storia 0031);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | il catalogo delle metriche è **l'unico** vocabolario ammesso |
| storia `0015` | il piano viene eseguito dal motore di calcolo |
| epica di piattaforma UC 0061-0063 | il livello conversazionale non è ancora implementato: qui si dichiara il contratto e si costruisce il copilota interno, che ne è il primo consumatore |

## 7. Fuori ambito

- la forma della risposta e la scheda del numero: storia 0023;
- i rifiuti e il «non lo so»: storia 0024;
- la riservatezza applicata alla conversazione: storia 0025;
- il consumo della quota e il registro delle domande: storia 0026;
- azioni di scrittura da chat: storia 0032.

## 8. Punti aperti

- **Quale modello linguistico e dove gira?** Se il modello è di un fornitore esterno, la domanda dell'utente
  (che è dato personale, RT-8) esce dalla piattaforma: sarebbe un **nuovo responsabile esterno del trattamento**
  e cambierebbe la classificazione dell'app, che oggi non ne ha nessuno (§6.5 della descrizione). **Non lo decide
  un agente.** Chiude: **sviluppatore**, prima di implementare questa storia.
- **Il catalogo passato al modello quanto può crescere?** Con molte metriche personalizzate il contesto si
  allunga e il costo per domanda sale — che è il numero mancante del listino (punto aperto 9 della descrizione).
  Chiude: **sviluppatore**, con la misura su prototipo.
