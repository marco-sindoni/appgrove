"""Test unitari del Custom Message Lambda (UC 0018).

Girano offline: nessun boto3, nessun Cognito. I template vengono letti dalla sorgente condivisa del
repo — la stessa che Terraform inserisce nell'archivio e che il servizio Java copia nel proprio
artefatto — così un test verde qui vale anche per il contenuto che spediremo davvero.
"""

import os
import unittest

_REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", "..", ".."))

# Le env var sono lette all'import del modulo: impostarle prima.
os.environ.setdefault("APP_BASE_URL", "https://app.test.appgrove.app")
os.environ.setdefault("EMAIL_TEMPLATES_DIR", os.path.join(_REPO_ROOT, "shared", "email-templates"))

import handler  # noqa: E402


def _event(trigger, locale=None, email="utente@esempio.test", code="{####}"):
    request = {"userAttributes": {"email": email}, "codeParameter": code}
    if locale is not None:
        request["clientMetadata"] = {"locale": locale}
    return {"triggerSource": trigger, "request": request, "response": {}}


class LocaleTest(unittest.TestCase):
    def test_supported_languages(self):
        self.assertEqual("it", handler.normalize_locale("it"))
        self.assertEqual("en", handler.normalize_locale("en"))

    def test_regional_variants_resolve_to_their_language(self):
        for raw in ("it-IT", "it_IT", "IT", " it "):
            self.assertEqual("it", handler.normalize_locale(raw), raw)

    def test_everything_else_falls_back_to_english(self):
        for raw in (None, "", "  ", "de", "fr-FR", "spazzatura"):
            self.assertEqual("en", handler.normalize_locale(raw), raw)


class RenderTest(unittest.TestCase):
    def test_language_selects_the_copy(self):
        result = handler.handler(_event("CustomMessage_SignUp", locale="it"))
        self.assertEqual("Conferma il tuo indirizzo email", result["response"]["emailSubject"])
        self.assertIn("Benvenuto su appgrove", result["response"]["emailMessage"])

    def test_missing_language_falls_back_to_english(self):
        result = handler.handler(_event("CustomMessage_SignUp"))
        self.assertEqual("Confirm your email address", result["response"]["emailSubject"])

    def test_forgot_password_uses_the_reset_copy(self):
        result = handler.handler(_event("CustomMessage_ForgotPassword", locale="it"))
        self.assertEqual("Reimposta la password", result["response"]["emailSubject"])
        self.assertIn("/reset?", result["response"]["emailMessage"])

    def test_resend_uses_the_verify_copy(self):
        result = handler.handler(_event("CustomMessage_ResendCode", locale="en"))
        self.assertEqual("Confirm your email address", result["response"]["emailSubject"])
        self.assertIn("/verify?", result["response"]["emailMessage"])


class CodePlaceholderTest(unittest.TestCase):
    """Il cuore del caso d'uso: se il segnaposto non arriva intatto, il collegamento non verifica nulla."""

    def test_placeholder_reaches_the_message_verbatim(self):
        result = handler.handler(_event("CustomMessage_SignUp", locale="it"))
        message = result["response"]["emailMessage"]
        self.assertIn("code={####}", message, "Cognito sostituisce cercando esattamente {####}")
        self.assertNotIn("%7B%23%23%23%23%7D", message, "il segnaposto non va codificato nell'URL")

    def test_address_is_url_encoded_but_the_placeholder_is_not(self):
        result = handler.handler(_event("CustomMessage_SignUp", email="mario+test@esempio.test"))
        message = result["response"]["emailMessage"]
        self.assertIn("email=mario%2Btest%40esempio.test", message)
        self.assertIn("code={####}", message)

    def test_ampersand_between_parameters_is_escaped_in_html(self):
        # Senza escape il lettore di posta interpreta `&code` come inizio di entità HTML.
        result = handler.handler(_event("CustomMessage_SignUp"))
        self.assertIn("&amp;code=", result["response"]["emailMessage"])


class FailSafeTest(unittest.TestCase):
    def test_unrelated_triggers_are_left_untouched(self):
        for trigger in ("CustomMessage_Authentication", "CustomMessage_UpdateUserAttribute",
                        "CustomMessage_AdminCreateUser", ""):
            result = handler.handler(_event(trigger, locale="it"))
            self.assertEqual({}, result["response"], trigger)

    def test_missing_attributes_degrade_to_the_default_message(self):
        # Meglio l'email di default di Cognito (che il codice ce l'ha) che un collegamento rotto.
        result = handler.handler(_event("CustomMessage_SignUp", email=None))
        self.assertEqual({}, result["response"])

    def test_render_failure_does_not_break_signup(self):
        # Il trigger è sincrono: sollevare qui farebbe fallire la registrazione dell'utente, non
        # solo l'email. Si degrada al messaggio di default.
        original = handler._templates
        try:
            handler._templates = {"layout_html": "{{inesistente}}", "layout_text": "",
                                  "catalogs": {"en": {"messages": {"verify": {}}}, "it": {"messages": {}}}}
            result = handler.handler(_event("CustomMessage_SignUp"))
            self.assertEqual({}, result["response"])
        finally:
            handler._templates = original


class LoggingTest(unittest.TestCase):
    def test_logs_never_carry_the_code_or_the_address(self):
        import io
        import contextlib

        buffer = io.StringIO()
        with contextlib.redirect_stdout(buffer):
            handler.handler(_event("CustomMessage_SignUp", locale="it", email="segreto@esempio.test"))
        logs = buffer.getvalue()

        self.assertIn("custom_message_rendered", logs)
        self.assertIn('"locale": "it"', logs)
        self.assertNotIn("segreto@esempio.test", logs, "l'indirizzo non deve finire nei log")
        self.assertNotIn("{####}", logs, "il segnaposto del codice non deve finire nei log")


if __name__ == "__main__":
    unittest.main()
