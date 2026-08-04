# 0018 — Bozza di risposta assistita

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 04 — Risposte e recensioni negative
**Storia**: `0018` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che alle nove di sera trova una recensione da tre stelle e non sa come rispondere
> voglio una bozza di risposta già scritta, che posso correggere
> così da rispondere in due minuti invece che rimandare per una settimana.

**Contesto.** È la funzione che il catalogo chiama «risposte assistite» ed è la più facile da fare male. Farla
male significa due cose. La prima: **pubblicare da sola**. Una risposta è un atto pubblico a nome dell'azienda —
la conferma umana non è un'opzione da spuntare (storia 0019). La seconda: **produrre testi tutti uguali**, che i
lettori riconoscono a colpo d'occhio e che fanno più danno del silenzio. La bozza deve partire dal contenuto della
recensione, non da un modello con il nome sostituito.

C'è anche una cosa che questa funzione **non deve fare mai**, ed è scritta nella descrizione §1: l'assistente
scrive **risposte**, non recensioni. Non esiste, e non esisterà, una funzione che generi il testo di una
recensione.

## 2. Requisiti funzionali

1. **RF-1** — Dalla scheda di una recensione si chiede una bozza di risposta. La bozza si salva in stato `bozza`,
   con l'indicazione che è stata proposta dall'assistente e non ancora rivista.
2. **RF-2** — La bozza si può richiedere con un tono fra pochi dichiarati (proposta: cortese, sintetico, caloroso)
   e si può rigenerare; ogni rigenerazione sostituisce la bozza, non ne accumula dieci.
3. **RF-3** — La bozza tiene conto del contenuto della recensione: se il cliente lamenta l'attesa, la risposta
   parla dell'attesa. Se la recensione non ha testo, la bozza è un ringraziamento breve e lo dice.
4. **RF-4** — La bozza **non promette rimedi** (sconti, omaggi, rimborsi) di propria iniziativa: proporre un
   vantaggio in risposta pubblica a una recensione è vicino all'incentivo e non è una decisione che un
   generatore possa prendere per un'azienda. Se il titolare vuole scriverlo, lo scrive lui.
5. **RF-5** — Una bozza modificata a mano resta modificata: una rigenerazione successiva chiede conferma prima di
   sovrascrivere il lavoro della persona.
6. **RF-6** — Il testo generato passa un controllo prima di poter essere pubblicato: non deve contenere dati
   personali del cliente che non siano già nella recensione (storia 0019).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `risposta` filtra per `tenant_id` preso dal
  token verificato; la generazione riceve solo la recensione dell'account che la chiede.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/recensioni/v1/recensioni/{id}/bozza-risposta` e
  `PUT /api/recensioni/v1/risposte/{id}`; errori in `application/problem+json` con un codice per «servizio di
  generazione non disponibile»; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `risposta` (storia 0002) con stato, autore della bozza, autore della
  modifica e momento; una recensione ha al massimo una risposta viva.
- **RT-4 — Modulo frontend (§3, §5).** Scheda della recensione: riquadro della risposta con il pulsante «proponi
  una bozza», scelta del tono, area di testo modificabile, indicazione visibile che si tratta di una bozza non
  pubblicata. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Interfaccia in `en, it, fr, es, de`. La **bozza** si genera nella lingua della
  recensione, non in quella dell'interfaccia: si risponde a chi ha scritto, non a sé stessi. Se la lingua della
  recensione non è riconosciuta, si usa quella predefinita della sede e lo si dice.
- **RT-6 — Varchi e quota (§6, §7).** La generazione richiede abbonamento in uno stato che dà accesso; `402` con
  `canceled`. **Non consuma la quota `sedi_monitorate`**: la metrica dell'app è una sola (descrizione §3). Se la
  generazione avesse un costo variabile per chiamata, il presidio è un limite di frequenza tecnico dichiarato, non
  una seconda metrica commerciale.
