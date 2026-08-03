# 0027 — Esportazione dei dati

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 05 — Acquisizione e scambio dei lead
**Storia**: `0027` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0008`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio poter tirare fuori i miei dati in un file quando mi serve
> così da sapere che l'archivio è mio e non resta prigioniero dell'app.

**Contesto.** L'esportazione serve a tre cose diverse che vanno tenute distinte: la comodità («mi serve l'elenco
per una riunione»), la libertà dal fornitore (nessun cliente compra un archivio da cui non può uscire) e il
diritto dell'interessato, che è una cosa a sé e resta accessibile **sempre**, anche ad abbonamento scaduto. È
anche il punto in cui nasce una **lista**: e una lista di numeri di telefono, in Italia, va confrontata con il
Registro pubblico delle opposizioni prima di essere usata per chiamare
([application-description.md](../application-description.md) §2.3 punto 4). L'app non lo fa al posto del cliente,
ma glielo dice.

## 2. Requisiti funzionali

1. **RF-1** — L'utente può esportare contatti, aziende e trattative in formato tabellare, applicando gli stessi
   filtri della schermata da cui parte.
2. **RF-2** — L'esportazione dei contatti include, per ogni riga, lo **stato delle preferenze di contatto** per
   canale: una lista senza quell'informazione è una lista pericolosa.
3. **RF-3** — Prima di produrre il file compare un avviso che dice tre cose: che il file contiene dati di persone,
   che la protezione dipende da chi lo custodisce, e che l'uso per contatti commerciali richiede una base
   giuridica e — per le telefonate in Italia — il confronto con il Registro pubblico delle opposizioni.
4. **RF-4** — Ogni esportazione è registrata: chi, quando, che cosa, quante righe.
5. **RF-5** — L'esportazione richiede ruolo `owner` o `admin`: è l'uscita in massa dei dati dell'account.
6. **RF-6** — L'esportazione dei propri dati come **diritto dell'interessato** resta accessibile anche con
   abbonamento non attivo, e passa dal contratto dati dell'app, non da questa funzione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'esportazione comprende solo dati dell'account del token verificato: un
  difetto qui è una violazione di dati personali, non un errore di elenco.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/sales/v1/exports` che avvia l'estrazione e
  `GET /api/sales/v1/exports/{id}` che ne restituisce stato e file; le estrazioni grandi si producono in modo
  asincrono; errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `export_log` sullo schema `app_sales`, con migrazione
  `V<N>__export_log.sql`: `tenant_id`, autore, tipo, filtri applicati, numero di righe, momento. È lo stesso
  registro che usa la storia 0024.
- **RT-4 — Modulo frontend (§3, §5).** Azione «Esporta» nelle barre degli strumenti, con la finestra di avviso e
  la conferma; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Testo dell'avviso, intestazioni delle colonne del file e messaggi in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Con abbonamento `canceled` questa esportazione
  risponde `402`, ma **l'esportazione come diritto dell'interessato resta accessibile in ogni caso**: sono due
  cose diverse e vanno provate entrambe.
- **RT-7 — Esposizione conversazionale (§12).** `export_contacts` è **scrittura irreversibile** e richiede
  **conferma umana obbligatoria** (storia 0035): la bozza dice quante righe usciranno e mostra lo stesso avviso.
  È l'unico strumento con effetto verso l'esterno insieme a `close_deal`.
- **RT-8 — Dati personali (§10).** È una **uscita di dati personali**: va dichiarata nel manifesto come
  trattamento (finalità: portabilità e uso commerciale del titolare), e il registro `export_log` va aggiunto a
  `exportData` e `purgeData`. Il file prodotto ha una vita breve e viene rimosso dopo il ritiro; la durata
  proposta è **7 giorni**, da confermare.
- **RT-9 — Registrazione eventi (§14).** «Esportazione avviata/completata» con tipo, numero di righe e autore;
  **mai** i dati esportati.

## 4. Criteri di accettazione

**CA-1 — Esportazione filtrata**
- **Dato** un elenco di contatti filtrato per origine «modulo web»
- **Quando** l'utente esporta
- **Allora** il file contiene solo quei contatti, con lo stato delle preferenze per canale

**CA-2 — Avviso obbligatorio**
- **Dato** l'azione di esportazione
- **Quando** l'utente la avvia
- **Allora** vede l'avviso completo (dati di persone, responsabilità, base giuridica, Registro pubblico delle
  opposizioni per le telefonate in Italia) e deve confermare

**CA-3 — Tracciamento**
- **Dato** un'esportazione completata
- **Quando** si consulta il registro delle esportazioni
- **Allora** compare la riga con autore, momento, tipo, filtri e numero di righe

**CA-4 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** avvia un'esportazione
- **Allora** riceve `403`

**CA-5 — Abbonamento scaduto**
- **Dato** un account con abbonamento `canceled`
- **Quando** un amministratore tenta questa esportazione, e poi esercita il diritto di esportazione dei dati
- **Allora** la prima risponde `402` e la seconda riesce

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` esporta
- **Allora** il file non contiene nulla di `B`, e non può scaricare il file di un'esportazione di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione del file e di **integrazione** sull'estrazione asincrona;
- [ ] prova di **isolamento fra account** sull'esportazione e sullo scaricamento del file;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-SALES]` (storia 0037) verifica che l'avviso compaia e
      che il file contenga solo i dati del proprio account; voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, avviso e intestazioni comprese;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per il trattamento «esportazione» e per
      `export_log`, presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotata la distinzione fra esportazione di comodo e diritto
      dell'interessato;
- [ ] contratto degli **strumenti conversazionali**: `export_contacts` con conferma obbligatoria;
- [ ] controllo automatico di **accessibilità** verde sull'avviso;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0007`, `0008`, `0013` | Servono i dati e i filtri da esportare |
| Storia `0011` | Lo stato delle preferenze va nel file |

## 7. Fuori ambito

- il confronto automatico con il Registro pubblico delle opposizioni: LeadGrove non compone numeri e non è un
  operatore di telemarketing; l'obbligo resta del cliente, l'app lo ricorda;
- l'invio del file per posta elettronica: sarebbe un canale verso l'esterno;
- l'esportazione programmata e ricorrente: non prevista, moltiplicherebbe le uscite di dati senza una persona che
  guarda.

## 8. Punti aperti

- **Durata di vita del file prodotto** — proposta 7 giorni. È una decisione di conservazione, chiude lo
  sviluppatore in sede di manifesto.
