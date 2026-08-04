# 0008 — Collegamento e revoca di una fonte

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 02 — Arrivo dei dati dalle altre app
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0003`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare dell'account
> voglio decidere io quali delle mie applicazioni appgrove alimentano InsightGrove, e poter revocare
> così da non trovarmi i numeri di un'area aziendale dentro un cruscotto che ho aperto ad altre persone senza
> averlo scelto.

**Contesto.** InsightGrove potrebbe raccogliere tutto ciò che passa sul bus per il proprio account, e sarebbe
sbagliato: il collegamento è un **atto volontario**, una fonte per volta, di chi ha il ruolo per farlo. È la
quarta proprietà che regge l'isolamento (§4.2 della [descrizione](../application-description.md)) ed è anche la
risposta pratica al principio di minimizzazione: non si raccoglie ciò che non si è scelto di raccogliere.
La revoca è il gesto opposto ed è **distruttivo**: cancella fisicamente i fatti ricevuti da quella fonte.

## 2. Requisiti funzionali

1. **RF-1** — La sezione **Fonti** elenca le applicazioni appgrove a cui l'account è abilitato e che sanno
   pubblicare fatti, ciascuna con il proprio stato: collegata, non collegata, sospesa.
2. **RF-2** — Un utente con ruolo `owner` o `admin` può **collegare** una fonte. Dal momento del collegamento i
   fatti di quella fonte vengono accettati (storia 0007) e viene chiesto il ripopolamento dello storico
   (storia 0009).
3. **RF-3** — Prima di collegare, l'interfaccia dice **che cosa entrerà**: l'elenco delle misure e delle
   dimensioni che quella fonte dichiara di pubblicare, con il loro significato. Nessun collegamento al buio.
4. **RF-4** — Un utente con ruolo `owner` o `admin` può **revocare** una fonte. La revoca richiede una conferma
   esplicita che mostra **quanti fatti verranno cancellati** e **quali indicatori smetteranno di produrre
   valori**.
5. **RF-5** — La revoca cancella **fisicamente** i fatti e le eventuali etichette di dimensione provenienti da
   quella fonte, e lascia una riga di prova nel registro delle purghe.
6. **RF-6** — Dopo una revoca, gli indicatori che dipendevano **solo** da quella fonte non producono un valore
   più piccolo: **non producono alcun valore**, e dicono perché.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `fonte` filtra per `tenant_id` preso dal
  gettone verificato; un `tenant_id` che arrivasse dal corpo della richiesta viene ignorato. Il collegamento di
  un account non ha alcun effetto su un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/insights/v1/fonti`,
  `POST /api/insights/v1/fonti/{app}/collegamento`, `DELETE /api/insights/v1/fonti/{app}/collegamento`;
  corpo validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Fonti` del modulo `insights`; dati letti con il client generato;
  solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe della sezione, compresi i testi di conferma della revoca,
  esistono in `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** Collegare e revocare richiedono ruolo `owner` o `admin`; un `member` vede
  l'elenco in sola lettura e riceve `403` se tenta. Il collegamento **non consuma quota**: è illimitato in ogni
  piano (§3 della descrizione).
- **RT-8 — Dati personali (§10).** La revoca è una **cancellazione fisica**, non una pseudonimizzazione:
  sostituire le etichette con codici non è cancellare. Le tabelle toccate sono le stesse dichiarate in
  `purgeData`.
- **RT-14 — Registrazione eventi (§14).** «Fonte collegata», «fonte revocata» con `tenant_id`, `app_id`,
  `user_id`, identificativo di correlazione e il conteggio dei fatti cancellati; senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Collegamento**
- **Dato** un utente `owner` di un account abilitato a BillGrove e a InsightGrove
- **Quando** apre Fonti, legge l'elenco delle misure che BillGrove pubblica e collega la fonte
- **Allora** la fonte risulta collegata, e i fatti pubblicati da BillGrove per quell'account cominciano a essere
  accettati

**CA-2 — Revoca con conferma informata**
- **Dato** una fonte collegata da cui sono arrivati 4.312 fatti, da cui dipendono tre indicatori
- **Quando** l'utente `owner` chiede di revocarla
- **Allora** vede una conferma che dice «4.312 fatti verranno cancellati; tre indicatori smetteranno di produrre
  valori: …» e solo dopo la conferma esplicita la revoca avviene

**CA-3 — Dopo la revoca l'indicatore tace, non mente**
- **Dato** una fonte revocata da cui dipendeva l'indicatore «fatturato emesso»
- **Quando** si apre il cruscotto
- **Allora** l'indicatore non mostra un numero più piccolo: mostra «non calcolabile — fonte fatturazione non
  collegata» con il rimando alla sezione Fonti

**CA-4 — Un `member` non può collegare**
- **Dato** un utente con ruolo `member`
- **Quando** tenta di collegare o revocare una fonte
- **Allora** riceve `403` e l'elenco resta in sola lettura

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, il primo con BillGrove collegata e il secondo no
- **Quando** un utente di `A` legge l'elenco delle fonti forzando l'identificativo di `B`
- **Allora** vede lo stato di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio di ciò che verrà cancellato e di **integrazione** sulla revoca, con
      database effimero e migrazioni vere, verificando che le righe siano **fisicamente** sparite;
- [ ] prova di **isolamento fra account** sulla risorsa delle fonti;
- [ ] prova sulla **matrice dei ruoli**: `owner` e `admin` sì, `member` no;
- [ ] **prova end-to-end**: *coprire ora* — il collegamento di una fonte è il primo passo del percorso
      `[J-INSIGHTS]`, creato qui e completato dalla storia 0034; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato: la revoca cancella tutto ciò che il manifesto dichiara per quella fonte;
- [ ] **registro delle decisioni** compilato, con la scelta della cancellazione fisica alla revoca e il perché;
- [ ] contratto degli **strumenti conversazionali**: `collega_fonte` e `scollega_fonte` dichiarati come
      **scrittura irreversibile con conferma obbligatoria** (storia 0032);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0003` | serve la sezione Fonti |
| storia `0007` | il collegamento serve a filtrare i fatti in ingresso |
| storia `0009` | il collegamento **innesca** il ripopolamento: le due si consegnano insieme |

## 7. Fuori ambito

- il ripopolamento vero e proprio: storia 0009;
- lo stato di salute e il ritardo di una fonte collegata: storia 0010;
- la sospensione temporanea di una fonte senza cancellare: vedi punti aperti.

## 8. Punti aperti

- **Serve una «sospensione» oltre a collegamento e revoca?** Una sospensione fermerebbe l'ingresso dei fatti
  senza cancellare lo storico — utile quando si vuole solo smettere di aggiornare. Raccomandazione: **sì, e la
  sospensione è lo stato intermedio raccomandato per l'interfaccia**, mentre la revoca resta l'atto distruttivo,
  esplicito e raro. Il modello dati la prevede già (stato `sospesa`). Chiude: **sviluppatore**.
- **Che cosa succede se il cliente disdice l'abbonamento all'app sorgente** e la fonte resta collegata? I fatti
  smettono di arrivare, e lo storico resta. Raccomandazione: **lo storico resta** ed è marcato come non più
  aggiornato (storia 0010), perché cancellarlo sarebbe una perdita che il cliente non ha chiesto. Chiude:
  **sviluppatore**.
