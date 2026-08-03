# Applicazioni escluse dal drill-down — peso normativo

Delle 60 applicazioni del [catalogo](../appgrove-catalogo-applicazioni.md), **venti sono escluse** dal
lavoro di drill-down (epiche, storie, artefatto navigabile). Le restanti **quaranta** hanno la loro
cartella in `docs/usecases/_catalogo/`.

## Il criterio

Non è la complessità del codice: è **quanto costa restare conformi a chi sviluppa da solo**.

Un'applicazione entra in questo elenco quando la conformità richiede una competenza che non si acquisisce
scrivendo software — un albo professionale, una licenza, una certificazione di prodotto, la responsabilità
di custodire dati altrui — oppure quando l'obbligo **cambia più in fretta di quanto una persona sola possa
inseguirlo**. In quei casi il rischio non è scrivere codice sbagliato: è vendere uno strumento su cui il
cliente fa affidamento per un adempimento di legge, e sbagliarlo per conto suo.

L'esclusione **non è un giudizio sul valore di mercato dell'idea**: diverse fra queste hanno la
disponibilità a pagare più alta di tutto il catalogo, ed è proprio la ragione per cui sono difficili. È un
giudizio sulla sostenibilità per un fondatore singolo, oggi.

## Le venti escluse

### Materia riservata, licenza o certificazione (6)

| # | Applicazione | Perché |
|---|---|---|
| 01 | InvoiceGrove | I mandati di fatturazione elettronica cambiano per paese e per anno, e il documento non esiste giuridicamente finché l'autorità non lo accetta: un errore di ciclo di vita è un documento fiscale mancante, non un difetto grafico |
| 10 | PayGrove | Il calcolo di una busta paga conforme è materia riservata a professionisti abilitati e cambia per paese |
| 15 | SignGrove | La firma elettronica qualificata richiede un prestatore di servizi fiduciari qualificato; sotto quel livello si vende valore probatorio che non si è in grado di garantire |
| 23 | CareGrove | Dati sanitari al centro del prodotto — categorie particolari dell'articolo 9 non aggirabili, più i requisiti sulla tenuta della documentazione clinica |
| 49 | ReconGrove | Leggere i conti correnti del cliente richiede di essere o di appoggiarsi a un intermediario autorizzato |
| 52 | SafetyGrove | Se lo strumento sbaglia una scadenza di formazione obbligatoria, la responsabilità che ne deriva ricade sul datore di lavoro: è un rischio che non si trasferisce con una clausola |

### Obbligo che ricade sul fornitore (7)

| # | Applicazione | Perché |
|---|---|---|
| 09 | PeopleGrove | Documenti del rapporto di lavoro e assenze per malattia: categorie particolari, più i limiti al controllo a distanza dei lavoratori |
| 11 | ShiftGrove | Timbrature e turni sono controllo a distanza dell'attività lavorativa: disciplina propria, con procedure di accordo o autorizzazione |
| 27 | FitGrove | Il certificato medico sportivo è un dato sanitario, e la sua verifica è un obbligo del gestore |
| 29 | ShopGrove | La memorizzazione e la trasmissione dei corrispettivi passano da un apparecchio omologato: è certificazione di prodotto, non integrazione |
| 44 | CarbonGrove | Il numero prodotto viene usato in dichiarazioni verso terzi: la metodologia di calcolo è contestabile e va difesa |
| 46 | TourGrove | Chi assembla pacchetti turistici ha obblighi propri, garanzia inclusa: lo strumento entra nel perimetro del suo adempimento |
| 58 | VetGrove | La ricetta elettronica veterinaria e la tracciabilità del farmaco sono flussi normati verso sistemi pubblici |

### Custodia di dati altrui o dipendenza da fornitore fuori dall'Unione (7)

| # | Applicazione | Perché |
|---|---|---|
| 05 | ChatGrove | Il canale di messaggistica appartiene a un fornitore extra-europeo: trasferimento verso paesi terzi, regole del fornitore, e un costo variabile per conversazione che erode il margine |
| 18 | VaultGrove | Vendere conformità altrui significa rispondere della conformità che si è promessa |
| 22 | DineGrove | Igiene degli alimenti più scontrino elettronico: due discipline distinte, entrambe con controlli |
| 35 | TrustGrove | Attestare prontezza a norme che non si è titolati a certificare è una promessa fragile per costruzione |
| 42 | DoorGrove | Contratti di locazione, registrazione e regimi fiscali cambiano per paese e per tipo di contratto |
| 57 | SecretGrove | Custodire le credenziali di altre aziende: una sola falla e il danno è dei clienti, non proprio |
| 60 | AssocGrove | Enti del terzo settore: registro unico, libri sociali e regimi fiscali propri |

## Cosa c'è in questa cartella

Due applicazioni erano **già state scritte per intero** prima che la soglia di esclusione fosse fissata:

- [`01-invoicegrove/`](01-invoicegrove/) — 6 epiche, 30 storie, artefatto navigabile a 9 schermate;
- [`05-chatgrove/`](05-chatgrove/) — 6 epiche, 29 storie, artefatto navigabile.

Sono conservate qui, **fuori dal catalogo attivo**: il lavoro resta consultabile e riusabile se un giorno
la valutazione cambia (per esempio appoggiandosi a un fornitore che si assume l'onere della conformità),
ma non fa parte del piano di costruzione e non va data in pasto alla skill `new-application`.

Due erano state **iniziate e interrotte** alla fissazione della soglia — 09 PeopleGrove e 10 PayGrove: il
materiale parziale è stato rimosso, perché un documento a metà è più dannoso di uno assente.

## Se una di queste rientra

L'esclusione è reversibile e costa poco: si rilancia il drill-down di quella sola applicazione con il kit
d'autore ([`../_kit/`](../_kit/)). La condizione che la farebbe rientrare va detta esplicitamente — di
norma è **la comparsa di un fornitore che si assume l'onere della conformità** (un canale certificato, un
prestatore qualificato, un intermediario autorizzato), che sposta il rischio fuori dal prodotto.
