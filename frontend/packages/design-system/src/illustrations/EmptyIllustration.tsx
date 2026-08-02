import { Illustration, type IllustrationProps } from './Illustration'

export type EmptyIllustrationProps = Omit<IllustrationProps, 'name' | 'children'>

/**
 * Figura degli STATI VUOTI (UC 0087): un contenitore aperto e ancora da riempire, con
 * una tessera in arrivo.
 *
 * Il soggetto è scelto per non mentire: uno stato vuoto non è un errore, è un inizio.
 * L'accento sta su un elemento solo — la tessera che sta per entrare — così l'occhio
 * capisce dove guarda il seguito del discorso (il pulsante che sta accanto al testo).
 */
export function EmptyIllustration(props: EmptyIllustrationProps) {
  return (
    <Illustration name="empty" {...props}>
      {/* piano d'appoggio */}
      <line
        x1="52"
        y1="122"
        x2="188"
        y2="122"
        className="stroke-line"
        strokeWidth="1.5"
        strokeLinecap="round"
      />

      {/* contenitore: pieno neutro e contorno aperto in alto — l'apertura si legge
          perché il tratto in cima semplicemente non c'è (stile piatto, niente prospettiva) */}
      <rect x="74" y="72" width="92" height="50" rx="10" className="fill-surface-3" />
      <path
        d="M74 72v40a10 10 0 0 0 10 10h72a10 10 0 0 0 10-10V72"
        fill="none"
        className="stroke-line"
        strokeWidth="2"
        strokeLinecap="round"
      />
      {/* due tacche: il bordo del contenitore, non una terza dimensione */}
      <path
        d="M74 84h10M156 84h10"
        className="stroke-line"
        strokeWidth="2"
        strokeLinecap="round"
      />

      {/* tessera in arrivo: l'unico elemento in accento (il logo non entra mai in una
          figura — è un segno, non un'illustrazione: vedi ILLUSTRAZIONI.md) */}
      <rect x="104" y="34" width="32" height="32" rx="9" className="fill-accent" />
      <rect x="112" y="44" width="16" height="3.5" rx="1.75" className="fill-accent-contrast" />
      <rect x="112" y="52" width="10" height="3.5" rx="1.75" className="fill-accent-contrast" />

      {/* traiettoria di caduta */}
      <path
        d="M120 24v-8M99 30l-5-6M141 30l5-6"
        className="stroke-accent"
        strokeWidth="2"
        strokeLinecap="round"
        opacity="0.35"
      />
    </Illustration>
  )
}
