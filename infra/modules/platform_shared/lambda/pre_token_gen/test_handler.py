"""Test unitari della Pre-Token-Gen Lambda (UC 0016 + UC 0117).

Mockano la connessione al DB e le credenziali: nessun boto3, nessun Postgres.
Verificano l'iniezione dei claim, il fail-closed, la regola platform-admin e —
da UC 0117 — la **tabella dei casi** della scelta dell'account attivo.

La classe `ScegliAccountAttivoTest` è la gemella di `ActiveAccountTest` (Java,
services/commons): stessa tabella di casi, stessi esiti. È quella parità a
tenere insieme locale e cloud — se una delle due cambia senza l'altra, i
collaudi locali dicono una cosa e l'ambiente reale un'altra.
"""

import os
import unittest

# Le env var sono lette all'import del modulo: impostarle prima.
os.environ.setdefault("DB_PROXY_HOST", "proxy.test")
os.environ.setdefault("DB_NAME", "appgrove")
os.environ.setdefault("DB_SECRET_ARN", "arn:test")

import handler  # noqa: E402

# Identificativi di appartenenza usati nelle righe finte (l'ordine delle righe è quello
# del SQL: anzianità crescente).
M1 = "d0000000-0000-4000-8000-000000000001"
M2 = "d0000000-0000-4000-8000-000000000002"
M_ALTRUI = "d0000000-0000-4000-8000-0000000000ff"


class FakeConn:
    """Connessione finta: registra le query e restituisce righe predefinite."""

    def __init__(self, rows):
        self.rows = rows
        self.queries = []

    def run(self, sql, **params):
        self.queries.append((sql, params))
        return self.rows


def _row(membership_id, tenant_id, role, stored):
    """Una riga come la restituisce _MEMBERSHIP_SQL: (id, tenant_id, role, active_membership_id)."""
    return (membership_id, tenant_id, role, stored)


def _event(sub):
    return {"request": {"userAttributes": {"sub": sub} if sub else {}}, "response": {}}


def _access_claims(result):
    details = result.get("response", {}).get("claimsAndScopeOverrideDetails")
    if not details:
        return None
    return details["accessTokenGeneration"]["claimsToAddOrOverride"]


class ScegliAccountAttivoTest(unittest.TestCase):
    """La tabella dei casi di UC 0117 §4.2 sulla funzione PURA — nessun database."""

    def test_nessuna_appartenenza_attiva(self):
        self.assertEqual(handler.choose_active_account([], None), (handler.CHOICE_NONE, None))
        self.assertEqual(
            handler.choose_active_account([], M1),
            (handler.CHOICE_NONE, None),
            "senza appartenenze attive il valore conservato non può resuscitare nulla",
        )

    def test_una_sola_appartenenza_vince_sul_valore_conservato(self):
        una = [(M1, "tenant-acme", "owner")]
        for stored in (None, M1, M_ALTRUI):
            esito, scelta = handler.choose_active_account(una, stored)
            self.assertEqual(esito, handler.CHOICE_CHOSEN)
            self.assertEqual(scelta[1], "tenant-acme")

    def test_piu_appartenenze_con_valore_conservato_valido(self):
        due = [(M1, "tenant-acme", "owner"), (M2, "tenant-beta", "member")]
        self.assertEqual(handler.choose_active_account(due, M2)[1][1], "tenant-beta")
        self.assertEqual(handler.choose_active_account(due, M1)[1][1], "tenant-acme")

    def test_piu_appartenenze_senza_scelta(self):
        due = [(M1, "tenant-acme", "owner"), (M2, "tenant-beta", "member")]
        self.assertEqual(
            handler.choose_active_account(due, None), (handler.CHOICE_MUST_CHOOSE, None)
        )

    def test_valore_conservato_non_corrispondente_non_produce_mai_una_scelta(self):
        """La prova che conta: il valore conservato NON è creduto."""
        due = [(M1, "tenant-acme", "owner"), (M2, "tenant-beta", "member")]
        self.assertEqual(
            handler.choose_active_account(due, M_ALTRUI),
            (handler.CHOICE_MUST_CHOOSE, None),
            "un'appartenenza che non è fra quelle attive non può diventare l'account della sessione",
        )


