# frontend — workspace React (npm workspaces)

Workspace del frontend appgrove. Contiene i **package condivisi** e le **due SPA** (entrambe statiche →
S3/CloudFront), come deciso in [docs/03-frontend.md](../docs/03-frontend.md) (dec.13/14):

```
frontend/
├── packages/
│   └── design-system/   # @appgrove/design-system — token, theming, primitivi (UC 0019)
└── apps/                # (in arrivo)
    ├── backoffice/      # SPA cliente + modular monolith + App Registry → app.appgrove.app (UC 0020)
    └── admin/           # console platform-admin separata → admin.appgrove.app (UC 0021)
```

- **Backoffice** = modular monolith React; i moduli app sono **componenti lazy** montati dalla shell
  via App Registry (∩ entitlement). Microfrontend rimandati.
- **Admin** = app **separata** (build/bundle/deploy distinti): i tenant non scaricano mai il codice admin.
- Entrambe condividono `@appgrove/design-system` (estetica identica, zero drift). Altri concerni condivisi
  (api-client, i18n) diventeranno package distinti quando arriva la shell (UC 0020).

## Stack

Vite + React + TypeScript · Tailwind CSS + componenti propri (shadcn/Radix headless) · TanStack Query +
Zustand · React Router · React Hook Form + Zod · client generato da OpenAPI. Dettaglio →
[docs/03-frontend.md](../docs/03-frontend.md).

## Comandi (dalla root `frontend/`)

```bash
npm install            # installa tutti i workspace
npm test               # test di tutti i package (Vitest)
npm run build          # build di tutti i package
npm run typecheck      # typecheck di tutti i package
npm run storybook      # Storybook del design-system (port 6006)
```

## Package: `@appgrove/design-system`

Fonte unica di brand & UI. Espone token (CSS custom properties light/dark + accent), un **Tailwind preset**,
tipografia, icone (Material Symbols), logo e i primitivi React. Consumabile anche **cross-progetto** dalla
vetrina Astro (UC 0036). Uso tipico in una SPA consumer:

```ts
// entry della SPA
import '@appgrove/design-system/tokens.css'
import '@appgrove/design-system/fonts.css'

// tailwind.config.js
import preset from '@appgrove/design-system/preset'
export default { presets: [preset], content: ['./src/**/*.{ts,tsx}'] }
```

```tsx
import { ThemeProvider, Button } from '@appgrove/design-system'
```

Showcase dei token e dei primitivi in light/dark e con accent configurabile: `npm run storybook`.
