# 0024 — Esportazione dell'agenda in formato calendario

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 04 — Attività e storico della relazione
**Storia**: `0024` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0020`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore che vive dentro il proprio calendario
> voglio portare le attività di LeadGrove nel calendario che uso già
> così da non dover guardare in due posti per sapere cosa ho da fare.

**Contesto.** La sincronizzazione viva con i calendari esterni è l'integrazione più richiesta del dominio ed è
**fuori perimetro** ([application-description.md](../application-description.md) §11.3), perché introdurrebbe un
responsabile esterno del trattamento. Questa storia è il compromesso onesto: un file in formato calendario
standard, scaricato dall'utente. Niente fornitori, niente accessi delegati, e il novanta per cento del beneficio
per chi ha un calendario che sa importare un file.

## 2. Requisiti funzionali

1. **RF-1** — L'utente può scaricare le proprie attività aperte in un file nel formato calendario standard
   (`.ics`), con titolo, momento, durata e un rimando alla scheda d'origine.
2. **RF-2** — L'esportazione riguarda **solo** le attività di cui l'utente è responsabile, salvo che abbia ruolo
   `owner` o `admin` e chieda esplicitamente quelle della squadra.
3. **RF-3** — Il file non contiene recapiti dei contatti: il titolo dell'evento riporta il riferimento (nome
   dell'azienda o del contatto) e nulla di più.
4. **RF-4** — Prima dello scaricamento l'app avverte che il file contiene dati di persone e che, una volta fuori,
   la loro protezione dipende da chi lo custodisce.
5. **RF-5** — L'esportazione è tracciata: chi, quando, quante attività.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'esportazione comprende solo attività dell'account del token verificato
  e, di norma, solo quelle del richiedente.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/sales/v1/activities/export.ics`; risposta con
  tipo di contenuto del calendario; errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; la traccia dell'esportazione usa il registro delle
  esportazioni introdotto dalla storia 0027, che va quindi rilasciata prima o insieme.
- **RT-4 — Modulo frontend (§3, §5).** Azione «Esporta nel calendario» nell'agenda, con la finestra di avviso;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette e avviso in `en, it, fr, es, de`; i titoli degli eventi dentro il file
  restano nella lingua in cui l'attività è stata scritta.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. L'esportazione resta accessibile anche con
  abbonamento in `past_due`; i **diritti dell'interessato** restano accessibili in ogni caso, anche con
  abbonamento `canceled` — ma questa esportazione è una comodità, non un diritto, quindi con `canceled` risponde
  `402`.
- **RT-7 — Esposizione conversazionale (§12).** Non esposta alla chat: produce un file, e un file prodotto da un
  assistente senza che nessuno lo guardi è dati che escono senza controllo.
- **RT-8 — Dati personali (§10).** Il file contiene dati di persone (nomi nei titoli): è una **uscita di dati**
  dall'app. Nessuna voce nuova nel manifesto, ma va dichiarato nel registro delle esportazioni. Nessun fornitore
  esterno coinvolto: è ciò che distingue questa storia dalla sincronizzazione viva.
- **RT-9 — Registrazione eventi (§14).** «Agenda esportata» con `tenant_id`, `app_id`, `user_id`, numero di
  attività e identificativo di correlazione; **mai** i titoli.

## 4. Criteri di accettazione

**CA-1 — File valido**
- **Dato** un venditore con tre attività aperte
- **Quando** esporta l'agenda
- **Allora** ottiene un file di calendario con tre eventi, che un'applicazione di calendario apre senza errori

**CA-2 — Solo le proprie**
- **Dato** un account con tre venditori
- **Quando** un `member` esporta
- **Allora** il file contiene solo le sue attività

**CA-3 — Nessun recapito**
- **Dato** un'attività riferita a un contatto con indirizzo e telefono
- **Quando** si ispeziona il file
- **Allora** non contiene né indirizzo di posta né numero di telefono

**CA-4 — Avviso mostrato**
- **Dato** l'azione di esportazione
- **Quando** l'utente la avvia
- **Allora** vede l'avviso sui dati di persone prima che il file venga prodotto

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` esporta
- **Allora** il file non contiene nulla di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione del formato calendario e di **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account** sull'esportazione;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo; coperta da prove d'integrazione, con il motivo nel
      registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, ma l'uscita è dichiarata nel registro delle esportazioni;
- [ ] **registro delle decisioni** compilato, con annotato perché si esporta un file invece di sincronizzare;
- [ ] contratto degli **strumenti conversazionali**: non esposta, con la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sull'azione e sull'avviso;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0019`, `0020` | Servono le attività e l'agenda da cui si esporta |
| Storia `0027` | Il registro delle esportazioni nasce lì: va rilasciata prima o insieme |

## 7. Fuori ambito

- la sincronizzazione viva con Google Calendar o Microsoft 365: punto aperto 3 della descrizione
  dell'applicazione;
- l'abbonamento a un indirizzo di calendario sempre aggiornato: sarebbe un indirizzo pubblico contenente dati di
  persone, e va valutato a parte;
- l'importazione di eventi dal calendario verso LeadGrove: non prevista.

## 8. Punti aperti

- **Indirizzo di calendario permanente.** Molti prodotti offrono un collegamento che il calendario interroga da
  solo. È molto comodo e molto pericoloso: un indirizzo con un segreto nell'URL che espone attività con nomi di
  persone, senza autenticazione vera. Non è previsto in questa proposta; se lo si vuole, è una decisione dello
  sviluppatore con ricaduta sulla classificazione dei dati.
