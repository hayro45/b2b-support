# Ubuntu 24 Single-Server Deployment Runbook

This runbook deploys the project on one Ubuntu host while keeping an existing personal website active.

## 1) One-time server setup

```bash
sudo apt update
sudo apt install -y git curl ca-certificates gnupg lsb-release
```

Install Docker engine and compose plugin using official Docker docs for Ubuntu.

Verify Docker before deployment:

```bash
docker version
docker compose version
```

Enable firewall:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80
sudo ufw allow 443
sudo ufw enable
```

## 2) Clone project

```bash
sudo mkdir -p /opt/b2b-support
sudo chown $USER:$USER /opt/b2b-support
cd /opt/b2b-support
git clone <your-repo-url> .
cp .env.example .env
```

Edit `.env` and set strong production values.

Important security notes:
- Set a long random Base64 value for `APP_JWT_SECRET`.
- Rotate demo user passwords before public demo if internet-exposed.

## 3) Start stack

```bash
docker compose -f infra/docker-compose.yml up -d --build
docker compose -f infra/docker-compose.yml ps
```

Check health:

```bash
curl http://127.0.0.1:8080/actuator/health
```

## 4) Nginx subdomain routing

Copy [infra/nginx/support.hayrettindal.com.conf](../../infra/nginx/support.hayrettindal.com.conf) to your host Nginx sites directory.

```bash
sudo cp infra/nginx/support.hayrettindal.com.conf /etc/nginx/sites-available/support.hayrettindal.com.conf
sudo ln -s /etc/nginx/sites-available/support.hayrettindal.com.conf /etc/nginx/sites-enabled/support.hayrettindal.com.conf
sudo nginx -t
sudo systemctl reload nginx
```

## 5) TLS certificates

```bash
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/bin/certbot
sudo certbot --nginx -d support.hayrettindal.com
```

Routing model for production:
- Frontend: `https://support.hayrettindal.com`
- API: `https://support.hayrettindal.com/api`

## 6) CI/CD secrets in GitHub

Set repository secrets:
- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`

The workflow deploys from `main` branch automatically.

## 7) Basic ops commands

```bash
cd /opt/b2b-support
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml logs -f backend
docker compose -f infra/docker-compose.yml restart backend
```

## 8) Demo login for first boot

- `agent@demo.local / demo12345`
- `customer@demo.local / demo12345`
