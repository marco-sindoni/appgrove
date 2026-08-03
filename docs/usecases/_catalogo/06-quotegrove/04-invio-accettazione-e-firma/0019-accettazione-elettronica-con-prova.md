# 0019 — Accettazione elettronica con prova

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 04 — Invio, accettazione e firma
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha ricevuto un «va bene» dal cliente
> voglio che quel «va bene» resti scritto, legato alla versione esatta dell'offerta e con la sua prova
> così da poter dimostrare, se un domani si discute, cosa è stato accettato, da chi e quando.

**Contesto.** È la storia che dà all'app il suo valore difendibile. Il regolamento europeo eIDAS (art. 25) dice
che a una firma elettronica non si possono negare effetti giuridici solo perché è elettronica; l'art. 20 comma
1-bis del Codice dell'amministrazione digitale aggiunge che il valore probatorio di una firma **semplice** è
liberamente valutato dal giudice in base a sicurezza, integrità e immodificabilità della soluzione, e che l'onere
della prova sta a chi se ne avvale (§2.3, punto 2 della descrizione dell'applicazione). Tradotto: il clic vale, e
vale **quanto è solida la prova che conserviamo**. Questa storia costruisce quella prova.

## 2. Requisiti funzionali

1. **RF-1** — Dalla pagina pubblica il destinatario accetta l'offerta dichiarando il proprio nome e cognome e
   spuntando una casella di presa visione delle condizioni; l'accettazione richiede una **conferma esplicita** in
   due passi (non un solo clic distratto).
2. **RF-2** — L'accettazione registra la **prova**: identità dichiarata, indirizzo di posta a cui il collegamento
   era stato inviato, momento, indirizzo di rete, tipo di dispositivo, **impronta della versione accettata** e
   identificativo del collegamento usato.
3. **RF-3** — L'accettazione è sempre riferita a **una versione precisa**: se nel frattempo è stata emessa una
   revisione, il collegamento vecchio non accetta più e lo dice.
4. **RF-4** — Dopo l'accettazione il preventivo diventa `accettato` e **immutabile**; la pagina pubblica mostra
   l'esito e resta consultabile fino alla scadenza del collegamento.
5. **RF-5** — Chi ha inviato riceve una notifica e vede la prova completa nel backoffice; il destinatario riceve
   una copia dell'accettazione al proprio indirizzo di posta.
6. **RF-6** — La prova si può esportare come documento leggibile: è ciò che si allega quando serve dimostrare
   qualcosa.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1) — via gettone.** L'accettazione arriva dalla superficie pubblica: vale
  quanto detto nella storia `0018`, il gettone abilita solo questo preventivo e solo questo atto.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/preventivi/v1/pubblico/{gettone}/accettazione`,
  **idempotente**: una seconda chiamata con lo stesso gettone restituisce la prova già registrata e non ne crea
  un'altra. Errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V12__prova_accettazione.sql`: tabella `prova_accettazione` con
  `tenant_id`, UUID versione 7, colonne di controllo; la riga **non si modifica mai** dopo la creazione.
- **RT-4 — Frontend (§3, §5).** Passo di conferma sulla pagina pubblica, leggibile da telefono, con il testo di
  ciò che si sta accettando sempre visibile; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutti i testi dell'accettazione, compresa la copia inviata al destinatario, in
  `en, it, fr, es, de`, resi nella lingua del destinatario.
- **RT-6 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: identità dichiarata, indirizzo
  di posta, indirizzo di rete, dispositivo — con finalità «prova dell'accettazione», base giuridica proposta
  «necessità di accertare o far valere un diritto» e durata proposta di dieci anni. Campi annotati
  `@PersonalData`; tabella aggiunta a `exportData` e `purgeData`.
- **RT-7 — Registrazione eventi (§14).** `preventivo accettato` con `tenant_id`, `app_id`, correlazione e
  identificativo della versione — **mai il nome del firmatario né il suo indirizzo di rete**.
- **RT-8 — Esposizione conversazionale (§12).** **Nessuno strumento conversazionale accetta un preventivo**, in
  nessuna forma e con nessuna conferma: l'accettazione è un atto del destinatario e farla compiere a un assistente
  sarebbe fabbricare una prova. Va scritto nel contratto degli strumenti (storia `0028`) come divieto esplicito.

## 4. Criteri di accettazione

**CA-1 — Accettazione riuscita**
- **Dato** un preventivo valido aperto dal destinatario · **Quando** dichiara il proprio nome, spunta la presa
  visione e conferma · **Allora** il preventivo è `accettato`, la prova contiene tutti i campi previsti e
  l'impronta coincide con quella della versione mostrata

**CA-2 — Versione superata**
- **Dato** un collegamento della versione 1 e una revisione già emessa · **Quando** il destinatario tenta di
  accettare · **Allora** l'app rifiuta spiegando che è disponibile una versione aggiornata, e non registra nulla

**CA-3 — Doppia accettazione**
- **Dato** un preventivo già accettato · **Quando** si richiama la stessa operazione · **Allora** si ottiene la
  prova già registrata, non una seconda prova

**CA-4 — Offerta scaduta**
- **Dato** un preventivo la cui validità è finita · **Quando** il destinatario tenta di accettare · **Allora**
  l'app rifiuta e invita a contattare il mittente (il prolungamento è della storia `0021`)

**CA-5 — Il documento non cambia più**
- **Dato** un preventivo accettato · **Quando** qualcuno dell'account tenta di modificarlo · **Allora**
  l'operazione è respinta e l'app propone di emettere un documento nuovo

**CA-6 — La prova si esporta**
- **Dato** una accettazione registrata · **Quando** si esporta la prova · **Allora** si ottiene un documento
  leggibile con tutti i campi e l'impronta della versione accettata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sull'idempotenza e sul confronto delle impronte, di **integrazione** sull'intero atto;
- [ ] prova di **isolamento fra account** e prova di sicurezza sul gettone;
- [ ] **prova end-to-end**: **coperta ora** — è il passo centrale del percorso del destinatario (storia `0030`),
      dove si aggiorna il registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue, compresa la copia inviata al destinatario;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la prova di accettazione;
- [ ] **registro delle decisioni** compilato: **cosa entra nella prova e perché ciascun campo è necessario**, con
      il riferimento all'art. 20 comma 1-bis del Codice dell'amministrazione digitale;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` | la pagina pubblica e il gettone |
| storia `0015` | l'accettazione è riferita a una versione congelata |

## 7. Fuori ambito

- la **firma elettronica avanzata o qualificata**: è di SignGrove (catalogo 15), che ha i servizi fiduciari;
- la marcatura temporale certificata: vedi punti aperti della storia `0015`;
- l'identificazione forte del firmatario (documento d'identità, identità digitale): non prevista.

## 8. Punti aperti

1. **Un secondo fattore per l'accettazione.** ePreventivo manda un codice usa e getta all'indirizzo di posta prima
   di far firmare (§2.1 della descrizione dell'applicazione): irrobustisce la prova legando l'atto al possesso
   della casella. Lo **propongo come opzione attivabile per preventivo**, ma non lo decido: aggiunge attrito e la
   soglia oltre la quale conviene è una scelta di prodotto dello sviluppatore.
2. **Cancellazione della prova** su richiesta dell'interessato: conflitto già registrato nella storia `0007` e nel
   punto 4 dei rischi. Lo chiude lo sviluppatore con revisione legale.
