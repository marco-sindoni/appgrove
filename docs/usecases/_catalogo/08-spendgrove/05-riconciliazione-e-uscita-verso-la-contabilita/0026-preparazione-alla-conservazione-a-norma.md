# 0026 — Preparazione alla conservazione a norma

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 05 — Riconciliazione e uscita verso la contabilità
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0025`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena digitalizzato trecento scontrini
> voglio sapere con chiarezza se posso buttare la carta, e cosa mi serve per poterlo fare
> così da non distruggere documenti che devo ancora conservare, e da non tenere in ufficio scatoloni che potrei
> aver smesso di tenere.

**Contesto.** È l'aspettativa numero uno del cliente ed è la delusione più probabile (descrizione, §2.5). La legge
lo permette: scontrini e ricevute sono documenti analogici **originali non unici**, quindi la copia informatica può
sostituire la carta senza l'intervento di un pubblico ufficiale — ma solo se il documento digitale è portato in
**conservazione a norma** secondo le Linee guida dell'Agenzia per l'Italia digitale, con firma digitale e marca
temporale a garanzia di autenticità, integrità e immodificabilità (descrizione, §2.3, fonte 6). SpendGrove
**non è un conservatore accreditato** e non lo diventa con una storia. Questa storia fa due cose oneste: prepara
tutto ciò che serve, e dice al cliente, dove lo legge, che cosa manca.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni periodo si produce un **pacchetto di versamento in conservazione**: i giustificativi, i loro
   metadati (tipo di documento, data, importo, soggetto, riferimento della spesa) e l'indice, in un formato aperto
   e documentato.
2. **RF-2** — Ogni documento porta la sua **impronta**, calcolata al caricamento (storia `0006`) e verificabile in
   qualunque momento; l'app espone una funzione di verifica che dice se un file è ancora identico a quello caricato.
3. **RF-3** — L'interfaccia dichiara in modo esplicito, in una schermata dedicata e nel testo accanto al pacchetto,
   che **SpendGrove non è un conservatore a norma** e che per distruggere la carta serve un servizio di
   conservazione conforme.
4. **RF-4** — Il pacchetto è consegnabile a un conservatore terzo: l'app lo produce e lo rende scaricabile; **non**
   appone firma digitale né marca temporale, perché sono l'atto del conservatore.
5. **RF-5** — Si registra, per ciascun periodo, l'esito dichiarato dal cliente («versato al conservatore X il
   giorno Y, con riferimento Z»), così che l'app sappia dire quali periodi sono coperti e quali no.
6. **RF-6** — La ritenzione predefinita dei documenti dentro l'app è **dieci anni** dalla chiusura dell'esercizio,
   coerente con la conservazione delle scritture, ed è dichiarata all'utente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Pacchetti di versamento e registrazioni filtrano per `tenant_id` preso
  dal token verificato; la funzione è riservata al ruolo `amministra`, e l'indirizzo di scaricamento è firmato, a
  scadenza breve e verificato contro l'account.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/notespese/v1/versamenti`,
  `GET /api/notespese/v1/versamenti`, `POST /api/notespese/v1/versamenti/{id}/esito`,
  `GET /api/notespese/v1/ricevute/{id}/verifica-impronta`; errori in `application/problem+json`; definizione
  OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V23__versamenti_conservazione.sql`: tabella `versamento_conservazione`
  con `tenant_id`, chiave UUID versione 7, periodo, stato, impronta del pacchetto, dati dell'esito dichiarato,
  colonne di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Esportazioni → Conservazione*: elenco dei periodi con lo stato di
  copertura, produzione del pacchetto, registrazione dell'esito, e **il riquadro che dice cosa l'app non fa**. Solo
  token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I testi passano dallo spazio-nomi `notespese` e sono presenti in
  `en, it, fr, es, de`, **dichiarando la giurisdizione**: la conservazione a norma come qui descritta è la
  disciplina italiana, e negli altri Paesi le regole sono altre.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo della metrica `receipts`. Con abbonamento `canceled` la
  produzione risponde `402`; restano invece sempre accessibili i diritti dell'interessato (storia `0030`).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: produrre un pacchetto di versamento
  è un atto amministrativo con conseguenze, e la registrazione dell'esito è una dichiarazione del cliente. In
  lettura, `stato_conservazione(periodo) → coperto | scoperto` è dichiarato fra le letture ammesse.
- **RT-8 — Dati personali (§10).** Come per il pacchetto per il commercialista, qui si concentra molto: voce nuova
  nel manifesto in italiano e inglese, tabella e pacchetti in `exportData` e `purgeData`. **La ritenzione decennale
  va dichiarata come tale** e legata al suo obbligo di legge — è la parte che si scontra con la richiesta di
  cancellazione di un ex collaboratore (punto aperto n. 7 della descrizione).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `pacchetto di versamento prodotto`, `esito registrato`,
  `impronta verificata` portano `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e periodo — mai
  contenuti.

