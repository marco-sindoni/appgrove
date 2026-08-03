# 0019 — Foglio ore settimanale

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 04 — Ore lavorate e fatturabilità
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore che il venerdì si ricorda di non aver segnato niente da lunedì
> voglio una griglia con le mie attività sulle righe e i giorni sulle colonne
> così da mettere a posto una settimana in mezzo minuto invece che in un quarto d'ora.

**Contesto.** È la storia con un **requisito di attrito**, che è insolito ma qui è quello che conta: se compilare
il foglio ore costa fatica, non lo compila nessuno, e senza ore l'intera epica 05 (margine, redditività) è
costruita sul vuoto. È il rischio operativo numero uno dell'app
([application-description.md](../application-description.md) §11). La griglia settimanale è la forma che l'intera
categoria ha convergentemente adottato, e non conviene inventarne un'altra.

## 2. Requisiti funzionali

1. **RF-1** — La griglia mostra una settimana: le attività su cui la persona ha dichiarato ore (o che le sono
   assegnate) sulle righe, i sette giorni sulle colonne, i totali per giorno e per riga.
2. **RF-2** — Si scrive una durata in una cella e si passa alla successiva senza toccare il puntatore; si accetta
   sia il formato decimale (`1,5`) sia quello orario (`1:30`).
3. **RF-3** — Si aggiunge una riga cercando un'attività di qualunque progetto dell'account.
4. **RF-4** — Le celle di un periodo **bloccato** (storia 0020) non sono modificabili e lo mostrano chiaramente,
   con il motivo.
5. **RF-5** — Si copia la settimana precedente come punto di partenza — struttura delle righe, **non** le durate:
   copiare anche le ore produrrebbe dichiarazioni false.
6. **RF-6** — La griglia mostra il totale della settimana e segnala i giorni con zero ore, senza giudicare: è un
   promemoria per chi scrive, non una segnalazione per il responsabile.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La griglia legge e scrive solo righe dell'account del token verificato e
  **solo dell'utente che chiama**: non esiste una griglia «di un'altra persona».
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/progetti/v1/me/timesheet?settimana=` e
  `PUT /api/progetti/v1/me/timesheet` con la scrittura **a lotto** della settimana (creazioni, modifiche e
  cancellazioni in una sola transazione); errori in `application/problem+json`; OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: la griglia scrive `time_entry`. La scrittura a lotto è
  idempotente rispetto alla settimana inviata e non deve poter duplicare righe.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Ore → Il mio foglio*; navigazione fra celle da tastiera; salvataggio
  automatico con indicazione dello stato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi dei giorni, formati di durata accettati, messaggi di blocco e di errore in
  `en, it, fr, es, de`; il primo giorno della settimana segue la lingua.
- **RT-6 — Varchi e quota (§6, §7).** La prima riga della persona occupa un posto (storia 0004): la griglia deve
  gestire il `429` senza perdere quello che l'utente ha scritto. Con abbonamento `canceled` risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dedicato alla griglia: `log_time` (storia 0029)
  copre la dichiarazione singola, ed è la forma sensata da una chat.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo rispetto alla storia 0017: la griglia è un altro
  modo di scrivere le stesse righe.
- **RT-9 — Registrazione eventi (§14).** «Foglio ore salvato» con `tenant_id`, `app_id`, `user_id`, settimana e
  numero di righe toccate; mai le note.

## 4. Criteri di accettazione

**CA-1 — Compilazione da tastiera**
- **Dato** una griglia con tre attività
- **Quando** la persona inserisce durate spostandosi solo con la tastiera
- **Allora** tutte le celle si salvano e i totali per giorno e per riga sono corretti

**CA-2 — Formati accettati**
- **Dato** una cella vuota
- **Quando** si scrive `1,5` in un caso e `1:30` nell'altro
- **Allora** entrambe valgono 90 minuti

**CA-3 — Periodo bloccato**
- **Dato** una settimana appartenente a un periodo chiuso
- **Quando** si apre la griglia
- **Allora** le celle non sono modificabili e mostrano il motivo del blocco

**CA-4 — Copia della settimana precedente**
- **Dato** una settimana precedente con quattro attività e 32 ore
- **Quando** si copia la struttura
- **Allora** compaiono le quattro righe con celle **vuote**, non con le 32 ore

**CA-5 — Quota esaurita senza perdita di lavoro**
- **Dato** un account senza posti liberi e una persona senza posto
- **Quando** salva la sua prima settimana
- **Allora** riceve `429`, il messaggio spiega il rimedio e quanto ha scritto resta sullo schermo

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` inserisce nella griglia un'attività di `B`
- **Allora** l'attività non è trovabile nella ricerca e l'invio diretto riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sull'analisi dei formati di durata e sull'idempotenza della scrittura a lotto, e di
      **integrazione** sul salvataggio della settimana;
- [ ] prova di **isolamento fra account** e prova che la griglia non possa mostrare le ore di un'altra persona;
- [ ] **prova end-to-end**: coprire ora — `[J-PROGETTI]` compila una settimana dalla griglia (storia 0031); voce
      nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, primo giorno della settimana compreso;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotato **perché la copia della settimana non copia le durate**;
- [ ] controllo automatico di **accessibilità** verde sulla griglia, con verifica esplicita della navigazione da
      tastiera;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0017` | La griglia scrive righe di ore |
| Storia `0018` | Le celle mostrano se le ore sono fatturabili |
| Storia `0020` | Il blocco del periodo determina quali celle sono modificabili: se 0020 non c'è ancora, tutte lo sono |

## 7. Fuori ambito

- il foglio ore di un'altra persona: non esiste, per scelta;
- l'approvazione delle ore da parte di un responsabile: sostituita dalla chiusura del periodo (storia 0020), che
  è una decisione sul **periodo** e non un giudizio sulla persona;
- l'inserimento da applicazione mobile dedicata: la griglia funziona su schermo stretto, l'applicazione nativa
  non è nel perimetro della piattaforma.

## 8. Punti aperti

- Nessuno.
