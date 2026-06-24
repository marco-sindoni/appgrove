import type { Meta, StoryObj } from '@storybook/react-vite'
import { Badge } from './Badge'

const meta: Meta<typeof Badge> = {
  title: 'Primitivi/Badge',
  component: Badge,
  args: { children: 'Etichetta' },
  argTypes: {
    tone: { control: 'select', options: ['neutral', 'accent', 'success', 'warning', 'danger'] },
  },
}
export default meta
type Story = StoryObj<typeof Badge>

export const Tutte: Story = {
  render: () => (
    <div className="flex items-center gap-2">
      <Badge tone="neutral">B2C</Badge>
      <Badge tone="accent">Pro</Badge>
      <Badge tone="success">Attivo</Badge>
      <Badge tone="warning">In pausa</Badge>
      <Badge tone="danger">Scaduto</Badge>
    </div>
  ),
}
