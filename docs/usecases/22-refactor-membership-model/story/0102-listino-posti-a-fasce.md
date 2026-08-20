# UC 0102 — Listino dei posti a fasce: modello versionato e calcolo del dovuto

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.2 Posti a pagamento](../epic/E22-02-posti-a-pagamento.md)
**Dipendenze**: UC 0098 (modello dati dell'accesso), UC 0022 (listino come codice e sincronizzazione col fornitore di pagamento)
**Sostituisce**: UC 0073 dell'epica 14 (posti come quota per applicazione)
**Piano di lavoro**: [task/0102](../task/0102-listino-posti-a-fasce.md)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Rendere calcolabile, in modo esatto e verificabile, quanto un account deve per i suoi posti, e conservare
il listino in una forma che permetta di rispondere anche fra un anno alla domanda «quanto pagava questo
cliente in marzo?».

**Incluso**: la definizione delle fasce con i confini disambiguati; la funzione di calcolo del dovuto; il
modello di conservazione **versionato** del listino; il conteggio di ciò che occupa un posto; il valore
iniziale del listino come codice.

**Escluso**: l'atto di acquistare → UC 0103; la riduzione → UC 0104; il cambio delle tariffe da console →
UC 0105; la presentazione al cliente → UC 0106.

## 2. Attori & ruoli

- **Sistema**: calcola il dovuto ogni volta che i posti cambiano o il periodo si rinnova.
- **Owner**: vede il risultato del calcolo (non lo governa).
- **Amministratore di piattaforma**: governa le tariffe (UC 0105).

## 3. Precondizioni

- Esistono le persone dell'account e i loro stati (UC 0098).
- Esiste l'impianto del listino come codice per le applicazioni, da cui questa storia prende la forma ma
  **non** il meccanismo (i posti non sono un'applicazione).

## 4. Flusso principale — la regola di calcolo

**Che cosa occupa un posto.** Occupano un posto: l'**owner**; ogni persona **attiva**; ogni **invito in
attesa** non scaduto; ogni persona **indicata per la cessazione** finché il periodo non scade. **Non**
occupano posto: le persone rimosse, gli inviti scaduti o revocati. Le persone **sospese** occupano posto
(la sospensione è un provvedimento reversibile, non una riduzione: chi vuole liberare il posto lo indica
per la cessazione).

**Il calcolo è a scaglioni progressivi**: ogni posto paga la tariffa della fascia in cui cade *quel
posto*, non la tariffa dell'ultima fascia raggiunta. In tre passi:

1. `n` = numero di posti occupati.
2. Se `n ≤ 3` → dovuto **zero**. La franchigia comprende l'owner.
3. Altrimenti si sommano gli **scaglioni**: per ogni fascia si contano i posti che vi cadono e si
   moltiplicano per la sua tariffa. Le fasce e le tariffe vigenti sono
   `1–3` → 0,00 · `4–10` → 2,99 · `11–50` → 1,99 · `51–100` → 0,99 · `oltre 100` → 0,49 (euro al mese
   per posto).

Esempio svolto, 52 posti: 3 gratuiti, poi sette posti (dal 4° al 10°) a 2,99 = 20,93; quaranta posti
(dall'11° al 50°) a 1,99 = 79,60; due posti (51° e 52°) a 0,99 = 1,98. **Totale 102,51 €.**

Esempi di riferimento (da usare come casi di prova, confini compresi):

| Posti | Composizione | Dovuto mensile | Costo del posto successivo |
|---|---|---|---|
| 1 · 2 · 3 | franchigia | 0,00 € | 2,99 € |
| 4 | 1 × 2,99 | 2,99 € | 2,99 € |
| 5 | 2 × 2,99 | 5,98 € | 2,99 € |
| 8 | 5 × 2,99 | 14,95 € | 2,99 € |
| 10 | 7 × 2,99 | 20,93 € | **1,99 €** (cambia fascia) |
| 11 | 7 × 2,99 + 1 × 1,99 | 22,92 € | 1,99 € |
| 12 | 7 × 2,99 + 2 × 1,99 | 24,91 € | 1,99 € |
| 50 | 7 × 2,99 + 40 × 1,99 | 100,53 € | **0,99 €** (cambia fascia) |
| 51 | … + 1 × 0,99 | 101,52 € | 0,99 € |
| 52 | … + 2 × 0,99 | 102,51 € | 0,99 € |
| 55 | … + 5 × 0,99 | 105,48 € | 0,99 € |
| 100 | … + 50 × 0,99 | 150,03 € | **0,49 €** (cambia fascia) |
| 101 | … + 1 × 0,49 | 150,52 € | 0,49 € |
| 120 | … + 20 × 0,49 | 159,83 € | 0,49 € |

**Il totale cresce sempre; a scendere è il costo del posto successivo.** È la proprietà che rende questo
modello spiegabile: chi cresce non vede mai il totale calare, e ai confini di fascia scopre che la persona
in più costa meno della precedente. Conseguenze operative: l'interfaccia mostra **entrambi** i numeri —
dovuto attuale e costo del prossimo posto — e un collaudo può legittimamente pretendere che il dovuto sia
**monotono crescente**, cosa che con il modello a tariffa unica di fascia era falsa.

**Modello precedente, scartato**: tariffa della fascia applicata a *tutti* i posti a pagamento. Era più
semplice da calcolare ma faceva **scendere il totale** ai confini (10 posti costavano più di 11), e un
prezzo che cala quando cresci è indifendibile davanti a un cliente, anche quando è a suo favore: sembra un
errore di conteggio. La progressività costa qualche riga di codice in più e si spiega in una frase.

## 5. Flussi alternativi / edge / errori

- **Edge — zero persone**: impossibile; c'è sempre l'owner. Il calcolo è comunque definito e vale zero.
- **Edge — periodo annuale**: il listino nasce **mensile**, coerente con la permanenza minima di un mese.
  Se un giorno servisse il ciclo annuale, sarà una versione nuova del listino con le sue tariffe, non un
  moltiplicatore applicato a queste. Punto aperto.
- **Edge — arrotondamento**: si calcola in centesimi interi, come tutto il resto della fatturazione; il
  dovuto è un numero intero di centesimi e non si arrotonda per riga.
- **Edge — valuta diversa dall'euro**: fuori dal listino iniziale; segue la strada delle applicazioni.
  Punto aperto.
- **Errore — nessuna versione di listino valida alla data richiesta**: si **nega** il calcolo con un
  errore esplicito, senza inventare una tariffa. Un dovuto calcolato su un listino inesistente è peggio
  di un errore.

## 6. Risorse & dati _(storia di modello e calcolo)_

Nessuna schermata. La presentazione è di UC 0106 (cliente) e UC 0105 (console di piattaforma).

## 7. Dati toccati

**Nuove tabelle di piattaforma** (non separate per account: il listino è di tutti):

- **`platform.seat_pricing_version`** — una versione del listino: identificativo, **data di decorrenza**,
  valuta, chi l'ha creata, quando, eventuale nota. Le versioni non si modificano: se una tariffa cambia,
  nasce una versione nuova (UC 0105).
- **`platform.seat_pricing_band`** — le fasce di una versione: posto iniziale, posto finale (vuoto per
  l'ultima fascia), tariffa in centesimi. La franchigia è rappresentata come **prima fascia a tariffa
  zero** da 1 a 3, così la regola non ha casi speciali cablati nel codice.

**Valore iniziale come codice**: un file di risorse del core, letto all'avvio, che crea la **prima**
versione se non esiste — lo stesso schema con cui il listino delle applicazioni si sincronizza da file.
Il file resta la sorgente del valore iniziale; da lì in poi la verità è la banca dati (UC 0105).

**Nessun dato personale**: tariffe e conteggi.

## 8. Permessi & gate

- **La lettura del listino vigente è pubblica dentro il prodotto** (serve a mostrare i prezzi anche a chi
  non è owner, per esempio nella pagina dei prezzi).
- **La scrittura è solo dell'amministratore di piattaforma** (UC 0105).
- **Il calcolo del dovuto è sempre fatto dal servizio**, mai dal frontend: l'interfaccia mostra il
  risultato, non lo ricalcola. Le stime mostrate prima di una azione arrivano dal servizio.

## 9. Requisiti di test

- **Unità, tabellare**: tutti i casi del §4, compresi i tre punti in cui il dovuto scende. È il collaudo
  più importante della sotto-epica.
- **Unità**: che cosa occupa un posto (persona attiva, invito in attesa, indicata per la cessazione,
  sospesa) e che cosa no (rimossa, invito scaduto o revocato).
- **Unità**: selezione della versione del listino per data; errore esplicito se nessuna versione è valida.
- **Integrazione**: creazione della prima versione dal file all'avvio, senza duplicarla ai riavvii
  successivi.
- **Percorsi end-to-end**: nessuno proprio; esente come *senza superficie*.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [Epica 22 §2](../epic/E22-00-rifacimento-modello-appartenenza.md);
  [PricingCatalogLoader.java](../../../../services/core/src/main/java/app/appgrove/core/catalog/PricingCatalogLoader.java)
  come modello del caricamento da file; [UC 0022](../../07-payments/0022-pricing-as-code-sincronizzazione.md).
- **Definition of Done**:
  1. le fasce sono conservate in versioni immutabili con la loro decorrenza;
  2. la funzione di calcolo copre tutti i casi della tabella, compresi i confini che scendono;
  3. la definizione di «cosa occupa un posto» è unica e provata;
  4. la prima versione nasce dal file, senza duplicarsi;
  5. `run-tests.sh backend` verde.

## Punti aperti / decisioni differite

- **Ciclo annuale dei posti**: non previsto ora. Se servirà, nuova versione di listino con tariffe
  proprie. Proprietario: questa storia.
- **Valute oltre l'euro**: rimandato; da allineare a come le applicazioni gestiscono la valuta.
  Proprietario: UC 0106.
- **Sconti o tariffe negoziate per singolo account**: non previsti (il listino è di tutti). Se un giorno
  servisse, sarà una deroga per account sopra il listino, non una modifica del listino. Proprietario:
  Epica 22.
