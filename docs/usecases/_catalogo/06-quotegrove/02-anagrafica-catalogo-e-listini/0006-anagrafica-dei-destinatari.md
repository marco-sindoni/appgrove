# 0006 — Anagrafica dei destinatari

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 02 — Anagrafica, catalogo e listini
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che prepara le offerte
> voglio tenere l'elenco di chi riceve i miei preventivi, con la persona di riferimento e i recapiti giusti
> così da non ridigitare ogni volta ragione sociale, indirizzo e posta elettronica, e da non sbagliare intestazione.

**Contesto.** Nella storia `0002` il destinatario è un testo libero: va bene per far partire lo schema, non per
lavorare. Serve una anagrafica vera, perché su di essa poggiano il listino dedicato (`0009`), l'invio (`0017`) e
la prova dell'accettazione (`0019`). È anche il punto in cui entrano nell'app i **primi dati personali**: da qui
in avanti l'applicazione tratta dati di persone e la storia `0007` ne prende atto formalmente.

## 2. Requisiti funzionali

1. **RF-1** — Si creano, modificano, cercano e cancellano (logicamente) i destinatari, con: ragione sociale o
   nome, natura (**impresa** o **consumatore**), persona di riferimento, posta elettronica, telefono, indirizzo,
   identificativo fiscale, lingua preferita fra le cinque, note.
2. **RF-2** — La **natura** del destinatario è obbligatoria, perché cambia il documento: a un consumatore vanno
   mostrate le informazioni precontrattuali e l'avviso sul diritto di recesso di quattordici giorni previsto per i
   contratti a distanza (§2.3 della descrizione dell'applicazione); a un'impresa no.
3. **RF-3** — La ricerca funziona su nome, posta elettronica e identificativo fiscale, con paginazione.
4. **RF-4** — Un destinatario usato da almeno un preventivo non si cancella fisicamente: si disattiva; l'elenco
   filtra i disattivati per impostazione predefinita.
5. **RF-5** — La lingua preferita del destinatario decide in quale lingua sono resi i testi standard del documento
   che riceverà (usata dalla storia `0014`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `destinatario` filtra per `tenant_id` preso
  dal token verificato; un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST|PUT|DELETE /api/preventivi/v1/destinatari`,
  corpo validato in modo dichiarativo sugli oggetti di trasferimento, errori in `problem+json`, definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V2__destinatari.sql` sullo schema `app_preventivi`: tabella
  `destinatario` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione **Catalogo e listini → Destinatari** del modulo `preventivi`; dati
  letti con il client generato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `preventivi` e sono presenti
  in `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** **È la storia che introduce dati personali nell'app.** Vanno annotati
  `@PersonalData` i campi `persona_riferimento`, `email`, `telefono`, `indirizzo`, `codice_fiscale` e, quando il
  destinatario è una persona fisica, `ragione_sociale`. Le voci corrispondenti entrano nel manifesto in italiano e
  in inglese e la tabella entra in `exportData` e `purgeData`: il lavoro formale lo completa la storia `0007`, ma
  **non si chiude questa senza quella**.
- **RT-7 — Registrazione eventi (§14).** `destinatario creato`, `destinatario disattivato` con `tenant_id`,
  `app_id`, `user_id` e correlazione — **mai il nome o l'indirizzo di posta**.

## 4. Criteri di accettazione

**CA-1 — Creazione e riuso**
- **Dato** un utente abilitato · **Quando** crea un destinatario «Fornitura Alfa» con persona di riferimento e
  posta elettronica · **Allora** lo ritrova cercando per una qualunque delle tre chiavi e può usarlo su un
  preventivo

**CA-2 — La natura cambia il documento**
- **Dato** un destinatario marcato **consumatore** · **Quando** si apre un preventivo intestato a lui · **Allora**
  l'interfaccia segnala che il documento deve riportare le informazioni precontrattuali e il diritto di recesso

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri destinatari · **Quando** un utente di `A` cerca per nome
- **Allora** trova solo i propri, anche forzando l'identificativo dell'altro account nella richiesta

**CA-4 — Cancellazione impedita se in uso**
- **Dato** un destinatario con due preventivi · **Quando** si tenta di cancellarlo · **Allora** l'app lo disattiva
  e lo dice, invece di rompere i documenti esistenti

**CA-5 — Validazione**
- **Dato** un indirizzo di posta malformato · **Quando** si salva · **Allora** `400` in `problem+json` con il
  campo in errore, e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sulla validazione e di **integrazione** sulla risorsa, con database effimero;
- [ ] prova di **isolamento fra account** sulla risorsa nuova;
- [ ] **prova end-to-end**: rimando alla storia `0029` (la creazione del destinatario è il primo passo del
      percorso) — registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con i campi annotati, e tabella presente in
      esportazione e cancellazione (formalizzato da `0007`);
- [ ] **registro delle decisioni** compilato (campi scelti, natura impresa/consumatore, disattivazione anziché
      cancellazione);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | schema e convenzioni |
| storia `0003` | la sezione dove vive l'elenco |
| storia `0007` | la chiude sul piano della conformità: si implementano insieme |

## 7. Fuori ambito

- l'importazione da file di una anagrafica esistente: rimandata, nessuno l'ha chiesta nella ricerca di mercato;
- la condivisione dell'anagrafica con il CRM (catalogo 04): non esiste ancora la suite.

## 8. Punti aperti

I testi delle informazioni precontrattuali e del diritto di recesso li scrive **il cliente**, non appgrove:
l'app li ospita nei modelli (`0014`). Se in futuro si volesse fornirne di predefiniti, sarebbe consulenza legale
e non una funzione: decisione dello sviluppatore con revisione legale.
