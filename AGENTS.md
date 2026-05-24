# Gateway Agent Guide

## Scope

`gateway` is the public backend and workflow orchestrator. It is Java 21 with Spring Boot 3.5, WebFlux, R2DBC/PostgreSQL, Spring Security/Cognito integration, MapStruct, Anthropic integration, internal file/subscription clients, and optional Elasticsearch/OpenSearch behavior.

When this repository is opened inside the Lexai parent workspace, also follow `../devtools/docs/agents/WORKSPACE.md`, `../devtools/docs/agents/ARCHITECTURE.md`, and `../devtools/docs/agents/DELIVERY.md`.

## Package Responsibilities

| Package | Responsibility |
| --- | --- |
| `rest/controller` | HTTP routing, request validation, response mapping only |
| `rest/dto` | Explicit API contracts |
| `service` | Domain workflows, access-sensitive operations, external orchestration |
| `data/entity` and `data/repository` | Persistence model and reactive queries |
| `mapper` | DTO/entity transformations through existing MapStruct patterns |
| `security` and `security/access` | Authentication, subscription gates, ownership permissions |
| `config` | Runtime clients, security and framework configuration |
| `exception` | Sanitized HTTP error translation |

## Reactive Backend Rules

- Preserve non-blocking request paths: return and compose `Mono`/`Flux`; do not call `block()`, use blocking database clients, or call `subscribe()` inside request/service logic.
- Keep controllers thin and put workflows in services. Keep mapping in mappers and query intent in repositories.
- Prefer Reactor operators for dependent work and error translation. Make concurrency intentional when combining external AI, file, or billing operations.
- Do not expose entities directly as public response contracts when a DTO exists or is required.
- Use R2DBC-safe persistence patterns; do not introduce JPA assumptions into reactive repositories.

## Domain And Security

- Reuse `AccessPermissionService` and existing security patterns before accessing team-, case-, document-, legal-entity-, or storage-file-owned resources.
- Authentication, cookie/JWT behavior, OAuth/Cognito flows, subscription gating, and CORS changes are security-sensitive; test successful and rejected cases.
- AI requests may contain confidential documents. Bound context/content handling, avoid content logs, and treat model output as untrusted until validated for its use.
- File operations must retain domain ownership checks before delegating S3 actions to `file-service`.
- Subscription information is supplied by `subscription-service`; avoid inventing billing truth in gateway state.

## Data And Contracts

- Public API changes require checking `client-front`; internal file or subscription client changes require checking the relevant NestJS service and `infra` URL/route wiring.
- When changing persisted domain models, update schema/bootstrap behavior and test data patterns already used by this service.
- Use sanitized, useful exception responses; never return third-party secrets or raw sensitive payloads.

## Commands

Run from `gateway`:

```bash
./mvnw test
./mvnw spring-boot:run
```

Use the running local environment where appropriate; do not start containers solely for routine code changes.
