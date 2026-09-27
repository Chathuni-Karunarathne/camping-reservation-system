# Existing-project audit

The supplied project has 14 Java classes, 13 NetBeans JFrame forms, three resource bundles, images, a local AbsoluteLayout JAR, and a feedback text file. There is no Git repository, database dump, schema DDL, or test suite.

## Confirmed findings
- lang is the configured entry point; Project prints Hello World and the POM names a nonexistent main class.
- Customer workflow: main → user_login/user_reg → welcome → customers → terms → bill → feedback. Administration: admin_login → manage → admin/ad_report.
- Database forms duplicate connections with embedded credentials. Many JDBC resources leak. Most queries already use parameters; unsafe SQL concatenation was not assumed.
- Passwords are plaintext and printed during login. The administrator password is hardcoded. Direct admin frame entry points bypass login.
- Booking trusts editable username, rate and nights. Null dates are formatted before validation; negative values, past dates and reversed ranges are not rejected.
- Availability compares dd/MM/yyyy strings with inclusive endpoints. IDs start at a fixed number and subtract counts; check/insert is not atomic.
- Bills correctly retain the reservation rate, but unchecked integer multiplication can overflow. Printing writes a screenshot to a machine-specific path and suppresses errors.
- Report campsite choices are hardcoded; only one match is shown. Feedback is unstructured text and read errors are swallowed.
- Hidden windows retain running timers. Locale constructors skip some initialization; Spanish contains missing/duplicate keys. Errors and headings often remain English.
- Fixed positioning, decorative fonts and absolute image paths make screens fragile.
- Java 21 compiles, with raw collection/deprecation warnings. Preview configuration is unnecessary; AbsoluteLayout uses a local pseudo repository.

## Legacy schema inferred from queries
Actual types/constraints/live records were unavailable.
| Table | Observed columns |
|---|---|
| CUSTOMERS | FULL_NAME, EMAIL, USERNAME, PASSWORD |
| CAMPSITES | CAMP_ID, CAMPSITE, PROVINCE, RATE_PER_PERSON |
| RESERVATION | RESERVATION_NUMBER, CAMP_ID, RATE_PER_PERSON, USERNAME, NUMBER_OF_PEOPLE, NUMBER_OF_NIGHTS, ARRIVAL_DATE, DEPARTURE_DATE |

## Decisions
Preserve Java 21, Swing, Maven, JCalendar, both languages and the existing functional workflows. Normalize user references, retain string campsite IDs, store ISO dates and integer minor currency units, derive nights/totals and snapshot rates. Preserve exclusive whole-campsite booking; checkout day permits another arrival.

Use one FlatLaf window with reusable layouts and service authorization. The exact original Java/form pairs are preserved in the ignored .local-backup/original-project.zip. Modern views use Java layout managers rather than NetBeans generated layouts. Legacy entry point names redirect to the authenticated app.

The user explicitly requested SQLite only after the missing database was identified. Initialize a fresh database; do not connect to Derby or claim existing records were transferred. No default account or password is shipped. The first administrator is created interactively. Sample campsites are opt-in resources.
