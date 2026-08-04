# 0029 — Strumenti di scrittura con conferma

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0028`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena chiuso una telefonata con un cliente
> voglio dire alla chat «ha promesso di pagare venerdì» e vedermi proporre l'annotazione da confermare
> così da registrare il fatto mentre è fresco, senza che nulla parta verso il cliente senza il mio sì.

**Contesto.** Gli strumenti di lettura (storia `0028`) sono liberi; quelli di scrittura no. Il catalogo pone una regola
di sicurezza che qui si applica al caso più delicato dell'intera suite: `invia_sollecito` manda un messaggio **a una
persona che non è nostro utente**, non si richiama indietro e, se il destinatario è sbagliato, produce una violazione di
dati personali — perché comunica a un terzo che qualcuno non ha pagato, che è esattamente la condotta che il Garante
sanziona ([documento capofila](../application-description.md) §2.3, punto 4). La regola non è una cortesia: è il
presidio. L'intelligenza artificiale prepara, la persona approva.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara sette strumenti di scrittura: `registra_incasso`,
   `registra_promessa_di_pagamento`, `apri_contestazione`, `sospendi_solleciti`, `prepara_sollecito`, `invia_sollecito`,
   `prepara_messa_in_mora`.
2. **RF-2** — Ogni strumento di scrittura produce una **bozza**: un oggetto con tutto ciò che verrebbe fatto, un
   riepilogo in lingua naturale e un identificativo; nulla è scritto e nulla parte finché la bozza non è confermata.
3. **RF-3** — La conferma è un atto **umano ed esplicito**, non un secondo passaggio dell'assistente: la bozza si
   conferma dall'interfaccia oppure con un gesto di conferma che la piattaforma riconosce come umano.
4. **RF-4** — `invia_sollecito` e `prepara_messa_in_mora` sono marcati **irreversibili** e la loro conferma non è mai
   aggirabile in nessuna configurazione, nemmeno da un'impostazione dell'account.
5. **RF-5** — Le bozze scadono: dopo un tempo breve non sono più confermabili, perché il contesto in cui sono nate non
   c'è più (nel frattempo il credito può essere stato pagato).
6. **RF-6** — La conferma esegue **ricontrollando** lo stato: se fra la bozza e la conferma il credito è stato pagato,
   sospeso o contestato, l'esecuzione si ferma e lo dichiara.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Bozza e conferma appartengono allo stesso account, ricavato dal token
  verificato; una bozza non è confermabile da un altro account nemmeno conoscendone l'identificativo. Nessuno strumento
  accetta un parametro di account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/crediti/v1/bozze` (crea, per tipo di operazione),
  `GET /api/crediti/v1/bozze/{id}` e `POST /api/crediti/v1/bozze/{id}/conferma`; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `bozza_operazione` sullo schema `app_crediti` (tipo,
  parametri, riepilogo, stato, scadenza, autore, esito) con `tenant_id`, chiave UUID versione 7, colonne di controllo e
  cancellazione logica. Le bozze scadute si eliminano con una lavorazione programmata: contengono dati personali.
- **RT-4 — Modulo frontend (§3, §5).** Elenco delle bozze in attesa nella *Panoramica*, con riepilogo leggibile e i due
  pulsanti conferma/scarta; per le operazioni irreversibili il pulsante di conferma è distinto e riporta cosa succede;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I riepiloghi delle bozze mostrati all'utente passano dallo spazio-nomi `crediti` e
  sono presenti in `en, it, fr, es, de`. Il testo del sollecito in bozza resta invece nella lingua del debitore.
- **RT-6 — Varchi e quota (§6, §7).** Sia la creazione della bozza sia la conferma attraversano la catena completa dei
  varchi. La quota si verifica **due volte**: alla bozza (per non proporre ciò che non si potrà fare) e alla conferma
  (perché nel frattempo può essersi esaurita), rispondendo `429` con il rimedio.
- **RT-7 — Esposizione conversazionale (§12).** È l'oggetto della storia. Tutti gli strumenti sono marcati **scrittura**;
  `invia_sollecito` e `prepara_messa_in_mora` portano in più la marcatura **irreversibile** e la conferma umana
  obbligatoria. Il contratto vive dentro il servizio; il server è di piattaforma e non ancora implementato
  (UC 0061-0063), così come il meccanismo di consenso delegato (UC 0062) su cui poggia la nozione di «gesto umano».
