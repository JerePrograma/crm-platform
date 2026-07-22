# Worker failures

Check readiness, worker health, `RETRY`/`DEAD` counts, leases, database latency, and correlation IDs. Keep sending guards blocked. Recover expired leases through the normal worker path. Requeue only a sanitized `DEAD` event in the same tenant with `SETTINGS_MANAGE`; do not edit payloads. Escalate repeated typed failures rather than increasing attempts blindly.
