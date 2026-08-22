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
let sentEmails: Array<{ email: string; token: string; body: Record<string, unknown> }>

const server = setupServer(
  http.get('http://localhost/api/platform/v1/users/me', () => HttpResponse.json(ME)),
  http.get('http://localhost/api/platform/v1/users', () =>
    HttpResponse.json({ content: members, page: 0, size: 100, totalElements: members.length }),
  ),
  http.get('http://localhost/api/platform/v1/invitations', () =>
    HttpResponse.json({ content: invites, page: 0, size: 100, totalElements: invites.length }),
  ),
  http.post('http://localhost/api/platform/v1/invitations', async ({ request }) => {
    const body = (await request.json()) as { email: string }
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
    const body = (await request.json()) as Record<string, unknown>
    sentEmails.push({ email: body.email as string, token: body.token as string, body })
    return new HttpResponse(null, { status: 202 })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  members = [
    {
      id: 'u-owner',
      email: 'owner@test',
      displayName: 'Owner',
      role: 'owner',
      status: 'active',
      joinedAt: '2024-01-01T00:00:00Z',
      apps: [
        { appId: 'app-1', app: 'crm', implicit: true },
        { appId: 'app-2', app: 'fatture', implicit: true },
      ],
    },
    {
      id: 'u-admin',
      email: 'admin@test',
      displayName: 'Admin',
      role: 'member',
      status: 'active',
      joinedAt: '2025-03-04T00:00:00Z',
      apps: [{ appId: 'app-1', app: 'crm', role: 'admin', implicit: false }],
    },
    {
      id: 'u-member',
      email: 'member@test',
      displayName: 'Member',
      role: 'member',
      status: 'active',
      joinedAt: '2025-06-07T00:00:00Z',
      apps: [],
    },
  ]
  invites = [
    { id: 'inv-old', email: 'pending@test', status: 'pending', expiresAt: '2026-07-01T00:00:00Z' },
  ]
  sentEmails = []
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ sub: 'u-owner', roles: ['owner'] }) })
})

