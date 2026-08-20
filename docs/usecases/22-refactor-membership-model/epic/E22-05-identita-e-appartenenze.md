# E22.5 — Identità della persona e appartenenze agli account

**Epica madre**: [Epica 22](E22-00-rifacimento-modello-appartenenza.md) · **Storie**: 0116, 0117, 0118
**Stato**: 🟢 analisi scritta · **Ultimo aggiornamento**: 2026-08-20

> **Numerata per ultima, si esegue per prima.** Le sotto-epiche portano il numero nell'ordine in cui sono
> state scritte, non in quello in cui si eseguono — come gli identificativi del registro delle decisioni,
> che non si rinumerano mai. Questa è nata dopo, esaminando il modello dei dati durante la rilettura, ed è
> **la prima da eseguire**: vedi «Perché prima di tutto il resto».

## Obiettivo

Sciogliere il vincolo **«una persona appartiene a un solo account»** e sostituirlo con **una persona, più
appartenenze**. Poi dare alla sessione il concetto di **account attivo**, e ai due percorsi d'ingresso
(invito, registrazione) la capacità di riconoscere un'identità che esiste già.

## Il problema, in due frasi che si vedono al primo cliente

`utente1@gmail.com`, invitato da `proprietario@business.com`, **non può** aprire un proprio account con lo
stesso indirizzo. E chi ha già provato appgrove per conto proprio **non può essere invitato** da un'azienda:
l'invito parte e il rifiuto arriva più tardi, come violazione di indice invece che come messaggio
comprensibile.

Non è una dimenticanza: è una scelta dichiarata di UC 0013, scritta nello schema come indici unici globali
su una tabella che sta **dentro** l'account
([V2__core_domain.sql](../../../../services/core/src/main/resources/db/migration/V2__core_domain.sql):
«membership foldata 1 utente→1 tenant»), registrata in [docs/02 §14](../../../02-auth-sicurezza.md) e
assunta dalla funzione che costruisce il token, che cerca **una** riga per identificativo di autenticazione.
Era la semplificazione giusta per un prodotto con un solo utente per cliente. Non lo è più per un prodotto
che vende posti.

## Perché prima di tutto il resto

[UC 0098](../story/0098-modello-dati-accesso-per-applicazione.md) crea la tabella degli accessi con un
riferimento alla persona. Se la forma dell'identità cambiasse **dopo**, quella tabella e la sua migrazione
si rifarebbero — e con esse la migrazione degli account (UC 0113). Stessa tabella, stessa funzione del
token, toccate due volte: la ragione per cui questa sotto-epica apre l'epica invece di chiuderla.

Il costo di anticiparla è basso: 0098 nasce direttamente col riferimento all'identità e non se ne accorge.

## Le tre storie

1. **[0116 — Identità e appartenenze](../story/0116-identita-e-appartenenze.md).** Il modello: identità di
   piattaforma, appartenenza di account, unicità spostata dove deve stare, migrazione senza perdite.
2. **[0117 — Account attivo e selettore](../story/0117-account-attivo-e-selettore.md).** Se le appartenenze
   sono più di una, il token deve dire *per conto di chi* stai agendo. È la storia più delicata dell'intera
   epica: tocca il punto in cui si stabilisce l'identità e l'account, cioè l'invariante numero uno.
3. **[0118 — Inviti e registrazione con identità esistente](../story/0118-inviti-e-registrazione-con-identita-esistente.md).**
   I due percorsi d'ingresso, e i messaggi comprensibili al posto delle violazioni di indice.

## Le decisioni portanti

**L'unicità si sposta, non si allenta.** Oggi l'indirizzo è unico globalmente su una tabella che è dentro
l'account: è quel disallineamento a produrre il vincolo di troppo. Dopo, l'indirizzo è unico sull'**identità**
(che non è di nessun account) e il vincolo che serve davvero — «non due volte nello stesso account» —
diventa **esplicito** sulla coppia. La regola giusta era nascosta dentro una regola più larga.

