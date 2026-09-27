# SQLite design and migration decisions

The current application uses SQLite only. The original database server was unavailable, and the project supplied no DDL or data files. The user explicitly instructed not to use Derby. This is a schema/application migration with a fresh database, **not a completed transfer of historical records**.

## Mapping from the inspected queries

| Original concept | SQLite representation |
|---|---|
| Customer username, name, email | `users` with generated integer ID and unique case-insensitive username/email |
| Plaintext password | salted PBKDF2 hash; no plaintext column |
| Source-code admin account | persisted `ADMIN` role, created through first-run setup |
| Campsite ID/name/province/rate | `campsites`; string ID retained, rate stored as integer cents |
| Reservation number | SQLite-generated integer ID |
| Reservation username | foreign-key `user_id` |
| Rate on reservation | immutable price snapshot in `rate_cents` |
| Manually entered night count | derived from arrival/departure dates |
| dd/MM/yyyy date strings | ISO `yyyy-MM-dd` text, validated as LocalDate before writing |
| Feedback text file | structured `feedback` records linked to users, with UTC timestamps |

Foreign keys are enabled per connection. Useful reservation and feedback indexes are in `schema.sql`. A `user_version` marks the initial schema version; newer unknown versions are rejected. Future schema changes should use explicit numbered migrations rather than modifying an already-deployed table in place.

Booking uses `BEGIN IMMEDIATE` so checking an active campsite, re-reading its rate, checking availability and inserting the reservation happen under the same SQLite writer lock. The caller's quoted rate must match the current database rate. Insert/update triggers additionally prohibit overlapping confirmed reservations. The interval rule is `existing.arrival < new.departure AND existing.departure > new.arrival`.

Archived sites remain available in administrative reports and existing bookings. Cancellation changes status and never deletes history. Monetary totals are derived with exact integer arithmetic. Rates are bounded to LKR 1,000,000 per person/night, guest counts to 100, and stay length to 365 nights.

Default location: `data/tenttrack.db`; override using `-Dtenttrack.db=...` or `TENTTRACK_DB`. No machine-specific path or default database password is required. Runtime files are ignored by Git. The old feedback file is left untouched locally and is not silently imported without attribution.
