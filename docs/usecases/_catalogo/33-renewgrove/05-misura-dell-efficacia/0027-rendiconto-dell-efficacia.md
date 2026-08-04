# 0027 — Rendiconto dell'efficacia

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 05 — Misura dell'efficacia
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0025`, `0026` — servono il gruppo di confronto e i motivi, altrimenti il rendiconto è una colonna sola
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che a fine anno deve decidere se questo abbonamento vale i suoi soldi
> voglio una pagina che dica quanti clienti a rischio ho avuto, su quanti sono intervenuto, quanti ne ho tenuti
> rispetto a chi non ho toccato, e quanto mi è costato in sconti e proroghe
> così da rinnovare per un motivo, o disdire per un motivo.

**Contesto.** Questa è la pagina su cui il prodotto si vende e su cui, se serve, si condanna. La
[descrizione](../application-description.md) lo scrive al §8: «alla fine dell'epica il prodotto sa dimostrare — o
smentire — il proprio valore, che è il suo unico vero argomento di vendita», e al §11 aggiunge la frase che questa
storia deve prendere sul serio: «un prodotto che dice *i tuoi interventi non hanno cambiato nulla* è più difendibile
di uno che tace». Da qui discendono due scelte che sembrano andare contro l'interesse commerciale e che invece lo
proteggono: la sezione **«che cosa questo numero non dimostra»** è un requisito a schermo, non una nota a piè di
pagina; e il rendiconto conserva **solo conteggi aggregati**, così che la cancellazione dei dati di una persona non
riscriva all'indietro la storia della misura (§6).

## 2. Requisiti funzionali

1. **RF-1** — Per un **periodo** scelto (mese, trimestre, anno) il rendiconto mostra: quanti rapporti sono entrati in
   fascia a rischio, quanti **interventi confermati**, quanti esiti valutati, e — separati per gruppo **intervenuto**
   e **di confronto** — quanti *trattenuti*, quanti *persi*, quanti *ancora aperti*. Ogni percentuale è mostrata
   **accanto al proprio denominatore**: mai una percentuale sola.
2. **RF-2** — Il rendiconto mostra il **costo in concessioni**: la somma delle **offerte di trattenuta autorizzate**
   nel periodo (`0022`), scomposta per tipo. Le concessioni monetarie (sconto, cambio condizioni) si sommano in
   valuta; le proroghe si contano in **giorni concessi** e si mostrano a parte, **senza** essere convertite in
   denaro: una conversione inventata sarebbe una cifra falsa dentro la pagina che deve essere la più onesta dell'app.
3. **RF-3** — Esiste una sezione **«che cosa questo numero non dimostra»**, sempre visibile **nel corpo della
   pagina** — non in un suggerimento a comparsa, non in fondo — con tre affermazioni obbligatorie: (a) il confronto
   **non dimostra un rapporto di causa ed effetto**; (b) i numeri sono **piccoli**, e una differenza fra i gruppi può
   essere caso; (c) il gruppo di confronto **non è casuale** e chi interviene sceglie di norma i clienti che conosce
   meglio. Quando la coorte non si è formata (`0025`, **RF-3**), la sezione lo dice al posto del confronto.
4. **RF-4** — Il rendiconto mostra la **distribuzione dei motivi di abbandono** (`0026`) sui rapporti persi del
   periodo, con accanto il numero di persi **senza motivo registrato**.
5. **RF-5** — Un periodo **concluso** si **chiude**: i conteggi vengono calcolati una volta e conservati in una
   tabella di sintesi che contiene **solo numeri** — nessun identificativo di rapporto, nessuna etichetta, nessuna
   riga riconducibile a una persona. Un periodo chiuso **non si ricalcola**; i periodi ancora in corso si calcolano
   a richiesta.
6. **RF-6** — La pagina dichiara a schermo che **i periodi chiusi non si riscrivono**, e che quindi una richiesta di
   cancellazione dei dati di una persona (`0032`) toglie il rapporto e i suoi esiti ma **non** modifica i conteggi
   già chiusi.
7. **RF-7** — Il rendiconto si **esporta** in un file leggibile da una persona, e il file contiene **dentro di sé**
   la sezione **«che cosa questo numero non dimostra»**: una cifra che gira senza il proprio avvertimento è il modo
   in cui l'avvertimento smette di esistere.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e ogni chiusura di periodo filtra per `tenant_id` preso dal
  token verificato; il calcolo di un account non attraversa mai righe di un altro; un `tenant_id` che arrivasse dal
  corpo della richiesta o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/fidelizzazione/v1/rendiconto?periodo=…` e
  `GET /api/fidelizzazione/v1/rendiconto/{periodo}/esportazione`; corpo e parametri validati; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__rendiconto_efficacia.sql` sullo schema `app_fidelizzazione`:
  tabella `rendiconto_efficacia` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica; colonne di **soli conteggi e importi aggregati** per periodo e gruppo. Un vincolo
  strutturale e una prova impediscono che vi compaia un riferimento a `rapporto`: è la garanzia tecnica del **RF-5**,
  non una promessa. Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione **Efficacia** del modulo `fidelizzazione`; dati letti con il client
  generato; solo token del sistema di design; tema chiaro e scuro. I colori-categoria `green`, `amber` e `red`
  restano riservati alle fasce di rischio e non si usano per «buono/cattivo» sui risultati; la lettura non dipende
  dal colore.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — comprese le tre affermazioni del **RF-3**, che sono la
  parte che non si può sbagliare — passano dallo spazio-nomi `fidelizzazione` e sono presenti in `en, it, fr, es,
  de`. Anche l'esportazione del **RF-7** esce nella lingua di chi la chiede.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: il rendiconto legge. Con abbonamento `canceled`
  risponde `402`. Il **costo in concessioni** è materia economica: lo vedono `owner` e `admin`; un `member` vede il
  resto del rendiconto e riceve un **rifiuto esplicito** (`403`) sulla parte economica, non un numero ridotto senza
  spiegazione.
- **RT-7 — Esposizione conversazionale (§12).** Il rendiconto è la sorgente dello strumento di lettura
  `efficacia_degli_interventi(periodo, tipo?)`, dichiarato nella storia `0028`: restituisce trattenuti, persi,
  confronto con il gruppo di riferimento, costo delle concessioni **e i tre limiti del RF-3**, che viaggiano con i
  numeri e non a parte. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non
  ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: `rendiconto_efficacia` contiene solo conteggi e
  importi aggregati, senza righe riconducibili a persone, ed è dichiarata nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml` come **esclusione motivata** da `exportData` e `purgeData`, con la
  motivazione scritta in italiano e inglese: è la scelta che permette a una cancellazione di essere completa senza
  riscrivere la misura (§6 della descrizione). L'esportazione del **RF-7** non contiene etichette di rapporti.
