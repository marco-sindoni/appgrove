# 0019 — Spedizione programmata della campagna

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 04 — Spedizione e canali
**Storia**: `0019` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha programmato una campagna per domani mattina
> voglio che parta da sola, che arrivi a tutti una volta sola e che io possa fermarla
> così da non dover restare davanti allo schermo e da non dover rimediare a un doppio invio.

**Contesto.** È la storia in cui l'app comincia a fare la cosa per cui esiste. Fino a qui c'erano un archivio, un
messaggio e un permesso; adesso c'è la consegna, che è l'unico atto **irreversibile verso l'esterno** dell'intera
applicazione: un messaggio consegnato non si richiama.

Due proprietà contano più di ogni funzione. La prima è che una spedizione interrotta a metà — un riavvio, un guasto
del fornitore, un servizio che cade — deve **riprendere da dove era**, non ricominciare: un doppio invio è la cosa
peggiore che questa app possa fare a un iscritto, e il modo più veloce per raccogliere segnalazioni di posta
indesiderata (storia `0021`), che costano il recapito di **tutti** gli account. La seconda è il **ritmo**: mandare
diecimila messaggi in trenta secondi è il comportamento che i grandi fornitori di posta leggono come abuso.

## 2. Requisiti funzionali

1. **RF-1** — Al momento previsto la campagna passa a `in corso` e genera **una** riga di consegna per destinatario
   selezionato dal segmento, ricalcolato in quel momento e non alla programmazione.
2. **RF-2** — Ogni consegna è **idempotente per destinatario**: qualunque cosa accada — riavvio, ripetizione,
   doppia esecuzione della lavorazione — lo stesso iscritto riceve **al massimo una volta** quella campagna.
3. **RF-3** — La spedizione ha un **ritmo** configurabile a livello di piattaforma (messaggi al minuto), applicato
   per account e per dominio ricevente, per non farsi leggere come abuso dai grandi fornitori di posta.
4. **RF-4** — Una spedizione **in corso** si può mettere in **pausa** e **riprendere**; si può **annullare**, e in
   quel caso i destinatari non ancora serviti non ricevono nulla mentre quelli già serviti restano serviti — non
   esiste il richiamo di un messaggio consegnato, e l'interfaccia lo dice con chiarezza **prima** di annullare.
5. **RF-5** — La quota `messages_sent` si prenota **per destinatario**, immediatamente prima della consegna. Se si
   esaurisce a metà, la campagna si mette in **pausa** con stato leggibile: nessun messaggio perduto, nessun
   doppione, nessun addebito a sorpresa. Alla ripresa (quota rinnovata o piano cambiato) riparte dal punto esatto.
6. **RF-6** — Lo stato della spedizione è visibile in **tempo quasi reale**: quanti serviti, quanti in coda, quanti
   in errore, con la stima del tempo residuo.
