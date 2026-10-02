"""Regression checks for the ECR login destination boundary; no AWS access."""
import base64
from contextlib import redirect_stderr, redirect_stdout
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from types import SimpleNamespace

SCRIPT = Path(__file__).resolve().parents[1] / "prepare-service.py"
SPEC = importlib.util.spec_from_file_location("prepare_service", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)

TEST_ACCOUNT = "123456789012"
TEST_REGISTRY = f"{TEST_ACCOUNT}.dkr.ecr.ap-northeast-2.amazonaws.com"
IDENTITY = SimpleNamespace(stdout=json.dumps({"Account": TEST_ACCOUNT}))


class EcrDestinationTests(unittest.TestCase):
    def arguments(self, output):
        registry = TEST_REGISTRY
        return [str(SCRIPT), "--profile", "test", "--output", str(output),
                "--backend-image", f"{registry}/goldenstep-backend:sha-manual-20261002T150222Z",
                "--frontend-image", f"{registry}/goldenstep-frontend:sha",
                "--algorithm-image", f"{registry}/goldenstep-ai:sha"]

    def test_invalid_images_rejected_before_cloudformation_or_output(self):
        invalid = [
            "attacker.example:5000/app:tag",
            "111111111111.dkr.ecr.ap-northeast-2.amazonaws.com/goldenstep-backend:tag",
            "123456789012.dkr.ecr.us-east-1.amazonaws.com/goldenstep-backend:tag",
            f"{TEST_REGISTRY}.attacker.example/goldenstep-backend:tag",
            f"{TEST_REGISTRY}/unrelated-repository:tag",
            f"{TEST_REGISTRY}/goldenstep-backend:tag;echo",
            f"{TEST_REGISTRY}/goldenstep-backend:tag\n",
            f"{TEST_REGISTRY}/goldenstep-backend@sha256:" + "a" * 64,
            f"{TEST_REGISTRY}/goldenstep-backend:" + "a" * 129,
        ]
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "payload.json"
            for option in ("--backend-image", "--frontend-image", "--algorithm-image"):
                for value in invalid:
                    with self.subTest(option=option, value=value):
                        args = self.arguments(output)
                        args[args.index(option) + 1] = value
                        with patch("sys.argv", args), patch.object(MODULE.subprocess, "run") as aws, redirect_stderr(io.StringIO()):
                            aws.return_value = IDENTITY
                            with self.assertRaises(SystemExit) as failure:
                                MODULE.main()
                            self.assertEqual(failure.exception.code, 2)
                            aws.assert_called_once()
                            self.assertEqual(aws.call_args.args[0][1:3], ["sts", "get-caller-identity"])
                            self.assertFalse(output.exists())

    def test_other_region_rejected_before_aws(self):
        args = self.arguments("unused.json") + ["--region", "us-east-1"]
        with patch("sys.argv", args), patch.object(MODULE.subprocess, "run") as aws, redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                MODULE.main()
            aws.assert_not_called()

    def test_invalid_sts_account_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "payload.json"
            with patch("sys.argv", self.arguments(output)), patch.object(MODULE.subprocess, "run") as aws, redirect_stderr(io.StringIO()):
                aws.return_value = SimpleNamespace(stdout=json.dumps({"Account": "bad-account"}))
                with self.assertRaises(SystemExit):
                    MODULE.main()
                aws.assert_called_once()
                self.assertFalse(output.exists())

    def test_valid_images_generate_login_to_profile_registry(self):
        outputs = {"ApplicationDatabaseSecretArn": "db-secret",
                   "ExternalApiSecretArn": "api-secret",
                   "DatabaseEndpoint": "db.example", "DatabaseName": "goldenstep"}
        response = json.dumps({"Stacks": [{"Outputs": [
            {"OutputKey": key, "OutputValue": value} for key, value in outputs.items()]}]})
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "payload.json"
            with patch("sys.argv", self.arguments(output)), patch.object(MODULE.subprocess, "run") as aws, redirect_stdout(io.StringIO()):
                aws.side_effect = [IDENTITY, SimpleNamespace(stdout=response)]
                MODULE.main()
            command = json.loads(output.read_text())["commands"][0]
            script = base64.b64decode(command.split()[2]).decode()
            self.assertIn(f"docker login --username AWS --password-stdin {TEST_REGISTRY}\n", script)
            self.assertIn("get-login-password --region ap-northeast-2", script)
            self.assertEqual(aws.call_count, 2)

    def test_service_repository_swap_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "payload.json"
            args = self.arguments(output)
            backend = args.index("--backend-image") + 1
            frontend = args.index("--frontend-image") + 1
            args[backend], args[frontend] = args[frontend], args[backend]
            with patch("sys.argv", args), patch.object(MODULE.subprocess, "run") as aws, redirect_stderr(io.StringIO()):
                aws.return_value = IDENTITY
                with self.assertRaises(SystemExit):
                    MODULE.main()
                aws.assert_called_once()
                self.assertFalse(output.exists())


if __name__ == "__main__":
    unittest.main()
