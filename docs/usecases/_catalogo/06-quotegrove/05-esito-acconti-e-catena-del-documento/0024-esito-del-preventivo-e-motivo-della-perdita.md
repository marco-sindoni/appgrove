# 0024 — Esito del preventivo e motivo della perdita

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 05 — Esito, acconti e catena del documento
**Storia**: `0024` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0020`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che perde metà delle offerte e non sa perché
> voglio registrare in due clic com'è andata e per quale motivo
> così da scoprire, dopo cinquanta preventivi, se perdo per il prezzo, per i tempi o perché rispondo tardi.

**Contesto.** Il destinatario può dichiarare un motivo quando rifiuta (storia `0020`), ma il più delle volte non
risponde affatto: il preventivo muore in silenzio, oppure si chiude per telefono e nessuno lo scrive. Questa
storia dà a **chi vende** il modo di chiudere il documento con il proprio giudizio — che è un dato diverso e più
onesto — ed è la materia prima degli indicatori della storia `0026`.

## 2. Requisiti funzionali

1. **RF-1** — Su ogni preventivo chiuso si registra un esito: **vinto**, **perso**, **annullato**, **senza
   risposta**.
2. **RF-2** — Per l'esito «perso» si sceglie un motivo da un elenco breve e configurabile dall'account (proposta:
   prezzo, tempi, concorrente, lavoro non più necessario, altro) più una nota libera.
3. **RF-3** — L'accettazione online imposta l'esito «vinto» da sola; il rifiuto propone «perso» con il motivo
   dichiarato dal cliente, che chi vende può correggere.
4. **RF-4** — Un preventivo senza risposta oltre la scadenza è proposto per la chiusura come «senza risposta»:
   l'app lo suggerisce, non lo decide.
5. **RF-5** — L'esito si può correggere finché il documento non è archiviato: chi ha chiuso e quando resta scritto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** `esito_preventivo` e l'elenco dei motivi filtrano per `tenant_id` preso
  dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/preventivi/v1/preventivi/{id}/esito` e
  `/api/preventivi/v1/impostazioni/motivi-perdita`; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V15__esito.sql`: tabella `esito_preventivo` con `tenant_id`, UUID
  versione 7, colonne di controllo, cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Azione di chiusura dall'elenco e dal dettaglio, in due clic; solo token del
  sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'interfaccia in `en, it, fr, es, de`; i motivi configurati dall'account sono
  contenuto del cliente.
- **RT-6 — Dati personali (§10).** Nessun campo di persona nuovo; **la nota libera può nominare persone** («il
  nuovo responsabile acquisti preferisce un altro fornitore»): la tabella entra in `exportData` e `purgeData` e va
  detto nel manifesto.
- **RT-7 — Registrazione eventi (§14).** `esito registrato` con `tenant_id`, `app_id`, `user_id`, correlazione e
  il **codice** dell'esito, senza la nota.

## 4. Criteri di accettazione

**CA-1 — Chiusura in due clic**
- **Dato** un preventivo inviato e mai risposto · **Quando** chi vende lo chiude come «perso — prezzo»
- **Allora** l'esito è registrato con autore e momento, e il documento non compare più fra quelli in attesa

**CA-2 — Esito automatico all'accettazione**
- **Dato** un preventivo accettato online · **Quando** si guarda l'esito · **Allora** è «vinto», senza che nessuno
  l'abbia scritto

**CA-3 — Motivo del cliente e motivo di chi vende**
- **Dato** un rifiuto con motivo «prezzo troppo alto» dichiarato dal cliente · **Quando** chi vende chiude
  indicando «concorrente» · **Allora** entrambi restano leggibili e distinti

**CA-4 — Suggerimento, non decisione**
- **Dato** un preventivo scaduto da un mese · **Quando** si apre l'elenco · **Allora** l'app propone di chiuderlo
  come «senza risposta» ma **non** lo chiude da sola

**CA-5 — Isolamento fra account**
- **Dato** due account con elenchi di motivi diversi · **Quando** ciascuno chiude un preventivo · **Allora** vede
  solo i propri motivi

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sulla derivazione automatica dell'esito e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: rimando alla storia `0030` per l'esito automatico all'accettazione; la chiusura manuale
      è coperta da integrazione — motivo scritto nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la nota libera dell'esito;
- [ ] **registro delle decisioni** compilato (elenco predefinito dei motivi, suggerimento anziché chiusura
      automatica);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0020` | il motivo dichiarato dal cliente è il punto di partenza |

## 7. Fuori ambito

- l'analisi dei motivi nel tempo: storia `0026`;
- il collegamento con l'opportunità del CRM (catalogo 04): la suite non esiste.

## 8. Punti aperti

Nessuno.
