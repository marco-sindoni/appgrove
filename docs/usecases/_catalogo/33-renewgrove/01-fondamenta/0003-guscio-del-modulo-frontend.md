# 0003 — Guscio del modulo frontend

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena attivato RenewGrove
> voglio trovarla nella barra laterale, aprirla e capire subito com'è fatta
> così da sapere dove guarderò i clienti a rischio prima ancora che ci sia dentro qualcosa.

**Contesto.** Il servizio risponde (`0001`) ma nel backoffice non compare nulla. Questa storia posa il guscio del
modulo: manifesto, registrazione, le cinque sezioni in cui il lavoro si organizzerà, le traduzioni nelle cinque
lingue e il colore-categoria. È deliberatamente **vuota di dominio** — le schermate si riempiono nelle epiche 02,
03, 04 e 05 — e va fatta adesso per una ragione pratica: una sezione aggiunta a posteriori porta con sé la
tentazione di scrivere una stringa a mano «solo per provare», e quella stringa non arriverà mai in tedesco.
C'è poi una scelta di forma che vale la pena difendere qui: la sezione **Efficacia** compare nel guscio fin da
subito, benché la si riempia soltanto nell'epica 05, perché è l'argomento di vendita dell'app (§5.2 della
[descrizione](../application-description.md)) e nasconderla fino alla fine la farebbe sembrare un ripensamento.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo caricato su richiesta in
   `frontend/apps/backoffice/src/modules/fidelizzazione/`, con un `manifest.ts` che dichiara
   `{ id: 'fidelizzazione', name, icon: 'heart-pulse', accentToken: 'teal', sections, resources, quota, component }`.
2. **RF-2** — Il modulo è aggiunto all'elenco `MODULES` in `frontend/apps/backoffice/src/registry/registry.ts` e
   compare nella barra laterale quando registro **e** abilitazione dicono di sì; se l'account non è abilitato, non
   compare.
3. **RF-3** — Il manifesto dichiara cinque sezioni, in quest'ordine: **Panoramica**, **Rapporti**, **Interventi**,
   **Fonti**, **Efficacia**. Ogni sezione è raggiungibile e mostra uno stato vuoto che dice a che cosa servirà e
   quale passo manca per riempirla.
4. **RF-4** — Lo stato vuoto della Panoramica dice la cosa vera e scomoda di quest'app: **senza almeno una fonte
   collegata RenewGrove non ha nulla da mostrare**, e rimanda alla sezione Fonti.
5. **RF-5** — Nessun testo visibile è scritto a mano nei componenti: tutte le stringhe passano dallo spazio-nomi
   `fidelizzazione` e sono presenti in `en, it, fr, es, de`.

## 3. Requisiti tecnici

- **RT-1 — Modulo frontend (§3).** Applicazione a pagina singola React + TypeScript + Vite; il modulo non gestisce
  l'autenticazione e non conosce il `tenant_id` se non attraverso il contesto che la shell gli passa; i dati, quando
  ci saranno, si leggono con il client generato dalla definizione OpenAPI della storia `0001`.
- **RT-2 — Sistema di design (§5).** Solo token di
  `frontend/packages/design-system/src/tokens/tokens.css`; colore-categoria `teal` in `accentToken`, che **deve
  coincidere** con il campo `category` del listino (storia `0004`); icona `heart-pulse`; tema chiaro e scuro;
  nessun colore scritto a mano e nessuna libreria con un aspetto proprio marcato.
