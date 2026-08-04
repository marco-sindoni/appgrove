# 0005 — Avvio locale e dati di prova

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che apre il repository per la prima volta
> voglio avviare SpendGrove in locale con dentro un account di prova già popolato
> così da vedere l'app funzionare in due minuti, invece di passare mezza giornata a costruirmi i dati a mano.

**Contesto.** Le quattro storie precedenti hanno costruito l'app; questa la rende **usabile subito dopo l'unione del
ramo**, che è un obbligo di piattaforma e non una cortesia. Serve adesso, prima di aprire l'epica del dominio,
perché ogni storia successiva vorrà provare a mano quello che ha scritto, e senza dati di partenza ognuno se li
inventerà a modo suo — con il risultato che due sviluppatori non vedranno mai la stessa cosa.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` mostra `notespese` con la sua porta e il suo schema, e `./app-start.sh` avvia
   l'app senza alcuna modifica manuale agli script.
2. **RF-2** — Esiste un insieme di dati di prova caricabile su richiesta: un account, tre collaboratori inventati,
   una ventina di spese distribuite su tutti gli stati, cinque immagini di ricevuta finte.
3. **RF-3** — I dati di prova sono **deterministici** (stessi identificativi, stesse date relative a oggi) e
   **inventati**: nessun nome reale, nessun esercente reale, nessuna immagine di un documento vero.
4. **RF-4** — Il caricamento dei dati di prova è **idempotente**: eseguirlo due volte non crea doppioni e non
   consuma quota.
5. **RF-5** — L'account di prova nasce con l'abilitazione accesa nello stub locale e con un piano che ha una quota
   piccola, così che il caso «quota esaurita» sia raggiungibile in pochi clic.
6. **RF-6** — La documentazione dell'app dice in tre righe come si avvia e come si caricano i dati di prova.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I dati di prova nascono su **due** account, non uno: serve un secondo
  account con i suoi dati perché le prove di isolamento abbiano qualcosa da confrontare anche a mano.
- **RT-2 — Interfaccia di programmazione (§2).** Il caricamento passa dalle rotte pubbliche del servizio o da un
  comando di sviluppo dedicato; **non** scrive direttamente nel database aggirando le regole di dominio.
- **RT-3 — Persistenza (§8).** Nessuna migrazione nuova. I dati di prova non vivono in una migrazione Flyway: una
  migrazione è per la struttura, non per il contenuto.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova; il modulo è abilitato nello stub locale
  dell'abilitazione, come previsto per le app finché l'abilitazione reale non esiste.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile nuovo. I testi dei dati di prova (descrizioni delle spese)
  sono contenuto, non interfaccia, e restano in italiano.
- **RT-6 — Varchi e quota (§6, §7).** Il caricamento dei dati di prova **non** consuma quota reale: le spese
  nascono già negli stati desiderati.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo.
- **RT-8 — Dati personali (§10).** I dati di prova **assomigliano** a dati personali (nomi di collaboratori) ma sono
  inventati. Va scritto in modo esplicito nel file dei dati e nel manifesto non entra nulla: un dato inventato non
  è un trattamento. Gli indirizzi di posta elettronica finti usano il dominio riservato alle prove.
- **RT-9 — Registrazione eventi (§14).** Il caricamento registra un evento con `tenant_id`, `app_id` e conteggio
  degli oggetti creati, senza nomi.
- **RT-10 — Avvio locale (§15).** La scoperta automatica dei servizi deriva tutto dal solo
  `services/notespese/src/main/resources/application.properties`: se venisse voglia di incollare righe in uno
  script di avvio, è un difetto della scoperta, non un passo del lavoro.

## 4. Criteri di accettazione

**CA-1 — Avvio senza cablaggi**
- **Dato** un repository appena clonato
- **Quando** si eseguono `./dev.sh services` e `./app-start.sh`
- **Allora** `notespese` compare nella mappa scoperta e risponde sulla sua porta, senza che nessuno abbia
  modificato uno script

**CA-2 — Dati di prova completi**
- **Dato** lo stack locale avviato · **Quando** si caricano i dati di prova
- **Allora** l'account di prova mostra spese in **tutti** gli stati della macchina, comprese una scartata e una
  respinta, e l'interfaccia non ha nessuna schermata vuota per mancanza di dati

**CA-3 — Idempotenza**
- **Dato** i dati di prova già caricati · **Quando** si esegue di nuovo il caricamento
- **Allora** il numero di spese non cambia e non compaiono doppioni

**CA-4 — Secondo account per l'isolamento**
- **Dato** i dati di prova caricati
- **Quando** si accede con l'utente del secondo account
- **Allora** si vedono solo le spese di quell'account, e sono diverse da quelle del primo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e smoke; l'intera suite prima del commit);
- [ ] prova di **avvio reale** nell'area smoke: l'app parte fuori dal profilo di prova;
- [ ] prova di **isolamento fra account** già coperta dalla storia `0002`; qui si verifica solo che i dati di prova
      nascano su due account;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che userà proprio questi dati di prova come base del
      percorso `[J-NOTESPESE]`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni**: nessuna stringa visibile nuova;
- [ ] **manifesto dei dati**: nessuna voce nuova (i dati di prova sono inventati e va detto);
- [ ] **registro delle decisioni** compilato, con la scelta di due account di prova e di dati deterministici;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali — è il cuore della storia.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001`–`0004` | I dati di prova popolano ciò che quelle storie hanno costruito; senza la quota non si può mostrare il caso «quota esaurita» |

## 7. Fuori ambito

- Le immagini di ricevuta usate come dati di prova sono grafiche finte generate al momento: la lettura automatica
  vera è della storia `0007`, e in locale il fornitore esterno è **sempre simulato**.
- L'avvio in ambiente di prova o di produzione: è della catena di integrazione continua, non di questa storia.

## 8. Punti aperti

- Nessuno.
