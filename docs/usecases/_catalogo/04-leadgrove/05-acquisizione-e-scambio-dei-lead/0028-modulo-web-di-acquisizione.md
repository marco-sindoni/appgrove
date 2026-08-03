# 0028 — Modulo web di acquisizione

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 05 — Acquisizione e scambio dei lead
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che riceve richieste dal proprio sito
> voglio che chi compila il modulo finisca direttamente in LeadGrove come trattativa da lavorare
> così da non ricopiare a mano le richieste dalla posta, perdendone una su tre.

**Contesto.** È l'unica delle tre integrazioni «del primo giorno» che possiamo fare senza fornitori esterni
([application-description.md](../application-description.md) §2.4): il modulo è nostro, il sito del cliente lo
incorpora. È anche l'**unica superficie di LeadGrove esposta a Internet senza autenticazione**, quindi la storia
che porta il peso maggiore di sicurezza. La parte legale — informativa e consensi — è così importante da avere una
storia propria (0029): senza quella, il modulo non si pubblica.

## 2. Requisiti funzionali

1. **RF-1** — Un amministratore crea un modulo scegliendo i campi da mostrare fra un insieme fisso (nome, cognome,
   azienda, posta elettronica, telefono, messaggio) e indicando pipeline e fase di destinazione.
2. **RF-2** — Il modulo ottiene un indirizzo pubblico e un frammento da incorporare nel proprio sito; l'aspetto
   segue i token del sistema di design e non richiede al cliente di scrivere stile.
3. **RF-3** — Un invio valido crea un contatto (origine «modulo web») e, se il modulo lo prevede, una trattativa
   nella fase indicata, e compare nell'agenda del responsabile designato.
4. **RF-4** — Il modulo è protetto da un limite di frequenza per indirizzo di rete e da un campo trappola per gli
   invii automatici; gli invii respinti non creano nulla ma vengono contati.
5. **RF-5** — La risposta pubblica non rivela **nulla** dell'account: né quali contatti esistono, né se l'indirizzo
   inviato era già presente, né il nome del responsabile.
6. **RF-6** — Un modulo si può disattivare in ogni momento; da quel momento l'indirizzo pubblico risponde che il
   modulo non è disponibile, senza dire perché.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le rotte autenticate di gestione filtrano per `tenant_id` dal token
  verificato. La rotta **pubblica** non ha token: l'account si ricava dalla **chiave pubblica del modulo**, che è
  un identificativo opaco e non indovinabile, e non da alcun parametro fornito da chi invia.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte autenticate
  `GET|POST|PATCH /api/sales/v1/web-forms[/{id}]`; rotta pubblica
  `POST /api/sales/public/v1/web-forms/{publicKey}/submissions`, esplicitamente fuori dalla catena di
  autenticazione; validazione severa dei campi; errori in `application/problem+json` **senza dettagli utili a chi
  sonda**; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabelle `web_form` e `form_submission` già create dalla storia 0002; indice su
  `(tenant_id, web_form_id, created_at)` e vincolo di unicità sulla chiave pubblica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Impostazioni → Moduli web per la configurazione; la pagina pubblica
  è servita a parte, minima, senza il guscio del backoffice; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'interfaccia di configurazione è in `en, it, fr, es, de`. Il **modulo pubblico**
  è nella lingua scelta dal cliente al momento della creazione, e i suoi testi sono modificabili da lui: chi lo
  compila è un visitatore del suo sito, non un utente della piattaforma.
- **RT-6 — Varchi e quota (§6, §7).** La rotta pubblica non attraversa i varchi di autenticazione, ma **rispetta**
  quello di abilitazione: se l'account non ha un abbonamento attivo, il modulo risponde che non è disponibile e
  non crea nulla. Il limite di frequenza è un presidio a sé, non una quota di piano.
- **RT-7 — Esposizione conversazionale (§12).** La configurazione del modulo non è esposta alla chat: crea una
  superficie pubblica. Gli invii ricevuti si leggono con `list_contacts` filtrando per origine.
