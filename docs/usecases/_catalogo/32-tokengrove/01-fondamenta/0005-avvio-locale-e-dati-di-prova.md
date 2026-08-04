# 0005 — Avvio locale e dati di prova

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende il lavoro su questa app
> voglio avviarla in locale con un comando e trovarci dentro dati verosimili
> così da poter lavorare sulla prossima storia senza prima passare mezza giornata a costruirmi uno scenario.

**Contesto.** Questa app ha un problema di ambiente che le altre non hanno: **dipende da fornitori esterni di
modelli**, e in locale non si possono chiamare davvero. Servirebbe una chiave vera di un'organizzazione vera, che
costa e che nessuno metterà mai in un ambiente di sviluppo condiviso. Senza un fornitore simulato, ogni storia
dell'epica 02 sarebbe impossibile da sviluppare e da provare. È quindi la storia che rende sviluppabili tutte le
successive, e va fatta adesso.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` mostra `spesa_modelli` con porta `8132` e schema `app_spesa_modelli`, ricavati
   dal solo file delle proprietà dell'applicazione; `./app-start.sh` avvia l'app senza modifiche manuali agli
   script.
2. **RF-2** — Esiste un **fornitore di modelli simulato** in locale che espone le stesse forme di risposta delle
   interfacce di consumo e costo dei fornitori veri (intervalli a minuto/ora/giorno per il consumo, solo giorno per
   il costo), con dati inventati e deterministici.
3. **RF-3** — Un comando di semina popola un account di prova con: due fonti (una di rendiconto sul fornitore
   simulato, una di invio), tre mesi di misure inventate su quattro modelli, alcune etichette e una quota di
   dimensioni compatibili con il piano intermedio.
4. **RF-4** — I dati di semina comprendono **i casi scomodi**, non solo quelli felici: misure senza etichette (che
   finiranno nel non attribuito), un modello sconosciuto al catalogo dei prezzi, un giorno con uno scarto di
   riconciliazione, un'impennata anomala.
5. **RF-5** — Tutti i dati di prova sono **inventati**: nessun nome di persona reale, indirizzi di posta solo nel
   dominio riservato alle prove, nessun importo copiato da una fattura vera.

## 3. Requisiti tecnici

- **RT-1 — Avvio locale (§15).** La scoperta automatica dei servizi deriva tutto dal solo file delle proprietà: da
  lì discendono avvio, migrazioni, rotte del proxy locale e avvii di collaudo. Se viene voglia di modificare a mano
  uno script di avvio, è un difetto della scoperta automatica e va corretto lì.
- **RT-2 — Isolamento fra account (§1).** La semina scrive su un account di prova identificato dal suo
  `tenant_id`; nessuna scrittura senza `tenant_id`.
- **RT-3 — Prove (§11).** I dati di prova sono **deterministici**: la stessa semina produce gli stessi numeri, così
  che le prove end-to-end possano affermare valori esatti senza attese a tempo.
- **RT-4 — Dati personali (§10).** Nessun dato personale reale, in nessuna forma. Le etichette seminate usano nomi
  di fantasia dichiarati come tali.
- **RT-5 — Registrazione eventi (§14).** La semina registra un evento di inizio e uno di fine con il conteggio
  delle righe scritte, senza contenuti.
- **RT-6 — Modulo frontend (§3).** Il modulo è abilitato nello stub locale dell'abilitazione, così che l'app si
  veda nella barra laterale appena avviato lo stack.

## 4. Criteri di accettazione

**CA-1 — L'app è scoperta e si avvia da sola**
- **Dato** un repository appena clonato
- **Quando** si esegue `./dev.sh services` e poi `./app-start.sh`
- **Allora** `spesa_modelli` compare con porta `8132` e schema `app_spesa_modelli`, l'app risponde e le sue rotte
  passano dal proxy locale, senza che nessuno abbia modificato uno script a mano

**CA-2 — Il fornitore simulato risponde come quelli veri**
- **Dato** lo stack locale avviato
- **Quando** l'app interroga il fornitore simulato per il consumo di un intervallo a giorno
- **Allora** riceve una risposta della stessa forma di quella dei fornitori veri, con paginazione e con il costo
  disponibile solo a granularità giornaliera

**CA-3 — La semina produce uno scenario utile**
- **Dato** un ambiente locale vuoto
- **Quando** si esegue il comando di semina
- **Allora** l'account di prova ha due fonti, tre mesi di misure su quattro modelli, e contiene almeno una misura
  senza etichette, un modello sconosciuto, un giorno con scarto e un'impennata

**CA-4 — Determinismo**
- **Dato** due esecuzioni della semina su ambienti puliti
- **Quando** si confrontano i totali di spesa per mese e per modello
- **Allora** sono identici

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend e collaudo di avvio; l'intera suite prima del
      commit);
- [ ] prova di **avvio reale** dell'artefatto nel profilo di spedizione, non solo nel profilo di prova;
- [ ] prova di **isolamento fra account**: la semina non scrive fuori dall'account di prova;
- [ ] **prova end-to-end**: nessun impatto diretto; questa storia **abilita** il percorso `[J-SPESA-MODELLI]` della
      storia `0034`, che ne è la proprietaria;
- [ ] **traduzioni**: nessun testo visibile nuovo oltre a quelli già presenti;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di simulare i fornitori invece di
      chiamarli davvero;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali;
- [ ] documentazione dell'avvio locale aggiornata con il fornitore simulato e il comando di semina.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0001`-`0004` | Servono servizio, tabelle, modulo frontend e quota per poter seminare qualcosa di sensato |

## 7. Fuori ambito

- il collegamento a un fornitore **vero**: è della storia `0006`, e in locale non si fa mai;
- il catalogo dei prezzi: qui la semina usa un catalogo minimo di prova, quello vero è dell'epica 03.

## 8. Punti aperti

Nessuno.
