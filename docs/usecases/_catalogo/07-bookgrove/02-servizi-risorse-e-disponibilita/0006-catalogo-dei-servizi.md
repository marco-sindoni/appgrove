# 0006 — Catalogo dei servizi prenotabili

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 02 — Servizi, risorse e disponibilità
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'attività che lavora su appuntamento
> voglio scrivere una volta per tutte cosa vendo e quanto tempo occupa
> così da non dover ricalcolare a mente la durata ogni volta che qualcuno chiede un appuntamento.

**Contesto.** Il servizio è la prima delle tre cose da cui nasce la disponibilità (le altre due sono la risorsa e
l'orario). Contiene un'informazione che il quaderno non ha mai: il **tempo di preparazione** prima e dopo. Una
piega dura quaranta minuti ma occupa la poltrona per cinquanta, perché dopo va pulita; una visita dura venti
minuti ma prima ne servono cinque per preparare la sala. Senza questa distinzione l'agenda si riempie di
appuntamenti che nella realtà si accavallano.

## 2. Requisiti funzionali

1. **RF-1** — Si crea, modifica e archivia un servizio con: nome, descrizione breve, durata, tempo di
   preparazione prima, tempo di riordino dopo, prezzo indicativo e valuta.
2. **RF-2** — Un servizio si marca **visibile al pubblico** oppure **solo interno**: ci sono prestazioni che si
   prenotano solo dal banco.
3. **RF-3** — Un servizio archiviato non è più prenotabile ma resta leggibile sulle prenotazioni passate, che non
   devono perdere il proprio significato.
4. **RF-4** — I servizi si ordinano e si raggruppano per categoria libera (per esempio «colore», «taglio»,
   «visite»), perché un elenco di trenta voci senza gruppi è inusabile.
5. **RF-5** — Il prezzo è **indicativo** e dichiarato tale: serve a informare chi prenota, non è un documento
   commerciale e non genera niente a valle.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dei `servizio` filtra per `tenant_id` preso
  dal token verificato; un `tenant_id` che arrivasse dalla richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/prenotazioni/v1/servizi`,
  `GET|PUT|DELETE /api/prenotazioni/v1/servizi/{id}`; corpo validato (durata positiva, tempi non negativi, prezzo
  non negativo); errori in `problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V2__servizi.sql` sullo schema `app_prenotazioni`: tabella `servizio`
  con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Servizi e risorse» del modulo `prenotazioni`, con elenco,
  raggruppamento e modulo di inserimento (React Hook Form più Zod); dati letti con il client generato; solo token
  del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili dallo spazio-nomi `prenotazioni`, presenti in
  `en, it, fr, es, de`. **Attenzione**: il *nome del servizio* lo scrive il cliente e resta nella sua lingua; a
  tradursi è l'interfaccia, non il contenuto.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: un servizio descrive una prestazione, non una
  persona. **Ma** il nome del servizio è testo libero e, se l'attività è sanitaria, diventa il dato che rende
  particolare la prenotazione: il collegamento è nella storia `0012` e nell'avviso del §6 della descrizione.
- **RT-7 — Registrazione eventi (§14).** `servizio creato`, `servizio archiviato` con `tenant_id`, `app_id`,
  `user_id` e correlazione.

## 4. Criteri di accettazione

**CA-1 — Creazione di un servizio**
- **Dato** un utente con ruolo sufficiente
- **Quando** crea il servizio «Piega» con durata 40 minuti e riordino 10 minuti
- **Allora** il servizio compare nell'elenco e l'occupazione totale calcolata è 50 minuti

**CA-2 — Visibilità al pubblico**
- **Dato** un servizio marcato «solo interno» · **Quando** si guarda l'elenco dei servizi pubblicabili · **Allora**
  non c'è, e non comparirà sulla pagina pubblica

**CA-3 — Archiviazione senza perdita di storia**
- **Dato** un servizio con prenotazioni passate · **Quando** lo si archivia · **Allora** non è più prenotabile ma
  le prenotazioni passate continuano a mostrarne il nome

**CA-4 — Validazione**
- **Dato** il modulo di inserimento · **Quando** si mette durata zero o negativa · **Allora** l'errore è chiaro,
  in `problem+json` dal servizio e a schermo dal modulo, e nulla viene salvato

**CA-5 — Isolamento fra account**
- **Dato** due account con i propri servizi · **Quando** un utente di uno chiede l'elenco forzando
  l'identificativo dell'altro · **Allora** vede solo i propri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sul calcolo dell'occupazione totale e di **integrazione** sulla risorsa `servizi`;
- [ ] prova di **isolamento fra account** sulla risorsa introdotta;
- [ ] **prova end-to-end**: *rimando* — è il primo passo del percorso `[J-BOOKGROVE]` creato dalla storia `0033`,
      dove si aggiorna [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la ragione scritta;
- [ ] **registro delle decisioni** compilato: tempi di preparazione separati dalla durata, e perché;
- [ ] avvio locale invariato; i dati di prova comprendono quattro servizi di durata diversa;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | schema e isolamento |
| storia `0003` | la sezione del modulo in cui mettere la schermata |

## 7. Fuori ambito

- chi eroga il servizio: storia `0007`;
- listini differenziati per cliente e sconti: non sono di questa app, sono dell'applicazione 06;
- il pacchetto di più servizi in un solo appuntamento: rimandato, lo raccoglie il verticale che ne ha bisogno.

## 8. Punti aperti

**Servizi a durata variabile.** Un colore può durare 60 o 120 minuti a seconda della testa. Le vie sono due:
durate multiple per lo stesso servizio, oppure una durata predefinita che l'operatore corregge sull'appuntamento.
La seconda è più semplice e copre quasi tutto: proposta, da confermare con la storia `0014`.
