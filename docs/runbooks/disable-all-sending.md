# Disable all sending

Set `SENDING_ENABLED=false`, `SENDING_DRY_RUN=true`, `SENDING_DAILY_LIMIT=0`, `SENDING_KILL_SWITCH=true`, and `MESSAGING_REAL_NETWORK_ALLOWED=false`; use NOOP/DEEPLINK provider modes. Restart the backend, verify effective safety through the API, verify the database `sending.kill-switch=true`, and query that `SENT`, `DELIVERED`, and `READ` counts are zero. The UI/API cannot relax environment blocks.
