import type { Meta, StoryObj } from '@storybook/react-vite'
import { Input } from './Input'

const meta: Meta<typeof Input> = {
  title: 'Primitivi/Input',
  component: Input,
  args: { placeholder: 'nome@appgrove.app' },
}
export default meta
type Story = StoryObj<typeof Input>

export const Default: Story = {}
export const Invalid: Story = { args: { invalid: true, defaultValue: 'non-valida' } }
export const Disabled: Story = { args: { disabled: true, defaultValue: 'bloccato' } }
