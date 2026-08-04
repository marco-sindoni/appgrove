# 0011 — Preferenze di contatto e prova del consenso

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 02 — Anagrafica di contatti e aziende
**Storia**: `0011` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che risponde di quello che il suo archivio contiene
> voglio sapere per ogni persona se posso contattarla, su quale canale e in base a cosa
> così da poter dimostrare che il contatto commerciale che ho fatto era lecito.

**Contesto.** L'analisi normativa ([application-description.md](../application-description.md) §2.3, fonte
[Cyber Security 360](https://www.cybersecurity360.it/legal/privacy-dati-personali/marketing-e-campagne-di-lead-generation-nel-rispetto-del-gdpr-linee-guida/))
dice tre cose che cambiano il modello dati: il consenso al marketing dev'essere **separato** dalle altre finalità,
dev'essere **documentabile** (marca temporale, testo accettato, canale) e dev'essere **revocabile** in ogni
momento. Una casella «accetta il marketing» non soddisfa nessuna delle tre. Serve una tabella di prova, e serve
adesso: se arriva dopo il modulo web (storia 0028) si raccolgono consensi che non si sanno dimostrare.

## 2. Requisiti funzionali

1. **RF-1** — Ogni contatto ha un elenco di preferenze, una per canale (posta elettronica, telefono, messaggistica),
   ciascuna con esito ammesso/negato, **base giuridica** (`consenso` oppure `legittimo interesse`), momento della
   registrazione, testo accettato e origine della prova (modulo web, dichiarazione dell'operatore, importazione).
2. **RF-2** — Una preferenza non si **modifica**: se ne aggiunge una nuova che rende superata la precedente. Lo
   storico resta integro perché è lui la prova.
3. **RF-3** — La revoca è una registrazione nuova con esito «negato»: **non cancella** la prova del consenso
   precedente, perché serve a dimostrare che all'epoca il contatto era lecito.
4. **RF-4** — La scheda del contatto mostra in evidenza lo stato attuale per canale, con il momento e la base
   giuridica, e uno storico consultabile.
5. **RF-5** — Quando la base giuridica dichiarata è «legittimo interesse», l'interfaccia ricorda che serve una
   valutazione di bilanciamento documentata, e che è responsabilità del titolare — cioè del cliente, non nostra.
6. **RF-6** — Un contatto con esito «negato» su un canale è **segnalato visibilmente** ovunque compaia quel
   recapito, non solo nella sezione dei consensi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le preferenze filtrano per `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/sales/v1/contacts/{id}/preferences`;
  **nessun** `PATCH` e **nessun** `DELETE`: la storia si aggiunge, non si riscrive. Errori in
  `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `contact_preference` già creata dalla storia 0002; qui si aggiunge l'indice
  che recupera l'ultima registrazione per contatto e canale, a partire da `tenant_id`. La cancellazione logica su
  questa tabella si usa **solo** per l'esercizio dei diritti dell'interessato, mai per «correggere» una
  registrazione.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Come posso contattarlo» nella scheda del contatto, con stato
  attuale, storico e modulo di registrazione; segnalazione accanto ai recapiti negati; solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, compresi i nomi delle basi giuridiche e l'avviso sul legittimo
  interesse, presenti in `en, it, fr, es, de`. Attenzione: i **testi accettati** dal contatto restano nella lingua
  in cui sono stati accettati e non si traducono — sono prova, non interfaccia.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Registrare una preferenza richiede un posto attivo.
- **RT-7 — Esposizione conversazionale (§12).** `get_contact` (storia 0034) restituisce lo **stato attuale** delle
  preferenze, perché un assistente che suggerisce di richiamare qualcuno deve sapere se si può. Registrare una
  preferenza **non** è esposto alla chat: è una dichiarazione con valore probatorio e va fatta da una persona
  nell'interfaccia. Scelta dichiarata.
- **RT-8 — Dati personali (§10).** È la storia che rende vera la voce `contact_preference.*` del manifesto: campi
  annotati `@PersonalData`, voci in italiano e inglese con finalità «dimostrare la liceità del contatto
  commerciale» e base giuridica «obbligo di dimostrabilità in capo al titolare». La tabella entra in `exportData` e
  `purgeData`. Nota di conformità da scrivere nel manifesto: la prova **sopravvive alla revoca** e viene meno solo
  con la cancellazione dei dati dell'interessato.
- **RT-9 — Registrazione eventi (§14).** «Preferenza registrata» e «preferenza revocata» con identificativo del
  contatto, canale e base giuridica; **mai** il recapito né il testo accettato.

## 4. Criteri di accettazione

**CA-1 — Registrazione con prova**
- **Dato** un contatto senza preferenze
- **Quando** l'operatore registra «posta elettronica ammessa, base giuridica consenso» indicando il testo accettato
- **Allora** la scheda mostra lo stato «ammesso» con il momento, la base giuridica e il testo, e la registrazione
  compare nello storico

**CA-2 — Revoca che non cancella**
- **Dato** un contatto con consenso registrato tre mesi fa
- **Quando** si registra la revoca
- **Allora** lo stato attuale diventa «negato», e la registrazione di tre mesi fa **è ancora presente** nello
  storico con i suoi dati

**CA-3 — Una preferenza non si riscrive**
- **Dato** una preferenza esistente
- **Quando** si tenta di modificarla con una richiesta di aggiornamento
- **Allora** il servizio rifiuta l'operazione: l'unica via è registrarne una nuova

**CA-4 — Il divieto si vede**
- **Dato** un contatto con «telefono negato»
- **Quando** un venditore apre la scheda o lo trova nell'elenco
- **Allora** il numero è mostrato con una segnalazione visibile che dice che non si può usare per contatti
  commerciali

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con contatti propri
- **Quando** un utente di `A` chiede le preferenze di un contatto di `B`
- **Allora** riceve `404`

**CA-6 — La prova esce nell'esportazione**
- **Dato** un contatto con tre registrazioni di preferenza
- **Quando** si esercita il diritto di esportazione
- **Allora** compaiono tutte e tre, con momento, canale, base giuridica e testo accettato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dello stato attuale a partire dallo storico e di **integrazione** sulla
      risorsa, con la verifica che l'aggiornamento sia rifiutato;
- [ ] prova di **isolamento fra account** sulle preferenze;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-SALES]` (storia 0037) include la registrazione di un
      consenso, perché è il passo che distingue questa app da una rubrica; voce aggiunta al registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con i testi accettati esclusi dalla traduzione;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `contact_preference`, campi annotati, tabella in
      esportazione e cancellazione, con la nota sulla sopravvivenza della prova alla revoca;
- [ ] **registro delle decisioni** compilato, con annotato perché la preferenza è ad accrescimento e non
      modificabile;
- [ ] contratto degli **strumenti conversazionali**: stato attuale in lettura, registrazione **non** esposta, con
      la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sulla sezione dei consensi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | Le preferenze stanno su un contatto |
| Conferma della classificazione dei dati personali | La base giuridica della prova del consenso è materia da validare, non da decidere qui |

## 7. Fuori ambito

- l'invio di comunicazioni commerciali: è l'app 16 del catalogo. Qui si registra il consenso, non lo si usa;
- la raccolta del consenso dal modulo web pubblico: storia 0029, che usa questa struttura;
- il confronto con il Registro pubblico delle opposizioni: fuori perimetro, LeadGrove non compone numeri
  ([application-description.md](../application-description.md) §2.3 punto 4);
- la verifica che la valutazione di bilanciamento del legittimo interesse esista davvero: è del titolare.

## 8. Punti aperti

- **Confine di responsabilità sul legittimo interesse** — l'app registra ciò che il cliente dichiara ma non lo può
  verificare. Dove finisce la nostra responsabilità di responsabile del trattamento va scritto nel contratto di
  trattamento, non solo nel manifesto. Chiude la revisione legale.
