"""Test unitari di db-bootstrap: modalità servizio (retrocompatibilità) e
modalità grant su schema altrui (UC 0016, E23-B). Mockano boto3/Data API:
verificano l'SQL generata, non toccano Aurora."""

import json
import os
import sys
import unittest
from unittest import mock

# boto3 e le env sono richiesti all'import del modulo: mock/inject prima.
sys.modules.setdefault("boto3", mock.MagicMock())
os.environ.setdefault("CLUSTER_ARN", "arn:cluster")
os.environ.setdefault("MASTER_SECRET_ARN", "arn:master")
os.environ.setdefault("DB_NAME", "appgrove")

import db_bootstrap  # noqa: E402


class DbBootstrapTest(unittest.TestCase):
    def setUp(self):
        self.executed = []
        self._exec_patch = mock.patch.object(
            db_bootstrap, "_execute", side_effect=lambda sql: self.executed.append(sql)
        )
        self._exec_patch.start()
        db_bootstrap.secrets = mock.MagicMock()
        db_bootstrap.secrets.get_secret_value.return_value = {
            "SecretString": json.dumps({"username": "auth_lambdas", "password": "pw"})
        }

    def tearDown(self):
        self._exec_patch.stop()

    def _sql(self):
        return "\n".join(self.executed)

    def test_modalita_servizio_crea_ruolo_e_schema(self):
        out = db_bootstrap.handler(
            {"role_name": "fatture", "schema_name": "fatture", "secret_arn": "arn:s"},
            None,
        )
        self.assertEqual(out["status"], "ok")
        sql = self._sql()
        self.assertIn("CREATE ROLE fatture LOGIN", sql)
        self.assertIn("CREATE SCHEMA IF NOT EXISTS fatture AUTHORIZATION fatture", sql)
        self.assertIn("REVOKE CREATE ON SCHEMA public FROM fatture", sql)

    def test_modalita_grant_ruolo_e_privilegi_minimi(self):
        out = db_bootstrap.handler(
            {
                "mode": "grant",
                "role_name": "auth_lambdas",
                "secret_arn": "arn:s",
                "grants": {
                    "schema": "platform",
                    "owner_role": "platform",
                    "select_all": True,
                    "write_tables": ["accounts", "users", "invitations"],
                },
            },
            None,
        )
        self.assertEqual(out, {"role": "auth_lambdas", "granted_on": "platform", "status": "ok"})
        sql = self._sql()
        # ruolo login creato, nessuno schema di proprietà (niente CREATE SCHEMA)
        self.assertIn("CREATE ROLE auth_lambdas LOGIN", sql)
        self.assertNotIn("CREATE SCHEMA", sql)
        # privilegi minimi su schema altrui
        self.assertIn("GRANT USAGE ON SCHEMA platform TO auth_lambdas", sql)
        self.assertIn("GRANT SELECT ON ALL TABLES IN SCHEMA platform TO auth_lambdas", sql)
        self.assertIn("GRANT SELECT, INSERT, UPDATE ON platform.accounts TO auth_lambdas", sql)
        self.assertIn("GRANT SELECT, INSERT, UPDATE ON platform.users TO auth_lambdas", sql)
        self.assertIn("GRANT SELECT, INSERT, UPDATE ON platform.invitations TO auth_lambdas", sql)

    def test_grant_senza_select_all_niente_lettura_globale(self):
        db_bootstrap.handler(
            {
                "mode": "grant",
                "role_name": "auth_lambdas",
                "secret_arn": "arn:s",
                "grants": {"schema": "platform", "owner_role": "platform", "write_tables": ["users"]},
            },
            None,
        )
        self.assertNotIn("ON ALL TABLES", self._sql())

    def test_identificatore_non_valido_rifiutato(self):
        with self.assertRaises(ValueError):
            db_bootstrap.handler(
                {
                    "mode": "grant",
                    "role_name": "auth_lambdas",
                    "secret_arn": "arn:s",
                    "grants": {"schema": "platform; DROP", "owner_role": "platform"},
                },
                None,
            )


if __name__ == "__main__":
    unittest.main()
