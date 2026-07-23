# Repository Guidelines

## Project Structure & Module Organization

- Multi‑module Maven build. Key modules: `compiler` (core), `javascript`, `handlebar`, `scala-extensions/*`, `mustache-maven-plugin`, `example`, `benchmarks`.
- Standard layout per module: `src/main/java|resources`, `src/test/java|resources` (Scala tests in `scala-extensions-*/src/test/scala`).
- Examples live under `example/src/main`, benchmarks under `benchmarks/src/main` and `benchmarks/src/main/resources`.

## Build, Test, and Development Commands

- Build all modules: `mvn -q package` (use `-DskipTests` to skip tests).
- Run all tests: `mvn -q test`.
- Build/test a single module (with deps): `mvn -q -pl compiler -am test`.
- Clean: `mvn -q clean`.
- Optional coverage (if configured): `mvn -q test jacoco:report`.

## Coding Style & Naming Conventions

- Language: Java (and some Scala in `scala-extensions`). Follow the existing two‑space indent and K&R braces in Java sources.
- Packages: lowercase; Classes: `PascalCase`; methods/fields: `lowerCamelCase`.
- Tests end with `*Test` and mirror source package structure.
- Keep imports organized; avoid unused code; prefer immutability (`final`) where sensible. No strict linter is enforced—match nearby style.

## Testing Guidelines

- Frameworks: JUnit 4 for Java tests; Scala tests use JUnit annotations under `scala-extensions`.
- Add tests for new features and bug fixes—focus on `compiler` behavior and edge cases (whitespace, partials, recursion limits).
- Run a single test: `mvn -q -Dtest=JavascriptObjectHandlerTest test` (adjust class name as needed).

## Commit & Pull Request Guidelines

- Commits: concise, imperative subject; reference issues when relevant (e.g., `ISSUE #257: clarify dynamic partials`).
- PRs must include: clear description, linked issues, tests or rationale for test impact, and any doc/example updates.
- CI hygiene: ensure `mvn -q package` and `mvn -q test` pass locally. Avoid breaking public APIs in `com.github.mustachejava` without discussion.
- If rendering output changes, include sample template and before/after output in the PR.

## Agent-Specific Instructions

- Keep changes scoped to the smallest relevant module. Do not alter public APIs lightly—preserve backwards compatibility and spec‑compliant whitespace behavior.
- Update README/examples only when behavior changes; otherwise keep diffs minimal.