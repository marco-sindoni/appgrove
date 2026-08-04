# 0026 — Redditività per commessa

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 05 — Avanzamento, margine e catena della suite
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0021`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena chiuso un lavoro
> voglio vedere in una schermata quanto ho fatturato, quanto mi è costato e cosa mi resta
> così da decidere se quel tipo di cliente conviene, invece di andare a sensazione.

**Contesto.** È la schermata che giustifica il prezzo dell'app. Il margine di commessa è una funzione riconosciuta
e comprata — la contabilità la offre dal lato opposto, dal conto verso il progetto
([Xero, controllo dei costi per commessa](https://www.xero.com/us/accounting-software/track-projects/job-costing/))
— ma nel segmento micro europeo è servita male: i concorrenti italiani che la fanno bene non pubblicano nemmeno
un prezzo ([application-description.md](../application-description.md) §2.1). È anche la schermata che in fase di
vendita va mostrata **per prima**, prima della lavagna.

La regola di prodotto che governa questa storia è una sola, e va tenuta ferma: **la redditività è per commessa e
per cliente, mai per persona**. Un elenco di persone ordinate per margine generato sarebbe una valutazione
automatizzata dei lavoratori, ed è precisamente ciò che questa app ha deciso di non essere (§6 della descrizione).

## 2. Requisiti funzionali

1. **RF-1** — Per un progetto si calcolano: **ricavo** (importo maturato dalle ore fatturabili + costi
   riaddebitati; se esiste un budget in importo, anche il confronto con quello), **costo** (ore totali —
   fatturabili e non — valorizzate al costo orario + costi esterni non riaddebitati), **margine** in valore e in
   percentuale.
2. **RF-2** — Ogni voce del conto è **apribile**: da «costo delle ore» si arriva alle ore che lo compongono, da
   «costi esterni» ai singoli costi. Un numero che non si può verificare non viene creduto.
3. **RF-3** — Il conto distingue la parte **già fatturata** (righe consegnate, storia 0022) da quella ancora da
   fatturare, perché sono due gradi di certezza diversi.
4. **RF-4** — Esiste una vista di riepilogo **per cliente**, che somma i progetti dello stesso cliente e ne mostra
   il margine complessivo.
5. **RF-5** — Il conto dichiara le proprie ipotesi in chiaro: costo orario usato, se ci sono ore senza tariffa, se
   il periodo è ancora aperto. Un margine calcolato su un mese non chiuso va etichettato come provvisorio.
6. **RF-6** — **Non esiste** alcuna vista di redditività per persona, né un ordinamento delle persone per margine,
   ore o rendimento.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Tutte le aggregazioni filtrano per `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/progetti/v1/projects/{id}/margin` e
  `GET /api/progetti/v1/reports/margin-by-customer`; errori in `application/problem+json`; OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: tutto **derivato** dalle righe di ore con tariffa congelata
  e dai costi. Gli importi si sommano in **centesimi**, mai in virgola mobile.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Rapporti → Redditività*, più il riquadro del margine nella scheda
  del progetto; le voci si aprono in linea; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi delle voci, spiegazioni delle ipotesi ed etichetta «provvisorio» in
  `en, it, fr, es, de`; importi e percentuali formattati secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Ruolo minimo: `admin`. Un `member` riceve `403` su
  queste rotte: il margine dell'azienda non è informazione di tutti.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `get_project_margin(id_progetto)`, **lettura**, con lo
  stesso controllo di ruolo (storia 0028). È lo strumento che rende il livello conversazionale utile per il
  titolare: «sul lavoro per Rossi ci abbiamo guadagnato?».
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. **Vincolo negativo da verificare con una prova**:
  nessuna rotta e nessuna schermata di questa storia deve poter restituire un aggregato economico raggruppato per
  persona.
- **RT-9 — Registrazione eventi (§14).** Nessun evento di dominio; le aggregazioni lente si registrano con durata
  e dimensione.

## 4. Criteri di accettazione

**CA-1 — Conto completo**
- **Dato** un progetto con 100 ore fatturabili a 50 €, 20 ore non fatturabili, costo orario 30 €, 450 € di costi
  riaddebitati e 200 € non riaddebitati
- **Quando** si apre la redditività
- **Allora** il ricavo è 5.450 €, il costo è 3.800 €, il margine è 1.650 € (30,3 %), e ogni voce si apre sul
  proprio dettaglio

**CA-2 — Fatturato contro da fatturare**
- **Dato** 60 ore già consegnate alla fatturazione e 40 no
- **Quando** si apre il conto
- **Allora** le due parti sono distinte e sommano al totale

**CA-3 — Ipotesi dichiarate**
- **Dato** un progetto con ore nel mese corrente non ancora chiuso
- **Quando** si apre il conto
- **Allora** compare l'etichetta «provvisorio» con il motivo

**CA-4 — Nessuna redditività per persona**
- **Dato** un utente con ruolo `admin`
- **Quando** cerca un raggruppamento per persona su qualunque rotta di questa storia
- **Allora** non esiste: nessun parametro lo consente e nessuna schermata lo mostra

**CA-5 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** chiama la rotta della redditività
- **Allora** riceve `403`

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede il riepilogo per cliente
- **Allora** vede solo i propri clienti e i propri progetti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sul calcolo del margine in centesimi (compresi arrotondamenti, ore senza tariffa,
      progetti senza costi) e di **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account** e prova della matrice dei ruoli;
- [ ] **prova end-to-end**: coprire ora — `[J-PROGETTI]` verifica il margine atteso a valle della consegna
      (storia 0031); voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la **prova automatica** che non esiste aggregazione per
      persona;
- [ ] **registro delle decisioni** compilato, con annotata la regola «margine per commessa e per cliente, mai per
      persona» e il perché;
- [ ] controllo automatico di **accessibilità** verde sulla schermata della redditività;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0018` | Servono tariffa congelata, costo orario e distinzione fatturabile/non fatturabile |
| Storia `0021` | Il confronto con il budget completa il conto |
| Storia `0024` | Senza i costi esterni il margine è falso |
| Storia `0022` | La distinzione fra fatturato e da fatturare viene dai lotti consegnati |

## 7. Fuori ambito

- il conto economico dell'azienda: è contabilità, non è questa app;
- i costi indiretti e le quote di struttura ripartite sulle commesse: fuori perimetro, e sarebbero un modo per
  produrre numeri che nessuno sa spiegare;
- la redditività per tipo di lavoro o per servizio: rimandata, arriva quando ci sarà un catalogo servizi condiviso.

## 8. Punti aperti

- **Costo orario unico d'account**: è la semplificazione decisa nella storia 0018. Rende il costo approssimato in
  aziende dove il socio e l'apprendista costano molto diversamente. Introdurre un costo per persona è possibile ma
  è una decisione dello sviluppatore con conseguenze sulla classificazione dei dati (somiglia a un dato
  retributivo): fino ad allora, il conto deve **dichiarare** che usa un costo medio.
