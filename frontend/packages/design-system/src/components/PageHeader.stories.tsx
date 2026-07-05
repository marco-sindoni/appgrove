import type { Meta, StoryObj } from '@storybook/react-vite'
import { Button } from './Button'
import { Icon } from './Icon'
import { PageHeader } from './PageHeader'

const meta: Meta<typeof PageHeader> = {
  title: 'Components/PageHeader',
  component: PageHeader,
  parameters: { layout: 'padded' },
}
export default meta

type Story = StoryObj<typeof PageHeader>

export const Piattaforma: Story = {
  args: {
    title: 'Workspace overview',
    subtitle: 'Lo stato del tuo workspace negli ultimi 30 giorni',
    actions: (
      <Button>
        <Icon name="add" size={19} />
        Nuova app
      </Button>
    ),
  },
}

export const PaginaApp: Story = {
  args: {
    title: 'Fatture',
    subtitle: 'Le fatture del tuo account',
    icon: 'receipt_long',
    iconClassName: 'bg-cat-blue/15 text-cat-blue',
    actions: (
      <Button className="bg-cat-blue shadow-[0_6px_16px_-6px_rgb(var(--ag-cat-blue))]">
        <Icon name="add" size={19} />
        Nuova fattura
      </Button>
    ),
  },
}
