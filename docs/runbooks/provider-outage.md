# Provider outage

Real providers are not connected in the validated profile. Keep them disconnected, record `BLOCKED_EXTERNAL`, and use NOOP/FAKE behavior only. If a future authorized integration is affected, activate all sending blocks, preserve sanitized provider error codes and correlation IDs, and do not retry non-retryable policy/configuration failures.
