# 0018 — Registrazione manuale dell'incasso

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0018` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetta alla reception
> voglio segnare in due clic che una quota è rientrata, o che non è rientrata e perché
> così da avere sempre sott'occhio l'elenco vero di chi deve ancora pagare.

**Contesto.** È la via **predefinita** con cui l'esito di un incasso entra in SubGrove, ed è il pilastro pratico
della postura del §5.2 della descrizione: appgrove non incassa, quindi qualcuno deve dirle com'è andata. Le altre
due vie — l'importazione da file (storia `0019`) e il collegamento in sola lettura al fornitore del cliente
(storia `0020`) — sono comodità che si aggiungono; questa deve funzionare da sola, sempre, anche per il cliente
che incassa in contanti al banco. Piccola ma centrale: senza di lei tutta l'epica dei solleciti non ha su cosa
poggiare.

## 2. Requisiti funzionali

1. **RF-1** — Una scadenza `in_attesa` si può marcare **incassata**, indicando data, importo e — facoltativi —
   modo di incasso e riferimento (numero di distinta, di ricevuta, di operazione).
2. **RF-2** — Una scadenza si può marcare **fallita**, scegliendo il motivo da un elenco chiuso (fondi
   insufficienti, autorizzazione revocata o decaduta, dati non validi, contestazione, altro) più una nota
   facoltativa.
3. **RF-3** — È ammesso l'incasso **parziale**: la scadenza resta aperta per il residuo, e l'interfaccia mostra
   sempre quanto manca.
4. **RF-4** — Una registrazione si può **correggere** (importo o data) e **annullare**; ogni correzione lascia
   traccia di chi e quando, perché sono i movimenti su cui poi si litiga.
5. **RF-5** — Marcare incassata una scadenza riporta l'abbonamento da `in_ritardo` ad `attivo`, se era lì per
   quella scadenza; marcarla fallita lo porta a `in_ritardo` e apre la catena dei solleciti.
6. **RF-6** — L'incasso registrato aggiorna la **data di ultimo utilizzo** dell'autorizzazione all'addebito, se
   ce n'è una: è ciò che tiene onesto il conteggio della decadenza (storia `0017`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni registrazione agisce su scadenze dell'account del token
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/abbonati/v1/scadenze/{id}/incasso` e
  `POST /api/abbonati/v1/scadenze/{id}/fallimento`, più `DELETE` per annullare la registrazione; errori in
  `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V13__registrazione_incasso.sql`: tabella `registrazione_incasso` con
  `tenant_id`, colonne di controllo, importo, data, modo, riferimento, autore. La scadenza **non** tiene un
  totale incassato scritto a mano: lo si deriva dalle registrazioni.
- **RT-4 — Ciclo di vita (§ storia `0011`).** I passaggi `attivo ↔ in_ritardo` passano dalla macchina a stati e
  registrano il motivo.
- **RT-5 — Modulo frontend (§3, §5).** Dall'elenco delle scadenze, azione rapida su ogni riga (due clic, non un
  modulo) e azione in blocco su una selezione; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Motivi di fallimento, etichette e messaggi in `en, it, fr, es, de`.
- **RT-7 — Varchi e quota (§6).** Nessun consumo di quota. Con abbonamento di piattaforma non attivo, `402`.
- **RT-8 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `registra_incasso(scadenza, importo, data, riferimento) → bozza`, marcato **scrittura**: produce una bozza e
  richiede conferma umana, perché dichiarare incassato ciò che non lo è spegne i solleciti.
- **RT-9 — Dati personali (§10).** Nessun dato personale nuovo: importi e riferimenti di operazione, riferiti a
  una scadenza già trattata. La tabella entra comunque in `exportData` e `purgeData` perché è legata a una
  persona per relazione.
- **RT-10 — Registrazione eventi (§14).** `incasso registrato`, `fallimento registrato (motivo)`,
  `registrazione annullata`, con `tenant_id`, `app_id`, `user_id` e correlazione, senza nomi.

## 4. Criteri di accettazione

**CA-1 — Incasso completo**
- **Dato** una scadenza da 39 € in attesa
- **Quando** l'addetta la marca incassata con importo 39 € e data odierna
- **Allora** la scadenza risulta incassata, sparisce dagli scoperti, e se l'abbonamento era `in_ritardo` torna
  `attivo`

**CA-2 — Incasso parziale**
- **Dato** la stessa scadenza · **Quando** si registra un incasso di 20 €
- **Allora** la scadenza resta aperta, mostra «mancano 19 €», e l'abbonamento non torna attivo

**CA-3 — Fallimento con motivo**
- **Dato** una scadenza con addebito respinto · **Quando** si registra il fallimento «fondi insufficienti»
- **Allora** l'abbonamento passa a `in_ritardo` e la catena dei solleciti si apre

**CA-4 — Correzione tracciata**
- **Dato** un incasso registrato con importo sbagliato · **Quando** lo si corregge
- **Allora** la scadenza si aggiorna e la cronologia mostra il valore precedente, chi ha corretto e quando

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** uno prova a registrare un incasso su una scadenza dell'altro
- **Allora** riceve una risposta di risorsa inesistente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sul residuo dell'incasso parziale e sui passaggi di stato conseguenti; **integrazione**
      sulle rotte;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-ABBONATI]` registra un fallimento e poi un incasso,
      verificando i due passaggi di stato; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la registrazione dell'incasso;
- [ ] **registro delle decisioni** compilato: totale derivato e non memorizzato, elenco chiuso dei motivi,
      aggiornamento della data d'uso dell'autorizzazione;
- [ ] contratto dello strumento `registra_incasso` dichiarato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | servono scadenze da chiudere |
| storia `0011` | i passaggi di stato passano dalla macchina |
| storia `0017` | l'aggiornamento della data d'uso richiede l'autorizzazione |

## 7. Fuori ambito

- l'importazione in blocco degli esiti da un file: storia `0019`;
- la lettura automatica dal fornitore del cliente: storia `0020`;
- i solleciti che seguono un fallimento: storia `0021`;
- la sospensione automatica: storia `0022`.

## 8. Punti aperti

**Nessuno.** La storia è deliberatamente semplice e non ha margini da decidere: è il minimo che deve funzionare
perché l'app stia in piedi senza alcuna integrazione.
