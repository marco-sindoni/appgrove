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
        self.assertIn("i.cognito_sub = :sub", sql)
        self.assertIn("i.status = 'active'", sql)
        self.assertIn("m.status = 'active'", sql)
        self.assertIn("i.deleted_at IS NULL", sql)
        self.assertIn("m.deleted_at IS NULL", sql)
        self.assertEqual(params["sub"], "sub-1")

    def test_query_legge_identita_e_appartenenza(self):
        """UC 0116: la persona sta in platform.identity, il suo posto in platform.membership."""
        conn = FakeConn([("tenant-1", "owner")])
        handler._connection = conn
        handler.handler(_event("sub-1"), None)
        sql, _ = conn.queries[0]
        self.assertIn("platform.identity", sql)
        self.assertIn("platform.membership", sql)
        self.assertNotIn("platform.users", sql, "platform.users è fredda dalla change 0088")

    def test_piu_appartenenze_prende_la_piu_antica(self):
        """Con più appartenenze attive si sceglie la più ANTICA, in modo deterministico.

        Ripiego dichiarato di UC 0116: la scelta dell'account attivo è di UC 0117. Il criterio è
        scritto nel SQL (ORDER BY m.created_at, m.id LIMIT 1) e deve restare identico a quello del
        fornitore locale (UserDirectory.java) — è quella la parità che tiene insieme locale e cloud.
        """
        conn = FakeConn([("tenant-vecchio", "owner"), ("tenant-nuovo", "member")])
        handler._connection = conn
        out = handler.handler(_event("sub-multi"), None)
        sql, _ = conn.queries[0]
        self.assertIn("ORDER BY m.created_at, m.id LIMIT 1", sql)
        self.assertEqual(_access_claims(out)["tenant_id"], "tenant-vecchio")

    def test_identita_senza_appartenenze_fail_closed(self):
        """Chi è uscito dal suo ultimo account non ottiene un token valido (UC 0116 §6)."""
        self._with_rows([])
        out = handler.handler(_event("sub-orfano"), None)
        self.assertIsNone(_access_claims(out))


if __name__ == "__main__":
    unittest.main()
