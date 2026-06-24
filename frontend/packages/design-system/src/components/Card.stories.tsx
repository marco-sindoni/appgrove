import type { Meta, StoryObj } from '@storybook/react-vite'
import { Card, CardContent, CardHeader, CardTitle } from './Card'

const meta: Meta<typeof Card> = {
  title: 'Primitivi/Card',
  component: Card,
}
export default meta
type Story = StoryObj<typeof Card>

export const Default: Story = {
  render: () => (
    <Card className="w-72">
      <CardHeader>
        <CardTitle>Spesa mensile</CardTitle>
      </CardHeader>
      <CardContent>
        <span className="font-mono text-2xl font-semibold text-fg">€ 248,00</span>
        <p className="mt-1">3 app attive</p>
      </CardContent>
    </Card>
  ),
}
