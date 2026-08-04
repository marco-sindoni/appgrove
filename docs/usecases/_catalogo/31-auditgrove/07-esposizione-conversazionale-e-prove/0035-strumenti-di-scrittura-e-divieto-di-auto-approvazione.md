# 0035 — Strumenti di scrittura e divieto di auto-approvazione

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0035` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0034`, `0019`, `0021`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde di ciò che fanno gli agenti della propria azienda
> voglio poter dire al mio assistente «ferma tutto quello che sta aspettando sullo strumento di cancellazione» e
> vedermi proporre una bozza da confermare io
> così da poter agire in fretta senza che la fretta diventi il modo con cui perdo il controllo.

**Contesto.** È la storia in cui questa applicazione deve resistere alla tentazione più naturale del catalogo:
esporre l'approvazione come strumento. Sarebbe comodo — «assistente, approva» — e distruggerebbe il prodotto.
Il senso dell'approvazione umana è che **una persona** si assuma la responsabilità di un'azione irreversibile: se
l'approvazione si ottiene chiedendola a un assistente, la catena di responsabilità è finita. Nel caso peggiore —
un assistente dirottato da istruzioni malevole, rischio previsto da
[UC 0065](../../../12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md) — esporre l'approvazione
significherebbe consegnare la chiave proprio a chi si voleva sorvegliare.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara tre strumenti di **scrittura**, tutti con **bozza e conferma umana esplicita**:
   `proponi_regola(strumento, decisione, motivo)`, `prepara_esportazione(periodo, formato)`,
   `nega_azione(id_nulla_osta, motivo)`.
2. **RF-2** — Nessuno dei tre produce l'effetto al momento dell'invocazione: producono una **bozza** con un
   identificativo e una scadenza, e l'effetto avviene solo dopo la conferma di una persona.
3. **RF-3** — **Non esiste, e non deve esistere, uno strumento che approva.** L'assenza è dichiarata nel
   contratto come scelta esplicita, con la motivazione, così che nessuno la scambi per una funzione mancante da
   aggiungere.
4. **RF-4** — Il rifiuto (`nega_azione`) è esposto perché va nella direzione sicura ed è sempre rimediabile da una
   persona; resta comunque con conferma umana obbligatoria.
5. **RF-5** — La conferma di una bozza avviene nell'interfaccia dell'applicazione, con l'indicazione di che cosa
   si sta per fare e da quale richiesta conversazionale nasce.
6. **RF-6** — Ogni bozza, conferma, rifiuto e scadenza di bozza è **una riga del registro**, con l'indicazione che
   la richiesta è nata dal canale conversazionale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni bozza appartiene all'account ricavato dal token verificato; la
  conferma è possibile solo da un utente dello stesso account. Un identificativo di bozza di un altro account non
  esiste, per chi lo chiede.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/agentaudit/v1/drafts` e
  `POST /api/agentaudit/v1/drafts/{id}/confirm`; corpo validato; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_agentaudit`: tabella delle bozze con `tenant_id`,
  chiave primaria UUID versione 7, colonne di controllo, cancellazione logica, scadenza, stato, origine della
  richiesta.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Da confermare» nel modulo `agentaudit`: elenco delle bozze in
  attesa, scheda che mostra l'effetto atteso, pulsanti di conferma e annullamento. Solo token del sistema di
  design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono presenti
  in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La creazione di una bozza e la sua conferma sono due righe del registro e
  consumano due unità della metrica `actions` (natura `flow`); a quota esaurita si risponde `429` con
  l'indicazione del rimedio. Il ruolo richiesto per confermare è quello richiesto dall'azione sottostante: chi non
  può cambiare una regola non può confermare una bozza che la cambia.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati come sopra, tutti marcati **scrittura** con
  conferma umana obbligatoria; `approva_azione` **deliberatamente non esposto**, con la motivazione scritta dentro
  il contratto. Il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Il motivo scritto di un rifiuto è testo libero prodotto da una persona: la voce
  esiste già nel manifesto dalla storia 0021 e va estesa per indicare che può nascere dal canale conversazionale.
  Nessun campo nuovo.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «bozza creata dal canale conversazionale», «bozza confermata»,
  «bozza scaduta» sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza
  dati personali.

