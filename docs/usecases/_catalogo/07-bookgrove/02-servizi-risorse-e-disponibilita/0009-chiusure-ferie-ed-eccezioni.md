# 0009 — Chiusure, ferie ed eccezioni

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 02 — Servizi, risorse e disponibilità
**Storia**: `0009` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che chiude due settimane ad agosto e ha un operatore in malattia da giovedì
> voglio togliere quei giorni dall'agenda con due clic
> così da non ricevere prenotazioni per un giorno in cui non c'è nessuno.

**Contesto.** L'orario settimanale dice la regola; le chiusure dicono le eccezioni, e in un'attività piccola le
eccezioni sono all'ordine del giorno. È una storia breve ma con un caso spinoso: cosa succede alle prenotazioni
**già prese** nel periodo che si chiude. Ignorarlo significherebbe che il cliente si presenta a saracinesca
abbassata.

## 2. Requisiti funzionali

1. **RF-1** — Si inserisce una chiusura con inizio, fine e motivo breve, riferita a **una risorsa** oppure
   all'**intera sede**.
2. **RF-2** — Una chiusura sottrae disponibilità: negli intervalli chiusi non compaiono spazi liberi.
3. **RF-3** — Se nel periodo che si sta chiudendo esistono prenotazioni confermate, il programma **le elenca prima
   di salvare** e chiede cosa farne, invece di salvare e lasciarle lì.
4. **RF-4** — Le chiusure si vedono in agenda come fasce marcate, distinguibili a colpo d'occhio dal tempo
   semplicemente non disponibile.
5. **RF-5** — Esiste anche l'eccezione opposta: un'**apertura straordinaria** in un giorno normalmente chiuso.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle `chiusura` filtra per `tenant_id` preso
  dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/prenotazioni/v1/chiusure` e
  `DELETE /api/prenotazioni/v1/chiusure/{id}`; il salvataggio accetta un parametro esplicito che dichiara di aver
  visto le prenotazioni in conflitto; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V5__chiusure.sql`: tabella `chiusura` con `tenant_id`, UUID versione 7,
  colonne di controllo, cancellazione logica e un indicatore di segno (sottrae o aggiunge disponibilità).
- **RT-4 — Modulo frontend (§3, §5).** Inserimento dalla vista agenda, per trascinamento su un intervallo o da
  modulo; elenco delle chiusure future nelle impostazioni; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Interfaccia e messaggi in `en, it, fr, es, de`; il **motivo** è testo libero
  scritto dal cliente e non si traduce.
- **RT-6 — Dati personali (§10).** Il campo motivo è testo libero e può contenere informazioni su una persona
  («Anna in malattia»): si dichiara nel manifesto in italiano e inglese come testo libero riferito al personale,
  si annota `@PersonalData` e la tabella entra in `exportData` e `purgeData`. È un caso in cui è facile
  dimenticarsene proprio perché il campo sembra innocuo.
- **RT-7 — Registrazione eventi (§14).** `chiusura inserita`, `chiusura rimossa` con `tenant_id`, `app_id`,
  `user_id` e correlazione — **mai il motivo**.

## 4. Criteri di accettazione

**CA-1 — Ferie della sede**
- **Dato** una sede aperta tutti i giorni · **Quando** si inserisce una chiusura dal 10 al 24 agosto · **Allora**
  in quei giorni non c'è nessuno spazio libero per nessuna risorsa

**CA-2 — Assenza di una sola risorsa**
- **Dato** due risorse · **Quando** si chiude solo la prima per giovedì · **Allora** giovedì la seconda resta
  prenotabile

**CA-3 — Prenotazioni in conflitto**
- **Dato** tre prenotazioni confermate nel periodo che si sta chiudendo
- **Quando** si prova a salvare la chiusura
- **Allora** il programma le elenca e chiede conferma esplicita; senza conferma non salva nulla

**CA-4 — Apertura straordinaria**
- **Dato** una domenica normalmente chiusa · **Quando** si inserisce un'apertura straordinaria 10-13 · **Allora**
  in quella fascia compaiono spazi liberi

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** uno chiede l'elenco delle chiusure forzando l'identificativo dell'altro
- **Allora** vede solo le proprie

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sulla sottrazione della disponibilità e di **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account** sulla risorsa introdotta;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-BOOKGROVE]` della storia `0033`, dove si aggiorna
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con il campo motivo, annotato, e la tabella presente
      in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato: il comportamento in caso di prenotazioni in conflitto;
- [ ] avvio locale invariato; i dati di prova comprendono una chiusura;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | la chiusura è un'eccezione a una regola che deve esistere |

## 7. Fuori ambito

- avvisare i clienti le cui prenotazioni cadono nella chiusura: la disdetta dall'attività e il messaggio sono
  della storia `0015`, il messaggio automatico della `0022`;
- le festività precaricate: punto aperto della storia `0008`.

## 8. Punti aperti

Nessuno.
