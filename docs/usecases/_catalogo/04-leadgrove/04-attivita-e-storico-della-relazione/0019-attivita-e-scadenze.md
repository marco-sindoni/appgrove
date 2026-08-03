# 0019 — Attività e scadenze

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 04 — Attività e storico della relazione
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0013` — è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore che ha appena chiuso una telefonata
> voglio annotare che devo richiamare fra dieci giorni, agganciato alla persona giusta
> così da non affidare il richiamo a un promemoria sul telefono che poi non so a chi si riferisce.

**Contesto.** L'attività è il meccanismo che fa tornare l'utente nell'app ogni giorno: senza, LeadGrove è un
archivio che si consulta, non uno strumento che si usa. È anche il rimedio principale al rischio dell'archivio
vuoto ([application-description.md](../application-description.md) §11): un venditore che apre l'app per sapere
chi richiamare ci mette poi anche i dati.

## 2. Requisiti funzionali

1. **RF-1** — Un utente con un posto crea un'attività indicando tipo (chiamata, riunione, compito, messaggio),
   titolo e scadenza, agganciandola a **un solo** riferimento fra contatto, azienda e trattativa.
2. **RF-2** — L'attività ha un responsabile, di norma chi la crea, riassegnabile a un altro membro con un posto.
3. **RF-3** — Un'attività si completa registrando l'esito in testo libero; una completata non torna aperta, se ne
   crea una nuova.
4. **RF-4** — Si può creare un'attività direttamente dalla scheda di contatto, azienda o trattativa, con il
   riferimento già compilato.
5. **RF-5** — Chiudendo una trattativa (storia 0016), le attività aperte collegate vengono segnalate e l'utente
   sceglie se completarle o lasciarle: non si cancellano in silenzio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `Activity` filtra per `tenant_id` dal token
  verificato; il riferimento indicato deve appartenere allo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST|GET|PATCH /api/sales/v1/activities[/{id}]` e
  `POST /api/sales/v1/activities/{id}/complete`; corpo validato (un solo riferimento valorizzato, scadenza
  presente); errori in `application/problem+json`; paginazione con totale; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `activity` già creata dalla storia 0002; qui il vincolo che uno e uno solo
  fra i tre riferimenti sia valorizzato, e l'indice su `(tenant_id, owner_user_id, due_at, completed_at)`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Attività e blocco «Prossime attività» nelle schede; solo token del
  sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tipi di attività, etichette e messaggi in `en, it, fr, es, de`; le date si
  formattano secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo della metrica `seats`. Chi non ha un posto riceve `403`.
- **RT-7 — Esposizione conversazionale (§12).** `list_activities` in lettura (storia 0034); `log_activity` in
  scrittura **con bozza e conferma** (storia 0035): dettare «richiamare Alfa lunedì» è uno dei tre motivi per cui
  il livello conversazionale rende questa app più utile.
- **RT-8 — Dati personali (§10).** `activity.title` e `activity.outcome` sono testo libero e sono già dichiarati
  nel manifesto: qui si valorizzano, quindi vanno verificati annotazione `@PersonalData` e presenza in
  `exportData` e `purgeData`. Accanto ai campi liberi compare l'avviso di non inserire dati sensibili.
- **RT-9 — Registrazione eventi (§14).** «Attività creata», «attività completata» con identificativi, tipo e
  scadenza; **mai** titolo o esito, che sono testo scritto da una persona.

## 4. Criteri di accettazione

**CA-1 — Creazione dalla scheda**
- **Dato** un venditore sulla scheda di un contatto
- **Quando** crea una chiamata con scadenza fra dieci giorni
- **Allora** l'attività compare nel blocco «prossime attività» della scheda e nella sezione Attività

**CA-2 — Riferimento unico**
- **Dato** una richiesta che indica insieme contatto e trattativa
- **Quando** arriva al servizio
- **Allora** riceve `400` con la spiegazione: un'attività ha un solo riferimento

**CA-3 — Completamento**
- **Dato** un'attività aperta
- **Quando** il venditore la completa registrando l'esito
- **Allora** risulta completata con momento e autore, e non si può riaprire

**CA-4 — Chiusura della trattativa con attività aperte**
- **Dato** una trattativa con due attività aperte
- **Quando** il venditore la chiude
- **Allora** l'app segnala le due attività e chiede cosa farne; nessuna viene cancellata da sola

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` crea un'attività riferita a un contatto di `B`
- **Allora** riceve `404` sul riferimento e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul vincolo del riferimento unico e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** sulle attività e sui loro riferimenti;
- [ ] **prova end-to-end**: coprire ora — la creazione di un'attività di richiamo è un passo del percorso
      `[J-SALES]` (storia 0037); voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato per `activity.title` e `activity.outcome`, con l'avviso sui dati sensibili
      accanto ai campi liberi;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: rimando a `list_activities` e `log_activity`;
- [ ] controllo automatico di **accessibilità** verde su elenco e modulo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0007` e `0013` | Le attività si agganciano a contatti, aziende e trattative |
| Storia `0004` | Serve un posto per creare e per essere responsabile |

## 7. Fuori ambito

- la vista «cosa devo fare oggi»: storia 0020;
- le notifiche in arrivo (posta, notifiche del sistema operativo): non previste in questa proposta, sono un canale
  verso l'esterno e vanno decise a parte;
- la ricorrenza («ogni lunedì»): non prevista;
- la sincronizzazione con calendari esterni: fuori perimetro
  ([application-description.md](../application-description.md) §11.3).

## 8. Punti aperti

- **Promemoria in arrivo.** Un'agenda senza promemoria richiede che l'utente apra l'app. Mandare un promemoria è
  un effetto verso l'esterno e apre il tema del canale (posta elettronica? quale fornitore?): è una decisione di
  prodotto dello sviluppatore, non di questa storia.
