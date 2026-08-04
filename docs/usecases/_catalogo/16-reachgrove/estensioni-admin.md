# ReachGrove — estensioni della console di amministrazione

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

ReachGrove ha una caratteristica che nessun'altra app della suite ha: **una risorsa condivisa e deperibile fra
tutti gli account**, la reputazione dell'infrastruttura di invio. Un solo cliente che spedisce a una lista comprata
fa salire il tasso di segnalazione dell'indirizzo di invio e i messaggi di **tutti** cominciano a essere respinti
dai server riceventi ([application-description.md](application-description.md) §2.3 punto 5 e §11, rischi noti).
Chi amministra la piattaforma deve quindi poter vedere quel numero per account e **fermare un singolo cliente
prima che bruci il recapito degli altri**: non è un pannello di comodità, è il presidio che tiene in piedi il
canale primario. Servono inoltre due viste diagnostiche che rispondono alla domanda più frequente
dell'assistenza — «non parte niente» — e che riguardano lo stato dei domini mittenti e dei canali collegati dal
cliente col proprio contratto. Tutto il resto (abbonamenti, abilitazioni, fatturazione, diritti degli interessati)
si governa con le viste comuni.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `sending.suspended` | Interruttore che sospende **ogni** invio dell'account: campagne in corso in pausa, campagne programmate non partono, automazioni ferme | spento | amministratore di piattaforma | È la leva di emergenza sulla risorsa condivisa. Chi sta bruciando la reputazione di tutti è, quasi sempre, l'ultimo ad accorgersene: se la leva stesse nelle sue mani non verrebbe usata |
| `sending.complaint_threshold` | Soglia del tasso di segnalazione oltre la quale scatta il blocco automatico (storia 0021) | valore di piattaforma, proposta 0,3 % | amministratore di piattaforma | È il limite imposto dai fornitori di posta riceventi, non una preferenza del cliente. Renderlo configurabile dal cliente significherebbe consentirgli di spegnere l'allarme |
| `sending.rate_per_hour` | Ritmo massimo di consegna dell'account, usato per il riscaldamento di un dominio nuovo o per contenere un account sospetto | valore di piattaforma | amministratore di piattaforma | Il ritmo dipende dallo stato dell'infrastruttura condivisa, non dalla fretta del cliente |
| `public_forms.rate_limit_per_minute` | Quanti invii al minuto accetta un modulo pubblico di iscrizione dallo stesso indirizzo di rete (storia 0009) | valore di piattaforma | amministratore di piattaforma | È un presidio contro gli abusi sull'unica superficie raggiungibile senza autenticazione: chi subisce l'abuso, di solito, lo alzerebbe «per far passare tutto» |
| `public_forms.enabled_override` | Spegne **tutti** i moduli pubblici di un account | acceso | amministratore di piattaforma | Serve quando un modulo viene inondato di invii falsi o usato per contenuti illeciti |
| `import.max_rows` | Numero massimo di righe per singola importazione (storia 0010) | valore di piattaforma | amministratore di piattaforma | È un limite di risorse condivise; alzarlo per un cliente in migrazione è una scelta operativa |

## 3. Quote e deroghe

- **Metrica governata**: `messages_sent` (natura `flow`, a consumo su finestra mensile).
- **Serve una deroga manuale?** **Sì, ma per un caso solo**: il cliente che ha finito la quota a metà di una
  spedizione già partita e chiede di poterla concludere. Con una metrica a consumo, il rimedio ordinario — cambiare
  piano — funziona bene e va preferito quasi sempre; la deroga esiste per il caso in cui aspettare il rinnovo
  significherebbe consegnare metà lista oggi e metà fra tre settimane, cioè un danno per i destinatari e non solo
  per il cliente.
- **Forma della deroga**: **credito una tantum** di invii aggiuntivi sulla finestra corrente, con quantità e
  scadenza obbligatorie. Non un tetto alternativo permanente: alla chiusura della finestra il credito residuo
  scade e non si accumula.
