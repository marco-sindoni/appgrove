# 0003 — Guscio del modulo frontend

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio che ha appena attivato ChatGrove
> voglio vedere l'app nella barra laterale del mio spazio di lavoro, nella mia lingua
> così da capire che l'abbonamento è servito a qualcosa, ancora prima di collegare il canale.

**Contesto.** Dopo lo scaffolding esiste un servizio che risponde ma nessuno lo vede. Questa storia consegna il
**guscio** navigabile: il modulo registrato, le sezioni della barra laterale, le pagine vuote con il loro
stato iniziale, le cinque lingue e il colore-categoria. È il momento giusto per farlo adesso perché tutte le
storie successive aggiungono schermate **dentro** questo guscio: se il guscio arriva dopo, ognuna se lo
reinventa.

## 2. Requisiti funzionali

1. **RF-1** — Il modulo `chat_commerce` è registrato nel registro delle app del backoffice e compare nella
   barra laterale quando registro e abilitazione dicono di sì.
2. **RF-2** — Il manifesto del modulo dichiara `id`, nome, icona, `accentToken: teal`, le sezioni
   (Conversazioni, Catalogo, Ordini, Contatti, Impostazioni) e la metrica di quota.
3. **RF-3** — Ogni sezione ha una pagina che si apre e mostra il proprio **stato vuoto**: titolo,
   spiegazione e un'azione, mai un vicolo cieco.
4. **RF-4** — La pagina d'atterraggio del modulo mostra il consumo della quota `messaggi_template` del mese e
   lo stato della connessione del canale (in questa storia: sempre «non collegato»).
5. **RF-5** — Nessun testo visibile è scritto a mano nei componenti: tutte le stringhe passano dallo
   spazio-nomi `chat_commerce` e sono presenti in `en, it, fr, es, de`.

## 3. Requisiti tecnici

- **RT-1 — Modulo frontend (§3, §5).** Modulo caricato su richiesta in
  `frontend/apps/backoffice/src/modules/chat_commerce/` con `manifest.ts`, aggiunto all'elenco `MODULES` del
  registro. I dati si leggono con il client generato dalla definizione OpenAPI; il modulo non gestisce
  l'autenticazione e non conosce il `tenant_id` se non attraverso il contesto della shell. Solo token del
  sistema di design; funziona in tema chiaro e in tema scuro; nessun colore scritto a mano.
- **RT-2 — Colore-categoria (§5).** `accentToken: teal` nel manifesto del modulo e `category: teal` nel
  listino: devono coincidere.
- **RT-3 — Cinque lingue (§4).** Traduzioni accanto al modulo, in
  `frontend/apps/backoffice/src/modules/chat_commerce/i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi
  `chat_commerce`. La storia non è conclusa se ne manca una.
- **RT-4 — Varchi (§6).** Con account non abilitato il modulo non compare; con abbonamento `canceled` le
  chiamate rispondono `402` e la schermata lo spiega invece di mostrare una pagina rotta.
- **RT-5 — Avvio locale (§15).** Il modulo è abilitato nello stub locale dell'abilitazione, così che sia
  visibile subito dopo l'unione del ramo senza passi manuali.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: la storia non introduce campi che riguardano
  una persona.
- **RT-7 — Prove (§11).** Prove del frontend con Vitest e strato di rete finto; controllo dei tipi
  `tsc --noEmit`; controllo automatico di accessibilità sulle schermate introdotte.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare**
- **Dato** un account abilitato a ChatGrove
- **Quando** l'utente apre il backoffice
- **Allora** vede la voce ChatGrove nella barra laterale, con il colore-categoria verde acqua, e le cinque
  sezioni al suo interno

**CA-2 — Le lingue ci sono tutte**
- **Dato** un utente con l'interfaccia in francese (e poi in tedesco, spagnolo, inglese, italiano)
- **Quando** apre una qualsiasi sezione del modulo
- **Allora** non compare nessuna chiave di traduzione grezza né testo in una lingua diversa da quella scelta

**CA-3 — Primo avvio**
- **Dato** un account appena abilitato, senza canale collegato e senza dati
- **Quando** apre la sezione Conversazioni
- **Allora** vede uno stato vuoto che spiega che serve collegare il canale, con il collegamento alle
  Impostazioni

**CA-4 — Account non abilitato**
- **Dato** un account il cui abbonamento a ChatGrove è `canceled`
- **Quando** tenta di aprire il modulo per collegamento diretto
- **Allora** riceve `402` dalle chiamate e la schermata spiega come riattivare, senza pagine rotte

**CA-5 — Tema chiaro e scuro**
- **Dato** un utente che cambia tema
- **Quando** naviga fra le sezioni
- **Allora** ogni schermata resta leggibile e usa solo i token del sistema di design

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sui componenti introdotti; controllo dei tipi verde;
- [ ] prova di **isolamento fra account**: non applicabile (nessuna risorsa nuova lato servizio);
- [ ] **prova end-to-end**: *rimando* alla storia `0029`, che crea il percorso `[J-CHAT-COMMERCE]` quando
      esiste un flusso completo da percorrere; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica (nessun dato personale nuovo);
- [ ] **registro delle decisioni** compilato, con l'elenco delle sezioni e il perché di quel taglio;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | Serve il servizio e la definizione OpenAPI da cui nasce il client |

## 7. Fuori ambito

- il contenuto vero delle schermate: ognuna arriva con la sua storia di dominio;
- la connessione del canale: la schermata Impostazioni qui è solo un segnaposto, la storia è la `0006`;
- il contatore vero della quota: la storia `0004`.

## 8. Punti aperti

- Nessuno.
