// Island React del selettore lingua (UC 0036, #14 12: "islands React per
// interattività mirata"). Minimale e stilato coi token del design system, senza
// importare i primitivi React del pacchetto (riuso profondo differito a UC 0037).
// Le opzioni arrivano già risolte dal server (l'HTML resta valido senza JS: la
// navigazione avviene al cambio di selezione).

export interface LanguageOption {
  locale: string
  label: string
  href: string
}

interface Props {
  current: string
  options: LanguageOption[]
}

export default function LanguageSwitcher({ current, options }: Props) {
  return (
    <label className="inline-flex items-center gap-2 text-sm text-fg-muted">
      <span className="sr-only">Lingua</span>
      <select
        aria-label="Lingua"
        value={current}
        onChange={(e) => {
          const next = options.find((o) => o.locale === e.target.value)
          if (next) window.location.href = next.href
        }}
        className="rounded-md border border-line bg-surface px-2 py-1 text-fg"
      >
        {options.map((o) => (
          <option key={o.locale} value={o.locale}>
            {o.label}
          </option>
        ))}
      </select>
    </label>
  )
}
