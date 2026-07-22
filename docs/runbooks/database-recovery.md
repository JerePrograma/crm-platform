# Database recovery

Stop application writers, preserve the failed database, verify the selected backup checksum, and restore only to a `crm_restore_` isolation target. Confirm Flyway history, tenant-scoped counts, constraints, audit/outbox continuity, and application readiness. Cutover requires operator approval. Never destroy the original volume as part of diagnosis.
