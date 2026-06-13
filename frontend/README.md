# frontend — React SPA

La **singola** applicazione frontend di appgrove: shell del backoffice + vetrina pubblica +
UI di tutte le app + pannello admin. Un solo `package.json`, un solo build → S3/CloudFront.

## Modular monolith + App Registry

Le app vivono come **moduli lazy-loaded** dentro `src/apps/`. La sidebar è dinamica: lo `shell`
legge l'**App Registry** e monta on-demand il modulo dell'app selezionata (`React.lazy` +
dynamic import). Niente costo operativo da microfrontend, ma estensibilità pronta.

```
src/
├── shell/            # layout, sidebar, routing, auth (Cognito/OIDC), appRegistry.ts
├── apps/             # UI di ogni app come modulo isolato (notes, dashboard, …)
├── admin/            # pannello admin — stessa shell, gated da Cognito group "admin"
└── shared/           # design system, api client, hooks condivisi
```

L'`appRegistry` mappa `app-id → { voce di menù, componente lazy }`:

```ts
export const apps = [
  { id: "notes",     label: "Note",      load: () => import("../apps/notes") },
  { id: "dashboard", label: "Dashboard", load: () => import("../apps/dashboard") },
];
```

## La cucitura per i microfrontend (futuro)

Se un'app dovrà diventare un microfrontend indipendente, cambia **solo** la sua riga nel registry
(da import locale a caricamento di modulo remoto). Shell, auth e routing restano invariati.
Decisione rimandata finché non c'è un motivo concreto.

## Test

```bash
npm test   # test accanto al codice: *.test.ts(x)
```
