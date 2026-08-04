# 0004 — Abbonamento e quota dei posti operatore

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha attivato DeskGrove
> voglio sapere in ogni momento quanti posti operatore sto usando dei miei, e ricevere una risposta chiara quando
> li ho finiti
> così da non scoprire il limite nel momento sbagliato e da capire subito come si rimedia.

**Contesto.** L'app esiste e si vede, ma nessuno paga per essa e nulla la limita. Questa storia mette in piedi la
catena dei varchi e la metrica: il listino come file nel repository, la lettura dell'abilitazione dalla proiezione
locale, il conteggio dei posti operatore e il blocco quando il tetto è raggiunto. Va fatta **prima** delle storie
di dominio perché il varco si aggiunge male dopo: se venti risorse nascono senza, diciannove lo avranno e una no.
La metrica è **una sola** — i posti operatore — ed è a **giacenza**: è un tetto su ciò che esiste ora, non un
consumo che si azzera il primo del mese. La differenza non è formale: una giacenza contata come consumo lascia
accumulare senza limite, un consumo contato come giacenza blocca l'utente per sempre.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il listino `services/core/src/main/resources/pricing/helpdesk.yaml`, registrato nell'indice
   dei listini, con i piani, la metrica `agents` di natura `stock` e le descrizioni nelle cinque lingue.
2. **RF-2** — Il servizio legge l'abilitazione dell'account dalla **proiezione locale** alimentata a eventi e ne
   ricava il tetto dei posti operatore del piano attivo.
3. **RF-3** — Esiste il conteggio dei posti occupati per account, esposto sia al servizio sia all'interfaccia.
4. **RF-4** — Una richiesta che occuperebbe un posto oltre il tetto viene respinta con `429` e un messaggio che
   dice quanti posti ci sono, quanti ne servono e come si rimedia — non un errore generico.
5. **RF-5** — Un account senza abbonamento attivo riceve `402`; un account con abbonamento in tolleranza per
   pagamento non riuscito continua invece ad accedere.
6. **RF-6** — L'interfaccia mostra il consumo dei posti nel modulo (quanti usati su quanti disponibili) e, quando
   il tetto è raggiunto, un avviso che dice cosa non si può più fare e come si sblocca.
7. **RF-7** — L'esportazione e la cancellazione dei dati restano accessibili **anche** con l'app disabilitata o
   l'abbonamento scaduto.

## 3. Requisiti tecnici

- **RT-1 — Varchi e quota (§6, §7).** L'accesso attraversa cinque varchi e **solo l'ultimo** è dell'app: token
  valido altrimenti `401`; app non spenta dalla piattaforma altrimenti `403`; account abilitato altrimenti `402`;
  ruolo sufficiente altrimenti `403`; quota non esaurita altrimenti `429`. Prima di assegnare un posto operatore il
  servizio **prenota** una unità della metrica `agents`; a tetto raggiunto risponde `429` con l'indicazione del
  rimedio.
- **RT-2 — Listino come codice (§7).** Il file del listino sta nel repository, non in un pannello a runtime: solo
  abbonamento ricorrente, doppio ciclo mensile e annuale con l'annuale in evidenza, prova gratuita disattivabile,
  metrica che dichiara la propria natura, blocco al limite senza addebito a sorpresa, **prezzi immutabili** una
  volta vivi. Il valore `category` del listino è `teal` e **coincide** con il colore d'accento del modulo frontend.
- **RT-3 — Abbonamento self-service (§13).** Danno accesso gli stati «in prova», «attivo» e «pagamento non
  riuscito» (con la tolleranza prevista); non danno accesso «sospeso» e «disdetto». Il passaggio a un piano
  inferiore è **bloccato** finché i posti occupati eccedono il tetto del piano di destinazione: è la conseguenza
  diretta di una metrica a giacenza. In locale il fornitore di pagamento è **sempre simulato**.
- **RT-4 — Isolamento fra account (§1).** Il conteggio dei posti si calcola sempre sull'account del token
  verificato; nessun percorso permette di leggere o consumare la quota di un altro account.
- **RT-5 — Interfaccia di programmazione (§2).** Le risposte `402` e `429` escono in `application/problem+json` con
  un tipo distinguibile a programma, non solo con un testo per l'essere umano; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-6 — Modulo frontend e cinque lingue (§3, §4, §5).** L'avviso di quota e la barra di consumo usano i token
  del sistema di design, funzionano in tema chiaro e scuro e hanno tutte le stringhe nello spazio-nomi `helpdesk`
  nelle cinque lingue.
- **RT-7 — Registrazione eventi (§14).** «Posto assegnato», «posto liberato» e «assegnazione respinta per quota» si
  registrano con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la quota conta posti, non persone. Il legame fra
  posto e utente lo introduce la storia `0018`.
