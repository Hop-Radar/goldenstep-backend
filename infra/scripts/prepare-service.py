"""Create a non-secret SSM payload for the initial GoldenStep deployment."""
import argparse
import base64
import json
from pathlib import Path
import shlex
import subprocess


def install_command(path, content, mode="600"):
    encoded = base64.b64encode(content.encode("utf-8")).decode("ascii")
    return f"printf %s {encoded} | base64 -d > {shlex.quote(path)}\nchmod {mode} {shlex.quote(path)}"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", required=True)
    parser.add_argument("--region", default="ap-northeast-2")
    parser.add_argument("--backend-image", required=True)
    parser.add_argument("--frontend-image", required=True)
    parser.add_argument("--algorithm-image", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    for value in (args.backend_image, args.frontend_image, args.algorithm_image):
        if any(c.isspace() for c in value) or "@" in value or ":" not in value:
            parser.error("Use an ECR image URI with a tag and no whitespace")
    result = subprocess.run([
        "aws", "cloudformation", "describe-stacks", "--stack-name", "goldenstep-service",
        "--profile", args.profile, "--region", args.region, "--output", "json",
    ], check=True, capture_output=True, text=True)
    outputs = {item["OutputKey"]: item["OutputValue"]
               for item in json.loads(result.stdout)["Stacks"][0]["Outputs"]}
    infra = Path(__file__).resolve().parents[1]
    config = {
        "region": args.region,
        "database_secret": outputs["ApplicationDatabaseSecretArn"],
        "api_secret": outputs["ExternalApiSecretArn"],
        "endpoint": outputs["DatabaseEndpoint"],
        "database": outputs["DatabaseName"],
    }
    # EC2 reads secrets with its instance role; secret values never enter the payload.
    remote_python = '''import json, os, subprocess
from pathlib import Path
os.umask(0o077)
config = json.loads(Path('/opt/goldenstep/runtime-config.json').read_text())
def secret(arn):
    result = subprocess.run(['aws', 'secretsmanager', 'get-secret-value',
        '--secret-id', arn, '--region', config['region'], '--output', 'json'],
        capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError('Secret retrieval failed; check instance-role permissions')
    return json.loads(json.loads(result.stdout)['SecretString'])
def checked(values):
    for key, value in values.items():
        if not isinstance(value, str) or not value or any(c in value for c in '\\r\\n\\0'):
            raise RuntimeError('Missing or invalid setting: ' + key)
    return ''.join(key + '=' + value + '\\n' for key, value in values.items())
try:
    db = secret(config['database_secret'])
    api = secret(config['api_secret'])
    backend = {'DB_URL': 'jdbc:mysql://' + config['endpoint'] + ':3306/' + config['database']
        + '?sslMode=REQUIRED&connectionTimeZone=Asia/Seoul',
        'DB_USERNAME': db['username'], 'DB_PASSWORD': db['password'],
        'CORS_ALLOWED_ORIGINS': 'https://www.goldenstep.site'}
    for key in ['NAVER_API_HUB_CLIENT_ID', 'NAVER_API_HUB_CLIENT_SECRET',
                'NAVER_MAPS_CLIENT_ID', 'NAVER_MAPS_CLIENT_SECRET']:
        backend[key] = api[key]
    frontend = {key: api[key] for key in ['NAVER_MAP_CLIENT_ID', 'TMAP_APP_KEY']}
    # Validate both files before writing either; never print their contents.
    texts = {'backend.env': checked(backend), 'frontend.env': checked(frontend)}
    for name, content in texts.items():
        target = Path('/opt/goldenstep/secrets') / name
        temporary = target.with_suffix('.tmp')
        temporary.write_text(content)
        temporary.chmod(0o600)
        temporary.replace(target)
    print('Runtime environment files: READY')
except Exception:
    raise SystemExit('Runtime environment preparation failed; verify secret keys and permissions')
'''
    env = "\n".join([
        "BACKEND_IMAGE=" + args.backend_image,
        "FRONTEND_IMAGE=" + args.frontend_image,
        "ALGORITHM_IMAGE=" + args.algorithm_image,
        "NGINX_CONFIG_PATH=/opt/goldenstep/nginx/default.conf",
        "TLS_CERT_DIR=/opt/goldenstep/letsencrypt",
        "AI_DATA_DIR=/opt/goldenstep/data",
        "BACKEND_ENV_FILE=/opt/goldenstep/secrets/backend.env",
        "FRONTEND_ENV_FILE=/opt/goldenstep/secrets/frontend.env", "",
    ])
    commands = ["set -euo pipefail", "umask 077",
        "install -d -m 700 /opt/goldenstep/secrets /opt/goldenstep/nginx",
        "test -s /opt/goldenstep/letsencrypt/live/www.goldenstep.site/fullchain.pem",
        "test -s /opt/goldenstep/letsencrypt/live/www.goldenstep.site/privkey.pem"]
    for path, content in [
        ("/opt/goldenstep/compose.prod.yaml", (infra / "compose.prod.yaml").read_text(encoding="utf-8")),
        ("/opt/goldenstep/nginx/default.conf", (infra / "nginx.prod.conf").read_text(encoding="utf-8")),
        ("/opt/goldenstep/.env.prod", env),
        ("/opt/goldenstep/runtime-config.json", json.dumps(config)),
        ("/opt/goldenstep/prepare-environment.py", remote_python),
    ]:
        commands.append(install_command(path, content))
    registry = args.backend_image.split("/")[0]
    if any(image.split("/")[0] != registry for image in
           (args.frontend_image, args.algorithm_image)):
        parser.error("All images must use the same ECR registry")
    commands += ["python3 /opt/goldenstep/prepare-environment.py",
        "cd /opt/goldenstep",
        "docker compose --env-file .env.prod -f compose.prod.yaml config --quiet",
        f"aws ecr get-login-password --region {shlex.quote(args.region)} | docker login --username AWS --password-stdin {shlex.quote(registry)}",
        "docker compose --env-file .env.prod -f compose.prod.yaml pull --quiet",
        "echo 'Deployment preparation: SUCCESS'"]
    script = "\n".join(commands) + "\n"
    payload = {"commands": [install_command("/opt/goldenstep-prepare.sh", script)
                            + "\nbash /opt/goldenstep-prepare.sh"], "executionTimeout": ["1800"]}
    Path(args.output).write_text(json.dumps(payload, ensure_ascii=True), encoding="utf-8")
    print("SSM preparation payload created (contains no secret values).")


if __name__ == "__main__":
    main()
