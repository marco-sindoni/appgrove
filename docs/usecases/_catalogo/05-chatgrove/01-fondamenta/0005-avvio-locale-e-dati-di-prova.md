# 0005 — Avvio locale e dati di prova

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che apre il repository per la prima volta
> voglio avviare ChatGrove in locale e trovarci dentro un negozio finto già popolato
> così da poter lavorare su una schermata senza prima procurarmi un numero vero e un fornitore vero.

**Contesto.** ChatGrove dipende da un canale esterno che in locale **non deve esistere**: non si collega un
numero vero per far girare le prove, come non si usa un fornitore di pagamento vero. Serve quindi un **canale
simulato**, con dati inventati, che riceva e mostri messaggi. È il momento giusto per farlo adesso: dalla
storia `0006` in poi ogni funzione presuppone un canale, e senza simulatore ogni sviluppatore se ne
costruirebbe uno diverso.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` elenca `chat_commerce` con la porta `8105` e lo schema `app_chat_commerce`,
   e `./app-start.sh` lo avvia senza modifiche manuali agli script.
2. **RF-2** — In profilo locale esiste un **canale simulato**: accetta gli invii, non contatta nessuna rete
   esterna e restituisce esiti di consegna verosimili (consegnato, letto, fallito).
3. **RF-3** — Il canale simulato permette di **iniettare un messaggio in arrivo**, così da poter provare la
   ricezione senza un telefono.
4. **RF-4** — Esiste un insieme di dati di prova inventati: un negozio con dieci contatti, tre conversazioni
   aperte, un catalogo di dodici prodotti e due ordini in stati diversi.
5. **RF-5** — I dati di prova sono **deterministici** e chiaramente finti: nomi inventati, numeri di telefono
   non assegnabili a persone reali, indirizzi di posta elettronica nel dominio `*.test`.
6. **RF-6** — In profilo diverso da quello locale e di prova, il canale simulato **non** si attiva: la
   configurazione lo impedisce e l'avvio fallisce se qualcuno prova a forzarlo.

## 3. Requisiti tecnici

- **RT-1 — Avvio locale (§15).** La mappa servizio → identificativo → porta → schema si deriva dal solo file
  `application.properties`. Nessuna riga incollata in `dev/lib/services.sh` o negli script di avvio: se venisse
  voglia di farlo, è un difetto della scoperta automatica.
- **RT-2 — Isolamento fra account (§1).** I dati di prova sono creati per **due** account diversi, così che le
  prove di isolamento abbiano da subito materiale vero su cui girare.
- **RT-3 — Prove (§11).** I dati di prova sono inventati e deterministici: nessun dato reale, mai. Il canale
  simulato è la controparte usata dalle prove di integrazione e dal percorso end-to-end.
- **RT-4 — Dati personali (§10).** Nessun dato personale reale: i contatti di prova sono inventati. Nessuna
  voce nuova nel manifesto.
- **RT-5 — Registrazione eventi (§14).** Il canale simulato registra ciò che «invierebbe» con `tenant_id`,
  `app_id` e identificativo di correlazione, **senza** il corpo del messaggio.
- **RT-6 — Modulo frontend (§3).** Il modulo è abilitato nello stub locale dell'abilitazione, così che sia
  visibile subito dopo l'unione del ramo.

## 4. Criteri di accettazione

**CA-1 — L'app parte da sola**
- **Dato** un repository appena clonato
- **Quando** si eseguono `./dev.sh services` e `./app-start.sh`
- **Allora** ChatGrove compare nella mappa e risponde sulla porta `8105`, senza alcun passo manuale

**CA-2 — Il negozio finto c'è**
- **Dato** lo stack locale avviato con i dati di prova
- **Quando** si apre il modulo nel backoffice
- **Allora** si vedono le tre conversazioni aperte, i dodici prodotti e i due ordini

**CA-3 — Messaggio in arrivo simulato**
- **Dato** il canale simulato attivo
- **Quando** si inietta un messaggio in arrivo da un numero inventato
- **Allora** compare una conversazione nuova con quel messaggio, senza che nessuna chiamata sia uscita verso
  la rete

**CA-4 — Il simulatore non esce dal locale**
- **Dato** un profilo di spedizione (non locale, non di prova)
- **Quando** si tenta di attivare il canale simulato
- **Allora** l'avvio fallisce con un errore esplicito, invece di partire con un finto canale in produzione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend e smoke; l'intera suite prima del commit);
- [ ] prove di **unità** sul caricamento dei dati di prova e di **integrazione** sul canale simulato;
- [ ] prova di **isolamento fra account**: i dati di prova coprono due account;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`; questa storia però consegna **le fondamenta** su cui
      quel percorso girerà (canale simulato e dati deterministici);
- [ ] **traduzioni**: non applicabile (nessun testo visibile nuovo);
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con la forma del canale simulato e il perché non si usa un
      fornitore vero in locale;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali — **è l'oggetto stesso della storia**;
- [ ] `run-tests.sh` aggiornato se l'area di collaudo cambia comando.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | I dati di prova hanno bisogno delle tabelle |
| `0003` | Servono le schermate in cui vederli |
| `0004` | I dati di prova comprendono un account vicino al tetto della quota |

## 7. Fuori ambito

- la connessione a un canale **vero**: storia `0006`;
- il percorso end-to-end: storia `0029`.

## 8. Punti aperti

- Nessuno.
