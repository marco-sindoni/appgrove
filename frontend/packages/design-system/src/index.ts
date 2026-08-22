// appgrove design system — entry pubblico (UC 0019).
// Nota: i token e i font sono CSS, importati a parte dai consumer:
//   import '@appgrove/design-system/tokens.css'
//   import '@appgrove/design-system/fonts.css'
// e il preset Tailwind: import preset from '@appgrove/design-system/preset'

export { cn } from './lib/cn'

export { Button, buttonVariants, type ButtonProps } from './components/Button'
export { Input, type InputProps } from './components/Input'
export { Card, CardHeader, CardTitle, CardContent } from './components/Card'
export { Badge, badgeVariants, type BadgeProps } from './components/Badge'
export { Switch, type SwitchProps } from './components/Switch'
export { Checkbox, type CheckboxProps } from './components/Checkbox'
export {
  SegmentedControl,
  type SegmentedControlProps,
  type SegmentedOption,
} from './components/SegmentedControl'
export { Icon, type IconProps } from './components/Icon'
export { Logo, type LogoProps } from './components/Logo'
export {
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableHeadCell,
  TableCell,
  type TableRowProps,
} from './components/Table'
export { PageHeader, type PageHeaderProps } from './components/PageHeader'
// Come l'interfaccia dice «non puoi» quando manca il ruolo (UC 0101): presente ma disabilitato, con la
// spiegazione al puntatore e per gli strumenti di assistenza. Il confronto fra ruoli sta in
// `@appgrove/api-client` (appRoleAtLeast): qui arriva già il suo esito.
export { DisabledForRole, type DisabledForRoleProps } from './components/DisabledForRole'

// Illustrazioni on-brand (UC 0087): figure decorative che seguono i token, quindi tema
// chiaro/scuro e accento, senza essere ridisegnate. Regole in ILLUSTRAZIONI.md.
export {
  Illustration,
  ILLUSTRATION_VIEWBOX,
  type IllustrationProps,
} from './illustrations/Illustration'
export { EmptyIllustration, type EmptyIllustrationProps } from './illustrations/EmptyIllustration'
export {
  NotFoundIllustration,
  type NotFoundIllustrationProps,
} from './illustrations/NotFoundIllustration'

export {
  ThemeProvider,
  useTheme,
  type ThemeProviderProps,
} from './theme/ThemeProvider'
export {
  THEMES,
  ACCENTS,
  ACCENT_COLORS,
  DEFAULT_THEME,
  DEFAULT_ACCENT,
  applyTheme,
  type Theme,
  type Accent,
} from './theme/theme'
