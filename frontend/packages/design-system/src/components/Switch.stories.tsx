import type { Meta, StoryObj } from '@storybook/react-vite'
import { Switch } from './Switch'

const meta: Meta<typeof Switch> = {
  title: 'Primitivi/Switch',
  component: Switch,
  args: { 'aria-label': 'Notifiche' },
}
export default meta
type Story = StoryObj<typeof Switch>

export const Off: Story = {}
export const On: Story = { args: { defaultChecked: true } }
export const Disabled: Story = { args: { disabled: true } }
