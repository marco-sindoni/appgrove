# 0015 — Versioni dell'offerta

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 03 — Redazione dell'offerta
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che tratta con un cliente indeciso
> voglio poter emettere una revisione dell'offerta tenendo traccia di quella precedente
> così da sapere sempre quale versione il cliente ha in mano e su quale si è pronunciato.

**Contesto.** «Versioning delle offerte» è nei casi d'uso principali della scheda di catalogo, e non è un lusso:
è la premessa perché l'accettazione della storia `0019` sia difendibile. L'art. 20 comma 1-bis del Codice
dell'amministrazione digitale lega il valore probatorio alla **immodificabilità** della soluzione (§2.3, punto 2
della descrizione dell'applicazione): senza una versione congelata con la sua impronta, «il cliente ha accettato»
è una frase senza oggetto.

## 2. Requisiti funzionali

1. **RF-1** — Ogni invio congela una **versione**: contenuto completo del documento (righe, totali, testi),
   numero di versione progressivo, autore, momento e **impronta crittografica** del contenuto congelato.
2. **RF-2** — Su un preventivo già inviato la modifica non è consentita: si apre una **revisione**, che porta il
   documento in `in_revisione` e poi in `bozza` fino al nuovo invio.
3. **RF-3** — L'elenco delle versioni è visibile, con data, autore e motivo della revisione (facoltativo ma
   suggerito).
4. **RF-4** — Si confrontano due versioni vedendo cosa è cambiato nelle righe e nei totali.
5. **RF-5** — Una versione congelata **non si modifica e non si cancella** finché il preventivo esiste: la
   cancellazione avviene solo con quella del documento o per esercizio dei diritti dell'interessato.
6. **RF-6** — L'accettazione (storia `0019`) è sempre riferita a **una versione precisa**, mai «al preventivo».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** `versione_preventivo` filtra per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/preventivi/v1/preventivi/{id}/versioni`,
  `POST .../revisione`; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V9__versioni.sql`: tabella `versione_preventivo` con `tenant_id`, UUID
  versione 7, colonne di controllo, contenuto congelato e impronta; unicità su `(tenant_id, preventivo, numero)`.
- **RT-4 — Modulo frontend (§3, §5).** Cronologia delle versioni nella schermata del preventivo, con confronto;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili in `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** La versione congelata **contiene** i dati del destinatario e i testi liberi:
  la tabella entra in `exportData` e `purgeData`, e va detto esplicitamente nel manifesto perché è la copia meno
  ovvia da ricordare.
- **RT-7 — Registrazione eventi (§14).** `versione congelata`, `revisione aperta` con `tenant_id`, `app_id`,
  `user_id`, correlazione e **impronta**, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — L'invio congela**
- **Dato** un preventivo in bozza · **Quando** viene inviato · **Allora** nasce la versione 1 con la sua impronta,
  e da quel momento il documento non è modificabile

**CA-2 — Revisione**
- **Dato** un preventivo inviato · **Quando** si apre una revisione e si cambia una riga · **Allora** il documento
  torna modificabile, la versione 1 resta intatta, e il nuovo invio crea la versione 2

**CA-3 — Confronto**
- **Dato** due versioni con un prezzo diverso · **Quando** le si confronta · **Allora** l'app indica la riga
  cambiata e la differenza di totale

**CA-4 — Immodificabilità**
- **Dato** una versione congelata · **Quando** si tenta per qualunque via di alterarne il contenuto · **Allora**
  l'operazione è respinta e l'impronta resta verificabile

**CA-5 — Isolamento fra account**
- **Dato** una versione dell'account `A` · **Quando** un utente di `B` ne chiede il contenuto · **Allora** riceve
  la risposta che riceverebbe per un documento inesistente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sull'impronta e sul confronto, di **integrazione** sulle risorse;
- [ ] prova di **isolamento fra account** sulle versioni;
- [ ] **prova end-to-end**: rimando alla storia `0030`, dove l'accettazione riguarda una versione precisa;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: la versione congelata contiene dati personali ed entra in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato (cosa entra nel contenuto congelato, algoritmo dell'impronta,
      immodificabilità);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0013` | si congelano i totali calcolati |

## 7. Fuori ambito

- la conservazione a norma di legge dell'archivio: è di SignGrove (catalogo 15);
- la marcatura temporale certificata: non prevista, vedi punti aperti.

## 8. Punti aperti

**La marcatura temporale.** L'impronta dimostra che il contenuto non è cambiato, ma il **momento** è quello
registrato dal nostro orologio. Una marcatura temporale certificata renderebbe la prova più forte e costerebbe un
fornitore esterno per documento. Non lo propongo per un'app da 15 €/mese: è una decisione di prodotto dello
sviluppatore, ed è anche il confine naturale con SignGrove (catalogo 15).
