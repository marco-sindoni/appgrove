// Dichiarazioni di tipo per `logo.mjs` (JavaScript puro, condiviso fra browser e Node).
// Il modulo è scritto in JavaScript perché lo importano anche strumenti fuori dal
// workspace TypeScript; queste dichiarazioni lo rendono comunque tipato per i consumatori
// TypeScript (il componente `Logo.tsx` e le SPA).

export declare const LOGO_VIEWBOX: string
export declare const LOGO_TILE_RADIUS: number
export declare const LOGO_COMPACT_BELOW: number

export interface LogoPath {
  /** Ruolo del colore del tracciato: `accent` è l'unico ruolo di partenza. */
  role: 'accent'
  /** `true` = tracciato pieno, `false` = tracciato a contorno. */
  fill: boolean
  /** Dati del tracciato SVG. */
  d: string
  /** Se `contrast`, il tracciato si dipinge col colore leggibile sopra l'accento. */
  on?: 'contrast'
  /** Spessore del contorno, per i tracciati non pieni. */
  strokeWidth?: number
  /** `always` = disegnato a ogni dimensione; `full` = solo nel disegno completo. */
  detail: 'always' | 'full'
}

export declare const LOGO_PATHS: readonly LogoPath[]

/** I tracciati da disegnare alla dimensione richiesta (compatto sotto la soglia). */
export declare function logoPathsFor(size?: number): readonly LogoPath[]

export declare function logoMarkSvg(opts?: {
  size?: number
  accent?: string
  contrast?: string
  title?: string
  /** Variante a un colore solo per gli sfondi difficili: il valore È il colore. */
  mono?: string
  /** Forza (o vieta) il disegno compatto; se assente decide la dimensione. */
  compact?: boolean
}): string

export declare function logoLockupSvg(opts?: {
  height?: number
  accent?: string
  contrast?: string
  text?: string
  mono?: string
}): string
