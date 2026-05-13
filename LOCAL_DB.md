# simulation-service Local DB Settings

`simulation-service` local profile imports configuration from Config Server.

If Config Server is running with the default Git backend, it serves the remote GitHub `configs` repository, not local edits under `../configs`.

## Quick Override

First make sure the Docker PostgreSQL container is running. If it is not running, `localhost:5432` may point to another local PostgreSQL instance and authentication can fail even when the service config looks correct.

```bash
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}'
pg_isready -h localhost -p 5432 -U postgres
```

Expected container:

```text
finlearn-postgres   postgres:16   0.0.0.0:5432->5432/tcp   Up ...
```

Use `update` for the local schema mode while testing manually. `create-drop` deletes tables when a second app process fails to start or exits.

```bash
SPRING_JPA_HIBERNATE_DDL_AUTO=update \
sh gradlew bootRun
```

If the running PostgreSQL password is not `postgres`, override Spring datasource directly:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/simulationdb \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=<actual-password> \
SPRING_JPA_HIBERNATE_DDL_AUTO=update \
sh gradlew bootRun
```

Spring's `SPRING_DATASOURCE_*` variables override values from Config Server.

## Use Local Configs

Start `config-server` with the native backend:

```bash
cd /Users/seongjun/Desktop/spartacoding/finlearn/config-server

CONFIG_LOCAL_REPO_PATH=/Users/seongjun/Desktop/spartacoding/finlearn/configs \
SPRING_PROFILES_ACTIVE=native \
sh gradlew bootRun
```

Then run simulation-service with DB variables:

```bash
cd /Users/seongjun/Desktop/spartacoding/finlearn/simulation-service

DB_HOST=localhost \
DB_PORT=5432 \
DB_NAME=simulationdb \
DB_USERNAME=postgres \
DB_PASSWORD=<actual-password> \
sh gradlew bootRun
```

## Port Collision

If Docker Compose PostgreSQL is intended but local PostgreSQL already uses `localhost:5432`, the service will connect to the local PostgreSQL instead of the container.

Check:

```bash
pg_isready -h localhost -p 5432 -U postgres
```

If a local `/Library/PostgreSQL/...` server is running on 5432, either stop it, map Docker PostgreSQL to a different host port, or set:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:<docker-host-port>/simulationdb
```

## Existing Volume Warning

Changing `POSTGRES_PASSWORD` in `.env` after a PostgreSQL data volume already exists does not change the existing `postgres` user's password. Use the existing password, reset it manually, or recreate the local volume.
