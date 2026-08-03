# 0014 — Classe di riservatezza e ruoli

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 03 — Catalogo delle metriche e tracciabilità
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vuole dare a tutta la squadra un cruscotto operativo
> voglio che il fatturato e i margini restino visibili solo a me e a chi amministra
> così da poter aprire lo strumento a tutti senza aprire i conti a tutti.

**Contesto.** È il secondo dei tre problemi veri di questa app, e va affrontato dicendo **anche quello che la
piattaforma non sa fare**. Ciò che c'è: tre ruoli per l'intero account — `owner`, `admin`, `member`
([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §6). Ciò che questa storia costruisce sopra: due
**classi di riservatezza** sulla metrica, non sulla persona. Ciò che resta fuori: qualunque permesso per risorsa
o per valore di dimensione — non si inventa un secondo modello di autorizzazione dentro una singola app
(§4.4 della [descrizione](../application-description.md), punto aperto 3).

## 2. Requisiti funzionali

1. **RF-1** — Ogni definizione di metrica porta una **classe di riservatezza** con due soli valori: `operativa`
   (quantità, tempi, volumi, conteggi) e `economica` (importi, margini, crediti, valore).
2. **RF-2** — Una metrica `operativa` è visibile a tutti i ruoli. Una metrica `economica` è visibile a `owner` e
   `admin`; per un `member` **non esiste**: non compare nel catalogo, non è selezionabile in un riquadro, non è
   citabile in una domanda al copilota.
3. **RF-3** — Le metriche predefinite nascono già classificate, e la classificazione è **la più prudente
   ragionevole**: nel dubbio, `economica`.
4. **RF-4** — Un `owner` può **alzare** la riservatezza di una metrica (da `operativa` a `economica`) senza
   ostacoli; può **abbassarla** solo con una conferma esplicita che dica chi comincerà a vederla.
5. **RF-5** — Una metrica **derivata** eredita la classe più restrittiva fra quelle che compone: un margine
   costruito su un ricavo economico è economico, e non c'è modo di renderlo operativo.
6. **RF-6** — La regola vale **ovunque**, senza eccezioni: cruscotti, copilota, avvisi, rapporti periodici,
   esportazioni e strumenti conversazionali. Un solo punto di applicazione nel codice, non uno per superficie.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La classe è un attributo della definizione, che è già filtrata per
  account; il ruolo di chi chiede viene dal gettone verificato, mai dalla richiesta.
- **RT-2 — Interfaccia di programmazione (§2).** Il filtro per classe si applica **nel livello di accesso ai
  dati**, non nell'interfaccia: una risorsa chiamata da un `member` non restituisce metriche economiche nemmeno
  se qualcuno le chiede per chiave. Errori in `application/problem+json`; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** La sezione Metriche mostra la classe con un contrassegno che non si
  affida al solo colore; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi delle due classi e i messaggi di rifiuto esistono in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** Il varco di ruolo è il quarto della catena: risponde `403` quando il ruolo non
  basta. **Regola specifica di questa app**: quando una metrica economica concorre a un calcolo richiesto da un
  `member`, il risultato **non è un numero calcolato senza quel pezzo** — è un rifiuto. Un aggregato filtrato è
  un numero sbagliato, non un numero parziale (§4.3 della descrizione, regola 3).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-14 — Registrazione eventi (§14).** «Classe di riservatezza modificata» con `tenant_id`, `app_id`,
  `user_id`, chiave della metrica, valore precedente e nuovo.
- **RT-11 — Prove (§11).** Matrice dei ruoli obbligatoria su **ogni** superficie: risorsa delle metriche,
  calcolo, cruscotto, copilota, esportazione, strumenti conversazionali.

## 4. Criteri di accettazione

**CA-1 — Un `member` non vede l'economico**
- **Dato** un utente con ruolo `member` e la metrica `fatturato_emesso` di classe `economica`
- **Quando** apre la sezione Metriche
- **Allora** quella metrica non compare nell'elenco

**CA-2 — Chiedere per chiave non basta**
- **Dato** lo stesso utente `member`, che conosce la chiave `fatturato_emesso`
- **Quando** chiama direttamente la risorsa con quella chiave
- **Allora** riceve `403` con `application/problem+json`, e nessun valore

**CA-3 — Il calcolo non si «adatta»**
- **Dato** un utente `member` e un cruscotto che contiene sia riquadri operativi sia economici
- **Quando** apre il cruscotto
- **Allora** vede i riquadri operativi e, al posto di quelli economici, un messaggio che dice che il suo ruolo
  non consente di vederli — **non** un valore ridotto

**CA-4 — La derivata eredita la restrizione**
- **Dato** una derivata che compone una metrica operativa e una economica
- **Quando** viene pubblicata
- **Allora** la sua classe risulta `economica` e non è modificabile in `operativa`

**CA-5 — Abbassare richiede una conferma informata**
- **Dato** un `owner` che vuole rendere `operativa` una metrica `economica`
- **Quando** chiede il cambio
- **Allora** vede una conferma che dice quante persone dell'account cominceranno a vederla, e solo dopo la
  conferma il cambio avviene

**CA-6 — Isolamento fra account**
- **Dato** due account con la stessa metrica classificata diversamente
- **Quando** un utente di `A` legge la classe
- **Allora** vede la classificazione di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'ereditarietà della classe nelle derivate e di **integrazione** sul filtro nel
      livello di accesso ai dati;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli completa** su ogni superficie: risorsa, calcolo,
      cruscotto, copilota, esportazione, strumenti conversazionali;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include il passo «un `member` non vede il
      fatturato»; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con le due sole classi, il rifiuto invece del calcolo parziale, e
      il perché non si è inventato un modello di permessi più fine;
- [ ] contratto degli **strumenti conversazionali**: `elenca_metriche` restituisce solo ciò che il ruolo di chi
      chiede consente (storia 0031);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | la classe è un attributo della definizione |
| modello dei ruoli di piattaforma | tre ruoli per l'intero account: è quello che c'è, e non si estende da qui |

## 7. Fuori ambito

- **permessi per risorsa** («Anna vede questo cruscotto e non quello») e **per valore di dimensione** («Luca vede
  solo la sede di Milano»): non esistono in piattaforma e **non si inventano qui**. Punto aperto 3 della
  descrizione, di proprietà della piattaforma;
- il comportamento del copilota di fronte a una metrica riservata: storia 0025 — qui si costruisce la regola, là
  si applica alla conversazione.

## 8. Punti aperti

- 🛑 **Il modello di ruoli della piattaforma non basta per il caso reale.** Due classi e tre ruoli coprono «il
  fatturato non lo vedono tutti», ma non coprono «ognuno vede la propria sede» né «il commerciale vede i ricavi e
  non i margini». La risposta onesta al cliente è che quelle configurazioni **non si possono fare**, non un
  meccanismo improvvisato. Chiude: **piattaforma** (punto aperto 3 della descrizione).
- **Due classi bastano o ne servono tre?** Una terza classe (`riservata al titolare`, visibile al solo `owner`)
  è concepibile per i dati più sensibili. Raccomandazione: **due**, perché con tre ruoli una terza classe
  distinguerebbe solo `owner` da `admin` — un caso che nessuna fonte di questa analisi ha mostrato.
  Chiude: **sviluppatore**.