- **Tracciamento**: chi l'ha concessa, quando, quanti invii, fino a quando, e il motivo scritto.
- **Limiti che non si negoziano**: una deroga **non è uno sconto** e non cambia l'abbonamento; se il cliente ha
  bisogno stabilmente di più invii, passa di piano. E soprattutto: **una deroga non scavalca mai un presidio**.
  Non esiste deroga che sblocchi un dominio mittente non verificato, una lista in quarantena, un recapito soppresso
  o un account bloccato per tasso di segnalazione: quelle non sono quote, sono le ragioni per cui l'app esiste.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Salute dell'invio per account** | Per ogni account: invii dell'ultima settimana, consegnati, rimbalzi permanenti e temporanei, **tasso di segnalazione** confrontato con la soglia, disiscrizioni; ordinabile per rischio | «Chi sta rovinando la reputazione di tutti?» — è la vista che giustifica questo documento | Conteggi e percentuali. **Nessun recapito**, nessun contenuto di messaggio, nessun nome |
| **Reputazione complessiva dell'infrastruttura** | Andamento aggregato di tutti gli account, per dominio d'invio e per fornitore ricevente principale | «Il problema è di un cliente o è nostro?» | Metriche aggregate |
| **Domini mittenti** | Per ogni account: domini dichiarati, esito della verifica dell'autenticazione del mittente (i tre controlli SPF, DKIM e DMARC), momento dell'ultimo controllo, motivo dell'eventuale fallimento | «Perché al cliente non parte niente?» — è la causa numero uno | Nomi di dominio (dato dell'azienda, non di una persona), stati e codici di errore |
| **Canali collegati** | Per ogni account: canali aggiuntivi attivati col contratto del cliente (messaggi brevi, messaggistica), stato della connessione, esito dell'ultima chiamata, ultimo errore riportato dal fornitore | «Perché non partono i messaggi brevi?» | Stato, momento, codice di errore. **Mai** le credenziali del cliente, che restano cifrate e non sono leggibili nemmeno da qui |
| **Code di spedizione** | Arretrato per account, campagne in corso, ripetizioni, errori ricorrenti | «C'è un accumulo?» | Conteggi |
| **Moduli pubblici di iscrizione** | Invii ricevuti nelle ultime 24 ore e negli ultimi 7 giorni, respinti per limite di frequenza, respinti come automatici | «Questo account sta subendo un abuso?» | Conteggi. Nessun contenuto degli invii, nessun indirizzo di rete completo |
| **Importazioni** | Ultime importazioni per account: momento, righe totali, create in quarantena, scartate, motivo prevalente | «Perché il cliente dice che l'importazione non ha funzionato?» | Conteggi e **codici** di motivo, mai le righe |

**Divieto di impersonificazione.** Nessuna di queste viste mostra i contenuti dell'account: non si vedono iscritti,
recapiti, testi delle campagne né prove di consenso. È un punto particolarmente sensibile in questa app, perché
l'archivio di un cliente è fatto di persone che non hanno alcun rapporto con noi. Se in assistenza serve capire
*cosa* c'è dentro un dato, la strada è chiedere al cliente o guidarlo, mai guardare al posto suo.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Sospendere l'invio di un account** | Tasso di segnalazione oltre soglia, segnalazione da un fornitore ricevente, sospetto di lista comprata | sì (si riattiva) | operatore, momento, motivo scritto, stato precedente | Il cliente si ferma senza capire: l'azione deve far partire una comunicazione verso di lui. Non farla è però il rischio maggiore, perché mette in pericolo tutti gli altri account |
| **Riattivare l'invio dopo una sospensione** | Il cliente ha ripulito la lista e il tasso è rientrato | sì | operatore, motivo, valori del tasso al momento della riattivazione | Riattivare troppo presto: la decisione va presa sui numeri, non sull'insistenza |
| **Spegnere i moduli pubblici di un account** | Modulo inondato di invii falsi o usato per contenuti illeciti | sì | operatore, momento, motivo | Il cliente smette di ricevere iscrizioni vere: va avvisato |
| **Concedere un credito di invii** | Spedizione da concludere prima del rinnovo della finestra | sì (scade da sola) | operatore, quantità, scadenza, motivo | Diventare la via ordinaria per non far passare di piano |
| **Ripetere una consegna fallita per un guasto nostro** | Un lotto di consegne è rimasto fermo per un'anomalia dell'infrastruttura | sì, con attenzione | operatore, motivo, identificativo del lotto | **Doppia consegna**: l'operazione dev'essere idempotente per destinatario (storia 0019), altrimenti la stessa persona riceve due volte lo stesso messaggio |
| **Forzare un nuovo controllo dei domini mittenti** | Il cliente dice di aver pubblicato i record e la verifica non si aggiorna | sì | operatore, dominio, esito | Nessuno rilevante |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni **irreversibili** o con effetti verso
l'esterno richiedono una conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai contenuti
dell'account. In questa app vale inoltre un divieto specifico, che discende dal dominio:

