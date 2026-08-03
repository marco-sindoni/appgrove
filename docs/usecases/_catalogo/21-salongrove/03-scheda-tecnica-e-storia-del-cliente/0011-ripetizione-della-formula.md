# 0011 — Ripetizione della formula

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 03 — Scheda tecnica e storia del cliente
**Storia**: `0011` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatrice che oggi serve una cliente della collega
> voglio prendere la formula dell'ultima volta e riportarla sulla scheda di oggi con un tocco
> così da fare lo stesso colore anche se non l'ho mai fatto io, senza telefonare a nessuno.

**Contesto.** La scheda tecnica serve a poco se per riusarla bisogna ricopiarla a mano da un elenco. Il valore
sta tutto in **«come l'ultima volta»**: è la frase che il cliente dice e che oggi costa un minuto di ricerca e un
margine d'errore di trascrizione. È anche la funzione che rende il salone meno dipendente dalla singola persona,
che è la ragione per cui il titolare compra.

## 2. Requisiti funzionali

1. **RF-1** — Aprendo una scheda tecnica nuova per un cliente che ne ha già, il programma propone di **riportare
   l'ultima formula** dello stesso servizio, mostrandola prima di applicarla.
2. **RF-2** — Si può scegliere **quale** scheda passata riportare, non solo l'ultima: le formule del salone non
   procedono sempre in avanti.
3. **RF-3** — Riportare una formula la **copia**, non la collega: la scheda nuova è indipendente, e correggere
   quella di oggi non tocca quella di sei settimane fa.
4. **RF-4** — La scheda nuova segnala in modo visibile **che cosa è cambiato** rispetto a quella riportata, quando
   l'operatore la modifica: è il modo in cui l'evoluzione del colore resta leggibile.
5. **RF-5** — Il confronto fra due schede dello stesso cliente si legge affiancato, campo per campo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Sia la proposta sia il confronto filtrano per `tenant_id` dal token
  verificato: la formula di un cliente di un altro salone non è raggiungibile in nessun modo.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/<app>/v1/schede-tecniche` accetta un riferimento a una
  scheda da cui copiare; `GET /api/<app>/v1/schede-tecniche/{id}/confronto?con={id}`; errori in `problem+json`;
  OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** La scheda copiata conserva il riferimento a quella d'origine come **dato
  informativo** (per dire «riportata da»), senza che questo crei una dipendenza: cancellare l'originale non rompe
  la copia.
- **RT-4 — Modulo frontend (§3, §5).** La proposta compare in cima al modulo della scheda nuova, con la formula
  leggibile e due bottoni chiari: «riporta» e «parti da vuoto». Il confronto è affiancato e evidenzia solo i campi
  diversi. Solo token del sistema di design, tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette della proposta, del confronto e dei messaggi di differenza in
  `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** Nessuna voce nuova nel manifesto: la storia usa i campi introdotti dalla storia
  `0010`. Il riferimento «riportata da» è un dato tecnico e va comunque compreso nell'esportazione.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento **di scrittura** nuovo. Lo strumento di lettura
  `scheda_tecnica_cliente` della storia `0010` risponde già alla domanda «che formula avevamo usato»; copiare una
  formula resta un atto che si fa nella scheda, con l'operatore davanti al risultato.
- **RT-8 — Registrazione eventi (§14).** `formula riportata` con `tenant_id`, `app_id`, `user_id` e correlazione,
  senza il contenuto.

## 4. Criteri di accettazione

**CA-1 — Come l'ultima volta**
- **Dato** una cliente con tre schede di colore, l'ultima di sei settimane fa
- **Quando** si apre una scheda nuova per lo stesso servizio
- **Allora** il programma propone la formula dell'ultima, mostrandola, e con un tocco la riporta nei campi

**CA-2 — Non è sempre l'ultima**
- **Dato** la stessa cliente
- **Quando** si sceglie di riportare la scheda di sei mesi fa invece dell'ultima
- **Allora** i campi si riempiono con quella, e la scheda nuova dice da quale è stata riportata

**CA-3 — La copia è indipendente**
- **Dato** una scheda riportata da un'altra
- **Quando** si modifica la scheda nuova
- **Allora** quella d'origine resta identica

**CA-4 — Le differenze si vedono**
- **Dato** una scheda riportata e poi modificata nel volume dell'ossidante
- **Quando** si apre il confronto con quella d'origine
- **Allora** il campo cambiato è evidenziato e gli altri no

**CA-5 — Isolamento fra account**
- **Dato** due account con schede tecniche
- **Quando** un utente del primo tenta di riportare una scheda dell'altro passandone l'identificativo
- **Allora** la richiesta è rifiutata e nessun contenuto trapela nel messaggio d'errore

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sul confronto campo per campo, di **integrazione** sulla copia;
- [ ] prova di **isolamento fra account** su proposta, copia e confronto;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-SALONGROVE]` della storia `0030`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; il riferimento «riportata da» è compreso nell'esportazione;
- [ ] **registro delle decisioni**: copia e non collegamento, scelta della scheda d'origine, evidenza delle
      differenze;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | non si riporta una formula che non esiste |

## 7. Fuori ambito

- suggerire una formula **diversa** da quella dell'ultima volta (per esempio adattandola alla ricrescita): è una
  proposta tecnica sul lavoro di un professionista e non ho nessuna base per farla. Fuori ambito, e il motivo è
  che sarebbe un consiglio, non un dato;
- le fotografie del prima e dopo: storia `0013`.

## 8. Punti aperti

Nessuno.
