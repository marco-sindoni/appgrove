# 0009 — Anagrafica dei fornitori

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 02 — Anagrafiche e catalogo prodotti
**Storia**: `0009` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve ricomprare
> voglio sapere da chi compro abitualmente ciascun articolo e a chi telefono per farlo
> così da poter trasformare un elenco di cose che stanno finendo in una lista della spesa già divisa per fornitore.

**Contesto.** Il fornitore entra in questa app per una sola ragione: senza di lui la proposta di riordino (storia
`0028`) è un elenco di articoli che qualcuno deve ancora smistare a mano, cioè esattamente «l'avviso che non porta
a niente» che le fonti indicano come il motivo per cui questi programmi vengono abbandonati (descrizione
dell'applicazione, §2.5). Va introdotto **ora** e non nell'epica 05 perché il fornitore preferito è un attributo
dell'articolo, e appenderlo dopo a un'anagrafica già piena costa una migrazione.

Questa è **l'unica tabella dell'applicazione fatta di dati di persone**. Tutto il resto — articoli, depositi,
movimenti, giacenze — parla di cose. Qui invece si scrive la ragione sociale di una ditta che può essere
individuale, il nome di chi risponde al telefono, il suo indirizzo di posta elettronica e il suo numero: dati
personali a tutti gli effetti quando la controparte è una persona fisica, e vanno trattati come tali dal primo
giorno, non sistemati dopo (descrizione, §6).

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella `fornitore` con `ragione_sociale`, `persona_riferimento`, `email`, `telefono`,
   `partita_iva`, `codice_fiscale`, `note` e `stato` (`attivo` | `archiviato`), oltre a `tenant_id`, chiave
   primaria UUID versione 7, colonne di controllo e cancellazione logica.
2. **RF-2** — La sola `ragione_sociale` è obbligatoria: una micro-impresa registra spesso solo il nome del
   fornitore e completa dopo. I campi vuoti non bloccano nulla.
3. **RF-3** — L'articolo ha un **fornitore preferito** facoltativo: è il fornitore che la proposta di riordino
   userà per raggruppare. Un articolo senza fornitore preferito finisce nel gruppo «da assegnare», mai perso.
4. **RF-4** — Il fornitore si **archivia**, non si cancella dall'interfaccia: gli articoli che lo indicavano
   restano leggibili con il riferimento evidenziato come non più attivo, e la proposta di riordino smette di
   proporlo.
5. **RF-5** — L'elenco dei fornitori è paginato, cercabile per ragione sociale e per identificativo fiscale, e la
   scheda di ciascuno mostra **quali articoli** lo indicano come preferito.
6. **RF-6** — Se compilata, la partita IVA è validata nella forma (undici cifre e carattere di controllo per
   l'Italia; per gli altri paesi si accetta il formato dichiarato senza pretendere di validarlo) e l'indirizzo di
   posta elettronica è validato nella forma.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `fornitore` filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Prova di
  isolamento fra due account sulla risorsa, con particolare attenzione alla ricerca per identificativo fiscale, che
  non deve mai attraversare gli account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/magazzino/v1/fornitori`,
  `GET|PATCH /api/magazzino/v1/fornitori/{id}`, `POST /api/magazzino/v1/fornitori/{id}/archiviazione`, più il campo
  `fornitore_preferito_id` sulla risorsa `articoli`; oggetti di trasferimento al bordo; validazione dichiarativa;
  errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V5__fornitore.sql` sullo schema `app_magazzino`: tabella `fornitore` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e `deleted_at`; colonna
  `fornitore_preferito_id` aggiunta ad `articolo` come riferimento **interno** allo schema. Nessuna chiave esterna
  verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sotto-sezione «Fornitori» dentro la sezione `impostazioni` del modulo
  `magazzino`, più il selettore del fornitore preferito nella scheda dell'articolo. Dati letti con il client
  generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — etichette dei campi, errori di validazione, avviso
  sui dati di persone — passano dallo spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Il fornitore **non consuma** la metrica `articoli_gestiti`: il tetto è sugli
  articoli attivi. Con abbonamento `canceled` il servizio risponde `402`; l'esportazione e la cancellazione dei
  dati restano accessibili in ogni caso (storia `0010`).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo in questa storia. Il fornitore compare come
  **campo del risultato** di `elenca_sotto_scorta` (storia `0034`), mai come destinatario di un'azione: l'app non
  manda niente a nessuno fuori dall'azienda, quindi non esiste e non esisterà uno strumento «ordina al fornitore»
  (descrizione, §7).
- **RT-8 — Dati personali (§10).** **La storia introduce dati personali** ed è l'unica dell'app a farlo in modo
  diretto. Voci nuove nel manifesto `docs/compliance/manifests/magazzino.yaml` in italiano e inglese per
  `fornitore.ragione_sociale`, `fornitore.persona_riferimento`, `fornitore.email`, `fornitore.telefono`,
  `fornitore.partita_iva` e `fornitore.codice_fiscale`, con base giuridica, finalità e durata di conservazione
  proposte al §6 della descrizione; campi annotati `@PersonalData` — **un campo annotato e non dichiarato fa
  fallire la compilazione**; tabella `fornitore` aggiunta a `exportData` e `purgeData` del contratto
  `MagazzinoDataContract` (storia `0010`). Nessuna categoria particolare dell'articolo 9 è coinvolta.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `fornitore creato`, `fornitore archiviato` e
  `fornitore preferito impostato` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, **senza** ragione sociale, nomi, indirizzi di posta elettronica, numeri di telefono e
  identificativi fiscali: nei registri si scrivono identificativi, non dati di persone.

## 4. Criteri di accettazione

**CA-1 — Fornitore con la sola ragione sociale**
- **Dato** un utente autenticato di un account abilitato
- **Quando** crea il fornitore «Ferramenta Rossi» senza compilare altro
- **Allora** il fornitore esiste in stato `attivo`, compare nell'elenco e può essere scelto come preferito su un
  articolo

**CA-2 — Partita IVA malformata**
- **Dato** il modulo del fornitore · **Quando** si inserisce una partita IVA italiana di dieci cifre
- **Allora** la risposta è `400` in `application/problem+json`, il messaggio dice quale formato è atteso, nulla
  viene salvato e gli altri campi restano compilati nel modulo

**CA-3 — Archiviazione con articoli collegati**
- **Dato** il fornitore «Ferramenta Rossi», preferito su 12 articoli
- **Quando** lo si archivia
- **Allora** l'operazione riesce, la scheda dei 12 articoli mostra il riferimento come non più attivo e la proposta
  di riordino li sposta nel gruppo «da assegnare»

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con un fornitore avente la stessa partita IVA
- **Quando** un utente di `A` cerca per quella partita IVA
- **Allora** trova solo il proprio fornitore, anche forzando l'identificativo dell'account `B` nella richiesta

**CA-5 — I dati del fornitore non finiscono nei registri**
- **Dato** un fornitore creato con nome, indirizzo di posta elettronica e telefono
- **Quando** si ispezionano le righe di registro prodotte dall'operazione
- **Allora** compaiono `tenant_id`, `app_id`, `user_id`, l'identificativo di correlazione e l'identificativo del
  fornitore, e **nessuno** dei valori dei campi anagrafici

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla validazione della partita IVA e sull'effetto dell'archiviazione sul raggruppamento;
      prove di **integrazione** sulla risorsa `fornitori`, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `fornitore`, compresa la ricerca per identificativo fiscale;
- [ ] **prova end-to-end**: *rimando* — il fornitore entra nel percorso `[J-MAGAZZINO]` attraverso la proposta di
      riordino, di proprietà della storia `0028`, e il percorso è della `0036`; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le sei voci del fornitore, campi annotati
      `@PersonalData`, tabella presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta della sola ragione sociale obbligatoria e con la
      classificazione dei dati del fornitore;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento nuovo, e il divieto di uno strumento che
      contatti il fornitore è messo per iscritto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | Il fornitore preferito è una colonna dell'articolo: l'anagrafica deve esistere |
| `0010` (in avanti) | Il manifesto completo e il contratto di esportazione e cancellazione si chiudono lì; questa storia porta le sue voci |

## 7. Fuori ambito

- **Inviare qualcosa al fornitore** — ordini, richieste di offerta, messaggi di posta elettronica: l'app non manda
  niente a nessuno fuori dall'azienda (descrizione, §1). L'invio dell'ordine è di ProcureGrove (catalogo 48).
- **Condizioni di acquisto, listini fornitore, tempi di consegna e minimi d'ordine**: sono materia del ciclo degli
  acquisti, non del magazzino. Il solo dato affine che StockGrove tiene è il costo medio d'acquisto (storia
  `0025`), che nasce dai carichi e non da un listino.
- **Più fornitori per articolo con priorità**: qui il preferito è uno. Se servisse una graduatoria, è una funzione
  del riordino e va valutata nell'epica 05.
- **Anagrafica condivisa dei fornitori fra le app della suite**: non esiste oggi; se nascerà, vale lo stesso
  ragionamento fatto per il catalogo prodotti nella storia `0012`.

## 8. Punti aperti

- **Durata di conservazione dei dati del fornitore** (la descrizione propone «durata del rapporto più dieci anni
  dall'ultimo movimento collegato»): è dedotta per analogia dal termine di prescrizione ordinaria e dalla
  conservazione dei documenti contabili, **non è un dato rilevato**. Chiude lo sviluppatore, con revisione legale.
- **Base giuridica del trattamento**: la proposta è l'esecuzione del contratto per i dati della ditta e il
  legittimo interesse per la persona di contatto. È una classificazione da confermare, non una decisione presa da
  un agente (descrizione, §6). Chiude lo sviluppatore.