7. **RF-7** — Un errore **temporaneo** del fornitore si ritenta con attese crescenti fino a un tetto; un errore
   **permanente** chiude quella consegna in errore e la passa alla gestione dei rimbalzi (storia `0021`), senza
   fermare la campagna.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `campaign` e `delivery` filtra per
  `tenant_id` preso dal token verificato. Anche la lavorazione asincrona porta con sé il `tenant_id` della campagna
  e non ne può servire un'altra: una lavorazione che perde il contesto dell'account deve **fallire**, non procedere.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/campaigns/v1/campaigns/{id}/pause`,
  `/resume`, `/cancel` e `GET /api/campaigns/v1/campaigns/{id}/progress`. Le transizioni di stato ammesse sono
  **solo** quelle della macchina a stati del §4 della descrizione: ogni altra combinazione risponde `409` con
  `application/problem+json`. Definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `delivery` (storia `0002`) con **vincolo di unicità** su
  `(tenant_id, campaign_id, subscriber_id)`: è il vincolo che rende impossibile il doppione anche se la logica
  sbaglia. Stato della consegna, numero di tentativi, momento e identificativo presso il fornitore. Schema
  `app_campaigns`, chiave primaria UUID versione 7, colonne di controllo, cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Schermata «Spedizione in corso» del modulo `campaigns`: barra di
  avanzamento, conteggi, pulsanti pausa/riprendi/annulla, con **conferma esplicita** sull'annullamento e la frase
  che spiega che i messaggi già partiti non tornano indietro. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — stati, motivi di pausa, testo della conferma di
  annullamento — dallo spazio-nomi `campaigns`, presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Prima di **ogni** consegna il servizio prenota **una** unità della metrica
  `messages_sent` (natura `flow`, finestra mensile); a quota esaurita la campagna si mette in pausa e la rotta di
  ripresa risponde `429` con l'indicazione del rimedio finché la quota non si rinnova o il piano non cambia. Con
  abbonamento `canceled` la spedizione non parte (`402`); con `past_due` parte, perché c'è tolleranza.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento **nuovo**: far partire una spedizione dalla chat
  passa comunque da `programma_invio` (storia `0018`), che restituisce l'esito e chiede conferma. `elenca_campagne`
  e `statistiche_campagna` (lettura) mostrano l'avanzamento. Mettere in pausa **non** è esposto alla chat in questa
  storia: è un'azione di emergenza e la si fa dove si vede cosa sta succedendo — scelta dichiarata.
- **RT-8 — Dati personali (§10).** La tabella `delivery` contiene un riferimento all'iscritto ed è quindi un dato
  che riguarda una persona: voce `delivery.*` nel manifesto
  `docs/compliance/manifests/campaigns.yaml` in italiano e inglese, con finalità «sapere se il messaggio è
  arrivato», base giuridica «esecuzione del contratto» e conservazione proposta 24 mesi; tabella presente in
  `exportData` e `purgeData`. Il **corpo del messaggio consegnato** non si conserva per destinatario: si conserva
  il modello, non la copia personalizzata.
- **RT-9 — Registrazione eventi (§14).** «Spedizione avviata», «consegna riuscita», «consegna fallita con codice»,
  «campagna in pausa per quota», «campagna annullata», con `tenant_id`, `app_id`, `user_id` (o l'identificativo
  della lavorazione automatica) e identificativo di correlazione. **Mai** l'indirizzo del destinatario: si registra
  l'identificativo dell'iscritto.

## 4. Criteri di accettazione

**CA-1 — La campagna parte e serve tutti una volta sola**
- **Dato** una campagna `programmata` per le 9:00 con 340 destinatari inviabili e quota sufficiente
- **Quando** arriva il momento
- **Allora** la campagna passa a `in corso`, vengono create 340 consegne, ognuna è servita **una sola volta** e al
  termine la campagna passa a `conclusa`

**CA-2 — Ripresa dopo un guasto, senza doppioni**
- **Dato** una spedizione interrotta dopo 120 consegne su 340
- **Quando** la lavorazione riparte
- **Allora** riprende dalla 121ª, le prime 120 **non** vengono servite di nuovo e il totale finale è 340 consegne

**CA-3 — Quota esaurita a metà**
- **Dato** un account con 100 invii residui e una campagna da 340 destinatari
- **Quando** la spedizione arriva al centesimo
- **Allora** la campagna si mette in **pausa** con motivo «quota esaurita», i 240 restanti **non** ricevono nulla,
  non viene addebitato nulla, e la ripresa risponde `429` finché la quota non si rinnova

**CA-4 — Annullamento**
- **Dato** una spedizione `in corso` a metà strada
- **Quando** l'utente annulla e conferma
- **Allora** le consegne non ancora servite vengono chiuse come «annullate», nessun altro messaggio parte, e
  l'interfaccia mostra quante persone **hanno già ricevuto** e non si possono richiamare

**CA-5 — Errore temporaneo e permanente**
- **Dato** un fornitore che risponde con un errore temporaneo su una consegna e con un rifiuto definitivo su
  un'altra
- **Quando** la spedizione procede
- **Allora** la prima viene ritentata con attese crescenti fino al tetto, la seconda viene chiusa in errore e
  passata alla gestione dei rimbalzi, e **in nessuno dei due casi** la campagna si ferma

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con campagne in corso
- **Quando** un utente di `A` chiede l'avanzamento della campagna di `B`, anche forzando il proprio `tenant_id` nel
  corpo
- **Allora** riceve `404` e nessun conteggio di `B` è visibile

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati della campagna (comprese le transizioni **non** ammesse) e sulla
      politica dei tentativi; prove di **integrazione** sulla spedizione completa con database effimero, migrazioni
      vere e fornitore di consegna sostituito da un doppio deterministico;
- [ ] prova specifica di **idempotenza**: la stessa lavorazione eseguita due volte produce lo stesso numero di
      consegne;
- [ ] prova di **isolamento fra account** su consegne e avanzamento;
- [ ] **prova end-to-end**: coprire ora — `[J-CAMPAIGNS]` (storia `0037`) esegue una spedizione completa su un
      segmento piccolo con indirizzi inventati sul dominio `.test`; registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per `delivery` in italiano e inglese, campi annotati `@PersonalData`,
      tabella in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato, con annotato perché l'unicità è un vincolo del database e non solo
      una regola applicativa;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo, con la motivazione scritta per la pausa non
      esposta;
- [ ] controllo automatico di **accessibilità** verde sulla schermata di spedizione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0017` | Non si spedisce da un dominio non verificato |
| Storia `0018` | La spedizione parte **solo** da uno stato raggiunto attraverso il controllo pre-volo, che viene ripetuto alla partenza |
| Scelta del fornitore di consegna della posta ([application-description.md](../application-description.md) §11.2) | È la storia che lo usa davvero: interfaccia di invio, forma degli identificativi e limiti di frequenza dipendono da lui |

## 7. Fuori ambito

- la verifica di contattabilità del **singolo** destinatario immediatamente prima della consegna: è la storia
  `0020`, che si innesta esattamente qui;
- la lettura dei ritorni del fornitore (rimbalzi, segnalazioni): è la storia `0021`;
- i canali diversi dalla posta elettronica: sono le storie `0022`-`0024`;
- la misurazione di aperture e clic: è la storia `0029`, facoltativa e spenta in partenza;
- l'invio di prova a sé stessi prima della campagna: appartiene alla composizione del messaggio (storia `0015`).

## 8. Punti aperti

- **Valore del ritmo di invio.** Quanti messaggi al minuto per account e per dominio ricevente non è un numero che
  si possa scegliere a tavolino: dipende dal fornitore e dalla reputazione dell'indirizzo di invio. Proposta:
  partire prudenti e alzare guardando i dati. Chiude lo sviluppatore col fornitore.
- **Finestra della quota e campagne a cavallo del mese.** Una spedizione che comincia il 31 e finisce il 1° tocca
  due finestre della metrica `flow`. La proposta è contare ogni consegna nella finestra in cui **avviene**, che è
  l'unica regola spiegabile al cliente. Da confermare.
- **Se la ripresa dopo una pausa per quota debba essere automatica** al rinnovo della finestra. Comoda, ma
  significa che qualcosa parte verso l'esterno senza che nessuno lo abbia deciso in quel momento. La proposta di
  questa storia è **manuale**, con un avviso al cliente. Chiude lo sviluppatore.