- **RT-9 — Registrazione eventi (§14).** `rendiconto calcolato (periodo)`, `periodo chiuso (periodo, numerosità)`,
  `rendiconto esportato`, `parte economica negata per ruolo`, con `tenant_id`, `app_id`, `user_id` e identificativo
  di correlazione, senza dati personali.
- **RT-10 — Prove (§11).** Unità sui conteggi per gruppo e sulla somma delle concessioni (comprese le proroghe, che
  non si sommano al denaro); integrazione sulla chiusura di periodo con database effimero e migrazioni vere; prova
  che dopo una purga (`0032`) i conteggi di un periodo chiuso sono **identici**; isolamento fra due account;
  controllo automatico di accessibilità sulla schermata.

## 4. Criteri di accettazione

**CA-1 — Il rendiconto racconta i due gruppi**
- **Dato** un periodo con 40 rapporti entrati in fascia a rischio, 12 interventi confermati e coorti formate
- **Quando** si apre la sezione Efficacia per quel periodo
- **Allora** compaiono, con i denominatori accanto, trattenuti/persi/ancora aperti per il gruppo intervenuto e per
  quello di confronto, e il costo in concessioni scomposto per tipo

**CA-2 — «Che cosa questo numero non dimostra» è a schermo**
- **Dato** un rendiconto con entrambi i gruppi valutati, in una qualunque delle cinque lingue
- **Quando** si apre la pagina e si esporta il file
- **Allora** le tre affermazioni — nessun rapporto di causa ed effetto, numeri piccoli, gruppo non casuale — sono
  nel corpo della pagina **e** dentro il file esportato

**CA-3 — Senza gruppo di confronto non si finge**
- **Dato** un periodo in cui nessuna coorte ha raggiunto la soglia minima
- **Quando** si apre il rendiconto
- **Allora** al posto del confronto compare «nessun gruppo di confronto formato in questo periodo» con la ragione, e
  nessuna percentuale di confronto viene mostrata

**CA-4 — Le proroghe non diventano denaro**
- **Dato** un periodo con tre sconti autorizzati per 450 € complessivi e due proroghe per 60 giorni complessivi
- **Quando** si legge il costo in concessioni
- **Allora** compaiono «450 €» e «60 giorni di proroga» come voci distinte, e nessun totale unico

