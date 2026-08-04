# 0033 — Esportazione dei report

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 06 — Report di conversione
**Storia**: `0033` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0027`, `0030`, `0031`, `0032`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve portare i numeri alla riunione del lunedì o al commercialista
> voglio scaricare i rapporti in un file
> così da poterli mettere in una presentazione senza ricopiarli a mano dallo schermo.

**Contesto.** Chiude l'epica dei rapporti con la funzione più banale e più richiesta. Ha però un pregio che
merita di essere sfruttato: a differenza dell'esportazione dei contatti (storia 0027), un rapporto è fatto di
**aggregati**, quindi non porta fuori dati di persone. È il momento giusto per rendere quella differenza esplicita
invece di trattare tutte le esportazioni allo stesso modo.

## 2. Requisiti funzionali

1. **RF-1** — Ognuno dei tre rapporti (imbuto, per responsabile, per origine) si può scaricare in formato tabellare
   con gli stessi filtri applicati sullo schermo.
2. **RF-2** — Il file contiene solo **aggregati**: nessun nome di contatto, nessun recapito, nessun titolo di
   trattativa.
3. **RF-3** — Il rapporto per responsabile è l'eccezione: contiene i **nomi dei membri** dell'account. Il file lo
   dichiara in intestazione e l'esportazione richiede ruolo `owner` o `admin`.
4. **RF-4** — Il file riporta in testa il periodo, i filtri applicati e il momento di generazione: un rapporto
   senza il suo contesto è un numero senza significato.
5. **RF-5** — L'esportazione è registrata nello stesso registro della storia 0027.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il file contiene solo aggregati dell'account del token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Parametro di formato sulle tre rotte dei rapporti, oppure rotta
  dedicata `GET /api/sales/v1/reports/{tipo}/export`; errori in `application/problem+json`; OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; usa `export_log` della storia 0027 con un tipo distinto per
  i rapporti.
- **RT-4 — Modulo frontend (§3, §5).** Azione «Scarica» su ogni rapporto; **nessun avviso sui dati personali** per
  imbuto e origini, perché non ne contengono; avviso presente sul rapporto per responsabile; solo token del
  sistema di design.
- **RT-5 — Cinque lingue (§4).** Intestazioni delle colonne e riga di contesto in `en, it, fr, es, de`, con numeri
  e date formattati secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Con abbonamento `canceled` risponde `402`: è una
  comodità, non un diritto.
- **RT-7 — Esposizione conversazionale (§12).** Non esposta: `conversion_report` (storia 0034) restituisce già i
  numeri in forma leggibile da un programma, quindi un file sarebbe un passaggio inutile.
- **RT-8 — Dati personali (§10).** Due casi distinti e da provare separatamente: imbuto e origini **non**
  contengono dati personali; per responsabile **sì** (nomi dei membri). Nessuna voce nuova nel manifesto, ma
  l'uscita del rapporto per responsabile va tracciata nel registro delle esportazioni.
- **RT-9 — Registrazione eventi (§14).** «Rapporto esportato» con tipo, periodo e autore.

## 4. Criteri di accettazione

**CA-1 — File con contesto**
- **Dato** il rapporto dell'imbuto filtrato sul trimestre e su una pipeline
- **Quando** si scarica
- **Allora** il file riporta in testa periodo, filtri e momento, e sotto gli stessi numeri dello schermo

**CA-2 — Nessun dato personale negli aggregati**
- **Dato** i file di imbuto e origini
- **Quando** si ispezionano
- **Allora** non contengono nomi, recapiti né titoli di trattativa

**CA-3 — Il rapporto per responsabile lo dichiara**
- **Dato** il rapporto per responsabile
- **Quando** un amministratore lo scarica
- **Allora** il file dichiara in intestazione che contiene nomi di persone, e l'esportazione è registrata

**CA-4 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** scarica il rapporto per responsabile
- **Allora** riceve `403`

**CA-5 — Isolamento fra account**
- **Dato** due account
- **Quando** un utente di `A` scarica
- **Allora** i numeri comprendono solo `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione del file e di **integrazione** sulle tre esportazioni;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli**;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo; coperta da prove d'integrazione, con il motivo nel
      registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificata la distinzione fra rapporti con e senza dati di
      persone;
- [ ] **registro delle decisioni** compilato, con annotata quella distinzione;
- [ ] contratto degli **strumenti conversazionali**: non esposta, con la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sulle azioni di scaricamento;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0030`, `0031`, `0032` | Sono i tre rapporti da esportare |
| Storia `0027` | Riusa il registro delle esportazioni |

## 7. Fuori ambito

- il formato per presentazioni o documenti stampabili: fuori perimetro, il formato tabellare basta;
- l'invio programmato del rapporto per posta elettronica: è un canale verso l'esterno;
- i rapporti costruiti dall'utente: sarebbe un generatore di rapporti, cioè un altro prodotto.

## 8. Punti aperti

- Nessuno.
