# 0008 — Collegamento a Trustpilot

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 02 — Sedi e collegamento alle piattaforme
**Storia**: `0008` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che raccoglie recensioni anche su Trustpilot
> voglio collegare il mio profilo Trustpilot alla sede, con le mie credenziali
> così da vedere in un posto solo quello che i clienti scrivono su due piattaforme diverse.

**Contesto. Perché una storia separata da 0007.** Trustpilot non è «Google con un altro nome»: il suo modello è
diverso e cambia il codice. L'unità di riferimento non è la sede fisica ma l'**unità aziendale**, che Trustpilot
lega a un dominio internet
([Business Units overview](https://developers.trustpilot.com/business-units-api-overview/)); l'accesso richiede un
account Trustpilot for Business con il modulo di connessione, e la parte pubblica delle interfacce si usa con una
chiave mentre la parte privata richiede la delega dell'utente aziendale
([Service Reviews API](https://developers.trustpilot.com/service-reviews-api/)). Ne discende un problema di
prodotto che va detto al cliente: **un'unità aziendale non corrisponde a una sede**, quindi un cliente con tre
punti vendita e un solo dominio vedrà le stesse recensioni su tutte e tre le sedi, a meno che non abbia unità
distinte. L'app deve dirlo, non nasconderlo.

Una nota di onestà: non ho trovato a quale piano di Trustpilot corrisponda il modulo di connessione né quanto
costi al cliente (descrizione §2.7). L'app deve quindi comportarsi bene anche quando il cliente scopre di non
averlo.

## 2. Requisiti funzionali

1. **RF-1** — Dalla scheda di una sede si collega Trustpilot indicando il dominio dell'attività; l'app risolve
   l'unità aziendale corrispondente e la mostra al cliente perché la confermi.
2. **RF-2** — Il collegamento richiede le credenziali del **contratto del cliente** con Trustpilot: è lui il
   titolare del rapporto, noi siamo il canale. L'app spiega in una riga cosa serve e cosa succede se manca.
3. **RF-3** — Se lo stesso dominio è già collegato a un'altra sede dello stesso account, l'app lo dice
   esplicitamente e chiede conferma: «le due sedi mostreranno le stesse recensioni».
4. **RF-4** — Stati, revoca e cifratura dei segreti funzionano come per Google (storia 0007): stessa macchina a
   stati, stessa schermata, stessa regola sui registri.
5. **RF-5** — Se le credenziali non danno accesso alla parte privata delle interfacce, l'app lo dice in parole
   comprensibili («il tuo piano Trustpilot non comprende l'accesso necessario») e non finge di funzionare a metà.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Come per 0007: ogni lettura e scrittura filtra per `tenant_id` dal token
  verificato; lo stato dello scambio di autorizzazione porta con sé l'account e viene verificato al ritorno.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte parallele a quelle di Google, con `trustpilot` al posto di
  `google`; errori in `application/problem+json` con un codice che distingue «dominio non trovato», «accesso non
  compreso nel piano», «credenziali rifiutate». Definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Stessa tabella `collegamento_piattaforma`; l'identificativo esterno contiene
  l'unità aziendale. Nessuna migrazione nuova.
- **RT-4 — Modulo frontend (§3, §5).** Stesso riquadro «Piattaforme collegate» della storia 0007, con la riga di
  avviso sull'unità aziendale condivisa fra sedi quando ricorre.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe, avvisi compresi, in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Solo sedi `attive`; ruolo `admin` o `owner`; `402` con abbonamento non
  attivo. Il collegamento **non** consuma quota per sé: la quota è la sede, non la piattaforma.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento, per la stessa ragione della storia 0007.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo; **secondo fornitore esterno** aggiunto all'elenco
  dei fornitori e all'informativa.
- **RT-9 — Registrazione eventi (§14).** Come per 0007, con la piattaforma nel campo dedicato. Mai le credenziali.

## 4. Criteri di accettazione

**CA-1 — Collegamento riuscito**
- **Dato** un account con una sede attiva e credenziali Trustpilot valide
- **Quando** indica il dominio, conferma l'unità aziendale e completa la delega
- **Allora** il collegamento risulta `attivo` e la scheda della sede mostra due piattaforme

**CA-2 — Piano insufficiente**
- **Dato** credenziali valide ma senza accesso alla parte privata delle interfacce
- **Quando** si completa il collegamento
- **Allora** l'app mostra un messaggio che spiega il motivo e lascia il collegamento in stato `in errore`, senza
  raccogliere nulla a metà

**CA-3 — Stesso dominio su due sedi**
- **Dato** una sede già collegata al dominio `esempio.test`
- **Quando** si collega una seconda sede allo stesso dominio
- **Allora** l'app avvisa che le due sedi mostreranno le stesse recensioni e procede solo dopo conferma esplicita

**CA-4 — Isolamento fra account**
- **Dato** due account con collegamenti Trustpilot
- **Quando** un utente di `A` chiede il dettaglio del collegamento di `B`
- **Allora** riceve `404`, anche forzando l'identificativo dell'account nella richiesta

**CA-5 — Revoca**
- **Dato** un collegamento attivo
- **Quando** il cliente lo revoca
- **Allora** la raccolta si ferma, il segreto viene cancellato e le recensioni già raccolte restano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla risoluzione del dominio in unità aziendale e di **integrazione** sulle rotte, con
      il fornitore **simulato**;
- [ ] prova di **isolamento fra account** sulla risorsa e sullo scambio di autorizzazione;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-RECENSIONI]` copre il collegamento a una sola piattaforma
      (Google, storia 0007); la seconda piattaforma è coperta a livello di integrazione. Voce nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con la motivazione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, **secondo fornitore esterno** aggiunto;
- [ ] **registro delle decisioni** compilato, con la nota sull'unità aziendale che non coincide con la sede;
- [ ] verificato che le credenziali non compaiano in nessuna risposta né in nessun registro.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | serve la sede |
| storia `0007` | riusa la macchina a stati, la schermata e la gestione dei segreti: farla dopo evita di scriverle due volte |

## 7. Fuori ambito

- l'invio degli inviti **attraverso** Trustpilot (Trustpilot ha un proprio meccanismo di inviti): l'app invia dal
  proprio canale, storia 0014. Se in futuro si volesse usare il loro, è una storia nuova;
- la raccolta vera delle recensioni — storia 0009;
- le recensioni di prodotto: RepGrove tratta recensioni dell'attività, non dei singoli prodotti.

## 8. Punti aperti

- **A quale piano Trustpilot corrisponde l'accesso necessario e quanto costa al cliente**: non l'ho trovato
  (descrizione §2.7). È una informazione da avere prima di promettere questa integrazione in fase di vendita.
- **Se convenga usare il meccanismo di inviti di Trustpilot** invece del nostro: il loro ha il vantaggio di essere
  «nativo» e conforme per costruzione, il nostro il vantaggio di funzionare anche su Google e di produrre un
  registro di equità unico (storia 0016). L'inclinazione è tenere il nostro come canale unico, ma è una decisione
  di prodotto.
</content>
