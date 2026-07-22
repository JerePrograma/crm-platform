# Production security checklist

- TLS termination and HSTS are active at the trusted edge; cookies are Secure, HttpOnly, and SameSite.
- PostgreSQL has no public listener and backup storage is encrypted with limited access.
- Runtime containers are non-root, no-new-privileges, resource-limited, and read-only except explicit tmpfs/volumes.
- CSP, frame denial, MIME sniffing protection, CSRF, session fixation protection, brute-force lockout, and restrictive CORS behavior were verified.
- Import file, row, column, cell, and request limits are enabled; XLSX content must match its extension.
- Webhooks require HMAC, timestamp, nonce, replay protection, size/type controls, and rate limits.
- Logs and metrics contain correlation/actor/organization identifiers but no passwords, tokens, cookies, full contact values, message bodies, or webhook payloads.
- All messaging guards are blocked and the persistent kill switch is true.
- Dependency findings and repository/secret scans have been reviewed; exceptions require owner, rationale, and expiry.