- **RT-9 — Esposizione conversazionale (§12).** Nessuno strumento proprio: la quota è un varco, non una funzione.
  Vale però la regola di piattaforma che **anche** le chiamate provenienti dal livello conversazionale attraversano
  abilitazione e quota (UC 0064): la storia lo dichiara come vincolo per chi implementerà l'epica 07.

## 4. Criteri di accettazione

**CA-1 — Il tetto viene rispettato**
- **Dato** un account sul piano con tre posti operatore, con tre posti già occupati
- **Quando** si prova ad assegnare un quarto posto
- **Allora** la risposta è `429`, il messaggio dice «tre posti su tre occupati, per aggiungerne uno libera un posto
  o passa al piano superiore», e **nessun** posto viene assegnato

**CA-2 — Il posto liberato torna disponibile**
- **Dato** lo stesso account, con un posto liberato
- **Quando** si assegna un posto a una persona nuova
- **Allora** l'operazione riesce: la metrica è a giacenza e non tiene memoria di ciò che è passato

**CA-3 — Senza abbonamento non si entra**
- **Dato** un account con abbonamento disdetto e periodo concluso
- **Quando** si chiama una qualunque risorsa dell'app
- **Allora** la risposta è `402` con l'indicazione di come riattivare

**CA-4 — Con pagamento non riuscito si continua**
- **Dato** un account con pagamento non riuscito, dentro il periodo di tolleranza
- **Quando** si chiama una risorsa dell'app
- **Allora** la risposta è quella normale: l'accesso non si interrompe al primo addebito fallito

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con piani diversi
- **Quando** un utente di `A` legge il proprio consumo
- **Allora** vede solo il proprio tetto e il proprio conteggio, anche forzando l'identificativo di `B` nella
  richiesta

**CA-6 — I diritti delle persone non si bloccano**
- **Dato** un account con l'app disabilitata
- **Quando** si chiede l'esportazione o la cancellazione dei dati
- **Allora** l'operazione resta accessibile: non è una funzione commerciale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del consumo e sulla decisione del varco, e di **integrazione** sulla risorsa
      protetta con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul conteggio e sul tetto;
- [ ] prove sui **pagamenti** di livello 1 (eventi del fornitore con carichi sintetici firmati) e di livello 2
      (percorso con fornitore simulato); mai guidare con il browser la finestra del fornitore di pagamento;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-HELPDESK]` nasce con la storia `0037` e comprenderà il
      passaggio del blocco per quota; la voce del registro di copertura è di quella storia;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese le descrizioni dei piani nel listino;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con annotata la natura a giacenza della metrica e il perché non è un
      consumo mensile;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` di questa app | Serve il servizio dove mettere il varco |
| Storia `0003` di questa app | L'avviso e la barra di consumo vivono nel modulo frontend |
| Conferma dello sviluppatore su prezzi e tetti (§5 del documento capofila) | Senza quella conferma il file del listino non si può scrivere: è una fermata di escalation |
| Proiezione locale dell'abilitazione alimentata a eventi | L'abilitazione **non** si chiede con una chiamata sincrona all'app centrale sul percorso caldo |

## 7. Fuori ambito

- **L'assegnazione vera dei posti alle persone**: qui c'è il conteggio e il blocco, non l'entità dell'operatore.
  La introduce la storia `0018`.
- **La deroga temporanea sui posti** durante una migrazione: è una funzione della console di amministrazione, non
  dell'app (vedi [estensioni-admin.md](../estensioni-admin.md)).
- **La conservazione come parametro del piano**: proposta nel documento capofila come rimedio al fatto che i posti
  non limitano il volume; la tratta la storia `0036`.

## 8. Punti aperti

- ⚠️ **Prezzi, tetti dei piani e durata della prova gratuita**: fermata di escalation dello sviluppatore. Il
  documento capofila (§5) propone tre piani — uno gratuito da un posto, uno da tre posti a 24 € al mese, uno da
  dieci posti a 59 € al mese — ma sono **proposte da confermare**, e questa storia non le può scrivere prima della
  conferma.
- **Il volume non è limitato da nessuna metrica**: la quota conta i posti, non le richieste né gli allegati
  conservati. È voluto (una metrica sola, e limitare le richieste punirebbe il cliente proprio quando ha più
  bisogno dello strumento), ma lascia scoperto il costo di archiviazione. Il rimedio proposto è la conservazione
  come parametro del piano: lo decide lo sviluppatore nella storia `0036`.
- **Il canale WhatsApp riservato al piano superiore** è una proposta del documento capofila, non una decisione: se
  confermata, cambia le funzioni per piano nel file del listino.
