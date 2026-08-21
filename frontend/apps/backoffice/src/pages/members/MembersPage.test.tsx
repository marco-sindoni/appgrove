import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { MembersPage } from './MembersPage'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../../test/utils'

const ME = { id: 'u-owner', email: 'owner@test', displayName: 'Owner', role: 'owner', status: 'active' }

let members: Array<Record<string, unknown>>
let invites: Array<Record<string, unknown>>
let sentEmails: Array<{ email: string; token: string }>

const server = setupServer(
  http.get('http://localhost/api/platform/v1/users/me', () => HttpResponse.json(ME)),
  http.get('http://localhost/api/platform/v1/users', () =>
    HttpResponse.json({ content: members, page: 0, size: 100, totalElements: members.length }),
  ),
  http.get('http://localhost/api/platform/v1/invitations', () =>
    HttpResponse.json({ content: invites, page: 0, size: 100, totalElements: invites.length }),
  ),
  http.post('http://localhost/api/platform/v1/invitations', async ({ request }) => {
    const body = (await request.json()) as { email: string; role: string }
    // UC 0118 — le due collisioni LECITE, distinte dal campo `type` del problem+json. La terza
    // situazione («quella persona ha già un account appgrove») NON è qui di proposito: risponde 201
    // come un indirizzo sconosciuto, perché non è un'informazione dell'account che invita.
    if (body.email === 'gia-membro@x.io') {
      return HttpResponse.json(
        { type: 'urn:appgrove:invitation:already-member', title: 'Conflict', status: 409 },
        { status: 409 },
      )
    }
    if (body.email === 'gia-invitato@x.io') {
      return HttpResponse.json(
        { type: 'urn:appgrove:invitation:already-invited', title: 'Conflict', status: 409 },
        { status: 409 },
      )
    }
    const created = {
      id: `inv-${invites.length + 1}`,
      email: body.email,
      role: body.role,
      status: 'pending',
      expiresAt: '2026-07-03T00:00:00Z',
      token: 'raw-token-xyz',
    }
    invites = [...invites, { ...created, token: undefined }]
    return HttpResponse.json(created, { status: 201 })
  }),
  http.delete('http://localhost/api/platform/v1/invitations/:id', ({ params }) => {
    invites = invites.filter((i) => i.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),
  http.patch('http://localhost/api/platform/v1/users/:id', async ({ params, request }) => {
    const body = (await request.json()) as { role?: string; status?: string }
    members = members.map((m) => (m.id === params.id ? { ...m, ...body } : m))
    return HttpResponse.json(members.find((m) => m.id === params.id))
  }),
  http.post('http://localhost/api/auth/invitations/send', async ({ request }) => {
    const body = (await request.json()) as { email: string; token: string }
    sentEmails.push({ email: body.email, token: body.token })
    return new HttpResponse(null, { status: 202 })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  members = [
    { id: 'u-owner', email: 'owner@test', displayName: 'Owner', role: 'owner', status: 'active' },
    { id: 'u-admin', email: 'admin@test', displayName: 'Admin', role: 'member', status: 'active' },
    { id: 'u-member', email: 'member@test', displayName: 'Member', role: 'member', status: 'active' },
  ]
  invites = [
    { id: 'inv-old', email: 'pending@test', role: 'member', status: 'pending', expiresAt: '2026-07-01T00:00:00Z' },
  ]
  sentEmails = []
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ sub: 'u-owner', roles: ['owner'] }) })
})

describe('MembersPage (UC 0059)', () => {
  it('le due collisioni lecite dell’invito hanno due messaggi distinti (UC 0118)', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    await user.type(screen.getByLabelText('Email'), 'gia-membro@x.io')
    await user.click(screen.getByRole('button', { name: 'Send invitation' }))
    expect(
      await screen.findByText('This person is already a member of this account.'),
    ).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Email'))
    await user.type(screen.getByLabelText('Email'), 'gia-invitato@x.io')
    await user.click(screen.getByRole('button', { name: 'Send invitation' }))
    expect(
      await screen.findByText('There is already a pending invitation for this address.'),
    ).toBeInTheDocument()
  })

  it('dice che il posto è dell’account, non della persona (UC 0118)', async () => {
    // La regola va scritta dove si invita: la prima reazione di chi invita qualcuno che ha già un
    // account altrove è «ma la paga già l'altra azienda».
    renderWithProviders(<MembersPage />)
    expect(
      await screen.findByText(
        'The seat belongs to this account: it is paid here even if the person already works in another account.',
      ),
    ).toBeInTheDocument()
  })

  it('mostra i membri e gli inviti pendenti', async () => {
    renderWithProviders(<MembersPage />)
    expect(await screen.findByText('owner@test')).toBeInTheDocument()
    expect(screen.getByText('admin@test')).toBeInTheDocument()
    expect(screen.getByText('member@test')).toBeInTheDocument()
    expect(screen.getByText('pending@test')).toBeInTheDocument()
  })

  it('invita un membro: crea invito, invia email, e compare tra i pendenti', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    await user.type(screen.getByLabelText('Email'), 'new@acme.test')
    await user.click(screen.getByRole('button', { name: 'Send invitation' }))

    expect(await screen.findByText('Invitation sent to new@acme.test.')).toBeInTheDocument()
    expect(sentEmails).toEqual([{ email: 'new@acme.test', token: 'raw-token-xyz' }])
    expect(await screen.findByText('new@acme.test')).toBeInTheDocument()
  })

  it('revoca un invito pendente dopo conferma (DELETE)', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await screen.findByText('pending@test')

    await user.click(screen.getByRole('button', { name: 'Revoke' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Revoke' }))

    await vi.waitFor(() => expect(screen.queryByText('pending@test')).not.toBeInTheDocument())
  })

  /**
   * Il ruolo di piattaforma ha due soli valori (UC 0098): non c'è nulla da scegliere, quindi la colonna
   * è una etichetta e non un selettore. Il collaudo che esercitava il cambio di ruolo è stato
   * **sostituito** da questo, che pretende l'assenza del comando: cancellarlo senza rimpiazzo avrebbe
   * lasciato tornare il selettore senza che nulla diventasse rosso.
   */
  it('il ruolo è una etichetta, non un selettore (due soli valori di piattaforma)', async () => {
    renderWithProviders(<MembersPage />)
    await screen.findByText('admin@test')

    expect(screen.queryAllByRole('combobox')).toHaveLength(0)
    const adminRow = screen.getByText('admin@test').closest('tr') as HTMLElement
    expect(within(adminRow).getByText('Member')).toBeInTheDocument()
  })

  it('protezioni UX: azioni distruttive disabilitate su se stessi / ultimo owner', async () => {
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    const ownerRow = screen.getByText('owner@test').closest('tr') as HTMLElement
    expect(within(ownerRow).getByRole('button', { name: 'Remove' })).toBeDisabled()

    const adminRow = screen.getByText('admin@test').closest('tr') as HTMLElement
    expect(within(adminRow).getByRole('button', { name: 'Remove' })).toBeEnabled()
  })

  it('non ha violazioni di accessibilità', async () => {
    const { container } = renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')
    expect(await axe(container)).toHaveNoViolations()
  })
})
