# Rollback

Application rollback means routing back to the last validated image while retaining the current database. V13 is additive, so the prior application ignores its organization settings columns, tag tables, indexes, and `pg_trgm` extension. Do not delete or edit Flyway history.

If a data rollback is required, stop writers, create and verify a fresh backup, restore the selected known-good backup into an isolated database, validate counts/Flyway/tenant integrity, and perform an operator-approved cutover. Never use `docker compose down -v` against an operational project.
