# PII and retention engineering note

The CRM stores institution data, contact names and channels, commercial notes, tasks, activities, message drafts/excerpts, external identifiers, exclusions, audit events, and import diagnostics to support the commercial workflow. PostgreSQL is the source of truth. Access is permission- and tenant-scoped; exports are permissioned and formula-neutralized.

Minimize collection and avoid secrets, full provider payloads, cookies, credentials, and unnecessary message bodies in outbox/inbound/audit/logs. Inbound metadata is limited and sanitized. Corrections use versioned domain operations. Contacts, notes, prospects, tags, and quarantine items preserve history through deactivation, archival, discard, or soft deletion where implemented; audit records are application-immutable.

Define retention periods with the organization and local counsel for contacts, no-contact/exclusions, audit, inbound/quarantine, imports, exports, and backups. Deletion requests must preserve legal no-contact safeguards and required audit evidence without reactivating campaigns. Backups may retain deleted data until rotation and require an exceptional restore/re-delete procedure.

This is a technical description, not definitive legal advice. Argentina and each operating jurisdiction require a qualified privacy/legal review before operational data or external messaging is enabled.
