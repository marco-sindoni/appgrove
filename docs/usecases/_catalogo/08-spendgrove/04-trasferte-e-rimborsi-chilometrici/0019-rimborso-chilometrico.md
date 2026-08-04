# 0019 — Rimborso chilometrico

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 04 — Trasferte e rimborsi chilometrici
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come commerciale che gira con la propria automobile
> voglio dichiarare il tragitto e i chilometri e vedere subito quanto mi spetta, con la tariffa che l'azienda ha
> stabilito
> così da non dover fare i conti a mano su un foglio e da non discutere ogni mese su quale tariffa si applica.

**Contesto.** Il rimborso chilometrico è una spesa **senza ricevuta**: non c'è nulla da fotografare, c'è un calcolo
da fare. In Italia la tariffa esente si determina sulle tabelle pubblicate ogni anno in Gazzetta Ufficiale
dall'Automobile Club d'Italia, e il rimborso è esente da imposta sul reddito solo per trasferte **fuori dal Comune**
sede di lavoro (descrizione, §2.3, fonte 5). Qui c'è una decisione tecnica importante: **l'app non incorpora quelle
tabelle**. Sono un'opera altrui, cambiano ogni anno, valgono per una sola giurisdizione e distribuirle dentro il
prodotto ci renderebbe responsabili della loro correttezza. L'account carica le proprie tariffe: l'app calcola.

## 2. Requisiti funzionali

1. **RF-1** — L'account definisce un **listino di tariffe chilometriche**: nome del profilo di veicolo (per esempio
   «automobile a benzina fino a 1.400 cm³»), tariffa per chilometro, periodo di validità, fonte dichiarata.
2. **RF-2** — Si registra una percorrenza indicando data, partenza e arrivo dichiarati, chilometri, profilo di
   veicolo e, se pertinente, la trasferta a cui appartiene.
3. **RF-3** — L'importo si calcola come chilometri × tariffa vigente alla data della percorrenza; il calcolo è
   **mostrato** (i tre numeri e il risultato), non solo il totale: chi legge deve poterlo rifare.
4. **RF-4** — Una percorrenza diventa una `Spesa` di categoria «rimborso chilometrico» che segue il ciclo normale —
   revisione, nota, approvazione — senza giustificativo fotografico e senza che questo sia segnalato come anomalia.
5. **RF-5** — Se alla data della percorrenza non esiste una tariffa valida per quel profilo, l'app lo dice e non
   inventa un valore.