- **RT-8 — Dati personali (§10).** Una bozza di sollecito contiene destinatario e testo, cioè dati personali: la tabella
  `bozza_operazione` va nel manifesto in italiano e inglese, in `exportData` e `purgeData`, con conservazione **breve**.
  Il controllo di sospensione della storia `0016` si applica **anche** alla conferma di una bozza: non esiste un
  percorso di invio che lo aggiri.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «bozza creata», «bozza confermata», «bozza scaduta», «conferma
  fermata dal ricontrollo» sono registrati con `tenant_id`, `app_id`, `user_id`, tipo di operazione e identificativo di
  correlazione, **senza parametri e senza testi**.

## 4. Criteri di accettazione

**CA-1 — Bozza e conferma**
- **Dato** una sessione conversazionale su un account abilitato
- **Quando** viene invocato `registra_promessa_di_pagamento` per venerdì prossimo
- **Allora** viene creata una bozza con il riepilogo leggibile, **nulla è registrato**, e solo dopo la conferma umana la
  promessa esiste e i solleciti risultano sospesi

**CA-2 — Invio irreversibile**
- **Dato** una bozza prodotta da `prepara_sollecito` · **Quando** `invia_sollecito` viene invocato senza conferma umana ·
  **Allora** nessun messaggio parte e la risposta dice che serve una conferma

**CA-3 — Ricontrollo alla conferma**
- **Dato** una bozza di sollecito creata stamattina e un incasso registrato nel frattempo che azzera il residuo
- **Quando** si conferma la bozza
- **Allora** l'esecuzione si ferma, lo dichiara, e **nessun messaggio parte**

**CA-4 — Bozza scaduta**
- **Dato** una bozza più vecchia del tempo di validità · **Quando** si tenta di confermarla · **Allora** la conferma è
  rifiutata con la spiegazione, e la bozza viene eliminata

**CA-5 — Quota esaurita fra bozza e conferma**
- **Dato** una bozza di operazione che consuma quota e un tetto raggiunto nel frattempo · **Quando** si conferma ·
  **Allora** la risposta è `429` con il rimedio, e nulla viene creato

**CA-6 — Isolamento fra account**
- **Dato** una bozza dell'account `A` · **Quando** un utente di `B` ne conosce l'identificativo e tenta di confermarla ·
  **Allora** riceve l'errore di risorsa non trovata

**CA-7 — Nessuna scorciatoia configurabile**
- **Dato** le impostazioni dell'account · **Quando** si cerca un modo di disattivare la conferma per gli invii ·
  **Allora** non esiste, e una prova automatica verifica che nessun percorso di codice invii senza conferma

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sul ricontrollo alla conferma e sulla scadenza delle bozze, di **integrazione** su ciascuno dei
      sette strumenti;
- [ ] prova di **isolamento fra account** su bozza e conferma;
- [ ] prova **strutturale** che nessun percorso di invio esista senza passare dalla conferma;
- [ ] **prova end-to-end**: *coprire ora* — «prepara da chat, conferma, il sollecito parte» è un passo del percorso
      `[J-CREDITI]`; voce registrata nel registro di copertura con proprietaria la storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `bozza_operazione`, conservazione breve, presente in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sull'elenco degli strumenti irreversibili e sul ricontrollo
      alla conferma;
- [ ] contratto degli **strumenti conversazionali** completo per la parte di scrittura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0028` | Il contratto degli strumenti nasce lì; qui si aggiunge la parte di scrittura |
| storie `0009`, `0014`, `0016`, `0018`, `0019`, `0021` | Sono le funzioni che gli strumenti mettono in bozza |
| UC 0061-0063 (livello conversazionale) e UC 0062 (consenso delegato) | Non implementati: la nozione di «gesto umano di conferma» sarà quella della piattaforma; nel frattempo la conferma avviene dall'interfaccia |

## 7. Fuori ambito

- Gli strumenti deliberatamente **non** esposti — importazione da file, stralcio, generazione del collegamento pubblico,
  esportazione, configurazione delle sequenze e dei modelli — con il motivo scritto nel contratto (storia `0028`, RF-6).
- Il meccanismo di consenso delegato e la sua interfaccia: di piattaforma.

## 8. Punti aperti

**Che cosa conta come «conferma umana»** quando la conferma avviene dentro una conversazione e non nel backoffice è una
decisione di piattaforma (UC 0062), non di questa app. Finché non è presa, la conferma resta nell'interfaccia: è la
scelta prudente e va dichiarata all'utente.
