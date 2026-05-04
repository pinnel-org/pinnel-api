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
- Config lives in `src/main/resources/application.properties`. Currently only `spring.application.name` is set — there is no datasource config yet, so the app falls back to the embedded H2.
- Tests under `src/test/java/...` mirror main packages; the only existing test is a `@SpringBootTest` context-load smoke test.

## Conventions to honor when adding code

- Prefer Lombok (`@Getter`/`@Setter`/`@RequiredArgsConstructor`/`@Slf4j`) — it's already wired through the compiler plugin's `annotationProcessorPaths`, and the `spring-boot-maven-plugin` is configured to exclude Lombok from the repackaged jar.
- Use constructor injection (works naturally with `@RequiredArgsConstructor`); avoid field injection.
- For new persistence code, use Spring Data JPA repositories. Test slices: `@DataJpaTest` (provided by `spring-boot-starter-data-jpa-test`) and `@WebMvcTest` (provided by `spring-boot-starter-webmvc-test`) — prefer slices over full `@SpringBootTest` where possible.
