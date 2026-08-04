# 0020 — Regole di mappatura dalle chiavi

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 04 — Attribuzione della spesa
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile tecnico che non ha ancora strumentato tutto
> voglio poter dire «tutto quello che passa dalla chiave di produzione è del progetto Alfa»
> così da attribuire subito la maggior parte della spesa, senza aspettare di aver messo le etichette dappertutto.

**Contesto.** È la storia che decide se il «non attribuito» resterà piccolo. Ciò che arriva dal rendiconto del
fornitore non ha etichette: ha chiavi, progetti e spazi di lavoro (§2.6, fonte 3), che sono l'unità di attribuzione
nativa dei fornitori. Molte aziende hanno già organizzato le proprie chiavi per ambiente o per prodotto, e in quel
caso una manciata di regole attribuisce l'80% della spesa in cinque minuti — senza toccare una riga del loro
codice. È l'anticamera del prodotto: chi vede funzionare le regole ha un motivo per strumentare il resto, chi vede
solo «non attribuito 100%» chiude la scheda.

## 2. Requisiti funzionali

1. **RF-1** — Una regola di attribuzione ha una **condizione** (chiave del fornitore, progetto, spazio di lavoro,
   modello, fonte, oppure una combinazione), un insieme di **assegnazioni** (valori per una o più dimensioni) e una
   **priorità**.
2. **RF-2** — Le regole si applicano **solo dove manca l'etichetta**: un'etichetta esplicita arrivata con la misura
   vince sempre su una regola. L'ordine di precedenza — etichetta, poi regola per priorità, poi non attribuito — è
   dichiarato nell'interfaccia, non solo nel codice.
3. **RF-3** — Prima di salvare, una regola mostra un'**anteprima**: quante misure del periodo scelto sarebbero
   attribuite da questa regola e quale importo rappresentano.
4. **RF-4** — Una regola nuova si applica **da qui in avanti**; applicarla anche allo storico è un'azione separata
   ed esplicita (storia `0021`), perché cambia numeri che il cliente potrebbe aver già usato.
5. **RF-5** — L'app **suggerisce** regole: guardando le chiavi e i progetti che generano più spesa non attribuita,
   propone la regola che li coprirebbe, con l'importo che recupererebbe.
6. **RF-6** — Le regole si possono disattivare senza cancellarle, e l'elenco mostra per ciascuna quanto ha
   attribuito nell'ultimo periodo: una regola che non attribuisce nulla è una regola da rivedere.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `regola_di_attribuzione` filtra per
  `tenant_id` preso dal gettone verificato; le regole di un account non si applicano mai alle misure di un altro.
  Prova di isolamento obbligatoria.
- **RT-2 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `regola_di_attribuzione` con
  `tenant_id`, condizione, assegnazioni, priorità, stato, `valida_da`, chiave primaria UUID versione 7, colonne di
  controllo e cancellazione logica.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `GET|POST|PATCH|DELETE /api/spesa_modelli/v1/regole` e
  `POST /api/spesa_modelli/v1/regole/anteprima`; corpo validato; errori in `problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Attribuzione», scheda «Regole»: elenco ordinato per priorità,
  costruttore della regola con anteprima, e i suggerimenti in testa. Solo token del sistema di design; tema chiaro
  e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe presenti in `en, it, fr, es, de`, compresa la spiegazione
  dell'ordine di precedenza.
- **RT-6 — Varchi e ruoli (§6).** Creare e modificare regole è riservato a `owner` e `admin`; un `member` le vede.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `crea_regola_di_attribuzione(condizione, assegnazioni,
  valida_da) → bozza`, marcato **scrittura con conferma** (storia `0033`): produce una bozza con l'anteprima
  dell'effetto e non salva finché una persona non conferma.
- **RT-8 — Dati personali (§10).** Le assegnazioni contengono valori di dimensione, già dichiarati nel manifesto
  (storia `0019`); la tabella `regola_di_attribuzione` entra in `exportData` e `purgeData` perché contiene sia
  quei valori sia l'autore.
- **RT-9 — Registrazione eventi (§14).** Eventi «regola creata, modificata, disattivata» e «regola applicata a N
  misure» con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza i valori delle etichette.

## 4. Criteri di accettazione

**CA-1 — La regola attribuisce ciò che non ha etichette**
- **Dato** una regola «chiave del fornitore `chiave-prod` → progetto Alfa» e misure importate dal rendiconto su
  quella chiave, prive di etichette
- **Quando** arrivano nuove misure
- **Allora** risultano attribuite al progetto Alfa

**CA-2 — L'etichetta vince sulla regola**
- **Dato** la stessa regola e una misura inviata con etichetta `progetto=beta` sulla stessa chiave
- **Quando** viene registrata
- **Allora** è attribuita a Beta, non ad Alfa

**CA-3 — Anteprima prima del salvataggio**
- **Dato** una regola in costruzione
- **Quando** si chiede l'anteprima sull'ultimo mese
- **Allora** si vede quante misure e quale importo verrebbero attribuiti, e nulla è salvato finché non si conferma

**CA-4 — Nessun effetto retroattivo automatico**
- **Dato** una regola appena salvata
- **Quando** si guarda la spesa del mese precedente
- **Allora** è invariata, e compare l'invito ad applicare la regola allo storico come azione separata

**CA-5 — Suggerimenti**
- **Dato** un account con il 60% della spesa non attribuita, concentrata su due chiavi del fornitore
- **Quando** apre la scheda delle regole
- **Allora** vede due regole suggerite, ciascuna con l'importo che recupererebbe

**CA-6 — Isolamento fra account**
- **Dato** due account con regole sulla stessa condizione
- **Quando** entrambe si applicano
- **Allora** ciascuna agisce solo sulle misure del proprio account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sull'ordine di precedenza e sulla risoluzione delle priorità in conflitto, e di
      **integrazione** sull'applicazione delle regole in ricezione;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulle regole;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «una regola attribuisce la
      spesa importata dal rendiconto», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: `regola_di_attribuzione` in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sull'ordine di precedenza e sull'assenza di effetto
      retroattivo automatico;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `crea_regola_di_attribuzione`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | Le regole servono soprattutto a ciò che arriva dal rendiconto |
| Storia `0019` | Servono dimensioni e valori su cui assegnare |

## 7. Fuori ambito

- l'applicazione delle regole allo **storico**: è la storia `0021`;
- la misura della copertura di attribuzione: è la storia `0021`;
- regole basate sul contenuto della richiesta: **impossibili per costruzione**, perché il contenuto non entra
  nell'app.

## 8. Punti aperti

- **Che cosa fare quando due regole di pari priorità coprono la stessa misura con assegnazioni diverse.** Proposta:
  rifiutare la creazione della seconda regola con un messaggio che indica il conflitto, invece di risolverlo con
  un criterio implicito (per esempio la più recente) che nessuno ricorderebbe. La chiude lo sviluppatore.
