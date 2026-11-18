# Claims Management Flow

```mermaid
flowchart TD
    Tester["Developer / API tester"] -->|Swagger UI| API["REST API"]
    Claimant["Claimant"] -->|Submit, track, respond| API
    Staff["Claims Staff"] -->|Queue, review, assess| API
    Manager["Manager"] -->|Final decision, dashboard| API

    subgraph App["Spring Boot modular monolith"]
        API --> Claims["Claims module"]
        API --> Work["Work management module"]
        API --> Reporting["Reporting module"]
        Work --> Claims
        Claims <--> H2[("H2 database")]
        Work --> H2
        Reporting --> H2

        Claims --> Submitted["Status: SUBMITTED"]
        Submitted --> Queue["Available in staff work queue"]
        Queue --> Pickup["Staff picks up claim"]
        Pickup --> Review["Status: IN_REVIEW"]
        Review --> Info{"More information needed?"}
        Info -- Yes --> Await["Status: AWAITING_INFORMATION"]
        Await -->|Claimant responds via API| Review
        Info -- No --> Assess["Staff submits assessment and recommendation"]
        Assess --> Pending["Status: PENDING_MANAGER_DECISION"]
        Pending --> Decision["Manager makes final decision"]
        Decision -->|Approve settlement| Progress["Status: SETTLEMENT_IN_PROGRESS"]
        Progress -->|Staff completes settlement| Settled["Status: SETTLED"]
        Decision -->|Reject| Rejected["Status: REJECTED"]
        Manager --> Dashboard["View backlog, exposure, team workload"]
        Dashboard --> Reporting
    end

    Review -. "status changed" .-> Outbox["Persist status event with claim update"]
    Await -. "status changed" .-> Outbox
    Pending -. "status changed" .-> Outbox
    Progress -. "status changed" .-> Outbox
    Settled -. "status changed" .-> Outbox
    Rejected -. "status changed" .-> Outbox
    Outbox --> Kafka["Kafka"]
    Kafka --> Notify["Notification consumer"]
    Notify --> Channel["Send status update by SMS or email"]
    Channel --> Claimant
```

## Flow notes

- Claim submission returns the claim reference and `SUBMITTED` status in the API response; that initial status is not sent as a separate notification.
- Staff can pick up and review a claim, request more information, and submit an assessment recommendation.
- The manager makes the final settlement or rejection decision. Staff can then complete an approved settlement.
- Notifications are queued only when a claimant-visible status changes. Internal edits such as staff assignment or notes do not notify claimants.
- Kafka events carry the claim reference and new status, not claim details or contact information.
- Swagger UI is for API verification; it is not a claimant-facing frontend.

The status names and workflow are a first-pass design and can be amended as business rules are clarified.
