# 0017 — Registrazione dichiarativa delle ore

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 04 — Ore lavorate e fatturabilità
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0004`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore che ha passato la mattina sul cantiere Rossi
> voglio scrivere «3 ore, cantiere Rossi, posa cavi» e basta
> così da chiudere la questione in dieci secondi, senza sentirmi controllato.

**Contesto.** È la storia che decide se FlowGrove ha una ragione di esistere. Le ore sono il ponte fra il lavoro e
il denaro: senza di esse non c'è consuntivo, non c'è margine, non c'è fattura. Ed è anche la storia più delicata
dell'intera applicazione, perché registra dati **sull'attività lavorativa di una persona**.

La scelta di fondo è **dichiarativa, non rilevativa**, ed è scritta qui come requisito perché non venga erosa
storia dopo storia. La ragione è duplice. Giuridica: l'articolo 4 dello Statuto dei lavoratori distingue gli
strumenti con cui il lavoratore rende la prestazione (comma 2) dagli strumenti di controllo (comma 1, che
richiede accordo sindacale o autorizzazione dell'ispettorato); una funzione che *rileva* — posizione, schermate,
inattività — sposta lo strumento dalla prima categoria alla seconda, e il precedente da 50.000 € del Garante sul
caso ARSAC mostra che nemmeno consenso e accordo sindacale bastano quando mancano base giuridica, proporzionalità
e valutazione d'impatto ([application-description.md](../application-description.md) §2.3). Pratica: uno
strumento percepito come sorveglianza viene compilato male, e ore false sono peggio di nessuna ora.

## 2. Requisiti funzionali

1. **RF-1** — Una riga di ore ha: data di competenza, durata, attività, autore, nota facoltativa. L'autore è
   **sempre** chi sta scrivendo: nessuno registra ore a nome di un altro.
2. **RF-2** — Le ore si dichiarano a posteriori, per giornata: la riga non contiene orario di inizio, orario di
   fine, pause, posizione geografica né alcun dato rilevato dal dispositivo.
3. **RF-3** — Esiste un cronometro come **scorciatoia di inserimento**, avviato e fermato dalla persona; ciò che
   resta scritto è comunque una riga di ore modificabile. Il cronometro non parte da solo, non resta acceso in
   sottofondo e non registra nulla mentre è fermo.
4. **RF-4** — L'autore può modificare e cancellare le proprie righe finché il periodo è aperto (stato `aperta`);
   quando il periodo si chiude (storia 0020) le righe diventano `bloccate`.
5. **RF-5** — La schermata dice **in chiaro** a chi scrive che cosa vedrà il responsabile: le sue ore per
   progetto e per periodo, non una ricostruzione della sua giornata.
6. **RF-6** — La prima riga di ore di una persona **occupa un posto** della quota; a tetto raggiunto la risposta è
   `429` con il rimedio (storia 0004).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `time_entry` filtra per `tenant_id` dal
  token verificato. In più: la scrittura impone `user_id = sub` del token; un `user_id` che arrivasse dal corpo
  della richiesta viene **ignorato**, non solo respinto.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/progetti/v1/time-entries`,
  `PATCH|DELETE /api/progetti/v1/time-entries/{id}`, `GET /api/progetti/v1/me/time-entries?dal=&al=`; corpo
  validato (durata positiva, sotto un massimo giornaliero plausibile, data non nel futuro); errori in
  `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V10__ore.sql`: `time_entry` con `tenant_id`, `task_id`, `user_id`,
  `work_date`, `minutes`, `billable`, `note`, `status`, colonne di controllo e cancellazione logica; indice
  `(tenant_id, user_id, work_date)` e `(tenant_id, task_id, work_date)`. **Nessuna colonna** di orario di
  inizio/fine, posizione o dispositivo: è un vincolo di modello.
- **RT-4 — Modulo frontend (§3, §5).** Inserimento rapido dalla riga di *Le mie attività* (storia 0013) e dalla
  scheda dell'attività; nota di trasparenza sempre visibile; solo token del sistema di design; tema chiaro e
  scuro; funziona su schermo stretto.
- **RT-5 — Cinque lingue (§4).** Etichette, messaggi di validazione e **nota di trasparenza** in
  `en, it, fr, es, de`. La nota di trasparenza è un testo giuridicamente rilevante: va tradotta con cura, non
  automaticamente.
