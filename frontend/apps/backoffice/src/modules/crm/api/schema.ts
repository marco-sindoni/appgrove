/**
 * Tipi dell'API crm — scritti a mano, fedeli al dominio reale del mini-CRM (UC 0054).
 *
 * ⚠️ Questo file è normalmente GENERATO dallo spec OpenAPI del servizio:
 *
 *     npm run gen:crm
 *
 * Lo spec nasce solo dopo la prima compilazione del backend
 * (`quarkus.smallrye-openapi.store-schema-directory`), quindi finché non lo si rigenera vale questa
 * versione a mano. Da quel momento lo spec è la fonte di verità e ogni divergenza fra frontend e
 * backend rompe `tsc` invece di arrivare in produzione (#10 G25).
 */

export interface paths {
  '/api/crm/v1/contacts': {
    get: {
      parameters: {
        query?: { q?: string; stage?: string; page?: number; size?: number }
      }
      responses: {
        200: { content: { 'application/json': components['schemas']['PageContactView'] } }
        403: { content?: never }
      }
    }
    post: {
      requestBody: { content: { 'application/json': components['schemas']['CreateContact'] } }
      responses: {
        201: { content: { 'application/json': components['schemas']['ContactView'] } }
        400: { content?: never }
        402: { content?: never }
        403: { content?: never }
      }
    }
  }
  '/api/crm/v1/contacts/{id}': {
    get: {
      parameters: { path: { id: string } }
      responses: {
        200: { content: { 'application/json': components['schemas']['ContactView'] } }
        404: { content?: never }
      }
    }
    delete: {
      parameters: { path: { id: string } }
      responses: { 204: { content?: never }; 404: { content?: never } }
    }
    patch: {
      parameters: { path: { id: string } }
      requestBody: { content: { 'application/json': components['schemas']['UpdateContact'] } }
      responses: {
        200: { content: { 'application/json': components['schemas']['ContactView'] } }
        404: { content?: never }
      }
    }
  }
  '/api/crm/v1/contacts/{contactId}/interactions': {
    get: {
      parameters: { path: { contactId: string } }
      responses: {
        200: { content: { 'application/json': components['schemas']['InteractionView'][] } }
        404: { content?: never }
      }
    }
    post: {
      parameters: { path: { contactId: string } }
      requestBody: { content: { 'application/json': components['schemas']['CreateInteraction'] } }
      responses: {
        201: { content: { 'application/json': components['schemas']['InteractionView'] } }
        400: { content?: never }
        404: { content?: never }
      }
    }
  }
  '/api/crm/v1/seats': {
    get: {
      responses: {
        200: { content: { 'application/json': components['schemas']['SeatSummary'] } }
        403: { content?: never }
      }
    }
    post: {
      requestBody: { content: { 'application/json': components['schemas']['AssignSeat'] } }
      responses: {
        200: { content: { 'application/json': components['schemas']['SeatView'] } }
        201: { content: { 'application/json': components['schemas']['SeatView'] } }
        402: { content?: never }
        403: { content?: never }
        429: { content?: never }
      }
    }
  }
  '/api/crm/v1/seats/{userId}': {
    delete: {
      parameters: { path: { userId: string } }
      responses: { 204: { content?: never }; 403: { content?: never }; 404: { content?: never } }
    }
  }
  '/api/crm/v1/quota': {
    get: {
      responses: {
        200: { content: { 'application/json': components['schemas']['QuotaStatusView'] } }
        403: { content?: never }
      }
    }
  }
}

export type webhooks = Record<string, never>

export interface components {
  schemas: {
    CreateContact: {
      displayName: string
      email?: string
      phone?: string
      organization?: string
      notes?: string
    }
    UpdateContact: {
      displayName?: string
      email?: string
      phone?: string
      organization?: string
      stage?: string
      notes?: string
    }
    ContactView: {
      id?: components['schemas']['UUID']
      displayName?: string
      email?: string
      phone?: string
      organization?: string
      stage?: string
      notes?: string
      tenantId?: string
    }
    PageContactView: {
      content?: components['schemas']['ContactView'][]
      page?: number
      size?: number
      totalElements?: number
      totalPages?: number
    }
    CreateInteraction: {
      kind?: string
      occurredOn?: components['schemas']['LocalDate']
      note?: string
    }
    InteractionView: {
      id?: components['schemas']['UUID']
      contactId?: components['schemas']['UUID']
      kind?: string
      occurredOn?: components['schemas']['LocalDate']
      note?: string
    }
    AssignSeat: { userId: string }
    SeatView: {
      id?: components['schemas']['UUID']
      userId?: string
    }
    SeatSummary: {
      used?: number
      limit?: number | null
      remaining?: number | null
      seats?: components['schemas']['SeatView'][]
    }
    QuotaStatusView: {
      metric?: string
      used?: number
      limit?: number | null
      remaining?: number | null
    }
    /** Format: date */
    LocalDate: string
    /** Format: uuid */
    UUID: string
  }
  responses: never
  parameters: never
  requestBodies: never
  headers: never
  pathItems: never
}

export type $defs = Record<string, never>
export type operations = Record<string, never>
