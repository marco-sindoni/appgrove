# Template delle email di autenticazione (UC 0018)

**Sorgente unica** dei testi delle email di autenticazione, in inglese e italiano. Questa cartella è letta da **due
programmi diversi**, ed è il motivo per cui esiste:

| Consumatore | Come li ottiene | Quali email rende |
|---|---|---|
| `services/auth` (Java) | copiati dentro l'artefatto a build time (`maven-resources-plugin` → `email-templates/`) | tutte, in locale; l'invito anche in cloud |
| Custom Message Lambda (Python) | inseriti nell'archivio della Lambda da Terraform (`custom_message.tf`) | verifica e reimpostazione password, in cloud |

**Non duplicare questi testi altrove.** Se un testo cambia qui, cambia in entrambi i percorsi senza altri interventi.

## File

- `layout.html` / `layout.txt` — impaginazione condivisa fra le lingue (grafica minima: intestazione col nome del
  prodotto, corpo, un solo collegamento, nessuna immagine remota e **nessun tracciamento** di aperture o click).
- `en.json` / `it.json` — le stringhe per lingua, una voce per messaggio (`verify`, `reset`, `invite`).

## Come vengono resi (identico nei due programmi)

Due passaggi di sostituzione, entrambi su segnaposto `{{nome}}`:

1. le stringhe della lingua vengono risolte contro i **valori dinamici** (`{{role}}`, …);
2. le stringhe risolte vengono inserite nei **buchi dell'impaginazione** (`{{heading}}`, `{{intro}}`,
   `{{actionLabel}}`, `{{actionUrl}}`, `{{fallback}}`, `{{footer}}`, `{{brand}}`).

Nella versione grafica i **valori dinamici sono sempre sottoposti a escape**; l'impaginazione e le stringhe per lingua
sono contenuto fidato del repository. L'escape del collegamento non è un dettaglio: l'indirizzo di verifica contiene
`&` fra i parametri, e senza escape il collegamento arriva rotto.

## Vincolo di Cognito da non violare

Per verifica e reimpostazione in cloud, il codice **non esiste** quando la Lambda compone il messaggio: Cognito passa
il segnaposto `{####}` e lo sostituisce **dopo**. Quel segnaposto deve quindi comparire nel messaggio finale **in
chiaro**, mai dentro una codifica. È il motivo per cui il collegamento usa due parametri distinti
(`?email=…&code={####}`) invece di un token unico. Il test `test_handler.py` presidia esattamente questo.

## Parità fra lingue

`en.json` e `it.json` devono avere **le stesse chiavi** e **gli stessi segnaposto in ogni stringa**. È verificato da un
test automatico (`services/auth`, `EmailTemplatesParityTest`): è la rete che impedisce la divergenza fra lingue.
