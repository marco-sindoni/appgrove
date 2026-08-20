# UC 0110 — «I miei dati» in forma ridotta per il collaboratore

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.3 Esperienza per ruolo](../epic/E22-03-esperienza-per-ruolo.md)
**Dipendenze**: UC 0107 (visibilità per ruolo), UC 0033 (self-service dei diritti dell'interessato)
**Piano di lavoro**: [task/0110](../task/0110-miei-dati-forma-ridotta.md)
**Prototipo**: [viewer.html](../prototype/viewer.html), sezione «I miei dati»
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo / Scope

Separare, dentro la pagina «I miei dati», **i diritti della persona** dagli **atti sull'account**: i primi
restano a tutti, i secondi diventano dell'owner.

**Incluso**: la forma ridotta della pagina per i collaboratori; la conferma che le vie di conformità restano
esenti dai ruoli; la revisione dei presidi lato servizio (oggi tarati su «owner o admin»).

**Escluso**: il resto della visibilità → UC 0107; le funzioni di conformità in sé, che esistono già e non
cambiano comportamento.

## 2. Attori & ruoli

- **Collaboratore** (qualunque ruolo sulle applicazioni): esercita i diritti **sui propri** dati.
- **Owner**: esercita anche gli atti sull'account.

## 3. Precondizioni

- Esiste la pagina dei diritti dell'interessato (UC 0033) con: rettifica del proprio nome, esportazione del
  **proprio** profilo, esportazione dell'**account**, recesso per applicazione, cancellazione
  dell'account, dichiarazione dei diritti.
- Esiste già l'esportazione del profilo aperta a ogni ruolo, dichiarata «esente dai gate»
  ([ProfileExportResource.java](../../../../services/core/src/main/java/app/appgrove/core/gdpr/ProfileExportResource.java)).

## 4. Flusso principale

1. Il collaboratore apre «I miei dati» e trova **tre** blocchi:
   1. **Il tuo profilo** — rettifica del nome visualizzato (art. 16 del Regolamento europeo);
   2. **Scarica i tuoi dati** — esportazione immediata del proprio profilo (artt. 15 e 20);
   3. **I tuoi diritti e chi contattare** — la dichiarazione dei diritti già presente, l'informativa e il
      contatto per la protezione dei dati.
2. **Non** trova: l'esportazione di tutto l'account, il recesso per applicazione, la cancellazione
   dell'account. Sono atti del titolare dell'account, e per il collaboratore non esistono nemmeno come
   comandi disabilitati.
3. Una riga di testo spiega la separazione, senza gergo: «qui trovi i dati che riguardano te. I dati
   dell'account e la sua chiusura sono del titolare dell'account.»
4. L'owner continua a vedere la pagina completa, esattamente come oggi.

## 5. Flussi alternativi / edge / errori

- **Edge — il collaboratore vuole cancellare i propri dati.** È il caso più delicato, e va gestito con
  onestà: la sua cancellazione dall'account **non è un atto suo** (dipende dal rapporto con il titolare),
  quindi la pagina **non** offre un pulsante «cancellami». Offre invece il **contatto** e la spiegazione:
  la richiesta si rivolge al titolare dell'account, e appgrove resta raggiungibile al contatto per la
  protezione dei dati. Va scritto in modo comprensibile, perché è esattamente la domanda che una persona
  si fa.
- **Edge — persona indicata per la cessazione** (UC 0104): può ancora esportare i propri dati, fino a
  quando esiste. Dopo la rimozione, valgono le regole di conservazione già stabilite.
- **Errore — esportazione non disponibile**: messaggio con possibilità di riprovare; il diritto non si
  nega per un guasto.
- **Edge — la persona è anche l'unico owner**: vede la pagina completa; nessun caso particolare.

## 6. Schermate & stati

Stessa pagina, meno blocchi. Non si crea una pagina nuova: si nascondono i blocchi non pertinenti, così
che ogni miglioramento futuro alla pagina valga per entrambi i casi. Ordine dei blocchi invariato per
l'owner.

Stati: caricamento, pronto, esportazione in corso, errore. Testi nelle cinque lingue, senza gergo giuridico
non spiegato: si nominano gli articoli, ma la frase è in italiano corrente.

## 7. Dati toccati

Nessuna modifica ai dati. Cambia **quali** operazioni la pagina offre a chi. Si conferma che l'esportazione
del profilo resta **esente** da ogni varco (diritti dell'interessato), come già stabilito.

Da verificare in implementazione: i presidi lato servizio delle operazioni di conformità **sull'account**
sono oggi tarati su «owner oppure admin»; con la scomparsa di `admin` come ruolo di piattaforma vanno
portati a **solo owner**. È una stretta, non un allargamento, e va provata.

## 8. Permessi & gate

- **Diritti della persona: nessun varco.** Nessun diritto d'accesso, nessuna quota, nessun ruolo.
- **Atti sull'account: solo owner**, nel servizio e nell'interfaccia.
- **Account solo dal token verificato**; il profilo esportato è sempre quello di chi chiama, mai un altro.

## 9. Requisiti di test

- **Componente**: per un collaboratore la pagina mostra tre blocchi e nessun atto sull'account; per l'owner
  la pagina è completa (non-regressione).
- **Integrazione**: un collaboratore che chiama le operazioni di conformità sull'account riceve un rifiuto;
  chi era `admin` prima del rilascio **non** le raggiunge più (prova esplicita della stretta).
- **Integrazione**: l'esportazione del proprio profilo riesce per ogni ruolo, compreso `viewer`.
- **Percorso end-to-end di livello 2** su `frontend/apps/backoffice/e2e/privacy.spec.ts` (esistente, da
  estendere) per la forma ridotta.

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [UC 0033](../../08-compliance-gdpr/0033-self-service-gdpr.md),
  [PrivacyPage.tsx](../../../../frontend/apps/backoffice/src/pages/privacy/PrivacyPage.tsx) — dove oggi
  `canManageAccountData` è `owner` **oppure** `admin`;
  [ProfileExportResource.java](../../../../services/core/src/main/java/app/appgrove/core/gdpr/ProfileExportResource.java).
- **Definition of Done**:
  1. il collaboratore vede i propri diritti e nessun atto sull'account;
  2. l'esportazione del proprio profilo funziona per ogni ruolo;
  3. i presidi degli atti sull'account sono portati a solo owner, con prova;
  4. il testo spiega la separazione e dice a chi rivolgersi per la cancellazione;
  5. le cinque lingue sono complete;
  6. `run-tests.sh frontend backend` verde più il percorso aggiornato.

## Punto di attenzione per la revisione legale

Questa storia nasce da una **correzione** al requisito iniziale, che chiedeva di nascondere del tutto la
sezione ai non-owner. La correzione è stata accettata dallo sviluppatore perché il diritto di accesso e di
portabilità appartiene a **ogni interessato** e non al titolare dell'account. Va **annotata nel registro di
revisione legale** ([docs/_REVISIONE-LEGALE.md](../../../_REVISIONE-LEGALE.md)) come punto da confermare a un
legale: la formulazione esatta di che cosa il collaboratore può esercitare da sé e che cosa deve chiedere al
titolare dell'account.

## Punti aperti / decisioni differite

- **Richiesta di cancellazione da parte di un collaboratore**: oggi si rimanda al contatto. Se il volume
  crescesse, servirebbe un percorso guidato (una richiesta che arriva all'owner e, per conoscenza, a
  appgrove). Proprietario: UC 0033.
- **Esportazione dei dati che il collaboratore ha prodotto dentro le applicazioni**: resta di titolarità
  dell'account, come già stabilito (i dati delle applicazioni appartengono all'account, non
  all'operatore). Confermato qui, non cambiato.
