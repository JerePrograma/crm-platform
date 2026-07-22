# Production architecture profile

The supported production-shaped profile remains a modular monolith: an unprivileged Java 21 backend, an unprivileged nginx frontend/reverse proxy, and PostgreSQL 17 as the source of truth. PostgreSQL is not published to the host. The runtime network is internal and provider-neutral; TLS terminates at an operator-selected reverse proxy or load balancer in front of the frontend.

The local profile in `deploy/docker-compose.production.yml` proves image construction, health probes, read-only application filesystems, resource limits, graceful shutdown, additive Flyway migration, and fail-closed messaging. It is not a production deployment.

Transactional outbox processing is at-least-once. Provider calls remain disabled. Tenant IDs are required in durable operational records and every query/mutation is scoped by the authenticated organization.
