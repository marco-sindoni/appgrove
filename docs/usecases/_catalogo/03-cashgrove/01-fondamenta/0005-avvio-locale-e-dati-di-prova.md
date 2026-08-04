# 0005 — Avvio locale e dati di prova

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende il lavoro su CashGrove
> voglio avviare l'app in locale e trovarci dentro un portafoglio crediti verosimile già popolato
> così da vedere subito come si comporta l'app piena, invece di dover inventare dati a ogni sessione.

**Contesto.** Un'app di gestione crediti vuota non dice nulla: l'anzianità dei crediti, gli stati, gli scaduti e la
quota si capiscono solo con qualche decina di righe dentro. Oggi ogni sviluppatore se le creerebbe a mano, in modo
diverso, e le prove end-to-end non avrebbero un punto di partenza stabile. Questa storia chiude l'epica delle fondamenta
rendendo l'app **utilizzabile subito dopo l'unione del ramo**, come prescrive la regola di piattaforma sull'avvio
locale.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un comando di popolamento dei dati di prova che crea, per un account locale, un insieme di
   debitori e crediti **inventati** e deterministici.
2. **RF-2** — I dati coprono tutti gli stati della macchina a stati del credito e tutte le fasce di anzianità
   (non scaduto, 1-30, 31-60, 61-90, oltre 90 giorni).
3. **RF-3** — I dati sono generati per **due** account distinti, così che le prove di isolamento abbiano sempre un
   secondo account con cui confrontarsi.
4. **RF-4** — Il comando è ripetibile: eseguirlo due volte non duplica le righe e non fa fallire nulla.
5. **RF-5** — Il popolamento non è mai attivo fuori dall'ambiente locale e di prova.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I dati di prova nascono già assegnati ai rispettivi `tenant_id`; il comando
  non aggira il filtro per account, lo usa.
- **RT-2 — Interfaccia di programmazione (§2).** Il popolamento non è una rotta pubblica: è un comando di sviluppo.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; il popolamento scrive su `debitore` e `credito` rispettando
  colonne di controllo e chiavi UUID versione 7.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova, ma con i dati dentro le schermate del guscio smettono di
  essere vuote: è il primo momento in cui si vede l'app somigliare a se stessa.
- **RT-5 — Cinque lingue (§4).** I debitori di prova hanno lingue preferite diverse fra `en, it, fr, es, de`, così che
  le storie sui modelli di messaggio (`0012`) trovino un caso per ciascuna.
- **RT-6 — Varchi e quota (§6, §7).** Il volume dei dati di prova sta **sotto** il tetto del piano gratuito per un
  account e **sopra** per l'altro, così da poter esercitare il `429` senza costruire nulla a mano.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo.
- **RT-8 — Dati personali (§10).** I dati sono **inventati**: nomi di fantasia, indirizzi con dominio `*.test`, partite
  IVA non attribuibili. Mai dati veri, nemmeno «presi da un cliente e modificati».
- **RT-9 — Registrazione eventi (§14).** Il comando registra quante righe ha creato, per account, senza nomi.
- **RT-10 — Avvio locale automatico (§15).** `./dev.sh services` mostra `crediti` con porta e schema; `./app-start.sh`
  avvia il servizio; `dev migrate` applica le migrazioni: tutto dalla sola scoperta automatica, senza modifiche agli
  script.

## 4. Criteri di accettazione

**CA-1 — L'app parte e ha dentro qualcosa**
- **Dato** un repository appena clonato con il ramo unito
- **Quando** si eseguono `./app-start.sh` e il comando di popolamento
- **Allora** aprendo il backoffice si vede CashGrove con debitori e crediti, distribuiti su tutte le fasce di anzianità

**CA-2 — Ripetibile**
- **Dato** i dati di prova già caricati · **Quando** si esegue di nuovo il comando · **Allora** il numero di righe non
  cambia e il comando termina senza errori

**CA-3 — Due account per l'isolamento**
- **Dato** i dati di prova caricati
- **Quando** si accede con l'utente del secondo account
- **Allora** si vedono crediti diversi, e nessun credito del primo account è raggiungibile in alcun modo

**CA-4 — Quota esercitabile**
- **Dato** l'account «pieno» dei dati di prova sul piano gratuito
- **Quando** si tenta di registrare un credito in più
- **Allora** si riceve `429`, senza aver dovuto creare righe a mano

**CA-5 — Mai fuori dall'ambiente locale**
- **Dato** il profilo di spedizione · **Quando** si tenta di eseguire il popolamento · **Allora** il comando rifiuta e
  spiega perché

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e smoke);
- [ ] prova di **integrazione** che esegue il popolamento due volte e verifica l'idempotenza;
- [ ] prova di **isolamento fra account** sui dati generati;
- [ ] **prova end-to-end**: *rimando* — i dati di prova saranno il punto di partenza del percorso `[J-CREDITI]`
      (storia `0031`), che è la storia proprietaria;
- [ ] **traduzioni**: nessun testo visibile nuovo;
- [ ] **manifesto dei dati**: nessuna modifica; è però annotato che i dati di prova sono inventati;
- [ ] **registro delle decisioni** compilato con la composizione del corredo di dati e il perché;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] `tools/smoke` avvia il servizio nel profilo di spedizione senza errori.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Servono le tabelle da popolare |
| storia `0004` | Il corredo di dati è calibrato sui tetti di piano |

## 7. Fuori ambito

- Dati di prova per solleciti, promesse e contestazioni: li aggiunge la storia che introduce la relativa tabella,
  estendendo lo stesso comando.
- L'importazione da file: è una funzione per l'utente, non un comando di sviluppo (storia `0008`).

## 8. Punti aperti

Nessuno.
