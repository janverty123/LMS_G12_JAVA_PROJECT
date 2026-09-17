# Run four separate Classify copies on Windows 11

Run these steps **on each of the four computers**. Each computer gets its own
accounts, classes, database, and uploaded files. Changes do not synchronize
between computers or with the deployed website. A new installation starts empty;
register new teacher/student accounts. This guide does not confirm the live
website's deployment status or copy its data.

## Can everything be installed with one command?

**Most software installation can be automated, but a fresh Windows computer
cannot be guaranteed a completely unattended, single-command setup.** Windows
may need administrator approval, a restart, hardware virtualization enabled in
BIOS/UEFI, and Docker's first-run agreement. Downloading dependencies initially
requires internet access. Once downloaded and running, the local LMS does not
require the deployed website.

The one-line installer below installs the host applications. The later commands
clone and start Classify. This repository does not currently provide a single
installer that handles Windows setup, reboots, configuration, and app startup.

## Requirements on each computer

| Requirement | How it is supplied |
| --- | --- |
| Supported, updated 64-bit Windows 11 | Already installed on the computer |
| Hardware virtualization enabled; WSL 2 | Firmware setting and Windows command below |
| At least 8 GB RAM; 16 GB recommended for comfortable use | Hardware; 16 GB is a practical recommendation, not a benchmark |
| About 20 GB free disk space initially, plus space for uploads | Planning estimate; usage grows with images, build cache, and files |
| Git | One-line WinGet install below |
| Docker Desktop with Linux containers and Compose v2 | One-line WinGet install below |
| Node.js LTS and bundled npm | One-line WinGet install below |
| Java 21 and Maven 3.9.9 | Downloaded inside the existing backend Docker build; no host installation needed |
| PostgreSQL 16 and MinIO | Downloaded by the repository's Docker Compose file |
| Edge, Chrome, or Firefox | Existing browser is sufficient |
| Internet and permission to install applications | Needed for initial downloads; private GitHub repositories also require access |

