# 0022 — Cronologia unificata

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 04 — Attività e storico della relazione
**Storia**: `0022` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0019`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore che sta per richiamare un cliente dopo tre mesi
> voglio una sola linea del tempo con tutto quello che è successo con lui
> così da non dover mettere insieme a mano tre elenchi diversi prima di alzare la cornetta.

**Contesto.** A questo punto dell'epica ci sono tre archivi separati — passaggi di fase (0015), attività (0019),
note (0021) — e ognuno racconta un pezzo. Metterli su una linea sola non è un abbellimento: è la differenza fra
«ho i dati» e «so cosa è successo». È anche ciò che rende possibile lo strumento `summarize_account` (storia 0036),
che senza una cronologia unificata dovrebbe ricomporla ogni volta.

## 2. Requisiti funzionali

1. **RF-1** — La scheda di contatto, azienda e trattativa mostra una cronologia unica che unisce attività
   completate e programmate, note e passaggi di fase, in ordine cronologico inverso.
2. **RF-2** — La cronologia di un'**azienda** comprende anche gli eventi dei suoi contatti e delle sue trattative:
   è la vista che serve prima di una telefonata.
3. **RF-3** — Ogni voce dice cosa è successo, quando e per mano di chi, ed è cliccabile per aprire l'elemento
   d'origine.
4. **RF-4** — La cronologia si carica a scaglioni («mostra altri») e si può filtrare per tipo di evento.
5. **RF-5** — Gli eventi di sistema (creazione della scheda, importazione, arrivo dal modulo web) compaiono anche
   loro, perché «da dove viene questo contatto» è una domanda frequente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni sorgente della cronologia filtra per `tenant_id` dal token
  verificato **prima** di essere unita alle altre.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/sales/v1/timeline` con tipo e identificativo del
  riferimento, filtro per tipo di evento e cursore di scorrimento; errori in `application/problem+json`; OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: la cronologia si compone leggendo le tabelle esistenti. Non
  si costruisce una tabella di eventi duplicata, che diventerebbe subito incoerente.
- **RT-4 — Modulo frontend (§3, §5).** Componente linea del tempo riusato nelle tre schede; solo token del sistema
  di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le descrizioni degli eventi sono **generate dall'interfaccia** a partire da dati
  strutturati (tipo, fase di partenza, fase di arrivo), non da frasi conservate nel database: solo così sono
  traducibili in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota.
- **RT-7 — Esposizione conversazionale (§12).** È la sorgente di `summarize_account` (storia 0036), in sola
  lettura.
- **RT-8 — Dati personali (§10).** Nessuna voce nuova; la cronologia mostra dati già dichiarati, comprese le note
  a testo libero.
- **RT-9 — Registrazione eventi (§14).** Nessun evento applicativo nuovo.

## 4. Criteri di accettazione

**CA-1 — Unione delle sorgenti**
- **Dato** una trattativa con due passaggi di fase, una nota e un'attività completata
- **Quando** si apre la sua cronologia
- **Allora** compaiono quattro voci in ordine cronologico inverso, ognuna con autore e momento

**CA-2 — Cronologia dell'azienda**
- **Dato** un'azienda con due contatti, ognuno con una nota, e una trattativa con un passaggio di fase
- **Quando** si apre la cronologia dell'azienda
- **Allora** compaiono tutte e tre le voci, con l'indicazione dell'elemento a cui si riferiscono

**CA-3 — Traduzione delle descrizioni**
- **Dato** l'interfaccia in tedesco
- **Quando** si apre una cronologia con un passaggio di fase
- **Allora** la descrizione dell'evento è in tedesco, mentre i nomi delle fasi restano quelli scritti dal cliente

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede la cronologia di una scheda di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sull'unione ordinata delle sorgenti e di **integrazione** sullo scorrimento a cursore;
- [ ] prova di **isolamento fra account** su ogni sorgente della cronologia;
- [ ] **prova end-to-end**: nessun impatto aggiuntivo — il percorso `[J-SALES]` verifica la cronologia come effetto
      dei passi precedenti;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per le descrizioni generate;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di non costruire una tabella di eventi
      duplicata;
- [ ] contratto degli **strumenti conversazionali**: sorgente di `summarize_account`;
- [ ] controllo automatico di **accessibilità** verde sulla linea del tempo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0015`, `0019`, `0021` | Sono le tre sorgenti da unire |

## 7. Fuori ambito

- gli eventi provenienti da altre app della suite (fatture emesse, ticket aperti): dipendono dal contratto degli
  eventi condivisi, che non esiste ancora
  ([application-description.md](../application-description.md) §11.4);
- i messaggi di posta elettronica: fuori perimetro (§11.3);
- l'esportazione della cronologia: rientra nell'esportazione dei dati, storia 0027.

## 8. Punti aperti

- Nessuno.
