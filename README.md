# pinnel-api

Backend service for **Pinnel** — a vibe-planning travel app for building day-by-day trips around places worth experiencing.

See [pinnel-docs](https://github.com/pinnel-org/pinnel-docs) for the full project description, architecture, and roadmap.

## Tech

- **Java 21**
- **Spring Boot 4.0.6** (Web MVC, Data JPA)
- **PostgreSQL** (prod) / **H2** (local dev)
- **Lombok**
- **Maven** (wrapper included)

## Run locally

```bash
./mvnw spring-boot:run
```

The app starts on port 8080 by default. H2 is used out of the box; PostgreSQL config will be added per environment.

## Build

```bash
./mvnw clean package
```

## Test

```bash
./mvnw test
```

## Related repos

| Repo | Purpose |
|------|---------|
| [pinnel-api](https://github.com/pinnel-org/pinnel-api) | Backend (this repo) |
| [pinnel-web](https://github.com/pinnel-org/pinnel-web) | Frontend — React SPA |
| [pinnel-docs](https://github.com/pinnel-org/pinnel-docs) | Architecture & docs |