**L'account attivo vive in banca dati, non presso il fornitore di identità.** Il gruppo di utenti Cognito
non dichiara attributi personalizzati ([auth.tf](../../../../infra/modules/platform_shared/auth.tf)) e
aggiungerne uno per via dichiarativa rischia di **ricreare il gruppo**, cioè di perdere gli utenti. La
funzione che costruisce il token interroga già la banca dati: una colonna in più non costa nulla e non
tocca l'infrastruttura.

**Il valore conservato non è creduto.** L'account attivo è un *suggerimento*: la verità è l'appartenenza
riverificata al momento della creazione del token. È la riga che impedisce a una manomissione di quella
colonna di diventare un varco fra due aziende — e l'invariante «account solo dal token verificato» resta
intatta, perché cambia la funzione che calcola il claim, non chi se ne fida.

**Gli esiti dell'invito non rivelano l'esistenza dell'identità.** Un messaggio chiaro — «questa persona ha
già un account appgrove» — sarebbe comodo e inaccettabile: rivelerebbe a un'azienda un rapporto che non le
appartiene. I messaggi comprensibili restano per le collisioni che sono **informazione dell'account** (già
membro, invito in attesa).

**Il posto si paga in ogni account.** La stessa persona in due account occupa un posto in ciascuno: ogni
account paga le persone che usano *le sue* applicazioni. È la regola più semplice ed è anche quella giusta,
ma va scritta nel testo mostrato al cliente, perché la prima reazione sarà «ma la paga già l'altra azienda».

**Con una sola appartenenza nulla cambia.** È il caso di tutti gli utenti di oggi: nessun selettore, nessun
passaggio in più. Un passaggio aggiuntivo lì sarebbe una regressione per il cento per cento delle persone a
beneficio di una minoranza.

## Rischi

| Rischio | Perché | Come lo si tiene |
|---|---|---|
| **Perdere persone nel travaso** | una migrazione che divide una tabella in due | controllo dei conteggi **dentro** la migrazione, che la fa fallire se non tornano; `platform.users` conservata come via di ritorno |
| **Varco fra due aziende** | si tocca il punto in cui si stabilisce l'account | l'appartenenza si riverifica sempre; prova di sicurezza che manomette la colonna |
| **Divergenza fra i due fornitori di identità** | la regola è attuata due volte, in Java e in Python | stessa tabella di casi eseguita su entrambe, commento incrociato |
| **Rivelare l'esistenza di un'identità** | il messaggio utile è anche quello che rivela | esiti indistinguibili, con collaudo dedicato |
| **Regressione per chi ha un solo account** | il caso semplice si complica per servire quello raro | collaudo che pretende l'assenza del selettore |

## Confini

**Dentro**: identità, appartenenze, account attivo, selettore, i due percorsi d'ingresso, migrazione dei
dati esistenti, parità dei fornitori di identità.

**Fuori**: più di un `owner` per account (resta escluso, requisito dello sviluppatore); l'unione di due
identità create per errore con indirizzi diversi; un piano commerciale per chi lavora per più clienti — la
sotto-epica lo rende **possibile**, ma il prodotto non lo prevede e la decisione è commerciale.

## Punti aperti

- **Rimborso del posto** se l'invito viene rifiutato o scade — riguarda denaro, quindi non si decide qui.
  Proprietario: [UC 0103](../story/0103-acquisto-anticipato-posto-invito.md).
- **Durata del token e ritardo massimo di una revoca**: legame che c'era già e che questa sotto-epica rende
  visibile. Va **scritto**, non lasciato implicito. Proprietario: UC 0017.
- ~~**Dove si vedono gli inviti in attesa**~~ — **chiuso**: nell'intestazione, accanto al selettore
  dell'account. Reso nei prototipi (`admin.html`) e mappato su `shell/InvitesMenu.tsx`.
