# 0004 — Abbonamento e quota sulle misure

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un account
> voglio sapere quante misure ho consumato del mio piano e cosa succede quando finiscono
> così da non scoprire il limite nel momento peggiore, cioè quando smetto di vedere la mia spesa.

**Contesto.** La metrica di quota è `misure_registrate`, di natura a consumo su finestra mensile (§3 del documento
capofila). Qui c'è una particolarità che va affrontata con attenzione: in questa app **la quota si esaurisce da
sola**, senza che l'utente clicchi nulla — sono le misure che arrivano dal prodotto del cliente e dai rendiconti a
consumarla. Un blocco silenzioso significherebbe che il cliente smette di vedere la propria spesa senza capire
perché, proprio mentre la spesa continua a esserci. Va quindi trattato come un evento da annunciare **prima**, non
da subire.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio mantiene una proiezione locale dell'abilitazione dell'account, alimentata a eventi: mai
   una chiamata di rete sincrona all'app centrale sul percorso caldo.
2. **RF-2** — Prima di registrare una misura il servizio prenota una unità della metrica `misure_registrate`; a
   quota esaurita risponde `429` con un messaggio che dice cosa è successo, che cosa non si può più fare e come si
   rimedia.
3. **RF-3** — Il consumo della quota è visibile nel modulo (barra con consumato, tetto e finestra) e non solo al
   momento del rifiuto.
4. **RF-4** — Al superamento dell'80% del tetto l'account riceve un avviso — nell'interfaccia e per posta al
   titolare — con la stima di quando si esaurirà al ritmo attuale.
5. **RF-5** — Quando la quota è esaurita, **la lettura resta accessibile**: si smette di registrare misure nuove,
   non di guardare quelle già registrate. Un cliente che ha pagato per i dati dello scorso mese continua a vederli.
6. **RF-6** — Con abbonamento in stato di tolleranza per pagamento fallito la funzione resta accessibile; con
   abbonamento disdetto e periodo scaduto risponde `402`. L'esportazione dei dati resta accessibile in ogni caso.

## 3. Requisiti tecnici

- **RT-1 — Varchi e quota (§6, §7).** La catena completa: gettone valido altrimenti `401`; app non spenta
  altrimenti `403`; account abilitato altrimenti `402`; ruolo sufficiente altrimenti `403`; quota non esaurita
  altrimenti `429`. Solo l'ultimo gradino è responsabilità dell'app. La metrica è `misure_registrate`, natura a
  consumo su finestra mensile; il tetto arriva dall'abilitazione, **non** è scritto nel codice dell'app.
- **RT-2 — Isolamento fra account (§1).** Il conteggio della quota è per `tenant_id` preso dal gettone verificato;
  nessun conteggio globale che possa mescolare account.
- **RT-3 — Interfaccia di programmazione (§2).** Rotta `GET /api/spesa_modelli/v1/quota` che restituisce
  consumato, tetto, finestra e istante di azzeramento; errori in `problem+json` con il campo che indica il rimedio.
- **RT-4 — Modulo frontend (§3, §5).** Barra di consumo nella panoramica e avviso in testa quando si supera l'80%;
  solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I messaggi di quota — che sono fra i pochi che l'utente leggerà davvero con
  attenzione — sono presenti in `en, it, fr, es, de`.
- **RT-6 — Registrazione eventi (§14).** Gli eventi «misura respinta per quota», «soglia dell'80% superata» e
  «finestra azzerata» sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza
  dati personali.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo.

## 4. Criteri di accettazione

**CA-1 — Il consumo è visibile prima del limite**
- **Dato** un account sul piano intermedio con 410.000 misure registrate su un tetto di 500.000
- **Quando** apre la panoramica
- **Allora** vede la barra con «410.000 di 500.000 misure — questo mese» e l'avviso di superamento dell'80% con la
  stima della data di esaurimento

**CA-2 — Quota esaurita: si blocca la scrittura, non la lettura**
- **Dato** un account che ha raggiunto il tetto della metrica `misure_registrate`
- **Quando** il suo prodotto invia una nuova misura
- **Allora** riceve `429` con un messaggio che spiega il rimedio, nulla viene registrato, **e** l'account continua
  a poter consultare ed esportare le misure già presenti

**CA-3 — Il tetto non è scritto nel codice**
- **Dato** un account il cui piano viene cambiato dall'abilitazione
- **Quando** si interroga la rotta della quota
- **Allora** il tetto restituito è quello del nuovo piano, senza alcun rilascio dell'app

**CA-4 — Abbonamento non attivo**
- **Dato** un account con abbonamento disdetto e periodo pagato terminato
- **Quando** chiama una qualunque rotta di lettura della spesa
- **Allora** riceve `402`; l'esportazione dei propri dati resta invece accessibile

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, di cui `A` ha esaurito la quota
- **Quando** `B` invia una misura
- **Allora** `B` non è bloccato: i contatori sono separati

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio e sull'azzeramento della finestra, e di **integrazione** sulla rotta della
      quota con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sui contatori;
- [ ] **prova end-to-end**: si **rimanda** alla storia `0034` (proprietaria del percorso `[J-SPESA-MODELLI]`), che
      include il passo del blocco per quota;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, messaggi di errore compresi;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di bloccare la scrittura ma non la
      lettura;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Serve il servizio con le sue rotte |
| Storia `0002` | Serve la tabella delle misure da contare |
| Listino dell'app approvato (§5 del documento capofila) | I tetti dei piani arrivano da lì |

## 7. Fuori ambito

- la **deroga temporanea** concessa dall'assistenza per il recupero iniziale dello storico: è nella console di
  amministrazione ([estensioni-admin.md](../estensioni-admin.md) §3);
- la conservazione differenziata dello storico per piano (30 giorni / 13 mesi / 25 mesi): è una funzionalità del
  piano che va applicata alla cancellazione periodica; **si rimanda** e la possiede la storia `0035`, insieme alla
  cancellazione dei dati.

## 8. Punti aperti

- **Che cosa consuma esattamente una unità di quota quando la misura arriva dal rendiconto del fornitore.** Un
  rendiconto giornaliero aggregato è una riga sola pur rappresentando migliaia di chiamate: contarla come una
  misura è generoso e coerente («una riga registrata»), ma rende i due modi di entrare non confrontabili. La
  proposta è contare le **righe registrate**, qualunque sia la loro origine, e dirlo con chiarezza nel listino. È
  una decisione di prodotto con effetti sui prezzi: la chiude lo sviluppatore insieme al punto P1.
