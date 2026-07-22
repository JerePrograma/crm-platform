# Incident response

Classify the incident, preserve UTC timestamps and correlation IDs, and avoid copying secrets or PII into the ticket. For any messaging concern, apply the environment kill switch first and verify the PostgreSQL switch. Preserve audit/outbox/inbound rows and sanitized logs.

Contain access, rotate affected credentials through the external secret manager, invalidate sessions where appropriate, and isolate compromised components. Recover using the database runbook and validated backups. Verify tenant isolation, audit continuity, probes, and zero forbidden message states before reopening. Record cause, scope, decisions, evidence, and follow-up owners.