class PreTokenGenTest(unittest.TestCase):
    def setUp(self):
        # Stato pulito tra i test (le globali sono cache tra invocazioni).
        handler._connection = None
        handler._credentials = ("auth_lambdas", "secret")
        handler.PLATFORM_ADMIN_SUBS = frozenset()

    def _with_rows(self, rows):
        handler._connection = FakeConn(rows)

    def test_membership_attiva_inietta_tenant_e_ruolo(self):
        self._with_rows([_row(M1, "tenant-1", "owner", None)])
        out = handler.handler(_event("sub-1"), None)
        claims = _access_claims(out)
        self.assertEqual(claims["tenant_id"], "tenant-1")
        self.assertEqual(claims["roles"], ["owner"])

    def test_platform_admin_da_allow_list(self):
        handler.PLATFORM_ADMIN_SUBS = frozenset({"sub-admin"})
        self._with_rows([_row(M1, "tenant-1", "admin", None)])
        out = handler.handler(_event("sub-admin"), None)
        claims = _access_claims(out)
        self.assertEqual(claims["roles"], ["admin", "platform-admin"])

    def test_sub_non_in_allow_list_niente_platform_admin(self):
        handler.PLATFORM_ADMIN_SUBS = frozenset({"altro"})
        self._with_rows([_row(M1, "tenant-1", "member", None)])
        out = handler.handler(_event("sub-1"), None)
        self.assertEqual(_access_claims(out)["roles"], ["member"])

    def test_nessuna_membership_fail_closed(self):
        self._with_rows([])  # query non trova righe
        out = handler.handler(_event("sconosciuto"), None)
        self.assertIsNone(_access_claims(out), "nessun claim deve essere iniettato")

    def test_sub_assente_fail_closed(self):
        out = handler.handler(_event(None), None)
        self.assertIsNone(_access_claims(out))

    def test_query_filtra_su_sub_attivo_non_cancellato(self):
        conn = FakeConn([_row(M1, "tenant-1", "owner", None)])
        handler._connection = conn
        handler.handler(_event("sub-1"), None)
        sql, params = conn.queries[0]
        self.assertIn("i.cognito_sub = :sub", sql)
        self.assertIn("i.status = 'active'", sql)
        self.assertIn("m.status = 'active'", sql)
        self.assertIn("i.deleted_at IS NULL", sql)
        self.assertIn("m.deleted_at IS NULL", sql)
        self.assertEqual(params["sub"], "sub-1")

    def test_query_legge_identita_appartenenze_e_account_attivo(self):
        """UC 0116/0117: identità + TUTTE le appartenenze attive + il riferimento conservato."""
        conn = FakeConn([_row(M1, "tenant-1", "owner", None)])
        handler._connection = conn
        handler.handler(_event("sub-1"), None)
        sql, _ = conn.queries[0]
        self.assertIn("platform.identity", sql)
        self.assertIn("platform.membership", sql)
        self.assertIn("i.active_membership_id", sql)
        self.assertNotIn("platform.users", sql, "platform.users è fredda dalla change 0088")
        self.assertNotIn(
            "LIMIT 1", sql, "servono TUTTE le appartenenze attive: è la scelta a stabilire quale vale"
        )

    def test_una_sola_appartenenza_ignora_il_valore_conservato(self):
        """Il caso di tutti gli utenti di oggi: nulla cambia, nemmeno con la colonna manomessa."""
        self._with_rows([_row(M1, "tenant-1", "owner", M_ALTRUI)])
        out = handler.handler(_event("sub-1"), None)
        self.assertEqual(_access_claims(out)["tenant_id"], "tenant-1")

    def test_piu_appartenenze_usa_l_account_attivo_conservato(self):
        """UC 0117: non più «la più antica» (ripiego di UC 0116) ma l'account attivo scelto."""
        self._with_rows(
            [
                _row(M1, "tenant-vecchio", "owner", M2),
                _row(M2, "tenant-nuovo", "member", M2),
            ]
        )
        out = handler.handler(_event("sub-multi"), None)
        self.assertEqual(_access_claims(out)["tenant_id"], "tenant-nuovo")
        self.assertEqual(_access_claims(out)["roles"], ["member"])

    def test_piu_appartenenze_senza_account_attivo_fail_closed(self):
        """Nessuno decide al posto della persona per conto di chi sta agendo."""
        self._with_rows(
            [
                _row(M1, "tenant-vecchio", "owner", None),
                _row(M2, "tenant-nuovo", "member", None),
            ]
        )
        out = handler.handler(_event("sub-multi"), None)
        self.assertIsNone(_access_claims(out))

    def test_account_attivo_manomesso_non_produce_mai_un_claim(self):
        """La prova di sicurezza: la colonna scritta a mano su un'appartenenza non attiva."""
        self._with_rows(
            [
                _row(M1, "tenant-vecchio", "owner", M_ALTRUI),
                _row(M2, "tenant-nuovo", "member", M_ALTRUI),
            ]
        )
        out = handler.handler(_event("sub-manomesso"), None)
        self.assertIsNone(_access_claims(out), "il valore conservato non è creduto")

    def test_identita_senza_appartenenze_fail_closed(self):
        """Chi è uscito dal suo ultimo account non ottiene un token valido (UC 0116 §6)."""
        self._with_rows([])
        out = handler.handler(_event("sub-orfano"), None)
        self.assertIsNone(_access_claims(out))


if __name__ == "__main__":
    unittest.main()
