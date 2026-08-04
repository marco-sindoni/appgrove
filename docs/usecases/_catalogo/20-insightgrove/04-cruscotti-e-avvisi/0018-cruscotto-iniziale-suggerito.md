# 0018 — Cruscotto iniziale suggerito

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 04 — Cruscotti e avvisi
**Storia**: `0018` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena attivato InsightGrove
> voglio trovare il cruscotto già fatto, costruito su quello che ho collegato
> così da capire in trenta secondi se questa app mi serve, invece di dover imparare a costruirla.

**Contesto.** È la storia che decide se l'app viene adottata o abbandonata. La lamentela ricorrente sui
concorrenti è la fatica di configurare (§2.5 della [descrizione](../application-description.md)), e il segmento
micro non costruirà mai un cruscotto da zero. InsightGrove ha però un vantaggio che i concorrenti non hanno:
**sa già che cosa il cliente ha collegato e che cosa quelle fonti pubblicano**. Non deve chiedere, deve
proporre. È l'applicazione pratica della differenza descritta al §2.1: sparisce il costo del collegamento, e con
esso il costo della configurazione.

## 2. Requisiti funzionali

1. **RF-1** — Al primo accesso, o dopo il primo collegamento di una fonte, l'app crea un **cruscotto
   predefinito** popolato con le metriche calcolabili date le fonti collegate.
2. **RF-2** — La composizione segue un ordine dichiarato, dal più utile al meno utile per il segmento: fatturato
   del mese con confronto, incassato, crediti scaduti, giorni medi di incasso, valore di magazzino, trattative
   aperte, spese approvate. Si fermano ai primi otto disponibili.
3. **RF-3** — Se le fonti collegate non bastano a produrre alcun indicatore, il cruscotto **non è vuoto**:
   mostra che cosa comparirebbe collegando ciascuna delle fonti disponibili, con il rimando alla sezione Fonti.
4. **RF-4** — Quando l'account collega una fonte nuova, l'app **propone** — non impone — di aggiungere al
   cruscotto gli indicatori che quella fonte rende possibili.
5. **RF-5** — Il cruscotto suggerito è **modificabile come ogni altro** (storia 0017): non è un blocco
   speciale, è un punto di partenza.
6. **RF-6** — Se l'utente lo modifica, l'app **non lo ricostruisce mai più** da sola: un suggerimento che si
   ripresenta dopo essere stato scartato è una molestia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La composizione si calcola sulle fonti collegate **di quell'account**,
  lette con `tenant_id` dal gettone verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/insights/v1/cruscotti/suggerito` che restituisce
  la proposta senza salvarla, e la creazione avviene con la risorsa ordinaria dei cruscotti; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** La proposta si presenta come stato iniziale della sezione Cruscotto, con
  titolo, spiegazione e **una azione** — mai un vicolo cieco; solo token del sistema di design; tema chiaro e
  scuro.
- **RT-5 — Cinque lingue (§4).** I testi della proposta e dello stato iniziale esistono in `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** La creazione del cruscotto suggerito richiede `owner` o `admin`; un `member`
  che entra per primo vede il cruscotto se già esiste, altrimenti l'invito a chiedere a chi amministra.
  **Nessun consumo di quota**.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-14 — Registrazione eventi (§14).** «Cruscotto suggerito creato», «proposta scartata» con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Il primo accesso non è vuoto**
- **Dato** un account che ha collegato fatturazione e incassi
- **Quando** un `owner` entra per la prima volta in InsightGrove
- **Allora** vede un cruscotto già composto con fatturato del mese, incassato, crediti scaduti e giorni medi di
  incasso, ciascuno con il proprio valore o il proprio motivo di indisponibilità

**CA-2 — Nessuna fonte, nessun vicolo cieco**
- **Dato** un account senza fonti collegate
- **Quando** entra in InsightGrove
- **Allora** vede l'elenco di ciò che comparirebbe collegando ciascuna fonte disponibile, con il pulsante che
  porta alla sezione Fonti

**CA-3 — Una fonte nuova propone, non impone**
- **Dato** un cruscotto già in uso, e l'account collega la fonte magazzino
- **Quando** torna sul cruscotto
- **Allora** vede una proposta di aggiungere «valore di magazzino» e «giorni di giacenza», che può accettare o
  scartare

**CA-4 — Il suggerimento scartato non torna**
- **Dato** una proposta scartata
- **Quando** l'utente rientra nell'app, anche dopo giorni
- **Allora** la proposta non si ripresenta

**CA-5 — Isolamento fra account**
- **Dato** due account con fonti diverse
- **Quando** ciascuno chiede la propria proposta
- **Allora** ognuno riceve la composizione derivata dalle **proprie** fonti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'ordine di composizione al variare delle fonti collegate, e di **integrazione**
      sulla creazione;
- [ ] prova di **isolamento fra account** sulla proposta;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` comincia con «collega una fonte e trova
      il cruscotto già pieno»; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con l'ordine di priorità degli indicatori e la fonte da cui viene
      (§2.5 della descrizione, fonti 5 e 6);
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0008` | la composizione dipende dalle fonti collegate |
| storia `0017` | il cruscotto suggerito è un cruscotto ordinario |
| storia `0012` | le metriche predefinite sono ciò che si propone |

## 7. Fuori ambito

- una composizione che si adatti al settore del cliente: richiederebbe di sapere che settore è, e nessuno glielo
  ha chiesto. Se servirà, sarà una storia a sé;
- gli avvisi suggeriti: vedi punti aperti.

## 8. Punti aperti

- **Si suggeriscono anche gli avvisi?** «Avvisami se i crediti scaduti superano X» è utile quanto il cruscotto,
  ma richiede di indovinare la soglia — e una soglia sbagliata suggerita dall'app genera avvisi che il cliente
  ignora, il che è peggio di nessun avviso. Raccomandazione: **non suggerire soglie**, ma suggerire *quali*
  avvisi valga la pena creare, lasciando all'utente il numero. Chiude: **sviluppatore**.
- **Otto riquadri iniziali sono i giusti?** La fonte 5 dice «5-10». Otto sta in mezzo. Chiude: **sviluppatore**.
