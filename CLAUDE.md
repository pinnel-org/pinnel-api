# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

`pinnel-api` is the backend for **Pinnel**, a vibe-planning travel app. It is one of three repos under the `pinnel-org` GitHub org; the workspace-level `C:\dev\pinnel\CLAUDE.md` covers the cross-repo concept, MVP scope, and sibling repos (`pinnel-web`, `pinnel-docs`). Read that file for product-level context before making non-trivial backend decisions.

The repo is currently a freshly bootstrapped Spring Boot skeleton — no controllers, services, entities, or repositories exist yet. Domain code is yet to be written; expect to be establishing patterns rather than following them.

## Stack

- Java 21, Spring Boot **4.0.6** (note: 4.x, not 3.x — APIs and starter coordinates differ; e.g. `spring-boot-starter-webmvc` instead of `spring-boot-starter-web`, `spring-boot-h2console` as a dedicated starter)
- Spring Data JPA + Spring Web MVC
- PostgreSQL (prod, runtime-scoped) and H2 (local default, runtime-scoped) — both drivers are on the classpath; selection is via `application.properties` / profile config
- Lombok (annotation processor wired in `pom.xml`)
- Maven via the bundled wrapper

## Common commands

PowerShell (this is a Windows dev environment — use `.\mvnw.cmd`; the bash form `./mvnw` only works under Git Bash / WSL):

```powershell
.\mvnw.cmd spring-boot:run             # run the app on :8080
.\mvnw.cmd clean package               # build a runnable jar in target/
.\mvnw.cmd test                        # run all tests
.\mvnw.cmd test "-Dtest=ClassName"     # run a single test class
.\mvnw.cmd test "-Dtest=ClassName#method"  # run a single test method
```

Quote `-D...` args in PowerShell so the shell does not eat the `=`.

## Layout notes

- Single Maven module rooted at `pom.xml`. Base package is `org.pinnel.pinnelapi`.
- Entry point: `src/main/java/org/pinnel/pinnelapi/PinnelApi1Application.java` (`@SpringBootApplication`). The `Api1` suffix is incidental from the Spring Initializr scaffold; treat it as the canonical app class until renamed.
- **Layer-based package structure** (not feature-packages). Drop new classes into the matching layer package:
  - `controller/` — `@RestController` classes
  - `service/` — `@Service` classes
  - `repository/` — Spring Data repository interfaces
  - `entity/` — `@Entity` classes (suffix `Entity`, e.g. `UserEntity`)
  - `dto/` — request and response DTOs (suffix `Dto`, e.g. `UserDto`, `UpdateUserDto`)
- Config lives in `src/main/resources/application.properties`. The H2 console is enabled at `/h2-console`; `spring.jpa.show-sql` is on for dev.
- Tests under `src/test/java/...` mirror main packages; the only existing test is a `@SpringBootTest` context-load smoke test.

## Conventions to honor when adding code

- **Naming.** Entities end with `Entity` (`UserEntity`), DTOs end with `Dto` (`UserDto`, `UpdateUserDto`). One canonical `<Resource>Dto` per resource for responses; add `Update<Resource>Dto` / `Create<Resource>Dto` only when input fields actually differ from the response (e.g. omitting identity / server-managed fields). Never use `*Request` / `*Response` suffixes.
- **Validation lives on the DTO, not in the service.** Annotate write-DTO fields with Jakarta constraints (`@NotNull`, `@NotBlank`, `@Size`, …) and put `@Valid` on the controller's `@RequestBody` parameter (`spring-boot-starter-validation` is on the classpath). The service must not do `if (dto.x() != null)` guards — assume validated input. Practical consequence: PUT bodies use strict-replace semantics (every field required); to clear a free-text field, the client sends `""`, so reserve `@NotBlank` for fields that must always carry content.
- **Service-layer rules.**
  - No `@Transactional` on simple read methods — Spring Data already runs the repo call in a transaction. Annotate `@Transactional` only when the service does multiple repo calls or relies on dirty-checking after `findById` (update flows).
  - `deleteById` is idempotent — do **not** guard it with `existsById` and a 404. Just call `deleteById`.
  - Name private lookup helpers `getX`, not `loadX`.
  - Manage entity timestamps (`createdAt`, `updatedAt`) explicitly in the service, not via `@PrePersist` / `@PreUpdate` lifecycle callbacks.
- **Lombok** is already wired (annotation processor in the compiler plugin, excluded from the repackaged jar). Prefer `@Getter` / `@Setter` / `@RequiredArgsConstructor` / `@Slf4j`. Use constructor injection via `@RequiredArgsConstructor`; never field injection.
- **Persistence.** Spring Data JPA repositories. Test slices: `@DataJpaTest` and `@WebMvcTest` — prefer slices over full `@SpringBootTest` where possible.
- **Git workflow.** Every backlog issue gets its own branch (`feat/<slug>`) off `main`; finish by opening a PR back to `main` (`gh pr create`). Don't commit task work directly to `main`.
