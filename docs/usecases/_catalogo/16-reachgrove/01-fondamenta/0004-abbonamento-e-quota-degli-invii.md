# 0004 — Abbonamento e quota degli invii

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha scelto un piano
> voglio sapere quanti invii mi restano questo mese e che l'app si fermi quando li ho finiti
> così da non trovarmi una bolletta a sorpresa né una campagna partita a metà senza capire perché.

**Contesto.** ReachGrove fa pagare **gli invii**, non gli iscritti archiviati
([application-description.md](../application-description.md) §3): è la scelta di posizionamento più importante
dell'app e va resa vera nel codice prima che esista una campagna, non dopo. Un invio è un messaggio consegnato a
un canale per un destinatario: cinquecento destinatari sono cinquecento invii.

Da qui discende la conseguenza tecnica che questa storia deve fissare: la quota si prenota **per destinatario, al
momento dell'invio**, non alla creazione della campagna. Prenotarla alla creazione vorrebbe dire bloccare un
cliente che sta ancora scrivendo una bozza, e sbagliare il conto ogni volta che il segmento cambia fra la scrittura
e la partenza — e i segmenti si ricalcolano al momento dell'invio (storia 0013).

## 2. Requisiti funzionali

1. **RF-1** — L'app legge l'abilitazione dell'account dalla **proiezione locale** alimentata a eventi, mai con una
   chiamata di rete sul percorso caldo, e ne ricava il tetto mensile della metrica `messages_sent`.
2. **RF-2** — Ogni funzione protetta attraversa la catena dei varchi: token non valido → `401`; app spenta dalla
   piattaforma → `403`; account non abilitato → `402`; ruolo insufficiente → `403`; quota esaurita → `429`.
3. **RF-3** — La quota si prenota **una unità per destinatario, al momento dell'invio**: creare, modificare o
   programmare una campagna non consuma nulla.
4. **RF-4** — A tetto raggiunto il servizio risponde `429` con un messaggio che dice cosa è successo, cosa non si
   può più fare e come si rimedia (passare di piano oppure attendere la finestra successiva). Nessun addebito a
   consumo, mai.
5. **RF-5** — Il contatore si azzera all'inizio di ogni finestra mensile, perché la metrica è di natura `flow`:
   gli iscritti archiviati **non** consumano quota e una lista ferma non costa nulla.
6. **RF-6** — Il consumo è visibile nella panoramica del modulo — quanti invii usati su quanti, in quale finestra —
   e un avviso compare **prima** di partire con una campagna il cui numero di destinatari supererebbe il residuo.
7. **RF-7** — Il superamento del tetto **durante** una spedizione già in corso non fa perdere messaggi: la
   spedizione si mette in pausa nello stato «bloccata» e riprende quando la quota torna disponibile.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il conteggio del consumo e il tetto si leggono per l'account del token
  verificato; un identificativo di account che arrivasse dal corpo o dai parametri viene ignorato. Il consumo di
  un account non è mai visibile né influenzabile da un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/campaigns/v1/quota` che restituisce tetto,
  consumo e inizio della finestra corrente; il rifiuto per quota esaurita esce come `429` in
  `application/problem+json` con un tipo di problema stabile, così che il modulo frontend possa riconoscerlo senza
  leggere il testo. Definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella di contatore del consumo per account e finestra sullo schema
  `app_campaigns`, con `tenant_id`, chiave UUID versione 7 e colonne di controllo. La prenotazione è
  un'operazione atomica: due spedizioni contemporanee non possono sfondare il tetto.
- **RT-4 — Modulo frontend (§3, §5).** La panoramica del modulo `campaigns` mostra la barra di consumo e l'avviso
  di quota; solo token del sistema di design; funziona in tema chiaro e scuro. Il messaggio di quota esaurita
  porta l'azione «cambia piano», non un vicolo cieco.
- **RT-5 — Cinque lingue (§4).** Tutti i testi di quota e di rifiuto passano dallo spazio-nomi `campaigns` e sono
  presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Prima di consegnare un messaggio a un destinatario il servizio prenota una
  unità della metrica `messages_sent` (natura `flow`); a quota esaurita risponde `429` con l'indicazione del
  rimedio e **nulla viene consegnato**. Con abbonamento in `trialing`, `active` o `past_due` la funzione resta
  accessibile; con `paused` o `canceled` risponde `402`. I diritti dell'interessato (esportazione, cancellazione)
  restano accessibili in ogni caso, anche a quota esaurita e ad abbonamento scaduto.
- **RT-7 — Esposizione conversazionale (§12).** Le chiamate che arrivano dal livello conversazionale attraversano
  la **stessa** catena di varchi e lo stesso contatore: un assistente non ha una corsia preferenziale. Il
  meccanismo è di piattaforma (UC 0064) e qui si dichiara solo che la quota si applica al chiamante, chiunque sia.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il contatore conta messaggi, non persone, e non
  memorizza a chi sono stati inviati. Il collegamento fra invio e destinatario vive in `delivery` (storia 0002).
- **RT-9 — Registrazione eventi (§14).** Gli eventi «quota prenotata», «quota esaurita», «spedizione messa in
  pausa per quota» sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione; si
  registrano conteggi e identificativi, **mai** il recapito del destinatario.

## 4. Criteri di accettazione

**CA-1 — Il consumo si vede**
- **Dato** un account sul piano intermedio con cinquemila invii mensili, di cui milleduecento già usati
- **Quando** l'utente apre la panoramica di ReachGrove
- **Allora** vede «1.200 di 5.000 invii — questo mese» e la finestra a cui il numero si riferisce

**CA-2 — La bozza non consuma**
- **Dato** un account con quota residua
- **Quando** l'utente crea una campagna e la programma verso un segmento di trecento persone
- **Allora** il consumo non cambia: la quota si preleva solo quando i messaggi partono

**CA-3 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto della metrica `messages_sent`
- **Quando** tenta di far partire una campagna
- **Allora** riceve `429` con un messaggio che spiega come rimediare, e **nessun** messaggio viene consegnato

**CA-4 — Il tetto raggiunto a metà spedizione mette in pausa, non perde**
- **Dato** una campagna in corso verso mille destinatari e una quota residua di quattrocento
- **Quando** i quattrocento invii sono consumati
- **Allora** la campagna passa allo stato «bloccata» con il motivo «quota esaurita», i seicento destinatari
  restanti non risultano né inviati né falliti, e nessun messaggio viene consegnato due volte quando la spedizione
  riprende

**CA-5 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled`
- **Quando** chiede il proprio consumo o tenta un invio
- **Allora** riceve `402`; l'esportazione dei propri dati resta invece accessibile

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con piani diversi
- **Quando** un utente di `A` chiede la quota forzando l'identificativo di `B` nella richiesta
- **Allora** riceve la propria, e l'identificativo passato viene ignorato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della finestra mensile e sulla prenotazione atomica, e di **integrazione**
      sulla rotta della quota e sul rifiuto `429`, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulle rotte introdotte, con la verifica del
      `402` ad abbonamento non attivo;
