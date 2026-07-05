import { cn } from '@appgrove/design-system'

/* Tavolozza ciclica del mockup per gli avatar tenant/utente (assegnazione stabile per nome). */
const AVATAR_TINTS = [
  'bg-cat-blue',
  'bg-cat-violet',
  'bg-cat-teal',
  'bg-cat-amber',
  'bg-accent',
  'bg-cat-green',
  'bg-cat-red',
]

function hashOf(text: string): number {
  let hash = 0
  for (let i = 0; i < text.length; i += 1) hash = (hash * 31 + text.charCodeAt(i)) | 0
  return Math.abs(hash)
}

/** Avatar quadrato 28px con le iniziali del tenant/utente, colore stabile per nome (mockup tabelle admin). */
export function TenantAvatar({ name, className }: { name?: string; className?: string }) {
  const label = name?.trim() || '—'
  const parts = label.split(/\s+/).filter(Boolean)
  const initials =
    parts.length >= 2 ? (parts[0][0] + parts[1][0]).toUpperCase() : label.slice(0, 2).toUpperCase()

  return (
    <span
      aria-hidden
      className={cn(
        'flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-[11px] font-bold text-white',
        AVATAR_TINTS[hashOf(label) % AVATAR_TINTS.length],
        className,
      )}
    >
      {initials}
    </span>
  )
}
