# 0008 — Iscrizione con doppia conferma

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 02 — Pubblico e prova del consenso
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che non vuole ritrovarsi in lista persone che non hanno mai chiesto di esserci
> voglio che chi si iscrive riceva un messaggio e debba confermare di essere davvero lui
> così da avere una prova che regge anche quando qualcuno scrive l'indirizzo di un altro.

**Contesto.** La conferma in due passi non è una funzione da spuntare in un pannello: il Garante la considera una
misura minima di garanzia allo stato dell'arte, in particolare quando le liste vengono da terzi, e ha stabilito
che gli **account non confermati vanno esclusi dalle liste di marketing**
([application-description.md](../application-description.md) §2.3 punto 3). Ne discendono due conseguenze che
questa storia rende vere nel codice invece che nella documentazione: la doppia conferma è il **comportamento
predefinito**, e un iscritto non confermato **non è inviabile**, punto. La storia viene prima del modulo pubblico
(0009) perché il modulo è solo uno dei modi di entrare: il meccanismo di conferma deve esistere prima, così che
nessuna porta d'ingresso possa aggirarlo.

## 2. Requisiti funzionali

1. **RF-1** — Ogni richiesta d'iscrizione, da qualunque porta arrivi, crea l'iscritto in stato
   `in attesa di conferma` e genera un messaggio di conferma con un collegamento a uso singolo.
2. **RF-2** — Il collegamento di conferma scade dopo un periodo dichiarato (proposta: 7 giorni) e vale una volta
   sola; usato due volte, la seconda dice che la conferma è già avvenuta e non crea una seconda registrazione.
3. **RF-3** — Alla conferma nasce una registrazione di consenso (storia 0007) con origine `doppia conferma`, il
   testo esatto che l'iscritto aveva davanti al momento della richiesta, il momento della conferma e l'indirizzo
   di rete da cui è arrivata.
4. **RF-4** — Prima della conferma l'iscritto **non è inviabile su nessun canale**: non entra in nessun segmento
   che alimenti una campagna, non fa partire nessuna automazione, non compare nel conteggio dei contattabili.
5. **RF-5** — Il messaggio di conferma è **transazionale**: non è una comunicazione commerciale, non contiene
   offerte, non consuma la metrica `messages_sent` e non porta il collegamento di disiscrizione (non c'è ancora
   niente da cui disiscriversi).
6. **RF-6** — Il messaggio di conferma si può rimandare una volta, con un limite di frequenza per recapito, e non
   si può rimandare a chi ha già confermato o a un recapito soppresso.
7. **RF-7** — Le richieste non confermate scadono: dopo il termine l'iscritto passa `in quarantena` con il motivo
   «conferma non arrivata», e resta consultabile perché è un'informazione utile a chi guarda la salute della lista.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il collegamento di conferma è legato a un solo account e a un solo
  iscritto; presentato a un altro account non risolve niente. Ogni lettura filtra per `tenant_id` dal token, salvo
  la rotta pubblica di conferma, che non ha token e ricava l'account **dal segno crittografico del collegamento**,
  mai da un parametro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/campaigns/v1/subscribers/{id}/confirmations`
  (rimando, autenticata) e `GET /api/campaigns/v1/public/confirm/{token}` (pubblica, senza token di accesso). La
  rotta pubblica ha un limite di frequenza per indirizzo di rete, non rivela se un recapito esiste e risponde
  sempre con la stessa pagina. Errori in `application/problem+json`; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Tabella `confirmation_token` sullo schema `app_campaigns` con `tenant_id`, chiave
  UUID versione 7, colonne di controllo e cancellazione logica; il segno si conserva **solo come impronta
  crittografica**, non in chiaro, così che chi legge il database non possa confermare al posto di nessuno.
- **RT-4 — Modulo frontend (§3, §5).** Nella scheda dell'iscritto: stato «in attesa di conferma» con il momento
  della richiesta, la scadenza e il pulsante di rimando. La pagina pubblica di esito conferma è una schermata
  minima, con i colori del sistema di design, funzionante in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Il messaggio di conferma e la pagina pubblica di esito escono nella **lingua
  dell'iscritto** fra `en, it, fr, es, de`; nessun testo scritto a mano nei componenti.
- **RT-6 — Varchi e quota (§6, §7).** Il messaggio di conferma **non** consuma `messages_sent`: è un messaggio di
  servizio, e farlo pagare scoraggerebbe esattamente il comportamento che vogliamo. Va però contato a parte, e un
  numero anomalo di conferme mai completate è un segnale che la console di amministrazione deve vedere. Con
  abbonamento `canceled` la porta pubblica di iscrizione risponde `402` e non crea nuovi iscritti.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. `stato_iscritto` (storia 0034) distingue
  `in attesa di conferma` da `attivo`, perché sono due risposte diverse alla domanda «posso scrivergli?».
- **RT-8 — Dati personali (§10).** Nessun campo personale **nuovo** sull'iscritto; si aggiunge al manifesto la
  voce del segno di conferma con l'indirizzo di rete e il momento, in italiano e inglese, con finalità «prova
  della conferma» e conservazione allineata a quella della registrazione di consenso. Tabella in `exportData` e
  `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Conferma richiesta», «conferma completata», «conferma scaduta»,
  «rimando rifiutato per frequenza» con `tenant_id`, `app_id`, `user_id` quando c'è, identificativo dell'iscritto
  e identificativo di correlazione; **mai** il recapito, **mai** il segno.

