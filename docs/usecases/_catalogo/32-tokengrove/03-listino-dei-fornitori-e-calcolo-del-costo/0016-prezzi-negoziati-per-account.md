# 0016 — Prezzi negoziati per account

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 03 — Listino dei fornitori e calcolo del costo
**Storia**: `0016` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile tecnico che ha strappato uno sconto del 20% al proprio fornitore
> voglio che TokenGrove usi il **mio** prezzo e non quello pubblico
> così da non guardare tutti i giorni un numero che so essere sbagliato del 20%.

**Contesto.** Chi supera una certa soglia di consumo negozia, e da quel momento il listino pubblico non descrive
più la sua realtà: i totali sono gonfiati e la riconciliazione con il rendiconto mostra uno scarto costante che non
dipende da un difetto. È un bisogno reale e documentato — LiteLLM offre da tempo la possibilità di sovrascrivere il
prezzo pubblico con il proprio (§2.6, fonte 9). La disciplina delle date resta identica a quella del catalogo
pubblico: uno sconto ha una data d'inizio, e i conti prima di quella data non cambiano.

## 2. Requisiti funzionali

1. **RF-1** — Un account può dichiarare un prezzo proprio per una coppia fornitore-modello, oppure uno sconto
   percentuale su un intero fornitore, con una data **da cui** vale.
2. **RF-2** — Il prezzo proprio ha la precedenza su quello pubblico per le misure il cui istante cade dentro la
   sua validità; fuori da quella, vale il pubblico.
3. **RF-3** — La scheda del costo di una misura dichiara **quale** prezzo è stato usato: pubblico (con la versione
   del catalogo) o negoziato (con la data da cui vale).
4. **RF-4** — Dichiarare un prezzo proprio **non ricalcola** nulla di ciò che è già stato congelato: l'account
   viene invitato al ricalcolo esplicito (storia `0017`) se vuole applicarlo al passato.
5. **RF-5** — I prezzi propri sono visibili e modificabili solo dai ruoli `owner` e `admin`, e ogni modifica
   resta nella cronologia con chi, quando e cosa: un prezzo negoziato cambia i conti dell'azienda ed è
   un'informazione di cui deve restare traccia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La tabella dei prezzi negoziati è **l'unica** dell'epica 03 che porta
  `tenant_id`, e ogni lettura filtra per il `tenant_id` preso dal gettone verificato. Prova di isolamento
  obbligatoria e severa: uno sconto di un account non deve **mai** influenzare il costo di un altro. È il difetto
  più insidioso possibile qui, perché produrrebbe numeri plausibili e sbagliati senza far scattare nulla.
- **RT-2 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `prezzo_negoziato` con
  `tenant_id`, fornitore, chiave del modello (facoltativa se lo sconto è sull'intero fornitore), prezzi o
  percentuale, `valido_da`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `GET|POST|DELETE /api/spesa_modelli/v1/prezzi-negoziati`;
  corpo validato (una percentuale fuori dall'intervallo ammesso è respinta); errori in `problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Fonti», scheda «I miei prezzi»; la scheda del costo di una misura
  indica visibilmente quando il prezzo usato è negoziato. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** Riservato a `owner` e `admin`; un `member` vede che esiste un prezzo negoziato
  ma non i suoi valori, perché è un'informazione commerciale sensibile dell'azienda; se tenta di modificarlo
  riceve `403`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: dichiarare uno sconto cambia i
  conti dell'azienda ed è un'azione che si fa guardando lo schermo. La presenza di un prezzo negoziato compare nel
  risultato di `confronta_costo_modelli` (storia `0032`), altrimenti il confronto sarebbe fatto su prezzi che
  l'account non paga.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. Il prezzo negoziato è però un **dato commerciale
  riservato** dell'account e non deve comparire in nessuna vista di piattaforma né nei registri.
- **RT-9 — Registrazione eventi (§14).** Evento «prezzo negoziato dichiarato o modificato» con `tenant_id`,
  `app_id`, `user_id`, fornitore e modello — **senza l'importo**.

## 4. Criteri di accettazione

**CA-1 — Il prezzo proprio prevale**
- **Dato** uno sconto del 20% dichiarato su un fornitore a partire dal 1° agosto
- **Quando** arriva una misura del 5 agosto su quel fornitore
- **Allora** il costo congelato è inferiore del 20% rispetto a quello che sarebbe stato col prezzo pubblico, e la
  scheda del costo dichiara che il prezzo usato è negoziato

**CA-2 — Prima della data vale il pubblico**
- **Dato** lo stesso sconto valido dal 1° agosto
- **Quando** arriva una misura del 28 luglio (arrivata in ritardo)
- **Allora** il costo è calcolato col prezzo pubblico

**CA-3 — Nessun ricalcolo automatico**
- **Dato** misure già congelate al prezzo pubblico
- **Quando** si dichiara un prezzo negoziato con validità retroattiva
- **Allora** i totali passati non cambiano e compare l'invito al ricalcolo esplicito

**CA-4 — Isolamento fra account**
- **Dato** l'account `A` con uno sconto del 50% e l'account `B` senza sconti, entrambi con misure sullo stesso
  modello e istante
- **Quando** si leggono i due costi
- **Allora** quello di `B` è calcolato al prezzo pubblico pieno, senza alcuna influenza dello sconto di `A`

**CA-5 — Ruoli**
- **Dato** un utente con ruolo `member`
- **Quando** apre la scheda dei prezzi negoziati
- **Allora** vede che ne esiste uno ma non i valori, e un tentativo di modifica riceve `403`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla precedenza fra prezzo negoziato e pubblico ai confini di validità, e di
      **integrazione** sul calcolo con sconto;
- [ ] prova di **isolamento fra account** dedicata e severa sui prezzi negoziati, e **matrice dei ruoli**;
- [ ] **prova end-to-end**: **si rimanda** alla storia `0034`; il percorso include il caso dello sconto applicato
      dalla data giusta;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; è però dichiarato che il prezzo negoziato è dato commerciale
      riservato escluso da ogni vista di piattaforma;
- [ ] **registro delle decisioni** compilato, in particolare sul perché questa tabella porta `tenant_id` mentre le
      altre dell'epica no;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0014` | Serve il calcolo del costo su cui innestare la precedenza |
| Storia `0013` | Serve la disciplina delle date del catalogo, che qui si riusa identica |

## 7. Fuori ambito

- l'applicazione retroattiva dello sconto: è la storia `0017`;
- i crediti e le promozioni a somma fissa concessi dai fornitori (per esempio un bonus di 500 dollari): non sono
  un prezzo e non si modellano qui. **Rimandati**: se emergessero come bisogno reale, sarebbero una storia propria
  nell'epica della riconciliazione, perché è lì che oggi compaiono come scarto.

## 8. Punti aperti

Nessuno.
