# 0006 — Servizi a fasi con tempi di posa

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 02 — Servizi del salone e agenda a fasi
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un salone
> voglio descrivere un colore come lo faccio davvero — venti minuti di applicazione, trentacinque di posa,
> venticinque di finitura — dicendo in quali di questi minuti l'operatore è occupato e in quali no
> così da smettere di bloccare un'ora e venti di agenda per un'ora e venti di poltrona ma quarantacinque minuti
> di persona.

**Contesto.** È la storia che rende SalonGrove diverso da un'agenda. Le fonti di settore descrivono come funzione
di riferimento le impostazioni di *tempo di posa* che permettono di «gestire la doppia prenotazione durante servizi
come il colore» (§2.5 della descrizione). Oggi in BookGrove un servizio ha una durata e due tempi di preparazione,
e la prenotazione è un blocco continuo: modello giusto per un'officina, sbagliato per un salone. Questa storia
descrive **il servizio a fasi**; la storia `0007` insegna al motore a usarlo.

## 2. Requisiti funzionali

1. **RF-1** — Un servizio può avere da una a sei **fasi** in ordine, ciascuna con un nome, una durata in minuti e
   l'indicazione di quali risorse impegna.
2. **RF-2** — Ogni fase dichiara se l'**operatore è occupato** durante quella fase e se la **postazione è
   occupata**: una posa tipica ha operatore libero e postazione occupata.
3. **RF-3** — Un servizio senza fasi si comporta esattamente come oggi — un blocco unico per tutta la durata:
   nessun salone è obbligato a configurare le fasi per usare il programma.
4. **RF-4** — La somma delle durate delle fasi è la durata del servizio, mostrata come tale nel catalogo e nella
   pagina pubblica: chi prenota vede «un'ora e venti», non tre pezzi.
5. **RF-5** — Il catalogo dei servizi mostra, accanto alla durata totale, i **minuti di operatore** effettivamente
   impegnati: è il numero che dice quanto costa davvero quel servizio in ore di lavoro.
6. **RF-6** — Una fase con operatore libero **e** postazione libera non è ammessa: sarebbe un buco, e i buchi si
   fanno con i tempi di preparazione che l'agenda ha già.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle fasi filtra per `tenant_id` dal token
  verificato; un `tenant_id` che arrivasse dalla richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|PUT /api/<app>/v1/servizi/{id}/fasi` e
  `GET /api/<app>/v1/servizi` arricchito con durata totale e minuti di operatore; corpo validato (ordine senza
  buchi, durate positive, almeno una risorsa impegnata per fase); errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Tabella `fase_servizio` con `tenant_id`, UUID versione 7, colonne di controllo e
  cancellazione logica; vincolo di unicità su (servizio, ordine).
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione «Servizi», la scheda del servizio mostra le fasi come una
  striscia proporzionale ai minuti, con l'indicazione visiva di quando l'operatore è libero. Solo token del
  sistema di design, tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi delle fasi predefiniti («applicazione», «posa», «finitura»), etichette e
  messaggi di errore in `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** **Nessun dato personale nuovo**: una fase descrive un servizio, non una persona.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: la lettura del catalogo passa dagli
  strumenti dell'epica 07.
- **RT-8 — Registrazione eventi (§14).** `fasi del servizio modificate` con `tenant_id`, `app_id`, `user_id` e
  correlazione.

## 4. Criteri di accettazione

**CA-1 — Un colore descritto per fasi**
- **Dato** un servizio «Colore» senza fasi
- **Quando** si definiscono tre fasi — applicazione 20′ con operatore e postazione occupati, posa 35′ con
  operatore libero e postazione occupata, finitura 25′ con entrambi occupati
- **Allora** il servizio risulta di 80 minuti totali e **45 minuti di operatore**

**CA-2 — Il servizio senza fasi continua a funzionare**
- **Dato** un servizio «Taglio» da 45 minuti senza fasi
- **Quando** lo si legge e lo si prenota
- **Allora** si comporta come un blocco unico da 45 minuti, esattamente come prima

**CA-3 — Fase priva di senso rifiutata**
- **Dato** il modulo delle fasi
- **Quando** si salva una fase con operatore libero e postazione libera
- **Allora** l'errore è chiaro e spiega che una fase deve impegnare qualcosa, e nulla viene salvato

**CA-4 — Isolamento fra account**
- **Dato** due account con un servizio ciascuno che si chiama «Colore»
- **Quando** un utente del primo legge o modifica le fasi forzando l'identificativo del servizio dell'altro
- **Allora** riceve una risposta di non trovato, indistinguibile da quella di un identificativo inesistente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; suite intera prima del commit);
- [ ] prove di **unità** sul calcolo di durata totale e minuti di operatore, di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** su lettura e scrittura delle fasi;
- [ ] **prova end-to-end**: *rimando* — primo passo del percorso `[J-SALONGROVE]` della storia `0030`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni**: massimo sei fasi, servizio senza fasi come blocco unico, divieto della fase
      vuota, scelta di mostrare al cliente finale solo la durata totale;
- [ ] avvio locale invariato; il salone di prova ha almeno un servizio a fasi.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | la tabella delle fasi |
| storia `0003` | la sezione «Servizi» del modulo |
| catalogo dei servizi di BookGrove (storia `0006` di quell'app) | le fasi descrivono un servizio che deve già esistere |

## 7. Fuori ambito

- il calcolo degli spazi liberi che tiene conto delle fasi: storia `0007`, ed è dove sta la difficoltà vera;
- le varianti per cliente (lunghezza, ricrescita): storia `0008`;
- il consumo di prodotto per fase: epica 04.

## 8. Punti aperti

**Sotto la via (b) questa storia modifica un'entità di BookGrove.** Il servizio a fasi non è un'aggiunta laterale:
cambia il significato di «durata» per tutte le applicazioni che poggiano sull'agenda. La proposta è che sia una
**capacità del motore di BookGrove** — utile anche a un'officina e a un ambulatorio — e non una particolarità del
beauty (§0.3 della descrizione, punto 3). Se lo sviluppatore la vuole confinata, va detto qui, perché cambia dove
vive la tabella.
