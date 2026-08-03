# 0018 — Pagina pubblica e tracciamento della visualizzazione

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 04 — Invio, accettazione e firma
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha mandato un'offerta e non riceve risposta
> voglio sapere se il cliente l'ha almeno aperta
> così da capire se devo insistere o se il problema è che non gli è mai arrivata.

**Contesto.** Nella ricerca di mercato «sapere se il cliente l'ha aperto» è la funzione che viene citata più
spesso come motivo per pagare (§2.5 della descrizione dell'applicazione). Serve una pagina che il destinatario
apra **senza registrarsi**: è la superficie più esposta dell'applicazione e la ragione per cui il gettone della
storia `0017` deve essere fatto bene. Questa storia mostra il documento e registra la visualizzazione; l'atto di
accettare è della storia `0019`.

## 2. Requisiti funzionali

1. **RF-1** — Il collegamento riservato apre una pagina pubblica che mostra il documento della versione inviata,
   leggibile da telefono, nella lingua del destinatario.
2. **RF-2** — La pagina mostra chi ha inviato l'offerta, fino a quando è valida e cosa si può fare (accettare,
   rifiutare, chiedere una modifica — le azioni arrivano con le storie `0019` e `0020`).
3. **RF-3** — Ogni apertura registra un evento con momento e indirizzo di rete; l'elenco degli eventi è visibile a
   chi ha inviato.
4. **RF-4** — La pagina porta una **informativa breve** al destinatario: chi tratta i suoi dati (l'azienda che ha
   inviato il preventivo, con appgrove come fornitore), quali dati sono raccolti su quella pagina e perché.
5. **RF-5** — Un gettone scaduto, revocato o inesistente porta a una pagina neutra che **non rivela** se il
   preventivo esiste, e invita a contattare chi ha inviato.
6. **RF-6** — La pagina non è indicizzabile dai motori di ricerca e non contiene nulla che permetta di risalire ad
   altri documenti dello stesso account.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1) — deviazione dichiarata.** Qui **non c'è un token di accesso**: il
  `tenant_id` e l'identificativo del preventivo arrivano dal **gettone di capacità firmato dal server** della
  storia `0017`, verificato a ogni richiesta. Il gettone abilita **una sola** cosa: leggere quel preventivo (e,
  con la storia `0019`, rispondergli). Non concede nessun'altra lettura, non è un'identità e non diventa mai una
  sessione. È l'unica deviazione dall'invariante ed è approvata a parte (punto 2 dei rischi della descrizione
  dell'applicazione).
- **RT-2 — Interfaccia di programmazione (§2).** Rotte pubbliche separate e riconoscibili, per esempio
  `/api/preventivi/v1/pubblico/{gettone}`, con limitazione di frequenza per indirizzo di rete e risposte in
  `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V11__eventi_preventivo.sql`: tabella `evento_preventivo` con
  `tenant_id`, UUID versione 7, colonne di controllo, tipo dell'evento, momento e indirizzo di rete.
- **RT-4 — Frontend (§3, §5).** La pagina pubblica **non vive dentro il backoffice**: è una superficie a sé, che
  usa gli stessi token del sistema di design, funziona in tema chiaro e scuro e non carica nulla che richieda
  un'autenticazione.
- **RT-5 — Cinque lingue (§4).** La pagina è resa nella lingua del destinatario fra `en, it, fr, es, de`, con
  ricaduta sulla lingua predefinita se manca.
- **RT-6 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: momento e **indirizzo di rete**
  di chi apre la pagina, con finalità «informare il mittente della presa visione», base giuridica e durata
  proposta di 24 mesi; campi annotati `@PersonalData`; tabella `evento_preventivo` aggiunta a `exportData` e
  `purgeData`. **Nessun tracciamento oltre questo**: niente strumenti di analisi, niente cookie non tecnici,
  nessun banner di consenso.
- **RT-7 — Registrazione eventi (§14).** `pagina pubblica aperta`, `gettone rifiutato` con `tenant_id`, `app_id`
  e correlazione; l'indirizzo di rete resta nel dato applicativo, **non** nel registro tecnico.
- **RT-8 — Prove (§11).** Prova di sicurezza dedicata: gettone di un altro account, gettone scaduto, gettone
  manomesso, gettone revocato — tutti respinti allo stesso modo.

## 4. Criteri di accettazione

**CA-1 — Il cliente vede l'offerta**
- **Dato** un preventivo inviato · **Quando** il destinatario apre il collegamento dal telefono · **Allora** vede
  il documento nella propria lingua, con validità e mittente, senza doversi registrare

**CA-2 — La visualizzazione si vede**
- **Dato** la pagina appena aperta · **Quando** chi ha inviato guarda il preventivo nel backoffice · **Allora**
  legge che è stato visto, quando, e lo stato passa da `inviato` a `visto`

**CA-3 — Gettone non valido**
- **Dato** un gettone scaduto, revocato, manomesso o di un altro account · **Quando** lo si apre · **Allora** si
  vede la **stessa** pagina neutra in tutti e quattro i casi, e nulla lascia capire se il documento esista

**CA-4 — Informativa presente**
- **Dato** la pagina pubblica · **Quando** la si apre · **Allora** l'informativa breve è visibile senza doverla
  cercare, e dice chi tratta i dati e quali

**CA-5 — Nessuna scoperta laterale**
- **Dato** un gettone valido · **Quando** si tenta di modificarne una parte per raggiungere un altro preventivo
- **Allora** la richiesta è respinta, e la frequenza delle richieste dallo stesso indirizzo è limitata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sulla verifica del gettone e di **integrazione** sulla rotta pubblica;
- [ ] prova di **isolamento fra account** in forma di prova di sicurezza sul gettone (RT-8);
- [ ] **prova end-to-end**: **coperta ora** — è il primo passo del percorso del destinatario, creato dalla storia
      `0030`, dove si aggiorna il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** della pagina pubblica in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con gli eventi di visualizzazione;
- [ ] **registro delle decisioni** compilato: **la deviazione sull'origine del `tenant_id`, con la forma esatta del
      gettone e il perché** — è la decisione più importante dell'applicazione;
- [ ] avvio locale invariato: la pagina pubblica è raggiungibile dal proxy locale senza cablaggi a mano.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | il gettone e l'invio |
| approvazione dello sviluppatore sul gettone di capacità | è una deviazione da un invariante di piattaforma |

## 7. Fuori ambito

- accettazione e rifiuto: storie `0019` e `0020`;
- il conteggio di quante volte la pagina è stata aperta da persone diverse: non distinguibile in modo affidabile e
  non richiesto.

## 8. Punti aperti

**Anteprime automatiche dei programmi di posta.** Alcuni sistemi aprono i collegamenti per generare l'anteprima o
per controllo di sicurezza: l'app rischia di dire «visto» quando nessuno ha guardato. Attenuazione proposta:
registrare comunque l'evento ma distinguere le aperture con interazione. Se la distinzione non è affidabile, va
detto all'utente invece di fingere precisione. Da verificare in implementazione.
