import { Illustration, type IllustrationProps } from './Illustration'

export type NotFoundIllustrationProps = Omit<IllustrationProps, 'name' | 'children'>

/**
 * Figura della pagina NON TROVATA (UC 0087): una griglia regolare di tessere con un
 * posto vuoto — e la tessera mancante che non c'è più.
 *
 * Il soggetto dice la cosa giusta senza parole e senza dramma: non «hai sbagliato»,
 * ma «qui non c'è niente». L'accento marca il vuoto, cioè esattamente l'informazione.
 * Niente testo dentro la figura: le lingue del prodotto sono cinque.
 */
export function NotFoundIllustration(props: NotFoundIllustrationProps) {
  return (
    <Illustration name="not-found" {...props}>
      {/* griglia di tessere: tre colonne, due righe — il posto in alto a destra è vuoto */}
      <g className="fill-surface stroke-line" strokeWidth="1.5">
        <rect x="52" y="40" width="40" height="34" rx="10" />
        <rect x="100" y="40" width="40" height="34" rx="10" />
        <rect x="52" y="86" width="40" height="34" rx="10" />
        <rect x="100" y="86" width="40" height="34" rx="10" />
        <rect x="148" y="86" width="40" height="34" rx="10" />
      </g>

      {/* glifi neutri dentro le tessere piene */}
      <g className="fill-line-strong" opacity="0.8">
        <rect x="62" y="52" width="20" height="4" rx="2" />
        <rect x="62" y="60" width="12" height="4" rx="2" />
        <rect x="110" y="52" width="20" height="4" rx="2" />
        <rect x="110" y="60" width="12" height="4" rx="2" />
        <rect x="62" y="98" width="20" height="4" rx="2" />
        <rect x="62" y="106" width="12" height="4" rx="2" />
        <rect x="110" y="98" width="20" height="4" rx="2" />
        <rect x="110" y="106" width="12" height="4" rx="2" />
        <rect x="158" y="98" width="20" height="4" rx="2" />
        <rect x="158" y="106" width="12" height="4" rx="2" />
      </g>

      {/* il posto vuoto: contorno tratteggiato in accento — è il punto focale */}
      <rect
        x="148"
        y="40"
        width="40"
        height="34"
        rx="10"
        fill="none"
        className="stroke-accent"
        strokeWidth="2"
        strokeDasharray="6 6"
        strokeLinecap="round"
      />
    </Illustration>
  )
}
