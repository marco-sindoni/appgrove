# 0022 — Segmenti di contatti

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 05 — Contatti, campagne e recupero
**Storia**: `0022` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio poter dire «i clienti che hanno comprato negli ultimi tre mesi» senza spuntare duecento caselle
> così da mandare l'offerta giusta alle persone giuste.

**Contesto.** Senza segmenti, una campagna è o «tutti» o una selezione a mano che nessuno rifà due volte.
Serve però resistere alla tentazione di costruire un motore di regole complicato: il §2.5 dell'analisi dice
che il vocabolario da marketing e i costruttori a diagrammi sono proprio ciò che il segmento rifiuta. Qui
bastano pochi criteri, combinati con la «e» logica, ognuno dei quali un negoziante capisce leggendolo.

## 2. Requisiti funzionali

1. **RF-1** — Un segmento ha un nome e un insieme di criteri combinati con la «e» logica.
2. **RF-2** — I criteri disponibili sono cinque, non di più: consenso agli invii promozionali, ha ordinato
   almeno una volta, ha ordinato negli ultimi N giorni, ha speso più di X, lingua del contatto.
3. **RF-3** — Il segmento si **ricalcola alla lettura**: non conserva un elenco di persone, conserva i criteri.
4. **RF-4** — Prima di salvare, l'app mostra quante persone rientrano nel segmento **adesso** e ne mostra un
   campione.
5. **RF-5** — I segmenti si creano, si modificano, si duplicano e si cancellano; la cancellazione di un
   segmento non tocca i contatti.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Segmenti e valutazione dei criteri filtrano per `tenant_id` preso
  dal token verificato: un segmento non può mai includere contatti di un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/chat_commerce/v1/segments`,
  `GET|PUT|DELETE /api/chat_commerce/v1/segments/{id}` e `GET .../segments/{id}/preview`; corpo validato (i
  criteri sono un elenco chiuso: qualunque altro criterio è respinto); errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V15__segmenti.sql`: tabella `segment` con `tenant_id`, chiave
  primaria UUID versione 7, criteri in forma strutturata (**mai** un frammento di interrogazione scritto
  dall'utente), colonne di controllo e cancellazione logica.
- **RT-4 — Dati personali (§10).** Il segmento contiene **criteri**, non persone: nessuna voce nuova nel
  manifesto. È una scelta consapevole — conservare l'elenco calcolato significherebbe duplicare dati personali
  che invecchiano e che la cancellazione dovrebbe inseguire.
- **RT-5 — Modulo frontend (§3, §4, §5).** Sezione Segmenti dentro Contatti, con costruttore a criteri fissi e
  anteprima del conteggio. Tutte le stringhe, **compresi i nomi dei criteri**, in `en, it, fr, es, de`.
- **RT-6 — Registrazione eventi (§14).** `segmento creato`, `segmento valutato` con il conteggio, `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione, senza elenchi di persone.

## 4. Criteri di accettazione

**CA-1 — Creazione con anteprima**
- **Dato** un account con 300 contatti, 80 dei quali hanno ordinato negli ultimi 90 giorni
- **Quando** si crea il segmento «clienti recenti» con quel criterio
- **Allora** l'anteprima dice 80 e mostra un campione, prima del salvataggio

**CA-2 — Criteri combinati**
- **Dato** il segmento «clienti recenti» · **Quando** si aggiunge il criterio «consenso dato»
- **Allora** il conteggio scende ai soli contatti che soddisfano **entrambi** i criteri

**CA-3 — Ricalcolo**
- **Dato** un segmento salvato con 80 persone · **Quando** dieci contatti revocano il consenso
- **Allora** alla lettura successiva il segmento ne conta 70, senza che nessuno l'abbia aggiornato

**CA-4 — Criterio non ammesso**
- **Dato** una richiesta che indica un criterio non previsto · **Quando** si tenta il salvataggio
- **Allora** la risposta è `400` e nulla viene salvato

**CA-5 — Isolamento fra account**
- **Dato** due account con contatti che soddisfano gli stessi criteri
- **Quando** `A` valuta il proprio segmento · **Allora** il conteggio comprende solo i contatti di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** su ciascuno dei cinque criteri e sulla loro combinazione, e di **integrazione** sul
      ricalcolo alla lettura;
- [ ] prova di **isolamento fra account** sulla valutazione dei segmenti;
- [ ] **prova end-to-end**: *nessun impatto diretto* — il segmento è coperto attraverso la campagna della
      storia `0023` dentro il percorso della storia `0029`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i nomi dei criteri;
- [ ] **manifesto dei dati**: nessuna voce nuova (i segmenti conservano criteri, non persone);
- [ ] **registro delle decisioni** compilato, con l'elenco chiuso dei cinque criteri e il perché del ricalcolo;
- [ ] contratto degli **strumenti conversazionali**: la lettura di un segmento restituisce il **conteggio** e
      non l'elenco delle persone;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0021` | I criteri operano sui contatti e sul loro storico |

## 7. Fuori ambito

- criteri combinati con «oppure» e con esclusioni: rimandati finché non ci sarà una richiesta vera;
- le etichette manuali sui contatti: sarebbero un sesto criterio e una funzione a sé;
- l'invio: storia `0023`.

## 8. Punti aperti

- Nessuno.