**CA-5 — Una cancellazione non riscrive la storia**
- **Dato** un periodo chiuso con 12 trattenuti su 40
- **Quando** si esegue la purga dei dati di uno dei rapporti di quel periodo (`0032`)
- **Allora** il rapporto e i suoi esiti spariscono, i conteggi del periodo chiuso restano 12 su 40, e la pagina lo
  dichiara

**CA-6 — Ruolo sulla parte economica**
- **Dato** un utente con ruolo `member`
- **Quando** chiede il rendiconto e poi il costo in concessioni
- **Allora** riceve il rendiconto senza la parte economica e un `403` esplicito su di essa, con un messaggio che
  dice chi può vederla

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sui conteggi e sulle somme delle concessioni, e di **integrazione** sulla chiusura di
      periodo, con database effimero e migrazioni vere;
- [ ] prova che una **purga non altera** i conteggi di un periodo chiuso;
- [ ] prova di **isolamento fra account** su lettura, chiusura ed esportazione;
- [ ] **prova end-to-end**: *rimando* — il rendiconto compare nel percorso `[J-FIDELIZZAZIONE]` della storia `0030`
      come lettura finale; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta la voce `da-coprire` con
      motivo («richiede finestre di osservazione concluse») e storia proprietaria `0030`;
- [ ] **traduzioni** in `en, it, fr, es, de`, comprese le tre affermazioni del **RF-3** e l'esportazione;
- [ ] **manifesto dei dati**: nessuna voce nuova; `rendiconto_efficacia` dichiarata come esclusione motivata in
      italiano e inglese;
- [ ] **registro delle decisioni** compilato: solo aggregati e perché, periodi chiusi che non si ricalcolano,
      proroghe non convertite in denaro, restrizione di ruolo sulla parte economica;
- [ ] contratto degli **strumenti conversazionali**: `efficacia_degli_interventi` dichiarato nella `0028` con i
      limiti compresi nel risultato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la descrizione tratta l'effetto della cancellazione sulla misura (§6).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0025` (gruppo di confronto) | senza il gruppo di confronto il rendiconto ha una colonna sola, e una colonna sola non misura |
| storia `0026` (motivi di abbandono) | la distribuzione dei motivi è una delle quattro parti della pagina |
| storia `0022` (offerte di trattenuta) | il costo in concessioni somma le offerte **autorizzate**: senza autorizzazione non c'è costo da contare |
| storia `0032` (chiusura del contratto dati) | è la storia che verifica che la purga non tocchi gli aggregati: qui si costruisce la proprietà, là si prova |

## 7. Fuori ambito

- il **ricavo trattenuto** in euro: richiederebbe di conoscere il valore di ogni rapporto, che l'app non ha e non
  deve chiedere (§5.1 della descrizione: un dato dichiarato dal cliente non è un dato). Qui si contano rapporti e
  concessioni, non fatturato salvato;
- la **previsione** di quanti se ne perderanno: è previsione sull'aggregato, cioè materia di SubGrove (`0027`-`0030`)
  e di 20 InsightGrove;
- il confronto fra **periodi** o fra **tipi di intervento**: numerosità insufficiente in questo segmento (§7 della
  storia `0025`);
- l'invio periodico del rendiconto per posta elettronica a chi lavora: è un avviso interno di piattaforma, non una
  funzione di questa storia;
- la lettura trasversale dell'efficacia insieme agli altri numeri dell'attività: è di **20 InsightGrove**, che
  riceve aggregati.

## 8. Punti aperti

- **Il rendiconto arriva troppo tardi per la prova gratuita.** È il rischio commerciale numero uno del prodotto,
  già registrato come punto aperto n. 3 della [descrizione](../application-description.md): al quattordicesimo
  giorno questa pagina è vuota, perché nessuna finestra di osservazione si è chiusa. Un rendiconto anticipato sui
  primi esiti sarebbe possibile, ma mostrerebbe numeri minuscoli proprio nel momento in cui il cliente decide se
  fidarsi. Non è una scelta tecnica. Chiude: **sviluppatore** — prezzi e direzione commerciale.
- **Se conservare solo aggregati sia la scelta giusta rispetto ai diritti dell'interessato.** La proposta (§6 della
  descrizione, «va confermata») è che i conteggi chiusi sopravvivano alla cancellazione perché non contengono dati
  riferiti a persone. È difendibile, ma va **detto nel manifesto** invece di essere dato per scontato: un aggregato
  su numeri molto piccoli può, in casi limite, restare riconducibile a una persona. Chiude: **sviluppatore** (dati
  personali) con la **revisione legale**
  ([docs/_REVISIONE-LEGALE.md](../../../../_REVISIONE-LEGALE.md)).
