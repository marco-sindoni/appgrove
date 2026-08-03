# 0024 — Regole di provvigione

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 06 — Operatori, provvigioni e rendimento
**Storia**: `0024` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha concordato con Sara il 50 % sui servizi e il 10 % sulla rivendita
> voglio scrivere quella regola una volta sola e vederla applicata da sola a ogni conto chiuso
> così da non rifare i conti a mano ogni mese e da non dover ricordare da quando vale l'accordo nuovo.

**Contesto.** Il **percentualista** è una figura ordinaria del settore: le fonti descrivono uno split «50/50
classico, 70/30 nei saloni che affittano solo l'utilizzo della sedia», e un salone tipo fatto dal titolare più due
o tre fra dipendenti e percentualisti (§2.5 della [descrizione](../application-description.md)). Oggi quel conto si
fa con la calcolatrice a fine mese, e si discute. Questa storia mette la regola dentro l'applicazione; il conteggio
del periodo è la storia `0025`.

⚠️ **Che cosa è, e che cosa non è.** Una regola di provvigione è una **condizione economica del rapporto di
lavoro o di collaborazione**: è un dato personale delicato quanto uno stipendio, e va trattato come tale — non
visibile a tutti, non esposto agli strumenti conversazionali della persona sbagliata. SalonGrove **non calcola
buste paga** (app 10 PayGrove, esclusa dal catalogo): calcola quanto ha prodotto ciascuno e quanto gli spetta
secondo la regola concordata, e si ferma lì.

## 2. Requisiti funzionali

1. **RF-1** — Una regola di provvigione lega un **operatore** a una **base** (servizi, rivendita, o entrambe con
   percentuali diverse) e a una **percentuale**, con una **validità da/a**: cambiare accordo significa chiudere la
   regola vecchia e aprirne una nuova, mai riscrivere quella esistente.
2. **RF-2** — La percentuale può essere **a scaglioni** sul prodotto del periodo (per esempio 40 % fino a 2.000 €,
   50 % oltre), con scaglioni contigui e senza sovrapposizioni.
3. **RF-3** — La regola si applica sulla base **al netto degli sconti** e al netto di ciò che è stato pagato con un
   pacchetto già incassato in precedenza, secondo un'impostazione dell'account che dichiara quale delle due basi si
   usa; l'impostazione predefinita è «al netto degli sconti, pacchetti compresi al momento dell'utilizzo».
4. **RF-4** — Un operatore **senza regola non matura nulla** e non fa fallire niente: il salone che non usa le
   provvigioni non deve configurare niente (vincolo di progetto, §2.5 della descrizione).
5. **RF-5** — Le regole sono visibili a chi **amministra** l'account e, per la propria, all'operatore interessato.
   Nessun operatore vede la regola di un collega.
6. **RF-6** — Un conto chiuso porta con sé la regola **applicata in quel momento**: modificare o chiudere una
   regola non cambia il maturato dei conti già chiusi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `regola_provvigione` filtra per `tenant_id`
  preso dal token verificato; l'operatore indicato deve appartenere allo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/<app>/v1/regole-provvigione`,
  `POST /api/<app>/v1/regole-provvigione/{id}/chiusura`; corpo validato (percentuali fra 0 e 100, scaglioni
  contigui e crescenti, validità non sovrapposta per lo stesso operatore e la stessa base); errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema dell'app: tabelle `regola_provvigione` e
  `regola_provvigione_scaglione` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica; percentuali in **punti base interi** (5000 = 50,00 %) per non trascinare errori di
  arrotondamento; vincolo di non sovrapposizione delle validità verificato nel servizio e provato.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Operatori* del modulo: elenco delle persone con la regola vigente,
  form di apertura di una regola nuova che mostra **da quando** varrà e che cosa succede a quella in corso. Solo
  token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette delle basi, testi degli scaglioni e messaggi di validazione presenti in
  `en, it, fr, es, de`, con i formati numerici della lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Funzione accesa dal piano che comprende le
  provvigioni; a piano insufficiente `402`. Con abbonamento in `past_due` resta leggibile.
- **RT-7 — Dati personali (§10).** Voce di manifesto `regola_provvigione` in italiano e inglese: interessato =
  chi lavora nel salone; categoria «condizione economica del rapporto di lavoro»; finalità «calcolare quanto
  spetta»; base «esecuzione del contratto di lavoro o di collaborazione», trattata da appgrove **per conto del
  salone**; durata proposta: finché la regola è valida, più 24 mesi. Campi annotati `@PersonalData`; tabelle in
  esportazione e cancellazione (storie `0014` e `0032`).