6. **RF-6** — Le percorrenze fuori Comune e quelle dentro il Comune sono distinte e riportate separatamente, perché
   il loro trattamento fiscale è diverso.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Tariffe e percorrenze filtrano per `tenant_id` preso dal token
  verificato; le tariffe sono **per account**, mai condivise fra clienti.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/notespese/v1/tariffe-chilometriche` e
  `GET|POST /api/notespese/v1/percorrenze`; errori in `application/problem+json` con `422` quando manca la tariffa
  vigente; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V16__percorrenze_e_tariffe.sql`: tabelle `tariffa_chilometrica` e
  `percorrenza_veicolo` con `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione logica. Sulla
  percorrenza si conserva **la tariffa applicata**, non solo il riferimento: se domani la tariffa cambia, il
  calcolo di ieri deve restare ricostruibile.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Trasferte → Percorrenze* e, in *Impostazioni*, il listino delle
  tariffe con l'importazione da file. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, spiegazione del calcolo e messaggi passano dallo spazio-nomi `notespese`
  e sono presenti in `en, it, fr, es, de`; unità di misura e formati numerici seguono la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Alla conferma, la spesa nata da una percorrenza consuma una unità della
  metrica `receipts` come tutte le altre: la metrica conta i documenti di spesa lavorati, e questo lo è anche senza
  foto. Va detto nell'interfaccia, altrimenti sorprende.
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara
  `crea_percorrenza(data, partenza, arrivo, km, profilo) → bozza di spesa`, marcato **scrittura**: produce una bozza
  in `da_rivedere` e richiede conferma umana. Dipendenza: UC 0061-0063.
- **RT-8 — Dati personali (§10).** 🛑 Dato di spostamento di un lavoratore, **dichiarato dall'interessato**: voce
  nuova nel manifesto in italiano e inglese, tabella `percorrenza_veicolo` in `exportData` e `purgeData`.
  **Nessun rilevamento**: niente posizione satellitare, niente lettura del contachilometri, niente ricostruzione
  automatica del percorso. Partenza e arrivo sono testo scritto dalla persona. È la scelta che tiene l'app dalla
  parte dello strumento di lavoro e non del controllo a distanza (descrizione, §6).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `percorrenza registrata`, `tariffa mancante` portano
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e chilometri — **mai** partenza e arrivo.

## 4. Criteri di accettazione

**CA-1 — Calcolo trasparente**
- **Dato** una tariffa di 0,52 €/km valida per il 2026 sul profilo «automobile a benzina media»
- **Quando** il collaboratore registra 84 km il 14 luglio 2026 con quel profilo
- **Allora** l'importo è 43,68 € e a schermo si leggono i tre fattori del calcolo

**CA-2 — Tariffa mancante**
- **Dato** nessuna tariffa valida per il profilo scelto alla data indicata
- **Quando** si tenta di registrare la percorrenza
- **Allora** l'app risponde `422`, dice quale tariffa manca e **non** applica un valore predefinito

**CA-3 — Il passato resta ricostruibile**
- **Dato** una percorrenza calcolata con 0,52 €/km · **Quando** l'account carica il listino dell'anno successivo
- **Allora** la percorrenza già registrata continua a mostrare 0,52 €/km e lo stesso importo

**CA-4 — Nessun giustificativo richiesto**
- **Dato** una spesa nata da una percorrenza
- **Quando** entra in una nota spese
- **Allora** **non** compare fra le spese «senza giustificativo»: il calcolo è il suo giustificativo

**CA-5 — Isolamento fra account**
- **Dato** due account con tariffe diverse per lo stesso profilo
- **Quando** ciascuno registra 100 km
- **Allora** ognuno ottiene l'importo della **propria** tariffa

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo e sulla scelta della tariffa vigente alla data; di **integrazione** sulle due
      risorse con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su tariffe e percorrenze;
- [ ] **prova end-to-end**: *coprire ora* il passo «registro una percorrenza e finisce in nota» nel percorso
      `[J-NOTESPESE]`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la dichiarazione che i dati di percorso sono
      dichiarati e non rilevati;
- [ ] **registro delle decisioni** compilato, con la scelta di **non** incorporare tabelle tariffarie di terzi;
- [ ] contratto dello strumento `crea_percorrenza` dichiarato, marcato scrittura con conferma;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0018` | La percorrenza si lega quasi sempre a una trasferta, e da lì eredita il contesto «fuori Comune» |

## 7. Fuori ambito

- **Distribuire le tabelle tariffarie ufficiali dentro il prodotto**: sono un'opera di terzi, cambiano ogni anno e
  valgono per una giurisdizione sola. L'app importa un listino, non lo possiede.
- Il calcolo automatico dei chilometri da un servizio di mappe: introdurrebbe un fornitore esterno che riceve
  indirizzi di partenza e arrivo di lavoratori — cioè dati di spostamento a un terzo. Deliberatamente escluso: i
  chilometri li dichiara la persona.
- Il valore d'uso del veicolo aziendale concesso al dipendente: è materia di busta paga, non di note spese.

## 8. Punti aperti

- **Verifica dei chilometri dichiarati**: oggi nessuna. Un'azienda che volesse controllarli avrebbe bisogno di un
  riscontro esterno, che è esattamente ciò che questa storia rifiuta di introdurre. Se il tema tornerà, va
  affrontato come questione di controllo dei lavoratori, non come funzione (punto aperto n. 6 della descrizione).
- **Tariffe per giurisdizioni diverse dall'Italia**: il modello le regge (sono solo listini), le regole di esenzione
  no (punto aperto n. 3 della descrizione).
