# Change 0083: Riconciliazione netto/revenue — lordo → commissioni → netto → accredito

**Branch**: `change/0083-use-case-0071-riconciliazione-netto-revenue`
**Aree**: `services/core`, `frontend` (apps/admin, packages/i18n, packages/api-client), `docs` (manifesto dati, registro copertura)
**Data**: 2026-08-02
**Autore**: Platform Engineering (modalità fast)
**Use case sorgente**: [docs/usecases/13-abbonamenti-self-service/0071-riconciliazione-netto-revenue.md](../../docs/usecases/13-abbonamenti-self-service/0071-riconciliazione-netto-revenue.md)
**Tocca dati personali?**: Sì, in senso stretto — due colonne nuove (commissione e netto per transazione) della
stessa categoria dell'importo già trattato: dati di fatturazione riferiti al conto, base giuridica «esecuzione
del contratto», stessa conservazione. Classificazione **MINOR**: nessuna nuova finalità, nessun nuovo
destinatario, nessun aggiornamento di versione di informativa o condizioni. Le tabelle degli accrediti sono
dati economici della piattaforma, non riferiti ad alcun conto.

## Problema / Obiettivo

Il fornitore di pagamento è **venditore ufficiale verso il cliente**: incassa lui, trattiene le proprie
commissioni (una percentuale più una quota fissa per transazione) e accredita a noi il **netto**, con accrediti
periodici che non coincidono con le singole vendite. Oggi la piattaforma registra solo il **lordo**: la pagina
di fatturazione del cliente e la console admin mostrano quanto è stato addebitato, e nessuno sa quanto è
davvero entrato. La differenza non è una percentuale fissa — dipende dal mix di transazioni, perché la quota
fissa pesa moltissimo sui piccoli importi — quindi non è nemmeno stimabile a occhio.

Manca inoltre qualunque forma di **quadratura**: se un accredito non arriva, o arriva di un importo diverso
dalla somma dei netti che dovrebbe contenere, non c'è nulla che lo faccia notare.

Obiettivo: rendere osservabile il netto reale. Registrare, per ogni transazione, quanto è stato trattenuto e
quanto resta; acquisire gli accrediti e ricondurli alle transazioni che li compongono; esporre nella console
admin la catena **lordo → commissioni → netto → accredito** per periodo, con la quadratura di ogni accredito e
la segnalazione degli scostamenti; misurare il tutto per l'osservabilità, incluso l'accredito atteso che non
arriva.

Non è un blocco né un gate: è osservabilità gestionale. Non è contabilità fiscale — quella resta del
commercialista.

## Scope

### `services/core` — acquisizione e riconciliazione

1. **Commissioni e netto per transazione.** La riga di storico di ogni transazione acquisisce tre attributi:
   la commissione trattenuta, il netto residuo e la **provenienza** del dato (dichiarato dal fornitore oppure
   stimato da noi). Se il payload della transazione porta i valori, vincono sempre; se non li porta, il
   backend li stima con la formula del listino (percentuale più quota fissa, entrambe configurabili) e la riga
   resta marcata come stimata. La provenienza è visibile nella vista: una stima non dev'essere mai scambiata
   per un dato del fornitore.
2. **Rimborsi e contestazioni.** Una transazione contestata o rimborsata non genera una riga nuova: cambia
   stato la riga esistente (il riferimento presso il fornitore è la chiave di idempotenza). Il suo importo
   esce dall'incassato ed entra fra gli **storni**; il suo netto va a zero. Si aggiunge lo stato «rimborsata»
   accanto a «contestata», per distinguere il rimborso volontario dalla contestazione del cliente.
3. **Accrediti.** Nuove entità di piattaforma: l'accredito (riferimento presso il fornitore, importo, valuta,
   data di accredito) e le sue **righe di dettaglio**, una per transazione accreditata, ciascuna con il netto
   accreditato **al momento dell'accredito**. Il denaro restituito da un rimborso o da una contestazione
   compare come riga di dettaglio con importo negativo in un accredito successivo.
4. **Acquisizione.** Gli eventi di accredito passano dalla stessa pipeline webhook degli altri (firma
   verificata, coda, deduplicazione sull'identificativo dell'evento, guardia contro gli eventi vecchi,
   riprova e coda di scarto), nella **stessa transazione** dell'effetto: o si registrano accredito e dettaglio
   o non si registra nulla.
