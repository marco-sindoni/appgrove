# UC 0108 — Cruscotto del collaboratore, senza azioni dispositive

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.3 Esperienza per ruolo](../epic/E22-03-esperienza-per-ruolo.md)
**Dipendenze**: UC 0107 (visibilità per ruolo), UC 0097 (cruscotto operativo del workspace)
**Piano di lavoro**: [task/0108](../task/0108-cruscotto-collaboratore.md)
**Prototipi**: [admin](../prototype/admin.html) · [editor](../prototype/editor.html) · [viewer](../prototype/viewer.html)
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Dare al collaboratore un cruscotto **utile e onesto**: le applicazioni a cui è abilitato e come entrarci,
senza nulla che appartenga al governo dell'account.

**Incluso**: quali riquadri del cruscotto restano e quali spariscono per un collaboratore; il testo di
benvenuto; il caso «nessuna applicazione»; le scorciatoie.

**Escluso**: il menu → UC 0107; il catalogo → UC 0109.

## 2. Attori & ruoli

- **Collaboratore**: vede il cruscotto ridotto, con qualunque ruolo sulle applicazioni.
- **Owner**: vede il cruscotto completo come oggi, senza cambiamenti.

## 3. Precondizioni

- Esiste il cruscotto operativo (UC 0097) con i suoi riquadri: applicazioni attive, spesa, scadenze,
  attività, scorciatoie.
- La shell conosce gli accessi della persona (UC 0099, UC 0107).

## 4. Flusso principale

1. Il collaboratore entra e vede il cruscotto.
2. Restano: il **saluto**, l'elenco delle **applicazioni a cui è abilitato** — con il ruolo che ha su
   ognuna, dichiarato senza giri di parole («puoi consultare», «puoi modificare», «puoi gestire gli
   utenti») — e il pulsante per **entrare** in ognuna.
3. Sparisce tutto ciò che è dispositivo o di governo:
   - il comando «**gestisci il piano**» sulla scheda di ogni applicazione (porta alla fatturazione);
   - la **spesa mensile** dell'account e ogni cifra economica;
   - le **scadenze di pagamento** e gli avvisi di fatturazione;
   - la scorciatoia «**invita una persona**»;
   - le scorciatoie a fatturazione e catalogo restano solo dove hanno senso (il catalogo sì, in sola
     lettura; la fatturazione no).
4. Sulla scheda di ogni applicazione resta la **barra di consumo** della quota, se l'applicazione la
   dichiara: è informazione operativa e riguarda il lavoro, non il denaro. Nessun invito all'aumento di
   piano, che sarebbe una leva dell'owner.

## 5. Flussi alternativi / edge / errori

- **Edge — nessuna applicazione abilitata**: il cruscotto lo dice con chiarezza e senza colpevolizzare
  («il titolare dell'account non ti ha ancora abilitato a nessuna applicazione»), e offre due vie: il
  catalogo, dove può chiedere l'installazione di qualcosa (UC 0109), e il supporto.
- **Edge — applicazione abilitata ma disattivata dalla piattaforma**: la scheda resta visibile con lo
  stato, come già avviene, senza suggerire azioni che il collaboratore non può compiere.
- **Edge — quota esaurita**: il collaboratore lo vede (è informazione di lavoro) ma non riceve l'invito ad
  aumentare il piano: al suo posto, il suggerimento di avvisare il titolare dell'account.
- **Errore — letture non disponibili**: stato di errore con possibilità di riprovare, come già oggi.

## 6. Schermate & stati

Il cruscotto del collaboratore ha **un solo blocco** invece di quattro: le sue applicazioni. Il saluto
resta. Sotto le schede, una riga di scorciatoie ridotta.

Ogni scheda di applicazione mostra: nome e icona, il **ruolo** della persona su quella applicazione,
l'eventuale barra di consumo, il pulsante «Apri». Niente stato dell'abbonamento, niente prezzo, niente
rinnovo.

Stati: caricamento, pronto, nessuna applicazione, errore.

## 7. Dati toccati

Nessuno nuovo. La pagina consuma la stessa lettura di UC 0099 (applicazioni con ruolo) invece della lettura
dei diritti dell'account, che per un collaboratore non è pertinente. Attenzione a **non chiedere** le
letture economiche quando chi guarda non è l'owner: sarebbero rifiutate e produrrebbero errori inutili in
console.

## 8. Permessi & gate

- La pagina è accessibile a tutti gli autenticati; **il contenuto** dipende dal ruolo di piattaforma.
- Le letture economiche sono chiamate **solo** se chi guarda è l'owner.
- Nessun comando dispositivo raggiungibile, nemmeno disabilitato: qui si tratta di ambito, non di
  permesso su una singola operazione (regola di UC 0101).

## 9. Requisiti di test

- **Componente**: per un collaboratore il cruscotto non contiene «gestisci il piano», né cifre, né la
  scorciatoia di invito; per l'owner tutto resta come prima (prova di non-regressione).
- **Componente**: nessuna chiamata alle letture economiche quando chi guarda non è l'owner (verifica sulle
  chiamate simulate).
- **Componente**: caso «nessuna applicazione» con i suoi due rimandi.
- **Percorso end-to-end di livello 2** su `frontend/apps/backoffice/e2e/dashboard.spec.ts` (esistente, da
  estendere).

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [UC 0097](../../21-catalogo-app-backoffice/0097-dashboard-operativa.md),
  [DashboardPage.tsx](../../../../frontend/apps/backoffice/src/pages/dashboard/DashboardPage.tsx) — dove
  oggi `canManage` è calcolato su `owner` **oppure** `admin` e va rifatto sul solo ruolo di piattaforma.
- **Definition of Done**:
  1. il cruscotto del collaboratore mostra solo le sue applicazioni, con il suo ruolo;
  2. nessuna leva dispositiva né cifra economica;
  3. il caso «nessuna applicazione» è accogliente e offre vie d'uscita;
  4. il cruscotto dell'owner non cambia;
  5. `run-tests.sh frontend` verde più il percorso aggiornato.

## Punti aperti / decisioni differite

- **Sezione «inviti in attesa» in testa al cruscotto** (da [UC 0118](0118-inviti-e-registrazione-con-identita-esistente.md)):
  chi appartiene o può appartenere a più account trova qui gli inviti ricevuti, con accetta e rifiuta, prima
  delle applicazioni; la voce «Dashboard» del menu porta il numero. È lavoro di UC 0118, ma **atterra su
  questa schermata**: da coordinare quando si implementano. Reso in
  [prototype/admin.html](../prototype/admin.html). Proprietario: UC 0118.

- **Riquadro delle attività recenti**: se un collaboratore debba vedere le attività dell'account o solo le
  proprie. Proposta: **solo le proprie**, per minimizzazione. Da rifinire quando il riquadro esisterà
  davvero con dati reali. Proprietario: UC 0097.
- **Avviso al titolare quando una quota è esaurita**: utile, e sarebbe il gemello della richiesta di
  installazione (UC 0109). Rimandato a dopo il primo uso reale. Annotato in
  [docs/_BACKLOG.md](../../../_BACKLOG.md).
