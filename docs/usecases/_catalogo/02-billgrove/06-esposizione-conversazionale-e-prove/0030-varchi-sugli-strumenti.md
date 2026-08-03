# 0030 — Varchi sugli strumenti

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0030` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0028`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile della piattaforma
> voglio che abilitazione, ruolo e quota valgano allo stesso modo quando a chiamare è un assistente
> così da non ritrovarmi con una via d'accesso che scavalca i controlli costruiti per l'interfaccia, che è il modo
> più facile per farsi male con un livello conversazionale.

**Contesto.** Le storie `0028` e `0029` dichiarano gli strumenti; questa verifica che passino dagli stessi cinque
varchi dell'interfaccia: token valido, app non spenta, account abilitato, ruolo sufficiente, quota non esaurita. È
una storia piccola ma non rinviabile, perché la classe di difetti che chiude — «dalla chat si può fare quello che
dall'interfaccia è vietato» — è invisibile finché non succede. La applicazione dei varchi alle chiamate
dell'assistente è anche un use case di piattaforma (UC 0064): qui si fa la parte che spetta all'app.

## 2. Requisiti funzionali

1. **RF-1** — Ogni invocazione di strumento attraversa i cinque varchi nell'ordine, con gli stessi codici di
   risposta dell'interfaccia.
2. **RF-2** — Il ruolo dell'utente per conto del quale l'assistente agisce è quello vero: un utente in sola lettura
   (storia `0027`) non può usare strumenti di scrittura, nemmeno da chat.
3. **RF-3** — La quota si consuma una sola volta per operazione, indipendentemente dalla via da cui arriva.
4. **RF-4** — Il messaggio restituito quando un varco nega è **comprensibile a chi legge la chat**: dice che cosa
   manca e come si rimedia, non un codice.
5. **RF-5** — Nessuno strumento espone dati o funzioni che l'utente non vedrebbe dall'interfaccia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il `tenant_id` viene dal contesto autenticato della chiamata delegata;
  una prova dedicata verifica che nessuno strumento possa essere indotto a lavorare su un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** I varchi sono quelli del filtro comune del servizio: **un solo
  punto di applicazione**, non una copia per gli strumenti. Se il varco fosse duplicato, le due copie
  divergerebbero.
- **RT-6 — Varchi e quota (§6, §7).** La catena: `401` senza token valido, `403` ad app spenta, `402` senza
  abilitazione, `403` a ruolo insufficiente, `429` a quota esaurita. La prenotazione della quota è quella della
  storia `0004`, invocata dallo stesso punto.
- **RT-7 — Esposizione conversazionale (§12).** È la parte d'app di UC 0064 (applicazione di abilitazione e quota
  alle chiamate dell'assistente): l'app garantisce che i suoi strumenti rispettino i varchi; il consenso delegato e
  l'identità dell'assistente sono di piattaforma (UC 0062).
- **RT-5 — Cinque lingue (§4).** I messaggi di diniego destinati alla persona passano dallo spazio-nomi `billing` e
  sono presenti in `en, it, fr, es, de`.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. Va però verificato che i messaggi di diniego non
  rivelino l'esistenza di dati di altri account: un «non hai accesso a quel documento» che si distingue da «quel
  documento non esiste» è già una perdita di informazione.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `strumento negato per abilitazione`, `per ruolo`, `per quota`
  sono registrati con `tenant_id`, `app_id`, `user_id`, nome dello strumento e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled`
- **Quando** l'assistente invoca `elenca_documenti`
- **Allora** riceve il diniego corrispondente a `402`, con un messaggio comprensibile

**CA-2 — Ruolo insufficiente**
- **Dato** un utente in sola lettura · **Quando** l'assistente invoca `crea_fattura` per suo conto
- **Allora** l'operazione è negata con il diniego corrispondente a `403`

**CA-3 — Quota esaurita**
- **Dato** un account con quota `documenti` esaurita e una bozza pronta
- **Quando** viene confermata l'emissione da strumento
- **Allora** la risposta corrisponde a `429`, il documento resta in bozza e nessun numero è consumato

**CA-4 — Quota consumata una volta sola**
- **Dato** un'emissione avviata da strumento
- **Quando** va a buon fine
- **Allora** è stata consumata **una** unità, come se fosse partita dall'interfaccia

**CA-5 — Nessuna informazione di troppo**
- **Dato** un documento dell'account `B`
- **Quando** l'assistente di `A` chiede di leggerlo
- **Allora** il diniego non permette di dedurre che quel documento esista

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`; l'intera suite prima del commit);
- [ ] prove di **unità** sui messaggi di diniego e di **integrazione** su tutta la matrice varco × strumento, con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** applicate agli strumenti: sono le prove centrali
      di questa storia;
- [ ] **prova end-to-end**: *rimando* — non esiste un livello conversazionale da guidare; la matrice è coperta da
      prove di integrazione. Proprietaria del rimando: storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per i messaggi di diniego;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato;
- [ ] **registro delle decisioni** compilato, con annotata la scelta del punto unico di applicazione dei varchi;
- [ ] contratto degli **strumenti conversazionali** invariato: questa storia non ne aggiunge, li protegge;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0028` | Servono gli strumenti di lettura da proteggere |
| storia `0029` | Servono gli strumenti di scrittura da proteggere |
| UC 0062 e UC 0064 (piattaforma, non implementate) | Autenticazione e consenso delegato, e applicazione di abilitazione e quota alle chiamate dell'assistente |

## 7. Fuori ambito

- il tracciamento e la sicurezza del server conversazionale (UC 0065): sono di piattaforma;
- la limitazione della frequenza delle chiamate: di piattaforma;
- l'industrializzazione degli strumenti nello scaffolding (UC 0066): di piattaforma.

## 8. Punti aperti

Nessuno lato app. Resta la dipendenza dalle use case di piattaforma non implementate: finché non esistono, questa
storia si prova sul contratto e non su un assistente vero.
