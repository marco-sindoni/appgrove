# 0004 — Abbonamento, piano e quota

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un salone che apre la quinta poltrona
> voglio che il programma me lo dica chiaramente e mi mostri come si fa a tenerla
> così da non scoprire il limite del mio piano mentre ho un cliente davanti.

**Contesto.** La catena dei varchi è di piattaforma; ciò che spetta a questa storia è **l'ultimo anello**: il tetto
sulla metrica `postazioni`, a giacenza. La scelta della natura `stock` è argomentata al §3 della descrizione e ha
una conseguenza pratica che va rispettata qui: il limite si incontra **quando si apre una postazione**, cioè
davanti a un utente autenticato che può capire e rimediare — mai davanti a un cliente finale che sta prenotando.

## 2. Requisiti funzionali

1. **RF-1** — Il listino del verticale esiste come file nel repository, con i piani, i limiti sulla metrica
   `postazioni` e le funzioni accese da ciascun piano. **I prezzi non li fissa questa storia**: arrivano dalla
   decisione dello sviluppatore (§5 della descrizione).
2. **RF-2** — L'apertura di una postazione oltre il tetto del piano è **rifiutata** con `429` e con un messaggio
   che dice quante ne sono aperte, quante ne prevede il piano e come si rimedia.
3. **RF-3** — Chiudere una postazione libera immediatamente una unità: la metrica è a giacenza e si legge dallo
   stato, non da un contatore che qualcuno deve ricordarsi di decrementare.
4. **RF-4** — Le sezioni verticali (cabina, pacchetti, fedeltà, provvigioni, fotografie) si accendono in base alle
   funzioni dichiarate dal piano, non a un elenco scritto nel codice del frontend.
5. **RF-5** — Il passaggio a un piano inferiore è **bloccato** finché le postazioni aperte superano il tetto del
   piano di destinazione, con l'indicazione di quante bisogna chiuderne.
6. **RF-6** — I **diritti dell'interessato** (esportazione e cancellazione) restano accessibili anche con
   abbonamento scaduto o app disabilitata.

## 3. Requisiti tecnici

- **RT-1 — Catena dei varchi (§6).** `401` senza token, `403` ad app spenta dalla piattaforma, `402` ad account
  non abilitato, `403` a ruolo insufficiente, `429` a quota esaurita. L'abilitazione si legge dalla **proiezione
  locale** alimentata a eventi: mai una chiamata di rete sincrona all'app centrale sul percorso caldo.
- **RT-2 — Listino come codice (§7).** File del listino registrato nell'indice; metrica **una sola**
  (`postazioni`, natura `stock`); solo abbonamento ricorrente, doppio ciclo mensile e annuale, blocco al limite e
  **mai** addebito a consumo. Sotto la via (b) non è un file nuovo ma un **piano in più** in `prenotazioni.yaml`,
  e la metrica resta `risorse_prenotabili`.
- **RT-3 — Stati che danno accesso (§13).** `trialing`, `active`, `past_due` danno accesso; `paused` e `canceled`
  no. Con `past_due` la funzione resta accessibile (periodo di tolleranza); con `canceled` risponde `402`.
- **RT-4 — Isolamento fra account (§1).** Il conteggio delle postazioni aperte è per account e si calcola dal
  token verificato.
- **RT-5 — Modulo frontend (§3, §5).** L'avviso di quota compare **prima** dell'azione che la consuma, non dopo il
  salvataggio; le sezioni spente portano il motivo e il rimedio; solo token del sistema di design.
- **RT-6 — Cinque lingue (§4).** Messaggi di quota, di piano insufficiente e di blocco del passaggio a un piano
  inferiore in `en, it, fr, es, de`.
- **RT-7 — Registrazione eventi (§14).** `postazione aperta`, `apertura respinta per quota`, `passaggio di piano
  bloccato` con `tenant_id`, `app_id`, `user_id` e correlazione, senza dati personali.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.

## 4. Criteri di accettazione

**CA-1 — Apertura entro il tetto**
- **Dato** un account sul piano `salone` (quattro postazioni) con tre aperte
- **Quando** ne apre una quarta
- **Allora** l'operazione riesce e il consumo mostra quattro su quattro

**CA-2 — Quota esaurita**
- **Dato** lo stesso account con quattro postazioni aperte
- **Quando** tenta di aprirne una quinta
- **Allora** riceve `429`, il messaggio dice «quattro su quattro» e come si rimedia, e **nulla viene creato**

**CA-3 — La quota si libera chiudendo**
- **Dato** l'account al limite
- **Quando** chiude una postazione e ne apre un'altra
- **Allora** l'apertura riesce senza nessun intervento manuale sul conteggio

**CA-4 — Passaggio a un piano inferiore bloccato**
- **Dato** un account con sei postazioni aperte che vuole passare al piano `salone` (quattro)
- **Quando** chiede il cambio
- **Allora** il cambio è rifiutato con l'indicazione che deve chiuderne due, e nessuna postazione viene chiusa
  d'autorità

**CA-5 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled`
- **Quando** apre una qualunque sezione del verticale
- **Allora** riceve `402`; **ma** l'esportazione dei propri dati resta accessibile

**CA-6 — Isolamento fra account**
- **Dato** due account, uno al limite e uno no
- **Quando** quello al limite tenta di aprire una postazione forzando l'identificativo dell'altro account
- **Allora** il tentativo è ignorato e la risposta resta `429`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, e la suite intera prima del commit);
- [ ] prove di **unità** sul conteggio a giacenza e di **integrazione** sui varchi;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulle operazioni di apertura e chiusura;
- [ ] prova di **pagamenti** di livello 1 e 2 se la storia tocca gli eventi di abbonamento;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-SALONGROVE]` della storia `0030`;
- [ ] **traduzioni** in tutte e cinque le lingue per tutti i messaggi di varco;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni**: metrica, natura, funzioni accese da ciascun piano, comportamento del passaggio
      a un piano inferiore. **I prezzi restano una decisione dello sviluppatore e vanno riportati come tale**;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | serve una nozione di postazione da contare |
| storia `0003` | l'avviso e le sezioni spente vivono nel modulo |
| **decisione sui prezzi** (§5 della descrizione) | fermata di escalation: la storia implementa il meccanismo, non fissa i numeri |

## 7. Fuori ambito

- la scelta dei prezzi, dei tetti e della durata della prova: fermata di escalation dello sviluppatore;
- l'acquisto e la disdetta self-service: sono di piattaforma;
- il consumo di eventuali metriche secondarie: **non esistono**, la metrica è una sola.

## 8. Punti aperti

**Che cosa conta come «postazione aperta».** Un macchinario prenotabile è una postazione? Una cabina con due
lettini è una o due? La proposta è: conta **ciò che si può prenotare in modo indipendente**, perché è ciò che
genera valore. Ma è una definizione con conseguenze sul prezzo percepito, e va confermata insieme al listino.
