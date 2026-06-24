import type { Meta, StoryObj } from '@storybook/react-vite'
import { SegmentedControl } from './SegmentedControl'

const meta: Meta<typeof SegmentedControl> = {
  title: 'Primitivi/SegmentedControl',
  component: SegmentedControl,
}
export default meta
type Story = StoryObj<typeof SegmentedControl>

export const Ciclo: Story = {
  args: {
    'aria-label': 'Ciclo di fatturazione',
    options: [
      { value: 'monthly', label: 'Mensile' },
      { value: 'annual', label: 'Annuale' },
    ],
  },
}

export const Viste: Story = {
  args: {
    'aria-label': 'Vista calendario',
    options: [
      { value: 'month', label: 'Mese' },
      { value: 'week', label: 'Settimana' },
      { value: 'day', label: 'Giorno' },
    ],
  },
}
