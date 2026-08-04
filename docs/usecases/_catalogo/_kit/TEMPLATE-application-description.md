# Modello — `application-description.md`

> **Istruzioni per l'agente-app (cancellare questo riquadro a stesura conclusa).**
> Copia tutto ciò che sta **sotto la riga** in `docs/usecases/_catalogo/NN-<slug>/application-description.md` e
> riempi i segnaposto `‹…›`. Questo è il **documento capofila** dell'applicazione: contiene già pronte le risposte
> che la skill `new-application` pretenderà al momento dello scaffolding, e fa da **indice** delle epiche e delle
> storie. Nessuna sezione è facoltativa: se una non si applica, si scrive perché, non si cancella.
> Regole di forma in [GUIDA-AUTORE.md](GUIDA-AUTORE.md), vincoli tecnici in [PRINCIPI-APPGROVE.md](PRINCIPI-APPGROVE.md).

---

# ‹Nome dell'app› — descrizione dell'applicazione

**Numero di catalogo**: ‹NN› · **Tipo**: ‹orizzontale | verticale · settore› · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda ‹NN›](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: ‹AAAA-MM-GG›
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** ‹Due o tre frasi. Che cosa produce l'app, concretamente. Niente promesse di marketing.›

**Per chi.** ‹Il cliente tipo: dimensione, ruolo di chi compra, ruolo di chi usa tutti i giorni. Il perimetro è
micro-impresa 1-10 addetti e piccola impresa 10-50, mercato globale con priorità europea.›

**Quale problema toglie.** ‹Il dolore preciso, nelle parole del cliente. Come lo risolve oggi senza di noi —
foglio di calcolo, quaderno, messaggistica, prodotto concorrente — e perché quel modo costa.›

**Cosa NON fa.** ‹Il perimetro escluso, dichiarato subito. È la sezione che evita metà delle discussioni dopo.›

**Rischio di sostituzione da parte dei modelli linguistici.** ‹`minacciata` / `neutra` / `rafforzata`, come nel
catalogo, con una riga di motivazione: dove sta il valore che un assistente generico non può dare — il flusso di
lavoro, i dati proprietari, l'integrazione, la conformità.›

---

## 2. Mercato e analisi in rete

> Compilata dopo almeno **4-6 ricerche mirate** ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4).
> Ciò che non è stato trovato va **dichiarato**, non colmato a intuito.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| ‹nome› | ‹paese/mercato› | ‹una riga› | ‹€…/mese, unità di misura› | ‹collegamento› |
| ‹nome› | | | | |

‹Una o due frasi di lettura: dove sono forti, dove lasciano scoperto il segmento micro, su cosa ci si può
differenziare.›

### 2.2 Prezzi praticati nel dominio

‹Fasce osservate, unità di misura prevalente (per utente / per sede / per veicolo / per documento / per unità),
presenza di un piano gratuito, durata tipica della prova. Distinguere ciò che è **rilevato su pagina ufficiale**
da ciò che viene da siti di comparazione.›

### 2.3 Obblighi normativi del settore

‹Cosa la legge impone a chi usa questo software: conservazione, tracciabilità, requisiti fiscali, sicurezza,
dati sanitari, differenze fra giurisdizioni. È la sorgente più frequente di requisiti che cambiano il modello dati.
Se il dominio è poco normato, dirlo esplicitamente.›

### 2.4 Integrazioni attese dal cliente

‹Elenco di ciò che un cliente tipo si aspetta di collegare, in ordine di richiesta. Marcare quelle che
introdurrebbero un **fornitore esterno che tratta dati per nostro conto** — vanno segnalate in sede di
classificazione dei dati personali.›

### 2.5 Aspettative funzionali dei clienti micro e piccoli

‹Cosa chiedono davvero e cosa rifiutano. Se hai trovato recensioni o discussioni, cita le lamentele ricorrenti:
sono requisiti travestiti.›

### 2.6 Fonti consultate

1. ‹titolo› — ‹collegamento completo› — ‹cosa ne ho ricavato›
2. ‹…›

### 2.7 Cosa NON sono riuscito a determinare