- **RT-8 — Dati personali (§10).** `form_submission.payload` è già dichiarata nel manifesto: qui si valorizza. È
  il trattamento più delicato dell'app, perché riguarda persone che **non hanno alcun rapporto** con il cliente:
  vedi la storia 0029 per informativa e consensi, che sono la condizione di liceità. Conservazione proposta del
  grezzo: **24 mesi**, da confermare. Nessun fornitore esterno: il modulo è nostro.
- **RT-9 — Registrazione eventi (§14).** «Invio ricevuto», «invio respinto per limite di frequenza», «invio
  respinto come automatico» con identificativo del modulo e conteggi; **mai** il contenuto inviato, e l'indirizzo
  di rete solo in forma ridotta e per il tempo necessario al presidio.

## 4. Criteri di accettazione

**CA-1 — Invio che diventa lavoro**
- **Dato** un modulo attivo collegato alla pipeline predefinita
- **Quando** un visitatore lo compila con nome e indirizzo di posta validi
- **Allora** viene creato un contatto con origine «modulo web» e una trattativa nella fase indicata, visibile al
  responsabile designato

**CA-2 — Nessuna informazione trapela**
- **Dato** un indirizzo di posta già presente nell'archivio del cliente
- **Quando** un visitatore lo invia
- **Allora** la risposta pubblica è identica a quella di un indirizzo nuovo

**CA-3 — Limite di frequenza**
- **Dato** più invii dallo stesso indirizzo di rete oltre la soglia
- **Quando** arriva quello in eccesso
- **Allora** viene respinto, nulla viene creato e il rifiuto è contato

**CA-4 — Modulo disattivato**
- **Dato** un modulo disattivato
- **Quando** qualcuno invia i dati al suo indirizzo pubblico
- **Allora** riceve una risposta di non disponibilità e nulla viene creato

**CA-5 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled` e un modulo attivo
- **Quando** arriva un invio
- **Allora** nulla viene creato

**CA-6 — Isolamento fra account**
- **Dato** le chiavi pubbliche di due account
- **Quando** si invia alla chiave di `A`
- **Allora** il contatto nasce in `A`, e nessun parametro dell'invio può farlo nascere in `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione pubblica e di **integrazione** sul percorso non autenticato;
- [ ] prova di **isolamento fra account** sulla rotta pubblica, con tentativi di forzare l'account dai parametri;
- [ ] **prova end-to-end**: coprire ora — l'invio dal modulo pubblico e la comparsa della trattativa sono un passo
      del percorso `[J-SALES]` (storia 0037); voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per la configurazione;
- [ ] **manifesto dei dati** aggiornato per `web_form` e `form_submission`, presenti in esportazione e
      cancellazione, con la durata di conservazione annotata come **da confermare**;
- [ ] **registro delle decisioni** compilato, con annotate le scelte di sicurezza della superficie pubblica;
- [ ] contratto degli **strumenti conversazionali**: configurazione non esposta;
- [ ] controllo automatico di **accessibilità** verde sul modulo pubblico, che è la pagina più esposta dell'app;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0007`, `0013` | L'invio crea contatto e trattativa |
| Storia `0029` | Senza informativa e consensi il modulo **non si pubblica**: le due vanno rilasciate insieme |

## 7. Fuori ambito

- i moduli con campi personalizzati liberi: si sceglie da un insieme fisso, per non trasformare il modulo in un
  raccoglitore di qualunque cosa;
- il caricamento di file da parte del visitatore: fuori perimetro;
- l'invio di una conferma automatica a chi compila: è un canale verso l'esterno, punto aperto.

## 8. Punti aperti

- **Conferma automatica al visitatore.** Rispondere «abbiamo ricevuto la tua richiesta» è la cosa che chi compila
  si aspetta, ed è anche un invio di posta elettronica verso l'esterno con un fornitore da scegliere. Decisione di
  prodotto dello sviluppatore.
- **Durata di conservazione degli invii grezzi** — proposta 24 mesi, da confermare nel manifesto.
