# UC 0112 — Copilota dei ruoli nella skill `new-application` e parità dello scaffolding

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.4 Dentro le applicazioni](../epic/E22-04-app-e-industrializzazione.md)
**Dipendenze**: UC 0101 (semantica dei ruoli), UC 0099 (varco riusabile), UC 0111 (schermata condivisa), UC 0046 (skill `new-application`)
**Piano di lavoro**: [task/0112](../task/0112-copilota-ruoli-new-application.md)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Fare in modo che un'applicazione **nuova** nasca già rispettosa dei tre ruoli, e che chi la crea sia
**costretto a pensare** a quali sono le sue operazioni dispositive — perché è l'unica domanda che un
generatore non può rispondere al posto suo.

Lo sviluppatore ha chiesto di *valutare* questa evoluzione. La valutazione è affermativa, e con una ragione
precisa: senza questa domanda, ogni applicazione nuova nascerebbe con tre ruoli **dichiarati** e un
comportamento **indifferenziato**. Sarebbe peggio che non avere i ruoli, perché sembrerebbe funzionare.

**Incluso**: il nuovo passo di intervista della skill (il copilota dei ruoli); ciò che il generatore emette
di conseguenza; il documento delle operazioni dell'applicazione; l'aggiornamento dei modelli-sorgente e del
collaudo di parità; l'aggiornamento del registro delle deviazioni consapevoli.

**Escluso**: la semantica dei ruoli in sé → UC 0101; il componente della schermata → UC 0111.

## 2. Attori & ruoli

- **Chi crea un'applicazione** (Platform Engineer, attraverso la skill): risponde alle domande del
  copilota.
- **Skill `new-application`**: conduce l'intervista e passa le risposte al generatore.
- **Generatore deterministico**: emette il codice, i collaudi e il documento delle operazioni.
- **Collaudo di parità**: va in rosso se i modelli-sorgente restano indietro.

## 3. Precondizioni

- Esistono il varco riusabile (UC 0099), la semantica dei ruoli (UC 0101) e la schermata condivisa
  (UC 0111).
- Esiste la skill con i suoi quattro passi e il suo generatore
  ([tools/new-application](../../../../tools/new-application/)).

## 4. Flusso principale — il nuovo passo di intervista

Il copilota dei ruoli si colloca **dopo** l'identità dell'applicazione e **prima** del listino (le
operazioni dispositive influenzano la metrica di quota, quindi vanno chiarite prima dei prezzi). Fa
**cinque** domande, una alla volta, in prosa, come gli altri copiloti della skill:

1. **«Quali sono le operazioni dispositive di questa applicazione?»** — si parte dall'elenco delle
   operazioni che il generatore sta per creare e si chiede conferma della classificazione automatica
   (scrittura → almeno `editor`, lettura → `viewer`). Serve a cogliere i casi che la regola automatica
   sbaglia.
2. **«C'è qualcosa che un `viewer` non deve vedere affatto?»** — la risposta attesa è **no**, perché il
   contratto dice che il `viewer` vede tutti i dati. Un sì è un **segnale d'allarme**: significa che
   servirebbe un ruolo nuovo o che l'applicazione tratta dati particolarmente sensibili. In quel caso il
   copilota **si fermare e chiede** allo sviluppatore, e la risposta finisce nei punti aperti
   dell'applicazione. Non si inventa un quarto ruolo.
3. **«L'`admin` di questa applicazione ha poteri specifici, oltre alla gestione degli utenti?»** — per
   esempio configurazioni dell'applicazione, o l'accesso a una traccia di controllo. Se sì, quelle
   operazioni richiedono `admin` e vanno segnate.
4. **«Ci sono operazioni esenti dai ruoli?»** — tipicamente le vie di conformità sui propri dati. La
   risposta serve a non proteggerle per errore.