- ‹punto aperto› — ‹perché non l'ho trovato› — ‹cosa servirebbe per chiuderlo›

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa
> ([step-01-identity.md](../../../../.claude/skills/new-application/step-01-identity.md)). L'identificativo
> dell'app finisce nel nome dello schema del database, nei nomi delle code, nella rotta pubblica e nell'istanza
> del modulo di infrastruttura: cambiarlo dopo **non è una rinomina, è una migrazione di dati**.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `‹app_id›` | Deve rispettare `^[a-z][a-z0-9_]{0,30}$`: minuscolo, inizia per lettera, solo lettere, cifre e trattini bassi, al massimo 31 caratteri. Breve, stabile, riferito a **cosa l'app è**, non a come è commercializzata oggi. ‹motivo della scelta› |
| **Modello utente** | `‹single | multi›` | `single` = una persona per account (rivolto al singolo professionista); `multi` = più persone per account, con inviti e posti. ‹Perché questo modello: chi usa l'app in una giornata tipo del cliente.› Cambiare dopo è scomodo: un'app a utente singolo non ha il concetto di «chi ha fatto cosa». |
| **Porta locale** | `‹8100 + NN›` | Convenzione del kit per non far collidere le sessanta proposte. Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `‹metrica›` | La **sola** cosa che il piano limita. ‹Perché è questa e non un'altra: è ciò che cresce con il valore che il cliente riceve.› |
| **Natura della metrica** | `‹flow | stock›` | `flow` = consumo su una finestra che si azzera («‹200 documenti al mese›»: a marzo se ne possono fare altri 200 comunque sia andato febbraio). `stock` = tetto su ciò che esiste ora («‹10 posti›»: per aggiungerne uno bisogna toglierne uno). ‹Frase di esempio nelle parole dell'app.› Sbagliarla è l'errore più costoso del listino: una giacenza contata come consumo lascia accumulare senza limite; un consumo contato come giacenza blocca l'utente per sempre. |
| **Colore-categoria e icona** | `‹green | amber | red | blue | violet | teal›` · icona `‹nome›` | Deve essere lo stesso nel listino (`category`) e nel modulo frontend (`accentToken`). ‹Perché questo colore rispetto alle app vicine del catalogo.› |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `‹Entita›` | ‹una riga› | ‹campi che contano, non l'elenco completo› | ‹sì/no — quali› |

**Relazioni.** ‹Come si legano: uno-a-molti, appartenenza, cicli di vita. Se una entità ha una macchina a stati
(bozza → inviato → accettato → chiuso), disegnala qui: è la parte che le storie devono rispettare.›

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_‹app_id›`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/‹app_id›.yaml`.

**Ragionamento.** ‹Da dove nascono questi numeri: fasce rilevate al §2.2, valore percepito, costo variabile per
unità se esiste (per esempio un fornitore che si paga a documento o a messaggio), soglia sotto la quale il piano
base non sta in piedi.›

| Piano | Prezzo mensile | Prezzo annuale | Limite sulla metrica `‹metrica›` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | ‹cap› | — | ‹abbastanza per vedere il valore, non abbastanza per viverci› |
| `‹pro›` | ‹€ …› | ‹€ …› (= 10× il mensile, «due mesi in regalo») | ‹cap› | ‹14 giorni› | ‹…› |
| `‹team›` | ‹€ …› | ‹€ …› | ‹cap o illimitato› | ‹14 giorni› | ‹…› |

**Note obbligate.**

- Si raccomandano **due o tre piani**: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite **lasciato vuoto significa illimitato**, non zero: le due letture distano un refuso.
- Una prova gratuita su un'app che ha già un piano gratuito è spesso ridondante: ‹dire se qui lo è›.
- **Costo effettivo dell'incasso**: sui prezzi bassi la parte fissa per transazione pesa molto. ‹Se la proposta ha
  un piano sotto i ~5 €/mese, dirlo: è un segnale, non un veto. Rimedi naturali: alzare il prezzo o spingere
  l'annuale.›
- I prezzi sono **immutabili una volta vivi**: un cambio di prezzo si fa creando un prezzo nuovo, non modificando
  quello esistente.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/‹app_id›.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

‹**SE il dominio tocca categorie particolari — articolo 9: salute, dati biometrici, dati genetici, opinioni
politiche, convinzioni religiose, orientamento sessuale, appartenenza sindacale — apri la sezione con un avviso
forte, in grassetto, e non nasconderlo in fondo:**›

> 🛑 **Attenzione — categorie particolari (articolo 9).** ‹Quali dati, dove entrano, perché sono inevitabili o
> perché si possono evitare.› Servono una base giuridica rafforzata e una valutazione d'impatto. Vale anche se il
> campo è facoltativo: un dato particolare facoltativo resta un dato particolare. Un'app che può evitarli, di
> norma deve evitarli.

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `‹entita.campo›` | ‹tabella/colonna› | ‹cliente, dipendente, contatto…› | ‹anagrafica, contatto, economico…› | ‹finalità› | ‹esecuzione del contratto / obbligo di legge / legittimo interesse / consenso› | ‹durata e da quando decorre› |

