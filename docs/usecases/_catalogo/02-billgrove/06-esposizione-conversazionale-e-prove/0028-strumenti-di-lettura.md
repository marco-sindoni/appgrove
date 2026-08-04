# 0028 — Strumenti di lettura

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta guidando e vuole sapere chi non l'ha pagato
> voglio poter chiedere all'assistente «chi mi deve dei soldi da più di un mese?»
> così da avere la risposta in una frase invece che in tre schermate, senza aprire il computer.

**Contesto.** Il catalogo pone a tutte le sessanta app un requisito trasversale: ogni funzione deve essere
comandabile da una chat. Il livello conversazionale **non esiste ancora** nel repository — è l'epica
`12-ready-for-ai-mcp`, use case 0061-0066, scritta e non implementata. Il compito di questa storia non è costruire
il server, che è di piattaforma: è **dichiarare il contratto** degli strumenti di sola lettura e tenerlo dentro il
servizio, versionato con esso. Si comincia dalla lettura perché è la parte senza rischio: nessun effetto, nessuna
conferma.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara sei strumenti di **sola lettura**: `elenca_documenti`, `leggi_documento`,
   `elenca_non_pagati`, `riepilogo_incassi`, `cerca_cliente`, `elenca_documenti` filtrato per tipo (preventivi,
   documenti di trasporto).
2. **RF-2** — Ogni strumento dichiara nome stabile, descrizione in lingua naturale, schema dei parametri, schema del
   risultato, marcatura **lettura** e **idempotenza**.
3. **RF-3** — I risultati sono **minimizzati**: solo i campi che servono a rispondere, mai l'entità intera.
4. **RF-4** — Ogni risultato che contiene importi dichiara la valuta; ogni risultato che contiene date dichiara il
   fuso di riferimento.
5. **RF-5** — Il contratto è versionato: aggiungere un campo è ammesso, toglierlo o rinominarlo richiede una
   versione nuova.
6. **RF-6** — Il contratto è verificabile da un programma: esiste una prova che fallisce se uno strumento cambia
   forma senza cambiare versione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni strumento risolve il `tenant_id` **dal contesto della chiamata
  autenticata**, mai da un parametro: uno strumento che accettasse un identificativo di account fra i parametri
  sarebbe una falla, e va impedito dal contratto stesso.
- **RT-2 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano alle rotte esistenti, non ne creano di
  parallele: una funzione esposta due volte diverge. Il contratto vive in `services/billing`, versionato con
  l'app.
- **RT-4 — Modulo frontend (§3).** Nessuna schermata nuova; la Panoramica mostra già che le funzioni saranno
  richiamabili da chat (storia `0003`).
- **RT-5 — Cinque lingue (§4).** Le **descrizioni degli strumenti** sono in inglese, perché sono destinate a un
  modello e non a una persona; le risposte all'utente sono formulate dal livello conversazionale nella lingua della
  conversazione. Questa distinzione va scritta, perché è controintuitiva rispetto alla regola delle cinque lingue.
- **RT-6 — Varchi e quota (§6).** Gli strumenti di lettura non consumano la metrica `documenti`; passano comunque
  dai varchi di autenticazione, abilitazione e ruolo (storia `0030`).
- **RT-7 — Esposizione conversazionale (§12).** È la storia che realizza il punto: strumenti di **lettura liberi**,
  senza conferma. Dipendenza dichiarata: UC 0061-0063 (architettura del server, autenticazione delegata, mappatura
  operazioni → strumenti), non ancora implementate: finché non esistono, il contratto è dichiarato e provato ma non
  raggiungibile da una chat vera.
- **RT-8 — Dati personali (§10).** Gli strumenti fanno **uscire** dati personali verso un livello conversazionale:
  la minimizzazione dei risultati non è una ottimizzazione, è una misura di protezione. Va dichiarato nel manifesto
  che esiste questa via di uscita, con la finalità e la base giuridica.
- **RT-9 — Registrazione eventi (§14).** Ogni invocazione di strumento è registrata con `tenant_id`, `app_id`,
  `user_id`, nome dello strumento e identificativo di correlazione, **senza i parametri**, che possono contenere
  nomi di persone.

## 4. Criteri di accettazione

**CA-1 — Contratto dichiarato**
- **Dato** il servizio avviato
- **Quando** si chiede l'elenco degli strumenti dichiarati
- **Allora** compaiono i sei strumenti con nome, descrizione, schema dei parametri, schema del risultato e
  marcatura `lettura`

**CA-2 — Risultato minimizzato**
- **Dato** una chiamata a `elenca_non_pagati`
- **Allora** ogni voce contiene cliente, importo residuo, giorni di ritardo e identificativo del documento, e
  **non** l'intero documento con le sue righe

**CA-3 — Nessun account nei parametri**
- **Dato** lo schema dei parametri di ciascuno strumento
- **Quando** lo si ispeziona
- **Allora** nessuno accetta un identificativo di account: il contesto lo determina

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B` con documenti scaduti
- **Quando** si invoca `elenca_non_pagati` nel contesto di `A`
- **Allora** compaiono solo documenti di `A`

**CA-5 — Contratto stabile**
- **Dato** una modifica che toglie un campo dal risultato di uno strumento senza cambiare versione
- **Quando** si esegue la suite · **Allora** la prova di stabilità del contratto fallisce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla minimizzazione dei risultati e di **integrazione** sull'invocazione degli strumenti,
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su ogni strumento dichiarato;
- [ ] **prova end-to-end**: *rimando* — non esiste un livello conversazionale da guidare; il percorso `[J-BILLING]`
      resta sull'interfaccia. Motivo: dipendenza da UC 0061-0063 non implementate. Proprietaria del rimando:
      storia `0031`;
- [ ] **traduzioni**: non applicabile agli strumenti, con la motivazione scritta;
- [ ] **manifesto dei dati** aggiornato con la via di uscita verso il livello conversazionale;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di appoggiarsi alle rotte esistenti;
- [ ] contratto degli **strumenti conversazionali** dichiarato e verificato da una prova;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: il contratto degli strumenti è descritto accanto al servizio.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0018` | `elenca_non_pagati` legge lo scadenzario |
| storia `0021` | `riepilogo_incassi` legge il report |
| UC 0061-0063 (piattaforma, non implementate) | Architettura del server conversazionale, autenticazione delegata e mappatura operazioni → strumenti. Nel frattempo il contratto è dichiarato e provato dentro l'app |

## 7. Fuori ambito

- gli strumenti di scrittura: storia `0029`;
- i varchi di abilitazione e quota sulle chiamate dell'assistente: storia `0030`;
- la costruzione del server conversazionale: è di piattaforma.

## 8. Punti aperti

Nessuno per questa storia. Resta il punto aperto generale: finché le UC 0061-0063 non esistono, il contratto è una
dichiarazione verificata ma non usata da nessuno.
