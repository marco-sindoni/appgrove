# 0016 — Documento stampabile e anteprima

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 03 — Redazione dell'offerta
**Storia**: `0016` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta per mandare un'offerta importante
> voglio vedere esattamente il documento che riceverà il cliente, e poterlo scaricare
> così da non mandare per sbaglio qualcosa di diverso da quello che ho controllato.

**Contesto.** Tutti i concorrenti esaminati vendono anzitutto questo: un documento che sembri fatto da un'azienda
seria (ePreventivo vende addirittura i piani a numero di documenti generati, §2.1 della descrizione
dell'applicazione). Per QuoteGrove il documento è anche il **contenuto della versione congelata**: ciò che si
vede in anteprima, ciò che si scarica e ciò che il cliente vedrà sulla pagina pubblica devono essere la stessa
cosa, generata dalla stessa sorgente.

## 2. Requisiti funzionali

1. **RF-1** — Da un preventivo si ottiene un documento stampabile con intestazione dell'azienda, dati del
   destinatario, righe, riepilogo per aliquota, totale, validità e testi del modello.
2. **RF-2** — L'anteprima nell'interfaccia mostra **lo stesso** documento, non una approssimazione.
3. **RF-3** — L'account configura l'intestazione: logo, ragione sociale, recapiti, eventuale numero di iscrizione.
4. **RF-4** — Il documento è reso nella **lingua del destinatario** e con la formattazione dei numeri coerente.
5. **RF-5** — Il documento generato da una versione congelata è **riproducibile**: rigenerarlo domani dà lo stesso
   risultato di oggi.
6. **RF-6** — Il documento non contiene mai informazioni interne: margine, costi, note interne restano dentro.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La generazione filtra per `tenant_id` dal token verificato; il logo e
  l'intestazione appartengono all'account chiamante.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/preventivi/v1/preventivi/{id}/documento` con la
  versione come parametro; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Il documento **non si memorizza**: si rigenera dalla versione congelata. Si
  memorizza solo l'intestazione dell'account. Se in futuro servisse conservarlo, sarebbe una decisione nuova.
- **RT-4 — Modulo frontend (§3, §5).** Anteprima nella schermata del preventivo e pulsante di scarico; solo token
  del sistema di design; tema chiaro e scuro; l'anteprima è leggibile anche su schermo stretto.
- **RT-5 — Cinque lingue (§4).** L'interfaccia dallo spazio-nomi `preventivi` in tutte e cinque le lingue; il
  **documento** segue la lingua del destinatario, che è una cosa diversa e va provata a parte.
- **RT-6 — Dati personali (§10).** Il documento contiene i dati del destinatario: non introduce campi nuovi ma è
  un **modo nuovo di renderli disponibili**. Va detto nel manifesto che l'esportazione dei dati dell'interessato
  comprende i documenti rigenerabili.
- **RT-7 — Registrazione eventi (§14).** `documento generato` con `tenant_id`, `app_id`, `user_id`, correlazione e
  identificativo della versione, senza contenuti.
- **RT-8 — Prove (§11).** Prova che il documento generato due volte dalla stessa versione è identico byte per
  byte, a meno di un eventuale campo di data di generazione dichiarato.

## 4. Criteri di accettazione

**CA-1 — Anteprima uguale al documento**
- **Dato** un preventivo completo · **Quando** si apre l'anteprima e poi si scarica il documento · **Allora** i
  due mostrano gli stessi dati, gli stessi totali e gli stessi testi

**CA-2 — Lingua del destinatario**
- **Dato** un destinatario spagnolo · **Quando** si genera il documento · **Allora** intestazioni e testi sono in
  spagnolo, anche se chi lo genera ha l'interfaccia in italiano

**CA-3 — Riproducibilità**
- **Dato** una versione congelata · **Quando** si rigenera il documento a distanza di tempo · **Allora** il
  risultato è identico

**CA-4 — Niente informazioni interne**
- **Dato** un preventivo con margine e note interne · **Quando** si genera il documento · **Allora** né margine né
  note interne compaiono

**CA-5 — Isolamento fra account**
- **Dato** un preventivo dell'account `A` · **Quando** un utente di `B` ne chiede il documento · **Allora** riceve
  la risposta che riceverebbe per un documento inesistente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** sulla resa dei testi e di **integrazione** sulla generazione, con verifica di
      riproducibilità;
- [ ] prova di **isolamento fra account** sulla risorsa;
- [ ] **prova end-to-end**: rimando alle storie `0029` e `0030` (il documento è ciò che il cliente vede);
- [ ] **traduzioni** dell'interfaccia in tutte e cinque le lingue, più prova della resa del documento in ciascuna;
- [ ] **manifesto dei dati** aggiornato con la nota sull'esportazione dei documenti;
- [ ] **registro delle decisioni** compilato (documento rigenerato e non conservato, motore di resa scelto,
      riproducibilità);
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` | i testi del modello |
| storia `0015` | la versione congelata da cui si rigenera |

## 7. Fuori ambito

- l'archiviazione a norma: è di SignGrove (catalogo 15);
- modelli grafici personalizzabili dall'utente: un solo aspetto curato, configurabile nell'intestazione; il resto
  è rimandato in attesa che qualcuno lo chieda.

## 8. Punti aperti

Nessuno.