- [ ] **prova end-to-end**: rimando — la prova del blocco a `429` entra nel percorso `[J-CAMPAIGNS]` della storia
      0037, che è la sua proprietaria, perché serve una campagna vera per esercitarla; qui la copertura è di
      integrazione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i messaggi di rifiuto;
- [ ] **manifesto dei dati**: nessuna voce nuova, il contatore non tratta dati di persone;
- [ ] **registro delle decisioni** compilato, con annotato perché la quota si prenota per destinatario al momento
      dell'invio e non alla creazione della campagna;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; dichiarato che le chiamate dell'assistente
      consumano la stessa quota;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Serve il servizio con i varchi 1 e 2 già in piedi |
| Storia `0002` | Il contatore è una tabella dello schema dell'app |
| Conferma dei prezzi e dei limiti ([application-description.md](../application-description.md) §5) | I tetti dei piani stanno nel listino come codice: il meccanismo si costruisce comunque, i numeri li conferma lo sviluppatore |
| Proiezione locale dell'abilitazione alimentata a eventi | L'abilitazione non si chiede all'app centrale sul percorso caldo |

## 7. Fuori ambito

- la coda di spedizione, la ripresa dopo un guasto e la garanzia di non consegnare due volte: sono la storia 0019,
  che possiede la macchina della spedizione. Qui si dichiara **che cosa** deve succedere alla quota esaurita, non
  **come** la coda lo realizza: il rimando è esplicito;
- il file di listino `pricing/campaigns.yaml`: lo scrive la skill con lo sviluppatore;
- il costo dei canali aggiuntivi, che il cliente paga al proprio fornitore pur consumando la nostra quota: è la
  storia 0022;
- le deroghe temporanee di quota concesse dall'assistenza: sono nella console di amministrazione
  ([estensioni-admin.md](../estensioni-admin.md)).

## 8. Punti aperti

- **Quota consumata dai canali aggiuntivi** — sul canale dei messaggi brevi e della messaggistica l'invio lo paga
  il cliente al **suo** fornitore, ma consuma comunque la nostra quota, perché la segmentazione, il controllo del
  consenso e il tracciamento li facciamo noi
  ([application-description.md](../application-description.md) §5). È coerente ma può sembrare un doppio addebito:
  come si spiega nel listino è una decisione commerciale dello sviluppatore.
- **Comportamento del piano gratuito** — cinquecento invii al mese sono deliberatamente pochi; se il gratuito
  debba avere un tetto anche sugli iscritti archiviati, per scoraggiare chi lo usa come deposito di liste
  comprate, è una decisione di prodotto che questa storia non prende.
