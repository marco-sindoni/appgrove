# 0028 — Scrittura degli appuntamenti sul calendario

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 06 — Sincronizzazione con i calendari esterni
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore
> voglio ritrovare i miei appuntamenti di lavoro nel calendario che guardo tutti i giorni
> così da vedere la mia giornata intera in un posto solo, anche quando sono fuori.

**Contesto.** È la metà più semplice della sincronizzazione, e quella che l'operatore percepisce subito. Ha però
un risvolto di riservatezza che non si vede a prima vista: il calendario personale può essere condiviso con
colleghi, familiari o con il proprio datore di lavoro, e scriverci dentro «Mario Rossi — visita dermatologica»
significa esporre il dato di un cliente a persone che non c'entrano nulla. Per questo il titolo predefinito è
minimizzato.

## 2. Requisiti funzionali

1. **RF-1** — Una prenotazione confermata su una risorsa con collegamento in scrittura compare come evento sul
   calendario esterno di quell'operatore.
2. **RF-2** — Il titolo predefinito è **minimizzato**: nome dell'attività e ora, senza il nome del cliente e
   senza il nome del servizio. L'operatore può scegliere titoli più espliciti, con un avviso su cosa comporta.
3. **RF-3** — Spostamento e disdetta aggiornano o rimuovono l'evento esterno; nessun evento resta appeso dopo una
   disdetta.
4. **RF-4** — Gli eventi creati dall'app sono riconoscibili e non vengono confusi con gli impegni personali
   dell'operatore.
5. **RF-5** — Se la scrittura fallisce, l'app riprova per un tempo limitato e poi lo dice; **la prenotazione resta
   valida in ogni caso**, perché il calendario esterno è una comodità, non la fonte di verità.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La scrittura avviene solo verso il calendario collegato alla risorsa di
  quell'account; nessuna scrittura incrociata è possibile.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova: la sincronizzazione è una
  lavorazione interna; lo stato per prenotazione è leggibile dalla rotta della prenotazione. OpenAPI aggiornata
  dove cambia.
- **RT-3 — Persistenza (§8).** Migrazione `V19__eventi_esterni.sql`: tabella di corrispondenza fra prenotazione ed
  evento esterno, con `tenant_id`, UUID versione 7, colonne di controllo, identificativo dell'evento presso il
  fornitore e momento dell'ultima sincronizzazione.
- **RT-4 — Idempotenza e assenza di cicli.** Ogni scrittura porta una chiave che ne impedisce la ripetizione;
  l'evento scritto dall'app è marcato come proprio, così che la lettura della storia `0029` non lo riconsideri
  come impegno esterno e non generi un ciclo.
- **RT-5 — Asincronia.** La scrittura non sta sul percorso della prenotazione: se il fornitore è lento, la
  prenotazione si conferma lo stesso.
- **RT-6 — Modulo frontend (§3, §5).** Lo stato della sincronizzazione è visibile sulla prenotazione, non
  invadente; solo token del sistema di design; tema chiaro e scuro.
- **RT-7 — Cinque lingue (§4).** Gli avvisi in `en, it, fr, es, de`; il titolo dell'evento segue la lingua
  dell'operatore.
- **RT-8 — Dati personali (§10).** Il fornitore del calendario **riceve** il fatto che a una certa ora c'è un
  impegno e, se l'operatore lo sceglie, anche nome del cliente e del servizio: la scelta va dichiarata nel
  manifesto in italiano e inglese, con l'avvertenza che un titolo esplicito trasferisce a un fornitore esterno un
  dato che può essere particolare (§6 della descrizione).
- **RT-9 — Registrazione eventi (§14).** `evento esterno creato`, `aggiornato`, `rimosso`, `scrittura fallita` con
  `tenant_id`, `app_id`, fornitore e correlazione — mai il titolo dell'evento.

## 4. Criteri di accettazione

**CA-1 — L'appuntamento compare**
- **Dato** una risorsa con collegamento in scrittura · **Quando** si conferma una prenotazione · **Allora**
  compare un evento sul calendario esterno all'ora giusta, con la durata giusta

**CA-2 — Titolo minimizzato**
- **Dato** le impostazioni predefinite · **Quando** si guarda l'evento esterno · **Allora** non contiene né il
  nome del cliente né il nome del servizio

**CA-3 — Disdetta**
- **Dato** un appuntamento sincronizzato · **Quando** viene disdetto · **Allora** l'evento esterno sparisce

**CA-4 — Fallimento senza danni**
- **Dato** un fornitore irraggiungibile · **Quando** si conferma una prenotazione · **Allora** la prenotazione è
  confermata lo stesso, e lo stato della sincronizzazione dice che è in attesa

**CA-5 — Nessun ciclo**
- **Dato** un evento scritto dall'app · **Quando** la lettura degli impegni esterni gira · **Allora** quell'evento
  non viene riconsiderato come impegno personale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `compliance`);
- [ ] prove di **unità** sull'idempotenza e di **integrazione** con fornitore simulato, compreso il fallimento;
- [ ] prova di **isolamento fra account** sulla scrittura;
- [ ] **prova end-to-end**: *rimando* — fornitore simulato; motivo e storia proprietaria dichiarati in
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con ciò che il fornitore del calendario riceve;
- [ ] **registro delle decisioni** compilato: **titolo minimizzato per impostazione predefinita** e perché;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0027` | serve il collegamento autorizzato |

## 7. Fuori ambito

- leggere gli impegni esterni: storia `0029`;
- la modifica di un appuntamento fatta **dentro** il calendario esterno: **deliberatamente non supportata**, e
  dichiarata a schermo. Riportarla indietro significherebbe accettare che la fonte di verità sia il calendario
  esterno, cosa che questa app non fa.

## 8. Punti aperti

Nessuno.
