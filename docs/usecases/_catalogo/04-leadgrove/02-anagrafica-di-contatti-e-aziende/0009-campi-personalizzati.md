# 0009 — Campi personalizzati

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 02 — Anagrafica di contatti e aziende
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'azienda con un mestiere particolare
> voglio aggiungere due o tre informazioni che servono solo a me sulle schede
> così da non tenerle in un foglio a parte, ma senza dover progettare un gestionale.

**Contesto.** I campi personalizzati sono l'arma a doppio taglio della categoria: senza, il cliente con una
esigenza specifica se ne va; con troppi, si costruisce la complessità che fa fallire una implementazione su tre
([application-description.md](../application-description.md) §2.5). La posizione di questa storia è di darne
**pochi e tardi**: dopo l'anagrafica essenziale, con un tetto esplicito e senza tipi esotici.

## 2. Requisiti funzionali

1. **RF-1** — Un amministratore dell'account può definire campi aggiuntivi su contatto, azienda o trattativa,
   scegliendo etichetta, tipo (testo breve, numero, data, elenco a scelta singola, sì/no) e obbligatorietà.
2. **RF-2** — Il numero di campi personalizzati per entità è limitato a **dieci**, con un messaggio che spiega
   perché il limite esiste quando lo si raggiunge.
3. **RF-3** — I campi definiti compaiono nel modulo di inserimento e nella scheda, sotto i campi standard, mai
   mescolati a essi.
4. **RF-4** — Un campo può essere disattivato: i valori già inseriti restano leggibili e restano nell'esportazione,
   ma il campo sparisce dai moduli.
5. **RF-5** — Accanto alla definizione di un campo compare l'avviso che i campi personalizzati **non** vanno usati
   per dati sensibili (salute, appartenenza sindacale, convinzioni personali).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Definizioni e valori filtrano per `tenant_id` dal token verificato: un
  campo definito da un account non esiste per gli altri.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST|PATCH /api/sales/v1/custom-fields[/{id}]`; i
  valori viaggiano dentro gli oggetti di trasferimento delle entità ospiti, non su una risorsa a parte; errori in
  `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabelle `custom_field` e `custom_field_value` già create dalla storia 0002; qui si
  aggiunge il vincolo di unicità dell'etichetta per entità e account, e l'indice sui valori a partire da
  `tenant_id`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Impostazioni per la definizione; i campi generati usano i
  componenti senza stile proprio del sistema di design, con validazione dichiarativa lato modulo.
- **RT-5 — Cinque lingue (§4).** Le etichette **dei campi** le scrive il cliente e restano nella sua lingua; tutto
  il resto dell'interfaccia (tipi, messaggi, avviso sui dati sensibili) è presente in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo della metrica `seats`; il tetto di dieci campi è un limite di
  prodotto, non una quota di piano, e non produce `429` ma `422` con spiegazione.
- **RT-7 — Esposizione conversazionale (§12).** Gli strumenti di lettura restituiscono i valori personalizzati solo
  se richiesti espressamente: è la stessa regola di minimizzazione della storia 0034.
- **RT-8 — Dati personali (§10).** `custom_field_value.value` è già dichiarato nel manifesto come campo il cui
  contenuto è **definito dal cliente**: resta annotato `@PersonalData` e la tabella resta in `exportData` e
  `purgeData`. È una delle due vie d'ingresso non presidiate segnalate al §6 della descrizione: l'avviso di RF-5 è
  il presidio proposto.
- **RT-9 — Registrazione eventi (§14).** «Campo definito», «campo disattivato» registrati con identificativi;
  **mai** i valori inseriti.

## 4. Criteri di accettazione

**CA-1 — Definizione e uso**
- **Dato** un amministratore che definisce sul contatto un campo «Codice cliente» di tipo testo breve
- **Quando** un venditore apre il modulo di inserimento di un contatto
- **Allora** il campo compare sotto quelli standard e il valore inserito si rivede nella scheda

**CA-2 — Tetto raggiunto**
- **Dato** un'entità con dieci campi personalizzati attivi
- **Quando** l'amministratore ne definisce un undicesimo
- **Allora** riceve `422` con un messaggio che spiega il limite e come liberarne uno

**CA-3 — Disattivazione non distruttiva**
- **Dato** un campo con valori già inseriti
- **Quando** l'amministratore lo disattiva
- **Allora** sparisce dai moduli ma i valori restano leggibili nella scheda e presenti nell'esportazione

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B` con campi personalizzati diversi
- **Quando** un utente di `A` apre un modulo di inserimento
- **Allora** vede solo i campi di `A`, anche forzando l'identificativo di `B`

**CA-5 — I valori personalizzati escono nell'esportazione dei dati**
- **Dato** un contatto con due valori personalizzati
- **Quando** si esercita il diritto di esportazione
- **Allora** i due valori compaiono, con la loro etichetta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione per tipo e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** su definizioni e valori;
- [ ] **prova end-to-end**: nessun impatto sul percorso `[J-SALES]` — i campi personalizzati non stanno nel
      percorso minimo; la copertura resta alle prove d'integrazione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per l'interfaccia, con la nota che le etichette dei campi
      restano nella lingua del cliente;
- [ ] **manifesto dei dati** verificato: `custom_field_value` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato il perché del tetto di dieci campi;
- [ ] contratto degli **strumenti conversazionali**: regola di minimizzazione annotata;
- [ ] controllo automatico di **accessibilità** verde sui moduli generati;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0006` e `0007` | I campi si aggiungono a entità che devono già esistere |

## 7. Fuori ambito

- i campi calcolati e le formule: fuori perimetro, sono l'inizio del gestionale;
- i campi obbligatori condizionali («obbligatorio solo se…»): non previsti;
- la rilevazione automatica di contenuti sensibili nei valori: è un tema trasversale di piattaforma, non di questa
  app (§6 della descrizione).

## 8. Punti aperti

- **Tetto di dieci campi per entità** — è una proposta di prodotto, non un vincolo tecnico. Chi lo cambia è lo
  sviluppatore; va deciso prima, perché abbassarlo dopo significa togliere qualcosa a clienti che ci stanno sopra.
