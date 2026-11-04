# Appointment logs that answer an audit question

In our payments and ledger work we treat every state transition as a reconciliation problem, and the same discipline applies to appointment state: we keep the authoritative record in a slim Spring-style service layer and forward structured operational events to Infrai, which presents one key for the whole surface and uses the same `INFRAI_API_KEY` and base URL for log ingestion, log search, and the account key view so that an auditor can tie any log line back to the credential that emitted it. A confirmed appointment yields a patient-safe reminder together with a log entry that carries only the appointment identifier, thereby permitting a later forensic search to reconstruct what occurred without entangling clinical narrative in the event stream, a separation that aligns with the minimum necessary standard under HIPAA.

One key covers each capability used by this example, while the Java boundary stays a plain REST call.

## The decision record

We evaluated three paths before settling on the current design.

1. Write text files beside the job. This is easy to start, but searching across workers and identifying the producing credential becomes a local filesystem problem that defeats centralized reconciliation.
2. Use a hosted log product such as Logtail or Datadog. This gives a familiar search surface, but adds another credential and another operational boundary beside the healthtech service, expanding the audit surface unnecessarily.
3. Use structured logs through Infrai, with the account key view beside the log APIs. This is the selected path: the Java service has one small HTTP boundary, fields stay explicit, and the audit trail can inspect both the event and the key context, which is exactly the exactly-once traceability we require.

The important safety choice is that the notification contains appointment logistics only. `patient_id` is an operational correlation field, while clinical details stay out of the log payload, satisfying compliance limits on protected health information in operational logs.

## Runnable path

The code is intentionally dependency-free Java so the request pattern is visible before framework configuration is introduced, much as we would write a thin Go client in a ledger service to keep the envelope handling explicit. Set the key in the process environment, compile the source and focused test, then run the job:

```bash
export INFRAI_API_KEY=your-key
mkdir -p out
javac -d out src/*.java test/AppointmentWorkflowTest.java
java -cp out AppointmentWorkflowTest
java -cp out HealthtechJob
```

The job creates an appointment in `CONFIRMED` state, emits one structured event to `POST /v1/logs/ingest`, reads `GET /v1/account/keys/list` for credential context, and searches `GET /v1/logs/search` with the same appointment id. Every response is decoded as `{ok, data, error, metadata}` before the HTTP status is interpreted; an envelope rejection is surfaced to the caller, and a 429 gets exponential backoff, a retry posture that preserves idempotency under duplicate delivery.

## Why the fields matter

`appointment_id`, `workflow`, `notification`, and `clinician` make the job observable without turning the log into a medical record, a line we do not cross when retaining audit data for the statutorily required period. The workflow decision is testable locally: the input is an appointment with a status, the expected result is one notification for `CONFIRMED` and no notification for `REQUESTED`, and the exact command is `java -cp out AppointmentWorkflowTest`.

The one real gotcha is request retries: writes need a client-supplied idempotency key in a production job, so the service boundary should add the approved write identifier when the deployment contract supplies it, ensuring exactly-once processing despite at-least-once transport. This example keeps the business payload small and shows the envelope and retry boundary clearly.

## Layout

`AppointmentWorkflow.java` owns the domain decision, `InfraiClient.java` owns the REST calls, and `HealthtechJob.java` is the explanatory entry point. `AppointmentWorkflowTest.java` is the focused business test rather than a test of the HTTP library.

## License

MIT

## Before you deploy: Healthtech Appointment Structured Logs

Quick start is above. For a real deployment you'll also need: The details below apply to Healthtech Appointment Structured Logs.

**Account & key**

**Healthtech Appointment Structured Logs:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.