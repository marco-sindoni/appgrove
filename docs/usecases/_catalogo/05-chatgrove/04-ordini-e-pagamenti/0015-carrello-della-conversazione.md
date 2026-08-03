# 0015 — Carrello della conversazione

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 04 — Ordini e pagamenti
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che sta trattando con un cliente
> voglio mettere da parte quello che il cliente sta scegliendo, con quantità e totale che si aggiorna
> così da non fare i conti a mente mentre la conversazione va avanti.

**Contesto.** È il cuore dell'app: il punto in cui una chiacchierata diventa una cosa con un totale. Oggi il
negoziante somma a mente o sulla calcolatrice del telefono, e sbaglia. Il carrello sta **dentro** la
conversazione, non in una schermata separata, perché la conversazione è il luogo dove il negoziante lavora.

## 2. Requisiti funzionali

1. **RF-1** — Ogni conversazione ha al più **un** carrello aperto; si apre da sola alla prima aggiunta.
2. **RF-2** — Si aggiungono righe indicando prodotto o variante e quantità; si modificano le quantità e si
   tolgono le righe.
3. **RF-3** — Il totale si calcola sempre dal listino corrente e si mostra accanto al carrello, con la valuta.
4. **RF-4** — Si può applicare uno **sconto** sul totale, in valore o in percentuale, con una nota che ne
   spiega il motivo.
5. **RF-5** — Il carrello si può svuotare; l'operazione è reversibile solo rifacendolo, e l'app lo dice prima.
6. **RF-6** — Righe che puntano a prodotti nel frattempo ritirati o esauriti sono **segnalate**, e impediscono
   la creazione dell'ordine finché non si risolvono.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `cart` e `cart_line` filtra per
  `tenant_id` preso dal token verificato; il carrello appartiene alla conversazione, che appartiene all'account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/chat_commerce/v1/conversations/{id}/cart`,
  `POST .../cart/lines`, `PUT|DELETE .../cart/lines/{lineId}`, `DELETE .../cart`; corpo validato (quantità
  intera positiva, sconto non superiore al totale); errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V8__carrelli.sql` sullo schema `app_chat_commerce`: tabelle `cart` e
  `cart_line` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
  Gli importi sono in centesimi; il totale **non** si conserva come verità ma si ricalcola, salvo il momento
  della conversione in ordine (storia `0016`).
- **RT-4 — Modulo frontend (§3, §4, §5).** Pannello del carrello accanto al filo della conversazione; tutte le
  stringhe in `en, it, fr, es, de`; solo token del sistema di design; il totale è annunciato alle tecnologie
  assistive quando cambia.
- **RT-5 — Dati personali (§10).** Il carrello non introduce campi che riguardano una persona, ma la tabella
  è **collegata** a un contatto: `cart` e `cart_line` vanno aggiunte a `exportData` e `purgeData` del
  contratto dati, altrimenti la cancellazione lascia indietro l'acquisto di una persona cancellata.
- **RT-6 — Registrazione eventi (§14).** `riga aggiunta al carrello`, `carrello svuotato` con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione, senza nomi né numeri.

## 4. Criteri di accettazione

**CA-1 — Totale che si aggiorna**
- **Dato** una conversazione senza carrello
- **Quando** l'addetto aggiunge due unità di un prodotto da 12,50 € e una di uno da 5,00 €
- **Allora** il carrello mostra tre righe con totale 30,00 €

**CA-2 — Sconto**
- **Dato** il carrello di cui sopra · **Quando** si applica uno sconto del 10 % con nota «cliente abituale»
- **Allora** il totale è 27,00 € e la nota resta visibile

**CA-3 — Prodotto diventato esaurito**
- **Dato** un carrello con una riga il cui prodotto è stato esaurito nel frattempo
- **Quando** l'addetto apre il carrello
- **Allora** la riga è segnalata e la creazione dell'ordine è impedita finché non la si toglie o la si sostituisce

**CA-4 — Un solo carrello aperto**
- **Dato** una conversazione con un carrello aperto · **Quando** si aggiunge un'altra riga
- **Allora** la riga entra nello stesso carrello: non ne nasce un secondo

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** un utente di `A` chiede il carrello di una conversazione di `B`
- **Allora** riceve `404`

**CA-6 — Quantità non valida**
- **Dato** il carrello · **Quando** si indica quantità zero o negativa · **Allora** la richiesta è respinta con
  `400` e il carrello resta invariato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del totale e dello sconto (compresi gli arrotondamenti) e di
      **integrazione** sulle rotte del carrello;
- [ ] prova di **isolamento fra account** sul carrello e sulle sue righe;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`, che percorre catalogo → carrello → ordine → pagamento;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: `cart` e `cart_line` presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta di ricalcolare il totale invece di conservarlo;
- [ ] contratto degli **strumenti conversazionali**: `aggiungi_al_carrello` dichiarato come **scrittura
      reversibile senza conferma** — non esce nulla verso l'esterno e si può svuotare;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0012` | Il carrello punta a prodotti e varianti con la loro disponibilità |
| `0014` | Il carrello nasce dentro la conversazione, dove si inviano già le schede |

## 7. Fuori ambito

- la conversione in ordine: storia `0016`;
- il recupero del carrello abbandonato: storia `0020`;
- il calcolo di imposte e spese di spedizione: fuori dalla prima versione, va detto nella documentazione
  dell'app perché è un'assenza che si nota.

## 8. Punti aperti

- **Imposte.** Un carrello senza imposte è accettabile per un micro-negozio che vende al prezzo esposto, ma
  non ovunque. Aggiungerle cambia il modello dati e chiama in causa regole per giurisdizione: è una decisione
  di prodotto, e va presa prima di vendere in mercati dove il prezzo si espone al netto.
