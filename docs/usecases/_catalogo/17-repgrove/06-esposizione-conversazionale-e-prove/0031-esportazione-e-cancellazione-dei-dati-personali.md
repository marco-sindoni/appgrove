# 0031 — Esportazione e cancellazione dei dati personali

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: tutte le storie che introducono una tabella (`0002`, `0006`, `0007`, `0009`, `0011`, `0012`, `0013`, `0014`, `0018`, `0021`, `0022`, `0024`, `0025`, `0026`, `0028`, `0029`)
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile della conformità della piattaforma
> voglio che l'esportazione e la cancellazione dei dati di RepGrove coprano **tutte** le tabelle, senza dimenticarne
> nessuna, e che la risposta a chi chiede la cancellazione sia vera fino in fondo
> così da non scoprire mesi dopo che una tabella era rimasta fuori, o che abbiamo detto a una persona di aver
> cancellato una cosa che è ancora pubblica altrove.

**Contesto.** Ogni storia precedente ha fatto la sua parte: ha aggiunto le proprie voci al manifesto e le proprie
tabelle al contratto dati. Questa storia chiude il cerchio e verifica che il conto torni, perché il difetto di
conformità più probabile in un'app nuova è **dimenticare una tabella**. Sta in coda all'epica, dopo il percorso
end-to-end, proprio perché è una **verifica finale**: ha bisogno che tutte le tabelle esistano, e non è un
prerequisito di nessun'altra storia.

Due cose la rendono più difficile che nelle altre app della suite. La prima: i dati riguardano persone che **non
sono nostri utenti**. Il cliente finale invitato non ha scelto di stare nel nostro sistema; l'autore di una
recensione, ancora meno — non ha mai avuto rapporti né con noi né, necessariamente, con l'app che lo ha importato.
La seconda: **la nostra copia non è l'originale**. La recensione vive sulla piattaforma d'origine, e noi possiamo
cancellare solo la copia. Dirlo o non dirlo è la differenza fra una risposta corretta e una risposta fuorviante
(descrizione §6).

Sopra tutto questo pende la decisione dell'articolo 9 (descrizione §6 e §11.7): se il testo delle recensioni resta
in casa, questa storia gestisce dati che possono riguardare la salute di persone che non sappiamo nemmeno chi
siano.

## 2. Requisiti funzionali

1. **RF-1** — Il contratto `RecensioniDataContract` implementa `appId()`, `manifest()`, `exportData(ambito)` e
   `purgeData(ambito)` coprendo **tutte** le tabelle dello schema `app_recensioni` che contengono dati riferibili a
   una persona: `servizio_erogato`, `richiesta_recensione`, `recensione`, `risposta`, `segnalazione`,
   `bozza_operazione`.
