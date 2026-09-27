# Modernization verification

Verified on Windows with Oracle JDK 21.0.1 and Maven 3.9.16.

## Build results

The final clean test/package run passed **17 tests, 0 failures, 0 errors, 0 skipped** with the opt-in desktop smoke test enabled. The standard test run excludes that graphical test so it can run on a headless machine.

```shell
mvn -Dtenttrack.uiSmoke=true clean test package
```

The local environment required the additional `-Dmaven.repo.local` argument documented in the README. Initial Maven cache access/dependency download problems were resolved before verification.

The executable `target/camping-reservation-1.0.0.jar` was launched independently using `javaw -jar`. Its first-run setup window appeared, the process remained running, and startup stderr was empty. The new local database contains no preset user accounts.

No Java compiler warnings were reported in the final build. Maven Shade reports overlapping dependency manifests and an SLF4J license resource; these are packaging metadata overlaps, not duplicate application classes. The generated manifest launches the correct entry point, JDBC service resources are merged, and the packaged application starts successfully.

## Coverage

| Area | Verified behavior |
|---|---|
| Initialization | Repeatable schema initialization, per-connection foreign keys, rejection of orphan data |
| Authentication | Registration, administrator setup, login for both roles, hashed storage, failed login, password clearing, unique username/email validation |
| Authorization | User cannot administer sites, view admin feedback/reports, or read/cancel another user's reservation; logout revokes access |
| Campsites | Search, editable details/rates, archive, unavailable archived sites, repeatable optional seed loading |
| Reservations | Valid booking, derived nights, persisted rates, missing sites, null/past/reversed/equal dates, guest/stay limits, terms acceptance, changed rates |
| Availability | Exact/partial/contained/enclosing overlap cases, checkout-day adjacency, database-trigger protection, two concurrent writers with exactly one winner |
| Cancellation | History preserved, status updated, dates released, new reservation gets a distinct ID |
| Pricing/billing | Exact minor-unit arithmetic beyond 32-bit totals, snapshot preserved after rate changes, localized summary formatting |
| Reports | Persisted records filtered by arrival dates/campsite, correct booking-value output |
| Feedback | Required content validation, trimmed persistence, admin retrieval |
| Localization | Matching English/Spanish keys, localized bills and real language switching in the running UI |
| Desktop | First-run setup, registration, both logins, all major navigation pages, booking and summary dialogs, persisted booking visible to admin, feedback visible to admin |

The desktop test captures 19 screenshots under `target/screenshots/`. Representative reviewed copies are included under `docs/screenshots/`. The screen review covered forms, tables, booking details, billing, feedback, reporting and Spanish navigation. The desktop journey runs against a temporary database and never seeds the user's working database.

The subsequent UI refresh also passed all 17 tests. After final viewport fixes, the desktop journey and packaging passed again. That journey now checks instant campsite search, province filtering, resetting filters and booking directly from a card. Screenshots were reviewed at 1200×820 and 1000×720, including Spanish. Form width tracking and initial scroll positions were corrected based on that review.

## Final source review

* No Derby or AbsoluteLayout classes are present in the runnable JAR or active application source/configuration.
* No machine-specific image/database/printing paths or default account credentials remain in active code.
* Password literals in tests are confined to isolated temporary test accounts. Original credentials/source are retained only in the ignored local backup.
* UI classes do not contain JDBC queries. Connections/statements/result sets use try-with-resources; booking rollback failures are preserved as suppressed exceptions.
* User-facing UI strings are in language bundles apart from product identity and startup diagnostics.
* Legacy launchers redirect through authentication rather than constructing unguarded admin forms.
* No Git repository was present during modernization. A local repository was subsequently requested and prepared from the completed files; see `HISTORY.md`. No older development history was reconstructed, and nothing was pushed.

## Remaining limits

No existing database records were imported: none were provided, and the user requested SQLite only. Original NetBeans generated layouts are locally archived, while new screens use ordinary Swing layout managers. Existing feedback text is preserved locally but is not automatically attributed/imported.

Physical printing and OS file-dialog export were not exercised by automation. Document generation was tested, and both actions are implemented with surfaced errors. No payment collection, tax/refund processing, email password recovery, PDF export or multi-device server is provided. Desktop layout verification was performed on Windows; other operating systems and accessibility assistive technologies were not manually verified. Reports/tables load matching data into memory and are intended for a small local desktop dataset.

## Major changed components

* `pom.xml`, `nbactions.xml`, `Project.java`: portable Java 21 build, launch and executable packaging.
* `config/Database.java`, `db/schema.sql`: SQLite setup and integrity.
* `model/`, `repository/`, `service/`: domain data, JDBC access, authentication/session and business rules.
* `ui/AppFrame.java`, `CampsitesView`, `DashboardView`, `ReservationsView`, `FeedbackView`, `Theme`, `Documents`: common shell and redesigned workflows.
* `i18n/`: English/Spanish resources and formatting.
* `ApplicationTest`, `UiSmokeTest`: service/database and desktop verification.
* `.gitignore`, `README.md`, audit/database/verification documents: GitHub preparation and setup instructions.

Implementation references: [SQLite JDBC](https://github.com/xerial/sqlite-jdbc), [FlatLaf](https://www.formdev.com/flatlaf/), [OWASP password storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html), [Google Java formatter](https://github.com/google/google-java-format). The formatter was used as a development tool, not added as an application dependency.
