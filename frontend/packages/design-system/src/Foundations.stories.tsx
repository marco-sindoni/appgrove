import type { Meta, StoryObj } from '@storybook/react-vite'
import { Icon } from './components/Icon'
import { Logo } from './components/Logo'

const meta: Meta = {
  title: 'Foundations/Overview',
  parameters: { layout: 'fullscreen' },
}
export default meta
type Story = StoryObj

const Swatch = ({ className, label }: { className: string; label: string }) => (
  <div className="flex flex-col items-center gap-1">
    <div className={`h-14 w-14 rounded-lg border border-line ${className}`} />
    <span className="font-mono text-xs text-fg-muted">{label}</span>
  </div>
)

export const Tokens: Story = {
  render: () => (
    <div className="min-h-screen bg-bg p-8 text-fg">
      <h1 className="mb-6 font-sans text-2xl font-extrabold tracking-tight">appgrove — design tokens</h1>

      <section className="mb-8">
        <h2 className="mb-3 font-sans text-sm font-bold uppercase tracking-wide text-fg-muted">Palette</h2>
        <div className="flex flex-wrap gap-4">
          <Swatch className="bg-bg" label="bg" />
          <Swatch className="bg-surface" label="surface" />
          <Swatch className="bg-surface-2" label="surface-2" />
          <Swatch className="bg-accent" label="accent" />
          <Swatch className="bg-success" label="success" />
          <Swatch className="bg-warning" label="warning" />
          <Swatch className="bg-danger" label="danger" />
        </div>
        <div className="mt-4 flex flex-wrap gap-4">
          <Swatch className="bg-cat-green" label="cat-green" />
          <Swatch className="bg-cat-amber" label="cat-amber" />
          <Swatch className="bg-cat-red" label="cat-red" />
          <Swatch className="bg-cat-blue" label="cat-blue" />
          <Swatch className="bg-cat-violet" label="cat-violet" />
          <Swatch className="bg-cat-teal" label="cat-teal" />
        </div>
      </section>

      <section className="mb-8">
        <h2 className="mb-3 font-sans text-sm font-bold uppercase tracking-wide text-fg-muted">Tipografia</h2>
        <p className="font-sans text-3xl font-extrabold tracking-tight">Plus Jakarta Sans — 800</p>
        <p className="font-sans text-base">Plus Jakarta Sans — testo corrente 400</p>
        <p className="font-mono text-base">JetBrains Mono — € 1.234,56 · INV-0042</p>
      </section>

      <section className="mb-8">
        <h2 className="mb-3 font-sans text-sm font-bold uppercase tracking-wide text-fg-muted">Raggi & ombre</h2>
        <div className="flex flex-wrap gap-4">
          <div className="grid h-20 w-28 place-items-center rounded-md bg-surface shadow-sm font-mono text-xs">sm</div>
          <div className="grid h-20 w-28 place-items-center rounded-lg bg-surface shadow font-mono text-xs">md</div>
          <div className="grid h-20 w-28 place-items-center rounded-xl bg-surface shadow-lg font-mono text-xs">lg</div>
        </div>
      </section>

      <section className="mb-8">
        <h2 className="mb-3 font-sans text-sm font-bold uppercase tracking-wide text-fg-muted">Icone (Material Symbols)</h2>
        <div className="flex items-center gap-4 text-fg">
          <Icon name="dashboard" />
          <Icon name="receipt_long" />
          <Icon name="calendar_month" />
          <Icon name="eco" filled />
          <Icon name="settings" />
        </div>
      </section>

      <section>
        <h2 className="mb-3 font-sans text-sm font-bold uppercase tracking-wide text-fg-muted">Logo</h2>
        <Logo />
      </section>
    </div>
  ),
}
