# 0024 — Tempo medio di incasso

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 05 — Analisi e previsione
**Storia**: `0024` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio sapere quanti giorni passano in media fra la fattura e l'incasso, e se il numero sta migliorando
> così da capire se il tempo che dedico ai solleciti sta producendo un risultato o no.

**Contesto.** È l'indicatore che il settore chiama «giorni medi di incasso» (in inglese *days sales outstanding*, spesso
abbreviato in DSO: il numero medio di giorni che intercorrono fra l'emissione della fattura e il suo incasso). Vale due
volte: come misura interna e come **prova che l'app serve**. Il documento capofila (§5.1) propone di rinunciare alla
componente di prezzo legata al recuperato e di spostare l'argomento dal listino al prodotto: questa è la storia che lo
fa. Se dopo tre mesi il numero è sceso di dieci giorni, l'abbonamento si rinnova da solo.

## 2. Requisiti funzionali

1. **RF-1** — L'app calcola i giorni medi di incasso su un periodo scelto, come media dei giorni intercorsi fra data del
   documento e data dell'incasso, ponderata per importo.
2. **RF-2** — Accanto al valore compare l'andamento rispetto al periodo precedente, con il segno e la variazione.
3. **RF-3** — L'app mostra anche il **ritardo medio oltre la scadenza**, che è il numero che il titolare capisce meglio:
   «i miei clienti pagano in media 18 giorni dopo la scadenza».
4. **RF-4** — I due indicatori sono disponibili per l'intero account e per singolo debitore.
5. **RF-5** — Quando i dati non bastano a dire qualcosa di sensato (meno di un numero minimo di incassi nel periodo),
   l'app **dichiara** che il campione è insufficiente invece di mostrare un numero.
6. **RF-6** — Un riquadro mostra l'andamento su dodici mesi, così che il miglioramento sia visibile e non solo
   affermato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni calcolo filtra per `tenant_id` preso dal token verificato; non esistono
  medie di piattaforma né confronti con altri account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/crediti/v1/indicatori/tempo-incasso` (con periodo) e
  `GET /api/crediti/v1/indicatori/tempo-incasso/andamento`; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si calcola da `credito` e `incasso`. Se il calcolo dell'andamento
  su dodici mesi risultasse costoso, si valuta una vista materializzata aggiornata dalla lavorazione quotidiana.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro degli indicatori nella *Panoramica* con l'andamento su dodici mesi; solo
  token del sistema di design; funziona in tema chiaro e scuro. Il grafico deve restare leggibile in bianco e nero e
  avere una alternativa testuale: un andamento che si capisce solo dal colore non è un indicatore.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`. **L'indicatore non si chiama con la sigla inglese** nell'interfaccia: si scrive «giorni medi di
  incasso», con la sigla eventualmente fra parentesi alla prima occorrenza.
- **RT-6 — Varchi e quota (§6, §7).** Non consuma quota; accessibile anche in sola lettura.
- **RT-7 — Esposizione conversazionale (§12).** `indicatore_tempo_medio_incasso(periodo) → giorni medi e andamento` è
  dichiarato qui come strumento di **lettura**, raccolto nel contratto della storia `0028`.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo; l'indicatore per debitore è una elaborazione su dati
  già dichiarati.
- **RT-9 — Registrazione eventi (§14).** Nessun evento applicativo nuovo oltre alle richieste ordinarie.

## 4. Criteri di accettazione

**CA-1 — Calcolo del valore**
- **Dato** tre fatture incassate rispettivamente a 30, 60 e 90 giorni dall'emissione, di pari importo
- **Quando** si chiede l'indicatore sul periodo che le contiene
- **Allora** il valore è 60 giorni

**CA-2 — Ponderazione per importo**
- **Dato** una fattura da 10.000 € incassata a 90 giorni e una da 100 € incassata a 10 giorni
- **Quando** si chiede l'indicatore
- **Allora** il valore è vicino a 90, non a 50: la media è ponderata per importo e il criterio è dichiarato
  nell'interfaccia

**CA-3 — Campione insufficiente**
- **Dato** un account con due soli incassi nel periodo e una soglia minima di cinque · **Quando** si apre la
  *Panoramica* · **Allora** al posto del numero compare «dati insufficienti per il periodo scelto», con la spiegazione

**CA-4 — Ritardo medio oltre la scadenza**
- **Dato** fatture con termine a 30 giorni incassate a 45 · **Quando** si guarda l'indicatore · **Allora** i giorni medi
  di incasso sono 45 e il ritardo medio oltre la scadenza è 15

**CA-5 — Isolamento fra account**
- **Dato** due account con storici diversi · **Quando** ciascuno chiede l'indicatore · **Allora** i valori riguardano
  solo il proprio account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sul calcolo (media ponderata, incassi parziali, campione insufficiente) e di **integrazione**
      sull'andamento;
- [ ] prova di **isolamento fra account** sugli indicatori;
- [ ] **prova end-to-end**: *nessun impatto* — l'indicatore è una lettura derivata e le prove di integrazione bastano;
      il percorso `[J-CREDITI]` non ne dipende;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, senza sigle non spiegate;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, in particolare sulla ponderazione per importo e sulla soglia del campione;
- [ ] contratto degli **strumenti conversazionali**: `indicatore_tempo_medio_incasso` dichiarato come lettura;
- [ ] **accessibilità**: grafico con alternativa testuale, verificata dal controllo automatico;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0023` | Condivide l'impianto di aggregazione e la collocazione nella *Panoramica* |

## 7. Fuori ambito

- Il confronto con medie di settore: richiederebbe dati esterni che nessuna fonte consultata rende disponibili in forma
  utilizzabile, e un confronto sbagliato è peggio di nessun confronto.
- La misura del «recuperato grazie a CashGrove»: è la base della componente di prezzo respinta nel documento capofila
  §5.1 e richiede la riconciliazione bancaria. Fuori ambito, e volutamente.

## 8. Punti aperti

La **soglia minima di campione** (proposta: cinque incassi nel periodo) è arbitraria. La conferma lo sviluppatore: è
una scelta di onestà statistica, non di prodotto.
