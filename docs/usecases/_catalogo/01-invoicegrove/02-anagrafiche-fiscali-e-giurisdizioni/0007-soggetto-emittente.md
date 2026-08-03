# 0007 — Soggetto emittente

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 02 — Anagrafiche fiscali e giurisdizioni
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo
> voglio dichiarare una volta sola chi è il soggetto che emette le fatture, con la sua partita IVA e il suo regime
> così da non doverlo riscrivere su ogni documento e da non sbagliarlo mai.

**Contesto.** Ogni documento fiscale porta i dati dell'emittente, e ogni giurisdizione ne pretende di suoi: in
Italia servono partita IVA, codice fiscale e regime fiscale; in Belgio l'identificativo IVA e l'identificativo di
rete. Un'impresa può avere più soggetti emittenti (una seconda partita IVA, una sede estera): il piano `europa`
della proposta di listino ne prevede fino a tre, il piano `studio` senza limite. La storia apre l'epica perché
tutto il resto — controparti, documenti, canali — pende da qui.

## 2. Requisiti funzionali

1. **RF-1** — Si può creare, modificare e disattivare un soggetto emittente con: denominazione, identificativo
   IVA, codice fiscale, indirizzo completo, paese, regime fiscale, contatto amministrativo.
2. **RF-2** — Ogni soggetto emittente dichiara la propria **giurisdizione** di appartenenza, scelta fra quelle
   attive nel registro (storia `0008`).
3. **RF-3** — L'identificativo IVA è validato **nella forma prevista dal paese** dichiarato; un formato non valido
   viene rifiutato con un messaggio che dice cosa ci si aspettava.
4. **RF-4** — Un soggetto emittente con documenti già trasmessi non si può cancellare: si **disattiva**, e resta
   leggibile per lo storico e per la conservazione.
5. **RF-5** — Il numero di soggetti emittenti attivi è limitato dal piano; superarlo risponde `402` con
   l'indicazione del piano che lo consente.
6. **RF-6** — La sezione «Impostazioni» del modulo elenca i soggetti emittenti e ne permette la gestione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `legal_entity` filtra per `tenant_id` preso
  dal token verificato; un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato. Prova di isolamento
  fra due account sulla risorsa.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/einvoicing/v1/legal-entities` e
  `GET|PUT|DELETE /api/einvoicing/v1/legal-entities/{id}`; corpo validato in modo dichiarativo sugli oggetti di
  trasferimento; errori in `application/problem+json`; paginazione a pagina/dimensione con totale; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V3__legal_entity_fields.sql` sullo schema `app_einvoicing`: la tabella
  `legal_entity` si arricchisce dei campi fiscali, con `tenant_id`, chiave UUID versione 7, colonne di controllo e
  cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Impostazioni → Soggetti emittenti» del modulo `einvoicing`; dati
  letti con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `einvoicing` e sono presenti in
  `en, it, fr, es, de`, compresi i messaggi di validazione del formato dell'identificativo IVA.
- **RT-6 — Varchi e quota (§6, §7).** Il soggetto emittente **non** consuma la metrica `documenti`: il limite sul
  numero di soggetti è un effetto del piano, non della quota, e si esprime con `402`, non con `429`. Ruolo
  richiesto per la modifica: `owner` o `admin`; il ruolo `member` legge soltanto.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: la configurazione fiscale non
  si comanda a voce, si guarda. Se un giorno servisse, sarebbe scrittura con conferma.
- **RT-8 — Dati personali (§10).** **Sì.** Se il cliente è una ditta individuale o un libero professionista,
  denominazione e codice fiscale sono dati di una persona. Voci `legal_entity.denominazione` e
  `legal_entity.codice_fiscale` nel manifesto in italiano e inglese, campi annotati `@PersonalData`, tabella
  presente in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `soggetto emittente creato`, `modificato`, `disattivato` sono
  registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** denominazione né
  codice fiscale: solo l'identificativo interno.

## 4. Criteri di accettazione

**CA-1 — Creazione di un soggetto emittente italiano**
- **Dato** un utente con ruolo `admin` su un account abilitato
- **Quando** crea un soggetto con denominazione, partita IVA italiana valida, indirizzo e regime
- **Allora** il soggetto è creato, compare nell'elenco e la sua giurisdizione risulta «Italia»

**CA-2 — Identificativo IVA in formato sbagliato**
- **Dato** lo stesso utente
- **Quando** inserisce una partita IVA italiana di 10 cifre invece di 11
- **Allora** riceve `400` con un messaggio che dice quale formato ci si aspettava, e nulla viene creato

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri soggetti emittenti
- **Quando** un utente di `A` chiede l'elenco dei soggetti emittenti
- **Allora** vede solo i propri, anche se forza l'identificativo dell'account `B` nella richiesta

**CA-4 — Non si cancella ciò che ha già emesso**
- **Dato** un soggetto emittente con almeno un documento trasmesso
- **Quando** si tenta di cancellarlo
- **Allora** l'operazione è rifiutata con una spiegazione, e resta disponibile la disattivazione

**CA-5 — Limite del piano**
- **Dato** un account sul piano che consente un solo soggetto emittente, con uno già attivo
- **Quando** tenta di crearne un secondo
- **Allora** riceve `402` con l'indicazione del piano che lo consentirebbe

**CA-6 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** tenta di modificare un soggetto emittente
- **Allora** riceve `403`, mentre la lettura riesce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla validazione del formato dell'identificativo IVA per paese e di **integrazione**
      sulla risorsa, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla risorsa;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-EINVOICING]` (storia `0030`) attraverserà la creazione del
      soggetto emittente come primo passo;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati, tabella in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: nessuno, e il motivo è scritto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Serve la tabella `legal_entity` con le colonne comuni |
| `0003` | Serve il modulo per la sezione Impostazioni |
| `0006` (registro delle giurisdizioni) | La giurisdizione va scelta fra quelle attive nel registro, che deve quindi esistere prima |

## 7. Fuori ambito

- Il **canale di trasmissione** configurato per il soggetto (credenziali, identificativo di rete): appartiene alle
  storie `0017` e `0018`, che sanno cosa serve a ciascun canale.
- La verifica di **esistenza** dell'identificativo IVA presso un registro pubblico: qui si valida solo la forma;
  l'esistenza si verifica per le controparti, storia `0009`.

## 8. Punti aperti

- **Quanti soggetti emittenti per piano** è una decisione di listino, quindi una fermata di escalation dello
  sviluppatore (descrizione dell'applicazione §5). La storia legge il limite dal piano, non lo fissa.
- Se un giorno servisse un soggetto emittente in una giurisdizione **non implementata**, la scelta fra «lo si
  crea e non si può trasmettere» e «lo si rifiuta» è direzione di prodotto: qui si adotta la prima, perché
  l'anagrafica serve anche solo per l'archivio, ma va confermata.
