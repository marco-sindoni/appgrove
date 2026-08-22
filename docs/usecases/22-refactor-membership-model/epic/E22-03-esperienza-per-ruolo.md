# E22.3 — Esperienza del backoffice per ruolo

**Epica madre**: [Epica 22](E22-00-rifacimento-modello-appartenenza.md) · **Storie**: 0107, 0108, 0109, 0110, **0119**
**Stato**: 🟢 analisi scritta · **Ultimo aggiornamento**: 2026-08-22
**Prototipi**: [owner](../prototype/owner.html) · [admin](../prototype/admin.html) · [editor](../prototype/editor.html) · [viewer](../prototype/viewer.html)

## Obiettivo

Rendere il backoffice **onesto** rispetto a chi lo guarda. Oggi ogni utente dell'account vede la stessa
interfaccia, comprese leve che non gli competono; da domani il collaboratore vede un prodotto più
piccolo ma **coerente**: nessuna voce che porti a un rifiuto, nessun pulsante che produca un errore di
autorizzazione.

## Il principio che tiene insieme le prime quattro storie

**Non mostrare ciò che non si può fare — ma non mentire su ciò che esiste.** Due regole applicate con
criterio diverso:

- Ciò che è **dell'owner** (governo dell'account: dati di fatturazione, gestione delle persone,
  cancellazione dell'account) **non compare** al collaboratore. Non è un segreto, è un ambito
  differente: mostrarlo disabilitato genererebbe solo la domanda «perché non posso?».
- Ciò che il collaboratore **potrebbe** fare in un altro contesto (creare un contatto, se fosse
  `editor`) compare **disabilitato con una spiegazione**. Qui nascondere sarebbe peggio: farebbe
  credere che la funzione non esista.

La distinzione non è estetica. È la specifica che chi implementa traduce in due meccanismi diversi:
*assenza* dalla navigazione contro *disabilitazione* con testo di aiuto.

## Le decisioni portanti

**Il menu laterale mostra solo le applicazioni a cui si ha accesso.** Oggi mostra l'intersezione fra
registro dei moduli e diritti dell'account; diventa un'intersezione a tre: registro ∩ diritti
dell'account ∩ **accesso della persona**. L'owner vede tutte le applicazioni dell'account per
definizione.

**Account, Billing e Members sono dell'owner.** Le tre voci scompaiono dal menu del collaboratore e
le rispettive rotte lo rimandano alla pagina di rifiuto — difesa a due livelli, perché nascondere una
voce non protegge un indirizzo digitato a mano. Nota tecnica: la guardia di oggi ammette `owner` **o**
`admin`; va stretta a `owner`, perché `admin` non è più un ruolo di piattaforma.

**«I miei dati» resta, in forma ridotta.** È la correzione più importante fatta ai requisiti iniziali,
e ha una ragione legale, non di gusto: il diritto di accedere ai propri dati e di portarli via
appartiene **a ogni persona**, non al titolare dell'account, e la documentazione di progetto lo
dichiarava già «esente dai gate, per ogni ruolo». Il collaboratore vede quindi la rettifica del proprio
nome, l'esportazione dei **propri** dati, l'informativa e il contatto; non vede l'esportazione
dell'intero account, il recesso per applicazione né la cancellazione dell'account.

**Il cruscotto del collaboratore è informativo.** Mostra le applicazioni a cui è abilitato e come
entrarci. Sparisce tutto ciò che è dispositivo — in primo luogo il collegamento «gestisci il piano» —
e sparisce quello che non gli riguarda (la spesa dell'account, le scadenze di pagamento).

**Il catalogo resta visibile a tutti, in sola lettura, con una via d'uscita utile.** Un collaboratore
che scopre un'applicazione interessante non deve trovare un muro: trova **«chiedi all'owner di
installarla»**, che recapita all'owner un'email con chi ha chiesto e cosa. È una funzione piccola con un
effetto sproporzionato: trasforma il collaboratore in un canale di vendita interno, invece che in un
utente frustrato.

## Sorveglianza sulla richiesta all'owner

Ogni funzione che spedisce email su iniziativa dell'utente è una potenziale seccatura per il
destinatario. La storia 0109 prevede quindi: **una richiesta per applicazione ogni ventiquattro ore**
per persona, una traccia visibile di ciò che è stato chiesto («già richiesto il …»), e **nessun dato
personale nel corpo oltre il nome di chi chiede** — l'owner sa già chi sono i suoi collaboratori.

## La quinta storia, aggiunta dopo: la responsività (UC 0119)

[UC 0119](../story/0119-responsivita-backoffice.md) sta in questa sotto-epica, e non nelle altre, per una
ragione di materia: **E22.3 è la sotto-epica che possiede la forma con cui il backoffice si presenta**.
Le prime quattro storie decidono *che cosa* si mostra a chi; la quinta decide *che il mostrato stia dentro
lo schermo*. Sono due dimensioni della stessa superficie — il ruolo di chi guarda e la larghezza da cui
guarda — e lavorano sugli stessi componenti: la barra laterale e il menu che 0107 rifà, le tabelle che
0100 e 0111 riempiono.

Il requisito **preesiste** e non lo inventa questa epica:
[docs/03-frontend.md](../../../03-frontend.md) punto 12 prescrive «tutto responsive dal PoC, backoffice
incluso» dal primo giorno. Nessuna storia dell'epica 22 lo presidiava e nessun collaudo lo misurava: 0119
lo rende **esigibile**, con un criterio verificabile invece di un giudizio a occhio.

**Si esegue per prima delle cinque**, non nell'ordine in cui è scritta — come E22.5 rispetto all'epica
intera. Il motivo è che le storie 0107–0111 aggiungono ancora colonne e comandi alle stesse tabelle:
mettere il collaudo prima significa che ognuna di esse trova un rosso se lo dimentica, invece di lasciare
il conto da pagare alla fine.

## Come si vede che ha funzionato

- Un collaboratore non incontra **mai** una pagina di rifiuto navigando normalmente.
- Un `viewer` capisce **perché** non può creare un contatto, senza dover chiedere.
- L'owner riceve richieste di installazione utili e non ripetute.
- Ogni persona, qualunque ruolo, può scaricare i propri dati.
- Le stesse schermate, su una finestra da telefono, non debordano e i loro comandi si raggiungono col
  pollice — e un collaudo automatico lo afferma a ogni commit, invece di lasciarlo alla vista.