## 4. Criteri di accettazione

**CA-1 — La bozza non produce l'effetto**
- **Dato** una regola che oggi dice «consenti» sullo strumento `cancella_cliente`
- **Quando** l'assistente invoca `proponi_regola` per portarla a «richiedi approvazione»
- **Allora** compare una bozza in attesa, la regola in vigore **non cambia**, e nessuna richiesta di nulla osta si
  comporta in modo diverso finché la bozza non è confermata

**CA-2 — L'approvazione non è esposta**
- **Dato** l'elenco degli strumenti dichiarati dal servizio
- **Quando** lo si interroga
- **Allora** non compare nessuno strumento che approvi un nulla osta, e la dichiarazione contiene la motivazione
  dell'assenza

**CA-3 — Il rifiuto passa comunque da una persona**
- **Dato** tre nulla osta in attesa sullo strumento `elimina_archivio`
- **Quando** l'assistente invoca `nega_azione` su uno di essi
- **Allora** si crea una bozza di rifiuto; i tre nulla osta restano in attesa; solo dopo la conferma di una
  persona uno di essi passa a «negato», con il motivo scritto e l'indicazione dell'origine conversazionale

**CA-4 — Ruolo insufficiente**
- **Dato** un utente con ruolo che non consente di cambiare le regole
- **Quando** tenta di confermare una bozza di regola
- **Allora** riceve `403` e la bozza resta in attesa

**CA-5 — Scadenza della bozza**
- **Dato** una bozza creata e non confermata entro la sua scadenza
- **Quando** la scadenza passa
- **Allora** la bozza risulta scaduta, nessun effetto è stato prodotto, e la scadenza compare come riga del
  registro

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con bozze in attesa
- **Quando** un utente di `A` tenta di confermare la bozza di `B` usandone l'identificativo
- **Allora** riceve la stessa risposta che riceverebbe per una bozza inesistente, e nulla accade

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina a stati della bozza e di **integrazione** sulle due rotte, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle bozze;
- [ ] **prova di sicurezza specifica**: un collaudo che fallisce se un giorno qualcuno dichiara uno strumento che
      approva un nulla osta — il divieto va reso **rosso automaticamente**, non affidato alla memoria;
- [ ] **prova end-to-end**: risposta «coprire ora» limitatamente alla parte di interfaccia (creazione e conferma
      di una bozza), estendendo il percorso `[J-AGENTAUDIT]`; la parte conversazionale resta `da-coprire` con
      proprietaria l'epica di piattaforma 12, nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per l'origine conversazionale del motivo scritto;
- [ ] **registro delle decisioni** compilato, con la voce obbligatoria sul perché `approva_azione` non esiste;
- [ ] contratto degli **strumenti conversazionali** dichiarato, comprese le assenze motivate;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove il contratto degli strumenti è descritto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0034` | Il contratto degli strumenti di lettura definisce forma e convenzioni |
| storie `0019`, `0021` | Le regole e i nulla osta sono ciò che le bozze modificano |
| storia `0027` | `prepara_esportazione` prepara l'estrazione già costruita lì |
| UC 0061-0063 (livello conversazionale, non implementato) | Manca il server che pubblica gli strumenti: le bozze e la conferma sono comunque utilizzabili dall'interfaccia |

## 7. Fuori ambito

- il consumo di quota e i ruoli sulle chiamate dell'assistente in generale: storia 0036;
- qualunque forma di approvazione automatica o assistita: **non è rimandata, è esclusa**;
- la conferma di una bozza da fuori dall'applicazione (per esempio da un messaggio di posta): non nel perimetro
  iniziale, perché sposterebbe il punto di responsabilità fuori da un contesto autenticato.

## 8. Punti aperti

- **Durata della scadenza di una bozza**: propongo un'ora, perché una bozza vecchia è un rischio. Da confermare.
- **Se la conferma debba richiedere una riautenticazione** per le decisioni più gravi (per esempio il rifiuto in
  blocco di molte richieste). È una scelta di sicurezza e di attrito: la segnalo, non la decido.
