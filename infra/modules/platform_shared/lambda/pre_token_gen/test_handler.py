"""Test unitari della Pre-Token-Gen Lambda (UC 0016).

Mockano la connessione al DB e le credenziali: nessun boto3, nessun Postgres.
Verificano l'iniezione dei claim, il fail-closed e la regola platform-admin
(parità col provider locale, UC 0010).
"""

import os
import unittest

# Le env var sono lette all'import del modulo: impostarle prima.
os.environ.setdefault("DB_PROXY_HOST", "proxy.test")
os.environ.setdefault("DB_NAME", "appgrove")
os.environ.setdefault("DB_SECRET_ARN", "arn:test")

import handler  # noqa: E402


class FakeConn:
    """Connessione finta: registra le query e restituisce righe predefinite."""

    def __init__(self, rows):
        self.rows = rows
        self.queries = []

    def run(self, sql, **params):
        self.queries.append((sql, params))
        return self.rows


def _event(sub):
    return {"request": {"userAttributes": {"sub": sub} if sub else {}}, "response": {}}


def _access_claims(result):
    details = result.get("response", {}).get("claimsAndScopeOverrideDetails")
    if not details:
        return None
    return details["accessTokenGeneration"]["claimsToAddOrOverride"]


class PreTokenGenTest(unittest.TestCase):
    def setUp(self):
        # Stato pulito tra i test (le globali sono cache tra invocazioni).
        handler._connection = None
        handler._credentials = ("auth_lambdas", "secret")
        handler.PLATFORM_ADMIN_SUBS = frozenset()

    def _with_rows(self, rows):
        handler._connection = FakeConn(rows)

    def test_membership_attiva_inietta_tenant_e_ruolo(self):
        self._with_rows([("tenant-1", "owner")])
        out = handler.handler(_event("sub-1"), None)
        claims = _access_claims(out)
        self.assertEqual(claims["tenant_id"], "tenant-1")
        self.assertEqual(claims["roles"], ["owner"])

    def test_platform_admin_da_allow_list(self):
        handler.PLATFORM_ADMIN_SUBS = frozenset({"sub-admin"})
        self._with_rows([("tenant-1", "admin")])
        out = handler.handler(_event("sub-admin"), None)
        claims = _access_claims(out)
        self.assertEqual(claims["roles"], ["admin", "platform-admin"])

    def test_sub_non_in_allow_list_niente_platform_admin(self):
        handler.PLATFORM_ADMIN_SUBS = frozenset({"altro"})
        self._with_rows([("tenant-1", "member")])
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
        conn = FakeConn([("tenant-1", "owner")])
        handler._connection = conn
        handler.handler(_event("sub-1"), None)
        sql, params = conn.queries[0]
        self.assertIn("cognito_sub = :sub", sql)
        self.assertIn("status = 'active'", sql)
        self.assertIn("deleted_at IS NULL", sql)
        self.assertEqual(params["sub"], "sub-1")


if __name__ == "__main__":
    unittest.main()
