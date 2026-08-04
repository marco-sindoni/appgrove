# 0031 — Conservazione del contenuto per strumento

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 06 — Dati delle persone e diritti
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0030`, `0018`
**Ultimo aggiornamento**: 2026-08-03

> 🛑 **Fermata di escalation — classificazione dei dati personali.** Questa storia apre deliberatamente la porta a
> un trattamento la cui natura **non è prevedibile**: i parametri di una chiamata possono contenere qualunque cosa,
> comprese categorie particolari di dati (articolo 9: salute, dati biometrici o genetici, opinioni politiche,
> convinzioni religiose, orientamento sessuale, appartenenza sindacale). L'app **non vuole** trattarli, non ha
> campi che li ospitino e non li chiede — ma se il cliente attiva la conservazione su uno strumento che li
> maneggia, entrano. La classificazione di ciò che entrerebbe è **materialmente ambigua** e non la decide un
> agente: è una decisione dello sviluppatore, con la revisione legale già chiamata in causa dalla storia 0030.

## 1. Narrazione

> Come titolare che deve poter dimostrare **che cosa** è stato scritto in un messaggio inviato ai propri clienti da
> un agente
> voglio poter attivare la conservazione del contenuto per quel singolo strumento, sapendo esattamente cosa comporta
> così da avere la prova dove mi serve davvero, senza trasformare tutto il registro in un deposito di dati altrui.

**Contesto.** L'impostazione predefinita dell'app è **non conservare i contenuti** (storia 0009), ed è la scelta
che le impedisce di diventare la raccolta di dati sensibili più grande e meno presidiata dell'azienda del cliente
(§6.3 della [descrizione dell'applicazione](../application-description.md)). Ma quell'impostazione ha un costo: per
alcuni strumenti l'impronta non basta. Se un agente ha mandato una comunicazione a un cliente e quel cliente
contesta il contenuto, sapere che «il parametro `testo` era lungo 412 caratteri e aveva questa impronta» non serve
a nulla — a meno che qualcuno non produca il testo da confrontare, che è precisamente ciò che nella contestazione
manca.

Perciò la conservazione esiste, ma **come eccezione governata**: uno strumento alla volta, a mano, con un avviso
che dice cosa comporta, con un ruolo alto, e per un tempo più corto della catena. Ogni parola di quella frase è un
requisito.

## 2. Requisiti funzionali

1. **RF-1** — La conservazione del contenuto si attiva **per un singolo strumento** del catalogo (storia 0018),
   mai per una sorgente intera e mai globalmente.
2. **RF-2** — L'attivazione mostra un avviso esplicito, che il richiedente deve confermare, e che dice: che cosa
   verrà conservato, per quanto tempo, chi potrà vederlo, che il contenuto può contenere dati di persone
   imprevedibili e che il titolare del trattamento resta il cliente.
3. **RF-3** — L'attivazione richiede un **ruolo alto** (proprietario o amministratore): non la può fare un membro
   e non la può fare un revisore (storia 0029).
4. **RF-4** — La conservazione ha una **durata propria, più corta della conservazione della catena**, con un
   valore predefinito prudente e modificabile entro un massimo.
5. **RF-5** — La conservazione vale **da quel momento in avanti**: non si applica retroattivamente alle azioni già
   registrate, perché il contenuto di quelle non l'abbiamo mai avuto.
6. **RF-6** — Attivazione, modifica della durata e disattivazione sono **righe del registro**: chi ha deciso di
   conservare i contenuti di quello strumento, quando e con quale durata, è un fatto che deve restare.
7. **RF-7** — Alla disattivazione i contenuti già conservati **non spariscono da soli**: restano fino alla loro
   scadenza o fino a una cancellazione esplicita (storia 0032), e l'interfaccia lo dice chiaramente per non
   ingannare chi crede di aver «spento e pulito».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'impostazione di conservazione è per account e per strumento
  dell'account, con `tenant_id` preso dal token verificato. Due account che osservano uno strumento con lo stesso
  nome hanno impostazioni indipendenti.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `PUT /api/agentaudit/v1/tools/{id}/content-retention` con
  corpo validato (attivo, durata, conferma esplicita dell'avviso); errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit. La rotta di ingresso delle azioni (storia 0008) consulta
  questa impostazione per decidere se scrivere il contenuto nel deposito cifrato.
- **RT-3 — Persistenza (§8).** Migrazione `V…__conservazione_contenuti.sql` sullo schema `app_agentaudit`: campi
  di conservazione sulla tabella degli strumenti, con `tenant_id`, colonne di controllo e cancellazione logica. I
  contenuti veri vanno nel deposito cifrato della storia 0030, non qui.
- **RT-4 — Modulo frontend (§3, §5).** Comando nella scheda dello strumento, con l'avviso da confermare in una
  finestra dedicata; lo stato «contenuti conservati» è visibile nell'elenco degli strumenti a colpo d'occhio, non
  nascosto in un pannello di impostazioni; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono presenti
  in `en, it, fr, es, de`. **Il testo dell'avviso è la stringa più delicata dell'applicazione**: è ciò su cui il
  cliente basa una decisione che riguarda dati di terzi, e va tradotto con la stessa cura di un testo legale — non
  con una traduzione automatica.
- **RT-6 — Varchi e quota (§6, §7).** L'attivazione richiede ruolo sufficiente (`403` altrimenti) e abbonamento
  attivo (`402` altrimenti). Non consuma quota se non per la riga di registro che la traccia. **Nota di prodotto**:
  la conservazione dei contenuti ha un costo di deposito che cresce nel tempo e che la metrica `actions` non
  misura — se diventasse una funzione molto usata, il modello di listino andrebbe riconsiderato (punto aperto).
- **RT-7 — Esposizione conversazionale (§12).** L'attivazione **non viene esposta** come strumento a un
  assistente: accendere la conservazione dei contenuti su richiesta di un modello linguistico è precisamente ciò
  che questa storia esiste per impedire. Il divieto va scritto nel contratto degli strumenti (storia 0035) accanto
  a quello sull'approvazione.
- **RT-8 — Dati personali (§10).** Voci nel manifesto `docs/compliance/manifests/agentaudit.yaml` in italiano e
  inglese: l'impostazione stessa (chi l'ha attivata) e il rimando alla voce del contenuto allegato già dichiarata
  dalla storia 0030. Campi annotati `@PersonalData`, tabelle presenti in `exportData` e `purgeData`. **La
  presenza di categorie particolari non è esclusa e va dichiarata come possibile**, non taciuta perché scomoda.
- **RT-9 — Registrazione eventi (§14).** Attivazione, modifica e disattivazione sono registrate con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione; nel registro tecnico non finisce nessun contenuto.

## 4. Criteri di accettazione

**CA-1 — Attivazione consapevole**
- **Dato** un amministratore sulla scheda di uno strumento senza conservazione
- **Quando** attiva la conservazione
- **Allora** deve confermare un avviso che dice cosa verrà conservato, per quanto, chi potrà vederlo e che il
  titolare resta il cliente; senza conferma nulla viene attivato

**CA-2 — Ruolo insufficiente**
- **Dato** un utente con ruolo di membro o di revisore
- **Quando** tenta di attivare la conservazione
- **Allora** riceve `403` con l'indicazione del ruolo necessario, e nulla cambia

**CA-3 — Vale in avanti, non all'indietro**
- **Dato** uno strumento con mille azioni già registrate senza contenuto
- **Quando** si attiva la conservazione
- **Allora** le mille azioni precedenti restano senza contenuto, e solo le successive lo portano

**CA-4 — Disattivare non è cancellare**
- **Dato** uno strumento con contenuti conservati e conservazione attiva
- **Quando** l'amministratore disattiva la conservazione
- **Allora** i nuovi contenuti non vengono più scritti, i contenuti esistenti restano fino alla scadenza,
  l'interfaccia lo dichiara e propone la cancellazione esplicita (storia 0032)

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che osservano uno strumento con lo stesso nome
- **Quando** `A` attiva la conservazione
- **Allora** in `B` nulla cambia, e nessun contenuto di `B` viene scritto

## 5. Definizione di fatto

- [ ] **la revisione legale richiamata dalla storia 0030 è stata fatta e il suo esito è recepito**, e la
      classificazione dei dati che possono entrare è stata confermata dallo sviluppatore — voce di sbarramento;
- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla decisione «scrivere o non scrivere il contenuto» e di **integrazione** sul percorso
      completo attivazione → ingresso di un'azione → contenuto cifrato nel deposito, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** sull'impostazione e sui contenuti scritti;
- [ ] prova di **matrice dei ruoli** sull'attivazione;
- [ ] **prova end-to-end**: **coprire ora** — il percorso `[J-AGENTAUDIT]` (storia 0037) riceve il passo
      «attivazione della conservazione con avviso confermato», e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) è aggiornato di conseguenza;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), con cura particolare al testo
      dell'avviso;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con la possibile presenza di categorie particolari
      dichiarata invece che taciuta;
- [ ] **registro delle decisioni** compilato, con le voci su: attivazione per singolo strumento, durata più corta
      della catena, non retroattività, disattivazione che non cancella;
- [ ] contratto degli **strumenti conversazionali**: dichiarato che l'attivazione **non** viene esposta, con il
      motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0030` | Senza i due strati e il deposito cifrato non c'è dove mettere il contenuto |