5. **«I dati di questa applicazione sono del gruppo di lavoro o della persona che li ha creati?»** — la
   domanda dell'**ambito dei dati** ([UC 0115](0115-ambito-dati-applicazione.md)), che **prende il posto**
   di quella sul modello utenti ritirata da [UC 0114](0114-ritiro-categoria-b2c-b2b.md). A differenza
   della precedente, questa cambia il filtro delle interrogazioni: dalla risposta dipende quale classe base
   il generatore usa per le entità. **In questo giro il generatore supporta solo `account`**: se la
   risposta è `utente`, il copilota **si ferma e lo dice** invece di generare a metà (le applicazioni si
   affrontano dopo il rifacimento dell'appartenenza — punto aperto in UC 0115).

Le risposte producono tre effetti concreti:

1. il **documento delle operazioni** dell'applicazione (UC 0101), scritto nel servizio;
2. le **annotazioni del varco** già applicate sulle operazioni generate, con il ruolo minimo giusto;
3. i **collaudi generati** che provano i tre ruoli: una prova per ruolo su una operazione di lettura e una
   di scrittura, più la verifica strutturale che nessuna scrittura sia priva di dichiarazione.

Ogni risposta finisce nel registro delle decisioni della change, come per gli altri copiloti.

## 5. Flussi alternativi / edge / errori

- **Edge — applicazione a utente singolo**: il copilota fa comunque le domande, perché anche un'applicazione
  pensata per una persona può essere guardata da due con ruoli diversi (§7 dell'epica). Le risposte saranno
  più semplici, non assenti.
- **Errore — chi crea l'applicazione non sa rispondere**: la skill **non** procede con valori inventati.
  Presenta la classificazione automatica come proposta e chiede conferma: è il minimo per non generare una
  finzione.
- **Edge — applicazione senza operazioni di scrittura** (sola consultazione): legittimo; il documento lo
  dichiara e i collaudi generati si adattano.
- **Edge — il modello-sorgente resta indietro**: è il modo tipico in cui questo impianto si guasta. Lo
  coglie il collaudo di parità, che confronta i modelli con l'applicazione di riferimento.

## 6. Risorse & runbook _(storia di strumenti)_

- Nuovo passo nella skill: `.claude/skills/new-application/step-02-roles.md` (i passi successivi si
  rinumerano).
- Modelli-sorgente del generatore aggiornati: varco cablato, documento delle operazioni, collaudi per
  ruolo, voce «Utenti» nel menu del modulo.
- `docs/_PARITA-SCAFFOLD.md`: se qualcosa resta deliberatamente indietro, va scritto **là** con il motivo.

## 7. Dati toccati

Nessun dato di prodotto. Si toccano modelli di generazione, la skill e i collaudi. Il documento delle
operazioni è un file dentro il servizio, leggibile da un programma, senza dati personali.

## 8. Permessi & gate

- **Il copilota si ferma e chiede** nel caso della seconda domanda (dati che un `viewer` non dovrebbe
  vedere): è materia di prodotto e potenzialmente di dati sensibili, quindi rientra nei casi in cui
  l'automatismo cede la parola allo sviluppatore.
- **Nessuna applicazione generata è priva del varco**: il collaudo di parità e la verifica strutturale lo
  garantiscono meccanicamente.
- **Mai correggere l'esito del generatore a mano**: se l'esito è sbagliato si corregge il modello e si
  rigenera. Regola già in vigore nella skill, che qui vale anche per i ruoli.

## 9. Requisiti di test

- **Collaudo del generatore** (area degli strumenti): un'applicazione generata contiene il varco su ogni
  operazione di scrittura, il documento delle operazioni e i collaudi per ruolo.
- **Collaudo di parità**: va in rosso se il modello-sorgente non porta i ruoli mentre l'applicazione di
  riferimento sì.
- **Applicazione generata verde**: la suite dell'applicazione appena creata passa senza interventi, compresi
  i collaudi dei ruoli.
- **Percorsi end-to-end**: nessuno proprio (è strumentazione); esente come *senza superficie*.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [UC 0046](../../10-skills-tooling/0046-skill-new-application.md),
  [SKILL.md](../../../../.claude/skills/new-application/SKILL.md),
  [step-01-identity.md](../../../../.claude/skills/new-application/step-01-identity.md),
  [docs/_PARITA-SCAFFOLD.md](../../../_PARITA-SCAFFOLD.md),
  [tools/scaffold-parity](../../../../tools/scaffold-parity/).
- **Definition of Done**:
  1. la skill ha il passo del copilota dei ruoli, con le quattro domande e la fermata prevista;
  2. il generatore emette varco, documento delle operazioni, collaudi per ruolo e voce «Utenti»;
  3. il collaudo di parità coglie un modello rimasto indietro;
  4. un'applicazione generata da zero nasce verde e rispetta i tre ruoli senza codice scritto a mano;
  5. `run-tests.sh tooling backend` verde.

## Punti aperti / decisioni differite

- **La skill che dismette un'applicazione** (`drop-application`) deve rimuovere anche le righe di accesso e
  la voce «Utenti». Da fare nella stessa change o subito dopo: annotato qui, proprietario UC 0048.
- **Ruoli e interfacce per assistenti automatici** (epica 12): quando maturerà, le sue operazioni dovranno
  dichiarare il ruolo minimo come tutte le altre. Annotato là.
- **Un quarto ruolo**: se la seconda domanda del copilota riceverà un «sì» ricorrente da più applicazioni,
  sarà il segnale che tre ruoli non bastano. Proprietario: UC 0101.
