# Deployment

The backend ships to a single EC2 instance. There is **no deploy on merge to
`main`** — deploys are triggered manually.

## Pipeline

| Workflow | File | Trigger | What it does |
|----------|------|---------|--------------|
| CI | `.github/workflows/ci.yml` | PR opened / updated against `main` | `./mvnw clean test` |
| Deploy | `.github/workflows/deploy.yml` | Manual (`workflow_dispatch`) | Builds the chosen branch and ships the JAR to EC2 |

To deploy: **Actions → Deploy → Run workflow**, pick the branch, run it. The job
builds `pinnel-api-*.jar`, copies it to the instance, restarts the service, and
polls `/actuator/health`. If the health check fails it restores the previous
JAR and the workflow fails — the running version is left in place.

This is a pre-MVP setup: the JAR is copied straight to the host with no S3
artifact store and no versioning. A future iteration will move to ECR.

## GitHub Actions secrets

Set these under **Settings → Secrets and variables → Actions**:

| Secret | Value |
|--------|-------|
| `EC2_HOST` | Public IP or DNS of the EC2 instance |
| `EC2_USER` | SSH login user (`ec2-user` on Amazon Linux, `ubuntu` on Ubuntu) |
| `EC2_SSH_KEY` | Private key (full PEM contents) for that user |

No AWS access keys are needed — the deploy connects over plain SSH.

## One-time EC2 host setup

Reproduce this on a rebuilt instance.

### 1. Java runtime

Install a Java 21 JRE/JDK, e.g. on Amazon Linux 2023:

```bash
sudo dnf install -y java-21-amazon-corretto-headless
```

### 2. Application directory

```bash
sudo mkdir -p /opt/pinnel-api
```

### 3. Environment file

The `prod` profile reads the datasource from environment variables. Create
`/etc/pinnel-api/pinnel-api.env` (root-owned, `chmod 600` — it holds the DB
password):

```ini
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=80
SPRING_DATASOURCE_URL=jdbc:postgresql://<rds-endpoint>:5432/pinnel
SPRING_DATASOURCE_USERNAME=pinnel
SPRING_DATASOURCE_PASSWORD=<password>
```

### 4. systemd unit

Create `/etc/systemd/system/pinnel-api.service`:

```ini
[Unit]
Description=Pinnel API
After=network.target

[Service]
WorkingDirectory=/opt/pinnel-api
EnvironmentFile=/etc/pinnel-api/pinnel-api.env
ExecStart=/usr/bin/java -jar /opt/pinnel-api/pinnel-api.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

`SERVER_PORT=80` in the env file makes the app serve on port 80. The service
runs as `root` (systemd's default when no `User=` is set), which is fine for
pre-MVP — it can bind the privileged port directly. A later hardening step is to
run it as a dedicated non-root user with
`AmbientCapabilities=CAP_NET_BIND_SERVICE`.

Then enable it (it will fail to start until the first deploy lands a JAR):

```bash
sudo systemctl daemon-reload
sudo systemctl enable pinnel-api
```

### 5. Deploy permissions

The deploy workflow runs `sudo` on the host to move the JAR into
`/opt/pinnel-api` and restart the service. The default instance user
(`ec2-user` / `ubuntu`) already has password-less `sudo`, so no extra config is
needed when `EC2_USER` is that account.

If you instead create a dedicated deploy user, grant it password-less sudo,
e.g. in `/etc/sudoers.d/pinnel-deploy`:

```
deploy ALL=(ALL) NOPASSWD: /bin/mv, /bin/cp, /bin/systemctl restart pinnel-api
```

### 6. Security group

Allow inbound TCP **22** from the network that runs the deploy. GitHub-hosted
runners use a wide, changing set of IP ranges, so this effectively means
opening 22 broadly — acceptable for pre-MVP with key-only auth. Hardening
options for later: a self-hosted runner in the VPC, or SSM Session Manager
port-forwarding so port 22 needs no public ingress at all.

Port **80** must be open for normal API traffic. The deploy's health check runs
on the instance against `localhost`, so it adds no extra security-group
requirement.

## Rollback

Each deploy backs up the live JAR to `pinnel-api.jar.bak` before overwriting.
If the post-restart health check fails, the workflow restores the backup and
restarts the service, so the previously working version keeps running. To roll
back manually:

```bash
sudo mv -f /opt/pinnel-api/pinnel-api.jar.bak /opt/pinnel-api/pinnel-api.jar
sudo systemctl restart pinnel-api
```
