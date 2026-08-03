# 0008 — Catalogo di prodotti e servizi

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 02 — Anagrafica, catalogo e listini
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che prepara le offerte
> voglio un elenco delle cose che vendo, con descrizione, unità di misura e prezzo base
> così da comporre un preventivo scegliendo dalle voci invece di riscriverle, sempre uguali e sempre diverse.

**Contesto.** Nella ricerca di mercato i prodotti italiani a basso prezzo si fermano al documento; quelli
anglosassoni hanno il catalogo ma lo vendono a caro prezzo. Il catalogo è ciò che rende ripetibile il lavoro:
senza, ogni preventivo è un foglio bianco e i prezzi divergono da un cliente all'altro senza che nessuno se ne
accorga. È anche una delle **entità condivise** che il catalogo appgrove indica come collante della suite
(prodotti e listini sono condivisi con magazzino, retail e verticali).

## 2. Requisiti funzionali

1. **RF-1** — Si creano, modificano, cercano e disattivano voci di catalogo con: codice interno, descrizione
   breve e lunga, unità di misura, prezzo base, aliquota d'imposta predefinita, tipo (**prodotto** o **servizio**).
2. **RF-2** — Il codice interno è univoco per account.
3. **RF-3** — Le voci si organizzano per **categoria** (un livello solo: niente alberi), per poterle filtrare.
4. **RF-4** — La descrizione lunga può essere resa in ciascuna delle cinque lingue: è il testo che finirà nel
   documento del cliente straniero.
5. **RF-5** — Una voce disattivata non si può più aggiungere a un preventivo nuovo, ma resta leggibile su quelli
   che la citano.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `voce_catalogo` filtra per `tenant_id` preso
  dal token verificato; il `tenant_id` che arrivasse dalla richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `/api/preventivi/v1/catalogo`, corpo validato, errori in
  `problem+json`, definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V3__catalogo.sql` sullo schema `app_preventivi`: tabella
  `voce_catalogo` con `tenant_id`, UUID versione 7, colonne di controllo, cancellazione logica; unicità su
  `(tenant_id, codice)`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione **Catalogo e listini → Voci**; solo token del sistema di design;
  tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Sia l'interfaccia sia le descrizioni **dei dati** sono a cinque lingue: attenzione
  a non confonderle — la prima passa dallo spazio-nomi `preventivi`, la seconda è contenuto del cliente.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: un prodotto non è una persona. Fa eccezione il
  testo libero della descrizione, già dichiarato come rischio trasversale nella descrizione dell'applicazione.
- **RT-7 — Registrazione eventi (§14).** `voce di catalogo creata`, `voce disattivata`, con gli identificativi
  d'obbligo e senza contenuti.

## 4. Criteri di accettazione

**CA-1 — Creazione e ricerca**
- **Dato** un utente abilitato · **Quando** crea la voce `INST-01` «Installazione contatore», unità «ora»,
  prezzo base 45 € · **Allora** la ritrova cercando per codice o per descrizione e la può filtrare per categoria

**CA-2 — Codice duplicato**
- **Dato** una voce con codice `INST-01` · **Quando** se ne crea un'altra con lo stesso codice · **Allora** `409`
  in `problem+json` e nulla viene creato

**CA-3 — Isolamento fra account**
- **Dato** due account con lo stesso codice `INST-01` · **Quando** ciascuno cerca il proprio · **Allora** vede la
  propria voce, e la coincidenza dei codici non crea alcun conflitto

**CA-4 — Voce disattivata**
- **Dato** una voce disattivata citata da un preventivo esistente · **Quando** si apre quel preventivo · **Allora**
  la riga si legge normalmente, ma la voce non compare più fra quelle selezionabili in un documento nuovo

**CA-5 — Descrizione in più lingue**
- **Dato** una voce con descrizione lunga in italiano e in tedesco · **Quando** si compone un preventivo per un
  destinatario con lingua preferita tedesca · **Allora** il documento usa il testo tedesco

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sull'unicità del codice e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** sulla risorsa nuova;
- [ ] **prova end-to-end**: rimando alla storia `0029` (la scelta di una voce di catalogo è un passo del percorso);
- [ ] **traduzioni** dell'interfaccia in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, motivata sopra;
- [ ] **registro delle decisioni** compilato (un solo livello di categoria, descrizioni multilingua come contenuto
      del cliente);
- [ ] avvio locale invariato; l'insieme di dati di prova della storia `0005` è esteso con un catalogo minimo.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0003` | la sezione dove vive il catalogo |

## 7. Fuori ambito

- giacenze e movimenti di magazzino: sono di StockGrove (catalogo 14);
- varianti e configuratori di prodotto: rifiutati esplicitamente dal segmento (§2.5 della descrizione);
- l'importazione da file: rimandata.

## 8. Punti aperti

Nessuno.
