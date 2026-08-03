# 0004 — Abbonamento e quota

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare abbonato al piano `pro`
> voglio sapere in ogni momento quanti preventivi posso ancora inviare questo mese e cosa succede quando finiscono
> così da non scoprire il limite nel momento peggiore, cioè mentre sto mandando un'offerta a un cliente.

**Contesto.** Il listino dell'app dichiara la metrica `preventivi_inviati` di natura `flow` con un tetto mensile
per piano. Questa storia costruisce la catena dei varchi e il consumo della quota **prima** che esista l'invio
vero (storia `0017`), perché il varco va progettato con la funzione, non aggiunto sopra: la prenotazione
dell'unità deve avvenire nella stessa transazione dell'atto che la consuma, altrimenti un invio fallito la brucia.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio applica la catena dei varchi nell'ordine: `401` se il token manca o non è valido,
   `403` se l'app è spenta dalla piattaforma, `402` se l'account non è abilitato, `403` se il ruolo non basta,
   `429` se la quota del mese è esaurita.
2. **RF-2** — L'abilitazione si legge dalla **proiezione locale** alimentata a eventi: nessuna chiamata di rete
   sincrona all'app centrale sul percorso caldo.
3. **RF-3** — Esiste una risorsa `GET /api/preventivi/v1/quota` che dice usato, tetto e data di azzeramento
   della finestra.
4. **RF-4** — La Panoramica del modulo mostra il consumo (usati su tetto, con la finestra) e avvisa quando
   supera l'80 %.
5. **RF-5** — Il messaggio di quota esaurita dice tre cose: cosa è successo, cosa non si può più fare, come si
   rimedia (cambiare piano) — e non crea nulla.
6. **RF-6** — Con abbonamento in `past_due` la funzione resta accessibile; con `canceled` risponde `402`; i
   diritti dell'interessato (esportazione, cancellazione) restano accessibili in ogni caso.

## 3. Requisiti tecnici

- **RT-1 — Varchi e quota (§6, §7).** Prima di ogni invio il servizio prenota una unità della metrica
  `preventivi_inviati` (natura `flow`, finestra mensile); a quota esaurita risponde `429` con l'indicazione del
  rimedio. La prenotazione è nella stessa transazione dell'atto e viene rilasciata se l'atto fallisce.
- **RT-2 — Listino come codice (§7).** La storia non fissa prezzi: consuma il tetto pubblicato dall'abilitazione.
- **RT-3 — Isolamento fra account (§1).** Il contatore è per account, letto dal token verificato.
- **RT-4 — Interfaccia di programmazione (§2).** `GET /api/preventivi/v1/quota`; il `429` esce in
  `application/problem+json` con un codice stabile che il frontend può riconoscere.
- **RT-5 — Cinque lingue (§4).** I messaggi di quota e di abbonamento passano dallo spazio-nomi `preventivi` in
  tutte e cinque le lingue.
- **RT-6 — Abbonamento (§13).** Stati che danno accesso: `trialing`, `active`, `past_due`. Non danno accesso:
  `paused`, `canceled`.
- **RT-7 — Registrazione eventi (§14).** `quota prenotata`, `quota rilasciata`, `invio respinto per quota` con
  `tenant_id`, `app_id`, `user_id` e correlazione, senza dati personali.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.

## 4. Criteri di accettazione

**CA-1 — Consumo visibile**
- **Dato** un account sul piano `pro` che ha inviato 12 preventivi questo mese · **Quando** apre la Panoramica
- **Allora** legge «12 di 60 preventivi inviati — questo mese» e la data in cui il conteggio riparte

**CA-2 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `preventivi_inviati` · **Quando** tenta un invio · **Allora**
  riceve `429`, un messaggio che spiega come rimediare, e **nulla viene inviato né creato**

**CA-3 — La finestra si azzera**
- **Dato** un account che il mese scorso aveva esaurito il tetto · **Quando** comincia il mese nuovo · **Allora**
  può inviare di nuovo senza alcun intervento

**CA-4 — Abbonamento non attivo**
- **Dato** un account in stato `canceled` · **Quando** apre l'app · **Allora** riceve `402`, ma può comunque
  esportare i propri dati

**CA-5 — Bozze gratuite**
- **Dato** un account con quota esaurita · **Quando** crea o modifica un preventivo in bozza · **Allora**
  l'operazione riesce: la quota si consuma sull'invio, non sulla stesura

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sul contatore a finestra e di **integrazione** sui cinque varchi in ordine;
- [ ] prova di **isolamento fra account** sul contatore (il consumo di `A` non tocca `B`);
- [ ] **prova end-to-end**: rimando alla storia `0029`, che percorre anche il blocco per quota;
- [ ] **traduzioni** in tutte e cinque le lingue per i messaggi di quota e abbonamento;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato (finestra mensile, momento della prenotazione, comportamento in
      `past_due`);
- [ ] contratto degli **strumenti conversazionali**: nessuno qui, ma la regola vale anche per le chiamate
      dell'assistente (storia `0028`);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | serve una entità su cui applicare i varchi |
| storia `0003` | il consumo si mostra nella Panoramica |
| listino `preventivi.yaml` approvato dallo sviluppatore | i tetti vengono da lì |

## 7. Fuori ambito

- l'invio vero, che è l'atto che consuma: storia `0017`;
- la deroga temporanea al tetto concessa dall'assistenza: `estensioni-admin.md`.

## 8. Punti aperti

I tetti proposti (5 / 60 / 300 al mese) sono una proposta della descrizione dell'applicazione: li conferma lo
sviluppatore. La storia funziona con qualunque tetto.
