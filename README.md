# Claims Management API

A Java 17 / Spring Boot modular-monolith starter for claim intake, review, manager decisions, reporting, and status-change notifications. It provides REST APIs only; Swagger UI is for API exploration and verification, not a claimant-facing frontend.

[View the application flow diagrams](docs/application-flow.md).

## Package layout

- `claim.domain` and `claim.application` contain claim state and use cases.
- `controller` contains the REST controllers and API exception handling.
- `notification.application` contains notification use cases and module contracts.
- `notification.infrastructure.kafka` contains Kafka configuration and adapters.
- `notification.infrastructure.persistence` contains JPA entities, repositories, and adapters.
- `notification.infrastructure.sender` contains the demo SMS/email sender adapter.

## Run locally

Prerequisites: Java 17+, Maven 3.9+, and (for Kafka notifications) a Kafka broker at `localhost:9092`.

The demo API uses HTTP Basic authentication. Set three local passwords before starting; do not commit real credentials:

```powershell
$env:CLAIMANT_PASSWORD = "replace-with-a-local-secret"
$env:STAFF_PASSWORD = "replace-with-a-local-secret"
$env:MANAGER_PASSWORD = "replace-with-a-local-secret"
mvn spring-boot:run
```

The application fails fast if any demo password is missing. Swagger UI is at `http://localhost:8080/swagger-ui.html`; OpenAPI JSON is at `/v3/api-docs`. Use the `claimant`, `staff`, and `manager` accounts in Swagger's **Authorize** dialog.

The H2 database is in-memory and resets when the process stops. The H2 console is disabled. H2 is for local development and tests only.

Kafka notifications are emitted from a transactional outbox only when a claimant-visible status changes. The consumer uses a demo notification adapter that logs the channel and status; it does not send real SMS or email. Configure a provider-specific adapter before enabling real delivery. Set `KAFKA_BOOTSTRAP_SERVERS` to use another broker.

## API outline

| API | Purpose |
| --- | --- |
| `POST /api/v1/claims` | Claimant submits an incident |
| `GET /api/v1/claims` | Claimant lists their own claims |
| `GET /api/v1/claims/{claimId}` | Claimant tracks their own claim |
| `POST /api/v1/claims/{claimId}/information-responses` | Claimant responds to a request |
| `GET /api/v1/staff/work-queue` | Staff views unassigned or assigned-to-self work |
| `POST /api/v1/staff/claims/{claimId}/pickup` | Staff picks up a submitted claim |
| `POST /api/v1/staff/claims/{claimId}/information-requests` | Staff requests claimant information |
| `POST /api/v1/staff/claims/{claimId}/assessment` | Staff submits a recommendation |
| `POST /api/v1/staff/claims/{claimId}/settlement-completion` | Staff marks an approved settlement complete |
| `GET /api/v1/staff/team-workload` | Staff or manager views team workload |
| `GET /api/v1/manager/dashboard` | Manager views backlog and claimed-amount exposure by currency |
| `POST /api/v1/manager/claims/{claimId}/decision` | Manager makes the final settlement/rejection decision |

Manager exposure is the sum of claimed amounts for non-terminal claims, grouped by currency; it is not an actuarial reserve or liability estimate.

## Claimant-visible statuses

`SUBMITTED`, `IN_REVIEW`, `AWAITING_INFORMATION`, `PENDING_MANAGER_DECISION`, `SETTLEMENT_IN_PROGRESS`, `SETTLED`, and `REJECTED`. A notification is queued only when a persisted claim changes from one status to another; initial submission returns its status in the API response.

## Verify

```powershell
mvn test
```
