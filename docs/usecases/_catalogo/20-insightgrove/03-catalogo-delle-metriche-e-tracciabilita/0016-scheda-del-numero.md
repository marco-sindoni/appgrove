# 0016 — Scheda del numero

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 03 — Catalogo delle metriche e tracciabilità
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0011`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta per prendere una decisione su un numero
> voglio poter vedere da dove viene quel numero, quanto è completo e a quando è aggiornato
> così da poterlo usare, o da accorgermi che non posso.

**Contesto.** È la storia che tiene in piedi la promessa dell'applicazione. Il materiale del 2026 sugli
assistenti analitici dice due cose che qui diventano requisiti: gli errori sono **plausibili**, quindi non si
vedono; e chi ricalcola dal dato d'origine trova circa **tre volte più errori** di chi verifica chiedendo a un
secondo assistente (§2.5 della [descrizione](../application-description.md), fonti 4 e 7). La conseguenza è che
la verifica va **resa possibile**, non raccomandata: ogni numero porta la sua ricevuta, e la ricevuta è a un clic.

## 2. Requisiti funzionali

1. **RF-1** — Ogni valore mostrato dall'app — su un riquadro, in una risposta del copilota, dentro un rapporto o
   una esportazione — ha una **scheda del numero** raggiungibile in un gesto.
2. **RF-2** — La scheda contiene: metrica e **versione della definizione**; che cosa significa, in lingua
   naturale; periodo e calendario usato; fonti che hanno concorso, con il conteggio dei fatti per ciascuna;
   momento del fatto più recente per fonte; **grado di completezza**; il piano eseguito in forma leggibile;
   fino a dieci **rimandi** alla riga d'origine (storia 0011).
3. **RF-3** — Il **grado di completezza** ha tre valori: `completo`, `parziale` (una fonte è silente o in
   caricamento, oppure un sotto-periodo non ha dati), `non calcolabile` (una fonte richiesta non è collegata,
   oppure il calcolo non è definito). La scheda dice sempre **quale pezzo manca**.
4. **RF-4** — Un valore `parziale` porta un **contrassegno accanto alla cifra**, non in una nota a fondo pagina;
   il contrassegno non si affida al solo colore.
5. **RF-5** — La scheda dichiara che i numeri di InsightGrove **non hanno valore contabile** e possono non
   coincidere con i dati fiscali: è la frase che evita di perdere la fiducia del cliente davanti al
   commercialista (§2.3 della descrizione).
6. **RF-6** — La scheda si può **ricalcolare** su richiesta: se il numero cambia, la scheda mostra il valore
   precedente, il nuovo e che cosa è cambiato (fatti arrivati, definizione modificata).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La traccia si legge con `tenant_id` dal gettone verificato; una
  traccia di un altro account non è raggiungibile nemmeno conoscendone l'identificativo.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/insights/v1/tracce/{id}`; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `traccia_del_calcolo` con `tenant_id`, piano eseguito, metriche e
  versioni usate, fonti e conteggi, completezza, momento dell'esecuzione; chiave primaria UUID versione 7,
  colonne di controllo, cancellazione logica. Conservazione proposta: dodici mesi.
- **RT-4 — Modulo frontend (§3, §5).** La scheda è un pannello laterale richiamabile da qualunque valore; solo
  token del sistema di design; funziona in tema chiaro e scuro; il contrassegno di incompletezza soddisfa il
  controllo automatico di accessibilità.
- **RT-5 — Cinque lingue (§4).** Tutti i testi della scheda, compresi i nomi dei gradi di completezza e la frase
  sul valore non contabile, esistono in `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** La scheda rispetta la classe di riservatezza: chi non può vedere la metrica non
  può vederne la traccia. **Non consuma quota**.
- **RT-8 — Dati personali (§10).** La scheda mostra i rimandi (opachi) e, se la via (A) è stata scelta, le
  etichette di dimensione, che sono dati personali. Non mostra altro dell'origine.
- **RT-14 — Registrazione eventi (§14).** «Traccia consultata», «numero ricalcolato» con `tenant_id`, `app_id`,
  `user_id`, identificativo della traccia; senza dati personali.

## 4. Criteri di accettazione

**CA-1 — La ricevuta c'è**
- **Dato** un riquadro che mostra «fatturato emesso — luglio — 42.300 €»
- **Quando** l'utente apre la scheda del numero
- **Allora** legge: metrica `fatturato_emesso` versione 3, periodo 1-31 luglio con calendario dell'account,
  fonti fatturazione (118 fatti) e note di credito (4 fatti), ultimo dato di stamattina alle 06:15, completo,
  il piano eseguito, e fino a dieci rimandi

**CA-2 — Il parziale si vede prima del numero**
- **Dato** una fonte richiesta silente da sei giorni
- **Quando** si apre il cruscotto
- **Allora** il valore porta il contrassegno di incompletezza accanto alla cifra, e la scheda dice quale fonte
  tace e da quando

**CA-3 — Non calcolabile**
- **Dato** una metrica che richiede una fonte non collegata
- **Quando** si guarda il riquadro
- **Allora** non compare alcun numero: compare «non calcolabile — richiede la fonte magazzino» con il rimando
  alla sezione Fonti

**CA-4 — Il ricalcolo spiega la differenza**
- **Dato** un valore calcolato ieri, e nel frattempo sono arrivati 12 fatti con periodo di competenza in quel
  mese
- **Quando** l'utente chiede il ricalcolo
- **Allora** vede il valore precedente, quello nuovo e «12 fatti arrivati dopo il primo calcolo»

**CA-5 — Riservatezza rispettata**
- **Dato** un utente `member` e una traccia di una metrica economica
- **Quando** prova ad aprirla conoscendone l'identificativo
- **Allora** riceve `403`

**CA-6 — Isolamento fra account**
- **Dato** una traccia dell'account `B`
- **Quando** un utente di `A` prova ad aprirla con il suo identificativo
- **Allora** riceve `404`: la traccia non esiste, per lui

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del grado di completezza (tutti e tre i valori, e le loro cause) e di
      **integrazione** sulla risorsa della traccia;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla traccia;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include «apri la scheda del numero e
      segui un rimando»; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato: la traccia non introduce dati personali oltre a quelli già dichiarati;
- [ ] **registro delle decisioni** compilato, con i tre gradi di completezza, la conservazione di dodici mesi e
      la frase sul valore non contabile;
- [ ] contratto degli **strumenti conversazionali**: `spiega_numero` restituisce esattamente il contenuto di
      questa scheda (storia 0031);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | la completezza dipende dallo stato di salute delle fonti |
| storia `0011` | i rimandi alla riga d'origine sono parte della scheda |
| storia `0015` | la traccia è prodotta dal calcolo |

## 7. Fuori ambito

- l'uso della scheda dentro la risposta del copilota: storia 0023;
- l'esportazione con la scheda in testa: storia 0027;
- il confronto fra due tracce di due utenti diversi: non serve a nessuno oggi.

## 8. Punti aperti

- **Dodici mesi di conservazione delle tracce sono i giusti?** Servono a spiegare un numero già letto; oltre
  l'anno la definizione sarà probabilmente cambiata comunque. Chiude: **sviluppatore**.
- **La scheda va mostrata anche quando il numero è completo?** Mostrarla sempre educa alla verifica; mostrarla
  solo quando serve non appesantisce. Raccomandazione: **sempre raggiungibile, mai imposta** — un gesto per
  aprirla, nessun ingombro se non la si apre. Chiude: **sviluppatore**.
