import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { axe } from 'jest-axe'
import { Card, CardContent, CardHeader, CardTitle } from './Card'

describe('Card', () => {
  it('compone header/title/content', () => {
    render(
      <Card>
        <CardHeader>
          <CardTitle>Fatture aperte</CardTitle>
        </CardHeader>
        <CardContent>3 in sospeso</CardContent>
      </Card>,
    )
    expect(screen.getByRole('heading', { name: 'Fatture aperte' })).toBeInTheDocument()
    expect(screen.getByText('3 in sospeso')).toBeInTheDocument()
  })

  it('non ha violazioni a11y', async () => {
    const { container } = render(
      <Card>
        <CardHeader>
          <CardTitle>Titolo</CardTitle>
        </CardHeader>
        <CardContent>Contenuto</CardContent>
      </Card>,
    )
    expect(await axe(container)).toHaveNoViolations()
  })
})
