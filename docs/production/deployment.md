# Deployment procedure

1. Review the exact commit and sanitized validation evidence.
2. Build both images from the commit without cache and record digests and SBOM/scanner output.
3. Restore the latest synthetic backup in an isolated database and verify Flyway history.
4. Inject runtime secrets through the platform, with all sending guards fixed to their blocked values.
5. Run migrations as a controlled, single-writer step; V1-V13 are forward-only and V13 is additive.
6. Start PostgreSQL privately, then the backend, then the frontend. Require readiness before routing traffic.
7. Verify security headers, login, tenant isolation, `message_record` zero forbidden states, and provider modes.

The repository does not authorize or perform a deployment. Cloud, DNS, certificate, database HA, and platform choices remain operator decisions.
