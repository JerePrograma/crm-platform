# Webhook failures

Verify the fake endpoint is explicitly enabled only in a synthetic environment, its secret is present only in environment state, and the timestamp window/rate/size limits are correct. Inspect sanitized status and correlation ID, never the full body/signature. Invalid signatures, stale timestamps, wrong content type, oversized bodies, and replay are expected rejections. Unknown or ambiguous valid senders belong in quarantine.