2. **RF-2** — Ogni altra tabella è **esclusa con motivo scritto**: `sede`, `regola_di_equita`,
   `modello_di_messaggio`, `punteggio_reputazione`, `riquadro_pubblico`, `dichiarazione_trasparenza`,
   `rapporto_periodico`, `rifiuto_pratica` (dati dell'azienda o aggregati) e `collegamento_piattaforma`, che
   contiene **credenziali di delega** e non deve comparire in nessuna esportazione, per nessun motivo.
3. **RF-3** — L'esportazione produce un insieme leggibile e completo, organizzato per entità, con i riferimenti fra
   entità comprensibili senza conoscere lo schema, e comprende **il testo delle recensioni e delle risposte** che
   riguardano la persona.
4. **RF-4** — La cancellazione è **fisica**: le righe spariscono. Sostituire il nome con un codice non è cancellare
   e non è ammesso come esito. La cancellazione lascia una riga di prova nel registro delle purghe — cosa, quando,
   per quale richiesta — senza contenere i dati cancellati.
5. **RF-5** — Alla cancellazione dei dati di un autore di recensione, la risposta comprende **l'avvertenza che
   l'originale resta sulla piattaforma d'origine** e va richiesto a chi la ospita, con l'indicazione di come farlo.
   Cancellare la copia e tacere sull'originale sarebbe fuorviante.
6. **RF-6** — La **prova di equità** sopravvive alla cancellazione in forma **aggregata**: quanti clienti serviti,
   quanti invitati, quanti esclusi e per quale motivo, per sede e per periodo — numeri che non contengono persone.
   Perdere la prova a ogni cancellazione renderebbe il registro della storia 0016 inservibile proprio quando serve.
7. **RF-7** — Un controllo automatico confronta l'elenco delle tabelle dello schema con quelle classificate dal
   contratto e **fa fallire la compilazione** se una tabella nuova non è né coperta né esclusa con motivo.
8. **RF-8** — Esportazione e cancellazione restano accessibili anche quando l'app è disabilitata o l'abbonamento è
   `canceled`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Esportazione e cancellazione operano su un ambito che comprende sempre
  l'account; nessuna operazione attraversa due account, nemmeno per un cliente finale che compare in entrambi —
  caso tutt'altro che teorico, perché la stessa persona può essere cliente di due attività che usano RepGrove.
- **RT-2 — Interfaccia di programmazione (§2).** L'app non espone rotte proprie per i diritti dell'interessato:
  risponde al contratto che la piattaforma invoca. Errori in `application/problem+json`.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. La cancellazione fisica rispetta l'ordine delle dipendenze
  logiche (prima `bozza_operazione` e `richiesta_recensione`, poi `servizio_erogato`) e resta **una operazione
  unica**: una purga interrotta a metà è peggio di una purga non fatta. Le tabelle **ad accrescimento**
  (`richiesta_recensione`, `regola_di_equita`) fanno eccezione al divieto di modifica solo per la purga, e l'eccezione
  va scritta nel codice come tale, non aggirata.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova: i diritti dell'interessato si esercitano dalla
  sezione «I miei dati» della piattaforma. Il modulo aggiunge soltanto, nella scheda di un servizio erogato e in
  quella di una recensione, l'indicazione di quali dati la riguardano e per quanto si conservano.
- **RT-5 — Cinque lingue (§4).** Le poche stringhe aggiunte passano dallo spazio-nomi `recensioni` e sono presenti
  in `en, it, fr, es, de`. Il **manifesto** invece vuole due sole lingue, italiano e inglese: sono due elenchi
  diversi e non vanno confusi.
- **RT-6 — Varchi e quota (§6, §7).** I diritti dell'interessato non consumano quota e **non** passano dal varco
  dell'abbonamento: restano accessibili con `canceled` e con app disabilitata.
- **RT-7 — Esposizione conversazionale (§12).** **Nessuno strumento.** Esportare o cancellare dati personali su
  richiesta di un assistente non è un vantaggio, è un rischio: è un'operazione irreversibile su dati di terzi, e la
  richiesta arriva comunque da un canale di piattaforma. Esclusione deliberata, annotata nel contratto degli
  strumenti (storia 0027).
- **RT-8 — Dati personali (§10).** È l'oggetto della storia. Le voci del manifesto vengono rilette **integralmente**
  in italiano e inglese contro le tabelle esistenti, comprese quelle nate dopo la prima stesura
  (`bozza_operazione`). I termini di conservazione proposti dalla descrizione §6 — 24 mesi per i dati del cliente
  servito, 36 mesi per il recapito usato come prova, 5 anni per l'identità del segnalante — diventano qui
  cancellazioni programmate, non buoni propositi.
- **RT-9 — Registrazione eventi (§14).** `esportazione eseguita` e `purga eseguita` con `tenant_id`, `app_id`,
  ambito, numero di righe per entità e identificativo di correlazione, **senza** i dati trattati.

## 4. Criteri di accettazione

**CA-1 — Nessuna tabella dimenticata**
- **Dato** lo schema `app_recensioni` con tutte le sue tabelle
- **Quando** il controllo automatico confronta schema e contratto
- **Allora** ogni tabella risulta coperta o esclusa con motivo; aggiungerne una nuova senza classificarla fa fallire
  la compilazione

**CA-2 — Esportazione completa**
- **Dato** un account con dati in tutte le entità e una persona presente come cliente servito, come invitata e come
  autrice di una recensione
- **Quando** si esegue l'esportazione per quella persona
- **Allora** l'insieme prodotto contiene le sue righe in tutte e tre le entità, compresi il testo della recensione e
  la risposta pubblicata, e **non** contiene alcuna credenziale di collegamento

**CA-3 — Cancellazione fisica**
- **Dato** una purga eseguita sull'ambito di un cliente finale
- **Quando** si interroga direttamente il database
- **Allora** le righe non esistono più — non sono presenti con i campi sostituiti da codici

**CA-4 — L'originale resta, e lo si dice**
- **Dato** una richiesta di cancellazione da parte dell'autore di una recensione
- **Quando** la purga viene eseguita
- **Allora** la copia sparisce e la risposta comprende l'avvertenza che la recensione resta sulla piattaforma
  d'origine, con l'indicazione di come chiederne la rimozione a chi la ospita

**CA-5 — La prova di equità sopravvive in aggregato**
- **Dato** un periodo con dieci clienti serviti, e la cancellazione dei dati di due di loro
- **Quando** si esporta il registro di equità di quel periodo
- **Allora** i conteggi restano dieci serviti, otto invitati, due esclusi con i motivi, e nessun nome compare

**CA-6 — Accessibile senza abbonamento**
- **Dato** un account con abbonamento `canceled` · **Quando** si esercita l'esportazione · **Allora** funziona

**CA-7 — Isolamento fra account**
- **Dato** la stessa persona presente come cliente in due account
- **Quando** si purga l'ambito di uno
- **Allora** i dati dell'altro restano intatti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e compliance);
- [ ] prove di **unità** sul controllo di copertura schema↔contratto e sul calcolo aggregato che sopravvive alla
      purga; di **integrazione** su esportazione e purga con database effimero popolato in **tutte** le entità;
- [ ] prova di **isolamento fra account** sulla purga, con la stessa persona presente in due account;
- [ ] **prova end-to-end**: *nessun impatto sul percorso applicativo* — i diritti dell'interessato hanno il proprio
      percorso di piattaforma; qui si verifica con prove di integrazione, e la voce corrispondente del registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) lo dichiara;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per le poche stringhe aggiunte;