**Esportazione e cancellazione.** ‹Elenco delle tabelle che contengono dati di persone: **ognuna** deve comparire
sia nell'esportazione sia nella cancellazione del contratto dati dell'app. Dimenticarne una è il difetto di
conformità più probabile in un'app nuova. La cancellazione è **fisica**: sostituire i nomi con dei codici non è
cancellare.›

**Testo libero.** ‹Se l'app ha campi nota liberi, dirlo: sono un ingresso non presidiato per categorie
particolari. L'app non fa rilevazione di contenuto; il presidio, se servirà, è un tema trasversale.›

**Integrazioni esterne.** ‹Ogni integrazione del §2.4 che riceverebbe dati personali è un potenziale nuovo
fornitore che tratta dati per nostro conto: elencarle qui, perché finiscano nell'elenco dei fornitori e
nell'informativa.›

**Classificazione della change.** Una app nuova introduce finalità nuove e categorie nuove: di norma è un
cambiamento **sostanziale**. ‹Confermare o motivare il contrario — la classificazione descrive la realtà, non è
una leva per evitare adempimenti.›

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `‹elenca_x›` | `(filtro?, periodo?) → elenco di ‹X› minimizzato` | ‹cosa restituisce› | lettura | no |
| `‹crea_x›` | `(campi obbligatori) → bozza di ‹X›` | ‹cosa crea› | scrittura | **sì** |
| `‹invia_x›` | `(id) → esito della trasmissione` | ‹effetto verso l'esterno› | scrittura irreversibile | **sì, obbligatoria** |

‹Riga di lettura: quali strumenti sono la ragione per cui il livello conversazionale rende questa app più utile
delle sue concorrenti.›

---

## 8. Indice delle epiche e delle storie

> È questa tabella a rendere il documento l'indice dell'applicazione. I collegamenti sono **relativi** e devono
> puntare a file che esistono davvero. La numerazione delle storie è progressiva a livello di app e **non si
> azzera** a ogni epica.

### Epica 01 — Fondamenta

‹Una riga: cosa deve esistere alla fine dell'epica perché l'app sia accesa, vuota e utilizzabile.›

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-‹slug›.md) | ‹titolo› | ‹cosa consegna› |
| [0002](01-fondamenta/0002-‹slug›.md) | ‹titolo› | ‹…› |

### Epica 02 — ‹Nome dell'epica di dominio›

‹Una riga.›

| # | Storia | In una riga |
|---|---|---|
| [0006](02-‹slug›/0006-‹slug›.md) | ‹titolo› | ‹…› |

### ‹… altre epiche …›

### Epica ‹NN› — Esposizione conversazionale e prove end-to-end

‹Una riga.›

| # | Storia | In una riga |
|---|---|---|
| [00NN](‹NN›-esposizione-conversazionale-e-prove/00NN-‹slug›.md) | ‹titolo› | ‹…› |

**Totale**: ‹N› epiche, ‹M› storie.

---

## 9. Estensioni della console di amministrazione

‹Riepilogo in tre righe di cosa serve alla console di amministrazione per governare questa app — oppure la frase
«nessuna estensione oltre lo standard di piattaforma», se è così.›

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| ‹NN — Nome› | ‹dipende da / alimenta / condivide dati con / si sovrappone a› | ‹anagrafica clienti, catalogo prodotti, anagrafica dipendenti, catena del documento contabile…› |

‹Riga di lettura: se l'app ha senso da sola oppure solo dentro la suite. Il catalogo (§6) individua come entità
condivise l'anagrafica clienti, il catalogo prodotti e listini, l'anagrafica dipendenti e la catena preventivo →
ordine → fattura → incasso: se la tua app tocca una di queste, dillo qui.›

**Sovrapposizioni da evitare.** ‹Se un'altra app del catalogo fa già una parte di questo, dirlo: è meglio saperlo
adesso che dopo aver costruito due volte la stessa cosa.›

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | ‹…› | ‹manca un dato di mercato / è una decisione di prodotto / dipende da un'epica non implementata› | ‹sviluppatore / epica ‹NN› / storia ‹NNNN›› |

**Rischi noti**

- ‹Rischio› — ‹effetto se si avvera› — ‹cosa lo attenuerebbe›.

**Fuori dimensionamento** (compilare solo se applicabile): ‹se le epiche o le storie escono dalla fascia
raccomandata — 4-7 epiche, 4-8 storie per epica, 20-45 storie in tutto — spiegare perché.›