describe('MembersPage — elenco unico di persone (UC 0100)', () => {
  /**
   * Il cuore della storia: **una** tabella. Prima erano due — le persone e gli inviti in attesa — e
   * chi guardava doveva sommare a mente. Il collaudo pretende una sola tabella e che l'invito in
   * attesa stia dentro, con il suo stato.
   */
  it('è una sola tabella, e gli inviti in attesa stanno dentro', async () => {
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    expect(screen.getAllByRole('table')).toHaveLength(1)
    const table = screen.getByRole('table')
    for (const email of ['owner@test', 'admin@test', 'member@test', 'pending@test']) {
      expect(within(table).getByText(email)).toBeInTheDocument()
    }
    // La riga dell'invito si riconosce dallo stato, non da una tabella separata.
    const pendingRow = within(table).getByText('pending@test').closest('tr') as HTMLElement
    expect(within(pendingRow).getByText('Invitation pending')).toBeInTheDocument()
    expect(within(pendingRow).getByText(/expires/)).toBeInTheDocument()
  })

  /**
   * **Nessuna colonna di ruolo.** Il collaudo pretende l'assenza, non la presenza delle colonne
   * nuove: cancellare la colonna senza un collaudo che ne vieti il ritorno l'avrebbe fatta tornare
   * alla prima revisione, senza che nulla diventasse rosso.
   */
  it('non ha una colonna di ruolo, e nessuna etichetta di ruolo di piattaforma', async () => {
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent)
    expect(headers).toEqual(['Email', 'Name', 'Status', 'Apps', 'Joined', 'Actions'])
    expect(screen.queryByRole('columnheader', { name: 'Role' })).not.toBeInTheDocument()
    // Le due etichette del ruolo di piattaforma non compaiono da nessuna parte della pagina.
    const ownerRow = screen.getByText('owner@test').closest('tr') as HTMLElement
    expect(within(ownerRow).queryByText('Owner', { selector: 'span' })).not.toBeInTheDocument()
    const adminRow = screen.getByText('admin@test').closest('tr') as HTMLElement
    expect(within(adminRow).queryByText('Member')).not.toBeInTheDocument()
    // E non esiste nessun selettore: non c'è nulla da scegliere.
    expect(screen.queryAllByRole('combobox')).toHaveLength(0)
  })

  it('conta le applicazioni di ciascuno e apre il dettaglio in sola lettura', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    const ownerRow = screen.getByText('owner@test').closest('tr') as HTMLElement
    expect(within(ownerRow).getByRole('button', { name: 'Apps for owner@test' })).toHaveTextContent('2 apps')
    const adminRow = screen.getByText('admin@test').closest('tr') as HTMLElement
    expect(within(adminRow).getByRole('button', { name: 'Apps for admin@test' })).toHaveTextContent('1 app')
    // Chi non è abilitato a nulla: stato legittimo, e si dice come si abilita.
    const memberRow = screen.getByText('member@test').closest('tr') as HTMLElement
    expect(within(memberRow).getByRole('button', { name: 'Apps for member@test' })).toHaveTextContent('No apps')

    await user.click(within(adminRow).getByRole('button', { name: 'Apps for admin@test' }))
    // Il ruolo si legge NEL dettaglio, accanto al nome dell'applicazione: è il ruolo su QUELLA
    // applicazione, non un ruolo della persona.
    const detail = (await screen.findByText('crm')).closest('li') as HTMLElement
    expect(within(detail).getByText('Admin')).toBeInTheDocument()
    // Il dettaglio dice dove si cambia il ruolo: non c'è un comando qui, ed è voluto (UC 0111).
    expect(
      screen.getByText('Roles on an app are changed from that app’s user management.'),
    ).toBeInTheDocument()

    await user.click(within(memberRow).getByRole('button', { name: 'Apps for member@test' }))
    expect(
      await screen.findByText('Not enabled on any app yet — you enable it from that app’s user management.'),
    ).toBeInTheDocument()
  })

  it('l’owner è in testa all’elenco', async () => {
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')
    const firstCell = screen.getAllByRole('row')[1].querySelector('td')
    expect(firstCell).toHaveTextContent('owner@test')
  })

  it('l’invito chiede solo l’indirizzo, e spiega perché non chiede il ruolo', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    expect(
      screen.getByText(
        'There is no role to choose: whoever joins is part of the workspace, and permissions are granted inside each app.',
      ),
    ).toBeInTheDocument()

    await user.type(screen.getByLabelText('Email'), 'new@acme.test')
    await user.click(screen.getByRole('button', { name: 'Send invitation' }))

    expect(await screen.findByText('Invitation sent to new@acme.test.')).toBeInTheDocument()
    expect(sentEmails).toHaveLength(1)
    // Nessun ruolo verso il servizio delle email: non c'è più un ruolo da annunciare.
    expect(sentEmails[0].body.role).toBeUndefined()
    expect(await screen.findByText('new@acme.test')).toBeInTheDocument()
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

  it('revoca un invito in attesa dopo conferma (DELETE)', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await screen.findByText('pending@test')

    await user.click(screen.getByRole('button', { name: 'Revoke' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Revoke' }))

    await vi.waitFor(() => expect(screen.queryByText('pending@test')).not.toBeInTheDocument())
  })

  it('protezioni UX: azioni distruttive disabilitate su se stessi / ultimo owner', async () => {
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    const ownerRow = screen.getByText('owner@test').closest('tr') as HTMLElement
    expect(within(ownerRow).getByRole('button', { name: 'Remove' })).toBeDisabled()
    expect(within(ownerRow).getByRole('button', { name: 'Suspend' })).toBeDisabled()

    const adminRow = screen.getByText('admin@test').closest('tr') as HTMLElement
    expect(within(adminRow).getByRole('button', { name: 'Remove' })).toBeEnabled()

    // Una riga di invito non ha né sospensione né rimozione: non c'è ancora nessuno da sospendere.
    const pendingRow = screen.getByText('pending@test').closest('tr') as HTMLElement
    expect(within(pendingRow).queryByRole('button', { name: 'Suspend' })).not.toBeInTheDocument()
    expect(within(pendingRow).getByRole('button', { name: 'Revoke' })).toBeEnabled()
  })

  it('non ha violazioni di accessibilità', async () => {
    const { container } = renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')
    expect(await axe(container)).toHaveNoViolations()
  })
})