- [ ] **manifesto dei dati** riletto **integralmente** in italiano e inglese contro le tabelle esistenti: è la
      verifica finale, non una formalità;
- [ ] **registro delle decisioni** compilato, in particolare sulla forma dell'avvertenza sull'originale e sulla
      sopravvivenza aggregata della prova di equità;
- [ ] contratto degli **strumenti conversazionali**: esclusione deliberata, annotata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| tutte le storie che introducono una tabella | non si può verificare la copertura finché le tabelle non ci sono tutte |
| storia `0016` | il registro di equità è la funzione che deve sopravvivere alla cancellazione, in forma aggregata |
| decisione sull'articolo 9 (descrizione §6) | se il testo delle recensioni non si conserva, cambia cosa c'è da esportare e da cancellare |

## 7. Fuori ambito

- l'interfaccia con cui l'interessato presenta la richiesta: è di piattaforma;
- la cancellazione dei dati alla chiusura dell'account: è il percorso di piattaforma, che invoca lo stesso contratto;
- la conservazione delle copie di sicurezza: è di piattaforma e ha regole proprie;
- la rimozione della recensione **dalla piattaforma d'origine**: non è in nostro potere — l'unica strada offerta al
  cliente è la segnalazione motivata della storia 0021, che è cosa diversa da un diritto dell'interessato.

## 8. Punti aperti

🛑 **Chi risponde alla richiesta dell'autore di una recensione.** La persona che ha scritto la recensione non è
nostra utente né utente del cliente: la sua richiesta può arrivare a noi, al nostro cliente o alla piattaforma
d'origine, e i tre non hanno lo stesso ruolo né gli stessi obblighi. Qui si implementa la parte che ci compete —
cancellare la nostra copia e dire la verità sull'originale — ma **chi è titolare del trattamento della copia** è
una domanda che **decide lo sviluppatore** con la revisione legale pre-go-live. È strettamente legata al punto
aperto n. 1 della descrizione §11 (base giuridica dell'invito) e all'avviso sull'articolo 9.

- **Conflitto fra cancellazione e prova.** Il cliente ha un interesse a conservare la prova di aver invitato
  correttamente; l'invitato ha diritto alla cancellazione. La proposta — cancellare i dati personali e conservare
  l'aggregato — è ragionevole ma va confermata: se un domani la prova dovesse essere nominativa per essere
  opponibile, questa scelta andrebbe rivista.
- **La conservazione delle recensioni** dipende dalle condizioni delle piattaforme, non verificate (descrizione
  §11.2): se il testo non si potesse conservare, parte di questa storia si semplifica e parte diventa inutile.
