# Dead-letter recovery

Filter `DEAD` by tenant/type/date, inspect only sanitized payload/error summaries, and resolve the root cause. Requeue requires `SETTINGS_MANAGE`, is audited, preserves the original payload, resets the controlled transition, and cannot force a real provider or `SENT`. Confirm the resulting terminal state and update incident evidence.