## 4. Criteri di accettazione

**CA-1 — Il percorso completo**
- **Dato** una richiesta d'iscrizione con un indirizzo su dominio `.test`
- **Quando** l'iscritto apre il collegamento ricevuto
- **Allora** lo stato passa da `in attesa di conferma` ad `attivo`, ed esiste una registrazione di consenso con
  origine `doppia conferma`, momento della conferma, indirizzo di rete e testo accettato

**CA-2 — Prima della conferma non si spedisce**
- **Dato** un iscritto `in attesa di conferma` che soddisfa i criteri di un segmento
- **Quando** si prepara una campagna su quel segmento
- **Allora** il conteggio dei destinatari **non** lo comprende, e il controllo pre-volo (storia 0018) lo elenca
  fra gli esclusi con il motivo

**CA-3 — Il collegamento vale una volta**
- **Dato** un collegamento di conferma già usato
- **Quando** lo si apre di nuovo
- **Allora** la pagina dice che la conferma è già avvenuta, e **non** nasce una seconda registrazione di consenso

**CA-4 — Il collegamento scade**
- **Dato** una richiesta d'iscrizione di otto giorni fa, mai confermata
- **Quando** l'iscritto apre il collegamento
- **Allora** la pagina dice che è scaduto e offre di chiederne uno nuovo; l'iscritto risulta `in quarantena` con il
  motivo «conferma non arrivata»

**CA-5 — Il collegamento non serve a scoprire chi è iscritto**
- **Dato** un collegamento manomesso o inventato
- **Quando** lo si apre
- **Allora** la risposta è indistinguibile da quella di un collegamento scaduto: nessun messaggio, nessun tempo di
  risposta e nessun codice rivelano se quel recapito esista in qualche account

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con un iscritto ciascuno in attesa
- **Quando** un utente di `A` tenta di rimandare la conferma dell'iscritto di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione e verifica del segno (uso singolo, scadenza, impronta) e di
      **integrazione** sulla rotta pubblica, compreso il limite di frequenza;
- [ ] prova di **isolamento fra account** sulla conferma e sul rimando;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) parte dall'iscrizione con
      doppia conferma, perché è la porta d'ingresso canonica dell'app; voce aggiunta al registro di copertura;
- [ ] **traduzioni** del messaggio di conferma e della pagina di esito in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per il segno di conferma, con indirizzo di rete e
      momento, e tabella in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché la conferma è predefinita e non disattivabile e
      perché il messaggio di conferma non consuma quota;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; distinzione degli stati dichiarata per 0034;
- [ ] controllo automatico di **accessibilità** verde sulla pagina pubblica di esito;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | La conferma produce una registrazione di consenso: senza il registro non c'è dove scriverla |
| Storia `0017` (epica 04) | Il messaggio di conferma è pur sempre posta che deve arrivare: in locale si consegna al fornitore simulato, in produzione richiede un dominio mittente verificato |

## 7. Fuori ambito

- il modulo pubblico da cui la richiesta arriva: è la storia 0009;
- la disattivazione della doppia conferma: **non esiste** e non è un rimando, è una scelta;
- l'invio del messaggio di conferma su canali diversi dalla posta elettronica: la conferma passa dalla posta
  elettronica anche quando l'iscrizione riguarda un altro canale, perché è il canale che possediamo (epica 04).

## 8. Punti aperti

- **Durata di validità del collegamento.** Proposta: 7 giorni. Non ho trovato un riferimento normativo o di
  settore che indichi un valore: è una scelta operativa, la chiude lo sviluppatore.
- **Conservazione dell'indirizzo di rete della conferma.** È un dato personale raccolto per prova. Il termine
  proposto è quello della registrazione di consenso; la revisione legale deve dire se sia proporzionato o se vada
  ridotto.
