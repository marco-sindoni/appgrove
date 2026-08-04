# 0006 — Collegamento di una fonte in sola lettura

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 02 — Ingresso dei dati di consumo
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile tecnico che paga le chiamate ai modelli
> voglio incollare una chiave di sola lettura del mio fornitore e vedere subito la mia spesa
> così da capire in cinque minuti se questo prodotto mi serve, senza toccare una riga del mio codice.

**Contesto.** È la prima cosa che fa chiunque provi l'app ed è il motivo per cui la scommessa di TokenGrove è
quella a validazione più rapida del catalogo: il valore arriva **senza rilasciare nulla**. Anthropic e OpenAI
espongono entrambi interfacce amministrative che restituiscono consumo e costo dell'organizzazione a fronte di una
chiave dedicata (§2.6, fonti 1-3): questa storia le fa collegare. Va detto con onestà a chi collega che cosa
riusciremo a vedere e cosa no, perché il costo è disponibile **solo a granularità giornaliera** e l'attribuzione
nativa si ferma a chiave, progetto o spazio di lavoro.

## 2. Requisiti funzionali

1. **RF-1** — Dalla sezione Fonti si aggiunge una fonte scegliendo il fornitore da un elenco e incollando una
   credenziale amministrativa **di sola lettura**.
2. **RF-2** — Alla conferma il servizio esegue una verifica immediata: interroga il fornitore su un intervallo
   breve e mostra l'esito. Una credenziale non valida, scaduta o senza i permessi giusti produce un messaggio che
   dice **quale** dei tre casi è, non un generico «errore».
3. **RF-3** — La credenziale è custodita cifrata nell'archivio dei segreti, mai in tabella e mai nei registri;
   dopo il salvataggio non è più leggibile da nessuno, nemmeno da chi amministra la piattaforma: si può solo
   sostituire o revocare.
4. **RF-4** — La schermata di collegamento dichiara **prima** della conferma che cosa TokenGrove leggerà (conteggi,
   costi, modelli, chiavi e spazi di lavoro), che cosa **non** leggerà (nessun contenuto di richieste e risposte) e
   con quale ritardo i dati saranno disponibili.
5. **RF-5** — Una fonte si può sospendere e scollegare; allo scollegamento la credenziale è cancellata subito,
   mentre le misure già raccolte **restano** (sono la storia contabile del cliente) e la fonte resta visibile come
   scollegata.