- **RT-7 — Esposizione conversazionale (§12).** È esattamente lo strumento `prepara_risposta` (storia 0028),
  marcato **scrittura**, che produce una bozza e **non pubblica niente**. Il contratto vive dentro il servizio; il
  server conversazionale è di piattaforma e non ancora implementato (casi d'uso 0061-0063).
- **RT-8 — Dati personali (§10).** **Voce nuova nel manifesto**: `risposta.testo` — testo aziendale che però può
  citare dati del cliente. Va dichiarato in italiano e inglese, con la finalità e la conservazione. Il testo della
  recensione esce verso il servizio di generazione: è un **fornitore esterno** e va nell'elenco dei fornitori e
  nell'informativa. ⚠️ Se la recensione contenesse dati sulla salute (descrizione §6), quel dato uscirebbe dal
  sistema: è un punto che va valutato **prima** di scegliere il fornitore, e insieme alla valutazione d'impatto.
- **RT-9 — Registrazione eventi (§14).** `bozza generata`, `bozza modificata`, `generazione fallita`, con
  `tenant_id`, `app_id`, `user_id`, identificativo della recensione e identificativo di correlazione. **Mai** il
  testo della recensione o della risposta nei registri.

## 4. Criteri di accettazione

**CA-1 — La bozza nasce dalla recensione**
- **Dato** una recensione da due stelle che lamenta l'attesa al banco
- **Quando** si chiede una bozza
- **Allora** la bozza fa riferimento all'attesa, è in stato `bozza` e **non è pubblicata da nessuna parte**

**CA-2 — Recensione senza testo**
- **Dato** una recensione con solo il voto
- **Quando** si chiede una bozza
- **Allora** si ottiene un ringraziamento breve, senza inventare contenuti che il cliente non ha scritto

**CA-3 — Nessuna promessa di vantaggi**
- **Dato** una recensione negativa
- **Quando** si chiede una bozza
- **Allora** il testo non offre sconti, omaggi né rimborsi

**CA-4 — Il lavoro della persona non si perde**
- **Dato** una bozza modificata a mano
- **Quando** si chiede di rigenerarla
- **Allora** l'app chiede conferma e spiega che il testo scritto verrebbe sostituito

**CA-5 — Lingua della risposta**
- **Dato** una recensione scritta in tedesco, con l'interfaccia in italiano
- **Quando** si chiede una bozza
- **Allora** la bozza è in tedesco

**CA-6 — Isolamento fra account**
- **Dato** due account con recensioni
- **Quando** un utente di `A` chiede una bozza per una recensione di `B`
- **Allora** riceve `404` e nessuna generazione parte

**CA-7 — Servizio di generazione non disponibile**
- **Dato** il servizio di generazione irraggiungibile
- **Quando** si chiede una bozza
- **Allora** l'app lo dice con un messaggio comprensibile e la risposta si può comunque scrivere a mano

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla scelta della lingua e sulla gestione della bozza modificata; di **integrazione**
      sulle rotte con il servizio di generazione **simulato** (nessuna chiamata reale nelle prove);
- [ ] prova di **isolamento fra account** sulla generazione e sulle risposte;
- [ ] **prova end-to-end**: *coprire ora* il passo «chiedo una bozza di risposta» nel percorso `[J-RECENSIONI]`,
      con generazione simulata, e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con `risposta.testo` e con il **fornitore della generazione** nell'elenco
      dei fornitori;
- [ ] **registro delle decisioni** compilato, con la scelta del fornitore, la valutazione sul testo che esce e il
      divieto di promettere vantaggi;
- [ ] contratto degli **strumenti conversazionali**: `prepara_risposta`, scrittura, bozza, conferma umana.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | serve la scheda della recensione da cui si parte |
| **scelta del fornitore della generazione** | è una decisione di piattaforma con effetti sui dati personali: non la fa questa storia |

## 7. Fuori ambito

- la pubblicazione — storia 0019: qui la bozza resta dentro l'app;
- la generazione del testo dell'**invito** — storia 0013, con il suo controllo;
- la generazione di **recensioni**: non esiste e non esisterà (descrizione §1, rifiuto 7).

## 8. Punti aperti

- **Il testo della recensione esce verso un fornitore.** Con la possibilità che contenga dati sulla salute
  (descrizione §6), la scelta del fornitore e la sua collocazione geografica non sono dettagli. Va deciso insieme
  alla valutazione d'impatto, prima di scrivere codice.
- **Costo variabile della generazione.** Se il fornitore si paga a chiamata, il limite di frequenza tecnico va
  dimensionato: non diventa una seconda metrica di listino, ma nemmeno può essere illimitato.
- **Rischio di risposte tutte uguali**: è il modo più comune di rovinare questa funzione. Serve una verifica su
  testi veri prima del rilascio, non solo prove automatiche.
</content>
