"""Test unitari del canary di uptime (UC 0007).

Girano offline: nessuna rete, nessun boto3. Si verifica la logica pura
(classificazione del codice HTTP, costruzione del payload della metrica) e che
l'handler pubblichi la metrica giusta, sostituendo il seam degli effetti
collaterali (`probe`, `_publish_metric`).
"""

import unittest
from unittest import mock

import uptime_ping


class ClassifyTest(unittest.TestCase):
    def test_2xx_e_3xx_sono_sani(self):
        for status in (200, 204, 301, 302, 399):
            self.assertEqual(1, uptime_ping.classify(status), status)

    def test_4xx_5xx_e_none_sono_non_sani(self):
        for status in (400, 403, 404, 500, 503, None):
            self.assertEqual(0, uptime_ping.classify(status), status)


class EvaluateTest(unittest.TestCase):
    def test_bersaglio_raggiungibile(self):
        with mock.patch.object(uptime_ping, "probe", return_value=200):
            self.assertEqual(1, uptime_ping.evaluate("https://esempio.test/"))

    def test_bersaglio_irraggiungibile(self):
        # probe ritorna None su timeout/DNS/connessione → non sano.
        with mock.patch.object(uptime_ping, "probe", return_value=None):
            self.assertEqual(0, uptime_ping.evaluate("https://esempio.test/"))

    def test_errore_server(self):
        with mock.patch.object(uptime_ping, "probe", return_value=503):
            self.assertEqual(0, uptime_ping.evaluate("https://esempio.test/"))


class MetricPayloadTest(unittest.TestCase):
    def test_namespace_metrica_e_dimensione(self):
        payload = uptime_ping.build_metric_data(1)
        self.assertEqual("appgrove/uptime", payload["Namespace"])
        datum = payload["MetricData"][0]
        self.assertEqual("Healthy", datum["MetricName"])
        self.assertEqual(1.0, datum["Value"])
        self.assertEqual([{"Name": "Target", "Value": uptime_ping.TARGET_LABEL}], datum["Dimensions"])


class HandlerTest(unittest.TestCase):
    def test_handler_pubblica_lo_stato_valutato(self):
        with mock.patch.object(uptime_ping, "probe", return_value=200), \
             mock.patch.object(uptime_ping, "_publish_metric") as publish:
            result = uptime_ping.handler({}, None)
        self.assertEqual(1, result["healthy"])
        publish.assert_called_once_with(1)

    def test_handler_down_pubblica_zero(self):
        with mock.patch.object(uptime_ping, "probe", return_value=None), \
             mock.patch.object(uptime_ping, "_publish_metric") as publish:
            result = uptime_ping.handler({}, None)
        self.assertEqual(0, result["healthy"])
        publish.assert_called_once_with(0)


if __name__ == "__main__":
    unittest.main()