## 4. Criteri di accettazione

**CA-1 — Pacchetto di versamento**
- **Dato** un trimestre con trenta giustificativi
- **Quando** si produce il pacchetto di versamento
- **Allora** contiene i trenta file, i loro metadati e l'indice, e la sua impronta è dichiarata

**CA-2 — Verifica dell'impronta**
- **Dato** una ricevuta caricata sei mesi fa · **Quando** se ne verifica l'impronta
- **Allora** l'esito dice che il file è identico a quello caricato, con la data del caricamento

**CA-3 — Il limite è dichiarato**
- **Dato** un utente che apre la sezione Conservazione
- **Quando** legge la schermata
- **Allora** trova scritto in modo non ambiguo che l'app **non** è un conservatore a norma e che cosa serve per
  poter distruggere la carta

**CA-4 — Copertura per periodo**
- **Dato** due trimestri, di cui uno con esito di versamento registrato
- **Quando** si apre l'elenco
- **Allora** uno risulta coperto con i riferimenti dichiarati e l'altro scoperto

**CA-5 — Ruolo insufficiente**
- **Dato** un collaboratore con ruolo `approva` · **Quando** tenta di produrre un pacchetto di versamento
- **Allora** riceve `403`

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** l'uno tenta di scaricare il pacchetto di versamento dell'altro
- **Allora** l'accesso è negato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione dell'indice e sulla verifica dell'impronta; di **integrazione** sulla
      produzione con database effimero, migrazioni vere e archivio simulato;
- [ ] prova di **isolamento fra account** e di ruolo su versamenti e scaricamenti;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che nel percorso `[J-NOTESPESE]` verifica la presenza e la
      leggibilità del riquadro che dichiara il limite; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con la giurisdizione dichiarata;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la ritenzione decennale motivata dall'obbligo di
      legge;
- [ ] **registro delle decisioni** compilato, con la scelta di **non** diventare conservatore e di dichiararlo;
- [ ] contratto dello strumento `stato_conservazione` dichiarato, marcato lettura;
- [ ] documentazione e testi di prodotto aggiornati: il limite va detto anche fuori dall'app (landing, aiuto), non
      solo dentro.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | L'impronta si calcola al caricamento: senza, la verifica non esiste |
| `0025` | Pacchetto per il commercialista e pacchetto di versamento sono due cose diverse e vanno progettate insieme per non confonderle |

## 7. Fuori ambito

- **Firma digitale e marca temporale**: sono l'atto del conservatore, non nostro.
- L'accreditamento come conservatore: è un'impresa a sé, non una storia.
- La conservazione dei documenti aziendali in generale: è VaultGrove (catalogo 18).

## 8. Punti aperti

- 🛑 **Fino a dove arriva l'app.** Tre strade: restare qui (produciamo il pacchetto, il cliente sceglie il
  conservatore), integrare un conservatore terzo (che diventa un fornitore che tratta dati per nostro conto), o
  offrire il servizio attraverso VaultGrove. È il punto aperto n. 4 della descrizione dell'applicazione e lo chiude
  lo sviluppatore.
- **Come si comunica il limite senza far sembrare l'app incompleta**: è un tema di prodotto e di testi, non solo di
  interfaccia. Meglio dirlo che lasciarlo credere, ma va detto bene.
