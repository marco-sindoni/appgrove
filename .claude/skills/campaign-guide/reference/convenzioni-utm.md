# Convenzioni UTM — attribuzione cookieless con Plausible

Gli **UTM** sono i parametri che si aggiungono all'URL di destinazione (`?utm_source=…&utm_medium=…`) e che
**Plausible** legge per raggruppare le visite per sorgente/mezzo/campagna. Sono l'**unica** via di attribuzione
in una postura cookieless (niente pixel): senza UTM coerenti, il traffico a pagamento risulta indistinguibile.

Questa convenzione è **posseduta dalla skill** (lato campagna). I **goal** di Plausible — definiti in **UC 0039**
— devono combaciare con questi valori: il coordinamento è tracciato come punto aperto in UC 0039 (non duplicato qui).

## I cinque parametri

| Parametro | Significato | Valori ammessi (esempi) |
|---|---|---|
| `utm_source` | La **piattaforma/sorgente** | `google`, `meta` |
| `utm_medium` | Il **tipo di traffico** | `cpc` (Google Search a pagamento), `paid-social` (Meta) |
| `utm_campaign` | La **campagna**, nome stabile | `<app>-<obiettivo>-<mese-anno>` es. `fatture-traffico-2026-08` |
| `utm_content` | La **variante** (annuncio/creatività), per distinguere gli A/B | `titolo-a`, `titolo-b`, `hero-1` |
| `utm_term` | La **parola chiave** (solo ricerca) o il **pubblico** (Meta) | `partita-iva-forfettaria`, `pubblico-lookalike` |

## Regole di scrittura (perché Plausible non frammenti i dati)
- **Solo** lettere minuscole `a-z`, cifre `0-9` e **trattino** `-` come separatore. Niente spazi, maiuscole,
  accenti o underscore: Plausible distingue maiuscole/minuscole, quindi `Google` e `google` diventerebbero due
  righe diverse. Normalizza sempre.
- **Stabilità**: fissa i valori **prima** di avviare e **non cambiarli** a campagna in corso — cambiarli spezza
  la serie storica.
- `utm_campaign` **parlante e stabile**: `<app>-<obiettivo>-<mese-anno>` rende leggibile il report.
- `utm_content` **solo** per distinguere varianti che vuoi confrontare (test A/B dei titoli/creatività).
- `utm_term` **solo** dove ha senso: parola chiave nella ricerca, pubblico su Meta. Omettilo se non serve.

## Esempi pronti (URL già etichettati)

**Google Search — obiettivo Traffico verso la landing di `fatture`:**
```
https://appgrove.app/it/app/fatture?utm_source=google&utm_medium=cpc&utm_campaign=fatture-traffico-2026-08&utm_content=titolo-a&utm_term=fattura-elettronica-forfettari
```

**Meta — obiettivo Traffico, variante creativa "hero-1":**
```
https://appgrove.app/it/app/fatture?utm_source=meta&utm_medium=paid-social&utm_campaign=fatture-traffico-2026-08&utm_content=hero-1
```

> Per i **Lead Form native** i lead restano sulla piattaforma e non c'è un URL di destinazione sul sito: in quel
> caso gli UTM non si applicano al sito, e l'attribuzione è il **click nativo** della piattaforma. Usa comunque un
> `utm_campaign` coerente nel nome della campagna sulla piattaforma, per leggere i report in modo uniforme.

## Coordinamento con i goal Plausible (UC 0039)
I goal che Plausible conta (es. iscrizione newsletter, click verso l'app) sono impostati in **UC 0039**. Perché
il report campagna sia leggibile, i nomi dei goal e questi valori UTM devono essere pensati insieme. Questo è un
**punto di contatto** con UC 0039, dove è tracciato: quando i goal verranno definiti, allinea i due schemi.
