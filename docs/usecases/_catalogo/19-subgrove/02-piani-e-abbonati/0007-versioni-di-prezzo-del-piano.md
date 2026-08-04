# 0007 — Versioni di prezzo del piano

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 02 — Piani e abbonati
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che a gennaio aumenta i prezzi
> voglio che chi è già abbonato resti sul prezzo che ha firmato, finché non decido diversamente
> così da non ritrovarmi cento telefonate di gente che ha visto un addebito diverso da quello concordato.

**Contesto.** Il prezzo è la parte del piano che cambia, e cambiarla nel posto sbagliato è il difetto più costoso
che questa app possa avere: se il prezzo vive dentro il piano e lo si modifica, **tutti** gli abbonamenti in
corso cambiano importo alla prossima scadenza, retroattivamente e senza che nessuno l'abbia deciso. È lo stesso
problema che la piattaforma ha già risolto per sé con la regola dell'**immutabilità del prezzo vivo** — prezzo
nuovo, vecchio archiviato, chi è dentro resta sul suo ([docs/09-pagamenti.md](../../../../09-pagamenti.md)
dec. 35). Questa storia applica la stessa regola, con le stesse parole, ai piani del cliente: è uno dei riusi di
semantica dichiarati al §10.1 della descrizione, e vale la pena riconoscerlo come tale.

## 2. Requisiti funzionali

1. **RF-1** — Un piano ha una o più **versioni di prezzo**, ciascuna con importo (in centesimi), valuta,
   aliquota d'imposta applicata, data di decorrenza e stato (`viva` o `archiviata`).
2. **RF-2** — Una versione di prezzo con abbonamenti agganciati **non si modifica nell'importo**: il tentativo è
   rifiutato con un messaggio che spiega la via corretta.
3. **RF-3** — Cambiare prezzo significa **creare una versione nuova** e archiviare la precedente: i nuovi
   abbonamenti prendono la versione viva, quelli esistenti restano agganciati alla loro.
4. **RF-4** — Esiste un'azione esplicita e separata per **spostare** gli abbonamenti esistenti sulla versione
   nuova, che dice quanti ne coinvolge e da quando ha effetto (dalla prima scadenza successiva, mai prima), e
   richiede una conferma.
5. **RF-5** — L'elenco delle versioni mostra, per ciascuna, quanti abbonamenti la usano: una versione archiviata
   con zero abbonamenti si può eliminare, una con abbonamenti no.
6. **RF-6** — L'importo si esprime **in centesimi**, e l'interfaccia lo presenta nel formato della lingua attiva.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Versioni di prezzo filtrate per `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/abbonati/v1/piani/{id}/prezzi` e
  `POST /api/abbonati/v1/piani/{id}/prezzi/{idPrezzo}/sposta-abbonamenti`; errori in `problem+json` con codice
  stabile per «prezzo vivo non modificabile»; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V3__versione_prezzo.sql`: tabella `versione_prezzo` con `tenant_id`,
  chiave UUID versione 7, colonne di controllo e cancellazione logica; l'aggancio dell'abbonamento alla versione
  è un riferimento **logico** dentro lo stesso schema.
- **RT-4 — Modulo frontend (§3, §5).** Dentro la scheda del piano, un riquadro «Prezzi» con la versione viva in
  evidenza e lo storico sotto; l'azione di spostamento passa da una finestra di conferma che dice **quanti** e
  **da quando**; solo token del sistema di design.
- **RT-5 — Cinque lingue (§4).** Etichette, messaggi di rifiuto e testo della conferma in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** Nessun consumo di quota. Con abbonamento di piattaforma non attivo, `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura qui: cambiare prezzo a una
  clientela intera non è un'operazione da chat. La lettura del prezzo vivo entra in `elenca_abbonamenti`
  (storia `0031`).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-9 — Registrazione eventi (§14).** `versione di prezzo creata`, `modifica rifiutata su prezzo vivo`,
  `abbonamenti spostati (quanti)` con `tenant_id`, `app_id`, `user_id` e correlazione.

## 4. Criteri di accettazione

**CA-1 — Il prezzo vivo non si modifica**
- **Dato** un piano con una versione di prezzo usata da 30 abbonamenti
- **Quando** l'utente prova a cambiarne l'importo
- **Allora** riceve un rifiuto che spiega di creare una versione nuova, e nulla cambia

**CA-2 — Nuova versione, vecchi abbonati fermi**
- **Dato** lo stesso piano · **Quando** l'utente crea una versione a 45 € e archivia quella a 39 €
- **Allora** i nuovi abbonamenti nascono a 45 €, i 30 esistenti continuano a rinnovarsi a 39 €

**CA-3 — Spostamento esplicito**
- **Dato** i 30 abbonamenti a 39 €
- **Quando** l'utente sceglie di spostarli e conferma
- **Allora** il messaggio ha detto «30 abbonamenti, dalla prima scadenza successiva», nessuna scadenza già
  generata cambia importo, e le successive nascono a 45 €

**CA-4 — Isolamento fra account**
- **Dato** due account con piani omonimi · **Quando** uno legge le versioni di prezzo
- **Allora** vede solo le proprie

**CA-5 — Nessun ricalcolo all'indietro**
- **Dato** uno spostamento appena eseguito · **Quando** si guardano le scadenze già emesse
- **Allora** portano ancora il vecchio importo: il passato non si riscrive

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sulla regola di immutabilità e di **integrazione** sullo spostamento in blocco;
- [ ] prova di **isolamento fra account** sulle versioni di prezzo;
- [ ] **prova end-to-end**: *rimando* — il cambio di prezzo non entra nel percorso principale; voce `da-coprire`
      nel registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con storia
      proprietaria `0033`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato: immutabilità del prezzo vivo come **riuso dichiarato** della
      semantica di piattaforma (dec. 35), e perché lo spostamento è esplicito e non automatico;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | il prezzo appartiene a un piano che deve esistere |

## 7. Fuori ambito

- l'aggancio dell'abbonamento a una versione: storia `0010`, che lo usa;
- il conguaglio proporzionale di un cambio a metà periodo: storia `0014`;
- l'obbligo di **avvisare** l'abbonato del prezzo che si applicherà al rinnovo: storia `0013`, ed è un obbligo di
  legge, non una cortesia.

## 8. Punti aperti

**Prezzi in più valute.** Un cliente che vende oltre confine vorrebbe listini in valute diverse. La proposta
tiene la valuta sulla versione di prezzo, il che lo renderebbe possibile senza cambiare il modello, ma tutto il
resto dell'app (metriche, previsioni, totali) assume **una** valuta per account. Aprirlo davvero significa
decidere come si sommano importi in valute diverse, che è una domanda contabile. Chiude: lo sviluppatore, se e
quando il bisogno si presenta.
