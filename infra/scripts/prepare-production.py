#!/usr/bin/env python3
"""One-time, local-to-server secret/account provisioning. Never prints secrets.

Run as the deployment operator. Files use exclusive creation and mode 0600.
Existing database passwords are retained, not rotated by changing an env file.
"""
import argparse
import base64
import json
import os
from pathlib import Path
import secrets
import subprocess


def private_json(path, value):
    with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w") as stream:
        json.dump(value, stream, indent=2)
        stream.write("\n")


def container_env(name):
    result = subprocess.run(["docker", "inspect", name], check=True, capture_output=True, text=True)
    return dict(item.split("=", 1) for item in json.loads(result.stdout)[0]["Config"]["Env"] if "=" in item)


def psql(sql):
    result = subprocess.run(
        ["docker", "exec", "-i", "support-postgres", "sh", "-c",
         'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -A'],
        input=sql, text=True, capture_output=True,
    )
    if result.returncode:
        raise RuntimeError("Database provisioning failed; credentials were not printed")
    return result.stdout.strip()


def prepare_env(path):
    if path.exists():
        raise RuntimeError("Production env already exists; refusing to overwrite")
    database = container_env("support-postgres")
    backend = container_env("support-backend")
    password = backend.get("SPRING_DATASOURCE_PASSWORD")
    if not password or password == "support_pass":
        raise RuntimeError("Existing database password is absent or the development default")
    values = {
        "POSTGRES_DB": database["POSTGRES_DB"],
        "POSTGRES_USER": database["POSTGRES_USER"],
        "POSTGRES_PASSWORD": password,
        "APP_JWT_SECRET": base64.b64encode(secrets.token_bytes(48)).decode(),
        "APP_CORS_ALLOWED_ORIGINS": "https://support.hayrettindal.com",
        "APP_JWT_EXPIRATION_SECONDS": "3600",
        "VITE_API_BASE_URL": "/api",
    }
    # Compose dotenv supports literal values in single quotes. Refuse unexpected
    # quoting/control characters instead of silently changing an existing secret.
    if any(any(char in value for char in "'\r\n\x00") for value in values.values()):
        raise RuntimeError("Existing env value requires operator-reviewed quoting")
    with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w") as stream:
        for name, value in values.items():
            stream.write(f"{name}='{value}'\n")
    print("Private production env prepared; existing database password retained; JWT key rotated.")


def provision_owner(path):
    email = "owner@support.hayrettindal.com"
    if path.exists():
        raise RuntimeError("Credential file already exists; refusing to overwrite")
    if psql(f"SELECT count(*) FROM app_users WHERE lower(email) = '{email}';") != "0":
        raise RuntimeError("Owner account already exists; refusing to change its password")
    password = secrets.token_urlsafe(32)
    # Persist the only copy privately BEFORE the transaction. On a database
    # failure the file remains recoverable; no existing account is modified.
    private_json(path, {"url": "https://support.hayrettindal.com", "email": email, "password": password})
    result = psql(f"""BEGIN;
INSERT INTO app_users (id, organization_id, full_name, email, role, is_active, password_hash)
SELECT gen_random_uuid(), id, 'Portfolio Owner', '{email}', 'ADMIN', true,
       '{{bcrypt}}' || crypt('{password}', gen_salt('bf', 12))
FROM organizations ORDER BY created_at, id LIMIT 1;
COMMIT;
SELECT count(*) FROM app_users WHERE email = '{email}' AND is_active;
""")
    if not result.endswith("1"):
        raise RuntimeError("Owner creation did not produce one active account; inspect locally")
    print("Private owner account provisioned with BCrypt; credential file is mode 0600.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=["prepare-env", "provision-owner"])
    parser.add_argument("path", type=Path, help="New private output file; never committed")
    args = parser.parse_args()
    if args.operation == "prepare-env":
        prepare_env(args.path)
    else:
        provision_owner(args.path)
