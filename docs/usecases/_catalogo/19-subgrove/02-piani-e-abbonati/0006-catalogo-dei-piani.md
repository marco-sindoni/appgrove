# 0006 — Catalogo dei piani

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 02 — Piani e abbonati
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un centro che vende tre formule diverse
> voglio descrivere una volta per tutte cosa vendo, con quali condizioni
> così da non doverle ripetere a voce a ogni iscritto e da non litigarci sopra fra sei mesi.

**Contesto.** Oggi le condizioni dell'abbonamento vivono in tre posti: il volantino, la memoria del titolare e
un foglio A4 in reception. Ne discende che due iscritti allo stesso «annuale» hanno condizioni diverse, e che
nessuno sa più quali. Questa storia rende il **piano** un dato: cosa costa, con che cadenza, per quanto tempo
minimo, se si rinnova da solo e con quanto preavviso si può disdire. Non è burocrazia: le condizioni di rinnovo
e disdetta sono **materia di legge** (§2.3 della descrizione — preavviso e clausola di rinnovo tacito evidenziata
e accettata separatamente), e se non sono un dato non si può né rispettarle né dimostrarle. È la prima storia
dell'epica perché senza piani non c'è nulla a cui abbonare qualcuno.

**Attenzione a non confondere due cose.** Questi sono i piani che il **cliente** vende ai **suoi** clienti. Il
piano che il cliente ha con appgrove è un'altra cosa, sta nella sezione Fatturazione del backoffice, e non si
tocca da qui (§10.1 della descrizione).

## 2. Requisiti funzionali

1. **RF-1** — Si può creare, modificare e archiviare un piano con: nome, descrizione, **ciclo** (mensile,
   trimestrale, semestrale, annuale), **durata minima** in cicli, **rinnovo tacito** sì/no, **giorni di
   preavviso** per la disdetta, **giorni di prova** iniziali.
2. **RF-2** — Un piano ha tre stati: `bozza` (non sottoscrivibile), `attivo` (sottoscrivibile), `archiviato`
   (non più sottoscrivibile, ma gli abbonamenti in corso restano validi).
3. **RF-3** — Archiviare un piano **non** tocca gli abbonamenti già sottoscritti su di esso; l'elenco dice quanti
   ne restano vivi, così che nessuno archivi al buio.
4. **RF-4** — Un piano con rinnovo tacito attivo **deve** avere un numero di giorni di preavviso maggiore di
   zero: il salvataggio è rifiutato se manca, con una spiegazione che cita l'obbligo.
5. **RF-5** — La schermata avverte esplicitamente di **non usare nomi di persona** come nome del piano, perché il
   nome del piano non è un dato personale e non viene esportato con i dati dell'abbonato.
6. **RF-6** — L'elenco dei piani mostra, per ciascuno, quanti abbonamenti vivi ha e quanto ricavo ricorrente
   mensile rappresenta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dei piani filtra per `tenant_id` preso dal
  token verificato; un identificativo di account che arrivasse dalla richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST|PATCH /api/abbonati/v1/piani` e
  `/api/abbonati/v1/piani/{id}`; corpo validato in modo dichiarativo; errori in `problem+json` con un codice
  stabile per «preavviso mancante su piano a rinnovo tacito»; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V2__piano_condizioni.sql` sullo schema `app_abbonati`: la tabella
  `piano` della storia `0002` si arricchisce dei campi contrattuali, con `tenant_id`, colonne di controllo e
  cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Piani* del modulo `abbonati`: elenco e modulo di inserimento;
  dati letti con il client generato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compresi i nomi dei cicli e i messaggi di rifiuto,
  passano dallo spazio-nomi `abbonati` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** I piani **non** consumano quota: la metrica è `abbonamenti_attivi`. Con
  abbonamento di piattaforma non attivo la sezione risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: la lettura dei piani entra in
  `elenca_abbonamenti` alla storia `0031`. Contratto dichiarato lì, non qui.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo — **a condizione** che il nome del piano non ne
  diventi uno: da qui l'avvertenza di RF-5 e il punto aperto n. 5 della descrizione.
- **RT-9 — Registrazione eventi (§14).** `piano creato`, `piano archiviato`, `salvataggio rifiutato per preavviso
  mancante`, con `tenant_id`, `app_id`, `user_id` e correlazione.

## 4. Criteri di accettazione

**CA-1 — Creazione di un piano completo**
- **Dato** un utente con ruolo sufficiente
- **Quando** crea un piano «Annuale sala pesi», ciclo annuale, durata minima 1 ciclo, rinnovo tacito attivo,
  preavviso 30 giorni
- **Allora** il piano è salvato in stato `bozza` e compare nell'elenco con le sue condizioni

**CA-2 — Rinnovo tacito senza preavviso**
- **Dato** lo stesso utente · **Quando** salva un piano con rinnovo tacito e preavviso a zero
- **Allora** riceve un rifiuto con messaggio esplicito, e nulla viene salvato

**CA-3 — Archiviazione che non rompe nulla**
- **Dato** un piano con 40 abbonamenti vivi
- **Quando** l'utente lo archivia dopo aver visto l'avviso «40 abbonamenti resteranno attivi su questo piano»
- **Allora** il piano sparisce dai sottoscrivibili e i 40 abbonamenti continuano a rinnovarsi

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri piani
- **Quando** un utente di `A` chiede l'elenco dei piani
- **Allora** vede solo i propri, anche forzando l'identificativo dell'altro account nella richiesta

**CA-5 — Cinque lingue**
- **Dato** l'interfaccia in ciascuna lingua · **Quando** si apre il modulo di inserimento
- **Allora** cicli, etichette e messaggi di errore sono tradotti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sulla regola «rinnovo tacito implica preavviso» e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** sui piani;
- [ ] **prova end-to-end**: *rimando* — la creazione del piano è il primo passo del percorso `[J-ABBONATI]` della
      storia `0033`; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la motivazione scritta (il piano non descrive persone);
- [ ] **registro delle decisioni** compilato: quali condizioni contrattuali sono dati e perché;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | serve la tabella `piano` |
| storia `0003` | serve la sezione *Piani* nel modulo |

## 7. Fuori ambito

- il **prezzo** del piano e la sua immutabilità: storia `0007` — è un'entità a parte proprio per questo;
- la sottoscrizione di un abbonamento su un piano: storia `0010`;
- gli sconti, le promozioni e i codici: non hanno una storia in questo indice (vedi punto aperto);
- i piani che il cliente ha con appgrove: sono di piattaforma.

## 8. Punti aperti

**Sconti e promozioni.** Sono la prima richiesta prevedibile dopo il lancio («i primi tre mesi a metà prezzo»,
«sconto famiglia»), e sono anche il modo più rapido per rendere illeggibile il calcolo del ricavo ricorrente
(epica 06). **Proposta**: tenerli fuori dal primo giro e, quando arriveranno, modellarli come **versione di
prezzo dedicata** anziché come percentuale applicata al volo — così restano dentro la regola di immutabilità
della storia `0007` e le metriche continuano a tornare. Chiude: lo sviluppatore, con la direzione di prodotto.