Docker's supported Windows/WSL versions and hardware requirements are listed in
its [Windows installation documentation](https://docs.docker.com/desktop/setup/install/windows-install/).
This is a local development/demo setup, using the repository's development
credentials and Vite server. Use it on a trusted computer/network; it is not a
production deployment configuration.

## 1. Install the host applications

Open **PowerShell as Administrator**. Confirm that WinGet is available:

```powershell
winget --version
```

If it is missing, install/update Microsoft's **App Installer** through Microsoft
Store, then reopen PowerShell. Run this single line to install the three host
applications:

```powershell
winget install --id Git.Git --exact --source winget --accept-source-agreements --accept-package-agreements; winget install --id Docker.DockerDesktop --exact --source winget --accept-source-agreements --accept-package-agreements; winget install --id OpenJS.NodeJS.LTS --exact --source winget --accept-source-agreements --accept-package-agreements
```

The agreement flags accept installer/source agreements. Review each command's
result: PowerShell's semicolons continue to the next installer even if an earlier
one fails. Resolve failures before proceeding. WinGet supports command-driven
installation; see [Microsoft's install reference](https://learn.microsoft.com/en-us/windows/package-manager/winget/install).

If WSL is not installed, run:

```powershell
wsl --install --no-distribution
```

Restart Windows if requested. Then run in an administrator PowerShell:

```powershell
wsl --update
wsl --version
```

See Microsoft's [WSL installation guide](https://learn.microsoft.com/en-us/windows/wsl/install).
If virtualization is disabled, enable it in BIOS/UEFI; a normal application
installer cannot reliably do that for you.

Open Docker Desktop from Start, complete its first-run setup, and use **Linux
containers** with the WSL 2 backend. Wait until the engine is running. Open a
**new, regular PowerShell** so the installed programs are on PATH, then verify:

```powershell
git --version
node --version
npm.cmd --version
docker version
docker compose version
```

`docker version` must show a working server, not just the client. This guide uses
`npm.cmd` to avoid PowerShell blocking the `npm.ps1` launcher.

## 2. Download the project

In regular PowerShell, in a folder where you want to keep the project:

```powershell
git clone https://github.com/janverty123/LMS_G12_JAVA_PROJECT.git
Set-Location LMS_G12_JAVA_PROJECT
```

If you already cloned it, open that folder instead. Do not overwrite an existing
installation or its configuration. For a private repository, sign in when Git
requests access.

## 3. Start the database and file storage

Run from the repository root:

```powershell
$env:FRONTEND_ORIGIN = "http://localhost:5173"
docker compose up -d --wait
```

Both services must become healthy. The existing Compose file starts **only
PostgreSQL and MinIO**, not the backend or frontend.

Find this computer's IPv4 address:

```powershell
ipconfig
```

Use the IPv4 address of your active Wi-Fi/Ethernet adapter, for example
`192.168.1.20`. Do not use the WSL/Docker virtual adapter. Set it below, replacing
the example with **this computer's** address:

```powershell
$classifyIp = "192.168.1.20"
```

Why an address is needed even for a separate local copy: the backend runs inside
Docker, while uploads go directly from the browser to MinIO using signed URLs.
The configured MinIO address must work from both places. `localhost` inside the
backend container would refer to that container, not MinIO. Keep the computer
connected to its local network; internet is not required after setup. Reserving
its address in the router avoids configuration changes later.

## 4. Build and start the backend

In the same PowerShell, at the repository root:

```powershell
docker build -t classify-backend:local ./backend
```

Wait for a successful build, then run:

```powershell
docker run -d --name classify-backend --restart unless-stopped -p 127.0.0.1:8080:8080 -e SPRING_PROFILES_ACTIVE=dev -e DB_URL=jdbc:postgresql://host.docker.internal:5432/apptitle -e DB_USERNAME=apptitle -e DB_PASSWORD=apptitle_dev_password -e "MINIO_ENDPOINT=http://${classifyIp}:9000" -e MINIO_ACCESS_KEY=apptitle_admin -e MINIO_SECRET_KEY=apptitle_dev_password -e MINIO_BUCKET=apptitle-files -e MINIO_INITIALIZE_BUCKET=true -e FRONTEND_ORIGIN=http://localhost:5173 classify-backend:local
```

Docker Desktop supplies `host.docker.internal` for container-to-host access.
The backend uses the development profile to create the fresh database schema
and storage bucket. No SQL migration commands are needed for this fresh setup.
Do not apply this initialization procedure to an existing production database.

Check startup:

```powershell
docker logs --tail 100 classify-backend
Invoke-RestMethod http://localhost:8080/api/health
```

Wait for startup to finish before checking health. If startup failed, inspect
the logs and check that PostgreSQL and MinIO are healthy and the IP is correct.

## 5. Start the frontend and use Classify

In a PowerShell at the repository root:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort
```

Leave that terminal open. Open **http://localhost:5173** on this computer.
The frontend proxies `/api` to the local backend. For a clean clone, no frontend
`.env` is needed. If reusing an old checkout, remove any deployed
`VITE_API_BASE_URL` override from its environment/`.env` files so requests stay
local. Spring Boot does not automatically load `backend/.env` files; this guide
passes its configuration explicitly through Docker.

Repeat steps 1–5 on all four computers, using each computer's own IPv4 address.
Using `localhost:5173` on each computer opens that computer's independent copy.
There is no need to open inbound frontend or backend access for the other PCs.
The existing Compose file publishes database/storage ports; keep those limited
to your trusted network with the host firewall. Do not configure router port
forwarding.

## 6. Verify each installation

1. Confirm `docker compose ps` shows healthy PostgreSQL and MinIO services.
2. Confirm `http://localhost:8080/api/health` responds.
3. Register a teacher and student and confirm login works.
4. Create a Class Section and Subject, request/approve the subject link, then
   request/approve the student's section enrollment.
5. Upload a learning material, then download it as an authorized student.
   This checks the direct-to-MinIO path, which login alone does not exercise.
6. Verify an account created on one computer is absent on another.

## Start again after a shutdown

Open Docker Desktop and wait for its engine. In PowerShell at the repository
root, run:

```powershell
docker compose up -d --wait
docker start classify-backend
Set-Location frontend
npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort
```

You do not need to reinstall applications, rebuild the backend, or run `npm ci`
every time. After dependency/source updates, rebuild the backend and reinstall
frontend dependencies as appropriate. If the backend restarted before the
database was ready, run `docker restart classify-backend` after Compose is healthy.

To stop, press **Ctrl+C** in the frontend terminal. From the repository root:

```powershell
docker stop classify-backend
docker compose stop
```

Database records and uploaded files persist in Docker volumes. **Do not run
`docker compose down -v` or delete the volumes in Docker Desktop unless you
intend to erase that computer's local data.** Back up both PostgreSQL and MinIO
before moving or resetting an installation; Git does not store their data.

## Troubleshooting

| Problem | What to check |
| --- | --- |
| Command is not recognized | Reopen PowerShell; check the install command succeeded. |
| Docker cannot connect to its engine | Start Docker Desktop; check WSL and virtualization. |
| Port 5173 is already in use | Stop the other process; keep 5173 to match the configured origin. |
| Backend cannot connect to PostgreSQL | Check `docker compose ps` and the backend logs. |
| Login works but uploads/downloads fail | Check `http://YOUR-PC-IP:9000/minio/health/live`, the configured IP, firewall, and MinIO health; use exactly `http://localhost:5173` as browser origin. |
| Windows/network firewall blocks MinIO | Allow Docker's storage traffic on your trusted/private network according to the computer's policy; do not disable the firewall. |
| Computer changes network/IP address | Recreate the backend container with the new `MINIO_ENDPOINT` as described below. |
| Backend container name already exists | Use `docker start classify-backend` for the existing installation. |
| Local accounts differ from the deployed site | Expected: this is a separate database, with no automatic data synchronization. |

To change the backend's configured IP, stop and remove **only its app container**:

```powershell
docker stop classify-backend
docker rm classify-backend
```

Set `$classifyIp` to the new address and repeat the `docker run` command from
step 4. Database and uploaded files are in the separate Compose volumes and
remain intact. Refresh the browser to request new signed file URLs.

For the alternative native Java/Maven development workflow, see [SETUP.md](SETUP.md).
The commands above were checked against the repository configuration; a fresh
Windows 11 installation and four-computer acceptance test must still be run on
the actual machines.
