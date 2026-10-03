"""Initialize RDS through a local SSM tunnel without printing credentials."""

import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile


def run(command, *, data=None, env=None):
    result = subprocess.run(command, input=data, capture_output=True, env=env)
    if result.returncode:
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace"))
    return result.stdout.decode("utf-8", errors="replace")


def option_value(value):
    if any(char in value for char in "\r\n\x00"):
        raise ValueError("Unsupported control character in database credentials")
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", required=True)
    parser.add_argument("--region", default="ap-northeast-2")
    parser.add_argument("--stack", default="goldenstep-service")
    parser.add_argument("--port", type=int, default=13306)
    parser.add_argument("--mysql", default=shutil.which("mysql"))
    args = parser.parse_args()
    if not args.mysql:
        parser.error("MySQL client missing; provide --mysql with mysql.exe path")
    aws = ["aws", "--profile", args.profile, "--region", args.region]
    sensitive = []

    try:
        stack = json.loads(run(aws + ["cloudformation", "describe-stacks",
                                     "--stack-name", args.stack, "--output", "json"]))
        outputs = {item["OutputKey"]: item["OutputValue"]
                   for item in stack["Stacks"][0]["Outputs"]}
        database = outputs["DatabaseName"]
        if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*", database):
            raise ValueError("Unexpected database name")

        def secret(key):
            response = json.loads(run(aws + ["secretsmanager", "get-secret-value",
                                            "--secret-id", outputs[key], "--output", "json"]))
            credentials = json.loads(response["SecretString"])
            sensitive.append(credentials["password"])
            return credentials

        admin = secret("DatabaseAdminSecretArn")
        app = secret("ApplicationDatabaseSecretArn")
        if app["username"] != "goldenstep_app" or not re.fullmatch(r"[A-Za-z0-9]{32}", app["password"]):
            raise ValueError("Application secret does not match the template's generated credentials")
        schema = Path(__file__).resolve().parents[2] / "src/main/resources/schema.sql"
        schema_sql = schema.read_text(encoding="utf-8-sig")

        # Credentials are stored only in an access-restricted temporary directory.
        with tempfile.TemporaryDirectory(prefix="goldenstep-db-") as temporary:
            directory = Path(temporary)
            if directory.resolve().parent != Path(tempfile.gettempdir()).resolve():
                raise RuntimeError("Unexpected temporary directory")
            if os.name == "nt":
                identity = run(["whoami"]).strip()
                run(["icacls", str(directory), "/inheritance:r", "/grant:r",
                     identity + ":(OI)(CI)F"])
            else:
                directory.chmod(0o700)

            def mysql(credentials, sql):
                config = directory / "client.cnf"
                config.write_text("[client]\nuser=" + option_value(credentials["username"])
                                  + "\npassword=" + option_value(credentials["password"])
                                  + "\n", encoding="utf-8")
                config.chmod(0o600)
                client_env = os.environ.copy()
                client_env.pop("MYSQL_PWD", None)
                # MySQL 8.0 lacks --no-login-paths; isolate the login file instead.
                client_env["MYSQL_TEST_LOGIN_FILE"] = str(directory / "unused.mylogin.cnf")
                return run([args.mysql, "--defaults-file=" + str(config),
                            "--host=127.0.0.1", "--port=" + str(args.port), "--protocol=TCP",
                            "--ssl-mode=REQUIRED", "--connect-timeout=10",
                            "--default-character-set=utf8mb4", "--batch", "--raw",
                            "--skip-column-names", "--database=" + database],
                           data=sql.encode("utf-8"), env=client_env)

            mysql(admin, "SELECT 1;")
            print("Administrator connection: OK")
            mysql(admin, schema_sql)
            print("Repository schema applied: OK")
            password = app["password"]  # Validated alphanumeric before SQL interpolation.
            mysql(admin, f"""
CREATE USER IF NOT EXISTS 'goldenstep_app'@'%' IDENTIFIED BY '{password}' REQUIRE SSL;
ALTER USER 'goldenstep_app'@'%' IDENTIFIED BY '{password}' REQUIRE SSL;
GRANT SELECT, INSERT, UPDATE, DELETE ON `{database}`.* TO 'goldenstep_app'@'%';
""")
            print("Application user provisioned: OK")
            tables = mysql(app, "SHOW TABLES;").splitlines()
            expected = {"search_session", "analysis_run", "time_result", "priority_place",
                        "place_check", "snapshot", "snapshot_place"}
            if not expected.issubset(set(tables)):
                raise RuntimeError("Expected application tables are missing")
            print("Application connection: OK")
            print("Tables (7 required): " + ", ".join(tables))
            print("Database bootstrap: SUCCESS")
    except (RuntimeError, ValueError, KeyError, OSError) as error:
        message = str(error)
        for password in sensitive:
            if password:
                message = message.replace(password, "[REDACTED]")
        print("Database bootstrap failed: " + message, file=sys.stderr)
        print("Earlier successful DDL may remain; inspect the error before retrying.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
