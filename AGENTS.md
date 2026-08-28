# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 Spring Boot/Maven application for Ounce Market. Main code lives in `src/main/java/ounce/market/demo`, organized by domain such as `member`, `product`, `order`, `cart`, `coupon`, `delivery`, `search`, and `common`. Keep new features in the matching domain package and follow the existing `controller`, `service`, `repository`, `entity`, and `dto` subpackage pattern.

Templates are in `src/main/resources/templates`, reusable Thymeleaf fragments are in `templates/fragments`, static CSS/JS/images are in `src/main/resources/static`, and Elasticsearch mappings/settings are in `src/main/resources/elastic`. Tests belong under `src/test/java` with the same package path as the code under test. Local Docker and deployment support files are at the repository root, with additional docs in `docs/` and `LOCAL.md`.

## Build, Test, and Development Commands

- `./mvnw test`: runs the Maven test suite.
- `./mvnw -DskipTests package`: builds `target/demo-0.0.1-SNAPSHOT.jar` for Docker image creation.
- `./mvnw spring-boot:run`: starts the app directly from Maven when local config is available.
- `docker compose up -d`: starts the local stack when `.env` sets `COMPOSE_FILE=docker-compose.yml:docker-compose.local.yml`.
- `docker compose logs -f --tail=100 app-blue`: follows application logs.
- `k6 run load-test.js` or `k6 run flash-sale.js`: runs load scripts after editing their target URLs and payloads.

## Coding Style & Naming Conventions

Use the repository’s existing Java style: 4-space indentation, package-private test methods, Lombok where already established, and clear Spring stereotypes such as `*Controller`, `*Service`, and `*Repository`. Name DTOs by direction and purpose, for example `CartAddRequest` or `ProductSearchResponse`. Keep Thymeleaf templates lowercase with hyphens where needed, and keep browser scripts in `static/js` named after their page or component.

## Testing Guidelines

Tests use JUnit 5, Mockito, Spring Boot Test, and Spring Security Test. Prefer focused unit tests for service behavior and repository/controller integration tests when framework wiring matters. Name test classes `*Test`; use descriptive method names and `@DisplayName` when it clarifies Korean or business-facing scenarios.

## Commit & Pull Request Guidelines

Recent history mostly uses short Conventional Commit-style prefixes such as `feat:` and `chore:` followed by concise Korean summaries. Keep commits focused and use prefixes like `feat:`, `fix:`, `test:`, `docs:`, or `chore:`. Pull requests should summarize behavior changes, list verification commands, link related issues, and include screenshots for template or static UI changes.

## Security & Configuration Tips

Do not commit `.env` or `src/main/resources/application*.yml`; these are ignored and CI injects production configuration from secrets. Avoid logging JWTs, passwords, OAuth tokens, database passwords, or customer data. Use `LOCAL.md` for Docker setup details before changing compose files.
