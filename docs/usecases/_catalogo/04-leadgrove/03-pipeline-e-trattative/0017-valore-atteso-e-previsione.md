# 0017 — Valore atteso e previsione

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 03 — Pipeline e trattative
**Storia**: `0017` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve decidere se assumere
> voglio sapere quanto vale il lavoro in corso pesato per la probabilità di chiuderlo
> così da avere un numero difendibile invece della somma di tutti i sogni.

**Contesto.** La somma dei valori delle trattative aperte è un numero inutile: dice quanto varrebbe vincere tutto.
Il valore **atteso** — ogni trattativa pesata per la probabilità della fase in cui si trova (storia 0012) — è il
numero che serve. È deliberatamente una storia piccola: fa un calcolo e lo mostra, non costruisce un motore di
previsione.

## 2. Requisiti funzionali

1. **RF-1** — Ogni trattativa aperta espone un valore atteso = valore × probabilità della fase corrente.
2. **RF-2** — La panoramica mostra tre numeri: valore totale in corso, valore atteso e numero di trattative
   aperte, con il periodo di riferimento della data attesa di chiusura (questo mese, questo trimestre, tutto).
3. **RF-3** — Le trattative **senza valore** sono escluse dal calcolo e contate a parte, con un invito a
   completarle: contarle come zero falserebbe la previsione al ribasso.
4. **RF-4** — Le trattative senza data attesa di chiusura restano fuori dai periodi e compaiono in un gruppo
   «senza data».
5. **RF-5** — Il calcolo è visibile e spiegato: passando sul numero si legge come è stato ottenuto, perché un
   numero di cui non si capisce la provenienza non viene usato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Gli aggregati si calcolano solo sulle trattative dell'account del token
  verificato: un difetto qui sommerebbe i soldi di un altro cliente.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/sales/v1/deals/forecast` con parametri di periodo
  e pipeline; errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: il calcolo è una interrogazione aggregata. Nessun valore
  calcolato viene conservato, perché cambierebbe silenziosamente quando cambiano le probabilità delle fasi.
- **RT-4 — Modulo frontend (§3, §5).** Blocco di indicatori nella panoramica e in testa alla lavagna; solo token
  del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, periodi e spiegazione del calcolo in `en, it, fr, es, de`; importi
  formattati secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota; valgono i varchi ordinari.
- **RT-7 — Esposizione conversazionale (§12).** Il valore atteso fa parte di quanto restituisce `get_pipeline`
  (storia 0034), in sola lettura.
- **RT-8 — Dati personali (§10).** Nessun dato personale: sono aggregati. Nessuna voce nuova nel manifesto.
- **RT-9 — Registrazione eventi (§14).** Nessun evento applicativo nuovo; si registra solo la durata
  dell'interrogazione se supera una soglia.

## 4. Criteri di accettazione

**CA-1 — Calcolo**
- **Dato** due trattative aperte da 1.000 € in una fase al 20 % e da 2.000 € in una fase al 50 %
- **Quando** si apre la panoramica
- **Allora** il valore in corso è 3.000 € e il valore atteso 1.200 €

**CA-2 — Trattative senza valore**
- **Dato** tre trattative aperte, di cui una senza valore
- **Quando** si apre la panoramica
- **Allora** il valore atteso considera le due con valore e l'interfaccia segnala «1 trattativa senza valore»

**CA-3 — Cambio di probabilità**
- **Dato** una fase con probabilità al 20 %
- **Quando** l'amministratore la porta al 40 %
- **Allora** il valore atteso si aggiorna alla lettura successiva, senza bisogno di ricalcoli manuali

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B` con trattative aperte
- **Quando** un utente di `A` chiede la previsione
- **Allora** i numeri comprendono solo le trattative di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo, compresi i casi limite (nessuna trattativa, tutte senza valore) e di
      **integrazione** sull'aggregazione;
- [ ] prova di **isolamento fra account** sull'aggregazione;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo; coperta da prove d'integrazione, con il motivo
      annotato nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di non conservare i valori calcolati;
- [ ] contratto degli **strumenti conversazionali**: valore atteso incluso in `get_pipeline`;
- [ ] controllo automatico di **accessibilità** verde sul blocco di indicatori;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0012` | Le probabilità stanno sulle fasi |
| Storia `0013` | Servono valore e data attesa sulle trattative |

## 7. Fuori ambito

- previsioni basate sullo storico invece che sulle probabilità dichiarate: fuori perimetro, sarebbe un modello e
  non un calcolo;
- gli obiettivi di vendita e il confronto con essi: non previsti in questa proposta;
- la conversione fra valute: punto aperto della storia 0013.

## 8. Punti aperti

- Nessuno.