| storia `0018` | La conservazione si attiva su uno strumento del catalogo: il catalogo deve esistere |
| storia `0008` | È la rotta di ingresso che, consultata l'impostazione, scrive o non scrive il contenuto |
| storia `0029` | Il divieto per il revisore presuppone che il ruolo esista |
| **Revisione legale + conferma della classificazione** | Fermata di escalation dichiarata in testa |

## 7. Fuori ambito

- **la cancellazione dei contenuti su richiesta**: storia 0032;
- **la rivelazione del contenuto** nella scheda di un'azione: storia 0025;
- **la conservazione selettiva per singolo parametro** («conserva `testo` ma non `destinatario`»): sarebbe più
  fine e più sicura, ma presuppone che l'app conosca la semantica dei parametri di uno strumento che non è suo.
  Rimandata, e citata qui perché è la prima evoluzione sensata;
- **il riconoscimento automatico di categorie particolari** nel contenuto: l'app non fa rilevazione semantica
  (storia 0010 si ferma alla forma dei segreti), e fingere il contrario sarebbe una promessa che non si mantiene.

## 8. Punti aperti

- **La durata predefinita della conservazione dei contenuti.** Propongo trenta giorni, molto più corta della
  catena, perché il contenuto serve quasi sempre a ridosso del fatto. Ma è un numero che ha effetti legali e
  commerciali insieme: lo conferma lo sviluppatore.
- **Se esista un tetto massimo invalicabile.** Sono propenso a sì — un cliente che imposta dieci anni di
  conservazione dei contenuti sta costruendo un problema che poi è anche nostro. Dove metterlo, non lo so.
- **Il costo di deposito non è misurato dalla metrica di quota** (`actions`, natura `flow`): se la conservazione
  dei contenuti diventasse molto usata, il listino andrebbe riconsiderato. Fermata di escalation sui prezzi: non
  la decide un agente (§5 della descrizione dell'applicazione).
- **Chi può vedere un contenuto conservato**: la decisione è condivisa con le storie 0025 e 0029 e va presa una
  volta sola.