- **RT-8 — Esposizione conversazionale (§12).** **Nessuno strumento** legge o scrive le regole di provvigione: è
  una condizione economica di una persona, e la sua lettura in chat esporrebbe un dato retributivo a chiunque
  abbia in mano la sessione. Divieto dichiarato nel contratto degli strumenti (storia `0028`).
- **RT-9 — Registrazione eventi (§14).** `regola aperta`, `regola chiusa` con `tenant_id`, `app_id`, `user_id`,
  correlazione e identificativo dell'operatore — **mai** la percentuale e mai il nome.

## 4. Criteri di accettazione

**CA-1 — La regola si applica da sola**
- **Dato** Sara con regola 50 % sui servizi e 10 % sulla rivendita, valida dal primo del mese
- **Quando** si chiude un conto con 80 € di servizi e 30 € di rivendita attribuiti a lei
- **Allora** maturano 40 € + 3 € = 43 €

**CA-2 — Gli scaglioni**
- **Dato** una regola 40 % fino a 2.000 € di prodotto nel periodo e 50 % oltre
- **Quando** nel periodo l'operatore arriva a 2.400 €
- **Allora** matura 800 € + 200 € = 1.000 €

**CA-3 — Il cambio di accordo non riscrive il passato**
- **Dato** conti già chiusi con la regola al 40 %
- **Quando** si apre una regola nuova al 50 % da oggi
- **Allora** il maturato dei conti chiusi resta quello di prima, e solo i conti successivi usano il 50 %

**CA-4 — Senza regola non succede niente**
- **Dato** un operatore senza alcuna regola
- **Quando** gli si attribuiscono righe e si chiude il conto
- **Allora** la chiusura riesce, non matura nulla per lui e nessun errore viene mostrato

**CA-5 — Le validità non si sovrappongono**
- **Dato** una regola valida dal 1° gennaio, ancora aperta
- **Quando** si apre una seconda regola sulla stessa base dal 1° marzo senza chiudere la prima
- **Allora** il sistema chiude la prima al 28 febbraio oppure rifiuta con un messaggio chiaro, secondo
  l'impostazione scelta, e non lascia mai due regole vive sullo stesso periodo

**CA-6 — Nessuno vede la regola del collega**
- **Dato** due operatori con regole diverse
- **Quando** uno dei due apre la sezione *Operatori*
- **Allora** vede soltanto la propria, e la rotta interrogata direttamente con l'identificativo dell'altro risponde
  come per una regola inesistente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo con scaglioni, arrotondamenti e basi al netto degli sconti; di **integrazione**
      sulle rotte con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli**, compresa la lettura della regola altrui dentro
      lo stesso account;
- [ ] **prova end-to-end**: *rimando* — la regola si vede all'opera nel percorso `[J-SALONGROVE]` (storia `0030`),
      che verifica il maturato di una chiusura; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la categoria «condizione economica del rapporto
      di lavoro» e la sua base giuridica;
- [ ] **registro delle decisioni**: base di calcolo predefinita, trattamento dei pacchetti, punti base interi,
      divieto di esposizione conversazionale;
- [ ] avvio locale invariato; il salone di prova ha due regole diverse su due operatori.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0023` | senza il venduto attribuito non c'è base su cui calcolare |
| storia `0019` | il maturato si fissa alla chiusura del conto, non prima |

## 7. Fuori ambito

- il **prospetto** del periodo e la sua chiusura: storia `0025`;
- il **cedolino**, i contributi e ogni adempimento del rapporto di lavoro: perimetro escluso (app 10, esclusa);
- l'affitto della poltrona come contratto (canone fisso invece che percentuale): rappresentabile con una
  percentuale zero e un accordo fuori dall'app; se servisse come entità propria, è materia di una storia futura.

## 8. Punti aperti

**Su quale base si calcola davvero.** Al netto degli sconti è quasi sempre giusto; sui pacchetti no: un pacchetto
incassato a gennaio e consumato a marzo può maturare provvigione all'incasso (chi ha venduto) o all'utilizzo (chi
esegue). Le due scuole esistono entrambe nel settore e cambiano il compenso delle persone. La proposta è
«all'utilizzo», perché è il momento in cui il lavoro viene fatto, ma è una **decisione di prodotto con effetti
retributivi**: la chiude lo sviluppatore, e va scritta in `decisions.json` e nelle condizioni d'uso.
