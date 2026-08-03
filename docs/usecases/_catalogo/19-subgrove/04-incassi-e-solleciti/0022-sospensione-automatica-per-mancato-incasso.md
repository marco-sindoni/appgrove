# 0022 — Sospensione automatica per mancato incasso

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0022` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0021`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio che chi non paga da troppo tempo smetta di risultare in regola, senza che io debba deciderlo caso per caso
> così da non avere trenta persone che usano il servizio gratis perché nessuno ha avuto il coraggio di dirglielo.

**Contesto.** È la conseguenza della catena dei solleciti: quando il recupero è esaurito senza rientro, l'app
deve prendere atto. Il disegno segue la stessa idea di **tolleranza** che la piattaforma applica a sé
([docs/09-pagamenti.md](../../../../09-pagamenti.md) dec. 26): il pagamento fallito non toglie il servizio
subito — c'è una finestra in cui si mantiene tutto e si sollecita — e solo all'esito finale negativo l'accesso si
taglia. La finestra qui è la durata della catena, e l'evidenza del §2.5 della descrizione dice che oltre le due
settimane il recupero decade nettamente: allungarla non recupera denaro, tiene solo la contabilità in un limbo.

La cosa che questa storia deve fare bene è **non sorprendere nessuno**: si avvisa prima, si dice quando, e il
cliente può fermare tutto.

## 2. Requisiti funzionali

1. **RF-1** — Esaurita la catena dei solleciti senza rientro, l'abbonamento passa in automatico a `sospeso` con
   motivo «mancato incasso».
2. **RF-2** — L'ultimo sollecito della catena **annuncia** la sospensione: dice il giorno esatto in cui avverrà e
   come evitarla.
3. **RF-3** — La sospensione automatica si può **disattivare** per account: chi preferisce decidere a mano trova
   gli abbonamenti pronti in un elenco «da sospendere», con l'azione a un clic.
4. **RF-4** — Un abbonamento sospeso per mancato incasso torna `attivo` da solo quando la scadenza rientra,
   senza che nessuno debba ricordarsene.
5. **RF-5** — La sospensione automatica **non** cessa l'abbonamento: la cessazione resta un atto umano, perché
   chiude un rapporto e non si annulla.
6. **RF-6** — Ogni sospensione automatica è nella cronologia con causa, scadenza che l'ha provocata e giorno, e
   si distingue a colpo d'occhio da una sospensione concordata (storia `0015`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione elabora un account alla volta, filtrando per
  `tenant_id`; l'impostazione di disattivazione è per account.
- **RT-2 — Ciclo di vita (§ storia `0011`).** Il passaggio ad `sospeso` passa dalla macchina a stati, con motivo
  distinto da quello della sospensione concordata; il rientro passa da lì allo stesso modo.
- **RT-3 — Interfaccia di programmazione (§2).** Rotta `PUT /api/abbonati/v1/impostazioni/sospensione-automatica`
  e `GET /api/abbonati/v1/abbonamenti?stato=da-sospendere`; errori in `problem+json`; OpenAPI aggiornata.
- **RT-4 — Persistenza (§8).** Nessuna tabella nuova: si usano `transizione_abbonamento` (storia `0011`) e
  `sospensione` (storia `0015`), con la causa che le distingue. Aggiungere una tabella qui sarebbe il modo
  migliore per avere due verità sullo stesso fatto.
- **RT-5 — Modulo frontend (§3, §5).** Elenco «da sospendere» nella panoramica quando l'automatismo è spento;
  bollino distinto per la sospensione automatica; impostazione nelle preferenze dell'app; solo token del
  sistema di design.
- **RT-6 — Cinque lingue (§4).** Motivi, avvisi e testo dell'annuncio in `en, it, fr, es, de`.
- **RT-7 — Varchi e quota (§6).** L'abbonamento sospeso **continua** a consumare quota (storia `0004`): il
  cliente ne è avvisato nell'elenco, perché è controintuitivo.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: `sospendi_abbonamento` è già dichiarato
  dalla storia `0015`, con conferma obbligatoria. L'automatismo non passa dagli strumenti.
- **RT-9 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-10 — Registrazione eventi (§14).** `sospensione automatica applicata (scadenza che l'ha causata)`,
  `rientro automatico`, con `tenant_id`, `app_id` e correlazione, senza nomi.

## 4. Criteri di accettazione

**CA-1 — Sospensione annunciata e applicata**
- **Dato** una catena di solleciti al suo ultimo passo, con annuncio «sospensione il giorno 20»
- **Quando** arriva il giorno 20 senza che la scadenza sia rientrata
- **Allora** l'abbonamento passa a `sospeso` con motivo «mancato incasso» e la cronologia lo registra

**CA-2 — Rientro automatico**
- **Dato** lo stesso abbonamento sospeso · **Quando** la scadenza viene registrata incassata
- **Allora** l'abbonamento torna `attivo` da solo, senza intervento

**CA-3 — Automatismo disattivato**
- **Dato** un account che ha spento la sospensione automatica
- **Quando** una catena si esaurisce senza rientro
- **Allora** nulla viene sospeso, e l'abbonamento compare nell'elenco «da sospendere» con l'azione a un clic

**CA-4 — Nessuna cessazione automatica**
- **Dato** un abbonamento sospeso da mesi per mancato incasso · **Quando** passa altro tempo
- **Allora** resta sospeso: nessun automatismo lo cessa

**CA-5 — Distinzione dalle sospensioni concordate**
- **Dato** due abbonamenti sospesi, uno per accordo e uno per mancato incasso
- **Quando** si guarda l'elenco
- **Allora** i due casi si distinguono a colpo d'occhio e per filtro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sulla condizione di sospensione e sul rientro automatico; **integrazione** sulla
      lavorazione;
- [ ] prova di **isolamento fra account** sull'impostazione e sulla lavorazione;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-ABBONATI]` porta un abbonamento fino alla sospensione
      automatica e poi lo fa rientrare; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato: **riuso dichiarato** dell'idea di tolleranza della piattaforma,
      sospensione annunciata prima, nessuna cessazione automatica;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0021` | la sospensione è la conseguenza della catena esaurita |
| storia `0015` | riusa lo stesso stato e la stessa tabella, con causa diversa |

## 7. Fuori ambito

- la cessazione automatica: deliberatamente assente, perché chiude un rapporto contrattuale;
- il blocco dell'accesso fisico o del servizio erogato: SubGrove dice **se il rapporto è in regola**, non apre e
  non chiude tornelli. Chi vuole legare le due cose lo fa a evento, e non è di questa app;
- il recupero del credito: storia `0021` e, oltre, CashGrove.

## 8. Punti aperti

**Cosa significa «sospeso» per il servizio erogato.** Nell'app è uno stato di dati; nella realtà del cliente
dovrebbe voler dire «non entra in palestra». Il collegamento fra le due cose non è di SubGrove — passerebbe da un
evento verso un'app di accesso o prenotazione — ma il cliente si aspetterà che qualcosa succeda, e la delusione
va prevenuta a schermo. **Proposta**: una riga esplicita nell'interfaccia che dice che la sospensione è
un'indicazione gestionale e non blocca automaticamente l'accesso. Chiude: lo sviluppatore, con la direzione di
prodotto.
