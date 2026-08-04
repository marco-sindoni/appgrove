# 0013 — Agenda multi-risorsa

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 03 — Anagrafica dei clienti e agenda interna
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi sta dietro il banco
> voglio vedere la giornata di tutte le postazioni su una schermata sola
> così da sapere in un colpo d'occhio chi è occupato, dove ci sono buchi e cosa succede fra un'ora.

**Contesto.** È la schermata su cui l'attività passa la giornata: se è lenta o confusa, l'app non viene usata,
qualunque cosa ci sia sotto. La vista giorno con una colonna per risorsa è la forma che tutti i concorrenti
adottano e che il personale riconosce senza spiegazioni. Questa storia consegna la **lettura**; scrivere in
agenda è della storia `0014`.

## 2. Requisiti funzionali

1. **RF-1** — Vista **giorno** con una colonna per risorsa attiva, fasce orarie sull'asse verticale e gli
   appuntamenti come blocchi proporzionati alla durata.
2. **RF-2** — Vista **settimana** per una singola risorsa, per la persona che vuole vedere la propria settimana.
3. **RF-3** — Il tempo non disponibile (fuori orario, chiuso, in ferie) è visivamente distinto dal tempo libero:
   sono due cose diverse e confonderle fa prendere appuntamenti impossibili.
4. **RF-4** — Ogni blocco mostra ora, servizio, nome del cliente e stato; il colore viene dalla risorsa, lo stato
   da un contrassegno.
5. **RF-5** — Si filtra per risorsa e per stato, e si salta a una data; il giorno corrente è l'apertura
   predefinita.
6. **RF-6** — Su schermo piccolo la vista giorno resta usabile: è il caso normale, perché il personale guarda
   l'agenda dal telefono.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'agenda legge solo prenotazioni dell'account del token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/prenotazioni/v1/agenda` con parametri da, a,
  risorse, stati; risposta minimizzata (per il blocco servono ora, durata, servizio, stato, nome breve del
  cliente); errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Modulo frontend (§3, §5).** Sezione «Agenda» del modulo `prenotazioni`; stato del server con TanStack
  Query, stato locale con Zustand; solo token del sistema di design, con il colore-categoria `green` e i colori
  delle risorse presi dai token; tema chiaro e scuro; nessun colore scritto a mano.
- **RT-4 — Cinque lingue (§4).** Nomi dei giorni, formati di data e ora, etichette degli stati in `en, it, fr,
  es, de`; l'ordine dei giorni della settimana segue la lingua.
- **RT-5 — Dati personali (§10).** Nessuna voce nuova nel manifesto: l'agenda mostra dati già dichiarati. Ma
  **minimizzazione**: la risposta porta il nome del cliente, non il suo telefono né la sua posta elettronica, che
  si vedono solo aprendo la prenotazione.
- **RT-6 — Accessibilità.** L'agenda è navigabile da tastiera e ogni blocco ha un'etichetta leggibile da un
  lettore di schermo; il controllo automatico di accessibilità è parte della definizione di fatto.
- **RT-7 — Registrazione eventi (§14).** Nessun evento applicativo nuovo: la lettura dell'agenda non è un fatto
  da registrare oltre alla traccia tecnica della richiesta.

## 4. Criteri di accettazione

**CA-1 — Vista giorno**
- **Dato** due risorse con quattro appuntamenti nella giornata · **Quando** si apre l'agenda · **Allora** si
  vedono due colonne, ogni appuntamento nella sua fascia, con durata proporzionata

**CA-2 — Tempo non disponibile**
- **Dato** una risorsa in ferie e una fuori orario · **Quando** si guarda la giornata · **Allora** le due
  situazioni sono distinguibili fra loro e dal tempo libero

**CA-3 — Vista settimana**
- **Dato** una risorsa · **Quando** si passa alla vista settimana · **Allora** si vedono sette giorni con i
  rispettivi appuntamenti

**CA-4 — Schermo piccolo**
- **Dato** una larghezza da telefono · **Quando** si apre l'agenda · **Allora** resta leggibile e si scorre senza
  che il contenuto esca lateralmente dalla pagina

**CA-5 — Isolamento fra account**
- **Dato** due account con appuntamenti alla stessa ora · **Quando** un utente del primo apre l'agenda
- **Allora** vede solo i propri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`, compreso il controllo dei tipi);
- [ ] prove di **unità** sulla disposizione dei blocchi e di **integrazione** sulla rotta dell'agenda;
- [ ] prova di **isolamento fra account** sulla rotta dell'agenda;
- [ ] **prova end-to-end**: **coperta ora** — l'agenda è un passo del percorso `[J-BOOKGROVE]`; se il percorso non
      esiste ancora, la storia lo prepara e la `0033` lo chiude, aggiornando
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue, compresi giorni e formati;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la ragione scritta;
- [ ] **registro delle decisioni** compilato: forma dell'agenda e minimizzazione della risposta;
- [ ] controllo automatico di **accessibilità** verde sulla schermata;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | serve sapere cosa è disponibile e cosa no |
| storia `0011` | serve il nome del cliente sul blocco |

## 7. Fuori ambito

- creare o spostare un appuntamento: storia `0014`;
- la vista mensile e le viste di riempimento: la prima non è richiesta, le seconde sono della storia `0026`.

## 8. Punti aperti

Nessuno.
