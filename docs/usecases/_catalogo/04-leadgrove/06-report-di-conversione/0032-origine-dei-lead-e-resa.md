# 0032 — Origine dei lead e resa

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 06 — Report di conversione
**Storia**: `0032` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0028`, `0030`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che spende in fiere, sito e passaparola
> voglio sapere da quale origine arrivano i contatti che poi diventano affari
> così da mettere i soldi dove rendono invece che dove si è sempre fatto.

**Contesto.** L'origine del contatto è un campo che la storia 0007 ha reso non falsificabile proprio in vista di
questo rapporto. È la domanda che il titolare di una micro-impresa si pone davvero prima di rinnovare la
partecipazione a una fiera; ed è anche l'unico rapporto dell'epica che parla di **soldi spesi**, non di lavoro
fatto.

## 2. Requisiti funzionali

1. **RF-1** — Il rapporto mostra, per periodo, quanti contatti sono arrivati da ciascuna origine (inserito a mano,
   importazione, modulo web, con distinzione fra i diversi moduli).
2. **RF-2** — Per ciascuna origine mostra quante trattative ne sono nate, quante sono state vinte, il tasso di
   conversione e il valore complessivo vinto.
3. **RF-3** — L'origine di una trattativa è quella del **contatto** collegato al momento della creazione, e resta
   quella anche se il contatto viene poi modificato.
4. **RF-4** — Le origini con pochi numeri sono marcate come non significative, con la stessa soglia della storia
   0030.
5. **RF-5** — Il rapporto mostra anche il **tempo mediano** dal primo contatto alla vincita per origine: un canale
   che rende ma in nove mesi è un'informazione diversa da uno che rende in tre settimane.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'aggregazione comprende solo dati dell'account del token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/sales/v1/reports/by-source` con periodo; errori
  in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Colonna dell'origine sulla trattativa, valorizzata alla creazione con migrazione
  `V<N>__deal_source.sql`: senza, il RF-3 sarebbe impossibile da garantire.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Rapporti → Origini, in forma di tabella con una barra proporzionale
  per riga; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi delle origini di sistema, intestazioni e avvisi in `en, it, fr, es, de`; i
  nomi dei moduli web restano quelli scritti dal cliente.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota; il rapporto sull'intero account richiede ruolo
  `owner` o `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Rientra in `conversion_report` (storia 0034) con il parametro di
  raggruppamento per origine.
- **RT-8 — Dati personali (§10).** Nessun dato personale: sono aggregati. Nessuna voce nuova.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo.

## 4. Criteri di accettazione

**CA-1 — Resa per origine**
- **Dato** 30 contatti da modulo web e 20 da fiera importati, con 6 e 2 trattative vinte
- **Quando** si apre il rapporto
- **Allora** mostra i due tassi con i conteggi che li generano

**CA-2 — L'origine non cambia a posteriori**
- **Dato** una trattativa nata da un contatto con origine «modulo web»
- **Quando** il contatto viene fuso con un altro di origine diversa (storia 0010)
- **Allora** l'origine della trattativa resta «modulo web»

**CA-3 — Distinzione fra moduli**
- **Dato** due moduli web diversi sul sito del cliente
- **Quando** si apre il rapporto
- **Allora** le due origini sono distinte, con il nome dato dal cliente

**CA-4 — Isolamento fra account**
- **Dato** due account con moduli e origini simili
- **Quando** un utente di `A` apre il rapporto
- **Allora** i numeri comprendono solo `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla persistenza dell'origine e di **integrazione** sull'aggregazione;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli**;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo; coperta da prove d'integrazione, con il motivo nel
      registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotato perché l'origine si congela sulla trattativa;
- [ ] contratto degli **strumenti conversazionali**: raggruppamento per origine;
- [ ] controllo automatico di **accessibilità** verde sulla tabella;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0028` | I moduli web sono una delle origini distinte |
| Storia `0030` | Riusa soglia e calcoli |

## 7. Fuori ambito

- il costo per canale e il ritorno sull'investimento: richiederebbe di far inserire le spese, che è un altro
  mestiere;
- il tracciamento della provenienza dal sito (quale pagina, quale campagna): sarebbe tracciamento, e dentro le app
  appgrove non se ne fa ([PRINCIPI-APPGROVE.md](../../_kit/PRINCIPI-APPGROVE.md) §10);
- le origini definite dal cliente oltre a quelle di sistema: si ottengono con moduli web distinti o con un campo
  personalizzato.

## 8. Punti aperti

- Nessuno.
