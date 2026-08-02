// Dichiarazioni di tipo per `assets.mjs` — i derivati del logo (UC 0087).
// Il modulo è JavaScript puro perché lo eseguono anche script fuori dal workspace
// TypeScript; queste dichiarazioni lo rendono tipato per i consumatori TypeScript.

export declare const FAVICON_SIZE: number
export declare const APP_ICON_SIZE: number
export declare const APPLE_TOUCH_SIZE: number
export declare const OG_WIDTH: number
export declare const OG_HEIGHT: number
export declare const OG_PAYOFF: string

export declare function faviconSvg(): string
export declare function appIconSvg(opts?: { size?: number }): string
export declare function platformOgSvg(): string

export interface SvgAsset {
  /** Percorso di destinazione, relativo alla radice del monorepo. */
  path: string
  /** Contenuto generato dal disegno unico. */
  content: string
}

export interface RasterAsset {
  path: string
  /** Sorgente vettoriale da rasterizzare. */
  svg: string
  width: number
  height: number
}

export declare function svgAssets(): SvgAsset[]
export declare function rasterAssets(): RasterAsset[]