> **Nessuna azione della console può rendere contattabile qualcuno.** Non esiste, e non deve esistere, un modo per
> togliere un iscritto dalla quarantena, rimuovere un recapito dall'elenco di soppressione, saltare il controllo
> pre-volo o inviare per conto del cliente. Un amministratore di piattaforma che potesse farlo sarebbe una
> scorciatoia intorno all'unico presidio che il prodotto vende.

Della soppressione vale la pena dire il rovescio: la console **può aggiungere** un recapito all'elenco di
soppressione di un account, per esempio quando una persona scrive direttamente a noi chiedendo di non essere più
contattata da un cliente e il cliente non risponde. È un'azione a senso unico, tracciata, e va nella direzione
prudente.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Invii, consegne, rimbalzi, segnalazioni e disiscrizioni per account | metrica | no | È la diagnosi della risorsa condivisa: senza, la sospensione sarebbe arbitraria |
| Tasso di segnalazione per account e per dominio d'invio | metrica | no | Il numero che decide una sospensione |
| Numero di iscritti, di iscritti in quarantena e di recapiti soppressi per account | metrica | no | Distinguere l'account sano da quello che ha caricato una lista comprata |
| Domini mittenti e loro stato di autenticazione | metadato | no (dato dell'azienda cliente) | Prima causa di «non parte niente» |
| Stato dei canali collegati e ultimo errore del fornitore | metadato | no | Seconda causa di «non parte niente» |
| Esiti delle importazioni con codici di scarto | metrica + codici | no | Assistenza sul primo giorno del cliente |
| Registro delle azioni amministrative: chi, quando, su quale account, con quale motivo | metadato | l'identificativo dell'amministratore di piattaforma | Rendicontazione delle sospensioni e delle deroghe |
| Registro delle esportazioni dei rapporti (storia 0033) | metadato | l'identificativo dell'utente del cliente che ha esportato, non il suo nome | Ricostruire un'uscita di dati se il cliente la segnala |

**Verifica obbligatoria.** Nessuna riga di questa tabella espone un recapito, un nome di iscritto, il testo di una
campagna o una prova di consenso: sono i dati che rendono l'app quello che è, e l'accesso amministrativo non ne ha
bisogno per fare il proprio lavoro. Le uniche due righe che toccano persone riguardano **identificativi di
operatori** — l'amministratore di piattaforma e l'utente del cliente che ha esportato — e vanno dichiarate nel
manifesto dati dell'app: l'accesso amministrativo è un trattamento come gli altri.

Un'aggiunta che **non** è nella tabella e che va detta a chiare lettere: se un giorno servisse mostrare in console
il recapito che ha generato una segnalazione — richiesta plausibile, perché un fornitore ricevente ci segnala un
indirizzo — la risposta di questo documento è **no**. Il fornitore va gestito sull'identificativo del messaggio, e
l'account viene informato tramite la propria interfaccia. Se emergesse che non si può fare altrimenti, è un punto
aperto (§7), non una deroga da prendersi.

## 7. Punti aperti

- **Chi avvisa il cliente quando l'invio gli viene sospeso d'ufficio**, e con quale testo. L'azione è nostra e
  l'effetto lo subisce lui, nel mezzo di una campagna: serve un percorso di comunicazione, che è di piattaforma
  (assistenza) e non di questa app. Chiude lo sviluppatore.
- **Soglia di intervento manuale rispetto al blocco automatico.** Il blocco automatico scatta sopra lo 0,3 %
  (storia 0021); non so dire a quale valore un essere umano dovrebbe guardare prima, perché non ho dati reali di
  esercizio. Proposta: partire osservando gli scostamenti dal comportamento abituale dell'account e fissare le
  soglie dopo qualche mese di esercizio. Chiude lo sviluppatore.
- **Quantità massima e frequenza dei crediti di invii.** Proposta: non più di un credito per finestra e non oltre
  il 20 % del tetto del piano. È una decisione commerciale, non tecnica.
- **Se la console debba vedere lo stato di verifica di un dominio mittente condiviso fra più account.** Oggi ogni
  account porta i propri domini; se in futuro esistesse un dominio d'invio comune, servirebbe una vista di
  piattaforma diversa. Dipende dalla scelta del fornitore di consegna, che è punto aperto §11.2 della descrizione.