5. **Vista di riconciliazione** per il solo amministratore di piattaforma, che espone:
   - i **totali** della finestra osservata: lordo incassato, commissioni, netto, storni, quanto è già stato
     accreditato e quanto è **ancora da accreditare**;
   - una riga **per mese** (mese dell'addebito, mai dell'accredito): lordo, commissioni, netto, storni,
     numero di transazioni, peso delle commissioni sul lordo e segnalazione quando supera la soglia di
     attenzione;
   - l'**elenco degli accrediti**: importo accreditato, somma dei netti collegati, scostamento, numero di
     transazioni, intervallo di addebiti coperto e **esito della quadratura** (quadrato, scostamento, oppure
     non quadrabile perché le valute in gioco differiscono).
6. **Osservabilità.** Una sorveglianza periodica pubblica le misure del netto, del peso delle commissioni e
   del non ancora accreditato, e segnala — con misura e log di avvertimento — l'**accredito atteso che non
   arriva**: netto non accreditato più vecchio di una soglia configurabile. Nessuna azione correttiva
   automatica.
7. **Stub locale.** Due scenari nuovi del simulatore: **accredito** (raccoglie il netto non ancora accreditato
   del conto e dell'app e lo accredita) e **rimborso** (rimborso totale di una transazione, seguito
   dall'accredito negativo). Servono a rendere la vista verificabile in locale, dove non esiste alcun account
   reale del fornitore. Le transazioni simulate portano commissioni dichiarate, così il percorso «dato del
   fornitore» è esercitato tanto quanto quello stimato.
8. Contratto pubblicato: descrizione delle interfacce rigenerata e tipi del client frontend riallineati.

### `frontend/apps/admin` — la pagina «Riconciliazione»

9. Nuova pagina nel gruppo **Revenue** del menu laterale, accanto a Fatturazione, con la sua voce di
   navigazione e la sua etichetta nella barra superiore.
10. **Riquadri dei totali** (lordo, commissioni, netto, da accreditare) con gli importi formattati nella
    valuta prevalente; **avviso** quando il peso delle commissioni supera la soglia, e quando un accredito
    atteso è in ritardo.
11. **Tabella per mese** con la catena lordo → commissioni → netto, gli storni, il numero di transazioni e il
    peso delle commissioni evidenziato quando supera la soglia.
12. **Tabella degli accrediti** con la quadratura: importo accreditato, somma dei netti, scostamento evidente
    quando c'è, esito della quadratura come contrassegno colorato, e l'indicazione di quante righe hanno le
    commissioni **stimate** anziché dichiarate.
13. Stati di caricamento, errore con riprova e stato vuoto coerenti col resto della console.
14. Tutte le nuove diciture nei **5 cataloghi di lingua** (`en`, `it`, `fr`, `es`, `de`).

### Compliance e registri

15. Le due colonne nuove entrano nel **manifesto dei dati personali** della piattaforma e nell'**esportazione**
    prevista dai diritti dell'interessato, con la stessa finalità e conservazione dell'importo.
16. **Registro di copertura end-to-end**: lo use case 0071 esce dalle esenzioni «non implementato» ed entra
    fra quelli con superficie applicativa, collegato al percorso nuovo che lo copre.

## Fuori scope

- **Ricavo ricorrente mensile e tasso di abbandono** accanto al netto: appartengono a UC 0021 e sono già
  annotati in `docs/_BACKLOG.md`. La pagina è collocata dove atterreranno, ma non li anticipa.
- **Implementazione reale del fornitore**: `PaddlePaymentProvider` resta non implementato finché non esiste
  l'account (bloccato da #14). Vale lo stub locale. Se il dettaglio delle commissioni non dovesse arrivare col
  webhook reale servirà una lettura periodica dall'interfaccia del fornitore: rimando già tracciato nello use
  case, che solo i dati reali possono sciogliere.
- **Rimborsi parziali**: il modello rappresenta il rimborso totale (cambio di stato della transazione). Il
  parziale richiede una riga di rettifica separata; rimando tracciato nello use case 0071.
- **Tasso di cambio applicato dal fornitore**: quando le valute differiscono la quadratura è dichiarata non
  calcolabile invece di sommare valute diverse. Come registrare il cambio resta il punto aperto dello use
  case, che dipende dal formato reale dei dati.
- **Contabilità e adempimenti fiscali**: competenza del commercialista (`docs/_COMMERCIALISTA.md`). Questa è
  osservabilità gestionale e non va confusa con quella.
- **Vista per singolo cliente del proprio netto**: il netto è un dato economico della piattaforma, non del
  cliente; la pagina di fatturazione del cliente non cambia.
- **Allarmi infrastrutturali** sulle nuove misure (soglie in Terraform): l'esposizione delle misure è qui, la
  loro configurazione come allarme è del modulo infrastruttura ed è rimandata all'accensione reale.

## Criteri di accettazione

- [ ] Ogni transazione registrata porta commissione, netto e provenienza del dato; quando il payload porta i
      valori del fornitore quelli vincono, altrimenti la riga è stimata con la formula configurata e marcata
      come tale.
- [ ] Un evento di accredito registra l'accredito e le sue righe di dettaglio nella stessa transazione, è
      idempotente sulla ri-consegna dello stesso evento e non viene applicato se più vecchio dello stato già
      registrato.
- [ ] La vista amministrativa espone totali, righe per mese (attribuite al mese dell'addebito) ed elenco degli
      accrediti con scostamento ed esito della quadratura; un accredito le cui righe non sommano all'importo
      accreditato risulta in scostamento; un accredito con valute diverse risulta non quadrabile e non mostra
      uno scostamento inventato.
- [ ] Una transazione rimborsata o contestata esce dall'incassato, entra fra gli storni e non rompe la
      quadratura degli accrediti già registrati; il rimborso compare come riga negativa nell'accredito
      successivo.
- [ ] Un accredito che copre addebiti di due mesi resta un solo accredito e le sue transazioni restano
      attribuite ai rispettivi mesi.
- [ ] La vista è accessibile solo all'amministratore di piattaforma: un utente senza quel ruolo riceve un
      rifiuto e nessun dato economico globale.
- [ ] Le misure di riconciliazione sono pubblicate e l'accredito atteso in ritardo produce misura e log di
      avvertimento.
- [ ] La pagina «Riconciliazione» è raggiungibile dal menu, mostra totali, tabella per mese e accrediti con la
      quadratura, e gestisce caricamento, errore con riprova e stato vuoto.
- [ ] Tutte le nuove diciture esistono nelle 5 lingue e la verifica di parità dei cataloghi è verde.
- [ ] `./run-tests.sh` completa è verde; il registro di copertura end-to-end non ha più lo use case 0071 fra
      le esenzioni ed è coerente.

## Invarianti appgrove toccati

- **Tenant dal token verificato**: la vista di riconciliazione è cross-tenant e amministrativa, protetta dal
  ruolo di piattaforma; non riceve né accetta alcun tenant come parametro di richiesta. Il tenant delle
  transazioni continua a venire dai dati personalizzati del payload **firmato** del webhook, mai da input del
  client.
- **Filtro per riga sul tenant**: le letture amministrative restano l'eccezione già documentata (query native
  cross-tenant gated dal ruolo di piattaforma, UC 0021); nessuna lettura tenant-scoped perde il proprio
  filtro, e le nuove entità di accredito non sono riferite ad alcun conto.
- **Modulo Terraform `microsaas_app`**: non toccato (nessuna infrastruttura in questa change).
- **Log strutturati**: la scrittura degli accrediti passa dallo stesso contesto di log del consumer webhook
  (conto, applicazione, identità di sistema); la sorveglianza periodica logga con il contesto di piattaforma.

## Requisiti di test

- **Integrazione (`services/core`)**: dato un insieme di transazioni con commissioni note, il netto derivato
  è corretto e la provenienza è «fornitore»; senza commissioni nel payload il netto è stimato e la provenienza
  è «stimata». Un accredito simulato quadra con la somma dei netti collegati; un accredito di importo diverso
  produce uno scostamento; un accredito con valute miste è dichiarato non quadrabile.
- **Edge**: rimborso e contestazione riducono l'incassato, non rompono la quadratura degli accrediti già
  registrati e compaiono come riga negativa nell'accredito successivo; un accredito a cavallo di due mesi
  lascia le transazioni attribuite ai rispettivi mesi; la ri-consegna dello stesso evento di accredito è un
  non-effetto; un evento di accredito più vecchio dello stato registrato non lo sovrascrive.
- **Osservabilità**: la sorveglianza riconosce il netto non accreditato oltre soglia come accredito in ritardo
  e resta silente sotto soglia.
- **Sicurezza**: la vista risponde solo all'amministratore di piattaforma; un utente con ruoli ordinari riceve
  un rifiuto.
- **Componente (`frontend`)**: totali, tabella per mese, tabella degli accrediti con i tre esiti di
  quadratura, avviso di commissioni sopra soglia, avviso di accredito in ritardo, stati di caricamento/errore
  con riprova/vuoto, accessibilità della pagina.
- **End-to-end livello 2 (`L2-ADMIN-RECON`)**: dal menu alla pagina, totali visibili, tabella per mese,
  accrediti con quadratura e scostamento.
- **Verde prima del commit**: `./run-tests.sh` senza parametri (modalità fast).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — colonne nuove facoltative, nuovo stato di transazione additivo, nessuna interfaccia esistente modificata |
| Contratto cross-area | Sì — frontend ↔ `services/core` (nuova lettura amministrativa): descrizione delle interfacce e tipi del client rigenerati nello stesso commit |
| Version bump | minor |
