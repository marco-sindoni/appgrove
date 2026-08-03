# 0004 — Abbonamento e quota

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha scelto un piano
> voglio che l'app mi lasci lavorare fin dove arriva il mio piano e mi dica con chiarezza quando l'ho esaurito
> così da non scoprire il limite nel momento sbagliato e da sapere subito come si sblocca.

**Contesto.** Il modulo si vede e le tabelle esistono, ma nulla impedisce a un account del piano gratuito di portare
dentro diecimila crediti. Questa storia collega l'app alla catena dei varchi della piattaforma e applica la metrica
`crediti_monitorati`, di natura **a giacenza**: il limite riguarda quanti crediti sono sotto sorveglianza **adesso**,
non quanti se ne sono aperti nel mese. Si fa prima delle epiche di dominio perché ogni storia che crea un credito
dovrà passare da qui.

## 2. Requisiti funzionali

1. **RF-1** — Un credito è «monitorato» quando il suo stato è diverso da `incassato` e `stralciato` e non è cancellato
   logicamente; il conteggio dei monitorati è la metrica `crediti_monitorati`.
2. **RF-2** — Prima di portare dentro un nuovo credito il servizio verifica il tetto del piano: se è raggiunto,
   risponde `429` e non crea nulla.
3. **RF-3** — Il messaggio di rifiuto dice tre cose: che il tetto è stato raggiunto, che cosa non si può più fare, come
   si rimedia (chiudere crediti già incassati oppure passare a un piano superiore).
4. **RF-4** — Chiudere un credito (`incassato` o `stralciato`) **libera** immediatamente una unità della metrica.
5. **RF-5** — L'interfaccia mostra il consumo in ogni schermata da cui si può creare un credito, e l'avviso compare
   **prima** del modulo di inserimento, non dopo il salvataggio.
6. **RF-6** — Con abbonamento in stato `past_due` l'app resta accessibile; con `canceled` risponde `402`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il conteggio dei monitorati è per account, calcolato con filtro
  `WHERE tenant_id = :tid` sul `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/crediti/v1/quota` che restituisce metrica, uso attuale
  e tetto; errori in `application/problem+json` con il tipo di problema che distingue `402` da `429`.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. Serve un indice che renda economico il conteggio dei monitorati
  per account, perché è una lettura su ogni creazione.
- **RT-4 — Modulo frontend (§3, §5).** Barra di consumo e avviso nella *Panoramica* e nelle schermate di creazione;
  solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I messaggi di quota raggiunta e di abbonamento non attivo passano dallo spazio-nomi
  `crediti` e sono presenti in tutte e cinque le lingue: sono i testi che l'utente legge nel momento peggiore, tradurli
  male è peggio che non tradurli.
- **RT-6 — Varchi e quota (§6, §7).** Si applica la catena completa: `401` senza token valido, `403` ad app spenta dalla
  piattaforma, `402` senza abilitazione derivata dall'abbonamento, `403` a ruolo insufficiente, `429` a quota esaurita.
  L'abilitazione si legge dalla **proiezione locale** alimentata a eventi: nessuna chiamata sincrona all'app centrale
  sul percorso caldo. La storia non fissa prezzi: consuma il tetto pubblicato dall'abilitazione per la metrica
  `crediti_monitorati` (natura `stock`).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo, ma si stabilisce la regola che vale per tutti:
  una chiamata dell'assistente attraversa gli **stessi** varchi di una chiamata dall'interfaccia (dipendenza UC 0064,
  non implementato).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-9 — Registrazione eventi (§14).** L'evento «creazione respinta per quota» è registrato con `tenant_id`,
  `app_id`, `user_id`, metrica, uso e tetto, senza dati personali.
- **RT-10 — Diritti dell'interessato sempre accessibili (§13).** Esportazione e cancellazione restano raggiungibili
  anche con app disabilitata o abbonamento scaduto.

## 4. Criteri di accettazione

**CA-1 — Dentro il tetto**
- **Dato** un account sul piano con tetto 150 e 149 crediti monitorati
- **Quando** registra un nuovo credito
- **Allora** il credito è creato e il consumo passa a 150 di 150

**CA-2 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `crediti_monitorati`
- **Quando** tenta di registrare un altro credito
- **Allora** riceve `429`, un messaggio che spiega come rimediare, e **nulla viene creato**

**CA-3 — Chiudere libera la quota**
- **Dato** lo stesso account al tetto
- **Quando** un credito viene portato a `incassato`
- **Allora** il consumo scende di uno e la creazione successiva riesce

**CA-4 — Abbonamento in tolleranza**
- **Dato** un abbonamento in stato `past_due` · **Quando** l'utente apre l'app e registra un credito · **Allora**
  l'operazione riesce, perché `past_due` dà ancora accesso

**CA-5 — Abbonamento disdetto**
- **Dato** un abbonamento in stato `canceled` e il periodo pagato concluso
- **Quando** l'utente chiama una qualsiasi rotta dell'app
- **Allora** riceve `402`, ma l'esportazione dei propri dati resta accessibile

**CA-6 — Isolamento del conteggio**
- **Dato** due account `A` e `B` con crediti propri · **Quando** `A` è al tetto e `B` no · **Allora** `B` continua a
  creare crediti senza impedimenti, e nessun conteggio dell'uno influenza l'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sul conteggio dei monitorati e di **integrazione** sul rifiuto `429` con database effimero;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulle rotte introdotte;
- [ ] **prova end-to-end**: *rimando* — il passo «quota esaurita» entra nel percorso `[J-CREDITI]` con la storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sulla definizione di «credito monitorato»;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta, ma la regola dei varchi è annotata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Serve la tabella `credito` da contare |
| storia `0003` | Serve una schermata dove mostrare il consumo |
| Listino `crediti.yaml` deciso dallo sviluppatore | I tetti li pubblica l'abilitazione, non li scrive l'app |

## 7. Fuori ambito

- Il passaggio di piano e la disdetta: sono della sezione Fatturazione della piattaforma, non dell'app.
- La deroga temporanea al tetto per il caricamento iniziale: è una estensione della console di amministrazione, vedi
  [estensioni-admin.md](../estensioni-admin.md).

## 8. Punti aperti

Se lo sviluppatore decidesse di comprendere nel canone i canali a costo variabile (messaggio breve, messaggistica),
servirebbe un **secondo** limite; ma la metrica di quota è una sola per costruzione, quindi quel limite sarebbe una
funzionalità del piano e non una quota sorvegliata. Punto aperto n. 5 del documento capofila §11.
