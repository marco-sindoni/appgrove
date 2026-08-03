# 0028 — Scomposizione della variazione

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 06 — Metriche dei ricavi ricorrenti
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vede il ricavo del mese sceso di trecento euro
> voglio sapere **perché** è sceso: se sono arrivati meno iscritti nuovi o se se ne sono andati i vecchi
> così da sapere se il problema è la vendita o il servizio, e non tirare a indovinare.

**Contesto.** La storia `0027` produce un numero; da sola serve a poco. La domanda che il titolare fa davvero non è
«quanto», è «perché è cambiato»: se il ricavo cala perché **entrano meno iscritti nuovi** si lavora sulla vendita,
se cala perché **se ne vanno i vecchi** si lavora sul servizio, e sono due mestieri diversi. La scomposizione è la
risposta: la differenza fra due mesi si spezza in quattro pezzi — quanto è entrato di **nuovo**, quanto è cresciuto
per **passaggi a piani superiori** (espansione), quanto è calato per **passaggi a piani inferiori** (contrazione),
quanto è uscito per **abbandono**. La prova che la scomposizione è giusta è aritmetica e verificabile: i quattro
pezzi, sommati, devono dare esattamente la differenza fra le due istantanee. Se non quadra, c'è un movimento che il
sistema non sa spiegare — ed è un difetto, non un arrotondamento.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni mese chiuso l'app calcola e salva la scomposizione della variazione rispetto al mese
   precedente in quattro voci: **nuovo**, **espansione**, **contrazione**, **abbandono**.
2. **RF-2** — Le quattro voci **quadrano**: `ricavo del mese − ricavo del mese precedente = nuovo + espansione −
   contrazione − abbandono`. La quadratura è verificata a ogni calcolo; se non torna, l'istantanea è marcata come
   **da verificare** e l'anomalia è registrata invece di essere nascosta.
3. **RF-3** — Ogni voce è **apribile**: dalla cifra si arriva all'elenco degli abbonamenti che l'hanno prodotta,
   con l'evento del ciclo di vita che l'ha causata e la sua data.
4. **RF-4** — La ripresa di un abbonamento sospeso e il rientro di un abbonamento cessato si contano come **nuovo**,
   con la sotto-etichetta «ritorno», così che non si confondano con la prima sottoscrizione.
5. **RF-5** — Un abbandono si conta nel mese in cui l'abbonamento **cessa davvero**, non nel mese in cui è arrivata
   la disdetta: fino a fine periodo il canone è ancora dovuto (coerente con la `0027`).
6. **RF-6** — La sezione *Andamento* mostra la scomposizione del mese come quattro grandezze accostate, con il
   saldo in evidenza e una riga in parole («+2 nuovi, −1 andato via, saldo +40 € al mese»).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Calcolo e lettura filtrano per `tenant_id` preso dal token verificato;
  gli elenchi apribili non possono risolvere su abbonamenti di un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/abbonati/v1/metriche/variazione` (per mese) e
  `GET /api/abbonati/v1/metriche/variazione/{mese}/{voce}` per l'elenco apribile; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V21__scomposizione_variazione.sql`: colonne delle quattro voci (in
  centesimi) e marcatura «da verificare» sulla tabella `istantanea_ricavi` della storia `0027`, più la tabella
  `movimento_ricavo` con `tenant_id`, chiave UUID versione 7, riferimento logico all'abbonamento, voce, importo,
  causa, momento, colonne di controllo e cancellazione logica.
- **RT-4 — Origine dei movimenti.** I movimenti si derivano dalle **transizioni della macchina a stati** e dai
  cambi di piano già registrati (storie `0011` e `0014`): non si inventa una seconda verità: se un movimento non ha
  una transizione che lo spieghi, è l'anomalia del **RF-2**.
- **RT-5 — Modulo frontend (§3, §5).** Nella sezione *Andamento*: quattro grandezze accostate disegnate con i soli
  token del sistema di design (niente librerie di grafici, che porterebbero un aspetto proprio), leggibili in tema
  chiaro e scuro, e la stessa informazione disponibile **in parole** per chi non legge i grafici.
- **RT-6 — Cinque lingue (§4).** «nuovo», «espansione», «contrazione», «abbandono», la riga in parole e le
  intestazioni degli elenchi in `en, it, fr, es, de`. I termini vanno spiegati alla prima occorrenza a schermo: chi
  li usa non è un analista finanziario.
- **RT-7 — Varchi e quota (§6, §7).** Lettura: non consuma la metrica `abbonamenti_attivi`. Con abbonamento di
  piattaforma `canceled` risponde `402`, in `past_due` resta accessibile.
- **RT-8 — Esposizione conversazionale (§12).** Completa il contratto di
  `metriche_ricorrenti(mese) → ricavo, attivi, nuovo/espansione/contrazione/abbandono`, marcato **lettura**, senza
  conferma; il contratto è raccolto nella storia `0031`.
