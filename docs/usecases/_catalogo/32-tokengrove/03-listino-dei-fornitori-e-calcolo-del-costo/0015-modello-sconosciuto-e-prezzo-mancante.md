# 0015 — Modello sconosciuto e prezzo mancante

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 03 — Listino dei fornitori e calcolo del costo
**Storia**: `0015` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che guarda il totale del mese
> voglio sapere se una parte della mia spesa non è stata calcolata perché mancava il prezzo
> così da non prendere per completo un numero che è incompleto.

**Contesto.** I fornitori pubblicano modelli nuovi in continuazione, spesso con nomi che cambiano fra un giorno e
l'altro, e il nostro catalogo li conoscerà sempre con qualche giorno di ritardo. Il momento in cui una misura
riferisce un modello che non sta nel catalogo è quindi normale, non eccezionale, e va gestito con la regola
d'onestà che vale per tutta l'app: **un numero che non si sa non si inventa**. Le due scorciatoie facili sono
entrambe sbagliate: assegnare zero (che nasconde la spesa) o usare il prezzo di un modello simile (che produce un
numero verosimile e falso, che è la cosa peggiore).

## 2. Requisiti funzionali

1. **RF-1** — Una misura il cui modello non è nel catalogo viene **registrata** con il suo consumo, ma senza
   costo: la riga porta lo stato «prezzo non noto», non zero.
2. **RF-2** — Il totale di un periodo che contiene misure senza prezzo noto è mostrato insieme a un secondo numero:
   quante chiamate e quale volume di consumo non sono valorizzati. Il totale non pretende di essere completo.
3. **RF-3** — La schermata elenca le **chiavi di modello sconosciute** incontrate, ordinate per volume, con la
   possibilità di segnalarle a chi conduce il servizio in un clic.
4. **RF-4** — Quando il catalogo viene aggiornato con un prezzo che copre quelle chiamate, l'account viene avvisato
   che può valorizzare il periodo, con un rimando esplicito al ricalcolo (storia `0017`). Il costo **non** viene
   assegnato automaticamente, perché sarebbe una modifica silenziosa di dati passati.
5. **RF-5** — La stessa disciplina vale quando il catalogo conosce il modello ma non uno dei conteggi (per esempio
   un fornitore introduce un nuovo tipo di unità): si valorizza ciò che si sa e si dichiara ciò che manca, senza
   arrotondare il resto a zero.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le misure senza prezzo si contano per `tenant_id` preso dal gettone
  verificato. L'aggregazione delle chiavi sconosciute **su tutta la piattaforma** vive solo nella console di
  amministrazione ([estensioni-admin.md](../estensioni-admin.md) §4) e non mescola dati di account.
- **RT-2 — Persistenza (§8).** La colonna dello stato del costo della misura ammette il valore «prezzo non noto»;
  nessuna riga con costo `0` che significhi in realtà «non lo so».
- **RT-3 — Interfaccia di programmazione (§2).** Ogni risposta che contiene un totale porta accanto il conteggio e
  il volume non valorizzati: il totale non viaggia mai da solo. Rotta
  `GET /api/spesa_modelli/v1/modelli-sconosciuti`; errori in `problem+json`; definizione OpenAPI aggiornata.
- **RT-4 — Modulo frontend (§3, §5).** Il numero «non valorizzato» sta **accanto** al totale, con lo stesso peso
  visivo di una nota, non nascosto in un pannello. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** «Prezzo non noto» e «non valorizzato» sono presenti in `en, it, fr, es, de`.
- **RT-6 — Esposizione conversazionale (§12).** Lo strumento `leggi_spesa` (storia `0032`) restituisce **sempre**
  anche la quota non valorizzata: un assistente che riportasse il solo totale direbbe una cosa falsa. Il contratto
  lo dichiara come campo obbligatorio del risultato.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-8 — Registrazione eventi (§14).** Evento «chiave di modello sconosciuta» con `tenant_id`, `app_id`, chiave
  del modello e identificativo di correlazione. È l'evento che alimenta la vista di piattaforma.

## 4. Criteri di accettazione

**CA-1 — Nessun costo inventato**
- **Dato** una misura su un modello assente dal catalogo
- **Quando** viene registrata
- **Allora** il suo consumo è conservato, il costo è «non noto» e non `0`, e nessun prezzo di un modello simile è
  stato usato

**CA-2 — Il totale dichiara la propria incompletezza**
- **Dato** un mese con 10.000 chiamate di cui 400 su un modello sconosciuto
- **Quando** si legge il totale
- **Allora** accanto al totale compare «400 chiamate non valorizzate», e lo stesso vale nella risposta della rotta
  e nel risultato dello strumento conversazionale

**CA-3 — Elenco delle chiavi sconosciute**
- **Dato** un account che ha incontrato tre chiavi di modello sconosciute
- **Quando** apre la schermata dedicata
- **Allora** le vede ordinate per volume e può segnalarle in un clic

**CA-4 — Nessuna valorizzazione automatica**
- **Dato** un catalogo aggiornato che ora conosce il modello prima sconosciuto
- **Quando** la pubblicazione avviene
- **Allora** le misure passate restano non valorizzate e l'account riceve l'invito a ricalcolare, senza che nulla
  sia cambiato da solo

**CA-5 — Isolamento fra account**
- **Dato** due account che incontrano lo stesso modello sconosciuto
- **Quando** ciascuno apre l'elenco
- **Allora** vede i conteggi propri e non quelli dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla distinzione fra costo zero e costo non noto, e di **integrazione** sulla propagazione
      del conteggio non valorizzato in tutte le risposte che contengono un totale;
- [ ] prova di **isolamento fra account** sui conteggi delle chiavi sconosciute;
- [ ] **prova end-to-end**: **si rimanda** alla storia `0034`, che include il caso del modello sconosciuto nel
      percorso `[J-SPESA-MODELLI]`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sul divieto di usare il prezzo di un modello simile;
- [ ] contratto degli **strumenti conversazionali** aggiornato: la quota non valorizzata è campo obbligatorio del
      risultato di `leggi_spesa`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0014` | Serve il calcolo del costo, di cui questa storia tratta il caso in cui non si può fare |

## 7. Fuori ambito

- la valorizzazione retroattiva quando il prezzo diventa noto: è la storia `0017`;
- l'aggiornamento automatico del catalogo quando compare un modello sconosciuto: rimandato con il punto P6 del
  documento capofila.

## 8. Punti aperti

Nessuno.
