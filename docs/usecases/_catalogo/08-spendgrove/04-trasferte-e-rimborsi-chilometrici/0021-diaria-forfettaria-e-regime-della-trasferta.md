# 0021 — Diaria forfettaria e regime della trasferta

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 04 — Trasferte e rimborsi chilometrici
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0020`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi tiene l'amministrazione
> voglio poter scegliere, per una trasferta, se rimborsare le spese documentate oppure una somma fissa al giorno
> (o entrambe le cose)
> così da applicare quello che l'azienda ha stabilito senza tenere il conto a parte su un altro foglio.

**Contesto.** Non tutte le aziende rimborsano allo stesso modo. Alcune pagano solo ciò che è documentato (regime
analitico), altre danno una somma fissa giornaliera (forfettario), altre mescolano — per esempio l'albergo
documentato più una diaria per i pasti (misto). La scelta cambia i numeri, cambia il trattamento fiscale e cambia
che cosa il collaboratore deve consegnare. Va fatta dopo la trasferta e la verifica di tracciabilità perché si
appoggia a entrambe: senza il contesto della trasferta la diaria non ha giornate su cui calcolarsi, e senza la
valutazione fiscale non si sa se la somma resta esente.

## 2. Requisiti funzionali

1. **RF-1** — L'account definisce le proprie **diarie**: importo giornaliero, se è per l'Italia o per l'estero,
   periodo di validità, e a quali categorie sostituisce il rimborso documentato.
2. **RF-2** — Ogni trasferta dichiara il **regime**: `analitico`, `forfettario` o `misto`; il regime si può cambiare
   finché nessuna spesa della trasferta è entrata in una nota inviata.
3. **RF-3** — In regime forfettario o misto l'app calcola la diaria spettante (giorni × importo, con la regola sui
   giorni parziali dichiarata) e la aggiunge come spesa della trasferta.
4. **RF-4** — In regime forfettario l'app **avvisa** se nella trasferta compaiono anche spese documentate delle
   categorie coperte dalla diaria: è la combinazione che genera più errori, e va vista prima dell'approvazione.
5. **RF-5** — L'app **segnala** quando la diaria supera l'importo esente configurato, indicando l'eccedenza; non
   calcola imposte e non produce alcun documento fiscale.
6. **RF-6** — Se non è configurata nessuna diaria, l'app funziona come prima: il regime analitico è quello
   predefinito e non richiede alcuna impostazione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Diarie, regimi e calcoli filtrano per `tenant_id` preso dal token
  verificato; la definizione delle diarie è riservata ai ruoli `approva` e `amministra`.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/notespese/v1/diarie` e il campo `regime` sulla
  trasferta; `POST /api/notespese/v1/trasferte/{id}/diaria` per generare la spesa di diaria; errori in
  `application/problem+json` con `409` se la trasferta ha spese già in una nota inviata; definizione OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V18__diarie.sql`: tabella `diaria` con `tenant_id`, chiave UUID versione
  7, importo giornaliero, ambito, validità, categorie coperte, soglia di esenzione, colonne di controllo e
  cancellazione logica; colonna `regime` sulla trasferta. Sulla spesa di diaria si conservano **i fattori del
  calcolo** (giorni, importo unitario, versione della diaria), non solo il risultato.
- **RT-4 — Modulo frontend (§3, §5).** Selettore del regime nella scheda della trasferta, con una spiegazione in
  due righe di cosa cambia; in *Impostazioni* la gestione delle diarie. Solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi dei regimi, spiegazioni e avvisi passano dallo spazio-nomi `notespese` e sono
  presenti in `en, it, fr, es, de`. Come per la storia `0020`, i testi dichiarano a quale giurisdizione si
  riferiscono.
- **RT-6 — Varchi e quota (§6, §7).** La spesa di diaria, quando viene confermata, consuma una unità della metrica
  `receipts` come tutte le altre: è un documento di spesa lavorato.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: scegliere il regime di una trasferta
  è una decisione con conseguenze fiscali e resta un gesto dell'interfaccia. In lettura, `riepilogo_spese` distingue
  la quota documentata dalla quota forfettaria.
- **RT-8 — Dati personali (§10).** Nessuna categoria nuova rispetto a trasferta e spesa; la diaria è un importo
  legato a una persona e a delle giornate, già coperto dalle voci esistenti — voce **aggiornata**, non nuova, nel
  manifesto in italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `regime scelto`, `diaria calcolata`, `eccedenza segnalata`
  portano `tenant_id`, `app_id`, `user_id`, identificativo di correlazione, identificativi e numero di giorni —
  non gli importi.

## 4. Criteri di accettazione

**CA-1 — Diaria calcolata**
- **Dato** una diaria di 46,48 € al giorno per l'Italia e una trasferta di tre giorni in regime forfettario
- **Quando** si genera la diaria
- **Allora** compare una spesa di 139,44 € con i fattori del calcolo visibili, legata alla trasferta

**CA-2 — Regime misto**
- **Dato** una trasferta in regime misto con diaria sui pasti e albergo documentato
- **Quando** si genera la diaria
- **Allora** l'albergo resta una spesa documentata e la diaria copre solo le categorie dichiarate

**CA-3 — Sovrapposizione segnalata**
- **Dato** una trasferta in regime forfettario · **Quando** vi si assegna una spesa di categoria Vitto documentata
- **Allora** compare l'avviso di sovrapposizione, visibile anche nella schermata di approvazione, e l'operazione
  **non** è bloccata

**CA-4 — Eccedenza rispetto all'esente**
- **Dato** una diaria di 70 € al giorno e una soglia di esenzione configurata a 46,48 €
- **Quando** si genera la diaria per un giorno
- **Allora** l'app segnala l'eccedenza di 23,52 € e dice che il trattamento della parte eccedente non è compito suo

**CA-5 — Regime bloccato dopo l'invio**
- **Dato** una trasferta con spese già in una nota inviata · **Quando** si tenta di cambiarne il regime
- **Allora** l'operazione è respinta con `409`

**CA-6 — Isolamento fra account**
- **Dato** due account con diarie diverse · **Quando** ciascuno calcola una trasferta di due giorni
- **Allora** ognuno ottiene l'importo della propria diaria

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dei giorni (compresi i giorni parziali) e sulla rilevazione della
      sovrapposizione; di **integrazione** sulla generazione della diaria con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e di ruolo su diarie e regimi;
- [ ] **prova end-to-end**: *rimando* alla storia `0031` — il percorso `[J-NOTESPESE]` copre il regime analitico,
      che è il caso predefinito; il ramo forfettario è aggiunto lì come variante, con la voce nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese;
- [ ] **registro delle decisioni** compilato, con la scelta di non calcolare imposte e di limitarsi a segnalare;
- [ ] contratto degli **strumenti conversazionali**: nessuno di scrittura, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0018` | La diaria si calcola sui giorni di una trasferta |
| `0020` | La segnalazione dell'eccedenza usa lo stesso meccanismo degli avvisi fiscali |

## 7. Fuori ambito

- Il calcolo delle imposte e dei contributi sulla parte eccedente: è materia di busta paga (catalogo 10), e questa
  app non la tocca.
- Le soglie di esenzione **fornite** dal prodotto: sono valori di legge che cambiano e valgono per giurisdizione.
  Le configura il cliente con il suo consulente; l'app le applica.
- Il rimborso a piè di lista con anticipo di cassa: caso reale ma non coperto in questo giro.

## 8. Punti aperti

- 🛑 **Le soglie di esenzione sono numeri di legge**: l'app le tratta come configurazione del cliente proprio per
  non assumersi la responsabilità di tenerle aggiornate. Va deciso se questa è la postura definitiva o se il
  prodotto vuole fornirle (con la responsabilità che ne consegue) — stessa decisione della storia `0020`, punto
  aperto sulle regole fiscali. La chiude lo sviluppatore.
- **Giorni parziali**: se il primo e l'ultimo giorno di trasferta contino per intero o per metà è una regola che
  varia per contratto collettivo. L'app la rende configurabile, ma il valore predefinito va scelto da chi conosce il
  caso d'uso prevalente.
