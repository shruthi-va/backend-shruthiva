# Claims Management Application Flow

The diagrams below show the claim lifecycle and the asynchronous claimant-status notification path. Swagger UI is used to verify the REST APIs; it is not a claimant-facing frontend.

## Claim workflow

```mermaid
flowchart TD
    Tester["Developer / API tester"] -->|Swagger UI| API["REST API"]
    Claimant["Claimant API client"] -->|Submit, track, respond| API
    Staff["Claims Staff API client"] -->|Queue, review, assess| API
    Manager["Manager API client"] -->|Dashboard, final decision| API

    API --> Controllers["controller package"]
    Controllers --> Service["claim.application.ClaimService"]
    Service <--> DB[("H2: claims and status history")]

    Service --> Submitted["SUBMITTED<br/>Claim ID and status returned"]
    Submitted --> Queue["Staff work queue"]
    Queue --> Pickup["Staff picks up claim"]
    Pickup --> Review["IN_REVIEW"]

    Review --> MoreInfo{"More information needed?"}
    MoreInfo -- Yes --> Awaiting["AWAITING_INFORMATION"]
    Awaiting -->|Claimant responds through API| Review
    MoreInfo -- No --> Assessment["Staff submits assessment and recommendation"]
    Assessment --> Pending["PENDING_MANAGER_DECISION"]
    Pending --> Decision{"Manager decision"}
    Decision -- Reject --> Rejected["REJECTED"]
    Decision -- Settle --> Progress["SETTLEMENT_IN_PROGRESS"]
    Progress -->|Staff marks settlement complete| Settled["SETTLED"]

    Manager --> Dashboard["Manager dashboard<br/>backlog, exposure, team workload"]
    Dashboard --> Service
```

## Status notification path

```mermaid
flowchart LR
    Transition["Claimant-visible status changes"] --> Transaction["ClaimService saves claim status<br/>and outbox event in one transaction"]
    Transaction --> H2[("H2 outbox")]
    H2 --> Publisher["Kafka outbox publisher"]
    Publisher --> Kafka["Kafka"]
    Kafka --> Listener["Kafka listener"]
    Listener --> Notify["StatusNotificationService"]
    Notify --> Lookup["ClaimNotificationLookup<br/>gets claimant channel/address"]
    Lookup --> Claims["ClaimService"]
    Notify --> Dedup["Check and record delivery"]
    Dedup --> H2
    Notify --> Sender["Demo sender"]
    Sender --> Logs["Logs only<br/>real SMS/email provider not configured"]
```

## Notes

- Initial `SUBMITTED` status is returned in the API response; it does not produce a separate notification.
- Later claimant-visible status changes are written to the outbox in the same transaction as the claim update.
- Internal edits such as staff assignment or notes do not notify claimants.
- Kafka events contain the claim reference and new status, not claim details or contact information.
- The current notification sender is a demo adapter and does not send real SMS or email.