- **RT-6 — Varchi e quota (§6, §7).** Prima della prima riga di una persona il servizio prenota un posto sulla
  metrica `seats` (natura `stock`); a tetto raggiunto risponde `429`. Con abbonamento `past_due` la registrazione
  resta possibile; con `canceled` risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `log_time(attività, data, durata, fatturabile?, nota?)`,
  **scrittura con bozza e conferma umana**, con il vincolo aggiuntivo che scrive **solo a nome di chi chiama**
  (storia 0029).
- **RT-8 — Dati personali (§10).** È la voce più delicata del manifesto: `time_entry.user_id`, `work_date`,
  `minutes` e `note` vanno dichiarati in italiano e inglese come dati sull'attività lavorativa di un dipendente,
  con finalità (consuntivo e fatturazione), base giuridica e conservazione; campi annotati `@PersonalData`;
  tabella in `exportData` e `purgeData`. La nota è **testo libero**: dichiararlo.
- **RT-9 — Registrazione eventi (§14).** «Ore registrate», «ore modificate», «registrazione respinta per quota»
  con `tenant_id`, `app_id`, `user_id`, identificativo dell'attività e durata; **mai** il testo della nota.

## 4. Criteri di accettazione

**CA-1 — Registrazione**
- **Dato** una persona con un'attività assegnata
- **Quando** dichiara 3 ore sulla data di ieri
- **Allora** la riga esiste in stato `aperta`, con lei come autore, e il totale del giorno si aggiorna

**CA-2 — Nessuna scrittura a nome di altri**
- **Dato** una persona `X`
- **Quando** invia una registrazione indicando nel corpo della richiesta l'identificativo di `Y`
- **Allora** la riga viene creata a nome di `X`: l'identificativo passato è ignorato

**CA-3 — Correzione entro il periodo aperto**
- **Dato** una riga in stato `aperta`
- **Quando** l'autore la corregge da 3 a 2 ore
- **Allora** la modifica riesce e resta traccia della correzione

**CA-4 — Validazione**
- **Dato** una registrazione con data nel futuro oppure durata di 30 ore in un giorno
- **Quando** si invia
- **Allora** la risposta è `422` con il motivo, e nulla viene scritto

**CA-5 — Quota esaurita**
- **Dato** un account con tutti i posti occupati
- **Quando** una persona senza posto dichiara le sue prime ore
- **Allora** riceve `429` con il rimedio e nulla viene scritto

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` dichiara ore su un'attività di `B`
- **Allora** riceve `404` e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sulla validazione e di **integrazione** sulla registrazione, compreso il caso in cui si
      tenti di forzare l'autore;
- [ ] prova di **isolamento fra account** su tutte le rotte introdotte;
- [ ] **prova end-to-end**: coprire ora — la dichiarazione delle ore è il passo centrale di `[J-PROGETTI]`
      (storia 0031); voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con la nota di trasparenza revisionata;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `time_entry`, con la conservazione dichiarata;
- [ ] **registro delle decisioni** compilato, con annotata per esteso la scelta **dichiarativo contro
      rilevativo** e le fonti che la motivano;
- [ ] controllo automatico di **accessibilità** verde sull'inserimento rapido;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0004` | La prima riga di ore occupa un posto |
| Storia `0012` | Le ore si dichiarano su attività, tipicamente assegnate |
| Storia `0013` | È il posto da cui si dichiarano le ore nella pratica quotidiana |

## 7. Fuori ambito

- la tariffa e la distinzione fatturabile/non fatturabile: storia 0018 (qui il campo esiste, il calcolo no);
- il foglio ore settimanale: storia 0019;
- il blocco del periodo: storia 0020;
- **qualunque forma di rilevazione automatica**: fuori perimetro dell'applicazione, per sempre e per scelta.

## 8. Punti aperti

- **Conservazione delle righe di ore**: la durata dichiarata nel manifesto va confermata dallo sviluppatore
  insieme alla revisione legale ([application-description.md](../application-description.md) §11.5). Questa storia
  non si chiude senza un valore scritto: «da definire» nel manifesto non è ammesso.
- **Qualificazione rispetto all'articolo 4 dello Statuto dei lavoratori**: chi qualifica è il cliente, che è il
  titolare del trattamento verso i propri lavoratori. L'app deve **mettere in condizione** il cliente di
  informare i propri collaboratori, e non può decidere al suo posto. Il testo dell'avviso in prodotto va
  rivisto da un legale prima del go-live.
