# 0031 — Prova a due varianti

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 06 — Rendimento e salute della lista
**Storia**: `0031` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0019`, `0030`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che non sa quale oggetto funzioni meglio
> voglio provare due oggetti su due piccole porzioni del segmento e mandare al resto quello che ha funzionato
> così da smettere di decidere a intuito, ma solo quando la differenza è davvero leggibile.

**Contesto.** È la funzione che nella categoria si chiama «test A/B» e che, sul segmento di una micro-impresa, è
quasi sempre venduta male: con 300 destinatari per porzione la differenza fra due oggetti è indistinguibile dal
caso, ma lo strumento dichiara comunque un vincitore e il cliente ci crede. Questa storia la implementa con
l'obbligo opposto: **dire quando il risultato non significa niente**. La prova arriva dopo il rapporto (storia
0030) perché ne riusa i numeri, e presuppone il controllo pre-volo (storia 0018), che deve verificare **entrambe**
le varianti.

## 2. Requisiti funzionali

1. **RF-1** — Una campagna può avere due varianti che differiscono **solo per l'oggetto**. Il corpo, il mittente,
   il segmento e il canale sono gli stessi: è il fattore singolo che rende la lettura del risultato onesta.
2. **RF-2** — Le due varianti vanno a due porzioni **casuali, disgiunte e di uguale dimensione** del segmento; il
   resto del segmento non riceve nulla finché una persona non sceglie.
3. **RF-3** — Se la porzione risulta più piccola di una dimensione minima (proposta: 500 destinatari per porzione),
   l'app avvisa **prima** di partire che il risultato sarà indicativo e non decisivo, e chiede una conferma
   esplicita; non impedisce la prova, ma non lascia credere che sia una misura.
4. **RF-4** — Alla lettura del risultato l'app dichiara **una** di tre risposte: «ha funzionato meglio la variante
   A», «ha funzionato meglio la variante B», «la differenza non è leggibile». La terza è un esito legittimo e
   scritto in chiaro, non un'assenza di risultato.
5. **RF-5** — L'invio al resto del segmento richiede una **conferma esplicita di una persona**, che sceglie quale
   variante mandare — anche in contrasto con l'esito. Nessuna scelta automatica del vincitore, nessun invio
   programmato che parta da solo.
6. **RF-6** — Gli invii delle due porzioni consumano la metrica `messages_sent` come qualunque altro invio, e
   l'app dice in anticipo quanti invii costerà l'intera prova, porzioni più resto del segmento.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Varianti, porzioni ed esiti filtrano per `tenant_id` preso dal token
  verificato; un `tenant_id` che arrivasse dal corpo della richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/campaigns/v1/campaigns/{id}/variants`,
  `GET /api/campaigns/v1/campaigns/{id}/variants/result` e
  `POST /api/campaigns/v1/campaigns/{id}/variants/{key}/send-rest`; corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__add_campaign_variants.sql` sullo schema `app_campaigns`: tabella
  `campaign_variant` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione
  logica; riferimento della variante su `delivery`, così che ogni invio sappia quale oggetto ha ricevuto.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Prova a due varianti» nella scheda della campagna del modulo
  `campaigns`: composizione dei due oggetti, dimensione delle porzioni con l'avviso di RF-3, schermata di lettura
  del risultato con il pulsante di conferma; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `campaigns` in `en, it, fr, es, de`,
  compresa la frase «la differenza non è leggibile», che è il testo più importante della schermata.
- **RT-6 — Varchi e quota (§6, §7).** Prima di generare gli invii delle porzioni il servizio prenota le unità
  necessarie sulla metrica `messages_sent` (natura `flow`); se la quota residua non basta per **entrambe** le
  porzioni risponde `429` con l'indicazione del rimedio e **non parte niente**: mezza prova non è una prova. La
  funzione appartiene al piano alto del listino proposto (§5 della descrizione), quindi con un piano inferiore
  risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: `statistiche_campagna` (storia 0034)
  restituisce l'esito della prova come dato di lettura, e la scelta del vincitore **non è esposta alla chat**,
  perché produce invii veri verso persone reali. Motivazione da scrivere nel contratto. Livello conversazionale
  non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la variante è un attributo dell'invio, non della
  persona. Il riferimento della variante su `delivery` entra comunque nell'esportazione e nella cancellazione
  insieme alla tabella che lo ospita.
- **RT-9 — Registrazione eventi (§14).** «Prova a due varianti avviata», «esito calcolato» e «variante scelta e
  inviata al resto» registrati con `tenant_id`, `app_id`, `user_id`, identificativo della campagna e
  identificativo di correlazione; nessun dato personale.

## 4. Criteri di accettazione

**CA-1 — Porzioni uguali e disgiunte**
- **Dato** un segmento di 4.000 iscritti inviabili e porzioni da 500
- **Quando** si avvia la prova
- **Allora** partono 1.000 invii, 500 per variante, nessun iscritto riceve entrambe e i restanti 3.000 non
  ricevono nulla

**CA-2 — Avviso sulla porzione troppo piccola**
- **Dato** un segmento di 600 iscritti inviabili
- **Quando** si imposta una prova con porzioni da 300
- **Allora** compare l'avviso che il risultato sarà indicativo e la prova parte solo dopo una conferma esplicita

**CA-3 — Differenza non leggibile**
- **Dato** due varianti con esiti molto vicini su porzioni piccole
- **Quando** si legge il risultato
- **Allora** l'app dichiara «la differenza non è leggibile» e non indica alcun vincitore

**CA-4 — Nessun invio automatico al resto**
- **Dato** una prova conclusa con un esito chiaro
- **Quando** passa il tempo senza che nessuno confermi
- **Allora** al resto del segmento **non parte nulla**: l'invio richiede la scelta di una persona

**CA-5 — Quota insufficiente per entrambe le porzioni**
- **Dato** un account con meno unità residue di quante ne servano per le due porzioni
- **Quando** avvia la prova
- **Allora** riceve `429` con il rimedio e nessun invio viene generato

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede il risultato di una prova di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla ripartizione casuale disgiunta e sulla regola che dichiara la differenza non
      leggibile, e di **integrazione** sulle tre rotte;
- [ ] prova di **isolamento fra account** su varianti e risultati;
- [ ] **prova end-to-end**: rimando — il percorso `[J-CAMPAIGNS]` (storia 0037) non include la prova a due
      varianti, perché richiede volumi che rendono il percorso lento e fragile; voce `da-coprire` nel registro con
      motivo e storia proprietaria `0031`, coperta da prove d'integrazione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la verifica scritta che la variante non è un dato della
      persona;
- [ ] **registro delle decisioni** compilato, con annotati il fattore singolo (solo l'oggetto), la dimensione
      minima della porzione e il divieto di scelta automatica;
- [ ] contratto degli **strumenti conversazionali**: esito in lettura, scelta del vincitore **non** esposta, con
      la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sulla schermata di lettura del risultato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0018` | Il controllo pre-volo deve verificare **entrambe** le varianti prima che parta la prima porzione |
| Storia `0019` | Le porzioni sono invii veri e passano dalla stessa coda |
| Storia `0030` | L'esito si legge dagli stessi numeri del rapporto |

## 7. Fuori ambito

- le varianti di corpo, di mittente e di orario di invio: fuori, perché con più fattori insieme il risultato non
  si può attribuire a nessuno di essi;
- più di due varianti: fuori, per lo stesso motivo e perché moltiplica la dimensione minima del segmento;
- la scelta automatica del vincitore dopo un tempo di attesa: deliberatamente esclusa (RF-5).

## 8. Punti aperti

- **Dimensione minima della porzione e regola di leggibilità della differenza.** La proposta è 500 destinatari per
  porzione e una soglia di scostamento calcolata sul numero di invii; entrambe sono scelte di prodotto che
  cambiano quello che il cliente vede scritto e vanno confermate dallo sviluppatore. Il principio — «se non è
  leggibile, si dice» — invece non è negoziabile.
