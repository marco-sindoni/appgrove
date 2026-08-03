# 0015 — Sospensione e ripresa

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 03 — Rinnovi e scadenze
**Storia**: `0015` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare a cui un iscritto dice «mi opero al ginocchio, torno fra due mesi»
> voglio mettere in pausa il suo abbonamento e fargli recuperare i mesi al rientro
> così da non perderlo come cliente e da non regalargli due mesi per gentilezza mal contabilizzata.

**Contesto.** La sospensione è la funzione che ogni palestra, scuola e centro chiede per prima, ed è anche quella
che nei fogli di calcolo produce il maggior numero di errori: si smette di segnare il pagato, ci si dimentica, e
sei mesi dopo nessuno sa più fino a quando quella persona è coperta. Qui la sospensione è un fatto preciso: il
periodo in corso si **congela**, le scadenze non maturano, e alla ripresa tutte le date si **spostano in avanti**
del tempo effettivamente sospeso. È una storia piccola ma delicata, perché tocca l'aritmetica delle date della
storia `0012` e ne può rompere le assunzioni.

## 2. Requisiti funzionali

1. **RF-1** — Si può sospendere un abbonamento `attivo` o `in_prova`, indicando motivo e — facoltativamente — la
   data prevista di ripresa; lo stato diventa `sospeso`.
2. **RF-2** — Durante la sospensione **non** nascono scadenze nuove e **non** partono avvisi di rinnovo.
3. **RF-3** — Alla ripresa, tutte le date dell'abbonamento (fine del periodo in corso, prossimo rinnovo, ultimo
   giorno utile per disdire) si spostano in avanti dei **giorni effettivamente sospesi**, e la scheda mostra il
   conteggio.
4. **RF-4** — Le scadenze **già emesse e non incassate** restano dovute: la sospensione non cancella un debito
   passato, e l'interfaccia lo dice prima di confermare.
5. **RF-5** — Il cliente vede in ogni momento quanti giorni di sospensione ha accumulato un abbonamento nel
   periodo in corso e nella sua storia.
6. **RF-6** — La sospensione **non** libera quota di appgrove: l'abbonamento resta sorvegliato (storia `0004`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Sospensione e ripresa agiscono su abbonamenti dell'account del token
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/abbonati/v1/abbonamenti/{id}/sospendi` e
  `.../riprendi`; errori in `problem+json` con codice stabile per «stato non sospendibile»; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V10__sospensione.sql`: tabella `sospensione` con `tenant_id`, colonne
  di controllo, inizio, fine, motivo. Il conteggio dei giorni si **deriva** dalle righe, non si tiene in un
  contatore che può divergere.
- **RT-4 — Ciclo di vita (§ storia `0011`).** I passaggi `attivo → sospeso` e `sospeso → attivo` passano dalla
  macchina a stati e ne registrano la cronologia: nessuna scorciatoia.
- **RT-5 — Modulo frontend (§3, §5).** Azione sulla scheda dell'abbonamento con finestra di conferma che dice
  cosa succede alle scadenze già emesse; indicatore dei giorni sospesi; solo token del sistema di design.
- **RT-6 — Cinque lingue (§4).** Motivi, avvisi e messaggi in `en, it, fr, es, de`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `sospendi_abbonamento(abbonamento, motivo) → bozza`, marcato **scrittura irreversibile** — toglie il servizio a
  una persona — con **conferma umana obbligatoria**. La ripresa, che restituisce il servizio, resta scrittura con
  conferma semplice.
- **RT-8 — Dati personali (§10).** Il motivo della sospensione è a testo libero: porta l'avvertenza di non
  inserire dati sanitari («operazione al ginocchio» è un dato sulla salute — è il caso tipico, e va detto sopra
  il campo). La tabella entra nel manifesto, in `exportData` e in `purgeData`.
- **RT-9 — Registrazione eventi (§14).** `abbonamento sospeso`, `abbonamento ripreso (giorni)`, con `tenant_id`,
  `app_id`, `user_id` e correlazione, **senza** il motivo, che può contenere testo libero.

## 4. Criteri di accettazione

**CA-1 — Sospensione che congela**
- **Dato** un abbonamento mensile con periodo dall'1 al 30 e rinnovo il 1° del mese successivo
- **Quando** viene sospeso il giorno 10
- **Allora** al 1° del mese successivo non nasce alcuna scadenza e non parte alcun avviso

**CA-2 — Ripresa che sposta le date**
- **Dato** lo stesso abbonamento ripreso dopo 20 giorni
- **Quando** si guarda la scheda
- **Allora** la fine del periodo è spostata di 20 giorni e il conteggio «20 giorni sospesi» è visibile

**CA-3 — Il debito passato resta**
- **Dato** un abbonamento con una scadenza non incassata · **Quando** lo si sospende
- **Allora** la conferma avvisa che la scadenza resta dovuta, e dopo la sospensione essa è ancora lì

**CA-4 — Stato non sospendibile**
- **Dato** un abbonamento `cessato` · **Quando** si prova a sospenderlo
- **Allora** il rifiuto dice qual è lo stato attuale e quali azioni sono possibili

**CA-5 — La quota non si libera**
- **Dato** un account al tetto · **Quando** sospende dieci abbonamenti
- **Allora** il conteggio della quota non cambia e una nuova sottoscrizione resta rifiutata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sullo spostamento delle date, comprese sospensioni multiple sullo stesso periodo;
      **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account**;
- [ ] **prova end-to-end**: *rimando* — voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml), storia proprietaria `0033`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la tabella `sospensione`, motivo compreso;
- [ ] **registro delle decisioni** compilato: giorni sospesi derivati e non contati, debito passato che resta,
      quota che non si libera;
- [ ] contratto dello strumento `sospendi_abbonamento` dichiarato con conferma obbligatoria;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0011` | i passaggi passano dalla macchina a stati |
| storia `0012` | lo spostamento delle date tocca l'aritmetica dei periodi |

## 7. Fuori ambito

- la sospensione **automatica** per mancato incasso: storia `0022` — stesso stato, causa diversa, e conviene non
  confonderle nella cronologia;
- il tetto massimo di giorni sospendibili per piano: vedi punto aperto;
- la sospensione chiesta dall'abbonato dal portale: non prevista (il portale fa disdetta e cambio piano).

## 8. Punti aperti

**Tetto ai giorni di sospensione.** In molti contratti reali la sospensione è limitata («fino a 30 giorni
all'anno») e oltre quel limite si paga comunque. Modellarlo significa aggiungere un campo al piano (storia
`0006`) e una regola di rifiuto qui. **Proposta**: tenerlo fuori dal primo giro, perché senza dati d'uso non si
sa se il limite serve davvero, e perché il cliente può governarlo a mano. Chiude: lo sviluppatore, con la
direzione di prodotto.

**Il motivo della sospensione è un ingresso per l'articolo 9.** «Infortunio», «gravidanza», «malattia» sono le
parole che l'addetta scriverà, e sono dati sulla salute. L'avvertenza a schermo è un presidio debole. **Proposta
alternativa da valutare**: motivi da elenco chiuso e neutro («temporanea indisponibilità dell'abbonato») senza
campo libero. Chiude: lo sviluppatore — classificazione dei dati personali.