- **RT-9 — Dati personali (§10).** Gli aggregati non contengono dati di persone; gli **elenchi apribili** invece
  nominano abbonati: non introducono campi nuovi, ma la lettura va trattata come lettura di dati personali già
  dichiarati (nessuna voce nuova nel manifesto, nessuna tabella nuova in esportazione — il movimento riferisce un
  abbonamento, non una persona).
- **RT-10 — Registrazione eventi (§14).** `scomposizione calcolata (mese)`, `quadratura fallita (differenza)`, con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza nomi.
- **RT-11 — Prove (§11).** Unità sulla quadratura con casi costruiti a mano (nuovo, cambio in su, cambio in giù,
  cessazione, ritorno) e su un caso volutamente incoerente che deve produrre l'anomalia.

## 4. Criteri di accettazione

**CA-1 — Quadratura**
- **Dato** un mese con due sottoscrizioni nuove, un passaggio a un piano superiore, una cessazione
- **Quando** si calcola la scomposizione
- **Allora** la somma delle quattro voci è **esattamente** la differenza fra i ricavi dei due mesi, al centesimo

**CA-2 — Anomalia dichiarata, non nascosta**
- **Dato** uno stato di dati in cui un movimento non ha una transizione che lo spieghi
- **Quando** gira il calcolo
- **Allora** l'istantanea è marcata «da verificare», l'anomalia è registrata con la differenza, e il numero **non**
  viene forzato per farlo quadrare

**CA-3 — L'abbandono si conta alla cessazione**
- **Dato** un abbonamento disdetto il 5 marzo con periodo fino al 30 aprile
- **Quando** si guardano le scomposizioni di marzo e aprile
- **Allora** marzo non registra abbandono, aprile sì, e il canone di marzo era ancora contato nel ricavo

**CA-4 — Voce apribile**
- **Dato** la voce «contrazione» del mese, pari a 25 €
- **Quando** la si apre
- **Allora** si vede l'abbonamento che è passato a un piano inferiore, con la data e la causa

**CA-5 — Ritorno distinto dalla prima volta**
- **Dato** un abbonato che era cessato e torna con un abbonamento nuovo
- **Quando** si legge la scomposizione
- **Allora** contribuisce a «nuovo» con la sotto-etichetta «ritorno», ed è distinguibile nell'elenco

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** uno apre una voce della scomposizione
- **Allora** vede solo abbonamenti propri, anche forzando l'identificativo dell'altro account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`);
- [ ] prove di **unità** sulla quadratura e sulla classificazione dei movimenti; **integrazione** sul calcolo
      mensile con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sugli elenchi apribili;
- [ ] **prova end-to-end**: *rimando* — la scomposizione entra nel percorso `[J-ABBONATI]` della storia `0033`, con
      voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) e storia proprietaria `0033`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con i quattro termini spiegati alla prima occorrenza;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la motivazione scritta;
- [ ] **registro delle decisioni** compilato: regola di quadratura, momento in cui si conta l'abbandono,
      trattamento dei ritorni, comportamento davanti all'anomalia;
- [ ] contratto dello strumento `metriche_ricorrenti` completato con la scomposizione;
- [ ] controllo di accessibilità verde sulla sezione *Andamento*.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0027` | la scomposizione spiega la differenza fra due istantanee: senza istantanee non esiste |
| storia `0011` | i movimenti si derivano dalle transizioni della macchina a stati |
| storia `0014` | i cambi di piano sono ciò che produce espansione e contrazione |

## 7. Fuori ambito

- il tasso di abbandono e la durata media: storia `0029`;
- la previsione dei mesi futuri: storia `0030`;
- il confronto con altri clienti o con medie di settore: **mai** — sarebbe un uso secondario dei dati dei clienti,
  vietato dai principi di piattaforma;
- l'attribuzione commerciale («da quale campagna viene questo abbonato»): è mestiere di **04 LeadGrove**.

## 8. Punti aperti

**Cambio di piano nello stesso mese in cui l'abbonamento nasce.** Un abbonato che sottoscrive il 3 e passa a un
piano superiore il 20 produce sia «nuovo» sia «espansione» nello stesso mese: contarli entrambi è corretto in
aritmetica ma può confondere chi legge. **Proposta**: contarli entrambi (la quadratura lo impone) e mostrarli nello
stesso elenco apribile, così che si capisca che è la stessa persona. Chiude: lo sviluppatore, con la direzione di
prodotto.

**Come si chiamano le quattro voci a schermo.** «Espansione» e «contrazione» sono parole da analista, non da
reception. **Proposta**: usare i termini per esteso e spiegati («cresciuto per passaggi a piani superiori»),
accettando di essere più lunghi, coerentemente con la regola di lingua del progetto. Chiude: lo sviluppatore.
