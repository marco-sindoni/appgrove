# 0010 — Deduplica e arrivi in ritardo

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 02 — Ingresso dei dati di consumo
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile tecnico che guarda il totale del mese
> voglio che ogni chiamata sia contata una volta sola, anche se il mio codice l'ha mandata due volte o se arriva
> con un giorno di ritardo
> così da poter usare quel numero senza doverlo verificare a mano ogni volta.

**Contesto.** Due strade portano gli stessi dati dentro l'app: il rendiconto del fornitore e l'invio dal prodotto
del cliente. Se entrambe alimentassero il totale, il cliente pagherebbe due volte sul cruscotto ciò che ha pagato
una volta al fornitore — ed è il difetto più veloce a distruggere la fiducia in questo prodotto, perché il primo
controllo che chiunque fa è confrontare il nostro numero con la propria fattura. La stessa cosa vale per gli invii
ripetuti: chi manda misure in modo asincrono e a perdere (storia `0009`) prima o poi ne rimanda una.

## 2. Requisiti funzionali

1. **RF-1** — Una misura con lo stesso identificativo esterno già presente per lo stesso account **non viene
   registrata due volte**: la seconda occorrenza è riconosciuta e contata come duplicato, non come errore.
2. **RF-2** — Quando la stessa chiamata arriva da **due origini diverse** (invio e rendiconto), vale la regola
   dichiarata: la misura di **invio** è quella che entra nel dettaglio attribuito, il rendiconto è la verità di
   fatturazione e resta separato; i due non si sommano mai.
3. **RF-3** — Una misura che arriva in ritardo viene collocata nel **giorno in cui è avvenuta la chiamata**, non nel
   giorno in cui è arrivata; i totali del giorno interessato si aggiornano e la variazione è visibile.
4. **RF-4** — Esiste una **finestra di accettazione del ritardo** dichiarata (proposta: 7 giorni): oltre quella,
   la misura viene comunque conservata ma segnalata come «arrivata fuori finestra», perché altera un periodo che il
   cliente potrebbe aver già chiuso.
5. **RF-5** — Una misura con istante nel **futuro** oltre una tolleranza breve viene respinta: è quasi sempre un
   orologio sbagliato, e accettarla sposterebbe la spesa in un periodo che non è ancora cominciato.
6. **RF-6** — Il conteggio dei duplicati riconosciuti è visibile nella salute della fonte: un cliente che manda il
   30% di duplicati ha un difetto nel proprio codice e deve saperlo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'unicità è per (`tenant_id`, `fonte`, `identificativo_esterno`): due
  account possono legittimamente usare lo stesso identificativo esterno senza interferire. Prova di isolamento
  esplicita su questo punto, perché è il caso in cui un errore di indice mescolerebbe i dati di due clienti.
- **RT-2 — Persistenza (§8).** Il vincolo di unicità esiste dalla storia `0002`; qui si aggiunge il conteggio dei
  duplicati per fonte e il contrassegno «fuori finestra» sulla misura.
- **RT-3 — Interfaccia di programmazione (§2).** Il riepilogo restituito dal ricevitore distingue tre esiti:
  accettate, **duplicate**, respinte. Un duplicato non è un errore e non deve far pensare al cliente di avere un
  problema quando non ce l'ha.
- **RT-4 — Varchi e quota (§6, §7).** Un duplicato riconosciuto **non consuma** quota: il cliente ha già pagato
  quella misura la prima volta. È una scelta che va scritta nel listino, perché altrimenti un difetto del suo
  codice gli costerebbe il piano.
- **RT-5 — Esposizione conversazionale (§12).** Nessuno strumento nuovo; il conteggio dei duplicati entra nel
  risultato di `stato_fonti`.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-7 — Registrazione eventi (§14).** Eventi «duplicato riconosciuto», «misura fuori finestra», «istante nel
  futuro respinto» con `tenant_id`, `app_id` e identificativo di correlazione, senza dati personali e senza il
  corpo della misura.

## 4. Criteri di accettazione

**CA-1 — Lo stesso invio due volte conta una volta**
- **Dato** una misura con identificativo esterno `chiamata-991` già registrata
- **Quando** la stessa misura viene inviata di nuovo
- **Allora** il riepilogo la conta come duplicata, il totale di spesa non cambia e la quota non viene consumata

**CA-2 — Invio e rendiconto non si sommano**
- **Dato** una giornata in cui il cliente ha inviato le proprie misure **e** il rendiconto del fornitore è stato
  importato
- **Quando** si legge il totale della giornata
- **Allora** il totale attribuito viene dalle misure di invio, l'importo del rendiconto è mostrato a parte come
  verità di fatturazione, e la loro somma non compare da nessuna parte

**CA-3 — Arrivo in ritardo nel giorno giusto**
- **Dato** una misura avvenuta due giorni fa che arriva adesso
- **Quando** viene registrata
- **Allora** compare nel totale di due giorni fa, non di oggi, e il grafico del periodo si aggiorna

**CA-4 — Fuori finestra**
- **Dato** una misura avvenuta trenta giorni fa, oltre la finestra dichiarata
- **Quando** arriva
- **Allora** viene conservata, contrassegnata come fuori finestra e segnalata nella salute della fonte

**CA-5 — Istante nel futuro**
- **Dato** una misura con istante di domani
- **Quando** arriva
- **Allora** è respinta con un messaggio che suggerisce di verificare l'orologio del sistema che invia

**CA-6 — Isolamento fra account**
- **Dato** due account che inviano entrambi una misura con identificativo esterno `chiamata-1`
- **Quando** entrambe arrivano
- **Allora** entrambe sono registrate, ciascuna nel proprio account, e nessuna è scambiata per duplicato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla finestra di ritardo e sulla precedenza fra origini, e di **integrazione** sulla
      deduplica con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul vincolo di unicità;
- [ ] **prova end-to-end**: **si rimanda** alla storia `0034`, che include nel percorso `[J-SPESA-MODELLI]` il
      doppio invio della stessa misura; il motivo del rimando è che la deduplica non ha superficie propria;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per i tre esiti del riepilogo;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sulla regola di precedenza fra invio e rendiconto e
      sul fatto che un duplicato non consuma quota;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | Serve l'importazione dei rendiconti, una delle due origini |
| Storia `0009` | Serve il ricevitore delle misure, l'altra origine |

## 7. Fuori ambito

- il confronto quantitativo fra le due origini e la misura dello scarto: è della storia `0011`;
- la correzione retroattiva dei costi quando cambia il listino: è della storia `0017`.

## 8. Punti aperti

- **Che cosa fare quando il cliente non manda un identificativo esterno.** Senza, la deduplica non è possibile.
  Proposta: accettare la misura ma segnalare che quella fonte non è protetta dai duplicati, e mostrarlo nella
  salute della fonte. L'alternativa — generare noi un identificativo dai campi della misura — sembra comoda ma
  scambierebbe per duplicate due chiamate identiche fatte davvero due volte. La chiude lo sviluppatore.