6. **RF-6** — Il piano gratuito ammette una sola fonte di rendiconto; il tentativo di aggiungerne una seconda
   spiega il limite e come rimediare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `fonte` filtra per `tenant_id` preso dal
  gettone verificato; un `tenant_id` che arrivasse dal corpo della richiesta viene ignorato. Prova di isolamento
  fra due account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/spesa_modelli/v1/fonti`,
  `GET /api/spesa_modelli/v1/fonti`, `POST /api/spesa_modelli/v1/fonti/{id}/verifica`,
  `DELETE /api/spesa_modelli/v1/fonti/{id}`; corpo validato; errori in `problem+json` che distinguono credenziale
  non valida, permessi insufficienti e fornitore irraggiungibile; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** La tabella `fonte` esiste già (storia `0002`); qui si aggiunge il riferimento al
  segreto e lo stato. Nessuna colonna che contenga la credenziale in chiaro.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Fonti» del modulo `spesa_modelli`; dati letti con il client
  generato; solo token del sistema di design; funziona in tema chiaro e scuro. Il campo della credenziale non viene
  mai ripopolato dopo il salvataggio.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `spesa_modelli` e sono presenti in
  `en, it, fr, es, de`, messaggi di errore compresi.
- **RT-6 — Varchi e ruoli (§6).** Collegare e scollegare una fonte è riservato ai ruoli `owner` e `admin`; un
  `member` la vede ma non la modifica, e riceve `403` se ci prova.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `stato_fonti() → elenco`, marcato **lettura**.
  Il collegamento di una fonte **non** è esposto come strumento di scrittura, perché comporta l'inserimento di una
  credenziale: è un'azione che si fa guardando lo schermo. Motivo registrato nel registro delle decisioni.
- **RT-8 — Dati personali (§10).** Nessuna categoria nuova di dati personali; la credenziale non è un dato
  personale ma un **segreto del cliente** e va trattata come tale (cifratura, nessun registro, cancellazione
  immediata allo scollegamento). Voce aggiunta al manifesto per `fonte.creata_da`, in italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** Eventi «fonte collegata», «verifica fallita con causa», «fonte
  scollegata» con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione. **Nessun frammento della
  credenziale** compare nei registri, nemmeno troncato.

## 4. Criteri di accettazione

**CA-1 — Collegamento riuscito**
- **Dato** un utente con ruolo `admin` e una credenziale valida di sola lettura del fornitore simulato
- **Quando** aggiunge la fonte
- **Allora** la verifica riesce, la fonte passa in stato attivo e la schermata mostra da quale giorno comincerà a
  raccogliere e con quale ritardo atteso

**CA-2 — Credenziale sbagliata: si dice quale è il problema**
- **Dato** una credenziale valida ma **senza** i permessi amministrativi necessari
- **Quando** si tenta il collegamento
- **Allora** l'esito distingue esplicitamente «permessi insufficienti» da «credenziale non valida» e spiega quale
  permesso serve; nulla viene salvato

**CA-3 — Il segreto non torna più fuori**
- **Dato** una fonte collegata
- **Quando** si rilegge la fonte dall'interfaccia, dalle rotte o dai registri
- **Allora** la credenziale non compare in nessuna forma, nemmeno parziale

**CA-4 — Scollegamento**
- **Dato** una fonte attiva con misure già raccolte
- **Quando** l'utente la scollega
- **Allora** la credenziale è cancellata immediatamente, le misure restano consultabili e la fonte risulta
  scollegata con la data

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie fonti
- **Quando** un utente di `A` chiede l'elenco delle fonti forzando l'identificativo di `B`
- **Allora** vede solo le proprie

**CA-6 — Limite del piano gratuito**
- **Dato** un account sul piano gratuito con una fonte di rendiconto già collegata
- **Quando** tenta di collegarne una seconda
- **Allora** riceve un rifiuto che spiega il limite del piano e come rimediare, e nulla viene salvato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla distinzione delle cause di errore e di **integrazione** sulle rotte, con database
      effimero, migrazioni vere e fornitore simulato;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla risorsa `fonte`;
- [ ] **prova end-to-end**: **coprire ora** il primo passo del percorso `[J-SPESA-MODELLI]` (collegamento di una
      fonte al fornitore simulato) ed estendere il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `fonte.creata_da`;
- [ ] **registro delle decisioni** compilato, in particolare sul perché il collegamento non è uno strumento
      conversazionale;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `stato_fonti`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | Serve la tabella `fonte` |
| Storia `0003` | Serve la sezione «Fonti» del modulo |
| Storia `0005` | Serve il fornitore simulato: in locale non si chiamano fornitori veri |
| Archivio dei segreti di piattaforma | La credenziale non può stare in tabella |

## 7. Fuori ambito

- il recupero periodico dei dati: è della storia `0007`;
- il canale di **invio** delle misure dal prodotto del cliente: è una fonte di tipo diverso e nasce nella storia
  `0009`.

## 8. Punti aperti

- **Che cosa fare quando il fornitore non offre una credenziale di sola lettura.** Alcuni fornitori minori non
  distinguono i permessi: chiedere una chiave piena significherebbe custodire una credenziale che potrebbe anche
  spendere. La proposta è **non collegare** quei fornitori finché non offrono la sola lettura, e dirlo nell'elenco.
  È una decisione di prodotto: la chiude lo sviluppatore.