- **RT-3 — Cinque lingue (§4).** Traduzioni in
  `frontend/apps/backoffice/src/modules/fidelizzazione/i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi
  `fidelizzazione`; la storia non è conclusa se ne manca una.
- **RT-4 — Isolamento fra account (§1).** Il modulo non compone mai un identificativo di account nelle chiamate:
  lo prende la shell dal token verificato. Nessuna schermata offre un selettore di account.
- **RT-5 — Varchi e quota (§6).** Il guscio prevede il posto dove l'indicatore di quota `rapporti_sorvegliati`
  verrà mostrato in Panoramica, e la resa dei rifiuti `402` e `429` come messaggio leggibile; il comportamento è
  della storia `0004`.
- **RT-6 — Esposizione conversazionale (§12).** Nessuno strumento introdotto: il guscio non espone funzioni. La
  dipendenza dall'epica di piattaforma non implementata (UC 0061-0063) resta dichiarata per le storie a valle.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo: il guscio non mostra e non chiede dati riferiti a
  persone.
- **RT-8 — Registrazione eventi (§14).** Nessun evento applicativo nuovo lato servizio; la navigazione non si
  registra.
- **RT-9 — Prove (§11).** Vitest + Testing Library con strato di rete finto sulle cinque sezioni e sui rispettivi
  stati vuoti; controllo dei tipi `tsc --noEmit` verde; controllo automatico di accessibilità sulle schermate
  introdotte; prova che ogni chiave di traduzione esiste in tutte e cinque le lingue.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare a chi è abilitato**
- **Dato** un utente di un account abilitato a `fidelizzazione`
- **Quando** apre il backoffice
- **Allora** vede RenewGrove nella barra laterale, con l'icona `heart-pulse` e l'accento `teal`, e può aprire tutte
  e cinque le sezioni

**CA-2 — Il modulo non compare a chi non è abilitato**
- **Dato** un utente di un account **non** abilitato
- **Quando** apre il backoffice
- **Allora** RenewGrove non compare nella barra laterale, e la sua rotta diretta non mostra contenuti

**CA-3 — Lo stato vuoto dice la verità**
- **Dato** un account abilitato senza alcuna fonte collegata
- **Quando** apre la Panoramica
- **Allora** legge che senza almeno una fonte collegata non c'è niente da mostrare, con un rimando alla sezione
  Fonti

**CA-4 — Cinque lingue davvero**
- **Dato** l'insieme delle chiavi dello spazio-nomi `fidelizzazione`
- **Quando** si esegue la prova sulle traduzioni
- **Allora** ogni chiave esiste in `en, it, fr, es, de` e nessuna stringa visibile è scritta a mano in un componente

**CA-5 — Tema chiaro e tema scuro**
- **Dato** le cinque sezioni · **Quando** si passa da tema chiaro a tema scuro
- **Allora** testi e sfondi restano leggibili e nessun colore proviene da un valore scritto a mano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sui componenti delle sezioni e sugli stati vuoti, con strato di rete finto;
- [ ] prova di **isolamento fra account**: *nessun impatto* lato frontend — il modulo non compone identificativi di
      account; l'isolamento è coperto dalle prove del servizio (`0002`);
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-FIDELIZZAZIONE]` nasce con la storia `0030`, che partirà
      dall'apertura del modulo; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato: le cinque sezioni scelte, l'ordine, e il perché Efficacia compare fin
      da subito;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali; il modulo è abilitato nello
      stub locale di abilitazione (verifica d'insieme nella storia `0005`);
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve la definizione OpenAPI da cui si genera il client, e l'identificativo del modulo |
| assegnazione del colore-categoria (punto aperto n. 7 della descrizione) | `accentToken` e `category` del listino devono coincidere: se `teal` cambia, cambiano insieme |

## 7. Fuori ambito

- il contenuto delle sezioni: Rapporti nella storia `0009`, Fonti nelle storie `0008` e `0011`, Interventi
  nell'epica 04, Efficacia nell'epica 05, Panoramica quando ci sarà un punteggio da mostrare (epica 03);
- l'indicatore di quota e i messaggi di rifiuto: storia `0004`;
- la pagina vetrina dell'app e i suoi testi commerciali: sono di piattaforma, non di questo modulo.

## 8. Punti aperti

**Quante sezioni sono troppe.** Cinque è già il massimo che il segmento tollera: le fonti del §2.5 della
descrizione dicono che questo pubblico rifiuta i cruscotti con dodici indicatori. Se in corso d'opera servisse una
sesta sezione, la raccomandazione è di ricavarla dentro una esistente invece di aggiungerla alla barra. Chiude: lo
sviluppatore, quando l'epica 05 avrà preso forma.
